// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.utils.enums.ModList;

public final class HeartOfTheUniverseStructure {

    public static final int WIDTH = 121;
    public static final int HEIGHT = 112;
    public static final int LENGTH = 109;
    public static final int OFFSET_X = 62;
    public static final int OFFSET_Y = 88;
    public static final int OFFSET_Z = 15;
    public static final String SYMBOLS = "ABCDEFGHPRST";
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/heart_of_the_universe.mbs";
    private static final String[][] SHAPE = loadShape();

    private HeartOfTheUniverseStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    public static Block block(char symbol) {
        return switch (symbol) {
            case 'C' -> FieldExtruderCasing.HyperMechanical.getBlock();
            case 'F' -> DimensionalFocusCasing.FusionGlass.getBlock();
            case 'S' -> DimensionalFocusCasing.DimensionInjection.getBlock();
            default -> casing(symbol).getBlock();
        };
    }

    public static int metadata(char symbol) {
        return switch (symbol) {
            case 'C' -> FieldExtruderCasing.HyperMechanical.getMetadata();
            case 'F' -> DimensionalFocusCasing.FusionGlass.getMetadata();
            case 'S' -> DimensionalFocusCasing.DimensionInjection.getMetadata();
            default -> casing(symbol).getMetadata();
        };
    }

    public static <T> IStructureElement<T> element(char symbol) {
        return StructureUtility.ofBlock(block(symbol), metadata(symbol));
    }

    private static HeartCasing casing(char symbol) {
        return switch (symbol) {
            case 'A' -> HeartCasing.GravitonFieldConstraintCasing;
            case 'B', 'G' -> HeartCasing.AnnihilateCore;
            case 'D' -> HeartCasing.DimensionalStabilityCasing;
            case 'E' -> HeartCasing.TemporalAnchorFieldCasing;
            case 'H' -> HeartCasing.RheniumReinforcedEnergyGlass;
            case 'P' -> HeartCasing.DimensionConnectionCasing;
            case 'R' -> HeartCasing.DysonDeploymentMagnet;
            case 'T' -> HeartCasing.CreateCasing;
            default -> throw new IllegalArgumentException("Not a Heart casing symbol: " + symbol);
        };
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        int controllers = 0;
        try (InputStream stream = HeartOfTheUniverseStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing Heart structure: " + RESOURCE);
            try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
                if (readVarInt(data) != -1 || readVarInt(data) != 1)
                    throw new IOException("Unsupported Heart structure format");
                if (readVarInt(data) != 2 || readVarInt(data) != 0 || readVarInt(data) != 4)
                    throw new IOException("Unexpected Heart structure axes");
                if (readVarInt(data) != LENGTH) throw new IOException("Invalid Heart structure depth");
                for (int aisle = 0; aisle < LENGTH; aisle++) {
                    if (readVarInt(data) != HEIGHT) throw new IOException("Invalid Heart structure height");
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = data.readUTF();
                        if (source.length() != WIDTH) throw new IOException("Invalid Heart structure width");
                        for (int column = 0; column < WIDTH; column++) {
                            char symbol = source.charAt(column);
                            if (symbol == '~') controllers++;
                            else if (symbol != ' ' && SYMBOLS.indexOf(symbol) < 0)
                                throw new IOException("Invalid Heart structure symbol: " + symbol);
                        }
                        result[HEIGHT - row - 1][LENGTH - aisle - 1] = source;
                    }
                }
                if (data.read() != -1) throw new IOException("Unexpected Heart structure data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Heart structure", exception);
        }
        if (controllers != 1 || result[OFFSET_Y][OFFSET_Z].charAt(OFFSET_X) != '~')
            throw new IllegalStateException("Invalid Heart controller offset");
        return result;
    }

    private static int readVarInt(DataInputStream data) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = data.readUnsignedByte();
            value |= (next & 127) << shift;
            if ((next & 128) == 0) return value;
        }
        throw new IOException("Invalid Heart structure integer");
    }
}
