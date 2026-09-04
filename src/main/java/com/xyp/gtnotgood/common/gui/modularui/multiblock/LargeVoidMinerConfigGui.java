package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.api.gui.OreEntryInfo;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.util.GTUtility;

/**
 * Crust Matter Aggregator style configuration terminal for the electric Large Void Miner.
 * <p>
 * The screen keeps the expected ore-mode, fortune, dimension-slot, search, filtering, and directional controls while
 * replacing all steam readouts with EU/t values from the electric controller.
 */
public class LargeVoidMinerConfigGui implements IGuiHolder<PosGuiData> {

    private static final int PANEL_WIDTH = 475;
    private static final int PANEL_HEIGHT = 350;
    private static final int LEFT_X = 18;
    private static final int SLOT_TITLE_Y = 45;
    private static final int SLOT_COL_Y = 57;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_GRID_COLS = 5;
    private static final int SLOT_GAP = 1;
    private static final int REFRESH_X = LEFT_X + 32;
    private static final int REFRESH_Y = SLOT_TITLE_Y - 1;
    private static final int DIM_INCREASE_X = REFRESH_X + 30;
    private static final int DIM_TEXT_Y = 145;
    private static final int DIRECTIONAL_Y = 163;
    private static final int HINT_Y = 185;
    private static final int HINT_W = 157;
    private static final int HINT_H = 83;
    private static final int BROWSER_X = 180;
    private static final int BROWSER_Y = 58;
    private static final int BROWSER_W = 290;
    private static final int BROWSER_H = 208;
    private static final int WEIGHT_INCREASE_X = BROWSER_X + 64;
    private static final int SEARCH_Y = 62;
    private static final int HEADER_Y = 82;
    private static final int LIST_X = BROWSER_X + 4;
    private static final int LIST_Y = 100;
    private static final int LIST_W = BROWSER_W - 8;
    private static final int LIST_H = 156;
    private static final int COL_ICON = 16;
    private static final int COL_NAME = 100;
    private static final int COL_WEIGHT = 44;
    private static final int COL_DIM = 56;
    private static final int COL_ACTION = 44;
    private static final int SEARCH_FIELD_W = 124;
    private static final int BUTTON_W = 44;
    private static final int SEARCH_BTN_X = BROWSER_X + BROWSER_W - 4 - BUTTON_W * 3 - 8;
    private static final int CATEGORY_BTN_X = BROWSER_X + BROWSER_W - 4 - BUTTON_W * 2 - 4;
    private static final int CLEAR_BTN_X = BROWSER_X + BROWSER_W - 4 - BUTTON_W;
    private static final int PLUGIN_SLOT_COUNT = 25;
    private static final String[] ORE_MODE_NAMES = { "raw", "crude", "crushed" };
    private static final String[] ROMAN_NUMERALS = { "III", "V", "VII", "IX", "XI", "XIII", "XV" };

    private final StringValue searchValue = new StringValue("");
    private String searchText = "";
    private int categoryMode = 0;
    private GenericListSyncHandler<OreEntryInfo> oreListSync;
    private MinerActionSyncHandler actionSync;
    private ListWidget<IWidget, ?> oreListWidget;
    private BooleanSyncValue directionalSync;
    private final LargeVoidMiner miner;

