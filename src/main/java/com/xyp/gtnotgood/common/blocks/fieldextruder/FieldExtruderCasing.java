package com.xyp.gtnotgood.common.blocks.fieldextruder;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum FieldExtruderCasing implements StructureCasing {

    Iridium(0, "iridium_casing", true,
        // #tr tile.field_extruder_casing.iridium.name
        // # Iridium Reinforced Machine Casing
        // # zh_CN 铱强化机械方块
        "tile.field_extruder_casing.iridium.name", GTNGItemList.IridiumCasing),
    HyperMechanical(1, "hyper_mechanical_casing", true,
        // #tr tile.field_extruder_casing.hyper_mechanical.name
        // # Hyper Mechanical Casing
        // # zh_CN 超能机械方块
        "tile.field_extruder_casing.hyper_mechanical.name", GTNGItemList.HyperMechanicalCasing),
    Molecular(2, "molecular_casing", false,
        // #tr tile.field_extruder_casing.molecular.name
        // # Molecular Machine Casing
        // # zh_CN 分子机械方块
        "tile.field_extruder_casing.molecular.name", GTNGItemList.MolecularCasing),
    Hollow(3, "hollow_casing", false,
        // #tr tile.field_extruder_casing.hollow.name
        // # Hollow Casing
        // # zh_CN 中空机械方块
        "tile.field_extruder_casing.hollow.name", GTNGItemList.HollowCasing),
    ContainmentFieldGenerator(4, "containment_field_generator", false,
        // #tr tile.field_extruder_casing.containment_field_generator.name
        // # Containment Field Generator
        // # zh_CN 遏制场发生器
        "tile.field_extruder_casing.containment_field_generator.name", GTNGItemList.ContainmentFieldGenerator),
    CleanroomGlass(5, "cleanroom_glass", true,
        // #tr tile.field_extruder_casing.cleanroom_glass.name
        // # Cleanroom Glass
        // # zh_CN 洁净室玻璃
        "tile.field_extruder_casing.cleanroom_glass.name", GTNGItemList.CleanroomGlass);

    private static final FieldExtruderCasing[] VARIANTS = values();
    private final int metadata;
    private final String texture;
    private final boolean connected;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    FieldExtruderCasing(int metadata, String texture, boolean connected, String nameKey, GTNGItemList itemContainer) {
        this.metadata = metadata;
        this.texture = texture;
        this.connected = connected;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public int getMetadata() {
        return metadata;
    }

    public String getTextureName() {
        return texture;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isGlass() {
        return this == CleanroomGlass;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public BlockFieldExtruderCasing getBlock() {
        return FieldExtruderBlocks.getBlock(isGlass());
    }

    public int getTextureId() {
        return FieldExtruderBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        if (hasNativeCasing()) return NativeStructureCasings.texture(this);
        return FieldExtruderBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> legacyElement() {
        return StructureUtility.ofBlock(getBlock(), metadata);
    }

    public static FieldExtruderCasing fromMetadata(int metadata) {
        return metadata >= 0 && metadata < VARIANTS.length ? VARIANTS[metadata] : null;
    }
}
