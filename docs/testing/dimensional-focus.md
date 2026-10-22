# 维度聚焦激光蚀刻阵列

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat spotlessApply compileJava processResources
.\gradlew.bat -I scripts/dimensional-focus-qa.init.gradle spotlessApply runClient17
```

夹具使用独立的 `run/dimensional-focus-qa`，关闭全局配方提速；由 `HideFactoryClient.ps1` 保持静音、透明、穿透和任务栏隐藏。

检查内容：

- 原始 89×89×89 结构的四向建造、实际舱室识别、固定方块缺失、线圈混用与空格位置。
- 144 个超导线圈的运行／停止材质状态、控制器旋转与拆除后的状态清理及结构兼容性。
- 七个生存配方、两种光刻胶合成配方的注册与运行时材料完整性。
- 原生激光配方的材料、光刻胶和产量核对，以及透镜保留、原生电压门槛与满输出保护。
- 真实扫描仪消耗一份产物和空白数据盘，生成对应研究数据。
- 研究数据匹配与算力门槛；原生运行循环在算力中断时暂停进度和耗电，恢复后继续。
- 真实控制器保存重载，保留运行进度、功耗、算力需求与待输出物品。
- 原生 TESR 的四向中心坐标、完整核心包围盒与 256 格显示距离，原生网络流的状态／线圈颜色往返。
- 用真实运行配方核对客户端工作、空闲、结构失效、算力中断与恢复状态；在原始周期第 0／50 tick 截图。
- 六根十边形空心光束的原始几何与颜色脉冲，以及直接绘制后的 GL 程序、矩阵、FBO、纹理和混合状态恢复。
- 结构、材质、GUI 和中英文普通／Shift tooltip 截图。

报告位于 `build/dimensional-focus-qa/result.txt`，截图位于同目录的 `screenshots/`。只有 `PASS` 表示全部检查完成。

定向单元测试：`./gradlew.bat test --tests '*DimensionalFocus*'`，共 16 项，覆盖结构坐标、
原生配方与替代输入、NBT 研究标识、算力并行限制及保存恢复。新增的 3 项核心几何测试核对
360 顶点序列、六轴空心圆柱变换，以及原始透明度、白色脉冲和颜色量化。

2026-10-21：16 项定向测试和全部客户端检查通过。核心第 0／50 tick 的实际截图存在脉动差异；
空闲、算力中断和结构失效时关闭，恢复后重新显示。客户端世界总时间驱动动画，避免方块停止 tick 后冻结。

2026-10-21：使用最终完整源码和正常 `RecipeLoader`（包含巨型组装厂与超导电磁工厂配方）重测，
全部客户端检查 `PASS`，未隔离配方加载器。日志 `build/dimensional-focus-full-qa.log` 记录
`BUILD SUCCESSFUL`。已人工核对本轮核心第 0／50 tick 截图的
亮度差异、空闲与算力中断时熄灭，以及算力恢复后重新显示。

历史记录：此前正常启动曾被 `stickLongHSLA` 材料查找阻断，当时使用旧 `RecipeLoader` 完成隔离检查。
旧日志 `build/dimensional-focus-core-qa-isolated.log` 与 `build/dimensional-focus-core-qa-launch.log`
仅保留为该次阻塞和隔离验证的记录；本轮完整加载已通过。
