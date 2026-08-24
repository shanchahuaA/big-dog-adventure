# 大狗叫 MOD 第一版实现计划

## Context

用户希望将当前 `big-fruit-mod` 项目逐步制作成 Fabric 1.20.1 的“大狗叫”恶搞 MOD。第一版新增一个中立生物：它使用二维表情包 billboard 形式显示，平时不会主动攻击；受到有效攻击后，会先蓄力并循环播放“大狗大狗大狗大狗”，随后播放“叫！”并释放持续的大范围直线音波。音波会攻击范围内除大狗自身以外的所有生物，包括玩家、召唤者和其他大狗，并且会持续破坏前方方块，破坏力按 TNT 级别处理。

当前仓库已经是 Fabric 1.20.1、Yarn 1.20.1、Fabric API 0.92.11、Java 17 的模板，不需要迁移加载器。仓库中已有用户未提交的 `fresh_fruit` 物品改动；实现时必须保留并兼容这些改动，不能覆盖或回滚它们。当前没有实体、声音、渲染器或测试实现。

本计划只覆盖讨论后确认的第一版可玩功能。正式表情包和正式“大狗叫”音频尚未提供；第一版可以使用简单的临时 PNG 和时长匹配的占位 OGG，正式素材替换前仍需再次讨论文件内容、尺寸和授权。

## 用户原始要求汇总（供 opencode 参考，避免误解）

> 本节每次更新计划时需与正文保持同步，确保 opencode 能看到完整的人为约束，而非仅技术方案。

1.  **基础约束**：Minecraft 1.20.1 + Fabric；继续使用当前 `big-fruit-mod` 项目，不覆盖已有的 `fresh_fruit` 未提交改动；所有贴图/音频等素材需先与用户讨论后再提供或替换，第一版允许使用临时占位素材。
2.  **生物形态**：新增一个类似狼的中立生物，模型替换为“大狗叫”表情包；最终确认为**二维 billboard**（始终面向观察者），后续仅替换纹理。
3.  **核心玩法**：受击后反击的远程音波攻击，类似循声守卫：先蓄力（期间循环播放“大狗大狗大狗大狗”）、结束时播放“叫！”并释放**持续的、大范围的直线 AOE**（叫声结束攻击即停）；中立，不主动索敌。
4.  **数值与范围（逐步确认）**：第一版采用测试默认值（蓄力 3s、释放 2s、射程 20→本次已提升至 30、宽度 2、每 10t 4 伤害、冷却 1s）；后续以正式音频时长为准同步（见本次迭代）。
5.  **召唤方式**：物品召唤，且**两者都要**：标准刷怪蛋 + 自定义召唤物（`BigDogSummonItem`）；按“混合方式”——刷怪蛋用于测试、召唤物暂仅创造/`/give`，第一版不擅自定生存配方。
6.  **目标筛选**：范围清场，但明确为**除自身外所有生物均受伤**，包括玩家、召唤者、其他大狗、队友、被动/敌对生物。
7.  **方块破坏**：音波需持续破坏前方方块，破坏力 **TNT 同级（强度 4）**，且为**持续推进**（每段只处理一次，`FIRING_TICKS` 内逐步推进至射程），允许穿墙，本次破坏距离由 20 提升至 30。
8.  **音频接入（本次）**：用户已将 `bigdog.MP3`（蓄力 6.11s）与 `bark_long.MP3`（释放 4.00s）放入 `src/main/resources/assets/big-fruit-mod/sounds/entity/big_dog/`；要求由我分配最终位置（已确认：转码后覆盖 `charge_loop.ogg`/`call.ogg`）、转换为 OGG Vorbis 单声道 22k、删除 MP3、并将蓄力/释放时长与音频同步（122t/80t）、蓄力音频按 opencode P0 结论改为**单次播放**（122t 恰好覆盖一次，不循环）。
9.  **协作约定**：关乎 MOD 制作的所有细节需先讨论再工作；计划文档需另存一份供 opencode 审阅，本文件与 `docs/BIG_DOG_PLAN_FOR_OPENCODE.md` 保持一致。

## 已确认的第一版规格

- Minecraft/Loader：Fabric 1.20.1。
- 实体显示名：`大狗叫`；推荐内部 ID：`big_dog`。
- 生物性格：中立，仅在受到有效攻击后反击；没有主动索敌。
- 攻击目标：音波范围内所有 `LivingEntity`，只排除大狗叫攻击实体自身；不排除召唤者、玩家、其他大狗或队友。
- 召唤方式：同时提供原版风格刷怪蛋和自定义召唤物。按“混合方式”先让刷怪蛋用于测试，自定义召唤物通过创造模式或 `/give` 获取，第一版暂不擅自设计生存配方。
- 显示方式：先实现始终面向观察者的二维 billboard；后续只替换纹理即可更新正式表情包。
- 临时素材：允许生成简单占位 PNG 和时长匹配的占位 OGG，不使用未经确认的正式梗图或音频。
- 攻击默认数值：蓄力 60 tick（3 秒）、释放 40 tick（2 秒）、射程 30 格（已按本次要求由 20 提升至 30）、宽度约 2 格、每 10 tick 对范围内目标造成 4 点伤害、结束后冷却 20 tick（1 秒）。
- 音频节奏：蓄力循环音约 1 秒，释放音约 2 秒；释放状态和生物伤害在固定 tick 到达时结束，使“叫！”音频结束与攻击停止同步。
- 方块效果：释放期间持续向前推进破坏。路径按格分段，每一段只处理一次，使用 TNT 级别（爆炸强度 4 的等效破坏）处理，避免每 tick 对同一地点重复爆炸；实体音波伤害独立按 10 tick 间隔计算。

## 推荐实现步骤

### 1. 整理现有注册并添加公共注册模块

保留现有 `ModItems.FRESH_FRUIT` 和其资源，不重写用户正在进行的水果物品功能。将 `ModItems` 整理为统一的注册入口，在其中加入自定义召唤物和 `SpawnEggItem`，并确保 `BigFruitMod.onInitialize()` 按顺序初始化实体、属性、声音和物品。清理仅在确认不影响现有水果物品的情况下进行，例如未使用的 import 或重复注册 helper。

新增：

- `registry/ModEntities.java`：注册 `BIG_DOG` 的 `EntityType`，设置碰撞箱、追踪距离和更新频率。
- `registry/ModSounds.java`：注册蓄力循环音和释放音两个 `SoundEvent`。
- `ModItems` 中的 `BIG_DOG_SPAWN_EGG` 与 `BIG_DOG_SUMMON`：分别使用标准刷怪蛋逻辑和自定义右键召唤逻辑。
- 适当加入创造模式物品栏入口，确保可以通过创造菜单和 `/give` 测试。

当前 `fabric.mod.json`、`build.gradle` 的 main/client split 和 Fabric datagen 入口已经满足需求，不为实体功能引入 Mixin 或自定义网络包。

