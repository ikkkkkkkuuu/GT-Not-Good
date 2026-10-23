---
name: gtng-upstream
description: 分析 GTNG 上游依赖源码，固定版本并移植代码或视觉资产。
---

先读 docs/development/project-rules.md 和已有 reference/UPSTREAM_PORT_NOTES.md。
从依赖坐标、manifest、缓存 JAR 或现有记录确定精确版本与提交；优先源码 JAR、已有 reference，再访问官方仓库。检查许可证后才复制。
区分当前 API、上游目标与项目适配层，记录方法签名、线程/端侧、保存数据、网络与资源约束；不把新版 API 直接用于旧版。
参考 checkout 不进入 compilation/resource source sets；来源、commit、license、目的路径和修改内容写入 provenance 与相应资产 manifest。
AE2LT Packaged Provider 视觉资产的 CC BY-NC-SA 3.0 授权独立于项目 MIT。连接器普通右键 Provider 选择、Shift 右键目标绑定。
按实际改动运行编译、资源或定向测试；上游差异结论附可复查的路径与版本。
