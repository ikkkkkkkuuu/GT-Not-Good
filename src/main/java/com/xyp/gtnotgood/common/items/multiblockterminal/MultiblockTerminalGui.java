package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.gtnewhorizon.structurelib.StructureLib;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.ModernThemeAdapter;

public final class MultiblockTerminalGui {

    private final PlayerInventoryGuiData data;
    private final ModernThemeAdapter theme = new ModernThemeAdapter(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
    private final IntSyncValue mode, fullBuild, noHatches, replace, mirror, ae, count;

    public MultiblockTerminalGui(PlayerInventoryGuiData data, PanelSyncManager sync) {
        this.data = data;
        mode = flag(sync, "mode", TerminalSettings::dismantle);
        fullBuild = flag(sync, "full_build", TerminalSettings::fullBuild);
        noHatches = flag(sync, "no_hatches", TerminalSettings::noHatches);
        replace = flag(sync, "replace", TerminalSettings::replace);
        mirror = flag(sync, "mirror", TerminalSettings::mirror);
        ae = flag(sync, "ae", TerminalSettings::ae);
        count = new IntSyncValue(() -> TerminalSettings.count(terminal()), value -> {
            ItemStack stack = editableTerminal();
            if (stack != null) TerminalSettings.setCount(stack, value);
        }).allowC2S();
        sync.syncValue("count", count);
    }

    public ModularPanel build() {
        ModularPanel panel = new ModularPanel("multiblock_terminal").size(340, 308).background(theme.panel);
        panel.child(IKey.lang("item.multiblock_terminal.name").color(0xff503b0d).asWidget().pos(14, 12));
        panel.child(
            button("mode", 14, 34, 312).overlay(IKey.dynamic(() -> modeLabel(mode.getIntValue())).color(0xff30384b))
                .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                    if (mouse.isClient()) return;
                    ItemStack stack = editableTerminal();
                    if (stack != null) TerminalSettings.setDismantle(stack, !TerminalSettings.dismantle(stack));
                })));
        // #tr gui.multiblock_terminal.count
        // # Master channel / repeat count
        // # zh_CN 主频道 / 重复层数
        panel.child(IKey.lang("gui.multiblock_terminal.count").color(0xff30384b).asWidget().pos(14, 68));
        panel.child(theme.textField().name("terminal_count").value(count).numbersInt(1, TerminalSettings.MAX_COUNT)
            .setMaxLength(3).pos(226, 63).size(100, 20));
        // #tr gui.multiblock_terminal.no_hatches
        // # Skip / keep hatches
        // # zh_CN 跳过 / 保留仓室
        panel.child(toggle("no_hatches", "gui.multiblock_terminal.no_hatches", 14, 90, 312, noHatches,
            TerminalSettings::noHatches, TerminalSettings::setNoHatches));
        // #tr gui.multiblock_terminal.replace
        // # Replace selected blocks
        // # zh_CN 替换选定方块
        panel.child(toggle("replace", "gui.multiblock_terminal.replace", 14, 118, 153, replace,
            TerminalSettings::replace, TerminalSettings::setReplace));
        // #tr gui.multiblock_terminal.mirror
        // # Mirror structure
        // # zh_CN 镜像结构
        panel.child(toggle("mirror", "gui.multiblock_terminal.mirror", 173, 118, 153, mirror, TerminalSettings::mirror,
            TerminalSettings::setMirror));
        // #tr gui.multiblock_terminal.ae
        // # Use bound AE network
        // # zh_CN 使用绑定的 AE 网络
        panel.child(toggle("ae", "gui.multiblock_terminal.ae", 14, 146, 312, ae, TerminalSettings::ae,
            TerminalSettings::setAe));
        // #tr gui.multiblock_terminal.channels
        // # Projection channels / block tiers
        // # zh_CN 投影频道 / 方块等级
        panel.child(button("projection_channels", 14, 174, 312)
            .overlay(IKey.lang("gui.multiblock_terminal.channels").color(0xff30384b))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (mouse.isClient() || editableTerminal() == null) return;
                EntityPlayer player = data.getPlayer();
                player.openGui(StructureLib.instance(), 0, player.worldObj, data.getSlotIndex(), 0, 0);
            })));
        // #tr gui.multiblock_terminal.registries
        // # Material source priorities
        // # zh_CN 材料源优先级
        panel.child(button("projection_registries", 14, 202, 312)
            .overlay(IKey.lang("gui.multiblock_terminal.registries").color(0xff30384b))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(
                mouse -> { if (mouse.isClient()) StructureLib.instance().proxy().displayConfigGUI("registries"); })));
        panel.child(button("full_build", 14, 230, 312)
            .overlay(IKey.dynamic(() -> buildLabel(fullBuild.getIntValue())).color(0xff30384b))
            // #tr gui.multiblock_terminal.native_note
            // # Native building keeps the projector budget, creative rebuilding, channels and material sources.
            // # zh_CN 原生建造保留投影仪预算、创造重建、频道与材料源。
            .addTooltipLine(IKey.lang("gui.multiblock_terminal.native_note"))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (mouse.isClient()) return;
                ItemStack stack = editableTerminal();
                if (stack != null) TerminalSettings.setFullBuild(stack, !TerminalSettings.fullBuild(stack));
            })));
        // #tr gui.multiblock_terminal.use
        // # Right-click a controller to project; Shift-right-click to build or dismantle.
        // # zh_CN 右键控制器显示投影；Shift 右键执行建造或拆除。
        panel.child(
            IKey.lang("gui.multiblock_terminal.use").color(0xff525b69).asWidget().scale(0.8f).pos(14, 263).width(390));
        // #tr gui.multiblock_terminal.channel_note
        // # Channels select block tiers; middle-click picking works as on the projector.
        // # zh_CN 频道用于选择方块等级；沿用投影仪的中键拾取操作。
        panel.child(IKey.lang("gui.multiblock_terminal.channel_note").color(0xff525b69).asWidget().scale(0.8f)
            .pos(14, 281).width(390));
        return panel;
    }

    private IntSyncValue flag(PanelSyncManager sync, String name, Function<ItemStack, Boolean> getter) {
        IntSyncValue value = new IntSyncValue(() -> getter.apply(terminal()) ? 1 : 0);
        sync.syncValue(name, value);
        return value;
    }

    private ButtonWidget<?> button(String name, int x, int y, int width) {
        return theme.button().name(name).pos(x, y).size(width, 23);
    }

    private ButtonWidget<?> toggle(String name, String key, int x, int y, int width, IntSyncValue state,
        Function<ItemStack, Boolean> getter, BiConsumer<ItemStack, Boolean> setter) {
        return button(name, x, y, width)
            .background(new DynamicDrawable(() -> !terminalOptionsEnabled() ? theme.disabled
                : state.getIntValue() == 1 ? theme.hover : theme.button))
            .hoverBackground(new DynamicDrawable(() -> terminalOptionsEnabled() ? theme.hover : theme.disabled))
            .overlay(IKey.dynamic(() -> StatCollector.translateToLocal(key) + ": " + stateLabel(state.getIntValue()))
                .color(0xff30384b))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (mouse.isClient()) return;
                ItemStack stack = editableTerminal();
                if (stack != null && (TerminalSettings.dismantle(stack) || TerminalSettings.fullBuild(stack)))
                    setter.accept(stack, !getter.apply(stack));
            }));
    }

    private boolean terminalOptionsEnabled() {
        return mode.getIntValue() == 1 || fullBuild.getIntValue() == 1;
    }

    private ItemStack terminal() {
        ItemStack stack = data.getPlayer().inventory.getStackInSlot(data.getSlotIndex());
        return stack != null && stack.getItem() instanceof MultiblockTerminalItem ? stack : null;
    }

    private ItemStack editableTerminal() {
        EntityPlayer player = data.getPlayer();
        return player.worldObj.isRemote || player.isDead || player.inventory.currentItem != data.getSlotIndex() ? null
            : terminal();
    }

    private static String modeLabel(int value) {
        if (value == 1) {
            // #tr gui.multiblock_terminal.mode.dismantle
            // # Dismantle mode
            // # zh_CN 拆除模式
            return StatCollector.translateToLocal("gui.multiblock_terminal.mode.dismantle");
        }
        // #tr gui.multiblock_terminal.mode.build
        // # Build mode
        // # zh_CN 建造模式
        return StatCollector.translateToLocal("gui.multiblock_terminal.mode.build");
    }

    private static String stateLabel(int value) {
        if (value == 1) {
            // #tr gui.multiblock_terminal.enabled
            // # On
            // # zh_CN 开启
            return StatCollector.translateToLocal("gui.multiblock_terminal.enabled");
        }
        // #tr gui.multiblock_terminal.disabled
        // # Off
        // # zh_CN 关闭
        return StatCollector.translateToLocal("gui.multiblock_terminal.disabled");
    }

    private static String buildLabel(int value) {
        if (value == 1) {
            // #tr gui.multiblock_terminal.build.full
            // # Build: Full structure
            // # zh_CN 建造方式：整机终端
            return StatCollector.translateToLocal("gui.multiblock_terminal.build.full");
        }
        // #tr gui.multiblock_terminal.build.native
        // # Build: Native projector
        // # zh_CN 建造方式：原生投影仪
        return StatCollector.translateToLocal("gui.multiblock_terminal.build.native");
    }
}
