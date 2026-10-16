package com.xyp.gtnotgood.common.parts.mestock;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;

enum StockText {

    // #tr gui.mestock.requester
    // # ME Requester
    // # zh_CN ME 自动请求器
    Requester("gui.mestock.requester"),
    // #tr gui.mestock.set_amount
    // # Middle-click to set the stock threshold.
    // # zh_CN 中键设置库存阈值。
    SetAmount("gui.mestock.set_amount"),
    // #tr gui.mestock.select_amount
    // # Select Amount
    // # zh_CN 选择数量
    SelectAmount("gui.mestock.select_amount"),
    // #tr gui.mestock.set
    // # Set
    // # zh_CN 设置
    Set("gui.mestock.set"),
    // #tr gui.mestock.submit
    // # Apply target and batch amounts
    // # zh_CN 应用目标量与每单量
    Submit("gui.mestock.submit"),
    // #tr gui.mestock.search
    // # Search
    // # zh_CN 搜索
    Search("gui.mestock.search"),
    // #tr gui.mestock.no_requesters
    // # No requesters
    // # zh_CN 没有请求器
    NoRequesters("gui.mestock.no_requesters"),

    // #tr gui.mestock.target
    // # Target / reserve
    // # zh_CN 目标量 / 保留量
    Target("gui.mestock.target"),
    // #tr gui.mestock.batch
    // # Batch (0 = auto)
    // # zh_CN 每单量（0 自动）
    Batch("gui.mestock.batch"),
    // #tr gui.mestock.lower
    // # Lower threshold
    // # zh_CN 下限
    Lower("gui.mestock.lower"),
    // #tr gui.mestock.upper
    // # Upper threshold
    // # zh_CN 上限
    Upper("gui.mestock.upper"),
    // #tr gui.mestock.above
    // # Export only ME stock above the threshold
    // # zh_CN 仅输出 ME 库存中超出阈值的部分
    Above("gui.mestock.above"),
    // #tr gui.mestock.below
    // # Export while ME stock is at or below the threshold
    // # zh_CN ME 库存不高于阈值时输出
    Below("gui.mestock.below"),
    // #tr gui.mestock.low_signal
    // # Signal below the lower threshold; stop at the upper
    // # zh_CN 低于下限时发信，达到上限时停止
    LowSignal("gui.mestock.low_signal"),
    // #tr gui.mestock.high_signal
    // # Signal at the upper threshold; stop below the lower
    // # zh_CN 达到上限时发信，低于下限时停止
    HighSignal("gui.mestock.high_signal"),
    // #tr gui.mestock.units
    // # Items: count. Fluids: L. Enter confirms.
    // # zh_CN 物品按个，流体按 L；回车确认。
    Units("gui.mestock.units"),
    // #tr gui.mestock.sample
    // # Place an item or fluid sample. Shift-click to clear.
    // # zh_CN 放入物品或流体样本，Shift 点击清空。
    Sample("gui.mestock.sample"),
    // #tr gui.mestock.enabled
    // # On
    // # zh_CN 开
    Enabled("gui.mestock.enabled"),
    // #tr gui.mestock.disabled
    // # Paused
    // # zh_CN 暂停
    Disabled("gui.mestock.disabled"),
    // #tr gui.mestock.ready
    // # Stocked
    // # zh_CN 库存充足
    Ready("gui.mestock.ready"),
    // #tr gui.mestock.waiting
    // # Queued
    // # zh_CN 等待调度
    Waiting("gui.mestock.waiting"),
    // #tr gui.mestock.calculating
    // # Calculating
    // # zh_CN 计算中
    Calculating("gui.mestock.calculating"),
    // #tr gui.mestock.crafting
    // # Crafting
    // # zh_CN 合成中
    Crafting("gui.mestock.crafting"),
    // #tr gui.mestock.materials
    // # Missing materials
    // # zh_CN 缺少材料
    Materials("gui.mestock.materials"),
    // #tr gui.mestock.cpu
    // # Waiting for CPU
    // # zh_CN 等待合成 CPU
    Cpu("gui.mestock.cpu"),
    // #tr gui.mestock.offline
    // # Offline / redstone paused
    // # zh_CN 离线或红石暂停
    Offline("gui.mestock.offline"),
    // #tr gui.mestock.pattern
    // # No crafting pattern
    // # zh_CN 没有可用样板
    Pattern("gui.mestock.pattern"),
    // #tr gui.mestock.failed
    // # Calculation failed; retrying
    // # zh_CN 计算失败，稍后重试
    Failed("gui.mestock.failed"),
    // #tr gui.mestock.redstone_ignore
    // # Redstone: ignored
    // # zh_CN 红石：忽略信号
    RedstoneIgnore("gui.mestock.redstone_ignore"),
    // #tr gui.mestock.redstone_high
    // # Redstone: run with signal
    // # zh_CN 红石：有信号运行
    RedstoneHigh("gui.mestock.redstone_high"),
    // #tr gui.mestock.redstone_low
    // # Redstone: run without signal
    // # zh_CN 红石：无信号运行
    RedstoneLow("gui.mestock.redstone_low"),
    // #tr gui.mestock.scheduling_default
    // # Scheduling: first to last
    // # zh_CN 调度顺序：从前往后
    SchedulingDefault("gui.mestock.scheduling_default"),
    // #tr gui.mestock.scheduling_round_robin
    // # Scheduling: round robin
    // # zh_CN 调度顺序：轮询
    SchedulingRoundRobin("gui.mestock.scheduling_round_robin"),
    // #tr gui.mestock.scheduling_random
    // # Scheduling: random
    // # zh_CN 调度顺序：随机
    SchedulingRandom("gui.mestock.scheduling_random"),
    // #tr gui.mestock.stored
    // # Stored
    // # zh_CN 当前库存
    Stored("gui.mestock.stored"),
    // #tr gui.mestock.pending
    // # In flight
    // # zh_CN 在途数量
    Pending("gui.mestock.pending"),
    // #tr gui.mestock.invalid
    // # Lower threshold must not exceed upper.
    // # zh_CN 下限不能大于上限。
    Invalid("gui.mestock.invalid"),
    // #tr gui.mestock.requester_hint
    // # Click to pause new requests. Submitted jobs continue until complete.
    // # zh_CN 点击暂停新请求，已提交的任务仍会继续完成。
    RequesterHint("gui.mestock.requester_hint"),
    // #tr gui.mestock.style_small
    // # Terminal height: small
    // # zh_CN 终端高度：小
    StyleSmall("gui.mestock.style_small"),
    // #tr gui.mestock.style_medium
    // # Terminal height: medium
    // # zh_CN 终端高度：中
    StyleMedium("gui.mestock.style_medium"),
    // #tr gui.mestock.style_tall
    // # Terminal height: tall
    // # zh_CN 终端高度：高
    StyleTall("gui.mestock.style_tall"),
    // #tr gui.mestock.style_full
    // # Terminal height: full
    // # zh_CN 终端高度：全高
    StyleFull("gui.mestock.style_full");

    private final String key;

    StockText(String key) {
        this.key = key;
    }

    IKey label() {
        return IKey.lang(key);
    }

    String text() {
        // LangKey.get() loads ClientScreenHandler; shared GUI construction also runs on physical servers.
        return StatCollector.translateToLocal(key);
    }
}
