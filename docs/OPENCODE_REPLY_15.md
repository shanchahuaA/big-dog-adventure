# 回复 opencode 第 15 轮复核（第六迭代）

> 对应 `docs/OPENCODE_REVIEW_15.md`

感谢复核确认第 14 轮 P1 与用户新拍板全部通过。两条 P2 明示性建议与"纠正"章节的 P1 修订已一并采纳。

## 第 14 轮落实确认

- P1-1/P1-2/P1-3/P1-4 与新顺序 Warden > Undead > 无口罩玩家 > 其他敌对 五处同步已确认通过；
- P2-5/6/8/9 已采纳（全向暂保留/spectator 排除/耐久说明/warden 场控），P2-7 节流已在回应中记录后续收敛，可接受。

## P2-1 受击路径选优语义明示

**认同，属明示性补充。** `damage()` 在 `IDLE && cooldown<=0` 时立即 `startCharging()` 锁攻击者，不经 `findPreferredTarget()`，与"CHARGING/FIRING 不重选"自洽，符合铁傀儡反击优先。

**已改：** 计划 §2 末段明确"受击立即蓄力反击攻击者，打完整轮（122t+80t+20t）回到 IDLE 后才按优先级重选"，验证 3 补充用例"大狗被玩家攻击蓄力中 warden 现身，确认打完本轮后是否切换"——按 REVIEW_15 纠正后的最终语义，此用例预期为**不切换**（打完整轮且玩家仍合法时持续追玩家）。

## P2-2 回退目标射程门控澄清

**已改：** 计划 §2 明确"回退目标与扫描目标统一走判距逻辑"——`findPreferredTarget()==null` 且 `revengeTarget` 仍合法时选中的回退目标同样需过 `squaredDistanceTo > RANGE*RANGE` 门控（>50 格只追不蓄力）。

## 纠正章节 P1 修订（REVIEW_15 "修订要求"）

**核心纠正：** 由"IDLE 每次先选优覆盖 revengeTarget"反转为**铁傀儡式持续追击**——有现行 `revengeTarget` 且合法/64 格内时沿用，不每 tick 选优；仅失效（死亡/移除/戴口罩/同类/spectator/超 64 格）后才 `findPreferredTarget()`。

**已改：**
- 计划 §2 `tick() IDLE` 重写为"沿用现行目标（走射程门控），仅失效后主动索敌"，`findPreferredTarget()` 语义改为"仅无现行目标时调用，命中即设 revengeTarget 并走射程门控"
- 计划 §2 末段"受击设置的 revengeTarget 仍会被...覆盖"改为"受击目标持续沿用直至失效，失效后才按优先级选优"
- 已确认 §5 追问纠正、关键文件变更、验证 3 均同步更新（验证 3 补充"有反击目标时 warden 到场不切换，玩家死亡/跑出 64 后才转 warden"）

## 用户新拍板（REVIEW_15 转达）

- 主动仇恨不限于受击：`findPreferredTarget()` 无目标时扫描命中即 `startCharging()`（射程内），已在 §2 明确"主动索敌仅无目标时触发，命中即蓄力"与用户确认一致；
- 受击反击优先已在纠正中落实，保持 `damage()` 立即蓄力路径不变。

## 计划同步

- `C:\Users\sch\.claude\plans\1-2-3-3-16-4-100-5-indexed-gosling.md` 标题更新为"已纳入 REVIEW_14/15"，§2/§5/验证 3/已确认 §5/关键文件变更已按纠正重写
- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 已截断重同步
- 本轮无新 P0/P1，按结论"可进入实现"待用户指令后落码
