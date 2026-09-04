package com.xyp.gtnotgood.common.items.toolbelt;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 工具带配置数据类
 * 提供物品黑白名单和配置检查功能
 * 实际配置值从主 Config 类读取
 */
public class ConfigData {

    public static Set<String> whitelist = new HashSet<>();
    public static Set<String> blacklist = new HashSet<>();
    public static boolean allowAllNonStackableItems = true;

    // 从主 Config 类引用配置值（使用字段而非方法）
    public static boolean releaseToSwap = true;
    public static boolean allowClickOutsideBounds = true;
    public static boolean displayEmptySlots = true;
    public static boolean minecraftHasNoCircles = false;
    public static float radialDeadzoneOffset = 8.0f;

    static {
        // 从主 Config 同步配置值
        syncFromMainConfig();
    }

    /**
     * 从主 Config 类同步配置值
     */
    public static void syncFromMainConfig() {
        releaseToSwap = Config.releaseToSwap;
        allowClickOutsideBounds = Config.allowClickOutsideBounds;
        displayEmptySlots = Config.displayEmptySlots;
        minecraftHasNoCircles = Config.minecraftHasNoCircles;
        radialDeadzoneOffset = Config.radialDeadzoneOffset;
    }

    /**
     * 检查物品是否允许放入腰带
     */
    public static boolean isItemStackAllowed(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;

        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        String itemName = id != null ? id.toString() : null;
        if (itemName == null) return false;

        // Whitelist takes priority
        if (whitelist.contains(itemName)) return true;

        // Check blacklist
        if (blacklist.contains(itemName)) return false;

        // Default: allow non-stackable items
        if (allowAllNonStackableItems && stack.getMaxStackSize() == 1) return true;

        return false;
    }

    public static boolean isItemStackAllowed(ItemStack stack, Set<String> customWhitelist,
        Set<String> customBlacklist) {
        if (stack == null || stack.getItem() == null) return false;

        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        String itemName = id != null ? id.toString() : null;
        if (itemName == null) return false;

        if (customWhitelist.contains(itemName) || whitelist.contains(itemName)) return true;

        if (customBlacklist.contains(itemName) || blacklist.contains(itemName)) return false;

        if (allowAllNonStackableItems && stack.getMaxStackSize() == 1) return true;

        return false;
    }
}
