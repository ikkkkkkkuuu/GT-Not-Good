package com.xyp.gtnotgood.common.blocks.stellar;

import com.xyp.gtnotgood.client.stellar.StellarConnectedTexture;

import gregtech.api.interfaces.ITexture;

public final class StellarCasingTextures {

    public static final ITexture EDDINGTON = new StellarConnectedTexture(
        StellarBlocks.getBlock(StellarBlockVariant.Group.Casing2),
        StellarBlockVariant.EddingtonWallCasing.getMetadata());

    private StellarCasingTextures() {}
}
