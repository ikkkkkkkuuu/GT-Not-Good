# 加载末尾配方索引内存溢出

2026-10-24，GTNG 1.6.8 / GT5 5.09.54.205 / NewHorizonsCoreMod 2.9.84。

## 根因和实测

客户端在 99% 卡住；用户启动的服务器 PID 76220 使用 `-Xms6G -Xmx6G`，
最终控制台明确报告 Server thread 和 Netty IO 的 OutOfMemoryError。
只读 `jstat -gc` 显示老年代接近 100%，Full GC 持续增长。
`jcmd GC.class_histogram -all` 中 GTRecipeLookupBranch 为 93,069,983 个 / 2,978,239,456 字节，
GTRecipeLookup$Node 为 104,290,108 个 / 2,502,962,592 字节，合计约 5.10 GiB。

GTNG 的 TentacleAssemblyRecipes 在 GT 完成加载回调中导入原生装配线配方，
将多选原料转换成 GTRecipe_WithAlt，再调用普通 RecipeMapBackend.compileRecipe。
固定 GT5 的 GTRecipeLookup.addRecursive 为每个原料组的每种键递归构建独立子树，
不同组产生笛卡尔积。物品键和材料数据键也会形成分支，路径数不等于真实配方数量。

决定性的异常配方是 **Shuttle Schematic**，注册物品为
`GalacticraftAmunRa:item.schematic`。来源是固定 NewHorizonsCoreMod 的
`com/dreammaster/scripts/ScriptAmunRa.java` 506–531 行：钐、塔塔里特、镉、铯、镧、铈六项
`createOreVariants(material, 64)` 输入，各自允许多种母岩矿石形态。
导入时实际索引组大小为：

```text
20,20,22,22,22,22,2,2,2,2,2,1,1,1,1,1,1,1,1
20² × 22⁴ × 2⁵ = 2,998,476,800 条完整路径
```

诊断时完整整合包中共有 29 条导入配方超过 4096 条索引路径；其他配方包括高阶马达、力场发生器、
能源仓和聚变部件等，远小于穿梭机图纸，不能把它们全部描述成必然 OOM。
`nether_fungus` 未注册报错之后碎片世界的 239 条配方已完成注册，它不是本次索引爆炸的原因；
该独立兼容问题本轮未修改。

源码证据为本机缓存的 GT5 sources JAR（SHA-1 `5447ea903c5833f0319123e38ac586f348e599f3`）
及 NHCore sources JAR（SHA-1 `c14bcf0a3211799a537f53e8f44a302a10e1a547`）。
原日志、线程栈、直方图及提取的只读源码位于 `.gradle/memory-diagnosis/`。

## 修复边界

按用户要求，直接在 `TentacleAssemblyRecipes.convert` 固定这条图纸的原料。
确认 GalacticraftAmunRa 已加载且产物匹配已有 `ModsItemlist.ShuttleSchematic` 后，
保留原生 `mInputs` 的默认物品和数量，导入副本的全部 `mOreDictAlt` 槽位留空。
这样不再展开这条配方的多母岩矿石及其他替代原料组合。
流体、产物、耗时、电压不变，原生装配线配方自身和其他导入配方的替代原料保持原样。

此前尝试的通用紧凑索引方案已撤回：删除新增的 `RecipeAlternativeIndex`、
`RecipeLookupAlternativesMixin`、对应单测和 loader 注册，最终修复不新增或修改 Mixin。
原有其他功能 Mixin 保留。不升级依赖、不提高用户内存上限。

## 验证与复现

```powershell
& scripts/dev/Invoke-Gradle.ps1 -GradleArguments @('-I','scripts/recipe-lookup-check.init.gradle','-PrecipeLookupBuildDirectory=D:/CodexTools/GTNGMemory-20261023/build','--project-cache-dir','D:/CodexTools/GTNGMemory-20261023/project-cache','spotlessApply','compileJava','processResources','spotlessCheck','assemble','--offline','--console=plain')

& scripts/dev/Invoke-Gradle.ps1 -GradleArguments @('-I','scripts/recipe-lookup-check.init.gradle','-I','scripts/recipe-lookup-qa.init.gradle','-PrecipeLookupBuildDirectory=D:/CodexTools/GTNGMemory-20261023/client-build','--project-cache-dir','D:/CodexTools/GTNGMemory-20261023/client-cache','spotlessApply','runClient25','--offline','--no-configuration-cache','--console=plain')
```

- 最终构建 `shuttle-fixed-build.log` 退出 0、31s，编译、资源生成、Spotless、普通 JAR 打包通过。
  相同任务 `shuttle-fixed-build-repeat.log` 退出 0、3s；再次执行
  `shuttle-fixed-build-warm.log` 退出 0、998ms，31 UP-TO-DATE，configuration cache reused。
- Java 25 隔离客户端 1m18s、退出 0，448 条原生装配线配方的默认输入、数量、产物、
  时间、电压及 reInit 后配方保留通过。
  实际装配/电路加工、低级/UIV、乱序输入、缺料无损拒绝、缓存重复、NBT 恢复、原生 ME 样板总成精确产出通过。
  结果 `client-build/memory-tentacle-qa/result.txt`；日志 `shuttle-fixed-client.log`。
  开发客户端不含 AmunRa；具体穿梭机配方的加载与内存行为由下面的完整服务端覆盖。
- 用户完整服务器的 mods/config/scripts/libraries 复制到 D 盘独立目录，未复制用户世界。
  新世界绑定 127.0.0.1:25575，使用修复普通 JAR、Java 25、`-Xms512M -Xmx6G`。
  最终固定原料版本 PID 58104，30.02s 启动完成，采样已用堆 1,901,742 KiB（约 1.81 GiB）。
  出现 Done、正常保存和退出 0；两类查询树节点合计 183,605,928 字节（约 175.10 MiB）。
  `-all` 直方图包含待回收对象，不作为精确保留堆或长期泄漏基准。
  最终证据 `qa-server-result.json`、`qa-server-heap.txt`、`qa-server-histogram.txt`、`qa-server-console.log`。
- 历史紧凑索引服务端结果移入 `superseded-compact-index/`，不是最终方案的验证依据。
  中间失败日志保留：JDK Unix socket 长路径通过进程级短路径解决；已撤回方案的人工用例曾因
  GTRecipe.copy 未复制 category 触发 OOM，修正夹具后通过。最终方案不含该人工用例或索引实现。
- 构建插件曾在离线模式仍请求固定整合包 manifest；将已有且哈希一致的固定 manifest 复制到
  elytra-conventions 读取的本机缓存后成功。未修改依赖版本或生产构建配置。
- 结构检查通过：1586 Java / 3414 imports / 161 mixins / 179 reserved / 385 QA paths；
  `git diff --check` 通过。`jar-audit.json` 确认普通 JAR 无 QA 类、无已删除的索引/Mixin 类，
  修改类基础字节码版本为 52。普通 JAR SHA-256：
  `C5D11F4E427AB79BA37FC1E5A8AC11DE18B8E1B173B3B59CB44C2BD28FF1309A`。
- 第三方启动警告和客户端退出线程异常文本保留。不声称本轮逐项实测了每条配方的所有可替代物品。

最终客户端实测使用上述 `scripts/recipe-lookup-qa.init.gradle`。
QA 客户端静音并由精确工作区标记隐藏，测试客户端及服务端均已退出。
普通 JAR 不包含 QA 类；用户原客户端和服务器目录未替换文件，未提交或发布。
