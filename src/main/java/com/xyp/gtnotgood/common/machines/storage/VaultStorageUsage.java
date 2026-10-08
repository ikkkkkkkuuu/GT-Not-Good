package com.xyp.gtnotgood.common.machines.storage;

import java.math.BigInteger;

/**
 * Tracks the shared item/fluid byte cost without walking the vault on each AE simulation.
 * Keeps the unclamped total so extracting from an over-capacity legacy save cannot create false free space.
 */
public final class VaultStorageUsage {

    private final BigInteger capacity;
    private final int bytesPerType;
    private BigInteger usedBytes = BigInteger.ZERO;

    public VaultStorageUsage(long capacity, int bytesPerType) {
        this.capacity = BigInteger.valueOf(capacity);
        this.bytesPerType = bytesPerType;
    }

    public long usedBytes() {
        return usedBytes.min(capacity).longValue();
    }

    public void clear() {
        usedBytes = BigInteger.ZERO;
    }

    /** Returns the accepted quantity without changing the ledger, including space in a partially used byte. */
    public long insertableAmount(long currentAmount, long requestedAmount, int amountPerByte) {
        if (requestedAmount <= 0) return 0;
        long freeBytes = capacity.longValue() - usedBytes();
        if (currentAmount <= 0) freeBytes -= bytesPerType;
        if (freeBytes < 0) return 0;
        long unusedAmount = currentAmount <= 0 ? 0 : (amountPerByte - currentAmount % amountPerByte) % amountPerByte;
        long byteLimitedAmount = freeBytes > Long.MAX_VALUE / amountPerByte ? Long.MAX_VALUE
            : freeBytes * amountPerByte;
        byteLimitedAmount = byteLimitedAmount > Long.MAX_VALUE - unusedAmount ? Long.MAX_VALUE
            : byteLimitedAmount + unusedAmount;
        return Math.min(requestedAmount, Math.min(byteLimitedAmount, Long.MAX_VALUE - currentAmount));
    }

    /**
     * Records one committed quantity change; zero quantity releases the type overhead as well.
     * Simulation must not call this method. Loading uses a transition from zero for each merged stored type.
     *
     * @param before        previous stored quantity
     * @param after         new stored quantity
     * @param amountPerByte units fitting in one byte, for the relevant storage channel
     */
    public void update(long before, long after, int amountPerByte) {
        usedBytes = usedBytes.subtract(cost(before, amountPerByte)).add(cost(after, amountPerByte));
    }

    private BigInteger cost(long amount, int amountPerByte) {
        if (amount <= 0) return BigInteger.ZERO;
        long dataBytes = amount / amountPerByte + (amount % amountPerByte == 0 ? 0 : 1);
        return BigInteger.valueOf(dataBytes).add(BigInteger.valueOf(bytesPerType));
    }
}
