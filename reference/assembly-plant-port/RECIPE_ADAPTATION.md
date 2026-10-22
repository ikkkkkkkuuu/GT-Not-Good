# 巨型组装厂与超导电磁工厂配方适配

## 来源与许可

官方 `GregTech-Leisure-1.4.5.3.zip`，CurseForge 文件 `9238465`：
<https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465>。
归档 SHA-256 为 `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`。
已读取实际归档根目录 `LICENSE`，其许可为 CC BY-NC-SA 4.0。

另读取 GNSW/GTLCore `6443c848264fb15c90cf50e031f146522616a8b9` 和
GTCEu Modern `e3844ba943e2388c0856617ae4f8b473a0ae1b64` 的 LGPL 3.0 许可证。
不同来源的配方分开保存，各 Java 文件保留对应 SPDX，不归入项目的一般 MIT 授权。
完整许可、源文件哈希、原图与派生连纹清单随 JAR 放在 `META-INF/assembly-plant-port/`。

| 本地文件，均位于 `common/recipe/machine/` | 上游文件与行号 | 许可 |
| --- | --- | --- |
| `MageAssemblerRecipes.java` | 官方 `.minecraft/kubejs/server_scripts/gtceu.js`：2170–2194、3110–3118、11998–12017 | CC BY-NC-SA 4.0 |
| `SuperconductingElectromagneticRecipes.java` | 同上：2248–2269、17211–17215 | CC BY-NC-SA 4.0 |
| `AssemblyPlantCasingRecipes.java` | GTCEu `src/main/java/com/gregtechceu/gtceu/data/recipe/misc/GCyMRecipes.java`：209–223；另有本地原生方块外观转换 | LGPL 3.0 |
| `LightningProcessorRecipes.java` | GTLCore `src/main/java/org/gtlcore/gtlcore/data/recipe/Misc.java`：192–198 | LGPL 3.0 |

参考检出、离线生成器和上游构建脚本不参加编译或资源打包。

## 加工图

巨型组装厂保留三种可切换模式：

| GTL 配方图 | GTNH 5.09.54.205 配方图 |
| --- | --- |
| `ASSEMBLER_RECIPES` | `RecipeMaps.assemblerRecipes` |
| `PRECISION_ASSEMBLER_RECIPES` | `GoodGeneratorRecipeMaps.preciseAssemblerRecipes` |
| `CIRCUIT_ASSEMBLER_RECIPES` | `RecipeMaps.circuitAssemblerRecipes` |

精密组装采用 GoodGenerator 的精密组装图，不使用 BartWorks 电路装配线图。
保留原生配方输入、流体、电压和精密等级要求；不把不同配方图串联。
原生 `MTEPreciseAssembler` 通过 `recipe.mSpecialValue <= casingTier + 1` 检查精密等级。
原生共有不精密单元和 T1–T4 四种精密电子单元，最高结构能力为 4，不能解释成五级精密能力。
本地固定结构提供等级 4，并仍检查配方电压；高于 4 的外部精密配方不能运行。

超导电磁工厂保留磁化、电磁分离、电解、电弧炉四种原生图，以及新增闪电处理图。
它们分别对应 `polarizerRecipes`、`electroMagneticSeparatorRecipes`、`electrolyzerRecipes`、
`arcFurnaceRecipes` 和 `GTNGRecipeMaps.LightningProcessorRecipes`。
两台机器兼容标准 GT 能源仓与特殊能源仓，包括原生无线能源仓。
控制器不提供独立无线电网模式，也不直接从拥有者的能源账户扣费。

## 控制器

巨型组装厂保留 UHV、800 tick、16 类物品输入和四路流体输入：

