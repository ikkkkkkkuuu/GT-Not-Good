package com.xyp.gtnotgood.config;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(modid = ModList.ModIds.GT_NOT_GOOD, configSubDirectory = "GTNOTGOOD", filename = "Interaction")
@Config.Sync
@Config.RequiresMcRestart
public final class InteractionConfig {

    @Config.Comment("方块交互距离（格），最小 5 格，用于挖掘、放置和右键方块，不改变实体攻击距离。联机以服务器配置为准，修改后重启。")
    @Config.DefaultDouble(16.0)
    @Config.RangeDouble(min = 5.0)
    public static double blockReachDistance = 16.0;

    public static double getBlockReachDistance() {
        return Double.isFinite(blockReachDistance) ? Math.max(5.0, blockReachDistance) : 16.0;
    }

    public static double getContainerDistanceSquared(EntityPlayer player) {
        double reach = player instanceof EntityPlayerMP serverPlayer
            ? serverPlayer.theItemInWorldManager.getBlockReachDistance()
            : getBlockReachDistance();
        // Reach starts at the eyes and hits a face; containers measure from the feet to the block center.
        double distance = Math.max(8.0, reach + player.getEyeHeight() + Math.sqrt(3.0) / 2.0);
        return distance * distance;
    }
}
