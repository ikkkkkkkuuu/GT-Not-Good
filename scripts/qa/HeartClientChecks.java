package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.Language;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.xyp.gtnotgood.client.nei.HeartGenerationFrontend;
import com.xyp.gtnotgood.client.render.HeartBlackHoleRenderer;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ParameterModuleHatch;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.HeartOfTheUniverse;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularGeneratorBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.HeartOfTheUniverseStructure;
import com.xyp.gtnotgood.common.machines.processing.GenerationWork;
import com.xyp.gtnotgood.common.machines.processing.HeartGenerationRules;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
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
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.nei.GTNEIDefaultHandler;
import gtPlusPlus.core.material.MaterialsElements;

@Mod(modid = "heartqa", name = "Heart QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class HeartClientChecks {

    private final boolean maintenanceOnly = Boolean.getBoolean("gtng.heart.maintenanceOnly");
    private final String[][] shape = HeartOfTheUniverseStructure.shape();
    private final List<int[]> hatchPositions = new ArrayList<>();
    private final List<Rig> visualRigs = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private Rig rig;
    private boolean started;
    private volatile boolean checked, finished;
    private volatile int requestedView = -1, shownView = -1;
    private int clientTicks, serverTicks, frames, capturedView = -1;
    private long controlledTick = 1_000_000;
    private long firstRenderedFrame;
    private GTNEIDefaultHandler controllerNeiHandler;
    private GTNEIDefaultHandler generationNeiHandler;
    private int generationNeiStage;
    private String chineseGenerationDescription;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.heart.qa")) return;
        FMLCommonHandler.instance().bus().register(this);
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == 'S') hatchPositions.add(new int[] { col, row, depth });
        require(hatchPositions.size() >= 8, "Original structure exposes enough modular hatch slots");
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 18000, "Heart client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.clouds = false;
            mc.gameSettings.hideGUI = checked;
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 16;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("heart-qa-" + System.currentTimeMillis(), "Heart QA",
                    new WorldSettings(733L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (maintenanceOnly && checked) {
                finished = true;
                mc.shutdown();
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
            require(++serverTicks <= 12000, "Heart server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) {
                player.worldObj.setWorldTime(6000);
                player.worldObj.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                rig = buildRig(player, 0, 120, 0, ForgeDirection.NORTH);
                visualRigs.add(rig);
            }
            if (serverTicks == 60) {
                if (maintenanceOnly) {
                    verifyMaintenanceFree();
                    Files.write(new File(output(), "result.txt").toPath(),
                        ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                    checked = true;
                    return;
                }
                verifyControllerRecipe();
                verifyGenerationRecipe();
                verifyStructure();
                verifyGeneration();
                verifyCores();
                verifyCoreFairness();
                verifyParameterModules();
                verifyCorePause();
                verifyOutputBacklog();
                verifyDirections(player);
                for (Rig visual : visualRigs) {
                    rig = visual;
                    reset();
                    disableCores();
                    rig.machine.setAlwaysMaxParallel(false);
                    rig.machine.setPowerPanelMaxParallel(1);
                    rig.base.enableWorking();
                }
                rig = visualRigs.get(0);
                checked = true;
                requestedView = 0;
            }
            if (maintenanceOnly) return;
            if (!checked) return;
            if (rig.visualEnabled && !rig.machine.mInputBusses.isEmpty() && !rig.machine.mEnergyHatches.isEmpty()) {
                rig.base.enableWorking();
                fillEnergy(rig, true);
                seed(rig, 64);
                repair(rig.machine);
            }
            if (requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                rig = visualRigs.get(view >= 3 && view <= 4 ? 1 : 0);
                reloadRig();
                if (view == 2) occluder(true);
                else if (view == 3) {
                    Rig previous = rig;
                    rig = visualRigs.get(0);
                    occluder(false);
                    rig = previous;
                }
                if (view == 5) {
                    rig.visualEnabled = false;
                    rig.base.disableWorking();
                    rig.machine.stopMachine(ShutDownReasonRegistry.NONE);
                    rig.base.setActive(false);
                } else if (view == 6) {
                    rig.visualEnabled = true;
                    rig.base.enableWorking();
                }
                double[] center = center(rig);
                double dx = view == 0 ? -90 : view == 3 ? -75 : -55;
                double dy = view == 0 || view == 3 ? 65 : 12;
                double dz = view == 3 ? 85 : view == 4 ? 40 : view == 0 ? -105 : -75;
                move(player, center[0] + dx, center[1] + dy, center[2] + dz, center);
                shownView = view;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private Rig buildRig(EntityPlayerMP player, int x, int y, int z, ForgeDirection facing) {
        World world = player.worldObj;
        for (int cx = (x - 128) >> 4; cx <= (x + 128) >> 4; cx++)
            for (int cz = (z - 128) >> 4; cz <= (z + 128) >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
        BaseMetaTileEntity base = place(world, x, y, z, GTNGMachineID.HeartOfTheUniverse.id);
        base.setFrontFacing(facing);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        rig = new Rig(base, (HeartOfTheUniverse) base.getMetaTileEntity());
        rig.machine.construct(new ItemStack(Items.diamond), false);
        placeHatch(0, ItemList.Hatch_Input_Bus_UV.get(1));
        placeHatch(1, ItemList.Hatch_Output_Bus_UV.get(1));
        if (!maintenanceOnly) placeHatch(2, ItemList.Hatch_Maintenance.get(1));
        placeHatch(3, ItemList.Hatch_Energy_UHV.get(1));
        rig.machine.setAlwaysMaxParallel(false);
        rig.machine.setPowerPanelMaxParallel(1);
        rig.machine.setVoidingMode(VoidingMode.VOID_NONE);
        repair(rig.machine);
        WirelessNetworkManager.strongCheckOrAddUser(base.getOwnerUuid());
        return rig;
    }

    private void verifyStructure() {
        HeartOfTheUniverse machine = rig.machine;
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(rig.base, null, errors);
        require(errors.isEmpty(), "Original structure checks without errors: " + errors);
        require(machine.checkStructure(true, rig.base),
            "Physical 121x112x109 structure forms " + rig.base.getFrontFacing());
        require(
            machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1
                && machine.mMaintenanceHatches.size() == (maintenanceOnly ? 0 : 1)
                && !machine.mEnergyHatches.isEmpty(),
            "Real input/output, optional maintenance and local-energy hatches discovered");
        require(shape.length == 112 && shape[0].length == 109 && shape[0][0].length() == 121,
            "Source structure dimensions");
        Vec3Impl offset = machine.getBlackHoleOffset();
        require(offset.get1() == 36 && Math.abs(offset.get0()) + Math.abs(offset.get2()) == 39,
            "Black hole center follows the original offset and facing");
        require(machine.getMaxRenderDistanceSquared() >= 384 * 384, "Original 384-block render range");
        report.append("Physical structure forms facing ").append(rig.base.getFrontFacing()).append(".\n");
    }

    private void verifyMaintenanceFree() throws Exception {
        verifyStructure();
        placeHatch(2, ItemList.Hatch_Maintenance.get(1));
        rig.machine.clearHatches();
        require(rig.machine.checkStructure(true, rig.base) && rig.machine.mMaintenanceHatches.size() == 1,
            "An installed legacy maintenance hatch remains an optional structure replacement");
        int[] local = hatchPositions.get(2);
        int[] position = position(local[0], local[1], local[2]);
        rig.base.getWorld().setBlock(position[0], position[1], position[2], HeartOfTheUniverseStructure.block('S'),
            HeartOfTheUniverseStructure.metadata('S'), 3);
        verifyStructure();
        reset();
        require(!rig.machine.getDefaultHasMaintenanceChecks() && !rig.machine.shouldCheckMaintenance(),
            "Generator maintenance remains disabled independently of saved settings");
        rig.machine.mWrench = rig.machine.mScrewdriver = rig.machine.mSoftMallet = rig.machine.mHardHammer = false;
        rig.machine.mSolderingTool = rig.machine.mCrowbar = false;
        rig.machine.mEfficiency = 1000;
        NBTTagCompound legacy = new NBTTagCompound();
        rig.machine.saveNBTData(legacy);
        rig.machine.loadNBTData(legacy);
        require(rig.machine.getRepairStatus() == rig.machine.getIdealStatus(),
            "Broken legacy repair NBT cannot create generator maintenance faults");
        rig.machine.checkMaintenance();
        require(rig.machine.mWrench && rig.machine.mScrewdriver
            && rig.machine.mSoftMallet
            && rig.machine.mHardHammer
            && rig.machine.mSolderingTool
            && rig.machine.mCrowbar, "Generator normalizes native GUI repair flags");
        Method damage = null;
        for (Class<?> type = rig.machine.getClass(); type != null && damage == null; type = type.getSuperclass()) {
            try {
                damage = type.getDeclaredMethod("doRandomMaintenanceDamage");
            } catch (NoSuchMethodException ignored) {}
        }
        require(damage != null, "Native maintenance damage method exists");
        damage.setAccessible(true);
        for (int attempt = 0; attempt < 10000; attempt++)
            require((boolean) damage.invoke(rig.machine), "Random maintenance damage cannot block generation");
        seed(rig, 1);
        fillEnergy(rig, true);
        require(rig.machine.checkProcessing().wasSuccessful(),
            "Real fuel starts without repairs or a maintenance hatch");
        require(rig.machine.getGenerationWorks().size() == 1, "One real fuel task starts");
        GenerationWork work = rig.machine.getGenerationWorks().get(0);
        BigInteger expected = work.nextEnergy();
        long stored = storedEnergy();
        tick(false);
        require(work.progress() == 1 && rig.machine.mEfficiency == 10000,
            "Real fuel progresses at full efficiency without QA repair injection");
        require(
            rig.machine.getGeneratedLastTick().equals(expected) && stored - storedEnergy() == 2_097_152L
                && rig.machine.getMaintenanceEUt() == 2_097_152L,
            "Removing maintenance hatches preserves exact generation and the original 64A LuV upkeep");
        saveAndReload();
        work = rig.machine.getGenerationWorks().get(0);
        int progress = work.progress();
        BigInteger balance = WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid());
        fillEnergy(rig, false);
        tick(false);
        require(
            work.progress() == progress && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(balance),
            "Missing physical upkeep power still pauses generation");
        finishTasks();
        require(
            rig.machine.getGenerationWorks().isEmpty()
                && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "No-hatch generation survives NBT reload and credits the exact full fuel energy once");
        report.append("Heart: real no-maintenance-hatch structure, optional legacy hatch, broken repair NBT, ")
            .append("10000 damage checks, full-efficiency fuel generation and NBT reload passed. ")
            .append("Original 64A LuV physical upkeep and power-loss pause remain intact; no render tour rerun.\n");
    }

    private void verifyControllerRecipe() {
        List<GTRecipe.RecipeAssemblyLine> recipes = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, GTNGItemList.HeartOfTheUniverse.get(1)))
            .collect(Collectors.toList());
        require(recipes.size() == 1, "Exactly one native assembly-line controller acquisition recipe");
        GTRecipe.RecipeAssemblyLine recipe = recipes.get(0);
        require(
            recipe.mOutput.stackSize == 1 && recipe.mDuration == 2_304_000
                && recipe.mEUt == TierEU.RECIPE_MAX
                && recipe.mResearchTime == 2_304_000
                && recipe.mResearchVoltage == TierEU.RECIPE_MAX,
            "Controller output, duration, voltage and scanning gate remain unchanged");
        validateControllerInputs(recipe.mInputs, recipe.mFluidInputs);
        report.append("Controller acquisition: unique native 14-item/4-fluid assembly line, 16384000mB native UU.\n");
    }

    private static void validateControllerInputs(ItemStack[] items, FluidStack[] fluids) {
        require(items.length == 14 && fluids.length == 4, "Controller has fourteen item lanes and four fluid lanes");
        int[] amounts = { 128, 256, 256, 128, 1024, 1024, 1024, 1024, 64, 64, 64, 16, 16, 16 };
        ItemStack uuCell = Materials.UUMatter.getCells(1);
        for (int index = 0; index < items.length; index++) {
            ItemStack item = items[index];
            require(item != null && item.getItem() != null && item.stackSize == amounts[index],
                "Controller item lane quantity remains unchanged " + index);
            FluidStack contents = GTUtility.getFluidForFilledItem(item, true);
            require(
                !GTUtility.areStacksEqual(item, uuCell)
                    && (contents == null || !contents.isFluidEqual(Materials.UUMatter.getFluid(1))),
                "Controller item lanes contain no UU-filled cells");
        }
        FluidStack[] expected = { Materials.Eternity.getMolten(331_776_000),
            MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(754_974_720),
            Materials.TranscendentMetal.getMolten(460_800_000), Materials.UUMatter.getFluid(16_384_000) };
        for (int index = 0; index < fluids.length; index++) require(
            fluids[index] != null && fluids[index].isFluidEqual(expected[index])
                && fluids[index].amount == expected[index].amount,
            "Controller native fluid identity, order and exact amount " + index);
    }

    private void verifyGenerationRecipe() {
        List<GTRecipe> recipes = new ArrayList<>(GTNGRecipeMaps.HeartGenerationRecipes.getAllRecipes());
        require(recipes.size() == 1, "Exactly one native Heart generation recipe");
        validateGenerationRecipe(recipes.get(0));
        report.append("Generation recipe remains one rod + one seed, 64 byproducts at 60%, 1200 ticks and 0 EU/t.\n");
    }

    private static void validateGenerationRecipe(GTRecipe recipe) {
        require(recipe.mInputs.length == 2 && recipe.mOutputs.length == 1
            && recipe.mFluidInputs.length == 0
            && recipe.mFluidOutputs.length == 0, "Generation recipe retains its real item and fluid lanes");
        require(
            GTUtility.areStacksEqual(recipe.mInputs[0], GTNGItemList.StrangeAnnihilationFuelRod.get(1))
                && recipe.mInputs[0].stackSize == 1
                && GTUtility.areStacksEqual(recipe.mInputs[1], GTNGItemList.BlackHoleSeed.get(1))
                && recipe.mInputs[1].stackSize == 1,
            "Generation consumes exactly one annihilation rod and one black-hole seed");
        require(
            GTUtility.areStacksEqual(recipe.mOutputs[0], GTNGItemList.AnnihilationConstrainer.get(1))
                && recipe.mOutputs[0].stackSize == 64
                && recipe.getOutputChance(0) == 6000
                && recipe.mDuration == 1200
                && recipe.mEUt == 0,
            "Generation keeps the real 60% stack of 64 byproducts and 60-second base duration");
        require(recipe.getNeiDesc() == null, "Generation description is translated at display time");
    }

    private void verifyGeneration() throws Exception {
        reset();
        require(rig.machine.getMaintenanceEUt() == 2_097_152L, "Base demand is 64A LuV every operating tick");
        seed(rig, 1);
        fillEnergy(rig, true);
        require(rig.machine.checkProcessing().wasSuccessful(), "Real registered two-item fuel recipe starts");
        require(
            count(GTNGItemList.StrangeAnnihilationFuelRod.get(1)) == 0 && count(GTNGItemList.BlackHoleSeed.get(1)) == 0,
            "Exactly one rod and seed commit once");
        require(rig.machine.getGenerationWorks().size() == 1, "Controller owns one independent fuel task");
        GenerationWork work = rig.machine.getGenerationWorks().get(0);
        require(work.parallels == 1 && work.duration == 1200 && work.energy.equals(HeartGenerationRules.FUEL_ENERGY),
            "Base task preserves the source fuel energy over 1200 ticks");
        BigInteger expectedTick = work.nextEnergy();
        long before = storedEnergy();
        tick(false);
        require(rig.machine.getGeneratedLastTick().equals(expectedTick),
            "First tick credits its exact BigInteger share");
        require(before - storedEnergy() == rig.machine.getMaintenanceEUt(), "Local hatch pays exactly 64A LuV once");
        require(WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(expectedTick),
            "Generated energy goes directly to controller owner's wireless network");
        int progress = work.progress();
        BigInteger balance = WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid());
        fillEnergy(rig, false);
        tick(false);
        require(
            work.progress() == progress && rig.machine.getGeneratedLastTick().signum() == 0
                && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(balance),
            "Missing maintenance energy pauses both task progress and wireless output");
        tick(true);
        require(work.progress() == progress + 1, "Power restoration resumes the committed task");
        saveAndReload();
        require(
            rig.machine.getGenerationWorks().size() == 1
                && rig.machine.getGenerationWorks().get(0).progress() == progress + 1,
            "Real TileEntity reload preserves fuel progress");
        require(
            count(GTNGItemList.StrangeAnnihilationFuelRod.get(1)) == 0 && count(GTNGItemList.BlackHoleSeed.get(1)) == 0,
            "Reload does not request a second fuel pair");
        finishTasks();
        require(rig.machine.getGenerationWorks().isEmpty(), "Completed task flushes output and retires");
        require(WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "Pause/reload/resume credits exactly one fuel's energy without duplication");
        BigInteger completed = WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid());
        saveAndReload();
        tick(true);
        require(WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(completed),
            "Reloading a completed batch does not repeat wireless generation");
        report.append("Fuel consumption, 64A LuV upkeep, wireless output, pause/resume and TE reload passed.\n");
    }

    private void verifyCores() throws Exception {
        reset();
        placeHatch(4, GTNGItemList.ExecutionCore.get(1));
        placeHatch(5, GTNGItemList.AdvancedExecutionCore.get(1));
        placeHatch(6, GTNGItemList.PerfectExecutionCore.get(1));
        placeHatch(7, GTNGItemList.DynamicParallelControllerT1.get(1));
        int[] parallelSlot = hatchPositions.get(7);
        int[] parallelPosition = position(parallelSlot[0], parallelSlot[1], parallelSlot[2]);
        BaseMetaTileEntity parallelTile = (BaseMetaTileEntity) rig.base.getWorld().getTileEntity(parallelPosition[0],
            parallelPosition[1], parallelPosition[2]);
        ((ParameterModuleHatch) parallelTile.getMetaTileEntity()).setParameter(1);
        rig.machine.setAlwaysMaxParallel(true);
        verifyStructure();
        seed(rig, 6);
        tick(true);
        List<GenerationWork> works = rig.machine.getGenerationWorks();
        require(works.size() == 4, "Controller and three real execution cores each start a fuel task");
        require(
            count(GTNGItemList.StrangeAnnihilationFuelRod.get(1)) == 0 && count(GTNGItemList.BlackHoleSeed.get(1)) == 0,
            "Four cores commit exactly six fuel pairs");
        for (CoreType type : CoreType.values()) {
            GenerationWork task = works.stream().filter(value -> value.coreType == type).findFirst()
                .orElseThrow(() -> new AssertionError("Missing core type " + type));
            require(task.duration == (type == CoreType.Normal ? 1200 : type == CoreType.Advanced ? 300 : 1),
                "Generator-specific execution time " + type);
            require(task.parallels == (type == CoreType.Normal ? 1 : 2),
                "Normal cores share their quota; advanced and perfect cores each use the full quota");
            require(task.owner.equals(rig.base.getOwnerUuid()), "All generation belongs to the controller owner");
        }
        require(WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).signum() == 0,
            "Advanced and perfect generation cores do not prepay or refund fuel energy");
        finishTasks();
        require(
            rig.machine.getGenerationWorks().isEmpty() && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid())
                .equals(HeartGenerationRules.FUEL_ENERGY.multiply(BigInteger.valueOf(6))),
            "Different-duration cores finish independently and conserve all six fuel energies");
        report.append("Physical normal/advanced/perfect cores generate independently in 1200/300/1 ticks.\n");
    }

    private void verifyParameterModules() throws Exception {
        verifyParameter(GTNGItemList.DynamicSpeedControllerT1, 2, 600, 2_097_152L);
        verifyParameter(GTNGItemList.LowSpeedPerfectOverclockController, 1, 300, 2_097_152L);
        verifyParameter(GTNGItemList.PerfectOverclockController, 1, 150, 2_097_152L);
        verifyParameter(GTNGItemList.SingularityPerfectOverclockController, 1, 75, 2_097_152L);
        verifyParameter(GTNGItemList.PowerConsumptionControllerT8, 1, 1200, 524_288L);
        report.append("Speed/three overclock/upkeep modules preserve fuel energy at 600/300/150/75/1200 ticks.\n");
    }

    private void verifyCoreFairness() throws Exception {
        reset();
        restoreModules();
        require(hatchPositions.size() >= 13, "Original structure has room for nine execution cores");
        List<String> coreKeys = new ArrayList<>();
        for (int index = 4; index < 13; index++) {
            placeHatch(index, GTNGItemList.PerfectExecutionCore.get(1));
            BaseMetaTileEntity tile = moduleTile(index);
            coreKeys.add(
                CoreType.Perfect.name() + ":" + tile.getXCoord() + ":" + tile.getYCoord() + ":" + tile.getZCoord());
        }
        verifyStructure();
        saveAndReload();
        seed(rig, 16);
        tick(true);
        require(rig.machine.getGenerationWorks().size() == 8, "A scheduling round starts at most eight fuel tasks");
        List<String> waiting = new ArrayList<>();
        for (String key : coreKeys)
            if (rig.machine.getGenerationWorks().stream().noneMatch(work -> work.core.equals(key))) waiting.add(key);
        require(waiting.size() == 2, "First round leaves two of nine perfect cores waiting behind the controller");
        tick(true);
        for (String key : waiting)
            require(rig.machine.getGenerationWorks().stream().anyMatch(work -> work.core.equals(key)),
                "The next scheduling round reaches every previously waiting perfect core");
        require(
            count(GTNGItemList.StrangeAnnihilationFuelRod.get(1)) == 0 && count(GTNGItemList.BlackHoleSeed.get(1)) == 0,
            "Two scheduling rounds consume exactly sixteen fuel pairs");
        finishTasks();
        require(
            rig.machine.getGenerationWorks().isEmpty() && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid())
                .equals(HeartGenerationRules.FUEL_ENERGY.multiply(BigInteger.valueOf(16))),
            "Nine perfect cores preserve all sixteen fuel energies while rotating scheduling priority");
        report.append("Nine physical perfect cores rotate past the eight-task start limit without starvation.\n");
        restoreModules();
    }

    private void verifyParameter(GTNGItemList item, int parameter, int duration, long maintenance) throws Exception {
        reset();
        restoreModules();
        placeHatch(4, item.get(1));
        ((ParameterModuleHatch) moduleTile(4).getMetaTileEntity()).setParameter(parameter);
        verifyStructure();
        rig.machine.setAlwaysMaxParallel(false);
        rig.machine.setPowerPanelMaxParallel(1);
        seed(rig, 1);
        fillEnergy(rig, true);
        require(rig.machine.checkProcessing().wasSuccessful(), "Parameter module admits real fuel " + item);
        GenerationWork work = rig.machine.getGenerationWorks().get(0);
        require(work.duration == duration && work.parallels == 1, "Generator-specific duration " + item);
        require(rig.machine.getMaintenanceEUt() == maintenance, "Generator-specific maintenance " + item);
        long before = storedEnergy();
        tick(false);
        require(before - storedEnergy() == maintenance, "Physical hatch pays adapted upkeep " + item);
        finishTasks();
        require(
            rig.machine.getGenerationWorks().isEmpty()
                && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "Parameter module preserves exact total fuel energy " + item);
    }

    private void verifyCorePause() throws Exception {
        reset();
        restoreModules();
        placeHatch(4, GTNGItemList.AdvancedExecutionCore.get(1));
        verifyStructure();
        seed(rig, 2);
        tick(true);
        GenerationWork advanced = advancedWork();
        int progress = advanced.progress();
        BigInteger remaining = advanced.remainingEnergy();
        moduleTile(4).disableWorking();
        tick(true);
        require(advanced.progress() == progress && advanced.remainingEnergy().equals(remaining),
            "Disabling a core freezes its committed task and remaining energy");
        moduleTile(4).enableWorking();
        tick(true);
        require(advanced.progress() == progress + 1, "Re-enabling a core resumes its bound task");
        progress = advanced.progress();
        remaining = advanced.remainingEnergy();
        restoreModule(4);
        verifyStructure();
        tick(true);
        require(advanced.progress() == progress && advanced.remainingEnergy().equals(remaining),
            "Removing a core leaves its unfinished task paused");
        saveAndReload();
        advanced = advancedWork();
        require(advanced.progress() == progress && advanced.remainingEnergy().equals(remaining),
            "Removed-core task survives a real controller reload without payout");
        placeHatch(4, GTNGItemList.AdvancedExecutionCore.get(1));
        verifyStructure();
        tick(true);
        require(advancedWork().progress() == progress + 1, "Matching core at the original position resumes its task");
        finishTasks();
        require(
            rig.machine.getGenerationWorks().isEmpty() && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid())
                .equals(HeartGenerationRules.FUEL_ENERGY.multiply(BigInteger.valueOf(2))),
            "Core removal/reload/rebind credits exactly two committed fuel energies");
        report.append("Disabled and removed cores pause their bound tasks; matching reinstallation resumes exactly.\n");
    }

    private GenerationWork advancedWork() {
        return rig.machine.getGenerationWorks().stream().filter(work -> work.coreType == CoreType.Advanced).findFirst()
            .orElseThrow(() -> new AssertionError("Missing committed advanced-core work"));
    }

    private void verifyOutputBacklog() throws Exception {
        reset();
        restoreModules();
        ItemStack byproduct = GTNGItemList.AnnihilationConstrainer.get(1);
        GenerationWork deterministic = new GenerationWork("controller", CoreType.Normal, rig.base.getOwnerUuid(),
            HeartGenerationRules.duration(CoreType.Normal, 1, 1), 1, HeartGenerationRules.FUEL_ENERGY,
            new WirelessOutputs(new ItemStack[] { byproduct }, null));
        NBTTagCompound saved = new NBTTagCompound();
        rig.machine.saveNBTData(saved);
        NBTTagList tasks = new NBTTagList();
        tasks.appendTag(deterministic.save());
        saved.setTag("gtngGenerationWork", tasks);
        rig.machine.loadNBTData(saved);
        for (var bus : rig.machine.mOutputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Blocks.stone, 64));
        for (int tick = 0; tick < deterministic.duration; tick++) tick(true);
        require(rig.machine.getGenerationWorks().size() == 1 && rig.machine.getGenerationWorks().get(0).finished(),
            "Full output bus retains a completed task with a deterministic byproduct");
        require(WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "Completed blocked task has already credited its exact fuel energy");
        fillEnergy(rig, false);
        for (int i = 0; i < 5; i++) tick(false);
        require(
            rig.machine.getGeneratedLastTick().signum() == 0
                && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "Waiting for output space does not generate additional energy");
        saveAndReload();
        tick(false);
        require(
            rig.machine.getGenerationWorks().size() == 1 && rig.machine.getGenerationWorks().get(0).finished()
                && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "Reloading a completed blocked task does not credit it again");
        rig.machine.mOutputBusses.get(0).setInventorySlotContents(0, null);
        tick(false);
        int count = 0;
        for (var bus : rig.machine.mOutputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack item = bus.getStackInSlot(slot);
            if (item != null && GTUtility.areStacksEqual(item, byproduct)) count += item.stackSize;
        }
        require(
            count == 1 && rig.machine.getGenerationWorks().isEmpty()
                && WirelessNetworkManager.getUserEU(rig.base.getOwnerUuid()).equals(HeartGenerationRules.FUEL_ENERGY),
            "Freeing output space flushes the byproduct exactly once without replaying generation");
        report.append("Blocked output survives reload and flushes once without repeating generation.\n");
    }

    private BaseMetaTileEntity moduleTile(int index) {
        int[] local = hatchPositions.get(index);
        int[] position = position(local[0], local[1], local[2]);
        return (BaseMetaTileEntity) rig.base.getWorld().getTileEntity(position[0], position[1], position[2]);
    }

    private void restoreModules() {
        for (int index = 4; index < hatchPositions.size(); index++) restoreModule(index);
        verifyStructure();
        rig.machine.setAlwaysMaxParallel(false);
        rig.machine.setPowerPanelMaxParallel(1);
    }

    private void restoreModule(int index) {
        int[] local = hatchPositions.get(index);
        int[] position = position(local[0], local[1], local[2]);
        rig.base.getWorld().setBlock(position[0], position[1], position[2], HeartOfTheUniverseStructure.block('S'),
            HeartOfTheUniverseStructure.metadata('S'), 3);
    }

    private void verifyDirections(EntityPlayerMP player) {
        Rig original = rig;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.SOUTH, ForgeDirection.WEST,
            ForgeDirection.EAST }) {
            buildRig(player, 384, 120, 384, facing);
            verifyStructure();
            if (facing != ForgeDirection.EAST) clearStructure();
        }
        visualRigs.add(rig);
        rig = original;
    }

    private void clearStructure() {
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++) if (shape[row][depth].charAt(col) != ' ') {
                int[] position = position(col, row, depth);
                rig.base.getWorld().setBlockToAir(position[0], position[1], position[2]);
            }
    }

    private void saveAndReload() {
        NBTTagCompound saved = new NBTTagCompound();
        rig.base.writeToNBT(saved);
        World world = rig.base.getWorld();
        int x = rig.base.getXCoord(), y = rig.base.getYCoord(), z = rig.base.getZCoord();
        world.removeTileEntity(x, y, z);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(world);
        restored.readFromNBT(saved);
        world.setTileEntity(x, y, z, restored);
        rig.base = restored;
        rig.machine = (HeartOfTheUniverse) restored.getMetaTileEntity();
        verifyStructure();
    }

    private void reloadRig() {
        World world = rig.base.getWorld();
        int x = rig.base.getXCoord(), y = rig.base.getYCoord(), z = rig.base.getZCoord();
        for (int cx = (x - 128) >> 4; cx <= (x + 128) >> 4; cx++)
            for (int cz = (z - 128) >> 4; cz <= (z + 128) >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        require(tile != null && tile.getMetaTileEntity() instanceof HeartOfTheUniverse,
            "Visual controller reloads with its structure chunks");
        rig.base = tile;
        rig.machine = (HeartOfTheUniverse) tile.getMetaTileEntity();
        verifyStructure();
    }

    private void reset() {
        rig.base.disableWorking();
        NBTTagCompound saved = new NBTTagCompound();
        rig.machine.saveNBTData(saved);
        saved.setTag("gtngGenerationWork", new NBTTagList());
        rig.machine.loadNBTData(saved);
        seed(rig, 0);
        for (var bus : rig.machine.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        WirelessNetworkManager.setUserEU(rig.base.getOwnerUuid(), BigInteger.ZERO);
        repair(rig.machine);
    }

    private void tick(boolean refill) throws Exception {
        rig.base.enableWorking();
        if (refill) fillEnergy(rig, true);
        Method run = GTNGModularGeneratorBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class,
            long.class);
        run.setAccessible(true);
        run.invoke(rig.machine, rig.base, controlledTick += 5);
    }

    private void finishTasks() throws Exception {
        int remaining = 0;
        for (GenerationWork work : rig.machine.getGenerationWorks())
            remaining = Math.max(remaining, work.duration - work.progress());
        for (int tick = 0; tick <= remaining && !rig.machine.getGenerationWorks().isEmpty(); tick++) tick(true);
    }

    private long storedEnergy() {
        long result = 0;
        for (var hatch : rig.machine.mEnergyHatches) result += hatch.getBaseMetaTileEntity().getStoredEU();
        return result;
    }

    private static void fillEnergy(Rig rig, boolean filled) {
        for (var hatch : rig.machine.mEnergyHatches) {
            BaseMetaTileEntity tile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
            tile.setStoredEU(filled ? tile.getEUCapacity() : 0);
        }
    }

    private static void seed(Rig rig, int amount) {
        var bus = rig.machine.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        if (amount > 0) {
            bus.setInventorySlotContents(0, GTNGItemList.StrangeAnnihilationFuelRod.get(amount));
            bus.setInventorySlotContents(1, GTNGItemList.BlackHoleSeed.get(amount));
        }
    }

    private int count(ItemStack expected) {
        int count = 0;
        for (var bus : rig.machine.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack actual = bus.getStackInSlot(slot);
            if (actual != null && GTUtility.areStacksEqual(actual, expected)) count += actual.stackSize;
        }
        return count;
    }

    private void placeHatch(int index, ItemStack stack) {
        int[] local = hatchPositions.get(index);
        int[] position = position(local[0], local[1], local[2]);
        BaseMetaTileEntity tile = place(rig.base.getWorld(), position[0], position[1], position[2],
            stack.getItemDamage());
        tile.setOwnerUuid(UUID.randomUUID());
        if (tile.getMetaTileEntity() instanceof ExecutionCoreHatch) tile.enableWorking();
    }

    private void disableCores() {
        for (int index = 4; index < hatchPositions.size(); index++) {
            int[] local = hatchPositions.get(index);
            int[] position = position(local[0], local[1], local[2]);
            TileEntity tile = rig.base.getWorld().getTileEntity(position[0], position[1], position[2]);
            if (tile instanceof BaseMetaTileEntity core) core.disableWorking();
        }
    }

    private int[] position(int col, int row, int depth) {
        int[] offset = new int[3];
        rig.machine.getExtendedFacing().getWorldOffset(new int[] { col - HeartOfTheUniverseStructure.OFFSET_X,
            row - HeartOfTheUniverseStructure.OFFSET_Y, depth - HeartOfTheUniverseStructure.OFFSET_Z }, offset);
        return new int[] { rig.base.getXCoord() + offset[0], rig.base.getYCoord() + offset[1],
            rig.base.getZCoord() + offset[2] };
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private static void repair(HeartOfTheUniverse machine) {
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
    }

    private static double[] center(Rig rig) {
        Vec3Impl offset = rig.machine.getBlackHoleOffset();
        return new double[] { rig.base.getXCoord() + offset.get0() + 0.5, rig.base.getYCoord() + offset.get1() + 0.5,
            rig.base.getZCoord() + offset.get2() + 0.5 };
    }

    private void occluder(boolean place) {
        double[] center = center(rig);
        for (int x = -43; x <= -29; x++)
            for (int y = -3; y <= 17; y++) rig.base.getWorld().setBlock((int) center[0] + x, (int) center[1] + y,
                (int) center[2] - 49, place ? Blocks.stonebrick : Blocks.air, 0, 3);
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
        if (event.phase != TickEvent.Phase.END || finished || !checked || maintenanceOnly) return;
        try {
            if (generationNeiHandler != null) {
                captureGenerationNei();
                return;
            }
            if (controllerNeiHandler != null) {
                captureControllerNei();
                return;
            }
            if (shownView != capturedView + 1 || requestedView >= 0) return;
            Minecraft mc = Minecraft.getMinecraft();
            Rig current = visualRigs.get(shownView >= 3 && shownView <= 4 ? 1 : 0);
            TileEntity tile = mc.theWorld.getTileEntity(current.base.getXCoord(), current.base.getYCoord(),
                current.base.getZCoord());
            if (
                !(tile instanceof BaseMetaTileEntity client)
                    || !(client.getMetaTileEntity() instanceof HeartOfTheUniverse machine)
            ) return;
            boolean expectedWorking = shownView != 5;
            if (machine.isHeartWorking() != expectedWorking) {
                frames = 0;
                return;
            }
            if (++frames == 1) firstRenderedFrame = HeartBlackHoleRenderer.getRenderedFrames();
            if (frames < 180) return;
            if (expectedWorking) {
                require(HeartBlackHoleRenderer.isAvailable(),
                    "Full black-hole volume, blur and composite shaders compile");
                require(HeartBlackHoleRenderer.getLastPassCount() == 7,
                    "Both volume targets, four blurs and composite draw");
                require(HeartBlackHoleRenderer.getRenderedFrames() > firstRenderedFrame,
                    "Actual client submits working black hole every frame");
            }
            String[] names = { "north-overview", "north-black-hole", "north-depth-occlusion", "east-overview",
                "east-black-hole", "north-stopped", "north-restarted" };
            capture(names[shownView]);
            report.append("Native client view ").append(names[shownView]).append(" captured.\n");
            capturedView = shownView;
            frames = 0;
            if (capturedView + 1 < names.length) requestedView = capturedView + 1;
            else openControllerNei();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void openControllerNei() throws Exception {
        GTMod.proxy.reloadNEICache();
        controllerNeiHandler = new GTNEIDefaultHandler(RecipeMaps.assemblylineVisualRecipes.getDefaultRecipeCategory());
        controllerNeiHandler.loadCraftingRecipes(GTNGItemList.HeartOfTheUniverse.get(1));
        require(controllerNeiHandler.arecipes.size() == 1, "Native NEI handler finds exactly one controller recipe");
        GTNEIDefaultHandler.CachedDefaultRecipe cached = (GTNEIDefaultHandler.CachedDefaultRecipe) controllerNeiHandler.arecipes
            .get(0);
        validateControllerInputs(cached.mRecipe.mInputs, cached.mRecipe.mFluidInputs);
        ArrayList<ICraftingHandler> handlers = new ArrayList<>();
        handlers.add(controllerNeiHandler);
        var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
        constructor.setAccessible(true);
        Minecraft.getMinecraft().displayGuiScreen(constructor.newInstance(handlers));
        frames = 0;
    }

    private void captureControllerNei() throws Exception {
        require(++frames <= 1200, "Controller acquisition NEI screenshot timeout");
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiRecipe gui) || frames < 90) return;
        GTNEIDefaultHandler.CachedDefaultRecipe cached = (GTNEIDefaultHandler.CachedDefaultRecipe) controllerNeiHandler.arecipes
            .get(0);
        List<PositionedStack> fluids = new ArrayList<>();
        for (PositionedStack ingredient : cached.getIngredients())
            if (StackInfo.isFluidDisplayItem(ingredient.item)) fluids.add(ingredient);
        require(fluids.size() == 4, "Native controller NEI renders all four fluid inputs");
        PositionedStack fourth = fluids.get(3);
        FluidStack uu = StackInfo.getFluid(fourth.item);
        require(uu != null && uu.isFluidEqual(Materials.UUMatter.getFluid(1)) && uu.amount == 16_384_000,
            "Native UU is displayed in the fourth fluid input slot with its exact amount");
        var position = gui.getRecipePosition(0);
        require(
            position.x + fourth.relx >= 0 && position.y + fourth.rely >= 0
                && position.x + fourth.relx + 16 <= gui.width
                && position.y + fourth.rely + 16 <= gui.height,
            "Fourth native UU fluid slot is inside the visible NEI screen");
        capture("controller-acquisition-nei");
        report.append("Native controller NEI screenshot: fourth fluid slot shows 16384000mB UU, no UU item lane.\n")
            .append(
                "Acquisition QA covers registration and native NEI display; no full controller craft is executed.\n");
        controllerNeiHandler = null;
        openGenerationNei();
    }

    private void openGenerationNei() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        String locale = generationNeiStage % 2 == 0 ? "zh_CN" : "en_US";
        boolean found = false;
        for (Language language : mc.getLanguageManager().getLanguages())
            if (language.getLanguageCode().equals(locale)) {
                mc.getLanguageManager().setCurrentLanguage(language);
                mc.gameSettings.language = locale;
                found = true;
                break;
            }
        require(found, "Native client language exists " + locale);
        mc.gameSettings.guiScale = generationNeiStage < 2 ? 2 : 3;
        mc.refreshResources();
        GTMod.proxy.reloadNEICache();
        generationNeiHandler = new GTNEIDefaultHandler(
            GTNGRecipeMaps.HeartGenerationRecipes.getDefaultRecipeCategory());
        generationNeiHandler.loadCraftingRecipes(GTNGRecipeMaps.HeartGenerationRecipes.unlocalizedName);
        require(generationNeiHandler.arecipes.size() == 1, "Native generation NEI opens the registered fuel recipe");
        GTNEIDefaultHandler.CachedDefaultRecipe cached = (GTNEIDefaultHandler.CachedDefaultRecipe) generationNeiHandler.arecipes
            .get(0);
        validateGenerationRecipe(cached.mRecipe);
        ArrayList<ICraftingHandler> handlers = new ArrayList<>();
        handlers.add(generationNeiHandler);
        var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
        constructor.setAccessible(true);
        mc.displayGuiScreen(constructor.newInstance(handlers));
        frames = 0;
    }

    private void captureGenerationNei() throws Exception {
        require(++frames <= 1200, "Generation NEI screenshot timeout");
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiRecipe) || frames < 90) return;
        require(GTNGRecipeMaps.HeartGenerationRecipes.getFrontend() instanceof HeartGenerationFrontend,
            "Generation NEI uses its dedicated release-time frontend");
        HeartGenerationFrontend frontend = (HeartGenerationFrontend) GTNGRecipeMaps.HeartGenerationRecipes
            .getFrontend();
        List<String> lines = frontend.getGenerationDescriptionLines();
        int width = frontend.getDescriptionWidth();
        require(width == 160 && !lines.isEmpty(), "Generation description has its bounded 160-pixel content panel");
        for (String line : lines) require(mc.fontRenderer.getStringWidth(line) <= width,
            "Generation description fits the real font-rendered content width: " + line);
        require(77 + (lines.size() - 1) * 10 + mc.fontRenderer.FONT_HEIGHT <= 202,
            "Generation description fits below y77 inside its 202-pixel recipe panel");
        String description = EnumChatFormatting.getTextWithoutFormattingCodes(String.join(" ", lines));
        String numbers = description.replace(",", "").replace(" ", "").toLowerCase(Locale.ROOT);
        require(numbers.contains("7.56e23") && (description.contains("~") || description.contains("约")),
            "Generation NEI labels its scientific-notation total energy as approximate");
        require(
            numbers.matches(".*(?:^|[^0-9])60(?:[^0-9]|$).*") && numbers.matches(".*(?:^|[^0-9])15(?:[^0-9]|$).*")
                && numbers.contains("1t")
                && (numbers.contains("s") || numbers.contains("秒")),
            "Generation NEI states normal 60s, advanced 15s and perfect 1-tick release times");
        require(numbers.contains("64") && numbers.contains("luv") && numbers.contains("2097152"),
            "Generation NEI keeps both 64A LuV and the exact 2097152 EU/t upkeep");
        require(!description.contains("gtng."), "Generation NEI contains translated text instead of language keys");
        if (generationNeiStage == 0) chineseGenerationDescription = description;
        if (generationNeiStage % 2 == 1)
            require(!description.equals(chineseGenerationDescription), "Generation description updates with language");
        int actualScale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        if (
            mc.gameSettings.guiScale == 3 && !mc.fontRenderer.getUnicodeFlag()
                && mc.displayWidth >= 960
                && mc.displayHeight >= 720
        ) require(actualScale == 3, "GUI scale three is exercised when supported by the native client");
        capture("generation-nei-" + mc.gameSettings.language + "-gui" + mc.gameSettings.guiScale);
        report.append("Native generation NEI ").append(mc.gameSettings.language).append(" GUI ")
            .append(mc.gameSettings.guiScale).append(" (effective ").append(actualScale)
            .append("): scientific energy, 60s/15s/1t and upkeep fit the 160x202 recipe content panel.\n");
        if (++generationNeiStage < 4) openGenerationNei();
        else finish();
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
        System.out.println("HEART_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File directory = new File(System.getProperty("gtng.heart.output"));
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

    private static final class Rig {

        private BaseMetaTileEntity base;
        private HeartOfTheUniverse machine;
        private boolean visualEnabled = true;

        private Rig(BaseMetaTileEntity base, HeartOfTheUniverse machine) {
            this.base = base;
            this.machine = machine;
        }
    }
}
