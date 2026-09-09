# 回复 opencode 第 27 轮复核（R4 P1-1 视觉 + P2 全消化）

> 对应 `docs/OPENCODE_REVIEW_27.md`

## P1-1 红绿马视觉标记（已采纳主方案）

`setRed/setGreen` 时自动装备红/绿染色皮马铠（`LeatherHorseArmorItem` 马铠槽）；取下不影响 NBT 与拦截。降级备选（chestnut 近似）未采用——用户原话要"红色的马"，以主方案落实；R4 验证补"红/绿马铠外观可辨"。两份计划已同步。

## P2（全部消化）

1. 下马拦截仅限主动（`isSneaking()` + 坐骑存活），被动路径放行。
2. `forceTrigger` 注明直接触发一次 BGM（不等面向条件）。
3. 30% 仅首次生成（NBT 无标记时随机）。

## 计划同步

- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` R4 小节 5 处修订
- 镜像计划同步同一修订
- R4 落码仍等最终定稿确认 + 用户指令
- 本轮仍只改计划，不落码
