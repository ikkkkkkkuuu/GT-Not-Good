# 超级粒子对撞机客户端检查

使用独立世界和目录运行，客户端由 `HideFactoryClient.ps1` 静音、透明、穿透并隐藏任务栏图标：

```powershell
.\gradlew.bat --no-configuration-cache -I scripts/particle-collider-qa.init.gradle runClient17
```

结果写入 `build/particle-collider-qa/result.txt`，截图在同目录的 `screenshots/`，隔离世界位于
`run/particle-collider-qa/`。只有结果以 `PASS` 开头才算通过。

检查覆盖：

- 原版 47×7×47 结构的四个水平方向、全部 2799 个构件、四种固定构件缺失及修复。
- 中央空白位允许石头和空气，保留上游 `any` 语义。
- 三种构件的组装机配方、控制器装配线配方的实际注册、完整运行时材料和 UV 电路前置。
- 五条原生元素转化的真实输入、输出、UV 电压和 200 tick 时长，以及产物在原生配方中的用途。
- 原生 ME 合成输入仓接收 Ultimate Encoded Pattern 双流体任务，使用 `MEInventoryCrafting` 原生流体
  槽写入 `PatternSlot`，整仓保存恢复后由机器真实双输入路径扣料，检查精确产出及扣料后的持久化。
- 输入功率限制并行，普通超频耗电 4 倍、耗时减半，额外安培不会代替电压超频。
- 低电压、氦等离子体少 1 L、输出仓满时均不扣输入；清空输出后能重试。
- 真实服务端 tick 加工、320 个线圈的原生 `GTCoilTracker` 租约及客户端运行/停止外观、结构失效回退，
  以及运行中真实 TileEntity NBT 保存、恢复并继续推进。
- 结构远景、运行线圈局部、原生 GUI，以及中英文普通和 Shift 结构说明截图。

线圈检查先等待客户端收到全部 2799 个结构构件，再按客户端实际收到的 320 个运行/停止状态推进，
每个阶段最多等待 1200 个服务端 tick。世界截图需完整结构连续稳定 40 个客户端 tick，并在镜头到位后
预热 240 个渲染帧；拍摄期间隐藏 HUD 和手持物品。

纯结构单测为 `SuperParticleColliderStructureTest`，校验完整上游几何 SHA-256、尺寸、符号数量、控制器
和仓室面位置、对称性与数组隔离；来源及重复 `d` 映射的依据见
[`reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md`](../../reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md)。
