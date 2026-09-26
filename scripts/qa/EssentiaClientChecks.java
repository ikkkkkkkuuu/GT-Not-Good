package com.xyp.gtnotgood.common.machines.basicMachine;

import java.io.File;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import appeng.api.AEApi;
import appeng.api.storage.IMEMonitor;
import appeng.tile.storage.TileDrive;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/** Disposable integration checks for GT processing, real ME storage and the native machine screen. */
@Mod(modid = "essentiaqa", name = "Essentia QA", version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class EssentiaClientChecks {
    private boolean started;
    private volatile boolean finished;
    private int ticks, frames;
    private BaseMetaTileEntity base;
    private EssentiaDisassembler machine;
    private TileDrive drive;
    private ItemStack cell;
    private AspectList expected;
    private IMEMonitor<AEEssentiaStack> monitor;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) { FMLCommonHandler.instance().bus().register(this); }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("essentia-qa-" + System.currentTimeMillis(), "Essentia QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        if (++ticks == 1) {
            world.setBlock(4, 8, 4, GregTechAPI.sBlockMachines, 0, 3);
            base = (BaseMetaTileEntity) world.getTileEntity(4, 8, 4);
            base.setInitialValuesAsNBT(null, (short) GTNGMachineID.ESSENTIA_DISASSEMBLER.ID);
            base.setFrontFacing(net.minecraftforge.common.util.ForgeDirection.NORTH);
            machine = (EssentiaDisassembler) base.getMetaTileEntity();
            var blocks = AEApi.instance().definitions().blocks();
            world.setBlock(3, 8, 4, blocks.energyCellCreative().maybeBlock().get());
            world.setBlock(3, 8, 5, blocks.drive().maybeBlock().get());
            drive = (TileDrive) world.getTileEntity(3, 8, 5);
            cell = ThEApi.instance().items().EssentiaCell_64k.getStack();
            drive.getInternalInventory().setInventorySlotContents(0, cell);
            player.playerNetServerHandler.setPlayerLocation(4.5, 9, 2, 0, 15);
            player.capabilities.isFlying = true;
            Config.recipeSpeedMode = 0;
            require(EssentiaDisassembler.processingDuration() == 20, "base duration");
            Config.recipeSpeedMode = 2;
            Config.recipeSpeedMultiplier = .1F;
            require(EssentiaDisassembler.processingDuration() == 2, "multiplier duration");
            Config.recipeSpeedMode = 1;
            Config.recipeSpeedFixedDuration = 1;
            require(EssentiaDisassembler.processingDuration() == 1, "fixed duration");
            expected = EssentiaDisassembler.getEssentia(new ItemStack(Items.stick));
            require(expected.size() > 0, "native stick aspects");
        }
        base.setStoredEU(100000);
        if (ticks == 80) {
            require(machine.getProxy().isActive(), "active real network");
            monitor = (IMEMonitor<AEEssentiaStack>) machine.getProxy().getStorage().getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
            base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.stick, 32));
        }
        if (ticks == 120) {
            require(base.getStackInSlot(machine.getInputSlot()) == null, "32 items processed in 40 ticks");
            for (Aspect aspect : expected.getAspects()) require(amount(aspect) == expected.getAmount(aspect) * 32L, "exact aspect yield");
            cell = drive.getInternalInventory().getStackInSlot(0);
            drive.getInternalInventory().setInventorySlotContents(0, null);
        }
        if (ticks == 130) base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.stick, 2));
        if (ticks == 160) {
            require(base.getStackInSlot(machine.getInputSlot()).stackSize == 2, "no storage does not consume input");
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            require(tag.hasKey("EssentiaBatch"), "persistent batch tag");
            NBTTagCompound pending = new NBTTagCompound();
            new AspectList().add(Aspect.AIR, 3).writeToNBT(pending);
            pending.setBoolean("Finished", true);
            tag.setTag("EssentiaBatch", pending);
            // Content round-trip on the live tile must not reload an already joined AE node.
            tag.removeTag("essentia_proxy");
            machine.loadNBTData(tag);
            NBTTagCompound dropped = new NBTTagCompound();
            machine.setItemNBT(dropped);
            AspectList retained = new AspectList();
            retained.readFromNBT(dropped.getCompoundTag("EssentiaBatch"));
            require(retained.getAmount(Aspect.AIR) == 3, "wrench item retains pending batch");
            machine.saveNBTData(tag);
            tag.removeTag("essentia_proxy");
            machine.loadNBTData(tag);
            drive.getInternalInventory().setInventorySlotContents(0, cell);
        }
        if (ticks == 200) {
            require(base.getStackInSlot(machine.getInputSlot()) == null, "resume after storage returns");
            for (Aspect aspect : expected.getAspects()) require(amount(aspect) == expected.getAmount(aspect) * 34L, "no duplicate output");
            require(amount(Aspect.AIR) == 3, "saved pending batch delivered exactly once");
            machine.onRightclick(base, player);
            System.out.println("ESSENTIA_QA: PASS native yield, 20/2/1 tick configuration, 32 consecutive crafts, full storage pause, recovery, exact outputs");
            finished = true;
        }
    }

    private long amount(Aspect aspect) {
        var stack = monitor.getStorageList().findPrecise(new AEEssentiaStack(aspect, 1));
        return stack == null ? 0 : stack.getStackSize();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("ESSENTIA_QA: " + message);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (!finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (++frames == 60) {
            require(mc.currentScreen != null, "native GUI open");
            ScreenShotHelper.saveScreenshot(new File("."), "essentia-qa.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            Files.write(new File("essentia-qa-result.txt").toPath(), "PASS".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }
}
