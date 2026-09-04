package com.xyp.gtnotgood.common.mebridge;

/** Shared validation rules for player-defined ME bridge channel names. */
public final class MEBridgeChannelName {

    public static final int MAX_LENGTH = 64;

    private MEBridgeChannelName() {}

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValid(String value) {
        return value != null && value.length() <= MAX_LENGTH;
    }
}
