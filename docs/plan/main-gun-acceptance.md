# 120mm 主炮：AP/HE 弹种 + 大口径表现 —— 验收报告

目标 goal：`goal-330b6ac0-e5fc-43ae-9516-7f804fea6de4`

## 验收标准逐条对照

### 1. AP/HE 可明确区分，参数 40/25，Q3 参数确实变大 —— **通过**
- 弹种：`MainGunAmmoType` = `AP("ap")` / `HE("he")`；HUD 读数显示 **穿甲弹 / 高爆弹**（实机截图 `item8_shell_ap_he_hud.png` 显示 `120mm HE`）；B 键切换，两弹独立弹仓。
- 数值（配置）：AP `main_gun_vehicle_damage=40.0` / `main_gun_splash_radius=3.0`；HE `main_gun_he_vehicle_damage=25.0` / **`main_gun_he_splash_radius=4.0`**（Q3 增大，取我上轮提议区间 3.5–4.0 的上界）。
- 实机核对（`item8_shell_damage.txt`）：对装甲靶 IFV（90 血）**AP 90→50（Δ40）**、**HE 90→65（Δ25）**。

### 2. 开火时炮口飞出明显大口径火球，明显区别于原先子弹样式 —— **通过**
- 主炮由 hitscan 改为 **`MainGunShellEntity` 弹丸实体**（初速 10 格/t、重力 0.02、寿命 60t）；主炮**不再发送曳光包**（原"子弹 streak"消失）；同轴与其它武器仍是 hitscan，未受影响。
- 炮口：`EXPLOSION_EMITTER` 白核 + 18×FLAME + 10×LARGE_SMOKE；飞行中每 tick FLAME+SMOKE 火球（弹丸本身不画模型，火球即视觉）。
- 证据：`item9_shell_impact.png`（开火帧：炮口白闪 + 火焰）。

### 3. 命中处方块破坏和烟尘 —— **通过**
- `ExplosionInteraction.TNT`（配置 `main_gun_breaks_blocks`，默认 true）+ 弹着 `EXPLOSION_EMITTER`/EXPLOSION/24×LARGE_SMOKE 烟尘。
- 实机（`item9_shell_impact.txt`）：石墙 `BEFORE_STONE` → 开火 → **`AFTER_WALL_GONE`（方块被摧毁）**。

### 4. 音效完整，风格对齐战地 2042 —— **实现完成，但本机无法试听（如实标注）**
- 链路三段：**开火** = `gun_shot`(1.5/0.62 AP、1.5/0.50 HE) 叠加 `explosion`(1.2/低音) 做炮口轰鸣；**飞行** = `rocket_launch` 升调(1.8) 每 3t 一次破空；**命中** = `explosion`(1.6/0.7) 深炸。
- 限制：本 headless 机打不开 OpenAL（`Error starting SoundSystem`），**无法试听**；且本机**无 ogg 编码器**（ffmpeg/sox/oggenc/opusenc 全缺）→ **不新增音频文件**，全部复用工程自带 5 个 ogg（engine/gun/gun_shot/rocket_launch/explosion，均已注册且文件存在）。

### 5. 全程无震屏 —— **通过**
- 全库检索 `shake/cameraAngle/ComputeCameraAngles/ViewportEvent/tilt`：唯一命中是 `LittleBirdCamera`/`ApacheCamera` 的 `computeCameraAngles`，二者都**以 `player.getVehicle() instanceof LittleBirdEntity|ApacheEntity` 为前置条件**（地面载具根本不进入），且只做**滚转(bank)**，不是震动。地面载具路径**无任何震屏**，新增弹丸代码也没有。

## 改动文件清单
新增：`entity/MainGunShellEntity.java`、`entity/weapon/MainGunShellBallistics.java`（+单测）、`client/MainGunShellRenderer.java`
改动：`entity/weapon/MainGunAmmoType.java`(AP/HE)、`entity/weapon/MainGunAmmo.java`、`entity/MainBattleTankEntity.java`(发射弹丸+音效)、`LittleBirdConfig.java`(HE 三键 + `main_gun_breaks_blocks` + `main_gun_particle_scale` + 按弹种访问器)、`registry/ModEntities.java`(注册 main_gun_shell)、`client/ClientModEvents.java`(渲染器注册)、`lang/{en_us,zh_cn}.json`、`WeaponSelectionTest`/`MainGunAmmo*Test`(随改名更新)
回退点：`docs/plan/backups/2026-09-12_pre-coax-driver`（更早状态）；本阶段改动可用"把 `fireMainGun` 换回 `GroundWeapons.fireHitscan(...)` + 删除三个新文件 + 移除注册"回退（步骤见下）。

## 复现步骤
1. `./gradlew runClient` → 创造模式 → `/summon battlefield:main_battle_tank 6 119 12` → 右键上车。
2. 弹种/数值：`/summon battlefield:infantry_fighting_vehicle 6 119 20` → 开火 → `/data get entity @e[type=battlefield:infantry_fighting_vehicle,limit=1] Health`（AP→50）→ 重召 → 按 **B** → 开火（HE→65）。
3. 方块破坏：`/fill 3 119 18 9 122 18 minecraft:stone` → 开火 → `/execute unless block 6 120 18 minecraft:stone run say GONE`。
4. 震屏：全库 grep（见上）无命中。

## 未能确认 / 未验证
1. **音效无法试听**（headless 无 OpenAL）；无编码器故未新增音频。
2. **飞行中火球的单帧截图未取到**（第一人称被车体遮挡；第三人称机位落在车内），以开火帧+命中帧为可视证据。
3. 本阶段修掉的缺陷：新实体未注册渲染器导致客户端 NPE 崩溃（`crash-2026-09-12_19.06.40-client.txt`），已加空渲染器并复测通过。
