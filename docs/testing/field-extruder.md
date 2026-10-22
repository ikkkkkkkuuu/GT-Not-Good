# 力场压模工厂

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat spotlessApply compileJava processResources
.\gradlew.bat -I scripts/modular-machine-qa.init.gradle spotlessApply runClient17
```

模块化回归夹具使用独立的 `run/modular-machine-qa`，关闭全局配方提速。测试客户端通过现有 `HideFactoryClient.ps1` 保持静音、透明、穿透和任务栏隐藏。

检查内容：

- 原始 15×9×63 结构及四种水平方向建造，实际输入／输出总线、能源仓和可选维护仓识别。
- 六种固定结构块缺失时拒绝成型，修复后恢复；上游空格位置允许空气与任意实心方块。
- 压模与压缩两种原生配方图，以及原生配方的精确材料消耗和产量；非消耗输入遵守配方定义。
- 使用通用 `ModularProcessingLogic`；每个核心独立处理一种配方的并行任务，核对输入提交和产物。
- 并行模块作用于机器并行上限；默认有损超频及各类模块效果与其他模块化机器一致。
- 在同一配方图放入不同配方原料，确认单个核心只启动一种配方，附加核心可独立启动另一种配方。
- 切换配方图、原生电压门槛、满输出保护及失败不扣料。
- 真实控制器 TileEntity 保存重载，保留模式及各核心任务的运行时间、功率和待输出物品。
- 原生模式按钮的客户端／服务端同步，远景、局部材质、两种 GUI，以及中英文普通／Shift tooltip 截图。

报告位于 `build/modular-machine-qa/result.txt`，截图位于同目录的 `screenshots/`；仅 `PASS` 表示夹具完成全部检查。

四方向、原结构材质及模式按钮的专项检查使用 `scripts/field-extruder-qa.init.gradle`，
其结果保存在 `build/field-extruder-qa/`；联合夹具负责通用加工、模块及原生 tooltip 检查。
