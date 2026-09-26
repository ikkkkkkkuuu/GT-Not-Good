# LDLib 扩展控件验证

范围：ProgressBar、Slider、SearchComponent、Menu、Dialog、TreeList、SplitView、
VirtualScrollerView、ColorSelector、GraphView。Scene 按用户要求排除。

## 自动化验证

`ExtendedControlsTest` 覆盖拖动越界、步长、静默 setter、模态焦点隔离和恢复、
嵌套弹窗、拥有者移除、万行虚拟列表、模型缩短、滚动条定位、分栏最小尺寸、
缩放锚点、节点移动与连线清理、树节点归属与键盘导航、搜索、ARGB 往返、进度边界。

运行：

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat test --tests 'com.xyp.ldlib.*' processResources checkstyleMain
```

## 客户端验证

当前状态：编译、资源生成、36 项 LDLib 自动化测试、主代码/测试代码风格检查、打包均通过。
已检查发布包包含十个控件与来源清单，排除 Scene 和测试专用 Mod。
2026-09-26 已完成实际客户端渲染验证：英文默认缩放（实际 3 倍）10 个阶段，中文 2 倍缩放 16 个阶段。
两轮均输出 `LDLIB_CONTROLS_QA_PASS`，共 26 张截图已逐张检查。

项目的 `docs/testing/rts-lifecycle.md` 记录了用户要求在桌面 2 运行图形测试客户端，
不在当前任务栏暴露窗口。先安排测试桌面，或者取得本次当前桌面运行的明确许可。
用户在本任务中明确要求“开始直接测试”，已授权本轮直接启动。此参数不会隐藏或移动窗口。

```powershell
.\gradlew.bat -I scripts/ldlib-controls-qa.init.gradle runClient -PldlibVisibleQa=true --no-configuration-cache
```

测试使用独立 `run/ldlib-controls-check` 目录，不打开世界。测试专用 Mod 位于 test 源集，
不进入发布包。测试会打开生产演示页，通过生产 UIInput 分发鼠标/键盘事件，检查原生文本框编辑与 GL 错误，
保存十六张截图到 `build/ldlib-controls-qa/zh_CN-scale2/screenshots`，输出 `LDLIB_CONTROLS_QA_PASS` 后退出。
可通过 `-PldlibQaLanguage=en_US -PldlibQaScale=3` 选择其他语言和缩放。
运行时固定工程输出快照，避免同目录其他任务编译覆盖正在验证的代码。

已检查：数值四向填充、搜索弹层、菜单、弹窗、树形分栏、虚拟列表最后一页、
HSV 与透明度、节点画布及缩放后连线、完整 LibraryDemoScreen 中文侧栏。
客户端断言另覆盖：空搜索、输入中文物品名后筛选唯一结果、回车关闭搜索、子菜单动作仅执行一次、
确认弹窗回调、树节点选择、分隔条拖动、HSV 点击、ARGB 文本输入、节点拖动、中键平移和 Home 适配。
一万条数据的末尾页面只实例化 8 个行控件。

证据：`build/ldlib-controls-client.log`（首轮）和 `build/ldlib-controls-client-zh.log`（中文扩展轮）。
首轮游戏检查通过，但 Gradle 最后因 QA 快照闭包不能序列化而返回失败。脚本已把此测试任务标记为
不兼容配置缓存，第二轮退出码为 0、`BUILD SUCCESSFUL`；上述命令显式关闭该次运行的配置缓存。
英文首轮截图保留在 `build/ldlib-controls-qa/screenshots`，中文截图保留在独立子目录。

验证边界：使用真实 Minecraft/Angelica 渲染和生产事件分发，鼠标键盘事件由测试程序注入，
并非操作系统层面的物理输入回放；未打开世界或测试任何服务端机器同步。
