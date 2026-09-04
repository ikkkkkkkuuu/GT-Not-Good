package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_FACTORY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_FACTORY_ACTIVE;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.StoneType;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;

/**
 * Controller for a compact multiblock that bulk-processes ore-like inputs into dust and byproducts.
 * <p>
 * This machine intentionally does not use the normal GregTech single-recipe processing loop. Instead, it scans every
 * stored input stack, finds a matching entry in {@link GTNGRecipeMaps#OreProcessingRecipes}, runs as many parallels as
 * the stack size allows, and moves anything unprocessable to the output bus. The structure is backed by StructureLib so
 * the same definition drives validation, survival construction, and NEI/tooltip projection data.
 *
 * @see GTNGMultiBlockBase
 * @see ISurvivalConstructable
 */
public class LargeOreProcessor extends GTNGMultiBlockBase<LargeOreProcessor> implements ISurvivalConstructable {

    private static final int MAX_PARALLEL = Integer.MAX_VALUE;
    private static final int DURATION_TICKS = 20;
    private static final int EU_PER_TICK = 0;

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final String[][] STRUCTURE_SHAPE = new String[][] { { "AAA" }, { "A~A" } };
    private static final int HORIZONTAL_OFFSET = 1;
    private static final int VERTICAL_OFFSET = 1;
    private static final int DEPTH_OFFSET = 0;
    private static final int CASING_META = 0;

    private int mCountCasing = 0;

    public LargeOreProcessor(String aName) {
        super(aName);
    }

