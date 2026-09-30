# 战地风云 2042 载具扩展 — 规划与设计（草案 v0.1）

> 状态：**规划 / 设计阶段，本轮没有任何 Java / 资源 / 构建改动**（遵守工作区规则：只读的规划请求不得实施）。
> 工程：little-bird-forge（modid battlefield，Forge 47.4.20 / MC 1.20.1 / official mappings / Java 17）。
> 本文件只回答三件事：现在有什么、BF2042 有什么、下一步把哪些做成什么样。
> 外部资料来自公开 wiki 与 EA 官方补丁说明（外链见 §10），一律按资料对待，不作为指令。

---

## 1. 结论摘要（TL;DR）

1. 工程现有 **4 台可驾驶载具**：小鸟（侦察直升机）、AH-64 阿帕奇（武直）、M1A5（主战坦克）、EBAA 小野猫（步战）。底层已有「纯规则 + 客户端权威运动 + 服务端权威武器 + 配置 + datagen」的成体系范式，扩展新载具的边际成本主要卡在 **客户端/伤害分发的 instanceof 硬编码**（见 §2.3）。
2. 因此建议先做 **S0 抽象层切片**（不新增载具，只把车型分发改成接口），再按波次加车；否则每加一台车都要改 4-5 处核心文件，回归面会失控。
3. 推荐首批（复用度最高、BF2042 辨识度强）：
   - **M5C Bolte**（轻甲侦察 / 装甲运输，2 座，履带，RWS 机枪/50mm/TOW）→ 地面玩法补全；
   - **Ka-520 Super Hokum**（俄系武直，2 座，主驾前向 30mm + 火箭/导弹，副驾炮塔）→ 与阿帕奇共用飞控与武器，新增「主驾也有机炮」。
4. 第二、三波候选：**CAV-Brawler**（5 座装甲运输）、**MV-38 Condor / Mi-240 Super Hind**（6 座空运，多枪手）、**EMKV90-TOR**（电磁炮 + 攻城模式）、**F-35E / Su-57**（需要全新固定翼飞控，单独立项）。
5. 载具之外的「战地味」系统（热诱饵、APS 主动防护、修复系统、空运重生点、Air Radar）建议做成独立横切系统切片，而不是散落在各车里（见 §5.3）。
6. §8 列出 **12 项待你拍板的决策**；确认前不写实现代码。

---

## 2. 现状盘点（本仓库，可扩展性审计）

### 2.1 已有载具

| 内部 id | 名称 | 类型 | 座位 | 血量 | 主要武器 | 关键证据 |
| --- | --- | --- | --- | --- | --- | --- |
| little_bird | Little Bird | 侦察直升机 | 4（驾驶 + 3 乘客） | 80 | 双联 7.62 机枪 + 70mm 火箭 | entity/LittleBirdEntity.java:78；seat/LittleBirdSeat.java:11-14 |
| apache | AH-64 Apache | 武装直升机 | 2（主/副驾，V 换座） | 100 | 19/19 火箭 + 1/5 陶氏 + 30mm 链炮（副驾） | entity/ApacheEntity.java:70-90；seat/ApacheSeat.java |
| main_battle_tank | M1A5 | 主战坦克 | 2（驾驶 + 炮手） | 120 | 120mm 主炮（AP/HE）+ 同轴机枪 + 烟雾 | entity/MainBattleTankEntity.java:62-64；seat/GroundSeat.java:11-14 |
| infantry_fighting_vehicle | EBAA Wildcat | 步兵战车 | 4（驾驶 + 炮手 + 2 载员） | 90 | 双联 30mm + ATGM | entity/InfantryFightingVehicleEntity.java:50-52；seat/GroundSeat.java:16-21 |

### 2.2 可直接复用的系统

| 子系统 | 复用点 | 证据 |
| --- | --- | --- |
| 地面运动 | 纯规则、按 Spec 参数化：GroundDrive.Spec(maxSpeed, accel, braking, turnRate, turnAtStandstill) | entity/ground/GroundDrive.java:20-29 |
| 地面武器 | GroundWeapons.fireHitscan(...) 一把通吃：直击/装甲价/溅射/曳光/音效全在管线里 | entity/ground/GroundWeapons.java:47-84 |
| 炮塔 | ChainGunTurret（绝对世界瞄准、热度、射界）；MainGun（俯仰/方位状态机） | entity/weapon/ChainGunTurret.java；MainGun.java |
| 弹仓 | AmmoPool（单池）、TwoStageMagazine（待发/备弹）、AmmoSupply（弹药箱）、SmokeLauncher | entity/weapon/*.java |
| 投射物 | LittleBirdRocketEntity、ApacheMissileEntity（SACLOS）、MainGunShellEntity | entity/*.java |
| 飞行 | HelicopterFlightModel（四轴纯规则）+ 每机 FlightConfig；ApacheFlightProfile 是第二台机的先例 | entity/flight/*.java |
| 客户端 | GroundVehicleControls（输入/座位接口）、GroundView（舱内视角接口）、GroundVehicleInput（按键采集） | client/GroundVehicleInput.java:42；client/GroundView.java |
| 骑乘/部署/存亡 | GunshipRider 标记、AircraftDestruction（单次销毁守卫）、XxxDeployItem、AmmoSupply/AircraftRearm | entity/GunshipRider.java；item/*DeployItem.java |
| 配置 | CLIENT ground.{tank,ifv} 手感 + COMMON balance.{...} 数值，每车一组工厂方法 | LittleBirdConfig.java:226-260, 363-480, 670-796 |
| 资产 | 代码建模 client/*Model.java；models/ 纯 Python 盒模型 + STL/侧视 PNG；tools/generate_ground_unit_textures.py（Python，WSL 原生） | models/README.md；tools/ |
| 数据 | datagen 配方/解锁/物品模型 + LangFileTest 校验双语 | data/ModRecipeProvider.java；test/**/LangFileTest.java |

