package com.xyp.gtnotgood.qa;

import static gregtech.api.util.GTRecipeConstants.CLEANROOM;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
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
import com.xyp.gtnotgood.client.render.KerrNewmanBlackHoleRenderer;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.KerrNewmanHomogenizer;
import com.xyp.gtnotgood.common.machines.multiblock.structure.KerrNewmanHomogenizerStructure;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "kerrnewmanqa",
    name = "Kerr-Newman QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class KerrNewmanClientChecks {

    private Item fixture;
    private BaseMetaTileEntity base;
    private volatile KerrNewmanHomogenizer machine;
    private final String[][] shape = KerrNewmanHomogenizerStructure.shape();
    private final List<int[]> hatchPositions = new ArrayList<>();
    private final List<int[]> coilPositions = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private Map<Character, ItemStack> built = new HashMap<>();
    private boolean started;
    private volatile boolean checked, finished, requestGui, openedGui;
    private volatile int requestedState = -1, appliedState = -1, requestedView = -1, shownView = -1;
    private int clientTicks, serverTicks, frames, stageFrames, capturedView = -1, visualStage, tooltipStage = -1;
    private int[] broken;
    private Block brokenBlock;
    private int brokenMeta;
    private boolean glChecked;
    private boolean shrinkingCaptured;
    private MultiblockTooltipBuilder currentTooltip;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.kerrNewman.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("kerrNewmanFixture");
        GameRegistry.registerItem(fixture, "fixture");
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == 'K') hatchPositions.add(new int[] { col, row, depth });
                else if (shape[row][depth].charAt(col) == 'T' || shape[row][depth].charAt(col) == '^')
                    coilPositions.add(new int[] { col, row, depth, shape[row][depth].charAt(col) });
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
                mc.launchIntegratedServer("kerr-newman-qa-" + System.currentTimeMillis(), "Kerr-Newman QA",
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
                require(
                    GTNGItemList.KerrNewmanHomogenizer.get(1).getItemDamage() == GTNGMachineID.KerrNewmanHomogenizer.id,
                    "Registered controller identity");
                player.worldObj.setWorldTime(6000);
                buildRig(player, 0, 90, 0, ForgeDirection.NORTH);
            }
            if (serverTicks == 60) {
                verifyStructure();
                verifyCoilFormation();
                verifyDirections(player);
                verifyRequiredBlocks();
                verifySurvivalRecipes();
                verifyNativeMixing();
                registerFixtures();
                verifyParallelAndFluids();
                verifyVoltageGate();
                verifyOutputProtection();
                verifyPersistence(stack(2, 1));
                applyState(0);
                checked = true;
                requestedView = 0;
            }
            if (checked && requestedView >= 0) {
                int view = requestedView;
                requestedView = -1;
                player.closeScreen();
                if (view == 0) player.playerNetServerHandler.setPlayerLocation(-68, 130, -60, -40, 24);
                else if (view == 1) player.playerNetServerHandler.setPlayerLocation(-13, 100, 15, -35, 18);
                else player.playerNetServerHandler.setPlayerLocation(-7, 87, 17, -25, -9);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                shownView = view;
            }
            if (checked && requestedState >= 0) {
                int state = requestedState;
                requestedState = -1;
                applyState(state);
            }
            if (checked && (appliedState == 0 || appliedState == 2 || appliedState == 4)) {
                for (var hatch : machine.mEnergyHatches) {
                    BaseMetaTileEntity energy = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
                    energy.setStoredEU(energy.getEUCapacity());
                }
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
        base = place(world, x, y, z, GTNGMachineID.KerrNewmanHomogenizer.id);
        base.setFrontFacing(facing);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        machine = (KerrNewmanHomogenizer) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        verifyConstructedGeometry();
        placeHatch(0, ItemList.Hatch_Input_Bus_EV.get(1));
        placeHatch(1, ItemList.Hatch_Output_Bus_EV.get(1));
        placeHatch(2, ItemList.Hatch_Maintenance.get(1));
        placeHatch(3, ItemList.Hatch_Energy_EV.get(1));
        placeHatch(4, ItemList.Hatch_Input_UV.get(1));
        placeHatch(5, ItemList.Hatch_Output_UV.get(1));
        machine.setAlwaysMaxParallel(false);
        machine.setBatchMode(false);
        machine.setPowerPanelMaxParallel(1);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
    }

    private void verifyConstructedGeometry() {
        built = new HashMap<>();
        int total = 0;
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++) {
                char symbol = shape[row][depth].charAt(col);
                if (symbol == ' ' || symbol == '~') continue;
                int[] pos = position(col, row, depth);
                Block block = base.getWorld().getBlock(pos[0], pos[1], pos[2]);
                require(block != Blocks.air, "Built block " + symbol + " at " + Arrays.toString(pos));
                ItemStack stack = new ItemStack(block, 1, base.getWorld().getBlockMetadata(pos[0], pos[1], pos[2]));
                ItemStack expected = built.putIfAbsent(symbol, stack);
                require(expected == null || GTUtility.areStacksEqual(stack, expected), "Uniform block " + symbol);
                total++;
            }
        require(total == 15637 && built.size() == 33, "All 15637 source structure blocks are built");
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Structure: " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Physical 83x36x83 structure forms");
        require(machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1
            && machine.mInputHatches.size() == 1
            && machine.mOutputHatches.size() == 1
            && machine.mMaintenanceHatches.size() == 1
            && !machine.mEnergyHatches.isEmpty(), "All physical item/fluid/energy/maintenance hatches discovered");
        verifyBounds();
    }

    private void verifyBounds() {
        ForgeDirection back = base.getFrontFacing().getOpposite();
        Vec3Impl offset = machine.getBlackHoleOffset();
        require(offset.get0() == back.offsetX * 34 && offset.get1() == 0 && offset.get2() == back.offsetZ * 34,
            "Black hole center is 34 blocks behind controller " + base.getFrontFacing());
        double x = base.getXCoord() + offset.get0() + 0.5;
        double y = base.getYCoord() + 0.5;
        double z = base.getZCoord() + offset.get2() + 0.5;
        AxisAlignedBB bounds = machine.getRenderBoundingBox(base.getXCoord(), base.getYCoord(), base.getZCoord());
        require(bounds.minX <= x - 4.5 && bounds.maxX >= x + 4.5
            && bounds.minY <= y - 4.5
            && bounds.maxY >= y + 4.5
            && bounds.minZ <= z - 4.5
            && bounds.maxZ >= z + 4.5, "Native culling bounds include complete event horizon");
        require(machine.getMaxRenderDistanceSquared() >= 65536, "Native render range reaches 256 blocks");
    }

    private void verifyDirections(EntityPlayerMP player) throws Exception {
        BaseMetaTileEntity originalBase = base;
        KerrNewmanHomogenizer original = machine;
        Map<Character, ItemStack> originalBlocks = built;
        for (ForgeDirection direction : new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST }) {
            buildRig(player, 192, 90, 192, direction);
            verifyStructure();
            machine.mMaxProgresstime = 100;
            base.setActive(true);
            updateBlackHole();
            require(coilsMatch(base.getWorld(), true), "Disposable controller activates both coil classes");
            machine.onRemoval();
            require(coilsMatch(base.getWorld(), false), "Controller removal clears all 1078 active coils");
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
                for (int col = 0; col < shape[row][depth].length(); col++) {
                    if (shape[row][depth].charAt(col) == ' ') continue;
                    int[] pos = position(col, row, depth);
                    base.getWorld().setBlockToAir(pos[0], pos[1], pos[2]);
                }
        }
        base = originalBase;
        machine = original;
        built = originalBlocks;
        verifyStructure();
        report.append("Exact original structure forms in four directions; native bounds and core centers rotate.\n");
    }

    private void verifyCoilFormation() {
        require(coilPositions.size() == 1078, "Original 512 accelerator and 566 fusion coil positions");
        for (int[] local : coilPositions) {
            int[] pos = position(local[0], local[1], local[2]);
            coil(local[3]).getBlock().setActive(base.getWorld(), pos[0], pos[1], pos[2], true);
        }
        require(coilsMatch(base.getWorld(), true), "Both coil classes accept active metadata 11/12");
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(), "Active coil metadata remains valid for structure checking");
        for (int[] local : coilPositions) {
            int[] pos = position(local[0], local[1], local[2]);
            coil(local[3]).getBlock().setActive(base.getWorld(), pos[0], pos[1], pos[2], false);
        }
        verifyStructure();
        report.append("All 1078 coils accept active metadata and are cleared on controller removal.\n");
    }

    private static KerrNewmanCasing coil(int symbol) {
        return symbol == 'T' ? KerrNewmanCasing.AcceleratorCoilUhv : KerrNewmanCasing.AdvancedFusionCoil;
    }

    private boolean coilsMatch(World world, boolean active) {
        for (int[] local : coilPositions) {
            int[] pos = position(local[0], local[1], local[2]);
            KerrNewmanCasing expected = coil(local[3]);
            int metadata = expected.getMetadata() | (active ? KerrNewmanCasing.ACTIVE_BIT : 0);
            if (
                world.getBlock(pos[0], pos[1], pos[2]) != expected.getBlock()
                    || world.getBlockMetadata(pos[0], pos[1], pos[2]) != metadata
            ) return false;
        }
        return true;
    }

    private void verifyRequiredBlocks() {
        for (char symbol : built.keySet()) {
            if (symbol == 'K') continue;
            int[] pos = firstPosition(symbol);
            World world = base.getWorld();
            Block block = world.getBlock(pos[0], pos[1], pos[2]);
            int meta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
            world.setBlockToAir(pos[0], pos[1], pos[2]);
            require(!machine.checkStructure(true, base), "Missing component " + symbol + " rejects formation");
            world.setBlock(pos[0], pos[1], pos[2], block, meta, 3);
            verifyStructure();
        }
        report.append("Every fixed source component rejects missing blocks and recovers after repair.\n");
    }

    private void verifySurvivalRecipes() {
        var recipes = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, GTNGItemList.KerrNewmanHomogenizer.get(1)))
            .collect(Collectors.toList());
        require(recipes.size() == 1, "One native controller assembly-line recipe");
        var controller = recipes.get(0);
        require(
            controller.mInputs.length == 9 && controller.mFluidInputs.length == 3
                && controller.mDuration == 800
                && controller.mEUt == 2097152
                && controller.mResearchTime == 2400
                && controller.mResearchVoltage == TierEU.RECIPE_UEV,
            "Original controller input lanes, amounts and gates");
        int[] amounts = { 16, 16, 16, 16, 8, 8, 32, 32, 32 };
        validateIngredients(controller.mInputs, controller.mFluidInputs);
        for (int index = 0; index < amounts.length; index++)
            require(controller.mInputs[index].stackSize == amounts[index], "Controller lane amount " + index);
        for (FluidStack input : controller.mFluidInputs) require(input.amount == 1000, "Original 1000mB fluid lane");
        require(GTUtility.areStacksEqual(controller.mInputs[8], MaterialsAlloy.QUANTUM.getPlate(1)),
            "Quantanium form adapts to obtainable native Quantum plate");
        require(GTUtility.areStacksEqual(controller.mResearchItem, GregtechItemList.Industrial_Mixer.get(1)),
            "Native industrial mixer remains the research predecessor");
        require(
            controller.mFluidInputs[1].isFluidEqual(MaterialsAlloy.TITANSTEEL.getFluidStack(1))
                && controller.mFluidInputs[2].isFluidEqual(MaterialsAlloy.ABYSSAL.getFluidStack(1)),
            "Missing GTO alloys adapt to obtainable native Titansteel and Abyssal Alloy");
        for (KerrNewmanCasing casing : KerrNewmanCasing.values()) {
            List<GTRecipe> matches = Arrays
                .asList(RecipeMaps.assemblerRecipes, RecipeMaps.arcFurnaceRecipes,
                    GoodGeneratorRecipeMaps.preciseAssemblerRecipes)
                .stream().flatMap(map -> map.getAllRecipes().stream())
                .filter(recipe -> Arrays.stream(recipe.mOutputs)
                    .anyMatch(output -> output != null && GTUtility.areStacksEqual(output, casing.getStack(1))))
                .collect(Collectors.toList());
            var assemblyLines = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, casing.getStack(1)))
                .collect(Collectors.toList());
            var crafting = CraftingManager.getInstance().getRecipeList().stream()
                .filter(
                    recipe -> recipe != null && GTUtility.areStacksEqual(recipe.getRecipeOutput(), casing.getStack(1)))
                .collect(Collectors.toList());
            require(!matches.isEmpty() || !assemblyLines.isEmpty() || !crafting.isEmpty(),
                "Native survival recipe " + casing);
            for (GTRecipe recipe : matches) {
                validateIngredients(recipe.mInputs, recipe.mFluidInputs);
                require(recipe.mInputs.length > 0, "Complete assembler survival inputs " + casing);
            }
            if (casing == KerrNewmanCasing.AdvancedFusionCoil) {
                require(matches.size() == 1 && assemblyLines.isEmpty(),
                    "Advanced fusion coil uses native nine-slot assembler facility");
                GTRecipe recipe = matches.get(0);
                require(
                    RecipeMaps.assemblerRecipes.getAllRecipes().contains(recipe) && recipe.mInputs.length == 8
                        && recipe.mFluidInputs.length == 1
                        && recipe.mDuration == 100
                        && recipe.mEUt == 491520
                        && recipe.getMetadataOrDefault(CLEANROOM, false),
                    "Advanced fusion coil preserves original eight inputs, cost and cleanroom gate");
            }
            if (casing == KerrNewmanCasing.BioactiveCasing) {
                require(matches.size() == 1, "One native precise bioactive recipe");
                GTRecipe recipe = matches.get(0);
                require(
                    GoodGeneratorRecipeMaps.preciseAssemblerRecipes.getAllRecipes().contains(recipe)
                        && recipe.mInputs.length == 4
                        && recipe.mFluidInputs.length == 4
                        && recipe.mDuration == 200
                        && recipe.mEUt == TierEU.RECIPE_UV
                        && recipe.mSpecialValue == 0,
                    "Bioactive casing preserves native precise facility with complete flattened predecessor cost");
            }
            for (var recipe : assemblyLines) {
                validateIngredients(recipe.mInputs, recipe.mFluidInputs);
                require(recipe.mInputs.length > 0, "Complete assembly-line survival inputs " + casing);
            }
            if (casing == KerrNewmanCasing.AmprosiumPipeCasing) {
                require(crafting.size() == 1 && crafting.get(0) instanceof ShapedRecipes,
                    "Amprosium pipe preserves native 3x3 crafting recipe");
                ShapedRecipes recipe = (ShapedRecipes) crafting.get(0);
                require(recipe.recipeWidth == 3 && recipe.recipeHeight == 3 && recipe.recipeItems.length == 9,
                    "Amprosium pipe preserves ABA/BCB/ABA ingredient grid");
                validateIngredients(recipe.recipeItems, new FluidStack[0]);
            }
        }
        report
            .append("Controller 9 item / 3 fluid lanes preserve original quantities; all 23 casings are craftable.\n");
    }

    private static void validateIngredients(ItemStack[] items, FluidStack[] fluids) {
        require(items != null && fluids != null, "Survival ingredient arrays exist");
        for (int index = 0; index < items.length; index++) {
            ItemStack input = items[index];
            require(input != null && input.getItem() != null && input.stackSize >= 0,
                "Unavailable item lane " + index + ": " + input);
            String name = input.getDisplayName();
            require(name != null && !name.contains("Unnamed")
                && !name.contains("unnamed")
                && !name.contains(".name")
                && !name.contains("null"), "Unavailable runtime item lane " + index + ": " + name + " " + input);
        }
        for (int index = 0; index < fluids.length; index++)
            require(fluids[index] != null && fluids[index].getFluid() != null && fluids[index].amount > 0,
                "Unavailable fluid lane " + index + ": " + fluids[index]);
    }

    private void verifyNativeMixing() {
        require(machine.getRecipeMap() == RecipeMaps.mixerNonCellRecipes, "Native non-cell mixer map identity");
        GTRecipe recipe = machine.getRecipeMap().getAllRecipes().stream()
            .filter(candidate -> candidate.mEUt > 0 && candidate.mEUt <= 1920
                && candidate.mInputs.length > 0
                && candidate.mInputs.length <= 8
                && candidate.mOutputs.length > 0
                && candidate.mFluidInputs.length == 1
                && candidate.mSpecialValue >= 0
                && guaranteed(candidate.mInputChances)
                && guaranteed(candidate.mOutputChances)
                && guaranteed(candidate.mFluidInputChances))
            .findFirst().orElseThrow(() -> new AssertionError("Native item + fluid mixer recipe"));
        resetProcessing();
        ItemStack[] inputs = Arrays.stream(recipe.mInputs).map(ItemStack::copy).toArray(ItemStack[]::new);
        for (ItemStack input : inputs) if (input.stackSize == 0) input.stackSize = 1;
        seed(inputs);
        machine.mInputHatches.get(0).mFluid = recipe.mFluidInputs[0].copy();
        require(machine.checkProcessing().wasSuccessful(), "Real native mixer recipe starts");
        for (ItemStack input : recipe.mInputs) require(inputCount(input) == (input.stackSize == 0 ? 1 : 0),
            "Native mixer item consumption preserves nonconsumed circuit");
        for (ItemStack output : recipe.mOutputs)
            require(outputCount(output) == output.stackSize, "Native mixer output amount");
        require(machine.mInputHatches.get(0).mFluid == null || machine.mInputHatches.get(0).mFluid.amount == 0,
            "Native mixer exact fluid consumption");
        report.append("A real native mixer item/fluid recipe runs with exact consumption and catalyst retention.\n");
    }

    private void registerFixtures() {
        GTRecipeBuilder.builder().itemInputs(stack(0, 1), stack(1, 0)).itemOutputs(stack(2, 1))
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 100)).fluidOutputs(new FluidStack(FluidRegistry.LAVA, 50))
            .duration(4800).eut(30).addTo(machine.getRecipeMap());
        GTRecipeBuilder.builder().itemInputs(stack(3, 1)).itemOutputs(stack(4, 1)).duration(80).eut(7680)
            .addTo(machine.getRecipeMap());
        GTRecipeBuilder.builder().itemInputs(stack(10, 1)).itemOutputs(stack(11, 1)).duration(100000000).eut(1)
            .addTo(machine.getRecipeMap());
    }

    private void verifyParallelAndFluids() {
        placeHatch(3, ItemList.Hatch_Energy_LV.get(1));
        verifyStructure();
        resetProcessing();
        machine.setPowerPanelMaxParallel(8);
        seed(stack(0, 8), stack(1, 1));
        machine.mInputHatches.get(0).mFluid = new FluidStack(FluidRegistry.WATER, 800);
        require(machine.checkProcessing().wasSuccessful(), "Budget-limited parallel mixer starts");
        int parallel = outputCount(stack(2, 1));
        String lv = processingDiagnostics("LV parallel fixture");
        require(
            parallel > 0 && parallel < 8
                && inputCount(stack(0, 1)) == 8 - parallel
                && inputCount(stack(1, 1)) == 1
                && machine.mInputHatches.get(0).mFluid.amount == 800 - parallel * 100,
            "Native energy budget limits parallel and consumes exact items and fluids");
        require(
            machine.mOutputFluids.length == 1 && machine.mOutputFluids[0].amount == parallel * 50
                && Math.abs(machine.lEUt) <= machine.getMaxInputEu(),
            "Fluid output and EU respect physical hatch budget");
        require(machine.mMaxProgresstime == 80 && Math.abs(machine.lEUt) == 30L * parallel,
            "6000% base speed divides 4800 ticks by 60 without an EU/t discount: " + lv);
        machine.setPowerPanelMaxParallel(1);
        placeHatch(3, ItemList.Hatch_Energy_MV.get(1));
        verifyStructure();
        resetProcessing();
        seed(stack(0, 1), stack(1, 1));
        machine.mInputHatches.get(0).mFluid = new FluidStack(FluidRegistry.WATER, 100);
        boolean successful = machine.checkProcessing().wasSuccessful();
        String mv = processingDiagnostics("MV single fixture");
        require(successful && machine.mMaxProgresstime == 20 && Math.abs(machine.lEUt) == 120,
            "One native perfect overclock uses 4x EU and one-quarter duration after the 60x speed bonus: " + mv);
        placeHatch(3, ItemList.Hatch_Energy_EV.get(1));
        verifyStructure();
        report.append("Base speed is 6000% (60x) with unchanged EU/t; perfect OC is 4x EU / 4x speed.\n");
    }

    private String processingDiagnostics(String label) {
        StringBuilder text = new StringBuilder(label).append(": duration=").append(machine.mMaxProgresstime)
            .append(", eut=").append(machine.lEUt).append(", parallelOutputs=").append(outputCount(stack(2, 1)))
            .append(", remainingItems=").append(inputCount(stack(0, 1))).append(", remainingCatalyst=")
            .append(inputCount(stack(1, 1))).append(", maxInputEUt=").append(machine.getMaxInputEu()).append(", batch=")
            .append(machine.isBatchModeEnabled()).append(", energyHatches=").append(machine.mEnergyHatches.size())
            .append(", exoticHatches=").append(machine.getExoticEnergyHatches().size());
        for (var hatch : machine.mEnergyHatches) {
            var tile = hatch.getBaseMetaTileEntity();
            text.append("; hatch=").append(hatch.getStackForm(1)).append(" @").append(tile.getXCoord()).append(',')
                .append(tile.getYCoord()).append(',').append(tile.getZCoord()).append(" voltage=")
                .append(tile.getInputVoltage()).append(" amperage=").append(tile.getInputAmperage());
        }
        for (GTRecipe recipe : machine.getRecipeMap().getAllRecipes()) if (
            Arrays.stream(recipe.mInputs)
                .anyMatch(input -> input != null && GTUtility.areStacksEqual(input, stack(0, 1)))
        ) text.append("; registeredFixture duration=").append(recipe.mDuration).append(" eut=").append(recipe.mEUt)
            .append(" inputs=").append(Arrays.toString(recipe.mInputs));
        String value = text.toString();
        report.append(value).append('\n');
        System.out.println("KERR_NEWMAN_PROCESSING " + value);
        try {
            Files.write(new File(output(), "processing-diagnostics.txt").toPath(),
                (value + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception failure) {
            throw new AssertionError("Cannot write processing diagnostics", failure);
        }
        return value;
    }

    private void verifyVoltageGate() {
        placeHatch(6, ItemList.Hatch_Energy_EV.get(1));
        verifyStructure();
        resetProcessing();
        seed(stack(3, 1));
        require(machine.getMaxInputEu() >= 7680 && !machine.checkProcessing().wasSuccessful()
            && inputCount(stack(3, 1)) == 1, "Aggregate EV amperage cannot bypass IV recipe voltage");
        placeHatch(3, ItemList.Hatch_Energy_IV.get(1));
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(4, 1)) == 1,
            "A physical IV hatch unlocks the unchanged recipe");
        int[] local = hatchPositions.get(6);
        int[] pos = position(local[0], local[1], local[2]);
        ItemStack casing = built.get('K');
        base.getWorld().setBlock(pos[0], pos[1], pos[2], Block.getBlockFromItem(casing.getItem()),
            casing.getItemDamage(), 3);
        placeHatch(3, ItemList.Hatch_Energy_EV.get(1));
        verifyStructure();
        report.append("Two lower-voltage hatches cannot bypass the recipe tier; a real hatch upgrade succeeds.\n");
    }

    private void verifyOutputProtection() {
        resetProcessing();
        var bus = machine.mOutputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        seed(stack(0, 1), stack(1, 1));
        machine.mInputHatches.get(0).mFluid = new FluidStack(FluidRegistry.WATER, 100);
        require(
            !machine.checkProcessing().wasSuccessful() && inputCount(stack(0, 1)) == 1
                && machine.mInputHatches.get(0).mFluid.amount == 100,
            "Protected output rejects without consuming inputs");
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        require(machine.checkProcessing().wasSuccessful() && outputCount(stack(2, 1)) == 1,
            "Free output storage permits the same item/fluid operation");
        report.append("Output protection rejects atomically and permits retry after storage is freed.\n");
    }

    private void applyState(int state) throws Exception {
        if (state == 0 || state == 2 || state == 4) {
            if (brokenBlock != null) {
                base.getWorld().setBlock(broken[0], broken[1], broken[2], brokenBlock, brokenMeta, 3);
                brokenBlock = null;
                verifyStructure();
            }
            resetProcessing();
            seed(stack(10, 1));
            machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
            machine.mSolderingTool = machine.mCrowbar = true;
            machine.mEfficiency = 10000;
            require(machine.checkProcessing().wasSuccessful(), "Actual operation starts for black hole rendering");
            base.enableWorking();
            base.setActive(true);
        } else if (state == 1 || state == 5) {
            base.disableWorking();
            machine.stopMachine(ShutDownReasonRegistry.NONE);
            base.setActive(false);
            if (state == 5) cleanupFixtures();
        } else if (state == 3) {
            broken = firstPosition('A');
            brokenBlock = base.getWorld().getBlock(broken[0], broken[1], broken[2]);
            brokenMeta = base.getWorld().getBlockMetadata(broken[0], broken[1], broken[2]);
            base.getWorld().setBlockToAir(broken[0], broken[1], broken[2]);
            require(!machine.checkStructure(true, base), "Broken working structure becomes unformed");
            base.disableWorking();
        }
        updateBlackHole();
        require(machine.isBlackHoleWorking() == (state == 0 || state == 2 || state == 4),
            "Server working/idle/broken gate " + state);
        require(coilsMatch(base.getWorld(), state == 0 || state == 2 || state == 4),
            "Server updates both complete coil groups for state " + state);
        appliedState = state;
    }

    private void updateBlackHole() throws Exception {
        Method update = KerrNewmanHomogenizer.class.getDeclaredMethod("updateBlackHole", IGregTechTileEntity.class);
        update.setAccessible(true);
        update.invoke(machine, base);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            require(++stageFrames <= 2400, "Visual stage timeout");
            Minecraft mc = Minecraft.getMinecraft();
            if (tooltipStage >= 0) {
                if (!(mc.currentScreen instanceof TooltipProbeGui) || ++frames < 40) return;
                capture("tooltip-" + (tooltipStage < 2 ? "zh_CN" : "en_US")
                    + (tooltipStage % 2 == 0 ? "-normal" : "-structure"));
                frames = stageFrames = 0;
                if (++tooltipStage == 4) {
                    finish();
                    return;
                }
                showTooltip();
                return;
            }
            if (openedGui) {
                if (!(mc.currentScreen instanceof GuiContainerWrapper) || ++frames < 60) return;
                capture("controller-gui");
                tooltipStage = 0;
                frames = stageFrames = 0;
                showTooltip();
                return;
            }
            KerrNewmanHomogenizer client = clientMachine();
            if (client == null) {
                frames = 0;
                return;
            }
            if (shownView < 2) {
                if (shownView != capturedView + 1 || requestedView >= 0 || !client.isBlackHoleWorking()) return;
                if (++frames < 150) return;
                capture(shownView == 0 ? "structure-overview" : "structure-textures");
                capturedView = shownView;
                requestedView = shownView + 1;
                frames = stageFrames = 0;
                return;
            }
            boolean working = visualStage == 0 || visualStage == 2 || visualStage == 4;
            if (
                requestedState >= 0 || appliedState != visualStage
                    || client.isBlackHoleWorking() != working
                    || !coilsMatch(mc.theWorld, working)
            ) {
                frames = 0;
                return;
            }
            ++frames;
            if (!working && !shrinkingCaptured && frames >= 60) {
                capture(visualStage == 1 ? "black-hole-stopping" : "black-hole-breaking");
                shrinkingCaptured = true;
            }
            if (frames < 240) return;
            verifyStream();
            if (!glChecked && working) {
                verifyGlState(client);
                return;
            }
            require(KerrNewmanBlackHoleRenderer.isDistortionAvailable(), "Scene-refraction shader compiled in client");
            String[] labels = { "black-hole-working", "black-hole-stopped", "black-hole-restarted", "black-hole-broken",
                "black-hole-repaired", "black-hole-restored-idle" };
            capture(labels[visualStage]);
            report.append("Actual native client state ").append(labels[visualStage]).append(" passed.\n");
            frames = stageFrames = 0;
            shrinkingCaptured = false;
            if (++visualStage < labels.length) requestedState = visualStage;
            else requestGui = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private KerrNewmanHomogenizer clientMachine() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return null;
        TileEntity tile = mc.theWorld.getTileEntity(base.getXCoord(), base.getYCoord(), base.getZCoord());
        return tile instanceof BaseMetaTileEntity client
            && client.getMetaTileEntity() instanceof KerrNewmanHomogenizer controller ? controller : null;
    }

    private void verifyStream() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            machine.writeToStream(buffer);
            BaseMetaTileEntity clientBase = new BaseMetaTileEntity();
            clientBase.setWorldObj(Minecraft.getMinecraft().theWorld);
            clientBase.setInitialValuesAsNBT(null, (short) GTNGMachineID.KerrNewmanHomogenizer.id);
            KerrNewmanHomogenizer copy = (KerrNewmanHomogenizer) clientBase.getMetaTileEntity();
            copy.readFromStream(buffer);
            require(
                !buffer.isReadable() && copy.isBlackHoleWorking() == machine.isBlackHoleWorking()
                    && copy.getBlackHoleOffset().equals(machine.getBlackHoleOffset()),
                "Native stream roundtrip preserves alignment and working state");
        } finally {
            buffer.release();
        }
    }

    private void verifyGlState(KerrNewmanHomogenizer client) throws Exception {
        Field statesField = KerrNewmanBlackHoleRenderer.class.getDeclaredField("ANIMATIONS");
        statesField.setAccessible(true);
        Object state = ((Map<?, ?>) statesField.get(null)).get(client);
        require(state != null, "Actual TESR created black hole animation state");
        Class<?> submission = Class.forName(KerrNewmanBlackHoleRenderer.class.getName() + "$DrawSubmission");
        var constructor = submission.getDeclaredConstructor(World.class, float.class);
        constructor.setAccessible(true);
        Method draw = KerrNewmanBlackHoleRenderer.class.getDeclaredMethod("draw", Object.class);
        draw.setAccessible(true);
        int previousMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glFrustum(-1.4, 1.4, -1, 1, 1, 200);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslated(0, 0, -20);
        try {
            List<String> before = glState();
            draw.invoke(null, constructor.newInstance(Minecraft.getMinecraft().theWorld, 1F));
            require(before.equals(glState()), "Deferred renderer restores GL matrices, buffers, programs and flags");
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(previousMode);
        }
        glChecked = true;
        report.append("Deferred black hole draw restores sampled GL state after native WorldLast completion.\n");
    }

    private void verifyPersistence(ItemStack expectedOutput) {
        require(machine.mMaxProgresstime > 0, "Save a running mixing operation");
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
        machine = (KerrNewmanHomogenizer) base.getMetaTileEntity();
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
                new int[] { col - KerrNewmanHomogenizerStructure.OFFSET_X,
                    row - KerrNewmanHomogenizerStructure.OFFSET_Y, depth - KerrNewmanHomogenizerStructure.OFFSET_Z },
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
        Method create = KerrNewmanHomogenizer.class.getDeclaredMethod("createTooltip");
        create.setAccessible(true);
        MultiblockTooltipBuilder tooltip = (MultiblockTooltipBuilder) create.invoke(machine);
        String text = String.join("\n", tooltip.getInformation()) + "\n\n"
            + String.join("\n", tooltip.getStructureInformation());
        require(!text.contains("gtng.") && !text.contains("gt.mbtt.") && !text.contains("gt.blockmachines."),
            "All tooltip translations resolve: " + locale);
        require(text.contains("83"), "Tooltip reports original structure dimensions: " + locale);
        List<String> information = Arrays.asList(tooltip.getInformation());
        MultiblockTooltipBuilder expected = new MultiblockTooltipBuilder().addStaticSpeedInfo(60F).addPerfectOCInfo();
        expected.toolTipFinisher();
        for (String performance : Arrays.copyOf(expected.getInformation(), 2))
            require(information.contains(performance),
                "Native 6000% speed and perfect-overclock tooltip: " + locale + " / " + performance);
        Files.write(new File(output(), "tooltip-" + locale + ".txt").toPath(), text.getBytes(StandardCharsets.UTF_8));
        return tooltip;
    }

    private void showTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = 1;
        if (tooltipStage % 2 == 0) currentTooltip = verifyTooltips(tooltipStage < 2 ? "zh_CN" : "en_US");
        List<String> lines = new ArrayList<>();
        lines.add(GTNGItemList.KerrNewmanHomogenizer.get(1).getDisplayName());
        lines.addAll(Arrays.asList(
            tooltipStage % 2 == 0 ? currentTooltip.getInformation() : currentTooltip.getStructureInformation()));
        mc.displayGuiScreen(new TooltipProbeGui(lines));
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
        System.out.println("KERR_NEWMAN_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.kerrNewman.output"));
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
}
