package com.xyp.gtnotgood.mixins.late.Gregtech;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.items.MetaGeneratedTool;

/**
 * Scales crafting wear and isolates container-item queries from the caller's tool stack.
 * <p>
 * GregTech's usability checks update enchantment NBT before getContainerItem makes its own single-tool copy.
 * Copying the argument at entry also protects callers performing simulated crafting or querying shared templates.
 * Container items still represent exactly one used tool; returning the input count would duplicate tools.
 */
@Mixin(value = MetaGeneratedTool.class, remap = false)
public abstract class GTMetaTools {

    /**
     * Keeps both container-item queries from changing the caller's count or NBT.
     *
     * @param stack tool supplied by the crafting inventory, possibly null
     * @return independent stack for GregTech's existing checks
     */
    @ModifyVariable(
        method = { "getContainerItem", "hasContainerItem" },
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 2)
    private ItemStack gtnotgood$copyCraftingTool(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    /**
     * Preserves GregTech's damage and breakage handling while scaling only crafting wear.
     *
     * @param damage original crafting damage
     * @return rounded-up damage, with invalid configuration falling back to the original amount
     */
    @ModifyArg(
        method = "getContainerItem",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/items/MetaGeneratedTool;doDamage(Lnet/minecraft/item/ItemStack;J)Z"),
        index = 1,
        require = 1)
    private long gtnotgood$scaleCraftingDamage(long damage) {
        float multiplier = Config.gtToolsCraftingDurability;
        if (damage <= 0 || !Float.isFinite(multiplier) || multiplier <= 1.0F) return damage;
        return Math.max(1L, (long) Math.ceil(damage / (double) multiplier));
    }
}
