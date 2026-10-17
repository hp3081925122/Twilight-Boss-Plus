# 暮色森林 Boss 资料（1.20.1 Forge，暮色 4.3.2508）

## 来源

- 依赖：`curse.maven:the-twilight-forest-227639:5468648`（`twilightforest-1.20.1-4.3.2508-universal.jar`，2024-06-24，1.20.1 最新正式版，无额外硬依赖）。
- 可读源码（Parchment 名）：`D:\AI临时文件\tf_github\twilightforest-1.20.1\src\main\java\twilightforest`，取自 GitHub `TeamTwilight/twilightforest` 的 `1.20.1` 分支。分支头可能比 2508 构建略新，写代码时以开发环境中的反混淆 jar 为准。
- 运行时 jar 的 CFR 反编译（SRG 名）：`D:\AI临时文件\tf_1201_4.3.2508_src`。
- Boss 类：`twilightforest.entity.boss.*`；AI：`twilightforest.entity.ai.goal.*`；Boss 刷怪笼：`twilightforest.block.entity.spawner.*SpawnerBlockEntity`。

## 推荐进度顺序

娜迦 → 巫妖 → 米诺菇 / 九头蛇 → 幻影骑士 → 暮初恶魂 → 雪怪首领 → 冰雪女王 →（高原 Boss，未完成）

## 总览

| Boss | 类 | 生命 | 攻击 | 移速 | 击退抗性 | 经验 | Boss 条 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 娜迦 | `Naga` | 120（普通 +80，困难 +130） | 5 | 0.5 | 0.25 | 217 | 绿色 NOTCHED_10 |
| 巫妖 | `Lich` | 100 | 3 | 0.45 | - | 217 | 黄→紫→红 |
| 米诺菇 | `Minoshroom`（继承 `Minotaur`） | 120 | 继承 | 0.25 | 0.5 | - | - |
| 九头蛇 | `Hydra` | 360 | 无攻击属性（伤害写死） | 0.28 | - | 511 | - |
| 幻影骑士 | `KnightPhantom` ×6 | 35/只 | 1（冲锋时 +7） | - | - | - | - |
| 暮初恶魂 | `UrGhast`（继承 `CarminiteGhastguard`） | 250 | - | - | 1.0 | - | - |
| 雪怪首领 | `AlphaYeti` | 200 | 1 | 0.38 | 0.5 | - | - |
| 冰雪女王 | `SnowQueen` | 200 | 7 | 0.23（飞行 0.23） | 0.75 | - | - |
| 高原 Boss | `PlateauBoss` | 默认 | - | - | - | 647 | 白色，无 AI 占位 |

## 娜迦 Naga

- 多段身体：最多 12 节 `NagaSegment`，节数随血量减少（每 10% 血一节），节数越少移速越高。
- 600 tick 未受伤后每秒回 1 血；免疫爆炸；领地外的攻击者无效；会破坏树叶，冲锋/离家时破坏方块。
- AI：`NagaMovementPattern` 状态机 `CIRCLE → INTIMIDATE → CHARGE / STUNLESS_CHARGE / CRUMBLE → CIRCLE`。
  - 威吓：停下摇尾响声，计算是否进行「无眩晕冲锋」，几率随失血和难度提高（上限 50%）。
  - 冲锋：被盾挡住时把玩家抛飞、盾牌掉 5 耐久、自身受 2 伤害并进入眩晕 `DAZE`（60~100 tick）。
  - 无眩晕冲锋：头顶愤怒粒子、身体变红；盾牌掉 10 耐久并冷却 200 tick。
  - 碎地 `CRUMBLE`：目标在上方时破坏目标脚下方块。
  - 眩晕期间受到超过 15 伤害会强制恢复绕圈。
- 其它目标：`SimplifiedAttackGoal`、`NagaSmashGoal`、回家 `AttemptToGoHomeGoal`。
- 原版特效：眩晕暴击粒子、愤怒村民粒子、碎地暴击粒子。

## 巫妖 Lich

- 三阶段由 `getPhase()` 判定：
  1. 护盾强度 > 0（初始 6）：只能被 `BREAKS_LICH_SHIELDS` 标签且伤害 > 2 的攻击削盾；`LichShadowsGoal` 瞬移、生成最多 2 个影分身，交替发射 `LichBolt`（6 伤害）和 `LichBomb`（爆炸威力 2，不破坏方块）。
  2. 仍有仆从可召唤（初始 9，同时最多 3）：`LichMinionsGoal` 手持僵尸权杖召唤巫妖仆从，近身攻击，远程法弹。
  3. 近战：`MeleeAttackGoal` 拿金剑；受击 1/6 几率瞬移（其它阶段 1/3）。
- 通用：`LichPopMobsGoal` 吸收 `LICH_POPPABLES` 标签生物回血 2；`LichAbsorbMinionsGoal` 吸收自己仆从回复其剩余血量；卡墙自动瞬移。
- 影分身受伤只播音效不掉血。
- 自带 `makeMagicTrail` 魔法轨迹粒子。

## 米诺菇 Minoshroom

- 继承牛头人：`ChargeAttackGoal`（速度 1.5，冲撞）、近战（攻击距离平方被压到 5）。
- `GroundAttackGoal`：蓄力后猛砸地面，命中 15×3×15 范围内站在地上的玩家，伤害 = 攻击力 × 0.5，并轻微上抛；蓄力动画由 `GROUND_CHARGE` 同步。
- 有领地限制目标。

## 九头蛇 Hydra

