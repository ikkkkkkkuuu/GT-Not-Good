package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.math.BigInteger;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;

public class SingularityDataHubGui extends GTNGModernMultiBlockBaseGui<SingularityDataHub> {

    private static final String ITEM_TYPES_SYNC_KEY = "singularityDataHubItemTypes";
    private static final String FLUID_TYPES_SYNC_KEY = "singularityDataHubFluidTypes";
    private static final String STORAGE_BYTES_SYNC_KEY = "singularityDataHubStorageBytes";
    private static final String WIRELESS_MODE_SYNC_KEY = "singularityDataHubWirelessMode";
    private static final String AUTO_VOID_SYNC_KEY = "singularityDataHubAutoVoid";
    private static final String PORT_HATCH_SYNC_KEY = "singularityDataHubPortHatch";

    private static final int TEXT = 0xFFD6D0E0;
    private static final int MUTED = 0xFFAAA4B2;
    private static final int VALUE = 0xFF8377FF;
    private static final int ITEM = 0xFFF89737;
    private static final int FLUID = 0xFF3FD6FF;
    private static final int GOOD = 0xFF6CFFA0;
    private static final int GAUGE_FRAME = 0xFFB0B0C8;
    private static final int GAUGE_INSET = 0xFF2B2834;
    private static final int GAUGE_SHADOW = 0xFF17141E;
    private static final int GAUGE_FILL = 0xFF00B800;
    private static final int GAUGE_FILL_HIGHLIGHT = 0xFF42E842;
    private static final int PANEL_ALT = 0xFF201E27;

    public SingularityDataHubGui(SingularityDataHub multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(ITEM_TYPES_SYNC_KEY, new LongSyncValue(multiblock::itemsCount));
        syncManager.syncValue(FLUID_TYPES_SYNC_KEY, new LongSyncValue(multiblock::fluidsCount));
        syncManager.syncValue(STORAGE_BYTES_SYNC_KEY, new LongSyncValue(multiblock::getUsedStorageBytes));
        syncManager.syncValue(WIRELESS_MODE_SYNC_KEY, new BooleanSyncValue(() -> multiblock.wirelessMode));
        syncManager.syncValue(AUTO_VOID_SYNC_KEY, new BooleanSyncValue(() -> multiblock.doVoidExcess));
        syncManager.syncValue(PORT_HATCH_SYNC_KEY, new BooleanSyncValue(() -> multiblock.portHatch != null));
    }

    @Override
    protected int getBasePanelWidth() {
        return 344;
    }

    @Override
    protected int getBasePanelHeight() {
        return 232;
    }

