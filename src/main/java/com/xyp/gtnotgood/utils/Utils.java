package com.xyp.gtnotgood.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.enums.OutputBusType;
import gregtech.api.interfaces.IOutputBus;

/**
 * Miscellaneous utility helpers shared by migrated GT Not Good code.
 */
@SuppressWarnings("unused")
public class Utils {
    // endregion

    /**
     * Localize by key and given formats.
     * If the key does not exist in both of the currently used language and fallback language (English), the key itself
     * is returned.
     *
     * @param key    the localization key
     * @param format the formats
     * @return the localized text by the key and formats, or key if the key does not exist.
     * @see StatCollector#translateToLocalFormatted(String, Object...)
     */
    public static String tr(String key, Object... format) {
        return StatCollector.translateToLocalFormatted(key, format);
    }

    /**
     * Localize by the key.
     * If the key does not exist in both of the currently used language and fallback language (English), the key itself
     * is returned.
     *
     * @param key the localization key
     * @return the localized text by the key, or key if the key does not exist.
     * @see StatCollector#translateToLocal(String)
     */
    public static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    public static ArrayList<ItemStack> multiplyItemStacksSize(List<ItemStack> itemStacks, float mult,
        boolean splitStacks) {
        ArrayList<ItemStack> newItemStacks = new ArrayList<>(itemStacks.size() * 2);
        for (ItemStack is : itemStacks) {
            if (is == null) {
                continue;
            }
            is.stackSize = (int) (is.stackSize * mult);
            newItemStacks.add(is);
            // In case of stack size > max
            while (is.stackSize > (splitStacks ? 1 : is.getMaxStackSize())) {
                newItemStacks.add(is.splitStack(is.getMaxStackSize()));
            }
        }
        return newItemStacks;
    }

    public static void placeItemBackInInventory(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.stackSize == 0) return;

        if (!player.inventory.addItemStackToInventory(stack)) {
            player.func_146097_a(stack, false, false);
        } else if (stack.stackSize > 0) {
            player.func_146097_a(stack, false, false);
        }

        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
    }

    public static ArrayList<ItemStack> multiplyItemStacksSize(List<ItemStack> itemStacks, float mult) {
        return multiplyItemStacksSize(itemStacks, mult, false);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ItemStack newItemStack(Item aItem) {
        return new ItemStack(aItem, 1, 0);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ItemStack newItemStack(Block aBlock) {
        return new ItemStack(aBlock, 1, 0);
    }

    public static boolean isClientSide() {
        return FMLCommonHandler.instance()
            .getSide()
            .isClient();
    }

    public static boolean isServerSide() {
        return FMLCommonHandler.instance()
            .getSide()
            .isServer();
    }

    public static boolean isClientThreaded() {
        return FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient();
    }

    /**
     * 确保 NBT 中存在 UUID，如果不存在则生成一个新的
     *
     * @param aNBT NBT 标签
     * @return UUID 字符串
     */
    public static String ensureUUID(NBTTagCompound aNBT) {
        if (!aNBT.hasKey("storeUUID")) {
            aNBT.setString(
                "storeUUID",
                UUID.randomUUID()
                    .toString());
        }
        return aNBT.getString("storeUUID");
    }

    public static final String ZERO_STRING = "0";

    /**
     * 判断输出总线是否为 ME 类型
     */
    public static boolean isMEOutputBus(IOutputBus outputBus) {
        OutputBusType type = outputBus.getBusType();
        return type == OutputBusType.MECacheFiltered || type == OutputBusType.MEFiltered
            || type == OutputBusType.MECacheUnfiltered
            || type == OutputBusType.MEUnfiltered;
    }

    public static long toLongSafe(BigInteger value) {
        if (value == null) return 0L;
        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);
        if (value.compareTo(longMax) > 0) return Long.MAX_VALUE;
        BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);
        if (value.compareTo(longMin) < 0) return Long.MIN_VALUE;
        return value.longValue();
    }

    public static long toLongSafe(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0L;
        if (value > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (value < Long.MIN_VALUE) return Long.MIN_VALUE;
        return (long) value;
    }

    public static String shortFormat(long value) {
        if (value < 1000) return String.valueOf(value);
        if (value < 1_000_000) return String.format("%.1fK", value / 1000.0);
        if (value < 1_000_000_000L) return String.format("%.1fM", value / 1_000_000.0);
        if (value < 1_000_000_000_000L) return String.format("%.1fB", value / 1_000_000_000.0);
        return String.format("%.1fT", value / 1_000_000_000_000.0);
    }

    /** @deprecated identical to {@link #shortFormat(long)}; kept as an alias. */
    @Deprecated
    public static String formatNumbers(long value) {
        return shortFormat(value);
    }

}
