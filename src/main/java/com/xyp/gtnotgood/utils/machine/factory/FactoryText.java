package com.xyp.gtnotgood.utils.machine.factory;

import net.minecraft.util.StatCollector;

/** Shared localized labels for the production controller and its ModularUI2 editor. */
public enum FactoryText {

    // #tr factory.gtnotgood.presets
    // # Built-in lines
    // # zh_CN 内置产线
    Presets("factory.gtnotgood.presets"),
    // #tr factory.gtnotgood.preset_prismatic_acid
    // # Prismatic acid / nitrobenzene and strontium recovery
    // # zh_CN 海晶酸／硝基苯与锶回收
    PresetPrismaticAcid("factory.gtnotgood.preset_prismatic_acid"),
    // #tr factory.gtnotgood.preset_prismatic_acid_help
    // # Feed prismarine, H2O2, nitrobenzene, crystalline alloy ingots, H2, O2, N2 and boron plasma; recycles Sr.
    // # zh_CN 输入海晶碎片、过氧化氢、硝基苯、晶化合金锭、氢、氧、氮与硼等离子体；回收锶。
    PresetPrismaticAcidHelp("factory.gtnotgood.preset_prismatic_acid_help"),
    // #tr factory.gtnotgood.preset_netherite
    // # Netherite / air and netherrack processing
    // # zh_CN 下界合金／空气与地狱岩处理
    PresetNetherite("factory.gtnotgood.preset_netherite"),
    // #tr factory.gtnotgood.preset_netherite_help
    // # Nether air, glowstone, grade 2 water, lava, hellish metal -> bonded nanoparticles; scrap is recycled.
    // # zh_CN 输入下界空气、荧石、二级净化水、岩浆与地狱金属；回收碎片，产出强键合纳米颗粒及副产物。
    PresetNetheriteHelp("factory.gtnotgood.preset_netherite_help"),
    // #tr factory.gtnotgood.preset_epoxy
    // # Epoxy resin / propene recovery
    // # zh_CN 环氧树脂／丙烯回收路线
    PresetEpoxy("factory.gtnotgood.preset_epoxy"),
    // #tr factory.gtnotgood.preset_epoxy_help
    // # Feed propene, phenol, acetone, water and chlorine; makes epoxy resin and recycles salt water and acid.
    // # zh_CN 输入丙烯、苯酚、丙酮、水与氯，产出环氧树脂，回收盐水与多余盐酸。
    PresetEpoxyHelp("factory.gtnotgood.preset_epoxy_help"),
    // #tr factory.gtnotgood.preset_pbi
    // # PBI / phenol, chlorine and acid recovery
    // # zh_CN PBI／苯酚、氯与酸回收
    PresetPbi("factory.gtnotgood.preset_pbi"),
    // #tr factory.gtnotgood.preset_pbi_help
    // # Feed benzene, methane, oxygen, ammonia, acids, Cu, Zn and dichromate; recycle phenol, chlorine and acid.
    // # zh_CN 输入苯、甲烷、氧、氨、酸、铜、锌与重铬酸钾；生产 PBI，回收苯酚、氯与硫酸。
    PresetPbiHelp("factory.gtnotgood.preset_pbi_help"),
    // #tr factory.gtnotgood.preset_rp1
    // # Rocket fuel / RP-1
    // # zh_CN 火箭燃料／RP-1
    PresetRp1("factory.gtnotgood.preset_rp1"),
    // #tr factory.gtnotgood.preset_rp1_help
    // # Refines diesel through kerosene and RP-1; adds liquid oxygen to make rocket fuel.
    // # zh_CN 将柴油依次精炼为煤油、RP-1，再与液氧合成火箭燃料。
    PresetRp1Help("factory.gtnotgood.preset_rp1_help"),
    // #tr factory.gtnotgood.preset_hydrazine
    // # Rocket fuel / dense hydrazine
    // # zh_CN 火箭燃料／浓缩肼
    PresetHydrazine("factory.gtnotgood.preset_hydrazine"),
    // #tr factory.gtnotgood.preset_hydrazine_help
    // # Feed hydrogen, oxygen, anthracene, ammonia and methanol; includes a peroxide production loop.
    // # zh_CN 输入氢、氧、蒽、氨与甲醇合成浓缩肼，包含过氧化氢循环生产工序。
    PresetHydrazineHelp("factory.gtnotgood.preset_hydrazine_help"),
    // #tr factory.gtnotgood.preset_cn_fuel
    // # Rocket fuel / CN3H7O3
    // # zh_CN 火箭燃料／CN3H7O3
    PresetCnFuel("factory.gtnotgood.preset_cn_fuel"),
    // #tr factory.gtnotgood.preset_cn_fuel_help
    // # Feed hydrogen, oxygen, anthracene, ammonia, carbon and nitric acid; includes peroxide and hydrazine.
    // # zh_CN 输入氢、氧、蒽、氨、碳与硝酸；生产 CN3H7O3 燃料，包含过氧化氢与肼合成。
    PresetCnFuelHelp("factory.gtnotgood.preset_cn_fuel_help"),
    // #tr factory.gtnotgood.preset_h8_fuel
    // # Rocket fuel / H8N4C2O4
    // # zh_CN 火箭燃料／H8N4C2O4
    PresetH8Fuel("factory.gtnotgood.preset_h8_fuel"),
    // #tr factory.gtnotgood.preset_h8_fuel_help
    // # Feed hydrogen, oxygen, anthracene, ammonia, methanol and nitric acid; recycles copper and anthraquinone.
    // # zh_CN 输入氢、氧、蒽、氨、甲醇与硝酸；生产 H8N4C2O4 燃料，回收铜与蒽醌。
    PresetH8FuelHelp("factory.gtnotgood.preset_h8_fuel_help"),
    // #tr factory.gtnotgood.preset_cetane
    // # Rocket fuel / cetane-boosted diesel
    // # zh_CN 火箭燃料／高十六烷值柴油
    PresetCetane("factory.gtnotgood.preset_cetane"),
    // #tr factory.gtnotgood.preset_cetane_help
    // # Feed light fuel, heavy fuel, ethenone and nitric acid; includes diesel and tetranitromethane synthesis.
    // # zh_CN 输入轻燃油、重燃油、乙烯酮与硝酸；生产高十六烷值柴油，包含柴油与四硝基甲烷合成。
    PresetCetaneHelp("factory.gtnotgood.preset_cetane_help"),
    // #tr factory.gtnotgood.preset_help
    // # Load into an empty page, preview and install; transfer products between pages externally
    // # zh_CN 载入空白页后预览并安装；跨页产物须从外部输送
    PresetHelp("factory.gtnotgood.preset_help"),
    // #tr factory.gtnotgood.preset_loaded
    // # Preset loaded; preview and confirm installation
    // # zh_CN 预设已载入，请预览并确认安装
    PresetLoaded("factory.gtnotgood.preset_loaded"),
    // #tr factory.gtnotgood.preset_unavailable
    // # Preset unavailable or page is not empty
    // # zh_CN 预设配方不可用，或当前页不是空白页
    PresetUnavailable("factory.gtnotgood.preset_unavailable"),
    // #tr factory.gtnotgood.preset_chance
    // # Recovery loops balance automatically; chance outputs settle as whole batches
    // # zh_CN 自动配平回收循环；概率产出折算为确定的整批产出
    PresetChance("factory.gtnotgood.preset_chance"),
    // #tr factory.gtnotgood.deterministic_batch
    // # Waiting for materials or power for a whole deterministic batch
    // # zh_CN 等待确定性整批所需的材料或供电
    DeterministicBatch("factory.gtnotgood.deterministic_batch"),
    // #tr factory.gtnotgood.preset_platinum_group
    // # Complete platinum group / single page
    // # zh_CN 完整铂族处理（单页）
    PresetPlatinumGroup("factory.gtnotgood.preset_platinum_group"),
    // #tr factory.gtnotgood.preset_platinum_group_help
    // # Combines all seven platinum-group branches on one page with shared recovery loops.
    // # zh_CN 单页整合铂族全部七条分支，共用回收工序并整线配平结算。
    PresetPlatinumGroupHelp("factory.gtnotgood.preset_platinum_group_help"),