    public LargeVoidMinerConfigGui(LargeVoidMiner miner) {
        this.miner = miner;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        GenericListSyncHandler<OreEntryInfo> oreListSync = new GenericListSyncHandler<>(
            miner::getOreEntries,
            null,
            LargeVoidMinerConfigGui::readOreInfo,
            LargeVoidMinerConfigGui::writeOreInfo,
            LargeVoidMinerConfigGui::oreInfoEqual,
            null);
        IntSyncValue oreModeSync = new IntSyncValue(() -> miner.mOreMode);
        IntSyncValue fortuneSync = new IntSyncValue(() -> miner.mFortuneLevel);
        DoubleSyncValue energyMultSync = new DoubleSyncValue(miner::getEnergyMultiplier);
        IntSyncValue energyCostSync = new IntSyncValue(() -> (int) miner.getEnergyCostPerTick());
        BooleanSyncValue directionalSync = new BooleanSyncValue(miner::getDirectionalMode);
        DoubleSyncValue uuMultSync = new DoubleSyncValue(miner::getUUMultiplier);
        DoubleSyncValue weightIncreaseSync = new DoubleSyncValue(miner::getWeightIncreasePercent);
        DoubleSyncValue dimIncreaseSync = new DoubleSyncValue(miner::getDimensionIncreasePercent);
        this.directionalSync = directionalSync;
        this.actionSync = new MinerActionSyncHandler(miner);
        this.oreListSync = oreListSync;
        oreListSync.setChangeListener(this::refreshOreList);

        syncManager.syncValue("gtng.vm.cfg.oreList", oreListSync);
        syncManager.syncValue("gtng.vm.cfg.oreMode", oreModeSync);
        syncManager.syncValue("gtng.vm.cfg.fortune", fortuneSync);
        syncManager.syncValue("gtng.vm.cfg.energyMult", energyMultSync);
        syncManager.syncValue("gtng.vm.cfg.energyCost", energyCostSync);
        syncManager.syncValue("gtng.vm.cfg.directionalMode", directionalSync);
        syncManager.syncValue("gtng.vm.cfg.uuMult", uuMultSync);
        syncManager.syncValue("gtng.vm.cfg.weightIncrease", weightIncreaseSync);
        syncManager.syncValue("gtng.vm.cfg.dimIncrease", dimIncreaseSync);
        syncManager.syncValue("gtng.vm.cfg.action", this.actionSync);

        ListWidget<IWidget, ?> oreListWidget = new ListWidget<>();
        this.oreListWidget = oreListWidget;
        oreListWidget.pos(LIST_X, LIST_Y)
            .size(LIST_W, LIST_H);

        ModularPanel panel = ModularPanel.defaultPanel("large_void_miner_config", PANEL_WIDTH, PANEL_HEIGHT)
            .background(GTGuiTextures.BACKGROUND_STANDARD)
            .child(ButtonWidget.panelCloseButton())
            // #tr gui.gtnotgood.largeVoidMiner.config.title
            // # Large Void Miner
            // # zh_CN 大型虚空矿机
            .child(
                IKey.lang("gui.gtnotgood.largeVoidMiner.config.title")
                    .asWidget()
                    .pos(8, 6))
            .child(
                buildConfigRow(oreModeSync, fortuneSync, energyMultSync, energyCostSync, directionalSync, uuMultSync))
            // #tr gui.gtnotgood.largeVoidMiner.config.slots_title
            // # Dimensions
            // # zh_CN 维度槽
            .child(
                IKey.lang("gui.gtnotgood.largeVoidMiner.config.slots_title")
                    .asWidget()
                    .pos(LEFT_X, SLOT_TITLE_Y))
            // #tr gui.gtnotgood.largeVoidMiner.config.browser_title
            // # Ore Browser
            // # zh_CN 矿石浏览器
            .child(
                IKey.lang("gui.gtnotgood.largeVoidMiner.config.browser_title")
                    .asWidget()
                    .pos(BROWSER_X, SLOT_TITLE_Y))
            .child(
                IKey.dynamic(() -> formatIncreaseValue(weightIncreaseSync.getDoubleValue()))
                    .asWidget()
                    .pos(WEIGHT_INCREASE_X, SLOT_TITLE_Y))
            .child(buildBrowserBackground())
            .child(buildSearchField())
            .child(buildSearchButton())
            .child(buildCategoryButton())
            .child(buildClearConfigButton(this.actionSync))
            .child(buildTableHeader())
            .child(oreListWidget)
            .child(SlotGroupWidget.playerInventory(true));

        IItemHandler pluginHandler = new InvWrapper(miner.getPluginSlotInventory());
        for (int i = 0; i < PLUGIN_SLOT_COUNT; i++) {
            panel.child(buildPluginSlot(new ModularSlot(pluginHandler, i), i == 0, i));
        }
        panel.child(buildRefreshButton(this.actionSync));
        panel.child(
            IKey.dynamic(() -> formatIncreaseValue(dimIncreaseSync.getDoubleValue()))
                .asWidget()
                .pos(DIM_INCREASE_X, SLOT_TITLE_Y));
        panel.child(
            IKey.dynamic(
                () -> EnumChatFormatting.BLACK + StatCollector.translateToLocal(
                    // #tr gui.gtnotgood.largeVoidMiner.config.dim_text.directional
                    // # Directional mining has a base energy surcharge and each extra dimension adds more.
                    // # zh_CN 定向采矿有基础能耗增幅，每个额外维度会继续增加。
                    // #tr gui.gtnotgood.largeVoidMiner.config.dim_text.filtered
                    // # Each extra dimension increases energy cost.
                    // # zh_CN 每个额外维度会增加能耗。
                    directionalSync.getValue() ? "gui.gtnotgood.largeVoidMiner.config.dim_text.directional"
                        : "gui.gtnotgood.largeVoidMiner.config.dim_text.filtered"))
                .asWidget()
                .pos(LEFT_X, DIM_TEXT_Y));
        panel.child(buildDirectionalButton(directionalSync, this.actionSync));
        panel.child(
            new TextWidget<>(
                IKey.dynamic(() -> buildModeHintText(directionalSync.getValue(), uuMultSync.getDoubleValue())))
                    .pos(LEFT_X, HINT_Y)
                    .size(HINT_W, HINT_H));

        refreshOreList();
        return panel;
    }

