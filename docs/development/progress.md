# 当前开发进度

## 1.6.8 提交与发布（2026-10-23，进行中）

- 授权与基线：用户“提交并更新”授权完整发布；上次公开版本 1.6.7，当前 main / origin/main 为 75f45720。本次汇总原生结构件统一、增强版对撞机、十级并行仓材质、16 套透明面板资源及 IDEA 文档改动；对应开发任务均已完成。
- 准备：README 当前版本和下载链接更新为 1.6.8，补齐全部有效改动的中文说明；保留旧版历史说明。为新来源目录补充原始字节保护，依赖与构建工具版本不变。
- 发布前验证：隔离输出 D:/CodexTools/GTNGRelease-1.6.8，完整 spotlessApply build networkRegression 退出 0、32s；705 项单测无失败/错误、1 项既有跳过，12 项网络回归通过。格式化后相同任务再执行退出 0、6s；日志 .gradle/release-1.6.8/build.log / build-repeat.log。
- 资源与静态检查：audit-index.json 核对 8138 项资源/来源文件与暂存内容、普通/dev JAR，源清单哈希、ZIP、Multi-Release、基础/多版本字节码、refmap 和无 QA 类均通过。沿用文本文件换行规范化，原始移植资产保持字节；为新增对撞机来源文件补齐 cr-at-eol 属性。结构检查通过（1586 Java / 3413 imports / 161 mixins / 179 reserved / 384 QA paths），暂存 diff --check 通过。
- 剩余：提交并打标签，验证精确标签构建后推送；等待 GitHub CI 与 Release，核对中文说明、三份发布资产和 README 链接，更新最终记录。尚未宣称提交或发布完成。

## 新增多方块原生结构件统一（2026-10-23，已完成）

- 目标：全面核对移植多方块结构件，已有 GTNH 对应物时复用原生方块；没有对应物时保留自定义方块。基线 HEAD 75f45720，保留并行对撞机、并行仓和资源修改，不提交或发布。
- 证据：固定 GT5 5.09.54.205 sources JAR（SHA-1 5447ea903c5833f0319123e38ac586f348e599f3）的 Casings、ItemList、方块注册和语言资源。Serena Java Broken pipe，按仓库规则回退 rg/源码。
- 范围与结果：核对八组 88 个移植结构件变体，以及 Stellar 54 种结构外观、28 种装饰面板；38 个重复变体改用 33 种原生 block/meta，其余没有对应物的专用构件保留。完整映射、保留理由和复现命令见 docs/development/native-structure-casings.md；未来新增结构件优先原生的要求已写入长期规则。
- 实现：结构提示/自动建造、物品别名、仓室材质及配方使用原生件。已发布旧注册 ID/meta 保留，旧结构和 NBT 仍接受；旧复制件从创造栏隐藏，移除生产配方，加入组装机 25 号电路 1:1 单向转换。实机发现自动逆向嬗变配方会重新生成旧件，已排除旧件回收输出并复测；没有新增结构件注册、修改依赖版本或迁移用户世界。
- 生命周期修正：原生贴图解析必须延后到完整注册之后，保留并行对撞机任务加入的 DeferredTexture；旧方块自身仍注册旧贴图，不在贴图注册阶段读取尚未初始化的原生 ItemList。
- 构建：.gradle/native-casing-audit/final-build.log 退出 0、26s，spotlessApply build 的编译、语言生成、Spotless、Checkstyle、完整单测和打包通过。705 项测试，0 失败/错误、1 项既有跳过；同命令 final-repeat.log 退出 0、1s，37 UP-TO-DATE，configuration cache reused。普通 JAR ZIP 完整，包含新生产类且没有 QA 夹具。
- 实机：client-final.log 退出 0、BUILD SUCCESSFUL、1m15s；独立 Java 25 客户端报告 PASS，1190 项检查、38 项映射，覆盖注册身份、旧 NBT、创造栏、全配方输出扫描、转换配方、原生/旧件接受、错误方块拒绝、建造优先项、六面贴图及资源重载；猎光号/卫星工厂/宇宙之心全部符号映射和完整场驱动压模工厂四方向建造/成型/仓室/缺块/修复通过。已查看最终 native-casings-2.png，全部 38 个图标和名称正常显示。QA 位于 D:/CodexTools/GTNGNativeCasings-20261023/qa-build/native-casing-qa/，静音隐藏客户端已退出；日志保留退出阶段 Client Shutdown Thread 异常文本，不把退出线程无异常列为验证结论。
- 静态检查：CheckProjectStructure.ps1 退出 0（1586 Java / 3413 imports / 161 mixins / 179 reserved types / 384 QA paths）；git -c core.whitespace=cr-at-eol diff --check 通过，遵循移植资源原始换行保护。失败阶段的盘符参数、导入缺失、过早贴图解析、自定义主菜单识别和逆向回收问题日志均保留，最终重跑通过。
- 状态与边界：本轮统一工作完成，无剩余必做项；未提交或发布，保留全部并行改动。未重跑每台巨型机器的完整加工/能源流程或独立服务端，不将历史验证冒认为本轮覆盖。

## 增强版超级粒子对撞机结构（2026-10-23，已完成）

- 目标与来源：按用户截图和指定的 GregTech Leisure-增强版 实例修正结构；基线 HEAD 75f45720，未提交或发布，保留所有并行修改。旧版来自 GTLCore 47×7×47；截图实际为 GTL-Enhancedcore 3.8.0 的 197×83×209 镂空巨构，构件数量逐项匹配。
- 固定证据：增强核心 JAR SHA-256 555a9514c4d2fd11a029c091ac053d5cf7c52fba62411f2f043e04454094f7aa，包内声明 LGPL-3.0；未确认源码提交。hollow/gtl/super_particle_collider.pattern.gz 原字节复制，压缩 SHA-256 129ead53fed01d9407e8d7a2c355005b2ee3e245610120878180a0e3b0a85eda，来源/许可/坐标详见 reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md 和 ENHANCED_STRUCTURE.json。
- 实现：缓存压缩结构，校验尺寸、字符、控制器和总块数；只逆转深度、@ 转 ~，保留全部 any 空格。共 302625 个非空位置（含控制器），偏移 (93,41,0)，控制器离底层 41 格、24 个正面仓室位。既有小结构需重建，控制器 ID 与生产处理保持原有规则。
- 构件：九类复用 GTNH 原生注册，路菲恩外壳和加速管道复用已有移植，无新增重复方块或贴图。玻璃最终沿用项目既有夹层玻璃对应关系 ChemicalGradeGlass；硅岩合金外壳采用原生 RadiantNaquadahAlloyCasing，对应物及外观差异明确记入来源表。已发布旧移植线圈保留注册/资源，通过电路 6 的 1:1 装配机配方转为原生线圈。
- 集成修复：并行原生结构件改动在贴图注册阶段提前解析尚未注册的 FusionMachineCasingMKIV，导致 QA 启动失败；本任务仅将 NativeStructureCasings.texture 改为首次绘制时延迟解析，保留其余映射与并行工作。Serena Java Broken pipe，依仓库规则回退 rg 和固定源码。
- 最终实机：.gradle/collider-structure-client-glass.log 退出 0、2m34s，result.txt 为 PASS。完整四方向搭建/成型、逐格原生 block/meta、十一类缺块拒绝与修复、中心 any、实际仓室、原生 ME 双流体订单、五条元素配方耗料/产出、真实模块仓并行、电压超频、缺料/低压/满输出保护均通过。客户端收到全部 302624 个非控制器构件；真实加工启停、损坏修复、运行中 NBT 恢复和继续推进通过。已查看最终远景、原生材质、GUI、中英文普通/Shift 提示，云层关闭仅作用于 QA。
- 最终构建：同一隔离 build/cache，spotlessApply compileJava processResources spotlessCheck test --tests '*SuperParticleColliderStructureTest' assemble --offline；collider-structure-glass-build.log 退出 0、38s，3 项单测零失败/错误。相同命令 collider-structure-glass-repeat.log 退出 0、1s、35 UP-TO-DATE、configuration cache reused。命令及范围见 docs/testing/particle-collider.md。
- 打包与检查：普通游戏/dev JAR 中结构 gzip 与原始源哈希完全一致，165 份对撞机纹理/元数据逐字节一致，14 份原始资产哈希、ZIP、Multi-Release、无 QA 类检查通过，报告 .gradle/collider-structure-jar-audit.json。结构检查通过（1586 Java / 3413 imports / 161 mixins / 179 reserved types / 384 QA paths），git diff --check 通过。
- 尝试记录：保留 PowerShell 未引用 -P 路径、固定 ProjectRed 依赖首次离线缺缓存、旧 QA 整机构建覆盖仓室、测试 cutcorners 配置遗漏、旧加工缓存断言等失败日志；分别修正参数、补齐固定依赖、逐块恢复、还原 QA 配置、使用 ModuleWork 和真实模块仓。曾一次 CoFH 登录异常，原样重跑通过，未声称修复 CoFH；modular/visual 两次中间版本也完整 PASS，最终以 glass 日志为准。第三方启动警告和既有退出线程异常文本保留。
- 状态：本次必做项全部完成，独立证据目录 D:/CodexTools/GTNGColliderStructure-20261023；QA Java 进程已退出，未操作用户正常客户端、修改参考实例、升级生产依赖或发布。未进行大型多人服务器性能基准，不将其列为已验证。

## 1.6.7 发布与多方块 IDE 诊断（2026-10-23，已完成）

- 目标与结果：用户“提交并更新”授权已执行；基线 1.6.6 / c70251eb，发布提交 3b491cfcdb5101f7c528c4e1c9604f5b249f7b34，main 与标签 1.6.7 已原子推送。Release https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/tag/1.6.7 已公开，标题与中文正文覆盖全部九类有效变化，README 下载链接已更新。
- 集成验证：发布前 release-build.log 退出 0、27s；标签状态下 release-tag-build.log 再次执行 spotlessApply build networkRegression 退出 0、33s。705 项单元测试无失败（1 项既有跳过）、12 项网络回归通过；Checkstyle、Spotless、打包及结构检查 1583 Java / 3381 imports / 161 mixins / 179 reserved / 364 QA paths 通过。GitHub Build/Test 39950405532 与 Release 39950406159 均成功。
- 资产与链接：普通、dev、sources 三份 1.6.7 JAR 已发布；删除本次 CI 误上传的 predowngrade / dev-preshadow 中间产物。Release 页面、README 普通 JAR 和 sources 下载链接均 HTTP 200，长度分别为 31,943,149 与 24,678,762 字节。GitHub 返回普通 JAR SHA-256 f18b3d06b56b101a83489fdcb62d05b269067804da0e77f7ec36727bacb9e9f9，元数据保存在 .gradle/java-migration/final-release.json。
- 本地产物审计：1,556 项移植资源在工作区、Git 标签与游戏/dev JAR 中逐项哈希一致；ZIP、版本元数据、多版本字节码、refmap、来源清单和 sources 内容通过。未包含标签之后新增的资源。报告 .gradle/java-migration/release-audit.json；不把不同平台本地构建的整体哈希冒认为 CI 资产哈希。
- Git 字节保护：补齐新移植目录的 -text 与动画元数据 cr-at-eol，避免 CRLF 规范化改变来源清单哈希；原始 blob 核对及暂存空白检查通过。*.hprof 已忽略，原堆转储保留；缓存、日志、凭据未提交。
- IDE 已确认事实：用户改为项目级别 25、重新导入后仍标红，主模块 138 个库没有重复 GT5。固定 GT5 的源码及 javap 基础/17/21/25 视图均确认 MTEMultiBlockBase 具体实现两个 canDump 方法，QuantumComputer 正常继承；未补重复方法或改为抽象类。
- IDE 主要线索：截图中 GTUtility.ItemId 也标红；GT5 的外部类最高在 Java 21 目录，ItemId / FluidId 在 Java 17 目录。本机 IDEA 2025.3.1.1 的 ClsFileImpl$1.findInnerClass 仅查询同目录内部类，与 JetBrains 当前源码一致，指向多版本 JAR 嵌套类解析问题。详情见 docs/development/java.md；生成的扁平 Java 25 诊断副本仅在忽略目录，未应用到 IDE 或实际依赖。
- GTNL 对照：本地 dev-290 使用相同 GT5 5.09.54.205 缓存 JAR、相同 Java 25 / JVM Downgrader 路线及同类继承和 ItemId 用法。GTNL 的插件/Gradle 较新，但未发现针对该解析问题的处理；用户回答不知道 GTNL 编辑器是否无红线，不能把它当作已确认的差异。
- 下载复核：实际下载 GitHub 的普通游戏 JAR，SHA-256 与上述 GitHub digest 完全一致；文件位于 .gradle/java-migration/http-assets。此前 gh 并行下载停滞，仅结束本任务核实过的下载进程后改用 HTTP，不影响其他进程。
- IDE 最终结果：用户明确确认“降到17就行了”，截图和磁盘均为 SDK zulu-25、项目语言级别 17；红线消失。保留该设置及 Gradle 的 Java 25 / JVM Downgrader 配置，不再改造依赖导入、补机器方法或升级依赖。语言级别 17 绕开 GT5 外部类与内部类跨多版本目录的解析问题，未宣称修复 IDEA 本身；已纠正 Java 文档中统一设为 25 的建议。
- 使用边界补充：用户询问能否继续用 Java 25 新语法/API；核对 JetBrains 语言级别与 SDK 文档、现有 Gradle 开关。实际语言级别 17 的模块不会正常接受更新语法，虽 Gradle 仍按 25 编译；SDK 25 提供新版 API，但目标旧 JVM 的可用性还取决于 JVM Downgrader 支持和测试。17 是 IDE 临时兼容方案，不代表完整 Java 25 编辑体验；本次仅说明边界，没有新增代码、测试或发布。
- 状态：发布与本轮 IDE 排查完成，无剩余必做项。保留其他聊天在发布后新增的机器正面资源和并行控制仓工作，不并入已发布标签。收尾仅更新排查文档，未引入构建或游戏代码变化。

