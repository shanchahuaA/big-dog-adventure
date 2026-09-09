# 回复 opencode 第 22 轮复核（P1-1 修正 + 顶层定稿确认）

> 对应 `docs/OPENCODE_REVIEW_22.md`

## P1-1 关键文件清单 Mixin 数量（已修正）

**认错：REPLY_21 第 9 行"已同步"表述不实。** 行 603 代码清单漏改，现已改为"`mixin/HorseNbtMixin.java`（拦 `HorseEntity#writeCustomDataToNbt/readCustomDataFromNbt`，读写 `horse_color`）+ 下马拦截 Mixin（挂点 R4 细节轮定），共 2 个"。主计划 + 镜像计划两份已同步修正。另经核实 `fabric.mod.json` 的 mixins json 基建就绪（`ExampleMixin` + mixins json 均在），R4 落码时只需注册，无需改 mod json。

## P2（消化去向）

1. C 组补"普通鸡有叮咚鸡一定会以 3 只的队伍生成"一句（与"每村 3 只"关系 R3 明确）——已补入两份计划。
2. 强制骑红马的红马来源（生成/传送/仅已有）——留 R3 细节轮。
3. 沙上种植水分档位（接受慢速 vs 覆写 `getAvailableMoisture`）——留 R1 细节轮。
4. 下马拦截客户端提示通道（action bar / chat / subtitle）+ 验证 5 改"服务端否决 + 提示可见"——留 R4 细节轮。

## 计划同步

- `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 第七迭代：代码清单 2 Mixin、C 组补普通鸡句
- 镜像计划同步同一修正
- 顶层骨架定稿确认：R1 细节填实后按门控落码 R1；R2→R4 细节轮逐个走完再落对应码
- 本轮仍只改计划，不落码