    private IWidget buildConfigRow(IntSyncValue oreModeSync, IntSyncValue fortuneSync, DoubleSyncValue energyMultSync,
        IntSyncValue energyCostSync, BooleanSyncValue directionalSync, DoubleSyncValue uuMultSync) {
        ButtonWidget<?> oreModeButton = new ButtonWidget<>().size(104, 18)
            .overlay(IKey.dynamic(() -> formatOreModeLabel(oreModeSync.getIntValue())))
            .onMousePressed(mouseButton -> {
                actionSync.sendCycleOreMode();
                return true;
            })
            // #tr gui.gtnotgood.largeVoidMiner.config.ore_mode.tip
            // # Cycle output form
            // # zh_CN 切换输出形态
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.ore_mode.tip")));

        ButtonWidget<?> fortuneButton = new ButtonWidget<>().size(104, 18)
            .overlay(IKey.dynamic(() -> formatFortuneLabel(fortuneSync.getIntValue())))
            .onMousePressed(mouseButton -> {
                actionSync.sendCycleFortune();
                return true;
            })
            // #tr gui.gtnotgood.largeVoidMiner.config.fortune.tip
            // # Cycle fortune level; raw ore mode ignores fortune
            // # zh_CN 切换时运等级，原矿模式不使用时运
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.fortune.tip")))
            .onUpdateListener(button -> button.setEnabled(oreModeSync.getIntValue() != 0), true);

        IWidget energyText = IKey
            .dynamic(() -> formatEnergyCostLine(energyCostSync.getIntValue(), energyMultSync.getDoubleValue()))
            .asWidget()
            .scale(0.9f)
            .tooltipBuilder(t -> {
                // #tr gui.gtnotgood.largeVoidMiner.config.energy_cost.tip
                // # Current EU/t includes mode, dimension, filter, and directional multipliers.
                // # zh_CN 当前 EU/t 包含模式、维度、过滤和定向倍率。
                t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.energy_cost.tip"));
                t.addLine(IKey.dynamic(() -> {
                    if (directionalSync.getValue()) {
                        return EnumChatFormatting.GRAY + "UU x " + String.format("%.2f", uuMultSync.getDoubleValue());
                    }
                    return EnumChatFormatting.GRAY + "x " + String.format("%.2f", energyMultSync.getDoubleValue());
                }));
            });

        return Flow.row()
            .pos(8, 22)
            .height(18)
            .childPadding(4)
            .child(oreModeButton)
            .child(fortuneButton)
            .child(energyText);
    }

