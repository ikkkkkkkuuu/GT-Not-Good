# 宇宙之心验证

## 设计基线

固定上游为 Dragonators/GTLAdditions `34fb152886164c56a07fc9d1b10e056f0c22b7ab`。
完整保留 121×112×109 原版结构、37,144 个非空位置及空格任意方块条件。
StructureLib 控制器偏移为 `(62,88,15)`；黑洞位于主机上方 36 格、后方 39 格。
朝向 SOUTH/NORTH/WEST/EAST 的世界偏移分别为 `(0,36,-39)`、`(0,36,39)`、
`(39,36,0)`、`(-39,36,0)`。目标点对应原 JSON 的 `(列62,行59,切片54)`，位于中央空腔。

每套燃料消耗一根奇异湮灭燃料棒和一个黑洞种子，提供 `Long.MAX_VALUE × 81920` EU，
并保留 60% 概率产出 64 个湮灭约束器。能量使用 BigInteger，按任务累计进度之差逐 tick 记账，
避免整数除法丢失尾数。主机所有者的 GT 无线网络接收发电；实体能源仓承担维持耗电。
基础维持耗电为每台运行机器每 tick 一次 64A LuV（2,097,152 EU/t），能耗模块降低该数值。
维护仓不是成形条件；旧维护仓可保留为可选结构位置。维修故障及效率惩罚禁用，维持耗电仍由实体能源仓提供。

2026-10-22 免维护定向实机验证通过：无维护仓结构成形、旧维修 NBT 恢复、完整燃料能量与进度持久化正确，
维持电不足时仍暂停；结果为 `build/heart-maintenance-qa/result.txt` 的 `PASS`，Gradle 成功。

机器内置普通核心。附加执行核心共享输入、独立燃烧，普通核心分摊公共并行额度，
高级和完美核心使用各自的并行额度；并行模块上限为 `1＋参数`，仍接受原生功率面板限制。
普通/高级/完美核心基础释放时间为 1200/300/1 tick，即普通 60 秒、高级 15 秒，完美仍为 1 tick。
秒数按 20 TPS 计。速度和超频模块缩短释放时间，向上取整、最低 1 tick；
发电专用超频释放倍率为 `timeReduction × 4 / powerIncrease`，低速无损/无损/奇点分别为 4/8/16，
普通核心无其他加速时对应 300/150/75 tick；速度参数 2 单独安装时对应 600 tick。
这些模块不增加每套燃料总能量，发电核心也不使用加工机器的无线电预付/退款语义。
同类参数仓限一个；执行核心可重复安装。已提交燃料、核心身份、进度、剩余能量和待输出产物由主机保存。
无维持电、手动停机、核心禁用/移除或结构失效时暂停；恢复后继续已有任务。

原图、结构编码及获取配方说明见 `reference/heart-of-the-universe-port/STRUCTURE.md`、`ITEMS.md`；
打包许可及来源记录见 `META-INF/heart-of-the-universe-port/`。

## 单元与资源检查

```powershell
.\gradlew.bat compileJava processResources
.\gradlew.bat test --tests '*Heart*' --tests '*ModularProcessingLogicTest' --tests '*ModuleWorkTest'
.\gradlew.bat spotlessJavaCheck assemble
```

检查范围：

- `HeartOfTheUniverseStructureTest`：全部符号数量、逐格源数据 SHA-256、MBS 编码、控制器偏移和独立数组副本。
- `HeartGenerationRulesTest`：精确维持耗电、三类核心与模块的释放时间、超 long 总能量和保存恢复后的守恒。
- `HeartOfTheUniverseRecipesTest`：两项燃料真实消耗、60%/64 件副产物、可运行配方标志及种子四帧动画。
- `HeartBlackHoleMathTest`：球面绕序、原始世界半径、位置种子与倾斜范围、第一/第三人称相机射线原点。
- 素材清单原图哈希、141 张 CTM、零邻接图与原图像素一致；控制器顶面/正面的工作状态与动画。

## 隔离客户端

仅检查取消维护仓要求后的真实结构与发电：

```powershell
.\gradlew.bat --no-configuration-cache -PheartMaintenanceOnly=true -I scripts/heart-qa.init.gradle runClient17
```

该模式验证无维护仓成形、旧维护仓兼容、损坏维修 NBT、随机维修检查及真实燃料满效率发电；
保留维持电不足暂停和 NBT 恢复后的燃料总能量守恒检查，不重跑渲染巡检。
世界与结果分别位于 `run/heart-maintenance-qa/`、`build/heart-maintenance-qa/result.txt`。

```powershell
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = ($previousJavaToolOptions + ' -Djdk.net.unixdomain.tmpdir=C:/Windows/win.ini').Trim()
    .\gradlew.bat --no-configuration-cache -I scripts/heart-qa.init.gradle runClient17 --no-daemon
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
}
```

