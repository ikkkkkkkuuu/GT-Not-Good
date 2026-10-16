package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

/** Electric disassembly chamber using the normal GT transactional recipe and output-protection pipeline. */
public final class LargeTransmutationMachine extends GTNGMultiBlockBase<LargeTransmutationMachine>
    implements ISurvivalConstructable {

    /** Horizontal layers, front to back. A low energy pod extends only from the chamber's right side. */
    private static final String[][] SHAPE = { { "FCCCF  ", "CPPPC  ", "CPLPC  ", "CPPPC  ", "FCCCF  " },
        { "FGGGF  ", "G---G  ", "G-P-G  ", "G---G  ", "FGGGF  " },
        { "CC~CC  ", "G---GCF", "G-P-PPL", "G---GCF", "FGGGF  " },
        { "FGGGF  ", "G---GGC", "G-P-G-C", "G---GGC", "FGGGF  " },
        { "FCCCF  ", "CCCCCCF", "CCPCCPC", "CCCCCCF", "FCCCF  " } };
    private IStructureDefinition<LargeTransmutationMachine> definition;
    private int casings;

    public LargeTransmutationMachine(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public LargeTransmutationMachine(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeTransmutationMachine(mName);
    }

    @Override
    public IStructureDefinition<LargeTransmutationMachine> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<LargeTransmutationMachine>builder().addShape("main", transpose(SHAPE))
                .addElement('C',
                    ofChain(buildHatchAdder(LargeTransmutationMachine.class)
                        .atLeast(InputBus, OutputBus, OutputHatch, Energy, Maintenance).casingIndex(casingTexture())
                        .hint(1).build(), onElementPass(m -> ++m.casings, ofBlock(GregTechAPI.sBlockCasings2, 0))))
                .addElement('F', GTStructureUtility.ofFrame(Materials.Titanium))
                .addElement('P', ofBlock(GregTechAPI.sBlockCasings2, 13))
                .addElement('G', ofBlock(ItemRegistry.bw_realglas, 0)).addElement('L', ofBlock(Blocks.glowstone, 0))
                .addElement('-', isAir()).build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 2, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 2, 2, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        casings = 0;
        if (!checkPiece("main", 2, 2, 0, errors)) return;
        checkCasingMin(errors, casings, 12);
        if (mInputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, InputBus, 0, 1));
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputBus, 0, 1));
        if (mOutputHatches.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputHatch, 0, 1));
        if (mEnergyHatches.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, 0, 1));
        if (shouldCheckMaintenance() && mMaintenanceHatches.isEmpty())
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Maintenance, 0, 1));
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.TransmutationRecipes;
    }

    @Override
    public int getMaxParallelRecipes() {
        return 16;
    }

    private static int casingTexture() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(casingTexture());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? Textures.BlockIcons.OVERLAY_FRONT_DISASSEMBLER_ACTIVE
                : Textures.BlockIcons.OVERLAY_FRONT_DISASSEMBLER).extFacing().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.transmutation.tooltip.casing
        // # Solid Steel Machine Casing
        // # zh_CN 坚实钢机械方块
        String casing = StatCollector.translateToLocal("gtng.transmutation.tooltip.casing");
        tt.addMachineType(StatCollector.translateToLocal("gtng.LargeTransmutationMachine.name"))
            // #tr gtng.transmutation.tooltip.function
            // # Recovers ingredients from assembly and crafting recipes without Shimmer
            // # zh_CN 无需微光，将装配与合成产物拆回原料
            .addInfo(StatCollector.translateToLocal("gtng.transmutation.tooltip.function")).addStaticParallelInfo(16)
            .addPerfectOCInfo()
            // #tr gtng.transmutation.tooltip.power
            // # Base recipe: 1920 EU/t for 5 seconds
            // # zh_CN 基础配方：1920 EU/t，耗时 5 秒
            .addInfo(StatCollector.translateToLocal("gtng.transmutation.tooltip.power"))
            // #tr gtng.transmutation.tooltip.speed
            // # Global recipe speed settings apply; actual duration is shown in NEI
            // # zh_CN 实际耗时受全局配方加速设置影响，详见 NEI
            .addInfo(StatCollector.translateToLocal("gtng.transmutation.tooltip.speed")).addSeparator()
            // #tr gtng.transmutation.tooltip.rules
            // # Check Shimmer recovery recipes in NEI; incomplete batches stay in input
            // # zh_CN 在 NEI 查看微光拆解配方；不足一批的物品留在输入端
            .addInfo(StatCollector.translateToLocal("gtng.transmutation.tooltip.rules"))
            .beginStructureBlock(7, 5, 5, false)
            // #tr gtng.transmutation.tooltip.controller
            // # Front center of the chamber, third layer
            // # zh_CN 主腔正面中央，第 3 层
            .addController(StatCollector.translateToLocal("gtng.transmutation.tooltip.controller"))
            .addCasing("12+", casing, false)
            // #tr gtng.transmutation.tooltip.structure
            // # 22 titanium frames, 34 borosilicate glass, 15 steel pipe casings, 2 glowstone
            // # zh_CN 22 个钛框架、34 块硼硅玻璃、15 个钢管机械方块、2 块荧石
            .addStructureInfo(StatCollector.translateToLocal("gtng.transmutation.tooltip.structure"))
            .addEnergyHatch("1+", casing, 1).addMaintenanceHatch(shouldCheckMaintenance() ? "1+" : "0+", casing, 1)
            .addInputBus("1+", casing, 1).addOutputBus("1+", casing, 1).addOutputHatch("1+", casing, 1)
            .toolTipFinisher();
        return tt;
    }
}