    private static String formatOreModeLabel(int mode) {
        int m = Math.min(Math.max(mode, 0), 2);
        // #tr gui.gtnotgood.largeVoidMiner.config.ore_mode.raw
        // # Raw Ore
        // # zh_CN 原矿
        // #tr gui.gtnotgood.largeVoidMiner.config.ore_mode.crude
        // # Crude Drop
        // # zh_CN 粗矿
        // #tr gui.gtnotgood.largeVoidMiner.config.ore_mode.crushed
        // # Crushed Ore
        // # zh_CN 粉碎矿
        String name = StatCollector
            .translateToLocal("gui.gtnotgood.largeVoidMiner.config.ore_mode." + ORE_MODE_NAMES[m]);
        int bonus = (int) Math.round(LargeVoidMiner.ORE_MODE_ENERGY_BONUS[m] * 100.0d);
        return name + " +" + bonus + "%";
    }

    private static String formatFortuneLabel(int level) {
        int idx = Math.min(Math.max((level - 3) / 2, 0), 6);
        int bonus = (int) Math.round(LargeVoidMiner.FORTUNE_ENERGY_BONUS[idx] * 100.0d);
        // #tr gui.gtnotgood.largeVoidMiner.config.fortune_level
        // # Fortune
        // # zh_CN 时运
        return StatCollector.translateToLocal(
            "gui.gtnotgood.largeVoidMiner.config.fortune_level") + " " + ROMAN_NUMERALS[idx] + " +" + bonus + "%";
    }

    private static String formatEnergyCostLine(int energyCost, double energyMult) {
        // #tr gui.gtnotgood.largeVoidMiner.config.energy_cost
        // # Energy
        // # zh_CN 能耗
        return EnumChatFormatting.BOLD
            + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.config.energy_cost")
            + " "
            + NumberFormatUtil.formatNumber(energyCost)
            + " EU/t "
            + EnumChatFormatting.GOLD
            + "x"
            + String.format("%.2f", energyMult);
    }

    private static String formatIncreaseValue(double percent) {
        return EnumChatFormatting.BLACK.toString() + EnumChatFormatting.BOLD
            + "+"
            + String.format("%.0f", percent)
            + "%";
    }

    private static String buildModeHintText(boolean directional, double uuMult) {
        if (directional) {
            // #tr gui.gtnotgood.largeVoidMiner.config.mode_hint.directional
            // # Mode: directional mining\nOnly targeted ores are extracted.\nUU-Matter is consumed while running.
            // # zh_CN 模式：定向采矿\n只抽取标记为定向的矿石。\n运行时消耗 UU 物质。
            return EnumChatFormatting.BLUE
                + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.config.mode_hint.directional")
                + "\n"
                + EnumChatFormatting.LIGHT_PURPLE
                // #tr gui.gtnotgood.largeVoidMiner.config.uu_cost
                // # UU-Matter:
                // # zh_CN UU物质：
                + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.config.uu_cost")
                + " "
                + NumberFormatUtil.formatNumber(Math.round(uuMult))
                + " L/s";
        }
        // #tr gui.gtnotgood.largeVoidMiner.config.mode_hint.filtered
        // # Mode: filtering\nFiltered ores are removed from the weighted pool.\nMore filters increase energy cost.
        // # zh_CN 模式：过滤\n过滤矿石会从权重池中移除。\n过滤越多，能耗越高。
        return EnumChatFormatting.BLUE
            + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.config.mode_hint.filtered");
    }

    private IWidget buildRefreshButton(MinerActionSyncHandler actionSync) {
        return new ButtonWidget<>().pos(REFRESH_X, REFRESH_Y)
            .size(26, 12)
            // #tr gui.gtnotgood.largeVoidMiner.config.refresh
            // # Refresh
            // # zh_CN 刷新
            .overlay(IKey.lang("gui.gtnotgood.largeVoidMiner.config.refresh"))
            .onMousePressed(mouseButton -> {
                actionSync.sendRefreshPool();
                return true;
            })
            // #tr gui.gtnotgood.largeVoidMiner.config.refresh.tip
            // # Rebuild dimension ore pool
            // # zh_CN 重建维度矿池
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.refresh.tip")));
    }

