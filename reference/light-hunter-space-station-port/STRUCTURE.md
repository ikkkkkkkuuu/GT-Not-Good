# 猎光号空间站与枢纽卫星工厂结构

固定来源 Dragonators/GTLAdditions `34fb152886164c56a07fc9d1b10e056f0c22b7ab`，GPL-3.0-only。
原始模式与符号映射取自 `MultiBlockMachine.kt:205-233,2088-2131`；原始 JSON 和源码保存在 `upstream/gtladditions/`。
`upstream/` 不参加 Java 编译或资源打包。实际资产来源、修订、许可、原始及目标哈希见
`STRUCTURE_ASSET_MANIFEST.json`；同一清单和许可证随 JAR 放在 `META-INF/light-hunter-space-station-port/`。

| 结构 | 宽×高×深 | 非空（含控制器） | 原控制器（列、行、切片） | StructureLib 偏移 |
| --- | --- | --- | --- | --- |
| 猎光号主站 | 73×73×189 | 48,567 | (36,36,188) | (36,36,0) |
| 四种卫星工厂共用结构 | 5×21×13 | 795 | (2,10,12) | (2,10,0) |

MBS 无损保存全部原切片、行与符号，空格保持任意方块条件。加载转换为
`shape[height-1-row][length-1-aisle][column]`，宽度不镜像，再交给 `StructureUtility.transpose`。
两个结构工具均提供独立数组副本的 `shape()`、`SYMBOLS` 字符串、`block(char)`、`metadata(char)` 和 `element(char)`。
主站 `d` 和模块 `O` 的默认块均为高能机器外壳，控制器自行覆盖仓室规则；主站 `^`、模块 `N` 均为静态原模块连接器。

## 对接几何

主站有 20 个原连接器，源坐标为 `(26或46,36,75+8k)`，`k=0..9`；相对控制器的 ABC 为
`(±10,0,113-8k)`。卫星工厂的 `N` 位于源 `(2,10,11)`，相对自身控制器 ABC 为 `(0,0,1)`，即背后一格。
因此卫星工厂控制器位于主站横向 ±9、后方 41..113（间距 8），面向主轴，连接器才能与 ±10 原槽位重合。
每座工厂与原主站共享 770 个静态方块，另有 1 个控制器和 24 个 `O` 仓位；四向各 20 槽离线逐格叠放均无材质冲突。
若 `outer = moduleOffset(hostFacing,0,side)`、`side=±9`，正确模块正面向量为 `(-outerX/9,0,-outerZ/9)`。

## 方块与材质

新增 `common.blocks.lighthunter.LightHunterCasing` 的 20 个变体及 `LightHunterBlocks.register()`，固定 GT 材质页 127。
15 种普通金属件位于 `light_hunter_casing`，4 种工作态件位于 `light_hunter_casing_2`，力场玻璃单独注册。
超级计算机组件、超能核心、MK-V/MK-IV 聚变机械方块的元数据分别为 0/1/2/3，工作态为 8/9/10/11。
`asElement()` 接受两态，`IBlockWithActiveOffset` 使用位 8；主机通过原生 `activeCoils`/`GTCoilTracker` 同步工作外观，
保留共享核心引用计数。普通金属件及玻璃不应用工作态偏移，掉落和物品形式统一为非工作态。

- GTCEu Modern `e3844ba943e2388c0856617ae4f8b473a0ae1b64`：电压外壳、高能外壳、散热口、计算机外壳、
  UHV 终极电容器、密封层、聚变发光层及 `data_bank`/`fusion_reactor` 控制器面板；LGPL-3.0-only。
- GTLCore `6443c848264fb15c90cf50e031f146522616a8b9`：龙之力量三钛、回响、恒星容器、超级计算机组件、
  超能核心及 MK-IV/MK-V 聚变原模型与材质；LGPL-3.0-only。保留默认/工作态及原发光层。
