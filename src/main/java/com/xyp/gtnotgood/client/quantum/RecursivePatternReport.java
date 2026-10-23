// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.client.quantum;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;

import com.google.common.collect.ImmutableList;
import com.xyp.gtnotgood.client.quantum.CraftingCycleIndex.Match;
import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder.Cycle;
import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder.PatternLocation;

import appeng.api.storage.data.IAEStack;
import appeng.api.util.NamedDimensionalCoord;
import appeng.client.render.highlighter.BlockPosHighlighter;
import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Shows the crafting cycles of a request in the chat and highlights the machines holding their patterns.
 */
public final class RecursivePatternReport {

    private static final int MAX_LISTED_ITEMS = 6;
    private static final int MAX_HIGHLIGHTS = 32;

    // Highlights the cycle of what is under the mouse in the crafting tree, or every cycle
    public static final KeyBinding HIGHLIGHT = new KeyBinding(
        // #tr key.gtng.highlight_recursive_patterns
        // # Highlight Recursive Patterns
        // # zh_CN 高亮递归样板所在机器
        "key.gtng.highlight_recursive_patterns", Keyboard.KEY_H,
        // #tr key.categories.gtng
        // # Science Not Leisure
        // # zh_CN Science Not Leisure
        "key.categories.gtng");

    public static void register() {
        ClientRegistry.registerKeyBinding(HIGHLIGHT);
    }

    public static boolean isHighlightKey(int key) {
        return key != Keyboard.KEY_NONE && key == HIGHLIGHT.getKeyCode();
    }

    public static String highlightKeyName() {
        return GameSettings.getKeyDisplayString(HIGHLIGHT.getKeyCode());
    }

    /**
     * Every cycle, highlighting the machines of those without net output when there are any, since they are what
     * stops a calculation.
     */
    public static void show(Minecraft minecraft, IAEStack<?> request, List<Cycle> cycles) {
        var player = minecraft.thePlayer;
        if (player == null) return;
        if (cycles.isEmpty()) {
            player
                // #tr gtng.recursive_patterns.none
                // # §b[Recursive Patterns]§r %s: no crafting cycle found.
                // # zh_CN §b[递归样板]§r %s：没有找到合成循环。
                .addChatMessage(new ChatComponentTranslation("gtng.recursive_patterns.none", request.getDisplayName()));
            return;
        }
        boolean anyWithoutNet = false;
        for (var cycle : cycles) {
            anyWithoutNet |= !cycle.netOutput();
        }
        player.addChatMessage(
            // #tr gtng.recursive_patterns.header
            // # §b[Recursive Patterns]§r %s: %s crafting cycles found.
            // # zh_CN §b[递归样板]§r %s：找到 %s 个合成循环。
            new ChatComponentTranslation("gtng.recursive_patterns.header", request.getDisplayName(), cycles.size()));
        var highlights = new LinkedHashMap<NamedDimensionalCoord, String[]>();
        for (int i = 0; i < cycles.size(); i++) {
            var cycle = cycles.get(i);
            list(player, i + 1, cycle);
            if (!cycle.netOutput() || !anyWithoutNet) collect(highlights, cycle.patterns());
        }
        highlight(player, highlights, request);
    }

    /**
     * The cycles the plan cannot start because of, told once when the calculation is done.
     */
    public static void showBlocking(Minecraft minecraft, CraftingCycleIndex index) {
        var player = minecraft.thePlayer;
        if (player == null || index.blocking().isEmpty()) return;
        player.addChatMessage(new ChatComponentTranslation(
            // #tr gtng.recursive_patterns.blocked
            // # §c[Cycles]§r %s: %s blocking cycles highlighted. Hover another and press %s.
            // # zh_CN §c[循环]§r %s：已高亮 %s 个阻塞循环。悬停其他循环并按 %s 可查看。
            "gtng.recursive_patterns.blocked", index.request().getDisplayName(), index.blocking().size(),
            highlightKeyName()));
        var highlights = new LinkedHashMap<NamedDimensionalCoord, String[]>();
        for (int i = 0; i < index.cycles().size(); i++) {
            var cycle = index.cycles().get(i);
            if (!index.blocking().contains(cycle)) continue;
            list(player, i + 1, cycle);
            collect(highlights, cycle.patterns());
        }
        highlight(player, highlights, index.request());
    }

