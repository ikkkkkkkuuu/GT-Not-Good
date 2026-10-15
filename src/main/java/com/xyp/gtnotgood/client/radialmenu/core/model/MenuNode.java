package com.xyp.gtnotgood.client.radialmenu.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

public class MenuNode {

    public String title;

    public IconSpec icon;

    public boolean keepOpen;

    public SlotLayout layout;

    public MenuStyle style;

    public List<MenuNode> children;

    public ActionSpec action;

    public String builtIn;

    // Server inventory snapshots never belong in a saved menu profile.
    public transient List<MenuNode> liveChildren;

    public boolean isBuiltIn() {
        return builtIn != null;
    }

    public static MenuNode leaf(String title, IconSpec icon, ActionSpec action) {
        MenuNode node = new MenuNode();
        node.title = title;
        node.icon = icon;
        node.action = action;
        return node;
    }

    public static MenuNode category(String title, IconSpec icon, SlotLayout layout) {
        MenuNode node = new MenuNode();
        node.title = title;
        node.icon = icon;
        node.layout = layout;
        node.children = new ArrayList<>();
        return node;
    }

    public boolean isCategory() {
        return children != null;
    }

    public List<MenuNode> childrenOrEmpty() {
        if (liveChildren != null) return liveChildren;
        return children == null ? Collections.<MenuNode>emptyList() : children;
    }

    public SlotLayout layoutOrDefault() {
        return layout == null ? SlotLayout.dynamic() : layout;
    }

    public int slotCount() {
        if (layoutOrDefault().mode == SlotLayout.Mode.Fixed) {
            return layoutOrDefault().slotCount(childrenOrEmpty().size());
        }
        return filledCount();
    }

    public int filledCount() {
        int count = 0;
        for (MenuNode child : childrenOrEmpty()) {
            if (child != null) {
                count++;
            }
        }
        return count;
    }

    public int deepCount() {
        int count = 0;
        for (MenuNode child : childrenOrEmpty()) {
            if (child == null) {
                continue;
            }
            count++;
            count += child.deepCount();
        }
        return count;
    }

    public MenuNode childAt(int slotIndex) {
        int index = childIndexForSlot(slotIndex);
        return index < 0 ? null : childrenOrEmpty().get(index);
    }

    public MenuNode childAtIndex(int index) {
        List<MenuNode> kids = childrenOrEmpty();
        return index >= 0 && index < kids.size() ? kids.get(index) : null;
    }

    public int childIndexForSlot(int slotIndex) {
        List<MenuNode> kids = childrenOrEmpty();
        if (slotIndex < 0) {
            return -1;
        }
        if (layoutOrDefault().mode == SlotLayout.Mode.Fixed) {
            return slotIndex < kids.size() && kids.get(slotIndex) != null ? slotIndex : -1;
        }
        int seen = 0;
        for (int i = 0; i < kids.size(); i++) {
            if (kids.get(i) == null) {
                continue;
            }
            if (seen == slotIndex) {
                return i;
            }
            seen++;
        }
        return -1;
    }

    public int slotForChildIndex(int index) {
        if (index < 0) {
            return -1;
        }
        if (layoutOrDefault().mode == SlotLayout.Mode.Fixed) {
            return index;
        }
        List<MenuNode> kids = childrenOrEmpty();
        int seen = 0;
        for (int i = 0; i < index && i < kids.size(); i++) {
            if (kids.get(i) != null) {
                seen++;
            }
        }
        return seen;
    }

    public int appendIndex() {
        List<MenuNode> kids = childrenOrEmpty();
        int last = -1;
        for (int i = 0; i < kids.size(); i++) {
            if (kids.get(i) != null) {
                last = i;
            }
        }
        return last + 1;
    }

    public void ensureSlotCapacity() {
        if (isBuiltIn()) return;
        if (!isCategory()) {
            return;
        }
        if (layoutOrDefault().mode != SlotLayout.Mode.Fixed) {

            return;
        }
        if (children.size() > layout.slots) {
            layout.slots = SlotLayout.clampSlots(children.size());
        }
        int required = layoutOrDefault().slotCount(children.size());
        while (children.size() < required) {
            children.add(null);
        }
    }

    public void setChildAt(int slotIndex, MenuNode child) {
        if (isBuiltIn()) return;
        if (children == null) {
            children = new ArrayList<>();
        }
        while (children.size() <= slotIndex) {
            children.add(null);
        }
        children.set(slotIndex, child);
    }

    public boolean moveChild(int index, int direction) {
        if (isBuiltIn()) return false;
        if (!isCategory()) {
            return false;
        }
        return moveInList(children, index, direction, layoutOrDefault().mode == SlotLayout.Mode.Fixed);
    }

    public static boolean moveInList(List<MenuNode> kids, int index, int direction, boolean fixedLayout) {
        if (kids == null || direction == 0) {
            return false;
        }
        if (index < 0 || index >= kids.size() || kids.get(index) == null) {
            return false;
        }

        int target = -1;
        if (fixedLayout) {
            int candidate = index + direction;
            if (candidate >= 0 && candidate < kids.size()) {
                target = candidate;
            }
        } else {
            for (int i = index + direction; i >= 0 && i < kids.size(); i += direction) {
                if (kids.get(i) != null) {
                    target = i;
                    break;
                }
            }
        }

        if (target < 0) {
            return false;
        }
        MenuNode moved = kids.get(index);
        kids.set(index, kids.get(target));
        kids.set(target, moved);
        return true;
    }

    public MenuNode copy() {
        MenuNode copy = new MenuNode();
        copy.title = title;
        copy.icon = icon == null ? null : icon.copy();
        copy.keepOpen = keepOpen;
        copy.builtIn = builtIn;
        copy.liveChildren = liveChildren;
        copy.layout = layout;
        copy.style = style;
        copy.action = action == null ? null : action.copy();
        if (children != null) {
            copy.children = new ArrayList<>(children.size());
            for (MenuNode child : children) {
                copy.children.add(child == null ? null : child.copy());
            }
        }
        return copy;
    }

    public void normalize() {
        if (icon != null) {
            icon.normalize();
        }
        if (isCategory()) {
            action = null;
            if (layout == null) {
                layout = SlotLayout.dynamic();
            }
            layout.normalize();
            for (MenuNode child : children) {
                if (child != null) {
                    child.normalize();
                }
            }
            ensureSlotCapacity();
        } else {
            layout = null;
            style = null;
            if (action != null) {
                action.normalize();
            }
        }
    }
}
