package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mod(
    modid = "modernmodeqa",
    name = "Modern Mode QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class ModernMachineModeClientChecks {

    private static final GTNGMachineID[] IDS = { GTNGMachineID.PrecisionAssembler,
        GTNGMachineID.IntegratedChemicalPlant, GTNGMachineID.FieldExtruderFactory, GTNGMachineID.MageAssembler,
        GTNGMachineID.SuperconductingElectromagneticFactory, GTNGMachineID.NyarlathotepsTentacle,
        GTNGMachineID.NexusSatelliteFactoryMkI, GTNGMachineID.NexusSatelliteFactoryMkII,
        GTNGMachineID.NexusSatelliteFactoryMkIII, GTNGMachineID.NexusSatelliteFactoryMkIV };
    private static final int[] COUNTS = { 2, 3, 2, 3, 5, 2, 10, 9, 11, 6 };
    private final MTEMultiBlockBase[] machines = new MTEMultiBlockBase[IDS.length];
    private final BaseMetaTileEntity[] tiles = new BaseMetaTileEntity[IDS.length];
    private final StringBuilder report = new StringBuilder();
    private volatile int requested = -1, opened = -1;
    private int ticks, serverTicks, openAt, opening = -1, stage, frames, scenario;
    private boolean started, finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++ticks < 7200, "client timeout at scenario " + scenario + " stage " + stage);
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 2;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.gameSettings.guiScale = 2;
                mc.launchIntegratedServer("modern-modes-" + System.currentTimeMillis(), "Modern Mode QA",
                    new WorldSettings(733L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            serverTicks++;
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (machines[0] == null) {
                for (int i = 0; i < IDS.length; i++) {
                    player.worldObj.setBlock(i * 3, 12, 0, GregTechAPI.sBlockMachines, 0, 3);
                    tiles[i] = (BaseMetaTileEntity) player.worldObj.getTileEntity(i * 3, 12, 0);
                    tiles[i].setInitialValuesAsNBT(null, (short) IDS[i].id);
                    tiles[i].setOwnerName(player.getCommandSenderName());
                    tiles[i].setOwnerUuid(player.getUniqueID());
                    tiles[i].disableWorking();
                    machines[i] = (MTEMultiBlockBase) tiles[i].getMetaTileEntity();
                    machines[i].setMachineMode(0);
                    require(machines[i].getAvailableRecipeMaps().size() == COUNTS[i], "mode count " + IDS[i]);
                    int modeIndex = 0;
                    for (var map : machines[i].getAvailableRecipeMaps()) {
                        machines[i].setMachineMode(modeIndex++);
                        require(machines[i].getRecipeMap() == map, "list order matches actual mode " + IDS[i]);
                    }
                    machines[i].setMachineMode(0);
                }
                requested = 0;
            }
            if (requested >= 0 && serverTicks >= 40) {
                opening = requested;
                requested = -1;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(opening * 3 + 0.5, 13, -3, 0, 0);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                openAt = serverTicks + 30;
            }
            if (opening >= 0 && serverTicks >= openAt) {
                int i = opening;
                opening = -1;
                machines[i].onRightclick(tiles[i], player);
                opened = i;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || opened < 0) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 20) return;
            var screen = wrapper.getScreen();
            String prefix = opened >= 6 ? "satellite" : "gtng";
            ButtonWidget<?> button = (ButtonWidget<?>) named(screen.getMainPanel(), prefix + "ModeSelector");
            require(button != null, "selector exists for " + IDS[opened]);
            IntSyncValue mode = screen.getSyncManager().getMainPSM().findSyncHandler("machineMode", IntSyncValue.class);
            ModularPanel popup = screen.getPanelManager().getOpenPanels().stream()
                .filter(panel -> panel.getName().equals(prefix + "MachineModes")).findFirst().orElse(null);
            if (stage == 0) {
                require(mode.getIntValue() == 0, "initial mode unchanged");
                require(button.getArea().width == 16 && button.getArea().height == 16, "16 pixel mode button");
                capture("main");
                button.onMousePressed(0);
            } else if (stage == 1) {
                if (popup == null) return;
                ListWidget<?, ?> list = (ListWidget<?, ?>) named(popup, prefix + "ModeList");
                require(list.getChildren().size() == COUNTS[opened], "all modes listed");
                capture("list");
                list.getScrollData().scrollTo(list.getScrollArea(), Integer.MAX_VALUE);
            } else if (stage == 2) {
                if (popup == null) return;
                ButtonWidget<?> last = (ButtonWidget<?>) named(popup, prefix + "Mode:" + (COUNTS[opened] - 1));
                ListWidget<?, ?> list = (ListWidget<?, ?>) named(popup, prefix + "ModeList");
                require(last.getArea().ey() - list.getScrollData().getScroll() <= list.getArea().ey(),
                    "last row visible");
                last.onMousePressed(0);
            } else if (stage == 3) {
                if (mode.getIntValue() != COUNTS[opened] - 1 || machines[opened].getMachineMode() != mode.getIntValue())
                    return;
                require(popup != null, "ordinary selection keeps popup open");
                capture("selected");
                ListWidget<?, ?> list = (ListWidget<?, ?>) named(popup, prefix + "ModeList");
                list.getScrollData().scrollTo(list.getScrollArea(), 0);
            } else if (stage == 4) {
                shiftClick((ButtonWidget<?>) named(popup, prefix + "Mode:0"));
            } else if (stage == 5) {
                if (popup != null || mode.getIntValue() != 0 || machines[opened].getMachineMode() != 0) return;
                button.onMousePressed(0);
            } else if (stage == 6) {
                if (popup == null) return;
                require(mode.getIntValue() == 0, "selection survives popup reopening");
                shiftClick((ButtonWidget<?>) named(popup, prefix + "Mode:0"));
            } else if (stage == 7) {
                if (popup != null) return;
                require(mode.getIntValue() == 0, "shift clicking current mode closes without cycling");
                int type = opened;
                opened = -1;
                requested = type;
            } else if (stage == 8) {
                require(mode.getIntValue() == 0 && machines[opened].getMachineMode() == 0,
                    "full GUI reopening preserves mode");
                report.append("PASS ").append(IDS[opened]).append(" scale ").append(mc.gameSettings.guiScale).append(
                    ": ordered mode list, scroll, ordinary click stays open, Shift selection and same-mode close, server/client sync, reopen.\n");
                Files.write(new File(output(), "progress.txt").toPath(),
                    report.toString().getBytes(StandardCharsets.UTF_8));
                if (++scenario >= IDS.length + 2) {
                    finished = true;
                    Files.write(new File(output(), "result.txt").toPath(),
                        ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                    mc.shutdown();
                    return;
                }
                mc.gameSettings.guiScale = scenario < IDS.length ? 2 : scenario == IDS.length ? 1 : 3;
                requested = scenario < IDS.length ? scenario : 8;
                opened = -1;
                stage = -1;
            }
            stage++;
            frames = 0;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void shiftClick(ButtonWidget<?> button) throws Exception {
        var field = Class.forName("org.lwjglx.input.Keyboard").getField("sdlKeyPressedArray");
        ByteBuffer previous = (ByteBuffer) field.get(null);
        ByteBuffer keys = ByteBuffer.allocate(previous.limit());
        keys.put(previous.duplicate()).rewind();
        int shift = (Integer) Class.forName("org.lwjglx.input.KeyCodes").getMethod("lwjglToSdlScancode", int.class)
            .invoke(null, Keyboard.KEY_LSHIFT);
        try {
            keys.put(shift, (byte) 1);
            field.set(null, keys);
            require(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT), "QA Shift key reaches the input API");
            button.onMousePressed(0);
        } finally {
            field.set(null, previous);
        }
    }

    private static IWidget named(IWidget root, String name) {
        if (name.equals(root.getName())) return root;
        for (IWidget child : root.getChildren()) {
            IWidget found = named(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void capture(String suffix) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(),
            IDS[opened] + "-scale" + mc.gameSettings.guiScale + "-" + suffix + ".png", mc.displayWidth,
            mc.displayHeight, mc.getFramebuffer());
    }

    private static File output() {
        File dir = new File(System.getProperty("gtng.satelliteModes.output"));
        dir.mkdirs();
        return dir;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
