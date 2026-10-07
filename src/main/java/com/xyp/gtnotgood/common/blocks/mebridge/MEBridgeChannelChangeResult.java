package com.xyp.gtnotgood.common.blocks.mebridge;

/** Result of a server-authoritative sender channel change. */
public enum MEBridgeChannelChangeResult {

    SUCCESS,
    INVALID_NAME,
    CHANNEL_OCCUPIED,
    NOT_SERVER_SIDE;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
