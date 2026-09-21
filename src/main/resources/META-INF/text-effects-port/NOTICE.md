# GT Not Leisure text effects port

Source: https://github.com/ABKQPO/GT-Not-Leisure
Authors: ABKQPO and GT-Not-Leisure contributors.
Pinned revision: 52345d2c059c871febec1365d6012424c7d64547.
Requested commits: d28217f99d6390dc2ac231a7c17fab2320e18c44,
9bd2f59af16af6011fe62470e9023f40ec678a53, 52345d2c059c871febec1365d6012424c7d64547.

The ported Java text renderer, syntax utilities and text-effect mixins retain the upstream LGPL-3.0
license (GTNL-LGPL-3.0.txt and its incorporated GPL-3.0.txt). They are not relicensed under the
project's general MIT license. Shader files retain their upstream attribution comments and the
included calamity-overhaul-text-effects-MIT.txt and fargo-text-effects-MIT.txt notices.

Source-to-destination mapping:
- src/main/java/com/science/gtnl/client/text/** -> com/xyp/gtnotgood/client/text/**
- src/main/java/com/science/gtnl/utils/text/effect/** -> com/xyp/gtnotgood/utils/text/effect/**
- src/main/java/com/science/gtnl/mixins/{early,late}/texteffect/** -> com/xyp/gtnotgood/mixins/{early,late}/texteffect/**
- assets/sciencenotleisure/shaders/text/** -> assets/gtnotgood/shaders/text/**

Modified 2026-09-21 for GT-Not-Good: relocated Java packages and shader identifiers; renamed mixin
bridge methods; adapted modern library APIs to Java 8/Guava and records to Jabel; integrated
client initialization, resource reload, optional mixin loading and generated translations;
renamed preview command to gtngtexteffects; connected machine tooltip credits to EXOTIC_RAINBOW.
GLSL rendering algorithms and the final upstream sampling fix are retained.

Editable source is included in this project's source tree and sources artifact. When distributing
the binary, also provide the corresponding source and build files so recipients can rebuild and
replace the modified LGPL portions. Reference downloads are outside the source/resource roots.
