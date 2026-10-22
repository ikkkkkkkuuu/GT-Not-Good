# Dimensional Focus Engraving Array dynamic core

The dynamic core uses the machine-specific renderer from GregTech-Odyssey/GTOCore,
revision `3121e23241e7f6ccd2e6a8dbd5655962de63ed9a` (LGPL-3.0):

- `src/main/java/com/gtocore/client/renderer/machine/DimensionalFocusEngravingArrayRenderer.java`.
- `src/main/java/com/gtocore/client/renderer/RenderHelper.java`, `renderCylinder`.

The primitive and render states are referenced from the owning GregTech-Odyssey/GregTech-Modern fork,
revision `30cfd9fb3de3faee38cab6b36c5ba171ed82ba48` (LGPL-3.0),
`src/main/java/com/gregtechceu/gtceu/client/renderer/GTRenderTypes.java`, `LIGHT_RING` / `getLightRing`.
This pinned source uses POSITION_COLOR, TRIANGLE_STRIP, disabled face culling, Minecraft's built-in
POSITION_COLOR_SHADER and TRANSLUCENT_TRANSPARENCY. It uses no texture or custom shader.
GTOCore declares the GTCEu Maven dependency `26.7.3`. The LIGHT_RING/getLightRing render-state excerpt
is identical in the fork's 2026-07-03 snapshot `d228b2929332f4a52313cfb798e4780c7c1b3013`;
both snapshots and a normalized excerpt hash are retained. The Maven source JAR is unavailable,
so binary identity with that artifact is not asserted. Default LEQUAL depth testing and color/depth writes are retained.

The exact color channel conversions, upper clamp, truncation and easing are referenced from
Low-Drag-MC/LDLib-MultiLoader `5d68947b2fa1db7fdaadf36922973adbd1a2a183` (GPL-3.0), version 1.0.50:

- `common/src/main/java/com/lowdragmc/lowdraglib/utils/ColorUtils.java`.
- `common/src/main/java/com/lowdragmc/lowdraglib/utils/interpolate/Eases.java`, `EaseQuadIn`.
- `common/src/main/java/com/lowdragmc/lowdraglib/utils/interpolate/IEase.java`.

GTOCore's `dependencies.gradle` pins CurseForge project 626676 / file 7809449, which is LDLib 1.0.50.
Its older `gradle.properties` field `ldlib_version=1.0.48` is unused by that dependency declaration.
The copied source methods are not represented as the current general-purpose LDLib release.

Local adaptations:

- `com/xyp/gtnotgood/client/render/DimensionalFocusCoreGeometry.java`.
- `com/xyp/gtnotgood/client/render/DimensionalFocusCoreRenderer.java`.

The combined adaptations retain GPL-3.0-only, with the permissive-to-GPL option of the LGPL sources;
their licenses are not replaced by the project's general MIT license. Original license texts accompany this notice.
The local implementation replaces modern machine, matrix, vertex buffer and render lifecycle APIs with
Minecraft 1.7.10 / Forge / OpenGL APIs. It preserves the original six cylinders, radius 0.5, length 8,
10 angular segments, cumulative rotations, controller-relative center and 100-tick quadratic ARGB pulse.
The original emitter consists of 360 vertices rendered as one triangle strip. It contains no imported artwork.
The optional bloom integration uses the installed GTNHLib rendering API; it does not import Shimmer's renderer
or postprocessing implementation. The regular rendering path corresponds to the upstream non-Shimmer fallback.

Unmodified raw references are retained under `reference/dimensional-focus-port/render-upstream/`,
outside both compilation and resource packaging. `RENDER_MANIFEST.json` records exact source URLs,
revisions, reference paths, licenses, source hashes, adaptation destinations, modifications and destination hashes.
The source manifests are independent of `ASSET_MANIFEST.json`, which records the structure-block artwork.
