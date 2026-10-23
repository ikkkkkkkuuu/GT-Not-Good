package com.xyp.gtnotgood.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.client.assemblyplant.AssemblyPlantConnectedIcons;
import com.xyp.gtnotgood.client.assemblyplant.AssemblyPlantConnections;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperconductingElectromagneticStructure;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ElectromagneticRotorModel implements IBlockAccess {

    private final String[][] shape = SuperconductingElectromagneticStructure.shape();
    private final List<Face> faces = new ArrayList<>();
    private final RenderBlocks renderer = new RenderBlocks(this);

    ElectromagneticRotorModel() {
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
        renderer.enableAO = false;
        for (int y = -8; y <= 8; y++) for (int z = -3; z <= 3; z++) for (int x = -3; x <= 3; x++) {
            Block block = getBlock(x, y, z);
            if (block == Blocks.air) continue;
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                if (getBlock(x + side.offsetX, y + side.offsetY, z + side.offsetZ) != Blocks.air) continue;
                IIcon icon = block == AssemblyPlantCasing.ElectrolyticCell.getBlock()
                    ? AssemblyPlantConnectedIcons
                        .getIcon(AssemblyPlantCasing.ElectrolyticCell,
                            AssemblyPlantConnections.getMask(this, x, y, z, side.ordinal(),
                                AssemblyPlantCasing.ElectrolyticCell),
                            true)
                    : block.getIcon(this, x, y, z, side.ordinal());
                faces.add(new Face(block, x, y, z, side, icon));
            }
        }
    }

    void render() {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(0xf000f0);
        for (Face face : faces) {
            float shade = switch (face.side) {
                case DOWN -> 0.6F;
                case NORTH, SOUTH -> 0.85F;
                case WEST, EAST -> 0.75F;
                default -> 1F;
            };
            tessellator.setColorOpaque_F(shade, shade, shade);
            double x = face.x - 0.5, y = face.y - 0.5, z = face.z - 0.5;
            switch (face.side) {
                case DOWN -> renderer.renderFaceYNeg(face.block, x, y, z, face.icon);
                case UP -> renderer.renderFaceYPos(face.block, x, y, z, face.icon);
                case NORTH -> renderer.renderFaceZNeg(face.block, x, y, z, face.icon);
                case SOUTH -> renderer.renderFaceZPos(face.block, x, y, z, face.icon);
                case WEST -> renderer.renderFaceXNeg(face.block, x, y, z, face.icon);
                case EAST -> renderer.renderFaceXPos(face.block, x, y, z, face.icon);
                default -> {}
            }
        }
        tessellator.draw();
    }

    private char symbol(int x, int y, int z) {
        int column = x + 7, row = 10 - y, depth = z + 7;
        return SuperconductingElectromagneticStructure.isRotorPosition(column, row, depth)
            ? shape[row][depth].charAt(column)
            : ' ';
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return switch (symbol(x, y, z)) {
            case 'C' -> DimensionalFocusCasing.ImprovedSuperconductor.getBlock();
            case 'E' -> AssemblyPlantCasing.ElectrolyticCell.getBlock();
            case 'H' -> ParticleColliderCasing.AcceleratedPipeline.getBlock();
            default -> Blocks.air;
        };
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return switch (symbol(x, y, z)) {
            case 'C' -> DimensionalFocusCasing.ImprovedSuperconductor.getMetadata() | 8;
            case 'E' -> AssemblyPlantCasing.ElectrolyticCell.getMetadata() | AssemblyPlantCasing.ACTIVE_BIT;
            case 'H' -> ParticleColliderCasing.AcceleratedPipeline.getMetadata();
            default -> 0;
        };
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
        return 0xf000f0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z) == Blocks.air;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return 0;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
        return !isAirBlock(x, y, z);
    }

    @Desugar
    private record Face(Block block, int x, int y, int z, ForgeDirection side, IIcon icon) {}
}
