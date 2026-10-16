# Decorative panel casing connected textures

These 235 native 16x16 connected-texture variants derive from five casing PNGs in
GTNewHorizons/GT5-Unofficial `5.09.54.183`, commit
`f8453bce28793713d314f6dcfa5ac551d700efd6`.
Original authors: GT5-Unofficial contributors. The pinned upstream README and
`LICENSE.txt` were inspected before adaptation. The README credits texture
contributors collectively; it does not assign these five textures individually.

Upstream: https://github.com/GTNewHorizons/GT5-Unofficial/tree/5.09.54.183
These assets retain LGPL-3.0-or-later outside this project's general MIT grant.
The full LGPL and incorporated GPL terms are bundled beside this notice.

`scripts/generate-stellar-panel-ctm.py` changes only the native 1-pixel perimeter:
joined edges extend the existing circuit, blue field, silver plate or mirror-grid
material; exposed frames and concave corner caps remain. Central patterns,
colours and internal grid lines are unchanged. Isolated mask 0 copies the source
bytes exactly. `ASSET_MANIFEST.json` records every source URL, version, hash,
destination, modification and the generator hash.

These backgrounds connect only between this addon's panels sharing a material.
The original GregTech casing blocks and renderer are not changed. Each panel's
front artwork, animation and glow remain separate. Its inventory icon retains the
original native casing. No MyCTMLib, additional mod or resource pack is required.

Source extraction and previews remain under ignored build directories outside
compilation and resource packaging.
