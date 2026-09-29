# ME data access hatch (IV)

Baseline: GTNH 2.9.0-RC-1, GT 5.09.54.183, AE2 rv3-beta-1073-GTNH.
Machine ID: 28518; `GTNGItemList.MEDataAccessHatch`.

The hatch reads distinct stored GT data sticks from the attached ME network and supplies
their assembly-line recipes through `MTEHatchDataAccess`. Items remain in ME storage.
It requires one channel and ME power, connects on the front, and has no inventory GUI.
Its physical automation handler is empty. Dynamic research slots cannot be inserted into,
extracted from, or dropped when the block breaks.

Assembler recipe: EV data access hatch, ME interface, IV hull, two IV circuits,
IV sensor, IV emitter, and 576 mB molten soldering alloy; 20 seconds at IV.
The user explicitly requested moving the upstream UV machine and recipe to IV progression.

## Automated checks

Run `gradlew.bat compileJava processResources test --tests "*.MEDataStickSnapshotTest"`.
The six regression tests cover exact item/metadata filtering, empty entries, distinct NBT,
quantity and ordering changes, same-size research swaps, removals, defensive copies,
clearing/rebuilding snapshots, and more than sixteen research entries.

On this Windows workstation, set the process-local `JAVA_TOOL_OPTIONS` to
`-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp` before launching Gradle.

## In-game acceptance (not yet executed)

1. Attach the hatch to a valid assembly line; connect its front to a powered ME network
   with a free channel. Put researched assembly-line data sticks into ME storage.
2. Check recipe discovery through both ordinary and advanced assembly lines, including
   the existing packaged-provider adapter. Data-stick quantities must remain unchanged.
3. Share one network between two hatches. Both should provide the same research without
   moving items into either hatch.
4. Add, remove and replace sticks, including two different NBT values for the same item.
   The next server update should refresh recipe authorization and notify the controller.
5. Remove power/channel or split the network. No cached research should remain readable.
   Reconnect to a different network and verify only that network's sticks are visible.
6. Save/reload and unload/reload the chunk. Research becomes available only after AE is ready;
   no virtual sticks are saved as owned inventory or dropped by breaking the hatch.
7. Check the IV casing, existing ME input overlay, front-only cable connection, item tooltip
   in both languages, NEI recipe, and creative-tab entry in a running client.

Source, pinned revision, license and port changes are recorded in
`reference/UPSTREAM_PORT_NOTES.md` and the packaged `META-INF/me-data-access-port/` notices.
