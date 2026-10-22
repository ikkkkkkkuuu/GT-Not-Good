# 克尔-纽曼均质仪来源

固定来源为 [GTOCore 3121e23241e7f6ccd2e6a8dbd5655962de63ed9a](https://github.com/GregTech-Odyssey/GTOCore/tree/3121e23241e7f6ccd2e6a8dbd5655962de63ed9a)，LGPL-3.0。
本目录只作来源参考，不参与 Java 编译或资源打包。打包声明和清单位于 `src/main/resources/META-INF/kerr-newman-port/`。

## 结构和字符映射

`upstream/pattern/kerr_newman_homogenizer.mbs` 是原始 gzip MBS，9446 字节，SHA-256
`1e97839453a7d5e31e1460c14a4ca217ff5214b7ed7949ebbd8e7d2a3c176dde`。
运行资源是其逐字节副本，尺寸 **83×36×83**，未缩小、未裁切。
原始轴为 LEFT、UP、FRONT；源控制器在 aisle 75、row 23、column 41。
转换为 StructureLib 顶到底的水平层时采用 `result[35-row][82-aisle]`，宽度不反转，仅把控制器 `b` 换为 `~`。
对应 offset 为 **(41,12,7)**。232366 个空格继续表示 `any()`，不能换成空气。

以下映射来自 `MultiBlockH.java` 的 `KERR_NEWMAN_HOMOGENIZER`，数量均为原始结构字符数量：

| 字符 | 数量 | 原始构件 | 本地元素 |
| --- | ---: | --- | --- |
| A | 512 | ENHANCE_HYPER_MECHANICAL_CASING | KerrNewmanCasing.EnhancedHyperCasing |
| B | 1200 | IRIDIUM_CASING | FieldExtruderCasing.Iridium |
| C | 2220 | HYPER_MECHANICAL_CASING | FieldExtruderCasing.HyperMechanical |
| D | 668 | ZIRCONIA_CERAMIC_HIGH_STRENGTH_BENDING_RESISTANCE_MECHANICAL_BLOCK | KerrNewmanCasing.ZirconiaCeramicCasing |
| E | 2016 | PRESSURE_CONTAINMENT_CASING | KerrNewmanCasing.PressureContainmentCasing |
| F | 272 | NaquadahAlloy frame | Materials.NaquadahAlloy |
| G | 112 | AMPROSIUM_GEARBOX | KerrNewmanCasing.AmprosiumGearbox |
| H | 56 | AMPROSIUM_PIPE_CASING | KerrNewmanCasing.AmprosiumPipeCasing |
| I | 172 | OIL_GAS_TRANSPORTATION_PIPE_CASING | KerrNewmanCasing.OilGasPipeCasing |
| J | 288 | HIGH_PRESSURE_PIPE_CASING | KerrNewmanCasing.HighPressurePipeCasing |
| K | 418 | DIMENSION_INJECTION_CASING or abilities | DimensionalFocusCasing.DimensionInjection + native hatches |
| L | 386 | GRAVITY_STABILIZATION_CASING | KerrNewmanCasing.GravityStabilizationCasing |
| M | 187 | INDUSTRIAL_FRAMELESS_GLASS | KerrNewmanCasing.IndustrialFramelessGlass |
| N | 1136 | SHIELDED_ACCELERATOR | KerrNewmanCasing.ShieldedAcceleratorCasing |
| O | 110 | DIMENSION_INJECTION_CASING | DimensionalFocusCasing.DimensionInjection |
| P | 468 | HastelloyN frame | MaterialsAlloy.HASTELLOY_N |
| Q | 224 | BIOACTIVE_MECHANICAL_CASING | KerrNewmanCasing.BioactiveCasing |
| R | 104 | AMPROSIUM_ACTIVE_CASING | KerrNewmanCasing.AmprosiumActiveCasing |
| S | 544 | HIGH_STRENGTH_SUPPORT_SPINDLE | KerrNewmanCasing.HighStrengthSupportSpindle |
| T | 512 | ACCELERATOR_ELECTROMAGNETIC_COIL_CONSTRAINT_CASING_UHV | KerrNewmanCasing.AcceleratorCoilUhv |
| U | 320 | EXTREME_DENSITY_CASING | KerrNewmanCasing.ExtremeDensityCasing |
| V | 10 | DIMENSIONAL_BRIDGE_CASING | KerrNewmanCasing.DimensionalBridgeCasing |
| W | 64 | TungstenCarbide frame | Materials.TungstenCarbide |
| X | 608 | PPS_CORROSION_RESISTANT_MECHANICAL_HOUSING | KerrNewmanCasing.PpsCorrosionResistantCasing |
| Y | 256 | QUANTUM_GLASS | KerrNewmanCasing.QuantumGlass |
| Z | 168 | PLASMA_HEATER_CASING | KerrNewmanCasing.PlasmaHeaterCasing |
| [ | 896 | GCYMBlocks.CASING_ATOMIC | KerrNewmanCasing.AtomicCasing |
| \ | 472 | RADIATION_ABSORBENT_CASING | KerrNewmanCasing.RadiationAbsorbentCasing |
| ] | 528 | FUSION_CASING_MK4 | KerrNewmanCasing.FusionCasingMkIV |
| ^ | 566 | ADVANCED_FUSION_COIL | KerrNewmanCasing.AdvancedFusionCoil |
| _ | 102 | HOLLOW_CASING | FieldExtruderCasing.Hollow |
| &#96; | 32 | Trinium frame | Materials.Trinium |
| a | 10 | CONTAINMENT_FIELD_GENERATOR | FieldExtruderCasing.ContainmentFieldGenerator |
| b | 1 | controller | ~ |
| space | 232366 | any | any |

K 的原版声明允许自动输入输出/能源能力、最多一个并行控制仓和恰好一个维护仓，没有机壳最少数量声明。
本地机器类负责 GTNH 舱室规则，表中 418 只是可替换位置数。

新增 23 个外观构件，注册 `KerrNewmanBlocks.register()`，占用 3 个方块 ID：
`kerr_newman_casing`、`kerr_newman_casing_2`、`kerr_newman_glass`；固定纹理页 122。
`getMetadata()` 是对应方块内元数据，不能用 enum ordinal 代替。
运行线圈 T、^ 位于第二组元数据 3、4，运行态分别 11、12；其结构元素接受两种状态。
同样材质的连接检查先验证实际方块和枚举，不能直接对所有元数据清除 bit 8：第一组 8–15 是独立机壳。

## 纹理和动态效果

GTO 的原始 PNG、连接图集、动画 metadata 和模型声明保存在 `upstream/gto/`。
原子机壳图源为 owning fork [GregTech-Modern 30cfd9fb](https://github.com/GregTech-Odyssey/GregTech-Modern/tree/30cfd9fb3de3faee38cab6b36c5ba171ed82ba48)，同为 LGPL-3.0。
这是可核查的来源快照，不声称它与 GTO 声明依赖 GTCEu 26.7.3 的源码包逐字节一致。
原 PNG 原样打包；不兼容的 LDLib 连接 metadata 原样保存在许可证目录，运行时使用 47 种四分面组合。
生物活性、安普洛活性、维度桥接、等离子加热机壳保留原始动画帧序和帧时间。
T、^ 的上游运行模型采用 base + bloom 两层，本地用原生 GT 渲染器分别绘制普通基底与发光覆盖。
维度桥接的已注册模型只有普通 base，未启用参考目录中存在但未被引用的 bloom 图。

中央黑洞来自机器类实际启用的 `BlackHole` FX，不能把 `KerrNewmanHomogenizerRenderer` 中关闭的彩色环当作原版效果。
核心/视界半径 2.5/4.5，中心位于控制器后方 34 格；完整原始 shader 与屏幕捕获、球体、投影源码均保留。
GLSL 150 接口适配为 GLSL 120，场景颜色改从当前世界 framebuffer 捕获；保留原折射公式。
球体交换每个三角形的后两个顶点，以在原生 CCW 状态下让外表面与 shader 的 `gl_FrontFacing` 一致。
详细修改与最终哈希见 `RENDER_MANIFEST.json`。

## 重建和校验

```text
python reference/kerr-newman-port/import-assets.py
python reference/kerr-newman-port/refresh-manifests.py
```

第一条复用固定原图并生成 CTM；第二条只更新清单、验证原始结构和刷新格式化后的目标哈希。
所有已存在的原始参考文件均保留，不被脚本覆盖。
纯结构测试核对逆转换后全部坐标的哈希、完整字符数量、原始二进制、控制器位置和返回数组隔离。
渲染数学测试核对投影与球面绕序。
