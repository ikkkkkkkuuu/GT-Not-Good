// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.BlockLightHunterCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public final class LightHunterSpaceStationStructure {

    public static final int WIDTH = 73;
    public static final int HEIGHT = 73;
    public static final int LENGTH = 189;
    public static final int OFFSET_X = 36;
    public static final int OFFSET_Y = 36;
    public static final int OFFSET_Z = 0;
    public static final String SYMBOLS = "=ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcd";
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/light_hunter_space_station.mbs";
    private static final String[][] SHAPE = loadShape();

    private LightHunterSpaceStationStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    public static Block block(char symbol) {
        return switch (symbol) {
            case '=' -> LightHunterCasing.NeutroniumPipeCasing.getBlock();
            case 'A' -> LightHunterCasing.UhvMachineCasing.getBlock();
            case 'B' -> FieldExtruderCasing.HyperMechanical.getBlock();
            case 'C' -> LightHunterCasing.UxvMachineCasing.getBlock();
            case 'D' -> DimensionalFocusCasing.FusionGlass.getBlock();
            case 'E' -> DimensionalFocusCasing.DimensionInjection.getBlock();
            case 'F' -> LightHunterCasing.DimensionalBridgeCasing.getBlock();
            case 'G' -> FieldExtruderCasing.ContainmentFieldGenerator.getBlock();
            case 'H' -> KerrNewmanCasing.AtomicCasing.getBlock();
            case 'I' -> KerrNewmanCasing.ExtremeDensityCasing.getBlock();
            case 'K' -> LightHunterCasing.EchoCasing.getBlock();
            case 'L' -> LightHunterCasing.DragonStrengthTritaniumCasing.getBlock();
            case 'M' -> LightHunterCasing.HyperCore.getBlock();
            case 'N' -> DimensionalFocusCasing.DimensionallyTranscendent.getBlock();
            case 'O' -> LightHunterCasing.UxvHermeticCasing.getBlock();
            case 'P' -> LightHunterCasing.RestraintDevice.getBlock();
            case 'Q' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getBlock();
            case 'R' -> LightHunterCasing.HighPowerCasing.getBlock();
            case 'S' -> FieldExtruderCasing.Molecular.getBlock();
            case 'U' -> LightHunterCasing.FusionCasingMkIV.getBlock();
            case 'V' -> HeartCasing.TemporalAnchorFieldCasing.getBlock();
            case 'W' -> LightHunterCasing.FusionCasingMkV.getBlock();
            case 'X' -> LightHunterCasing.UltimateStellarContainmentCasing.getBlock();
            case 'Y' -> KerrNewmanCasing.EnhancedHyperCasing.getBlock();
            case 'Z' -> DimensionalFocusCasing.ImprovedSuperconductor.getBlock();
            case '[' -> LightHunterCasing.ForceFieldGlass.getBlock();
            case ']' -> KerrNewmanCasing.GravityStabilizationCasing.getBlock();
            case '^' -> LightHunterCasing.ModuleConnector.getBlock();
            case '_' -> LightHunterCasing.AdvancedComputerCasing.getBlock();
            case '`' -> LightHunterCasing.ComputerHeatVent.getBlock();
            case 'a' -> LightHunterCasing.SuperComputationComponent.getBlock();
            case 'b' -> LightHunterCasing.UivMachineCasing.getBlock();
            case 'c' -> LightHunterCasing.UhvUltimateBattery.getBlock();
            case 'd' -> LightHunterCasing.HighPowerCasing.getBlock();
            case 'J' -> Block.getBlockFromItem(neutroniumFrame().getItem());
            case 'T' -> AEApi.instance().definitions().blocks().quartzVibrantGlass().maybeBlock().get();
            default -> throw new IllegalArgumentException("Unknown Light Hunter structure symbol: " + symbol);
        };
    }

    public static int metadata(char symbol) {
        return switch (symbol) {
            case '=' -> LightHunterCasing.NeutroniumPipeCasing.getMetadata();
            case 'A' -> LightHunterCasing.UhvMachineCasing.getMetadata();
            case 'B' -> FieldExtruderCasing.HyperMechanical.getMetadata();
            case 'C' -> LightHunterCasing.UxvMachineCasing.getMetadata();
            case 'D' -> DimensionalFocusCasing.FusionGlass.getMetadata();
            case 'E' -> DimensionalFocusCasing.DimensionInjection.getMetadata();
            case 'F' -> LightHunterCasing.DimensionalBridgeCasing.getMetadata();
            case 'G' -> FieldExtruderCasing.ContainmentFieldGenerator.getMetadata();
            case 'H' -> KerrNewmanCasing.AtomicCasing.getMetadata();
            case 'I' -> KerrNewmanCasing.ExtremeDensityCasing.getMetadata();
            case 'K' -> LightHunterCasing.EchoCasing.getMetadata();
            case 'L' -> LightHunterCasing.DragonStrengthTritaniumCasing.getMetadata();
            case 'M' -> LightHunterCasing.HyperCore.getMetadata();
            case 'N' -> DimensionalFocusCasing.DimensionallyTranscendent.getMetadata();
            case 'O' -> LightHunterCasing.UxvHermeticCasing.getMetadata();
            case 'P' -> LightHunterCasing.RestraintDevice.getMetadata();
            case 'Q' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getMetadata();
            case 'R' -> LightHunterCasing.HighPowerCasing.getMetadata();
            case 'S' -> FieldExtruderCasing.Molecular.getMetadata();
            case 'U' -> LightHunterCasing.FusionCasingMkIV.getMetadata();
            case 'V' -> HeartCasing.TemporalAnchorFieldCasing.getMetadata();
            case 'W' -> LightHunterCasing.FusionCasingMkV.getMetadata();
            case 'X' -> LightHunterCasing.UltimateStellarContainmentCasing.getMetadata();
            case 'Y' -> KerrNewmanCasing.EnhancedHyperCasing.getMetadata();
            case 'Z' -> DimensionalFocusCasing.ImprovedSuperconductor.getMetadata();
            case '[' -> LightHunterCasing.ForceFieldGlass.getMetadata();
            case ']' -> KerrNewmanCasing.GravityStabilizationCasing.getMetadata();
            case '^' -> LightHunterCasing.ModuleConnector.getMetadata();
            case '_' -> LightHunterCasing.AdvancedComputerCasing.getMetadata();
            case '`' -> LightHunterCasing.ComputerHeatVent.getMetadata();
            case 'a' -> LightHunterCasing.SuperComputationComponent.getMetadata();
            case 'b' -> LightHunterCasing.UivMachineCasing.getMetadata();
            case 'c' -> LightHunterCasing.UhvUltimateBattery.getMetadata();
            case 'd' -> LightHunterCasing.HighPowerCasing.getMetadata();
            case 'J' -> neutroniumFrame().getItemDamage();
            case 'T' -> 0;
            default -> throw new IllegalArgumentException("Unknown Light Hunter structure symbol: " + symbol);
        };
    }

    public static <T> IStructureElement<T> element(char symbol) {
        if (symbol == 'Z') return DimensionalFocusCasing.ImprovedSuperconductor.asElement();
        Block block = block(symbol);
        int metadata = metadata(symbol);
        if (block instanceof BlockLightHunterCasing casing) return casing.getVariant(metadata).asElement();
        return StructureUtility.ofBlock(block, metadata);
    }

    private static ItemStack neutroniumFrame() {
        ItemStack frame = GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1);
        if (frame == null || Block.getBlockFromItem(frame.getItem()) == Blocks.air)
            throw new IllegalStateException("Native neutronium frame is unavailable");
        return frame;
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        int controllers = 0;
        try (InputStream stream = LightHunterSpaceStationStructure.class.getResourceAsStream(RESOURCE)) {
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
            throw new IllegalStateException("Invalid Light Hunter controller offset");
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
