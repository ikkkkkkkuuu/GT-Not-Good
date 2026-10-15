package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiCheckbox;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiTabButton;
import com.xyp.gtnotgood.client.radialmenu.client.icon.IconRenderer;
import com.xyp.gtnotgood.client.radialmenu.client.icon.PlayerHeadIcons;
import com.xyp.gtnotgood.client.radialmenu.client.icon.PotionIcons;
import com.xyp.gtnotgood.client.radialmenu.client.icon.SpriteAtlas;
import com.xyp.gtnotgood.client.radialmenu.client.icon.UserIconLoader;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;

public class GuiIconPicker extends UiScreen {

    public interface Callback {

        void onIconPicked(IconSpec icon);
    }

    private enum Tab {

        // #tr radialmenu.icons.items
        // # Items
        // # zh_CN 物品
        Items("radialmenu.icons.items"),
        // #tr radialmenu.icons.sprites
        // # Sprites
        // # zh_CN 精灵图
        Sprites("radialmenu.icons.sprites"),
        // #tr radialmenu.icons.effects
        // # Effects
        // # zh_CN 效果
        Effects("radialmenu.icons.effects"),
        // #tr radialmenu.icons.files
        // # My PNGs
        // # zh_CN 我的 PNG 文件
        Files("radialmenu.icons.files"),
        // #tr radialmenu.icons.players
        // # Heads
        // # zh_CN 玩家头像
        Players("radialmenu.icons.players");

        private final String labelKey;

