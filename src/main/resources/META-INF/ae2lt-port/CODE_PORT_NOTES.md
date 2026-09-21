# GTNG runtime port, 2026-09-21

The Java packages `com.xyp.gtnotgood.common.packaged` and
`com.xyp.gtnotgood.client.packaged` are provided under LGPL-3.0-only, including
GTNG modifications. They are excluded from the project's general MIT grant.
Copyright 2026 GTNG contributors. Upstream authors, licenses and full pinned
revisions are retained in NOTICE.md and the accompanying license files.

All entries below are modified: implementations were rewritten for Minecraft
1.7.10, GTNH AE2 and ModularUI2; no high-version class is compiled unchanged.
Source paths are relative to each upstream Java package. Source revisions:

- PP: AE2LT Packaged Pattern Provider 1.2.0-beta.1,
  `1d3f183ecf258aff0001567d742ed30d6038d77b`, LGPL-3.0.
- LT: AE2 Lightning Tech 2.1.0-beta.5,
  `1d4589b6bd50672051f78d766505530beacfebc0`, LGPL-3.0.
- AE: Applied Energistics 2 19.2.17,
  `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`, LGPL-3.0.

Local paths below are relative to `src/main/java/com/xyp/gtnotgood/`.

| Local file | Upstream reference | Modification |
| --- | --- | --- |
| client/packaged/WirelessConnectorRenderer.java; common/packaged/TilePackagedProvider.java overlay snapshot | LT client/WirelessConnectorRenderer.java, WirelessConnectorRenderFilter.java, Ae2ltRenderTypes.java; AE client/render/overlay/OverlayRenderType.java | Original colors, cube/face geometry, 3px face-center lines, selected-host filter and preview; Forge RenderWorldLastEvent/Tessellator adaptation; bounded S35 coordinate-only snapshots; 4-tick loaded-host scan; direct binding suppresses duplicate blocks |
| common/packaged/PackagedProviderGui.java | PP client/PackagedPatternProviderScreen.java, menu/PackagedPatternProviderMenu.java, client/PackagedProviderTextureButton.java; LT client/gui/FrequencyScreen.java; AE client/gui/implementations/PatternProviderScreen.java, PriorityScreen.java, client/gui/widgets/UpgradesPanel.java, NumberEntryWidget.java | Original geometry/art adapted to MUI; bound-target subset of frequency UI; server synchronization |
| client/packaged/PackagedCoreRenderer.java | PP client/PackagedCoreItemRenderer.java | Forge 1.7.10 rendering; installed TC matrix or GT5U assembly controller overlay |
| common/packaged/ItemPackagedCore.java, PackagedCoreRegistry.java | PP item/PackagedCoreDefinition.java, logic/multiblock/MultiblockAdapter.java | Public 1.7.10 adapter API and optional TC core |
| common/packaged/TilePackagedProvider.java | PP logic/PackagedPatternProviderLogic.java, patternprovider/StablePatternProviderLogic.java | AE crafting medium; bounded round robin/backoff; persistent receipts and exact output recovery |
| common/packaged/BlockPackagedProvider.java | PP models/block/wireless_packaged_pattern_provider.json and blockstates/wireless_packaged_pattern_provider.json (resource paths) | Cube-all model expressed as 1.7.10 block icons |
| common/packaged/ItemWirelessConnector.java, MessagePackagedConnector.java, PackagedTarget.java | LT item/OverloadedWirelessConnectorItem.java, network/WirelessConnectorUsePacket.java, logic/wireless/support/WirelessConnection*.java | Explicit normal-select / Shift-bind contract; integer dimension schema; same-dimension targets; server reach/permission checks |
| common/packaged/PackagedServerActions.java | New GTNG support | Bounded server action queue |
| common/packaged/ThaumcraftInfusionAdapter.java, AltarStatus.java | New GTNG TC4 integration | Calls installed TC4 recipe/matrix APIs; no TC4 code or artwork copied |
| common/packaged/PackagedCraftingLock.java and TilePackagedProvider.java lock handling | AE api/config/LockCraftingMode.java, helpers/patternprovider/PatternProviderLogic.java, client/gui/widgets/SettingToggleButton.java | Five mode order, icons, persistent edge detection and real ME-result accounting; GTNH interface-terminal API bridge |
| common/packaged/PackagedRecipes.java | New GTNG acquisition recipes | User-selected AE interface and TC matrix prerequisites |

The final GUI uses the existing original atlas and scrollbar PNGs unchanged for
lock/visibility controls, return tabs and target scrolling. Recovery is a new TC
extension using the same target rows and tooltip styling. Per user scope, direct
binding replaces full controller/frequency management; it is not presented as an
implementation of the entire upstream frequency system.

All source files in these two packages, including future corrections to this
port, must accompany binary distribution as corresponding source under LGPL.
The repository source is the editable form; a distributor must actually supply
the matching source, rather than relying solely on this notice. Registration
edits outside these packages and the opt-in QA harness are new GTNG glue.

Assets are separately licensed as recorded in ASSET_MANIFEST.json. No source-code
license changes the noncommercial/ShareAlike conditions on CC-covered artwork.

Direct AE essentia supply is a new GTNG extension: DirectEssentiaSupply,
ThaumicEnergisticsSupply, InfusionSourceAccess and the two Packaged TC4 mixins.
It uses the installed Thaumic Energistics 1.7.60-GTNH API (upstream commit
fbefd7b), without copying its implementation or textures. The source-mode button
reuses the already-manifested frequency_connect.png and original button atlas,
unchanged; only layout/behavior and localized tooltips are extended.
