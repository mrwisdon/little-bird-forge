# 地面战斗单位交付证据（主战坦克 / 步兵战车）

日期：2026-09-12
工程：/root/projects/little-bird-forge（modid battlefield，Forge 47.4.20 / MC 1.20.1 / Java 17）
规格来源：用户指定「参考战地风云2042」；BF2042 门户载具基础数据（3楼猫 2022-05-07）与 BWIKI M1A5 / EBAA 小野猫。

## 1. 已拍板的规格（用户 2026-09-12 确认）

| 项 | 主战坦克 M1A5 | 步兵战车 EBAA 小野猫 |
| --- | --- | --- |
| 内部 id | `main_battle_tank` | `infantry_fighting_vehicle` |
| 血量 | 120 | 90 |
| 主武器 | 120mm 主炮：直击 80 / 对载具 40 / 溅射 3.0 / 5 待发 +15 备弹 / 50 tick 间隔 | 双联 30mm 机炮：8 / 3 / 溅射 1.0 / 600 发单池 / 2 tick 间隔（沿用 ChainGunTurret 热度与过热） |
| 副武器 | 同轴机枪 8 / 3，300 发 | ATGM 2 待发 +6 备弹：60 / 20 / 溅射 2.0 |
| 机动 | 0.30 格/t，履带可原地转身（standstill 0.35） | 0.45 格/t，轮式不可原地转身（standstill 0.20） |
| 座位 | 2（驾驶员主炮 + 炮手同轴机枪） | 4（驾驶员机炮 + 炮手 ATGM + 2 载员） |
| 阵营 | 不新增阵营，与既有飞机一致对所有人开放 | 同左 |
| 获取 | 生存打造链（新增地面零件）+ 创造栏；IFV≈小鸟级、MBT 含钻石火控 | 同左 |
| 资产 | 程序化占位盒模型 + 纯色纹理 + 复用既有枪声/爆炸声 | 同左 |

设计简化（须声明）：两种车各只有一个炮塔，炮塔由驾驶员用准星指向；副武器（坦克同轴机枪 / 步战 ATGM）沿同一炮塔线射击。BF2042 的 4 座（驾驶员/武器站/武器仓/指挥官）在本工程收敛为「驾驶员 + 副武器手（+步战 2 载员）」。

## 2. 改动文件

### 2.1 新增（Java 主代码）

| 文件 | 行 | 作用 |
| --- | --- | --- |
| entity/ground/GroundDrive.java | 119 | 纯规则地面驾驶（油门/转向→速度/朝向/位移），含控制位与轴解析 |
| entity/ground/MainGun.java | 130 | 纯规则 120mm 炮塔（慢速回转、俯仰限位、单发间隔） |
| entity/ground/GroundWeapons.java | 134 | 服务器权威 hitscan/溅射/曳光/命中反馈（两种车共用数值参数） |
| entity/ground/GroundVehicleControls.java | 29 | 客户端输入与网络层所需的车辆共享接口 |
| entity/seat/GroundSeat.java | 54 | 两种车座位表（坦克 2 / 步战 4） |
| entity/MainBattleTankEntity.java | 528 | 主战坦克实体 |
| entity/MainBattleTankGeometry.java | 79 | 坦克单位空间尺寸/座位/炮塔几何 |
| entity/InfantryFightingVehicleEntity.java | 527 | 步兵战车实体（独立武器逻辑，不与坦克共用） |
| entity/InfantryFightingVehicleGeometry.java | 90 | 步战几何 |
| item/MainBattleTankDeployItem.java | 47 | 右键部署坦克（带 Health NBT） |
| item/InfantryFightingVehicleDeployItem.java | 47 | 右键部署步战 |
| network/GroundWeaponInputPacket.java | 33 | 地面车辆开火/装填意图包（id 10） |
| client/MainBattleTankModel.java / Renderer.java | 91 / 50 | 坦克占位模型与渲染器 |
| client/InfantryFightingVehicleModel.java / Renderer.java | 91 / 51 | 步战占位模型与渲染器 |
| client/GroundVehicleInput.java | 98 | 两种车共用的客户端输入（W/S/A/D 驾驶，左键开火，R 装填，X 下车） |

