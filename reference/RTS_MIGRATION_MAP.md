历史记录：旧 RTS 预览实现已于 2026-09-24 删除。下文保留迁移过程，不代表当前入口或可运行检查；当前实现见 docs/testing/rts-official-integration.md。

# RTS Building → GTNG migration

## Official baseline — 2026-09-24 (supersedes all staged-preview notes)

The user selected Hcrab's official `forge-1.7.10` branch as the integration base.
Commit `c8bdff25ea9aa692c641fee231671afe58057a39` is now vendored in
`src/vendor/rtsbuilding`, including main/uiCore/uiKit and official resources.
The normal G-key entry runs the official interface and service stack inside GTNG.
GTNG owns item/entity registration, lifecycle and the packet-channel namespace.
The old preview no longer registers its client events by default.

GTNH AE2 rv3 API adaptation and null insertion-remainder handling replace upstream's
still-rv6 reflection signatures. Current host dependency versions are retained.
Validation and remaining acceptance scope: `docs/testing/rts-official-integration.md`.

The following staged-preview notes are historical, not the current architecture.

## Latest checkpoint (supersedes historical phase notes below)

The explicitly named development preview now joins detached camera/cursor selection, real inventory
equipment, native held/empty-hand interaction, progressive native mining, and bounded sequential
fill/mining tasks. Tasks support pause/resume/cancel, real backpack refill and missing-material waits.
PgUp/PgDn moves the selected region vertically. Server mining revalidates session, world, block and
real tool identity each tick; native harvesting owns drops, durability and BreakEvent. Close has a
bounded acknowledgement outbox independent of camera restoration.

This is a usable core-operation checkpoint, **not near-complete upstream parity**. Full upstream
GUI/animation, blueprint capture/rotation/ghosts, linked storage and crafting, persistent multi-job
workflow, GT/AE2/StructureLib-specific adapters, Java 17 full-pack and multiplayer acceptance remain
open. The development toolbar is not the final upstream top-bar port. See the player guide and
dated runtime evidence in `docs/testing/` for the actual tested scope.

## Pinned inputs and scope

* Behaviour reference: https://github.com/Hcrab/RTSbuilding at
  `b5a70d83a41d7149f7c34d4aae2081017532ab08` (Minecraft 1.21.1 / NeoForge).
* Legacy implementation reference: https://github.com/zslingy/RTSbuilding-1.7.10-GTNH at
  `5147af59f8e35cd7710db6359f7a240dd1892644` (Minecraft 1.7.10 / Forge).
* Reference checkouts are ignored, outside all production source/resource roots, and must never be shipped.
* This is a staged port, not a claim that the legacy branch is equivalent to current upstream.
  Initial review followed the lifecycle, camera, placement and mining execution paths below.
  Remaining GUI/blueprint/rendering owners are migration targets; their exhaustive behaviour and visual
  comparison is an acceptance requirement of their phases, not something established by this map.

## Actual target dependencies

Resolved using `gradlew dependencies --configuration compileClasspath` on 2026-09-24:

| Component | Resolved version |
| --- | --- |
| Minecraft / Forge / MCP | 1.7.10 / 10.13.4.1614 / stable_12 |
| Pack dependency convention | GTNH 2.9.0-beta-3 (not inferred from the workspace directory name) |
| GregTech | 5.09.54.133 |
| AE2 Unofficial | rv3-beta-1050-GTNH |
| StructureLib | 1.4.42 |
| GTNHLib | 0.11.46 |
| ModularUI2 | 2.3.88-1.7.10 |

GTNG uses Jabel and Java 8 target bytecode. The local build launcher is JDK 25;
Java 17 game-runtime validation is separate and must not be inferred from compilation.
The legacy fork targets AE2 rv3-beta-695 and GTNHLib 0.7.10, so its compat calls need revalidation.

## Integration points

* `CommonProxy.preInit`: existing common configuration, network and server event registration.
* `ClientProxy.init`: client event registration; common classes never import client classes.
* `common/packet/NetWorkHandler`: append discriminators to the existing `GTNotGood.channel`.
* `config/Config`: use the existing Forge configuration, Chinese inline config descriptions.
* `utils/keybind/KeyBindManager`: eventual configurable G entry and RTS actions.
* No changes to the managed `build.gradle.kts`, upstream GT/AE2/StructureLib, or a second config/network system.
* Existing QA pattern: opt-in test-only `@Mod`, FML tick handlers, disposable integrated worlds,
  Gradle init script, result file and screenshots. Reuse this pattern, not a new test framework.

