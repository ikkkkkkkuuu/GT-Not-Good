# AE2LT faithful port audit

## RTS official Forge baseline — 2026-09-24

Source: https://github.com/Hcrab/RTSbuilding/tree/forge-1.7.10
Pinned commit: `c8bdff25ea9aa692c641fee231671afe58057a39`.
Reference: `reference/RTSbuilding-official-forge-1.7.10`, ignored and not a build input.
Destination: `src/vendor/rtsbuilding/java` and `src/vendor/rtsbuilding/resources`.
User explicitly requested source and original-resource packaging in GTNG.
Original source LGPL-3.0-only and original-media LICENSE-ASSETS notices remain intact;
this records the packaging instruction, not a new license grant or MIT relicensing.
Full notices: `src/vendor/rtsbuilding/licenses`; packaged under `META-INF/licenses/rtsbuilding`.

Changes: merge lifecycle into GTNG; remove standalone mod discovery; register plugins
through GTNGItem/GTNGItemList and host creative tab; retain original resource namespace;
dedicated host RTS packet channel; relocate the five official early mixins to host config;
adapt AE2 reflection from rv6 to GTNH rv3 and fix successful insertion's null remainder;
adapt optional startup QA for custom menus, unique worlds, API checks and screenshots.
The host's GTNHLib/GT/AE2/MUI/StructureLib dependencies are retained without downgrading.
Follow-up crash fix: correct destruction/mining/placement progress NBT type checks
to the actual 1.7.10 int-array encoding; accept native long lists in placement job
definitions; use the registered GTNG instance for remote chunk tickets.
`OFFICIAL_IMPORT_MANIFEST.json` records each imported source/resource path and original hash.
Unchanged upstream translations are imported verbatim; new GTNG translations continue
to use adjacent Java `#tr` declarations.

The older RTS staged-port section below is historical and does not describe this import.

## RTS Building staged port (2026-09-24)

Behaviour source: https://github.com/Hcrab/RTSbuilding, commit
`b5a70d83a41d7149f7c34d4aae2081017532ab08`, local `RTSbuilding/`.
Legacy API reference: https://github.com/zslingy/RTSbuilding-1.7.10-GTNH, commit
`5147af59f8e35cd7710db6359f7a240dd1892644`, local `RTSbuilding-1.7.10-GTNH/`.
Both are ignored reference-only checkouts, outside production source/resource roots.

Inspected original `LICENSE`, `LICENSE-ASSETS`, `ASSET-LICENSES.md`, `License.md`, and legacy
`LICENSE.txt`. Original source/non-media files are LGPL-3.0-only; original visual/audio paths
are all-rights-reserved with redistribution permission for the complete unmodified official mod.
Do not transfer original or fork textures into GTNG based on the fork's root license alone.
Earlier asset grants need path/commit-specific proof before reuse. Current phase copies no media.

Migration review: [RTS_MIGRATION_MAP.md](RTS_MIGRATION_MAP.md). Phase 2 destinations are
`client/rts/*`, `common/rts/session/*`, and `common/packet/RtsSessionMessage.java` under the GTNG
Java package. These are newly authored Forge 1.7.10 implementations of the inspected lifecycle
semantics, not wholesale copies of the fork. Changes include explicit open/close messages,
connection-bound request generations, server tick dispatch, server-issued expiring leases,
and idempotent restoration of camera/input/mouse on screen/world/player replacement.
There are no copied algorithms or visual assets in this initial foundation.

Packaged provenance is `META-INF/rts-port/NOTICE.md` and `ASSET_MANIFEST.json` (empty assets).
When subsequent phases adapt LGPL source, append exact source/destination/modification records
and package the required source license notices; never describe adapted LGPL files as MIT.

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
  3x2x1 StructureLib shape; fixed 144 pattern slots; fixed singularity CPU and maximum matrix parallel;
  one-tick matrix cycles; no controller, dispatch or storage-transfer energy cost; overflow,
  interrupted-output and persistence fixes. No upstream raster assets copied.
- Translation strings adapted from upstream English/Chinese resources into adjacent Java #tr comments.
- Full upstream checkout remains in the OS temporary reference directory, outside all source sets.

RTS Phase 3 source adaptation: CameraMotionSolver.java and RtsCameraSmoothingMath.java from Hcrab/RTSbuilding commit b5a70d83a41d7149f7c34d4aae2081017532ab08, LGPL-3.0-only, relocated to client/rts. Records replaced with Java 8 immutable classes; Mth clamps replaced with Math. Packaged manifest and licenses updated. Compilation pending: C drive exhausted during this work; truncated this task's runtime log and restored NOTICE.md. No graphical client launched.

