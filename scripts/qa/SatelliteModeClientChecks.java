package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.machines.multiblock.LightHunterSpaceStation;
import com.xyp.gtnotgood.common.machines.multiblock.NexusSatelliteFactory;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
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
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@Mod(
    modid = "satellitemodeqa",
    name = "Satellite Mode QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class SatelliteModeClientChecks {

    private final NexusSatelliteFactory[] machines = new NexusSatelliteFactory[4];
    private final BaseMetaTileEntity[] tiles = new BaseMetaTileEntity[4];
    private final int[] modeCounts = { 10, 9, 11, 6 };
    private final StringBuilder report = new StringBuilder();
    private volatile int requested = -1, opened = -1;
    private boolean started, finished;
    private int ticks, serverTicks, stage, frames, scenario;
    private int openingType = -1, openAt;
    private int originalParallel;
    private boolean originalAlwaysMax, originalPowerfail;
    private volatile boolean hoverRequested, hoverReady;
    private volatile boolean clearOutputsRequested;
    private boolean outputPreviewReady;
    private LightHunterSpaceStation station;
    private BaseMetaTileEntity stationTile;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.satelliteModes.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++ticks < 7200, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 2;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI().clearChatMessages();
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.gameSettings.guiScale = 2;
                mc.launchIntegratedServer("satellite-mode-qa-" + System.currentTimeMillis(), "Satellite Mode QA",
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
            if (Boolean.getBoolean("gtng.stationWaila.qa")) {
                stationServer(player);
                return;
            }
            if (machines[0] == null) {
                GTNGMachineID[] ids = { GTNGMachineID.NexusSatelliteFactoryMkI, GTNGMachineID.NexusSatelliteFactoryMkII,
                    GTNGMachineID.NexusSatelliteFactoryMkIII, GTNGMachineID.NexusSatelliteFactoryMkIV };
                for (int type = 0; type < machines.length; type++) {
                    int x = type * 3;
                    player.worldObj.setBlock(x, 12, 0, GregTechAPI.sBlockMachines, 0, 3);
                    tiles[type] = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 12, 0);
                    tiles[type].setInitialValuesAsNBT(null, (short) ids[type].id);
                    tiles[type].setOwnerName(player.getCommandSenderName());
                    tiles[type].setOwnerUuid(player.getUniqueID());
                    tiles[type].disableWorking();
                    machines[type] = (NexusSatelliteFactory) tiles[type].getMetaTileEntity();
                    machines[type].setMachineMode(0);
                    require(machines[type].getAvailableRecipeMaps().size() == modeCounts[type], "mode count " + type);
                    checkWaila(machines[type], tiles[type], player);
                    NBTTagCompound preview = new NBTTagCompound();
                    machines[type].saveNBTData(preview);
                    NBTTagCompound outputs = new WirelessOutputs(
                        new ItemStack[] { new ItemStack(Items.diamond, 64), new ItemStack(Items.iron_ingot, 32),
                            new ItemStack(Items.gold_ingot, 16), new ItemStack(Items.coal, 8) },
                        new FluidStack[] { new FluidStack(FluidRegistry.WATER, 16000) }).save();
                    outputs.getTagList("items", 10).getCompoundTagAt(0).setString("amount", "18446744073709551616");
                    preview.setTag("lightHunterBatchOutputs", outputs);
                    preview.setBoolean("lightHunterOutputReady", true);
                    machines[type].loadNBTData(preview);
                }
                player.playerNetServerHandler.setPlayerLocation(0.5, 13, -3, 0, 0);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                requested = 0;
            }
            if (hoverRequested) {
                hoverRequested = false;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 2);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                hoverReady = true;
            }
            if (clearOutputsRequested) {
                clearOutputsRequested = false;
                NBTTagCompound cleared = new NBTTagCompound();
                machines[0].saveNBTData(cleared);
                cleared.removeTag("lightHunterBatchOutputs");
                cleared.setBoolean("lightHunterOutputReady", false);
                machines[0].loadNBTData(cleared);
            }
            if (requested >= 0 && serverTicks >= 40) {
                openingType = requested;
                requested = -1;
                player.closeScreen();
                openAt = serverTicks + 10;
            }
            if (openingType >= 0 && serverTicks >= openAt) {
                int type = openingType;
                openingType = -1;
                player.playerNetServerHandler.setPlayerLocation(type * 3 + 0.5, 13, -3, 0, 0);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                machines[type].onRightclick(tiles[type], player);
                opened = type;
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
            if (Boolean.getBoolean("gtng.stationWaila.qa")) {
                stationRender(mc);
                return;
            }
            if (stage == 15) {
                if (!hoverReady || mc.currentScreen != null || ++frames < 120) return;
                require(
                    mc.objectMouseOver != null && mc.objectMouseOver.blockX == 0
                        && mc.objectMouseOver.blockY == 12
                        && mc.objectMouseOver.blockZ == 0,
                    "camera points at satellite controller for real Waila hover");
                capture("waila");
                finished = true;
                Files.write(new File(output(), "result.txt").toPath(),
                    ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                mc.shutdown();
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 30) return;
            var screen = wrapper.getScreen();
            var sync = screen.getSyncManager().getMainPSM();
            if (sync.findSyncHandlerNullable("machineMode", 0) == null) return;
            GenericListSyncHandler<?> outputs = sync.findSyncHandler("lightHunterOutputs",
                GenericListSyncHandler.class);
            if (stage == 16) {
                if (!outputs.getValue().isEmpty()) return;
                IWidget outputRows = named(screen.getMainPanel(), "satelliteBatchOutputs");
                if (outputRows == null || !outputRows.getChildren().isEmpty()) return;
                capture("outputs-cleared");
                report.append("PASS output rows disappear after the server clears pending outputs.\n");
                mc.gameSettings.guiScale = 2;
                stage = 15;
                frames = 0;
                hoverRequested = true;
                return;
            }
            require(
                sync.findSyncHandlerNullable("lightHunterRecipes", 0) == null
                    && sync.findSyncHandlerNullable("lightHunterThreads", 0) == null,
                "main terminal uses native state without custom batch rows");
            if (stage >= 6) {
                checkBatchSettings(wrapper);
                return;
            }
            IntSyncValue mode = sync.findSyncHandler("machineMode", IntSyncValue.class);
            ButtonWidget<?> selector = (ButtonWidget<?>) named(screen.getMainPanel(), "satelliteModeSelector");
            require(selector != null, "selector button exists");
            if (stage == 0) {
                IWidget outputRows = named(screen.getMainPanel(), "satelliteBatchOutputs");
                if (outputs.getValue().size() != 5 || outputRows == null || outputRows.getChildren().size() != 5)
                    return;
                require(
                    ((NBTTagCompound) outputs.getValue().get(0)).getString("amount").equals("18446744073709551616")
                        && ((NBTTagCompound) outputs.getValue().get(4)).hasKey("fluid"),
                    "GUI synchronizes complete big quantities and fluid output");
                if (!outputPreviewReady) {
                    ListWidget<?, ?> terminal = (ListWidget<?, ?>) named(screen.getMainPanel(), "satelliteTerminal");
                    terminal.getScrollData().scrollTo(terminal.getScrollArea(), Integer.MAX_VALUE);
                    outputPreviewReady = true;
                    frames = 0;
                    return;
                }
                int before = mode.getIntValue();
                capture("main");
                selector.onMousePressed(0);
                require(mode.getIntValue() == before, "opening selector preserves mode");
                advance();
                return;
            }
            ModularPanel popup = screen.getPanelManager().getOpenPanels().stream()
                .filter(panel -> panel.getName().equals("satelliteMachineModes")).findFirst().orElse(null);
            if (stage == 4) {
                selector.onMousePressed(0);
                advance();
                return;
            }
            if (popup == null) return;
            if (stage == 1) {
                ListWidget<?, ?> list = (ListWidget<?, ?>) named(popup, "satelliteModeList");
                require(list != null, "scrollable mode list exists");
                for (int index = 0; index < modeCounts[opened]; index++)
                    require(named(popup, "satelliteMode:" + index) != null, "mode row " + index);
                require(named(popup, "satelliteMode:" + modeCounts[opened]) == null, "no extra mode rows");
                capture("first-modes");
                list.getScrollData().scrollTo(list.getScrollArea(), Integer.MAX_VALUE);
                advance();
            } else if (stage == 2) {
                ButtonWidget<?> last = (ButtonWidget<?>) named(popup, "satelliteMode:" + (modeCounts[opened] - 1));
                ListWidget<?, ?> list = (ListWidget<?, ?>) named(popup, "satelliteModeList");
                int scroll = list.getScrollData().getScroll();
                require(last.getArea().y() - scroll >= list.getArea().y()
                    && last.getArea().ey() - scroll <= list.getArea().ey(), "last mode visible after scrolling");
                last.onMousePressed(0);
                advance();
            } else if (stage == 3) {
                int last = modeCounts[opened] - 1;
                if (mode.getIntValue() != last || machines[opened].getMachineMode() != last) return;
                capture("selected-last-mode");
                popup.closeIfOpen();
                advance();
            } else if (stage == 5) {
                require(mode.getIntValue() == modeCounts[opened] - 1, "selection survives popup close and reopen");
                popup.closeIfOpen();
                advance();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void checkBatchSettings(GuiContainerWrapper wrapper) throws Exception {
        var screen = wrapper.getScreen();
        ButtonWidget<?> settings = (ButtonWidget<?>) named(screen.getMainPanel(), "satelliteBatchSettings");
        require(settings != null, "native parallel settings button exists");
        require(named(screen.getMainPanel(), "satelliteMinimumDuration") == null,
            "duration input removed from main terminal");
        if (stage == 6 || stage == 11 || stage == 13) {
            settings.onMousePressed(0);
            advance();
            return;
        }
        ModularPanel panel = screen.getPanelManager().getOpenPanels().stream()
            .filter(p -> p.getName().equals("powerPanel")).findFirst().orElse(null);
        if (panel == null) return;
        TextFieldWidget duration = (TextFieldWidget) named(panel, "satelliteMinimumDuration");
        require(duration != null, "duration input is inside native parallel settings panel");
        NexusSatelliteFactory machine = machines[opened];
        if (stage == 7) {
            require(fieldCount(panel) == 2, "native parallel input preserved alongside duration input");
            checkBounds(panel, panel);
            require(duration.getStringValue().getStringValue().equals(String.valueOf(machine.getMinimumDuration())),
                "opening settings displays saved duration");
            if (scenario < 4) require(machine.getMinimumDuration() == 20, "default duration remains 20 ticks");
            originalParallel = machine.getPowerPanelMaxParallel();
            originalAlwaysMax = machine.isAlwaysMaxParallel();
            originalPowerfail = machine.makesPowerfailEvents();
            capture("batch-settings");
            edit(duration, "0");
            advance();
        } else if (stage == 8) {
            if (machine.getMinimumDuration() != 1) return;
            require(duration.getStringValue().getStringValue().equals("1"), "zero input clamps to one tick");
            edit(duration, String.valueOf(Integer.MAX_VALUE));
            advance();
        } else if (stage == 9) {
            if (machine.getMinimumDuration() != Integer.MAX_VALUE) return;
            require(duration.getStringValue().getStringValue().equals(String.valueOf(Integer.MAX_VALUE)),
                "upper bound synchronizes without overflow");
            edit(duration, String.valueOf(37 + scenario));
            advance();
        } else if (stage == 10) {
            if (machine.getMinimumDuration() != 37 + scenario) return;
            require(machine.getPowerPanelMaxParallel() == originalParallel
                && machine.isAlwaysMaxParallel() == originalAlwaysMax
                && machine.makesPowerfailEvents() == originalPowerfail, "duration does not alter native settings");
            capture("batch-settings-edited");
            panel.closeIfOpen();
            advance();
        } else if (stage == 12) {
            require(duration.getStringValue().getStringValue().equals(String.valueOf(37 + scenario)),
                "duration survives settings close/reopen");
            int type = opened;
            opened = -1;
            requested = type;
            advance();
        } else if (stage == 14) {
            require(duration.getStringValue().getStringValue().equals(String.valueOf(37 + scenario)),
                "duration survives full controller GUI close/reopen");
            Minecraft mc = Minecraft.getMinecraft();
            report.append("PASS Mk").append(opened + 1).append(" GUI scale ").append(mc.gameSettings.guiScale)
                .append(": modes; batch settings placement, native parallel field, bounds 1..2147483647, ")
                .append("server synchronization, independent settings, popup and controller close/reopen.\n");
            panel.closeIfOpen();
            Files.write(new File(output(), "progress.txt").toPath(),
                report.toString().getBytes(StandardCharsets.UTF_8));
            if (++scenario >= 6) {
                stage = 16;
                frames = 0;
                clearOutputsRequested = true;
            } else {
                int next = scenario < 4 ? scenario : 0;
                mc.gameSettings.guiScale = scenario < 4 ? 2 : scenario == 4 ? 1 : 3;
                opened = -1;
                stage = frames = 0;
                outputPreviewReady = false;
                requested = next;
            }
        }
    }

    private void checkWaila(NexusSatelliteFactory machine, BaseMetaTileEntity tile, EntityPlayerMP player)
        throws Exception {
        NBTTagCompound original = new NBTTagCompound();
        machine.saveNBTData(original);
        boolean originallyActive = tile.isActive();
        NBTTagCompound fixture = (NBTTagCompound) original.copy();
        fixture.setInteger("lightHunterMinimumDuration", 73);
        fixture.setInteger("lightHunterBatchRecipes", 2);
        fixture.setLong("lightHunterBatchParallels", 274_877_906_816L);
        NBTTagCompound ledger = new WirelessOutputs(new ItemStack[] { new ItemStack(Items.diamond, 64) },
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 16000) }).save();
        ledger.getTagList("items", 10).getCompoundTagAt(0).setString("amount", "18446744073709551616");
        fixture.setTag("lightHunterBatchOutputs", ledger);
        try {
            machine.loadNBTData(fixture);
            require(machine.getMinimumDuration() == 73, "duration NBT key remains compatible");
            machine.mMaxProgresstime = 200;
            tile.setActive(true);
            NBTTagCompound active = wailaData(machine, tile, player);
            require(
                active.getInteger("satelliteRecipes") == 2 && active.getLong("satelliteParallels") == 274_877_906_816L,
                "Waila exact batch counters");
            require(active.getInteger("satelliteThreads") == 128 && active.getInteger("satelliteMinimumDuration") == 73,
                "Waila threads and minimum duration");
            require(active.getInteger("satelliteUsedThreads") == 128, "full parallel capacity uses all threads");
            require(!active.hasKey("satelliteOutputs") && !active.hasKey("satelliteMoreOutputs"),
                "Waila no longer sends output previews");
            require(
                machine.getBatchOutputRows().size() == 2
                    && machine.getBatchOutputRows().get(0).getString("amount").equals("18446744073709551616")
                    && machine.getBatchOutputRows().get(1).hasKey("fluid")
                    && machine.getBatchOutputRows().get(0).getInteger("ticks") == 200,
                "GUI output snapshot preserves large item quantities, fluids and working duration");
            machine.loadNBTData(original);
            List<String> lines = wailaBody(machine, tile, player, active);
            require(
                lines
                    .contains(StatCollector.translateToLocalFormatted("gtng.light_hunter.satellite.threads", 128, 128)),
                "Waila displays used / total from the server snapshot");
            require(
                lines.contains(StatCollector
                    .translateToLocalFormatted("gtng.light_hunter.satellite.minimum_duration_value", "73")),
                "Waila reads server data, not local state");
            require(
                lines.stream().noneMatch(line -> line.contains(new ItemStack(Items.diamond).getDisplayName()))
                    && lines.stream().noneMatch(line -> line.contains(FluidRegistry.WATER.getLocalizedName())),
                "Waila no longer displays item and fluid output names");
            long[] parallels = { 0, 1, Integer.MAX_VALUE, (long) Integer.MAX_VALUE + 1, Long.MAX_VALUE };
            int[] expectedThreads = { 0, 1, 1, 2, 128 };
            for (int i = 0; i < parallels.length; i++) {
                fixture.setLong("lightHunterBatchParallels", parallels[i]);
                machine.loadNBTData(fixture);
                machine.mMaxProgresstime = 200;
                tile.setActive(true);
                require(wailaData(machine, tile, player).getInteger("satelliteUsedThreads") == expectedThreads[i],
                    "thread usage rounding / saturation boundary " + parallels[i]);
            }
            tile.setActive(false);
            require(wailaData(machine, tile, player).getInteger("satelliteUsedThreads") == 0,
                "inactive machine reports zero used threads despite saved batch data");
            tile.setActive(true);
            fixture.setBoolean("lightHunterOutputReady", true);
            fixture.setTag("lightHunterBatchOutputs",
                new WirelessOutputs(new ItemStack[] { new ItemStack(Items.diamond), new ItemStack(Items.iron_ingot),
                    new ItemStack(Items.gold_ingot), new ItemStack(Items.coal) }, null).save());
            machine.loadNBTData(fixture);
            NBTTagCompound waiting = wailaData(machine, tile, player);
            require(waiting.getInteger("satelliteUsedThreads") == 0, "waiting outputs occupy no processing threads");
            require(machine.getBatchOutputRows().size() == 4, "GUI is not limited to three Waila previews");
            require(machine.getBatchOutputRows().get(0).getInteger("ticks") == 0,
                "waiting output has zero production rate");
            require(wailaBody(machine, tile, player, waiting)
                .contains(StatCollector.translateToLocal("gtng.modular.gui.waiting_output")), "Waila waiting status");
            fixture.removeTag("lightHunterBatchOutputs");
            fixture.setBoolean("lightHunterOutputReady", false);
            machine.loadNBTData(fixture);
            NBTTagCompound idle = wailaData(machine, tile, player);
            require(idle.getInteger("satelliteUsedThreads") == 0, "idle machine reports zero used threads");
            require(idle.getInteger("satelliteRecipes") == 0 && idle.getLong("satelliteParallels") == 0
                && machine.getBatchOutputRows().isEmpty(), "idle Waila hides stale batch counters");
        } finally {
            machine.loadNBTData(original);
            tile.setActive(originallyActive);
        }
        report.append(
            "PASS Waila: used/total threads, rounding and overflow bounds, inactive/idle/waiting zero usage, server snapshot, outputs.\n");
    }

    private static NBTTagCompound wailaData(MTEMultiBlockBase machine, BaseMetaTileEntity tile, EntityPlayerMP player) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.getWailaNBTData(player, tile, tag, player.worldObj, tile.xCoord, tile.yCoord, tile.zCoord);
        return tag;
    }

    private static List<String> wailaBody(MTEMultiBlockBase machine, BaseMetaTileEntity tile, EntityPlayerMP player,
        NBTTagCompound tag) {
        IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
            IWailaDataAccessor.class.getClassLoader(), new Class<?>[] { IWailaDataAccessor.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getNBTData" -> tag;
            case "getTileEntity" -> tile;
            case "getPlayer" -> player;
            case "getWorld" -> player.worldObj;
            default -> null;
            });
        IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
            IWailaConfigHandler.class.getClassLoader(), new Class<?>[] { IWailaConfigHandler.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getConfig" -> args.length > 1 ? args[1] : true;
            case "getModuleNames" -> Collections.emptySet();
            case "getConfigKeys" -> new HashMap<>();
            default -> null;
            });
        List<String> lines = new ArrayList<>();
        machine.getWailaBody(null, lines, accessor, config);
        return lines;
    }

    private static void edit(TextFieldWidget field, String text) {
        field.setText(text);
        field.onRemoveFocus(field.getContext());
    }

    private static int fieldCount(IWidget root) {
        int count = root instanceof TextFieldWidget ? 1 : 0;
        for (IWidget child : root.getChildren()) count += fieldCount(child);
        return count;
    }

    private static void checkBounds(IWidget root, ModularPanel panel) {
        if (root instanceof TextFieldWidget) {
            require(root.getArea().y() >= panel.getArea().y() && root.getArea().ey() <= panel.getArea().ey(),
                "settings input vertically inside panel");
            require(root.getArea().x() >= panel.getArea().x() && root.getArea().ex() <= panel.getArea().ex(),
                "settings input horizontally inside panel");
        }
        for (IWidget child : root.getChildren()) checkBounds(child, panel);
    }

    private void advance() {
        stage++;
        frames = 0;
    }

    private void stationServer(EntityPlayerMP player) {
        if (station == null) {
            player.worldObj.setBlock(0, 12, 0, GregTechAPI.sBlockMachines, 0, 3);
            stationTile = (BaseMetaTileEntity) player.worldObj.getTileEntity(0, 12, 0);
            stationTile.setInitialValuesAsNBT(null, (short) GTNGMachineID.LightHunterSpaceStation.id);
            stationTile.setOwnerName(player.getCommandSenderName());
            stationTile.setOwnerUuid(player.getUniqueID());
            stationTile.setFrontFacing(ForgeDirection.NORTH);
            stationTile.disableWorking();
            station = (LightHunterSpaceStation) stationTile.getMetaTileEntity();
            NBTTagCompound empty = wailaData(station, stationTile, player);
            require(empty.getInteger("lightHunterArrays") == 0 && empty.getInteger("lightHunterModules") == 0,
                "new station Waila counts start at zero");
            station.installAstralArrays(64);
            station.mMachine = true;
            int[] pos = station.getModulePositions().get(0);
            player.worldObj.setBlock(pos[0], pos[1], pos[2], GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity moduleTile = (BaseMetaTileEntity) player.worldObj.getTileEntity(pos[0], pos[1], pos[2]);
            moduleTile.setInitialValuesAsNBT(null, (short) GTNGMachineID.NexusSatelliteFactoryMkI.id);
            moduleTile.setFrontFacing(ForgeDirection.EAST);
            NexusSatelliteFactory module = (NexusSatelliteFactory) moduleTile.getMetaTileEntity();
            module.mMachine = true;
            NBTTagCompound connected = wailaData(station, stationTile, player);
            require(connected.getInteger("lightHunterArrays") == 64 && connected.getInteger("lightHunterModules") == 1,
                "Waila uses installed arrays and real module-position count");
            module.mMachine = false;
            station.mMachine = false;
            require(wailaData(station, stationTile, player).getInteger("lightHunterModules") == 0,
                "Waila updates disconnected module count");
            List<String> lines = wailaBody(station, stationTile, player, connected);
            require(
                lines.contains(StatCollector.translateToLocalFormatted("gtng.light_hunter.arrays", 64))
                    && lines.contains(StatCollector.translateToLocalFormatted("gtng.light_hunter.modules", 1)),
                "Waila body reads server snapshot with original labels and limits");
            report.append("PASS station Waila: 0/64, 64/64, connected 1/20, disconnected 0/20, server snapshot.\n");
            player.playerNetServerHandler.setPlayerLocation(0.5, 13, -3, 0, 0);
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            requested = 0;
        }
        if (hoverRequested) {
            hoverRequested = false;
            player.closeScreen();
            player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 2);
            hoverReady = true;
        }
        if (requested >= 0 && serverTicks >= 40) {
            requested = -1;
            player.closeScreen();
            openingType = 0;
            openAt = serverTicks + 10;
        }
        if (openingType == 0 && serverTicks >= openAt) {
            openingType = -1;
            station.onRightclick(stationTile, player);
            opened = 0;
        }
    }

    private void stationRender(Minecraft mc) throws Exception {
        if (stage == 3) {
            if (!hoverReady || mc.currentScreen != null || ++frames < 120) return;
            require(mc.objectMouseOver != null && mc.objectMouseOver.blockX == 0
                && mc.objectMouseOver.blockY == 12
                && mc.objectMouseOver.blockZ == 0, "real station hover target");
            capture("station-waila");
            finished = true;
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
            return;
        }
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 45) return;
        var sync = wrapper.getScreen().getSyncManager().getMainPSM();
        require(
            sync.findSyncHandlerNullable("lightHunterArrays", 0) == null
                && sync.findSyncHandlerNullable("lightHunterModules", 0) == null,
            "native station terminal without counters");
        capture("station-main");
        report.append("PASS station native GUI scale ").append(mc.gameSettings.guiScale).append(" and reopen.\n");
        advance();
        if (stage == 3) {
            mc.gameSettings.guiScale = 2;
            hoverRequested = true;
        } else {
            mc.gameSettings.guiScale = stage == 1 ? 1 : 3;
            opened = -1;
            requested = 0;
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

    private void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(),
            "mk" + (opened + 1) + "-scale" + mc.gameSettings.guiScale + "-" + name + ".png", mc.displayWidth,
            mc.displayHeight, mc.getFramebuffer());
    }

    private static File output() {
        File output = new File(System.getProperty("gtng.satelliteModes.output"));
        output.mkdirs();
        return output;
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
