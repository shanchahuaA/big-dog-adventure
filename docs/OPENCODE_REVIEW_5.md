# opencode 对开发计划的审查意见（第 5 轮：第二次迭代新增部分）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md` 新增的"第二次迭代"（第 174-225 行，5 点新需求）
> 审查者：opencode
> 说明：此前第一版 + 增量音频部分已在第 1-4 轮闭环通过。本轮只审查新增的 5 点，重点看它们与现有架构（服务端 `world.playSound`、`sonicBoom` 伤害源、`DataTracker`/`trackedUpdateRate 2`、单客户端渲染）的交互，以及方案本身是否自洽可落地。

---

## P0-1（必须解决）：死亡截断方案自相矛盾，且混用了两种不可同时成立的做法

第 195 行（死亡截断）在同一段里同时出现了：

- “**客户端持有 + DataTracker 驱动**”的 SoundInstance（推荐方案），与
- “**服务端死亡/取消时向客户端发送 stopSound**”以及“**让 `BigDogEntity` 的 `onDeath`/`remove` 调用客户端 stop**”

这两个方向是**互斥**的，必须二选一，且其中“让服务端实体 `onDeath` 直接调用客户端 stop”在架构上**不可行**——服务端 `BigDogEntity` 无法直接调用客户端 `MinecraftClient`，跨端必须走数据包。计划若不挑明唯一方案，实现者会无所适从。

请 claudeCode 选定并写死一条路径：

- **方案甲（客户端持有）**：各客户端基于 `DataTracker` 同步的 `STATE`，自行 `getSoundManager().play(定位 SoundInstance)`，每 tick 检查 `isAlive()/isRemoved()` 停掉。若选这个，**必须同时从现有代码里删除服务端 `world.playSound(...)` 的 charge/call 调用**（见 P0-2），否则声音会播放两遍。
- **方案乙（服务端驱动 + 停止包）**：保留服务端 `world.playSound` 广播（距离衰减正确、多玩家一次到位），另外新增一个**轻量 Fabric 自定义数据包**（`ServerPlayNetworking` → 客户端 `stopSound`）。这需要打破第一版“不引入自定义网络包”的约定，需在计划里明确接受。

## P0-2（必须解决）：客户端持有 SoundInstance 若与现有服务端播放并存，会双重播放

若按方案甲实现，而计划没有明说“删除 `BigDogEntity` 现有 `tickCharging()`/`startFiring()` 里的 `world.playSound(...)`”，则同一个蓄力/释放音会**被播两次**（服务端广播一次 + 本地客户端实例一次）。

请 claudeCode 在计划中显式写明：采用客户端持有方案时，**删除/注释掉服务端 `world.playSound` 的 charge/call 调用**，只保留一套播放路径。

## P0-3（必须解决）：客户端持有的声音要正确处理距离衰减与多玩家/专用服务器

`getSoundManager().play(...)` 是**纯本地**播放。若方案甲不加定位+衰减，会出现：
- 玩家离大狗 100 格仍能听见（无距离衰减）；
- 专用服务器 / 联机时，其他客户端只能靠各自 `DataTracker` 同步状态自行播放——这要求 `STATE` 同步及时。

请 claudeCode 明确：客户端 SoundInstance 必须是**带位置与衰减**的（`PositionedSoundInstance`/`AbstractSoundInstance` 设置 `x/y/z/attenuation`），以复现原 `world.playSound` 的“附近才听得见”。另外注意 `trackedUpdateRate=2`（每 2t 同步一次），声音启动有 ≤2t 延迟，属可接受，但计划应写明。

---

## P1-1（建议确认）：击退与 `sonicBoom` 伤害源是否叠加

现有 `applySonicDamage()` 用的是 `getDamageSources().sonicBoom(this)`。原版循声守卫的音波击退是由 `SonicBoomTask` 显式 `takeKnockback` 施加的，DamageSource 本身一般不带击退——但**请 claudeCode 在实现前确认** 1.20.1 下 `sonicBoom` 源是否会对目标附带击退。若会，再手动 `addVelocity(dir*0.8)` 会**叠加成双重击退**。建议在计划中写清：要么用普通 `mobAttack` 源 + 手动击退，要么保留 `sonicBoom` 并确认其不击退后再手动加。验证项里也应加一条“单次音波只击退一次”。

## P1-2（建议与用户确认）：抗性 0.2→0.35 不在需求清单内

用户第 3 点只要求“护甲略微提高至 4”，计划却在第 203 行把 `GENERIC_KNOCKBACK_RESISTANCE 0.2→0.35`。这是**需求外的新改动**。建议要么删除、要么在“待确认”里单独向用户说明，避免擅自扩大平衡改动。

---

## P2（低）：贴图 488×511 与 billboard 方形 UV

`barkwhite.jpg`/`nobarkwhite.jpg` 为 488×511（接近方形，非 2 的幂）。现代 MC 大多支持 NPOT 纹理，且渲染器用固定 0..1 UV，正方形 UV 配近方形图问题不大；但若白底转透明后可见主体比例与 0.6×0.6 半宽不匹配，显示会偏大/偏小。建议计划注明：转换后确认主体在画布内的比例，必要时微调 `halfW/halfH`（第 207 行已提 0.6-0.8，可保留）。

## P2（低）：30 格长光柱的视锥剔除边界

长 30 格的 beam 由实体渲染器绘制，当实体本身在屏幕外、仅光柱延伸进视野时，实体渲染可能因视锥剔除而不触发，导致光柱看不到。第一版可接受，建议在计划里注明这一边界，避免测试时误判为 bug。

---

## 需 claudeCode 拍板/明确的清单

1. 死亡截断选**方案甲（客户端持有）还是方案乙（服务端+停止包）**，并写死唯一路径（P0-1）。
2. 若选甲：明确**删除服务端 `world.playSound`**，避免双重播放（P0-2）。
3. 客户端声音的**定位/衰减/多玩家/专用服务器**处理方式（P0-3）。
4. 击退是否与 `sonicBoom` 源叠加，验证项加“只击退一次”（P1-1）。
5. 抗性 0.2→0.35 是否属于需求外改动，需向用户确认（P1-2）。

## 结论

新增 5 点的方向（截断、光波、平衡、贴图、击退）均合理，但**死亡截断的实现方案当前自相矛盾且混用不可行做法（P0-1）**，并隐含**双重播放（P0-2）**与**客户端声音衰减/多端处理（P0-3）**三个必须解决的问题。请 claudeCode 先把这三点定死成单一可落地设计，再交回复核。
