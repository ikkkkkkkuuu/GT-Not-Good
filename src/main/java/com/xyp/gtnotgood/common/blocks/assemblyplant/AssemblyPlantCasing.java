package com.xyp.gtnotgood.common.blocks.assemblyplant;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum AssemblyPlantCasing implements StructureCasing {

    AdvancedAssemblyLineUnit("advanced_assembly_line_unit", false, false, true,
        // #tr tile.assembly_plant_casing.advanced_assembly.name
        // # Advanced Assembly Line Unit Casing
        // # zh_CN 进阶装配线控制外壳
        "tile.assembly_plant_casing.advanced_assembly.name", GTNGItemList.AdvancedAssemblyLineUnit),
    SpacetimeAssemblyLineUnit("spacetime_assembly_line_unit", false, false, false,
        // #tr tile.assembly_plant_casing.spacetime_assembly.name
        // # Spacetime Assembly Line Unit
        // # zh_CN 超时空装配单元
        "tile.assembly_plant_casing.spacetime_assembly.name", GTNGItemList.SpacetimeAssemblyLineUnit),
    LaminatedGlass("laminated_glass", true, true, false,
        // #tr tile.assembly_plant_casing.laminated_glass.name
        // # Laminated Glass
        // # zh_CN 层压玻璃
        "tile.assembly_plant_casing.laminated_glass.name", GTNGItemList.LaminatedGlass),
    FilterCasing("filter_casing", true, false, false,
        // #tr tile.assembly_plant_casing.filter.name
        // # Filter Machine Casing
        // # zh_CN 过滤机械方块
        "tile.assembly_plant_casing.filter.name", GTNGItemList.FilterCasing),
    PtfePipeCasing("ptfe_pipe_casing", false, false, false,
        // #tr tile.assembly_plant_casing.ptfe_pipe.name
        // # PTFE Pipe Casing
        // # zh_CN 聚四氟乙烯管道方块
        "tile.assembly_plant_casing.ptfe_pipe.name", GTNGItemList.PtfePipeCasing),
    TemperedGlass("tempered_glass", true, true, false,
        // #tr tile.assembly_plant_casing.tempered_glass.name
        // # Tempered Glass
        // # zh_CN 钢化玻璃
        "tile.assembly_plant_casing.tempered_glass.name", GTNGItemList.TemperedGlass),
    MolybdenumDisilicideCoil("molybdenum_disilicide_coil", true, false, true,
        // #tr tile.assembly_plant_casing.molybdenum_disilicide.name
        // # Molybdenum Disilicide Coil
        // # zh_CN 二硅化钼线圈方块
        "tile.assembly_plant_casing.molybdenum_disilicide.name", GTNGItemList.MolybdenumDisilicideCoil),
    ElectrolyticCell("electrolytic_cell", false, false, true,
        // #tr tile.assembly_plant_casing.electrolytic_cell.name
        // # Electrolytic Cell
        // # zh_CN 电解槽
        "tile.assembly_plant_casing.electrolytic_cell.name", GTNGItemList.ElectrolyticCell);

    public static final int ACTIVE_BIT = 8;
    private static final AssemblyPlantCasing[] VARIANTS = values();
    private final String textureName;
    private final boolean connected;
    private final boolean glass;
    private final boolean active;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    AssemblyPlantCasing(String textureName, boolean connected, boolean glass, boolean active, String nameKey,
        GTNGItemList itemContainer) {
        this.textureName = textureName;
        this.connected = connected;
        this.glass = glass;
        this.active = active;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public int getMetadata() {
        return ordinal();
    }

    public String getTextureName() {
        return textureName;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isGlass() {
        return glass;
    }

    public boolean hasActiveAppearance() {
        return active;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public BlockAssemblyPlantCasing getBlock() {
        return AssemblyPlantBlocks.getBlock(glass);
    }

    public int getTextureId() {
        return AssemblyPlantBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        if (hasNativeCasing()) return NativeStructureCasings.texture(this);
        return AssemblyPlantBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> legacyElement() {
        IStructureElement<T> inactive = StructureUtility.ofBlock(getBlock(), getMetadata());
        return active
            ? StructureUtility.ofChain(inactive, StructureUtility.ofBlock(getBlock(), getMetadata() | ACTIVE_BIT))
            : inactive;
    }

    public static AssemblyPlantCasing fromMetadata(int metadata) {
        if (metadata < 0 || metadata > 15) return null;
        AssemblyPlantCasing variant = VARIANTS[metadata & 7];
        return (metadata & ACTIVE_BIT) != 0 && !variant.active ? null : variant;
    }
}
