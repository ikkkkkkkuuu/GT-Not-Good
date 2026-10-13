// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.client.wirelessfrequency;

import net.minecraft.util.StatCollector;

final class WirelessCardText {

    private WirelessCardText() {}

    static String tr(String suffix, Object... args) {
        String key = switch (suffix) {
            // #tr gtnotgood.wireless.gui.source_delete
            // # Delete
            // # zh_CN 删除
            case "source_delete" -> "gtnotgood.wireless.gui.source_delete";
            // #tr gtnotgood.wireless.gui.source_delete_hint
            // # Remove this saved source from the card; existing wireless links remain.
            // # zh_CN 从卡片移除此发射源；已有无线连接保持不变。
            case "source_delete_hint" -> "gtnotgood.wireless.gui.source_delete_hint";
            // #tr gtnotgood.wireless.gui.sources_title
            // # Wireless Frequency Card · Sources
            // # zh_CN 无线频率卡 · 发射源
            case "sources_title" -> "gtnotgood.wireless.gui.sources_title";
            // #tr gtnotgood.wireless.gui.sources_hint
            // # Sneak-right-click an ME Controller to add a source.
            // # zh_CN 潜行右键 ME 控制器可添加发射源。
            case "sources_hint" -> "gtnotgood.wireless.gui.sources_hint";
            // #tr gtnotgood.wireless.gui.sources_auto_hint
            // # Auto-connect uses the selected source; existing links stay unchanged.
            // # zh_CN 自动连接使用当前选中的源；已有连接保持不变。
            case "sources_auto_hint" -> "gtnotgood.wireless.gui.sources_auto_hint";
            // #tr gtnotgood.wireless.gui.sources_open
            // # Click to select a saved source network.
            // # zh_CN 点击选择已保存的源网络。
            case "sources_open" -> "gtnotgood.wireless.gui.sources_open";
            // #tr gtnotgood.wireless.gui.source_loaded
            // # Loaded
            // # zh_CN 已加载
            case "source_loaded" -> "gtnotgood.wireless.gui.source_loaded";
            // #tr gtnotgood.wireless.gui.source_unloaded
            // # Unloaded or unavailable
            // # zh_CN 未加载或不可用
            case "source_unloaded" -> "gtnotgood.wireless.gui.source_unloaded";
            // #tr gtnotgood.wireless.gui.source_selected
            // # Selected
            // # zh_CN 已选中
            case "source_selected" -> "gtnotgood.wireless.gui.source_selected";
            // #tr gtnotgood.wireless.gui.source_select
            // # Select
            // # zh_CN 选择
            case "source_select" -> "gtnotgood.wireless.gui.source_select";
            // #tr gtnotgood.wireless.gui.title
            // # Wireless Frequency Card · Network
            // # zh_CN 无线频率卡 · 网络管理
            case "title" -> "gtnotgood.wireless.gui.title";
            // #tr gtnotgood.wireless.gui.shared
            // # Shared: %s
            // # zh_CN 共用：%s
            case "shared" -> "gtnotgood.wireless.gui.shared";
            // #tr gtnotgood.wireless.gui.budget_compact
            // # Source channels: %s / %s
            // # zh_CN 源网络频道：%s / %s
            case "budget_compact" -> "gtnotgood.wireless.gui.budget_compact";
            // #tr gtnotgood.wireless.gui.online_metric
            // # Online %s/%s
            // # zh_CN 在线 %s/%s
            case "online_metric" -> "gtnotgood.wireless.gui.online_metric";
            // #tr gtnotgood.wireless.gui.missing_metric
            // # Missing channels %s
            // # zh_CN 缺少频道 %s
            case "missing_metric" -> "gtnotgood.wireless.gui.missing_metric";
            // #tr gtnotgood.wireless.gui.online_compact
            // # %s/%s
            // # zh_CN 在线 %s/%s
            case "online_compact" -> "gtnotgood.wireless.gui.online_compact";
            // #tr gtnotgood.wireless.gui.missing_compact
            // # Miss %s
            // # zh_CN 缺少 %s
            case "missing_compact" -> "gtnotgood.wireless.gui.missing_compact";
            // #tr gtnotgood.wireless.gui.entrances_compact
            // # In %s
            // # zh_CN 入口 %s
            case "entrances_compact" -> "gtnotgood.wireless.gui.entrances_compact";
            // #tr gtnotgood.wireless.gui.short_state.active
            // # Linked
            // # zh_CN 已连接
            case "short_state.active" -> "gtnotgood.wireless.gui.short_state.active";
            // #tr gtnotgood.wireless.gui.short_state.waiting
            // # Waiting
            // # zh_CN 等待恢复
            case "short_state.waiting" -> "gtnotgood.wireless.gui.short_state.waiting";
            // #tr gtnotgood.wireless.gui.short_state.conflict
            // # Conflict
            // # zh_CN 频率冲突
            case "short_state.conflict" -> "gtnotgood.wireless.gui.short_state.conflict";
            // #tr gtnotgood.wireless.gui.short_state.paused
            // # Paused
            // # zh_CN 已暂停
            case "short_state.paused" -> "gtnotgood.wireless.gui.short_state.paused";
            // #tr gtnotgood.wireless.gui.short_state.limited
            // # Limited
            // # zh_CN 扫描受限
            case "short_state.limited" -> "gtnotgood.wireless.gui.short_state.limited";
            // #tr gtnotgood.wireless.gui.copy
            // # Copy
            // # zh_CN 复制
            case "copy" -> "gtnotgood.wireless.gui.copy";
            // #tr gtnotgood.wireless.gui.highlight
            // # Mark
            // # zh_CN 高亮
            case "highlight" -> "gtnotgood.wireless.gui.highlight";
            // #tr gtnotgood.wireless.gui.teleport
            // # TP
            // # zh_CN 传送
            case "teleport" -> "gtnotgood.wireless.gui.teleport";
            // #tr gtnotgood.wireless.gui.teleport_unavailable
            // # The target device or source is not loaded. Teleport is unavailable.
            // # zh_CN 目标设备或发射端尚未加载，暂时无法传送。
            case "teleport_unavailable" -> "gtnotgood.wireless.gui.teleport_unavailable";
            // #tr gtnotgood.wireless.gui.teleport_no_space
            // # No safe landing space near the device. Clear ground and two blocks of headroom.
            // # zh_CN 设备附近没有安全落脚点，请留出实心地面和两格头顶空间。
            case "teleport_no_space" -> "gtnotgood.wireless.gui.teleport_no_space";
            // #tr gtnotgood.wireless.gui.teleport_failed
            // # Unable to teleport right now.
            // # zh_CN 当前无法完成传送。
            case "teleport_failed" -> "gtnotgood.wireless.gui.teleport_failed";
            // #tr gtnotgood.wireless.gui.teleport_done
            // # Teleported near the device: D%s %s, %s, %s.
            // # zh_CN 已传送至设备附近：D%s %s, %s, %s。
            case "teleport_done" -> "gtnotgood.wireless.gui.teleport_done";
            // #tr gtnotgood.wireless.gui.copied
            // # Copied dimension, coordinates and part side to the clipboard.
            // # zh_CN 已复制目标维度、坐标和部件方向到剪贴板。
            case "copied" -> "gtnotgood.wireless.gui.copied";
            // #tr gtnotgood.wireless.gui.save_name
            // # Save name
            // # zh_CN 保存备注
            case "save_name" -> "gtnotgood.wireless.gui.save_name";
            // #tr gtnotgood.wireless.gui.name_label
            // # Name
            // # zh_CN 备注
            case "name_label" -> "gtnotgood.wireless.gui.name_label";
            // #tr gtnotgood.wireless.gui.search
            // # Find
            // # zh_CN 查找
            case "search" -> "gtnotgood.wireless.gui.search";
            // #tr gtnotgood.wireless.gui.search_placeholder
            // # Name / coordinates
            // # zh_CN 名称 / 坐标
            case "search_placeholder" -> "gtnotgood.wireless.gui.search_placeholder";
            // #tr gtnotgood.wireless.gui.filter.0
            // # All states
            // # zh_CN 全部状态
            case "filter.0" -> "gtnotgood.wireless.gui.filter.0";
            // #tr gtnotgood.wireless.gui.filter.1
            // # Paused
            // # zh_CN 已暂停
            case "filter.1" -> "gtnotgood.wireless.gui.filter.1";
            // #tr gtnotgood.wireless.gui.filter.2
            // # Conflict
            // # zh_CN 频率冲突
            case "filter.2" -> "gtnotgood.wireless.gui.filter.2";
            // #tr gtnotgood.wireless.gui.filter.3
            // # Connected
            // # zh_CN 已连接
            case "filter.3" -> "gtnotgood.wireless.gui.filter.3";
            // #tr gtnotgood.wireless.gui.filter.4
            // # Waiting
            // # zh_CN 等待恢复
            case "filter.4" -> "gtnotgood.wireless.gui.filter.4";
            // #tr gtnotgood.wireless.gui.filter_invalid
            // # Invalid filter: enter an integer dimension ID or leave it blank.
            // # zh_CN 筛选无效：维度应填写整数或留空。
            case "filter_invalid" -> "gtnotgood.wireless.gui.filter_invalid";
            // #tr gtnotgood.wireless.gui.no_matches
            // # No matching loaded clusters. Clear the search and dimension, then select All states.
            // # zh_CN 没有匹配的已加载簇。清空搜索、维度并选择全部状态可恢复列表。
            case "no_matches" -> "gtnotgood.wireless.gui.no_matches";
            // #tr gtnotgood.wireless.gui.highlight_dimension
            // # Target is in dimension %s. Travel there before highlighting it.
            // # zh_CN 目标在维度 %s，请前往该维度后再高亮。
            case "highlight_dimension" -> "gtnotgood.wireless.gui.highlight_dimension";
            // #tr gtnotgood.wireless.gui.highlight_started
            // # Marked %s positions for 5 seconds (loaded targets within 256 blocks; max 512).
            // # zh_CN 已标记 %s 个位置，持续 5 秒（仅显示 256 格内已加载目标，最多 512 个）。
            case "highlight_started" -> "gtnotgood.wireless.gui.highlight_started";
            // #tr gtnotgood.wireless.gui.detail_title
            // # Wireless Entrances
            // # zh_CN 无线入口明细
            case "detail_title" -> "gtnotgood.wireless.gui.detail_title";
            // #tr gtnotgood.wireless.gui.matched
            // # Matches: %s
            // # zh_CN 匹配簇：%s
            case "matched" -> "gtnotgood.wireless.gui.matched";
            // #tr gtnotgood.wireless.gui.auto_on
            // # Auto-connect: ON
            // # zh_CN 自动连接：开启
            case "auto_on" -> "gtnotgood.wireless.gui.auto_on";
            // #tr gtnotgood.wireless.gui.auto_off
            // # Auto-connect: OFF
            // # zh_CN 自动连接：关闭
            case "auto_off" -> "gtnotgood.wireless.gui.auto_off";
            // #tr gtnotgood.wireless.gui.visualisation
            // # Network view: %s
            // # zh_CN 网络可视化：%s
            case "visualisation" -> "gtnotgood.wireless.gui.visualisation";
            // #tr gtnotgood.wireless.gui.visualisation_button
            // # View: %s
            // # zh_CN 显示：%s
            case "visualisation_button" -> "gtnotgood.wireless.gui.visualisation_button";
            // #tr gtnotgood.wireless.gui.back_compact
            // # Back
            // # zh_CN 返回
            case "back_compact" -> "gtnotgood.wireless.gui.back_compact";
            // #tr gtnotgood.wireless.gui.visualisation.full.short
            // # All
            // # zh_CN 全部
            case "visualisation.full.short" -> "gtnotgood.wireless.gui.visualisation.full.short";
            // #tr gtnotgood.wireless.gui.visualisation.nodes.short
            // # Nodes
            // # zh_CN 节点
            case "visualisation.nodes.short" -> "gtnotgood.wireless.gui.visualisation.nodes.short";
            // #tr gtnotgood.wireless.gui.visualisation.channels.short
            // # Channels
            // # zh_CN 频道
            case "visualisation.channels.short" -> "gtnotgood.wireless.gui.visualisation.channels.short";
            // #tr gtnotgood.wireless.gui.visualisation.nonum.short
            // # Nodes+links
            // # zh_CN 节点+连线
            case "visualisation.nonum.short" -> "gtnotgood.wireless.gui.visualisation.nonum.short";
            // #tr gtnotgood.wireless.gui.visualisation.nodes_one_channel.short
            // # N.+source
            // # zh_CN 节点+源连线
            case "visualisation.nodes_one_channel.short" -> "gtnotgood.wireless.gui.visualisation.nodes_one_channel.short";
            // #tr gtnotgood.wireless.gui.visualisation.one_channel.short
            // # Source
            // # zh_CN 源连线
            case "visualisation.one_channel.short" -> "gtnotgood.wireless.gui.visualisation.one_channel.short";
            // #tr gtnotgood.wireless.gui.visualisation.p2p.short
            // # P2P
            // # zh_CN P2P
            case "visualisation.p2p.short" -> "gtnotgood.wireless.gui.visualisation.p2p.short";
            // #tr gtnotgood.wireless.gui.visualisation.proxy.short
            // # Proxies
            // # zh_CN 合成代理
            case "visualisation.proxy.short" -> "gtnotgood.wireless.gui.visualisation.proxy.short";
            // #tr gtnotgood.wireless.gui.visualisation_off
            // # Off
            // # zh_CN 关闭
            case "visualisation_off" -> "gtnotgood.wireless.gui.visualisation_off";
            // #tr gtnotgood.wireless.gui.visualisation.full
            // # Everything (with channel counts)
            // # zh_CN 全部（含频道数）
            case "visualisation.full" -> "gtnotgood.wireless.gui.visualisation.full";
            // #tr gtnotgood.wireless.gui.visualisation.nodes
            // # Nodes
            // # zh_CN 节点
            case "visualisation.nodes" -> "gtnotgood.wireless.gui.visualisation.nodes";
            // #tr gtnotgood.wireless.gui.visualisation.channels
            // # Channel links
            // # zh_CN 频道连线
            case "visualisation.channels" -> "gtnotgood.wireless.gui.visualisation.channels";
            // #tr gtnotgood.wireless.gui.visualisation.nonum
            // # Nodes and links (no numbers)
            // # zh_CN 节点与连线（无数字）
            case "visualisation.nonum" -> "gtnotgood.wireless.gui.visualisation.nonum";
            // #tr gtnotgood.wireless.gui.visualisation.nodes_one_channel
            // # Nodes and remote source links
            // # zh_CN 节点与源节点远程连线
            case "visualisation.nodes_one_channel" -> "gtnotgood.wireless.gui.visualisation.nodes_one_channel";
            // #tr gtnotgood.wireless.gui.visualisation.one_channel
            // # Remote source links
            // # zh_CN 源节点远程连线
            case "visualisation.one_channel" -> "gtnotgood.wireless.gui.visualisation.one_channel";
            // #tr gtnotgood.wireless.gui.visualisation.p2p
            // # P2P links
            // # zh_CN P2P 连线
            case "visualisation.p2p" -> "gtnotgood.wireless.gui.visualisation.p2p";
            // #tr gtnotgood.wireless.gui.visualisation.proxy
            // # Crafting input proxy links
            // # zh_CN 合成输入代理连线
            case "visualisation.proxy" -> "gtnotgood.wireless.gui.visualisation.proxy";
            // #tr gtnotgood.wireless.gui.visualisation_hint
            // # Hold or wear the card to view the network. Switch views here or by hotkey.
            // # zh_CN 手持或佩戴此卡查看网络；可在此处或用快捷键切换视图。
            case "visualisation_hint" -> "gtnotgood.wireless.gui.visualisation_hint";
            // #tr gtnotgood.wireless.gui.refresh
            // # Refresh
            // # zh_CN 刷新
            case "refresh" -> "gtnotgood.wireless.gui.refresh";
            // #tr gtnotgood.wireless.gui.source
            // # Source: %s
            // # zh_CN 发射源：%s
            case "source" -> "gtnotgood.wireless.gui.source";
            // #tr gtnotgood.wireless.gui.unbound
            // # Frequency card is unbound
            // # zh_CN 频率卡尚未绑定
            case "unbound" -> "gtnotgood.wireless.gui.unbound";
            // #tr gtnotgood.wireless.gui.bind_hint
            // # Sneak-use an ME Controller to bind
            // # zh_CN 先潜行右击 ME 控制器绑定频率
            case "bind_hint" -> "gtnotgood.wireless.gui.bind_hint";
            // #tr gtnotgood.wireless.gui.source_waiting
            // # Source unloaded; waiting for recovery
            // # zh_CN 发射端未加载，等待恢复
            case "source_waiting" -> "gtnotgood.wireless.gui.source_waiting";
            // #tr gtnotgood.wireless.gui.unlimited
            // # AE2 channel limits are disabled
            // # zh_CN AE2 频道限制已关闭
            case "unlimited" -> "gtnotgood.wireless.gui.unlimited";
            // #tr gtnotgood.wireless.gui.empty
            // # No loaded clusters; unloaded records are retained
            // # zh_CN 没有已加载的簇；未加载区块的记录仍保留
            case "empty" -> "gtnotgood.wireless.gui.empty";
            // #tr gtnotgood.wireless.gui.loaded
            // # Loaded: %s
            // # zh_CN 已加载簇：%s
            case "loaded" -> "gtnotgood.wireless.gui.loaded";
            // #tr gtnotgood.wireless.gui.initializing
            // # Identifying multiblocks... The list will appear when their startup checks finish.
            // # zh_CN 正在识别多方块… 启动检查完成后将显示列表。
            case "initializing" -> "gtnotgood.wireless.gui.initializing";
            // #tr gtnotgood.wireless.gui.entrances
            // # Entrances %s
            // # zh_CN 入口 %s
            case "entrances" -> "gtnotgood.wireless.gui.entrances";
            // #tr gtnotgood.wireless.gui.usage
            // # Entrance channels: %s
            // # zh_CN 当前入口频道占用：%s
            case "usage" -> "gtnotgood.wireless.gui.usage";
            // #tr gtnotgood.wireless.gui.usage_waiting
            // # Channels: waiting for pathing
            // # zh_CN 频道占用：等待网络稳定
            case "usage_waiting" -> "gtnotgood.wireless.gui.usage_waiting";
            // #tr gtnotgood.wireless.gui.usage_disabled
            // # Channel limits disabled
            // # zh_CN 已关闭频道限制
            case "usage_disabled" -> "gtnotgood.wireless.gui.usage_disabled";
            // #tr gtnotgood.wireless.gui.no_runtime
            // # No runtime entrance
            // # zh_CN 当前无运行时入口
            case "no_runtime" -> "gtnotgood.wireless.gui.no_runtime";
            // #tr gtnotgood.wireless.gui.denied
            // # Cannot change: target changed, AE permission denied, or source unloaded.
            // # zh_CN 无法修改：目标已变化、AE 权限不足或发射源未加载。
            case "denied" -> "gtnotgood.wireless.gui.denied";
            // #tr gtnotgood.wireless.gui.stale
            // # Physical cluster changed. Retry after refreshing.
            // # zh_CN 物理簇已变化，请在刷新后重新操作。
            case "stale" -> "gtnotgood.wireless.gui.stale";
            default -> "gtnotgood.wireless.gui." + suffix;
        };
        return StatCollector.translateToLocalFormatted(key, args);
    }
}
