package com.xyp.gtnotgood.common.blocks.dimensionalfocus;

import java.util.Arrays;
import java.util.Collections;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum DimensionalFocusCasing implements StructureCasing {

    DimensionallyTranscendent("dimensionally_transcendent_casing", true,
        // #tr tile.dimensional_focus_casing.transcendent.name
        // # Dimensionally Transcendent Casing
        // # zh_CN 超维度机械方块
        "tile.dimensional_focus_casing.transcendent.name", GTNGItemList.DimensionallyTranscendentCasing),
    FusionGlass("fusion_glass", true,
        // #tr tile.dimensional_focus_casing.fusion_glass.name
        // # Fusion Glass
        // # zh_CN 聚变玻璃
        "tile.dimensional_focus_casing.fusion_glass.name", GTNGItemList.FusionGlass),
    DimensionInjection("dimension_injection_casing", true,
        // #tr tile.dimensional_focus_casing.injection.name
        // # Dimension Injection Casing
        // # zh_CN 维度注入机械方块
        "tile.dimensional_focus_casing.injection.name", GTNGItemList.DimensionInjectionCasing),
    MolecularCoil("molecular_coil", false,
        // #tr tile.dimensional_focus_casing.molecular_coil.name
        // # Molecular Coil
        // # zh_CN 分子线圈
        "tile.dimensional_focus_casing.molecular_coil.name", GTNGItemList.MolecularCoil),
    ImprovedSuperconductor("improved_superconductor_coil", true,
        // #tr tile.dimensional_focus_casing.superconductor.name
        // # Improved Superconductor Coil
        // # zh_CN 改良型超导线圈方块
        "tile.dimensional_focus_casing.superconductor.name", GTNGItemList.ImprovedSuperconductorCoil),
    LaserSafe("laser_safe_engraving_casing", true,
        // #tr tile.dimensional_focus_casing.laser_safe.name
        // # Laser-Safe Engraving Casing
        // # zh_CN 激光安全蚀刻机械方块
        "tile.dimensional_focus_casing.laser_safe.name", GTNGItemList.LaserSafeEngravingCasing);

    private static final DimensionalFocusCasing[] VARIANTS = values();
    private final String texture;
    private final boolean connected;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    DimensionalFocusCasing(String texture, boolean connected, String nameKey, GTNGItemList itemContainer) {
        this.texture = texture;
        this.connected = connected;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public int getMetadata() {
        return ordinal();
    }

    public String getTextureName() {
        return texture;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isGlass() {
        return this == FusionGlass;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public BlockDimensionalFocusCasing getBlock() {
        return DimensionalFocusBlocks.getBlock(isGlass());
    }

    public int getTextureId() {
        return DimensionalFocusBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        if (hasNativeCasing()) return NativeStructureCasings.texture(this);
        return DimensionalFocusBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> legacyElement() {
        if (
            this == ImprovedSuperconductor
        ) return StructureUtility.ofBlocksMap(
            Collections.singletonMap(getBlock(), Arrays.asList(getMetadata(), getMetadata() | 8)), getBlock(),
            getMetadata());
        return StructureUtility.ofBlock(getBlock(), getMetadata());
    }

    public static DimensionalFocusCasing fromMetadata(int metadata) {
        int variant = metadata & 7;
        return variant < VARIANTS.length ? VARIANTS[variant] : null;
    }
}
