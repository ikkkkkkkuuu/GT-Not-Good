package com.xyp.gtnotgood.utils.enums;

import static gregtech.api.enums.GTValues.NI;
import static gregtech.api.enums.ItemList.Machine_LV_Miner;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.IItemContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.GTLanguageManager;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public enum GTNGItemList implements IItemContainer {

    StructureCompass,
    WorkingApiary,
    IronElectrode,

    SuperradiantMirrorCasing,
    MirrorBackingCasing,
    BoltInjectorRailMkI,
    BoltInjectorRailMkII,
    BoltInjectorRailMkIII,
    ErgosphereFieldCoil,
    BeamFocusingQuadrupole,
    BeamDump,
    BeamBandCasing,
    JetCrescentCasing,
    GalacticShellPlating,
    BastionPlate,
    CasimirPlate,
    ErgosphereExtractionCasing,
    KerrCompressorModuleCore,
    KerrExtractorModuleCore,
    HarmonicLining,
    DiscFieldCoilTierI,
    DiscFieldCoilTierII,
    DiscFieldCoilTierIII,
    DiscFieldCoilTierIV,
    EddingtonWallCasing,
    RimGasInjector,
    JetCollimatorRing,
    BurstThermaliserCasing,
    RadiatorPetal,
    StirringBandCasing,
    ShearHelixCoil,
    FeedVesselCasing,
    VortexInlet,
    RegionDeckBand,
    RegionCrystal,
    PhaseRingCasing,
    ReassemblyHullPlating,
    ReassemblyTrayPlating,
    ReassemblyClaw,
    EternityShellCasing,
    RegionAnchorCasing,
    LatticeFrameCasing,
    ContainmentNodeSuperdenseErgomatter,
    ContainmentNodeAttractorDarkMatter,
    ContainmentNodeUniversium,
    ContainmentNodeConstrainedEternity,
    EventHorizonThermaliserCasing,
    UniversalCompressionFieldGenerator,
    DarkMatterHeatingCoil,
    AttractorDarkMatterFrameBox,
    MagnetarReinforcedBorosilicateGlass,
    DarkMatterBorosilicateGlass,
    ReactorFuelPillarFrame,
    FueledReactorFuelPillarFrame,
    RodLoadingPort,
    ClockwiseTurbineHousing,
    AnticlockwiseTurbineHousing,

    BurstCrownPanel,
    BurstCrownActivePanel,
    ConduitJunctionPanel,
    ConduitJunctionActivePanel,
    ConduitMirrorPanel,
    ConduitMirrorActivePanel,
    EyeOfHarmonyPanel,
    EyeOfHarmonyActivePanel,
    HomogenyPanel,
    HomogenyActivePanel,
    KerrCompressorPanel,
    KerrCompressorActivePanel,
    KerrEnginePanel,
    KerrEngineActivePanel,
    KerrExtractorPanel,
    KerrExtractorActivePanel,
    QuasarEnginePanel,
    QuasarEngineActivePanel,
    RealityDiffractionPanel,
    RealityDiffractionActivePanel,
    RealityRefabricationPanel,
    RealityRefabricationActivePanel,
    WormholeMouthPanel,
    WormholeMouthActivePanel,
    BurstControlHatchPanel,
    DirectiveHatchPanel,
    GrbReceptorHatchPanel,
    StellarFieldSensorHatchPanel,

    LargeCombProcessor,
    LargeTransmutationMachine,
    EssentiaDisassembler,
    UniversalFluidPump,
    SteamChemicalReactor,
    SteamCentrifuge,
    SteamElectrolyzer,
    SteamCropManager,
    SteamAssembler,
    SeedConverter,
    BasicIndustrialApiary,

    WirelessPackagedPatternProvider,
    ItemWirelessConnector,
    BasicPackagedCore,
    AssemblyLineCore,
    CircuitAssemblyLineCore,
    BloodAltarCore,
    AdvancedAssemblyLineCore,
    ThaumcraftInfusionCore,
    ArcaneWorkbenchCore,
    ThaumcraftCrucibleCore,

    MechanicalUser,
    MechanicalUserSpeedUpgrade,
    WebScreen,

    NetworkController,
    NetworkPipe,
    NetworkConnector,
    FluxPlug,
    FluxPoint,
    FluxLogisticsPlug,

    VaultPortHatch,
    CrossRecipeWirelessEnergyHatch,
    CircuitMEPatternBuffer,
    CircuitMEPatternMirror,
    WirelessMultiAmpEnergyLV256A,
    WirelessMultiAmpEnergyLV1024A,
    WirelessMultiAmpEnergyLV4096A,
    WirelessMultiAmpEnergyLV16384A,
    WirelessMultiAmpEnergyLV65536A,
    WirelessMultiAmpEnergyLV262144A,
    WirelessMultiAmpEnergyLV1048576A,
    WirelessLaserDynamoLV,
    WirelessMultiAmpEnergyMV256A,
    WirelessMultiAmpEnergyMV1024A,
    WirelessMultiAmpEnergyMV4096A,
    WirelessMultiAmpEnergyMV16384A,
    WirelessMultiAmpEnergyMV65536A,
    WirelessMultiAmpEnergyMV262144A,
    WirelessMultiAmpEnergyMV1048576A,
    WirelessLaserDynamoMV,
    WirelessMultiAmpEnergyHV256A,
    WirelessMultiAmpEnergyHV1024A,
    WirelessMultiAmpEnergyHV4096A,
    WirelessMultiAmpEnergyHV16384A,
    WirelessMultiAmpEnergyHV65536A,
    WirelessMultiAmpEnergyHV262144A,
    WirelessMultiAmpEnergyHV1048576A,
    WirelessLaserDynamoHV,
    WirelessMultiAmpEnergyEV256A,
    WirelessMultiAmpEnergyEV1024A,
    WirelessMultiAmpEnergyEV4096A,
    WirelessMultiAmpEnergyEV16384A,
    WirelessMultiAmpEnergyEV65536A,
    WirelessMultiAmpEnergyEV262144A,
    WirelessMultiAmpEnergyEV1048576A,
    WirelessLaserDynamoEV,
    WirelessMultiAmpEnergyIV256A,
    WirelessMultiAmpEnergyIV1024A,
    WirelessMultiAmpEnergyIV4096A,
    WirelessMultiAmpEnergyIV16384A,
    WirelessMultiAmpEnergyIV65536A,
    WirelessMultiAmpEnergyIV262144A,
    WirelessMultiAmpEnergyIV1048576A,
    WirelessLaserDynamoIV,
    WirelessMultiAmpEnergyLuV256A,
    WirelessMultiAmpEnergyLuV1024A,
    WirelessMultiAmpEnergyLuV4096A,
    WirelessMultiAmpEnergyLuV16384A,
    WirelessMultiAmpEnergyLuV65536A,
    WirelessMultiAmpEnergyLuV262144A,
    WirelessMultiAmpEnergyLuV1048576A,
    WirelessLaserDynamoLuV,
    WirelessMultiAmpEnergyZPM256A,
    WirelessMultiAmpEnergyZPM1024A,
    WirelessMultiAmpEnergyZPM4096A,
    WirelessMultiAmpEnergyZPM16384A,
    WirelessMultiAmpEnergyZPM65536A,
    WirelessMultiAmpEnergyZPM262144A,
    WirelessMultiAmpEnergyZPM1048576A,
    WirelessLaserDynamoZPM,
    WirelessMultiAmpEnergyUV256A,
    WirelessMultiAmpEnergyUV1024A,
    WirelessMultiAmpEnergyUV4096A,
    WirelessMultiAmpEnergyUV16384A,
    WirelessMultiAmpEnergyUV65536A,
    WirelessMultiAmpEnergyUV262144A,
    WirelessMultiAmpEnergyUV1048576A,
    WirelessLaserDynamoUV,
    WirelessMultiAmpEnergyUHV256A,
    WirelessMultiAmpEnergyUHV1024A,
    WirelessMultiAmpEnergyUHV4096A,
    WirelessMultiAmpEnergyUHV16384A,
    WirelessMultiAmpEnergyUHV65536A,
    WirelessMultiAmpEnergyUHV262144A,
    WirelessMultiAmpEnergyUHV1048576A,
    WirelessLaserDynamoUHV,
    WirelessMultiAmpEnergyUEV256A,
    WirelessMultiAmpEnergyUEV1024A,
    WirelessMultiAmpEnergyUEV4096A,
    WirelessMultiAmpEnergyUEV16384A,
    WirelessMultiAmpEnergyUEV65536A,
    WirelessMultiAmpEnergyUEV262144A,
    WirelessMultiAmpEnergyUEV1048576A,
    WirelessLaserDynamoUEV,
    WirelessMultiAmpEnergyUIV256A,
    WirelessMultiAmpEnergyUIV1024A,
    WirelessMultiAmpEnergyUIV4096A,
    WirelessMultiAmpEnergyUIV16384A,
    WirelessMultiAmpEnergyUIV65536A,
    WirelessMultiAmpEnergyUIV262144A,
    WirelessMultiAmpEnergyUIV1048576A,
    WirelessLaserDynamoUIV,
    WirelessMultiAmpEnergyUMV256A,
    WirelessMultiAmpEnergyUMV1024A,
    WirelessMultiAmpEnergyUMV4096A,
    WirelessMultiAmpEnergyUMV16384A,
    WirelessMultiAmpEnergyUMV65536A,
    WirelessMultiAmpEnergyUMV262144A,
    WirelessMultiAmpEnergyUMV1048576A,
    WirelessLaserDynamoUMV,
    WirelessMultiAmpEnergyUXV256A,
    WirelessMultiAmpEnergyUXV1024A,
    WirelessMultiAmpEnergyUXV4096A,
    WirelessMultiAmpEnergyUXV16384A,
    WirelessMultiAmpEnergyUXV65536A,
    WirelessMultiAmpEnergyUXV262144A,
    WirelessMultiAmpEnergyUXV1048576A,
    WirelessLaserDynamoUXV,
    WirelessMultiAmpEnergyMAX256A,
    WirelessMultiAmpEnergyMAX1024A,
    WirelessMultiAmpEnergyMAX4096A,
    WirelessMultiAmpEnergyMAX16384A,
    WirelessMultiAmpEnergyMAX65536A,
    WirelessMultiAmpEnergyMAX262144A,
    WirelessMultiAmpEnergyMAX1048576A,
    WirelessLaserDynamoMAX,
    SingularityDataHub,

    SteamTurbineLV,
    SteamTurbineMV,
    SteamTurbineHV,
    SteamTurbineEV,
    SteamTurbineIV,
    SteamTurbineLuV,

    VeinMiningPickaxe,
    LargeOreProcessor,
    QuantumComputer,
    AssemblerMatrix,
    PrecisionAssembler,
    LargeVoidMiner,
    LargeVoidDriller,
    LargeBeeBreeder,
    LargeIndustrialApiary,
    LargeCropBreeder,
    LargeIndustrialFarm,
    IntegratedProductionFactory,
    IntegratedChemicalPlant,
    MaxCapacityMEOutputBus,
    MaxCapacityMEOutputHatch,
    MEOutputAssembly,
    MEDataAccessHatch,
    MEBridgeSender,
    MEBridgeReceiver,
    MEContainer,
    StockIOInterface,
    StockIOInterfacePart,
    LargeInterface,
    LargeInterfacePart,
    AdvancedIOBus,
    ThresholdExportBus,
    ThresholdLevelEmitter,
    MERequester,
    MERequesterTerminal,
    MEWirelessTransceiver,
    WirelessFrequencyCard,
    WildcardPattern,
    PatternSorter,
    IronFuelRod,
    DepletedIronFuelRod,
    WirelessDualInterfaceTerminal,
    SuperMTEHatchCraftingInputBusME,
    SuperMTEHatchCraftingInputME,
    CompactSuperMTEHatchCraftingInputME,
    SuperMTEHatchCraftingInputSlave,
    SuperAdvancedMEInputHatch,
    SuperAdvancedMEInputBus;

    public boolean mHasNotBeenSet;
    public boolean mDeprecated;
    public boolean mWarned;

    public ItemStack mStack;

    GTNGItemList() {
        mHasNotBeenSet = true;
    }

    GTNGItemList(boolean aDeprecated) {
        if (aDeprecated) {
            mDeprecated = true;
            mHasNotBeenSet = true;
        }
    }

    public void sanityCheck() {
        if (mHasNotBeenSet) {
            throw new IllegalAccessError("The Enum '" + name() + "' has not been set to an Item at this time!");
        }
        if (mDeprecated && !mWarned) {
            GTNotGood.LOG.error("{} is now deprecated", this, new Exception());
            mWarned = true;
        }
    }

    public Item getItem() {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) return null;
        return mStack.getItem();
    }

    public Block getBlock() {
        sanityCheck();
        return Block.getBlockFromItem(getItem());
    }

    @Override
    public ItemStack get(long aAmount, Object... aReplacements) {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) {
            GTNotGood.LOG.warn("Object in the GTNGItemList is null at:", new NullPointerException());
            return GTUtility.copyAmountUnsafe(Math.toIntExact(aAmount), Machine_LV_Miner.get(1));
        }
        return GTUtility.copyAmountUnsafe(Math.toIntExact(aAmount), mStack);
    }

    public ItemStack getWithMeta(long aAmount, int meta, Object... aReplacements) {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) {
            GTNotGood.LOG.warn("Object in the GTNGItemList is null at:", new NullPointerException());
            ItemStack fallback = Machine_LV_Miner.get(1);
            fallback.setItemDamage(meta);
            return GTUtility.copyAmountUnsafe(Math.toIntExact(aAmount), fallback);
        }

        ItemStack stack = GTUtility.copyAmountUnsafe(Math.toIntExact(aAmount), mStack);
        stack.setItemDamage(meta);
        return stack;
    }

    public int getWithMeta() {
        return mStack.getItemDamage();
    }

    public GTNGItemList set(Item aItem) {
        if (aItem == null) return this;
        return set(new ItemStack(aItem));
    }

    /**
     * Assigns an item stack and routes GregTech machines to their machine or wireless-hatch creative tab.
     * <p>
     * The stored stack is always copied to stack size one. If the assigned item belongs to
     * {@link GregTechAPI#sBlockMachines}, a copy is passed to {@link GTNGCreativeTabs#addToMachineList(ItemStack)} so
     * custom meta-tile entities automatically appear under the appropriate GT Not Good tab.
     *
     * @param aStack registered stack or meta-tile stack form
     * @return this enum constant for chained registration calls
     */
    public GTNGItemList set(ItemStack aStack) {
        if (aStack == null) return this;
        mHasNotBeenSet = false;
        mStack = GTUtility.copyAmountUnsafe(1, aStack);
        Item item = mStack.getItem();
        if (item != null && Block.getBlockFromItem(item) == GregTechAPI.sBlockMachines) {
            GTNGCreativeTabs.addToMachineList(mStack.copy());
        }
        return this;
    }

    /**
     * Assigns a GregTech meta-tile entity by storing its stack form.
     * <p>
     * Machine loaders should normally call this overload after constructing a controller, hatch, or single-block
     * machine. The stack then flows through {@link #set(ItemStack)}, which handles creative-tab insertion.
     *
     * @param metaTileEntity registered GregTech meta-tile entity
     * @return this enum constant for chained registration calls
     */
    public GTNGItemList set(IMetaTileEntity metaTileEntity) {
        if (metaTileEntity == null) throw new IllegalArgumentException();
        return set(metaTileEntity.getStackForm(1L));
    }

    public boolean hasBeenSet() {
        return !mHasNotBeenSet;
    }

    /**
     * Exposes the stored stack without copying it.
     * <p>
     * Prefer {@link #get(long, Object...)} for normal recipe and UI code. This method is intentionally marked unsafe
     * because callers can mutate the shared backing stack if they change the returned instance.
     *
     * @return internal backing stack reference
     */
    public ItemStack getInternalStackUnsafe() {
        return mStack;
    }

    @Override
    public boolean isStackEqual(Object aStack) {
        return isStackEqual(aStack, false, false);
    }

    @Override
    public boolean isStackEqual(Object aStack, boolean aWildcard, boolean aIgnoreNBT) {
        if (mDeprecated && !mWarned) {
            GTNotGood.LOG.error("{} is now deprecated", this, new Exception());
            mWarned = true;
        }
        if (GTUtility.isStackInvalid(aStack)) return false;
        return GTUtility.areUnificationsEqual((ItemStack) aStack, aWildcard ? getWildcard(1) : get(1), aIgnoreNBT);
    }

    @Override
    public ItemStack getWildcard(long aAmount, Object... aReplacements) {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) return GTUtility.copyAmount(aAmount, aReplacements);
        return GTUtility.copyAmountAndMetaData(aAmount, GTRecipeBuilder.WILDCARD, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getUndamaged(long aAmount, Object... aReplacements) {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) return GTUtility.copyAmount(aAmount, aReplacements);
        return GTUtility.copyAmountAndMetaData(aAmount, 0, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getAlmostBroken(long aAmount, Object... aReplacements) {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) return GTUtility.copyAmount(aAmount, aReplacements);
        return GTUtility.copyAmountAndMetaData(aAmount, mStack.getMaxDamage() - 1, GTOreDictUnificator.get(mStack));
    }

    /**
     * Returns a copy of this stack with an ad-hoc display name localization.
     * <p>
     * This follows GregTech's container contract for dynamic display names. The generated key is based on the item's
     * unlocalized name plus a camel-cased display-name suffix, and the localization is registered at runtime.
     *
     * @param aAmount       amount to copy
     * @param aDisplayName  display name to attach through GTLanguageManager
     * @param aReplacements fallback replacements used by the inherited item-container API
     * @return copied stack with the custom display name, or {@link gregtech.api.enums.GTValues#NI} on invalid stacks
     */
    @Override
    public ItemStack getWithName(long aAmount, String aDisplayName, Object... aReplacements) {
        ItemStack rStack = get(1, aReplacements);
        if (GTUtility.isStackInvalid(rStack)) return NI;

        StringBuilder tCamelCasedDisplayNameBuilder = new StringBuilder();
        final String[] tDisplayNameWords = aDisplayName.split("\\W");
        for (String tWord : tDisplayNameWords) {
            if (!tWord.isEmpty()) {
                tCamelCasedDisplayNameBuilder.append(tWord.substring(0, 1).toUpperCase(Locale.US));
            }
            if (tWord.length() > 1) {
                tCamelCasedDisplayNameBuilder.append(tWord.substring(1).toLowerCase(Locale.US));
            }
        }
        if (tCamelCasedDisplayNameBuilder.length() == 0) {
            tCamelCasedDisplayNameBuilder.append(((Long) (long) aDisplayName.hashCode()));
        }

        final String tKey = rStack.getUnlocalizedName() + ".with." + tCamelCasedDisplayNameBuilder + ".name";

        GTLanguageManager.addStringLocalization(tKey, aDisplayName);
        rStack.setStackDisplayName(StatCollector.translateToLocal(tKey));
        return GTUtility.copyAmount(aAmount, rStack);
    }

    @Override
    public ItemStack getWithCharge(long aAmount, int aEnergy, Object... aReplacements) {
        ItemStack rStack = get(1, aReplacements);
        if (GTUtility.isStackInvalid(rStack)) return null;
        GTModHandler.chargeElectricItem(rStack, aEnergy, Integer.MAX_VALUE, true, false);
        return GTUtility.copyAmount(aAmount, rStack);
    }

    @Override
    public ItemStack getWithDamage(long aAmount, long aMetaValue, Object... aReplacements) {
        sanityCheck();
        if (GTUtility.isStackInvalid(mStack)) return GTUtility.copyAmount(aAmount, aReplacements);
        return GTUtility.copyAmountAndMetaData(aAmount, aMetaValue, GTOreDictUnificator.get(mStack));
    }

    @Override
    public IItemContainer registerOre(Object... aOreNames) {
        sanityCheck();
        for (Object tOreName : aOreNames) {
            GTOreDictUnificator.registerOre(tOreName, get(1));
        }
        return this;
    }

    @Override
    public IItemContainer registerWildcardAsOre(Object... aOreNames) {
        sanityCheck();
        for (Object tOreName : aOreNames) {
            GTOreDictUnificator.registerOre(tOreName, getWildcard(1));
        }
        return this;
    }
}
