# 炮塔内视角跟随关系 — 阶段一 现状记录（只读）

## 1. 结论先行（决定了可行方案）
内视角**没有"可挂载的相机节点"**：它是**原版第一人称相机**（挂在乘客 Player 上）。模组只做了三件事：
1. 强制相机类型 `GroundVehicleInput.applyCameraMode` → `FIRST_PERSON`（client/GroundVehicleInput.java:134-137）；
2. 抬高眼位：`MainBattleTankEntity.positionRider`（:478-497），内视角额外加 `cockpit_eye_lift_blocks`；
3. 只改 FOV：`GroundCamera.computeFov`（client/GroundCamera.java:29-45）。
相机**不在炮塔层级里**，与炮塔无父子关系 —— 这正是"炮塔壳体扫过视野"的根因，也意味着"把相机挂到炮塔挂点"在本工程
没有现成挂点可用（预案 3 的情况成立，故停在查证）。

## 2. 节点层级（现状）
```
MainBattleTankEntity
├─ 车体姿态 yaw      ← GroundDrive 客户端预测（entity/MainBattleTankEntity.java:365-377）
├─ 炮塔姿态 turret   ← MainGun.State，服务端每 tick 缓动+限位（:213-231，权威）
│                      同步字段 DATA_TURRET_YAW/PITCH（:230-231）
└─ 乘客 Player(driver)
   └─ 原版第一人称相机  位置 = 座位+眼位抬升（positionRider），朝向 = 鼠标 yaw/pitch
                         —— 与炮塔无任何父子/数据关系

渲染层（纯视觉，不参与相机）: root → body → turret → { mantlet, barrel, fifty_barrel, cheeks, bustle, ... }
  MainBattleTankModel.setupAnim(:325-331)
    turret.yRot = ApacheModelMath.turretYawRadians(entity.getTurretYawDeg(), entity.getYRot())
    turret.xRot = ApacheModelMath.turretPitchRadians(entity.getTurretPitchDeg(), entity.getXRot())
```
**与假设的偏差（重要）**：偏航与俯仰**都施加在同一个 `turret` ModelPart` 上**，炮管是其子节点，**没有独立的俯仰节点**
（client/MainBattleTankModel.java:256 `turret.addOrReplaceChild("barrel", ...)`）。所以"偏航留炮塔、俯仰留炮管"在本工程不成立，
且该层级只影响渲染，改它对相机没有任何作用。

## 3. 旋转量来源与每帧顺序
- 服务端：`tickServer()`(:156) → `tickTurret()`(:213-231) 读 `driver.getYRot()/getXRot()` → `MainGun.desiredAim`（限位）
  → `MainGun.advance`（3°/tick 缓动）→ 写 `DATA_TURRET_YAW/PITCH`（20 Hz 同步）。
- 客户端每帧：实体同步值 → 渲染 `setupAnim`（模型）+ 相机构建；`ViewportEvent.ComputeCameraAngles` 在相机构建时触发，
  **`GroundCamera` 未订阅它**（只有 `LittleBirdCamera:39`、`ApacheCamera:24` 订阅，用于机身滚转）。
- 因此相机朝向 = 玩家视角（鼠标），与炮塔值**没有任何耦合**；炮塔只用同步值旋转"模型"。

## 4. 方案选项（需你确认后再动手）
- **A（推荐，最小改动）相机偏航跟随炮塔**：在 `GroundCamera` 新增 `computeCameraAngles`，仅当"骑地面载具 + 内视角"时
  `event.setYaw(炮塔偏航)`（**设值而非叠加**，避免双重旋转）。效果：视野方向 = 炮管方向，炮塔壳体相对镜头**固定**，
  世界+车体扫过；准星=炮管方向（与既有火控口径一致）。位置仍留在座位。约 15 行，纯客户端，可一键回退。
  代价：炮塔缓动期间视角滞后跟随（20 Hz 步进，不抖）。
- **B（真正的"挂点"，改动大）**：在 A 基础上把相机**位置**也移到炮塔枢轴世界坐标+局部偏移。需 mixin 改 `Camera#setup`
  的位置（工程已有先例：`mixin/CameraDistanceMixin` + `battlefield.mixins.json` 的 client 段），要额外处理近裁剪、
  第三人称不受影响、重生/重进载具一致性。
- **C（视觉掩盖）内视角隐藏炮塔壳体**：你已明确禁止，不采用，仅列出。

## 5. 实现前需你确认的 3 点
1. 选 **A** 还是 **B**？（A 已能满足"炮塔壳体不再相对镜头扫过"）
2. 俯仰是否也要跟随炮塔？（A 只跟随 yaw；俯仰仍是"相机=鼠标、炮塔缓动到鼠标角"，收敛后炮管居中）
3. 相机位置继续留在驾驶座位（车体内）可以吗？

## 6. 本轮改动
无（只读查证）。回退点沿用 `docs/plan/backups/2026-09-12_pre-turret-stops/`；实现时会另建 `..._pre-turret-view/` 快照。
