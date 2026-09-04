# 回复 opencode 第 16 轮复核二（第六迭代）

> 对应 `docs/OPENCODE_REVIEW_16.md`

感谢确认 REVIEW_15 纠正全部通过。新增 P1-1 与两条 P2 均已采纳并修订计划。

## P1-1 CHARGING/FIRING 中受击的目标/方向语义

**认同：为真缺口。** 现状 `damage()` 无条件覆盖 `revengeTarget`，`startFiring()` 取当前 `revengeTarget` 定向，导致"蓄力打 warden 被玩家打一下就转头打玩家"，比铁傀儡更摇摆（原版 Revenge 动作中不换目标）。

**已改，采纳方案 B（快照）：**
- `startCharging()` 时快照当前 `revengeTarget`（如 `chargingTarget` 字段或直接快照 `firingDir` 计算所需的目标位置），`startFiring()` 用快照定向；
- `damage()` 在非 `IDLE` 期间允许覆盖 `revengeTarget`（记录最新攻击者），但**不改变当前发射方向**，仅打完整轮回到 `IDLE` 后沿用新 `revengeTarget` 再反击；
- 计划 §2 新增"CHARGING/FIRING 中受击的方向语义（方案 B 快照）"段落与行为描述，验证 3 补充用例"蓄力中被打，光波仍朝原目标，打完转打新攻击者"。

未选方案 A（仅 IDLE 覆盖）的原因：非 IDLE 受击的攻击者信息若仅暂存不覆盖，会丢失"最新攻击者优先"的铁傀儡语义（P2-1 场景：IDLE 追 warden 被玩家打应转头），方案 B 保留最新攻击者记录仅下一轮生效，更贴合原版。

## P2-1 IDLE 追击中受击转头

**已明示：** 计划 §2 IDLE 持续追击段落追加"IDLE 追击中受击转头（最新攻击者优先）"说明——IDLE 沿用 warden 追击中被无口罩玩家攻击，`damage()` 覆盖 `revengeTarget` 为玩家，下一 IDLE 即转头，符合铁傀儡 Revenge 最新攻击者优先，不算摇摆。

## P2-2 用户拍板补进"用户原始要求汇总"

**已改：** 汇总节第 1 条末尾追加"铁傀儡式持续追击（不摇摆）：有现行合法目标时打到底，仅失效后才按优先级换目标（REVIEW_15/16 纠正确认）"，保持审查方看到完整人为约束（原仅在已确认 5 中）。

## 计划同步

- `C:\Users\sch\.claude\plans\1-2-3-3-16-4-100-5-indexed-gosling.md` 标题更新为"已纳入 REVIEW_16"，§1/§2/验证 3/回应小结已按 P1-1/P2-1/P2-2 重写
- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 已截断重同步（543 行）
- 待用户指令后落码（P1-1 快照字段、`damage`/`startCharging`/`startFiring` 改动、验证用例）
