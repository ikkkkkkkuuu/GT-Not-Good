# 大型工业蜂箱

基线：GTNH 2.9.0-RC-1、GT5-Unofficial 5.09.54.183、Forestry 4.11.38。
控制器 ID：28527；物品枚举：`GTNGItemList.LargeIndustrialApiary`。

结构保留原版 15 × 17 × 15 布局，玻璃固定为 `bartworks:BW_TieredGlass:0`。
能源仓没有最低 LuV 限制，也不受玻璃等级限制。
普通模式容量为 `floor(总输入 EU/t / 32) × 8`，每只已存蜂后基础消耗 32 EU/t。
容量与实际供电分别校验；供电不足以支持全部蜂后时拒绝生产。
普通模式任一花位有已注册花源即可，不检查蜂后花源基因。
分蜂模式保留原版蜂王浆消耗、劣等公主蜂副本及普通超频。
控制器使用 MV 组装机配方，120 EU/t、30 秒。

## 自动验证

在 PowerShell 中运行：

```powershell
$env:JAVA_OPTS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat --offline --no-configuration-cache -I scripts/industrial-apiary-qa.init.gradle spotlessApply compileJava processResources runClient17
```

测试沿用工厂 QA 的运行依赖，需要缓存中的
`build/factory-qa-dependencies/etfuturum-2.6.59-GTNH-dev.jar`。
独立客户端工作目录为 `run/industrial-apiary-qa`，由
`scripts/qa/HideFactoryClient.ps1` 保持透明、点击穿透且不显示在任务栏。

2026-10-15 客户端验证通过，覆盖：

- 结构数组与原版完全一致，缺失外壳不能成型。
- LV 能源仓成型；普通硼玻璃支持 UV 能源仓；普通玻璃及其他硼玻璃变体被拒绝。
- 容量边界、整数饱和保护及 MV 控制器配方。
- 无花源拒绝生产；仙人掌花源基因的蜂后可使用普通红花。
- 每只蜂后 32 EU/t、5 秒周期；容量扩展不增加可用电力。
- 蜂王浆满额时产出三倍，计入整数产物和小数进度。
- 运行时模式切换保护，库存非空时不能切换普通/分蜂。
- 蜂后完整基因及小数产出进度经过 NBT 保存、加载后保留。
- 输出已满时保留蜂后，成功输出后再移除；重新输入不复制、不丢失基因。
- 分蜂保留原蜂后，输出劣等公主蜂，并保留超频行为。
- 中文控制器名称、状态面板、蜂后库存和配置面板在 MUI2 客户端中正常渲染。

结果保存在 `build/industrial-apiary-qa/result.txt`，截图位于同目录的 `screenshots/`。
移植来源和许可记录见 `reference/UPSTREAM_PORT_NOTES.md` 及
`src/main/resources/META-INF/industrial-apiary/`。
