# GT-Not-Leisure 外部物品引用核对记录

来源为 [GT-Not-Leisure](https://github.com/ABKQPO/GT-Not-Leisure/tree/9c6292d9ef2b8750fce0a6258a48293f8a4cb1f7) 的 `src/main/java`（提交 `9c6292d9ef2b8750fce0a6258a48293f8a4cb1f7`，LGPL-3.0）。扫描固定模组 ID、注册名与元数据的 `GTModHandler.getModItem` 调用，并补入四个固定注册表引用；动态计算的引用不在清单内。原始扫描有 533 组，统一模组 ID 后去掉 3 组重复引用，再排除与本项目原有条目重合的 10 组，新增 520 项。此外，本项目单独收录末影蓄水槽（`EnderStorage:enderChest`，元数据 1）；纸和红石使用 Minecraft 原版物品直接引用，不列入外部物品枚举。`ModsItemlist` 合计 541 项。

每项中文名称参照 [GTNH-Translations](https://github.com/GTNewHorizons/GTNH-Translations/tree/19a75945e59cf4d17ee2d94303bef0381bfe5ea2)（提交 `19a75945e59cf4d17ee2d94303bef0381bfe5ea2`）、模组 `zh_CN.lang` 和对应子物品的元数据定义。

枚举保留上游使用的注册标识；其中尚未接入本项目配方的条目只作为集中清单。模组版本更新后应重新核对注册名和元数据。
