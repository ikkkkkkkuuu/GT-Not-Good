// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.api.stacks;

public final class AEKeyType {

    private final String id;
    private final int amountPerUnit;
    private final int amountPerByte;
    private final boolean fuzzyRangeSearch;

    public AEKeyType(String id, int amountPerUnit, int amountPerByte, boolean fuzzyRangeSearch) {
        this.id = id;
        this.amountPerUnit = Math.max(1, amountPerUnit);
        this.amountPerByte = Math.max(1, amountPerByte);
        this.fuzzyRangeSearch = fuzzyRangeSearch;
    }

    public String getId() {
        return this.id;
    }

    public int getAmountPerUnit() {
        return this.amountPerUnit;
    }

    public int getAmountPerOperation() {
        return 1;
    }

    public int getAmountPerByte() {
        return this.amountPerByte;
    }

    public boolean supportsFuzzyRangeSearch() {
        return this.fuzzyRangeSearch;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