当前 Windows 主机临时指定非目录的 Unix 域套接字路径，使 JDK 回环通信使用 TCP；该设置只作用于
本次进程，不修改 `win.ini`。QA 的主代码和测试编译输出分别隔离至 `build/heart-qa/main-classes/`
与 `test-classes/`，避免其他 Gradle 进程覆盖常规 `build/classes/`。

客户端使用 `scripts/qa/HideFactoryClient.ps1`，仅控制本次工厂 QA 标记的进程，保持静音、透明、
鼠标穿透并隐藏任务栏图标。不要同时运行其他工厂 QA，也不修改正常游戏客户端的窗口或声音。
独立世界位于 `run/heart-qa/`，结果写入 `build/heart-qa/result.txt`，截图在其 `screenshots/` 子目录。
仅结果文件以 `PASS` 开头、进程正常退出且 Gradle 成功时，才能记录客户端通过。

## 历史客户端验证：调整前的 20/5/1 tick

以下 PASS 属于基础释放时间调整前的移植版本，不表示当前 1200/300/1 tick 已通过客户端验证：

- 四个水平朝向的完整实体结构成型，正确识别输入总线、输出总线、维护仓、实体能源仓和模块仓。
  核对黑洞中心与原空腔对应、384 格渲染距离及控制器外的完整渲染边界。
- 使用已注册真实燃料配方，恰好扣除一组燃料。真实能源仓每工作 tick 仅扣一次 64A LuV；
  主机所有者无线余额增加对应 BigInteger 份额，T8 能耗模块将维持耗电降至 524,288 EU/t。
- 缺维持电时进度与无线发电同时暂停，复电续烧；真实 TileEntity NBT 恢复不重复扣料、
  不重复发电，完整燃料任务的总入账恰好等于原始能量。
- 真实普通/高级/完美核心分别独立按 20/5/1 tick 完成；多核心恰好消耗相应燃料组数，
  不预扣无线余额，总入账守恒。速度/三种超频模块分别验证 10/5/3/2 tick，并行和节能模块保持燃料总能量。
- 九个真实完美核心能轮询越过每轮八任务启动预算；核心禁用、移除及对应位置重装后正确暂停和续烧。
- 确定性副产物在输出堵塞时保留，跨 NBT 恢复、解除堵塞后只结算一次，不重复无线入账。
- GLSL 1.20 真实编译并执行七 pass；视界、吸积盘、体积细节和模糊合成正常，结构遮挡尊重世界深度。
  已检查 NORTH/EAST 远景、近景、遮挡、停机和重启视角；绘制返回时恢复 GL/FBO 状态。

尚未覆盖完整 shaderpack 兼容矩阵、多人服务器压力、资源重载、全部第一/第三人称视角、运行中破坏结构，
以及本机 GUI 和中英文普通/Shift tooltip 的专项截图核对。这些项目保留待验证，不包含在历史 PASS 内。

## 历史验证记录（时长调整前）

2026-10-22：离线素材检查核对全部 1,477,168 个原始坐标、控制器偏移、33 个原始资源哈希、
141 张 CTM 哈希及零邻接像素。调整前的 `spotlessJavaCheck`、定向测试与 `assemble` 均为 `BUILD SUCCESSFUL`；
25 项测试、0 失败、0 错误：渲染数学 4、结构 4、发电规则 4、物品配方 2、输入事务 6、模块任务 5。

隔离客户端 `runClient17` 正常退出，耗时 2 分 5 秒，Gradle 为 `BUILD SUCCESSFUL`，
`build/heart-qa/result.txt` 为 `PASS`。上述实体结构、燃料、发电、模块、暂停恢复与渲染检查通过。
该历史普通 JAR 为 10,845,084 字节，已确认包含 Heart 代码、六个 shader 文件、MBS、物品及来源清单，
未打包 `reference` 原始源码。

七张实际截图位于 `build/heart-qa/screenshots/`，已人工检查黑洞、吸积盘、遮挡及停机/重启：

- `north-overview.png`、`north-black-hole.png`、`north-depth-occlusion.png`。
- `east-overview.png`、`east-black-hole.png`。
- `north-stopped.png`、`north-restarted.png`。

## 当前时长调整验证通过

按用户要求将普通核心改为 1200 tick、高级核心改为 300 tick，完美核心保持 1 tick。
总燃料能量、速度/超频倍率及节能规则不变；旧任务仍按已保存的释放时间继续，新的基础时间用于新任务。

