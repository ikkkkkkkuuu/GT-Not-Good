// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

/** Server-synchronized infusion feedback shown in the original core-slot tooltip. */
public enum AltarStatus {

    // #tr gui.packaged.altar_idle
    // # Altar: idle
    // # zh_CN 祭坛：空闲
    IDLE("gui.packaged.altar_idle"),
    // #tr gui.packaged.altar_running
    // # Altar: active job or waiting for return
    // # zh_CN 祭坛：任务进行中或等待回收
    RUNNING("gui.packaged.altar_running"),
    // #tr gui.packaged.altar_essentia
    // # Altar: waiting for AE essentia, power or channel
    // # zh_CN 祭坛：等待 AE 源质、电力或频道
    ESSENTIA("gui.packaged.altar_essentia"),
    // #tr gui.packaged.altar_inactive
    // # Altar: activate the structure with a wand first
    // # zh_CN 祭坛：请先使用法杖激活结构
    INACTIVE("gui.packaged.altar_inactive"),
    // #tr gui.packaged.altar_owner
    // # Altar: owner must be online in this dimension
    // # zh_CN 祭坛：放置者须在本维度在线
    OWNER("gui.packaged.altar_owner"),
    // #tr gui.packaged.altar_unloaded
    // # Altar: waiting for loaded chunks
    // # zh_CN 祭坛：等待区块加载
    UNLOADED("gui.packaged.altar_unloaded"),
    // #tr gui.packaged.altar_occupied
    // # Altar: structure or pedestal contents prevent dispatch
    // # zh_CN 祭坛：结构或基座物品阻止派单
    OCCUPIED("gui.packaged.altar_occupied"),
    // #tr gui.packaged.altar_recipe
    // # Altar: check research, ingredients and the encoded output
    // # zh_CN 祭坛：请检查研究、材料与样板产物
    RECIPE("gui.packaged.altar_recipe"),
    // #tr gui.packaged.altar_interrupted
    // # Altar stopped without its expected output; inspect it before releasing the job.
    // # zh_CN 祭坛已停止且缺少预期产物；检查祭坛后可释放中断任务。
    INTERRUPTED("gui.packaged.altar_interrupted"),
    // #tr gui.packaged.returns_full
    // # Return inventory full; result remains at the altar.
    // # zh_CN 回收槽已满；产物保留在祭坛。
    RETURNS_FULL("gui.packaged.returns_full");

    public final String key;

    AltarStatus(String key) {
        this.key = key;
    }
}
