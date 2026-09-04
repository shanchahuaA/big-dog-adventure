# opencode 对开发计划的审查意见（第 18 轮 / 第六迭代复核四）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`"第六迭代计划（已修订待复核）"第 466/470 行（方向快照与受击反击）与第 529 行（验证 3）
> 审查者：opencode

## REVIEW_17 意见落实情况（通过）

- **P2（方案 B 实现细节）** ✓：第 466 行细化——`chargingTarget: LivingEntity` **实体引用**快照（非冻结 Vec3d）、`startFiring()` 取 `chargingTarget.getEyePos()` 定向、`tickCharging()` 的 `lookAt` 跟随 `chargingTarget`；第 518 行关键文件变更与第 541 行回应小结同步。

## 新增建议

### P1-1（需确认，与既有 P1-2 矛盾）

**受击立即蓄力路径缺少射程门控**。第 470 行保留"受击时若 `IDLE && cooldown<=0` 仍立即 `startCharging()` 锁攻击者"（对应现有代码 `BigDogEntity.java:146-148`），但该路径**不经第 463 行射程门控**。场景：无口罩玩家在 55 格（FOLLOW 64 内、RANGE 50 外）用弓攻击大狗 → `damage()` 立即 `startCharging()` → 122t 蓄力 → `startFiring` 朝 55 格目标发射 50 格光波 → **打不中目标但空放破坏 50 格方块（约 1500 块）**，与 P1-2"避免 50~64 格原地空放"的修复目标直接矛盾。验证方案 4 只覆盖主动索敌路径，未覆盖受击路径。

建议（与 463 行一致）：`damage()` 中**射程内**（`squaredDistanceTo <= RANGE*RANGE`）才立即 `startCharging()`；**射程外**仅设 `revengeTarget`/`setTarget` 不蓄力，由下一 `IDLE` 沿用判定 + 门控（第 461/463 行）追近至 50 格内再蓄力。并在验证方案 4 补用例："55 格弓箭手攻击大狗（受击路径），大狗先追不蓄力，进入 50 格再蓄力发射，无空放破坏"。

### P2（不阻塞）

1. **`cancelAttack()` 同步清 `chargingTarget`**：第 466 行新增 `chargingTarget` 后，`cancelAttack()`（现有代码 `BigDogEntity.java:289-299` 清 `revengeTarget`）应同步置 `chargingTarget = null`，防止悬挂引用（死亡/打断/口罩脱战路径）。建议在第 466 行或 §2 补一句。

## 结论

REVIEW_17 已落实。新增 P1-1 一处（受击路径缺射程门控，与 P1-2 矛盾，需确认），明确后即可进入实现；P2 为实现细节。