2026-10-22：新时长的 `spotlessJavaCheck`、`compileJava`、`processResources`、`test` 和 `assemble`
均为 `BUILD SUCCESSFUL`。普通 JAR 已重新打包，`javap` 确认 `BASE_DURATION=1200`，
双语 tooltip 资源为 60 秒/15 秒/1 tick。28 项测试、0 失败、0 错误：渲染数学 4、结构 4、发电规则 7、物品配方 2、
输入事务 6、模块任务 5。48 种速度/超频组合保持总燃料能量；速度 2 与奇点倍率 16 组合时，
普通/高级核心分别为 38/10 tick。

新一轮隔离客户端正常退出，Gradle 为 `BUILD SUCCESSFUL`，耗时 2 分钟；
`build/heart-qa/result.txt` 为 `PASS`，明确记录 1200/300/1 tick 核心及
600/300/150/75/1200 tick 速度、三档超频和节能模块检查。
完整普通核心燃烧、64A LuV 维持耗电、无线入账、断电、核心禁用/拆除/重装、存档恢复和
堵塞输出结算均在真实实体机器中通过；四向结构、九核心轮询及七 pass 渲染继续通过。

七张截图已于 11:21:43–11:22:06（Asia/Shanghai）刷新，文件名沿用上方截图清单。
此前列出的 shaderpack、多玩家、资源重载及其他未覆盖项仍待验证，本轮未扩大这些验证范围。

## UU 流体配方调整验证通过

按用户要求移除控制器配方的 UU 物质单元，改为 14 项物品、4 项流体输入，
第 4 项为 16384000 mB（16384 桶）原生 UU 物质。该量参考 GT5 5.09.54.205 的
UXV 能源仓 64000 mB ×256，是本模组定制平衡，不保留上游 921600000000 mB 的绝对成本。
原始上游源及其哈希不变；详细依据见 `reference/heart-of-the-universe-port/ITEMS.md`。

2026-10-22：本轮 `compileJava`、`processResources`、`test --tests '*Heart*'` 与 `assemble`
均为 `BUILD SUCCESSFUL`，定向格式化及检查通过；17 项测试、0 失败、0 错误。
普通 JAR 已重新打包，字节码及打包来源清单均核对为 16384000 mB UU。

隔离客户端正常退出，Gradle 为 `BUILD SUCCESSFUL`，耗时 2 分 14 秒，
`build/heart-qa/result.txt` 为 `PASS`。检查确认唯一原生装配线配方有 14 项物品和 4 项流体，
第 4 项为精确 16384000 mB 原生 UU；其余原料数量、2304000 tick、MAX 电压及扫描研究保持不变。
四向结构、1200/300/1 tick 发电核心、模块、暂停恢复、存档、堵塞结算及渲染回归继续通过。

八张截图于 12:47:01–12:47:26（Asia/Shanghai）刷新；新增
`build/heart-qa/screenshots/controller-acquisition-nei.png` 已人工检查，旧 UU 单元槽移除，
第 4 流体槽显示紫色 UU，界面简写为 16ML，精确数量由 QA 校验。
本轮控制器获取检查限于配方注册与原生 NEI 显示，未完整制造控制器。
此前列出的 shaderpack、多玩家、资源重载及其他未覆盖项仍待验证。

## 发电 NEI 说明溢出修复验证通过

发电页改用 `HeartGenerationFrontend`，绘制时按当前语言翻译，科学计数显示每组约 7.56×10²³ EU。
基础 64A LuV 与 2,097,152 EU/t 分行；普通/高级释放时间为 60/15 秒，完美为 1 tick，
并说明加速、超频缩短释放时间且不改变每组燃料总能量。说明及作者署名按 160 像素折行，页面高度为 202。

2026-10-22：本轮 `compileJava`、`processResources`、定向测试及 `assemble` 均为
`BUILD SUCCESSFUL`，17 项测试、0 失败、0 错误。QA 使用独立运行时 JAR 副本，避免并发构建改写。
隔离客户端正常退出，Gradle 为 `BUILD SUCCESSFUL`，耗时 2 分 43 秒，
`build/heart-qa/result.txt` 为 `PASS`，中文和英文均验证实际 GUI 缩放 2 与 3。

四张原生 NEI 截图于 13:10:37–13:10:52（Asia/Shanghai）生成并已人工检查，位于
`build/heart-qa/screenshots/`：

- `generation-nei-zh_CN-gui2.png`、`generation-nei-en_US-gui2.png`。
- `generation-nei-zh_CN-gui3.png`、`generation-nei-en_US-gui3.png`。

每组约 7.56×10²³ EU、64A LuV、2,097,152 EU/t、60/15 秒、完美 1 tick、两条模块说明及作者署名
均完整显示，无横向溢出或底部裁切，切换语言后说明更新；默认加工时间和旧长整数行已移除。
原结构、发电及获取配方显示回归继续通过。本轮未验证全部 TST 配方，未完整制造控制器；
此前列出的 shaderpack、多玩家、资源重载及其他未覆盖项仍待验证。
