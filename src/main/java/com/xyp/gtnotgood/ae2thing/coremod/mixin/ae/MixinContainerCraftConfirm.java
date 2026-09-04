package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import static com.xyp.gtnotgood.ae2thing.api.Constants.MessageType.ADD_PINNED_ITEM;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.network.SPacketMEItemInvUpdate;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftConfirm;

@Mixin(ContainerCraftConfirm.class)
public abstract class MixinContainerCraftConfirm extends AEBaseContainer {

    @Shadow(remap = false)
    protected ICraftingJob result;

    @Shadow(remap = false)
    public abstract boolean isSimulation();

    @Shadow(remap = false)
    public abstract IGrid getGrid();

    private IAEStack<?> is = null;

    public MixinContainerCraftConfirm(InventoryPlayer ip, ITerminalHost anchor) {
        super(ip, anchor);
    }

    @Inject(method = "setItemToCraft", at = @At("HEAD"), remap = false)
    public void setItemToCraft(IAEStack<?> itemToCraft, CallbackInfo ci) {
        if (itemToCraft != null) {
            is = itemToCraft.copy();
        }
    }

    @Inject(method = "startJob()V", at = @At("HEAD"), remap = false)
    public void startJob(CallbackInfo ci) {
        if (this.result != null && !this.isSimulation() && getGrid() != null && is != null) {
            SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate(ADD_PINNED_ITEM);
            piu.appendStack(is);
            AE2Thing.proxy.netHandler.sendTo(piu, (EntityPlayerMP) this.getPlayerInv().player);
            is = null;
        }
    }
}
