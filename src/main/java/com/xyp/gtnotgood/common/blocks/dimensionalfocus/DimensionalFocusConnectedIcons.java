package com.xyp.gtnotgood.common.blocks.dimensionalfocus;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DimensionalFocusConnectedIcons {

    private static final Map<DimensionalFocusCasing, IIcon[]> icons = new EnumMap<>(DimensionalFocusCasing.class);
    private static IIcon[] activeIcons;

    private DimensionalFocusConnectedIcons() {}

    public static void register(IIconRegister register, DimensionalFocusCasing variant) {
        icons.put(variant, register(register, variant.getTextureName()));
    }

    public static void registerActive(IIconRegister register) {
        activeIcons = register(register, "improved_superconductor_coil_active");
    }

    private static IIcon[] register(IIconRegister register, String name) {
        IIcon[] connected = new IIcon[256];
        for (int mask = 0; mask < connected.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) {
                connected[mask] = register
                    .registerIcon(ModList.GTNotGood.getResourcePath("dimensionalfocus/ctm/" + name + "/" + mask));
            }
        }
        for (int mask = 0; mask < connected.length; mask++) {
            connected[mask] = connected[StellarConnections.normalizeMask(mask)];
        }
        return connected;
    }

    public static IIcon getIcon(DimensionalFocusCasing variant, int mask) {
        return getIcon(variant, mask, false);
    }

    public static IIcon getIcon(DimensionalFocusCasing variant, int mask, boolean active) {
        IIcon[] connected = active && variant == DimensionalFocusCasing.ImprovedSuperconductor ? activeIcons
            : icons.get(variant);
        return connected == null ? null : connected[mask & 255];
    }
}