    // #tr factory.gtnotgood.preset_platinum
    // # Platinum / salt and calcium recovery
    // # zh_CN 铂／铂盐与钙回收
    PresetPlatinum("factory.gtnotgood.preset_platinum"),
    // #tr factory.gtnotgood.preset_platinum_help
    // # Refines metallic platinum with aqua regia and ammonium chloride; exports platinum, Pd ammonia and residue.
    // # zh_CN 用王水与氯化铵处理铂金属粉，产出铂、富钯氨与铂渣。
    PresetPlatinumHelp("factory.gtnotgood.preset_platinum_help"),
    // #tr factory.gtnotgood.preset_palladium
    // # Palladium / salt loop
    // # zh_CN 钯／钯盐循环
    PresetPalladium("factory.gtnotgood.preset_palladium"),
    // #tr factory.gtnotgood.preset_palladium_help
    // # Refines Pd ammonia with formic acid into palladium and byproducts.
    // # zh_CN 用甲酸处理富钯氨，产出钯与副产物。
    PresetPalladiumHelp("factory.gtnotgood.preset_palladium_help"),
    // #tr factory.gtnotgood.preset_residue
    // # Platinum residue / branch separation
    // # zh_CN 铂渣／分离后续支线
    PresetResidue("factory.gtnotgood.preset_residue"),
    // #tr factory.gtnotgood.preset_residue_help
    // # Separates platinum residue into ruthenium salt, osmium solution, iridium oxide and rhodium solution.
    // # zh_CN 将铂渣分离为钌酸钠、酸性锇溶液、二氧化铱与硫酸铑溶液。
    PresetResidueHelp("factory.gtnotgood.preset_residue_help"),
    // #tr factory.gtnotgood.preset_ruthenium
    // # Ruthenium / refining and salt recovery
    // # zh_CN 钌／精炼与盐回收
    PresetRuthenium("factory.gtnotgood.preset_ruthenium"),
    // #tr factory.gtnotgood.preset_ruthenium_help
    // # Refines sodium ruthenate with chlorine and hydrochloric acid into ruthenium.
    // # zh_CN 用氯与盐酸处理钌酸钠，精炼出钌。
    PresetRutheniumHelp("factory.gtnotgood.preset_ruthenium_help"),
    // #tr factory.gtnotgood.preset_osmium
    // # Osmium / distillation and reduction
    // # zh_CN 锇／蒸馏还原
    PresetOsmium("factory.gtnotgood.preset_osmium"),
    // #tr factory.gtnotgood.preset_osmium_help
    // # Distills and reduces acidic osmium solution with hydrochloric acid to recover osmium.
    // # zh_CN 用盐酸处理酸性锇溶液，经蒸馏与还原回收锇。
    PresetOsmiumHelp("factory.gtnotgood.preset_osmium_help"),
    // #tr factory.gtnotgood.preset_iridium
    // # Iridium / calcium and residue recovery
    // # zh_CN 铱／钙与泥渣回收
    PresetIridium("factory.gtnotgood.preset_iridium"),
    // #tr factory.gtnotgood.preset_iridium_help
    // # Refines iridium dioxide with hydrochloric acid and ammonium chloride; recovers iridium, copper and nickel.
    // # zh_CN 用盐酸与氯化铵处理二氧化铱，回收铱、铜与镍。
    PresetIridiumHelp("factory.gtnotgood.preset_iridium_help"),
    // #tr factory.gtnotgood.preset_rhodium
    // # Rhodium / zinc and salt recovery
    // # zh_CN 铑／锌与盐回收
    PresetRhodium("factory.gtnotgood.preset_rhodium"),
    // #tr factory.gtnotgood.preset_rhodium_help
    // # Refines rhodium sulfate solution into rhodium; excess byproducts must leave the line.
    // # zh_CN 精炼硫酸铑溶液以回收铑，多余副产物须排出产线。
    PresetRhodiumHelp("factory.gtnotgood.preset_rhodium_help"),

