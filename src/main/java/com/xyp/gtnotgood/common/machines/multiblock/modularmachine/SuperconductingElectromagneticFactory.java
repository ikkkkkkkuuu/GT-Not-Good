package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.client.render.ElectromagneticRotorRenderer;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.SuperconductingElectromagneticFactoryGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiModeMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperconductingElectromagneticStructure;
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
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class SuperconductingElectromagneticFactory extends
    GTNGMultiModeMachineBase<SuperconductingElectromagneticFactory> implements ISurvivalConstructable, IMTERenderer {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "fieldextruder/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active_glow");

    private IStructureDefinition<SuperconductingElectromagneticFactory> definition;
    private boolean rotorRunning;

    public SuperconductingElectromagneticFactory(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public SuperconductingElectromagneticFactory(String name) {
        super(name);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.electromagnetic_factory.name
        // # Superconducting Electromagnetic Factory
        // # zh_CN 超导电磁工厂
        return "gtng.electromagnetic_factory.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SuperconductingElectromagneticFactory(mName);
    }

    @Override
    public IStructureDefinition<SuperconductingElectromagneticFactory> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<SuperconductingElectromagneticFactory>builder()
                .addShape("main", transpose(SuperconductingElectromagneticStructure.shape()))
                .addElement('A', ParticleColliderCasing.Lafium.asElement())
                .addElement('B', AssemblyPlantCasing.TemperedGlass.asElement())
                .addElement('C',
                    GTStructureUtility.activeCoils(DimensionalFocusCasing.ImprovedSuperconductor.asElement()))
                .addElement('D',
                    GTStructureUtility.activeCoils(AssemblyPlantCasing.MolybdenumDisilicideCoil.asElement()))
                .addElement('E', GTStructureUtility.activeCoils(AssemblyPlantCasing.ElectrolyticCell.asElement()))
                .addElement('F', ofChain(
                    GTStructureUtility.ofHatchAdder(SuperconductingElectromagneticFactory::addModularHatchToMachineList,
                        ParticleColliderCasing.Lafium.getTextureId(), 1),
                    GTStructureUtility.ofHatchAdder(SuperconductingElectromagneticFactory::addMaintenanceToMachineList,
                        ParticleColliderCasing.Lafium.getTextureId(), 1),
                    buildHatchAdder(SuperconductingElectromagneticFactory.class)
                        .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy))
                        .casingIndex(ParticleColliderCasing.Lafium.getTextureId()).hint(1)
                        .buildAndChain(ofBlock(ParticleColliderCasing.Lafium.getBlock(),
                            ParticleColliderCasing.Lafium.getMetadata()))))
                .addElement('H', ParticleColliderCasing.AcceleratedPipeline.asElement()).build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, SuperconductingElectromagneticStructure.OFFSET_X,
            SuperconductingElectromagneticStructure.OFFSET_Y, SuperconductingElectromagneticStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, SuperconductingElectromagneticStructure.OFFSET_X,
                SuperconductingElectromagneticStructure.OFFSET_Y, SuperconductingElectromagneticStructure.OFFSET_Z,
                budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (
            !checkPiece("main", SuperconductingElectromagneticStructure.OFFSET_X,
                SuperconductingElectromagneticStructure.OFFSET_Y, SuperconductingElectromagneticStructure.OFFSET_Z,
                errors)
        ) return;
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasInputHatch(errors);
        checkHasOutputHatch(errors);
        checkHasAnyEnergy(errors);
        checkModularHatches(errors);
    }

    @Override
    protected int getMachineModeCount() {
        return 5;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) updateRotor(tile);
        else ElectromagneticRotorRenderer.update(this);
    }

    @Override
    public void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        if (tile.isServerSide()) updateRotor(tile);
    }

    private void updateRotor(IGregTechTileEntity tile) {
        boolean running = mMachine && tile.isActive() && mMaxProgresstime > 0;
        if (rotorRunning == running) return;
        rotorRunning = running;
        tile.issueTileUpdate();
    }

    public boolean isRotorRunning() {
        return rotorRunning;
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeBoolean(rotorRunning);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        rotorRunning = buffer.readBoolean();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        int[] offset = new int[3];
        getExtendedFacing().getWorldOffset(new int[] { 0, -10, 7 }, offset);
        return AxisAlignedBB.getBoundingBox(x + offset[0] - 10, y + offset[1] - 10, z + offset[2] - 10,
            x + offset[0] + 11, y + offset[1] + 11, z + offset[2] + 11);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256D * 256D;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float partialTicks) {
        ElectromagneticRotorRenderer.render(this, x, y, z, partialTicks);
    }

    @Override
    public void onRemoval() {
        clearRotor();
        super.onRemoval();
    }

    @Override
    public void onUnload() {
        clearRotor();
        super.onUnload();
    }

    private void clearRotor() {
        rotorRunning = false;
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) ElectromagneticRotorRenderer.remove(this);
    }

    @Override
    protected RecipeMap<?> getRecipeMap(int mode) {
        return switch (mode) {
            case 1 -> RecipeMaps.electroMagneticSeparatorRecipes;
            case 2 -> RecipeMaps.electrolyzerNonCellRecipes;
            case 3 -> GTNGRecipeMaps.LightningProcessorRecipes;
            case 4 -> RecipeMaps.arcFurnaceRecipes;
            default -> RecipeMaps.polarizerRecipes;
        };
    }

    @Override
    public String getMachineModeKey() {
        // #tr gtng.electromagnetic_factory.mode.separator
        // # Electromagnetic Separator
        // # zh_CN 电磁分离
        if (machineMode == 1) return "gtng.electromagnetic_factory.mode.separator";
        // #tr gtng.electromagnetic_factory.mode.electrolyzer
        // # Electrolyzer
        // # zh_CN 电解
        if (machineMode == 2) return "gtng.electromagnetic_factory.mode.electrolyzer";
        // #tr gtng.electromagnetic_factory.mode.lightning
        // # Lightning Processor
        // # zh_CN 闪电处理
        if (machineMode == 3) return "gtng.electromagnetic_factory.mode.lightning";
        // #tr gtng.electromagnetic_factory.mode.arc
        // # Arc Furnace
        // # zh_CN 电弧炉
        if (machineMode == 4) return "gtng.electromagnetic_factory.mode.arc";
        // #tr gtng.electromagnetic_factory.mode.polarizer
        // # Polarizer
        // # zh_CN 磁化
        return "gtng.electromagnetic_factory.mode.polarizer";
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new SuperconductingElectromagneticFactoryGui(this);
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
        // #tr gtng.electromagnetic_factory.type
        // # Electromagnetic Polarizer, Electromagnetic Separator, Electrolyzer, Lightning Processing, Arc Furnace
        // # zh_CN 磁化机，电磁分离机，电解机，闪电处理，电弧炉
        addModularMachineType(tt, StatCollector.translateToLocal("gtng.electromagnetic_factory.type"))
            // #tr gtng.electromagnetic_factory.function
            // # Polarizing, electromagnetic separation, electrolysis, lightning processing and arc smelting
            // # zh_CN 可切换磁化、电磁分离、电解、闪电处理和电弧炉模式
            .addInfo(StatCollector.translateToLocal("gtng.electromagnetic_factory.function"))
            .addInfo(StatCollector.translateToLocal("gtng.assembly_plant.switch"));
        addModularTooltip(tt);
        tt.beginStructureBlock(SuperconductingElectromagneticStructure.WIDTH,
            SuperconductingElectromagneticStructure.HEIGHT, SuperconductingElectromagneticStructure.LENGTH, false)
            // #tr gtng.electromagnetic_factory.controller
            // # Front center, bottom layer
            // # zh_CN 正面中央，最底层
            .addController(StatCollector.translateToLocal("gtng.electromagnetic_factory.controller"))
            .addCasing("724+", ParticleColliderCasing.Lafium.getStack(1).getDisplayName(), false)
            .addCasing("364", AssemblyPlantCasing.TemperedGlass.getStack(1).getDisplayName(), false)
            .addCasing("316", DimensionalFocusCasing.ImprovedSuperconductor.getStack(1).getDisplayName(), false)
            .addCasing("24", AssemblyPlantCasing.MolybdenumDisilicideCoil.getStack(1).getDisplayName(), false)
            .addCasing("156", AssemblyPlantCasing.ElectrolyticCell.getStack(1).getDisplayName(), false)
            .addCasing("19", ParticleColliderCasing.AcceleratedPipeline.getStack(1).getDisplayName(), false)
            // #tr gtng.electromagnetic_factory.hatch
            // # Lafium Casings (hint 1)
            // # zh_CN 路菲恩机械方块（提示点 1）
            .addInputBus("1+", StatCollector.translateToLocal("gtng.electromagnetic_factory.hatch"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("gtng.electromagnetic_factory.hatch"), 1)
            .addInputHatch("1+", StatCollector.translateToLocal("gtng.electromagnetic_factory.hatch"), 1)
            .addOutputHatch("1+", StatCollector.translateToLocal("gtng.electromagnetic_factory.hatch"), 1)
            .addEnergyHatch("1+", StatCollector.translateToLocal("gtng.electromagnetic_factory.hatch"), 1);
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }
}
