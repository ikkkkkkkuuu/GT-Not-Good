// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

/** Server-synchronized adapter feedback shown in the original core-slot tooltip. */
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
    RETURNS_FULL("gui.packaged.returns_full"),
    // #tr gui.packaged.blood_recipe
    // # Blood Altar: check tier, single-step ingredients and output.
    // # zh_CN 血祭坛：请检查等级、单步配方材料与产物。
    BLOOD_RECIPE("gui.packaged.blood_recipe"),
    // #tr gui.packaged.arcane_owner
    // # Workbench: owner must be online here and allowed to use the target.
    // # zh_CN 工作台：所有者须在本维度在线且有权使用目标。
    ARCANE_OWNER("gui.packaged.arcane_owner"),
    // #tr gui.packaged.arcane_occupied
    // # Workbench: clear the grid and close its screen.
    // # zh_CN 工作台：请清空九宫格并关闭其界面。
    ARCANE_OCCUPIED("gui.packaged.arcane_occupied"),
    // #tr gui.packaged.arcane_wand
    // # Workbench: insert a crafting wand or sceptre.
    // # zh_CN 工作台：请放入可合成的法杖或权杖。
    ARCANE_WAND("gui.packaged.arcane_wand"),
    // #tr gui.packaged.arcane_recipe
    // # Workbench: check recorded grid, research, exact inputs and output.
    // # zh_CN 工作台：请检查录入的九宫格、研究、材料及产物。
    ARCANE_RECIPE("gui.packaged.arcane_recipe"),
    // #tr gui.packaged.arcane_capacity
    // # Workbench: the recipe exceeds this wand's capacity.
    // # zh_CN 工作台：配方所需 Vis 超过法杖容量。
    ARCANE_CAPACITY("gui.packaged.arcane_capacity"),
    // #tr gui.packaged.arcane_essentia
    // # Workbench: waiting for matching AE primal essentia (Thaumic Energistics).
    // # zh_CN 工作台：等待 AE 中对应的基础源质（需要神秘能源）。
    ARCANE_ESSENTIA("gui.packaged.arcane_essentia"),
    // #tr gui.packaged.arcane_returns_full
    // # Workbench: make room in the provider's return inventory.
    // # zh_CN 工作台：请腾出供应器回收栏空间。
    ARCANE_RETURNS_FULL("gui.packaged.arcane_returns_full"),
    // #tr gui.packaged.arcane_ready
    // # Workbench: last craft completed; results buffered for AE.
    // # zh_CN 工作台：上次合成已完成，产物已进入 AE 回收流程。
    ARCANE_READY("gui.packaged.arcane_ready"),
    // #tr gui.packaged.crucible_owner
    // # Crucible: owner must be online here and allowed to use the target.
    // # zh_CN 坩埚：所有者须在本维度在线且有权使用目标。
    CRUCIBLE_OWNER("gui.packaged.crucible_owner"),
    // #tr gui.packaged.crucible_heat
    // # Crucible: heat above 150, or place a heat source under an empty crucible.
    // # zh_CN 坩埚：温度须高于 150；空坩埚可直接在下方放置热源。
    CRUCIBLE_HEAT("gui.packaged.crucible_heat"),
    // #tr gui.packaged.crucible_recipe
    // # Crucible: check research, one catalyst, output and competing local aspects.
    // # zh_CN 坩埚：请检查研究、单个催化剂、产物和已有源质的配方冲突。
    CRUCIBLE_RECIPE("gui.packaged.crucible_recipe"),
    // #tr gui.packaged.crucible_essentia
    // # Crucible: waiting for missing AE essentia (Thaumic Energistics).
    // # zh_CN 坩埚：等待 AE 中缺少的源质（需要神秘能源）。
    CRUCIBLE_ESSENTIA("gui.packaged.crucible_essentia"),
    // #tr gui.packaged.crucible_returns_full
    // # Crucible: make room in the provider's return inventory.
    // # zh_CN 坩埚：请腾出供应器回收栏空间。
    CRUCIBLE_RETURNS_FULL("gui.packaged.crucible_returns_full"),
    // #tr gui.packaged.crucible_ready
    // # Crucible: alchemy complete; results buffered for AE.
    // # zh_CN 坩埚：炼金术已完成，产物已进入 AE 回收流程。
    CRUCIBLE_READY("gui.packaged.crucible_ready");

    public final String key;

    AltarStatus(String key) {
        this.key = key;
    }
}
