package com.xyp.gtnotgood.qa;

import static gregtech.api.util.GTRecipeConstants.CLEANROOM;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.Language;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiModeMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.MageAssemblerStructure;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperconductingElectromagneticStructure;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.ContainerNull;
import appeng.helpers.UltimatePatternHelper;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import goodgenerator.util.ItemRefer;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.ICleanroomReceiver;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEWirelessEnergy;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.FakeCleanroom;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.data.GTCoilTracker;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gtPlusPlus.core.material.MaterialsAlloy;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

@Mod(
    modid = "assemblyplantqa",
    name = "Assembly Plant QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class AssemblyPlantClientChecks {

    private BaseMetaTileEntity base;
    private volatile GTNGMultiModeMachineBase<?> machine;
    private volatile Rig rig;
    private Rig[] rigs;
    private int rigIndex;
    private Item fixture;
    private String[][] shape;
    private List<int[]> hatchSlots;
    private Map<Character, ItemStack> builtBlocks;
    private GTRecipe[] modeFixtures;
    private GTRecipe fluidFixture, voltageFixture;
    private final StringBuilder report = new StringBuilder();
    private boolean started;
    private volatile boolean checked, finished, requestedNextRig, finishRequested;
    private volatile boolean completeClientStructure, observedActiveCoils, observedInactiveCoils;
    private volatile int clientStructureBlocks, clientActiveCoils, clientInactiveCoils, clientGeometryStableTicks;
    private volatile ClientBlock[] expectedClientBlocks;
    private RunningPhase runningPhase;
    private int phaseStartedTick;
    private volatile int requestedView = -1, shownView = -1, openedMode = -1;
    private volatile boolean requestedGui;
    private int clientTicks, frames, stageFrames, capturedView = -1, warmedView = -1, capturedMode = -1;
    private volatile int serverTicks;
    private int tooltipStage = -1, savedProgress;
    private int[] brokenCasing;
    private MultiblockTooltipBuilder currentTooltip;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.assemblyPlant.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("assemblyPlantFixture");
        GameRegistry.registerItem(fixture, "fixture");
        rigs = new Rig[] {
            new Rig("mage-assembler", GTNGItemList.MageAssembler, GTNGMachineID.MageAssembler.id,
                MageAssemblerStructure.shape(), 4, 5, 0, 'a', "cdefghi", "f", 1377, 55, 0,
                new RecipeMap<?>[] { RecipeMaps.assemblerRecipes, GoodGeneratorRecipeMaps.preciseAssemblerRecipes,
                    RecipeMaps.circuitAssemblerRecipes },
                new double[][] { { -32, 35, -25, -38, 21.4 }, { -9, 20, -5, -45, 24 } }),
            new Rig("electromagnetic-factory", GTNGItemList.SuperconductingElectromagneticFactory,
                GTNGMachineID.SuperconductingElectromagneticFactory.id, SuperconductingElectromagneticStructure.shape(),
                7, 20, 0, 'F', "ABCDEH", "CDE", 1628, 496, 96,
                new RecipeMap<?>[] { RecipeMaps.polarizerRecipes, RecipeMaps.electroMagneticSeparatorRecipes,
                    RecipeMaps.electrolyzerRecipes, GTNGRecipeMaps.LightningProcessorRecipes,
                    RecipeMaps.arcFurnaceRecipes },
                new double[][] { { 58, 48, -32, -44.3, 23.5 }, { 84, 25, -6, -53, 14 } }) };
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 14400, "Client QA timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = tooltipStage >= 0 ? 1 : 2;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0F);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 12;
            if (mc.theWorld != null) observeClientStructure(mc);
            if (finishRequested) {
                finish();
                return;
            }
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("assembly-plant-qa-" + System.currentTimeMillis(), "Assembly Plant QA",
                    new WorldSettings(733L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || finishRequested) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++serverTicks <= 12000, "Server QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) {
                player.worldObj.setWorldTime(6000);
                selectRig(player, 0);
            }
            if (serverTicks == 60) prepareChecks(player);
            if (requestedNextRig) {
                requestedNextRig = false;
                base.disableWorking();
                machine.stopMachine(ShutDownReasonRegistry.NONE);
                cleanupFixtures();
                if (++rigIndex == rigs.length) {
                    finishRequested = true;
                    return;
                }
                selectRig(player, rigIndex);
                prepareChecks(player);
            }
            if (modeFixtures != null) chargeEnergy();
            if (runningPhase != null) advanceRunningChecks();
            if (checked && requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                player.closeScreen();
                double[] camera = rig.cameras[view];
                player.playerNetServerHandler.setPlayerLocation(camera[0], camera[1], camera[2], (float) camera[3],
                    (float) camera[4]);
                shownView = view;
            }
            if (requestedGui) {
                requestedGui = false;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(rig.originX, 17, -4, 0, 0);
                machine.setMachineMode(0);
                machine.onRightclick(base, player);
                openedMode = 0;
            }
            if (openedMode >= 0) openedMode = machine.getMachineMode();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void selectRig(EntityPlayerMP player, int index) {
        expectedClientBlocks = null;
        rig = rigs[index];
        shape = rig.shape;
        modeFixtures = null;
        checked = false;
        runningPhase = null;
        observedActiveCoils = observedInactiveCoils = completeClientStructure = false;
        clientStructureBlocks = clientActiveCoils = clientInactiveCoils = clientGeometryStableTicks = 0;
        requestedView = shownView = openedMode = capturedView = capturedMode = warmedView = -1;
        requestedGui = false;
        tooltipStage = -1;
        frames = stageFrames = 0;
        require(rig.controller.get(1).getItemDamage() == rig.machineId, "Registered controller identity " + rig.slug);
        buildRig(player, rig.originX, 16, 0, ForgeDirection.NORTH);
        player.playerNetServerHandler.setPlayerLocation(rig.originX, 32, (shape[0].length - 1) / 2.0, 180, 70);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private void prepareChecks(EntityPlayerMP player) {
        verifyStructure();
        verifyDirections(player);
        verifyRequiredBlocks();
        verifyUnconstrainedSpace();
        verifyWirelessHatches();
        verifySurvivalRecipes();
        verifyNativeModes();
        registerFixtures();
        verifyParallelAndOverclock();
        verifyVoltageGate();
        verifyOutputProtection();
        verifyNativeMeBuffer();
        resetProcessing();
        machine.stopMachine(ShutDownReasonRegistry.NONE);
        cacheClientStructure();
        beginPhase(RunningPhase.WaitStructure);
    }

    private void beginPhase(RunningPhase phase) {
        runningPhase = phase;
        phaseStartedTick = serverTicks;
        System.out.println("ASSEMBLY_PLANT_QA_PHASE " + rig.slug + " " + phase + " at " + serverTicks);
    }

    private void advanceRunningChecks() {
        require(serverTicks - phaseStartedTick <= 1200,
            "Timed out in " + rig.slug
                + " "
                + runningPhase
                + "; blocks="
                + clientStructureBlocks
                + "/"
                + rig.requiredBlocks
                + ", active="
                + clientActiveCoils
                + ", inactive="
                + clientInactiveCoils);
        switch (runningPhase) {
            case WaitStructure -> {
                if (!completeClientStructure || clientInactiveCoils != rig.coils || clientGeometryStableTicks < 20)
                    return;
                verifyCoils(false);
                startRunning();
                beginPhase(RunningPhase.WaitActive);
            }
            case WaitActive -> {
                if (!base.isActive() || machine.mProgresstime <= 0 || clientActiveCoils != rig.coils) return;
                verifyCoils(true);
                base.disableWorking();
                machine.stopMachine(ShutDownReasonRegistry.NONE);
                beginPhase(RunningPhase.WaitStopped);
            }
            case WaitStopped -> {
                if (serverTicks <= phaseStartedTick || clientInactiveCoils != rig.coils) return;
                verifyCoils(false);
                startRunning();
                beginPhase(RunningPhase.WaitRestarted);
            }
            case WaitRestarted -> {
                if (!base.isActive() || clientActiveCoils != rig.coils) return;
                verifyCoils(true);
                brokenCasing = firstPosition(rig.fixedSymbols.charAt(0));
                base.getWorld().setBlockToAir(brokenCasing[0], brokenCasing[1], brokenCasing[2]);
                require(!machine.checkStructure(true, base), "Running structure rejects missing block " + rig.slug);
                base.disableWorking();
                beginPhase(RunningPhase.WaitBroken);
            }
            case WaitBroken -> {
                if (serverTicks <= phaseStartedTick || clientInactiveCoils != rig.coils) return;
                verifyCoils(false);
                ItemStack casing = builtBlocks.get(rig.fixedSymbols.charAt(0));
                base.getWorld().setBlock(brokenCasing[0], brokenCasing[1], brokenCasing[2],
                    Block.getBlockFromItem(casing.getItem()), casing.getItemDamage(), 3);
                machine.stopMachine(ShutDownReasonRegistry.NONE);
                startRunning();
                beginPhase(RunningPhase.WaitRepaired);
            }
            case WaitRepaired -> {
                if (!completeClientStructure || !base.isActive() || clientActiveCoils != rig.coils) return;
                verifyCoils(true);
                verifyPersistence();
                beginPhase(RunningPhase.WaitReloadedProgress);
            }
            case WaitReloadedProgress -> {
                if (
                    serverTicks - phaseStartedTick < 10 || machine.mProgresstime <= savedProgress
                        || clientActiveCoils != rig.coils
                        || !completeClientStructure
                ) return;
                verifyCoils(true);
                report.append(rig.slug).append(": full client geometry, native coil activation/deactivation, broken ")
                    .append("structure recovery and running tile reload with advancing progress passed.\n");
                runningPhase = null;
                checked = true;
                requestedView = 0;
            }
        }
    }

    private void buildRig(EntityPlayerMP player, int x, int y, int z, ForgeDirection facing) {
        World world = player.worldObj;
        for (int cx = (x - 48) >> 4; cx <= (x + 48) >> 4; cx++)
            for (int cz = (z - 48) >> 4; cz <= (z + 48) >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
        base = place(world, x, y, z, rig.machineId);
        base.setFrontFacing(facing);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        machine = (GTNGMultiModeMachineBase<?>) base.getMetaTileEntity();
        ((ISurvivalConstructable) machine).construct(new ItemStack(Items.diamond), false);
        if (rigIndex == 0) {
            int[] muffler = firstPosition('-');
            place(world, muffler[0], muffler[1], muffler[2], ItemList.Hatch_Muffler_EV.get(1).getItemDamage());
        }
        verifyConstructedGeometry();
        hatchSlots = localPositions(rig.hatchSymbol);
        hatchSlots.sort(Comparator.<int[]>comparingInt(pos -> pos[2]).thenComparingInt(pos -> pos[1])
            .thenComparingInt(pos -> pos[0]));
        require(hatchSlots.size() >= 14, "Enough original hatch positions " + rig.slug);
        placeSlot(0, ItemList.Hatch_Input_Bus_EV.get(1));
        placeSlot(1, ItemList.Hatch_Output_Bus_EV.get(1));
        for (int slot = 2; slot <= 7; slot++) placeSlot(slot, ItemList.Hatch_Input_UV.get(1));
        if (rigIndex == 1) {
            placeSlot(8, ItemList.Hatch_Output_UV.get(1));
            placeSlot(9, ItemList.Hatch_Output_UV.get(1));
        }
        placeSlot(10, ItemList.Hatch_Maintenance.get(1));
        setEnergy(ItemList.Hatch_Energy_UHV);
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(1);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
    }

    private void verifyConstructedGeometry() {
        builtBlocks = new HashMap<>();
        int total = 0;
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++) {
                char symbol = shape[row][depth].charAt(col);
                if (symbol == ' ' || symbol == '~') continue;
                int[] pos = position(col, row, depth);
                Block block = base.getWorld().getBlock(pos[0], pos[1], pos[2]);
                require(block != Blocks.air, "Required original block " + rig.slug + " " + symbol);
                ItemStack stack = new ItemStack(block, 1, base.getWorld().getBlockMetadata(pos[0], pos[1], pos[2]));
                ItemStack expected = builtBlocks.putIfAbsent(symbol, stack);
                require(expected == null || GTUtility.areStacksEqual(expected, stack),
                    "Uniform source symbol " + symbol);
                total++;
            }
        require(total == rig.requiredBlocks, "Complete original block count " + rig.slug);
        if (rigIndex == 0) {
            require(shape[5][0].charAt(4) == '~' && shape[5][32].charAt(4) == '-'
                && shape[9][16].equals("aaaaaaaaa")
                && shape[0][16].equals("   aha   "), "Assembler front/rear and lower/upper source landmarks");
            require(position(4, 9, 16)[1] == base.getYCoord() - 4 && position(4, 0, 16)[1] == base.getYCoord() + 5,
                "Assembler preserves source UP axis");
        } else {
            require(
                shape[20][0].charAt(7) == '~' && shape[0][9].charAt(7) == 'F'
                    && shape[19][1].charAt(5) == 'F'
                    && shape[10][7].charAt(7) == 'H',
                "Electromagnetic tower keeps lower controller, upper hatch and central pipeline");
            require(position(7, 20, 0)[1] == base.getYCoord() && position(7, 0, 7)[1] == base.getYCoord() + 20,
                "Tower rises above its original lower controller");
        }
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(), rig.slug + " structure: "
            + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Physical structure forms " + rig.slug);
        require(
            machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1
                && machine.mInputHatches.size() == 6
                && machine.mMaintenanceHatches.size() == 1
                && (!machine.mEnergyHatches.isEmpty() || !machine.getExoticEnergyHatches().isEmpty()),
            "All required physical hatches are discovered " + rig.slug);
        if (rigIndex == 0) require(machine.mMufflerHatches.size() == 1, "Rear muffler is required and discovered");
        else require(machine.mOutputHatches.size() == 2, "Two physical fluid output hatches are discovered");
    }

    private void verifyDirections(EntityPlayerMP player) {
        BaseMetaTileEntity mainBase = base;
        GTNGMultiModeMachineBase<?> mainMachine = machine;
        Map<Character, ItemStack> mainBlocks = builtBlocks;
        List<int[]> mainSlots = hatchSlots;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST }) {
            buildRig(player, 256, 32, 256, facing);
            verifyStructure();
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
                for (int col = 0; col < shape[row][depth].length(); col++) {
                    int[] pos = position(col, row, depth);
                    base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
                }
        }
        base = mainBase;
        machine = mainMachine;
        builtBlocks = mainBlocks;
        hatchSlots = mainSlots;
        verifyStructure();
        report.append(rig.slug).append(": every original block and source-axis landmark forms in four directions.\n");
    }

    private void verifyRequiredBlocks() {
        for (char symbol : rig.fixedSymbols.toCharArray()) {
            int[] pos = firstPosition(symbol);
            World world = base.getWorld();
            Block block = world.getBlock(pos[0], pos[1], pos[2]);
            int metadata = world.getBlockMetadata(pos[0], pos[1], pos[2]);
            world.setBlockToAir(pos[0], pos[1], pos[2]);
            require(!machine.checkStructure(true, base),
                "Missing source material invalidates " + rig.slug + " " + symbol);
            world.setBlock(pos[0], pos[1], pos[2], block, metadata, 3);
            verifyStructure();
        }
        if (rigIndex == 0) {
            int[] pos = firstPosition('-');
            worldBlock(pos, Blocks.air, 0);
            require(!machine.checkStructure(true, base), "Reserved '-' remains a required muffler, never air");
            place(base.getWorld(), pos[0], pos[1], pos[2], ItemList.Hatch_Muffler_EV.get(1).getItemDamage());
            verifyStructure();
        }
        report.append(rig.slug)
            .append(": missing fixed materials and the original muffler requirement are enforced.\n");
    }

    private void verifyUnconstrainedSpace() {
        int[] pos = firstPosition(' ');
        worldBlock(pos, Blocks.stone, 0);
        verifyStructure();
        worldBlock(pos, Blocks.air, 0);
        verifyStructure();
        report.append(rig.slug).append(": original any positions accept both solid blocks and air.\n");
    }

    private void verifyWirelessHatches() {
        for (ItemStack stack : new ItemStack[] { ItemList.Wireless_Hatch_Energy_LV.get(1),
            GTNGItemList.WirelessMultiAmpEnergyUV256A.get(1), GTNGItemList.WirelessMultiAmpEnergyUXV256A.get(1) }) {
            placeSlot(11, stack);
            verifyStructure();
            int[] pos = slotPosition(11);
            BaseMetaTileEntity wirelessBase = (BaseMetaTileEntity) base.getWorld().getTileEntity(pos[0], pos[1],
                pos[2]);
            UUID owner = UUID.randomUUID();
            wirelessBase.setOwnerName(base.getOwnerName());
            wirelessBase.setOwnerUuid(owner);
            wirelessBase.setStoredEU(0);
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            BigInteger credit = BigInteger.valueOf(Long.MAX_VALUE / 4);
            WirelessNetworkManager.setUserEU(owner, credit);
            var wireless = wirelessBase.getMetaTileEntity();
            require(wireless instanceof MTEWirelessEnergy || wireless instanceof MTEHatchWirelessMulti,
                "Ordinary native wireless energy implementation is installed");
            wireless.onFirstTick(wirelessBase);
            long funded = wirelessBase.getStoredEU();
            require(
                funded > 32
                    && WirelessNetworkManager.getUserEU(owner).equals(credit.subtract(BigInteger.valueOf(funded))),
                "Physical wireless hatch debits its native owner account and fills its EU buffer exactly");
            BigInteger account = WirelessNetworkManager.getUserEU(owner);
            require(
                machine.drainEnergyInput(32) && wirelessBase.getStoredEU() == funded - 32
                    && WirelessNetworkManager.getUserEU(owner).equals(account),
                "Controller draws native EU from its installed wireless hatch buffer");
            WirelessNetworkManager.setUserEU(owner, BigInteger.ZERO);
            wirelessBase.setStoredEU(0);
            if (wireless instanceof MTEWirelessEnergy energy) energy.tryFetchingEnergy();
            if (wireless instanceof MTEHatchWirelessMulti multi) multi.tryFetchingEnergy();
            require(
                wirelessBase.getStoredEU() == 0 && !machine.drainEnergyInput(32)
                    && WirelessNetworkManager.getUserEU(owner).signum() == 0,
                "An unfunded wireless hatch cannot supply machine power");
            setEnergy(ItemList.Hatch_Energy_UHV);
            verifyStructure();
        }
        report.append(rig.slug).append(": native ordinary, project multi-amp and native TecTech wireless hatches ")
            .append("form, debit their owner accounts and supply the controller through real physical EU buffers.\n");
    }

    private void verifySurvivalRecipes() {
        GTNGItemList[] outputs = { GTNGItemList.MageAssembler, GTNGItemList.SpacetimeAssemblyLineUnit,
            GTNGItemList.SuperconductingElectromagneticFactory };
        int[] itemCounts = { 16, 11, 13 }, durations = { 800, 400, 1200 };
        long[] tiers = { TierEU.RECIPE_UHV, TierEU.RECIPE_UHV, TierEU.RECIPE_UEV };
        for (int index = 0; index < outputs.length; index++) {
            ItemStack output = outputs[index].get(1);
            List<GTRecipe.RecipeAssemblyLine> matches = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, output)).collect(Collectors.toList());
            require(matches.size() == 1, "One native assembly-line survival recipe " + outputs[index]);
            var recipe = matches.get(0);
            require(
                recipe.mInputs.length == itemCounts[index] && recipe.mFluidInputs.length == 4
                    && recipe.mDuration == durations[index]
                    && recipe.mEUt == tiers[index]
                    && recipe.mResearchItem != null
                    && recipe.mResearchTime == 2400
                    && recipe.mResearchVoltage == tiers[index],
                "Complete survival ingredient lanes and gates " + outputs[index]);
            validateIngredients(recipe.mInputs, recipe.mFluidInputs);
            if (
                index == 1
            ) require(
                GTUtility.areStacksEqual(recipe.mInputs[2], ItemRefer.Precise_Electronic_Unit_T4.get(1))
                    && recipe.mInputs[2].stackSize == 1,
                "Every spacetime unit consumes the native highest precise unit");
            if (index == 2) {
                ItemStack wire = GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.SuperconductorZPMBase, 1);
                require(wire != null, "Native ZPM superconductor fine wire exists");
                for (int slot = 5; slot <= 8; slot++) require(
                    recipe.mInputs[slot].stackSize == 64 && GTUtility.areStacksEqual(wire, recipe.mInputs[slot]),
                    "Four separate 64-fine-wire gates");
            }
        }
        for (AssemblyPlantCasing casing : AssemblyPlantCasing.values()) {
            if (casing == AssemblyPlantCasing.SpacetimeAssemblyLineUnit) continue;
            if (casing == AssemblyPlantCasing.AdvancedAssemblyLineUnit) {
                require(
                    CraftingManager.getInstance().getRecipeList().stream()
                        .anyMatch(recipe -> recipe != null
                            && GTUtility.areStacksEqual(recipe.getRecipeOutput(), casing.getStack(1))),
                    "Advanced assembly unit has a native crafting recipe");
                continue;
            }
            List<GTRecipe> matches = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
                .filter(recipe -> Arrays.stream(recipe.mOutputs)
                    .anyMatch(output -> output != null && GTUtility.areStacksEqual(output, casing.getStack(1))))
                .collect(Collectors.toList());
            require(matches.size() == 1, "One native assembler survival recipe " + casing);
            GTRecipe recipe = matches.get(0);
            int items = casing == AssemblyPlantCasing.ElectrolyticCell ? 4
                : casing == AssemblyPlantCasing.MolybdenumDisilicideCoil ? 3 : 2;
            int fluids = casing == AssemblyPlantCasing.MolybdenumDisilicideCoil ? 1 : 0;
            require(recipe.mInputs.length == items && recipe.mFluidInputs.length == fluids,
                "No missing native material was silently removed from " + casing);
            validateIngredients(recipe.mInputs, recipe.mFluidInputs);
            if (
                casing == AssemblyPlantCasing.MolybdenumDisilicideCoil
            ) require(
                recipe.mFluidInputs[0].isFluidEqual(Materials.TungstenSteel.getMolten(144))
                    && recipe.mFluidInputs[0].amount == 144,
                "Molybdenum disilicide coils use obtainable native tungstensteel instead of RotaryCraft-only HSLA");
        }
        verifyRegisteredSurvivalMaterials();
        report.append(rig.slug).append(": controller and all eight block survival recipes have complete native inputs ")
            .append("and retain the highest precise unit and four ZPM fine-wire gates.\n");
    }

    private void verifyRegisteredSurvivalMaterials() {
        List<String> missing = new ArrayList<>();
        StringBuilder audit = new StringBuilder("ASSEMBLY_PLANT_ORE_AUDIT\n");
        ItemStack[] mage = survivalInputs(GTNGItemList.MageAssembler);
        ItemStack[] spacetime = survivalInputs(GTNGItemList.SpacetimeAssemblyLineUnit);
        ItemStack[] electromagnetic = survivalInputs(GTNGItemList.SuperconductingElectromagneticFactory);
        auditSurvivalInputs("mage", mage, audit);
        auditSurvivalInputs("spacetime", spacetime, audit);
        auditSurvivalInputs("electromagnetic", electromagnetic, audit);
        for (Materials material : new Materials[] { Materials.TungstenCarbide, Materials.YttriumBariumCuprate,
            Materials.NaquadahAlloy }) auditSurvivalOre("screw" + material.mName, mage, missing, audit);
        auditSurvivalOre("screw" + WerkstoffLoader.RhodiumPlatedPalladium.getVarName(), mage, missing, audit);
        for (Materials material : new Materials[] { Materials.TungstenCarbide, Materials.TungstenSteel,
            Materials.Ultimet }) auditSurvivalOre("stickLong" + material.mName, mage, missing, audit);
        auditSurvivalOre("stickLong" + MaterialsAlloy.INCOLOY_MA956.getUnlocalizedName(), mage, missing, audit);
        auditSurvivalOre("plate" + MaterialsAlloy.LAFIUM.getUnlocalizedName(), mage, missing, audit);
        auditSurvivalOre("plateDouble" + Materials.Iridium.mName, mage, missing, audit);
        auditSurvivalOre("plateDouble" + MaterialsAlloy.PIKYONIUM.getUnlocalizedName(), spacetime, missing, audit);
        auditSurvivalOre("frameGt" + Materials.Adamantium.mName, spacetime, missing, audit);
        auditSurvivalOre("gearGt" + Materials.BlueSteel.mName, spacetime, missing, audit);
        auditSurvivalOre("gearGt" + Materials.RedSteel.mName, spacetime, missing, audit);
        auditSurvivalOre("wireFine" + Materials.SuperconductorZPMBase.mName, electromagnetic, missing, audit);
        for (Materials material : new Materials[] { Materials.Tantalum, Materials.Silver, Materials.Silicon })
            auditSurvivalOre("plateDouble" + material.mName, electromagnetic, missing, audit);
        auditSurvivalOre("plate" + WerkstoffLoader.Ruthenium.getVarName(), electromagnetic, missing, audit);
        List<ItemStack> advancedInputs = new ArrayList<>();
        for (var recipe : CraftingManager.getInstance().getRecipeList()) {
            if (
                recipe instanceof ShapedOreRecipe shaped && GTUtility.areStacksEqual(recipe.getRecipeOutput(),
                    AssemblyPlantCasing.AdvancedAssemblyLineUnit.getStack(1))
            ) {
                for (Object ingredient : shaped.getInput()) {
                    if (ingredient instanceof ItemStack stack) advancedInputs.add(stack);
                    else if (ingredient instanceof Iterable<?>alternatives) {
                        for (Object alternative : alternatives)
                            if (alternative instanceof ItemStack stack) advancedInputs.add(stack);
                    }
                }
            }
        }
        ItemStack[] advanced = advancedInputs.toArray(new ItemStack[0]);
        auditSurvivalInputs("advanced", advanced, audit);
        auditSurvivalOre("gearGt" + WerkstoffLoader.Rhodium.getVarName(), advanced, missing, audit);
        report.append(audit);
        System.out.print(audit);
        require(missing.isEmpty(), "Unregistered survival material forms: " + String.join("; ", missing));
    }

    private static ItemStack[] survivalInputs(GTNGItemList output) {
        return GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, output.get(1))).findFirst()
            .orElseThrow(() -> new AssertionError("Missing survival recipe " + output)).mInputs;
    }

    private static void auditSurvivalOre(String oreName, ItemStack[] inputs, List<String> missing,
        StringBuilder audit) {
        List<ItemStack> registered = OreDictionary.getOres(oreName, false);
        List<ItemStack> matches = new ArrayList<>();
        for (ItemStack input : inputs) {
            if (input == null) continue;
            for (int index = 0; index < registered.size(); index++) {
                ItemStack ore = registered.get(index);
                if (ore != null && ore.getItem() == input.getItem() && ore.getItemDamage() == input.getItemDamage()) {
                    matches.add(input);
                    break;
                }
            }
        }
        audit.append(oreName).append(": entries=").append(registered.size()).append(", recipeMatches=")
            .append(matches.size()).append(", listClass=").append(registered.getClass().getName()).append('\n');
        for (int index = 0; index < registered.size(); index++) audit.append("  registered[").append(index).append("] ")
            .append(auditItem(registered.get(index))).append('\n');
        if (matches.isEmpty()) missing.add(oreName + " (registered entries=" + registered.size() + ")");
    }

    private static void auditSurvivalInputs(String name, ItemStack[] inputs, StringBuilder audit) {
        for (int slot = 0; slot < inputs.length; slot++)
            audit.append(name).append(" input[").append(slot).append("] ").append(auditItem(inputs[slot])).append('\n');
    }

    private static String auditItem(ItemStack stack) {
        if (stack == null) return "null";
        return Item.itemRegistry.getNameForObject(stack.getItem()) + "@"
            + stack.getItemDamage()
            + " x"
            + stack.stackSize
            + " ores="
            + Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName)
                .collect(Collectors.joining(","));
    }

    private static void validateIngredients(ItemStack[] items, FluidStack[] fluids) {
        for (ItemStack input : items)
            require(input != null && input.getItem() != null && input.stackSize >= 0, "Runtime survival item exists");
        for (FluidStack input : fluids)
            require(input != null && input.getFluid() != null && input.amount > 0, "Runtime survival fluid exists");
    }

    private void verifyNativeModes() {
        require(new ArrayList<>(machine.getAvailableRecipeMaps()).equals(Arrays.asList(rig.maps)),
            "Native recipe map order " + rig.slug);
        machine.setPowerPanelMaxParallel(1);
        for (int mode = 0; mode < rig.maps.length; mode++) {
            int selectedMode = mode;
            resetProcessing();
            machine.setMachineMode(mode);
            require(machine.getRecipeMap() == rig.maps[mode], "Native mode map " + rig.slug + " " + mode);
            GTRecipe recipe = rig.maps[mode].getAllRecipes().stream()
                .filter(candidate -> candidate.mEUt > 0 && candidate.mEUt <= TierEU.RECIPE_UHV
                    && candidate.mDuration > 0
                    && candidate.mInputs.length <= 16
                    && candidate.mFluidInputs.length <= 6
                    && candidate.mFluidOutputs.length <= (rigIndex == 0 ? 0 : 2)
                    && candidate.mSpecialItems == null
                    && (rigIndex != 0 || selectedMode != 1 || candidate.mSpecialValue <= 4))
                .filter(candidate -> !candidate.getMetadataOrDefault(CLEANROOM, false))
                .filter(candidate -> Arrays.stream(candidate.mInputs)
                    .allMatch(input -> input != null && input.getItem() != null)
                    && Arrays.stream(candidate.mFluidInputs)
                        .allMatch(input -> input != null && input.getFluid() != null)
                    && Arrays.stream(candidate.mOutputs).allMatch(output -> output != null && output.getItem() != null)
                    && Arrays.stream(candidate.mFluidOutputs)
                        .allMatch(output -> output != null && output.getFluid() != null))
                .sorted(Comparator.comparingInt(candidate -> candidate.mEUt))
                .filter(candidate -> rig.maps[selectedMode].findRecipeQuery().items(candidate.mInputs)
                    .fluids(candidate.mFluidInputs).voltage(TierEU.RECIPE_UHV).findAll()
                    .allMatch(match -> Arrays.stream(match.mOutputs).allMatch(output -> output != null)
                        && Arrays.stream(match.mFluidOutputs).allMatch(output -> output != null)))
                .findFirst().orElseThrow(() -> new AssertionError(
                    "No usable native recipe in " + rig.slug + " mode " + machine.getMachineMode()));
            System.out.println("ASSEMBLY_PLANT_NATIVE_RECIPE " + rig.slug
                + " mode="
                + mode
                + " items="
                + Arrays.toString(recipe.mInputs)
                + " fluids="
                + Arrays.toString(recipe.mFluidInputs)
                + " outputs="
                + Arrays.toString(recipe.mOutputs));
            setEnergy(ItemList.Hatch_Energy_UHV);
            verifyStructure();
            seedRecipe(recipe, 1);
            require(machine.checkProcessing().wasSuccessful(),
                "Registered native recipe processes " + rig.slug + " " + mode);
            for (ItemStack input : recipe.mInputs) {
                int remaining = inputCount(input);
                require(input.stackSize > 0 ? remaining == 0 : remaining > 0,
                    "Native recipe consumes material and retains nonconsumables " + rig.slug + " mode " + mode);
            }
            for (var hatch : machine.mInputHatches) require(hatch.getFluidAmount() == 0,
                "Native recipe consumes its exact fluids " + rig.slug + " " + mode);
            require(
                (machine.mOutputItems != null && machine.mOutputItems.length > 0)
                    || (machine.mOutputFluids != null && machine.mOutputFluids.length > 0),
                "Native recipe schedules real products " + rig.slug + " " + mode);
        }
        report.append(rig.slug).append(": all ").append(rig.maps.length).append(
            " native maps process existing recipes with exact fluid/material use and nonconsumable retention.\n");
    }

    private void registerFixtures() {
        modeFixtures = new GTRecipe[rig.maps.length];
        for (int mode = 0; mode < modeFixtures.length; mode++) {
            GTRecipeBuilder.builder().itemInputs(stack(mode * 10, 1)).itemOutputs(stack(mode * 10 + 1, 1)).duration(128)
                .eut(30).addTo(rig.maps[mode]);
            modeFixtures[mode] = fixtureRecipe(rig.maps[mode], mode * 10);
        }
        int fluidMode = rigIndex == 0 ? 0 : 2;
        GTRecipeBuilder.builder().itemInputs(stack(80, 1)).fluidInputs(new FluidStack(FluidRegistry.WATER, 250))
            .itemOutputs(stack(81, 1)).duration(128).eut(30).addTo(rig.maps[fluidMode]);
        fluidFixture = fixtureRecipe(rig.maps[fluidMode], 80);
        GTRecipeBuilder.builder().itemInputs(stack(82, 1)).itemOutputs(stack(83, 1)).duration(128).eut(TierEU.RECIPE_IV)
            .addTo(rig.maps[0]);
        voltageFixture = fixtureRecipe(rig.maps[0], 82);
        verifyModeLockPersistence();
        if (rigIndex == 0) verifyCleanroomGate();
    }

    private GTRecipe fixtureRecipe(RecipeMap<?> map, int meta) {
        return map.getAllRecipes().stream()
            .filter(recipe -> Arrays.stream(recipe.mInputs)
                .anyMatch(input -> input != null && GTUtility.areStacksEqual(input, stack(meta, 1))))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fixture was rejected by native map " + map.unlocalizedName));
    }

    private void verifyCleanroomGate() {
        GTRecipeBuilder.builder().itemInputs(stack(84, 1)).itemOutputs(stack(85, 1)).metadata(CLEANROOM, true)
            .duration(128).eut(30).addTo(rig.maps[0]);
        GTRecipe recipe = fixtureRecipe(rig.maps[0], 84);
        resetProcessing();
        machine.setMachineMode(0);
        setEnergy(ItemList.Hatch_Energy_LV);
        verifyStructure();
        ICleanroomReceiver receiver = machine.getCapability(ICleanroomReceiver.class, ForgeDirection.UNKNOWN);
        require(receiver != null, "Native cleanroom receiver capability exists");
        boolean required = Config.requireCleanroom;
        boolean bypass = FakeCleanroom.CLEANROOM_BYPASS;
        try {
            Config.requireCleanroom = true;
            FakeCleanroom.CLEANROOM_BYPASS = false;
            require(!FakeCleanroom.isCleanroomBypassEnabled(), "Cleanroom fixture enables the existing requirement");
            receiver.setCleanroom(null);
            seedRecipe(recipe, 1);
            var missingCleanroom = machine.checkProcessing();
            require(!missingCleanroom.wasSuccessful() && inputCount(stack(84, 1)) == 1,
                "Cleanroom-required recipe preserves input: " + missingCleanroom.getDisplayString());
            receiver.setCleanroom(FakeCleanroom.INSTANCE);
            require(machine.checkProcessing().wasSuccessful() && outputCount(stack(85, 1)) == 1,
                "A native valid cleanroom provider unlocks the preserved recipe");
        } finally {
            receiver.setCleanroom(null);
            Config.requireCleanroom = required;
            FakeCleanroom.CLEANROOM_BYPASS = bypass;
        }
        report.append("mage-assembler: cleanroom metadata rejects missing providers and accepts a valid native ")
            .append("provider without enabling global cleanroom bypass.\n");
    }

    private void verifyModeLockPersistence() {
        resetProcessing();
        machine.setMachineMode(0);
        setEnergy(ItemList.Hatch_Energy_LV);
        verifyStructure();
        machine.setRecipeLocking(true);
        seedRecipe(modeFixtures[0], 1);
        require(machine.checkProcessing().wasSuccessful() && machine.getSingleRecipeCheck() != null,
            "Native single-recipe locking records the selected recipe " + rig.slug);
        base = reloadTile(base);
        machine = (GTNGMultiModeMachineBase<?>) base.getMetaTileEntity();
        require(
            machine.isRecipeLockingEnabled() && machine.getSingleRecipeCheck() != null
                && machine.getMachineMode() == 0
                && machine.getRecipeMap() == rig.maps[0],
            "Native single-recipe lock and mode persist across a real tile NBT reload");
        verifyStructure();
        machine.setMachineMode(1);
        require(machine.getSingleRecipeCheck() == null,
            "Changing mode clears the previous map's single-recipe checker");
        resetProcessing();
        seedRecipe(modeFixtures[0], 1);
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(0, 1)) == 1,
            "Mode change rejects the previous mode's recipe without consuming inputs");
        seedRecipe(modeFixtures[1], 1);
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(11, 1)) == 1,
            "New mode processes its own recipe after changing a locked map");
        machine.setRecipeLocking(false);
        for (int mode = 0; mode < rig.maps.length; mode++) {
            machine.setMachineMode(mode);
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            machine.setMachineMode((mode + 1) % rig.maps.length);
            machine.loadNBTData(tag);
            require(machine.getMachineMode() == mode && machine.getRecipeMap() == rig.maps[mode],
                "Every native mode survives NBT " + rig.slug + " " + mode);
        }
        report.append(rig.slug).append(": all modes and native recipe locking persist; mode changes clear the old ")
            .append("checker and cannot process inputs from a different map.\n");
    }

    private void verifyParallelAndOverclock() {
        resetProcessing();
        machine.setMachineMode(0);
        machine.setPowerPanelMaxParallel(8);
        setEnergy(ItemList.Hatch_Energy_LV);
        verifyStructure();
        seedRecipe(modeFixtures[0], 5);
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(1, 1)) == 2
            && inputCount(stack(0, 1)) == 3
            && machine.mMaxProgresstime == 128
            && machine.lEUt == -60, "Two-amp LV power limits five inputs to two native parallel operations");
        resetProcessing();
        machine.setPowerPanelMaxParallel(1);
        setEnergy(ItemList.Hatch_Energy_MV);
        verifyStructure();
        seedRecipe(modeFixtures[0], 1);
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(1, 1)) == 1
            && machine.mMaxProgresstime == 32
            && machine.lEUt == -120, "One perfect overclock uses 4x EU/t and one quarter of the duration");
        resetProcessing();
        machine.setPowerPanelMaxParallel(40);
        setEnergy(ItemList.Hatch_Energy_HV);
        placeSlot(13, ItemList.Hatch_Energy_MV.get(1));
        verifyStructure();
        seedRecipe(modeFixtures[0], 64);
        require(
            machine.getMaxInputEu() == 1280 && machine.checkProcessing().wasSuccessful()
                && outputCount(stack(1, 1)) == 40
                && inputCount(stack(0, 1)) == 24
                && machine.lEUt == -1200
                && -machine.lEUt <= machine.getMaxInputEu(),
            "Mixed HV/MV hatches retain their complete 1280 EU/t budget for forty 30-EU/t parallels");
        restoreSlot(13);
        machine.setPowerPanelMaxParallel(1);
        verifyStructure();
        report.append(rig.slug)
            .append(": native power limits parallels and perfect overclocking uses 4x EU/t, 4x speed.\n");
    }

    private void verifyVoltageGate() {
        resetProcessing();
        setEnergy(ItemList.Hatch_Energy_EV);
        placeSlot(13, ItemList.Hatch_Energy_EV.get(1));
        verifyStructure();
        seedRecipe(voltageFixture, 1);
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(82, 1)) == 1,
            "Two EV hatches cannot combine their voltage to unlock an IV recipe");
        restoreSlot(13);
        setEnergy(ItemList.Hatch_Energy_IV);
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(83, 1)) == 1,
            "A wired IV hatch unlocks the preserved IV recipe");
        report.append(rig.slug).append(": voltage gating preserves inputs and respects each wired hatch's voltage.\n");
    }

    private void verifyOutputProtection() {
        resetProcessing();
        machine.setMachineMode(0);
        setEnergy(ItemList.Hatch_Energy_LV);
        verifyStructure();
        var bus = machine.mOutputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        seedRecipe(modeFixtures[0], 3);
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(0, 1)) == 3,
            "Full protected output bus refuses atomically");
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        require(
            machine.checkProcessing().wasSuccessful() && outputCount(stack(1, 1)) == 1 && inputCount(stack(0, 1)) == 2,
            "Clearing output storage permits one exact retry");
        report.append(rig.slug).append(": full protected output storage never consumes an input on failed start.\n");
    }

    private void verifyNativeMeBuffer() {
        resetProcessing();
        machine.setMachineMode(rigIndex == 0 ? 0 : 2);
        placeSlot(12, ItemList.Hatch_CraftingInput_Bus_ME.get(1));
        int[] pos = slotPosition(12);
        BaseMetaTileEntity bufferBase = (BaseMetaTileEntity) base.getWorld().getTileEntity(pos[0], pos[1], pos[2]);
        MTEHatchCraftingInputME buffer = (MTEHatchCraftingInputME) bufferBase.getMetaTileEntity();
        require(buffer.supportsFluids(), "Native ME input buffer supports fluids");
        ItemStack encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound item = new NBTTagCompound(), fluid = new NBTTagCompound(), output = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(stack(80, 1)), item);
        Platform.writeStackNBT(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 250)), fluid);
        Platform.writeStackNBT(AEItemStack.create(stack(81, 1)), output);
        inputs.appendTag(item);
        inputs.appendTag(fluid);
        for (int index = 2; index < 16; index++) inputs.appendTag(new NBTTagCompound());
        outputs.appendTag(output);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        encoded = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
        var pattern = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, base.getWorld());
        require(
            pattern instanceof UltimatePatternHelper && Arrays.stream(pattern.getAEInputs())
                .anyMatch(entry -> entry instanceof IAEFluidStack && entry.getStackSize() == 250),
            "Persisted Ultimate pattern retains native fluid NBT");
        MEInventoryCrafting delivered = new MEInventoryCrafting(new ContainerNull(), 4, 4);
        int slot = 0;
        for (var entry : pattern.getAEInputs())
            if (entry != null) delivered.setInventorySlotContents(slot++, entry.copy());
        require(delivered.getAEStackInSlot(1) instanceof IAEFluidStack,
            "MEInventoryCrafting exposes real native fluid");
        buffer.setInventorySlotContents(0, encoded);
        var inventory = buffer.inventories().next();
        require(inventory.insertItemsAndFluids(delivered), "Native PatternSlot accepts a real item/fluid delivery");
        bufferBase = reloadTile(bufferBase);
        buffer = (MTEHatchCraftingInputME) bufferBase.getMetaTileEntity();
        inventory = buffer.inventories().next();
        require(
            inventory.getPatternDetails() instanceof UltimatePatternHelper && inventory.getFluidInputs().length == 1
                && inventory.getFluidInputs()[0].amount == 250
                && inventory.getItemInputs().length == 1,
            "Physical buffer NBT reload retains exact native item/fluid job");
        verifyStructure();
        require(machine.mDualInputHatches.contains(buffer), "Physical structure discovers native dual-input buffer");
        require(machine.checkProcessing().wasSuccessful() && inventory.isEmpty() && outputCount(stack(81, 1)) == 1,
            "Native dual-input processing commits both buffered ingredients and exact output");
        NBTTagCompound consumed = new NBTTagCompound();
        buffer.saveNBTData(consumed);
        buffer.loadNBTData(consumed);
        require(buffer.inventories().next().isEmpty(), "Consumed fluid job remains empty after NBT reload");
        restoreSlot(12);
        verifyStructure();
        report.append(rig.slug).append(": native Ultimate item/fluid buffer delivery, persistence and real dual-input ")
            .append("consumption passed; this checks the buffer integration rather than AE CPU scheduling.\n");
    }

    private void startRunning() {
        resetProcessing();
        machine.setMachineMode(0);
        verifyStructure();
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.mStartUpCheck = 0;
        machine.mUpdate = 0;
        machine.setPowerPanelMaxParallel(1);
        setEnergy(ItemList.Hatch_Energy_LV);
        verifyStructure();
        seedRecipe(modeFixtures[0], 64);
        require(machine.checkProcessing().wasSuccessful(),
            "Start real fixture processing for tick and appearance checks");
        chargeEnergy();
        base.enableWorking();
    }

    private void chargeEnergy() {
        for (var hatch : machine.mEnergyHatches) {
            BaseMetaTileEntity energy = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
            energy.setStoredEU(energy.getEUCapacity());
        }
    }

    private void verifyPersistence() {
        savedProgress = machine.mProgresstime;
        int duration = machine.mMaxProgresstime;
        long eut = machine.lEUt;
        base = reloadTile(base);
        machine = (GTNGMultiModeMachineBase<?>) base.getMetaTileEntity();
        require(
            machine.getMachineMode() == 0 && machine.mProgresstime == savedProgress
                && machine.mMaxProgresstime == duration
                && machine.lEUt == eut
                && outputCount(stack(1, 1)) == 1,
            "Running tile reload preserves mode, progress, duration, energy and pending output");
        verifyStructure();
        machine.mStartUpCheck = 0;
        machine.mUpdate = 0;
        chargeEnergy();
        base.enableWorking();
    }

    private void verifyCoils(boolean active) {
        int count = 0;
        for (char symbol : rig.coilSymbols.toCharArray()) for (int[] local : localPositions(symbol)) {
            int[] pos = position(local[0], local[1], local[2]);
            ItemStack expected = builtBlocks.get(symbol);
            World world = base.getWorld();
            require(
                world.getBlock(pos[0], pos[1], pos[2]) == Block.getBlockFromItem(expected.getItem())
                    && world.getBlockMetadata(pos[0], pos[1], pos[2]) == expected.getItemDamage()
                    && GTCoilTracker.isCoilActive(world, pos[0], pos[1], pos[2]) == active,
                "Native server coil state " + rig.slug + " " + active + " " + Arrays.toString(pos));
            count++;
        }
        require(count == rig.coils, "All original active components are checked " + rig.slug);
        if (active)
            require(machine.mCoils.size() == rig.coils, "Native tracker owns every active component " + rig.slug);
    }

    private void cacheClientStructure() {
        List<ClientBlock> blocks = new ArrayList<>();
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++) {
                char symbol = shape[row][depth].charAt(col);
                if (symbol == ' ' || symbol == '~') continue;
                int[] pos = position(col, row, depth);
                World world = base.getWorld();
                Block block = world.getBlock(pos[0], pos[1], pos[2]);
                boolean coil = rig.coilSymbols.indexOf(symbol) >= 0;
                int activeOffset = coil ? ((IBlockWithActiveOffset) block).getActiveOffset() : 0;
                blocks.add(new ClientBlock(pos[0], pos[1], pos[2], block,
                    world.getBlockMetadata(pos[0], pos[1], pos[2]), activeOffset));
            }
        require(blocks.size() == rig.requiredBlocks, "Expected client geometry covers every source block " + rig.slug);
        expectedClientBlocks = blocks.toArray(new ClientBlock[0]);
    }

    private void observeClientStructure(Minecraft mc) {
        ClientBlock[] expected = expectedClientBlocks;
        Rig current = rig;
        if (expected == null || current == null) return;
        int matched = 0, active = 0, inactive = 0;
        for (ClientBlock block : expected) {
            if (mc.theWorld.getBlock(block.x, block.y, block.z) != block.block) continue;
            int metadata = mc.theWorld.getBlockMetadata(block.x, block.y, block.z);
            if (block.activeOffset != 0) {
                if (metadata == block.metadata) inactive++;
                if (metadata == block.metadata + block.activeOffset) active++;
                metadata %= block.activeOffset;
            }
            if (metadata == block.metadata) matched++;
        }
        clientStructureBlocks = matched;
        clientActiveCoils = active;
        clientInactiveCoils = inactive;
        completeClientStructure = matched == current.requiredBlocks && active + inactive == current.coils;
        clientGeometryStableTicks = completeClientStructure ? clientGeometryStableTicks + 1 : 0;
        if (active == current.coils) observedActiveCoils = true;
        if (inactive == current.coils && observedActiveCoils) observedInactiveCoils = true;
    }

    private void resetProcessing() {
        base.disableWorking();
        machine.mMaxProgresstime = machine.mProgresstime = 0;
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.lEUt = 0;
        for (var bus : machine.mInputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var bus : machine.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (var hatch : machine.mOutputHatches) hatch.setFillableStack(null);
    }

    private void seedRecipe(GTRecipe recipe, int operations) {
        var bus = machine.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (int slot = 0; slot < recipe.mInputs.length; slot++) {
            ItemStack input = recipe.mInputs[slot].copy();
            input.stackSize = input.stackSize == 0 ? 1 : input.stackSize * operations;
            bus.setInventorySlotContents(slot, input);
        }
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (int index = 0; index < recipe.mFluidInputs.length; index++) {
            FluidStack input = recipe.mFluidInputs[index].copy();
            input.amount *= operations;
            machine.mInputHatches.get(index).mFluid = input;
        }
    }

    private int inputCount(ItemStack expected) {
        int count = 0;
        for (var bus : machine.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack input = bus.getStackInSlot(slot);
            if (input != null && GTUtility.areStacksEqual(input, expected)) count += input.stackSize;
        }
        return count;
    }

    private int outputCount(ItemStack expected) {
        return machine.mOutputItems == null ? 0
            : Arrays.stream(machine.mOutputItems)
                .filter(output -> output != null && GTUtility.areStacksEqual(output, expected))
                .mapToInt(output -> output.stackSize).sum();
    }

    private void cleanupFixtures() {
        for (RecipeMap<?> map : rig.maps) map.getBackend()
            .removeRecipes(map.getAllRecipes().stream().filter(
                recipe -> Arrays.stream(recipe.mInputs).anyMatch(input -> input != null && input.getItem() == fixture))
                .collect(Collectors.toList()));
        modeFixtures = null;
    }

    private ItemStack stack(int metadata, int amount) {
        return new ItemStack(fixture, amount, rigIndex * 100 + metadata);
    }

    private void setEnergy(ItemList hatch) {
        placeSlot(11, hatch.get(1));
    }

    private void placeSlot(int slot, ItemStack hatch) {
        int[] pos = slotPosition(slot);
        place(base.getWorld(), pos[0], pos[1], pos[2], hatch.getItemDamage());
    }

    private int[] slotPosition(int slot) {
        int[] local = hatchSlots.get(slot);
        return position(local[0], local[1], local[2]);
    }

    private void restoreSlot(int slot) {
        ItemStack casing = builtBlocks.get(rig.hatchSymbol);
        worldBlock(slotPosition(slot), Block.getBlockFromItem(casing.getItem()), casing.getItemDamage());
    }

    private void worldBlock(int[] pos, Block block, int metadata) {
        base.getWorld().setBlock(pos[0], pos[1], pos[2], block, metadata, 3);
    }

    private List<int[]> localPositions(char symbol) {
        List<int[]> positions = new ArrayList<>();
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == symbol) positions.add(new int[] { col, row, depth });
        return positions;
    }

    private int[] firstPosition(char symbol) {
        int[] local = localPositions(symbol).get(0);
        return position(local[0], local[1], local[2]);
    }

    private int[] position(int col, int row, int depth) {
        int[] offset = new int[3];
        machine.getExtendedFacing()
            .getWorldOffset(new int[] { col - rig.offsetX, row - rig.offsetY, depth - rig.offsetZ }, offset);
        return new int[] { base.getXCoord() + offset[0], base.getYCoord() + offset[1], base.getZCoord() + offset[2] };
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private static BaseMetaTileEntity reloadTile(BaseMetaTileEntity target) {
        NBTTagCompound saved = new NBTTagCompound();
        target.writeToNBT(saved);
        World world = target.getWorld();
        int x = target.getXCoord(), y = target.getYCoord(), z = target.getZCoord();
        target.onChunkUnload();
        world.removeTileEntity(x, y, z);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(world);
        restored.readFromNBT(saved);
        world.setTileEntity(x, y, z, restored);
        return restored;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            require(++stageFrames <= 3600,
                "Visual stage timeout " + rig.slug
                    + "; blocks="
                    + clientStructureBlocks
                    + "/"
                    + rig.requiredBlocks
                    + ", active="
                    + clientActiveCoils
                    + ", stable="
                    + clientGeometryStableTicks
                    + ", view="
                    + shownView
                    + ", mode="
                    + openedMode
                    + ", frames="
                    + frames
                    + ", camera="
                    + (mc.thePlayer == null ? "missing"
                        : mc.thePlayer.posX + "," + mc.thePlayer.boundingBox.minY + "," + mc.thePlayer.posZ));
            if (tooltipStage >= 0) {
                if (!(mc.currentScreen instanceof TooltipProbeGui) || ++frames < 40) return;
                String locale = tooltipStage < 2 ? "zh_CN" : "en_US";
                capture("tooltip-" + locale + (tooltipStage % 2 == 0 ? "-normal" : "-structure"));
                frames = stageFrames = 0;
                if (++tooltipStage == 4) {
                    require(observedActiveCoils && observedInactiveCoils,
                        "Native packets synchronize every active component to both running and stopped states");
                    report.append(rig.slug).append(": complete world views, every GUI mode button, Chinese/English ")
                        .append("names and normal/Shift tooltips rendered.\n");
                    checked = false;
                    mc.displayGuiScreen(null);
                    requestedNextRig = true;
                    return;
                }
                showTooltip();
                return;
            }
            if (openedMode < 0) {
                if (shownView != capturedView + 1 || requestedView >= 0) return;
                double[] camera = rig.cameras[shownView];
                boolean atCamera = mc.thePlayer != null && Math.abs(mc.thePlayer.posX - camera[0]) < 1
                    && Math.abs(mc.thePlayer.boundingBox.minY - camera[1]) < 3
                    && Math.abs(mc.thePlayer.posZ - camera[2]) < 1;
                if (
                    !atCamera || !completeClientStructure
                        || clientGeometryStableTicks < 40
                        || clientActiveCoils != rig.coils
                ) {
                    frames = 0;
                    return;
                }
                if (warmedView != shownView) {
                    mc.renderGlobal.loadRenderers();
                    mc.gameSettings.hideGUI = true;
                    warmedView = shownView;
                    frames = 0;
                }
                if (++frames < 240) return;
                capture(shownView == 0 ? "structure-overview" : "structure-active-textures");
                capturedView = shownView;
                frames = stageFrames = 0;
                if (capturedView == 0) requestedView = 1;
                else {
                    mc.gameSettings.hideGUI = false;
                    requestedGui = true;
                }
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
            CycleButtonWidget button = findModeButton(wrapper.getScreen().getMainPanel(), rig.maps.length);
            require(button != null, "Native GUI exposes all " + rig.maps.length + " mode icons");
            int expected = capturedMode + 1;
            if (openedMode != expected || button.getIntValue().getIntValue() != expected) {
                frames = 0;
                return;
            }
            if (++frames < 40) return;
            capture("mode-" + openedMode);
            capturedMode = openedMode;
            frames = stageFrames = 0;
            if (capturedMode + 1 < rig.maps.length) button.next();
            else {
                tooltipStage = 0;
                showTooltip();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static CycleButtonWidget findModeButton(IWidget widget, int states) {
        if (widget instanceof CycleButtonWidget button && button.getStateCount() == states) return button;
        for (IWidget child : widget.getChildren()) {
            CycleButtonWidget button = findModeButton(child, states);
            if (button != null) return button;
        }
        return null;
    }

    private MultiblockTooltipBuilder verifyTooltips(String locale) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (Language language : mc.getLanguageManager().getLanguages()) {
            if (!language.getLanguageCode().equals(locale)) continue;
            mc.getLanguageManager().setCurrentLanguage(language);
            mc.gameSettings.language = locale;
            mc.refreshResources();
            break;
        }
        require(
            rig.controller.get(1).getDisplayName().equals(StatCollector.translateToLocal(machine.getLocalNameKey())),
            "Controller name follows language " + rig.slug + " " + locale);
        for (AssemblyPlantCasing casing : AssemblyPlantCasing.values()) require(
            casing.getStack(1).getDisplayName().equals(StatCollector.translateToLocal(casing.getNameKey()))
                && !casing.getStack(1).getDisplayName().contains("tile."),
            "Localized block name " + casing + " " + locale);
        Method create = machine.getClass().getDeclaredMethod("createTooltip");
        create.setAccessible(true);
        MultiblockTooltipBuilder tooltip = (MultiblockTooltipBuilder) create.invoke(machine);
        String text = String.join("\n", tooltip.getInformation()) + "\n\n"
            + String.join("\n", tooltip.getStructureInformation());
        require(!text.contains("gtng.") && !text.contains("gt.mbtt.") && !text.contains("gt.blockmachines."),
            "Every tooltip translation resolves " + rig.slug + " " + locale);
        require(
            text.contains(Integer.toString(shape[0][0].length())) && text.contains(Integer.toString(shape.length))
                && text.contains(Integer.toString(shape[0].length)),
            "Tooltip reports original dimensions " + rig.slug + " " + locale);
        Files.write(new File(output(), rig.slug + "-tooltip-" + locale + ".txt").toPath(),
            text.getBytes(StandardCharsets.UTF_8));
        return tooltip;
    }

    private void showTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = 1;
        if (tooltipStage % 2 == 0) currentTooltip = verifyTooltips(tooltipStage < 2 ? "zh_CN" : "en_US");
        List<String> lines = new ArrayList<>();
        lines.add(rig.controller.get(1).getDisplayName());
        lines.addAll(Arrays.asList(
            tooltipStage % 2 == 0 ? currentTooltip.getInformation() : currentTooltip.getStructureInformation()));
        mc.displayGuiScreen(new TooltipProbeGui(lines));
    }

    private static final class TooltipProbeGui extends GuiScreen {

        private final List<String> lines;
        private int tooltipX, tooltipY;

        private TooltipProbeGui(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void initGui() {
            int textWidth = lines.stream().mapToInt(fontRendererObj::getStringWidth).max().orElse(0);
            int textHeight = 8 + Math.max(0, lines.size() - 1) * 10 + (lines.size() > 1 ? 2 : 0);
            require(textWidth + 32 <= width && textHeight + 32 <= height,
                "Tooltip fits viewport: " + textWidth + "x" + textHeight + " in " + width + "x" + height);
            tooltipX = (width - textWidth) / 2 - 12;
            tooltipY = (height - textHeight) / 2 + 12;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF303238);
            drawHoveringText(lines, tooltipX, tooltipY, fontRendererObj);
        }
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
        System.out.println("ASSEMBLY_PLANT_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), rig.slug + "-" + name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.assemblyPlant.output"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(),
                ("FAIL\n" + trace + "\n" + report).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private enum RunningPhase {
        WaitStructure,
        WaitActive,
        WaitStopped,
        WaitRestarted,
        WaitBroken,
        WaitRepaired,
        WaitReloadedProgress
    }

    private static final class Rig {

        private final String slug;
        private final GTNGItemList controller;
        private final int machineId, offsetX, offsetY, offsetZ, requiredBlocks, coils, originX;
        private final String[][] shape;
        private final char hatchSymbol;
        private final String fixedSymbols, coilSymbols;
        private final RecipeMap<?>[] maps;
        private final double[][] cameras;

        private Rig(String slug, GTNGItemList controller, int machineId, String[][] shape, int offsetX, int offsetY,
            int offsetZ, char hatchSymbol, String fixedSymbols, String coilSymbols, int requiredBlocks, int coils,
            int originX, RecipeMap<?>[] maps, double[][] cameras) {
            this.slug = slug;
            this.controller = controller;
            this.machineId = machineId;
            this.shape = shape;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.hatchSymbol = hatchSymbol;
            this.fixedSymbols = fixedSymbols;
            this.coilSymbols = coilSymbols;
            this.requiredBlocks = requiredBlocks;
            this.coils = coils;
            this.originX = originX;
            this.maps = maps;
            this.cameras = cameras;
        }
    }

    private static final class ClientBlock {

        private final int x, y, z, metadata, activeOffset;
        private final Block block;

        private ClientBlock(int x, int y, int z, Block block, int metadata, int activeOffset) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.metadata = metadata;
            this.activeOffset = activeOffset;
        }
    }
}
