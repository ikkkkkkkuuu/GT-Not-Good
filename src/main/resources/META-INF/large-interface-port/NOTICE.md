# Large ME Dual Interface

The implementation links to the installed GTNH Applied Energistics 2 and AE2 Fluid
Crafting libraries. Native interface inventory, crafting, return, upgrade, wrench,
orientation and settings APIs remain supplied by those libraries.

- Applied Energistics 2 Unofficial: rv3-beta-1073-GTNH,
  commit `151550f6d558a663eee792f0bfa22e12a53c0e54`, LGPL-3.0-or-later.
  https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial
- AE2FluidCraft-Rework: 1.5.110-gtnh,
  commit `6e8dd013bd4ec123cf74daaecf581534d87f2391`, LGPL-3.0.
  https://github.com/GTNewHorizons/AE2FluidCraft-Rework

The block render layout and setting icon indices were adapted from these native
interfaces. Adapted source files are `client/largeinterface/LargeInterfaceRenderer.java`
and `common/blocks/largeinterface/LargeInterfaceGui.java` under
`src/main/java/com/xyp/gtnotgood/` and are distributed under LGPL-3.0-or-later.
Their changes provide an orange default render, a scrollable 900-slot pattern area,
MUI2 server-authoritative controls and no stock request configuration.

No upstream textures are copied into this archive. The orange interface textures
and AE setting icons are resolved from the user's installed dependencies at runtime
and remain covered by their upstream asset licenses. This project's MIT license
does not relicense those external resources. Dependency code is not embedded.

The GNU LGPL and GPL license texts are included in the existing
`META-INF/ae2lt-port/AE2-LGPL-3.0.txt` and
`META-INF/ae2lt-port/GPL-3.0.txt` resources.
