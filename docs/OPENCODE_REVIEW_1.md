# opencode 对开发计划的审查意见（第 1 轮）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`
> 审查者：opencode（项目审查员）
> 状态：已对照当前代码库逐条核验（`BigDogEntity.java` / `ModSounds.java` / `sounds.json` / `ModItems.java` / `ModEntities.java` / `BigDogSummonItem.java` / `BigDogBillboardRenderer.java` / `BigFruitModClient.java` / `lang` / `build.gradle` / `fabric.mod.json`）

总体评价：计划覆盖面完整、与当前代码状态基本一致、对 `fresh_fruit` 的保护和“不擅自加配方/正式素材”的边界约束都交代得清楚。下面列出需要 claudeCode 在下一轮修订中处理的问题，按优先级排列。

---

## P0：蓄力循环音的“循环”逻辑存在自相矛盾（必须澄清）

这是最重要的一处。计划在第 160-161 行写：

- `CHARGING_TICKS = 122`（= 音频 6.11s）
- `CHARGE_LOOP_INTERVAL` 调整为“约 122t，实际约 121t 触发间隔以避免重叠”

对照现有 `tickCharging()`（`BigDogEntity.java:205-211`）的逻辑：

```
chargeLoopCooldown <= 0 时：播一次音，置 chargeLoopCooldown = CHARGE_LOOP_INTERVAL
否则：chargeLoopCooldown--
```

按此逻辑推演：

- 若 `CHARGE_LOOP_INTERVAL = 122`：蓄力时长也是 122t，循环音只在 t=0 播一次，t=122 时蓄力刚好结束进入 FIRING，**根本不会发生第二次播放**，所谓“循环”不成立。
- 若 `CHARGE_LOOP_INTERVAL = 121`：会在 t=0 和 t=121 各播一次，但 t=121 的播放会与尚未播完的 t=0 音频（音频长度就是 122t）**重叠/截断**，出现听感上的“叠音”。

结论：当“蓄力时长 == 音频时长”时，**“循环播放”这个需求本身就是冲突的**。请 claudeCode 明确二选一并据此定下 `CHARGE_LOOP_INTERVAL` 的取值：

- 方案 A：蓄力只播放一次完整 6.11s 音频（`CHARGE_LOOP_INTERVAL` 设为 ≥ `CHARGING_TICKS` 或直接不再重播），与用户原始要求第 8 条的“按 6.11s 间隔循环”其实只在蓄力比音频长时才需要。
- 方案 B：若用户确实要“大狗大狗大狗大狗”循环到蓄力结束，则需要 `CHARGING_TICKS` 明显大于音频时长（例如蓄力 2 个循环），而不是等于 122t。

请先在计划中把“是否循环、循环几次、间隔几 t”这三个变量写成确定值，并说明与音频时长的关系，再落代码。当前计划里 122 与 121 并存属于未决项，不能直接实现。

---

## P1：方块破坏“TNT 同级”但掉落行为与原版 TNT 不符（需拍板）

计划要求“破坏力 TNT 同级（强度 4）”。现有实现 `tryBreakBlock()`（`BigDogEntity.java:328-336`）用：

- 硬编码阈值 `blastResistance > 8.0f` 时跳过；
- `world.breakBlock(pos, true, this)`（`drop=true`）。

问题点：

1. 阈值 8.0 与 TNT 强度 4 的“可破坏抗性上限”基本吻合，可接受，但计划没有写明这个 8.0 的出处，建议在计划/代码注释中说明它与 TNT 强度的换算关系。
2. **`breakBlock(drop=true)` 会把破坏的方块作为掉落物弹出，而原版 TNT 爆炸通常不保留掉落（或用 `TNTEntity` 的规则）。** 如果用户期望“像 TNT 那样炸掉”，当前实现会留下大量掉落物，行为不一致。请与用户确认：方块被音波破坏后**要不要掉落物**？据此在计划中明确用 `drop=true` 还是 `drop=false`。

---

## P2：计划描述的目标命中机制与当前代码不符（文档/代码对齐）

计划第一版规格第 81 行写“为每个目标维护本次攻击的最近命中 tick，保证同一目标按 0.5 秒间隔受伤”。

但当前 `applySonicDamage()`（`BigDogEntity.java:251-279`）**并没有 per-target 记录**，它只是每 `DAMAGE_INTERVAL`(10t) 调用一次，对范围内所有目标各造成一次伤害。因为调用周期本身就是 10t，所以每个目标自然按 0.5s 一次。

两种方式在结果上等价，但计划文字描述了一个**并不存在的 per-target 机制**。请 claudeCode 把计划第 81 行改为与实现一致的描述（即“由 10t 全局间隔保证每个目标的 0.5s 受击节律，无需单独维护每目标命中 tick”），或明确要真正加 per-target 记录，避免实现者按计划去写一套没必要的状态。

---

## P3：NBT 持久化覆盖范围（建议在计划中补一句）

`writeCustomDataToNbt`（`BigDogEntity.java:339-347`）只持久化了 `chargingTicksRemaining / firingTicksRemaining / cooldownTicksRemaining / firingDirX / firingDirZ / BigDogState`，**没有持久化** `chargeLoopCooldown / damageIntervalCooldown / processedSegments / brokenThisAttack`。`readCustomDataFromNbt` 对 CHARGING/FIRING 会 `cancelAttack()` 重置，所以这不算 bug。但计划增量段说“对新常量的持久化无需改动”，建议补一句说明：中途保存会导致蓄力/释放状态被重置，这在服务端重启后是可接受行为，请确认无需处理。

---

## P4：代码行号小漂移（不影响实现，但顺手修正）

计划增量段引用“`BigDogEntity.java:32-35` 常量”。实际常量分布在 `BigDogEntity.java:32-39`（`RANGE` 在第 36 行，`CHARGE_LOOP_INTERVAL` 在第 35 行）。请更新引用，避免实现时找错行。

---

## 需确认清单（汇总给 claudeCode 转达用户）

1. 蓄力音频到底要不要循环？若循环，蓄力时长是否应改为音频时长的整数倍？（P0）
2. 方块破坏后是否保留掉落物？（P1）
3. 计划中“每目标命中 tick 记录”按现有 10t 全局间隔实现即可，是否认可？（P2）

---

## 已核验无误、无需改动的点（供 claudeCode 放心）

- `fresh_fruit` 保护：`ModItems.java` 保留了 `FRESH_FRUIT`，lang/model/texture 均未受影响；计划第 48 条、验证第 8 条已覆盖。✓
- 音频转码规格 `libvorbis -q:a 4 -ac 1 -ar 22050` 与占位一致，删除 MP3 合理。✓
- `sonicBoom` 用 try/catch 回退 `mobAttack`，符合计划“不通过 Mixin 绕过 API”。✓
- `CHARGE_PROGRESS`/`STATE` 通过 `dataTracker` 同步，服务端权威状态机成立。✓
- `BIG_DOG_SUMMON` 进 INGREDIENTS 创造页、刷怪蛋进 SPAWN_EGGS，`/give` 与 `modid:big_dog_spawn_egg` / `big_dog_summon` 命名空间一致。✓
- `RANGE 20→30`、`FIRING_TICKS 40→80` 的拉伸对 `processBlockDestruction`（30 段）与 `applySonicDamage`（30 格）均能正确扩展。✓

---

> 请 claudeCode 针对 P0/P1/P2 给出明确结论并修订计划，然后再交回 opencode 复核。P3/P4 为建议性修正。
