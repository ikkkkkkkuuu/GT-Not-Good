# 猎光号渲染来源与移植

主来源为 [GTLAdditions 34fb1528](https://github.com/Dragonators/GTLAdditions/tree/34fb152886164c56a07fc9d1b10e056f0c22b7ab)。完整源路径、固定提交、Git blob SHA-1 与 SHA-256 记录于 `RENDERING_SOURCES.json`；随包 `META-INF/light-hunter-space-station-port/RENDERING_MANIFEST.json` 同时记录目标路径、来源映射、目标哈希与修改内容。

| 部分 | 来源 | 本地目标 |
| --- | --- | --- |
| 主体、恒星切换与轨道 | `LightHunterSpaceStationRenderer.kt`、`RenderUtils.kt`、`CircularMotionParams.kt`、`RotationParams.kt`、`Constants.kt`、`CommonUtils.kt` | `client/render/LightHunterSpaceStationRenderer.java`、`LightHunterSpaceStationMath.java` |
| 双层光束与延迟提交 | `LightHunterSpaceStationBeamRenderer.kt`、`AntichristBeamRenderer.kt`、`DeferredMachineRenderer.kt`、`DeferredOculusCompat.kt` | 上述主 renderer 与 `shaders/lighthunter/beam.vsh`、`beam.fsh` |
| 恒星与空间模型 | [GTLCore 6443c848](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9) 的 `models/obj/star.obj`、`space.obj` 与对应 MTL | `models/lighthunter/`；`LightHunterModelMesh.java` 负责原几何读取 |
| 恒星、空间、光晕、光束与十种行星纹理 | GTLAdditions 原 PNG | `textures/render/lighthunter/`，字节不变 |
| 主世界、下界、末地行星纹理 | [GTCEu e3844ba9](https://github.com/GregTechCEu/GregTech-Modern/tree/e3844ba943e2388c0856617ae4f8b473a0ae1b64) 的 `dim_markers/*/west.png` | 同目录对应行星 PNG，字节不变 |
| 轨道绘制状态 | 同一 GTCEu 提交的 `GTRenderTypes.java` | 原白色、禁剔除、深度状态适配到 1.7.10 GL |

保留原 13 个 `cube_all` 方块形行星、各自位置种子的旋转、大小及倾斜圆周运动。轨道半径 22–53 格、倾角 70–90°、128 段、1.5 宽度与 4096 项三角函数表沿用原实现。主站原朝向基准为 EAST；恒星在局部 X=-206，光束从控制器中心延伸至局部 X=-174。星体位置随主站朝向旋转，独立轨道的随机倾角保持原规则。

恒星模型保留 960 个三角形、74 原始半径与 0.20 缩放，实际半径约 14.8 格。星阵数恰好达到 64 时使用原 `space.obj` 的 3840 个三角形与原空间纹理，并绘制 1.02 倍光晕。全部 17 张 PNG 和两组 OBJ/MTL 原字节保留；4096×2048 恒星与 8192×4096 光束/空间纹理没有缩小。

尺度另以固定 GTLAdditions 提交实际随附的 `libs/gtlcore-1.2.3.2-fix4.jar` 复核，
其 Git blob 为 `399807a82dc467658ac7bcb39ad32184a4b4448e`，内含两种 OBJ 的原始最大半径均为 74。
原模型 JSON 没有额外变换；实际声明的 Forge 47.3.7 OBJ loader 直接保留顶点坐标，没有隐含尺寸归一化。
原 renderer 在绘制恒星或空间球后独立绘制轨道，64 星阵只切换模型与光晕，不扩大球体。
因此 22–53 格轨道上的维度行星原本就在 14.8 格球体外，光晕半径为 15.096 格。
这与完整随附模型及当前实机结果一致，不存在漏乘模型缩放的问题。

光束保留两层朝向相机的半圆柱面、0.85 外半径、0.45 内层比例、原颜色与量化 alpha、强度 2/4、UV 时间运动及亮度调制 GLSL。原十段缓冲中九段只是重复末端形成的零面积三角形，本地只提交实际可见段。GLSL 150 接口改为 120；几何、相机方向衰减和纹理亮度数学保持。星、行星、轨道仍在 TESR 的不透明阶段；光束通过 GTNHLib 在世界末尾延迟绘制。

现代 OBJ 烘焙改为保留顶点、法线和 `flip_v` 的显示列表；纹理改为单独绑定原图片。相机原点复用现有 `HeartBlackHoleMath.cameraOffset` 的 modelview 逆变换，覆盖眼高及第三人称。位置种子缓存有数量上限，世界卸载会清理缓存与队列，资源重载会释放模型列表及着色器。

GTLAdditions 代码、着色器及所取纹理按其仓库 GPL-3.0 记录；GTLCore 模型和 GTCEu 纹理/参考绘制状态按对应仓库 LGPL-3.0 记录。随包 `RENDERING_LICENSE_*` 保留原许可文本。GTCEu README 另对 GregTech Refreshed 的 ULSTICK、Ghostipedia 的一致性修改及 ZedTech 社区纹理工作作一般致谢；本记录不声称每个资源均由分发仓库原创。参考源不参与编译或资源打包。

数学与资源测试检查四向中心/光束间距、轨道运动和包围范围、种子稳定性、原模型面数与半径。实际渲染通过独立静音隐藏客户端验证；最终结果以 `build/light-hunter-qa/result.txt` 与对应截图为准。