### 2.3 新载具的隐藏成本：instanceof 硬编码点（关键）

新增一台地面载具，不是只写一个实体类；以下位置目前都按具体车型分派，必须逐处修改：

| 位置 | 现行写法 | 影响 |
| --- | --- | --- |
| client/GroundHud.java:67-70 | if (vehicle instanceof MainBattleTankEntity) ... else if (instanceof InfantryFightingVehicleEntity) | 第三台车没有 HUD |
| client/GroundCamera.java:35-36 | vehicle instanceof MainBattleTankEntity 或 InfantryFightingVehicleEntity | FOV/相机不生效 |
| client/GroundRiderRender.java:30-31 | 同上二选一 | 乘员身体不隐藏 |
| entity/GroundWeapons.java:101-110 | healthFraction() 是一长串 instanceof + 静态 MAX_HEALTH | 命中血条对新车型失效 |
| entity/AirframeTargets.java:18-26 | instanceof ApacheEntity / LittleBirdEntity | 新飞机在狗斗定价里被当成无装甲 |
| entity/seat/GroundSeat.java:11-21 | TANK_* / IFV_* 常量 + isTankDriver 等 | 座位语义词被车型绑死 |
| entity/weapon/WeaponSelection.java | forTankDriver() 专用 | 新车的武器 carousel 要另写 |
| client/ClientModEvents.java:28-65 | 逐条注册 layer / HUD / renderer | 机械追加，但漏一个渲染器客户端崩溃 |
| registry/ModEntities.java / ModItems.java / ModCreativeTabs.java | 逐条注册 | 机械追加 |
| network/ModNetwork.java:23-81 | 手工编号 0-10 | 若新系统加包需继续编号 |

已泛化的好接口：GroundVehicleControls（GroundVehicleInput 只认它）、GroundView（相机只认它）、GroundDrive.Spec、GroundWeapons。S0 的目标就是把上面那张表也变成「只认接口」。

### 2.4 测试与资产基线

- 测试：**44 个测试文件 / 319 个 @Test**（grep -rh @Test | wc -l，本轮实测），纯规则类不 import net.minecraft。
- 构建：./gradlew compileJava / test / build（build 尾部 syncLocalMods 同步到 PCL2 实例）。
- 现存约束：每切片 ≤ 8 文件；根代理负责集成、全量构建、JAR 同步与自审（AGENTS.md）。

---

## 3. BF2042 载具全谱（外部资料汇编）

> 来源为 battlefield.fandom.com（社区 wiki，经 MediaWiki API 读取）+ EA 官方补丁说明。社区 wiki 有滞后与冲突，§3.4 已列不确定项；数值只作规格参考，落码时以「手感标定 + 可配置」为准。

### 3.1 地面载具（All-Out Warfare 核心）

| 载具 | 类别 | 阵营 | 座位 | 火力 | 特性 / 定位 | 加入 |
| --- | --- | --- | --- | --- | --- | --- |
| M1A5 | 重甲 / MBT | 美 | 4 | 120mm（MPAT/HE/STAFF）+ 同轴机枪；枪手 12.7mm/TOW；前枪手火箭/榴弹巢 | 工程已实现（简化为 2 座） | 首发 |
| T28 | 重甲 / MBT | 俄 | 4 | 同 M1A5 结构 | M1A5 的俄系镜像 | 首发 |
| EBAA Wildcat | 轻甲 / 自行防空 | 共 | 4 | 双联 30/35mm + 防空或反坦克导弹；枪手 HMG/榴弹 | 工程已实现（简化为 4 座） | 首发 |
| M5C Bolte | 装甲运输 / 轻甲侦察 | 共 | 2 | 枪手 RWS：7.62 机枪 / 40mm 燃烧榴弹 / 50mm 炮 / TOW；驾驶员地雷/烟雾 | 履带、高机动、风挡抗弹 | 首发 |
| LATV4 Recon | 轻运输 | 共 | 4 | RWS：7.62 / 12.7 / 20mm 霰弹 | 高速敞篷侦察 | 首发 |
| LCAA Hovercraft | 轻运输 | 共 | 4 | 顶枪手 7.62/40mm 榴弹/12.7；两侧 LMG | 两栖气垫船 | 首发 |
| MAV | 装甲运输 | 共 | 5 | 40mm 炮 + 副枪手导弹 + 武器吊舱 | 两栖、团队移动重生点 | 首发 |
| CAV-Brawler | 装甲运输 | 共 | 5 | 40mm 霰弹炮 + 同轴 LMG；副枪手 7.62/20mm 防空/40mm 燃烧 | 威胁侦测、近战反步兵 | S4 |
| EBLC-Ram | 重甲 / 装甲侦察 | 共 | 4 | 40mm 炮 / 105mm 榴弹炮 / 导弹齐射 + 同轴；导弹站；吊舱 | 团队重生信标 | S2 |
| EMKV90-TOR | 重甲 / 坦克歼击 | 共 | 2 | 电磁炮（蓄力）+ 同轴；枪手 RWS | 攻城模式（锁定蓄力更快） | S3 |
| M3A3 Bradley / BMP-2 | 重甲 / IFV | 美/俄 | 4 | 25mm 机炮 + 同轴/导弹/APFSDS；枪手 HMG | 战役末期后追加（2025-01） | 8.4 |
| Ranger | 无人机 | 共 | 0 | 机载机枪 | 自主 AI 地面机器狗 | 首发 |
| 4x4 / Polaris Sportsman / RZR / Tuk-tuk | 非战斗民用 | 共 | 2-4 | 无 | 地图刷新的代步工具 | 首发/2.1 |

