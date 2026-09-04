package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.GregTechAPI.sBlockCasings1;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeBeeBreederGui;
import com.xyp.gtnotgood.common.machines.bee.BeeBreedingHelper;
import com.xyp.gtnotgood.common.machines.bee.BeeBreedingPlanner;
import com.xyp.gtnotgood.common.machines.bee.DronePool;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;

import forestry.api.apiculture.IAlleleBeeSpecies;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * 大型蜜蜂杂交机
 * <p>
 * 自动杂交繁育蜜蜂的电力多方块机器。
 * <ul>
 * <li>在控制器GUI中设置目标蜜蜂品种名</li>
 * <li>输入任意基础蜜蜂（公主蜂或雄蜂）→ 品种自动进入雄蜂池</li>
 * <li>机器根据雄蜂池中已有品种，自动计算并逐步走完繁育链到达目标</li>
 * <li>繁育产生的新品种永久加入雄蜂池，下次繁育无需重复</li>
 * <li>每输入一只公主蜂，最终输出一只目标品种的公主蜂</li>
 * <li>每次繁育尝试消耗 400EU/t 基础电力，每周期 3.2 秒</li>
 * </ul>
 */
public class LargeBeeBreeder extends GTNGMultiBlockBase<LargeBeeBreeder> implements ISurvivalConstructable {

    // ==================== 常量 ====================

    /** 基础耗电（无玻璃时每个并行杂交任务 400EU/t） */
    private static final long BASE_EUT_PER_BREEDING = 400L;

    /** 每级玻璃对每个并行杂交任务增加的 EU/t */
    private static final long EUT_PER_GLASS_TIER = 400L;

    /** 每周期繁育耗时 (ticks) */
    private static final int TICKS_PER_BREEDING = 64;

    /** 每周期最大并行繁育步骤数 */
    private static final int MAX_PARALLEL_STEPS = 16;

    private static final byte OPERATION_NONE = 0;
    private static final byte OPERATION_BREEDING = 1;
    private static final byte OPERATION_OUTPUT = 2;

    // ==================== 结构定义 ====================

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFF_SET = 7;
    private static final int VERTICAL_OFF_SET = 8;
    private static final int DEPTH_OFF_SET = 0;

    private int mCountCasing = 0;

    /** 控制器槽是否有不锈钢齿轮（+2%成功率） */
    private boolean hasStainlessSteelGear = false;

    /** 玻璃等级（每级+1%成功率） */
    private int glassTier = -1;

    private IStructureDefinition<LargeBeeBreeder> STRUCTURE_DEFINITION = null;

