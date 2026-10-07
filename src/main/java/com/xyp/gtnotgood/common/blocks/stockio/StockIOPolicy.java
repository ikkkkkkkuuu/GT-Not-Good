package com.xyp.gtnotgood.common.blocks.stockio;

import net.minecraft.nbt.NBTTagCompound;

/** ME reserves remain long quantities; a single machine input is bounded by Forge's stack amount. */
public final class StockIOPolicy {

    public long reserve;
    public int batch;

    public StockIOPolicy(boolean fluid) {
        batch = fluid ? 1000 : 64;
    }

    public int offered(long stored, boolean limited, boolean fixed) {
        long available = Math.max(0, Math.max(0, stored) - (limited ? Math.max(0, reserve) : 0));
        int amount = Math.max(1, batch);
        return fixed ? (available >= amount ? amount : 0) : (int) Math.min(Integer.MAX_VALUE, available);
    }

    void write(NBTTagCompound tag) {
        tag.setLong("reserve", Math.max(0, reserve));
        tag.setInteger("batch", Math.max(1, batch));
    }

    void read(NBTTagCompound tag) {
        reserve = Math.max(0, tag.getLong("reserve"));
        if (tag.hasKey("batch")) batch = Math.max(1, tag.getInteger("batch"));
    }
}
