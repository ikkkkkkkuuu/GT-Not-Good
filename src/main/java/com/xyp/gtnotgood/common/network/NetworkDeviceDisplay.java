package com.xyp.gtnotgood.common.network;

import java.io.IOException;
import java.util.Base64;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

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
        MovingObjectPosition hit = new MovingObjectPosition(
            target.xCoord,
            target.yCoord,
            target.zCoord,
            endpoint.side(),
            Vec3.createVectorHelper(target.xCoord + 0.5, target.yCoord + 0.5, target.zCoord + 0.5));
        return target.getBlockType()
            .getPickBlock(hit, target.getWorldObj(), target.xCoord, target.yCoord, target.zCoord);
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
