# 项目全景（Little Bird Forge / modid `battlefield`）

> 「完全理解项目」的产物：设计文档 + 三路源码深读交叉核对后写成。
> 上游：https://github.com/mrwisdon/little-bird-forge ，本机 HEAD `76b7187`（单 commit 快照，历史全在 docs/plan）。
> 文中 `文件:行号` 均相对 `src/main/java/io/github/littlebird/`，除非另注。

## 1. 它是什么

Minecraft **1.20.1 / Forge 47.4.20** 的「战地风云式载具包」：可驾驶直升机、固定翼、轰炸机、主战坦克、
步战车、自行火炮；服务器权威的武器、弹药、装甲伤害、火控与 HUD。模型/贴图/音效全部程序化生成，
无第三方资产（版权红线见 `docs/plan/tech-design.md` §8）。

名字来自最早的小鸟直升机，实际已覆盖 **10 台载具 + 11 类弹药/效果**。

## 2. 内容清单（以注册代码为准）

| 类别 | 实体 id | 座位 | 主要武器 |
| --- | --- | --- | --- |
| 直升机 | `little_bird` | 4（仅 0 号可操控） | 7.62 双联机枪、70mm 火箭巢 7 发 |
| 直升机 | `apache` | 2（后座飞行员 / 前座炮手） | 30mm 链炮、19 管火箭、TOW |
| 固定翼 | `f35` / `su57` | 1 | 机炮、Hydra、IR/AGM/雷达弹、热诱饵 |
| 轰炸机 | `b2` | 1（座舱/吊舱两套视角） | JDAM、燃烧弹、弹舱门 |
| 地面 | `main_battle_tank` | 2（驾驶 + 炮手） | 120mm 主炮（AP/HE/霰弹）、并列/炮长机枪、烟幕 |
| 地面 | `infantry_fighting_vehicle` | 4（驾驶 + 炮手 + 2 载员） | 30mm 机炮、ATGM |
| 地面 | `bradley` | 6（驾驶兼炮塔 + 顶置机枪 + 4 载员） | 25mm 机炮、M2、TOW |
| 地面 | `phl191` | 1（驾驶兼放列） | 370mm 齐射火箭 |
| 地面 | `plz05` | 1（驾驶兼操炮） | 155mm 榴弹 + `J` 呼叫 30 发火力任务 |
| 弹药/效果 | `little_bird_rocket`、`apache_missile`、`lock_on_missile`、`main_gun_shell`、`autocannon_shell`、`phl191_rocket`、`plz05_shell`、`jdam_bomb`、`incendiary_bomb`、`smoke_grenade`、`smoke_cloud` | — | — |

规模：**21 个实体、36 个物品、2 个方块**（`ammo_crate`、`vehicle_factory`）、1 个方块实体、1 个粒子、
12 个音效、1 个创造标签；**98 个测试类 / 726 个 `@Test`**；98 个 Java 包目录 268 个源文件。

## 3. 架构主线（先记住这四条）

1. **运动学客户端权威**：驾驶员客户端每 tick 解算纯函数飞行/行驶模型 → `move()`；位置与朝向走 vanilla
   `ServerboundMoveVehiclePacket`，协议外的量（滚转角、炮塔角、弹舱状态）走自定义包。
2. **武器/伤害/弹药服务端权威**：客户端只发「意图」（扳机 held、换弹、换武器、切座），服务器按真实位姿
   计算弹道、结算伤害与寿命；每个 C→S 包都复验「发件人 == controllingPassenger/对应座位」并夹值。
3. **纯规则优先**：飞行数学、弹道、弹仓、伤害表、HUD 布局、组装表都是不 import `net.minecraft` 的静态类；
   98 个测试里只有 4 个用 `Vec3/AABB/Shapes`，且不启动游戏。
4. **单一数据源**：武器伤害收敛到 `entity/weapon/VehicleDamageTable`（19 行 × flesh/aircraft/apache/heavy 四列），
   数值镜像在 `LittleBirdConfig`（225 个 `define`）；改平衡只改这两处。

