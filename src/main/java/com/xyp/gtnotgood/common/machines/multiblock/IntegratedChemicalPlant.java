package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
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
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.common.recipe.metadata.FuelRefiningTier;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import goodgenerator.loader.Loaders;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.GTStructureChannels;

public final class IntegratedChemicalPlant extends GTNGMultiBlockBase<IntegratedChemicalPlant>
    implements ISurvivalConstructable {

    private static final int MODE_FUEL = 0;
    private static final int MODE_WOOD = 1;
    private static final int MODE_PETROCHEMICAL = 2;
    private static final String[][] SHAPE = { { "FCCCF", "CCCCC", "CCCCC", "CCCCC", "FCCCF" },
        { "FGGGF", "GHHHG", "GH-HG", "GHHHG", "FGGGF" }, { "CC~CC", "GHHHG", "GHRHG", "GHHHG", "CCCCC" },
        { "FGGGF", "GHHHG", "GH-HG", "GHHHG", "FGGGF" }, { "FCCCF", "CCCCC", "CCCCC", "CCCCC", "FCCCF" } };

    private IStructureDefinition<IntegratedChemicalPlant> definition;
    private HeatingCoilLevel coilLevel = HeatingCoilLevel.None;
    private int casingCount;
    private int refiningTier;

    public IntegratedChemicalPlant(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public IntegratedChemicalPlant(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new IntegratedChemicalPlant(mName);
    }

    @Override
    public IStructureDefinition<IntegratedChemicalPlant> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<IntegratedChemicalPlant>builder();
            definition = builder
                .addShape("main",
                    transpose(SHAPE))
                .addElement('C',
                    ofChain(
                        buildHatchAdder(IntegratedChemicalPlant.class)
                            .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(Casings.ChemicallyInertMachineCasing.getTextureId()).hint(1).build(),
                        onElementPass(machine -> ++machine.casingCount,
                            Casings.ChemicallyInertMachineCasing.asElement())))
                .addElement('G', Casings.ReinforcedGlass.asElement())
                .addElement('F', GTStructureUtility.ofFrame(Materials.StainlessSteel))
                .addElement('H',
                    GTStructureChannels.HEATING_COIL.use(GTStructureUtility
                        .ofCoil((machine, level) -> { machine.coilLevel = level; }, machine -> machine.coilLevel)))
                .addElement('R',
                    ofChain(
                        onElementPass(machine -> machine.refiningTier = 0,
                            Casings.ChemicallyInertMachineCasing.asElement()),
                        onElementPass(machine -> machine.refiningTier = 1, ofBlock(Loaders.FRF_Coil_1, 0)),
                        onElementPass(machine -> machine.refiningTier = 2, ofBlock(Loaders.FRF_Coil_2, 0)),
                        onElementPass(machine -> machine.refiningTier = 3, ofBlock(Loaders.FRF_Coil_3, 0)),
                        onElementPass(machine -> machine.refiningTier = 4, ofBlock(Loaders.FRF_Coil_4, 0))))
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
        casingCount = 0;
        coilLevel = HeatingCoilLevel.None;
        refiningTier = 0;
        if (!checkPiece("main", 2, 2, 0, errors)) return;
        checkCasingMin(errors, casingCount, 32);
        if (mInputBusses.isEmpty() && mDualInputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(InputBus));
        if (mInputHatches.isEmpty() && mDualInputHatches.isEmpty())
            errors.add(StructureErrors.missingHatch(InputHatch));
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(OutputBus));
        if (mOutputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(OutputHatch));
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
    }

    public int getHeatingCapacity() {
        return (int) coilLevel.getHeat();
    }

    public int getRefiningTier() {
        return refiningTier;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return switch (machineMode) {
            case MODE_WOOD -> GTNGRecipeMaps.WoodDistillationRecipes;
            case MODE_PETROCHEMICAL -> GTNGRecipeMaps.PetrochemicalRecipes;
            default -> GTNGRecipeMaps.FuelRefiningRecipes;
        };
    }

    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(GTNGRecipeMaps.FuelRefiningRecipes, GTNGRecipeMaps.WoodDistillationRecipes,
            GTNGRecipeMaps.PetrochemicalRecipes);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return (machineMode + 1) % 3;
    }

    @Override
    public void setMachineMode(int index) {
        super.setMachineMode(normalizeMode(index));
        for (var hatch : mInputHatches) hatch.mRecipeMap = getRecipeMap();
        for (var bus : mInputBusses) bus.mRecipeMap = getRecipeMap();
    }

    private static int normalizeMode(int index) {
        return index >= MODE_FUEL && index <= MODE_PETROCHEMICAL ? index : MODE_FUEL;
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        machineMode = normalizeMode(machineMode);
    }

    @Override
    public String getMachineModeKey() {
        return switch (machineMode) {
            // #tr gtng.chemical.mode.wood
            // # Wood Distillation
            // # zh_CN 木化
            case MODE_WOOD -> "gtng.chemical.mode.wood";
            // #tr gtng.chemical.mode.petrochemical
            // # Petrochemical Processing
            // # zh_CN 石化
            case MODE_PETROCHEMICAL -> "gtng.chemical.mode.petrochemical";
            // #tr gtng.chemical.mode.fuel
            // # Fuel Refining
            // # zh_CN 燃料精炼
            default -> "gtng.chemical.mode.fuel";
        };
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
    @Nonnull
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new GTNGModernMultiBlockBaseGui<>(this).withMachineModeIcons(
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_CHEMBATH, GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DISTILLING,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DISTILLATION_TOWER);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (machineMode != MODE_FUEL) return CheckRecipeResultRegistry.SUCCESSFUL;
                int requiredTier = recipe.getMetadataOrDefault(FuelRefiningTier.INSTANCE, 0);
                if (requiredTier > refiningTier) return CheckRecipeResultRegistry.insufficientMachineTier(requiredTier);
                int requiredHeat = recipe.getMetadataOrDefault(COIL_HEAT, recipe.mSpecialValue);
                return requiredHeat <= getHeatingCapacity() ? CheckRecipeResultRegistry.SUCCESSFUL
                    : CheckRecipeResultRegistry.insufficientHeat(requiredHeat);
            }

            @Override
            @Nonnull
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                OverclockCalculator calculator = super.createOverclockCalculator(recipe);
                if (machineMode == MODE_FUEL)
                    calculator.setRecipeHeat(recipe.getMetadataOrDefault(COIL_HEAT, recipe.mSpecialValue))
                        .setMachineHeat(getHeatingCapacity());
                return calculator;
            }
        }.enablePerfectOverclock().setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    public int getMaxParallelRecipes() {
        return 16;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons
            .getCasingTextureForId(Casings.ChemicallyInertMachineCasing.getTextureId());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE
                : Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR).extFacing().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(StatCollector.translateToLocal("gtng.chemical.name"))
            // #tr gtng.chemical.tooltip.modes
            // # Three modes: fuel refining, wood distillation and petrochemical processing
            // # zh_CN 三种模式：燃料精炼、木化、石化
            .addInfo(StatCollector.translateToLocal("gtng.chemical.tooltip.modes"))
            // #tr gtng.chemical.tooltip.switch
            // # Switch modes with the GUI or screwdriver; sneak-screwdriver toggles recipe locking
            // # zh_CN GUI或螺丝刀切换模式；潜行使用螺丝刀切换配方锁定
            .addInfo(StatCollector.translateToLocal("gtng.chemical.tooltip.switch"))
            // #tr gtng.chemical.tooltip.processing
            // # Perfect overclocking, up to 16 parallels; outputs follow the displayed recipes
            // # zh_CN 无损超频，最多16并行；产量以配方显示为准
            .addInfo(StatCollector.translateToLocal("gtng.chemical.tooltip.processing"))
            // #tr gtng.chemical.tooltip.heat
            // # Fuel refining requires the recipe's coil temperature and refining core tier
            // # zh_CN 燃料精炼需要满足配方的线圈温度与精炼核心等级
            .addInfo(StatCollector.translateToLocal("gtng.chemical.tooltip.heat"))
            // #tr gtng.chemical.tooltip.core
            // # Center core: inert casing (tier 0) or Fuel Refinery Coil I-IV (tier 1-4)
            // # zh_CN 中央核心：化学惰性外壳（0级）或燃料精炼厂线圈I-IV（1-4级）
            .addInfo(StatCollector.translateToLocal("gtng.chemical.tooltip.core")).beginStructureBlock(5, 5, 5, false)
            // #tr gtng.chemical.tooltip.casing
            // # Chemically Inert Machine Casing (hatches may replace outer casing)
            // # zh_CN 化学惰性机械方块（仓室可替换外壳）
            .addCasing("32+", StatCollector.translateToLocal("gtng.chemical.tooltip.casing"), false)
            // #tr gtng.chemical.tooltip.structure
            // # 24 heating coils, 30 reinforced glass and 16 stainless steel frames
            // # zh_CN 24个加热线圈、30块强化玻璃、16个不锈钢框架
            .addInfo(StatCollector.translateToLocal("gtng.chemical.tooltip.structure")).addInputBus("1+", "C", 1)
            .addOutputBus("1+", "C", 1).addInputHatch("1+", "C", 1).addOutputHatch("1+", "C", 1)
            .addEnergyHatch("1+", "C", 1).addMaintenanceHatch(shouldCheckMaintenance() ? "1" : "0", "C", 1)
            .toolTipFinisher();
    }
}
