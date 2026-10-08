package com.xyp.gtnotgood.common.blocks.mebridge;

/** Result of a server-authoritative sender channel change. */
public enum MEBridgeChannelChangeResult {

    Success,
    InvalidName,
    ChannelOccupied,
    NotServerSide;

    public boolean isSuccess() {
        return this == Success;
    }
}
