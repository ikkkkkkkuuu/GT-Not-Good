package com.xyp.gtnotgood.common.gui.modularui.wildcard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardExpansion;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.Materials;

/**
 * Source Wildcard Pattern index page: a 158x80 preview with two 3x2 slot groups.
 * Advances once per 20 active client updates. Slots are display-only and never consume items.
 * Refresh receives a committed preview snapshot, so editing another tab does not mutate it before saving.
 */
public final class WildcardIndexPage extends ParentWidget<WildcardIndexPage> {

    private List<WildcardExpansion.Expanded> allPatterns = Collections.emptyList();
    private List<WildcardExpansion.Expanded> patterns = Collections.emptyList();
    private String search = "";
    private int ticks, index;

    public WildcardIndexPage(Consumer<Materials> excludeMaterial) {
        size(158, 80);
        child(
            // #tr gui.wildcardpattern.available_count
            // # %s Patterns Available
            // # zh_CN %s 个可用样板
            IKey.dynamic(
                () -> IKey.lang("gui.wildcardpattern.available_count", patterns.size())
                    .get())
                .asWidget()
                .pos(2, 2)
                .size(136, 15));
        child(
            WildcardPatternGui.THEME.button()
                .pos(140, 2)
                .size(14)
                .overlay(
                    UITexture.builder()
                        .location(ModList.ModIds.GT_NOT_GOOD, "gui/ldlib/modern/close")
                        .build())
                .onUpdateListener(button -> button.setEnabled(currentMaterial() != null))
                .tooltipBuilder(
                    tooltip -> tooltip.addLine(
                        // #tr gui.wildcardpattern.exclude_current
                        // # Exclude the current material and save
                        // # zh_CN 排除当前材料并保存到黑名单
                        IKey.lang("gui.wildcardpattern.exclude_current")))
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    Materials material = currentMaterial();
                    if (material != null) excludeMaterial.accept(material);
                    return true;
                }));
        for (int i = 0; i < 6; i++) {
            final int slot = i;
            child(new WildcardIconWidget(() -> stack(true, slot)).pos(10 + i % 3 * 18, 18 + i / 3 * 18));
            child(new WildcardIconWidget(() -> stack(false, slot)).pos(94 + i % 3 * 18, 18 + i / 3 * 18));
        }
        child(
            UITexture.builder()
                .location(ModList.ModIds.GT_NOT_GOOD, "gui/ldlib/modern/progress_bar_arrow")
                .imageSize(20, 40)
                .subAreaXYWH(0, 0, 20, 20)
                .build()
                .asWidget()
                .pos(74, 32)
                .size(15, 10));
        child(
            // #tr gui.wildcardpattern.preview_search
            // # Search
            // # zh_CN 搜索
            IKey.lang("gui.wildcardpattern.preview_search")
                .asWidget()
                .pos(2, 61)
                .size(28, 12));
        child(
            WildcardPatternGui.THEME.textField()
                .pos(32, 59)
                .size(124, 16)
                .autoUpdateOnChange(true)
                .value(new StringValue.Dynamic(() -> search, text -> {
                    search = text == null ? "" : text;
                    filterPatterns();
                })));
    }

    public void refresh(List<WildcardExpansion.Expanded> snapshot) {
        allPatterns = new ArrayList<>(snapshot);
        filterPatterns();
    }

    /**
     * Searches internal and client-localized material names in the cached snapshot.
     * Typing never reruns recipe expansion or changes item data.
     */
    private void filterPatterns() {
        String query = search.trim()
            .toLowerCase(Locale.ROOT);
        List<WildcardExpansion.Expanded> matches = new ArrayList<>();
        for (WildcardExpansion.Expanded pattern : allPatterns) {
            Materials material = pattern.material;
            if (query.isEmpty() || (material != null && (material.mName.toLowerCase(Locale.ROOT)
                .contains(query)
                || material.getLocalizedName()
                    .toLowerCase(Locale.ROOT)
                    .contains(query))))
                matches.add(pattern);
        }
        patterns = matches;
        ticks = 0;
        index = 0;
    }

    private ItemStack stack(boolean input, int slot) {
        if (patterns.isEmpty()) return null;
        WildcardExpansion.Expanded pattern = patterns.get(index);
        List<ItemStack> stacks = input ? pattern.inputs : pattern.outputs;
        return slot < stacks.size() ? stacks.get(slot) : null;
    }

    private Materials currentMaterial() {
        return patterns.isEmpty() ? null : patterns.get(index).material;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (areAncestorsEnabled() && !patterns.isEmpty() && ++ticks >= 20) {
            ticks = 0;
            index = (index + 1) % patterns.size();
        }
    }
}
