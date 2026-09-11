package com.xyp.gtnotgood.utils.machine.factory;

import net.minecraft.util.StatCollector;

/** Shared localized labels for the production controller and its ModularUI2 editor. */
public enum FactoryText {

    // #tr factory.gtnotgood.drain_running
    // # Finishing remaining jobs before refund
    // # zh_CN 正在完成剩余工序，随后退还
    DRAIN_RUNNING("factory.gtnotgood.drain_running"),
    // #tr factory.gtnotgood.catalyst_missing
    // # Missing reserved catalyst
    // # zh_CN 缺少需收纳的不可消耗物
    CATALYST_MISSING("factory.gtnotgood.catalyst_missing"),
    // #tr factory.gtnotgood.drain_jobs
    // # Finishing jobs:
    // # zh_CN 待完成工序：
    DRAIN_JOBS("factory.gtnotgood.drain_jobs"),
    // #tr factory.gtnotgood.drain_items
    // # Buffered items:
    // # zh_CN 待排物品：
    DRAIN_ITEMS("factory.gtnotgood.drain_items"),
    // #tr factory.gtnotgood.drain_fluids
    // # Buffered fluids:
    // # zh_CN 待排流体：
    DRAIN_FLUIDS("factory.gtnotgood.drain_fluids"),
    // #tr factory.gtnotgood.item_output_blocked
    // # Item output blocked
    // # zh_CN 物品输出堵塞
    ITEM_OUTPUT_BLOCKED("factory.gtnotgood.item_output_blocked"),
    // #tr factory.gtnotgood.fluid_output_blocked
    // # Fluid output blocked
    // # zh_CN 流体输出堵塞
    FLUID_OUTPUT_BLOCKED("factory.gtnotgood.fluid_output_blocked"),
    // #tr factory.gtnotgood.refund_blocked
    // # Refund blocked; free output bus space
    // # zh_CN 退还受阻，请腾出输出总线
    REFUND_BLOCKED("factory.gtnotgood.refund_blocked"),
    // #tr factory.gtnotgood.refund_list
    // # Deposits waiting for return
    // # zh_CN 待退还的控制器和不可消耗物
    REFUND_LIST("factory.gtnotgood.refund_list"),
    // #tr factory.gtnotgood.refund_empty
    // # No deposits waiting for return
    // # zh_CN 没有待退还的收纳物
    REFUND_EMPTY("factory.gtnotgood.refund_empty"),
    // #tr factory.gtnotgood.pattern_chance
    // # Expected chance output; AE export unavailable.
    // # zh_CN 含概率期望产物，不能导出确定产出的AE样板。
    PATTERN_CHANCE("factory.gtnotgood.pattern_chance"),
    // #tr factory.gtnotgood.pattern_internal
    // # ! marks retained surplus; balance before AE export.
    // # zh_CN ！标记内部余量；请先配平，再导出AE样板。
    PATTERN_INTERNAL("factory.gtnotgood.pattern_internal"),

    // #tr factory.gtnotgood.pattern_export
    // # Export AE pattern
    // # zh_CN 导出AE样板
    PATTERN_EXPORT("factory.gtnotgood.pattern_export"),
    // #tr factory.gtnotgood.pattern_help
    // # Creates a free pattern from preview quantities; fractions are scaled together.
    // # zh_CN 免费生成预览对应的样板；小数数量会整组同比放大。
    PATTERN_HELP("factory.gtnotgood.pattern_help"),
    // #tr factory.gtnotgood.pattern_lock_first
    // # Lock the line and wait for installation before exporting.
    // # zh_CN 请先锁定产线，等待安装完成后导出。
    PATTERN_LOCK_FIRST("factory.gtnotgood.pattern_lock_first"),
    // #tr factory.gtnotgood.pattern_invalid
    // # Cannot encode: missing net input/output or batch amount overflow.
    // # zh_CN 无法编码：缺少净输入输出，或批次数量溢出。
    PATTERN_INVALID("factory.gtnotgood.pattern_invalid"),
    // #tr factory.gtnotgood.pattern_exported
    // # Pattern exported to output bus.
    // # zh_CN 已编码，样板送至输出总线。
    PATTERN_EXPORTED("factory.gtnotgood.pattern_exported"),
    // #tr factory.gtnotgood.pattern_output_full
    // # Output bus cannot accept the pattern.
    // # zh_CN 输出总线无法接收样板，请腾出空间。
    PATTERN_OUTPUT_FULL("factory.gtnotgood.pattern_output_full"),

