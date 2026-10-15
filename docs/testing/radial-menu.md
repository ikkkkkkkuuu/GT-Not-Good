# R 键径向菜单

测试在 `run/radial-menu-qa` 中创建一次性平坦世界，使用真实集成服务器、菜单、编辑器和按键动作执行器。
不读取正常游戏实例的存档或菜单配置。Windows 窗口由 `scripts/qa/HideFactoryClient.ps1` 按工作区和
`gtng.factory.qa` 标记定位，保持透明、点击穿透并隐藏任务栏图标。

## 运行

需要已缓存的 `build/factory-qa-dependencies/etfuturum-2.6.59-GTNH-dev.jar`。

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat --no-configuration-cache -I scripts/radial-menu-qa.init.gradle runClient
```

测试类仅从独立的 `build/radial-menu-qa/test-classes` 加载，不进入发布包。
成功后 `build/radial-menu-qa/result.txt` 内容为 `PASS`；失败会记录原因并使 Gradle 任务失败。
运行日志中的 `RADIAL_MENU_QA` 行提供逐项断言结果。测试运行包继承正常 JAR 的 manifest，
同时加载通用 mixin 和按键访问器的 early mixin，避免开发环境漏载。

## 覆盖

- 默认 R 键、默认轮盘内置工具腰带外圈入口，以及内置内容的只读保护。
- 真实服务端 10 槽逐个存入、满槽拒绝、持有工具交换、空手取出、非法物品和越界槽保护。
- 工具名称、损耗、附魔、自定义 NBT 在存入、交换和 NBT 保存重载后保留。
- 从实际工具腰带菜单取出物品，再选中空槽放回；两次均经客户端动作和网络包到达服务端。
- 工具腰带图标渲染器拿到完整 ItemStack NBT，而非只有物品 ID 和 metadata。
- 菜单配置保存时排除工具腰带的实时物品和 NBT，物品仍由服务端玩家数据持久化。
- 两层替换式子菜单进入和返回；外圈展开保留主轮盘并产生附加绘制环。
- 实际槽位编辑器保存中文名称，配置重新加载后名称、层级和外圈设置保留。
- 一次 TAP 动作通过真实按键事件只消费一次，随后释放并还原未绑定键。

测试通过反射设置目标扇区并调用生产菜单处理函数，避免隐藏窗口依赖桌面鼠标焦点；服务端物品数据、
动作队列、网络传输、编辑器保存和渲染均使用实际实现。

## 视觉复核

检查 `build/radial-menu-qa/screenshots/`：

- `main-wheel.png`：默认轮盘、中文标签、内置腰带入口。
- `tool-belt.png`：10 个附魔工具图标、名称、选中状态和间距。
- `outer-ring.png`：父轮盘与外圈扇区，确认边界、图标和文字没有重叠。
- `slot-editor.png`：中文标题输入框、动作字段和保存按钮。

截图生成不等于视觉验收；应打开截图逐项检查后再记录通过结果。

## 验证记录

2026-10-15：上述真实客户端测试通过，`result.txt` 为 `PASS`。已打开四张截图检查轮盘透明度、
描边、内外圈间距、十槽工具图标与耐久条，以及中文编辑器布局，未发现遮挡或裁切。
编辑器截图中的 `key.radialmenuqa.probe` 是测试专用按键，不进入发布包。

独立运行的 30 个径向菜单测试类共 300 项测试全部通过；`spotlessCheck assemble` 通过。
打包检查确认 LuaJ 已重定位，early mixin 配置和许可证齐全，参考代码目录与 QA 类未进入发布包。