### 2. 实现大狗叫实体与服务端状态机

新增 `entity/BigDogEntity.java`，建议继承 `PathAwareEntity`，不要继承 `WolfEntity` 或 `TameableEntity`，以免自动带入驯服、主人、繁殖和狼的敌对逻辑。

实现以下公共逻辑：

- 使用 `MobEntity.createMobAttributes()` 注册基础生命值、速度和碰撞属性。
- 只添加游荡、观察和基础移动目标，不添加主动 `ActiveTargetGoal`。
- 在 `damage(...)` 成功且伤害来源能解析出 `LivingEntity` 攻击者时触发反击；环境伤害没有攻击者时不强行选择目标。
- 使用服务端权威状态机：`IDLE -> CHARGING -> FIRING -> COOLDOWN -> IDLE`。
- `CHARGING` 期间停止导航并面向当前攻击者；进入 `FIRING` 时捕获水平释放方向，2 秒内固定沿该方向攻击。
- 目标失效、实体死亡或攻击被明确打断时清理状态和声音；正在蓄力/释放时忽略新的触发，避免多个伤害事件重置计时器。
- 用 1.20.1 的旧版 `DataTracker.registerData` / `startTracking` 同步客户端需要的状态、蓄力进度和释放方向；内部计时器和命中冷却只由服务端维护。
- 释放结束后进入 20 tick 冷却，冷却期间不能再次开始蓄力。

### 3. 实现音波伤害和持续方块破坏

在 `BigDogEntity` 中集中处理攻击状态，避免 AI Goal 与实体 tick 同时修改计时器。

- 进入 `CHARGING` 时由服务端播放蓄力音，并按约 20 tick 的间隔重播占位循环音（占位规格，最终以增量计划的 122t 单次播放为准）；取消蓄力时不再重播，且已播放的 6.11s 音频按 opencode 建议 2 方案 A 自然播完，不主动停止。
- 蓄力完成时播放一次“叫！”音，切换到 `FIRING`。
- `FIRING` 每 10 tick 查询固定方向的 30 格宽直线区域，对区域内所有存活生物造成 4 点伤害；排除自身，其他所有生物均可受伤。由 10t 全局间隔保证每个目标的 0.5s 受击节律，无需单独维护每目标命中 tick（已按 opencode P2 对齐实现，移除“为每个目标维护命中 tick”的误述）；攻击只在服务端执行，避免客户端/服务端双重扣血。
- 优先使用 Yarn 1.20.1 可用的 sonic boom DamageSource；若当前 mappings 没有对应方法，使用普通的实体攻击 DamageSource，不通过 Mixin 绕过 API。
- 将释放路径按 1 格左右的离散段推进，在 40 tick 内持续处理前方尚未处理的段（音频同步后为 80t 内推进 30 段）。每段只进行一次 TNT 等效强度 4（对应抗爆阈值 8.0，`blastResistance > 8.0f` 跳过）的方块破坏（`world.breakBlock(pos, false, this)`，已按用户确认**不掉落**，区别于原版 TNT 的掉落规则），并记录已处理段，防止重复爆炸导致不受控的指数破坏。
- 方块破坏实现必须在服务端执行，并避免让同一段的爆炸额外重复计算音波伤害；若使用原版爆炸 API，需要明确其实体伤害和掉落行为，必要时采用仅处理方块的等效破坏逻辑。
- 第一版按已确认规则允许音波穿墙，并允许破坏前方方块；不增加保护召唤者、队友或同类的例外。
- `FIRING` 计时结束后立即停止伤害、停止新的方块破坏和 beam 状态，再进入冷却。

### 4. 实现客户端 billboard 渲染与占位资源

在 `src/client/java` 中新增自定义 `BigDogBillboardRenderer`，用一张透明 PNG 绘制始终朝向摄像机的四边形，而不是使用新版 RenderState API。通过 `EntityRendererRegistry.register` 注册，并使用实体纹理位置返回占位图。实体状态可以先只同步声音和后续粒子所需的数据，第一版不强制加入复杂 beam 渲染。

新增或保留资源：

- `assets/big-fruit-mod/textures/entity/big_dog.png`：简单临时二维占位贴图。
- `assets/big-fruit-mod/sounds.json`：声明 `big_dog_charge_loop` 和 `big_dog_call`。
- `assets/big-fruit-mod/sounds/entity/big_dog/charge_loop.ogg`：约 1 秒占位音。
- `assets/big-fruit-mod/sounds/entity/big_dog/call.ogg`：约 2 秒占位音。
- `assets/big-fruit-mod/lang/zh_cn.json`、`en_us.json`：加入实体、刷怪蛋和召唤物名称，同时保留现有 `fresh_fruit` 文本。
- `assets/big-fruit-mod/models/item/big_dog_spawn_egg.json` 和自定义召唤物模型；刷怪蛋使用原版 spawn egg 模型父项。

占位资源仅用于本地测试。正式贴图和音频由用户提供或确认后，单独讨论分辨率、透明区域、OGG Vorbis 编码、音量、循环间隔、授权和再分发范围，再替换资源文件。

### 5. 实现自定义召唤物

自定义召唤物实现为普通 `Item` 的右键行为：在服务端根据玩家位置和朝向生成 `BIG_DOG`，设置初始旋转和生成位置，并在成功生成后消耗物品（创造模式不消耗）。标准刷怪蛋继续用于原版风格的快速测试。

第一版不添加自然生成、掉落表、繁殖、驯服、主人保护或生存配方，避免在核心音波机制稳定前扩大范围。后续如果要加入配方，会单独和用户讨论材料与获取途径。

## 验证方案

1. 运行 `./gradlew build`，确认 Java 17 编译、资源处理和 Fabric 模组打包成功。
2. 运行客户端，使用创造菜单或以下命令测试两种物品：
   - `/give @s big-fruit-mod:big_dog_spawn_egg`
   - `/give @s big-fruit-mod:big_dog_summon`
3. 使用刷怪蛋和召唤物分别生成实体，确认二维贴图始终面向观察者，实体碰撞箱和视觉占位大小可接受。
4. 用近战、投射物和其他生物攻击大狗，确认只有有效攻击者触发蓄力；检查 3 秒循环音、2 秒“叫！”音（占位规格，最终以增量计划的 6.11s 单次 / 4s 为准）、30 格直线、每 0.5 秒 4 点伤害和 1 秒冷却。
5. 将玩家、被动生物、敌对生物、其他大狗和召唤者放在音波路径中，确认除攻击实体自身外全部会受伤。
6. 在路径上放置方块，确认破坏从释放开始持续推进、每段只处理一次、破坏力接近 TNT 且覆盖 30 格，并确认不会因每 tick 重复爆炸造成异常扩大。
7. 测试目标死亡、实体卸载/重载、多只大狗同时攻击、客户端/服务端分离和专用服务器启动，确保状态与伤害只由服务端推进。
8. 检查 `git diff`，确认现有 `fresh_fruit` Java、语言、模型和贴图改动仍然存在且未被覆盖。正式素材替换前停止并先与用户讨论。

