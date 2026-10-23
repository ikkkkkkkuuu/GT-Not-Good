# 碎片世界采集器移植

## 来源与范围

- 用户提供的实例：`C:\Users\Administrator\Desktop\10-22\.minecraft\versions\GregTech Leisure-增强版`。
- 本地机器来自 `gtlcore-1.2.3.2-fix4.jar`，SHA-256 和原始贴图逐项哈希见包内 `META-INF/fragment-world-collector-port/MANIFEST.json`。
- 可读源码固定于 [GNSW/GTLCore 6443c848264fb15c90cf50e031f146522616a8b9](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9)，本目录 `upstream/` 保留三个相关源文件。LGPL-3.0-only，包内有许可证和 NOTICE；外部素材及改编配方不属于项目通用 MIT 许可。
- 本地二进制反编译的 `skyOres`、`skyOres_n`、`skyFluid`、`skyFluid_n`、`skyDNumber`、`skyDMap` 与固定源码逐 token 比较一致。本地 JAR 与源码提交分别记录，不推断其构建提交。
- 普通 `ulv_fragment_world_collection_machine` 是 ULV 单方块；`large_fragment_world_collection_machine` 是独立大型机器，不在本次范围。GTLAdditions / Enhancedcore 的星核剥离器复制此池到独立机器，不是普通机器的替换实现。

## GTNH 实现

注册 ID **28633**，内部名 `FragmentWorldCollector`。两个输入槽、独立电路槽、十二个输出槽及各一个输入/输出储罐。保留七份原始纹理/动画文件，底壳使用 GTNH ULV 原生外壳。

GUI 继承 GTNH 单方块界面，输出槽按四列三行排列并扩展容器宽度，避免原九槽容器导致越界。物品/流体自动输出、两个原生开关、Shift 点击选面及存档同步全部复用 GTNH；新放机器沿用原版默认关闭状态。不另造按钮、选面面板或搬运逻辑。电路槽使用原生选择器，不额外显示本机配方用不到的虚拟模具槽。

全部配方基础耗电 8 EU/t。矿物配方耗时为 `24000 / 原表分母` tick；流体、特殊资源、大马士革钢配方为 200 tick。仍受本模组既有 CutCorners 配方加速设置影响；默认固定 1 tick 是该设置的效果。

维度方块放入输入槽，不消耗，可以在任意维度运行。原 0.5% 世界碎片产出也替换为对应维度方块。获取方式复用现有搅拌机维度方块配方，不另加 GTL 的世界碎片、火箭、奇迹水晶兑换链。

| GTL 维度 | GTNH 维度展示方块 |
| --- | --- |
| 主世界 | 主世界 / Ow |
| 下界 | 下界 / Ne |
| 末地 | 末地 / ED |
| 远古世界 Reactor | 暮色森林 / TF（用户确认） |
| 月球 | 月球 / Mo |
| 火星 | 火星 / Ma |
| 金星 | 金星 / Ve |
| 水星 | 水星 / Me |
| 谷神星 | 谷神星 / Ce |
| 木卫一 | 木卫一 / Io |
| 木卫三 | 木卫三 / Ga |
| 冥王星 | 冥王星 / Pl |
| 土卫二 | 土卫二 / En |
| 土卫六 | 土卫六 / Ti |
| 霜原星 Glacio | 海卫一 / Tr（用户确认） |
| 巴纳德 | 巴纳德 C / BC |

原始六表完整保存在 JSON：50 种矿脉组合、26 种流体定义和16组维度分配。按维度展开，不改矿物数量和权重。矿物选原生 `rawOre`，不存在时选同材料 `crushed`；Goethite→BrownLimonite、Hematite→BandedIron、Plutonium239→Plutonium。岩石使用该星球 Galacticraft/GalaxySpace 方块；深板岩粉使用 GTNH 原生 StoneType。

矿物电路保留源编号（主世界 2–23，其余从 1 开始）。GTNH 集成电路最大配置 24，流体电路按该维度原表顺序改为 1–N，并须插入钻头。专用后端拒绝多余类型的输入，也对缓存配方检查，防止装着钻头却匹配同电路的矿物配方。特殊资源用电路 0，主世界有机资源用 1，暮色大马士革钢用 9 并需钢粉及润滑油。

钢、钛、硅岩合金、中子素四种钻头分别为 1%、0.9%、0.8%、0.7% 消耗概率；产液倍数为 1、16、128、1024。GTL 的满额研磨头外壳改为无尽钻头，0.6% 消耗，单次输出 2,147,483,647 mB。储罐容量适配为该数值，开工前以 long 检查相加，已存同液 1 mB 时拒绝满额配方，避免 int 溢出。

机器合成保留铅线、橡胶原木、LV 外壳与铁砧结构；GTL 宝藏/采矿水晶分别替换为 LV 传感器/发射器。

## 可用性与差异

- 原物品和流体概率沿用 GTNH 原生概率配方 API。樱花/红树树苗、蜜脾、灵魂土、远古残骸、两种下界菌、龙息和紫颂产物使用 Et Futurum Requiem；关闭相关上游配置或不装该模组时只略去不可用项。
- 疑似沙子/沙砾改为普通沙子/沙砾；冰碎片改为原生冰粉，仍为 95%。霜原星特殊资源配方随海卫一映射保留。
- GTL 独有采矿/宝藏/奇迹水晶及其下游链未新增；霜原星精魄、AE2 mysterious_cube、幽匿尖啸体/感测体、海带和龙首没有本次已核实的等价原生物品，不生成虚假替代物。
- 本次固定 GTNH 依赖没有可解析的 Chalcocite、Calorite、Ostrum、Celestine 矿物、Diorite 粉及 BarnardaAir。保留原表名称并在启动日志列出不可用项；仅略去缺失产物，不删除所在维度的其他采集配方。以后添加提供相同矿辞/流体的模组可自动启用。
- NaturalGas、Helium3、HydrochloricAcid 使用原生 Materials 字段，避免 GTNH 历史内部名拼写差异；RawOil→Oil，GTL Oil→OilMedium。其他已核实流体按原生 Materials 或流体注册表解析。

## 再生成与验证

在仓库根目录运行 `python reference/fragment-world-collector-port/prepare.py <本地GTLCore.jar>` 可重建 JSON、原始贴图和来源清单；此工具不参与构建。

真实客户端 QA：`scripts/fragment-collector-qa.init.gradle`，加 `-PfragmentCollectorClientQA`，可用 `-PfragmentCollectorQaRoot=<独立目录>` 隔离 build/run。通过 `scripts/dev/Invoke-Gradle.ps1` 执行 `spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache`。

夹具只操作自行启动的静音、透明、穿透且隐藏任务栏的客户端。逐条配方通过实际机器检查，验证16维度、输入保留、缓存切换、钻头和流体概率、满仓拒绝、流体溢出边界、完整 TileEntity NBT 恢复后恰好完成一次；另检查原生自动输出到箱子/流体输入仓、开关及满箱保留、NBT 方向与状态恢复、实际按钮和 Shift 选面双端同步，三种 GUI 比例、12槽同步和 NEI 及实际运行方块截图。具体命令、结果及失败修复记录见 `docs/development/progress.md`。
