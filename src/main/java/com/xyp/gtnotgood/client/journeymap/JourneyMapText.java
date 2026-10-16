package com.xyp.gtnotgood.client.journeymap;

import net.minecraft.util.StatCollector;

final class JourneyMapText {

    private JourneyMapText() {}

    static String teleport() {
        // #tr gtnotgood.journeymap.teleport
        // # Teleport here
        // # zh_CN 传送到此处
        return StatCollector.translateToLocal("gtnotgood.journeymap.teleport");
    }

    static String newWaypoint() {
        // #tr gtnotgood.journeymap.new_waypoint
        // # Create waypoint
        // # zh_CN 新建导航点
        return StatCollector.translateToLocal("gtnotgood.journeymap.new_waypoint");
    }

    static String dimension() {
        // #tr gtnotgood.journeymap.dimension
        // # Switch to your current dimension to teleport
        // # zh_CN 请切回当前维度后传送
        return StatCollector.translateToLocal("gtnotgood.journeymap.dimension");
    }

    static String permission() {
        // #tr gtnotgood.journeymap.permission
        // # Teleport requires cheats or /tp permission
        // # zh_CN 传送需要开启作弊或取得 /tp 权限
        return StatCollector.translateToLocal("gtnotgood.journeymap.permission");
    }

    static String blocked() {
        // #tr gtnotgood.journeymap.blocked
        // # No clear landing with solid ground
        // # zh_CN 未找到有支撑且无障碍的落点
        return StatCollector.translateToLocal("gtnotgood.journeymap.blocked");
    }

    static String unknown() {
        // #tr gtnotgood.journeymap.unknown
        // # Unloaded terrain: confirm height before teleporting
        // # zh_CN 地形未加载：传送前需确认高度
        return StatCollector.translateToLocal("gtnotgood.journeymap.unknown");
    }

    static String height(int y) {
        // #tr gtnotgood.journeymap.height
        // # Landing height: %s
        // # zh_CN 落点高度：%s
        return StatCollector.translateToLocalFormatted("gtnotgood.journeymap.height", y);
    }

    static String confirm() {
        // #tr gtnotgood.journeymap.confirm
        // # Terrain is unknown. Teleport anyway?
        // # zh_CN 无法检查目的地地形，仍要传送吗？
        return StatCollector.translateToLocal("gtnotgood.journeymap.confirm");
    }

    static String destination(int x, int y, int z) {
        // #tr gtnotgood.journeymap.destination
        // # Destination: %s, %s, %s. Solid blocks, lava or a fall may be present.
        // # zh_CN 目的地：%s, %s, %s。可能存在实心方块、岩浆或坠落风险。
        return StatCollector.translateToLocalFormatted("gtnotgood.journeymap.destination", x, y, z);
    }
}