## 4. 空中载具

### 4.1 飞行模型

- **直升机**（`entity/flight/HelicopterFlightModel.java`，277 行纯函数）：4 轴——yaw 角追踪、pitch 角回中、
  roll **角速率积分**、collective 升降速。推力沿机头轴按 `cos(pitch)` 投影，叠加侧滑加速度，再乘阻力与限速；
  滚转无 bank 上限；协调转弯 `g·tan(bank)/v` 在客户端算完写回玩家 yaw（`LittleBirdEntity:213-220`）。
- **喷气**（`JetFlightModel.java`，272 行）：3 轴 + 油门积分 + 加力；失速强制低头、升力 ∝ 水平速度²、
  转弯率 = cap × 低速权 × 高速衰减 × pull 权 × sin(roll)，速度矢量随 yaw 转。
- **手感层**：`HelicopterInputDamping`（一阶阻尼，滚转松键瞬断）、`HelicopterRollDamper`（滚转起动 slew）。
- 机型差异收在 `FlightConfig` + `F35Profile`/`Su57Profile`/`B2Profile`；加新机型优先换 profile。

### 4.2 座位与输入

- 小鸟 4 座，只有 seat0 操控；其余座位输入在 `client/LittleBirdInput:84-92` 被清零，服务端每个入口再校验一次。
- 阿帕奇 2 座：后座飞 + 火箭/导弹，前座链炮；`H` 只翻转 `DATA_SEATS_SWAPPED`（数据/视角），不动真实乘客表。
- 喷气/B2 单座；乘员受伤由 `LittleBirdRiderProtection` 直接 cancel（`LivingAttackEvent`）。
- 键位：小鸟 `1/2/Z/左键/C/X/R`；阿帕奇 `1/2/R/H` + 前座鼠标瞄准；喷气 `1-5/W·S/A·D/鼠标/V/F/冲刺`；B2 `G/Z/Shift/R/P`。

### 4.3 渲染与相机

- 相机滚转统一在 `ComputeCameraAngles` 叠加 `renderRoll × factor`；渲染顺序固定 `scale → Y → X → Z`。
- 模型是程序化 `ModelPart` 盒体；B2 走 OBJ 渲染器。分层：本体 → 炮管 emissive → 玻璃 → 旋翼模糊盘。
- 只有喷气做滚转帧插值。

### 4.4 同步

- `SynchedEntityData`：小鸟 13 字段、阿帕奇 17 字段、B2 追加 5 个弹舱字段。
- 节流：滚转 0.5°/次，喷气轴额外 0.005；回写全部 clamp ±180。

## 5. 地面载具与火力

### 5.1 数值（默认值，均在 `LittleBirdConfig` / 纯规则常量）

| 载具 | 主武器 | 伤害（肉/机/重） | 弹药 | 血 |
| --- | --- | --- | --- | --- |
| MBT | 120mm AP / HE / 霰弹 | 80/40/40 · 80/25/30 · 80 粒×40 | 5 待发 + 15 备，60t 装填 | 120 |
| IFV | 30mm / ATGM | 8/3/3 · 60/20/45 | 600 发 / 2+6 | 90 |
| Bradley | 25mm / TOW / M2 | 6/4/3 · 90/40/45 · 6/1.5/0 | 300+600 / 2+5 / 400 | 110 |
| PHL-191 | 370mm 齐射 | 14/40/40（溅 7，坑 5） | 8 管，400t 装填，射程 140 | 160 |
| PLZ-05 | 155mm | 22/60/60（溅 10，坑 8） | 1 发/10t，300t 装填，射程 200 | 160 |

坦克主炮射界 −30°…+10°（`MainGun:20-21`），炮塔同 tick 跟随驾驶员视线；PHL/PLZ 无炮塔，方位取车体 yaw，
仰角由 `Z/X` 包控制。HUD 侧只复算落点标记，发射与命中都在服务端。

### 5.2 伤害结算

