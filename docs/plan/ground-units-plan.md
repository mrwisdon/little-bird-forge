# 地面战斗单位（主战坦克 / 步兵战车）— 勘察与规格草案

状态：**勘察完成，规格待用户确认（未写任何实现代码）**
日期：2026-09-12
参照：用户指定「参考战地风云2042 的主战坦克与步兵战车」；工程既有飞行器范式为唯一实现模板。

## 1. 现状清单（全部来自实际读取的文件，非假设）

| 项 | 结论 | 证据（文件:行） |
| --- | --- | --- |
| 工程/技术栈 | Forge 47.4.20 / MC 1.20.1 / official mappings / Java 17 / Gradle 8.8，modid=**battlefield**，包 `io.github.littlebird` | gradle.properties:2-16；build.gradle |
| 工程位置 | WSL 原生 `/root/projects/little-bird-forge`；测试实例 mods 在 /mnt/c（syncLocalMods） | gradle.properties:local_mods_dir；build.gradle:115-130 |
| 构建/测试基线 | `./gradlew compileJava test` **BUILD SUCCESSFUL**（本次实测）；单测 **247 个 @Test / 33 个测试文件** | 本次 job bash-1 输出；grep -c @Test |
| 既有单位 | 只有**飞行器**：Little Bird（class `LittleBirdEntity`，HP **80**）与 AH-64 Apache（`ApacheEntity`，HP **100**）。**全工程无任何地面/履带载具** | entity/LittleBirdEntity.java:78；entity/ApacheEntity.java:71；grep tank/rover/履带 无命中 |
| 实体范式 | `extends Entity` + `DeferredRegister<EntityType<?>>` + `EntityType.Builder.of(...).sized(aabbW,aabbH).clientTrackingRange(24).updateInterval(1)`；`getAddEntityPacket` 用 `NetworkHooks.getEntitySpawningPacket` | registry/ModEntities.java:19-53；ApacheEntity.java:1145-1147 |
| AABB/尺寸范式 | 每机型一个纯规则 Geometry 类，16 units/block，`aabbWidthBlocks()/aabbHeightBlocks()` | entity/ApacheGeometry.java:17-66 |
| 座位范式 | 纯规则 Seat 类：Apache 2 座（PILOT/GUNNER，V 键交换）；Little Bird **4 座**（PILOT+3 乘客，仅 PILOT 可操作）。乘客用 `canAddPassenger` 限制 | entity/seat/ApacheSeat.java；entity/seat/LittleBirdSeat.java:11-14；LittleBirdEntity.java:847-848 |
| 运动范式 | **客户端权威**：`isControlledByLocalInstance()` 时由驾驶员 yRot 驱动，客户端 `move(MoverType.SELF,...)`；服务器只做校验/武器。无地面摩擦/履带模型 | ApacheEntity.java:514-663；PLAN.md:79-87 |
| 武器范式（纯规则+服务器权威） | 30mm `ChainGunTurret`（炮塔 yaw/pitch 状态机）；`TwoStageMagazine`（待发/备弹）；`AmmoPool`；hitscan 用 `LittleBirdBallistics`；爆炸用 `server.explode(...ExplosionInteraction.NONE)` | entity/weapon/ChainGunTurret.java；TwoStageMagazine.java；ApacheEntity.java:695-1005 |
| 伤害/死亡范式 | `hurt(DamageSource,float)` → `AircraftDestruction` 单次销毁守卫 → 爆炸 + `discard()`；血量存 `EntityDataAccessor<Float> DATA_HEALTH` | ApacheEntity.java:976-1005；AircraftDestruction.java |
| 骑乘保护 | `GunshipRider` 标记接口 + `LittleBirdRiderProtection`（骑乘时免伤） | entity/GunshipRider.java；LittleBirdRiderProtection.java |
| 部署/生成入口 | 每种载具一个 `XxxDeployItem extends Item`，右键在玩家前方 `GunshipPlacementProbe.spawnPosition` 落地并 `addFreshEntity`，物品 NBT 携带 Health，潜行空手右键打包 | item/ApacheDeployItem.java:26-45；item/GunshipPlacementProbe.java |
| 注册入口 | `LittleBird` 构造里注册 ModEntities/ModBlocks/ModItems/ModCreativeTabs/ModSounds + 两个 Config | LittleBird.java:22-35 |
| 创造栏 | 单一 `battlefield:main` 标签，逐条 `output.accept(...)` | registry/ModCreativeTabs.java:15-33 |
| 配置范式 | `LittleBirdConfig`：CLIENT（手感/视效）+ COMMON（`balance` 下每机型一节：伤害/血量/装填）；全部 `defineInRange` 有注释 | LittleBirdConfig.java:177-274 |
| 造价/解锁 | 无货币/阵营系统。生存链：9 个零件 → 中间件 → 整机（datagen 生成 recipes + advancements）；小鸟约 60-70 铁当量，阿帕奇约 150 铁当量+1 钻石 | data/ModRecipeProvider.java:28-127；docs/plan/survival-crafting.md:53-71 |
| 资源约定 | 实体贴图 `textures/entity/<id>.png`（512x512）；物品图标 16x16；音效 `sounds/*.ogg` + `sounds.json`；lang `zh_cn/en_us` 双语；模型走 LayerDefinition（`client/*Model.java`）+ `EntityRenderer` | resources/assets/battlefield/**；client/ClientModEvents.java:26-52 |
| 渲染注册 | `ClientModEvents`：RegisterLayerDefinitions + RegisterRenderers；漏注册渲染器会客户端崩溃（技能明示） | client/ClientModEvents.java:35-52 |
| 网络 | `ModNetwork` SimpleChannel 逐包编号（已用 0-9），PLAY_TO_SERVER 意图包 + PLAY_TO_CLIENT 反馈包 | network/ModNetwork.java:23-76 |
| 测试范式 | 纯规则类**不 import net.minecraft**，JUnit5 单测是验证主干；datagen 产物与 LangFileTest 校验 lang | src/test/java/**；LangFileTest.java |
| 演示/验证方式 | `./gradlew runClient`（需图形栈；本机有 /tmp/.X11-unix/X0，但未装 xdpyinfo/截图工具）与 `./gradlew runServer --nogui`；工程曾用 run/smoketest 世界做服务器冒烟。构建成功≠游戏内验证 | build.gradle:28-47；run/ 目录；AGENTS.md |
| 参考先例（只读，勿改） | sibling 工程 `/mnt/c/.../final-paradox-forge` 有**地面载具** `TerrastalkerRoverEntity.java`（1847 行）：地面移动、炮塔 yaw、炮管 pitch、能量/损毁 | 实测文件存在（wc -l=1847） |

**关键结论：本工程 0 个地面单位、0 套地面运动模型、0 阵营/货币系统。** 因此主战坦克/步兵战车的「同类参照」在工程内不存在，必须按 §3 由用户拍板。

## 2. 战地风云2042 外部证据（用于规格对齐）

来源（只读外部数据）：
- 3楼猫《门户模式载具基础数据》(2022-05-07)：https://game.3loumao.org/98312?language=zh-tw
- 战地2042 BWIKI：M1A5 https://wiki.biligame.com/zhandi2042/M1A5 ；T28 https://wiki.biligame.com/zhandi2042/T28 ；EBAA小野猫 https://wiki.biligame.com/zhandi2042/EBAA_小野猫

| 载具 | 类型 | 火力 | 生存性 | 机动性 | 速度 | 座位 | 主武器 | 副武器 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M1A5（美）/ T28（俄） | 主战坦克 | 80 | 80 | 20 | 15 | 驾驶员/武器站/武器仓/指挥官 | 120mm 稳定滑膛主炮（MPAT/HEAT，5 发） | 同轴轻/重机枪（∞） |
| EBAA 小野猫 | 装甲侦察/步战类 | 70 | 65 | 30 | 15 | 驾驶员/武器站/武器仓/指挥官 | 双联 30mm 机炮（∞）/40mm（∞）/57mm（5） | 防空导弹 2 / 反坦克导弹 2 |
| （对照）AH-64GX | 攻击直升机 | 75 | 70 | 60 | 50 | — | — | — |
| （对照）MD540 夜莺 | 侦查直升机 | 50 | 60 | 80 | 50 | — | — | — |

限制：BF2042 未公开绝对数值（HP/每秒伤害），只有 0-100 抽象评分；**评分到 MC 数值的换算必须由用户确认**。EBAA 在 BWIKI 被归类为「坦克」，用户称其为「步兵战车」，命名口径也需确认。

## 3. 规格草案（数值映射提议，待确认）

映射原则：以工程内 AH-64=100 HP / 火力评分70 为锚点，HP ≈ 评分 ×(100/70)，取整到 5。

| 项 | 主战坦克（M1A5 式） | 步兵战车（EBAA 式） | 工程内对照 |
| --- | --- | --- | --- |
| 血量 | **120**（评分80） | **90**（评分65） | Apache 100 / 小鸟 80 |
| 主武器 | 120mm 主炮：直击 80、对机体 40、溅射半径 3、5 待发，射击间隔约 2.5s | 双联 30mm：8/发、对机体 3、溅射 1、约 600 发单池 | Apache 链炮 8/3/1；TOW 直击 60/机体 20 |
| 副武器 | 同轴机枪：沿用 Apache 链炮数值，300 发 | ATGM 2 发：直击 60、对机体 20 | 陶氏 1 待发/5 备弹 |
| 防护/相克 | 正面吃 30mm 低效（3/发）；被陶氏/主炮重创；被 7.62 几乎免疫 | 机炮可打飞机与步兵；对坦克装甲低效，需 ATGM | 既有：机炮打机体 0.5(小鸟)/3(阿帕奇)，火箭 10/7 |
| 机动 | 履带式，极速约 **0.30 格/t**（6 格/s），转向慢 | 轮式，极速约 **0.45 格/t**（9 格/s），转向快 | Apache 飞行极速 1.3889 格/t |
| 座位 | 4 座（驾驶员/武器站/武器仓/指挥官） | 4 座（同左） | 小鸟 4 座、阿帕奇 2 座 |
| 生成 | 右键部署物品（沿用 ApacheDeployItem），创造栏 + 合成 | 同左 | DeployItem 范式 |
| 音效 | 复用爆炸/机炮音 + 新增引擎音（或复用） | 同左 | ModSounds |

## 4. 已拍板决策（2026-09-12，用户确认）

| # | 决策 | 结论 |
| --- | --- | --- |
| 1 | 命名/参照 | M1A5 主战坦克 + EBAA 小野猫 步战，中性内部 id `main_battle_tank` / `infantry_fighting_vehicle` |
| 2 | 阵营 | 不新增阵营，两车与既有飞机一致对所有人开放 |
| 3 | 运动架构 | 新增地面载具范式：Entity + 客户端权威油门/转向 + 独立炮塔；纯规则 GroundDrive/MainGun + 单测 |
| 4 | 数值 | 接受 §3 草案映射（坦克 120HP / 主炮 80-40 / 极速 0.30；步战 90HP / 双联30mm 8-3 / ATGM 60-20 / 极速 0.45） |
| 5 | 造价 | 沿用生存打造链 + 新增地面零件（IFV≈小鸟级、MBT 含钻石火控）；创造栏同时提供 |
| 6 | 座位 | 坦克 2 座（驾驶员+炮手）；步战 2 座 + 2 载员 = 4 座 |
| 7 | 资产 | 先程序化占位（盒模型 + 纯色纹理 + 复用音效），缺失清单单列 |

实现与证据见 ground-units-evidence.md。

## 5. 原始待确认清单（已由 §4 回答，保留供追溯）

1. 命名/参照：坦克取 M1A5 还是 T28？两车用现实型号名还是中性 `main_battle_tank`/`infantry_fighting_vehicle`？
2. 阵营：工程无阵营系统 —— 是否**不新增**，两车对所有人开放（推荐）？
3. 运动架构：新增「地面载具」范式（Entity + 客户端权威油门/转向 + 独立炮塔，纯规则 TrackedDriveModel+单测，参考 sibling 的 TerrastalkerRover）？还是复用原版船/矿车物理？
4. 数值映射表（§3）：是否接受？HP/主炮伤害/极速/弹药容量哪些要改？
5. 造价/解锁：新增地面零件走生存链（IFV≈小鸟级、MBT≈阿帕奇级以上），还是**仅创造栏**最小改动？
6. 座位/载员：MBT 2 操作座、IFV 2 操作座+2 载员（合计4）？还是 4 座全操作位？还是单座？
7. 资产：无坦克/步战模型贴图 —— 用程序化占位（简单盒模型+纯色/简单纹理+复用音效）并把缺失清单单列？
