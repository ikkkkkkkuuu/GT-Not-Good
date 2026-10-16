package com.xyp.gtnotgood.qa;

import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.xyp.gtnotgood.common.machines.multiblock.IntegratedChemicalPlant;
import com.xyp.gtnotgood.common.recipe.metadata.FuelRefiningTier;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.utils.GuiSlider;
import blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler;
import blockrenderer6343.integration.nei.GuiMultiblockHandler;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.loader.Loaders;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockCasings5;

@Mod(
    modid = "chemicalplantqa",
    name = "Chemical Plant QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class ChemicalPlantClientChecks {

    private Item fixture;
    private BaseMetaTileEntity base;
    private volatile IntegratedChemicalPlant machine;
    private boolean started, structureCaptured;
    private volatile boolean serverChecked, finished;
    private volatile int requestedMode = -1, openedMode = -1;
    private int clientTicks, serverTicks, frames, stageFrames, capturedMode = -1, neiMode = -1;
    private int refiningPreviewStage = -1;
    private final int[] nativeCounts = new int[3];
    private final StringBuilder checks = new StringBuilder();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.chemicalPlant.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("chemicalPlantFixture");
        GameRegistry.registerItem(fixture, "fixture");
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 6000, "Client QA timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = 2;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("chemical-plant-qa-" + System.currentTimeMillis(), "Chemical Plant QA",
                    new WorldSettings(731L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            require(++serverTicks <= 3600, "Server QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) setup(player);
            if (serverTicks == 60) {
                verifyRefiningConstruction();
                verifyStructure();
                require(machine.getRefiningTier() == 0, "Default refining core");
                verifyNativeRecipes();
                verifyModePersistence();
                registerFixtures();
                verifyProcessing();
                verifyHeat();
                verifyRefiningCores();
                verifyRecipeLockSwitch();
                resetProcessing();
                machine.setRecipeLocking(false);
                machine.setMachineMode(0);
                for (int mode = 0; mode < 3; mode++) {
                    RecipeMap<?> map = map(mode);
                    map.getBackend()
                        .removeRecipes(map.getAllRecipes().stream()
                            .filter(recipe -> Arrays.stream(recipe.mInputs)
                                .anyMatch(input -> input != null && input.getItem() == fixture))
                            .collect(Collectors.toList()));
                    require(map.getAllRecipes().size() == nativeCounts[mode], "Fixture cleanup in mode " + mode);
                }
                checks.append("Structure; native recipe yields; three modes; save/reload; invalid mode; wrong mode; ")
                    .append("heat rejection and coil upgrade; refining channels 1-4 match core tiers 1-4; ")
                    .append("unset refining channel stays tier 0 at main channel 1 or 64; ")
                    .append("refining channels 5 and 64 clamp to tier 4; ")
                    .append("core tier rejection and four core upgrades; missing/invalid core rejection; ")
                    .append("core downgrade clears the previous tier; ").append("recipe lock map switching.\n");
                serverChecked = true;
            }
            int mode = requestedMode;
            if (mode >= 0) {
                requestedMode = -1;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(1, 13, -4, 0, 0);
                machine.setMachineMode(mode);
                machine.onRightclick(base, player);
                openedMode = mode;
            }
            if (serverChecked && openedMode >= 0) openedMode = machine.getMachineMode();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        require(!Loader.isModLoaded(ModList.ModIds.GT_NOT_LEISURE), "QA must run without GTNL");
        require(GTNGItemList.IntegratedChemicalPlant.get(1).getItemDamage() == GTNGMachineID.IntegratedChemicalPlant.id,
            "Controller registration");
        World world = player.worldObj;
        world.setWorldTime(6000);
        base = place(world, 0, 12, 0, GTNGMachineID.IntegratedChemicalPlant.id);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (IntegratedChemicalPlant) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        place(world, -2, 12, 0, ItemList.Hatch_Energy_EV.get(1).getItemDamage());
        place(world, -1, 12, 0, ItemList.Hatch_Input_Bus_EV.get(1).getItemDamage());
        place(world, 1, 12, 0, ItemList.Hatch_Output_Bus_EV.get(1).getItemDamage());
        place(world, 2, 12, 0, ItemList.Hatch_Input_EV.get(1).getItemDamage());
        place(world, -1, 14, 0, ItemList.Hatch_Output_EV.get(1).getItemDamage());
        place(world, 1, 14, 0, ItemList.Hatch_Maintenance.get(1).getItemDamage());
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(1);
        player.playerNetServerHandler.setPlayerLocation(-8, 16, -9, -40, 18);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private void verifyRefiningConstruction() {
        for (int tier = 1; tier <= 4; tier++) {
            ItemStack trigger = new ItemStack(Items.diamond);
            ChannelDataAccessor.setChannelData(trigger, "refining", tier);
            verifyRefiningConstruction(trigger, tier, "Refining subchannel " + tier);
        }
        for (int channel : new int[] { 5, 64 }) {
            ItemStack trigger = new ItemStack(Items.diamond);
            ChannelDataAccessor.setChannelData(trigger, "refining", channel);
            verifyRefiningConstruction(trigger, 4, "Out-of-range refining subchannel " + channel);
        }
        for (int mainChannel : new int[] { 64, 1 }) {
            verifyRefiningConstruction(new ItemStack(Items.diamond, mainChannel), 0,
                "Unset refining subchannel with main channel " + mainChannel);
        }
    }

    private void verifyRefiningConstruction(ItemStack trigger, int tier, String description) {
        World world = base.getWorld();
        world.setBlockToAir(0, 12, 2);
        machine.construct(trigger, false);
        Block expected = tier == 0 ? GregTechAPI.sBlockCasings8 : coreBlock(tier);
        require(world.getBlock(0, 12, 2) == expected && world.getBlockMetadata(0, 12, 2) == 0,
            description + " constructs tier " + tier);
        verifyStructure();
        require(machine.getRefiningTier() == tier, description + " validates as recipe tier " + tier);
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Structure: " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Real structure validation");
        require(machine.getHeatingCapacity() > 0, "Real heating coils");
        require(machine.mInputBusses.size() == 1 && machine.mInputHatches.size() == 1
            && machine.mOutputBusses.size() == 1
            && machine.mOutputHatches.size() == 1, "Required hatches");
    }

    private void verifyNativeRecipes() throws Exception {
        int[] expectedCounts = { 25, 1, 11 };
        setCoils(HeatingCoilLevel.MAX);
        base.getWorld().setBlock(0, 12, 2, coreBlock(4), 0, 3);
        verifyStructure();
        require(machine.getRefiningTier() == 4, "Native recipe test uses the tier 4 core");
        Method createLogic = IntegratedChemicalPlant.class.getDeclaredMethod("createProcessingLogic");
        createLogic.setAccessible(true);
        Field lastRecipe = ProcessingLogic.class.getDeclaredField("lastRecipe");
        lastRecipe.setAccessible(true);
        for (int mode = 0; mode < 3; mode++) {
            RecipeMap<?> map = map(mode);
            nativeCounts[mode] = map.getAllRecipes().size();
            require(nativeCounts[mode] == expectedCounts[mode], "Native recipe count in mode " + mode);
            machine.setMachineMode(mode);
            require(machine.getRecipeMap() == map, "Recipe map in mode " + mode);
            int processed = 0;
            for (GTRecipe recipe : map.getAllRecipes()) {
                require(recipe.mEnabled && recipe.mSpecialItems == null, "Native recipe eligibility in mode " + mode);
                ProcessingLogic logic = (ProcessingLogic) createLogic.invoke(machine);
                logic.setMachine(machine).setRecipeMapSupplier(machine::getRecipeMap).setVoidProtection(false, false)
                    .setAvailableVoltage(Integer.MAX_VALUE).setAvailableAmperage(1).setMaxParallelSupplier(() -> 1)
                    .setInputItems(copyItems(recipe.mInputs)).setInputFluids(copyFluids(recipe.mFluidInputs));
                var result = logic.process();
                require(result.wasSuccessful(), "Native processing in mode " + mode + ": " + result.getID());
                GTRecipe selected = (GTRecipe) lastRecipe.get(logic);
                require(selected == recipe, "Exact native recipe selected in mode " + mode + " at index " + processed);
                verifyOutputs(recipe.mOutputs, recipe.mFluidOutputs, logic.getOutputItems(), logic.getOutputFluids());
                processed++;
            }
            require(processed == nativeCounts[mode], "Every native recipe processed in mode " + mode);
            checks.append("Mode ").append(mode).append(": ").append(processed)
                .append(" native recipes; exact selection and item/fluid yields unchanged.\n");
        }
        setCoils(HeatingCoilLevel.LV);
        base.getWorld().setBlock(0, 12, 2, GregTechAPI.sBlockCasings8, 0, 3);
        verifyStructure();
        require(machine.getRefiningTier() == 0, "Default core restored after native recipes");
    }

    private void verifyModePersistence() {
        for (int mode = 0; mode < 3; mode++) {
            machine.setMachineMode(mode);
            NBTTagCompound saved = new NBTTagCompound();
            base.writeToNBT(saved);
            World world = base.getWorld();
            world.removeTileEntity(0, 12, 0);
            BaseMetaTileEntity restored = new BaseMetaTileEntity();
            restored.setWorldObj(world);
            restored.readFromNBT(saved);
            world.setTileEntity(0, 12, 0, restored);
            base = restored;
            base.disableWorking();
            machine = (IntegratedChemicalPlant) base.getMetaTileEntity();
            require(machine.getMachineMode() == mode && machine.getRecipeMap() == map(mode), "Reload mode " + mode);
            verifyStructure();
        }
        for (int invalid : new int[] { -1, 3, Integer.MAX_VALUE }) {
            machine.setMachineMode(invalid);
            int mode = machine.getMachineMode();
            require(mode >= 0 && mode < 3 && machine.getRecipeMap() == map(mode), "Invalid mode " + invalid);
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            saved.setInteger("machineMode", invalid);
            machine.loadNBTData(saved);
            mode = machine.getMachineMode();
            require(mode >= 0 && mode < 3 && machine.getRecipeMap() == map(mode), "Invalid saved mode " + invalid);
        }
    }

    private void registerFixtures() {
        for (int mode = 0; mode < 3; mode++) {
            fixtureRecipe(mode, mode).metadata(COIL_HEAT, 1).metadata(FuelRefiningTier.INSTANCE, 0).addTo(map(mode));
            fixtureRecipe(10, mode).metadata(COIL_HEAT, 1).metadata(FuelRefiningTier.INSTANCE, 0).addTo(map(mode));
        }
        fixtureRecipe(11, 0).metadata(COIL_HEAT, machine.getHeatingCapacity() + 1)
            .metadata(FuelRefiningTier.INSTANCE, 0).addTo(map(0));
        fixtureRecipe(12, 0).metadata(COIL_HEAT, 1).metadata(FuelRefiningTier.INSTANCE, 1).addTo(map(0));
    }

    private GTRecipeBuilder fixtureRecipe(int input, int mode) {
        return GTRecipeBuilder.builder().itemInputs(stack(input, 1)).fluidInputs(water(250))
            .itemOutputs(mode == 1 ? new ItemStack[] { stack(21, 3) } : new ItemStack[0])
            .fluidOutputs(new FluidStack(FluidRegistry.LAVA, 500 + 100 * mode)).duration(100).eut(30);
    }

    private void verifyProcessing() {
        machine.setRecipeLocking(false);
        machine.setMachineMode(1);
        seed(0);
        require(!machine.checkProcessing().wasSuccessful(), "Wrong mode rejected");
        require(inputAmount() == 1 && inputFluidAmount() == 250, "Wrong mode preserves inputs");
        for (int mode = 0; mode < 3; mode++) {
            resetProcessing();
            machine.setMachineMode(mode);
            seed(mode);
            require(machine.checkProcessing().wasSuccessful(), "Processing mode " + mode);
            require(inputAmount() == 0 && inputFluidAmount() == 0, "Exact input consumption in mode " + mode);
            verifyFixtureOutputs(mode);
        }
    }

    private void verifyHeat() throws Exception {
        resetProcessing();
        machine.setMachineMode(0);
        seed(11);
        int lowHeat = machine.getHeatingCapacity();
        var result = machine.checkProcessing();
        require(!result.wasSuccessful() && result.getID().contains("heat"), "Low heat rejected: " + result.getID());
        require(inputAmount() == 1 && inputFluidAmount() == 250, "Low heat preserves inputs");
        setCoils(HeatingCoilLevel.MAX);
        verifyStructure();
        require(machine.getHeatingCapacity() > lowHeat, "Upgraded real coils increase heat");
        require(machine.checkProcessing().wasSuccessful(), "Higher heat permits the same recipe");
        verifyFixtureOutputs(0);
    }

    private void setCoils(HeatingCoilLevel level) throws Exception {
        Field shapeField = IntegratedChemicalPlant.class.getDeclaredField("SHAPE");
        shapeField.setAccessible(true);
        String[][] shape = (String[][]) shapeField.get(null);
        for (int y = 0; y < shape.length; y++) for (int z = 0; z < shape[y].length; z++)
            for (int x = 0; x < shape[y][z].length(); x++) if (shape[y][z].charAt(x) == 'H') base.getWorld()
                .setBlock(2 - x, 14 - y, z, GregTechAPI.sBlockCasings5, BlockCasings5.getMetaFromCoilHeat(level), 3);
    }

    private void verifyRefiningCores() {
        resetProcessing();
        machine.setMachineMode(0);
        seed(12);
        var result = machine.checkProcessing();
        require(!result.wasSuccessful() && result.getID().contains("tier"),
            "Base core rejects tier 1: " + result.getID());
        require(inputAmount() == 1 && inputFluidAmount() == 250, "Insufficient core tier preserves inputs");
        for (int tier = 1; tier <= 4; tier++) {
            base.getWorld().setBlock(0, 12, 2, coreBlock(tier), 0, 3);
            verifyStructure();
            require(machine.getRefiningTier() == tier, "Real refining core tier " + tier);
            if (tier == 1) {
                require(machine.checkProcessing().wasSuccessful(), "Tier 1 core permits the same recipe");
                verifyFixtureOutputs(0);
                resetProcessing();
            }
        }
        verifyInvalidRefiningCore(Blocks.air, "Missing core");
        base.getWorld().setBlock(0, 12, 2, coreBlock(4), 0, 3);
        verifyStructure();
        require(machine.getRefiningTier() == 4, "Tier 4 restored after removing the core");
        verifyInvalidRefiningCore(Blocks.stone, "Unsupported core block");
        base.getWorld().setBlock(0, 12, 2, coreBlock(4), 0, 3);
        verifyStructure();
        require(machine.getRefiningTier() == 4, "Tier 4 restored before downgrade");
        base.getWorld().setBlock(0, 12, 2, GregTechAPI.sBlockCasings8, 0, 3);
        verifyStructure();
        require(machine.getRefiningTier() == 0, "Default core restored after tier 4");
        seed(12);
        result = machine.checkProcessing();
        require(!result.wasSuccessful() && result.getID().contains("tier"),
            "Downgraded core rejects tier 1: " + result.getID());
        require(inputAmount() == 1 && inputFluidAmount() == 250, "Downgraded core preserves inputs");
        resetProcessing();
    }

    private void verifyInvalidRefiningCore(Block block, String description) {
        base.getWorld().setBlock(0, 12, 2, block, 0, 3);
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(!errors.isEmpty() && !machine.checkStructure(true, base), description + " rejects the structure");
        require(machine.getRefiningTier() < 0, description + " clears the previous core tier");
    }

    private static Block coreBlock(int tier) {
        return switch (tier) {
            case 1 -> Loaders.FRF_Coil_1;
            case 2 -> Loaders.FRF_Coil_2;
            case 3 -> Loaders.FRF_Coil_3;
            case 4 -> Loaders.FRF_Coil_4;
            default -> throw new AssertionError("Unexpected core tier " + tier);
        };
    }

    private void verifyRecipeLockSwitch() {
        resetProcessing();
        machine.setRecipeLocking(true);
        for (int mode : new int[] { 0, 1, 2, 0 }) {
            machine.setMachineMode(mode);
            require(machine.getSingleRecipeCheck() == null, "Mode switch clears the old recipe lock");
            seed(10);
            require(machine.checkProcessing().wasSuccessful(), "Recipe lock processing in mode " + mode);
            require(machine.getSingleRecipeCheck() != null, "Recipe lock established in mode " + mode);
            verifyFixtureOutputs(mode);
            resetProcessing();
        }
    }

    private void seed(int meta) {
        var bus = machine.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        bus.setInventorySlotContents(0, stack(meta, 1));
        machine.mInputHatches.get(0).mFluid = water(250);
    }

    private int inputAmount() {
        ItemStack input = machine.mInputBusses.get(0).getStackInSlot(0);
        return input == null ? 0 : input.stackSize;
    }

    private int inputFluidAmount() {
        FluidStack input = machine.mInputHatches.get(0).mFluid;
        return input == null ? 0 : input.amount;
    }

    private void resetProcessing() {
        machine.mMaxProgresstime = 0;
        machine.mProgresstime = 0;
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.lEUt = 0;
        if (!machine.mInputBusses.isEmpty()) {
            var bus = machine.mInputBusses.get(0);
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        }
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
    }

    private void verifyFixtureOutputs(int mode) {
        verifyOutputs(mode == 1 ? new ItemStack[] { stack(21, 3) } : new ItemStack[0],
            new FluidStack[] { new FluidStack(FluidRegistry.LAVA, 500 + 100 * mode) }, machine.mOutputItems,
            machine.mOutputFluids);
    }

    private static void verifyOutputs(ItemStack[] expectedItems, FluidStack[] expectedFluids, ItemStack[] actualItems,
        FluidStack[] actualFluids) {
        require(Arrays.equals(itemAmounts(expectedItems, expectedItems), itemAmounts(expectedItems, actualItems)),
            "Recipe item output quantities");
        require(Arrays.equals(itemAmounts(actualItems, expectedItems), itemAmounts(actualItems, actualItems)),
            "No extra item products");
        require(Arrays.equals(fluidAmounts(expectedFluids, expectedFluids), fluidAmounts(expectedFluids, actualFluids)),
            "Recipe fluid output quantities");
        require(Arrays.equals(fluidAmounts(actualFluids, expectedFluids), fluidAmounts(actualFluids, actualFluids)),
            "No extra fluid products");
    }

    private static long[] itemAmounts(ItemStack[] types, ItemStack[] stacks) {
        if (types == null) return new long[0];
        long[] amounts = new long[types.length];
        if (
            stacks != null
        ) for (int i = 0; i < types.length; i++) for (ItemStack stack : stacks)
            if (types[i] != null && stack != null && GTUtility.areStacksEqual(types[i], stack))
                amounts[i] += stack.stackSize;
        return amounts;
    }

    private static long[] fluidAmounts(FluidStack[] types, FluidStack[] stacks) {
        if (types == null) return new long[0];
        long[] amounts = new long[types.length];
        if (stacks != null) for (int i = 0; i < types.length; i++) for (FluidStack stack : stacks)
            if (types[i] != null && stack != null && types[i].isFluidEqual(stack)) amounts[i] += stack.amount;
        return amounts;
    }

    private static ItemStack[] copyItems(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(stack -> stack == null ? null : stack.copy()).toArray(ItemStack[]::new);
    }

    private static FluidStack[] copyFluids(FluidStack[] stacks) {
        return Arrays.stream(stacks).map(stack -> stack == null ? null : stack.copy()).toArray(FluidStack[]::new);
    }

    private ItemStack stack(int meta, int amount) {
        return new ItemStack(fixture, amount, meta);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static RecipeMap<?> map(int mode) {
        return switch (mode) {
            case 0 -> GTNGRecipeMaps.FuelRefiningRecipes;
            case 1 -> GTNGRecipeMaps.WoodDistillationRecipes;
            case 2 -> GTNGRecipeMaps.PetrochemicalRecipes;
            default -> throw new AssertionError("Unexpected mode " + mode);
        };
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !serverChecked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            require(++stageFrames <= 1200,
                "Visual stage timeout: opened=" + openedMode
                    + ", captured="
                    + capturedMode
                    + ", NEI="
                    + neiMode
                    + ", refining preview="
                    + refiningPreviewStage
                    + ", screen="
                    + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName()));
            if (refiningPreviewStage >= 0) {
                verifyRefiningPreview();
                return;
            }
            if (neiMode >= 0) {
                if (!(mc.currentScreen instanceof GuiRecipe) || ++frames < 60) return;
                capture("nei-mode-" + neiMode);
                frames = 0;
                stageFrames = 0;
                if (neiMode < 2) {
                    openNei(++neiMode);
                    return;
                }
                neiMode = -1;
                refiningPreviewStage = 0;
                return;
            }
            if (openedMode < 0) {
                if (structureCaptured) return;
                if (++frames < 40) return;
                capture("structure");
                structureCaptured = true;
                frames = 0;
                stageFrames = 0;
                requestedMode = 0;
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
            CycleButtonWidget button = findModeButton(wrapper.getScreen().getMainPanel());
            require(button != null, "Native GUI three-mode button");
            int expectedMode = capturedMode + 1;
            if (openedMode != expectedMode || button.getIntValue().getIntValue() != expectedMode) {
                frames = 0;
                return;
            }
            if (++frames < 40) return;
            frames = 0;
            capture("mode-" + openedMode);
            capturedMode = openedMode;
            stageFrames = 0;
            if (capturedMode < 2) {
                button.next();
                return;
            }
            neiMode = 0;
            GTMod.proxy.reloadNEICache();
            openNei(neiMode);
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void openNei(int mode) {
        String key = switch (mode) {
            case 0 -> "recipe.gtnotgood.fuel_refining";
            case 1 -> "recipe.gtnotgood.wood_distillation";
            case 2 -> "recipe.gtnotgood.petrochemical";
            default -> throw new AssertionError("Unexpected NEI mode " + mode);
        };
        require(GuiCraftingRecipe.openRecipeGui(key), "NEI recipe page " + key);
    }

    private void verifyRefiningPreview() throws Exception {
        if (refiningPreviewStage == 0) {
            IntegratedChemicalPlant controller = (IntegratedChemicalPlant) GregTechAPI.METATILEENTITIES[GTNGMachineID.IntegratedChemicalPlant.id];
            ConstructableData data = ConstructableData.getTierData(controller);
            if (!data.hasData()) return;
            require(data.getChannelMaxTierMap().getInt("refining") == 4, "NEI discovers refining tiers 1-4");
            GTNEIMultiblockHandler handler = new GTNEIMultiblockHandler();
            handler.loadCraftingRecipes(GTNGItemList.IntegratedChemicalPlant.get(1));
            require(handler.numRecipes() == 1, "Chemical plant structure preview selected");
            ArrayList<ICraftingHandler> handlers = new ArrayList<>();
            handlers.add(handler);
            var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
            constructor.setAccessible(true);
            Minecraft.getMinecraft().displayGuiScreen(constructor.newInstance(handlers));
            refiningPreviewStage = 1;
            frames = 0;
            stageFrames = 0;
            return;
        }
        GuiMultiblockHandler handler = MultiblockHandler.getCurrentGuiHandler();
        if (handler == null || ++frames < 60) return;
        GuiSlider refining = previewSlider(handler, "Refining");
        require(refining.getMaxValue() == 4, "NEI refining slider ends at tier 4");
        if (refiningPreviewStage == 1) {
            refining.setValue(1);
            previewSlider(handler, I18n.format("blockrenderer6343.nei.layer")).setValue(2);
            refiningPreviewStage = 2;
        } else {
            int tier = refiningPreviewStage - 1;
            verifyPreviewCore(handler, tier);
            capture("refining-" + tier);
            if (tier == 2) {
                checks.append("NEI refining channel maximum 4; displayed tiers 1 and 2 construct matching cores.\n");
                finish();
                return;
            }
            refining.setValue(2);
            refiningPreviewStage = 3;
        }
        frames = 0;
        stageFrames = 0;
    }

    private static GuiSlider previewSlider(GuiMultiblockHandler handler, String name) throws Exception {
        Field buttons = GuiMultiblockHandler.class.getDeclaredField("allButtons");
        buttons.setAccessible(true);
        for (Object button : (List<?>) buttons.get(handler)) {
            if (button instanceof GuiSlider slider && slider.name.equals(name)) return slider;
        }
        throw new AssertionError("Missing NEI slider " + name);
    }

    private static void verifyPreviewCore(GuiMultiblockHandler handler, int tier) throws Exception {
        Field triggerField = GuiMultiblockHandler.class.getDeclaredField("trigger");
        triggerField.setAccessible(true);
        ItemStack trigger = (ItemStack) triggerField.get(handler);
        require(ChannelDataAccessor.hasSubChannel(trigger, "refining")
            && ChannelDataAccessor.getChannelData(trigger, "refining") == tier, "Displayed refining tier " + tier);
        Field rendererField = GuiMultiblockHandler.class.getDeclaredField("renderer");
        rendererField.setAccessible(true);
        World world = ((WorldSceneRenderer) rendererField.get(null)).world;
        var origin = GuiMultiblockHandler.MB_PLACE_POS;
        BaseMetaTileEntity previewBase = (BaseMetaTileEntity) world.getTileEntity(origin.x, origin.y, origin.z);
        require(previewBase != null && previewBase.getMetaTileEntity() instanceof IntegratedChemicalPlant,
            "Chemical plant controller in the preview world");
        ForgeDirection inward = previewBase.getFrontFacing().getOpposite();
        int x = origin.x + inward.offsetX * 2;
        int y = origin.y + inward.offsetY * 2;
        int z = origin.z + inward.offsetZ * 2;
        require(world.getBlock(x, y, z) == coreBlock(tier) && world.getBlockMetadata(x, y, z) == 0,
            "NEI refining " + tier + " constructs the matching recipe core");
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(),
            ("PASS\nGTNL absent; native recipe counts=" + Arrays.toString(nativeCounts)
                + "; GUI packet round trips=2; GUI modes=3; NEI maps=3.\n"
                + checks).getBytes(StandardCharsets.UTF_8));
        System.out.println("CHEMICAL_PLANT_QA_PASS " + checks);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static CycleButtonWidget findModeButton(IWidget widget) {
        if (widget instanceof CycleButtonWidget button && button.getStateCount() == 3) return button;
        for (IWidget child : widget.getChildren()) {
            CycleButtonWidget button = findModeButton(child);
            if (button != null) return button;
        }
        return null;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.chemicalPlant.output"));
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
