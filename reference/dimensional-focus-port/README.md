# 维度聚焦激光蚀刻阵列材质

- GTLCore [`6443c848`](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9)：
  超维度、维度注入、改良型超导线圈的原图与 LDLib 连纹图集，LGPL-3.0。
- GTCEu Modern [`e3844ba9`](https://github.com/GregTechCEu/GregTech-Modern/tree/e3844ba943e2388c0856617ae4f8b473a0ae1b64)：
  聚变玻璃、激光安全蚀刻机械方块的原图与连纹图集，LGPL-3.0。
- 官方 GTL 1.4.5.3 整包，CurseForge 文件 `9238465`：分子线圈的侧面及顶底面，CC BY-NC-SA 4.0。
  ZIP SHA-256：`1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
- 遏制场发生器、聚变反应堆控制器面板沿用 `fieldextruder` 已打包的同一上游材质。

使用现有 `reference/field-extruder-port/generate-connected-textures.py` 的 LDLib 四分面算法，无损组合 282 张连纹图。
分子线圈保留侧面与顶底面的区别；超导线圈保留停机与运行材质，运行状态通过方块元数据的高位同步。
现代 Shimmer/GTLBloom 着色器不随移植导入，运行线圈使用原图和原生全亮渲染。
中央动态光柱另外移植后续 GTOCore 的同名机器渲染，使用 GTNH 原生机器 TESR 与可选 Bloom；
六向光柱与线圈色的百 tick 脉动保留上游规则。来源见 `META-INF/dimensional-focus-port/RENDER_NOTICE.md`，
未修改的渲染参考源码位于 `render-upstream/`。
所有原图、许可证、来源路径、去向和 SHA-256 位于 `META-INF/dimensional-focus-port/`。
材质各自保留上游许可，不并入项目通用 MIT 许可。参考源码不参与编译或资源打包。

重新导入（需要 Pillow）：

```text
python reference/dimensional-focus-port/import-assets.py <GTLCore参考目录> <官方1.4.5.3.zip>
```

动态核心另外采用 GTOCore `3121e232` 的专用渲染器，保留六向圆柱、位置、色彩脉冲与三角带顶点顺序。
移植后的渲染器与几何类位于 `src/main/java/com/xyp/gtnotgood/client/render/`。
原始参考源码位于 `render-upstream/`，不参与编译或资源打包；许可证及来源记录见
`META-INF/dimensional-focus-port/RENDER_NOTICE.md` 和 `RENDER_MANIFEST.json`。
该效果仅使用顶点颜色，无贴图或独立着色器。渲染器组合移植部分保留 GPL-3.0-only。

重新取得参考源码、刷新渲染清单：

```text
python reference/dimensional-focus-port/import-render-reference.py
```
