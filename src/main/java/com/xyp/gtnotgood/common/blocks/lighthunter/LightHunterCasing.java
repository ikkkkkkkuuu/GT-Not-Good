package com.xyp.gtnotgood.common.blocks.lighthunter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum LightHunterCasing implements StructureCasing {

    UhvMachineCasing(Group.Casing, 0,
        // #tr tile.light_hunter_casing.uhv_machine_casing.name
        // # UHV Machine Casing
        // # zh_CN UHV机械方块
        "tile.light_hunter_casing.uhv_machine_casing.name", GTNGItemList.UhvMachineCasing,
        bottomTop("casings_voltage_uhv_side", "casings_voltage_uhv_bottom", "casings_voltage_uhv_top")),
    UivMachineCasing(Group.Casing, 1,
        // #tr tile.light_hunter_casing.uiv_machine_casing.name
        // # UIV Machine Casing
        // # zh_CN UIV机械方块
        "tile.light_hunter_casing.uiv_machine_casing.name", GTNGItemList.UivMachineCasing,
        bottomTop("casings_voltage_uiv_side", "casings_voltage_uiv_bottom", "casings_voltage_uiv_top")),
    UxvMachineCasing(Group.Casing, 2,
        // #tr tile.light_hunter_casing.uxv_machine_casing.name
        // # UXV Machine Casing
        // # zh_CN UXV机械方块
        "tile.light_hunter_casing.uxv_machine_casing.name", GTNGItemList.UxvMachineCasing,
        bottomTop("casings_voltage_uxv_side", "casings_voltage_uxv_bottom", "casings_voltage_uxv_top")),
    ComputerHeatVent(Group.Casing, 3,
        // #tr tile.light_hunter_casing.computer_heat_vent.name
        // # Computer Heat Vent
        // # zh_CN 计算机散热口
        "tile.light_hunter_casing.computer_heat_vent.name", GTNGItemList.ComputerHeatVent,
        bottomTop("casings_hpca_computer_heat_vent_side", "casings_hpca_computer_heat_vent_top_bot",
            "casings_hpca_computer_heat_vent_top_bot")),
    SuperComputationComponent(Group.Casing2, 0,
        // #tr tile.light_hunter_casing.super_computation_component.name
        // # Super Computation Component
        // # zh_CN 超级计算机组件
        "tile.light_hunter_casing.super_computation_component.name", GTNGItemList.SuperComputationComponent,
        cube("casings_hpca_damaged_advanced_hpca_component_side")),
    ModuleConnector(Group.Casing, 4,
        // #tr tile.light_hunter_casing.module_connector.name
        // # Module Connector
        // # zh_CN 太空电梯模块连接器
        "tile.light_hunter_casing.module_connector.name", GTNGItemList.ModuleConnector, cube("module_connector")),
    UltimateStellarContainmentCasing(Group.Casing, 5,
        // #tr tile.light_hunter_casing.ultimate_stellar_containment_casing.name
        // # Ultimate Stellar Containment Casing
        // # zh_CN 终极恒星热力容器
        "tile.light_hunter_casing.ultimate_stellar_containment_casing.name",
        GTNGItemList.UltimateStellarContainmentCasing, cube("stellar_containment_casing")),
    DragonStrengthTritaniumCasing(Group.Casing, 6,
        // #tr tile.light_hunter_casing.dragon_strength_tritanium_casing.name
        // # Dragon Strength Tritanium Machine Casing
        // # zh_CN 龙之力量三钛合金机械方块
        "tile.light_hunter_casing.dragon_strength_tritanium_casing.name", GTNGItemList.DragonStrengthTritaniumCasing,
        cube("casings_extreme_strength_tritanium_casing")),
    HighPowerCasing(Group.Casing, 7,
        // #tr tile.light_hunter_casing.high_power_casing.name
        // # High Power Casing
        // # zh_CN 高能机器外壳
        "tile.light_hunter_casing.high_power_casing.name", GTNGItemList.HighPowerCasing,
        cube("casings_hpca_high_power_casing")),
    UhvUltimateBattery(Group.Casing, 8,
        // #tr tile.light_hunter_casing.uhv_ultimate_battery.name
        // # UHV Ultimate Capacitor
        // # zh_CN UHV终极电池
        "tile.light_hunter_casing.uhv_ultimate_battery.name", GTNGItemList.UhvUltimateBattery,
        bottomTop("casings_battery_uhv_ultimate_side", "casings_battery_uhv_ultimate_top",
            "casings_battery_uhv_ultimate_top")),
    AdvancedComputerCasing(Group.Casing, 9,
        // #tr tile.light_hunter_casing.advanced_computer_casing.name
        // # Advanced Computer Casing
        // # zh_CN 高级计算机外壳
        "tile.light_hunter_casing.advanced_computer_casing.name", GTNGItemList.AdvancedComputerCasing,
        new String[] { "casings_hpca_advanced_computer_casing_bottom", "casings_hpca_advanced_computer_casing_top",
            "casings_hpca_advanced_computer_casing_front", "casings_hpca_advanced_computer_casing_back",
            "casings_hpca_advanced_computer_casing_side", "casings_hpca_advanced_computer_casing_side" }),
    EchoCasing(Group.Casing, 10,
        // #tr tile.light_hunter_casing.echo_casing.name
        // # Echo Reinforced Machine Casing
        // # zh_CN 回响强化机械方块
        "tile.light_hunter_casing.echo_casing.name", GTNGItemList.EchoCasing, cube("casings_echo_casing")),
    HyperCore(Group.Casing2, 1,
        // #tr tile.light_hunter_casing.hyper_core.name
        // # Hyper Core
        // # zh_CN 超能核心
        "tile.light_hunter_casing.hyper_core.name", GTNGItemList.HyperCore, cube("hyper_core")),
    NeutroniumPipeCasing(Group.Casing, 11,
        // #tr tile.light_hunter_casing.neutronium_pipe_casing.name
        // # Neutronium Pipe Casing
        // # zh_CN 中子素管道方块
        "tile.light_hunter_casing.neutronium_pipe_casing.name", GTNGItemList.NeutroniumPipeCasing,
        cube("neutronium_pipe_casing")),
    FusionCasingMkV(Group.Casing2, 2,
        // #tr tile.light_hunter_casing.fusion_casing_mk5.name
        // # Fusion Machine Casing MK-V
        // # zh_CN 聚变机械方块 MK-V
        "tile.light_hunter_casing.fusion_casing_mk5.name", GTNGItemList.FusionCasingMkV,
        cube("casings_fusion_fusion_casing_mk5")),
    UxvHermeticCasing(Group.Casing, 12,
        // #tr tile.light_hunter_casing.uxv_hermetic_casing.name
        // # Hermetic Machine Casing XII
        // # zh_CN 密封机械方块 XII
        "tile.light_hunter_casing.uxv_hermetic_casing.name", GTNGItemList.UxvHermeticCasing,
        bottomTop("uxv_hermetic_side", "casings_voltage_uxv_bottom", "casings_voltage_uxv_top")),
    RestraintDevice(Group.Casing, 13,
        // #tr tile.light_hunter_casing.restraint_device.name
        // # Restraint Device
        // # zh_CN 力场约束装置
        "tile.light_hunter_casing.restraint_device.name", GTNGItemList.RestraintDevice, cube("restraint_device")),
    ForceFieldGlass(Group.Glass, 0,
        // #tr tile.light_hunter_casing.force_field_glass.name
        // # Force Field Glass
        // # zh_CN 力场玻璃
        "tile.light_hunter_casing.force_field_glass.name", GTNGItemList.ForceFieldGlass, cube("force_field_glass")),
    DimensionalBridgeCasing(Group.Casing, 14,
        // #tr tile.light_hunter_casing.dimensional_bridge_casing.name
        // # Dimensional Bridge Casing
        // # zh_CN 维度桥接方块
        "tile.light_hunter_casing.dimensional_bridge_casing.name", GTNGItemList.LightHunterDimensionalBridgeCasing,
        cube("dimensional_bridge_casing")),
    FusionCasingMkIV(Group.Casing2, 3,
        // #tr tile.light_hunter_casing.fusion_casing_mk4.name
        // # Fusion Machine Casing MK-IV
        // # zh_CN 聚变机械方块 MK-IV
        "tile.light_hunter_casing.fusion_casing_mk4.name", GTNGItemList.LightHunterFusionCasingMkIV,
        cube("casings_fusion_fusion_casing_mk4"));

    private static final Set<String> CONNECTED_TEXTURES = new HashSet<>(Arrays.asList("casings_echo_casing",
        "casings_extreme_strength_tritanium_casing", "casings_fusion_fusion_casing_mk5",
        "casings_fusion_fusion_casing_mk4", "casings_fusion_fusion_casing_bloom",
        "casings_hpca_advanced_computer_casing_back", "casings_hpca_advanced_computer_casing_bottom",
        "casings_hpca_advanced_computer_casing_front", "casings_hpca_advanced_computer_casing_side",
        "casings_hpca_advanced_computer_casing_top", "casings_hpca_computer_heat_vent_side",
        "casings_hpca_computer_heat_vent_top_bot", "casings_hpca_high_power_casing", "stellar_containment_casing"));
    public static final int ACTIVE_BIT = 8;
    private static final LightHunterCasing[][] BY_METADATA = new LightHunterCasing[Group.values().length][16];

    static {
        for (LightHunterCasing variant : values()) {
            BY_METADATA[variant.group.ordinal()][variant.metadata] = variant;
            if (variant.hasActiveAppearance())
                BY_METADATA[variant.group.ordinal()][variant.getActiveMetadata()] = variant;
        }
    }

    private final Group group;
    private final int metadata;
    private final String nameKey;
    private final GTNGItemList itemContainer;
    private final String[] textures;

    LightHunterCasing(Group group, int metadata, String nameKey, GTNGItemList itemContainer, String[] textures) {
        this.group = group;
        this.metadata = metadata;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
        this.textures = textures;
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
        return group == Group.Casing2;
    }

    public int getActiveMetadata() {
        return hasActiveAppearance() ? metadata | ACTIVE_BIT : metadata;
    }

    public String getGlowTextureName() {
        return switch (this) {
            case HyperCore -> "hyper_core_bloom";
            case SuperComputationComponent -> "gtlbloom_masks_damaged_advanced_hpca_component_side";
            case FusionCasingMkIV, FusionCasingMkV -> "casings_fusion_fusion_casing_bloom";
            default -> null;
        };
    }

    public boolean isGlass() {
        return group == Group.Glass;
    }

    public String getTextureName(int side) {
        return textures[side >= 0 && side < 6 ? side : 2];
    }

    public String getTextureName(int side, boolean active) {
        return active && this == SuperComputationComponent ? "damaged_advanced_hpca_component_side"
            : getTextureName(side);
    }

    public boolean isConnected(int side) {
        return CONNECTED_TEXTURES.contains(getTextureName(side));
    }

    public static boolean isConnectedTexture(String name) {
        return CONNECTED_TEXTURES.contains(name);
    }

    public BlockLightHunterCasing getBlock() {
        return LightHunterBlocks.getBlock(group);
    }

    public int getTextureId() {
        return LightHunterBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        if (hasNativeCasing()) return NativeStructureCasings.texture(this);
        return LightHunterBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> legacyElement() {
        IStructureElement<T> inactive = StructureUtility.ofBlock(getBlock(), metadata);
        return hasActiveAppearance()
            ? StructureUtility.ofChain(inactive, StructureUtility.ofBlock(getBlock(), getActiveMetadata()))
            : inactive;
    }

    public static LightHunterCasing fromMetadata(Group group, int metadata) {
        return metadata >= 0 && metadata < 16 ? BY_METADATA[group.ordinal()][metadata] : null;
    }

    private static String[] cube(String texture) {
        return new String[] { texture, texture, texture, texture, texture, texture };
    }

    private static String[] bottomTop(String side, String bottom, String top) {
        return new String[] { bottom, top, side, side, side, side };
    }

    public enum Group {
        Casing,
        Casing2,
        Glass
    }
}
