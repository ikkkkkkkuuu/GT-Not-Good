# 电路装配线封包验证

## 接口与拒单检查

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing'
.\gradlew.bat -I scripts/circuit-assembly-qa.init.gradle runClient17 --no-configuration-cache
```

使用独立的 `run/circuit-assembly-qa` 目录和临时世界；客户端静音、透明、鼠标穿透且不显示任务栏窗口。运行前快照主代码及 QA 类，测试模组、配方和参考源码不进入发布文件。

结果为 `build/circuit-assembly-qa/result.txt`，核心截图为 `build/circuit-assembly-qa/screenshots/circuit-assembly-core.png`。

检查覆盖：

- 原版左键铭刻、刻印消耗、永久绑定和 NBT 恢复；CAL／电路组装机模式各自的 Crafting Input Buffer 支持。
- 原生终极编码样板保存与读取、原生流体数量、重复材料按总线顺序拆分。
- 未铭刻、错铭刻、错模式、机器忙碌、未成型、禁用、仓室占用、ME 输入总线及材料数量错误时的原子拒单。
- 真实供应器发布样板、创建订单回执、原生 CAL 配方检查与耗料，以及普通输出总线到供应器回收栏的移交。
- 派单后切换电路组装机模式仍可回收成品；新核心的注册、无序合成配方与物品渲染。

机器夹具使用真实控制器与仓室，并设置成型状态；输出阶段由测试调用原生输出方法，再验证回收。该检查不代替完整结构搭建、持续供电到自然完工、AE CPU 全流程或专用服务器测试。

2026-10-19：`compileJava`、`processResources`、本次 Java 格式检查及隔离客户端检查通过。客户端以原版 early loader 加载，输出 `PASS`；已检查核心截图。日志含整合包既有警告和退出阶段后台线程异常，不能视为无警告运行。

## 完整机器与 AE 订单检查

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing'
.\gradlew.bat -I scripts/circuit-assembly-qa.init.gradle -PcalEndToEnd runClient17 --no-configuration-cache
```

使用独立的 `run/circuit-assembly-e2e` 目录与临时世界，仅在该目录关闭配方提速，并加载注册真实 CAL 配方的 NHCore。测试使用原版微处理器配方和永久铭刻，不注册替代配方。

检查实际壳体、EV 玻璃、维护仓、普通输入输出仓和能源仓自然成型；原生调试发电机经真实绝缘电缆供电。原生 AE 网络包含物品／流体存储元件、合成 CPU、终端和封包供应器，通过真实合成计算与提交接口下单三份配方，等待原版机器自然加工和成品返回实体 ME 存储元件。

测试不会设置成型标记、手调加工进度、直接派单或注入成品。运行期间短暂停供并恢复，记录普通能源仓实际耗电；最终核对订单完成、准确产量、原料耗尽及机器／供应器仓室清空。

结果与证据：`build/circuit-assembly-e2e/result.txt`、`report.json`、`events.log`、`screenshots/circuit-assembly-full.png`。成功后保存的世界名写入 `save-name.txt`，世界保留在独立运行目录的 `saves/` 下。

2026-10-19：完整检查通过。五片结构自然成型，微处理器配方连续加工三批，每批 32 个、600 tick、600 EU/t，共观察到 1800 个加工 tick。216 mB 熔融焊锡及三批物品原料全部耗尽，96 个微处理器全部返回实体物品存储元件；输入输出仓、供应器回收栏和订单回执均清空，永久铭刻保留。

订单完成以实际 CPU 的原生完成回调、准确产量和清理状态验收：完成数量 96，CPU 空闲、已用字节和剩余物品均为 0。独立订单的链接没有 nexus，不能以其 `isDone()` 作为完成依据。断开调试发电机四 tick 时，能源仓减少 1473 EU，恢复后继续自然加工。

存档中控制器坐标为 `(8, 12, 8)`，封包供应器为 `(3, 10, -3)`，ME 驱动器为 `(1, 10, -3)`。测试关闭该独立目录的 NHCore 退出确认，正常保存并退出。退出后直接读取磁盘区块 NBT，再次确认三次加工、原刻印、实体物品元件中的 96 个成品，以及空流体元件和空供应器任务；解析结果为 `build/circuit-assembly-e2e/save-inspection.json`。

`compileJava`、`processResources`、完整 QA 类编译、Java 格式检查和 `git diff --check` 通过；已检查本轮机器截图。此检查未覆盖专用服务器和存档重载回归。
