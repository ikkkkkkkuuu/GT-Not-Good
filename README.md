# GT-Not-Good
看名字你应该知道就是抄袭GTNL的模组

本模组大量引用了别人的代码甚至可以说抄袭 代码都是ai跑的

如有违反或让原作者不满 我会立即删除

本项目作自用 大量魔改修改难度


添加自定义任务线  介绍了本mod加了的一些物品方块和机器   加入BetterQuestingAPI这个mod可自动显现!!!
https://github.com/ABKQPO/BetterQuestingAPI


添加了个闪电科技封包功能
神秘时代注魔封包核心
装配线和进阶装配线封包核心
直接给神秘注魔自动化这坨大分杀了  直接给装配线杀了
 



本mod添加了个至尊功能 可以在配置文件关闭
只针对单方块
ae 两个接口对单方块机器下单 直接会在推料之前修改单方块机器里的虚拟编程电路
也就是说类似可编程电路mod  但是不需要额外写一个虚拟编程电路了 一个单方块机器 25种电路配方完全洒洒水啦
遇见bug 即时反馈 

本mod还对单方块机器添加了自己的虚拟模具槽 处于电路槽上方 可参与配方合成


mixin关闭了配方超净间需求

mixin开启了灵魂瓶能装所有 导致屠宰场所有生物可以有产物



