# 炮塔内视角跟随炮塔 — 查证 / 方案 / 实施记录

基线：/root/projects/little-bird-forge（Forge 1.20.1，modid battlefield）。
改动前编译基线：`./gradlew compileJava` exit 0（本轮实测，见第 3 节）。
备份：`docs/plan/backups/2026-09-13_pre-turret-follow/`（6 个文件 + SHA256SUMS.txt）。

## 0. 结论先行

- 内视角 = **原版第一人称相机**，挂在乘客 `Player` 上；模组只强制相机模式、抬高眼位、改 FOV，
  **从不写相机朝向**。地面载具没有 `ComputeCameraAngles` 订阅（飞行器有）。
- 炮塔偏航是**服务端权威**的绝对世界角，经 `SynchedEntityData` 以 20 Hz 同步；客户端只拿它转“模型”，
  与相机之间**不存在任何数据链路**。
- 因此驾驶时相机朝向 = 鼠标视角，炮塔以 3°/tick 缓动追鼠标；两者之差（缓动滞后）+ 相机不在炮塔枢轴上，
  就是“整个炮塔扫过视野”。**只补偏航不够**：见第 2.3 节实测几何，相机相对炮塔枢轴有 1.79 格偏移，
  只同步偏航时炮塔壳体/炮管仍会绕镜头摆最多约 178°。

## 1. 查证（只读）

### 1.1 炮塔旋转的唯一权威来源

| 环节 | 位置 | 说明 |
| --- | --- | --- |
| 输入 | `client/GroundVehicleInput.java:59-76` | 只读 WASD 驱动力；鼠标瞄准不打包，走原版玩家旋转 |
| 期望角 | `entity/MainBattleTankEntity.java:213-225` | `MainGun.desiredAim(driver.getYRot(), driver.getXRot())`，俯仰限位 [-30°,+10°] |
| 缓动/限位 | `entity/ground/MainGun.java:46-51` | `advance`：偏航/俯仰每 tick 最多 3° 逼近（约 60°/s），无限位（俯仰有限位） |
| 落库 | `MainBattleTankEntity.java:229-231` | `entityData.set(DATA_TURRET_YAW/PITCH)`，**绝对世界角** |
| IFV 同构 | `InfantryFightingVehicleEntity.java:151-168` | 复用 `ChainGunTurret.desiredAim/advance`，同样服务端权威 |

写入点唯一：只有服务端 `tickTurret` 写 `DATA_TURRET_YAW`；客户端明确注释“不做本地炮塔预览”
（`MainBattleTankEntity.java:378-381`）。渲染层 `MainBattleTankModel.setupAnim:324-331` /
`InfantryFightingVehicleModel.setupAnim:302-310` 只把绝对角转成相对车体的局部角。

### 1.2 内视角相机的挂载

```
MainBattleTankEntity            （炮塔偏航 = DATA_TURRET_YAW，绝对世界角）
├─ 车体姿态 yaw                 ← GroundDrive 客户端预测 (MainBattleTankEntity.java:365-377)
└─ 乘客 Player(driver)
   └─ 原版第一人称相机
      位置 = positionRider 的座位偏移，用 getYRot()（车体 yaw）旋转 (MainBattleTankEntity.java:477-497)
      朝向 = 鼠标 yaw/pitch（原版），本模组不写
      FOV  = GroundCamera.computeFov (client/GroundCamera.java:28-47)
```

- 相机**不在炮塔层级里**，没有父子/挂点关系。
- 地面载具**未订阅** `ViewportEvent.ComputeCameraAngles`；飞行器有：
  `LittleBirdCamera.java:38-48`、`ApacheCamera.java:23-33`（只用来加机身滚转）。
- 炮塔偏航 → 相机的数据流：**不存在该链路**（炮塔只驱动模型与 HUD）。

### 1.3 现象确认与复现

读码即可确认（无需运行时）：
1. 相机 yaw 每帧 = `Player#getViewYRot`（鼠标），炮塔 yaw = 服务端缓动值；
2. 快甩鼠标后炮塔以 3°/tick 追，滞后最大可达 180°，滞后期间炮塔相对镜头旋转 → 壳体扫过视野；
3. 相机相对炮塔枢轴还固定偏移 1.79 格（车长座前 26、侧 -12 单位），即使偏航对齐，炮塔也会绕镜头平移。

复现步骤：进入 M1A5/IFV 驾驶座 → 按 C 进内视角（`GroundVehicleInput.java:84-88` 强制 FIRST_PERSON）
→ 鼠标左右各满转一圈 → 炮塔壳体/炮管扫过画面。

