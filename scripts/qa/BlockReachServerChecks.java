package com.xyp.gtnotgood.qa;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.SidedPosGuiData;
import com.cleanroommc.modularui.factory.SidedTileEntityGuiFactory;
import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidMinerConfigGuiFactory;
import com.xyp.gtnotgood.config.InteractionConfig;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "blockreachserverqa",
    name = "Block Reach Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class BlockReachServerChecks {

    private int checks;

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.blockreach.serverQA")) return;
        MinecraftServer server = MinecraftServer.getServer();
        Path result = Path.of(System.getProperty("gtng.blockreach.qa.output"), "result.txt");
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            verify(server);
            Files.write(result, "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println("BLOCK_REACH_SERVER_QA: PASS (" + checks + " checks)");
        } catch (Throwable failure) {
            Files.write(result, ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
            throw new AssertionError("block reach server QA failed", failure);
        } finally {
            server.initiateShutdown();
        }
    }

    public int verify(MinecraftServer server) throws Exception {
        WorldServer world = server.worldServerForDimension(0);
        double original = InteractionConfig.blockReachDistance;
        try {
            equal(16, original, "configured default");
            ItemInWorldManager manager = new ItemInWorldManager(world);
            equal(16, manager.getBlockReachDistance(), "server manager uses default reach");
            manager.setBlockReachDistance(12.25);
            equal(12.25, manager.getBlockReachDistance(), "Forge public reach setter remains effective");
            InteractionConfig.blockReachDistance = 6.5;
            equal(6.5, new ItemInWorldManager(world).getBlockReachDistance(),
                "new manager reads changed configuration");
            InteractionConfig.blockReachDistance = 2;
            equal(5, new ItemInWorldManager(world).getBlockReachDistance(),
                "manual config cannot enable one to four blocks");
            InteractionConfig.blockReachDistance = 16;
            EntityPlayerMP player = new EntityPlayerMP(server, world,
                new GameProfile(UUID.fromString("4b547cf1-dc7b-49f0-a88d-1528665c6a70"), "ReachQA"),
                new ItemInWorldManager(world));
            NetHandlerPlayServer handler = new NetHandlerPlayServer(server, new NetworkManager(false), player);
            player.setPosition(128.5, 80, 128.5);
            for (WorldSettings.GameType mode : new WorldSettings.GameType[] { WorldSettings.GameType.SURVIVAL,
                WorldSettings.GameType.CREATIVE }) {
                player.setGameType(mode);
                verifyPackets(world, player, handler, mode);
            }
            verifyContainers(world, player, handler);
            verifyVanillaTargets(world, player);
            verifyFactories(world, player);
            return checks;
        } finally {
            InteractionConfig.blockReachDistance = original;
        }
    }

    private void verifyPackets(WorldServer world, EntityPlayerMP player, NetHandlerPlayServer handler,
        WorldSettings.GameType mode) throws Exception {
        int x = 128, y = 80, near = 144, far = 148;
        player.setPosition(128.5, 80, 128.5);
        player.inventory.currentItem = 0;
        player.inventory.mainInventory[0] = null;
        world.setBlock(x, y - 1, near, Blocks.stone);
        world.setBlock(x, y, near, mode.isCreative() ? Blocks.stone : Blocks.torch, 0, 3);
        handler.processPlayerDigging(dig(x, y, near));
        require(world.isAirBlock(x, y, near), mode + " digging beyond vanilla reach succeeds");
        world.setBlock(x, y - 1, far, Blocks.stone);
        world.setBlock(x, y, far, mode.isCreative() ? Blocks.stone : Blocks.torch, 0, 3);
        handler.processPlayerDigging(dig(x, y, far));
        require(!world.isAirBlock(x, y, far),
            mode + " digging beyond configured reach plus Forge tolerance is rejected");
        world.setBlock(x, y, near, Blocks.stone);
        world.setBlockToAir(x, y + 1, near);
        player.inventory.mainInventory[0] = new ItemStack(Blocks.cobblestone, 2);
        handler.processPlayerBlockPlacement(place(x, y, near, player.getHeldItem()));
        require(world.getBlock(x, y + 1, near) == Blocks.cobblestone, mode + " far placement reaches server world");
        require(player.getHeldItem().stackSize == (mode.isCreative() ? 2 : 1), mode + " placement consumption");
        world.setBlock(x, y, far, Blocks.stone);
        world.setBlockToAir(x, y + 1, far);
        handler.processPlayerBlockPlacement(place(x, y, far, player.getHeldItem()));
        require(world.isAirBlock(x, y + 1, far), mode + " out-of-range placement is rejected");
        world.setBlockToAir(x, y + 1, near);
        world.setBlockToAir(x, y, near);
        world.setBlockToAir(x, y, far);
    }

    private void verifyContainers(WorldServer world, EntityPlayerMP player, NetHandlerPlayServer handler)
        throws Exception {
        int x = 128, y = 80, z = 144;
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        player.setPosition(128.5, 80, 128.5);
        player.inventory.mainInventory[0] = null;
        world.setBlock(x, y, z, Blocks.chest);
        world.setBlockToAir(x, y + 1, z);
        handler.processPlayerBlockPlacement(place(x, y, z, null));
        require(player.openContainer instanceof ContainerChest, "far right-click opens real vanilla chest container");
        require(player.openContainer.canInteractWith(player), "chest remains usable at sixteen blocks");
        player.setPosition(128.5, 80, 125.5);
        require(!player.openContainer.canInteractWith(player), "chest still rejects excessive distance");
        player.closeScreen();
        player.setPosition(128.5, y - 17, z + 0.5);
        handler.processPlayerBlockPlacement(place(x, y, z, null));
        require(player.openContainer instanceof ContainerChest && player.openContainer.canInteractWith(player),
            "chest above the player stays usable when its face is within sixteen blocks of the eyes");
        player.closeScreen();
        player.setPosition(128.5, 80, 128.5);
        ItemStack machine = ItemList.Machine_LV_E_Furnace.get(1);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) machine.getItemDamage());
        tile.setOwnerUuid(player.getUniqueID());
        for (int i = 0; i < 3; i++) tile.updateEntity();
        require(tile.isUseableByPlayer(player), "native GT furnace inventory accepts sixteen-block distance");
        player.setPosition(128.5, 80, 125.5);
        require(!tile.isUseableByPlayer(player), "native GT furnace rejects excessive distance");
        world.setBlockToAir(x, y, z);
    }

    private void verifyVanillaTargets(WorldServer world, EntityPlayerMP player) {
        int x = 128, y = 80, z = 144;
        for (Block block : new Block[] { Blocks.chest, Blocks.furnace, Blocks.hopper, Blocks.dispenser,
            Blocks.brewing_stand, Blocks.beacon }) {
            player.setPosition(128.5, 80, 128.5);
            world.setBlock(x, y, z, block);
            IInventory inventory = (IInventory) world.getTileEntity(x, y, z);
            String name = inventory.getClass().getSimpleName();
            require(inventory.isUseableByPlayer(player), name + " accepts sixteen-block distance");
            player.setPosition(128.5, 80, 125.5);
            require(!inventory.isUseableByPlayer(player), name + " rejects excessive distance");
            player.setPosition(128.5, 80, 128.5);
            world.setBlockToAir(x, y, z);
            require(!inventory.isUseableByPlayer(player), name + " preserves removed-tile rejection");
        }
        world.setBlock(x, y, z, Blocks.ender_chest);
        TileEntityEnderChest enderChest = (TileEntityEnderChest) world.getTileEntity(x, y, z);
        require(enderChest.func_145971_a(player), "ender chest accepts sixteen-block distance");
        player.setPosition(128.5, 80, 125.5);
        require(!enderChest.func_145971_a(player), "ender chest rejects excessive distance");
        player.setPosition(128.5, 80, 128.5);
        world.setBlockToAir(x, y, z);
        require(!enderChest.func_145971_a(player), "ender chest preserves removed-tile rejection");
        Block[] blocks = { Blocks.crafting_table, Blocks.enchanting_table, Blocks.anvil };
        for (int i = 0; i < blocks.length; i++) {
            world.setBlock(x, y, z, blocks[i]);
            Container container = switch (i) {
                case 0 -> new ContainerWorkbench(player.inventory, world, x, y, z);
                case 1 -> new ContainerEnchantment(player.inventory, world, x, y, z);
                default -> new ContainerRepair(player.inventory, world, x, y, z, player);
            };
            String name = container.getClass().getSimpleName();
            require(container.canInteractWith(player), name + " accepts sixteen-block distance");
            player.setPosition(128.5, 80, 125.5);
            require(!container.canInteractWith(player), name + " rejects excessive distance");
            player.setPosition(128.5, 80, 128.5);
            world.setBlockToAir(x, y, z);
            require(!container.canInteractWith(player), name + " preserves missing-block rejection");
        }
    }

    private void verifyFactories(WorldServer world, EntityPlayerMP player) {
        int x = 128, y = 80, z = 144;
        player.setPosition(128.5, 80, 128.5);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) GTNGItemList.LargeVoidMiner.get(1).getItemDamage());
        tile.setOwnerUuid(player.getUniqueID());
        PosGuiData data = new PosGuiData(player, x, y, z);
        SidedPosGuiData sided = new SidedPosGuiData(player, x, y, z, ForgeDirection.NORTH);
        require(TileEntityGuiFactory.INSTANCE.canInteractWith(player, data), "MUI tile factory accepts sixteen blocks");
        require(SidedTileEntityGuiFactory.INSTANCE.canInteractWith(player, sided),
            "MUI sided factory accepts sixteen blocks");
        require(LargeVoidMinerConfigGuiFactory.INSTANCE.canInteractWith(player, data),
            "existing GTNG controller factory accepts sixteen blocks");
        player.setPosition(128.5, 80, 125.5);
        require(!TileEntityGuiFactory.INSTANCE.canInteractWith(player, data),
            "MUI tile factory rejects excessive distance");
        require(!SidedTileEntityGuiFactory.INSTANCE.canInteractWith(player, sided),
            "MUI sided factory rejects excessive distance");
        require(!LargeVoidMinerConfigGuiFactory.INSTANCE.canInteractWith(player, data),
            "existing GTNG controller factory rejects excessive distance");
        player.setPosition(128.5, 80, 128.5);
        var otherPlayer = FakePlayerFactory.getMinecraft(world);
        require(!TileEntityGuiFactory.INSTANCE.canInteractWith(player, new PosGuiData(otherPlayer, x, y, z)),
            "MUI tile factory preserves player identity check");
        require(
            !SidedTileEntityGuiFactory.INSTANCE.canInteractWith(player,
                new SidedPosGuiData(otherPlayer, x, y, z, ForgeDirection.NORTH)),
            "MUI sided factory preserves player identity check");
        require(!LargeVoidMinerConfigGuiFactory.INSTANCE.canInteractWith(player, new PosGuiData(otherPlayer, x, y, z)),
            "existing GTNG controller factory preserves superclass player check");
        world.setBlockToAir(x, y, z);
        require(!TileEntityGuiFactory.INSTANCE.canInteractWith(player, data), "MUI tile factory rejects missing tile");
        require(!SidedTileEntityGuiFactory.INSTANCE.canInteractWith(player, sided),
            "MUI sided factory rejects missing tile");
        require(!LargeVoidMinerConfigGuiFactory.INSTANCE.canInteractWith(player, data),
            "existing GTNG controller factory rejects missing tile");
    }

    private static C07PacketPlayerDigging dig(int x, int y, int z) throws Exception {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
        try {
            data.writeByte(0);
            data.writeInt(x);
            data.writeByte(y);
            data.writeInt(z);
            data.writeByte(1);
            C07PacketPlayerDigging packet = new C07PacketPlayerDigging();
            packet.readPacketData(data);
            return packet;
        } finally {
            data.release();
        }
    }

    private static C08PacketPlayerBlockPlacement place(int x, int y, int z, ItemStack held) throws Exception {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
        try {
            data.writeInt(x);
            data.writeByte(y);
            data.writeInt(z);
            data.writeByte(1);
            data.writeItemStackToBuffer(held);
            data.writeByte(8);
            data.writeByte(16);
            data.writeByte(8);
            C08PacketPlayerBlockPlacement packet = new C08PacketPlayerBlockPlacement();
            packet.readPacketData(data);
            return packet;
        } finally {
            data.release();
        }
    }

    private void equal(double expected, double actual, String message) {
        require(Math.abs(expected - actual) < 0.000001, message + ": " + actual);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
