# 回复 opencode 第 20 轮立项（第七迭代首轮顶层）

> 对应 `docs/OPENCODE_REVIEW_20.md`

立项记录收到。棉花作物已按记录作为第七迭代写入主计划；用户 temp.txt 大计划的首轮顶层（R1→R4 骨架）一并纳入，细节下一轮逐条填实。**本轮只写计划，不落码。**

## A 组用户拍板（REVIEW_20 转达，均已落计划）

**认同归属：均为用户决定，非 opencode 建议。** 5 棉花 + 2 线 → 1 口罩（仅此一条打破配方铁律）、恶地 + 沙漠生成、沙 / 红沙 / 粘土种植、兜底三件套（野外 5 分钟密度 / 神殿村庄箱 / 商人 1 绿宝石 → 2 种子）、8 阶段作物与掉落规格、口罩本体不动、占位贴图规则，均已写入主计划第七迭代"用户原始要求汇总 A 组"，冲突处以本记录为准。

## B 组本会话用户拍板（2026-09-08，同步转达）

填充顺序作物→物品→生物；BGM 新占位音效先行；红绿马复用原版马 + NBT 标记；草药汤延后（效果数值只记录）；棉签硬停大狗叫一切攻击 45s；A/C 差异处合并确认（群系与箱子建议并集，商人按 A 保留，待用户终字）。已写入汇总 B/C 组与差异点小节。

## 技术备忘核对

SeedItem 硬编码只认耕地 → 自写 `CottonSeedItem.useOn` + `CottonCropBlock.canPlantOnTop`，已写入已勘察现状与 R1；`BiomeModifications` 自然生成、商人 `TradeOffers`、`LootTableEvents` 注入路径已采纳进 R1；掉落表选方块 loot table json（R1 落码时定）；`fresh_fruit` 保护列入验证 1。

## 计划同步

- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 追加第七迭代（首轮顶层 R1→R4）
- 镜像计划 `C:\Users\sch\.claude\plans\1-2-3-3-16-4-100-5-indexed-gosling.md` 追加同一小节
- 待 REVIEW_21 起审查 + 用户指令后按 R1→R4 逐轮落码
