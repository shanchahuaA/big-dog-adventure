# opencode 对开发计划的审查意见（第 8 轮）

> 审查对象：`BIG_DOG_PLAN_FOR_OPENCODE.md`"第三次迭代：全向光波 + 数值加强"（第 230-292 行）
> 审查者：opencode

## 现状抽查（通过）

- `BigDogEntity.java:176` `dir = getRotationVector().multiply(1,0,1)`、`:43-44` 仅 `FIRING_DIR_X/Z`、`:289-292` AABB `minY/maxY = origin.y ± HALF_WIDTH`——与计划"已勘察现状"一致。
- `BigDogSonicBeamRenderer.java:26-30` 以 X/Z 双零判断无效方向（近垂直光波会被误判）、`:35` 仅 yaw、`:79` 半径硬编码 1.5——与计划描述一致。

## 新迭代意见

### P1-1（计划第 260 行）：方块破坏半径公式自相矛盾

正文同时声称"与伤害半径一致"（HALF_WIDTH=2.5）、"约5×5减角"、"~30方块/段"，但给出的公式 `r = ceil(HALF_WIDTH) = 3` + `dx²+dy²+dz² <= r²` 实际是**半径 3 的球**：

- 中心截面（垂直于光波轴）是半径 3 的圆 → **7 格宽**（dy,dz∈[-3,3]），每段 **29 块**，不是 5×5；
- 超出伤害半径 2.5 达 0.5 格，破坏范围与伤害范围不一致（违背用户"与伤害半径一致"要求）。

建议统一为：包围盒 `dx,dy,dz∈[-2,2]`（`r = floor(HALF_WIDTH)`），过滤条件 `dx²+dy²+dz² <= HALF_WIDTH²`（即 6.25）→ 每段约 **21 块**（5×5 减角）。球心截面在任意朝向都是正确圆柱截面，该"球面近似"思路本身成立，只改半径即可。

### P2-1（计划第 260 行）：破坏方块尝试量上升约 7 倍

每段 21~29 块 × 30 段 ≈ **630~870 次** `breakBlock` 尝试/次攻击（原水平十字仅 ~90）。虽有 `brokenThisAttack` 去重且分散在 80t 内（峰值 ~10 块/tick），服务端 breakBlock 会触发方块更新与区块脏标记，建议验证时留意多狗同时 FIRING 的性能；如担心可将每段判定改为仅沿光波轴推进时同步相邻段，或接受现状。

### P2-2（计划第 269 行）：垂直方向的 yaw/pitch 组合建议直接用 `rotationTo`

`yaw = atan2(dirX,dirZ)` + `pitch = -atan2(dirY, hypot(dirX,dirZ))` 在近垂直（hypot→0）时 yaw 不稳定（atan2(0,0) 未定义）。既然已计划用 `Quaternionf`，建议直接 `rotationTo(new Vector3f(0,0,1), dir3d)` 单步完成对齐，绕开两极奇点；原计划的组合写法作为备选即可。

## 其余核验（无问题）

- `FIRING_DIR_Y` 追加在 tracked 末尾保持 ID 顺序、旧存档 NBT 缺省 0、三方向同 tick 写入、客户端三维长度+有限性校验——同步兼容方案自洽。
- 伤害 AABB 三轴取 min/max±HALF_WIDTH、`isInBeam` 三维投影过滤、水平击退保留（Y 由 `damage` 内部处理）——与第二迭代决策衔接一致。
- `cancelAttack()` 补清 `FIRING_PROGRESS=0`（原缺口）、渲染原点 `translate(0, getEyeY()-getY(), 0)` 与服务端 `getEyePos()` 一致、视锥 swept Box 覆盖——均无问题。
- 验证方案 6 项覆盖水平回归、垂直、破坏、渲染、存档——完整。

## 结论

计划整体完整可落地，P1-1 需在进入实现前修订（破坏半径数字），其余为可选优化。修订后可闭环。