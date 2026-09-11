package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.ObjectValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.IntegratedProductionFactory;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPreview;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryReservations;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRouting;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;

import cpw.mods.fml.common.network.ByteBufUtils;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.util.GTUtility;

/**
 * Modern machine panel with a BOX-style route list and server-authoritative recipe imports.
 */
public class IntegratedProductionFactoryGui extends GTNGModernMultiBlockBaseGui<IntegratedProductionFactory> {

    private final FactoryGraph visibleGraph = new FactoryGraph();
    private final FactoryReservations visibleReservations = new FactoryReservations();
    private IPanelHandler details;
    private IPanelHandler editor;
    private IPanelHandler transfer;
    private IPanelHandler clear;
    private boolean locked;
    private boolean draining;
    private String drainDetails = "";
    private final com.cleanroommc.modularui.value.StringValue routingCode = new com.cleanroommc.modularui.value.StringValue(
        "");
    private final com.cleanroommc.modularui.value.StringValue routeParallel = new com.cleanroommc.modularui.value.StringValue(
        "1");
    private IPanelHandler preview;
    private FactoryPreview.Snapshot previewSnapshot = new FactoryPreview.Snapshot();
    private final int[] previewPages = new int[2];
    private String previewCode = "";
    private List<String> nodeLines = new ArrayList<>();
    private List<NBTTagCompound> requirements = new ArrayList<>();
    private int requirementPage;
    private int selected = -1;
    private final com.cleanroommc.modularui.value.StringValue nodeEUtText = new com.cleanroommc.modularui.value.StringValue(
        "");
    private FactoryActions actions;

    public IntegratedProductionFactoryGui(IntegratedProductionFactory factory) {
        super(factory);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager manager) {
        super.registerSyncValues(manager);
        actions = new FactoryActions(multiblock, this);
        manager.syncValue(
            "factoryRequirements",
            new GenericListSyncHandler<>(
                multiblock::getRequirementTags,
                values -> requirements = values,
                ByteBufUtils::readTag,
                ByteBufUtils::writeTag,
                NBTTagCompound::equals,
                value -> (NBTTagCompound) value.copy()));
        manager.syncValue("factoryActions", actions);
        manager.syncValue(
            "factoryDraining",
            new StringSyncValue(
                () -> Boolean.toString(multiblock.isDraining()),
                value -> draining = Boolean.parseBoolean(value)));
        manager.syncValue(
            "factoryDrainDetails",
            new StringSyncValue(multiblock::getDrainDetails, value -> drainDetails = value));
        manager.syncValue(
            "factoryLocked",
            new StringSyncValue(
                () -> Boolean.toString(multiblock.isRoutingLocked()),
                value -> locked = Boolean.parseBoolean(value)));
        manager.syncValue("factoryEditorStatus", new StringSyncValue(multiblock::getEditorStatus, value -> {}));
        manager.syncValue(
            "factoryReservations",
            new GenericListSyncHandler<>(
                () -> Collections.singletonList(multiblock.getReservationsTag()),
                values -> { if (!values.isEmpty()) visibleReservations.read(values.get(0)); },
                ByteBufUtils::readTag,
                ByteBufUtils::writeTag,
                NBTTagCompound::equals,
                value -> (NBTTagCompound) value.copy()));
        manager.syncValue("factoryStatus", new StringSyncValue(multiblock::getFactoryStatus, value -> {}));
        manager.syncValue(
            "factoryGraph",
            new GenericListSyncHandler<>(
                () -> Collections.singletonList(
                    multiblock.getDraft()
                        .write()),
                values -> { if (!values.isEmpty()) visibleGraph.read(values.get(0)); },
                ByteBufUtils::readTag,
                ByteBufUtils::writeTag,
                NBTTagCompound::equals,
                value -> (NBTTagCompound) value.copy()));
        manager.syncValue("factoryNodeStatus", new GenericListSyncHandler<>(() -> {
            List<String> lines = new ArrayList<>();
            for (FactoryGraph.Node node : multiblock.getDraft().nodes)
                lines.add(node.id + ":" + multiblock.getNodeStatus(node.id));
            return lines;
        },
            values -> nodeLines = values,
            ByteBufUtils::readUTF8String,
            ByteBufUtils::writeUTF8String,
            String::equals,
            value -> value));
    }

