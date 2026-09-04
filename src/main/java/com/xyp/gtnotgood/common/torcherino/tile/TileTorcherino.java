package com.xyp.gtnotgood.common.torcherino.tile;

/**
 * Standard area Torcherino tier with a 1x acceleration multiplier.
 */
public class TileTorcherino extends TileTorcherinoBase {

    @Override
    protected int getSpeedMultiplier() {
        return 1;
    }

    @Override
    protected String getGuiTitleKey() {
        // #tr gtnotgood.torcherino.gui.title
        // # Torcherino
        // # zh_CN 加速火把
        return "gtnotgood.torcherino.gui.title";
    }

    @Override
    protected String getGuiPanelId() {
        return "gtnotgood_torcherino";
    }
}
