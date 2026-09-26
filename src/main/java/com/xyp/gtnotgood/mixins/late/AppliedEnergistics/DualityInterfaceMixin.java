package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternCache;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.tile.inventory.AppEngInternalInventory;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class DualityInterfaceMixin {

    /** Per-slot ownership prevents mutable AE2 priorities from leaking between identical configurations. */
    @Unique
    private final Map<Integer, WildcardPatternCache<List<ICraftingPatternDetails>>> wildcardpattern$caches = new HashMap<>();

    @Shadow
    private AppEngInternalInventory patterns;

    @Shadow
    private IInterfaceHost iHost;

    @Shadow
    public List<ICraftingPatternDetails> craftingList;

    @Shadow
    protected abstract int getPriority();

    @Inject(method = "addToCraftingList", at = @At("HEAD"), cancellable = true, remap = false)
    private void wildcardpattern$expandWildcardPattern(int slot, CallbackInfo ci) {
        ItemStack stack = this.patterns.getStackInSlot(slot);
        if (!WildcardPatternGenerator.isWildcardPattern(stack)) {
            this.wildcardpattern$caches.remove(slot);
            return;
        }

        World world = this.iHost.getTileEntity()
            .getWorldObj();
        List<ICraftingPatternDetails> detailsList = this.wildcardpattern$caches
            .computeIfAbsent(slot, ignored -> new WildcardPatternCache<>())
            .get(stack, world, () -> WildcardPatternGenerator.generateAllDetails(stack, world));
        if (this.craftingList == null) {
            this.craftingList = new LinkedList<>();
        }

        int priority = slot - 36 * this.getPriority();
        for (ICraftingPatternDetails details : detailsList) {
            details.setPriority(priority);
            this.craftingList.add(details);
        }
        ci.cancel();
    }
}