    @Override
    protected ModularPanel getBasePanel(com.cleanroommc.modularui.factory.PosGuiData guiData,
        PanelSyncManager syncManager, UISettings uiSettings) {
        return super.getBasePanel(guiData, syncManager, uiSettings).background(GTNGGuiTextures.MODERN_VAULT_BACKGROUND);
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager syncManager) {
        LongSyncValue itemTypes = syncManager.findSyncHandler(ITEM_TYPES_SYNC_KEY, LongSyncValue.class);
        LongSyncValue fluidTypes = syncManager.findSyncHandler(FLUID_TYPES_SYNC_KEY, LongSyncValue.class);
        LongSyncValue storageBytes = syncManager.findSyncHandler(STORAGE_BYTES_SYNC_KEY, LongSyncValue.class);
        BooleanSyncValue wirelessMode = syncManager.findSyncHandler(WIRELESS_MODE_SYNC_KEY, BooleanSyncValue.class);
        BooleanSyncValue autoVoid = syncManager.findSyncHandler(AUTO_VOID_SYNC_KEY, BooleanSyncValue.class);
        BooleanSyncValue portHatch = syncManager.findSyncHandler(PORT_HATCH_SYNC_KEY, BooleanSyncValue.class);

        ParentWidget<?> content = new ParentWidget<>().size(336, 224);
        // #tr Gui_SingularityDataHub_Online
        // # Online
        // # zh_CN 在线
        // #tr Gui_SingularityDataHub_Unformed
        // # Unformed
        // # zh_CN 未成型
        content.child(section(0, 4, 176, 112));
        // #tr Gui_SingularityDataHub_ItemStorage
        // # Item Storage
        // # zh_CN 物品存储
        // #tr Gui_SingularityDataHub_CommonStorage
        // # Common Storage
        // # zh_CN 通用存储
        content.child(text(IKey.lang("Gui_SingularityDataHub_CommonStorage"), 8, 12, 148, ITEM, Alignment.CenterLeft));
        // #tr Gui_SingularityDataHub_Amount
        // # Amount
        // # zh_CN 存储量
        // #tr Gui_SingularityDataHub_Types
        // # Types
        // # zh_CN 种类
        // #tr Gui_SingularityDataHub_Bytes
        // # Bytes
        // # zh_CN 字节
        content.child(bytesRow(storageBytes::getLongValue, 27));
        // #tr Gui_SingularityDataHub_FluidStorage
        // # Fluid Storage
        // # zh_CN 流体存储
        // #tr Gui_SingularityDataHub_NeedVaultPortHatch
        // # Vault Port Hatch is required
        // # zh_CN 需要仓库端口仓
        content.child(
            dynamic(
                () -> portHatch.getBoolValue() ? ""
                    : StatCollector.translateToLocal("Gui_SingularityDataHub_NeedVaultPortHatch"),
                38,
                43,
                126,
                0xFFFF5555,
                Alignment.CenterLeft));
        content.child(section(182, 4, 154, 214));
        // #tr Gui_SingularityDataHub_SystemLoad
        // # System Load
        // # zh_CN 系统负载
        content.child(text(IKey.lang("Gui_SingularityDataHub_SystemLoad"), 190, 12, 138, TEXT, Alignment.Center));
        content.child(darkInset(190, 28, 138, 172));
        DoubleSupplier load = () -> storageLoad(storageBytes.getLongValue());
        content.child(
            new StorageLoadWidget(load).pos(198, 40)
                .size(32, 126));
        content.child(dynamic(() -> formatPercent(load.getAsDouble()), 198, 168, 32, GOOD, Alignment.Center));
        // #tr Gui_SingularityDataHub_CurrentLoad
        // # Current Load
        // # zh_CN 当前负载
        content.child(
            metric("Gui_SingularityDataHub_CurrentLoad", () -> formatPercent(load.getAsDouble()), 238, 44, TEXT));
        // #tr Gui_SingularityDataHub_ItemTypes
        // # Item Types
        // # zh_CN 物品种类
        content.child(
            metric("Gui_SingularityDataHub_ItemTypes", () -> compactAmount(itemTypes.getLongValue()), 238, 62, ITEM));
        // #tr Gui_SingularityDataHub_FluidTypes
        // # Fluid Types
        // # zh_CN 流体种类
        content.child(
            metric(
                "Gui_SingularityDataHub_FluidTypes",
                () -> compactAmount(fluidTypes.getLongValue()),
                238,
                78,
                FLUID));
        content.child(
            // #tr Gui_SingularityDataHub_Mode
            // # Mode
            // # zh_CN 模式
            // #tr Gui_SingularityDataHub_Wireless
            // # Wireless
            // # zh_CN 无线
            // #tr Gui_SingularityDataHub_Wired
            // # Wired
            // # zh_CN 有线
            metric(
                "Gui_SingularityDataHub_Mode",
                () -> StatCollector.translateToLocal(
                    wirelessMode.getBoolValue() ? "Gui_SingularityDataHub_Wireless" : "Gui_SingularityDataHub_Wired"),
                238,
                96,
                wirelessMode.getBoolValue() ? VALUE : TEXT));
        content.child(
            // #tr Gui_SingularityDataHub_AutoVoid
            // # Auto-voiding
            // # zh_CN 自动销毁溢出
            // #tr Gui_SingularityDataHub_Enabled
            // # Enabled
            // # zh_CN 已启用
            // #tr Gui_SingularityDataHub_Disabled
            // # Disabled
            // # zh_CN 已禁用
            metric(
                "Gui_SingularityDataHub_AutoVoid",
                () -> StatCollector.translateToLocal(
                    autoVoid.getBoolValue() ? "Gui_SingularityDataHub_Enabled" : "Gui_SingularityDataHub_Disabled"),
                238,
                114,
                autoVoid.getBoolValue() ? GOOD : MUTED));
        // #tr Gui_SingularityDataHub_RunningCost
        // # Running Cost
        // # zh_CN 运行成本
        content.child(metric("Gui_SingularityDataHub_RunningCost", () -> "0 EU/t", 238, 132, TEXT));
        content.child(
            SlotGroupWidget.playerInventory((index, slot) -> slot.background(GTNGGuiTextures.MODERN_VAULT_ITEM_SLOT))
                .pos(8, 123));
        content.child(createFixedButtonColumn(syncManager).pos(309, 160));
        return Flow.column()
            .padding(4)
            .child(content);
    }

    private IWidget section(int x, int y, int width, int height) {
        return new ParentWidget<>().pos(x, y)
            .size(width, height)
            .background(GTNGGuiTextures.MODERN_VAULT_PANEL_BORDER);
    }

    private IWidget darkInset(int x, int y, int width, int height) {
        ParentWidget<?> inset = new ParentWidget<>().pos(x, y)
            .size(width, height)
            .background(new Rectangle().color(0xFFC9C3D6));
        inset.child(
            new ParentWidget<>().pos(1, 1)
                .size(width - 2, height - 2)
                .background(new Rectangle().color(0xFF17141E)));
        inset.child(
            new ParentWidget<>().pos(2, 2)
                .size(width - 4, height - 4)
                .background(new Rectangle().color(0xFF201E27)));
        return inset;
    }

    private Flow createFixedButtonColumn(PanelSyncManager syncManager) {
        return Flow.column()
            .size(18, 40)
            .reverseLayout(true)
            .child(createPowerSwitchButton())
            .child(createStructureUpdateButton(syncManager));
    }

