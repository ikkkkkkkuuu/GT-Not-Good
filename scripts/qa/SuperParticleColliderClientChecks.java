package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
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
import net.minecraft.item.ItemStack;
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

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.SuperParticleCollider;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperParticleColliderStructure;
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
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.data.GTCoilTracker;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gtPlusPlus.core.material.MaterialsElements;

@Mod(
    modid = "particlecolliderqa",
    name = "Super Particle Collider QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class SuperParticleColliderClientChecks {

    private BaseMetaTileEntity base;
    private volatile SuperParticleCollider machine;
    private final String[][] shape = SuperParticleColliderStructure.shape();
    private final StringBuilder report = new StringBuilder();
    private GTRecipe[] recipes;
    private boolean started;
    private volatile boolean checked, finished;
    private volatile boolean observedActiveCoils, observedInactiveCoils;
    private volatile boolean completeClientStructure;
    private volatile int clientStructureBlocks, clientActiveCoils, clientInactiveCoils, clientGeometryStableTicks;
    private volatile ClientBlock[] expectedClientBlocks;
    private RunningPhase runningPhase;
    private int phaseStartedTick;
    private volatile int requestedView = -1, shownView = -1;
    private volatile boolean requestedGui, openedGui;
    private int clientTicks, frames, stageFrames, capturedView = -1, warmedView = -1;
    private volatile int serverTicks;
    private int tooltipStage = -1, savedProgress;
    private int[] brokenCasing;
    private MultiblockTooltipBuilder currentTooltip;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.particleCollider.qa")) return;
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 7200, "Client QA timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = tooltipStage >= 0 ? 1 : 2;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0F);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 12;
            if (mc.theWorld != null && expectedClientBlocks != null) observeClientStructure(mc);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("particle-collider-qa-" + System.currentTimeMillis(), "Particle Collider QA",
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
            require(++serverTicks <= 4800, "Server QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) {
                require(
                    GTNGItemList.SuperParticleCollider.get(1).getItemDamage() == GTNGMachineID.SuperParticleCollider.id,
                    "Registered controller identity");
                player.worldObj.setWorldTime(6000);
                buildRig(player, 0, 12, 0, ForgeDirection.NORTH);
                player.playerNetServerHandler.setPlayerLocation(0, 28, 23, 180, 70);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
            }
            if (serverTicks == 60) {
                verifyStructure();
                verifyDirections(player);
                verifyRequiredBlocks();
                verifyUnconstrainedSpace();
                verifySurvivalRecipes();
                verifyNativeRecipes();
                verifyNativeMeBuffer();
                verifyParallelAndOverclock();
                verifyVoltageGate();
                verifyOutputProtection();
                resetProcessing();
                machine.stopMachine(ShutDownReasonRegistry.NONE);
                cacheClientStructure();
                beginPhase(RunningPhase.WaitStructure);
            }
            if (serverTicks > 60) chargeEnergy();
            if (runningPhase != null) advanceRunningChecks();
            if (checked && requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                player.closeScreen();
                if (view == 0) player.playerNetServerHandler.setPlayerLocation(-50, 40, -26, -45.6F, 22);
                else player.playerNetServerHandler.setPlayerLocation(-12, 19, -8, -45, 28);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                shownView = view;
            }
            if (requestedGui) {
                requestedGui = false;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0, 13, -4, 0, 0);
                machine.onRightclick(base, player);
                openedGui = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void beginPhase(RunningPhase phase) {
        runningPhase = phase;
        phaseStartedTick = serverTicks;
        System.out.println("PARTICLE_COLLIDER_QA_PHASE " + phase + " at " + serverTicks);
    }

    private void advanceRunningChecks() {
        require(serverTicks - phaseStartedTick <= 1200,
            "Timed out in " + runningPhase
                + "; client structure="
                + clientStructureBlocks
                + "/2799, active coils="
                + clientActiveCoils
                + ", inactive coils="
                + clientInactiveCoils);
        switch (runningPhase) {
            case WaitStructure -> {
                if (!completeClientStructure || clientInactiveCoils != 320 || clientGeometryStableTicks < 20) return;
                verifyCoils(false);
                startRunning();
                beginPhase(RunningPhase.WaitActive);
            }
            case WaitActive -> {
                if (!base.isActive() || machine.mProgresstime <= 0 || clientActiveCoils != 320) return;
                verifyCoils(true);
                base.disableWorking();
                machine.stopMachine(ShutDownReasonRegistry.NONE);
                beginPhase(RunningPhase.WaitStopped);
            }
            case WaitStopped -> {
                if (serverTicks <= phaseStartedTick || clientInactiveCoils != 320) return;
                verifyCoils(false);
                startRunning();
                beginPhase(RunningPhase.WaitRestarted);
            }
            case WaitRestarted -> {
                if (!base.isActive() || clientActiveCoils != 320) return;
                verifyCoils(true);
                brokenCasing = firstPosition('c');
                base.getWorld().setBlockToAir(brokenCasing[0], brokenCasing[1], brokenCasing[2]);
                require(!machine.checkStructure(true, base), "Running structure becomes invalid after removing casing");
                base.disableWorking();
                beginPhase(RunningPhase.WaitBroken);
            }
            case WaitBroken -> {
                if (serverTicks <= phaseStartedTick || clientInactiveCoils != 320) return;
                verifyCoils(false);
                base.getWorld().setBlock(brokenCasing[0], brokenCasing[1], brokenCasing[2],
                    ParticleColliderCasing.Lafium.getBlock(), ParticleColliderCasing.Lafium.getMetadata(), 3);
                machine.stopMachine(ShutDownReasonRegistry.NONE);
                startRunning();
                beginPhase(RunningPhase.WaitRepaired);
            }
            case WaitRepaired -> {
                if (!completeClientStructure || !base.isActive() || clientActiveCoils != 320) return;
                verifyCoils(true);
                verifyPersistence();
                beginPhase(RunningPhase.WaitReloadedProgress);
            }
            case WaitReloadedProgress -> {
                if (
                    serverTicks - phaseStartedTick < 10 || machine.mProgresstime <= savedProgress
                        || clientActiveCoils != 320
                        || !completeClientStructure
                ) return;
                verifyCoils(true);
                report.append("The client receives all 2799 structure blocks before processing checks. Real server ")
                    .append("ticks activate all 320 client coils; stop and broken-structure checks wait for complete ")
                    .append("client deactivation. A running tile reload preserves and advances processing.\n");
                runningPhase = null;
                checked = true;
                requestedView = 0;
            }
        }
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

    private void buildRig(EntityPlayerMP player, int x, int y, int z, ForgeDirection facing) {
        World world = player.worldObj;
        for (int cx = (x - 64) >> 4; cx <= (x + 64) >> 4; cx++)
            for (int cz = (z - 64) >> 4; cz <= (z + 64) >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
        base = place(world, x, y, z, GTNGMachineID.SuperParticleCollider.id);
        base.setFrontFacing(facing);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        machine = (SuperParticleCollider) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        verifyConstructedGeometry();
        placeAt(21, 2, 0, ItemList.Hatch_Input_UV);
        placeAt(22, 2, 0, ItemList.Hatch_Input_UV);
        placeAt(24, 2, 0, ItemList.Hatch_Output_UV);
        placeAt(25, 2, 0, ItemList.Hatch_Maintenance);
        setEnergy(ItemList.Hatch_Energy_UV);
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(1);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
    }

    private void verifyConstructedGeometry() {
        Map<Character, ItemStack> blocks = new HashMap<>();
        int total = 0;
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++) {
            for (int col = 0; col < shape[row][depth].length(); col++) {
                char symbol = shape[row][depth].charAt(col);
                if (symbol == ' ' || symbol == '~') continue;
                int[] pos = position(col, row, depth);
                Block block = base.getWorld().getBlock(pos[0], pos[1], pos[2]);
                require(block != Blocks.air, "Built required block " + symbol + " at " + Arrays.toString(pos));
                ItemStack stack = new ItemStack(block, 1, base.getWorld().getBlockMetadata(pos[0], pos[1], pos[2]));
                ItemStack expected = blocks.putIfAbsent(symbol, stack);
                require(expected == null || GTUtility.areStacksEqual(stack, expected),
                    "Consistent constructed block for symbol " + symbol);
                total++;
            }
        }
        require(total == 2799 && blocks.size() == 5, "All 2799 upstream structure blocks are built");
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Structure: " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Physical controller forms the complete 47x7x47 structure");
        require(
            machine.mInputHatches.size() == 2 && machine.mOutputHatches.size() == 1
                && machine.mEnergyHatches.size() >= 1
                && machine.mMaintenanceHatches.size() == 1,
            "Physical fluid, energy and maintenance hatches are discovered");
    }

    private void verifyDirections(EntityPlayerMP player) {
        BaseMetaTileEntity mainBase = base;
        SuperParticleCollider mainMachine = machine;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST }) {
            buildRig(player, 192, 12, 192, facing);
            verifyStructure();
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++) {
                for (int col = 0; col < shape[row][depth].length(); col++) {
                    int[] pos = position(col, row, depth);
                    base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
                }
            }
        }
        base = mainBase;
        machine = mainMachine;
        verifyStructure();
        report.append("Original 47x7x47 geometry builds and forms in all four horizontal directions.\n");
    }

    private void verifyRequiredBlocks() {
        for (char symbol : new char[] { 'c', 'd', 'e', 'f' }) {
            int[] pos = firstPosition(symbol);
            World world = base.getWorld();
            Block block = world.getBlock(pos[0], pos[1], pos[2]);
            int meta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
            world.setBlockToAir(pos[0], pos[1], pos[2]);
            require(!machine.checkStructure(true, base), "Missing required block " + symbol + " invalidates structure");
            world.setBlock(pos[0], pos[1], pos[2], block, meta, 3);
            verifyStructure();
        }
        report.append("All four fixed material symbols reject missing blocks and recover after repair.\n");
    }

    private void verifyUnconstrainedSpace() {
        require(shape[3][23].charAt(23) == ' ', "Selected upstream any position in the ring center");
        int[] pos = position(23, 3, 23);
        base.getWorld().setBlock(pos[0], pos[1], pos[2], Blocks.stone, 0, 3);
        verifyStructure();
        base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
        verifyStructure();
        report.append("The unconstrained ring center accepts both solid blocks and air.\n");
    }

    private void verifySurvivalRecipes() {
        for (ParticleColliderCasing casing : ParticleColliderCasing.values()) {
            List<GTRecipe> matches = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
                .filter(recipe -> Arrays.stream(recipe.mOutputs)
                    .anyMatch(output -> GTUtility.areStacksEqual(output, casing.getStack(1))))
                .collect(Collectors.toList());
            require(matches.size() == 1, "Registered survival recipe for " + casing);
            GTRecipe recipe = matches.get(0);
            int expectedInputs = casing == ParticleColliderCasing.Lafium ? 3
                : casing == ParticleColliderCasing.AcceleratedPipeline ? 6 : 2;
            boolean pipeline = casing == ParticleColliderCasing.AcceleratedPipeline;
            require(
                recipe.mDuration == (pipeline ? 400 : 50) && recipe.mEUt == (pipeline ? TierEU.RECIPE_IV : 16)
                    && recipe.mInputs.length == expectedInputs
                    && recipe.mFluidInputs.length == (pipeline ? 1 : 0)
                    && recipe.mOutputs.length == 1
                    && recipe.mOutputs[0].stackSize == (casing == ParticleColliderCasing.Lafium ? 2 : 1),
                "Exact assembler timing, voltage, yield and complete ingredient lanes for " + casing);
            for (ItemStack input : recipe.mInputs)
                require(input != null && input.getItem() != null && input.stackSize >= 0, "Runtime item for " + casing);
            for (FluidStack input : recipe.mFluidInputs)
                require(input != null && input.getFluid() != null && input.amount > 0, "Runtime fluid for " + casing);
            if (pipeline)
                require(
                    recipe.mFluidInputs[0].isFluidEqual(Materials.SolderingAlloy.getMolten(288))
                        && recipe.mFluidInputs[0].amount == 288,
                    "Accelerated pipeline requires 288 L of soldering alloy");
        }
        List<GTRecipe.RecipeAssemblyLine> controllers = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, GTNGItemList.SuperParticleCollider.get(1)))
            .collect(Collectors.toList());
        require(controllers.size() == 1, "Controller has one native assembly-line recipe");
        var controller = controllers.get(0);
        require(
            controller.mDuration == 800 && controller.mEUt == TierEU.RECIPE_ZPM
                && controller.mResearchTime == 1200
                && controller.mResearchVoltage == TierEU.RECIPE_ZPM
                && controller.mResearchItem != null
                && controller.mInputs.length == 10
                && controller.mFluidInputs.length == 4,
            "Controller preserves ZPM assembly, research and complete input lanes");
        for (ItemStack input : controller.mInputs)
            require(input != null && input.getItem() != null && input.stackSize > 0, "Controller runtime item");
        for (FluidStack input : controller.mFluidInputs)
            require(input != null && input.getFluid() != null && input.amount == 1152, "Controller runtime fluid");
        for (int index = 2; index <= 5; index++) require(
            controller.mInputs[index].stackSize == 64
                && GTUtility.areStacksEqual(controller.mInputs[index], ItemList.Circuit_Chip_UHPIC.get(1)),
            "Controller retains four separate 64-chip UHPIC lanes");
        ItemStack uv = GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UV, 32);
        require(uv != null && GTRecipe.RecipeAssemblyLine.getMatchedIngredientAmount(uv, controller.mInputs[7],
            controller.mOreDictAlt[7]) == 32, "Controller retains its 32 UV circuit prerequisite");
        report.append("Three casings and the controller have complete, available native survival recipes.\n");
    }

    private void verifyNativeRecipes() {
        MaterialsElements elements = MaterialsElements.getInstance();
        FluidStack[] inputs = { Materials.Plutonium.getMolten(4096), Materials.Thorium.getMolten(4096),
            Materials.Uranium.getMolten(4096), elements.PROTACTINIUM.getFluidStack(4096),
            elements.CALIFORNIUM.getFluidStack(4096) };
        FluidStack[] outputs = { elements.CURIUM.getFluidStack(4000), Materials.Uranium.getMolten(4000),
            Materials.Plutonium.getMolten(4000), elements.NEPTUNIUM.getFluidStack(4000),
            elements.FERMIUM.getFluidStack(4000) };
        require(map().getAllRecipes().size() == 5, "Exactly five native element transmutations are registered");
        recipes = new GTRecipe[5];
        for (int index = 0; index < recipes.length; index++) {
            FluidStack input = inputs[index], output = outputs[index];
            List<GTRecipe> matches = map().getAllRecipes().stream()
                .filter(recipe -> recipe.mFluidInputs.length == 2 && recipe.mFluidOutputs.length == 1
                    && recipe.mFluidInputs[0].isFluidEqual(input)
                    && recipe.mFluidOutputs[0].isFluidEqual(output))
                .collect(Collectors.toList());
            require(matches.size() == 1, "Native collision pair " + index);
            GTRecipe recipe = recipes[index] = matches.get(0);
            require(
                recipe.mInputs.length == 0 && recipe.mOutputs.length == 0
                    && recipe.mDuration == 200
                    && recipe.mEUt == TierEU.RECIPE_UV
                    && recipe.mFluidInputs[0].amount == 4096
                    && recipe.mFluidInputs[1].isFluidEqual(Materials.Helium.getPlasma(4096))
                    && recipe.mFluidInputs[1].amount == 4096
                    && recipe.mFluidOutputs[0].amount == 4000,
                "Exact native fluids, UV voltage and 200-tick duration for collision " + index);
            boolean downstream = RecipeMap.ALL_RECIPE_MAPS.values().stream().filter(other -> other != map())
                .flatMap(other -> other.getAllRecipes().stream()).anyMatch(other -> other.mFluidInputs != null && Arrays
                    .stream(other.mFluidInputs).anyMatch(fluid -> fluid != null && fluid.isFluidEqual(output)));
            require(downstream, "Native product has an existing downstream recipe: " + output.getLocalizedName());
            resetProcessing();
            seed(recipe, 1);
            require(machine.checkProcessing().wasSuccessful(), "Real registered collision processes " + index);
            require(
                inputAmount(0) == 0 && inputAmount(1) == 0
                    && pendingOutput(output) == 4000
                    && machine.mMaxProgresstime == 200
                    && machine.lEUt == -TierEU.RECIPE_UV,
                "Collision consumes both exact inputs and schedules exact output/power/time " + index);
        }
        report.append("All five registered native element recipes consume 4096 L of each input and schedule ")
            .append("4000 L at UV/200 ticks; every product has an existing downstream use.\n");
    }

    private void verifyParallelAndOverclock() {
        GTRecipe recipe = recipes[0];
        resetProcessing();
        machine.setPowerPanelMaxParallel(8);
        seed(recipe, 3);
        require(machine.checkProcessing().wasSuccessful(), "Native parallel processing starts");
        require(
            pendingOutput(recipe.mFluidOutputs[0]) == 8000 && inputAmount(0) == 4096
                && inputAmount(1) == 4096
                && machine.mMaxProgresstime == 200
                && machine.lEUt == -2L * TierEU.RECIPE_UV,
            "Two-amp UV hatch limits three available recipes to two parallel operations");
        resetProcessing();
        machine.setPowerPanelMaxParallel(1);
        setEnergy(ItemList.Hatch_Energy_UHV);
        verifyStructure();
        seed(recipe, 1);
        require(
            machine.checkProcessing().wasSuccessful() && machine.mMaxProgresstime == 100
                && machine.lEUt == -4L * TierEU.RECIPE_UV
                && pendingOutput(recipe.mFluidOutputs[0]) == 4000,
            "One native non-perfect overclock halves time and quadruples EU/t");
        resetProcessing();
        setEnergy(ItemList.Hatch_Energy_UV);
        placeAt(22, 3, 0, ItemList.Hatch_Energy_UV);
        verifyStructure();
        seed(recipe, 1);
        require(
            machine.checkProcessing().wasSuccessful() && machine.mMaxProgresstime == 200
                && machine.lEUt == -TierEU.RECIPE_UV,
            "Additional amperage does not overclock a voltage-limited recipe");
        restoreCasing(22, 3, 0);
        verifyStructure();
        report.append("Native input power bounds parallels; voltage provides 4x EU/t, 2x speed overclocking, ")
            .append("and additional amperage never substitutes for voltage.\n");
    }

    private void verifyNativeMeBuffer() {
        resetProcessing();
        int[] pos = position(21, 4, 0);
        BaseMetaTileEntity bufferBase = place(base.getWorld(), pos[0], pos[1], pos[2],
            ItemList.Hatch_CraftingInput_Bus_ME.get(1).getItemDamage());
        MTEHatchCraftingInputME buffer = (MTEHatchCraftingInputME) bufferBase.getMetaTileEntity();
        require(buffer.supportsFluids(), "Physical native crafting input buffer supports fluids");
        ItemStack encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        for (FluidStack fluid : recipes[0].mFluidInputs) {
            NBTTagCompound nativeEntry = new NBTTagCompound();
            Platform.writeStackNBT(AEFluidStack.create(fluid.copy()), nativeEntry);
            inputs.appendTag(nativeEntry);
        }
        for (int index = inputs.tagCount(); index < 16; index++) inputs.appendTag(new NBTTagCompound());
        NBTTagCompound result = new NBTTagCompound();
        Platform.writeStackNBT(AEFluidStack.create(recipes[0].mFluidOutputs[0].copy()), result);
        outputs.appendTag(result);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        encoded = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
        var pattern = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, base.getWorld());
        require(
            pattern instanceof UltimatePatternHelper && Arrays.stream(pattern.getAEInputs())
                .filter(entry -> entry instanceof IAEFluidStack && entry.getStackSize() == 4096).count() == 2,
            "Persisted Ultimate Encoded Pattern contains two exact native fluid entries");
        MEInventoryCrafting delivered = new MEInventoryCrafting(new ContainerNull(), 4, 4);
        int slot = 0;
        for (var entry : pattern.getAEInputs()) if (entry != null) {
            delivered.setInventorySlotContents(slot, entry.copy());
            require(delivered.getAEStackInSlot(slot) instanceof IAEFluidStack,
                "CPU delivery exposes native fluids through getAEStackInSlot");
            slot++;
        }
        buffer.setInventorySlotContents(0, encoded);
        var inventory = buffer.inventories().next();
        require(inventory.insertItemsAndFluids(delivered) && inventory.getFluidInputs().length == 2,
            "Native PatternSlot accepts both fluids from the real MEInventoryCrafting");
        bufferBase = reloadTile(bufferBase);
        buffer = (MTEHatchCraftingInputME) bufferBase.getMetaTileEntity();
        inventory = buffer.inventories().next();
        require(
            inventory.getPatternDetails() instanceof UltimatePatternHelper && inventory.getFluidInputs().length == 2
                && Arrays.stream(inventory.getFluidInputs()).allMatch(fluid -> fluid.amount == 4096),
            "Physical native buffer NBT reload retains the Ultimate pattern and both delivered fluids");
        verifyStructure();
        require(machine.mDualInputHatches.contains(buffer), "Physical structure discovers the real dual input buffer");
        require(
            machine.checkProcessing().wasSuccessful() && inventory.isEmpty()
                && pendingOutput(recipes[0].mFluidOutputs[0]) == 4000
                && machine.mMaxProgresstime == 200
                && machine.lEUt == -TierEU.RECIPE_UV,
            "Native dual-input processing commits both buffered fluids and schedules the exact collision output");
        NBTTagCompound consumed = new NBTTagCompound();
        buffer.saveNBTData(consumed);
        buffer.loadNBTData(consumed);
        require(buffer.inventories().next().isEmpty(), "Consumed native fluid job stays empty after buffer NBT reload");
        restoreCasing(21, 4, 0);
        verifyStructure();
        report.append("A real native ME crafting input buffer accepts, persists and consumes a two-fluid Ultimate ")
            .append("Encoded Pattern job through MEInventoryCrafting and the native dual-input recipe path.\n");
    }

    private void verifyVoltageGate() {
        resetProcessing();
        setEnergy(ItemList.Hatch_Energy_ZPM);
        placeAt(22, 3, 0, ItemList.Hatch_Energy_ZPM);
        verifyStructure();
        seed(recipes[0], 1);
        require(!machine.checkProcessing().wasSuccessful() && inputAmount(0) == 4096 && inputAmount(1) == 4096,
            "Combined low-voltage hatch power cannot unlock a UV recipe or consume its fluids");
        restoreCasing(22, 3, 0);
        setEnergy(ItemList.Hatch_Energy_UV);
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful(), "UV hatch upgrade unlocks the preserved recipe inputs");
        resetProcessing();
        seed(recipes[0], 1);
        machine.mInputHatches.get(1).mFluid.amount--;
        require(!machine.checkProcessing().wasSuccessful() && inputAmount(0) == 4096 && inputAmount(1) == 4095,
            "One missing litre refuses processing without consuming either fluid");
        report.append("Insufficient voltage and missing plasma reject processing without input loss.\n");
    }

    private void verifyOutputProtection() {
        resetProcessing();
        var hatch = machine.mOutputHatches.get(0);
        hatch.setFillableStack(new FluidStack(FluidRegistry.WATER, hatch.getCapacity()));
        seed(recipes[0], 1);
        require(
            !machine.checkProcessing().wasSuccessful() && inputAmount(0) == 4096
                && inputAmount(1) == 4096
                && pendingOutput(recipes[0].mFluidOutputs[0]) == 0
                && hatch.getFluidAmount() == hatch.getCapacity(),
            "Protected full output hatch refuses atomically and keeps both inputs and stored output");
        hatch.setFillableStack(null);
        require(machine.checkProcessing().wasSuccessful() && pendingOutput(recipes[0].mFluidOutputs[0]) == 4000,
            "Clearing output storage permits retry");
        report.append("Full protected output storage preserves all inputs and permits a clean retry.\n");
    }

    private void startRunning() {
        resetProcessing();
        verifyStructure();
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.mStartUpCheck = 0;
        machine.mUpdate = 0;
        machine.setPowerPanelMaxParallel(1);
        seed(recipes[0], 16);
        require(machine.checkProcessing().wasSuccessful(), "Start a real cycle for tick and visual checks");
        chargeEnergy();
        base.enableWorking();
    }

    private void chargeEnergy() {
        for (var hatch : machine.mEnergyHatches) {
            var energy = hatch.getBaseMetaTileEntity();
            ((BaseMetaTileEntity) energy).setStoredEU(energy.getEUCapacity());
        }
    }

    private void verifyPersistence() {
        savedProgress = machine.mProgresstime;
        int duration = machine.mMaxProgresstime;
        long eut = machine.lEUt;
        base = reloadTile(base);
        machine = (SuperParticleCollider) base.getMetaTileEntity();
        require(
            machine.mProgresstime == savedProgress && machine.mMaxProgresstime == duration
                && machine.lEUt == eut
                && pendingOutput(recipes[0].mFluidOutputs[0]) == 4000,
            "Real running tile NBT reload preserves elapsed time, duration, power and pending fluids");
        verifyStructure();
        machine.mStartUpCheck = 0;
        machine.mUpdate = 0;
        chargeEnergy();
        base.enableWorking();
    }

    private void verifyCoils(boolean active) {
        int count = 0;
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++) {
            for (int col = 0; col < shape[row][depth].length(); col++) {
                if (shape[row][depth].charAt(col) != 'e') continue;
                int[] pos = position(col, row, depth);
                World world = base.getWorld();
                require(
                    world.getBlock(pos[0], pos[1], pos[2]) == ParticleColliderCasing.SuperconductingCoil.getBlock()
                        && world.getBlockMetadata(pos[0], pos[1], pos[2])
                            == ParticleColliderCasing.SuperconductingCoil.getMetadata()
                        && GTCoilTracker.isCoilActive(world, pos[0], pos[1], pos[2]) == active,
                    "Coil has native running state " + active + " at " + Arrays.toString(pos));
                count++;
            }
        }
        require(count == 320, "Every original superconducting coil is checked");
        if (active) require(machine.mCoils.size() == 320, "Native controller tracks every superconducting coil");
    }

    private void cacheClientStructure() {
        List<ClientBlock> blocks = new ArrayList<>();
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++) {
            for (int col = 0; col < shape[row][depth].length(); col++) {
                char symbol = shape[row][depth].charAt(col);
                if (symbol == ' ' || symbol == '~') continue;
                int[] pos = position(col, row, depth);
                World world = base.getWorld();
                blocks.add(new ClientBlock(pos[0], pos[1], pos[2], world.getBlock(pos[0], pos[1], pos[2]),
                    world.getBlockMetadata(pos[0], pos[1], pos[2]), symbol == 'e'));
            }
        }
        require(blocks.size() == 2799, "Expected client geometry contains every original structure block");
        expectedClientBlocks = blocks.toArray(new ClientBlock[0]);
    }

    private void observeClientStructure(Minecraft mc) {
        int matched = 0, active = 0, inactive = 0;
        for (ClientBlock expected : expectedClientBlocks) {
            if (mc.theWorld.getBlock(expected.x, expected.y, expected.z) != expected.block) continue;
            int meta = mc.theWorld.getBlockMetadata(expected.x, expected.y, expected.z);
            if (expected.coil) {
                if (meta == expected.metadata) inactive++;
                if (meta == (expected.metadata | ParticleColliderCasing.ACTIVE_BIT)) active++;
                meta &= ~ParticleColliderCasing.ACTIVE_BIT;
            }
            if (meta == expected.metadata) matched++;
        }
        clientStructureBlocks = matched;
        clientActiveCoils = active;
        clientInactiveCoils = inactive;
        completeClientStructure = matched == 2799 && active + inactive == 320;
        clientGeometryStableTicks = completeClientStructure ? clientGeometryStableTicks + 1 : 0;
        if (active == 320) observedActiveCoils = true;
        if (inactive == 320 && observedActiveCoils) observedInactiveCoils = true;
    }

    private static final class ClientBlock {

        private final int x, y, z, metadata;
        private final Block block;
        private final boolean coil;

        private ClientBlock(int x, int y, int z, Block block, int metadata, boolean coil) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.metadata = metadata;
            this.coil = coil;
        }
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

    private void resetProcessing() {
        base.disableWorking();
        machine.mMaxProgresstime = machine.mProgresstime = 0;
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.lEUt = 0;
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (var hatch : machine.mOutputHatches) hatch.setFillableStack(null);
    }

    private void seed(GTRecipe recipe, int operations) {
        for (int index = 0; index < recipe.mFluidInputs.length; index++) {
            FluidStack fluid = recipe.mFluidInputs[index].copy();
            fluid.amount *= operations;
            machine.mInputHatches.get(index).mFluid = fluid;
        }
    }

    private int inputAmount(int index) {
        FluidStack fluid = machine.mInputHatches.get(index).mFluid;
        return fluid == null ? 0 : fluid.amount;
    }

    private int pendingOutput(FluidStack expected) {
        return machine.mOutputFluids == null ? 0
            : Arrays.stream(machine.mOutputFluids).filter(fluid -> fluid != null && fluid.isFluidEqual(expected))
                .mapToInt(fluid -> fluid.amount).sum();
    }

    private void setEnergy(ItemList hatch) {
        placeAt(21, 3, 0, hatch);
    }

    private void placeAt(int col, int row, int depth, ItemList hatch) {
        int[] pos = position(col, row, depth);
        place(base.getWorld(), pos[0], pos[1], pos[2], hatch.get(1).getItemDamage());
    }

    private void restoreCasing(int col, int row, int depth) {
        int[] pos = position(col, row, depth);
        base.getWorld().setBlock(pos[0], pos[1], pos[2], ParticleColliderCasing.Lafium.getBlock(),
            ParticleColliderCasing.Lafium.getMetadata(), 3);
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
        machine.getExtendedFacing()
            .getWorldOffset(
                new int[] { col - SuperParticleColliderStructure.OFFSET_X,
                    row - SuperParticleColliderStructure.OFFSET_Y, depth - SuperParticleColliderStructure.OFFSET_Z },
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

    private static RecipeMap<?> map() {
        return GTNGRecipeMaps.SuperParticleColliderRecipes;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            require(++stageFrames <= 3600,
                "Visual stage timeout; client structure=" + clientStructureBlocks
                    + "/2799, active coils="
                    + clientActiveCoils
                    + ", view="
                    + shownView
                    + ", camera="
                    + (mc.thePlayer == null ? "missing"
                        : mc.thePlayer.posX + "," + mc.thePlayer.boundingBox.minY + "," + mc.thePlayer.posZ)
                    + ", frames="
                    + frames);
            if (tooltipStage >= 0) {
                if (!(mc.currentScreen instanceof TooltipProbeGui) || ++frames < 40) return;
                String locale = tooltipStage < 2 ? "zh_CN" : "en_US";
                capture("tooltip-" + locale + (tooltipStage % 2 == 0 ? "-normal" : "-structure"));
                frames = stageFrames = 0;
                if (++tooltipStage == 4) {
                    report.append("Rendered native GUI and normal/Shift tooltips in Chinese and English.\n");
                    finish();
                    return;
                }
                showTooltip();
                return;
            }
            if (!openedGui) {
                if (shownView != capturedView + 1 || requestedView >= 0) return;
                double cameraX = shownView == 0 ? -50 : -12;
                double cameraY = shownView == 0 ? 40 : 19;
                double cameraZ = shownView == 0 ? -26 : -8;
                boolean atCamera = mc.thePlayer != null && Math.abs(mc.thePlayer.posX - cameraX) < 1
                    && Math.abs(mc.thePlayer.boundingBox.minY - cameraY) < 3
                    && Math.abs(mc.thePlayer.posZ - cameraZ) < 1;
                if (
                    !atCamera || !completeClientStructure || clientGeometryStableTicks < 40 || clientActiveCoils != 320
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
            if (!(mc.currentScreen instanceof GuiContainerWrapper) || ++frames < 40) return;
            capture("native-gui");
            frames = stageFrames = 0;
            tooltipStage = 0;
            showTooltip();
        } catch (Throwable failure) {
            fail(failure);
        }
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
        Method create = SuperParticleCollider.class.getDeclaredMethod("createTooltip");
        require(
            GTNGItemList.SuperParticleCollider.get(1).getDisplayName()
                .equals(StatCollector.translateToLocal(machine.getLocalNameKey())),
            "Controller name follows locale: " + locale);
        create.setAccessible(true);
        MultiblockTooltipBuilder tooltip = (MultiblockTooltipBuilder) create.invoke(machine);
        String text = String.join("\n", tooltip.getInformation()) + "\n\n"
            + String.join("\n", tooltip.getStructureInformation());
        require(!text.contains("gtng.") && !text.contains("gt.mbtt.") && !text.contains("gt.blockmachines."),
            "All tooltip translations resolve: " + locale);
        require(text.contains("47") && text.contains("7"), "Tooltip reports original structure dimensions: " + locale);
        Files.write(new File(output(), "tooltip-" + locale + ".txt").toPath(), text.getBytes(StandardCharsets.UTF_8));
        return tooltip;
    }

    private void showTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = 1;
        if (tooltipStage % 2 == 0) currentTooltip = verifyTooltips(tooltipStage < 2 ? "zh_CN" : "en_US");
        List<String> lines = new ArrayList<>();
        lines.add(GTNGItemList.SuperParticleCollider.get(1).getDisplayName());
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
        require(observedActiveCoils && observedInactiveCoils,
            "Native network packets synchronize all 320 client coils into both running and stopped states");
        Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
        System.out.println("PARTICLE_COLLIDER_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.particleCollider.output"));
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
