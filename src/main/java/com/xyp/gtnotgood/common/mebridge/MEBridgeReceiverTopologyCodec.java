package com.xyp.gtnotgood.common.mebridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class MEBridgeReceiverTopologyCodec {

    private static final String RECORD_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = ":";

    private MEBridgeReceiverTopologyCodec() {}

    static String encode(Map<Integer, Integer> counts) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (encoded.length() > 0) encoded.append(RECORD_SEPARATOR);
            encoded.append(entry.getKey())
                .append(FIELD_SEPARATOR)
                .append(entry.getValue());
        }
        return encoded.toString();
    }

    static List<Entry> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return Collections.emptyList();
        List<Entry> entries = new ArrayList<>();
        for (String record : encoded.split(RECORD_SEPARATOR, -1)) {
            String[] fields = record.split(FIELD_SEPARATOR, -1);
            if (fields.length != 2) continue;
            try {
                entries.add(new Entry(Integer.parseInt(fields[0]), Integer.parseInt(fields[1])));
            } catch (NumberFormatException ignored) {}
        }
        return entries;
    }

    static final class Entry {

        final int dimension;
        final int receiverCount;

        Entry(int dimension, int receiverCount) {
            this.dimension = dimension;
            this.receiverCount = receiverCount;
        }
    }
}