## 2. 方案（先定文件与行号，再改）

### 2.1 目标
内视角相机在炮塔转向时**刚性跟随炮塔**（位置 + 偏航都在炮塔系），炮管俯仰仍只由玩家俯仰决定，
不改炮塔/武器逻辑、不改输入、不加依赖。

### 2.2 最小改动点

| # | 文件:行号 | 改动 |
| --- | --- | --- |
| 1 | `client/GroundView.java:10-15` | 接口加 `float turretYawDeg()`，让相机统一读到权威炮塔偏航 |
| 2 | `entity/MainBattleTankEntity.java:398-406` | 实现 `turretYawDeg()`（委托已有 `getTurretYawDeg():605`） |
| 3 | `entity/InfantryFightingVehicleEntity.java:281-288` | 同上（委托 `getTurretYawDeg():473`） |
| 4 | `entity/MainBattleTankEntity.java:485`、`InfantryFightingVehicleEntity.java:358` | `positionRider` 的旋转 yaw：内视角用炮塔偏航，其余用车体 yaw |
| 5 | `client/GroundViewMath.java` | 新增纯规则 `followYawDeg(interior, hullYaw, turretYaw)`（可单测） |
| 6 | `client/GroundCamera.java` | 新增 `computeCameraAngles`：内视角 `event.setYaw(炮塔偏航)` |
| 7 | `client/GroundHud.java:258-281` | `projectAimPoint` 增加相机 yaw 参数；内视传炮塔偏航，外部路径不变 |

俯仰明确**不**跟随炮塔（第 4 条只换 yaw）：相机 pitch 仍 = 鼠标 pitch，炮塔 pitch 仍由服务端缓动，
与改动前一致。

### 2.3 为什么必须连位置一起跟随（几何实测）

用 `positionRider` 的旋转公式 + `MainGun.aimDirection` + 坦克模型偏移做数值检查
（车体固定、炮塔偏航 0→360°，相机 yaw 都已设为炮塔偏航，即“只补偏航”的最好情况）：

| 炮塔部件 | 只补偏航：相对镜头最大偏摆 | 偏航+位置跟随 |
| --- | --- | --- |
| 炮口 (f=6.0 格) | 17.4° | 恒定 9.73° |
| 炮盾/防盾 (f=1.625) | 177.6° | 恒定 90.0° |
| 左/右炮塔脸颊 (f=1.25) | 178.4° / 178.8° | 恒定 102.1° / 146.3° |

相机相对炮塔枢轴偏移 = 1.790 格。只补偏航时炮塔仍绕镜头画半径 1.79 格的圆 → 壳体和炮管照样扫过；
把座位偏移按**炮塔 yaw** 旋转后，炮塔在镜头里是常量（刚性跟随）。

### 2.4 影响面

- 第三人称：`computeCameraAngles` 仅在 `isInteriorView()` 时写 yaw，第三视角不受影响；
  `positionRider` 的 `interiorView` 门控同样只在内视生效。
- 观瞄/HUD：内视准星按“炮管轴线在屏幕上的投影”绘制，改用同一相机 yaw 后投影基准一致；
  “GUN xx°”读数仍用鼠标视角（保持原语义：显示炮塔滞后鼠标多少度）。
- 服务器/多人：`interiorView` 是客户端标志，`positionRider` 客户端与服务端的乘员位置差 =
  座位偏移按炮塔 yaw 与车体 yaw 之差旋转的量（最多约 3.6 格）。这在本工程已有先例
  （内视抬升本来就只作用客户端，见 view-system-stage0.md 第 71 节）；服务端炮塔/武器/命中判定全部
  使用自己的绝对炮塔角，不受客户端相机影响。
- AI：两车均为载具，无 AI 炮塔调用同一实体；`ChainGunTurret`（Apache/IFV 共用）不改。
- 不新增依赖、不改输入映射、不改配置键。

## 3. 实施记录（已改，待实机观感确认）

改动前的编译基线：`./gradlew compileJava` exit 0。改动后：

| 命令 | 结果 | 证据 |
| --- | --- | --- |
| `./gradlew test --tests "*GroundViewMath*"` | BUILD SUCCESSFUL，6/0/0 | `build/test-results/test/TEST-...#GroundViewMathTest.xml` |
| `./gradlew build` | BUILD SUCCESSFUL | 315 tests / 0 failures / 0 errors（全部测试） |
| JAR | `build/libs/battlefield-0.1.0.jar` 466445 B | sha256 `8ae83e2afdfcd5b5dba783480608fde7597575b28bc56779102ecee2a6f459a9`（含时间戳，逐次构建会变） |
| 同步 | 已复制到实例 mods | 实例同名 JAR 同尺寸同 sha256；mods 里只有一份 battlefield JAR |

