# Kerr-Newman recipe and processing audit

Source: GTOCore `3121e23241e7f6ccd2e6a8dbd5655962de63ed9a`, LGPL-3.0-only.
The controller recipe is `src/main/java/com/gtocore/data/recipe/classified/AssemblyLine.java`,
builder `mega_mixer`; the definition is `MultiBlockH.KERR_NEWMAN_HOMOGENIZER`.

The native assembly-line adaptation preserves nine item lanes with amounts
16/16/16/16/8/8/32/32/32, three 1000 mB fluid lanes, 800 ticks and exactly
2,097,152 EU/t. The items are UHV mixers; UEV pumps, motors, robot arms and sensors;
UIV circuits; long osmium and europium rods; and Quantum plates.

| GTO ingredient | Obtainable GTNH adaptation | Reason |
| --- | --- | --- |
| Quantanium curved plate ×32 | GT++ Quantum plate ×32 | Quantanium and curved plate forms do not exist in this native API. Quantum is a generated GT++ alloy with native plate recipes. |
| Mutated Living Solder 1000 mB | Native Mutated Living Solder 1000 mB | Same material and quantity. |
| High Durability Compound Steel 1000 mB | GT++ Titansteel 1000 mB | GTO's tungstensteel/HSSS/HSSG/ruridit alloy has no native equivalent. |
| Orichalcum 1000 mB | GT++ Abyssal Alloy 1000 mB | The similarly named native fantasy material disables automatic blast-furnace recipes; its survival acquisition was not established. |
| GCYM Large Mixer research | GT++ Industrial Mixer research | Available native predecessor. |

Quantum, Titansteel and Abyssal Alloy are generated through `MaterialGenerator.generate`
in the installed GT5-Unofficial 5.09.54.205. That path generates the native alloy,
blast-smelter, fluid and component recipes. Missing controller materials fail registration
with an explicit ingredient error.

The original research requires 256 CWU/t. This is a computation rate, not a duration.
Native assembly-line scanning uses the established port convention of 2400 ticks at UEV;
it retains the predecessor and late-game component gates without inventing a 256-tick scan.

The old official GTL 1.4.5.3 pack has a different dimensionally transcendent mixer recipe.
Its 17 lanes and 2000-tick UIV recipe are not the GTO Kerr-Newman controller recipe and
were not substituted here.

## Closed library boundary

GTO declares `CrossRecipeMultiblockMachine(holder, false, true, MachineUtils::getHatchParallel)`
and the mixer recipe map. Its `multipleRecipesTooltips` call is commented out.
The pinned release depends on GTOLib 26.7.4. The official pack's
`mods/gtocore-forge-1.20.1-0.5.6-beta.jar` contains that exact nested library:

- GTOCore jar SHA-256: `fb5b563d98eed58a81e978113895057a09e05aedfcc7f1ed9925126593836b3c`
- Nested GTOLib jar SHA-256: `7384cd66cc844ce8910a2af8b525d9e14db31b950847ad0a5d98f4a9061529ad`

`javap -p -c` confirms the base's behavior is native; its constructor only throws
`UnsatisfiedLinkError("Not Impl")`, and its recipe/configuration methods are native.
The public library source repository is unavailable. Its constructor booleans, base
overclocking factors and cross-recipe scheduling cannot be verified from the published
library implementation. No closed library code is copied or packaged. Public GTO sources
separately confirm that this machine accepts acceleration, overclock and thread hatches;
their supported effects must not be mistaken for absent performance bonuses.

At the user's request, this port now uses fixed 6000% speed (60 times base speed,
processing duration divided by 60) and perfect overclocking (four times EU/t and one
quarter duration per overclock). Native energy/voltage protection and the power-panel
parallel limit still apply. No separate EU/t discount is applied. Native minimum-tick
and sub-tick parallel handling remain in effect. These are explicit local balance
settings rather than reconstructed upstream rates or ports of the modern feature hatches.
The tooltip uses GTNH's native static-speed and perfect-overclock helpers.