## 全项目静态审查与 GTNL 目录对照（2026-10-23，已完成）

- 目标：按用户要求审查项目优化点，重点对照本地 GT-Not-Leisure-dev-290 的文件位置、目录格式和命名。开始及结束均核对 Git；HEAD c70251eb，保留全部既有和并行修改，不执行代码重构或发布。
- 范围：枚举 1439 个主源码 Java、144 个测试 Java 和全部资源；对照 GTNL 的 1359 个主源码文件。读取注册、网络、机器处理、GUI、渲染、构建和 QA 入口，并扫描翻译、命名及资源内容。GTNL 本地目录无 .git，以文件哈希固定对照，未冒认提交号。
- 结果：报告 docs/development/project-review-2026-10-23.md 列出 12 项工程问题/维护建议。优先项为结构开发脚本进入 JAR、QA 硬编码本机路径、结构检查未接常规构建、738.5 MiB 未忽略堆转储；另列业务类归属、GUI 位置、配方/消息命名和大纹理测量候选，区分已确认事实与待实测收益。
- 验证：CheckProjectStructure.ps1 退出 0（1583 Java / 3381 imports / 161 mixins / 179 reserved types / 364 QA paths），git diff --check 退出 0；报告 UTF-8、空白检查通过。现有 1.6.6 JAR 确认含 3 个 Python 工具和 input.txt；原始扫描、JAR 哈希与日志在 .gradle/project-review-20261023/。
- 工具边界：Serena 激活成功，Java 概览请求因 Broken pipe 失败，回退 rg/源码；未把文本匹配当完整调用图。审查期间 Java 25 迁移在另一任务完成，本轮没有重跑其测试，也未进行全量游戏场景或性能测试。
- 状态：审查和建议整理完成，无剩余审查交付项；建议尚未实施。本轮只新增报告及更新此记录，没有移动/删除代码或资源、清理堆转储、提交或发布。

## Java 25 / JVM Downgrader 迁移（2026-10-23，已完成）

- 目标：按用户授权对齐 GTNL 的 jvmDowngrader、Java 8 基础字节码、17/21/25 多版本 JAR、GTNHLib stubs 路线。基线 HEAD c70251eb，保留所有既有修改，不提交或发布。
- 已核对：GTNHGradle 2.0.20 自带降级、重混淆、测试和打包接线；实际 GTNHLib 0.11.52、Lombok 1.18.42 满足要求。Serena 已激活，无需 Java 语义查询。插件、wrapper、整合包及模组依赖版本未升级。
- 实现：四项开关已启用，Gradle 堆上限增至 4 GiB；addon.gradle 修复降级器首次构建遗漏输出导致测试 NO-SOURCE，并让 networkRegression 使用降级后的类。量子客户端/服务端 QA 支持选择 Java 25、降级夹具并避免已移除的 SecurityManager 开关。新增 2 项现代 API/record 回归，README、长期规则、工具说明及 docs/development/java.md 已更新。
- 构建：隔离输出 D:/CodexTools/GTNGJavaMigration-20261023/build；缓存 D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache。final.log 的 spotlessApply compileJava processResources spotlessCheck test networkRegression assemble --offline 退出 0、11 秒；相同任务 final-repeat.log 退出 0、933ms、36 UP-TO-DATE、configuration cache reused。完整命令与范围见 Java 文档。
- 测试：完整单元测试 705 项，704 通过、既有 StockIOLogicTest 1 项跳过，0 失败；networkRegression 12 项通过。现代 API/record 两项分别在 Java 8/17/21/25 实际通过，每个版本使用首次不存在的测试目录，验证首次构建不再跳过测试。报告在独立 build/test-results/；汇总 .gradle/java-migration/test-summary.json。
- 实机：server25.log 退出 0、47 秒、QUANTUM_SERVER_QA PASS；client25.log 退出 0、1 分 19 秒、QUANTUM_CRAFTING_QA PASS。运行日志确认 Java 25.0.2，覆盖真实加载、两端 Mixin、订单耗料/零耗能、NBT 恢复及循环 GUI；已查看 quantum-crafting-5.png。客户端静音隐藏，相关进程已退出，未操作用户实例。保留第三方 AE2 CSV Export 退出线程异常文本。
- 打包：审计普通游戏 JAR 与 dev JAR，原始主类 major=69，基础字节码不高于 52，17/21 多版本类 major=61/65，Multi-Release/refmap/ZIP 完整且无 QA 类。25 目录因与更低版本的类内容相同被去重，已核对降级器实现，不强行添加重复内容；报告 .gradle/java-migration/jar-audit.json。
- 失败与修正：保留 build.log 的 PowerShell -I 参数歧义、build-retry.log 的默认 512 MiB 堆 GC thrashing、tests.log 的 NO-SOURCE、tests-fixed.log 的过早读取 inputCollection 及 inspect.log 的配置解析锁失败。改用 --init-script、4 GiB 堆、延迟输出声明后最终验证通过；最初审计错误要求存在 25 目录的假设已更正。
- 最终结构检查通过（1583 Java / 3381 imports / 161 mixins / 179 reserved types / 364 QA paths），git diff --check 通过。无剩余必做迁移项；未提交或发布。Java 8/17/21 本轮仅做定向运行时回归；旧 QA 若直接装载原始测试类，需按 Java 文档改用降级输出，不将全部旧夹具或整合包全功能写成已验证。保留全部并行修改。


## GTNL Java 版本核对（2026-10-23，已完成）

- 目标：回答 GTNL 实际采用的 Java 版本。Git 状态已核对，保留所有既有修改。
- 证据：ABKQPO/GT-Not-Leisure 默认分支 HEAD 906849466f13a50ae1ec582068b25d9aee75ab97；README 声明开发/构建需要 JDK 25。固定提交 gradle.properties 为 enableModernJavaSyntax=jvmDowngrader、downgradeTargetVersion=8、jvmDowngraderMultiReleaseVersions=17,21,25、jvmDowngraderStubsProvider=gtnhlib。
- 结论：GTNL 采用 Java 25 开发配合 JVM Downgrader，构建配置保留 Java 8 基础字节码和 17/21/25 多版本类；不能将其描述成仅支持 Java 25。对齐 GTNL 应评估 jvmDowngrader 路线，区别于本项目现有 jabel 和上一轮提出的原生 modern 选项。
- 状态：只核对源码配置，未下载/运行 GTNL 发布 JAR，未宣称所有 JVM 的实机兼容性已测试；未修改本项目构建、依赖或代码，无剩余查询项。


## Java 25 与编译兼容性核对（2026-10-23，咨询已完成）

- 目标：核实项目是否必须保留 Java 8 限制。Git 状态已检查；gradle.properties 既有 diff 仅新增构建缓存，当前仍为 enableModernJavaSyntax=jabel。
- 证据：GTNH 官方 FAQ 推荐 Java 17–25；官方 ExampleMod 配置说明区分 jabel、jvmDowngrader、modern。读取本机缓存 GTNHGradle 2.0.20 sources JAR，确认三种模式及 runClient25/runServer25 注册存在。Serena session/项目激活成功，本轮无需 Java 语义查询。
- 结论：Java 8 字节码是当前兼容策略，不是 Minecraft 1.7.10 或本项目不可改变的要求；游戏运行版本、源码语法/API、产物最低 JVM 应分别判断。不再将 Java 8 兼容描述为用户明确要求。若选择原生 Java 25，需明确提高客户端/服务端最低运行版本并验证加载链；JVM Downgrader 是保留旧 JVM 支持的另一条路线。
- 状态：本轮只核对和说明，未迁移构建配置、升级依赖、运行编译或客户端；未宣称 Java 25 原生构建已经通过。实际迁移与两端验证尚未开展，保留所有既有修改。


## 大型熔炉复用 GTNH 原生结构砖（2026-10-23，已完成）

- 用户纠正：焦炉砖和耐火砖应使用 GTNH 现有方块，不重复新增。核对固定 GT5 5.09.54.205：CokeOvenCasing=sBlockCasings12:0，Casing_Firebricks=sBlockCasings4:15，与原生焦炉/土高炉一致。
- 实现：D/E 结构位置改用上述原生方块，仍为 64/102 块；tooltip 从原生 ItemList 取名称。删除本轮未发布的两种重复方块类、注册、GTNGItemList 项、获取配方与两张贴图，来源清单和测试文档同步更新。
- 实机：.gradle/large-furnace-native-bricks-client.log 退出 0、BUILD SUCCESSFUL、1m42s，完整报告 PASS。166 个砖块位置逐格校验 block/meta、无重复注册、成型、加工、Waila、并行仓和资源重载回归均通过；已查看原生砖块结构截图。证据在 D:/CodexTools/GTNGLargeFurnace/build/large-furnace-qa/。
- 打包：QA JAR 中无重复砖块类、两张废弃贴图或对应翻译；剩余六张 PNG 和四份动画元数据逐项哈希匹配。最终结构检查通过（1581 Java / 3378 imports / 363 QA paths），git diff --check 通过。
- 构建：同一 init/独立 build/cache，compileJava processResources spotlessCheck --offline；native-bricks-build.log 退出 0、2s；重复 native-bricks-repeat.log 退出 0、20 UP-TO-DATE、configuration cache reused。
- 状态：无剩余项，本任务 QA 客户端全部退出；未提交、发布或操作用户正常客户端，保留其他聊天修改。

## 并行控制仓缺失贴图修复（2026-10-23，已完成）

- 用户截图显示 IV–MAX 并行仓正面紫黑缺失贴图。原因：移植的四张 PNG 是 16×64 四帧动画，遗漏了原始 `.png.mcmeta`。前次功能/GUI 验证未检查仓室正面，覆盖不足，保留原记录。
- 实现：从已固定哈希的 GTCEu 1.4.4 JAR 原字节复制四份元数据，每帧 8 tick；PNG 不变。来源、目的、SHA-256 与许可证清单已更新，QA JAR 内八张 PNG 和四份动画文件哈希匹配。
- 实机：.gradle/large-furnace-textures-client.log 退出 0、BUILD SUCCESSFUL、1m27s；既有加工/Waila/GUI 回归与新增十级实际图集对象/四帧/16×16 检查通过，完整报告 PASS。已查看 controller-scale-3、parallel-hatches-world、parallel-hatches-reloaded，全部十级物品图标和世界正面没有紫黑缺失，资源重载后保持正常。证据在 D:/CodexTools/GTNGLargeFurnace/build/large-furnace-qa/。
- 构建：同一 init/独立 build 与 cache，compileJava processResources spotlessCheck --offline；large-furnace-textures-build.log 退出 0、2s，重复 textures-repeat.log 退出 0、1s、20 UP-TO-DATE、configuration cache reused。结构检查（1583 Java / 3384 imports / 365 QA paths）及 git diff --check 通过。
- 状态：无剩余项，本任务 QA 客户端已退出；没有操作用户游戏客户端、提交或发布，保留其他聊天工作。

## 量子计算机上游优化（2026-10-23，已完成）

