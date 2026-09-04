# opencode 对开发计划的审查意见（第 14 轮 / 第六次迭代）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`"第六迭代计划：仇恨重构 + 口罩 + 蓄力护甲 + 数值 + 同类免伤"（第 409-530 行）
> 审查者：opencode
> 验证性检查：`git status` 干净（仅计划文档修改）；`fresh_fruit` 的 Java/lang/model/texture 均完好；`docs/reference/dragon-breath-ref.jpg` 已就位。

## 总体判断

计划逻辑自洽、覆盖完整、可落地。抽读源码核对"已勘察现状"与 API 选型：`addTemporaryModifier`/`removeModifier(UUID)`、`EntityGroup.UNDEAD`、`getNonSpectatingEntities(Class, Box, Predicate)`、`ArmorItem(ArmorMaterial, Type, Settings)`、`EntityType.WARDEN` 在 Yarn 1.20.1 均可用；`BigDogEntity.java:221-232`（IDLE 分支）、`:160-167`（startCharging 停导航）、`:245-264`（tickCharging）、`:130-152`（damage）、`:301-330`（applySonicDamage）与计划描述一致。**无 P0**。以下为 P1/P2。

## P1（建议确认）

1. **受击反击 vs 优先级索敌的冲突（计划第 462-463 行）**：IDLE 分支"`revengeTarget` 存活且合法 → 沿用现有链路"优先于 `findPreferredTarget()`。后果：无口罩玩家攻击大狗后（`damage()` 设 `revengeTarget`，代码 `BigDogEntity.java:144`），即使场上同时存在 warden/僵尸，大狗仍追玩家不放，与"优先级 warden > undead > hostile > 无口罩玩家"的类铁傀儡语义冲突（铁傀儡的反击优先来自 RevengeGoal 优先级高于索敌，但用户列出的优先级把玩家放最后，语义存在歧义）。建议澄清：若"任何时候按优先级选优"，则 IDLE 分支改为先 `findPreferredTarget()`，`revengeTarget` 仅作为评分输入之一（或仅当它是当前最高优先级时才沿用）；并补验证用例"被低优先级目标攻击后，场上出现高优先级目标时是否切换"。

2. **射程外目标原地空放（计划第 462 行"沿用现有链路"）**：现有代码 IDLE 分支 `startMovingTo` 后**同一 tick** 无条件 `startCharging()`，而 `startCharging()` 内 `getNavigation().stop()`（`BigDogEntity.java:165`）立即取消导航——实际从未发生追击，永远原地蓄力。FOLLOW 64 > RANGE 50，主动索敌会扫到 50~64 格的目标并原地空放（光波破坏 50 格方块但打不到目标，且每轮空放都产生约 1500 块破坏）。建议：目标距离 > RANGE（或 RANGE×0.8）时只 `startMovingTo` 不蓄力，进入射程再 `startCharging`；若维持"原地炮台"语义，请在计划中明确注明这是已确认行为。

3. **验证方案 5 的 `/data get entity` 不可靠（计划第 527 行）**：`addTemporaryModifier` 的临时 modifier 在 1.20.1 `AttributeInstance.toNbt()` 中不序列化（temporary 只存在于运行内存，`/data get` 看到的 `Attributes` 是 NBT 序列化结果，`generic.armor` 仍显示 Base 8 + 持久 modifier）。建议改用 `/attribute @e[type=big-fruit-mod:big_dog,limit=1] minecraft:generic.armor get`（显示含临时 modifier 的最终计算值，1.16+ 可用）。

4. **蓄力护甲验证缺口（计划第 487-495 行）**：建议补玩法级验证——蓄力期间受击掉血显著低于非蓄力（护甲 16 实际生效）；`cancelAttack`（死亡/打断/口罩脱战）后护甲回落 8。另提示：`removeModifier(UUID)` 对不存在 UUID 幂等，在 `startFiring/enterCooldown/cancelAttack/onDeath` 多处调用安全；`addTemporaryModifier` 同 UUID 重复添加会覆盖，计划"先移除再添加"即可。

## 给 claudeCode 的话：用户新要求（2026-08-27，用户拍板，非 opencode 建议）

**无口罩玩家的仇恨优先级调整为第三级**（原计划第 456 行为第四级、最低档）。新顺序：

> Warden（循声守卫）> 亡灵生物（Undead）> **无口罩玩家** > 其他敌对生物（Monster/HostileEntity）

同优先级内仍按 `squaredDistanceTo` 最近者胜；戴口罩玩家依旧直接排除。请据此修订计划第 456 行 `findPreferredTarget` 的打分顺序，并同步更新第 525 行验证方案 3（"creeper 死后才转玩家"的预期改为"无口罩玩家在 hostile 之前被锁定"）。

## P2（低优先级）

5. **扫描垂直范围（计划第 455 行）**：`expand(64,64,64)` 全向会选到天上/深坑目标（导航不可达 → 原地空放）。建议模仿铁傀儡 `getTargetSearchArea`（水平 followRange、垂直 ±4~±8），或至少过滤不可达目标。
6. **spectator 玩家（计划第 455 行 predicate）**：建议追加排除 `e instanceof PlayerEntity && ((PlayerEntity)e).isSpectator()`（旁观者不可被攻击，原版索敌均排除）。
7. **扫描节流（计划第 463 行）**：IDLE 且 `cooldown<=0` 时每 tick 全量 `getNonSpectatingEntities`（64 格）扫描，多狗/多实体场景有开销；可每 10t 扫一次并缓存结果（与 `DAMAGE_INTERVAL` 思路一致）。非必需。
8. **口罩耐久（计划第 474 行）**：`ArmorMaterials.LEATHER` 头盔耐久 55 会磨损损坏；占位期可接受，若预期"口罩不损耗"需正式版确认（可 `Settings.maxDamage(0)` 等）。
9. **验证 3 的场控（计划第 525 行）**：warden 原生半径 16 内主动攻击，测试时可能先杀玩家/大狗导致"顺序验证"失效；建议拉开距离或控制仇恨（如先挂机/喝隐形）再逐级验证优先级。

## 结论

无 P0，计划可进入修订。P1-1/P1-2 涉及玩法语义（优先级与射程外行为），需用户/方案方拍板；P1-3/P1-4 为验证方案修订。修订后交回复核。