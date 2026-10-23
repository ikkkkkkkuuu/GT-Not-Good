---
name: gtng-multiblock
description: 开发或修复 GTNG 多方块机器、结构、配方与处理生命周期。
---

先读 docs/development/project-rules.md 和 docs/project-structure.md。
沿 GTNGMultiBlockBase、同类控制器、独立 structure 定义、GTNGItemList 注册追踪实际行为。模块化控制器放 modularmachine。
核对结构尺寸、朝向、舱室、能源、并行、超频、输入消耗、输出容量、中断及 NBT 恢复；让缓存有明确失效条件，不每 tick 扫描世界。
含流体 AE 处理配方使用 Ultimate Encoded Pattern；用 getAEStackInSlot 读取原生流体，测试写入真实流体 NBT。
修改前选择对应 docs/testing 文档、scripts/*qa.init.gradle 或 src/test；先编译和定向测试，再按需验证结构/启动/失败路径。tooltip 性能与结构必须和实现一致，结构放 Shift。