- 目标：补齐 GTNL dev-290 最近量子计算改动，保留本地零耗能、固定规格、大批量流体派单和独立 CPU 分配；不提交或发布。基线 HEAD c70251eb，保留全部并行改动。
- 来源：5837c5a8、2b79458b（#646）、7b176111；GitHub API 固定源码与 LGPL 已核实。Serena Java 初始化 Broken pipe，按规则改用 rg/源码。
- 实现：量子算法、循环视图、超时、材料变体配额、样板优化、名称输入；Java 8/Jabel 与现有 AE2 rv3-beta-1080-GTNH 适配，新网络包追加 ID，late Mixin 按模组和端注册，翻译 Java #tr。许可清单已加入 META-INF/quantum-crafting-port。
- 编译：build-final.log 格式、编译、资源通过，31s；前次 build.log 翻译占位校验、build-retry.log 解构语法、build-api.log 新 Java API、build-java8.log 剩余 API 错误均保留。并行新增源码另有两个 record 缺 Desugar 和 MouseData 误作 int，做最小编译修复。
- 初次实机：client.log，退出 0、2m9s；原生/量子双订单、精确耗料、零耗能、同 tick CPU 分配、断网及矩阵存档恢复、模式按钮检查通过。证据位于 D:/CodexTools/GTNGQuantumPort-20261023/build/quantum-crafting-qa。
- 增强实机：client-final.log 退出 0、1m45s，样板优化次数、量子确认同步、合成树序列化、缺料不扣库存、有种子的净产出循环均通过；client-tree.log 进一步打开真实循环视图，退出 0、1m58s、QUANTUM_CRAFTING_QA: PASS。已查看截图 2–5，名称提示、模式按钮、确认列表和循环回边可见。夹具前次 client-expanded.log 因整理 import 后缺引用编译失败，client-expanded-retry.log 因已有客户端偏好为 true 而失败，分别补齐 import 并在夹具初始化重置偏好，未修改生产默认值。
- 独立服务端：server.log 退出 0、1m17s、QUANTUM_SERVER_QA: PASS；实际加载 common 目标类及 Mixin，检查默认超时、材料变体配额 NBT/精确消耗/取消清空、AE2 缺席不注册以及服务端排除 GUI Mixin。
- 最终构建：使用 scripts/quantum-check.init.gradle 限定 Java 格式化，spotlessApply compileJava processResources spotlessCheck --offline，隔离输出 D:/CodexTools/GTNGQuantumPort-20261023/build，project-cache D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache。final-check.log 退出 0、26s；相同任务 final-repeat.log 退出 0、21s、configuration cache reused；final-repeat2.log 退出 0、3s，compileJava/processResources UP-TO-DATE、23 项 UP-TO-DATE。命令与覆盖边界见 docs/testing/quantum-crafting.md。
- 最终核对：结构检查通过（1583 Java / 3384 imports / 161 mixins / 179 reserved mixin types / 365 QA paths），git diff --check 通过；89 项来源文件/目标与 SHA-256 校验通过，两张原图像素未改。本任务 QA Java 进程已退出，第三方客户端退出线程异常文本保留于日志，未影响 PASS 和 Gradle 退出码。
- 状态：完成，无剩余必做项；默认关闭量子模式，网络有量子计算机时可在 AE 合成数量界面开启。本轮循环视图使用测试配方和生产 API 注入的循环索引，没有穷举整合包循环、自动查询网络包、流体执行或最新 GTNL 同装验证，也没有速度基准；不将其写成已验证。未提交、发布、升级依赖或操作用户实例，保留所有并行修改。

## 超导电磁工厂中部旋转（2026-10-23，已完成）

- 目标：阅读原版神之锻炉旋转源码，让超导电磁工厂运行时中部结构绕竖轴旋转。
- 基线：HEAD c70251eb，保留其他聊天修改，不提交或发布。完整规则及开发、上游、多方块工作流已读。Serena 激活成功，Java 查询 Broken pipe，使用 rg 与固定源码 JAR。
- 已核对：本项目 GT5 5.09.54.205 的 MTEForgeOfGods/createRenderer、destroyRenderer 和 RenderForgeOfGods/initRings、renderRing；上游是缓存结构模型加时间旋转，运行时移除真实环、停止重建。
- 实现：中部五层线圈及中心柱共 233 个方块（148 C / 68 E / 17 H）绕机器竖轴每 240 tick 一圈；外围和上下端盖静止。真实方块保留，客户端同步运行状态并精确隐藏静态面；缓存可见面和连纹，资源重载重建，停止/卸载清理。
- 编译：首轮 32s 因两个 record 缺 Desugar 及并行文件的 formatted/MouseData 错误失败，均已修正；build-retry.log 报 BUILD SUCCESSFUL、54s，格式/编译/资源和 4 项结构测试通过（无失败/跳过）。日志在 .gradle/electromagnetic-rotation/。
- QA：client.log（1m45s）因旧夹具检查现已禁用的单配方锁定失败；client-retry.log（1m40s）已通过启动/停止与精确静态面检查，但旧夹具试图重新提交仍保留的模块任务，启动断言失败。夹具现改为恢复同一 ModuleWork，并保存/比较完整任务 NBT，未改机器加工逻辑。
- 实机：client-modules.log 报 BUILD SUCCESSFUL、2m18s，PASS 结果在 D:/CodexTools/GTNGElectromagneticRotorQA-20261023/build/electromagnetic-rotor-qa/result.txt。四向成型、233 个中部方块精确隐藏、全部原生构件保留、启动/停止/破坏/修复重启、完整模块任务及待输出 NBT 恢复、资源重载、客户端卸载钩子清理通过。QA 将已接受任务 NBT 时长延至 20000 tick 便于观察，生产配方不变。
- 视觉：已查看远景、rotation-0/rotation-1 和资源重载后四张实机截图；中部角度变化且外围固定。最终 compileJava processResources spotlessCheck test（4 项结构测试）成功、26s，final-build.log；结构检查通过（1583 Java / 3384 imports / 438 QA paths），git diff --check 通过。
- 状态：完成，无剩余必做项；本任务静音隐藏客户端已退出。未提交、发布、升级依赖或操作用户游戏实例。来源固定 GT5 a3e1e11241a814c9fa0dd0973d5699548428f689，记录在 ROTOR_REFERENCE.json；其他聊天并行改动保留。

## 大型熔炉与并行控制仓移植（2026-10-23，已完成）

- 目标：移植截图 Enhancedcore 大型熔炉、并行控制仓；按用户追加要求新建 multiblock/parallelmachine 专用目录；线程和并行状态只显示于 Waila，不显示在主界面。
- 基线：HEAD c70251eb，保留所有并行修改，不提交或发布。Serena Java Broken pipe，按规则回退 rg/JAR。
- 来源：本地 Enhancedcore 3.8.0（既有 SHA-256 555a9514…94f7aa）、GTCEu 1.4.4、GTLAdditions 3.3.1Custom。核对 11×13×12 原结构、716 非空位置、LV 64 并行/2 线程、电压逐级翻倍、最多一个并行仓与线程仓；上游为多配方合并批次。
- 实现：新增熔炉 28545、IV–MAX 十级可调倍率并行仓 28546–28555；原结构和八份原 PNG、来源哈希/许可、主机适配获取配方已加入。共用带独占绑定的 Ω 线程仓；多配方批次以原生电炉配方工作，完成输出以账本保留。GUI 独立类，状态仅通过 Waila 服务端快照显示。
- 编译：早期全源码受其他任务未完成源码影响，曾在 D 盘副本隔离调试；未回滚工作区。large-furnace-client-final*.log 已恢复直接编译当前全工作区，compileJava/processResources/spotlessCheck 通过。结构检查 1583 Java / 3384 imports / 161 mixins / 179 reserved / 365 QA paths 和 git diff --check 通过。
- 实机：.gradle/large-furnace-client-final-4.log 退出 0、BUILD SUCCESSFUL、1m9s，完整报告 PASS。原结构、LV/MV、十级倍率/限幅/NBT、重复仓拒绝、Ω 线程增减、铁金两配方 128 并行、精确耗料产出、满输出预检、完成堵塞恢复、断电恢复及 Waila 快照断言通过。GUI 三种比例、真实字段输入同步及关闭重开通过；已查看主界面、并行仓重开、Waila 和全结构截图，主界面没有线程/并行文字，Waila 随倍率更新为 192。
- 证据：D:/CodexTools/GTNGLargeFurnace/build/large-furnace-qa/result.txt 与 screenshots/。命令见 docs/testing/large-furnace.md；当前全源码使用独立 build/cache/run，参考解包和旧快照不参与最终编译。旧夹具的同步项查询、同 tick 关闭/重开窗口及超时日志保留，未把失败记为通过。
- 重复验证：compileJava processResources spotlessCheck --offline，large-furnace-warm-build.log 退出 0、2s；相同命令 large-furnace-repeat-build.log 退出 0、997ms、20 UP-TO-DATE、configuration cache reused。最终结构检查和 git diff --check 再次通过；本任务客户端已全部退出。
- 状态：无剩余实现或验证项；未提交、推送或发布，未改动用户正常客户端，保留其他聊天工作。

## 碎片世界采集器移植（2026-10-23，已完成）

- 目标：从用户指定的 GregTech Leisure-增强版实例移植碎片世界采集器；用户明确要求世界碎片输入替换为各维度对应的 GTNH 维度展示方块。
- 基线：HEAD c70251eb，保留所有已有未提交及并行改动，不提交或发布。已读取完整规则及移植工作流。
- 已核对：本地 GTLCore 1.2.3.2-fix4，普通机器注册名 ulv_fragment_world_collection_machine；另有独立大型变体，不混为同一机器。仓库 ModsItemlist 已有 NEIOrePlugin 各维度方块引用及火箭分级获取配方。
- 工具：Serena initial_instructions/activate_project 成功，但实际 Java 符号查询因语言服务器 Broken pipe 失败，按导航规则使用 rg 和本地 JAR/source 检查。
- 实现：ULV 单方块 ID 28633，16维度原表展开为239条原生配方；世界碎片替换为不消耗的维度展示方块，用户确认远古世界→暮色森林、霜原星→海卫一。七份原始贴图/动画、配方 JSON、来源清单、LGPL 和差异说明在 reference/fragment-world-collector-port 与包内同名 META-INF。
- GUI：按用户反馈撤销自定义按钮/选面面板和默认启用覆盖，直接继承 GTNH 单方块物品/流体开关、Shift 选面、默认值、NBT 同步和自动输出逻辑。输出槽改为4×3、容器宽72，界面194×180，消除九槽容器造成的越界；原生电路槽不叠加本机用不到的模具槽。
- 最终实机：D:/CodexTools/GTNGFragmentPort/client-native-controls-sides.log，spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 成功，1m30s，退出0。239条配方逐条接受、16维度、缺输入/满仓拒绝、MAX+1流体防溢出、原始耗时、钻头29/3000和岩浆94/2000概率、完整NBT恢复恰好完成一次通过。真实相邻箱子/原生LV流体输入仓的自动输出、关闭/满箱保留、原生两个按钮及Shift选面两端同步、关闭重开和GUI比例1/2/3均通过。
- 视觉：PASS报告及截图在上述独立build/fragment-collector-qa；已查看最终比例2/3主界面、原生选面面板、NEI流体页与实际运行方块。此前client-visual-final.log（1m9s）是旧布局通过记录，不作为新布局证据。
- 失败修复：保留早期夹具过早开GUI、首条30tick误断言>200tick、漏调朝向初始化记录。新版client-gui-output.log编译错误（含并行文件及本GUI MouseData参数）已修正；输入仓须等待原生前5tick保护结束。自动输出会原生掉落物品，GUI夹具改用满箱阻挡；调用选面按钮须经原版panel.doSafe事件保护，方向断言按Forge旋转矩阵而非猜测东西。原生生产选面实现未修改。对应失败日志client-gui-output-*、client-gui-verified、client-native-controls-*保留；首次新命令参数解析失败亦保留。
- 最终构建：package-native-controls.log成功9s；相同compileJava processResources spotlessCheck assemble --offline再次成功，package-native-repeat.log复用configuration cache。JAR中控制器/GUI/配方/许可齐全，7份素材哈希正确且无QA/reference类。结构检查1583 Java/3384 imports/365 QA paths通过，git diff --check通过。
- 状态：完成，无剩余必做项；静音隐藏QA客户端已退出。测试构建在D:/CodexTools/GTNGFragmentPort/build/libs；未替换用户游戏实例、提交或发布，全部并行修改保留。GTL独有物品的缺失/替代详见reference/fragment-world-collector-port/PORT.md，不宣称全部物品一比一移植。

## 共用多方块模式选择列表（2026-10-23，已完成）

