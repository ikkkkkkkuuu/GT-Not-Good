# 特效文字

移植 GT-Not-Leisure 的 d28217f、9bd2f59、52345d2，最终源码固定在
`52345d2c059c871febec1365d6012424c7d64547`。

在客户端输入 `/gtngtexteffects` 打开预览，可输入中英文、翻页，并切换配色、粗体和斜体。
点击某一效果行后按“应用到机器署名”，选择会立即用于所有引用 `AnimatedText.GT_NOT_GOOD`
的机器提示，并保存到客户端配置目录的 `gtnotgood-text-effects.cfg`。黄色行是当前选中，
`*` 标出已应用的效果。默认效果为 `EXOTIC_RAINBOW`。翻译前缀按显示时的语言解析。

普通字体渲染入口支持如下文字（聊天、告示牌和支持原版字体的界面）：

```text
&{er}彩虹文字&r
&{ba;#fc0;2}Hi
&{pu}&oHello&r
```

`&r` / `§r` 重置效果；普通颜色代码替换当前配色。可用 `colors` / `c` 指定最多八种
RGB 颜色，`speed` / `s` 指定非负速度，0 表示静止。告示牌仍遵守原版每行 15 字符限制。

| 别名 | 效果 |
| --- | --- |
| ir | infernum_red_rarity |
| gc | genesis_component_rarity_shader |
| pc | pulse_circle |
| nb | nameless_boss_bar_shader |
| pu | pulse_upwards |
| cr | calamity_red |
| er | exotic_rainbow |
| sb | superboss_rarity |
| is | infernum_spark_rarity |
| ba | burnished_auric |
| ec | evercold_cyan |
| ss | starsilver_rarity |

Java 接入：`AnimatedTooltipHandler.renderedText(text, TextEffects.EXOTIC_RAINBOW)`，
或 `TextEffects.apply(text, style)` 创建可嵌套的封闭片段。`TextEffects.format(style)`
创建直到 reset 的内联声明。自定义渲染器在客户端初始化时注册到 `TextEffectRegistry`，
并可通过 `TextEffectFormat.registerAlias` 注册短别名。

Angelica 为可选集成，当前适配 API 为 2.2.13。无 Angelica 时走原版字体；不支持着色器或
帧缓冲时退回普通文字。资源重载会释放文字遮罩缓存并重新编译着色器。
NEI 注入仅在客户端且 NEI 已加载时注册。原版字体和格式类必须通过早期 Mixin 注入。

移植来源和许可证见 `src/main/resources/META-INF/text-effects-port/NOTICE.md`。

## 验证记录（2026-09-21）

- `compileJava`、`processResources`、`assemble`、`sourcesJar` 和 7 项 `TextEffectsTest` 通过。
- 带 Angelica 2.2.13 / 不带 Angelica 的真实客户端均通过：12 个预设渲染、粗体/斜体、
  中英文、机器署名、效果文字宽度、NEI 保留输入声明，以及资源重载后重新渲染。
- 带 Angelica 的客户端验证了点击效果行并应用后机器提示立即变化，重读配置后选择仍生效。
- 截图：`build/text-effects-qa/screenshots/{angelica,vanilla}-{60,120,180}.png`。
- 成品包含 15 个 GLSL 文件及来源/许可记录，不包含测试类和参考源码目录。
- 未进行世界内告示牌的遮挡回归、独立服务器启动或第三方字体的全面兼容测试。

可用 `gradlew -I scripts/text-effects-qa.init.gradle runClient --no-configuration-cache` 复现客户端检查。
添加 `-Dgtng.text.noAngelica=true` 可检查无 Angelica 分支。脚本在主菜单执行，
将本项目构建输出复制到临时目录以隔离并行构建，完成后自动关闭测试客户端。
