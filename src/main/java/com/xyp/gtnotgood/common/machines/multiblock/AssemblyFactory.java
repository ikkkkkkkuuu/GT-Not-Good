package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.blocks.machine.AssemblyMatrixBlock;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.AssemblyFactoryGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGCleanWirelessMultiMachineBase;
import com.xyp.gtnotgood.loader.BlockLoader;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.IGTHatchAdder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;

/**
 * Two-mode Assembly Factory with automatic high-tier wireless cross-recipe processing.
 * <p>
 * The normal component mode works at both structure tiers. Assembly Line mode requires tier two matrix blocks and a
 * Data Access hatch. A tier two structure with no normal or exotic energy hatch automatically uses the owner's
 * wireless EU network and the configurable cross-recipe batch limits from the shared wireless base.
 */
public class AssemblyFactory extends GTNGCleanWirelessMultiMachineBase<AssemblyFactory>
    implements ISurvivalConstructable {

    public static final int MODE_COMPONENT = 0;
    public static final int MODE_ASSEMBLY_LINE = 1;
    private static final int HORIZONTAL_OFFSET = 5;
    private static final int VERTICAL_OFFSET = 7;
    private static final int DEPTH_OFFSET = 0;
    private static final String STRUCTURE_PIECE_MAIN = "main";

    // spotless:off
    @SuppressWarnings("SpellCheckingInspection")
    private static final String[][] SHAPE = new String[][] {
        {"           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           ","           "},
        {"    HHH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HIH    ","    HHH    ","           "},
        {"   HJJJH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HCCCH   ","   HJJJH   ","           "},
        {" HHGJJJGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGBBBGHH "," HHGJJJGHH ","           "},
        {"HJJJJJJJJJH","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","H         H","H         H","H  AA AA  H","HJJJJJJJJJH","           "},
        {"HJJJJJJJJJH","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","J    D    J","J    D    J","J AA D AA J","HJJJJJJJJJH","           "},
        {"HJJJJFJJJJH","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","J   DAD   J","J   DAD   J","JAA DAD AAJ","HJJJJJJJJJH","           "},
        {"HJJJF~FJJJH","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","J  DAEAD  J","J  DAEAD  J","JA DAEAD AJ","HJJJJJJJJJH","           "},
        {"HJJFFFFFJJH","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","J   DAD   J","J   DAD   J","JA  DAD  AJ","HJJJJJJJJJH","           "},
        {"HJFFFFFFFJH","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","J    D    J","J    D    J","JA   D   AJ","HJJJJJJJJJH","           "},
        {"HFFFFFFFFFH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","           "},
        {"HFFFFFFFFFH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","HHHHHHHHHHH","           "}
    };
    // spotless:on

    private static final IStructureDefinition<AssemblyFactory> STRUCTURE_DEFINITION = StructureDefinition
        .<AssemblyFactory>builder()
        .addShape(STRUCTURE_PIECE_MAIN, transpose(SHAPE))
        .addElement('A', Casings.AssemblyLineCasing.asElement())
        .addElement('B', Casings.AssemblerMachineCasing.asElement())
        .addElement('C', Casings.HermeticCasing9.asElement())
        .addElement('D', Casings.ComputerHeatVent.asElement())
        .addElement('E', Casings.AdvancedComputerCasing.asElement())
        .addElement(
            'F',
            HatchElementBuilder.<AssemblyFactory>builder()
                .anyOf(
                    HatchElement.InputBus,
                    HatchElement.InputHatch,
                    HatchElement.OutputBus,
                    HatchElement.OutputHatch,
                    HatchElement.Energy.or(HatchElement.ExoticEnergy),
                    AssemblyFactoryHatchElement.DataAccess)
                .casingIndex(Casings.AdvancedMolecularCasing.textureId)
                .hint(1)
                .buildAndChain(
                    ofBlock(
                        Casings.AdvancedMolecularCasing.getBlock(),
                        Casings.AdvancedMolecularCasing.getBlockMeta())))
        .addElement('G', Casings.MolecularCoil.asElement())
        .addElement('H', Casings.UltimateMolecularCasing.asElement())
        .addElement(
            'I',
            ofBlocksTiered(
                AssemblyFactory::getMatrixBlockTier,
                Arrays.asList(
                    Pair.of(BlockLoader.assemblyMatrixBlock, 0),
                    Pair.of(BlockLoader.advancedAssemblyMatrixBlock, 0)),
                0,
                AssemblyFactory::setLevelTier,
                AssemblyFactory::getLevelTier))
        .addElement('J', Casings.QuantumGlass.asElement())
        .build();

    private final ArrayList<MTEHatchDataAccess> dataAccessHatches = new ArrayList<>();
    private final List<GTRecipe.RecipeAssemblyLine> allowedAssemblyRecipes = new ArrayList<>();
    private int levelTier;

    public AssemblyFactory(int id, String name, String regionalName) {
        super(id, name, regionalName);
        machineMode = MODE_COMPONENT;
    }

    public AssemblyFactory(String name) {
        super(name);
        machineMode = MODE_COMPONENT;
    }

    /** @return tier written by the matrix structure element */
    public int getLevelTier() {
        return levelTier;
    }

    /** Sets the uniform matrix tier found by StructureLib. */
    public void setLevelTier(int tier) {
        levelTier = tier;
    }

    /** @return whether the controller is currently processing a recipe */
    public boolean isMachineActive() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.isActive();
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public String getMachineModeName() {
        if (machineMode == MODE_ASSEMBLY_LINE) {
            // #tr machine.gtnotgood.assembly_factory.mode.assembly_line
            // # Assembly Line
            // # zh_CN 装配线
            return StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.mode.assembly_line");
        }
        // #tr machine.gtnotgood.assembly_factory.mode.component
        // # Component Assembly Line
        // # zh_CN 部件装配线
        return StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.mode.component");
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new AssemblyFactoryGui(this);
    }

    /**
     * Handles the main-GUI mode button. Active machines cannot switch, and Assembly Line mode is only retained by a
     * tier two structure. The server immediately rechecks the structure so hatch requirements update with the mode.
     */
    @Override
    public void setMachineMode(int index) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        int requested = index == MODE_ASSEMBLY_LINE ? MODE_ASSEMBLY_LINE : MODE_COMPONENT;
        if (base != null && base.isServerSide()) {
            if (base.isActive()) return;
            if (requested == MODE_ASSEMBLY_LINE && levelTier != 2) return;
        }
        super.setMachineMode(requested);
        if (base != null && base.isServerSide()) checkStructure(true, base);
    }

    @Override
    protected boolean isEnablePerfectOverclock() {
        return true;
    }

    @Override
    public int getMaxParallelRecipes() {
        if (levelTier == 2) return Integer.MAX_VALUE;
        return (int) Math.min(Integer.MAX_VALUE, Math.pow(3, getInputVoltageTier()));
    }

    @Override
    protected boolean supportsCrossRecipeParallel() {
        return true;
    }

    @Override
    protected CheckRecipeResult validateRecipeForMachine(GTRecipe recipe) {
        if (machineMode != MODE_COMPONENT || isWirelessModeActive()) return CheckRecipeResultRegistry.SUCCESSFUL;
        long energyTier = getInputVoltageTier();
        if (energyTier <= 0 || recipe.mSpecialValue > energyTier) {
            return CheckRecipeResultRegistry.insufficientMachineTier(recipe.mSpecialValue);
        }
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected Stream<GTRecipe> filterRecipeMatches(Stream<GTRecipe> recipes) {
        return recipes.filter(this::isAssemblyRecipeAllowed);
    }

    @Override
    public IStructureDefinition<AssemblyFactory> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity base, ItemStack stack, List<StructureError> errors) {
        levelTier = 0;
        dataAccessHatches.clear();
        setWirelessModeAvailable(false);
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return;

        setWirelessModeAvailable(levelTier == 2 && areEnergyHatchesEmpty());
        setWirelessModeEnabled(isWirelessModeAvailable());
        if (machineMode == MODE_ASSEMBLY_LINE) {
            if (levelTier != 2) {
                // #tr machine.gtnotgood.assembly_factory.error.need_tier2
                // # Assembly Line mode requires advanced matrix blocks
                // # zh_CN 装配线模式需要高级装配矩阵方块
                errors.add(StructureErrors.of("machine.gtnotgood.assembly_factory.error.need_tier2"));
                return;
            }
        }

        if (mInputBusses.isEmpty() && mInputHatches.isEmpty() && mDualInputHatches.isEmpty()) {
            errors.add(StructureErrors.of("GT5U.gui.text.structure_error.no_input"));
        }
        if (mOutputBusses.isEmpty() && mOutputHatches.isEmpty()) {
            errors.add(StructureErrors.of("GT5U.gui.text.structure_error.no_output"));
        }
        if (!isWirelessModeActive() && areEnergyHatchesEmpty()) {
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, HatchElement.Energy, 0, 1));
        }
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return machineMode == MODE_ASSEMBLY_LINE ? GTNGRecipeMaps.AssemblyFactoryAssemblyLineRecipes
            : GoodGeneratorRecipeMaps.componentAssemblyLineRecipes;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.<RecipeMap<?>>asList(
            GoodGeneratorRecipeMaps.componentAssemblyLineRecipes,
            GTNGRecipeMaps.AssemblyFactoryAssemblyLineRecipes);
    }

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        if (!mMachine) return CheckRecipeResultRegistry.NO_RECIPE;
        if (machineMode == MODE_ASSEMBLY_LINE) {
            if (levelTier != 2) return CheckRecipeResultRegistry.insufficientMachineTier(2);
            if (!collectAllowedAssemblyRecipes()) return CheckRecipeResultRegistry.NO_DATA_STICKS;
        }
        return super.checkProcessing();
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (machineMode == MODE_ASSEMBLY_LINE) {
            for (MTEHatchDataAccess hatch : dataAccessHatches) {
                hatch.getBaseMetaTileEntity()
                    .setActive(true);
            }
        }
        return super.onRunningTick(stack);
    }

    private boolean collectAllowedAssemblyRecipes() {
        return true;
    }

    private boolean isAssemblyRecipeAllowed(@Nullable GTRecipe recipe) {
        return true;
    }

    private boolean addDataAccessToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof MTEHatchDataAccess hatch)) return false;
        ((MTEHatch) hatch).updateTexture(casingIndex);
        return dataAccessHatches.add(hatch);
    }

    private static Integer getMatrixBlockTier(Block block, int meta) {
        return block instanceof AssemblyMatrixBlock matrix ? matrix.getLevelTier() : null;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stack, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stack,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new AssemblyFactory(mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        // #tr machine.gtnotgood.assembly_factory.machine_type
        // # Assembly Factory: Component Assembly Line | Assembly Line
        // # zh_CN 装配工厂: 部件装配线 | 装配线
        String type = StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.machine_type");
        // #tr machine.gtnotgood.assembly_factory.tooltip.mode_button
        // # Switch recipe mode with the main GUI button; switching is disabled while running
        // # zh_CN 使用主界面按钮切换配方模式，运行中无法切换
        String modeButton = StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.tooltip.mode_button");
        // #tr machine.gtnotgood.assembly_factory.tooltip.component_mode
        // # Component mode accepts Component Assembly Line recipes at both structure tiers
        // # zh_CN 部件装配线模式：一级和二级结构均可处理部件装配线配方
        String componentMode = StatCollector
            .translateToLocal("machine.gtnotgood.assembly_factory.tooltip.component_mode");
        // #tr machine.gtnotgood.assembly_factory.tooltip.energy_tier
        // # Component mode recipe tier cannot exceed the highest installed energy hatch tier
        // # zh_CN 部件装配线模式的配方等级被能源仓限制
        String energyTier = StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.tooltip.energy_tier");
        // #tr machine.gtnotgood.assembly_factory.tooltip.assembly_line_mode
        // # Assembly Line mode only works with a uniform tier 2 Advanced Assembly Matrix structure
        // # zh_CN 装配线模式：仅支持全部使用二级高级装配矩阵方块的结构
        String assemblyLineMode = StatCollector
            .translateToLocal("machine.gtnotgood.assembly_factory.tooltip.assembly_line_mode");
        // #tr machine.gtnotgood.assembly_factory.tooltip.data_access
        // # Requires a Data Access hatch; recipes must be authorized by its flash media or a controller Data Stick
        // # zh_CN 装配线模式不需要数据访问仓，无需闪存就可工作！！
        String dataAccess = StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.tooltip.data_access");
        // #tr machine.gtnotgood.assembly_factory.tooltip.parallel
        // # Wired parallel: tier 1 = 3 ^ energy hatch tier; tier 2 = Integer.MAX_VALUE
        // # zh_CN 配方并行：一级为 3^能源仓等级，二级为 Integer.MAX_VALUE
        String parallel = StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.tooltip.parallel");
        // #tr machine.gtnotgood.assembly_factory.tooltip.perfect_overclock
        // # All wired recipes use perfect overclocking
        // # zh_CN 所有配方均启用无损超频
        String perfectOverclock = StatCollector
            .translateToLocal("machine.gtnotgood.assembly_factory.tooltip.perfect_overclock");
        // #tr machine.gtnotgood.assembly_factory.tooltip.wireless_trigger
        // # Tier 2 with no normal or exotic energy hatch automatically enters wireless mode
        // # zh_CN 二级结构未安装能源仓时自动进入无线模式
        String wirelessTrigger = StatCollector
            .translateToLocal("machine.gtnotgood.assembly_factory.tooltip.wireless_trigger");
        // #tr machine.gtnotgood.assembly_factory.tooltip.matrix_tier
        // # All Assembly Matrix Blocks must be the same tier
        // # zh_CN 全部装配矩阵方块必须保持同一等级
        String matrixTier = StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.tooltip.matrix_tier");
        // #tr machine.gtnotgood.assembly_factory.tooltip.hatch_positions
        // # Place all hatches only in Advanced Molecular Casing positions; no maintenance hatch is required
        // # zh_CN 所有仓室只能替换高级分子外壳位置；无需维护仓
        String hatchPositions = StatCollector
            .translateToLocal("machine.gtnotgood.assembly_factory.tooltip.hatch_positions");
        return new MultiblockTooltipBuilder().addMachineType(type)
            .addSeparator()
            .addInfo(EnumChatFormatting.YELLOW + modeButton)
            .addInfo(EnumChatFormatting.AQUA + componentMode)
            .addInfo(EnumChatFormatting.DARK_AQUA + energyTier)
            .addInfo(EnumChatFormatting.AQUA + assemblyLineMode)
            .addInfo(EnumChatFormatting.GOLD + dataAccess)
            .addInfo(EnumChatFormatting.WHITE + parallel)
            .addInfo(EnumChatFormatting.GOLD + perfectOverclock)
            .addInfo(EnumChatFormatting.LIGHT_PURPLE + wirelessTrigger)
            .beginStructureBlock(11, 12, 34, false)
            .addStructureInfo(EnumChatFormatting.GRAY + matrixTier)
            .addStructureInfo(EnumChatFormatting.GRAY + hatchPositions)
            .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, ForgeDirection side, ForgeDirection facing, int colorIndex,
        boolean active, boolean redstone) {
        if (side == facing) {
            return new ITexture[] {
                TextureFactory
                    .of(Casings.AdvancedMolecularCasing.getBlock(), Casings.AdvancedMolecularCasing.getBlockMeta()),
                TextureFactory.of(active ? TexturesGtBlock.oMCAQFTActive : TexturesGtBlock.oMCAQFT) };
        }
        return new ITexture[] { TextureFactory
            .of(Casings.UltimateMolecularCasing.getBlock(), Casings.UltimateMolecularCasing.getBlockMeta()) };
    }

    private enum AssemblyFactoryHatchElement implements IHatchElement<AssemblyFactory> {

        DataAccess;

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return Collections.singletonList(MTEHatchDataAccess.class);
        }

        @Override
        public IGTHatchAdder<AssemblyFactory> adder() {
            return AssemblyFactory::addDataAccessToMachineList;
        }

        @Override
        public long count(AssemblyFactory machine) {
            return machine.dataAccessHatches.size();
        }
    }
}
