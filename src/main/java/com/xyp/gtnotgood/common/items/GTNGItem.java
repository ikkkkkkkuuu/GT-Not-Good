package com.xyp.gtnotgood.common.items;

import net.minecraft.item.Item;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

/**
 * Common registration defaults for ordinary items owned by this mod.
 * GT nuclear fuel cells cannot extend this class because their reactor behavior requires ItemRadioactiveCellIC.
 */
public class GTNGItem extends Item {

    /**
     * Sets the item name, icon, and creative tab. The caller still registers the item with Forge.
     *
     * @param name stable item registry and texture name
     */
    public GTNGItem(String name) {
        setUnlocalizedName(ModList.ModIds.GT_NOT_GOOD + "." + name);
        setTextureName(ModList.GTNotGood.getResourcePath(name));
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
    }
}
