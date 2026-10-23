# Fragment World Collector attribution

The collector recipe tables, adapted recipe registration, and machine overlay
textures derive from **GTLCore**, https://github.com/GNSW/GTLCore, by its contributors.
They are licensed under **LGPL-3.0-only**, independently of GT-Not-Good's general MIT
license. See `LICENSE-GTLCore` in this directory.

Readable source reference: commit `6443c848264fb15c90cf50e031f146522616a8b9`,
`src/main/java/org/gtlcore/gtlcore/data/recipe/processing/SkyTearsAndGregHeart.java`,
`common/data/GTLMachines.java` and `common/data/GTLRecipeTypes.java`.
Textures are copied without pixel or animation changes from the user's local
`gtlcore-1.2.3.2-fix4.jar`; the local binary is identified separately by its SHA-256
in `MANIFEST.json`. This does not assert that the binary was built from that commit.
All six recipe data tables were compared with the local binary's decompiled tables
and match the pinned readable source.

Adaptations for Minecraft 1.7.10 / GTNH: native single-block machine, GTNH recipe
chance handling and NBT, twelve-output ModularUI2 inventory and NEI frontend,
non-consumable dimension display blocks, native materials and planetary rocks,
selectable circuit numbers, and optional-item availability checks. User-approved
dimension substitutions: Reactor → Twilight Forest; Glacio → Triton.

Adapted source:
`com/xyp/gtnotgood/common/recipe/machine/FragmentWorldCollectorRecipes.java`.
Extracted data: `assets/gtnotgood/recipes/fragment_world_collection.json`.
Unmodified overlay destinations and checksums are listed in `MANIFEST.json`.
The complete mapping, omissions, source evidence, and generation procedure are in
`reference/fragment-world-collector-port/PORT.md` in the GT-Not-Good source repository.
Reference sources and offline tools are not included in the runtime classes.
