# AE2LT faithful port audit

## GTNL quantum crafting refresh — 2026-10-23

Pinned GTNL `5837c5a8`, `2b79458b` (#646), and final `7b176111` for the quantum planner, cycle visualization, variant budgets, optimizer repair and name-field fix. LGPL-3.0 inspected before copying. File mappings, original-source hashes, artwork hashes, license and adaptation details are packaged in `META-INF/quantum-crafting-port/`. Existing compact CPU, batching and energy behavior is selectively merged; unrelated upstream changes excluded. Reference: `D:/CodexTools/GTNGQuantumPort-20261023/upstream`, outside all source sets. Validation and remaining work are recorded in `docs/development/progress.md`.

## 碎片世界采集器 — 2026-10-23

- 来源为用户指定 GTL 增强版实例的 `gtlcore-1.2.3.2-fix4.jar`；固定可读源码
  GNSW/GTLCore `6443c848264fb15c90cf50e031f146522616a8b9`，LGPL-3.0-only。
  本地二进制与固定源码的六组采集表逐项一致；不将其误记为已知构建提交。
- 普通 ULV 单方块，GTNG ID 28633；七份原图/动画不改像素，GTNH 原生底壳、
  两输入/十二输出与独立电路槽，原生概率、加工与 NBT。大型变体不在范围。
- 世界碎片改为各维度展示方块，不消耗；按用户确认映射远古世界到暮色森林、
  霜原星到海卫一。16维度表、矿物数量、流体倍数与基础能耗/耗时保留。
  流体电路与钻头、原生材料、可选物品及未移植 GTL 专属产物的差异见
  `reference/fragment-world-collector-port/PORT.md`。
- 目标为 `FragmentWorldCollector*`、`FragmentWorldCollection*`、配方 JSON 与
  `textures/blocks/basicmachines/fragment_collector/`；源/目标/修改/哈希见包内
  `META-INF/fragment-world-collector-port/`。参考源码与离线生成器不参与编译或打包；
  外部素材与改编配方不属于项目通用 MIT 许可。

## 奈亚拉托提普之触模块化移植 — 2026-10-23

- 用户指定 GTO 26.10.5 实例，并选择现版原结构/外观及 GTNG 模块化参数仓与执行核心。
- 固定 GTOCore-Main `156835a0e66a90cbc4e60c3e95a36c35212b97d6`，LGPL-3.0；原 MBS 与实例逐字节一致。
  保留 165×48×49、29,045 个非空位置与空格 any 语义，控制器偏移 `(82,23,0)`。
- 十四类构件、分子常亮层、乌鲁工作线圈、动画和 611 张 CTM；复用同源现有构件，保留所有已有注册 ID。
  GTM 无菌过滤原图取自内嵌 26.10.53；布局转换器沿用既有 LDLib GPL-3.0 记录。
- 新控制器位于 `modularmachine/NyarlathotepsTentacle.java`，ID 28544；后按用户要求取消本机装配研究及电路印刻。
  超时空装配线统一包含 GTNH 全等级原生装配配方，不再单列 UIV 子集；不附带 GTO 专属电路/无人机支线。
- 模块化性能、材料与研究映射、来源和验证入口见 `reference/nyarlathoteps-tentacle-port/PORT.md`。
  包内来源与许可见 `META-INF/nyarlathoteps-tentacle-port/`，外部作品不属于项目通用 MIT 许可。
- 运行渲染复核补齐 154 个超能核心随工作状态发光；七组原渐变纹理生成 289 张逐 tick 播放贴图（含 CTM），
  不改原始资产，派生资源仍遵循 LGPL-3.0。生成器、时序和哈希见清单及 PORT.md；未声称原 GTO 实机同场景逐帧对照。
- 配方前端基于当前 GT5-Unofficial 5.09.54.205 的 `gregtech/api/recipe/maps/AssemblyLineFrontend.java`，
  官方 tag 对应 `a3e1e11241a814c9fa0dd0973d5699548428f689`，LGPL-3.0；源码 SHA-256
  `827630d2f3245d97fd14296a9a8c0aa15cc2872538ed211e99067529edfd62ab`。
  `client/nei/SuprachronalAssemblyFrontend.java` 继承物品/流体布局，改编前两段进度条，移除闪存箭头；
  原生纹理仅引用，未复制。来源记录见包内 `RECIPE_FRONTEND.json`；原版配方池的研究/印刻规则不变。

## GTL Enhanced End Crystal Resonator — 2026-10-23

- Reference: the supplied 1.20.1 instance's `GTL-Enhancedcore-3.8.0.jar`, S_Fast,
  SHA-256 `555a9514c4d2fd11a029c091ac053d5cf7c52fba62411f2f043e04454094f7aa`.
  No matching source commit has been established. Inspected manifest, mods.toml,
  Chinese translations, model JSON and javap output for EndCrystalResonatorMachine,
  ResonatorPlacementEvents and WirelessMachineRegistration. The archive declares LGPL-3.0.
- Fourteen original 16x16 fronts retain their bytes and license separately from MIT;
  packaged attribution, full license texts and source/destination/hash records live in
  `META-INF/crystal-resonator-port/`. Native GTNH casings are referenced, not copied.
  Upstream ordinal tiers 12/13 (UXV/OpV) map to GTNH UMV/UXV; GTNH voltages apply.
- Independent 1.7.10 implementation: `common/machines/basic/EndCrystalResonator.java`,
  `loader/EndCrystalResonatorLoader.java`, machine IDs 28619–28632, central enum entries.
  Preserves the exact-block crystal test, 20-tick scan/deposit interval, no crystal
  consumption and generation V[tier] × (LV:32; others:128). Native wireless team accounting
  replaces GTMThings. The upstream emitter container is never charged by its generator;
  the port advertises and implements wireless generation without a fictitious cable output.
- Uses authoritative GT ownership instead of upstream's nearest-player fallback. Pending
  EU and bound owner persist in NBT; removal flushes pending EU. GT's work toggle pauses
  generation; already generated EU can still settle after stopping or losing the crystal.
- The existing optional Et Futurum `end_crystal` item places a vanilla EntityEnderCrystal
  on the machine's top face. Placement checks edit permission, free space, duplicates and
  successful spawning before consuming one item; no new required mod dependency is added.
- Acquisition recipes are host-specific assembler recipes: a same-tier hull, two emitters,
  two circuits, four obsidian and two Eyes of Ender; 200 ticks at the native practical
  voltage. They do not claim to reproduce upstream progression. Tooltip follows the
  existing SteamTurbine description style and declares translations beside Java use.
- Reference inspections are in ignored `build/reference-gtl-enhancedcore` (a junction to
  this task's D-drive reference directory), outside source sets. Current-workspace compilation,
  resources, scoped Spotless and structure checks pass. The real client passes 296 checks
  with the original coremod manifest inherited by the QA jar; native early mixins and text
  effects are loaded. Verified all fourteen fronts in-world and LV/MAX tooltip screenshots.
  Commands, timings and final logs are recorded in `docs/development/progress.md`.

## 枢纽卫星工厂模式按钮 — 2026-10-23

- 参考用户截图，复用 GregTechCEu/GregTech-Modern 固定提交
  `e3844ba943e2388c0856617ae4f8b473a0ae1b64` 的
  `src/main/resources/assets/gtceu/textures/item/lv_robot_arm.png`；已先核对该提交 LGPL-3.0 许可证。
- 原 PNG 逐字节复制到 `assets/gtnotgood/textures/gui/overlay_button/machine_mode.png`，
  无裁切、重绘或缩放；仅由界面按按钮尺寸绘制，不添加物品或依赖。
- 来源、Git blob、SHA-256、目的路径与许可证记录于
  `META-INF/satellite-mode-port/ASSET_MANIFEST.json`；完整许可证随该目录打包。
- 模式列表为本项目针对当前 ModularUI2 2.3.92-1.7.10 API 的实现；没有复制上游 GUI 代码。
  模式顺序、编号及服务端控制器的配方映射逻辑保持兼容。

## TST 模块化仓室 — 2026-10-22

- 来源：Nxer/Twist-Space-Technology-Mod，固定提交
  `f59f874694906144e205a3451dfd665c701eca97`，根目录及资源目录许可 GPL-3.0。
  改编代码和原贴图保留 GPL-3.0-only，不属于项目通用 MIT 许可。
- 目标：`common/machines/hatch/modular/`、`common/recipe/machine/ModularHatchRecipes.java`、
  `assets/gtnotgood/textures/blocks/modular/`。40 个参数仓、3 个超频仓、3 个执行核心，
  保留 TST 并行、速度、能耗默认参数、原始等级和原贴图动画；动态参数迁至 MUI2、服务端校验、NBT 保存。
  超频倍率按用户要求定制，非 TST 原始倍率：低速无损仓耗时÷4、EU/t×4，
  无损仓耗时÷8、EU/t×4，奇点无损仓耗时÷8、EU/t×2。
  执行进度和产物由控制器保存，仓室仅保存配置；普通、高级、完美核心保留原供电种类。
- `GTNGModularMachineBase` 与 `common/machines/processing/` 接入六台 GTL/GTO 移植机器，默认有损超频 2/4，
  全部参数仓及执行核心均可安装；主机内置执行核心，附加核心增加独立任务。
  保留各机研究、算力、线圈和速度机制；拆下或禁用核心时暂停原任务。
  输入在副本上结算，联合预检原生 ME 库存及 AE 电量后再提交；任务进度、预付电量和待输出仅存于控制器。
  普通、特殊能源仓不设额外数量上限，仍须至少一个能源仓并满足结构舱位要求。
  输入功率安全合计，配方电压按最高单仓计算，避免多只低压仓累加电压造成越级。
- 六台模块化加工机器及宇宙之心无需维护仓，不产生维修故障或维修效率惩罚。
  七台控制器统一放在 `common/machines/multiblock/modularmachine/`，基类和结构定义保留各自子包。
  结构保留已有维护仓的可选兼容，但不自动建造维护仓；旧存档及遗留加工任务恢复满维修效率。
- 四十五个配方保持原生 GTNH 原料、用量、耗时和电压；奇点超频仓把 TST 英雄之证替换为
  完美执行核心，迁至标准 GT 装配线并增加 192 小时、30 EU/t 扫描，其余大宗材料和 64 天工时不变。
  完整配方映射见 `reference/tst-modular-port/RECIPE_ADAPTATION.md`。
- 共存适配：本模组 41 条组装机模块配方使用非消耗 19 号电路，普通执行核心研究自家静态并行仓 T1。
  同装时仅给 TST 原来没有电路的低速超频配方补 18 号，以免其子集输入及缓存抢占本模组配方。
  可选加载顺序及配方副本替换不引入 TST 编译依赖，材料、流体、工艺时间及电压保持不变。
  同时移除 TST 免研究池自动复制的本模组五种模块配方，防止执行核心再次抢配方、跳过研究或截断大宗用量；
  保留双方原生装配线及 TST 自己的免研究配方。
- 默认开发运行依赖补齐 NHC 及其集成依赖，为普通、高级执行核心和低速无损超频仓提供真实高能量流电路。
  GTNH 外仍保留可选集成行为：缺少该物品时仅跳过这三项原料依赖配方。
- 12 张 PNG 与 9 个动画元数据文件逐字节保留，来源、目标、修改及哈希见
  `META-INF/tst-modular-port/ASSET_MANIFEST.json`；原许可证与改编说明一并打包。
  参考检出仅位于 `build/reference-tst`，不参与编译或资源打包。

## Beyond the Stars decorative blocks — 2026-10-16

- Source: https://github.com/v3rysp3d/GTNH-Beyond-the-Stars, user-supplied sibling
  `GTNH-Beyond-the-Stars-master`. This local snapshot has no Git metadata; commit is unknown.
  Inspected the root/resource MIT licenses, README credits, block classes and texture generator.
  Copyright 2026 V3rySp3d; the original MIT notice is packaged unchanged.
- All 136 files in `assets/beyondthestars/textures/blocks/` move byte-for-byte to
  `assets/gtnotgood/textures/blocks/stellar/`: 56 ordinary block PNGs, 56 controller/hatch
  overlay PNGs and 24 animation metadata files. The overlays form 28 decorative panels,
  using original normal/glow layers, animated versions and runtime GT casing backgrounds.
  Packaged source/destination/hash and local snapshot records: `META-INF/stellar-block-port/`.
- `common/blocks/stellar/` adapts upstream metadata, six-face texture selection, translucent
  glass/face culling and the crystal/node light levels. Registers 54 ordinary variants
  across eight IDs and 28 panels across two more IDs, including the two formerly hidden
  module cores and both fuel/turbine appearances. Existing metadata holes remain empty. Every variant has a central
  `GTNGItemList` container and generated English/Chinese names in the block creative tab.
  Panels use native GT `IBlockWithTextures` rendering with a north-facing front and no TE.
- The existing Vault uses Eddington Wall Casings and the Kerr Compressor's normal/active
  front and glow artwork. Its controller and port share the Eddington background without
  taking a global GT casing texture index. Only the Eddington metadata conducts GT machine
  updates, so removing/replacing its structure propagates a native recheck. Its initial
  recheck delay uses the native 50 ticks instead of blocking changes for 200 ticks. Invalid Vaults
  unbind their port; the required casing is reflected in the generated structure tooltip.
- Adds 2,632 deterministic 16x16 MIT connected tiles across all 56 ordinary face
  textures. Existing Eddington paths and its 47 tiles remain unchanged and are reproduced
  by `scripts/generate-eddington-ctm.py`. `scripts/generate-stellar-ctm.py` adds the
  remaining 2,585 tiles with per-texture profiles: preserve central artwork; sample safe
  core backgrounds; continue bands, fins and rail interiors; extend the original
  alpha-110 glass through joined 1 px frames. Both generators preserve isolated mask 0
  byte-for-byte, retain exposed frames/concave caps and introduce no new colors or rotation.
  The manifest records every upstream source and destination hash, generator SHA-256 and
  profile separately. Original imported assets, references and snapshot hashes are unchanged.
- `StellarConnections`, `StellarConnectedIcons` and `StellarConnectedTexture` replace the
  Eddington-only classes. The built-in engine covers 54 ordinary variants, 28 panel backgrounds
  and the Vault controller/port using the vanilla/GT copied-block face UVs. Identity is
  each face's texture key: empty/fueled pillar top faces join while their sides remain
  distinct; panels with the same base join across different overlays; Extraction panels
  join the matching local casing. Different glass metadata retains its shared interface,
  and matching glass in the face-normal direction excludes hidden faces from connections.
  Native GT casing renderers are unchanged, so ordinary native GT casing blocks do not join.
  Front graphics, animation/glow and inventory appearances remain separate. There is no
  MyCTMLib, resource-pack rule, additional dependency, ticking or networking requirement.
- Five native GT panel bases add 235 derived LGPL-3.0-or-later tiles. These are outside
  the MIT asset list and the project's general MIT grant, with their own exact source,
  revision, modification, hash and license records in `META-INF/stellar-panel-ctm/`.
  Together the 56 MIT textures and five native GT bases have 61 materials and 2,867 tiles.
- Adds a host-specific MV assembler recipe for two Eddington Wall Casings: six steel
  plates, one steel frame, four copper foils, circuit 24 and 144 mB molten tin;
  a base duration of 10 seconds at 120 EU/t. This acquisition recipe is not copied from upstream.
- No upstream recipes, controllers, inventories, fuel/rotor interactions, heating-coil capability,
  glass-tier registration or stellar TESR/shader carriers are ported. Descriptions claim only
  decorative use. The sibling reference project never participates in compilation or packaging.
- Resource audit and native-pixel preview: ignored `build/stellar-block-reference/`.
  Isolated client validation: `scripts/stellar-block-qa.init.gradle`,
  `scripts/vault-appearance-qa.init.gradle`, `scripts/eddington-ctm-qa.init.gradle`,
  `scripts/eddington-recipe-qa.init.gradle`, `scripts/stellar-ctm-qa.init.gradle`
  and their matching classes in `scripts/qa/`;
  marked client windows use `HideFactoryClient.ps1`.
- General connected-texture QA passes 8,027 checks in each of two real clients,
  with and without Angelica; neither includes MyCTMLib. Covers all 82 appearances,
  61 sets of 47 stitched tiles, unchanged source PNGs/animation metadata, six-face
  masks, glass boundaries, panel graphics/glow, shared pillar ends, chunk-edge
  removal/replacement and the existing Vault. Each run captures 11 screenshots.
- Recipe QA loads GT5-Unofficial 5.09.54.183 and NewHorizonsCoreMod 2.9.76:
  7,893 loaded assembler entries have zero bidirectional quantity-independent conflicts.
  Native lookup rejects LV, accepts MV, consumes exact materials and retains circuit 24.

## Radial menu and toolbelt — 2026-10-15

- Source: https://github.com/Navatusein/GTNH-Radial-Menu, pinned commit
  `6fc34b6904cf66b7aa818025542bb61a550a9312`; original code is MIT, copyright 2026 Navatusein.
  Inspected the root license and bundled Phosphor license before copying. Phosphor artwork is also MIT,
  copyright 2020-2021 Phosphor Icons. Both notices are retained verbatim in `META-INF/radial-menu-port/`.
- Source mapping: `src/main/java/com/navatusein/radialmenu/` to
  `src/main/java/com/xyp/gtnotgood/client/radialmenu/`. `client/gui/WheelRenderer.java`,
  `WheelRing.java`, `WheelAnimator.java`, `GuiRadialWheel.java`, geometry/animation helpers and
  `client/gui/{editor,ui}/` preserve the upstream wheel drawing, nested rings, editor layout and controls.
  The wheel's fills, feathered edges, outlines and highlights are drawn in code; no screenshot is used as a texture.
- Four unchanged resource files move from `assets/radialmenu/` to `assets/gtnotgood/radialmenu/`:
  `textures/icons/phosphor.png`, its `LICENSE-phosphor`, `icons/phosphor.json` and `scripts/prelude.lua`.
  `ASSET_MANIFEST.json` records original and destination SHA-256 values and renderer source attribution.
  Native Minecraft/item textures, potion icons and player skins are resolved at runtime, not copied.
- Adaptations: host namespace and client lifecycle replace the standalone mod; the wheel shares the existing R-key
  registration and tool-menu navigation bindings. The existing server-authoritative ten-slot toolbelt becomes an
  built-in submenu with editable title, icon, opening mode and server-confirmed capacity (2–24, default 10).
  Live inventory slots retain synchronized ItemStack NBT and support stackable items, while runtime inventory children
  stay outside saved menu profiles. Shrinking occupied tail slots is rejected. Normal custom entries remain editable.
  English and Chinese text is declared next to Java key use and generated by the existing language task.
- The key-binding accessor lives in `mixins/early/radialmenu/KeyBindingAccessor.java` and is registered in
  `mixins.gtnotgood.early.json` through `loader/EarlyMixinsLoader.java`, so it loads before vanilla KeyBinding.
  Client-only registration keeps the accessor out of dedicated servers.
- Lua scripts use shaded `org.luaj:luaj-jse:3.0.1`. The cached binary JAR has no license entry; its Maven POM declares
  MIT. Verified the matching sources JAR and the POM-linked official license at
  https://luaj.sourceforge.net/license.txt. `LICENSE-LuaJ.txt` preserves that notice verbatim and
  `LUAJ_COPYRIGHTS.txt` retains the matching sources' copyright lines, including 2015 notices.
  The manifest records binary/POM/source/license hashes and artifact URLs.
- The pinned checkout stays in the operating-system temporary directory. Inspection artifacts under
  `build/radial-port-tools/` are not source/resource inputs. No reference checkout or remote screenshot is packaged.

## ME stock IO interface — 2026-10-07

- Pinned renderer reference: GTNH AE2 `rv3-beta-1073-GTNH`, sources SHA-1
  commit `151550f6d558a663eee792f0bfa22e12a53c0e54`,
  `12f114eb4e27c346d0278fcff7a09b5a3067b86`; inspected native `PartInterface` geometry
  and AlgorithmX2 LGPL-3.0-or-later headers before adaptation.
- Destination: `common/parts/stockio/PartStockIOInterface.java`. Interface cuboids and cable depth
  remain native; front/side/back reuse the existing AdvancedAE IO artwork unchanged.
  AdvancedAE commit/license/source/hashes remain in `META-INF/advancedio-port/`.
  Full-block appearance references native AE2's purple interface texture at runtime.
- The item/fluid GUI combines this project's super advanced ME input layout with 900 marks per type,
  independent automatic selection, per-resource reserves and fixed availability, using installed GT textures.
- GT5-Unofficial `5.09.54.183` native singleblock recipe checks are wrapped with independent input copies
  and transactional ME consumption. Inputs are never staged into the target. Output recycling uses the
  existing sided inventory/fluid adapter; unsettled resources stay durable across removal/conversion.
- Packaged records: `META-INF/stockio-interface/NOTICE.md` and `ASSET_MANIFEST.json`.
  References remain outside compiled source/resource sets. Usage: `docs/stock-io-interface.md`.

## GTL circuit pattern buffer — 2026-10-06

- Behavioral reference: https://github.com/GNSW/GTLCore, commit `478601a3ca554af3df5c2ad7ffb8e2a6d8e0228b`.
  Inspected `MEPatternBufferPartMachine`, `MEBufferPatternHelper`, `PatternCircuitConfigurator`, base and recipe traits.
- This snapshot has no root license and GitHub reports no license. No upstream implementation or visual assets are
  copied or packaged. Reference Java files are stored in ignored `reference/gtl-pattern-buffer`, outside source sets.
- Independent GTNH implementation: `common/machines/hatch/me/{CircuitMEPatternBuffer,CircuitPatternCodec}.java`;
  shared MUI2 hatch GUI, central item/machine registration and existing LV assembler progression. Long buffering
  is implemented in `common/machines/hatch/me/LongCircuitBuffer.java` without upstream code.
- Behavior: automatically reads per-pattern integrated circuits, native AE input requests with circuits removed,
  900 pattern slots, 64 item and 64 fluid types per pattern, each bounded
  to `Long.MAX_VALUE`. Stable int-sized recipe views settle GT debits into the long reserve and refill on access.
  Whole CPU batches are rejected atomically if a type limit or long quantity limit is exceeded.
- Native AE Ultimate Encoded Pattern fluid NBT remains intact. Runtime circuit identity is serialized in native
  pattern NBT to distinguish equal materials with different circuits and survive CPU task reload.
- Existing GT textures and MUI2 controls are referenced from the installed dependency; no new bitmap assets.
- Circuit configuration controls and shared fallback are omitted by user request; physical patterns are never edited.
- This circuit variant also omits the virtual mold control and only shares physical manual item slots. Its previous
  int save format migrates on load. Failed long refunds remain in a saved queue; dropped hatches retain these reserves
  for ME refund after replacement instead of creating billions of int-sized item entities. Waila aggregates use
  exact counters so totals across multiple full long buffers cannot wrap negative.
- Native AE2 `Platform.poweredInsert` uses a ceiling division that adds the unit divisor before subtracting one.
  Extreme refunds reserve that headroom and send the remaining tail separately; the buffer still stores the full
  signed-long maximum. Real item/fluid storage-cell tests verify exact partial-refund remainders.
- The quick terminal has an opt-in NEI import control for zero-sized circuits and current virtual mold selector items.
  It reuses the selector's matcher, encodes these inputs as one item, and continues omitting other non-consumables.

## ME stock controls — 2026-10-06

Inspected ExtendedAE `76691dbd5636c16ba05684f4b8d1cd98adc923aa` (26.1.2-neoforge, LGPL-3.0)
and ME Requester `95fbd4b6bfded008c65425bc0c52aec4f07b3ede` (26.1, LGPL-3.0), including licenses,
threshold parts, requester lifecycle, screens and asset models before adapting behavior/copying original PNGs.
Inspected AppliedEnergistics/Applied-Energistics-2 tag `v26.1.12-beta`, commit
`b7cf5822d9c128a61d9291cb2c1f92319253e4f0`, LICENSE and README license before copying shared GUI assets.
Its textures/models are CC BY-NC-SA 3.0, credited to Ridanisaurus Rid (2020), AlgorithmX2 et al (2013-2020);
these are excluded from the project's general MIT grant, with noncommercial distribution accepted by the user.
Inspected GTNH AE2 `rv3-beta-1073-GTNH`, sources SHA-1 `12f114eb4e27c346d0278fcff7a09b5a3067b86`.
Native exact resource queries, stack watchers and grid-cache APIs replace modern AEKey/capability APIs.
Destinations: `common/mestock/`; the requester has a newly authored, per-network bounded scheduler and
automatic deficit batching, shared CPU accounting and authorized inline terminal editing.
GUI APIs are rewritten for ModularUI2: original threshold bus/amount/emitter frames and toolbar crops,
requester row layout, state/submit/status widgets, grouped search/scroll and four adaptive terminal height modes.
Fluid fields display GTNH liters (L); exact native long amounts have no bucket scaling, as requested by the user.
Submit packets validate live identity.
Original PNGs remain unchanged and atlas/9-slice coordinates preserve native pixels.
Item and placed-device rendering now uses the pinned upstream cuboids, face UVs, rotations and state layers.
Destinations: `client/mestock/StockModelRenderer.java`, `META-INF/me-stock-port/models/` and
`assets/gtnotgood/textures/blocks/mestock/`. The 27 model JSONs retain their geometry/UV data; only line endings
and trailing whitespace are normalized. All PNGs are copied unchanged, including the requester active texture
and its animation metadata. Generated binding/root lists adapt the original resource names to this mod.
The three cable items register the native AE bus item renderer; the requester uses a client-only block renderer.
Texture-stitch completion publishes immutable baked quads for Angelica workers and rebuilds them on resource reload.
Requester power/channel events synchronize only a changed visual flag, without additional tick polling.
Packaged source/asset/license/destination/modification/hash records: `META-INF/me-stock-port/`.
Inspection checkouts are in the OS temporary directory and native sources in `build/aestock-reference`,
outside source/resource inputs. Usage and TPS design are documented in `docs/ME_STOCK_CONTROLS.md`.

## Processing batch dispatch reference — 2026-10-05

- Inspected `2824799/Apeiron`, commit `2b84f3ef8e720e629e039db934d75e5f57e53657`, particularly `PatternBatchDispatch` and its CPU mixin. Its original code is GPL-3.0-only; no source or assets were copied.
- Extended this project's existing `CraftingBatchPlannerImpl` and `MixinCraftingCPUCluster` to handle native AE fluid stacks and this mod's processing hatches. The existing batch transaction and diagnostics accounting remain in use.
- Physical hatch batches are bounded by their int-sized GT input stacks. Quantum CPUs retain a per-update work budget for native dispatch; ordinary AE CPUs retain their operation and energy limits. This is long-count batching, not Apeiron's arbitrary-precision inventory implementation.
- `reference/Apeiron` is an ignored inspection checkout outside compilation and resource source sets. Regression fixture: `scripts/qa/ProcessingBatchChecks.java`, launched with `scripts/processing-batch-qa.init.gradle`.

## Super Advanced Stocking Input Bus (ME) — 2026-10-05

- Uses the same pinned GT5-Unofficial `5.09.54.183` source artifact and LGPL-3.0 license below.
- Adapted native `MTEHatchInputBusME` and its GUI into `SuperAdvancedMEInputBus` and its GUI.
- Extends the fluid variant's 900 marks, reserve and fixed availability policies to items,
  preserving native circuit/manual slots and metadata/NBT identity. No ME item buffer.
- Source/destination/modification records are in `META-INF/super-storage-input-port/NOTICE.md`.
- Native item-slot adapters and textures are referenced at runtime; no artwork is copied.

## Super Advanced Stocking Input Hatch (ME) — 2026-10-05

- Reference: GTNewHorizons/GT5-Unofficial tag `5.09.54.183`, sources artifact SHA-1
  `b0f3eb42604cfc3ad88d56c50d0dbfa375d89419`. Inspected native input hatch, GUI,
  slot serialization and the pinned LGPL-3.0 license before adapting source.
- Destinations and modifications are recorded in `META-INF/super-storage-input-port/NOTICE.md`.
- Independent native stocking-hatch subclass with 900 marks, per-fluid reserves,
  fixed per-check available quantities and no physical buffer. Mining clears marks
  and configuration so drops stack with fresh hatches. Existing hatch definitions stay intact.
- Native textures/icons are resolved from installed GregTech. No artwork copied.
- Extracted sources in `build/storage-input-reference` do not participate in compilation or packaging.

## Wireless provider optimization toggle — 2026-09-30

Adds an opt-in toggle through GTNH AE2 `IInterfaceViewable#allowsPatternOptimization`.
New and legacy providers default to disabled; the choice is persisted in tile NBT.
The existing toolbar gains one button at (-18, 111), without moving upstream controls.
References the installed AE2 `guis/states.png` icons 178/194 at runtime, matching
`GuiInterface` / `GuiToggleButton` inspected in the rv3-beta-1050-GTNH source reference
(LGPL-3.0-or-later source headers). No assets are copied or modified and the packaged
asset manifest remains unchanged. Destinations: `PackagedProviderGui.java` and
`TilePackagedProvider.java`; server-side owner checks protect the toggle.

## Programmable Hatches ME data access hatch — 2026-09-29

Source: https://github.com/reobf/Programmable-Hatches-Mod
Pinned commit: `d035ae837db01d91a3a6bcc779dc5504459c4754` (`290-daily-latest`), MIT.
Original: `src/main/java/reobf/proghatches/gt/metatileentity/DataHatchME.java`.
Destination: `src/main/java/com/xyp/gtnotgood/common/machines/hatch/me/MEDataAccessHatch.java`
and `MEDataStickSnapshot.java`; recipe in `common/recipe/machine/MEDataAccessRecipes.java`.
The inspection checkout lives outside the project under the system temporary directory and is not a build input.
Packaged license and modification record: `src/main/resources/META-INF/me-data-access-port/`.

User requested IV tier and a recipe obtainable in IV. Uses EV data access hatch, ME interface,
IV hull, two IV circuits, IV sensor/emitter and 576 mB soldering alloy; 20 seconds at IV.
Preserves front-only ME connection, one channel, shared network data sticks and no GUI.
Replaces reflection and persisted borrowed inventory with native GT recipe caching and an NBT-sensitive,
defensively copied snapshot. Refreshes on relevant network changes, monitor replacement and a 400-tick fallback.
Only actual stored stacks authorize recipes; disconnection clears research and notifies GT watchers.
Uses the installed GT ME overlays and tier casing API; no copied image assets.

## Separate AdvancedAE IO bus port (2026-09-22)

Reference: `AdvancedAE/`, https://github.com/pedroksl/AdvancedAE, pinned commit
`5ad43ee1e5f7a8b9fe7a1eacfaebdd44b61b624c`. Inspected root `LICENSE.md` (LGPL-3.0), bus/menu
sources and generated part model before copying the three original PNG textures. The local checkout is
ignored and excluded from compilation/packaging. Exact source/destination/hash records and modifications
are packaged in `META-INF/advancedio-port/{NOTICE.md,ASSET_MANIFEST.json}`. The GUI retains the original AdvancedAE/AE2 layout and amount sub-screen.
Additional pinned references inspected before copying art: AE2 v26.1.10-beta
`3a051bb473de0b8fd329b39db4262f731d17e7e5` (LGPL code, CC BY-NC-SA 3.0 art), and AE2AddonLib
26.1.3-alpha-neoforge `5b48a86deea7ebf50bf95166f7b74a24057c90ac` (GPL-3.0).
`AdvancedIOGui.java` is GPL-3.0-only. Exact code mappings, visual licenses, destination paths,
unchanged asset hashes and modifications are recorded in the packaged notice/manifest.
These checkouts are ignored and never participate in compilation or packaging.

## Blood altar core extension (2026-09-21)

The blood altar core reuses the unchanged AE2LTPP core base and the existing renderer listed below:
source commit `1d3f183ecf258aff0001567d742ed30d6038d77b`, LGPL-3.0, source
`src/main/resources/assets/ae2ltpp/textures/item/provider_core_base.png`, destination
`src/main/resources/assets/gtnotgood/textures/items/packaged/provider_core_base.png`.
The pinned renderer and license were inspected before extending usage. The installed Blood Magic
altar item is rendered dynamically at the existing overlay size and placement; no Blood Magic assets
or source code were copied. The packaged `ASSET_MANIFEST.json` records this additional use.
New adapter and completion-stop logic were checked against the official GTNewHorizons/BloodMagic
`1.9.4` source artifact (cached source JAR SHA-1 `94434ffc9864f3dbfc7516f374f5618722c75239`).
Inspected source files were extracted to the OS temporary directory, outside compilation and packaging.

## Assembly line core extension (2026-09-21)

The two GTNG assembly line cores reuse the unmodified `provider_core_base.png` from
AE2LTPP commit `1d3f183ecf258aff0001567d742ed30d6038d77b` (LGPL-3.0; packaged destination
`assets/gtnotgood/textures/items/packaged/provider_core_base.png`, recorded in the asset manifest).
The existing port of `PackagedCoreItemRenderer` now accepts a target-item supplier, retaining the
upstream overlay size and placement. GT5U controller icons are obtained from installed items at
runtime; no additional upstream art was copied. Adapter logic is new GTNG code checked against
the pinned GT5-Unofficial `5.09.54.133` source archive. Reference files remain outside source sets.

Audit date: 2026-09-21. Runtime scope is the **direct-bound Provider + TC4** variant, explicitly selected by the user. A full AE2LT wireless-controller/frequency system is not part of this delivery. This file distinguishes verified upstream facts from intentional GTNH adaptations.

## Reproducible references

The original `ae2lt/AE2-Lightning-Tech` and `ae2lt/AE2LT-Packaged-Pattern-Provider` Git URLs returned Repository not found during this audit. Local reference directories contain full Git checkouts (not shallow clones) fetched from public preservation repositories. These are preservation copies, not a claim that the original maintainers endorse GTNG.

| Directory | Preservation repository | Checkout | Declared version |
| --- | --- | --- | --- |
| AE2-Lightning-Tech | https://github.com/bfzds/AE2-Lightning-Tech | `1d4589b6bd50672051f78d766505530beacfebc0` | 2.1.0-beta.5, MC 1.21.1 |
| AE2LT-Packaged-Pattern-Provider | https://github.com/bfzds/AE2LT-Packaged-Pattern-Provider | `1d3f183ecf258aff0001567d742ed30d6038d77b` | 1.2.0-beta.1, MC 1.21.1 |

The latter declares AE2LT **2.1.0-beta.1** and Thunderbolt **2.0.0-beta.1** dependencies. The two reference checkouts must not be presented as a tested compatible release pair. Initial discovery copies from gjmhmm8 and QianChang-official remain available through each checkout's `origin` remote; `backup` identifies the repositories above.

## License scope

Evidence is pinned to the commits above, not to a distribution site's general license label.

| Category | AE2 Lightning Tech | Packaged Pattern Provider |
| --- | --- | --- |
| Java | Root LICENSE: LGPL 3.0 | Root LICENSE and mod_license: LGPL 3.0 |
| Textures, GUI textures, item textures, block textures, icons | LICENSE_ASSETS.md explicitly covers textures and other visual assets: CC BY-NC-SA 3.0 | Root LGPL 3.0 is the available repository-wide grant; no separate asset license found in this checkout |
| Models | Visual assets fall within the AE2LT visual-assets grant | Root LGPL 3.0; models reference AE2LT and Minecraft parents, which do not become LGPL by reference |
| lang | No explicit lang-specific statement found; root LGPL is the general grant, subject to any file-specific provenance | Root LGPL 3.0; no lang-specific exception found |
| Other assets | Classify individually: visual assets use CC BY-NC-SA; nonvisual data use the general grant absent exceptions | General root grant absent exceptions; externally referenced assets retain their own license |

The PP asset conclusion is an interpretation of its repository-wide license, not an explicit asset-specific declaration. Neither project license grants ownership of third-party material. AE2, Minecraft and optional-mod item art referenced by models must not be copied under the addon's license. In particular, GUI dependencies living in the `ae2` namespace are not automatically AE2-authored or covered by AE2's code license.

The user explicitly accepted noncommercial distribution and preservation of the asset license on 2026-09-21. AE2LT visual files retain CC BY-NC-SA 3.0: attribution, license link, noncommercial use, and ShareAlike for adaptations. They are not relicensed under GTNG's root MIT license. LGPL-derived code/resources require the applicable notices and corresponding source when distributing binaries; retain GPL 3.0 together with LGPL 3.0. Source availability must be fulfilled for a release, not merely asserted by a notice.

Author attribution from the checked-out metadata: AE2LT — MOAKIEE, CystrySU, gjmhmm8, _leng, TedXenon, MHanHanBing; PP — MOAKIEE, gjmhmm8. Original notices remain intact in reference. Selected notices are also included in `src/main/resources/META-INF/ae2lt-port/` for packaging.

Authoritative license links:

- https://creativecommons.org/licenses/by-nc-sa/3.0/legalcode
- https://www.gnu.org/licenses/lgpl-3.0.html
- https://www.gnu.org/licenses/gpl-3.0.html

## Actual copied files

`src/main/resources/META-INF/ae2lt-port/ASSET_MANIFEST.json` records each source project, full commit, source path, destination, license, modification flag and SHA-256. All 25 selected files are byte-for-byte copies; only paths changed. They include the provider background, core and block textures, connector animation and metadata, frequency page backgrounds and buttons, and inherited AE2 GUI art (including three priority button sprites). No `.lang` files or unchanged upstream Java classes have been copied into GTNG. No new artwork has been drawn. Adapted Java is separately recorded in `META-INF/ae2lt-port/CODE_PORT_NOTES.md` and retains LGPL-3.0.

### Inherited AE2 dependency

PP build.gradle declares Curse Maven artifact `applied-energistics-2-223794:7027323`. Its JAR metadata identifies AE2 **19.2.17**. Official tag `neoforge/v19.2.17` resolves to **79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a**. The exact JAR was downloaded for inspection into `reference/dependencies` (excluded from Git and builds): SHA-256 `460d779a0609b81409907d9956de8f6f70a1b0912257e3e5c3c7e75ac9630e95`.

The official README at that commit explicitly assigns textures/models to CC BY-NC-SA 3.0 and text/translations to CC0. Java implementation headers specify LGPL-3.0-only. API licensing is separate (MIT). The README, LGPL text and relevant GUI source excerpts are retained under `reference/dependencies`; these excerpts are reference material only. AE2's original README/license and art attribution also accompany the production textures.

`Icon.java` uses `guis/states.png` (toolbar background UVs: normal 176,128; focus 194,128; hover 212,128; all 18×20). `UpgradesPanel` uses `guis/extra_panels.png`, 128×128, with 18-pixel slots and 5-pixel padding. Both original atlases are now copied, avoiding a substitute GTNH visual style. `common/player_inventory.json` specifies inventory left=8,bottom=84, hotbar left=8,bottom=26 and inventory title left=8,bottom=95. `common/common.json` places the vertical toolbar at left=3,top=1.

FrequencyScreen uses a 195×157 crop of each 256×256 page texture. Tabs are 22×22 at 22-pixel pitch. List pages show five rows, search pages four; rows start at (9,38), are 160×20 with 21-pixel pitch. Idle/hover row art is embedded at v=158/180. Scrollbar starts at (175,38), handle 12×15. All four page PNGs and associated custom tab icons have been copied without raster changes.

## GUI source map and measured layout

PP package prefix: `src/main/java/com/moakiee/ae2lt/packaged/`.

- `client/PPClientScreens.java` loads `/screens/packaged_pattern_provider.json`.
- `client/PackagedPatternProviderScreen.java` extends AE2 `PatternProviderScreen`, adds frequency binding and auto-return to the left toolbar, hides unsupported toolbar buttons, and attaches an AE2 `UpgradesPanel` for the core.
- `menu/PackagedPatternProviderMenu.java` adds the adapter slot, rejects non-core items, caps its stack at one and disallows dragging. Auto-return changes are server-side.
- `client/PackagedProviderTextureButton.java` uses 16×16 AE2LT foreground icons and inherited AE2 toolbar normal/hover/focus backgrounds. Background rendering is 18×20; hover shifts the foreground down one pixel; disabled opacity is 0.5.
- `src/main/resources/assets/ae2/screens/packaged_pattern_provider.json` defines a **176×245** background crop from a **256×256** PNG. Do not stretch the whole PNG to panel size.

| Element | Upstream position / behavior |
| --- | --- |
| Title | (8, 6) |
| Patterns label | (8, 31) |
| Pattern items | (8, 42), 9 columns, 18-pixel pitch |
| Pattern capacity | 36 total in StablePatternProviderLogic; one 9×4 page |
| Return label | (8, 116) |
| Return items | (8, 127), horizontal |
| Core item | (178, 78), external right-hand panel |
| Core panel | right=2, top=72, inherited UpgradesPanel geometry |
| Priority button | (152, -5), 20×20 |
| Lock reason | (5, 15) |
| Player inventory | inherited common/player_inventory.json, must resolve against matching AE2 version |

This PP version does **not** declare additional pattern pages or an inline target-management page in its screen/menu. Do not invent these and describe them as 1:1 upstream behavior. Frequency binding opens `FrequencyApi.openBindingScreen(menu)`; AE2LT's `client/gui/FrequencyScreen.java`, its menu and style files must be audited alongside the inherited AE2 screen before the complete GUI is implemented. The main background has been inspected at native resolution; this is not in-game validation.

## Provider block rendering

`models/block/wireless_packaged_pattern_provider.json` inherits the packaged provider's cube-all model. That model uses `textures/block/overload_packaged_pattern_provider.png`. The wireless blockstate maps **all seven push_direction values to that same cube-all model**. An oriented model also exists and inherits `ae2lt:block/overloaded_pattern_provider_oriented`, but it is not used by those wireless blockstate variants. Do not invent directional art merely because an unused model exists. The wireless cube can therefore preserve its actual visual result through 1.7.10 six-face icon rendering using the copied texture. JSON model files cannot simply be dropped into Minecraft 1.7.10 and expected to render.

## Connector behavior

AE2LT files: `item/OverloadedWirelessConnectorItem.java`, `network/WirelessConnectorUsePacket.java`, `logic/wireless/support/WirelessConnection*.java`, `client/WirelessConnectorRenderer.java`.

- Original animation PNG and `.png.mcmeta` have both been copied. Frame durations are 20, 3, 3, 3, 3 ticks with frame indices 0, 1, 2, 3, 0.
- Selection compound: `SelectedProvider`, with `Dim`, packed-long `Pos`, and `HostType`; provider host type is `provider`.
- Air use clears selection while preserving unrelated item data.
- Server checks held tool, loaded chunk and interaction reach before changing selection/links.
- The audited beta.5 handler selects a clicked provider without requiring Shift. **GTNG must follow the user's explicit contract instead: normal right-click provider to select; Shift-right-click target to bind.** Record this as an intentional adaptation rather than falsely attributing it to that upstream revision.
- 1.7.10 integer dimension IDs need an explicit representation/migration policy; do not claim byte-identical NBT across versions with resource-key dimensions.

## Core framework and TC4 extension

- `item/PackagedCoreDefinition.java`: central definitions map each core to a target mod and target item.
- `client/PackagedCoreItemRenderer.java`: draws `provider_core_base.png`, then the target's own item rendering. Target center is (0.78, 0.22, 0.57), scale 0.45, depth scale 0.02.
- Magic/ritual cores exist for Ars, Mystical Agriculture, Malum, Occultism and Botania. They share that same core base and overlay mechanism. There is no TC4 core in the audited definitions.
- TC4 should reuse the base and render the installed Thaumcraft infusion-matrix item as the target overlay. This avoids inventing a replacement visual system or redistributing Thaumcraft artwork.
- `logic/multiblock/MultiblockAdapter.java` and its registry/binding types are the integration seam. `PackagedPatternProviderLogic` caches recipe bindings, rotates real target candidates, applies lane retry delays (1,2,3,4,5,8,10,20,40 ticks) and polls real-lane returns every 20 ticks. Virtual processing is distinct from real altar execution.
- GTNH AE2 `rv3-beta-1050-GTNH` exposes ICraftingProvider/ICraftingMedium and MENetworkCraftingPatternChange. TC4 4.2.3.5 exposes TileInfusionMatrix.craftingStart(EntityPlayer), validLocation(), and real pedestal/essentia processing. Research, altar structure and essentia must not be silently bypassed by the port.

## Build boundary and outstanding work

Only selected production resources live under `src/main/resources`. Reference checkouts must remain outside Java/resource source sets, Gradle included builds and distributable JARs. Their own build scripts are never executed by this port.

Implemented: original cube/icon rendering; original 176x245 main GUI and slot geometry; server-synchronized auto-return, priority and target management; 36 processing patterns, nine persistent returns and a locked-while-active core slot; AE crafting-medium integration; Normal-select / Shift-bind Connector with reach/permission checks; extensible core registry; bounded round-robin scheduling/backoff; TC4 recipe/research matching and real matrix execution. The TC core uses the original base plus the installed matrix item. The original priority sprites retain their three-pixel nine-slice border in MUI. External core/toolbar widgets reserve NEI exclusion areas.

Final-scope additions: all five AE2 crafting-lock modes and original atlas icons; persisted redstone edge/result waiting conditions; the original terminal-visibility toggle backed by GTNH's IInterfaceViewable registry, exposing only the 36 pattern slots; original 12x15 scrollbar at (175,38), five-row pitch, return-item tabs and one-pixel toolbar hover shift. Subpages hide the main panel rather than layering both backgrounds. The priority button uses a 20x20 tab background. New TC-only recovery is Shift-right-click on a direct-target row: it refuses running/unloaded lanes and lanes with an uncollected expected output, then only releases the interrupted receipt, never altar contents or refunds. The AE CPU request must be cancelled separately.

User-approved GTNG acquisition recipes require an AE interface, wireless receiver and base core for the provider; engineering processor/fluix/iron for the base core; fluix/wireless receiver for the connector; and a real TC infusion matrix plus base core for the TC core. These are GTNG-specific recipes, not attributed to upstream.

The complete frequency/member/search/create system is explicitly outside the selected scope. The direct-target list uses original artwork but different content and is not called a full upstream frequency page. The two preserved upstream versions remain an unverified release pair; no running high-version side-by-side comparison or pixel-identical claim is made. Dedicated multiplayer and long-term instability stress testing remain unverified.

Validation: `verify-port-resources.ps1` checks all 25 source hashes and PNG dimensions. `scripts/run-packaged-qa.ps1` runs two separate real clients against a disposable test save. Checks cover recipes, binding/duplicate rejection, AE pattern publication, research/occupied/inactive-altar rejection, real matrix start, no double dispatch, protected core extraction, saved jobs/inventory/locks, sustained versus rising-edge redstone, actual save-close-restart, consumption of eight AIR essentia from a real jar, exact-once collection, storage backpressure, result unlock only after actual ME insertion, interrupted receipt recovery preserving altar items, no forced chunks, all three GUI screens, and server-synchronized lock/visibility buttons. First-stage SAVED and second-stage PASS markers are required, preventing a clean process exit from masking missing tests. Native screenshots are saved under `run/client/screenshots/packaged-*-qa.png`. Test classes/recipes and reference checkouts are excluded from release JARs.

The harness compiles only its own opt-in package and snapshots main/test outputs so unrelated concurrent compilations cannot delete running-client classes. A same-process world reconnect exposed an unrelated CoFH OreDictionaryArbiter login race; testing across a full process restart avoids that race and checks actual disk persistence. A GUI regression exposed an initially missing MUI C2S permission on the scrollbar; the scrollbar and editable priority value now explicitly enable client requests, while authoritative status fields remain server-only. Slider bounds update only when the target count changes. Tests exercise scrolling then removing the correct server-side row, priority buttons and typed-value synchronization, not only panel opening. Development logs also contain unrelated pack warnings and must not be described as warning-free.

The initial build blocker was Windows JDK 25 Unix-domain selector initialization. A command-local `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing` (nonexistent directory) makes the selector fall back to TCP; no global environment or starter build file was changed. Usage, limitations and opt-in test invocation are documented in `docs/packaged-provider.md`.

## Direct AE essentia extension (2026-09-21)

New GTNG code connects real TC4 per-unit consumption to the owning Provider's AE
essentia storage. API reference: GTNewHorizons/ThaumicEnergistics 1.7.60-GTNH,
commit fbefd7b; no ThE implementation or artwork copied. The existing licensed
frequency_connect.png and button atlas are reused unchanged for source selection.
The original extra_panels.png slot starts at U=0; the local core slot's extra
five-pixel horizontal inset was removed to align the icon with that original art.

## Held wireless connector overlay (2026-09-21)

Source: AE2 Lightning Tech 2.1.0-beta.5, commit
`1d4589b6bd50672051f78d766505530beacfebc0`, LGPL-3.0, files
`client/WirelessConnectorRenderer.java`, `WirelessConnectorRenderFilter.java`,
and `Ae2ltRenderTypes.java`. AE2 19.2.17 commit
`79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a` supplies the inspected
`client/render/overlay/OverlayRenderType.java` state definitions (LGPL-3.0-only).
Local destination: `src/main/java/com/xyp/gtnotgood/client/packaged/WirelessConnectorRenderer.java`;
all modified for Forge 1.7.10 Tessellator/GL, retaining original colors, inner
cube, bound faces, line endpoints and selection filtering. Provider description
packets synchronize only bounded binding coordinates. No new visual assets.
The existing direct-binding scope has no offhand or Ctrl-group binding;
preview follows its existing whole-block duplicate rule. Packaged provenance:
`META-INF/ae2lt-port/CODE_PORT_NOTES.md`.

Connector overlay validation: compileJava and focused compileTestJava passed. A real Forge client in a disposable world captured all six held/selection/preview/unheld/unbind/rebind states and emitted PACKAGED_CONNECTOR_SYNC_PASS; client binding counts were 1/1/1/1/0/1. Screenshots: run/client/screenshots/packaged-connector-{1..6}.png. The coordinate-only packet was checked not to include other provider state. No dedicated-server or high-version side-by-side visual comparison was performed.

## GTNL text-effects port (2026-09-21)

Independent of the AE2LT port above. Source: https://github.com/ABKQPO/GT-Not-Leisure/tree/52345d2c059c871febec1365d6012424c7d64547 . Requested commits: d28217f, 9bd2f59, 52345d2. Inspected upstream LICENSE.txt (LGPL-3.0) and bundled MIT shader notices before copying. Source/destination/modification records and licenses are packaged in src/main/resources/META-INF/text-effects-port/NOTICE.md; shader hashes are in asset-manifest.json. Reference downloads reside in the OS temporary directory and are not compiled or packaged.

Coexistence correction (2026-10-06): GTNL retains ownership of shared font hooks, while
`/gtngtexteffects` and its wireless preview remain registered. GTNG maps its preset namespace to
GTNL only when encoding displayed tooltips, previews and HUD text. Stored renderer IDs, palette,
speed, bold and italic settings remain independent; no new upstream source or assets are copied.
Verified with GTNG alone and the complete published GTNL `0.2.7-rc1` development jar plus
Angelica: both command registrations, GTNG machine/wireless preview dispatch, all twelve preset
IDs, immediate preference application, saved preference reload, tooltip/HUD rendering and resource
reload passed. Two focused namespace/palette/speed JUnit checks passed. Hidden-client screenshots
and results are in ignored `build/text-effects-qa/{angelica,gtnl}`.

## GTNL wireless energy cover recipes (2026-10-16)

- Source: `AssemblerRecipes#loadWirelessHatch` and `utils/item/ItemUtils` from
  https://github.com/ABKQPO/GT-Not-Leisure at `2abe879191443af5db2ae653a9087d34ad0432dc`.
- Inspected `LICENSE.txt` (LGPL-3.0); packaged license and provenance are in
  `src/main/resources/META-INF/licenses/GT-Not-Leisure-NOTICE.md` and its linked license files.
- Destination: `common/recipe/gregtech/WirelessEnergyCoverRecipes.java` under `com.xyp.gtnotgood`.
- Retained the fourteen native 1 A cover recipes, costs and solder alternatives. Registration uses
  the GT builder at load complete, after ore unification, and is skipped when GTNL is installed.
- Original GT recipes remain. GTNL-only 4 A covers, hatches, disassembly maps and assets are excluded.
- Local reference checkouts remain outside compilation and resource source sets.

## GTNL compact AE machines (2026-09-22)

- Source: https://github.com/ABKQPO/GT-Not-Leisure
- Commit: `6cbc6927af4f44c445ea7a879796b4764b00988d`.
- License: LGPL-3.0; upstream LICENSE.txt copied verbatim to
  `src/main/resources/META-INF/licenses/GT-Not-Leisure-LGPL-3.0.txt`.
- Derived Java files carry SPDX notices; they are not relicensed under this project's general MIT grant.
- Upstream destinations preserve the source-relative filenames below under `com.xyp.gtnotgood`:
  `common/machine/multiblock/{QuantumComputer,AssemblerMatrix}` -> `common/machines/multiblock/`;
  `common/gui/modularui/{QuantumComputerGui,AssemblerMatrixGui}` -> `common/gui/modularui/`;
  `utils/{ECraftingCPUCluster,DireCraftingPatternDetails,LargeInventoryCrafting}`;
  `utils/machine/AssemblerMatrixPatternState`; `utils/crafting/{CraftingBatchPlanner,CraftingBatchPlannerImpl}`;
  `mixins/late/appliedEnergistics/{MixinCraftingCPUCluster,AccessorTaskProgress,AccessorSessionCraftCount}`
  and `quamtumComputer/MixinCraftingGridCache` -> `mixins/late/AppliedEnergistics/compact/`;
  `mixins/early/minecraft/MixinInventoryCrafting` -> late compact mixin on AE's MEInventoryCrafting.
- Adaptations: local GTNG base, registries, GUI textures and Java 8 APIs; ore-processor-style
  3x2x1 StructureLib shape; fixed 576 pattern slots; fixed singularity CPU and maximum matrix parallel;
  one-tick matrix cycles; no controller, dispatch or storage-transfer energy cost; overflow,
  interrupted-output and persistence fixes. No upstream raster assets copied.
- Translation strings adapted from upstream English/Chinese resources into adjacent Java #tr comments.
- Full upstream checkout remains in the OS temporary reference directory, outside all source sets.

## LDLib extended controls — 2026-09-26

Source: user-supplied `../LDLib2-1.21`, version 2.2.40, Low-Drag-MC/LDLib2,
https://github.com/Low-Drag-MC/LDLib2, LGPL-3.0 (read before implementation).
No Git metadata exists in this snapshot; source SHA-256 hashes are pinned in
`src/main/resources/META-INF/ldlib-port/CONTROLS_MANIFEST.json`.

Destination: `src/main/java/com/xyp/ldlib/gui/ui/elements/` for ProgressBar, Slider,
SearchComponent, Menu, Dialog, TreeList, SplitView, VirtualScrollerView, ColorSelector,
and GraphView. Scene is explicitly excluded at the user's request.

Changes: modern layout/style/editor/RPC integrations replaced by host fixed bounds,
client-side callbacks, LWJGL2/Tessellator rendering and the existing MUI2 boundary.
Added ModalLayer and modal focus isolation in UIInput; extended ScrollerView with
thumb dragging and model extent overrides. No new upstream art copied. Existing theme
assets keep their existing licenses. Fixed-height virtualization, single-selection trees
and textured graph nodes are the supported adaptation scope; full editor/XML, variable
row-height virtualization and graph execution are not claimed. The upstream directory
remains outside compilation/resource inputs. Validation details: docs/testing/ldlib-controls.md.

## Arcane workbench core — 2026-09-26

- Existing core base and renderer inspected in the pinned AE2LT-Packaged-Pattern-Provider checkout,
  commit `1d3f183ecf258aff0001567d742ed30d6038d77b`, `LICENSE` (LGPL-3.0).
- Destination: existing `textures/items/packaged/provider_core_base.png` and `PackagedCoreRenderer`.
  The base PNG and overlay geometry remain unchanged. The arcane core renders the installed
  Thaumcraft 4.2.3.5 `blockTable:15` item; no Thaumcraft texture/source code is copied or redistributed.
- Existing upstream connection-list row gains a server-authorized left-click capture action.
  Its layout and artwork remain unchanged; source/asset license records above still apply.
- New native API integration is implemented in `ArcaneWorkbenchAdapter` and `ArcaneWorkbenchPatterns`.
  Local dependency source was inspected for recipe matching, wand units/cost and table metadata.
- Reference checkouts remain outside compilation and resource packaging.

## Large Transmutation Machine — 2026-09-26

- Pinned upstream: ABKQPO/GT-Not-Leisure dev-290, `f1b74060d2a91b422eb950076075c601a644f882`.
- Read LICENSE.txt (LGPL-3.0), ShimmerRecipes, DisassemblerHelper, ReversedRecipeRegistry and crafting mixins.
- Ported blacklist/material transformations and recipe-source selection. Full source/destination,
  modification and license records: `src/main/resources/META-INF/shimmer-port/NOTICE.md`.
- Installed GTNL is loaded first and supplies its real conversion table, including hard overrides.
- 2026-10-06, issue #4: an empty GTNL RC1 table now falls back to local recovery generation;
  disabling Shimmer no longer disables this powered machine. GTNL settings and its table stay unchanged.
- Verified published GTNL 0.2.7-rc1 `LoadCompleteRecipeScheduler`: conversions are deferred until
  the first client/server END tick. GTNG now loads once at LOWEST priority after that scheduler,
  unregisters the hook and refreshes GregTech's NEI cache. No GTNL scheduler source was copied.
- Adaptations: EV power, exact batches, native fluid output hatches, GT NBT matching/output protection.
- Standard machine GUI and NEI only; no dedicated LDLib screen or browser button.
- Structure uses existing GT/BartWorks/vanilla blocks; no upstream artwork is copied.
- Reference checkouts remain excluded from compilation and resource packaging.

## Crucible core — 2026-09-26

- Inspected pinned AE2LT-Packaged-Pattern-Provider renderer and LICENSE (LGPL-3.0), commit
  `1d3f183ecf258aff0001567d742ed30d6038d77b`, before reusing the existing core rendering.
- Destination: existing `PackagedCoreRenderer` and `textures/items/packaged/provider_core_base.png`.
  Base PNG, geometry and provider layout remain unchanged; the new overlay renders installed TC4
  4.2.3.5 `blockMetalDevice:0`. No TC artwork or implementation source is copied into the addon.
- Inspected the pinned TC4 dependency source for crucible recipe selection, research, heat,
  water consumption, aspect removal and post-reaction decay delay. New integration uses native APIs.
- Existing asset licenses remain applicable; usage is recorded in the packaged asset manifest.
  Reference checkouts remain outside compilation and resource packaging.

## Working Apiary / 工作蜂箱 (2026-09-27)

- Source: https://github.com/GTNewHorizons/ForestryMC, tag `4.11.38`, commit `0bd9a7978a3852c3a815e41a16e678176344aa3d`.
- Inspected the matching Gradle sources artifact (SHA-1 `7434384c6402bc8669354baff0ae61dc0ce781c1`) and the pinned repository `LICENSE.txt` before implementation. Upstream implementation license: LGPL-3.0; API source headers identify MIT.
- Destination: `src/main/java/com/xyp/gtnotgood/common/blocks/beekeeping/` and `mixins/late/Forestry/MixinWorkingApiaryProducts.java`.
- Integration: subclass native `BlockApiculture`/`TileApiary`, delegate texture lookup to the installed apiary, reuse `ItemBlockForestry`, native containers, GUI, bee logic, inventories and packets. Independent machine definition and registry entry; do not mutate upstream definitions.
- Modifications: separate name, server clock multiplier, successful ordinary/specialty product quantity multipliers, lifespan/mutation modifiers and conversion recipe. Defaults: 1x speed/mutation, 5x ordinary/specialty quantities (preserved concurrent workspace edits), user-requested 2,147,436x lifespan.
- Assets: no upstream textures, icons, GUI files or source implementations are copied or packaged. Runtime assets stay in the separately installed Forestry mod and retain its license. Resource-pack substitutions remain effective. Reference sources were extracted outside the project source sets and are not compiled or packaged.


## Comb Processor / 蜂窝处理机 (2026-09-27)

- Source: sibling GT-Not-Cool checkout, HEAD `65222fcd4a2f8a56c146323361ee527b9b372d23`; inspected local `LargeCombProcessor.java`, `CombProcessingRecipes.java`, recipe map and assembler recipe before copying.
- License: MIT; source notice preserved in `src/main/resources/META-INF/licenses/GT-Not-Cool-MIT.txt`.
- Destinations under `src/main/java/com/xyp/gtnotgood/`: `common/machines/multiblock/LargeCombProcessor.java`, `common/recipe/gtnotgood/CombProcessingRecipes.java`, `common/recipe/machine/LargeCombProcessorRecipes.java`.
- Registration: GTNGItemList, machine ID 28517, own recipe map and NEI controller icon; imports run once at Forge load-complete after native recipe registration.
- Adaptations: shared GTNG electric base/modern GUI, long EU/t accounting, native solid steel casing (sBlockCasings2:0), existing singularity overlays. LV assembler replaces unavailable SteamAssemblerModule; other controller ingredients and 100 ticks/32 EU/t retained.
- Retained recipe policy: centrifuge priority, chemical comb-to-dust conversion without acid, fluid extraction fallback, 30 EU/t recipe entries, 10% processing duration/EU modifier and perfect overclocking.
- Cross-recipe processing retains summed duration/EU/t, with remaining-power checks and a 256-group bound. Output protection uses a single native transaction; cross-recipe grouping requires both output types to allow overflow. Fluid outputs remain separate stacks to avoid integer overflow.
- Structure: original 15 x 17 x 15 arrangement; short source rows padded on the right, preserving block positions. Controller offset (7, 8, 0).
- No new visual assets copied. Sibling/reference source trees are not compilation or resource inputs.

## Personal wireless network HUD (2026-10-06)

- Functional reference: https://github.com/MIAOKATZE/GT-Simple-Wireless-Network, inspected HEAD
  `c311328a32ab58750f928a9e5980a1adc55b2d68`, README, `WirelessMonitorHUD`, `HudController`,
  `EUDataSet`, request/reply packets and `LICENSE` (AGPL-3.0).
- Independently implemented the three-line display from its public behavior using the installed
  GT5U 5.09.54.183 `WirelessNetworkManager` / `SpaceProjectManager` APIs. No upstream implementation
  source, artwork, textures or bundled code is copied or packaged. The reference checkout is under
  ignored `build/upstream-wireless-monitor`, outside all source and resource sets.
- Destinations: `client/wireless`, `common/wireless/monitor`, `common/packet/WirelessMonitor*`.
  Uses the existing local animated-text engine and preview; its existing license records remain applicable.
- Adaptations: available to every player without an item, one configurable toggle key, client-local
  persistent settings, own/team-only server snapshots, exact arbitrary-precision balance arithmetic,
  five-minute rolling net EU/t and animated palettes/speed independent of machine-credit settings.
- Coexistence: distinct packages, namespaced translations, original GTNG packet channel and client
  configuration, no upstream mixins or registrations. Default position is at the bottom left, away from
  the upstream monitor's food-bar anchor. Original GTSWN functionality is not intercepted.
- QA uses the published `gtswn-v1.8.16-dev.jar` as an opt-in runtime dependency only; it is never
  embedded or added to production dependency declarations.
- Verified: formatting/compilation/resources, eight focused JUnit checks and hidden client integration
  checks with GTSWN v1.8.16 installed. Empty-inventory activation, charging/discharging, disabled polling,
  saved effect settings, independent machine-credit settings, both HUDs visible and upstream-only HUD
  after disabling GTNG all passed. Screenshots/results are in ignored `build/wireless-monitor-qa`.
- Position editing: inspected the installed GT5U 5.09.54.183 power-goggles keybind and HUD-config
  interactions as a behavior reference; no source or assets copied. Independent Home-key editor
  drags the complete HUD using its shared render bounds, clamps it inside the screen, resets its
  position and saves existing X/Y offsets on exit. It does not change the goggles or GTSWN settings.
- Additional hidden-client QA passed: edit key opens/closes, drag offset preserved, saved position
  reloads, out-of-screen dragging clamps, reset persists, Esc exits, and both mods continue to coexist.

## Large ME dual interface — 2026-10-07

- Native references: GTNH Applied Energistics 2 `rv3-beta-1073-GTNH`, commit
  `151550f6d558a663eee792f0bfa22e12a53c0e54`, LGPL-3.0-or-later; AE2FluidCraft-Rework
  `1.5.110-gtnh`, commit `6e8dd013bd4ec123cf74daaecf581534d87f2391`, LGPL-3.0.
  Inspected packaged sources and AE2FluidCraft `LICENSE.txt` before using its interfaces.
- Destinations: `common/blocks/largeinterface`, `common/parts/largeinterface`,
  `common/items/largeinterface`, `client/largeinterface`, plus scoped late mixins.
  Native `TileFluidInterface` / `PartFluidInterface` behavior is inherited; pattern
  inventory grows to 900, native priority bands grow accordingly, and permanently
  empty item/fluid configuration inventories forbid stock requests after NBT or
  memory-card imports. Native item/fluid product return paths remain active.
- GUI middle layout reuses this project's `SuperMTEHatchCraftingInputMEGui`:
  9 columns, 4 visible rows, vertical scrolling. Native left-side settings,
  priority and upgrade slots remain; hatch tools, manual slots and ghost mold
  slots are omitted. The source layout and native setting-icon indices adapted
  for `LargeInterfaceRenderer` and `LargeInterfaceGui` are LGPL-3.0-or-later.
- Runtime assets: AE2FC `interface/fluid_interface_Orange` and its colored front,
  alternate and arrow textures; AE2 `guis/states` setting icon atlas. No upstream
  PNGs are copied or packaged. The installed dependencies supply those assets.
- Packaged provenance: `META-INF/large-interface-port/NOTICE.md`; existing LGPL
  and GPL license texts in `META-INF/ae2lt-port`. Reference extraction under
  ignored `build/qa/interface-source` never enters compilation or resources.

## Current local package layout — 2026-10-07

The structure refactor relocated host Java sources by responsibility. Earlier dated
entries retain their original local paths; upstream source paths, pinned revisions,
licenses and resource provenance are unchanged. Paths in this table are relative to
`src/main/java/com/xyp/gtnotgood/`.

| Previous local location | Current local location |
| --- | --- |
| `common/packaged/`, except the item, packet and recipe classes below | `common/blocks/packaged/` |
| `common/packaged/ItemPackagedCore.java`, `ItemWirelessConnector.java` | `common/items/packaged/ItemPackagedCore.java`, `ItemWirelessConnector.java` |
| `common/packaged/MessagePackagedConnector.java` | `common/network/packaged/MessagePackagedConnector.java` |
| `common/packaged/PackagedRecipes.java` | `common/recipe/gtnotgood/PackagedRecipes.java` |
| `common/advancedio/`, except its item and recipes | `common/parts/advancedio/` |
| `common/advancedio/ItemAdvancedIOBus.java` | `common/items/advancedio/ItemAdvancedIOBus.java` |
| `common/advancedio/AdvancedIORecipes.java` | `common/recipe/gtnotgood/AdvancedIORecipes.java` |
| `common/mestock/`, except the item and requester block classes below | `common/parts/mestock/` |
| `common/mestock/ItemStockPart.java` | `common/items/mestock/ItemStockPart.java` |
| `common/mestock/BlockMERequester.java`, `TileMERequester.java`, `ItemRequesterBlock.java` | `common/blocks/mestock/BlockMERequester.java`, `TileMERequester.java`, `ItemRequesterBlock.java` |
| `common/compass/` | `common/items/compass/` |
| `common/beekeeping/` | `common/blocks/beekeeping/` |
| `common/patternsorter/`, except its recipes | `common/items/patternsorter/` |
| `common/patternsorter/PatternSorterRecipes.java` | `common/recipe/gtnotgood/PatternSorterRecipes.java` |
| `common/flux/`, `common/mecontainer/` | `common/blocks/flux/`, `common/blocks/mecontainer/` |
| `common/mebridge/`, except the item and packet classes below | `common/blocks/mebridge/` |
| `common/mebridge/ItemMEWirelessTransceiver.java`, `MEWirelessTransceiverGui.java` | `common/items/mebridge/ItemMEWirelessTransceiver.java`, `MEWirelessTransceiverGui.java` |
| `common/mebridge/MessageMEWirelessNodeAction.java`, `MessageMEWirelessVisualization.java` | `common/network/mebridge/MessageMEWirelessNodeAction.java`, `MessageMEWirelessVisualization.java` |
| `common/user/`, except its speed upgrade item | `common/blocks/mechanicaluser/` |
| `common/user/ItemUserSpeedUpgrade.java` | `common/items/mechanicaluser/ItemUserSpeedUpgrade.java` |
| `common/network/` programmable network sources | `common/blocks/network/` |
| `common/packet/` | `common/network/` |
| `common/machines/basicMachine/` | `common/machines/basic/` |
| `mixins/late/AppliedEnergistics/`, `Gregtech/`, `Forestry/` | `mixins/late/appliedenergistics/`, `gregtech/`, `forestry/` |

The other late mixin package segments formerly named `Accessor`, `BloodMagic`,
`CropsNH`, `CutCorners`, `EOH`, `EnderIO`, `FOG`, `Railcraft`, `SpiceOfLife`,
`TreatedWater`, `Thaumcraft` and `WarpTheory` now use their all-lowercase forms.
The public provider extension API is
`com.xyp.gtnotgood.common.blocks.packaged.PackagedCoreRegistry.Adapter`;
its `TilePackagedProvider` and `PackagedTarget` signature types share that package.
External integrations using the former `com.xyp.gtnotgood.common.packaged` API
must update their Java paths and rebuild. The corresponding source-license notice
in `META-INF/ae2lt-port/CODE_PORT_NOTES.md` covers all relocated port sources.

## Large ME dual interface appearance — 2026-10-08

- Retains MUI2, the real 900-pattern inventory, 9-column/4-visible-row scrollable
  grid, native settings, four upgrade slots and synchronized priority popup.
  Replaces the GUI's GT default material with the existing modern AE2 resources;
  the orange interface block render and GTNH setting semantics remain intact.
- Official AE2 reference: version 19.2.17, tag `neoforge/v19.2.17`, commit
  `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`, inspected `PatternProviderScreen`,
  `AEBaseScreen`, `Icon`, `VerticalButtonBar`, `UpgradesPanel`, screen JSON layouts
  and README license declarations. Official source:
  https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a
- Visual sources are the already packaged AE2 19.2.17 `priority`, `states`,
  `extra_panels`, `button`, `button_highlighted` and `big_scroller` PNGs. The
  slot-free priority background provides a two-pixel nine-slice main frame.
  Existing source URLs, author credits and CC BY-NC-SA 3.0 declarations remain
  in `META-INF/ae2lt-port`.
- The priority text field shares the existing `textures/gui/me_stock/text_field`
  atlas, pinned to official AE2 `v26.1.12-beta`, commit
  `b7cf5822d9c128a61d9291cb2c1f92319253e4f0`; its original source and license are
  recorded in `META-INF/me-stock-port`. Unfocused/focused crops are
  `(0, 0, 128, 12)` and `(0, 24, 128, 12)` with two-pixel adaptable borders.
- Destination: `common/blocks/largeinterface/LargeInterfaceGuiTextures.java` and
  `LargeInterfaceGui.java`. The texture adapter composes cropped existing PNGs
  for the frame, slots, settings sidebar, upgrade panel, buttons and scroll handle.
  No PNG is changed or duplicated; shared file hashes match the packaged manifest.
  Runtime crop/composition adaptations retain the artwork's CC BY-NC-SA 3.0 terms.
- Central pattern slots combine the modern AE2 slot surface with the installed
  GT5-Unofficial 5.09.54.183 MUI2 `PatternSlot` empty-pattern overlay,
  `GTGuiTextures.OVERLAY_SLOT_PATTERN_ME` at
  `gregtech:textures/gui/overlay_slot/pattern_me.png`. No GT PNG is copied; the
  installed dependency remains its source and retains its upstream resource terms.
- The upgrade-frame drawable repaints four modern `SLOT` backgrounds after the
  frame at offsets `(4, 5 + slot * 18)`, each 18x18. These share the widget drawing
  layer, preventing the frame from covering MUI2's prior slot-background pass;
  the real upgrade widgets retain item rendering and hover behavior.
- Exact shared-source paths, crop rectangles and SHA-256 hashes are recorded in
  `META-INF/large-interface-port/ASSET_MANIFEST.json`; its `NOTICE.md` supplements
  the existing AE2LT notices. This artwork is outside the project's general MIT grant.
- Official reference downloads remain under ignored
  `build/reference/ae2-19.2.17-gui`, outside all source and resource sets.

## Large ME dual interface dark default and pattern hints — 2026-10-08

- Dark visual source: Ridanisaurus, **AE2 Dark Mode 1.0.6**, Minecraft 1.21,
  published July 10, 2024. Author's release page:
  https://www.curseforge.com/minecraft/texture-packs/ae2-dark-mode/files/5520011
  Alternate author distribution:
  https://modrinth.com/resourcepack/ae2-dark-mode/version/BXpYgw04
- This resource pack publishes immutable release/file IDs rather than a source
  commit: CurseForge project `1038055`, file `5520011`; Modrinth project
  `UP2ZKKkg`, version `BXpYgw04`. Source archive `AE2DarkMode-V1.0.6.zip`:
  SHA-256 `a5a721617a5043b53d7223b102626b878661f53931e638b23dcc7ffdb58c1249`;
  the download also matches the publisher's SHA-512 from its version metadata.
- License: **CC BY-NC-SA 4.0**, as declared on both author project pages;
  this differs from the original AE2 default artwork's CC BY-NC-SA 3.0.
  The dark artwork and composition adaptations retain the 4.0 terms and remain
  outside the project's MIT grant. Full text is packaged under
  `META-INF/large-interface-port/CC-BY-NC-SA-4.0.txt`.
- Destination: `assets/gtnotgood/textures/gui/large_interface/`; only required
  GUI PNGs are copied without pixel edits. The feature texture adapter preserves
  AE2 crop geometry, nine-slice borders, upgrade-slot repainting and MUI2 state.
  Main and priority text use the pack palette's `DEFAULT_TEXT_COLOR` /
  `TEXTFIELD_TEXT` (`#f2f2f2`). Other features' shared light textures stay intact.
- The pack has no `button_highlighted.png`. Numeric-button hover reuses its
  authored `states` toolbar hover crop `(212, 128, 18, 20)` with three-pixel
  nine-slice borders; hovered numeric text uses that atlas's dark outline color
  `#0a090e`. The original GT empty-pattern hint receives an independent runtime
  tint from `MUTED_TEXT_COLOR` (`#cbccd4`), restoring GL white after drawing it.
- Pattern slots disable the underlying MUI2/GT theme background so it cannot
  show light borders through the dark sprite's transparent perimeter. Empty
  upgrade slots draw the dark `BACKGROUND_UPGRADE` chip crop `(240, 208, 16, 16)`
  inset by one pixel, after the upgrade frame and before native hover shading;
  occupied upgrade slots continue to display the actual card.
- Empty upgrade tooltips adapt the installed native `GuiUpgradeable` card-cache
  and supported-limit rules using `MaterialType`, `IUpgradeModule` and
  `IUpgradeInventory.getMaxInstalled`. These small source adaptations remain
  LGPL-3.0-or-later under the native AE2 source notice; displayed card limits
  include cards already installed and do not change insertion behavior.
- Pattern hint visibility follows installed native AE2 `AEBaseGui`: an occupied
  interface slot keeps its plain slot background; only empty slots receive the
  GregTech pattern hint. Installed GT `PatternSlot` continues to render the
  native recipe output and count; Ultimate Encoded Patterns inherit the native
  encoded-pattern API, so no alternative fluid format or special decoder is added.
- Reference extraction, project metadata and version metadata stay under ignored
  `build/reference/ae2-dark-mode-1.0.6`, outside compilation and packaged resources.
  The current asset manifest records each copied file and its crop adaptations.

## Wireless dual interface terminal buttons — 2026-10-12

- Visual source: ModernityGTNH/Modernity-GTNH-UI, commit
  `87fee0aac305ca8f1736278266ad87d94deff3b4`,
  `assets/appliedenergistics2/textures/guis/states.png`.
  https://github.com/ModernityGTNH/Modernity-GTNH-UI/tree/87fee0aac305ca8f1736278266ad87d94deff3b4
- The unmodified 256x256 atlas is packaged at
  `assets/gtnotgood/textures/gui/quick_terminal/states.png` for terminal-specific
  button rendering. Its SHA-256 is
  `4fb85eee1c5c86da35b33c4e7d343e3dcd4d5f977aeb01d2a705ca13aa0e3876`.
- Modernity's CC BY-NC-SA 4.0 terms apply. The existing license text is
  `META-INF/ldlib-port/Modernity-LICENSE.txt`; this asset's source, destination
  and hash are recorded in `META-INF/quick-terminal-port/ASSET_MANIFEST.json`.
  The ignored reference download remains outside compilation and packaging.
- Dark visual source: ModernityGTNH/Modernity-GTNH-Dark-UI, commit
  `0322e00ef22e10628ed875154d6d604cc572d08a`, the same
  `assets/appliedenergistics2/textures/guis/states.png` path.
  https://github.com/ModernityGTNH/Modernity-GTNH-Dark-UI/tree/0322e00ef22e10628ed875154d6d604cc572d08a
- The unmodified 256x256 dark atlas is packaged as
  `assets/gtnotgood/textures/gui/quick_terminal/states_dark.png`; source and
  destination SHA-256 are both
  `8602baf1f970ce1b99a5015ffff3786b1d2370fc96c16982319b0324d4432118`.
  Modernity-GTNH-Dark-UI's CC BY-NC-SA 4.0 applies, with full text at
  `META-INF/ldlib-port/Modernity-LICENSE.txt`. The ignored dark reference stays
  outside compilation and packaging, and the asset manifest records its mapping.

## Wireless Frequency Card — 2026-10-12

- Java source: GT-Not-Leisure PR 627, commit
  `651396c75d108e6ff5399a44c56c12214ad27d3d`, LGPL-3.0.
  The adapted item, manager GUI, network packets and wireless backend are placed
  under GTNG `common.items.wirelessfrequency`, `client.wirelessfrequency`,
  `common.network.wirelessfrequency` and `common.wirelessfrequency` respectively.
  AE2 and GregTech adapters are under `mixins.early.wirelessfrequency`,
  `mixins.late.appliedenergistics.wireless` and `mixins.late.gregtech.wireless`.
  The ME Interface name adapter limits the upstream custom-name bypass to target
  metadata collection.
  Changes include package names, GTNG registration, generated translation keys,
  SimpleNetworkWrapper packet routing and GTNH integration.
- GT 5.09.54.183 ownership adaptation (2026-10-13): structure checks record the
  controller's registered input/output hatches, buses and dual-input hatches.
  Presentation keeps these associations for incomplete machines as well as
  formed ones; structure-version changes still invalidate removed associations.
  Shared hatches and physical AE clusters spanning controllers remain separate.
  With type folding enabled, same-type controllers share a controller list;
  each controller opens its own ME devices without flattening their ownership.
  Machine children show their actual ME device names and icons; single-node
  children have no additional entrance-detail layer.
- GTNG multi-source extension: each card saves up to 64 source bindings and an
  automatic-placement preference per source. The upper-left source line opens
  a source selector. The legacy active binding remains readable; selecting a
  source changes the manager and future placement target without moving links.
  Source changes are validated by the server against the saved list, owner,
  current snapshot revision and ME build permission when the source is loaded.
  Source aliases are stored per card and shown alongside coordinates. The source
  selector reuses the rename dialog; renaming preserves network selection and
  automatic-placement settings, including for unloaded sources.
  Source rows also expose deletion of the saved binding without disconnecting
  existing links; removing the active source selects a remaining source or
  leaves the card unbound when the list becomes empty.
- Item art: AE2 Lightning Tech Reborn `overloaded_frequency_card.png`, commit
  `2d8369e1e8b952300ec7ee73837e75cb61c7fc8d`, redistributed by GTNL
  unchanged as `wireless_frequency_card.png`, CC BY-NC-SA 3.0. GTNG copies the
  same bytes unchanged to `assets/gtnotgood/textures/items/wireless_frequency_card.png`.
  Source, destination, modification flag and SHA-256 are in
  `META-INF/wireless-frequency-card/ASSET_MANIFEST.json`.
- GTNL's GUI references `appliedenergistics2:textures/guis/wireless_network_manager.png`.
  GTNH AE2 `rv3-beta-1080-GTNH` already supplies this asset; it is not copied.
  The ignored reference checkout is excluded from compilation and packaging.

## GTNL wireless frequency card performance — 2026-10-19

- Source: https://github.com/ABKQPO/GT-Not-Leisure, five consecutive commits:
  `3e2d43da1260507a29fe674eb90dc8e99553bf83`,
  `0730f502bbe1f3f48e27699ab8085ee8c0d58929`,
  `1ea3990f6d6c3b02fc09e5459e48c2b93c5f357e`,
  `27712ce26fd045526768fef396b16fe57ec56dcc`,
  `831f06ceb172025a91a55ca00deca7c21ea4a6f0` (2026-10-17).
  Inspected the complete range and `LICENSE.txt` at the final commit (LGPL-3.0).
- Destinations under `common/wirelessfrequency`: `AutomaticEntrancePlanner`,
  `AutomaticWirelessEntrances`, `EntranceCandidates`, `IWirelessPathingState`,
  `PhysicalClusterTracker`, `PhysicalMeTopology`, `WirelessCardVisualisation`,
  `WirelessChannelManager`, `WirelessClusterManager`, `WirelessLinkPersistence`
  and `WirelessTopologyWatch`. Also adapts the item and adjacent translation
  declarations in `common/items/wirelessfrequency`, plus `MixinWirelessGridNode`
  and `MixinWirelessPathGridCache` in `mixins/late/appliedenergistics/wireless`.
- Reuses stable topology for 600 ticks with prompt invalidation for physical
  changes and 20-tick retries while saved nodes are unavailable. Reuses entrance
  indexes, channel counts, per-tick statistics and batch candidate distance maps.
  Entrance trials cover multiple missing devices before one repath and roll back
  trials without channel gain. Render-only device refreshes keep the cached graph.
- Adaptations preserve GTNG packages, `gtng$` hooks, Java-compatible collection
  helpers, multi-source bindings and machine ownership behavior. New tooltip text
  uses Java translation declarations. Packaged code provenance is appended to
  `META-INF/wireless-frequency-card/NOTICE.md`. No new assets; existing texture
  hashes and license records remain unchanged. Reference checkouts stay outside
  source sets and resources.
- Validation: `compileJava`, `processResources` and all 22 focused wireless-frequency
  JUnit tests passed. `scripts/wireless-frequency-qa.init.gradle` with
  `-PwirelessFrequencyPerformance=true runClient17` passed on AE2
  `rv3-beta-1073-GTNH` and GT `5.09.54.183`: 180 stable ticks and 60 render refreshes
  retained physical membership; native wired additions/removals changed membership
  and topology revisions; actual remote chunk unload removed nodes/entrances without
  reloading the chunk. `-PwirelessFrequencyMultiSource=true runClient17` also passed
  source switching, separate auto preferences, naming and deletion preserving links.
  Both clients use the scoped hidden-window helper and muted sound settings.

## Packaged provider pattern capacity — 2026-10-13

User-requested extension: 80 rows of nine patterns, with four visible rows and wheel/scrollbar navigation.
Inspected pinned AE2LTPP `1d3f183ecf258aff0001567d742ed30d6038d77b` screen JSON and LGPL-3.0 license,
and AE2LT `1d4589b6bd50672051f78d766505530beacfebc0` separate CC-BY-NC-SA-3.0 asset grant.
Destination: `common/blocks/packaged/PackagedProviderGui.java`; existing provider atlas pixels are cropped
at runtime to widen the frame by 14 pixels and render scrolling slots. Core panel and priority tab move
14 pixels right. Existing `big_scroller.png` is reused; no PNG was changed or newly copied.
Sources, destinations, licenses and unchanged hashes remain in `META-INF/ae2lt-port/ASSET_MANIFEST.json`.
Tile storage retains 720 slots; versioned inventory loading migrates legacy return/core slots.
The terminal initially exposes two rows and grows/shrinks using the super pattern assembly rules.
Cached row counts and existing native terminal resize guards keep server inventories and client recipe caches aligned.
Reference checkouts remain outside build inputs.

## Integrated chemical recipes — 2026-10-15

- Source: ABKQPO/GT-Not-Leisure, commit
  `651396c75d108e6ff5399a44c56c12214ad27d3d`.
  https://github.com/ABKQPO/GT-Not-Leisure/tree/651396c75d108e6ff5399a44c56c12214ad27d3d
  Upstream `LICENSE.txt` was inspected before adaptation and grants LGPL-3.0.
- Recipe sources under `com/science/gtnl/common/recipe/gtnl/`:
  `FuelRefiningComplexRecipes.java`, `WoodDistillationRecipes.java` and
  `PetrochemicalPlantRecipes.java`. Their SHA-256 hashes are respectively
  `819fba4db90f4dbb59027cc07079fa02831eb8ee1e8ebd5fe1a5cc75e272e342`,
  `6b6906728e0d8b32fd937da2626d4c2db6166e6944653e9ec0839ac0fe677a2e` and
  `697b646139ed6456830e0ad37dcd19e18e22d3b68d0647cf4ded24686f9a2bcf`.
  Destinations under `com/xyp/gtnotgood/common/recipe/gtnotgood/`:
  `FuelRefiningRecipes.java`, `WoodDistillationRecipes.java` and
  `PetrochemicalRecipes.java`.
- Metadata source: `com/science/gtnl/utils/recipes/metadata/FuelRefiningMetadata.java`,
  SHA-256 `4aa57b54ab6673f51278b3231dd2ec490c37f44fee70aca10d13c4f3cf8cef8b`.
  Destination: `com/xyp/gtnotgood/common/recipe/metadata/FuelRefiningTier.java`.
- Adaptations replace GTNL package names, recipe maps, registration and custom
  builder with local registration and native GTNH `GTRecipeBuilder`. Fuel recipes
  retain all 25 entries, including their `COIL_HEAT` values and refining tiers
  0 through 4. The wood pool contains its one recipe; the petrochemical pool
  contains 11 recipes, omitting the Barnarda C sap recipe because its fluid is
  supplied by GTNL. No replacement fluid is registered. The three recipe pools
  are registered independently of GTNL.
- The adapted sources remain LGPL-3.0 and are outside the project's general MIT
  grant. Packaged provenance and full LGPL/GPL texts are under
  `META-INF/chemical-recipes-port/`; no visual assets are copied. The ignored
  reference checkout never participates in compilation or resource packaging.

## Large Industrial Farm — 2026-10-15

- Source: GTNewHorizons/CropsNH `2.0.129`, commit
  `47debb72b230382c91aa256b92d715062c3a318e`, confirmed by the upstream tag API.
  https://github.com/GTNewHorizons/CropsNH/tree/47debb72b230382c91aa256b92d715062c3a318e
  The pinned `LICENSE.txt` and README were inspected before adaptation; GTNH
  modifications are LGPL-3.0-or-later, with original Agricraft MIT attribution.
- Source file:
  `src/main/java/com/gtnewhorizon/cropsnh/tileentity/multi/MTEIndustrialFarm.java`,
  SHA-256 `9a3c90241c6b0252b03d0008aab8783f2bd4a0508ce24e07f6d67ba39f4fc795`.
  Destination:
  `src/main/java/com/xyp/gtnotgood/common/machines/multiblock/LargeIndustrialFarm.java`.
- The controller uses CropsNH crop and environment APIs and runtime textures.
  Adapted structure, validation and farming calculations fix the maximum dimensions
  at 5 x 4 x 14, accept upgrades independently of seed-bed tier, support multi-amp
  energy by default and use fixed borosilicate glass without an energy-tier limit.
  Native `MultiAmpEnergy` accepts only hatch type 1, rejecting wireless type 2
  above 64 A. The controller accepts `MTEHatchWirelessMulti` inputs, including
  local `WirelessLaserEnergyHatch` subclasses and native UXV catalog entries.
  These use the native `WIRELESS` connection type, without an overclock upgrade
  prerequisite or glass voltage cap. The limit remains one exotic energy hatch,
  with no normal/exotic mixing; ordinary `LASER` hatches are rejected.
  Up to 36 seed types are grouped automatically without soil requirements.
  Each type independently holds 12 times the native seed-bed capacity and uses
  its own two environment cards. Storage contains 108 slots: 36 sets of one seed
  slot and two independent environment-card slots. Migration from the older
  38/74-slot shared-card layouts retains the two cards on the saved selected
  type, leaving the other types' card slots empty. Migration from the original
  144-slot layout retains every type's own cards. Stored soil is returned safely.
  Growth and overclock modules coexist; perfect overclocks multiply power and
  throughput by four. Higher module tiers strengthen their respective effects,
  and water/fertilizer consumption scales with the planted seed count and
  overclock throughput.
- Local additions:
  `src/main/java/com/xyp/gtnotgood/common/machines/crop/IndustrialFarmInventory.java`,
  `src/main/java/com/xyp/gtnotgood/common/machines/crop/SelectedCropInventory.java`
  and `src/main/java/com/xyp/gtnotgood/common/gui/modularui/multiblock/LargeIndustrialFarmGui.java`.
  `SelectedCropInventory` adapts the selected seed and its two environment cards
  to the native slot indices. The GUI inherits `MTEIndustrialFarmGui` with its
  original panel size, layout, controls and textures, removes only the soil
  widget from the native row, and adds previous/next seed-type selection and a
  counter. Three slots remain visible, without a soil placeholder. No scrolling
  list or side panel is added. No upstream GUI source,
  textures or other visual assets are copied into these additions.
- The adapted controller retains LGPL-3.0-or-later outside the project's general
  MIT grant. Provenance and unchanged pinned LGPL text, plus the incorporated GPL
  terms, are packaged under `META-INF/industrial-farm/`. Dependency source archives
  remain outside compilation and resource packaging.

## JourneyMap context menu — 2026-10-16

- Feature reference: peco331/journeymap-quick-teleport-GTNH, commit
  `b50d0009717e11ce9c33e00cae0dce4e3561431b` (LGPL-3.0).
  https://github.com/peco331/journeymap-quick-teleport-GTNH/tree/b50d0009717e11ce9c33e00cae0dce4e3561431b
  Its README, source and license were inspected. This integration is independently
  implemented; no source files or visual assets from that repository are incorporated.
- Destinations: `client/journeymap/`, `utils/journeymap/JourneyMapCompat.java`
  and `mixins/late/journeymap/FullscreenMenuMixin.java` under the main Java package.
  The installed JourneyMap API is accessed at runtime, without bundling its classes.
  The late mixin replaces the reference CoreMod transformer; native coordinate
  projection, per-screen menu state, local landing checks, unloaded-terrain
  confirmation, generated translations and optional client-only loading are local.
- Baseline: GTNH 2.9.0-RC-1, JourneyMap 5.2.23-fairplay. Reference checkout remains
  in an external temporary directory, outside compilation and resource packaging.

## ME output assembly overlays — 2026-10-15

- Source: GTNewHorizons/GT5-Unofficial `5.09.54.183`, commit
  `f8453bce28793713d314f6dcfa5ac551d700efd6`, the resolved development dependency.
  https://github.com/GTNewHorizons/GT5-Unofficial/tree/f8453bce28793713d314f6dcfa5ac551d700efd6
  The pinned README and `LICENSE.txt` were inspected before adapting the assets.
  Their LGPL-3.0-or-later license is retained outside the project's general MIT grant.
- Sources: `assets/gregtech/textures/blocks/iconsets/OVERLAY_ME_HATCH.png`,
  `OVERLAY_ME_FLUID_HATCH.png` and their `_ACTIVE` variants. The source artifact is
  `GT5-Unofficial-5.09.54.183-sources.jar`, SHA-1
  `b0f3eb42604cfc3ad88d56c50d0dbfa375d89419`.
- Destinations: `assets/gtnotgood/textures/blocks/iconsets/OVERLAY_ME_OUTPUT_ASSEMBLY.png`
  and `OVERLAY_ME_OUTPUT_ASSEMBLY_ACTIVE.png`. Both stay native 16x16 RGBA, preserving
  the original orange 2x2 item center at 7..8. A blue fluid ring occupies the 5..10
  inner-panel perimeter, and the native orange outer rim occupies the 2..13 perimeter,
  with original gray metal spacing between them. Active and inactive use the same
  source orange/blue RGB values, so the two concentric composites match pixel for pixel.
- Complete source/destination hashes and precise pixel changes are in
  `META-INF/me-output-assembly/ASSET_MANIFEST.json`; full LGPL/GPL terms and attribution
  are packaged beside it. Extracted references and previews stay in ignored
  `build/me-output-reference`, outside compilation and packaged resources.

## Large Industrial Apiary — 2026-10-15

- Source: GTNewHorizons/GT5-Unofficial `5.09.54.183`, commit
  `f8453bce28793713d314f6dcfa5ac551d700efd6`, confirmed through the upstream tag API.
  https://github.com/GTNewHorizons/GT5-Unofficial/tree/f8453bce28793713d314f6dcfa5ac551d700efd6
  The pinned README, `LICENSE.txt`, controller and base-class headers were inspected
  before adaptation. The controller/base explicitly retain kuba6000's 2022-2024
  LGPL-3.0-or-later notice; the GUI has no separate header and uses the repository's
  LGPL-3.0-or-later grant. The adapted controller and GUI are outside the project's
  general MIT grant.
- Sources under `src/main/java/`:
  `kubatech/tileentity/gregtech/multiblock/MTEMegaIndustrialApiary.java` and
  `kubatech/gui/modularui2/MTEMegaIndustrialApiaryGui.java`, with
  `kubatech/api/implementations/KubaTechGTMultiBlockBase.java` as an additional
  source for its fractional voltage-tier formula. Destinations:
  `com/xyp/gtnotgood/common/machines/multiblock/LargeIndustrialApiary.java` and
  `com/xyp/gtnotgood/common/gui/modularui/multiblock/LargeIndustrialApiaryGui.java`.
- Adaptations use local registration, translations and the GTNG electric multiblock
  base while preserving the upstream 15 x 17 x 15 structure, queen storage,
  production, swarming and royal-jelly bonus. Energy hatches no longer require LuV;
  glass is fixed to standard Borosilicate Glass Blocks (`bartworks:BW_TieredGlass`,
  metadata 0; `ItemRegistry.bw_realglas`) without restricting energy tier.
  Flower checks do not depend on queen genes.
  Each stored queen uses LV power and capacity is eight times the LV-based value.
  The local controller recipe uses MV assembler power and MV-or-earlier materials.
- Icons and textures are referenced from the installed dependency at runtime;
  no visual assets are copied. Full source hashes, attribution, adaptation details
  and LGPL/GPL texts are packaged under `META-INF/industrial-apiary/`.
  Extracted dependency references remain in ignored `build/tmp/apiary-study`,
  outside compilation and resource packaging.

## Precision Auto-Assembler — 2026-10-18

- Source: GTNewHorizons/GT5-Unofficial `5.09.54.205`, commit
  `a3e1e11241a814c9fa0dd0973d5699548428f689`,
  `src/main/java/goodgenerator/blocks/tileEntity/MTEPreciseAssembler.java`
  and `src/main/resources/assets/gregtech/lang/en_US/tooltip/precise-assembler.md`.
- Destination: `src/main/java/com/xyp/gtnotgood/common/machines/multiblock/PrecisionAssembler.java`.
  The structure, two processing modes and tooltip wording are adapted with five GregTech
  mechanical casing tiers, an MV controller recipe, perfect overclocking and standard
  energy-hatch amperage behavior. Internal machine casings limit base recipe tiers,
  without limiting energy hatch tiers or overclocking voltage.
- The adapted source retains LGPL-3.0-or-later, outside the project's general MIT grant.
  Attribution and the full LGPL/GPL terms are packaged under
  `META-INF/licenses/precision-assembler/`. Textures are referenced from GregTech at
  runtime; no upstream images or reference checkout files are packaged.

## Circuit Assembly Line packaged core — 2026-10-19

- Inspected GTNewHorizons/GT5-Unofficial `5.09.54.205`, commit
  `a3e1e11241a814c9fa0dd0973d5699548428f689`, `MTECircuitAssemblyLine.java`,
  `CircuitImprint.java` and recipe lookup sources from its source JAR. The controller
  source carries the BartWorks MIT notice. No GT5U implementation is copied.
- Destination: new `common/blocks/packaged/CircuitAssemblyLineAdapter.java` and
  core registration, acquisition recipe and tooltip glue. The native CAL recipe
  map and existing permanent imprint determine accepted jobs; circuit-assembler
  mode and its Crafting Input Buffer/Bus support are unchanged.
- Inspected the pinned AE2LTPP renderer and LGPL-3.0 license at
  `1d3f183ecf258aff0001567d742ed30d6038d77b`. The existing renderer and unchanged
  core base are reused; the installed CAL controller supplies its icon at runtime.
  Packaged records: `META-INF/ae2lt-port/ASSET_MANIFEST.json` and `CODE_PORT_NOTES.md`.
  No textures or reference sources are added to compilation or packaging.

# Basic Industrial Apiary (2026-10-20)

- Source: GTNewHorizons/GT5-Unofficial `5.09.54.183`, commit
  `f8453bce28793713d314f6dcfa5ac551d700efd6`,
  `src/main/java/gregtech/common/tileentities/machines/basic/MTEIndustrialApiary.java`.
- Destination: `common/machines/basic/BasicIndustrialApiary.java`; source remains
  LGPL-3.0-or-later. Existing LGPL/GPL texts and the feature's
  `META-INF/industrial-apiary/BASIC_APIARY_NOTICE.md` are packaged.
- Preserves native bee genetics, breeding, effects, upgrades, original controls and
  runtime overlays. Adds independent LV registration, 32 EU/t operation, 64x base
  acceleration, 16x product quantities, relaxed work/specialty conditions, offspring
  recycling, 54 output slots, persistent long-count overflow and per-tick auto-output.
- Expanded GUI keeps native controls and upgrade interactions; no textures are copied.
  Extracted reference sources stay in ignored `build/basic-apiary-reference/`, outside
  compilation and resource packaging.

## Field Extruder Factory visual assets — 2026-10-21

- GTLCore `6443c848264fb15c90cf50e031f146522616a8b9`, LGPL-3.0: original iridium,
  hyper mechanical and molecular casing PNGs and two LDLib CTM atlases.
- GTCEu Modern tag `1.20.1-1.4.4`, commit `e3844ba943e2388c0856617ae4f8b473a0ae1b64`,
  LGPL-3.0: original cleanroom glass, CTM atlas and fusion reactor controller overlays/animation.
- Official GregTech Leisure `1.4.5.3`, CurseForge file `9238465`, archive SHA-256
  `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`:
  hollow casing and containment field generator PNGs/animation. The actual archive's root
  license is CC BY-NC-SA 4.0; these assets retain that license and are excluded from MIT.
- Destinations: `assets/gtnotgood/textures/blocks/fieldextruder/`,
  `common/blocks/fieldextruder/` and `client/fieldextruder/`. The independently implemented
  GT/Forge block renderer supports connected glass/casings and the controller/hatch casing background.
  Original assets remain byte-for-byte unchanged; LDLib metadata is kept under the provenance directory.
- `reference/field-extruder-port/generate-connected-textures.py` losslessly composes 141 16×16 tiles
  from the three original 32×32 atlases. Layout reference: LDLib-MultiLoader
  `1d4e4390ae58d16038845c651e122185c29f334b`, `Connections.fillSubmaps` / `Submap.uvs`, GPL-3.0.
  No modern renderer, shader or bloom code is compiled or packaged. Reference checkouts stay outside source sets.
- Exact source/destination/revision/license/hash records, original LDLib metadata and all applicable license texts:
  `META-INF/field-extruder-port/`. Further source and regeneration notes: `reference/field-extruder-port/README.md`.

## 力场压模工厂 — 2026-10-21

- 结构与行为参考：GNSW/GTLCore `6443c848264fb15c90cf50e031f146522616a8b9`。
  `MultiBlockMachineAStructureC.FIELD_EXTRUDER_FACTORY` 保留完整 15×9×63 数据和空格 any 语义，
  仅压缩重复切片并转换前后顺序及 StructureLib 层布局；结构 Java 保留 LGPL-3.0 和上游 TST 结构署名。
- 本地控制器保留 GT5 原生压模／压缩两种配方图，按用户要求使用通用 `GTNGModularMachineBase`
  与 `ModularProcessingLogic`。每个核心独立处理一种配方的并行任务，并行上限接受并行模块调整；
  输入提交、功率校验、输出保护和任务保存沿用其他模块化机器的通用处理，非消耗输入遵守原生配方定义。
  已移除专属混合配方批次、64 倍批量、总耗电折算耗时和最低 20 tick 算法。
- 生存配方参考官方 GTL 1.4.5.3 整包 `gtceu.js`，实际包内许可 CC BY-NC-SA 4.0。
  控制器保留 UEV 装配线与 UIV 电路，结构件保留 LuV／UV 等级；材料及原生研究接口映射详见打包 NOTICE。
- 完整源路径、许可证、目标与修改记录：`src/main/resources/META-INF/field-extruder-port/NOTICE.md`。
  视觉源文件与派生 CTM 哈希清单沿用上节记录；参考源码和离线生成器不参与编译。

## 维度聚焦激光蚀刻阵列 — 2026-10-21

- 结构与行为参考 GNSW/GTLCore `6443c848264fb15c90cf50e031f146522616a8b9`，LGPL-3.0。
  完整保留 89×89×89 坐标和空格语义；原结构以 gzip 保存，加载时转换为 StructureLib 坐标。
- 保留 GTLCore 超维度、维度注入、超导线圈及 GTCEu `e3844ba943e2388c0856617ae4f8b473a0ae1b64`
  聚变玻璃、激光蚀刻机械方块的原始 PNG、连纹与运行贴图，均为 LGPL-3.0。
  分子线圈原图来自官方 GTL 1.4.5.3 归档，实际包内许可证为 CC BY-NC-SA 4.0。
- 目标：`common/blocks/dimensionalfocus/`、`assets/gtnotgood/textures/blocks/dimensionalfocus/`、
  `assets/gtnotgood/structures/dimensional_focus_engraving_array.shape.gz`。
  282 张连纹为原图象限的无损组合；Java 渲染适配 Forge/GT5，超导线圈使用元数据同步运行外观。
  复用力场压模工厂的遏制场发生器与同源聚变控制器面板。
- GTNH 适配：原生激光配方、光刻胶、1/5 耗时、4 倍功耗、线圈并行及完美超频。
  高阶配方由原生扫描仪制作产物研究盘，使用数据访问仓和 TecTech 算力包；算力不足时暂停。
  控制器没有无线模式。控制器与六种结构件提供生存配方，保留 UEV/UIV 门槛。
- 来源、修订、许可证、修改、目标及原图/连纹哈希见 `META-INF/dimensional-focus-port/`；
  参考检出与离线生成器不参与编译或资源打包，外部资产不属于项目通用 MIT 许可。

## 维度聚焦阵列中央动态渲染 — 2026-10-21

- 来源：GregTech-Odyssey/GTOCore `3121e23241e7f6ccd2e6a8dbd5655962de63ed9a` 的
  `DimensionalFocusEngravingArrayRenderer` 与 `RenderHelper.renderCylinder`，LGPL-3.0。
  该后续实现不在最初采用的 GTLCore 快照中；仅移植同名机器的中央渲染。
- 原样保留六向光柱、0.5 格半径、8 格长度、十边截面与 360 个顶点的三角带顺序。
  核心位于控制器后方 22 格、下方 18 格，按线圈材质色以 100 tick 周期向白色脉动。
- 顶点与混合状态参考上游 GTCEu 分支；颜色量化与二次缓动参考实际依赖的 LDLib 1.0.50。
  本地适配文件 `client/render/DimensionalFocusCoreGeometry.java` 与
  `DimensionalFocusCoreRenderer.java` 保留 GPL-3.0-only 许可，不并入项目通用 MIT 许可。
- 使用 GT5 原生 `IMTERenderer` 和机器数据流同步工作状态、线圈及动画相位；
  核心渲染包围盒覆盖完整光柱，避免控制器在画面外时被错误剔除。
  客户端以世界总时间外推同步相位，兼容 GTNH 停止客户端方块 tick 的优化。
  可选 Bloom 通过已有 GTNHLib 后处理接口实现，不引入现代 Shimmer 或额外贴图。
- 原始参考、修订、许可证、源文件及目标文件哈希见 `META-INF/dimensional-focus-port/RENDER_MANIFEST.json`；
  修改与来源说明见同目录 `RENDER_NOTICE.md`，参考源码存于 `reference/dimensional-focus-port/render-upstream/`，
  不参与编译或资源打包。

## 超级粒子对撞机 — 2026-10-21

- 结构来源：[GNSW/GTLCore](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9)
  `MultiBlockMachineAStructureA.SUPER_PARTICLE_COLLIDER`，LGPL-3.0-only。
  完整保留 47×7×47 坐标、空格 any 语义与 TST 结构署名；仅无损合并切片并转换 StructureLib 坐标。
  上游重复定义 `d` 后实际采用加速管道，因此保留 560 管道，不新增聚合核心。
- GTLCore 同一修订提供路菲恩机械方块原图与连纹；GTCEu Modern
  `e3844ba943e2388c0856617ae4f8b473a0ae1b64` 提供超导线圈原图、活动发光层、连纹与模型，均保留 LGPL-3.0。
  加速管道原图来自官方 GregTech Leisure 1.4.5.3，CurseForge 文件 9238465，
  归档 SHA-256 `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
  该归档实际根许可证为 CC BY-NC-SA 4.0；复用力场压模工厂的同源聚变控制器面板。
- 目标：`common/blocks/particlecollider/`、`client/particlecollider/`、
  `assets/gtnotgood/textures/blocks/particlecollider/` 与
  `common/machines/multiblock/structure/SuperParticleColliderStructure.java`。
  原始资源不改字节，141 张连纹由原图象限无损组合；离线布局参考 LDLib-MultiLoader
  `1d4e4390ae58d16038845c651e122185c29f334b`，生成器保留 GPL-3.0。
  方块材质页固定 124；超导线圈使用 GT5 原生 `activeCoils` / `GTCoilTracker` 与发光材质，
  服务器元数据 2、客户端活动外观 10，不移植现代渲染器或 Shimmer 后处理。
- `SuperParticleColliderRecipes.java` 改编官方归档 `gtceu.js` 的 2700–2709、10408–10421、
  8016–8039 行，保留 CC BY-NC-SA 4.0。控制器与结构件映射 GTNH 原生材料，保留五条有完整原生终点的
  元素碰撞配方，省略无法闭合的材料与反物质支线；原生超导线圈可 1:1 转换为保留上游外观的新线圈。
- 精确来源、修订、许可证、目标、修改及原始/派生资源哈希：`META-INF/particle-collider-port/`。
  结构核对见 `reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md`，配方映射见
  `reference/particle-collider-port/RECIPE_ADAPTATION.md`。外部素材与改编源码不属于项目通用 MIT 许可；
  参考检出、上游原始模型和离线生成器不参与源码编译。

## 巨型组装厂与超导电磁工厂 — 2026-10-21

- 结构来源：GNSW/GTLCore `6443c848264fb15c90cf50e031f146522616a8b9`，LGPL-3.0-only。
  `MAGE_ASSEMBLER` 保留 9×10×33 坐标与 GTNH 结构署名；`SUPERCONDUCTING_ELECTROMAGNETISM`
  保留 15×21×15 坐标，仅无损合并切片并转换 StructureLib 坐标。结构证据见
  `reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md`。
- 原材质：GTLCore 同修订进阶装配线控制外壳；GTCEu Modern
  `e3844ba943e2388c0856617ae4f8b473a0ae1b64` 的层压/钢化玻璃、过滤机械方块、PTFE 管道方块、
  二硅化钼线圈、电解槽、装配单元发光动画和大型组装机控制器面板，均保留 LGPL-3.0。
  超时空装配单元原图来自官方 GTL 1.4.5.3，CurseForge 文件 9238465，实际包内许可 CC BY-NC-SA 4.0，
  归档 SHA-256 `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
- 目标：`common/blocks/assemblyplant/`、`client/assemblyplant/`、
  `assets/gtnotgood/textures/blocks/assemblyplant/`。复用既有铱外壳、路菲恩机械方块、加速管道、
  改良型超导线圈与聚变控制器面板，原物品元数据和资源路径不变；新材质页固定 123。
  新增八种原视觉结构件，三种新活跃方块及复用改良线圈使用 GT5 原生 activeCoils/GTCoilTracker。
  原始文件不改字节；235 张连纹由五组原图象限无损组合。离线布局参考 LDLib-MultiLoader
  `1d4e4390ae58d16038845c651e122185c29f334b`，生成器保留 GPL-3.0，不移植现代渲染器或 Shimmer。
- 生存配方：`MageAssemblerRecipes`、`SuperconductingElectromagneticRecipes` 改编官方整包
  `gtceu.js`，保留 CC BY-NC-SA 4.0；`AssemblyPlantCasingRecipes` 改编同 pin GTCEu GCyM 配方，
  `LightningProcessorRecipes` 改编同 pin GTLCore 末影珍珠闪电处理配方，分别保留 LGPL-3.0-only。
  原料与原生 GTNH 阶段对应，兼容标准 GT 能源仓和特殊能源仓，包括原生无线能源仓；
  控制器不提供独立无线电网模式，也不直接从拥有者的能源账户扣费。
- 完整来源、修订、许可证、目标、修改和原始/派生资源哈希见 `META-INF/assembly-plant-port/`。
  配方映射见 `reference/assembly-plant-port/RECIPE_ADAPTATION.md`；外部素材与改编源码不属于项目通用 MIT
  许可，参考检出、上游原始模型和离线生成器不参与源码编译。

### 超导电磁工厂中部旋转 — 2026-10-23

- 参考当前依赖 GT5 `5.09.54.205`，commit `a3e1e11241a814c9fa0dd0973d5699548428f689`，LGPL-3.0-or-later。
  `MTEForgeOfGods#createRenderer/destroyRenderer` 切换真实环与渲染 TileEntity；
  `RenderForgeOfGods#initRings/renderRings/renderRing` 缓存环网格并按时间施加旋转矩阵。
- 本地独立实现 `client/render/ElectromagneticRotorRenderer` 与 `ElectromagneticRotorModel`，
  复用现有结构的 148 个 C、68 个 E、17 个 H 及原连纹。只旋转中部五环与中心柱，240 tick 一周；
  服务端真实方块、结构检查与加工不变，客户端按同步运行状态隐藏对应静态面，停机/卸载恢复。
- 未复制神之锻炉的删除/重建逻辑、模型、贴图或着色器。参考源码不参与编译；版本、源码哈希、
  目标与差异记录在 `META-INF/assembly-plant-port/ROTOR_REFERENCE.json`。

## 克尔-纽曼均质仪 — 2026-10-21

- 来源：GregTech-Odyssey/GTOCore `3121e23241e7f6ccd2e6a8dbd5655962de63ed9a`，LGPL-3.0-only。
  原始压缩结构逐字节保留，83×36×83、15,638 个固定构件（含控制器）；空间沿用 any 语义。
  23 种新构件保留原纹理、连纹和运行发光层，复用已有同源构件，材质页固定 122。
- 中央黑洞保留 2.5/4.5 格半径、60 tick 展开和收缩、0.12 强度的真实背景折射。
  使用 GT5 IMTERenderer、GTNHLib 后处理与 GLSL 1.20，独立尾动画覆盖停机、失效和拆除。
  原球面网格仅修正三角形绕序，兼容前表面和深度测试；所有渲染类位于 `client/render`。
- 加工接入 GTNH 原生搅拌配方、能源仓、功率面板和电压限制。
  按用户指定采用 6000% 速度（耗时除以 60）与完美超频（每级耗电乘以 4、耗时除以 4），
  tooltip 使用 GTNH 原生性能辅助方法；这属于本地平衡设定，不冒充上游原始倍率。
  原 GTOLib 26.7.4 基类实现为闭源 native，无法核实的参数明确记录为兼容选择。
  控制器保留九路物品数量、三路各 1000 mB、800 tick 与 2,097,152 EU/t；
  专属材料、研究与无尘条件的原生适配逐项记于 `RECIPE_ADAPTATION.md`。
- 完整来源、修订、许可证、修改、目标及哈希见 `META-INF/kerr-newman-port/`；
  外部素材和改编代码不属于项目通用 MIT 许可，参考源与离线生成器不参与编译或资源打包。

## 宇宙之心 — 2026-10-22

- 主参考：[Dragonators/GTLAdditions](https://github.com/Dragonators/GTLAdditions/tree/34fb152886164c56a07fc9d1b10e056f0c22b7ab)
  `34fb152886164c56a07fc9d1b10e056f0c22b7ab`，GPL-3.0。结构取自
  `scripts/structure_sources/multiblock/annihilate_generator.json`，控制器定义为 `HeartOfTheUniverse`。
  完整保留原版 121×112×109 外形、37,144 个非空位置（含控制器）及空格 any 语义；
  原 JSON/GTLASB3 保存作参考，运行资源仅转换为 GZIP MBS 编码。
  原坐标 `(column,row,aisle)` 转为 `shape[111-row][108-aisle][column]`，宽度不镜像；
  控制器原位置 `(62,23,93)` 对应 StructureLib 偏移 `(62,88,15)`。
- 新结构方块位于 `common/blocks/heart/`，固定材质页 121。GTLCore
  `6443c848264fb15c90cf50e031f146522616a8b9` 提供引力场约束、铼强化聚能玻璃、维度连接、
  创造机械方块与 `sps_casing` 原图，LGPL-3.0；时间锚定场外壳保留上游对 `sps_casing` 的引用。
  141 张 CTM 由原图象限无损组合，布局算法沿用 LDLib-MultiLoader
  `1d4e4390ae58d16038845c651e122185c29f334b`，GPL-3.0。
  超能机械方块、聚变玻璃和维度注入机械方块复用已有同源移植材质。
- 湮灭核心、维度稳定方块、戴森磁体及湮灭约束器原图来自官方 GregTech Leisure 1.4.5.3，
  CurseForge 文件 9238465，归档 SHA-256
  `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
  归档实际根许可为 CC BY-NC-SA 4.0；保留原动画、磁体侧面/顶底区别及核心亮度。
  控制器底色使用 GTCEu Modern `e3844ba943e2388c0856617ae4f8b473a0ae1b64` 的高功率外壳，LGPL-3.0；
  正面/工作发光层来自 GTLCore，顶面来自 GTLAdditions 的 `cosmos_simulation` 覆盖资源，分别保留 LGPL/GPL。
  燃料棒与黑洞种子采用 GTLAdditions 原图，种子保留四帧动画。
- 黑洞、吸积盘、心跳体积效果和模糊合成参考同 pin 的 `client/render/machine/heart/`、
  `shaders/core/gtladditions_heart_*` 与 `shaders/include/heart_volume_common.glsl`，GPL-3.0-only。
  GLSL 适配 1.20，保留原 200 步体积积分与七 pass 合成，使用 GT5 `IMTERenderer`、GTNHLib 后处理、FBO 和现有世界深度；
  不编译现代 GTCEu 渲染接口或 Oculus 桥接。局部 `(0,-36,39)` 在 SOUTH 朝向映射为
  世界 `(0,+36,-39)`，处于原结构中央空腔；保留 384 格视距与完整特效边界。
  基础 GL 状态保存与 GTNHLib 接入参考项目既有克尔-纽曼适配（LGPL-3.0-only），沿用其来源记录；
  黑洞视觉与 shader 仍取自上述 GTLAdditions 快照。
- 每套燃料实际消耗一根奇异湮灭燃料棒和一个黑洞种子，保留原版总能量
  `Long.MAX_VALUE × 81920` EU，以及 60% 概率产出 64 个湮灭约束器。
  GTNH 原生配方、研究和材料映射见 `reference/heart-of-the-universe-port/ITEMS.md`；
  不虚构现代材料注册项。控制器按用户要求移除 UU 单元，改用第 4 项原生 UU 流体，物品输入为 14 项。
  UU 用量重平衡为 16384000 mB（16384 桶），参考 GT5 5.09.54.205 的 UXV 能源仓 64000 mB ×256；
  这是本模组定制值，不是官方 MAX 标准，不保留上游 921600000000 mB 的绝对成本。原始源和哈希不变。
- 本地模块化发电分支使用 `GTNGModularGeneratorBase` 和 `GenerationWork`，
  内置普通核心，附加核心独立燃烧并共享输入；普通核心分摊并行额度，高级/完美核心独立执行。
  基础释放时间按用户要求设为普通 1200 tick（60 秒）、高级 300 tick（15 秒）、完美 1 tick，秒数按 20 TPS 计。
  速度/超频模块只缩短释放窗口，最低 1 tick；发电超频倍率仍为 4/8/16，普通核心对应 300/150/75 tick，
  不增加每套燃料总能量。能耗模块仅降低基础 64A LuV（2,097,152 EU/t）维持耗电，
  运行中每台机器每 tick 支付一次。实际发电以 BigInteger 分步记入主机所有者的 GT 无线能源网；
  无电、停机或核心移除时暂停，燃料提交状态、已释放进度、剩余能量及产物由主机 NBT 保存。
- 目标包括 `HeartOfTheUniverse.java`、`common/machines/multiblock/structure/HeartOfTheUniverseStructure.java`、
  `common/blocks/heart/`、
  `common/items/fuel/HeartFuelItems.java`、`client/render/HeartBlackHole*`、`assets/gtnotgood/shaders/heart/` 及对应素材目录。
  原始文件、来源、修订、许可、目标、修改和哈希见 `reference/heart-of-the-universe-port/`
  与 `META-INF/heart-of-the-universe-port/`。外部素材及 GPL 改编源码不属于项目通用 MIT 许可；
  参考检出、原始现代模型和离线生成器不参与编译或资源打包。验证计划见 `docs/testing/heart-of-the-universe.md`。

## 猎光号空间站 — 2026-10-22

- 主参考 Dragonators/GTLAdditions `34fb152886164c56a07fc9d1b10e056f0c22b7ab`，GPL-3.0。
  用户选择保留原主站与四类卫星工厂机制；不接入本模组参数模块和执行核心。
- 保留主站 73×73×189、卫星工厂 5×21×13 原结构全部坐标和空格语义、20 个连接位。
  四向叠放校验每座工厂共享主站 770 个壳体位置，控制器需朝向站轴，连接器无错位或材质冲突。
- 主站按原电路 1、800 tick、UXV 536,870,912 EU/t 运行。工厂独立供电、独立 I/O；
  原配方合为一批，累计原总能耗，默认最短 20 tick。
  主站最多 1 个非激光能源仓；工厂最多 2 个非激光能源仓和 1 个激光仓。
  GTNH 普通/多安分支统一计数；无线仓保留其原生能源类型，不把全部 ExoticEnergy 当成激光。
  工厂按各仓电压×额定工作安数之和处理配方和批次，不丢弃多安输入。
  依据固定 GTLCore `6443c848264fb15c90cf50e031f146522616a8b9` 的
  `mixin/gtm/api/misc/EnergyContainerListMixin.java:48–61` 和
  `mixin/gtm/fix/WorkableElectricMultiblockMachineMixin.java:72–82`；
  Git blob 分别为 `42d6d76a1856f7545f6467dbe114e36e9a7fd661`、`566296a5e795ed5c1f6f0b5b402a0eafafeb28ab`。
  基础 128 线程分摊 `Integer.MAX_VALUE×128` 并行配额；主站安装 64 星阵后解除该配额。
  原生 ME 提交前复核合计库存及 AE 能量，失败不保留未确认批次；精确产出账本和进度持久化。
- 原星阵动画、原恒星/空间 OBJ、13 个行星、倾斜轨道和 GLSL 双层光束均保留。
  GTCEu、GTLCore 与官方 GTL 整包提供的材质分别按其 LGPL/GPL/CC BY-NC-SA 4.0 原许可记录。
  原 PNG、动画、CTM 派生及固定源 Git blob 验证见清单；不以重绘资源替换原图。
- GTNH 原生材料和获取方适配、未移植奥术星图导致压缩星阵无本地新增产出、
  蒸发映射化学脱水及岩石粉碎流体成本等差异见 `reference/light-hunter-port/ITEMS_RECIPES.md`。
  结构及渲染源/修订/许可/目标/修改/哈希见 `reference/light-hunter-space-station-port/` 与对应 META-INF 清单。
  外部素材及改编源码不属于项目通用 MIT 许可；参考检出和生成器不参与编译或打包。
- 验证计划与运行命令见 `docs/testing/light-hunter-space-station.md`。


## 大型熔炉与并行控制仓 — 2026-10-23

- 来源：用户实例 Enhancedcore 3.8.0、GTCEu 1.4.4、GTLAdditions 3.3.1Custom；固定 JAR SHA-256 见 `META-INF/large-furnace-port/ASSET_MANIFEST.json`。未确定 Enhancedcore 源码提交，不以最新分支冒充该版本。
- 已先核对 Enhancedcore/GTCEu 的 LGPL-3.0 声明。原始结构、六张 PNG 与四份动画元数据的来源/目的/哈希、许可全文在该 META-INF 目录；资产不属项目 MIT 授权。既有 GPL-3.0 批处理/输出账本与 GUI 代码保留许可。
- `LargeFurnaceMachine`、`ProcessingMachineRegistration`、`ThreadLimitedRecipeLogic`、GTLAdditions `calculateParallels`：LV 基准 64 并行、2 线程，电压每级两者翻倍；并行仓乘法，线程仓加法；选最多线程数种配方，共享并行×线程容量，合并批次能量与输出。复用原批次最短 20 tick 和 normalRates 规则，不改成免费 EU 或独立执行核心。
- 原几何 11×13×12、716 非空格，Y/Z 反向适配 StructureLib，控制器偏移 (5,11,0)。J→原生钢块；L→原生坚固钢机械方块/仓室；D→GT 焦炉砖块（sBlockCasings12:0，64 块）；E→GT 耐火砖块（sBlockCasings4:15，102 块）；B/C/G/K/M→铁栏杆，H/I→岩浆源；空格仍不约束。砖块映射已核对固定 GT5 5.09.54.205 源码 BlockCasings12、BlockCasings4、MTECokeOven、MTEBrickedBlastFurnace。原版维护仓上限在 GTNH 适配为必装 1 个；物品输入/输出、1–2 普通能源仓，最多各 1 个并行/线程仓。
- 按用户追加要求，控制器放 `multiblock/parallelmachine`，通用基类仍放 `multiMachineBase`。ID 28545；IV–MAX 并行仓 28546–28555，不迁移已有注册 ID。并行仓倍率 4^(tier−4)，GUI 1..上限、默认 1、服务端校验、NBT 保存。
- Ω-天球分歧引擎复用现有普通星阵（128 线程/个，最多 64 个）；新增 ThreadModifierHost，保留卫星工厂原方法重载及归属验证。
- GTNH 电炉 backend 每次只检查第一种物品，因此本机逐种查询原生池，去重后公平轮换，避免只加工首种物料。事务式输入预检、堵塞产物账本、NBT 和普通能源使用沿用既有实现。
- 获取配方采用原生 GTNH 材料：LV 电炉/钢机壳/电路制作控制器；同级机壳/传感器/电路制作并行仓。结构砖直接使用 GTNH 已有方块及配方，按用户纠正删除移植时重复新增的两种砖、配方和贴图。不声称复刻上游整包配方进度，不增加新版模组硬依赖。
- 可复查运行命令与测试边界见 `docs/testing/large-furnace.md`，调查文件在忽略的 D:/CodexTools/GTNGLargeFurnace/reference。
- 用户反馈并行仓缺失贴图后，补齐四份原始 overlay_front.png.mcmeta：每张 PNG 为 16×64 四帧动画、每帧 8 tick。元数据原字节及哈希纳入同一清单，不裁切或重绘 PNG；检查十级物品/世界显示及资源重载。

- 大型熔炉后续修正：通过独立 NEIGTNGConfig 为原生 smelting handler 注册控制器 catalyst，支持左侧机器列表及 U 用途查询；不能仅依赖禁用 NEI 注册的 furnaceRecipes 配方池。控制器继续使用原高版本面板，只将两张 16×16 图各 62 个低饱和青绿边框像素转为脱氧钢灰色；原图布局、透明度、指示灯逐像素保留，待机/运行一致。换色预览的重绘布局未打包，四张并行仓 PNG 和动画元数据保持原字节，源/产出哈希记入 manifest。

- 用户确认采用原生分层方式后，进一步将上述两张正面的 62 个金属边框像素改为透明，取消固定灰色；底图继续由 LargeFurnace#getTexture 单独选用 GTNH casing 16。其余 194 个像素与原版逐像素一致，面板/指示灯保持原样，资源路径不变；清单保留最初源哈希并更新最终哈希。生成预览改动了布局，未作为最终素材使用。


## GTL 透明正面资源库（2026-10-23）

- 固定来源：GTL-Enhancedcore 3.8.0 与 GTLCore 1.2.3.2-fix4，安装包 META-INF/mods.toml 均声明 LGPL 第 3 版；源码提交尚未确认，以包 SHA-256 固定版本。
- 选入 16 套待机、运行正面及原有暂停/发光层，目的目录 assets/gtnotgood/textures/blocks/parallel/fronts；只选择原生透明面板，不修改像素，不覆盖现有大型熔炉资源。
- 全部 PNG 和 mcmeta 原字节保留。来源、作者、许可、源/产出哈希与尺寸见 META-INF/machine-front-overlays/ASSET_MANIFEST.json，附独立 NOTICE 和完整 LGPL/GPL 文本。
- 1.7.10 接入：图标通过 Textures.BlockIcons.custom 注册，casing + front + 可选 glow 层由控制器明确选择；保留的新版 bloom 元数据不等于移植了新版泛光渲染。这里只移植资源，未注册对应机器或新增模组依赖。预览与使用说明见 docs/development/machine-front-overlays.md。


### 并行控制仓完整外观修正（2026-10-23）

- 用户确认提供的截图为目标，要求原上游等级机壳和正面。原先仅移植 mk1-4 且后六级复用 mk4；本次修正为十级一一对应，补全待机/运行/发光层及动画。
- 依据固定 GTLCore 1.2.3.2-fix4 的 common/data/machines/GCyMMachines.PARALLEL_HATCH：tier 5–14，workableTieredHullRenderer 路径 parallel_hatch_mk(tier-4)；GTCEu 1.4.4 TieredHullMachineRenderer 选择 voltage/<tier> 的 bottom/top/side。对应反编译参考仅存 D:/CodexTools/GTNGLargeFurnace/reference/hatch-render，不进入构建。
- mk1-4、30 张等级机壳来自 GTCEu；mk5-10 来自固定 GTLCore 包，LGPL 第 3 版。全部原字节拷贝，源/产出哈希和等级映射补入 large-furnace-port/ASSET_MANIFEST.json 与 NOTICE。mk1-9 四帧、mk10 八帧，统一 8 tick。
- GTNH 适配为底图 + 正面 + glow；原十级注册 ID、NBT 和前四级资源路径保留，外观按用户要求在物品/世界中使用上游等级机壳。


## 增强版超级粒子对撞机 — 2026-10-23

- 用户实例 GTL-Enhancedcore 3.8.0，作者 S_Fast，JAR SHA-256 `555a9514c4d2fd11a029c091ac053d5cf7c52fba62411f2f043e04454094f7aa`；归档 LGPL-3.0，无已确定源码提交。
- 原字节复制 hollow/gtl/super_particle_collider.pattern.gz，保留 197×83×209、302625 非空位置、24 仓室位及 41 格控制器高度，与截图匹配，替换普通 47×7×47 版本。
- 九类构件采用 GTNH 原生对应物，路菲恩外壳/加速管道复用已有移植。无新增重复方块/贴图；已发布旧线圈保留 ID 并提供回收转换。
- 来源/哈希/绑定/许可/适配见 META-INF/particle-collider-port/ENHANCED_STRUCTURE.json 和 reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md。结构资源不属于 MIT，沿用目录内 LGPL/GPL 全文。
- 参考文件在 D:/CodexTools/GTNGColliderStructure-20261023/upstream，未加入编译/资源 source sets。验证与剩余项见进度和测试文档。


## Native multiblock casing consolidation (2026-10-23)

- Verified against GT5-Unofficial 5.09.54.205, commit a3e1e11241a814c9fa0dd0973d5699548428f689,
  sources SHA-1 5447ea903c5833f0319123e38ac586f348e599f3 (LGPL-3.0-or-later).
- 38 previously ported casing variants now resolve to existing native block stacks for construction, recipe inputs,
  names and controller/hatch backgrounds. This overrides the earlier appearance-conversion recipe descriptions.
- Published legacy block IDs, metadata and artwork remain for saved items/structures. Legacy-only production recipes
  were removed; circuit 25 assembler recipes convert old stacks one-to-one to native equivalents.
- No additional upstream implementation or artwork was copied. Existing asset manifests remain byte-accurate.
  Full mappings, retained special casings and validation scope: docs/development/native-structure-casings.md.
