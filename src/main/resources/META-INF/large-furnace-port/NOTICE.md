# Large Furnace port

Large Furnace geometry and behaviour: S_Fast, installed GTL-Enhancedcore 3.8.0, LGPL-3.0.
Parallel hatch behaviour, tiered hulls and mk1-4 front layers: GregTechCEu / GregTech-Modern 1.4.4, LGPL-3.0.
Parallel hatch mk5-10 front layers: GTLCore 1.2.3.2-fix4, POWSDSSAG / EasterFG / Maple, LGPL-3.0 as declared in META-INF/mods.toml.
Multiple-recipe batch semantics inspected from GTLAdditions 3.3.1Custom. Existing GTNG GPL-3.0 batch/output helpers are reused.
Archive hashes and individual source/output asset hashes are in ASSET_MANIFEST.json. All ten parallel hatch tiers retain their idle, active and emissive PNGs and animation metadata: mk1-9 have four frames, mk10 has eight, all at eight ticks per frame. Original top/bottom/side hull textures are used for every tier, including the seven original animation metadata files: UHV has two frames, OpV eight, and MAX side ten with its original explicit frame sequence. GTNH UMV/UXV map by ordinal to upstream UXV/OpV, respectively. No matching Enhancedcore source commit has been established.
These external works are not relicensed under the project's general MIT license.
The port replaces modern APIs, uses native GTNH steel casings/steel blocks, Coke Oven Bricks (gt.blockcasings12:0), Firebricks (gt.blockcasings4:15) and voltage tiers, and supplies host-specific controller/hatch acquisition recipes. No duplicate brick blocks or brick recipes are registered.
Original pattern: 11 x 13 x 12, 716 non-space cells; spaces unconstrained. Front controller offset (5,11,0).
Parallel hatches: IV through MAX, 4^(tier-4), selected multiplier 1..maximum. UXV/OpV ordinal tiers map to GTNH UMV/UXV.
LV base: 64 parallel x 2 threads; both double per voltage tier. A batch selects at most the thread count of distinct recipes and shares parallel x threads capacity.
Batch EU is the sum of recipe EU/t x duration x amount, with upstream minimum 20 ticks and rate calculation. No free power or independent physical execution cores.
The existing Omega thread hatch is shared via an ownership-checked host interface. No galaxy network or unrelated upstream integrations are added.

The two furnace overlays retain their original 16x16 panel layout. The 62 metal-frame pixels per sprite are fully transparent so the separately rendered GTNH casing supplies their appearance. All remaining 194 pixels, including indicators and existing transparency, are unchanged. Generated editing previews were rejected for changing the geometry; only the original source pixels and the exact frame mask are packaged. All hatch PNGs and metadata are unchanged source bytes; the original hatch_1 through hatch_4 resource paths are preserved. No matching GTLCore source commit has been established for the installed archive.
