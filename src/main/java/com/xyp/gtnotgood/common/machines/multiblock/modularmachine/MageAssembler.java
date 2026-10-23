package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.MageAssemblerGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiModeMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.MageAssemblerStructure;
import com.xyp.gtnotgood.utils.enums.ModList;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gtPlusPlus.core.material.MaterialsAlloy;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class MageAssembler extends GTNGMultiModeMachineBase<MageAssembler> implements ISurvivalConstructable {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "assemblyplant/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "assemblyplant/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "assemblyplant/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "assemblyplant/controller_front_active_glow");

    private IStructureDefinition<MageAssembler> definition;
    private int casingCount;

    public MageAssembler(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public MageAssembler(String name) {
        super(name);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.mage_assembler.name
        // # Mega Assembly Plant
        // # zh_CN 巨型组装厂
        return "gtng.mage_assembler.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MageAssembler(mName);
    }

    @Override
    public IStructureDefinition<MageAssembler> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<MageAssembler>builder()
                .addElement('-',
                    buildHatchAdder(MageAssembler.class).atLeast(Muffler)
                        .casingIndex(FieldExtruderCasing.Iridium.getTextureId()).hint(2).build())
                .addShape("main", transpose(MageAssemblerStructure.shape()))
                .addElement('a',
                    ofChain(
                        GTStructureUtility.ofHatchAdder(MageAssembler::addModularHatchToMachineList,
                            FieldExtruderCasing.Iridium.getTextureId(), 1),
                        GTStructureUtility.ofHatchAdder(MageAssembler::addMaintenanceToMachineList,
                            FieldExtruderCasing.Iridium.getTextureId(), 1),
                        buildHatchAdder(MageAssembler.class)
                            .atLeast(InputBus, OutputBus, InputHatch, Energy.or(ExoticEnergy))
                            .casingIndex(FieldExtruderCasing.Iridium.getTextureId()).hint(1)
                            .buildAndChain(onElementPass(machine -> machine.casingCount++,
                                FieldExtruderCasing.Iridium.asElement()))))
                .addElement('c', AssemblyPlantCasing.LaminatedGlass.asElement())
                .addElement('d', AssemblyPlantCasing.FilterCasing.asElement())
                .addElement('e', GTStructureUtility.ofFrame(MaterialsAlloy.HASTELLOY_N))
                .addElement('f',
                    GTStructureUtility.activeCoils(AssemblyPlantCasing.AdvancedAssemblyLineUnit.asElement()))
                .addElement('g', GTStructureUtility.ofFrame(Materials.TungstenSteel))
                .addElement('h', AssemblyPlantCasing.SpacetimeAssemblyLineUnit.asElement())
                .addElement('i', AssemblyPlantCasing.PtfePipeCasing.asElement()).build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, MageAssemblerStructure.OFFSET_X, MageAssemblerStructure.OFFSET_Y,
            MageAssemblerStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, MageAssemblerStructure.OFFSET_X, MageAssemblerStructure.OFFSET_Y,
                MageAssemblerStructure.OFFSET_Z, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (
            !checkPiece("main", MageAssemblerStructure.OFFSET_X, MageAssemblerStructure.OFFSET_Y,
                MageAssemblerStructure.OFFSET_Z, errors)
        ) return;
        checkCasingMin(errors, casingCount, 666);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasInputHatch(errors);
        checkHatchExact(errors, Muffler, 1);
        checkHasAnyEnergy(errors);
        checkModularHatches(errors);
    }

    @Override
    protected int getMachineModeCount() {
        return 3;
    }

    @Override
    protected RecipeMap<?> getRecipeMap(int mode) {
        return switch (mode) {
            case 1 -> GoodGeneratorRecipeMaps.preciseAssemblerRecipes;
            case 2 -> RecipeMaps.circuitAssemblerRecipes;
            default -> RecipeMaps.assemblerRecipes;
        };
    }

    @Override
    public String getMachineModeKey() {
        // #tr gtng.mage_assembler.mode.precise
        // # Precise Assembler
        // # zh_CN 精密组装
        if (machineMode == 1) return "gtng.mage_assembler.mode.precise";
        // #tr gtng.mage_assembler.mode.circuit
        // # Circuit Assembler
        // # zh_CN 电路组装
        if (machineMode == 2) return "gtng.mage_assembler.mode.circuit";
        // #tr gtng.mage_assembler.mode.assembler
        // # Assembler
        // # zh_CN 组装
        return "gtng.mage_assembler.mode.assembler";
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new MageAssemblerGui(this);
    }

    @Override
    protected CheckRecipeResult validateRecipeForMode(GTRecipe recipe) {
        return machineMode == 1 && recipe.mSpecialValue > 4
            ? CheckRecipeResultRegistry.insufficientMachineTier(recipe.mSpecialValue)
            : CheckRecipeResultRegistry.SUCCESSFUL;
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
        // #tr gtng.mage_assembler.type
        // # Assembler, Precise Assembler, Circuit Assembler
        // # zh_CN 组装机，精密组装机，电路组装机
        addModularMachineType(tt, StatCollector.translateToLocal("gtng.mage_assembler.type"))
            // #tr gtng.mage_assembler.function
            // # Processes assembly recipes in three selectable modes
            // # zh_CN 可切换三种组装模式
            .addInfo(StatCollector.translateToLocal("gtng.mage_assembler.function"))
            // #tr gtng.mage_assembler.precise_tier
            // # Supports precise assembly casing tiers up to 4
            // # zh_CN 支持最高 4 级精密组装配方
            .addInfo(StatCollector.translateToLocal("gtng.mage_assembler.precise_tier"))
            // #tr gtng.mage_assembler.cleanroom
            // # Cleanroom recipes require a valid room with positive cleanness
            // # zh_CN 洁净室配方需要有效且洁净的洁净室
            .addInfo(StatCollector.translateToLocal("gtng.mage_assembler.cleanroom"))
            // #tr gtng.assembly_plant.switch
            // # Switch modes with a screwdriver or the GUI
            // # zh_CN 使用螺丝刀或 GUI 切换加工模式
            .addInfo(StatCollector.translateToLocal("gtng.assembly_plant.switch"));
        addModularTooltip(tt);
        tt.beginStructureBlock(MageAssemblerStructure.WIDTH, MageAssemblerStructure.HEIGHT,
            MageAssemblerStructure.LENGTH, false)
            // #tr gtng.mage_assembler.controller
            // # Front center, sixth layer from the top
            // # zh_CN 正面中央，自顶向下第 6 层
            .addController(StatCollector.translateToLocal("gtng.mage_assembler.controller"))
            .addCasing("666+", FieldExtruderCasing.Iridium.getStack(1).getDisplayName(), false)
            .addCasing("280", AssemblyPlantCasing.LaminatedGlass.getStack(1).getDisplayName(), false)
            .addCasing("124", AssemblyPlantCasing.FilterCasing.getStack(1).getDisplayName(), false)
            .addCasing("30", MaterialsAlloy.HASTELLOY_N.getFrameBox(1).getDisplayName(), false)
            .addCasing("55", AssemblyPlantCasing.AdvancedAssemblyLineUnit.getStack(1).getDisplayName(), false)
            // #tr gtng.mage_assembler.frame
            // # Tungstensteel Frame Box
            // # zh_CN 钨钢框架
            .addCasing("4", StatCollector.translateToLocal("gtng.mage_assembler.frame"), false)
            .addCasing("27", AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getStack(1).getDisplayName(), false)
            .addCasing("126", AssemblyPlantCasing.PtfePipeCasing.getStack(1).getDisplayName(), false)
            // #tr gtng.mage_assembler.hatch
            // # Iridium Casings (hint 1)
            // # zh_CN 铱强化机械方块（提示点 1）
            .addInputBus("1+", StatCollector.translateToLocal("gtng.mage_assembler.hatch"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("gtng.mage_assembler.hatch"), 1)
            .addInputHatch("1+", StatCollector.translateToLocal("gtng.mage_assembler.hatch"), 1)
            .addEnergyHatch("1+", StatCollector.translateToLocal("gtng.mage_assembler.hatch"), 1)
            // #tr gtng.mage_assembler.muffler
            // # Rear center, sixth layer from the top (hint 2)
            // # zh_CN 背面中央，自顶向下第 6 层（提示点 2）
            .addMufflerHatch("1", StatCollector.translateToLocal("gtng.mage_assembler.muffler"), 2);
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }
}