    /**
     * The machines of the patterns picked in the crafting tree.
     */
    public static void highlight(Minecraft minecraft, IAEStack<?> request, List<Match> matches) {
        var player = minecraft.thePlayer;
        if (player == null || matches.isEmpty()) return;
        var cycles = new LinkedHashSet<Integer>();
        var highlights = new LinkedHashMap<NamedDimensionalCoord, String[]>();
        for (var match : matches) {
            cycles.add(match.number());
            collect(highlights, ImmutableList.of(match.location()));
        }
        var names = new StringBuilder();
        for (var match : matches) {
            if (names.length() > 0) names.append(", ");
            names.append(match.location().output().getDisplayName());
        }
        player.addChatMessage(new ChatComponentTranslation(
            // #tr gtng.recursive_patterns.highlighted
            // # §b[Recursive Patterns]§r Cycle %1$s: %3$s machines highlighted (%2$s)
            // # zh_CN §b[递归样板]§r 循环 %1$s：已高亮 %3$s 台机器（%2$s）
            "gtng.recursive_patterns.highlighted", cycles.toString(), names.toString(), highlights.size()));
        highlight(player, highlights, request);
    }

    private static void list(EntityClientPlayerMP player, int number, Cycle cycle) {
        player.addChatMessage(new ChatComponentTranslation(
            // #tr gtng.recursive_patterns.cycle.net
            // # §a Cycle %s (net output)§r: %s
            // # zh_CN §a 循环 %s（有净产出）§r：%s
            // #tr gtng.recursive_patterns.cycle.no_net
            // # §c Cycle %s (no net output)§r: %s
            // # zh_CN §c 循环 %s（无净产出）§r：%s
            cycle.netOutput() ? "gtng.recursive_patterns.cycle.net" : "gtng.recursive_patterns.cycle.no_net", number,
            items(cycle.items())));
        for (var pattern : cycle.patterns()) {
            player.addChatMessage(new ChatComponentTranslation(
                // #tr gtng.recursive_patterns.pattern
                // # §7 Pattern §e%s§7: %s machines
                // # zh_CN §7 样板 §e%s§7：%s 台机器
                "gtng.recursive_patterns.pattern", pattern.output().getDisplayName(), pattern.machines().size()));
        }
    }

    private static void collect(Map<NamedDimensionalCoord, String[]> highlights, List<PatternLocation> patterns) {
        for (var pattern : patterns) {
            for (var machine : pattern.machines()) {
                if (highlights.size() >= MAX_HIGHLIGHTS) return;
                highlights.put(new NamedDimensionalCoord(machine, pattern.output().getDisplayName()),
                    // #tr gtng.recursive_patterns.found
                    // # §7 %2$s (cycle for %1$s) at %3$s, %4$s, %5$s
                    // # zh_CN §7 %2$s（%1$s 的循环样板）位于 %3$s, %4$s, %5$s
                    // #tr gtng.recursive_patterns.wrong_dim
                    // # §7 %2$s (cycle for %1$s) in dimension %3$s
                    // # zh_CN §7 %2$s（%1$s 的循环样板）在维度 %3$s
                    new String[] { "gtng.recursive_patterns.found", "gtng.recursive_patterns.wrong_dim" });
            }
        }
    }

    private static void highlight(EntityClientPlayerMP player,
        LinkedHashMap<NamedDimensionalCoord, String[]> highlights, IAEStack<?> request) {
        if (highlights.isEmpty()) {
            // #tr gtng.recursive_patterns.no_machines
            // # §7 No machines found for these patterns.
            // # zh_CN §7 没有找到存放这些样板的机器。
            player.addChatMessage(new ChatComponentTranslation("gtng.recursive_patterns.no_machines"));
            return;
        }
        BlockPosHighlighter.highlightNamedBlocks(player, highlights, request.getDisplayName());
    }

    private static String items(List<IAEStack<?>> items) {
        var text = new StringBuilder();
        for (int i = 0; i < items.size() && i < MAX_LISTED_ITEMS; i++) {
            if (i > 0) text.append(EnumChatFormatting.GRAY).append(" ⇄ ").append(EnumChatFormatting.RESET);
            text.append(items.get(i).getDisplayName());
        }
        if (items.size() > MAX_LISTED_ITEMS) {
            text.append(" (+").append(items.size() - MAX_LISTED_ITEMS).append(')');
        }
        return text.toString();
    }
}
