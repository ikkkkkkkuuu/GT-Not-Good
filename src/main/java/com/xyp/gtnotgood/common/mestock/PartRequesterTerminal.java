package com.xyp.gtnotgood.common.mestock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

import appeng.parts.reporting.PartTerminal;

/** Native cable terminal geometry; paged requester access is authorized against the terminal's live network. */
public final class PartRequesterTerminal extends PartTerminal {

    public PartRequesterTerminal(ItemStack stack) {
        super(stack);
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, Vec3 pos) {
        if (player.isSneaking()) return false;
        if (!player.worldObj.isRemote) StockGuiFactory.instance.open(player, this);
        return true;
    }
}
