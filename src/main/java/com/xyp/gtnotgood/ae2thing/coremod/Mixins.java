package com.xyp.gtnotgood.ae2thing.coremod;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.xyp.gtnotgood.ae2thing.integration.Mods;

public enum Mixins implements IMixins {

    AeClient(new MixinBuilder()
        .addClientMixins("ae.AccessorGuiScrollbar", "ae.AccessorContainerMEMonitorable", "ae.MixinAEBaseGui",
            "ae.MixinContainerCraftConfirm", "ae.MixinGuiCraftAmount", "ae.MixinGuiCraftConfirm",
            "ae.MixinGuiCraftingTerm", "ae.MixinItemRepo", "ae.MixinContainerCraftAmount")
        .addRequiredMod(Mods.AE2).setPhase(Phase.LATE)),

    AeServer(
        new MixinBuilder()
            .addCommonMixins("ae.MixinContainerCraftConfirm", "ae.MixinCraftingCPUClusterFollow",
                "ae.MixinPacketSwitchGuis", "ae.MixinContainerCraftAmount")
            .addRequiredMod(Mods.AE2).setPhase(Phase.LATE)),

    NEI(new MixinBuilder()
        .addClientMixins("nei.MixinGuiContainerManager", "nei.MixinGuiOverlayButton", "nei.MixinIOverlayHandler",
            "nei.MixinPanelWidget", "nei.MixinRecipeItemInputHandler")
        .addRequiredMod(Mods.NotEnoughItems).setPhase(Phase.LATE)),

    BR(new MixinBuilder().addClientMixins("br.MixinBRUtil").addRequiredMod(Mods.NotEnoughItems)
        .addRequiredMod(Mods.BlockRenderer).setPhase(Phase.LATE));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