### Critical Files for Implementation

- `D:\Project\java_project\MCMOD\ex1\src\main\java\com\mymod\bigfruit\BigFruitMod.java`
- `D:\Project\java_project\MCMOD\ex1\src\main\java\com\mymod\bigfruit\item\ModItems.java`
- `D:\Project\java_project\MCMOD\ex1\src\main\java\com\mymod\bigfruit\registry\ModEntities.java`
- `D:\Project\java_project\MCMOD\ex1\src\main\java\com\mymod\bigfruit\entity\BigDogEntity.java`
- `D:\Project\java_project\MCMOD\ex1\src\client\java\com\mymod\bigfruit\client\BigFruitModClient.java`
- `D:\Project\java_project\MCMOD\ex1\src\client\java\com\mymod\bigfruit\client\render\BigDogBillboardRenderer.java`
- `D:\Project\java_project\MCMOD\ex1\src\main\resources\assets\big-fruit-mod\sounds.json`
- `D:\Project\java_project\MCMOD\ex1\src\main\resources\assets\big-fruit-mod\textures\entity\big_dog.png`

---

## 本次迭代：接入正式音频并同步时长（增量计划）

### Context（增量）

用户已在 `src/main/resources/assets/big-fruit-mod/sounds/entity/big_dog/` 下放入两段正式音频：`bigdog.MP3`（蓄力、约 6.11s、立体声 44.1kHz）与 `bark_long.MP3`（释放、约 4.00s）。当前已实现版本使用占位 OGG：`charge_loop.ogg`（1.00s）与 `call.ogg`（2.00s），并通过 `ModSounds.BIG_DOG_CHARGE_LOOP` / `BIG_DOG_CALL` 与 `sounds.json` 的 `big_dog_charge_loop` / `big_dog_call` 事件绑定；`BigDogEntity` 中以固定常量驱动：`CHARGING_TICKS=60`、`FIRING_TICKS=40`、`CHARGE_LOOP_INTERVAL=20`，且蓄力期间每 20t 重播循环音。用户本次要求：由我决定音频在资源树中的最终位置，并将蓄力/释放时长与音频时长同步。**已确认：覆盖占位文件、转换后删除 MP3、蓄力音频按 opencode P0 改为单次播放。**

关键约束：Minecraft 1.20.1 仅接受 OGG Vorbis（单声道更稳定），MP3 需转换为 OGG；`fresh_fruit` 相关改动仍需保留；不擅自决定最终梗图替换。

### 已勘察到的现状

- 新音频位置：`src/main/resources/assets/big-fruit-mod/sounds/entity/big_dog/bigdog.MP3`（6.11s）与 `bark_long.MP3`（4.00s），均为未跟踪文件，与占位 `charge_loop.ogg` / `call.ogg` 并存
- 当前注册：`ModSounds.java:12-13` 定义两个 `SoundEvent`（`big_dog_charge_loop` / `big_dog_call`），`sounds.json:2-12` 映射到 `entity/big_dog/charge_loop` 与 `entity/big_dog/call`
- 当前状态机：`BigDogEntity.java:32-39` 固定 60t/40t/20t，`RANGE` 仍为 20.0（本次需一并提升至 30），`tickCharging()` 每 20t 调用 `world.playSound(... BIG_DOG_CHARGE_LOOP)`（本次将改为单次播放，见下），`startFiring()` 播放 `BIG_DOG_CALL` 一次；伤害与方块破坏均以 `FIRING_TICKS` 为分母做进度推进
- 构建：`build.gradle` 的 `splitEnvironmentSourceSets` 与 `fabric.mod.json:16-25` 已满足，差异仅为资源与常量

### 推荐方案（增量，不推翻第一版）

**1. 资源分配与格式转换**

- 保留 `ModSounds` 的两个事件 ID 不变（避免改动注册与存档兼容性），仅替换底层 OGG 文件内容：`bigdog.MP3` 转码为 `charge_loop.ogg`，`bark_long.MP3` 转码为 `call.ogg`，覆盖当前占位文件
- 转码规格：`ffmpeg -i input.MP3 -c:a libvorbis -q:a 4 -ac 1 -ar 22050 output.ogg`（与占位一致的单声道 22k Vorbis，便于距离衰减），转换后删除两个 MP3（已确认），避免将 MP3 打入 jar 造成无效资源与体积增加
- `sounds.json` 保持 `entity/big_dog/charge_loop` 与 `entity/big_dog/call` 的映射不变，仅在需要时调整 `subtitle` 或增加 `attenuation_distance` 等可选字段，不新增事件（已确认覆盖方案）

**2. 时长同步**

- 以实测时长换算 tick：`bigdog 6.11s ≈ 122t`（122.2），`bark 4.00s ≈ 80t`；为与音频结束对齐，更新 `BigDogEntity.java:32-39` 为 `CHARGING_TICKS = 122`、`FIRING_TICKS = 80`（取整，允许 ±1t 误差；若需精确到采样点可在注释中记录 6.11s/4.00s），并将 `RANGE` 由 20 提升至 30（本次新增要求，`+10`）
- 蓄力循环逻辑（已按 opencode P0 与用户确认改为单次播放）：`CHARGING_TICKS` 122t 恰好等于音频 6.11s，进入 `CHARGING` 时播一次 `BIG_DOG_CHARGE_LOOP` 即完整覆盖蓄力，`CHARGE_LOOP_INTERVAL` 设为 ≥122t（实际不再重播，避免与未播完的音频重叠）；`FIRING` 仍为进入时播一次 `BARK`，伤害/破坏的 `DAMAGE_INTERVAL` 与分段推进保持不变但会随新的 `FIRING_TICKS`（80t）与 `RANGE`（30）自动拉伸至 30 段
- 保留 `COOLDOWN_TICKS = 20` 与 `HALF_WIDTH/DAMAGE` 不变；`writeCustomDataToNbt` / `readCustomDataFromNbt` 仅持久化 `chargingTicksRemaining/firingTicksRemaining/cooldownTicksRemaining/firingDir` 与状态，`chargeLoopCooldown/damageIntervalCooldown/processedSegments/brokenThisAttack` 不持久化，`readCustomDataFromNbt` 对 `CHARGING/FIRING` 做 `cancelAttack()` 重置，中途存档会导致蓄力/释放重置，属可接受行为（opencode P3）；另按 opencode 建议 2 方案 A，中途取消蓄力时已播放的 6.11s 音频不主动停止、自然播完（`world.playSound` 一次性播放，无 SoundInstance 句柄），属可接受边界

**3. 关键文件变更（增量）**

