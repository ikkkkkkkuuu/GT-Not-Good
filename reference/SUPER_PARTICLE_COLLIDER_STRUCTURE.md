# Super Particle Collider structure

The structure data in `SuperParticleColliderStructure.java` derives from
[GNSW/GTLCore](https://github.com/GNSW/GTLCore), commit
`6443c848264fb15c90cf50e031f146522616a8b9`, under the upstream GNU LGPL 3.0 license.
The derived Java file retains `SPDX-License-Identifier: LGPL-3.0-only` and is not covered by this project's MIT grant.

Sources at this pin:

- [MultiBlockMachineAStructureA.java, SUPER_PARTICLE_COLLIDER](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/java/org/gtlcore/gtlcore/common/data/machines/structure/MultiBlockMachineAStructureA.java#L173).
- [MultiBlockMachineA.java, block predicates](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/java/org/gtlcore/gtlcore/common/data/machines/MultiBlockMachineA.java#L588).
- [Upstream license](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/LICENSE).

The structure is 47 blocks wide, 7 blocks high and 47 blocks deep. The source controller occupies column 23,
row 3 of aisle 46. The port reverses aisle order, replaces `a` with StructureLib's `~`, groups identical slices,
and returns horizontal layers for `StructureUtility.transpose`. The StructureLib offsets are `(23, 3, 0)`.
With a north-facing controller the structure extends 46 blocks south, 23 blocks either side and 3 blocks up/down.

| Symbol | Effective upstream block | Positions |
| --- | --- | ---: |
| `a` → `~` | Controller | 1 |
| `b` | Lafium mechanical casing or permitted hatch | 14 |
| `c` | Lafium mechanical casing | 1777 |
| `d` | Accelerated pipeline | 560 |
| `e` | Superconducting coil | 320 |
| `f` | Enriched naquadah frame | 128 |
| Space | Unconstrained position | 12663 |

All 14 hatch positions lie on the front face. The structure contains no required-air positions; the center
and other blank positions must remain unconstrained. There are 1791 casing positions before replacing hatches.

## Repeated `d` predicate

The source maps `d` first to `kubejs:aggregatione_core` and then to `kubejs:accelerated_pipeline`.
The pinned dependency is GTCEu `1.20.1-1.4.4`, commit `e3844ba943e2388c0856617ae4f8b473a0ae1b64`.
Its [FactoryBlockPattern.where implementation](https://github.com/GregTechCEu/GregTech-Modern/blob/e3844ba943e2388c0856617ae4f8b473a0ae1b64/src/main/java/com/gregtechceu/gtceu/api/pattern/FactoryBlockPattern.java)
uses `symbolMap.put(symbol, predicate)`, so the second predicate replaces the first. The effective source
structure therefore has 560 accelerated pipelines and no aggregation cores. The port preserves this behavior;
the source gives no distinct character or position from which to infer an intended aggregation core placement.

## Validation

`SuperParticleColliderStructureTest` checks every dimension, block count, the front controller and hatch zone,
left/right and vertical symmetry, the unconstrained ring center and independence of returned arrays. It also
reconstructs the original aisle order and controller character and checks the complete source geometry hash.

Canonical input is all original aisle rows in original order, joined by LF without a final LF. Its SHA-256 is
`bab9591197189fdddfa6ecf6b490286468be6dc0494f2bde0ebcbf4426b186d5`.
