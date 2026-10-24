# 多方块终端

多方块终端将 GTL 的建造、拆除设置接到 GTNH 的 StructureLib 投影仪上。当前固定依赖为 StructureLib 1.4.45、GT5 5.09.54.205、AE2 rv3-beta-1080-GTNH；验证范围与命令见下文。

## 操作与原生投影

- 右键控制器显示结构投影及原生结构说明；右键空气打开终端设置。
- Shift 右键控制器执行当前建造或拆除模式；Shift 右键 ME 节点绑定材料网络，随后在设置中启用 AE。
- 设置中的“投影频道 / 方块等级”打开原生频道 GUI；“材料源优先级”打开原生注册表设置。Shift 右键空气也保留原生注册表入口。
- “建造方式”可切换整机终端和原生投影仪。原生方式直接使用投影仪原有分批预算、机器专属生存建造及创造重搭逻辑；终端的仓室、替换、镜像和 AE 开关在该建造方式下暂停生效。切回整机或选择拆除后恢复这些设置。

物品继承 `ItemConstructableTrigger`，继续复用 StructureLib 的全息/错误提示、主频道与子频道、NEI 拖拽、世界及容器中键拾取频道、清空确认、`/sl_channel` 命令和材料源优先级。终端保存 1–648 的虚拟主频道数值，向原生结构逻辑传递副本，物理物品始终一件。升级合成继承输入投影仪的 NBT，输入数量迁移到该数值。

主频道沿用各机器的原生换算，不统一等于实际层数。例如原生蒸馏塔主频道 6 对应八层；具体尺寸和等级仍可通过原生子频道分别选择。

| GTL 设置 | 本平台对应行为 |
| --- | --- |
| 建造 / 拆除 | 在控制器原生 `survivalConstruct` 的每片回调中即时执行 V2 `survivalBuild` 包装，保留专用末片、逐层上下文、必需仓室计数和朝向，按原生规则处理草/雪。成型结构采集真实检测布局；拆除按构件匹配并走玩家采掘流程。 |
| 无仓室 | 默认开启；跳过 GT `MTEHatch` 仓室的建造/拆除，保留已有仓室。 |
| 等级方块 / 替换 | 通过原生注册频道选择等级；替换限于可识别频道等级、无 TileEntity 的匹配构件。新版四类 BlockMap 不适用于当前平台，不另造同名注册表。 |
| 镜像 | 有 `IAlignment` 的控制器通过 `toolSetFlip` 选择水平翻转，保留方向、rotation 及垂直翻转；不支持该镜像时提示并不改方块。无 alignment 的结构使用坐标镜像；拆除优先使用控制器实际 alignment。 |
| 重复层数 | 使用原生主频道/触发数量，具体层数、等级和范围由各机器自己的构建逻辑决定。 |
| AE | 优先使用绑定节点，不可用时尝试玩家携带的注册无线终端；逐操作检查权限并使用原生耗能存取。 |

## 材料与拆除边界

材料优先从已启用、同维度且区块已加载的绑定 AE 节点提取；绑定不可用时尝试主背包及 Baubles 栏中的 AE 注册无线终端，随后委托 `IItemSource.fromPlayer`。无线回退通过终端本身的加密绑定、原生距离/电量及玩家权限检查，使用终端电池支付传输能量，并保留其现有无限距离/电量升级规则；不搜索任意网络。原有 `inventoryProviders`、`stackExtractors`、嵌套材料容器和玩家排序继续生效，不反射其私有注册表。绑定支持 AE multipart/FMP、普通 `IGridHost` 及 GT MTE；不会强行加载区块。无线终端位置限定为上述可直接同步的玩家槽，不泛化未知嵌套背包写回。

物品回收依次尝试 AE、Forestry 原生背包存入、玩家背包和掉落，保留物品 NBT。Forestry 使用公开 `ItemBackpack.tryStowing`，遵循背包过滤、锁定和 Stow 事件；不将工具箱当作通用建材箱，也不承诺任意第三方背包均可写入。流体材料服务复用 AE 原生流体/容器接口；装桶后重新读取并检查流体类型、NBT 和数量，不能无损装桶的余量保存为原生 ME 栈数据包。`takeFluid` 也能直接消费匹配的流体数据包，并将未使用的数量保存为剩余数据包。

拆除保留控制器，跳过空气、提示占位、不匹配构件和未加载位置，保留采掘/编辑权限及 Forge 事件。成型检查还记录旧式 GT 仓室 adder 实际接受的精确类型，只有整片检查成功才提交这些证据，不重新执行有计数副作用的单元素检查。GT 原生 `MTEHatchInput`、`MTEHatchOutput`、`MTEHatchMultiInput` 的**精确类**在生存采掘成功后另行回收其未写入掉落 NBT 的物理流体；子类继续使用各自的保存/掉落行为，避免重复返还。不能据此承诺未知仓室、虚拟库存或正在执行的第三方任务均可无损搬迁。

