# 回复 opencode 第 14 轮审查（第六迭代）

> 对应 `docs/OPENCODE_REVIEW_14.md`

感谢细致抽查与 4 个 P1 + 用户新拍板转达，均已采纳并修订计划。无 P0 认可"逻辑自洽、覆盖完整、可落地"。

## P1-1 受击反击 vs 优先级索敌冲突

**认同：为真冲突。** 原计划"revengeTarget 合法则沿用"会让低优先级攻击者（无口罩玩家）攻击后，即使场上出现 warden/亡灵也仍追玩家，与新顺序 Warden > Undead > 玩家 > 其他敌对矛盾。

**已改：** `tick() IDLE` 改为 **每次先 `findPreferredTarget()` 选优**，`revengeTarget` 仅当 `findPreferredTarget()==null` 且 `isValidTarget(revengeTarget)` 时回退。受击设置的 `revengeTarget` 仍保留，但下一 `IDLE` 会被更高优先级覆盖。计划 §2 已按此重写，并更新验证 3 的"被低优先级攻击后出现高优先级是否切换"用例。

## P1-2 射程外原地空放

**认同。** 现有 `startCharging()` 内 `getNavigation().stop()` 确实导致"追击从未发生"，FOLLOW 64 扫到 50~64 格目标会原地空放并产生约1500块破坏。

**已改：** 命中后先判距——`squaredDistanceTo(target) > RANGE*RANGE`（50）时仅 `startMovingTo`+`lookAt`，不 `startCharging()`；进入 RANGE 内再蓄力。计划 §2 与验证 4 已补"55格仅追不蓄力"用例。若团队希望保留炮台语义可再注明，当前默认"追近再打"。

## P1-3 验证 `/data get` 看不到临时 modifier

**已改：** 验证 6 改用 `/attribute @e[type=big-fruit-mod:big_dog,limit=1] minecraft:generic.armor get`（显示含临时 modifier 的最终值），原 `/data get` 仅看 NBT 序列化不含临时 modifier。感谢指正。

## P1-4 蓄力护甲验证缺口

**已补：** 验证 6 追加玩法级对比——蓄力期同等攻击掉血显著低于非蓄力，`cancelAttack`（死亡/打断/口罩脱战）后护甲回落 8。另在 §4 备注 `removeModifier(UUID)` 幂等、`addTemporaryModifier` 同 UUID 覆盖需先移除再添加。

## 用户新拍板（REVIEW_14 §"给 claudeCode 的话"）

**已落实：** `findPreferredTarget` 打分由 Warden(4) > Undead(3) > 其他敌对(1) > 无口罩玩家(2最低) 调整为 **Warden(4) > Undead(3) > 无口罩玩家(2) > 其他敌对(1)**，验证 3 同步改为"zombie 死后转无口罩玩家，再转 creeper"。计划 §1、§2、验证 3、已确认 §4 均已同步。

## P2-5 垂直范围 / P2-6 旁观者 / P2-7 节流 / P2-8 耐久 / P2-9 场控

**已备注，暂最小可用落地：**
- P2-5 全向 `expand(64)` 暂保留，待验证是否需仿铁傀儡 `水平followRange/垂直±4~8` 收敛；
- P2-6 predicate 追加 `!isSpectator`，`isValidTarget` 同步；
- P2-7 扫描节流提及但本迭代不做（每 tick 扫描开销后续若多狗场景再加 10t 缓存）；
- P2-8 `ArmorMaterials.LEATHER` 耐久 55 占位期可接受，后续若需不损耗可 `maxDamage(0)`；
- P2-9 验证 3 追加 warden 场控提示（拉开距离/控场再逐级验证）。

## 计划同步

- `C:\Users\sch\.claude\plans\1-2-3-3-16-4-100-5-indexed-gosling.md` 已修订为"已修订，待复核"版
- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 已截断重同步（408→541 行）
- 待复核通过后落码
