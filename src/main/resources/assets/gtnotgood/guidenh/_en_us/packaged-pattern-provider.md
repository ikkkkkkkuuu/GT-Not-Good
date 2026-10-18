---
navigation:
  title: Wireless Packaged Pattern Provider
  parent: index.md
  icon: gtnotgood:wireless_packaged_pattern_provider
  position: 10
  recommend: 10
  keywords:
    - Packaged Pattern Provider
    - Packaged Core
    - Wireless Connector
    - Arcane Workbench
item_ids:
  - gtnotgood:wireless_packaged_pattern_provider
  - gtnotgood:packaged_wireless_connector
  - gtnotgood:arcane_workbench_packaged_core
---

# Wireless Packaged Pattern Provider

Dispatch ME autocrafting jobs to bound machines using a matching core. This page uses an **Arcane Workbench** to show the complete process, from binding a target to requesting your first craft.

<a name="setup" />

## Prepare the equipment

Equipment: <ItemImage id="gtnotgood:wireless_packaged_pattern_provider" /> <ItemImage id="gtnotgood:packaged_wireless_connector" /> <ItemImage id="gtnotgood:arcane_workbench_packaged_core" />

- A Wireless Packaged Pattern Provider, an Item Wireless Connector, and an Arcane Workbench Packaged Core.
- A powered ME network with an available channel, storage, a Crafting CPU, and a terminal that can request crafts.
- An Arcane Workbench, a wand suitable for arcane crafting, and the recipe's required research, ingredients, and Vis.
- At least one Blank Pattern in your inventory and one empty pattern slot in the provider.

The scene below illustrates a possible layout. The ME Controller represents your existing network; power, storage, and the Crafting CPU are omitted. Device spacing is flexible, and the stone brick platform is not required.

<a name="layout" />

<GameScene width="350" height="220" interactive={true} showGrid={true}>
  <ImportStructure src="/assets/packaged-provider-layout.snbt" />
  <BlockAnnotation pos="0 1 1" color="#FF55BBDD" thickness="0.025">
    Connect to a powered ME network and give the provider a channel.
  </BlockAnnotation>
  <BlockAnnotation pos="1 1 1" color="#FFFFD166" thickness="0.025">
    Right-click the provider. Install an Arcane Workbench Packaged Core in the core slot on the right.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="0.025">
    Leave the wand in the workbench. Remove the example ingredients after recording the recipe.
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.6 1.5" text="Provider + Arcane Core" color="#FFFFD166" maxWidth="100" />
  <TextAnnotation pos="5.5 2.4 1.5" text="Workbench + wand" color="#FF88CC88" maxWidth="100" />
</GameScene>

Drag to rotate the scene, zoom, or inspect individual layers. Hover over a highlighted block to read its instructions. The scene illustrates the layout; perform the actual steps on devices in your world.

<a name="bind" />

## Bind the workbench

1. Connect the provider to ME, right-click to open its screen, and install an **Arcane Workbench Packaged Core**.
2. Hold the **Item Wireless Connector** and **right-click the provider without sneaking** to select it.
3. Keep holding the connector and **Shift-right-click the Arcane Workbench** to bind it.
4. Right-click air to clear the connector's selection. Targets already bound to the provider remain connected.

<a name="binding-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/packaged-provider-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FFFFD166" thickness="0.035" alwaysOnTop={true}>
    Hold the connector and right-click the provider without holding Shift.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="0.035" alwaysOnTop={true}>
    Hold the connector and Shift-right-click the target workbench.
  </BlockAnnotation>
  <LineAnnotation from="2 1.7 1.5" to="5 1.7 1.5" arrow="end" color="#FFFFD166" thickness="0.04">
    Wireless binding. No cable is required along this line.
  </LineAnnotation>
  <TextAnnotation pos="1.5 2.8 1.5" text="Select: right-click" color="#FFFFD166" maxWidth="100" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Bind: Shift-right-click" color="#FF88CC88" maxWidth="100" />