    // #tr factory.gtnotgood.preview_stale
    // # Routes changed; reopen the preview before confirming.
    // # zh_CN 工序已改变，请重新打开预览后确认。
    PREVIEW_STALE("factory.gtnotgood.preview_stale"),
    // #tr factory.gtnotgood.route_details
    // # Details
    // # zh_CN 详情
    ROUTE_DETAILS("factory.gtnotgood.route_details"),
    // #tr factory.gtnotgood.shift_delete
    // # Shift-click to delete this route.
    // # zh_CN Shift＋点击删除工序。
    SHIFT_DELETE("factory.gtnotgood.shift_delete"),
    // #tr factory.gtnotgood.route_code
    // # Import / Export
    // # zh_CN 导入／导出
    ROUTE_CODE("factory.gtnotgood.route_code"),
    // #tr factory.gtnotgood.route_code_help
    // # GTNG route code: paste to import into an empty list.
    // # zh_CN GTNG 工序代码：粘贴后导入空工序列表。
    ROUTE_CODE_HELP("factory.gtnotgood.route_code_help"),
    // #tr factory.gtnotgood.import_code
    // # Import
    // # zh_CN 导入
    IMPORT_CODE("factory.gtnotgood.import_code"),
    // #tr factory.gtnotgood.copy_code
    // # Copy code
    // # zh_CN 复制代码
    COPY_CODE("factory.gtnotgood.copy_code"),
    // #tr factory.gtnotgood.clear
    // # Clear routes
    // # zh_CN 清空工序
    CLEAR("factory.gtnotgood.clear"),
    // #tr factory.gtnotgood.clear_help
    // # Clear all routes? Deposits return after active work finishes.
    // # zh_CN 清空全部工序？当前任务完成后退还已收纳物品。
    CLEAR_HELP("factory.gtnotgood.clear_help"),
    // #tr factory.gtnotgood.locked
    // # Locked; clear routes to edit again.
    // # zh_CN 已锁定；清空工序后可重新编辑。
    LOCKED("factory.gtnotgood.locked"),
    // #tr factory.gtnotgood.confirm_lock
    // # Confirm and lock
    // # zh_CN 确认并锁定
    CONFIRM_LOCK("factory.gtnotgood.confirm_lock"),

