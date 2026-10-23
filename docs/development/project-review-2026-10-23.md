# GTNG 项目审查与 GTNL 目录对照

审查日期：2026-10-23。结论：保留 GTNG 现有主干，优先修正开发工具的存放位置、QA 可复现性和业务代码归属；GTNL 可参考其按机器族分类的方式，不宜整体照搬目录及命名。

## 范围与基线

- GTNG：当前工作区，HEAD `c70251eb623a71bc146d8f8745004a4016a51d3a`，包含既有未提交及未跟踪源码。审查期间另有 Java 25 迁移修改 `addon.gradle`、`gradle.properties` 等文件，因此这是工作区静态审查，不是某个干净提交的发布验收。
- GTNL：相邻目录 `../GT-Not-Leisure-dev-290`。该目录没有 `.git`，不能仅凭目录名断言对应某个提交，也不将之前联网查询得到的 GTNL HEAD 套用到这份本地内容。
- 全量枚举主源码、测试、资源和包目录；扫描包名、文件名、翻译声明、同名类型、资源大小及重复内容；定向阅读初始化、注册、网络、机器处理、GUI、渲染、构建与 QA 入口。
- 未逐条执行全部机器配方、全部 GUI 和联网场景，未运行本轮全量 Gradle 测试或性能分析。文件扫描不等于完整语义调用图，不能据此直接删除“未搜索到调用”的公开 API。
- 本轮只新增此报告、更新进度，审查中间结果写入忽略目录 `.gradle/project-review-20261023/`；没有重命名代码、迁移资源、删除文件、改依赖、提交或发布。

### 数量口径

| 项目 | GTNG | 本地 GTNL |
| --- | ---: | ---: |
| `src/main/java` 下 Java 文件 | 1,439 | 1,359 |
| 主模组包下 Java 文件 | 1,394 | 1,299 |
| `src/test/java` 下 Java 文件 | 144 | 0 |
| 主模组中包含大写目录段的包路径 | 1 | 48 |
| 多方块主目录直接存放的 Java 文件 | 17 | 61 |

GTNG 另外 45 个主源码文件属于 `com.xyp.ldlib`；GTNL 另外 60 个分布在 ReAvaritia 等命名空间。以上是文件数，不是测试用例数、覆盖率或代码质量评分。GTNL 的 0 只表示这份目录没有 `src/test/java` 文件。

GTNG 唯一混合大小写包 `common.machines.multiblock.multiMachineBase` 是项目规则明确保留的路径，不属于待修复违规。GTNL 的 `basicMachine`、`appliedEnergistics`、`portableWorkbench`、`quamtumComputer` 等不应复制为本项目的新命名标准。

## 已确认的工程问题

这里的 P2 表示值得优先安排的工程改进，P3 表示后续维护项，不表示已证实发生游戏崩溃。

### 1. P2：结构转换脚本进入了游戏资源和发布 JAR

证据：`src/main/resources/assets/gtnotgood/multiblock/python/` 存有 `compile_mbs.py`、`convert_to_mbs.py`、`decompile_mbs.py` 和 `input.txt`。脚本是带命令行输入的开发工具；检查现有 `build/libs/gtnotgood-1.6.6.jar`，四个文件均在包内。

建议迁到 `scripts/structures/`，把输入样例放在同目录的 `fixtures/`。修改脚本的路径解析，明确传入资源根目录。运行时 `.mbs` 继续留在原资源路径，搬迁后检查 JAR 不再包含开发脚本。收益是职责和打包边界清晰，不夸大为明显的包体积优化。

现有 JAR 只是证明这个问题已经出现在产物中，本轮没有重新构建该 JAR。

### 2. P2：部分 QA 脚本绑定了开发者机器

证据：`scripts/tentacle-qa.init.gradle:2,7` 和 `scripts/tentacle-recipes-qa.init.gradle:2,7` 写死 `D:/CodexTools/...` 的缓存及输出目录；两份 JourneyMap QA 初始化脚本包含 `C:/Users/Administrator/Desktop/GTNH/instances/`。部分夹具还要求预先存在 `build/factory-qa-dependencies/...jar`。

