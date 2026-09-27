package com.xyp.gtnotgood.common.compass;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.ModernThemeAdapter;

/** Handheld compass panel. Display values flow from the server; buttons revalidate the held item before any action. */
public final class StructureCompassGui {

    private final PlayerInventoryGuiData data;
    private final ModernThemeAdapter theme = new ModernThemeAdapter(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
    private final IntSyncValue mode, state, x, y, z, dimension, distance;

    public StructureCompassGui(PlayerInventoryGuiData data, PanelSyncManager sync) {
        this.data = data;
        mode = new IntSyncValue(() -> tag().getInteger("Mode") == 1 ? 1 : 0);
        state = new IntSyncValue(
            () -> StructureSearch.INSTANCE.isSearching(data.getPlayer()) ? 1
                : tag().getBoolean("Found") ? tag().getBoolean("Confirmed") ? 3 : 2 : 0);
        x = new IntSyncValue(() -> tag().getInteger("X"));
        y = new IntSyncValue(() -> tag().getInteger("Y"));
        z = new IntSyncValue(() -> tag().getInteger("Z"));
        dimension = new IntSyncValue(() -> tag().getInteger("Dimension"));
        distance = new IntSyncValue(() -> {
            EntityPlayer player = data.getPlayer();
            if (player.dimension != tag().getInteger("Dimension")) return -1;
            return (int) Math.hypot(tag().getInteger("X") + .5 - player.posX, tag().getInteger("Z") + .5 - player.posZ);
        });
        sync.syncValue("mode", mode);
        sync.syncValue("state", state);
        sync.syncValue("x", x);
        sync.syncValue("y", y);
        sync.syncValue("z", z);
        sync.syncValue("dimension", dimension);
        sync.syncValue("distance", distance);
    }

    private NBTTagCompound tag() {
        ItemStack compass = compass();
        return compass != null && compass.hasTagCompound() ? compass.getTagCompound() : new NBTTagCompound();
    }

    /**
     * Resolves the opening inventory slot each time. Vanilla replaces its stack with a copy after handling
     * the right-click packet, so retaining the construction-time object would reject every subsequent click.
     *
     * @return the current compass in the opening slot, or null if that slot no longer contains a compass
     */
    private ItemStack compass() {
        ItemStack stack = data.getPlayer().inventory.getStackInSlot(data.getSlotIndex());
        return stack != null && stack.getItem() instanceof StructureCompassItem ? stack : null;
    }

    public ModularPanel build() {
        ModularPanel panel = new ModularPanel("structure_compass").size(340, 222)
            .background(theme.panel);
        panel.child(
            IKey.lang("item.structure_compass.name")
                .color(0xff503b0d)
                .asWidget()
                .pos(14, 12));
        for (int target = 0; target < 2; target++) {
            final int selected = target;
            panel.child(
                button(
                    "target_" + target,
                    StructureCompassItem.modeKey(target),
                    14 + target * 159,
                    34,
                    153,
                    () -> StructureCompassItem.selectMode(data.getPlayer(), compass(), selected)).background(
                        new com.cleanroommc.modularui.drawable.DynamicDrawable(
                            () -> mode.getIntValue() == selected ? theme.hover : theme.button)));
        }
        panel.child(
            IKey.dynamic(() -> status(state.getIntValue()))
                .color(0xff655019)
                .asWidget()
                .pos(14, 68)
                .width(312));
        panel.child(
            IKey.dynamic(this::position)
                .color(0xff30384b)
                .asWidget()
                .pos(14, 88)
                .width(312));
        panel.child(
            IKey.dynamic(
                () -> state.getIntValue() < 2 ? ""
                    : distance.getIntValue() < 0 ? StatCollector.translateToLocal("compass.dimension")
                        : StatCollector.translateToLocalFormatted("compass.distance", distance.getIntValue()))
                .color(0xff525b69)
                .asWidget()
                .pos(14, 106)
                .width(312));
        // #tr gui.compass.search
        // # Search
        // # zh_CN 开始搜索
        panel.child(
            button(
                "search",
                "gui.compass.search",
                14,
                134,
                100,
                () -> StructureCompassItem.search(data.getPlayer(), compass()),
                () -> state.getIntValue() != 1));
        // #tr gui.compass.cancel
        // # Cancel search
        // # zh_CN 取消搜索
        panel.child(
            button(
                "cancel",
                "gui.compass.cancel",
                120,
                134,
                100,
                () -> StructureSearch.INSTANCE.cancel(data.getPlayer()),
                () -> state.getIntValue() == 1));
        // #tr gui.compass.clear
        // # Clear target
        // # zh_CN 清除目标
        panel.child(
            button(
                "clear",
                "gui.compass.clear",
                226,
                134,
                100,
                () -> StructureCompassItem
                    .selectMode(data.getPlayer(), compass(), StructureCompassItem.mode(compass()))));
        // #tr gui.compass.teleport
        // # Teleport to safe surface
        // # zh_CN 传送至目标附近安全地表
        panel.child(button("teleport", "gui.compass.teleport", 14, 163, 312, () -> {
            EntityPlayer player = data.getPlayer();
            if (player instanceof EntityPlayerMP && tag().getBoolean("Found")) {
                CompassTravel.teleport((EntityPlayerMP) player, compass());
                player.closeScreen();
            }
        }, () -> state.getIntValue() >= 2 && distance.getIntValue() >= 0));
        // #tr gui.compass.note
        // # Search predicts only. Teleport loads terrain. ESC closes.
        // # zh_CN 搜索仅预测；传送才加载地形。按 ESC 关闭。
        panel.child(
            IKey.lang("gui.compass.note")
                .color(0xff525b69)
                .asWidget()
                .pos(14, 199)
                .width(312));
        return panel;
    }

    /** Validates slot identity at execution time; no GUI construction-time access to PanelSyncManager.getPlayer(). */
    private ButtonWidget<?> button(String name, String key, int x, int y, int width, Runnable action) {
        return button(name, key, x, y, width, action, () -> true);
    }

    /** Keeps unavailable actions visible with a disabled appearance while rejecting their server-side clicks. */
    private ButtonWidget<?> button(String name, String key, int x, int y, int width, Runnable action,
        java.util.function.BooleanSupplier available) {
        return theme.button()
            .name(name)
            .pos(x, y)
            .size(width, 23)
            .background(
                new com.cleanroommc.modularui.drawable.DynamicDrawable(
                    () -> available.getAsBoolean() ? theme.button : theme.disabled))
            .hoverBackground(
                new com.cleanroommc.modularui.drawable.DynamicDrawable(
                    () -> available.getAsBoolean() ? theme.hover : theme.disabled))
            .overlay(
                IKey.lang(key)
                    .color(0xff30384b))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (mouse.isClient() || !available.getAsBoolean()) return;
                EntityPlayer player = data.getPlayer();
                if (player.isDead || player.inventory.currentItem != data.getSlotIndex() || compass() == null) return;
                action.run();
            }));
    }

    private String position() {
        if (state.getIntValue() < 2) return "X —    Y —    Z —";
        return "X " + x.getIntValue()
            + "    Y "
            + (state.getIntValue() == 2 ? "?" : y.getIntValue())
            + "    Z "
            + z.getIntValue()
            + "    DIM "
            + dimension.getIntValue();
    }

    private static String status(int state) {
        if (state == 1) {
            // #tr gui.compass.searching
            // # Searching seed candidates... Keep holding the compass.
            // # zh_CN 正在搜索种子候选区域……请保持手持。
            return StatCollector.translateToLocal("gui.compass.searching");
        }
        if (state == 2) {
            // #tr gui.compass.candidate
            // # Candidate area - approach to confirm the structure
            // # zh_CN 待确认区域 · 靠近后验证是否生成对应遗迹
            return StatCollector.translateToLocal("gui.compass.candidate");
        }
        if (state == 3) {
            // #tr gui.compass.confirmed
            // # Recorded structure - ready to navigate or teleport
            // # zh_CN 已记录遗迹 · 可以导航或直接传送
            return StatCollector.translateToLocal("gui.compass.confirmed");
        }
        // #tr gui.compass.idle
        // # Select a target and search within 8192 blocks
        // # zh_CN 请选择目标，搜索周围 8192 格区域
        return StatCollector.translateToLocal("gui.compass.idle");
    }
}
