package com.xyp.gtnotgood.ae2thing.loader;

import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.util.BaublesUtil;
import com.xyp.gtnotgood.ae2thing.util.InvUtil;

public class InvLoader implements Runnable {

    @Override
    public void run() {
        InvUtil.INVENTORY.add(player -> player.inventory);
        if (Mods.BAUBLES.isModLoaded()) {
            InvUtil.INVENTORY.add(BaublesUtil::getBaublesInv);
        }
    }
}
