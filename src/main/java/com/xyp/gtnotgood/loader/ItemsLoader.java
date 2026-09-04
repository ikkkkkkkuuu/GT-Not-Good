package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.common.items.VeinMiningPickaxe.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternItem;
import com.xyp.gtnotgood.common.mebridge.ItemMEWirelessTransceiver;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers ordinary Forge items owned by GT Not Good.
 */
public final class ItemsLoader {

    public static ItemMEWirelessTransceiver meWirelessTransceiver;
    public static WildcardPatternItem wildcardPattern;
    public static VeinMiningPickaxe veinMiningPickaxe;

    private ItemsLoader() {}

    /**
     * Registers all non-GregTech items and stores their stacks for recipes and tooltips.
     */
    public static void registry() {
        meWirelessTransceiver = new ItemMEWirelessTransceiver();
        GameRegistry.registerItem(meWirelessTransceiver, ItemMEWirelessTransceiver.ITEM_NAME);
        GTNGItemList.MEWirelessTransceiver.set(meWirelessTransceiver);

        wildcardPattern = new WildcardPatternItem();
        GameRegistry.registerItem(wildcardPattern, WildcardPatternItem.ITEM_NAME);
        GTNGItemList.WildcardPattern.set(wildcardPattern);
        // 注册矿脉挖掘镐
        veinMiningPickaxe = new VeinMiningPickaxe();
    }
}