### 3.2 空中载具（全 14 台）

| 载具 | 类别 | 阵营 | 座位 | 武器 | 特性 | 加入 |
| --- | --- | --- | --- | --- | --- | --- |
| MD540 Nightbird | 攻击直升机（实为侦察） | 共 | 4（驾/观察/2 乘客） | 主驾双联 7.62 / 20mm 机炮 + 70mm 火箭 / AGM；观察位传感器 | 工程的小鸟是其同类；BF2042 版多了观察座 | 首发 |
| AH-64GX Apache Warchief | 攻击直升机 | 美 | 2 | 主驾智能火箭/AIM-9Z/AGM/TOW；枪手 360° 30mm | 工程已实现 | 首发 |
| Ka-520 Super Hokum | 攻击直升机 | 俄 | 2 | 同阿帕奇结构 + 主驾前向双联 30mm | 工程未实现 | 首发 |
| MV-38 Condor | 空运 / 炮艇 | 美 | 6 | 主驾无武器；40mm 榴弹 / 50mm 炮 + 两门 7.62 | 重型装甲、VTOL/平飞切换 | 首发 |
| Mi-240 Super Hind | 空运 / 炮艇 | 俄 | 6 | 同 Condor | 同上 | 首发 |
| RAH-68 Huron / YG-99 Hannibal | 隐身攻击直升机 | 美/俄 | 2 | HMG/30mm 吊舱 + AGM/IR/线导；枪手压制 HMG + EMP | 隐身模式（收武器舱、免锁定、HE 炸弹） | S1 |
| UH-60 | 空运 / 炮艇 | 共 | 5 | 两门 7.62 机枪 | 战役末期后追加（2024-09） | 8.1 |
| YUV-2 Pondhawk | 轻空运（飞行摩托） | 共 | 2 | 无（乘客用个人武器） | 微型 VTOL、脆弱 | S6 |
| F-35E Panther | 战斗机（VTOL） | 美 | 1 | 25/30mm 机炮 + AIM-9Z/AGM/雷达弹/火箭巢 | 固定翼 + 悬停、加力 | 首发 |
| Su-57 Felon | 战斗机 | 俄 | 1 | 25/30mm + AA-11/AGM/雷达弹/火箭 | 固定翼、机动更好 | 首发 |
| XFAD-4 Draugr | 攻击机 / 武装无人机 | 共 | 1 | 20mm 机炮（HE/AP）+ 火箭巢 + EMP/燃烧/精确炸弹 | 遥控、热成像 HUD、DRFM 干扰 | S7 |
| A-10 Warthog / Su-25TM | 攻击机 | 美/俄 | 1 | GAU-8/GSh-30 30mm + AIM-9Z/AA-11/AGM/火箭 | 固定翼、反载具 | 9.2（2025-08） |

### 3.3 BF2042 载具战斗系统（可供本模组借鉴的战地味）

| 系统 | 作用 | 归属 | 本模组实现难度 |
| --- | --- | --- | --- |
| Missile Countermeasures（热诱饵） | 干扰锁定导弹、短暂免疫再锁定；冷却约 14s | 空中 | 中：需要先有「锁定导弹」 |
| APS 主动防护 | 硬杀伤拦截来袭投射物，短窗生效后进入冷却；与维修共用槽位 | 地面重甲 | 中：拦截投射物实体 + 纯规则状态机 |
| 烟雾三件套 | Smoke Discharger / Smoke Launcher / Thermal Smoke（挡红外制导） | 地面为主 | 低：已有 SmokeLauncher |
| Repair System | 一次性修复全部部件 + 持续再生，受击打断 | 全载具 | 低-中：纯规则计时 + 服务端回血 |
| Repair Tool | 工程兵在车外/车内持续修理 | 玩家工具 | 中：交互 + 目标选择 |
| Air Radar | 小地图标记空中目标 | 空中 + Wildcat | 中高：需要 HUD 雷达绘制 |
| 移动重生点 / 团队信标 | MAV / EBLC-Ram 的团队重生 | 运输/侦察 | 高：需重生点系统 |
| 呼叫空投 | 战场任意点呼叫地面载具（空中只能出生点） | 系统 | 中：可复用现有部署物品，或做呼叫键 |
| 系统损坏 | 部件受损导致功能降级 | 全载具 | 高：范围大，建议后置 |

