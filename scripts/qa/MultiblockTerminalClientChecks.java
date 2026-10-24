package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.gui.GuiScreenConfigureChannels;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "multiblockterminalclientqa",
    name = "Multiblock Terminal Client QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MultiblockTerminalClientChecks {

    private boolean started;
    private volatile boolean equipped;
    private volatile boolean guiPrepared;
    private volatile TerminalInteractionClientChecks interaction;
    private volatile boolean nativeVerified;
    private volatile boolean disabledClicksRequested;
    private volatile boolean disabledVerified;
    private volatile boolean terminalRestored;
    private volatile boolean settingsVerified;
    private volatile boolean channelsVerified;
    private volatile boolean finished;
    private volatile int requestedOpen;
    private int opened;
    private int openDelay;
    private int ticks;
    private int serverTicks;
    private int stage = -4;
    private int frames;
    private int checks;
    private GuiContainerWrapper nativeScreen;
    private GuiContainerWrapper firstScreen;
    private GuiContainerWrapper secondScreen;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.terminal.qa") && !Boolean.getBoolean("gtng.terminal.serverQA"))
            FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("terminal-qa-" + System.currentTimeMillis(), "Multiblock Terminal QA",
                    new WorldSettings(74111L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (++ticks > 3600) throw new AssertionError("multiblock terminal client QA timed out at stage " + stage);
            if (interaction != null && !interaction.done()) interaction.client(mc);
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
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (!equipped) {
                if (++serverTicks < 60) return;
                int verified = new MultiblockTerminalServerChecks().verify(server);
                System.out.println("MULTIBLOCK_TERMINAL_INTEGRATED_SERVER_QA: PASS (" + verified + " checks)");
                interaction = new TerminalInteractionClientChecks();
                equipped = true;
                return;
            }
            if (!interaction.done()) {
                interaction.server(server, player);
                return;
            }
            if (!guiPrepared) {
                ItemStack terminal = MultiblockTerminalServerChecks.terminal();
                TerminalSettings.setCount(terminal, 7);
                ChannelDataAccessor.setChannelData(terminal, "qa_server_channel", 3);
                player.inventory.currentItem = 0;
                player.inventory.mainInventory[0] = terminal;
                MultiblockTerminalServerChecks.projectionFixture(player.worldObj, 128, 80, 128);
                player.inventoryContainer.detectAndSendChanges();
                player.playerNetServerHandler.setPlayerLocation(128.5, 85, 120.5, 0, 0);
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                guiPrepared = true;
                requestedOpen = 1;
                return;
            }
            ItemStack terminal = player.getHeldItem();
            if (!nativeVerified && !TerminalSettings.fullBuild(terminal)) {
                ItemStack restored = ItemStack.loadItemStackFromNBT(terminal.writeToNBT(new NBTTagCompound()));
                require(!TerminalSettings.fullBuild(restored),
                    "real GUI native-building selection persists in the server item NBT");
                nativeVerified = true;
            }
            if (disabledClicksRequested && !disabledVerified && TerminalSettings.count(terminal) == 8) {
                require(
                    !TerminalSettings.fullBuild(terminal) && !TerminalSettings.dismantle(terminal)
                        && TerminalSettings.noHatches(terminal)
                        && !TerminalSettings.replace(terminal)
                        && !TerminalSettings.mirror(terminal)
                        && !TerminalSettings.ae(terminal),
                    "disabled advanced buttons leave server settings unchanged in native build mode");
                disabledVerified = true;
            }
            if (
                !terminalRestored && disabledVerified
                    && TerminalSettings.fullBuild(terminal)
                    && TerminalSettings.count(terminal) == 7
            ) {
                require(
                    TerminalSettings.noHatches(terminal) && !TerminalSettings.replace(terminal)
                        && !TerminalSettings.mirror(terminal)
                        && !TerminalSettings.ae(terminal),
                    "restoring whole-structure building preserves the advanced settings");
                terminalRestored = true;
            }
            if (
                TerminalSettings.count(terminal) == 648 && TerminalSettings.dismantle(terminal)
                    && TerminalSettings.fullBuild(terminal)
                    && !TerminalSettings.noHatches(terminal)
                    && TerminalSettings.replace(terminal)
                    && TerminalSettings.mirror(terminal)
                    && TerminalSettings.ae(terminal)
            ) settingsVerified = true;
            if (
                !channelsVerified && ChannelDataAccessor.hasSubChannel(terminal, "qa_client_channel")
                    && ChannelDataAccessor.getChannelData(terminal, "qa_client_channel") == 77
            ) {
                require(settingsVerified && TerminalSettings.count(terminal) == 648,
                    "native channel upload retains server-authoritative terminal settings");
                channelsVerified = true;
            }
            if (requestedOpen > opened && (requestedOpen < 4 || channelsVerified)) {
                if (++openDelay < 10) return;
                openDelay = 0;
                opened = requestedOpen;
                terminal.getItem().onItemRightClick(terminal, player.worldObj, player);
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !guiPrepared) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null) return;
            if (++frames < 20) return;
            if (stage == -4 && mc.currentScreen instanceof GuiContainerWrapper wrapper) {
                nativeScreen = wrapper;
                require(
                    value(wrapper.getScreen(), "full_build").getIntValue() == 1
                        && value(wrapper.getScreen(), "mode").getIntValue() == 0,
                    "main GUI defaults to whole-structure building");
                click(wrapper.getScreen(), "full_build");
                advance();
            } else if (
                stage == -3 && nativeVerified
                    && mc.currentScreen instanceof GuiContainerWrapper wrapper
                    && value(wrapper.getScreen(), "full_build").getIntValue() == 0
            ) {
                ModularScreen screen = wrapper.getScreen();
                click(screen, "no_hatches");
                click(screen, "replace");
                click(screen, "mirror");
                click(screen, "ae");
                editCount(screen, 8);
                disabledClicksRequested = true;
                advance();
            } else if (
                stage == -2 && disabledVerified
                    && mc.currentScreen instanceof GuiContainerWrapper wrapper
                    && value(wrapper.getScreen(), "count").getIntValue() == 8
            ) {
                mc.thePlayer.closeScreen();
                requestedOpen = 2;
                advance();
            } else
                if (stage == -1 && mc.currentScreen instanceof GuiContainerWrapper wrapper && wrapper != nativeScreen) {
                    ModularScreen screen = wrapper.getScreen();
                    require(
                        value(screen, "full_build").getIntValue() == 0 && value(screen, "count").getIntValue() == 8
                            && value(screen, "no_hatches").getIntValue() == 1
                            && value(screen, "replace").getIntValue() == 0
                            && value(screen, "mirror").getIntValue() == 0
                            && value(screen, "ae").getIntValue() == 0,
                        "closing and reopening retains native building and unchanged advanced settings");
                    capture("terminal-native-builder");
                    click(screen, "full_build");
                    editCount(screen, 7);
                    advance();
                } else if (stage == 0 && terminalRestored && mc.currentScreen instanceof GuiContainerWrapper wrapper) {
                    if (
                        !(mc.theWorld.getTileEntity(128, 80,
                            128) instanceof MultiblockTerminalServerChecks.FixtureController controller)
                    ) return;
                    firstScreen = wrapper;
                    ModularScreen screen = wrapper.getScreen();
                    require(value(screen, "mode").getIntValue() == 0 && value(screen, "count").getIntValue() == 7,
                        "initial GUI receives server mode and master channel");
                    ItemStack terminal = mc.thePlayer.getHeldItem();
                    int previousCalls = controller.constructCalls();
                    terminal.getItem().onItemUseFirst(terminal, mc.thePlayer, mc.theWorld, 128, 80, 128, 2, 0.5f, 0.5f,
                        0.5f);
                    require(controller.constructCalls() == previousCalls + 1 && controller.lastTriggerCount() == 7,
                        "ordinary controller use retains real client StructureLib projection with the virtual master channel");
                    require(controller.lastChannel() == 3, "ordinary projection retains native subchannels");
                    capture("terminal-build");
                    click(screen, "mode");
                    click(screen, "no_hatches");
                    click(screen, "replace");
                    click(screen, "mirror");
                    click(screen, "ae");
                    editCount(screen, 648);
                    advance();
                } else if (stage == 1 && settingsVerified && mc.currentScreen instanceof GuiContainerWrapper wrapper) {
                    require(
                        value(wrapper.getScreen(), "mode").getIntValue() == 1
                            && value(wrapper.getScreen(), "count").getIntValue() == 648,
                        "real GUI actions synchronize dismantle mode and master channel");
                    capture("terminal-dismantle");
                    mc.thePlayer.closeScreen();
                    requestedOpen = 3;
                    advance();
                } else if (
                    stage == 2 && mc.currentScreen instanceof GuiContainerWrapper wrapper && wrapper != firstScreen
                ) {
                    secondScreen = wrapper;
                    ModularScreen screen = wrapper.getScreen();
                    require(
                        value(screen, "mode").getIntValue() == 1 && value(screen, "no_hatches").getIntValue() == 0
                            && value(screen, "replace").getIntValue() == 1
                            && value(screen, "mirror").getIntValue() == 1
                            && value(screen, "ae").getIntValue() == 1
                            && value(screen, "full_build").getIntValue() == 1
                            && value(screen, "count").getIntValue() == 648,
                        "closing and reopening preserves every terminal setting");
                    capture("terminal-reopened");
                    click(screen, "projection_channels");
                    advance();
                } else if (stage == 3 && mc.currentScreen instanceof GuiScreenConfigureChannels channels) {
                    require(ChannelDataAccessor.getChannelData(mc.thePlayer.getHeldItem(), "qa_server_channel") == 3,
                        "original channel GUI sees existing native subchannels");
                    textField(channels, "key").setText("qa_client_channel");
                    textField(channels, "value").setText("77");
                    channels.doActionPerformed(channels.getButtonList().get(0));
                    require(ChannelDataAccessor.getChannelData(mc.thePlayer.getHeldItem(), "qa_client_channel") == 77,
                        "original channel GUI can add an arbitrary subchannel");
                    capture("terminal-native-channels");
                    mc.thePlayer.closeScreen();
                    requestedOpen = 4;
                    advance();
                } else if (
                    stage == 4 && channelsVerified
                        && mc.currentScreen instanceof GuiContainerWrapper wrapper
                        && wrapper != secondScreen
                ) {
                    require(
                        value(wrapper.getScreen(), "mode").getIntValue() == 1
                            && value(wrapper.getScreen(), "count").getIntValue() == 648,
                        "native channel editing preserves terminal mode on reopening");
                    capture("terminal-after-channels");
                    click(wrapper.getScreen(), "projection_registries");
                    advance();
                } else if (stage == 5 && mc.currentScreen instanceof GuiConfig) {
                    require(true, "original StructureLib material-source configuration remains accessible");
                    capture("terminal-native-registries");
                    writeResult("PASS");
                    System.out.println("MULTIBLOCK_TERMINAL_CLIENT_QA: PASS (" + checks + " checks)");
                    finished = true;
                }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void editCount(ModularScreen screen, int amount) {
        TextFieldWidget field = (TextFieldWidget) named(screen.getMainPanel(), "terminal_count");
        require(field != null, "master channel text field exists");
        field.setText(Integer.toString(amount));
        field.onRemoveFocus(screen.getContext());
    }

    private static IntSyncValue value(ModularScreen screen, String name) {
        return screen.getSyncManager().getMainPSM().findSyncHandler(name, IntSyncValue.class);
    }

    private static void click(ModularScreen screen, String name) {
        IWidget widget = named(screen.getMainPanel(), name);
        if (!(widget instanceof ButtonWidget<?>button)) throw new AssertionError("missing terminal button " + name);
        button.onMousePressed(0);
    }

    private static IWidget named(IWidget root, String name) {
        if (name.equals(root.getName())) return root;
        for (IWidget child : root.getChildren()) {
            IWidget found = named(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static GuiTextField textField(GuiScreenConfigureChannels screen, String name) throws Exception {
        Field field = GuiScreenConfigureChannels.class.getDeclaredField(name);
        field.setAccessible(true);
        return (GuiTextField) field.get(screen);
    }

    private void advance() {
        stage++;
        frames = 0;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File(System.getProperty("gtng.terminal.qa.output")), name + ".png",
            mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void writeResult(String value) throws Exception {
        Files.write(Path.of(System.getProperty("gtng.terminal.qa.output"), "result.txt"),
            value.getBytes(StandardCharsets.UTF_8));
    }

    private static void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            writeResult("FAIL: " + failure);
        } catch (Exception writeFailure) {
            failure.addSuppressed(writeFailure);
        }
        FMLCommonHandler.instance().exitJava(1, false);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
