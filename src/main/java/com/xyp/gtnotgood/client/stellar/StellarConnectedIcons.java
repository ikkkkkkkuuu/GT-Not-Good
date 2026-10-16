package com.xyp.gtnotgood.client.stellar;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class StellarConnectedIcons {

    private static final Map<String, IIcon[]> icons = new HashMap<>();

    private StellarConnectedIcons() {}

    public static void register(IIconRegister register, String texture) {
        IIcon[] variants = new IIcon[256];
        for (int mask = 0; mask < variants.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) {
                String path = texture.equals("sl_eddington") ? "eddington_" + mask : texture + "/" + mask;
                variants[mask] = register.registerIcon(ModList.GTNotGood.getResourcePath("stellar/ctm/" + path));
            }
        }
        for (int mask = 0; mask < variants.length; mask++) {
            variants[mask] = variants[StellarConnections.normalizeMask(mask)];
        }
        icons.put(texture, variants);
    }

    public static IIcon getIcon(String texture, int mask) {
        IIcon[] variants = icons.get(texture);
        return variants == null ? null : variants[mask & 255];
    }
}
