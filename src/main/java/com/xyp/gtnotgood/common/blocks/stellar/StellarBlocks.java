package com.xyp.gtnotgood.common.blocks.stellar;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.stellar.StellarBlockVariant.Group;

import cpw.mods.fml.common.registry.GameRegistry;

public final class StellarBlocks {

    private static final Map<Group, BlockStellar> blocks = new EnumMap<>(Group.class);
    private static final Map<StellarPanelVariant.Group, BlockStellarPanel> panels = new EnumMap<>(
        StellarPanelVariant.Group.class);

    private StellarBlocks() {}

    public static void register() {
        for (Group group : Group.values()) {
            register(group);
        }
        for (StellarPanelVariant.Group group : StellarPanelVariant.Group.values()) {
            BlockStellarPanel block = new BlockStellarPanel(group);
            GameRegistry.registerBlock(block, ItemBlockStellarPanel.class, group.getRegistryName());
            panels.put(group, block);
            for (StellarPanelVariant variant : StellarPanelVariant.values()) {
                if (variant.getGroup() == group) {
                    variant.getItemContainer().set(new ItemStack(block, 1, variant.getMetadata()));
                }
            }
        }
    }

    private static void register(Group group) {
        BlockStellar block = new BlockStellar(group);
        GameRegistry.registerBlock(block, ItemBlockStellar.class, group.getRegistryName());
        blocks.put(group, block);
        for (StellarBlockVariant variant : StellarBlockVariant.values()) {
            if (variant.getGroup() == group) {
                variant.getItemContainer().set(new ItemStack(block, 1, variant.getMetadata()));
            }
        }
    }

    public static BlockStellar getBlock(Group group) {
        return blocks.get(group);
    }

    public static BlockStellarPanel getPanel(StellarPanelVariant.Group group) {
        return panels.get(group);
    }
}
