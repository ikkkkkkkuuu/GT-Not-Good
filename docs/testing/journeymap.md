# JourneyMap 右键菜单

基线为 GTNH 2.9.0-RC-1 的 JourneyMap 5.2.23-fairplay。此功能不新增 JourneyMap 编译或运行时硬依赖。

## 行为

- 全屏地图右键打开“传送到此处 / 新建导航点”，菜单贴近点击位置并限制在屏幕内。
- Esc、再次右键或点击菜单外关闭菜单；其他按键关闭菜单后继续交给 JourneyMap。聊天框、工具栏保持原交互。
- 使用原生 `GridRenderer.getBlockUnderMouse` 换算物理鼠标坐标，不自行猜测 GUI 缩放和地图缩放。
- 点击已有导航点时使用其完整 X/Y/Z；原生 getter 和 `Waypoint.at` 保留下界坐标换算。
- 地表模式检查同列落点，洞穴或导航点在首选高度上下 16 格查找；需要完整支撑、两格空气并排除已知危险材质。
- 区块未加载时不强行读取或加载区块。确认界面显示将使用的高度及未知地形风险，取消或 Esc 返回地图。
- 发送前再次检查世界、玩家、维度、权限和可用落点，传送到方块中心，避免整数边界落点。

## 客户端与服务端

客户端读取自身 `config/GTNOTGOOD/gtnotgood.cfg` 的 `JourneyMap.enableContextMenu`，默认开启，修改后重启。
客户端无 JourneyMap 时不注册相关 Mixin；专用服务端即使存在同名模组标记也不加载该集成。
启动时只读取类字节核对 API，遇到不兼容版本或原 `jmquicktp` 菜单时跳过集成并记录一次日志。

单人游戏沿用 JourneyMap 的作弊权限检查。客户端连接独立服务端时，仅发送原有 `/tp` 指令，由服务端处理权限与最终移动。
本地双端分离同样如此，不通过新增网络包或客户端直接改位置实现传送；本功能不要求服务端安装 JourneyMap。
客户端落点检查仅反映当前已同步的地形，无法保证服务端执行命令时地形未改变。

## 验证入口

单元测试：`JourneyMapLandingTest`、`JourneyMapSideIsolationTest`。
实际客户端：`gradlew.bat -I scripts/journeymap-qa.init.gradle runClient17 --no-configuration-cache`。
无 JourneyMap：追加 `-PjourneyMapAbsent`。可用 `-PjourneyMapJar=<path>` 指定另一份 JourneyMap。
QA 使用独立临时世界与工作目录，并通过 `HideFactoryClient.ps1` 将测试窗口隐藏、透明化且设为鼠标穿透。
截图及结果位于 `build/journeymap-qa`；无模组运行位于 `build/journeymap-qa-absent`。

独立双端：`gradlew.bat -I scripts/journeymap-remote-qa.init.gradle runJourneyMapRemoteQA --no-configuration-cache`。
该入口在 `127.0.0.1:25576` 启动无 JourneyMap 的独立服务端，再启动装有 JourneyMap 的客户端。
先核对非 OP 的权限错误与位置不变，再仅为临时 QA 玩家授予 OP，验证两端实际落点；结束撤销 OP 并关闭两个进程。
工作目录为 `run/journeymap-remote-server-qa`、`run/journeymap-remote-client-qa`，结果位于 `build/journeymap-remote-qa`。

## 已执行检查（2026-10-16）

- 格式检查、Java 编译、资源生成、`assemble` 和项目结构检查通过。
- 10 项单元测试通过，包括落点策略和未安装模组/服务端的类加载隔离。
- JourneyMap 5.2.23-fairplay 实际客户端 162 项检查通过，覆盖 GUI 缩放 1/2/3、地图缩放 0/2/4、负坐标、四角菜单、导航点完整坐标与下界倍率、原生编辑器、实际传送、未知地形确认取消、跨维度、配置关闭和单人权限拒绝。
- 不安装 JourneyMap 的实际客户端正常启动，4 项检查确认没有定义集成类或注册其 Mixin。
- 两个独立 Java 21 进程的本地双端测试通过。服务端未安装 JourneyMap；非 OP 收到权限拒绝，40 个服务端 tick 内两端位置不变。临时授予 OP 后，菜单经原版 `/tp` 到达 `(-32.5, 71.0, -46.5)`，服务端位置与客户端脚部位置一致；结束时撤销权限并正常关闭测试进程。
