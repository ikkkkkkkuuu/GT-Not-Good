# LDLib 正式虚空矿机配置界面

正式控制器：`大型虚空矿机`，保留 ID `28506`、注册名、原配方和 NBT。
已移除独立演示机（28516）的注册及类，旧配置界面实现由新版 LDLib 界面取代。
正式矿机的结构、采矿、能耗、维度槽和主界面均继续使用原实现。

入口保持两层：右键控制器打开原 `LargeVoidMinerGui` 主界面，点击原有齿轮
“配置虚空采矿”按钮进入 LDLib 配置界面。原主界面及原矿机入口不改动。

配置界面使用 LDLib 的 Button、Label、TextField、VirtualScrollerView、Dialog。
单页恢复原版信息布局：左侧 5×5 维度槽和模式说明，右侧矿石列表，底部玩家库存。
显示矿石模式名称、时运增耗、EU/t 与总倍率、维度/过滤增幅、定向 UU 消耗。
列表提供名称搜索、未选择/已选择筛选、权重升降排序、独立列头和悬停完整名称。
原矿模式禁用时运。沿用通配样板符的 SpriteTexture 染色方式和蓝/黄/紫/青/橙色板，
配合文字区分模式与矿石状态；原主界面继续保留。
MUI2 仅承载真实库存槽和网络同步。矿石列表复用原序列化与服务端操作处理器。
`LDLibModularScreen` 将原生控件区域的输入交给 LDLib，维度槽留出透明输入区域，
与下方玩家库存一同交给 MUI2。
确认弹窗打开时拦截库存区域的点击。面板持有同步模型，避免位置工厂二次创建 holder 丢失模型。

## 复现

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat runClient -I scripts/ldlib-miner-qa.init.gradle -PldlibVisibleQa=true --no-configuration-cache
```

本任务用户已明确授权直接启动测试。参数不会隐藏窗口。
脚本使用独立 `run/ldlib-miner-check`，每轮生成临时创造平坦世界，不访问玩家已有存档。
测试类仅在 test 源集，不进入发布包；运行时固定工程输出快照。

测试依次截图原主界面、单页配置、修改后状态、确认弹窗、矿石列表、已选择矿石和搜索结果，
另检查已选择空分类、权重升序及降序滚动末页。
从原主界面的真实配置按钮进入，再通过生产界面事件入口驱动 LDLib 按钮。
服务端确认矿石模式、时运、定向模式、开关、矿石选择、清空选择的结果。
开关的基线在动作前取值，避免未成型机器自动停机造成误判。

日志：`build/ldlib-miner-client.log`；成功标记：`LDLIB_MINER_QA_PASS`。
截图：`build/ldlib-miner-qa/zh_CN-scale2/screenshots`。

2026-09-26 完整两层入口客户端测试通过，输出成功标记且 Gradle 返回成功。
搜索将 64 条矿石缩小到 1 条。编译、资源生成、主代码/测试代码风格检查、
打包和 36 项 LDLib 控件测试通过。发布只保留正式矿机及新版界面，排除测试专用 Mod。

验证边界：真实 Minecraft/Angelica 客户端和集成服务端，事件由测试程序注入。
控制器未搭建完整多方块，不表示已经验证完整采矿产出或跨维度物品运输。
没有移植 Scene。正式接入后的客户端日志为 `build/ldlib-miner-production-client.log`。
