# 喷气机（F-35E Panther / Su-57 Felon）— 规划与设计 v0.1

> 状态：**设计阶段，本轮没有任何 Java / 资源 / 构建改动**。
> 上位文档：docs/plan/bf2042-vehicle-expansion.md（选型与波次）；本文是「喷气机」这一项的落地设计。
> 用户目标：先做喷气机，尽量还原战地风云 2042。
> 资料来自 fandom wiki（经 r.jina.ai 抓取）与既有子代理调研，外链见 §10；外部文本只作资料。

---

## 1. 还原口径：什么能做、什么只能近似、什么不做

### 1.1 可以做（BF2042 味）
- 固定翼飞行 + 节流阀 + 加力（Shift）：速度与机动分离，加力更快但更钝。
- **F-35E 的 VTOL/悬停切换**（BF2042 用 Prone 键切换前飞/悬停；本模组用一个自定义键）。
- 单座、追尾第三人称 + 座舱视角。
- 机炮 25mm（默认，高射速）/ 30mm（高伤、低射速、更快过热）。
- 副武器选择器：IR 锁定弹（AIM-9Z / AA-11）、空对地导弹（AGM）、雷达弹（AIM-260R / AA-12）、Hydra-70 火箭巢。
- 热诱饵（Missile Countermeasures，约 14s 冷却）+ 被锁定的告警/脱锁。
- Air Radar（空中目标小地图标记）。
- 机炮过热、导弹冷却/备弹、HUD 的速度/高度/节流阀/加力/武器/诱饵状态。

### 1.2 只能近似（必须声明）
- 射程：BF2042 直升机/喷气机反载具导弹统一 800m；MC 视距与渲染距离装不下，改为 96-128 格，配置可调。
- 系统级损坏、Repair Tool、Cyberwarfare、精确的 6 自由度气动、加力的真实推力曲线，都不做 1:1。
- 服务端 20 tick/s：所有速度以「格/tick」标定，HUD 再换算 km/h。
- 机炮是 hitscan，不是逐发弹丸；火箭用现有投射物实体。
- 不做账号解锁/战斗通行证；继续创造栏 + 生存打造链。

### 1.3 明确不做
- 海军、Portal 载具、专家系统、128 人、地形破坏。
- 任何 EA/DICE 模型/贴图/音效提取；资产继续程序化生成。

---

## 2. 现有底子：复用与缺口