- `ArmorClass` 只按实体类型分档（`entity/AirframeTargets:38-58`）；`VehicleDamageTable.row` 给出四列价；
  **bullet 对重/轻甲恒 0**，shell 才定价（`VehicleDamageTable:182-185`）。
- 没有命中部位与入射角，穿深是二值闸门（`DamageType:9-19`）。
- `WarheadSplash` = 同价 × 溅射比例 × 距离衰减，但**对载具恒 0**（`40-46`），所以溅射只伤软目标；
  炮兵目标额外 ×1.2。
- 已知不一致：`GroundWeapons.splash` 未排除车辆（`230-246`），与上面的"溅射不伤车"口径相左，值得复核。

### 5.3 火力任务

`J`（`KeyMappings:125`）→ 雷达面板（`client/ArtilleryCommand:28-39`）→ 点击发包（id 42）→ 服务端校验驾驶者/
距离/冷却（`entity/weapon/ArtilleryStrike:74-85`）→ 按 `shellsDue` 空投。PLZ：5s 预警、30 发/10s、散 15 格、
冷却 120s；PHL：7×7×2 = 98 发/14s、间距 6 格（`RocketStrikeGrid:7-11`）。

## 6. 基础设施

### 6.1 入口与注册

`LittleBird.java:24-39`：Entities → Blocks → BlockEntities → Items → Particles → CreativeTabs → Sounds，
再挂 CLIENT/COMMON 配置；网络在 `commonSetup` 的 `enqueueWork` 里注册。11 个物品类 = 8 个 `DeployItem`
+ `GunshipPlacement/GunshipPlacementProbe` + `SmokeGrenadeItem`，放置统一走地面探针。

### 6.2 配置（225 个键）

CLIENT 5 节：`flight`（含 `damping`）、`view`、`effects`、`ground{tank,ifv,bradley}`、`b2`；
COMMON：`ammo_crate`、`balance{tow,apache}`、`ground{main_battle_tank,infantry_fighting_vehicle,bradley}`、
`b2`、`smoke_grenade`。常用旋钮：`max_forward_speed`(1.3889)、`yaw_rate`(9)、`yaw_damping`(0.2)、
`camera_roll_factor`(1)、`hud_scale`(2)、弹药箱 40 发/s、TOW 转向 12°/t、`gun_damage`(14)、`jdam_damage`(240)。

### 6.3 网络

单 channel `battlefield:main`，协议号 `"1"` 双向精确匹配。**22 个包，id 0–18 与 40–42**，全部 `consumerMainThread`：
17 个 C→S 只发意图，5 个 S→C 只做表现（命中反馈 3、弹道 5、主炮落点 13、炸弹生成 17、战果 18）。
`ModNetworkRegistrationTest` 会在编译期锁住 22 个唯一 id。

### 6.4 HUD

唯一 `IGuiOverlay` 注册在最上层（`client/ClientModEvents:67-71`）→ `HudView{appliesTo,render}`，先匹配者胜：
`HudB2` → `HudAir`（小鸟/阿帕奇/喷气，显式排除 B2）→ `HudGround`（坦克/野猫/Bradley）→ `HudArtillery`（PLZ/PHL）。
绘制只用 `HudCanvas` 的 panel/row/bar/reticle/chevron/line/alerts/hint；配色与网格在 `HudStyle`；
缩放只放大边距不缩字体；几何抽到 `LittleBirdHudLayout`/`GroundHudLayout`/`B2HudLayout` 以便纯测。
F10 即时写回 `CLIENT_SPEC.save()`。

### 6.5 配方与进度

`ModRecipeProvider` 生成 29 个配方（全部带 `unlockedBy`），另有 8 个手写 JSON（7 个 `*_repair` + 烟雾弹）。
8 个 `first_*` 成就走 `inventory_changed`，根 `story/root`。`VehicleAssembly.ALL` 7 行（零件 + ticks），
载具工厂按"最贵可造"匹配；`VehicleAssemblyTest` 与生成配方交叉校验。

### 6.6 测试体系

