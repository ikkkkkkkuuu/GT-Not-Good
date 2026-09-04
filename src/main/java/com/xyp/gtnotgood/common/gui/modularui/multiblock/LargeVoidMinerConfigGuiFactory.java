package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import net.minecraft.entity.player.EntityPlayer;

import com.xyp.gtnotgood.common.gui.modularui.PosMetaTileGuiFactory;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;

/**
 * ModularUI2 factory for the Large Void Miner configuration terminal.
 */
public final class LargeVoidMinerConfigGuiFactory {

    public static final PosMetaTileGuiFactory<LargeVoidMiner> INSTANCE = new PosMetaTileGuiFactory<>(
        "gtnotgood:lvm_config",
        LargeVoidMiner.class,
        LargeVoidMinerConfigGui::new,
        "Large Void Miner");

    private LargeVoidMinerConfigGuiFactory() {}

    public static void open(EntityPlayer player, LargeVoidMiner miner) {
        INSTANCE.openGui(player, miner);
    }
}