| 子系统 | 可复用 | 证据 | 喷气机的缺口 |
| --- | --- | --- | --- |
| 飞行 | HelicopterFlightModel 四轴纯规则 + 每机 FlightConfig | entity/flight/HelicopterFlightModel.java；ApacheFlightProfile.java | **需要全新固定翼模型**（节流阀/升力/失速/加力） |
| 输入 | 骑乘时鼠标已转玩家 yRot/xRot；按键走原版 keyUp/Down/Left/Right | client/LittleBirdInput.java:24-28,71-83 | **鼠标 X 要变成滚转**，需要新的采集方式（§4） |
| 同步 | LittleBirdControlPacket 单独同步滚转角（不在载具位移包内） | network/LittleBirdControlPacket.java:15 | 加力/VTOL 状态要一并同步 |
| 武器 | 30mm 链炮 hitscan、80mm 火箭实体、陶氏 SACLOS 导弹、弹仓/过热 | entity/ground/GroundWeapons.java；entity/weapon/* | **锁定导弹 + 热诱饵**是全新的 |
| 承伤 | AirframeTargets 分类 + 命中反馈 + 坠机结算 | entity/AirframeTargets.java:18-26 | 新增 airframe 类型（建议改接口，见 §9 决策 6） |
| HUD | GunshipHudDraw 共享绘制、HudMath/HudLayout | client/GunshipHudDraw.java:19 | 需要喷气机专用面板（速度/节流阀/加力/锁定框） |
| 网络 | SimpleChannel 编号 0-10 | network/ModNetwork.java:23-81 | 继续递增 |
| 注册/配置/数据 | ModEntities/ModItems/ModCreativeTabs/ClientModEvents/LittleBirdConfig/datagen | 见审计表 | 机械追加 |

结论：喷气机是**本规划里最大的一项**，核心新增只有两处——**固定翼纯规则飞行模型**和**鼠标滚转输入**；锁定导弹+热诱饵是第二条独立子系统，可先做武器后补。

---

## 3. BF2042 喷气机事实（资料）

| 项 | F-35E Panther（美） | Su-57 Felon（俄） |
| --- | --- | --- |
| 座位 | 1 | 1 |
| 主武器 | 25mm Cannon（默认）→ 30mm Cannon（伤害更高、射速/弹速更低、过热更快） | 25mm Cannon（默认）→ 30mm Cannon（同上） |
| 副武器 | AIM-9Z IR 导弹（默认，AA 锁定、发射后不管）→ AGM（对地/激光指示）→ AIM-260R 雷达弹（可无锁发射、自导引飞机）→ Hydra-70 火箭巢 | AA-11 IR 导弹（默认，锁定发射后不管）→ AGM → AA-12 雷达弹 → Hydra-70 火箭巢 |
| 特殊 | **VTOL/悬停切换**（Prone 键），悬停更慢更脆弱；转向半径较大、反应快 | 无 VTOL，机动性更好 |
| 装备 | Missile Countermeasures（默认）、Repair System、Cyberwarfare；Air Radar | 同左 |
| 加力 | 按冲刺键开启，速度更快但机动性下降（系列通例） | 同左 |

来源：F-35 页、Su-57 Felon 页、Fighter Aircraft 页、Flight Mode 页（含「F-35E 与 MV-38 用 Prone 键切换前飞/悬停、W/S 为节流阀」）见 §10。

---

## 4. 控制方案（最关键的一处设计分叉）

现有直升机：鼠标 X = 偏航、鼠标 Y = 俯仰、A/D = 滚转、W/S = 总距。
BF2042 喷气机：鼠标 X = 滚转（压坡度）、鼠标 Y = 俯仰、A/D = 方向舵、W/S = 节流阀、Shift = 加力。

推荐**按 BF2042 口径**（鼠标滚转），否则「还原」无从谈起：

| 输入 | 映射 | 实现要点 |
| --- | --- | --- |
| 鼠标 X | 滚转率指令 | 每 tick 取玩家 yRot 增量 Δ，rollInput = clamp(Δ / 单 tick 最大鼠标角度)；随后把玩家 yRot 锚定回机体航向，避免相机跟着鼠标乱转 |
| 鼠标 Y | 俯仰指令 | 直接复用玩家 xRot（与直升机一致） |
| A / D | 方向舵偏航 | 原版 keyLeft/keyRight，直接偏航（低速时权限高） |
| W / S | 节流阀 + / - | 按住渐进，节流阀是**状态量**（0..1），不是瞬时速度 |
| Shift（冲刺） | 加力 | 节流阀高位时生效；更快、转向权限略降 |
| 左键 | 开火 | 当前武器槽 |
| 1..4 | 切武器槽 | 机炮 / IR 弹 / 对地弹 / 火箭 |
| C | 座舱/追尾 | 复用现有 COCKPIT 键 |
| X | 下机 | 复用现有 EXIT 键 |
| V | VTOL 前飞/悬停（仅 F-35E） | 新增 FLIGHT_MODE 键，默认 V（喷气机单座，与换座键不冲突） |

备选：配置项 jet_mouse_roll 默认 true；设 false 退回直升机式「鼠标偏航」，照顾手感一致性。**这条需要你拍板**（§9 决策 1）。

---

## 5. 固定翼纯规则飞行模型（J0 的核心）

新增、且不含任何 Minecraft 类，全部可 JUnit：

    entity/flight/JetState.java     状态：yaw/pitch/roll、vx/vy/vz、throttle、afterburner、vtol
    entity/flight/JetInput.java     一 tick 意图：pitchInput、rollInput、yawInput、throttleDelta、afterburner、vtolToggle、onGround
    entity/flight/JetConfig.java    可调参数（纯数据）
    entity/flight/JetFlightModel.java  核心解算
    entity/flight/F35Profile.java / Su57Profile.java  两机预设

物理（街机固定翼，逐条可测）：
1. 节流阀：throttle 向目标渐进（throttleRate），W/S 增减。
2. 推力：thrust = baseThrust * (throttle + (afterburner ? abBonus : 0))，沿机头方向；加力消耗/回复一个加力条。
3. 升力：lift = liftCoeff * speed^2，方向随滚转倾斜；升力抵消重力后余量用于转弯。
4. 转弯：沿用直升机已验证的公式 rate = g*tan(bank)/v，但 cap 更大且不随速度淡出（固定翼靠机翼转弯）；A/D 叠加少量方向舵偏航。
5. 失速：speed < stallSpeed 时升力崩溃，机头下沉、操纵权限下降；低于 stallSpeed 不能再靠拉杆拉起。
6. 俯仰耦合：爬升掉速、俯冲增速（能量守恒的近似），这是喷气机手感的关键。
7. 加力：推力乘子 + 转向权限乘子 <1（更快但更钝），加力条 drain/regen。
8. VTOL（仅 F-35E）：切到悬停子模型（可复用 HelicopterFlightModel 的悬停参数），此时鼠标 X 回退为偏航；再按 V 回前飞。
9. 坠机：复用 HelicopterFlightModel.isDiveCrash 的判定思路。

预设提案（可配置，进游戏再标定；HUD 换算 1 格/t = 72 km/h）：

| 参数 | F-35E | Su-57 | 参照 |
| --- | --- | --- | --- |
| 巡航极速 | 1.8 格/t（约 130 km/h） | 1.9 | 小鸟 1.39 / 阿帕奇 1.25 |
| 加力极速 | 2.5（约 180 km/h） | 2.7 | 喷气机应明显快于直升机 |
| 基础推力 | 0.030 | 0.032 | 阿帕奇 forwardAccel 0.024 |
| 加力推力加成 | +0.025 | +0.028 | — |
| 刹车/侧滑 | 低速权限高 | 同左 | 沿用直升机侧滑思路 |
| 转弯 cap | 6.0 度/t | 7.0 | 阿帕奇 bankedTurn 5.0 |
| 俯仰率 | 3.0 度/t | 3.4 | 阿帕奇 4.5（直升机更灵） |
| 滚转率 | 6.0 度/t | 7.0 | 阿帕奇 4.5 |
| 失速速度 | 巡航的 35% | 32% | 越低越易失速 |
| 血量 | 100 | 90 | 苏系更脆但更灵活 |

---

## 6. 武器与系统

### 6.1 机炮（复用 hitscan 管线）
- 25mm：高射速、低单发，对空/轻甲有效，不能伤重甲；过热快。
- 30mm：单发更高、射速/弹速更低、过热更快（对应 BF2042 的取舍）。
- 直接复用 GroundWeapons 的直击/装甲价/曳光/热度过热状态机。

### 6.2 副武器选择器
| 槽 | F-35E | Su-57 | 行为 |
| --- | --- | --- | --- |
| IR 弹 | AIM-9Z | AA-11 | 锁定空中目标、发射后不管；热诱饵可脱锁；对空一发重创 |
| 对地弹 | AGM | AGM | 锁定地面载具；复用陶氏级伤害 |
| 雷达弹 | AIM-260R | AA-12 | 可无锁发射、自导引飞机，伤害低、冷却长 |
| 火箭巢 | Hydra-70 | Hydra-70 | 无制导齐射，复用火箭投射物，反轻甲/步兵 |

### 6.3 锁定导弹 + 热诱饵（独立子系统，建议 J4 单独做）
- 纯规则：MissileSeeker（锁定锥角/距离/时间、丢锁、诱饵欺骗概率、过载）+ Countermeasures（诱饵弹数/冷却/生效窗口/脱锁）。
- 实体：LockOnMissileEntity（锁定/追踪/近炸/寿命），或评估继承 ApacheMissileEntity 加锁定模式。
- 网络：锁定状态 S2C（谁锁了谁）、诱饵触发 C2S、命中反馈沿用现有。
- HUD：锁定框 + 告警音 + 诱饵冷却。
- 这套系统做完，未来客机/直升机也能直接挂热诱饵。

### 6.4 其他
- Air Radar：HUD 小地图标记空中目标（后置切片）。
- Repair System：先不做，或复用现有修复链；BF2042 与 APS 共用槽位，本模组可不引入 APS（喷气机没有 APS）。

---

## 7. 实体 / 相机 / HUD / 网络 / 资产

- 实体：单座、客户端权威运动（与小鸟一致），服务端权威武器/承伤。建议一个 FighterJetEntity + JetType(F35/SU57) 变体，避免两份 700 行实体；若坚持「一车一类」，则 F35Entity/Su57Entity 抽公共基类。
- 相机：JetCamera（追尾 + 座舱），FOV 与滚转联动复用 LittleBirdCamera 的 ViewportEvent 写法（client/LittleBirdCamera.java:38-48）。
- HUD：JetHud + JetHudLayout（速度、高度、节流阀条、加力条、武器槽、诱饵、锁定框、弹着准星、空中雷达）。
- 网络：新增 JetControlPacket（节流阀/加力/VTOL/滚转）或扩展现有 LittleBirdControlPacket；武器走现有意图包模式。
- 资产：JetModel（F-35 单发/苏-57 双发；细长机体+后掠翼+座舱）+ 程序化贴图 + 图标；建议补一个 Python 版贴图脚本（现有 apache 贴图是 PowerShell，WSL 需 interop）。
- 音效：新增 jet_engine / afterburner 两个合成音，武器复用现有。

---

## 8. 切片计划（每片 ≤ 8 文件）

| 切片 | 内容 | 文件 | 验证 |
| --- | --- | --- | --- |
| J0 | 固定翼纯规则：JetState/JetInput/JetConfig/JetFlightModel + F35/Su57 预设 + 单测 | 5-7 | 新单测（节流/失速/转弯/加力/VTOL） |
| J1 | 实体：FighterJetEntity + 权威 + 几何 + 注册 + 部署物品 | 5-6 | runServer 生成/骑乘/坠落 |
| J2 | 客户端：JetFlightInput（鼠标滚转）+ JetCamera + 控制包扩展 | 5-6 | runClient 手感 |
| J3 | 武器：25/30mm 机炮 + Hydra-70 火箭巢 | 4-5 | 单测 + 对射 |
| J4 | 锁定导弹 + 热诱饵（纯规则 + 实体 + 包） | 6-8 | 单测 + 联机 |
| J5 | HUD：JetHud + 布局 | 4-5 | 截图 |
| J6 | 模型/贴图/渲染器 | 5 | 多角度截图 |
| J7 | VTOL 模式（仅 F-35E） | 3-4 | 悬停/切换实测 |
| J8 | 数据：配方/成就/语言/创造栏/配置 | 5 | runData + LangFileTest |
| J9 | Su-57 变体（第二个外观/数值预设） | 3-4 | 截图 + 手感 |
| J10 | 集成：平衡标定、全量 build、JAR 同步、自审 | 根代理 | ./gradlew build + 哈希 |

依赖：J0 -> J1 -> J2 -> J3/J5/J6 可并行；J4 依赖 J3 的武器槽；J7 依赖 J2 与 J0 的 VTOL 子模型。

---

## 9. 待确认决策（带推荐默认值）

1. **控制口径**：鼠标 X 做滚转（BF2042 还原，推荐）还是沿用直升机鼠标偏航（手感一致）？——这是最大分叉。
2. **机型范围**：先做 F-35E（含 VTOL，推荐）还是一步到位两台（F-35E + Su-57）？
3. **锁定导弹/热诱饵**：首版就做（推荐，2042 味核心，但 J4 是最重一片）还是先只做机炮+火箭、导弹后补？
4. **速度标定**：接受 §5 提案（巡航 1.8 格/t ≈ 130 km/h、加力 2.5 ≈ 180 km/h）？还是要更快/更慢？
5. **VTOL 键**：默认 V（推荐）/ 其他？
6. **AirframeTargets**：先做一个小改写（新增 airframe 用接口，避免继续 instanceof）？推荐是。

若回复「按推荐来」，我就从 J0 开工：先交出纯规则飞行模型 + 单测，编译与测试全绿后再进 J1。

---

## 9.1 J0 落地状态（已实现）

- 纯规则 6 文件（`entity/flight/`）：`JetState`、`JetInput`、`JetConfig`、`JetFlightModel`、`F35Profile`、`Su57Profile`；测试 `JetFlightModelTest` 17 项。
- 模型：鼠标 X 滚转率指令 + A/D 方向舵；W/S 积分油门（0..1）；加力为能量条，烧干后松键才回充；失速按**总空速**判定、升力按**水平来流**计算（垂直俯冲仍加速）；转弯复用直升机协调转弯公式并按速度收窄。
- 实测校准（单测钉死）：F-35E 巡航 1.8 / 加力 2.5 格每 tick（130 / 180 km/h）；Su-57 1.9 / 2.7（137 / 194 km/h）。油门 0.02/tick；加力耗 0.015、回 0.006；失速 0.35·maxSpeed；默认正稳（松杆 0.8°/t 回平）。
- 验证：`./gradlew build` 成功；17/17 新测试通过；JAR `battlefield-0.1.0.jar` sha256 `5fcb226d…9872` 与实例一致，实例仅一份 JAR。
- 待实测：转弯半径、加力时长、失速可见度需进游戏看；J0 尚未接入实体，飞机还不可生成。

## 9.2 J1 / J2 落地状态（已实现）

- **J1 实体与注册**：`JetEntity`（单座；客户端权威飞行；`JetControlPacket` 同步滚转/油门/加力/垂直起降；受击、坠毁、打包、存读档）、`JetGeometry`、`JetDeployItem`、`JetGeometryTest` 7 项；注册 `battlefield:f35` 实体与物品并加入创造标签。
- **J2 客户端**：`JetInput`（W/S 油门、A/D 方向舵、Shift 加力、V 垂直起降、C 座舱、X 离机；鼠标 Y 俯仰、鼠标 X 滚转由实体按 yaw delta 采样并回锚）、`JetCamera`（FOV + 滚转）、`JetRenderer`（占位，模型在 J6）；`CameraDistanceMixin` 为喷气机拉远第三人称；新增 `VTOL` 键默认 V，阿帕奇换座改默认 H。
- **验证**：`./gradlew build` 成功；JAR sha256 `d2b7e679…f97e` 与 PCL2 实例一致；实例仅一份 battlefield JAR，无 littlebird 残留。
- **待实测**：鼠标滚转灵敏度（`ROLL_MOUSE_FULL_DEG=6°`）、第三人称距离（暂复用阿帕奇配置）、起飞与失速手感。

## 9.3 J3 落地状态（已实现）

- 纯规则 `JetCannon`（25mm/30mm 共用一个状态机：预热、射速、热量、过热锁定与冷却），`JetCannonTest` 5 项。
- `JetEntity` 武器：武器槽（机炮/火箭）；机炮命中走 `GroundWeapons.fireHitscan`（曳光/装甲价/命中反馈）；Hydra-70 复用 `LittleBirdRocketEntity` 与 `RocketPodState`；弹药由弹药箱补给，R 键从备弹补挂架。
- 客户端：`JetInput` 左键开火、1/2 选槽、R 补弹；`LittleBirdWeaponInputPacket`/`ApacheReloadPacket` 增加 JetEntity 分派。
- 验证：`./gradlew build` 成功；JAR sha256 `4aebc6f6…1ed5` 与实例一致。
- 待实测：25mm 伤害/射速、Hydra 散布、对射平衡；现有武器命中喷气机时 `AirframeTargets` 仍按 NONE 计价（J4 统一为空天目标价）。

## 9.4 J4a 落地状态（纯规则，已实现）

- `TargetLock`（锁定窗/换靶重置/丢锁宽限）、`Countermeasures`（诱饵数/冷却/生效窗/欺骗概率）、`MissileGuidance`（限转率比例导引 + 提前量），共 10 项单测。
- `AirframeTargets` 增加 `FIGHTER_JET`；喷气机现按"空天目标价"计（暂与小鸟同价），TOW 直击秒杀仅限旋翼。
- 验证：`./gradlew build` 成功；JAR sha256 `37ae671f…c816`。
- 余下 J4b：`LockOnMissileEntity` + 注册/渲染 + 喷气机导弹槽与热诱饵键 + 网络包 + 锁定同步。

## 9.5 J4b 落地状态（导弹/热诱饵实体，已实现）

- `LockOnMissileEntity`：继承 `LittleBirdRocketEntity`，复用其弹道/曳光/命中/爆炸管线，仅覆盖 `steer()` 做 `MissileGuidance` 寻的；目标在诱饵窗口内时被欺骗脱锁后转无制导。注册 `lock_on_missile`（追踪 32），渲染暂借火箭模型。
- 喷气机：新增槽位 3 = AIM-9Z 红外弹（6 发）；服务端 96 格/约 26° 锥角自动锁定，`TargetLock.IR` 1.5s 成锁；`JetFlarePacket` + F 键投放热诱饵（8 发，1s 冷却，1.5s 窗口，60% 欺骗）；弹药箱补给导弹与诱饵。
- 同步数据：导弹余量/诱饵数/锁定目标 id/锁定进度/已锁定，供 J5 HUD 直接使用。
- 验证：`./gradlew build` 成功；JAR sha256 `35db3e12…9dc0`。
- 待实测：锁定手感、诱饵欺骗率、导弹命中判定与伤害（对空 24）；数值暂用常量，J8 进配置。

## 9.6 J4c 落地状态（AGM + 雷达弹，已实现）

- `MissileKind` 枚举统一三种副武器：IR（可诱饵欺骗）/ AGM（锁地面车辆、免疫诱饵）/ RADAR（无锁可发、飞行中自搜索）。同一 `LockOnMissileEntity` 按 kind 选导引与战斗部。
- 喷气机槽位补齐：1=25mm、2=Hydra-70、3=AIM-9Z、4=AGM、5=AIM-260R；共用 6 发弹池。锁定制按槽切换目标集/距离/锥角/锁定窗；雷达弹允许未成锁发射。
- `AirframeTargets.isGroundVehicle`（M1A5 / Wildcat）供 AGM 使用。
- 验证：`./gradlew build` 成功（含 `MissileKindTest` 4 项）；JAR sha256 `2e5a94b9…e9b3`。
- 待平衡：AGM 对坦克 14 伤害（约需 9 发）与 6 发弹池偏低，J10 统一标定。

## 9.7 J5 落地状态（JetHud，已实现）

- `JetHud` 复用 `GunshipHudDraw`（中心姿态带/准星/命中标记/敌机血条/告警）：右下 230×108 面板含武器名、弹药、5 个槽位、机炮热量条、节流阀条、加力条、诱饵与机体；锁定时画中心方括号 + LOCK，未成锁画进度条。
- `JetEntity.lockAcquireTicksFor(weapon)` 与 `cannonMaxHeat()` 供 HUD 读取；`ClientModEvents` 注册 `jet_hud`；中英文各补 12 个 HUD 文案键。
- 验证：`./gradlew build` 成功（LangFileTest 通过）；JAR sha256 `7736a2a4…b2d8`。
- 待实测：需在游戏内截图确认面板不重叠、锁定框与真机模型对位（J6 模型后一并看）。

## 9.8 J6 落地状态（F-35E 模型/贴图/渲染器，已实现）

- `JetModel`：16 单位/块空间内 26 个盒体——尖锥机头、菱形机翼、外倾双垂尾、单发喷口、座舱盖、进气道、翼下挂架与 Hydra 吊舱；`ClientModEvents` 注册 `JET_LAYER`。
- `JetRenderer`：真实模型 + 共享旋转约定（yaw/pitch/roll），替换占位渲染器。
- 贴图：新增 WSL 原生脚本 `tools/generate_jet_texture.py`（无 pwsh，纯 zlib 写 PNG）生成 512×512 分区调色板 `f35.png`（2382 字节）。
- 验证：`./gradlew build` 成功；JAR 内含 `JetModel.class` 与 `f35.png`；JAR sha256 `b1b3223d…446a`。
- 待实测：需在游戏内截图核对比例/朝向/垂尾倾角与第三人称取景（`JetCamera`）。

## 9.9 J7 落地状态（VTOL 细化，已实现）

- `JetConfig` 新增 `vtolMaxHorizontalSpeed`（默认 0.45 格/刻）：VTOL 悬停不再被矢量推力慢慢加速到巡航速度，真正"悬"得住。
- `JetModel` 喷口改为绕自身中心枢轴的下摆部件，`setVtol(true)` 时下摆 82°，`JetRenderer` 按 `isRenderVtol()` 驱动，切换可见。
- 单测：`vtolHoverHoldsAltitudeAtMidThrottle`（半油门悬停垂速≈0）、`vtolCapsHorizontalSpeedToTheHoverLimit`（悬停水平速受上限约束），共 2 项；`JetFlightModelTest` 19 项全绿。
- 验证：`./gradlew build` 成功；JAR sha256 `071d1527…17a0`。
- 待实测：游戏内 VTOL 切换过渡手感、悬停操纵与喷口动画。

## 9.10 J8 落地状态（数据/配置，已实现）

- 合成：`ModRecipeProvider` 新增 F-35E 无序合成（4×机架段 + 2×发动机 + 航电 + 座舱玻璃 + 2×挂架 + 瞄准计算机）；`runData` 生成 `recipes/f35.json` 与解锁进度。
- 物品：`models/item/f35.json` + 16×16 图标 `textures/item/f35.png`（同一 WSL 脚本生成）；成就 `first_f35.json` + 中英文文案（创造栏 J1 已收）。
- 配置：新增 `jet_third_person_distance`（11 格）、`jet_flare_count`、`jet_lock_range_blocks`；喷气机第三人称改用独立距离，热诱饵/锁定距离读取配置。
- 验证：`./gradlew runData` 与 `./gradlew build` 成功；JAR sha256 `dd2395c5…b44a`，与 PCL2 实例一致。
- 说明：导弹战斗部数值仍在 `MissileKind` 常量中，留待 J10 平衡时按需进配置。

## 9.11 J9 落地状态（Su-57 变体，已实现）

- `JetType` 枚举收敛差异：F-35E（`F35Profile` + 25mm + 支持 VTOL）与 Su-57（`Su57Profile` + `SU57_30MM` + 无 VTOL）；`JetEntity.jetType()/flightConfig()` 取代硬编码 `F35Profile`，Su-57 由 `Su57Entity` 覆写。
- VTOL 按机型门控：`setClientVtol` 与 `applyClientControl` 对 `supportsVtol()==false` 的机型忽略切换。
- 渲染：新增 `Su57Model`（放宽双发机身、双喷口、外张双垂尾）与 `Su57Renderer`；`ClientModEvents` 注册 `SU57_LAYER` 与渲染器。
- 物品/数据：`ModItems.SU57`（`JetDeployItem` 改为按实体类型部署，F35 同步改造）、创造栏、`su57` 配方与成就、中英文案、16×16 图标与 512×512 `su57.png`（同脚本生成）。
- 验证：`./gradlew runData` + `build` 成功；JAR 含 `Su57Entity/Su57Model/Su57Renderer.class` 与 `su57` 配方/成就/贴图；sha256 `53ff9f34…92c4` 与 PCL2 实例一致。
- 待实测：Su-57 与 F-35E 的差异手感、两套模型外观、VTOL 门控。

## 9.12 J10 落地状态（集成与平衡，已实现）

- 平衡：IR 10/30、AGM 45/16（对 120 HP 坦克 3 发击杀）、RADAR 8/26；机炮对装甲 2→4、对软目标 6→8。
- 自审：`JetRenderer`/`JetModel` 无占位标记；`./gradlew test` 全绿；`runData` + `build` 成功。
- 全量构建 + `syncLocalMods` 完成，JAR sha256 `9477412c…dd02f`，实例仅一份 battlefield JAR。
- 遗留（非阻塞）：`instanceof JetEntity` 硬编码未抽象（记入 implementation-status §64.5）；导弹战斗部未进 config。

## 9.13 起飞修复（地面失速低头，已修复）

- 根因：`JetFlightModel.step` 的失速低头分支既不检查 `onGround`、也不接受飞行员俯仰输入。F-35 失速阈值 = 1.8×0.35 = 0.63 格/刻，地面静止 `airspeed=0` 时每刻 +2°，直到 `pitchMaxDeg=90` **正好竖直朝下**；随后 `cos(90°)=0` 使水平推力归零，速度到不了失速阈值 → 永久竖直朝下，无法起飞（无 VTOL 的 Su-57 完全无法起飞）。
- 修复 1：失速低头仅在 `!in.onGround()` 时生效；地面保持姿态、保留完整俯仰权限，可拉杆抬头滑跑起飞。
- 修复 2：`JetEntity.applyLocalFlight` 在首次接管（`clientState==null`）时 `pilot.setXRot(0)`，不再把放置时的低头视线变成瞬间低头指令。
- 单测：新增 3 项（地面零速不低头 / 地面拉杆抬头 >30° / 空中低空速仍低头），`JetFlightModelTest` 22 项全绿。
- 验证：`./gradlew test` + `build` 成功；JAR sha256 `2dab90c5…f4d9`，与 PCL2 实例一致。
- 待实测：真实滑跑、离地、复飞手感。

## 9.14 手感修复（俯仰-速度能量耦合 + 标定，已修复）

- 根因：计划 §5.6 要求的"爬升掉速、俯冲增速"从未实现；原 `noseDownDiveBleedsForwardSpeed` 测试反而把"俯冲掉速"钉成预期。推进沿机头分解、阻力只作用水平 → 任何非平飞姿态都掉速，只有平飞保速。
- 修复：`JetConfig` 新增 `pitchSpeedCoupling`（默认 0.7）；前飞分支把重力沿机头分量加到水平速度（俯冲增速、爬升掉速），并以 `capScale` 让速度上限随俯仰浮动（俯冲可短时超巡航、爬升上限降低）。
- 标定：F-35E 俯仰率 3.0→5.0 度/刻、Su-57 3.4→5.6；鼠标滚转满舵阈值 `ROLL_MOUSE_FULL_DEG` 6°→10°（降低过敏）。
- 单测：`noseDownDiveBleedsForwardSpeed` 改为 `diveGainsForwardSpeedPastTheCruiseCap`，新增 `sustainedClimbBleedsForwardSpeed`；`JetFlightModelTest` 23 项全绿。
- 验证：`./gradlew test` + `build` 成功；JAR sha256 `509ba42f…c061`，与 PCL2 实例一致。
- 待实测：能量手感（俯冲加速/爬升掉速）、俯仰跟手度、滚转灵敏度。

## 9.15 机动改造：俯仰驱动转弯（已实施，2026-09-14）

- 调研：见 `docs/plan/bf2042-jet-flight-reference.md`。BF2042 是"滚转→拉杆"的**街机高角速率**模型；**角点速度**中速转弯最快；失速是低速深层状态、恢复慢。
- 根因：旧模型转弯只看坡度与速度、俯仰不参与；按俯仰**瞬时钳**速度上限；升力只看水平速度，拉杆反而抽干升力；且转弯只转机头、**速度矢量不跟机头**，快转时推力方向互相抵消把速度拖垮 → 压坡度拉杆 2s 内必失速。
- 改造（`JetFlightModel`）：
  1. 俯仰按坡度分解：目标俯仰 ×`cos(roll)`，90° 坡度拉杆变成平转，平飞拉杆才是筋斗；
  2. `turnRateDegPerTick(flow, roll, pull, cfg)`：坡度定转弯平面、拉杆放大角速率、**角点速度**取峰（低速掉舵效、高速转弯半径变大）；
  3. 拉杆并入升力 `support = lift*clamp01(cos(roll) + pullSupport*pull)`，坡度中拉杆保高；
  4. 去掉向下钳制，代价改为**诱导阻力** `horizontalDrag + inducedDrag*pull`；俯冲仅上调上限（`diveOverspeed`）；
  5. **速度矢量按本 tick 偏航增量跟随新机头**，否则快转时推力互相抵消。
- 参数：F-35E 转弯 cap 6→7、Su-57 7→7.5；失速 0.35/0.32→0.30/0.28；新增 `cornerSpeedFraction 0.55`、`turnBaseAuthority 0.30`、`pullTurnGain 0.70`、`pullSupport 1.0`、`inducedDrag 0.01`、`diveOverspeed 0.35`、`pitchSpeedCoupling 0.7→0.5`。
- 单测：`bankedTurnGrowsWithBankAndFallsWithSpeed` 改为角点版；新增 `bankedPullTurnsTightlyWithoutStalling`（90° 坡度拉杆 40 刻不失速、转角 >100°）；`JetFlightModelTest` 24 项全绿；全量 `build` 成功，JAR sha256 `22e7234a…22c7` 与实例一致。
- 探针：60° 坡度 -0.8 拉杆 40 刻转 165°、速度稳在 0.77 不失速；80° 坡度稳在 1.3。
- 待实测：转弯速率/角点速度/掉速力度、60° 坡度拉杆会爬升是否可接受。

## 9.16 跟随视角（相机俯仰锚定机体，已实施，2026-09-14）

- 现象：第三人称"视角很差"——相机偏航跟机头（`pilot.setYRot` 已锚定），但**俯仰跟鼠标**：`JetEntity.applyLocalFlight` 从未锚定 `pilot.setXRot`，`JetCamera` 只补了滚转，于是视点与机体脱节。
- 修复：鼠标 Y 改成**俯仰位移累加**（`pitchCommandDeg`，±90° 钳制）；每 tick 取 `pilot.getXRot() - state.pitchDeg()` 作为纯鼠标位移，步进后 `pilot.setXRot(next.pitchDeg())` 把视点锚回机体。相机 pitch/yaw 均跟机头、roll 由 `JetCamera` 叠加，形成真正的追尾跟随视角；座舱视角同样沿机头看。
- 验证：全量 `build` 成功，24 项飞行单测保持全绿，JAR sha256 `46960311…f6f6` 与实例一致。
- 待实测：第三人称距离（11 格）/FOV 倍率/`cameraRollFactor` 是否合适；鼠标俯仰灵敏度（满杆 = 鼠标移动 90°）。

## 9.17 俯仰反转设置（已实施，2026-09-14）

- 需求：加入俯仰反转，并允许在设置里开关。
- 设置：`LittleBirdConfig` 新增 CLIENT 布尔 `jet_invert_pitch`（默认 false），访问器 `jetInvertPitch()` 与写回配置的 `toggleJetInvertPitch()`（`CLIENT_SPEC.save()`）——在 Forge 配置界面（Mods → Battlefield → Config，或 `config/battlefield-client.toml`）里可勾选。
- 热键：`LittleBirdKeyMappings.INVERT_PITCH`（默认 `I`，可在"按键绑定 → 战场载具"改键），全局生效（不必驾驶中），切换时动作栏提示，并把结果写回配置。
- 生效：`JetEntity.applyLocalFlight` 对**鼠标位移**取反（`pitchDelta = -pitchDelta`），只翻转后续行程、不改已累积指令，因此飞行中切换不会让机头跳变。默认关闭 = 现状（鼠标上推抬头）。
- 验证：全量 `build` 成功；语言键 en/zh 各 111 且集合一致；JAR sha256 `fa0eb411…fedc` 与实例一致。
- 待实测：反转手感；热键 `I` 是否与其他模组冲突。

## 9.18 修复：滚转时跟随视角脱离（已实施，2026-09-14）

- 现象：滚转时跟随视角脱离/回弹。
- 根因（已定）：第三人称相机的朝向与位置都取自**玩家**的 `getViewYRot/getViewXRot(partialTick)`（`GameRenderer` 在 `camera.setup(...)` 里取），而鼠标每帧都会写 `pilot.yRot/xRot`（`MouseHandler` → `Player.turn`）；`JetEntity.applyLocalFlight` 每 **tick** 才把视点锚回机体一次（`JetEntity:335-350`），`JetCamera` 只叠加 roll、从不改写 yaw/pitch（`JetCamera:35-45`）。于是非 tick 帧相机跟着**原始鼠标**走、下一次 tick 再弹回。滚转用鼠标横轴，一次快甩的原始 yaw 远超滚转指令（指令被 `ROLL_MOUSE_FULL_DEG` 限幅，相机不限幅），在 11 格追尾距离（`CameraDistanceMixin` + `jet_third_person_distance=11`）下 10° 偏差≈1.9 格、30°≈5.7 格 → 飞机明显脱出画面再弹回。
- 排除：Forge 的 roll 只在 `camera.setup` **之后**加到视图矩阵（`GameRenderer.java.patch` 第 105-110 行 `camera.setAnglesInternal(...)` + `Axis.ZP.rotationDegrees(getRoll())`），不参与相机定位，所以不是滚转轴本身。
- 次要：roll 未做 partialTick 插值（`JetRenderer:44-46`、`JetCamera:43`），20Hz 步进。
- 已实施（仅喷气机）：① 新增 `JetEntity.anchorPilotView(Player)`，由 `CameraDistanceMixin` 注入 `Camera.setup` HEAD（相机定位之前）每帧调用，把机体的 `yRot/yRotO`、`xRot/xRotO` **端点对端点拷贝**到飞行员，相机朝向与位置从此只看机体、不看鼠标，且不双重插值；② 鼠标位移改为**每帧累加到 `pendingMouseYaw/Pitch`、每 tick 消费一次**，`applyLocalFlight` 不再读玩家视点（否则机头转动会被当成鼠标位移）；③ 新增 `interpolatedRenderRollDeg(partialTick)`（上一 tick roll→当前），`JetRenderer` 与 `JetCamera` 一起按 partialTick 插值，去掉 20Hz 步进。
- 验证：全量 `build` 成功，mixin refmap 命中 `Camera.setup → m_90575_`，24 项飞行单测全绿，JAR sha256 `02e724b2…5a30` 与实例一致。
- 待实测：滚转/大机动时相机是否完全跟随、操纵延迟与额度是否与改前一致；倒飞视角（可调 `camera_roll_factor`）；玩家模型自身旋转是否因端点拷贝而抖动。Apache/Little Bird 尚未套用。

## 9.19 修复：起飞滑跑抽搐抖动（已实施，2026-09-14）

- 现象：刚上机、地面加油门准备起飞时，机头/机体快速抽搐抖动。
- 根因（已用字节码确认）：`JetFlightModel.step` 在地面把 `vy` 归零（旧 `if (in.onGround() && vy < 0) vy = 0`），而原版 `Entity.move` 用 `setOnGroundWithKnownMovement(verticalCollisionBelow, y)` 设置 `onGround`，`verticalCollisionBelow = verticalCollision && pos.y < 0` —— 竖直位移恰为 0 时 `onGround` 被清成 false（`Entity.class` 反编译：`move` 偏移 371-414 调用 `setOnGroundWithKnownMovement`，后者偏移 0-2 直接 `putfield onGround`）。于是地面每隔一 tick 一次 `onGround=false`，§9.13 那个只在 `!onGround` 生效的失速低头分支在地面交替触发 → 抽搐。
- 修复：地面保留向下微探针 `GROUND_CONTACT_PROBE = 0.05`，`if (in.onGround() && vy <= 0) vy = -probe`；`move` 的碰撞自行把它归零，位置不下沉，而 `onGround` 每 tick 都为真。向上请求（抬轮起飞、悬停爬升）不受影响。
- 新增单测：`groundedJetKeepsADownwardProbeForGroundDetection`（60 刻地面，断言 `vy<0` 且 `>-0.2`、机头保持水平）。
- 验证：全量 `build` 成功，飞行单测 25 项全绿，JAR sha256 `8f310c27…a5e4` 与实例一致、仅一份。地面静止/滑跑是否彻底不抖、抬轮是否正常待实测。

## 9.20 喷气机鼠标偏航/滚转切换开关（已实施，2026-09-14）

- 需求：把鼠标横轴在“滚转”和“偏航”之间切换。
- 实现：客户端配置 `jet_mouse_yaw`（默认 false = BF 预设：鼠标滚转、A/D 偏航）；按键 `Y`（`key.battlefield.mouse_yaw`）切换并写回配置，屏幕提示“偏航（A/D 滚转）/ 滚转（A/D 偏航）”。换轴逻辑抽成纯规则 `flight/JetAxes.of(mouseAxis, keyAxis, mouseYaw)`，`JetEntity.applyLocalFlight` 只调用一次；俯仰轴不受影响（与 §9.17 反转互相独立）。
- 直升机（小鸟/阿帕奇）本就是鼠标偏航 + A/D 滚转（`LittleBirdInput`：鼠标横轴由原版转向 yaw、A/D 走 `CONTROL_ROLL_*`），本次未改；若也要它们能切到鼠标滚转需另做（它们没有喷气机那套每帧锚定来累计鼠标横移）。
- 验证：`JetAxesTest` 2 项；全量 build 成功、376 测试 0 失败；JAR sha256 `d2a85d5f…0372` 与实例一致、仅一份。
- 待实测：两种手感的主观对比、`Y` 键是否与其他模组冲突。

## 9.21 两架固定翼视觉升级（已实施，2026-09-14）

- 贴图：`tools/generate_jet_texture.py` 重写为细节绘制，6 个 UV 分区矩形不变；F-35 6→478 色、Su-57 6→573 色（面板线/铆钉/迷彩/做旧/低可视标识/座舱反光/喷口热变色/挂架喷字）。
- 几何：`JetModel` / `Su57Model` 各加一批细节件（见 implementation-status 第 64.22 节），`JetGeometry` 常量与 `JetGeometryTest` 零改动。
- 验证：376 测试 0 失败；JAR sha256 `b08df55c…d602` 与实例一致。

---

## 10. 来源

- F-35：https://battlefield.fandom.com/wiki/F-35
- Su-57 Felon：https://battlefield.fandom.com/wiki/Su-57_Felon
- Fighter Aircraft：https://battlefield.fandom.com/wiki/Fighter_Aircraft
- Flight Mode（F-35E/MV-38 用 Prone 键切换前飞/悬停）：https://battlefield.fandom.com/wiki/Flight_Mode
- Missile Countermeasures：https://battlefield.fandom.com/wiki/Missile_Countermeasures
- Air Radar：https://battlefield.fandom.com/wiki/Air_Radar
- 工程内证据：见 §2 表 file:line；约束见 AGENTS.md、bf2042-vehicle-expansion.md。
