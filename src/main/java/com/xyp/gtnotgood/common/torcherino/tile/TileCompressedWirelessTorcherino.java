package com.xyp.gtnotgood.common.torcherino.tile;

/**
 * Compressed wireless Torcherino tier with a 9x acceleration multiplier.
 */
public class TileCompressedWirelessTorcherino extends TileWirelessTorcherinoBase {

    @Override
    protected int getSpeedMultiplier() {
        return 9;
    }

    @Override
    protected String getGuiTitleKey() {
        // #tr gtnotgood.torcherino.gui.wireless_compressed.title
        // # Compressed Wireless Torcherino
        // # zh_CN 压缩无线加速火把
        return "gtnotgood.torcherino.gui.wireless_compressed.title";
    }

    @Override
    protected String getGuiPanelId() {
        return "gtnotgood_compressed_wireless_torcherino";
    }
}
