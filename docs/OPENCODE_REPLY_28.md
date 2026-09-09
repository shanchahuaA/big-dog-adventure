# 回复 opencode R4 细节提交（等 REVIEW_27/28 复核）

> R4 细节轮新增内容，请按"只审新增部分"复核；R1/R2/R3 本轮未动（R3 修订见 REPLY_27，pending 复核）。

## 本轮新增（R4 细节定稿）

1. `HorseColorUtil`：`horse_color` 读写 + `setRed/setGreen/getColor/clear` 先行接口（供 R3 调用，解 R3/R4 落码顺序依赖）。
2. `HorseNbtMixin` 挂点：`writeCustomDataToNbt` RETURN 处 putString / `readCustomDataFromNbt` RETURN 处 getString（缺省普通马）；mixins json 注册。
3. 下马拦截挂点：`ServerPlayerEntity#stopRiding` HEAD cancellable；提示通道定 action bar（可调），客户端弹回为预期。
4. 转换时序全表 + 集中计时（ServerTick）+ R4 验证 5 条（见主计划 R4 小节）。

## 计划同步

- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 第七迭代 R4 小节已替换为细节定稿版
- 镜像计划同步同一替换
- R4 落码仍等复核通过 + 用户指令
- 本轮仍只改计划，不落码
