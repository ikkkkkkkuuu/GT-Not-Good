# Original black-hole rendering and native adaptation

Source: GregTech-Odyssey/GTOCore revision `3121e23241e7f6ccd2e6a8dbd5655962de63ed9a`, LGPL-3.0.
Copyright remains with the GTOCore contributors and credited upstream authors.
The machine's active client ticker creates `BlackHole(center, 2.5f, 4.5f)` at the controller's center plus 34 blocks
opposite its front face. This live FX is the source of the ported central black core and scene refraction.

Preserved references include `KerrNewmanHomogenizer.java`, `fx/BlackHole.java`, `RenderHelper.java`,
`GTORenderTypes.java`, screen-space projection, scene capture, mesh rendering, FX lifecycle and the complete
`black_hole_event_horizon.{vsh,fsh,json}` source resources. See SOURCE_MANIFEST.json for exact paths/hashes.
The optional rainbow torus code in `KerrNewmanHomogenizerRenderer.java` is disabled upstream by RENDER_RINGS=false;
it remains disabled. That source file credits a prior GTNL effect under LGPLv3; no independent GTNL source
snapshot or unrelated GTNL machine structure is substituted for the fixed GTO source in this port.

Destinations:

- `com/xyp/gtnotgood/client/render/KerrNewmanBlackHoleRenderer.java`
- `com/xyp/gtnotgood/client/render/KerrNewmanBlackHoleMath.java`
- `assets/gtnotgood/shaders/kerrnewman/black_hole_event_horizon.vsh`
- `assets/gtnotgood/shaders/kerrnewman/black_hole_event_horizon.fsh`

All four retain LGPL-3.0. They replace modern PoseStack/JOML/MultiBufferSource/FXManager integration with the
Forge 1.7.10 world rendering hook and native OpenGL, preserve source radii, growth/shrink curves and refraction
formulas, and retain a world-fixed ending animation after the controller stops. A pure mathematical regression
test checks sphere topology/winding and screen projection independently of Minecraft's client.

The original GLSL 150 interfaces are adapted to GLSL 120 compatibility built-ins and syntax. ScreenOrigin is added
so the same screen-space math works when the current viewport starts away from (0,0). The sphere's triangle winding
is reversed to outward CCW because the original helper's inward order otherwise conflicts with the fragment
shader's gl_FrontFacing test on the native renderer. Sphere positions and detail levels remain unchanged.

The captured scene uses the installed GTNHLib CustomFramebuffer. Native framebuffer blit is preferred when
available; the EXT-only fallback copies from the active world color target with glCopyTexSubImage2D.
The renderer restores OpenGL and framebuffer state and releases resources on reload. The scene capture replaces
Minecraft 1.20's private WeatherTarget integration; it is not a static image or approximate replacement texture.
The shader samples the live scene, so no additional original texture artwork is needed.

Exact source and final destination hashes, including these compatibility changes, are in RENDER_MANIFEST.json.
The reference tree and this port do not require Minecraft 1.20, modern GTCEu, LDLib or Shimmer at runtime.
