# 大型熔炉验证

入口：`scripts/large-furnace-qa.init.gradle`、`scripts/qa/LargeFurnaceClientChecks.java`。
默认独立构建目录是 build/large-furnace-isolated，可通过 largeFurnaceBuildDirectory 指定；本机证据在 D:/CodexTools/GTNGLargeFurnace。

结构砖复用 GTNH 自带方块：64 块 GT 焦炉砖块（sBlockCasings12:0）、102 块 GT 耐火砖块（sBlockCasings4:15）。QA 对每个对应结构位置核对真实方块和 metadata，再验证整机成型；不再注册或制作本模组的重复结构砖。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/Invoke-Gradle.ps1 --init-script scripts/large-furnace-qa.init.gradle --project-cache-dir D:/CodexTools/GTNGLargeFurnace/project-cache -PlargeFurnaceBuildDirectory=D:/CodexTools/GTNGLargeFurnace/build spotlessApply compileJava processResources spotlessCheck runClient --offline --no-configuration-cache --console=plain
```

真实客户端覆盖 716 单元原结构、LV/MV 性能、十级并行仓倍率与 NBT、重复并行仓拒绝、Ω 星阵线程增加及拆除失效、原生铁/金矿同批 128 并行、精确产物、满输出不扣输入、完成但堵塞的产物 NBT/单次回填、断电不推进及供电恢复。GUI 比例 1/2/3、并行仓双端数值同步、关闭重开与结构截图另行断言。线程和并行状态仅显示于 Waila：覆盖服务端快照、活动/停机线程数、实际悬停，以及主界面移除该状态的检查。

早期量子合成和旋转移植尚未编译通过时，曾使用 D 盘源码快照隔离调试，记录保留在 snapshot/excluded.txt；没有回滚工作区。后续 large-furnace-client-final*.log 已恢复直接编译当前工作区全部主源码，不再依赖快照。最终验证结果以 progress.md 和 PASS 报告为准。

客户端通过 `gtng.factory.qa` 与工作区路径限定隐藏 helper，保持静音、透明、穿透、隐藏任务栏。截图和结果在 build/large-furnace-qa；失败和重试保留 .gradle/large-furnace-*.log。最终状态以 progress.md 和 PASS 报告为准。

此入口运行 Java 8，沿用项目 Java 25 / JVM Downgrader 配置，主类使用项目标准降级开发 JAR（沿用启动前快照），QA 类使用 downgradeTestClasses 输出；启动前校验 QA 类版本。

贴图回归：四张并行仓正面原图均为 16×64 动画条带，必须连同原始 `.png.mcmeta` 打包。客户端逐一核对十级仓室实际使用的图集对象、16×16 帧尺寸、动画元数据及四帧数量；控制器 GUI 内展示全部十级物品，独立摆放十级仓室截图，并在资源重载后重复检查。

NEI 与正面换色回归：检查自然加载的配置只向原生 smelting 注册一个大型熔炉 catalyst，未注册到 fuel；实际打开烧制页，检查左侧列表可见且命中大型熔炉，并从机器 U 用途查询找到烧制配方。资源重载后核对两种 16×16 正面图集与62 个金属边框像素透明化（加上原透明像素共 122 个），保存真实待机/运行正面和 NEI 页面截图。