- 目标：现代基类模式按钮统一为卫星样式列表，普通点击保留窗口，Shift 点击选择后关闭；用户明确排除工业农场、工业蜂场、装配矩阵等独立实现。
- 基线：HEAD c70251eb，保留按钮尺寸修复及全部并行修改；触手现为装配/电路两模式，不恢复旧第三模式。不提交或发布。
- 实现：抽取 MachineModeSelector，共用基类依实际有序配方池生成列表；卫星保留原面板和控件标识并复用列表。服务端先选择再关闭同步面板，相同模式不重复调用 setter。三种排除 GUI 未编辑。原触手 QA 已改为打开列表选择第二项，编译通过。
- 失败记录：初次 QA 将非泛型 GT 基类写为泛型导致编译失败（35s），临时格式脚本调用错误（1s）及旧 LWJGL 键盘字段不存在（1m20s）均已修正，失败日志 .gradle/codex-modern-modes-qa.log、-retry.log、-final.log。最终按固定 lwjgl3ify 源码临时替换本 QA 进程的 SDL 键盘快照，调用实际按钮并在 finally 还原输入，不修改用户客户端或系统按键。
- 最终验证：spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 成功，退出 0、2m15s；日志 .gradle/codex-modern-modes-qa-input.log。精密装配机、综合化工厂、力场压模工厂、法师组装机、超导电磁工厂、触手及四级卫星逐项核对配方池次序与模式编号、普通选择保留窗口、Shift 选择/同项关闭、两端同步及整个 GUI 重开；卫星 Mk III 比例 1/2/3 和最后一项滚动通过。
- 证据：D:/CodexTools/GTNGModernModesQA-20261023/build/modern-machine-modes-qa/result.txt 为 PASS；已查看两项、三项、五项模式弹窗及 Mk III 比例 3 滚动选中图。截图为未成型 QA 控制器，仅验证 GUI，不冒充实际加工测试。新翻译由 Java 声明生成；结构检查通过（最终 1486 Java / 3107 imports / 306 QA paths；期间保留其他聊天新增文件），git diff --check 通过。
- 恢复：scripts/satellite-mode-qa.init.gradle 加 -PmodernMachineModesQA、-PsatelliteModeBuildDirectory=D:/CodexTools/GTNGModernModesQA-20261023/build；项目缓存 D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache。附加 .gradle/modern-modes-check.init.gradle 同时编译更新后的触手 QA；触手全结构 QA 本轮未重复运行，真实模式交互由新场景覆盖。
- 状态：完成，无剩余必做项；静音隐藏客户端已退出。工业农场、工业蜂场、装配矩阵源文件与 HEAD 无差异；未提交、发布、改依赖或运行逻辑，其他并行改动保留。

## 超时空装配线重复分类合并（2026-10-23，已完成）

- 目标：按用户纠正，将“超时空装配线”和“超时空装配线（UIV+）”合并为唯一装配配方池；保留无闪存、无序输入、样板总成和电路无印刻行为。
- 基线：HEAD c70251eb，保留其他聊天的 GUI、渲染等全部并行改动，不提交或发布。
- 实现：删除 UIV 子集注册和重复导入；所有等级使用 tentacle_assembly。机器只保留装配与电路两个模式，独立 GUI 同步减为两个图标；旧模式 0/1→0、旧电路 2→1，新 NBT 标记防止二次迁移。移除过时的 UIV 模式 tooltip。
- 配方验证：通过 scripts/tentacle-recipes-qa.init.gradle，独立 build/project-cache 位于 D:/CodexTools/GTNGTentacleRecipes；merge-client.log 退出 0、1m55s，格式、编译、资源和真实客户端均通过。唯一分类保留全部 453 条原生装配配方；低级/UIV 同模式加工、缺料保护、缓存、完整任务 NBT、实际 ME Ultimate 流体投料、电路无印刻、旧模式迁移和二次存取全部 PASS。
- 视觉：已查看 recipe-2.png 和 recipe-3.png，分别为控制器完整十二物品/四流体配方及 UIV 配方，同名“超时空装配线”、无闪存格。结果与截图位于 build/tentacle-qa；GUI 比例 1/2/3 的 NEI 断言通过。
- GUI 验证：同一 init 加 -PtentacleGuiOnly，merge-gui.log 退出 0、1m15s；真实 CycleButtonWidget 在装配/电路两模式间切换，两端同步 PASS。已查看 build/tentacle-gui-qa/screenshots 的两张实机图。此运行仅验证结构与 GUI，不重复声称验证动态渲染。
- 最终检查：compileJava processResources spotlessCheck --offline 退出 0、2s；相同任务再次运行退出 0、1s，20 项 UP-TO-DATE，configuration cache reused，日志 merge-build.log / merge-repeat.log。git diff --check 和结构检查通过（1483 Java / 3104 imports / 304 QA paths）。翻译由 processResources 生成，UIV 独立分类及过时 tooltip 键已移除。
- 状态：完成，无剩余必做项；本任务两个静音隐藏 QA 客户端已退出，未提交、发布或安装到用户实例。客户端退出阶段第三方导出/关闭线程有异常文本，测试断言与 Gradle 均成功；未把它作为本任务失败。其他聊天并行改动保留。

## 现代化 GUI 按钮尺寸统一（2026-10-23，已完成）

- 目标：截图中标准控制按钮统一为第一个按钮的 16×16；保留操作和服务端同步。
- 基线：HEAD c70251eb，保留所有既有及并行改动，不提交或发布。Serena 激活和 Java 符号查询成功。
- 实现：基类与卫星模式按钮共用 16 像素尺寸；输入分离、批次和配方锁显式覆盖两种状态图标，避免上游状态图标优先于通用 overlay；悬浮/禁用纹理取样排除顶部 1/2 像素透明区域，不改 PNG。
- 验证：首次独立 project-cache 在加载 GitHub manifest 时失败（31s），日志 .gradle/codex-modern-buttons-qa.log。复用 D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache 后，spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 全部成功，退出 0、2m47s；日志 .gradle/codex-modern-buttons-qa-retry.log，独立输出 D:/CodexTools/GTNGModernButtonsQA-20261023/build。退出后第三方 AE2 CSV Export 线程有异常文本，未影响 PASS 和退出码。
- 实机证据：卫星 Mk I–IV、GUI 比例 1/2/3、模式切换与服务端同步、设置及控制器关闭重开均通过；已查看 Mk I 比例 2/3 和 Mk IV 主界面截图。比例 2 截图中普通与禁用按钮外框均为 32×32 屏幕像素，顶/底边对齐（对应 16×16 GUI 单位）。PASS 报告和截图在上述 build/satellite-mode-qa/；保留原有 Waila 与产出显示回归，没有新增加工行为。
- 状态：完成，无剩余必做项；结构检查通过（1483 Java / 3104 imports / 304 QA paths），git diff --check 通过。静音、透明隐藏的本任务客户端已退出，未操作用户正常客户端，未提交或发布。其他控制器继承同一基类，未逐台实机检查；同期卫星产出显示等改动保留。

## 卫星工厂产出恢复主界面（2026-10-23，已完成）

- 目标：用户要求物品/流体产出恢复主界面原版风格；批次、已用/总线程与最短批次时间等状态继续在 Waila。
- 基线：HEAD c70251eb，已读取最近 GUI 分层记录；保留新建 NexusSatelliteFactoryGui 和其他聊天改动，GUI 逻辑不放回控制器。
- 实现：在独立 GUI 中恢复只读批次产出同步与列表，复用现代基类产出行（图标、名称、数量、速率与悬浮详情）；控制器继续提供原始大整数快照。移除 Waila 的产出 NBT、名称/数量行和更多提示，不修改实际输出账本或加工行为。
- 验证：spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 成功，退出 0、Gradle 2m41s。四级卫星、GUI 比例 1/2/3、模式与批次设置、关闭重开通过；五项产出包含流体及超过 long 的数量，服务端清空后同步列表与界面行均消失。Waila 原状态回归通过，且不再传输或显示产出行。报告 D:/CodexTools/GTNGSatelliteBatchQA-20261023/build/satellite-mode-qa/result.txt，日志 .gradle/codex-satellite-output-gui-qa.log。
- 视觉证据：已查看 screenshots/mk1-scale2-main.png、mk1-scale3-main.png 和 mk1-scale2-waila.png；产出图标、数量与速率恢复主界面。截图使用待输出测试账本，0/s 对应等待状态；运行中快照另检查 200 tick 时长。git diff --check 通过，带本轮 QA 标识的客户端进程已退出。
- 剩余：无；未提交或发布，保留其他聊天改动。

## 最近新增多方块 GUI 分层（2026-10-23，已完成）

- 目标：按用户要求，最近新增的 10 种多方块控制器各自使用 gui/modularui/multiblock 下的独立 GUI 类，继承 GTNGModernMultiBlockBaseGui；界面布局、图标和交互不留在机器实现内。
- 基线：HEAD c70251eb，工作区已有卫星工厂、主站 Waila、末晶、触手及渲染等修改，保留所有既有工作。Serena initial_instructions、activate_project 和 SatelliteGui 符号正文查询成功。
- 范围：卫星工厂、猎光号主站及 modularmachine 下的 8 种机器；四级卫星共用其控制器对应 GUI。保持模式编号、同步键、翻译键、NBT 与运行逻辑；不提交或发布。
- 实现：10 个独立 GUI 已建立；卫星控件、模式列表、批次设置和翻译声明整体迁出，通过既有 getAvailableRecipeMaps 读取配方池，不开放私有字段。四种多模式机器的图标配置移入对应 GUI，移除多模式机器基类的图标钩子；其余保留基类原界面。机器处理、同步键、模式编号及 NBT 不变。目录规则和相关 QA 格式化路径已同步。
- 验证：定向 spotlessApply、全工作区 compileJava / processResources、spotlessCheck 和卫星 runClient17 离线成功，退出 0、Gradle 2m49s；日志 .gradle/codex-gui-separation-qa.log。四级卫星模式/批次设置/服务端同步/关闭重开及 GUI 比例 1/2/3 通过，Waila 原有回归通过；已查看主界面、批次设置与 Mk IV 模式选择截图。证据 D:/CodexTools/GTNGGuiSeparationQA-20261023/build/satellite-mode-qa/。退出后已有第三方导出/关闭线程异常文本，未影响 PASS 与退出码。
- 触手验证边界：完整 QA 退出 1、Gradle 1m33s，在 circuitAssembly 的“native circuit assembly starts without imprint”断言失败，尚未进入 GUI；日志 .gradle/codex-gui-separation-tentacle.log，失败报告 build/tentacle-qa/result.txt。下方配方任务正在修改该路径，本轮没有改机器加工逻辑或隐去失败。
- 触手 GUI：增加 -PtentacleGuiOnly 独立场景，复用原真实模式按钮/两端同步检查，跳过配方加工及运行外观；输出在独立 tentacle-gui-qa。首轮提前打开界面，客户端未收到控制器而断开；日志 .gradle/codex-gui-separation-tentacle-gui.log，按精确 QA 标记终止本任务进程。夹具改为等待客户端确认控制器元实体后再打开，并增加超时检查；未修改生产代码。
- 最终验证：spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 退出 0、Gradle 1m8s；日志 .gradle/codex-gui-separation-tentacle-gui-final.log。触手三模式按钮及双端同步通过，已查看模式 0/2 实机截图；报告与三张图位于 D:/CodexTools/GTNGGuiSeparationQA-20261023/build/tentacle-gui-qa/。本轮未逐台实机打开其余 8 种控制器，不把全源码编译称为全部机器的运行验证。
- 恢复：卫星使用 scripts/satellite-mode-qa.init.gradle，-PsatelliteModeBuildDirectory 指向上述 build；触手使用原 scripts/tentacle-qa.init.gradle 的本任务 D 盘路径副本 .gradle/gui-separation-tentacle.init.gradle，加 -PtentacleClientQA -PtentacleGuiOnly。项目缓存 D:/CodexTools/GTNGGuiSeparationQA-20261023/project-cache；格式化限定清单 .gradle/gui-extraction-files.txt 与 .gradle/gui-extraction-format.init.gradle。
- 状态：本次 GUI 分层完成，无剩余实现项；结构检查通过（1483 Java / 3105 imports / 302 QA paths），git diff --check 通过。所有本任务 QA 客户端已退出；未提交、推送、发布、改依赖或操作用户正常客户端。完整触手加工 QA 的配方失败仍如实保留，后续由配方任务核查；同期新增的配方/NEI 文件与其他聊天修改均保留。