    private IWidget buildDirectionalButton(BooleanSyncValue directionalSync, MinerActionSyncHandler actionSync) {
        return new ButtonWidget<>().pos(LEFT_X, DIRECTIONAL_Y)
            .size(90, 18)
            .overlay(
                IKey.dynamic(
                    () -> StatCollector.translateToLocal(
                        // #tr gui.gtnotgood.largeVoidMiner.config.directional.on
                        // # Directional
                        // # zh_CN 定向模式
                        // #tr gui.gtnotgood.largeVoidMiner.config.directional.off
                        // # Filter
                        // # zh_CN 过滤模式
                        directionalSync.getValue() ? "gui.gtnotgood.largeVoidMiner.config.directional.on"
                            : "gui.gtnotgood.largeVoidMiner.config.directional.off")))
            .onMousePressed(mouseButton -> {
                actionSync.sendToggleDirectional();
                return true;
            })
            // #tr gui.gtnotgood.largeVoidMiner.config.directional.tip
            // # Toggle directional mining
            // # zh_CN 切换定向采矿
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.directional.tip")));
    }

    private ItemSlot buildPluginSlot(ModularSlot slot, boolean isControllerSlot, int index) {
        slot.filter(LargeVoidMiner::isDimensionDisplayItem)
            .singletonSlotGroup();
        ItemSlot itemSlot = new ItemSlot().slot(slot)
            .pos(
                LEFT_X + (index % SLOT_GRID_COLS) * (SLOT_SIZE + SLOT_GAP),
                SLOT_COL_Y + (index / SLOT_GRID_COLS) * (SLOT_SIZE + SLOT_GAP))
            .size(SLOT_SIZE);
        if (isControllerSlot) {
            // #tr gui.gtnotgood.largeVoidMiner.config.slot1_hint
            // # Shared controller slot for a dimension display item
            // # zh_CN 可放维度显示物品的控制器共享槽
            itemSlot.tooltipDynamic(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.slot1_hint")));
        }
        return itemSlot;
    }

    private IWidget buildBrowserBackground() {
        return new com.cleanroommc.modularui.widget.ParentWidget<>().pos(BROWSER_X, BROWSER_Y)
            .size(BROWSER_W, BROWSER_H)
            .background(GuiTextures.DISPLAY);
    }

    private IWidget buildSearchField() {
        return new TextFieldWidget().pos(BROWSER_X + 4, SEARCH_Y)
            .size(SEARCH_FIELD_W, 16)
            .setMaxLength(32)
            .value(searchValue)
            // #tr gui.gtnotgood.largeVoidMiner.config.search_hint
            // # Search ore names
            // # zh_CN 搜索矿石名称
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.search_hint")));
    }

    private IWidget buildSearchButton() {
        return new ButtonWidget<>().pos(SEARCH_BTN_X, SEARCH_Y)
            .size(BUTTON_W, 16)
            // #tr gui.gtnotgood.largeVoidMiner.config.search
            // # Search
            // # zh_CN 搜索
            .overlay(IKey.lang("gui.gtnotgood.largeVoidMiner.config.search"))
            .onMousePressed(mouseButton -> {
                searchText = searchValue.getStringValue()
                    .trim();
                refreshOreList();
                return true;
            });
    }

    private IWidget buildCategoryButton() {
        return new ButtonWidget<>().pos(CATEGORY_BTN_X, SEARCH_Y)
            .size(BUTTON_W, 16)
            .overlay(IKey.dynamic(() -> StatCollector.translateToLocal(categoryLangKey(categoryMode))))
            .onMousePressed(mouseButton -> {
                categoryMode = (categoryMode + 1) % 5;
                refreshOreList();
                return true;
            })
            // #tr gui.gtnotgood.largeVoidMiner.config.category.tip
            // # Cycle list filter and sort mode
            // # zh_CN 切换列表过滤和排序
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.category.tip")));
    }

    private IWidget buildClearConfigButton(MinerActionSyncHandler actionSync) {
        return new ButtonWidget<>().pos(CLEAR_BTN_X, SEARCH_Y)
            .size(BUTTON_W, 16)
            // #tr gui.gtnotgood.largeVoidMiner.config.clear_config
            // # Clear
            // # zh_CN 清空
            .overlay(IKey.lang("gui.gtnotgood.largeVoidMiner.config.clear_config"))
            .onMousePressed(mouseButton -> {
                actionSync.sendClearConfig();
                return true;
            })
            // #tr gui.gtnotgood.largeVoidMiner.config.clear_config.tip
            // # Clear current mode selections
            // # zh_CN 清空当前模式选择
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.largeVoidMiner.config.clear_config.tip")));
    }

    private IWidget buildTableHeader() {
        Flow row = Flow.row()
            .pos(BROWSER_X + 4, HEADER_Y)
            .height(14)
            .childPadding(4);
        row.child(
            IKey.str("")
                .asWidget()
                .width(COL_ICON));
        // #tr gui.gtnotgood.largeVoidMiner.config.col.name
        // # Ore
        // # zh_CN 矿石
        row.child(headerLabel("gui.gtnotgood.largeVoidMiner.config.col.name", COL_NAME));
        // #tr gui.gtnotgood.largeVoidMiner.config.col.weight
        // # Weight
        // # zh_CN 权重
        row.child(headerLabel("gui.gtnotgood.largeVoidMiner.config.col.weight", COL_WEIGHT));
        // #tr gui.gtnotgood.largeVoidMiner.config.col.dim
        // # Dim
        // # zh_CN 维度
        row.child(headerLabel("gui.gtnotgood.largeVoidMiner.config.col.dim", COL_DIM));
        // #tr gui.gtnotgood.largeVoidMiner.config.col.action
        // # Action
        // # zh_CN 操作
        row.child(headerLabel("gui.gtnotgood.largeVoidMiner.config.col.action", COL_ACTION));
        return row;
    }

    private static IWidget headerLabel(String key, int width) {
        return IKey
            .str(EnumChatFormatting.GOLD.toString() + EnumChatFormatting.BOLD + StatCollector.translateToLocal(key))
            .asWidget()
            .width(width)
            .scale(0.8f);
    }

    private void refreshOreList() {
        if (oreListWidget == null || oreListSync == null) return;
        List<OreEntryInfo> all = oreListSync.getValue();
        List<OreEntryInfo> visible = new ArrayList<>();
        if (all != null) {
            for (OreEntryInfo info : all) {
                if (info.ore == null) continue;
                if (categoryMode == 1 && info.filtered) continue;
                if (categoryMode == 2 && !info.filtered) continue;
                if (!searchText.isEmpty() && !info.ore.getDisplayName()
                    .toLowerCase()
                    .contains(searchText.toLowerCase())) continue;
                visible.add(info);
            }
        }
        if (categoryMode == 3) {
            visible.sort(Comparator.comparingDouble(o -> o.weight));
        } else if (categoryMode == 4) {
            visible.sort(
                Comparator.comparingDouble((OreEntryInfo o) -> o.weight)
                    .reversed());
        }
        oreListWidget.removeAll();
        if (visible.isEmpty()) {
            // #tr gui.gtnotgood.largeVoidMiner.config.empty
            // # No ores
            // # zh_CN 没有矿石
            oreListWidget.child(
                IKey.str(
                    EnumChatFormatting.WHITE
                        + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.config.empty"))
                    .asWidget());
            return;
        }
        for (OreEntryInfo info : visible) {
            oreListWidget.child(buildOreRow(info));
        }
    }

    private IWidget buildOreRow(OreEntryInfo info) {
        IWidget icon = new ItemDrawable(info.ore).asWidget()
            .size(16);
        String nameText = EnumChatFormatting.WHITE.toString() + EnumChatFormatting.BOLD + info.ore.getDisplayName();
        String weightText = EnumChatFormatting.GOLD.toString() + EnumChatFormatting.BOLD + formatWeight(info.weight);
        String dimText = EnumChatFormatting.GOLD.toString() + EnumChatFormatting.BOLD + String.join("+", info.dimAbbrs);
        ButtonWidget<?> actionButton = new ButtonWidget<>().size(COL_ACTION, 16)
            .overlay(
                IKey.dynamic(
                    () -> directionalSync.getValue() ? StatCollector.translateToLocal(
                        // #tr gui.gtnotgood.largeVoidMiner.config.directional_ore
                        // # Aim
                        // # zh_CN 定向
                        // #tr gui.gtnotgood.largeVoidMiner.config.directional_ore.off
                        // # Unaim
                        // # zh_CN 取消
                        info.aimed ? "gui.gtnotgood.largeVoidMiner.config.directional_ore.off"
                            : "gui.gtnotgood.largeVoidMiner.config.directional_ore")
                        : StatCollector.translateToLocal(
                            // #tr gui.gtnotgood.largeVoidMiner.config.filter
                            // # Filter
                            // # zh_CN 过滤
                            // #tr gui.gtnotgood.largeVoidMiner.config.unfilter
                            // # Open
                            // # zh_CN 解除
                            info.filtered ? "gui.gtnotgood.largeVoidMiner.config.unfilter"
                                : "gui.gtnotgood.largeVoidMiner.config.filter")))
            .onMousePressed(mouseButton -> {
                if (directionalSync.getValue()) {
                    actionSync.sendToggleDirectionalOre(info);
                } else {
                    actionSync.sendToggleFilter(info);
                }
                return true;
            });
        return Flow.row()
            .widthRel(1f)
            .height(18)
            .childPadding(4)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(icon)
            .child(
                IKey.str(nameText)
                    .asWidget()
                    .width(COL_NAME)
                    .scale(0.9f))
            .child(
                IKey.str(weightText)
                    .asWidget()
                    .width(COL_WEIGHT)
                    .scale(0.9f))
            .child(
                IKey.str(dimText)
                    .asWidget()
                    .width(COL_DIM)
                    .scale(0.9f))
            .child(actionButton);
    }

    private static String formatWeight(float weight) {
        if (!Float.isInfinite(weight) && weight == Math.floor(weight)) {
            return String.format("%.0f", weight);
        }
        return String.format("%.1f", weight);
    }

    private static String categoryLangKey(int mode) {
        switch (mode) {
            case 1:
                // #tr gui.gtnotgood.largeVoidMiner.config.category.unfiltered
                // # Open
                // # zh_CN 未过滤
                return "gui.gtnotgood.largeVoidMiner.config.category.unfiltered";
            case 2:
                // #tr gui.gtnotgood.largeVoidMiner.config.category.filtered
                // # Filtered
                // # zh_CN 已过滤
                return "gui.gtnotgood.largeVoidMiner.config.category.filtered";
            case 3:
                // #tr gui.gtnotgood.largeVoidMiner.config.category.asc
                // # Weight +
                // # zh_CN 权重升
                return "gui.gtnotgood.largeVoidMiner.config.category.asc";
            case 4:
                // #tr gui.gtnotgood.largeVoidMiner.config.category.desc
                // # Weight -
                // # zh_CN 权重降
                return "gui.gtnotgood.largeVoidMiner.config.category.desc";
            default:
                // #tr gui.gtnotgood.largeVoidMiner.config.category.all
                // # All
                // # zh_CN 全部
                return "gui.gtnotgood.largeVoidMiner.config.category.all";
        }
    }

    private static OreEntryInfo readOreInfo(PacketBuffer buf) {
        ItemStack ore = ByteBufUtils.readItemStack(buf);
        float weight = buf.readFloat();
        int dimCount = buf.readInt();
        List<String> dimAbbrs = new ArrayList<>(dimCount);
        for (int i = 0; i < dimCount; i++) {
            dimAbbrs.add(ByteBufUtils.readUTF8String(buf));
        }
        boolean filtered = buf.readBoolean();
        boolean aimed = buf.readBoolean();
        return new OreEntryInfo(ore, weight, dimAbbrs, filtered, aimed);
    }

    private static void writeOreInfo(PacketBuffer buf, OreEntryInfo info) {
        ByteBufUtils.writeItemStack(buf, info.ore);
        buf.writeFloat(info.weight);
        buf.writeInt(info.dimAbbrs.size());
        for (String abbr : info.dimAbbrs) {
            ByteBufUtils.writeUTF8String(buf, abbr);
        }
        buf.writeBoolean(info.filtered);
        buf.writeBoolean(info.aimed);
    }

    private static boolean oreInfoEqual(OreEntryInfo a, OreEntryInfo b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.weight != b.weight || a.filtered != b.filtered || a.aimed != b.aimed) return false;
        if (!a.dimAbbrs.equals(b.dimAbbrs)) return false;
        if (a.ore == null || b.ore == null) return a.ore == b.ore;
        GTUtility.ItemId aid = GTUtility.ItemId.create(a.ore);
        GTUtility.ItemId bid = GTUtility.ItemId.create(b.ore);
        return aid != null && aid.equals(bid);
    }

    /**
     * Server action sync handler shared by all configuration controls.
     */
    public static class MinerActionSyncHandler extends SyncHandler<MinerActionSyncHandler> {

        private static final int ACTION_CYCLE_ORE_MODE = 1;
        private static final int ACTION_CYCLE_FORTUNE = 2;
        private static final int ACTION_TOGGLE_FILTER = 3;
        private static final int ACTION_REFRESH_POOL = 4;
        private static final int ACTION_TOGGLE_DIRECTIONAL = 5;
        private static final int ACTION_TOGGLE_DIRECTIONAL_ORE = 6;
        private static final int ACTION_CLEAR_CONFIG = 7;

        private final LargeVoidMiner miner;

        public MinerActionSyncHandler(LargeVoidMiner miner) {
            this.miner = miner;
            allowC2S();
        }

        public void sendCycleOreMode() {
            syncToServer(ACTION_CYCLE_ORE_MODE, buf -> {});
        }

        public void sendCycleFortune() {
            syncToServer(ACTION_CYCLE_FORTUNE, buf -> {});
        }

        public void sendToggleFilter(OreEntryInfo info) {
            writeOreAction(ACTION_TOGGLE_FILTER, info);
        }

        public void sendRefreshPool() {
            syncToServer(ACTION_REFRESH_POOL, buf -> {});
        }

        public void sendToggleDirectional() {
            syncToServer(ACTION_TOGGLE_DIRECTIONAL, buf -> {});
        }

        public void sendToggleDirectionalOre(OreEntryInfo info) {
            writeOreAction(ACTION_TOGGLE_DIRECTIONAL_ORE, info);
        }

        public void sendClearConfig() {
            syncToServer(ACTION_CLEAR_CONFIG, buf -> {});
        }

        private void writeOreAction(int action, OreEntryInfo info) {
            syncToServer(action, buf -> {
                GameRegistry.UniqueIdentifier uid = info == null || info.ore == null ? null
                    : GameRegistry.findUniqueIdentifierFor(info.ore.getItem());
                ByteBufUtils.writeUTF8String(buf, uid == null ? "" : uid.modId + ":" + uid.name);
                buf.writeInt(info == null || info.ore == null ? 0 : info.ore.getItemDamage());
            });
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) throws IOException {}

        @Override
        public void readOnServer(int id, PacketBuffer buf) throws IOException {
            switch (id) {
                case ACTION_CYCLE_ORE_MODE:
                    miner.cycleOreMode();
                    break;
                case ACTION_CYCLE_FORTUNE:
                    miner.cycleFortuneLevel();
                    break;
                case ACTION_TOGGLE_FILTER:
                    toggleOre(buf, false);
                    break;
                case ACTION_REFRESH_POOL:
                    miner.forceRefreshPool();
                    break;
                case ACTION_TOGGLE_DIRECTIONAL:
                    miner.toggleDirectionalMode(null);
                    break;
                case ACTION_TOGGLE_DIRECTIONAL_ORE:
                    toggleOre(buf, true);
                    break;
                case ACTION_CLEAR_CONFIG:
                    miner.clearCurrentModeConfig();
                    break;
                default:
                    break;
            }
        }

        private void toggleOre(PacketBuffer buf, boolean directional) {
            String name = ByteBufUtils.readUTF8String(buf);
            int meta = buf.readInt();
            if (name == null || name.isEmpty()) return;
            String[] parts = name.split(":", 2);
            if (parts.length != 2) return;
            Item item = GameRegistry.findItem(parts[0], parts[1]);
            if (item == null) return;
            GTUtility.ItemId id = GTUtility.ItemId.createNoCopy(new ItemStack(item, 1, meta));
            if (directional) {
                miner.setOreAimed(id, !miner.isOreAimed(id));
            } else {
                miner.setOreFiltered(id, !miner.isOreFiltered(id));
            }
        }
    }

}
