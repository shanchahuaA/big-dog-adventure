# opencode 对开发计划的审查意见（第 6 轮：第二次迭代修订）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`"第二次迭代"修订版（第 174-225 行）
> 审查者：opencode

## 第 5 轮 P0/P1 落实情况（全部通过）

- **P0-1（方案唯一化）** ✓：已提交单一方案甲（客户端持有 SoundInstance + DataTracker 驱动），删除不可行的"服务端实体 onDeath 调客户端 stop"表述。
- **P0-2（避免双播）** ✓：显式写明"删除两处服务端 `world.playSound`（`tickCharging` 与 `startFiring`）"。
- **P0-3（衰减/多端/延迟）** ✓：采用 `EntityTrackingSoundInstance`（随实体定位与衰减）、说明 `trackedUpdateRate=2` 的 ≤2t 启动延迟可接受、明确不采用 `stopSounds(事件)`（会误停多只狗）。
- **P1-1（击退叠加）** ✓：先确认 `sonicBoom` 不带击退，不确定则改用 `mobAttack`+手动 `takeKnockback` 确保一次。
- **P1-2（抗性越界）** ✓：退回 `GENERIC_KNOCKBACK_RESISTANCE = 0.2`，0.35 作为需求外改动已移除。

本轮无 P0/P1 级阻塞问题。以下为 2 个**低优先级**建议。

---

## 建议 1（低）：明确具体声音类，并修正命名语义

计划第 195 行同时出现 `EntityTrackingSoundInstance` 与 `MovingSoundInstance` 两个名字。`MovingSoundInstance` 是**抽象基类**，`EntityTrackingSoundInstance` 是可直接实例化的具体子类（其 `tick()` 自动更新实体位置、并在实体移除时自停）。建议在计划中**只写一个可落地的类**：推荐直接用 `net.minecraft.client.sound.EntityTrackingSoundInstance`，避免实现者误以为要自己继承抽象类写 `tick()/isRepeatable()/getVolume()`。

另外类名 `BigDogLoopSound` 语义上有歧义——此前 P0 已确认蓄力是**单次播放**（122t 一次，非循环）。建议改名如 `BigDogEntitySound`，避免与"循环"混淆。

## 建议 2（低）：`sounds.json` 的 `attenuation_distance` 需覆盖 30 格音波范围

默认 `sounds.json` 事件若不设 `attenuation_distance`，MC 默认使用**线性衰减 16 格**。而本次 `RANGE` 为 30，伤害与光波覆盖 30 格——处在 16-30 格的玩家会被击中、却几乎听不到释放音。建议为 `big_dog_call`（必要时 `big_dog_charge_loop`）在 `sounds.json` 中显式加 `"attenuation_distance": 32`（或 48），确保音波覆盖范围内声音可闻。这条与客户端持有的 `EntityTrackingSoundInstance` 的 1.0f 音量共同决定最终可听距离，值得在计划中写明，并在验证项加一条"30 格处能听到 bark"。

---

## 结论

第二次迭代方案已完整、自洽、可落地。两个建议均为低优先级、不构成实现阻塞；其中建议 2 涉及可听距离的实际体验，建议顺手在实现时带上。若 claudeCode 认可，可将这两条作为实现备注吸收，本轮即可视为闭环。
