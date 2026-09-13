# 实施切片计划（确认 PLAN.md 第 9 节后再开工）

规则（技能 + 根 AGENTS.md）：每个 DeepSeek 切片 = 一个子系统，1-5 个文件，最多 8 个；
一次只跑一个聚焦验证命令；写操作切片串行，只读调查可并行；
根代理负责诊断、方案、集成、全量构建、JAR 同步与强制自审。
每个切片开工前必须写 handoff 文件到 C:/Users/19818/.codex/subagent-handoffs/<task_name>.md，
并让子代理维护 C:/Users/19818/.codex/subagent-status/<task_name>.json（超过 2 分钟的切片）。

## M0 工程脚手架（根代理执行，不委派）

产出：
    build.gradle, gradle.properties, settings.gradle, gradlew(.bat), gradle/wrapper/**,
    src/main/resources/META-INF/mods.toml, pack.mcmeta,
    src/main/java/io/github/littlebird/LittleBird.java
    src/main/java/io/github/littlebird/registry/ModEntities.java（空注册器占位）
验证：
    ./gradlew compileJava  （WSL 原生 JDK 17）
停止条件：编译通过；runClient 能进入游戏并显示 Little Bird 模组已加载（无实体也要能启动）。
注意：参考工程的 build.gradle 带 Curios 配置，需移除未使用的依赖后再用。

## M1 纯规则层 + 单测（1 个切片，可委派）

拥有文件（5 个实现 + 5 个测试）：
    entity/flight/HelicopterInput.java, HelicopterState.java, FlightConfig.java, HelicopterFlightModel.java
    entity/weapon/LittleBirdGunCycle.java, LittleBirdBallistics.java, RocketPodState.java
    test/.../HelicopterFlightModelTest.java, LittleBirdGunCycleTest.java,
    LittleBirdBallisticsTest.java, RocketPodStateTest.java, HelicopterInputTest.java
约束：不得 import 任何 net.minecraft.* 类（先例：DroneGatlingCycle/DroneGatlingBallistics 的注释明确写了这一点）。
验证：./gradlew test --tests "io.github.littlebird.*"
停止条件：全部测试通过；常量与 flight-control.md 第 5 节一致。

## M2 载具实体骨架（1 个切片）

拥有文件：
    entity/LittleBirdEntity.java（骑乘、tick 分支、NBT、血量字段、getAddEntityPacket）
    registry/ModEntities.java（真实注册）
    network/ModNetwork.java + 基础包（Exit / Control / Fire）
验证：./gradlew compileJava，然后 runServer 用召唤命令生成实体、右键骑乘、下机。
停止条件：能骑乘下机、重登不丢载具、无客户端崩溃（缺渲染器会崩，本切片渲染器先用隐形渲染器占位）。

## M3 客户端飞控（1 个切片）

拥有文件：
    client/LittleBirdInput.java, LittleBirdKeyMappings.java, LittleBirdCamera.java, ClientModEvents.java（按键注册）
验证：./gradlew compileJava + runClient 手动：鼠标转朝向、W/S 升降、A/D 压坡度、X 下机。
停止条件：四项轴手感与 flight-control.md 一致；截图留档。

## M4 模型与渲染（1 个切片）

拥有文件：
    client/LittleBirdModel.java, LittleBirdModelMath.java, LittleBirdRenderer.java,
    client/ClientModEvents.java（layer + renderer 注册）
    assets: textures/entity/little_bird.png（占位或用户提供）
验证：runClient 出图；正/侧/俯/运动中四个角度截图。
停止条件：旋翼随转速转动、机体随坡度滚转、无渲染错位（先例要求：局部坐标与姿态先算对再调全局 yaw）。

## M5 双联速射机枪（1 个切片）

拥有文件：
    entity/LittleBirdWeapons.java（服务器侧接线，或并入实体文件的小改动）
    entity/weapon/LittleBirdGunRuntime.java, network/LittleBirdTracerPacket.java,
    client/LittleBirdTracerRenderer.java, client/LittleBirdTracerTimeline.java,
    client/LittleBirdGunSoundInstance.java
验证：runServer 逻辑冒烟（伤害/过热/弹药） + runClient 截图（曳光、枪口焰、过热）。
停止条件：10 发/秒、双枪交替、过热锁定、曳光在多人视角可见。

## M6 70mm 火箭巢（1 个切片）

拥有文件：
    entity/LittleBirdRocketEntity.java, network/LittleBirdRocketSpawnPacket.java（如需）,
    entity/weapon/RocketPodRuntime.java, client/LittleBirdRocketRenderer.java,
    entity/LittleBirdDamage.java
验证：runServer 冒烟（14 发、左右交替、齐射节奏、爆炸伤害与自伤边界） + runClient 截图。
停止条件：弹药耗尽行为符合确认后的再装填决策；不破坏方块（默认）。

## M7a HUD 平视显示（独立切片：新子系统，无参考先例）

拥有文件：
    client/LittleBirdHud.java, client/LittleBirdHudLayout.java,
    network/LittleBirdHitFeedbackPacket.java,
    resources: assets/littlebird/lang/zh_cn.json, en_us.json（HUD 文案与按键名）
规则：布局计算放 LittleBirdHudLayout 纯规则类（不 import Minecraft）以便单测；
    渲染层只做 fill/blit/drawString，且每帧不新建对象。
验证：./gradlew test --tests "*HudLayout*"；再 runClient 出图（GUI scale 1/2/3、F1、四种风格、过热、空巢、20% 耐久）。
停止条件：只在驾驶时出现、F1 能隐藏、文字不溢出、中央视野不被遮挡。

## M7b 血量 / 坠机 / 配置 / 本地化（独立切片）

拥有文件：
    entity/LittleBirdEntity.java（坠机与血量结算改造）, network/LittleBirdImpactPacket.java,
    LittleBirdConfig.java, LittleBird.java（配置注册）
    resources: lang 补全, META-INF/mods.toml（描述）
验证：runClient 撞墙坠机截图；改配置后手感/耐久变化；语言文件无缺失键。
停止条件：坠机伤害可复现且不重复结算；配置项全部生效。

## M8 集成与交付（根代理）

1. 读最终 diff，检查遗漏、重复注册、过期资源、缺失翻译键、时序错误、客户端/服务器边界。
2. ./gradlew build 全量通过（资源校验 + 测试 + reobf）。
3. runClient / runServer 最终一轮验证并按批次请求截图。
4. JAR 同步到本地测试实例并核对时间戳 + 大小/哈希。
5. 强制自审：逐条对照用户需求（双联机枪 / 70mm 火箭巢 / BF6 鼠标偏航飞控）与 flight-control.md 常量表，
   输出「已验证 / 仍需游戏内观察」清单。构建成功不等于视觉验证通过。

## 可选后续（第二版，需用户点头）

- M9 副驾驶 + 两侧舱门机枪手；M10 热诱饵弹与锁定告警；M11 与 Final Paradox 联动（在竞技场刷载具）。
