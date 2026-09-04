package com.xyp.gtnotgood.common.torcherino.tile;

/**
 * Double-compressed area Torcherino tier with an 81x acceleration multiplier.
 */
public class TileDoubleCompressedTorcherino extends TileTorcherinoBase {

    @Override
    protected int getSpeedMultiplier() {
        return 81;
    }

    @Override
    protected String getGuiTitleKey() {
        // #tr gtnotgood.torcherino.gui.double_compressed.title
        // # Double-Compressed Torcherino
        // # zh_CN 二重压缩加速火把
        return "gtnotgood.torcherino.gui.double_compressed.title";
    }

    @Override
    protected String getGuiPanelId() {
        return "gtnotgood_double_compressed_torcherino";
    }
}
