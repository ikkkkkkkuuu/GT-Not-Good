# Satellite thread modifier port

Source: Dragonators/GTLAdditions, commit 34fb152886164c56a07fc9d1b10e056f0c22b7ab, GPL-3.0-only.
Original implementation: ThreadPartMachine.kt and ThreadMultiplierStrategy.kt.
Original registration and acquisition recipe: GTLAddMachines.kt:304-317 and data/recipes/PartMachine.kt:264-286.

The independent GTNH hatch preserves its single ordinary-Astral-Array slot, 64-item limit, nonconsumption,
additional-thread rule n*64*m, nonsharing and item recovery. The satellite default m=2 is retained;
the unrelated GTLCore global duration multiplier is not introduced into this addon.
GTCEu/LDLib GUI and part lifecycle APIs are rewritten with native GTNH MTEHatch/MUI2 and validated live ownership.
The original 16x128 overlay and seven-tick frame metadata are copied unchanged; emissivity 15 is mapped to GTNH glow.
Its original GTLCore create_casing hull reuses the existing Heart casing port under LGPL-3.0-only.

Exact source and destination hashes and modifications are in THREAD_MODIFIER_MANIFEST.json.
The included GPL license covers the original overlay and adapted source; they are outside the general MIT grant.
Reference sources are not compiled or packaged.
