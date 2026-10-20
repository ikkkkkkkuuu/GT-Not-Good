# Basic Industrial Apiary

- Source: GTNewHorizons/GT5-Unofficial, tag `5.09.54.183`, commit
  `f8453bce28793713d314f6dcfa5ac551d700efd6`.
- Source file: `src/main/java/gregtech/common/tileentities/machines/basic/MTEIndustrialApiary.java`.
- Authors: kuba6000 and GTNH contributors. License: LGPL-3.0-or-later.
  The accompanying `LICENSE-LGPL-3.0.txt` and `LICENSE-GPL-3.0.txt` retain the license texts.
- Destination: `com/xyp/gtnotgood/common/machines/basic/BasicIndustrialApiary.java`.
- Changes: independent LV registration and recipe; fixed 32 EU/t with upgrades;
  baseline 64x acceleration and 16x product quantities; built-in offspring recycling;
  bypassed flower, climate, day/night, weather, sky and specialty environment checks;
  54 output slots; persisted pending and overflow output counts; per-tick automatic
  output; expanded GUI with accurate power/speed information; bounded new-cycle
  admission during output congestion; preserved buffered items when dismantled.
- `ApiaryOutputBuffer` is independently authored integration code. It stores item
  quantities separately from Minecraft's byte-sized stack NBT and retains item NBT.
- Industrial Apiary textures, overlays and GUI icons are referenced from the separately
  installed GregTech dependency. No upstream image assets are copied or packaged.
- Extracted reference sources are under ignored `build/basic-apiary-reference/`, outside
  all Java and resource source sets.

This adapted source retains its LGPL terms rather than the project's general MIT grant.