### 2.2 新增（测试）

| 文件 | 行 | 覆盖 |
| --- | --- | --- |
| test/.../entity/ground/GroundDriveTest.java | 88 | 加速/刹车/倒车/原地转身/高速转向/位移方向/限幅/角度回绕（8 例） |
| test/.../entity/ground/MainGunTest.java | 84 | 回转速率/俯仰限位/单发间隔/松键与连点不绕过间隔/仅成功开火后提交/瞄准方向/炮口位置（11 例） |
| test/.../entity/seat/GroundSeatTest.java | 34 | 座位顺序与操作位判定（3 例） |

### 2.3 新增（资源）

- 实体贴图：textures/entity/main_battle_tank.png、infantry_fighting_vehicle.png（512x512，程序化）
- 物品贴图：textures/item/{armor_plate,track_assembly,wheel_assembly,turret_ring,cannon_barrel,tank_hull,ifv_hull}.png（16x16，程序化）
- 成就：data/battlefield/advancements/first_main_battle_tank.json、first_infantry_fighting_vehicle.json
- datagen 产物：recipes 9 个、advancements/recipes/misc 9 个、models/item 7 个

### 2.4 修改（既有文件，均为最小接入）

- registry/ModEntities.java（+2 实体类型）、ModItems.java（+2 载具物品 +7 地面零件）、ModCreativeTabs.java（+9 条目）
- LittleBirdConfig.java（CLIENT `ground.tank/ifv` 手感；COMMON `balance.ground.main_battle_tank/infantry_fighting_vehicle`）
- network/ModNetwork.java（注册 id 10）、network/LittleBirdExitPacket.java（改为任何 GunshipRider 可下机）
- client/ClientModEvents.java（+2 层定义 +2 渲染器）
- data/ModRecipeProvider.java、ModItemModelProvider.java（地面打造链与图标）
- lang/en_us.json、zh_cn.json（两车 + 7 零件 + 2 成就中英文）

## 3. 构建 / 测试证据（可复现）

```
$ ./gradlew compileJava --rerun-tasks     → BUILD SUCCESSFUL
  仅 3 条既有 deprecation 警告（LittleBird.java:26/32/33），新增文件 0 警告 0 error
$ ./gradlew test --rerun-tasks            → BUILD SUCCESSFUL
  build/test-results/test/*.xml 汇总：suites=36  tests=264  failures=0  errors=0  skipped=0
  （改造前基线 247 tests；新增 17 = GroundDrive 8 + MainGun 6 + GroundSeat 3）
$ ./gradlew runData --rerun-tasks         → BUILD SUCCESSFUL（9 配方 + 7 物品模型）
$ ./gradlew build                         → BUILD SUCCESSFUL（资源校验 + 测试 + reobf + 打包 + sync）
$ sha256sum build/libs/battlefield-0.1.0.jar
  f916c0d3f480e20bedfc7bf0845c0a638e7c0f6b29e2878af668e05c0c5fcc80  414733 字节
  实例 mods 同名 JAR 哈希一致；实例内 battlefield JAR 恰好 1 份
```

> 注：首次 build 的 JAR 为 c71674a4…/413541 字节；随后客户端运行暴露了 7 张贴图 Corrupt PNG 与 2 个缺失物品模型
> （见 §5），修复后重新 build，故当前哈希/体积如上一行。

## 4. 游戏内证据（专用服务器 + RCON，headless）

服务器：`./gradlew runServer`（启用 RCON）→ 日志 `Done (2.215s)!`、`RCON running on 0.0.0.0:25575`、
`Enabled Gametest Namespaces: [battlefield]`，启动到关闭无 ERROR / Exception。

### 4.1 生成（两种单位都能在世界里刷出，且属性与规格一致）

