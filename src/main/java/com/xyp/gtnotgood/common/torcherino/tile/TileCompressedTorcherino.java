package com.xyp.gtnotgood.common.torcherino.tile;

/**
 * Compressed area Torcherino tier with a 9x acceleration multiplier.
 */
public class TileCompressedTorcherino extends TileTorcherinoBase {

    @Override
    protected int getSpeedMultiplier() {
        return 9;
    }

    @Override
    protected String getGuiTitleKey() {
        // #tr gtnotgood.torcherino.gui.compressed.title
        // # Compressed Torcherino
        // # zh_CN 压缩加速火把
        return "gtnotgood.torcherino.gui.compressed.title";
    }

    @Override
    protected String getGuiPanelId() {
        return "gtnotgood_compressed_torcherino";
    }
}
