package com.xyp.gtnotgood.mixins.late.BloodMagic;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.packaged.BloodAltarAccess;
import com.xyp.gtnotgood.common.packaged.TilePackagedProvider;

import WayofTime.alchemicalWizardry.common.tileEntity.TEAltar;

/**
 * Stops a dispatched single-step result from becoming the next slate while its provider is unavailable.
 * The marker survives chunk and world reloads and never replaces Blood Magic's actual crafting cycle.
 */
@Mixin(value = TEAltar.class, remap = false)
public abstract class MixinPackagedBloodAltar implements BloodAltarAccess {

    @Unique
    private ItemStack gtnotgood$expected;

    @Override
    public void gtnotgood$holdResult(ItemStack expected) {
        gtnotgood$expected = ItemStack.copyItemStack(expected);
        ((TEAltar) (Object) this).markDirty();
    }

    @Inject(method = "startCycle", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnotgood$holdCompletedResult(CallbackInfo callback) {
        if (gtnotgood$expected == null) return;
        TEAltar altar = (TEAltar) (Object) this;
        ItemStack actual = altar.getStackInSlot(0);
        if (actual == null) {
            gtnotgood$holdResult(null);
        } else if (TilePackagedProvider.sameItem(actual, gtnotgood$expected)) {
            // setActive is Blood Magic's public stop method despite its misleading name.
            altar.setActive();
            callback.cancel();
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true, require = 1)
    private void gtnotgood$read(NBTTagCompound tag, CallbackInfo callback) {
        gtnotgood$expected = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("GTNGBloodAltarResult"));
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void gtnotgood$write(NBTTagCompound tag, CallbackInfo callback) {
        if (gtnotgood$expected != null) {
            tag.setTag("GTNGBloodAltarResult", gtnotgood$expected.writeToNBT(new NBTTagCompound()));
        } else tag.removeTag("GTNGBloodAltarResult");
    }
}