- 官方 GregTech Leisure 1.4.5.3，CurseForge 文件 9238465：模块连接器、力场约束装置、中子素管道、力场玻璃及维度桥接方块；
  原归档根 LICENSE 为 CC BY-NC-SA 4.0。归档 SHA-256 为
  `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。

原 PNG 与动画元数据按字节复制；14 套 CTM 共 658 张图由原始四象限无损组合。
布局沿用现有移植的 LDLib-MultiLoader `1d4e4390ae58d16038845c651e122185c29f334b` 算法，GPL-3.0-only。
密封机械方块侧面按原模型的两个重合立方体面做原分辨率透明合成，未重画；其余多面布局按原 JSON 保留。
控制器资源为 `lighthunter/station_front{,_active,_glow,_active_glow}` 与 `module_front` 同套资源，
底色使用 `LightHunterCasing.HighPowerCasing`。普通和工作面板分别选择各自发光层。
这些外部资产及改编源码不属于项目通用 MIT 许可。

原石英玻璃灯与中子素框架分别直接使用 GTNH AE2、GregTech 的同名原生方块。
`naquadah_alloy_casing` 的原模型直接引用超能机械方块材质，因此复用已有 `FieldExtruderCasing.HyperMechanical`。
`sps_casing` 复用已有 `HeartCasing.TemporalAnchorFieldCasing` 的同源 `sps_casing` 原图，未替换为其他外观。
其余既有同源件复用维度聚焦、力场压模、巨型组装及克尔-纽曼移植，沿用相应原始许可记录。
克尔-纽曼复用件的原始基图已逐像素核对；维度桥接方块原图是 16×64 四帧，与既有两帧版本不同，
因此单独注册，中央物品枚举为 `LightHunterDimensionalBridgeCasing`，不改变旧方块。
MK-IV 聚变机械方块也单独注册为 `LightHunterFusionCasingMkIV`，保留工作态发光，避免改动旧静态方块。

## 符号映射

### 主站

| 符号 | 数量 | GTNH 映射 |
| --- | ---: | --- |
| `=` | 34 | `LightHunterCasing.NeutroniumPipeCasing` |
| `A` | 2528 | `LightHunterCasing.UhvMachineCasing` |
| `B` | 9558 | `FieldExtruderCasing.HyperMechanical` |
| `C` | 1029 | `LightHunterCasing.UxvMachineCasing` |
| `D` | 3528 | `DimensionalFocusCasing.FusionGlass` |
| `E` | 5884 | `DimensionalFocusCasing.DimensionInjection` |
| `F` | 1038 | `LightHunterCasing.DimensionalBridgeCasing` |
| `G` | 280 | `FieldExtruderCasing.ContainmentFieldGenerator` |
| `H` | 3651 | `KerrNewmanCasing.AtomicCasing` |
| `I` | 1348 | `KerrNewmanCasing.ExtremeDensityCasing` |
| `J` | 560 | `原生中子素框架` |
| `K` | 666 | `LightHunterCasing.EchoCasing` |
| `L` | 293 | `LightHunterCasing.DragonStrengthTritaniumCasing` |
| `M` | 27 | `LightHunterCasing.HyperCore` |
| `N` | 4094 | `DimensionalFocusCasing.DimensionallyTranscendent` |
| `O` | 2400 | `LightHunterCasing.UxvHermeticCasing` |
| `P` | 1440 | `LightHunterCasing.RestraintDevice` |
| `Q` | 1500 | `AssemblyPlantCasing.SpacetimeAssemblyLineUnit` |
| `R` | 434 | `LightHunterCasing.HighPowerCasing` |
| `S` | 224 | `FieldExtruderCasing.Molecular` |
| `T` | 1073 | `原生 AE2 石英玻璃灯` |
| `U` | 64 | `LightHunterCasing.FusionCasingMkIV` |
| `V` | 80 | `HeartCasing.TemporalAnchorFieldCasing` |
| `W` | 176 | `LightHunterCasing.FusionCasingMkV` |
| `X` | 208 | `LightHunterCasing.UltimateStellarContainmentCasing` |
| `Y` | 702 | `KerrNewmanCasing.EnhancedHyperCasing` |
| `Z` | 120 | `DimensionalFocusCasing.ImprovedSuperconductor` |
| `[` | 12 | `LightHunterCasing.ForceFieldGlass` |
| `]` | 4643 | `KerrNewmanCasing.GravityStabilizationCasing` |
| `^` | 20 | `LightHunterCasing.ModuleConnector` |
| `_` | 258 | `LightHunterCasing.AdvancedComputerCasing` |
| ``` | 230 | `LightHunterCasing.ComputerHeatVent` |
| `a` | 120 | `LightHunterCasing.SuperComputationComponent` |
| `b` | 180 | `LightHunterCasing.UivMachineCasing` |
| `c` | 144 | `LightHunterCasing.UhvUltimateBattery` |
| `d` | 20 | `LightHunterCasing.HighPowerCasing` |

### 卫星工厂

| 符号 | 数量 | GTNH 映射 |
| --- | ---: | --- |
| `A` | 20 | `LightHunterCasing.EchoCasing` |
| `B` | 22 | `LightHunterCasing.UxvMachineCasing` |
| `C` | 16 | `KerrNewmanCasing.AtomicCasing` |
| `D` | 4 | `LightHunterCasing.DragonStrengthTritaniumCasing` |
| `E` | 1 | `LightHunterCasing.HyperCore` |
| `F` | 119 | `DimensionalFocusCasing.DimensionallyTranscendent` |
| `G` | 102 | `LightHunterCasing.UhvMachineCasing` |
| `H` | 132 | `FieldExtruderCasing.HyperMechanical` |
| `I` | 120 | `LightHunterCasing.UxvHermeticCasing` |
| `J` | 72 | `LightHunterCasing.RestraintDevice` |
| `K` | 70 | `DimensionalFocusCasing.DimensionInjection` |
| `L` | 75 | `AssemblyPlantCasing.SpacetimeAssemblyLineUnit` |
| `M` | 16 | `LightHunterCasing.DimensionalBridgeCasing` |
| `N` | 1 | `LightHunterCasing.ModuleConnector` |
| `O` | 24 | `LightHunterCasing.HighPowerCasing` |

## 离线验证

已逐格解码 MBS 对比全部 1,008,546 个原坐标、两控制器偏移与所有空格，核对原始和派生资源哈希，
并验证全部零邻接 CTM 的像素与原图一致。`LightHunterStructureTest` 有 8 项结构、编码与副本检查；
实际编译、单元测试及客户端结果由统一构建记录，本素材导入不启动 Gradle 或游戏。
