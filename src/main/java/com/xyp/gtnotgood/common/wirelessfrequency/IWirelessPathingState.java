// SPDX-License-Identifier: LGPL-3.0-only

package com.xyp.gtnotgood.common.wirelessfrequency;

public interface IWirelessPathingState {

    boolean gtng$isRepathPending();

    /** Counts grid membership and wired connections; excludes frequency-card connections. */
    long gtng$topologyRevision();

    void gtng$topologyChanged();
}
