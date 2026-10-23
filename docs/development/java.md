# Java 开发与兼容

本项目使用 JDK 25 编译，采用与 GTNL 相同的 JVM Downgrader 路线。运行游戏的 Java 版本和源码编译版本分开管理。

```properties
enableModernJavaSyntax = jvmDowngrader
downgradeTargetVersion = 8
jvmDowngraderMultiReleaseVersions = 17,21,25
jvmDowngraderStubsProvider = gtnhlib
```

- 可使用现代语法及 JVM Downgrader 支持的新版标准库 API，不再受 Jabel 的 Java 8 标准库限制。新增特性仍须通过降级及目标 JVM 测试。
- 游戏用 JAR 的基础类版本不高于 52（Java 8），新 JVM 自动选择适用的多版本类。降级器会移除除 class 版本号外完全相同的高版本副本，因此不保证每次都有 `META-INF/versions/25` 目录。
- `@Desugar` 的旧声明可保留，构建插件提供兼容注解；新 record 无需该注解。
- 保留 GTNHGradle 2.0.20、Gradle 9.3.1、GTNH 2.9.0-RC-2 及现有模组依赖。GTNHLib 0.11.52 提供运行时 stubs，Lombok 1.18.42 满足 Java 25 要求。
- Gradle daemon 使用最大 4 GiB 堆、512 MiB metaspace。首次实际降级构建在默认 512 MiB 堆下因 GC thrashing 失败，增大后通过；不修改系统 Java。

## 构建

Windows 使用包装脚本解决本机 JDK 25 的 Unix-domain socket 临时路径问题：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 compileJava processResources spotlessCheck test networkRegression assemble --offline --console=plain
```

首次缺少依赖缓存时去掉 `--offline`。发布使用重混淆后的普通 JAR；`-predowngrade`、`-dev-preshadow` 是中间产物，不能作为游戏用 JAR 发布。

`addon.gradle` 显式声明 `downgrade*Classes` 输出目录，修复 JVM Downgrader 1.3.5 在输入目录尚不存在时遗漏输出、导致第一次测试被误判 `NO-SOURCE` 的问题。`networkRegression` 与普通 `test` 均使用降级后的主类和测试类。

`ModernJavaCompatibilityTest` 检查 record 值语义，以及 `List.of`、`reversed`、首尾访问/删除、`String.formatted` 和 `strip` 的兼容行为。

## IntelliJ IDEA 同步

- 项目 SDK 保留 JDK 25。本机 IDEA 2025.3.1.1 目前采用项目语言级别 17：用户已确认从 25 改回 17 后红线消失，截图与 `.idea/misc.xml` 均确认 SDK zulu-25 / JDK_17。这是当前编辑器的兼容设置，不要求同步降低 Gradle 编译版本。
- 构建和测试委托给 Gradle，确保执行 Java 25 编译、降级与打包任务。语言级别 17 会限制编辑器接受的源码语法；以后确需 21/25 特有语法时再处理 IDE 兼容，不重新强制当前项目为 25。原版 Minecraft、启动器等辅助模块保留插件给定级别。
- 若所有多方块都提示未实现 `IVoidable.canDumpItemToME(List<ItemId>)`，先检查依赖与索引。固定 GT5 5.09.54.205 的 `MTEMultiBlockBase` 已实现该方法及流体对应方法，GTNG 基类会继承；已核对源码、基础 JAR 和各多版本视图的签名，无需逐台添加重复方法或将控制器改成抽象类。
- 本机已把项目默认级别改为 25 并重新加载 Gradle，用户确认仍标红；主模块 138 个库未发现重复 GT5 类。截图中 `GTUtility.ItemId` 自身也无法解析，因此不能再仅以语言级别或缓存解释。
- GT5 的 `GTUtility` 外部类最高位于 Java 21 目录，`ItemId` / `FluidId` 内部类最高位于 Java 17 目录。JVM 可以逐类选择版本；本机 IDEA 2025.3.1.1 的 `ClsFileImpl$1.findInnerClass` 字节码却只在外部类所在目录查找内部类文件，与 [JetBrains 源码](https://github.com/JetBrains/intellij-community/blob/master/java/java-psi-impl/src/com/intellij/psi/impl/compiled/ClsFileImpl.java#L625)一致。语言级别 17 避开该跨目录组合，与用户确认红线消失的结果相符；这是绕过编辑器兼容问题，并未修补 IDEA 本身。
- 本地 GTNL dev-290 使用同一个 GT5 5.09.54.205 缓存 JAR，同样继承该多方块基类并引用 `GTUtility.ItemId`。它的 GTNHGradle / Gradle 版本较新，但未发现专门处理上述内部类问题的逻辑；用户没有确认 GTNL 在 IDEA 中无红线，不能据此归因于本项目代码。
- 忽略目录 `.gradle/idea-libraries/` 中的扁平 GT5 副本仅用于诊断，未替换 Gradle 依赖、IDE 库或发布产物。用户通过语言级别 17 解决当前红线后，停止依赖导入改造，无新增构建补丁。

IDE 导入设置参考 [JetBrains Gradle 文档](https://www.jetbrains.com/help/idea/gradle.html)。发布 JAR 的 Java 8 基础兼容由 JVM Downgrader 保持，与 IDE 源码语言级别分别配置。

## 实机检查

量子合成的现有 QA 支持 `-PquantumJavaVersion=25`，夹具自身也经过降级；Java 25 不再设置已移除的 `java.security.manager=allow`。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 runServer25 --init-script scripts/quantum-server-qa.init.gradle -PquantumBuildDirectory=D:/CodexTools/GTNGJavaMigration-20261023/build -PquantumJavaVersion=25 --project-cache-dir D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache --offline --no-configuration-cache --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 runClient25 --init-script scripts/quantum-crafting-qa.init.gradle -PquantumBuildDirectory=D:/CodexTools/GTNGJavaMigration-20261023/build -PquantumJavaVersion=25 --project-cache-dir D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache --offline --no-configuration-cache --console=plain
```