## 超时空装配线配方与输入修正（2026-10-23，已完成）

- 目标：配方池改名超时空装配线，NEI 使用原版装配线布局且无闪存栏；本机装配无需研究、无需顺序输入，可用样板总成；电路装配无需印刻。
- 基线：HEAD c70251eb，保留卫星、末晶和触手运行渲染等既有并行改动，不提交或发布。Serena initial_instructions / activate_project 和 Java 符号查询成功。
- 实现：装配池改名“超时空装配线”，UIV 子集带等级后缀；继承原版 4×4 物品区、独立四格流体列及输出位置，去除闪存槽和对应箭头。取消本机研究校验和必装数据仓，保留已有数据仓兼容性及模式编号。电路复制到独立免印刻配方池，原生电路池保持印刻要求；中英文 tooltip 明确无需闪存、无序输入、支持样板总成及无需印刻。
- 加载修正：实际测试发现本 Mod 的 load-complete 执行时电路原池仍为空，原生配方稍后在 GT sGTCompleteLoad 中注册；将本机配方导入排在该回调尾部，解决空电路池。UIV 子集使用独立配方分类，避免 NEI 被普通池分类占用；电路副本保留已经应用过加速设置的耗时，避免倍率重复作用。
- 构建：scripts/tentacle-recipes-qa.init.gradle，独立 build/cache/run 放 D:/CodexTools/GTNGTentacleRecipes。初轮 spotlessApply compileJava processResources spotlessCheck 和两项结构测试通过，53s，build.log；最终同任务通过，12s，final-build.log；重复调用 2s、26 UP-TO-DATE、configuration cache reused，repeat-build.log。两项结构测试无失败/跳过。旧、新两份客户端夹具一起编译及格式检查通过，6s，qa-compile.log。
- 实机：最终 -PtentacleClientQA runClient17 --offline --no-configuration-cache 退出 0、1m49s，client.log 与 build/tentacle-qa/result.txt 为 PASS。完整原结构在无数据仓时成型，实际配方逆序物品/流体供料、缺物品/流体无损拒绝、缓存重复、UIV 模式、电路无印刻及控制器完整 NBT 恢复和精确产出均通过。原版 ME 样板总成取代全部普通输入总线/流体仓，真实供电、Ultimate 原生流体 NBT、pushPattern 和装配/电路两模式精确产出通过。原生电路池无印刻仍拒绝。
- 视觉：五张 NEI 实机截图全部查看，GUI 比例 1/2/3 与重开无槽位重叠、文字遮挡；控制器配方的十二项物品与四项流体完整显示，闪存栏/箭头消失，UIV 与电路池正常显示。截图在 build/tentacle-qa/screenshots/recipe-0.png 至 recipe-4.png；翻译由 processResources 生成，未手改语言文件。前端 GT5 5.09.54.205 来源及 LGPL 记录在包内 RECIPE_FRONTEND.json。
- 失败记录：客户端夹具初次使用 Java 8 不支持的无参 orElseThrow，已修正；初轮筛选要求 >=200 tick，与默认配方加速后的 1 tick 不符，已移除此夹具限制；随后定位并修复上述电路导入时序。失败日志分别为 client-compile-failed.log、client-fixture-failed.log、client-circuit-failed.log、client-loading-failed.log，不计为通过。
- 状态：完成，无剩余必做项。结构检查通过（1483 Java / 3105 imports / 303 QA paths），git diff --check 通过；静音、隐藏的本任务客户端已退出。未提交、发布、改依赖或操作用户正常客户端。并行 GUI 分层及运行渲染等改动保留，最终编译基于当前完整工作区；本次实机结果验证配方/输入/NEI，不扩大为所有并行 GUI 或渲染的验收。


## 卫星工厂 Waila 已用线程（2026-10-23，已完成）

- 目标：Waila 线程行显示“已用线程 / 总线程”。开始时 HEAD c70251eb，保留当前所有并行开发改动，不提交或发布。
- 口径：现有实现没有独立线程调度，线程只定义共享并行额度（Integer.MAX_VALUE × 总线程）。已向用户说明采用当前批次并行量向上折算、以总线程封顶；不以配方种数冒充线程占用，不改变配方分配和吞吐。
- 实现：服务端 Waila 增加 satelliteUsedThreads，以活动状态、有效批次和加工时长为前提；空闲、停机或只等待输出为 0；大并行使用减一后除法避免 long 溢出。沿用线程翻译键改为两个参数；总数仍随现有线程仓计算，存档键不变。
- 验证：spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 退出 0、Gradle 2m26s。四种卫星均通过 0、1、Integer.MAX_VALUE、再加 1、Long.MAX_VALUE 的边界、128/128、停机/空闲/等待输出归零及服务端快照显示；测试活动状态使用 QA 批次夹具，不声称新增独立线程调度。原有模式/时长设置、GUI 比例 1/2/3 与重开回归通过。
- 证据：命令沿用 satellite-mode-qa.init.gradle 与 D 盘 build/project-cache；日志 .gradle/codex-satellite-used-threads-qa.log。PASS 和实际悬浮截图在 D:/CodexTools/GTNGSatelliteBatchQA-20261023/build/satellite-mode-qa/；已查看 mk1-scale2-waila.png，空闲显示“线程（已用/总数）：0 / 128”。中英文翻译均由 processResources 正确生成。
- 状态：完成，无剩余必做项；git diff --check 通过，静音隐藏的 QA 客户端已退出。未提交、推送、发布、改依赖或操作用户正常客户端；其他聊天修改保留。

## 猎光号主站计数移至 Waila（2026-10-23，已完成）

- 目标：按用户截图，将主界面的“已安装星阵 / 64”和“卫星工厂 / 20”移到 Waila；保留原版运行状态、进度和现有扫描器信息。
- 实现：移除 StationGui 自定义状态区，直接使用 GTNG 基类 GUI；主站 getWailaNBTData 提供实际安装量与已连接模块数，getWailaBody 使用服务端数据和原翻译键。没有改变安装、配额、连接判定或存档键。
- 基线：HEAD c70251eb；保留既有卫星工厂、末晶、触手和渲染相关改动。构建及 QA 通过 scripts/satellite-mode-qa.init.gradle 的 -PstationWailaQA 选择主站场景，输出在 D:/CodexTools/GTNGSatelliteBatchQA-20261023/build/station-waila-qa；日志 .gradle/codex-station-waila-qa.log。
- 验证：spotlessApply compileJava processResources spotlessCheck runClient17 --offline --no-configuration-cache 全部通过，退出 0、Gradle 1m37s。provider 检查覆盖星阵 0/64 与 64/64、按实际模块位置和连接判定得到 1/20、断开后 0/20，以及客户端依据服务端快照显示。连接计数测试临时设置 QA 控制器成型标志，随后恢复；不将其称为完整结构成型测试。
- 实机：GUI 比例 1/2/3 和关闭重开通过，主界面没有两个计数同步项；已查看主界面与真实指向主站的 Waila 截图，Waila 显示 64/64 星阵与 0/20 卫星。该悬浮场景为未成型 QA 主站，测试客户端静音、按 QA 标记隐藏并自行退出。四张图和 PASS 报告位于上述 station-waila-qa 目录；退出后后台第三方导出线程出现异常文本，Gradle 与测试结果均成功，未影响本次断言。
- 状态：完成，git diff --check 通过，无剩余必做项；未提交、推送、发布或更改依赖，未操作用户正常客户端。其他聊天的渲染开发保持原状。

## 奈亚拉托提普之触运行渲染对照（2026-10-23，已完成）

- 目标：对照固定 GTO 实例的运行渲染，尽量还原特效。HEAD c70251eb，保留原有全部未提交改动。
- 实现：原机使用 WorkableCasingMachineRenderer；GTM ActiveBlock 随工作状态切换。补齐移植版遗漏的 E 位 154 个超能核心 activeCoils；七组外壳/叠加层的 interpolate 展开为 289 张逐 tick 播放贴图，含 CTM，保持原始帧序、周期和 alpha。原图不改；维度桥接使用共享构件，所以其其他使用处也获得渐变修复。清单记录派生文件和输入哈希，动画 PNG 共 740881 字节。
- 证据：D:/CodexTools/GTNGSuprachronalPort/render-audit/ 中固定 GTM/GTO 源码及本地模型。尚未在原 GTO 客户端录制运行画面，不将源码对照称为同场景动态实拍。
- 验证：初轮编译的 Java 8 集合/Optional API 问题已修正。早期运行采样落在一 tick 任务启停边界而失败，亦纠正了服务端只存 GTCoilTracker 记录、客户端才改 metadata 的检查方式，没有用这些失败证明视觉通过。最终 `render-client-final.log` 退出 0、1m53s，全部 2671 个部件待机/运行/停机两端同步通过，真实图集使用渐变贴图；保留原有研究、输入事务、NBT 恢复、精确产出与三模式 GUI 验证。视觉夹具只在 QA 存档将一份已接受任务延长到 20000 tick，正常推进 204 个服务端 tick；不是原配方时长验证，生产配方未改。
- 视觉证据：已查看最终运行近景、全景和停机图；同视角待机/运行截图保留全部相同舱室。截图在 `D:/CodexTools/GTNGSuprachronalPort/build/tentacle-qa/screenshots/`；`animation-comparison.gif` 为源资源动画时序重放与移植播放贴图对照，不是原 GTO 实机录像。没有虚构原机专属光束/扭曲，不承诺不同光影环境的 Bloom 逐像素一致。
- 最终检查：`compileJava processResources spotlessCheck test --tests '*NyarlathotepsTentacleStructureTest' assemble --offline` 退出 0、15s，`render-final-build.log`；2 项测试无失败/跳过。结构检查通过（1472 Java / 3078 imports / 295 QA paths），`git diff --check` 通过。最终 JAR 的 1523 项资源和元数据与源码/清单一致，无 QA 类或 reference；`render-artifact-audit.json`，SHA-256 `c6d09551599e859626f767e0c1ec42426fec81d7b89c887408c689ddbb4fbdc5`。
- 状态：本轮可由固定源码/资源确认的渲染差异已修复并验证。代理客户端已退出；未修改依赖、版本或用户实例，未提交、推送或发布。原版同场景实拍及光影包差异仍未验证，作为明确验证边界记录，不声称已经完成。

## 枢纽卫星工厂原版主界面与批次设置（2026-10-23，已完成）

- 目标：最短批次时间移到原生调并行按钮打开的面板；主界面取消额外批次、线程与产出文字，移至指向控制器的 Waila。
- 基线：HEAD c70251eb；沿用上一轮模式选择实现，保留工作区已有末晶、触手、tooltip、资源、工具与文档改动；本轮未创建 worktree、提交或发布。
- 实现：通过原生面板标题扩展点加入时长输入，保留并行输入、最大并行切换与 Powerfail Events；默认 20 tick、最低 1 tick，保存键不变。移除 SatelliteGui 的自定义 terminal 覆写，沿用原版主状态区；Waila 使用服务端 NBT 显示批次、线程、时长、等待输出和物品/流体产出预览，最多三项并提示更多，保留大整数数量，空闲时不显示遗留批次计数。
- 验证：以当前工作区 Java 源码构建，spotlessApply / compileJava / processResources / spotlessCheck / runClient17 --offline --no-configuration-cache 全部通过；最终退出 0、Gradle 2m20s。Mk I–IV、GUI 比例 1/2/3 验证原生并行面板双输入、范围 1..2147483647、服务端同步、其他设置独立、设置弹窗和控制器完整关闭重开；主界面不存在原有批次自定义同步项。
- Waila：四类机器均通过真实 provider 的服务端 NBT 与显示回调检查，测试夹具覆盖空闲、加工、等待输出、超过 long 的物品数量、流体、三项预览与更多提示。恢复机器状态后再按服务端快照渲染，确认不依赖客户端旧字段；空闲不残留历史批次。真实鼠标射线指向控制器的 Waila 截图已查看，显示当前批次、128 线程、已修改的 42 tick；此悬浮截图为未成型的空闲 QA 控制器，不冒充完整机器加工演示。
- 视觉与作用域：已查看最终主界面、设置面板与 Waila 截图；主状态区沿用原版，最短批次时间只在并行面板编辑。测试进程静音并由 HideFactoryClient 按本任务标记隐藏，客户端自行退出。截图与 PASS 报告位于 D:/CodexTools/GTNGSatelliteBatchQA-20261023/build/satellite-mode-qa/；初轮日志另存 duration-client.log。
- 恢复：scripts/satellite-mode-qa.init.gradle + scripts/qa/SatelliteModeClientChecks.java；使用 -PsatelliteModeBuildDirectory=D:/CodexTools/GTNGSatelliteBatchQA-20261023/build，项目缓存 D:/CodexTools/GTNGSatelliteModeQA-20261023/project-cache，不传源码快照；日志 .gradle/codex-satellite-batch-waila-qa.log。
- 状态：本轮需求已完成，无剩余必做项，git diff --check 通过。期间另见 KerrNewmanConnectedIcons 与对应动画资源变化，保留其他聊天改动；不将本轮结果扩展为这些并行改动的验证。未提交、推送、发布、改依赖或操作用户正常客户端。

