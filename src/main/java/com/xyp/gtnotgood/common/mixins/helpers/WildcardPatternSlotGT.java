package com.xyp.gtnotgood.common.mixins.helpers;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternCache;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.enums.GTValues;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public class WildcardPatternSlotGT extends MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> {

    public static final String KEY_ACTIVE_PATTERN = "WildcardActivePattern";
    public static final String KEY_ACTIVE_PATTERN_ID = "WildcardActivePatternId";

    private ICraftingPatternDetails activePatternDetails;
    private ItemStack activePatternStack;
    private String activeGeneratedPatternId = "";
    private final WildcardPatternCache<List<ICraftingPatternDetails>> expansionCache = new WildcardPatternCache<>();
    private List<ICraftingPatternDetails> cachedExpandedDetails = java.util.Collections.emptyList();

    public WildcardPatternSlotGT(MTEHatchCraftingInputME parent, ItemStack pattern,
        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> originalSlot) {
        super(pattern, parent);
        if (originalSlot != null) {
            for (ItemStack itemStack : originalSlot.getItemInputs()) {
                if (itemStack != null) {
                    this.itemInventory.add(itemStack.copy());
                }
            }
            for (FluidStack fluidStack : originalSlot.getFluidInputs()) {
                if (fluidStack != null) {
                    this.fluidInventory.add(fluidStack.copy());
                }
            }
        }
    }

    public void setActivePatternDetails(ICraftingPatternDetails activePatternDetails) {
        if (this.activePatternDetails == activePatternDetails) return;
        this.activePatternDetails = activePatternDetails;
        setActivePatternStack(activePatternDetails == null ? null : activePatternDetails.getPattern());
    }

    private void setActivePatternStack(ItemStack activePattern) {
        this.activePatternStack = activePattern == null ? null : activePattern.copy();
        this.activeGeneratedPatternId = WildcardPatternGenerator.getGeneratedPatternId(activePattern);
    }

    public String getActiveGeneratedPatternId() {
        return this.activeGeneratedPatternId;
    }

    public List<ICraftingPatternDetails> getExpandedDetails(ItemStack patternStack, World world) {
        this.cachedExpandedDetails = expansionCache
            .get(patternStack, world, () -> WildcardPatternGenerator.generateAllDetails(patternStack, world));
        return this.cachedExpandedDetails;
    }

    public boolean canAcceptPattern(ICraftingPatternDetails patternDetails) {
        if (patternDetails == null || isEmpty()) {
            return true;
        }
        String requestedId = WildcardPatternGenerator.getGeneratedPatternId(patternDetails.getPattern());
        if (requestedId.isEmpty()) {
            return false;
        }
        if (this.activeGeneratedPatternId.isEmpty()) {
            setActivePatternDetails(recoverActiveDetails(getCachedOrGeneratedDetails()));
        }
        return requestedId.equals(this.activeGeneratedPatternId);
    }

    public void restoreActivePatternDetails(NBTTagCompound savedActivePatternTag,
        List<ICraftingPatternDetails> detailsList) {
        if (this.activePatternDetails != null || !hasStoredInputs()) {
            return;
        }
        String savedId = savedActivePatternTag == null ? "" : savedActivePatternTag.getString(KEY_ACTIVE_PATTERN_ID);
        ItemStack savedActivePattern = null;
        if (savedActivePatternTag != null && savedActivePatternTag.hasKey(KEY_ACTIVE_PATTERN, NBT.TAG_COMPOUND)) {
            savedActivePattern = ItemStack
                .loadItemStackFromNBT(savedActivePatternTag.getCompoundTag(KEY_ACTIVE_PATTERN));
            if (savedId.isEmpty()) {
                savedId = WildcardPatternGenerator.getGeneratedPatternId(savedActivePattern);
            }
        }

        ICraftingPatternDetails restored = findMatchingPatternDetails(savedId, detailsList);
        if (restored == null) {
            restored = findMatchingPatternDetails(savedActivePattern, detailsList);
        }
        if (restored == null && savedActivePattern != null) {
            ICraftingPatternDetails savedDetails = createDetailsFromPattern(savedActivePattern);
            if (savedDetails != null && inputsMatchSlot(savedDetails)) {
                restored = savedDetails;
            }
        }
        if (restored == null) {
            restored = recoverActiveDetails(detailsList);
        }
        setActivePatternDetails(restored);
    }

    @Override
    public ICraftingPatternDetails getPatternDetails() {
        if (this.activePatternDetails != null) {
            if (!hasStoredInputs()) {
                setActivePatternDetails(null);
                return super.getPatternDetails();
            }
            return this.activePatternDetails;
        }
        if (hasStoredInputs()) {
            if (this.activePatternStack != null) {
                ICraftingPatternDetails savedDetails = createDetailsFromPattern(this.activePatternStack);
                if (savedDetails != null && inputsMatchSlot(savedDetails)) {
                    setActivePatternDetails(savedDetails);
                    return this.activePatternDetails;
                }
            }
            setActivePatternDetails(recoverActiveDetails(getCachedOrGeneratedDetails()));
            return this.activePatternDetails;
        }
        return super.getPatternDetails();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        boolean hasInputs = hasStoredInputs();
        NBTTagCompound written = super.writeToNBT(nbt);
        if (hasInputs && this.activePatternDetails == null) {
            setActivePatternDetails(recoverActiveDetails(getCachedOrGeneratedDetails()));
        }
        ItemStack activePattern = this.activePatternStack;
        if (hasInputs && activePattern != null && !this.activeGeneratedPatternId.isEmpty()) {
            NBTTagCompound activeTag = new NBTTagCompound();
            activePattern.writeToNBT(activeTag);
            written.setTag(KEY_ACTIVE_PATTERN, activeTag);
            written.setString(KEY_ACTIVE_PATTERN_ID, this.activeGeneratedPatternId);
        } else {
            written.removeTag(KEY_ACTIVE_PATTERN);
            written.removeTag(KEY_ACTIVE_PATTERN_ID);
        }
        return written;
    }

    private List<ICraftingPatternDetails> getCachedOrGeneratedDetails() {
        if (this.pattern == null) {
            return this.cachedExpandedDetails;
        }
        World world = this.parentMTE.getBaseMetaTileEntity()
            .getWorld();
        if (world == null) {
            return this.cachedExpandedDetails;
        }
        return getExpandedDetails(this.pattern, world);
    }

    private ICraftingPatternDetails recoverActiveDetails(List<ICraftingPatternDetails> detailsList) {
        if (detailsList == null || detailsList.isEmpty()) {
            return null;
        }
        ICraftingPatternDetails recovered = null;
        for (ICraftingPatternDetails details : detailsList) {
            if (!inputsMatchSlot(details)) {
                continue;
            }
            if (recovered != null && !arePatternStacksEqual(recovered.getPattern(), details.getPattern())) {
                return null;
            }
            recovered = details;
        }
        return recovered;
    }

    private ICraftingPatternDetails createDetailsFromPattern(ItemStack patternStack) {
        if (patternStack == null) {
            return null;
        }
        World world = this.parentMTE.getBaseMetaTileEntity()
            .getWorld();
        return WildcardPatternGenerator.createDetailForCurrentStack(patternStack.copy(), world);
    }

    private boolean inputsMatchSlot(ICraftingPatternDetails details) {
        IAEItemStack[] inputs = details == null ? null : details.getInputs();
        if (inputs == null) {
            return false;
        }
        boolean sawInput = false;
        for (IAEItemStack input : inputs) {
            if (input == null) {
                continue;
            }
            ItemStack inputStack = input.getItemStack();
            if (inputStack == null) {
                continue;
            }
            sawInput = true;
            if (isFluidPatternInput(inputStack)) {
                FluidStack fluidStack = getFluidFromPatternInput(inputStack);
                if (fluidStack == null || countStoredFluid(fluidStack) < getRequiredFluidAmount(fluidStack, inputs)) {
                    return false;
                }
                continue;
            }
            if (countStoredItems(inputStack) < getRequiredItemAmount(inputStack, inputs)) {
                return false;
            }
        }
        return sawInput && storedItemsCoveredByPattern(inputs) && storedFluidsCoveredByPattern(inputs);
    }

    private long getRequiredItemAmount(ItemStack expected, IAEItemStack[] inputs) {
        long required = 0L;
        for (IAEItemStack input : inputs) {
            if (input == null) {
                continue;
            }
            ItemStack inputStack = input.getItemStack();
            if (inputStack == null || isFluidPatternInput(inputStack)
                || !GTUtility.areStacksEqual(inputStack, expected)) {
                continue;
            }
            required += Math.max(Math.max(1L, input.getStackSize()), inputStack.stackSize);
        }
        return required;
    }

    private long getRequiredFluidAmount(FluidStack expected, IAEItemStack[] inputs) {
        long required = 0L;
        for (IAEItemStack input : inputs) {
            if (input == null) {
                continue;
            }
            ItemStack inputStack = input.getItemStack();
            if (inputStack == null || !isFluidPatternInput(inputStack)) {
                continue;
            }
            FluidStack fluidStack = getFluidFromPatternInput(inputStack);
            if (fluidStack != null && GTUtility.areFluidsEqual(fluidStack, expected)) {
                required += Math.max(1L, fluidStack.amount);
            }
        }
        return required;
    }

    private long countStoredItems(ItemStack expected) {
        long count = 0L;
        for (ItemStack stored : this.itemInventory) {
            if (stored != null && stored.stackSize > 0 && GTUtility.areStacksEqual(stored, expected)) {
                count += stored.stackSize;
            }
        }
        return count;
    }

    private long countStoredFluid(FluidStack expected) {
        long amount = 0L;
        for (FluidStack stored : this.fluidInventory) {
            if (stored != null && stored.amount > 0 && GTUtility.areFluidsEqual(stored, expected)) {
                amount += stored.amount;
            }
        }
        return amount;
    }

    private boolean storedItemsCoveredByPattern(IAEItemStack[] inputs) {
        for (ItemStack stored : this.itemInventory) {
            if (stored != null && stored.stackSize > 0 && !patternHasItemInput(stored, inputs)) {
                return false;
            }
        }
        return true;
    }

    private boolean storedFluidsCoveredByPattern(IAEItemStack[] inputs) {
        for (FluidStack stored : this.fluidInventory) {
            if (stored != null && stored.amount > 0 && !patternHasFluidInput(stored, inputs)) {
                return false;
            }
        }
        return true;
    }

    private boolean patternHasItemInput(ItemStack stored, IAEItemStack[] inputs) {
        for (IAEItemStack input : inputs) {
            if (input == null) {
                continue;
            }
            ItemStack inputStack = input.getItemStack();
            if (inputStack != null && !isFluidPatternInput(inputStack)
                && GTUtility.areStacksEqual(stored, inputStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean patternHasFluidInput(FluidStack stored, IAEItemStack[] inputs) {
        for (IAEItemStack input : inputs) {
            if (input == null) {
                continue;
            }
            ItemStack inputStack = input.getItemStack();
            if (inputStack == null || !isFluidPatternInput(inputStack)) {
                continue;
            }
            FluidStack fluidStack = getFluidFromPatternInput(inputStack);
            if (fluidStack != null && GTUtility.areFluidsEqual(stored, fluidStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStoredInputs() {
        return !isEmpty();
    }

    private static ICraftingPatternDetails findMatchingPatternDetails(String savedId,
        List<ICraftingPatternDetails> detailsList) {
        if (savedId == null || savedId.isEmpty() || detailsList == null || detailsList.isEmpty()) {
            return null;
        }
        for (ICraftingPatternDetails details : detailsList) {
            if (details != null
                && savedId.equals(WildcardPatternGenerator.getGeneratedPatternId(details.getPattern()))) {
                return details;
            }
        }
        return null;
    }

    private static ICraftingPatternDetails findMatchingPatternDetails(ItemStack savedActivePattern,
        List<ICraftingPatternDetails> detailsList) {
        if (savedActivePattern == null || detailsList == null || detailsList.isEmpty()) {
            return null;
        }
        for (ICraftingPatternDetails details : detailsList) {
            if (details != null && arePatternStacksEqual(savedActivePattern, details.getPattern())) {
                return details;
            }
        }
        return null;
    }

    private static boolean arePatternStacksEqual(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.getItem() != right.getItem() || left.getItemDamage() != right.getItemDamage()) {
            return false;
        }
        String leftId = WildcardPatternGenerator.getGeneratedPatternId(left);
        String rightId = WildcardPatternGenerator.getGeneratedPatternId(right);
        if (!leftId.isEmpty() || !rightId.isEmpty()) {
            return leftId.equals(rightId);
        }
        NBTTagCompound leftTag = left.getTagCompound();
        NBTTagCompound rightTag = right.getTagCompound();
        if (leftTag == rightTag) {
            return true;
        }
        return leftTag != null && leftTag.equals(rightTag);
    }

    @Override
    public GTDualInputPattern getPatternInputs() {
        ICraftingPatternDetails details = getPatternDetails();
        GTDualInputPattern dualInputs = new GTDualInputPattern();
        if (details == null) {
            dualInputs.inputItems = GTValues.emptyItemStackArray;
            dualInputs.inputFluid = GTValues.emptyFluidStackArray;
            return dualInputs;
        }
        ItemStack[] sharedItems = this.parentMTE.getSharedItems();
        IAEStack<?>[] inputs = details.getAEInputs();
        int itemCount = sharedItems == null ? 0 : sharedItems.length;
        int fluidCount = 0;
        for (IAEStack<?> input : inputs) {
            if (input instanceof IAEItemStack) itemCount++;
            else if (input instanceof IAEFluidStack) fluidCount++;
        }
        ItemStack[] inputItems = new ItemStack[itemCount];
        FluidStack[] inputFluids = new FluidStack[fluidCount];
        int itemIndex = sharedItems == null ? 0 : sharedItems.length;
        if (itemIndex > 0) System.arraycopy(sharedItems, 0, inputItems, 0, itemIndex);
        int fluidIndex = 0;
        for (IAEStack<?> singleInput : inputs) {
            if (singleInput instanceof IAEItemStack ais) {
                inputItems[itemIndex++] = ais.getItemStack();
            } else if (singleInput instanceof IAEFluidStack ifs) {
                inputFluids[fluidIndex++] = ifs.getFluidStack();
            }
        }

        dualInputs.inputItems = inputItems;
        dualInputs.inputFluid = inputFluids;
        return dualInputs;
    }

    /** 从样板输入中提取FluidStack,同时支持GT5原生的ItemFluidDisplay和AE2FC的ItemFluidDrop */
    private static FluidStack getFluidFromPatternInput(ItemStack stack) {
        if (stack == null) return null;
        FluidStack result = GTUtility.getFluidFromDisplayStack(stack);
        if (result != null) return result;
        // [液滴分类] 可迁原生：仅从样板输入读出流体用于与已存库存比对(数量校验)，不构造合成请求
        if (FluidDropCompat.isFluidDrop(stack.getItem())) {
            return FluidDropCompat.getFluidStack(stack);
        }
        return null;
    }

    /** 判断ItemStack是否为流体输入(GT5 ItemFluidDisplay 或 AE2FC ItemFluidDrop) */
    private static boolean isFluidPatternInput(ItemStack stack) {
        return getFluidFromPatternInput(stack) != null;
    }

}