</GameScene>

The provider and workbench must be in the **same dimension, with their chunks loaded**. Wireless binding does not keep chunks loaded. The provider's owner must be online in the target's dimension and have permission to use it.

<a name="record" />

## Record an arcane recipe

1. Arrange one craft of an unlocked arcane recipe in the real Arcane Workbench. Insert a wand and confirm that the recipe matches.
2. Close the workbench screen, leaving the example ingredients and wand in place. Put a Blank Pattern in your inventory.
3. Open the provider's **Connected Targets** list and **left-click the workbench's entry**. The provider consumes one Blank Pattern and places the recorded processing pattern in an empty pattern slot.
4. Recording does not consume the example ingredients. After recording, clear the crafting grid and output slot. **Leave the wand installed and close the workbench screen.**
5. Store the actual ingredients in ME and request the output from a terminal.

Use the recorded pattern: it preserves the arcane recipe's crafting grid. Crafting patterns cannot dispatch these jobs, and an ordinary processing pattern containing only unordered inputs and outputs cannot replace the recorded arcane layout.

<a name="returns" />

## Vis and returned items

- The workbench's wand supplies Vis first. Use a wand that supports arcane crafting and has enough capacity to pay for one recipe.
- If the wand lacks Vis, Thaumic Energistics can supply the deficit from matching primal essentia stored in ME, according to the server configuration. For your first test, charge the wand with enough Vis beforehand.
- Arcane crafting outputs and returned container items enter the provider's **9 return slots**, then move into ME. This path does not depend on the Auto Return setting.
- If ME storage is full, items remain in the return slots. If those slots cannot hold the recipe's outputs and container items, that craft will not run.

Each provider has **720 pattern slots** and supports up to **1024 distinct targets**. Several workbenches can share a provider, but every target must match the installed core.

## Use a different machine

The provider requires a **machine-specific Packaged Core**. The Basic Packaged Core is only a crafting component for those cores; it cannot dispatch jobs to chests or arbitrary machines.

| Target | Core and main requirements |
| --- | --- |
| Arcane Workbench | Arcane Workbench Packaged Core; record the grid as described above and leave the wand installed |
| Crucible | Thaumcraft Crucible Packaged Core; meet the research and heat requirements and supply essentia |
| Infusion altar | Thaumcraft Infusion Packaged Core; activate the altar, empty the pedestals, and supply essentia |
| Blood Altar | Blood Altar Packaged Core; meet the altar tier and LP requirements, and encode only one recipe step per processing pattern |
| Assembly line | Assembly Line Packaged Core or Advanced Assembly Line Packaged Core, as appropriate; form and power the machine, supply recipe data, and use the required ordinary input and output buses and hatches |

Processing recipes with fluid inputs require **Ultimate Encoded Patterns**. Targets that retain their outputs, such as Blood Altars, infusion altars, and assembly lines, require **Auto Return** on the provider's left toolbar. It is disabled by default and collects only the expected outputs of jobs dispatched by this provider.

<a name="troubleshooting" />

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Binding fails | First right-click a loaded provider in this dimension, then Shift-right-click the target. Check permissions, duplicate targets, and the target limit |
| Target is bound but receives no job | Check the matching core, ME power and channel, a valid processing pattern, and the owner being online in the target's dimension |
| Arcane recipe cannot be recorded | Check research, the recipe on the table, a Blank Pattern in your inventory, and an empty provider pattern slot |
| Workbench is reported as occupied | Close its screen, empty the crafting grid and output slot, and leave the wand installed |
| Missing Vis or essentia | Check wand capacity and charge. For essentia supplementation, check Thaumic Energistics and the matching essentia in ME |
| Output does not return to ME | Check ME storage and the return slots. For altars or assembly lines, enable Auto Return |

In the Connected Targets list, right-click an idle target to unlink it. Active targets cannot be unlinked directly. Shift-right-click an entry to try interrupting and releasing its job, then inspect leftover materials and separately cancel the corresponding AE crafting request.
