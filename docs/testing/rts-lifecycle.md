历史记录：旧 RTS 预览实现已于 2026-09-24 删除。下文保留迁移过程，不代表当前入口或可运行检查；当前实现见 docs/testing/rts-official-integration.md。

# RTS Phase 2 lifecycle verification

Status on 2026-09-24: the earlier Java 8 lifecycle probe passed; subsequent cursor checks and
fresh-world native held-item actions passed. Current combined post-reconnect acceptance remains
open. GUI interaction foundations have 33 passing focused tests and passing compile/checkstyle;
the production GUI and physical input acceptance remain unfinished. Details and Java 17 limits follow.

## Window preference

The user requires graphical clients on Desktop 2 without exposing them on the current taskbar.
Do not automatically start a visible client on the current desktop. The QA init script refuses
client launch unless `-PrtsVisibleQa=true` is explicitly supplied after the desktop is arranged.
That flag is an acknowledgement, **not** an implementation of desktop movement or hidden windows.
There is no hidden-window startup option in the inspected lwjgl3ify Display implementation.
The current window tool does not expose virtual-desktop movement. The user explicitly allowed
the current desktop for this test session; this does not cancel the general Desktop 2 preference.
Background compilation and JUnit checks do not open a game window.

## Commands

On this workstation the Gradle launcher requires the existing project workaround:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing'
.\gradlew.bat compileJava
.\gradlew.bat test --tests 'com.xyp.gtnotgood.client.rts.*'
```

After arranging the graphical desktop:

```powershell
.\gradlew.bat -I scripts/rts-qa.init.gradle runClient17 -PrtsVisibleQa=true
```

`runClient` is also supported for legacy Java 8 diagnostics; it is not evidence of Java 17 acceptance.
The test-only mod follows the existing Advanced IO QA pattern: a disposable `rts-lifecycle-qa-*`
world, classpath snapshot, FML tick checks, screenshot, result file and automatic exit.
Release jars do not contain the probe screen or QA mod. The detached camera is production code.

## Evidence and limitations

* Baseline `compileJava`: passed before edits.
* RTS protocol/state unit suite: five tests passed, covering cancellation/late grants, stale token and
  dimension replies, server revocation, heartbeat/timeout, unauthorized activation, wire round-trip,
  all truncated packet lengths, trailing bytes and non-finite/out-of-world anchors.
* Modified Java formatting, compileJava, checkstyleMain and checkstyleTest: passed in focused checks.
* Full local test run: 101 tests, two failures in pre-existing local `FluxDropDataTest`:
  `logisticsChannelTargetsCargoAndUnknownTagsRemainIntact` and
  `customEnergySettingsSurviveNormalizationAndReload`. RTS tests passed. Flux files were not changed;
  no claim that the complete project check is green.
* An unscoped Spotless apply reformatted four unrelated existing files. Those changes were removed;
  subsequent formatting was scoped to the RTS files and their integration points.
* Initial Java 8 real-client run reached authorized entry, detached probe-camera ownership, a heartbeat,
  actual ESC cleanup, server lease revocation and immediate-cancel/late-reply rejection.
  `run/client/screenshots/rts-lifecycle-view.png` is only a **test fixture** image, not a finished RTS GUI.
* The next assertion used a vanilla inventory screen by identity. Creative mode replaces that screen
  with its creative inventory, so the probe was corrected to use `GuiIngameMenu`. That correction and
  the death/dimension/disconnect/reconnect stages have not yet completed a new graphical run.
* Java 17 attempt initially failed before mod initialization because Angelica's RFB plugin could not
  resolve `xyz.wagyourtail.jvmdg.j18.stub.java_base.J_L_System`. A test-only JVM Downgrader API artifact
  now supplies this early launcher dependency; it is not a production runtime dependency.
* The next Java 17 attempt passed that point but failed during existing mod initialization, reporting
  `mods/railcraft/common/items/firestone/IItemFirestoneBurning` missing, with a CropsNH shaded MCLib
  `NoOp` error also logged. Java 17 world loading and dedicated/multiplayer acceptance are unverified.
* Visible launches were stopped at the user's request. No background process should auto-restart them.

## Remaining acceptance

Complete the graphical lifecycle script on the arranged desktop, resolve the Java 17 development
classpath without changing upstream mods, then verify dedicated-server startup and session isolation.
The camera/GUI/action migration phases listed in `reference/RTS_MIGRATION_MAP.md` remain outstanding.
Every action implementation must validate permissions/materials on the server independently of the
session lease; Phase 2 grants no remote world-edit ability.

## Phase 3 continuation

Disk space recovered. Added upstream movement/scroll/smoothing math and detached production camera,
client render/tick integration, focus-loss input reset, and a screen-owned LWJGL 2 input adapter.
The lifecycle probe now attaches the production camera rather than EntityOtherPlayerMP.
No graphical launch was performed. The production screen and its input dispatch remain pending;
the adapter is not registered globally and cannot interfere with other screens.
Nine focused tests pass: five session/protocol tests and four camera math tests. Camera tests cover
view-direction movement/dolly, anchor bounds, quarter-turn snapping, 30/60/144 FPS scroll conservation,
shortest-angle interpolation, invalid time and settling after key release.
The detached view uses yOffset=1.62 because the actual 1.7.10 EntityRenderer derives its origin from
that field. Render/tile alignment, live input and camera handoff still require arranged Desktop 2 QA.

Final focused validation: compileJava, processResources, all nine RTS tests, checkstyleMain and checkstyleTest passed. No whole-project test success is claimed. Unrelated formatter-only and generated-language changes were removed.

## Current-desktop runtime verification (2026-09-24)

User explicitly authorized the current desktop for this session. The Java 8 rerun completed all
stages with `RTS_QA PASS` and `BUILD SUCCESSFUL` at 11:22:49, then exited automatically.
Evidence: `build/rts-qa-visible-final.log` and `build/rts-qa-java8-result.txt` (PASS).
Verified server lease ownership/revocation, detached camera and neutral input, ESC, cancelled opens,
replacement GUI, death/respawn, dimension change, disconnect/reconnect, repeated close and config off.

The initial screenshot revealed the real player's held item, crosshair and hotbar over the detached
view. RTS client render events now suppress those only while the RTS session is active. The rerun
screenshot `run/client/screenshots/rts-lifecycle-view.png` was inspected and confirms their removal.
It is a lifecycle fixture, not the finished upstream GUI, and does not validate physical mouse/key
interaction, tile-entity rendering, placement or breaking.

Two test-fixture timing issues were corrected: wait for the server's post-transfer acknowledgement
before reading its lease status, and apply vanilla portal cooldown after scripted creative-mode
transfer so the arrival portal cannot immediately return the test player to the overworld.
No production server lease logic was relaxed to satisfy these assertions.

Java 17 was rerun against the current sources and still fails before world load with missing
`mods/railcraft/common/items/firestone/IItemFirestoneBurning`; the CropsNH shaded CGLIB NoOp
class-loading failure is also present. Evidence: `build/rts-qa-java17-current.log` and
`run/client/crash-reports/crash-2026-09-24_11.23.36-client.txt`.
Java 17, dedicated-server and multiplayer acceptance remain open. No RTS QA client remains running.
Use `--no-configuration-cache` for the opt-in graphical QA script to avoid unsupported Project capture.

Post-fix compile/test and checkstyleMain/checkstyleTest passed; the RTS suite remains 9 tests with no failures. Generated unrelated factory translation changes were removed.

## Cursor and cuboid foundation (headless continuation)

Added render-matrix capture, pure matrix inversion/unprojection and a bounded client voxel walk.
The picker preserves block-specific collisionRayTrace results, including sides and multipart subHit,
without calling the server or traversing unloaded client terrain. Session close clears captured
world/camera references and selection state. The GUI must explicitly dispatch world input after
widget handling; no global mouse listener or placeholder production GUI was installed.
Cuboid selection validates world height, lease X/Z bounds and volume before accepting a preview.
A miss/invalid endpoint cannot confirm a stale preview; cancel retains the previous committed box.
Seven geometry tests cover aspect/direction, camera translation/rotation, singular and NaN matrices,
negative coordinates, stationary axes, diagonal ties, bounded traversal, reverse drag and invalid selection.
The real-client probe now also asserts a center-cursor top-face hit and a one-block selection.
Those new runtime assertions have NOT been run; the earlier Java 8 PASS predates this change.
Shaders, partial/multipart shapes and full selection GUI behavior remain runtime acceptance work.

Cursor continuation validation: compileJava, 16 focused RTS tests, checkstyleMain and checkstyleTest passed (build/rts-selection-final.log). No graphical client was started for this continuation.

## Native held-item action continuation

Added fixed-size request/reply packets (69/31 bytes), a bounded server-thread queue, per-lease replay
fence and a client one-outstanding-action endpoint. Native Forge right-click handling receives the
real inventory stack. Loaded chunks, both clicked and adjacent target bounds, world/spawn protection,
current slot/item identity and live lease are checked before invoking mod code. No direct world writes,
prototype tools or player teleport occur. The hash is a stale-view check, not an inventory authorization.
The server always authorizes the actual selected stack, regardless of the signature supplied by the client.

Twenty focused tests pass, including four new action tests: wire round trips, every truncated packet,
trailing bytes, invalid flags/slot/face/non-finite offsets and replay fencing after a simulated hook failure.
compileJava, checkstyleMain and checkstyleTest pass; evidence is build/rts-native-use-final.log.
The runtime probe now stages real survival cobblestone placement, verifies one item consumed, replays
the same sequence and checks native interaction-hook count, then cancels a second Forge interaction
and checks that neither material nor world changes occur. These new runtime stages are NOT executed yet.
The previous Java 8 PASS must not be presented as native action acceptance. No new window was launched.

## GUI layout and dispatch continuation

Ported upstream mainline layout, rectangle/inset geometry and nested clip stack without changing
coordinates. Four upstream layout cases pass under JUnit 4, and two new tests verify panel/modal
world-input exclusion and clip-stack intersections/restoration. RtsScreenInput joins those regions
to the existing camera, cursor, cuboid and native-use endpoints. It is screen-owned, not a global
mouse handler; full production screen drawing/public entry remains unfinished.
Total focused RTS suite: 26 tests, zero failures. Compile and main/test checkstyle passed in
build/rts-gui-core-check.log. No new graphical launch; all newer runtime acceptance remains pending.

## September 24 graphical action acceptance

Normal visible Java 8 clients were launched with the user's authorization. The rendered center-ray
top-face hit and one-block selection passed in the full probe, along with ESC, late-grant cancellation,
GUI replacement, death/respawn and dimension cleanup. The death fixture now uses survival-mode lethal
void damage instead of only setting health to zero; the previous fixture timed out waiting for death.
The inspected screenshot is run/client/screenshots/rts-lifecycle-view.png.

Fresh-world native action acceptance PASSED at 12:42:08 in build/rts-actions-isolated.log:
real survival cobblestone placement, exactly one item consumed, duplicate sequence invokes the native
hook only once, and cancelled Forge right-click leaves both the second target and material unchanged.
This uses production packets/service/client state and the actual inventory stack, not direct world edits.
Run this focused mode with the existing graphical command plus -PrtsActionsOnly=true. Its PASS explicitly
means actions-only; it skips lifecycle/cursor stages and must not be reported as their combined PASS.

The combined current-source probe is NOT green. An earlier run reached native use after reconnect but
failed its acknowledgement assertion; later runs failed at reconnect, including a reproducible CoFH
OreDictionaryArbiter.registerOreDictionaryEntry NullPointerException during login/config sync.
Evidence: build/rts-actions-runtime-rerun.log, build/rts-actions-runtime-diagnostic2.log and
run/client/crash-reports/crash-2026-09-24_12.40.22-server.txt. Fresh-world success does not resolve the
post-reconnect failure. Java 17 and full GUI/physical-input/machine/multipart acceptance remain open.
## Floating-window interaction continuation

Ported upstream window geometry, scrolling and tooltip models and connected a screen-owned window
layer to RtsScreenInput. It captures presses including child content and non-left buttons, keeps
capture across frames, consumes the release of a closed window and cancels on focus/modal changes.
Rendering can use the same drawOrder snapshot that hit testing uses. No production panel renderer
or public RTS entry was added in this continuation; no new graphical acceptance is claimed.
Seven regression tests cover overlapping window order, captured dragging outside bounds, wrong-button
release, close-before-release, child-widget precedence, resize minima, large-list bounds and tooltip
fallback. Combined focused suite: 33 tests. Validation log: build/rts-windows-final.log.
# Floating-window graphical probe, 2026-09-24 12:54

Java 8 visible-client windows-only probe PASSED in build/rts-windows-runtime.log; launched with
-PrtsVisibleQa=true -PrtsWindowsOnly=true. Actual GUI size was 367x247 in a 1100x740 window.
Two diagnostic rectangles exercise production RtsScreenInput/RtsWindowLayer geometry and capture.
Assertions cover overlap ownership, out-of-screen drag clamping, capture surviving a frame,
wrong-button release, minimum corner resize, cancellation and modal world exclusion. The existing
render-matrix cursor/selection and ESC restoration assertions also passed.
Inspected screenshot: run/client/screenshots/rts-window-interaction-qa.png.
Input was scripted through the interaction layer; OS/LWJGL mouse-event dispatch and physical feel
remain unverified. The diagnostic rectangles are not a production RTS GUI or visual-parity claim.
checkstyleTest passed in build/rts-windows-runtime-check.log. Client exited after completion.
## Bottom-bar data continuation

Upstream bottom-bar state/reducer and shared grid geometry now compile on 1.7.10. Four original
state-transition tests and three GTNG inventory/grid tests bring the focused suite to 40 tests.
RtsScreenInput refreshes a read-only 36-slot inventory catalog on tick; close clears its snapshot.
Icons are defensive copies retaining NBT, equal-name slots remain distinct, variant replacement
invalidates selection, filtering clamps page bounds, and old-frame entry handles are rejected.
Selection is local UI intent only; actual held inventory and native action authorization are unchanged.
Finite-pointer guards prevent NaN from selecting cell zero. Build evidence: build/rts-bottom-final.log.
This stage supplies data and interaction state; complete panel rendering and runtime acceptance
of the inventory panel remain open. No new graphical run is claimed for this continuation.
# Manual development entry, 2026-09-24

Manual entry now exists: enableRTSBuilding=true on client/server, then use the configurable
"Open RTS development preview" binding (default G). Local run/client/config/GTNOTGOOD/gtnotgood.cfg
is enabled for this session. ESC exits, and 1-9 selects the real hotbar slot using vanilla synchronization.
WASD moves the detached camera; Space/Shift changes height; right drag rotates; middle drag pans;
wheel zooms. Pick mode uses two left clicks for a bounded cuboid; Build mode uses the actual held item.
Inventory search/paging is a read-only preview; selecting a catalog entry does not extract remote materials.

The development screen deliberately identifies itself as a preview and reuses upstream bar/grid geometry;
its original procedural panel art is not a completed upstream visual port. Remote storage, crafting and
mining remain pending. This manual preview is not a claim of full RTS acceptance.

Java 8 entry-only runtime PASS at 13:11:31: queued registered key binding -> server-authorized screen,
ESC restores original camera/input, server revocation, and reopening. Evidence: build/rts-entry-runtime.log,
run/client/screenshots/rts-manual-entry.png (inspected). The key event is programmatically queued,
so physical mouse/key feel remains user acceptance. Forty focused tests, compile/checkstyle and
assemble passed in build/rts-entry-final.log. Java 17 compatibility blockers remain open.
# Camera lateral-direction correction

User testing found reversed movement. CameraMotionSolver used (+cos(yaw), +sin(yaw)) for the
positive-D axis, but vanilla renders with Ry(yaw+180), whose right vector has both signs negated.
Corrected the lateral keyboard and pan basis; forward/backward and vertical pan remain unchanged.
Two regressions independently project movement through the vanilla view transform at all four
cardinal yaws: W/S move the viewpoint up/down, A/D left/right, and stationary terrain follows
middle-drag right/down. Build/test/checkstyle/assemble evidence: build/rts-camera-direction-fix.log.
The already running client still has the old loaded classes; restart is required for this fix.
# Bottom toolbar runtime acceptance, 2026-09-24 13:24

Final compile, 48 focused tests, main/test checkstyle and assemble also passed in
build/rts-bottom-controls-final.log. During final packaging, a newly added optional SpiceOfLife
MixinFoodModifier lacked @Pseudo and could not compile without that optional target class. Added
only the @Pseudo declaration/import, matching the existing WarpTheory pattern and preserving the
loadedMods guard. This resolves compilation; SpiceOfLife gameplay behavior was not tested here.

Manual screen now uses upstream BottomPanelBrowseLayout/BottomPanelSortLayout for both drawing and
clicking. Added clear search, upstream pager positions, quantity/mod/name ordering, ascending/descending,
22-pixel panel height adjustments and real item tooltips. Sorting retains source-slot/NBT identities.
The five upstream geometry tests plus a selection-after-sort test bring the RTS suite to 48 tests,
zero failures; compile/checkstyle passed in build/rts-bottom-controls-check.log.

Java 8 real-client PASS in build/rts-bottom-controls-runtime.log: 36 real inventory slots, search and
clear, descending quantity ordering, repeated height reduction clamped to 113 pixels, second-page
navigation, half-open button boundary rejection, no world-use packet from toolbar actions, then ESC
and reopen. The production screen and handlers are used; input is scripted rather than OS mouse input.
Screenshot run/client/screenshots/rts-bottom-controls.png was inspected. Existing manual clients were
not closed; they require restart to load these changes and the previous camera-direction correction.

# Native mining, inventory and tasks (2026-09-24)

Production additions: protocol v2 (69-byte request, 32-byte result/progress), empty-hand use,
progressive server mining using real hardness and ItemInWorldManager.tryHarvestBlock, live tool and
block identity checks, cancellation, replay fencing and lease cleanup. Mining never arms the legacy
manager's uncancellable deferred-finish flag. The ItemStack copy in RtsMiningOperation is only an
identity snapshot; it is never passed to harvesting, placement or inventory mutation.

The preview now supports real backpack-to-hotbar vanilla transactions, selection-height movement,
a bounded 4096-position sequential fill/mining queue, pause/resume/cancel, exact-NBT backpack refill
and missing-material suspension. Placement waits for both server acknowledgement and visible target
change before advancing. Errors are terminal, not auto-retried. No direct world writes, spawned
prototype tools, player teleports or inventory synthesis were introduced.

Screen acceptance: build/rts-operations-screen-recheck.log PASS (13:52), including search/sort/paging,
real source-slot 35 / hotbar exchange (36 and 1 items), ESC and reopen. Screenshot
run/client/screenshots/rts-operations.png was inspected. The first screen attempt ended before the
probe completed; it was not counted as a PASS.

Native-operation acceptance: build/rts-operations-cleanup-debug.log PASS (13:57), including native
survival placement, duplicate rejection, Forge right-click denial, empty hand, gradual mining,
cancel before harvest, real pickaxe durability, BreakEvent denial, two-position placement/mining,
backpack refill, missing-material pause, resume and cleanup. Subsequent diagnostic logging exposed
an intermittent QA interference: after CLOSE generation 1, OPEN generation 2 arrived during the
cleanup wait, so the old assertion mistakenly treated the new session as leaked authority. The
fixture now holds a non-pausing input screen during that wait. A separate bounded close-acknowledgement
outbox also strengthens production cleanup; unit tests cover lost initial closes and mismatched replies.

The migration map's latest checkpoint and rts-player-guide.md list incomplete scope explicitly.
These Java 8 checks do not establish Java 17, multiplayer or all GT/AE2/StructureLib behavior.

Final verification: build/rts-operations-verified-runtime.log PASS at 14:04:29 with the isolated
cleanup wait and production close acknowledgement. build/rts-operations-release.log passes compile,
52 focused RTS tests (zero failures), main/test checkstyle and assemble. The final source additionally
removes an arbitrary 60-second mining cap: positive-hardness operations remain bounded by the live
session and cancellation, so legitimately slow GT blocks are not cut off at a fixed duration.
Artifact: build/libs/gtnotgood-v1.1.4-main.1+e947ff5865-dirty.jar (14:05). Existing user clients were
left running and require restart to load the new code. No complete upstream-parity claim is made.
