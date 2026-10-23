// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableList;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.quantum.CraftingCycleIndex;
import com.xyp.gtnotgood.client.quantum.ICraftingCycleHost;
import com.xyp.gtnotgood.client.quantum.ICraftingTreeCycles;
import com.xyp.gtnotgood.client.quantum.RecursivePatternButton;
import com.xyp.gtnotgood.client.quantum.RecursivePatternReport;
import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder.Cycle;
import com.xyp.gtnotgood.common.network.quantum.RecursivePatternQueryPacket;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.GuiSub;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.localization.ColorUtils;

/**
 * Finds the crafting cycles of the request once it is calculated, marks them in the crafting tree and the list, and
 * tells once where the machines of those the plan cannot start because of are. The button lists them all, the highlight
 * key highlights the cycle of what is under the mouse, and shift-clicking an item of a cycle in the list shows the
 * cycle as a ring in the tree view, left by its button or Esc.
 */
@Mixin(value = GuiCraftConfirm.class, remap = false)
public abstract class MixinGuiCraftConfirm extends GuiSub implements ICraftingCycleHost {

    @Unique
    // #tr gtng.quantum_crafting.message.CraftingNoNetOutput
    // # Patterns cannot yield a net output. Check cycles and missing external inputs.
    // # zh_CN 样板无法净产出请求物品，请检查循环样板及缺少的外部输入。
    private static final String GTNG$NO_NET_OUTPUT = "gtng.quantum_crafting.message.CraftingNoNetOutput";

    @Shadow
    @Final
    private ContainerCraftConfirm ccc;

    @Shadow
    @Final
    private GuiCraftingTree craftingTree;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> missing;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> storage;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> pending;

    @Shadow
    @Final
    private List<IAEStack<?>> filteredVisual;

    @Shadow
    private int tooltip;

    @Shadow
    private int rows;

    @Shadow
    private GuiTabButton switchDisplayMode;

    @Shadow
    private GuiCraftConfirm.DisplayMode displayMode;

    @Shadow
    private GuiAeButton hideAvailable;

    @Shadow
    private MEGuiTextField searchField;

    @Unique
    private RecursivePatternButton gtng$recursivePatterns;

    @Unique
    private GuiAeButton gtng$backToTree;

    @Unique
    private boolean gtng$queried;

    @Unique
    private CraftingCycleIndex gtng$index;

    // The ring shown was opened from the list, to which leaving it goes back
    @Unique
    private boolean gtng$ringFromList;

    @Unique
    private boolean gtng$listMarked;

    public MixinGuiCraftConfirm() {
        super(null);
    }

