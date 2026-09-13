# 飞控规格：BF6 鼠标偏航四轴模型

## 1. 依据（外部资料 + 已核实摘录）

- BF6 直升机由四个轴驱动，官方/攻略一致表述为：
  总距（油门）= 垂直升降（W/S）；偏航（舵）= 机头左右旋转（A/D 或自定义为鼠标）；
  俯仰 = 机头上下指向、决定前后移动（鼠标 Y）；滚转 = 左右压坡度、产生平移与倾斜转弯（A/D 或自定义）。
- BF6 攻略给出的建议 PC 改键正是「鼠标偏航」预设：把俯仰与偏航都放到鼠标上，
  滚转移到 A/D，总距在 W/S。原文要点：这样把两个瞄准轴放在同一只手，攻击时对机头控制更精确。
- 结论：用户要的「鼠标偏航」= 鼠标同时给偏航与俯仰，滚转在 A/D。本项目按此实现，
  并把 BF6 默认布局（A/D 偏航 + 鼠标俯仰/滚转）做成可切换的备选预设。
- 链接见 evidence.md 第 1 节。

## 2. 轴映射（首版默认，可配置）

| 轴 | 输入 | 语义 | 目标量 |
| --- | --- | --- | --- |
| 偏航 yaw | 鼠标 X（骑乘玩家 yRot） | 直接给机头朝向 | 机体 yaw（限速跟随） |
| 俯仰 pitch | 鼠标 Y（骑乘玩家 xRot） | 机头上下 → 前后推力 | 俯仰角与视线 1:1，硬限幅 ±90° |
| 滚转 roll | 原版 keyLeft / keyRight | 压坡度 → 悬停侧移 + 有速度时协调转弯 | 坡度 = 速率积分，无上限（可翻滚倒飞） |
| 总距 collective | 原版 keyUp / keyDown | 升降 | 垂直速度目标 ±0.40 / -0.50 格/t |
| 机枪开火 | 原版 keyAttack（左键） | 持续射击 | 布尔 held |
| 火箭开火 | 原版 keyAttack（左键，当前武器为火箭时） | 单发/长按连发 | 布尔 held |
| 武器切换 | 原版热键 1 / 2（骑乘时拦截） | 机枪 / 火箭巢 | 枚举 |
| 齐射模式 | 新键位 Z | 单发 ↔ 连发切换 | 布尔 |
| 退出载具 | 新键位 X（LittleBirdKeyMappings.EXIT） | 下机 | 边沿包 |

设计取舍：
- 只新增 2 个按键（Z 齐射、X 退出），其余全部复用原版按键，避免键位冲突，也贴近 BF6 的 1/2 + 左键习惯。
- 骑乘时热键 1/2 的原版语义（切主手物品槽）无意义，因此安全拦截。
- 不采用自由视角：BF6 载具默认相机锁定机头轴线，本模组保持相机与机头耦合（首版）。

## 3. 状态与输入的数据结构（纯规则，无 Minecraft 类）

    final class HelicopterInput {          // 每 tick 的输入快照
        float yawDeg;                      // 绝对朝向（鼠标）
        float pitchInput;                  // -1..1，正 = 机头下压（前飞）
        float rollInput;                   // -1..0..1，正 = 右坡度
        float collectiveInput;             // -1..0..1，正 = 上升
        boolean onGround;
    }

    final class HelicopterState {          // 可序列化/可断言的状态
        float yawDeg, pitchDeg, rollDeg;   // pitchDeg 正 = 机头下压（与 MC xRot 同号，直接复用）
        double vx, vy, vz;                 // 格/t
        float rotorRpm;                    // 0..1
        boolean engineOn;
    }

    final class HelicopterFlightModel {    // 纯函数：step(state, input, config) -> state
        static HelicopterState step(HelicopterState s, HelicopterInput in, FlightConfig cfg);
    }