## GTO 超时空装配线模块化移植（2026-10-23，已完成）

- 目标：从用户指定 `Desktop/10-22/.minecraft/versions/GregTech Odyssey` 移植超时空装配线；用户明确选择本实例现版“奈亚拉托提普之触”的结构/外观，并要求接入本项目模块化机器、参数仓与执行核心。
- 基线：HEAD c70251eb，开始已有末晶谐振器、卫星工厂 GUI、工具及文档等修改，均保留；不提交或发布。
- 来源：实例 GTOCore 26.10.5、pack 0.6.0-dev17，JAR SHA-256 `041e3ce8c13c04168f01ea3a9dc3078acfe7533aa8e46348cdc10b1dd3c115b6`；manifest 构建标识对应官方 GTOCore-Main 提交 `156835a0e66a90cbc4e60c3e95a36c35212b97d6`，根许可 LGPL-3.0。机器定义在该提交 MultiBlockD，原名获取方已改为此机器；内嵌 GTM 26.10.53 提供无菌过滤原图。原结构与固定源码逐字节一致。
- 环境：初始 C 盘空间为 0，用户回应开始后复查约 2.8 GiB 可用；本任务上游参考与临时输出放 `D:/CodexTools/GTNGSuprachronalPort`。
- 实现：ID 28544，`modularmachine/NyarlathotepsTentacle`；保留 165×48×49、29,045 个非空位置和 any 空格，62 个舱位、424 个同种加热线圈。十四类构件、原动画/发光层与 611 张 CTM，已有不同版本构件不覆盖。接入参数仓、三类执行核心、原生/特殊能源仓、控制器 NBT 和原输入预检/账本。
- 配方范围：装配模式导入原生装配线并保留数据介质门槛；电路模式使用原生电路装配线与控制器槽位印刻；超时空模式使用原生 UIV 及以上装配配方。没有移植 GTO 专属电路、无人机或终极电池支线。控制器及全部十四构件有生存获取方，性能与材料/研究差异逐项记录在 `reference/nyarlathoteps-tentacle-port/PORT.md`，不冒充闭源 GTOLib 原始性能。
- 构建：使用 `scripts/tentacle-qa.init.gradle` 和 D 盘项目缓存。初轮发现 BartWorks 包名及两个原生字段名不匹配，修正后 `spotlessApply compileJava processResources spotlessCheck test --tests '*NyarlathotepsTentacleStructureTest'` 成功，退出 0、59 s，日志 `D:/CodexTools/GTNGSuprachronalPort/verify.log`。最后 `compileJava processResources spotlessCheck test --tests '*NyarlathotepsTentacleStructureTest' assemble --offline` 再次通过，退出 0、30 s，日志 `final-build.log`；两项结构测试无失败/跳过。
- 实机：`-PtentacleClientQA runClient17 --offline --no-configuration-cache` 最终退出 0、1m23s，日志 `D:/CodexTools/GTNGSuprachronalPort/client-final.log`。PASS 包括完整结构、十四获取方、四参数仓/三核心/两能源仓、缺数据仓/错误朝向失败；真实装配配方无研究不扣料、有效原生介质加工、缓存再次核对研究、控制器完整 NBT 重载后恰好输出一次；真实电路配方无印刻拦截、有效印刻加工与精确产出。实际 MUI2 按钮依次切换三个模式，服务端与客户端同步一致。
- 视觉：已查看完整原结构总览与实际模块化 GUI，四张截图在 `D:/CodexTools/GTNGSuprachronalPort/build/tentacle-qa/screenshots/`。首轮功能 PASS 的截图拍到云层，因此没有视为外观通过；修正测试相机、等待场景加载并关闭云后重跑。客户端静音，HideFactoryClient 按 QA 标记隐藏；旧轮结束确认拦截时仅终止本任务精确标记的进程，最终轮正常退出，未操作用户客户端。
- 打包：`D:/CodexTools/GTNGSuprachronalPort/build/libs/gtnotgood-1.6.6-main+c70251eb62-dirty.jar`，SHA-256 `72e6a8a3df81c59a6f3dabd4dba52ddb50f87e5b9f930a80e160877ca82f8d18`。945 项原始/派生资源及动画元数据全部与源码、清单相符，含 52 原始资源、611 CTM、282 派生动画元数据；未包含 QA 类或 reference。证据 `artifact-audit.json`。结构检查通过（1472 Java / 3079 imports / 293 QA paths），`git diff --check` 退出 0。
- 状态：本轮机器移植及约定范围验证完成，无剩余必做项。构建来自当前完整工作区，含并行聊天已写入的其他功能，不是仅本功能快照；未改变依赖、版本、用户实例、提交、推送或发布。其他任务的记录保持原状。

## 末晶谐振器移植（2026-10-23，已完成）

- 目标：按用户指定，从桌面 10-22 的 GTL 增强版实例移植末影水晶发电单方块；tooltip 参考现有 SteamTurbine。
- 基线：HEAD c70251eb；开始已有 AGENTS.md、gradle.properties、NexusSatelliteFactory.java 修改及 .agents/.serena/docs/scripts 未跟踪内容，保留原状。Serena initial_instructions / activate_project 成功，SteamTurbine 符号正文查询成功。
- 来源：GTL-Enhancedcore 3.8.0，SHA-256 555a9514c4d2fd11a029c091ac053d5cf7c52fba62411f2f043e04454094f7aa；JAR 声明 LGPL-3.0，未确认源码提交。已核查相关字节码、语言、模型和贴图。
- 已完成：LV–MAX 共 14 等级，稳定 ID 28619–28632；原始正面贴图及独立 LGPL 来源记录、GTNH 无线电网发电、机主与待入网 EU 的 NBT、Et Futurum 水晶顶面放置、本级组装机配方。tooltip 使用 SteamTurbine 的说明格式和数字格式化。运行不消耗水晶；LV 发电量 V×32，其余等级 V×128；原版 UXV/OpV 对应 GTNH UMV/UXV。
- 验证：当前工作区 `spotlessApply compileJava processResources spotlessCheck` 退出 0，25 s，日志 `D:/CodexTools/GTNGCrystalQA-final-live-build.log`。最初曾因其他正在开发的机器源码未完成而受阻，先用 c70251eb 加本功能的源码快照验证；最后已切回当前完整工作区并通过，不再以快照代替最终集成检查。实际运行 GT5-Unofficial 5.09.54.205、Et Futurum 2.6.59-GTNH，未修改依赖声明。
- 真实客户端：最终 `runClient` 退出 0，1m15s，`CRYSTAL_RESONATOR_QA: PASS (296 checks)`；日志 `D:/CodexTools/GTNGCrystalQA-final-native-client.log`。检查全部等级的精确无线发电、停机、NBT 恢复、邻格/死亡水晶、拆机结算、空物品堆/错误面/占用/拒绝编辑请求、放置扣量与防重复，以及 100 个自然服务端 tick 的产能。权限拒绝使用 Forge FakePlayer 覆写 canPlayerEdit，未声称测试第三方领地模组。
- 视觉与打包：QA JAR 已继承本体 manifest，日志确认 EarlyMixinsLoader 实际加载；LV/MAX tooltip 的本地文字特效正常，没有原始标签或截断，世界图展示 14 等级及水晶。三张已查看截图在 `D:/CodexTools/GTNGCrystalQA/build/crystal-resonator-qa/screenshots/`。14 张贴图的源文件、目标和 manifest SHA-256 全部一致；结构检查通过（1472 Java locations / 3079 imports / 293 QA paths），`git diff --check` 退出 0。
- 复跑：`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 --init-script scripts/crystal-resonator-qa.init.gradle --project-cache-dir D:/CodexTools/GTNGCrystalQA/project-cache --no-daemon -PcrystalResonatorQaRoot=D:/CodexTools/GTNGCrystalQA runClient --offline --console=plain`。本机命令子进程另设 `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/CodexTools/GTNGCrystalQA/tmp`；脚本提供可选输出目录，未改变全局 Java/Gradle 配置。参考文件迁至该 D 盘目录，原忽略的 build/reference-gtl-enhancedcore 为 junction。
- 状态：本功能无剩余实现或验证工作；代理测试客户端已退出。启动时 C 盘约 40 MiB，因此本次构建、缓存与客户端使用 D 盘；后来复查 C 盘已有空间，并非本任务清理所得。未清理用户内容、提交、推送、发布或操作用户正常客户端；其他聊天的 GTO/卫星/触手等改动和进度保持原状。

## 枢纽卫星工厂配方池 tooltip（2026-10-23，已完成）

- 目标：修复 Mk I–IV 仅显示相同共用说明、遗漏各自配方池的问题；不改变配方或加工行为。
- 实现与证据：`NexusSatelliteFactory#createTooltip` 按实际 `getFactoryRecipeMaps()` 顺序逐行显示本级配方池，四级分别为 10 / 9 / 11 / 6 个，彼此独立而非逐级累加。Mk IV 补充精密组装最高 Mk-4，与现有 `mSpecialValue > 4` 检查一致。依赖 tooltip 缓存按机器 ID 区分，各级不会串用列表。
- 翻译：两个新文案在 Java 使用处声明 `#tr`，由 `processResources` 生成中英文资源；配方池沿用原生翻译键，当前 GT5 依赖缺少中文的池名会回退英文。源目录和构建目录的中英文语言文件 SHA-256 分别一致。
- 验证：`scripts/dev/Invoke-Gradle.ps1 spotlessApply compileJava processResources spotlessCheck --offline --console=plain` 首轮成功，退出码 0、Gradle 50 秒，日志 `.gradle/codex-satellite-tooltip-build.log`。调整颜色复位及 Mk-4 措辞后，编译和格式检查完成，但资源任务曾因目标纹理 `sl_deckband.png` 暂时不可访问失败，退出码 1、27 秒，日志 `.gradle/codex-satellite-tooltip-final.log`；文件随后存在，原命令重试成功，退出码 0、1 秒、24 项 UP-TO-DATE、配置缓存复用，日志 `.gradle/codex-satellite-tooltip-retry.log`。最终 `git diff --check` 退出码 0。
- 范围与剩余：本轮未新增测试、启动游戏、提交或发布，未做客户端视觉验证；tooltip 代码及资源检查已完成。工作区同时出现另一项模式选择 GUI 修改及下方进度记录，已保留，本轮不将该 GUI 的运行验证记为完成。原有 AGENTS.md、Gradle 配置及工具文件改动保持原状。

## 枢纽卫星工厂模式选择（2026-10-23，已完成）

