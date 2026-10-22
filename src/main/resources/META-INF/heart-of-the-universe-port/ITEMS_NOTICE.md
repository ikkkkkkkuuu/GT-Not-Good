# Heart of the Universe items

Strange Annihilation Fuel Rod and Black Hole Seed artwork and item model references:
Dragonators/GTLAdditions, commit `34fb152886164c56a07fc9d1b10e056f0c22b7ab`, GPL-3.0.
The original PNGs and the seed's animation metadata are copied unchanged. The single-layer modern
generated models are represented by ordinary 1.7.10 item icons; their JSON files are reference-only.

Annihilation Constrainer artwork: GregTech Leisure official pack 1.4.5.3, CurseForge file 9238465,
`.minecraft/kubejs/assets/kubejs/textures/item/annihilation_constrainer.png`, copied unchanged.
The archive's actual root LICENSE is CC-BY-NC-SA-4.0; the artwork retains that license and attribution.

`HeartOfTheUniverseRecipes.java` adapts GTLAdditions `Misc.kt` and the controller/fuel recipe design
to verified GTNH native materials, GoodGenerator's precise assembler and the GTNH assembly line.
Recipe material and quantity changes are listed in `reference/heart-of-the-universe-port/ITEMS.md`.
The runnable zero-EU fuel recipe retains consumption of both inputs and the 60% chance of 64 constrainers;
the controller separately handles upkeep and wireless energy generation.

See `ITEMS_MANIFEST.json` for original and destination paths, hashes and per-file licensing.
These external assets and adapted GPL recipe code are excluded from the project's general MIT grant.
