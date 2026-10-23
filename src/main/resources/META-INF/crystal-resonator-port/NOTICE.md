# End Crystal Resonator artwork

Source: the user's installed `GTL-Enhancedcore-3.8.0.jar`, authored by S_Fast.
SHA-256: `555a9514c4d2fd11a029c091ac053d5cf7c52fba62411f2f043e04454094f7aa`.
No matching source revision has been established; no Git commit is claimed.

The archive declares LGPL-3.0 in META-INF/mods.toml, preserved as UPSTREAM-mods.toml.
The fourteen original front textures are copied byte for byte. ASSET_MANIFEST.json
records their original paths, destinations and SHA-256 hashes. They retain that
license and attribution and are excluded from the project's general MIT grant.
The LGPL-3.0 and accompanying GPL-3.0 texts are included in this directory.

Machine behavior was inspected using javap on the pinned installed artifact.
The GTNH implementation and acquisition recipes are newly written for Minecraft
1.7.10; no upstream Java implementation or modern API classes are packaged.
Native GregTech casings are referenced at runtime. Upstream UXV and OpV fronts
map to GTNH UMV and UXV respectively, preserving the ordinal voltage tiers.
