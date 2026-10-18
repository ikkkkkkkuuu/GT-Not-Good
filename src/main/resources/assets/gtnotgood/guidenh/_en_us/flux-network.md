---
navigation:
  title: Flux Energy and Logistics
  parent: index.md
  icon: gtnotgood:flux_plug
  position: 40
  recommend: 10
  keywords:
    - Flux Plug
    - Flux Point
    - Flux Logistics Plug
    - Wireless EU
    - Team energy
    - Restocking
item_ids:
  - gtnotgood:flux_plug
  - gtnotgood:flux_point
  - gtnotgood:flux_logistics_plug
---

# Flux Energy and Logistics

Flux Plugs and Flux Points connect to the **GTNH wireless EU network**, using the placing player's GT team balance. Flux Logistics Plugs supply or collect items and fluids through a **named ME Bridge channel**.

- [Choosing the three blocks](#devices)
- [Store EU with a Plug, supply EU with a Point](#energy-setup)
- [Voltage, amperage and priority](#energy-settings)
- [Chunk loading and relocation](#chunk-loading)
- [Logistics: restocking and collection](#logistics-setup)
- [Troubleshooting](#troubleshooting)

Rotate and zoom the 3D previews, and hover over colored boxes for operating notes. Arrows show transfer direction and stone bricks are display stands. These static scenes do not run energy or ME networks. Energy connection arms appear when the blocks connect to GT ports in the actual world.

<a name="devices" />

## Choosing the three blocks

<a name="trio-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gtnotgood:flux_plug" x="1" y="1" z="1" />
  <Block id="gtnotgood:flux_point" x="3" y="1" z="1" />
  <Block id="gtnotgood:flux_logistics_plug" x="5" y="1" z="1" meta="0" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Flux Plug: receives EU packets from GT outputs and deposits them in the placing player's GT team account. Right-click to set input limits.
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    Flux Point: withdraws team EU and supplies adjacent GT inputs. Right-click to check output voltage and amperage.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Flux Logistics Plug: attach to a container, right-click to select an ME Bridge channel, then set stock targets or automatic collection.
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.7 1.5" text="Plug: EU deposit" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="3.5 0.4 1.5" text="Point: EU supply" color="#FFFFD166" maxWidth="150" />
  <TextAnnotation pos="5.5 2.7 1.5" text="Logistics: items / fluids" color="#FF88CC88" maxWidth="150" />
</GameScene>

| Block | Local attachment | Network |
| --- | --- | --- |
| Flux Plug | Output of a generator, dynamo hatch or GT cable | Placing player's GT team wireless EU account |
| Flux Point | Input of a GT machine, energy hatch or GT cable | Placing player's GT team wireless EU account |
| Flux Logistics Plug | Accessible face of a container or machine | Selected named ME Bridge channel |

<a name="energy-setup" />

## Store EU with a Plug, supply EU with a Point

<a name="energy-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/flux-energy-layout.snbt" />
  <BlockAnnotation pos="0 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    LV Battery Buffer: its energy output faces east, toward the Flux Plug. Actual operation requires a compatible charged battery.
  </BlockAnnotation>
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Flux Plug: accepts packets pushed by the source. Check voltage, amperage, Enable Transfer and Connect Network in Settings.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    Flux Point: emits packets at its configured voltage. Check team balance and the receiver and cable ratings first.
  </BlockAnnotation>
  <BlockAnnotation pos="6 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    LV Extruder: receives EU on its west face. Its front faces north and automatic output faces south. Actual recipes must still be satisfied.
  </BlockAnnotation>
  <LineAnnotation from="1.5 2.1 1.5" to="5.5 2.1 1.5" arrow="end" color="#FFFFD166" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="Output → Plug" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Point → Input" color="#FF88CC88" maxWidth="150" />
</GameScene>

1. Attach a **Flux Plug** to a GT output. A GT cable also works; make sure the cable is connected to the Plug.
2. Have the intended user place the Plug. It automatically uses that player's GT wireless account, settled against their current GT team.
3. Place a **Flux Point** at the consumer, touching an energy input or connected GT cable. Use the same player or another member of the same GT team to share the balance.
4. Right-click both devices. Under **Settings**, check voltage and amperage, and leave **Enable Transfer** and **Connect Network** enabled.
5. Check wireless balance and actual EU/t under **Network Statistics**. Use **Connections** to inspect adjacent GT devices on all six faces.

**Network Selection** shows the GTNH wireless account and its connection switch. Placement binds the account automatically; no named energy network or connector pairing is required. Cross-dimensional operation requires both endpoint chunks to remain loaded.

The Plug waits for a generator, dynamo hatch or GT cable to push energy into it. The Point supplies adjacent receivers; all its receiver faces share the same transfer limit.

<a name="energy-settings" />

## Voltage, amperage and priority

Only the **placing player** can edit an energy device. Its display name identifies the device; its account is determined by the placing player.

| GUI tab | Controls and information |
| --- | --- |
| Home | Name, priority, Surge Mode and Chunk Loading; actual transfer and buffer |
| Network Selection | GTNH wireless account and Connect Network switch |
| Connections | Adjacent GT energy devices on all six faces |
| Network Statistics | Team wireless balance, actual EU/t and voltage reminder |
| Settings | Voltage, amperage, Enable Transfer, Connect Network and Require Redstone |

Voltage is detected once on placement. Plugs use the highest readable adjacent GT output voltage; Points use the lowest readable GT input rating. Unknown ratings fall back to **32 V**. Both start at **1 A**. Replacing neighboring devices does not change saved settings: check them again manually.

- **Transfer limit = voltage × amperage EU/t.** For example, 32 V and 4 A allows up to 128 EU/t, subject to supply, balance, receiver capacity and cables.
- A Plug accepts packets up to its configured voltage, with total amperage per tick capped by its setting. Raising the voltage limit does not increase the power of lower-voltage packets.
- A Point emits complete packets at its **configured voltage**. It waits if the balance cannot fund one packet. Match the receiver rating: GT overvoltage and cable amperage limits still apply.
- Priority ranges from **-9999 to 9999**. Higher-priority Points withdraw first. **Surge Mode** takes precedence over ordinary priority; voltage and amperage still determine the transfer limit.
- With **Require Redstone** enabled, the device needs a redstone signal to transfer. Previously accepted EU is still settled when transfer is paused.

<a name="chunk-loading" />

## Chunk loading and relocation

The Home tab's **Chunk Loading** switch forces only **the energy device's own chunk**. Forge server quotas apply, and a failed request leaves the switch off. Generators, consumers and cables crossing chunk borders must also remain loaded. Load the Logistics Plug's area using the available chunk-loading system.

Breaking an energy device returns accepted buffered EU to its original wireless account; the dropped block carries no account balance. Configuration resets on removal. Placement binds the new placing player and detects voltage again at 1 A. Check settings before restarting the consumer.

<a name="logistics-setup" />

## Logistics: restocking and collection

<a name="logistics-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/flux-logistics-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    ME Bridge Sender: connect to the main ME network and right-click to set an available channel name. Select this name at the Logistics Plug.
  </BlockAnnotation>
  <BlockAnnotation pos="4 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    Flux Logistics Plug: place by clicking a face of the destination. Its arm points at the target. Right-click to select channel and direction.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Target container: supply checks its local stock; collection extracts resources allowed through the attachment face.
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="4.5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.7 1.5" text="Sender: named ME channel" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="4.5 0.4 1.5" text="Plug: select channel" color="#FFFFD166" maxWidth="150" />
  <TextAnnotation pos="5.5 2.7 1.5" text="Container: check face" color="#FF88CC88" maxWidth="150" />
</GameScene>

The arrow shows **ME → Container** supply. The Sender's cables, power and storage are omitted. Automatic collection returns resources to ME in the opposite direction.

1. Connect an **ME Bridge Sender** to the powered main ME network. Right-click, enter a channel name unused by another Sender, then press Enter or close the screen to save.
2. Hold the Logistics Plug and click an **accessible face** of the container or machine to place it. The Plug accesses only the adjacent container its arm points toward.
3. Right-click the Plug. Select a channel from the left directory, or enter and confirm the same name, then wait for **Online**. Only the placing player can edit it.
4. Click the direction button at the upper right to choose **ME → Container (filtered)** or **Container → ME (automatic)**.

The Logistics Plug joins the Sender's ME network wirelessly and requires **1 ME channel**. Both endpoints must stay loaded, with ME power, available channels and suitable storage.

Removal resets its channel, direction, samples and targets; actual pending cargo is preserved. Select the channel and configure supply or collection again after placing it.

### ME → Container: replenish target stock

There are **9 item and 9 fluid filters**. Click a sample slot while holding an item or fluid container, then enter its target quantity beside it. Samples are not consumed. Fluid quantities use **mB**.

- **Positive target:** replenish up to that stock. With a target of 64 and 20 already present, supply at most 44 more. A fluid target of 1000 means replenish to 1000 mB.
- **Blank or 0:** keep filling that resource until the container has no space or ME stock runs out.
- Only deficits are supplied; excess stock stays in the container. Each resource has one sample row. Marking it again removes its earlier sample.

Set the correct sample before entering its quantity: a quantity alone does not select a resource. Items match by type, metadata and NBT. Fluid containers must report current stock for positive targets to work. Missing ingredients wait for existing ME stock to become available; the Plug does not request crafting.

Coordinate targets and extraction when multiple Plugs or other pipes serve one container.

### Container → ME: automatic collection

Collects items and fluids extractable through the attachment face, **ignoring all filters and targets**. Before switching, check which ingredients the container also allows extracting.

Machine input/output faces, covers, slot access and tank capacities still apply. Transfers wait when ME stock is missing, storage is full or the target denies access. Restore network conditions after a disconnection.

<a name="troubleshooting" />

## Troubleshooting

| Symptom | Check |
| --- | --- |
| A Plug does not deposit EU | Check GT output direction, cable connection, input voltage limit, amperage and transfer switches. The source must push packets |
| A Point does not supply EU | Check team and balance, EU for a complete packet, the input face, receiver capacity and redstone conditions |
| Power is wrong after replacing a machine | Voltage detection runs only on placement. Set it manually and check machine and cable ratings |
| Surge Mode does not reach the expected EU/t | Surge changes withdrawal order. Check voltage × amperage, supply and receiver capacity |
| The GUI opens but settings cannot be edited | Only the placing player can configure it. A shared team balance grants no editing rights over another player's devices |
| Operation stops when leaving | Check both endpoints and devices across chunk borders. An energy block loads only its own chunk, subject to server quotas |
| A Logistics Plug is offline | Check the Sender's channel, endpoint loading, ME power and available channels |
| A Logistics Plug also removes ingredients | Automatic collection ignores filters and targets. Check direction and resources extractable from the attached face |
| Entering a target does not restock | Set a sample first, then check ME stock, container space and insertion rules on that face |

See [AE Buses, Interfaces and Terminals](ae-devices.md) for other ME Bridge, stock bus and interface uses, or [ME Hatches and Virtual Molds](me-hatches.md) for multiblock ME input, output and pattern devices.
