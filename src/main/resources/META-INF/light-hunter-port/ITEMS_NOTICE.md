# Light Hunter Space Station items and recipes

Astral Array and Compressed Astral Array artwork, model references, interaction design, acquisition recipes
and aggregate batch rules are adapted from Dragonators/GTLAdditions, commit
`34fb152886164c56a07fc9d1b10e056f0c22b7ab`, under GPL-3.0.

The two original 16x496 PNGs and their 3-tick animation metadata are copied byte-for-byte. The modern
single-layer generated item models are represented by native Minecraft 1.7.10 item icons; the original
model JSONs are reference-only and are not packaged as executable models.

Forge 1.7.10 item interaction, GTNH acquisition recipes and the extracted batch rules are rewritten
for this addon. Recipe substitutions, limits, the intentionally absent unported Arcanic Astrograph
compression process, and native rock-breaking/evaporation adaptations are recorded in
`reference/light-hunter-port/ITEMS_RECIPES.md`.
The optional thread hatch retains the default machine multiplier of two. Its acquisition recipe preserves
upstream item quantities; all four mapped fluid quantities are explicitly rebalanced to 1/64 to fit the
GTNH 1.7.10 fluid representation and its endgame materials. The original absolute fluid costs are not retained.

See `ITEMS_MANIFEST.json` for source and destination paths, per-file SHA-256 hashes and modifications.
The GPL artwork and adapted source files are excluded from the project's general MIT grant.
The upstream GPL license is included as `ITEMS_LICENSE.GPL-3.0.txt`.
Structure and renderer assets have separate manifests and may retain different upstream licenses.