## 4. 每 tick 解算（可作为单测逐条断言的规格）

    // 0) 输入整形
    pitchIn  = deadzone(in.pitchInput, cfg.pitchDeadzoneDeg / cfg.pitchMaxDeg)
    rollIn   = in.rollInput
    collIn   = in.collectiveInput

    // 1) 旋翼转速（发动机启动/无驾驶员时衰减）
    targetRpm = (engineOn && hasPilot) ? 1.0 : 0.0
    rotorRpm  = approach(rotorRpm, targetRpm, cfg.rotorSpoolRate)   // 0 -> 1 约 40 tick
    liftScale = clamp01((rotorRpm - cfg.rotorMinRpm) / (1 - cfg.rotorMinRpm))

    // 2) 偏航：鼠标给绝对朝向，限速跟随，不做自动回正；结果回绕到 -180..180
    yaw = wrap(approachAngle(yaw, in.yawDeg, cfg.maxYawRateDegPerTick))   // 9.0 度/t = 180 度/秒

    // 3) 坡度：速率指令 —— 按住多久就滚多少，没有坡度上限（可以滚过 90 度、倒飞）
    if (abs(rollIn) < 1e-3) rollDeg = approachAngle(rollDeg, 0, cfg.rollLevelRateDegPerTick)
    else                    rollDeg = wrap(rollDeg + rollIn * cfg.rollRateDegPerTick)   // 6.0 度/t = 120 度/秒
    // 默认 rollLevelRateDegPerTick = 0：松开滚转键后坡度**精确保留**；
    // 把 auto_level 打开才会按最短路径回平。
    // 键盘是开关量，滚转轴两级缓入：先过 HelicopterInputDamping（0.25/tick），
    // 再过 HelicopterRollDamper（每 tick 最多 1/rollSpinUpTicks，默认 10 tick 到满速率）——
    // 这就是「启动阻尼」：按下 A/D 后滚转速率平滑爬升，而不是第一 tick 就跳到满舵。
    // 抬键时两级都当 tick 归零（不阻尼），否则坡度会多滚 0.2~0.4 秒，无法精确停住。

    // 4) 俯仰：与视线 1:1（输入轴 = xRot / pitchMaxDeg，满舵就是竖直朝下/朝上），
    //    区间硬限幅 ±cfg.pitchMaxDeg（默认 ±90），120 度/秒，无输入按 auto_level 决定保持还是回平
    pitchTarget = clamp(in.pitchInput, -1, 1) * cfg.pitchMaxDeg
    rate        = (abs(pitchIn) < 1e-3) ? cfg.pitchCenterRateDegPerTick : cfg.pitchRateDegPerTick
    pitchDeg    = clamp(approach(pitchDeg, pitchTarget, rate), -cfg.pitchMaxDeg, +cfg.pitchMaxDeg)

    // 5) 水平速度：**推力沿机体机头轴输出** —— 机头指哪，飞机就往哪飞
    //    推力大小与俯仰「范围」解耦：命令和姿态都在 pitchThrustReferenceDeg（32 度）饱和，
    //    所以俯仰放开到 ±90 之后，正常压 32 度依然是满功率，不需要俯冲 90 度才有速度。
    cmd     = clamp01(abs(pitchIn) * cfg.pitchMaxDeg / cfg.pitchThrustReferenceDeg)
    thrust  = min(1, abs(pitchDeg) / cfg.pitchThrustReferenceDeg)
              * (pitchDeg >= 0 ? cfg.forwardAccel : cfg.reverseAccel) * liftScale * cmd   // 恒 >= 0
    cosP    = cos(pitchDeg); sinP = sin(pitchDeg)
    fwd     = sign(pitchDeg) * thrust * cosP        // 机头轴的水平分量（压得越陡，前推力越小）
    steer   = clamp01(cfg.pitchDiveCoupling)        // 机头对「垂直行程」的操控强度（单一旋钮）
    dive    = thrust * sinP * steer                 // 机头轴的垂直分量：下压=俯冲，拉杆=上仰爬升
    side  =  sin(rollDeg) * cfg.sideSlipAccel * liftScale * (1 - speed/maxForwardSpeed)   // 正坡度 = 右移
    // 悬停时满侧移，有速度后淡出：同一份坡度改由下面的协调转弯体现
    dirFwd   = (-sin(yaw), 0,  cos(yaw))      // MC 约定：yaw 0 = +Z(南)，90 = -X(西)
    dirRight = (-cos(yaw), 0, -sin(yaw))      // 与 vanilla Vec3.directionFromRotation 一致
    vx += (dirFwd.x * fwd + dirRight.x * side)
    vz += (dirFwd.z * fwd + dirRight.z * side)
    horizontal = clampLength(vx, vz, cfg.maxForwardSpeed)                    // 0.90 格/t = 18 格/秒

    // 5b) 协调转弯（不在 step 内，由 LittleBirdEntity 施加到驾驶员朝向）：
    //     bankedTurnDegPerTick(speed, rollDeg, lift, cfg)
    //       = clamp(g * tan(坡度) / max(speed, 0.05 * maxForwardSpeed), ±cfg.bankedTurnDegPerTick)
    //         * clamp01(speed / (0.35 * maxForwardSpeed)) * clamp01(lift)
    //     物理：转弯半径 v^2 / (g tan 坡度)，所以坡度越大、速度越慢转得越快；
    //     悬停时速度淡入 = 0（只侧移不转弯），旋翼无升力不转弯，cfg 设 0 可整体关闭。
    //     该角速度同时加到骑乘玩家 yaw 和阻尼滤波器 yaw（rotateYaw），机头与相机一起转，
    //     否则阻尼滤波器会把这次转弯当成偏航误差、把机头拽回鼠标方向（蟹行）。

    // 6) 垂直：总距给目标垂直速度，但**可用的垂直权限随旋翼盘倾角下降**
    //    verticalAuthority = liftScale * (1 - steer * (1 - cos(pitch)))：机头竖直向下/向上时
    //    旋翼盘侧对气流，垂直权限归零，重力不再被托住 —— 这就是「大俯角换俯冲」的来源。
    //    steer = 0 时 verticalAuthority = liftScale，垂直方向完全回到旧模型（只由总距决定）。
    vyTarget = collIn > 0 ? collIn * cfg.maxClimbSpeed : collIn * cfg.maxDescentSpeed
    vy       = s.vy - dive                                     // 机头轴带来的俯冲/爬升分量
    vy       = approach(vy, vyTarget * verticalAuthority, cfg.verticalAccel * verticalAuthority)
    if (abs(collIn) < 1e-3) vy -= cfg.hoverSinkAccel * verticalAuthority   // 悬停自稳关闭时缓慢下沉
    vy      -= cfg.gravity * (1 - verticalAuthority)^2         // 旋翼未起转 / 盘面侧倾时的重力残留

    // 7) 地面/碰撞
    if (onGround && vy < 0) vy = 0
    vy = clamp(vy, -cfg.maxFallSpeed, +cfg.maxClimbSpeed)      // 俯冲上限 1.2 格/t = 24 格/秒

