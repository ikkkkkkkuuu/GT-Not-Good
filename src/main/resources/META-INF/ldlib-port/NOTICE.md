# LDLib GUI port for Minecraft 1.7.10

Framework source: user-supplied LDLib2-1.21 snapshot, version 2.2.40, by KilaBash /
Low-Drag-MC, https://github.com/Low-Drag-MC/LDLib2. Its LGPL-3.0 terms are in LICENSE.
This unofficial subset is distributed in com.xyp.ldlib with the project's source tree
and sources JAR. Original authors retain copyright. It is not a drop-in replacement.

Reference classes: UIElement, UIEvent, UIEvents, Label, TextField, Button, Toggle,
ScrollerView, ModularUIScreen, SpriteTexture, TextTexture, GuiTextureGroup,
ColorRectTexture, ColorBorderTexture and ItemStackTexture.
Rendering uses the 1.7.10 font/item renderers and Tessellator. Java Flow layouts
replace native Taffy/Yoga. TextField delegates cursor, selection and clipboard
operations to 1.7.10 GuiTextField rather than the modern editor.

Fancy contracts/organization and the root background.png/button.png under assets/gtnotgood/textures/gui/ldlib:
GregTechCEu/GregTech-Modern v7.4.0-1.20.1,
commit 07ac5207f6f58ba3a2f9cd8b862b382d24300a17.
https://github.com/GregTechCEu/GregTech-Modern/tree/v7.4.0-1.20.1
Its LGPL-3.0 terms are reproduced in GTCEu-LICENSE.
IFancyUIProvider, FancyMachineUIWidget, TabsWidget and VerticalTabsWidget are
adapted without GregTech machine dependencies or real inventory slots.

Copied image files (unchanged bytes):
- base/background.png -> background.png
- widget/button.png -> button.png
- icon/close.png -> modern/close.png
- progress_bar/progress_bar_arrow.png -> modern/progress_bar_arrow.png

The MUI2 selector bridge and wildcard preview follow the interaction contracts and
layout of LeoDreamer/Wildcard-Pattern 0.1.2 (MIT, user-supplied source).
See Wildcard-Pattern-LICENSE. The selector is a new MUI2 implementation; no modern
LDLib SelectorWidget source is copied. Property/flag candidates use GTNH properties/SubTags.

Modern theme slot and side-tab artwork: ModernityGTNH/Modernity-GTNH-UI contributors,
commit 87fee0aac305ca8f1736278266ad87d94deff3b4.
https://github.com/ModernityGTNH/Modernity-GTNH-UI/tree/87fee0aac305ca8f1736278266ad87d94deff3b4
Licensed separately under CC BY-NC-SA 4.0; see Modernity-LICENSE.txt.
Unchanged files from assets/modularui2/textures/gui/ mapped to ldlib/modern/:
- slot/item.png -> slot.png
- tab/tabs_left.png -> tabs_left.png
Only runtime stretching, atlas selection and tinting are applied.

AE2 1.21.1 artwork: Applied Energistics 2 contributors,
commit fd8b717a405672ce4f65ba540f1db8c91317daa4.
https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/fd8b717a405672ce4f65ba540f1db8c91317daa4
LGPL-3.0; the upstream license is included in AE2-LICENSE.
Unchanged assets/ae2/textures/ images mapped to ldlib/modern/:
- guis/background.png -> background.png and title_bar_background.png
- guis/text_field.png -> text_field.png (normal/disabled/focused 128x12 rows)
- gui/sprites/button.png -> button.png
- gui/sprites/button_highlighted.png -> button_highlighted.png
- gui/sprites/button_disabled.png -> button_disabled.png
- gui/sprites/small_scroller.png -> small_scroller.png
Button nine-slice border 3 follows the upstream .png.mcmeta.
The 1.7.10 renderer stretches these images at runtime; no image bytes are modified.

The GPL-3.0 terms incorporated by LGPL-3.0 are also included in
META-INF/licenses/MessTech-GPL-3.0.txt.

Not ported: native layout engines, LSS/XML, editors, shaders, animation/transforms,
real inventory slots, server synchronization or RPC. ItemStackTexture is
display-only. All callbacks are client-only.
