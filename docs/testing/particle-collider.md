# 超级粒子对撞机客户端检查

当前目标是用户指定增强核心 3.8.0 的 197×83×209 镂空巨构；不是旧 47×7×47 版本。
原始数据/哈希/许可/原生构件映射见 `reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md`。

## 构建与单测

```powershell
& scripts/dev/Invoke-Gradle.ps1 --init-script scripts/particle-collider-check.init.gradle '-PcolliderBuildDirectory=D:/CodexTools/GTNGColliderStructure-20261023/build' --project-cache-dir D:/CodexTools/GTNGColliderStructure-20261023/project-cache spotlessApply compileJava processResources spotlessCheck test --tests '*SuperParticleColliderStructureTest' assemble --offline --console=plain
```

三项纯结构测试逐格重建原始压缩数据的解压内容并核对 SHA-256，验证全部字符数量、
197×83×209 尺寸、控制器偏移 (93,41,0)、24 个正面仓室位、any 空格和数组隔离。

## 隔离客户端

```powershell
& scripts/dev/Invoke-Gradle.ps1 --init-script scripts/particle-collider-check.init.gradle --init-script scripts/particle-collider-qa.init.gradle '-PcolliderBuildDirectory=D:/CodexTools/GTNGColliderStructure-20261023/qa-build' --project-cache-dir D:/CodexTools/GTNGColliderStructure-20261023/project-cache spotlessApply runClient25 --offline --no-configuration-cache --console=plain
```

客户端采用 Java 25 和降级夹具。build、QA 输出和运行目录独立；
HideFactoryClient.ps1 只处理带有本工作区和 QA 标记的进程，静音、透明、穿透并隐藏任务栏。
DreamCore 退出确认关闭于本 QA 配置。首次 ProjectRed 运行依赖缺缓存时去掉 --offline 下载固定版本。
结果：`<build>/particle-collider-qa/result.txt`，只有 PASS 才算完整通过；截图在同目录 screenshots。

检查内容：

- 四个水平方向的完整搭建和成型，逐格核对全部 302624 个非控制器构件及原生 block/meta。
- 十一种固定构件缺失时拒绝成型，按原方块恢复后重新成型；中央 any 位置接受空气或石头。
- 24 个前置仓室位置的实际发现；原生流体/能源/维护仓和 ME 合成输入仓。
- 两种保留移植构件、控制器获取配方、旧移植超导线圈回收为原生线圈的精确输入输出。
- 十八条碰撞配方、原生元素终点、精确双流体消耗与产出、UV/UHV 电压/时长、并行和超频。
- 锔→锎在 UV 下拒绝且不耗料，升级 UHV 后使用保留输入成功加工；其他十七条保持 UV。
- Ultimate Encoded Pattern 原生双流体任务、整仓 NBT 恢复、低压/少料/满输出保护。
- 真实服务端加工、停止/恢复、破坏结构和修复、运行中 TileEntity NBT 恢复并继续推进。
- 客户端收到全部构件后拍摄远景、原生材质、GUI、中英文普通/Shift tooltip。

现结构使用 GTNH 原生超导线圈，不添加移植线圈活动元数据或发光租约。
客户端每 20 tick 检查一次全结构同步，这是 QA 夹具行为，未加入生产机器 tick。
实际通过记录及失败原因保存在 `docs/development/progress.md`；不能用旧版本 QA 代替本轮结果。

## 2026-10-23 验证记录

- `collider-structure-final-build.log`：编译、翻译生成、资源、定向格式检查、3 项结构单测和 assemble 通过，29 秒。
- `collider-structure-client-modular.log`：完整实机 PASS，2 分 16 秒；夹具按当前 ModuleWork 验证流体输出、耗电和时长，并使用真实并行模块仓。
- `collider-structure-client-visual.log`：完整实机 PASS，2 分 3 秒；关闭云层、提高 QA 视距并补拍远景，原生产逻辑未改变。
- 最终玻璃映射 `Casings.ChemicalGradeGlass`：`collider-structure-client-glass.log` 完整实机 PASS、退出 0、2 分 34 秒；最终截图与 result.txt 来自此轮。
- 最终 `collider-structure-glass-build.log`：同一构建命令通过，38 秒，3 项单测零失败；相同命令 `collider-structure-glass-repeat.log` 通过，1 秒、35 UP-TO-DATE、configuration cache reused。
- JAR 审计检查源结构 gzip 的 SHA-256、全部对撞机纹理与来源文件一致，排除 QA 类；报告 `.gradle/collider-structure-jar-audit.json`。

