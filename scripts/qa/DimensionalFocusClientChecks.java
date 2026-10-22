package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.xyp.gtnotgood.client.render.DimensionalFocusCoreGeometry;
import com.xyp.gtnotgood.client.render.DimensionalFocusCoreRenderer;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.DimensionalFocusEngravingArray;
import com.xyp.gtnotgood.common.machines.multiblock.structure.DimensionalFocusEngravingStructure;
import com.xyp.gtnotgood.common.recipe.machine.DimensionalFocusFluids;
import com.xyp.gtnotgood.common.recipe.machine.DimensionalFocusResearch;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTScannerResult;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.tileentities.machines.basic.MTEScanner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import tectech.mechanics.dataTransport.QuantumDataPacket;
import tectech.thing.metaTileEntity.hatch.MTEHatchDataInput;

@Mod(
    modid = "dimensionalfocusqa",
    name = "Dimensional Focus QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class DimensionalFocusClientChecks {

    private Item fixture;
    private BaseMetaTileEntity base;
    private volatile DimensionalFocusEngravingArray machine;
    private final String[][] shape = DimensionalFocusEngravingStructure.shape();
    private final List<int[]> hatchPositions = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private boolean started;
    private volatile boolean checked, finished;
    private volatile int requestedView = -1, shownView = -1;
    private volatile int requestedCoreState = -1, appliedCoreState = -1;
    private volatile boolean requestGui, openedGui;
    private int clientTicks, serverTicks, frames, stageFrames, capturedView = -1;
    private int tooltipStage = -1;
    private int coreCaptureStage;
    private boolean coreGlChecked;
    private Block coreMissingBlock;
    private int coreMissingMetadata;
    private int[] coreMissingPosition;
    private MultiblockTooltipBuilder currentTooltip;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.dimensionalFocus.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("dimensionalFocusFixture");
        GameRegistry.registerItem(fixture, "fixture");
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == 'G') hatchPositions.add(new int[] { col, row, depth });
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 12000, "Client QA timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = tooltipStage >= 0 ? 1 : 2;
            mc.gameSettings.clouds = false;
            mc.gameSettings.hideGUI = checked && !openedGui && tooltipStage < 0;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0F);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 14;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("dimensional-focus-qa-" + System.currentTimeMillis(), "Dimensional Focus QA",
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
            require(++serverTicks <= 7200, "Server QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) {
                require(GTNGItemList.DimensionalFocusEngravingArray.get(1).getItemDamage()
                    == GTNGMachineID.DimensionalFocusEngravingArray.id, "Registered controller identity");
                player.worldObj.setWorldTime(6000);
                buildRig(player, 0, 90, 0, ForgeDirection.NORTH);
            }
            if (serverTicks == 60) {
                verifyStructure();
                verifyDirections(player);
                verifyRequiredBlocks();
                verifyRequiredHatches();
                verifyMixedCoils();
                verifySuperconductorActivity();
                verifyUnconstrainedSpace();
                verifySurvivalRecipes();
                verifyNativeEngraving();
                verifyNativeResearch();
                verifyProcessing();
                verifyVoltageGate();
                verifyResearchAndComputation();
                verifyOutputProtection();
                resetProcessing();
                registerVisualRecipe();
                applyCoreState(0);
                checked = true;
                requestedView = 0;
            }
            if (checked && requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                player.closeScreen();
                if (view == 0) player.playerNetServerHandler.setPlayerLocation(-68, 126, -60, -40, 28);
                else if (view == 1) player.playerNetServerHandler.setPlayerLocation(-18, 112, 10, -75, 15);
                else player.playerNetServerHandler.setPlayerLocation(-9, 77, 4, -24, 12);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                shownView = view;
            }
            if (checked && requestedCoreState >= 0) {
                int state = requestedCoreState;
                requestedCoreState = -1;
                applyCoreState(state);
            }
            if (checked && (appliedCoreState == 0 || appliedCoreState == 3 || appliedCoreState == 5)) {
                for (var hatch : machine.mEnergyHatches) {
                    BaseMetaTileEntity energy = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
                    energy.setStoredEU(energy.getEUCapacity());
                }
                machine.getComputationHatches().get(0).setContents(new QuantumDataPacket(4L));
            }
            if (requestGui) {
                requestGui = false;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0, 90, -4, 0, 0);
                machine.onRightclick(base, player);
                openedGui = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void buildRig(EntityPlayerMP player, int x, int y, int z, ForgeDirection facing) {
        World world = player.worldObj;
        for (int cx = (x - 90) >> 4; cx <= (x + 90) >> 4; cx++)
            for (int cz = (z - 90) >> 4; cz <= (z + 90) >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
        base = place(world, x, y, z, GTNGMachineID.DimensionalFocusEngravingArray.id);
        base.setFrontFacing(facing);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (DimensionalFocusEngravingArray) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        verifyConstructedGeometry();
        placeHatch(0, ItemList.Hatch_Input_Bus_EV.get(1));
        placeHatch(1, ItemList.Hatch_Output_Bus_EV.get(1));
        placeHatch(2, ItemList.Hatch_Maintenance.get(1));
        placeHatch(3, ItemList.Hatch_Energy_EV.get(1));
        placeHatch(4, ItemList.Hatch_Input_EV.get(1));
        placeHatch(5, prototype(MTEHatchDataAccess.class));
        placeHatch(6, prototype(MTEHatchDataInput.class));
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(8);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
    }

    private void verifyConstructedGeometry() {
        Map<Character, ItemStack> blocks = new HashMap<>();
        int total = 0;
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++) {
                char symbol = shape[row][depth].charAt(col);
                if (symbol == ' ' || symbol == '~') continue;
                int[] pos = position(col, row, depth);
                World world = base.getWorld();
                Block block = world.getBlock(pos[0], pos[1], pos[2]);
                require(block != Blocks.air, "Built required block " + symbol + " at " + Arrays.toString(pos));
                ItemStack stack = new ItemStack(block, 1, world.getBlockMetadata(pos[0], pos[1], pos[2]));
                ItemStack expected = blocks.putIfAbsent(symbol, stack);
                require(expected == null || GTUtility.areStacksEqual(stack, expected),
                    "Consistent constructed block for symbol " + symbol);
                total++;
            }
        require(total == 10053 && blocks.size() == 8, "All 10053 original structure blocks are built");
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Structure: " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Physical controller forms the complete 89x89x89 structure");
        require(machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1
            && machine.mEnergyHatches.size() == 1
            && machine.mMaintenanceHatches.size() == 1
            && machine.mInputHatches.size() == 1, "Physical item/fluid/energy/maintenance hatches are discovered");
        require(machine.getResearchHatches().size() == 1 && machine.getComputationHatches().size() == 1,
            "Physical research and computation hatches are discovered");
        verifyCoreBounds();
    }

    private void verifyCoreBounds() {
        ForgeDirection back = base.getFrontFacing().getOpposite();
        Vec3Impl offset = machine.getCoreRenderOffset();
        require(offset.get0() == back.offsetX * 22 && offset.get1() == -18 && offset.get2() == back.offsetZ * 22,
            "Core center follows the original structure in direction " + base.getFrontFacing());
        AxisAlignedBB box = machine.getRenderBoundingBox(base.getXCoord(), base.getYCoord(), base.getZCoord());
        double centerX = base.getXCoord() + offset.get0() + 0.5;
        double centerY = base.getYCoord() + offset.get1() + 0.5;
        double centerZ = base.getZCoord() + offset.get2() + 0.5;
        require(box.minX <= centerX - 8 && box.maxX >= centerX + 8
            && box.minY <= centerY - 8
            && box.maxY >= centerY + 8
            && box.minZ <= centerZ - 8
            && box.maxZ >= centerZ + 8, "Native render bounds include the six complete core beams");
        require(machine.getMaxRenderDistanceSquared() >= 256.0 * 256,
            "Native TESR range extends beyond the default 64-block cutoff");
    }

    private void registerVisualRecipe() {
        GTRecipeBuilder.builder().itemInputs(stack(10, 1), stack(1, 0)).itemOutputs(stack(11, 1)).duration(100000000)
            .eut(1).metadata(DimensionalFocusResearch.COMPUTATION, 4L)
            .metadata(DimensionalFocusResearch.RESEARCH, DimensionalFocusResearch.fingerprint(stack(11, 1)))
            .addTo(machine.getRecipeMap());
    }

    private void applyCoreState(int state) throws Exception {
        if (state == 0 || state == 3) {
            restoreCoreStructure();
            resetProcessing();
            seed(stack(10, 1), stack(1, 1));
            machine.getResearchHatches().get(0).setInventorySlotContents(0,
                DimensionalFocusResearch.createResearchStick(stack(11, 1)));
            machine.getComputationHatches().get(0).setContents(new QuantumDataPacket(4L));
            machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
            machine.mSolderingTool = machine.mCrowbar = true;
            machine.mEfficiency = 10000;
            require(machine.checkProcessing().wasSuccessful(), "Real working recipe for core rendering");
            base.enableWorking();
            base.setActive(true);
        } else if (state == 2 || state == 7) {
            restoreCoreStructure();
            resetProcessing();
            base.disableWorking();
            base.setActive(false);
            verifyStructure();
            if (state == 7) cleanupFixtures();
        } else if (state == 4) {
            require(machine.mMaxProgresstime > 0, "Pause a committed core-rendering operation");
            machine.getComputationHatches().get(0).setContents(null);
        } else if (state == 5) {
            machine.getComputationHatches().get(0).setContents(new QuantumDataPacket(4L));
        } else if (state == 6) {
            coreMissingPosition = firstPosition('A');
            World world = base.getWorld();
            coreMissingBlock = world.getBlock(coreMissingPosition[0], coreMissingPosition[1], coreMissingPosition[2]);
            coreMissingMetadata = world.getBlockMetadata(coreMissingPosition[0], coreMissingPosition[1],
                coreMissingPosition[2]);
            world.setBlockToAir(coreMissingPosition[0], coreMissingPosition[1], coreMissingPosition[2]);
            require(!machine.checkStructure(true, base), "Unformed controller disables core rendering");
        }
        Method update = DimensionalFocusEngravingArray.class.getDeclaredMethod("updateCoreRendering",
            IGregTechTileEntity.class, long.class);
        update.setAccessible(true);
        update.invoke(machine, base, base.getTimer());
        require(machine.isCoreRendering() == (state == 0 || state == 3 || state == 5),
            "Server core render gate for visual state " + state);
        appliedCoreState = state;
    }

    private void restoreCoreStructure() {
        if (coreMissingBlock == null) return;
        base.getWorld().setBlock(coreMissingPosition[0], coreMissingPosition[1], coreMissingPosition[2],
            coreMissingBlock, coreMissingMetadata, 3);
        coreMissingBlock = null;
        verifyStructure();
    }

    private void verifyDirections(EntityPlayerMP player) throws Exception {
        BaseMetaTileEntity mainBase = base;
        DimensionalFocusEngravingArray mainMachine = machine;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST }) {
            buildRig(player, 192, 90, 192, facing);
            verifyStructure();
            setSuperconductorsActive(true);
            List<int[]> coils = superconductorPositions();
            machine.onRemoval();
            assertSuperconductorState(coils, false, "Controller removal clears active coils");
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
                for (int col = 0; col < shape[row][depth].length(); col++) {
                    if (shape[row][depth].charAt(col) == ' ') continue;
                    int[] pos = position(col, row, depth);
                    base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
                }
        }
        base = mainBase;
        machine = mainMachine;
        verifyStructure();
        report.append("Original 89x89x89 geometry forms in all four directions; removing disposable controllers ")
            .append("clears their 144 active coils.\n");
    }

    private void verifyRequiredBlocks() {
        for (char symbol : new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'H' }) {
            int[] pos = firstPosition(symbol);
            World world = base.getWorld();
            Block block = world.getBlock(pos[0], pos[1], pos[2]);
            int meta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
            world.setBlockToAir(pos[0], pos[1], pos[2]);
            require(!machine.checkStructure(true, base), "Missing required block " + symbol + " rejects formation");
            world.setBlock(pos[0], pos[1], pos[2], block, meta, 3);
            verifyStructure();
        }
        report.append("Seven fixed structure components reject missing blocks and recover after repair.\n");
    }

    private void verifyMixedCoils() {
        int[] pos = firstPosition('H');
        World world = base.getWorld();
        Block block = world.getBlock(pos[0], pos[1], pos[2]);
        int meta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
        world.setBlock(pos[0], pos[1], pos[2], block, (meta + 1) % 16, 3);
        require(!machine.checkStructure(true, base), "Mixed heating-coil tiers reject formation");
        world.setBlock(pos[0], pos[1], pos[2], block, meta, 3);
        verifyStructure();
        report.append("Heating coils must share one tier throughout all 3312 coil positions.\n");
    }

    private void verifyRequiredHatches() {
        for (int index : new int[] { 5, 6 }) {
            int[] relative = hatchPositions.get(index);
            int[] pos = position(relative[0], relative[1], relative[2]);
            base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
            require(!machine.checkStructure(true, base), "Missing research/computation hatch rejects formation");
            placeHatch(index, prototype(index == 5 ? MTEHatchDataAccess.class : MTEHatchDataInput.class));
            verifyStructure();
        }
        report.append("Research and computation reception each require their own physical hatch.\n");
    }

    private void verifyUnconstrainedSpace() {
        require(shape[0][0].charAt(0) == ' ', "Selected upstream any position");
        int[] pos = position(0, 0, 0);
        World world = base.getWorld();
        world.setBlock(pos[0], pos[1], pos[2], Blocks.stone, 0, 3);
        verifyStructure();
        world.setBlockToAir(pos[0], pos[1], pos[2]);
        verifyStructure();
        report.append("Original unconstrained spaces accept both a solid block and air.\n");
    }

    private void verifySuperconductorActivity() throws Exception {
        List<int[]> coils = superconductorPositions();
        for (boolean active : new boolean[] { true, false }) {
            setSuperconductorsActive(active);
            assertSuperconductorState(coils, active, "Superconductor active metadata follows controller");
            verifyStructure();
        }
        setSuperconductorsActive(true);
        ForgeDirection facing = base.getFrontFacing();
        base.setFrontFacing(facing.getRotation(ForgeDirection.UP));
        require(!machine.checkStructure(true, base), "Rotating only the controller invalidates the original structure");
        assertSuperconductorState(coils, false, "Controller rotation clears the original active coils");
        base.setFrontFacing(facing);
        verifyStructure();
        setSuperconductorsActive(true);
        assertSuperconductorState(coils, true, "Restoring controller facing reactivates the original coils");
        setSuperconductorsActive(false);
        verifyStructure();
        report.append("All 144 superconductor coils switch active texture state and remain valid structure blocks.\n");
        report.append("Changing controller facing clears old active coils; restoring facing permits reactivation.\n");
    }

    private List<int[]> superconductorPositions() {
        List<int[]> positions = new ArrayList<>();
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == 'F') positions.add(position(col, row, depth));
        require(positions.size() == 144, "Original superconductor position count");
        return positions;
    }

    private void assertSuperconductorState(List<int[]> positions, boolean active, String description) {
        for (int[] pos : positions) {
            int metadata = base.getWorld().getBlockMetadata(pos[0], pos[1], pos[2]);
            require(((metadata & 8) != 0) == active, description + " at " + Arrays.toString(pos));
        }
    }

    private void setSuperconductorsActive(boolean active) throws Exception {
        base.setActive(active);
        Method update = DimensionalFocusEngravingArray.class.getDeclaredMethod("updateSuperconductors",
            IGregTechTileEntity.class, boolean.class);
        update.setAccessible(true);
        update.invoke(machine, base, true);
    }

    private void verifyProcessing() {
        GTRecipeBuilder.builder().itemInputs(stack(0, 1), stack(1, 0)).itemOutputs(stack(2, 1)).duration(80).eut(30)
            .addTo(machine.getRecipeMap());
        resetProcessing();
        seed(stack(0, 1), stack(1, 1));
        require(machine.checkProcessing().wasSuccessful(), "A low-voltage fixture processes on the real structure");
        require(inputCount(stack(0, 1)) == 0 && inputCount(stack(1, 1)) == 1 && outputCount(stack(2, 1)) == 1,
            "Exact input/output accounting preserves the non-consumed lens");
        report.append("Physical processing consumes its material, preserves the lens and produces the exact output.\n");
    }

    private void verifyNativeEngraving() {
        GTRecipe recipe = machine.getRecipeMap().getAllRecipes().stream()
            .filter(candidate -> candidate.mEUt > 0 && candidate.mEUt <= 1920
                && candidate.mOutputs.length > 0
                && candidate.mInputs.length <= 8
                && candidate.mFluidInputs != null
                && candidate.mFluidInputs.length == 1
                && candidate.mSpecialValue >= 0
                && guaranteed(candidate.mInputChances)
                && guaranteed(candidate.mOutputChances)
                && guaranteed(candidate.mFluidInputChances))
            .findFirst().orElseThrow(() -> new AssertionError("Adapted native laser recipe with photoresist"));
        resetProcessing();
        ItemStack[] inputs = Arrays.stream(recipe.mInputs).map(ItemStack::copy).toArray(ItemStack[]::new);
        for (ItemStack input : inputs) if (input.stackSize == 0) input.stackSize = 1;
        seed(inputs);
        FluidStack fluid = recipe.mFluidInputs[0].copy();
        machine.mInputHatches.get(0).mFluid = fluid.copy();
        require(machine.checkProcessing().wasSuccessful(), "A real adapted native laser recipe starts");
        for (ItemStack input : recipe.mInputs) require(inputCount(input) == (input.stackSize == 0 ? 1 : 0),
            "Native recipe consumes material and retains its non-consumed lens");
        for (ItemStack output : recipe.mOutputs)
            require(outputCount(output) == output.stackSize, "Native recipe exact output count");
        require(machine.mInputHatches.get(0).mFluid == null || machine.mInputHatches.get(0).mFluid.amount == 0,
            "Native adaptation consumes its exact photoresist quantity");
        report.append("An imported native laser recipe runs with exact item/fluid consumption and lens retention.\n");
    }

    private void verifySurvivalRecipes() {
        GTNGItemList[] outputs = { GTNGItemList.DimensionalFocusEngravingArray, GTNGItemList.DimensionInjectionCasing,
            GTNGItemList.MolecularCoil };
        int[] items = { 8, 7, 5 }, fluids = { 4, 4, 4 }, amounts = { 1, 2, 2 };
        long[] tiers = { TierEU.RECIPE_UEV, TierEU.RECIPE_UEV, TierEU.RECIPE_UV };
        for (int index = 0; index < outputs.length; index++) {
            ItemStack expected = outputs[index].get(1);
            List<GTRecipe.RecipeAssemblyLine> recipes = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, expected)).collect(Collectors.toList());
            require(recipes.size() == 1, "Exactly one assembly-line recipe for " + outputs[index]);
            GTRecipe.RecipeAssemblyLine recipe = recipes.get(0);
            require(
                recipe.mInputs.length == items[index] && recipe.mFluidInputs.length == fluids[index]
                    && recipe.mEUt == tiers[index]
                    && recipe.mOutput.stackSize == amounts[index]
                    && recipe.mResearchItem != null
                    && recipe.mResearchTime == 2400,
                "Assembly-line ingredients, tier, yield and research remain complete: " + outputs[index]);
            validateIngredients(recipe.mInputs, recipe.mFluidInputs, outputs[index].name());
        }
        GTNGItemList[] assembled = { GTNGItemList.ImprovedSuperconductorCoil, GTNGItemList.LaserSafeEngravingCasing,
            GTNGItemList.FusionGlass, GTNGItemList.DimensionallyTranscendentCasing };
        int[] assemblerItems = { 4, 4, 4, 9 }, assemblerFluids = { 1, 1, 1, 1 };
        for (int index = 0; index < assembled.length; index++) {
            ItemStack expected = assembled[index].get(1);
            List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream().filter(
                recipe -> Arrays.stream(recipe.mOutputs).anyMatch(output -> GTUtility.areStacksEqual(output, expected)))
                .collect(Collectors.toList());
            require(recipes.size() == 1, "Exactly one assembler recipe for " + assembled[index]);
            GTRecipe recipe = recipes.get(0);
            require(
                recipe.mInputs.length == assemblerItems[index] && recipe.mFluidInputs.length == assemblerFluids[index],
                "Assembler ingredients remain complete: " + assembled[index]);
            validateIngredients(recipe.mInputs, recipe.mFluidInputs, assembled[index].name());
        }
        for (FluidStack expected : new FluidStack[] { DimensionalFocusFluids.photoresist(2000),
            DimensionalFocusFluids.euvPhotoresist(1000) }) {
            List<GTRecipe> recipes = RecipeMaps.multiblockChemicalReactorRecipes.getAllRecipes().stream()
                .filter(recipe -> Arrays.stream(recipe.mFluidOutputs).anyMatch(output -> output.isFluidEqual(expected)))
                .collect(Collectors.toList());
            require(recipes.size() == 1, "Exactly one synthesis recipe for " + expected.getFluid().getName());
            GTRecipe recipe = recipes.get(0);
            require(
                recipe.mInputs.length == 2 && recipe.mFluidInputs.length == 2
                    && recipe.mFluidOutputs[0].amount == expected.amount,
                "Photoresist synthesis ingredients and yield");
            validateIngredients(recipe.mInputs, recipe.mFluidInputs, expected.getFluid().getName());
        }
        report.append("All seven survival recipes and both photoresist syntheses have complete runtime inputs.\n");
    }

    private static void validateIngredients(ItemStack[] items, FluidStack[] fluids, String recipe) {
        List<String> itemNames = new ArrayList<>();
        for (ItemStack input : items) itemNames.add(input == null ? "null"
            : input.getItem() == null ? "null item:" + input.getItemDamage() + " x" + input.stackSize
                : Item.itemRegistry.getNameForObject(input.getItem()) + ":"
                    + input.getItemDamage()
                    + " x"
                    + input.stackSize
                    + " ("
                    + input.getDisplayName()
                    + ")");
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack input = items[slot];
            require(input != null && input.getItem() != null && input.stackSize >= 0,
                "Unavailable runtime item in " + recipe
                    + " slot "
                    + slot
                    + ": "
                    + itemNames.get(slot)
                    + "; all inputs="
                    + itemNames);
        }
        for (int slot = 0; slot < fluids.length; slot++) {
            FluidStack input = fluids[slot];
            require(input != null && input.getFluid() != null && input.amount > 0,
                "Unavailable runtime fluid in " + recipe
                    + " slot "
                    + slot
                    + ": "
                    + (input == null ? "null"
                        : input.getFluid() == null ? "null fluid x" + input.amount
                            : input.getFluid().getName() + " x" + input.amount));
        }
    }

    private void verifyNativeResearch() {
        GTRecipe recipe = machine.getRecipeMap().getAllRecipes().stream()
            .filter(DimensionalFocusResearch::requiresResearch).findFirst()
            .orElseThrow(() -> new AssertionError("Imported high-tier research recipe"));
        ItemStack sample = recipe.mOutputs[0].copy();
        GTScannerResult result = RecipeMaps.scannerHandlers.getFirst().apply(machine, sample,
            ItemList.Tool_DataStick.get(1), null);
        require(result != null && !result.isNotMet()
            && result.inputConsume == 1
            && result.specialConsume == 1
            && result.duration == 2400, "Native scanner handler returns a complete sample-and-data-stick research");
        require(DimensionalFocusResearch.hasToken(result.output, recipe.getMetadata(DimensionalFocusResearch.RESEARCH)),
            "Native scanner emits research matching the imported recipe");
        require(RecipeMaps.scannerHandlers.getFirst().apply(machine, sample, result.output, null)
            == GTScannerResult.NOT_FOUND, "Scanner refuses to overwrite an existing research stick");
        BaseMetaTileEntity scannerBase = place(base.getWorld(), 96, 90, -20,
            ItemList.ScannerUMV.get(1).getItemDamage());
        MTEScanner scanner = (MTEScanner) scannerBase.getMetaTileEntity();
        sample.stackSize = 1;
        scanner.setInventorySlotContents(scanner.getInputSlot(), sample);
        scanner.setInventorySlotContents(scanner.getSpecialSlotIndex(), ItemList.Tool_DataStick.get(1));
        scannerBase.setStoredEU(scannerBase.getEUCapacity());
        require(scanner.checkRecipe() > 0 && scanner.mMaxProgresstime > 0,
            "Physical native scanner starts imported engraving research");
        require(
            scanner.getStackInSlot(scanner.getInputSlot()).stackSize == 0
                && scanner.getStackInSlot(scanner.getSpecialSlotIndex()).stackSize == 0
                && DimensionalFocusResearch.hasToken(scanner.mOutputItems[0],
                    recipe.getMetadata(DimensionalFocusResearch.RESEARCH)),
            "Native scanner consumes one sample and blank data stick, and commits matching research output");
        base.getWorld().setBlockToAir(96, 90, -20);
        report.append("A native scanner consumes a sample and blank data stick to produce imported recipe research.\n");
    }

    private void verifyOutputProtection() {
        resetProcessing();
        var bus = machine.mOutputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        seed(stack(0, 1), stack(1, 1));
        require(!machine.checkProcessing().wasSuccessful(), "A full protected output bus rejects processing");
        require(inputCount(stack(0, 1)) == 1 && inputCount(stack(1, 1)) == 1,
            "Output failure preserves both material and lens");
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(2, 1)) == 1,
            "Clearing output storage permits retry");
        report.append("A full output bus prevents input consumption and permits retry after clearing storage.\n");
    }

    private void verifyVoltageGate() {
        GTRecipeBuilder.builder().itemInputs(stack(3, 1), stack(1, 0)).itemOutputs(stack(4, 1)).duration(80).eut(7680)
            .addTo(machine.getRecipeMap());
        resetProcessing();
        seed(stack(3, 1), stack(1, 1));
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(3, 1)) == 1,
            "EV voltage refuses an IV recipe without consuming its input");
        placeHatch(3, ItemList.Hatch_Energy_IV.get(1));
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(4, 1)) == 1,
            "Physical IV energy hatch upgrade unlocks the recipe");
        placeHatch(3, ItemList.Hatch_Energy_EV.get(1));
        verifyStructure();
        report.append("Recipe voltage gates survive processing and permit retry after a physical hatch upgrade.\n");
    }

    private void verifyResearchAndComputation() throws Exception {
        String research = DimensionalFocusResearch.fingerprint(stack(6, 1));
        GTRecipeBuilder.builder().itemInputs(stack(5, 1), stack(1, 0)).itemOutputs(stack(6, 1)).duration(10000).eut(30)
            .metadata(DimensionalFocusResearch.COMPUTATION, 4L).metadata(DimensionalFocusResearch.RESEARCH, research)
            .addTo(machine.getRecipeMap());
        resetProcessing();
        seed(stack(5, 1), stack(1, 1));
        var computation = machine.getComputationHatches().get(0);
        var data = machine.getResearchHatches().get(0);
        computation.setContents(new QuantumDataPacket(4L));
        require(machine.getAvailableComputation() == 4, "Native data packet supplies exact computation");
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(5, 1)) == 1,
            "Missing research prevents processing without consuming inputs");
        data.setInventorySlotContents(0, DimensionalFocusResearch.createResearchStick(stack(99, 1)));
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(5, 1)) == 1,
            "Unrelated research cannot authorize the recipe");
        data.setInventorySlotContents(0, DimensionalFocusResearch.createResearchStick(stack(6, 1)));
        computation.setContents(new QuantumDataPacket(3L));
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(5, 1)) == 1,
            "Insufficient computation preserves material");
        computation.setContents(new QuantumDataPacket(4L));
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(6, 1)) == 1,
            "Matching research and exact computation unlock the recipe");
        require(machine.getRequiredComputation() == 4 && inputCount(stack(5, 1)) == 0 && inputCount(stack(1, 1)) == 1,
            "Committed computation demand and exact item accounting");
        require(DimensionalFocusResearch.hasToken(data.getStackInSlot(0), research),
            "Research stick remains after the operation starts");
        verifyComputationPause();
        verifyPersistence(stack(6, 1));
        require(machine.getRequiredComputation() == 4, "Committed computation demand survives controller reload");
        report.append("Native data packets and matching research gate processing; missing/wrong research and ")
            .append("insufficient computation preserve inputs.\n");
    }

    private void verifyComputationPause() throws Exception {
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
        BaseMetaTileEntity energy = (BaseMetaTileEntity) machine.mEnergyHatches.get(0).getBaseMetaTileEntity();
        energy.setStoredEU(energy.getEUCapacity());
        long stored = energy.getStoredEU();
        int progress = machine.mProgresstime;
        int duration = machine.mMaxProgresstime;
        Method run = MTEMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        var computation = machine.getComputationHatches().get(0);
        computation.setContents(null);
        for (long tick = 100; tick < 103; tick++) run.invoke(machine, base, tick);
        require(
            machine.mProgresstime == progress && machine.mMaxProgresstime == duration
                && energy.getStoredEU() == stored
                && outputCount(stack(6, 1)) == 1,
            "Computation dropout preserves committed progress/output and consumes no energy");
        computation.setContents(new QuantumDataPacket(4L));
        run.invoke(machine, base, 103L);
        require(machine.mProgresstime == progress + 1 && energy.getStoredEU() < stored && outputCount(stack(6, 1)) == 1,
            "Restored computation resumes the same committed operation");
        report.append("The native run loop pauses progress and EU draw on computation loss, then resumes.\n");
    }

    private void verifyPersistence(ItemStack expectedOutput) {
        require(machine.mMaxProgresstime > 0, "Save a running engraving operation");
        int duration = machine.mMaxProgresstime;
        int progress = machine.mProgresstime;
        long eut = machine.lEUt;
        NBTTagCompound saved = new NBTTagCompound();
        base.writeToNBT(saved);
        World world = base.getWorld();
        int x = base.getXCoord(), y = base.getYCoord(), z = base.getZCoord();
        world.removeTileEntity(x, y, z);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(world);
        restored.readFromNBT(saved);
        world.setTileEntity(x, y, z, restored);
        base = restored;
        base.disableWorking();
        machine = (DimensionalFocusEngravingArray) base.getMetaTileEntity();
        require(
            machine.mMaxProgresstime == duration && machine.mProgresstime == progress
                && machine.lEUt == eut
                && outputCount(expectedOutput) == 1,
            "A real controller reload preserves duration, power and pending output");
        verifyStructure();
        report.append("A controller TileEntity reload preserves committed progress, power and pending output.\n");
    }

    private void cleanupFixtures() {
        RecipeMap<?> recipes = machine.getRecipeMap();
        recipes.getBackend()
            .removeRecipes(recipes.getAllRecipes().stream().filter(
                recipe -> Arrays.stream(recipe.mInputs).anyMatch(input -> input != null && input.getItem() == fixture))
                .collect(Collectors.toList()));
    }

    private void resetProcessing() {
        machine.mMaxProgresstime = 0;
        machine.mProgresstime = 0;
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.lEUt = 0;
        if (!machine.mInputBusses.isEmpty()) seed();
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (var hatch : machine.getComputationHatches()) hatch.setContents(null);
        for (var hatch : machine.getResearchHatches())
            for (int slot = 0; slot < hatch.getSizeInventory(); slot++) hatch.setInventorySlotContents(slot, null);
        for (var bus : machine.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
    }

    private void seed(ItemStack... inputs) {
        var bus = machine.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (int slot = 0; slot < inputs.length; slot++) bus.setInventorySlotContents(slot, inputs[slot]);
    }

    private int inputCount(ItemStack expected) {
        int count = 0;
        for (var bus : machine.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack stack = bus.getStackInSlot(slot);
            if (stack != null && GTUtility.areStacksEqual(stack, expected)) count += stack.stackSize;
        }
        return count;
    }

    private int outputCount(ItemStack expected) {
        return machine.mOutputItems == null ? 0
            : Arrays.stream(machine.mOutputItems)
                .filter(stack -> stack != null && GTUtility.areStacksEqual(stack, expected))
                .mapToInt(stack -> stack.stackSize).sum();
    }

    private void placeHatch(int index, ItemStack stack) {
        int[] position = hatchPositions.get(index);
        int[] pos = position(position[0], position[1], position[2]);
        place(base.getWorld(), pos[0], pos[1], pos[2], stack.getItemDamage());
    }

    private static ItemStack prototype(Class<? extends IMetaTileEntity> type) {
        for (IMetaTileEntity candidate : GregTechAPI.METATILEENTITIES)
            if (candidate != null && candidate.getClass() == type) return candidate.getStackForm(1);
        throw new AssertionError("Registered hatch prototype " + type.getName());
    }

    private int[] firstPosition(char symbol) {
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++) {
            int col = shape[row][depth].indexOf(symbol);
            if (col >= 0) return position(col, row, depth);
        }
        throw new AssertionError("No structure symbol " + symbol);
    }

    private int[] position(int col, int row, int depth) {
        int[] offset = new int[3];
        machine.getExtendedFacing().getWorldOffset(new int[] { col - DimensionalFocusEngravingStructure.OFFSET_X,
            row - DimensionalFocusEngravingStructure.OFFSET_Y, depth - DimensionalFocusEngravingStructure.OFFSET_Z },
            offset);
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

    private ItemStack stack(int meta, int amount) {
        return new ItemStack(fixture, amount, meta);
    }

    private static boolean guaranteed(int[] chances) {
        return chances == null || Arrays.stream(chances).allMatch(chance -> chance == 10000);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            require(++stageFrames <= 2400, "Visual stage timeout");
            if (tooltipStage >= 0) {
                if (!(mc.currentScreen instanceof TooltipProbeGui) || ++frames < 40) return;
                String locale = tooltipStage < 2 ? "zh_CN" : "en_US";
                capture("tooltip-" + locale + (tooltipStage % 2 == 0 ? "-normal" : "-structure"));
                frames = stageFrames = 0;
                if (++tooltipStage == 4) {
                    report.append("Rendered normal and Shift tooltips in Chinese and English.\n");
                    finish();
                    return;
                }
                showTooltip();
                return;
            }
            if (!openedGui) {
                if (shownView == 2) {
                    verifyClientCoreStates();
                    return;
                }
                if (shownView != capturedView + 1 || requestedView >= 0) return;
                DimensionalFocusEngravingArray client = clientCore();
                if (client == null || !client.isCoreRendering()) {
                    frames = 0;
                    return;
                }
                if (!coreGlChecked) verifyCoreGlState(client);
                if (++frames < 120) return;
                capture(shownView == 0 ? "structure-overview" : "structure-textures");
                capturedView = shownView;
                frames = stageFrames = 0;
                if (capturedView == 0) requestedView = 1;
                else requestedView = 2;
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
            if (++frames < 60) return;
            capture("controller-gui");
            report.append("Rendered native controller GUI and original structure materials.\n");
            tooltipStage = 0;
            frames = stageFrames = 0;
            showTooltip();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private DimensionalFocusEngravingArray clientCore() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return null;
        TileEntity tile = mc.theWorld.getTileEntity(base.getXCoord(), base.getYCoord(), base.getZCoord());
        return tile instanceof BaseMetaTileEntity client
            && client.getMetaTileEntity() instanceof DimensionalFocusEngravingArray controller ? controller : null;
    }

    private void verifyClientCoreStates() throws Exception {
        DimensionalFocusEngravingArray client = clientCore();
        int expectedState = coreCaptureStage < 2 ? 0 : coreCaptureStage;
        boolean working = expectedState == 0 || expectedState == 3 || expectedState == 5;
        if (
            client == null || requestedCoreState >= 0
                || appliedCoreState != expectedState
                || client.isCoreRendering() != working
        ) {
            frames = 0;
            return;
        }
        require(client.getCoreRenderRgb() == machine.getCoreRenderRgb(), "Actual client receives native coil RGB");
        Vec3Impl offset = client.getCoreRenderOffset();
        require(offset.get0() == 0 && offset.get1() == -18 && offset.get2() == 22,
            "Actual client receives the controller alignment and original core center");
        if (++frames < 45) return;
        long phase = Math.floorMod(client.getCoreRenderTimer(), 100);
        if (coreCaptureStage == 0 && phase > 2) return;
        if (coreCaptureStage == 1 && (phase < 50 || phase > 52)) return;
        verifyCoreStream();
        String[] names = { "core-phase-0", "core-phase-50", "core-idle", "core-working", "core-computation-paused",
            "core-computation-restored", "core-unformed", "core-restored-idle" };
        capture(names[coreCaptureStage]);
        report.append("Actual client core state ").append(names[coreCaptureStage]).append(": rendering=")
            .append(client.isCoreRendering()).append(", rgb=").append(Integer.toHexString(client.getCoreRenderRgb()))
            .append(", phase=").append(phase).append(".\n");
        frames = stageFrames = 0;
        coreCaptureStage++;
        if (coreCaptureStage == 1) return;
        if (coreCaptureStage < names.length) requestedCoreState = coreCaptureStage;
        else requestGui = true;
    }

    private void verifyCoreStream() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            machine.writeToStream(buffer);
            BaseMetaTileEntity clientBase = new BaseMetaTileEntity();
            clientBase.setWorldObj(Minecraft.getMinecraft().theWorld);
            clientBase.xCoord = base.getXCoord();
            clientBase.yCoord = base.getYCoord();
            clientBase.zCoord = base.getZCoord();
            clientBase.setInitialValuesAsNBT(null, (short) GTNGMachineID.DimensionalFocusEngravingArray.id);
            DimensionalFocusEngravingArray copy = (DimensionalFocusEngravingArray) clientBase.getMetaTileEntity();
            copy.readFromStream(buffer);
            require(
                !buffer.isReadable() && copy.isCoreRendering() == machine.isCoreRendering()
                    && copy.getCoreRenderRgb() == machine.getCoreRenderRgb()
                    && copy.getCoreRenderOffset().equals(machine.getCoreRenderOffset()),
                "Native controller stream roundtrip preserves complete alignment, state and coil RGB");
        } finally {
            buffer.release();
        }
    }

    private void verifyCoreGlState(DimensionalFocusEngravingArray client) throws Exception {
        require(
            DimensionalFocusCoreGeometry.CYLINDERS == 6 && DimensionalFocusCoreGeometry.SIDES == 10
                && DimensionalFocusCoreGeometry.VERTEX_COUNT == 360
                && DimensionalFocusCoreGeometry.vertices().length == 1080,
            "Renderer retains the six original ten-sided open cylinders");
        require(
            DimensionalFocusCoreGeometry.color(client.getCoreRenderRgb(), 0, 0)
                != DimensionalFocusCoreGeometry.color(client.getCoreRenderRgb(), 50, 0),
            "Original pulse phases produce distinct native-coil colors");
        List<String> before = glState();
        Method draw = DimensionalFocusCoreRenderer.class.getDeclaredMethod("drawGeometry", double.class, double.class,
            double.class, int.class);
        draw.setAccessible(true);
        draw.invoke(null, 1000.0, 1000.0, 1000.0, DimensionalFocusCoreGeometry.color(client.getCoreRenderRgb(), 25, 0));
        List<String> after = glState();
        require(before.equals(after), "Core renderer restores GL state: before=" + before + "; after=" + after);
        coreGlChecked = true;
        report.append("Core geometry and native pulse colors match the source; synchronous drawing restores GL ")
            .append("program, matrices, framebuffer, textures, lighting, depth, blend and client array state.\n");
    }

    private static List<String> glState() {
        List<String> state = new ArrayList<>();
        for (int flag : new int[] { GL11.GL_LIGHTING, GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_ALPHA_TEST,
            GL11.GL_CULL_FACE, GL11.GL_TEXTURE_2D }) state.add(flag + "=" + GL11.glIsEnabled(flag));
        for (int value : new int[] { GL11.GL_MATRIX_MODE, GL11.GL_MODELVIEW_STACK_DEPTH, GL11.GL_PROJECTION_STACK_DEPTH,
            GL13.GL_ACTIVE_TEXTURE, GL13.GL_CLIENT_ACTIVE_TEXTURE, GL20.GL_CURRENT_PROGRAM, GL11.GL_TEXTURE_BINDING_2D,
            GL11.GL_BLEND_SRC, GL11.GL_BLEND_DST, GL15.GL_ARRAY_BUFFER_BINDING, GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING })
            state.add(value + "=" + GL11.glGetInteger(value));
        state.add("depthMask=" + GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        if (GLContext.getCapabilities().OpenGL30) {
            state.add("drawFbo=" + GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
            state.add("readFbo=" + GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
        } else if (GLContext.getCapabilities().GL_EXT_framebuffer_object)
            state.add("fbo=" + GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT));
        for (int value : new int[] { GL11.GL_CURRENT_COLOR, GL11.GL_MODELVIEW_MATRIX, GL11.GL_PROJECTION_MATRIX,
            GL11.GL_TEXTURE_MATRIX }) {
            FloatBuffer values = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(value, values);
            float[] snapshot = new float[value == GL11.GL_CURRENT_COLOR ? 4 : 16];
            values.get(snapshot);
            state.add(value + "=" + Arrays.toString(snapshot));
        }
        return state;
    }

    private MultiblockTooltipBuilder verifyTooltips(String locale) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (Language language : mc.getLanguageManager().getLanguages())
            if (language.getLanguageCode().equals(locale)) {
                mc.getLanguageManager().setCurrentLanguage(language);
                mc.gameSettings.language = locale;
                mc.refreshResources();
                break;
            }
        Method create = DimensionalFocusEngravingArray.class.getDeclaredMethod("createTooltip");
        create.setAccessible(true);
        MultiblockTooltipBuilder tooltip = (MultiblockTooltipBuilder) create.invoke(machine);
        String text = String.join("\n", tooltip.getInformation()) + "\n\n"
            + String.join("\n", tooltip.getStructureInformation());
        require(!text.contains("gtng.") && !text.contains("gt.mbtt.") && !text.contains("gt.blockmachines."),
            "All tooltip translations resolve: " + locale);
        require(text.contains("89"), "Tooltip reports original structure dimensions: " + locale);
        Files.write(new File(output(), "tooltip-" + locale + ".txt").toPath(), text.getBytes(StandardCharsets.UTF_8));
        return tooltip;
    }

    private void showTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = 1;
        if (tooltipStage % 2 == 0) currentTooltip = verifyTooltips(tooltipStage < 2 ? "zh_CN" : "en_US");
        List<String> lines = new ArrayList<>();
        lines.add(GTNGItemList.DimensionalFocusEngravingArray.get(1).getDisplayName());
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
                "Rendered tooltip fits viewport: " + textWidth + "x" + textHeight + " in " + width + "x" + height);
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
        System.out.println("DIMENSIONAL_FOCUS_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.dimensionalFocus.output"));
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
