# 超级粒子对撞机结构

当前来源：用户指定的 GregTech Leisure-增强版 实例，GTL-Enhancedcore-3.8.0.jar，作者 S_Fast。
JAR SHA-256：`555a9514c4d2fd11a029c091ac053d5cf7c52fba62411f2f043e04454094f7aa`。
归档 META-INF/mods.toml 声明 LGPL-3.0；未确定对应源码提交，不冒认 Git 修订。
完整来源清单：`META-INF/particle-collider-port/ENHANCED_STRUCTURE.json`。

此前 47×7×47 是 GTLCore 6443c848264fb15c90cf50e031f146522616a8b9 的普通版本。
截图对应增强核心镂空巨构：197×83×209，共 302,625 个非空位置（含控制器）。
截图的 45,634 超导线圈、1,992 ZPM 外壳、654 钨钢管道、7,774 玻璃、19,250 加速管道逐项匹配。
实心版数量不同，未采用。

## 坐标与资源

源 `data/gtl_enhancedcore/structures/hollow/gtl/super_particle_collider.pattern.gz` 原字节复制到
`assets/gtnotgood/structures/super_particle_collider.pattern.gz`。
压缩 SHA-256：`129ead53fed01d9407e8d7a2c355005b2ee3e245610120878180a0e3b0a85eda`。
解压完整 ASCII（含尺寸首行和末尾 LF）SHA-256：
`01023570c9f99f8d7ac6c106347ff0b8d3066cf089371ec65a134c1aa6e37b96`。

javap 核对了 StructureData.read/forEachAisle、StructurePatterns.start 的读取顺序。
源控制器 (93,41,208)，仅逆转深度、@ 换为 ~，转成 StructureLib 横层数组。
偏移 (93,41,0)，控制器距底层 41 格；朝北时相对覆盖 x=-93…103、y=-41…41、z=0…208。
24 仓室位保持正面 x=91…95、行=39…43 的 5×5 区域，中心为控制器。
3,114,734 个空格保持 any 语义。首次加载校验尺寸、字符、控制器及总块数并缓存。

## 方块映射

GT5 固定 5.09.54.205，ProjectRed 固定 4.12.47-GTNH。
原生对应物使用原生外观；几何完全保留。

| 字符 | 数量 | GTNH 结构件 |
| --- | ---: | --- |
| A | 173,483 | `ParticleColliderCasing.Lafium` |
| B | 7,556 | `Materials.NaquadahEnriched frame` |
| D | 42,995 | `ItemList.RadiantNaquadahAlloyCasing` |
| E | 45,634 | `ItemList.Casing_Coil_Superconductor` |
| F | 7,774 | `Casings.ChemicalGradeGlass` |
| G | 14 | `ProjRed|Illumination:projectred.illumination.lamp:25` |
| I | 4 | `ProjRed|Illumination:projectred.illumination.lamp:17` |
| J | 19,250 | `ParticleColliderCasing.AcceleratedPipeline` |
| K | 3,244 | `ItemList.Casing_Fusion2` |
| L | 1,992 | `ItemList.Casing_ZPM` |
| N | 654 | `ItemList.Casing_Pipe_TungstenSteel` |

路菲恩外壳和加速管道没有原生同类结构件，复用本项目已有移植，未重复注册。
硅岩合金外壳采用原生辐射硅岩合金机械方块；夹层玻璃沿用项目既有的一比一外观对应关系，采用原生化学级玻璃。
这些是 GTNH 对应物，不宣称图像与现代版相同。灯采用原生反转灯。
旧移植线圈 ID/资源保留；装配机：旧线圈 + 电路 6 → 原生超导线圈，1:1、50 tick、16 EU/t。
旧小结构需重建；控制器 ID、机器处理和模块仓规则保留。

## 验证

结构单测核对全部字符数量、原数据哈希、尺寸、控制器/仓室位置和数组隔离。
实机入口与范围见 docs/testing/particle-collider.md；旧移植线圈发光不属于新结构验证。
