# Assembly and electromagnetic factory structures

The structure data in `MageAssemblerStructure.java` and `SuperconductingElectromagneticStructure.java` derives
from [GNSW/GTLCore](https://github.com/GNSW/GTLCore), commit
`6443c848264fb15c90cf50e031f146522616a8b9`, under the upstream GNU LGPL 3.0 license. Both derived files retain
`SPDX-License-Identifier: LGPL-3.0-only`; this source data is not covered by this project's general MIT grant.
The reference checkout stays outside this project's sources and resource roots.

Pinned sources:

- [MAGE_ASSEMBLER geometry, StructureA.java:330](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/java/org/gtlcore/gtlcore/common/data/machines/structure/MultiBlockMachineAStructureA.java#L330).
- [MAGE_ASSEMBLER predicates, MultiBlockMachineA.java:968](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/java/org/gtlcore/gtlcore/common/data/machines/MultiBlockMachineA.java#L968).
- [SUPERCONDUCTING_ELECTROMAGNETISM geometry, StructureC.java:39](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/java/org/gtlcore/gtlcore/common/data/machines/structure/MultiBlockMachineAStructureC.java#L39).
- [SUPERCONDUCTING_ELECTROMAGNETISM predicates, MultiBlockMachineA.java:1103](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/java/org/gtlcore/gtlcore/common/data/machines/MultiBlockMachineA.java#L1103).
- [Chinese machine names](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/src/main/resources/assets/gtceu/lang/zh_cn.json)
  identify the machines as 巨型组装厂 and 超导电磁工厂.
- [Upstream license](https://github.com/GNSW/GTLCore/blob/6443c848264fb15c90cf50e031f146522616a8b9/LICENSE).

## Coordinate conversion

The pinned GTCEu dependency is `1.20.1-1.4.4`, commit `e3844ba943e2388c0856617ae4f8b473a0ae1b64`.
[FactoryBlockPattern.start](https://github.com/GregTechCEu/GregTech-Modern/blob/e3844ba943e2388c0856617ae4f8b473a0ae1b64/src/main/java/com/gregtechceu/gtceu/api/pattern/FactoryBlockPattern.java#L99)
uses the axes `LEFT`, `UP`, `FRONT`; rows increase upwards and the final aisle faces the controller.
[BlockPattern](https://github.com/GregTechCEu/GregTech-Modern/blob/e3844ba943e2388c0856617ae4f8b473a0ae1b64/src/main/java/com/gregtechceu/gtceu/api/pattern/BlockPattern.java#L112)
subtracts the controller coordinates before transforming these axes into world coordinates.

Both helpers reverse source rows and aisles, keep the original characters, group identical slices and return
horizontal layers for `StructureUtility.transpose`. StructureLib's north-facing default axes are west, down,
south. This conversion preserves the source's actual orientation, including the tower's bottom controller.

| Machine | Width × height × depth | Source controller column,row,aisle | StructureLib offsets |
| --- | --- | --- | --- |
| Mage Assembler | 9×10×33 | 4,4,32 | 4,5,0 |
| Superconducting Electromagnetic Factory | 15×21×15 | 7,0,14 | 7,20,0 |

For north-facing machines, the assembler extends four blocks sideways, four down, five up and 32 south from
its controller. The electromagnetic factory extends seven sideways, 20 up and 14 south from its controller.

## Mage Assembler symbols

| Symbol | Upstream predicate | Positions |
| --- | --- | ---: |
| `~` | Controller | 1 |
| `a` | Iridium casing or permitted hatch; at least 666 real casings | 730 |
| `c` | Laminated glass | 280 |
| `d` | Filter machine casing | 124 |
| `e` | Hastelloy N frame | 30 |
| `f` | Advanced assembly line unit | 55 |
| `g` | Tungstensteel frame | 4 |
| `h` | Spacetime assembly line unit | 27 |
| `i` | PTFE pipe casing | 126 |
| `-` | Muffler hatch | 1 |
| Space | Unconstrained | 1592 |

There are 1377 required non-controller positions, including the rear muffler. No positions require air.
The muffler occupies local column 4, row 5, depth 32, directly opposite the controller at the rear.
Spaces inherit GTCEu's default `any` predicate even though this machine does not explicitly map a space.

**StructureLib reserved-character constraint:** its `addShape` automatically assigns `-` to air, and
`Builder.addElement` uses `putIfAbsent`. The controller must register the `-` muffler element **before** calling
`addShape`; registering it afterwards silently retains the air predicate. The source character is preserved
here so the controller can preserve the original muffler requirement.

## Superconducting Electromagnetic Factory symbols

| Symbol | Upstream predicate | Positions |
| --- | --- | ---: |
| `~` | Controller | 1 |
| `A` | Lafium mechanical casing | 724 |
| `B` | Tempered glass | 364 |
| `C` | Improved superconductor coil | 316 |
| `D` | Molybdenum disilicide coil | 24 |
| `E` | Electrolytic cell | 156 |
| `F` | Lafium mechanical casing or permitted hatch | 25 |
| `H` | Accelerated pipeline | 19 |
| Space | Unconstrained | 3096 |

There are 1628 required non-controller positions, with 749 casing positions before hatch replacement.
All spaces explicitly use `any`; there are no required-air positions. Hatch positions occupy the original
top and lower front zones. For example, local `(column,row,depth)` positions `(5,19,1)` and `(7,0,9)` are `F`.

## Geometry fingerprints

The tests reconstruct all original aisles and bottom-to-top source rows, joined by LF without a final LF.
This canonical input has the following SHA-256 fingerprints:

- Mage Assembler: `0cc2fa0c5afbbb124ec55603e70cdeb096d7e670b1af7d75697e6f9d7474680a`.
- Superconducting Electromagnetic Factory: `dc20319fdeb108130e94cba4c203bd0eb5f7d706620ef2759fb00eec0ccdc70b`.

Each test also checks dimensions, all symbol counts, fixed front/rear/top/bottom landmarks, the source's
upwards world axis and independent returned arrays. The data preserves the original structure; power,
recipe and wireless behavior belong to the controller and are not encoded by these helpers.
