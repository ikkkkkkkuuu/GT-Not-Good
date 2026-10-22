# Light Hunter Space Station rendering

This distribution ports the Light Hunter Space Station renderer from Dragonators/GTLAdditions, commit `34fb152886164c56a07fc9d1b10e056f0c22b7ab`. Its Java/Kotlin rendering logic, beam GLSL, model descriptors and selected PNG assets are recorded under the upstream GPL-3.0 license. Ported Java and GLSL carry `SPDX-License-Identifier: GPL-3.0-only`.

The original `star.obj`, `space.obj` and accompanying MTL files come from GNSW/GTLCore commit `6443c848264fb15c90cf50e031f146522616a8b9`, whose repository license is LGPL-3.0. Three dimension-marker planet textures and the light-ring render-state reference come from GregTechCEu/GregTech-Modern commit `e3844ba943e2388c0856617ae4f8b473a0ae1b64`, also under its LGPL-3.0 repository license. See the accompanying `RENDERING_LICENSE_*` texts. These upstream materials are not included in the project's general MIT grant.

`RENDERING_MANIFEST.json` records immutable source URLs, source Git blob/SHA-256 hashes, destination hashes, source mappings and modifications. All 17 PNGs and both OBJ/MTL pairs retain their original bytes. The port preserves the original star/space branches, thirteen textured cube planets, deterministic tilted orbital motion and double-layer shader beam. Modern rendering interfaces are adapted to Java, Minecraft 1.7.10, GT5 rendering and GTNHLib; reference checkouts are excluded from compilation and resource packaging.

GregTech Modern's README generally credits GregTech Refreshed by ULSTICK, consistency edits and additions by Ghostipedia, and ZedTech community texture work. This record does not assert exclusive original authorship of each individual resource by its distributing repository.

Structure and controller-face assets have separate `STRUCTURE_*` records. No upstream runtime library is bundled by this rendering port.
