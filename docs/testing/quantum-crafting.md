# 量子计算优化验证

## 来源与行为

移植 GTNL dev-290 的 `5837c5a8`、`2b79458b`（PR #646）及 `7b176111`，固定源码、哈希与许可见 `src/main/resources/META-INF/quantum-crafting-port/`。

网络存在量子计算机时，AE 合成数量界面显示量子模式按钮；默认关闭，选择保存在客户端并同步至服务器。量子计划仅交给量子 CPU；原有零耗能、批量执行、同 tick 独立 CPU 分配及机器规格保留。独立服务器计算超时默认 30 秒，缺料候选搜索上限默认 5 秒，配置见 `GTNOTGOOD/QuantumCrafting.cfg`。

## 自动检查

客户端夹具 `scripts/qa/QuantumCraftingClientChecks.java` 在真实 Minecraft / AE2 环境验证：

- 量子计算机和样板总成形成、576 个样板位、原生 crafting provider 可见。
- 原生订单与量子订单精确耗料、无 EU/AE 扣费、同 tick 双订单使用独立 CPU。
- 断网恢复、矩阵存档重载和订单完成产出。
- 量子模式按钮显示、关闭/开启、服务端状态及网络失去量子 CPU 后隐藏。
- 量子计划进入原生样板优化容器，次数正确；合成树序列化及确认状态同步。
- 缺料预览不消耗库存；有种子的净产出循环能够生成计划。
- 真实合成树打开循环视图并截图；名称提示、数量界面及确认界面截图。

独立服务端夹具 `scripts/qa/QuantumCraftingServerChecks.java` 验证 common 类实际加载和 Mixin 应用、超时启用、材料变体配额 NBT/精确扣减/清空，以及 AE2 缺席和服务端的 Mixin 注册条件。

边界：循环用合成的铁锭自增样板检验，环视图由夹具通过生产 API 注入已计算的循环索引；不声称穷尽真实整合包循环、自动查询网络包、原生流体执行或与最新完整 GTNL 同装。没有速度基准数据。

## 重现

在仓库根目录顺序运行。以下是本机使用的隔离输出与已缓存 project-cache，可按机器调整路径；首次缺少依赖缓存时移除 `--offline`。

```powershell
$quantumBuild = 'D:/CodexTools/GTNGQuantumPort-20261023/build'
$quantumCache = 'D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 --init-script scripts/quantum-check.init.gradle "-PquantumBuildDirectory=$quantumBuild" --project-cache-dir $quantumCache spotlessApply compileJava processResources spotlessCheck --offline --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 --init-script scripts/quantum-crafting-qa.init.gradle --init-script scripts/quantum-check.init.gradle "-PquantumBuildDirectory=$quantumBuild" --project-cache-dir $quantumCache spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 --init-script scripts/quantum-server-qa.init.gradle --init-script scripts/quantum-check.init.gradle "-PquantumBuildDirectory=$quantumBuild" --project-cache-dir $quantumCache compileJava processResources runServer17 --offline --no-configuration-cache --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/qa/CheckProjectStructure.ps1
git diff --check
```

两个运行任务使用 Java 21，独立 run/classes 目录；客户端静音且由 `HideFactoryClient.ps1` 限定作用域隐藏。服务端绑定 `127.0.0.1:25579`。不操作用户实例。

## 2026-10-23 结果

- `D:/CodexTools/GTNGQuantumPort-20261023/client-tree.log`：退出 0，`BUILD SUCCESSFUL in 1m 58s`，`QUANTUM_CRAFTING_QA: PASS`；结果位于 `build/quantum-crafting-qa/run/quantum-crafting-qa-result.txt`。
- 同目录 `server.log`：退出 0，`BUILD SUCCESSFUL in 1m 17s`，`QUANTUM_SERVER_QA: PASS`；结果位于 `build/quantum-server-qa/run/quantum-server-result.txt`。
- 已检查 `build/quantum-crafting-qa/run/screenshots/quantum-crafting-2.png` 至 `quantum-crafting-5.png`：名称提示、模式按钮、确认列表和循环回边可见，没有遮挡。
- 客户端退出阶段仍出现第三方关闭线程异常文本；测试断言、PASS 报告和 Gradle 退出码成功，未隐去日志。

上面的 `build/` 证据路径均相对于 `D:/CodexTools/GTNGQuantumPort-20261023/`。首次移植编译失败及夹具修正过程保存在同目录日志，最终状态见 `docs/development/progress.md`。