改动文件与行号（改后）：

| 文件 | 行 | 改动 |
| --- | --- | --- |
| `client/GroundView.java` | 16-21（`turretYawDeg` :21） | 新增 `float turretYawDeg()` |
| `client/GroundViewMath.java` | 31-51（`followYawDeg` :44） | 新增纯规则 `followYawDeg(interior, hullYaw, turretYaw)` |
| `client/GroundCamera.java` | 48-68（`computeCameraAngles` :60，`setYaw` :67） | 内视角 `event.setYaw(炮塔偏航)` |
| `client/GroundHud.java` | 216-219 / 234-237 / 261-276（`cameraYawDeg` :263） | `projectAimPoint` 用显式相机 yaw；内视传炮塔偏航，外部路径不变；GUN 读数仍用鼠标视角 |
| `entity/MainBattleTankEntity.java` | import :4 / `turretYawDeg` :410 / `positionRider` :489-496 | 内视按炮塔 yaw 摆座位 |
| `entity/InfantryFightingVehicleEntity.java` | import :4 / `turretYawDeg` :293 / `positionRider` :361-367 | 同上 |

测试新增（`GroundViewMathTest`）：`interiorCameraFollowsTheTurretYaw`（含 -179° 不回卷）、
`everyOtherCameraKeepsTheHullYaw`（非内视 / NaN / Inf 一律回车体 yaw）。

更新顺序：没有改 Update/FixedUpdate/LateUpdate，也没有加插值。相机的 yaw 在
`ViewportEvent.ComputeCameraAngles`（渲染前、相机建好后）写一次，位置在 `Entity#positionRider`
（每 tick，客户端与服务端同一入口）写一次；两者读同一个炮塔同步值。未加平滑的理由见第 4.3 节。

### 3.1 实机验收（改动前后同一存档、同一操作脚本）

证据目录：`docs/plan/evidence/detail-phase/turret-follow_*.png`，方法与逐条结论见
`turret-follow_verification.txt`。

| 验收 | 方式 | 结果 |
| --- | --- | --- |
| 1 炮塔本体不再扫过、炮管相对镜头稳定 | 内视角 + 鼠标步进，满转一圈 13 帧 | 改动前：炮管在画面左缘/出画（打墙 "6 m"）/右下之间跳，出现 "GUN -xx°"；改动后（`after_full_circle.png`）：13 帧炮管都在镜头同一位置，世界转满一圈 |
| 2 偏航一致、车体叠加正确、无反向无跳变 | 按住 D 让车体转、鼠标不动；再快速 ±500px 反复 | `after_hull_turn.png`：车体+世界在镜头下转，炮管不动（炮塔稳定）；`after_rapid.png`：来回切换炮管不抖、无穿模 |
| 3 极限/回正/快速切换无抖动滞后穿模 | 快速左右反复（0.2s 采集）×5 | 炮管相对镜头不抖；"滞后"只剩炮塔自身 3°/tick 的缓动（"GUN" 读数如实显示），未叠加任何额外平滑 |
| 4 其余相机行为不变 | 只取炮塔 yaw | 俯仰仍 = `player.getViewXRot`；内视准星仍按炮管轴线投影；"GUN" 读数仍用鼠标视角（语义与改动前相同） |

运行说明：dev 客户端在本环境需要把 LWJGL natives 放进 `java.library.path`
（`JAVA_TOOL_OPTIONS=-Djava.library.path=/tmp/mclibs`）。这是**改动前就存在**的开发环境缺口：
未改动的 12:19 `runClient` 日志同样以 `Failed to locate library: liblwjgl.so` 失败，与本次改动无关。
受控截图跑在 `DISPLAY=:99`（Xvfb + openbox）上，因为第一人称鼠标视角需要窗口拿到真实输入焦点。

## 4. 未决 / 需确认

1. 位置是否也要跟随炮塔：本轮按验收标准 1（“炮塔本体不再扫过视野、炮管相对镜头稳定”）判定**必须跟随**，
   否则第 2.3 节实测的 17°~178° 摆动仍在。若你只想“偏航跟随、位置仍留驾驶座”，把 2.2 的第 4 条撤掉即可
   （一行），但验收标准 1 会不达标。