RTS Phase 3 continuation: RtsCameraController adapts CameraOrbitService and CameraVisualPoseState
from the same Hcrab pin under LGPL-3.0-only. See packaged manifest for destinations/modifications.
RtsCameraEntity and RtsCameraInput are local Forge/LWJGL 2 adapters. Detached cameras are never
spawned, and camera input never grants world-edit authority. Frame updates close on camera ownership
loss rather than fighting another mod. Mouse gestures cancel over UI panels; captured-cursor rotation
and full sensitivity GUI parity remain pending with the faithful screen port.

RTS cursor/selection continuation: inspected pinned RaycastHelper, ScreenCursorPicker,
ShapeSelectionSession and ShapeSelectionLimiter. Added newly authored RtsRayMath,
RtsCursorPicker and RtsSelectionState (no additional upstream source or media copied).
The adapter inverts the actual 1.7.10 render matrices rather than rebuilding a ray from option FOV.
The client-only bounded voxel walk invokes each block's collisionRayTrace and retains the vanilla
hit face, offset and subHit. It stops at EmptyChunk/unloaded client chunks; chunkExists alone is not
valid because vanilla ChunkProviderClient always returns true. A 128-block cursor reach matches the
pinned ScreenCursorPicker; cuboid storage is bounded to 4096 cells locally, with no world changes.
Full shape/handle/culling/entity-picking and GUI input routing remain separate unfinished work.

Native held-item interaction adapter: newly authored RtsUseBlockMessage, RtsUseBlockService and
RtsActionLedger; no additional upstream code/media copied. The design follows the migration map's
real-player/material contract using local Forge 1.7.10 ItemInWorldManager.activateBlockOrUseItem.
The actual held stack is used, not a copied prototype. Sneak state is temporarily scoped in finally;
player position, reach, item metadata and NBT are not rewritten by the adapter. Hook failures consume
the request sequence and report an indeterminate error rather than retrying or undoing partial mod effects.

GUI foundation: ported RtsMainlineLayout, UiRect, UiInsets and UiClipStack from the pinned upstream
uiKit/uiCore source sets into client/rts/gui. LGPL-3.0-only retained, package names relocated;
coordinates, half-open hit rectangles and nested clipping semantics unchanged. The upstream four
RtsMainlineLayoutTest cases are ported to JUnit 4 with their coordinate assertions unchanged.
RtsUiHitRegions and RtsScreenInput are newly authored adapters connecting those rectangles to
camera/selection/native-use endpoints. Source/test mappings are in the packaged manifest.
No restricted texture was copied. Full production screen composition and drawing are still open.
# RTS floating-window interaction continuation

Source: Hcrab/RTSbuilding at b5a70d83a41d7149f7c34d4aae2081017532ab08, LGPL-3.0-only.
Inspected root LICENSE and README's separate all-rights-reserved media boundary before copying.
No media copied. Four pure Java classes were ported into client/rts/gui:
UiWindowInteractionModel (uiKit/window), UiScrollModel and UiVisibleRange (uiKit/scroll),
UiTooltipPlacement (uiKit/tooltip). Changes are package relocation, license header, nested-type
Javadoc and formatting; original geometry/scroll/fallback algorithms remain intact.
Full source and destination paths are in META-INF/rts-port/ASSET_MANIFEST.json.
GTNG-authored RtsWindowLayer owns stacking and capture and RtsScreenInput connects it to the
existing camera/world dispatch. These are interaction foundations, not a completed upstream screen.
# Bottom-bar state and inventory continuation

Pinned Hcrab/RTSbuilding b5a70d83a41d7149f7c34d4aae2081017532ab08, LGPL-3.0-only:
ported uiCore/bottom/BottomBarUi* state/action/reducer/value types, uiKit/layout/BottomPanelGridLayout,
and the four original reducer tests. Package relocation, JUnit 4 adaptation, headers/Javadoc and
formatting are recorded in the packaged manifest. Grid hit testing additionally rejects NaN/infinity.
No upstream media copied. RtsInventoryCatalog is a GTNG-authored read-only inventory bridge; it does
not implement remote storage extraction, creative item grants, crafting or selection-to-held-slot changes.
# Camera right-axis adaptation correction