98 类 / 726 例：weapon 41/341、client 22/172、flight 6/78、ground 11/61、entity 8/45、seat 3/14、根 5/11、
item 1/2、network 1/2。Gradle 只 `useJUnitPlatform()`，不跑 client/server、不 Bootstrap。
数据从 JSON 读（`VehicleAssemblyTest`、`RecipeGridTest`），网络用正则读源码（`ModNetworkRegistrationTest`）。

## 7. 扩展检查清单

| 要加什么 | 必须改的文件 |
| --- | --- |
| 新载具 | `ModEntities` + 实体类 + `Geometry/Model/Renderer` + `ClientModEvents` + `GroundSeat`/座位 + `HudGround` 或 `HudAir` 分支 + `lang` + `Drive.Spec`/`FlightProfile` + `AirframeTargets`（漏了按无甲计价） |
| 新弹种 | `MainGunAmmoType` + `LittleBirdConfig` + `VehicleDamageTable.Weapon` + 弹丸实体/Renderer + HUD 键 |
| 新网络包 | 新 Packet + `ModNetwork` id/方向 + **提升 `PROTOCOL_VERSION`** + 更新注册测试 |
| 新 HUD 面板 | 新 `HudView` + `HudOverlay.VIEWS` 顺序（B2 必须在 Air 前）+ 复用 `HudCanvas/HudStyle` + Layout 类与测试 |
| 新物品 | `ModItems` + item 类 + `CreativeTabs` +（可部署）+ `ItemModelProvider` + `lang` + 配方 + 测试 |
| 修复类物品 | 走 `GunshipRider.maxHealth()/setHealth()`，不要按车型 instanceof |
| 新配方 | `ModRecipeProvider` → `runData --rerun-tasks` → 过 `RecipeGrid`/`VehicleAssembly`/`RepairRecipe` 三测 |
| 新飞机 | 换 `FlightConfig` profile；若加机型差异则扩 `JetType` |

## 8. 已验证的技术债（可直接开工的清单）

1. `ApacheEntity.tickRemoteInterpolation()` 空实现（522-524）：远端玩家看阿帕奇不插值，小鸟有完整 `lerpTo`（407-437）。
2. `applyLocalFlight` 在 `LittleBirdEntity`(211-273) 与 `ApacheEntity`(527-593) 逐行重复。
3. 具体车型 `instanceof` 仍散落 **36 处**（HUD/相机/乘员渲染/座位/血条）；`GunshipRider`、`GroundView`、
   `GroundVehicleControls`、`ArtilleryPlatform` 已存在，说明 S0 抽象层做了一半。
4. `assets/battlefield/ui_theme/{dark,olive,amber}.json` 已无任何代码引用（HUD 重建后遗留的死资源）。
5. `MainGunShellEntity.configure` 仍接三价参数但已弃用，`detonate` 重新查表（82-95 vs 226）。
6. `GroundWeapons.splash` 不排除车辆，与 `WarheadSplash` 口径不一致（见 §5.2）。
7. 文档漂移：`bf2042-vehicle-expansion.md` 写「4 车 / 44 测试文件 / 319 例」，实际 10 车 / 98 文件 / 726 例；
   `HANDOFF.md` 写「242 单测」。
8. `AGENTS.md` 第 1 条强制要求的技能文件在本机不存在（作者另一台机器的路径）。

## 9. 开发流程（本机已验证）

```bash
export JAVA_HOME="D:\\DevTools\\Java\\jdk-17.0.20+8"
./gradlew compileJava     # 几秒（增量）
./gradlew test            # 726 例，约 1 分钟
./gradlew build           # 2 分钟以内，产 build/libs/battlefield-0.1.0.jar
./gradlew runData --rerun-tasks   # 只有改配方/物品模型时
```

构建细节见 `docs/BUILD-WINDOWS.md`。约定（`AGENTS.md`）：改动前先只读诊断、证据优先、切片 ≤ 8 文件、
纯规则类先写单测、build 成功不等于游戏内验证通过。
