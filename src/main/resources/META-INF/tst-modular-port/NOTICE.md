# Twist Space Technology modular hatch port

Source: [Nxer/Twist-Space-Technology-Mod](https://github.com/Nxer/Twist-Space-Technology-Mod),
revision `f59f874694906144e205a3451dfd665c701eca97`.
Copyright belongs to the original Twist Space Technology contributors, including Nxer.
The upstream repository and resource root contain the GNU GPL version 3 license.
The adapted code and copied assets listed below retain GPL-3.0-only and are excluded from this project's general MIT grant.
The complete license accompanies this notice as `LICENSE-TST-GPL-3.0.txt`.

Adapted on 2026-10-22:

- `common/modularizedMachine/modularHatches/` becomes
  `com/xyp/gtnotgood/common/machines/hatch/modular/`. The original parallel, speed and power-consumption
  defaults, module tiers, active/inactive textures and three execution-core grades are retained.
  Overclock ratios are customized at the user's request: low-speed mode divides duration by 4 and multiplies
  EU/t by 4; perfect mode divides duration by 8 and multiplies EU/t by 4; singularity mode divides duration
  by 8 and multiplies EU/t by 2. These ratios differ from the pinned upstream implementation.
  Parameter hatches are consolidated by module type. Dynamic input uses server-synchronized ModularUI2
  integer fields with validated ranges and NBT persistence. Recipe work and output buffers live in the controller.
- `ModularizedMachineLogic/` informs `GTNGModularMachineBase` and the modular processing helpers.
  All six processing machines and the Heart generator operate without maintenance hatches, repair faults or repair
  efficiency penalties. Existing maintenance hatches remain optional structure replacements and are not auto-built.
  All six GTL/GTO ports accept every parameter and execution-core type. Without an overclock module,
  their default overclock divides duration by 2 and multiplies EU/t by 4;
  modules override it. The controller has its own execution core; extra core hatches add independent tasks.
  Core identity, progress, prepaid energy and buffered outputs persist only in controller NBT.
  Missing or disabled cores pause their work. Existing machine research, computation, coil and speed rules remain.
  Input snapshots and aggregated native ME stock/AE-energy preflight run before committing recipe inputs.
  Normal and exotic energy hatches have no additional count limit beyond available structure positions.
  At least one energy hatch remains required. Input power is summed; recipe voltage uses the highest single hatch.
- `recipe/craftRecipe/machine/ModularHatchesRecipes.java` becomes
  `com/xyp/gtnotgood/common/recipe/machine/ModularHatchRecipes.java`.
  Material costs, durations and voltages are preserved. All 41 assembler recipes select non-consumed circuit 19.
  The normal core researches this mod's Static Parallel Controller T1 for 24 hours at 30 EU/t.
  When TST is installed, its low-speed overclock recipe receives non-consumed circuit 18
  after TST finishes registering recipes. A copied recipe replaces the original and rebuilds the lookup index;
  its materials, fluids, duration, voltage and category are preserved. TST remains optional.
  TST also copies standard assembly-line recipes into its research-free map. Compatibility removes the five
  automatically copied GTNG module recipes from that map, preventing identical execution-core inputs from
  colliding and preserving research gates and bulk quantities. Both mods' standard assembly-line recipes
  and TST's own research-free recipes remain available.
  The singularity overclock recipe replaces TST's Proof of Heroes with one Perfect Execution Core and moves
  from TST's research-free assembly-line map to the standard GT assembly line. It requires research of the
  Perfect Execution Core for 192 hours at 30 EU/t. All other bulk ingredients and the original 64-day
  processing time are retained. This removes the TST runtime dependency and preserves a late-game gate.
  The High Energy Flow Circuit is looked up as `dreamcraft:HighEnergyFlowCircuit`, metadata 0,
  through the external-item registry. If absent, only the three recipes consuming it are omitted.
  No New Horizons Core Mod classes are linked by the modular recipe loader.
- `assets/gtnhcommunitymod/textures/blocks/ModularHatchOverlay/` becomes
  `assets/gtnotgood/textures/blocks/modular/`. The 12 PNG files and nine animation metadata files used by
  these hatches are copied without changing their bytes. Unused static parallel/speed artwork is omitted,
  matching the upstream code's animated overlays for both static and dynamic parameter modules.

See `ASSET_MANIFEST.json` for every source path, destination, modification and SHA-256 hash.
The reference checkout is stored in `build/reference-tst` and is excluded from compilation and packaging.
