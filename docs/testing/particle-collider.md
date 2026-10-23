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
- 五条碰撞配方、原生元素终点、精确双流体消耗与产出、UV 电压/时长、并行和超频。
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
