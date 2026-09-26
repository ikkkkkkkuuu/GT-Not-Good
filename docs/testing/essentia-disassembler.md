# GT essentia disassembler

- Registered GT single-block HV machine: ID 28610, `GTNGItemList.EssentiaDisassembler`.
- Crafting: HV extractor + ME interface + Thaumcraft alchemical furnace (shapeless).
- Requires Thaumcraft and Thaumic Energistics. Connect an EU supply and a powered ME network with a free channel and essentia storage. Insert items through the native GT input slot or normal automation.
- Base duration is 20 ticks at 128 EU/t. Dynamic yields use the same central `Config.getModifiedRecipeDuration` policy as RecipeSpeedMixin, once per fresh recipe. Default fixed-duration mode produces one-tick recipes; no separate output timer throttles consecutive crafts.
- Yields match TC's `getObjectTags` + `getBonusTags`, including compound aspects. Items without aspects are not consumed. Input containers are consumed as in the TC furnace.
- A complete batch is simulated before consuming an item. Actual insertion remainders stay in the machine and block further consumption. Pending batches survive saves and wrench drops. An unfinished batch in a wrench drop restarts its processing time when placed, so removal cannot bypass EU payment.
- GT owns input inventory, energy, progress, shutdown, rotation, screen and tile lifecycle. No separate Forge block, GUI art or upstream assets are copied.

## Reproducible client checks

`./gradlew.bat -I scripts/essentia-qa.init.gradle runClient`

The disposable test mod launches a new flat world, creates a real GT machine and AE network with an essentia cell, and checks 20/2/1 tick duration modes, native yields, 32 consecutive crafts within 40 ticks, exact network totals, missing storage rejection, recovery, pending batch save/load and wrench-item NBT. It opens the native machine GUI and writes `run/client/screenshots/essentia-qa.png` and `run/client/essentia-qa-result.txt`.

On Windows systems with the JDK Unix socket temporary-directory error, set the command's `JAVA_TOOL_OPTIONS` to `-Djdk.net.unixdomain.tmpdir=C:/gtng-tmp` after creating that directory. No project Java settings need changing.
