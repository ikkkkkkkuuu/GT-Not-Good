# GTNG 开发导航

- Minecraft 1.7.10 / Forge / GTNH addon；保留现有依赖、存档、注册 ID、资源路径与网络兼容性。
- 开始任务先读 `docs/development/progress.md`，用 Git 状态核实记录；结束或中断前更新目标、证据、剩余工作，不能把计划写成已完成。
- 修改 Java、资源、构建或发布前，必须读取 `docs/development/project-rules.md` 的完整规则；本文件是导航，原有规则继续有效。
- 保持 `build.gradle.kts` 对齐 starter；自定义逻辑放 `addon.gradle`，依赖与仓库各归其文件。不擅自升级依赖。
- 翻译在 Java 使用位置声明 `#tr`，不直接编辑 `.lang`；tooltip 沿用 GTNH 风格并核实实际行为。
- 普通 late Mixin 在 `LateMixinsLoader#getMixins` 按 loadedMods 注册；early Mixin 才直接写 JSON。
- 仅操作代理启动的 QA 客户端，静音、透明、穿透、隐藏任务栏；遵循 `scripts/qa/HideFactoryClient.ps1` 的作用域。
- “提交并更新”授权完整发布流程；版本末位逢十进位、README 下载链接与发布资产一致，中文说明覆盖上次发布以来全部有效改动。详见完整规则。

## 按需工作流

仓库 Skills 位于 `.agents/skills/`（每项读取对应 `SKILL.md`）：

- `gtng-multiblock`：多方块结构、配方与机器生命周期。
- `gtng-mixin`：注入点、可选模组与两端加载验证。
- `gtng-modularui`：界面布局、服务端交互、同步和视觉 QA。
- `gtng-upstream`：固定上游版本、许可证、移植与来源记录。
- `gtng-build`：Gradle、定向测试、缓存和 Windows 诊断。

工具配置与实测见 `docs/development/tooling.md`。优先用 rg 定位文本，语义引用查询仅在 Serena 健康时使用；不把文本匹配当成完整调用图。不为普通任务自动创建 worktree。

Serena 可用时，先 `initial_instructions` 获取 session，再 `activate_project` 激活当前仓库；不能仅凭全局 MCP 已登记判断 Java 导航健康。
