# 项目目录与命名

Java 主代码位于 `src/main/java/com/xyp/gtnotgood`。文件目录必须与 `package` 一致，按职责分类后再按功能分组。

```text
com/xyp/gtnotgood/
├─ client/                 客户端渲染、HUD、输入和界面
├─ common/
│  ├─ blocks/              Forge 方块、TileEntity 和配套功能逻辑
│  │  ├─ beekeeping/
│  │  ├─ flux/
│  │  ├─ mebridge/
│  │  ├─ mechanicaluser/
│  │  ├─ mecontainer/
│  │  ├─ mestock/
│  │  ├─ network/           可编程网络方块与传输逻辑
│  │  ├─ packaged/
│  │  └─ stockio/
│  ├─ items/               物品及其独立功能
│  │  ├─ advancedio/
│  │  ├─ compass/
│  │  ├─ fuel/
│  │  ├─ mebridge/
│  │  ├─ mechanicaluser/
│  │  ├─ mestock/
│  │  ├─ packaged/
│  │  ├─ patternsorter/
│  │  ├─ stockio/
│  │  ├─ toolbelt/
│  │  ├─ veinmining/
│  │  └─ wildcard/
│  ├─ parts/               AE 部件、库存配置与部件调度
│  │  ├─ advancedio/
│  │  ├─ mestock/
│  │  └─ stockio/
│  ├─ machines/            GregTech 单方块机器、多方块和舱室
│  ├─ network/             网络消息、处理器及消息注册
│  ├─ gui/                 通用 GUI 基础、纹理和组件
│  ├─ recipe/              配方定义
│  ├─ compat/              跨模组兼容逻辑
│  └─ …                    其他已有通用职责
├─ loader/                 初始化和注册入口
├─ mixins/                 early、late Mixin 类型与配置插件
├─ config/                 配置和同步服务
└─ utils/                  通用工具、枚举和机器工具
```

## 文件归属

- 普通 `Item` 实现放在 `common.items.<功能>`；AE 部件的物品表示也放在这里。
- `Block`、`TileEntity` 和对应 `ItemBlock` 放在 `common.blocks.<功能>`。`ItemBlock` 是方块注册适配器，随方块维护。
- `Part` 实现放在 `common.parts.<功能>`。同一功能共享的配置、库存观察和调度逻辑留在该功能子包。
- 网络消息放在 `common.network` 或其功能子包。`common.blocks.network` 专指可编程网络的方块系统。
- 配方定义放在 `common.recipe`；通用注册入口放在 `loader`。
- TileEntity 与 GUI、适配器存在紧密的包内访问关系时，保留同一功能子包；客户端专用渲染、HUD 等继续放在 `client`。
- Mixin 配置 JSON 的 `package` 及其子包是保留命名空间，只放带 `@Mixin` 的顶层类型和 JSON 声明的配置插件。普通运行时辅助类按职责和功能放在保留前缀外的 `common.*` 等包中，例如槽位组件放在 `common.gui.slots`；即使没有列入 Mixin 配置，也不能放进保留包，否则正常类加载会被 Mixin 拒绝。
- 测试文件在 `src/test/java` 镜像被测功能的包结构；`scripts/qa` 中独立测试夹具保持脚本入口位置。

`ae2thing`、`commandtree`、`com.xyp.ldlib` 和 `src/vendor` 保留各自移植模块的结构。`reference` 中的上游检出仅用于参考，不参与编译或资源打包。

## 命名

- 类、接口和枚举类型使用大驼峰；枚举成员同样使用大驼峰，如 `LargeOreProcessor`、`WirelessLaserEnergyLV256A`。
- 方法、参数和普通字段使用小驼峰，如 `gasInput`、`structureDefinition`、`storedItems`。
- 真正的 `static final` 常量保留全大写下划线形式；外部接口覆盖、Mixin `@Shadow` 和反射目标遵循对应 API。
- 新增包目录使用全小写。项目规范指定的 `common.machines.multiblock.multiMachineBase` 保持现有路径。
- 普通文件名与其公开顶层类型一致，如 `NetworkHandler.java`。

重命名 Java 标识符时保留物品与方块注册 ID、机器数字 ID、NBT 键、配置键、网络消息编号及资源路径；反射使用的 Java 字段名必须同步。翻译由 Java 中的 `#tr` 注释生成，不直接编辑 `.lang`。

## 检查

运行 `scripts/qa/CheckProjectStructure.ps1` 检查目录与包声明、项目内 import、物品与方块归属、Mixin 配置、保留包中的顶层类型和 QA 源路径。结构调整后运行 Gradle 的格式化、编译和测试检查。

2026-10-07 结构调整后，封包供应器的公开扩展接口位于 `com.xyp.gtnotgood.common.blocks.packaged.PackagedCoreRegistry.Adapter`，其参数类型 `TilePackagedProvider`、`PackagedTarget` 位于同一包。使用旧 Java 包名的外部附属模组需要更新引用并重新编译。
