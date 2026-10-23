# 超级粒子对撞机来源与材质

当前采用增强核心 3.8.0 的 197×83×209 镂空结构，原始数据和原生映射见 `ENHANCED_STRUCTURE.json`。
以下记录为已有视觉资源来源，现结构只使用路菲恩外壳与加速管道。

结构详见 `reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md`；配方详见本目录 `RECIPE_ADAPTATION.md`。
打包许可证、来源、改动和哈希记录位于 `src/main/resources/META-INF/particle-collider-port/`。

- GTLCore：[`6443c848`](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9)，
  LGPL-3.0。保留路菲恩机械方块 PNG、原始 32×32 LDLib CTM 图集及连接元数据。
- GTCEu Modern：[`1.20.1-1.4.4`](https://github.com/GregTechCEu/GregTech-Modern/tree/e3844ba943e2388c0856617ae4f8b473a0ae1b64)，
  LGPL-3.0。保留超导线圈、运行发光层、CTM 图集及原模型。该旧移植外观仅保留存档兼容，现结构按用户要求采用 GTNH 原生线圈。
  发光层按其实际 `.mcmeta` 引用 `casings/coils/machine_coil_superconductor_bloom_ctm`。
- 官方 GTL：[1.4.5.3 / CurseForge 9238465](https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465)，
  ZIP SHA-256 `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
  加速管道和改编配方使用包内根 `LICENSE` 的 **CC BY-NC-SA 4.0**，不采用下载页的不同许可证标签。
- CTM 四分面布局：[`LDLib-MultiLoader 1d4e4390`](https://github.com/Low-Drag-MC/LDLib-MultiLoader/tree/1d4e4390ae58d16038845c651e122185c29f334b)，
  GPL-3.0。本目录生成器复用既有布局转换器，按原图象限无损生成 141 张邻接纹理；不重画、调色或缩放。

旧 47×7×47 来源重复映射 `d` 后使用加速管道，没有聚合核心；该小结构已被增强版数据替换。
保留的旧移植线圈运行发光由原生 `GTStructureUtility.activeCoils`、`GTCoilTracker` 与 `IBlockWithActiveOffset` 同步，
偏移量 8；服务器线圈 metadata 为 2，客户端运行外观为 10。GT 原生渲染器提供发光层，未移植现代 shader。
固定 GT 外壳纹理页为 124，注册时检查冲突；125 为同时开发的 Dimensional Focus，126 为力场压模工厂。

原始 PNG 位于 `assets/gtnotgood/textures/blocks/particlecollider/`，原始 CTM 图集保存在 `source_ctm/`。
现代模型与 LDLib 元数据仅作为来源资料保存在打包记录目录，不参与 1.7.10 模型加载。
控制器直接引用力场压模工厂中同源的 GTCEu 聚变面板；本次清单单独登记引用，不改旧清单。

重新生成（需 Pillow）：

```text
python reference/particle-collider-port/generate-connected-textures.py src/main/resources/assets/gtnotgood/textures/blocks/particlecollider --manifest src/main/resources/META-INF/particle-collider-port/CTM_MANIFEST.json
```