| 上游材料 | 本地对应 |
| --- | --- |
| 16 台大型组装机、16 台大型电路组装机 | 16 台原生 UV 组装机、16 台 UV 电路组装机；按加工功能替代现代多方块控制器 |
| 16 个精密电路装配机器人 Mk1 | 16 个 UV 机械臂；GTLCore `AdvancedMultiBlockMachineA.java:300–303` 明确 Mk1 对应 UV、Mk2 对应 UHV |
| 32 个 UV 流体调节器、64 个 UV 机械臂、32 个 UHV 电路 | 原生同级物品及电路矿辞 |
| 碳化钨、钇钡铜氧、镀铑钯、硅岩合金螺丝，各 64 个 | 原生同名材料；镀铑钯使用 BartWorks `RhodiumPlatedPalladium` |
| RTM 合金长杆 32 根 | GT++ Incoloy MA956 长杆 32 根，不新增只用于一个配方的合金 |
| 碳化钨、Ultimet 长杆，各 32 根 | 原生同名材料 |
| HSLA 钢长杆 32 根 | 原生钨钢长杆 32 根；原生在没有 RotaryCraft 时禁用 HSLA 物品，保留长杆形态、材料数量及更高成本 |
| 拉菲姆板 16、双重铱板 8 | GT++ 拉菲姆板和原生双重铱板 |
| 变异活性焊料 5760 mB、坎塔尔合金 5760 mB | 原生 GT++ `MaterialMisc.MUTATED_LIVING_SOLDER` 和原生坎塔尔熔体 |
| 诺贝尔 2880 mB、铹 2880 mB | 原生镄与 GG 亚稳态鿫，各 2880 mB；保留高阶核材料成本，不新增缺少下游的元素 |

超导电磁工厂保留 UEV、1200 tick、13 类物品输入和四路流体：

- 4 台大型电解、4 台大型电磁、4 台大型电弧机器分别使用原生 GT++ 工业电解机、工业电磁分离机、工业电弧炉。
- 保留 16 个 UV 发射器、8 个 UEV 电路和四组各 64 根细导线。
  现代 ZPM 超导体细线映射为原生 `SuperconductorZPMBase` 细线，不声称材料化学组成相同。
- 钽、银、硅双重板各 8；实际客户端矿辞确认钌双重板未注册，使用原生钌板 16 保持相同材料总量。
- Indalloy 140 和变异活性焊料各 2880 mB、焊锡 5760 mB 保留同名原生材料。
  现代 UHV 超导基材熔体映射为原生 `SuperconductorUHVBase` 5760 mB。

两个控制器的现代研究站研究转换为原生扫描仪研究：分别扫描本地精密组装机和已移植超导线圈，
耗时 2400 tick，使用各自原始 UHV／UEV 电压。没有把现代 CWU 数值错误当成原生扫描时长。

## 新结构件

高级装配线单元保留原始九宫格：一个原生装配线机械方块、两个 UV 电路、四块 HSS-G 板、两个铑齿轮。

时空装配单元保留 UHV、400 tick、原始输入数量及四路流体用量。
现代电路装配线机械方块替换为 **GG `ItemRefer.Precise_Electronic_Unit_T4`**；每个时空单元消耗一块，
巨型组装厂的 27 个对应结构位置因此需要 27 块原生最高精密电子单元。
该原生单元在 UHV 组装机制造，需要 UEV 机械方块、UV 场发生器、T4 高能电路和 T3 精密电子单元等，
不是用便宜装饰块直接解锁最高精密等级。原生来源为 `goodgenerator/loader/RecipeLoader2.java:1233–1246`。

时空单元的其他映射：Mk2 机器人使用 UHV 机械臂；锇纳米蜂群使用中子素纳米蜂群；
六个光学处理核心使用六枚原生光学芯片。保留 Adamantium 框架、两个 UEV 电路、蓝钢与红钢齿轮各四个、
四个 ZPM 机械臂及六块双重 Pikyonium 板。液晶凯夫拉 5760 mB、Pikyonium 1296 mB 使用原生同名材料；
Enderite 与高耐久复合钢分别使用 GT++ Abyssal 与 Incoloy MA956，各 1296 mB。
研究物为高级装配线单元，扫描耗时 2400 tick、UHV。

其余六种新增结构件：

| 结构件 | 生存配方 |
| --- | --- |
| 层压玻璃 | 原生化学级玻璃 1:1 转换 |
| 过滤机械方块 | 原生过滤机械方块 1:1 转换 |
| PTFE 管道机械方块 | 原生 PTFE 管道机械方块 1:1 转换 |
| 钢化玻璃 | 原生硼硅玻璃 1:1 转换 |
| 电解单元 | 保留铂双导线 4、钨单电缆 1、IV 电路 1；原生耐化学腐蚀机械方块替代现代绝缘外壳，16 EU/t、50 tick、产出 2 |
| 二硅化钼线圈 | 原生钼环 32、硅粉 16、石墨烯粉 4、钨钢熔体 144 mB，HV、500 tick、产出 1 |

