package com.xyp.gtnotgood.common.torcherino.tile;

/**
 * Double-compressed wireless Torcherino tier with an 81x acceleration multiplier.
 */
public class TileDoubleCompressedWirelessTorcherino extends TileWirelessTorcherinoBase {

    @Override
    protected int getSpeedMultiplier() {
        return 81;
    }

    @Override
    protected String getGuiTitleKey() {
        // #tr gtnotgood.torcherino.gui.wireless_double_compressed.title
        // # Double-Compressed Wireless Torcherino
        // # zh_CN 二重压缩无线加速火把
        return "gtnotgood.torcherino.gui.wireless_double_compressed.title";
    }

    @Override
    protected String getGuiPanelId() {
        return "gtnotgood_double_compressed_wireless_torcherino";
    }
}
