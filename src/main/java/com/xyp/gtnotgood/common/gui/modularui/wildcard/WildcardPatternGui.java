package com.xyp.gtnotgood.common.gui.modularui.wildcard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardComponentCodec;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardExpansion;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;

import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;

/**
 * 通配样板符的 MUI2 GUI（手持物品）。标签页导航 + 卡片式可增删列表 + 主页预览。
 *
 * <p>
 * 同步模型：客户端在工作副本（inputs/outputs/filters）上即时编辑；点保存时通过一个 C2S 的
 * StringSyncValue 把序列化后的配置发到服务端，服务端 setter 解析并写回真实物品并 markDirty。
 * 每个 widget 的编辑绑定都是纯赋值，不在其中触碰面板结构（参考 steam-void-miner-dim-filter 的崩溃教训）。
 */
public class WildcardPatternGui {

    private static final int MAX_COMPONENTS = 6;
    private static final int PANEL_W = 192;
    private static final int PANEL_H = 292;

    static final com.xyp.ldlib.integration.modularui.ModernThemeAdapter THEME = new com.xyp.ldlib.integration.modularui.ModernThemeAdapter(
        path -> com.xyp.gtnotgood.utils.enums.ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));

    private static final IDrawable REFERENCE_PURPLE = com.xyp.ldlib.integration.modularui.ModernThemeAdapter.drawable(
        THEME.theme.button.copy()
            .setColor(0xFF9933FF));

    private static final IDrawable REFERENCE_CYAN = com.xyp.ldlib.integration.modularui.ModernThemeAdapter.drawable(
        THEME.theme.button.copy()
            .setColor(0xFF337777));

    /** Uses the black numeric editor found in the reference, retaining MUI value synchronization. */
    private static TextFieldWidget referenceField() {
        return new TextFieldWidget().setTextColor(0xFFFFFFFF);
    }

    private static final IDrawable REFERENCE_CLOSE = com.cleanroommc.modularui.drawable.UITexture.builder()
        .location(com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD, "gui/ldlib/modern/close")
        .build();

    private final int slotIndex;

    private final List<IWildcardIOComponent> inputs = new ArrayList<>();
    private final List<IWildcardIOComponent> outputs = new ArrayList<>();
    private final List<IWildcardFilterComponent> filters = new ArrayList<>();

    private ItemStack backingStack;
    private StringSyncValue configSync;

    private WildcardIndexPage previewPage;
    private ListWidget<IWidget, ?> filterCards;

    public WildcardPatternGui(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        this.backingStack = data.getUsedItemStack();
        loadFromStack();

        // C2S 配置同步：客户端 getter 序列化工作副本；服务端 setter 解析并写回真实物品。
        // setter 是纯数据写入，不触碰任何 widget/面板结构。
        this.configSync = new StringSyncValue(this::serializeConfig, cfg -> applyConfigOnServer(data, cfg));
        this.configSync.allowC2S();
        syncManager.syncValue("wildcardConfig", this.configSync);

        PagedWidget.Controller controller = new PagedWidget.Controller();

        Flow tabs = Flow.column()
            .pos(0, 24)
            .size(24, 180)
            .child(tabButton(controller, 0, new ItemDrawable(iconStack())))
            .child(
                tabButton(
                    controller,
                    1,
                    IKey.str("IN")
                        .color(0xFFFFFFFF)
                        .shadow(true)))
            .child(
                tabButton(
                    controller,
                    2,
                    IKey.str("OUT")
                        .color(0xFFFFFFFF)
                        .shadow(true)))
            .child(tabButton(controller, 3, new ItemDrawable(dustOf(Materials.Aluminium))));
        PagedWidget<?> pages = new PagedWidget<>().controller(controller)
            .pos(27, 23)
            .size(158, 180)
            .addPage(buildPreviewPage())
            .addPage(buildIOPage(true))
            .addPage(buildIOPage(false))
            .addPage(buildFilterPage());
        IDrawable shell = (context, x, y, w, h, style) -> {
            THEME.panel.draw(context, x + 20, y + 16, 172, h - 16, style);
            THEME.panel.draw(context, x + 28, y + 3, 18, 15, style);
            THEME.panel.draw(context, x + 46, y, 138, 18, style);
        };
        com.cleanroommc.modularui.widgets.SlotGroupWidget inventory = com.cleanroommc.modularui.widgets.SlotGroupWidget
            .playerInventory((index, slot) -> {
                slot.background(THEME.slot);
                if (index == slotIndex) slot.setEnabled(false);
                return slot;
            })
            .pos(25, 110);
        ModularPanel panel = ModularPanel.defaultPanel("wildcard_pattern", PANEL_W, PANEL_H - 100)
            .background(shell)
            .disableHoverBackground()
            .child(
                THEME.button()
                    .pos(28, 5)
                    .size(18, 12)
                    .background((IDrawable) null)
                    .overlay(
                        IKey.str("<")
                            .color(0xFF000000))
                    .onMousePressed(mouse -> {
                        controller.setPage(0);
                        return true;
                    }))
            .child(
                new ItemDrawable(iconStack()).asWidget()
                    .pos(49, 3)
                    .size(12))
            .child(
                // #tr gui.wildcardpattern.reference_title
                // # Wildcard Pattern Configuration
                // # zh_CN 通配符样板配置
                IKey.lang("gui.wildcardpattern.reference_title")
                    .asWidget()
                    .pos(64, 4)
                    .size(117, 10))
            .child(pages)
            .child(tabs)
            .child(inventory);
        pages.onPageChange(page -> {
            int offset = page == 0 ? 100 : 0;
            panel.height(PANEL_H - offset);
            pages.height(180 - offset);
            inventory.top(210 - offset);
            panel.scheduleResize();
        });
        return panel;
    }

    private PageButton tabButton(PagedWidget.Controller controller, int index, IDrawable icon) {
        IDrawable padded = (context, x, y, w, h, style) -> {
            if (icon instanceof IKey) icon.draw(context, x + 2, y + 4, w - 4, h - 8, style);
            else icon.draw(context, x + 3, y + 4, w - 7, h - 8, style);
        };
        return new PageButton(index, controller).size(24, 26)
            .background(false, THEME.tab, padded)
            .background(true, THEME.selectedTab, padded)
            .marginBottom(2);
    }

    private ItemStack iconStack() {
        return backingStack != null ? backingStack.copy()
            : new ItemStack(com.xyp.gtnotgood.loader.ItemsLoader.wildcardPattern);
    }

    // ============================================================
    // 主页/预览：源版双槽组与每秒轮播
    // ============================================================

    private IWidget buildPreviewPage() {
        previewPage = new WildcardIndexPage(this::excludePreviewMaterial);
        previewPage.refresh(WildcardExpansion.expand(inputs, outputs, filters));
        return previewPage;
    }

    /** Adds an exact-name blacklist entry once, saves through the existing C2S path and refreshes both views. */
    private void excludePreviewMaterial(Materials material) {
        if (material == null) return;
        for (IWildcardFilterComponent filter : filters) {
            if (filter instanceof StringFilterComponent) {
                StringFilterComponent name = (StringFilterComponent) filter;
                if (name.isExact() && !name.isWhitelist() && material.mName.equalsIgnoreCase(name.getPattern())) return;
            }
        }
        filters.add(StringFilterComponent.exactBlacklist(material.mName));
        pushConfig();
        if (filterCards != null) rebuildFilterCards(filterCards);
    }

    private static String safeName(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (RuntimeException ignored) {
            return String.valueOf(stack.getItem());
        }
    }

    // ============================================================
    // 输入/输出页
    // ============================================================

    private IWidget buildIOPage(boolean input) {
        List<IWildcardIOComponent> list = input ? inputs : outputs;
        ListWidget<IWidget, ?> cards = new ListWidget<>().pos(0, 3)
            .size(156, 150);
        rebuildIOCards(cards, list);
        return new com.cleanroommc.modularui.widget.ParentWidget<>().size(158, 180)
            .child(cards)
            .child(
                // #tr gui.wildcardpattern.single
                // # Single
                // # zh_CN 单一
                addButton("gui.wildcardpattern.single", () -> addIO(cards, list, SimpleIOComponent.empty()))
                    .pos(2, 155))
            .child(
                // #tr gui.wildcardpattern.tag
                // # Tag
                // # zh_CN 标签
                addButton("gui.wildcardpattern.tag", () -> addIO(cards, list, PrefixIOComponent.empty())).pos(37, 155))
            .child(saveButton().pos(126, 155));
    }

    private void addIO(ListWidget<IWidget, ?> cards, List<IWildcardIOComponent> list, IWildcardIOComponent component) {
        if (list.size() < MAX_COMPONENTS) {
            list.add(component);
            rebuildIOCards(cards, list);
        }
    }

    private void rebuildIOCards(ListWidget<IWidget, ?> cards, List<IWildcardIOComponent> list) {
        cards.removeAll();
        for (int i = 0; i < list.size(); i++) {
            cards.child(ioCard(cards, list, i));
        }
    }

    private IWidget ioCard(ListWidget<IWidget, ?> cards, List<IWildcardIOComponent> list, int index) {
        IWildcardIOComponent component = list.get(index);
        com.cleanroommc.modularui.widget.ParentWidget<?> row = new com.cleanroommc.modularui.widget.ParentWidget<>()
            .size(156, 25)
            .background(component instanceof PrefixIOComponent ? REFERENCE_PURPLE : REFERENCE_CYAN);
        if (component instanceof PrefixIOComponent) row.child(prefixEditor((PrefixIOComponent) component));
        else if (component instanceof SimpleIOComponent) row.child(simpleEditor((SimpleIOComponent) component));
        else if (component instanceof FluidIOComponent) row.child(fluidEditor((FluidIOComponent) component).pos(22, 4));
        row.child(deleteButton(() -> {
            if (index >= 0 && index < list.size()) {
                list.remove(index);
                rebuildIOCards(cards, list);
            }
        }).marginLeft(0)
            .pos(138, 5));
        return row;
    }

    /** 前缀组件编辑：拖入槽(自动识别前缀) + 前缀名文本框 + 数量数字框。 */
    private IWidget prefixEditor(PrefixIOComponent component) {
        WildcardDropWidget drop = new WildcardDropWidget(component::getDisplayStack, stack -> {
            WildcardMaterials.PrefixMaterial pm = WildcardMaterials.parseItem(stack);
            if (pm.prefix != null) component.setPrefix(pm.prefix);
        });
        TextFieldWidget name = THEME.textField()
            .pos(25, 5)
            .size(43, 15)
            .background((IDrawable) null)
            .value(new StringValue.Dynamic(component::getRawText, component::setRawText))
            .setPattern(java.util.regex.Pattern.compile("[a-zA-Z0-9]*"));
        TextFieldWidget amount = referenceField().pos(80, 5)
            .size(50, 15)
            .value(
                new com.cleanroommc.modularui.value.LongValue.Dynamic(
                    component::getAmount,
                    val -> component.setAmount((int) Math.max(1, Math.min(Integer.MAX_VALUE, val)))))
            .numbersLong(1, Integer.MAX_VALUE);
        return new com.cleanroommc.modularui.widget.ParentWidget<>().size(136, 25)
            .child(drop.pos(3, 3))
            .child(name)
            .child(amount)
            .child(
                IKey.str("x")
                    .color(0xFFFFFFFF)
                    .shadow(true)
                    .asWidget()
                    .pos(70, 7)
                    .size(8, 10));
    }

    /** 固定组件编辑：拖入槽(放固定物品或固定流体) + 名称 + 数量数字框。布局与前缀组件对齐。 */
    private IWidget simpleEditor(SimpleIOComponent component) {
        WildcardDropWidget drop = new WildcardDropWidget(
            component::getStack,
            stack -> component.setStack(normalizeFixedStack(stack)));
        return new com.cleanroommc.modularui.widget.ParentWidget<>().size(136, 25)
            .child(drop.pos(3, 3))
            .child(
                IKey.dynamic(() -> component.getStack() == null ? "" : safeName(component.getStack()))
                    .color(0xFFFFFFFF)
                    .shadow(true)
                    .asWidget()
                    .pos(25, 7)
                    .size(43, 10))
            .child(
                IKey.str("x")
                    .color(0xFFFFFFFF)
                    .shadow(true)
                    .asWidget()
                    .pos(70, 7)
                    .size(8, 10))
            .child(
                referenceField().pos(80, 5)
                    .size(50, 15)
                    .value(
                        new com.cleanroommc.modularui.value.LongValue.Dynamic(
                            component::getAmount,
                            component::setAmount))
                    .numbersLong(1, Integer.MAX_VALUE));
    }

    /**
     * 归一化固定组件放入的物品：若是 GT 流体显示物品，转成 AE2FC 的 ItemFluidDrop（合成 CPU 能识别的流体请求）；
     * 否则原样返回。这让"+固定"既能放固定物品也能放固定流体（如熔融橡胶）。
     */
    private static ItemStack normalizeFixedStack(ItemStack stack) {
        if (stack == null) return null;
        net.minecraftforge.fluids.FluidStack fluid = gregtech.api.util.GTUtility.getFluidFromDisplayStack(stack);
        if (fluid != null && fluid.getFluid() != null) {
            if (fluid.amount <= 0) fluid.amount = 1000;
            // [液滴分类] 必须留液滴：把固定流体转成 ItemFluidDrop 存入样板，合成 CPU 才能识别为流体请求
            return com.xyp.gtnotgood.common.compat.FluidDropCompat.newStack(fluid);
        }
        return stack;
    }

    /** 流体组件编辑：状态循环按钮 + 数量文本框。 */
    private Flow fluidEditor(FluidIOComponent component) {
        ButtonWidget<?> stateButton = THEME.button()
            .size(58, 16)
            .marginLeft(2)
            .overlay(
                IKey.dynamic(
                    () -> component.getState() == null ? "?"
                        : component.getState()
                            .name()))
            .onMousePressed(button -> {
                component.setState(nextFluidState(component.getState()));
                return true;
            });
        TextFieldWidget amountField = THEME.textField()
            .size(40, 16)
            .marginLeft(3)
            .setTextColor(0xFFFFFFFF)
            .value(
                new com.cleanroommc.modularui.value.LongValue.Dynamic(
                    () -> component.getAmount(),
                    component::setAmount))
            .numbersLong(1, Integer.MAX_VALUE);
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(stateButton)
            .child(amountField);
    }

    private static FluidState nextFluidState(FluidState current) {
        FluidState[] order = { FluidState.MOLTEN, FluidState.LIQUID, FluidState.GAS, FluidState.PLASMA };
        int idx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == current) {
                idx = i;
                break;
            }
        }
        return order[(idx + 1) % order.length];
    }

    private IWidget iconWidget(IWildcardIOComponent component) {
        ItemStack display = component == null ? null : component.getDisplayStack();
        if (display == null) {
            return Flow.row()
                .size(16);
        }
        return new ItemDrawable(display).asWidget()
            .size(16);
    }

    private String ioLabel(IWildcardIOComponent component) {
        if (component instanceof SimpleIOComponent) {
            ItemStack stack = ((SimpleIOComponent) component).getStack();
            return stack == null ? "(empty)" : safeName(stack);
        }
        return component == null ? "" : component.typeKey();
    }

    // ============================================================
    // 过滤页
    // ============================================================

    private IWidget buildFilterPage() {
        ListWidget<IWidget, ?> cards = new ListWidget<>().pos(0, 3)
            .size(156, 150);
        filterCards = cards;
        rebuildFilterCards(cards);
        return new com.cleanroommc.modularui.widget.ParentWidget<>().size(158, 180)
            .child(cards)
            .child(
                // #tr gui.wildcardpattern.filter_name
                // # Name
                // # zh_CN 名称
                addButton("gui.wildcardpattern.filter_name", () -> addFilter(cards, StringFilterComponent.empty()))
                    .pos(2, 155))
            .child(
                // #tr gui.wildcardpattern.filter_property
                // # Property
                // # zh_CN 属性
                addButton(
                    "gui.wildcardpattern.filter_property",
                    () -> addFilter(cards, PropertyFilterComponent.empty())).pos(37, 155))
            .child(
                // #tr gui.wildcardpattern.filter_subtag
                // # Flag
                // # zh_CN 标记
                addButton("gui.wildcardpattern.filter_subtag", () -> addFilter(cards, SubTagFilterComponent.empty()))
                    .pos(72, 155))
            .child(saveButton().pos(126, 155));
    }

    private void addFilter(ListWidget<IWidget, ?> cards, IWildcardFilterComponent component) {
        if (filters.size() < MAX_COMPONENTS) {
            filters.add(component);
            rebuildFilterCards(cards);
        }
    }

    private void rebuildFilterCards(ListWidget<IWidget, ?> cards) {
        cards.removeAll();
        for (int i = 0; i < filters.size(); i++) {
            cards.child(filterCard(cards, i));
        }
    }

    private IWidget filterCard(ListWidget<IWidget, ?> cards, int index) {
        IWildcardFilterComponent component = filters.get(index);
        com.cleanroommc.modularui.widget.ParentWidget<?> row = new com.cleanroommc.modularui.widget.ParentWidget<>()
            .size(156, 25)
            .background(filterBackground(component));
        if (component instanceof StringFilterComponent)
            row.child(stringFilterEditor((StringFilterComponent) component));
        else if (component instanceof PropertyFilterComponent)
            row.child(propertyEditor(cards, (PropertyFilterComponent) component));
        else if (component instanceof SubTagFilterComponent)
            row.child(subTagEditor(cards, (SubTagFilterComponent) component));
        row.child(
            whitelistButton(component).pos(120, 5)
                .size(14));
        row.child(deleteButton(() -> {
            if (index >= 0 && index < filters.size()) {
                filters.remove(index);
                rebuildFilterCards(cards);
            }
        }).marginLeft(0)
            .pos(138, 5));
        return row;
    }

    /** Source filter palette: type chooses the color pair, W/B selects its light or dark member. */
    private static IDrawable filterBackground(IWildcardFilterComponent component) {
        int whitelistColor = 0xFF44AAFF, blacklistColor = 0xFF4852FF;
        if (component instanceof PropertyFilterComponent) {
            whitelistColor = 0xFFFFFF33;
            blacklistColor = 0xFFFF8800;
        } else if (component instanceof SubTagFilterComponent) {
            whitelistColor = 0xFFFF33FF;
            blacklistColor = 0xFF9933FF;
        }
        IDrawable whitelist = com.xyp.ldlib.integration.modularui.ModernThemeAdapter.drawable(
            THEME.theme.button.copy()
                .setColor(whitelistColor));
        IDrawable blacklist = com.xyp.ldlib.integration.modularui.ModernThemeAdapter.drawable(
            THEME.theme.button.copy()
                .setColor(blacklistColor));
        return new com.cleanroommc.modularui.drawable.DynamicDrawable(
            () -> component.isWhitelist() ? whitelist : blacklist);
    }

    private IWidget stringFilterEditor(StringFilterComponent component) {
        return THEME.textField()
            .pos(3, 5)
            .size(110, 15)
            .setTextColor(0xFFFFFFFF)
            .value(new StringValue.Dynamic(component::getPattern, component::setPattern));
    }

    /** Source selector: changing the example replaces candidates and selects the first available property. */
    private IWidget propertyEditor(ListWidget<IWidget, ?> cards, PropertyFilterComponent component) {
        com.xyp.ldlib.integration.modularui.SelectorWidget selector = new com.xyp.ldlib.integration.modularui.SelectorWidget(
            "wildcard_property_" + filters.indexOf(component),
            THEME.button,
            propertyNames(component.getExample()))
                .setValue(
                    component.getProperty() == null ? ""
                        : component.getProperty()
                            .displayName())
                .setOnChanged(name -> component.setProperty(WildcardMaterials.findProperty(name)));
        WildcardDropWidget drop = new WildcardDropWidget(
            () -> component.getExample() == null ? null : dustOf(component.getExample()),
            stack -> {
                Materials material = WildcardMaterials.parseItem(stack).material;
                if (!WildcardMaterials.isRealMaterial(material)) return;
                component.setExample(material);
                List<String> names = propertyNames(material);
                selector.setCandidates(names);
                selector.setValue(names.get(0));
                component.setProperty(WildcardMaterials.findProperty(names.get(0)));
            });
        return new com.cleanroommc.modularui.widget.ParentWidget<>().size(115, 25)
            .child(drop.pos(3, 3))
            .child(selector.pos(25, 5));
    }

    private static List<String> propertyNames(Materials material) {
        List<String> names = new ArrayList<>();
        if (WildcardMaterials.isRealMaterial(material)) {
            for (WildcardMaterials.Property property : WildcardMaterials.propertiesOf(material))
                names.add(property.displayName());
        }
        if (names.isEmpty()) {
            // #tr gui.wildcardpattern.no_property
            // # no property
            // # zh_CN 无属性
            names.add(
                IKey.lang("gui.wildcardpattern.no_property")
                    .get());
        }
        return names;
    }

    /** Example material icon used by both dropdown editors. */
    private static ItemStack dustOf(Materials material) {
        return WildcardMaterials.makePrefixStack(OrePrefixes.dust, material, 1);
    }

    /** Flags use 1.7.10 SubTags with the original source's candidate replacement interaction. */
    private IWidget subTagEditor(ListWidget<IWidget, ?> cards, SubTagFilterComponent component) {
        com.xyp.ldlib.integration.modularui.SelectorWidget selector = new com.xyp.ldlib.integration.modularui.SelectorWidget(
            "wildcard_subtag_" + filters.indexOf(component),
            THEME.button,
            subTagNames(component.getExample()))
                .setValue(component.getSubTag() == null ? "" : component.getSubTag().mName)
                .setOnChanged(name -> component.setSubTag(WildcardMaterials.findSubTag(name)));
        WildcardDropWidget drop = new WildcardDropWidget(
            () -> component.getExample() == null ? null : dustOf(component.getExample()),
            stack -> {
                Materials material = WildcardMaterials.parseItem(stack).material;
                if (!WildcardMaterials.isRealMaterial(material)) return;
                component.setExample(material);
                List<String> names = subTagNames(material);
                selector.setCandidates(names);
                selector.setValue(names.get(0));
                component.setSubTag(WildcardMaterials.findSubTag(names.get(0)));
            });
        return new com.cleanroommc.modularui.widget.ParentWidget<>().size(115, 25)
            .child(drop.pos(3, 3))
            .child(selector.pos(25, 5));
    }

    private static List<String> subTagNames(Materials material) {
        List<String> names = new ArrayList<>();
        if (WildcardMaterials.isRealMaterial(material)) {
            for (gregtech.api.enums.SubTag tag : WildcardMaterials.subTagsOf(material)) names.add(tag.mName);
        }
        if (names.isEmpty()) {
            // #tr gui.wildcardpattern.no_flag
            // # no flag
            // # zh_CN 无标记
            names.add(
                IKey.lang("gui.wildcardpattern.no_flag")
                    .get());
        }
        return names;
    }

    private ButtonWidget<?> whitelistButton(IWildcardFilterComponent component) {
        return THEME.button()
            .size(16)
            .overlay(IKey.dynamic(() -> component.isWhitelist() ? "W" : "B"))
            .onMousePressed(button -> {
                component.setWhitelist(!component.isWhitelist());
                return true;
            });
    }

    // ============================================================
    // 公共按钮
    // ============================================================

    private ButtonWidget<?> addButton(String langKey, Runnable action) {
        return THEME.button()
            .size(30, 20)
            .marginRight(2)
            .overlay(
                IKey.lang(langKey)
                    .color(0xFFFFFFFF)
                    .shadow(true))
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> deleteButton(Runnable action) {
        return THEME.button()
            .size(14)
            .marginLeft(3)
            .overlay(REFERENCE_CLOSE)
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> saveButton() {
        return THEME.button()
            .size(30, 20)
            // #tr gui.wildcardpattern.save
            // # Save
            // # zh_CN 保存
            .overlay(
                IKey.lang("gui.wildcardpattern.save")
                    .color(0xFFFFFFFF)
                    .shadow(true))
            .onMousePressed(button -> {
                pushConfig();
                return true;
            });
    }

    /** 把当前工作副本推送到服务端（C2S），并同步写回本地物品副本（tooltip/预览即时反映）。 */
    private void pushConfig() {
        if (configSync != null) {
            configSync.setValue(serializeConfig(), true, true);
        }
        saveToLocalStack();
        if (previewPage != null) previewPage.refresh(WildcardExpansion.expand(inputs, outputs, filters));
    }

    private void saveToLocalStack() {
        if (backingStack == null) return;
        WildcardModelState.ensureInitialized(backingStack);
        WildcardModelState.setInputs(backingStack, inputs);
        WildcardModelState.setOutputs(backingStack, outputs);
        WildcardModelState.setFilters(backingStack, filters);
        WildcardModelState.setExpandedCount(backingStack, countExpanded());
    }

    // ============================================================
    // 同步 / 状态读写
    // ============================================================

    private String serializeConfig() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(WildcardModelState.KEY_INPUT, WildcardComponentCodec.writeIO(inputs));
        tag.setTag(WildcardModelState.KEY_OUTPUT, WildcardComponentCodec.writeIO(outputs));
        tag.setTag(WildcardModelState.KEY_FILTER, WildcardComponentCodec.writeFilters(filters));
        return tag.toString();
    }

    /** 服务端 setter：解析配置并写回真实物品。纯数据写入，不触碰面板结构。 */
    private void applyConfigOnServer(PlayerInventoryGuiData data, String cfg) {
        if (cfg == null || cfg.isEmpty()) return;
        NBTTagCompound parsed;
        try {
            parsed = (NBTTagCompound) net.minecraft.nbt.JsonToNBT.func_150315_a(cfg);
        } catch (net.minecraft.nbt.NBTException e) {
            return;
        }
        ItemStack stack = data.getUsedItemStack();
        if (stack == null) return;
        WildcardPatternGenerator.markAsWildcard(stack);
        List<IWildcardIOComponent> in = WildcardComponentCodec.readIO(parsed, WildcardModelState.KEY_INPUT);
        List<IWildcardIOComponent> out = WildcardComponentCodec.readIO(parsed, WildcardModelState.KEY_OUTPUT);
        List<IWildcardFilterComponent> fil = WildcardComponentCodec.readFilters(parsed, WildcardModelState.KEY_FILTER);
        WildcardModelState.ensureInitialized(stack);
        WildcardModelState.setInputs(stack, in);
        WildcardModelState.setOutputs(stack, out);
        WildcardModelState.setFilters(stack, fil);
        WildcardModelState.setExpandedCount(stack, WildcardExpansion.countExpanded(in, out, fil));
    }

    private void loadFromStack() {
        inputs.clear();
        outputs.clear();
        filters.clear();
        if (backingStack == null) return;
        WildcardPatternGenerator.markAsWildcard(backingStack);
        inputs.addAll(WildcardModelState.getInputs(backingStack));
        outputs.addAll(WildcardModelState.getOutputs(backingStack));
        filters.addAll(WildcardModelState.getFilters(backingStack));
    }

    private int countExpanded() {
        return WildcardExpansion.countExpanded(inputs, outputs, filters);
    }
}