        Tab(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private static final class Entry {

        final String id;
        final int meta;
        final String label;
        final String search;

        Entry(String id, int meta, String label) {
            this.id = id;
            this.meta = meta;
            this.label = label == null || label.isEmpty() ? id : label;
            this.search = (id + ' ' + this.label).toLowerCase();
        }
    }

    private static final int ID_TAB_BASE = 10;
    private static final int ID_COLOR = 20;
    private static final int ID_REFRESH = 21;
    private static final int ID_ORIGINAL = 22;

    private static final int CELL = 20;

    private static final String VANILLA = "minecraft";

    private static final int COLOR_WIDTH = 66;

    private static final int NAME_HEIGHT = 10;

    private final Callback callback;

    private Tab tab = Tab.Items;

    private final List<Entry> itemEntries = new ArrayList<>();
    private final List<Entry> visible = new ArrayList<>();

    private GuiTextField searchField;
    private String query = "";

    private String color = "";

    private boolean originalColors = true;

    private int gridTop;
    private int gridLeft;
    private int columns;
    private int scrollRow;

    public GuiIconPicker(Callback callback) {
        this(callback, null);
    }

    /** @param currentColor tint of the icon being replaced, so re-picking does not silently reset it to white */
    public GuiIconPicker(Callback callback, String currentColor) {
        this.callback = callback;
        if (currentColor != null && !currentColor.trim().isEmpty()) {
            this.color = currentColor;
        }
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.editor.pickIcon
        // # Pick an icon
        // # zh_CN 选择图标
        return "radialmenu.editor.pickIcon";
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    @Override
    protected void buildControls() {
        if (itemEntries.isEmpty()) {
            loadItems();
        }

        int left = contentLeft();
        int y = contentTop();

        int refreshLeft = contentRight() - Ui.ROW;
        int colorLeft = refreshLeft - Ui.GAP - COLOR_WIDTH;
        int searchWidth = colorLeft - Ui.GAP - left;

        searchField = new GuiTextField(this.fontRendererObj, left + 1, y + 3, searchWidth - 2, 14);
        searchField.setMaxStringLength(64);
        searchField.setText(query);
        searchField.setCursorPositionEnd();
        searchField.setFocused(true);

        GuiButton colorButton = new GuiButton(ID_COLOR, colorLeft, y, COLOR_WIDTH, Ui.ROW, "");
        // Only sprites and untinted-by-choice PNGs take a colour; an item or an effect carries its own.
        colorButton.enabled = tab == Tab.Sprites || (tab == Tab.Files && !originalColors);
        this.buttonList.add(colorButton);
        // #tr radialmenu.icons.color.tip
        // # Tint applied to sprites and your own PNGs. Items keep their own colors.
        // # zh_CN 应用于精灵图和自定义 PNG 的颜色滤镜。物品会保留自身颜色。
        tooltip(ID_COLOR, I18n.format("radialmenu.icons.color.tip"));
        this.buttonList.add(new GuiButton(ID_REFRESH, refreshLeft, y, Ui.ROW, Ui.ROW, "R"));
        // #tr radialmenu.icons.refresh.tip
        // # Re-scan the icons folder for PNG files you just added.
        // # zh_CN 重新扫描图标文件夹，查找刚添加的 PNG 文件。
        tooltip(ID_REFRESH, I18n.format("radialmenu.icons.refresh.tip"));

        y += Ui.STEP;

        Tab[] tabs = Tab.values();
        int tabWidth = (contentWidth() - (tabs.length - 1) * 2) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            this.buttonList.add(new UiTabButton(ID_TAB_BASE + i, left + i * (tabWidth + 2), y, tabWidth,
                I18n.format(tabs[i].labelKey), tabs[i] == tab));
        }

        int afterTabs = y + UiTabButton.HEIGHT;

        if (tab == Tab.Files) {
            this.buttonList.add(new UiCheckbox(ID_ORIGINAL, left, afterTabs + Ui.GAP, contentWidth(),
                // #tr radialmenu.icons.original
                // # Keep the image's own colors
                // # zh_CN 保持图片原始颜色
                I18n.format("radialmenu.icons.original"), originalColors));
            // #tr radialmenu.icons.original.tip
            // # Draw your PNG exactly as it was saved, ignoring the tint chosen for sprites.
            // # zh_CN 按照 PNG 文件保存时的样子绘制，忽略为精灵图选择的颜色滤镜。
            tooltip(ID_ORIGINAL, I18n.format("radialmenu.icons.original.tip"));
            afterTabs += Ui.STEP;
        }

        // The grid frame starts exactly where the tabs end, so the selected one runs into it.
        gridTop = afterTabs;
        gridLeft = left;
        columns = Math.max(1, contentWidth() / CELL);

        refilter();
        addBottomBar("gui.cancel", null, null);
    }

    /** The hovered name is drawn below the grid, so it gets a line of its own rather than the panel's padding. */
    @Override
    protected int footerHeight() {
        return NAME_HEIGHT;
    }

    private int gridBottom() {
        return viewportBottom();
    }

    private void loadItems() {
        List<String> names = new ArrayList<>();
        for (Object key : Item.itemRegistry.getKeys()) {
            names.add(String.valueOf(key));
        }

        Collections.sort(names, new Comparator<String>() {

            @Override
            public int compare(String left, String right) {
                int byMod = compareMods(domainOf(left), domainOf(right));
                return byMod != 0 ? byMod : registryId(left) - registryId(right);
            }
        });

        for (String name : names) {
            Item item = (Item) Item.itemRegistry.getObject(name);
            if (item == null) {
                continue;
            }
            for (int meta : metasOf(item)) {
                itemEntries.add(new Entry(name, meta, displayName(item, meta)));
            }
        }
    }

    private static List<Integer> metasOf(Item item) {
        List<ItemStack> stacks = new ArrayList<>();
        CreativeTabs[] tabs = null;
        try {
            tabs = item.getCreativeTabs();
        } catch (Throwable broken) {
            // Same class of failure as below: an override that assumes more than a GUI can give it.
        }
        if (tabs == null || tabs.length == 0) {
            // A null tab is what vanilla's own default is called with, and it yields the plain meta-0 stack.
            tabs = new CreativeTabs[] { null };
        }

        for (CreativeTabs creativeTab : tabs) {
            try {
                item.getSubItems(item, creativeTab, stacks);
            } catch (Throwable broken) {
                // getSubItems can reach into world or config state the picker has no business having; the rest of
                // the registry is still worth listing.
            }
        }

        List<Integer> metas = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (ItemStack stack : stacks) {
            if (stack != null && seen.add(stack.getItemDamage())) {
                metas.add(stack.getItemDamage());
            }
        }
        if (metas.isEmpty()) {
            metas.add(0);
        }
        return metas;
    }

    /** Resolved once, here, because the name comes from the item's own code and that code can throw. */
    private static String displayName(Item item, int meta) {
        try {
            return new ItemStack(item, 1, meta).getDisplayName();
        } catch (Throwable broken) {
            return "";
        }
    }

    private static String domainOf(String name) {
        int colon = name.indexOf(':');
        return colon < 0 ? "" : name.substring(0, colon);
    }

    /** Vanilla leads, because it is what a player reaches for first and what they can name from memory. */
    private static int compareMods(String left, String right) {
        boolean leftVanilla = VANILLA.equals(left);
        boolean rightVanilla = VANILLA.equals(right);
        if (leftVanilla != rightVanilla) {
            return leftVanilla ? -1 : 1;
        }
        return left.compareToIgnoreCase(right);
    }

    private static int registryId(String name) {
        Item item = (Item) Item.itemRegistry.getObject(name);
        return item == null ? Integer.MAX_VALUE : Item.getIdFromItem(item);
    }

    private int rowsVisible() {
        return Math.max(1, (gridBottom() - gridTop - 2) / CELL);
    }

    private void refilter() {
        String needle = query.trim().toLowerCase();
        visible.clear();

        switch (tab) {
            case Items:
                for (Entry entry : itemEntries) {
                    if (matches(entry, needle)) {
                        visible.add(entry);
                    }
                }
                break;
            case Sprites:
                for (SpriteAtlas.Sprite sprite : SpriteAtlas.search(needle)) {
                    visible.add(new Entry(sprite.name, 0, null));
                }
                break;
            case Effects:
                for (String name : PotionIcons.list()) {
                    // Matched on the translated name as well: a player looking for Speed should not have to know
                    // the game calls it potion.moveSpeed.
                    Entry entry = new Entry(name, 0, I18n.format(name));
                    if (matches(entry, needle)) {
                        visible.add(entry);
                    }
                }
                break;
            case Files:
                for (String name : UserIconLoader.listFiles()) {
                    Entry entry = new Entry(name, 0, null);
                    if (matches(entry, needle)) {
                        visible.add(entry);
                    }
                }
                break;
            case Players:
                fillPlayers(needle);
                break;
            default:
                break;
        }
        clampScroll();
    }

    private void fillPlayers(String needle) {
        Set<String> seen = new HashSet<>();
        List<String> names = new ArrayList<>(PlayerHeadIcons.listLoaded());
        names.addAll(PlayerHeadIcons.listCached());

        String typed = query.trim();
        if (!typed.isEmpty()) {
            visible.add(new Entry(typed, 0, null));
            seen.add(typed.toLowerCase());
        }
        for (String name : names) {
            if (!seen.add(name.toLowerCase())) {
                continue;
            }
            Entry entry = new Entry(name, 0, null);
            if (matches(entry, needle)) {
                visible.add(entry);
            }
        }
    }

    private static boolean matches(Entry entry, String needle) {
        return needle.isEmpty() || entry.search.contains(needle);
    }

    private void clampScroll() {
        int maxRow = Math.max(0, (visible.size() + columns - 1) / columns - rowsVisible());
        scrollRow = Math.max(0, Math.min(maxRow, scrollRow));
    }

    private String hoverLabel(Entry entry) {
        switch (tab) {
            case Items:
                return entry.label + " (" + entry.id + (entry.meta != 0 ? "#" + entry.meta : "") + ")";
            case Effects:
                return entry.label;
            default:
                return entry.id;
        }
    }

    /** What to say when a tab has nothing in it. */
    private String emptyMessage() {
        switch (tab) {
            case Files:
                // #tr radialmenu.icons.noFiles
                // # Place PNGs in %s, then press R.
                // # zh_CN 将 PNG 放入 %s，再按 R 刷新。
                return I18n.format("radialmenu.icons.noFiles", ProfileStorage.iconsDir().getPath());
            case Players:
                // #tr radialmenu.icons.noPlayers
                // # Type a player name, or stand near someone to learn their head
                // # zh_CN 输入玩家名称，或站在某人附近以获取其头像
                return I18n.format("radialmenu.icons.noPlayers");
            default:
                // #tr radialmenu.icons.noMatches
                // # Nothing matches that search
                // # zh_CN 没有匹配项
                return I18n.format("radialmenu.icons.noMatches");
        }
    }

    /** Builds the spec for an entry of the current tab, so drawing and picking cannot disagree about it. */
    private IconSpec specFor(Entry entry) {
        switch (tab) {
            case Sprites:
                return IconSpec.sprite(SpriteAtlas.qualify(entry.id), color);
            case Effects:
                return IconSpec.effect(entry.id);
            case Players:
                return IconSpec.player(entry.id);
            case Files:
                IconSpec file = IconSpec.file(entry.id);
                // Null means untinted, which is how the artwork's own colours survive.
                file.color = originalColors ? null : color;
                return file;
            case Items:
            default:
                return IconSpec.item(entry.id, entry.meta);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            onCancel();
            return;
        }
        if (button.id == ID_COLOR) {
            GuiStack.push(new GuiColorPicker(color, new GuiColorPicker.Result() {

                @Override
                public void onColorPicked(String hex) {
                    color = hex;
                    requestRebuild();
                }
            }));
            return;
        }
        if (button.id == ID_ORIGINAL) {
            ((UiCheckbox) button).toggle();
            originalColors = ((UiCheckbox) button).checked;
            requestRebuild();
            return;
        }
        if (button.id == ID_REFRESH) {
            // Picks up a PNG just dropped into the folder, without restarting the game.
            UserIconLoader.refresh();
            requestRebuild();
            return;
        }

        int tabIndex = button.id - ID_TAB_BASE;
        Tab[] tabs = Tab.values();
        if (tabIndex >= 0 && tabIndex < tabs.length) {
            tab = tabs[tabIndex];
            scrollRow = 0;
            requestRebuild();
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        searchField.drawTextBox();
        Ui.list(gridLeft, gridTop, contentRight(), gridBottom());

        Entry hovered = null;
        int rows = rowsVisible();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = (row + scrollRow) * columns + column;
                if (index >= visible.size()) {
                    break;
                }
                int x = gridLeft + 2 + column * CELL;
                int y = gridTop + 2 + row * CELL;

                if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                    drawRect(x, y, x + CELL, y + CELL, Ui.ROW_HOVER);
                    hovered = visible.get(index);
                }
                IconRenderer.draw(specFor(visible.get(index)), x + 2, y + 2);
            }
        }

