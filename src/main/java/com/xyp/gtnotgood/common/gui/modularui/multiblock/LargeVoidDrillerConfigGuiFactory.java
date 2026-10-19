package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import net.minecraft.entity.player.EntityPlayer;

import com.xyp.gtnotgood.common.gui.modularui.PosMetaTileGuiFactory;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidDriller;
import com.xyp.gtnotgood.utils.enums.ModList;

public final class LargeVoidDrillerConfigGuiFactory {

    public static final PosMetaTileGuiFactory<LargeVoidDriller> INSTANCE = new PosMetaTileGuiFactory<>(
        ModList.GTNotGood.getID() + ":lvd_config", LargeVoidDriller.class, LargeVoidDrillerConfigGui::new,
        "Large Void Driller");

    private LargeVoidDrillerConfigGuiFactory() {}

    public static void open(EntityPlayer player, LargeVoidDriller driller) {
        INSTANCE.openGui(player, driller);
    }
}
