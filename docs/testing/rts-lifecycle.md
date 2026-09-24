# RTS Phase 2 lifecycle verification

Status on 2026-09-24: foundation implemented; runtime acceptance is **not complete**.
This does not deliver the camera controller, production GUI or building actions.

## Window preference

The user requires graphical clients on Desktop 2 without exposing them on the current taskbar.
Do not automatically start a visible client on the current desktop. The QA init script refuses
client launch unless `-PrtsVisibleQa=true` is explicitly supplied after the desktop is arranged.
That flag is an acknowledgement, **not** an implementation of desktop movement or hidden windows.
There is no hidden-window startup option in the inspected lwjgl3ify Display implementation.
The current window tool does not expose virtual-desktop movement. Graphical reruns remain deferred.
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
Release jars do not contain the probe screen, camera or QA mod.

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