- 覆盖：`src/main/resources/assets/big-fruit-mod/sounds/entity/big_dog/charge_loop.ogg`（由 `bigdog.MP3` 转码）、`src/main/resources/assets/big-fruit-mod/sounds/entity/big_dog/call.ogg`（由 `bark_long.MP3` 转码）
- 删除：`src/main/resources/assets/big-fruit-mod/sounds/entity/big_dog/bigdog.MP3`、`bark_long.MP3`（已确认删除）
- 修改：`src/main/java/com/mymod/bigfruit/entity/BigDogEntity.java:32-39` 常量（`CHARGING_TICKS` 122 / `FIRING_TICKS` 80 / `RANGE` 30 / `CHARGE_LOOP_INTERVAL` ≥122 单次）与 `tickCharging()` 的循环播放分支（改为单次，P0）及 `tryBreakBlock()` 的 `drop=false`（P1）；按需微调 `src/main/resources/assets/big-fruit-mod/sounds.json` 的注释/字幕

**4. 验证**

- `ffprobe` 核对转换后 OGG 时长为 6.11s±0.05 与 4.00s±0.05，`./gradlew build` 确认无 `MP3` 未转换残留且 jar 内 `assets/.../charge_loop.ogg` 与 `call.ogg` 为新文件
- 游戏内：`/give` 两种召唤物、` /summon`、受击后观测蓄力约 6.1s 单次完整播放（已按 P0 改为不循环）、`bark` 约 4s 内释放直线 AOE 与按段破坏完整覆盖 30 格（已由 20 提升至 30，`drop=false` 不掉落，P1），结束即停；检查 `git diff` 中 `fresh_fruit` 仍保留

### 待确认（需用户拍板后再替换正式梗图）

- 是否需要微调音量/衰减距离，或保持 `1.2f/2.0f` 的现有 `playSound` 参数

---

## 第二次迭代：死亡截断、光波特效、平衡与贴图（本次新增 5 点，2026-08-22）

### 用户新增要求（已确认细节）

1.  **死亡音频**：大狗死亡后“大狗大狗…”仍在播放不合理，需修复——**立即截断**（而非自然播完），音频即攻击叫声，死亡即停。
2.  **光波特效**：音波需可视化为**黄色光波**，光柱 + 多个平行于横截面的光环围绕。
3.  **平衡**：生命 40→**50**，护甲略微提高至 **4**（`GENERIC_ARMOR`），横截面半宽 1.0→**1.5**（直径 3），距离保持 30 不变。
4.  **贴图**：`textures/entity/` 已放入 `barkwhite.jpg`（488×511，释放时）与 `nobarkwhite.jpg`（488×511，常态/蓄力），需**转为带透明 PNG**并按状态切换（`bark` 仅 FIRING，`nobark` 其余）。
5.  **击退**：光波略微增加击退（建议 **0.8**，`applySonicDamage` 内对命中目标施加）。

### 已勘察现状（增量前）

- 音频：`BigDogEntity` 用 `world.playSound(null, ...)` 一次性播放，`tickCharging` 122t 单次、`startFiring` 80t 单次，`cancelAttack` 与死亡路径均不持有 SoundInstance，死亡后已播音频无法截断；`isDead/isRemoved` 仅在 `tickCharging` 首行检查是否取消，未处理已播声音。
- 特效：仅服务端 `Box`/`isInBeam` 判定，无任何 `RenderLayer`/`Particle`/`Beam` 绘制；`BigDogBillboardRenderer` 仅 `getEntityCutoutNoCull(big_dog.png)` 单张 64×64 billboard，不感知状态。
- 属性：`createAttributes` 为 40 血 / 0.28 移速 / 32 跟随 / 0.2 抗击退 / 2 攻击，无护甲；`RANGE 30`、`HALF_WIDTH 1.0`、`DAMAGE 4`、`DAMAGE_INTERVAL 10`；`applySonicDamage` 仅 `damage()`，无击退。
- 贴图：`textures/entity/` 下 `barkwhite.jpg`/`nobarkwhite.jpg` 为 488×511 RGB JPG 无 alpha，未被 `ModEntities`/`Renderer` 引用；`big_dog.png` 仍为占位；`ModEntities` 尺寸 `0.8×1.2`，billboard 半宽 0.6。

### 推荐方案

**1. 死亡截断（由“自然播完”改为“立即截断”）**

- 现状 `world.playSound(null, ...)` 为一次性世界音，`cancelAttack` 与死亡路径均不持有句柄，死亡后已播音频无法截断，且服务端 `world.playSound` 会向所有客户端广播，若再叠加客户端 SoundInstance 会**双播**；且 `tick()` 仅检查 `world.isClient`，未检查 `!isAlive()/isRemoved()`，死亡后约 20t 的 `KILLED` 移除窗口仍会进入 `CHARGING/FIRING`。修正：**删除两处服务端 `world.playSound`**（`tickCharging` 与 `startFiring`），改为**客户端持有的 `EntityTrackingSoundInstance`**（`net.minecraft.client.sound.EntityTrackingSoundInstance` 可直接实例化，随实体定位与衰减，`tick()` 自动更新位置并在实体移除时自停；`MovingSoundInstance` 为抽象基类不直接使用），通过 `MinecraftClient.getInstance().getSoundManager().play()` 持有句柄，`DataTracker STATE` 驱动生命周期；服务端 `tick` 首行追加 `if (!isAlive()||isRemoved()) {cancelAttack(); return;}`，客户端每 tick 检查 `!entity.isAlive()||isRemoved()||state!=CHARGING/FIRING` 时立即 `soundManager.stop(instance)`。`trackedUpdateRate=2` 至多 2t 启动延迟可接受；`stopSounds(事件ID/类别)` 会误停多只狗，不采用。

**2. 光波特效（黄色光柱 + 光环）**

- 为实现可视化，`firingDir` 仅普通字段无法让客户端获知固定方向，需新增 **DataTracker 方向**（建议单 `INTEGER` 角度或两个 `FLOAT` 的 `firingDirX/Z` 同步）及 **FIRING_PROGRESS（0..100）**，供客户端计算推进。新增 `client/render/BigDogSonicBeamRenderer` 或在 `BigDogBillboardRenderer` 内当 `state==FIRING` 时叠加渲染：以 `firingDir` 为轴，用 `MatrixStack` 沿射线方向绘制半透明黄色光柱（`RenderLayer.getEntityTranslucent` 或 `getBeaconBeam` 变体，`0xFFFFE040` 附近，白色辅助纹理着色），并在光柱上每 4-5 格生成一个**平行于横截面的光环**（6 个环、16 段环带、半径≈`HALF_WIDTH 1.5`、间距约 5，按 `FIRING_PROGRESS` 沿 dir 推进并用 `tickDelta` 平滑，随进度淡出）。仅客户端 `isClient` 渲染，不影响服务端判定；长光柱需注意视锥边界。

