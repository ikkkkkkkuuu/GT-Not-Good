package com.xyp.gtnotgood.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.compat.VirtualMachineMolds;
import com.xyp.gtnotgood.common.items.patternsorter.PatternSorter;
import com.xyp.gtnotgood.common.items.patternsorter.PatternSorterGui;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.gui.texture.ItemStackTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.Button;
import com.xyp.ldlib.gui.ui.elements.Label;
import com.xyp.ldlib.gui.ui.elements.SearchComponent;
import com.xyp.ldlib.gui.ui.elements.VirtualScrollerView;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Native LDLib search and grouped preview above the synchronized player inventory. */
@SideOnly(Side.CLIENT)
public final class PatternSorterView extends UIElement {

    private final PatternSorterGui model;
    private final ModernTheme theme = new ModernTheme(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
    private final VirtualScrollerView<PatternSorter.Entry> groups, patterns;
    private final SearchComponent<String> mapSelector;
    private final Button sort, front;
    private List<PatternSorter.Entry> snapshot = Collections.emptyList();
    private String selected = "", lastMap = "";
    private final Map<String, Integer> counts = new LinkedHashMap<>();

    public PatternSorterView(PatternSorterGui model) {
        super(0, 0, 420, 217);
        this.model = model;
        addChild(new Label(8, 5, 404, 16, tr("item.pattern_sorter.name")));
        // #tr gui.pattern_sorter.machine
        // # Machine / recipe map
        // # zh_CN 机器／配方类型
        addChild(new Label(8, 26, 112, 20, tr("gui.pattern_sorter.machine")));
        mapSelector = new SearchComponent<>(124, 26, 286, 20, theme, PatternSorterView::tr);
        List<String> maps = PatternSorterGui.maps();
        maps.sort(Comparator.comparing(PatternSorterView::tr));
        mapSelector.setCandidates(maps).setOnChange(model.actions::chooseMap);
        addChild(mapSelector);
        // #tr gui.pattern_sorter.groups
        // # Configuration groups
        // # zh_CN 电路／模具分组
        addChild(new Label(8, 50, 186, 16, tr("gui.pattern_sorter.groups")));
        // #tr gui.pattern_sorter.preview
        // # Patterns in selected group
        // # zh_CN 当前分组的样板
        addChild(new Label(202, 50, 208, 16, tr("gui.pattern_sorter.preview")));
        groups = new VirtualScrollerView<>(8, 68, 186, 98, 32,
            (entry, index) -> theme.button(0, 0, 178, 31,
                () -> (entry.group().equals(selected) ? "> " : "") + groupName(entry)
                    + " ×"
                    + counts.getOrDefault(entry.group(), 0),
                () -> {
                    selected = entry.group();
                    updatePatterns();
                }));
        groups.setThumbTexture(theme.scrollThumb);
        addChild(groups);
        patterns = new VirtualScrollerView<>(202, 68, 208, 98, 24, this::patternRow);
        patterns.setThumbTexture(theme.scrollThumb);
        addChild(patterns);
        // #tr gui.pattern_sorter.sort
        // # Sort all groups
        // # zh_CN 按组整理
        sort = theme.button(8, 172, 186, 20, () -> tr("gui.pattern_sorter.sort"), () -> model.actions.sort(""));
        addChild(sort);
        // #tr gui.pattern_sorter.front
        // # Bring this group to front
        // # zh_CN 此组排到前面
        front = theme.button(202, 172, 208, 20, () -> tr("gui.pattern_sorter.front"),
            () -> model.actions.sort(selected));
        addChild(front);
        // #tr gui.pattern_sorter.hint
        // # Only pattern slots move. Preview numbers follow inventory order.
        // # zh_CN 仅交换样板位置；编号对应背包格序，快捷栏排最后。
        addChild(new Label(8, 197, 404, 16, tr("gui.pattern_sorter.hint")));
    }

    private UIElement patternRow(PatternSorter.Entry entry, int index) {
        UIElement row = new UIElement(0, 0, 198, 24);
        ItemStack icon = entry.stack;
        String name = entry.stack.getDisplayName();
        try {
            ICraftingPatternDetails pattern = ((ICraftingPatternItem) entry.stack.getItem())
                .getPatternForItem(entry.stack.copy(), Minecraft.getMinecraft().theWorld);
            if (pattern != null) for (IAEStack<?> output : pattern.getAEOutputs()) {
                if (output instanceof IAEItemStack item) {
                    icon = item.getItemStack();
                    name = icon.getDisplayName();
                    break;
                } else if (output instanceof IAEFluidStack fluid) {
                    name = fluid.getFluidStack().getLocalizedName();
                    break;
                }
            }
        } catch (RuntimeException ignored) {
            // Malformed previews still show the original pattern and remain removable through real slots.
        }
        row.addChild(new UIElement(0, 4, 16, 16).setBackground(new ItemStackTexture(icon)));
        row.addChild(new Label(18, 0, 180, 24, "#" + ((entry.slot + 27) % 36 + 1) + " " + name));
        return row;
    }

    private void updatePatterns() {
        patterns
            .setItems(snapshot.stream().filter(entry -> entry.group().equals(selected)).collect(Collectors.toList()));
    }

    @Override
    public void tick() {
        String map = model.selectedMap.getValue();
        if (map != null && !map.equals(lastMap)) {
            lastMap = map;
            mapSelector.setValue(map);
        }
        List<PatternSorter.Entry> latest = model.entries.getValue();
        if (latest != null && !snapshot.equals(latest)) {
            snapshot = new ArrayList<>(latest);
            counts.clear();
            Map<String, PatternSorter.Entry> representatives = new LinkedHashMap<>();
            for (PatternSorter.Entry entry : snapshot) {
                counts.merge(entry.group(), entry.stack.stackSize, Integer::sum);
                representatives.putIfAbsent(entry.group(), entry);
            }
            if (!representatives.containsKey(selected)) {
                selected = representatives.isEmpty() ? "" : representatives.keySet().iterator().next();
            }
            groups.setItems(new ArrayList<>(representatives.values()));
            updatePatterns();
        }
        boolean ready = !lastMap.isEmpty() && !snapshot.isEmpty()
            && Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null;
        sort.setEnabled(ready);
        front.setEnabled(ready && !selected.isEmpty());
        super.tick();
    }

    private static String groupName(PatternSorter.Entry entry) {
        switch (entry.status) {
            case PatternSorter.AMBIGUOUS:
                // #tr gui.pattern_sorter.ambiguous
                // # Ambiguous configuration
                // # zh_CN 有歧义，需核对
                return tr("gui.pattern_sorter.ambiguous");
            case PatternSorter.UNMATCHED:
                // #tr gui.pattern_sorter.unmatched
                // # Unmatched / unsupported
                // # zh_CN 未匹配／不支持
                return tr("gui.pattern_sorter.unmatched");
            case PatternSorter.CRAFTING:
                // #tr gui.pattern_sorter.crafting
                // # Crafting-table patterns
                // # zh_CN 工作台合成样板
                return tr("gui.pattern_sorter.crafting");
            case PatternSorter.INVALID:
                // #tr gui.pattern_sorter.invalid
                // # Invalid / blank patterns
                // # zh_CN 无效／空白样板
                return tr("gui.pattern_sorter.invalid");
            default:
                break;
        }
        // #tr gui.pattern_sorter.none
        // # None
        // # zh_CN 无
        String none = tr("gui.pattern_sorter.none");
        ItemStack mold = VirtualMachineMolds.at(entry.mold);
        // #tr gui.pattern_sorter.configuration
        // # Circuit %s / %s
        // # zh_CN 电路 %s／%s
        return StatCollector.translateToLocalFormatted("gui.pattern_sorter.configuration",
            entry.circuit < 0 ? none : Integer.toString(entry.circuit), mold == null ? none : mold.getDisplayName());
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }
}
