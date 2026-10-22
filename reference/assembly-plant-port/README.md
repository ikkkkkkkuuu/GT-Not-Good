# Mage Assembler / Superconducting Electromagnetic Factory

Pinned sources:

- [GNSW/GTLCore 6443c848](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9), LGPL-3.0.
- [GTCEu Modern 1.20.1-1.4.4](https://github.com/GregTechCEu/GregTech-Modern/tree/e3844ba943e2388c0856617ae4f8b473a0ae1b64), LGPL-3.0.
- [Official GregTech Leisure 1.4.5.3, CurseForge file 9238465](https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465),
  actual archive root license CC BY-NC-SA 4.0; SHA-256
  `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`.
- [LDLib-MultiLoader 1d4e4390](https://github.com/Low-Drag-MC/LDLib-MultiLoader/tree/1d4e4390ae58d16038845c651e122185c29f334b),
  GPL-3.0, offline connected-tile layout reference.

`AssemblyPlantCasing` exposes eight variants through `getStack(int)`, `getTextureId()`, `getTexture()` and
`asElement()`. Metal and glass use separate blocks; texture page 123 is reserved with an explicit conflict check.
The three active new variants use metadata 0/8, 6/14 and 7/15. Wrap their structure elements in native
`GTStructureUtility.activeCoils(...)`; GT5 tracks activity and synchronizes active client appearances.
The original improved-superconductor block is reused through its existing 4/12 metadata and the same interface.

Both machines accept standard GT energy and exotic hatches, including native wireless energy hatches.
The controller has no wireless-network mode and does not debit the owner's energy account directly.

The new art is copied without modification. Connected filter casing, laminated glass, tempered glass,
molybdenum-disilicide coil and its emissive layer each have 47 normalized masks. Regenerate all 235 tiles with:

```text
python reference/assembly-plant-port/generate-connected-textures.py src/main/resources/assets/gtnotgood/textures/blocks/assemblyplant --manifest src/main/resources/META-INF/assembly-plant-port/CTM_MANIFEST.json
```

The generator verifies that each disconnected tile exactly matches the source PNG's pixels. It reuses the
recorded Field Extruder offline quarter-tile adapter; reference checkouts and generators are outside source sets.
Original modern models and connection metadata are kept only under the provenance directory. The Forge renderer
uses the original images and native GT texture/animation interfaces, with no modern renderer or shader code.

The large-assembler controller uses `assemblyplant/controller_front[_active][_glow]`. The electromagnetic
controller reuses the existing Field Extruder fusion-reactor overlay. All copied assets, source hashes and
licenses are recorded under `src/main/resources/META-INF/assembly-plant-port/`.
Structure evidence is in `reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md`; recipe mappings are in
`reference/assembly-plant-port/RECIPE_ADAPTATION.md`.