CameraMotionSolver's upstream lateral basis was incompatible with this port's positive-D input.
Changed strafe and pan-right to (-cos(yaw), -sin(yaw)), verified against MC 1.7.10's actual
EntityRenderer Rx(pitch)*Ry(yaw+180) transform. Forward, dolly and vertical pan were preserved.
Recorded in the camera source manifest entry; no upstream media involved.
# Bottom toolbar controls continuation

Pinned Hcrab/RTSbuilding b5a70d83a41d7149f7c34d4aae2081017532ab08, LGPL-3.0-only:
ported uiKit/layout/BottomPanelBrowseLayout and BottomPanelSortLayout and their uiKitTest cases.
Source/destination/license/modification records are in the packaged manifest. Geometry is unchanged;
packages, Javadoc and JUnit 4 imports are adapted. No media copied.
The manual preview now consumes the same upstream rectangles for drawing and clicking clear/search,
previous/next page, sorting/direction and height controls. Height changes use upstream's 22-pixel step.
The local inventory adapter sorts separate real slots by quantity/mod/name without moving any items.
Minecraft 1.7.10 tooltips display actual stack metadata; final upstream theme parity remains open.

# Native operations and sequential task continuation

Behavior references remain pinned to Hcrab b5a70d83a41d7149f7c34d4aae2081017532ab08:
RtsMiningStateMachine, ConstructionMaterialSources and PlacementExecutePipe. The 1.7.10 adapters
RtsMiningOperation, RtsTaskQueue and RtsCloseHandshake are newly authored, not copied upstream source.
Mining accumulates native player-relative hardness and calls ItemInWorldManager.tryHarvestBlock;
it deliberately does not arm the legacy uncancellable deferred-finish flag. Original GUI parity,
linked storage, histories and persistent workflow remain pending; these adapters are not substitutes
for acceptance of those upstream systems. No upstream media was copied.

## Official quick-drop null-stack compatibility — 2026-09-24
Pinned official source remains c8bdff25ea9aa692c641fee231671afe58057a39. RtsTransferExtractor now uses StackCompat.count for nullable linked/hotbar/network extraction results. This preserves extraction priority and limits while accepting Minecraft 1.7.10 null empty stacks. Added independently authored OfficialRtsQuickDropTest and an empty-source quick-drop packet probe in the opt-in startup smoke. No assets changed.

## GT native quick-build placement — 2026-09-24
Inspected the installed GT5-Unofficial 5.09.54.133 sources, gregtech/common/blocks/ItemMachines.java placeBlockAt. RTS quick build now delegates ItemMachines placement to that native hook with the original prototype and clicked face, rather than setting only block metadata. This preserves MetaTileEntity initialization, ownership, connection hooks and synchronization; it avoids repeating generic placement callbacks afterwards. No GT source or assets copied. Official RTS pinned revision remains unchanged.

## Full-chunk tile description synchronization — 2026-09-24
User follow-up exposed a second GT issue in RtsRemoteMenuChunkLease. The pinned RTS branch sent S21PacketChunkData for distant interactions, but sent only the clicked tile description later. A controlled pre-fix client run at more than eight blocks reproduced loss of the first controller subtype after placing the second in the same chunk (build/rts-remote-repro-client.log; remote-before-fix.txt). The 1.7.10 adapter now sends every live tile's native description packet immediately after full chunk data, isolating failures per tile. Inspected GT5 5.09.54.133 CommonBaseMetaTileEntity.getDescriptionPacket (native S35 data); no GT source copied. Near-only prior tests did not cover this lease path.

## Nullable durable placement prototype — 2026-09-24
The user's 15:29:31 crash is PlaceBatchJob.itemPrototype copying a null optional prototype. The constructor and NBT format already permit absent prototypes (empty-hand/current-tool tasks); the getter now uses StackCompat.copyOrNull. Added restore/resave and unreadable-prototype regression cases plus an actual sendEmptyHandPlace durable-task probe. No inventory semantics, task format or assets changed.

## GT rotation and destruction history compatibility — 2026-09-24
Added independently authored common/placement/GtMachineRotation.java. Both client rotation handles and the server resolve legal facings through IGregTechTileEntity; server mutation uses setFrontFacing and markDirty, preserving tile identity and GT notifications. Inspected installed GT5 5.09.54.133 BaseMetaTileEntity.setFrontFacing and CommonBaseMetaTileEntity.isValidFacing. No GT source copied. Existing RTS permission/range checks remain in effect.
RtsDestructionBatch now serializes native case-sensitive registry names and metadata, instead of converting names through ResourceLocation and dropping metadata. Unavailable history records are logged/skipped so terminal task history cannot crash the server; other records are retained. The startup probe checks all mixed-case block registry entries for metadata-preserving history round trips and performs four real GT rotation requests.

