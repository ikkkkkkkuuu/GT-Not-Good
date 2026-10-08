package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksMap;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofOreDictBlockMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * Electric comb processor ported from GT-Not-Cool with its original structure and recipe bonuses.
 * Uses the shared modern GUI and long-valued GregTech power accounting.
 */
public class LargeCombProcessor extends GTNGMultiBlockBase<LargeCombProcessor> implements ISurvivalConstructable {

    public LargeCombProcessor(String aName) {
        super(aName);
    }

    public LargeCombProcessor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new LargeCombProcessor(this.mName);
    }

    public String getMachineType() {
        // #tr gtng.comb.type
        // # Comb Processing
        // # zh_CN 蜂窝处理
        return StatCollector.translateToLocal("gtng.comb.type");
    }

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFFSET = 7;
    private static final int VERTICAL_OFFSET = 8;
    private static final int DEPTH_OFFSET = 0;
    private static final int MAX_PARALLEL_RECIPES = Integer.MAX_VALUE;

    private int mCountCasing = 0;

    private IStructureDefinition<LargeCombProcessor> structureDefinition = null;

    // 15 wide (x), 17 tall (y), 15 deep (z)
    // A=glass, B=dirt/grass, G=casing+hatches, H=wood planks, I=wood slabs, J/K/L/N/O/P=bronze frame
    private static final String[][] shape = transpose(new String[][] {
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
            " HBB   BB BBH  ", "  HBBB    BBH  ", "  HHBBBBBBBHH  ", "    HHBBBHH    ", "      HHH      " },
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

    @Override
    public IStructureDefinition<LargeCombProcessor> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<LargeCombProcessor>builder()
                .addShape(STRUCTURE_PIECE_MAIN, shape).addElement('A', chainAllGlasses())
                .addElement('B', ofChain(ofBlockAnyMeta(Blocks.dirt, 0), ofBlock(Blocks.grass, 0)))
                .addElement('G',
                    ofChain(
                        buildHatchAdder(LargeCombProcessor.class)
                            .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(getCasingTextureID()).hint(1).build(),
                        onElementPass(x -> ++x.mCountCasing, ofBlock(GregTechAPI.sBlockCasings2, 0))))
                .addElement('H', ofBlocksMap(ofOreDictBlockMap("plankWood"), Blocks.planks, 0))
                .addElement('I', ofBlocksMap(ofOreDictBlockMap("slabWood"), Blocks.wooden_slab, 0))
                .addElement('J', ofBlock(GregTechAPI.sBlockFrames, Materials.Bronze.mMetaItemSubID))
                .addElement('K', ofBlock(GregTechAPI.sBlockFrames, Materials.Bronze.mMetaItemSubID))
                .addElement('L', ofBlock(GregTechAPI.sBlockFrames, Materials.Bronze.mMetaItemSubID))
                .addElement('N', ofBlock(GregTechAPI.sBlockFrames, Materials.Bronze.mMetaItemSubID))
                .addElement('O', ofBlock(GregTechAPI.sBlockFrames, Materials.Bronze.mMetaItemSubID))
                .addElement('P', ofBlock(GregTechAPI.sBlockFrames, Materials.Bronze.mMetaItemSubID)).build();
        }
        return structureDefinition;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCountCasing = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return;
        checkHasAnyEnergy(errors);
        checkCasingMin(errors, mCountCasing, 1);
    }

    // ==================== 纹理 ====================

    public int getCasingTextureID() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        int id = getCasingTextureID();
        if (side == aFacing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(id),
                TextureFactory.builder().addIcon(BlockIcons.OverlayFrontSingularityDataHubActive).extFacing().build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(id),
                TextureFactory.builder().addIcon(BlockIcons.OverlayFrontSingularityDataHub).extFacing().build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(id) };
    }

    // ==================== 并行 ====================

    @Override
    public int getMaxParallelRecipes() {
        return MAX_PARALLEL_RECIPES;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.CombProcessingRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().enablePerfectOverclock().setMaxParallelSupplier(this::getTrueParallel)
            .setSpeedBonus(0.1).setEuModifier(0.1);
    }

    // ==================== 跨配方并行 ====================

    /** Remaining shared power budget while collecting additional recipe groups. */
    private long remainingPower = -1;

    /**
     * Caps additional groups before inputs are consumed to prevent aggregate power overcommit.
     * 
     * @param logic processing logic receiving the remaining power budget
     */
    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        super.setProcessingLogicPower(logic);
        if (remainingPower >= 0) {
            logic.setAvailableVoltage(Math.min(getAverageInputVoltage(), remainingPower));
            logic.setAvailableAmperage(1);
        }
    }

    /**
     * Collects groups with upstream summed duration and EU/t semantics, bounded to 256 groups per check.
     * Output protection uses one native transaction so pending outputs cannot overfill shared buses.
     * 
     * @return the first native failure, or success after at least one group was consumed
     */
    @Override
    public CheckRecipeResult checkProcessing() {
        CheckRecipeResult first = super.checkProcessing();
        if (!first.wasSuccessful() || protectsExcessItem() || protectsExcessFluid()) return first;
        long totalEUt = Math.abs(lEUt);
        long availablePower = getMaxInputEu();
        int duration = mMaxProgresstime;
        ArrayList<ItemStack> items = cloneItemArray(mOutputItems);
        ArrayList<FluidStack> fluids = cloneFluidArray(mOutputFluids);
        try {
            for (int group = 1; group < 256 && totalEUt < availablePower; group++) {
                if (duration > Integer.MAX_VALUE / 2) break;
                remainingPower = availablePower - totalEUt;
                CheckRecipeResult next = super.checkProcessing();
                if (!next.wasSuccessful()) break;
                totalEUt += Math.abs(lEUt);
                duration = (int) Math.min(Integer.MAX_VALUE, (long) duration + mMaxProgresstime);
                mergeItemStacks(items, mOutputItems);
                mergeFluidStacks(fluids, mOutputFluids);
            }
        } finally {
            remainingPower = -1;
        }
        mOutputItems = items.toArray(new ItemStack[0]);
        mOutputFluids = fluids.toArray(new FluidStack[0]);
        mMaxProgresstime = duration;
        lEUt = -totalEUt;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private static ArrayList<ItemStack> cloneItemArray(ItemStack[] items) {
        ArrayList<ItemStack> list = new ArrayList<>();
        if (items != null) {
            for (ItemStack s : items) {
                if (s != null) list.add(s.copy());
            }
        }
        return list;
    }

    private static ArrayList<FluidStack> cloneFluidArray(FluidStack[] fluids) {
        ArrayList<FluidStack> list = new ArrayList<>();
        if (fluids != null) {
            for (FluidStack f : fluids) {
                if (f != null) list.add(f.copy());
            }
        }
        return list;
    }

    private static void mergeItemStacks(ArrayList<ItemStack> acc, ItemStack[] items) {
        if (items == null) return;
        for (ItemStack toMerge : items) {
            if (toMerge == null) continue;
            boolean merged = false;
            for (ItemStack existing : acc) {
                if (GTUtility.areStacksEqual(toMerge, existing) && existing.stackSize < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.stackSize;
                    int add = Math.min(toMerge.stackSize, space);
                    existing.stackSize += add;
                    if (add >= toMerge.stackSize) {
                        merged = true;
                        break;
                    }
                    ItemStack remainder = toMerge.copy();
                    remainder.stackSize = toMerge.stackSize - add;
                    toMerge = remainder;
                }
            }
            if (!merged) {
                acc.add(toMerge.copy());
            }
        }
    }

    private static void mergeFluidStacks(ArrayList<FluidStack> acc, FluidStack[] fluids) {
        if (fluids == null) return;
        for (FluidStack fluid : fluids) {
            if (fluid != null) acc.add(fluid.copy());
        }
    }

    // ==================== 信息显示 ====================

    @Override
    public String[] getInfoData() {
        ArrayList<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(StatCollector.translateToLocalFormatted("GT5U.multiblock.curparallelism",
            "" + EnumChatFormatting.YELLOW + getMaxParallelRecipes()));
        return info.toArray(new String[0]);
    }

    // ==================== 多块构建 ====================

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        return survivalBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET,
            elementBudget, env, false, true);
    }

    // ==================== Tooltip ====================

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.comb.casing
        // # Solid Steel Machine Casing
        // # zh_CN 固态钢机器外壳
        String casing = StatCollector.translateToLocal("gtng.comb.casing");
        tt.addMachineType(getMachineType())
            // #tr gtng.comb.direct
            // # Processes combs into dust, gems, fluids and special products
            // # zh_CN 直接将蜂窝加工为粉末、宝石、流体与特殊产物
            .addInfo(StatCollector.translateToLocal("gtng.comb.direct"))
            // #tr gtng.comb.bonus
            // # Perfect overclocking; 10% recipe duration and EU usage
            // # zh_CN 完美超频；配方耗时与耗电均为原值的10%
            .addInfo(StatCollector.translateToLocal("gtng.comb.bonus"))
            // #tr gtng.comb.cross
            // # With overflow allowed: groups share power; duration and EU/t add up
            // # zh_CN 允许溢出时跨配方处理：共享功率预算，耗时与EU/t累加
            .addInfo(StatCollector.translateToLocal("gtng.comb.cross")).beginStructureBlock(15, 17, 15, false)
            // #tr gtng.comb.controller
            // # Front center, ninth layer from the top
            // # zh_CN 正面中央，从顶向下第九层
            .addController(StatCollector.translateToLocal("gtng.comb.controller")).addInputBus(casing, 1)
            .addOutputBus(casing, 1).addInputHatch(casing, 1).addOutputHatch(casing, 1).addEnergyHatch(casing, 1)
            .addMaintenanceHatch(casing, 1).toolTipFinisher();
        return tt;
    }

    // ==================== 音效 ====================

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.IC2_MACHINES_INDUCTION_LOOP;
    }
}
