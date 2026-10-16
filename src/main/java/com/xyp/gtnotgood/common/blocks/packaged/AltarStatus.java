// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.blocks.packaged;

/** Server-synchronized adapter feedback shown in the original core-slot tooltip. */
public enum AltarStatus {

    // #tr gui.packaged.altar_idle
    // # Altar: idle
    // # zh_CN 祭坛：空闲
    Idle("gui.packaged.altar_idle"),
    // #tr gui.packaged.altar_running
    // # Altar: active job or waiting for return
    // # zh_CN 祭坛：任务进行中或等待回收
    Running("gui.packaged.altar_running"),
    // #tr gui.packaged.altar_essentia
    // # Altar: waiting for AE essentia, power or channel
    // # zh_CN 祭坛：等待 AE 源质、电力或频道
    Essentia("gui.packaged.altar_essentia"),
    // #tr gui.packaged.altar_inactive
    // # Altar: activate the structure with a wand first
    // # zh_CN 祭坛：请先使用法杖激活结构
    Inactive("gui.packaged.altar_inactive"),
    // #tr gui.packaged.altar_owner
    // # Altar: owner must be online in this dimension
    // # zh_CN 祭坛：放置者须在本维度在线
    Owner("gui.packaged.altar_owner"),
    // #tr gui.packaged.altar_unloaded
    // # Altar: waiting for loaded chunks
    // # zh_CN 祭坛：等待区块加载
    Unloaded("gui.packaged.altar_unloaded"),
    // #tr gui.packaged.altar_occupied
    // # Altar: structure or pedestal contents prevent dispatch
    // # zh_CN 祭坛：结构或基座物品阻止派单
    Occupied("gui.packaged.altar_occupied"),
    // #tr gui.packaged.altar_recipe
    // # Altar: check research, ingredients and the encoded output
    // # zh_CN 祭坛：请检查研究、材料与样板产物
    Recipe("gui.packaged.altar_recipe"),
    // #tr gui.packaged.altar_interrupted
    // # Altar stopped without the expected result. Inspect the altar before releasing its job.
    // # zh_CN 祭坛已停止且缺少预期产物，检查祭坛后再释放任务。
    Interrupted("gui.packaged.altar_interrupted"),
    // #tr gui.packaged.returns_full
    // # Return slots full. The result remains in the altar.
    // # zh_CN 回收槽已满，产物保留在祭坛中。
    ReturnsFull("gui.packaged.returns_full"),
    // #tr gui.packaged.blood_recipe
    // # Blood Altar: check tier, single-step ingredients and output.
    // # zh_CN 血祭坛：请检查等级、单步配方材料与产物。
    BloodRecipe("gui.packaged.blood_recipe"),
    // #tr gui.packaged.arcane_owner
    // # Workbench: owner must be online here and allowed to use the target.
    // # zh_CN 工作台：所有者须在本维度在线且有权使用目标。
    ArcaneOwner("gui.packaged.arcane_owner"),
    // #tr gui.packaged.arcane_occupied
    // # Workbench: clear the grid and close its screen.
    // # zh_CN 工作台：请清空九宫格并关闭其界面。
    ArcaneOccupied("gui.packaged.arcane_occupied"),
    // #tr gui.packaged.arcane_wand
    // # Workbench: insert a crafting wand or sceptre.
    // # zh_CN 工作台：请放入可合成的法杖或权杖。
    ArcaneWand("gui.packaged.arcane_wand"),
    // #tr gui.packaged.arcane_recipe
    // # Workbench: check recorded grid, research, exact inputs and output.
    // # zh_CN 工作台：请检查录入的九宫格、研究、材料及产物。
    ArcaneRecipe("gui.packaged.arcane_recipe"),
    // #tr gui.packaged.arcane_capacity
    // # Workbench: the recipe exceeds this wand's capacity.
    // # zh_CN 工作台：配方所需 Vis 超过法杖容量。
    ArcaneCapacity("gui.packaged.arcane_capacity"),
    // #tr gui.packaged.arcane_essentia
    // # Workbench: waiting for matching AE primal essentia (Thaumic Energistics).
    // # zh_CN 工作台：等待 AE 中对应的基础源质（需要神秘能源）。
    ArcaneEssentia("gui.packaged.arcane_essentia"),
    // #tr gui.packaged.arcane_returns_full
    // # Workbench: make room in the provider's return inventory.
    // # zh_CN 工作台：请腾出供应器回收栏空间。
    ArcaneReturnsFull("gui.packaged.arcane_returns_full"),
    // #tr gui.packaged.arcane_ready
    // # Workbench: crafting complete; results are waiting to enter ME.
    // # zh_CN 工作台：合成已完成，产物等待送入 ME。
    ArcaneReady("gui.packaged.arcane_ready"),
    // #tr gui.packaged.crucible_owner
    // # Crucible: owner must be online here and allowed to use the target.
    // # zh_CN 坩埚：所有者须在本维度在线且有权使用目标。
    CrucibleOwner("gui.packaged.crucible_owner"),
    // #tr gui.packaged.crucible_heat
    // # Crucible: heat above 150, or place a heat source below a dry crucible.
    // # zh_CN 坩埚：温度须高于 150；无水时可直接在下方放置热源。
    CrucibleHeat("gui.packaged.crucible_heat"),
    // #tr gui.packaged.crucible_recipe
    // # Crucible: check research, one catalyst, output and competing local aspects.
    // # zh_CN 坩埚：请检查研究、单个催化剂、产物和已有源质的配方冲突。
    CrucibleRecipe("gui.packaged.crucible_recipe"),
    // #tr gui.packaged.crucible_essentia
    // # Crucible: waiting for missing AE essentia (Thaumic Energistics).
    // # zh_CN 坩埚：等待 AE 中缺少的源质（需要神秘能源）。
    CrucibleEssentia("gui.packaged.crucible_essentia"),
    // #tr gui.packaged.crucible_returns_full
    // # Crucible: make room in the provider's return inventory.
    // # zh_CN 坩埚：请腾出供应器回收栏空间。
    CrucibleReturnsFull("gui.packaged.crucible_returns_full"),
    // #tr gui.packaged.crucible_ready
    // # Crucible: alchemy complete; results are waiting to enter ME.
    // # zh_CN 坩埚：炼金术已完成，产物等待送入 ME。
    CrucibleReady("gui.packaged.crucible_ready");

    public final String key;

    AltarStatus(String key) {
        this.key = key;
    }
}