    private IWidget bytesRow(LongSupplier usedBytes, int y) {
        ParentWidget<?> row = new ParentWidget<>().pos(8, y)
            .size(160, 12);
        row.child(text(IKey.lang("Gui_SingularityDataHub_Bytes"), 0, 0, 28, MUTED, Alignment.CenterLeft));
        row.child(
            new HostProgressWidget(() -> storageLoad(usedBytes.getAsLong()), FLUID).pos(30, 1)
                .size(36, 9));
        row.child(dynamic(() -> compactAmount(usedBytes.getAsLong()), 72, 0, 24, GOOD, Alignment.CenterRight));
        row.child(dynamic(() -> "/", 98, 0, 6, MUTED, Alignment.Center));
        row.child(
            dynamic(
                () -> compactAmount(SingularityDataHub.MAX_STORAGE_BYTES),
                106,
                0,
                52,
                FLUID,
                Alignment.CenterLeft));
        return row;
    }

    private IWidget metric(String key, java.util.function.Supplier<String> value, int x, int y, int color) {
        return dynamic(
            () -> StatCollector.translateToLocal(key) + ": " + value.get(),
            x,
            y,
            96,
            color,
            Alignment.CenterLeft);
    }

    private IWidget text(IKey key, int x, int y, int width, int color, Alignment alignment) {
        return key.asWidget()
            .pos(x, y)
            .size(width, 12)
            .color(color)
            .textAlign(alignment);
    }

    private IWidget dynamic(java.util.function.Supplier<String> value, int x, int y, int width, int color,
        Alignment alignment) {
        return text(IKey.dynamic(value), x, y, width, color, alignment);
    }

    private static double storageLoad(long usedBytes) {
        return Math.min(1D, usedBytes / (double) SingularityDataHub.MAX_STORAGE_BYTES);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100D);
    }

    private static String compactAmount(long amount) {
        return compactAmount(BigInteger.valueOf(amount));
    }

    private static String compactAmount(BigInteger amount) {
        BigInteger safeAmount = amount == null ? BigInteger.ZERO : amount.abs();

        if (safeAmount.compareTo(BigInteger.valueOf(1_000L)) < 0) {
            return safeAmount.toString();
        }

        // ""、K、M、G、T、P、E
        String[] units = { "", "K", "M", "G", "T", "P", "E" };

        double scaled = safeAmount.doubleValue();
        int unitIndex = 0;

        while (scaled >= 1_000D && unitIndex < units.length - 1) {
            scaled /= 1_000D;
            unitIndex++;
        }

        // E（Exa）使用两位小数，避免原来的 6E03
        if (unitIndex == units.length - 1) {
            return String.format(Locale.ROOT, "%.2f%s", scaled, units[unitIndex]);
        }

        return String.format(Locale.ROOT, scaled < 10D ? "%.1f%s" : "%.0f%s", scaled, units[unitIndex]);
    }

    private static final class StorageLoadWidget extends Widget<StorageLoadWidget> {

        private final DoubleSupplier progress;

        private StorageLoadWidget(DoubleSupplier progress) {
            this.progress = progress;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> entry) {
            WidgetTheme theme = getActiveWidgetTheme(entry, isHovering());
            int width = getArea().width;
            int height = getArea().height;
            new Rectangle().color(GAUGE_SHADOW)
                .draw(context, 0, 0, width, height, theme);
            new Rectangle().color(GAUGE_FRAME)
                .draw(context, 1, 1, width - 2, height - 2, theme);
            new Rectangle().color(GAUGE_INSET)
                .draw(context, 3, 3, width - 6, height - 6, theme);
            int filled = Math
                .max(2, (int) Math.round((height - 8) * Math.max(0D, Math.min(1D, progress.getAsDouble()))));
            new Rectangle().color(GAUGE_FILL)
                .draw(context, 4, height - filled - 4, width - 8, filled, theme);
            new Rectangle().color(GAUGE_FILL_HIGHLIGHT)
                .draw(context, 4, height - filled - 4, width - 8, 1, theme);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
    }

    private static final class HostProgressWidget extends Widget<HostProgressWidget> {

        private final DoubleSupplier progress;
        private final int color;

        private HostProgressWidget(DoubleSupplier progress, int color) {
            this.progress = progress;
            this.color = color;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> entry) {
            WidgetTheme theme = getActiveWidgetTheme(entry, isHovering());
            int width = getArea().width;
            int height = getArea().height;
            GTNGGuiTextures.MODERN_VAULT_PANEL_BORDER.draw(context, 0, 0, width, height, theme);
            new Rectangle().color(PANEL_ALT)
                .draw(context, 2, 2, width - 4, height - 4, theme);
            int filled = (int) Math.round(Math.max(0D, Math.min(1D, progress.getAsDouble())) * (width - 6));
            if (filled > 0) new Rectangle().color(color)
                .draw(context, 3, 3, filled, height - 6, theme);
        }
    }
}