2. 乘员实际位置在客户端会随炮塔摆动（服务端仍按车体系）。若你在意他人在第三人称里看到的副驾驶/驾驶员
   位置，需要改为 Camera mixin 方案（改动更大，本轮未采用）。
3. 20 Hz 同步带来的步进：相机与炮塔模型读同一个同步值，二者永不分离；世界会以 20 Hz 步进旋转。
   未加任何插值/平滑（符合“不用平滑掩盖结构问题”的约束）。实机满转一圈的截图序列里没有观察到
   会破坏验收的抖动；若实机仍觉得 20 Hz 步进明显，再单独议插值方案（属于新议题，不在本次范围）。
4. 本次只实测了坦克（M1A5）；IFV 走的是同一段代码（同一个 `followYawDeg` 调用与
   `computeCameraAngles` 门控），但没有实机驱动过，属于待补的验证项。

## 5. 后续修复：炮塔跟手（2026-09-13 本轮）

用户反馈：驾驶时炮塔旋转不跟手（跟鼠标不灵敏/不跟随）。只读定位后按“限速/阻尼”这一类根因修。

### 5.1 根因（只读证据）
- 唯一权威链：鼠标 → `Player#getYRot/getXRot` → `MainBattleTankEntity.tickTurret:214-232` /
  `InfantryFightingVehicleEntity.tickTurret:152-169` → `MainGun.advance` / `ChainGunTurret.advance`。
- 旧 `advance` 用 `HelicopterFlightModel.approachAngle` 每 tick 只逼近 3°/tick（坦克 = 60°/s）、
  6°/tick（IFV/阿帕奇，120°/s）：限速远低于鼠标角速度，故落后且在松手后继续追齐。
- 内视角相机 yaw 被 `GroundCamera.computeCameraAngles` 钉在同一个缓动值上，限速因此直接拖慢视野。
- 结论：根因属“过强的平滑/阻尼（限速）”，不是帧率相关、输入丢失，也不是父节点/动画覆盖。

### 5.2 改动（最小）
| 文件 | 行（改后） | 改动 |
| --- | --- | --- |
| entity/ground/MainGun.java | 42-49（`advance` :47） | 偏航/俯仰直接取瞄准角 `wrap(targetYaw(desired))` / `clampPitch(...)`；删除 `SLEW_DEGREES_PER_TICK=3.0` |
| entity/weapon/ChainGunTurret.java | 76-81（`advance` :77） | 同上：`normalize(targetYaw(desired))` / `targetPitch(desired)`；删除 `SLEW_DEGREES_PER_TICK=6.0` |
| test MainGunTest.java | :28 / :36 | “一 tick 只转 3°” → “一 tick 到 90°” + “跨 ±180 停在 -170°” |
| test ChainGunTurretTest.java | :49 / :57 | “一 tick 只转 6°” → “一 tick 到 60°” + “跨缝不回卷” |

俯仰止点（坦克 -30/+10、机炮 -11/+60）**未动**；射速/热度/冷却逻辑未动。
影响对象：坦克主炮、IFV 30mm（共用 `ChainGunTurret`）、阿帕奇 30mm（共用 `ChainGunTurret`）。

### 5.3 验证
- `./gradlew build` BUILD SUCCESSFUL；**316 tests / 0 failures / 0 errors / 0 skipped**（44 类）。
- JAR `battlefield-0.1.0.jar` 466179 B，sha256 `3ebd3e51c233ed5646ca6d2044fef7a5185c131e42347a227278948cd79c49f8`；
  已随 `syncLocalMods` 同步，实例 mods 同尺寸同哈希、仅一份 battlefield JAR。

**实机（dev client，Xvfb :99 + openbox + llvmpipe）**：临时在 `tickTurret` 打一行 `System.out`
（跑完已撤，`MainBattleTankEntity.java` 已还原为改动前字节）记录每 tick
`driver.getYRot/getXRot` 与 `turret.aimYawDegrees/aimPitchDegrees`，xdotool 甩动鼠标。

| 观测 | 结果 |
| --- | --- |
| 全程 2633 tick 偏航误差 \|wrap(look−turret)\| | **最大 0.000004°，0 tick 超出 0.01°** |
| 单 tick 视线最大变化（快速甩动） | **135°**，同 tick 炮塔完全跟上 |
| 跨 ±180°（例 tick1325→1326） | look 135.00009 → −176.99991，turret 同 tick 同值，无回弹/反向/卡死 |
| 停止鼠标（连续多段） | look 恒定（如 −32.99991）时 turret 同值恒定 ≥20 tick，无漂移/缓慢追齐 |
| 俯仰 | look −72°/+18° 时 turret 停在 **−30°/+10°** 止点（限位保留，非滞后） |
| 帧率 | 该环境软件 GL 下约 16 fps；瞄准按 20 Hz 服务端 tick 计算、链路无 deltaTime/逐帧项，帧率不影响 |

