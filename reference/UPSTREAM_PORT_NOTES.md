# AE2LT faithful port audit

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

The official README at that commit explicitly assigns textures/models to CC BY-NC-SA 3.0 and text/translations to CC0. Java implementation headers specify LGPL-3.0-or-later. API licensing is separate (MIT). The README, LGPL text and relevant GUI source excerpts are retained under `reference/dependencies`; these excerpts are reference material only. AE2's original README/license and art attribution also accompany the production textures.

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
`client/render/overlay/OverlayRenderType.java` state definitions (LGPL-3.0-or-later).
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