    // #tr factory.gtnotgood.name
    // # Integrated Production Factory
    // # zh_CN 产线集成工厂
    NAME("factory.gtnotgood.name"),
    // #tr factory.gtnotgood.edit
    // # Production routes
    // # zh_CN 工序列表
    EDIT("factory.gtnotgood.edit"),
    // #tr factory.gtnotgood.search
    // # Search output / machine
    // # zh_CN 搜索产物或机器
    SEARCH("factory.gtnotgood.search"),
    // #tr factory.gtnotgood.apply
    // # Submit
    // # zh_CN 提交工序
    APPLY("factory.gtnotgood.apply"),
    // #tr factory.gtnotgood.unload
    // # Unload
    // # zh_CN 卸载产线
    UNLOAD("factory.gtnotgood.unload"),
    // #tr factory.gtnotgood.delete
    // # Delete node
    // # zh_CN 删除工序
    DELETE("factory.gtnotgood.delete"),
    // #tr factory.gtnotgood.parallel
    // # Parallel
    // # zh_CN 并行
    PARALLEL("factory.gtnotgood.parallel"),
    // #tr factory.gtnotgood.oc
    // # Overclock
    // # zh_CN 超频
    OC("factory.gtnotgood.oc"),
    // #tr factory.gtnotgood.wired
    // # Wired power
    // # zh_CN 能源仓供电
    WIRED("factory.gtnotgood.wired"),
    // #tr factory.gtnotgood.wireless
    // # Wireless grid
    // # zh_CN 无线电网
    WIRELESS("factory.gtnotgood.wireless"),
    // #tr factory.gtnotgood.idle
    // # Submit a production graph
    // # zh_CN 等待确认汇总配方
    IDLE("factory.gtnotgood.idle"),
    // #tr factory.gtnotgood.running
    // # Running
    // # zh_CN 运行中
    RUNNING("factory.gtnotgood.running"),
    // #tr factory.gtnotgood.paused
    // # Paused; jobs retained
    // # zh_CN 已暂停，任务保留
    PAUSED("factory.gtnotgood.paused"),
    // #tr factory.gtnotgood.draining
    // # Finishing jobs and unloading buffers
    // # zh_CN 正在完成任务并排出缓存
    DRAINING("factory.gtnotgood.draining"),
    // #tr factory.gtnotgood.power
    // # Insufficient power
    // # zh_CN 供电不足
    POWER("factory.gtnotgood.power"),
    // #tr factory.gtnotgood.input
    // # Waiting for inputs or catalysts
    // # zh_CN 等待原料或催化物
    INPUT("factory.gtnotgood.input"),
    // #tr factory.gtnotgood.host
    // # Insert a matching multiblock controller; stored internally until unloading
    // # zh_CN 缺少对应多方块控制器
    HOST("factory.gtnotgood.host"),
    // #tr factory.gtnotgood.blocked
    // # Waiting for downstream consumption / output space
    // # zh_CN 等待下游消耗或输出空间
    BLOCKED("factory.gtnotgood.blocked"),
    // #tr factory.gtnotgood.invalid
    // # Missing or unsupported recipe
    // # zh_CN 配方不存在或暂不支持
    INVALID("factory.gtnotgood.invalid"),
    // #tr factory.gtnotgood.limit
    // # Configuration exceeds numeric limits
    // # zh_CN 配置超出数值上限
    LIMIT("factory.gtnotgood.limit"),
    // #tr factory.gtnotgood.help
    // # Click nodes to edit; drag to move. Right-click source then destination to connect.
    // # zh_CN 单击节点打开详情；拖动节点移动；依次右键源节点和目标节点连线；拖动空白平移。
    HELP("factory.gtnotgood.help"),
    // #tr factory.gtnotgood.tooltip.power
    // # Energy hatch installed: wired. No normal/exotic energy hatch: owner's wireless grid.
    // # zh_CN 安装能源仓时有线供电；无普通或特殊能源仓时使用归属玩家的无线电网。
    POWER_HELP("factory.gtnotgood.tooltip.power"),
    // #tr factory.gtnotgood.tooltip.graph
    // # Up to 32 nodes; parallel up to 2,147,483,647. Linked materials stay inside; other products are exported.
    // # zh_CN 最多32工序，默认1并行；物料自动衔接，其余产物排出。
    GRAPH_HELP("factory.gtnotgood.tooltip.graph"),
    // #tr factory.gtnotgood.tooltip.safety
    // # Output jams stop producers. Power loss pauses jobs without refunding consumed inputs.
    // # zh_CN 输出堵塞时上游停止；断电暂停任务，已消耗输入不退回。
    SAFETY_HELP("factory.gtnotgood.tooltip.safety"),
    // #tr factory.gtnotgood.tooltip.recipes
    // # Ordinary GT recipes only; special machine conditions require dedicated adapters.
    // # zh_CN 支持普通GT配方；特殊机器条件需要专用适配。
    RECIPE_HELP("factory.gtnotgood.tooltip.recipes"),
    // #tr factory.gtnotgood.add_node
    // # Add node
    // # zh_CN 新增节点
    ADD_NODE("factory.gtnotgood.add_node"),
    // #tr factory.gtnotgood.node_details
    // # Node details
    // # zh_CN 节点详情
    NODE_DETAILS("factory.gtnotgood.node_details"),
    // #tr factory.gtnotgood.empty
    // # No recipe: import from NEI
    // # zh_CN 空节点：请从 NEI 导入配方
    EMPTY("factory.gtnotgood.empty"),
    // #tr factory.gtnotgood.inputs
    // # Inputs
    // # zh_CN 输入
    INPUTS("factory.gtnotgood.inputs"),
    // #tr factory.gtnotgood.outputs
    // # Outputs
    // # zh_CN 输出
    OUTPUTS("factory.gtnotgood.outputs"),
    // #tr factory.gtnotgood.catalysts
    // # Non-consumable inputs
    // # zh_CN 不可消耗物
    CATALYSTS("factory.gtnotgood.catalysts"),
    // #tr factory.gtnotgood.import_help
    // # Open an NEI recipe with R, then click + to import here.
    // # zh_CN 保持此页打开，在 NEI 中按 R 查看配方，点击＋导入工序。
    IMPORT_HELP("factory.gtnotgood.import_help"),
    // #tr factory.gtnotgood.reserved
    // # Controllers and non-consumables are stored internally and returned on unload.
    // # zh_CN 控制器与不可消耗物收入内部保管，卸载产线时返还。
    RESERVED("factory.gtnotgood.reserved"),
    // #tr factory.gtnotgood.balance
    // # Auto balance
    // # zh_CN 自动配平
    BALANCE("factory.gtnotgood.balance"),
    // #tr factory.gtnotgood.balanced
    // # Draft balanced; submit to apply
    // # zh_CN 草稿已配平，请提交工序
    BALANCED("factory.gtnotgood.balanced"),
    // #tr factory.gtnotgood.balance_failed
    // # No balance found (incompatible ratios or search limit); draft unchanged
    // # zh_CN 未找到配平结果（比例冲突或搜索受限），草稿未改动
    BALANCE_FAILED("factory.gtnotgood.balance_failed"),
    // #tr factory.gtnotgood.requirements
    // # Submitted requirements (stored / needed)
    // # zh_CN 工序需求（已收入 / 需要）
    REQUIREMENTS("factory.gtnotgood.requirements"),
    // #tr factory.gtnotgood.requirement_help
    // # Insert controllers and non-consumables into input buses.
    // # zh_CN 请将控制器和不可消耗物放入输入总线。
    REQUIREMENT_HELP("factory.gtnotgood.requirement_help"),
    // #tr factory.gtnotgood.host_help
    // # Any multiblock controller supporting this recipe map is accepted.
    // # zh_CN 放入支持此配方表的多方块控制器即可。
    HOST_HELP("factory.gtnotgood.host_help"),
    // #tr factory.gtnotgood.no_requirements
    // # No submitted requirements
    // # zh_CN 暂无已提交工序需求
    NO_REQUIREMENTS("factory.gtnotgood.no_requirements"),
    // #tr factory.gtnotgood.controller
    // # Multiblock controller
    // # zh_CN 多方块控制器
    CONTROLLER("factory.gtnotgood.controller"),
    // #tr factory.gtnotgood.missing
    // # Missing
    // # zh_CN 缺少
    MISSING("factory.gtnotgood.missing"),
    // #tr factory.gtnotgood.auto_parallel
    // # Auto parallel (max 2,147,483,647); preview uses node settings.
    // # zh_CN 自动并行（上限2,147,483,647）；预览按节点设置计算。
    AUTO_PARALLEL("factory.gtnotgood.auto_parallel"),
    // #tr factory.gtnotgood.node_eut
    // # Base EU/t
    // # zh_CN 基础耗电 EU/t
    NODE_EUT("factory.gtnotgood.node_eut"),
    // #tr factory.gtnotgood.set
    // # Apply value
    // # zh_CN 应用数值
    SET("factory.gtnotgood.set"),
    // #tr factory.gtnotgood.recipe_default
    // # Recipe default
    // # zh_CN 配方默认
    RECIPE_DEFAULT("factory.gtnotgood.recipe_default"),
    // #tr factory.gtnotgood.import_virtual
    // # Virtual recipe needs a dedicated adapter
    // # zh_CN 此虚拟展示配方尚未适配
    IMPORT_VIRTUAL("factory.gtnotgood.import_virtual"),
    // #tr factory.gtnotgood.import_disabled
    // # Recipe is disabled or hidden
    // # zh_CN 此配方已禁用或隐藏
    IMPORT_DISABLED("factory.gtnotgood.import_disabled"),
    // #tr factory.gtnotgood.import_custom
    // # Custom recipe implementation is not adapted
    // # zh_CN 此配方的特殊处理逻辑尚未适配
    IMPORT_CUSTOM("factory.gtnotgood.import_custom"),
    // #tr factory.gtnotgood.import_special
    // # Special item or machine condition is not adapted
    // # zh_CN 此配方的特殊物品或机器条件尚未适配
    IMPORT_SPECIAL("factory.gtnotgood.import_special"),
    // #tr factory.gtnotgood.import_metadata
    // # Recipe metadata conditions are not adapted
    // # zh_CN 此配方附带的额外条件尚未适配
    IMPORT_METADATA("factory.gtnotgood.import_metadata"),
    // #tr factory.gtnotgood.import_timing
    // # Recipe requires positive EU/t and duration
    // # zh_CN 此配方的耗电或耗时不适用于当前执行器
    IMPORT_TIMING("factory.gtnotgood.import_timing"),
    // #tr factory.gtnotgood.import_slots
    // # Recipe exceeds 16 slots for one input/output type
    // # zh_CN 此配方某类输入或输出超过16槽
    IMPORT_SLOTS("factory.gtnotgood.import_slots"),
    // #tr factory.gtnotgood.import_chances
    // # Probabilistic inputs or fluids are not adapted
    // # zh_CN 此配方的概率消耗或概率流体尚未适配
    IMPORT_CHANCES("factory.gtnotgood.import_chances"),
    // #tr factory.gtnotgood.import_unregistered
    // # NEI recipe does not match the registered recipe
    // # zh_CN NEI配方与已注册配方未匹配
    IMPORT_UNREGISTERED("factory.gtnotgood.import_unregistered"),
    // #tr factory.gtnotgood.import_handler
    // # This NEI recipe handler is not adapted
    // # zh_CN 此NEI配方处理器尚未适配
    IMPORT_HANDLER("factory.gtnotgood.import_handler"),
    // #tr factory.gtnotgood.target
    // # Target product node
    // # zh_CN 目标产物节点
    TARGET("factory.gtnotgood.target"),
    // #tr factory.gtnotgood.set_target
    // # Set target
    // # zh_CN 设为目标产物
    SET_TARGET("factory.gtnotgood.set_target"),
    // #tr factory.gtnotgood.unset_target
    // # Unset target
    // # zh_CN 取消目标产物
    UNSET_TARGET("factory.gtnotgood.unset_target"),
    // #tr factory.gtnotgood.no_target
    // # Set a target node for each separate production line
    // # zh_CN 请为每条独立产线设置目标产物节点
    NO_TARGET("factory.gtnotgood.no_target"),
    // #tr factory.gtnotgood.preview
    // # Line preview
    // # zh_CN 产线预览
    PREVIEW("factory.gtnotgood.preview"),
    // #tr factory.gtnotgood.preview_note
    // # Node rates exclude auto batching; internal loops need no startup material.
    // # zh_CN 节点基础速率，不含自动批量；内部循环无需启动物料。
    PREVIEW_NOTE("factory.gtnotgood.preview_note"),
    // #tr factory.gtnotgood.external_input
    // # External input
    // # zh_CN 外部输入
    EXTERNAL_INPUT("factory.gtnotgood.external_input"),
    // #tr factory.gtnotgood.final_output
    // # Exported output
    // # zh_CN 对外输出
    FINAL_OUTPUT("factory.gtnotgood.final_output"),
    // #tr factory.gtnotgood.internal_surplus
    // # Internal surplus
    // # zh_CN 内部剩余
    INTERNAL_SURPLUS("factory.gtnotgood.internal_surplus"),
    // #tr factory.gtnotgood.per_tick
    // # /tick
    // # zh_CN /tick
    PER_TICK("factory.gtnotgood.per_tick"),
    // #tr factory.gtnotgood.preview_invalid
    // # Complete all node recipes before previewing
    // # zh_CN 请先为所有节点设置有效配方
    PREVIEW_INVALID("factory.gtnotgood.preview_invalid");

    private final String key;

    FactoryText(String key) {
        this.key = key;
    }

    public String text() {
        return StatCollector.translateToLocal(key);
    }
}
