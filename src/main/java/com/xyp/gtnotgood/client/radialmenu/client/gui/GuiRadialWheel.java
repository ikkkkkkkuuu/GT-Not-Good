package com.xyp.gtnotgood.client.radialmenu.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.client.radialmenu.client.action.ActionExecutors;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiMenuSettings;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiSlotEditor;
import com.xyp.gtnotgood.client.radialmenu.client.input.HeldKeyResync;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.script.ScriptHost;
import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.config.RadialMenuConfig;
import com.xyp.gtnotgood.client.radialmenu.config.WheelConfig;
import com.xyp.gtnotgood.client.radialmenu.core.geometry.ArcLayout;
import com.xyp.gtnotgood.client.radialmenu.core.geometry.RadialGeometry;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;
import com.xyp.gtnotgood.client.radialmenu.core.model.WheelColors;

public class GuiRadialWheel extends GuiScreen {

    private static final int MAX_RING_LEVELS = 5;

    private static final double MIN_INLINE_SLOT_SPAN = 8.0;

    private static final double MAX_INLINE_SLOT_SPAN = 45.0;

    private final Deque<MenuNode> path = new ArrayDeque<>();

    private final Deque<List<MenuNode>> unfoldedBehind = new ArrayDeque<>();

    private final Set<MenuNode> expanded = Collections.newSetFromMap(new IdentityHashMap<MenuNode, Boolean>());

    private boolean sticky;

    private boolean closed;

    private final boolean scripted;

    private Map<MenuNode, WheelAnimator> animators = new IdentityHashMap<>();

    private List<WheelRing> rings = Collections.emptyList();

    private int hoveredRing;

    private int hoveredSlot = RadialGeometry.NO_SLOT;

    private int pointerRing;

    private int pointerSlot = RadialGeometry.NO_SLOT;

    private MenuNode hoverCandidate;

    private long hoverCandidateSince;

    private MenuNode lastChosen;

    private MenuNode scriptInline;

    public GuiRadialWheel(MenuNode root) {
        this(root, true);
    }

    public GuiRadialWheel() {
        this(ProfileManager.active().root, false);
    }

    private GuiRadialWheel(MenuNode root, boolean scripted) {
        if (!scripted) ToolBeltMenu.attach(root);
        refreshBuiltIns(root);
        this.allowUserInput = RadialMenuConfig.allowInputWhileOpen;
        this.path.push(root);
        this.scripted = scripted;
        this.sticky = scripted;
    }

    public void replaceRoot(MenuNode root) {
        path.clear();
        unfoldedBehind.clear();
        path.push(root);
        forgetExpanded();
        lastChosen = null;
    }

    public boolean attachInline(MenuNode anchor, MenuNode submenu) {
        if (!scripted || anchor == null || submenu == null || submenu.childrenOrEmpty().isEmpty()) {
            return false;
        }
        if (RadialMenuConfig.scrollToSelect) {
            return false;
        }
        WheelRing parent = ringHolding(anchor);
        if (parent == null) {
            return false;
        }

        anchor.action = null;
        anchor.children = new ArrayList<>(submenu.childrenOrEmpty());
        anchor.layout = submenu.layoutOrDefault();
        anchor.layout.opening = SlotLayout.Opening.Inline;
        anchor.style = submenu.style;

        scriptInline = null;
        expand(parent, anchor);
        scriptInline = anchor;

        animators.remove(anchor);
        sticky = true;
        return true;
    }

    public MenuNode lastChosen() {
        return lastChosen;
    }

    private WheelRing ringHolding(MenuNode node) {
        for (WheelRing ring : rings) {
            for (MenuNode child : ring.menu.childrenOrEmpty()) {
                if (child == node) {
                    return ring;
                }
            }
        }
        return null;
    }