- 目标：模式按钮使用用户参考图的机械臂图标，点击打开可滚动模式列表，显示当前选择；覆盖四种卫星工厂各自的配方模式。
- 基线：HEAD c70251eb；开始时已有 AGENTS.md、gradle.properties 修改及 .agents/、.serena/、docs/development/、scripts/dev/ 未跟踪内容，均保留。未创建 worktree。
- 已核对：现实现为装配机图标的循环按钮；当前 GregTech 5.09.54.205 源码中基类提供 machineMode 同步值，卫星控制器 setMachineMode 保留输入仓/总线配方映射和配方锁清理。
- 实现：使用参考图同款 GregTech Modern LV 机械臂原图，点击打开六行可视、可滚动的模式列表；当前模式用青绿色高亮。四类列表分别有 10 / 9 / 11 / 6 项，沿用原生配方池名称与翻译。点击只在服务端调用既有模式同步值，选中当前模式不重置配方锁；保留螺丝刀轮换模式及原模式编号。操作 tooltip 与弹窗标题在 Java 使用处声明翻译。
- 素材：固定 GregTechCEu/GregTech-Modern 提交 e3844ba943e2388c0856617ae4f8b473a0ae1b64；PNG Git blob 1f393766036a657c6d0b7299ec2962be41f7a016，SHA-256 9426bca7fc0f4b57e007a26ccefa766f42ee76be314e5f114a49615088d9b293，原图逐字节保留。来源与 LGPL-3.0 许可证见 reference/UPSTREAM_PORT_NOTES.md 和 META-INF/satellite-mode-port/。
- 验证范围：执行中出现其他聊天的配方池 tooltip、末晶谐振器及触手机器修改，均保留。普通构建曾被并行末晶代码的 CustomIcon 编译错误阻断，之后 C 盘空间不足；这些失败没有记为通过。最终采用 HEAD Java 源码快照叠加本次当前 NexusSatelliteFactory / GTNGGuiTextures 两个文件，构建与客户端输出放 D:/CodexTools/GTNGSatelliteModeQA-20261023；不据此声称整个并行工作区通过。
- 客户端证据：scripts/satellite-mode-qa.init.gradle + scripts/qa/SatelliteModeClientChecks.java，runClient17 离线运行成功，退出码 0、Gradle 1m44s。Mk I–IV 在 GUI 比例 2 验证列表数量、滚动到底、真实 GUI 点击的服务端选择、客户端同步及关闭重开；Mk I 另验证比例 1 / 3。手工查看实际主界面、选择列表及选中截图，确认图标、青绿色高亮、六行布局与比例变化。测试客户端始终静音并由 HideFactoryClient.ps1 按专属 QA 标记隐藏；已退出，没有操作用户客户端。
- 最终检查：限定本次三个 Java 文件的 spotlessApply / spotlessCheck 通过；格式化仅改变换行后，同步快照并再次执行 compileJava processResources spotlessCheck，退出码 0、Gradle 29s。git diff --check 退出码 0。日志为 D:/CodexTools/GTNGSatelliteModeQA-20261023/client-complete.log、verification-final.log；PASS 报告和 18 张截图在 build/satellite-mode-qa/ 子目录。
- 恢复参数：通过 Invoke-Gradle.ps1 的 -GradleArguments 数组传 -I scripts/satellite-mode-qa.init.gradle；此次另传 -PsatelliteModeSourceSnapshot=D:/CodexTools/GTNGSatelliteModeQA-20261023/source-snapshot/src/main/java、-PsatelliteModeBuildDirectory=D:/CodexTools/GTNGSatelliteModeQA-20261023/build、--project-cache-dir D:/CodexTools/GTNGSatelliteModeQA-20261023/project-cache、--offline --no-build-cache --no-configuration-cache。默认不传源码快照参数时检查当前完整工作区；未改变项目的全局缓存设置或依赖版本。
- 状态：本次按钮与列表需求完成，无剩余实现项；未提交、推送或发布。其他开发任务的状态以各自记录为准。

更新时间：2026-10-23；基线 HEAD：c70251eb（1.6.6）。

## 目标与范围

为 GTNG 评估和配置 Codex 辅助工具，保留游戏功能与依赖版本。不包含提交、推送或发布。

## 已验证

- 原项目规则已逐字保存为 project-rules.md；AGENTS.md 改为导航。
- 五个仓库 Skill 和 Windows Gradle 包装脚本已创建，定位基准已实际运行。
- socket 参数解决当前 Gradle daemon 启动问题；编译、资源、Spotless 检查通过，缓存复用通过；结构检查通过。具体命令、范围、日志与耗时见 tooling.md。
- 仅启用本地 build cache，原 configuration cache/parallel 与全部版本保留。
- 已有两个 worktree；本次未增加。

## 最终状态与剩余工作

- 已完成本轮高收益配置：五个 Skill 全部通过官方 quick_validate.py，完整规则保存一致性通过，git diff --check 通过。Python 编译与 PowerShell 解析通过。
- Serena 2.1.0 已安装，uv tool list / --version / init 与 MCP 握手、initial_instructions、项目激活成功；已通过官方 Codex CLI 登记全局 stdio MCP，原配置已备份。Java 符号正文与引用追踪最终验证通过，完整 MCP 测试退出码 0、passed=true。
- Serena 安装任务完成：独立 JDTLS 1.64.0-202610221949 + 已有 JDK 25 / Lombok / 项目 wrapper；语言服务器和本次下载缓存/项目语义缓存放 D:/CodexTools/Serena，工具本体保留默认 uv 目录。未清理用户既有内容。uv tool list、serena --version 与 Codex MCP 配置复核通过。
- 实测 find_symbol 首次含导入 109.916 s；最终复测含同步 97.750 s、同会话重复 0.102 s；引用查询 6.904 s，返回 19 个文件。证据 .gradle/codex-serena-smoke.json。当前会话不能动态加载新 MCP，用户新开聊天或重启后用 /mcp 做桌面侧确认。
- 已测定位微任务、缓存冷热构建及 Serena 符号/引用查询；首次导入成本高，尚无完整开发任务耗时对照，不能据此声称整项任务提速。
- 本轮未提交、推送、发布、启动游戏或新增 worktree。

## 恢复步骤

先 git status --short 和 git log -1，阅读 tooling.md，再核查 .local/bin/serena.exe、uv tool list 与相关日志。不要重复安装现有工具或恢复旧任务时覆盖用户新改动。

## 外部 Java 风格 Skill 评估（2026-10-23）

- 目标：评估用户提供的 `gtnh-java-style-skill` 是否能补充当前 GTNG 开发规则；不安装或执行该包。
- 证据：npm registry 当前 latest 为 `0.1.1`；下载发布归档并核对 registry SHA-1 `060e42c1e7f66d5dfb94ae95dd996cef65ee93c1`，只读检查全部六个文件，包括 Skill、参考指南和安装脚本。安装器复制 Skill 文档；包未声明依赖或安装生命周期脚本，本次未执行安装器。
- 结论：可补充日志风格、弃用说明配对、字段可见性与魔法值等人工审查提示；命名、注释和格式化与现有完整项目规则、`gtnh-development` 及 Spotless 重叠较多，当前增益有限。它不提供构建加速或语义导航功能。暂不安装；若以后采用，须明确完整项目规则优先，并按项目实际 Java 配置使用示例。
- 状态：本次评估已完成，仅追加此记录；未修改 Java、资源或构建配置，未运行构建、安装 Skill、提交或发布。没有待完成的安装步骤；此前 Serena 状态见上方后续安装结果；该 Skill 评估本身未执行 Serena 安装。

## Java 审查规则整合（2026-10-23）

- 目标：按用户要求，把外部风格指南中有用且不重复的规则补入现有 Skill，以 GTNG 完整项目规则为准，不安装 npm 包。
- 已完成：更新用户目录 `C:/Users/Administrator/.codex/skills/gtnh-development/SKILL.md`，补充日志写法、弃用注解与替代说明、字段可见性及业务常量四项审查提示，限定于任务涉及的代码；保留 GTNH 风格指南来源链接，未复制整份文档或示例。
- 优先级：入口明确要求完整读取 GTNG 的 `docs/development/project-rules.md`，仓库规则和 formatter 优先；GT-Not-Cool 配置仅在该项目使用。没有重复收录命名、注释、import 或格式化规则，也没有引入固定 Java 版本的示例。
- 验证：系统 `skill-creator/scripts/quick_validate.py` 返回 `Skill is valid!`（退出码 0），使用已有 Python 与缓存 PyYAML，无新增安装；对照修改前副本检查差异，Skill 的 `git diff --no-index --check` 无空白错误输出（退出码 1，表示文件存在差异）；仓库 `git diff --check` 通过（退出码 0）。本次仅修改 Skill 文档与此进度记录，不涉及游戏代码，因此未运行 Gradle。
- 状态：本次整合完成，无剩余操作；npm 包未安装、安装器未执行，未提交或发布。该用户目录 Skill 不在本仓库 Git 跟踪范围。结束核对时另见新增的 `.serena/` 未跟踪目录，本次未创建或操作它，也未验证 Serena 当前安装状态；后续恢复相关工作前需重新核实。
- 调用核对：`gtnh-development/agents/openai.yaml` 未关闭自动选择；官方文档 `https://learn.chatgpt.com/docs/build-skills` 确认 `allow_implicit_invocation` 默认 true，模型按 description 匹配后才加载完整 Skill，也可显式指定 `$gtnh-development`。当前 GTNG 的 AGENTS.md 尚未要求每次 Java 开发都加载此通用 Skill，因此新增四条不能视为每次自动强制加载的项目规则；本次仅澄清机制，没有修改调用策略或 AGENTS.md。

## 桌面 MCP 确认（2026-10-23）

- 目标：响应 `/mcp`，确认当前桌面会话已加载 Serena。
- 证据：当前工具列表包含 24 个 Serena 工具；直接调用 `initial_instructions` 与随后带 session 的 `activate_project` 均返回 `isError=false`，项目为当前 GT-Not-Good，活动语言服务器为 java。HEAD 仍为 c70251eb，工作区已有修改保持原状。
- 状态：桌面工具加载、服务响应与项目激活已确认，完成此前待新会话确认项。本轮未重跑 Java 符号或引用查询，因此不新增语义导航健康结论；相关已有实测见上文。未运行 onboarding、构建、安装、提交或发布。

## C 盘空间排查（2026-10-23，清理受阻）

- 目标：按用户要求清理无用文件，保留项目、存档和个人资料。开始 Git 状态核对完成，既有开发改动保留。
- 证据：开始 C 盘可用 0；只读统计用户 Temp 1.84 GiB、npm 缓存 0.67 GiB、CrashDumps 0.25 GiB、Gradle daemon 1.87 GiB。Gradle 依赖、JDK、桌面和 Codex 数据未操作。
- 阻塞：两次清理命令均在创建进程时被自动审批拒绝（blocked by policy）；未确认任何成功删除。随后观测可用约 2.75 GiB，原因未确认，不能归因于本轮清理。
- 剩余：获得明确清理范围后再尝试删除已核对的缓存、崩溃转储和非运行中旧日志，并核实实际释放量；本轮未修改游戏代码、构建或发布。


### QQ / 微信空间排查（2026-10-23）

- 用户追加询问聊天软件占用；仅统计目录大小，未读取聊天正文、删除或移动聊天数据。QQ 文档目录 5.10 GiB，QQ 应用数据 0.56 GiB；微信文档目录 5.80 GiB，微信应用数据 1.28 GiB，总计约 12.74 GiB。
- 微信消息附件目录 5.09 GiB（file 3.94、attach 0.78、video 0.38）；数据库 0.36 GiB、cache 0.26 GiB、日志 0.17 GiB。QQ nt_data 4.62 GiB、nt_db 0.31 GiB。
- QQ 目前运行中。聊天附件属于用户内容，已询问只清缓存日志、选旧附件或迁移 D 盘的偏好；等待范围，尚未执行聊天软件清理。


### 清理范围已明确授权，但执行仍被拦截（2026-10-23）

- 用户明确授权删除 QQ/微信聊天文件、图片、视频，以及 npm 下载缓存、CrashDumps、已停止运行的 Gradle 旧日志；附件已核对约 8.1 GiB，三项缓存/转储/日志约 2.5 GiB。保留聊天数据库、表情及开发依赖。
- 新的附件删除命令仍在创建进程前被自动审批拒绝，原因仅 blocked by policy；未成功执行删除。没有改用其它方式绕过执行拦截。
- 已在桌面生成供用户手动运行的清理脚本（清理QQ微信附件及缓存-20261023.ps1），明确列出授权目录，只删除普通文件、不跟随链接，Gradle 仅删除超过 2 天且 PID 已不存在的 daemon 日志；跳过失败文件、输出 D 盘报告。已通过 PowerShell 语法解析，未执行脚本，未宣称已释放空间。
- 剩余：用户手动运行脚本后，以实际报告核对释放量与跳过项。本轮仅追加进度记录，保留既有开发改动。


### 清理实际完成（2026-10-23）

- 用户明确要求直接运行桌面清理脚本；重新读取完整脚本并核对授权范围、Git 状态后，直接调用该脚本，本次执行成功。
- 实际删除 31,939 个文件，共 10.48 GiB，跳过 0 个；脚本测得 C 盘净增加 10.46 GiB，可用从约 2.40 GiB 增至 12.86 GiB。范围为 QQ/微信本地图片、视频、文件，npm 下载缓存、CrashDumps，以及超过 2 天且 PID 已不存在的 Gradle daemon 日志。
- 证据：D:/CodexTools/DiskCleanup-20261023/result-20261023-115009.json；脚本正常执行完成、报告已读取核实，QQ nt_db 与微信 db_storage 目录仍存在并已统计文件。聊天数据库、表情包及项目/依赖未列入删除范围；未启动或结束用户聊天软件。
- 状态：本次授权清理已完成，无待执行删除；此前拦截记录为历史状态。报告和桌面脚本保留供核查。既有开发改动保留，未运行构建、提交或发布。


### 第二轮空间排查（2026-10-23，清理被拦截）

