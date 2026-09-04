package com.xyp.gtnotgood.common.gui.modularui.wildcard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
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
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 210;

    private final int slotIndex;

    private final List<IWildcardIOComponent> inputs = new ArrayList<>();
    private final List<IWildcardIOComponent> outputs = new ArrayList<>();
    private final List<IWildcardFilterComponent> filters = new ArrayList<>();

    private ItemStack backingStack;
    private StringSyncValue configSync;

    // 预览状态（客户端）
    private java.util.List<WildcardExpansion.Expanded> previewCache = java.util.Collections.emptyList();
    private String previewSearch = "";

    public WildcardPatternGui(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(com.xyp.gtnotgood.GTNotGood.MODID, mainPanel);
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
            .width(22)
            .heightRel(1f)
            .child(tabButton(controller, 0, new ItemDrawable(iconStack())))
            .child(tabButton(controller, 1, IKey.str("IN")))
            .child(tabButton(controller, 2, IKey.str("OUT")))
            .child(tabButton(controller, 3, IKey.str("FIL")));

        PagedWidget<?> pages = new PagedWidget<>().controller(controller)
            .sizeRel(1f, 1f)
            .addPage(buildPreviewPage())
            .addPage(buildIOPage(true))
            .addPage(buildIOPage(false))
            .addPage(buildFilterPage());

        return ModularPanel.defaultPanel("wildcard_pattern", PANEL_W, PANEL_H)
            .child(
                // #tr gui.wildcardpattern.title
                // # Wildcard Pattern
                // # zh_CN 通配样板符
                IKey.lang("gui.wildcardpattern.title")
                    .asWidget()
                    .pos(8, 6))
            .child(
                Flow.row()
                    .top(18)
                    .left(6)
                    .right(6)
                    .bottom(6)
                    .child(tabs)
                    .child(
                        pages.marginLeft(3)
                            .expanded()
                            .heightRel(1f)));
    }

    private PageButton tabButton(PagedWidget.Controller controller, int index, IDrawable icon) {
        return new PageButton(index, controller).size(20, 18)
            .background(false, GuiTextures.MC_BUTTON, icon)
            .background(true, GuiTextures.MC_BUTTON_PRESSED, icon)
            .marginBottom(2);
    }

    private ItemStack iconStack() {
        return backingStack != null ? backingStack.copy()
            : new ItemStack(com.xyp.gtnotgood.loader.ItemsLoader.wildcardPattern);
    }

    // ============================================================
    // 主页/预览：可滚动列表 + 搜索 + 快速排除
    // ============================================================

    private IWidget buildPreviewPage() {
        ListWidget<IWidget, ?> list = new ListWidget<>().size(158, 150);
        rebuildPreviewList(list);

        TextFieldWidget search = new TextFieldWidget().size(150, 16)
            .marginTop(2)
            .setTextColor(0xFFFFFFFF)
            .value(new StringValue.Dynamic(() -> previewSearch, text -> {
                previewSearch = text == null ? "" : text;
                rebuildPreviewList(list);
            }));

        return Flow.column()
            .sizeRel(1f, 1f)
            .child(
                // #tr gui.wildcardpattern.patterns_available
                // # Patterns:
                // # zh_CN 可用样板:
                IKey.dynamic(
                    () -> IKey.lang("gui.wildcardpattern.patterns_available")
                        .get() + " "
                        + countExpanded())
                    .asWidget()
                    .marginTop(4)
                    .marginBottom(2))
            .child(list)
            .child(search);
    }

    private void rebuildPreviewList(ListWidget<IWidget, ?> list) {
        list.removeAll();
        refreshPreviewCache();
        String q = previewSearch.trim()
            .toLowerCase(java.util.Locale.ROOT);
        int shown = 0;
        for (WildcardExpansion.Expanded ex : previewCache) {
            if (ex.material == null) continue;
            if (!q.isEmpty() && !ex.material.mName.toLowerCase(java.util.Locale.ROOT)
                .contains(q)) continue;
            if (shown++ >= 200) break; // 上限保护
            list.child(previewRow(list, ex));
        }
        if (shown == 0) {
            list.child(
                // #tr gui.wildcardpattern.empty_hint
                // # Add a component below
                // # zh_CN 点下方按钮添加组件
                IKey.lang("gui.wildcardpattern.empty_hint")
                    .asWidget()
                    .height(12));
        }
    }

    /** 一行预览：[材料名] [输入图标...] > [输出图标...] [X排除]。 */
    private IWidget previewRow(ListWidget<IWidget, ?> list, WildcardExpansion.Expanded ex) {
        final Materials material = ex.material;
        Flow row = Flow.row()
            .widthRel(1f)
            .height(20)
            .marginBottom(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.str(material.mName)
                    .asWidget()
                    .size(70, 16)
                    .marginRight(2));
        for (ItemStack in : ex.inputs) {
            row.child(new WildcardIconWidget(() -> in));
        }
        row.child(
            IKey.str(">")
                .asWidget()
                .size(8, 16)
                .marginLeft(2)
                .marginRight(2));
        for (ItemStack out : ex.outputs) {
            row.child(new WildcardIconWidget(() -> out));
        }
        // X 排除：把该材料作为精确名黑名单加进筛选，立即保存并刷新预览
        row.child(
            new ButtonWidget<>().size(14)
                .marginLeft(3)
                .overlay(IKey.str("x"))
                .onMousePressed(button -> {
                    if (excludeMaterial(material)) {
                        pushConfig();
                        rebuildPreviewList(list);
                    }
                    return true;
                }));
        return row;
    }

    /** 把某材料加入筛选黑名单（精确名）。已存在返回 false；成功添加返回 true。 */
    private boolean excludeMaterial(Materials material) {
        if (material == null || material.mName == null) return false;
        for (IWildcardFilterComponent f : filters) {
            if (f instanceof StringFilterComponent) {
                StringFilterComponent sf = (StringFilterComponent) f;
                if (sf.isExact() && !sf.isWhitelist() && material.mName.equalsIgnoreCase(sf.getPattern())) {
                    return false; // 已排除
                }
            }
        }
        // 预览快速排除不受可编辑组件上限约束（筛选页是可滚动列表，能容纳更多）
        filters.add(StringFilterComponent.exactBlacklist(material.mName));
        return true;
    }

    private void refreshPreviewCache() {
        previewCache = WildcardExpansion.expand(inputs, outputs, filters);
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
        ListWidget<IWidget, ?> cards = new ListWidget<>().sizeRel(1f, 0.8f);
        rebuildIOCards(cards, list);

        Flow buttons = Flow.row()
            .coverChildrenHeight()
            .widthRel(1f)
            .marginTop(2)
            // #tr gui.wildcardpattern.add_prefix
            // # +Prefix
            // # zh_CN +前缀
            .child(addButton("gui.wildcardpattern.add_prefix", () -> addIO(cards, list, PrefixIOComponent.empty())))
            // #tr gui.wildcardpattern.add_fluid
            // # +Fluid
            // # zh_CN +流体
            .child(addButton("gui.wildcardpattern.add_fluid", () -> addIO(cards, list, FluidIOComponent.empty())))
            // #tr gui.wildcardpattern.add_fixed
            // # +Fixed
            // # zh_CN +固定
            .child(addButton("gui.wildcardpattern.add_fixed", () -> addIO(cards, list, SimpleIOComponent.empty())))
            .child(saveButton());

        return Flow.column()
            .sizeRel(1f, 1f)
            .child(cards)
            .child(buttons);
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
        Flow row = Flow.row()
            .widthRel(1f)
            .height(20)
            .marginBottom(2)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER);

        if (component instanceof PrefixIOComponent) {
            row.child(prefixEditor((PrefixIOComponent) component));
        } else if (component instanceof FluidIOComponent) {
            row.child(iconWidget(component))
                .child(fluidEditor((FluidIOComponent) component));
        } else if (component instanceof SimpleIOComponent) {
            row.child(simpleEditor((SimpleIOComponent) component));
        } else {
            row.child(iconWidget(component))
                .child(
                    IKey.str(ioLabel(component))
                        .asWidget()
                        .height(16)
                        .marginLeft(2));
        }

        return row.child(deleteButton(() -> {
            if (index >= 0 && index < list.size()) {
                list.remove(index);
                rebuildIOCards(cards, list);
            }
        }));
    }

    /** 前缀组件编辑：拖入槽(自动识别前缀) + 前缀名文本框 + 数量数字框。 */
    private IWidget prefixEditor(PrefixIOComponent component) {
        WildcardDropWidget drop = new WildcardDropWidget(component::getDisplayStack, stack -> {
            WildcardMaterials.PrefixMaterial pm = WildcardMaterials.parseItem(stack);
            if (pm.prefix != null) component.setPrefix(pm.prefix);
        });
        TextFieldWidget prefixField = new TextFieldWidget().size(52, 16)
            .marginLeft(2)
            .setTextColor(0xFFFFFFFF)
            .value(new StringValue.Dynamic(component::getRawText, component::setRawText))
            .setPattern(java.util.regex.Pattern.compile("[a-zA-Z0-9]*"));
        TextFieldWidget amountField = new TextFieldWidget().size(36, 16)
            .marginLeft(3)
            .setTextColor(0xFFFFFFFF)
            .value(
                new com.cleanroommc.modularui.value.LongValue.Dynamic(
                    () -> component.getAmount(),
                    val -> component.setAmount((int) Math.max(1L, Math.min(Integer.MAX_VALUE, val)))))
            .numbersLong(1, Integer.MAX_VALUE);
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(drop)
            .child(prefixField)
            .child(amountField);
    }

    /** 固定组件编辑：拖入槽(放固定物品或固定流体) + 名称 + 数量数字框。布局与前缀组件对齐。 */
    private IWidget simpleEditor(SimpleIOComponent component) {
        WildcardDropWidget drop = new WildcardDropWidget(
            component::getStack,
            stack -> component.setStack(normalizeFixedStack(stack)));
        IWidget nameLabel = IKey.dynamic(() -> {
            ItemStack stack = component.getStack();
            return stack == null ? "(empty)" : safeName(stack);
        })
            .asWidget()
            .size(52, 16)
            .marginLeft(2);
        TextFieldWidget amountField = new TextFieldWidget().size(36, 16)
            .marginLeft(3)
            .setTextColor(0xFFFFFFFF)
            .value(new com.cleanroommc.modularui.value.LongValue.Dynamic(component::getAmount, component::setAmount))
            .numbersLong(1, Integer.MAX_VALUE);
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(drop)
            .child(nameLabel)
            .child(amountField);
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
    private IWidget fluidEditor(FluidIOComponent component) {
        ButtonWidget<?> stateButton = new ButtonWidget<>().size(58, 16)
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
        TextFieldWidget amountField = new TextFieldWidget().size(40, 16)
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
        ListWidget<IWidget, ?> cards = new ListWidget<>().sizeRel(1f, 0.8f);
        rebuildFilterCards(cards);

        Flow buttons = Flow.row()
            .coverChildrenHeight()
            .widthRel(1f)
            .marginTop(2)
            // #tr gui.wildcardpattern.add_property
            // # +Prop
            // # zh_CN +属性
            .child(
                addButton("gui.wildcardpattern.add_property", () -> addFilter(cards, PropertyFilterComponent.empty())))
            // #tr gui.wildcardpattern.add_subtag
            // # +Tag
            // # zh_CN +标签
            .child(addButton("gui.wildcardpattern.add_subtag", () -> addFilter(cards, SubTagFilterComponent.empty())))
            // #tr gui.wildcardpattern.add_string
            // # +Name
            // # zh_CN +名称
            .child(addButton("gui.wildcardpattern.add_string", () -> addFilter(cards, StringFilterComponent.empty())))
            .child(saveButton());

        return Flow.column()
            .sizeRel(1f, 1f)
            .child(cards)
            .child(buttons);
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
        Flow row = Flow.row()
            .widthRel(1f)
            .height(20)
            .marginBottom(2)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER);

        if (component instanceof StringFilterComponent) {
            row.child(stringFilterEditor((StringFilterComponent) component));
        } else if (component instanceof PropertyFilterComponent) {
            row.child(propertyEditor(cards, (PropertyFilterComponent) component));
        } else if (component instanceof SubTagFilterComponent) {
            row.child(subTagEditor(cards, (SubTagFilterComponent) component));
        } else {
            row.child(
                IKey.str(component.describe())
                    .asWidget()
                    .height(16)
                    .marginLeft(2));
        }

        // W/B 白黑名单放末尾、删除键前面
        row.child(whitelistButton(component).marginLeft(3));
        return row.child(deleteButton(() -> {
            if (index >= 0 && index < filters.size()) {
                filters.remove(index);
                rebuildFilterCards(cards);
            }
        }));
    }

    private IWidget stringFilterEditor(StringFilterComponent component) {
        return new TextFieldWidget().size(120, 16)
            .marginLeft(2)
            .setTextColor(0xFFFFFFFF)
            .value(new StringValue.Dynamic(component::getPattern, component::setPattern));
    }

    /** 属性过滤编辑：拖入示例物品(如橡胶) → 循环按钮只在该材料实际拥有的属性里切换。 */
    private IWidget propertyEditor(ListWidget<IWidget, ?> cards, PropertyFilterComponent component) {
        WildcardDropWidget drop = new WildcardDropWidget(
            () -> component.getExample() == null ? null : dustOf(component.getExample()),
            stack -> {
                WildcardMaterials.PrefixMaterial pm = WildcardMaterials.parseItem(stack);
                if (pm.material != null) {
                    component.setExample(pm.material);
                    rebuildFilterCards(cards); // 拖入后整体重建，卡片用新材料的属性集
                }
            });
        ButtonWidget<?> cycle = new ButtonWidget<>().size(100, 16)
            .marginLeft(3)
            .overlay(
                IKey.dynamic(
                    () -> component.getProperty() == null ? "?"
                        : component.getProperty()
                            .name()))
            .onMousePressed(button -> {
                component.setProperty(nextProperty(component));
                return true;
            });
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(drop)
            .child(cycle);
    }

    /** 示例材料的粉尘物品，用于在拖入槽里显示。 */
    private static ItemStack dustOf(Materials material) {
        return WildcardMaterials.makePrefixStack(OrePrefixes.dust, material, 1);
    }

    /** 下一个属性：优先在示例材料实际拥有的属性里循环，无示例时在全部属性里循环。 */
    private static WildcardMaterials.Property nextProperty(PropertyFilterComponent component) {
        java.util.List<WildcardMaterials.Property> pool = component.getExample() == null
            ? java.util.Arrays.asList(WildcardMaterials.Property.values())
            : WildcardMaterials.propertiesOf(component.getExample());
        if (pool.isEmpty()) return component.getProperty();
        int idx = pool.indexOf(component.getProperty());
        return pool.get((idx + 1) % pool.size());
    }

    /** SubTag 过滤编辑：拖入示例物品 → 循环按钮只在该材料实际拥有的 SubTag 里切换（对齐属性组件）。 */
    private IWidget subTagEditor(ListWidget<IWidget, ?> cards, SubTagFilterComponent component) {
        WildcardDropWidget drop = new WildcardDropWidget(
            () -> component.getExample() == null ? null : dustOf(component.getExample()),
            stack -> {
                WildcardMaterials.PrefixMaterial pm = WildcardMaterials.parseItem(stack);
                if (pm.material != null) {
                    component.setExample(pm.material);
                    rebuildFilterCards(cards);
                }
            });
        ButtonWidget<?> cycle = new ButtonWidget<>().size(100, 16)
            .marginLeft(3)
            .overlay(IKey.dynamic(() -> component.getSubTag() == null ? "?" : component.getSubTag().mName))
            .onMousePressed(button -> {
                component.setSubTag(nextSubTag(component));
                return true;
            });
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(drop)
            .child(cycle);
    }

    /** 下一个 SubTag：优先在示例材料实际拥有的 SubTag 里循环，无示例时不变。 */
    private static gregtech.api.enums.SubTag nextSubTag(SubTagFilterComponent component) {
        if (component.getExample() == null) return component.getSubTag();
        java.util.List<gregtech.api.enums.SubTag> pool = WildcardMaterials.subTagsOf(component.getExample());
        if (pool.isEmpty()) return component.getSubTag();
        int idx = pool.indexOf(component.getSubTag());
        return pool.get((idx + 1) % pool.size());
    }

    private ButtonWidget<?> whitelistButton(IWildcardFilterComponent component) {
        return new ButtonWidget<>().size(16)
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
        return new ButtonWidget<>().size(36, 16)
            .marginRight(2)
            .overlay(IKey.lang(langKey))
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> deleteButton(Runnable action) {
        return new ButtonWidget<>().size(14)
            .marginLeft(3)
            .overlay(IKey.str("x"))
            .onMousePressed(button -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> saveButton() {
        return new ButtonWidget<>().size(36, 16)
            // #tr gui.wildcardpattern.save
            // # Save
            // # zh_CN 保存
            .overlay(IKey.lang("gui.wildcardpattern.save"))
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