四种外观转换使用电路 24、16 EU/t、50 tick，不免费生成结构件。
GTNH 无独立二硅化钼材料或同名原生线圈，因此将原来的 32 个二硅化钼环拆成
32 个钼环（8 单位钼）与 16 单位硅，保持 MoSi2 的元素配比，不增加孤立材料或额外冶炼树。
原生石墨烯不注册箔片，因此用四份石墨烯粉替代十六张箔片，按每张箔片四分之一单位保持材料总量。
原生虽然注册 HSLA 熔体，但 `ProcessingModSupport.java:17–19` 在没有 RotaryCraft 时禁用 HSLA 物品，
当前 GT5 5.09.54.205 与 NewHorizonsCoreMod 2.9.84 源码没有 HSLA 熔体的独立生产配方。
为保证正常 GTNH 环境中线圈可制作，使用同为 144 mB 的钨钢熔体，保留 HV、500 tick 和其他输入。
原生 `MixerRecipes.java:388–397` 用钨粉与钢粉生产钨钢粉；钨钢锭可通过
`ProcessingIngot.java:71–74` 注册的反向流体熔炼取得熔体，具有完整的原生获取路径。
铱机械方块、拉菲姆机械方块、改良超导线圈和加速管道复用已有移植方块与生存配方。

## 闪电处理范围

| 来源 | 输入 | 输出 | 功耗与时长 |
| --- | --- | --- | --- |
| GTLCore `Misc.java:192–198` | 铍粉 1、钾粉 4、氮气 5000 mB、非消耗电路 1 | 末影珍珠粉 10 | LV、400 tick |
| 官方 `gtceu.js:17211–17215` | 空气 10000 mB | 一氧化氮 1000 mB | 120 EU/t、200 tick |

两条都使用现有物品与流体，没有添加只用于展示的新材料。
暂不迁移神秘锭粉配方，以免额外引入 Thaumcraft 进度旁路。
稀土、精金、末地空气等其他闪电配方依赖现代材料或专属中间产物，本次不替换成不同物质来凑配方数量。

## 注册阶段与验证

普通配方阶段调用四类的 `loadRecipes()`。
GT++ 重流体注册完成后，再调用 `MageAssemblerRecipes.loadPostInitRecipes()` 和
`SuperconductingElectromagneticRecipes.loadPostInitRecipes()`，注册控制器与时空装配单元。
这些类不注册新材料、流体、机器或配方图。
缺少关键材料形态或流体时直接报告缺失，不允许原生配方构建器静默丢掉相应成分。

本轮客户端按实际矿辞列表逐下标核验 20 种关键材料形态，同时记录全部注册条目与实际配方输入，
避免将定制矿辞列表的流式遍历结果误当成注册情况。缺失形态的替代及对应成本见控制器一节。
材料审计不代表整套客户端联合检查已经通过。

## 洁净室条件

官方整包 `gtceu.yaml` 启用洁净室，且 `cleanMultiblocks: false`；同 pin GTLCore 的
`CleanroomConditionMixin` 因此仍要求配方对应的洁净室供给。过滤机械方块与层压玻璃不会自动提供洁净室。
GTNH 通用多方块配方路径不会自动检查此条件，本地对原生 `mSpecialValue` 为 -200/-300 或
`CLEANROOM` metadata 为真的配方，通过控制器的原生洁净室 capability 校验有效洁净室且洁净度大于零。
生产控制器遵循现有 `CleanroomReference` 配置及项目默认洁净室豁免，不额外引入随机产物损失。
QA 为覆盖未豁免的门槛，临时设置 `Config.requireCleanroom=true` 并关闭
`FakeCleanroom.CLEANROOM_BYPASS`；检查缺少提供者时不扣料及有效提供者解锁加工，
最后在 `finally` 清除测试提供者并恢复两个配置原值，不改变生产默认设置。
