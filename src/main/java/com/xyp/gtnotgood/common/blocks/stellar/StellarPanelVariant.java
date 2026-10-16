package com.xyp.gtnotgood.common.blocks.stellar;

import net.minecraft.block.Block;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

public enum StellarPanelVariant {

    BurstCrownPanel(0, "OVERLAY_FRONT_BURST_CROWN", Base.SpaceBending,
        // #tr tile.stellar_controller_panel.0.name
        // # Burst Thermaliser Crown Panel
        // # zh_CN 爆发热化冠装饰面板
        "tile.stellar_controller_panel.0.name", GTNGItemList.BurstCrownPanel),
    BurstCrownActivePanel(1, "OVERLAY_FRONT_BURST_CROWN_ACTIVE", Base.SpaceBending,
        // #tr tile.stellar_controller_panel.1.name
        // # Burst Thermaliser Crown Panel (Animated)
        // # zh_CN 爆发热化冠装饰面板（动态）
        "tile.stellar_controller_panel.1.name", GTNGItemList.BurstCrownActivePanel),
    ConduitJunctionPanel(2, "OVERLAY_FRONT_CONDUIT_JUNCTION", Base.Temporal,
        // #tr tile.stellar_controller_panel.2.name
        // # Conduit Junction Panel
        // # zh_CN 导管交汇器装饰面板
        "tile.stellar_controller_panel.2.name", GTNGItemList.ConduitJunctionPanel),
    ConduitJunctionActivePanel(3, "OVERLAY_FRONT_CONDUIT_JUNCTION_ACTIVE", Base.Temporal,
        // #tr tile.stellar_controller_panel.3.name
        // # Conduit Junction Panel (Animated)
        // # zh_CN 导管交汇器装饰面板（动态）
        "tile.stellar_controller_panel.3.name", GTNGItemList.ConduitJunctionActivePanel),
    ConduitMirrorPanel(4, "OVERLAY_FRONT_CONDUIT_MIRROR", Base.Temporal,
        // #tr tile.stellar_controller_panel.4.name
        // # Conduit Mirror Panel
        // # zh_CN 导管反射镜装饰面板
        "tile.stellar_controller_panel.4.name", GTNGItemList.ConduitMirrorPanel),
    ConduitMirrorActivePanel(5, "OVERLAY_FRONT_CONDUIT_MIRROR_ACTIVE", Base.Temporal,
        // #tr tile.stellar_controller_panel.5.name
        // # Conduit Mirror Panel (Animated)
        // # zh_CN 导管反射镜装饰面板（动态）
        "tile.stellar_controller_panel.5.name", GTNGItemList.ConduitMirrorActivePanel),
    EyeOfHarmonyPanel(6, "OVERLAY_FRONT_EYE_OF_HARMONY", Base.Spatial,
        // #tr tile.stellar_controller_panel.6.name
        // # Eye of Harmony Panel
        // # zh_CN 谐振之眼装饰面板
        "tile.stellar_controller_panel.6.name", GTNGItemList.EyeOfHarmonyPanel),
    EyeOfHarmonyActivePanel(7, "OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE", Base.Spatial,
        // #tr tile.stellar_controller_panel.7.name
        // # Eye of Harmony Panel (Animated)
        // # zh_CN 谐振之眼装饰面板（动态）
        "tile.stellar_controller_panel.7.name", GTNGItemList.EyeOfHarmonyActivePanel),
    HomogenyPanel(8, "OVERLAY_FRONT_HOMOGENY", Base.Temporal,
        // #tr tile.stellar_controller_panel.8.name
        // # Homogeny Panel
        // # zh_CN 均质化装置装饰面板
        "tile.stellar_controller_panel.8.name", GTNGItemList.HomogenyPanel),
    HomogenyActivePanel(9, "OVERLAY_FRONT_HOMOGENY_ACTIVE", Base.Temporal,
        // #tr tile.stellar_controller_panel.9.name
        // # Homogeny Panel (Animated)
        // # zh_CN 均质化装置装饰面板（动态）
        "tile.stellar_controller_panel.9.name", GTNGItemList.HomogenyActivePanel),
    KerrCompressorPanel(10, "OVERLAY_FRONT_KERR_COMPRESSOR", Base.RadiationAbsorbent,
        // #tr tile.stellar_controller_panel.10.name
        // # Kerr Compressor Module Panel
        // # zh_CN 克尔压缩模块装饰面板
        "tile.stellar_controller_panel.10.name", GTNGItemList.KerrCompressorPanel),
    KerrCompressorActivePanel(11, "OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE", Base.RadiationAbsorbent,
        // #tr tile.stellar_controller_panel.11.name
        // # Kerr Compressor Module Panel (Animated)
        // # zh_CN 克尔压缩模块装饰面板（动态）
        "tile.stellar_controller_panel.11.name", GTNGItemList.KerrCompressorActivePanel),
    KerrEnginePanel(12, "OVERLAY_FRONT_KERR_ENGINE", Base.SpaceBending,
        // #tr tile.stellar_controller_panel.12.name
        // # Kerr Ergosphere Engine Panel
        // # zh_CN 克尔能层引擎装饰面板
        "tile.stellar_controller_panel.12.name", GTNGItemList.KerrEnginePanel),
    KerrEngineActivePanel(13, "OVERLAY_FRONT_KERR_ENGINE_ACTIVE", Base.SpaceBending,
        // #tr tile.stellar_controller_panel.13.name
        // # Kerr Ergosphere Engine Panel (Animated)
        // # zh_CN 克尔能层引擎装饰面板（动态）
        "tile.stellar_controller_panel.13.name", GTNGItemList.KerrEngineActivePanel),
    KerrExtractorPanel(14, "OVERLAY_FRONT_KERR_EXTRACTOR", Base.Extraction,
        // #tr tile.stellar_controller_panel.14.name
        // # Kerr Extractor Module Panel
        // # zh_CN 克尔提取模块装饰面板
        "tile.stellar_controller_panel.14.name", GTNGItemList.KerrExtractorPanel),
    KerrExtractorActivePanel(15, "OVERLAY_FRONT_KERR_EXTRACTOR_ACTIVE", Base.Extraction,
        // #tr tile.stellar_controller_panel.15.name
        // # Kerr Extractor Module Panel (Animated)
        // # zh_CN 克尔提取模块装饰面板（动态）
        "tile.stellar_controller_panel.15.name", GTNGItemList.KerrExtractorActivePanel),
    QuasarEnginePanel(16, "OVERLAY_FRONT_QUASAR_ENGINE", Base.SpaceBending,
        // #tr tile.stellar_controller_panel_2.0.name
        // # Quasar Engine Panel
        // # zh_CN 类星体引擎装饰面板
        "tile.stellar_controller_panel_2.0.name", GTNGItemList.QuasarEnginePanel),
    QuasarEngineActivePanel(17, "OVERLAY_FRONT_QUASAR_ENGINE_ACTIVE", Base.SpaceBending,
        // #tr tile.stellar_controller_panel_2.1.name
        // # Quasar Engine Panel (Animated)
        // # zh_CN 类星体引擎装饰面板（动态）
        "tile.stellar_controller_panel_2.1.name", GTNGItemList.QuasarEngineActivePanel),
    RealityDiffractionPanel(18, "OVERLAY_FRONT_REALITY_DIFFRACTION", Base.Electromagnetic,
        // #tr tile.stellar_controller_panel_2.2.name
        // # Reality Diffraction Panel
        // # zh_CN 现实衍射装置装饰面板
        "tile.stellar_controller_panel_2.2.name", GTNGItemList.RealityDiffractionPanel),
    RealityDiffractionActivePanel(19, "OVERLAY_FRONT_REALITY_DIFFRACTION_ACTIVE", Base.Electromagnetic,
        // #tr tile.stellar_controller_panel_2.3.name
        // # Reality Diffraction Panel (Animated)
        // # zh_CN 现实衍射装置装饰面板（动态）
        "tile.stellar_controller_panel_2.3.name", GTNGItemList.RealityDiffractionActivePanel),
    RealityRefabricationPanel(20, "OVERLAY_FRONT_REALITY_REFAB", Base.Electromagnetic,
        // #tr tile.stellar_controller_panel_2.4.name
        // # Reality Refabrication Panel
        // # zh_CN 现实重组装置装饰面板
        "tile.stellar_controller_panel_2.4.name", GTNGItemList.RealityRefabricationPanel),
    RealityRefabricationActivePanel(21, "OVERLAY_FRONT_REALITY_REFAB_ACTIVE", Base.Electromagnetic,
        // #tr tile.stellar_controller_panel_2.5.name
        // # Reality Refabrication Panel (Animated)
        // # zh_CN 现实重组装置装饰面板（动态）
        "tile.stellar_controller_panel_2.5.name", GTNGItemList.RealityRefabricationActivePanel),
    WormholeMouthPanel(22, "OVERLAY_FRONT_WORMHOLE_MOUTH", Base.Temporal,
        // #tr tile.stellar_controller_panel_2.6.name
        // # Wormhole Mouth Panel
        // # zh_CN 虫洞口装饰面板
        "tile.stellar_controller_panel_2.6.name", GTNGItemList.WormholeMouthPanel),
    WormholeMouthActivePanel(23, "OVERLAY_FRONT_WORMHOLE_MOUTH_ACTIVE", Base.Temporal,
        // #tr tile.stellar_controller_panel_2.7.name
        // # Wormhole Mouth Panel (Animated)
        // # zh_CN 虫洞口装饰面板（动态）
        "tile.stellar_controller_panel_2.7.name", GTNGItemList.WormholeMouthActivePanel),
    BurstControlHatchPanel(24, "OVERLAY_HATCH_BURST_CONTROL", Base.SpaceBending,
        // #tr tile.stellar_controller_panel_2.8.name
        // # Burst Control Hatch Panel
        // # zh_CN 爆发控制舱装饰面板
        "tile.stellar_controller_panel_2.8.name", GTNGItemList.BurstControlHatchPanel),
    DirectiveHatchPanel(25, "OVERLAY_HATCH_DIRECTIVE", Base.Temporal,
        // #tr tile.stellar_controller_panel_2.9.name
        // # Directive Hatch Panel
        // # zh_CN 指令舱装饰面板
        "tile.stellar_controller_panel_2.9.name", GTNGItemList.DirectiveHatchPanel),
    GrbReceptorHatchPanel(26, "OVERLAY_HATCH_GRB_RECEPTOR", Base.SpaceBending,
        // #tr tile.stellar_controller_panel_2.10.name
        // # GRB Receptor Port Panel
        // # zh_CN 伽马射线暴接收端口装饰面板
        "tile.stellar_controller_panel_2.10.name", GTNGItemList.GrbReceptorHatchPanel),
    StellarFieldSensorHatchPanel(27, "OVERLAY_HATCH_STELLAR_FIELD_SENSOR", Base.Spatial,
        // #tr tile.stellar_controller_panel_2.11.name
        // # Stellar Field Sensor Hatch Panel
        // # zh_CN 恒星场传感舱装饰面板
        "tile.stellar_controller_panel_2.11.name", GTNGItemList.StellarFieldSensorHatchPanel);

