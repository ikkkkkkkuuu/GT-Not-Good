package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.network.CPacketCraftConfirmReplan;
import com.xyp.gtnotgood.ae2thing.util.NameConst;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;

@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends AEBaseGui {

    @Shadow(remap = false)
    private GuiAeButton start;

    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> storage;
    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> pending;
    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> missing;
    @Shadow(remap = false)
    @Final
    private List<IAEItemStack> visual;
    private GuiAeButton replan = null;
    private boolean clickStart = false;

    public MixinGuiCraftConfirm(Container container) {
        super(container);
    }

    @Inject(method = "actionPerformed", at = @At(value = "HEAD"))
    private void actionPerformed(GuiButton btn, CallbackInfo ci) {
        if (btn == start) {
            clickStart = true;
        } else if (btn == replan) {
            clickStart = false;
            start.enabled = false;
            replan.visible = false;
            // Do NOT cast to the concrete ItemList: when the crafting result contains fluids these lists are
            // GTNH's IAEStackList, not ItemList, so a cast throws ClassCastException (crash on Start/Replan for
            // any fluid craft). Both implementations expose a clear() method, so invoke it reflectively via the
            // IItemList interface instead.
            clearList(this.storage);
            clearList(this.pending);
            clearList(this.missing);
            this.visual.clear();
            AE2Thing.proxy.netHandler.sendToServer(new CPacketCraftConfirmReplan());
        }
    }

    private static void clearList(IItemList<IAEItemStack> list) {
        if (list == null) return;
        try {
            list.getClass()
                .getMethod("clear")
                .invoke(list);
        } catch (Throwable t) {
            // Fallback: at least reset the stack sizes so the stale view is cleared before replanning.
            list.resetStatus();
        }
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(
            replan = new GuiAeButton(
                0,
                start.xPosition,
                start.yPosition,
                start.width,
                start.height,
                // #tr gtnotgood.gui.button.replan
                // # Replan
                // # zh_CN 重新规划合成任务
                I18n.format(NameConst.GUI_BUTTON_REPLAN),
                ""));
        this.replan.visible = false;
    }

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false)
    public void drawFG(CallbackInfo ci) {
        try {
            if (clickStart || !start.enabled) {
                replan.visible = true;
                start.visible = false;
            } else {
                replan.visible = false;
                start.visible = true;
            }
        } catch (Exception ignored) {}

    }
}
