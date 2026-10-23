---
name: gtng-mixin
description: 开发、审查或调试 GTNG 的 early/late Mixin 与可选模组兼容。
---

先读 docs/development/project-rules.md。
确认 dependencies.gradle 和当前缓存 JAR 的真实版本；读取目标方法签名及字节码，再确定 descriptor、At、ordinal、slice 和 remap。不要凭其他版本源码猜测。
检查 loader/LateMixinsLoader.java、EarlyMixinsLoader.java 及 mixins JSON；普通 late 条目由 getMixins(loadedMods) 返回，可选模组必须有存在检查；只有必须早载的项目直接注册 early JSON。
核对 dedicated server 的类加载、注入次数和缺少可选模组时行为。编译成功不等于注入成功；需要现有目标测试或受控客户端/服务端日志证明目标匹配。记录未做的运行时验证。
