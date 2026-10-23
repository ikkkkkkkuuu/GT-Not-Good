package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.error;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.SuperParticleColliderGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperParticleColliderStructure;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class SuperParticleCollider extends GTNGModularMachineBase<SuperParticleCollider>
    implements ISurvivalConstructable {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "fieldextruder/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active_glow");

    private IStructureDefinition<SuperParticleCollider> definition;

    public SuperParticleCollider(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public SuperParticleCollider(String name) {
        super(name);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new SuperParticleColliderGui(this);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.particle_collider.name
        // # Super Particle Collider
        // # zh_CN 超级粒子对撞机
        return "gtng.particle_collider.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SuperParticleCollider(mName);
    }

    @Override
    public IStructureDefinition<SuperParticleCollider> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<SuperParticleCollider>builder()
                .addShape("main", transpose(SuperParticleColliderStructure.shape()))
                .addElement('#',
                    ofChain(
                        GTStructureUtility.ofHatchAdder(SuperParticleCollider::addModularHatchToMachineList,
                            ParticleColliderCasing.Lafium.getTextureId(), 1),
                        GTStructureUtility.ofHatchAdder(SuperParticleCollider::addMaintenanceToMachineList,
                            ParticleColliderCasing.Lafium.getTextureId(), 1),
                        buildHatchAdder(SuperParticleCollider.class)
                            .atLeast(InputHatch, OutputHatch, Energy.or(ExoticEnergy))
                            .casingIndex(ParticleColliderCasing.Lafium.getTextureId()).hint(1)
                            .buildAndChain(ParticleColliderCasing.Lafium.asElement())))
                .addElement('A', ParticleColliderCasing.Lafium.asElement())
                .addElement('B', GTStructureUtility.ofFrame(Materials.NaquadahEnriched))
                .addElement('D', Casings.RadiantNaquadahAlloyCasing.asElement())
                .addElement('E', Casings.SuperconductingCoilBlock.asElement())
                .addElement('F', Casings.ChemicalGradeGlass.asElement())
                .addElement('G',
                    ModsItemlist.InvertedCyanLamp.getBlock() == null ? error()
                        : ofBlock(ModsItemlist.InvertedCyanLamp.getBlock(), 25))
                .addElement('I',
                    ModsItemlist.InvertedOrangeLamp.getBlock() == null ? error()
                        : ofBlock(ModsItemlist.InvertedOrangeLamp.getBlock(), 17))
                .addElement('J', ParticleColliderCasing.AcceleratedPipeline.asElement())
                .addElement('K', Casings.FusionMachineCasingMKII.asElement())
                .addElement('L', Casings.ZPMMachineCasing.asElement())
                .addElement('N', Casings.TungstensteelPipeCasing.asElement()).build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, SuperParticleColliderStructure.OFFSET_X,
            SuperParticleColliderStructure.OFFSET_Y, SuperParticleColliderStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, SuperParticleColliderStructure.OFFSET_X,
                SuperParticleColliderStructure.OFFSET_Y, SuperParticleColliderStructure.OFFSET_Z, budget, env, false,
                true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (
            !checkPiece("main", SuperParticleColliderStructure.OFFSET_X, SuperParticleColliderStructure.OFFSET_Y,
                SuperParticleColliderStructure.OFFSET_Z, errors)
        ) return;
        checkHasInputHatch(errors);
        checkHasOutputHatch(errors);
        checkHasAnyEnergy(errors);
        checkModularHatches(errors);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.SuperParticleColliderRecipes;
    }

    @Override
    protected ProcessingLogic createMachineProcessingLogic() {
        return new ModularProcessingLogic().setMaxParallelSupplier(this::getTrueParallel).setMaxTierSkips(0);
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList()))
            voltage = Math.max(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
        logic.setAvailableVoltage(voltage).setAvailableAmperage(voltage == 0 ? 0 : getMaxInputEu() / voltage)
            .setAmperageOC(false);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GT_MACHINES_FUSION_LOOP;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = ParticleColliderCasing.Lafium.getTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE_GLOW : FRONT_GLOW).extFacing().glow().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        addModularMachineType(tt, StatCollector.translateToLocal(getRecipeMap().unlocalizedName))
            // #tr gtng.particle_collider.function
            // # Transmutes molten elements using helium plasma
            // # zh_CN 使用氦等离子体转化熔融元素
            .addInfo(StatCollector.translateToLocal("gtng.particle_collider.function"))
            // #tr gtng.particle_collider.recipes
            // # Base recipe: 10 seconds at UV; 4096 L of each input yields 4000 L of product
            // # zh_CN 基础配方：UV 级，耗时 10 秒，各消耗 4096 L 输入流体，产出 4000 L 产物
            .addInfo(StatCollector.translateToLocal("gtng.particle_collider.recipes"));
        addModularTooltip(tt);
        tt.beginStructureBlock(SuperParticleColliderStructure.WIDTH, SuperParticleColliderStructure.HEIGHT,
            SuperParticleColliderStructure.LENGTH, false)
            // #tr gtng.particle_collider.controller
            // # Front face, 41 blocks above the bottom; 93 blocks from the left edge
            // # zh_CN 正面，距底层 41 格，距左边缘 93 格
            .addController(StatCollector.translateToLocal("gtng.particle_collider.controller"))
            .addCasing("173483+", ParticleColliderCasing.Lafium.getStack(1).getDisplayName(), false)
            .addCasing("19250", ParticleColliderCasing.AcceleratedPipeline.getStack(1).getDisplayName(), false)
            .addCasing("45634", ItemList.Casing_Coil_Superconductor.get(1).getDisplayName(), false)
            .addCasing("42995", ItemList.RadiantNaquadahAlloyCasing.get(1).getDisplayName(), false)
            .addCasing("7774", Casings.ChemicalGradeGlass.toStack(1).getDisplayName(), false)
            .addCasing("3244", ItemList.Casing_Fusion2.get(1).getDisplayName(), false)
            .addCasing("1992", ItemList.Casing_ZPM.get(1).getDisplayName(), false)
            .addCasing("654", ItemList.Casing_Pipe_TungstenSteel.get(1).getDisplayName(), false)
            // #tr gtng.particle_collider.frame
            // # Enriched Naquadah Frame Box
            // # zh_CN 富集硅岩框架
            .addCasing("7556", StatCollector.translateToLocal("gtng.particle_collider.frame"), false)
            // #tr gtng.particle_collider.lamps
            // # Inverted Lamps (ProjectRed): 14 Cyan, 4 Orange
            // # zh_CN 反转灯（红石计划）：青色 14 个，橙色 4 个
            .addStructureInfo(StatCollector.translateToLocal("gtng.particle_collider.lamps"))
            // #tr gtng.particle_collider.hatch_location
            // # 24 front Lafium Casing positions (hint 1)
            // # zh_CN 正面 24 个路菲恩机械方块位置（提示点 1）
            .addInputHatch("1+", StatCollector.translateToLocal("gtng.particle_collider.hatch_location"), 1)
            .addOutputHatch("1+", StatCollector.translateToLocal("gtng.particle_collider.hatch_location"), 1)
            .addEnergyHatch("1+", StatCollector.translateToLocal("gtng.particle_collider.hatch_location"), 1);
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }
}