    @Unique
    private ICraftingTreeCycles gtng$tree() {
        return (ICraftingTreeCycles) this.craftingTree;
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    private void gtng$addButtons(CallbackInfo ci) {
        this.gtng$recursivePatterns = new RecursivePatternButton(this.guiLeft + this.xSize + 2,
            this.guiTop + 8 + 20 * 3);
        this.buttonList.add(this.gtng$recursivePatterns);
        // #tr gtng.gui.crafting_ring.back
        // # Back
        // # zh_CN 返回
        var back = StatCollector.translateToLocal("gtng.gui.crafting_ring.back");
        this.gtng$backToTree = new GuiAeButton(0, this.guiLeft + 9, this.guiTop + 20,
            Math.min(this.craftingTree.widgetW, this.fontRendererObj.getStringWidth(back) + 10), 18, back, "");
        this.gtng$backToTree.visible = false;
        this.buttonList.add(this.gtng$backToTree);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtng$buttons(GuiButton button, CallbackInfo ci) {
        if (button == this.gtng$recursivePatterns) {
            GTNotGood.channel.sendToServer(new RecursivePatternQueryPacket(true));
            ci.cancel();
        } else if (button == this.gtng$backToTree) {
            gtng$leaveRing();
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), remap = true)
    private void gtng$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        var error = this.ccc.getErrorMessage();
        if (!this.gtng$queried && (this.ccc.getUsedBytes() > 0 || !error.isEmpty())) {
            this.gtng$queried = true;
            GTNotGood.channel.sendToServer(new RecursivePatternQueryPacket(false));
        }
        if (this.gtng$recursivePatterns != null) {
            this.gtng$recursivePatterns.setAttention(
                GTNG$NO_NET_OUTPUT.equals(error) || this.gtng$index != null && !this.gtng$index.blocking().isEmpty());
        }
        if (gtng$tree().gtng$ringView() == null) this.gtng$ringFromList = false;
        boolean inRing = this.displayMode == GuiCraftConfirm.DisplayMode.TREE && gtng$tree().gtng$ringView() != null;
        if (this.gtng$backToTree != null) this.gtng$backToTree.visible = inRing;
        if (this.hideAvailable != null) this.hideAvailable.visible = !inRing;
    }

    @Inject(method = "drawFG", at = @At("TAIL"))
    private void gtng$ringSummary(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        var view = gtng$tree().gtng$ringView();
        if (view == null || this.displayMode != GuiCraftConfirm.DisplayMode.TREE || this.gtng$backToTree == null) {
            return;
        }
        var ring = view.ring();
        var text = StatCollector.translateToLocalFormatted(
            // #tr gtng.gui.crafting_ring.summary
            // # %s crafts, %s loops back, %s inputs
            // # zh_CN %s 次合成，%s 处绕回，%s 项外部输入
            "gtng.gui.crafting_ring.summary", ring.crafts(), ring.backEdges().size(), ring.externalInputs().size());
        if (view.omitted() > 0) {
            // #tr gtng.gui.crafting_ring.omitted
            // # , %s patterns not shown
            // # zh_CN ，另有 %s 个样板不在这一圈上
            text += StatCollector.translateToLocalFormatted("gtng.gui.crafting_ring.omitted", view.omitted());
        }
        int x = this.gtng$backToTree.xPosition - this.guiLeft + this.gtng$backToTree.width + 4;
        this.fontRendererObj.drawString(text, x, 25, ColorUtils.guiTextColorGray.getColor());
    }

    @Unique
    private static boolean gtng$has(IItemList<IAEStack<?>> list, IAEStack<?> item) {
        var found = list.findPrecise(item);
        return found != null && (found.getStackSize() > 0 || found.getCountRequestable() > 0);
    }

    /**
     * The cycles the plan cannot start because of: those without net output when that is the error, those short of
     * something of their own, and when the plan cannot start while showing nothing missing, as the lightweight and
     * standard calculations leave a cycle they could not close, those the plan goes through.
     */
    @Unique
    private List<Cycle> gtng$blocking(List<Cycle> cycles) {
        var error = this.ccc.getErrorMessage();
        boolean noNetOutput = GTNG$NO_NET_OUTPUT.equals(error);
        var blocking = new ArrayList<Cycle>();
        if (!noNetOutput && !this.ccc.isSimulation()) return blocking;
        for (var cycle : cycles) {
            boolean blocks = noNetOutput && !cycle.netOutput();
            for (var item : cycle.items()) {
                if (blocks) break;
                blocks = gtng$has(this.missing, item);
            }
            if (blocks) blocking.add(cycle);
        }
        if (blocking.isEmpty() && (noNetOutput || this.missing.isEmpty())) {
            for (var cycle : cycles) {
                for (var item : cycle.items()) {
                    if (gtng$has(this.storage, item) || gtng$has(this.pending, item)) {
                        blocking.add(cycle);
                        break;
                    }
                }
            }
        }
        if (blocking.isEmpty() && noNetOutput) blocking.addAll(cycles);
        return blocking;
    }

    @Override
    public void gtng$cyclesFound(IAEStack<?> request, List<Cycle> cycles, boolean manual) {
        var blocking = gtng$blocking(cycles);
        this.gtng$index = CraftingCycleIndex.of(request, cycles, blocking);
        gtng$tree().gtng$setCycles(this.gtng$index);
        if (manual) {
            RecursivePatternReport.show(this.mc, request, cycles);
        } else {
            RecursivePatternReport.showBlocking(this.mc, this.gtng$index);
        }
    }

    // One ring back, and from the last one back to where it was opened from
    @Unique
    private void gtng$leaveRing() {
        gtng$tree().gtng$closeRingView();
        if (gtng$tree().gtng$ringView() != null) return;
        if (this.gtng$ringFromList && this.displayMode == GuiCraftConfirm.DisplayMode.TREE) {
            this.actionPerformed(this.switchDisplayMode);
        }
        this.gtng$ringFromList = false;
    }

    // What the mouse is over in the list
    @Unique
    private IAEStack<?> gtng$listStack() {
        if (this.displayMode != GuiCraftConfirm.DisplayMode.LIST || this.tooltip < 0 || this.getScrollBar() == null) {
            return null;
        }
        int index = this.getScrollBar().getCurrentScroll() * 3 + this.tooltip;
        return index < this.filteredVisual.size() ? this.filteredVisual.get(index) : null;
    }

    @Inject(method = "drawTreeFG", at = @At("HEAD"), cancellable = true)
    private void gtng$drawRingWithoutTree(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        // A ring of a cycle found is shown whether or not there is a tree
        var view = gtng$tree().gtng$ringView();
        if (view != null && !view.ofTree()) {
            this.craftingTree.draw(mouseX, mouseY);
            ci.cancel();
        }
    }

    @Inject(method = "drawListFG", at = @At("HEAD"))
    private void gtng$startList(CallbackInfo ci) {
        this.gtng$listMarked = false;
    }

    @Inject(
        method = "drawListFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"))
    private void gtng$markListUnderTooltip(CallbackInfo ci) {
        gtng$markList();
    }