### Current support version
| GTNH Version | Start Version | Newest Support Version |                                                            Download                                                            | Maintenance status |
|:------------:|:-------------:|:----------------------:|:------------------------------------------------------------------------------------------------------------------------------:|:------------------:|
| 2.9.0-beta2  |     1.0.0     |         1.0.1          | [![1.0.1](https://img.shields.io/badge/release-v1.0.1-00FF00)](https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/tag/v1.0.1) |         ❌          |
| 2.9.0-beta3  |     1.0.3     |         1.1.5          | [![1.1.5](https://img.shields.io/badge/release-v1.1.5-00FF00)](https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/tag/v1.1.5) |         ✔️         |
|  2.9.0-RC-1  |     1.1.6     |         1.3.1          | [![1.3.1](https://img.shields.io/badge/release-1.3.1-00FF00)](https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/tag/1.3.1) |         ✔️         |

最新版本：[1.3.1 发布说明](https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/tag/1.3.1)。
下载：[游戏用 JAR](https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/download/1.3.1/gtnotgood-1.3.1.jar) · [源码 JAR](https://github.com/ikkkkkkkuuu/GT-Not-Good/releases/download/1.3.1/gtnotgood-1.3.1-sources.jar)。

### 大型矿石处理机

保留原有粉末和宝石产量，按原生筛选配方补齐缺失产物，覆盖原矿、粗磨矿、洗净矿和下界/末地富矿。
例如方钍石保留 8 个原粉，并追加独立的钍粉（6%、3%）和钍-232粉（1%）；富矿产量翻倍，概率不变。
按实际材料匹配配方，避免石墨等矿石通过矿辞分组继承其他材料的产物。
已核对当前开发环境的 67 种材料、580 条输入配方，详见 [矿石筛选产物与验证说明](docs/testing/ore-sifting.md)。

### 电路样板总成（ME）

支持 **900 个样板槽**，每个样板独立缓冲 **64 种物品和 64 种流体**，每种上限 **2⁶³−1**。
将不同编程电路的处理样板放进同一个总成，总成自动读取各自电路，AE 下单不需要提供实物编程电路。
已注册到接口终端，名称随连接的多方块主机配方图更新，支持无线二合一接口终端上传与取回样板。
界面只保留手动物品槽，不提供虚拟模具槽、手动电路配置和批量写电路按钮。
GT 每次读取的物品与流体数量仍受 `int` 限制，消耗后从 `long` 缓冲继续补充。
存档、拆除、放回和退料保留完整数量，ME 暂时拒收的材料会保留并重试退回。

无线二合一接口终端新增“保留电路与虚拟模具物品”开关，默认关闭。
开启后，NEI 导入处理配方时保留不消耗的编程电路，以及当前虚拟模具列表中允许的物品；其他不消耗物品仍跳过。
允许列表直接使用现有虚拟模具数据，后续更新自动生效；含流体配方仍使用 GTNH 原生至尊编码样板。

### 无线电网 HUD

每位玩家均可使用，默认按 **P** 开关，不需要物品或饰品；可在控制设置中改键。
默认按 **Home** 进入位置编辑，左键拖动整块 HUD；再次按 Home、Esc 或“保存并退出”保存位置。
编辑界面可恢复默认位置，关闭 HUD 时也能预览调整；编辑按键可在控制设置中修改。
显示所属 GT 无线电网的电量、实时净 EU/t、最近五分钟平均净 EU/t，以及安培数和电压等级。
每 100 tick 同步一次，首次开启先采样；关闭后停止请求。

使用 `/gtngtexteffects wireless` 打开滚动颜色预览，或绑定“配置无线 HUD 滚动颜色”按键。
配色、特效、粗体与斜体独立保存，不影响机器“添加者”署名。
`config/GTNOTGOOD/gtnotgood-wireless-monitor.cfg` 可配置 `Speed`、`Palette`、`CustomPalette`、
`AnimatedColors`、`AnimateValues`、`Scale`、`XOffset`、`YOffset` 和科学计数法；文件中的手动修改重启客户端生效。
默认整行动态颜色；设置 `AnimateValues=false` 后仅标题滚动，净功率保留绿增红减。
默认放在左下方，可与 GT-Simple-Wireless-Network 同时安装，各自的开关与配置独立。

### 🔌 ME 库存控制

1.2.8 新增阈值输出总线、双阈值发信器、ME 自动请求器与自动请求终端，支持物品和 GTNH 原生流体。
阈值总线按网络保留量输出，双阈值发信器在上下限之间保持状态，请求器通过 AE 合成 CPU 自动补齐目标库存。
四个界面使用上游原贴图、布局和交互；流体以 L 输入和显示，1000 就是 1000 L，不做桶单位换算。
大量放置时采用资源监听、休眠、通知合并和每网限额队列。详细用法与测试范围见 [ME 库存控制说明](docs/ME_STOCK_CONTROLS.md)。


### 🔌 ME 网桥 / ME Bridge
<p align="center">
    <img src="README/mebe1.png" width="800">
</p>

<table align="center">
    <tr>
        <td align="center" width="50%"><strong>ME 网桥发起端</strong></td>
        <td align="center" width="50%"><strong>ME 网桥接收端</strong></td>
    </tr>
</table>

ME 网桥由发起端和接收端组成。
- **发起端**：连接 ME 网络，并可在 GUI 中配置频率(可设置中文)。
- **接收端**：通过GUI相同频率与发起端建立连接。
- 支持跨维度连接。
- 不占用 AE2 Channel。

<p align="center">
    <img src="README/mebe2.png" width="800">
</p>

ME 无线收发器
可直接右键空气打开频率选择界面，然后直接 shift + 右键，相关 AE2 节点即可直接连接。实在是太超模啦

### 🔌 ME无线二合一接口终端/ ME Wireless Dual Interface Terminal
<p align="center">
    <img src="README/me1.png" width="800">
</p>

<p align="center">
    <img src="README/me2.png" width="800">
</p>

如图所示 是一个集成编码 库存 接口终端为一体的无线二合一接口终端4
自动填充nei配方时可直接在接口搜索栏里自动填入配方名称 
优化过不会出现 搜索栏里是组装机 4  结果组装机 24排在比组装机 4更上面的情况
可自动填充nei配方后 如果有和搜索栏相同的接口 直接会放入样板在里面

NEI → GTNotGood 新增“优先使用本模组接口自动命名”，默认“否”。
开启后，超级样板输入总成等使用自己的自动名称，忽略 GTNL 等模组追加的命名后缀，保留电路、手动槽和模头编号。
各玩家独立保存设置，切换后重新打开接口终端生效。

### 🔌 Xnet

<p align="center">
    <img src="README/Xnet.png" width="800">
</p>

完全抄袭Xnet


### 🔌 ME容器/ ME Container

<p align="center">
    <img src="README/me3.png" width="800">
</p>

添加me容器 可以配置物品或流体 然后从连接的ae网络里抽取 同步显示数量  然后可以被外部访问抽取

### 🔌 通量网络 / Flux Network

<p align="center">
    <img src="README/fl.png" width="800">
</p>

<p align="center">
    <img src="README/fl1.png" width="800">
</p>

<p align="center">
    <img src="README/fl2.png" width="800">
</p>

x

如图所示 抄袭

通量网络由发起端和接收端组成。
- **发起端**：连接通量网络，并可在 GUI 中配置频率(可设置中文)。
- **接收端**：通过GUI相同频率与发起端建立连接。
- 支持跨维度连接。




先写到这
