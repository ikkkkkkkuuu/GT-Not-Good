# ME stock controls — Forge 1.7.10 / GTNH adaptation

Behavior references inspected before implementation:

- GlodBlock/ExtendedAE, `76691dbd5636c16ba05684f4b8d1cd98adc923aa` (26.1.2-neoforge),
  LGPL-3.0, `LICENSE.txt`, `PartThresholdExportBus`, `PartThresholdLevelEmitter`, their menus/screens and part models.
- AlmostReliable/merequester, `95fbd4b6bfded008c65425bc0c52aec4f07b3ede` (26.1),
  LGPL-3.0, `LICENSE`, `Request`, `RequestManager`, `RequesterBlockEntity`, requester/terminal screens and block models.
- GTNewHorizons/Applied-Energistics-2-Unofficial `rv3-beta-1073-GTNH`,
  source artifact SHA-1 `12f114eb4e27c346d0278fcff7a09b5a3067b86`, LGPL-3.0-or-later source headers,
  MIT API headers. Inspected export bus, level emitter, exact storage queries, watchers, grid caches and crafting APIs.
- AppliedEnergistics/Applied-Energistics-2, tag `v26.1.12-beta`,
  `b7cf5822d9c128a61d9291cb2c1f92319253e4f0`. Inspected `LICENSE`, README asset license,
  io-bus/set-stock-amount screen styles, upgrade panels, number entry, toolbar and scrollbar widgets.
  Source: LGPL-3.0-or-later; API: MIT; textures: CC BY-NC-SA 3.0.

Destinations: `com/xyp/gtnotgood/common/mestock/`. Threshold semantics are adapted under LGPL-3.0-only.
The requester scheduler, registration and persistence are newly authored for GTNH.
`StockGui`, `StockGuiAssets` and `StockNumbers` adapt the pinned screen/widget behavior under LGPL-3.0-only,
replacing incompatible client/menu APIs with ModularUI2 while preserving original pixel layouts and artwork.
Modern AEKey/capability/value-storage interfaces are replaced with native item/fluid AE stacks and Forge sided access.

Differences from modern upstream:

- Exact item/fluid identities only; no wildcard/fuzzy/crafting-card/energy/chemical modes.
- 9 threshold filters, expanded to 63 by three capacity cards; eight upgrade slots with native GTNH speed cards.
- Five requester rows, optional maximum batch size (0 automatically fills the deficit), standalone native CPU jobs
  returning products to ME storage. Submitted jobs survive removal/pausing; pending calculations are canceled.
- One bounded scheduler per network: eight device visits/tick, two simultaneous calculations, one new calculation
  per ten ticks; CPU outputs indexed at most once per twenty ticks. No per-device full inventory or CPU scans.
- Exact storage watchers, coalesced wakeups and delayed retries. Dormant emitters and threshold buses sleep.
- Threshold bus: original 176x253 io-bus frame, upgrade strip and toolbar; middle-click opens the original
  176x107 amount sub-screen, step buttons, Enter/Set submission and back tab.
- Emitter: original 176x186 frame, two numeric fields, filter and signal-mode icon.
- Requester: original 195-pixel stitched frame, state boxes, two fields, submit buttons and status strips.
  Request submission carries both fields together; numbers support exact long storage quantities.
- User-requested GTNH unit adaptation: fluid fields/overlays use L, matching the host Advanced IO bus.
  Native fluid amounts are displayed, edited and stepped directly; modern bucket scaling is removed.
- Terminal: original grouped inline list, right-click-cleared search, wheel/drag scrollbar and four adaptive
  height modes. Synchronization covers visible rows (at most 64); directory rebuilds occur only on changes.
  Remote editing validates distance, live row/device identity, loaded network membership and BUILD permission.
- Native GTNH cable/block geometry. All PNGs in the manifest are byte-identical copies of pinned upstream
  assets. The submit hover uses the correct right-half crop of the original 24x12 image.
  ExtendedAE artwork is credited to Sea_Kerman; requester artwork/model to Almost Reliable.
  AE2 textures/models: (c) 2020 Ridanisaurus Rid, (c) 2013-2020 AlgorithmX2 et al,
  https://creativecommons.org/licenses/by-nc-sa/3.0/ . Noncommercial distribution only for these assets;
  adaptations retain the same asset license. Original notices/license evidence are retained alongside this file.

The inspection checkouts live under the operating-system temporary directory. Extracted GTNH source is under
`build/aestock-reference`. Neither is a compilation/resource input.

These LGPL sources/assets and CC BY-NC-SA assets are not covered by the project's general MIT grant.
See `ASSET_MANIFEST.json` for source paths, exact revisions, destinations and SHA-256 hashes.