    // #tr factory.gtnotgood.pattern_mismatch
    // # Pattern inputs do not match an installed process or page
    // # zh_CN 样板输入与已安装工序或产线页不匹配
    PatternMismatch("factory.gtnotgood.pattern_mismatch"),
    // #tr factory.gtnotgood.pattern_ambiguous
    // # Pattern inputs match multiple processes; no materials taken
    // # zh_CN 样板输入匹配多个工序或页面，已暂停取料
    PatternAmbiguous("factory.gtnotgood.pattern_ambiguous"),

    // #tr factory.gtnotgood.drain_running
    // # Finishing remaining jobs before refund
    // # zh_CN 正在完成剩余工序，随后退还
    DrainRunning("factory.gtnotgood.drain_running"),
    // #tr factory.gtnotgood.catalyst_missing
    // # Missing reserved catalyst
    // # zh_CN 缺少需收纳的不可消耗物
    CatalystMissing("factory.gtnotgood.catalyst_missing"),
    // #tr factory.gtnotgood.drain_jobs
    // # Finishing jobs:
    // # zh_CN 待完成工序：
    DrainJobs("factory.gtnotgood.drain_jobs"),
    // #tr factory.gtnotgood.drain_items
    // # Buffered items:
    // # zh_CN 待排物品：
    DrainItems("factory.gtnotgood.drain_items"),
    // #tr factory.gtnotgood.drain_fluids
    // # Buffered fluids:
    // # zh_CN 待排流体：
    DrainFluids("factory.gtnotgood.drain_fluids"),
    // #tr factory.gtnotgood.item_output_blocked
    // # Item output blocked
    // # zh_CN 物品输出堵塞
    ItemOutputBlocked("factory.gtnotgood.item_output_blocked"),
    // #tr factory.gtnotgood.fluid_output_blocked
    // # Fluid output blocked
    // # zh_CN 流体输出堵塞
    FluidOutputBlocked("factory.gtnotgood.fluid_output_blocked"),
    // #tr factory.gtnotgood.refund_blocked
    // # Refund blocked; free output bus space
    // # zh_CN 退还受阻，请腾出输出总线
    RefundBlocked("factory.gtnotgood.refund_blocked"),
    // #tr factory.gtnotgood.refund_list
    // # Deposits waiting for return
    // # zh_CN 待退还的控制器和不可消耗物
    RefundList("factory.gtnotgood.refund_list"),
    // #tr factory.gtnotgood.refund_empty
    // # No deposits waiting for return
    // # zh_CN 没有待退还的收纳物
    RefundEmpty("factory.gtnotgood.refund_empty"),
    // #tr factory.gtnotgood.pattern_internal
    // # ! marks retained surplus; balance before AE export.
    // # zh_CN ！标记内部余量；请先配平，再导出AE样板。
    PatternInternal("factory.gtnotgood.pattern_internal"),

