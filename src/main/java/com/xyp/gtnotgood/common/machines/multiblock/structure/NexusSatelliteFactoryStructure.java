// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.BlockLightHunterCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.utils.enums.ModList;

public final class NexusSatelliteFactoryStructure {

    public static final int WIDTH = 5;
    public static final int HEIGHT = 21;
    public static final int LENGTH = 13;
    public static final int OFFSET_X = 2;
    public static final int OFFSET_Y = 10;
    public static final int OFFSET_Z = 0;
    public static final String SYMBOLS = "ABCDEFGHIJKLMNO";
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/light_hunter_space_station_module.mbs";
    private static final String[][] SHAPE = loadShape();

    private NexusSatelliteFactoryStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    public static Block block(char symbol) {
        return switch (symbol) {
            case 'A' -> LightHunterCasing.EchoCasing.getBlock();
            case 'B' -> LightHunterCasing.UxvMachineCasing.getBlock();
            case 'C' -> KerrNewmanCasing.AtomicCasing.getBlock();
            case 'D' -> LightHunterCasing.DragonStrengthTritaniumCasing.getBlock();
            case 'E' -> LightHunterCasing.HyperCore.getBlock();
            case 'F' -> DimensionalFocusCasing.DimensionallyTranscendent.getBlock();
            case 'G' -> LightHunterCasing.UhvMachineCasing.getBlock();
            case 'H' -> FieldExtruderCasing.HyperMechanical.getBlock();
            case 'I' -> LightHunterCasing.UxvHermeticCasing.getBlock();
            case 'J' -> LightHunterCasing.RestraintDevice.getBlock();
            case 'K' -> DimensionalFocusCasing.DimensionInjection.getBlock();
            case 'L' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getBlock();
            case 'M' -> LightHunterCasing.DimensionalBridgeCasing.getBlock();
            case 'N' -> LightHunterCasing.ModuleConnector.getBlock();
            case 'O' -> LightHunterCasing.HighPowerCasing.getBlock();
            default -> throw new IllegalArgumentException("Unknown Light Hunter structure symbol: " + symbol);
        };
    }

    public static int metadata(char symbol) {
        return switch (symbol) {
            case 'A' -> LightHunterCasing.EchoCasing.getMetadata();
            case 'B' -> LightHunterCasing.UxvMachineCasing.getMetadata();
            case 'C' -> KerrNewmanCasing.AtomicCasing.getMetadata();
            case 'D' -> LightHunterCasing.DragonStrengthTritaniumCasing.getMetadata();
            case 'E' -> LightHunterCasing.HyperCore.getMetadata();
            case 'F' -> DimensionalFocusCasing.DimensionallyTranscendent.getMetadata();
            case 'G' -> LightHunterCasing.UhvMachineCasing.getMetadata();
            case 'H' -> FieldExtruderCasing.HyperMechanical.getMetadata();
            case 'I' -> LightHunterCasing.UxvHermeticCasing.getMetadata();
            case 'J' -> LightHunterCasing.RestraintDevice.getMetadata();
            case 'K' -> DimensionalFocusCasing.DimensionInjection.getMetadata();
            case 'L' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getMetadata();
            case 'M' -> LightHunterCasing.DimensionalBridgeCasing.getMetadata();
            case 'N' -> LightHunterCasing.ModuleConnector.getMetadata();
            case 'O' -> LightHunterCasing.HighPowerCasing.getMetadata();
            default -> throw new IllegalArgumentException("Unknown Light Hunter structure symbol: " + symbol);
        };
    }

    public static <T> IStructureElement<T> element(char symbol) {
        Block block = block(symbol);
        int metadata = metadata(symbol);
        if (block instanceof BlockLightHunterCasing casing) return casing.getVariant(metadata).asElement();
        return StructureUtility.ofBlock(block, metadata);
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        int controllers = 0;
        try (InputStream stream = NexusSatelliteFactoryStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing Light Hunter structure: " + RESOURCE);
            try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
                if (readVarInt(data) != -1 || readVarInt(data) != 1)
                    throw new IOException("Unsupported Light Hunter structure format");
                if (readVarInt(data) != 2 || readVarInt(data) != 0 || readVarInt(data) != 4)
                    throw new IOException("Unexpected Light Hunter structure axes");
                if (readVarInt(data) != LENGTH) throw new IOException("Invalid Light Hunter structure depth");
                for (int aisle = 0; aisle < LENGTH; aisle++) {
                    if (readVarInt(data) != HEIGHT) throw new IOException("Invalid Light Hunter structure height");
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = data.readUTF();
                        if (source.length() != WIDTH) throw new IOException("Invalid Light Hunter structure width");
                        for (int column = 0; column < WIDTH; column++) {
                            char symbol = source.charAt(column);
                            if (symbol == '~') controllers++;
                            else if (symbol != ' ' && SYMBOLS.indexOf(symbol) < 0)
                                throw new IOException("Invalid Light Hunter structure symbol: " + symbol);
                        }
                        result[HEIGHT - row - 1][LENGTH - aisle - 1] = source;
                    }
                }
                if (data.read() != -1) throw new IOException("Unexpected Light Hunter structure data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Light Hunter structure", exception);
        }
        if (controllers != 1 || result[OFFSET_Y][OFFSET_Z].charAt(OFFSET_X) != '~')
            throw new IllegalStateException("Invalid Nexus satellite controller offset");
        return result;
    }

    private static int readVarInt(DataInputStream data) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = data.readUnsignedByte();
            value |= (next & 127) << shift;
            if ((next & 128) == 0) return value;
        }
        throw new IOException("Invalid Light Hunter structure integer");
    }
}
