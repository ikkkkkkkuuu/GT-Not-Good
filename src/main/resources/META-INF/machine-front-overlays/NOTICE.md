# GTL machine front overlays

Original visual assets from installed GTL-Enhancedcore 3.8.0 (S_Fast) and GTLCore 1.2.3.2-fix4 (POWSDSSAG, EasterFG, Maple).
Both archives declare LGPL version 3 in META-INF/mods.toml. These assets remain under that license, independently of GTNG's general MIT license. Full LGPL-3.0 and GPL-3.0 texts accompany this notice.

ASSET_MANIFEST.json records the pinned archive SHA-256, original paths, destination paths, source authors, license declarations, dimensions, and per-file source/output SHA-256. Matching source commits have not been established for these installed builds. GTLCore's project URL declared in its metadata is https://github.com/nutant233/GTLCore.

All PNGs and accompanying animation metadata are copied byte-for-byte. No recoloring, background removal, resizing, or state synthesis has been applied. Only families with transparent idle and active fronts are selected. Original paused and emissive layers are included where present.

The textures are reusable resources, not registrations of the upstream machines. GTNH controllers must explicitly draw a casing, then the selected front, then any emissive layer using TextureFactory.glow(). The upstream Shimmer/gtlbloom metadata is preserved for provenance but does not provide a Minecraft 1.7.10 bloom implementation.
