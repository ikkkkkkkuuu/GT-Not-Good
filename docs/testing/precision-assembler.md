# 精密自动组装机

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat spotlessApply compileJava processResources
.\gradlew.bat -I scripts/precision-assembler-qa.init.gradle spotlessApply runClient17
```

夹具使用 `run/precision-assembler-qa`，关闭该目录中的全局配方提速，并保持测试客户端静音、透明、穿透和任务栏隐藏。

检查内容：

- 五种结构外壳、混搭拒绝、降级重检，以及 16／32／64／128／256 的实际常规并行产量。
- 固定使用 42 块普通硼玻璃，拒绝普通玻璃、其他玻璃及全部强化硼玻璃；主信道和旧 glass 子信道均不改变建造用玻璃。
- 精密与常规配方图、精密配方外壳等级限制、模式切换、配方锁定和方块保存重载。
- 内部 MV 机械方块拒绝原始 HV 配方；内部 HV 机械方块配合双 MV 能源仓运行 HV 配方。
- 内部 MV 机械方块搭配 EV 能源仓：常规 LV 配方为 1 tick、常规 MV 配方为 4 tick、精密 LV 配方为 2 tick，均为 1920 EU/t。
- MV 控制器配方的六种物品、576 mB 焊锡，以及原始 120 EU/t、600 tick 和单个产物。
- 五档控制器和舱室材质的客户端同步、GUI 切换的数据包往返，以及中英文普通／Shift tooltip 截图。

测试报告位于 `build/precision-assembler-qa/result.txt`，截图位于同目录的 `screenshots/`。
