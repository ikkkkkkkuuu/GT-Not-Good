package com.xyp.gtnotgood.common.user;

import net.minecraft.item.Item;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Stackable mechanical-user accelerator; twenty upgrades reach one operation per tick. */
public final class ItemUserSpeedUpgrade extends Item {

    public ItemUserSpeedUpgrade() {
        // #tr item.mechanical_user_speed.name
        // # Mechanical User Speed Upgrade
        // # zh_CN 使用者速度升级
        setUnlocalizedName("mechanical_user_speed");
        setTextureName(ModList.GTNotGood.getResourcePath("mechanical_user_speed"));
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        setMaxStackSize(20);
    }
}
