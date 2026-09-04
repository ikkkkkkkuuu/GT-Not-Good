package com.xyp.gtnotgood.ae2thing.coremod.hooker;

import java.util.HashMap;
import java.util.List;

import net.minecraft.client.resources.I18n;

import com.glodblock.github.client.gui.GuiFluidInterface;
import com.xyp.gtnotgood.ae2thing.util.Util;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CoreModHooksClient {

    private static class ItemInfo {

        public String name;
        public String modId;
        public List<String> tooltip;

        public ItemInfo(String modId, String name, List<String> tooltip) {
            this.name = name;
            this.modId = modId;
            this.tooltip = tooltip;
        }
    }

    private static final HashMap<IAEItemStack, ItemInfo> cache = new HashMap<>();

    public static String getModId(final IAEItemStack is) {
        if (cache.containsKey(is) && cache.get(is).modId != null) {
            return cache.get(is).modId;
            // [液滴分类] 可迁原生：仅判定是否液滴以取其 modId 供 tooltip 显示,不参与合成
        } else if (FluidDropCompat.isFluidDrop(is.getItem())) {
            String id = Util.getModId(is);
            putCache(is, id, null, null);
            return id;
        } else {
            return Platform.getModId(is);
        }
    }

    public static String getItemDisplayName(final Object o) {
        if (o instanceof IAEItemStack is) {
            if (cache.containsKey(is) && cache.get(is).name != null) {
                return cache.get(is).name;
                // [液滴分类] 可迁原生：仅判定是否液滴以取其显示名,纯 GUI 显示不参与合成
            } else if (FluidDropCompat.isFluidDrop(is.getItem())) {
                String name = Util.getDisplayName(is);
                putCache(is, null, name, null);
                return name;
            }
        }
        return Platform.getItemDisplayName(o);
    }

    private static void putCache(IAEItemStack is, String modId, String name, List<String> tooltip) {
        if (!cache.containsKey(is)) {
            cache.putIfAbsent(is, new ItemInfo(modId, name, tooltip));
        } else {
            ItemInfo info = cache.get(is);
            if (info.modId == null) info.modId = modId;
            if (info.name == null) info.name = name;
            if (info.tooltip == null) info.tooltip = tooltip;
        }
    }

    public static List<String> getTooltip(final Object o) {
        if (o instanceof IAEItemStack is) {
            if (cache.containsKey(is) && cache.get(is).tooltip != null) {
                return cache.get(is).tooltip;
            }
        }

        return Platform.getTooltip(o);
    }

    public static String translateToLocal(String displayName, GuiFluidInterface dualInterface) {
        return I18n.format(displayName);
    }
}
