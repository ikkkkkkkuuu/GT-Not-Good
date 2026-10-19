# 严格配方校验下的 1.6.0 启动修复

原 1.6.0 的过氧化氢配方调用 `Materials.Platinum.getDust(0)`。GT 的材料查询拒绝小于 1 的数量，
返回 `null`；完整整合包启用 `crashOnNullRecipeInput=true` 时，`GTRecipeBuilder.itemInputs`
立即抛出异常，客户端和服务端均在 `postInit` 退出。

修复先查询一份真实铂粉，再用 `GTUtility.copyAmount(0, ...)` 构造不消耗的催化剂。
配方输入、产量、耗时和功率不变，不关闭严格检查。

`LargeChemicalReactorRecipesTest` 强制启用原生严格空值校验，先证明旧写法会抛出异常，再检查实际
加载器生成一条完整配方、铂粉和电路不消耗、流体准确扣除，以及缺少铂粉时拒绝加工。

完整包验证使用 GTNH 2.9.0-RC-1 的实际生产模组、配置和脚本，仅在
`run/production-startup-qa/strict-server`、`strict-client` 隔离副本替换重混淆 JAR；
保留 `crashOnNullRecipeInput=true`，不复制或加载玩家存档。

服务端使用 Java 25.0.2，成功到达 `Done`，继续运行后通过 `stop` 正常保存退出。
结果为 `build/production-startup-qa/server-result.txt`，原生日志位于隔离副本的 `logs/`。

客户端使用原 Prism 实例的 Minecraft、Forge、RFB 和依赖库，以及 Java 25.0.1、
lwjgl3ify 3.0.35、MUI2 2.3.91。离线测试账号使用独立目录、静音设置和限定到测试进程的隐藏窗口助手。
实际加载 330 个模组后，读取 `Minecraft.currentScreen` 确认为原包主菜单
`lumien.custommainmenu.gui.GuiCustom`，`theWorld` 为 `null`。
结果为 `run/production-startup-qa/strict-client/main-menu-result.txt`。

2026-10-19：完整 `check build`、Spotless、Checkstyle 与项目结构检查通过，
611 项测试零失败、零错误，1 项跳过。修复前后双端的日志与隔离生产包验证结果相符。
