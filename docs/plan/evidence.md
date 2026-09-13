# 证据与先例（规划阶段的只读调查结果）

## 1. BF6 直升机四轴与「鼠标偏航」预设（外部资料）

- Thing's Blog《戰地 6 直升機控制：按鍵綁定、靈敏度和核心機動》（2025-10-12）
  https://things.recmg.com/zh-TW/tech/blogs/battlefield-6-helicopter-controls-keybinds-sensitivity-and-core-maneuvers/
  摘录要点（原文为繁体中文，以下为归纳，未改写数值）：
  - 「每架直升機均由四軸驅動」：
    油門（集體）= 垂直上升或下降，鍵盤 W/S；
    偏航（舵）= 左/右旋轉直升機，鍵盤 A/D 或滑鼠（自訂）；
    俯仰 = 將機頭向上/向下指以向前/向後移動，鍵盤滑鼠上下；
    滾轉 = 向左/右傾斜以平移和傾斜轉彎，鍵盤 A/D（自訂）。
  - 建議 PC 改鍵：「把俯仰和偏航放在滑鼠上、滾轉移到 A/D、油門留在 W/S」，
    理由是「把兩個瞄準軸置於一隻手之下，攻擊時對機頭控制更精確」。
  - 另提到靈敏度與死區是關鍵設定，直升機獎勵穩定輸入而不是高靈敏度。
- Steam 社群（BF2042 鍵鼠飛行討論）
  https://steamcommunity.com/app/1517290/discussions/0/3414305211871798807
  摘录：A/D = yaw left/right，方向鍵 = pitch/roll，「Mouse Control for pitch and yaw when aiming」。
  说明「鼠标同时控制俯仰+偏航」在 BF 系列是玩家长期使用的可配置方案，不是 BF6 独有。
- EA 官方空中载具指南（偏喷气机，作为方向性参考，未取到正文数值）：
  https://www.ea.com/zh-hant/games/battlefield/redsec/news/aerial-vehicles-guide-how-to-fly-jets

结论与限制：BF6 未公开飞控数值（角速率/加速度/最大速度），因此 flight-control.md 第 5 节的常量
是「以 BF 四轴语义为骨架 + 手感标定」的首版值，必须在 runClient 里迭代；不得对外声称与 BF6 数值一致。

## 2. 本机工程先例（参考工程 final-paradox-forge，只读，未修改）

| 主题 | 文件:行 | 复用点 |
| --- | --- | --- |
| 客户端权威载具旋转 | entity/DroneEntity.java:455-466 | isControlledByLocalInstance 时用乘客 yRot/xRot 驱动载具，正是「鼠标偏航」的挂接点 |
| 客户端权威位移 | entity/DroneEntity.java:660-682 | setDeltaMovement + move(MoverType.SELF, ...) 的位移写法 |
| 远端插值 | entity/DroneEntity.java:684-711 | 自定义 lerpTo 与 tickRemoteInterpolation，避免其他玩家看到抖动 |
| 纯规则 + 单测 | entity/DroneGatlingCycle.java、DroneGatlingBallistics.java | 明确「不含 Minecraft 类」的纯规则写法，是同款做法的直接模板 |
| 客户端输入与「变化才发」 | client/DroneInput.java:33-95, 137-144 | 只发变化、Exit/开火分包、RenderHand/RenderPlayer 取消 |
| 载具输入与轮询 | client/TerrastalkerRoverInput.java:36-91 | 从 minecraft.options 读原版按键、边沿检测 |
| 武器循环挂载 | entity/TerrastalkerRoverEntity.java:457-499 | 服务器侧 tickControls 驱动射击节奏与冷却 |
| 网络通道注册 | network/ModNetwork.java:24-102 | SimpleChannel 逐包注册（当前已用到 id 12，新工程从 0 重新编号） |
| 渲染与层注册 | client/ClientModEvents.java:26-106 | RegisterKeyMappingsEvent / RegisterRenderers / RegisterLayerDefinitions |
| 实体注册 | registry/ModEntities.java | DeferredRegister + EntityType.Builder 的既有写法 |
| 曳光渲染 | client/DroneTracerRenderer.java、DroneTracerTimeline.java | 客户端曳光时间线渲染 |
| JUnit 测试范式 | src/test/java/.../DroneGatlingCycleTest.java 等 37 个测试 | 纯规则测试的组织与命名 |
| 构建与 JAR 同步 | build.gradle:133-134, 152-154, 216-219 | JUnit 5.10.3、useJUnitPlatform、-Plocal_mods_dir 同步任务 |

## 3. 环境事实（命令输出）
> 以下为 **2026-09-10 迁移前**在 Windows 侧采集的原始输出，保留原样；2026-09-12 起本工程位于 WSL `/root/projects/little-bird-forge`，JDK 为原生 OpenJDK 17，命令用 `./gradlew`。

- pwd = C:\Users\19818\Desktop\newmod
- 参考工程 gradle.properties: minecraft_version=1.20.1、forge_version=47.4.20、mapping_channel=official、mapping_version=1.20.1
- gradle/wrapper/gradle-wrapper.properties: gradle-8.8-bin.zip
- build.gradle: java.toolchain.languageVersion = JavaLanguageVersion.of(17)
- java -version 无输出、JAVA_HOME 为空 → 必须显式使用 C:\Program Files\Java\jdk-17
- C:\Users\19818\AppData\Roaming\.minecraft\mods 当前为空（尚未建立测试实例 JAR 目的地）
- final-paradox-forge/.b6-audio-import 仅含 2 个音乐 ogg，与本载具音效无关

## 4. 尚未验证 / 需要实施阶段确认的事项

1. 自定义实体（非 LivingEntity）在 Forge 1.20.1 中，乘员鼠标旋转驱动载具 yaw 的实际手感
   （参考工程只在 DroneEntity 上验证过，无人机是非载具式骑乘；需要 runClient 实证）。
2. ViewportEvent.ComputeCameraAngles 的 setRoll 在第三人称下的实际效果（未在本机验证过，仅有 Forge API 文档知识）。
3. 高速实体（约 18 格/秒）的追踪与插值观感，需要多人或双客户端验证。
4. 骑乘时拦截原版热键 1/2 的可行性与副作用（原版 KeyMapping 消费路径需实测）。
5. 资产（贴图/音效）来源未定，见 PLAN.md 第 9 节第 7 条。
