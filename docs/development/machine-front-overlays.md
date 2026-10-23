# 机器透明正面材质

从本机 GTL 安装包固定版本移植 16 套原生透明正面，统一放在 `src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/<机器>/`。

[打开面板预览](machine-front-overlays.html)（棋盘格表示透明区域，动画播放遵循原 mcmeta 的帧序和时长）。

这些是供后续控制器使用的资源；本次没有新增机器或替换现有机器正面。每套都有待机和运行图，其他状态以实际文件为准。原图和动画元数据逐字节保留，没有把带固定机壳底色的整面贴图混入其中。

| 面板 | 文件夹 | 发光层 | 暂停态 | 动画 |
| --- | --- | --- | --- | --- |
| 极限反应堆 | [extreme_reactor](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/extreme_reactor) | 有 | 有 | 有 |
| 热压浸解联合体 | [hydrothermal_leaching_complex](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/hydrothermal_leaching_complex) | 有 | 有 | 有 |
| 中子控制工厂 | [neutron_control_factory](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/neutron_control_factory) | 有 | 无 | 有 |
| 星璇号空间站 | [stellar_vortex](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/stellar_vortex) | 有 | 有 | 无 |
| 脱水机 | [dehydrator](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/dehydrator) | 有 | 无 | 无 |
| 闪电处理机 | [lightning_processor](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/lightning_processor) | 有 | 无 | 有 |
| 中子态素压缩机 | [neutron_compressor](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/neutron_compressor) | 无 | 无 | 有 |
| 世界信息扫描仪 | [world_data_scanner](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/world_data_scanner) | 有 | 有 | 无 |
| 宇宙模拟 | [cosmos_simulation](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/cosmos_simulation) | 有 | 无 | 有 |
| 超维度等离子锻炉 | [dimensionally_transcendent_plasma_forge](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/dimensionally_transcendent_plasma_forge) | 有 | 无 | 有 |
| 大型蒸汽离心机 | [steam_centrifuge](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/steam_centrifuge) | 无 | 无 | 有 |
| 大型蒸汽电路组装机 | [steam_circuit_assembler](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/steam_circuit_assembler) | 无 | 无 | 有 |
| 蒸汽搅拌机 | [steam_mixer](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/steam_mixer) | 无 | 无 | 有 |
| 蒸汽洗矿机 | [steam_ore_washer](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/steam_ore_washer) | 有 | 无 | 无 |
| 蒸汽挤压机 | [steam_pressor](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/steam_pressor) | 有 | 无 | 无 |
| 大型蒸汽热力离心机 | [steam_thermal_centrifuge](../../src/main/resources/assets/gtnotgood/textures/blocks/parallel/fronts/steam_thermal_centrifuge) | 有 | 无 | 有 |

## 文件与接入

- `overlay_front.png`：待机。
- `overlay_front_active.png`：运行。
- `overlay_front_emissive.png` / `overlay_front_active_emissive.png`：对应状态的发光层，存在时才引用。
- `overlay_front_paused.png` / `overlay_front_paused_emissive.png`：部分机器带暂停态。
- `*.png.mcmeta`：动画帧时长和顺序，必须与对应 PNG 一起保留。

参照 `LargeFurnace#getTexture` 的独立机壳加正面叠层；结构定义不会自动决定控制器的底图。
例如注册极限反应堆的待机图（省略 `.png`，不包含 `textures/blocks/`）：

```java
private static final IIconContainer FRONT = Textures.BlockIcons.custom(
    ModList.GTNotGood.getResourceLocation(), "parallel/fronts/extreme_reactor/overlay_front");
```

在机器初始化、纹理拼接前创建并复用图标，避免在每次渲染时创建。正面返回的纹理按顺序叠加：

```java
new ITexture[] {
    casing,
    TextureFactory.builder().addIcon(front).extFacing().build(),
    TextureFactory.builder().addIcon(emissive).extFacing().glow().build()
};
```

`casing` 由机器选择，例如现有大型熔炉使用 casing 16；`front` 和 `emissive` 随机器状态选取。没有发光图的面板只返回前两层。原图上的面板、仪表颜色保留；透明部分显示所选机壳。`_emissive` 文件名本身不会自动启用 GTNH 发光，需要上述 `glow()` 层。

原动画均为方形帧纵向排列（16×16，星璇号为 32×32 静态图），无需缩放。Minecraft 1.7.10 读取 `animation` 元数据；保留的 `shimmer` / `gtlbloom` 字段不会自动提供新版泛光效果。

## 来源

- GTL-Enhancedcore 3.8.0，S_Fast，4 套。
- GTLCore 1.2.3.2-fix4，POWSDSSAG / EasterFG / Maple，12 套。

两者安装包均声明 LGPL 第 3 版；资产不归入项目的通用 MIT 授权。精确包哈希、路径及每个文件哈希见 [资产清单](../../src/main/resources/META-INF/machine-front-overlays/ASSET_MANIFEST.json)，许可证见 [NOTICE](../../src/main/resources/META-INF/machine-front-overlays/NOTICE.md)。尚未确认这些安装包对应的源码提交，因此不记录猜测的 commit。
