# Flux Networks port

Upstream: https://github.com/SonarSonic/Flux-Networks

## Code and block resources

The plug/point specialization, connector architecture and bounded buffer arithmetic are
adapted from `TileFluxPlug`, `TileFluxPoint`, `TileFluxConnector`, `ConnectionTransfer`,
`BaseTransferHandler` and `FluxTransferHandler` at revision
`0f013d8eaae70906722bba21b5452392b4ffdb8d` (1.12.2).
Copyright (c) 2018 Ollie Lansdell, MIT.

The six `textures/blocks/flux/flux_{plug,point}_{on,off,colour}.png` files and
`models/flux/{fluxplug,fluxpoint,fluxconnection}.json` are copied without changing
pixels or geometry from that revision's `assets/fluxnetworks/textures/model/`
and `assets/fluxnetworks/models/block/` respectively. Runtime rendering adapts
the original JSON UVs and rotations to the Minecraft 1.7.10 tessellator.

The Java port replaces SonarCore/FE/custom-network services with Minecraft 1.7.10,
GT EU packets and `gregtech.common.misc.WirelessNetworkManager`. GT's existing
owner/team ledger remains the sole source of stored wireless energy.

## Modern GUI

GUI code/layout reference: `GuiFluxDeviceHome`, `GuiTabCore`, `GuiFluxCore`,
`GuiFocusable`, `FluxEditBox`, `NavigationButton`, and `SwitchButton`,
revision `20a3d8b2ffe9bcd489afec2e58af5849276473ae` (1.20 branch).
Code copyright (c) 2017-2021 SonarSonic, BloCamLimb; MIT.

The modern GUI images `textures/gui/flux/gui_{background,frame,icon}.png` are
copied unchanged from FluxNetworks-1.20.1-7.2.1.15.jar, CurseForge file 5234697:
https://www.curseforge.com/minecraft/mc-mods/flux-networks/files/5234697
Corresponding `.png.mcmeta` files are copied from the 1.20 source tree.

GUI designs and GUI resources: copyright (c) 2019-2021 BloCamLimb,
Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International,
as declared by the upstream README. This license also covers the adapted GUI
design in `FluxConnectorGui`. Attribution, noncommercial use and share-alike
conditions apply. https://creativecommons.org/licenses/by-nc-sa/4.0/

Changes: LDLib texture rendering plus MUI2 synchronized controls for 1.7.10;
Home retains name/priority and surge/chunk switches. The separate transfer-limit
field and bypass switch are removed; throughput is configured voltage times amperage.
Voltage/amperage, enable and redstone controls are on a settings page.
Network selection shows the existing GTNH owner account. Connections show
adjacent GT EU endpoints. Statistics show GTNH balance and actual EU/t.
Independent Flux network creation, player charging and custom member management
are omitted because this port uses GTNH's existing wireless owner/team network.

Bundled license texts:
- `META-INF/licenses/Flux-Networks-MIT.txt`
- `META-INF/licenses/Flux-Networks-Modern-MIT.txt`
- `META-INF/licenses/Flux-Networks-GUI-CC-BY-NC-SA-4.0.txt`
