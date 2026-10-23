package com.xyp.gtnotgood.common.blocks.assemblyplant;

import com.xyp.gtnotgood.client.assemblyplant.AssemblyPlantConnectedTexture;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class AssemblyPlantBlocks {

    private static final int TEXTURE_PAGE = 123;
    private static final ITexture[] textures = new ITexture[AssemblyPlantCasing.values().length];
    private static BlockAssemblyPlantCasing casing;
    private static BlockAssemblyPlantCasing glass;

    private AssemblyPlantBlocks() {}

    public static void register() {
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page 123 is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        casing = new BlockAssemblyPlantCasing(false);
        glass = new BlockAssemblyPlantCasing(true);
        GameRegistry.registerBlock(casing, ItemBlockAssemblyPlantCasing.class, "assembly_plant_casing");
        GameRegistry.registerBlock(glass, ItemBlockAssemblyPlantCasing.class, "assembly_plant_glass");
        for (AssemblyPlantCasing variant : AssemblyPlantCasing.values()) {
            variant.getItemContainer().set(variant.getLegacyStack(1));
            ITexture texture = new AssemblyPlantConnectedTexture(variant, false);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(variant.getTextureId(), texture);
        }
    }

    public static BlockAssemblyPlantCasing getBlock(boolean transparent) {
        BlockAssemblyPlantCasing block = transparent ? glass : casing;
        if (block == null) throw new IllegalStateException("Assembly plant casings have not been registered");
        return block;
    }

    public static int getTextureId(AssemblyPlantCasing variant) {
        return (TEXTURE_PAGE << 7) + variant.ordinal();
    }

    public static ITexture getTexture(AssemblyPlantCasing variant) {
        return textures[variant.ordinal()];
    }
}