**3. 平衡**

- `createAttributes` 中 `GENERIC_MAX_HEALTH 40→50`、`GENERIC_ARMOR 4`、`HALF_WIDTH 1.0→1.5`；`RANGE` 保持 30；`GENERIC_KNOCKBACK_RESISTANCE` 保持 **0.2**（0.35 为需求外改动，除非另确认）；`applySonicDamage` 中 `sonicBoom(this)` 已有 0.4 自动击退（`LivingEntity.damage` 内），此处额外 `takeKnockback(0.4, -dir)` 使总强度 0.8，方向取反，需 `velocityDirty`。

**4. 贴图**

- 将 `barkwhite.jpg`/`nobarkwhite.jpg` 转为 `bark.png`/`nobark.png`（Pillow 转 PNG，白色背景转为透明或保留白底视需求，488×511 需缩至 64/128 方幂或保持原尺寸但需配置 UV）；`BigDogBillboardRenderer.getTexture()` 改为按 `entity.getState()` 返回：`FIRING → bark.png`，其余 → `nobark.png`；删除占位 `big_dog.png` 或保留作 fallback。`ModEntities` 尺寸保持 `0.8×1.2`，billboard 半宽可随新贴图微调至 0.6-0.8。

**5. 击退**

- 与 3 同步，在 `applySonicDamage` 内对每个命中目标 `takeKnockback(0.4, -dir)`（0.4 + 已有 0.4 = 总强度 0.8，方向取反），与 10t 伤害节律一致；玩家额外 `velocityModified` 标记。

**关键文件变更**

- 修改：`src/main/java/com/mymod/bigfruit/entity/BigDogEntity.java:68-71` 属性、`36-37` `HALF_WIDTH`/`RANGE`、`32-39` 常量、`205-237` tick 死亡截断、`250-279` 击退
- 新增/修改：`src/client/java/com/mymod/bigfruit/client/render/BigDogBillboardRenderer.java` 状态感知贴图、`src/client/java/com/mymod/bigfruit/client/render/BigDogSonicBeamRenderer.java`（或内联）与 `src/client/java/com/mymod/bigfruit/client/sound/BigDogEntitySound.java` 可停止音频（原名 `BigDogLoopSound` 易与“单次”混淆，已更名）
- 转换：`src/main/resources/assets/big-fruit-mod/textures/entity/barkwhite.jpg`→`bark.png`、`nobarkwhite.jpg`→`nobark.png`（带透明），`src/main/resources/assets/big-fruit-mod/sounds.json` 为 `big_dog_call`（必要时 `big_dog_charge_loop`）显式加 `"attenuation_distance": 32`（默认 16 无法覆盖 30 格，需确保 30 格处可闻）

**验证**

- 死亡截断：蓄力/释放中击杀大狗，`charge_loop`/`call` 立即停止，不再播至结束；`./gradlew build` 无 SoundInstance 泄漏
- 光波：`FIRING` 80t 内可见黄色光柱 + 4-6 光环随射线推进，平行于横截面，`RANGE 30` 内完整覆盖
- 平衡：`/summon` 后 `Health 50`、受击减伤可见护甲生效、光波命中时目标被击退、`Box` 宽度 3 格内均命中
- 贴图：`IDLE/CHARGING/COOLDOWN` 显示 `nobark`，`FIRING` 切换为 `bark`，JPG 已转为 PNG 且带透明
- 声音可闻：`sounds.json` `attenuation_distance 32` 确保 30 格处能听到 `bark`/`charge`，与 `EntityTrackingSoundInstance` 共同决定可听距离
- `fresh_fruit` 仍保留，`build` 通过

---

## 第三次迭代：全向光波 + 数值加强（2026-08-24）

### Context

大狗叫第二迭代已闭环（死亡截断/黄色光柱+光环/50血4甲/1.5半宽/0.8击退/bark/nobark 透明切换，build通过，3轮opencode+并发复核）。当前光波强制水平（`startFiring` 中 `dir.y=0`、`FIRING_DIR_X/Z` 仅二维、`SonicBeamRenderer` 仅yaw、`processBlockDestruction` 水平分支），射程30、半宽1.5、伤害4。用户提出第三迭代：在保留第二迭代所有成果（`fresh_fruit` 仍保留、音频122t/80t、死亡截断、双召唤物）的前提下，让光波可朝任意方向并同步加强数值。

### 用户原始要求汇总（本次新增，供 opencode 参考）

1. **全向发射**：光波现在只能朝正面垂直方向（实际为水平）发射，需改为可朝任意方向（完全3D、无俯仰限制），能瞄向天上/地下的攻击者。（已确认：采用"狗 eyePos → 目标 eyePos"的3D向量）
2. **数值加强**：生命 50→**80**、护甲 4→**8**、光波横截面半径+1格（半宽 1.5→**2.5**，直径5）、破坏范围随光波同步扩大（与伤害半径一致，约5×5实心圆柱）、伤害 4→**6**。距离、时长、击退、冷却等其余保持不变。（已确认三问）

### 已勘察现状（增量前）

- `BigDogEntity.java:31-44`：`RANGE 30.0`、`HALF_WIDTH 1.5`、`DAMAGE 4.0f`、`CHARGING 122t/FIRING 80t/COOLDOWN 20t`；属性 `MAX_HEALTH 50/ARMOR 4/KB_RESIST 0.2`；DataTracker 仅 `FIRING_DIR_X/Z`(FLOAT) + `FIRING_PROGRESS` + `STATE/CHARGE_PROGRESS`，无Y；`firingDirX/Z` 为普通double字段 + NBT `FiringDirX/Z`
- `startFiring:168-186`：`dir = (targetX - selfX, 0, targetZ - selfZ)` 强制 y=0，fallback `getRotationVector().multiply(1,0,1)`；`applySonicDamage:284-286` `origin=pos+(0,eyeY-Y-0.3,0)` + `dir=(firingDirX,0,firingDirZ)`；`isInBeam` 已是3D投影但受限于水平dir；`processBlockDestruction:342-356` 水平分支 `if(|dirX|>|dirZ|) p=center.add(0,dy,dx)` 且每段仅十字3格
- `BigDogSonicBeamRenderer.java:20-49`：仅取 X/Z、归一化后 `yaw=atan2(Z,X)-90`、绕Y旋转，`radius=1.5f`、`beamLen=30.0f` 恒定、`hw/hh=0.32`、`rings=6` 间距5；环面位于 XY 平面、沿Z推进，不支持俯仰
- `createAttributes:69-76`、`ModEntities` 0.8×1.2、`sounds.json` attenuation 32 已满足30格可闻

### 推荐方案（增量，不推翻前两版）

**1. 服务端：DataTracker 与方向改为3D**

