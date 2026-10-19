# 大型虚空钻机客户端验证

控制器：`LargeVoidDriller`，机器 ID `28530`。

沿用虚空矿机的 7×9×7 钢制钻塔，以流体输出仓替代输出总线。使用拥有者无线电网，
基础功耗 480 EU/t、周期 400 tick；四级超频依次为 100、25、6、1 tick，每级功耗乘 4。
25 个维度槽全部留空且未选巨行星时使用本地维度；四个巨行星按钮可与维度物品同时选择。

每周期按权重选择一批流体。地下流体沿用 GT 配置中的批次产量范围，海洋固定 1000 L，
巨行星沿用原生虹吸配方的批次数量。每维度总权重为 1，同维度的地下流体和海洋各占一份；
巨行星的各深度配方等权抽取。过滤模式排除已选流体，定向模式仅抽取已选流体并在周期开始时
消耗 UU 物质。更多维度和更窄的选择提高功耗，具体倍率与产量范围显示在终端中。

夹具使用独立 `run/void-driller-qa` 目录，每次创建临时创造平坦世界。测试类仅由
init 脚本加入测试源集，并通过独立 class 目录加载，不进入发布包。

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat compileTestJava -I scripts/void-driller-qa.init.gradle --no-configuration-cache
.\gradlew.bat runClient17 -I scripts/void-driller-qa.init.gradle --no-configuration-cache
```

`runClient17` 在此脚本中使用 Java 21。可使用 `-PvoidDrillerNoAngelica` 诊断渲染兼容问题；
常规验证保留 Angelica。语言和缩放可通过 `-PvoidDrillerQaLanguage=en_US`、
`-PvoidDrillerQaScale=2` 切换。

Windows 运行时复用 `scripts/qa/HideFactoryClient.ps1`，仅处理命令行同时含当前工程路径和
`-Dgtng.factory.qa=true` 的测试 Java 进程，使窗口透明、穿透且不显示在任务栏。
独立 options 文件与每 tick 的主音量设置均将 Minecraft 静音。本夹具不启动浏览器或媒体进程，
不操作用户正常游戏窗口。

夹具通过真实注册控制器与 GT 原生仓室搭建完整 7×9×7 结构，不放置能源仓。
地下流体使用临时的原生 `GTUODimensionList` 配置：主世界水 1000 L、熔岩 2000 L，
地下权重为 1:3；生产池同时读取主世界固有水源。测试结束恢复原配置。
这使产量与概率检查不依赖整个整合包的油品注册顺序。

自动检查覆盖：

- 原生结构检查、真实 tick 完成产液周期、原生输出仓接收流体及无线耗电。
- 480 EU 精确扣款、余额不足时保持电量，五档完美超频与不改变当前周期。
- 过滤、定向模式及选择的 NBT 往返，25 个维度槽及普通物品拒绝规则。
- 真实下界维度物品第 25 槽 NBT 与流体选择；四巨行星的气体种类及数量对照原生虹吸配方表。
- 木卫二、天仓五e、土卫六与巴纳德C的已注册海洋流体均为每批 1000 L。
- 四个巨行星开关的 NBT 与无效索引保护；真实 GUI 开启木星后显示气体，关闭后恢复本地流体。
- 不支持维度、空定向、UU 不足、满输出仓保护，以及清理条件后的恢复。
- 待输出 1000 L、仓内仅余 500 L 空间时，三次重试及 NBT 重载均不部分写入；排空后恰好转出一次。
- 待机更改过滤清除未启动的流体选择；运行中更改配置保留本周期流体与进度快照。
- 右键打开主界面，再通过真实齿轮按钮进入 475×365 配置界面。
- 服务端确认超频、定向、流体选择与清空；真实 LDLib 搜索和确认弹窗。
- 原生中文文本、英文注册名搜索及退格编辑；兼容输入副本不重复插入或删除。
- 配置界面的 25 个维度槽和 36 个玩家库存槽。

成功文件：`build/void-driller-qa/{zh_CN,en_US}-scale2/result.txt`，首行为 `PASS`。
日志成功标记为 `VOID_DRILLER_QA_PASS`。截图在同目录的 `screenshots` 下：
`primary.png`、`configuration.png`、`directional-selection.png`、
`clear-confirmation.png`、`cleared.png`、`fluid-search.png`、`gas-dimensions.png`。
任一断言失败写入 `FAIL` 与堆栈，并使 Gradle 任务失败。

验证边界：测试事件由夹具注入，使用真实 Minecraft 客户端、集成服务端及生产 GUI。
当前开发环境的 GT jar 内含 GTNEIOrePlugin 与 GTNH Intergalactic；脚本补充与现有工厂 QA
相同的开发运行依赖。夹具直接使用其原生维度物品与气体配方表。
跨维度选择不要求加载目标世界。检查覆盖原生维度数据与产量，实际 tick 产液在主世界完整结构中执行。

2026-10-19 验证通过：中文与英文客户端均完成全套检查，54 项相关回归测试通过，
Checkstyle 与项目结构检查通过。构建记录见 `build/void-driller-verification.log`；
客户端日志为 `build/void-driller-client.log`、`build/void-driller-client-en.log`。