非 StructureLib 控制器若仍支持原生 `ConstructableUtility`，继续使用其投影/建造入口；未暴露可采集结构的位置无法拆除，会给出提示。替换不会清空任意障碍物，也不替换带 TileEntity 的等级方块。放置/替换失败时恢复快照并返还本次材料；实际兼容范围须由定向 QA 确认。

完整成型结构以原生成功检测的片段确定实际高度/长度，不要求新终端先复制原来的主频道数量。未成型结构尝试 1–648 的原生主频道布局，以实际构件匹配选择候选；已有子频道保持，固定几何签名去重，评分材料访问只模拟。实际不在已加载区块中的部分不会被推断或拆除。旧式 adder 允许但没有建造描述符的位置，在结构破损、无法获得成功检查证据时可能保留仓室。

## 验证范围

- 统一入口：`scripts/multiblock-terminal-qa.init.gradle`，外部 build/cache/run 位于 `D:/CodexTools/GTNGTerminal-20261024`。运行 `scripts/dev/Invoke-Gradle.ps1 runServer25 --init-script scripts/multiblock-terminal-qa.init.gradle -PterminalServerQA --project-cache-dir D:/CodexTools/GTNGTerminal-20261024/project-cache --offline --no-configuration-cache --console=plain`；客户端改为 `runClient25` 并去掉 `-PterminalServerQA`。静音隐藏的客户端仅由本轮脚本启动。
- 独立服务端 `server-final.log`：259 项核心及72项真实 ME 存储检查通过。覆盖一般/镜像结构、精确材料收支、缺料、替换取消/异常、采掘/放置保护、流体 NBT、原生仓室与 Forestry；真实高炉、八层蒸馏塔和五片装配线建造、原生校验及完整拆除，另验破损高塔和顶部旧式输出仓。ME 使用实际线缆、驱动、实体物品/流体元件、能源、无线接入点和安全终端，覆盖断电/恢复、拒绝权限、节点移除、主背包/Baubles 无线供料与真实电池收支。
- 客户端 `client-final.log`：258 项集成服务端及18项界面检查通过。真实控件回调和网络同步验证整机/原生方式、禁用开关、模式/频道保存重开、原生子频道编辑上传及材料源注册表；客户端真实投影收到虚拟主频道与原有子频道。七张截图位于 `build/terminal-client-qa/screenshots/`，已检查布局与文本。不是物理鼠标坐标命中或独立远端联机测试；NEI 拖拽、中键拾取和命令通过原物品继承与原生协议保留，本轮未逐项重放这些操作。
- 用户反馈后补真实 Shift 右键链路：`client-interaction-fix.log` 通过6次实际 `PlayerController.onPlayerRightClick` 和25项服务端检查；实际客户端发出原生C0B潜行状态、C08操作，服务端只观察结果。覆盖创造简单结构及空料真实高炉、生存精确耗料与拆除返材、原生创造重搭和原版投影仪对照；原258项核心及18项GUI也通过。修正客户端过早返回true吞掉操作包的问题，不新增自定义协议。
- 格式、编译、资源、普通/开发/源码打包以及来源/图标/语言键、Java 8/Multi-Release 与 QA 类隔离审计，最终证据见 `docs/development/progress.md`。源码及测试均保持现有依赖版本。

## 固定来源与许可

- GTLCore：[`6443c848264fb15c90cf50e031f146522616a8b9`](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9)，行为参考 `UltimateTerminalBehavior`、`AdvancedBlockPattern`；两帧 16×32 原图和 `frametime: 6` 元数据逐字节保留，受 LGPL-3.0 约束。现代 Java 源码仅作参考，不加入编译或资源源集。
- StructureLib：1.4.45 / [`4fdea655379db43443223aa448255144b76d85ca`](https://github.com/GTNewHorizons/StructureLib/tree/4fdea655379db43443223aa448255144b76d85ca)，`TerminalStructureCapture` 改编 `StructureUtility.iterateV2` 的导航遍历算法；`TerminalBuildPiece` 适配导航元数据并包装原生 V2 `survivalBuild`。两文件保留 GPL-3.0-only 声明；其他入口调用固定依赖 API。

完整源路径、源文件/资产 SHA-256、目标映射、改动说明及两份许可证位于 `src/main/resources/META-INF/multiblock-terminal-port/`。上述外来源作品及算法派生文件不属于本项目通用 MIT 授权。只读原始参考在 `D:/CodexTools/GTNGTerminal-20261024/upstream/`；构建与 QA 证据由 `progress.md` 及本轮测试记录维护。
