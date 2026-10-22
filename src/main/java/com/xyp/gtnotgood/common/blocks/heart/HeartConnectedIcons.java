package com.xyp.gtnotgood.common.blocks.heart;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class HeartConnectedIcons {

    private static final Map<HeartCasing, IIcon[]> icons = new EnumMap<>(HeartCasing.class);

    private HeartConnectedIcons() {}

    public static void register(IIconRegister register, HeartCasing variant) {
        IIcon[] connected = new IIcon[256];
        for (int mask = 0; mask < connected.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) connected[mask] = register
                .registerIcon(ModList.GTNotGood.getResourcePath("heart/ctm/" + variant.getTextureName() + "/" + mask));
        }
        for (int mask = 0; mask < connected.length; mask++)
            connected[mask] = connected[StellarConnections.normalizeMask(mask)];
        icons.put(variant, connected);
    }

    public static IIcon getIcon(HeartCasing variant, int mask) {
        IIcon[] connected = icons.get(variant);
        return connected == null ? null : connected[mask & 255];
    }
}
