package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.FieldExtruderFactoryGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.FieldExtruderStructure;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.HatchElement;
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
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class FieldExtruderFactory extends GTNGModularMachineBase<FieldExtruderFactory>
    implements ISurvivalConstructable {

    private static final int MODE_EXTRUDER = 0;
    private static final int MODE_COMPRESSOR = 1;
    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "fieldextruder/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active_glow");

    private IStructureDefinition<FieldExtruderFactory> definition;
    private int casingCount;

    public FieldExtruderFactory(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public FieldExtruderFactory(String name) {
        super(name);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.field_extruder.name
        // # Field Extruder Factory
        // # zh_CN 力场压模工厂
        return "gtng.field_extruder.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new FieldExtruderFactory(mName);
    }

    @Override
    public IStructureDefinition<FieldExtruderFactory> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<FieldExtruderFactory>builder()
                .addShape("main", transpose(FieldExtruderStructure.shape()))
                .addElement('A', ofChain(
                    GTStructureUtility.ofHatchAdder(FieldExtruderFactory::addModularHatchToMachineList,
                        FieldExtruderCasing.Iridium.getTextureId(), 1),
                    GTStructureUtility.ofHatchAdder(FieldExtruderFactory::addMaintenanceToMachineList,
                        FieldExtruderCasing.Iridium.getTextureId(), 1),
                    buildHatchAdder(FieldExtruderFactory.class).atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy))
                        .casingIndex(FieldExtruderCasing.Iridium.getTextureId()).hint(1)
                        .buildAndChain(onElementPass(machine -> machine.casingCount++,
                            ofBlock(FieldExtruderCasing.Iridium.getBlock(),
                                FieldExtruderCasing.Iridium.getMetadata())))))
                .addElement('B',
                    ofBlock(FieldExtruderCasing.HyperMechanical.getBlock(),
                        FieldExtruderCasing.HyperMechanical.getMetadata()))
                .addElement('C', GTStructureUtility.ofFrame(Materials.Naquadria))
                .addElement('D',
                    ofBlock(FieldExtruderCasing.Hollow.getBlock(), FieldExtruderCasing.Hollow.getMetadata()))
                .addElement('E',
                    ofBlock(FieldExtruderCasing.Molecular.getBlock(), FieldExtruderCasing.Molecular.getMetadata()))
                .addElement('F',
                    ofBlock(FieldExtruderCasing.ContainmentFieldGenerator.getBlock(),
                        FieldExtruderCasing.ContainmentFieldGenerator.getMetadata()))
                .addElement('G', ofBlock(FieldExtruderCasing.CleanroomGlass.getBlock(),
                    FieldExtruderCasing.CleanroomGlass.getMetadata()))
                .build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, FieldExtruderStructure.OFFSET_X, FieldExtruderStructure.OFFSET_Y,
            FieldExtruderStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, FieldExtruderStructure.OFFSET_X, FieldExtruderStructure.OFFSET_Y,
                FieldExtruderStructure.OFFSET_Z, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (
            !checkPiece("main", FieldExtruderStructure.OFFSET_X, FieldExtruderStructure.OFFSET_Y,
                FieldExtruderStructure.OFFSET_Z, errors)
        ) return;
        checkCasingMin(errors, casingCount, 676);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasAnyEnergy(errors);
        checkMaximum(errors, InputBus, mInputBusses.size() + mDualInputHatches.size(), 24);
        checkMaximum(errors, OutputBus, mOutputBusses.size(), 24);
        checkModularHatches(errors);
    }

    private static void checkMaximum(List<StructureError> errors, HatchElement element, int count, int maximum) {
        if (count > maximum) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, element, count, maximum));
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return machineMode == MODE_COMPRESSOR ? RecipeMaps.compressorRecipes : RecipeMaps.extruderRecipes;
    }

    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(RecipeMaps.extruderRecipes, RecipeMaps.compressorRecipes);
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new FieldExtruderFactoryGui(this);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return machineMode == MODE_EXTRUDER ? MODE_COMPRESSOR : MODE_EXTRUDER;
    }

    @Override
    public void setMachineMode(int index) {
        super.setMachineMode(index == MODE_COMPRESSOR ? MODE_COMPRESSOR : MODE_EXTRUDER);
        for (var bus : mInputBusses) bus.mRecipeMap = getRecipeMap();
    }

    @Override
    public String getMachineModeKey() {
        // #tr gtng.field_extruder.mode.compressor
        // # Compressor
        // # zh_CN 压缩
        if (machineMode == MODE_COMPRESSOR) return "gtng.field_extruder.mode.compressor";
        // #tr gtng.field_extruder.mode.extruder
        // # Extruder
        // # zh_CN 压模
        return "gtng.field_extruder.mode.extruder";
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (getBaseMetaTileEntity().isClientSide()) return;
        if (player.isSneaking()) super.onScrewdriverRightClick(side, player, x, y, z, tool);
        else {
            setMachineMode(nextMachineMode());
            GTUtility.sendChatTrans(player, "gt.interact.desc.mb.mode", getMachineModeName());
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        machineMode = machineMode == MODE_COMPRESSOR ? MODE_COMPRESSOR : MODE_EXTRUDER;
        batchMode = false;
        mLockedToSingleRecipe = false;
        setSingleRecipeCheck(null);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = FieldExtruderCasing.Iridium.getTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE_GLOW : FRONT_GLOW).extFacing().glow().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.field_extruder.machine_type
        // # Extruder, Compressor
        // # zh_CN 压模机，压缩机
        addModularMachineType(tt, StatCollector.translateToLocal("gtng.field_extruder.machine_type"))
            // #tr gtng.field_extruder.function
            // # Processes extrusion or compressor recipes in the selected mode
            // # zh_CN 处理当前模式下的压模或压缩配方
            .addInfo(StatCollector.translateToLocal("gtng.field_extruder.function"))
            // #tr gtng.field_extruder.switch
            // # Switch modes with a screwdriver or the GUI
            // # zh_CN 使用螺丝刀或 GUI 切换加工模式
            .addInfo(StatCollector.translateToLocal("gtng.field_extruder.switch"));
        addModularTooltip(tt);
        tt.beginStructureBlock(FieldExtruderStructure.WIDTH, FieldExtruderStructure.HEIGHT,
            FieldExtruderStructure.LENGTH, false)
            // #tr gtng.field_extruder.controller
            // # Front center, second layer from the top
            // # zh_CN 正面中央，自顶向下第 2 层
            .addController(StatCollector.translateToLocal("gtng.field_extruder.controller"))
            .addCasing("676+", FieldExtruderCasing.Iridium.getStack(1).getDisplayName(), false)
            .addCasing("432", FieldExtruderCasing.HyperMechanical.getStack(1).getDisplayName(), false)
            // #tr gtng.field_extruder.frame
            // # Naquadria Frame Box
            // # zh_CN 超能硅岩框架
            .addCasing("928", StatCollector.translateToLocal("gtng.field_extruder.frame"), false)
            .addCasing("144", FieldExtruderCasing.Hollow.getStack(1).getDisplayName(), false)
            .addCasing("48", FieldExtruderCasing.Molecular.getStack(1).getDisplayName(), false)
            .addCasing("16", FieldExtruderCasing.ContainmentFieldGenerator.getStack(1).getDisplayName(), false)
            .addCasing("302", FieldExtruderCasing.CleanroomGlass.getStack(1).getDisplayName(), false)
            // #tr gtng.field_extruder.hatch_location
            // # Iridium Casings (hint 1)
            // # zh_CN 铱强化机械方块（提示点 1）
            .addEnergyHatch("1+", StatCollector.translateToLocal("gtng.field_extruder.hatch_location"), 1)
            .addInputBus("1-24", FieldExtruderCasing.Iridium.getStack(1).getDisplayName(), 1)
            .addOutputBus("1-24", FieldExtruderCasing.Iridium.getStack(1).getDisplayName(), 1);
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }
}
