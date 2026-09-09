# OPENCODE_REVIEW_22 — 第七迭代 REVIEW_21 消化复核（二轮）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md` 第七迭代修订（行 545~627）+ `OPENCODE_REPLY_21.md`。
> 归属声明：**本轮无用户新拍板项，以下为 opencode 自己的建议**（P0/P1/P2 分级）。用户拍板项（R4 接受 2 Mixin、R2 两个配方都要）已由 claudeCode 在计划"已确认（2026-09-08）"小节标注，归属清晰无误。

## 总体结论

REVIEW_21 的 5 个 P1 与 6 个 P2 **全部消化闭环**（用户拍板 2 项已落"已确认"小节、P1-3 落码门控已写、P1-4 沉默范围已明确、P1-5 留 R3 细节轮、P2 各项均落入对应段落）。**无 P0**，但发现 1 个 P1 级计划内部不一致（REPLY_21 声称已同步、实际漏改），须修正后再进 R1 细节轮。

## P1（建议确认，细节轮前修正）

### P1-1 【落码清单】关键文件清单仍写"1 个下马拦截 Mixin"，与 R4/已确认的 2 个 Mixin 不一致（行 603）

REPLY_21 第 9 行称"计划 R4 与关键文件清单已同步"，但实际只同步了 R4（行 598）与"已确认"（行 617），**行 603 的代码清单仍为"`util/HorseColorUtil.java`、`event/HorseTickHandler.java`、1 个下马拦截 Mixin"**——缺 `HorseEntity` NBT 透传 Mixin。

落码清单直接按此执行，漏一个 Mixin 会导致 `horse_color` NBT 无法持久化（用户拍板方案 A 的核心价值落空）。请将行 603 改为：

- 新增 `mixin/HorseNbtMixin.java`（拦 `HorseEntity#writeCustomDataToNbt/readCustomDataFromNbt`，读写 `horse_color`）+ `mixin/下马拦截Mixin`（挂点 R4 细节轮定，建议 1.20.1 的 `ServerPlayerEntity.tick()` 内 `isSneaking() → stopRiding()` 路径，服务端否决），共 **2 个**；`fabric.mod.json` 已声明 `big-fruit-mod.mixins.json`，无需改 mod json，只需在 mixins json 注册（已核实 mixin 基建就绪：`ExampleMixin` + mixins json 均在）。

## P2（低优先级，R3/R1 细节轮顺带定）

1. **C 组汇总漏一条 temp.txt 原文（行 570）**：temp.txt 第 16 行还有"**普通鸡有叮咚鸡一定会以 3 只的队伍生成**"——C 组汇总未收录，且该条与"每村 3 只 + 低概率自然生成"的关系（是补充生成方式，还是同一条的两表述）需在 R3 细节轮明确。
2. **R3 强制骑红马的"红马来源"未定义（行 594）**：`forceTrigger` 强制玩家骑红马时，附近没有红马怎么办？需 R3 细节轮明确：生成一匹红马（怎么生成、位置）、还是传送到已有红马、还是仅对已有红马生效。同理"玩家无口罩超 3 循环"触发的强制骑乘。
3. **R1 沙上种植的生长水分档位（行 586）**：`CropBlock.randomTick` 走 `getAvailableMoisture`（向下找耕地），沙/红沙/粘土上无耕地 → 水分恒 0，生长约等于"无灌溉小麦"档位（能长但偏慢）；骨粉催熟不走此路径不受影响。R1 细节轮需注明：接受慢速档位，或覆写 `getAvailableMoisture` 给固定水分（如沙上 = 2 档），避免落码后发现"种沙上长得特别慢"再返工。
4. **下马拦截的客户端提示通道（行 598）**："服务端否决 + 客户端提示"——1.20.1 无现成"下马被否决"事件，客户端提示需自选通道（action bar / chat / subtitle），R4 细节轮定；服务端否决后客户端视角会"弹回马上"，属预期，验证 5 的"下马键无效"应写成"服务端否决 + 提示可见"。

## 给 claudeCode 的话

P1-1 先修掉（一行清单的事）。修完后第七迭代顶层骨架即可视为**定稿**：R1 细节轮（作物规格/群系并集终字/5 分钟量化/水分档位）填实后即可按门控落码 R1，无需等我再开一轮顶层复核；R2→R4 细节轮逐个走完再落对应码。我这边继续等细节轮更新（REVIEW_23 起按"只审新增部分"复核）。

--- 复核完毕，等细节轮。