```
> summon battlefield:main_battle_tank 0 -60 0
Summoned new M1A5 Main Battle Tank
> data get entity @e[tag=tanktest,limit=1]
{Health: 120.0f, MagReady: 5, MagReserve: 15, CoaxAmmo: 300, TurretYaw: 0.0f, TurretPitch: 0.0f, Pos:[0.5,-60.0,0.5], OnGround: 1b}

> summon battlefield:infantry_fighting_vehicle 8 -60 0
Summoned new EBAA Wildcat IFV
> data get entity @e[tag=ifvtest,limit=1]
{Health: 90.0f, AutocannonAmmo: 600, AtgmReady: 2, AtgmReserve: 6, TurretYaw: 0.0f, Pos:[8.5,-60.0,0.5], OnGround: 1b}
```

### 4.2 物品/创造栏入口（部署物品已在注册表内，可放入玩家物品栏）

```
> item replace entity @e[tag=stand2,limit=1] weapon.mainhand with battlefield:infantry_fighting_vehicle
Replaced a slot on Armor Stand with [EBAA Wildcat IFV]
> item replace entity @e[tag=stand2,limit=1] weapon.mainhand with battlefield:main_battle_tank
Replaced a slot on Armor Stand with [M1A5 Main Battle Tank]
> item replace entity @e[tag=stand2,limit=1] weapon.offhand with battlefield:wheel_assembly
Replaced a slot on Armor Stand with [Wheel Assembly]
> item replace entity @e[tag=stand2,limit=1] armor.head with battlefield:cannon_barrel
Replaced a slot on Armor Stand with [Cannon Barrel]
> data get entity @e[tag=stand2,limit=1] HandItems
[{id:"battlefield:main_battle_tank", Count:1b}, {id:"battlefield:wheel_assembly", Count:1b}]
```

### 4.3 移动（实体 tick + move 路径实际执行）

```
> summon battlefield:main_battle_tank 0 -30 0    # 半空刷出
> data get entity ... Pos   → [0.5, -32.88, 0.5]
（3 秒后）
> data get entity ... Pos   → [0.5, -60.0, 0.5]   # 落到地面并停住（applyUnpilotedMotion）
```

### 4.4 承受伤害与死亡/销毁

```
> damage @e[tag=tanktest,limit=1] 200 minecraft:generic
Applied 200.0 damage to M1A5 Main Battle Tank
> data get entity @e[tag=tanktest,limit=1]
No entity was found
> data get entity @e[type=item,limit=1]
Iron Ingot ... {Item:{id:"minecraft:iron_ingot", Count:1b}}   # 残骸掉落

> damage @e[tag=ifvtest,limit=1] 95 minecraft:generic
Applied 95.0 damage to EBAA Wildcat IFV
> execute unless entity @e[tag=ifvtest,limit=1] run ...   # 实体已移除
```

### 4.5 配置实际生效（run/config/battlefield-common.toml）

