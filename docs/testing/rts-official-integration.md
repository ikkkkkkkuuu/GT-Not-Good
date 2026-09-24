# Official RTS Building integration — 2026-09-24

The active implementation is Hcrab/RTSbuilding `forge-1.7.10`, pinned to
`c8bdff25ea9aa692c641fee231671afe58057a39` (Alpha 0.0.1).
The prior GTNG development preview remains available as reference code, not the normal entry point.

## Included

- The official main, uiCore and uiKit Java sources, original UI resources, language files and data.
- GTNG-owned lifecycle (including durable-task flush before world unload), item registration and creative tab.
- Separate `gtnotgood_rts` packet channel; no additional standalone `@Mod` entry.
- Official early vanilla/Forge mixins relocated into the existing host configuration and refmap.
- AE2 rv3-beta-1050-GTNH adaptation: ForgeDirection, BaseActionSource/PlayerSource,
  getItemInventory, createItemStack, getItemStack; successful full insertion handles null remainder.
- Existing GTNHLib 0.11.46 retained. The upstream forced 0.7.10 dependency was not imported.
- Upstream JEI/Refined Storage implementations remain excluded as in the official branch.

## Use

Set `RTS_Building.enableRTSBuilding=true` in `config/GTNOTGOOD/gtnotgood.cfg`
and restart both game and server. The local test configuration already enables it.
Press **G** to use the official interface. Use **G / Esc** to exit.
The old development-preview binding is unassigned by default.
Detailed official configuration lives under `config/rts_building/`.
Creative mode provides immediate access; survival follows the official control-core/plugin progression.
Do not install a standalone RTS Building jar alongside this embedded copy: the upstream Java namespace is retained.

## Verified

- `compileJava`, `processResources`, `checkstyleMain`, `assemble`: passed.
- Scoped RTS tests: 57 passed across 14 suites, including five official-packet regression cases.
- Java 8 real client with Angelica: passed at 14:34:53 on 2026-09-24.
  Official AE2 bridge API resolution, Chinese translation, world creation, camera enable,
  empty-tool mining start/abort, empty-hand interaction, red-wool metadata placement,
  official UI rendering, camera disable, integrated-server shutdown.
- Screenshot: `run/client/screenshots/rts-official-integrated.png`.
- Runtime evidence: `build/rts-official-client.log`.
- Build/test evidence: `build/rts-official-verify.log`.
- Java 17 was attempted again (`build/rts-official-java17.log`) and failed during
  dependency initialization, before the RTS world probe: the existing CropsNH
  shaded-MCLib failure and missing Railcraft `IItemFirestoneBurning` surfaced.
  Java 17 full-pack acceptance is therefore not passed.

The first harness build omitted the host JAR manifest; that run is not a successful
mixin-enabled integration check. The passing run includes the normal host manifest.
The upstream smoke test also needed to accept the pack's custom main menu.

## Further acceptance scope

This imports the complete official branch's enabled implementation, not a claim that
all inherited Alpha features have been accepted. Blueprint capture/replay, persistence
recovery, linked AE2 networks under load, remote GT/ModularUI machines, multiplayer,
and NEI recipe transfer still need dedicated end-to-end coverage. The smoke test
checks AE2 API resolution, not a powered storage network's complete behavior.

## Area-action crash fix — 2026-09-24

The user's 14:43:34 server crash exposed an upstream 1.7.10 persistence mismatch:
destruction and mining snapshots store packed long coordinates as int-array pairs,
but their decoders required NBT lists. Placement progress had the same mismatch;
its job definition instead really uses a native list of longs. Decoders now validate
the actual encoding, and the shared reader supports both representations while
rejecting odd-length int arrays and lists with non-long elements.

Five new codec tests cover destruction progress, placement definition/progress,
mining targets, and invalid encodings. Remote menu tickets now use the registered
GTNG mod instance instead of the removed standalone RTS mod container.
The first real-client regression passed durable area destruction of the newly
placed red wool (`build/rts-area-destroy-client.log`). No user save was edited.
The final real-client run passed at 14:52:20: red wool placement, durable area
destruction, durable batch reconstruction with metadata 14, and camera close
(`build/rts-batch-actions-client.log`). The process exited successfully, but the
log ends with an incomplete Client Shutdown Thread exception line; this run does
not establish clean shutdown acceptance. The complete scoped
RTS suite now has 62 passing tests across 15 suites; checkstyle and packaging pass.

## Import records

See `META-INF/rts-port/OFFICIAL_IMPORT_MANIFEST.json` and bundled upstream notices.
The checked-out reference never participates directly in compilation or packaging.
`scripts/update-rts-import-manifest.ps1` regenerates import hashes from the pinned reference.

