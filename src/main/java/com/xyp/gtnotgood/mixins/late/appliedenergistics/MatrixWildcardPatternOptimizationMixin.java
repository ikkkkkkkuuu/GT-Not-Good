package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternState;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;

import appeng.api.AEApi;
import appeng.container.implementations.ContainerOptimizePatterns;
import codechicken.nei.ItemStackMap;

/** Matches a physical wildcard template to any of its expanded patterns selected by the matrix. */
@Mixin(value = ContainerOptimizePatterns.class, remap = false)
public abstract class MatrixWildcardPatternOptimizationMixin {

    @WrapOperation(
        method = "optimizePatterns",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/ItemStackMap;get(Lnet/minecraft/item/ItemStack;)Ljava/lang/Object;"),
        require = 1)
    private Object gtng$findWildcardTemplate(ItemStackMap<?> lookup, ItemStack physicalPattern,
        Operation<Object> original) {
        Object exact = original.call(lookup, physicalPattern);
        if (
            exact != null || !WildcardPatternGenerator.isWildcardPattern(physicalPattern)
                || WildcardPatternGenerator.isGeneratedPattern(physicalPattern)
        ) return exact;

        Pair<?, ?> selected = null;
        int multiplier = 0;
        for (ItemStackMap.Entry<?> entry : lookup.entries()) {
            if (
                !WildcardPatternGenerator.isGeneratedPattern(entry.key) || !sameTemplate(physicalPattern, entry.key)
                    || !(entry.value instanceof Pair<?, ?>candidate)
                    || !(candidate.getRight() instanceof Integer bits)
                    || bits <= multiplier
            ) continue;
            selected = candidate;
            multiplier = bits;
        }

        int safeMultiplier = Math.min(multiplier, WildcardPatternState.getMaxBitMultiplier(physicalPattern));
        return selected == null || safeMultiplier <= 0 ? null : Pair.of(selected.getLeft(), safeMultiplier);
    }

    private static boolean sameTemplate(ItemStack physical, ItemStack generated) {
        if (!sameSourceItem(physical, generated)) return false;
        NBTTagCompound physicalTag = templateTag(physical);
        NBTTagCompound generatedTag = templateTag(generated);
        return physicalTag.equals(generatedTag);
    }

    private static boolean sameSourceItem(ItemStack physical, ItemStack generated) {
        if (physical.getItem() == generated.getItem() && physical.getItemDamage() == generated.getItemDamage())
            return true;
        if (
            !generated.hasTagCompound() || !generated.getTagCompound().hasKey("gtngWildcardSource", NBT.TAG_COMPOUND)
                || !(AEApi.instance().definitions().items().encodedPattern().isSameAs(generated)
                    || AEApi.instance().definitions().items().encodedUltimatePattern().isSameAs(generated))
        ) return false;
        NBTTagCompound source = generated.getTagCompound().getCompoundTag("gtngWildcardSource");
        return source.hasKey("id", NBT.TAG_STRING) && source.hasKey("Damage", NBT.TAG_INT)
            && source.getString("id").equals(Item.itemRegistry.getNameForObject(physical.getItem()))
            && source.getInteger("Damage") == physical.getItemDamage();
    }

    private static NBTTagCompound templateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound() == null ? new NBTTagCompound()
            : (NBTTagCompound) stack.getTagCompound().copy();
        tag.removeTag("in");
        tag.removeTag("out");
        tag.removeTag("crafting");
        tag.removeTag("InvalidPattern");
        tag.removeTag("WildcardSelectedMaterial");
        tag.removeTag("WildcardPattern");
        tag.removeTag(WildcardPatternGenerator.KEY_GENERATED_PATTERN_ID);
        tag.removeTag(WildcardModelState.KEY_EXPANDED_COUNT);
        tag.removeTag("WildcardExpandedPatternCount");
        tag.removeTag("gtngPatternCircuit");
        tag.removeTag("gtngPatternMolds");
        tag.removeTag("gtngWildcardSource");
        tag.setBoolean("substitute", tag.getBoolean("substitute"));
        tag.setBoolean("beSubstitute", tag.getBoolean("beSubstitute"));
        return tag;
    }
}
