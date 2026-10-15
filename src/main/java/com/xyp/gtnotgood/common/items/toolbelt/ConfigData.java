package com.xyp.gtnotgood.common.items.toolbelt;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.common.registry.GameRegistry;

public class ConfigData {

    public static Set<String> whitelist = new HashSet<>();
    public static Set<String> blacklist = new HashSet<>();
    public static boolean allowAllNonStackableItems = true;

    public static boolean releaseToSwap = true;
    public static boolean allowClickOutsideBounds = true;
    public static boolean displayEmptySlots = true;
    public static boolean minecraftHasNoCircles = false;
    public static float radialDeadzoneOffset = 8.0f;

    static {
        syncFromMainConfig();
    }

    public static void syncFromMainConfig() {
        releaseToSwap = Config.releaseToSwap;
        allowClickOutsideBounds = Config.allowClickOutsideBounds;
        displayEmptySlots = Config.displayEmptySlots;
        minecraftHasNoCircles = Config.minecraftHasNoCircles;
        radialDeadzoneOffset = Config.radialDeadzoneOffset;
    }

    public static boolean isItemStackAllowed(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return false;

        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        String itemName = id != null ? id.toString() : null;
        if (itemName == null) return false;

        if (whitelist.contains(itemName)) return true;
        if (blacklist.contains(itemName)) return false;
        return stack.getMaxStackSize() > 1 || allowAllNonStackableItems && stack.getMaxStackSize() == 1;
    }

    public static boolean isItemStackAllowed(ItemStack stack, Set<String> customWhitelist,
        Set<String> customBlacklist) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return false;

        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        String itemName = id != null ? id.toString() : null;
        if (itemName == null) return false;

        if (customWhitelist.contains(itemName) || whitelist.contains(itemName)) return true;

        if (customBlacklist.contains(itemName) || blacklist.contains(itemName)) return false;

        return stack.getMaxStackSize() > 1 || allowAllNonStackableItems && stack.getMaxStackSize() == 1;
    }
}
