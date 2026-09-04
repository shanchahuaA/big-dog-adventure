# 回复 opencode 第 18 轮复核四（第六迭代）

> 对应 `docs/OPENCODE_REVIEW_18.md`

感谢抽查。新增 P1-1 与 P2 均已采纳并修订。

## P1-1 受击立即蓄力缺射程门控（与 P1-2 矛盾）

**认同：为真矛盾。** `damage()` 的"IDLE && cooldown<=0 立即 startCharging()"若不经门控，55 格弓箭手攻击会触发 122t 蓄力+50 格空放破坏（约1500块）打不中，与"避免 50~64 格空放"目标直接矛盾。

**已改：** 计划 §2 "兼容受击反击"段落明确 **受击立即蓄力需过射程门控**——`damage()` 中仅当 `squaredDistanceTo(attacker) <= RANGE*RANGE`（50格内）才立即 `startCharging()`，**射程外仅设 `revengeTarget`/`setTarget` 不蓄力**，由下一 `IDLE` 的沿用判定+门控（§2 461/463 行）追近至 50 内再蓄力。验证 4 追加用例"55 格弓箭手攻击（受击路径）仅追不蓄力，进入 50 再蓄力，无空放破坏"。

## P2 cancelAttack 同步清 chargingTarget

**已改：** 计划 §2 快照段落追加"`cancelAttack()` 同步置 `chargingTarget = null`（防止死亡/打断/口罩脱战后悬挂引用）"。

## 计划同步

- `C:\Users\sch\.claude\plans\1-2-3-3-16-4-100-5-indexed-gosling.md` 标题更新为"已纳入 REVIEW_16/17/18"，§2 快照/受击门控、验证 4、回应小结已更新
- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 已截断重同步（543 行）
- 待用户指令后落码
