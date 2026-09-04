# GT-Not-Good

GT-Not-Good is a GTNH-compatible Forge mod project for Minecraft 1.7.10.

This project was created from the official
[GTNewHorizons ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10)
starter layout.

## Development

Run these commands from the repository root:

```powershell
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat build
```

GTNHGradle derives the mod version from Git. After the first commit, create an
initial version tag such as `0.1.0` before making release builds.

Project identity is configured in `gradle.properties`:

- `modName = GT-Not-Good`
- `modId = gtnotgood`
- `modGroup = com.xyp.gtnotgood`

Add mod dependencies in `dependencies.gradle` and custom repositories in
`repositories.gradle`, keeping the upstream GTNH build script easy to update.

## Localization

Do not write translations directly in `.lang` files. Put translation keys next
to the Java code that uses them:

```java
// #tr gui.example.key
// # English text
// # zh_CN Chinese text
```

`addon.gradle` extracts those comments and generates lang files during
`processResources`.
