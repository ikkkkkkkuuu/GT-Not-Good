# 基础工业蜂箱

独立 LV 单方块机器，ID `28618`，物品枚举为 `GTNGItemList.BasicIndustrialApiary`。
沿用 GregTech 工业蜂箱贴图、蜂后/雄蜂输入和四个升级槽。
基础速度为原版 64 倍，普通产物和特产数量乘 16；固定 32 EU/t、1 A 输入。
速度升级在基础速度上继续提升，不增加电压或耗电。
忽略花源、昼夜、雨天、露天和温湿度工作限制；保留蜂后、雄蜂及原版育种要求。

GUI 提供 54 个输出槽，每槽仍遵循物品原有堆叠上限。
产物不足以放入输出槽时写入持久化缓存，每 tick 尝试补充输出槽并自动输出最多 54 栈。
自动输出遵守机器的物品输出开关和输出方向。
缓存达到 `1,048,576` 件后暂停开启新周期，正在进行的周期可以完整结束。

## 自动验证

在 PowerShell 中运行：

```powershell
$env:JAVA_OPTS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat --offline --no-configuration-cache -I scripts/basic-apiary-qa.init.gradle spotlessApply compileJava processResources runClient17
```

运行依赖沿用工厂 QA，需要缓存中的
`build/factory-qa-dependencies/etfuturum-2.6.59-GTNH-dev.jar`。
独立构建目录为 `.gradle/basic-apiary-qa-build`，客户端工作目录为
`build/basic-apiary-qa/client`。
脚本启用 `HideFactoryClient.ps1`，仅处理带本工作目录及 QA 标记的 Java 客户端窗口，
保持透明、点击穿透且不显示在任务栏。
`options.txt` 与客户端初始化均关闭主音量。

检查覆盖：

- 机器注册、LV 输入限制和唯一的 LV 组装机配方。
- 基础速度、速度升级及固定 LV 耗电。
- 午夜、雨天、封闭、无花环境中的真实蜂后生产和特产；核对 16 倍产量。特产品系从运行时有效表中选择。
- 公主蜂和雄蜂育种、缺少雄蜂时拒绝启动。
- 生产中断电后保留蜂后与待产物，保存加载后继续完成。
- 重复取消生产只退回一只原蜂后，不保留尚未完成的产物。
- 拆机物品保留已完成产物，生产中退回原蜂后，育种中退回原公主蜂和消耗的雄蜂；重放后不重复产出。
- 满输出槽下完成生产，超量产物及完整 NBT 保存恢复。
- 大批量缓存反复填充与排空不丢失数量，缓存限额阻止新周期。
- 自动输出每 tick 传输 54 栈至双箱，物品输出开关生效。
- 中文名称、54 个输出槽及原版蜂箱操作区的实际客户端渲染。

运行结果保存在 `build/basic-apiary-qa/result.txt`；客户端 GUI 截图保存至同目录的 `screenshots/`。

2026-10-20 客户端验证通过，以上场景全部完成。
本次特产场景自动选择 `forestry.speciesValiant`，普通生产场景使用 `forestry.speciesModest`。
中文界面截图确认 9 × 6 输出网格、蜜蜂输入、四格升级、原版控制按钮和玩家物品栏完整显示。

`ApiaryOutputBufferTest` 共 8 项测试通过，覆盖超过 `Integer.MAX_VALUE` 的数量、
基因 NBT、metadata 通配值隔离、堵塞恢复、标准堆叠和合并溢出守恒。
`compileJava`、`processResources`、`checkstyleMain` 和项目目录检查通过。