    private final int id;
    private final String overlayTexture;
    private final Base base;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    StellarPanelVariant(int id, String overlayTexture, Base base, String nameKey, GTNGItemList itemContainer) {
        this.id = id;
        this.overlayTexture = overlayTexture;
        this.base = base;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public Group getGroup() {
        return id < 16 ? Group.ControllerPanel : Group.ControllerPanel2;
    }

    public int getMetadata() {
        return id % 16;
    }

    public String getOverlayTexture() {
        return overlayTexture;
    }

    public Base getBase() {
        return base;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public enum Base {

        SpaceBending(Casings.ExtremeDensitySpaceBendingCasing, "base_space_bending"),
        RadiationAbsorbent(Casings.BackgroundRadiationAbsorbentCasing, "base_radiation_absorbent"),
        Spatial(Casings.ReinforcedSpatialStructureCasing, "base_spatial"),
        Temporal(Casings.ReinforcedTemporalStructureCasing, "base_temporal"),
        Electromagnetic(Casings.ElectromagneticallyIsolatedCasing, "base_electromagnetic"),
        Extraction(null, "sl_extraction");

        private final Casings casing;
        private final String connectedTexture;

        Base(Casings casing, String connectedTexture) {
            this.casing = casing;
            this.connectedTexture = connectedTexture;
        }

        public String getConnectedTexture() {
            return connectedTexture;
        }

        public Block getBlock() {
            return casing == null ? StellarBlocks.getBlock(StellarBlockVariant.Group.Casing) : casing.getBlock();
        }

        public int getMetadata() {
            return casing == null ? StellarBlockVariant.ErgosphereExtractionCasing.getMetadata()
                : casing.getBlockMeta();
        }

        @SideOnly(Side.CLIENT)
        public ITexture getTexture() {
            return casing == null ? TextureFactory.of(getBlock(), getMetadata()) : casing.getCasingTexture();
        }
    }

    public enum Group {

        ControllerPanel("stellar_controller_panel"),
        ControllerPanel2("stellar_controller_panel_2");

        private final String registryName;

        Group(String registryName) {
            this.registryName = registryName;
        }

        public String getRegistryName() {
            return registryName;
        }
    }
}
