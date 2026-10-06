# Issue #4：嬗变配方与矿物处理机控制器

问题来源：[GitHub issue #4](https://github.com/ikkkkkkkuuu/GT-Not-Good/issues/4)。
验证基线：GTNH 2.9.0-RC-1 开发依赖、GT5 `5.09.54.183`、Java 21、完整 GTNL `0.2.7-rc1` 开发 JAR。
GTNL JAR SHA-256：`c36ff329db26dd5df3d70e825bb2741c12fe1818c9d67d4a7b4323cd6ed6e8cc`。

## 原因与修复

- GTNL RC1 的 `ShimmerRecipes.loadRecipes()` 在关闭 `enableShimmerDisassemblyRecipes` 时直接返回，
  留下空转换表；原嬗变加载器仍视为导入成功，跳过本地生成。
- RC1 的 `LoadCompleteRecipeScheduler` 还会把完整配方注册延迟到首次客户端或服务器 END tick。
  在 Forge load-complete 阶段导入，微光开启时同样会读得过早。
- 装有 GTNL 时，嬗变配方现在于首次 END tick 的 LOWEST 优先级加载，等待 GTNL 的 NORMAL
  优先级注册结束。加载后注销监听并刷新 GregTech 的 NEI 缓存；空表使用本地生成，非空表保留
  GTNL 的实际转换和特殊覆盖。不修改 GTNL 配置或回填它的转换表。
- 任务引用的控制器是 `GTNGItemList.LargeOreProcessor`，机器 ID `28500`。它已注册，但没有合成配方。
  按原 GT-Not-Cool 组装机配方补回：铜板×4、铝板×4、任意 LV 电路×4、配置电路 24；32 EU/t，
  基础 200 ticks。项目的全局配方加速会修改实际时长，当前测试配置下为 1 tick。

## 复现与验证

先准备 `scripts/gtnl-compat-qa.init.gradle` 所用的固定 GTNL JAR、Et Futurum 开发 JAR，以及
`run/gtnl-compat-check/config/GTNotLeisure/GTNotLeisure.cfg`。此配置仅作为隔离测试的模板。

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp -Djava.io.tmpdir=C:/Windows/Temp'
$env:TEMP='C:/Windows/Temp'
$env:TMP='C:/Windows/Temp'
.\gradlew.bat -I scripts/issue-4-qa.init.gradle -Pissue4Mode=enabled runClient17 --no-configuration-cache
.\gradlew.bat -I scripts/issue-4-qa.init.gradle -Pissue4Mode=disabled runClient17 --no-configuration-cache
.\gradlew.bat -I scripts/issue-4-qa.init.gradle -Pissue4Mode=absent runClient17 --no-configuration-cache
```

三种模式使用各自的 `run/issue-4-<mode>` 目录，不使用玩家存档。Java 21 客户端由
`HideFactoryClient.ps1` 限定到当前工作区、带 QA 标记的进程，保持透明、点击穿透并隐藏任务栏按钮。
测试代码仅由 init 脚本加入测试类路径，不进入发布 JAR。

`scripts/qa/Issue4RecipeChecks.java` 等待两次 tick 后检查真实配方注册和原生匹配：

- 关闭微光后，GTNL 转换表仍为空、开关仍关闭，嬗变机拥有本地生成配方。
- 开启微光时，保留 GTNL 特殊转换测试项，避免误用本地组装机逆向结果。
- 未安装 GTNL 时正常生成，无 GTNL 硬依赖。
- 从组装机测试配方回收：每 2 个输入得到 3 根木棍和 250 mB 原生流体输出。
- 不足一批不匹配，3 个输入加工一批后保留 1 个；原生 GT 机器工作台配方也可逆向回收。
- 控制器配方可以通过 32 EU/t 的组装机查询找到，输入数量及配置电路正确。

报告必须以 `PASS` 开头，Gradle 才视为验证成功：`build/issue-4-qa/<mode>.txt`。
日志：`build/issue-4-<mode>.log`。

2026-10-06 验证结果：三种模式均 PASS；开启微光 4090 条、关闭微光 3976 条、无 GTNL 3135 条
嬗变配方（含隔离测试项）。`compileJava`、`processResources`、`checkstyleMain`、定向 Java 格式化
与 `git diff --check` 通过。

覆盖范围为开发客户端的注册、配方匹配和批量扣料；没有宣称完整整合包或实机多方块加工已验证。
