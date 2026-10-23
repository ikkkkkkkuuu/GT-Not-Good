// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.xyp.gtnotgood.utils.enums.ModList;

// GTOCore 156835a0e66a90cbc4e60c3e95a36c35212b97d6; META-INF/nyarlathoteps-tentacle-port/NOTICE.md.
public final class NyarlathotepsTentacleStructure {

    public static final int WIDTH = 165;
    public static final int HEIGHT = 48;
    public static final int LENGTH = 49;
    public static final int OFFSET_X = 82;
    public static final int OFFSET_Y = 23;
    public static final int OFFSET_Z = 0;
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/nyarlathoteps_tentacle.mbs";
    private static final String[][] SHAPE = loadShape();

    private NyarlathotepsTentacleStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        try (InputStream stream = NyarlathotepsTentacleStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing Tentacle structure: " + RESOURCE);
            try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
                if (readVarInt(data) != -1 || readVarInt(data) != 1)
                    throw new IOException("Unsupported Tentacle structure format");
                if (readVarInt(data) != 2 || readVarInt(data) != 0 || readVarInt(data) != 4)
                    throw new IOException("Unexpected Tentacle structure axes");
                if (readVarInt(data) != LENGTH) throw new IOException("Invalid Tentacle structure depth");
                for (int aisle = 0; aisle < LENGTH; aisle++) {
                    if (readVarInt(data) != HEIGHT) throw new IOException("Invalid Tentacle structure height");
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = data.readUTF();
                        if (source.length() != WIDTH) throw new IOException("Invalid Tentacle structure width");
                        for (int column = 0; column < WIDTH; column++) {
                            char symbol = source.charAt(column);
                            if (symbol != ' ' && symbol != '~' && (symbol < 'A' || symbol > 'a'))
                                throw new IOException("Invalid Tentacle structure symbol: " + symbol);
                        }
                        result[HEIGHT - row - 1][LENGTH - aisle - 1] = source.replace('~', 'b').replace('a', '~');
                    }
                }
                if (data.read() != -1) throw new IOException("Unexpected Tentacle structure data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Tentacle structure", exception);
        }
        return result;
    }

    private static int readVarInt(DataInputStream data) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = data.readUnsignedByte();
            value |= (next & 127) << shift;
            if ((next & 128) == 0) return value;
        }
        throw new IOException("Invalid Tentacle structure integer");
    }
}
