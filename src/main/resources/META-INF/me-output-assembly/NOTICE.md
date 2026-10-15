# ME output assembly texture provenance

The two 16x16 overlays combine the native ME output bus and fluid hatch textures
from GTNewHorizons/GT5-Unofficial `5.09.54.183`, commit
`f8453bce28793713d314f6dcfa5ac551d700efd6`.
Original authors: GT5-Unofficial contributors.
Upstream: https://github.com/GTNewHorizons/GT5-Unofficial/tree/5.09.54.183

The pinned upstream README and `LICENSE.txt` were inspected before adaptation.
The README grants LGPL-3.0-or-later; these derived textures retain that license
and are outside this project's general MIT grant. Full LGPL and incorporated
GPL terms are bundled beside this notice.

The original orange 2x2 item center is surrounded by a blue fluid ring and the
native orange outer rim, with original gray metal spacing and alpha preserved.
Both states use the same concentric geometry. Upstream active and inactive
orange/blue RGB values are identical, so the composite states match pixel for pixel.
`ASSET_MANIFEST.json` records all source/destination paths and hashes,
the pinned source artifact hash and the precise pixel changes.

Extracted references and preview images remain under ignored
`build/me-output-reference`, outside compilation and resource packaging.