建议提供统一的输出根目录和外部实例参数，默认使用项目内忽略的独立目录；已有依赖应通过固定坐标或明确的准备步骤取得。可沿用 `large-furnace-qa.init.gradle` 已有的“属性覆盖、相对路径默认值”做法。独立 build/run/cache 和 QA 窗口作用域继续保留。

### 3. P2：项目结构规则尚未进入常规构建校验

证据：`scripts/qa/CheckProjectStructure.ps1` 能检查包路径、项目 import、Mixin 保留包及 QA 源路径，本轮执行通过；但仓库 `addon.gradle` 和 `.github/workflows` 中没有调用它或等价结构检查。CI 使用 GTNH 共用工作流，当前上游执行 `assemble`、`build` 和服务器启动，没有自动发现这份项目专用 PowerShell 脚本的步骤。参见 [GTNH 共用构建工作流](https://github.com/GTNewHorizons/GTNH-Actions-Workflows/blob/master/.github/workflows/build-and-test.yml)。该链接为审查日可变分支，不代表过去 CI 每次运行的版本。

建议将等价检查接入 `check` 或独立 CI job，并兼顾 Linux/Windows；构建接线仍放 `addon.gradle`。在已有检查上补充新包全小写、开发脚本禁止打包等规则，保留 `multiMachineBase` 和移植模块例外。不要把简单正则匹配升级描述为完整 Java 语义检查。

### 4. P2：根目录堆转储未被 Git 忽略

证据：`java_pid20200.hprof` 为未跟踪文件，大小 774,369,519 字节，约 738.5 MiB；`.gitignore` 没有覆盖此文件。

建议加入堆转储及 JVM 崩溃产物的忽略规则，并将需要保留的诊断文件存入约定的忽略目录。先判断是否仍需诊断，再决定是否清理；本轮没有读取其内存内容或删除它。此项主要防止误暂存和仓库污染，不能据文件存在推断当前存在内存泄漏。

### 5. P3：预提交钩子安装任务是空入口

证据：`addon.gradle:121` 的 `installPreCommitHook` 从 `scripts/pre-commit` 复制，但该源文件不存在，`onlyIf` 会让任务跳过。

建议补齐实际钩子，或去掉无用任务。质量门禁应由 `check`/CI 提供，不能依赖每个开发者自愿安装本地钩子。若保留安装功能，还应使用 Git 解析 hooks 路径，兼容 `.git` 为文件的 worktree。

## 目录与命名对照

以下路径均相对于各自主模组 Java 包。

| 职责 | GTNG 现状 | GTNL 现状 | 对 GTNG 的建议 |
| --- | --- | --- | --- |
| 方块 | `common/blocks/<功能>` | `common/block/blocks`，另有 tile/item 子包 | 保留 GTNG，方块、Tile、ItemBlock 按功能聚合更直观 |
| 普通物品 | `common/items/<功能>` | `common/item/items` 及其他功能子包 | 保留 GTNG，避免增加重复的 item/items 层级 |
| AE 部件 | `common/parts/<功能>` | `common/part` | 保留，单复数不值得全局迁移 |
| 单方块机器 | `common/machines/basic` | `common/machine/basicMachine` | 保留全小写 `basic` |
| 多方块 | `multiblock`、`modularmachine`、`parallelmachine` | `multiblock`、`steam`、`wireless`、`module` 等 | 学习按机器族分类；目前 17 个普通控制器无需强拆更多目录 |
| 基类 | `multiblock/multiMachineBase` | `machine/multiMachineBase` | 按本项目规则保留，不为少一层目录破坏既有引用 |
| 结构定义 | `multiblock/structure` | 结构类/文件按上游机器组织 | 保留独立结构层，控制器不再塞大型形状数据 |
| 多方块 GUI | `gui/modularui/multiblock`，少数留在父包 | 79 个文件直接在 `gui/modularui` | 完成本项目自己的归类，比照搬上游扁平包更合适 |
| 渲染 | `client/render` 及功能客户端包 | `common/render`、`utils/render` 等 | 保留本项目 client 边界；按明文规则保持核心渲染包扁平 |
| 网络消息 | `common/network/<功能>` | `common/packet` | 保留 network，统一新增消息的类名和功能子包 |
| 配方 | `recipe/gregtech`、`gtnotgood`、`machine` | `recipe/gregtech`、`gtnl`、按兼容模组分包 | 明确分类判定，不因上游用了 gtnl 就必须保留相同分类逻辑 |
| 移植模块 | `ae2thing`、`commandtree`、`com.xyp.ldlib` | ReAvaritia 等独立命名空间 | 保留来源边界和许可，不强行揉进主模组目录 |

### 6. P2：把有状态的机器业务从 utils 归回功能层

`utils/machine/factory` 有 18 个文件。`FactoryRuntime.java:20` 持有任务状态、物料缓冲、剩余时间及能耗，并负责任务推进和 NBT；`FactoryRouting`、`FactoryReservations` 等也是工厂业务。它们已被拆分为多个类，下一步应修正归属，而不是重新合为大类。

建议业务运行、计划和存储放到 `common/machines/processing/factory`，显示/文本职责按实际依赖分别保留或归入 GUI。`utils/crafting` 的批量合成逻辑可评估归到 `common/crafting/batch`，与已有 `common/crafting/quantum`、`cycle` 相邻。纯数学、编码和无状态通用辅助才继续放 utils。

迁移时逐项检查测试、Mixin 引用、反射字符串、QA 路径和来源清单；若存在外部 Java API 使用者，需要兼容转发或明确版本边界。包移动本身不会让 tick 更快。

### 7. P3：完成多方块 GUI 的位置统一，保留紧耦合例外

`AssemblerMatrixGui.java:58`、`QuantumComputerGui.java:38` 都继承 `GTNGModernMultiBlockBaseGui`，但还在 `common/gui/modularui` 根包。其余大多数机器 GUI 已位于 `common/gui/modularui/multiblock`，这两项可以单独迁移。

`FragmentWorldCollectorGui` 等是包内可见类；方块、Tile 与 GUI 紧耦合留在同一功能包也是项目已有允许的例外。不能以“名字包含 Gui”为条件一键搬到 client：ModularUI 面板包含服务端同步和交互逻辑，`common/gui` 的存在是合理的。

### 8. P3：给配方分类增加明确判定

当前 `recipe/machine` 有 48 个文件，既有机器获取配方，也有 ME 网络、方块/部件配方和业务规则；`recipe/gtnotgood` 又包含 `AdvancedIORecipes`、`PackagedRecipes`、`PatternSorterRecipes` 等获取配方。新功能容易出现“类似文件在两个分类都能找到”的问题。

建议先在结构文档明确：原生 GT 配方池扩展、定制加工配方、方块/物品/机器获取配方、加载后研究与适配规则分别放哪里。实施时可让获取配方按 `recipe/content/<功能>` 分类，定制加工配方按 `recipe/process/<功能>` 分类；这是候选规则，需要先统一规范再逐功能迁移，不能只搬一半。`RecipeLoader` 继续控制 init/postInit 顺序，整理文件时不能改变原顺序和可选模组条件。

### 9. P3：修正少量不一致名称，避免全仓库改名

| 现有名称或模式 | 建议 | 边界 |
| --- | --- | --- |
| `MachineLoader.registerbasicMachine()`，315 行 | `registerBasicMachines()` | 修正单词边界和复数；同步 426 行调用及可能的外部引用 |
| `NetworkHandler.registerAllMessage()` | `registerAllMessages()` | 方法名调整不得改变消息注册顺序 |
| `ItemsLoader` 与 `BlockLoader` | 后续统一为 `ItemLoader` / `BlockLoader` | 一致性改进，收益低于业务归类 |
| `MessageMEWirelessVisualization`、`WirelessVisualisationPacket`、`SwapItems` | 新消息统一 `<Feature><Action>Packet`，统一美式/英式拼写 | 旧类型分批处理，保持网络 ID |
| `Config`、`MainConfig` | 明确“总入口”与“机器配置”等职责 | 不直接改已有配置文件名、分类和键 |
| `MainConfig.someOption` / `isSomeOption()` | 核对用途后用业务名或移除确实无用的占位入口 | 文本引用检索不足以证明公开方法可安全删除 |
| `QuestLoader:8` 的字面量 `"gtnotgood"` | 复用 `ModList` 中的模组 ID | 实际 ID 字符串不变 |

当前项目主源码中的文件名整体遵循大驼峰；扫描得到的下划线类型名例外位于既有 `ae2thing` 模块，不建议为视觉统一破坏移植结构。`ModsItemlist`、`GTNGItemList`、`GTNGMachineID` 和 `multiMachineBase` 都受现有项目规则约束，不在本轮随意重命名候选中。

## 性能与可维护性候选

### 10. P2 候选：先测量大纹理，收益可能高于资源去重

资源总文件大小约 25.07 MiB，其中下面三张占 51.1%：

| 路径，均位于 `textures/render/lighthunter/` | 实际编码 | 尺寸 | 文件字节数 |
| --- | --- | --- | ---: |
| `space_layer.png` | JPEG | 8192×4096 | 7,200,691 |
| `heartofuniverse.png` | JPEG | 8192×4096 | 1,610,876 |
| `star_layer_1.png` | PNG | 4096×2048 | 4,629,468 |

按每像素 4 字节估算，这三张纹理的基础像素数据约 288 MiB，未计 mipmap、上传临时副本等。这是容量估算，不是本轮测得的 GPU 显存或 FPS。`LightHunterSpaceStationRenderer` 会引用这些纹理，且已有按工作状态渲染、模型懒加载和有限大小 profile 缓存，不应重复提出已经存在的缓存方案。

建议先测开启机器、停止、资源重载和远近视角时的显存与帧时间；若确有瓶颈，再提供可选低分辨率档或更精细的加载策略，并进行实机视觉对比。两张扩展名与编码不一致的图片沿用上游原文件名，来源清单已有记录；这是工具校验需识别的例外，不直接判定游戏加载失败，也不擅自转换原资产。

全资源中有 6,776 个 `.png` 路径，内容相同的重复组 265 组，重复字节总量仅约 278.4 KiB。不要为了这点收益批量改贴图路径、动画关联或许可证清单。

### 11. P3：按变更热点拆职责，避免按行数机械拆文件

| 文件 | 行数 | 后续检查方向 |
| --- | ---: | --- |
| `ae2thing/quickterminal/client/GuiQuickEncodingTerminal.java` | 2,716 | 编辑状态、输入处理、布局和预览是否可独立维护 |
| `common/crafting/quantum/crafting/CraftingCalculation.java` | 2,190 | 保留上游来源和模块结构，先补压力场景及取消/超时观察，再考虑局部拆分 |
| `client/research/WeightedResearchSolver.java` | 1,544 | 保留纯算法测试，按实际耗时定位求解热点 |
| `common/machines/basic/BasicIndustrialApiary.java` | 1,451 | 生命周期、产出计算、库存持久化边界 |
| `common/machines/multiblock/AssemblerMatrix.java` | 1,438 | 样板缓存、库存、NBT 和机器生命周期边界 |

行数只是维护热点候选，不证明性能差。GTNL 的 `MachineLoader` 达 5,149 行，因此不能以“像 GTNL 一样集中”为优化目标。GTNG 已有 144 个测试源码和多类真实客户端夹具，重构时应复用现有验证，增加真正覆盖故障边界的测试，不追求空泛的数量指标。

### 12. P3：强化翻译冲突和协议编号的防回归

Java `#tr` 静态扫描共得到 2,269 个不同键，26 个键有重复声明，未发现这次扫描范围内的英/中文冲突。`addon.gradle:56` 对未来发生的不同值覆盖只打印日志，然后继续写入。

建议继续允许相同值重复声明，对同语言同键不同值报错，并使用稳定的源文件顺序生成 lang。保持翻译在 Java 使用位置声明，不手改 `.lang`，不把当前没有冲突描述成已经发生错误。

`NetworkHandler:33` 以 `id++` 决定消息编号；同一配置消息双向共享编号是现有设计。未来按名字或文件夹重新排序注册调用会改变协议。可固定已发布编号常量或加注册表快照校验；新增消息追加，不能为整理代码重排旧编号。

## 建议的整理顺序

1. 先补结构/打包校验及堆转储忽略规则，解决 QA 路径可配置问题；核实或移除空的 hooks 入口。
2. 将结构转换脚本迁到 `scripts/structures`。为 114 份 `.init.gradle` 和 114 个根层 QA Java 文件建立按功能查找的索引；保留现有 `scripts/qa` 夹具位置，先减少重复配置，不急于全盘移动入口。
3. 单独迁移两个 GUI 类；再按功能迁移工厂业务类及测试。每个小批次检查 package/import、反射、来源清单和 QA 路径，并运行相关编译与回归。
4. 明确配方及命名规则后，只处理当前功能涉及的文件；已发布 ID、NBT 键、配置键、资源路径和消息编号保持兼容。
5. 最后进行纹理显存、量子合成大订单、机器 tick 的针对性测量，按实测排序性能工作。

候选结构示例，标注的目录是建议，不表示已创建：

```text
common/
  blocks/<feature>/                 保留
  items/<feature>/                  保留
  parts/<feature>/                  保留
  machines/
    basic/                          保留
    hatch/<feature>/                保留
    multiblock/
      multiMachineBase/             保留指定历史路径
      modularmachine/               保留
      parallelmachine/              保留
      structure/                    保留
    processing/factory/             建议：工厂业务
  crafting/
    quantum/                        保留移植内部结构
    cycle/                          保留
    batch/                          建议：批量合成业务
  gui/modularui/multiblock/          建议：补齐两项 GUI
  network/<feature>/                保留并统一新增消息命名
scripts/
  dev/                              保留
  qa/                               保留夹具入口
  structures/                       建议：结构转换工具
```

文档方面，`docs/project-structure.md:75` 仍列出当前不存在的 `src/vendor`；建议删除过时描述或明确它只是未来保留约定。`progress.md` 已包含大量已完成任务及历史失败记录，可把完成记录按日期归档，当前页只保留活动任务和证据链接，归档时不能丢失失败和验证边界。

## 验证与证据

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/qa/CheckProjectStructure.ps1`：通过，1,583 个 Java 位置、3,381 个项目内 import、161 项 Mixin 注册检查、179 个保留命名空间顶层类型、364 个 QA 路径。
- `git diff --check`：通过；Git 提示部分既有文件将规范化 CRLF，不是本轮引入的空白错误。
- Serena `initial_instructions` 和项目激活成功，实际 Java 概览请求失败，错误为语言服务器初始化 Broken pipe。按仓库规则改用 rg/源码，不宣称语义导航健康。
- 现有包内开发工具清单：`.gradle/project-review-20261023/packaged-dev-tools.json`，含被检查 JAR 的 SHA-256。
- 枚举、扫描与基线：同目录 `inventory.json`、`static-audit.json`、`evidence-files.json`、`git-status-start.txt`。这些是本机辅助证据，长期结论以本报告为准。
- 本地 GTNL 的 `gradle.properties` SHA-256：`273ce5be2ef4a846af7b2a242d495f3689dfafdd1c815d96c89ddb4e1de940fc`；`MachineLoader.java`：`f09c351e5357597dc3b906eab6da7d33062268f90d4ec506439e83028deb19ac`。它们用于识别对照文件，不替代未知的 Git 提交号。

本轮没有得到“全项目运行无缺陷”或“某优化可提高多少性能”的证据。审查及建议整理已完成，上述改进尚未实施。