    // 15 wide (x), 17 tall (y), 15 deep (z)
    // A=glass, B=dirt/grass, G=casing+hatches, H=wood planks, I=wood slabs, J/K/L/N/O/P=bronze frame
    // W(water) and F(flowers) replaced with spaces (air)
    private static final String[][] shape = transpose(
        new String[][] {
            { "               ", "               ", "               ", "      HHH      ", "    HHAAAHH    ",
                "    HAPLPAH    ", "   HAPAAAPAH   ", "   HALAAALAH   ", "   HAPAAAPAH   ", "    HAPLPAH    ",
                "    HHAAAHH    ", "      HHH      ", "               ", "               ", "               " },
            { "               ", "               ", "      GGG      ", "    GG   GG    ", "   G       G   ",
                "   G       G   ", "  G         G  ", "  G         G  ", "  G         G  ", "   G       G   ",
                "   G       G   ", "    GG   GG    ", "      GGG      ", "               ", "               " },
            { "               ", "      HHH      ", "   HHH   HHH   ", "  HG       GH  ", "  H         H  ",
                "  H         H  ", " H           H ", " H           H ", " H           H ", "  H         H  ",
                "  H         H  ", "  HG       GH  ", "   HHH   HHH   ", "      HHH      ", "               " },
            { "      GGG      ", "   GGG   GGG   ", "  G         G  ", " G           G ", " G           G ",
                " G           G ", "G             G", "G             G", "G             G", " G           G ",
                " G           G ", " G           G ", "  G         G  ", "   GGG   GGG   ", "      GGG      " },
            { "      AAA      ", "   OLA   ALO   ", "  P         P  ", " O           O ", " L           L ",
                " A           A ", "A             A", "A             A", "A             A", " A           A ",
                " L           L ", " O           O ", "  P         P  ", "   OLA   ALO   ", "      AAA      " },
            { "     AAAAA     ", "   NA     AO   ", "  P         P  ", " N           O ", " A           A ",
                "A             A", "A     III     A", "A     III     A", "A     III     A", "A             A",
                " A           A ", " N           N ", "  P         P  ", "   NA     AN   ", "     AAAAA     " },
            { "     AAAAA     ", "   NA     AO   ", "  P         P  ", " N           O ", " A           A ",
                "A             A", "A     JJJ     A", "A     JKJ     A", "A     JJJ     A", "A             A",
                " A           A ", " N           N ", "  P         P  ", "   NA     AN   ", "     AAAAA     " },
            { "      AAA      ", "   OLA   ALO   ", "  P         P  ", " O           O ", " L           L ",
                " A           A ", "A             A", "A             A", "A             A", " A           A ",
                " L           L ", " O           O ", "  P         P  ", "   OLA   ALO   ", "      AAA      " },
            { "      G~G      ", "   GGGBBBGGG   ", "  GBB     BBG  ", " GBB       BBG ", " GB         BG ",
                " G           G ", "GB           BG", "GB           BG", "GB           BG", " G           G ",
                " GB         BG ", " GBB       BBG ", "  GBB     BBG  ", "   GGGBBBGGG   ", "      GGG      " },
            { "      HHH      ", "    HHBBBHH    ", "  HHBBBBBBBHH  ", "  HBBB   BBBH  ", " HBB       BBH ",
                " HBB BBBBB BBH ", "HBB  BBBBBB BBH", "HBB BBBBBBB BBH", "HBB BBBBBB  BBH", " HB  BBBBB BBH ",
                " HBB   BB BBH ", "  HBBB    BBH  ", "  HHBBBBBBBHH  ", "    HHBBBHH    ", "      HHH      " },
            { "               ", "     GGGGG     ", "   GGBBBBBGG   ", "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ",
                " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ",
                "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ", "   GGBBBBBGG   ", "     GGGGG     ", "               " },
            { "               ", "      HHH      ", "    HHBBBHH    ", "   HBBBBBBBH   ", "  HBBBBBBBBBH  ",
                "  HBBBBBBBBBH  ", " HBBBBBBBBBBBH ", " HBBBBBBBBBBBH ", " HBBBBBBBBBBBH ", "  HBBBBBBBBBH  ",
                "  HBBBBBBBBBH  ", "   HBBBBBBBH   ", "    HHBBBHH    ", "      HHH      ", "               " },
            { "               ", "               ", "      GGG      ", "    GGBBBGG    ", "   GBBBBBBBG   ",
                "   GBBBBBBBG   ", "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ", "   GBBBBBBBG   ",
                "   GBBBBBBBG   ", "    GGBBBGG    ", "      GGG      ", "               ", "               " },
            { "               ", "               ", "       H       ", "     HHBHH     ", "    HBBBBBH    ",
                "   HBBBBBBBH   ", "   HBBBBBBBH   ", "  HBBBBBBBBBH  ", "   HBBBBBBBH   ", "   HBBBBBBBH   ",
                "    HBBBBBH    ", "     HHBHH     ", "       H       ", "               ", "               " },
            { "               ", "               ", "               ", "       G       ", "     GGBGG     ",
                "    GBBBBBG    ", "    GBBBBBG    ", "   GBBBBBBBG   ", "    GBBBBBG    ", "    GBBBBBG    ",
                "     GGBGG     ", "       G       ", "               ", "               ", "               " },
            { "               ", "               ", "               ", "               ", "      HHH      ",
                "     HHHHH     ", "    HHBBBHH    ", "    HHBBBHH    ", "    HHBBBHH    ", "     HHHHH     ",
                "      HHH      ", "               ", "               ", "               ", "               " },
            { "               ", "               ", "               ", "               ", "               ",
                "               ", "      GGG      ", "      GHG      ", "      GGG      ", "               ",
                "               ", "               ", "               ", "               ", "               " } });

