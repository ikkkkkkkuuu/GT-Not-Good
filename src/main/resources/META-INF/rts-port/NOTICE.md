# Embedded RTS Building

GTNG includes a modified integration of Hcrab / JerryLunar and contributors'
RTS Building, official `forge-1.7.10` branch, commit
`c8bdff25ea9aa692c641fee231671afe58057a39`:
https://github.com/Hcrab/RTSbuilding/tree/forge-1.7.10

Source: `src/vendor/rtsbuilding/java` in the GTNG source distribution.
Original GUI, textures, language resources and data are bundled from that commit.
This is a modified GTNG integration, not an official RTS Building release.

Code and non-media files retain LGPL-3.0-only. Original visual/audio assets
retain the separate upstream LICENSE-ASSETS terms (All Rights Reserved).
Interface artwork credited to Re_Construction (ReConstruction-127) retains
that author's attribution. PinIn data retains its MIT notice.
These files are not relicensed under GTNG's general MIT license.
Full upstream notices are in META-INF/licenses/rtsbuilding and
META-INF/licenses/PinIn-LICENSE.txt.

OFFICIAL_IMPORT_MANIFEST.json records original paths, hashes, destinations,
and modified-source flags. Integration modifications include host lifecycle,
GTNG item ownership, separate packet channel, early mixin relocation,
GTNH AE2 rv3 API adaptation, and opt-in client regression checks.

The earlier staged preview implementation has been removed. Historical provenance remains in reference/RTS_LEGACY_ASSET_MANIFEST.json in the source repository.