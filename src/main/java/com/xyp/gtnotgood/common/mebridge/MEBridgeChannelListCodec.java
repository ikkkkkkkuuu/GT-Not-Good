package com.xyp.gtnotgood.common.mebridge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** Delimiter-safe server-to-client snapshot for the receiver channel browser. */
final class MEBridgeChannelListCodec {

    private static final String RECORD_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "\u0001";

    private MEBridgeChannelListCodec() {}

    static String encode(List<MEBridgeChannelInfo> channels) {
        StringBuilder encoded = new StringBuilder();
        for (MEBridgeChannelInfo channel : channels) {
            if (channel.name == null || channel.name.isEmpty()) continue;
            int receiverCount = MEBridgeReceiverRegistry.count(channel.name);
            if (encoded.length() > 0) encoded.append(RECORD_SEPARATOR);
            encoded.append(encodeName(channel.name))
                .append(FIELD_SEPARATOR)
                .append(receiverCount > 0 ? '1' : '0')
                .append(FIELD_SEPARATOR)
                .append(channel.x)
                .append(FIELD_SEPARATOR)
                .append(channel.y)
                .append(FIELD_SEPARATOR)
                .append(channel.z)
                .append(FIELD_SEPARATOR)
                .append(channel.dim)
                .append(FIELD_SEPARATOR)
                .append(receiverCount);
        }
        return encoded.toString();
    }

    static List<Entry> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return Collections.emptyList();

        List<Entry> entries = new ArrayList<>();
        for (String record : encoded.split(RECORD_SEPARATOR, -1)) {
            String[] fields = record.split(FIELD_SEPARATOR, -1);
            if (fields.length != 7) continue;
            try {
                String name = decodeName(fields[0]);
                if (name.isEmpty()) continue;
                entries.add(
                    new Entry(
                        name,
                        "1".equals(fields[1]),
                        Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]),
                        Integer.parseInt(fields[5]),
                        Integer.parseInt(fields[6])));
            } catch (IllegalArgumentException ignored) {}
        }
        return entries;
    }

    private static String encodeName(String name) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeName(String encodedName) {
        return new String(
            Base64.getUrlDecoder()
                .decode(encodedName),
            StandardCharsets.UTF_8);
    }

    static final class Entry {

        final String name;
        final boolean online;
        final int x;
        final int y;
        final int z;
        final int dimension;
        final int receiverCount;

        Entry(String name, boolean online, int x, int y, int z, int dimension, int receiverCount) {
            this.name = name;
            this.online = online;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.receiverCount = receiverCount;
        }
    }
}