    // ==================== 机器状态 ====================

    /** 目标蜜蜂品种名 */
    private String targetBeeSpecies = "";

    /** Permanently unlocked species archive. */
    private DronePool dronePool = new DronePool();

    /** Current immutable plan and the list exposed to legacy display code. */
    private BeeBreedingPlanner.Plan breedingPlan = BeeBreedingPlanner.Plan.empty("");
    private List<BeeBreedingHelper.BreedingStep> breedingChain = new ArrayList<>();
    private boolean breedingPlanDirty = true;
    /** Work frozen at recipe start. It is the only work that may settle at recipe completion. */
    private byte activeOperation = OPERATION_NONE;
    private List<BreedingAttempt> activeBreedingBatch = new ArrayList<>();

    /** 待输出的目标公主蜂数量 */
    private int pendingPrincessOutputs = 0;

    /** 当前缺少的品种名（用于GUI显示） */
    private String missingDroneSpecies = "";

    /** 是否因缺少品种而无法继续 */
    private boolean allTasksBlocked = false;

    /** 繁育链总步数 */
    private int chainTotalSteps = 0;

    /** 繁育链已完成步数 */
    private int chainCompletedSteps = 0;

    /** 用于客户端同步的雄蜂池品种数 */
    private int syncedPoolSize = 0;

    /** Species UID currently blocking the plan. */
    private String syncedMissingInfo = "";

    /** Structured GUI data. UIDs are resolved to localized names on the client. */
    private List<String> syncedPoolSpecies = Collections.emptyList();
    private List<NBTTagCompound> syncedChainSteps = Collections.emptyList();

    /** Last server-side target validation result, displayed beside the Apply button. */
    private boolean targetInputValid = true;

    /**
     * GUI 摘要脏标记。
     * <p>
     * 池摘要 / 链摘要只依赖 (雄蜂池内容 + 繁育链 + 齿轮/玻璃加成)，这些只在
     * 投入蜜蜂、繁育出新品种、重算链、结构/齿轮变化时才会变。用脏标记避免每秒无谓重建字符串。
     */
    private boolean displayDirty = true;

    private void markDisplayDirty() {
        displayDirty = true;
    }

    private void invalidateBreedingPlan() {
        breedingPlanDirty = true;
        markDisplayDirty();
    }

    private static final class BreedingAttempt {

        private final BeeBreedingHelper.BreedingStep step;
        private final double effectiveChance;

        private BreedingAttempt(BeeBreedingHelper.BreedingStep step, double effectiveChance) {
            this.step = step;
            this.effectiveChance = effectiveChance;
        }

        private NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("parent1", step.parent1);
            tag.setString("parent2", step.parent2);
            tag.setString("result", step.result);
            tag.setDouble("baseChance", step.chance);
            tag.setDouble("effectiveChance", effectiveChance);
            return tag;
        }

