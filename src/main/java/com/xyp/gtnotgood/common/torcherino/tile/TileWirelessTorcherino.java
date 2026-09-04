package com.xyp.gtnotgood.common.torcherino.tile;

/**
 * Standard wireless Torcherino tier with a 1x acceleration multiplier.
 */
public class TileWirelessTorcherino extends TileWirelessTorcherinoBase {

    @Override
    protected int getSpeedMultiplier() {
        return 1;
    }

    @Override
    protected String getGuiTitleKey() {
        // #tr gtnotgood.torcherino.gui.wireless.title
        // # Wireless Torcherino
        // # zh_CN 无线加速火把
        return "gtnotgood.torcherino.gui.wireless.title";
    }

    @Override
    protected String getGuiPanelId() {
        return "gtnotgood_wireless_torcherino";
    }
}
