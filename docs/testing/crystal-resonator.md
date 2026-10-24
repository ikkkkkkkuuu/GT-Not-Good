# 末晶谐振器放置回归

## 服务端崩溃

GTNH 2.9.0-RC-2、GTNG 1.6.9 独立服务器右键谐振器顶面放置水晶时，
`EndCrystalResonator.onRightclick` 调用 `EntityEnderCrystal(World,double,double,double)`，
抛出 `NoSuchMethodError`。用户原始报告为 `crash-2026-10-24_01.02.59-server.txt`，
诊断副本保存在 `.gradle/crystal-name/user-server-crash.txt`。

固定 Minecraft 1.7.10 源码中，该构造方法带 `@SideOnly(Side.CLIENT)`。
单人游戏的集成服务器仍运行在客户端进程中，所以此前客户端 QA 未覆盖这个问题。
修复使用双端存在的 `EntityEnderCrystal(World)`，再以 `setPosition` 设置相同坐标。
权限检查、空位检查、生成成功后扣量、原实体类型、存档和无线发电参数保持原样。

## 独立服务端检查

`scripts/qa/EndCrystalResonatorServerChecks.java` 首先确认 dedicated server，
并通过反射断言旧的客户端构造方法不存在，避免再次用集成服务器冒充验证。
随后在真实世界中逐一检查 LV–MAX 共 14 等级：

- 生存放置、准确坐标、只生成一个实体、扣除一个物品。
- 错误面、占位和重复放置被拒绝，不多扣物品。
- 放置后 20 tick 的无线电网增量准确，运行不消耗水晶。
- 创造模式放置成功且不耗物品。

运行命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 --init-script scripts/crystal-resonator-qa.init.gradle --project-cache-dir D:/CodexTools/GTNGCrystalName-20261024/project-cache -PcrystalResonatorQaRoot=D:/CodexTools/GTNGCrystalName-20261024 -PcrystalResonatorServerQA spotlessApply compileJava processResources spotlessCheck assemble runServer25 --offline --no-configuration-cache --console=plain
```

2026-10-24：退出 0，1m23s，`CRYSTAL_RESONATOR_SERVER_QA: PASS (212 checks)`。
日志 `.gradle/crystal-name/server.log`；隔离服务器保存并正常退出。
测试使用开发依赖，Et Futurum 为 2.6.59-GTNH；用户完整实例为 2.6.60-GTNH。
本次没有替换用户服务器 JAR、操作其世界或宣称完整整合包联机回归已完成。
首次新世界的白名单文件缺失与既有模组配方警告保留在日志中。

客户端复跑使用同一脚本，去掉 `-PcrystalResonatorServerQA`，将任务替换为 `runClient25`。
它使用独立目录、静音配置和 `HideFactoryClient.ps1`，覆盖既有 296 项检查及中文 tooltip 截图。
脚本已适配 Java 25 和降级后的测试夹具；客户端截图仍需逐次查看后才记为视觉通过。

2026-10-24：`client-fixed.log` 为 BUILD SUCCESSFUL、1m34s，296 项检查 PASS，
客户端请求退出 code 0，测试 Java 进程已退出。已查看 LV/MAX tooltip 截图，文字完整、名称正确。
外层 PowerShell 输出收集未自行返回，核实游戏已退出后仅结束本轮收集进程；其自然退出码未取得。
日志中的既有 AE2 CSV Export 退出异常保留。
最终编译/翻译/格式/打包两轮均退出 0（33s / 1s），第二轮复用 configuration cache，31 项 UP-TO-DATE。
普通 JAR 审计 `jar-audit.json` 确认已去掉客户端专用构造方法，保留 Java 8 基础字节码且没有 QA 类。

## 物品名称与获取

实际物品是 Et Futurum 的 `etfuturum:end_crystal`，原生中文名为“末地水晶”。
GTNG 的三处中文提示及外部物品注释已统一名称；翻译由 Java `#tr` 生成。
本机客户端和服务器的 `config/etfuturum/entities.cfg` 都是 `enableDragonRespawn=true`。

NHCore 2.9.84 的 `RecipeRemover` 移除原配方，`ScriptEFR` 注册新配方：
六个 AE2 聚能石英玻璃、一个 HEE 活跃物质、一个双足飞龙核心和一个量子之眼，
形状 `VGV / VSV / VQV`，V/G/S/Q 对应上述四种材料。
源码摘录在 `.gradle/crystal-name/ScriptEFR.java`。
LV 谐振器可制作并不意味着 LV 已能获得原生水晶，本次没有降低该配方门槛。
