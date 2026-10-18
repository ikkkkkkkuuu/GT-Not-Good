---
navigation:
  title: AE Buses, Interfaces and Terminals
  parent: index.md
  icon: gtnotgood:advanced_io_bus
  position: 30
  recommend: 10
  keywords:
    - advanced IO bus
    - threshold export
    - level emitter
    - stock IO interface
    - large dual interface
    - wireless terminal
    - requester
    - wildcard pattern
    - ME bridge
item_ids:
  - gtnotgood:advanced_io_bus
  - gtnotgood:threshold_export_bus
  - gtnotgood:threshold_level_emitter
  - gtnotgood:stock_io_interface
  - gtnotgood:stock_io_interface_part
  - gtnotgood:large_interface
  - gtnotgood:large_interface_part
  - gtnotgood:requester_terminal
  - gtnotgood:me_requester
  - gtnotgood:wireless_dual_interface_terminal
  - gtnotgood:WildcardPattern
  - gtnotgood:pattern_sorter
  - gtnotgood:me_container
  - gtnotgood:blockMEBridgeSender
  - gtnotgood:blockMEBridgeReceiver
  - gtnotgood:MEWirelessTransceiver
  - gtnotgood:wireless_frequency_card
---

# AE Buses, Interfaces and Terminals

Use this mod's AE buses, interfaces and terminals for stock control, pattern management, restocking and wireless connections. Choose the device for the job, then configure its stock target, patterns or network source.