- 用户反馈 C 盘又只剩约 8 GiB，要求继续排查。开始可用 8.69 GiB；Git 状态已复核，现有开发改动保留。Temp 由此前 1.84 GiB 增至约 4.33 GiB（增加约 2.49 GiB），包含大量 EBWebView 临时配置。未读取聊天正文或新增删除个人附件。
- 新候选：NVIDIA DXCache/GLCache 共约 4.09 GiB；Gradle jdks 根目录 8 个老安装压缩包（8 个对应已安装目录均找到 java.exe），安装后的 JDK 保留；Gradle .tmp 根目录超过 7 天的 gradle_download 文件 15 个、约 0.315 GiB。
- 临时目录另有约 2.23 GiB 的非运行中 WebView 临时数据候选（最近文件超过 30 分钟）；通过进程 --user-data-dir 排除当前 19 个活动 WebView 配置，执行前需要再次复核。未结束任何程序。IntelliJ 索引、插件、LocalHistory 与配置、项目、系统休眠文件均保留。
- 状态：新增范围的清理命令在创建进程前被自动审批拒绝（blocked by policy），未执行任何删除，未绕过拦截。需向用户报告已核对的具体范围与大小，再按明确范围继续；不能将预计释放量记为实际释放。


## 大型熔炉 NEI 与脱氧钢色正面（2026-10-23，已完成）

- 目标：补齐原生 NEI 烧制页机器列表；按用户澄清保留移植面板，仅把青绿金属边框换为脱氧钢灰色。前期主界面状态移至 Waila、复用原生结构砖、并行仓贴图等改动继续保留。
- 基线：HEAD c70251eb，工作区多项既有并行开发改动保留；未创建 worktree、提交或发布。
- 实现：新增客户端 NEIGTNGConfig，以固定 NEI 2.8.155-GTNH API 将大型熔炉关联 FurnaceRecipeHandler；GT5 5.09.54.205 的 furnaceRecipes 本身 disableRegisterNEI，不会自动出现在原生烧制列表。两张正面各仅 62 个金属色像素转灰，逐像素核对其余 194 个像素、16×16 尺寸及 alpha 不变；源哈希与修改后哈希写入资产清单。生成的颜色研究图改动了布局，未将其重绘布局打包。
- 当前证据：完整工作区 compileJava / processResources / spotlessCheck 已进入成功后的真实客户端阶段；结构检查通过（1582 Java、3381 imports、161 mixins、364 QA 路径），git diff --check 退出码 0。
- 初次验证状态：NEI 与待机贴图已通过；运行状态与最终构建复核在下列记录完成。

- 最终客户端：.gradle/large-furnace-nei-steel-verified.log 成功，退出码 0、1m34s；result.txt 为 PASS。实际打开原生烧制页，断言左侧列表有且仅一个大型熔炉、未误注册燃料页，并从控制器 U 查询进入烧制配方。真实通电/持续供料后确认客户端 active 状态，查看待机、运行与 NEI 截图；两种状态边框均灰色，绿色运行灯与橙红热量条保留。原有结构、原生砖、批处理/输出/NBT/断电、十级并行仓与动画资源重载、GUI 同步/Waila 全部回归通过。
- 修正的测试问题：静态 sprite 在上传后会清空 CPU 帧缓存，颜色核对改为读取实际资源管理器；运行截图最初延迟过长，样本批次仅 20 tick，现持续供料并等待实际客户端 active 状态。过程中其他开发将项目切换为 JVM Downgrader，未降级的 QA 类 major 69 被 Forge 8 扫描忽略；只结束已核实的本任务 QA PID 35624，使用临时 -PenableModernJavaSyntax=jabel 完成检查，并为该 QA 入口加字节码版本提前失败提示。保留新的项目构建配置，未回滚其他工作。对应失败日志 nei-steel-client.log / client-final.log / active.log / active-jabel.log 保留，不记为通过。
- 最终构建：相同 Jabel 临时参数下完整当前主源码 compileJava、processResources、spotlessCheck 通过；.gradle/large-furnace-nei-steel-build.log 与 repeat.log 记录连续两轮，第二轮配置缓存复用、20 项 UP-TO-DATE。生产 QA JAR 内新 NEI 配置及 10 个 PNG/动画元数据哈希全部核对成功。
- 状态：此次 NEI 与颜色修正完成，无剩余实现项；测试客户端已退出，未操作用户客户端、提交、推送或发布。图片证据：D:/CodexTools/GTNGLargeFurnace/build/large-furnace-qa/screenshots/furnace-front-inactive.png、furnace-front-active.png、nei-smelting-catalyst.png、nei-furnace-usages.png。

- 渲染机制核对（用户后续询问）：固定 GT5 5.09.54.205 的 Textures.BlockIcons#createTextureWithCasing 明确按机壳底图、透明面板、可选发光层叠加；MTEElectricBlastFurnace#getCasingTexture 单独选择底图，不由 StructureDefinition 自动推导。当前 LargeFurnace#getTexture 同样是 casing 16 + overlay；移植 PNG 自带金属边框，前次只是将其换灰，尚未拆分为可随底图变化的透明区域。本轮仅核对并解释，没有新增材质修改或待执行实现承诺。

## 大型熔炉透明正面叠层（2026-10-23，已完成）

- 目标：按用户确认，将正面代表机壳的金属边框透明化，沿用 GTNH 机壳底图 + 正面 overlay；保留原面板、按钮和指示灯。
- 实现：两张 16×16 正面各将原 62 个边框像素改为 alpha 0，其余 194 个像素与固定上游原图逐像素一致，最终各 122 个透明像素。控制器已有独立 casing 16 + overlay 渲染，无需改动玩法或结构定义。来源哈希、最终哈希和改动说明已更新。生成编辑预览改变了原布局，未作为最终资源使用。
- QA 适配：读取新的项目规则和 java.md 后，将本任务 QA 改为加载项目标准降级开发 JAR 和 downgradeTestClasses 输出，沿用 Java 25 / JVM Downgrader 默认配置，不再使用 Jabel 临时覆盖。仅改动本任务测试入口。
- 资源核对：原始布局及非边框像素保持一致；实际运行 JAR 的 10 个 PNG/动画元数据 SHA-256 均匹配清单，主类基础版本 52，未回退 Java 25 / JVM Downgrader 配置。

- 最终验证：.gradle/large-furnace-overlay-client-final.log 退出 0、1m29s、34 项任务，result.txt 为 PASS；已查看两种真实正面及 NEI 截图，机壳从透明边框处露出，仪表和运行灯保持原样。原有结构、原生砖、批处理/输出/NBT/断电、并行仓动画与资源重载、GUI/Waila、NEI 列表/U 查询均回归通过。
- 构建适配过程：第一轮旧自制主 JAR 与新默认开发 JAR 重复加载；仅结束已核实的本 QA PID 8428，移除旧自制主 JAR 加载，直接沿用标准降级开发 JAR 和项目启动前不可变快照，之后测试成功。首次失败日志 large-furnace-overlay-client.log 保留，不计为通过。
- 收尾：large-furnace-overlay-build.log / overlay-repeat.log 两轮编译、资源、Spotless 成功；重复运行 20 项 UP-TO-DATE、configuration cache reused。结构检查通过（1583 Java、3381 imports、161 mixins、364 QA 路径），git diff --check 通过。未修改玩法、结构、注册 ID 或资源路径。
- 状态：透明正面叠层完成，无剩余实现项；QA 客户端已退出，未操作用户客户端、提交或发布。最新实机图片仍在 D:/CodexTools/GTNGLargeFurnace/build/large-furnace-qa/screenshots/。


## GTL 机器透明正面资源（2026-10-23，已完成）

- 目标：按用户要求增加不同机器的透明正面资源，供独立机壳叠层复用。
- 实现：从固定本地 Enhancedcore 3.8.0（4 套）与 GTLCore 1.2.3.2-fix4（12 套）拷贝 16 套原生透明图，合计 61 张 PNG、19 份 mcmeta、39,260 字节，包含原有发光、暂停和动画；存入 assets/gtnotgood/textures/blocks/parallel/fronts。全部源字节保留，16 种待机图哈希互异，筛除不透明整面图。
- 来源：META-INF/machine-front-overlays 保存安装包版本/哈希、作者、mod 元数据中的 LGPL 第 3 版声明、源/目的路径、逐文件哈希及完整 LGPL/GPL 文本。源码提交未确认，不猜测。reference/UPSTREAM_PORT_NOTES.md 已补记。
- 资源检查：.gradle/front-overlays-audit.json 记录所有 PNG 每帧透明像素数量及源哈希核对；16/32 方形帧、纵向排列、全部帧透明区域、有效动画索引/时长均通过。初次 PowerShell 检查把 JSON Int64 当对象误报中子态素压缩机索引；改用 ValueType 后通过，并用 Python 独立核对索引，没有修改正确的上游动画。
- 构建：使用 scripts/dev/Invoke-Gradle.ps1、既有 large-furnace-qa.init.gradle、D:/CodexTools/GTNGLargeFurnace 的隔离 build/cache 执行 processResources --offline --console=plain；.gradle/front-overlays-resources.log 退出 0，21 秒。相同命令重复记录 front-overlays-resources-repeat.log，退出 0、1 秒、11 项 UP-TO-DATE、configuration cache reused。输出目录的 80 个资源与 4 个许可/清单文件逐字节匹配。git diff --check 通过。
- 文档：docs/development/machine-front-overlays.md 列出路径、状态、动画、casing + front + glow 接法；相邻 HTML 提供原 PNG 的状态/动画叠层预览。已检查 16 张卡片、78 个本地引用和 JavaScript 语法；内置浏览器因 file 协议限制拒绝打开，未进行浏览器截图验证，也未绕过限制。
- 状态：资源移植完成；本轮没有新增机器、绑定现有控制器、改动游戏 Java/ID/依赖或覆盖大型熔炉贴图，未做新机器实机验证。原游戏内容与其他任务改动保留，未启动 QA 客户端、提交或发布。


## 并行控制仓上游外观补齐（2026-10-23，已完成）

- 用户确认截图为目标，要求各等级外壳和正面均按图移植。基线 HEAD 3b491cfc；上轮未提交的 16 套面板库保留。
- 已确认问题：原先只加载 mk1-4，并把后六级钳制到 mk4；漏掉发光/运行层，外壳沿用 GTNH 默认机壳。
- 实现：补齐 GTCEu 1.4.4 十级顶/底/侧机壳、mk1-4 与 GTLCore 1.2.3.2-fix4 mk5-10 的待机/运行/发光层和 mcmeta。十级外观一一映射，GTNH UMV/UXV 对应上游 UXV/OpV；机壳在物品和结构内均保持源等级外观。保持旧资源路径、注册 ID、倍率和 NBT。
- 资源：117 个并行仓/机壳文件（40 张正面、40 份正面 mcmeta、30 张机壳、7 份机壳 mcmeta）逐字节匹配源；mk1-9 四帧、mk10 八帧。UHV 机壳两帧、上游 OpV 八帧、MAX 侧面十帧及原显式帧序均保留。实际客户端快照 JAR 的全部 117 个哈希核对成功，来源和许可证记录已更新。
- 初轮检查：parallel-hatch-appearance-client.log 退出 1，QA 捕获 UHV 机壳缺失图；当时少了七份机壳动画元数据。补齐后增强用例，逐面断言帧尺寸、动画元数据及帧数，首次失败不计为通过。
- 最终实机：.gradle/parallel-hatch-appearance-client-final.log 退出 0、1m40s、34 项任务，result.txt 为 PASS；十级分别核对真实图集、待机/运行/发光层、六方向机壳、世界实例及资源重载。已查看 parallel-hatches-world.png，外壳和各色正面与用户目标相符；物品、GUI 同步/Waila、结构、并行倍率/NBT、双配方批处理/输出安全/断电、NEI 全部回归通过。
- 收尾：parallel-hatch-appearance-build.log / repeat.log 编译、资源、Spotless 均退出 0（20 秒 / 1 秒）；重复轮 20 项 UP-TO-DATE、configuration cache reused。结构检查通过（1583 Java、3381 imports、161 mixins、364 QA 路径），git diff --check 通过。过程中共享仓库其他任务推进 HEAD，未回退其改动；运行快照为 db917afcc8。
- 状态：外观修正完成，无剩余实现项。截图 D:/CodexTools/GTNGLargeFurnace/build/large-furnace-qa/screenshots/parallel-hatches-world.png、parallel-hatches-reloaded.png；本任务 QA 客户端已退出，未操作用户游戏窗口，未自行提交或发布。