- 新增 `TrackedData<Float> FIRING_DIR_Y`，声明/注册在现有 tracked 字段末尾（避免改变旧 `FIRING_DIR_X/Z/FIRING_PROGRESS` 的 DataTracker ID 顺序）；`initDataTracker` 默认 `0f`；新增 `getFiringDirYTracked()`、普通字段 `firingDirY`、`getFiringDirY()`；NBT 新增 `FiringDirY` 读写，旧存档缺失默认0
- `startFiring()`：`origin = getEyePos()`；若 `revengeTarget` 存活，`dir = revengeTarget.getEyePos().subtract(origin)`（眼睛对眼睛，含Y）；否则 `dir = getRotationVector()`（不再 `multiply(1,0,1)`，保留完整俯仰）；`lengthSquared<1e-6` 回退 `(0,0,1)`；normalize 后同时写入 `firingDirX/Y/Z` 与三个 DataTracker。蓄力期间 `lookAt(revengeTarget)` 保持含Y视觉跟随
- 所有旧逻辑中 `new Vec3d(firingDirX,0,firingDirZ)` 改为 `new Vec3d(firingDirX,firingDirY,firingDirZ)`

**2. 服务端：伤害 / 判定 / 破坏改为3D**

- `applySonicDamage()`：`origin = getEyePos()`，`dir` 3D，`end = origin+dir*RANGE`；AABB 三轴均取 `min(origin,end)±HALF_WIDTH` / `max(origin,end)±HALF_WIDTH`（不再只围绕 origin.y）；`takeKnockback(0.4, -dir.x, -dir.z)` 保持水平击退（Y击退由 `damage` 内部处理，避免击飞过高）
- `isInBeam()`：复用三维投影 `proj = toTarget.dot(dir)`，过滤 `proj<0 || proj>RANGE`，`distSq <= (HALF_WIDTH + width*0.5)^2`；`toTarget` 的 Y 用 `targetY+height*0.5 - origin.y`，与3D dir 一致
- `processBlockDestruction()`：`origin = getEyePos()` 统一；`dir` 3D；删除旧水平十字分支，改为**半径内实心圆柱**（按 opencode P1-1 修订）：`r = floor(HALF_WIDTH)`=2 遍历 `dx,dy,dz∈[-r,r]`，每块判定用**薄片约束** `|axial| = |(p-center)·dir| ≤ 0.5`（保证每段是垂直于光波轴的 1 格厚薄片，相邻段间隔 1 格无缝覆盖 30 格）+ **圆柱半径** `perpSq = |p-center|² - axial² ≤ HALF_WIDTH²`（6.25，半径与伤害一致=2.5），每段约 21 块（5×5 减角），总尝试约 630 次/攻击；去重用 `brokenThisAttack`，保留 `drop=false` 与 `blastResistance>8.0` 跳过
- `cancelAttack()` 补清 `FIRING_PROGRESS=0`

**3. 数值常量**

- `HALF_WIDTH 1.5→2.5`、`DAMAGE 4.0f→6.0f`、`GENERIC_MAX_HEALTH 50→80`、`GENERIC_ARMOR 4→8`；`RANGE 30`、`DAMAGE_INTERVAL 10`、`CHARGING 122/FIRING 80/COOLDOWN 20`、`KNOCKBACK_RESISTANCE 0.2` 不变

**4. 客户端：渲染改为3D**

- `BigDogSonicBeamRenderer.render()`：同时读取 `FIRING_DIR_X/Y/Z`（fallback 到普通字段）；用三维长度判有效性（不能只判 X/Z——完全垂直时光波 X/Z 可能同时为零）；归一化后用 `Quaternionf.rotationTo(new Vector3f(0,0,1), dir3d)` **单步**将本地 +Z 轴对齐到 dir（opencode P2-2，绕开近垂直 `atan2(0,0)` 奇点；yaw+pitch 组合仅作备选注释）；`radius = (float)HALF_WIDTH`（2.5）、`beamLen = RANGE`（引用常量不再硬编码30/1.5）；`translate(0, getEyeY()-getY(), 0)` 替代固定0.7；环面局部 XY 平面经四元数旋转后自然垂直于 dir；各 quad 法线按各自局部平面设置（不再统一 `(0,1,0)`）
- 视锥：`BigDogBillboardRenderer` 可选覆盖 `shouldRender()`，用光波 swept Box（端点±HALF_WIDTH）做 Frustum 判定，避免大狗在视野边缘时整条30格光波被提前裁剪

**5. 同步与兼容**

- 新增 `FIRING_DIR_Y` 追加在旧 tracker 末尾保持 ID 顺序；三个方向分量在同一个 `startFiring()` tick 写入；客户端遇未同步完整时用三维长度+有限性校验，X/Z 为零但 Y 合法时不得丢弃
- `sounds.json` / `ModSounds` / `BigDogEntitySound` / `BigFruitModClient` 无需改动（死亡截断已在客户端跟踪）

### 验证方案

1. `./gradlew build` 通过，`fresh_fruit` 未覆盖
2. 水平目标：受击后光波仍水平命中，伤害6、击退0.8、每10t一次
3. 垂直目标：高塔/深坑中攻击，狗抬头/低头，光柱沿3D方向延伸30格，光环垂直于光柱推进，`RANGE` 内目标受伤
4. 方块破坏：斜向/垂直发射，破坏截面约5×5实心圆柱（`floor(HALF_WIDTH)`+薄片约束，每段约21块），每段仅一次，`blastResistance>8.0` 跳过、`drop=false`、30段内完整覆盖；多只大狗同时 FIRING 时观察服务端性能（P2-1，峰值约8~10块/tick）
5. 渲染：`FIRING` 80t内光柱30格、6环×16段随 `FIRING_PROGRESS` 推进、近垂直不消失；死亡时声音立即截断
6. 存档兼容：旧存档加载方向Y默认0，仍可正常进入下一轮攻击

### Critical Files for Implementation

- `src/main/java/com/mymod/bigfruit/entity/BigDogEntity.java`
- `src/client/java/com/mymod/bigfruit/client/render/BigDogSonicBeamRenderer.java`
- `src/client/java/com/mymod/bigfruit/client/render/BigDogBillboardRenderer.java`（视锥/调用）
- `src/main/java/com/mymod/bigfruit/registry/ModEntities.java`（确认尺寸无需改）
- `src/main/resources/assets/big-fruit-mod/sounds.json`（确认 attenuation 32 保持）

---

## 第四次迭代：动漫冲击波 3D 光波 + 破坏提速（本次，计划中）

### Context

