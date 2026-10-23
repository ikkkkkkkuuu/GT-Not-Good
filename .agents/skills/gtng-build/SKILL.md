---
name: gtng-build
description: 构建、测试或诊断 GTNG Gradle 性能、缓存、Java 和 Windows 构建问题。
---

先读 docs/development/project-rules.md 和 docs/development/tooling.md。
使用 gradlew.bat，保留 wrapper、GTNH、Forge 与插件版本。Windows 优先 scripts/dev/Invoke-Gradle.ps1，临时设置 daemon 所需 Unix-domain socket 短路径，不改系统 Java。
先 git diff --check；Java 改动执行 compileJava，资源或翻译执行 processResources；按功能选 src/test 或现有 scripts/*qa.init.gradle；networkRegression 仅覆盖可编程网络。
格式化用 spotlessApply，再 spotlessCheck；修改 formatter 时第二次 --rerun-tasks 应不产生差异。结构检查用 scripts/qa/CheckProjectStructure.ps1。
先重复相同任务核实 UP-TO-DATE 与 configuration cache reused。不要惯用 clean、--refresh-dependencies、--rerun-tasks；缺失缓存才去掉 --offline。runClient/Server 已声明不兼容配置缓存，不能据此全局关闭缓存。
本地 build cache 只对可缓存任务有效；不标记未知任务 cacheable、不强制编译增量，先检查 Mixin/Jabel 注解处理器。并行 worktree 各用独立 build/run，不并发写同一工作区。
将命令、退出码、计时、日志路径及验证范围写入进度；失败不得记为通过。