### 3.4 资料可信度与不确定项（落码前必须复核）

1. 社区 wiki 有滞后/自相矛盾：热诱饵冷却官方补丁是 14s，但部分车页仍写 15s；Super Hind 前枪手武器在信息框（40mm 榴弹/50mm 炮）与正文（30mm）冲突。
2. 部分加入时间标注的是赛季首发、实际晚到补丁（如 XFAD-4 Draugr 标注 S7、实装于 Update 7.2）。
3. M3A3 Bradley / BMP-2（2025-01）、UH-60（2024-09）、A-10/Su-25TM（2025-08）在最终赛季（S7）之后，属于收尾期内容；本规划按最终完整阵容收录。
4. 座位数与武器解锁数是活的平衡数值，会随补丁变化；本模组一律转成可配置的标定值，不宣称与 BF2042 数值一致。
5. EBAA Wildcat 的解锁等级在英文 wiki 与日文 wiki 冲突，不影响本模组（本工程按载具能力建模，不做账号解锁）。

---

## 4. 差距分析与选型

### 4.1 已覆盖 / 未覆盖

- 已覆盖：轻型侦察直升机（小鸟 ≈ MD540）、美系武直（阿帕奇 ≈ AH-64GX）、美系 MBT（M1A5）、装甲侦察/步战（Wildcat）。
- 明显缺口：轻甲侦察/运输类（Bolte、LATV4、Hovercraft、MAV、Brawler）、俄系武直（Ka-520）、多座空运炮艇（Condor/Super Hind/UH-60）、坦克歼击/电磁炮（TOR）、固定翼喷气机（F-35E/Su-57/A-10）、隐身武直、无人机（Ranger）、两栖。
- 一句话：现有 4 台都是单/双人火力平台，缺的是多人协同载具、两栖/运输、固定翼、以及战术系统（反制/防护/重生）。

### 4.2 选型评分矩阵（5 分制；复用度越高越省，资产/风险越高越贵）

| 载具 | BF2042 辨识度 | 复用度 | 玩法差异 | 资产成本 | 技术风险 | 多人价值 | 建议波次 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| M5C Bolte | 4 | 5 | 3 | 3 | 2 | 4 | W1 |
| Ka-520 Super Hokum | 4 | 5 | 3 | 3 | 2 | 4 | W1 |
| CAV-Brawler | 4 | 4 | 4 | 4 | 3 | 5 | W2 |
| MV-38 Condor / Mi-240 | 5 | 4 | 5 | 5 | 4 | 5 | W2 |
| EMKV90-TOR | 5 | 3 | 5 | 4 | 2 | 4 | W3 |
| F-35E / Su-57 | 5 | 1 | 5 | 5 | 5 | 5 | W3（单独立项） |
| EBLC-Ram | 4 | 4 | 4 | 4 | 4 | 4 | W4 |
| LCAA Hovercraft | 3 | 3 | 5 | 5 | 5 | 4 | W4 |
| MAV | 3 | 3 | 5 | 5 | 5 | 5 | W4 |
| RAH-68 / YG-99 | 3 | 4 | 4 | 4 | 3 | 3 | W4 |
| M3A3 / BMP-2 | 3 | 5 | 2 | 3 | 2 | 3 | W4 |
| T28 | 3 | 5 | 1 | 2 | 1 | 2 | W5（变体） |
| LATV4 Recon | 3 | 4 | 3 | 3 | 2 | 3 | W5 |
| UH-60 | 4 | 4 | 4 | 4 | 3 | 4 | W5 |
| YUV-2 Pondhawk | 2 | 4 | 2 | 2 | 2 | 2 | W5 |
| XFAD-4 Draugr | 4 | 2 | 5 | 5 | 4 | 4 | W5 |
| A-10 / Su-25TM | 5 | 1 | 4 | 5 | 5 | 3 | W5 |
| Ranger | 4 | 1 | 5 | 4 | 5 | 4 | 后置（AI 子系统） |
| 民用 4x4 / Polaris / RZR | 2 | 3 | 1 | 2 | 1 | 1 | 可跳过 |

### 4.3 推荐波次与理由

- W1（先证明架构 + 补地面/空中各一台）：M5C Bolte + Ka-520 Super Hokum。
  - 两台几乎不用新纯规则：Bolte 用 GroundDrive + ChainGunTurret/GroundWeapons + 现有 TOW 投射物；Super Hokum 用现成飞控 + 火箭/导弹 + 链炮。
  - 价值：地面多了高速轻甲侦察；空中多了主驾也有前向机炮的俄系武直，狗斗生态更完整。
- W2（多人协同，本模组最缺）：CAV-Brawler（5 座地面）+ MV-38 Condor 或 Mi-240 Super Hind（6 座空运）。
  - 需要先解决多枪手座位 + 每人独立瞄准的网络/输入扩展（阿帕奇的副驾瞄准包可作先例）。