第三迭代已落码（80血/8甲/2.5半宽/6伤害、完全3D全向、`rotationTo` 对齐、薄片圆柱破坏，opencode 8-9轮通过，build通过）。当前光波在第三迭代后已是3D方向，但视觉仍廉价、且部分角度不可见：经勘察 `BigDogSonicBeamRenderer` 为2片薄交叉quad（hw=hh=0.32，整束仅0.64宽 vs 判定直径5）、`BEAM_LAYER=getBeaconBeam(..., true)` 开启背面剔除（camera在 -Y/-X侧时两片同时被剔除→整束消失）、`quad()` 法线全写 `(0,1,0)`（第二片法线错误→光照暗）、几何过薄导致掠射角投影接近0。用户反馈"太廉价、不是3D、有些角度看不见"，并提供参考图 `src/main/resources/light.jpg`（龙息三视图：中心双螺旋亮核 + 多层六边形嵌套环 + 蓝色电弧 + 外层火焰/电路纹理 + 顶部冲击波/地面龟裂），要求改为**动漫冲击波**风格并保留黄系，且破坏"加快一点点"。

### 用户原始要求汇总（本次新增，供 opencode 参考）

1. **光波视觉升级**：太廉价、非3D、部分角度消失，需改为炫酷3D特效，风格已选 **动漫冲击波**（按三视图龙息：双螺旋内核 + 多层六边形光环 + 电弧外晕 + 粒子弥散 + 地面冲击感）。颜色保持黄系但需层次感（内核更亮、外晕更饱和）。
2. **破坏提速**：当前30格破坏用80t（4s，0.375格/tick）推进太慢，要求"加快一点点"，已确认提速至 **约50t跑完30格（0.6格/tick，提速60%），仍保留推进感**，而非瞬间打满。伤害节律与音频时长（122t/80t）保持不变，破坏提前完成、后续仅视觉/伤害延续。
3. **参考图处置**：`src/main/resources/light.jpg` 仅作设计参考，不应打入jar；要求使用后移到合适位置或删除。

### 已勘察现状（增量前）

- `BigDogSonicBeamRenderer.java`（当前 rotationTo 3D 版）：`BEAM_TEX=beacon_beam.png`、`BEAM_LAYER=getBeaconBeam(..., true)`（第二参为 depthTest 而非剔除开关，beacon beam 层本身双面 `Cull.DISABLE`；原计划误诊为剔除）；`hw/hh=0.32` 双交叉quad共8顶点；`radius=HALF_WIDTH 2.5`、`rings=6` 每环16段、`thick 0.18`，`getEntityTranslucent` 双面；`quad()` 法线固定 `(0,1,0)`（第二片法线错误）；`render()` 仅 `FIRING` 时绘制，`translate eyeY` + `rotationTo(+Z,dir)` 已3D对齐
- `BigDogBillboardRenderer.java`：1.2×1.2 billboard + `FIRING` 时叠加 beam，状态感知贴图
- `BigDogEntity.java`：`FIRING_TICKS 80` 驱动 `FIRING_PROGRESS` + `processBlockDestruction`（30段/80t，每tick最多1段，`r=floor(2.5)=2` 薄片圆柱）；`applySonicDamage` 每10t 半径2.5 圆柱
- `src/main/resources/light.jpg`：JPG 三视图，位于 `src/main/resources` 会被 gradle 打入 `build/resources/main`，需移出

### 推荐方案（增量，不推翻前三版）

**1. 修复"部分角度看不见"（P0，按 P1-2 修正）**

- 根因：0.64 宽交叉片在掠射角投影接近0 + 第二片法线固定 `(0,1,0)` 过暗 + α 仅0.42；beacon beam 层本身双面（第二参 `depthTest` 非剔除），原计划误诊为剔除
- 修复：`BEAM_LAYER` 改为 **双面且保留深度测试**：`RenderLayer.getEntityTranslucent(BEAM_TEX)`（Yarn 源码 `Cull.DISABLE`），或维持 `getBeaconBeam(..., true)` 不动；**不要**用 `getBeaconBeam(..., false)`（会关深度测试→穿墙可见）
- 光束本体由2片十字升级为 **6～8片围成的棱柱/圆柱**（如6边棱柱绕Z每60°一片，共6*4=24顶点，或8边形 8*4=32顶点，修正原"16片"笔误），使任意掠射角均有厚度；每片法线按真实面朝外计算（`normal = outerDir`），不再固定 `(0,1,0)`

**2. 动漫冲击波视觉升级（黄系层次感，参考三视图）**

- **外层光柱**：半径 `HALF_WIDTH 2.5`（与判定一致），半透明橙黄晕（α 0.22～0.32），6～8片棱柱侧面，纹理沿Z滚动（`progress` 驱动 UV 偏移，`tickDelta` 平滑）
- **内核光柱**：半径约 0.7～1.0 的更亮内芯（α 0.65～0.85，颜色近白黄 `1.0,0.96,0.72`），同样棱柱但略短/随推进淡出，形成"外晕包亮核"的体积感
- **双螺旋**：在内核内用2条细螺旋带（各8～12段小quad，半径0.3，随 **0..1 归一化 progress（`getFiringProgress()/100.0f`）** 旋转 `progress* TAU` + `tickDelta`），颜色白黄 + 轻微自发光，模拟参考图中扭曲核心
- **光环升级**：由6个圆形扁环改为 **6个六边形冲击环**（每环6段棱柱而非16段圆环，或保留16段但顶点按六边形半径调制），`thick` 0.18→0.22，颜色外环 `1.0,0.78,0.15` α 0.55～0.72，**边缘叠加蓝色电弧**（第二层同半径、更薄、颜色 `0.45,0.68,1.0` α 0.45，`uvOffset = (progress * 8.0f) % 1.0f` 闪烁），段间距保持5，沿光束 `base = i*5 + (progress*6)%5` 推进（progress 均指 0..1 归一化）
- **粒子/冲击感（轻量）**：发射口处可选 1～2 帧的环形粒子/发光quad，地面冲击点可选裂纹贴花（低优先级）；优先保证光束+光环主体

**3. 破坏提速（30格 80t → 约50t，按 P1-1 修正公式）**

- 新增常量 `BLOCK_BREAK_DURATION_TICKS = 50`，`processBlockDestruction` 的 `targetSegments` 改为 `ceil(progressDestruction * totalSegments)`，其中 `progressDestruction = clamp((FIRING_TICKS - firingTicksRemaining) / (float) BLOCK_BREAK_DURATION_TICKS, 0.0, 1.0)`（elapsed 模式，与现有 `BigDogEntity.java:259/345` 一致），使30段在前50t内跑完（峰值约 0.6 段/tick，`while` 连续推进可偶发2段/tick），剩余30t仅保留伤害/视觉
- `brokenThisAttack` 去重、`tryBreakBlock` 逻辑、`HALF_WIDTH` 半径、`drop=false`/`blastResistance>8` 保持不变

**4. 参考图处置**

- 使用后将 `src/main/resources/light.jpg` 移至 `docs/reference/dragon-breath-ref.jpg`（或 `docs/REFERENCE/`），或若无需保留则删除；确保 `src/main/resources` 下不再包含该 JPG，避免打入 `build/resources/main` 与 jar