    /** Job batching is automatic; the inherited power-panel multiplier must not expose an ineffective control. */
    @Override
    public boolean showMaxParallelRow() {
        return false;
    }

    /** Keeps the existing controls at their normal width and adds a persistent requirement column. */
    @Override
    public ModularPanel build(com.cleanroommc.modularui.factory.PosGuiData data, PanelSyncManager manager,
        com.cleanroommc.modularui.screen.UISettings settings) {
        ModularPanel panel = super.build(data, manager, settings).size(480, 260);
        panel.child(
            new com.cleanroommc.modularui.widget.ParentWidget<>().pos(200, 4)
                .size(276, 222)
                .background(new com.cleanroommc.modularui.drawable.Rectangle().color(0xff192331)));
        card(panel, 200, 4, 276, 20, 0xff304762);
        panel.child(
            new TextWidget<>(
                IKey.dynamic(() -> draining ? FactoryText.REFUND_LIST.text() : FactoryText.REQUIREMENTS.text()))
                    .pos(204, 8)
                    .size(264, 14)
                    .color(0xffedf3fa)
                    .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft));
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> draining ? drainDetails : FactoryText.REQUIREMENT_HELP.text()))
                .pos(204, 26)
                .size(264, 26)
                .color(0xffc8d2df)
                .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft));
        for (int i = 0; i < 5; i++) {
            final int row = i;
            panel.child(
                new TextWidget<>(IKey.dynamic(() -> requirementLine(row))).pos(204, 57 + i * 28)
                    .size(264, 26)
                    .color(0xffc8d2df)
                    .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft)
                    .tooltipBuilder(t -> {
                        t.setAutoUpdate(true);
                        String line = requirementLine(row);
                        if (!line.isEmpty()) t.addLine(line);
                        NBTTagCompound requirement = requirement(row);
                        if (requirement != null && requirement.getCompoundTag("key")
                            .hasKey("map")) {
                            t.addLine(FactoryText.HOST_HELP.text());
                        }
                    }));
        }
        panel.child(button(() -> "<", 204, 204, 32, () -> requirementPage = Math.max(0, currentRequirementPage() - 1)));
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> (currentRequirementPage() + 1) + "/" + requirementPages()))
                .pos(242, 207)
                .size(185, 14)
                .color(0xffedf3fa));
        panel.child(
            button(
                () -> ">",
                436,
                204,
                32,
                () -> requirementPage = Math.min(requirementPages() - 1, currentRequirementPage() + 1)));
        panel.child(
            new TextWidget<>(IKey.dynamic(FactoryText.AUTO_PARALLEL::text)).pos(8, 233)
                .size(464, 18)
                .color(0xff304762));
        return panel;
    }

    private int requirementPages() {
        return Math.max(1, (requirements.size() + 4) / 5);
    }

    private int currentRequirementPage() {
        return Math.min(requirementPage, requirementPages() - 1);
    }

    private NBTTagCompound requirement(int row) {
        int index = currentRequirementPage() * 5 + row;
        return index < requirements.size() ? requirements.get(index) : null;
    }

    /** Includes the programmed circuit configuration, which GT stores in damage rather than its display name. */
    private static String itemName(ItemStack item) {
        if (item == null) return FactoryText.INVALID.text();
        String name = item.getDisplayName();
        if (item.getItem() instanceof gregtech.common.items.ItemIntegratedCircuit) {
            int metadata = item.getItemDamage();
            String mode = switch ((byte) (metadata >>> 8)) {
                case 0 -> "#";
                case 1 -> "<=";
                case 2 -> ">=";
                case 3 -> "<";
                case 4 -> ">";
                default -> "? ";
            };
            name += " (" + mode + (byte) (metadata & 0xFF) + ")";
        }
        return name;
    }

    /** Names are translated on the viewing client; the server supplies only identities and counts. */
    private String requirementLine(int row) {
        NBTTagCompound data = requirement(row);
        if (data == null) return row == 0 && requirements.isEmpty()
            ? (draining ? FactoryText.REFUND_EMPTY.text() : FactoryText.NO_REQUIREMENTS.text())
            : "";
        NBTTagCompound key = data.getCompoundTag("key");
        String name;
        if (key.hasKey("map")) {
            name = FactoryText.CONTROLLER.text() + ": "
                + net.minecraft.util.StatCollector.translateToLocal(key.getString("map"));
        } else {
            ItemStack item = ItemStack.loadItemStackFromNBT(key.getCompoundTag("item"));
            name = itemName(item);
        }
        int received = data.getInteger("received"), required = data.getInteger("required");
        if (draining) return "§e" + name + " ×" + required;
        return (received < required ? "§e" : "§a") + name
            + "  "
            + received
            + "/"
            + required
            + (received < required ? "  " + FactoryText.MISSING.text() + " " + (required - received) : " ✓");
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager manager) {
        StringSyncValue status = manager.findSyncHandler("factoryStatus", StringSyncValue.class);
        // Relative anchors in the inherited terminal/inventory rows must resolve inside the left column.
        return super.createMainColumn(panel, manager).pos(0, 0)
            .size(198, 230)
            .crossAxisAlignment(com.cleanroommc.modularui.utils.Alignment.CrossAxis.START)
            .child(
                new TextWidget<>(IKey.dynamic(status::getStringValue)).width(186)
                    .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft)
                    .height(24));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager manager) {
        preview = manager.syncedPanel("factoryPreview", true, (sm, sh) -> createPreview());
        details = manager.syncedPanel("factoryNode", true, (sm, sh) -> createDetails());
        transfer = manager.syncedPanel("factoryTransfer", true, (sm, sh) -> createTransfer());
        clear = manager.syncedPanel("factoryClear", true, (sm, sh) -> createClear());
        editor = manager.syncedPanel("factoryEditor", true, (sm, sh) -> createEditor(manager));
        return super.createButtonColumn(panel, manager).child(
            new ButtonWidget<>().size(18)
                .background(GTNGGuiTextures.MODERN_BUTTON)
                .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
                .tooltipBuilder(t -> t.addLine(FactoryText.EDIT.text()))
                .onMousePressed(button -> {
                    editor.openPanel();
                    return true;
                }));
    }

    /** BOX-style list: NEI appends routes directly, details edit one route, and preview confirms the installation. */
    private ModularPanel createEditor(PanelSyncManager manager) {
        ModularPanel panel = ModularPanel.defaultPanel("factoryEditor", 360, 254)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        card(panel, 6, 5, 326, 20, 0xffadc2d7);
        card(panel, 6, 27, 222, 189, 0xffb6bdc9);
        card(panel, 232, 27, 120, 189, 0xffbec6d1);
        card(panel, 6, 218, 346, 30, 0xffadbccc);
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> FactoryText.EDIT.text() + "  " + visibleGraph.nodes.size() + "/32"))
                .pos(10, 8)
                .size(260, 14));
        com.cleanroommc.modularui.widget.ScrollWidget<?> list = new com.cleanroommc.modularui.widget.ScrollWidget<>(
            new com.cleanroommc.modularui.widget.scroll.VerticalScrollData()).pos(10, 30)
                .size(214, 183);
        for (int i = 0; i < FactoryGraph.MAX_NODES; i++) {
            final int index = i;
            com.cleanroommc.modularui.widget.ParentWidget<?> row = new com.cleanroommc.modularui.widget.ParentWidget<>()
                .pos(0, i * 27)
                .size(208, 25)
                .background(
                    new com.cleanroommc.modularui.drawable.Rectangle().color(i % 2 == 0 ? 0xffd9dee6 : 0xffcbd2dd))
                .setEnabledIf(w -> index < visibleGraph.nodes.size());
            row.child(new ItemDisplayWidget().item(new ObjectValue.Dynamic<>(ItemStack.class, () -> {
                FactoryGraph.Node node = route(index);
                FactoryRecipeCatalog.Entry entry = node == null ? null : FactoryRecipeCatalog.get(node.recipe);
                return entry == null ? null : controllerIcon(entry);
            }, value -> {}))
                .pos(0, 3));
            row.child(new TextWidget<>(IKey.dynamic(() -> {
                FactoryGraph.Node node = route(index);
                return node == null ? "" : "#" + (index + 1) + "  P ×" + node.parallel;
            })).pos(22, 5)
                .size(75, 16));
            row.child(button(FactoryText.ROUTE_DETAILS::text, 100, 2, 50, () -> {
                FactoryGraph.Node node = route(index);
                if (node == null) return;
                selected = node.id;
                routeParallel.setStringValue(Integer.toString(node.parallel));
                nodeEUtText.setStringValue("");
                details.openPanel();
            }));
            row.child(button(() -> "×", 158, 2, 28, () -> {
                FactoryGraph.Node node = route(index);
                if (node != null && shiftDown()) actions.send(1, node.id, 0, 0, "");
            }).setEnabledIf(w -> !locked)
                .tooltipBuilder(t -> t.addLine(FactoryText.SHIFT_DELETE.text())));
            list.child(row);
        }
        list.onUpdateListener(
            w -> w.getScrollArea()
                .getScrollY()
                .setScrollSize(visibleGraph.nodes.size() * 27));
        panel.child(list);
        panel.child(button(FactoryText.PREVIEW::text, 236, 31, 112, this::openPreview));
        panel.child(button(() -> "×2", 236, 57, 52, () -> actions.send(14, 0, 0, 0, "")).setEnabledIf(w -> !locked));
        panel.child(button(() -> "÷2", 296, 57, 52, () -> actions.send(15, 0, 0, 0, "")).setEnabledIf(w -> !locked));
        panel.child(
            button(FactoryText.BALANCE::text, 236, 83, 112, () -> actions.send(9, 0, 0, 0, ""))
                .setEnabledIf(w -> !locked));
        panel.child(
            button(FactoryText.PATTERN_EXPORT::text, 236, 57, 112, () -> actions.send(18, 0, 0, 0, ""))
                .setEnabledIf(w -> locked)
                .tooltipBuilder(t -> t.addLine(FactoryText.PATTERN_HELP.text())));
        panel.child(button(FactoryText.ROUTE_CODE::text, 236, 109, 112, () -> {
            routingCode.setStringValue(visibleGraph.nodes.isEmpty() ? "" : FactoryRouting.encode(visibleGraph));
            transfer.openPanel();
        }));
        panel.child(button(FactoryText.CLEAR::text, 236, 135, 112, () -> clear.openPanel()));
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> locked ? FactoryText.LOCKED.text() : FactoryText.IMPORT_HELP.text()))
                .pos(236, 164)
                .size(112, 48));
        StringSyncValue status = manager.findSyncHandler("factoryEditorStatus", StringSyncValue.class);
        panel.child(
            new TextWidget<>(IKey.dynamic(status::getStringValue)).pos(10, 222)
                .size(338, 24));
        return panel;
    }

    private FactoryGraph.Node route(int index) {
        return index < visibleGraph.nodes.size() ? visibleGraph.nodes.get(index) : null;
    }

    private static boolean shiftDown() {
        return net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
    }

    private void openPreview() {
        FactoryGraph graph = visibleGraph.copy();
        previewCode = FactoryRouting.encode(graph);
        FactoryRouting.connect(graph);
        previewSnapshot = FactoryPreview.describe(graph);
        previewPages[0] = previewPages[1] = 0;
        preview.openPanel();
    }

    /** Text transfer uses local recipe identities, with complete validation on the server before importing. */
    private ModularPanel createTransfer() {
        ModularPanel panel = ModularPanel.defaultPanel("factoryTransfer", 360, 112)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            new TextWidget<>(IKey.dynamic(FactoryText.ROUTE_CODE_HELP::text)).pos(10, 10)
                .size(320, 34));
        panel.child(
            new com.cleanroommc.modularui.widgets.textfield.TextFieldWidget().value(routingCode)
                .setMaxLength(8192)
                .pos(10, 48)
                .size(340, 18));
        panel.child(
            button(
                FactoryText.IMPORT_CODE::text,
                10,
                80,
                160,
                () -> actions.send(17, 0, 0, 0, routingCode.getStringValue()))
                    .setEnabledIf(w -> !locked && visibleGraph.nodes.isEmpty()));
        panel.child(button(FactoryText.COPY_CODE::text, 190, 80, 160, this::copyCode));
        return panel;
    }

    private void copyCode() {
        net.minecraft.client.gui.GuiScreen.setClipboardString(routingCode.getStringValue());
    }

    /** Explicit clear confirmation preserves the machine's draining and deposit-refund semantics. */
    private ModularPanel createClear() {
        ModularPanel panel = ModularPanel.defaultPanel("factoryClear", 280, 100)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            new TextWidget<>(IKey.dynamic(FactoryText.CLEAR_HELP::text)).pos(10, 10)
                .size(252, 42));
        panel.child(button(FactoryText.CLEAR::text, 10, 67, 120, () -> {
            actions.send(16, 0, 0, 0, "");
            clear.closePanel();
        }));
        panel.child(button(() -> "×", 150, 67, 120, () -> clear.closePanel()));
        return panel;
    }

    /** Snapshot preview is computed locally from server-confirmed draft values; opening it never consumes items. */
    private ModularPanel createPreview() {
        ModularPanel panel = ModularPanel.defaultPanel("factoryPreview", 510, 330)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        card(panel, 6, 5, 477, 20, 0xffadc2d7);
        card(panel, 6, 27, 498, 20, 0xffb4c2d1);
        card(panel, 6, 49, 498, 26, 0xff303c4e);
        card(panel, 6, 76, 240, 219, 0xff243243);
        card(panel, 264, 76, 240, 219, 0xff253b39);
        card(panel, 6, 76, 240, 3, 0xff69bbef);
        card(panel, 264, 76, 240, 3, 0xff73d4ad);
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            new TextWidget<>(IKey.dynamic(FactoryText.PREVIEW::text)).pos(10, 8)
                .size(460, 14));
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> previewSnapshot.info)).pos(10, 29)
                .size(490, 18));
        panel.child(
            new TextWidget<>(
                IKey.dynamic(
                    () -> previewSnapshot.exportIssue() == null ? FactoryText.PREVIEW_NOTE.text()
                        : "§e" + previewSnapshot.exportIssue()
                            .text())).pos(10, 50)
                                .size(490, 26)
                                .color(0xffdae3ee)
                                .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft));
        for (int side = 0; side < 2; side++) {
            final int column = side;
            int x = side == 0 ? 10 : 268;
            panel.child(
                new TextWidget<>(
                    IKey.dynamic(
                        () -> (column == 0 ? FactoryText.EXTERNAL_INPUT.text() : FactoryText.OUTPUTS.text()) + " ("
                            + FactoryText.PER_TICK.text()
                            + ")")).pos(x + 4, 82)
                                .size(224, 14)
                                .color(column == 0 ? 0xff92d3ff : 0xff9be9c7)
                                .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft));
            for (int slot = 0; slot < 24; slot++) {
                final int index = slot;
                panel.child(
                    new com.xyp.gtnotgood.common.gui.modularui.widget.FactoryIngredientWidget(
                        () -> previewIngredient(column, index)).pos(x + (slot % 6) * 39, 99 + (slot / 6) * 40)
                            .size(35)
                            .setEnabledIf(w -> previewIngredient(column, index) != null));
            }
            panel.child(
                button(() -> "<", x, 270, 32, () -> previewPages[column] = Math.max(0, previewPages[column] - 1)));
            panel.child(
                new TextWidget<>(IKey.dynamic(() -> (previewPages[column] + 1) + "/" + previewPageCount(column)))
                    .pos(x + 36, 273)
                    .size(160, 14)
                    .color(0xffe1e8f1));
            panel.child(
                button(
                    () -> ">",
                    x + 200,
                    270,
                    32,
                    () -> previewPages[column] = Math.min(previewPageCount(column) - 1, previewPages[column] + 1)));
        }
        panel.child(button(FactoryText.CONFIRM_LOCK::text, 176, 303, 158, () -> {
            actions.send(5, 0, 0, 0, previewCode);
            preview.closePanel();
        }).background(new com.cleanroommc.modularui.drawable.Rectangle().color(0xff9dd5bc))
            .hoverBackground(new com.cleanroommc.modularui.drawable.Rectangle().color(0xffb7ebd4))
            .setEnabledIf(w -> !locked && !visibleGraph.nodes.isEmpty()));
        return panel;
    }

    private int previewPageCount(int side) {
        return Math.max(1, ((side == 0 ? previewSnapshot.inputs : previewSnapshot.outputs).size() + 23) / 24);
    }

    private FactoryPreview.Ingredient previewIngredient(int side, int slot) {
        List<FactoryPreview.Ingredient> entries = side == 0 ? previewSnapshot.inputs : previewSnapshot.outputs;
        int index = previewPages[side] * 24 + slot;
        return index < entries.size() ? entries.get(index) : null;
    }

    private void tune(int parallel, int oc) {
        FactoryGraph.Node node = visibleGraph.find(selected);
        if (node != null) actions.send(
            4,
            selected,
            (int) Math.max(1, Math.min(Integer.MAX_VALUE, (long) node.parallel + parallel)),
            node.overclocks + oc,
            "");
    }

    private String nodeLine(int id) {
        String prefix = id + ":";
        for (String line : nodeLines) if (line.startsWith(prefix)) return line.substring(prefix.length());
        return "";
    }

    /** BOX route detail: scrollable ingredients above machine, power, duration and editable route parallel. */
    private ModularPanel createDetails() {
        ModularPanel panel = ModularPanel.defaultPanel("factoryNode", 360, 304)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        card(panel, 6, 5, 326, 20, 0xffadc2d7);
        card(panel, 6, 26, 348, 154, 0xffb6bdc9);
        card(panel, 6, 181, 348, 35, 0xffb5c5d4);
        card(panel, 6, 218, 348, 56, 0xffc2cad5);
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            new TextWidget<>(
                IKey.dynamic(
                    () -> FactoryText.NODE_DETAILS.text() + " #"
                        + (visibleGraph.nodes.indexOf(visibleGraph.find(selected)) + 1))).pos(10, 8)
                            .size(320, 14));
        com.cleanroommc.modularui.widget.ScrollWidget<?> list = new com.cleanroommc.modularui.widget.ScrollWidget<>(
            new com.cleanroommc.modularui.widget.scroll.VerticalScrollData()).pos(10, 29)
                .size(340, 148);
        for (int i = 0; i < 96; i++) {
            final int index = i;
            com.cleanroommc.modularui.widget.ParentWidget<?> row = new com.cleanroommc.modularui.widget.ParentWidget<>()
                .pos(0, i * 20)
                .size(330, 20)
                .background(
                    new com.cleanroommc.modularui.drawable.Rectangle().color(i % 2 == 0 ? 0xffdce1e7 : 0xffcbd2dd))
                .setEnabledIf(w -> index < detailIngredients().size());
            row.child(
                new ItemDisplayWidget()
                    .item(new ObjectValue.Dynamic<>(ItemStack.class, () -> detailItem(index), value -> {}))
                    .displayAmount(true)
                    .pos(0, 1));
            row.child(new TextWidget<>(IKey.dynamic(() -> {
                List<DetailIngredient> entries = detailIngredients();
                if (index >= entries.size()) return "";
                DetailIngredient entry = entries.get(index);
                return entry.label.text() + ": " + itemName(entry.item);
            })).pos(22, 3)
                .size(304, 16)
                .textAlign(com.cleanroommc.modularui.utils.Alignment.CenterLeft));
            list.child(row);
        }
        list.onUpdateListener(
            w -> w.getScrollArea()
                .getScrollY()
                .setScrollSize(detailIngredients().size() * 20));
        panel.child(list);
        panel.child(new TextWidget<>(IKey.dynamic(() -> {
            FactoryRecipeCatalog.Entry entry = selectedRecipe();
            FactoryGraph.Node node = visibleGraph.find(selected);
            if (entry == null || node == null) return FactoryText.EMPTY.text();
            try {
                long[] timing = FactoryGraph.timing(
                    node.customEUt < 0 ? entry.recipe.mEUt : node.customEUt,
                    entry.recipe.mDuration,
                    node.parallel,
                    node.overclocks);
                return net.minecraft.util.StatCollector.translateToLocal(entry.map.unlocalizedName) + "\n"
                    + timing[0]
                    + " EU/t | "
                    + timing[1]
                    + " t | OC "
                    + node.overclocks;
            } catch (ArithmeticException invalid) {
                return FactoryText.LIMIT.text();
            }
        })).pos(10, 184)
            .size(340, 30));
        panel.child(
            new TextWidget<>(IKey.dynamic(FactoryText.PARALLEL::text)).pos(10, 224)
                .size(50, 15));
        // MUI2's dragged-root background pass ignores disabled direct children. A backgroundless
        // disabled parent stops traversal before its editing fields/buttons can paint their backgrounds.
        com.cleanroommc.modularui.widget.ParentWidget<?> editing = new com.cleanroommc.modularui.widget.ParentWidget<>()
            .pos(0, 0)
            .size(360, 304)
            .setEnabledIf(w -> !locked);
        panel.child(editing);
        editing.child(
            new com.cleanroommc.modularui.widgets.textfield.TextFieldWidget().value(routeParallel)
                .setMaxLength(10)
                .pos(64, 222)
                .size(52, 18)
                .setEnabledIf(w -> !locked));
        editing.child(button(FactoryText.SET::text, 122, 222, 52, () -> {
            FactoryGraph.Node node = visibleGraph.find(selected);
            if (node == null) return;
            try {
                actions.send(4, selected, Integer.parseInt(routeParallel.getStringValue()), node.overclocks, "");
            } catch (NumberFormatException ignored) {}
        }).setEnabledIf(w -> !locked));
        editing.child(button(() -> "OC−", 184, 222, 76, () -> tune(0, -1)).setEnabledIf(w -> !locked));
        editing.child(button(() -> "OC+", 270, 222, 76, () -> tune(0, 1)).setEnabledIf(w -> !locked));
        panel.child(
            new TextWidget<>(IKey.dynamic(FactoryText.NODE_EUT::text)).pos(10, 253)
                .size(95, 20));
        editing.child(
            new com.cleanroommc.modularui.widgets.textfield.TextFieldWidget().value(nodeEUtText)
                .setMaxLength(19)
                .pos(110, 251)
                .size(115, 18)
                .setEnabledIf(w -> !locked));
        editing.child(
            button(
                FactoryText.SET::text,
                231,
                251,
                52,
                () -> actions.send(10, selected, 0, 0, nodeEUtText.getStringValue())).setEnabledIf(w -> !locked));
        editing.child(
            button(FactoryText.RECIPE_DEFAULT::text, 289, 251, 57, () -> actions.send(10, selected, 0, 0, "-1"))
                .setEnabledIf(w -> !locked));
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> locked ? FactoryText.LOCKED.text() : nodeLine(selected))).pos(10, 280)
                .size(340, 16));
        return panel;
    }

    /** Display-only ingredient metadata; never contains mutable inventory references. */
    private static final class DetailIngredient {

        final ItemStack item;
        final FactoryText label;

        DetailIngredient(ItemStack item, FactoryText label) {
            this.item = item;
            this.label = label;
        }
    }

    private String detailRecipeId = "";
    private final List<DetailIngredient> detailCache = new ArrayList<>();

    /** Cache the selected recipe so rendering many rows does not repeatedly allocate all item stacks. */
    private List<DetailIngredient> detailIngredients() {
        FactoryGraph.Node node = visibleGraph.find(selected);
        String id = node == null ? "" : node.recipe;
        if (id.equals(detailRecipeId)) return detailCache;
        detailRecipeId = id;
        detailCache.clear();
        for (int group = 0; group < 3; group++) {
            FactoryText label = group == 0 ? FactoryText.INPUTS
                : group == 1 ? FactoryText.OUTPUTS : FactoryText.CATALYSTS;
            for (int i = 0; i < 32; i++) {
                ItemStack item = ingredient(group, i);
                if (item == null) break;
                detailCache.add(new DetailIngredient(item, label));
            }
        }
        return detailCache;
    }

    private ItemStack detailItem(int index) {
        List<DetailIngredient> entries = detailIngredients();
        return index < entries.size() ? entries.get(index).item : null;
    }

    private final java.util.Map<String, ItemStack> controllerIcons = new java.util.HashMap<>();

    /** Find one compatible controller per recipe map, matching the same predicate as actual machine deposits. */
    private ItemStack controllerIcon(FactoryRecipeCatalog.Entry entry) {
        if (!controllerIcons.containsKey(entry.map.unlocalizedName)) {
            ItemStack found = null;
            for (gregtech.api.interfaces.metatileentity.IMetaTileEntity meta : gregtech.api.GregTechAPI.METATILEENTITIES) {
                if (!(meta instanceof gregtech.api.metatileentity.implementations.MTEMultiBlockBase)) continue;
                ItemStack stack = meta.getStackForm(1);
                if (IntegratedProductionFactory.supportsHost(entry, stack)) {
                    found = stack;
                    break;
                }
            }
            controllerIcons.put(entry.map.unlocalizedName, found);
        }
        return controllerIcons.get(entry.map.unlocalizedName);
    }

    private FactoryRecipeCatalog.Entry selectedRecipe() {
        FactoryGraph.Node node = visibleGraph.find(selected);
        return node == null ? null : FactoryRecipeCatalog.get(node.recipe);
    }

    /** Zero-sized recipe inputs are displayed separately as non-consumable catalysts. */
    private ItemStack ingredient(int group, int slot) {
        FactoryRecipeCatalog.Entry entry = selectedRecipe();
        if (entry == null) return null;
        List<ItemStack> stacks = new ArrayList<>();
        ItemStack[] items = group == 1 ? entry.recipe.mOutputs : entry.recipe.mInputs;
        for (ItemStack item : items) {
            if (item == null || (group == 0 && item.stackSize == 0) || (group == 2 && item.stackSize != 0)) continue;
            ItemStack display = item.copy();
            display.stackSize = Math.max(1, display.stackSize);
            stacks.add(display);
        }
        if (group != 2) for (FluidStack fluid : group == 0 ? entry.recipe.mFluidInputs : entry.recipe.mFluidOutputs) {
            if (fluid != null) stacks.add(GTUtility.getFluidDisplayStack(fluid, true));
        }
        return slot < stacks.size() ? stacks.get(slot) : null;
    }

    /** Background grouping keeps the inherited MUI layout and hit targets unchanged. */
    private void card(ModularPanel panel, int x, int y, int width, int height, int color) {
        panel.child(
            new com.cleanroommc.modularui.widget.Widget<>()
                .background(new com.cleanroommc.modularui.drawable.Rectangle().color(color))
                .pos(x, y)
                .size(width, height));
    }

    private ButtonWidget<?> button(Supplier<String> text, int x, int y, int width, Runnable action) {
        return new ButtonWidget<>().pos(x, y)
            .size(width, 20)
            .background(GTNGGuiTextures.MODERN_BUTTON)
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(IKey.dynamic(text))
            .onMousePressed(mouse -> {
                action.run();
                return true;
            });
    }

    /** Small typed editor commands travel over the bound MUI2 container; no arbitrary recipe NBT is accepted. */
    public static final class FactoryActions extends SyncHandler<FactoryActions> {

        private final IntegratedProductionFactory factory;
        private final IntegratedProductionFactoryGui gui;

        public FactoryActions(IntegratedProductionFactory factory, IntegratedProductionFactoryGui gui) {
            this.gui = gui;
            this.factory = factory;
            allowC2S();
        }

        /** Returns -2 for an editable route list and -1 otherwise; never targets a previously closed machine. */
        public int importTarget() {
            if (!isValid() || gui.locked) return -1;
            if ((gui.details != null && gui.details.isPanelOpen()) || (gui.preview != null && gui.preview.isPanelOpen())
                || (gui.transfer != null && gui.transfer.isPanelOpen())
                || (gui.clear != null && gui.clear.isPanelOpen())) return -1;
            return gui.editor != null && gui.editor.isPanelOpen()
                && gui.visibleGraph.nodes.size() < FactoryGraph.MAX_NODES ? -2 : -1;
        }

        public void send(int command, int id, int a, int b, String recipe) {
            syncToServer(command, buf -> {
                buf.writeInt(id);
                buf.writeInt(a);
                buf.writeInt(b);
                buf.writeStringToBuffer(recipe);
            });
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) throws IOException {}

        @Override
        public void readOnServer(int command, PacketBuffer buf) throws IOException {
            int id = buf.readInt();
            int a = buf.readInt();
            int b = buf.readInt();
            String recipe = buf.readStringFromBuffer((command == 17 || command == 5) ? 8192 : 256);
            factory.edit(command, id, a, b, recipe);
        }
    }
}
