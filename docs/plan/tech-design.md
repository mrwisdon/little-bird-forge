# 技术设计：Little Bird（Forge 1.20.1）

## 1. 工程布局（M0 落地）

    little-bird-forge/
      AGENTS.md                       项目约定（已建）
      build.gradle                    改写自 final-paradox-forge 模板（modid 换 littlebird，去掉 Curios 依赖）
      gradle.properties               minecraft_version=1.20.1 / forge_version=47.4.20 / mod_id=littlebird
      settings.gradle                 rootProject.name = little-bird-forge（不 include 其他子工程）
      gradlew, gradlew.bat, gradle/wrapper/**   从参考工程复制（Gradle 8.8）
      docs/plan/**                    本规划文档
      src/main/java/io/github/littlebird/**
      src/main/resources/META-INF/mods.toml, pack.mcmeta
      src/main/resources/assets/littlebird/**
      src/main/resources/data/littlebird/**
      src/test/java/io/github/littlebird/**

Java 包结构（预计 44-48 个文件）：

    io.github.littlebird
      LittleBird.java                 主类：注册 + 配置 + FMLCommonSetup
      LittleBirdConfig.java           Forge ModConfigSpec（客户端手感 / 通用平衡）
      registry/  ModEntities, ModItems, ModSounds, ModCreativeTabs
      entity/
        LittleBirdEntity.java         载具实体：tick、骑乘、血量、武器挂载、NBT
        LittleBirdRocketEntity.java   70mm 火箭投射物实体
        LittleBirdDamage.java         伤害源与命中结算（服务器）
        flight/HelicopterInput.java       纯规则
        flight/HelicopterState.java       纯规则
        flight/HelicopterFlightModel.java 纯规则（核心）
        flight/FlightConfig.java          纯规则（由 Forge 配置映射）
        weapon/LittleBirdGunCycle.java    纯规则（转速/射速/过热）
        weapon/LittleBirdBallistics.java  纯规则（枪管原点/收敛/散布）
        weapon/RocketPodState.java        纯规则（14 发 / 左右交替 / 齐射节奏）
      network/  ModNetwork, LittleBirdControlPacket(滚转+状态), LittleBirdFirePacket,
                LittleBirdWeaponSelectPacket, LittleBirdSalvoPacket, LittleBirdExitPacket,
                LittleBirdImpactPacket, LittleBirdTracerPacket(S2C), LittleBirdRocketSpawnPacket(S2C)
      client/   LittleBirdInput, LittleBirdKeyMappings, LittleBirdCamera, LittleBirdHud,
                LittleBirdModel, LittleBirdModelMath, LittleBirdRenderer,
                LittleBirdTracerRenderer, LittleBirdTracerTimeline,
                LittleBirdEngineSoundInstance, LittleBirdGunSoundInstance, ClientModEvents

## 2. 注册（沿用参考工程可用的模式）

| 注册项 | 方式 | 先例 |
| --- | --- | --- |
| 实体 | DeferredRegister.create(ForgeRegistries.ENTITY_TYPES) + EntityType.Builder.of(...).sized(3.0F, 1.8F).clientTrackingRange(12).updateInterval(2)，getAddEntityPacket 用 NetworkHooks.getEntitySpawningPacket | ModEntities.java / DroneEntity 811 行 |
| 渲染器 | @Mod.EventBusSubscriber(MOD, Dist.CLIENT) + EntityRenderersEvent.RegisterRenderers | ClientModEvents 39-100 行 |
| 模型层 | EntityRenderersEvent.RegisterLayerDefinitions | ClientModEvents 102-106 行 |
| 按键 | RegisterKeyMappingsEvent（只新增 2 个：EXIT、SALVO） | ClientModEvents 26-36 行 |
| HUD | RegisterGuiOverlaysEvent（IGuiOverlay） | 本项目新增模式（参考工程无 HUD 先例） |
| 音效 | DeferredRegister.create(ForgeRegistries.SOUND_EVENTS) + sounds.json | ModSounds.java |
| 配置 | ModConfigSpec（CLIENT 手感 / COMMON 平衡），ModLoadingContext.registerConfig | 本项目新增（参考工程未用配置） |
| 部署物品 | DeferredRegister.create(ForgeRegistries.ITEMS) + 右键生成实体 | ReconDroneItem / DroneEntity.deploy 124 行 |

实体尺寸与追踪：机体 AABB 为 2.4 x 2.6（真实比例，旋翼与挂架外伸，见 LittleBirdGeometry 常量类），
clientTrackingRange 12（192 格）、updateInterval 2，避免高速飞行时其他玩家看到抖动。

## 3. 权威与同步模型（本设计最关键的决定）

### 3.1 运动学：客户端权威（与 vanilla 船/马一致）

- LittleBirdEntity.getControllingPassenger() 返回驾驶员玩家；
  于是 Entity.isControlledByLocalInstance() 在驾驶员客户端为 true。
- vanilla LocalPlayer 每 tick 会对「由本地权威控制的根载具」发送 ServerboundMoveVehiclePacket，
  包含位置 + yRot + xRot；服务器 handleMoveVehicle 接受并广播。
  先例：DroneEntity.tick 第 455-466 行（客户端 isControlledByLocalInstance 时由乘客旋转驱动载具）。
- 因此：机体 yaw（鼠标偏航）与机体 xRot（俯仰姿态 = 鼠标俯仰）无需自定义包即可同步。
- 滚转角不在该协议内 → 单独同步（见 3.2）。
- 其他玩家看到的平滑：重写 lerpTo 做远端插值（先例 DroneEntity 684-711 行）。

### 3.2 需要自定义同步的量

| 数据 | 方向 | 机制 | 频率 |
| --- | --- | --- | --- |
| 滚转角 | C→S 再广播 | LittleBirdControlPacket（变化 >0.5 度才发）→ 服务器写 SynchedEntityData | 变化时，上限 10/s |
| 当前武器 / 齐射模式 | C→S | LittleBirdWeaponSelectPacket / LittleBirdSalvoPacket（边沿） | 按键时 |
| 机枪开火 held | C→S | LittleBirdFirePacket(boolean held)（变化才发） | 变化时 |
| 火箭开火 | C→S | LittleBirdFirePacket 同通道或独立 rocket 位（边沿 + held） | 边沿/变化 |
| 机枪热度 / 状态 / 弹药 | S→广播 | SynchedEntityData（服务器权威写入） | 状态变化 |
| 曳光线 | S→跟踪者 | LittleBirdTracerPacket（PacketDistributor.TRACKING_ENTITY） | 每 2 发一条 |
| 火箭发射视觉 | S→跟踪者 | LittleBirdRocketSpawnPacket（或直接依赖实体本身同步） | 每次发射 |
| 硬碰撞坠机 | C→S | LittleBirdImpactPacket(speed)（服务器校验后结算） | 事件时 |
| 旋翼转速 | 客户端本地 + 服务器 | 客户端本地解算用于自身渲染；服务器 entityData 供其他玩家 | 每 tick |

先例：ModNetwork.java 的通道注册与「变化才发」的发送策略（DroneGunInputPacket 的 updateGunInput 去重，DroneInput 137-144 行）。

### 3.3 信任边界（必须在实现里写明，不能默认客户端说了算）

- 武器原点/方向：服务器用实体真实位姿计算，绝不接受客户端坐标。
- 开火前校验：pilot == getControllingPassenger()，且服务器自己跑机枪循环与弹药计数（客户端不决定命中）。
- 坠机包：校验玩家确实在骑乘、（撞速 - 阈值）系数、每 tick 只结算一次、冷却 20 tick。
- 所有 C→S 包做长度/范围校验（PacketTest 覆盖），并限制非法包不抛异常。

## 4. 载具实体职责（LittleBirdEntity）

- 骑乘：canAddPassenger（首版 1 座驾驶）、getPassengersRidingOffset、shouldRiderSit=true。
- tick 分支：
  - level().isClientSide && isControlledByLocalInstance()：解算 HelicopterFlightModel，setYRot/xRot，move(MoverType.SELF, delta)。
  - level().isClientSide && 其他：远端插值 + 旋翼视觉。
  - 服务器：武器循环（机枪/火箭）、过热、弹药、伤害、坠机结算、无驾驶员清理策略。
- 血量：hurt() 扣血、低于 30% 冒烟粒子、0 血爆炸 + discard；骑乘者在坠机时受摔落/爆炸伤害。
- 持久化：NBT 保存血量、弹药、武器模式、归属者 UUID、旋翼状态（先例 Rover readAdditionalSaveData）。
- 归属：默认世界持久化载具；可配置为「拥有者离线 5 分钟后清理」以对齐参考工程的孤儿清理思路。
- 保护：骑乘者不被自己载具挤压；载具不被水推、不被活塞推；不参与怪物 AI 仇恨选择（除被攻击）。

## 5. 双联速射机枪（M5）

| 项 | 设计 |
| --- | --- |
| 位置 | 机头两侧固定前向，枪口左右偏移 0.55 格、下移 0.25 格、前伸 1.2 格（按最终比例缩放） |
| 循环 | LittleBirdGunCycle：预热 8 tick → 每 2 tick 交替开枪（左/右）→ 热度 +2/发，上限 120，冷却 1/t |
| 过热 | 120 度触发，锁定 80 tick，期间禁射并在机头喷烟粒子 |
| 命中 | hitscan：射线 128 格，命中方块/实体；双枪各从自己枪口朝 32 格收敛点；散布 0.35°→1.10° 随热度 |
| 伤害 | 默认 3.5 HP/发（约 6 发击杀无护甲玩家），配置可调；用 damage_type 数据包给击杀归属 |
| 曳光 | 每 2 发一条曳光，存活 3 tick，客户端时间线渲染（先例 DroneTracerRenderer / DroneTracerTimeline） |
| 音效 | 循环音（LittleBirdGunSoundInstance），射速变化用 pitch 微调 |
| 保真度限制 | 无法表达数千发/分；用交替枪管 + 每 2 tick + 伤害折算模拟「速射」体感（必须在交付说明里写明） |

## 6. 70mm 火箭巢（M6）

| 项 | 设计 |
| --- | --- |
| 挂载 | 两侧短翼各 1 个 7 管吊舱，共 14 发；每发的发射管位置由 RocketPodState 给出（左右、管序） |
| 模式 A 单发 | 每次按下发 1 发，左右吊舱交替、管序递增 |
| 模式 B 连发（齐射） | 长按以每 7 tick（约 3 发/秒）交替发射，直到松开或打空 |
| 弹道 | LittleBirdRocketEntity：初速 0.75 格/t（前向 + 少量散布），1 秒内线性加速到 2.0 格/t，重力 0.012 格/t^2，寿命 120 tick 自毁 |
| 伤害 | 直击 30 HP + 半径 3 的衰减爆炸（中心 30 → 边缘 0）；爆炸不破坏方块（默认，可配置） |
| 再装填 | 待确认（默认：落地/悬停且 20 秒无发射后自动补满；备选：无限弹药 / 补给物品） |
| 视觉 | 发射管口焰 + 尾烟粒子；HUD 显示剩余 14 格弹药条 |
| 自伤 | 近距离发射自伤：默认关闭（发射口朝前，通常不会命中自己） |

## 7. HUD（M5/M6 共用）

- 覆盖层（RegisterGuiOverlaysEvent）：
  - 中央准星（机头轴线，非玩家视线）；
  - 左下：机枪热度条 + 过热图标；
  - 右下：火箭巢 2x7 弹药指示 + 单发/连发模式；
  - 右上：旋翼转速 %、海拔、水平速度（格/秒）；
  - 可选简易地平仪（随坡度倾斜的横线）。
- 只在「玩家驾驶本载具」时渲染；屏幕为空（无 GUI）时才画。

## 8. 资产需求（必须在实施前确认来源）

| 资产 | 需求 | 默认方案 |
| --- | --- | --- |
| 模型 | 机体、主旋翼 4 叶、尾桨、2 挺机枪、2 个火箭巢（LayerDefinition 代码模型，可分层旋转） | 我用代码建模（无外部文件） |
| 贴图 | 机体 256x256 + 旋翼模糊贴图 | 待确认：自制占位 / 用户提供 |
| 音效 | 引擎循环（可按转速变调）、旋翼低频、机枪循环、火箭发射、爆炸 | 待确认：自制合成占位 / 用户提供 |
| 语言 | zh_cn + en_us 的实体名、物品名、HUD 文案、按键名 | 我方编写 |

版权红线：不提取、不重分发 BF6 的模型/贴图/音效（技能要求：原始资产视为用户提供的参考资料，
不假定可再分发）。参考工程里的 .b6-audio-import 只有两首音乐，与直升机无关。

## 9. 测试策略（纯规则优先，参考工程已有 37 个 JUnit 测试的同样做法）

    src/test/java/io/github/littlebird/
      flight/HelicopterFlightModelTest.java     极限速度、限速、回中、回绕、NaN、旋翼失效
      flight/HelicopterInputTest.java           死区、反转、clamp
      weapon/LittleBirdGunCycleTest.java        预热、射速、过热、冷却与锁定
      weapon/LittleBirdBallisticsTest.java      左右枪管交替、收敛方向、散布随热度单调
      weapon/RocketPodStateTest.java            14 发、左右交替、齐射节奏、空巢不发
      network/*PacketTest.java                  编码/解码往返、非法输入不抛异常

分层验证顺序（技能要求）：compileJava → test → build（含资源校验与 reobf）→ 服务器逻辑冒烟 → 客户端截图批次 → JAR 同步 + 哈希核对。

## 10. 构建与同步

- JAVA_HOME 必须显式指向 C:\Program Files\Java\jdk-17（本机 PATH 上没有 java）。
- 全量构建：gradlew.bat build；reobf 产物在 build/libs/。
- JAR 同步：沿用参考工程的思路 —— 由 -Plocal_mods_dir=<mods 目录> 注入的 Copy 任务在 build 成功后复制；
  目的地必须核对存在性 + 时间戳 + 大小或哈希，且运行时游戏可能锁定 JAR（Windows）。
