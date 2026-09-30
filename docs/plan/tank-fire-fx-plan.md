# 坦克开火特效强化（2026-09-29）

用户诉求：坦克开火时 **炮弹飞行要"明显、硬邦邦"**，**落点爆炸要"硬"**，**炮镜要"硬邦邦"**。
本文件是本次改造的冻结规格：诊断、接口、文件归属、验收。实施由 3 个并行切片 + 根代理集成完成。

## 0. 诊断（只读证据，改造前）

| 现象 | 证据 | 判断 |
| --- | --- | --- |
| 炮弹飞行"软、看不清" | \`MainGunShellEntity.spawnFireball()\`（\`entity/MainGunShellEntity.java:161-170\`）每 tick 在弹体位置喷 FLAME+SMOKE，跟着弹走 = 一团糊状火球；渲染器 \`MainGunShellRenderer.render\`（:40-58）用世界光照 \`packedLight\`，夜里/阴影里弹体直接黑掉 | 需要"刚性曳光 + 恒亮弹体"，去掉跟随火球 |
| 落点爆炸"软" | \`detonate()\`（:186-221）只有 EXPLOSION_EMITTER + 少量 EXPLOSION/LARGE_SMOKE + 原版 explode()，没有方向性碎片/冲击环/镜头反馈 | 需要硬冲击包：定向碎片 + 冲击环 + 尘土柱 + 镜头冲击 |
| 炮镜"不硬" | \`GroundCamera.computeFov\`（:28-48）内部视角用 \`cockpit_fov_multiplier\` 默认 1.00 = 完全不放大；\`GroundHud.drawTurretInterior\`（:333-362）只画一个十字 + 两行文字，没有镜筒/遮光罩/分划梯 | 需要真正的光学瞄准镜：放大倍率 + 镜筒遮罩 + 分划梯 |

## 1. 冻结接口（跨切片，签名不得改）

### 1.1 根代理已实现（\`LittleBirdConfig.java\`，其他切片只读）
\`\`\`java
public static double groundSightFovMultiplier();   // client "effects" -> ground_sight_fov_multiplier，默认 0.55
public static double mainGunTracerLengthBlocks();  // client "effects" -> main_gun_tracer_length_blocks，默认 6.0
public static double mainGunImpactShake();         // client "effects" -> main_gun_impact_shake，默认 0.55
\`\`\`

### 1.2 切片 B 提供，切片 C / 根代理消费
\`\`\`java
// io.github.littlebird.client.ImpactShake —— 客户端镜头冲击（纯规则状态机 + 静态入口）
public static void add(float intensity);              // 收到一次冲击（已按距离衰减后的强度）
public static void tick();                            // 每客户端 tick 衰减一次
public static float yawOffsetDeg(float partialTick);   // 本帧叠加到相机 yaw 的偏移
public static float pitchOffsetDeg(float partialTick); // 本帧叠加到相机 pitch 的偏移
public static void reset();

// io.github.littlebird.entity.ground.MainGunImpactFx —— 服务端冲击包（由根代理在弹体命中处调用）
public static void detonate(net.minecraft.server.level.ServerLevel server,
                            net.minecraft.world.entity.Entity shell,
                            net.minecraft.world.phys.Vec3 point,
                            net.minecraft.world.phys.Vec3 normal,   // 可空：无表面法线时传 null
                            double particleScale, double splashRadius, boolean breakBlocks);
\`\`\`
\`detonate\` 负责：命中特效粒子（定向碎片/尘土/冲击环）+ 爆炸 + 声音 + 向附近客户端广播 \`MainGunImpactPacket\`；
**不负责**伤害结算（仍留在 \`MainGunShellEntity.detonate\`）。

### 1.3 切片 B 新增网络包
\`\`\`java
// io.github.littlebird.network.MainGunImpactPacket(float x, float y, float z, float intensity)
//   PLAY_TO_CLIENT，ModNetwork id = 13（唯一一处 ModNetwork 改动）
//   handle() 在客户端调用 ImpactShake.add(intensity)（强度由服务端按距离衰减后发送）
\`\`\`

## 2. 文件归属（写作用域，互不重叠）

| 切片 | 所有者 | 文件 |
| --- | --- | --- |
| A 弹道曳光 | shell-fx | \`client/MainGunShellRenderer.java\`、\`client/MainGunTracerTrail.java\`(新)、\`test/client/MainGunTracerTrailTest.java\`(新) |
| B 落点冲击+镜头 | impact-fx | \`entity/ground/MainGunImpactFx.java\`(新)、\`network/MainGunImpactPacket.java\`(新)、\`network/ModNetwork.java\`(1 行)、\`client/ImpactShake.java\`(新)、\`client/ImpactClientEvents.java\`(新)、\`test/client/ImpactShakeTest.java\`(新) |
| C 炮镜 | gun-sight | \`client/GunSightMath.java\`(新)、\`client/GunSightDraw.java\`(新)、\`client/GroundHud.java\`、\`client/GroundCamera.java\`、\`test/client/GunSightMathTest.java\`(新) |
| 集成 | lead | \`entity/MainGunShellEntity.java\`（去掉跟随火球、改为刚性曳光余烟、命中处调用 \`MainGunImpactFx.detonate\`）、\`LittleBirdConfig.java\`、文档 |

**禁止**：任何切片不得修改上表之外的文件；不得改 \`MainGunShellEntity.java\`、\`LittleBirdConfig.java\`；不得新增语言键（\`lang/*.json\` 由 \`LangFileTest\` 守卫）；不得新增贴图/音频资源。

## 3. 验收

- 编译：每个切片各自 \`./gradlew compileJava\` 通过。
- 单测：\`./gradlew test --tests "*MainGunTracerTrail*" --tests "*ImpactShake*" --tests "*GunSightMath*"\` 通过；根代理最后跑全量 \`./gradlew build\` 并同步 JAR。
- 视觉：游戏内实测（离线环境无法替代），清单见第 4 节。

## 4. 游戏内待测清单（交付后由用户执行）

1. 炮镜（默认第三人称 → 切内部视角键）放大明显、镜筒遮罩居中、分划梯随距离可读、开炮瞬间不抖散。
2. 开炮瞬间：炮口大闪光 + 硬脆炮声 + 弹体拖着一条笔直亮曳光飞出去（夜里也亮）。
3. 命中地面：坑 + 定向碎片 + 尘柱 + 冲击环 + 近距离镜头一记短促硬震。
4. 命中载具：先命中反馈（击杀/血条），再有爆炸；不应出现"没打中就炸"。
5. 连续两发之间：装填环与曳光节奏不重叠；远距离（>100 格）曳光仍可辨。

