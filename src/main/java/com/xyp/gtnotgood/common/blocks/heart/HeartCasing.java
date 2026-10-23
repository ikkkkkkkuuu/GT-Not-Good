package com.xyp.gtnotgood.common.blocks.heart;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum HeartCasing implements StructureCasing {

    GravitonFieldConstraintCasing("graviton_field_constraint_casing", true,
        // #tr tile.heart_casing.graviton_field_constraint.name
        // # Graviton Field Constraint Casing
        // # zh_CN 引力场约束方块
        "tile.heart_casing.graviton_field_constraint.name", GTNGItemList.GravitonFieldConstraintCasing),
    AnnihilateCore("annihilate_core", false,
        // #tr tile.heart_casing.annihilate_core.name
        // # Annihilation Core
        // # zh_CN 湮灭核心
        "tile.heart_casing.annihilate_core.name", GTNGItemList.AnnihilateCore),
    DimensionalStabilityCasing("dimensional_stability_casing", false,
        // #tr tile.heart_casing.dimensional_stability.name
        // # Dimensional Stability Casing
        // # zh_CN 维度稳定方块
        "tile.heart_casing.dimensional_stability.name", GTNGItemList.DimensionalStabilityCasing),
    TemporalAnchorFieldCasing("temporal_anchor_field_casing", true,
        // #tr tile.heart_casing.temporal_anchor_field.name
        // # Temporal Anchor Field Casing
        // # zh_CN 时间锚定场外壳
        "tile.heart_casing.temporal_anchor_field.name", GTNGItemList.TemporalAnchorFieldCasing),
    RheniumReinforcedEnergyGlass("rhenium_reinforced_energy_glass", true,
        // #tr tile.heart_casing.rhenium_reinforced_energy_glass.name
        // # Rhenium Reinforced Energy Glass
        // # zh_CN 铼强化聚能玻璃
        "tile.heart_casing.rhenium_reinforced_energy_glass.name", GTNGItemList.RheniumReinforcedEnergyGlass),
    DimensionConnectionCasing("dimension_connection_casing", false,
        // #tr tile.heart_casing.dimension_connection.name
        // # Dimension Connection Casing
        // # zh_CN 维度连接机械方块
        "tile.heart_casing.dimension_connection.name", GTNGItemList.DimensionConnectionCasing),
    CreateCasing("create_casing", false,
        // #tr tile.heart_casing.create.name
        // # Creation Casing
        // # zh_CN 创造机械方块
        "tile.heart_casing.create.name", GTNGItemList.CreateCasing),
    DysonDeploymentMagnet("dyson_deployment_magnet", false,
        // #tr tile.heart_casing.dyson_deployment_magnet.name
        // # Dyson Deployment Magnet
        // # zh_CN 戴森球模块部署单元超导磁体
        "tile.heart_casing.dyson_deployment_magnet.name", GTNGItemList.DysonDeploymentMagnet);

    private static final HeartCasing[] VARIANTS = values();
    private final String texture;
    private final boolean connected;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    HeartCasing(String texture, boolean connected, String nameKey, GTNGItemList itemContainer) {
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
        return this == RheniumReinforcedEnergyGlass;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public BlockHeartCasing getBlock() {
        return HeartBlocks.getBlock(isGlass());
    }

    public int getTextureId() {
        return HeartBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        if (hasNativeCasing()) return NativeStructureCasings.texture(this);
        return HeartBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> legacyElement() {
        return StructureUtility.ofBlock(getBlock(), getMetadata());
    }

    public static HeartCasing fromMetadata(int metadata) {
        return metadata >= 0 && metadata < VARIANTS.length ? VARIANTS[metadata] : null;
    }
}
