---
navigation:
  title: 无线封包样板供应器
  parent: index.md
  icon: gtnotgood:wireless_packaged_pattern_provider
  position: 10
  recommend: 10
  keywords:
    - 打包样板供应器
    - 封包核心
    - 无线连接器
    - 奥术工作台
item_ids:
  - gtnotgood:wireless_packaged_pattern_provider
  - gtnotgood:packaged_wireless_connector
  - gtnotgood:arcane_workbench_packaged_core
---

# 无线封包样板供应器

将 ME 自动合成任务派发给已绑定的专用机器。本页以**奥术工作台**为例，演示从连接到首次合成的完整流程。

<a name="setup" />

## 准备设备

设备：<ItemImage id="gtnotgood:wireless_packaged_pattern_provider" /> <ItemImage id="gtnotgood:packaged_wireless_connector" /> <ItemImage id="gtnotgood:arcane_workbench_packaged_core" />

- 无线封包样板供应器、物品无线连接器、奥术工作台封包核心。
- 已供电、有可用频道的 ME 网络，以及存储、合成 CPU 和可下单的终端。
- 奥术工作台、可用于奥术合成的法杖，以及配方所需的研究、材料和 Vis。
- 玩家背包中至少一张空白样板，供应器中至少一个空样板槽。

下面是布局示意。ME 控制器代表你的现有网络，电源、存储和合成 CPU 未画出；设备间距可自行调整，石砖底座不是结构要求。

<a name="layout" />

<GameScene width="350" height="220" interactive={true} showGrid={true}>
  <ImportStructure src="/assets/packaged-provider-layout.snbt" />
  <BlockAnnotation pos="0 1 1" color="#FF55BBDD" thickness="0.025">
    接入已供电的 ME 网络，确保供应器获得频道。
  </BlockAnnotation>
  <BlockAnnotation pos="1 1 1" color="#FFFFD166" thickness="0.025">
    右键打开供应器，在右侧核心槽装入奥术工作台封包核心。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="0.025">
    奥术工作台须保留法杖。配方录入完成后清空示例材料。
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.6 1.5" text="供应器 + 奥术核心" color="#FFFFD166" maxWidth="100" />
  <TextAnnotation pos="5.5 2.4 1.5" text="工作台 + 法杖" color="#FF88CC88" maxWidth="100" />
</GameScene>

可拖动旋转场景、缩放和逐层查看；悬停高亮方块可查看操作说明。场景用于展示布局，实际操作请在世界中的设备上完成。

<a name="bind" />

## 连接工作台

1. 将供应器接入 ME 网络，右键打开界面，装入**奥术工作台封包核心**。
2. 手持**物品无线连接器**，**普通右键供应器**，选中发射端。
3. 继续手持连接器，**Shift 右键奥术工作台**，完成绑定。
4. 右键空气可取消连接器的当前选择；已绑定目标仍保留在供应器中。

<a name="binding-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/packaged-provider-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FFFFD166" thickness="0.035" alwaysOnTop={true}>
    手持连接器，普通右键供应器。无需按住 Shift。
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="0.035" alwaysOnTop={true}>
    手持连接器，Shift 右键目标工作台。
  </BlockAnnotation>
  <LineAnnotation from="2 1.7 1.5" to="5 1.7 1.5" arrow="end" color="#FFFFD166" thickness="0.04">
    无线绑定关系，不是必须放置的线缆。
  </LineAnnotation>
  <TextAnnotation pos="1.5 2.8 1.5" text="① 普通右键选中" color="#FFFFD166" maxWidth="100" />
  <TextAnnotation pos="5.5 2.8 1.5" text="② Shift 右键绑定" color="#FF88CC88" maxWidth="100" />
</GameScene>

供应器和工作台须处于**同一维度并保持区块加载**。无线绑定不会强制加载区块；供应器所有者须在目标维度在线，并具有目标的操作权限。

<a name="record" />

## 录入奥术配方

1. 在真实奥术工作台中摆好一份已解锁的奥术配方，并放入法杖，确认配方能正常匹配。
2. 关闭工作台界面，保留台上的示例材料和法杖。将空白样板放在自己的背包中。
3. 打开供应器的**已连接目标**列表，**左键对应工作台条目**。供应器会消耗一张空白样板，将录好的处理样板放入自己的空样板槽。
4. 录入不会消耗台上的示例材料。录入后将九宫格材料和产物槽清空，**保留法杖，并关闭工作台界面**。
5. 将实际合成材料存入 ME 网络，在终端中请求该产物。

请使用录入得到的样板，它保留了奥术配方的九宫格布局。普通合成样板不能用于这里的任务派发；只填写无序输入、输出的普通处理样板也不能代替已录入的奥术布局。

<a name="returns" />

## Vis 与产物回收

- 优先消耗工作台法杖中的 Vis。须使用支持奥术合成的法杖，且容量足以支付单次配方。
- Vis 不足时，安装神秘能源且 ME 中存有对应基础源质，可按服务器配置补足缺少的 Vis。首次验证时建议先给法杖充足 Vis。
- 奥术合成的产物及容器返还物直接进入供应器的 **9 格回收栏**，随后送回 ME；此路径不依赖“自动回收”开关。
- ME 没有存储空间时，物品保留在回收栏；回收栏无法容纳本次产物和返还物时，不会执行该次奥术合成。

供应器有 **720 个样板槽**，每台最多连接 **1024 个不同目标**。多个工作台可共用一个供应器，但每个目标必须符合所装核心的要求。

## 更换目标机器

供应器依靠**机器专用核心**工作。基础封包核心只是制作专用核心的材料，不能用于向箱子或任意普通机器派发任务。

| 目标 | 使用的核心与主要条件 |
| --- | --- |
| 奥术工作台 | 奥术工作台封包核心；按本页录入布局，保留法杖 |
| 坩埚 | 神秘时代坩埚封包核心；满足研究与热源要求，并供应源质 |
| 注魔祭坛 | 神秘时代注魔封包核心；祭坛已激活、基座为空并供应源质 |
| 血祭坛 | 血魔法祭坛封包核心；满足祭坛等级并供应 LP，每张处理样板只写单步配方 |
| 装配线 | 对应装配线核心；机器成型、有配方数据和供电，使用所需的普通输入/输出仓室 |

含流体输入的处理配方须使用**终极编码样板**。血祭坛、注魔祭坛和装配线等保留产物的目标，需要开启供应器左侧的**自动回收**；该开关默认关闭，只收取本供应器已派发任务的预期产物。

<a name="troubleshooting" />

## 无法开始时检查

| 现象 | 检查项目 |
| --- | --- |
| 连接失败 | 先普通右键选中本维度已加载的供应器，再 Shift 右键目标；检查权限、重复连接及目标数量 |
| 已绑定但不派发 | 核心是否匹配目标；ME 是否供电、有频道；是否放入有效处理样板；所有者是否在目标维度在线 |
| 奥术配方无法录入 | 研究与台上配方是否有效；背包是否有空白样板；供应器是否有空样板槽 |
| 显示工作台被占用 | 关闭工作台界面，清空九宫格和产物槽，保留法杖 |
| 缺少 Vis 或源质 | 检查法杖容量与充能；需要源质补充时，检查神秘能源及 ME 源质库存 |
| 产物未回到 ME | 检查 ME 存储与回收栏；使用祭坛或装配线时确认已开启自动回收 |

在已连接目标列表中，右键空闲目标可断开连接。有任务的目标不能直接断开；Shift 右键条目可尝试中断并释放任务，之后还须检查残留材料，并单独取消对应的 AE 合成订单。
