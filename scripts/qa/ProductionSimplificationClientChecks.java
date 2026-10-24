package com.xyp.gtnotgood.qa;

import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.xyp.gtnotgood.common.machines.multiblock.IntegratedChemicalPlant;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.KerrNewmanHomogenizer;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.processing.ModuleWork;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.ContainerNull;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.nei.GTNEIDefaultHandler;

@Mod(
    modid = "productionsimplificationqa",
    name = "Production Simplification QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class ProductionSimplificationClientChecks {

    private static final int[] COUNTS = { 4, 3 };
    private final MTEMultiBlockBase[] machines = new MTEMultiBlockBase[2];
    private final BaseMetaTileEntity[] tiles = new BaseMetaTileEntity[2];
    private final StringBuilder report = new StringBuilder();
    private Item fixture;
    private World world;
    private EntityPlayerMP player;
    private int position, ticks, serverTicks, openAt, opening = -1, stage, frames, selected;
    private volatile int requested = -1, opened = -1;
    private boolean started;
    private volatile boolean checked, finished;
    private int neiStage = -1;
    private GTNEIDefaultHandler neiHandler;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.productionSimplification.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("productionSimplificationFixture");
        GameRegistry.registerItem(fixture, "fixture");
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        try {
            require(++ticks < 9000, "Client timeout at GUI " + opened + " stage " + stage);
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.renderDistanceChunks = 2;
            mc.gameSettings.guiScale = 2;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("production-simplification-" + System.currentTimeMillis(),
                    "Production Simplification QA",
                    new WorldSettings(885L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            require(++serverTicks < 7200, "Server QA timeout");
            player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            world = player.worldObj;
            if (!checked) {
                world.setWorldTime(6000);
                machines[0] = rig(GTNGItemList.IntegratedChemicalPlant.get(1), 0);
                machines[1] = rig(GTNGItemList.KerrNewmanHomogenizer.get(1), 16);
                for (int i = 0; i < 2; i++) tiles[i] = (BaseMetaTileEntity) machines[i].getBaseMetaTileEntity();
                verifyMapsAndPersistence();
                verifyRegisteredValues();
                runAll(machines[0], 3, GTNGRecipeMaps.PetrochemicalSynthesisRecipes);
                runAll(machines[1], 1, GTNGRecipeMaps.RareEarthDustRecipes);
                runAll(machines[1], 2, GTNGRecipeMaps.RareEarthOreRecipes);
                verifyOilAlternatives();
                verifyModeIsolation();
                verifyME();
                verifySpeed();
                for (var machine : machines) {
                    reset(machine);
                    machine.setMachineMode(0);
                }
                checked = true;
                requested = 0;
                saveProgress();
            }
            if (requested >= 0) {
                opening = requested;
                requested = -1;
                player.closeScreen();
                BaseMetaTileEntity base = tiles[opening];
                player.playerNetServerHandler.setPlayerLocation(base.getXCoord() + 0.5, base.getYCoord() + 1,
                    base.getZCoord() - 3, 0, 0);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                openAt = serverTicks + 30;
            }
            if (opening >= 0 && serverTicks >= openAt) {
                int index = opening;
                opening = -1;
                machines[index].onRightclick(tiles[index], player);
                opened = index;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private MTEMultiBlockBase rig(ItemStack controller, int x) {
        MTEMultiBlockBase machine = (MTEMultiBlockBase) place(controller, x, 100, -8).getMetaTileEntity();
        machine.mMachine = true;
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        require(machine.addEnergyInputToMachineList(place(ItemList.Hatch_Energy_UV.get(1)), 0), "Energy hatch");
        for (int i = 0; i < 3; i++)
            require(machine.addInputBusToMachineList(place(ItemList.Hatch_Input_Bus_UV.get(1)), 0), "Input bus");
        for (int i = 0; i < 8; i++) {
            require(machine.addInputHatchToMachineList(place(ItemList.Hatch_Input_UV.get(1)), 0), "Input hatch");
            require(machine.addOutputBusToMachineList(place(ItemList.Hatch_Output_Bus_UV.get(1)), 0), "Output bus");
            require(machine.addOutputHatchToMachineList(place(ItemList.Hatch_Output_UV.get(1)), 0), "Output hatch");
        }
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(1);
        machine.setBatchMode(false);
        machine.setRecipeLocking(false);
        return machine;
    }

    private void verifyMapsAndPersistence() {
        RecipeMap<?>[][] maps = {
            { GTNGRecipeMaps.FuelRefiningRecipes, GTNGRecipeMaps.WoodDistillationRecipes,
                GTNGRecipeMaps.PetrochemicalRecipes, GTNGRecipeMaps.PetrochemicalSynthesisRecipes },
            { RecipeMaps.mixerNonCellRecipes, GTNGRecipeMaps.RareEarthDustRecipes,
                GTNGRecipeMaps.RareEarthOreRecipes } };
        for (int index = 0; index < machines.length; index++) {
            MTEMultiBlockBase machine = machines[index];
            require(new ArrayList<>(machine.getAvailableRecipeMaps()).equals(Arrays.asList(maps[index])),
                "Ordered maps " + index);
            for (int mode = 0; mode < COUNTS[index]; mode++) {
                machine.setMachineMode(mode);
                require(machine.getRecipeMap() == maps[index][mode], "Map identity " + index + "/" + mode);
                NBTTagCompound saved = new NBTTagCompound();
                machine.saveNBTData(saved);
                machine.setMachineMode(0);
                machine.loadNBTData(saved);
                require(machine.getMachineMode() == mode && machine.getRecipeMap() == maps[index][mode],
                    "Saved map " + index + "/" + mode);
                require(machine.nextMachineMode() == (mode + 1) % COUNTS[index], "Mode wrap " + index + "/" + mode);
            }
            for (int invalid : new int[] { -1, COUNTS[index], Integer.MAX_VALUE }) {
                machine.setMachineMode(invalid);
                require(machine.getMachineMode() == 0, "Invalid direct mode normalizes " + index);
                NBTTagCompound tag = new NBTTagCompound();
                machine.saveNBTData(tag);
                tag.setInteger("machineMode", invalid);
                machine.loadNBTData(tag);
                require(machine.getMachineMode() == 0, "Invalid saved mode normalizes " + index);
            }
            NBTTagCompound legacy = new NBTTagCompound();
            machine.saveNBTData(legacy);
            legacy.removeTag("machineMode");
            machine.loadNBTData(legacy);
            require(machine.getMachineMode() == 0, "Legacy save keeps original mode " + index);
        }
        report.append("Mode map order, all saved modes, wrap, invalid modes and legacy saves passed.\n");
    }

    private void verifyRegisteredValues() {
        int[] seconds = { 30, 30, 30, 30, 16, 36, 30, 60, 16, 12, 16, 16 };
        int[] eut = { 30, 120, 120, 120, 480, 120, 6144, 120, 480, 1960, 480, 10240 };
        int[] firstFluid = { 1656, 1656, 828, 1656, 12000, 12000, 11130, 9000, 9000, 1000, 9000, 1000 };
        List<GTRecipe> petro = new ArrayList<>(GTNGRecipeMaps.PetrochemicalSynthesisRecipes.getAllRecipes());
        int alternatives = petro.size() == 33 ? 4 : 1;
        require(petro.size() == 12 || petro.size() == 33, "12 logical petrochemical recipes, with optional expansion");
        for (int circuit = 1; circuit <= 12; circuit++) {
            final int selectedCircuit = circuit;
            List<GTRecipe> group = petro.stream().filter(recipe -> circuit(recipe) == selectedCircuit)
                .collect(Collectors.toList());
            require(group.size() == (circuit <= 7 ? alternatives : 1), "Petrochemical circuit " + circuit);
            for (GTRecipe recipe : group) {
                require(recipe.mDuration == seconds[circuit - 1] * 20 && recipe.mEUt == eut[circuit - 1],
                    "Upstream petrochemical time/power " + circuit);
                require(recipe.mFluidOutputs[0].amount == firstFluid[circuit - 1],
                    "Upstream principal fluid amount " + circuit);
                if (circuit == 7) {
                    require(GTUtility.areStacksEqual(recipe.mOutputs[0],
                        GTOreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 1))
                        && recipe.mOutputs[0].stackSize == 80, "Circuit 7 keeps the declared 80 carbon dust");
                    require(recipe.getOutputChance(0) == 8800 && recipe.getOutputChance(1) == 8000,
                        "Circuit 7 byproduct chances");
                }
                if (
                    circuit == 4
                ) require(recipe.mFluidOutputs[0].isFluidEqual(Materials.Styrene.getFluid(1)), "Circuit 4 is styrene");
            }
        }
        require(
            powerTimes(GTNGRecipeMaps.RareEarthDustRecipes)
                .equals(sorted("7680:6000", "7680:200", "16384:2400", "16384:2400", "16384:3600", "16384:3600",
                    "16384:3600", "32768:3000", "32768:3000", "32768:6000", "32768:320")),
            "All eleven upstream dust times and powers");
        require(
            powerTimes(GTNGRecipeMaps.RareEarthOreRecipes)
                .equals(sorted("480:2400", "7680:1200", "7680:1200", "7680:1200", "1960:2400", "1960:2400", "1960:2400",
                    "7680:2400", "7680:3600", "7680:3600", "12288:3600", "12288:3600")),
            "All twelve upstream ore times and powers");
        for (GTRecipe recipe : GTNGRecipeMaps.RareEarthOreRecipes.getAllRecipes()) {
            require(recipe.getMetadataOrDefault(COIL_HEAT, 0) == 0 && recipe.mSpecialValue == 0,
                "Ore recipes have no heat gate");
            require(
                recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].amount == 64000
                    && recipe.mFluidInputs[0].isFluidEqual(new FluidStack(FluidRegistry.WATER, 1)),
                "Ore recipe consumes exactly 64 buckets of water");
            require(Arrays.stream(recipe.mInputs).filter(input -> input.stackSize > 0)
                .mapToInt(input -> input.stackSize).sum() == 64, "Ore recipe consumes exactly 64 ore");
        }
        report.append("Registered petrochemical recipes=").append(petro.size())
            .append("; dust=11; ore=12. All upstream EU/t and durations match; no ore heat metadata.\n");
    }

    private static List<String> powerTimes(RecipeMap<?> map) {
        return map.getAllRecipes().stream().map(recipe -> recipe.mEUt + ":" + recipe.mDuration).sorted()
            .collect(Collectors.toList());
    }

    private static List<String> sorted(String... values) {
        return Arrays.stream(values).sorted().collect(Collectors.toList());
    }

    private static int circuit(GTRecipe recipe) {
        for (ItemStack stack : recipe.mInputs) for (int value = 1; value <= 12; value++)
            if (GTUtility.areStacksEqual(stack, GTUtility.getIntegratedCircuit(value))) return value;
        return -1;
    }

    private void runAll(MTEMultiBlockBase machine, int mode, RecipeMap<?> map) throws Exception {
        int count = 0;
        for (GTRecipe recipe : new ArrayList<>(map.getAllRecipes())) {
            reset(machine);
            machine.setMachineMode(mode);
            seed(machine, recipe);
            run(machine, recipe, map.unlocalizedName + " #" + ++count);
        }
        report.append(map.unlocalizedName).append(": ").append(count)
            .append(" registered recipes ran with ordinary real hatches, exact input consumption and products.\n");
    }

    private void run(MTEMultiBlockBase machine, GTRecipe recipe, String label) throws Exception {
        var result = machine.checkProcessing();
        require(result.wasSuccessful(), label + " starts: " + result.getID());
        for (ItemStack input : recipe.mInputs)
            require(itemCount(machine, input) == (input.stackSize == 0 ? 1 : 0), label + " exact item consumption");
        for (FluidStack input : recipe.mFluidInputs)
            require(fluidCount(machine, input) == 0, label + " exact fluid consumption");
        verifyOutputs(machine, recipe, label);
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        int mode = machine.getMachineMode();
        int duration = machine.mMaxProgresstime;
        String outputs = outputTag(machine).toString();
        machine.loadNBTData(saved);
        require(machine.getMachineMode() == mode && machine.mMaxProgresstime == duration
            && outputTag(machine).toString().equals(outputs), label + " committed work survives NBT");
        verifyOutputs(machine, recipe, label + " after NBT");
    }

    private void verifyOilAlternatives() throws Exception {
        IntegratedChemicalPlant machine = (IntegratedChemicalPlant) machines[0];
        GTRecipe recipe = GTNGRecipeMaps.PetrochemicalSynthesisRecipes.getAllRecipes().stream()
            .filter(candidate -> circuit(candidate) == 1).findFirst().orElseThrow();
        for (Materials oil : new Materials[] { Materials.Oil, Materials.OilLight, Materials.OilHeavy,
            Materials.OilExtraHeavy }) {
            reset(machine);
            machine.setMachineMode(3);
            seed(machine, recipe);
            machine.mInputHatches.get(0).mFluid = oil.getFluid(10000);
            require(machine.checkProcessing().wasSuccessful(), "Alternative raw oil " + oil);
            require(fluidCount(machine, oil.getFluid(1)) == 0, "Alternative oil fully consumed " + oil);
            verifyOutputs(machine, recipe, "Alternative oil " + oil);
        }
        report.append("All four raw-oil grades actually process; styrene naming follows the real fluid.\n");
    }

    private void verifyModeIsolation() throws Exception {
        for (int mode : new int[] { 1, 2 }) {
            MTEMultiBlockBase machine = machines[1];
            GTRecipe recipe = (mode == 1 ? GTNGRecipeMaps.RareEarthDustRecipes : GTNGRecipeMaps.RareEarthOreRecipes)
                .getAllRecipes().iterator().next();
            reset(machine);
            machine.setMachineMode(0);
            seed(machine, recipe);
            long beforeItems = totalItems(machine), beforeFluids = totalFluids(machine);
            require(!machine.checkProcessing().wasSuccessful(), "Original mixer rejects rare-earth mode " + mode);
            require(beforeItems == totalItems(machine) && beforeFluids == totalFluids(machine),
                "Wrong map preserves all inputs " + mode);
            machine.setMachineMode(mode);
            run(machine, recipe, "Switch from mixer to rare-earth " + mode);
        }
        report.append("Wrong mode refuses rare-earth inputs without consumption; switching enables the same inputs.\n");
    }

    private void verifyME() throws Exception {
        KerrNewmanHomogenizer machine = (KerrNewmanHomogenizer) machines[1];
        machine.mInputBusses.clear();
        machine.mInputHatches.clear();
        BaseMetaTileEntity bufferTile = place(ItemList.Hatch_CraftingInput_Bus_ME.get(1));
        require(machine.addInputBusToMachineList(bufferTile, 0), "ME input accepted by native hatch adder");
        IDualInputHatch buffer = (IDualInputHatch) bufferTile.getMetaTileEntity();
        for (int mode : new int[] { 1, 2 }) {
            reset(machine);
            machine.setMachineMode(mode);
            GTRecipe recipe = (mode == 1 ? GTNGRecipeMaps.RareEarthDustRecipes : GTNGRecipeMaps.RareEarthOreRecipes)
                .getAllRecipes().stream().filter(candidate -> candidate.mFluidInputs.length > 0).findFirst()
                .orElseThrow();
            seedME(buffer, recipe);
            require(machine.checkProcessing().wasSuccessful(), "Native fluid ME pattern mode " + mode);
            for (var iterator = buffer.inventories(); iterator.hasNext();) {
                IDualInputInventory inventory = iterator.next();
                for (ItemStack input : inventory.getItemInputs()) if (
                    input != null
                ) require(
                    input.stackSize == 1 && Arrays.stream(recipe.mInputs)
                        .anyMatch(catalyst -> catalyst.stackSize == 0 && GTUtility.areStacksEqual(catalyst, input)),
                    "ME retains only unconsumed circuit");
                for (FluidStack fluid : inventory.getFluidInputs())
                    require(fluid == null || fluid.amount == 0, "ME consumes exact native fluids");
            }
            verifyOutputs(machine, recipe, "ME mode " + mode);
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            String outputs = outputTag(machine).toString();
            machine.loadNBTData(saved);
            require(machine.getMachineMode() == mode && outputTag(machine).toString().equals(outputs),
                "ME committed result survives save/reload " + mode);
        }
        machine.mDualInputHatches.clear();
        for (int i = 0; i < 3; i++) machine.addInputBusToMachineList(place(ItemList.Hatch_Input_Bus_UV.get(1)), 0);
        machine.addInputHatchToMachineList(place(ItemList.Hatch_Input_UV.get(1)), 0);
        report.append("Native Ultimate Encoded Patterns process real dust and ore recipes with native fluid stacks, ")
            .append("exact consumption, catalyst retention and persistent committed outputs. No live AE CPU claim.\n");
    }

    private void seedME(IDualInputHatch hatch, GTRecipe recipe) {
        ItemStack encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        MEInventoryCrafting delivery = new MEInventoryCrafting(new ContainerNull(), 4, 4);
        int slot = 0;
        for (ItemStack input : recipe.mInputs) {
            ItemStack actual = input.copy();
            actual.stackSize = Math.max(1, actual.stackSize);
            var stack = AEItemStack.create(actual);
            NBTTagCompound item = new NBTTagCompound();
            Platform.writeStackNBT(stack, item);
            inputs.appendTag(item);
            delivery.setInventorySlotContents(slot++, stack.copy());
        }
        for (FluidStack input : recipe.mFluidInputs) {
            var stack = AEFluidStack.create(input);
            NBTTagCompound fluid = new NBTTagCompound();
            Platform.writeStackNBT(stack, fluid);
            inputs.appendTag(fluid);
            delivery.setInventorySlotContents(slot, stack.copy());
            require(delivery.getAEStackInSlot(slot++) instanceof IAEFluidStack, "ME delivery uses native fluids");
        }
        require(slot <= 16, "Pattern input width");
        while (inputs.tagCount() < 16) inputs.appendTag(new NBTTagCompound());
        for (ItemStack output : recipe.mOutputs) {
            NBTTagCompound item = new NBTTagCompound();
            Platform.writeStackNBT(AEItemStack.create(output), item);
            outputs.appendTag(item);
        }
        for (FluidStack output : recipe.mFluidOutputs) {
            NBTTagCompound fluid = new NBTTagCompound();
            Platform.writeStackNBT(AEFluidStack.create(output), fluid);
            outputs.appendTag(fluid);
        }
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        require(((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world) != null,
            "Native Ultimate Encoded Pattern decodes");
        hatch.setInventorySlotContents(0, encoded);
        var inventory = hatch.inventories().next();
        require(inventory instanceof MTEHatchCraftingInputME.PatternSlot<?>, "Real native GT pattern slot");
        require(((MTEHatchCraftingInputME.PatternSlot<?>) inventory).insertItemsAndFluids(delivery),
            "Native pattern accepts its delivered batch");
    }

    private void verifySpeed() throws Exception {
        KerrNewmanHomogenizer machine = (KerrNewmanHomogenizer) machines[1];
        RecipeMap<?> map = GTNGRecipeMaps.RareEarthOreRecipes;
        GTRecipe recipe = GTRecipeBuilder.builder().itemInputs(new ItemStack(fixture, 1, 0))
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 100)).itemOutputs(new ItemStack(fixture, 1, 1))
            .duration(4800).eut(30).addTo(map).iterator().next();
        try {
            for (int tier = 0; tier < 2; tier++) {
                reset(machine);
                machine.setMachineMode(2);
                machine.mEnergyHatches.clear();
                machine.addEnergyInputToMachineList(
                    place(tier == 0 ? ItemList.Hatch_Energy_LV.get(1) : ItemList.Hatch_Energy_MV.get(1)), 0);
                seed(machine, recipe);
                require(machine.checkProcessing().wasSuccessful(), "Speed fixture starts " + tier);
                ModuleWork work = machine.getModuleWorks().get(0);
                require(work.duration == (tier == 0 ? 80 : 40) && work.eut == (tier == 0 ? 30 : 120),
                    "60x base speed and existing 2x time / 4x power overclock: " + work.duration + "/" + work.eut);
            }
        } finally {
            map.getBackend().removeRecipes(List.of(recipe));
            reset(machine);
            machine.mEnergyHatches.clear();
            machine.addEnergyInputToMachineList(place(ItemList.Hatch_Energy_UV.get(1)), 0);
        }
        report.append("New modes retain Kerr's current 60x base speed and default 2x speed / 4x EU overclock.\n");
    }

    private static void reset(MTEMultiBlockBase machine) throws Exception {
        machine.getBaseMetaTileEntity().disableWorking();
        if (machine instanceof GTNGModularMachineBase<?>) {
            Field works = GTNGModularMachineBase.class.getDeclaredField("moduleWorks");
            works.setAccessible(true);
            ((List<?>) works.get(machine)).clear();
        }
        machine.mMaxProgresstime = machine.mProgresstime = 0;
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        ((MTEExtendedPowerMultiBlockBase<?>) machine).lEUt = 0;
        for (var bus : machine.mInputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
    }

    private static void seed(MTEMultiBlockBase machine, GTRecipe recipe) {
        int busIndex = 0, slot = 0;
        for (ItemStack input : recipe.mInputs) {
            int remaining = Math.max(1, input.stackSize);
            while (remaining > 0) {
                var bus = machine.mInputBusses.get(busIndex);
                if (slot >= bus.getSizeInventory()) {
                    bus = machine.mInputBusses.get(++busIndex);
                    slot = 0;
                }
                ItemStack portion = input.copy();
                portion.stackSize = Math.min(remaining, portion.getMaxStackSize());
                bus.setInventorySlotContents(slot++, portion);
                remaining -= portion.stackSize;
            }
        }
        for (int i = 0; i < recipe.mFluidInputs.length; i++)
            machine.mInputHatches.get(i).mFluid = recipe.mFluidInputs[i].copy();
    }

    private static long itemCount(MTEMultiBlockBase machine, ItemStack type) {
        long count = 0;
        for (var bus : machine.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack stack = bus.getStackInSlot(slot);
            if (stack != null && GTUtility.areStacksEqual(stack, type)) count += stack.stackSize;
        }
        return count;
    }

    private static long fluidCount(MTEMultiBlockBase machine, FluidStack type) {
        long count = 0;
        for (var hatch : machine.mInputHatches)
            if (hatch.mFluid != null && hatch.mFluid.isFluidEqual(type)) count += hatch.mFluid.amount;
        return count;
    }

    private static long totalItems(MTEMultiBlockBase machine) {
        long count = 0;
        for (var bus : machine.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack stack = bus.getStackInSlot(slot);
            if (stack != null) count += stack.stackSize;
        }
        return count;
    }

    private static long totalFluids(MTEMultiBlockBase machine) {
        return machine.mInputHatches.stream().mapToLong(hatch -> hatch.mFluid == null ? 0 : hatch.mFluid.amount).sum();
    }

    private static NBTTagCompound outputTag(MTEMultiBlockBase machine) {
        if (machine instanceof GTNGModularMachineBase<?>modular)
            return modular.getModuleWorks().isEmpty() ? new NBTTagCompound()
                : modular.getModuleWorks().get(0).outputs.save();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList items = new NBTTagList(), fluids = new NBTTagList();
        if (machine.mOutputItems != null) for (ItemStack stack : machine.mOutputItems) if (stack != null) {
            NBTTagCompound entry = new NBTTagCompound();
            ItemStack unit = stack.copy();
            unit.stackSize = 1;
            entry.setTag("stack", GTUtility.saveItem(unit));
            entry.setString("amount", Integer.toString(stack.stackSize));
            items.appendTag(entry);
        }
        if (machine.mOutputFluids != null) for (FluidStack stack : machine.mOutputFluids) if (stack != null) {
            NBTTagCompound entry = stack.writeToNBT(new NBTTagCompound());
            entry.setString("bigAmount", Integer.toString(stack.amount));
            fluids.appendTag(entry);
        }
        tag.setTag("items", items);
        tag.setTag("fluids", fluids);
        return tag;
    }

    private static void verifyOutputs(MTEMultiBlockBase machine, GTRecipe recipe, String label) {
        NBTTagCompound tag = outputTag(machine);
        NBTTagList items = tag.getTagList("items", 10), fluids = tag.getTagList("fluids", 10);
        for (int index = 0; index < recipe.mOutputs.length; index++) {
            long amount = 0;
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound entry = items.getCompoundTagAt(i);
                ItemStack stack = GTUtility.loadItem(entry.getCompoundTag("stack"));
                if (GTUtility.areStacksEqual(stack, recipe.mOutputs[index]))
                    amount += Long.parseLong(entry.getString("amount"));
            }
            int chance = recipe.getOutputChance(index), expected = recipe.mOutputs[index].stackSize;
            require(chance == 10000 ? amount == expected : amount >= 0 && amount <= expected,
                label + " item output " + index + ": " + amount + "/" + expected);
        }
        for (int index = 0; index < recipe.mFluidOutputs.length; index++) {
            long amount = 0;
            for (int i = 0; i < fluids.tagCount(); i++) {
                NBTTagCompound entry = fluids.getCompoundTagAt(i);
                FluidStack stack = FluidStack.loadFluidStackFromNBT(entry);
                if (stack != null && stack.isFluidEqual(recipe.mFluidOutputs[index]))
                    amount += Long.parseLong(entry.getString("bigAmount"));
            }
            require(amount == recipe.mFluidOutputs[index].amount, label + " exact fluid output " + index);
        }
        for (int i = 0; i < items.tagCount(); i++) {
            ItemStack actual = GTUtility.loadItem(items.getCompoundTagAt(i).getCompoundTag("stack"));
            require(Arrays.stream(recipe.mOutputs).anyMatch(type -> GTUtility.areStacksEqual(actual, type)),
                label + " no extra item type");
        }
        for (int i = 0; i < fluids.tagCount(); i++) {
            FluidStack actual = FluidStack.loadFluidStackFromNBT(fluids.getCompoundTagAt(i));
            require(Arrays.stream(recipe.mFluidOutputs).anyMatch(type -> actual != null && type.isFluidEqual(actual)),
                label + " no extra fluid type");
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || opened < 0) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (neiStage >= 0) {
                renderNei();
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 25) return;
            var screen = wrapper.getScreen();
            ButtonWidget<?> selector = (ButtonWidget<?>) named(screen.getMainPanel(), "gtngModeSelector");
            require(selector != null, "Modern mode selector " + opened);
            IntSyncValue mode = screen.getSyncManager().getMainPSM().findSyncHandler("machineMode", IntSyncValue.class);
            ModularPanel popup = screen.getPanelManager().getOpenPanels().stream()
                .filter(panel -> panel.getName().equals("gtngMachineModes")).findFirst().orElse(null);
            if (stage == 0) {
                require(mode.getIntValue() == 0, "Original mode before GUI selection");
                capture("main-original");
                selector.onMousePressed(0);
            } else if (stage == 1) {
                if (popup == null) return;
                ListWidget<?, ?> list = (ListWidget<?, ?>) named(popup, "gtngModeList");
                require(list.getChildren().size() == COUNTS[opened], "Complete GUI mode list " + opened);
                capture("mode-list");
                selected = opened == 0 ? 3 : 1;
                ((ButtonWidget<?>) named(popup, "gtngMode:" + selected)).onMousePressed(0);
            } else if (stage == 2) {
                if (mode.getIntValue() != selected || machines[opened].getMachineMode() != selected) return;
                require(popup != null, "Ordinary selection keeps list open");
                capture("selected-" + selected);
                if (opened == 1 && selected == 1) {
                    selected = 2;
                    ((ButtonWidget<?>) named(popup, "gtngMode:2")).onMousePressed(0);
                    frames = 0;
                    return;
                }
                selector.onMousePressed(0);
            } else if (stage == 3) {
                if (popup != null) return;
                capture("main-new-mode");
                int index = opened;
                opened = -1;
                requested = index;
            } else if (stage == 4) {
                require(mode.getIntValue() == selected && machines[opened].getMachineMode() == selected,
                    "GUI reopening retains authoritative selected mode");
                capture("reopened");
                report.append(opened == 0 ? "Chemical plant" : "Kerr-Newman")
                    .append(": full mode list, new-mode clicks, real server/client sync and reopening passed.\n");
                if (opened == 0) {
                    opened = -1;
                    requested = 1;
                    stage = -1;
                } else {
                    GTMod.proxy.reloadNEICache();
                    openNei(0);
                    return;
                }
            }
            stage++;
            frames = 0;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void openNei(int index) throws Exception {
        RecipeMap<?> map = switch (index) {
            case 0 -> GTNGRecipeMaps.PetrochemicalSynthesisRecipes;
            case 1 -> GTNGRecipeMaps.RareEarthDustRecipes;
            default -> GTNGRecipeMaps.RareEarthOreRecipes;
        };
        GTRecipe largest = map.getAllRecipes().stream()
            .max(Comparator.comparingInt(recipe -> index == 0 ? recipe.mFluidOutputs.length : recipe.mOutputs.length))
            .orElseThrow();
        neiHandler = new GTNEIDefaultHandler(map.getDefaultRecipeCategory());
        neiHandler.loadCraftingRecipes(map.unlocalizedName);
        neiHandler.arecipes.removeIf(cached -> ((GTNEIDefaultHandler.CachedDefaultRecipe) cached).mRecipe != largest);
        require(neiHandler.arecipes.size() == 1, "Largest real NEI recipe selected " + index);
        ArrayList<ICraftingHandler> handlers = new ArrayList<>();
        handlers.add(neiHandler);
        var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
        constructor.setAccessible(true);
        Minecraft.getMinecraft().displayGuiScreen(constructor.newInstance(handlers));
        neiStage = index;
        frames = 0;
    }

    private void renderNei() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiRecipe<?>gui) || ++frames < 60) return;
        GTNEIDefaultHandler.CachedDefaultRecipe cached = (GTNEIDefaultHandler.CachedDefaultRecipe) neiHandler.arecipes
            .get(0);
        GTRecipe recipe = cached.mRecipe;
        require(cached.getIngredients().size() == recipe.mInputs.length + recipe.mFluidInputs.length,
            "NEI shows every item/fluid input " + neiStage);
        require(cached.getOtherStacks().size() == recipe.mOutputs.length + recipe.mFluidOutputs.length,
            "NEI shows every item/fluid output " + neiStage);
        if (neiStage == 0) require(recipe.mFluidOutputs.length == 7, "Seven-fluid petrochemical NEI page");
        if (neiStage == 1) require(recipe.mOutputs.length == 18, "Eighteen-output rare-earth NEI page");
        List<PositionedStack> slots = new ArrayList<>(cached.getIngredients());
        slots.addAll(cached.getOtherStacks());
        var origin = gui.getRecipePosition(0);
        for (PositionedStack slot : slots)
            require(
                origin.x + slot.relx >= 0 && origin.y + slot.rely >= 0
                    && origin.x + slot.relx + 16 <= gui.width
                    && origin.y + slot.rely + 16 <= gui.height,
                "All real NEI slots fit the visible screen " + neiStage);
        capture("nei-" + (neiStage == 0 ? "petrochemical-seven-fluids"
            : neiStage == 1 ? "rare-earth-eighteen-outputs" : "rare-earth-ore"));
        report.append("NEI page ").append(neiStage).append(": all ").append(slots.size())
            .append(" input/output slots drawn inside the visible screen.\n");
        if (neiStage < 2) openNei(neiStage + 1);
        else {
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\nProduction controllers and real hatches; controlled formation/admission. ")
                    .concat("Existing large geometry and live AE CPU scheduling are outside this run.\n")
                    .concat(report.toString()).getBytes(StandardCharsets.UTF_8));
            System.out.println("PRODUCTION_SIMPLIFICATION_QA_PASS\n" + report);
            finished = true;
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
        ScreenShotHelper.saveScreenshot(output(), (opened == 0 ? "chemical" : "kerr") + "-" + suffix + ".png",
            mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private BaseMetaTileEntity place(ItemStack stack) {
        return place(stack, position++ % 20 * 3, 100, position / 20 * 3);
    }

    private BaseMetaTileEntity place(ItemStack stack, int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        base.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        return base;
    }

    private static File output() {
        File directory = new File(System.getProperty("gtng.productionSimplification.output"));
        directory.mkdirs();
        return directory;
    }

    private void saveProgress() throws Exception {
        Files.write(new File(output(), "processing.txt").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void fail(Throwable failure) {
        if (finished) return;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(),
                ("FAIL\n" + report + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
