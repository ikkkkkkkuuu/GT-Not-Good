# 大型嬗变机

控制器：`LargeTransmutationMachine`，GregTech ID `28516`。

## 使用

- 主界面沿用 GTNG 标准机器界面；通过 NEI 查询拆解配方，没有专用按钮或 LDLib 子界面。
- 将 NEI 列出的成品放进输入总线。每批消耗数量与原配方的成品数量一致；不足一批的物品留在输入端。
- 物品经输出总线返还，流体直接进入输出仓。不需要微光，也不生成流体包。
- 每批基础耗电 1920 EU/t、耗时 100 tick；最多 16 并行，实际并行受供电、输入数量和输出容量限制。
- 全局配方加速设置同样生效（当前默认固定 1 tick）。NEI显示注册后的实际耗时。
- 使用项目默认完美超频、批处理、输入分离、配方锁定和溢出保护。保持保护开启可避免输出空间不足时丢弃产物。
- 控制器通过 EV 装配机制造：EV 机器外壳、机械臂、活塞、传感器、电路、钛板和焊锡合金。

## 异形结构

外接尺寸为 **7 宽 × 5 高 × 5 深**。左侧是高玻璃反应舱，右侧是仅三层高的偏置能量舱。
控制器位于正面，从左数第三格、从上数第三格。可用 StructureLib 投影与生存建造。

- C：坚实钢机械方块（GT `sBlockCasings2:0`），至少保留 12 块；其余可替换为仓室。
- F：钛框架；P：钢管机械方块（GT `sBlockCasings2:13`）。
- G：硼硅玻璃（BartWorks `bw_realglas:0`）；L：萤石。
- `-`：必须为空气；空格：不检查的位置；`~`：控制器。
- 必需：输入总线、输出总线、输出仓、能源仓各至少一个；启用维护检查时需要维护仓。

每组是一层，从上到下；每层各行从正面到背面：

```text
FCCCF    FGGGF    CC~CC    FGGGF    FCCCF
CPPPC    G---G    G---GCF  G---GGC  CCCCCCF
CPLPC    G-P-G    G-P-PPL  G-P-G-C  CCPCCPC
CPPPC    G---G    G---GCF  G---GGC  CCCCCCF
FCCCF    FGGGF    FGGGF    FGGGF    FCCCF
```

## 拆解规则

参考 GT Not Leisure dev-290 固定提交 `f1b74060d2a91b422eb950076075c601a644f882`。
同时安装 GTNL 时，直接读取它完成注册后的微光转换表，包括特殊覆盖配方；尊重它的微光配方开关。
未安装 GTNL 时，依次读取装配机、装配线、太空装配机和 GT 机器工作台配方。
工作台范围与微光相同，仅捕获 `ItemMachines` 成品，不再反转所有普通合成。
沿用上游黑名单、解包配方排除和材料替换规则，取消之前自行设计的共同材料最低返还算法。
GTNL 新增或修改配方的特殊覆盖，仅在安装 GTNL 时读取；未安装时按当前整合包已有配方生成。移植来源及 LGPL 许可见
`src/main/resources/META-INF/shimmer-port/NOTICE.md`。

物品、数量与选择规则来自微光；执行方式改为机器耗电、原生流体输出、NBT 匹配和输出保护。
配方界面每页一条，物品 4×4 网格，流体独立一行，说明位于槽位下方。
基础耗时 100 tick，当前全局加速配置将其改为 1 tick，因此 NEI 显示 1 tick 符合实际执行。

## 验证

`gradlew.bat -I scripts/transmutation-qa.init.gradle runClient25` 在一次性创造世界中验证
结构成型、NBT 拒绝、整批消耗与余数、物品/流体返还、输出满载保护、标准 GUI 无查询按钮及 NEI 布局。
额外断言拆解表没有粒压成锭的配方。主界面进行三次资源重载检查。
添加 `-PtransmutationQaLiveResources` 可直接读取开发构建目录资源，验证 IDEA 的开发资源加载路径。
截图位于 `build/transmutation-qa/screenshots`；QA 类不进入发布 JAR。

2026-09-26：Java 8 与 Java 25 客户端第一轮检查均通过，开发环境生成 3,062 条微光规则配方。
原 Java 25 崩溃指向 `textures/gui/modernity/background.png` 解码为空；源文件、构建资源与 JAR 内的图片
均可读取，第一轮复测未复现该崩溃，不能据此断言原崩溃根因已修复。

Java 25 第二轮直接读取开发目录资源，并连续重载三次贴图，全部检查通过；粒→锭回归检查通过。

