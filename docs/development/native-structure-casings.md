# 多方块原生结构件复用

核对日期：2026-10-23。以实际依赖 GT5-Unofficial 5.09.54.205 为准；源码 JAR SHA-1
`5447ea903c5833f0319123e38ac586f348e599f3`，提交 `a3e1e11241a814c9fa0dd0973d5699548428f689`。
核对 `gregtech/api/casing/Casings.java`、`ItemList`、实际方块注册及中英文名称。
GT5U 同一发布包含本表用到的 TecTech、GT++、BartWorks、GTNH Lanthanides 和 Intergalactic 实现。

## 规则与兼容

- 新结构、建造提示、控制器/仓室背景和配方输入优先使用已注册的原生方块；保留机器几何与处理逻辑。
- 旧独立方块已随 1.6.6/1.6.7 发布，保留注册名、metadata、旧物品 NBT 和旧结构识别，避免存档缺失。
  这些兼容变体不再显示在创造栏，也不再有生产配方。组装机用旧方块 + 25 号电路，50 tick、16 EU/t，1:1 转换为原生方块。
  对撞机原有的 6 号电路回收途径保留；不会增加原生方块转回复制件的配方。自动嬗变/逆向回收生成器跳过这些迁移输入，避免重新生成旧件。
- `NativeStructureCasings` 统一对应关系，`StructureCasing.asElement()` 优先建造原生块，同时接受旧块。
  本模组物品容器在外部注册完成后绑定原生堆栈；旧块的贴图注册仍保留，原生纹理延后解析。
- 层压玻璃、钢化玻璃和六种心脏/猎光结构件沿用原来已经存在的一比一外观转换关系。
  UIV/UXV 使用机械方块本体，不把机器外壳物品或电池物品当作可放置方块。
- 没有确认对应原生方块的专用结构件保留。不能只凭相近的材料名或等级合并不同构件。

## 替换表

38 个旧变体统一复用 33 种原生方块；多个旧系列共享同一原生物品。