## Migration map

Paths in the upstream columns are below `com/rtsbuilding/rtsbuilding` unless otherwise stated.
Destination roots are `com.xyp.gtnotgood.client.rts`, `common.rts`, and `common.packet`.

| Area | Original owner / path | 1.7.10 reference or target | API replacement / reusable logic |
| --- | --- | --- | --- |
| Entry | `client/bootstrap/ClientKeyMappings`, `network/camera/handler/RtsCameraNetworkHandlers` | `ClientProxy`, `KeyBindManager`, existing channel | G binding retained; NeoForge mapping/payload → FML KeyBinding/IMessage; explicit open/close rather than racing toggles |
| Client lifecycle | `client/controller/ClientRtsLifecycleOwner` | `client.rts.RtsClientState`, `RtsClientEvents` | Retain capture-once, clear-before-exit and death handoff; add connection/request identity to reject delayed replies |
| Server session | `server/camera/RtsCameraManager` | `common.rts.session.RtsSessionManager` | Server establishes dimension/anchor/range; Netty only queues, ServerTick executes; bound queue and expire abandoned sessions |
| View restoration | `client/service/CameraViewRestoration` | `client.rts.RtsViewSnapshot` | CameraType/options → renderViewEntity/thirdPersonView/viewBobbing; restore mouse and movement-input ownership on all exits |
| Camera | `CameraOrbitService`, `CameraMotionSolver`, `CameraVisualPoseState` | fork `CameraViewModel`, `CameraInputHelper`, `entity/RtsCameraEntity`; target `client.rts.camera` | Detached client render entity; no player teleport. Preserve snap yaw, initial pitch 70°, height 18, dolly and bounds |
| Smoothing | `client/service/RtsCameraSmoothingMath` | `client.rts.camera` | Pure exponential approach, residual consumption and wrapped-angle math reusable; prefer current upstream over fork tick-dependent EMA |
| Keyboard | `CameraInputSampler`, `BuilderScreen` key router | fork `RtsInputRouter`; target `client.rts.input` | GLFW → LWJGL2; honour GUI focus and remapping, neutralize only player input, leave gravity/knockback intact |
| Mouse / drag | `BuilderScreen` pointer gesture owner | fork `RtsScreen`, `CameraInputHelper` | Right drag rotate, middle drag pan; preserve click/drag thresholds, capture/release and drag inversion |
| Zoom / height | `CameraMotionSolver.solve` | camera solver adapter | Scroll dolly 2.6, vertical 0.32 / fast 0.55, yaw/pitch gains 0.24/0.22; modern Mth → MathHelper/pure math |
| GUI layout | `BuilderScreen`, `BuilderScreenConstants`, `RtsMainlineLayout` | fork `RtsScreen`, `client/panel/*`; target `client.rts.gui` | Screen/GuiGraphics/PoseStack → GuiScreen/Tessellator/GL11; preserve coordinates, resize and docking, not a MUI redesign |
| HUD / tooltips | screen render owner, top/bottom panel owners | fork top/bottom/storage panels | Keep layering, selection/hover, item rendering and tooltip timing; translations beside Java usages |
| Cursor ray | `client/rendering/util/RaycastHelper` | fork `RtsInteractionHandler`, target `client.rts.selection` | ClipContext → world.rayTraceBlocks and collisionRayTrace; cursor ray must match rendered camera, not player look |
| Selection | `BuildPlacementService`, shape/capture controllers | fork selection/view-model owners | Item identity includes damage and NBT; preserve build shapes, rectangle selections and captured hit face/offset |
| Placement | `RtsPlacementService` → `PlacementExecutePipe` → batch → `RtsPlacementExecutor` | rewrite `common.rts.action` | Modern useItemOn → 1.7.10 ItemInWorldManager/ItemStack placement with Forge hooks; do not copy fork setBlock paths |
| Breaking | `server/service/mining/RtsMiningStateMachine` | rewrite `common.rts.action` | Progressive hardness + actual tool lease + tryHarvestBlock; no tool-copy durability loss or setBlockToAir fallback |
| Interaction | `RtsInteractionServiceImpl` → tool/linked-item/empty-hand interactors | rewrite `common.rts.action` | Preserve block/item hook ordering, sneaking and container semantics; permission/range checks precede operations |
| Material source | `ConstructionMaterialSources`, transfer/refund helpers | `IMaterialSource`, `PlayerInventoryMaterialSource` | Modern ItemStack.EMPTY/capabilities → nullable legacy stacks/inventory; real stack mutations and remainders, server only |
| Tasks | placement batch, workflow/task state | `common.rts.action` / task queue | Preserve bounded per-tick work, cancellation, suspended missing-material jobs and progress updates |
| Blueprint | `common/blueprint/transform/BlueprintTransform`, planner, capture | fork `blueprint/*`; target `common.rts.blueprint` | Coordinate/quarter-turn algorithms reusable; BlockState property rotations require per-item/block adapters, never overwrite machine metadata |
| Preview / ghost | `BuildGhostRenderer`, `ShapeGhostRenderer`, `BlueprintGhostRenderer`, `GhostBlockModelRenderer` | fork `client/render/*`; target `client.rts.render` | Modern buffers/model API → RenderBlocks/Tessellator/GL11 with complete render-state restoration; TE/parts need separate previews |
| World render | camera render sync, culling ray clipper | RenderWorldLastEvent / render tick | Keep interpolation (`prevPos` AND `lastTickPos`), loaded-chunk boundaries and camera ownership; Angelica verification required |
| Animation / state | `RtsPanelControlAnimations`, placement/break animation payloads | client render and GUI owners | Keep timing/state transitions; replace only clock/render/payload APIs |
| Network | client packet gateway, server handlers, typed payloads | common.packet + sided proxy | Validate length/op/dimension/connection/session/request/position/range/slot/item/permission; server thread executes |
| GregTech | no dedicated adapter found in legacy fork | `common.rts.compat.gregtech.GTCompat` | Actual GT machine ItemBlock/tools; preserve machine ID, stack NBT, covers and facing; verify real tile/multiblock updates |
| AE2 | fork `compat/ae2/RtsAe2Compat` primarily storage | `common.rts.compat.ae2.AE2Compat` | AE2 part Item placement, never assume ItemBlock; validate PartHost/grid/channel/NBT with actual rv3-beta-1050 |
| StructureLib | GTNH dependency 1.4.42 | `common.rts.compat.structurelib` | Definition → immutable plan/materials/ghost transform → same real-placement queue; API foundation initially, not instant world edits |

