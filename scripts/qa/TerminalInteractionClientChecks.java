package com.xyp.gtnotgood.qa;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtnotgood.common.items.multiblockterminal.MultiblockTerminalItem;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;

public final class TerminalInteractionClientChecks {

    private static final int Y = 80, Z = 128, FINISHED = 6;
    private static final int[] X = { 560, 592, 624, 624, 656, 688 };
    private volatile int phase = -1;
    private volatile int sentPhase = -1;
    private volatile boolean sneakReady;
    private volatile boolean released;
    private volatile boolean done;
    private volatile long readyAt;
    private long deadline;
    private boolean startSent, stopSent;
    private int serverChecks, clientCalls;
    private Item expectedItem;
    private WorldSettings.GameType previousMode;

    public boolean done() {
        return done;
    }

    public int checks() {
        return serverChecks + clientCalls;
    }

    public void server(MinecraftServer server, EntityPlayerMP player) {
        if (done) return;
        WorldServer world = (WorldServer) player.worldObj;
        if (phase < 0) {
            previousMode = player.theItemInWorldManager.getGameType();
            require(!player.isSneaking(), "real client starts without server sneak state");
            setup(world, player, 0);
            return;
        }
        if (
            System.currentTimeMillis() > deadline
        ) throw new AssertionError("real C08 interaction timed out at phase " + phase
            + ", server sneak="
            + player.isSneaking()
            + ", client call="
            + sentPhase);
        if (phase == FINISHED) {
            if (!released || player.isSneaking()) return;
            require(true, "native C0B stop-sneaking reaches the real server player");
            for (int x : new int[] { 560, 592, 624, 656, 688 }) clear(world, x);
            player.setGameType(previousMode);
            done = true;
            System.out.println("TERMINAL_REAL_C08_QA: PASS (" + serverChecks
                + " server checks, "
                + clientCalls
                + " actual client PlayerController calls)");
            return;
        }
        if (!sneakReady && player.isSneaking()) {
            require(true, "native C0B start-sneaking reaches the real server player");
            sneakReady = true;
        }
        if (sentPhase != phase || !completed(world)) return;
        require(player.isSneaking(), "C08 was processed while the actual server player is sneaking");
        require(world.getTileEntity(X[phase], Y, Z) != null, "C08 retains its controller");
        if (phase == 2) {
            require(count(player, Blocks.cobblestone) == 2 && count(player, Blocks.wool) == 2
                && count(player, Blocks.planks) == 2, "real survival C08 consumes one of each supplied material");
        } else if (phase == 3) {
            require(count(player, Blocks.cobblestone) == 3 && count(player, Blocks.wool) == 3
                && count(player, Blocks.planks) == 3, "real dismantle C08 returns exact original materials");
            require(block(world, X[phase], 3) == Blocks.bedrock && block(world, X[phase], 5) == Blocks.gold_block,
                "real dismantle C08 preserves air requirements and unconstrained blocks");
        } else {
            require(onlyHeld(player), "real creative C08 leaves the player without generated inventory materials");
        }
        if (phase >= 4) require(block(world, X[phase], 3) == Blocks.air,
            "native projector C08 preserves creative rebuilding of explicit air");
        System.out.println("TERMINAL_REAL_C08_PHASE: PASS " + phase);
        int next = phase + 1;
        if (next == FINISHED) {
            phase = FINISHED;
            deadline = System.currentTimeMillis() + 10000;
        } else setup(world, player, next);
    }

