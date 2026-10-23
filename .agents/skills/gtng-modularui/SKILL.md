---
name: gtng-modularui
description: 修改 GTNG ModularUI2 GUI、交互同步、布局、纹理和 tooltip。
---

先读 docs/development/project-rules.md，定位现有 GTNGModernMultiBlockBaseGui、GTNGGuiTextures 与相近界面。
确认使用的是当前依赖的 ModularUI2 API。用同步值传显示状态，动作在服务端处理；面板构造期不能调用 PanelSyncManager.getPlayer()。
保持服务端权威，检查关闭重开、物品变化、滚动边界和不同 GUI 比例。翻译块紧贴使用位置且每种语言一行；执行 processResources 验证格式。
可视改动需要实际客户端检查；使用既有 QA 脚本，启动前确认静音与隐藏 helper 作用域，仅作用于代理的进程。未做视觉验证必须明确说明。
移植 Packaged Provider 时先读 reference/UPSTREAM_PORT_NOTES.md；保留上游实际布局与交互，不重新设计。
