# GTNG Codex 工具评估

当前 Java 构建策略已迁移为 JDK 25 + JVM Downgrader，基础字节码为 Java 8，配置 17/21/25 多版本目标；实测与命令见 [Java 开发说明](java.md)。下文 Jabel 和默认堆大小相关描述属于工具评估时的历史基线。

评估日期：2026-10-23。基线提交 c70251eb，开始时工作区干净。没有修改游戏 Java、资源、依赖或发布版本。

## 环境和选型

Windows 11 10.0.26200，约 47 GiB 内存；Codex CLI 0.163.0-alpha.11（桌面应用随附 CLI，未升级）。PATH 为 Oracle JDK 25.0.2，另有 JDK 21。主代码 1315 个 Java 文件，测试 142 个文件。
Gradle wrapper 9.3.1；GTNH settings convention 2.0.20、Elytra conventions 1.2.1；Minecraft 1.7.10、Forge 10.13.4.1614，manifest 2.9.0-RC-2，Jabel 模式。所有版本保持原状。

首选现有 rg、Gradle wrapper 和仓库 Skills；仅新增 uv/Serena 这一套语义导航候选，不安装重复文件系统、Git、搜索或记忆 MCP。

## Skills 与规则

`.agents/skills/` 提供 multiblock、mixin、modularui、upstream、build 五个专项流程。Codex 按任务加载，避免所有流程进入每轮上下文。现有全局 gtnh-development 保持不变，项目特有约束以仓库 Skill 为准。
原 AGENTS.md 原文完整迁入 project-rules.md，并逐字对照 Git HEAD 验证。根 AGENTS 仅保留高风险约束、进度恢复和导航；进行相关修改前仍须读完整规则，不能借精简删减约束。

## 持久进度

新会话读 progress.md，然后核对 git status、HEAD 和相关 diff。只记录用户目标、范围、验证命令/退出码/日志、证据、剩余工作；完成后用最终状态替换过期任务。长期机制结论保存在主题文档，避免把聊天历史或猜测当记忆。
日志放忽略的 .gradle，长期可恢复结论写入本目录并随项目版本控制。这里不是后台自动记录器，代理在结束或中断前维护记录。

## Gradle

