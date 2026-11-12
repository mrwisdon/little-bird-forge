# B-2 系统规格（冻结 v1 · Lead）—— 舱内视角 / JDAM+燃烧弹 / 光电吊舱

工程：/root/projects/little-bird-forge（modid battlefield，Forge 1.20.1，Java 17）。构建一律 `flock -w 900 /root/.lb-gradle.lock ./gradlew …`。
**只许新增你的文件 + 对"你的"共享文件做最小插入**；不许 git add/commit/revert；不许碰别人的写作用域。
先例：相机=client/JetCamera.java、client/ApacheCamera.java；舱内=FIRST_PERSON + JetEntity.isCockpitView()；HUD=client/JetHud.java + GunshipHudDraw；网络=network/ModNetwork.java + network/JetFlarePacket.java；纯规则+单测=entity/weapon/*.java + src/test/**。

## 0 冻结接口（谁都不许改名/改签名，要改先找 Lead）
### 0.1 B2Entity（写作用域 = b2-wiring）
```java
public static final int WEAPON_JDAM = 0;
public static final int WEAPON_INCENDIARY = 1;
public static final int WEAPON_COUNT = 2;
public int  getBombWeapon();                 // 0=JDAM 1=燃烧弹
public void setBombWeapon(int weapon);
public int  bombCount(int weapon);           // 剩余
public int  bombCapacity(int weapon);
public boolean bombBayOpen();                // 投弹/装弹期间开
public void applyBombRelease(net.minecraft.server.level.ServerPlayer sender, boolean salvo);
public void setPodDesignation(double x, double y, double z);  // 由 B2PodDesignatePacket 调
public void clearPodDesignation();
public boolean hasPodDesignation();
public double podDesignationX(); public double podDesignationY(); public double podDesignationZ();
public long   podDesignationTick();          // 设计标定时的 gameTime，用于过期
```
### 0.2 弹药纯规则（写作用域 = b2-ordnance）
```java
public enum BombKind { JDAM, INCENDIARY }    // 各自 damage / blastRadius / drag / guided / massKg
public final class BombBay {                 // 纯规则，无 MC 类
    public static final int SALVO_COUNT = 2;          // 单次齐投枚数
    public static final int RELEASE_INTERVAL_TICKS = 6;
    public static boolean canRelease(int remaining, int cooldownTicks);
    public static int nextCooldown(boolean salvo);
    public static int bombsToDrop(boolean salvo, int remaining);
}
public final class BombBallistics {           // 纯规则，无 MC 类
    public static double[] step(double[] pos, double[] vel, BombKind kind, double[] target, boolean guided);
    public static boolean shouldDetonate(double y, boolean onGround);
    public static double dropLeadDistance(double speedBlocksPerTick, double altitude);
}
```
### 0.3 网络（写作用域 = b2-wiring；id 12/13/14 已冻结，不许撞车）
```java
public record B2BombReleasePacket(boolean salvo) {}                       // id 14 PLAY_TO_SERVER
public record B2PodDesignatePacket(double x, double y, double z, boolean clear) {} // id 15 PLAY_TO_SERVER
public record B2WeaponSelectPacket(int weapon) {}                         // id 16 PLAY_TO_SERVER
```
### 0.4 实体注册 id
`jdam_bomb` → entity/JdamBombEntity（写作用域 b2-ordnance，注册行由 b2-wiring 加）
`incendiary_bomb` → entity/IncendiaryBombEntity
### 0.5 按键（写作用域 = b2-cockpit）
- 复用：`LittleBirdKeyMappings.COCKPIT`=舱内视角开关、`SALVO`=投弹、`WEAPON_SWITCH`=JDAM/燃烧弹切换
- 新增一个：`public static final KeyMapping B2_POD = new KeyMapping("key.battlefield.b2_pod", GLFW_KEY_V, "key.categories.battlefield");`
- 客户端类：`client/B2PodView`（吊舱视角开关，静态 `isActive()/setActive(boolean)/gimbalYaw()/gimbalPitch()/zoomFov()`，供 cockpit 的 B2Input 调）
### 0.6 lang 键（写作用域 = b2-wiring，en_us + zh_cn）
`key.battlefield.b2_pod`、`message.battlefield.b2_pod_on|b2_pod_off`、`message.battlefield.b2_weapon_jdam|b2_weapon_incendiary`、
`message.battlefield.b2_no_bombs`、`message.battlefield.b2_designated`、`message.battlefield.b2_bay_open`、
`entity.battlefield.jdam_bomb`、`entity.battlefield.incendiary_bomb`
### 0.7 配置键（写作用域 = b2-ordnance，加在 LittleBirdConfig）
`jdam_damage`(默认 **240**) `jdam_ground_vehicle_damage`(**60**，2026-10-01：JDAM 打地载) `jdam_terminal_lock_radius`(**12.0**，2026-10-01：末端制导锁定半径，0=关闭) `jdam_breaks_blocks`(**true**，2026-10-01：地形破坏开关) `jdam_crater_radius`(**9.0**，上限 32，独立于爆炸杀伤半径；`CraterShape`：垂直压扁 1.6、中心下沉 1 格、抗爆 ≤100 才破、单发上限 20000 格；`BombKind` 新增 `groundVehicleDamage()` 列：JDAM 60 / 燃烧弹 0) `incendiary_damage`(默认 **90**) `jdam_blast_radius`(**10.0**) `incendiary_blast_radius`(**7.0**) —— 2026-09-30 用户选「A 档」加强，旧值 120/45/6.0/4.0
`incendiary_fire_radius`(**12.0**，2026-10-01 新增，独立于爆炸半径；上限 48) `incendiary_fire_ticks`(**200 = 10 秒**，2026-10-01 用户改定；旧 1200 → 2400 → 200) `incendiary_burn_damage_per_second`(**8.0**，2026-10-01 新增：火场每秒伤害脉冲，0=只烧不疼) `bomb_bay_capacity_jdam`(16) `bomb_bay_capacity_incendiary`(24)
`pod_fov_min`(0.16) `pod_fov_max`(1.0) `pod_gimbal_yaw_limit`(110.0) `pod_gimbal_pitch_min`(-120.0) `pod_gimbal_pitch_max`(10.0)
`cockpit_rigid_view`(true) —— 舱内视角"硬邦邦"：相机刚性锁在机体系（不做平滑/惯性）

## 1 舱内视角（b2-cockpit）：硬邦邦
- 眼点：B2Geometry.PILOT_SEAT_FORWARD_UNITS=60 / PILOT_SEAT_HEIGHT_UNITS=32（16 units=1 block）→ 眼点 (x=0, y=seat+0.55 blocks, z=-forward 方向)；FOV 乘 `cockpit_fov_multiplier`
- **刚性**：相机姿态直接取机体姿态（yaw/pitch/roll 与机体一致，零插值/零滞后）；关闭原版第一人称的头部晃动
- **座舱壳**：第一人称时在相机周围绘制座舱结构（风挡框/仪表台/中央杆/座舱盖内壁），并在舱内视角下隐藏机体外壳的遮挡面（或把外壳按"内表面"剔除）
- HUD：俯仰/滚转梯、航向、高度/速度、**弹舱状态 + 当前弹种 + 剩余枚数**、吊舱指向提示
- 单测：`B2CockpitMathTest`（眼点/FOV/刚性姿态换算，纯规则）

## 2 武器（b2-ordnance + b2-wiring）
- B-2 无炮：`WEAPON_CANNON` 在 B2 上不可用（选到炮就回落到 JDAM）
- **JDAM**：重力下坠 + 有限制导（有吊舱标定则朝标定点收敛，无标定则弹道下落）；命中 → 大当量爆炸（jdam_damage/blast_radius）+ 命中反馈
- **燃烧弹**：命中 → 小爆炸 + 生成火场（着火方块/持续燃烧伤害 incendiary_fire_ticks）
- 投弹：舱门开 → 按 RELEASE_INTERVAL_TICKS 间隔齐投/单投 → 舱门关；弹药池独立（AmmoPool 先例）
- 伤害口径走 `DamageType`/`ArmorClass`（沿用现成矩阵：爆炸类不新增装甲类别，除燃烧需要新 DamageType 才加）
- 单测：`BombBayTest`、`BombBallisticsTest`

## 3 光电吊舱视角（b2-pod）
- 相机：位于机头下方吊舱站（x=0, 前 ±?，用 B2Geometry 常量），朝向 = 云台 yaw/pitch；FOV 缩放 `pod_fov_min..pod_fov_max`（鼠标滚轮/按键）
- 云台：yaw ∈ ±pod_gimbal_yaw_limit，pitch ∈ [pod_gimbal_pitch_min, pod_gimbal_pitch_max]，跟随鼠标
- 叠层：单色/微光"光电"观感（整屏淡绿/灰单色 + 轻扫描线/噪点可用简单叠层实现）+ 中央十字 + 云台角度/距离读数 + 锁定框
- 标定：按键把当前准星与地面/实体的交点作为目标点 → `B2PodDesignatePacket(x,y,z,false)`；再按一次清除
- 单测：`B2PodMathTest`（云台限位、准星→射线、与地面求交、标定点）
- 与 b2-wiring 的唯一耦合：`new B2PodDesignatePacket(px,py,pz,clear)` + `ModNetwork.CHANNEL.sendToServer(...)`

## 4 验收（b2-verify）
- 独立复现：纯规则单测（自己另写对抗性用例，不引用实现者结论）、注册 id 不撞车（实体/网络）、lang 键存在、按键注册 1 处、配置键存在
- `./gradlew test` 全量（用 flock，等实现者都停手后跑一次）
- 报告 `docs/plan/b2-systems-verify.md`（≤20 行）+ 任何 FAIL 的原始数字

### 0.3 网络（写作用域 = b2-wiring；**id 14/15/16**（12/13 已被 JetFlarePacket/MainGunImpactPacket 占用，Lead 2026-09-30 修订）
```java
public record B2BombReleasePacket(boolean salvo) {}                       // id 14 PLAY_TO_SERVER
public record B2PodDesignatePacket(double x, double y, double z, boolean clear) {} // id 15 PLAY_TO_SERVER
public record B2WeaponSelectPacket(int weapon) {}                         // id 16 PLAY_TO_SERVER
```
### 0.8 炸弹实体冻结签名（b2-ordnance 实现；b2-wiring 调用）
```java
public JdamBombEntity(EntityType<? extends JdamBombEntity> type, Level level)          // 标准 MC 构造
public IncendiaryBombEntity(EntityType<? extends IncendiaryBombEntity> type, Level level)
public void launchFrom(Entity shooter, double x, double y, double z, double vx, double vy, double vz, boolean guided)
public void setTarget(double x, double y, double z)   // 制导目标（吊舱标定/无标定则不调）
```
### 0.9 配置访问器冻结名（b2-ordnance 实现于 LittleBirdConfig）
```
bombBayCapacityJdam() int · bombBayCapacityIncendiary() int · jdamDamage() double · incendiaryDamage() double
jdamBlastRadius() double · incendiaryBlastRadius() double · incendiaryFireTicks() int
podFovMin() double · podFovMax() double · podGimbalYawLimit() double · podGimbalPitchMin() double · podGimbalPitchMax() double
cockpitRigidView() boolean
```
### 0.10 纯规则返回值布局冻结
`BombBallistics.step(...)` 返回 `{posX,posY,posZ,velX,velY,velZ}`（长度 6 的 double[]）。
### 0.5 补充：B2_POD 按键用 **GLFW_KEY_P**（V 已被 VTOL 占用；C=舱内视角/Z=立刻投弹/R=自动投弹待命/G=切弹照旧）
