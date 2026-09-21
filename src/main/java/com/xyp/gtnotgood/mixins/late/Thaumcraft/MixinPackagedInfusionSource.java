package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.packaged.DirectEssentiaSupply;
import com.xyp.gtnotgood.common.packaged.InfusionSourceAccess;

import thaumcraft.common.tiles.TileInfusionMatrix;

/** A new manual craft must not inherit a completed Provider job's persistent source marker. */
@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class MixinPackagedInfusionSource implements InfusionSourceAccess {

    @Unique
    private NBTTagCompound gtnotgood$essentiaSource;

    @Redirect(
        method = "craftCycle",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/aspects/AspectList;reduce(Lthaumcraft/api/aspects/Aspect;I)Z"),
        require = 1)
    private boolean gtnotgood$batchSource(thaumcraft.api.aspects.AspectList remaining,
        thaumcraft.api.aspects.Aspect aspect, int amount) {
        return DirectEssentiaSupply.reduceBatch((TileInfusionMatrix) (Object) this, remaining, aspect, amount);
    }

    @Override
    public NBTTagCompound gtnotgood$getEssentiaSource() {
        return gtnotgood$essentiaSource;
    }

    @Override
    public void gtnotgood$setEssentiaSource(NBTTagCompound source) {
        gtnotgood$essentiaSource = source;
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), require = 1, remap = true)
    private void gtnotgood$readSource(NBTTagCompound tag, CallbackInfo callback) {
        gtnotgood$essentiaSource = tag.hasKey(DirectEssentiaSupply.TAG) ? tag.getCompoundTag(DirectEssentiaSupply.TAG)
            : null;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), require = 1, remap = true)
    private void gtnotgood$writeSource(NBTTagCompound tag, CallbackInfo callback) {
        if (gtnotgood$essentiaSource != null) tag.setTag(DirectEssentiaSupply.TAG, gtnotgood$essentiaSource);
        else tag.removeTag(DirectEssentiaSupply.TAG);
    }

    @Inject(method = "craftingStart", at = @At("HEAD"), require = 1)
    private void gtnotgood$resetSource(EntityPlayer player, CallbackInfo callback) {
        TileInfusionMatrix matrix = (TileInfusionMatrix) (Object) this;
        gtnotgood$essentiaSource = null;
        matrix.markDirty();
    }
}