### 关键文件变更（本次）

- 修改：`src/client/java/com/mymod/bigfruit/client/render/BigDogSonicBeamRenderer.java`（剔除/法线/棱柱化/双层光柱/双螺旋/六边形环+电弧/滚动UV）
- 轻改：`src/main/java/com/mymod/bigfruit/entity/BigDogEntity.java`（新增 `BLOCK_BREAK_DURATION_TICKS 50`、`processBlockDestruction` 用新分母）
- 可选：`src/client/java/com/mymod/bigfruit/client/render/BigDogBillboardRenderer.java`（视锥 swept Box）
- 资源处置：移动/删除 `src/main/resources/light.jpg` → `docs/reference/dragon-breath-ref.jpg`

### 验证方案（本次）

1. `./gradlew build` 通过，`light.jpg` 不在 `build/resources/main` 与 jar 中，`fresh_fruit` 未覆盖
2. 廉价修复：`FIRING` 时从任意角度（正前方、正后方、侧面、顶部、掠射角）观察光束均可见，无"环还在柱没了"现象；法线正确导致光照均匀
3. 3D体积感：外晕直径5（与判定一致）、内核更亮、双螺旋随时间旋转、光环为六边形且边缘有蓝电弧闪烁，整体黄系层次分明，近参考图动漫冲击波观感
4. 破坏提速：30格破坏在约50t内跑完（计时观察方块推进明显快于80t版），80t 内伤害仍每10t一次、音频完整、光束全程可见；多狗同时 `FIRING` 时服务端无明显卡顿
5. 回归：水平/垂直/斜向命中、死亡截断、存档兼容均保持第三迭代行为

---

## 第五次迭代：护甲伤害 + 鲜艳龙息配色 + 射程50 + 破坏覆盖视觉（本次，已落码）

### Context

第四迭代已闭环（动漫冲击波 6棱柱+双螺旋+六边环+蓝电弧、`BLOCK_BREAK_DURATION_TICKS 50` 50t前推、80血/8甲/0.8击退、铁傀儡式持续追杀、opencode 10-11轮通过、build通过）。用户实测反馈4点需增量修复，本迭代不推翻既有架构（`fresh_fruit`保留、122t/80t/20t状态机、3D全向、死亡截断、双召唤物）。

### 用户原始要求汇总（本次新增，供 opencode 参考）

1. **伤害去无视甲**：当前 `sonicBoom` 伤害无视护甲，需改为直接伤害（已确认改用 `mobAttack`，受护甲/抗性/保护附魔减免，数值仍6）
2. **光波更鲜艳**：偏淡，需更鲜艳贴近 `docs/reference/dragon-breath-ref.jpg`（已确认先出色板对比，已选方案A暖金冲击波）
3. **射程加长**：30→50格
4. **破坏覆盖特效**：特效盖住的地方都能被破坏（已确认全同步 HALF_WIDTH 2.5→3.0，直径6，伤害/破坏/视觉三者同步）

### 已勘察现状（增量前）

- `BigDogEntity.java:36-40` `RANGE 30`/`HALF_WIDTH 2.5`/`DAMAGE 6`/`BLOCK_BREAK_DURATION 50`/`FOLLOW_RANGE 32`
- `applySonicDamage:321-326` `sonicBoom(this)` 尝试（`bypasses_armor:true`）为主
- `processBlockDestruction:354-389` `r=floor(2.5)=2` 薄片圆柱每段21块总量630，视觉外沿2.5+0.22=2.72略超出破坏
- `BigDogSonicBeamRenderer.java` 外棱柱 `0.95,0.78,0.15 α0.22-0.38`、内核 `1.0,0.97,0.64 α0.62`、螺旋 `1.0,0.96,0.72 α0.55`、环 `1.0,0.86,0.18 α0.68` + 电弧 `0.52,0.72,1.0 α0.45`，均偏淡

### 推荐方案（增量，已落码）

**1. 伤害：** `applySonicDamage` 改 `getDamageSources().mobAttack(this)`（受甲），`mobAttack` 无内置击退直接 `takeKnockback(0.8, -dir)` 保持总击退0.8，删除 `try sonicBoom`

**2. 鲜艳：** 仅改 `BigDogSonicBeamRenderer` 6处 `color/alpha`（方案A暖金冲击波，已选）：
- 外棱柱 `0.95,0.78,0.15 → lerp(1.0,0.62,0.05, 1.0,0.85,0.35)`（helix驱动，`g 0.62→0.85, b 0.05→0.35`）、`α0.22-0.38→0.32-0.55`
- 内核 `1.0,0.97,0.64 α0.62→1.0,0.92,0.18 α0.75`
- 双螺旋 `1.0,0.96,0.72 α0.55→1.0,0.96,0.35 α0.65`
- 环 `1.0,0.86,0.18 α0.68→1.0,0.78,0.05 α0.78`
- 电弧 `0.52,0.72,1.0 α0.45→0.35,0.60,1.0 α0.60`
- 环数 `6→max(6, ceil(length/5))=10` 覆盖50格；`EntityTranslucent` 双面保留

**3. 距离：** `RANGE 30→50`、`FOLLOW_RANGE 32→64`、`sounds.json attenuation 32→64`、`BLOCK_BREAK_DURATION 50` 保持（50段/50t=1.0段/tick）

**4. 破坏覆盖：** `HALF_WIDTH 2.5→3.0`（直径6）三者同步；破坏 `r=3` 每段约28-30块总量约1500/攻击，峰值1段/tick≈30块/tick，50t跑完；环外沿 `3.0+thick0.22=3.22` 比破坏半径多0.22格为半透明装饰余量，可接受

### 关键文件变更（本次）

- 修改：`src/main/java/com/mymod/bigfruit/entity/BigDogEntity.java`（RANGE/FOLLOW/HALF_WIDTH/伤害源）
- 修改：`src/client/java/com/mymod/bigfruit/client/render/BigDogSonicBeamRenderer.java`（5处颜色/α + 环数随RANGE）
- 轻改：`src/main/resources/assets/big-fruit-mod/sounds.json`（32→64）
- 资源：`docs/reference/dragon-breath-ref.jpg` 已就位

### 验证方案（本次）

1. `./gradlew build` 通过，`fresh_fruit` 未覆盖
2. 伤害：无甲仍6/10t，穿甲伤害显著降低，击退0.8保持
3. 颜色：方案A暖金（外晕橙金、内核金黄、电弧更蓝）多角度不再偏淡
4. 距离：50格内目标可命中，声音50格可闻，FOLLOW 64下远目标可追踪
5. 破坏：直径6全覆盖，50格50t跑完，约1500块/攻击，多狗并发无卡顿
6. 回归：死亡截断、3D全向、122t/80t/20t、80血8甲、持续追杀保持

