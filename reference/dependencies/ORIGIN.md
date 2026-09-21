# AE2 GUI reference

Official source: https://github.com/AppliedEnergistics/Applied-Energistics-2

Revision: 79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a (neoforge/v19.2.17).

`LICENSE` and `README.md` are unmodified upstream root files.
`Icon.java`, `UpgradesPanel.java`, and `PatternProviderScreen.java` are unmodified excerpts from the respective `src/main/java/appeng/client/gui/` source paths (widgets and implementations subdirectories for the latter two). They retain upstream copyright/license headers and are not GTNG source files.

`common.json` and `player_inventory.json` were extracted unchanged from `assets/ae2/screens/common/` in the dependency JAR.

JAR download: https://www.cursemaven.com/curse/maven/applied-energistics-2-223794/7027323/applied-energistics-2-223794-7027323.jar

SHA-256: 460d779a0609b81409907d9956de8f6f70a1b0912257e3e5c3c7e75ac9630e95.

The JAR is only an inspection reference and is excluded from source control and the GTNG build. Production copies are listed separately in META-INF/ae2lt-port/ASSET_MANIFEST.json.

`PriorityScreen.java` and `NumberEntryWidget.java` are unchanged source references
from `src/main/java/appeng/client/gui/implementations/` and `widgets/`, respectively,
at the same pinned revision. The priority-button PNG metadata specifies a
nine-slice border of three pixels; the port implements this using MUI slicing.

`LockCraftingMode.java`, `SettingToggleButton.java` and `PatternProviderLogic.java`
are unchanged references from the same AE2 commit, respectively under
`src/main/java/appeng/api/config/`, `client/gui/widgets/` and `helpers/patternprovider/`.
They establish the five lock modes, icon coordinates, result-return accounting and
redstone edge semantics. GTNH's installed rv3-beta-1050-GTNH source JAR was inspected
for `IInterfaceViewable` and `IInterfaceTerminalRegistry`; the port calls these public APIs.
