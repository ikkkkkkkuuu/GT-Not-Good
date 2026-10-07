# ME Stock IO Interface provenance

Native interface geometry: GTNewHorizons/Applied-Energistics-2-Unofficial,
tag `rv3-beta-1073-GTNH`, commit `151550f6d558a663eee792f0bfa22e12a53c0e54`, sources artifact SHA-1
`12f114eb4e27c346d0278fcff7a09b5a3067b86`.
Inspected `appeng/parts/misc/PartInterface.java` and `PartBasicState.java`, including
AlgorithmX2's 2013–2014 LGPL-3.0-or-later headers, before adapting the cuboid renderer.
Destination: `common/parts/stockio/PartStockIOInterface.java`.
The three original interface cuboids, collision bounds and cable depth remain; textures reference
the already packaged AdvancedAE IO bus artwork. AE lifecycle and direct GT recipe input logic
replace DualityInterface's stock-buffer and crafting-provider behavior.

Existing AdvancedAE artwork: pedroksl/AdvancedAE,
commit `5ad43ee1e5f7a8b9fe7a1eacfaebdd44b61b624c`, LGPL-3.0,
pedroksl and contributors. Front, sides and back are reused unchanged at runtime;
their original source, license and hashes remain in `META-INF/advancedio-port/ASSET_MANIFEST.json`.
The full-block form references installed AE2's `interface/BlockInterface_Purple` texture.
No new PNGs or upstream checkout contents are packaged by this feature.

GUI and reserve/fixed behavior reference this project's super advanced ME input bus/hatch,
based on GT5-Unofficial `5.09.54.183` (LGPL-3.0); see
`META-INF/super-storage-input-port/NOTICE.md`. The shared GUI is newly assembled from installed
GregTech/ModularUI2 textures and APIs, with item/fluid pages and live authorization.
It does not reuse the Advanced IO bus's GPL amount screen.

Source/API reference: GT5-Unofficial `5.09.54.183`, sources artifact SHA-1
`b0f3eb42604cfc3ad88d56c50d0dbfa375d89419`, LGPL-3.0,
`MTEBasicMachine#checkRecipe(boolean)`. A mixin wraps the original recipe check;
temporary inputs settle actual ME consumption only after native checks succeed.
No full upstream machine source is copied. Inspect-only source archives and temporary
extractions remain outside compilation and resource inputs.
