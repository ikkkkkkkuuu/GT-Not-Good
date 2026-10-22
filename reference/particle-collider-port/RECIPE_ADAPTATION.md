# 超级粒子对撞机配方适配

配方来源为官方 `GregTech-Leisure-1.4.5.3.zip`，CurseForge 文件 `9238465`：
<https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465>。
已读取压缩包根目录 `LICENSE`，其许可为 **CC BY-NC-SA 4.0**。
下表行号来自该压缩包的 `.minecraft/kubejs/server_scripts/gtceu.js`，不对应其他维护分支。
改编文件 `src/main/java/com/xyp/gtnotgood/common/recipe/machine/SuperParticleColliderRecipes.java`
保留同一许可，不属于项目的一般 MIT 授权。

| 上游行号 | 上游内容 | 本地内容 |
| --- | --- | --- |
| 2700–2709 | 控制器组装线与 ZPM 研究 | ZPM 800 tick 组装线；以加速管道为研究物，原生扫描器研究 1200 tick、ZPM |
| 10408–10414 | 加速管道 | IV 400 tick、洁净室、288 mB 焊锡；用原生材料替代现代版专属材料 |
| 10416–10421 | 拉菲姆机械方块 | 富集硅岩框架、6 块拉菲姆板、电路 6，16 EU/t、50 tick，产出 2 块 |
| 8016–8039 | 16 条元素碰撞转化 | 仅保留具备原生材料和流体的 5 条；用量、UV 电压、200 tick 均保留 |
| 12766–12802 | 正电子至反物质链、混合反氢、反物质丸固化 | 本次不迁移，原因见下文 |

## 原生材料映射

| GTL | GTNH 5.09.54.205 |
| --- | --- |
| 钚、钍、铀 | `Materials.Plutonium/Thorium/Uranium.getMolten` |
| 镤、镎、锔、锎、镄 | GT++ `MaterialsElements.PROTACTINIUM/NEPTUNIUM/CURIUM/CALIFORNIUM/FERMIUM.getFluidStack` |
| 氦等离子体 | `Materials.Helium.getPlasma` |
| 拉菲姆 | GT++ `MaterialsAlloy.LAFIUM`，板与双重板沿用原生材料 |
| Uranium Rhodium Dinaquadide 16 倍线缆 | 原生 ZPM 超导线 `Materials.SuperconductorZPM`；按电压阶段映射，不声称化学组成相同 |
| Copper-76 粉 | 原生铜粉；不为一个结构配方新增无其他用途的铜同位素 |
| 铕四重流体管道 | 原生铌钛四重流体管道；当前 GTNH 确实注册该管道 |
| 氮化铌单电缆 | 原生铌钛单电缆；当前 GTNH 未注册氮化铌电缆 |
| LuV 电压线圈、UV 电路、反射板与 ZPM 元件 | 原生 `ItemList` 和电路矿辞 |

控制器保留 16 个 ZPM 场发生器、16 个 ZPM 电动泵、256 枚 UHPIC、16 组 16 倍 ZPM 超导线、
32 个 UV 电路、16 块中子反射板与 8 块双重拉菲姆板。
四种输入流体均保留 1152 mB：焊锡、镤、铀、镎。

GTL 的原生超导线圈不是整合包脚本新配方。为保留它的外观并沿用 GTNH 已有造价，
本地额外提供原生 `ItemList.Casing_Coil_Superconductor` 到移植线圈的 1:1 装配机转换，
电路 6、16 EU/t、50 tick；不生成材料或免费线圈。

方块配方在 `loadRecipes()` 注册。控制器和碰撞配方在 `loadPostInitRecipes()` 注册，
等待 GT++ 原生流体完成注册；不创建替代或重复流体。

## 保留的生产配方

每条均输入 4096 mB 母材和 4096 mB 氦等离子体，产出 4000 mB 子材，使用 UV、200 tick。

| 母材 | 子材 |
| --- | --- |
| 钚 | 锔 |
| 钍 | 铀 |
| 铀 | 钚 |
| 镤 | 镎 |
| 锎 | 镄 |

这些流体使用现有 GTNH/GT++ 对象。原生 `RecipeLoaderGTNH` 已用锔熔体合成锎，
`RecipeLoaderNuclear` 使用锔与镅等离子体；`ResearchStationAssemblyLine` 直接消费锔、镎、镄熔体。
它们还可进入原生 Godforge 材料处理；没有添加只用于展示的新流体。
不把 GT++ 粒子物品、Lanth 束流粒子或原生大型强子对撞机元数据当作同名流体替代。

## 本次未迁移

11 条元素转化为：Am→Bk、Cm→Es、Bk→Cf、Es→Md、Fm→No、Md→Lr、Bi→At、
Mt→Rg、Ds→Cn、Rg→Nh、Db→Bh。这些链条至少一个端点没有当前 GTNH 中的可用材料对象。
`gregtech.api.enums.Element` 中存在元素名，不代表已注册对应材料、流体和下游配方。
本次不添加一组缺乏后续用途的孤立材料，也不把不同元素相互替换。

正电子、反质子、反中子、反氢没有与 GTL 流体直接等价的原生注册。
GTNH 的 `Materials.Antimatter` 明确命名为 **Semi-Stable Antimatter**，由 GoodGenerator
反物质锻炉与反物质发生器使用。其发生器具有原生的高能量、非线性发电机制；
直接把 GTL 的 UV 碰撞产物映射到该流体，会改变原生反物质来源与进度。
因此本次不迁移反物质支线及反物质丸，也不新增没有完整下游的中间流体。

原生核对来源是当前 Gradle 依赖 `GT5-Unofficial-5.09.54.205-sources.jar` 中的
`gregtech/api/enums/Materials.java`、`gregtech/loaders/materials/MaterialsInit.java`、
`gtPlusPlus/core/material/MaterialsElements.java`、`MaterialsAlloy.java`、`Material.java`、
`goodgenerator/blocks/tileEntity/AntimatterGenerator.java` 和原生粒子实现。
这些文件仅用于核对 API 与材料身份，没有复制其实现到配方类。
