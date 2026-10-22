package com.xyp.gtnotgood.qa;

import static gregtech.api.util.GTRecipeConstants.CLEANROOM;
import static gregtech.api.util.GTRecipeConstants.LOW_GRAVITY;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationMath;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationRenderer;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.common.machines.hatch.lighthunter.SatelliteThreadModifierHatch;
import com.xyp.gtnotgood.common.machines.multiblock.LightHunterSpaceStation;
import com.xyp.gtnotgood.common.machines.multiblock.NexusSatelliteFactory;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.LightHunterSpaceStationStructure;
import com.xyp.gtnotgood.common.machines.multiblock.structure.NexusSatelliteFactoryStructure;
import com.xyp.gtnotgood.common.machines.processing.LightHunterEnergy;
import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoidingMode;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.nei.GTNEIDefaultHandler;
import tectech.thing.CustomItemList;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;

@Mod(
    modid = "lighthunterqa",
    name = "Light Hunter QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class LightHunterClientChecks {

    private final List<int[]> stationSlots = slots(LightHunterSpaceStationStructure.shape(), 'd');
    private final List<int[]> mkIVPositions = slots(LightHunterSpaceStationStructure.shape(), 'U');
    private final List<int[]> satelliteSlots = slots(NexusSatelliteFactoryStructure.shape(), 'O');
    private final StringBuilder report = new StringBuilder();
    private BaseMetaTileEntity stationTile, satelliteTile, threadTile;
    private LightHunterSpaceStation station;
    private NexusSatelliteFactory satellite;
    private SatelliteThreadModifierHatch threadHatch;
    private GTRecipe firstNativeRecipe, secondNativeRecipe;
    private boolean started;
    private volatile boolean checked, finished;
    private volatile int requestedView = -1, shownView = -1;
    private int clientTicks, serverTicks, frames, capturedView = -1;
    private long controlledTick = 1_000_000, firstRenderedFrame, firstBeamFrame;
    private boolean visualEnabled = true;
    private volatile int requestedGui = -1, openedGui = -1;
    private volatile List<NBTTagCompound> guiExpectedOutputs;
    private GTNEIDefaultHandler acquisitionHandler;
    private int guiStageFrames;
    private int viewFrames;
    private boolean materialIconsChecked;
    private boolean satelliteOverviewCaptured;
    private String energyFixtureDescription = "";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.lighthunter.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 18000, "Light Hunter client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.clouds = false;
            mc.gameSettings.hideGUI = checked && openedGui < 0 && acquisitionHandler == null;
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 16;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("light-hunter-qa-" + System.currentTimeMillis(), "Light Hunter QA",
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
            require(++serverTicks <= 12000, "Light Hunter server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) build(player);
            if (serverTicks == 60) {
                verifyStructures();
                verifyModulePositions();
                verifyThreadHatch();
                verifyEnergyHatchLimits(player);
                verifyStationPower();
                verifyNativeBatch();
                verifyMultiAmpThroughput(player);
                verifyArraysAndPersistence();
                startStation();
                checked = true;
                requestedView = 0;
            }
            if (!checked) return;
            if (visualEnabled && !station.mEnergyHatches.isEmpty()) {
                charge(station);
                stationTile.enableWorking();
                repair(station);
            }
            if (requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                if (view == 3)
                    require(station.installAstralArrays(64) == 64, "Visual comparison installs real 64 arrays");
                if (view == 5) {
                    visualEnabled = false;
                    stationTile.disableWorking();
                    station.stopMachine(ShutDownReasonRegistry.NONE);
                    stationTile.setActive(false);
                } else if (view == 6) {
                    visualEnabled = true;
                    startStation();
                }
                double[] star = starCenter();
                boolean overview = view == 0 || view == 3;
                double[] target = overview ? new double[] { 0.5, 120.5, 110.5 } : star;
                double x = overview ? 150.5 : view == 2 ? 65.5 : star[0] + 70;
                double y = overview ? 200.5 : view == 2 ? 150.5 : star[1] + 50;
                double z = overview ? 70.5 : view == 2 ? 65.5 : star[2] - 70;
                if (view == 2) target = new double[] { 0.5, 120.5, 110.5 };
                move(player, x, y, z, target);
                shownView = view;
            }
            if (openedGui == 1) charge(satellite);
            if (requestedGui >= 0) {
                int gui = requestedGui;
                requestedGui = -1;
                player.closeScreen();
                BaseMetaTileEntity target = gui == 0 ? stationTile : gui == 1 ? satelliteTile : threadTile;
                move(player, target.getXCoord() + 3.5, target.getYCoord() + 1, target.getZCoord() - 2.5,
                    new double[] { target.getXCoord() + 0.5, target.getYCoord() + 0.5, target.getZCoord() + 0.5 });
                if (gui == 1) {
                    satellite.stopMachine(ShutDownReasonRegistry.NONE);
                    satellite.setMinimumDuration(1200);
                    seed(firstNativeRecipe, secondNativeRecipe);
                    charge(satellite);
                    repair(satellite);
                    require(satellite.checkProcessing().wasSuccessful(), "Native batch starts for actual output GUI");
                    guiExpectedOutputs = satellite.getBatchOutputRows();
                    require(!guiExpectedOutputs.isEmpty(), "Running batch has exact physical output rows");
                }
                if (
                    gui == 2
                ) require(threadHatch.getThreadMultiplier() == 2 && threadHatch.getAdditionalThreads(satellite) == 8192,
                    "Physical thread GUI opens with2 multiplier and8192 extra threads: multiplier="
                        + threadHatch.getThreadMultiplier()
                        + ", threads="
                        + threadHatch.getAdditionalThreads(satellite)
                        + ", formed="
                        + satellite.mMachine
                        + ", arrays="
                        + threadHatch.getArrayCount());
                target.getMetaTileEntity().onRightclick(target, player, target.getFrontFacing(), 0.5F, 0.5F, 0.5F);
                openedGui = gui;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void build(EntityPlayerMP player) {
        World world = player.worldObj;
        world.setWorldTime(6000);
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        for (int cx = -8; cx <= 12; cx++) for (int cz = -5; cz <= 21; cz++) world.getChunkFromChunkCoords(cx, cz);
        stationTile = place(world, 0, 120, 0, GTNGMachineID.LightHunterSpaceStation.id, ForgeDirection.NORTH, player);
        station = (LightHunterSpaceStation) stationTile.getMetaTileEntity();
        station.construct(new ItemStack(Items.diamond), false);
        hatch(station, stationSlots.get(0), 36, 36, 0, ItemList.Hatch_Input_Bus_UV.get(1), player);
        hatch(station, stationSlots.get(1), 36, 36, 0, ItemList.Hatch_Energy_UXV.get(1), player);
        int[] module = station.getModulePositions().get(0);
        satelliteTile = place(world, module[0], module[1], module[2], GTNGMachineID.NexusSatelliteFactoryMkI.id,
            ForgeDirection.EAST, player);
        satellite = (NexusSatelliteFactory) satelliteTile.getMetaTileEntity();
        satellite.construct(new ItemStack(Items.diamond), false);
        ItemStack[] hatches = { ItemList.Hatch_Input_Bus_UV.get(1), ItemList.Hatch_Output_Bus_UV.get(1),
            ItemList.Hatch_Maintenance.get(1), ItemList.Hatch_Energy_UHV.get(1), ItemList.Hatch_Input_UV.get(1),
            ItemList.Hatch_Output_UV.get(1) };
        for (int index = 0; index < hatches.length; index++)
            hatch(satellite, satelliteSlots.get(index), 2, 10, 0, hatches[index], player);
        hatch(satellite, satelliteSlots.get(6), 2, 10, 0, GTNGItemList.SatelliteThreadModifierHatch.get(1), player);
        int[] threadPosition = position(satellite, satelliteSlots.get(6), 2, 10, 0);
        threadTile = (BaseMetaTileEntity) world.getTileEntity(threadPosition[0], threadPosition[1], threadPosition[2]);
        threadHatch = (SatelliteThreadModifierHatch) threadTile.getMetaTileEntity();
        satellite.setVoidingMode(VoidingMode.VOID_NONE);
        satellite.setMinimumDuration(20);
        satellite.setMachineMode(0);
        repair(station);
        repair(satellite);
    }

    private void verifyStructures() {
        require(LightHunterSpaceStationStructure.WIDTH == 73 && LightHunterSpaceStationStructure.HEIGHT == 73
            && LightHunterSpaceStationStructure.LENGTH == 189, "Original main station dimensions");
        require(NexusSatelliteFactoryStructure.WIDTH == 5 && NexusSatelliteFactoryStructure.HEIGHT == 21
            && NexusSatelliteFactoryStructure.LENGTH == 13, "Original satellite factory dimensions");
        verifyStructure(station, stationTile);
        verifyStructure(satellite, satelliteTile);
        require(station.mInputBusses.size() == 1 && station.mEnergyHatches.size() == 1,
            "Main station discovers real input and single energy hatches");
        require(satellite.mInputBusses.size() == 1 && satellite.mOutputBusses.size() == 1
            && satellite.mInputHatches.size() == 1
            && satellite.mOutputHatches.size() == 1
            && satellite.mMaintenanceHatches.size() == 1
            && satellite.mEnergyHatches.size() == 1, "Physical satellite discovers all six hatch types");
        report.append("Original 73x73x189 main station and 5x21x13 satellite both physically form.\n");
    }

    private static void verifyStructure(MTEMultiBlockBase machine, BaseMetaTileEntity tile) {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(tile, null, errors);
        require(errors.isEmpty(), "Original structure errors: " + errors);
        require(machine.checkStructure(true, tile), "Full original structure forms " + machine.getLocalName());
    }

    private void verifyModulePositions() {
        List<int[]> positions = station.getModulePositions();
        require(positions.size() == 20, "Host exposes all twenty original module positions");
        Set<String> unique = new HashSet<>();
        for (int[] position : positions) {
            require(position[1] == 120 && Math.abs(position[0]) == 9
                && position[2] >= 41
                && position[2] <= 113
                && (position[2] - 41) % 8 == 0, "Original two rows of ten relative module slots");
            unique.add(Arrays.toString(position));
        }
        require(unique.size() == 20 && satellite.getHost() == station && station.getConnectedModuleCount() == 1,
            "Physical satellite binds to the actual host at its exact socket");
        satelliteTile.setFrontFacing(ForgeDirection.WEST);
        require(!station.acceptsModule(satellite) && station.getConnectedModuleCount() == 0,
            "A satellite facing away from the original connector is rejected");
        satelliteTile.setFrontFacing(ForgeDirection.EAST);
        verifyStructure(satellite, satelliteTile);
        verifyStructure(station, stationTile);
        require(station.acceptsModule(satellite) && station.getConnectedModuleCount() == 1,
            "Restoring the original inward-facing connector restores the binding");
        require(!satellite.isUnrestricted() && satellite.getMinimumDuration() == 20,
            "Unfilled host uses normal allocation and the twenty-tick minimum");
        report.append("All20 distinct socket positions match the source; physical satellite binds at(-9,120,41).\n");
    }

    private void verifyThreadHatch() {
        require(satellite.hasThreadModifier(threadHatch), "Physical O-position thread hatch binds to its satellite");
        require(
            threadHatch.isItemValidForSlot(0, GTNGItemList.AstralArray.get(1))
                && !threadHatch.isItemValidForSlot(0, GTNGItemList.CompressedAstralArray.get(1)),
            "Thread hatch accepts ordinary arrays and rejects compressed arrays");
        threadHatch.setInventorySlotContents(0, GTNGItemList.AstralArray.get(1));
        require(satellite.getAdditionalThreads() == 128 && threadHatch.getThreadMultiplier() == 2,
            "One ordinary array adds64x2=128 threads with the original multiplier");
        threadHatch.setInventorySlotContents(0, GTNGItemList.AstralArray.get(64));
        require(satellite.getAdditionalThreads() == 8192 && threadHatch.getArrayCount() == 64,
            "Sixty-four ordinary arrays add8192 threads without consumption");
        threadHatch.setInventorySlotContents(0, GTNGItemList.CompressedAstralArray.get(1));
        require(satellite.getAdditionalThreads() == 0 && threadHatch.getArrayCount() == 0,
            "Even forced invalid inventory grants no additional threads");
        threadHatch.setInventorySlotContents(0, GTNGItemList.AstralArray.get(64));
        report.append("Real O-slot thread hatch:64 ordinary arrays grant8192 threads; compressed arrays rejected.\n");
    }

    private void verifyEnergyHatchLimits(EntityPlayerMP player) {
        ItemStack[] stationInputs = { ItemList.Hatch_Energy_UXV.get(1), CustomItemList.eM_energyMulti4_UXV.get(1),
            CustomItemList.eM_energyWirelessMulti4_UXV.get(1), CustomItemList.eM_energyWirelessTunnel1_UXV.get(1),
            GTNGItemList.WirelessMultiAmpEnergyUMV256A.get(1) };
        try {
            for (ItemStack input : stationInputs) {
                stationEnergy(player, input);
                verifyStructure(station, stationTile);
                require(energyCount(station) == 1 && laserCount(station) == 0,
                    "One ordinary/multi/native-wireless/catalog256A/addon256A hatch uses the station's nonlaser slot; "
                        + energyFixtureDescription);
                startStation();
                long before = stored(station);
                require(station.onRunningTick(null) && before - stored(station) == LightHunterRecipeRules.STATION_EUT,
                    "Each accepted station hatch supplies the real UXV running cost");
                station.stopMachine(ShutDownReasonRegistry.NONE);
            }
            stationEnergy(player, ItemList.Hatch_Energy_UXV.get(1), CustomItemList.eM_energyMulti4_UXV.get(1));
            rejectStructure(station, stationTile, "Station rejects two mixed nonlaser energy hatches");
            stationEnergy(player, CustomItemList.eM_energyWirelessMulti4_UXV.get(1),
                GTNGItemList.WirelessMultiAmpEnergyUMV256A.get(1));
            rejectStructure(station, stationTile, "Station counts native and addon wireless hatches together");
            for (ItemStack laser : new ItemStack[] { CustomItemList.eM_energyTunnel1_UXV.get(1),
                CustomItemList.eM_energyTunnel2_UXV.get(1) }) {
                stationEnergy(player, laser);
                rejectStructure(station, stationTile, "Station rejects actual native wired256A/1024A laser hatches");
            }
            stationEnergy(player, ItemList.Hatch_Energy_UXV.get(1));
            verifyStructure(station, stationTile);
            for (ItemStack input : new ItemStack[] { ItemList.Hatch_Energy_EV.get(1),
                CustomItemList.eM_energyMulti4_EV.get(1), CustomItemList.eM_energyWirelessMulti4_EV.get(1),
                CustomItemList.eM_energyWirelessTunnel1_UXV.get(1), GTNGItemList.WirelessMultiAmpEnergyEV256A.get(1),
                GTNGItemList.WirelessMultiAmpEnergyEV1024A.get(1) }) {
                satelliteEnergy(player, input);
                verifyStructure(satellite, satelliteTile);
                require(energyCount(satellite) == 1 && laserCount(satellite) == 0,
                    "Satellite recognizes ordinary/multi/native-wireless/catalog256A/addon256A/addon1024A as nonlaser; "
                        + energyFixtureDescription);
            }
            for (ItemStack laser : new ItemStack[] { CustomItemList.eM_energyTunnel1_IV.get(1),
                CustomItemList.eM_energyTunnel2_UXV.get(1) }) {
                satelliteEnergy(player, laser);
                verifyStructure(satellite, satelliteTile);
                require(energyCount(satellite) == 0 && laserCount(satellite) == 1,
                    "Satellite recognizes actual native256A/1024A wired hatches as one laser; "
                        + energyFixtureDescription);
            }
            satelliteEnergy(player, ItemList.Hatch_Energy_EV.get(1), CustomItemList.eM_energyMulti4_EV.get(1),
                CustomItemList.eM_energyTunnel1_IV.get(1));
            verifyStructure(satellite, satelliteTile);
            require(energyCount(satellite) == 2 && laserCount(satellite) == 1,
                "Satellite forms with two mixed nonlaser hatches and one laser");
            satelliteEnergy(player, CustomItemList.eM_energyWirelessMulti4_EV.get(1),
                GTNGItemList.WirelessMultiAmpEnergyEV256A.get(1), CustomItemList.eM_energyTunnel2_UXV.get(1));
            verifyStructure(satellite, satelliteTile);
            require(energyCount(satellite) == 2 && laserCount(satellite) == 1,
                "Addon256A stays nonlaser beside native4A wireless and a wired laser");
            satelliteEnergy(player, ItemList.Hatch_Energy_EV.get(1), CustomItemList.eM_energyMulti4_EV.get(1),
                GTNGItemList.WirelessMultiAmpEnergyEV1024A.get(1));
            rejectStructure(satellite, satelliteTile, "Satellite rejects three mixed nonlaser hatches");
            satelliteEnergy(player, ItemList.Hatch_Energy_EV.get(1), CustomItemList.eM_energyTunnel1_IV.get(1),
                CustomItemList.eM_energyTunnel2_UXV.get(1));
            rejectStructure(satellite, satelliteTile, "Satellite rejects two mixed laser hatches");
            report.append("Physical energy limits: station1 nonlaser/no laser; satellite2 nonlaser+1 laser; "
                + "native wireless4A/catalog256A and addon256/1024A use nonlaser slots; wired256/1024A uses laser.\n");
        } finally {
            stationEnergy(player, ItemList.Hatch_Energy_UXV.get(1));
            satelliteEnergy(player, ItemList.Hatch_Energy_UHV.get(1));
            verifyStructure(station, stationTile);
            verifyStructure(satellite, satelliteTile);
        }
    }

    private static int energyCount(MTEMultiBlockBase machine) {
        return LightHunterEnergy.countEnergyHatches(machine.mEnergyHatches, machine.getExoticEnergyHatches());
    }

    private static int laserCount(MTEMultiBlockBase machine) {
        return LightHunterEnergy.countLaserHatches(machine.getExoticEnergyHatches());
    }

    private static List<MTEHatch> energyHatches(MTEMultiBlockBase machine) {
        List<MTEHatch> hatches = new ArrayList<>();
        hatches.addAll(machine.mEnergyHatches);
        hatches.addAll(machine.getExoticEnergyHatches());
        return hatches;
    }

    private void rejectStructure(MTEMultiBlockBase machine, BaseMetaTileEntity tile, String message) {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(tile, null, errors);
        require(!errors.isEmpty() && !machine.checkStructure(true, tile),
            message + "; errors=" + errors + "; physical=" + energyFixtureDescription);
    }

    private void stationEnergy(EntityPlayerMP player, ItemStack... inputs) {
        station.stopMachine(ShutDownReasonRegistry.NONE);
        replaceEnergy(station, stationSlots, new int[] { 1, 2, 3 }, 36, 36, inputs, player);
    }

    private void satelliteEnergy(EntityPlayerMP player, ItemStack... inputs) {
        satellite.stopMachine(ShutDownReasonRegistry.NONE);
        replaceEnergy(satellite, satelliteSlots, new int[] { 3, 7, 8 }, 2, 10, inputs, player);
    }

    private void replaceEnergy(GTNGMultiBlockBase<?> machine, List<int[]> slots, int[] indices, int ox, int oy,
        ItemStack[] inputs, EntityPlayerMP player) {
        require(inputs.length <= indices.length, "Energy fixture has enough original hatch slots");
        World world = machine.getBaseMetaTileEntity().getWorld();
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < indices.length; i++) {
            int[] local = slots.get(indices[i]);
            int[] pos = position(machine, local, ox, oy, 0);
            world.setBlockToAir(pos[0], pos[1], pos[2]);
            if (i < inputs.length) {
                hatch(machine, local, ox, oy, 0, inputs[i], player);
                BaseMetaTileEntity placed = (BaseMetaTileEntity) world.getTileEntity(pos[0], pos[1], pos[2]);
                var mte = placed.getMetaTileEntity();
                if (description.length() > 0) description.append("; ");
                description.append(inputs[i].getDisplayName()).append(" id=").append(inputs[i].getItemDamage())
                    .append(" class=").append(mte.getClass().getName()).append(" hatchType=")
                    .append(mte instanceof MTEHatchEnergyMulti multi ? multi.getHatchType() : 0);
            } else world.setBlock(pos[0], pos[1], pos[2], LightHunterCasing.HighPowerCasing.getBlock(),
                LightHunterCasing.HighPowerCasing.getMetadata(), 3);
        }
        energyFixtureDescription = description.toString();
    }

    private void verifyStationPower() {
        stationTile.setInventorySlotContents(1, null);
        require(!station.checkProcessing().wasSuccessful(), "Main station refuses to start without circuit1");
        stationTile.setInventorySlotContents(1, GTUtility.copyAmountUnsafe(1, GTUtility.getIntegratedCircuit(1)));
        startStation();
        require(
            LightHunterRecipeRules.STATION_EUT == GTValues.V[VoltageIndex.UXV]
                && station.lEUt == -LightHunterRecipeRules.STATION_EUT
                && station.mMaxProgresstime == 800,
            "Main station retains the native long-valued UXV cost and800-tick cycle");
        long stored = stored(station);
        require(station.onRunningTick(null), "Physical native UXV hatch supplies the main station");
        require(stored - stored(station) == LightHunterRecipeRules.STATION_EUT,
            "Main station drains exactly the native UXV EU/t without integer overflow");
        require(
            stationTile.getStackInSlot(1) != null && stationTile.getStackInSlot(1).stackSize == 1
                && GTUtility.areStacksEqual(stationTile.getStackInSlot(1), GTUtility.getIntegratedCircuit(1)),
            "Configuration circuit1 is retained");
        report.append("Main station circuit1 and physical long UXV draw verified: ")
            .append(LightHunterRecipeRules.STATION_EUT).append(" EU/t.\n");
    }

    private void startStation() {
        charge(station);
        repair(station);
        stationTile.enableWorking();
        stationTile.setInventorySlotContents(1, GTUtility.copyAmountUnsafe(1, GTUtility.getIntegratedCircuit(1)));
        if (station.mMaxProgresstime <= 0) require(station.checkProcessing().wasSuccessful(), "Main station starts");
        stationTile.setActive(true);
        stationTile.issueTileUpdate();
        require(station.isStationWorking(), "Host is actually operating");
    }

    private void verifyNativeBatch() throws Exception {
        List<GTRecipe> nativeRecipes = new ArrayList<>(satellite.getRecipeMap().getAllRecipes());
        List<GTRecipe> candidates = new ArrayList<>();
        for (GTRecipe recipe : nativeRecipes) {
            if (!simpleRecipe(recipe)) continue;
            ItemStack input = recipe.mInputs[0].copy();
            if (matches(nativeRecipes, new ItemStack[] { input }) == 1) candidates.add(recipe);
        }
        GTRecipe first = null, second = null;
        search: for (GTRecipe a : candidates) for (GTRecipe b : candidates) {
            if (a == b || GTUtility.areStacksEqual(a.mInputs[0], b.mInputs[0])) continue;
            ItemStack[] inputs = { GTUtility.copyAmount(a.mInputs[0].stackSize * 2, a.mInputs[0]),
                GTUtility.copyAmount(b.mInputs[0].stackSize * 3, b.mInputs[0]) };
            if (matches(nativeRecipes, inputs) != 2) continue;
            BigInteger energy = energy(a, 2).add(energy(b, 3));
            if (energy.mod(BigInteger.valueOf(20)).signum() != 0) continue;
            first = a;
            second = b;
            break search;
        }
        require(first != null && second != null, "Two independent real native lathe recipes are available");
        firstNativeRecipe = first;
        secondNativeRecipe = second;
        seed(first, second);
        charge(satellite);
        repair(satellite);
        require(satellite.checkProcessing().wasSuccessful(), "Real physical satellite commits two native recipes");
        require(inputCount(first.mInputs[0]) == 0 && inputCount(second.mInputs[0]) == 0,
            "One batch atomically consumes2 and3 actual recipe sets");
        NBTTagCompound saved = new NBTTagCompound();
        satellite.saveNBTData(saved);
        require(saved.getInteger("lightHunterBatchRecipes") == 2 && saved.getLong("lightHunterBatchParallels") == 5,
            "Two distinct recipes are combined into one five-parallel batch");
        BigInteger expectedEnergy = energy(first, 2).add(energy(second, 3));
        require(BigInteger.valueOf(-satellite.lEUt).multiply(BigInteger.valueOf(satellite.mMaxProgresstime))
            .equals(expectedEnergy), "Native batch preserves both recipes' combined energy");
        long stored = stored(satellite), eut = -satellite.lEUt;
        run(satellite, satelliteTile);
        require(stored - stored(satellite) == eut && satellite.mProgresstime == 1,
            "Actual batch tick advances shared progress and drains its exact local power");
        stationTile.disableWorking();
        station.stopMachine(ShutDownReasonRegistry.NONE);
        stationTile.setActive(false);
        require(!station.isStationWorking(), "Host is stopped before the satellite completes");
        for (int tick = 0; tick < 10000 && satellite.mMaxProgresstime > 0; tick++) {
            charge(satellite);
            repair(satellite);
            run(satellite, satelliteTile);
        }
        require(satellite.mMaxProgresstime == 0, "Committed satellite batch finishes with its host stopped");
        List<ItemStack> expected = new ArrayList<>();
        addExpected(expected, first, 2);
        addExpected(expected, second, 3);
        for (ItemStack output : expected)
            require(outputCount(output) == output.stackSize, "Native combined batch has exact physical output");
        seed(first, second);
        require(
            !satellite.checkProcessing().wasSuccessful()
                && inputCount(first.mInputs[0]) == first.mInputs[0].stackSize * 2
                && inputCount(second.mInputs[0]) == second.mInputs[0].stackSize * 3,
            "Stopped host blocks a new batch without consuming either recipe's inputs");
        startStation();
        require(satellite.checkProcessing().wasSuccessful(), "Host restoration admits the waiting native batch");
        satellite.stopMachine(ShutDownReasonRegistry.NONE);
        satelliteTile.disableWorking();
        report.append(
            "Real native lathe recipes combined2+3 sets with exact inputs/outputs/energy; host-off gates only new batches.\n");
    }

    private void verifyMultiAmpThroughput(EntityPlayerMP player) throws Exception {
        try {
            startStation();
            satellite.setMachineMode(0);
            List<GTRecipe> recipes = new ArrayList<>(satellite.getRecipeMap().getAllRecipes());
            GTRecipe selected = null;
            BigInteger mixedPower = BigInteger.valueOf(GTValues.V[VoltageIndex.EV] * 8);
            BigInteger fastPower = BigInteger.valueOf(GTValues.V[VoltageIndex.EV] * 64);
            for (GTRecipe recipe : recipes) {
                if (
                    !simpleRecipe(recipe) || !fitsPowerFixture(recipe, 128)
                        || matches(recipes, new ItemStack[] { recipe.mInputs[0].copy() }) != 1
                ) continue;
                BigInteger energy = energy(recipe, 128);
                if (
                    energy.compareTo(mixedPower.multiply(BigInteger.valueOf(20))) <= 0
                        || energy.compareTo(fastPower.multiply(BigInteger.valueOf(20))) > 0
                        || energy.mod(mixedPower).signum() != 0
                        || energy.mod(BigInteger.valueOf(20)).signum() != 0
                ) continue;
                selected = recipe;
                break;
            }
            require(selected != null, "A unique real lathe recipe exposes ordinary/4A/8A/64A throughput");
            int normal = verifyPowerBatch(player, selected, GTValues.V[VoltageIndex.EV] * 2,
                ItemList.Hatch_Energy_EV.get(1));
            int multi = verifyPowerBatch(player, selected, GTValues.V[VoltageIndex.EV] * 4,
                CustomItemList.eM_energyMulti4_EV.get(1));
            int mixed = verifyPowerBatch(player, selected, GTValues.V[VoltageIndex.EV] * 8,
                CustomItemList.eM_energyMulti4_EV.get(1), CustomItemList.eM_energyWirelessMulti4_EV.get(1));
            int fast = verifyPowerBatch(player, selected, GTValues.V[VoltageIndex.EV] * 64,
                CustomItemList.eM_energyMulti64_EV.get(1));
            int addon = verifyPowerBatch(player, selected, GTValues.V[VoltageIndex.EV] * 256,
                GTNGItemList.WirelessMultiAmpEnergyEV256A.get(1));
            require(normal > multi && multi > mixed && mixed > fast && fast == 20 && addon == 20,
                "Same128 real recipe sets accelerate with total working power and retain the20-tick floor");
            report.append("Physical128-set batch durations ordinary2A/4A/mixed8A/64A/addon256A: ").append(normal)
                .append('/').append(multi).append('/').append(mixed).append('/').append(fast).append('/').append(addon)
                .append(" ticks; exact native inputs, outputs and energy preserved.\n");
            verifyTotalPowerRecipeGate(player);
        } finally {
            satellite.stopMachine(ShutDownReasonRegistry.NONE);
            satelliteTile.disableWorking();
            clearPowerInventory();
            satellite.setMachineMode(0);
            satellite.setMinimumDuration(20);
            satelliteEnergy(player, ItemList.Hatch_Energy_UHV.get(1));
            verifyStructure(satellite, satelliteTile);
        }
    }

    private int verifyPowerBatch(EntityPlayerMP player, GTRecipe recipe, long expectedPower, ItemStack... inputs)
        throws Exception {
        satelliteEnergy(player, inputs);
        verifyStructure(satellite, satelliteTile);
        require(LightHunterEnergy.totalInputPower(satellite.mEnergyHatches, satellite.getExoticEnergyHatches())
            == expectedPower, "Physical energy rating sums voltage times working amperage, without input headroom");
        clearPowerInventory();
        seedPower(recipe, 128);
        charge(satellite);
        repair(satellite);
        satelliteTile.enableWorking();
        require(satellite.checkProcessing().wasSuccessful(), "Real native batch starts with" + expectedPower + "EU/t");
        require(inputCount(recipe.mInputs[0]) == 0, "All128 native recipe inputs are consumed once");
        BigInteger totalEnergy = energy(recipe, 128);
        int duration = satellite.mMaxProgresstime;
        require(
            duration >= 20 && -satellite.lEUt <= expectedPower
                && BigInteger.valueOf(-satellite.lEUt).multiply(BigInteger.valueOf(duration)).equals(totalEnergy),
            "Working-power throughput retains the exact128-set native fuel cost");
        long before = stored(satellite), eut = -satellite.lEUt;
        run(satellite, satelliteTile);
        require(before - stored(satellite) == eut && satellite.mProgresstime == 1,
            "Physical wired/wireless buffers pay the actual batch EU/t and advance progress");
        for (int tick = 0; tick <= duration + 5 && satellite.mMaxProgresstime > 0; tick++) {
            charge(satellite);
            repair(satellite);
            run(satellite, satelliteTile);
        }
        require(satellite.mMaxProgresstime == 0, "Multi-amp native batch actually completes");
        List<ItemStack> outputs = new ArrayList<>();
        addExpected(outputs, recipe, 128);
        NBTTagCompound completed = new NBTTagCompound();
        satellite.saveNBTData(completed);
        for (ItemStack output : outputs) {
            int actual = outputCount(output);
            require(actual == output.stackSize,
                "All128 recipe outputs arrive exactly once: actual=" + actual
                    + ", expected="
                    + output.stackSize
                    + ", item="
                    + output.getDisplayName()
                    + ", meta="
                    + output.getItemDamage()
                    + ", batchParallels="
                    + completed.getLong("lightHunterBatchParallels")
                    + ", outputReady="
                    + completed.getBoolean("lightHunterOutputReady")
                    + ", ledger="
                    + completed.getTag("lightHunterBatchOutputs"));
        }
        satellite.stopMachine(ShutDownReasonRegistry.NONE);
        return duration;
    }

    private void verifyTotalPowerRecipeGate(EntityPlayerMP player) {
        List<RecipeMap<?>> maps = new ArrayList<>(satellite.getAvailableRecipeMaps());
        GTRecipe selected = null;
        int selectedMode = -1;
        long lowPower = GTValues.V[VoltageIndex.LV] * 2, highPower = GTValues.V[VoltageIndex.LV] * 256;
        for (int mode = 0; mode < maps.size() && selected == null; mode++) {
            List<GTRecipe> recipes = new ArrayList<>(maps.get(mode).getAllRecipes());
            for (GTRecipe recipe : recipes) {
                if (
                    !powerRecipe(recipe) || recipe.mEUt <= lowPower
                        || recipe.mEUt > highPower
                        || !fitsPowerFixture(recipe, 1)
                        || matches(recipes, new ItemStack[] { recipe.mInputs[0].copy() }) != 1
                ) continue;
                selected = recipe;
                selectedMode = mode;
                break;
            }
        }
        require(selected != null, "A real native satellite recipe lies above2A LV and below256A LV total power");
        satellite.setMachineMode(selectedMode);
        satelliteEnergy(player, ItemList.Hatch_Energy_LV.get(1));
        verifyStructure(satellite, satelliteTile);
        clearPowerInventory();
        seedPower(selected, 1);
        charge(satellite);
        require(
            !satellite.checkProcessing().wasSuccessful()
                && inputCount(selected.mInputs[0]) == selected.mInputs[0].stackSize,
            "Insufficient total input power rejects the high-EU native recipe without consuming inputs");
        satelliteEnergy(player, GTNGItemList.WirelessMultiAmpEnergyLV256A.get(1));
        verifyStructure(satellite, satelliteTile);
        charge(satellite);
        repair(satellite);
        require(satellite.checkProcessing().wasSuccessful() && inputCount(selected.mInputs[0]) == 0,
            "Same-voltage256A supply unlocks the real higher-EU recipe using original total-power normalization");
        report.append("Native recipe gate uses total working power: ").append(selected.mEUt)
            .append("EU/t recipe rejected at").append(lowPower).append(" and accepted at").append(highPower)
            .append(" with unchanged LV voltage.\n");
        satellite.stopMachine(ShutDownReasonRegistry.NONE);
    }

    private boolean fitsPowerFixture(GTRecipe recipe, int parallels) {
        if (!powerRecipe(recipe)) return false;
        int inputSlots = (recipe.mInputs[0].stackSize * parallels + recipe.mInputs[0].getMaxStackSize() - 1)
            / recipe.mInputs[0].getMaxStackSize();
        if (inputSlots >= satellite.mInputBusses.get(0).getSizeInventory()) return false;
        int outputSlots = 0;
        for (ItemStack output : recipe.mOutputs)
            outputSlots += (output.stackSize * parallels + output.getMaxStackSize() - 1) / output.getMaxStackSize();
        return outputSlots <= satellite.mOutputBusses.get(0).getSizeInventory();
    }

    private static boolean powerRecipe(GTRecipe recipe) {
        if (
            !recipe.mEnabled || recipe.mInputs.length != 1
                || recipe.mInputs[0] == null
                || recipe.mInputs[0].stackSize < 1
                || recipe.mFluidInputs.length != 0
                || recipe.mFluidOutputs.length != 0
                || recipe.mOutputs.length == 0
                || recipe.mDuration < 1
                || recipe.mEUt <= 0
                || recipe.mSpecialValue != 0
                || recipe.getMetadataOrDefault(CLEANROOM, false)
                || recipe.getMetadataOrDefault(LOW_GRAVITY, false)
        ) return false;
        for (int i = 0; i < recipe.mOutputs.length; i++)
            if (recipe.mOutputs[i] == null || recipe.getOutputChance(i) != 10000) return false;
        return true;
    }

    private void clearPowerInventory() {
        for (var bus : satellite.mInputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var bus : satellite.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
    }

    private void seedPower(GTRecipe recipe, int parallels) {
        var bus = satellite.mInputBusses.get(0);
        int remaining = recipe.mInputs[0].stackSize * parallels;
        for (int slot = 0; remaining > 0 && slot < bus.getSizeInventory(); slot++) {
            int amount = Math.min(recipe.mInputs[0].getMaxStackSize(), remaining);
            bus.setInventorySlotContents(slot, GTUtility.copyAmount(amount, recipe.mInputs[0]));
            remaining -= amount;
        }
        require(remaining == 0, "Native input bus physically stores the complete power fixture");
    }

    private static boolean simpleRecipe(GTRecipe recipe) {
        if (
            !recipe.mEnabled || recipe.mInputs.length != 1
                || recipe.mInputs[0] == null
                || recipe.mInputs[0].stackSize < 1
                || recipe.mInputs[0].stackSize > 16
                || recipe.mFluidInputs.length != 0
                || recipe.mFluidOutputs.length != 0
                || recipe.mOutputs.length == 0
                || recipe.mOutputs.length > 3
                || recipe.mDuration < 20
                || recipe.mEUt <= 0
                || recipe.mEUt > 128
                || recipe.mSpecialValue != 0
        ) return false;
        for (int index = 0; index < recipe.mOutputs.length; index++) if (
            recipe.mOutputs[index] == null || recipe.mOutputs[index].stackSize > 16
                || recipe.getOutputChance(index) != 10000
        ) return false;
        return true;
    }

    private static int matches(List<GTRecipe> recipes, ItemStack[] inputs) {
        int count = 0;
        for (GTRecipe recipe : recipes) if (
            recipe.mEnabled && recipe.mEUt >= 0 && recipe.isRecipeInputEqual(false, false, new FluidStack[0], inputs)
        ) count++;
        return count;
    }

    private static BigInteger energy(GTRecipe recipe, int parallels) {
        return BigInteger.valueOf(recipe.mEUt).multiply(BigInteger.valueOf(recipe.mDuration))
            .multiply(BigInteger.valueOf(parallels));
    }

    private void seed(GTRecipe first, GTRecipe second) {
        var bus = satellite.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        bus.setInventorySlotContents(0, GTUtility.copyAmount(first.mInputs[0].stackSize * 2, first.mInputs[0]));
        bus.setInventorySlotContents(1, GTUtility.copyAmount(second.mInputs[0].stackSize * 3, second.mInputs[0]));
    }

    private static void addExpected(List<ItemStack> result, GTRecipe recipe, int parallels) {
        for (ItemStack original : recipe.mOutputs) {
            ItemStack output = GTUtility.copyAmountUnsafe(original.stackSize * parallels, original);
            ItemStack existing = result.stream().filter(item -> GTUtility.areStacksEqual(item, output)).findFirst()
                .orElse(null);
            if (existing == null) result.add(output);
            else existing.stackSize += output.stackSize;
        }
    }

    private int inputCount(ItemStack expected) {
        int count = 0;
        for (var bus : satellite.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack item = bus.getStackInSlot(slot);
            if (item != null && GTUtility.areStacksEqual(item, expected)) count += item.stackSize;
        }
        return count;
    }

    private int outputCount(ItemStack expected) {
        int count = 0;
        for (var bus : satellite.mOutputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack item = bus.getStackInSlot(slot);
            if (item != null && GTUtility.areStacksEqual(item, expected)) count += item.stackSize;
        }
        return count;
    }

    private void verifyArraysAndPersistence() {
        require(
            station.installAstralArrays(63) == 63 && station.getAstralArrayCount() == 63 && !satellite.isUnrestricted(),
            "Sixty-three arrays retain normal recipe allocation");
        require(
            station.installAstralArrays(2) == 1 && station.getAstralArrayCount() == 64 && satellite.isUnrestricted(),
            "Only the64th array enables unrestricted allocation");
        require(station.installAstralArrays(1) == 0 && station.installAstralArrays(-1) == 0,
            "Array installation cannot overfill or remove arrays");
        satellite.setMinimumDuration(37);
        stationTile = reload(stationTile);
        satelliteTile = reload(satelliteTile);
        threadTile = reload(threadTile);
        station = (LightHunterSpaceStation) stationTile.getMetaTileEntity();
        satellite = (NexusSatelliteFactory) satelliteTile.getMetaTileEntity();
        threadHatch = (SatelliteThreadModifierHatch) threadTile.getMetaTileEntity();
        verifyStructures();
        require(
            station.getAstralArrayCount() == 64 && satellite.getMinimumDuration() == 37
                && satellite.getHost() == station
                && satellite.isUnrestricted(),
            "Real main/satellite TileEntity reload preserves arrays, duration and host rebinding");
        require(
            satellite.hasThreadModifier(threadHatch) && satellite.getAdditionalThreads() == 8192
                && threadHatch.getArrayCount() == 64,
            "Real thread-hatch reload preserves ordinary arrays and rebinds8192 additional threads");
        NBTTagCompound normal = new NBTTagCompound();
        station.saveNBTData(normal);
        normal.setInteger("lightHunterAstralArrays", 0);
        station.loadNBTData(normal);
        satellite.setMinimumDuration(20);
        stationTile.issueTileUpdate();
        report.append("64-array cap/unlock and real TE save/reload passed; visual fixture reset to0 for comparison.\n");
    }

    private static BaseMetaTileEntity reload(BaseMetaTileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        World world = tile.getWorld();
        int x = tile.getXCoord(), y = tile.getYCoord(), z = tile.getZCoord();
        world.removeTileEntity(x, y, z);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(world);
        restored.readFromNBT(tag);
        world.setTileEntity(x, y, z, restored);
        return restored;
    }

    private void run(MTEMultiBlockBase machine, BaseMetaTileEntity tile) throws Exception {
        Method run = MTEMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        run.invoke(machine, tile, ++controlledTick);
    }

    private static void charge(MTEMultiBlockBase machine) {
        for (var hatch : energyHatches(machine)) {
            BaseMetaTileEntity tile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
            tile.setStoredEU(tile.getEUCapacity());
        }
    }

    private static long stored(MTEMultiBlockBase machine) {
        long amount = 0;
        for (var hatch : energyHatches(machine)) amount += hatch.getBaseMetaTileEntity().getStoredEU();
        return amount;
    }

    private static void repair(MTEMultiBlockBase machine) {
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
    }

    private static List<int[]> slots(String[][] shape, char symbol) {
        List<int[]> result = new ArrayList<>();
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == symbol) result.add(new int[] { col, row, depth });
        return result;
    }

    private static void hatch(GTNGMultiBlockBase<?> machine, int[] local, int ox, int oy, int oz, ItemStack stack,
        EntityPlayerMP owner) {
        int[] position = position(machine, local, ox, oy, oz);
        place(machine.getBaseMetaTileEntity().getWorld(), position[0], position[1], position[2], stack.getItemDamage(),
            ForgeDirection.NORTH, owner);
    }

    private static int[] position(GTNGMultiBlockBase<?> machine, int[] local, int ox, int oy, int oz) {
        int[] offset = new int[3];
        machine.getExtendedFacing().getWorldOffset(new int[] { local[0] - ox, local[1] - oy, local[2] - oz }, offset);
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        return new int[] { tile.getXCoord() + offset[0], tile.getYCoord() + offset[1], tile.getZCoord() + offset[2] };
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id, ForgeDirection facing,
        EntityPlayerMP owner) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(facing);
        tile.setOwnerName(owner.getCommandSenderName());
        tile.setOwnerUuid(owner.getUniqueID());
        tile.disableWorking();
        return tile;
    }

    private double[] starCenter() {
        double[] offset = LightHunterSpaceStationMath.rotatedOffset(-206, 0, 0, stationTile.getFrontFacing().ordinal());
        return new double[] { stationTile.getXCoord() + offset[0], stationTile.getYCoord() + offset[1],
            stationTile.getZCoord() + offset[2] };
    }

    private static void move(EntityPlayerMP player, double x, double y, double z, double[] target) {
        double dx = target[0] - x, dy = target[1] - y, dz = target[2] - z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        player.closeScreen();
        player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            if (acquisitionHandler != null) {
                captureAcquisition();
                return;
            }
            if (openedGui >= 0) {
                captureGui();
                return;
            }
            if (shownView != capturedView + 1 || requestedView >= 0) return;
            require(++viewFrames <= 3600,
                "Celestial effect and active-casing synchronization timeout view" + shownView);
            Minecraft mc = Minecraft.getMinecraft();
            TileEntity tile = mc.theWorld.getTileEntity(stationTile.getXCoord(), stationTile.getYCoord(),
                stationTile.getZCoord());
            if (
                !(tile instanceof BaseMetaTileEntity client)
                    || !(client.getMetaTileEntity() instanceof LightHunterSpaceStation machine)
            ) return;
            boolean working = shownView != 5;
            int arrays = shownView >= 3 ? 64 : 0;
            if (machine.isStationWorking() != working || machine.getAstralArrayCount() != arrays) {
                frames = 0;
                return;
            }
            if (!materialIconsChecked) verifyMaterialIcons();
            if (!clientMkIVState(working)) {
                frames = 0;
                return;
            }
            if (++frames == 1) {
                firstRenderedFrame = LightHunterSpaceStationRenderer.getRenderedFrames();
                firstBeamFrame = LightHunterSpaceStationRenderer.getBeamFrames();
            }
            if (frames < 180) return;
            if (working) {
                require(LightHunterSpaceStationRenderer.isAvailable(),
                    "Original OBJ models and both beam shaders load");
                require(
                    LightHunterSpaceStationRenderer.getLastPlanetCount() == 13
                        && LightHunterSpaceStationRenderer.getLastBeamPassCount() == 2,
                    "Actual client renders thirteen planets and both original beam layers");
                require(
                    LightHunterSpaceStationRenderer.getRenderedFrames() > firstRenderedFrame
                        && LightHunterSpaceStationRenderer.getBeamFrames() > firstBeamFrame,
                    "Native TESR and GTNHLib deferred beam both submit real frames");
                require(LightHunterSpaceStationRenderer.wasLastParadoxical() == (arrays == 64),
                    "Full-array state selects the original space+halo branch");
            } else require(LightHunterSpaceStationRenderer.getRenderedFrames() == firstRenderedFrame,
                "Stopped station submits no new celestial effect");
            String[] names = { "station-overview", "stellar-planets-orbits", "station-beam", "paradoxical-overview",
                "paradoxical-space-halo", "station-stopped", "station-restarted" };
            capture(names[shownView]);
            report.append("Native client view ").append(names[shownView]).append(" captured.\n");
            capturedView = shownView;
            frames = viewFrames = 0;
            if (capturedView + 1 < names.length) requestedView = capturedView + 1;
            else requestedGui = 0;
        } catch (Throwable failure) {
            try {
                capture("failure-current-screen");
            } catch (Throwable screenshotFailure) {
                failure.addSuppressed(screenshotFailure);
            }
            fail(failure);
        }
    }

    private void verifyMaterialIcons() {
        require(LightHunterCasing.values().length == 20, "All twenty original adapted casing variants are registered");
        for (LightHunterCasing casing : LightHunterCasing.values()) for (int side = 0; side < 6; side++) {
            var icon = casing.getBlock().getIcon(side, casing.getMetadata());
            require(icon != null && !icon.getIconName().contains("missing"), "Original casing icon resolves " + casing);
            if (casing.hasActiveAppearance()) {
                var active = casing.getBlock().getIcon(side, casing.getActiveMetadata());
                require(active != null && !active.getIconName().contains("missing"),
                    "Original active casing icon resolves " + casing);
            }
        }
        materialIconsChecked = true;
        report.append("All20 casing variants and four active variants resolve their real native icons.\n");
    }

    private boolean clientMkIVState(boolean active) {
        int count = 0;
        World world = Minecraft.getMinecraft().theWorld;
        for (int[] local : mkIVPositions) {
            int[] pos = position(station, local, 36, 36, 0);
            if (
                world.getBlock(pos[0], pos[1], pos[2]) != LightHunterCasing.FusionCasingMkIV.getBlock()
                    || world.getBlockMetadata(pos[0], pos[1], pos[2])
                        != (active ? LightHunterCasing.FusionCasingMkIV.getActiveMetadata()
                            : LightHunterCasing.FusionCasingMkIV.getMetadata())
            ) return false;
            count++;
        }
        require(count == 64, "Original station contains exactly64 MkIV active coils");
        return true;
    }

    private void captureGui() throws Exception {
        require(++guiStageFrames <= 1800, "Native Light Hunter GUI timeout stage" + openedGui);
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
        String expectedSync = switch (openedGui) {
            case 0 -> "lightHunterArrays";
            case 1 -> "lightHunterMinimumDuration";
            default -> "threadMultiplier";
        };
        if (wrapper.getScreen().getSyncManager().getMainPSM().findSyncHandlerNullable(expectedSync, 0) == null) {
            frames = 0;
            return;
        }
        List<IWidget> widgets = new ArrayList<>();
        collect(wrapper.getScreen().getMainPanel(), widgets);
        if (++frames < 90) return;
        List<TextWidget<?>> text = new ArrayList<>();
        for (IWidget widget : widgets) if (widget instanceof TextWidget<?>value) text.add(value);
        if (openedGui == 0) {
            requireText(text, StatCollector.translateToLocalFormatted("gtng.light_hunter.arrays", 64));
            requireText(text, StatCollector.translateToLocalFormatted("gtng.light_hunter.modules", 1));
            var sync = wrapper.getScreen().getSyncManager().getMainPSM();
            require(
                sync.findSyncHandler("lightHunterArrays", IntSyncValue.class).getIntValue() == 64
                    && sync.findSyncHandler("lightHunterModules", IntSyncValue.class).getIntValue() == 1,
                "Actual station GUI synchronizes64 arrays and1 connected module");
        } else if (openedGui == 1) {
            requireText(text, StatCollector.translateToLocalFormatted("gtng.light_hunter.satellite.batch", 2, "5"));
            requireText(text, StatCollector.translateToLocal("gtng.light_hunter.satellite.minimum_duration"));
            var sync = wrapper.getScreen().getSyncManager().getMainPSM();
            require(sync.findSyncHandler("lightHunterMinimumDuration", IntSyncValue.class).getIntValue() == 1200,
                "Satellite GUI synchronizes the actual minimum batch duration");
            boolean inputVisible = false;
            for (IWidget widget : widgets) if (widget instanceof TextFieldWidget field)
                inputVisible |= field.getText().replaceAll("[^0-9]", "").equals("1200");
            require(inputVisible, "Native minimum-duration input displays1200 ticks");
            long icons = widgets.stream().filter(widget -> widget instanceof ItemDisplayWidget).count();
            require(icons == guiExpectedOutputs.size(), "Actual satellite GUI has all native batch output icons");
            for (NBTTagCompound output : guiExpectedOutputs) {
                requireText(text, WirelessRecipeDisplay.name(output));
                requireText(text,
                    "x " + WirelessRecipeDisplay.amount(output, true)
                        + " ("
                        + WirelessRecipeDisplay.rate(output, true)
                        + ")");
            }
        } else {
            requireText(text,
                StatCollector.translateToLocalFormatted("gtng.light_hunter.thread_modifier.multiplier", 2));
            requireText(text,
                StatCollector.translateToLocalFormatted("gtng.light_hunter.thread_modifier.threads", "8,192"));
            var sync = wrapper.getScreen().getSyncManager().getMainPSM();
            require(
                sync.findSyncHandler("threadMultiplier", IntSyncValue.class).getIntValue() == 2
                    && sync.findSyncHandler("additionalThreads", IntSyncValue.class).getIntValue() == 8192,
                "Actual thread hatch GUI synchronizes2 multiplier and8192 extra threads");
        }
        if (openedGui == 1 && !satelliteOverviewCaptured) {
            capture("satellite-gui-batch-outputs");
            for (IWidget widget : widgets) if (widget instanceof ListWidget list)
                list.getScrollData().scrollTo(list.getScrollArea(), Integer.MAX_VALUE);
            satelliteOverviewCaptured = true;
            frames = 60;
            return;
        }
        String[] names = { "station-gui-counts", "satellite-gui-output-amounts", "thread-hatch-gui" };
        capture(names[openedGui]);
        report.append("Native MUI2 view ").append(names[openedGui]).append(" synchronized; new text fits its panel.\n");
        frames = guiStageFrames = 0;
        if (openedGui < 2) requestedGui = openedGui + 1;
        else openAcquisition();
    }

    private static void collect(IWidget widget, List<IWidget> result) {
        result.add(widget);
        for (IWidget child : widget.getChildren()) collect(child, result);
    }

    private static String plain(String text) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(text);
    }

    private static void requireText(List<TextWidget<?>> widgets, String expected) {
        String plain = plain(expected);
        TextWidget<?> found = widgets.stream().filter(widget -> plain(widget.getKey().getFormatted()).equals(plain))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Native GUI missing text: " + plain
                + "; actual="
                + widgets.stream().map(widget -> plain(widget.getKey().getFormatted())).collect(Collectors.toList())));
        float right = found.getArea().x() + found.getArea().paddedWidth();
        IWidget parent = found;
        while (parent != found.getPanel()) {
            parent = parent.getParent();
            if (parent.getArea().w() > 0)
                right = Math.min(right, parent.getArea().x() + parent.getArea().paddedWidth());
        }
        float width = Minecraft.getMinecraft().fontRenderer.getStringWidth(found.getKey().getFormatted())
            * found.getScale();
        require(found.getArea().x() + width <= right + 1, "Native GUI text fits actual rendered panel width: " + plain);
    }

    private void openAcquisition() throws Exception {
        List<GTRecipe.RecipeAssemblyLine> recipes = new ArrayList<>();
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes)
            if (GTUtility.areStacksEqual(recipe.mOutput, GTNGItemList.LightHunterSpaceStation.get(1)))
                recipes.add(recipe);
        require(recipes.size() == 1, "Exactly one native Light Hunter acquisition recipe");
        GTRecipe.RecipeAssemblyLine recipe = recipes.get(0);
        require(
            recipe.mInputs.length == 12 && recipe.mFluidInputs.length == 3
                && recipe.mDuration == 2400
                && recipe.mEUt == TierEU.RECIPE_UXV
                && recipe.mFluidInputs[1].isFluidEqual(Materials.UUMatter.getFluid(1)),
            "Main station acquisition has its real native UU fluid lane and UXV gate");
        for (FluidStack input : recipe.mFluidInputs)
            require(input != null && input.amount == 1_600_000, "Original native acquisition fluid quantity");
        GTMod.proxy.reloadNEICache();
        acquisitionHandler = new GTNEIDefaultHandler(RecipeMaps.assemblylineVisualRecipes.getDefaultRecipeCategory());
        acquisitionHandler.loadCraftingRecipes(GTNGItemList.LightHunterSpaceStation.get(1));
        require(acquisitionHandler.arecipes.size() == 1, "Native NEI selects only the station acquisition recipe");
        ArrayList<ICraftingHandler> handlers = new ArrayList<>();
        handlers.add(acquisitionHandler);
        var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
        constructor.setAccessible(true);
        Minecraft.getMinecraft().displayGuiScreen(constructor.newInstance(handlers));
        frames = 0;
    }

    private void captureAcquisition() throws Exception {
        require(++frames <= 1200, "Light Hunter acquisition NEI timeout");
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe) || frames < 90) return;
        GTNEIDefaultHandler.CachedDefaultRecipe cached = (GTNEIDefaultHandler.CachedDefaultRecipe) acquisitionHandler.arecipes
            .get(0);
        List<FluidStack> fluids = new ArrayList<>();
        for (PositionedStack input : cached.getIngredients()) {
            require(input.item != null && input.item.getIconIndex() != null, "Native acquisition input icon resolves");
            if (StackInfo.isFluidDisplayItem(input.item)) fluids.add(StackInfo.getFluid(input.item));
        }
        require(fluids.size() == 3 && fluids.get(1).isFluidEqual(Materials.UUMatter.getFluid(1))
            && fluids.get(1).amount == 1_600_000, "Actual NEI displays1600000mB UU as the second fluid input");
        capture("station-acquisition-nei");
        report.append("Native station acquisition NEI: real item icons and1600000mB UU in fluid slot2.\n");
        finish();
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
        System.out.println("LIGHT_HUNTER_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File directory = new File(System.getProperty("gtng.lighthunter.output"));
        directory.mkdirs();
        return directory;
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
