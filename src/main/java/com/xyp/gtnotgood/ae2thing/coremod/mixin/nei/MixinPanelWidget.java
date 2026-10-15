package com.xyp.gtnotgood.ae2thing.coremod.mixin.nei;

import static com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config.getConfigValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.ae2thing.api.AE2ThingAPI;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.IGuiCraftAmount;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.ITerminal;
import com.xyp.gtnotgood.ae2thing.client.event.UpdateAmountTextEvent;
import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NeiCraftingFluid;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.util.Ae2ReflectClient;
import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.parts.IPart;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.me.ItemRepo;
import appeng.container.AEBaseContainer;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.helpers.MonitorableAction;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PanelWidget;
import codechicken.nei.Widget;
import codechicken.nei.guihook.IContainerTooltipHandler;

@Mixin(PanelWidget.class)
public abstract class MixinPanelWidget extends Widget implements IContainerTooltipHandler {

    @Shadow(remap = false)
    public abstract ItemStack getStackMouseOver(int mousex, int mousey);

    @Shadow(remap = false)
    public ItemStack draggedStack;

    @Inject(method = "handleClick", at = @At(value = "HEAD"), remap = false, cancellable = true)
    public void handleClick(int mousex, int mousey, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 2) return;
        try {
            ItemStack is = this.getStackMouseOver(mousex, mousey);
            if (is == null) return;
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if (gui instanceof IGuiCraftAmount g) {
                g.setAmount(Math.max(is.stackSize, 1));
                draggedStack = null;
                cir.setReturnValue(true);
            } else if (gui instanceof AEBaseGui g) {
                if (
                    !NEIClientUtils.altKey() && !NEIClientUtils.shiftKey()
                        && !GuiScreen.isCtrlKeyDown()
                        && g instanceof GuiQuickEncodingTerminal quickTerminal
                        && quickTerminal.requestFluidCraft(is)
                ) {
                    draggedStack = null;
                    cir.setReturnValue(true);
                    return;
                }
                IDisplayRepo repo = Util.getDisplayRepo(g);
                if (repo == null) return;
                if (NEIClientUtils.altKey()) {
                    repo.setSearchString(is.getDisplayName());
                    Util.setSearchFieldText(g, Platform.getItemDisplayName(is));
                } else if (g.inventorySlots instanceof AEBaseContainer c) {
                    IAEStack<?> craftTarget = getCraftTarget(is);
                    MonitorableAction action = null;
                    // shift / ctrl + middle-click should MOVE the item from the network straight into the player's
                    // inventory (like the vanilla ME terminal's shift+left-click), not pick it up onto the cursor.
                    // SHIFT_CLICK is the action that runs InventoryAdaptor.addItems into the player inventory; the old
                    // PICKUP_OR_SET_DOWN / PICKUP_SINGLE actions dropped the stack onto the mouse cursor instead.
                    if (NEIClientUtils.shiftKey() || GuiScreen.isCtrlKeyDown()) {
                        action = MonitorableAction.SHIFT_CLICK;
                    } else if (
                        getConfigValue(ButtonConstants.NEI_CRAFT_ITEM) && repo instanceof ItemRepo itemRepo
                            && canCraft(itemRepo, craftTarget)
                    ) {
                        craftTarget.setStackSize(craftTarget.isFluid() ? 1 : Math.max(is.stackSize, 1));
                        if (
                            c.getTarget() instanceof IGuiItemObject o && o.getItemStack() != null
                                && o.getItemStack().getItem() != null
                        ) {
                            openCraftAmount(o.getItemStack().getItem().getClass(), c, craftTarget);
                        } else if (c.getTarget() instanceof IPart p) {
                            ItemStack part = p.getItemStack(PartItemStack.Network);
                            if (part != null && part.getItem() != null) {
                                openCraftAmount(part.getItem().getClass(), c, craftTarget);
                            }
                        }
                    }
                    if (action != null) {
                        c.setTargetStack(AEItemStack.create(is));
                        final PacketMonitorableAction p = new PacketMonitorableAction(action, -1);
                        NetworkHandler.instance.sendToServer(p);
                    }
                }
                draggedStack = null;
                cir.setReturnValue(true);
            }
        } catch (Exception ignored) {}
    }

    private IAEStack<?> getCraftTarget(ItemStack stack) {
        FluidStack fluid = NeiCraftingFluid.getFluid(stack);
        return fluid == null ? AEItemStack.create(stack) : AEFluidStack.create(fluid);
    }

    private boolean canCraft(ItemRepo repo, IAEStack<?> target) {
        if (repo == null || target == null) return false;
        // Search the backing list so terminal filters cannot hide a craftable target.
        final IItemList<IAEStack<?>> list = Ae2ReflectClient.getList(repo);
        if (list == null) return false;
        final IAEStack<?> found = list.findPrecise(target);
        return found != null && found.isCraftable();
    }

    private void openCraftAmount(Class<? extends Item> o, AEBaseContainer c, IAEStack<?> target) {
        for (ITerminal terminal : AE2ThingAPI.instance().terminal().getTerminalSet()) {
            if (terminal.getClasses().contains(o)) {
                c.setTargetStack(target);
                terminal.openCraftAmount();
                MinecraftForge.EVENT_BUS.post(new UpdateAmountTextEvent((int) target.getStackSize()));
                break;
            }
        }
    }
}
