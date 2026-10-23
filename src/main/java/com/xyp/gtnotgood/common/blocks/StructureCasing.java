package com.xyp.gtnotgood.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

public interface StructureCasing {

    Block getBlock();

    int getMetadata();

    int getTextureId();

    GTNGItemList getItemContainer();

    <T> IStructureElement<T> legacyElement();

    default boolean hasNativeCasing() {
        return NativeStructureCasings.contains(this);
    }

    default ItemStack getLegacyStack(int amount) {
        return new ItemStack(getBlock(), amount, getMetadata());
    }

    default ItemStack getStack(int amount) {
        return hasNativeCasing() ? NativeStructureCasings.stack(this, amount) : getLegacyStack(amount);
    }

    default Block getStructureBlock() {
        return hasNativeCasing() ? Block.getBlockFromItem(getStack(1).getItem()) : getBlock();
    }

    default int getStructureMetadata() {
        return hasNativeCasing() ? getStack(1).getItemDamage() : getMetadata();
    }

    /** New construction uses native blocks; existing structures remain valid. */
    default <T> IStructureElement<T> asElement() {
        return hasNativeCasing()
            ? StructureUtility.ofChain(StructureUtility.ofBlock(getStructureBlock(), getStructureMetadata()),
                legacyElement())
            : legacyElement();
    }
}