## Traced differences that prohibit a wholesale import

1. Fork `C2SRtsPlaceMessage.Handler` reaches `RtsStorageManager.placeBlockDirect` (also reached by
   `PlacementExecutePipe`). It calls `world.setBlock` and changes item metadata to rotation steps.
   `RtsPlacementQuickBuild.placeStateBatchEntry`, `RtsPlacementService` and `HistoryExecutor` have
   additional direct-world-write paths. Every entry, including undo/redo/blueprints, needs review.
2. Fork `MiningOperationService` equips `toolPrototype.copy()`, temporarily teleports the real player,
   and catches failures with `setBlockToAir`. Restoration is not in a finally block. Do not import.
3. Fork `SessionValidatePipe` only verifies a player/world and obtains a storage session; that is not
   an authenticated live RTS camera session. Validate at the final execution boundary as well.
4. Fork camera packet handlers directly enter camera management. GTNG follows its existing
   `ServerConfigService` queue-on-Netty / execute-on-server-tick pattern instead.
5. Original current placement uses `ConstructionMaterialSources.extractOne` and real-main-hand
   interaction helpers with remainder refund. This is the behavioural reference for the rewrite.
6. Original UI now delegates layout/render/input to more owners than the legacy screen. A legacy
   GUI compiling is not evidence of visual parity. Compare actual screens before completing Phase 10.

## Ordered implementation and acceptance

| Phase | Planned files / integration | Acceptance |
| --- | --- | --- |
| 1 | this map, upstream notes, pinned provenance | source/license review, resolved dependencies, baseline compile |
| 2 | session protocol/service/message, client state/snapshot/events, proxies/config | request races, stale connection/reply, timeout, revoke, death/dimension/disconnect, repeated cleanup; live lifecycle QA |
| 3 | camera entity/solver/input, key bindings | detached camera, smooth movement/dolly/drag/bounds, original view restored; in-game checks |
| 4 | GUI/HUD owners and independently authored art | actual upstream layout; no substitute simplified production screen |
| 5 | selection/raycast/render | cursor ray matches world target/face at varying view/FOV/scale |
| 6–9 | action service/packets/material leases | survival place/break/use, cancelled Forge events, tools/NBT/remainders, no direct edits |
| 10 | complete panel/input/animation port | original-versus-port visual and interaction checklist |
| 11–12 | GTCompat, AE2Compat | machine/cable/pipe/hatches/covers/tools and AE parts/grid/channel checks |
| 13 | StructureLib plan adapter | rotation/material plan feeds the same placement service |
| 14 | existing client/server QA entry pattern | dedicated server, multiple players, reconnect, denial/abuse and Java 17 runtime |

