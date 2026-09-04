package com.xyp.gtnotgood.ae2thing.quickterminal;

import java.io.IOException;

import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import io.netty.buffer.ByteBuf;

/** Identifies one pattern slot in AE2's interface-terminal entry list. */
public final class InterfacePatternTarget {

    public static final StreamCodec<InterfacePatternTarget> CODEC = StreamCodecs
        .of(InterfacePatternTarget.class.getName(), InterfacePatternTarget::write, InterfacePatternTarget::read);

    private final long entryId;
    private final int slot;

    public InterfacePatternTarget(long entryId, int slot) {
        this.entryId = entryId;
        this.slot = slot;
    }

    public long getEntryId() {
        return entryId;
    }

    public int getSlot() {
        return slot;
    }

    private static void write(ByteBuf buffer, InterfacePatternTarget target) throws IOException {
        buffer.writeLong(target.entryId);
        buffer.writeInt(target.slot);
    }

    private static InterfacePatternTarget read(ByteBuf buffer) throws IOException {
        return new InterfacePatternTarget(buffer.readLong(), buffer.readInt());
    }
}
