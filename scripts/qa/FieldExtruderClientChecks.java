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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.FieldExtruderFactory;
import com.xyp.gtnotgood.common.machines.multiblock.structure.FieldExtruderStructure;
import com.xyp.gtnotgood.common.machines.processing.ModuleWork;
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
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

@Mod(
    modid = "fieldextruderqa",
    name = "Field Extruder QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class FieldExtruderClientChecks {

    private Item fixture;
    private BaseMetaTileEntity base;
    private volatile FieldExtruderFactory machine;
    private final String[][] shape = FieldExtruderStructure.shape();
    private final StringBuilder report = new StringBuilder();
    private final int[] nativeCounts = new int[2];
    private boolean started;
    private volatile boolean checked, finished;
    private volatile int requestedMode = -1, openedMode = -1, requestedView = -1, shownView = -1;
    private int clientTicks, serverTicks, frames, stageFrames, capturedView = -1, capturedMode = -1;
    private int tooltipStage = -1;
    private MultiblockTooltipBuilder currentTooltip;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.fieldExtruder.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("fieldExtruderFixture");
        GameRegistry.registerItem(fixture, "fixture");
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
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("field-extruder-qa-" + System.currentTimeMillis(), "Field Extruder QA",
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
                    GTNGItemList.FieldExtruderFactory.get(1).getItemDamage() == GTNGMachineID.FieldExtruderFactory.id,
                    "Registered controller identity");
                player.worldObj.setWorldTime(6000);
                buildRig(player, 0, 12, 0, ForgeDirection.NORTH);
            }
            if (serverTicks == 60) {
                verifyStructure();
                verifyDirections(player);
                verifyRequiredBlocks();
                verifyUnconstrainedSpace();
                verifyMachineOptions();
                verifySurvivalRecipes();
                verifyNativeExtrusion();
                registerFixtures();
                verifySingleRecipeTasks();
                verifyVoltageGate();
                verifyOutputProtection();
                verifyPersistence();
                resetProcessing();
                machine.setMachineMode(0);
                machine.setPowerPanelMaxParallel(8);
                setEnergy(ItemList.Hatch_Energy_EV);
                verifyStructure();
                cleanupFixtures();
                checked = true;
                requestedView = 0;
            }
            if (checked && requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                player.closeScreen();
                if (view == 0) player.playerNetServerHandler.setPlayerLocation(-45, 38, -24, -40, 20);
                else player.playerNetServerHandler.setPlayerLocation(-14, 16, 7, -96, 25);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                shownView = view;
            }
            if (requestedMode >= 0) {
                int mode = requestedMode;
                requestedMode = -1;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0, 13, -4, 0, 0);
                machine.setMachineMode(mode);
                machine.onRightclick(base, player);
                openedMode = mode;
            }
            if (checked && openedMode >= 0) openedMode = machine.getMachineMode();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void buildRig(EntityPlayerMP player, int x, int y, int z, ForgeDirection facing) {
        World world = player.worldObj;
        for (int cx = (x - 70) >> 4; cx <= (x + 70) >> 4; cx++)
            for (int cz = (z - 70) >> 4; cz <= (z + 70) >> 4; cz++) world.getChunkFromChunkCoords(cx, cz);
        base = place(world, x, y, z, GTNGMachineID.FieldExtruderFactory.id);
        base.setFrontFacing(facing);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (FieldExtruderFactory) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        verifyConstructedGeometry();
        placeAt(6, 0, 0, ItemList.Hatch_Input_Bus_EV);
        placeAt(7, 0, 0, ItemList.Hatch_Output_Bus_EV);
        placeAt(8, 0, 0, ItemList.Hatch_Maintenance);
        setEnergy(ItemList.Hatch_Energy_EV);
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(8);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
    }

    private void verifyConstructedGeometry() {
        Map<Character, ItemStack> blocks = new HashMap<>();
        int total = 0;
        for (int row = 0; row < FieldExtruderStructure.HEIGHT; row++) {
            for (int depth = 0; depth < FieldExtruderStructure.LENGTH; depth++) {
                for (int col = 0; col < FieldExtruderStructure.WIDTH; col++) {
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
        }
        require(total == 2598 && blocks.size() == 7, "All 2598 upstream structure blocks are built");
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Structure: " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Physical controller forms the complete 15x9x63 structure");
        require(
            machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1
                && machine.mEnergyHatches.size() == 1
                && machine.mMaintenanceHatches.size() == 1,
            "Physical input/output/energy/maintenance hatches are discovered");
    }

    private void verifyDirections(EntityPlayerMP player) {
        BaseMetaTileEntity mainBase = base;
        FieldExtruderFactory mainMachine = machine;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST }) {
            buildRig(player, 192, 12, 192, facing);
            verifyStructure();
            for (int row = 0; row < FieldExtruderStructure.HEIGHT; row++) {
                for (int depth = 0; depth < FieldExtruderStructure.LENGTH; depth++) {
                    for (int col = 0; col < FieldExtruderStructure.WIDTH; col++) {
                        int[] pos = position(col, row, depth);
                        base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
                    }
                }
            }
        }
        base = mainBase;
        machine = mainMachine;
        verifyStructure();
        report.append("Original 15x9x63 geometry builds and forms in all four horizontal directions.\n");
    }

    private void verifyRequiredBlocks() {
        for (char symbol : new char[] { 'B', 'C', 'D', 'E', 'F', 'G' }) {
            int[] pos = firstPosition(symbol);
            World world = base.getWorld();
            Block block = world.getBlock(pos[0], pos[1], pos[2]);
            int meta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
            world.setBlockToAir(pos[0], pos[1], pos[2]);
            machine.clearHatches();
            List<StructureError> errors = new ArrayList<>();
            machine.checkMachine(base, null, errors);
            require(!errors.isEmpty() && !machine.checkStructure(true, base),
                "Removing required block " + symbol + " invalidates the structure");
            world.setBlock(pos[0], pos[1], pos[2], block, meta, 3);
            verifyStructure();
        }
        report.append("All six fixed block types reject missing blocks and recover after repair.\n");
    }

    private void verifyUnconstrainedSpace() {
        require(shape[4][1].charAt(5) == ' ', "Selected upstream any position");
        int[] pos = position(5, 4, 1);
        World world = base.getWorld();
        world.setBlock(pos[0], pos[1], pos[2], Blocks.stone, 0, 3);
        verifyStructure();
        world.setBlockToAir(pos[0], pos[1], pos[2]);
        verifyStructure();
        report.append("Upstream unconstrained spaces accept both a solid block and air.\n");
    }

    private void verifyMachineOptions() {
        require(machine.getMaxParallelRecipes() == 1, "An unmodified controller has one base parallel");
        require(!machine.supportsBatchMode() && !machine.supportsSingleRecipeLocking(),
            "Extra native batch and single-recipe locking are disabled");
        require(machine.getRecipeMap() == map(0), "Default recipe map is extrusion");
    }

    private void verifyNativeExtrusion() {
        machine.setMachineMode(0);
        ItemStack iron = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1);
        ItemStack mold = ItemList.Shape_Extruder_Rod.get(1);
        GTRecipe nativeRecipe = map(0).getAllRecipes().stream()
            .filter(recipe -> recipe.mEUt <= 128 && recipe.mInputs.length == 2
                && recipe.mOutputs.length > 0
                && (recipe.mFluidInputs == null || recipe.mFluidInputs.length == 0)
                && Arrays.stream(recipe.mInputs)
                    .anyMatch(input -> input.stackSize > 0 && GTUtility.areStacksEqual(input, iron))
                && Arrays.stream(recipe.mInputs)
                    .anyMatch(input -> input.stackSize == 0 && GTUtility.areStacksEqual(input, mold)))
            .findFirst().orElseThrow(() -> new AssertionError("Native iron rod extrusion recipe"));
        ItemStack material = Arrays.stream(nativeRecipe.mInputs).filter(input -> input.stackSize > 0).findFirst()
            .orElseThrow(() -> new AssertionError("Native extrusion consumable input")).copy();
        int consumed = material.stackSize;
        material.stackSize *= 3;
        resetProcessing();
        setEnergy(nativeRecipe.mEUt <= 32 ? ItemList.Hatch_Energy_LV : ItemList.Hatch_Energy_MV);
        verifyStructure();
        seed(material, mold.copy());
        require(machine.checkProcessing().wasSuccessful(), "Native iron extrusion processes on the real structure");
        require(
            machine.getModuleWorks().size() == 1 && machine.getModuleWorks().get(0).parallels == 1
                && inputCount(material) == consumed * 2
                && inputCount(mold) == 1,
            "Exactly one iron recipe is consumed while the native non-consumed mold remains");
        for (ItemStack expected : nativeRecipe.mOutputs)
            require(outputCount(expected) == expected.stackSize, "Native rod task keeps exact single-recipe output");
        report.append("A native iron rod task consumes one recipe and preserves its GT-defined non-consumed mold.\n");
    }

    private void verifySurvivalRecipes() {
        GTNGItemList[] lineOutputs = { GTNGItemList.FieldExtruderFactory, GTNGItemList.IridiumCasing,
            GTNGItemList.HollowCasing, GTNGItemList.ContainmentFieldGenerator };
        int[] lineDurations = { 800, 200, 200, 500 };
        int[] lineAmounts = { 1, 2, 2, 1 };
        int[] lineInputs = { 8, 8, 7, 7 };
        int[] lineFluids = { 4, 2, 4, 4 };
        long[] lineTiers = { TierEU.RECIPE_UEV, TierEU.RECIPE_LuV, TierEU.RECIPE_UV, TierEU.RECIPE_UV };
        GTRecipe.RecipeAssemblyLine controller = null;
        for (int index = 0; index < lineOutputs.length; index++) {
            ItemStack expected = lineOutputs[index].get(1);
            List<GTRecipe.RecipeAssemblyLine> recipes = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, expected)).collect(Collectors.toList());
            require(recipes.size() == 1,
                "Exactly one native assembly-line recipe for " + lineOutputs[index] + "; runtime materials must exist");
            GTRecipe.RecipeAssemblyLine recipe = recipes.get(0);
            require(
                recipe.mDuration == lineDurations[index] && recipe.mEUt == lineTiers[index]
                    && recipe.mOutput.stackSize == lineAmounts[index],
                "Assembly-line duration, power and output quantity " + lineOutputs[index]);
            require(recipe.mInputs.length == lineInputs[index] && recipe.mFluidInputs.length == lineFluids[index],
                "No missing ingredient silently removed from " + lineOutputs[index]);
            validateIngredients(recipe.mInputs, recipe.mFluidInputs, lineOutputs[index].name());
            require(
                recipe.mResearchItem != null && recipe.mResearchItem.getItem() != null
                    && recipe.mResearchTime == 1200
                    && recipe.mResearchVoltage == lineTiers[index]
                    && recipe.getPersistentHash() != 0,
                "Native research and persistent recipe hash " + lineOutputs[index]);
            if (index == 0) controller = recipe;
        }
        require(controller != null, "Controller assembly-line recipe exists");
        ItemStack uiv = GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UIV, 4);
        require(uiv != null, "Native UIV circuit ore entry exists");
        require(GTRecipe.RecipeAssemblyLine.getMatchedIngredientAmount(uiv, controller.mInputs[4],
            controller.mOreDictAlt[4]) == 4, "Controller requires four actual UIV circuits in its circuit lane");
        require(
            controller.mInputs[0].stackSize == 4
                && GTUtility.areStacksEqual(controller.mInputs[0], ItemList.ExtruderUHV.get(1))
                && controller.mInputs[1].stackSize == 4
                && GTUtility.areStacksEqual(controller.mInputs[1], ItemList.CompressorUHV.get(1))
                && controller.mInputs[2].stackSize == 8
                && GTUtility.areStacksEqual(controller.mInputs[2], ItemList.Field_Generator_UEV.get(1))
                && controller.mInputs[3].stackSize == 4
                && GTUtility.areStacksEqual(controller.mInputs[3], ItemList.Emitter_UEV.get(1)),
            "Original UHV machine and UEV component prerequisites remain in the controller recipe");

        GTNGItemList[] assemblerOutputs = { GTNGItemList.HyperMechanicalCasing, GTNGItemList.MolecularCasing,
            GTNGItemList.CleanroomGlass };
        int[] assemblerDurations = { 50, 400, 100 };
        int[] assemblerAmounts = { 2, 1, 4 };
        int[] assemblerInputs = { 3, 6, 4 };
        int[] assemblerFluids = { 0, 1, 0 };
        long[] assemblerTiers = { 16, TierEU.RECIPE_UV, TierEU.RECIPE_EV };
        for (int index = 0; index < assemblerOutputs.length; index++) {
            ItemStack expected = assemblerOutputs[index].get(1);
            List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream().filter(
                recipe -> Arrays.stream(recipe.mOutputs).anyMatch(output -> GTUtility.areStacksEqual(output, expected)))
                .collect(Collectors.toList());
            require(recipes.size() == 1, "Exactly one native assembler recipe for " + assemblerOutputs[index]
                + "; runtime materials must exist");
            GTRecipe recipe = recipes.get(0);
            require(
                recipe.mDuration == assemblerDurations[index] && recipe.mEUt == assemblerTiers[index]
                    && recipe.mOutputs.length == 1
                    && recipe.mOutputs[0].stackSize == assemblerAmounts[index],
                "Assembler duration, power and output quantity " + assemblerOutputs[index]);
            require(
                recipe.mInputs.length == assemblerInputs[index] && recipe.mFluidInputs.length == assemblerFluids[index],
                "Native assembler input counts remain complete for " + assemblerOutputs[index]);
            validateIngredients(recipe.mInputs, recipe.mFluidInputs, assemblerOutputs[index].name());
            if (index == 1)
                require(recipe.getMetadataOrDefault(CLEANROOM, false), "Molecular casing retains its cleanroom gate");
        }
        report.append("Seven survival recipes exist in native registries with complete runtime ingredients, exact ")
            .append("duration/power/yields, research and the controller's four-UIV-circuit gate.\n");
    }

    private static void validateIngredients(ItemStack[] items, FluidStack[] fluids, String description) {
        require(items != null && items.length > 0, "Missing item inputs: " + description);
        for (ItemStack input : items) require(input != null && input.getItem() != null && input.stackSize >= 0,
            "Unavailable runtime item ingredient: " + description);
        require(fluids != null, "Missing fluid input array: " + description);
        for (FluidStack input : fluids) require(input != null && input.getFluid() != null && input.amount > 0,
            "Unavailable runtime fluid ingredient: " + description);
    }

    private void registerFixtures() {
        for (int mode = 0; mode < 2; mode++) {
            nativeCounts[mode] = map(mode).getAllRecipes().size();
            for (int input = mode * 2; input < mode * 2 + 2; input++) {
                GTRecipeBuilder builder = GTRecipeBuilder.builder().itemOutputs(stack(20 + input, 1)).duration(80)
                    .eut(30);
                if (mode == 0) builder.itemInputs(stack(input, 1), ItemList.Shape_Extruder_Rod.get(0));
                else builder.itemInputs(stack(input, 1));
                builder.addTo(map(mode));
            }
        }
        GTRecipeBuilder.builder().itemInputs(stack(4, 1), ItemList.Shape_Extruder_Rod.get(0)).itemOutputs(stack(24, 1))
            .duration(80).eut(7680).addTo(map(0));
    }

    private void verifySingleRecipeTasks() {
        setEnergy(ItemList.Hatch_Energy_LV);
        verifyStructure();
        for (int mode = 0; mode < 2; mode++) {
            resetProcessing();
            machine.setMachineMode(mode);
            if (mode == 0) seed(stack(0, 3), stack(1, 2), ItemList.Shape_Extruder_Rod.get(1));
            else seed(stack(2, 3), stack(3, 2));
            require(machine.checkProcessing().wasSuccessful(), "Single-recipe task in mode " + mode);
            require(machine.getModuleWorks().size() == 1 && machine.getModuleWorks().get(0).parallels == 1,
                "An unmodified controller starts one recipe task with one parallel, mode " + mode);
            int firstOutput = outputCount(stack(20 + mode * 2, 1));
            int secondOutput = outputCount(stack(21 + mode * 2, 1));
            require(firstOutput + secondOutput == 1 && firstOutput * secondOutput == 0,
                "Different input recipes are never merged into a task, mode " + mode);
            require(
                inputCount(stack(mode * 2, 1)) == 3 - firstOutput
                    && inputCount(stack(mode * 2 + 1, 1)) == 2 - secondOutput,
                "Only the selected recipe consumes its material, mode " + mode);
            if (mode == 0)
                require(inputCount(ItemList.Shape_Extruder_Rod.get(1)) == 1, "GT-defined non-consumed mold remains");
        }
        resetProcessing();
        machine.setMachineMode(1);
        seed(stack(0, 3), ItemList.Shape_Extruder_Rod.get(1));
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(0, 1)) == 3,
            "Compressor mode refuses extrusion inputs without consuming them");
        machine.setMachineMode(0);
        require(
            machine.checkProcessing().wasSuccessful() && outputCount(stack(20, 1)) == 1 && inputCount(stack(0, 1)) == 2,
            "Changing back to extrusion immediately selects its recipe map");
        report.append("Both maps start one selected recipe per task; mode changes never process the other map.\n");
    }

    private void verifyVoltageGate() {
        resetProcessing();
        machine.setMachineMode(0);
        setEnergy(ItemList.Hatch_Energy_MV);
        verifyStructure();
        seed(stack(4, 1), ItemList.Shape_Extruder_Rod.get(1));
        require(!machine.checkProcessing().wasSuccessful() && inputCount(stack(4, 1)) == 1,
            "MV voltage cannot start an IV recipe and preserves material");
        setEnergy(ItemList.Hatch_Energy_IV);
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(24, 1)) == 1,
            "An IV energy hatch unlocks the same high-voltage recipe");
        setEnergy(ItemList.Hatch_Energy_EV);
        verifyStructure();
        report.append("Native recipe voltage gating rejects low power and recovers after a physical hatch upgrade.\n");
    }

    private void verifyOutputProtection() {
        resetProcessing();
        machine.setMachineMode(1);
        var bus = machine.mOutputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        seed(stack(2, 3), stack(3, 2));
        require(!machine.checkProcessing().wasSuccessful() && machine.getModuleWorks().isEmpty(),
            "Full protected output bus rejects a new task");
        require(inputCount(stack(2, 1)) == 3 && inputCount(stack(3, 1)) == 2,
            "Failed output reservation consumes neither recipe input");
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        require(
            machine.checkProcessing().wasSuccessful() && outputCount(stack(22, 1)) + outputCount(stack(23, 1)) == 1
                && inputCount(stack(2, 1)) + inputCount(stack(3, 1)) == 4,
            "Emptying output storage permits one exact recipe task");
        report.append("A full output bus preserves inputs; clearing storage permits one selected recipe task.\n");
    }

    private void verifyPersistence() {
        require(machine.getMachineMode() == 1 && machine.getModuleWorks().size() == 1,
            "Save a committed compressor task");
        NBTTagCompound expectedWork = machine.getModuleWorks().get(0).save();
        int firstOutput = outputCount(stack(22, 1)), secondOutput = outputCount(stack(23, 1));
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
        machine = (FieldExtruderFactory) base.getMetaTileEntity();
        require(machine.getMachineMode() == 1 && machine.getRecipeMap() == map(1), "Persisted compressor mode");
        require(
            machine.getModuleWorks().size() == 1 && machine.getModuleWorks().get(0).save().equals(expectedWork)
                && outputCount(stack(22, 1)) == firstOutput
                && outputCount(stack(23, 1)) == secondOutput,
            "A real tile reload preserves the complete task ledger and exact pending output");
        verifyStructure();
        for (int mode = 0; mode < 2; mode++) {
            machine.setMachineMode(mode);
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            machine.setMachineMode(1 - mode);
            machine.loadNBTData(tag);
            require(machine.getMachineMode() == mode && machine.getRecipeMap() == map(mode), "Saved mode " + mode);
        }
        report.append("NBT reload preserves both recipe modes and the complete committed task ledger.\n");
    }

    private void cleanupFixtures() {
        for (int mode = 0; mode < 2; mode++) {
            RecipeMap<?> recipes = map(mode);
            recipes.getBackend().removeRecipes(recipes.getAllRecipes().stream().filter(
                recipe -> Arrays.stream(recipe.mInputs).anyMatch(input -> input != null && input.getItem() == fixture))
                .collect(Collectors.toList()));
            require(recipes.getAllRecipes().size() == nativeCounts[mode],
                "Native recipes remain unchanged mode " + mode);
        }
    }

    private void resetProcessing() {
        base.disableWorking();
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        saved.setTag("gtngModuleWork", new NBTTagList());
        saved.setBoolean("gtngModuleLegacyRun", false);
        machine.loadNBTData(saved);
        if (!machine.mInputBusses.isEmpty()) seed();
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
        BigInteger amount = BigInteger.ZERO;
        for (ModuleWork work : machine.getModuleWorks()) {
            NBTTagList items = work.outputs.save().getTagList("items", 10);
            for (int index = 0; index < items.tagCount(); index++) {
                NBTTagCompound entry = items.getCompoundTagAt(index);
                if (GTUtility.areStacksEqual(GTUtility.loadItem(entry, "stack"), expected))
                    amount = amount.add(new BigInteger(entry.getString("amount")));
            }
        }
        return amount.intValueExact();
    }

    private void setEnergy(ItemList hatch) {
        placeAt(6, 1, 0, hatch);
    }

    private void placeAt(int col, int row, int depth, ItemList hatch) {
        int[] pos = position(col, row, depth);
        place(base.getWorld(), pos[0], pos[1], pos[2], hatch.get(1).getItemDamage());
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
        machine.getExtendedFacing().getWorldOffset(new int[] { col - FieldExtruderStructure.OFFSET_X,
            row - FieldExtruderStructure.OFFSET_Y, depth - FieldExtruderStructure.OFFSET_Z }, offset);
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

    private static RecipeMap<?> map(int mode) {
        return mode == 0 ? RecipeMaps.extruderRecipes : RecipeMaps.compressorRecipes;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            require(++stageFrames <= 1800, "Visual stage timeout at mode " + openedMode);
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
            if (openedMode < 0) {
                if (shownView != capturedView + 1 || requestedView >= 0) return;
                if (++frames < 80) return;
                capture(shownView == 0 ? "structure-overview" : "structure-textures");
                capturedView = shownView;
                frames = stageFrames = 0;
                if (capturedView == 0) requestedView = 1;
                else requestedMode = 0;
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
            CycleButtonWidget button = findModeButton(wrapper.getScreen().getMainPanel());
            require(button != null, "Native two-mode GUI button");
            int expected = capturedMode + 1;
            if (openedMode != expected || button.getIntValue().getIntValue() != expected) {
                frames = 0;
                return;
            }
            if (++frames < 40) return;
            capture("mode-" + openedMode);
            capturedMode = openedMode;
            frames = stageFrames = 0;
            if (capturedMode == 0) {
                button.next();
                return;
            }
            report.append("Native mode button synchronizes extrusion/compression to the server.\n");
            tooltipStage = 0;
            showTooltip();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private MultiblockTooltipBuilder verifyTooltips(String locale) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (Language language : mc.getLanguageManager().getLanguages()) {
            if (language.getLanguageCode().equals(locale)) {
                mc.getLanguageManager().setCurrentLanguage(language);
                mc.gameSettings.language = locale;
                mc.refreshResources();
                break;
            }
        }
        Method create = FieldExtruderFactory.class.getDeclaredMethod("createTooltip");
        create.setAccessible(true);
        MultiblockTooltipBuilder tooltip = (MultiblockTooltipBuilder) create.invoke(machine);
        String text = String.join("\n", tooltip.getInformation()) + "\n\n"
            + String.join("\n", tooltip.getStructureInformation());
        require(!text.contains("gtng.") && !text.contains("gt.mbtt.") && !text.contains("gt.blockmachines."),
            "All tooltip translations resolve: " + locale);
        require(text.contains("15") && text.contains("9") && text.contains("63"),
            "Tooltip reports original structure dimensions: " + locale);
        Files.write(new File(output(), "tooltip-" + locale + ".txt").toPath(), text.getBytes(StandardCharsets.UTF_8));
        return tooltip;
    }

    private void showTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = 1;
        if (tooltipStage % 2 == 0) currentTooltip = verifyTooltips(tooltipStage < 2 ? "zh_CN" : "en_US");
        List<String> lines = new ArrayList<>();
        lines.add(GTNGItemList.FieldExtruderFactory.get(1).getDisplayName());
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

    private static CycleButtonWidget findModeButton(IWidget widget) {
        if (widget instanceof CycleButtonWidget button && button.getStateCount() == 2) return button;
        for (IWidget child : widget.getChildren()) {
            CycleButtonWidget button = findModeButton(child);
            if (button != null) return button;
        }
        return null;
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
        System.out.println("FIELD_EXTRUDER_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.fieldExtruder.output"));
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