Each implemented phase runs compileJava, available formatting/check/test tasks and focused runtime
verification. No production RTS menu is exposed by the Phase 2 foundation alone; the public entry
must wait for the camera and faithful GUI. Test-only screens may exercise ownership/restoration.

## Current implementation checkpoint

Phase 2: Java 8 lifecycle runtime probe passed before the subsequent cursor/action additions.
Phase 3: production detached camera and input adapters compile; physical drag/smoothing acceptance remains open.
Phase 4: faithful production screen/layout port remains open; no simplified replacement is exposed.
Phase 5: matrix-based cursor picker and bounded cuboid state implemented; 7 geometry tests pass,
with actual center-ray checks added to the next graphical probe.
Phase 6: native held-item block-use request/service/result path implemented. This uses the actual
selected inventory stack and ItemInWorldManager, with Forge hooks and no direct world writes.
Session/token/dimension, selected slot/item signature, loaded chunk, build height, horizontal lease
bounds, spawn/world protection, replay fence and one queued request per connection are enforced.
The GUI-facing endpoint is RtsClientState.useHeldItem; native ACCEPTED means handled, not necessarily
placed (a block may open a container). No full production GUI dispatch is claimed.
Survival consumption/replay/Forge-denial assertions are prepared in the opt-in runtime probe but
have not run. Empty-hand interaction, progressive mining, external material sources, batches,
orientation adapters and machine/multipart acceptance are still pending.
The production GUI work is the next integration priority; the lower-level endpoints now allow its
actual controls to be connected without placeholder world edits.

Phase 4 continuation: upstream mainline top/bottom geometry and clipping primitives are now ported,
with original baseline-coordinate regression tests. RtsScreenInput routes widget-approved events
into the camera, two-corner selection and native held-item use endpoints. Full screen composition,
textured controls, inventory panels, animated states and the public entry are not yet complete.

Runtime update: center-ray/top-face and single-block selection passed in the visible Java 8 probe.
Fresh-world survival use, actual consumption, duplicate suppression and Forge cancellation passed
in actions-only mode (build/rts-actions-isolated.log). Combined post-reconnect acceptance is still
open: the current full probe has a native acknowledgement failure and subsequent CoFH ore-dictionary
login crashes. See docs/testing/rts-lifecycle.md for evidence; do not extrapolate the focused PASS.
## Floating-window input continuation

Ported upstream drag/eight-edge resize, scrolling/visible-range and tooltip placement models.
RtsWindowLayer gives the screen one pointer owner, consistent draw/hit order, child-control priority,
capture across frames and out-of-window drags, close-before-release handling and focus/modal cleanup.
RtsScreenInput owns this layer and blocks camera/world input while a window owns the gesture.
Seven added regressions exercise overlap, drag bounds, release ownership, close/focus cleanup,
minimum resize, large-list bounds and tooltip fallback. Full production panel composition is still open.
## Bottom-bar data continuation

Ported the upstream bottom-bar state, command reducer and grid layout, including original regression
cases. Added read-only player inventory snapshots, name/id filtering, pagination, selected-entry
state and NBT-preserving icon copies. RtsScreenInput refresh/close owns catalog lifetime.
The panel renderer, creative/storage backend adapters and selection-to-material commands remain open.
## Manual development entry

The user requested a usable manual entry after the isolated probes. G now requests a server session
and opens RtsDevelopmentScreen; ESC restores the previous view. The preview connects camera input,
bounded selection with world outline, inventory search/paging and native held-stack use. It preserves
the ported bar/grid geometry but is explicitly not the finished faithful upstream GUI. Full panel
composition, storage/crafting/mining and compatibility acceptance remain on the migration plan.
## Bottom toolbar integration

The manual preview now consumes upstream search/clear/pager and sorting/height layout owners for
both drawing and dispatch. Quantity/mod/name ordering and direction are local to read-only inventory
snapshots; slot identities, NBT and selected source slot survive reordering. Panel resize uses the
upstream step and minimum rows. Java 8 runtime verifies a populated two-page inventory plus screen
exit/reopen. Full upstream theme, categories, remote storage/crafting and top-bar feature wiring remain open.
