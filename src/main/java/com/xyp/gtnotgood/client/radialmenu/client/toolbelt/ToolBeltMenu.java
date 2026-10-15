package com.xyp.gtnotgood.client.radialmenu.client.toolbelt;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.radialmenu.client.action.IActionExecutor;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;
import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;
import com.xyp.gtnotgood.common.network.SwapItems;

public final class ToolBeltMenu implements IActionExecutor {

    public static final String TYPE = "toolbelt";
    private static final String SLOT = "slot";

    public static MenuNode create() {
        // #tr radialmenu.toolbelt.title
        // # Tool Belt
        // # zh_CN 工具腰带
        MenuNode menu = MenuNode.category(StatCollector.translateToLocal("radialmenu.toolbelt.title"),
            IconSpec.sprite("wrench", null), SlotLayout.fixed(ToolBeltData.SLOT_COUNT));
        menu.builtIn = TYPE;
        menu.layout.opening = SlotLayout.Opening.Inline;
        return menu;
    }

    public static void attach(MenuNode root) {
        if (root == null || !root.isCategory() || contains(root)) return;
        for (int i = 0; i < root.slotCount(); i++) {
            if (root.childAtIndex(i) == null) {
                root.setChildAt(i, create());
                return;
            }
        }
        int index = root.appendIndex();
        root.setChildAt(index, create());
        if (root.layoutOrDefault().mode == SlotLayout.Mode.Fixed && index >= SlotLayout.MAX_SLOTS) {
            root.layout = SlotLayout.dynamic();
        }
        root.ensureSlotCapacity();
    }

    private static boolean contains(MenuNode menu) {
        if (TYPE.equals(menu.builtIn)) return true;
        for (MenuNode child : menu.childrenOrEmpty()) {
            if (child != null && contains(child)) return true;
        }
        return false;
    }

    public static void refresh(MenuNode menu) {
        if (menu == null || !TYPE.equals(menu.builtIn)) return;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ToolBeltData data = ToolBeltData.get(player);
        List<MenuNode> slots = new ArrayList<>(ToolBeltData.SLOT_COUNT);
        for (int i = 0; i < ToolBeltData.SLOT_COUNT; i++) {
            ItemStack stack = data == null ? null : data.getStackInSlot(i);
            // #tr radialmenu.toolbelt.empty
            // # Empty slot %s - insert held tool
            // # zh_CN 空槽 %s：放入手持工具
            String title = stack == null ? StatCollector.translateToLocalFormatted("radialmenu.toolbelt.empty", i + 1)
                : stack.getDisplayName();
            IconSpec icon = stack == null ? IconSpec.sprite("plus", "#80FFFFFF") : IconSpec.fromStack(stack);
            MenuNode slot = MenuNode.leaf(title, icon, new ActionSpec(TYPE).set(SLOT, Integer.toString(i)));
            slot.builtIn = TYPE + ":" + i;
            slots.add(slot);
        }
        menu.liveChildren = slots;
    }

    @Override
    public String typeId() {
        return TYPE;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return false;
        int slot;
        try {
            slot = Integer.parseInt(spec.paramsOrEmpty().get(SLOT));
        } catch (NumberFormatException e) {
            return false;
        }
        if (slot < 0 || slot >= ToolBeltData.SLOT_COUNT) return false;
        ItemStack held = player.getHeldItem();
        if (held != null && !ConfigData.isItemStackAllowed(held)) return false;
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null || held == null && data.getStackInSlot(slot) == null) return true;
        GTNotGood.channel.sendToServer(new SwapItems(slot));
        return true;
    }
}
