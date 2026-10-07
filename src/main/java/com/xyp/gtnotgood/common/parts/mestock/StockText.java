package com.xyp.gtnotgood.common.parts.mestock;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;

enum StockText {

    // #tr gui.mestock.requester
    // # ME Requester
    // # zh_CN ME 自动请求器
    Requester("gui.mestock.requester"),
    // #tr gui.mestock.set_amount
    // # Middle click to set amount
    // # zh_CN 中键设置数量
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
    // # Submit request settings
    // # zh_CN 提交请求设置
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
    // # Export surplus above threshold
    // # zh_CN 高于阈值：输出超出部分
    Above("gui.mestock.above"),
    // #tr gui.mestock.below
    // # Export when at/below threshold
    // # zh_CN 不高于阈值时输出
    Below("gui.mestock.below"),
    // #tr gui.mestock.low_signal
    // # Signal below lower; stop at upper
    // # zh_CN 低于下限发信，达到上限停止
    LowSignal("gui.mestock.low_signal"),
    // #tr gui.mestock.high_signal
    // # Signal at upper; stop below lower
    // # zh_CN 达到上限发信，低于下限停止
    HighSignal("gui.mestock.high_signal"),
    // #tr gui.mestock.units
    // # Items: count. Fluids: L. Enter confirms.
    // # zh_CN 物品按个，流体按 L；回车确认。
    Units("gui.mestock.units"),
    // #tr gui.mestock.sample
    // # Click to select; drag a sample. Shift-click clears.
    // # zh_CN 点击选槽，放入样本；Shift 点击清空。
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
    CPU("gui.mestock.cpu"),
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
    // # Pausing stops new orders; running jobs finish normally.
    // # zh_CN 暂停只停止新订单，已提交订单继续完成。
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
