# 装配线封包核心手动测试端

启动独立测试客户端：

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing'
.\gradlew.bat --no-daemon -I scripts/assembly-playground.init.gradle runClient
```

默认每次启动创建新的 `assembly-manual-*` 创造存档，不覆盖现有存档。测试构建快照与其他 playground 分离。
存档名记录在 `run/client/assembly-playground-save.txt`，准备结果记录在 `run/client/assembly-playground-ready.txt`。

- 普通装配线控制器：`0, 10, 0`；进阶装配线控制器：`0, 10, 16`。
- 每套机器前方有独立 ME 网络、合成 CPU、物品和流体元件、合成终端、样板终端、封包供应器。
- 两台机器安装同一个真实 GT 配方的数据棒，供应器已装对应核心、终极编码样板并绑定控制器。
- 产物为真实配方的 LuV 电动马达，每套网络备有 32 份原料，使用 LuV 能源舱。
- 直接在对应 ME 合成终端下单。观察各段输入总线、流体舱和输出总线，确认产物回到网络。
- 背包提供连接器、两种核心和备用供应器。重新连接时，普通右键供应器选择，再 Shift 右键机器控制器绑定。

测试模组只在这个启动入口加载，并持续给当前结构内的能源舱补电；实际配方仍由原机器执行。
准备完成后客户端保持打开；测试模组不会自动下单。测试代码和供电逻辑均不进入发布 JAR。

启动时增加 `-PassemblyBenchmark=true` 可在新测试存档中自动对两条线各下单 8 个 LuV 电动马达，检查多份任务并行、连续运行及实际回网数量。结果写入 `run/client/assembly-benchmark-result.txt`；此模式不能与恢复旧存档同时使用。

继续原测试存档时，在启动参数加入 `-PassemblyPlaygroundResume=assembly-manual-存档编号`。恢复模式保留建筑、库存及现有订单，不重新铺设测试台。含流体的配方使用新版 AE 终极编码样板，封包核心读取其原生流体数量。
