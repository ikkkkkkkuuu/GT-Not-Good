package com.xyp.gtnotgood.client.gui;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.LDLibModularScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Opt-in real client and integrated-server checks in a disposable creative world. */
@Mod(
    modid = "ldlibminerqa",
    name = "LDLib Miner QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class LDLibMinerClientChecks {

    private boolean started;
    private boolean openedFromPrimary;
    private volatile LargeVoidMiner miner;
    private int serverTicks, clientTicks, frames, stage;
    private volatile int requested;
    private volatile int verified;
    private boolean initialEnabled;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.ldminer.qa")) {
            com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (++clientTicks > 3600) throw new AssertionError("LDLIB_MINER_QA_TIMEOUT stage=" + stage);
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "ldlib-miner-qa-" + System.currentTimeMillis(),
                "LDLib Miner QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP viewer = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (miner == null) {
            var world = server.worldServerForDimension(0);
            world.setBlock(0, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(0, 6, 0);
            base.setInitialValuesAsNBT(null, (short) GTNGMachineID.LARGE_VOID_MINER.ID);
            base.setOwnerName(viewer.getCommandSenderName());
            base.setOwnerUuid(viewer.getUniqueID());
            miner = (LargeVoidMiner) base.getMetaTileEntity();
            miner.forceRefreshPool();
            initialEnabled = base.isAllowedToWork();
            viewer.playerNetServerHandler.setPlayerLocation(3, 7, -3, 40, 15);
            viewer.capabilities.isFlying = true;
            viewer.sendPlayerAbilities();
        }
        if (++serverTicks == 60) {
            miner.onRightclick(miner.getBaseMetaTileEntity(), viewer);
            System.out.println(
                "LDLIB_MINER_QA opened; ores=" + miner.getOreEntries()
                    .size());
        }
        if (requested == 1 && miner.mOreMode == 1) verified = 1;
        if (requested == 2 && miner.mFortuneLevel == 5) verified = 2;
        if (requested == 3 && miner.getDirectionalMode()) {
            initialEnabled = miner.getBaseMetaTileEntity()
                .isAllowedToWork();
            verified = 3;
        }
        if (requested == 4 && miner.getBaseMetaTileEntity()
            .isAllowedToWork() != initialEnabled) verified = 4;
        if (requested == 5 && miner.getOreEntries()
            .stream()
            .anyMatch(ore -> ore.aimed)) verified = 5;
        if (requested == 6 && miner.getOreEntries()
            .stream()
            .noneMatch(ore -> ore.aimed)) verified = 6;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
        if (!(wrapper.getScreen() instanceof LDLibModularScreen screen)) {
            if (!openedFromPrimary && ++frames >= 60) {
                capture("primary");
                var button = configButton(
                    wrapper.getScreen()
                        .getMainPanel());
                if (button == null) throw new AssertionError("Missing original configuration button");
                button.onMousePressed(0);
                openedFromPrimary = true;
                frames = 0;
            }
            return;
        }
        if (!openedFromPrimary) throw new AssertionError("Right click bypassed the original primary GUI");
        if (++frames < 60 || requested != verified) return;
        frames = 0;
        switch (stage++) {
            case 0:
                capture("overview");
                click(screen, 50, 36);
                requested = 1;
                break;
            case 1:
                click(screen, 170, 36);
                requested = 2;
                break;
            case 2:
                click(screen, 50, 194);
                requested = 3;
                break;
            case 3:
                click(screen, 140, 194);
                requested = 4;
                break;
            case 4:
                capture("changed");
                click(screen, 415, 60);
                break;
            case 5:
                capture("dialog");
                screen.onKeyPressed((char) 27, org.lwjgl.input.Keyboard.KEY_ESCAPE);
                break;
            case 6:
                click(screen, 200, 100);
                break;
            case 7:
                capture("ores");
                click(screen, 425, 120);
                requested = 5;
                break;
            case 8:
                capture("ore-selected");
                click(screen, 250, 80);
                for (char ch : "Tantalite".toCharArray()) screen.onKeyPressed(ch, 0);
                break;
            case 9:
                capture("search");

                click(screen, 415, 60);
                break;
            case 10:
                click(screen, 180, 158);
                requested = 6;
                break;
            case 11:
                click(screen, 250, 80);
                screen.onMousePressed(1);
                screen.onMouseRelease(1);
                click(screen, 400, 80);
                break;
            case 12:
                if (oreRows(screen).getItems()
                    .size() != 64) throw new AssertionError("Unselected category");
                click(screen, 400, 80);
                break;
            case 13:
                if (!oreRows(screen).getItems()
                    .isEmpty()) throw new AssertionError("Selected category after clear");
                capture("empty-category");
                click(screen, 400, 80);
                break;
            case 14:
                checkOrder(screen, true);
                capture("ascending");
                click(screen, 400, 80);
                break;
            case 15:
                checkOrder(screen, false);
                click(screen, 250, 120);
                for (int i = 0; i < 100; i++) screen.onMouseScroll(com.cleanroommc.modularui.api.UpOrDown.DOWN, 1);
                break;
            case 16:
                capture("scrolled-descending");
                System.out.println(
                    "LDLIB_MINER_QA_PASS primary config entry; mode fortune directional power selection clear; categories and weight ordering");
                mc.shutdown();
                break;
            default:
                throw new AssertionError("Unexpected QA stage");
        }
    }

    @SuppressWarnings("unchecked")
    private static com.xyp.ldlib.gui.ui.elements.VirtualScrollerView<com.xyp.gtnotgood.common.api.gui.OreEntryInfo> oreRows(
        LDLibModularScreen screen) {
        try {
            var controls = LDLibModularScreen.class.getDeclaredField("controls");
            controls.setAccessible(true);
            var view = controls.get(screen);
            var rows = LDLibVoidMinerView.class.getDeclaredField("rows");
            rows.setAccessible(true);
            return (com.xyp.ldlib.gui.ui.elements.VirtualScrollerView<com.xyp.gtnotgood.common.api.gui.OreEntryInfo>) rows
                .get(view);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void checkOrder(LDLibModularScreen screen, boolean ascending) {
        var items = oreRows(screen).getItems();
        if (items.size() != 64) throw new AssertionError("Sort lost ores");
        for (int i = 1; i < items.size(); i++) {
            int comparison = Float.compare(items.get(i - 1).weight, items.get(i).weight);
            if (ascending ? comparison > 0 : comparison < 0) throw new AssertionError("Incorrect weight ordering");
        }
    }

    private static com.cleanroommc.modularui.widgets.ButtonWidget<?> configButton(
        com.cleanroommc.modularui.api.widget.IWidget widget) {
        if (widget instanceof com.cleanroommc.modularui.widgets.ButtonWidget<?>button && button.isSynced()
            && button.getSyncHandler()
                .getKey()
                .startsWith("gtng.vm.openConfig"))
            return button;
        for (var child : widget.getChildren()) {
            var found = configButton(child);
            if (found != null) return found;
        }
        return null;
    }

    private static void click(LDLibModularScreen screen, int x, int y) {
        var area = screen.getMainPanel()
            .getArea();
        screen.getContext()
            .updateState(area.x + x, area.y + y, 0);
        screen.onMousePressed(0);
        screen.onMouseRelease(0);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.ldminer.qa.output"));
        output.mkdirs();
        ScreenShotHelper.saveScreenshot(output, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }
}
