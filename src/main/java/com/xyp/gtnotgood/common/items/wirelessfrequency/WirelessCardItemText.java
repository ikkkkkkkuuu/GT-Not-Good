// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.items.wirelessfrequency;

final class WirelessCardItemText {

    private WirelessCardItemText() {}

    static String key(String suffix) {
        return switch (suffix) {
            // #tr item.gtnotgood.wireless_frequency_card.name
            // # Wireless Frequency Card
            // # zh_CN 无线频率卡
            case "name" -> "item.gtnotgood.wireless_frequency_card.name";
            // #tr item.gtnotgood.wireless_frequency_card.tooltip.0
            // # Shift-right-click an ME Controller to save and select a source.
            // # zh_CN Shift 右键 ME 控制器，保存并选中发射源。
            case "tooltip.0" -> "item.gtnotgood.wireless_frequency_card.tooltip.0";
            // #tr item.gtnotgood.wireless_frequency_card.tooltip.1
            // # Right-click an ME device to connect or disconnect its whole cluster.
            // # zh_CN 右键 ME 设备，连接或断开其整个物理簇。
            case "tooltip.1" -> "item.gtnotgood.wireless_frequency_card.tooltip.1";
            // #tr item.gtnotgood.wireless_frequency_card.tooltip.2
            // # Shift-right-click air to remove the selected source.
            // # zh_CN Shift 右键空气，移除当前选中的发射源。
            case "tooltip.2" -> "item.gtnotgood.wireless_frequency_card.tooltip.2";
            // #tr item.gtnotgood.wireless_frequency_card.tooltip.3
            // # Use dense cable among ME interfaces: up to 32 channels per dense entrance, 8 per ordinary entrance.
            // # zh_CN 在 ME 接口簇中铺设致密线缆：致密入口最多 32 个频道，普通入口最多 8 个。
            case "tooltip.3" -> "item.gtnotgood.wireless_frequency_card.tooltip.3";
            // #tr item.gtnotgood.wireless_frequency_card.manage_hint
            // # Right-click air to open the manager.
            // # zh_CN 右键空气打开管理界面。
            case "manage_hint" -> "item.gtnotgood.wireless_frequency_card.manage_hint";
            // #tr item.gtnotgood.wireless_frequency_card.more_hint
            // # Hold Shift for controls.
            // # zh_CN 按住 Shift 查看操作。
            case "more_hint" -> "item.gtnotgood.wireless_frequency_card.more_hint";
            // #tr item.gtnotgood.wireless_frequency_card.unbound
            // # Unbound. Sneak-right-click an ME Controller.
            // # zh_CN 尚未绑定。请潜行右键 ME 控制器。
            case "unbound" -> "item.gtnotgood.wireless_frequency_card.unbound";
            // #tr item.gtnotgood.wireless_frequency_card.bind_hint
            // # Shift-right-click an ME Controller to bind. Shift-right-click air to remove the source.
            // # zh_CN Shift 右键 ME 控制器绑定发射源；Shift 右键空气移除发射源。
            case "bind_hint" -> "item.gtnotgood.wireless_frequency_card.bind_hint";
            // #tr item.gtnotgood.wireless_frequency_card.bound
            // # Source: dimension %s, position (%s, %s, %s).
            // # zh_CN 发射源：维度 %s，坐标 (%s, %s, %s)。
            case "bound" -> "item.gtnotgood.wireless_frequency_card.bound";
            // #tr item.gtnotgood.wireless_frequency_card.owner
            // # Bound player: %s
            // # zh_CN 绑定玩家：%s
            case "owner" -> "item.gtnotgood.wireless_frequency_card.owner";
            // #tr item.gtnotgood.wireless_frequency_card.source_unbound
            // # No source selected. Shift-right-click an ME Controller to bind one.
            // # zh_CN 尚未选择发射源，请 Shift 右键 ME 控制器进行绑定。
            case "source_unbound" -> "item.gtnotgood.wireless_frequency_card.source_unbound";
            // #tr item.gtnotgood.wireless_frequency_card.owner_none
            // # None
            // # zh_CN 无
            case "owner_none" -> "item.gtnotgood.wireless_frequency_card.owner_none";
            // #tr item.gtnotgood.wireless_frequency_card.wrong_owner
            // # Only the bound player %s can use, rebind or clear this card.
            // # zh_CN 这张卡只能由绑定玩家 %s 使用、重绑或清除。
            case "wrong_owner" -> "item.gtnotgood.wireless_frequency_card.wrong_owner";
            // #tr item.gtnotgood.wireless_frequency_card.denied
            // # You do not have permission to modify this position or ME network.
            // # zh_CN 你没有修改此位置或 ME 网络的权限。
            case "denied" -> "item.gtnotgood.wireless_frequency_card.denied";
            // #tr item.gtnotgood.wireless_frequency_card.invalid_target
            // # No usable ME node at the hit position. Aim at a cable or part.
            // # zh_CN 命中位置没有可用的 ME 节点。请瞄准线缆本体或部件。
            case "invalid_target" -> "item.gtnotgood.wireless_frequency_card.invalid_target";
            // #tr item.gtnotgood.wireless_frequency_card.unavailable
            // # The bound ME Controller or node is unavailable. Ensure its chunk is loaded.
            // # zh_CN 绑定的 ME 控制器或节点不可用，请确认发射端区块已加载。
            case "unavailable" -> "item.gtnotgood.wireless_frequency_card.unavailable";
            // #tr item.gtnotgood.wireless_frequency_card.unstable
            // # The controller network is booting or invalid. Wait until it is stable.
            // # zh_CN 控制器网络正在启动或结构无效，请待网络稳定后再操作。
            case "unstable" -> "item.gtnotgood.wireless_frequency_card.unstable";
            // #tr item.gtnotgood.wireless_frequency_card.failed
            // # Connection failed: %s
            // # zh_CN 连接失败：%s
            case "failed" -> "item.gtnotgood.wireless_frequency_card.failed";
            // #tr item.gtnotgood.wireless_frequency_card.cleared
            // # Selected source removed. Existing links remain; other saved sources are retained.
            // # zh_CN 已移除当前发射源，保留已有连接和其他已保存的发射源。
            case "cleared" -> "item.gtnotgood.wireless_frequency_card.cleared";
            // #tr item.gtnotgood.wireless_frequency_card.source_limit
            // # Up to %s sources can be saved. Remove a source before adding another.
            // # zh_CN 最多保存 %s 个发射源；请先移除一个再添加。
            case "source_limit" -> "item.gtnotgood.wireless_frequency_card.source_limit";
            // #tr item.gtnotgood.wireless_frequency_card.status
            // # Source network geometric budget: %s channels; active wireless entrances: %s.
            // # zh_CN 源网络几何预算：%s 频道；当前无线入口：%s 个。
            case "status" -> "item.gtnotgood.wireless_frequency_card.status";
            // #tr item.gtnotgood.wireless_frequency_card.allocation
            // # Last pathing allocation (including wired devices): %s / %s channels.
            // # zh_CN 最近一次寻路分配（含有线）：%s / %s 频道。
            case "allocation" -> "item.gtnotgood.wireless_frequency_card.allocation";
            // #tr item.gtnotgood.wireless_frequency_card.no_allocation
            // # No valid channel allocation yet. Create an entrance and wait for pathing.
            // # zh_CN 尚无有效的频道分配结果，请建立入口并等待网络稳定。
            case "no_allocation" -> "item.gtnotgood.wireless_frequency_card.no_allocation";
            // #tr item.gtnotgood.wireless_frequency_card.channels_disabled
            // # AE2 channels are disabled; the geometric budget above is not enforced.
            // # zh_CN AE2 已关闭频道限制，上述几何预算不作为上限。
            case "channels_disabled" -> "item.gtnotgood.wireless_frequency_card.channels_disabled";
            // #tr item.gtnotgood.wireless_frequency_card.cluster_connected
            // # Connected the ME cluster. Click any node to disconnect; wait for pathing before checking status.
            // # zh_CN 已连接整个物理 ME 簇。簇内任意节点均可断开；等待网络稳定后查看状态。
            case "cluster_connected" -> "item.gtnotgood.wireless_frequency_card.cluster_connected";
            // #tr item.gtnotgood.wireless_frequency_card.cluster_disconnected
            // # Removed this card's entrances and claims from the cluster. Other frequencies remain.
            // # zh_CN 已解除该物理 ME 簇与本卡频率的全部入口及绑定，其他频率记录保留。
            case "cluster_disconnected" -> "item.gtnotgood.wireless_frequency_card.cluster_disconnected";
            // #tr item.gtnotgood.wireless_frequency_card.cluster_status
            // # This frequency: %s cluster links; %s connected, %s frequency conflicts, %s waiting for recovery.
            // # zh_CN 本卡频率：%s 个簇链接，入口已连接 %s，异频冲突 %s，等待恢复 %s。
            case "cluster_status" -> "item.gtnotgood.wireless_frequency_card.cluster_status";
            // #tr item.gtnotgood.wireless_frequency_card.scan_limit
            // # Cluster scan exceeded 32768 nodes. Wireless entrances pause until the network is reduced.
            // # zh_CN 物理簇扫描超过 32768 个节点，所有无线入口暂停；绑定保留，缩小网络后恢复。
            case "scan_limit" -> "item.gtnotgood.wireless_frequency_card.scan_limit";
            // #tr item.gtnotgood.wireless_frequency_card.planner_status
            // # Automatic entrances: %s
            // # zh_CN 自动入口：%s
            case "planner_status" -> "item.gtnotgood.wireless_frequency_card.planner_status";
            // #tr item.gtnotgood.wireless_frequency_card.planner.waiting
            // # Waiting for settled pathing or the next check
            // # zh_CN 等待网络稳定或下一轮检查
            case "planner.waiting" -> "item.gtnotgood.wireless_frequency_card.planner.waiting";
            // #tr item.gtnotgood.wireless_frequency_card.planner.disabled
            // # AE2 channels disabled; channel expansion is unnecessary
            // # zh_CN AE2 频道已关闭，无需按频道增容
            case "planner.disabled" -> "item.gtnotgood.wireless_frequency_card.planner.disabled";
            // #tr item.gtnotgood.wireless_frequency_card.planner.unpowered
            // # Network unpowered; expansion paused
            // # zh_CN 网络缺电，暂停增容
            case "planner.unpowered" -> "item.gtnotgood.wireless_frequency_card.planner.unpowered";
            // #tr item.gtnotgood.wireless_frequency_card.planner.satisfied
            // # Current channel demand of carry-capable nodes is satisfied
            // # zh_CN 已满足当前可承载节点的频道需求
            case "planner.satisfied" -> "item.gtnotgood.wireless_frequency_card.planner.satisfied";
            // #tr item.gtnotgood.wireless_frequency_card.planner.budget_full
            // # Shared controller budget exhausted; no more entrances
            // # zh_CN 控制器总预算已用尽，停止增加入口
            case "planner.budget_full" -> "item.gtnotgood.wireless_frequency_card.planner.budget_full";
            // #tr item.gtnotgood.wireless_frequency_card.planner.entrance_limit
            // # Wireless entrance limit reached
            // # zh_CN 无线入口数量已达上限
            case "planner.entrance_limit" -> "item.gtnotgood.wireless_frequency_card.planner.entrance_limit";
            // #tr item.gtnotgood.wireless_frequency_card.planner.expanding
            // # Trying an entrance or verifying its benefit
            // # zh_CN 正在试建入口或验证增容效果
            case "planner.expanding" -> "item.gtnotgood.wireless_frequency_card.planner.expanding";
            // #tr item.gtnotgood.wireless_frequency_card.planner.rolling_back
            // # Removing an unproductive entrance and awaiting pathing
            // # zh_CN 撤回无收益入口，等待重新分配
            case "planner.rolling_back" -> "item.gtnotgood.wireless_frequency_card.planner.rolling_back";
            // #tr item.gtnotgood.wireless_frequency_card.planner.no_candidate
            // # No viable candidate; retry after topology or demand changes
            // # zh_CN 暂无有效候选，拓扑或需求变化后重试
            case "planner.no_candidate" -> "item.gtnotgood.wireless_frequency_card.planner.no_candidate";
            // #tr item.gtnotgood.wireless_frequency_card.auto_on
            // # Auto-connect: ON
            // # zh_CN 自动连接：开启
            case "auto_on" -> "item.gtnotgood.wireless_frequency_card.auto_on";
            // #tr item.gtnotgood.wireless_frequency_card.auto_off
            // # Auto-connect: OFF
            // # zh_CN 自动连接：关闭
            case "auto_off" -> "item.gtnotgood.wireless_frequency_card.auto_off";
            // #tr item.gtnotgood.wireless_frequency_card.visualisation_scroll_hint
            // # Shift-scroll to cycle network views: up for previous, down for next.
            // # zh_CN Shift 滚轮切换网络视图：上滚为上一项，下滚为下一项。
            case "visualisation_scroll_hint" -> "item.gtnotgood.wireless_frequency_card.visualisation_scroll_hint";
            // #tr item.gtnotgood.wireless_frequency_card.auto_ambiguous
            // # Multiple usable frequency cards. Hold the intended card or disable auto-connect on the others.
            // # zh_CN 有多张可用频率卡，请手持要操作的卡，或关闭其他卡的自动连接。
            case "auto_ambiguous" -> "item.gtnotgood.wireless_frequency_card.auto_ambiguous";
            // #tr item.gtnotgood.wireless_frequency_card.auto_no_card
            // # No frequency card you can operate was found in your inventory or bauble slots.
            // # zh_CN 背包或饰品栏中没有可由你操作的频率卡。
            case "auto_no_card" -> "item.gtnotgood.wireless_frequency_card.auto_no_card";
            // #tr item.gtnotgood.wireless_frequency_card.bauble_hint
            // # Can be worn as a bauble. Manual linking requires holding the card.
            // # zh_CN 可佩戴于饰品栏，手动连接时须手持此卡。
            case "bauble_hint" -> "item.gtnotgood.wireless_frequency_card.bauble_hint";
            // #tr item.gtnotgood.wireless_frequency_card.auto_busy
            // # Auto-connect queue is full. Connect this device manually later.
            // # zh_CN 自动连接队列已满，请稍后手动连接该设备。
            case "auto_busy" -> "item.gtnotgood.wireless_frequency_card.auto_busy";
            // #tr item.gtnotgood.wireless_frequency_card.auto_unavailable
            // # Auto-connect timed out. Load the source, check its network, then connect manually.
            // # zh_CN 自动连接等待超时，请确认发射端已加载且网络有效，再手动连接目标。
            case "auto_unavailable" -> "item.gtnotgood.wireless_frequency_card.auto_unavailable";
            // #tr item.gtnotgood.wireless_frequency_card.auto_failed
            // # Auto-connect failed: frequency conflict, controller, or AE security. Inspect with the card.
            // # zh_CN 未能自动连接：目标可能属于其他频率、含有控制器或被 AE 安全规则拒绝。可用卡手动检查。
            case "auto_failed" -> "item.gtnotgood.wireless_frequency_card.auto_failed";
            // #tr item.gtnotgood.wireless_frequency_card.gui_ambiguous
            // # Multiple usable frequency cards found. Hold the card whose manager you want to open.
            // # zh_CN 有多张可用频率卡，请手持要打开管理界面的卡。
            case "gui_ambiguous" -> "item.gtnotgood.wireless_frequency_card.gui_ambiguous";
            default -> "item.gtnotgood.wireless_frequency_card." + suffix;
        };
    }
}
