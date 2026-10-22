package com.xyp.gtnotgood.common.blocks.particlecollider;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.interfaces.ITexture;

public enum ParticleColliderCasing {

    Lafium(0, "lafium_mechanical_casing", true,
        // #tr tile.particle_collider_casing.lafium.name
        // # Lafium Mechanical Casing
        // # zh_CN 路菲恩机械方块
        "tile.particle_collider_casing.lafium.name", GTNGItemList.LafiumCasing),
    AcceleratedPipeline(1, "accelerated_pipeline", false,
        // #tr tile.particle_collider_casing.accelerated_pipeline.name
        // # Accelerated Pipeline
        // # zh_CN 加速管道
        "tile.particle_collider_casing.accelerated_pipeline.name", GTNGItemList.AcceleratedPipeline),
    SuperconductingCoil(2, "superconducting_coil", true,
        // #tr tile.particle_collider_casing.superconducting_coil.name
        // # Superconducting Coil
        // # zh_CN 超导线圈
        "tile.particle_collider_casing.superconducting_coil.name", GTNGItemList.SuperconductingCoil);

    public static final int ACTIVE_BIT = 8;
    private static final ParticleColliderCasing[] VARIANTS = values();
    private final int metadata;
    private final String textureName;
    private final boolean connected;
    private final String nameKey;
    private final GTNGItemList itemContainer;

    ParticleColliderCasing(int metadata, String textureName, boolean connected, String nameKey,
        GTNGItemList itemContainer) {
        this.metadata = metadata;
        this.textureName = textureName;
        this.connected = connected;
        this.nameKey = nameKey;
        this.itemContainer = itemContainer;
    }

    public int getMetadata() {
        return metadata;
    }

    public String getTextureName() {
        return textureName;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getNameKey() {
        return nameKey;
    }

    public GTNGItemList getItemContainer() {
        return itemContainer;
    }

    public BlockParticleColliderCasing getBlock() {
        return ParticleColliderBlocks.getBlock();
    }

    public ItemStack getStack(int amount) {
        return new ItemStack(getBlock(), amount, metadata);
    }

    public int getTextureId() {
        return ParticleColliderBlocks.getTextureId(this);
    }

    public ITexture getTexture() {
        return ParticleColliderBlocks.getTexture(this);
    }

    public <T> IStructureElement<T> asElement() {
        IStructureElement<T> inactive = StructureUtility.ofBlock(getBlock(), metadata);
        return this == SuperconductingCoil
            ? StructureUtility.ofChain(inactive, StructureUtility.ofBlock(getBlock(), metadata | ACTIVE_BIT))
            : inactive;
    }

    public static ParticleColliderCasing fromMetadata(int metadata) {
        int variant = metadata & ~ACTIVE_BIT;
        if (variant < 0 || variant >= VARIANTS.length) return null;
        if ((metadata & ACTIVE_BIT) != 0 && VARIANTS[variant] != SuperconductingCoil) return null;
        return VARIANTS[variant];
    }
}