## 5. 首版常量表（全部进 Forge 配置，先按下表标定，再在 runClient 里调）

| 常量 | 首版值 | 单位 | 说明 |
| --- | --- | --- | --- |
| maxForwardSpeed | 1.3889 | 格/t | 27.8 格/秒 = **100 km/h**（HUD 显示值），配置上限 2.0 |
| forwardAccel | 0.030 | 格/t^2 | 满下压沿机头轴的推力，约 30 tick 到极速 |
| reverseAccel | 0.040 | 格/t^2 | 抬头减速更快（BF 刹车感） |
| sideSlipAccel | 0.020 | 格/t^2 | 悬停满坡度侧移（有速度后按速度比例淡出） |
| maxClimbSpeed | 0.40 | 格/t | 8 格/秒 |
| maxDescentSpeed | 0.50 | 格/t | 10 格/秒 |
| verticalAccel | 0.035 | 格/t^2 | 约 12 tick 到满爬升率 |
| hoverDampAccel | 0.020 | 格/t^2 | 松手垂直速度归零 |
| maxYawRateDegPerTick | 9.0 | 度/t | 180 度/秒 |
| bankedTurnDegPerTick | 6.0 | 度/t | 协调转弯上限（0 = 关闭，只保留侧滑） |
| pitchMaxDeg | 90.0 | 度 | 俯仰硬限幅（= 视线可达范围，与机枪射界对齐） |
| pitchThrustReferenceDeg | 32.0 | 度 | 达到满推力的机头角，把「能看多低」和「推力多大」解耦 |
| pitchDiveCoupling | 1.0 | | 机头对垂直行程的操控强度：1 = 完全物理（下压掉升力转俯冲、拉杆 flare 爬升），0 = 垂直只由总距决定 |
| pitchRateDegPerTick | 6.0 | 度/t | 120 度/秒 |
| pitchCenterRateDegPerTick | 0.0 | 度/t | 0 = 松手保持俯仰；auto_level 打开时为 3.0 |
| pitchDeadzoneDeg | 1.5 | 度 | 鼠标手抖抑制 |
| rollRateDegPerTick | 6.0 | 度/t | 120 度/秒；坡度是速率指令，无坡度上限 |
| rollSpinUpTicks | 10 | tick | 滚转速率爬升到满舵所需时间（启动阻尼，2~40，松手不阻尼） |
| rollLevelRateDegPerTick | 0.0 | 度/t | 0 = 松手精确保持坡度；auto_level 打开时为 4.5 |
| rotorSpoolRate | 0.025 | /t | 0 -> 1 约 40 tick（2 秒） |
| rotorMinRpm | 0.15 | | 低于此转速无升力（启动过程会下沉） |
| gravity | 0.040 | 格/t^2 | 旋翼失效时的下坠 |
| maxFallSpeed | 1.20 | 格/t | 完全失力时的终端下坠速度 |
| crashSpeedThreshold | 0.45 | 格/t | 超过 9 格/秒撞方块开始受伤 |
| crashDamagePerSpeed | 90.0 | HP/(格/t) | 伤害 = (撞速 - 阈值) × 该系数 |
| health | 80.0 | HP | 载具耐久（实现为 LittleBirdEntity.MAX_HEALTH，不进 FlightConfig） |

