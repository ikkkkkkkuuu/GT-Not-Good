# 宇宙之心结构与材质

结构使用 GTLAdditions `34fb152886164c56a07fc9d1b10e056f0c22b7ab` 的
`scripts/structure_sources/multiblock/annihilate_generator.json`，GPL-3.0。
原 JSON 与 GTLASB3 二进制均保存于 `upstream/gtladditions/`，不参与编译和资源打包。
运行资源改为旧版机器通用的 GZIP MBS 编码，原始符号和坐标不变。

原始尺寸为 109 个切片、每切片 112 行、每行 121 列。StructureLib 使用自上而下的水平层，
转换为 `shape[111 - row][108 - aisle] = sourceRow`；宽度不镜像。
控制器原坐标为列 62、行 23、切片 93，转换偏移为 `(62, 88, 15)`。
共有 37,144 个非空位置，其中包含控制器一格。原有空格保持任意方块条件。

| 符号 | 原始方块 | GTNH 映射 | 数量 |
|---|---|---|---:|
| A | GTLCore graviton_field_constraint_casing | HeartCasing.GravitonFieldConstraintCasing | 2839 |
| B | KubeJS annihilate_core | HeartCasing.AnnihilateCore | 977 |
| C | GTLCore hyper_mechanical_casing | FieldExtruderCasing.HyperMechanical | 17604 |
| D | KubeJS dimensional_stability_casing | HeartCasing.DimensionalStabilityCasing | 555 |
| E | GTLAdditions temporal_anchor_field_casing | HeartCasing.TemporalAnchorFieldCasing | 11539 |
| F | GTCEu fusion_glass | DimensionalFocusCasing.FusionGlass | 253 |
| G | KubeJS annihilate_core | HeartCasing.AnnihilateCore | 213 |
| H | GTLCore rhenium_reinforced_energy_glass | HeartCasing.RheniumReinforcedEnergyGlass | 152 |
| P | GTLCore dimension_connection_casing | HeartCasing.DimensionConnectionCasing | 628 |
| R | KubeJS dyson_deployment_magnet | HeartCasing.DysonDeploymentMagnet | 1336 |
| S | GTLCore dimension_injection_casing，支持仓室替换 | DimensionalFocusCasing.DimensionInjection | 26 |
| T | GTLCore create_casing | HeartCasing.CreateCasing | 1021 |
| ~ | 控制器 | 宇宙之心 | 1 |

GTLCore 材质固定于 `GNSW/GTLCore` 的 `6443c848264fb15c90cf50e031f146522616a8b9`，LGPL-3.0。
GTLAdditions 的时间锚定场外壳实际引用 GTLCore 的 `casings/sps_casing` 材质，保留该映射。
引力子场外壳、时间锚定场外壳与铼玻璃按原始 LDLib 图集无损组合 141 张 CTM 象限纹理。
组合算法沿用 `reference/field-extruder-port/generate-connected-textures.py`，源自
LDLib-MultiLoader `1d4e4390ae58d16038845c651e122185c29f334b`，GPL-3.0。

湮灭核心、维度稳定外壳和戴森部署磁体使用官方 GTL 1.4.5.3 整包
（CurseForge 文件 `9238465`）内的原始 PNG，CC BY-NC-SA 4.0。
ZIP SHA-256 为 `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
维度稳定外壳保留原始动画；戴森磁体保留侧面与顶底面区别；湮灭核心保留亮度 4。
这些材质各自保留上游许可，不属于项目通用 MIT 授权。

控制器底色使用 GTCEu Modern `e3844ba943e2388c0856617ae4f8b473a0ae1b64` 的
`casings/hpca/high_power_casing`，LGPL-3.0。正面使用 GTLCore 的
`assets/gtceu/textures/block/multiblock/cosmos_simulation/overlay_front` 及运行/发光层；
顶面使用 GTLAdditions 覆盖的 `multiblock/top/cosmos_simulation/overlay_top` 及运行层，GPL-3.0。
这些图及 16 tick 帧动画均原样复制到 `heart/controller_*`，保留原版各面的材质与工作状态。

原始资产来源、修订、去向、修改和 SHA-256 见 `STRUCTURE_ASSET_MANIFEST.json`，
其副本与许可证随包保存于 `META-INF/heart-of-the-universe-port/`。

重新导入：

```text
python reference/heart-of-the-universe-port/import-structure-assets.py <官方GTL-1.4.5.3.zip>
```
