# 石化与稀土配方回归

```powershell
scripts/dev/Invoke-Gradle.ps1 spotlessApply compileTestJava runClient25 --init-script scripts/production-simplification-qa.init.gradle --project-cache-dir D:/CodexTools/GTNGProductionSimplification-20261024/qa-cache --offline --no-configuration-cache --console=plain
```

独立 build、客户端和结果位于 `D:/CodexTools/GTNGProductionSimplification-20261024`。
夹具使用 Java 25 入口及降级后的测试类，启动前应用仓库的静音、透明、穿透和隐藏任务栏辅助脚本。
`qa/result.txt` 以 `PASS` 开头才表示本轮完整通过；截图在 `qa/screenshots/`。

覆盖范围：

- 集成化工厂模式 0–2 保持，新增模式 3 石化合成；均质仪模式 0 保留搅拌，新增模式 1 粉体和 2 矿石。
- 全部模式保存、重载、循环切换、非法值和旧存档；GUI 模式列表、服务端交互回包、关闭重开。
- 12 条石化、11 条粉体、12 条矿石配方的实际耗料与产物；四种替代原油、不可消耗电路及概率产物。
- 石化电路 7 按声明保留 80 碳粉输出；电路 4 的实际产物为苯乙烯。
- 稀土矿石温度要求移除；新模式沿用均质仪 60 倍基础速度及默认耗时÷2、EU/t×4 的模块化规则。
- 原生 Ultimate Encoded Pattern 处理粉体和矿石配方，读取真实流体 NBT，核对耗料与已提交产物的保存。
- NEI 多产物页面的槽位边界和截图。

测试使用真实控制器、输入输出仓和样板总成，成型与供电准入由夹具控制。
完整巨型结构、真实 AE CPU 下单和长期吞吐沿用各自专项验证范围。
固定来源、MIT 许可和适配差异见 `META-INF/production-simplification-port/`。

2026-10-24：首轮 `client.log` 退出 0、1m11s；补充碳粉数量独立断言和 NEI 三页后，
`client-final.log` 退出 0、1m36s，`qa/result.txt` 为 PASS。NEI 三页分别验证 14、24、9 个槽位，
石化七流体产物和稀土十八物品产物截图已目视核对完整。最终代理客户端 68708 已退出。