        if (visible.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.GRAY + emptyMessage(), this.width / 2,
                gridTop + 8, Ui.TEXT);
        }

        if (hovered != null) {
            this.drawCenteredString(this.fontRendererObj, Ui.fit(hoverLabel(hovered), contentWidth()), this.width / 2,
                gridBottom() + Ui.GAP, 0xFFFFFF80);
        }
    }

    @Override
    protected void drawOverlay(int mouseX, int mouseY, float partialTicks) {
        for (Object raw : this.buttonList) {
            GuiButton button = (GuiButton) raw;
            if (button.id != ID_COLOR) {
                continue;
            }
            int swatchTop = button.yPosition + 4;
            boolean inherited = color.trim().isEmpty();

            int shown = inherited ? IconRenderer.resolveTint(null) : IconSpec.parseRgb(color, 0xFFFFFF);
            Ui.frame(button.xPosition + 4, swatchTop, button.xPosition + 16, swatchTop + 12, 0xFF000000 | shown,
                0xFF000000);

            String label = inherited ? I18n.format("radialmenu.editor.inherit") : color;
            this.fontRendererObj.drawString(Ui.fit(label, button.width - 24), button.xPosition + 20,
                button.yPosition + (Ui.ROW - 8) / 2, button.enabled ? Ui.TEXT : Ui.TEXT_MUTED);
            return;
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollRow += wheel > 0 ? -1 : 1;
            clampScroll();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!isShowing()) {
            return;
        }
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }
        int rows = rowsVisible();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = (row + scrollRow) * columns + column;
                if (index >= visible.size()) {
                    return;
                }
                int x = gridLeft + 2 + column * CELL;
                int y = gridTop + 2 + row * CELL;
                if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                    callback.onIconPicked(specFor(visible.get(index)));
                    GuiStack.pop();
                    return;
                }
            }
        }
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            query = searchField.getText();
            scrollRow = 0;
            refilter();
            return true;
        }
        return false;
    }
}
