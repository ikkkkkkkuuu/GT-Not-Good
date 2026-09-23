package com.xyp.gtnotgood.common.items.fuel;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.ItemRadioactiveCellIC;

/**
 * An iron-clad GT nuclear fuel rod. GT and Fission Evolved discover it through ItemRadioactiveCellIC.
 * The GT superclass registers this item as part of construction, so it must not be registered a second time.
 */
public final class IronFuelRod extends ItemRadioactiveCellIC {

    private static final String NAME = "iron_fuel_rod";
    private IIcon icon;

    /**
     * Copies the cell count and radiation behavior of a native four-cell uranium rod while applying configured values.
     *
     * @param base     native four-cell uranium rod from the installed GregTech version
     * @param depleted custom spent rod returned when this rod expires
     */
    public IronFuelRod(ItemRadioactiveCellIC base, ItemStack depleted) {
        super(
            ModList.ModIds.GT_NOT_GOOD + "." + NAME,
            "Iron Fuel Rod",
            base.numberOfCells,
            scaledDuration(base.getMaxDamageEx(), Config.FuelRod.durationPercent),
            base.sEnergy * Config.FuelRod.energyPercent / 100F,
            base.sRadiation,
            base.sHeat * Config.FuelRod.heatPercent / 100F,
            depleted,
            base.sMox,
            base.sHeatBonus);
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
    }

    /**
     * Keeps configured cell lifetime within the integer range accepted by GT's reactor item.
     *
     * @param baseDuration native rod lifetime
     * @param percent      configured multiplier in percent
     * @return clamped positive lifetime
     */
    private static int scaledDuration(int baseDuration, int percent) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) baseDuration * percent / 100L));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon(ModList.GTNotGood.getResourcePath(NAME));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }
}