    public void client(Minecraft mc) {
        if (done || phase < 0 || mc.thePlayer == null || mc.theWorld == null) return;
        boolean sneak = phase != FINISHED;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), sneak);
        mc.thePlayer.movementInput.updatePlayerMoveState();
        if (phase == FINISHED) {
            if (!mc.thePlayer.isSneaking() && !stopSent) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, 2));
                stopSent = true;
                released = true;
            }
            return;
        }
        if (!mc.thePlayer.isSneaking()) return;
        if (!startSent) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, 1));
            startSent = true;
        }
        int current = phase;
        if (!sneakReady || sentPhase == current || System.currentTimeMillis() < readyAt || mc.currentScreen != null)
            return;
        mc.thePlayer.inventory.currentItem = 0;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (
            held == null || held.getItem() != expectedItem
                || mc.playerController.isInCreativeMode() != creative(current)
        ) return;
        if (Math.abs(mc.thePlayer.posX - (X[current] + 0.5)) > 1 || Math.abs(mc.thePlayer.posZ - (Z - 2.5)) > 1) return;
        if (mc.theWorld.getTileEntity(X[current], Y, Z) == null) return;
        if (
            held.getItem() instanceof MultiblockTerminalItem && (TerminalSettings.dismantle(held) != (current == 3)
                || TerminalSettings.fullBuild(held) != (current != 4))
        ) return;
        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, 1));
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, X[current], Y, Z,
            ForgeDirection.NORTH.ordinal(), Vec3.createVectorHelper(X[current] + 0.5, Y + 0.5, Z));
        clientCalls++;
        sentPhase = current;
        System.out.println("TERMINAL_REAL_C08_SENT: " + current);
    }

    private void setup(WorldServer world, EntityPlayerMP player, int next) {
        int x = X[next];
        if (next != 3) {
            clear(world, x);
            for (int slot = 0; slot < player.inventory.mainInventory.length; slot++)
                player.inventory.mainInventory[slot] = null;
            ItemStack held = next == 5 ? ModsItemlist.StructureHologram.get(1)
                : MultiblockTerminalServerChecks.terminal();
            player.inventory.mainInventory[0] = held;
            if (next == 1) {
                world.setBlock(x, Y, Z, GregTechAPI.sBlockMachines, 0, 3);
                BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, Y, Z);
                tile.setInitialValuesAsNBT(null, (short) ItemList.Machine_Multi_BlastFurnace.get(1).getItemDamage());
                tile.setOwnerUuid(player.getUniqueID());
                tile.setFrontFacing(ForgeDirection.NORTH);
            } else {
                MultiblockTerminalServerChecks.projectionFixture(world, x, Y, Z);
                setBlock(world, x, 3, Blocks.bedrock);
                setBlock(world, x, 5, Blocks.gold_block);
            }
            if (next == 2) {
                player.inventory.mainInventory[1] = new ItemStack(Blocks.cobblestone, 3);
                player.inventory.mainInventory[2] = new ItemStack(Blocks.wool, 3);
                player.inventory.mainInventory[3] = new ItemStack(Blocks.planks, 3);
            }
        }
        player.inventory.currentItem = 0;
        ItemStack held = player.getHeldItem();
        if (next != 5) {
            TerminalSettings.setDismantle(held, next == 3);
            TerminalSettings.setFullBuild(held, next != 4);
            TerminalSettings.setNoHatches(held, next != 1);
            TerminalSettings.setCount(held, 1);
            if (next == 1) ChannelDataAccessor.setChannelData(held, "coil", 2);
        }
        expectedItem = held.getItem();
        player.setGameType(creative(next) ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL);
        player.capabilities.isFlying = false;
        player.sendPlayerAbilities();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) world.setBlock(x + dx, Y - 1, Z - 3 + dz, Blocks.stone, 0, 3);
        player.playerNetServerHandler.setPlayerLocation(x + 0.5, Y, Z - 2.5, 0, 20);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        readyAt = System.currentTimeMillis()
            + Math.max(500, ConfigurationHandler.INSTANCE.getAutoPlaceInterval() + 100);
        deadline = readyAt + 15000;
        phase = next;
    }

    private boolean completed(WorldServer world) {
        int x = X[phase];
        if (phase == 1) {
            BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, Y, Z);
            MTEElectricBlastFurnace furnace = (MTEElectricBlastFurnace) base.getMetaTileEntity();
            if (!furnace.checkStructure(true, base)) return false;
            require(
                furnace.getCoilLevel() == HeatingCoilLevel.MV && furnace.mInputHatches.size() == 1
                    && furnace.mOutputHatches.size() == 1
                    && furnace.mInputBusses.size() == 1
                    && furnace.mOutputBusses.size() == 1
                    && furnace.mMaintenanceHatches.size() == 1
                    && furnace.mEnergyHatches.size() == 1
                    && furnace.mMufflerHatches.size() == 1,
                "real creative C08 forms the native EBF with selected coils and all seven hatch roles");
            return true;
        }
        if (phase == 3) return block(world, x, 1) == Blocks.air && block(world, x, 2) == Blocks.air
            && block(world, x, 4) == Blocks.air;
        return block(world, x, 1) == Blocks.cobblestone && block(world, x, 2) == Blocks.wool
            && block(world, x, 4) == Blocks.planks;
    }

    private boolean onlyHeld(EntityPlayerMP player) {
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() != expectedItem || held.stackSize != 1) return false;
        for (int slot = 1; slot < player.inventory.mainInventory.length; slot++)
            if (player.inventory.mainInventory[slot] != null) return false;
        return true;
    }

    private static boolean creative(int phase) {
        return phase != 2 && phase != 3;
    }

    private static int count(EntityPlayerMP player, Block block) {
        int result = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == Item.getItemFromBlock(block)) result += stack.stackSize;
        return result;
    }

    private static int[] position(int x, int a) {
        int[] offset = new int[3];
        ExtendedFacing.DEFAULT.getWorldOffset(new int[] { a, 0, 0 }, offset);
        return new int[] { x + offset[0], Y + offset[1], Z + offset[2] };
    }

    private static Block block(WorldServer world, int x, int a) {
        int[] position = position(x, a);
        return world.getBlock(position[0], position[1], position[2]);
    }

    private static void setBlock(WorldServer world, int x, int a, Block block) {
        int[] position = position(x, a);
        world.setBlock(position[0], position[1], position[2], block, 0, 3);
    }

    private static void clear(WorldServer world, int x) {
        for (int dx = -7; dx <= 7; dx++) for (int dy = -1; dy <= 5; dy++)
            for (int dz = -5; dz <= 4; dz++) world.setBlockToAir(x + dx, Y + dy, Z + dz);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        serverChecks++;
    }
}