    // #tr factory.gtnotgood.pattern_export
    // # Export AE pattern
    // # zh_CN 导出AE样板
    PatternExport("factory.gtnotgood.pattern_export"),
    // #tr factory.gtnotgood.pattern_help
    // # Creates a free AE pattern using the previewed batch quantities
    // # zh_CN 按预览的批次数量免费生成 AE 样板
    PatternHelp("factory.gtnotgood.pattern_help"),
    // #tr factory.gtnotgood.pattern_lock_first
    // # Lock the line and wait for installation before exporting.
    // # zh_CN 请先锁定产线，等待安装完成后导出。
    PatternLockFirst("factory.gtnotgood.pattern_lock_first"),
    // #tr factory.gtnotgood.pattern_invalid
    // # Cannot encode: missing net input/output or batch amount overflow.
    // # zh_CN 无法编码：缺少净输入输出，或批次数量溢出。
    PatternInvalid("factory.gtnotgood.pattern_invalid"),
    // #tr factory.gtnotgood.pattern_exported
    // # Pattern exported to output bus.
    // # zh_CN 已编码，样板送至输出总线。
    PatternExported("factory.gtnotgood.pattern_exported"),
    // #tr factory.gtnotgood.pattern_output_full
    // # Output bus cannot accept the pattern.
    // # zh_CN 输出总线无法接收样板，请腾出空间。
    PatternOutputFull("factory.gtnotgood.pattern_output_full"),

