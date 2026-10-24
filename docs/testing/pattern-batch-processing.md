# 样板输入总成跨配方调度

大熔炉和卫星工厂先遍历所有非空样板库存，在每个样板自己的物品、流体和共享工具中查找配方，
再统一分配原有线程/并行额度、计算功率与时间。各组输入使用同一份事务快照，共享槽按对象身份
复用副本；输出检查成功后才一次性提交。不能用不同样板的半份原料拼出一条配方。
无可加工样板时继续原有普通输入路径。

模块化机器按样板库存轮转分配空闲核心，每个任务仍是独立配方、独立进度；积压的第一张样板
不会占据所有空闲核心。主机内置一个核心，额外独立任务仍需要执行核心或已有无线任务容量。
原有电压、共享功率、并行、研究、算力、预付无线电及输出限制继续参与处理。
轮转位置只影响下一次查找顺序，不是存档任务或额外吞吐额度。
卫星保留原有批量工作上限，达到上限时下批从后续候选继续，防止持续补满首样板导致后续饥饿。

## 定向单测

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 `
  --init-script scripts/pattern-batch-check.init.gradle `
  '-PpatternBatchBuildDirectory=D:/CodexTools/GTNGPatternBatch-20261024/build' `
  --project-cache-dir D:/CodexTools/GTNGPatternBatch-20261024/project-cache `
  spotlessApply compileJava processResources spotlessCheck test `
  --tests '*ModularProcessingLogicTest' --tests '*LightHunterBatchProcessingTest' `
  --tests '*ModuleWorkTest' --tests '*DimensionalFocusProcessingTest' --offline --console=plain
```

`LightHunterBatchProcessingTest` 新增两组物品/流体的原子提交、输入变更时拒绝整批、跨样板原料隔离、
同一个共享槽只扣一次；其余测试覆盖无线额度、大数、流体重复需求及任务持久化。
另验证首张长配方每批补满，后续样板仍可在下一批得到处理。

## 隔离客户端

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 `
  --init-script scripts/pattern-batch-check.init.gradle `
  --init-script scripts/pattern-batch-qa.init.gradle `
  '-PpatternBatchBuildDirectory=D:/CodexTools/GTNGPatternBatch-20261024/qa-build' `
  --project-cache-dir D:/CodexTools/GTNGPatternBatch-20261024/project-cache `
  spotlessApply runClient25 --offline --no-configuration-cache --console=plain
```

夹具使用真实注册控制器与物理输入总成、原生 Ultimate Encoded Pattern、`MEInventoryCrafting`
和原生流体 NBT。矩阵为 GT 原版、超级、紧凑和电路总成，分别接入大熔炉、七种加工模块化机器、
四级卫星；另检查纯物品超级样板总线的大熔炉路径。

- 大熔炉使用真实铁矿/金矿烧炼配方，同批两种配方、128 并行；检查满输出不扣料、恢复后的精确
  产物和重复输出防护。
- 模块化机器使用各自实际配方池中的隔离 QA 配方，内置核心与附加普通核心运行不同样板。
  首张样板故意保留大量积压；核对两种任务、精确物品/原生流体耗用及控制器 NBT 重载。
- 四级卫星在各自生产配方池中接受两条隔离 QA 配方，核对同批物品/流体、满输出保护和 NBT
  重载后的精确产物；追加长配方触发批量工作上限，持续补料后仍处理另一张样板的两批回归。
- 本模组总成同时是 ME 输出目的地，因此“满输出”夹具既填满普通总线，也把总成独立物品
  产物缓存填到 `Long.MAX_VALUE`；仅堵普通输出仓不能代表所有输出已满。

这些是生产控制器的定向输入/任务/产物回归，结构标志与调度 tick 由夹具控制；不代替全部巨型
结构、实际 AE CPU 下单、长期吞吐或所有机器模式的全流程测试。核心供电、研究与算力的专门
边界继续由既有模块化 QA 和单测负责。

结果位于隔离构建的 `pattern-batch-qa/result.txt`，只有 `PASS` 且 Gradle 成功算通过。
客户端按 `HideFactoryClient.ps1` 作用域静音、透明、穿透并隐藏任务栏；不操作用户实例。

2026-10-24 最终定向单测 26 项通过，隔离客户端 49 组组合及全部卫星长批轮转检查通过；客户端日志
`.gradle/pattern-batch/client-final.log`，Gradle 退出 0、1m，结果文件为 `PASS`。
首次夹具编译 API 不匹配、挤压配方缺少最低物品数、未填满总成独立 ME 产物缓存的中间失败
均保留在同目录旧日志；最终夹具修正后完整重跑，不将那些中间失败记为生产代码缺陷。
最终构建 `final-rotation-build.log` 退出 0、30s；QA 后首次重复因配置输入变化重建，
紧接同任务 `final-warm.log` 退出 0、1s、35 项 UP-TO-DATE、configuration cache reused。
项目结构检查通过；普通 JAR 的九个相关基础类均为 Java 8、Multi-Release 且不含 QA 类。
