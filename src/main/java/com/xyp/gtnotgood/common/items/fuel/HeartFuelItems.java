package com.xyp.gtnotgood.common.items.fuel;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.registry.GameRegistry;

public final class HeartFuelItems {

    private HeartFuelItems() {}

    public static void init() {
        // #tr item.gtnotgood.strange_annihilation_fuel_rod.name
        // # Strange Annihilation Fuel Rod
        // # zh_CN 奇异湮灭燃料棒
        register("strange_annihilation_fuel_rod", GTNGItemList.StrangeAnnihilationFuelRod, true);
        // #tr item.gtnotgood.black_hole_seed.name
        // # Black Hole Seed
        // # zh_CN 黑洞种子
        register("black_hole_seed", GTNGItemList.BlackHoleSeed, true);
        // #tr item.gtnotgood.annihilation_constrainer.name
        // # Annihilation Constrainer
        // # zh_CN 湮灭约束器
        register("annihilation_constrainer", GTNGItemList.AnnihilationConstrainer, false);
    }

    private static void register(String name, GTNGItemList entry, boolean fuel) {
        HeartFuelItem item = new HeartFuelItem(name, fuel);
        GameRegistry.registerItem(item, name);
        entry.set(item);
    }

    private static final class HeartFuelItem extends GTNGItem {

        private final boolean fuel;

        private HeartFuelItem(String name, boolean fuel) {
            super(name);
            this.fuel = fuel;
            setTextureName(ModList.GTNotGood.getResourcePath("heart/" + name));
        }

        @Override
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            if (fuel) {
                // #tr gtng.heart.fuel.description
                // # Consumed by the Heart of the Universe
                // # zh_CN 由宇宙之心消耗
                tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtng.heart.fuel.description"));
            } else {
                // #tr gtng.heart.constrainer.description
                // # Byproduct of the Heart of the Universe
                // # zh_CN 宇宙之心的副产物
                tooltip.add(
                    EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtng.heart.constrainer.description"));
            }
        }
    }
}