        private static BreedingAttempt fromNBT(NBTTagCompound tag) {
            if (tag == null) return null;
            String parent1 = tag.getString("parent1");
            String parent2 = tag.getString("parent2");
            String result = tag.getString("result");
            if (parent1.isEmpty() || parent2.isEmpty() || result.isEmpty()) return null;
            BeeBreedingHelper.BreedingStep step = new BeeBreedingHelper.BreedingStep(
                parent1,
                parent2,
                result,
                tag.getDouble("baseChance"));
            return new BreedingAttempt(step, tag.getDouble("effectiveChance"));
        }
    }

    // ==================== 构造函数 ====================

    public LargeBeeBreeder(String aName) {
        super(aName);
    }

    public LargeBeeBreeder(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new LargeBeeBreeder(this.mName);
    }

    // ==================== 机器类型 ====================

    public String getMachineType() {
        // #tr LargeBeeBreederRecipeType
        // # Bee Breeder
        // # zh_CN 养蜂机
        return StatCollector.translateToLocal("LargeBeeBreederRecipeType");
    }

    // ==================== 结构定义 ====================

    @Override
    public void onValueUpdate(byte aValue) {}

    @Override
    public byte getUpdateData() {
        return 0;
    }

    @Override
    public IStructureDefinition<LargeBeeBreeder> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<LargeBeeBreeder>builder()
                .addShape(STRUCTURE_PIECE_MAIN, shape)
                .addElement('A', chainAllGlasses(-1, (te, t) -> te.glassTier = t, te -> te.glassTier))
                .addElement('B', ofChain(ofBlockAnyMeta(Blocks.dirt, 0), ofBlock(Blocks.grass, 0)))
                .addElement(
                    'G',
                    ofChain(
                        buildHatchAdder(LargeBeeBreeder.class)
                            .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(10)
                            .hint(1)
                            .buildAndChain(),
                        onElementPass(x -> ++x.mCountCasing, ofBlock(sBlockCasings1, 10))))
                .addElement('H', ofBlock(Blocks.planks, 0))
                .addElement('I', ofBlock(Blocks.wooden_slab, 0))
                .addElement('J', ofBlock(gregtech.api.GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('K', ofBlock(gregtech.api.GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('L', ofBlock(gregtech.api.GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('N', ofBlock(gregtech.api.GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('O', ofBlock(gregtech.api.GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('P', ofBlock(gregtech.api.GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        this.buildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            hintsOnly,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        return this.survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCountCasing = 0;
        glassTier = -1;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET, errors)) return;
        checkCasingMin(errors, mCountCasing, 10);
        if (mInputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, InputBus, 0, 1));
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputBus, 0, 1));
        int energyHatchCount = mEnergyHatches.size() + mExoticEnergyHatches.size();
        if (energyHatchCount < 1)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, energyHatchCount, 1));
        if (shouldCheckMaintenance() && mMaintenanceHatches.isEmpty())
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Maintenance, 0, 1));
        hasStainlessSteelGear = checkStainlessSteelGear(getControllerSlot());
        // 玻璃等级/齿轮影响摘要里显示的成功率，重新组装后强制重建一次
        markDisplayDirty();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public int getMaxParallelRecipes() {
        return 1;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    private boolean checkStainlessSteelGear(ItemStack stack) {
        if (stack == null) return false;
        return stack.isItemEqual(GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.StainlessSteel, 1L));
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && mMachine && aTick % 20 == 0) {
            boolean gearBefore = hasStainlessSteelGear;
            hasStainlessSteelGear = checkStainlessSteelGear(getControllerSlot());
            if (gearBefore != hasStainlessSteelGear) markDisplayDirty();

            scanInputBuses();
            refreshBreedingPlan();

            // 摘要字符串只在池/链/加成变化时重建，避免每秒无谓的字符串拼接
            if (displayDirty) {
                syncedPoolSize = dronePool.getAvailableSpecies()
                    .size();
                syncedPoolSpecies = new ArrayList<>(dronePool.getAvailableSpecies());
                Collections.sort(syncedPoolSpecies);
                syncedChainSteps = buildStructuredChainSteps();
                // 同步重建缺失信息，使其与 chainSummary 同时触发客户端更新
                if (allTasksBlocked && missingDroneSpecies != null && !missingDroneSpecies.isEmpty()) {
                    syncedMissingInfo = missingDroneSpecies;
                } else {
                    syncedMissingInfo = "";
                }
                displayDirty = false;
            }
        }
    }

    // ==================== 纹理 ====================

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        if (side == aFacing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(10), TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(10), TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(10) };
    }

    // ==================== 核心处理逻辑 ====================

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        scanInputBuses();
        activeOperation = OPERATION_NONE;
        activeBreedingBatch.clear();

        if (targetBeeSpecies == null || targetBeeSpecies.isEmpty()) {
            refreshBreedingPlan();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        refreshBreedingPlan();
        if (pendingPrincessOutputs <= 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        if (dronePool.hasDrone(targetBeeSpecies)) {
            ItemStack princess = BeeBreedingHelper.createPrincess(targetBeeSpecies);
            if (princess == null) {
                allTasksBlocked = true;
                missingDroneSpecies = targetBeeSpecies;
                markDisplayDirty();
                // #tr GT5U.gui.text.recipe_result.BeeBreeder_unreachable_target
                // # Current species archive cannot reach the target species!
                // # zh_CN 当前物种档案无法到达目标品种！
                return SimpleCheckRecipeResult.ofFailure("BeeBreeder_unreachable_target");
            }
            ItemStack drone = BeeBreedingHelper.createDrone(targetBeeSpecies);
            mOutputItems = drone == null ? new ItemStack[] { princess } : new ItemStack[] { princess, drone };
            activeOperation = OPERATION_OUTPUT;
            prepareBreedingCycle(0L);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        List<BeeBreedingHelper.BreedingStep> readySteps = breedingPlan
            .getReadySteps(dronePool.getAvailableSpecies(), MAX_PARALLEL_STEPS);
        if (readySteps.isEmpty()) {
            allTasksBlocked = true;
            missingDroneSpecies = breedingPlan.getFirstMissingSpecies();
            if (missingDroneSpecies.isEmpty()) missingDroneSpecies = targetBeeSpecies;
            markDisplayDirty();
            // #tr GT5U.gui.text.recipe_result.BeeBreeder_missing_drone
            // # Species archive is missing a required parent!
            // # zh_CN 物种档案中缺少必要亲本！
            return SimpleCheckRecipeResult.ofFailure("BeeBreeder_missing_drone");
        }

        double chanceBonus = (hasStainlessSteelGear ? 2.0D : 0.0D) + Math.max(0, glassTier);
        List<BreedingAttempt> batch = new ArrayList<>(readySteps.size());
        for (BeeBreedingHelper.BreedingStep step : readySteps) {
            batch.add(new BreedingAttempt(step, step.chance + chanceBonus));
        }

        activeOperation = OPERATION_BREEDING;
        activeBreedingBatch = batch;
        allTasksBlocked = false;
        missingDroneSpecies = "";
        prepareBreedingCycle(getEnergyCostPerTick(batch.size()));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private long getEnergyCostPerTick(int parallelAttempts) {
        long eutPerAttempt = BASE_EUT_PER_BREEDING + (long) Math.max(0, glassTier) * EUT_PER_GLASS_TIER;
        return Math.max(0L, eutPerAttempt * Math.max(0, parallelAttempts));
    }

    public long getEuPerBreedingAttempt() {
        return getEnergyCostPerTick(1);
    }

    public int getMaxParallelBreedingSteps() {
        return MAX_PARALLEL_STEPS;
    }

    public int getMutationChanceBonus() {
        return (hasStainlessSteelGear ? 2 : 0) + Math.max(0, glassTier);
    }

    private void prepareBreedingCycle(long eut) {
        mMaxProgresstime = TICKS_PER_BREEDING;
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = -eut;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (mProgresstime >= mMaxProgresstime - 1) {
            settleActiveOperation();
        }
        return super.onRunningTick(aStack);
    }

    // ==================== 物品处理 ====================

    private boolean scanInputBuses() {
        ArrayList<ItemStack> inputs = getStoredInputs();
        List<ItemStack> toRemove = new ArrayList<>();
        boolean stateChanged = false;

        for (ItemStack stack : inputs) {
            if (stack == null) continue;

            if (BeeBreedingHelper.isDrone(stack)) {
                stateChanged |= dronePool.addDrone(stack);
                toRemove.add(stack);
            } else if (BeeBreedingHelper.isPrincess(stack)) {
                // 使用基因组中的实际 UID，确保同 unlocalizedName 但不同 UID 的品种不过混淆
                String uid = BeeBreedingHelper.getBeeUID(stack);
                if (uid != null) {
                    stateChanged |= dronePool.unlockSpecies(uid);
                    long newPending = (long) pendingPrincessOutputs + Math.max(1, stack.stackSize);
                    pendingPrincessOutputs = (int) Math.min(Integer.MAX_VALUE, newPending);
                }
                toRemove.add(stack);
                stateChanged = true;
            }
        }

        for (ItemStack stack : toRemove) {
            stack.stackSize = 0;
        }
        if (!toRemove.isEmpty()) {
            updateSlots();
            invalidateBreedingPlan();
            markDisplayDirty();
        }

        return stateChanged;
    }

    private void settleActiveOperation() {
        if (activeOperation == OPERATION_NONE) return;

        boolean archiveChanged = false;
        if (activeOperation == OPERATION_OUTPUT) {
            if (mOutputItems != null && mOutputItems.length > 0 && pendingPrincessOutputs > 0) {
                pendingPrincessOutputs--;
            }
        } else if (activeOperation == OPERATION_BREEDING) {
            for (BreedingAttempt attempt : activeBreedingBatch) {
                if (dronePool.hasDrone(attempt.step.result)) continue;
                if (BeeBreedingHelper.tryMutation(attempt.effectiveChance)) {
                    archiveChanged |= dronePool.unlockSpecies(attempt.step.result);
                }
            }
        }

        activeOperation = OPERATION_NONE;
        activeBreedingBatch.clear();
        if (archiveChanged) invalidateBreedingPlan();
        markDisplayDirty();
    }

    private void refreshBreedingPlan() {
        boolean previousBlocked = allTasksBlocked;
        String previousMissing = missingDroneSpecies;
        int previousCompleted = chainCompletedSteps;

        if (targetBeeSpecies == null || targetBeeSpecies.isEmpty()) {
            breedingPlan = BeeBreedingPlanner.Plan.empty("");
            breedingChain = new ArrayList<>();
            breedingPlanDirty = false;
            allTasksBlocked = false;
            missingDroneSpecies = "";
        } else {
            if (breedingPlanDirty) {
                breedingPlan = BeeBreedingPlanner.plan(targetBeeSpecies, dronePool.getAvailableSpecies());
                breedingChain = new ArrayList<>(breedingPlan.getSteps());
                breedingPlanDirty = false;
            }

            boolean targetUnlocked = dronePool.hasDrone(targetBeeSpecies);
            boolean hasReadyStep = !breedingPlan.getReadySteps(dronePool.getAvailableSpecies(), 1)
                .isEmpty();
            allTasksBlocked = pendingPrincessOutputs > 0 && !targetUnlocked && !hasReadyStep;
            missingDroneSpecies = allTasksBlocked ? breedingPlan.getFirstMissingSpecies() : "";
            if (allTasksBlocked && missingDroneSpecies.isEmpty()) {
                missingDroneSpecies = targetBeeSpecies;
            }
        }

        updateChainDisplayInfo();
        if (previousBlocked != allTasksBlocked || !java.util.Objects.equals(previousMissing, missingDroneSpecies)
            || previousCompleted != chainCompletedSteps) {
            markDisplayDirty();
        }
    }

    private int countCompletedSteps() {
        int count = 0;
        for (BeeBreedingHelper.BreedingStep step : breedingChain) {
            if (dronePool.hasDrone(step.result)) count++;
        }
        return count;
    }

    private void updateChainDisplayInfo() {
        chainTotalSteps = breedingChain.size();
        chainCompletedSteps = countCompletedSteps();
    }

    // ==================== GUI 交互 ====================

    private List<NBTTagCompound> buildStructuredChainSteps() {
        if (breedingChain.isEmpty()) return Collections.emptyList();
        List<NBTTagCompound> result = new ArrayList<>(breedingChain.size());
        double chanceBonus = (hasStainlessSteelGear ? 2.0D : 0.0D) + Math.max(0, glassTier);
        for (BeeBreedingHelper.BreedingStep step : breedingChain) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("parent1", step.parent1);
            tag.setString("parent2", step.parent2);
            tag.setString("result", step.result);
            tag.setDouble("chance", Math.min(100.0D, step.chance + chanceBonus));
            if (dronePool.hasDrone(step.result)) {
                tag.setByte("status", (byte) 2);
            } else if (dronePool.hasDrone(step.parent1) && dronePool.hasDrone(step.parent2)) {
                tag.setByte("status", (byte) 1);
            } else {
                tag.setByte("status", (byte) 0);
            }
            result.add(tag);
        }
        return result;
    }

    @Override
    protected MTEMultiBlockBaseGui<LargeBeeBreeder> getGui() {
        return new LargeBeeBreederGui(this);
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    @Override
    public int getGUIHeight() {
        return 192;
    }

    // ==================== NBT 存档 ====================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);

        aNBT.setString("targetBeeSpecies", targetBeeSpecies != null ? targetBeeSpecies : "");
        aNBT.setInteger("pendingPrincessOutputs", pendingPrincessOutputs);
        aNBT.setTag("dronePool", dronePool.toNBT());
        aNBT.setByte("beeBreederActiveOperation", activeOperation);
        NBTTagList attempts = new NBTTagList();
        for (BreedingAttempt attempt : activeBreedingBatch) {
            attempts.appendTag(attempt.toNBT());
        }
        aNBT.setTag("beeBreederActiveBatch", attempts);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);

        targetBeeSpecies = aNBT.getString("targetBeeSpecies");
        pendingPrincessOutputs = aNBT.getInteger("pendingPrincessOutputs");

        if (aNBT.hasKey("dronePool")) {
            dronePool = DronePool.fromNBT(aNBT.getCompoundTag("dronePool"));
        } else {
            dronePool = new DronePool();
        }

        activeOperation = aNBT.getByte("beeBreederActiveOperation");
        activeBreedingBatch = new ArrayList<>();
        NBTTagList attempts = aNBT.getTagList("beeBreederActiveBatch", 10);
        for (int i = 0; i < attempts.tagCount(); i++) {
            BreedingAttempt attempt = BreedingAttempt.fromNBT(attempts.getCompoundTagAt(i));
            if (attempt != null) activeBreedingBatch.add(attempt);
        }
        if (activeOperation != OPERATION_BREEDING) activeBreedingBatch.clear();

        breedingPlan = BeeBreedingPlanner.Plan.empty(targetBeeSpecies);
        breedingPlanDirty = true;
        breedingChain.clear();
        markDisplayDirty();
    }

    /**
     * 写入物品 NBT（挖掉机器时保留数据到掉落物 ItemStack）
     * <p>
     * GT 在方块被破坏时调用此方法，将数据写入掉落物的 NBT，
     * 重新放置时可从 loadNBTData 恢复。
     */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        aNBT.setString("targetBeeSpecies", targetBeeSpecies != null ? targetBeeSpecies : "");
        aNBT.setInteger("pendingPrincessOutputs", pendingPrincessOutputs);
        aNBT.setTag("dronePool", dronePool.toNBT());
        super.setItemNBT(aNBT);
    }

    // ==================== 访问器 ====================

    public String getTargetBeeSpecies() {
        return targetBeeSpecies;
    }

    public void setTargetBeeSpecies(String species) {
        // 优先按 UID 精确查找（来自 NEI 拖放），再按名称模糊匹配（用户手动输入）
        String candidate = species == null ? "" : species.trim();
        if (!candidate.isEmpty()) {
            IAlleleBeeSpecies resolved = BeeBreedingHelper.getSpeciesByUID(candidate);
            if (resolved == null) {
                resolved = BeeBreedingHelper.getSpeciesByName(candidate);
            }
            targetInputValid = resolved != null;
            if (resolved != null) {
                // 存储 UID 作为唯一标识（避免同 unlocalizedName 但不同 UID 的品种混淆）
                String resolvedUID = resolved.getUID();
                if (!resolvedUID.equals(targetBeeSpecies)) {
                    this.targetBeeSpecies = resolvedUID;
                    invalidateBreedingPlan();
                    markBaseTileDirty();
                }
                return;
            }
            markDisplayDirty();
            return;
        }
        targetInputValid = true;
        if (!targetBeeSpecies.isEmpty()) {
            this.targetBeeSpecies = "";
            invalidateBreedingPlan();
            markBaseTileDirty();
        }
    }

    private void markBaseTileDirty() {
        IGregTechTileEntity baseTile = getBaseMetaTileEntity();
        if (baseTile != null) baseTile.markDirty();
    }

    public DronePool getDronePool() {
        return dronePool;
    }

    public int getSyncedPoolSize() {
        return syncedPoolSize;
    }

    public int getPendingPrincessOutputs() {
        return pendingPrincessOutputs;
    }

    public int getChainTotalSteps() {
        return chainTotalSteps;
    }

    public int getChainCompletedSteps() {
        return chainCompletedSteps;
    }

    public boolean isAllTasksBlocked() {
        return allTasksBlocked;
    }

    public String getMissingDroneSpecies() {
        return missingDroneSpecies;
    }

    public String getSyncedMissingInfo() {
        return syncedMissingInfo;
    }

    public boolean isTargetInputValid() {
        return targetInputValid;
    }

    public List<String> getSyncedPoolSpecies() {
        return syncedPoolSpecies;
    }

    public void setSyncedPoolSpecies(List<String> species) {
        syncedPoolSpecies = species == null ? Collections.emptyList() : species;
    }

    public List<NBTTagCompound> getSyncedChainSteps() {
        return syncedChainSteps;
    }

    public void setSyncedChainSteps(List<NBTTagCompound> steps) {
        syncedChainSteps = steps == null ? Collections.emptyList() : steps;
    }

    // ==================== Tooltip ====================

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(StatCollector.translateToLocal("LargeBeeBreederRecipeType"));
        // #tr Tooltip_LargeBeeBreeder_00
        // # A large electric automatic bee breeding machine
        // # zh_CN 大型电力自动养蜂杂交机
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_00"));
        // #tr Tooltip_LargeBeeBreeder_01
        // # Input any princess or drone to unlock that species permanently
        // # zh_CN 投入任意公主蜂或雄蜂，永久记入物种档案
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_01"));
        // #tr Tooltip_LargeBeeBreeder_02
        // # The machine plans and runs every reachable mutation step automatically
        // # zh_CN 机器会自动规划并执行可到达的杂交步骤
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_02"));
        // #tr Tooltip_LargeBeeBreeder_03
        // # Each breeding cycle takes 3.2 seconds and uses Forestry mutation chance
        // # zh_CN 每次繁育周期 3.2 秒，使用林业原版杂交概率
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_03"));
        // #tr Tooltip_LargeBeeBreeder_04
        // # Each princess input becomes one target princess when the chain completes
        // # zh_CN 每投入一只公主蜂，完成后输出一只目标公主蜂
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_04"));
        // #tr Tooltip_LargeBeeBreeder_05
        // # Each parallel breeding attempt costs 400 EU/t before glass upgrades
        // # zh_CN 每个并行杂交任务基础消耗 400 EU/t
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_05"));
        // #tr Tooltip_LargeBeeBreeder_06
        // # Insert a Stainless Steel gear in the controller slot for extra mutation chance
        // # zh_CN 在主机内插入不锈钢齿轮提高杂交成功率
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_06"));
        // #tr Tooltip_LargeBeeBreeder_07
        // # Higher glass tier improves mutation chance and increases EU/t
        // # zh_CN 玻璃等级越高，成功率和耗电越高
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_07"));
        // #tr Tooltip_LargeBeeBreeder_Casing
        // # Machine casing
        // # zh_CN 机器外壳
        String casing = StatCollector.translateToLocal("Tooltip_LargeBeeBreeder_Casing");
        tt.beginStructureBlock(15, 17, 15, false)
            .addInputBus(casing, 1)
            .addOutputBus(casing, 1)
            .addEnergyHatch(casing, 1)
            .addMaintenanceHatch(casing, 1)
            .toolTipFinisher();
        return tt;
    }

    // ==================== 信息显示 ====================

    @Override
    public String[] getInfoData() {
        String[] superInfo = super.getInfoData();
        String[] info = new String[superInfo.length + 5];
        System.arraycopy(superInfo, 0, info, 0, superInfo.length);

        info[superInfo.length] = "Target: " + (targetBeeSpecies.isEmpty() ? "Not Set" : targetBeeSpecies);
        info[superInfo.length + 1] = "Pool Species: " + dronePool.getAvailableSpecies()
            .size();
        info[superInfo.length + 2] = "Chain Progress: " + countCompletedSteps() + "/" + breedingChain.size();
        info[superInfo.length + 3] = "Pending Output: " + pendingPrincessOutputs;
        info[superInfo.length + 4] = "EU/t per Attempt: " + NumberFormatUtil.formatNumber(getEnergyCostPerTick(1));

        return info;
    }
}
