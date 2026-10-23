# 巨型组装厂与超导电磁工厂客户端检查

使用独立世界运行联合检查：

```powershell
.\gradlew.bat --no-configuration-cache -I scripts/assembly-plant-qa.init.gradle runClient17
```

客户端由 `HideFactoryClient.ps1` 静音、透明、穿透并隐藏任务栏图标。世界保存在
`run/assembly-plant-qa/`，结果为 `build/assembly-plant-qa/result.txt`；只有以 `PASS` 开头才算通过。
截图写入结果目录的 `screenshots/`，文件名前缀分别为 `mage-assembler` 和 `electromagnetic-factory`。

联合夹具依次检查两台机器，覆盖：

- 全部原始构件在四个方向成形；核对前后、底部和顶部地标，保留 GTCEu 源码行轴向上增长的实际方向。
- 各固定材料缺失时失效；巨型组装厂后侧 `-` 仍要求消声仓，空白位接受石头或空气。
- 原生普通无线能源仓、本模组无线多安能源仓与原生 TecTech 无线仓均可成形；实际从仓主的原生无线账户
  提取 EU，并经实体仓缓冲向控制器供电。账户为空时无法供电；控制器不增加直连无线电网模式。
- 三种与五种模式分别处理已有原生配方，检查材料、流体精确扣除和非消耗品保留。
- 原生单配方锁定及模式的 TileEntity NBT 持久化；切换模式后清除旧配方检查器，拒绝其他模式输入。
- 带洁净室要求的配方测试临时设置 `Config.requireCleanroom=true`、
  `FakeCleanroom.CLEANROOM_BYPASS=false`，覆盖项目既有默认洁净室豁免；缺少提供者时不扣料，
  接入有效原生洁净室能力后才加工，`finally` 清除测试提供者并恢复两个配置原值。
- 完美超频将 128 tick、30 EU/t 配方变为 32 tick、120 EU/t；低压 2 A 能源限制并行。
- HV 2 A 与 MV 2 A 混压时保留全部 1280 EU/t，支持 40 个 30 EU/t 并行，耗电不超过真实输入功率。
- 多个低压仓不能叠加电压解锁高压配方；输出满时不扣输入，清空后可重试。
- 原生 ME 合成输入仓接收含原生流体 NBT 的 Ultimate Encoded Pattern，经 `MEInventoryCrafting` 和
  `PatternSlot` 接收、整仓保存恢复、真实双输入路径扣料；该项验证缓冲集成，不包含 AE CPU 网络调度。
- 等客户端完整收到 1377 / 1628 个构件后，再等待全部 55 / 496 个活动构件的原生运行、停止和失效同步。
- 真实服务端 tick 推进，运行中整机保存恢复后继续加工。
- 每机完整远景、活动材质近景、所有模式的真实 GUI 按钮同步，以及中英文名称和普通 / Shift 说明。

本轮实际客户端矿辞审计覆盖 20 种关键材料形态：

| 配方 | 核验形态 | 种数 |
| --- | --- | ---: |
| 巨型组装厂 | 碳化钨、钇钡铜氧、硅岩合金、镀铑钯螺丝；碳化钨、钨钢、Ultimet、Incoloy MA956 长杆；拉菲姆板、铱双重板 | 10 |
| 超时空装配单元 | Pikyonium 双重板、精金框架、蓝钢齿轮、红钢齿轮 | 4 |
| 超导电磁工厂 | ZPM 超导基材细导线；钽、银、硅双重板；钌板 | 5 |
| 进阶装配线控制外壳 | 铑齿轮 | 1 |

夹具逐下标读取矿辞列表，记录每个注册条目、实际配方输入、物品注册名、元数据、数量、矿辞名及匹配数，
输出段以 `ASSEMBLY_PLANT_ORE_AUDIT` 标记。实际矿辞没有钌双重板，控制器已由 8 块双重板改为
16 块原生钌板，保持材料总量；没有 HSLA 长杆时使用 32 根钨钢长杆。

生产控制器沿用现有 `CleanroomReference` 配置和项目默认豁免；夹具临时关闭豁免以验证实际门槛，
不将测试设置写入生产配置。

## 中部旋转专用检查

`scripts/electromagnetic-rotor-qa.init.gradle` 复用本夹具的超导电磁工厂场景，使用独立 build/run/classes。
`-PelectromagneticRotorBuildDirectory=<绝对路径>` 指定输出根目录；运行结果在其
`electromagnetic-rotor-qa/result.txt`，截图在 `screenshots/`。

专用场景检查四向成型、缺块失效、运行/停止/重启、233 个中部方块精确静态面隐藏、
原生线圈同步、运行任务 NBT 恢复、两张间隔动画画面、资源重载和客户端卸载钩子清理。
只在 QA 世界将已接受测试任务延长到 20000 tick 以观察动画，生产配方不变。
不运行旧夹具的 GUI 按钮测试；当前模式选择列表由独立现代 GUI 场景覆盖。

2026-10-23：上述专用场景通过，Gradle `BUILD SUCCESSFUL`、2m18s，日志
`.gradle/electromagnetic-rotation/client-modules.log`。输出根目录
`D:/CodexTools/GTNGElectromagneticRotorQA-20261023/build`；已查看远景、两张不同角度的旋转截图和资源重载后截图。
启动和重启沿用当前模块任务，完整任务 NBT（含待输出）保存恢复后继续推进。

截图使用客户端 `boundingBox.minY` 核对脚底高度，避免 1.7.10 客户端 `posY` 的 1.62 格偏移。
镜头到位、完整构件连续稳定 40 个客户端 tick 后，再预热 240 个渲染帧；拍世界图时隐藏 HUD 和手持物品。
每个加载或状态阶段最多等待 1200 个服务端 tick，失败报告包含构件数、活动构件数、镜头和帧数。

纯结构测试为 `MageAssemblerStructureTest` 与 `SuperconductingElectromagneticStructureTest`，共七项（含旋转选区）。
固定版本、原始几何 SHA-256、字符映射及 `-` 预注册要求见
[`reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md`](../../reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md)。

## 2026-10-21 完整实机验证

使用当前完整 `RecipeLoader` 运行两台机器的联合客户端检查，通过上述全部核验项目。
`build/assembly-plant-qa/result.txt` 以 `PASS` 开头，`client-gradle.log` 记录
`BUILD SUCCESSFUL`。已检查共 20 张两台机器的远景、活动材质、GUI 和中英文说明截图。
本轮包括实际 20 种矿辞审计、可获得的钨钢熔体输入、无线能源仓供能、洁净室临时配置恢复、
原生配方加工与存档恢复。
