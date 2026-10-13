// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class WirelessCardSnapshot {

    public static final int CHUNK_BYTES = 24 * 1024;

    private final NBTTagCompound payload;
    private final Map<Long, IWirelessLinkView> targets;

    WirelessCardSnapshot(NBTTagCompound payload, Map<Long, IWirelessLinkView> targets) {
        this.payload = (NBTTagCompound) payload.copy();
        this.targets = Collections.unmodifiableMap(new HashMap<>(targets));
    }

    IWirelessLinkView target(long id) {
        return targets.get(id);
    }

    boolean sameView(WirelessCardSnapshot previous) {
        return previous != null && payload.equals(previous.payload);
    }

    NBTTagCompound packet(long session, long revision, NBTTagList highlight) {
        var data = (NBTTagCompound) payload.copy();
        data.setLong("session", session);
        data.setLong("revision", revision);
        if (highlight != null) data.setTag("highlight", highlight.copy());
        return data;
    }
}
