# LDLib 1.7.10 基础库

基础框架适配本地 LDLib2 2.2.40；Fancy 窗口结构参考 GTCEu Modern v7.4.0-1.20.1，
默认主题使用 AE2 1.21.1 原图（LGPL-3.0），侧边标签与槽位使用 Modernity-GTNH-UI（CC BY-NC-SA 4.0）。
库代码不依赖本模组的机器和物品。
这是逐步移植的子集，不是完整 LDLib2，也不保证上游 API 原样兼容。

## 游戏内演示

更新后的客户端进入世界，执行 `/ldlib-demo`，无需手持物品。

- 基础控件：文字输入、复制粘贴、选区、数字校验、开关、按钮计数、物品图标。
- 滚动列表：40 个按钮，检查滚轮、边界、裁剪和 Tab 自动滚动至焦点。
- 主题材质：AE2 背景、普通/高亮/禁用按钮、普通/聚焦输入框及槽位。
- 切页返回后保留输入。Esc 关闭并恢复此前的键盘重复状态。

## 接入

通过 `ModernTheme.button`、`textField`、`scroller` 创建带统一外观的控件；
底层控件仍允许显式传入其他材质。已有界面需使用这些工厂或设置主题材质才会应用新外观。

创建 UIElement 根节点，用 addChild 添加控件，以 ModularUIScreen 打开。
Fancy 页面实现 IFancyUIProvider，通过 attachSideTabs 添加其他页面。
FancyMachineUIWidget 缓存页面；ModernTheme 由宿主传入资源路径解析器。
完整示例见 demo/LibraryDemoScreen.java，宿主适配见 LibraryDemoCommand。

## 契约

- 新界面用 ModularUIScreen / UIInput；mouseClicked 仅为首版原型的兼容入口。
- 坐标相对父元素，Flow 提供行列布局；布局在输入和绘制前更新。
- 子元素只有一个父节点。移除后可重新挂载，循环引用被拒绝。
- 事件按捕获、目标、冒泡传递。stopPropagation 保留当前节点其余监听器。
- 监听注册返回取消订阅函数。隐藏、禁用或移除页面后，其输入焦点会清理。
- ScrollerView 提供垂直滚动和嵌套裁剪，到边界后允许外层继续滚动。
- setText / setValue 不触发用户回调。校验器应允许空数字串等编辑中间态。
- 所有 TextField 默认支持右键清空并获得焦点，无需业务界面配置；非空内容清空时仅通知一次 onChange。
- 右键清空允许数量框进入空白编辑态；禁用控件、框外点击不清空。MUI2 适配输入框保留原生同类行为。
- integration/modularui/SelectorWidget 提供源通配符式下拉栏：80×15 按钮、最多5行、半透明黑底、白字。
- 下拉组件移除时关闭并清理弹层；每个实例使用独立菜单标识，重建筛选行不会重新打开旧实例的菜单。
- 下拉文字固定单行：短名称居中，长名称横向往返滚动并提供完整名称悬浮提示，避免固定行高裁掉换行文字。
- ModularUIScreen 和 integration/modularui/PixelFontModularScreen 使用原版像素英文字形，中文自动回退到 Unicode 字库；测量、绘制和输入期间统一切换，回调结束恢复字体状态。
- SpriteTexture 显式指定原图尺寸，支持图集区域、非对称九宫格和 ARGB 染色。
- ItemStackTexture 仅显示物品。所有回调均在客户端运行。

## 尚未移植

真实物品槽、服务端保存和同步、RPC、Taffy/Yoga、LSS/XML、可视化编辑器、
着色器和动画。许可证与源文件路径见 META-INF/ldlib-port/NOTICE.md。
