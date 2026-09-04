# opencode 对开发计划的审查意见（第 17 轮 / 第六迭代复核三）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`"第六迭代计划（已修订，待复核）"第 461/464/466 行（方向语义）与第 529 行（验证 3）
> 审查者：opencode

## REVIEW_16 意见落实情况（通过）

- **P1-1（CHARGING/FIRING 受击方向语义）** ✓：第 466 行选定**方案 B**——`startCharging()` 快照目标（`chargingTarget` 或 Vec3d），`startFiring()` 用快照定向；`damage()` 非 IDLE 覆盖 `revengeTarget` 仅下一轮生效，不改变当前发射方向。第 529 行验证 3 补"蓄力打 warden 时被玩家打，光波仍朝 warden，打完下一轮才转玩家"用例。
- **P2-1（IDLE 追击中受击转头明示）** ✓：第 461 行末段注明"最新攻击者优先，不算摇摆"。
- **P2-2（拍板补进汇总）** ✓：第 421 行汇总 1 追加"铁傀儡式持续追击（不摇摆）"。
- 第 441 行与第 537-543 行回应小结同步更新。✓

## 新增建议（P2，不阻塞）

1. **方案 B 实现细节**：第 466 行列出"快照 `chargingTarget` 字段**或**直接快照 Vec3d"两个选项，建议实现时选**实体引用快照**（`startFiring` 时再取 `chargingTarget.getEyePos()` 计算方向）而非快照 Vec3d——快照 Vec3d 会冻结蓄力开始时刻的方向，122t 内目标移动后光波会偏离；实体引用则保持现有"蓄力结束时取方向"的准确性。另建议 `tickCharging()` 的 `lookAt` 也跟随 `chargingTarget`（当前 `lookAt revengeTarget` 会在 CHARGING 中被打时视觉转向玩家而发射朝 warden，轻微违和，可选）。

## 结论

REVIEW_16 全部落实，无新 P0/P1。一条 P2 为实现细节提示。**第六迭代计划可进入实现。**