The Bioactive Casing uses the native Good Generator precise assembler at `specialValue(0)`.
Its original biological predecessor is flattened into obtainable native sterile flocculation
casing, cubic zirconia dust and molten glass while retaining the original biocells, regulator,
solder, sterile medium, tritanium, UV power and duration. The native precise assembler checks
only its casing tier; it has no cleanroom receiver or sterile-room gate. Adding the builder's
cleanroom metadata would change the special value to -200 without enforcing that condition,
so this unsupported room gate is explicitly omitted.

## Casing costs and native substitutions

The 23 casing recipes additionally reference the pinned revision's `Assembler.java`, `AssemblerA.java`,
`Vanilla.java:415–419`, `ArcFurnace.java:69–76`, `Temporary.java:169–178` and
`PrecisionAssembler.java:541–564`. `Temporary.java` is directly under `data/recipe/`; the other files are
under `data/recipe/classified/`. All original source files are retained in this reference tree.

| Original material or form | Native adaptation |
| --- | --- |
| Trinaquadalloy | GT++ Trinium Naquadah Carbonite |
| MAR-M200 Steel | GT++ Maraging 350 |
| Hastelloy N75 / Reactor Steel | GT++ Hastelloy N |
| Molten Seaborgium | Molten Neutronium |
| Depleted Uranium Alloy | Native Uranium |
| Orichalcum | GT++ Abyssal Alloy |
| Rhenium plate | Native GT++ elemental Rhenium plate |
| Babbitt Alloy frame in Shielded Accelerator | Two native GT++ Babbitt Alloy plates, preserving two material units |
| One double plate | Two ordinary plates, preserving its material quantity |
| Sixteen Zirconia Ceramic flakes | Four Cubic Zirconia dust; one flake is one quarter material unit |
| Eight Tricalcium Phosphate Ceramic flakes in the biological predecessor | Two Cubic Zirconia dust |
| HUI2 high-computation workstation | Native Master Quantum Computer, manufactured at LuV |
| HUI4 high-computation workstation | Native Wetware Computer, manufactured at ZPM |

The last two substitutions follow verified `Item03` manufacturing tiers in installed GT5-Unofficial 5.09.54.205,
not similarly named modern Quantum/Crystal circuit tiers.

Native GT++ Babbitt Alloy uses `MaterialGenerator.generate(..., false)` and has plate recipes but no frame form.
Only the Shielded Accelerator frame is replaced with two plates; Radiation Absorbent Casing retains its original
four Babbitt Alloy plates. The material remains Babbitt Alloy in both recipes.

Advanced Fusion Coil retains the native assembler process with its eight item ingredient types, original material
costs, 491,520 EU/t, 100-tick duration and cleanroom requirement. Installed GT5-Unofficial 5.09.54.205 defines
`RecipeMaps.assemblerRecipes.maxIO(9,...)`, and MV-or-higher native assemblers have nine item input slots.
The native recipe therefore requires no research or recipe-map substitution.

Bioactive Casing's biological predecessor is flattened into one native Slick Sterile Flocculation Casing,
two Cubic Zirconia dust and 576 mB molten Glass. The final native recipe also consumes 64 Biocells,
two UHV Fluid Regulators, 576 mB Mutated Living Solder, 576 mB Sterilized Bio Medium and 288 mB molten Tritanium.
It uses Good Generator's precise assembler, UV power, 200 ticks and special value 0. The inaccessible modern
biological predecessor and its sterile-room machinery are not introduced as separate native recipe chains.
The original sterile cleanroom gate is omitted for the precise assembler compatibility reason above.

Dimensional Bridge uses one existing native Dimensional Bridge casing in a one-to-one appearance conversion.
Industrial Frameless Glass retains the arc-furnace process: four existing mod Tempered Glass and
144 mB molten Silicon yield four glass blocks. Amprosium Pipe Casing retains its original crafting-table
recipe of four Neutronium plates, four medium Neutronium pipes and one Neutronium frame.

Native neutron reflectors use the registered `ItemList.Neutron_Reflector`; the obsolete unregistered
`Reactor_NeutronReflector` entry is not used. Ingredient helpers reject missing items and fluids rather than
registering recipes with silently removed inputs.
