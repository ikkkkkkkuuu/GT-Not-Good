---
navigation:
  title: ME Hatches and Virtual Molds
  parent: index.md
  icon: gregtech:gt.blockmachines:28504
  position: 20
  recommend: 10
  keywords:
    - pattern hatch
    - circuit pattern buffer
    - stocking input
    - output assembly
    - pattern mirror
    - virtual mold
    - data access
item_ids:
  - gregtech:gt.blockmachines:28503
  - gregtech:gt.blockmachines:28504
  - gregtech:gt.blockmachines:28519
  - gregtech:gt.blockmachines:28505
  - gregtech:gt.blockmachines:28523
  - gregtech:gt.blockmachines:28528
  - gregtech:gt.blockmachines:28521
  - gregtech:gt.blockmachines:28522
  - gregtech:gt.blockmachines:28501
  - gregtech:gt.blockmachines:28502
  - gregtech:gt.blockmachines:28526
  - gregtech:gt.blockmachines:28518
  - gregtech:gt.blockmachines:281
---

# ME Hatches and Virtual Molds

Connect ME crafting, stocked ingredients, and product outputs to GT machines. This page covers this mod's pattern hatches, circuit buffers, mirrors, stocking inputs, ME outputs, and virtual molds together.

- [Pattern hatches: supply ME crafting jobs](#pattern-hatches)
- [Pattern mirrors: share a master hatch](#pattern-mirrors)
- [Circuit buffers: configure each pattern separately](#circuit-patterns)
- [Automatic circuits for single-block machines](#automatic-circuits)
- [Stocking inputs: use existing ME stock](#stocking-inputs)
- [ME outputs and filters](#me-outputs)
- [Virtual mold slots](#virtual-molds)
- [ME Data Access Hatch](#data-access)
- [Troubleshooting](#troubleshooting)

**Input and output are relative to the machine.** Inputs supply ME ingredients to the machine; outputs return its products to ME. Pattern hatches receive crafting jobs, while stocking inputs offer marked network stock.

The following 3D scenes identify devices and demonstrate their operation. The stone platform, spacing, and arrows are illustrative. Follow the target machine's structure hints and formation check for actual installation positions.

### Compare pattern hatches

<a name="pattern-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gregtech:gt.blockmachines:28504" x="1" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="gregtech:gt.blockmachines:28519" x="3" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="gregtech:gt.blockmachines:28523" x="5" y="1" z="1" nbt="{mFacing:2s}" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Super Pattern Input Hatch: 900 pattern slots for items and fluids. Configure its shared circuit and mold in the GUI.
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Compact Super Pattern Input Hatch: 9 pattern slots, accepting both items and fluids.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    Circuit Pattern Buffer: 900 slots. Reads each original pattern's circuit and supported reusable molds.
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.7 1.5" text="Standard: 900 slots" color="#FF55BBDD" maxWidth="140" />
  <TextAnnotation pos="3.5 0.4 1.5" text="Compact: 9 slots" color="#FF88CC88" maxWidth="140" />
  <TextAnnotation pos="5.5 2.7 1.5" text="Circuit: 900 slots" color="#FFFFD166" maxWidth="140" />
</GameScene>

<a name="pattern-hatches" />

## Receive crafting jobs with a pattern hatch

Pattern hatches supply multiblocks with ingredients from ME crafting jobs. First check that the machine supports pattern input hatches, then choose the required type.

| Device | Pattern slots | Accepted ingredients |
| --- | --- | --- |
| Super Pattern Input Bus (ME) | 900 | Items only |
| Super Pattern Input Hatch (ME) | 900 | Items and fluids |
| Compact Super Pattern Input Hatch (ME) | 9 | Items and fluids |
| Circuit Pattern Buffer (ME) | 900 | Items and fluids; provides each pattern's circuit and reusable molds separately |

1. Install the hatch or bus in an allowed input position. Form and power the multiblock normally.
2. Connect ME cable to the hatch's **front**, with ME power and one available channel. Right-click with wire cutters without sneaking to enable connections on all sides.
3. Encode the actual machine recipe as a **processing pattern**, with the correct ingredients, quantities, and expected outputs. Recipes containing fluids require **Ultimate Encoded Patterns**.
4. Put the patterns in the hatch, store the actual ingredients in ME, and request the output from a terminal.

Each pattern buffers its ingredients separately. Patterns neither share those ingredients nor draw from the machine's other input buses or hatches. The **+** button opens **9 manual item slots**, shared by all patterns.

Ordinary pattern hatches and buses support a shared ghost circuit and ghost mold. Remove the corresponding reusable tools from the pattern inputs when supplying them through these slots. Use a Circuit Pattern Buffer when different recipes need different tools.

<a name="pattern-mirrors" />

## Share a master hatch with other machines

A **Super Pattern Input Mirror (ME)** can connect to any master listed above. A **Circuit Pattern Input Mirror (ME)** connects only to a Circuit Pattern Buffer. Mirrors share the master's pattern ingredients and product output caches; they add no pattern slots and do not duplicate ingredients.

1. Install the master and mirror in accepted pattern input positions. Keep both chunks loaded in the same dimension.
2. Hold a **Data Stick** and **left-click the master hatch** to record its position.
3. Keep holding that stick and **right-click the mirror without sneaking** to link it.

Once linked, right-click the mirror with an empty hand to open the master's screen. Right-click the mirror with a screwdriver to reverse the recipe iteration order. Reversing changes which recipe is checked first; it does not increase throughput.

<a name="circuit-patterns" />

## Give each pattern its own circuit and molds

With a **Circuit Pattern Buffer (ME)**, leave the required **configuration 0–24 Integrated Circuit** and molds in the original processing pattern. The buffer supplies each pattern's circuit separately. A mold becomes virtual only if matching real recipes use it without consuming it and it belongs to the supported mold catalog.

- ME crafting jobs do not request recognized virtual circuits or molds. The original physical pattern stays unchanged.
- Molds that are actual consumable ingredients are still supplied by ME and consumed normally.
- No shared circuit or ghost mold selection is needed; those selectors are hidden on the Circuit Pattern Buffer. Its 9 manual item slots remain shared by all patterns.
- Each pattern buffer holds up to **64 item types and 64 fluid types**, with up to **2^63 − 1 per type**.
- Wildcard patterns expand automatically. A slot must empty the current generated recipe's buffer before switching to another generated recipe.

Removing or replacing a pattern attempts to return its remaining ingredients to ME. Rejected returns stay buffered and retry later. Leave room in the network for both item and fluid returns.

<a name="automatic-circuits" />

## Let an ME interface configure a single-block machine

Ordinary ME Interfaces and Fluid Interfaces can dispatch processing patterns directly to adjacent **GT electric single-block machines**, selecting a ghost circuit from the recipe. The target must support ghost circuit selection and have a recipe map. Steam machines are unsupported. This feature is enabled by default but can be disabled in the server configuration.

1. Point the interface's output toward a machine face that accepts the ingredients, and connect it to ME.
2. Insert a processing pattern with accurate ingredients, quantities, and expected outputs. You may omit the Integrated Circuit. Recognized circuits or reusable molds encoded in the pattern can also be retained as virtual configuration.
3. Empty previous inputs, request one craft, and check the selected circuit, mold, and output before increasing the order. Recipes containing fluids still require Ultimate Encoded Patterns.

Matching checks the machine's recipes, actual ingredients, pattern outputs, and voltage. Conflicting circuit choices make the job wait. Several valid molds for the same circuit do not count as a circuit conflict; a matching mold already selected is preferred. Recipes with probabilistic consumption, probabilistic item outputs, or special-slot requirements are unsupported.

The same recipe can run continuously, with large orders refilling as processing frees space. Before changing circuits or molds, let the current batch finish completely and check that the machine is idle with empty inputs. The input slots and tank must still hold at least one complete operation.

### Bind a mirror with a Data Stick

<a name="mirror-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gregtech:gt.blockmachines:28504" x="1" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="gregtech:gt.blockmachines:28505" x="5" y="1" z="1" nbt="{mFacing:2s}" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Hold a Data Stick and left-click the master hatch to record its position.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Right-click the mirror with the same stick, without sneaking. Keep both devices loaded in the same dimension.
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true}>
    The mirror shares its master's buffer. It adds no pattern slots and does not duplicate ingredients.
  </LineAnnotation>
  <TextAnnotation pos="1.5 2.8 1.5" text="Stick: left-click master" color="#FF55BBDD" maxWidth="140" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Stick: right-click mirror" color="#FF88CC88" maxWidth="140" />
</GameScene>

This Data Stick procedure applies to pattern mirrors. The Wireless Packaged Pattern Provider uses an Item Wireless Connector, as described in its own chapter.

### Input and output directions

<a name="io-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gregtech:gt.blockmachines:28522" x="1" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="appliedenergistics2:tile.BlockController" x="3" y="1" z="1" />
  <Block id="gregtech:gt.blockmachines:28526" x="5" y="1" z="1" nbt="{mFacing:2s}" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Stocking Input Bus: mark ingredients to offer existing ME stock to the machine. It does not hold processing patterns.
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1">
    ME network illustration. Supply power, channels, appropriate item and fluid storage, and cable connections to each hatch.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    ME Output Assembly: separately buffers the machine's item and fluid products and sends them to ME.
  </BlockAnnotation>
  <LineAnnotation from="3 2.1 1.5" to="2 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <LineAnnotation from="5 2.1 1.5" to="4 2.1 1.5" arrow="end" color="#FF88CC88" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="Input: ME to machine" color="#FF55BBDD" maxWidth="140" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Output: machine to ME" color="#FF88CC88" maxWidth="140" />
</GameScene>

Arrows indicate material flow; actual cables are still required. The machine controller and remaining structure are omitted. Connect cables to a face allowed by each hatch or bus.

<a name="stocking-inputs" />

## Stocking inputs: supply machines from ME

| Hatch or bus | Purpose |
| --- | --- |
| <ItemImage id="gregtech:gt.blockmachines:28522" /> Super Advanced Stocking Input Bus (ME) | Supplies items to a multiblock; up to 900 marked types |
| <ItemImage id="gregtech:gt.blockmachines:28521" /> Super Advanced Stocking Input Hatch (ME) | Supplies fluids to a multiblock; up to 900 marked types |

“Input” is relative to the machine: materials move from ME into the machine. Install each hatch or bus in a position accepted for its input type and connect a powered ME network with an available channel. Each requires one channel. ME cables connect to the front by default; right-click with wire cutters to toggle connections on all sides.

### Manual marks and quantity settings

1. Disable auto-pull and mark the required items or fluids in the left panel. A filled fluid container can set a fluid mark.
2. The right panel shows quantities offered to recipes, not real materials stored inside the hatch or bus. The same material cannot be marked twice in one hatch or bus.
3. Right-click an existing mark to set its **Keep in ME** reserve and **Available per recipe check** quantity.
4. Enable reserve mode and fixed mode as needed using the middle buttons. Entering a value alone does not enable its mode.

- **Reserve mode:** offers only stock above the configured reserve. The reserve accepts values from 0 through 2^63 − 1.
- **Fixed mode:** offers the configured quantity to a recipe check only when enough stock is available; otherwise that material is unavailable. The range is 1 through 2^31 − 1, with defaults of 64 items or 1000 mB. Only actual recipe consumption is withdrawn.
- Both modes can be enabled together: the reserve is subtracted before testing the fixed quantity. Without fixed mode, one check can see up to 2^31 − 1 items or mB per marked type.

For example, reserve 1000 iron ingots and set a fixed quantity of 64. At 1063 ingots in ME, no iron is offered. At 1064, a check can use up to 64. A recipe consuming 8 ingots withdraws only 8.

### Auto-pull

Auto-pull refreshes marks from existing ME stock, selecting up to 900 types meeting the minimum quantity. Click its button or right-click the hatch or bus with a screwdriver to toggle it. Right-click the button to configure the minimum quantity and refresh interval. Defaults are 1 and 100 ticks, respectively; 100 ticks is 5 seconds at 20 TPS. Both settings accept 1 through 2^31 − 1.

Toggling auto-pull clears existing marks and per-material quantity settings. Manual marking is disabled while it is on. Auto-pull selects existing stock; it does not request ME autocrafting or replenish missing materials. There is no local material cache, so supply depends on ME power, a channel and available stock. Picking up the hatch or bus clears its marks and settings.

<a name="me-outputs" />

## ME outputs: return machine products to the network

| Hatch or bus | Accepted products | Total output cache capacity |
| --- | --- | --- |
| <ItemImage id="gregtech:gt.blockmachines:28501" /> Max Capacity ME Output Bus | Items | 2^63 − 1 items |
| <ItemImage id="gregtech:gt.blockmachines:28502" /> Max Capacity ME Output Hatch | Fluids | 2^63 − 1 L |
| <ItemImage id="gregtech:gt.blockmachines:28526" /> ME Output Assembly | Items and fluids | Separate caches: 2^63 − 1 items and 2^63 − 1 L |

Each capacity is shared by all products in that cache, not granted separately to every product type. The assembly counts its item and fluid caches separately.

“Output” is relative to the machine: products move into ME. Install it in an accepted machine output position and connect ME. Each output requires ME power and one channel; the assembly shares that channel between items and fluids. Cables connect to the front by default; wire cutters toggle connections on all sides.

**One ME Output Assembly can provide both an item output bus and a fluid output hatch**, saving multiblock output positions. Placement still depends on the machine's structure rules and formation check; special machines may restrict supported outputs.

### Route specific products

The GUI filter marks are whitelists that do not consume the samples. The item bus has 9 item marks, the fluid hatch has 9 fluid marks, and the assembly has 9 of each. The two assembly lists are independent.

- An empty list imposes no restriction for that product category. Once any sample is set, only products matching at least one sample in that list are accepted.
- Items match item identity, damage and NBT; fluids match fluid identity and NBT. Sample quantity does not affect matching.
- Normal output ordering prefers matching filtered outputs over unfiltered outputs. Mark selected products in one assembly and leave another suitable output unfiltered for the remaining byproducts.
- Changing marks does not clear existing cache contents. A storage cell's partition filters can further restrict a separate output bus or hatch; the assembly's cell partitions affect only its item side.

For example, mark iron ingots in an assembly's item row and water in its fluid row. It accepts those products; copper ingots and other fluids need another suitable output.

### Cache mode, check mode and full storage

By default, cached products are sent to ME. A storage cell is not required to expand these output caches. If ME lacks power, a channel or suitable storage space, products that cannot be sent remain cached. Large caches do not provide unlimited capacity or throughput. Further recipes depend on remaining cache space, check mode and the machine's output protection settings.

- **Right-click with a screwdriver** to toggle cache mode; **Shift + screwdriver** toggles check mode. Both also have GUI buttons.
- With a suitable internal storage cell, native cache mode stores products in that cell; the GUI priority controls that cell's ME storage priority. Without a cell, products are still sent to the network. The assembly's cell slot belongs to the item side; fluids continue to ME fluid storage.
- Check mode limits recipe parallelism according to remaining output cache space. In cache mode with an internal cell, it also checks that cell's space. It does not add room to full ME disks.

Drain caches before removing outputs. Picking up an ME Output Assembly clears both product caches and its filter marks. Do not use the separate max-capacity outputs to transport cached products; picking them up clears this mod's filter marks as well.

### Select a virtual mold in the GUI

<a name="mold-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gregtech:gt.blockmachines:28504" x="1" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="gregtech:gt.blockmachines:281" x="5" y="1" z="1" nbt="{mFacing:2s}" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Ordinary Super Pattern Input Hatch: open its GUI, then Shift + left-click the mold slot. All its patterns share this selection.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    LV Extruder example: finish current and pending inputs, empty the inputs, then select an extruder shape in the machine GUI.
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.8 1.5" text="Hatch: select in GUI" color="#FF55BBDD" maxWidth="140" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Extruder: select in GUI" color="#FFFFD166" maxWidth="140" />
</GameScene>

Virtual mold slots are inside device GUIs. The annotations identify which device to open; they do not mark physical insertion slots on its surface.

<a name="virtual-molds" />

## Virtual mold slots

Use a virtual mold slot to select a **non-consumed recipe tool** without crafting or carrying the physical tool first. Its catalog includes molds, extruder shapes, lenses, inscriber presses, nanites and molds for single-use tools. Catalog membership does not make an item reusable in every recipe. **Items actually consumed by a recipe still require normal supplies**, including the single-use tools themselves.

A selection changes recipe configuration. It does not put a mold in your inventory, and you cannot extract a mold from the slot. Supply the actual ingredients, fluids and power, and satisfy the machine's voltage and other recipe requirements. Use an appropriate physical tool slot for reusable tools outside the catalog.

### Electric single-block machines

For example, right-click an **LV Extruder** and find the mold icon above its existing ghost circuit slot. **This slot is inside the machine GUI, not on a face of the block.**

| Action | Result |
| --- | --- |
| Shift + left-click the mold slot | Open the scrollable catalog |
| Left-click a catalog entry | Select it without a physical item |
| Left-/right-click the mold slot | Cycle forward/backward, including the empty option |
| Scroll over the mold slot | Cycle catalog entries |
| Shift + right-click the mold slot | Clear the selection |
| Right-click a catalog entry or the current mold at the top | Clear the selection |

Before changing or clearing a mold, let the current operation and all pending input finish, then empty the item and fluid inputs. Manual changes are rejected while an operation is still in progress or the physical inputs contain ingredients. Pausing a machine does not finish its current operation.

The added slot applies to GT electric single-block machines that support circuit selection and have a recipe map. It is displayed by their standard MUI2 GUI. Steam machines, multiblocks, machines without circuit selection and machines without a recipe map are outside this scope. Older or custom GUIs are not guaranteed to show the selector. A machine's appearance alone does not establish support.

A single-block machine selects one catalog mold at a time, without using a real ingredient slot. A physically consumed mold still requires normal supplies. The circuit and mold are separate settings: selecting a mold does not supply the required programmed circuit.

### Shared slots on Super Pattern Input Hatches

The Super Pattern Input Bus, Super Pattern Input Hatch and Compact Super Pattern Input Hatch have a shared mold slot in the lower-left area of their main GUI. Shift + left-click opens the catalog; left-click selects an entry. With an empty cursor, left-/right-click or scroll over the slot to cycle, and **Shift + right-click to clear**. Right-clicking a catalog-grid entry also clears; holding Shift when clicking a grid entry closes the panel after the action. A normal slot click while holding a catalog mold copies that selection without consuming the held item.

The hatch's patterns share this selection. Separate tasks requiring different molds into different hatches, or use the Circuit Pattern Input Hatch's per-pattern settings. Finish old tasks before switching. The Circuit Pattern Input Hatch itself has no global mold selector.

Pattern optimization is a separate feature that lets multiblock recipe checks cache candidates from a pattern. It does not choose the shared mold or turn an encoded mold requirement into a virtual tool. When using the shared slot, omit the non-consumed mold it supplies from the pattern, and ensure the pattern, shared circuit and shared mold together match the recipe. For several circuit/mold configurations in one hatch, see this page's Circuit Pattern Input Hatch instructions.

<a name="data-access" />

## Data access: share Data Sticks from ME

<ItemImage id="gregtech:gt.blockmachines:28518" />

The **Data Access Hatch (ME, IV)** supplies recipes recorded on ME-stored Data Sticks to multiblocks that accept data access hatches. Install it in an accepted position, connect a powered ME network with an available channel to its front, and store researched Data Sticks in ME.

It reads actually stored sticks, which remain in ME and can be shared by several machines. A craftable entry with no stored stick cannot supply research. No manual insertion is required, and right-clicking does not open a storage GUI. Losing power, a channel, or the stored stick removes that research. Machine power, structure, and other recipe requirements still apply.

<a name="troubleshooting" />

## Troubleshooting

| Symptom | Check |
| --- | --- |
| A connected hatch does not work | Front cable connection, ME power, channels, and machine structure; mirrors also need a loaded master |
| A pattern's output is missing from the terminal | Processing pattern, actual machine recipe, Ultimate Encoded Pattern for fluids, and the master's network connection |
| A mirror will not link | Data Stick: left-click master, then right-click mirror without sneaking in the same dimension; circuit mirrors require a circuit buffer |
| A stocking input displays stock but recipes fail | Reserve, fixed quantity, actual stock, and other recipe conditions; the display is not a local physical buffer |
| Auto-pull removed existing marks | Toggling clears marks and quantity settings; refresh uses current stock and does not request autocrafting |
| Output stops or parallelism drops | Cache room, ME storage, check mode, filters, and machine output protection; capacity is finite |
| A virtual mold does not match | Supported catalog, non-consumed use, circuit, and ingredients; encode the tool in the original pattern for circuit buffers |
| A single-block machine cannot change mold or auto-select its circuit | Finish all current and pending input, empty inputs, then check machine support, recipe, voltage, and ambiguous circuit choices |
| A data access hatch supplies no research | Front connection, channels, and valid Data Sticks actually stored in ME |

For wireless delivery to an Arcane Workbench, altar, or assembly line, see the [Wireless Packaged Pattern Provider](packaged-pattern-provider.md).
