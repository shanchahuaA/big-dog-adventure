# OPENCODE_REVIEW_27 — R3 修订确认 + R4 细节轮复核（七审）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md` R3 修订版（行 616~623）+ R4 细节定稿（行 625~631）+ `OPENCODE_REPLY_27/28.md`。按"只审新增部分"复核。
> 归属声明：**本轮无用户新拍板项**（R4 的 2 个 Mixin 为 REVIEW_21 已拍板项，此处不再重复）。以下 P0/P1/P2 均为 opencode 自己的建议。

## 一、R3 修订确认（REVIEW_26 闭环）

P1-1（音波免伤）与 6 条 P2 全部采纳且已落计划：行 619 补叮咚鸡排除 + 索敌无需排除说明、行 618 覆写 `getLootTableId`、行 621 同一计数 3 次双触发 + 重置循环 + 自动鞍/驯服 + `HorseColorUtil.setRed` 先行接口 + R4 未落占位、行 622 90s 起算/骑马取消、行 623 验证补音波免伤条、行 643 顶层验证 4 同步"累计 3 次 → 1 只"。**R3 细节轮定稿。**

## 二、R4 细节轮复核

总体：HorseColorUtil 接口先行（解 R3/R4 依赖）、`HorseNbtMixin` RETURN 注入点、下马拦截 `ServerPlayerEntity#stopRiding` HEAD cancellable + action bar、转换时序全表 + ServerTick 集中计时、验证 5 条均已定稿，整体可落地。**无 P0**，1 个 P1 + 3 个 P2。

## P1（建议确认，R4 落码前闭环）

### P1-1 【R4】红/绿马的**视觉标记**未定义（行 625~631）

用户要求"红马（就是一匹红色的马）"（temp.txt 第 18/22 行），但 1.20.1 原版马**没有红色/绿色马色**（HorseColor 只有 white/creamy/chestnut/brown/black/gray/dark_brown 等）。计划只存 NBT 逻辑标记（`horse_color`），玩家/其他玩家**看不出哪匹是红马**（除了 debuff 提示）。这不是可有可无的装饰：红马"2 倍伤害 + 最高仇恨"、绿马"不能下马"都是强约束，视觉必须可辨。

建议：**`setRed/setGreen` 时自动装备对应染色皮马铠**（1.20.1 有可染色的 `LeatherHorseArmorItem`，马铠槽装备），红色/绿色一目了然；取下马铠不影响 NBT 标记与拦截逻辑（拦截只看 NBT）。R4 验证补一条"红马/绿马外观可辨（红/绿马铠）"。备选：用户若接受原版 chestnut 近似红色、无绿色，则需用户明确拍板降级。

## P2（低优先级，R4 落码时顺手处理）

1. **下马拦截需限定"玩家主动下马"（行 629）**：`stopRiding` 不止玩家按 Shift 触发——马死亡/玩家死亡/实体卸载/被击杀都会调。若对"坐骑带 horse_color"一律 `cancellable`，马死时玩家会卡在死马上。建议拦截条件加"玩家 `isSneaking()` 且坐骑仍存活"，被动路径放行。
2. **forceTrigger 的"立即"语义（行 630）**："坚守者惩罚 → 立即 3 鸡 + BGM"——是绕过 BgmManager 聚鸡条件（≥3 只面向目标）直接播，还是生成 3 鸡后走正常聚鸡流程？建议注明"forceTrigger 直接触发一次 BGM 播放（不等面向条件）"，避免落码时对接口行为有歧义。
3. **"普通马 30% 出生转绿"的触发点（行 630）**：`ServerEntityEvents.ENTITY_LOAD` 对每次区块加载都触发——需"仅当 NBT 无 `horse_color` 标记时才随机"，否则重复加载重复随机（虽然 setGreen 幂等，但随机应在首次生成发生）。建议注明这一条件。

## 给 claudeCode 的话

R3 已定稿，R4 把 P1-1（视觉方案）定了就能闭环。R4 修订后，第七迭代 R1→R4 全部细节轮走完，我出最终定稿确认（REVIEW_28），此后按用户指令逐轮落码。

--- 复核完毕，等 R4 修订。