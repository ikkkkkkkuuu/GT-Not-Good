# Structure compass API inspection

No upstream implementation or visual asset was copied into this feature. The compass face is original code-drawn geometry.
Optional adapters invoke the installed mods' generation predicates. Mixins record successful generation without changing it.

| Upstream | Inspected commit | License | Inspected API |
| --- | --- | --- | --- |
| [Roguelike-Dungeons](https://github.com/GTNewHorizons/Roguelike-Dungeons) | `9ed3801fab3af2d39929b03d242ea08660c8b53d` | GPL-3.0 | `Dungeon.canSpawnInChunk`, `HouseTower.generate`, `WorldEditor`, `ThemeHouse`, starter chest book |
| [LootGames](https://github.com/GTNewHorizons/LootGames) | `960cfc7760b3b65bb13727ea972ec3ccdf5e0f92` | All rights reserved; individual modification/distribution permission for GTNH | `LootGamesWorldGen.canSpawnInChunk_v3`, `StructureGenerator.generatePuzzleMicroDungeon`, `LGBlocks` tile IDs |

Local read-only references: `reference/structure-compass/`, ignored by Git and outside all source/resource sets.
Build dependencies follow the project's pack manifest; validation resolved Roguelike-Dungeons `1.6.6-GTNH` and LootGames `2.2.12`.
Original integration destination: `common/compass`, `mixins/late/compass`, `client/StructureCompassRenderer`.
Roguelike's seed editor overrides vanilla region random creation solely to avoid mutating the live world's RNG.

Compatibility inspection also used the installed EndlessIDs `1.7.3` source JAR (`BlockIDManager`, LGPL-3.0-only).
Live chunks use the stable `Chunk.getBlock` accessor, never the vanilla block arrays disabled by EndlessIDs.
Existing NBT supports `Blocks`/`Add`, EndlessIDs `BlocksB2Hi`/`BlocksB3`, and legacy `Blocks16` IDs.
