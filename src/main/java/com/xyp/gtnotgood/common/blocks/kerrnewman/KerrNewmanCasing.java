package com.xyp.gtnotgood.common.blocks.kerrnewman;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum KerrNewmanCasing {

    EnhancedHyperCasing(Group.Casing, 0, "enhanced_hyper_casing", true,
        // #tr tile.kerr_newman_casing.enhanced_hyper_casing.name
        // # Enhanced Hyper Mechanical Casing
        // # zh_CN 强化超能机械方块
        "tile.kerr_newman_casing.enhanced_hyper_casing.name", GTNGItemList.EnhancedHyperCasing),
    ZirconiaCeramicCasing(Group.Casing, 1, "zirconia_ceramic_casing", true,
        // #tr tile.kerr_newman_casing.zirconia_ceramic_casing.name
        // # Zirconia Ceramic High Strength Bending Resistant Mechanical Casing
        // # zh_CN 氧化锆陶瓷高强度耐弯折机械方块
        "tile.kerr_newman_casing.zirconia_ceramic_casing.name", GTNGItemList.ZirconiaCeramicCasing),
    PressureContainmentCasing(Group.Casing, 2, "pressure_containment_casing", true,
        // #tr tile.kerr_newman_casing.pressure_containment_casing.name
        // # Pressure Containment Casing
        // # zh_CN 压力容器机械方块
        "tile.kerr_newman_casing.pressure_containment_casing.name", GTNGItemList.PressureContainmentCasing),
    AmprosiumGearbox(Group.Casing, 3, "amprosium_gearbox", false,
        // #tr tile.kerr_newman_casing.amprosium_gearbox.name
        // # Amprosium Gearbox Casing
        // # zh_CN 安普洛齿轮箱机械方块
        "tile.kerr_newman_casing.amprosium_gearbox.name", GTNGItemList.AmprosiumGearbox),
    AmprosiumPipeCasing(Group.Casing, 4, "amprosium_pipe_casing", false,
        // #tr tile.kerr_newman_casing.amprosium_pipe_casing.name
        // # Amprosium Pipe Casing
        // # zh_CN 安普洛管道方块
        "tile.kerr_newman_casing.amprosium_pipe_casing.name", GTNGItemList.AmprosiumPipeCasing),
    OilGasPipeCasing(Group.Casing, 5, "oil_gas_pipe_casing", true,
        // #tr tile.kerr_newman_casing.oil_gas_pipe_casing.name
        // # Sealed Oil and Gas Pipe Casing
        // # zh_CN 油气输送密封管道机械方块
        "tile.kerr_newman_casing.oil_gas_pipe_casing.name", GTNGItemList.OilGasPipeCasing),
    HighPressurePipeCasing(Group.Casing, 6, "high_pressure_pipe_casing", true,
        // #tr tile.kerr_newman_casing.high_pressure_pipe_casing.name
        // # High Pressure Pipe Casing
        // # zh_CN 耐高压管道机械方块
        "tile.kerr_newman_casing.high_pressure_pipe_casing.name", GTNGItemList.HighPressurePipeCasing),
    GravityStabilizationCasing(Group.Casing, 7, "gravity_stabilization_casing", false,
        // #tr tile.kerr_newman_casing.gravity_stabilization_casing.name
        // # Gravity Stabilization Casing
        // # zh_CN 重力稳定机械方块
        "tile.kerr_newman_casing.gravity_stabilization_casing.name", GTNGItemList.GravityStabilizationCasing),
    IndustrialFramelessGlass(Group.Glass, 0, "industrial_frameless_glass", false,
        // #tr tile.kerr_newman_casing.industrial_frameless_glass.name
        // # Industrial Frameless Glass
        // # zh_CN 工业无框玻璃
        "tile.kerr_newman_casing.industrial_frameless_glass.name", GTNGItemList.IndustrialFramelessGlass),
    ShieldedAcceleratorCasing(Group.Casing, 8, "shielded_accelerator_casing", true,
        // #tr tile.kerr_newman_casing.shielded_accelerator_casing.name
        // # Shielded Accelerator Casing
        // # zh_CN 屏蔽加速器机械方块
        "tile.kerr_newman_casing.shielded_accelerator_casing.name", GTNGItemList.ShieldedAcceleratorCasing),
    BioactiveCasing(Group.Casing, 9, "bioactive_casing", true,
        // #tr tile.kerr_newman_casing.bioactive_casing.name
        // # Bioactive Mechanical Casing
        // # zh_CN 生物活性机械方块
        "tile.kerr_newman_casing.bioactive_casing.name", GTNGItemList.BioactiveCasing),
    AmprosiumActiveCasing(Group.Casing, 10, "amprosium_active_casing", false,
        // #tr tile.kerr_newman_casing.amprosium_active_casing.name
        // # Amprosium Active Casing
        // # zh_CN 安普洛活性机械方块
        "tile.kerr_newman_casing.amprosium_active_casing.name", GTNGItemList.AmprosiumActiveCasing),
    HighStrengthSupportSpindle(Group.Casing, 11, "high_strength_support_spindle", true,
        // #tr tile.kerr_newman_casing.high_strength_support_spindle.name
        // # High Strength Support Spindle
        // # zh_CN 高强度支撑主轴
        "tile.kerr_newman_casing.high_strength_support_spindle.name", GTNGItemList.HighStrengthSupportSpindle),
    AcceleratorCoilUhv(Group.Casing2, 3, "accelerator_coil_uhv", true,
        // #tr tile.kerr_newman_casing.accelerator_coil_uhv.name
        // # UHV Accelerator Electromagnetic Coil Casing
        // # zh_CN UHV加速器磁约束线圈机械方块
        "tile.kerr_newman_casing.accelerator_coil_uhv.name", GTNGItemList.AcceleratorCoilUhv),
    ExtremeDensityCasing(Group.Casing, 12, "extreme_density_casing", false,
        // #tr tile.kerr_newman_casing.extreme_density_casing.name
        // # Extreme Density Casing
        // # zh_CN 极密机械方块
        "tile.kerr_newman_casing.extreme_density_casing.name", GTNGItemList.ExtremeDensityCasing),
    DimensionalBridgeCasing(Group.Casing, 13, "dimensional_bridge_casing", true,
        // #tr tile.kerr_newman_casing.dimensional_bridge_casing.name
        // # Dimensional Bridge Casing
        // # zh_CN 维度桥接方块
        "tile.kerr_newman_casing.dimensional_bridge_casing.name", GTNGItemList.DimensionalBridgeCasing),
    PpsCorrosionResistantCasing(Group.Casing, 14, "pps_corrosion_resistant_casing", true,
        // #tr tile.kerr_newman_casing.pps_corrosion_resistant_casing.name
        // # PPS Corrosion Resistant Mechanical Casing
        // # zh_CN PPS耐腐蚀机械外壳
        "tile.kerr_newman_casing.pps_corrosion_resistant_casing.name", GTNGItemList.PpsCorrosionResistantCasing),
    QuantumGlass(Group.Glass, 1, "quantum_glass", true,
        // #tr tile.kerr_newman_casing.quantum_glass.name
        // # Quantum Glass
        // # zh_CN 量子玻璃
        "tile.kerr_newman_casing.quantum_glass.name", GTNGItemList.QuantumGlass),
    PlasmaHeaterCasing(Group.Casing, 15, "plasma_heater_casing", true,
        // #tr tile.kerr_newman_casing.plasma_heater_casing.name
        // # Plasma Heater Casing
        // # zh_CN 等离子加热机械方块
        "tile.kerr_newman_casing.plasma_heater_casing.name", GTNGItemList.PlasmaHeaterCasing),
    AtomicCasing(Group.Casing2, 0, "atomic_casing", true,
        // #tr tile.kerr_newman_casing.atomic_casing.name
        // # Atomic Casing
        // # zh_CN 原子机械方块
        "tile.kerr_newman_casing.atomic_casing.name", GTNGItemList.AtomicCasing),
    RadiationAbsorbentCasing(Group.Casing2, 1, "radiation_absorbent_casing", true,
        // #tr tile.kerr_newman_casing.radiation_absorbent_casing.name
        // # Radiation Absorbent Casing
        // # zh_CN 辐射吸收机械方块
        "tile.kerr_newman_casing.radiation_absorbent_casing.name", GTNGItemList.RadiationAbsorbentCasing),
    FusionCasingMkIV(Group.Casing2, 2, "fusion_casing_mk4", true,
        // #tr tile.kerr_newman_casing.fusion_casing_mk4.name
        // # Fusion Casing MK IV
        // # zh_CN 聚变机械方块 MK IV
        "tile.kerr_newman_casing.fusion_casing_mk4.name", GTNGItemList.FusionCasingMkIV),
    AdvancedFusionCoil(Group.Casing2, 4, "advanced_fusion_coil", false,
        // #tr tile.kerr_newman_casing.advanced_fusion_coil.name
        // # Advanced Fusion Coil Block
        // # zh_CN 进阶聚变线圈方块
        "tile.kerr_newman_casing.advanced_fusion_coil.name", GTNGItemList.AdvancedFusionCoil);

    public static final int ACTIVE_BIT = 8;
    private final Group group;
    private final int metadata;
    private final String texture;
    private final boolean connected;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    KerrNewmanCasing(Group group, int metadata, String texture, boolean connected, String nameKey,
        GTNGItemList itemContainer) {
        this.group = group;
        this.metadata = metadata;
        this.texture = texture;
        this.connected = connected;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public Group getGroup() {
        return group;
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
        return group == Group.Glass;
    }

    public boolean hasActiveAppearance() {
        return this == AcceleratorCoilUhv || this == AdvancedFusionCoil;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public BlockKerrNewmanCasing getBlock() {
        return KerrNewmanBlocks.getBlock(group);
    }

    public ItemStack getStack(int amount) {
        return new ItemStack(getBlock(), amount, metadata);
    }

    public int getTextureId() {
        return KerrNewmanBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        return KerrNewmanBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> asElement() {
        if (hasActiveAppearance()) return StructureUtility.ofBlocksMap(
            Collections.singletonMap(getBlock(), Arrays.asList(metadata, metadata | ACTIVE_BIT)), getBlock(), metadata);
        return StructureUtility.ofBlock(getBlock(), metadata);
    }

    public static KerrNewmanCasing fromMetadata(Group group, int metadata) {
        for (KerrNewmanCasing variant : values()) {
            if (
                variant.group == group && (metadata == variant.metadata
                    || variant.hasActiveAppearance() && metadata == (variant.metadata | ACTIVE_BIT))
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
