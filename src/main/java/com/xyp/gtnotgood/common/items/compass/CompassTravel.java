package com.xyp.gtnotgood.common.items.compass;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

/** Explicit compass travel loads destination terrain, then checks a solid floor and two empty blocks on the server. */
public final class CompassTravel {

    private CompassTravel() {}

    public static void teleport(EntityPlayerMP player, ItemStack stack) {
        NBTTagCompound tag = StructureCompassItem.data(stack);
        if (!tag.getBoolean("Found")) return;
        if (tag.getInteger("Dimension") != player.dimension) {
            // #tr compass.travel_dimension
            // # Switch modes to clear this target, then search in the current dimension.
            // # zh_CN 请切换模式清除旧目标，再搜索当前维度。
            player.addChatMessage(new ChatComponentTranslation("compass.travel_dimension"));
            return;
        }
        WorldServer world = (WorldServer) player.worldObj;
        long now = world.getTotalWorldTime();
        // Player-owned cooldown prevents switching stacks from bypassing the limit.
        long last = player.getEntityData()
            .getLong("GTNGCompassTravel");
        if (last > 0 && now >= last && now - last < 100) {
            // #tr compass.cooldown
            // # Wait five seconds between compass teleports.
            // # zh_CN 罗盘传送间隔为五秒，请稍候。
            player.addChatMessage(new ChatComponentTranslation("compass.cooldown"));
            return;
        }
        int tx = tag.getInteger("X"), tz = tag.getInteger("Z");
        if (Math.abs((long) tx) > 29_999_900 || Math.abs((long) tz) > 29_999_900) return;
        player.getEntityData()
            .setLong("GTNGCompassTravel", Math.max(1, now));
        StructureSearch.Spiral spiral = new StructureSearch.Spiral();
        for (int i = 0; i < 17 * 17; i++) {
            int x = tx + spiral.x, z = tz + spiral.z;
            spiral.advance();
            int y = world.getTopSolidOrLiquidBlock(x, z);
            if (!safe(world, x, y, z)) continue;
            if (player.ridingEntity != null) player.mountEntity(null);
            player.motionX = player.motionY = player.motionZ = 0;
            player.fallDistance = 0;
            player.playerNetServerHandler
                .setPlayerLocation(x + .5, y, z + .5, player.rotationYaw, player.rotationPitch);
            // #tr compass.arrived
            // # Teleported to the target area's surface. Candidate structures still need confirmation.
            // # zh_CN 已传送至目标区域地表；候选遗迹仍需实际确认。
            player.addChatMessage(new ChatComponentTranslation("compass.arrived"));
            return;
        }
        // #tr compass.unsafe
        // # No safe surface landing within 8 blocks of the target. You were not moved.
        // # zh_CN 目标周围 8 格内没有安全地表落脚点，未执行传送。
        player.addChatMessage(new ChatComponentTranslation("compass.unsafe"));
    }

    /** Requires a full supporting face and air for the player's body; rejects liquids, cactus and fire. */
    static boolean safe(WorldServer world, int x, int y, int z) {
        if (y < 1 || y > 253) return false;
        Block floor = world.getBlock(x, y - 1, z);
        if (!floor.isSideSolid(world, x, y - 1, z, ForgeDirection.UP) || floor.getMaterial()
            .isLiquid() || floor == Blocks.cactus || floor == Blocks.fire) return false;
        return world.isAirBlock(x, y, z) && world.isAirBlock(x, y + 1, z);
    }
}