    public LargeOreProcessor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    /**
     * Creates the runtime meta-tile entity instance placed in the world.
     * <p>
     * GregTech keeps one registered meta-tile entity as the template and calls this method to create the actual
     * controller instance for a base tile entity.
     *
     * @param aTileEntity base tile entity that will host the new meta-tile entity
     * @return fresh controller instance using the registered internal name
     */
    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new LargeOreProcessor(this.mName);
    }

    /**
     * Builds the StructureLib definition used for validation and construction hints.
     * <p>
     * The single element {@code A} is a chain: hatch positions are accepted first, then remaining positions must be
     * vanilla GregTech casing {@code sBlockCasings2:0}. The {@link #mCountCasing} counter is incremented only for real
     * casing blocks, then checked in {@link #checkMachine(IGregTechTileEntity, ItemStack, List)}.
     *
     * @return reusable StructureLib definition for this controller
     */
    @Override
    public IStructureDefinition<LargeOreProcessor> getStructureDefinition() {
        return StructureDefinition.<LargeOreProcessor>builder()
            .addShape(STRUCTURE_PIECE_MAIN, transpose(STRUCTURE_SHAPE))
            .addElement(
                'A',
                ofChain(
                    buildHatchAdder(LargeOreProcessor.class).casingIndex(getCasingTextureID())
                        .hint(1)
                        .atLeast(InputBus, OutputBus, Maintenance)
                        .build(),
                    onElementPass(machine -> ++machine.mCountCasing, ofBlock(GregTechAPI.sBlockCasings2, CASING_META))))
            .build();
    }

    /**
     * Places client-side construction hints or real blocks for the main structure piece.
     *
     * @param stackSize stack controlling StructureLib build amount and mode
     * @param hintsOnly true to show hologram hints instead of placing blocks
     */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    /**
     * Builds this machine in survival mode through StructureLib's budgeted construction API.
     * <p>
     * Returning {@code -1} when already formed matches the standard GTNH multiblock convention and prevents repeated
     * construction work against a valid machine.
     *
     * @param stackSize     stack controlling StructureLib build amount and mode
     * @param elementBudget maximum number of elements StructureLib may place this call
     * @param env           survival construction environment supplied by StructureLib
     * @return remaining budget result from StructureLib, or {@code -1} when already formed
     */
    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            env,
            false,
            true);
    }

    /**
     * Validates the physical multiblock and records structure errors for GregTech's diagnostics.
     * <p>
     * The casing counter is reset before every check because StructureLib calls the element pass callback for the
     * currently inspected world state. A minimum of one real casing is required so a hatch-only shell is rejected.
     *
     * @param aBaseMetaTileEntity base tile entity of the controller
     * @param aStack              stack used by GregTech during machine checks
     * @param errors              mutable structure-error list filled by failed checks
     */
    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCountCasing = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) {
            return;
        }
        checkCasingMin(errors, mCountCasing, 1);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.OreProcessingRecipes;
    }

    @Override
    public int getMaxParallelRecipes() {
        return MAX_PARALLEL;
    }

    /**
     * Disables the output overflow protection button for this machine.
     * <p>
     * The Large Ore Processor has custom all-at-once output handling, including moving unprocessable inputs to output.
     * Keeping GregTech's generic voiding/protection cycle unavailable avoids a misleading GUI control for this machine.
     *
     * @return false so the inherited GUI renders the button as forbidden and ignores clicks
     */
    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    /**
     * Disables input separation for this machine.
     * <p>
     * Processing intentionally scans all stored inputs together, so allowing bus separation would imply a recipe
     * selection behavior this machine does not use.
     *
     * @return false so the inherited GUI renders the button as forbidden and ignores clicks
     */
    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    /**
     * Disables GregTech batch mode for this machine.
     * <p>
     * The machine already consumes every valid input stack in one custom processing pass. The normal GregTech batch
     * toggle does not add meaningful behavior here.
     *
     * @return false so the inherited GUI renders the button as forbidden and ignores clicks
     */
    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    /**
     * Disables single-recipe locking for this machine.
     * <p>
     * Recipe locking is designed for normal recipe-map execution. This controller performs its own per-stack recipe
     * lookup, so locking to one recipe would not match the machine's processing model.
     *
     * @return false so the inherited GUI renders the button as forbidden and ignores clicks
     */
    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    /**
     * Processes every currently stored input stack in one machine cycle.
     * <p>
     * This method bypasses the default single-recipe handler because the Large Ore Processor is meant to consume many
     * stacks at once, preserve unprocessable items by moving them to output, and run without energy cost.
     *
     * @return GregTech recipe check result describing success or no-recipe state
     */
    @Override
    @NotNull
    public CheckRecipeResult checkProcessing() {
        List<ItemStack> inputs = getStoredInputs();
        if (inputs.isEmpty()) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        RecipeMap<?> recipeMap = getRecipeMap();
        if (recipeMap == null) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        ProcessingResult result = processInputs(inputs, recipeMap);
        if (result.outputs.isEmpty()) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        applyProcessingResult(result);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /**
     * Finds recipes for all input stacks and accumulates their output items.
     * <p>
     * Recipes are first searched with the original input stack. If that fails, ore blocks are normalized to the plain
     * stone variant so basalt, granite, marble, Nether, or End variants can still reuse normal ore recipes when
     * appropriate.
     *
     * @param inputs    mutable stored-input stacks from GregTech hatches and buses
     * @param recipeMap recipe map owned by the Large Ore Processor
     * @return generated output collection ready to apply to the machine
     */
    private ProcessingResult processInputs(List<ItemStack> inputs, RecipeMap<?> recipeMap) {
        List<ItemStack> outputs = new ArrayList<>();
        int totalParallel = 0;

        for (ItemStack input : inputs) {
            if (input == null || input.stackSize <= 0) continue;

            GTRecipe recipe = recipeMap.findRecipeQuery()
                .items(input)
                .notUnificated(true)
                .find();

            if (recipe == null) {
                ItemStack normalized = normalizeOreToStone(input);
                if (normalized != null) {
                    recipe = recipeMap.findRecipeQuery()
                        .items(normalized)
                        .notUnificated(true)
                        .find();
                }
            }

            if (recipe != null) {
                int parallel = Math.min(input.stackSize, MAX_PARALLEL - totalParallel);
                if (parallel <= 0) break;

                processRecipe(input, recipe, parallel, outputs);
                totalParallel += parallel;

                if (totalParallel >= MAX_PARALLEL) break;
            }
        }

        transferUnprocessedItems(inputs, outputs);
        return new ProcessingResult(outputs);
    }

    /**
     * Converts a GregTech ore stack to its normal Stone ore form for fallback recipe lookup.
     * <p>
     * {@link OreManager#getOreInfo(ItemStack)} exposes the material and stone type encoded in generated GT ore stacks.
     * The fallback keeps the material but forces {@link StoneType#Stone}, non-small, and non-natural flags so the
     * recipe
     * search can match the base ore recipe.
     *
     * @param input original ore-like stack
     * @return normalized Stone ore stack, or {@code null} when the input is not a GT ore stack
     */
    private ItemStack normalizeOreToStone(ItemStack input) {
        try (OreInfo<?> info = OreManager.getOreInfo(input)) {
            if (info == null || info.material == null) return null;
            try (OreInfo<IOreMaterial> stoneInfo = OreInfo.getNewInfo()) {
                stoneInfo.material = info.material;
                stoneInfo.stoneType = StoneType.Stone;
                stoneInfo.isSmall = false;
                stoneInfo.isNatural = false;
                return OreManager.getStack(stoneInfo, 1);
            }
        } catch (Throwable t) {
            GTNotGood.LOG.debug("Failed to normalize ore input for Large Ore Processor", t);
            return null;
        }
    }

    /**
     * Applies one found recipe for a chosen parallel count and appends multiplied outputs.
     * <p>
     * The input stack is reduced in place because GregTech stored-input lists are mutable working copies. Output
     * amounts
     * are multiplied manually instead of invoking the normal overclocking/parallel logic, matching this machine's
     * custom
     * all-at-once behavior.
     *
     * @param input    mutable input stack being consumed
     * @param recipe   matching recipe from the custom ore-processing map
     * @param parallel number of input items to process
     * @param outputs  mutable output accumulator
     */
    private void processRecipe(ItemStack input, GTRecipe recipe, int parallel, List<ItemStack> outputs) {
        input.stackSize -= parallel;

        for (ItemStack output : recipe.mOutputs) {
            if (output != null) {
                outputs.add(GTUtility.copyAmountUnsafe(output.stackSize * parallel, output));
            }
        }
    }

    /**
     * Moves any unprocessed input stacks to the output collection.
     * <p>
     * This prevents invalid items from staying in the input bus forever after a successful cycle with other valid ores.
     * Each moved stack is copied to output and then cleared from the mutable input list.
     *
     * @param inputs  mutable input stacks remaining after recipe matching
     * @param outputs mutable output accumulator receiving leftovers
     */
    private void transferUnprocessedItems(List<ItemStack> inputs, List<ItemStack> outputs) {
        for (ItemStack input : inputs) {
            if (input != null && input.stackSize > 0) {
                outputs.add(input.copy());
                input.stackSize = 0;
            }
        }
    }

    /**
     * Writes the prepared processing result back to GregTech's machine state.
     * <p>
     * Both {@code mEUt} and {@code lEUt} are set to zero because this machine is intentionally free to run. The
     * progress
     * time is still set so GregTech performs a normal one-cycle output flush.
     *
     * @param result generated outputs from {@link #processInputs(List, RecipeMap)}
     */
    private void applyProcessingResult(ProcessingResult result) {
        this.mOutputItems = result.outputs.toArray(new ItemStack[0]);
        this.mEUt = EU_PER_TICK;
        this.lEUt = EU_PER_TICK;
        this.mMaxProgresstime = DURATION_TICKS;
        updateSlots();
    }

    /**
     * Carries the generated outputs from one processing check before they are applied to the machine.
     * <p>
     * Keeping this as a tiny value object makes it easier to add more processing state later, such as per-cycle stats
     * or
     * diagnostics, without changing the public processing method signatures.
     */
    private static class ProcessingResult {

        final List<ItemStack> outputs;

        ProcessingResult(List<ItemStack> outputs) {
            this.outputs = outputs;
        }
    }

    /**
     * Returns controller textures for formed and unformed machine rendering.
     * <p>
     * The front face uses the GregTech ore factory overlay so the controller visually reads as an ore machine, while
     * every other side uses the configured casing texture derived from {@link GregTechAPI#sBlockCasings2}.
     *
     * @param aBaseMetaTileEntity base tile entity being rendered
     * @param side                side currently being rendered
     * @param aFacing             controller front facing
     * @param colorIndex          GregTech colorization index
     * @param aActive             true when the machine is active
     * @param redstoneLevel       current redstone state
     * @return texture layers for the requested side
     */
    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        int casingTextureID = getCasingTextureID();
        if (side == aFacing) {
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(casingTextureID), TextureFactory.builder()
                .addIcon(aActive ? OVERLAY_FRONT_ORE_FACTORY_ACTIVE : OVERLAY_FRONT_ORE_FACTORY)
                .extFacing()
                .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(casingTextureID) };
    }

    /**
     * Resolves the GregTech casing texture ID for {@code sBlockCasings2:0}.
     *
     * @return casing texture index used by structure hints and machine rendering
     */
    public int getCasingTextureID() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, CASING_META);
    }

    /**
     * Builds the GregTech multiblock tooltip shown in item tooltips and NEI structure pages.
     * <p>
     * Translation extraction requires each {@code // #tr} block to sit directly above the line that uses the key. Keep
     * those comments attached to the chained {@code addInfo} calls instead of moving them to the top of this method.
     *
     * @return tooltip builder with machine info, required hatches, and structure dimensions
     */
    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr recipe.gtnotgood.largeOreProcessor
        // # Large Ore Processor
        // # zh_CN 大型矿石处理
        String machineType = StatCollector.translateToLocal("recipe.gtnotgood.largeOreProcessor");
        tt.addMachineType(machineType)
            // #tr tooltip.gtnotgood.largeOreProcessor.0
            // # A large ore processing machine
            // # zh_CN 大型矿石处理机器
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeOreProcessor.0"))
            // #tr tooltip.gtnotgood.largeOreProcessor.1
            // # Processes all ore inputs at once
            // # zh_CN 一次处理全部输入矿石
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeOreProcessor.1"))
            // #tr tooltip.gtnotgood.largeOreProcessor.2
            // # No energy or lubricant required
            // # zh_CN 不需要能源或润滑油
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeOreProcessor.2"))
            // #tr tooltip.gtnotgood.largeOreProcessor.3
            // # Supports unlimited parallel recipes
            // # zh_CN 支持无限并行配方
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeOreProcessor.3"))
            // #tr tooltip.gtnotgood.largeOreProcessor.4
            // # Unprocessable items move to the output bus
            // # zh_CN 无法处理的物品会转到输出总线
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeOreProcessor.4"))
            .beginStructureBlock(3, 2, 2, false)
            .addInputBus("Any Input Bus", 1)
            .addOutputBus("Any Output Bus", 1)
            .addMaintenanceHatch("Any Maintenance Hatch", 1)
            .toolTipFinisher();
        return tt;
    }
}