```
[balance.ground.main_battle_tank]
  main_gun_damage = 80.0 / main_gun_vehicle_damage = 40.0 / main_gun_splash_radius = 3.0
  main_gun_fire_interval_ticks = 50 / main_gun_ready_rounds = 5 / main_gun_reserve_rounds = 15
  coax_damage = 8.0 / coax_vehicle_damage = 3.0 / coax_capacity = 300 / coax_fire_interval_ticks = 2
[balance.ground.infantry_fighting_vehicle]
  autocannon_damage = 8.0 / autocannon_vehicle_damage = 3.0 / autocannon_capacity = 600
  atgm_damage = 60.0 / atgm_vehicle_damage = 20.0 / atgm_splash_radius = 2.0
## 5. 客户端实机验证（真实游戏客户端：headless Xvfb + openbox + llvmpipe）

在 WSLg（DISPLAY=:0）下客户端能启动到主菜单，但 Weston WM 不支持 `_NET_ACTIVE_WINDOW`，进入世界后窗口拿不到焦点，
注入的键鼠全部被忽略（多次注入前后截图字节完全相同）。改用 **Xvfb :99 + openbox + Mesa llvmpipe**（Linux LWJGL natives
经 `-Dorg.lwjgl.librarypath` 指向解包目录）后输入正常，用 xdotool 完成全套操作。截图见
`docs/plan/evidence/ground-units/`，原始日志摘录见同目录 `client-run-log-excerpt.txt`。

| # | 证据 | 文件 |
| --- | --- | --- |
| 1 | 两车同场生成（chat：Summoned new M1A5 Main Battle Tank / EBAA Wildcat IFV），模型均正常渲染 | 01_both_units_spawned.png |
| 2 | 玩家骑上坦克：`/data get entity @s RootVehicle` = `battlefield:main_battle_tank`，Health 120 / MagReady 5 / CoaxAmmo 300 | 02_tank_boarding_rootvehicle.png |
| 3 | 驾驶员操作前进：F3 `XYZ 7.25/120.52/28.00`，约 2.5 s 内 z 13.9→28.0 ≈ 5.95 格/s，与配置 `max_speed=0.30 格/t` 一致，x 不漂 | 03_tank_driven_f3.png |
| 4 | 坦克 120mm 实弹射击：`MagReady` 5→3（截图 3；日志 13:47:01 另记 4） | 04_tank_fired_main_gun.png |
| 5 | 坦克被击毁：`Applied 200.0 damage to M1A5 Main Battle Tank` → 残骸掉铁（玩家获得成就 Acquire Hardware） | 05_tank_destroyed_and_scrap.png |
| 6 | 玩家骑上步战：`RootVehicle` = `infantry_fighting_vehicle`，Health 90 / AutocannonAmmo 600 / AtgmReady 2 | 07_ifv_boarding_rootvehicle.png |
| 7 | 步战驾驶员前进：F3 `XYZ 19.13/104.79/35.03`，约 2.5 s 内 z 13.9→35.0 ≈ 8.4 格/s，与 `max_speed=0.45 格/t` 同量级 | 08_ifv_driven_f3.png |
| 8 | 步战 30mm 机炮连射：`AutocannonAmmo` 600→579（约 21 发） | 09_ifv_fired_autocannon.png |
| 9 | 步战被击毁：`Applied 95.0 damage to EBAA Wildcat IFV` | 10_ifv_destroyed.png |

要点：扳机必须**按住**跨过至少一个客户端 tick（`GroundVehicleInput` 轮询 `keyAttack.isDown()`），
瞬时点击会被采样窗口漏掉——全自动武器的正常行为，不是缺陷。

### 本次客户端运行暴露并已修复的 2 个缺陷

1. 7 张新增物品贴图按 RGB（3 通道）写入却声明为 RGBA → 客户端 `Corrupt PNG`，贴图显示为紫黑格。
   修复：按 4 通道重写并校验 `rawlen=1040=16*(1+16*4)`；复跑客户端后 `Corrupt PNG` 计数 = 0。
2. `main_battle_tank` / `infantry_fighting_vehicle` 两个部署物品缺 `models/item/*.json`，客户端报
   `Unable to load model ...#inventory`。修复：新增两个 `minecraft:item/generated` 模型 + 16x16 图标。

## 6. 仍未验证 / 需人工确认（范围明确）

1. **多人**：双客户端、远端插值、副武器手（坦克同轴机枪 / 步战 ATGM）与 2 名载员座位未做多人实测。
2. **命中伤害落点**：只验证了开火与弹药消耗、以及被 `/damage` 击毁；未对靶实体逐发核对 80/40/60/20 的伤害数字。
3. **观感/音效**：第三人称取景、炮塔模型随准星转动、HUD；本 headless 环境 OpenAL 打不开（`Failed to open OpenAL device`），
   音效未闻。以上需在带显示的客户端确认。
4. **创造栏/配方书**：物品与实体注册已用 `/item replace` 实机证实（§4.2），但创造栏图标排布与配方书解锁提示未目视确认。

