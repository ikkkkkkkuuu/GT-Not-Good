package com.xyp.gtnotgood.qa;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.machines.basic.EndCrystalResonator;
import com.xyp.gtnotgood.loader.EndCrystalResonatorLoader;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(
    modid = "crystalresonatorserverqa",
    name = "End Crystal Resonator Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class EndCrystalResonatorServerChecks {

    private int checks;

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.crystalresonator.serverQA")) return;
        Path result = Path.of(System.getProperty("gtng.crystalresonator.qa.output"), "result.txt");
        MinecraftServer server = MinecraftServer.getServer();
        try {
            require(server.isDedicatedServer(), "dedicated server");
            try {
                EntityEnderCrystal.class.getConstructor(World.class, double.class, double.class, double.class);
                throw new AssertionError("client-only constructor must be absent on this runtime");
            } catch (NoSuchMethodException expected) {
                require(true, "client-only crystal constructor is stripped");
            }
            WorldServer world = server.worldServerForDimension(0);
            FakePlayer player = FakePlayerFactory.getMinecraft(world);
            for (int tier = 1; tier <= 14; tier++) verifyPlacement(world, player, tier);
            Files.write(result, "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println("CRYSTAL_RESONATOR_SERVER_QA: PASS (" + checks + " checks)");
        } catch (Throwable failure) {
            Files.write(result, ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
            throw failure;
        } finally {
            server.initiateShutdown();
        }
    }

    private void verifyPlacement(WorldServer world, EntityPlayer player, int tier) {
        int x = 64 + tier * 4, y = 80, z = 64;
        world.getChunkFromBlockCoords(x, z);
        world.setBlockToAir(x, y + 1, z);
        require(world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3), "place resonator " + tier);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        ItemStack machineStack = EndCrystalResonatorLoader.machine(tier).get(1);
        base.setInitialValuesAsNBT(null, (short) machineStack.getItemDamage());
        base.setOwnerUuid(player.getUniqueID());
        base.setFrontFacing(ForgeDirection.NORTH);
        EndCrystalResonator machine = (EndCrystalResonator) base.getMetaTileEntity();
        machine.onFirstTick(base);
        player.capabilities.isCreativeMode = false;
        ItemStack crystals = ModsItemlist.PlaceableEndCrystal.get(2);
        require(crystals != null, "native crystal item exists");
        player.inventory.setInventorySlotContents(player.inventory.currentItem, crystals);
        require(!machine.onRightclick(base, player, ForgeDirection.NORTH, 0.5F, 0.5F, 0.5F), "reject side face");
        world.setBlock(x, y + 1, z, Blocks.stone);
        require(!machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "reject occupied top");
        require(crystals.stackSize == 2, "rejected placement does not consume crystal");
        world.setBlockToAir(x, y + 1, z);
        require(machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "place crystal " + tier);
        require(crystals.stackSize == 1, "survival placement consumes exactly one");
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y + 1, z, x + 1, y + 2, z + 1);
        var entities = world.getEntitiesWithinAABB(EntityEnderCrystal.class, box);
        require(entities.size() == 1, "one crystal spawned above machine");
        EntityEnderCrystal crystal = (EntityEnderCrystal) entities.get(0);
        require(crystal.posX == x + 0.5 && crystal.posY == y + 1 && crystal.posZ == z + 0.5,
            "crystal uses exact original position");
        require(!machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "reject duplicate crystal");
        require(crystals.stackSize == 1, "duplicate placement consumes nothing");
        BigInteger before = WirelessNetworkManager.getUserEU(player.getUniqueID());
        base.enableWorking();
        for (int tick = 1; tick <= 20; tick++) machine.onPostTick(base, tick);
        BigInteger generated = WirelessNetworkManager.getUserEU(player.getUniqueID()).subtract(before);
        require(generated.equals(BigInteger.valueOf(EndCrystalResonator.generationForTier(tier) * 20)),
            "exact wireless generation after placement " + tier);
        require(!crystal.isDead, "generation preserves crystal");
        base.disableWorking();
        crystal.setDead();
        world.removeEntity(crystal);
        player.capabilities.isCreativeMode = true;
        require(machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "creative placement " + tier);
        require(crystals.stackSize == 1, "creative placement consumes nothing");
        for (Object entity : world.getEntitiesWithinAABB(EntityEnderCrystal.class, box)) {
            world.removeEntity((EntityEnderCrystal) entity);
        }
        world.setBlockToAir(x, y, z);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
