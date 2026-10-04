# GTNL / Issue #1 integration checks

## Baseline

- GTNH 2.9.0-RC-1, AE2 `rv3-beta-1073-GTNH`, GT5 `5.09.54.183`.
- Java 21.0.11; published [GTNL 0.2.7-rc1](https://github.com/ABKQPO/GT-Not-Leisure/releases/tag/0.2.7-rc1).
- Development jar SHA-256: `c36ff329db26dd5df3d70e825bb2741c12fe1818c9d67d4a7b4323cd6ed6e8cc`.
- Release jar SHA-256: `00cb88f6007159acd439d3c068de6a3109452f272ea6cad08340172062241290`.

The published RC1 sources do **not** contain the processing-pattern-capacity mixins present on GTNL's newer development branch. Those development sources were an incorrect basis for attributing the reported RC1 terminal crash. The custom terminal mode remains isolated from those newer hooks, but that is not evidence of the original RC1 crash's cause.

## Confirmed compatibility failure and fix

Loading both complete mods with Angelica reproduced a fatal injection failure in GTNL's `MixinFontBatchTextEffects.gtnl$sharpMaskCoverage`: `(0/3) succeeded`. GTNG's port had already redirected the same field accesses.

GTNG now detects the upstream renderer by class resource during early mixin discovery, without loading client classes. When present, GTNG skips its duplicate text-effect mixins, NEI text-field hook and local render lifecycle/preview command. It retains local machine-credit preferences; GTNL renders the shared inline effect syntax. Older GTNL versions without this renderer and installations without GTNL keep the local implementation.

## Checks

The dedicated-server test used a copy of the installed RC1 server's `mods`, `config`, `libraries` and `scripts`, plus the GTNL release jar and a separately reobfuscated GTNG QA jar. It used a new flat world and `127.0.0.1:25585`; the user's server and saves were not modified.

The full server loaded 301 mods and passed:

- Registration through postInit with native-class input and output hatches rated at `214748364 A`, inserted before recipe registration with easy recipes enabled.
- Terminal crafting/processing mode switching and synchronization.
- Item slots 31/32 and native fluid slot 255 surviving close/reopen.
- Native fluid pattern encoding and clearing hidden slots.

Report: `build/gtnl-compat-qa/full-server.txt`; log: `build/gtnl-full-server.log`.

The client harness loads the full GTNL development jar, its dependencies and Angelica, rather than mocked GTNL classes. It checks:

- Startup with the unsupported-amperage fixtures.
- Terminal packet round trips, 256-slot persistence, encoding and clear.
- Actual NEI import/encoding of 16 item inputs and 4 native fluid inputs, with exact quantities and fluid priority.
- Independent input/output scrolling, visible-slot bounds and interface viewport reads.
- GTNL ownership of the Angelica font bridge, absence of the duplicate GTNG bridge, and measuring/drawing a GTNG inline shader credit.

Reports: `build/gtnl-compat-qa/result.txt`, `build/gtnl-compat-qa/nei-result.txt`; log: `build/gtnl-compat-final.log`; screenshots: `build/gtnl-compat-qa/screenshots/`.

The same terminal harness also passed without GTNL (`build/gtnl-standalone-regression.log`). The three focused wireless-rating tests pass using `scripts/issue-1-qa.init.gradle`.

This covers the reported integration paths, not every GTNL machine or recipe. The full pack still logs unrelated recipe/resource warnings, including a caught `GT_CraftingRecipeLoader` index-5 exception; passing these checks does not mean every pack log entry is error-free.

## Reproduce

On this Windows host, Gradle's Unix-domain socket setup requires a short temporary directory:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp -Djava.io.tmpdir=C:/Windows/Temp'
$env:TEMP='C:/Windows/Temp'
$env:TMP='C:/Windows/Temp'
.\gradlew.bat -I scripts/gtnl-compat-qa.init.gradle runClient17 --no-configuration-cache
.\gradlew.bat -I scripts/terminal-scroll-qa.init.gradle runClient --no-configuration-cache
.\gradlew.bat -I scripts/issue-1-qa.init.gradle compileJava test --no-configuration-cache
```

Despite its task name, the GTNL profile selects Java 21. It downloads and verifies the pinned GTNL development jar. Run these profiles sequentially: Gradle can clean previous compiler outputs when switching build directories. Each client uses the scoped hidden-window helper.

Build the dedicated-server QA artifact with:

```powershell
.\gradlew.bat -I scripts/gtnl-server-qa.init.gradle reobfJar --no-configuration-cache
```

Use the **reobfuscated** `gtnotgood-<version>.jar` in `.gradle/gtnl-server-qa-build/libs/`, not the intermediate `gtnotgood-gtnl-server-qa.jar`. Replace GTNG only inside the copied server's mods directory, add the pinned GTNL release jar, and launch the copied server's normal Java 21 command with `-Dgtng.gtnlCompat.qa=true` and `-Dgtng.gtnlCompat.report=<absolute-report-path>`. The fixture writes its PASS report and stops the dedicated server after successful checks. This QA jar contains a test mod and must not be distributed as a release.
