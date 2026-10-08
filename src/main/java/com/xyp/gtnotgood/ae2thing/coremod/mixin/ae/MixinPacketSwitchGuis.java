package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.ae2thing.client.gui.container.ContainerCraftConfirm;
import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.util.BlockPos;
import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketSwitchGuis;

/**
 * The wireless dual interface terminal opens its craft-amount / craft-confirm GUIs through our own
 * {@code CraftingConfirmItem} path, so the container's {@code primaryGui} is never populated. The vanilla Cancel /
 * back button on {@link appeng.client.gui.implementations.GuiCraftConfirm} sends a {@link PacketSwitchGuis} with a null
 * target, and vanilla's server handler then does {@code getPrimaryGui().open(player)} — which NPEs here because
 * primaryGui is null (the {@code assert} is disabled in production).
 *
 * <p>
 * When the open container is our wireless
 * {@link com.xyp.gtnotgood.ae2thing.client.gui.container.ContainerCraftConfirm},
 * route the "go back" action through its own {@code switchToOriginalGUI()} (which reopens the wireless terminal via our
 * InventoryHandler) and cancel the vanilla handling to avoid the crash.
 */
@Mixin(value = PacketSwitchGuis.class, remap = false)
public abstract class MixinPacketSwitchGuis {

    @Inject(method = "serverPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void routeWirelessBack(INetworkInfo manager, AppEngPacket packet, EntityPlayer player, CallbackInfo ci) {
        final Container c = player.openContainer;
        if (c instanceof ContainerCraftConfirm) {
            // Reopen the wireless dual interface terminal via the same reliable path CPacketSwitchGuis uses
            // (locate the terminal's inventory slot). ContainerCraftConfirm.switchToOriginalGUI() relies on
            // getActionHost() which does not resolve for the wireless host, so route it here directly.
            if (player instanceof EntityPlayerMP mp) {
                int slot = Util.findDualInterfaceTerminal(mp);
                if (slot != -1) {
                    InventoryHandler.openGui(mp, mp.worldObj, new BlockPos(slot, 0, 0), ForgeDirection.UNKNOWN,
                        GuiType.WirelessDualInterfaceTerminal);
                }
            }
            ci.cancel();
        }
    }
}
