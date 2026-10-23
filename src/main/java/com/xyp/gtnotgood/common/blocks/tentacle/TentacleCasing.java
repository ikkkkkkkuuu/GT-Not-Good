package com.xyp.gtnotgood.common.blocks.tentacle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum TentacleCasing {

    MolecularCasing(Group.Casing2, 0, "molecular_casing",
        // #tr tile.tentacle_casing.molecular_casing.name
        // # Molecular Assembly Casing
        // # zh_CN 分子机械方块
        "tile.tentacle_casing.molecular_casing.name", GTNGItemList.TentacleMolecularCasing),
    UruiumCoil(Group.Casing2, 1, "uruium_coil_block",
        // #tr tile.tentacle_casing.uruium_coil_block.name
        // # Uruium Coil Block
        // # zh_CN 乌鲁线圈方块
        "tile.tentacle_casing.uruium_coil_block.name", GTNGItemList.UruiumCoil),
    SpacetimeAssemblyLineCasing(Group.Casing, 0, "spacetime_assembly_line_casing",
        // #tr tile.tentacle_casing.spacetime_assembly_line_casing.name
        // # Spacetime Assembly Line Casing
        // # zh_CN 超时空装配外壳
        "tile.tentacle_casing.spacetime_assembly_line_casing.name", GTNGItemList.SpacetimeAssemblyLineCasing),
    LaserCoolingCasing(Group.Casing, 1, "laser_cooling_casing",
        // #tr tile.tentacle_casing.laser_cooling_casing.name
        // # Laser Cooling Casing
        // # zh_CN 激光冷却方块
        "tile.tentacle_casing.laser_cooling_casing.name", GTNGItemList.LaserCoolingCasing),
    NaquadahAlloyCasing(Group.Casing, 2, "naquadah_alloy_casing",
        // #tr tile.tentacle_casing.naquadah_alloy_casing.name
        // # Naquadah Alloy Casing
        // # zh_CN 硅岩合金机械外壳
        "tile.tentacle_casing.naquadah_alloy_casing.name", GTNGItemList.NaquadahAlloyCasing),
    QuarkPipe(Group.Casing, 3, "quark_pipe",
        // #tr tile.tentacle_casing.quark_pipe.name
        // # Quark Pipe
        // # zh_CN 夸克管道
        "tile.tentacle_casing.quark_pipe.name", GTNGItemList.QuarkPipe),
    DegenerateRheniumConstrainedCasing(Group.Casing, 4, "degenerate_rhenium_constrained_casing",
        // #tr tile.tentacle_casing.degenerate_rhenium_constrained_casing.name
        // # Degenerate Rhenium Constrained Casing
        // # zh_CN 简并态铼约束外壳
        "tile.tentacle_casing.degenerate_rhenium_constrained_casing.name",
        GTNGItemList.DegenerateRheniumConstrainedCasing),
    SterilizingFilterCasing(Group.Casing, 5, "sterilizing_filter_casing",
        // #tr tile.tentacle_casing.sterilizing_filter_casing.name
        // # Sterilizing Filter Casing
        // # zh_CN 无菌过滤机械方块
        "tile.tentacle_casing.sterilizing_filter_casing.name", GTNGItemList.SterilizingFilterCasing),
    SpacetimeAssemblyLineUnit(Group.Casing, 6, "spacetime_assembly_line_unit",
        // #tr tile.tentacle_casing.spacetime_assembly_line_unit.name
        // # Spacetime Assembly Line Unit
        // # zh_CN 超时空装配单元
        "tile.tentacle_casing.spacetime_assembly_line_unit.name", GTNGItemList.TentacleSpacetimeAssemblyUnit),
    ContainmentFieldGenerator(Group.Casing, 7, "containment_field_generator",
        // #tr tile.tentacle_casing.containment_field_generator.name
        // # Containment Field Generator
        // # zh_CN 遏制场发生器
        "tile.tentacle_casing.containment_field_generator.name", GTNGItemList.TentacleContainmentFieldGenerator),
    HollowCasing(Group.Casing, 8, "hollow_casing",
        // #tr tile.tentacle_casing.hollow_casing.name
        // # Hollow Casing
        // # zh_CN 中空机械方块
        "tile.tentacle_casing.hollow_casing.name", GTNGItemList.TentacleHollowCasing),
    RestraintDevice(Group.Casing, 9, "restraint_device",
        // #tr tile.tentacle_casing.restraint_device.name
        // # Restraint Device
        // # zh_CN 约束装置
        "tile.tentacle_casing.restraint_device.name", GTNGItemList.TentacleRestraintDevice),
    LaserCasing(Group.Glass, 0, "laser_casing",
        // #tr tile.tentacle_casing.laser_casing.name
        // # Laser Casing
        // # zh_CN 激光机械方块
        "tile.tentacle_casing.laser_casing.name", GTNGItemList.LaserCasing),
    ElectronPermeableGlass(Group.Glass, 1, "electron_permeable_glass",
        // #tr tile.tentacle_casing.electron_permeable_glass.name
        // # Electron Permeable Amprosium Coated Glass
        // # zh_CN 电子渗透安普洛涂层玻璃
        "tile.tentacle_casing.electron_permeable_glass.name", GTNGItemList.ElectronPermeableAmprosiumCoatedGlass);

    public static final int ACTIVE_BIT = 8;
    private static final Set<String> CONNECTED_TEXTURES = new HashSet<>(Arrays.asList("containment_field_generator",
        "hollow_casing", "restraint_device", "degenerate_rhenium_constrained_casing", "electron_permeable_glass",
        "laser_casing", "molecular_casing", "molecular_casing_bloom", "naquadah_alloy_casing",
        "spacetime_assembly_line_casing", "sterilizing_filter_casing", "uruium_coil_block", "uruium_coil_block_bloom"));
    private final Group group;
    private final int metadata;
    private final String texture;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    TentacleCasing(Group group, int metadata, String texture, String nameKey, GTNGItemList itemContainer) {
        this.group = group;
        this.metadata = metadata;
        this.texture = texture;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public Group getGroup() {
        return group;
    }

    public int getMetadata() {
        return metadata;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public boolean hasActiveAppearance() {
        return this == UruiumCoil;
    }

    public boolean hasGlow() {
        return this == MolecularCasing || this == UruiumCoil;
    }

    public int getActiveMetadata() {
        return hasActiveAppearance() ? metadata | ACTIVE_BIT : metadata;
    }

    public String getGlowTextureName() {
        return hasGlow() ? texture + "_bloom" : null;
    }

    public boolean isGlass() {
        return group == Group.Glass;
    }

    public String getTextureName(int side) {
        return texture;
    }

    public String getTextureName(int side, boolean active) {
        return texture;
    }

    public boolean isConnected(int side) {
        return CONNECTED_TEXTURES.contains(texture);
    }

    public static boolean isConnectedTexture(String name) {
        return CONNECTED_TEXTURES.contains(name);
    }

    public BlockTentacleCasing getBlock() {
        return TentacleBlocks.getBlock(group);
    }

    public ItemStack getStack(int amount) {
        return new ItemStack(getBlock(), amount, metadata);
    }

    public int getTextureId() {
        return TentacleBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        return TentacleBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> asElement() {
        IStructureElement<T> inactive = StructureUtility.ofBlock(getBlock(), metadata);
        return hasActiveAppearance()
            ? StructureUtility.ofChain(inactive, StructureUtility.ofBlock(getBlock(), getActiveMetadata()))
            : inactive;
    }

    public static TentacleCasing fromMetadata(Group group, int metadata) {
        for (TentacleCasing variant : values()) {
            if (
                variant.group == group && (variant.metadata == metadata
                    || variant.hasActiveAppearance() && variant.getActiveMetadata() == metadata)
            ) return variant;
        }
        return null;
    }

    public enum Group {
        Casing,
        Casing2,
        Glass
    }
}