- 7 个头 `HydraHeadContainer`，初始 3 个激活；单头累计受伤 > 120 即断头，100 tick 后该头与另一个死头重生。
- 身体伤害 ÷8（`ARMOR_MULTIPLIER`），只有张嘴时打头才吃全额伤害。1000 tick 未受伤后每 5 tick 回 1 血。
- 头部状态机：`IDLE → BITE / FLAME / MORTAR → ATTACK_COOLDOWN`。
  - 啃咬（距离 4~10，前三个头）：48 伤害（写死）。
  - 喷火（距离 < 20）：19 伤害 + 点燃（写死），火焰追踪速度 0.04。
  - 迫击炮 `HydraMortar`（距离 8~32，目标不在上方）：直击 18，落地爆炸。
- 注意：伤害常数写死、无攻击力属性，重做时需要给它补攻击力基准，才能满足「技能伤害 = 基础攻击力 × 倍率」。

## 幻影骑士 Knight Phantom

- 刷怪笼一次生成 6 只，围成环形；装备骑士金属剑、幻影胸甲与头盔，三种武器分别为剑 / 斧 / 镐。
- 非冲锋时护甲 ×5（MULTIPLY_TOTAL 4.0），冲锋时攻击 +7。
- 阵型 `Formation`：悬停、大/小顺/逆时针盘旋、四方向冲锋、等待领队、攻击起手/攻击。
- `PhantomThrowWeaponGoal`：斧骑士每 4 tick 投斧；镐骑士向 8 个方向撒镐（伤害 3）。
- 最后一只死亡才算击败。

## 暮初恶魂 Ur-Ghast

- 依赖塔顶的恶魂陷阱 `GHAST_TRAP`，每 100 tick 扫描，悬停在陷阱上方 20 格。
- 普通：喷 `UrGhastFireball`（直击 16，多发散射）。
- 每受到约 18 伤害切换「暴怒」：暴怒时伤害 ÷10、清除目标、哭泣（`BOSS_TEAR` 粒子 + 天气渲染变化）、每 10 tick 执行哭泣伤害效果、视觉闪电、在两个陷阱处召唤砷铅铁恶灵（每处最多 6 只）。
- 恶灵飞进本体会被吸收并回 2 血。

## 雪怪首领 Alpha Yeti

- 未受伤前为远程模式：抛 `IceBomb`，且免疫弹射物。
- 受伤后开启 `YetiRampageGoal`（每 10 次判定，持续 180 tick）：原地连续跳跃，破坏头顶方块、随机落冰块、每秒在目标头顶落块、每秒向四周抛冰弹。
- 狂暴结束进入 `YetiTiredGoal`（100 tick 疲惫，可被弹射物攻击）。
- `ThrowRiderGoal`：抓起玩家骑在身上然后抛出。

## 冰雪女王 Snow Queen

- 7 块环绕冰盾 `SnowQueenIceShield`。
- 阶段循环 `SUMMON → DROP → BEAM → SUMMON`：
  - 召唤：`HoverSummonGoal` 召唤 6 个冰晶 `IceCrystal`，全部死亡后进入下落。
  - 下落：`HoverThenDropGoal` 悬停后砸落，冰盾挤压伤害（`SQUISH`），2~4 次成功下落后进入光束。
  - 光束：`HoverBeamGoal` 冰息光束（`ICE_BEAM` 粒子），期间受到 25 伤害后回到召唤。
- 近战 `MeleeAttackGoal`，攻击 7。

## 高原 Boss Plateau Boss

- 官方未完成：无 AI、默认属性，只有 Boss 条与掉落逻辑，最终城堡由 `FinalBossSpawnerBlockEntity` 生成。可作为本模组完全原创的 Boss 位。

## 可复用资源

- 暮色自定义粒子 `TFParticleType`：`LARGE_FLAME`、`LEAF_RUNE`、`BOSS_TEAR`、`GHAST_TRAP`、`PROTECTION`、`SNOW`、`SNOW_WARNING`、`EXTENDED_SNOW_WARNING`、`SNOW_GUARDIAN`、`ICE_BEAM`、`ANNIHILATE`、`HUGE_SMOKE`、`FIREFLY` 系列、`FALLEN_LEAF`、`OMINOUS_FLAME`、`CLOUD_PUFF` 等。
- 投射物实体：`LICH_BOLT`、`LICH_BOMB`、`HYDRA_MORTAR`、`THROWN_ICE`、`ICE_ARROW`、`ICE_SNOWBALL`、`THROWN_WEP`、`NATURE_BOLT`、`TOME_BOLT`、`WAND_BOLT`、`THROWN_BLOCK`。
- 网络：`TFPacketHandler.CHANNEL`（如 `ThrowPlayerPacket`、`ParticlePacket`）。
- 伤害类型：`TFDamageTypes`；伤害类型标签 `BREAKS_LICH_SHIELDS`；实体标签 `LICH_POPPABLES`。

## 重做时的技术切入点

- Boss 类均非 final，`registerGoals` 为 protected：可在 `EntityJoinLevelEvent` 中清空并替换目标，或用 Mixin 改 `hurt`、`customServerAiStep` 等阶段逻辑。
- 阶段判定建议统一用 `getHealth() / getMaxHealth()` 百分比（半血、25% 等）。
- 每个 Boss 需要独立配置（开关、属性倍率、技能倍率、冷却）。
- 特效方向：每个 Boss 用独立的自定义渲染（特效实体 + 自定义模型/贴图/着色），少用原版粒子堆叠，暮色自带粒子可少量点缀。
