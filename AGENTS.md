# Project Memory

- This is a GTNH / Minecraft 1.7.10 Forge addon based on `GTNewHorizons/ExampleMod1.7.10`.
- Keep `build.gradle.kts` aligned with the upstream starter. Put custom build logic in `addon.gradle`, dependencies in `dependencies.gradle`, and repositories in `repositories.gradle`.
- Use `com.xyp.gtnotgood.utils.enums.ModList` as the central source for mod ids, display names, resource domains, and related mod metadata. Do not hard-code this mod's id or name elsewhere unless Java annotation constant rules require referencing `ModList.ModIds` or `ModList.Names`.
- Use `com.xyp.gtnotgood.utils.enums.GTNGItemList` as the central GregTech-style item/machine container enum for this mod. Add new custom item or machine enum constants there and assign their `ItemStack`/`IMetaTileEntity` during registration.
- Use `com.xyp.gtnotgood.client.GTNGCreativeTabs` for this mod's creative tabs. Register ordinary items with `GTNGItem`, blocks with `GTNGItemBlock`, and GregTech machine stacks with `GTNGItemMachine` / `addToMachineList`.
- Use `com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase` as the default electric GregTech multiblock base. Its default GUI is `GTNGModernMultiBlockBaseGui`, with Cleanroom ModularUI textures registered in `GTNGGuiTextures`.
- Register normal/late mixins through `com.xyp.gtnotgood.loader.LateMixinsLoader#getMixins`, using `loadedMods` checks for optional target mods. Keep `mixins.gtnotgood.late.json` as the config returned by the loader and do not manually list ordinary late mixins in json.
- Only register mixins directly in json when they must load early, such as entries in `mixins.gtnotgood.early.json`.
- Do not write translations directly in `.lang` files. Declare translation keys beside the Java code that uses them, then let `preprocessLangInJavaFiles` generate lang files during `processResources`.
- Do not use `#tr` translation comments inside config classes. Forge config comments and category comments are hard-coded text, so write Chinese directly in the config string.
- Put each translation comment block immediately above the code line that uses that translation key. Do not collect translation comments at the top of a class or file.
- Keep each English and Chinese translation comment on one formatter-safe line. If a translation is long enough that code formatting would wrap it onto a continuation line, shorten/split the translated text or otherwise keep the generated `// # ...` line unwrapped, because wrapped translation comments break lang extraction.
- Add Javadoc immediately before each new class or enum. For special or non-obvious classes, methods, fields,
  and override points, write a full explanatory Javadoc block with behavior, constraints, `@param`, `@return`,
  and `@see` entries where useful, similar to the existing `utils.tr` method comments.
- Translation comment format:

```java
// #tr gui.example.key
// # English text
// # zh_CN Chinese text
```

- Color placeholders in translation comments may use names like `{\\RED}`, `{\\RESET}`, and `{\\SPACE}`; `addon.gradle` converts them to Minecraft formatting codes.
