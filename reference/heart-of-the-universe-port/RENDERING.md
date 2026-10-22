# 宇宙之心黑洞渲染移植

来源：[Dragonators/GTLAdditions](https://github.com/Dragonators/GTLAdditions/tree/34fb152886164c56a07fc9d1b10e056f0c22b7ab)，固定提交 `34fb152886164c56a07fc9d1b10e056f0c22b7ab`。相关 Kotlin 与 GLSL 按上游 GPL-3.0 移植，目标文件标记 `GPL-3.0-only`；随包许可为 `META-INF/heart-of-the-universe-port/LICENSE-GTLAdditions.txt`。

源文件、Git blob SHA-1、SHA-256、目标文件 SHA-256 与修改说明见随包 `RENDERING_MANIFEST.json`。参考源不参与编译或资源打包。

| 目标 | 对应上游源 |
| --- | --- |
| `client/render/HeartBlackHoleRenderer.java` | `HeartBlackHoleRenderer.kt`、`HeartBlackHoleCompositeRenderer.kt`、`HeartBlackHoleShaders.kt`、`HeartDeferredRenderer.kt`、`HeartOfTheUniverseRenderer.kt` |
| `client/render/HeartBlackHoleTargets.java` | `HeartBlackHoleFramebuffer.kt`、`HeartBlackHolePostTargets.kt` |
| `client/render/HeartBlackHoleMath.java` | `HeartBlackHoleRenderProfile.kt`、`HeartBlackHoleRenderer.kt`、`HeartBlackHoleQuadBuffers.kt` |
| `shaders/heart/black_hole.vsh`、`black_hole.fsh` | `gtladditions_heart_black_hole.vsh`、`gtladditions_heart_black_hole.fsh`、`heart_volume_common.glsl` |
| `shaders/heart/post.vsh`、`blur.fsh`、`composite.fsh` | `gtladditions_heart_post.vsh`、`gtladditions_heart_blur.fsh`、`gtladditions_heart_composite.fsh` |
| `shaders/heart/heart_volume_common.glsl` | 同名公共 GLSL，另内联到体积顶点与片元着色器 |

Java 路径相对于 `src/main/java/com/xyp/gtnotgood/`，资源路径相对于 `src/main/resources/assets/gtnotgood/`。上游 Kotlin 位于 `src/main/java/com/gtladd/gtladditions/client/render/machine/`，具体完整路径以 manifest 为准。

保留原 29 格黑洞/吸积盘半径、心跳体积边缘、200 步弯曲光线积分、温度与多普勒吸积盘、旋转星空、光子环、固定种子的缓慢旋转和垂直倾角限制。体积网格沿用 24×48 分段。渲染顺序保持 effect 与 mask 两个体积 pass、effect 与 mask 各两次分离高斯模糊、最后预乘 alpha 合成，共七个 pass；模糊目标沿用四分之一分辨率。

现代 `ShaderInstance`、`PoseStack`、`RenderTarget`、`RenderLevelStageEvent` 与 Oculus/Iris 桥接改写为 GTNHLib `ShaderProgram`、`CustomFramebuffer`、世界末尾延迟提交和 1.7.10 OpenGL 接口。GLSL 150 改为 120 的固定管线顶点、varying、采样与片元输出接口，公共 GLSL 内联；光线积分及颜色数学保留。体积 pass 临时共享当前世界 FBO 的深度/模板附件，禁止深度写入，结束后解除附件，不拥有或删除世界深度资源。相机原点从实际 modelview 逆变换计算，覆盖眼高及第三人称偏移。

基础 GL 状态保存恢复与 GTNHLib 接入也参考本项目既有 LGPL-3.0-only 的 `KerrNewmanBlackHoleRenderer.java`。宇宙之心保留独立的体积黑洞渲染与七 pass 合成；没有采用克尔-纽曼黑球或其折射着色器替代。

上游 `HeartBlackHoleRenderProfile.kt` 注释提及 Radiant Event Horizon 默认旋转参数，片元着色器注释提及其投影比例；这些说明保留。该引用不构成“全部视觉算法均为上游原创”的认定，也不补充授予其他项目的许可。

验证包括球面半径/朝向、稳定旋转与倾角、眼高/第三人称相机原点的单元测试，以及 `scripts/heart-qa.init.gradle` 的独立静音隐藏客户端。客户端截图、七 pass、遮挡和重启结果以 `build/heart-qa/result.txt` 和同目录截图为准；仅编译通过不能代替实际画面检查。