## RTS Q quick-drop crash fix — 2026-09-24
The 14:57:31 crash was inside the RTS quick-drop request, not vanilla item dropping. Nullable results from linked storage, selected/other hotbar slots and fallback extraction now contribute zero to the extracted count. Empty sources are a no-op; available stacks retain extraction limits and metadata. The focused suite passes 67 tests across 16 suites, and checkstyle plus assemble pass. The real-client probe uses the same quick-drop packet as Q while RTS is active, with no diamond sources; it then verifies empty-tool mining, pinned red-wool placement, durable destruction and batch rebuilding. Log: build/rts-q-fix-client.log. This is packet-route coverage, not a physical keyboard-input test.
The Q probe and building/destruction probes passed. The overall client run failed at RTS disable acknowledgement: the camera was disabled at 15:03:52 then another toggle re-enabled it at 15:03:54. The source of that second toggle was not established. Do not count this run as full lifecycle acceptance.

## GT wire/cable native placement — 2026-09-24
Quick build previously bypassed ItemMachines.placeBlockAt and initialized only the block state, losing GT MetaTileEntity subtype initialization. It now invokes the installed GT native placement hook, retaining prototype damage/NBT and clicked face. Generic onBlockPlacedBy/BlockEntityTag processing is skipped after this native hook to avoid double initialization. Existing access, claim, collision and material/refund checks remain in place.
67 scoped tests and checkstyle passed (build/rts-gt-placement-tests.log); assemble passed. Real-client runs passed with copper wire subtype 1360 and cable subtype 1366. The follow-up run also placed two assembly-line controllers via separate pinned-item interactions (subtype 1170), rechecked all four client tile identities for 60 frames, then closed RTS successfully. Report markers: GT_CONSECUTIVE_PLACEMENTS_STABLE and PASS; log: build/rts-gt-consecutive-client.log. This validates client subtype synchronization and consecutive placement, not every GT machine or survival network source. Previously malformed blocks are not automatically migrated.

## Distant interactions invalidating neighboring GT machines — 2026-09-24
The earlier native-placement fix was necessary for quick build but did not address distant pinned-item interactions. RtsRemoteMenuChunkLease sent a complete S21 chunk packet beyond eight blocks, followed only by the clicked tile description. Existing tiles elsewhere in that client chunk lost their synchronized subtype. The regression now places two controllers in the same chunk beyond the lease threshold and rechecks earlier tiles.
Before the sync fix, the real client failed with Earlier GT placement lost subtype: 2 (build/rts-remote-repro-client.log; build/reports/rts-official/remote-before-fix.txt). After sending every live chunk tile's native description after S21, the identical test passed, including GT_CONSECUTIVE_PLACEMENTS_STABLE and RTS close (build/rts-remote-fixed-client.log; remote-after-fix.txt). 67 scoped tests, checkstyle, assemble and diff checks pass. The client log still ends with an incomplete Client Shutdown Thread exception line, so clean shutdown remains outside acceptance. This specific failure is client-side data loss; reconnecting restores descriptions from the server. It does not imply every purple block from the separate old quick-build bug has valid server data.

## Missing placement prototype crash — 2026-09-24
The 15:29:31 server crash dereferenced PlaceBatchJob.itemPrototype when the optional stack was null. The getter now uses StackCompat.copyOrNull, preserving empty-hand/current-tool semantics and defensive copies. Two regressions cover absent prototypes through restore/resave and unreadable stack NBT. All 69 scoped tests pass, as do checkstyle and assemble. The real-client empty-hand placement request enters the durable-task fallback with a null prototype; NULL_PROTOTYPE_DURABLE_PLACE_OK and the final PASS confirm continued execution. Distant consecutive GT controller subtype checks also pass (build/rts-null-prototype-client.log). This does not establish complete hologram-projector behavior; the supplied log shows that preceding tool interaction returned success. The known incomplete shutdown-thread exception line remains outside acceptance.

## Native GT rotation and destruction history — 2026-09-24
GT machines now expose only legal rotation arcs through their native facing rules. The server uses setFrontFacing, marks the existing tile dirty and retains existing mode/progression/range/claim checks. The real-client probe explicitly enters ROTATE mode, checks handles, performs four quarter-turn network requests, verifies synchronized facing and unchanged client tile identity, then destroys the wire, cable and both controllers through area destruction. GT_ROTATION_FOUR_TURNS_OK, GT_AREA_DESTRUCTION_HISTORY_OK and final PASS are recorded in build/rts-rotation-history-final-client.log.
Destruction history now stores the exact native registry name and metadata. All 1183 mixed-case registry entries in this pack passed state round-trip validation. Missing history blocks are logged/skipped without aborting server ticks; the added regression verifies that path. The exact block from the user's failing saved history was not identified, so the serialization defects and fatal error handling are both addressed rather than claiming one confirmed offending mod. 70 scoped tests, checkstyle, assembly and diff checks pass. The first rotation probe omitted ROTATE mode and correctly timed out; only the corrected run counts as rotation acceptance. The known incomplete shutdown-thread exception line remains outside acceptance.

