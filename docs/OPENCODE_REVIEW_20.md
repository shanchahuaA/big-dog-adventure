# OPENCODE_REVIEW_20 — 新需求记录：棉花作物（立项说明，非计划审查）

> 本文件是 **opencode 写给 claudeCode 的立项记录**，不是对现有计划的审查意见。
> 背景：用户直接向 opencode 提出新需求（棉花作物），并在讨论中逐项拍板。
> 请 claudeCode 在下一轮把"棉花作物"作为新迭代写入 `BIG_DOG_PLAN_FOR_OPENCODE.md`（建议命名"第七迭代：棉花作物"），同步更新"用户原始要求汇总"一节，然后交回 opencode 按常规流程审查（REVIEW_21 起）。

---

## ⚠️ 用户拍板项（用户直接提出的要求/决定，非 opencode 建议，不可模糊归属）

1. **棉花 → 口罩生存配方**：用户明确拍板打破"不擅自加生存配方"铁律，仅限此一条配方。配方：**5 棉花 + 2 线 → 1 口罩**（U 形 + 耳挂样式）。除此之外不得扩展任何其他配方。
2. **自然生成群系**：**恶地（badlands / wooded_badlands / eroded_badlands）+ 沙漠（desert）**，与现实中棉花产地一致（温干气候）。
3. **可种植方块**：**沙（黄沙/红沙）、粘土块**，**不占耕地**（普通泥土/耕地不可种，写实派）。
4. **兜底获取途径（两项都要）**：
   - **野外生成**（恶地/沙漠，密度需调到"随便逛 5 分钟必能碰到"）
   - **战利品箱子**：沙漠神殿 + 沙漠村庄房屋箱子中少量出现棉花种子
   - 注：用户另确认要**流浪商人卖棉花种子**（1 绿宝石 → 2 种子）作为第三种兜底（讨论中用户选"选项 1 和 3 都要"时同时确认了商人选项）。
5. **棉花用途**：口罩配方材料（同上第 1 条）。

## 已确认的其余规格（讨论中用户接受无异议的默认建议）

- 作物形态：小麦式 8 生长阶段（age 0–7）、随机刻生长、骨粉可催熟、光照 ≥9。
- 掉落：成熟（age≥7）收获 → 2 棉花 + 1~2 种子；未成熟打掉 → 1 种子。
- 产出物品：`cotton`（棉花）+ `cotton_seeds`（种子，可右键种植）。
- 口罩本体不改：现有 `MASK`（皮革系 55 耐久占位）不动，仅新增配方解锁途径。
- 素材铁律不变：8 阶段作物贴图、棉花/种子物品贴图第一版允许占位，正式素材替换前必须与用户讨论。

---

## opencode 技术备忘（供 claudeCode 写计划参考，非用户要求）

- **1.20.1 的 SeedItem 硬编码只认 FARMLAND**（`cropsSupported` tag 是后续版本才有的）。要让种子能右键种在沙/红沙/粘土上，需要自定义 `CottonSeedItem`（extends SeedItem 或普通 Item）覆写 `useOn` 判断支持方块；同时 `CottonCropBlock`（extends CropBlock）覆写 `canPlantOnTop` 把支持方块改为 沙/红沙/粘土。计划里需明确这一实现路径，避免照抄新版本写法。
- 自然生成：`BiomeModifications.addFeature(VEGETAL_DECORATION, ...)` + 自注册 `ConfiguredFeature/PlacedFeature`（簇 2~4 株），无需 Mixin。
- 流浪商人：初始化时直接向 `TradeOffers.WANDERING_TRADER_TRADES` 添加 `SellItemFactory`（1 绿宝石 → 2 种子，次数 6，xp 1），原版 API 可改，无需 Mixin。
- 战利品注入：用 `LootTableEvents.MODIFY` 给 `desert_pyramid` 与 `village_desert_house` 箱子表加池。
- 掉落表：棉花作物做成方块 loot table json（按 age 分支），或覆写 `getDroppedStacks`，计划选其一即可。
- 铁律核对项：实现与验证中确认 `fresh_fruit`（Java/lang/model/texture）仍在、未被覆盖。

## 给 claudeCode 的话

请按以上内容起草第七迭代计划小节；如与上述任一用户拍板项冲突，以本记录为准并回写说明。opencode 将在计划更新后按常规审查（逻辑自洽、覆盖完整、可落地），不再逐条重复本记录。