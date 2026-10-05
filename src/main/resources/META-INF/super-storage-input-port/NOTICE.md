# Super Advanced Stocking Input Hatch (ME)

Reference: GTNewHorizons/GT5-Unofficial, tag `5.09.54.183`.
Sources artifact SHA-1: `b0f3eb42604cfc3ad88d56c50d0dbfa375d89419`.
Upstream license: GNU LGPL version 3, inspected before adaptation.
Source repository: https://github.com/GTNewHorizons/GT5-Unofficial/tree/5.09.54.183

Adapted source:
- `gregtech/common/tileentities/machines/MTEHatchInputME.java`
- `gregtech/common/gui/modularui/hatch/MTEHatchInputMEGui.java`
- `gregtech/common/tileentities/machines/MTEHatchInputBusME.java`
- `gregtech/common/gui/modularui/hatch/MTEHatchInputBusMEGui.java`

Destinations:
- `common/machines/hatch/me/SuperAdvancedMEInputHatch.java`
- `common/machines/hatch/me/SuperAdvancedMEInputHatchGui.java`
- `common/machines/hatch/me/SuperAdvancedMEInputBus.java`
- `common/machines/hatch/me/SuperAdvancedMEInputBusGui.java`

Changes: independent registered subclass, 900 fluid marks with scrollable grids,
per-fluid network reserves and fixed available quantities, configuration
persistence in world saves and data-stick copies, stackable drops that clear
configuration, and server-authoritative mode switches in the native center
column beneath the arrow. Fluids remain in ME until recipe consumption commits.
Native ME connection, channel,
security, controller transaction, and GT texture APIs are retained.
The item bus applies the same 900-mark policies to item counts and retains native
circuit and manual slots, moved beside the status row to leave room for the mode buttons.
Item identity includes metadata and NBT. Only consumed recipe quantities leave ME.

These adapted sources remain under LGPL-3.0 and are excluded from the project's
general MIT grant. Upstream GUI/block textures are referenced from the installed
GregTech mod; no artwork is copied into this addon. The extracted reference
sources under `build/storage-input-reference` are not compiled or packaged.
