// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.xyp.gtnotgood.client.render.HeartBlackHoleRenderer;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.HeartOfTheUniverseGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularGeneratorBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.HeartOfTheUniverseStructure;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class HeartOfTheUniverse extends GTNGModularGeneratorBase<HeartOfTheUniverse>
    implements ISurvivalConstructable, IMTERenderer {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "heart/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "heart/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "heart/controller_front_active_glow");
    private static final IIconContainer TOP = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "heart/controller_top");
    private static final IIconContainer TOP_ACTIVE = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "heart/controller_top_active");
    private static final IIconContainer HIGH_POWER = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "heart/high_power_casing");
    private static IStructureDefinition<HeartOfTheUniverse> definition;
    private boolean heartWorking;

    public HeartOfTheUniverse(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public HeartOfTheUniverse(String name) {
        super(name);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new HeartOfTheUniverseGui(this);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.heart.name
        // # Heart of the Universe
        // # zh_CN 宇宙之心
        return "gtng.heart.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new HeartOfTheUniverse(mName);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public IStructureDefinition<HeartOfTheUniverse> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<HeartOfTheUniverse>builder().addShape("main",
                transpose(HeartOfTheUniverseStructure.shape()));
            for (char symbol : "ABCDEFGHPRT".toCharArray())
                builder.addElement(symbol, HeartOfTheUniverseStructure.element(symbol));
            builder.addElement('S',
                ofChain(
                    GTStructureUtility.ofHatchAdder(HeartOfTheUniverse::addModularHatchToMachineList,
                        DimensionalFocusCasing.DimensionInjection.getTextureId(), 1),
                    GTStructureUtility.ofHatchAdder(HeartOfTheUniverse::addMaintenanceToMachineList,
                        DimensionalFocusCasing.DimensionInjection.getTextureId(), 1),
                    buildHatchAdder(HeartOfTheUniverse.class).atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy))
                        .casingIndex(DimensionalFocusCasing.DimensionInjection.getTextureId()).hint(1)
                        .buildAndChain(DimensionalFocusCasing.DimensionInjection.asElement())));
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, HeartOfTheUniverseStructure.OFFSET_X, HeartOfTheUniverseStructure.OFFSET_Y,
            HeartOfTheUniverseStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, HeartOfTheUniverseStructure.OFFSET_X,
                HeartOfTheUniverseStructure.OFFSET_Y, HeartOfTheUniverseStructure.OFFSET_Z, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (
            !checkPiece("main", HeartOfTheUniverseStructure.OFFSET_X, HeartOfTheUniverseStructure.OFFSET_Y,
                HeartOfTheUniverseStructure.OFFSET_Z, errors)
        ) return;
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasAnyEnergy(errors);
        checkModularHatches(errors);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.HeartGenerationRecipes;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) {
            boolean working = mMachine && tile.isActive()
                && (getGeneratedLastTick().signum() > 0 || mMaxProgresstime > 0);
            if (heartWorking != working) {
                heartWorking = working;
                tile.issueTileUpdate();
            }
        }
    }

    public boolean isHeartWorking() {
        return heartWorking;
    }

    public Vec3Impl getBlackHoleOffset() {
        int[] offset = new int[3];
        getExtendedFacing().getWorldOffset(new int[] { 0, -36, 39 }, offset);
        return new Vec3Impl(offset[0], offset[1], offset[2]);
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeBoolean(heartWorking);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        heartWorking = buffer.readBoolean();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        Vec3Impl offset = getBlackHoleOffset();
        double cx = x + offset.get0() + 0.5;
        double cy = y + offset.get1() + 0.5;
        double cz = z + offset.get2() + 0.5;
        return AxisAlignedBB.getBoundingBox(Math.min(x, cx - 40.25), Math.min(y, cy - 40.25), Math.min(z, cz - 40.25),
            Math.max(x + 1, cx + 40.25), Math.max(y + 1, cy + 40.25), Math.max(z + 1, cz + 40.25));
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 384D * 384D;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float partialTicks) {
        Vec3Impl offset = getBlackHoleOffset();
        HeartBlackHoleRenderer.render(this, x + offset.get0() + 0.5, y + offset.get1() + 0.5, z + offset.get2() + 0.5,
            getBaseMetaTileEntity().getFrontFacing(), heartWorking,
            getBaseMetaTileEntity().getWorld().getTotalWorldTime(), partialTicks);
    }

    @Override
    public void onUnload() {
        stopClientEffect();
        heartWorking = false;
        super.onUnload();
    }

    @Override
    public void onRemoval() {
        stopClientEffect();
        heartWorking = false;
        super.onRemoval();
    }

    private void stopClientEffect() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) stopEffect();
    }

    @SideOnly(Side.CLIENT)
    private void stopEffect() {
        HeartBlackHoleRenderer.stop(this);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = TextureFactory.of(HIGH_POWER);
        if (side == facing) {
            ITexture front = TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build();
            return active
                ? new ITexture[] { casing, front,
                    TextureFactory.builder().addIcon(FRONT_GLOW).extFacing().glow().build() }
                : new ITexture[] { casing, front };
        }
        return side == ForgeDirection.UP
            ? new ITexture[] { casing, TextureFactory.builder().addIcon(active ? TOP_ACTIVE : TOP).build() }
            : new ITexture[] { casing };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder();
        addModularMachineType(tooltip, StatCollector.translateToLocal(getRecipeMap().unlocalizedName));
        // #tr gtng.heart.fuel
        // # Consumes 1 Strange Annihilation Fuel Rod and 1 Black Hole Seed per fuel set
        // # zh_CN 每套燃料消耗 1 根奇异湮灭燃料棒和 1 个黑洞种子
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.fuel"));
        // #tr gtng.heart.output
        // # Sends fuel energy to the controller owner's GT wireless network
        // # zh_CN 燃料能量输出至主机所有者的 GT 无线能源网
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.output"));
        // #tr gtng.heart.upkeep
        // # Base running demand: 64A LuV (2,097,152 EU/t); pauses without power
        // # zh_CN 基础维持耗电：64A LuV（2,097,152 EU/t）；供电不足时暂停
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.upkeep"));
        // #tr gtng.heart.cores
        // # Built-in normal core; added cores run independent fuel tasks with shared inputs
        // # zh_CN 内置普通核心；附加核心独立燃烧，共享主机输入
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.cores"));
        // #tr gtng.heart.core_rates
        // # Core release time before modules: normal 60s, advanced 15s, perfect 1t
        // # zh_CN 模块加速前的释放时间：普通 60 秒，高级 15 秒，完美 1t
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.core_rates"));
        // #tr gtng.heart.parallel
        // # Parallel module: 1 + parameter fuel sets; normal cores share this limit
        // # zh_CN 并行模块：每批 1＋参数套燃料；普通核心分摊此额度
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.parallel"));
        // #tr gtng.heart.advanced_parallel
        // # Advanced and perfect cores each use the full parallel limit
        // # zh_CN 高级与完美核心各自使用完整并行额度
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.advanced_parallel"));
        // #tr gtng.heart.modules
        // # Speed and overclock modules shorten release time; minimum 1 tick
        // # zh_CN 加速与超频模块缩短释放时间，最短 1 tick
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.modules"));
        // #tr gtng.heart.overclock_rates
        // # Overclock release rates: low-speed x4, perfect x8, singularity x16
        // # zh_CN 超频释放速率：低速无损 ×4，无损 ×8，奇点无损 ×16
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.overclock_rates"));
        // #tr gtng.heart.efficiency
        // # Power modules reduce upkeep; modules never multiply energy per fuel set
        // # zh_CN 能耗模块降低维持耗电；模块不增加每套燃料的总能量
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.efficiency"));
        // #tr gtng.heart.persistence
        // # Removed cores pause their tasks; spent fuel and remaining energy stay in the controller
        // # zh_CN 拆除核心会暂停对应任务；已消耗燃料与剩余能量由主机保存
        tooltip.addInfo(StatCollector.translateToLocal("gtng.heart.persistence"));
        tooltip.beginStructureBlock(121, 112, 109, false);
        // #tr gtng.heart.hatches
        // # Dimension Injection Casings
        // # zh_CN 维度注入机械方块位置
        String position = StatCollector.translateToLocal("gtng.heart.hatches");
        tooltip.addInputBus(position, 1).addOutputBus(position, 1).addEnergyHatch(position, 1);
        // #tr gtng.heart.structure.modules
        // # Modular hatches replace Dimension Injection Casings
        // # zh_CN 模块仓替换维度注入机械方块
        tooltip.addStructureInfo(StatCollector.translateToLocal("gtng.heart.structure.modules"));
        return tooltip.toolTipFinisher();
    }
}