现有 configuration-cache 与 parallel 已开启；新增 org.gradle.caching=true，启用本地任务输出缓存。未设置远程缓存、强制增量、worker 数、堆大小或新的插件。
Windows 的默认 JDK 25 在本机启动 daemon 会报 Unable to establish loopback connection。scripts/dev/Invoke-Gradle.ps1 仅对子进程添加 -Djdk.net.unixdomain.tmpdir=C:/Windows/Temp，并恢复原 JAVA_TOOL_OPTIONS；不改系统 Java 或用户 Gradle 配置。可传已有参数：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 compileJava processResources --offline --console=plain
```

预处理翻译任务已声明 Java/校验脚本输入和 lang 输出；不改生成机制。Mixin 注解处理器生成 refmap/SRG，Jabel 参与编译，不凭普通 Java 项目经验强制增量。相同输入的 compileJava/processResources 已验证 UP-TO-DATE；这证明无需重复执行，不能证明每种源码改动都能增量编译。
runClient/Server 的运行时快照任务已显式不兼容配置缓存，保留原行为；开发编译不因此关闭配置缓存。避免无故 clean 和 refresh-dependencies。

实测：

| 场景 | 外部耗时 | 结果 |
| --- | ---: | --- |
| 原环境直接编译/资源，离线 | 1.67 s | 失败：daemon loopback |
| 相同任务加 socket 参数，首次建立配置缓存 | 11.70 s | 成功，11 项 up-to-date |
| 相同任务重复执行 | 1.15 s | 成功，配置缓存 reused |
| 加 build cache 首次运行 | Gradle 报告 24 s | 成功，4 executed / 7 up-to-date |
| 缓存启用后 compileJava/processResources/spotlessCheck | Gradle 报告 2 s | 成功，20 up-to-date |

以上缓存冷热、任务集合不同，不能把 11.70→1.15 s 全部归因于新配置。本地 build cache 未做删除输出后的 FROM-CACHE 实验；未证明其单独收益。
日志：.gradle/codex-baseline-build.log、codex-baseline-fixed.log、codex-warm-build.log、codex-cache-build.log、codex-verification.log。
结构检查通过：1456 Java locations、3040 project imports、152 mixins、170 reserved mixin types、270 QA source paths。

## 定位与上下文基准

可重复执行 scripts/dev/benchmark-navigation.py。任务：定位 getMixins 并读出 Railcraft 可选模组 guard，两种方式断言相同结论；交替执行各 5 次。

| 方法 | 中位耗时 | 检索和上下文 UTF-8 字节 |
| --- | ---: | ---: |
| 全源码 rg + 整个 loader 文件 | 25.134 ms | 12872 |
| Skill 路径指引 + 定向读取 | 7.126 ms | 2861 |

减少约 71.6% 本机检索时间、77.8% 读取字节。包含本机子进程启动，不包含 MCP/模型网络延迟、推理、编写和验证；不是整项开发任务提速承诺，也不是 Serena 前后对比。原始采样见 .gradle/codex-navigation-benchmark.json。

## Worktree

已有主目录 main 与 codex/interface-recipe-name-release 的另一个 worktree。适合真正独立、长时间且会改动同一批文件的任务；本次不新增。
每个 worktree 的 build、.gradle、run、Serena 缓存独立；可复用用户 Gradle 依赖下载缓存。不能共享客户端目录或并发写同一构建输出；新目录有首次反混淆和索引成本。默认继续使用当前工作区。

## 官方依据

- [Codex Skills 与仓库发现](https://learn.chatgpt.com/docs/build-skills)
- [Codex MCP 配置](https://learn.chatgpt.com/docs/extend/mcp?surface=cli)
- [Serena 官方安装](https://oraios.github.io/serena/02-usage/010_installation.html)
- [Serena Codex 集成](https://oraios.github.io/serena/02-usage/030_clients.html)
- [Serena Java 语言服务器实现与配置](https://github.com/oraios/serena/blob/main/src/solidlsp/language_servers/eclipse_jdtls.py)
- [uv 官方安装](https://docs.astral.sh/uv/getting-started/installation/)

## Serena 安装验证与启用条件（首次尝试记录）

选择官方维护的 oraios/serena（2.1.0 为 GPL-3.0-or-later；先前 MIT 判断已更正），PyPI 官方包 serena-agent 2.1.0，官方推荐 uv tool install -p 3.13 serena-agent。无需 API key 进行 LSP 导航；不安装第三方同名 MCP。uv 0.13.5 已经通过官方 Windows 安装器安装至用户 .local/bin；备用 pip 安装的重复 uv 已卸载。

uv 下载的 CPython 3.13.17 已用 --version 验证。默认 minor-version 链接步骤报 Missing expected target directory，指定已经下载的完整 python.exe 后成功完成 77 个依赖的解析；随后 PyPI wheel 下载多次超时，当前不能宣布 Serena 安装成功。没有修改用户 Codex 配置，没有注册不可用 MCP；不影响现有其他项目的 MCP。

Java 后端采用 JDTLS，需 Java 21+。已查官方实现：默认 VSIX 模式会下载带 JRE 的语言服务器，且可能使用自己的 Gradle；本项目若继续启用，应明确 gradle_wrapper_enabled=true，使用当前 wrapper 9.3.1，禁用 Maven 导入和后台 autobuild，并核实 GTNH/Jabel/Mixin 与 Buildship 导入诊断。可以复用现有 JDK；不得为语言服务器升级游戏依赖。尚未下载或验证 JDTLS，所以无法给出 Serena 代码导航前后实测。

后续恢复安装时复用现有缓存与解释器：

```powershell
& "$env:USERPROFILE/.local/bin/uv.exe" tool install --python "$env:APPDATA/uv/python/cpython-3.13.17-windows-x86_64-none/python.exe" serena-agent
```

官方提供 serena setup codex；先查看 --help 及拟写配置，备份原配置，再集成。当前推荐 stdio 与 codex context；桌面会话未必以仓库为 cwd，应在会话启动后显式激活此仓库。Windows 用可执行文件绝对路径避免旧桌面进程 PATH 未刷新。不额外安装提醒 hooks，保留 rg 等工具供文本查询。

scripts/dev/check-serena.py 是待环境就绪后使用的只读 MCP 验证器（120 秒工具超时，自动关闭子进程）；需用 Serena 隔离环境的 Python 执行。它执行 initialize、activate_project、initial_instructions、两次真实符号正文查询与引用查询，并将结果写入 .gradle/codex-serena-smoke.json。脚本已做语法检查，尚未完成端到端执行；仅 smoke 通过也不能替代桌面新会话 /mcp 验证。配置与实际能力都确认后才将状态改为已启用，并单列冷启动/热查询耗时，不与 rg 字符串匹配混为等价能力。

五个 Skill 均已使用系统 skill-creator 的 quick_validate.py 实际校验通过。验证器所需 PyYAML 复用了 uv 已下载缓存，没有额外安装一套工具。Python 脚本编译与 PowerShell 语法解析均通过；Gradle 包装脚本已执行成功。最终重复构建报告 809 ms、11 up-to-date、configuration cache reused；git diff --check 通过。

安装证据：.gradle/codex-serena-install.log 记录 sniffio 下载三次重试后超时（125.5 s）；低并发重试日志为 codex-serena-install-retry.log，仍停留在 pywin32/Pillow 下载后主动停止，未留下后台安装。uv 与成功下载的 Python/包缓存保留供恢复。未启用 Serena，所以当前会话没有可调用的 Serena 工具。

## Serena 后续安装（2026-10-23）

serena-agent 2.1.0 已通过官方 uv tool install 安装成功，复用现有 CPython 3.13.17 与缓存。uv tool list、serena --version、init、MCP initialize、initial_instructions 和 activate_project 已执行成功。
Serena 2.1 的 activate_project 需要 initial_instructions 返回的 session ID；验证器已适配 MCP 2.2 的 is_error 属性，并明确选择 tools 接口。
已用 Codex 官方 CLI 将绝对路径 serena.exe 注册到用户 config.toml；修改前备份为 ~/.codex/config.toml.before-serena-20261023，其他 MCP 配置保留。参数为 codex context、tools interface、project-from-cwd，关闭自动打开浏览器与 GUI 日志窗口；仅 MCP 子进程设置 socket 临时路径。
项目配置为 .serena/project.yml；机器路径在忽略的 project.local.yml，缓存同样被忽略。Serena 只信任本项目精确路径，以读取语言服务器设置。新桌面会话中先 initial_instructions 获取 session，再 activate_project 激活当前仓库。
默认 JDTLS VSIX 下载受 GitHub 超时影响，改用官方支持的 upstream JDTLS 模式；使用系统已有 JDK 25、已有 Lombok 1.18.42 和项目 wrapper。C 盘发现空间耗尽后，仅将本次不完整下载和新建语义缓存移到 D:/CodexTools/Serena，未清理用户其他内容。独立 JDTLS 的最终语义查询结果以下续实测记录为准。

JDTLS 已从 Eclipse 官方下载并解压：jdt-language-server-1.64.0-202610221949.tar.gz，SHA-256 为 `657ceee9655bf8342dbf29386d0e40281750e40d047e3656b1b743b51ac2ce77`。该快照仅用于独立导航工具，不参与 Mod 构建或打包。语言服务器目录 D:/CodexTools/Serena/jdtls-1.64.0-202610221949。
空间处理：隔离环境最终保留在默认 APPDATA/uv/tools/serena-agent，以保证 uv tool list / upgrade 能正确识别，CLI 已复测成功；本次下载的 uv 包缓存迁到 D:/CodexTools/Serena/uv-cache，C 盘保留少量元数据。以后升级可用 `uv --cache-dir D:/CodexTools/Serena/uv-cache tool upgrade serena-agent` 复用缓存。项目缓存迁到 D:/CodexTools/Serena/GT-Not-Good-cache，并保留 .serena/cache junction。C 盘仍有 JDTLS workspace/shared index；未移动用户原有 Gradle 缓存或游戏内容。
首次成功 Java find_symbol 调用含 Gradle 首次导入耗时 109.916 s，同会话第二次 0.102 s；查到 LateMixinsLoader/getMixins 的真实正文。引用查询需要完整的泛型类符号名 `GTNGMultiBlockBase<T extends GTNGMultiBlockBase<T>>`，短名会和两个构造方法歧义；验证器已据服务器返回结果修正。第一次导入采样保存在 .gradle/codex-serena-first-import.json，最后完整复测结果见 .gradle/codex-serena-smoke.json。

### 最终验证结果

安装、Codex stdio 配置与独立 Java 导航验证完成。最终 MCP 检查退出码 0、passed=true：initial_instructions 0.102 s，activate_project 0.060 s，find_symbol 首次含 Gradle 同步 97.750 s、重复 0.102 s，find_referencing_symbols 6.904 s，找到 19 个文件中的实际引用（包括 AssemblerMatrix）。取样包含 MCP 本机协议开销，不是整个开发任务提速证明。相比 rg，Serena 的收益是符号与引用关系；首次导入和跨会话重新同步的成本明显，不宜用它替代简单文本搜索。

JDTLS 导入期间曾报告 VM 元数据 XML 提前结束（磁盘耗尽后的首次缓存残留）；最新保存的 VM 设置已有完整 JavaSE-25 XML，符号与引用查询最终成功。本次仅证明选定查询正常，不宣称所有 Java 诊断、跨模组引用与语义编辑均无兼容问题。日志中的 clientInfo 兼容告警未阻止查询。

Codex 用户配置已登记 enabled=true。桌面当前会话不能即时获得新 MCP 工具列表；需新开会话或重启应用，再用 /mcp 确认 Serena。激活前读 initial_instructions，获取 session ID。外部 Python MCP 检查已经成功，但尚未替代用户桌面新会话的最后确认。

最终路径：工具与 Python 在默认用户 uv 目录，JDTLS 与下载缓存/项目语义缓存在 D:/CodexTools/Serena；未改全局 Gradle/JDK、游戏代码、Mod 依赖、资源与已有 worktree。原配置备份保留。安装日志为 .gradle/codex-serena-install-resume.log；真实查询证据为 .gradle/codex-serena-smoke.json 和 codex-serena-smoke.log；复测脚本 scripts/dev/check-serena.py 使用 Serena 的隔离 Python 执行。