残留：20 Hz 同步步进（既有，本轮未加插值）。

## 6. 第一人称卡顿 / 转向台阶修复（本会话）

用户反馈：第一人称（炮塔内视角）卡顿、转向不平滑，但"跟手"不能退化。

### 6.1 定位（只读）
唯一生效实现 = 原版第一人称相机 + `client/GroundCamera.computeCameraAngles`（渲染帧 `ViewportEvent`，非多套实现）：
相机 yaw `GroundCamera:67` → `GroundViewMath.followYawDeg` → `GroundView.turretYawDeg()` →
`entityData(DATA_TURRET_YAW)`（20 Hz 服务端同步）；炮塔网格 `MainBattleTankModel.setupAnim:327-330` /
`InfantryFightingVehicleModel:306-309` 读**同一原始同步值**；座位偏移 `positionRider` 亦然。车体用
`rotLerp/lerp` 插值，炮塔链路**无插值**、不涉及 deltaTime。

### 6.2 复现量化（临时探针，跑完已撤 / 已还原字节）
Xvfb :99 + openbox + llvmpipe、New World 骑 M1A5、C 进内视角、xdotool 匀速转 ~8 s；窗口缩到
320×180 把 FPS 抬到 ~25（**仅测量用**，修复本身不改分辨率/画质/帧率）。

| 指标（转向窗口 ~170 帧） | 改前 | 改后 |
| --- | --- | --- |
| 相机 yaw 逐帧"与上一帧相同"比例 | **26%**（原版视线 8%） | **9%**（原版视线 9%） |
| 一个 yaw 值保持的中位时长 | **49.3 ms**（=20 Hz tick） | **41.4 ms**（=帧间隔） |
| 连续 ≥2 帧不动的段数 | **27/127** | **1/159** |
| 非零步长 中位/最大 | 3.60° / **7.20°**（跳帧双倍步） | 2.84° / 4.99° |
| \|相机 yaw − 原版视线 yaw\| | 中位 **4.90°** / max 9.47° | **0.00°** |
| 帧时间 p50/p90/max | 43.3 / 52.5 / 62.8 ms | 41.1 / 51.5 / 61.9 ms |
| 停手后（尾 60 帧） | 恒定 | 恒定 |

### 6.3 根因
`GroundCamera:67` 把每帧已插值好的原版视线（`event.getYaw()`）丢弃，改用 20 Hz 同步炮塔值逐帧
**绝对赋值**；炮塔网格同样读原始同步值。渲染帧率与 20 Hz 不匹配 → 台阶。帧时间本身无尖峰，故
**不是** GC/分配/长帧问题。

### 6.4 修复（一处逻辑：内视角瞄准帧改用本机驾驶员自己的视线）
炮塔与驾驶员视线已是 1:1（第 5 节），对本机驾驶员而言最新且最平滑的就是其自身视线：
- `GroundView`：`turretYawDeg()` → `renderTurretYawDeg(partialTick)` / `renderTurretPitchDeg(partialTick)`。
- `MainBattleTankEntity` / `InfantryFightingVehicleEntity`：仅当 `interiorView && isControlledByLocalInstance() && 本地驾驶员`
  时返回 `driver.getViewYRot/getViewXRot(partialTick)`（pitch 按各自止点 clamp），否则回退同步值。
- `GroundCamera`、`MainBattleTankModel`、`InfantryFightingVehicleModel`、`GroundHud`、`positionRider` 统一读该值。
- 门控保证：**只有内视角 + 本机驾驶**才变；第三人称、其他玩家车体、服务端仍用同步值（未动）。
- 无新依赖；未改灵敏度/反转/FOV；未降分辨率/画质/限帧；可单文件回退。

### 6.5 验证
- `./gradlew build` BUILD SUCCESSFUL；**316 tests / 0 failures / 0 errors / 0 skipped**；
  JAR 466771 B sha256 `b42b8883c9808641e4363eac…`，已 `syncLocalMods` 同步、实例仅一份 battlefield JAR。
- 改前/改后见 6.2：周期性台阶段 27→1、重复帧 26%→9%（=原版水平）、相机相对原版视线的滞后
  4.9°→**0.00°**（延迟不增反降）、帧时间无尖峰、停手恒定。