- W3（旗舰新颖玩法）：EMKV90-TOR（蓄力电磁炮 + 攻城模式）+ 喷气机（F-35E/Su-57，需新固定翼纯规则飞控，单独立项评审）。
- W4（系统与两栖）：热诱饵/APS/修复等横切系统 + EBLC-Ram/LCAA Hovercraft/MAV（两栖、重生点）。
- W5（收尾与变体）：T28、LATV4、UH-60、Pondhawk、隐身武直、A-10/Su-25TM、Draugr、Ranger、民用。

### 4.4 本轮明确不做

- 海军舰艇、Battlefield Portal 载具线（那是另一套经典数值）。
- 专家/职业系统、Plus 实时改枪、系统级部件损坏、128 人大战场。
- BF2042 的账号解锁 / 战斗通行证门槛；本工程继续用创造栏 + 生存打造链。
- 任何 EA/DICE 的模型/贴图/音效提取；资产继续程序化生成。

---

## 5. 架构设计：让第 N 台载具变便宜

### 5.1 P0 抽象层（S0 切片，不新增载具）

目标：把 §2.3 的硬编码点收敛成接口，之后新车只实现接口即可被发现。

- 新增 entity/VehicleHull（或并入 GunshipRider）：
  - float maxHealth(); float health(); default float healthFraction();
  - 替换 GroundWeapons.healthFraction() 的 instanceof 链（GroundWeapons.java:101-110），命中血条对所有车自动生效。
- 新增 entity/ground/GroundHull：
  - extends GroundVehicleControls, GroundView；
  - int seatCount(); SeatRole seatRole(int seat);（DRIVER/GUNNER/OPERATOR/PASSENGER）；
  - GroundHudSource hudSource();
  - GroundRiderRender、GroundCamera 改成 instanceof GroundHull / GroundView。
- 新增 client/GroundHudSource：把 HUD 需要的读数（座位、武器槽、弹药、装填、耐久、炮塔角）做成接口，GroundHud 只认接口，绘制复用 GunshipHudDraw/GroundHudLayout。阿帕奇时代的 GunshipHudSource 思路可直接套用。
- entity/AirframeTargets 改为 AirframeTarget 接口（airframeClass() + airframeHealthFraction()），新增飞机不再改这个文件。
- entity/seat/GroundSeat 的 TANK_* / IFV_* 常量保留兼容，但判定改为走 GroundHull.isDriver/isOperator。
- WeaponSelection 增加通用工厂 of(int...) ，forTankDriver() 保留为语法糖。

验收：4 台现有载具 compileJava + 319 测试全绿 + runClient 目视回归（HUD/相机/隐藏乘员/命中血条），没有任何行为变化。这是唯一一个只重构、不加功能的切片，强烈建议先做。

### 5.2 P1 载具规格数据化（可选）

新增纯规则 GroundVehicleSpec record（HP、座位角色表、GroundDrive.Spec、武器槽定义、几何常量键），每车一个 Spec 工厂；config 与注册读它。好处：加车时配置、HUD、几何三处不再各写一遍。风险：过度抽象；建议在 W1 第一台车落地后再评估是否值得，不强制。

### 5.3 P2 横切系统（建议独立切片，不塞进单车）

| 系统 | 纯规则类 | 接线 | 依赖 |
| --- | --- | --- | --- |
| 热诱饵 / Missile Countermeasures | Countermeasures（charges/cooldown/deployedTicks/lockBreak） | 同步状态 + 粒子 + HUD 冷却 | 先有锁定导弹 |
| 锁定导弹（IR/AGM） | MissileGuidance（锁定/丢失/寿命/过载） | 新投射物实体或扩展 ApacheMissileEntity | 现有 SACLOS 不是锁定制导 |
| APS 主动防护 | ActiveProtection（activeWindow/recharge） | 服务器扫描附近投射物并 discard() | 需要投射物统一标记接口 |
| 修复系统 | RepairSystem（一次修复 + 再生/受击打断） | 服务端回血 + HUD | 无 |
| 空运重生点 | — | 服务器重生逻辑（原版 setRespawnPosition 思路） | 高，后置 |

### 5.4 P3 资产管线

