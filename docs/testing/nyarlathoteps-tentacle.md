# 奈亚拉托提普之触验证

当前验证结果以 `docs/development/progress.md` 为准；本页描述实际检查入口，不将计划视为通过。

初始化脚本 `scripts/tentacle-qa.init.gradle` 把任务输出放在 `D:/CodexTools/GTNGSuprachronalPort/build`，不修改依赖版本或默认构建路径。离线编译、资源与两项结构测试：

```powershell
& scripts/dev/Invoke-Gradle.ps1 -GradleArguments @('-I','scripts/tentacle-qa.init.gradle','--project-cache-dir','D:/CodexTools/GTNGSuprachronalPort/project-cache','spotlessApply','compileJava','processResources','spotlessCheck','test','--tests','*NyarlathotepsTentacleStructureTest','--offline','--console=plain')
```

结构测试核对原165×48×49全部坐标的独立哈希、空格语义、29,045 个非空位置、原 424 个 Q 线圈、62 个 A 舱位、控制器偏移及形状缓存隔离。

隔离客户端使用 `-PtentacleClientQA runClient17 --no-configuration-cache`，并采用上方相同初始化脚本。客户端保持静音、透明、穿透和隐藏任务栏，只通过 `HideFactoryClient.ps1` 操作本任务标记的进程。测试源 `scripts/qa/TentacleClientChecks.java` 不进入发布 JAR。

实机检查完整结构、十四类构件获取方、四种参数仓、三种执行核心、多个能源仓、可选数据仓及朝向失败路径。实际原生装配配方检查无研究介质启动、控制器任务 NBT 重载、完成后恰好输出一次。截图核对结构外观与缺失贴图；日志、result.txt 和 screenshots 均在独立输出目录。

构件来源、性能与配方适配见 `reference/nyarlathoteps-tentacle-port/PORT.md`。本次没有移植 GTO 独有电路、无人机与终极电池支线；超时空池明确使用原生 UIV 及以上配方。

## 初始移植结果（研究/印刻行为已被下方需求取代）

最终编译、资源、格式、两个结构测试和 assemble 退出 0，30 秒；`final-build.log`。最终隔离客户端退出 0，1 分 23 秒；`client-final.log` 和 `build/tentacle-qa/result.txt` 为 PASS。实际原生装配配方为铱强化机械方块，检查完整 NBT 重载和精确产出；电路装配也验证印刻拒绝、实际加工与精确产出。移除研究介质后缓存配方被重新拦截，输入未消耗。

已查看无云结构全景与实际模块化 GUI；三模式通过真实 CycleButtonWidget 操作切换，客户端与服务端一致。首轮截图未拍到机器，没有将其算作视觉通过，后来已修正相机并重跑。最终四张截图位于 `build/tentacle-qa/screenshots/`。

发布形态 JAR 内 52 原始资源、611 CTM、282 派生动画元数据共 945 项哈希匹配，未包含 QA 类或 reference；详见独立输出的 `artifact-audit.json`。构建中使用当前完整工作区，未安装到用户整合包或发布。

## 超时空配方池与无序输入

用户后续明确要求本机不需要闪存、顺序输入或电路印刻。专项夹具为
`scripts/qa/TentacleRecipeClientChecks.java`，初始化脚本为 `scripts/tentacle-recipes-qa.init.gradle`。
输出独立放在 `D:/CodexTools/GTNGTentacleRecipes/build`，避免与运行渲染 QA 共写目录。

```powershell
& scripts/dev/Invoke-Gradle.ps1 -GradleArguments @('-I','scripts/tentacle-recipes-qa.init.gradle','--project-cache-dir','D:/CodexTools/GTNGTentacleRecipes/project-cache','-PtentacleClientQA','spotlessApply','runClient17','--offline','--no-configuration-cache','--console=plain')
```

夹具覆盖完整无数据仓结构、逆序物品/流体、缺料无损拒绝、缓存重复、同一模式的低级/UIV 配方、电路无印刻、原生电路池不变、
控制器完整 NBT 恢复及精确产出；样板总成使用真实 ME 供电、Ultimate Encoded Pattern 原生流体 NBT 与 pushPattern，
并在没有普通输入总线和流体仓时检查两种装配模式。NEI 检查原版 4×4 布局、十二物品和四流体完整显示、
无特殊槽、唯一装配分类包含所有原生配方、电路池，以及 GUI 比例 1/2/3。另检查旧模式 0/1 合并、旧电路模式 2 迁移、再次保存恢复和两模式循环。执行结果以 progress.md 和 result.txt 为准。

最终专项客户端已通过：退出 0、1m49s，`D:/CodexTools/GTNGTentacleRecipes/client.log`。
五张实际 NEI 截图均已查看；无闪存、乱序输入、原版样板总成两模式加工、原生电路池仍需印刻及精确产出均通过。
初轮空电路池问题已通过延迟到 GT 原生配方回调末尾导入修复；完整失败与重试记录见 progress.md。

随后按用户纠正合并两个装配分类。`merge-client.log` 退出 0、1m55s，确认唯一装配分类包含全部 453 条原生配方，
低级和 UIV 同模式完成、原生样板总成投料、无印刻电路及旧模式 NBT 迁移全部通过。已查看控制器和 UIV 的实际 NEI 图。
同一初始化脚本加 `-PtentacleGuiOnly` 运行独立 GUI 场景，输出为 `build/tentacle-gui-qa`；`merge-gui.log` 退出 0、1m15s，
实际按钮的装配/电路两模式切换和两端同步通过，两张截图已查看。此 GUI 场景没有重复执行加工或运行渲染测试。
最终 `merge-build.log` / `merge-repeat.log` 编译、资源、格式检查均通过，重复构建 20 项 UP-TO-DATE 且配置缓存复用。

## 运行渲染复核

新增完整待机、运行、停机以及同视角近景截图。检查 B/D/E/Q/X 的 2671 个位置：服务端检查原生
GTCoilTracker 激活记录，客户端检查收到同步后的 metadata；不能要求服务端也保存客户端发光 metadata。
E 位包括之前遗漏的 154 个超能核心。原生配方的研究、输入事务、NBT 恢复和精确产出仍先独立执行。

为了避免一 tick 任务的启停边界影响截图，只在隔离 QA 存档里将一份已通过原料/研究检查的任务时长延长为
20000 tick，再由正常服务端 tick 推进并供电。生产配方和机器时长代码没有为拍照修改。暂停通过实际控制器
disableWorking 触发；验证停机后的激活记录释放和客户端外观恢复。该夹具不用于证明真实配方处理时长。

动画检查覆盖七组原始动画及 CTM 共 289 张播放贴图：原始关键帧、每段时长、循环长度、逐 tick alpha 和
清单哈希。真实客户端确认渐变 CTM 已加载进图集；`animation-comparison.gif` 为源资源时序重放与派生播放贴图
对照，不是原 GTO 客户端录像。原版本来源、发光/泛光的差异和限制见 PORT.md。

最终客户端 `render-client-final.log` 退出 0、1m53s，PASS 记录正常推进 204 个服务端 tick；完整运行/停机、同视角
近景和三模式 GUI 共八张截图已保存。最终编译/资源/格式/结构测试/assemble 为 `render-final-build.log`，退出 0、15s，
两项测试无失败。`render-artifact-audit.json` 核对 1523 项资源/元数据与最终 JAR 一致，原 945 项加上 578 项渐变播放资源。
