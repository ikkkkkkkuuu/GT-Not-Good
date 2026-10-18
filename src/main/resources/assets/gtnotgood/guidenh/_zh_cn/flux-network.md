---
navigation:
  title: 通量电网与物流
  parent: index.md
  icon: gtnotgood:flux_plug
  position: 40
  recommend: 10
  keywords:
    - 通量插头
    - 通量点
    - 通量物流插头
    - 无线 EU
    - 团队电网
    - 定量补货
item_ids:
  - gtnotgood:flux_plug
  - gtnotgood:flux_point
  - gtnotgood:flux_logistics_plug
---

# 通量电网与物流

通量插头与通量点连接 **GTNH 无线 EU 电网**，使用放置者所属 GT 团队的电量。通量物流插头通过 **ME 网桥名称频道**供应或回收物品与流体。

- [三个方块如何选择](#devices)
- [插头存电、通量点供电](#energy-setup)
- [电压、安培数与优先级](#energy-settings)
- [区块加载与拆装](#chunk-loading)
- [物流插头：定量补货与回收](#logistics-setup)
- [常见问题](#troubleshooting)

以下 3D 预览可旋转、缩放，悬停彩框可查看操作标注。箭头表示传输方向，石砖为展示底座；静态预览未运行电网或 ME 网络。能源连接臂以实际游戏中连接 GT 端口后的外形为准。

<a name="devices" />

## 三个方块如何选择

<a name="trio-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gtnotgood:flux_plug" x="1" y="1" z="1" />
  <Block id="gtnotgood:flux_point" x="3" y="1" z="1" />
  <Block id="gtnotgood:flux_logistics_plug" x="5" y="1" z="1" meta="0" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    通量插头：接收 GT 输出端送来的 EU，存入放置者的 GT 团队无线账户。右键调整输入限制。
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    通量点：从团队无线账户取电，向相邻 GT 输入端输出。右键核对输出电压与安培数。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    通量物流插头：贴在容器上，右键选择 ME 网桥频道，再设置补货目标或切换自动回收。
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.7 1.5" text="插头：EU 入网" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="3.5 0.4 1.5" text="通量点：EU 出网" color="#FFFFD166" maxWidth="115" />
  <TextAnnotation pos="5.5 2.7 1.5" text="物流：物品与流体" color="#FF88CC88" maxWidth="115" />
</GameScene>

| 方块 | 连接的一端 | 使用的网络 |
| --- | --- | --- |
| 通量插头 | 发电机、动力仓或 GT 线缆的输出端 | 放置者的 GT 团队无线 EU 账户 |
| 通量点 | GT 机器、能源仓或 GT 线缆的输入端 | 放置者的 GT 团队无线 EU 账户 |
| 通量物流插头 | 容器或机器允许存取的一面 | 已选择的 ME 网桥名称频道 |

<a name="energy-setup" />

## 插头存电、通量点供电

<a name="energy-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/flux-energy-layout.snbt" />
  <BlockAnnotation pos="0 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    LV 电池箱：能源输出面朝东，紧贴通量插头。实际使用须放入已充电的兼容电池。
  </BlockAnnotation>
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    通量插头：接收相邻设备主动送来的电包。设置页检查电压、安培数、启用传输与连接网络。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    通量点：按设定电压输出电包。先确认团队余额，再核对接收设备与线缆额定值。
  </BlockAnnotation>
  <BlockAnnotation pos="6 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    LV 挤压机：西侧接收通量点的 EU。正面朝北、自动输出面朝南，运行仍须满足实际配方。
  </BlockAnnotation>
  <LineAnnotation from="1.5 2.1 1.5" to="5.5 2.1 1.5" arrow="end" color="#FFFFD166" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="输出端 → 插头" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="5.5 2.8 1.5" text="通量点 → 输入端" color="#FF88CC88" maxWidth="115" />
</GameScene>

1. 让 GT 输出端紧贴**通量插头**；经过 GT 线缆也可接入，确认线缆已经连接到插头。
2. 由实际使用该电网的玩家放置插头。它自动使用该玩家的 GT 无线账户，按当前 GT 团队归属结算。
3. 在用电端放置**通量点**，紧贴机器的能源输入面或已连接的 GT 线缆。由同一玩家或同一 GT 团队成员放置，便可共用团队余额。
4. 普通右键打开两端 GUI，在“设置”页核对电压与安培数，并保持“启用传输”和“连接网络”开启。
5. 在“网络统计”页检查无线电网余额和实际 EU/t；在“连接”页核对六个方向的相邻 GT 设备。

“网络选择”页显示所属的 GTNH 无线电网账户，可切换是否连接。放置即完成账户绑定，无需另外创建名称电网或用连接器配对。跨维度使用时，两端所在区块都须保持加载。

插头等待发电机、动力仓或 GT 线缆主动送电。通量点会向相邻接收端供电；多个接收面共用这一通量点的传输上限。

<a name="energy-settings" />

## 电压、安培数与优先级

只有**放置者本人**能修改能源方块的设置。GUI 中显示的设备名称仅用于辨认设备；所属账户由放置者决定。

| GUI 页面 | 操作与含义 |
| --- | --- |
| 主页 | 设置名称、优先级、浪涌模式和区块加载；查看实际传输量与缓存 |
| 网络选择 | 查看 GTNH 无线电网账户，并切换“连接网络” |
| 连接 | 检查六个方向的相邻 GT 能源设备 |
| 网络统计 | 查看团队无线余额、实际 EU/t 和电压提示 |
| 设置 | 修改电压、安培数，切换“启用传输”“连接网络”和“需要红石信号” |

放置时只识别一次相邻 GT 端口：插头取可读取的最高输出电压，通量点取可读取的最低输入额定电压；无法识别时采用 **32 V**，初始均为 **1 A**。之后更换邻居不会自动改掉设定，须重新核对。

- **传输上限 = 电压 × 安培数 EU/t**。例如 32 V、4 A 的上限为 128 EU/t；实际流量还受供电、余额、接收能力和线缆限制。
- 插头只接收不高于所设电压的电包，每 tick 接收的总安培数受设定限制。提高电压上限不会把低压电包自动变成更高功率。
- 通量点按**设定电压**输出完整电包；余额不足一个电包时等待。输出须匹配接收设备，仍遵守 GT 过压及线缆安培数限制。
- 优先级范围为 **-9999 至 9999**，数值较高的通量点先从余额取电。“浪涌模式”优先于普通优先级，传输上限仍由电压和安培数决定。
- 开启“需要红石信号”后，设备须收到红石信号才传输。临时停用时也会保留已有 EU 的结算。

<a name="chunk-loading" />

## 区块加载与拆装

主页的“区块加载”开关只强加载**该能源方块所在的一个区块**，受服务器 Forge 配额限制；申请失败时开关保持关闭。跨区块的发电设备、机器和线缆仍须分别保证加载。物流插头的加载需由其所在区域的加载方式负责。

拆下能源方块时，已接收的缓存 EU 会退回原无线账户；掉落物不携带电网余额。拆装后配置重置，重新放置会绑定新的放置者，并重新识别电压、采用 1 A。重新安装后先核对设置，再启用用电设备。

<a name="logistics-setup" />

## 物流插头：定量补货与回收

<a name="logistics-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/flux-logistics-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    ME 网桥发起端：接入主 ME 网络，右键设置一个可用频道名。物流插头选择这个名称。
  </BlockAnnotation>
  <BlockAnnotation pos="4 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    通量物流插头：安装时点击目标容器的一面；连接臂指向目标。右键选择频道和传输方向。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    目标容器：补货模式检查这个容器的库存，回收模式提取连接面允许输出的资源。
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="4.5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.7 1.5" text="发起端：ME 名称频道" color="#FF55BBDD" maxWidth="115" />
  <TextAnnotation pos="4.5 0.4 1.5" text="插头：选择频道" color="#FFFFD166" maxWidth="115" />
  <TextAnnotation pos="5.5 2.7 1.5" text="容器：检查连接面" color="#FF88CC88" maxWidth="115" />
</GameScene>

图中箭头为“网络 → 容器”的补货方向。ME 发起端的线缆、供电和存储在图中省略；自动回收时，资源沿相反方向返回 ME。

1. 将 **ME 网桥发起端**接到已供电的主 ME 网络，右键输入一个未被其他发起端占用的频道名，按回车或关闭界面保存。
2. 手持物流插头，点击目标容器或机器**允许存取的一面**安装。插头只访问连接臂指向的相邻容器。
3. 右键打开插头 GUI，在左侧目录选择频道，或输入同一频道名并确认，等待状态变为“在线”。只有放置者能修改配置。
4. 点击右上角方向按钮，选择“网络 → 容器（按过滤槽）”或“容器 → 网络（自动回收）”。

物流插头无线接入发起端的 ME 网络，占用 **1 个 ME 频道**；两端须保持加载，ME 网络须有供电、可用频道及相应存储。

拆下物流插头会重置频道、传输方向、样本和目标量；实际待交付的缓存资源保留。重装后须重新选择频道并配置补货或回收。

### 网络 → 容器：按目标库存补货

物品和流体各有 **9 个过滤槽**。拿着物品或流体容器点击相应样本槽，再在右侧填写目标数量；样本不会被消耗。流体数量以 **mB** 为单位。

- **正数**：补足至目标库存。例如物品目标 64、容器已有 20，就最多再补 44；流体目标 1000 表示补至 1000 mB。
- **留空或 0**：持续填充该资源，直到容器没有可用空间或 ME 库存不足。
- 只补缺量，容器里超出目标的部分会保留。同一种资源只保留一条样本，重新标记会移除旧样本位置。

先设置正确样本，再填写数量；填写数量不会选择资源。物品按种类、元数据和 NBT 匹配。流体容器须能报告当前库存才能使用正数目标。不足的原料等待 ME 现有库存补充，不会自动发起合成任务。

多个插头或其他管道同时服务一个容器时，应协调各自的目标和取料设置。

### 容器 → 网络：自动回收

自动收取连接面允许取出的物品与流体，**忽略全部过滤槽和目标数量**。切换前先确认容器中哪些原料也允许被提取。

机器输入输出面、覆盖板、槽位访问规则与流体容器容量仍然生效。ME 无原料、无存储空间或目标拒绝存取时，传输等待；断线后也须先恢复网络条件。

<a name="troubleshooting" />

## 常见问题

| 现象 | 检查项目 |
| --- | --- |
| 插头没有存入 EU | 检查 GT 输出方向、线缆连接、输入电压上限、安培数和传输开关；输出端须主动送电 |
| 通量点不供电 | 检查所属团队与余额、完整电包所需 EU、目标输入面、接收容量及红石条件 |
| 换了机器后功率仍不对 | 电压只在放置时自动识别；右键手动调整，并核对线缆和机器额定值 |
| 开了浪涌仍达不到预期 EU/t | 浪涌调整取电顺序；继续检查电压 × 安培数上限、供电与接收能力 |
| 能打开 GUI 但改不了设置 | 仅放置者能修改配置，团队共享余额不会赋予他人的设备配置权 |
| 离开后停止工作 | 检查两端及跨区块设备的加载；能源方块只加载自身区块，且受服务器配额限制 |
| 物流插头离线 | 检查 ME 发起端频道、两端加载、ME 供电和可用频道 |
| 物流插头把原料也抽走了 | 自动回收忽略过滤槽与目标量；检查方向和机器连接面的可输出资源 |
| 填了目标量却没有补货 | 先设置样本，再检查 ME 库存、容器可用空间和该面的输入限制 |

ME 网桥、库存总线与接口的其他用法见 [AE 总线、接口与终端](ae-devices.md)；多方块的 ME 输入输出与样板设备见 [ME 舱室与虚拟模具](me-hatches.md)。