| 旧代码变体 | 旧名称 | 原生引用 |
| --- | --- | --- |
| AssemblyPlantCasing.LaminatedGlass | 层压玻璃 | `Casings.ChemicalGradeGlass` |
| AssemblyPlantCasing.FilterCasing | 过滤机械方块 | `Casings.FilterMachineCasing` |
| AssemblyPlantCasing.PtfePipeCasing | 聚四氟乙烯管道方块 | `Casings.PTFEPipeCasing` |
| AssemblyPlantCasing.TemperedGlass | 钢化玻璃 | `new ItemStack(ItemRegistry.bw_realglas, 1, 0)` |
| DimensionalFocusCasing.DimensionallyTranscendent | 超维度机械方块 | `Casings.DimensionallyTranscendentCasing` |
| DimensionalFocusCasing.DimensionInjection | 维度注入机械方块 | `Casings.DimensionalInjectionCasing` |
| DimensionalFocusCasing.MolecularCoil | 分子线圈 | `Casings.MolecularCoil` |
| FieldExtruderCasing.Iridium | 铱强化机械方块 | `Casings.AdvancedIridiumPlatedMachineCasing` |
| FieldExtruderCasing.Molecular | 分子机械方块 | `Casings.MolecularCasing` |
| FieldExtruderCasing.Hollow | 中空机械方块 | `Casings.HollowCasing` |
| FieldExtruderCasing.ContainmentFieldGenerator | 遏制场发生器 | `Casings.ContainmentFieldGenerator` |
| HeartCasing.DysonDeploymentMagnet | 戴森球模块部署单元超导磁体 | `ItemList.DysonSwarmDeploymentUnitMagnet.get(1)` |
| HeartCasing.GravitonFieldConstraintCasing | 引力场约束方块 | `Casings.UltimateContainmentFieldGenerator` |
| HeartCasing.DimensionalStabilityCasing | 维度稳定方块 | `Casings.CosmicFabricShieldingCore` |
| HeartCasing.TemporalAnchorFieldCasing | 时间锚定场外壳 | `Casings.SpacetimeAlteringCasing` |
| HeartCasing.DimensionConnectionCasing | 维度连接机械方块 | `Casings.DimensionalBridgeGenerator` |
| HeartCasing.CreateCasing | 创造机械方块 | `Casings.InfiniteSpacetimeEnergyBoundaryCasing` |
| KerrNewmanCasing.PressureContainmentCasing | 压力容器机械方块 | `Casings.PressureContainmentCasing` |
| KerrNewmanCasing.ShieldedAcceleratorCasing | 屏蔽加速器机械方块 | `Casings.ShieldedAcceleratorCasing` |
| KerrNewmanCasing.DimensionalBridgeCasing | 维度桥接方块 | `Casings.DimensionalBridge` |
| KerrNewmanCasing.QuantumGlass | 量子玻璃 | `Casings.QuantumGlass` |
| KerrNewmanCasing.FusionCasingMkIV | 聚变机械方块 MK IV | `Casings.FusionMachineCasingMKIV` |
| KerrNewmanCasing.AdvancedFusionCoil | 进阶聚变线圈方块 | `Casings.AdvancedFusionCoil` |
| LightHunterCasing.UhvMachineCasing | UHV机械方块 | `Casings.UHVMachineCasing` |
| LightHunterCasing.UivMachineCasing | UIV机械方块 | `Casings.UIVMachineCasing` |
| LightHunterCasing.UxvMachineCasing | UXV机械方块 | `Casings.UXVMachineCasing` |
| LightHunterCasing.ComputerHeatVent | 计算机散热口 | `Casings.ComputerHeatVent` |
| LightHunterCasing.HighPowerCasing | 高能机器外壳 | `Casings.HighPowerCasing` |
| LightHunterCasing.AdvancedComputerCasing | 高级计算机外壳 | `Casings.AdvancedComputerCasing` |
| LightHunterCasing.UxvHermeticCasing | 密封机械方块 XII | `Casings.HermeticCasing12` |
| LightHunterCasing.ForceFieldGlass | 力场玻璃 | `Casings.ForceFieldGlass` |
| LightHunterCasing.DimensionalBridgeCasing | 维度桥接方块 | `Casings.DimensionalBridge` |
| LightHunterCasing.FusionCasingMkIV | 聚变机械方块 MK-IV | `Casings.FusionMachineCasingMKIV` |
| LightHunterCasing.UltimateStellarContainmentCasing | 终极恒星热力容器 | `Casings.SingularityReinforcedStellarShieldingCasing` |
| ParticleColliderCasing.SuperconductingCoil | 超导线圈 | `Casings.SuperconductingCoilBlock` |
| TentacleCasing.MolecularCasing | 分子机械方块 | `Casings.MolecularCasing` |
| TentacleCasing.ContainmentFieldGenerator | 遏制场发生器 | `Casings.ContainmentFieldGenerator` |
| TentacleCasing.HollowCasing | 中空机械方块 | `Casings.HollowCasing` |

## 继续保留的结构件

以下已核对当前依赖，没有直接对应或既有一比一原生方块转换证据；保留其专用配方和结构身份。
湮灭核心与 UHV 终极电池原配方使用的是原生物品，不能直接作为结构方块，因此保留适配方块。

