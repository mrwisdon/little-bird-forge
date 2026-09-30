# 载具爬坡子系统重构说明（2026-09-14，含二次修正）

## 1. 目标与用户决策
- 目标：两大地面载具（M1A5 主战坦克、EBAA 小野猫步战）在 50° 目标坡面稳定爬升、全程无可感知抖动、车身姿态贴合坡面。
- 用户决策：50° **只作参数**；坦克+步战都改；车身姿态用**多探针贴合坡面法线**；测试基建先建；**最终验收由用户执行**；发现问题后**禁止代理再进游戏试**，只做代码级定位。

## 2. 引擎事实
- Minecraft Forge 1.20.1 / Forge 47.4.20 / Java 17，原版体素 AABB 碰撞，无第三方物理引擎。
- 原版 Entity.move 不处理 maxUpStep（映射后字节码仅 field/getter/setter），地面载具继承 Entity，台阶必须自研。
- +Y 向上，1 方块=1 格，20 tick/s，模型 16 units/block；坦克 AABB 3.4 格、步战 3.0 格。

## 3. 最终方案
### 3.1 姿态（新）
- entity/ground/GroundContact.java：四角地面高度拟合坡面（俯仰=前后均差、侧倾=左右均差；NaN/超 60° 无效）。
- entity/ground/HullPose.java：俯仰+侧倾 EMA 0.35 + 死区 0.75° + 限速 4°/tick，无效样本回平。
- GroundMover.sampleSurface：四角向下 ClipContext.Block.COLLIDER 射线；探针取车体自身 AABB 足迹（getBbWidth()/2），**不做前视**。
- 渲染：两渲染器在 Yaw→Pitch 后加 Axis.ZP 侧倾；侧倾为客户端渲染专用（未加网络同步）。
### 3.2 驱动坡度（修正）
- **驱动坡度用 SlopeTracker 实走坡度**，不用接触面：接触面会看前方地形、跨方块边界跳变，喂进速度回路会忽快忽慢（§64.30 的结论）。接触面只喂姿态。
### 3.3 台阶辅助（重写）
- 旧实现用 setPos 瞬移试探并在同 tick 多次 move，抬升后 AABB 可嵌进方块，下一 tick 被弹回（实测 z 逐 tick ±一个驱动步长、yaw 不变）；且「没抬够就 return」会留下离地状态，反复抬/落。
- 新实现 GroundMover.stepUp：不做任何 setPos；用 level.getBlockCollisions(vehicle, candidateAABB) 在 stepRises 梯级里找最小净空；然后一次真实 move 上抬→前移→落回。太高的墙原地不动。保证不以穿透/离地状态开始或结束。
### 3.4 参数
- ground.tank/ifv.max_grade_degrees = 50（含两份已存在配置文件）。gradeSpeedFactor 上坡线性降到 0.70，永不归零/回溜。
- HullPose：maxPitch 60°、maxRoll 35°、限速 4°/tick、死区 0.75°、EMA 0.35。

## 4. 改动文件
- 新增：GroundContact.java、HullPose.java、GroundContactTest.java、HullPoseTest.java、GroundClimbSimulationTest.java。
- 修改：GroundMover.java（采样 + stepUp 重写）、MainBattleTankEntity.java、InfantryFightingVehicleEntity.java、MainBattleTankRenderer.java、InfantryFightingVehicleRenderer.java、LittleBirdConfig.java。
- 暂留：HullTilt.java 及测试（不再被引用，验收通过后删）。
- 已删除：临时诊断采样器 io.github.littlebird.debug.ClimbTelemetry。

## 5. 已执行验证（构建层）
- ./gradlew test → 409 测试 0 失败。
- ./gradlew build → BUILD SUCCESSFUL；JAR sha256 6083fc1e852bb02a8b1905c9f33f2582536312cb2bb9032c64d1fcaea0c66954，与 Windows 实例 JAR 一致且仅一份。

## 6. 未验证 / 已知限制（用户验收项）
- 未由代理游戏内实跑（用户要求）：50° 上行、抖动主观评价、贴合/穿模、非爬坡回归均待用户实测。
- 台阶一次抬满，整块台阶可能有一次约 1 格视觉抬升；若可感知，下一步做「贴坡竖直跟随」而不是分 tick 半抬。
- 侧倾为客户端渲染专用，多人下其他客户端看不到。

## 7. 回滚
- 改动前副本：docs/plan/backups/2026-09-14_pre-ground-climb-refactor/（含 MANIFEST.sha256）。