    @Inject(method = "drawListFG", at = @At("TAIL"))
    private void gtng$markListWithoutTooltip(CallbackInfo ci) {
        gtng$markList();
    }

    @Unique
    private void gtng$markList() {
        if (this.gtng$listMarked || this.gtng$index == null || this.getScrollBar() == null) return;
        this.gtng$listMarked = true;
        int viewStart = this.getScrollBar().getCurrentScroll() * 3;
        int viewEnd = Math.min(viewStart + 3 * this.rows, this.filteredVisual.size());
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int z = viewStart; z < viewEnd; z++) {
            var stack = this.filteredVisual.get(z);
            if (stack == null || this.gtng$index.cyclesWith(stack).isEmpty()) continue;
            int x = (z - viewStart) % 3 * 68 + 57;
            int y = (z - viewStart) / 3 * 23 + 22;
            if (this.gtng$index.blocks(stack)) GL11.glColor4f(1, 0.4f, 0.4f, 1);
            else GL11.glColor4f(1, 1, 1, 1);
            RecursivePatternButton.drawIcon(this.mc, x - 5, y - 5, 8, 300);
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopAttrib();
    }

    @ModifyArg(
        method = "drawListFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"),
        index = 2)
    private String gtng$listTooltip(String text) {
        var stack = gtng$listStack();
        if (stack == null || this.gtng$index == null) return text;
        var cycles = this.gtng$index.cyclesWith(stack);
        if (cycles.isEmpty()) return text;
        var lines = new StringBuilder(text);
        for (var cycle : cycles) {
            lines.append('\n').append(StatCollector.translateToLocalFormatted(
                // #tr gtng.gui.recursive_patterns.tooltip.net
                // # §aCycle %s (net output)
                // # zh_CN §a递归样板：循环 %s（有净产出）
                cycle.netOutput() ? "gtng.gui.recursive_patterns.tooltip.net"
                    // #tr gtng.gui.recursive_patterns.tooltip.no_net
                    // # §cCycle %s (no net output)
                    // # zh_CN §c递归样板：循环 %s（无净产出）
                    : "gtng.gui.recursive_patterns.tooltip.no_net",
                this.gtng$index.cycles().indexOf(cycle) + 1));
        }
        int machines = 0;
        for (var match : this.gtng$index.matchesFor(stack)) {
            machines += match.location().machines().size();
        }
        lines.append('\n').append(StatCollector.translateToLocalFormatted(
            // #tr gtng.gui.recursive_patterns.tooltip.machines
            // # §7%s machines, press %s to highlight
            // # zh_CN §7存放在 %s 台机器中，按 %s 高亮
            "gtng.gui.recursive_patterns.tooltip.machines", machines, RecursivePatternReport.highlightKeyName()))
            .append('\n')
            // #tr gtng.gui.crafting_ring.open_list
            // # §7Shift-click: view as ring
            // # zh_CN §7Shift+左键：以环形查看这个循环
            .append(StatCollector.translateToLocal("gtng.gui.crafting_ring.open_list"));
        return lines.toString();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtng$openRingFromList(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 0 || !isShiftKeyDown() || this.gtng$index == null) return;
        var stack = gtng$listStack();
        var cycles = stack == null ? ImmutableList.<Cycle>of() : this.gtng$index.cyclesWith(stack);
        if (cycles.isEmpty()) return;
        this.actionPerformed(this.switchDisplayMode);
        if (gtng$tree().gtng$openCycle(cycles.get(0), stack)) {
            this.gtng$ringFromList = true;
        } else {
            this.actionPerformed(this.switchDisplayMode);
        }
        ci.cancel();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtng$keys(char character, int key, CallbackInfo ci) {
        var tree = gtng$tree();
        if (key == Keyboard.KEY_ESCAPE && tree.gtng$ringView() != null) {
            gtng$leaveRing();
            ci.cancel();
            return;
        }
        if (!RecursivePatternReport.isHighlightKey(key) || this.searchField.isFocused()) return;
        var listStack = gtng$listStack();
        var hovered = this.displayMode == GuiCraftConfirm.DisplayMode.TREE ? tree.gtng$hoveredPatterns()
            : listStack != null && this.gtng$index != null ? this.gtng$index.matchesFor(listStack)
                : ImmutableList.<CraftingCycleIndex.Match>of();
        if (!hovered.isEmpty()) {
            RecursivePatternReport.highlight(this.mc, this.gtng$index.request(), hovered);
        } else if (this.gtng$index != null) {
            RecursivePatternReport.show(this.mc, this.gtng$index.request(), this.gtng$index.cycles());
        } else {
            GTNotGood.channel.sendToServer(new RecursivePatternQueryPacket(true));
        }
        ci.cancel();
    }
}
