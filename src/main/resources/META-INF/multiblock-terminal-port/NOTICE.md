# Multiblock Terminal attribution

The item artwork and animation metadata are unmodified copies from GTLCore by
its contributors, repository https://github.com/GNSW/GTLCore, pinned commit
`6443c848264fb15c90cf50e031f146522616a8b9`. They retain LGPL-3.0, independently of
GT-Not-Good's general MIT grant. The complete LGPL and its incorporated GPL text
are provided as `LICENSE-GTLCore-LGPL-3.0.txt` and `LICENSE-GPL-3.0.txt`.

The same pinned revision's `UltimateTerminalBehavior`, `AdvancedBlockPattern`,
block selector, item registration and recipe are behavioral references. Its
terminal source comments acknowledge GTMThings, https://github.com/liansishen/GTMThings.
Modern Minecraft/Forge/AE2 implementation files are not compiled or packaged.
The GTNH implementation uses StructureLib construction/projection channels,
server-authoritative settings, native harvesting, material providers and AE2
security/energy APIs. Wireless fallback uses the registered terminal's native
encryption, range, upgrade and battery behavior in player/Baubles slots; it does
not create an unrestricted network lookup. Platform differences are documented in
`docs/development/multiblock-terminal.md` in the source distribution.

`com/xyp/gtnotgood/common/items/multiblockterminal/TerminalStructureCapture.java`
and `TerminalBuildPiece.java` adapt navigational traversal/metadata in StructureLib 1.4.45
`StructureUtility.iterateV2`, by GTNewHorizons/StructureLib contributors, pinned
commit `4fdea655379db43443223aa448255144b76d85ca`:
https://github.com/GTNewHorizons/StructureLib . Both adapted files retain
GPL-3.0-only and are not covered by the project's general MIT grant. The full
GPL-3.0 license is in `LICENSE-GPL-3.0.txt`. Changes collect structure positions,
retain ExtendedFacing transforms, apply the terminal's optional mirror and skip
unloaded positions. The build-piece wrapper invokes the native V2 survival-build
walker inside each native survivalConstruct callback to retain specialized last
pieces, per-layer context, element checking, hatch counts and facing. Formed
layouts follow successful native structure checks; broken layouts search native
master-channel candidates with geometry deduplication and simulated material
queries. Controller alignment
uses the public toolSetFlip API to select horizontal mirroring while retaining
direction, rotation and vertical flip; rejected mirror modes do not alter blocks,
and dismantling follows the controller's current alignment. Placement, recovery
and permission handling are supplied by the GTNH integration. Fluid returns check
container contents after filling to retain type/NBT; native ME fluid packets
preserve otherwise uncontainable remainder and can be consumed by the terminal.

`ASSET_MANIFEST.json` identifies each copied image/animation file and its original
bytes. `SOURCE_MANIFEST.json` records source artifacts, file checksums, mappings
and adaptation boundaries. Reference checkouts and source archives remain
outside runtime compilation/resource source sets. QA status is recorded in the
development progress and testing records; these provenance files do not certify
gameplay validation.