- `AssemblyPlantCasing`：进阶装配线控制外壳、超时空装配单元、二硅化钼线圈方块、电解槽。
- `DimensionalFocusCasing`：聚变玻璃、改良型超导线圈方块、激光安全蚀刻机械方块。
- `FieldExtruderCasing`：超能机械方块、洁净室玻璃。
- `HeartCasing`：湮灭核心、铼强化聚能玻璃。
- `KerrNewmanCasing`：强化超能机械方块、氧化锆陶瓷高强度耐弯折机械方块、安普洛齿轮箱机械方块、安普洛管道方块、油气输送密封管道机械方块、耐高压管道机械方块、重力稳定机械方块、工业无框玻璃、生物活性机械方块、安普洛活性机械方块、高强度支撑主轴、UHV加速器磁约束线圈机械方块、极密机械方块、PPS耐腐蚀机械外壳、等离子加热机械方块、原子机械方块、辐射吸收机械方块。
- `LightHunterCasing`：超级计算机组件、太空电梯模块连接器、龙之力量三钛合金机械方块、UHV终极电池、回响强化机械方块、超能核心、中子素管道方块、聚变机械方块 MK-V、力场约束装置。
- `ParticleColliderCasing`：路菲恩机械方块、加速管道。
- `TentacleCasing`：乌鲁线圈方块、超时空装配外壳、激光冷却方块、硅岩合金机械外壳、夸克管道、简并态铼约束外壳、无菌过滤机械方块、超时空装配单元、约束装置、激光机械方块、电子渗透安普洛涂层玻璃。

Stellar 的 54 种群星之外专用结构外观和 28 种装饰面板也已核对；GTNH 的空间、时间、辐射等基础方块原本已经直接复用，
其余名称/用途属于该移植的独有构件，没有新增第二套原生方块。
大型熔炉已使用原生焦炉砖、耐火砖和钢机械方块；末晶谐振器和碎片世界采集器是单方块机器，没有另注册多方块结构件。
增强版对撞机的大型几何修改由并行任务维护，本轮只接入共用旧线圈的物品兼容。

## 验证

实机入口为 `scripts/native-casing-qa.init.gradle`，夹具 `scripts/qa/NativeCasingClientChecks.java`。
覆盖每个映射的原生注册身份、旧 NBT、创造栏隐藏、原生与旧方块结构接受、缺块拒绝、实际建造优先项，
扫描工作台、GT 处理和装配线配方确保不再生产旧复制件，并验证旧件 1:1 回收。
另测猎光号、卫星工厂、宇宙之心全部符号映射，完整场驱动压模工厂四方向建造/成型/仓室/拆块/修复，以及实际物品贴图和资源重载。
2026-10-23 最终实机报告 PASS：1,190 项检查、38 项替换；已查看资源重载后的完整图标矩阵。
`spotlessApply build` 通过，705 项单元测试无失败/错误、1 项既有跳过；重复构建复用 configuration cache。
日志位于 `.gradle/native-casing-audit/`，构建为 `final-build.log` / `final-repeat.log`，实机为 `client-final.log`。


可重复执行的命令（PowerShell；引号保留 `-P` 参数中的盘符）：

```powershell
& scripts/dev/Invoke-Gradle.ps1 '--init-script' 'scripts/native-casing-check.init.gradle' '-PnativeCasingBuildDirectory=D:/CodexTools/GTNGNativeCasings-20261023/build' '--project-cache-dir' 'D:/CodexTools/GTNGNativeCasings-20261023/cache' spotlessApply build --offline --console=plain
& scripts/dev/Invoke-Gradle.ps1 '--init-script' 'scripts/native-casing-qa.init.gradle' '--init-script' 'scripts/native-casing-check.init.gradle' '-PnativeCasingBuildDirectory=D:/CodexTools/GTNGNativeCasings-20261023/qa-build' '--project-cache-dir' 'D:/CodexTools/GTNGNativeCasings-20261023/qa-cache' runClient25 --offline --no-configuration-cache --console=plain
```

QA 只操作独立世界，使用 `HideFactoryClient.ps1` 静音、透明、穿透并隐藏任务栏。
输出在 `D:/CodexTools/GTNGNativeCasings-20261023/qa-build/native-casing-qa/`：
`run/native-casing-qa-result.txt`、`mappings.tsv` 与 `screenshots/native-casings-*.png`。
本轮未重跑每台巨型机器的完整加工/能源流程或独立服务端；这些业务代码未修改，不将已有历史测试冒认为本轮覆盖。