## Range-handle cursor alignment — 2026-09-24
RtsCursorRay now captures the actual world model-view/projection/viewport before RTS overlays and unprojects framebuffer mouse coordinates. The previous analytical ray used raw FOV and eye position, which need not match the rendered 1.7.10 camera transform. Cached frames are rejected on camera/world/size changes; the analytical path remains a pre-frame fallback. Six-axis arrow picking gains a symmetric 0.12-block tolerance without changing drawn geometry. The opt-in smoke projects each visible arrow head through the captured matrices and checks a picking ray through that pixel. No assets changed.

## Plugin item display names — 2026-09-24
RtsPluginItem now translates the full .name key directly, falling back to the bare upstream key. Upstream includes both key forms; vanilla 1.7.10's getUnlocalizedNameInefficiently translates the bare key before appending .name, producing translated text followed by the literal suffix. The shared item override covers all plugins without editing language assets. Assembly passed.

## Linked AE drop insertion crash — 2026-09-24
User confirmed AE network linked before area destruction. RtsAggregateStorage.trackChange now treats a null insertion remainder as zero, matching native 1.7.10 success semantics. Added inventory full/partial/complete/simulation and slot-independent AnySlotInsertItemHandler regression coverage. Also fixed the analogous unchecked remainder count in linked-storage-to-player transfer. No changes to AE permissions or insertion policy.

## Linked network browser refresh — 2026-09-24
LinkedItemHandlerView now forwards RefreshableSnapshotHandler so the browser cache can refresh AE snapshots after network changes. RtsAe2Compat.getReportedCount accepts the shared reported-count interface as well as its legacy alias. Changes are independently authored compatibility adaptations; no new assets.

## Built-in unlocked RTS harvesting — 2026-09-24
GTNG now disables plugin progression regardless of legacy setting, granting existing unrestricted capabilities and unlimited mining tier. Physical tool borrowing, harvest eligibility and durability gates are removed from RTS mining. Independently authored RtsToollessHarvest retains Forge break-event cancellation, native removal/harvest callbacks, and unbreakable-block guards. Held stack and creative state are restored in finally; no physical tool or fortune/silk-touch bonus is applied. Native drops feed the existing capture/storage path.

## Top-bar texture decode recovery — 2026-09-24
Added independently authored TopBarTexture adapter. It checks ImageIO null decode results before legacy GL upload, respects resource-pack priority, falls back to the bundled icon on IOException, and lets TextureManager handle unrecoverable image errors. TopBarIconRenderer registers this reloadable texture type before first binding. No artwork modified. Supplied log identifies mode_rotate_active.png but does not identify which resource pack/file supplied its bytes; all 52 local top-bar PNGs decode successfully.

## Inventory RTS button removal — 2026-09-24
Removed the GuiInventory initialization subscriber that added the RTS plugin-management button. Existing client configuration cannot recreate the removed entry point. Keyboard RTS entry remains available.

## Legacy preview removal — 2026-09-24
Removed 60 client preview classes, five common RTS classes, both legacy packet classes, legacy registrations/proxy delivery/key binding/config fields, and preview-only tests. Retained OfficialRts regression fixtures and the embedded official integration. Historical ASSET_MANIFEST moved out of runtime resources to reference/RTS_LEGACY_ASSET_MANIFEST.json; NOTICE updated. Main addon packet discriminators preceding the removed tail entries remain unchanged.

## Host mixin source layout — 2026-09-24
Moved the five already host-packaged early RTS mixin sources from the upstream directory layout into src/main/java/com/xyp/gtnotgood/mixins/early/rts, matching their unchanged declared package and mixin JSON names. Updated the import manifest generator to map upstream paths to the relocated files and removed its obsolete dependency on the retired preview asset manifest. Runtime class names and injections are unchanged.

## Unsupported JEI source cleanup — 2026-09-24
Removed seven compat/jei source files and RecipeRegistryOverlayTransferMixin from the working import. They were already excluded from compilation and never active in the NEI-based 1.7.10 runtime. Removed obsolete Gradle exclusions; manifest generation explicitly skips these unsupported upstream sources, preserved in the pinned reference checkout. No NEI feature was removed. Compile, checkstyle and assembly pass.
