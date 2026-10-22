# Heart of the Universe rendering notice

This distribution includes a Java/GLSL port of the Heart of the Universe renderer from [Dragonators/GTLAdditions](https://github.com/Dragonators/GTLAdditions), pinned to commit `34fb152886164c56a07fc9d1b10e056f0c22b7ab`.

The upstream Kotlin renderer, framebuffer/composite helpers, visual profile, volume mesh and shaders are covered by the upstream GPL-3.0 license. The ported renderer and shader files carry `SPDX-License-Identifier: GPL-3.0-only`. See `LICENSE-GTLAdditions.txt` in this directory. These files are not covered by the project's general MIT grant.

`RENDERING_MANIFEST.json` records source paths, immutable source URLs, source Git blob and SHA-256 hashes, destination hashes, and modifications. Java and shader source is distributed in the project source repository; reference checkouts are excluded from compilation and resource packaging.

The port retains the upstream curved-ray integration, accretion disk, procedural starfield, photon ring, heartbeat volume edge, seeded rotation, world-depth occlusion, separate effect/mask targets, separable blur and premultiplied-alpha composition. Modern rendering APIs and GLSL 150 interfaces have been adapted to Minecraft 1.7.10, GTNHLib and GLSL 120. Existing project GL-state/FBO integration patterns from the LGPL-3.0-only Kerr-Newman renderer were also consulted; its black-sphere visual and refraction shader are not used here.

Upstream comments refer to Radiant Event Horizon default rotation parameters and projected proportions. Those references are retained as provenance facts. They do not establish that every algorithm originated in GTLAdditions, and do not grant additional rights in any separately licensed third-party work.

Structure and item assets have their own notices and manifests in this directory. No upstream runtime library is bundled by this rendering port.
