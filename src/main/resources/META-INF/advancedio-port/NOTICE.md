# AdvancedAE Advanced IO Bus port

Upstream: https://github.com/pedroksl/AdvancedAE
Pinned commit: `5ad43ee1e5f7a8b9fe7a1eacfaebdd44b61b624c` (inspected 2026-09-22).
Original authors: pedroksl and AdvancedAE contributors.
Repository license: LGPL-3.0 (`LICENSE.md`). No separate license for the copied textures was found
in the inspected checkout. This is a repository-wide grant interpretation, not an asset-specific statement.
The AdvancedAE-derived classes and its textures retain LGPL-3.0; the GUI adaptation also incorporates GPL-3.0 AE2AddonLib layout and is GPL-3.0-only. All are excluded from GTNG's general MIT grant.
The LGPL and its incorporated GPL text accompany this notice. Distributions must provide the corresponding
source for the adapted code and preserve these notices, not only the binary jar.

Behavior references:
- `src/main/java/net/pedroksl/advanced_ae/common/parts/AdvancedIOBusPart.java`
- `src/main/java/net/pedroksl/advanced_ae/common/parts/StockExportBusPart.java`
- `src/main/java/net/pedroksl/advanced_ae/common/helpers/FilteredImportStackTransferContext.java`
- `src/main/java/net/pedroksl/advanced_ae/gui/StockExportBusMenu.java`
- `src/main/java/net/pedroksl/advanced_ae/client/gui/AdvancedIOBusScreen.java`
- `src/generated/resources/assets/advanced_ae/models/part/advanced_io_bus_part.json`

Destinations: `com.xyp.gtnotgood.common.advancedio.PartAdvancedIOBus`, `AdvancedIOGui`, and
`ItemAdvancedIOBus`. These are Minecraft 1.7.10 rewrites/adaptations, not unchanged modern classes.
Retained behavior: exact item/fluid targets, optional excess regulation (default on), import of unlisted
products, 18 base configuration slots plus 9 per capacity card (maximum 63), eight upgrade slots,
and 8x transfer budget. Remainders smaller than one fluid operation are transferred exactly.

GTNH adaptations:
- Native AE item/fluid inventories, sided inventories and Forge IFluidHandler replace modern transfer strategies.
- Supports GTNH standard, super and superluminal acceleration (maximum four each), capacity (five)
  and redstone (one), sharing eight upgrade slots. Native PartExportBus computes the additive acceleration
  tiers; this bus applies the upstream eightfold multiplier to that budget.
  Crafting, fuzzy, inverter, chemical strategies and pulse mode are not advertised/supported by this port.
- Default, round-robin and random export scheduling; disabled capacity rows stay protected from unlisted-product import.
- Unexpected remainders are persisted until ME can accept them, and returned as drops when broken.
- The GUI preserves the original 176x253 layout, external upgrade strip, icon toolbar and 176x107
  middle-click quantity sub-screen. MUI2 replaces incompatible APIs; no GTNG theme substitution.
- A local recipe uses four import buses, two export buses, two calculation processors and one engineering processor.
- Front/back/side textures are unchanged upstream PNG files; their mappings and hashes are in ASSET_MANIFEST.json.
  GTNH's export-bus rendering geometry is inherited/adapted from the installed AE2 implementation.

AE2 API/renderer reference: GTNewHorizons/Applied-Energistics-2-Unofficial,
`rv3-beta-1050-GTNH` sources JAR. `PartBaseExportBus` carries AlgorithmX2's 2013-2014 LGPL-3.0-or-later
notice; retain that attribution for the equivalent cuboid renderer adaptation in `PartAdvancedIOBus`.
Reference checkouts and extracted dependency sources stay outside compiled and packaged source sets.

## GUI dependency references and visual licenses

Applied Energistics 2 v26.1.10-beta, commit `3a051bb473de0b8fd329b39db4262f731d17e7e5`:
https://github.com/AppliedEnergistics/Applied-Energistics-2
Inspected `README.md` and `LICENSE`: code LGPL-3.0-or-later; textures/models CC BY-NC-SA 3.0.
Original authors: AlgorithmX2, TeamAppliedEnergistics and contributors; artwork also Ridanisaurus Rid.
Source layout: `src/main/resources/assets/ae2/screens/io_bus.json`, `common/common.json`,
`common/player_inventory.json`; `src/client/java/appeng/client/gui/widgets/{UpgradesPanel,VerticalButtonBar,IconButton,NumberEntryWidget,TabButton}.java`.
Destination: `AdvancedIOGui.java` and `assets/gtnotgood/textures/gui/advancedio/`.
All copied PNGs are unchanged; atlas crops, nine-slice rendering and server synchronization are adapted for 1.7.10.
The visible optional crafting button remains hidden because this port does not register crafting upgrades.
Redstone pulse, creative hotkeys and modern expression entry are not claimed as fully reproduced interactions.

AE2AddonLib 26.1.3-alpha-neoforge, commit `5b48a86deea7ebf50bf95166f7b74a24057c90ac`:
https://github.com/pedroksl/AE2AddonLib
Inspected root LICENSE (GPL-3.0; full copy included),
`src/main/resources/assets/ae2/screens/set_amount.json`,
`src/main/java/net/pedroksl/ae2addonlib/client/screens/SetAmountScreen.java`.
Original author: pedroksl and contributors. Destination: AdvancedIOGui.amountPanel.
Port modifications: MUI2 sync values, guarded server-side submenu switching and exact native-fluid conversion;
upstream page dimensions, coordinates, plus/minus buttons and confirm/back semantics preserved.
This GUI source is GPL-3.0-only; it is not under the project's general MIT grant.

Reference checkouts `reference/AdvancedAE`, `reference/AE2AddonLib`, and
`reference/dependencies/ae2-modern-io` are ignored and never compiled or packaged.
