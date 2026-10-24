# Production Line Simplification recipe adaptation

Source: [QionSi/GregTech-Production-line-simplification](https://github.com/QionSi/GregTech-Production-line-simplification),
commit `0fe417a67a340575ed6fc31be3435354a519fa7e` (2026-10-20).
Copyright (c) 2026 QionSi. The adapted recipe code retains the MIT license in `LICENSE-MIT.txt`.

- `recipe/ModRecipes.java` supplies twelve petrochemical processing recipes, adapted into
  `common/recipe/gtnotgood/PetrochemicalSynthesisRecipes.java` and the Integrated Chemical Plant's fourth mode.
  Its four alternative oils, circuits, quantities, output chances, durations and EU/t are retained.
  Circuit 4 produces styrene, as the original implementation does, despite the original polystyrene method name.
  Dust stacks use the native unsafe amount copy to retain the declared 80-carbon output of circuit 7;
  the upstream helper accidentally clamps that output to 64 in this GT5 version.
- `recipe/RareEarthRecipes.java` supplies eleven dust and twelve ore processing recipes, adapted into
  `common/recipe/gtnotgood/RareEarthProcessingRecipes.java` and two additional Kerr-Newman Homogenizer modes.
  Ingredients, quantities, circuits, durations and EU/t are retained. Ore temperature requirements are removed
  at the user's request; all three modes use the homogenizer's existing speed and modular processing rules.
- Registrations use GTNG's post-init recipe loader and central recipe maps. Material lookups use the fixed
  GTNH 2.9.0-RC-2 / GT5-Unofficial 5.09.54.205 APIs. Upstream controller recipes, machines, structures,
  performance formulas, artwork, mixins and the Nascent White Branch are not included.

`SOURCE_MANIFEST.json` records the pinned source files, hashes, destinations and adaptations.
The reference checkout remains outside compilation and resource source sets.