    // #tr factory.gtnotgood.preview_stale
    // # Routes changed; reopen the preview before confirming.
    // # zh_CN 工序已改变，请重新打开预览后确认。
    PreviewStale("factory.gtnotgood.preview_stale"),
    // #tr factory.gtnotgood.route_details
    // # Details
    // # zh_CN 详情
    RouteDetails("factory.gtnotgood.route_details"),
    // #tr factory.gtnotgood.shift_delete
    // # Shift-click to delete this route.
    // # zh_CN Shift＋点击删除工序。
    ShiftDelete("factory.gtnotgood.shift_delete"),
    // #tr factory.gtnotgood.route_code
    // # Import / Export
    // # zh_CN 导入／导出
    RouteCode("factory.gtnotgood.route_code"),
    // #tr factory.gtnotgood.route_code_help
    // # Paste a route code to import it into an empty page
    // # zh_CN 粘贴工序代码，导入空白产线页
    RouteCodeHelp("factory.gtnotgood.route_code_help"),
    // #tr factory.gtnotgood.import_code
    // # Import
    // # zh_CN 导入
    ImportCode("factory.gtnotgood.import_code"),
    // #tr factory.gtnotgood.copy_code
    // # Copy code
    // # zh_CN 复制代码
    CopyCode("factory.gtnotgood.copy_code"),
    // #tr factory.gtnotgood.clear
    // # Clear routes
    // # zh_CN 清空工序
    Clear("factory.gtnotgood.clear"),
    // #tr factory.gtnotgood.clear_help
    // # Clear all routes? Deposits return after active work finishes.
    // # zh_CN 清空全部工序？当前任务完成后退还已收纳物品。
    ClearHelp("factory.gtnotgood.clear_help"),
    // #tr factory.gtnotgood.locked
    // # Locked; clear routes to edit again.
    // # zh_CN 已锁定；清空工序后可重新编辑。
    Locked("factory.gtnotgood.locked"),
    // #tr factory.gtnotgood.confirm_lock
    // # Confirm and lock
    // # zh_CN 确认并锁定
    ConfirmLock("factory.gtnotgood.confirm_lock"),

