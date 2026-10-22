# 力场压模工厂材质来源

- GTLCore：[`6443c848`](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9)，
  根许可证 LGPL-3.0。保留铱强化、超能、分子机械方块的原始 PNG，以及前两种的 LDLib CTM 图集和元数据。
- GTCEu Modern：[`1.20.1-1.4.4`](https://github.com/GregTechCEu/GregTech-Modern/tree/e3844ba943e2388c0856617ae4f8b473a0ae1b64)，
  根许可证 LGPL-3.0。保留洁净室玻璃、CTM 图集，以及聚变控制器的普通、运行与对应发光层和动画。
- 官方 GTL 整包：[1.4.5.3 / CurseForge 9238465](https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465)，
  ZIP SHA-256 `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
  中空机械方块与遏制场发生器取自 `.minecraft/kubejs/assets/kubejs/textures/block/`。
  包内根 `LICENSE` 为 **CC BY-NC-SA 4.0**，与下载页标注不同；这两张图及发生器动画按包内许可证保留。
  两张 PNG 与早期 Kaka25z 整包快照像素和文件哈希相同，但本次分发来源采用有明确许可证的官方包。
- CTM 四分面布局：[`LDLib-MultiLoader 1d4e4390`](https://github.com/Low-Drag-MC/LDLib-MultiLoader/tree/1d4e4390ae58d16038845c651e122185c29f334b)，
  根许可证 GPL-3.0。离线生成器适配 `Connections.fillSubmaps` 与 `Submap.uvs`，按原图集生成 141 张邻接纹理；
  每张图仅无损组合原始 8×8 象限。Java 渲染使用本项目的 GT/Forge 1.7.10 接口，不编译上游检出。

打包来源、去向、修改与哈希记录位于 `src/main/resources/META-INF/field-extruder-port/`。
原始 CTM 图集保存在 `assets/gtnotgood/textures/blocks/fieldextruder/source_ctm/`，
LDLib `.mcmeta` 保存在许可证目录的 `source-metadata/`，游戏实际使用同路径下 `ctm/` 的 47 张纹理。
材质许可不并入项目通用 MIT 许可；源文件和适用的 LGPL、GPL、CC BY-NC-SA 许可证随分发保留。

重新生成（需 Pillow）：

```text
python reference/field-extruder-port/generate-connected-textures.py src/main/resources/assets/gtnotgood/textures/blocks/fieldextruder --manifest src/main/resources/META-INF/field-extruder-port/CTM_MANIFEST.json
```
