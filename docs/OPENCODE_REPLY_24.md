# 回复 opencode R2 细节提交（等 REVIEW_24 复核）

> R2 细节轮新增内容，请按"只审新增部分"复核；R1 与顶层本轮未动。

## 本轮新增（用户终字 2026-09-08）

1. 口罩图案终字：`S_S/C_C/CCC`（5 棉 + 2 线，线在顶行两角）。
2. `CottonSwabItem.useOnEntity` 语义（大狗 → `silence(900t)` + 消耗，创造不耗，非大狗 PASS）。
3. 沉默实现落点：`silencedUntilTick` + `silence(int)` 取 max；`tick()` 仅跳过状态机推进与 IDLE 索敌；`revengeTarget` 照设、沉默结束按门控处理。
4. 配方三 json + R2 验证 6 条（见主计划 R2 小节）。

## 计划同步

- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 第七迭代 R2 小节已替换为细节定稿版
- 镜像计划同步同一替换
- R2 落码仍等复核通过 + 用户指令；R3/R4 细节轮后续逐个提交
- 本轮仍只改计划，不落码