    private void forgetExpanded() {
        for (MenuNode node : new ArrayList<>(expanded)) {
            collapse(node);
        }
        expanded.clear();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        if (scripted) {
            ScriptHost.wheelClosed(this);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        closed = false;
        for (MenuNode menu : path) refreshBuiltIns(menu);
        animators = new IdentityHashMap<>();
        if (RadialMenuConfig.centerCursorOnOpen) {
            Mouse.setCursorPosition(this.mc.displayWidth / 2, this.mc.displayHeight / 2);
        }

        HeldKeyResync.resyncHeldKeys();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        for (MenuNode menu : path) ToolBeltMenu.refresh(menu);
        for (MenuNode menu : expanded) ToolBeltMenu.refresh(menu);
    }

    private static void refreshBuiltIns(MenuNode menu) {
        if (menu.isBuiltIn()) {
            ToolBeltMenu.refresh(menu);
            return;
        }
        for (MenuNode child : menu.childrenOrEmpty()) {
            if (child != null) refreshBuiltIns(child);
        }
    }

    public boolean isSticky() {
        return sticky;
    }

    private MenuNode currentMenu() {
        return path.peek();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        boolean editMode = isEditModifierDown();
        rings = buildRings(editMode);

        resolvePointer(mouseX, mouseY, centerX, centerY);

        if (RadialMenuConfig.scrollToSelect) {

            clampScrolledSelection();
        } else {
            hoveredRing = pointerRing;
            hoveredSlot = pointerSlot;

            if (expandHovered()) {
                rings = buildRings(editMode);
                resolvePointer(mouseX, mouseY, centerX, centerY);
                hoveredRing = pointerRing;
                hoveredSlot = pointerSlot;
            }
        }

        WheelColors colors = WheelRenderer.colorsFor(currentMenu(), editMode);

        if (WheelConfig.dimBackground && (colors.background >>> 24) != 0) {

            drawRect(0, 0, this.width, this.height, colors.background);
        }

        drawRings(centerX, centerY, colors, editMode);

        WheelRenderer.drawHeader(this.width, ProfileManager.activeName(), breadcrumb(), editMode, scripted);

        if (editMode && pointerSlot == RadialGeometry.NO_SLOT) {

            WheelRing selected = ringAt(hoveredRing);
            WheelRenderer.drawCenterHint(selected == null ? currentMenu() : selected.menu, centerX, centerY,
                hoveredSlot);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawRings(int centerX, int centerY, WheelColors baseColors, boolean editMode) {
        Map<MenuNode, WheelAnimator> live = new IdentityHashMap<>();

        for (int i = 0; i < rings.size(); i++) {
            WheelRing ring = rings.get(i);
            WheelAnimator animator = animatorFor(ring.menu, live);
            int selected = i == hoveredRing ? hoveredSlot : RadialGeometry.NO_SLOT;
            animator.advance(ring.slotCount, selected);
            WheelColors colors = i == 0 ? baseColors : WheelRenderer.colorsFor(ring.menu, editMode);
            WheelRenderer.drawRing(ring, colors, centerX, centerY, selected, animator);
        }

        animators = live;

        WheelRing hovered = ringAt(hoveredRing);
        if (hovered != null) {
            WheelRenderer.drawLabel(hovered.menu, centerX, centerY, hoveredSlot);
        }
    }

    private WheelAnimator animatorFor(MenuNode menu, Map<MenuNode, WheelAnimator> live) {
        WheelAnimator animator = animators.get(menu);
        if (animator == null) {
            animator = new WheelAnimator();
        }
        live.put(menu, animator);
        return animator;
    }

    private WheelRing ringAt(int index) {
        return index >= 0 && index < rings.size() ? rings.get(index) : null;
    }

    private List<WheelRing> buildRings(boolean editMode) {
        List<WheelRing> built = new ArrayList<>();
        MenuNode base = currentMenu();
        built.add(WheelRing.root(base, sectorCount(base, editMode)));

        double limit = Math.min(this.width, this.height) / 2.0 - 2.0;

        int levelStart = 0;
        for (int level = 0; level < MAX_RING_LEVELS; level++) {
            if (WheelRing.outerRadius(level + 1) > limit) {
                break;
            }
            int levelEnd = built.size();
            List<int[]> origins = new ArrayList<>();
            List<Double> centers = new ArrayList<>();
            List<Double> spans = new ArrayList<>();

            for (int index = levelStart; index < levelEnd; index++) {
                WheelRing ring = built.get(index);
                for (int slot = 0; slot < ring.slotCount; slot++) {
                    MenuNode child = ring.childAt(slot);
                    if (child == null || !isUnfolded(child)) {
                        continue;
                    }
                    int childSlots = sectorCount(child, editMode);
                    if (childSlots <= 0) {
                        continue;
                    }
                    origins.add(new int[] { index, slot, childSlots });
                    centers.add(ring.slotCenter(slot));
                    spans.add(childSlots * inlineSlotSpan(ring, level + 1));
                }
            }

            if (origins.isEmpty()) {
                break;
            }

            ArcLayout.Arc[] arcs = ArcLayout.place(toArray(centers), toArray(spans));
            for (int i = 0; i < origins.size(); i++) {
                int[] origin = origins.get(i);
                WheelRing parent = built.get(origin[0]);
                built.add(new WheelRing(parent.childAt(origin[1]), level + 1, arcs[i].start, arcs[i].span, origin[2],
                    origin[0], origin[1]));
            }
            levelStart = levelEnd;
        }
        return built;
    }

    private static double inlineSlotSpan(WheelRing parent, int level) {
        double childMid = (WheelRing.innerRadius(level) + WheelRing.outerRadius(level)) / 2.0;
        if (childMid <= 0.0) {
            return MIN_INLINE_SLOT_SPAN;
        }
        double span = parent.slotSpan() * parent.midRadius() / childMid;
        return Math.max(MIN_INLINE_SLOT_SPAN, Math.min(MAX_INLINE_SLOT_SPAN, span));
    }

    private static double[] toArray(List<Double> values) {
        double[] array = new double[values.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private boolean isUnfolded(MenuNode node) {
        return isInlineSubmenu(node) && expanded.contains(node);
    }

    private boolean isInlineSubmenu(MenuNode node) {
        return node != null && node.isCategory()
            && node.layoutOrDefault().isInline()
            && !RadialMenuConfig.scrollToSelect;
    }

    private boolean expandHovered() {
        MenuNode hovered = hoveredNode();
        if (hovered == null || !isInlineSubmenu(hovered) || expanded.contains(hovered)) {
            hoverCandidate = null;
            return false;
        }

        long now = System.currentTimeMillis();
        if (hoverCandidate != hovered) {
            hoverCandidate = hovered;
            hoverCandidateSince = now;
        }
        if (now - hoverCandidateSince < RadialMenuConfig.inlineHoverDelayMs) {
            return false;
        }

        hoverCandidate = null;
        expand(ringAt(hoveredRing), hovered);
        return true;
    }

    private void expand(WheelRing parent, MenuNode node) {
        if (parent != null) {
            for (MenuNode sibling : parent.menu.childrenOrEmpty()) {
                if (sibling != null && sibling != node) {
                    collapse(sibling);
                }
            }
        }
        expanded.add(node);
    }

    private void collapse(MenuNode node) {
        if (node == null) {
            return;
        }
        expanded.remove(node);
        if (node == scriptInline) {
            scriptInline = null;
            ScriptHost.inlineCollapsed(this);
        }
        for (MenuNode child : node.childrenOrEmpty()) {
            if (child != null && child.isCategory()) {
                collapse(child);
            }
        }
    }

    private void resolvePointer(int mouseX, int mouseY, int centerX, int centerY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        pointerRing = 0;
        pointerSlot = RadialGeometry.NO_SLOT;
        if (distance < WheelConfig.effectiveInnerRadius()) {
            return;
        }

        double angle = RadialGeometry.angleTo(centerX, centerY, mouseX, mouseY);
        int bestLevel = -1;
        for (int i = 0; i < rings.size(); i++) {
            WheelRing ring = rings.get(i);
            if (distance < ring.inner || ring.level <= bestLevel) {
                continue;
            }
            int slot = ring.slotAt(angle);
            if (slot < 0) {
                continue;
            }
            bestLevel = ring.level;
            pointerRing = i;
            pointerSlot = slot;
        }
    }

    private void clampScrolledSelection() {
        WheelRing ring = ringAt(hoveredRing);
        if (ring == null) {
            hoveredRing = 0;
            ring = ringAt(0);
        }
        int slotCount = ring == null ? 0 : ring.slotCount;
        hoveredSlot = slotCount <= 0 ? RadialGeometry.NO_SLOT : Math.min(hoveredSlot, slotCount - 1);
        if (hoveredSlot < 0 && slotCount > 0) {
            hoveredSlot = 0;
        }
    }

    private MenuNode hoveredNode() {
        WheelRing ring = ringAt(hoveredRing);
        return ring == null ? null : ring.childAt(hoveredSlot);
    }

    private MenuNode pointedNode() {
        WheelRing ring = ringAt(pointerRing);
        return ring == null ? null : ring.childAt(pointerSlot);
    }

    private int sectorCount(MenuNode menu, boolean editMode) {
        int count = menu.slotCount();
        if (editMode && menu.layoutOrDefault().mode == SlotLayout.Mode.Dynamic) {
            count++;
        }
        return count;
    }

    private boolean isEditModifierDown() {
        return !scripted && !RadialMenuConfig.rightClickToEdit && isShiftKeyDown();
    }

    private String breadcrumb() {
        StringBuilder builder = new StringBuilder();
        MenuNode[] nodes = path.toArray(new MenuNode[0]);
        List<List<MenuNode>> behind = new ArrayList<>(unfoldedBehind);

        for (int i = nodes.length - (scripted ? 1 : 2); i >= 0; i--) {
            if (i < behind.size()) {
                for (MenuNode inline : behind.get(i)) {
                    appendTitle(builder, inline);
                }
            }
            appendTitle(builder, nodes[i]);
        }
        return builder.toString();
    }

    private static void appendTitle(StringBuilder builder, MenuNode node) {
        if (node.title == null || node.title.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" > ");
        }
        builder.append(node.title);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        boolean editRequested = !scripted
            && (RadialMenuConfig.rightClickToEdit ? button == 1 : button == 0 && isShiftKeyDown());

        if (editRequested) {

            if (pointerSlot == RadialGeometry.NO_SLOT) {
                if (!currentMenu().isBuiltIn()) GuiStack.push(new GuiMenuSettings(currentMenu()));
            } else {
                openEditor();
            }
            return;
        }
        if (button == 1) {
            goBackOrClose();
            return;
        }
        if (button == 0) {
            activateHovered();
        }
    }

    private void openEditor() {
        WheelRing ring = ringAt(pointerRing);
        if (ring == null) {
            return;
        }
        MenuNode menu = ring.menu;
        if (menu.isBuiltIn()) return;
        int index = menu.childIndexForSlot(pointerSlot);
        MenuNode child = menu.childAtIndex(index);
        if (child != null && child.isBuiltIn()) return;
        if (index < 0) {
            index = menu.layoutOrDefault().mode == SlotLayout.Mode.Dynamic ? menu.appendIndex() : pointerSlot;
        }
        GuiStack.push(new GuiSlotEditor(menu, index));
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        if (!RadialMenuConfig.scrollToSelect) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        WheelRing ring = ringAt(hoveredRing);
        int slotCount = ring == null ? sectorCount(currentMenu(), isEditModifierDown()) : ring.slotCount;
        if (slotCount <= 0) {
            return;
        }
        int step = wheel > 0 ? -1 : 1;
        hoveredSlot = ((hoveredSlot < 0 ? 0 : hoveredSlot) + step + slotCount) % slotCount;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {

            close();
            return;
        }

    }

    public void onWheelKeyReleased() {
        if (closed) return;
        if (sticky) {
            return;
        }
        if (RadialMenuConfig.releaseToSelect) {
            activateHovered();
        } else {
            close();
        }
    }

    private void activateHovered() {
        if (closed) return;
        WheelRing ring = ringAt(hoveredRing);
        MenuNode selected = ring == null ? null : ring.childAt(hoveredSlot);
        if (selected == null) {
            close();
            return;
        }

        if (selected.isCategory()) {
            if (isInlineSubmenu(selected)) {

                expand(ring, selected);
                sticky = true;
                return;
            }
            enterMenu(ring, selected);
            return;
        }

        lastChosen = selected;
        if (selected.keepOpen) {
            sticky = true;
            ActionExecutors.enqueue(selected.action);
            return;
        }

        close();
        ActionExecutors.enqueue(selected.action);
    }

    private void enterMenu(WheelRing from, MenuNode selected) {
        Deque<MenuNode> opened = new ArrayDeque<>();
        for (WheelRing ring = from; ring != null && ring.level > 0; ring = ringAt(ring.parent)) {
            opened.push(ring.menu);
        }
        unfoldedBehind.push(new ArrayList<>(opened));
        path.push(selected);

        forgetExpanded();
        sticky = true;

        animators = new IdentityHashMap<>();

        hoveredRing = 0;
        hoveredSlot = RadialMenuConfig.scrollToSelect ? 0 : RadialGeometry.NO_SLOT;
    }

    private void goBackOrClose() {
        MenuNode pointed = pointedNode();
        if (pointed != null && expanded.contains(pointed)) {
            collapse(pointed);
            return;
        }
        WheelRing ring = ringAt(pointerRing);
        if (ring != null && ring.level > 0) {
            collapse(ring.menu);
            return;
        }

        if (path.size() > 1) {
            path.pop();
            forgetExpanded();

            if (!unfoldedBehind.isEmpty()) {
                expanded.addAll(unfoldedBehind.pop());
            }
            hoveredRing = 0;
            hoveredSlot = RadialGeometry.NO_SLOT;
        } else {
            close();
        }
    }

    private void close() {
        closed = true;
        Minecraft.getMinecraft().displayGuiScreen(null);
    }
}
