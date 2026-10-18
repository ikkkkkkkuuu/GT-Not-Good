---
navigation:
  title: AE 总线、接口与终端
  parent: index.md
  icon: gtnotgood:advanced_io_bus
  position: 30
  recommend: 10
  keywords:
    - 高级 IO 总线
    - 阈值输出
    - 双阈值发信器
    - 库存 IO 接口
    - 大容量二合一接口
    - 无线终端
    - 自动请求器
    - 通配样板
    - 网桥
item_ids:
  - gtnotgood:advanced_io_bus
  - gtnotgood:threshold_export_bus
  - gtnotgood:threshold_level_emitter
  - gtnotgood:stock_io_interface
  - gtnotgood:stock_io_interface_part
  - gtnotgood:large_interface
  - gtnotgood:large_interface_part
  - gtnotgood:requester_terminal
  - gtnotgood:me_requester
  - gtnotgood:wireless_dual_interface_terminal
  - gtnotgood:WildcardPattern
  - gtnotgood:pattern_sorter
  - gtnotgood:me_container
  - gtnotgood:blockMEBridgeSender
  - gtnotgood:blockMEBridgeReceiver
  - gtnotgood:MEWirelessTransceiver
  - gtnotgood:wireless_frequency_card
---

# AE 总线、接口与终端

本章介绍本模组的 AE 总线、接口、终端，以及库存访问、自动补货和无线连接工具。先按需要选择设备，再设置库存目标、样板或网络连接。