上述日志位于工作区 `.gradle/`；独立输出为 `D:/CodexTools/GTNGColliderStructure-20261023`。
曾失败的尝试没有删除：夹具整机构建覆盖仓室、过期加工字段、测试配置遗漏和并行原生贴图注册次序问题均分别修正。
首次缺固定 ProjectRed 传递依赖的离线运行未启动客户端；另一次 CoFH 登录异常由原样重跑通过，不归因于结构或声称已修复 CoFH。
日志保留第三方启动警告及客户端退出线程异常文本；以完整 PASS 与 Gradle 退出 0 共同判定，不将其描述成无警告。
只覆盖本机固定依赖、上述实际场景；未操作用户实例、没有大型多人服务器性能基准。

## 原生元素配方扩充（2026-10-23）

现有五条配方保持输入、输出、电压与时长不变；新增十三条，共十八条。
下表每一步是一条配方，消耗 4096 L 对应原生熔融金属和 4096 L 氦等离子体，
输出 4000 L 原生熔融金属，基础时长 200 tick（10 秒）。

| 新增路线 | 电压 | 已核对的后续用途 |
| --- | --- | --- |
| 钼 → 钌 → 钯 | UV | 钌用于 Botmium 合金；钯继续进入原生金属加工 |
| 钽 → 铼 → 铱 | UV | 铼用于 HS188-A、Laurenium 合金；铱进入原生高阶制造 |
| 钨 → 锇 → 铂 | UV | 锇用于 Botmium 合金；铂进入原生金属加工 |
| 镨 → 钷 → 铕 | UV | 钷用于铕/镅聚变，铕用于 ZPM 力场发生器等配方 |
| 钕 → 钐 → 钆 | UV | 钐用于路菲恩合金及磁性钐，钆用于 Cinobite A243 |
| 铥 → 镥 | UV | 镥用于原生镅聚变 |
| 镎 → 镅 | UV | 接续旧镤→镎，镅用于中子素聚变和高阶制造 |
| 锔 → 锎 | UHV | 接续旧钚→锔和锎→镄；锎另用于原生束流配方 |

这是 GTNG 新增的元素嬗变路线，不是复制 GTNH 官方对撞机配方。
固定证据为 GT5-Unofficial 5.09.54.205 sources JAR，SHA-1
`5447ea903c5833f0319123e38ac586f348e599f3`：

- `gtPlusPlus/core/material/MaterialsElements.java`、`MaterialsAlloy.java`：原生元素、流体和合金成分。
- `gregtech/loaders/postload/recipes/FusionReactorRecipes.java`：铕、镅、锇、铂、铱等原生获取及后续用途。
- `gtPlusPlus/xmod/gregtech/loaders/recipe/RecipeLoaderGTNH.java`：锔→锎原生聚变要求 750,000,000 EU 启动能量，属于 Mk4。
- `tectech/loader/recipe/ResearchStationAssemblyLine.java`：Mk4 控制器与线圈的研究/装配线要求 UHV，新增锎路线因此采用 UHV 电压门槛；不模拟原聚变的启动能量和研究流程。
- `gtnhlanth/common/tileentity/recipe/beamline/BeamlineRecipeLoader.java`：锎粉的束流用途。

所有新增转化均沿原子序数增加 2 的方向，配方表没有反向转化或重复原料分支，
每次金属流体由 4096 L 减为 4000 L；对撞机自身不能构成材料增殖环。
这不等同于穷举其他模组所有跨机器循环。

本轮隔离输出使用 `D:/CodexTools/GTNGColliderRecipes-20261023`，沿用上面的构建/客户端入口，
把 `colliderBuildDirectory` 和 `--project-cache-dir` 替换成此目录下的 `build`、`qa-build` 与 `project-cache`。
本轮构建任务为 `spotlessApply compileJava processResources spotlessCheck assemble --offline --console=plain`；
未修改结构数据，未重复纯结构单测。客户端仍运行完整结构和加工回归。

验证结果：

- `.gradle/collider-recipes/build.log`：退出 0，38 秒；重复任务 `build-repeat.log` 退出 0、4 秒，`build-warm.log` 退出 0、7 秒，29 UP-TO-DATE 且 configuration cache reused。
- `.gradle/collider-recipes/client.log`：退出 0，2 分 19 秒；`qa-build/particle-collider-qa/result.txt` 为 PASS，列出十八条原生流体名称和各自电压。逐条实际扣除输入并核对待输出产物、时长与耗电；确认 UV 拒绝锔→锎后可升级 UHV 继续使用原输入。
- 既有原生 ME 双流体、并行、超频、输入/输出保护、四方向结构及运行恢复检查均通过。
- 普通 JAR 包含配方类且不含 QA 类；结构检查及 `git diff --check` 通过。QA 进程已退出。
- 保留第三方启动警告和 `AE2 CSV Export` 退出异常；未把它们归因于本次配方或宣称已修复，未运行全部单测或独立服务端。
