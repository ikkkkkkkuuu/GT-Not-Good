# GT essentia disassembler

- Registered GT single-block HV machine: ID 28610, `GTNGItemList.EssentiaDisassembler`.
- Crafting: HV extractor + ME interface + Thaumcraft alchemical furnace (shapeless).
- Requires Thaumcraft and Thaumic Energistics. Connect an EU supply and a powered ME network with a free channel and essentia storage. Insert items through the native GT input slot or normal automation.
- Base duration is 20 ticks at 128 EU/t. Dynamic yields use the same central `Config.getModifiedRecipeDuration` policy as RecipeSpeedMixin, once per fresh recipe. Default fixed-duration mode produces one-tick recipes; no separate output timer throttles consecutive crafts.
- Yields match TC's `getObjectTags` + `getBonusTags`, including compound aspects. Items without aspects are not consumed. Input containers are consumed as in the TC furnace.
- Each aspect is simulated before consuming an item. These simulations do not reserve aggregate capacity; actual insertion remainders stay in the machine and block further consumption. Pending batches survive saves and wrench drops. An unfinished batch in a wrench drop restarts its processing time when placed, so removal cannot bypass EU payment.
- Completely rejected output retries after 1, 2, 4, 8, 16 and then 20 ticks. Accepted output resets the delay; normal completion still flushes immediately. Only actual batch changes mark the tile dirty, once per attempt.
- A per-machine cache compares item, metadata and NBT, ignores stack size, returns copies and expires after 20 ticks. Idle recipe retries notify GT through `markInventoryBeenModified()` without rewriting the input stack.
- The native ModularUI2 screen shows machine/network status and aspect quantities, preferring the pending batch over an input preview. GT owns inventory, energy, processing and tile lifecycle. No upstream GUI assets are copied.

## Reproducible client checks

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
./gradlew.bat -I scripts/essentia-qa.init.gradle spotlessApply compileJava processResources compileTestJava runClient17
```

The init script uses Java 21 and isolates compilation in `.gradle/essentia-qa-build`, the client in `run/essentia-qa`, and evidence in `build/essentia-qa`. It needs the existing pack-pinned `build/factory-qa-dependencies/etfuturum-2.6.59-GTNH-dev.jar`. On Windows it starts `scripts/qa/HideFactoryClient.ps1` with the isolated run directory, matched by an explicit JVM property. Only this marked QA process becomes transparent, click-through and hidden from the taskbar.

The disposable test mod creates a new flat world, a GT machine and a real powered AE network. Its checks cover:

- Native yields and 20/2/1 tick duration settings; 32 consecutive crafts within 40 ticks with exact output totals.
- Missing-storage rejection without consuming input.
- A real 64k essentia cell filled to capacity, exact retry delays and zero dirty calls while all output is rejected.
- Room for only three Aer, partial insertion, exactly one dirty call, and the remaining seven Ignis retained in save/load and wrench-item NBT.
- Replacement storage accepting the saved remainder exactly once, followed by real network disconnection and reconnection with pending Aqua and queued input.
- A fixture using `IEssentiaContainerItem` to check cache hits, metadata/NBT invalidation, returned-copy isolation and expiry.
- Input-preview, pending-output and normal-idle GUI states.

Success writes `build/essentia-qa/result.txt` containing `PASS`. Screenshots are `build/essentia-qa/screenshots/essentia-input-preview.png`, `essentia-pending.png` and `essentia-normal.png`. Inspect `ESSENTIA_QA` in `run/essentia-qa/logs/fml-client-latest.log` and the window helper log in `build/essentia-qa/window-helper.log`.

The save/drop checks round-trip NBT on the live tile; they do not substitute for a complete process restart or actual wrench removal/replacement test. The runner exits only its disposable QA process after writing results.
