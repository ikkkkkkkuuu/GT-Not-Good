package com.xyp.gtnotgood.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;

import com.xyp.gtnotgood.GTNotGood;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.blocks.BlockCasingsAbstract;

public class StructureUtils {

    // 基础路径
    public static String BASE_PATH = "/assets/";
    public static ConcurrentHashMap<String, String[][]> MULTIBLOCK_CACHE = new ConcurrentHashMap<>();

    /**
     * 从文件读取多方块结构 (优先 .mbs 二进制, 回退 .mb 文本)
     */
    public static String[][] readStructureFromFile(String fileName) {
        return MULTIBLOCK_CACHE.computeIfAbsent(fileName, name -> {
            String pathMbs = BASE_PATH + name.replace(':', '/') + ".mbs";
            String pathMb = BASE_PATH + name.replace(':', '/') + ".mb";
            try {
                InputStream mbsStream = StructureUtils.class.getResourceAsStream(pathMbs);
                if (mbsStream != null) {
                    try (mbsStream) {
                        return StructureFileCodec.readBinary(mbsStream);
                    }
                }
                InputStream mbStream = StructureUtils.class.getResourceAsStream(pathMb);
                if (mbStream != null) {
                    try (mbStream) {
                        return StructureFileCodec.readText(mbStream);
                    }
                }
                throw new IllegalArgumentException("无法读取文件: " + name + " (.mbs 或 .mb)，请检查文件是否存在。");
            } catch (IOException e) {
                GTNotGood.LOG.error("Failed to load structure file: {}", name, e);
                throw new IllegalStateException("Failed to load multiblock structure: " + name, e);
            }
        });
    }

    /**
     * 转置二维数组
     *
     * @param original 原始二维数组
     * @return 转置后的二维数组
     */
    public static String[][] transposeStructure(String[][] original) {
        if (original == null || original.length == 0) {
            throw new IllegalArgumentException("矩阵为空，无法转置！");
        }

        int rows = original.length;
        int cols = original[0].length;
        String[][] transposed = new String[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = original[i][j];
            }
        }

        return transposed;
    }

    /**
     * 打印二维数组
     *
     * @param structure 二维字符串数组
     */
    public static void printStructure(String[][] structure) {
        for (String[] row : structure) {
            System.out.println(String.join(",", row));
        }
    }

    /**
     * Like structure definition, select a character from the structure definition string array as the target to place
     * blocks in the world, with the machine facing the XZ direction.
     *
     * @param aBaseMetaTileEntity the machine
     * @param offSetX             HORIZONTAL_OFF_SET of the machine structure definition
     * @param offSetY             VERTICAL_OFF_SET of the machine structure definition
     * @param offSetZ             DEPTH_OFF_SET of the machine structure definition
     * @param structureString     the machine structure definition string array
     * @param isStructureFlipped  if the machine is flipped, use getFlip().isHorizontallyFlipped() to get it
     * @param targetString        target character
     * @param targetBlock         target block
     * @param targetMeta          target block meta
     */
    public static void setStringBlockXZ(IGregTechTileEntity aBaseMetaTileEntity, int offSetX, int offSetY, int offSetZ,
        String[][] structureString, boolean isStructureFlipped, String targetString, Block targetBlock,
        int targetMeta) {
        int mDirectionX = aBaseMetaTileEntity.getFrontFacing().offsetX;
        int mDirectionZ = aBaseMetaTileEntity.getFrontFacing().offsetZ;
        int xDir = 0;
        int zDir = 0;
        if (mDirectionX == 1) {
            // EAST
            xDir = 1;
            zDir = 1;
        } else if (mDirectionX == -1) {
            // WEST
            xDir = -1;
            zDir = -1;
        }
        if (mDirectionZ == 1) {
            // SOUTH
            xDir = -1;
            zDir = 1;
        } else if (mDirectionZ == -1) {
            // NORTH
            xDir = 1;
            zDir = -1;
        }
        int lengthX = structureString[0][0].length();
        int lengthY = structureString.length;
        int lengthZ = structureString[0].length;
        for (int x = 0; x < lengthX; x++) {
            for (int z = 0; z < lengthZ; z++) {
                for (int y = 0; y < lengthY; y++) {
                    String listStr = String.valueOf(structureString[y][z].charAt(x));
                    if (!Objects.equals(listStr, targetString)) continue;

                    int aX = (offSetX - x) * xDir;
                    int aY = offSetY - y;
                    int aZ = (offSetZ - z) * zDir;
                    if (mDirectionX == 1 || mDirectionX == -1) {
                        int temp = aX;
                        aX = aZ;
                        aZ = temp;
                    }
                    if (isStructureFlipped) {
                        if (mDirectionX == 1 || mDirectionX == -1) {
                            aZ = -aZ;
                        } else {
                            aX = -aX;
                        }
                    }

                    aBaseMetaTileEntity.getWorld()
                        .setBlock(
                            aBaseMetaTileEntity.getXCoord() + aX,
                            aBaseMetaTileEntity.getYCoord() + aY,
                            aBaseMetaTileEntity.getZCoord() + aZ,
                            targetBlock,
                            targetMeta,
                            3);
                }
            }
        }
    }

    public static void setStringBlockXZ(IGregTechTileEntity aBaseMetaTileEntity, int offSetX, int offSetY, int offSetZ,
        String[][] structureString, boolean isStructureFlipped, String targetString, Block targetBlock) {
        setStringBlockXZ(
            aBaseMetaTileEntity,
            offSetX,
            offSetY,
            offSetZ,
            structureString,
            isStructureFlipped,
            targetString,
            targetBlock,
            0);
    }

    public static String[][] replaceLetters(String[][] array, String replacement) {
        String[][] outputArray = new String[array.length][];
        for (int i = 0; i < array.length; i++) {
            outputArray[i] = new String[array[i].length];
            for (int j = 0; j < array[i].length; j++) {
                outputArray[i][j] = array[i][j].replaceAll("[A-Z]", replacement);
            }
        }
        return outputArray;
    }

    public static int getTextureIndex(Block block, int meta) {
        return ((BlockCasingsAbstract) block).getTextureIndex(meta);
    }
}
