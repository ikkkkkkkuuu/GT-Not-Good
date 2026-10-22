# Ω-天球分歧引擎

原类是 GTLAdditions `ThreadPartMachine.kt`，固定提交
`34fb152886164c56a07fc9d1b10e056f0c22b7ab`，GPL-3.0-only。
注册见 `GTLAddMachines.kt:304-317`，获取配方见 `data/recipes/PartMachine.kt:264-286`；原文件保存在 `upstream/gtladditions/`。
目标为 `common.machines.hatch.lighthunter.SatelliteThreadModifierHatch`，独立于参数仓和执行核心。

原仓只有一个可取放物品槽，只接受普通星阵，默认最多 64 个；不接受压缩星阵，也不消耗存入的星阵。
额外线程为 `saturatedInt(n × 64 × m)`，不可共享。原倍率为
`round(min(1 / durationMultiplier, 4096) × machineMultiplier)`；四种卫星工厂未列入特例表，机器倍率为 2。
GTLCore 参考快照 `6443c848264fb15c90cf50e031f146522616a8b9` 的配置默认 `durationMultiplier=1`，
因此按原默认值移植 `m=2`。GTNH 版本不额外引入该全局配置，也不提供每仓自定义倍率的输入框。

| 普通星阵 | 额外线程 | 加上原基础 128 线程 |
| ---: | ---: | ---: |
| 0 | 0 | 128 |
| 1 | 128 | 256 |
| 64 | 8192 | 8320 |

界面只读显示当前倍率和额外线程，并提供原单槽的存取操作。未绑定有效、成型卫星工厂时显示 0。
`bind(factory)` 拒绝另一座仍有效工厂的共享；`getAdditionalThreads(factory)` 同时核对工厂反向持有关系、
成型状态及双方 TileEntity 仍为世界中的同一实例。绑定不写入 NBT，库存由 GTNH 原生保存与拆除掉落处理。
失效、解绑或载入后须重新通过真实结构绑定。额度和批次分配由控制器读取，仓室不执行配方。

外观保留原 `create_casing` 外壳，复用 `HeartCasing.CreateCasing` 的同源 GTLCore 原图。
正面为原 `thread_modifier_hatch.png`，16×128、8 帧、每帧 7 tick；原平面模型 `emissivity=15`
改用 GTNH 的正面自发光叠层。GUI 的 GTCEu/LDLib API 改写为 MUI2，保留两项只读数值和可取放槽。
原图、元数据、源码、模型、许可与修改见 `THREAD_MODIFIER_MANIFEST.json` 及随包同名清单。
原始源与离线工具不参加编译或打包，外部资产和改编源码不属于项目通用 MIT 许可。

获取配方映射及其材料数量适配见 `../light-hunter-port/ITEMS_RECIPES.md`；构建、库存操作和客户端结果由统一 QA 补录。