- [Choose buses and interfaces](#io-buses)
- [Advanced IO Bus](#advanced-io-bus)
- [Threshold Export Bus](#threshold-export-bus)
- [Threshold Level Emitter](#threshold-level-emitter)
- [Stock IO Interface](#stock-io-interface)
- [Large Dual Interface](#large-dual-interface)
- [Requesters and requester terminals](#requesters)
- [Wireless Dual Interface Terminal](#wireless-terminal)
- [Wildcard Pattern](#wildcard-pattern)
- [Pattern Sorter](#pattern-sorter)
- [ME Container](#me-container)
- [ME Bridges](#me-bridge)
- [ME Wireless Transceiver](#me-wireless-transceiver)
- [Wireless Frequency Card](#wireless-frequency)
- [Flux Logistics Plug](#flux-logistics)
- [Troubleshooting](#troubleshooting)

Rotate and zoom the following 3D previews, and hover over colored outlines for operation notes. They identify devices and orientations rather than simulate a running ME network. Actual use requires power, channels, cables, storage and the machine's normal conditions.

### Three cable-mounted control parts

<a name="buses-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/ae-buses-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Advanced IO Bus: maintains stock inside its adjacent container and recovers unmarked products to ME. Middle-click a sample to set its target.
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Threshold Export Bus: exports according to ME network stock. Middle-click a sample to configure that network threshold.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    Threshold Level Emitter: controls redstone using upper and lower stock thresholds, retaining state between them. Right-click to configure.
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.7 1.5" text="Machine stock: IO" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="3.5 0.4 1.5" text="ME stock: threshold" color="#FF88CC88" maxWidth="150" />
  <TextAnnotation pos="5.5 2.7 1.5" text="Thresholds: redstone" color="#FFFFD166" maxWidth="150" />
</GameScene>

Target containers are omitted. Buses and emitters act outward through their mounted face; install them directly against the target device.

<a name="io-buses" />

## Choose the right bus or interface

All devices below need ME power and one available channel. Cable-mounted parts act on the block directly in front of their mounted face. Check the machine's item and fluid access settings on that face.

| Device | Main use |
| --- | --- |
| ME Advanced IO Bus | Restock marked ingredients in an adjacent machine and return other resources |
| ME Threshold Export Bus | Export to an adjacent container according to ME network stock |
| ME Threshold Level Emitter | Control redstone using upper and lower ME stock thresholds |
| ME Stock IO Interface | Let supported GT singleblocks use marked ME ingredients and fuels directly |
| ME Large Dual Interface | Store many patterns, dispatch processing jobs, and accept returning items and fluids |

<a name="advanced-io-bus" />

## ME Advanced IO Bus: maintain machine stock

1. Mount the bus on cable facing the target machine or container, then right-click to configure it.
2. Set item or filled fluid-container samples. Samples are not consumed; a filled container marks its fluid.
3. **Middle-click a sample**, enter the amount to keep in the machine, and press Set or Enter. Items use counts; fluids use the displayed L unit.
4. Store the actual resources in ME and allow insertion through the target face. Returning products also requires extraction access through that face.

The bus fills marked resources from ME and always returns **unmarked resources** to ME. **Return stock above the target to ME** is enabled by default and controls excess marked stock. Turning it off still returns unmarked resources.

For example, mark Iron Ingots with a target of 64: fewer than 64 are replenished, while excess stock follows the recovery setting. Unmarked products return to ME. The target refers to **stock inside the machine**, rather than a network reserve.

There are **18 sample slots** initially. Each Capacity Card adds 9, up to **63**. The 8 upgrade slots accept acceleration, capacity, and redstone cards. A Redstone Card enables ignored, high-signal, or low-signal operation. Transfers obey the connected face's access rules; the bus does not configure the machine's circuit.

<a name="threshold-export-bus" />

## ME Threshold Export Bus: export within a stock condition

1. Face the bus toward the receiving container, then right-click and set an item or fluid sample.
2. **Middle-click the sample** to enter a threshold, then press Set or Enter.
3. Select the export mode on the left:

| Mode | Behavior |
| --- | --- |
| Export only ME stock above the threshold | With 1000 stored and a threshold of 800, exports at most the surplus 200 |
| Export while ME stock is at or below the threshold | Exports while stock is less than or equal to the threshold; pauses above it |

The threshold measures **ME network stock**, rather than restocking the destination to a target. The lower-stock mode can continue draining the remaining ME stock. It does not request crafting.

There are **9 sample slots** initially. Each Capacity Card adds 18, up to **63**. The 8 upgrade slots accept acceleration, capacity, and redstone cards. Samples match an exact item or fluid type. Removing a Capacity Card suspends rows beyond the remaining capacity.

<a name="threshold-level-emitter" />

## ME Threshold Level Emitter: add a stock control range

1. Mount the emitter on cable, right-click, and mark **one** item or fluid.
2. Enter the Lower threshold and Upper threshold, with the lower no greater than the upper.
3. Select low-stock or high-stock signaling on the left, facing the device that needs redstone control.

- **Low-stock signaling:** turns on below the lower threshold and off upon reaching the upper threshold.
- **High-stock signaling:** turns on upon reaching the upper threshold and off below the lower threshold.
- Between thresholds, the previous state is retained. The active signal strength is 15. Missing power, a channel, a sample, or a valid threshold range disables the signal.

For example, choose a lower threshold of 100, an upper threshold of 200, and low-stock signaling. Restocking starts below 100 and stops at 200. Falling back to 100–199 keeps it stopped. The emitter supplies redstone control; connect the actual restocking device separately.

### Direct stock supply and pattern dispatch

<a name="interfaces-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gtnotgood:stock_io_interface" x="1" y="1" z="1" meta="5" nbt="{stockIOTargetSide:5}" />
  <Block id="gregtech:gt.blockmachines:281" x="2" y="1" z="1" nbt="{mFacing:2s}" />
  <Block id="gtnotgood:large_interface" x="5" y="1" z="1" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Stock IO Interface: select the target direction in its GUI and mark items and fluids. Supported GT singleblocks use those ME ingredients directly.
  </BlockAnnotation>
  <BlockAnnotation pos="2 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    LV Extruder example: the interface touches its west face. The machine still needs power, the correct circuit and mold, and its normal recipe conditions.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Large Dual Interface: holds 900 patterns and dispatches items and fluids. It does not maintain marked stock.
  </BlockAnnotation>
  <LineAnnotation from="1.7 2.1 1.5" to="2.5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="Stock IO: direct supply" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Large: pattern dispatch" color="#FF88CC88" maxWidth="150" />
</GameScene>

The blue Stock IO Interface touches the LV Extruder, with east selected as its target. Confirm the target in the GUI. The orange Large Dual Interface dispatches pattern jobs; provide a route returning products to ME.

<a name="stock-io-interface" />

## Stock IO Interface: use ingredients directly from ME

The full block and cable-mounted part have the same function. Supported **GT single-block processing machines and fuel generators** can read marked ME resources directly. The interface does not prefill their inventories or dispatch processing-pattern jobs.

1. Place the interface against the machine and connect it to ME. For the full block, click the direction button until Target shows the correct machine. The cable-mounted part works toward its mounted face.
2. Open Items or Fluids and mark recipe ingredients or fuels. Each page has **900 marks**, and samples are not consumed.
3. **Right-click a sample** to set Keep in ME and Per check. Enable Reserve or Fixed as needed, and leave Enabled on.
4. Store the ingredients and configure the machine's circuit, power, and output normally. It runs its native recipes and extracts only what it actually consumes from ME.

| Setting | Effect |
| --- | --- |
| Reserve | Keeps the configured reserve in ME and offers only the surplus |
| Fixed | Offers the configured amount for each recipe or fuel check; waits until that whole amount is available |
| Recycle | Returns the target GT machine's item and fluid outputs that its connected face permits extracting |

For example, set Keep in ME to 1000 and Per check to 64, with Reserve and Fixed enabled. At least 1064 must be stored before 64 are offered for a check. A recipe consuming only 1 still removes only 1.

Automatic marking can fill empty marks from existing ME resources. Right-click its button to configure the refresh interval and minimum stock. Disable automatic marking on a page before editing its marks manually. Native voltage, cleanroom, and other recipe requirements still apply. If products do not return, check the output face, covers, and available ME storage.

### Cable-mounted interface variants

<a name="interface-parts-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/ae-interface-parts-layout.snbt" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Stock IO Interface part: functions like the full block, with the target fixed to the mounted face.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    Large Dual Interface part: holds 900 patterns like the full block and dispatches only through its mounted face.
  </BlockAnnotation>
  <TextAnnotation pos="1.5 2.8 1.5" text="Stock IO part" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Large interface part" color="#FFFFD166" maxWidth="150" />
</GameScene>

Both interfaces have full-block and cable-mounted variants. A part works outward through its mounted face. Right-click it normally to open the corresponding GUI.

<a name="large-dual-interface" />

## ME Large Dual Interface: manage 900 patterns

Both the full block and cable-mounted part store **900 patterns** in a scrollable 9-column grid. Patterns can also be managed from an Interface Terminal.

1. Connect the interface to ME and direct its output toward the target machine. The cable-mounted part dispatches only through its mounted face. Use a wrench to adjust a full block's output direction.
2. Right-click and insert processing patterns. Processing recipes containing fluids require **Ultimate Encoded Patterns**.
3. Request the output from a terminal. Route the machine's item and fluid products into the interface, or provide another return path to ME.
4. Adjust blocking, pattern optimization, and other settings on the left as needed. Upgrade features such as crafting locks require their corresponding cards.

This variant keeps the native Dual Interface's pattern dispatch and product return functions. It **does not maintain marked stock**. Use the Stock IO Interface or Advanced IO Bus for stock supply.

The multiplier button edits all eligible processing patterns in the interface: left-click doubles, right-click halves, and Shift applies larger changes, subject to quantity limits and exact divisibility. Crafting patterns are skipped.

### Configure automatic restocking centrally

<a name="requester-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <ImportStructure src="/assets/ae-requester-terminal-layout.snbt" />
  <Block id="appliedenergistics2:tile.BlockController" x="3" y="1" z="1" />
  <Block id="gtnotgood:me_requester" x="5" y="1" z="1" meta="2" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    Requester Terminal: right-click normally to manage loaded requesters on its network. This is not a wireless handheld terminal.
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    ME network illustration. Restocking needs actual storage, available patterns, processing machines and crafting CPUs.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    Requester: right-click to configure up to five stock targets. Batch zero requests the calculated shortage.
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="5 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.8 1.5" text="Terminal: configure" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="5.5 2.8 1.5" text="Requester: targets" color="#FF88CC88" maxWidth="150" />
</GameScene>

The requester terminal configures requesters on its own network; requesters place the orders. The arrow indicates management, and actual ME cables are required.

<a name="requesters" />

## ME Requester and ME Requester Terminal

<ItemImage id="gtnotgood:me_requester" /> The **ME Requester** places crafting orders to maintain stock. <ItemImage id="gtnotgood:requester_terminal" /> The **ME Requester Terminal** monitors and edits loaded requesters on the same network. The requester terminal is a cable part, not a wireless handheld terminal.

### Configure a stock target

1. Place a requester and connect ME. Each requester needs network power and one channel. The product requires an available pattern and the necessary machines, plus a crafting CPU able to accept its order.
2. Right-click the requester and mark items or fluids in up to **5 rows**. Marks do not consume samples; a corresponding fluid container can set a fluid mark.
3. Enter the target and batch amounts, then press Enter or click the apply button on that row. **Batch 0** automatically requests the current shortage. A positive batch value caps each order at that amount. Both fields accept integers from 0 through 2^63 − 1; fluid quantities use the displayed L unit.
4. Enable the row's checkbox. A zero target or disabled row stops new restocking orders for that row.

The shortage is the target minus ME stock and quantities already crafting or being calculated. Finished products return to ME storage, not a local requester inventory. Batch size limits one order; it is neither a maximum stock level nor a minimum amount that must be missing before crafting starts.

Requesters share at most **2 simultaneous recipe calculations per network**. This does not limit executing crafting jobs to two. Hover the status bar for stored and pending amounts or states such as missing materials, no pattern, waiting for a CPU or offline. Disabling a row, changing a target or removing a requester does not automatically cancel orders already submitted to crafting CPUs; cancel those through crafting status when necessary.

### Manage requesters centrally

Install the requester terminal on a cable, bring its network online and right-click normally. Search requester names or marked item names, scroll through rows and edit samples, targets, batches or enabled states directly. Named requesters are grouped by name; a renamed requester item passes its name to the placed block.

Only valid, loaded requesters on the same network are listed. Remote editing also requires network build permission. The terminal does not load remote chunks or list devices on another independent ME network. Right-click a sample slot with an empty cursor, or Shift-click it, to clear the mark.

<a name="wireless-terminal" />

## ME Wireless Dual Interface Terminal

<ItemImage id="gtnotgood:wireless_dual_interface_terminal" />

Access ME storage, interface patterns and pattern encoding in one screen. Storage is on the left, interfaces in the middle and the encoder on the right. Its crafting-terminal button switches to a wireless crafting view; switch back to continue editing. These crafting, encoding and interface views belong to the same handheld terminal.

### Link and open

1. Open an ME Security Terminal, insert the wireless terminal in its upper-right linking slot, then retrieve it from the slot below and charge it.
2. Right-click within the range of an active wireless access point on that network and in the same dimension. The network must function normally; storage access and crafting still depend on permissions and available resources.
3. The default **Open Dual Interface Terminal** key is **F**; **Send Held Item to Network** defaults to **C**. Change them in Controls as needed. The terminal can be carried in the inventory or a bauble slot.

Installing a supported Quantum Bridge Card or Infinity Booster Card through its NEI recipe removes range and dimension restrictions. An Energy Card removes the terminal's own power drain. These upgrades are independent and do not supply missing network storage space, ingredients or crafting CPUs.

### From an NEI recipe to an interface pattern

1. Place blank patterns in the encoder's blank-pattern slot. Use NEI recipe transfer, then verify ingredients, outputs and amounts.
2. Crafting recipes use a shaped 3×3 layout. Processing patterns use a scrollable 4×4 view, supporting up to 256 input entries and 256 output entries. Select crafting or processing mode as appropriate.
3. Processing import offers controls for combining matching entries, prioritizing fluids and keeping circuits and virtual molds. The last option imports supported non-consumed circuits and molds as one item; it does not retain every catalyst.
4. Click Encode to create a pattern. **Shift-click Encode** encodes and moves it into your inventory. **Ctrl + Shift-click Encode** encodes the entire stack of blanks with the same recipe.
5. For direct installation, first check that the middle panel highlights a suitable empty pattern slot, then **Alt-click Encode**. The target must still be empty and accept the pattern; keep the cursor empty. Normal clicks exchange interface patterns, and Shift-click moves them into your inventory.

For recipes with interchangeable ingredients, choose the required variant before encoding; Shift + mouse wheel cycles supported recipe ingredient alternatives. Processing recipes containing fluids use the current version's Ultimate Encoded Pattern. Check fluid identities and amounts before installing it.

The interface list retains search and filtering. **Ctrl-click an interface entry's option button** to attempt teleportation beside that interface; it fails if no safe standing position is found. **Alt-click that option button** toggles the interface's terminal visibility. These actions target interfaces actually tracked by the open terminal.

<a name="wildcard-pattern" />

## Wildcard Pattern: expand processing patterns by material

<ItemImage id="gtnotgood:WildcardPattern" />

One Wildcard Pattern can provide many concrete processing patterns using material rules. It suits a shared process applied to many materials; it does not complete arbitrary machine recipes or create ingredients.

1. Hold it and right-click to configure it. The left tabs select preview, IN inputs, OUT outputs and material filters.
2. Add components on the IN and OUT pages: **Single** fixes an item or fluid; **Tag** selects a material prefix such as `ingot` or `plate`; **Fluid** selects molten, liquid, gas or plasma forms of the current material. Each side supports up to 6 components, with amounts from 1 through 2^31 − 1.
3. Add name, property or flag filters and choose whitelist or blacklist behavior. Up to 64 filters are supported, and all conditions must pass. Name filters support `*` and `?`. Start with a small material selection to avoid exposing unwanted processes.
4. Click **Save**, then inspect the preview count and each material's actual inputs and outputs. The preview supports search; its exclude button blacklists the current material and saves.
5. Install the configured template in an ME interface or pattern input device supporting expansion. The template occupies one physical pattern slot while the network sees its concrete processing recipes.

A material is skipped if any required item form or fluid is unavailable. Expansion proves that its components can be generated; it does not verify the target machine's recipe, amounts or voltage. Check NEI before installation. Use a normal crafting pattern for shaped crafting-table recipes.

<a name="pattern-sorter" />

## Pattern Sorter: organize existing inventory patterns

<ItemImage id="gtnotgood:pattern_sorter" />

1. Put the patterns in your ordinary inventory or hotbar, then hold the tool and right-click.
2. Select **Machine / recipe map** and inspect the circuit/mold groups and their pattern previews.
3. Empty the cursor and choose **Sort all groups**, or select a group and use **Bring this group to front**. Preview numbers follow main inventory order, with the hotbar last.

The tool exchanges only slots already holding patterns. Counts and pattern data are preserved; other items and empty slots stay in place. It has no internal pattern inventory. Patterns without a unique circuit/mold match appear in ambiguous, unmatched/unsupported, crafting-table or invalid/blank groups instead of having their recipes guessed and rewritten. It does not sort patterns inside ME interfaces and requires no ME connection.

### Bridges and network stock access

<a name="network-preview" />

<GameScene width="350" height="220" interactive={true}>
  <ImportStructure src="/assets/me-hatch-platform.snbt" />
  <Block id="gtnotgood:blockMEBridgeSender" x="1" y="1" z="1" />
  <Block id="gtnotgood:blockMEBridgeReceiver" x="3" y="1" z="1" />
  <Block id="gtnotgood:me_container" x="5" y="1" z="1" />
  <BlockAnnotation pos="1 1 1" color="#FF55BBDD" thickness="1" alwaysOnTop={true}>
    ME Bridge Sender: connect to the main ME network and right-click to set its channel name.
  </BlockAnnotation>
  <BlockAnnotation pos="3 1 1" color="#FFFFD166" thickness="1" alwaysOnTop={true}>
    ME Bridge Receiver: click C on the desired directory row to connect. T attempts teleportation.
  </BlockAnnotation>
  <BlockAnnotation pos="5 1 1" color="#FF88CC88" thickness="1" alwaysOnTop={true}>
    ME Container: right-click to mark items and fluids, with 36 samples of each. Access it with hoppers or pipes.
  </BlockAnnotation>
  <LineAnnotation from="2 2.1 1.5" to="3 2.1 1.5" arrow="end" color="#FF55BBDD" thickness="2" alwaysOnTop={true} />
  <TextAnnotation pos="1.5 2.7 1.5" text="Sender: name channel" color="#FF55BBDD" maxWidth="150" />
  <TextAnnotation pos="3.5 0.4 1.5" text="Receiver: C connects" color="#FFFFD166" maxWidth="150" />
  <TextAnnotation pos="5.5 2.7 1.5" text="Container: mark stock" color="#FF88CC88" maxWidth="150" />
</GameScene>

A sender and receiver establish a wireless bridge using a channel name. Local devices still need cables from the receiver. The ME Container is a storage access point, not a wireless linking tool.

<a name="me-container" />

## ME Container: expose network stock to hoppers and pipes

<ItemImage id="gtnotgood:me_container" />

The ME Container supplies selected items and fluids and accepts external resources into ME. Cable it to a powered network with an available channel. It requires **1 channel**; all faces support cable connections and resource transfer.

1. Right-click its GUI and select the item or fluid page. Each has **36 sample slots**.
2. Set samples on the left by dragging from NEI or clicking with an item or filled fluid container. Samples are not consumed. Left-click with an empty cursor to clear.
3. Extract selected resources using hoppers or pipes. The right grid displays ME network stock; hover for exact quantities. Fluids use mB.

Each item mark prefetches up to one real stack, respecting that item's stack limit. After changing a mark, the previous stack remains extractable; the new item refills after it is removed. Fluids are withdrawn directly from ME when requested, without reserving a local tank. Losing power, connection or its channel prevents further network withdrawals.

External deposits are not restricted by extraction samples. Items rejected by ME remain in the ingress buffer; fluid insertion accepts only what ME can store. A block harvested in survival retains its samples and actual buffered resources, not the whole network inventory.

<a name="me-bridge" />

## ME Bridges: share a network by channel name

<ItemImage id="gtnotgood:blockMEBridgeSender" /> <ItemImage id="gtnotgood:blockMEBridgeReceiver" />

1. Connect a **Sender** to the main ME network. Right-click it, enter a name not already used by another sender, and press Enter or close the screen to save.
2. Place a **Receiver** at the remote site and connect its local devices. Open its directory and click **C** on the desired channel's row, or enter the same name on the right and save.
3. Wait for the connection to settle and check its status, device power and channels. Use **Clear** to disconnect the receiver.

One sender supports multiple receivers, including across dimensions. Keep both endpoint chunks loaded. Names are server-wide and limited to **64 characters**. Bridges merge ME networks; they do not create extra channels. Their dense-capacity nodes, attached cables and devices still obey AE capacity and network rules. Avoid merging two independently controlled networks indiscriminately.

The directory's **T** button attempts to teleport you to that sender. Its dimension must be loaded and a safe arrival position must exist. It does not connect the receiver.

<a name="me-wireless-transceiver" />

## ME Wireless Transceiver: bind an ME node directly

<ItemImage id="gtnotgood:MEWirelessTransceiver" />

This tool uses a **Bridge Sender's named channel** to connect a supported ME device node directly to that sender's network.

| Action | Result |
| --- | --- |
| Right-click air | Open the directory and choose a sender channel |
| Shift + right-click an ME node | Save and establish its wireless link |
| Repeat on that node with the same channel selected | Disconnect its wireless link |
| Select another channel, then Shift + right-click | Move that node to the new channel |
| Right-click an ME node normally | Inspect its wireless binding instead of opening the device GUI |

The sender and target must be loaded and expose usable ME nodes. Saved links wait for temporarily unavailable endpoints. This cannot add an ME interface to an ordinary chest, tank or machine without an ME node. Power, channels and AE security rules still apply.

While holding the transceiver, nearby bound nodes are marked in their channel colors. Set the channel color in its sender's GUI.

<a name="wireless-frequency" />

## Wireless Frequency Card: link an entire device cluster

<ItemImage id="gtnotgood:wireless_frequency_card" />

The frequency card binds an **ME Controller** as its source. It does not use a bridge's named channels.

1. Wait for a powered, valid ordinary ME Controller network to finish booting. Hold the card and **Shift + right-click the controller** to save and select it.
2. At the destination, **right-click an ME device or cable part normally**. This connects its whole cluster joined by physical cables. Aim at the part itself; a facade is not a node.
3. Right-click any node in that cluster again to disconnect this frequency's links to it. Do not use another controller network as the destination cluster.
4. Right-click air to open the manager. Switch saved sources, inspect links and channel allocation, pause links or enable auto-connect there.

A card saves up to **64 sources** and can be operated only by its bound player. Shift + right-clicking air removes the selected saved source, **leaving existing wireless links intact** and retaining other saved sources.

With auto-connect enabled, carry the card in your inventory or bauble slots; newly placed supported ME devices and parts attempt to join the selected source. If several cards are usable, hold the intended card or disable auto-connect on the others. Manual linking still requires holding the card.

Links can cross dimensions, but both endpoint chunks must remain loaded. Missing power, invalid controllers, AE permissions or frequency conflicts can prevent service. If physical cables join clusters assigned to different frequencies, separate them and inspect their bindings.

Wireless entrances share their source controller's channel budget with wired devices. For example, one isolated controller has a geometric budget of **6 × 32 = 192 channels**, shared across the network rather than granted to each receiver. Check the manager's actual allocations and waiting states.

Use the manager to select a world network view, or **Shift + scroll** while holding the card. Views display while holding or wearing it. The auto-connect, manager and network-view hotkeys are unbound by default; assign them in Controls if desired.

<a name="flux-logistics" />

## Flux Logistics Plug: restock or collect through a bridge channel

<ItemImage id="gtnotgood:flux_logistics_plug" />

Attach the Plug to an accessible container or machine face, then right-click to select an existing **Bridge Sender channel**. Supply target stock or switch to automatic collection. It connects wirelessly, requires **1 channel**, and needs both endpoints loaded.

See [Flux Energy and Logistics](flux-network.md#logistics-setup) for the full 3D installation example, filter slots and target controls. Hovering over the Logistics Plug and holding G also opens that chapter directly.

<a name="troubleshooting" />

## Troubleshooting

| Symptom | Check |
| --- | --- |
| A connected device does not work | Check power, channels, its mounted face, target access, and the machine's input/output settings |
| Advanced IO removes ingredients from the machine | Mark the ingredients: unmarked stock is recovered. The excess-return switch controls only surplus marked stock |
| A Threshold Export Bus does not restock the destination to its setting | Its threshold measures ME stock. Use an Advanced IO Bus to maintain destination stock |
| Stock IO does not fill a chest or vanilla furnace | Direct recipe access supports compatible GT singleblocks. Use Advanced IO or a Flux Logistics Plug to restock ordinary containers |
| A Large Interface holds patterns but products do not return | Check the machine, processing recipe, Ultimate Encoded Pattern for fluids, and the product return path |
| A requester waits for materials, a CPU, or a network | Check patterns, ingredients, CPUs, network state and loaded chunks. Changing targets does not cancel submitted CPU jobs |
| The wireless terminal will not open | Check linking, charge, access-point range, dimension and permissions. Range and energy upgrades have separate effects |
| Wireless connections use the wrong network | Distinguish bridge channel names from a frequency card's controller source, then inspect the selected source and target cluster |

For GT multiblock pattern hatches, ME input/output hatches and virtual molds, see [ME Hatches and Virtual Molds](me-hatches.md). For wireless pattern dispatch to special machines, see the [Wireless Packaged Pattern Provider](packaged-pattern-provider.md).
