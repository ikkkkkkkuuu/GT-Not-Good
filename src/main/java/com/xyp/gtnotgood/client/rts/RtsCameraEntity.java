package com.xyp.gtnotgood.client.rts;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Client-local render viewpoint, never registered or spawned into a world. It has no inventory,
 * collision or physics; only the owning camera controller advances its position.
 */
final class RtsCameraEntity extends EntityLivingBase {

    RtsCameraEntity(World world) {
        super(world);
        noClip = true;
        ignoreFrustumCheck = true;
        // 1.7.10 EntityRenderer uses yOffset - 1.62, rather than getEyeHeight(), for its view origin.
        yOffset = 1.62f;
    }

    /** Sets every vanilla interpolation origin to the already interpolated frame pose. */
    void apply(CameraMotionSolver.Pose pose) {
        setPosition(pose.x(), pose.y(), pose.z());
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
        prevRotationYaw = rotationYaw = pose.yawDeg();
        prevRotationPitch = rotationPitch = pose.pitchDeg();
    }

    @Override
    public float getEyeHeight() {
        return 0;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    /** A viewpoint has no equipped item; required by EntityLivingBase's render contract. */
    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public ItemStack getEquipmentInSlot(int slot) {
        return null;
    }

    /** Prevents accidentally treating this detached render object as an inventory. */
    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        if (stack != null) throw new UnsupportedOperationException("RTS camera has no equipment");
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return new ItemStack[0];
    }
}