    // #tr factory.gtnotgood.name
    // # Integrated Production Factory
    // # zh_CN 产线集成工厂
    Name("factory.gtnotgood.name"),
    // #tr factory.gtnotgood.edit
    // # Production routes
    // # zh_CN 工序列表
    Edit("factory.gtnotgood.edit"),
    // #tr factory.gtnotgood.page_previous
    // # Previous recipe page
    // # zh_CN 上一配方页
    PagePrevious("factory.gtnotgood.page_previous"),
    // #tr factory.gtnotgood.page_next
    // # Add or open the next recipe page
    // # zh_CN 添加或打开下一配方页
    PageNext("factory.gtnotgood.page_next"),
    // #tr factory.gtnotgood.page_delete
    // # Delete current recipe page
    // # zh_CN 删除当前配方页
    PageDelete("factory.gtnotgood.page_delete"),
    // #tr factory.gtnotgood.page_delete_help
    // # Delete this page? Running jobs finish before the remaining pages update.
    // # zh_CN 删除此页？当前任务完成后更新其余页面。
    PageDeleteHelp("factory.gtnotgood.page_delete_help"),
    // #tr factory.gtnotgood.search
    // # Search output / machine
    // # zh_CN 搜索产物或机器
    Search("factory.gtnotgood.search"),
    // #tr factory.gtnotgood.apply
    // # Submit
    // # zh_CN 提交工序
    Apply("factory.gtnotgood.apply"),
    // #tr factory.gtnotgood.unload
    // # Unload
    // # zh_CN 卸载产线
    Unload("factory.gtnotgood.unload"),
    // #tr factory.gtnotgood.delete
    // # Delete node
    // # zh_CN 删除工序
    Delete("factory.gtnotgood.delete"),
    // #tr factory.gtnotgood.parallel
    // # Parallel
    // # zh_CN 并行
    Parallel("factory.gtnotgood.parallel"),
    // #tr factory.gtnotgood.oc
    // # Overclock
    // # zh_CN 超频
    Overclock("factory.gtnotgood.oc"),
    // #tr factory.gtnotgood.wired
    // # Wired power
    // # zh_CN 能源仓供电
    Wired("factory.gtnotgood.wired"),
    // #tr factory.gtnotgood.wireless
    // # Wireless grid
    // # zh_CN 无线电网
    Wireless("factory.gtnotgood.wireless"),
    // #tr factory.gtnotgood.idle
    // # Submit a production graph
    // # zh_CN 等待确认汇总配方
    Idle("factory.gtnotgood.idle"),
    // #tr factory.gtnotgood.running
    // # Running
    // # zh_CN 运行中
    Running("factory.gtnotgood.running"),
    // #tr factory.gtnotgood.paused
    // # Paused; jobs retained
    // # zh_CN 已暂停，任务保留
    Paused("factory.gtnotgood.paused"),
    // #tr factory.gtnotgood.draining
    // # Finishing jobs and unloading buffers
    // # zh_CN 正在完成任务并排出缓存
    Draining("factory.gtnotgood.draining"),
    // #tr factory.gtnotgood.power
    // # Insufficient power
    // # zh_CN 供电不足
    Power("factory.gtnotgood.power"),
    // #tr factory.gtnotgood.input
    // # Waiting for inputs or catalysts
    // # zh_CN 等待原料或催化物
    Input("factory.gtnotgood.input"),
    // #tr factory.gtnotgood.host
    // # Insert a matching multiblock controller; stored internally until unloading
    // # zh_CN 缺少对应多方块控制器
    Host("factory.gtnotgood.host"),
    // #tr factory.gtnotgood.blocked
    // # Waiting for downstream consumption / output space
    // # zh_CN 等待下游消耗或输出空间
    Blocked("factory.gtnotgood.blocked"),
    // #tr factory.gtnotgood.invalid
    // # Missing or unsupported recipe
    // # zh_CN 配方不存在或暂不支持
    Invalid("factory.gtnotgood.invalid"),
    // #tr factory.gtnotgood.limit
    // # Configuration exceeds numeric limits
    // # zh_CN 配置超出数值上限
    Limit("factory.gtnotgood.limit"),
    // #tr factory.gtnotgood.help
    // # Click nodes to edit; drag to move. Right-click source then destination to connect.
    // # zh_CN 单击节点打开详情；拖动节点移动；依次右键源节点和目标节点连线；拖动空白平移。
    Help("factory.gtnotgood.help"),
    // #tr factory.gtnotgood.tooltip.function
    // # Combines linked recipes into an automated production line
    // # zh_CN 连接配方工序，自动执行整条产线
    FunctionHelp("factory.gtnotgood.tooltip.function"),
    // #tr factory.gtnotgood.tooltip.power
    // # Energy hatches take priority; without one, uses the owner's wireless grid
    // # zh_CN 安装能源仓时使用有线供电；未安装时使用拥有者的无线电网
    PowerHelp("factory.gtnotgood.tooltip.power"),
    // #tr factory.gtnotgood.tooltip.graph
    // # Up to 99 pages with 64 processes each; batch size adapts to supplies, power and output space
    // # zh_CN 最多 99 页，每页 64 道工序；批量随供料、功率与输出空间自动调整
    GraphHelp("factory.gtnotgood.tooltip.graph"),
    // #tr factory.gtnotgood.tooltip.safety
    // # Blocked outputs stop producers; power loss pauses jobs without returning consumed inputs
    // # zh_CN 输出堵塞时上游停产；断电暂停任务，已消耗的原料不退回
    SafetyHelp("factory.gtnotgood.tooltip.safety"),
    // #tr factory.gtnotgood.tooltip.recipes
    // # Supports ordinary GT recipes; special machine requirements need an adapter
    // # zh_CN 支持普通 GT 配方；特殊机器条件须有对应适配
    RecipeHelp("factory.gtnotgood.tooltip.recipes"),
    // #tr factory.gtnotgood.add_node
    // # Add node
    // # zh_CN 新增节点
    AddNode("factory.gtnotgood.add_node"),
    // #tr factory.gtnotgood.node_details
    // # Node details
    // # zh_CN 节点详情
    NodeDetails("factory.gtnotgood.node_details"),
    // #tr factory.gtnotgood.empty
    // # No recipe: import from NEI
    // # zh_CN 空节点：请从 NEI 导入配方
    Empty("factory.gtnotgood.empty"),
    // #tr factory.gtnotgood.inputs
    // # Inputs
    // # zh_CN 输入
    Inputs("factory.gtnotgood.inputs"),
    // #tr factory.gtnotgood.outputs
    // # Outputs
    // # zh_CN 输出
    Outputs("factory.gtnotgood.outputs"),
    // #tr factory.gtnotgood.catalysts
    // # Non-consumable inputs
    // # zh_CN 不可消耗物
    Catalysts("factory.gtnotgood.catalysts"),
    // #tr factory.gtnotgood.import_help
    // # Open an NEI recipe with R, then click + to import here.
    // # zh_CN 保持此页打开，在 NEI 中按 R 查看配方，点击＋导入工序。
    ImportHelp("factory.gtnotgood.import_help"),
    // #tr factory.gtnotgood.reserved
    // # Controllers and non-consumable inputs are stored inside and returned when unloading
    // # zh_CN 控制器与不可消耗物存于内部，卸载产线时返还
    Reserved("factory.gtnotgood.reserved"),
    // #tr factory.gtnotgood.balance
    // # Auto balance
    // # zh_CN 自动配平
    Balance("factory.gtnotgood.balance"),
    // #tr factory.gtnotgood.balanced
    // # Draft balanced; submit to apply
    // # zh_CN 草稿已配平，请提交工序
    Balanced("factory.gtnotgood.balanced"),
    // #tr factory.gtnotgood.balance_failed
    // # No balance found (incompatible ratios or search limit); draft unchanged
    // # zh_CN 未找到配平结果（比例冲突或搜索受限），草稿未改动
    BalanceFailed("factory.gtnotgood.balance_failed"),
    // #tr factory.gtnotgood.requirements
    // # Submitted requirements (stored / needed)
    // # zh_CN 工序需求（已收入 / 需要）
    Requirements("factory.gtnotgood.requirements"),
    // #tr factory.gtnotgood.requirement_help
    // # Put the required controllers and non-consumable inputs into input buses
    // # zh_CN 将所需控制器与不可消耗物放入输入总线
    RequirementHelp("factory.gtnotgood.requirement_help"),
    // #tr factory.gtnotgood.host_help
    // # Supply a multiblock controller that supports each selected recipe map
    // # zh_CN 为各配方表提供支持该表的多方块控制器
    HostHelp("factory.gtnotgood.host_help"),
    // #tr factory.gtnotgood.no_requirements
    // # No submitted requirements
    // # zh_CN 暂无已提交工序需求
    NoRequirements("factory.gtnotgood.no_requirements"),
    // #tr factory.gtnotgood.controller
    // # Multiblock controller
    // # zh_CN 多方块控制器
    Controller("factory.gtnotgood.controller"),
    // #tr factory.gtnotgood.missing
    // # Missing
    // # zh_CN 缺少
    Missing("factory.gtnotgood.missing"),
    // #tr factory.gtnotgood.auto_parallel
    // # Batch size adapts to supplies, power and output capacity
    // # zh_CN 批量随供料、功率与输出容量自动调整
    AutoParallel("factory.gtnotgood.auto_parallel"),
    // #tr factory.gtnotgood.batch_duration
    // # Batch duration
    // # zh_CN 整批加工时间
    BatchDuration("factory.gtnotgood.batch_duration"),
    // #tr factory.gtnotgood.per_batch
    // # Per batch
    // # zh_CN 每批
    PerBatch("factory.gtnotgood.per_batch"),
    // #tr factory.gtnotgood.atomic_note
    // # Consumes inputs at startup; releases all outputs together; fractional amounts round up
    // # zh_CN 启动时一次性扣料，完成时统一输出；小数数量向上取整
    AtomicNote("factory.gtnotgood.atomic_note"),
    // #tr factory.gtnotgood.node_eut
    // # Base EU/t
    // # zh_CN 基础耗电 EU/t
    NodeEut("factory.gtnotgood.node_eut"),
    // #tr factory.gtnotgood.set
    // # Apply value
    // # zh_CN 应用数值
    Set("factory.gtnotgood.set"),
    // #tr factory.gtnotgood.recipe_default
    // # Recipe default
    // # zh_CN 配方默认
    RecipeDefault("factory.gtnotgood.recipe_default"),
    // #tr factory.gtnotgood.import_virtual
    // # Virtual recipe needs a dedicated adapter
    // # zh_CN 此虚拟展示配方尚未适配
    ImportVirtual("factory.gtnotgood.import_virtual"),
    // #tr factory.gtnotgood.import_disabled
    // # Recipe is disabled or hidden
    // # zh_CN 此配方已禁用或隐藏
    ImportDisabled("factory.gtnotgood.import_disabled"),
    // #tr factory.gtnotgood.import_custom
    // # Custom recipe implementation is not adapted
    // # zh_CN 此配方的特殊处理逻辑尚未适配
    ImportCustom("factory.gtnotgood.import_custom"),
    // #tr factory.gtnotgood.import_special
    // # Special item or machine condition is not adapted
    // # zh_CN 此配方的特殊物品或机器条件尚未适配
    ImportSpecial("factory.gtnotgood.import_special"),
    // #tr factory.gtnotgood.import_metadata
    // # Recipe metadata conditions are not adapted
    // # zh_CN 此配方附带的额外条件尚未适配
    ImportMetadata("factory.gtnotgood.import_metadata"),
    // #tr factory.gtnotgood.import_timing
    // # Recipe requires positive EU/t and duration
    // # zh_CN 此配方的耗电或耗时不适用于当前执行器
    ImportTiming("factory.gtnotgood.import_timing"),
    // #tr factory.gtnotgood.import_slots
    // # Recipe exceeds 16 slots for one input/output type
    // # zh_CN 此配方某类输入或输出超过16槽
    ImportSlots("factory.gtnotgood.import_slots"),
    // #tr factory.gtnotgood.import_chances
    // # Probabilistic inputs or fluids are not adapted
    // # zh_CN 此配方的概率消耗或概率流体尚未适配
    ImportChances("factory.gtnotgood.import_chances"),
    // #tr factory.gtnotgood.import_unregistered
    // # NEI recipe does not match the registered recipe
    // # zh_CN NEI配方与已注册配方未匹配
    ImportUnregistered("factory.gtnotgood.import_unregistered"),
    // #tr factory.gtnotgood.import_handler
    // # This NEI recipe handler is not adapted
    // # zh_CN 此NEI配方处理器尚未适配
    ImportHandler("factory.gtnotgood.import_handler"),
    // #tr factory.gtnotgood.target
    // # Target product node
    // # zh_CN 目标产物节点
    Target("factory.gtnotgood.target"),
    // #tr factory.gtnotgood.set_target
    // # Set target
    // # zh_CN 设为目标产物
    SetTarget("factory.gtnotgood.set_target"),
    // #tr factory.gtnotgood.unset_target
    // # Unset target
    // # zh_CN 取消目标产物
    UnsetTarget("factory.gtnotgood.unset_target"),
    // #tr factory.gtnotgood.no_target
    // # Set a target node for each separate production line
    // # zh_CN 请为每条独立产线设置目标产物节点
    NoTarget("factory.gtnotgood.no_target"),
    // #tr factory.gtnotgood.preview
    // # Line preview
    // # zh_CN 产线预览
    Preview("factory.gtnotgood.preview"),
    // #tr factory.gtnotgood.preview_note
    // # Rates exclude automatic batching; internal loops need no startup materials
    // # zh_CN 速率不含自动批量；内部循环无需启动物料
    PreviewNote("factory.gtnotgood.preview_note"),
    // #tr factory.gtnotgood.external_input
    // # External input
    // # zh_CN 外部输入
    ExternalInput("factory.gtnotgood.external_input"),
    // #tr factory.gtnotgood.final_output
    // # Exported output
    // # zh_CN 对外输出
    FinalOutput("factory.gtnotgood.final_output"),
    // #tr factory.gtnotgood.internal_surplus
    // # Internal surplus
    // # zh_CN 内部剩余
    InternalSurplus("factory.gtnotgood.internal_surplus"),
    // #tr factory.gtnotgood.per_tick
    // # /tick
    // # zh_CN /tick
    PerTick("factory.gtnotgood.per_tick"),
    // #tr factory.gtnotgood.preview_invalid
    // # Complete all node recipes before previewing
    // # zh_CN 请先为所有节点设置有效配方
    PreviewInvalid("factory.gtnotgood.preview_invalid");

    private final String key;

    FactoryText(String key) {
        this.key = key;
    }

    public String text() {
        return StatCollector.translateToLocal(key);
    }
}
