package com.xyp.gtnotgood.common.network;

import java.io.IOException;
import java.util.Base64;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Resolves the actual machine item, including GT meta-tile IDs, for both icons and localized display names. */
final class NetworkDeviceDisplay {

    private NetworkDeviceDisplay() {}

    static ItemStack stack(NetworkTopology.Endpoint endpoint) {
        TileEntity target = endpoint == null ? null : endpoint.target();
        if (target == null) return null;
        if (target instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() != null) {
            return gt.getMetaTileEntity()
                .getStackForm(1);
        }
        // Forge's pick-block implementation calls client-only Block.getItem/getDamageValue methods.
        // The dedicated server strips those methods, although a single-player integrated server retains them.
        Block block = target.getBlockType();
        Item item = Item.getItemFromBlock(block);
        return item == null ? null : new ItemStack(item, 1, block.damageDropped(target.getBlockMetadata()));
    }

    static String encode(NetworkTopology.Endpoint endpoint) {
        ItemStack stack = stack(endpoint);
        if (stack == null) return "";
        try {
            byte[] bytes = CompressedStreamTools.compress(stack.writeToNBT(new NBTTagCompound()));
            // A GUI icon must not turn a device with very large item NBT into an oversized sync packet.
            if (bytes.length > 512) {
                stack = new ItemStack(stack.getItem(), 1, stack.getItemDamage());
                bytes = CompressedStreamTools.compress(stack.writeToNBT(new NBTTagCompound()));
            }
            return Base64.getEncoder()
                .encodeToString(bytes);
        } catch (IOException e) {
            return "";
        }
    }

    static ItemStack decode(String encoded) {
        if (encoded == null || encoded.isEmpty() || encoded.length() > 1024) return null;
        try {
            return ItemStack.loadItemStackFromNBT(
                CompressedStreamTools.func_152457_a(
                    Base64.getDecoder()
                        .decode(encoded),
                    new NBTSizeTracker(32768)));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    static String name(NetworkTopology.Endpoint endpoint) {
        ItemStack stack = stack(endpoint);
        return stack == null ? "" : stack.getDisplayName();
    }
}
