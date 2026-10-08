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

    LargeCombProcessor,
    LargeTransmutationMachine,
    EssentiaDisassembler,
    UniversalFluidPump,

    RtsControlCore,
    RemoteControlCore,
    StorageIntegrationPlugin,
    CraftTerminalPlugin,
    ChainBreakPlugin,
    AreaDestroyPlugin,
    BlueprintPlugin,
    RangeHidingPlugin,
    FieldDeploymentPlugin,
    RangeExtensionI,
    RangeExtensionII,
    RangeExtensionIII,
    RangeExtensionMax,
    StoneHarvestPlugin,
    IronHarvestPlugin,
    DiamondHarvestPlugin,
    UnlimitedHarvestPlugin,

    WirelessPackagedPatternProvider,
    ItemWirelessConnector,
    BasicPackagedCore,
    AssemblyLineCore,
    BloodAltarCore,
    AdvancedAssemblyLineCore,
    ThaumcraftInfusionCore,
    ArcaneWorkbenchCore,
    ThaumcraftCrucibleCore,

    MechanicalUser,
    MechanicalUserSpeedUpgrade,

    NetworkController,
    NetworkPipe,
    NetworkConnector,
    FluxPlug,
    FluxPoint,
    FluxLogisticsPlug,

    VaultPortHatch,
    CrossRecipeWirelessEnergyHatch,
    CircuitMEPatternBuffer,
    WirelessLaserEnergyLV256A,
    WirelessLaserEnergyLV1024A,
    WirelessLaserEnergyLV4096A,
    WirelessLaserEnergyLV16384A,
    WirelessLaserEnergyLV65536A,
    WirelessLaserEnergyLV262144A,
    WirelessLaserEnergyLV1048576A,
    WirelessLaserDynamoLV,
    WirelessLaserEnergyMV256A,
    WirelessLaserEnergyMV1024A,
    WirelessLaserEnergyMV4096A,
    WirelessLaserEnergyMV16384A,
    WirelessLaserEnergyMV65536A,
    WirelessLaserEnergyMV262144A,
    WirelessLaserEnergyMV1048576A,
    WirelessLaserDynamoMV,
    WirelessLaserEnergyHV256A,
    WirelessLaserEnergyHV1024A,
    WirelessLaserEnergyHV4096A,
    WirelessLaserEnergyHV16384A,
    WirelessLaserEnergyHV65536A,
    WirelessLaserEnergyHV262144A,
    WirelessLaserEnergyHV1048576A,
    WirelessLaserDynamoHV,
    WirelessLaserEnergyEV256A,
    WirelessLaserEnergyEV1024A,
    WirelessLaserEnergyEV4096A,
    WirelessLaserEnergyEV16384A,
    WirelessLaserEnergyEV65536A,
    WirelessLaserEnergyEV262144A,
    WirelessLaserEnergyEV1048576A,
    WirelessLaserDynamoEV,
    WirelessLaserEnergyIV256A,
    WirelessLaserEnergyIV1024A,
    WirelessLaserEnergyIV4096A,
    WirelessLaserEnergyIV16384A,
    WirelessLaserEnergyIV65536A,
    WirelessLaserEnergyIV262144A,
    WirelessLaserEnergyIV1048576A,
    WirelessLaserDynamoIV,
    WirelessLaserEnergyLuV256A,
    WirelessLaserEnergyLuV1024A,
    WirelessLaserEnergyLuV4096A,
    WirelessLaserEnergyLuV16384A,
    WirelessLaserEnergyLuV65536A,
    WirelessLaserEnergyLuV262144A,
    WirelessLaserEnergyLuV1048576A,
    WirelessLaserDynamoLuV,
    WirelessLaserEnergyZPM256A,
    WirelessLaserEnergyZPM1024A,
    WirelessLaserEnergyZPM4096A,
    WirelessLaserEnergyZPM16384A,
    WirelessLaserEnergyZPM65536A,
    WirelessLaserEnergyZPM262144A,
    WirelessLaserEnergyZPM1048576A,
    WirelessLaserDynamoZPM,
    WirelessLaserEnergyUV256A,
    WirelessLaserEnergyUV1024A,
    WirelessLaserEnergyUV4096A,
    WirelessLaserEnergyUV16384A,
    WirelessLaserEnergyUV65536A,
    WirelessLaserEnergyUV262144A,
    WirelessLaserEnergyUV1048576A,
    WirelessLaserDynamoUV,
    WirelessLaserEnergyUHV256A,
    WirelessLaserEnergyUHV1024A,
    WirelessLaserEnergyUHV4096A,
    WirelessLaserEnergyUHV16384A,
    WirelessLaserEnergyUHV65536A,
    WirelessLaserEnergyUHV262144A,
    WirelessLaserEnergyUHV1048576A,
    WirelessLaserDynamoUHV,
    WirelessLaserEnergyUEV256A,
    WirelessLaserEnergyUEV1024A,
    WirelessLaserEnergyUEV4096A,
    WirelessLaserEnergyUEV16384A,
    WirelessLaserEnergyUEV65536A,
    WirelessLaserEnergyUEV262144A,
    WirelessLaserEnergyUEV1048576A,
    WirelessLaserDynamoUEV,
    WirelessLaserEnergyUIV256A,
    WirelessLaserEnergyUIV1024A,
    WirelessLaserEnergyUIV4096A,
    WirelessLaserEnergyUIV16384A,
    WirelessLaserEnergyUIV65536A,
    WirelessLaserEnergyUIV262144A,
    WirelessLaserEnergyUIV1048576A,
    WirelessLaserDynamoUIV,
    WirelessLaserEnergyUMV256A,
    WirelessLaserEnergyUMV1024A,
    WirelessLaserEnergyUMV4096A,
    WirelessLaserEnergyUMV16384A,
    WirelessLaserEnergyUMV65536A,
    WirelessLaserEnergyUMV262144A,
    WirelessLaserEnergyUMV1048576A,
    WirelessLaserDynamoUMV,
    WirelessLaserEnergyUXV256A,
    WirelessLaserEnergyUXV1024A,
    WirelessLaserEnergyUXV4096A,
    WirelessLaserEnergyUXV16384A,
    WirelessLaserEnergyUXV65536A,
    WirelessLaserEnergyUXV262144A,
    WirelessLaserEnergyUXV1048576A,
    WirelessLaserDynamoUXV,
    WirelessLaserEnergyMAX256A,
    WirelessLaserEnergyMAX1024A,
    WirelessLaserEnergyMAX4096A,
    WirelessLaserEnergyMAX16384A,
    WirelessLaserEnergyMAX65536A,
    WirelessLaserEnergyMAX262144A,
    WirelessLaserEnergyMAX1048576A,
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
    LargeVoidMiner,
    LargeBeeBreeder,
    LargeCropBreeder,
    IntegratedProductionFactory,
    MaxCapacityMEOutputBus,
    MaxCapacityMEOutputHatch,
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
