# opencode 对开发计划的审查意见（第 10 轮）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`"第四次迭代：动漫冲击波 3D 光波 + 破坏提速"（第 296-353 行）
> 审查者：opencode

## 现状抽查

- `BigDogSonicBeamRenderer.java:18` `getBeaconBeam(BEAM_TEX, true)`、`:61-62` hw=hh=0.32 双交叉 quad、`:117-120` 法线固定 `(0,1,0)`、`:25` progress 已 `/100` 归一化、`:46` rotationTo 3D 对齐——勘察基本属实。
- `BigDogEntity.java:345` 现有 `progress = 1 - firingTicksRemaining/FIRING_TICKS`、`:354` while 循环推进段——勘察属实。
- `src/main/resources/light.jpg` 确实存在（会被打入资源目录），处置方案正确。

## 新迭代意见

### P1-1（计划第 332 行）：破坏提速公式与意图矛盾

`progressDestruction = 1 - firingTicksRemaining / BLOCK_BREAK_DURATION_TICKS(50)`：`firingTicksRemaining` 从 80 起算，progress 在 remaining=50（第 30t）才到 0、remaining=0（第 80t）才到 1——**破坏实际发生在第 30~80t，在 FIRING 结束那一刻才跑完**，与"使 30 段在前 50t 内跑完、剩余 30t 仅保留伤害/视觉"完全相反（且第 1 段要等 31t 才开始，比现状还慢）。

正确写法（与现有 `BigDogEntity.java:259/345` 的 elapsed 模式一致）：
`progressDestruction = clamp((FIRING_TICKS - firingTicksRemaining) / (float) BLOCK_BREAK_DURATION_TICKS, 0.0, 1.0)`，第 50t 时达 1.0，之后 while 循环自然空转，30 段在第 50t 内全部完成。

### P1-2（计划第 319 行）："剔除导致看不见"根因诊断错误

Yarn 1.20.1 `RenderLayer.getBeaconBeam(Identifier, boolean)` 的第二个参数是 **depthTest（深度测试）**，不是剔除开关；beacon beam 层本身即**双面**（`Cull.DISABLE`），不存在"camera 在 -Y/-X 侧两片同时被剔除"。当前代码 `getBeaconBeam(BEAM_TEX, true)` 是"双面 + 深度测试开"。

"部分角度看不见"的真实原因更可能是：0.64 宽交叉片在掠射角投影面积趋零 + 第二片（垂直面）法线错误 `(0,1,0)` 导致光照过暗 + α 仅 0.42~0.52。棱柱化 + 真实法线 + 双面层的修复方向正确，但：

- **不要**用 `getBeaconBeam(..., false)`——那是关闭深度测试，光波会穿墙可见（挡在中间的山体/建筑会被看穿），观感更怪；
- 建议 `RenderLayer.getEntityTranslucent(BEAM_TEX)`（双面且保留默认深度测试），或维持 beacon beam 层不动、只做棱柱化与法线修复。

### P2-1（计划第 325-327 行）：明确 progress 归一化

公式 `progress * TAU`、`(progress * 6) % 5` 均按 **0..1** 归一化进度书写（现有代码第 25 行 `getFiringProgress()/100.0f` 已归一化），但 `FIRING_PROGRESS` 的 DataTracker 值是 0..100。建议在方案中写明"progress 为 0..1 归一化（`getFiringProgress()/100.0f`）"，防止实现时直接用 tracker 原值导致旋转/闪烁快 100 倍。

### P2-2（计划第 320 行）："8 边形 16 片"笔误

8 边棱柱应为 8 片侧面（8×4=32 顶点）；"16 片"是 16 边形。请改为"6 片（24 顶点）或 8 片（32 顶点）"。

### P2-3（计划第 327 行）：电弧"UV 闪烁 progress% 偏移"表达式未定义

建议写明具体式，如 `uvOffset = (progress * 8.0f) % 1.0f`（每 1/8 次攻击闪一轮），避免实现时随意取值。

## 其余核验（无问题）

- 双层光柱（外晕 2.5 与判定一致 + 内核 0.7~1.0）、六边形环 + 蓝电弧、双螺旋旋转、发射口粒子为可选——结构与第三迭代渲染管线兼容，性能量级（每狗每帧约 250-350 quad）可接受，验证已含多狗。
- `BLOCK_BREAK_DURATION_TICKS=50` 独立于 `FIRING_TICKS=80`，破坏/伤害/音频三节奏解耦清晰；`brokenThisAttack`、`drop=false`、`blastResistance>8` 保持不变——自洽。
- `light.jpg` 移至 `docs/reference/` 并确认不入 jar、验证 5 回归覆盖——完整。

## 结论

两处 P1 需修订（破坏公式、剔除诊断），修订后即可闭环；P2 为文字级修正。