- [总线与接口如何选择](#io-buses)
- [高级 IO 总线](#advanced-io-bus)
- [阈值输出总线](#threshold-export-bus)
- [双阈值发信器](#threshold-level-emitter)
- [库存 IO 接口](#stock-io-interface)
- [大容量二合一接口](#large-dual-interface)
- [自动请求器与请求终端](#requesters)
- [无线二合一接口终端](#wireless-terminal)
- [通配样板符](#wildcard-pattern)
- [样板分类工具](#pattern-sorter)
- [ME 容器](#me-container)
- [ME 网桥](#me-bridge)
- [ME 无线收发器](#me-wireless-transceiver)
- [无线频率卡](#wireless-frequency)
- [通量物流插头](#flux-logistics)
- [常见问题](#troubleshooting)

以下 3D 预览可旋转、缩放，悬停彩框可查看操作标注。场景用于展示设备及朝向，未模拟正在工作的 ME 网络；实际使用仍须供电、频道、线缆、存储及机器条件。

### 线缆上的三种控制部件

<a name="buses-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/ae-buses-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    高级 IO 总线按相邻容器的库存补足目标，并将未标记产物回收至 ME。中键样本设置数量。
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    阈值输出总线按 ME 网络库存决定是否向外输出。中键样本设置网络阈值。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    双阈值发信器按库存上下限输出红石，区间内保持原状态。右键设置样本和阈值。
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.7 1.5" text="相邻库存：高级 IO" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="3.5 0.4 1.5" text="ME 库存：阈值输出" color="#FF88CC88" maxWidth="115" />
  <TextAnnotation pos="5.5 2.7 1.5" text="双阈值：红石控制" color="#FFFFD166" maxWidth="115" />
</GameScene>

图中省略了目标容器。总线和发信器均从安装面朝外工作，实际使用时应紧贴目标设备。

<a name="io-buses" />

## 先选对总线或接口

以下设备都需要 ME 供电和一个可用频道。线缆部件朝向紧贴的相邻方块工作，安装前先确认机器对应面的物品与流体访问设置。

| 设备 | 适合的用途 |
| --- | --- |
| ME 高级 IO 总线 | 将相邻机器的标记原料补至目标量，并回收其他资源 |
| ME 阈值输出总线 | 按 ME 网络库存阈值，向相邻容器输出资源 |
| ME 双阈值发信器 | 按 ME 库存上下限控制红石，避免在单一阈值附近频繁切换 |
| 库存 IO 接口 (ME) | 让受支持的 GT 单方块直接使用标记的 ME 原料与燃料 |
| ME 大容量二合一接口 | 存放大量样板，派发处理任务并接收回流的物品与流体 |

<a name="advanced-io-bus" />

## ME 高级 IO 总线：维持机器库存

1. 将总线安装在线缆上，朝向目标机器或容器，右键打开界面。
2. 用物品或装有流体的容器设置样本。样本不会被消耗；流体容器会标记其中的流体。
3. **中键点击样本**，输入希望机器保有的数量，点击“设置”或按回车确认。物品按个、流体按界面显示的 L 设置。
4. 将实际资源存入 ME，确认目标面允许输入；需要回收产物时，也要允许从该面输出。

总线会从 ME 补足标记资源，始终将**未标记资源**送回 ME。“将超出目标的库存送回 ME”默认开启，用于回收标记资源的超出部分；关闭后仍会回收未标记资源。

例如标记铁锭为 64：少于 64 时补足，多于 64 时按超量回收开关处理；未标记的加工产物会送回 ME。它检查的是**机器内的库存**，不是 ME 中的保留量。

默认 **18 个样本槽**，每张容量卡增加 9 个，最多 **63 个**；有 8 个升级槽，支持加速、容量和红石卡。安装红石卡后，可选择忽略信号、有信号运行或无信号运行。该总线遵循机器连接面的访问限制，不会替你切换机器电路。

<a name="threshold-export-bus" />

## ME 阈值输出总线：只输出指定库存范围

1. 将总线朝向接收资源的容器，右键设置物品或流体样本。
2. **中键点击样本**设置阈值，点击“设置”或按回车确认。
3. 在左侧选择输出模式：

| 模式 | 实际行为 |
| --- | --- |
| 仅输出 ME 库存中超出阈值的部分 | ME 有 1000 个、阈值为 800 时，最多输出多出的 200 个 |
| ME 库存不高于阈值时输出 | ME 库存低于或等于阈值时继续向外输出；高于阈值时暂停 |

阈值读取的是 **ME 网络库存**，不会按接收容器的库存补货。“不高于阈值时输出”可能继续耗尽 ME 中的剩余资源，不是自动请求合成。

默认 **9 个样本槽**，每张容量卡增加 18 个，最多 **63 个**；有 8 个升级槽，支持加速、容量和红石卡。样本精确区分物品或流体种类，容量卡移除后超出的行暂停工作。

<a name="threshold-level-emitter" />

## ME 双阈值发信器：设置库存回差

1. 在线缆上安装发信器，右键标记**一种**物品或流体。
2. 填写“下限”和“上限”，保证下限不大于上限。
3. 在左侧选择低库存发信或高库存发信，将发信器朝向需要红石控制的设备。

- **低库存发信**：库存低于下限时开启，达到上限时关闭。
- **高库存发信**：库存达到上限时开启，低于下限时关闭。
- 两个阈值之间保持原状态；有信号时强度为 15。掉电、缺频道、未设样本或下限高于上限时不发信。

例如下限 100、上限 200，选择低库存发信：低于 100 时启动补货，达到 200 时停止，随后库存落回 100–199 时保持停止。发信器只提供红石控制，实际补货设备需另行连接。

### 库存供料与样板派发

<a name="interfaces-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gtnotgood:stock_io_interface" x="1" y="1" z="1" meta="5" nbt="{stockIOTargetSide:5}" />
  <Block id="gregtech:gt.blockmachines:281" x="2" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="gtnotgood:large_interface" x="5" y="1" z="1" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    库存 IO 接口：在 GUI 选择目标方向，标记物品与流体，让受支持的 GT 单方块直接使用 ME 原料。
  </BlockAnnotation>
  <BlockAnnotation pos="2 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    LV 挤压机示例：接口紧贴其西面，机器仍须供电、选择正确电路及模具并满足原配方条件。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    大容量二合一接口：900 张样板，向目标派发物品与流体；不提供库存标记补充。
  </BlockAnnotation>
  <LineAnnotation from="1.7 2.1 1.5" to="2.5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="库存 IO：直接供料" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="5.5 2.8 1.5" text="大容量：样板派发" color="#FF88CC88" maxWidth="115" />
</GameScene>

蓝框库存 IO 接口紧贴 LV 挤压机，示例目标方向为东；在 GUI 中确认目标。橙色大容量二合一接口用于派发样板任务，产物须另行送回 ME。

<a name="stock-io-interface" />

## 库存 IO 接口：直接从 ME 使用原料

它有整方块和线缆部件两种形态，功能相同。适用于受支持的 **GT 单方块加工机器和燃料发电机**，不向机器预填库存，也不以处理样板派发任务。

1. 将接口紧贴目标机器并接入 ME。整方块在界面中点击方向按钮，确认“目标”显示正确机器；线缆部件直接朝安装面工作。
2. 打开“物品”或“流体”页，设置配方原料或燃料样本；两页**各有 900 个标记**，样本不消耗。
3. **右键样本**设置“ME 保留量”和“单次检查可用量”。按需要启用“限制”或“固定”，保持“运行”开启。
4. 存入实际原料，正常配置机器电路、供电和输出。机器按原配方加工，只从 ME 扣除实际消耗量。

| 开关 | 用法 |
| --- | --- |
| 限制 | 从 ME 总库存扣除设定保留量，只向机器提供剩余部分 |
| 固定 | 每次配方或燃料检查只提供设定数量；不足整份数量时等待 |
| 回收 | 将目标 GT 机器允许从连接面取出的物品和流体输出送回 ME |

例如 ME 保留量 1000、单次检查可用量 64，开启“限制”和“固定”：ME 至少有 1064 个时才提供 64 个供本次检查，但配方只用 1 个就只扣 1 个。

“自动标记”可根据 ME 中的现有资源填充空标记；右键其按钮设置刷新间隔和最低库存。需要手动编辑标记时，先关闭该页的自动标记。机器仍须满足原配方的电压、洁净室和其他要求；若不能回收产物，检查目标的输出面、覆盖板及 ME 空间。

### 接口的线缆部件形态

<a name="interface-parts-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/ae-interface-parts-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    库存 IO 接口部件：与整方块功能相同，目标方向固定为安装面朝外。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    大容量二合一接口部件：与整方块一样容纳 900 张样板，仅朝安装面派发。
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.8 1.5" text="库存 IO 接口部件" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="5.5 2.8 1.5" text="大容量接口部件" color="#FFFFD166" maxWidth="115" />
</GameScene>

两类接口都有整方块和线缆部件形态。部件按安装面朝向工作；普通右键部件打开对应界面。

<a name="large-dual-interface" />

## ME 大容量二合一接口：集中管理 900 张样板

整方块与线缆部件都能存放 **900 张样板**，以 9 列滚动显示，也可通过接口终端管理。

1. 将接口接入 ME，并朝目标机器投料。线缆部件只向安装面投料；整方块可用扳手调整投料方向。
2. 右键打开接口，放入处理样板。含流体的处理配方使用**终极编码样板**。
3. 在终端请求产物。机器的物品和流体产物需要送入接口，或通过其他回收路径返回 ME。
4. 按需要调整左侧的阻挡模式、样板优化等设置；合成锁定等升级功能需要对应卡片。

它保留原版二合一接口的样板派发与产物回流功能，**不支持标记资源并维持库存**。库存供料请使用前面的库存 IO 接口或高级 IO 总线。

界面的倍增按钮会修改接口内所有可处理的处理样板：左键加倍、右键减半，Shift 可一次调整更多倍数；受样板数值和可整除条件限制。普通合成样板会跳过。

### 集中设置自动补货

<a name="requester-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/ae-requester-terminal-layout.snbt" />
  <Block id="appliedenergistics2:tile.BlockController" x="3" y="1" z="1" />
  <Block id="gtnotgood:me_requester" x="5" y="1" z="1" meta="2" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    自动请求终端：普通右键管理同网已加载请求器，不是无线手持终端。
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    ME 网络示意。自动补货需要实际存储、可用样板、配方机器和合成 CPU。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    自动请求器：右键设置最多 5 个库存目标，每单量为 0 时按缺口下单。
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="终端：集中配置" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="5.5 2.8 1.5" text="请求器：库存目标" color="#FF88CC88" maxWidth="115" />
</GameScene>

自动请求终端用于配置同网请求器，请求器负责下单。箭头表示管理关系；实际连接仍需 ME 线缆。

<a name="requesters" />

## 自动请求器与自动请求终端

<ItemImage id="gtnotgood:me_requester" /> **ME 自动请求器**负责下单补库存；<ItemImage id="gtnotgood:requester_terminal" /> **ME 自动请求终端**集中查看和修改同网已加载请求器。请求终端是安装在线缆上的部件，不是无线手持终端。

### 设置一个库存目标

1. 放置请求器并接入 ME 网络。每个请求器占用一个频道，需要网络供电；产物必须有可用合成样板及相应机器，并有能承接订单的合成 CPU。
2. 右键请求器，在最多 **5 行**中标记物品或流体。标记不消耗样品；流体可用对应流体容器设置。
3. 填写目标量和每单量，点击该行右侧的应用按钮或按 Enter 提交。**每单量 0**表示按当前缺口自动计算；大于 0 时，每单最多补该数量。两项接受 0 至 2^63 − 1 的整数，流体按界面标注的 L 计量。
4. 勾选该行启用。目标量为 0 或取消勾选时，不再为该行发起新的补货订单。

请求器用“目标量 − ME 现有库存 − 正在合成或计算的数量”计算缺口，产物完成后回到 ME 存储，不进入请求器本地。每单量只限制单次订单，不是库存上限，也不是必须凑齐才合成的批量。

同网请求器最多同时进行 **2 次配方计算**，不会因此把正在执行的合成任务限制为两单。状态条可查看现有量、在途量，以及缺材料、无配方、等待 CPU、离线等状态。关闭一行、修改目标或拆下请求器，不会自动撤销已经交给合成 CPU 的订单；需要停止时到合成状态界面取消。

### 集中管理

将自动请求终端装在线缆上并使其接入有效网络，普通右键打开。可搜索请求器名称或所标记物品名称，滚动查看各行，直接修改样品、目标量、每单量与启用状态。请求器改名后会按名称分组；改名后的物品放置时沿用名称。

只有同一网络中已加载、有效的请求器会列出；远程编辑还须具备网络建造权限。它不会加载远处区块，也不会展示另一个独立 ME 网络中的设备。右键空鼠标或 Shift 点击样品格可清除标记。

<a name="wireless-terminal" />

## ME无线二合一接口终端

<ItemImage id="gtnotgood:wireless_dual_interface_terminal" />

把 ME 库存、接口样板列表和样板编码放在同一界面，适合边查库存、边录入配方、边安装样板。界面左侧为库存，中间为接口列表，右侧为编码区；合成终端按钮可切换到无线合成终端视图，再切回继续编辑。这里的合成、编码和接口页面都来自同一个手持终端。

### 连接与打开

1. 打开 ME 安全终端，将无线终端放入右侧上方的绑定槽，再从下方槽取出并充电。
2. 在同一维度、同网有效无线接入点的覆盖范围内右键使用。网络须能正常工作，物品存取和合成仍受网络权限及实际资源限制。
3. 也可使用“打开二合一接口终端”快捷键，默认 **F**；“发送手持物品到网络”默认 **C**。可在控制设置中修改；终端可放在背包或饰品槽。

按 NEI 配方安装量子桥接卡或无限增幅卡后，可解除距离及维度范围限制；安装能量卡后可免除终端自身的电量消耗。两种升级作用独立，都不会提供缺失的网络存储空间、合成材料或合成 CPU。

### 从 NEI 配方到接口样板

1. 在右侧空白样板槽放入空白样板，通过 NEI 的配方转移按钮导入配方，先核对输入、输出和数量。
2. 工作台合成使用 3×3 有序布局；处理样板使用可滚动的 4×4 视图，输入、输出各最多 256 项。支持选择合成样板或处理样板模式。
3. 导入处理配方时，可切换“合并相同项”“流体优先”和“保留电路和虚拟模具”。最后一项仅把支持的不消耗电路与模具按 1 件导入，其他不消耗物品不会因此全部加入样板。
4. 点击编码按钮生成样板。**Shift + 点击编码**会编码并移到玩家物品栏；**Ctrl + Shift + 点击编码**会将空白槽中整叠样板编码为相同配方。
5. 要直接装入接口，先确认中间列表高亮了合适的空样板格，再 **Alt + 点击编码按钮**。目标格必须仍为空、允许该样板，鼠标须为空。普通点击接口样板格可取放样板，Shift 点击可移入物品栏。

带可替换材料的 NEI 配方应先选择需要的材料变体；可在对应原料处用 Shift + 滚轮切换。含流体的处理配方使用当前版本的终极编码样板，核对显示的流体与数量后再安装。

接口列表保留搜索和筛选功能。**Ctrl + 点击接口条目的选项按钮**可尝试传送到该接口附近；没有安全落脚点时会失败。**Alt + 点击该选项按钮**切换接口在终端中的显示状态。这些操作针对当前终端实际列出的接口。

<a name="wildcard-pattern" />

## 通配样板符：按材料展开处理样板

<ItemImage id="gtnotgood:WildcardPattern" />

一张通配样板符可按材料规则提供多张具体处理样板，适合大量材料共用相同工艺的情况。它不是把任意机器配方自动补齐，也不会凭空产生材料。

1. 手持右键打开配置。左侧标签依次切换预览、IN 输入、OUT 输出和材料过滤。
2. 在 IN 与 OUT 页添加组件：**单一**固定某个物品或流体；**标签**选择 `ingot`、`plate` 等材料前缀；**流体**按当前材料的熔融、液态、气态或等离子态生成流体。每侧最多 6 个组件，数量范围为 1 至 2^31 − 1。
3. 设置材料名称、属性或标记过滤，可切换白名单与黑名单；最多 64 个过滤组件，多个条件须同时满足。名称支持 `*`、`?` 通配；先从少量材料开始，避免展开不需要的工艺。
4. 点击**保存**，在预览页检查展开数量和每个材料的实际输入输出。预览页可搜索；排除按钮会将当前材料加入黑名单并保存。
5. 将配置好的通配样板符放入支持展开的 ME 接口或样板输入设备。一个模板占一个物理样板格，网络看到的是展开后的具体处理配方。

缺少任意所需物品形态或流体的材料会跳过。展开只证明材料组件能生成，不保证目标机器存在该配方、数量正确或达到电压要求；安装前仍应按 NEI 核对。需要工作台有序合成时使用普通合成样板。

<a name="pattern-sorter" />

## 样板分类工具：整理背包中的已有样板

<ItemImage id="gtnotgood:pattern_sorter" />

1. 把需要整理的样板放在自己的普通物品栏或快捷栏，手持工具右键。
2. 在“机器／配方类型”中选择样板对应的配方类型，查看电路／模具分组及组内预览。
3. 清空鼠标，点击**按组整理**；也可选中一组后点**此组排到前面**。预览编号先按主物品栏顺序，快捷栏排最后。

工具只交换原本放着样板的格子，保留样板数量和数据，不搬动其他物品、不占用空格，也没有内部样板仓库。无法唯一确定电路／模具的样板会归入“有歧义”“未匹配／不支持”“工作台合成”或“无效／空白”等组，不会猜测后改写配方。它不会整理 ME 接口内部的样板，也不需要连接 ME 网络。

### 网桥与网络库存入口

<a name="network-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gtnotgood:blockMEBridgeSender" x="1" y="1" z="1" />
  <Block id="gtnotgood:blockMEBridgeReceiver" x="3" y="1" z="1" />
  <Block id="gtnotgood:me_container" x="5" y="1" z="1" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    ME 网桥发起端：接入主 ME 网络，右键设置名称频道。
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    ME 网桥接收端：在目录中点击目标行的 C 按钮连接。T 是传送按钮。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    ME 容器：右键标记物品与流体，各 36 个样本槽，再用管道或漏斗存取。
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="3 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.7 1.5" text="发起端：设置名称" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="3.5 0.4 1.5" text="接收端：C 连接" color="#FFFFD166" maxWidth="115" />
  <TextAnnotation pos="5.5 2.7 1.5" text="ME 容器：标记取料" color="#FF88CC88" maxWidth="115" />
</GameScene>

发起端与接收端按名称建立无线网桥，接收端附近仍需实体线缆接入设备。ME 容器是库存存取入口，不能充当无线绑定工具。

<a name="me-container" />

## ME 容器：把网络库存交给漏斗和管道

<ItemImage id="gtnotgood:me_container" />

ME 容器向外提供已标记的物品与流体，也能把外部输入存回 ME。用线缆接入已供电、有可用频道的网络；它占用 **1 个频道**，所有面都可接线和存取。

1. 右键打开 GUI，选择物品或流体页。每页有 **36 个样本槽**。
2. 在左侧设置样本，可从 NEI 拖入，或拿着物品、流体容器点击；样本不消耗。鼠标为空时左键清空。
3. 让漏斗或管道从容器抽取已标记的资源。右侧显示 ME 网络库存，悬停查看完整数量，流体单位为 mB。

每个物品标记会预取最多一组实物，受该物品堆叠上限约束。更换标记后，原有实物仍可抽走，取完后再补充新物品。流体按管道请求直接从 ME 抽取，不预留本地流体罐；断电、断线或缺频道时不能继续从网络取料。

外部存入的资源不受这些抽取标记限制。物品暂时无法入网时保留在输入缓存；流体只接受网络实际能存入的部分。生存模式拆下方块会保留标记及实际缓存，不会打包整个 ME 库存。

<a name="me-bridge" />

## ME 网桥：按名称共享网络

<ItemImage id="gtnotgood:blockMEBridgeSender" /> <ItemImage id="gtnotgood:blockMEBridgeReceiver" />

1. 将**发起端**接到主 ME 网络，右键输入一个未被其他发起端占用的频道名，按回车或关闭界面保存。
2. 在远端放置**接收端**并接上当地设备。右键打开频道目录，点击目标行的 **C** 按钮连接；也可在右侧填写相同频道名并保存。
3. 等待连接稳定，检查接收端的连接状态和设备的供电、频道。点击“断开”可清空接收端的频道。

一个发起端可连接多个接收端，支持跨维度；两端所在区块都须保持加载。频道名全服共用，最长 **64 字符**。网桥合并两端 ME 网络，不会生成额外频道；网桥按致密节点承载频道，连接线缆和设备仍受各自容量与 AE 规则限制。不要把两个各自独立的控制器网络随意接在一起。

目录中的 **T** 按钮会尝试将玩家传送至对应发起端，需目标维度已加载且附近有安全落点；它不是连接按钮。

<a name="me-wireless-transceiver" />

## ME 无线收发器：直接绑定 ME 节点

<ItemImage id="gtnotgood:MEWirelessTransceiver" />

这个工具沿用**网桥发起端的名称频道**，可将支持的 ME 设备节点直接接入发起端网络。

| 操作 | 结果 |
| --- | --- |
| 右键空气 | 打开目录，选择一个发起端频道 |
| Shift + 右键目标 ME 节点 | 保存并建立无线连接 |
| 保持相同频道，再次 Shift + 右键该节点 | 断开该节点的无线连接 |
| 换选另一频道，再 Shift + 右键节点 | 将该节点改接新频道 |
| 普通右键 ME 节点 | 查看当前无线绑定，不打开目标设备 GUI |

发起端与目标都须加载并提供可用 ME 节点。目标暂不可用时，保存的连接会等待恢复；不能用它给普通箱子、流体罐或没有 ME 节点的机器直接添加 ME 接口。供电、频道与 AE 安全规则仍然生效。

手持收发器时，附近已绑定节点会按频道颜色显示标记；可在发起端 GUI 中设置频道颜色。

<a name="wireless-frequency" />

## 无线频率卡：按控制器连接整个设备簇

<ItemImage id="gtnotgood:wireless_frequency_card" />

频率卡使用**绑定的 ME 控制器**作为发射源，不读取网桥的名称频道。

1. 等普通 ME 控制器网络供电、结构有效并完成启动，手持频率卡 **Shift + 右键控制器**，保存并选中发射源。
2. 到目标设备处，**普通右键设备或线缆部件**。它会连接整个通过实体线缆相连的设备簇；瞄准部件本体，装饰面板不是节点。
3. 再次普通右键该簇内任意节点，可断开本频率在该簇上的连接。不要把另一个控制器网络当作接收簇。
4. 右键空气打开管理界面，可切换已保存的发射源、查看连接与频道状态、暂停连接或开启自动连接。

每张卡最多保存 **64 个发射源**，只能由绑定玩家使用。Shift + 右键空气会移除当前保存的发射源，**不会拆除已有无线连接**，其他已保存发射源也保留。

自动连接开启时，将卡留在背包或饰品栏，放置支持的 ME 设备或部件后会尝试接入当前发射源。多张卡同时可用时，请手持要使用的卡，或关闭其他卡的自动连接。手动绑定仍须手持卡。

连接可跨维度，两端区块须加载；无电、无效控制器、AE 权限不足或异频冲突时不能正常接入。异频簇被实体线缆连在一起时，应先拆分并检查绑定。

无线入口共享源控制器的频道预算，也与有线设备共用。例如独立单个控制器的几何预算为 **6 × 32 = 192 频道**，不是每个接收设备各有 192 频道。以管理界面的实际分配和等待状态为准。

需要查看世界中的网络，可在管理界面切换网络视图；手持卡时 **Shift + 滚轮**也可切换。手持或佩戴卡时显示视图。自动连接、打开管理界面、切换网络视图的快捷键默认未绑定，需在按键设置中自行设置。

<a name="flux-logistics" />

## 通量物流插头：按网桥频道补货或回收

<ItemImage id="gtnotgood:flux_logistics_plug" />

把插头贴在容器或机器允许存取的一面，右键选择已有的**网桥发起端频道**，可按目标库存补货，或切换为自动回收。它通过无线网桥接入 ME，占用 **1 个频道**，两端都须保持加载。

完整的 3D 安装示例、过滤槽与目标数量操作见[通量电网与物流](flux-network.md#logistics-setup)。悬停物流插头按 G 也会直接打开该章。

<a name="troubleshooting" />

## 常见问题

| 现象 | 检查项目 |
| --- | --- |
| 设备接上 ME 仍不工作 | 检查供电、可用频道、安装面、目标访问权限和机器输入输出设置 |
| 高级 IO 把机器里的原料抽回去了 | 确认该原料已标记；未标记资源会被回收。标记资源超量部分由超量回收开关控制 |
| 阈值输出没有把容器补到设定量 | 它检查的是 ME 网络库存。维持容器库存应使用高级 IO 总线 |
| 库存 IO 接口不向箱子或原版熔炉填料 | 直接读取功能适用于受支持的 GT 单方块；普通容器补货使用高级 IO 总线或通量物流插头 |
| 大容量接口有样板但无产物回网 | 检查实际机器、处理配方、含流体的终极编码样板，以及机器产物的回收路径 |
| 请求器一直缺材料、等待 CPU 或离线 | 检查样板、原料、CPU、网络及区块加载；修改目标不会取消已有 CPU 订单 |
| 无线终端不能打开 | 检查绑定、终端电量、接入点范围、维度与权限；范围和能量升级分别生效 |
| 无线连接建立到了错误网络 | 先区分网桥名称频道与频率卡控制器来源，再核对已选来源和目标设备簇 |

GT 多方块的样板输入总成、ME 输入输出舱及虚拟模具见 [ME 舱室与虚拟模具](me-hatches.md)；特殊机器的无线配方派发见[无线封包样板供应器](packaged-pattern-provider.md)。