## Range arrow picking — 2026-09-24
World cursor rays now unproject the actual rendered frame matrices instead of estimating from raw FOV and entity eye coordinates. The six box handles use symmetric 0.12-block pick padding. Real-client projection/picking checks passed for all six arrow heads (SIX_HANDLE_FRAMEBUFFER_PICKING_OK), alongside rotation, consecutive GT placement and area destruction; final PASS in build/rts-handle-client.log. 70 scoped tests, checkstyle and assembly passed. This verifies geometry alignment at the tested camera/viewport, not exhaustive manual usability at every zoom or GUI scale.
Rotation arcs share the corrected rendered-frame cursor ray. Their hit radius is expanded from 0.15 to 0.24 blocks; nearest-hit selection remains unchanged. The final padding adjustment was build/regression checked, without a separate manual rotation-arc usability test.

## Linked AE mining-drop insertion — 2026-09-24
The supplied 16:19:26 crash follows successful storage linking and area destruction. Aggregate change tracking dereferenced the null remainder returned after complete acceptance. StackCompat.count now handles that remainder; an analogous player-transfer count was corrected. Five insertion tests cover full acceptance, partial acceptance, refusal, simulation, and slot-independent network insertion. The 75 scoped tests and packaging pass. The network test uses the AE-style AnySlotInsertItemHandler contract, not a live powered AE network; no new full AE network end-to-end claim is made.

## Linked network browser refresh — 2026-09-24
Fixed the permission wrapper dropping the refreshable-snapshot contract. Two regression cases exercise RtsHandlerCache.update with an initially empty network snapshot, additions, quantity changes, removal, and extract-only insertion refusal. They use a contract fixture, not a live AE network or full browser synchronization. All 77 scoped RTS tests and checkstyle passed. Real powered AE browser display remains to be verified in-game.

## Built-in unlocked RTS harvesting — 2026-09-24
Real Java 8 client report build/reports/rts-official/runClient-smoke.txt passed TOOLLESS_STONE_DIAMOND_GT_DROPS_BOTH_MODES_OK and final PASS. The server-thread fixture checks exactly one cobblestone, diamond, GT wire/cable/controller item per corresponding block in survival and creative, with empty hand, and rejects bedrock. Existing actual RTS packet tests also pass GT batch destruction and four rotations. An initial probe counted dead entities left in the world list; excluding isDead fixes the fixture rather than production drops. The new policy tests cover legacy progression settings and tool selection. These probes do not claim every modded block's custom drop behavior has been tested.

## Top-bar PNG recovery — 2026-09-24
All 52 bundled PNGs pass ImageIO decoding; malformed and missing input are converted to IOException. Java 8 real-client probe loaded all 44 catalog button/state textures and exercised bundled fallback with an intentionally failing resource manager (TOPBAR_ALL_STATES_AND_BUNDLED_FALLBACK_OK), then completed existing gameplay checks. This does not reproduce the user's modern-Java resource source; the supplied stack trace does not identify the resource pack providing the undecodable bytes.

The subsequent combined run reached the top-bar PASS marker but failed an existing creative pinned-wool placement check before the inventory stage. It is not counted as a full gameplay pass. Inventory button removal is being verified with a dedicated disposable-world visual path, independently of placement.

Dedicated inventory visual run passed (build/rts-inventory-only-client.log). Inspected run/client/screenshots/rts-inventory-no-button.png: the survival inventory upper-right area has no RTS button. All top-bar texture states and bundled fallback also passed in that run. Final assembly and 82 scoped RTS tests/checkstyle passed; no claim of a second complete gameplay run after the unrelated placement failure.

## Legacy preview removal — 2026-09-24
Compile, checkstyle, assembly and all 30 remaining official RTS regression tests passed. The former 82 total included 52 preview-specific tests, which were removed with that implementation. Final JAR inspection found zero old client/rts, common/rts or legacy RTS packet class entries and 2244 official namespace entries. Main-resource translations are regenerated from the remaining Java source.

Post-removal real-client gameplay regression passed in build/rts-cleanup-client.log: RTS enter/exit, native GT placement/rotation, area destruction, top-bar loading/fallback and empty-hand native drops in both modes. Final report PASS worldTicks>=140 rtsRenderTicks=60.
