package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.mixins.helpers.WildcardPatternSlotSuper;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.util.inv.MEInventoryCrafting;

@Mixin(value = SuperMTEHatchCraftingInputME.class, remap = false)
public abstract class SuperMTEHatchCraftingInputMEMixin {

    @Shadow
    private SuperMTEHatchCraftingInputME.PatternSlot[] internalInventory;

    @Shadow
    private Map patternDetailsPatternSlotMap;

    @Shadow
    private boolean justHadNewItems;

    @Shadow
    @Final
    private boolean supportFluids;

    @Shadow
    public abstract IInventory getPatterns();

    @Shadow
    public abstract boolean isActive();

    @Inject(method = "provideCrafting", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$provideExpandedPatterns(ICraftingProviderHelper craftingTracker, CallbackInfo ci) {
        if (!hasWildcardPattern()) {
            return;
        }

        ci.cancel();
        if (!isActive()) {
            return;
        }

        this.patternDetailsPatternSlotMap.values()
            .removeIf(s -> s instanceof WildcardPatternSlotSuper);

        IInventory patterns = getPatterns();
        World world = getWorld();
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            SuperMTEHatchCraftingInputME.PatternSlot slot = getOrCreateWildcardSlot(index);
            if (slot == null) {
                continue;
            }

            ItemStack stack = patterns.getStackInSlot(index);
            if (WildcardPatternGenerator.isWildcardPattern(stack)) {
                List<ICraftingPatternDetails> detailsList = getExpandedDetails(slot, stack, world);
                for (ICraftingPatternDetails details : detailsList) {
                    this.patternDetailsPatternSlotMap.put(details, slot);
                    craftingTracker.addCraftingOption((ICraftingProvider) (Object) this, details);
                }
                continue;
            }

            ICraftingPatternDetails details = slot.getPatternDetails();
            if (details != null) {
                craftingTracker.addCraftingOption((ICraftingProvider) (Object) this, details);
            }
        }
    }

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$pushExpandedPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table,
        CallbackInfoReturnable<Boolean> cir) {
        if (patternDetails == null) {
            cir.setReturnValue(false);
            return;
        }

        boolean hasWildcardPattern = hasWildcardPattern();
        boolean isWildcardRequest = isWildcardPatternDetails(patternDetails);
        SuperMTEHatchCraftingInputME.PatternSlot slot = (SuperMTEHatchCraftingInputME.PatternSlot) this.patternDetailsPatternSlotMap
            .get(patternDetails);
        if (slot != null && !isCurrentInternalSlot(slot)) {
            SuperMTEHatchCraftingInputME.PatternSlot detachedSlot = slot;
            this.patternDetailsPatternSlotMap.values()
                .removeIf(s -> s == detachedSlot);
            slot = null;
        }

        if (slot == null && (hasWildcardPattern || isWildcardRequest)) {
            slot = findWildcardPatternSlot(patternDetails);
            if (slot != null) {
                this.patternDetailsPatternSlotMap.put(patternDetails, slot);
            }
        }
        if (slot == null) {
            cir.setReturnValue(false);
            return;
        }
        if (!hasWildcardPattern && !isWildcardRequest) {
            return;
        }
        if (slot instanceof WildcardPatternSlotSuper wildcardSlot) {
            if (!wildcardSlot.canAcceptPattern(patternDetails)) {
                cir.setReturnValue(false);
                return;
            }
            String previousActiveId = wildcardSlot.getActiveGeneratedPatternId();
            wildcardSlot.setActivePatternDetails(patternDetails);
            if (!previousActiveId.equals(wildcardSlot.getActiveGeneratedPatternId())) {
                removeInventoryRecipeCache(slot);
            }
        }

        SuperMTEHatchCraftingInputME hatch = (SuperMTEHatchCraftingInputME) (Object) this;
        if (!hatch.isActive() || !hatch.getBaseMetaTileEntity()
            .isAllowedToWork()) {
            cir.setReturnValue(false);
            return;
        }
        if (hasUnsupportedFluidPacket(table)) {
            cir.setReturnValue(false);
            return;
        }

        if (!(table instanceof MEInventoryCrafting meTable)) {
            cir.setReturnValue(false);
            return;
        }

        if (!slot.insertItemsAndFluids(meTable)) {
            cir.setReturnValue(false);
            return;
        }

        this.justHadNewItems = true;
        cir.setReturnValue(true);
    }

    @Inject(method = "onPatternChange", at = @At("RETURN"))
    private void wildcardpattern$registerExpandedPatternMap(int index, ItemStack newItem, CallbackInfo ci) {
        registerExpandedPatterns(index, newItem);
    }

    @Inject(method = "loadNBTData", at = @At("RETURN"))
    private void wildcardpattern$registerLoadedExpandedPatternMap(NBTTagCompound tag, CallbackInfo ci) {
        IInventory patterns = getPatterns();
        Map<Integer, NBTTagCompound> activePatternTags = readSavedActivePatternTags(tag);
        boolean hasItems = false;
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            registerExpandedPatterns(
                index,
                patterns.getStackInSlot(index),
                activePatternTags.get(Integer.valueOf(index)));
            SuperMTEHatchCraftingInputME.PatternSlot slot = this.internalInventory[index];
            if (slot != null && (slot.getItemInputs().length > 0 || slot.getFluidInputs().length > 0)) {
                hasItems = true;
            }
        }
        if (hasItems) {
            this.justHadNewItems = true;
        }
    }

    private static boolean isWildcardPatternDetails(ICraftingPatternDetails patternDetails) {
        ItemStack pattern = patternDetails == null ? null : patternDetails.getPattern();
        return WildcardPatternGenerator.isWildcardPattern(pattern)
            || WildcardPatternGenerator.isGeneratedPattern(pattern);
    }

    private boolean hasUnsupportedFluidPacket(InventoryCrafting table) {
        if (this.supportFluids || table == null) {
            return false;
        }
        for (int index = 0; index < table.getSizeInventory(); index++) {
            ItemStack itemStack = table.getStackInSlot(index);
            if (itemStack != null && itemStack.getItem() instanceof ItemFluidPacket) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWildcardPattern() {
        IInventory patterns = getPatterns();
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            if (WildcardPatternGenerator.isWildcardPattern(patterns.getStackInSlot(index))) {
                return true;
            }
        }
        return false;
    }

    private void registerExpandedPatterns(int index, ItemStack stack) {
        registerExpandedPatterns(index, stack, null);
    }

    private void registerExpandedPatterns(int index, ItemStack stack, NBTTagCompound savedActivePatternTag) {
        if (index < 0 || index >= this.internalInventory.length) {
            return;
        }

        removeDetachedWildcardMappings();

        if (!WildcardPatternGenerator.isWildcardPattern(stack)) {
            return;
        }

        SuperMTEHatchCraftingInputME.PatternSlot targetSlot = this.internalInventory[index];
        if (targetSlot != null) {
            this.patternDetailsPatternSlotMap.values()
                .removeIf(s -> s == targetSlot);
        }

        SuperMTEHatchCraftingInputME.PatternSlot slot = getOrCreateWildcardSlot(index);
        if (slot == null) {
            return;
        }

        if (slot != targetSlot) {
            this.patternDetailsPatternSlotMap.values()
                .removeIf(s -> s == slot);
        }

        List<ICraftingPatternDetails> detailsList = getExpandedDetails(slot, stack, getWorld());
        if (slot instanceof WildcardPatternSlotSuper wildcardSlot) {
            wildcardSlot.restoreActivePatternDetails(savedActivePatternTag, detailsList);
        }
        for (ICraftingPatternDetails details : detailsList) {
            this.patternDetailsPatternSlotMap.put(details, slot);
        }
    }

    private void removeDetachedWildcardMappings() {
        this.patternDetailsPatternSlotMap.values()
            .removeIf(
                s -> s instanceof WildcardPatternSlotSuper
                    && !isCurrentInternalSlot((SuperMTEHatchCraftingInputME.PatternSlot) s));
    }

    private boolean isCurrentInternalSlot(SuperMTEHatchCraftingInputME.PatternSlot slot) {
        for (SuperMTEHatchCraftingInputME.PatternSlot current : this.internalInventory) {
            if (current == slot) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, NBTTagCompound> readSavedActivePatternTags(NBTTagCompound source) {
        Map<Integer, NBTTagCompound> result = new HashMap<>();
        if (source == null || !source.hasKey("internalInventory", NBT.TAG_LIST)) {
            return result;
        }
        NBTTagList inventory = source.getTagList("internalInventory", NBT.TAG_COMPOUND);
        for (int index = 0; index < inventory.tagCount(); index++) {
            NBTTagCompound slotWrapper = inventory.getCompoundTagAt(index);
            int patternSlot = slotWrapper.getInteger("patternSlot");
            NBTTagCompound slotTag = slotWrapper.getCompoundTag("patternSlotNBT");
            if (slotTag.hasKey(WildcardPatternSlotSuper.KEY_ACTIVE_PATTERN, NBT.TAG_COMPOUND)
                || slotTag.hasKey(WildcardPatternSlotSuper.KEY_ACTIVE_PATTERN_ID)) {
                result.put(Integer.valueOf(patternSlot), slotTag);
            }
        }
        return result;
    }

    private void removeInventoryRecipeCache(SuperMTEHatchCraftingInputME.PatternSlot slot) {
        Iterable<?> processingLogics = getProcessingLogics();
        if (processingLogics == null) {
            return;
        }
        for (Object processingLogic : processingLogics) {
            invokeRemoveInventoryRecipeCache(processingLogic, slot);
        }
    }

    private Iterable<?> getProcessingLogics() {
        Field field = findField(((Object) this).getClass(), "processingLogics");
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            Object value = field.get(this);
            if (value instanceof Iterable<?>iterable) {
                return iterable;
            }
        } catch (IllegalAccessException | SecurityException ignored) {
            // GTNH 2.9 removed this field; cache clearing is best-effort for older GT versions.
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void invokeRemoveInventoryRecipeCache(Object processingLogic,
        SuperMTEHatchCraftingInputME.PatternSlot slot) {
        if (processingLogic == null) {
            return;
        }
        for (Method method : processingLogic.getClass()
            .getMethods()) {
            if (!"removeInventoryRecipeCache".equals(method.getName()) || method.getParameterTypes().length != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].isInstance(slot)) {
                continue;
            }
            try {
                method.invoke(processingLogic, slot);
            } catch (ReflectiveOperationException | SecurityException ignored) {
                // Optional compatibility path only; stale cache is less dangerous than crashing the hatch.
            }
            return;
        }
    }

    private SuperMTEHatchCraftingInputME.PatternSlot findWildcardPatternSlot(ICraftingPatternDetails patternDetails) {
        ItemStack requestedPattern = patternDetails.getPattern();
        if (requestedPattern == null) {
            return null;
        }

        World world = getWorld();
        IInventory patterns = getPatterns();
        for (int index = 0; index < this.internalInventory.length && index < patterns.getSizeInventory(); index++) {
            SuperMTEHatchCraftingInputME.PatternSlot slot = this.internalInventory[index];
            ItemStack stack = patterns.getStackInSlot(index);
            if (slot == null || !WildcardPatternGenerator.isWildcardPattern(stack)) {
                continue;
            }

            for (ICraftingPatternDetails generated : getExpandedDetails(slot, stack, world)) {
                if (arePatternDetailsEqual(generated, patternDetails)) {
                    return slot;
                }
            }
        }
        return null;
    }

    private SuperMTEHatchCraftingInputME.PatternSlot getOrCreateWildcardSlot(int index) {
        if (index < 0 || index >= this.internalInventory.length) {
            return null;
        }

        SuperMTEHatchCraftingInputME.PatternSlot slot = this.internalInventory[index];
        IInventory patterns = getPatterns();
        if (patterns == null || index >= patterns.getSizeInventory()) {
            return slot;
        }

        ItemStack stack = patterns.getStackInSlot(index);
        if (!WildcardPatternGenerator.isWildcardPattern(stack) || slot instanceof WildcardPatternSlotSuper) {
            return slot;
        }

        WildcardPatternSlotSuper wrapped = new WildcardPatternSlotSuper(
            (SuperMTEHatchCraftingInputME) (Object) this,
            stack,
            slot);
        this.internalInventory[index] = wrapped;
        return wrapped;
    }

    private static List<ICraftingPatternDetails> getExpandedDetails(SuperMTEHatchCraftingInputME.PatternSlot slot,
        ItemStack stack, World world) {
        if (slot instanceof WildcardPatternSlotSuper wildcardSlot) {
            return wildcardSlot.getExpandedDetails(stack, world);
        }
        return WildcardPatternGenerator.generateAllDetails(stack, world);
    }

    private static boolean arePatternDetailsEqual(ICraftingPatternDetails left, ICraftingPatternDetails right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        ItemStack leftPattern = left.getPattern();
        ItemStack rightPattern = right.getPattern();
        if (leftPattern == rightPattern) {
            return true;
        }
        if (leftPattern == null || rightPattern == null) {
            return false;
        }
        if (leftPattern.getItem() != rightPattern.getItem()
            || leftPattern.getItemDamage() != rightPattern.getItemDamage()) {
            return false;
        }
        String leftId = WildcardPatternGenerator.getGeneratedPatternId(leftPattern);
        String rightId = WildcardPatternGenerator.getGeneratedPatternId(rightPattern);
        if (!leftId.isEmpty() || !rightId.isEmpty()) {
            return leftId.equals(rightId);
        }
        NBTTagCompound leftTag = leftPattern.getTagCompound();
        NBTTagCompound rightTag = rightPattern.getTagCompound();
        if (leftTag == rightTag) {
            return true;
        }
        if (leftTag == null || rightTag == null) {
            return false;
        }
        return leftTag.equals(rightTag);
    }

    private World getWorld() {
        return ((SuperMTEHatchCraftingInputME) (Object) this).getBaseMetaTileEntity()
            .getWorld();
    }
}
