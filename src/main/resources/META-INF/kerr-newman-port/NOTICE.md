# Kerr-Newman Homogenizer port

The structure, machine behavior references, recipe references, new casing artwork and black-hole rendering sources
come from GregTech-Odyssey/GTOCore revision `3121e23241e7f6ccd2e6a8dbd5655962de63ed9a`, under LGPL-3.0.
These upstream materials and their adaptations are excluded from the project's general MIT grant.
See `LICENSE-GTOCore-LGPL-3.0.txt` and the full incorporated GPL terms in `LICENSE-LDLib-GPL-3.0.txt`.
Independent native block/texture adapters retain the project's MIT license; this does not relicense the artwork.

The complete original `src/main/resources/pattern/kerr_newman_homogenizer.mbs` is shipped byte-for-byte at
`assets/gtnotgood/structures/kerr_newman_homogenizer.mbs`. The 83 x 36 x 83 layout, LEFT/UP/FRONT directions,
controller position and all unconstrained spaces are preserved. `KerrNewmanHomogenizerStructure.java` reads that
original binary format and converts it to StructureLib horizontal layers with offset (41,12,7).
Only the source controller symbol b becomes StructureLib ~; width is not mirrored. The original contains 15,638
non-space positions including its controller. K positions allow native hatches; their count is not a casing minimum.

Character mapping is taken from `common/data/machines/MultiBlockH.java`, KERR_NEWMAN_HOMOGENIZER.
It adds 23 distinct casing appearances in three block IDs, with stable indexed texture page 122.
Existing iridium, hyper mechanical, dimension injection, hollow and containment field blocks are reused rather than
duplicated; their original attribution remains in `META-INF/field-extruder-port/` and
`META-INF/dimensional-focus-port/`. In particular, those reused official-pack textures retain their existing
noncommercial/share-alike terms and do not acquire a new license through this port.

New PNG artwork is copied unchanged from GTOCore. Atomic Casing and its connected atlas come from
GregTech-Odyssey/GregTech-Modern revision `30cfd9fb3de3faee38cab6b36c5ba171ed82ba48`, also LGPL-3.0;
see `LICENSE-GregTech-Modern-LGPL-3.0.txt`. GTO declares GTCEu 26.7.3; this pinned owning-fork source is
recorded without claiming byte identity with an unavailable 26.7.3 source artifact.

Original metadata is retained byte-for-byte under `source-metadata/`. Runtime copies retain animation sequences,
per-frame timing and interpolation requests, and replace unsupported modern LDLib connection directives with
47 native quarter-tile combinations. Original PNG pixels are not repainted, recolored or downsampled.
Active Accelerator Coil UHV and Advanced Fusion Coil use the original two-layer base/bloom model with separate
native ordinary lighting and emissive overlays. Dimensional Bridge's registered ordinary model does not reference
its unused bloom artwork; the port does not activate it.

The offline CTM layout reuses `reference/field-extruder-port/generate-connected-textures.py`, adapted from
KilaBash / Low-Drag-MC LDLib-MultiLoader revision `1d4e4390ae58d16038845c651e122185c29f334b`,
`Connections.fillSubmaps` and `Submap.uvs`, under GPL-3.0. The generator is distributed as source; modern LDLib
rendering classes are not compiled or embedded by this feature. Generated artwork retains its source LGPL license.

Survival recipes reference the same GTO revision's `data/recipe/classified/Assembler.java`, `AssemblerA.java`,
`Vanilla.java` (Amprosium Pipe Casing), `ArcFurnace.java` (industrial glass), `PrecisionAssembler.java`
(bioactive/active casings) and `data/recipe/Temporary.java` (UHV accelerator coil).
The native implementations are `KerrNewmanCasingRecipes.java` and `KerrNewmanHomogenizerRecipes.java`.
They map unavailable materials and modern facilities to native GTNH equivalents and native recipe maps while
retaining explicit input quantities, processing times and voltage costs as documented beside the resulting recipes.
The controller uses UEV assembly-line components, sixteen UHV mixers and native scanning research.
User-selected processing balance is fixed 6000% speed (duration divided by 60) and perfect overclocking
(four times EU/t and duration divided by four per overclock), displayed through native GTNH tooltip helpers.
These settings are local overrides, not claims about original GTOLib performance.
The port supplies the original GTO appearance through obtainable structure components; appearance conversion
does not grant extra processing behavior to a casing block.

Material substitutions include Trinaquadalloy -> GT++ Trinium Naquadah Carbonite, MAR-M200 Steel -> Maraging 350,
Hastelloy N75/Reactor Steel -> Hastelloy N, Seaborgium -> molten Neutronium, Depleted Uranium Alloy -> Uranium,
and Orichalcum -> Abyssal Alloy. Native GT++ elemental Rhenium plates are used directly.
Native Babbitt Alloy has no generated frame form; Shielded Accelerator replaces its one frame with two Babbitt
plates, preserving two material units. Radiation Absorbent retains its four Babbitt plates.
Double plates count as two ordinary plates; sixteen zirconia flakes become four Cubic Zirconia dust.
HUI2/HUI4 workstations map to native LuV Master Quantum Computer / ZPM Wetware Computer manufacturing tiers.
The eight-ingredient Advanced Fusion Coil retains native assembler processing, material costs, 491520 EU/t,
100 ticks and its cleanroom requirement. Installed MV-or-higher native assemblers support nine item input slots.
Bioactive Casing uses native Slick Sterile Flocculation Casing and Cubic Zirconia as its flattened predecessor,
retains the final biocell/regulator/solder/sterile-medium/tritanium costs and adds the predecessor's molten Glass.
Native Good Generator precise assembler retains UV power and 200 ticks, special value 0; it has no receiver for
the original sterile cleanroom gate, which is explicitly omitted. Dimensional Bridge is a native one-to-one
appearance conversion. Industrial glass retains arc processing; Amprosium Pipe retains original crafting costs.
The detailed, cumulative recipe and closed-library compatibility audit is distributed as RECIPE_ADAPTATION.md.

The black-hole scene refraction and required compatibility changes are described in `RENDER_NOTICE.md`.
Exact source paths, revisions, licenses, destination changes and SHA-256 hashes are recorded in
`SOURCE_MANIFEST.json`, `ASSET_MANIFEST.json`, `STRUCTURE_MANIFEST.json`, `PORT_MANIFEST.json`
and `RENDER_MANIFEST.json`. Original reference files and rebuild scripts are distributed in
`reference/kerr-newman-port/`, excluded from compilation and resource packaging.
