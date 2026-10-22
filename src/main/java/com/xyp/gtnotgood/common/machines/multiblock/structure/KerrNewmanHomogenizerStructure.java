// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.xyp.gtnotgood.utils.enums.ModList;

// GTOCore 3121e23241e7f6ccd2e6a8dbd5655962de63ed9a; META-INF/kerr-newman-port/NOTICE.md.
public final class KerrNewmanHomogenizerStructure {

    public static final int WIDTH = 83;
    public static final int HEIGHT = 36;
    public static final int LENGTH = 83;
    public static final int OFFSET_X = 41;
    public static final int OFFSET_Y = 12;
    public static final int OFFSET_Z = 7;
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/kerr_newman_homogenizer.mbs";
    private static final String[][] SHAPE = loadShape();

    private KerrNewmanHomogenizerStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        try (InputStream stream = KerrNewmanHomogenizerStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing Kerr-Newman structure: " + RESOURCE);
            try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
                if (readVarInt(data) != -1 || readVarInt(data) != 1)
                    throw new IOException("Unsupported Kerr-Newman structure format");
                if (readVarInt(data) != 2 || readVarInt(data) != 0 || readVarInt(data) != 4)
                    throw new IOException("Unexpected Kerr-Newman structure axes");
                if (readVarInt(data) != LENGTH) throw new IOException("Invalid Kerr-Newman structure depth");
                for (int aisle = 0; aisle < LENGTH; aisle++) {
                    if (readVarInt(data) != HEIGHT) throw new IOException("Invalid Kerr-Newman structure height");
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = data.readUTF();
                        if (source.length() != WIDTH) throw new IOException("Invalid Kerr-Newman structure width");
                        for (int column = 0; column < WIDTH; column++) {
                            char symbol = source.charAt(column);
                            if (symbol != ' ' && (symbol < 'A' || symbol > 'b'))
                                throw new IOException("Invalid Kerr-Newman structure symbol: " + symbol);
                        }
                        result[HEIGHT - row - 1][LENGTH - aisle - 1] = source.replace('b', '~');
                    }
                }
                if (data.read() != -1) throw new IOException("Unexpected Kerr-Newman structure data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Kerr-Newman structure", exception);
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
        throw new IOException("Invalid Kerr-Newman structure integer");
    }
}
