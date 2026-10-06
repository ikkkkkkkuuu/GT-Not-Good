# 大型矿石处理的筛选产物

矿石配方在 load-complete 阶段按矿辞材料名补齐原生筛选机产物，覆盖原矿、粗磨矿、洗净矿以及下界/末地富矿。
原有粉末与宝石产量优先保留；缺失的筛选产物保留原数量、概率和独立产出槽位。富矿翻倍产出数量，概率不变。
材料匹配使用实际材料登记，排除 `AnyCarbon` 等矿辞分组别名，避免不同矿石互相继承筛选产物。
热离心矿、含杂粉和纯净粉不追加筛选产物。仅导入单个洗净矿输入、无流体与特殊槽要求的筛选配方。

方钍石保留 8 个方钍石粉，另有两项独立钍粉产出（6%、3%）及钍-232粉（1%）。
控制器使用 GregTech 的批量概率计算，超过整数上限的总产量拆成多个正数量 ItemStack。

## 验证

在 PowerShell 中执行：

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing'
.\gradlew.bat -I scripts/ore-sifting-qa.init.gradle spotlessApply compileJava compileTestJava --no-configuration-cache
.\gradlew.bat -I scripts/ore-sifting-qa.init.gradle runClient17 --no-configuration-cache
git diff --check
```

init 脚本复用 `build/factory-qa-dependencies/etfuturum-2.6.59-GTNH-dev.jar`，测试依赖与夹具不进入发布包。
运行时仅加载本轮测试类，避免旧 QA 类一起加载并创建额外测试存档。
客户端使用独立的 `run/ore-sifting-qa` 目录，并由 `HideFactoryClient.ps1` 保持透明、点击穿透且不显示任务栏图标。

结果位于 `build/ore-sifting-qa/result.txt`，首行必须为 `PASS`。检查包含：

- 实际 BartWorks 方钍石配方、原粉数量、两项钍粉概率及钍-232概率；原生筛选配方不被修改。
- 原矿、粗磨矿、洗净矿、原矿物品与富矿；重复产物槽位；下游中间产物排除；重复加载不增加产物或配方。
- 实际控制器对 20 万个方钍石的批量处理，零概率产物、确定产物及整数上限并行的总量。
- 所有产物能完整容纳于 NEI 的九槽输出网格。
- 排除 QA 夹具后，逐项核对全部原生矿石筛选产物，清单写入 `build/ore-sifting-qa/coverage.tsv`。

本机 2.9.0-RC-1 开发环境核对结果：67 种材料、580 条输入配方通过；石墨不继承煤炭的筛选产物。