两条命令串行运行，使用独立存档。客户端通过 `HideFactoryClient.ps1` 静音、透明、穿透并隐藏任务栏；不操作用户正常客户端。其他直接加载 `compileTestJava` 原始输出的旧 QA 脚本，运行在 Java 8/17/21 时也应改为加载 `downgradeTestClasses.outputCollection`。

## 迁移证据（2026-10-23）

- 构建及测试日志：`.gradle/java-migration/`；隔离产物和实机报告：`D:/CodexTools/GTNGJavaMigration-20261023/build/`。
- 完整单元测试执行 705 项，704 通过、1 跳过，0 失败；`tests-output-provider.log`。
- 最终 `spotlessApply compileJava processResources spotlessCheck test networkRegression assemble` 通过，`final.log` 退出 0、11 秒；完整测试同上，额外 `networkRegression` 的 12 项通过。唯一跳过项为既有 `StockIOLogicTest`。
- 相同任务重复执行 `final-repeat.log` 退出 0、933ms，36 项 UP-TO-DATE、configuration cache reused。格式和结构检查通过，所有本轮 QA Java 进程已退出。
- 上述现代 Java 回归的 2 项检查分别在 Java 8、17、21、25 上通过；`test-java8.log`、`test-java17.log`、`test-java21.log`、`test-java25.log`。各版本使用首次不存在的测试输出目录，验证首次构建不再跳过测试。
- JAR 审计确认原始主类 major=69，发布/开发 JAR 基础类不高于 52，包含 major=61/65 的多版本类及 `Multi-Release: true`，refmap 存在、没有 QA 类。Java 25 副本因等价内容被降级器去重；已核对 JVM Downgrader 1.3.5 的 `ClassDowngrader` 实现。
- Java 25.0.2 独立服务端（`server25.log`，47 秒）和客户端（`client25.log`，1 分 19 秒）均退出 0、报告 PASS。覆盖原生/量子订单、实际耗料与零耗能、NBT 恢复、循环视图、服务端 Mixin 与可选模组/端隔离；已查看 `quantum-crafting-5.png`。客户端退出时有既有第三方 AE2 CSV Export 线程异常文本，未影响断言及 Gradle 退出码。
- 完整游戏运行的 Java 8/17/21 验证不在本轮覆盖内；上述四版本检查是定向运行时回归，不代表整合包每个功能均已验证。
