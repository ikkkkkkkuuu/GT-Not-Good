# 特殊蜂窝处理

大型蜂窝处理机原先只导入离心机、大型化学反应釜和流体提取机配方。
海晶、下界合金、凯芙拉、龙血、力量、宇宙中子态素、无尽蜂窝的原生处理位于其他机器，因而缺失。

按用户选择，新增仅供本机使用的简化配方，每次消耗 1 个蜂窝：

| 蜂窝 | 产物 |
| --- | --- |
| 海晶 | 海晶碎片 ×1 |
| 下界合金 | 下界合金粉 ×1 |
| 凯芙拉 | 熔融凯芙拉 144 mB |
| 龙血 | 龙血粉 ×1 |
| 力量 | 力量粉 ×1 |
| 宇宙中子态素 | 宇宙中子态素粉 ×1 |
| 无尽 | 无尽粉 ×1 |

配方基础为 30 EU/t、100 ticks，实际处理受机器加速、超频和全局配方速度配置影响。
凯芙拉需要流体输出空间。其他六种需要物品输出空间，不需要辅料或流体输入。
这些产量是本机的简化规则，不复制原生多原料配方中的转化增产、催化液或副产物。
已有蜂窝配方优先，重复加载不会追加配方；原生机器的配方保持原样。

验证基线：GTNH `2.9.0-RC-1` 开发依赖、GT5 `5.09.54.183`、NewHorizonsCoreMod `2.9.76`。
原生产线见 GT5 的 `NetheriteRecipes`、`RecipeLoaderChemicalSkips`、`GTPPComb`，
以及 CoreMod 的 `DTPFRecipes`。

## 验证

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp -Djava.io.tmpdir=C:/Windows/Temp'
$env:TEMP='C:/Windows/Temp'
$env:TMP='C:/Windows/Temp'
.\gradlew.bat -I scripts/comb-processing-qa.init.gradle runClient17 --no-configuration-cache
```

隔离客户端在完成配方加载后检查七种蜂窝的实际注册、原生匹配、产物数量、输入不足、
扣料和重复加载，并核对原生配方未被修改。报告写入 `build/comb-processing-qa/result.txt`。
测试客户端静音，并通过 `HideFactoryClient.ps1` 隐藏窗口、启用点击穿透和移除任务栏按钮。

2026-10-19：98 项检查全部通过，七种蜂窝均可在真实配方表中匹配并正确扣料。
`compileJava`、`checkstyleMain`、定向 Spotless 格式化及 `git diff --check` 通过。
此回归覆盖配方注册与原生处理查询，未验证完整整合包中的多方块实机运行。