- 地面：models/src/ground_units.py 的盒模型 + models/params/*.json 扩展为每车一份；tools/generate_ground_unit_textures.py 生成实体贴图；物品图标沿用 16x16 生成脚本。
- 空中：现有 tools/generate_apache_texture.ps1 是 PowerShell，WSL 下无 pwsh（需 interop）；建议补一份 Python 版贴图脚本，与地面贴图脚本统一，避免每次走 interop。
- 音效：现有 engine/gun/gun_shot/rocket_launch/explosion 可复用；喷气机/运输机建议新增 jet_engine、rotor_heavy 两个合成音。

### 5.5 P4 注册与配置

- ModEntities / ModItems / ModCreativeTabs / ClientModEvents 逐条机械追加（注意：漏注册渲染器 = 客户端崩溃，见 ClientModEvents.java:50-65 注释）。
- LittleBirdConfig：每车新增 CLIENT 手感组 + COMMON balance.<vehicle> 数值组 + 静态 xxxDriveSpec() / 武器工厂（照抄 670-796 的写法）。
- datagen：配方/解锁/成就/物品模型一次跑 ./gradlew runData --rerun-tasks；语言文件改完必须过 LangFileTest。

### 5.6 多人 / 网络

- 现有通道编号 0-10（ModNetwork.java:23-81），继续递增即可；GroundWeaponInputPacket 已是通用意图包，多座位扩展只需按发送者座位分派。
- 多枪手座位的先例是阿帕奇副驾：客户端发瞄准角 + 扳机，服务端校验发送者确实坐该座（ApacheGunnerAimPacket.java:30 模式）。空运载具请直接复用这套。

---

## 6. 逐车设计规格（草案）

> 数值全部是提案，遵循现有锚点（阿帕奇 100 HP / 小鸟 80 / M1A5 120 / Wildcat 90）与可配置原则，进游戏后再标定。以下不写实现代码。

### 6.1 M5C Bolte（W1，地面，2 座）

| 项 | 规格 |
| --- | --- |
| 内部 id | m5c_bolte |
| 类别/角色 | 装甲运输 / 轻甲侦察；高速履带 |
| 座位 | 2：驾驶（0）、枪手（1，RWS 遥控站） |
| 血量 | 70（轻甲，低于 Wildcat） |
| 枪手武器 | RWS carousel：① 7.62mm 机枪（沿用 30mm 链炮的 hitscan 管线，伤害建议 6/2，600 发，过热）；② 50mm 炮（建议直击 20、装甲 8、溅射 1.5，约 3s/发）；③ TOW（复用 ApacheMissileEntity，1 待发 / 2 备弹） |
| 驾驶装备 | 烟雾（沿用 SmokeLauncher）+ 反坦克地雷（后置，或先做烟雾） |
| 运动 | 履带：maxSpeed 0.55 格/t（约 11 格/s）、accel 0.04、braking 0.06、turn 4.0 度/t、standstill 0.5 |
| 摄像机/HUD | 沿用 GroundView 舱内视角 + GroundHudSource；武器 carousel 3 槽 |
| 复用 | GroundDrive、ChainGunTurret、GroundWeapons、ApacheMissileEntity、SmokeLauncher、AmmoSupply |
| 新增纯规则 | 仅 ReconRws（若 RWS 需要独立射界/回转）与配置；测试覆盖载弹/切换/射界 |
| 资产 | 盒模型（低矮 6 轮/履带侦察车）+ 程序化贴图 + 新物品图标 |
| 风险 | 低 |

### 6.2 Ka-520 Super Hokum（W1，空中，2 座）

| 项 | 规格 |
| --- | --- |
| 内部 id | ka_520 |
| 类别 | 俄系攻击直升机；与阿帕奇同构，主驾多一门前向机炮 |
| 座位 | 2：主驾（0，飞控 + 前向武器 + 火箭/导弹）、副驾（1，独立 360 度 30mm 炮塔），V 换座 |
| 血量 | 110（略高于阿帕奇） |
| 主驾武器 | ① 前向双联 30mm（hitscan，8/3，600 发，过热，机头固定）；② 19/19 火箭巢（沿用）；③ 1/5 导弹（沿用 SACLOS/后续锁定弹） |
| 副驾武器 | 30mm 链炮炮塔（沿用 ChainGunTurret，绝对瞄准） |
| 飞行 | 新增 Ka520FlightProfile：极速 1.20（约 86 km/h）、偏航/俯仰比阿帕奇再低约 10%，更重 |
| 复用 | HelicopterFlightModel、ApachePods/TwoStageMagazine、ChainGunTurret、阿帕奇的双座位/输入包模式 |
| 新增纯规则 | SuperHokumFlightProfile + 前向机炮的机头固定弹道参数；测试覆盖双武器切换与炮塔射界 |
| 资产 | 共轴双旋翼模型（与阿帕奇明显区分）+ 程序化贴图 + 图标 |
| 风险 | 低-中（双武器/座位同步已有先例） |

### 6.3 CAV-Brawler（W2，地面，5 座）

| 项 | 规格 |
| --- | --- |
| 内部 id | cav_brawler |
| 类别 | 装甲运输；8x8 轮式 |
| 座位 | 5：驾驶（0）、枪手甲（1）、枪手乙（2）、载员（3、4） |
| 血量 | 140（重甲运输，高于 MBT） |
| 武器 | 枪手甲：40mm 霰弹炮（近距高伤 hitscan，建议 12/4，溅射 2）；枪手乙：7.62 机枪 / 20mm 防空；驾驶：烟雾 + APS（P2 系统） |
| 运动 | 8x8：maxSpeed 0.40、accel 0.03、braking 0.06、turn 3.0 度/t、standstill 0.15（不能原地转） |
| 特性 | 威胁侦测：对攻击本车者加短暂发光标记（可先做简版：HUD 方向提示） |
| 复用 | GroundDrive、ChainGunTurret、GroundWeapons、AmmoPool |
| 新增纯规则 | 多枪手座位的分工与两个独立炮塔状态；测试覆盖座位与武器映射 |
| 资产 | 8x8 轮式重甲盒模型 + 贴图 + 图标 |
| 风险 | 中（多枪手输入/同步） |

### 6.4 MV-38 Condor / Mi-240 Super Hind（W2，空中，6 座空运）

| 项 | 规格 |
| --- | --- |
| 内部 id | mv38_condor / mi240_super_hind（先做一台，另一台为变体） |
| 类别 | 空运炮艇；多枪手 |
| 座位 | 6：驾驶（0，无武器）、前枪手（1，40mm 榴弹 / 50mm 炮）、左门枪手（2）、右门枪手（3）、载员（4、5） |
| 血量 | 170（重甲：BF2042 里能扛 3 发 120mm） |
| 武器 | 前枪手俯仰/方位受限的 40mm 榴弹（弧线用投射物或大溅射 hitscan）；两门 7.62 机枪（限射界，沿用链炮） |
| 飞行 | 初期先用直升机模型放大参数（重、慢、惯性大）；VTOL/平飞切换放后续切片 |
| 复用 | HelicopterFlightModel、ChainGunTurret（限射界）、阿帕奇副驾瞄准包模式、GroundWeapons |
| 新增纯规则 | 门枪手射界（左右半球）+ 前枪手俯仰限位；测试覆盖射界钳制与座位映射 |
| 资产 | 大机体盒模型（旋翼/倾转短舱）+ 贴图 + 图标 |
| 风险 | 中-高（多座位同步、模型体量大、性能） |

### 6.5 EMKV90-TOR（W3，地面，2 座）

| 项 | 规格 |
| --- | --- |
| 内部 id | emkv90_tor |
| 类别 | 坦克歼击 / 电磁炮平台 |
| 座位 | 2：驾驶（0，主炮）、枪手（1，RWS 机枪） |
| 血量 | 100 |
| 主炮 | 蓄力电磁炮：按住蓄力（建议 50 tick 约 2.5s）到释放高伤直射（建议直击 90 / 装甲 60，几乎无溅射），过热/冷却；命中用 MainGunShellEntity 类投射物或加长 hitscan |
| 攻城模式 | 开关：锁定车体（运动归零）后蓄力时间减半、散布更小；关闭后恢复机动 |
| 运动 | 慢：maxSpeed 0.25、turn 2.0 度/t |
| 复用 | GroundDrive、MainGunShellEntity、GroundWeapons、ChainGunTurret（枪手 RWS） |
| 新增纯规则 | RailgunCharge（蓄力/释放/过热）+ SiegeMode（锁定与增益）；测试覆盖蓄力曲线、打断、模式切换 |
| 资产 | 低矮无炮塔设计 + 电磁炮管 + 程序化贴图（蓄力辉光） |
| 风险 | 低-中（新颖但纯规则清晰） |

### 6.6 F-35E Panther / Su-57 Felon（W3，空中，1 座，需单独立项）

- 需要全新的固定翼纯规则飞控（升力/失速/滚转-偏航协调/加力），与现有直升机四轴模型不同，是本规划里技术风险最高的一项，建议先写一份独立的 jet-flight-plan.md 再排期。
- 武器：25/30mm 机炮（hitscan）+ 锁定/雷达导弹 + 火箭巢；1 座，HUD 需要速度/高度/油门/加力条。
- 资产：细长机体 + 后掠翼 + 加力尾焰，逐台模型成本高。
- 建议：只做一台（F-35E 或 Su-57）作为喷气机范式的验证，确认手感与性能后再考虑第二台。

### 6.7 其他候选（一句话）

- T28：M1A5 的俄系变体，改外观/少量数值即可，最省，但玩法无新意。
- M3A3 Bradley / BMP-2：Wildcat 的近亲，车体+25mm 即可，适合做同族多车。
- EBLC-Ram：40mm/105mm + 导弹站 + 重生信标；信标是难点。
- LCAA Hovercraft / MAV：两栖是新物理（水面浮力/推进），建议独立研究。
- RAH-68 / YG-99：隐身模式（收武器舱、免锁定）是新的机制切片。
- UH-60 / Pondhawk / Draugr / A-10：见 §3.2；Pondhawk 最省，A-10 与喷气机同属固定翼立项。
- Ranger：AI 地面单位，属于新子系统，不纳入本轮。

---

## 7. 里程碑与切片（草案，每片 ≤ 8 文件）

| 切片 | 内容 | 主要文件 | 验证 |
| --- | --- | --- | --- |
| S0 | 载具抽象层：VehicleHull/GroundHull/GroundHudSource/AirframeTarget，现有 4 车回归 | 6-8 | compileJava + 319 测试 + 4 车 runClient 回归截图 |
| S1 | Bolte 纯规则 + 配置（RWS 射界/切换、50mm 参数） | 3-5 | 新单测 |
| S2 | Bolte 实体 + 座位 + 注册 + 部署物品 | 5 | runServer 生成/骑乘/下机 |
| S3 | Bolte 模型/贴图/渲染器 + HUD 接线 | 5-6 | runClient 多角度截图 |
| S4 | Bolte datagen（配方/成就/语言/创造栏） | 4-5 | runData + LangFileTest |
| S5 | Ka-520 纯规则 + 飞行预设 | 3-4 | 新单测 |
| S6 | Ka-520 实体/双座/双武器/注册 | 6 | runClient 双人联机 |
| S7 | Ka-520 模型/贴图/渲染/HUD | 5 | 截图 |
| S8 | 横切系统 A：锁定导弹 + 热诱饵（纯规则 + 接线） | 6-8 | 单测 + 联机对射 |
| S9 | Brawler（沿用 S1-S4 流程） | — | 同上 |
| S10 | Condor/Super Hind 空运（多枪手） | — | 多人实测 |
| S11 | TOR 电磁炮 + 攻城模式 | — | 单测 + 手感 |
| S12 | APS + 修复系统 | — | 单测 + 联机 |
| S13 | 喷气机（独立立项评审后） | — | 新飞控单测 + 手感 |
| S14 | 集成：平衡标定、全量 build、JAR 同步、自审 | 根代理 | ./gradlew build + 哈希核对 |

每个载具切片收尾都按 AGENTS.md：单独跑 build、同步 PCL2、核对 SHA-256、写自审报告（构建成功不等于视觉/行为验证）。

---

## 8. 待用户确认的决策（确认前不写实现代码）

1. 首批范围：是否按推荐先做 M5C Bolte + Ka-520 Super Hokum？还是你想先要某台（如喷气机/TOR/运输机）？
2. 保真度口径：BF2042 机制尽量还原还是 MC 化近似？（推荐近似：继续用部署物品 + 固定挂载，不做 Plus 改枪/专家/系统级损坏。）
3. S0 抽象层：是否同意先做这个零新功能的重构切片？不做的话后续每加一台车都会改 4-5 处核心文件。
4. 阵营：继续不引入阵营（推荐），T28/Ka-520 只作为外观与数值变体？
5. 喷气机：是否要做？这需要全新的固定翼飞控，是本规划最大单项工程。
6. 两栖：Hovercraft/MAV 的水面浮力与推进，是否纳入范围？
7. 重生点/呼叫空投：MAV/EBLC-Ram 的团队重生、BF2042 式战场呼叫，是否要做？（需要新系统，风险高。）
8. 反制系统：热诱饵/APS/修复是否作为独立系统先做，再挂到各车？
9. 战斗系统深度：Air Radar 小地图、威胁侦测、EMP、干扰弹这些是否列入？
10. 资产：继续程序化占位（推荐，无版权风险），还是你能提供参考图/期望更精细的模型？
11. 数值标定：沿用现有评分映射 + 进游戏再调的流程？是否接受 §6 提案值？
12. 生存打造：每台新车是否都要零件到半成品到整机的配方与成就（成本明显增加），还是新车先只进创造栏？

---

## 9. 风险

1. 扩展性风险：不加 S0 抽象层，instanceof 会持续扩散，回归面随载具数线性上升。
2. 多人同步风险：多枪手/多座位是本工程的新领域（阿帕奇双座是唯一先例），需要严格的服务端座位校验。
3. 固定翼风险：现有飞控是四轴直升机模型，喷气机需要独立物理，可能推翻部分相机/HUD 假设。
4. 性能风险：大机体（空运）+ 多发射物 + 多 HUD 面板同屏，需实测。
5. 平衡风险：新车会改变现有 4 车对抗关系（例如 TOR 一发秒 MBT、空运炮艇过强），需要一批平衡标定。
6. 存档/协议风险：新增实体注册名会写入存档；网络包继续追加编号，旧客户端连新服务端会因协议版本不符被拒（现有一致性检查已处理）。
7. 资产风险：全部程序化生成，观感上限有限；参考图如需外部获取，注意版权（只作比例参考，不提取资源）。

---

## 10. 主要来源

外部资料（均为第三方资料，仅作设计参考）：
- BF2042 载具导航模板与分类：https://battlefield.fandom.com/wiki/Template:Vehicles/BF2042
- 载具呼叫/投放：https://battlefield.fandom.com/wiki/Vehicle_Drop
- 载具专精与系统：https://battlefield.fandom.com/wiki/Specializations_(Battlefield_2042) ；主动防护 https://battlefield.fandom.com/wiki/Active_Protection ；热诱饵 https://battlefield.fandom.com/wiki/Missile_Countermeasures ；空雷达 https://battlefield.fandom.com/wiki/Air_Radar ；烟雾 https://battlefield.fandom.com/wiki/Smokescreen
- 地面载具页（均在 https://battlefield.fandom.com/wiki/ 下）：LATV4_Recon / LCAA_Hovercraft / MAV_(Vehicle) / M5C_Bolte / CAV-Brawler / EBAA_Wildcat / EBLC-Ram / EMKV90-TOR / M1_Abrams/Battlefield_2042 / T28 / M3A3_Bradley / BMP-2 / Ranger / Polaris_RZR
- 空中载具页：AH-64_Apache/Battlefield_2042 / Ka-52 / AH-6_Little_Bird / RAH-68_Huron / YG-99_Hannibal / V-22_Osprey/Battlefield_2042 / Mi-24_Hind / UH-60_Black_Hawk / YUV-2_Pondhawk / F-35 / Su-57_Felon / XFAD-4_Draugr / A-10_Thunderbolt_II / Su-25TM
- EA 官方补丁说明：Update 6.2 / 7.0.0 / 8.1.0 / 9.2.0（https://www.ea.com/games/battlefield/battlefield-2042/news/...），以及 Reworking Vehicle Loadouts 开发日志
- 交叉核对：日文 BF2042 wiki https://bf2042.swiki.jp/

本工程内部证据：见 §2 各表 file:line；规划约束见 AGENTS.md、PLAN.md、apache-plan.md、survival-crafting.md。