## 6. 相机与视角（BF6 观感的关键）

- 骑乘时强制第三人称（Minecraft.options.setCameraType），下机恢复原视角（配置可关）。
- 镜头滚转：Forge 事件 ViewportEvent.ComputeCameraAngles，setRoll(机体坡度 × cameraRollFactor)，
  默认 1.0，可配 0 关闭。该事件在 Forge 1.20.1 可用，不需要 Mixin。
- 镜头位置略高于机体、退后，力度以「看得见机头与准星轴线」为准，具体在 runClient 迭代。
- 座舱视角：驾驶员被 `positionRider` 沿**机体机头轴**前移（`LittleBirdCameraMath.cockpitSeatOffset`，
  含俯仰项），所以俯冲到 ±90 度时相机跟着座椅往下走，而不是被留在机体外面。
- 准星：机头轴线投影（hitscan 原点/方向由服务器按实体真实位姿计算），不是玩家视线。
- 隐藏第一人称元素：RenderHandEvent 取消、RenderPlayerEvent.Pre 取消（先例：DroneInput 第 104-117 行）、
  骑乘时取消 Item 使用/交互（InputEvent.InteractionKeyMappingTriggered，同上第 97-102 行）。

## 7. 平滑与手感调参（对齐 BF6 的灵敏度/死区设置项）

- 灵敏度：鼠标偏航是「绝对朝向」关系，因此灵敏度体现为 maxYawRateDegPerTick 与 pitchRateDegPerTick；
  实际操作体验 = 玩家自己的鼠标灵敏度 × 我们的限速，需要给玩家提供 0.5x/1.0x/1.5x 三档或滑条配置。
- 死区：pitchDeadzoneDeg（俯仰）已含；偏航死区不需要（绝对朝向，无漂移）。
- 反转俯仰：配置项 invertPitch，默认 false。
- 键盘是开关量：滚转/总距必须走 approach 斜坡（即第 4 节第 3/4/6 步），否则手感生硬。
  滚转例外：它是速率轴，按下经阻尼缓入、抬键立即归零，这样坡度既能带惯性又能精确停住。
- 所有手感数值必须集中在一个 FlightConfig 结构里，客户端配置读取，禁止散落魔法数字。

## 8. 边界与异常（必须被测试覆盖）

1. 无驾驶员 / 断开连接 / 下机：engineOn = false，旋翼衰减，重力接管，落地后停稳。
2. 玩家死亡或换维度：强制下机 + 载具状态清洗（先例：Rover prepareOwnerDisconnect 第 1581 行）。
3. 区块未加载 / 实体被卸载：位置与状态由 NBT 保存与恢复（先例：Rover readAdditionalSaveData 1638 行）。
4. 服务器上非驾驶员玩家：不允许驱动（服务器只信 getControllingPassenger）。
5. 数值安全：所有 approach/clamp 必须防 NaN（speed == 0 时的归一化、角度回绕）。
6. 水中：首版按「无法起飞、缓慢下沉」处理，配置项 allowWater = false。
7. 世界边界/高限度：撞到世界高度上限时停止上升，不产生 NaN。

## 9. 该子系统的验证方法

- JUnit（纯规则，不需要启动游戏）：
  - 满舵下压后水平速度应达到 maxForwardSpeed 且不超；
  - 滚转启动阻尼：第一 tick 不得到满舵，默认 10 tick 内单调爬到满速率，松手当 tick 归零；
  - 俯仰满舵可到 ±90 度并被硬限幅，半舵视线对应半舵姿态；
  - 推力在 pitchThrustReferenceDeg 饱和：0.38 舵就能达到极速；
  - 机头指向决定航向：压 45 度 = 前飞 + 下沉，拉 45 度 = 减速后退 + 爬升（flare）；
  - 机头 90 度向下 = 没有前推力、垂直速度冲向 maxFallSpeed；
  - 无输入 40 tick 后俯仰与坡度保持（auto_level 关闭），垂直速度趋稳；
  - 偏航限速：单 tick 朝向变化不超过 maxYawRateDegPerTick；协调转弯单独由 bankedTurnDegPerTick 限幅；
  - 角度回绕：从 179 度到 -179 度走最短路径（不能绕 358 度）；
  - NaN/极端输入（0 速度、超范围输入）不产生 NaN；
  - 旋翼未起转时 vy 必为负（下坠）。
- 游戏内（runClient，人工截图）：悬停稳定性、压坡度转弯的镜头滚转、
  高速掠地、撞墙坠机、贴地悬停、第三人称视野遮挡（树/建筑）。
