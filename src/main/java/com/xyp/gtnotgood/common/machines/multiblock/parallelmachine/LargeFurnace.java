// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.parallelmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeFurnaceGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGParallelMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.LargeFurnaceStructure;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class LargeFurnace extends GTNGParallelMachineBase<LargeFurnace> implements ISurvivalConstructable {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "parallel/furnace_front");
    private static final IIconContainer ACTIVE = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "parallel/furnace_active");
    private IStructureDefinition<LargeFurnace> definition;

    public LargeFurnace(int id, String name, String regional) {
        super(id, name, regional);
    }

    public LargeFurnace(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeFurnace(mName);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.large_furnace.name
        // # Large Furnace
        // # zh_CN 大型熔炉
        return "gtng.large_furnace.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    public static int scaleForTier(int base, int tier) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base << Math.min(30, Math.max(0, tier - 1)));
    }

    @Override
    protected int getBaseParallel() {
        return scaleForTier(64, GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected int getBaseThreads() {
        return scaleForTier(2, GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.furnaceRecipes;
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeFurnaceGui(this);
    }

    @Override
    public IStructureDefinition<LargeFurnace> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<LargeFurnace>builder()
                .addShape("main", transpose(LargeFurnaceStructure.shape())).addElement('L',
                    ofChain(GTStructureUtility.ofHatchAdder(LargeFurnace::addParallelHatch, 16, 1),
                        GTStructureUtility.ofHatchAdder(LargeFurnace::addThreadHatch, 16, 1),
                        buildHatchAdder(LargeFurnace.class).atLeast(InputBus, OutputBus, Energy, Maintenance)
                            .casingIndex(16).hint(1).buildAndChain(ofBlock(GregTechAPI.sBlockCasings2, 0))));
            ItemStack steel = Materials.Steel.getBlocks(1);
            builder.addElement('J', ofBlock(Block.getBlockFromItem(steel.getItem()), steel.getItemDamage()))
                .addElement('D', ofBlock(GregTechAPI.sBlockCasings12, 0))
                .addElement('E', ofBlock(GregTechAPI.sBlockCasings4, 15)).addElement('H', ofBlock(Blocks.lava, 0))
                .addElement('I', ofBlock(Blocks.lava, 0));
            for (char c : new char[] { 'B', 'C', 'G', 'K', 'M' }) builder.addElement(c, ofBlock(Blocks.iron_bars, 0));
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hints) {
        buildPiece("main", stack, hints, 5, 11, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 5, 11, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece("main", 5, 11, 0, errors)) return;
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasEnergyHatch(errors);
        if (mEnergyHatches.size() > 2) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY,
            TranslatableText.lang("GT5U.MBTT.EnergyHatch"), mEnergyHatches.size(), 2));
        checkHatchExact(errors, Maintenance, 1);
        checkParallelHatches(errors);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(16);
        return side == facing
            ? new ITexture[] { casing, TextureFactory.builder().addIcon(active ? ACTIVE : FRONT).extFacing().build() }
            : new ITexture[] { casing };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(StatCollector.translateToLocal(RecipeMaps.furnaceRecipes.unlocalizedName))
            // #tr gtng.large_furnace.performance
            // # LV: base parallel 64, base threads 2; both double per voltage tier
            // # zh_CN LV：基准并行 64、基准线程 2；电压每升一级，两者翻倍
            .addInfo(StatCollector.translateToLocal("gtng.large_furnace.performance"))
            // #tr gtng.large_furnace.batch
            // # Smelts multiple recipes in one batch with shared parallel capacity
            // # zh_CN 同批熔炼多种配方，共享并行额度
            .addInfo(StatCollector.translateToLocal("gtng.large_furnace.batch"))
            // #tr gtng.large_furnace.power
            // # Batch duration follows total recipe energy and input voltage; minimum 20 ticks
            // # zh_CN 按配方总能量与输入电压计算批次耗时，最短 20 tick
            .addInfo(StatCollector.translateToLocal("gtng.large_furnace.power"))
            // #tr gtng.large_furnace.upgrades
            // # Supports one parallel control hatch and one Ω-Spheres Divergence Engine
            // # zh_CN 支持 1 个并行控制仓与 1 个 Ω-天球分歧引擎
            .addInfo(StatCollector.translateToLocal("gtng.large_furnace.upgrades"))
            .beginStructureBlock(11, 13, 12, false)
            // #tr gtng.large_furnace.controller
            // # Front center, second layer from the bottom
            // # zh_CN 正面中央，自底向上第 2 层
            .addController(StatCollector.translateToLocal("gtng.large_furnace.controller"))
            .addCasing("146", Materials.Steel.getBlocks(1).getDisplayName(), false)
            .addCasing("102", ItemList.Casing_Firebricks.get(1).getDisplayName(), false)
            .addCasing("64", ItemList.CokeOvenCasing.get(1).getDisplayName(), false)
            .addCasing("89", new ItemStack(Blocks.iron_bars).getDisplayName(), false)
            // #tr gtng.large_furnace.lava
            // # 56 lava source blocks; unused steel casing positions accept hatches
            // # zh_CN 56 格岩浆源；钢机械方块位置可替换为仓室
            .addStructureInfo(StatCollector.translateToLocal("gtng.large_furnace.lava"))
            // #tr gtng.large_furnace.hatch
            // # Solid Steel Machine Casings (hint 1)
            // # zh_CN 坚固钢机械方块（提示点 1）
            .addInputBus("1+", StatCollector.translateToLocal("gtng.large_furnace.hatch"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("gtng.large_furnace.hatch"), 1)
            .addEnergyHatch("1-2", StatCollector.translateToLocal("gtng.large_furnace.hatch"), 1)
            .addMaintenanceHatch("1", StatCollector.translateToLocal("gtng.large_furnace.hatch"), 1).toolTipFinisher();
        return tt;
    }
}
