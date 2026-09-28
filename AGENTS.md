# Project Memory

- On the current GTNH/AE2 version, every processing recipe containing fluids uses the Ultimate Encoded Pattern. Read native fluid inputs from `MEInventoryCrafting#getAEStackInSlot`; its ItemStack view contains fluid packets, not legacy AE2FC fluid drops. Test fixtures must encode native fluid NBT in Ultimate Encoded Patterns.

- For the AE2LT Packaged Pattern Provider port, preserve the actual upstream GUI, textures, icons, layout and interactions; rewrite incompatible APIs rather than redesigning the visible result. Inspect the pinned reference source and license before drawing or copying assets. Keep source/commit/license/destination/modification records in `reference/UPSTREAM_PORT_NOTES.md` and the packaged asset manifest. Reference checkouts must never participate in compilation or resource packaging. The user accepts noncommercial distribution under CC BY-NC-SA 3.0 for applicable upstream visual assets; these assets are not covered by the project's general MIT grant. The connector contract is normal right-click Provider to select, then Shift-right-click target to bind.

- This is a GTNH / Minecraft 1.7.10 Forge addon based on `GTNewHorizons/ExampleMod1.7.10`.
- Keep `build.gradle.kts` aligned with the upstream starter. Put custom build logic in `addon.gradle`, dependencies in `dependencies.gradle`, and repositories in `repositories.gradle`.
- Use `com.xyp.gtnotgood.utils.enums.ModList` as the central source for mod ids, display names, resource domains, and related mod metadata. Do not hard-code this mod's id or name elsewhere unless Java annotation constant rules require referencing `ModList.ModIds` or `ModList.Names`.
- Use `com.xyp.gtnotgood.utils.enums.GTNGItemList` as the central GregTech-style item/machine container enum for this mod. Add new custom item or machine enum constants there and assign their `ItemStack`/`IMetaTileEntity` during registration.
- Use `com.xyp.gtnotgood.utils.enums.ModsItemlist` for external mod item and block registry references; keep its class and filename distinct from GregTech's `ItemList` and this mod's `GTNGItemList`.
- Use `com.xyp.gtnotgood.client.GTNGCreativeTabs` for this mod's creative tabs. Register ordinary items with `GTNGItem`, blocks with `GTNGItemBlock`, and GregTech machine stacks with `GTNGItemMachine` / `addToMachineList`.
- Use `com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase` as the default electric GregTech multiblock base. Its default GUI is `GTNGModernMultiBlockBaseGui`, with Cleanroom ModularUI textures registered in `GTNGGuiTextures`.
- Register normal/late mixins through `com.xyp.gtnotgood.loader.LateMixinsLoader#getMixins`, using `loadedMods` checks for optional target mods. Keep `mixins.gtnotgood.late.json` as the config returned by the loader and do not manually list ordinary late mixins in json.
- Only register mixins directly in json when they must load early, such as entries in `mixins.gtnotgood.early.json`.
- Do not write translations directly in `.lang` files. Declare translation keys beside the Java code that uses them, then let `preprocessLangInJavaFiles` generate lang files during `processResources`.
- Do not use `#tr` translation comments inside config classes. Forge config comments and category comments are hard-coded text, so write Chinese directly in the config string.
- Put each translation comment block immediately above the code line that uses that translation key. Do not collect translation comments at the top of a class or file.
- Keep each English and Chinese translation comment on one formatter-safe line. If a translation is long enough that code formatting would wrap it onto a continuation line, shorten/split the translated text or otherwise keep the generated `// # ...` line unwrapped, because wrapped translation comments break lang extraction.
- Do not add boilerplate class-level comments to straightforward registry enums such as `ModsItemlist`. For special or
  non-obvious classes, methods, fields, and override points, write explanatory Javadoc with behavior, constraints,
  `@param`, `@return`, and `@see` entries where useful, similar to the existing `utils.tr` method comments.
- Keep code and review descriptions concise. Do not add redundant metadata comments, registry lookup explanations, or repeated comments that merely restate names or code. For external items, comment the verified Chinese item name; explain metadata only when it changes behavior and that distinction is not clear from the name.
- Use consistent Java camel case: `UpperCamelCase` for types and enum constants, `lowerCamelCase` for methods and fields. Name item constants for the actual item or verified variant. Do not use placeholder names such as `UP_`, `Meta1`, or arbitrary `Variant1`, and do not add a mod or project prefix unless it prevents ambiguity.
- Import referenced classes normally. Do not use fully qualified class names in Java code when a normal import is possible; use a qualified name only to resolve an actual name conflict.
- In `ModsItemlist`, keep mod groups in alphabetical order and separate groups with one blank line; do not insert blank lines inside a group. Reference vanilla Minecraft items and blocks directly through `Items` and `Blocks`. Prefix Universal Singularities constants with `US` in camel case, without underscores.
- Translation comment format:

```java
// #tr gui.example.key
// # English text
// # zh_CN Chinese text
```

- Color placeholders in translation comments may use names like `{\\RED}`, `{\\RESET}`, and `{\\SPACE}`; `addon.gradle` converts them to Minecraft formatting codes.
