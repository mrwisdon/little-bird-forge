# 细节阶段：地面载具武器与 HUD（对标 BF2042 信息架构）

目标：`goal-7f004226-f61f-4f07-90e6-8ebd854cc547`
对标口径：**只参考 BF2042 的机制与信息层级，不复制其素材、不编造其数值**。

## 决策默认（用户连续未答，按其授权取默认）

先坦克；炮弹=多弹种切换+数量（暂不做实体弹道）；同轴保持炮手座；烟雾=车载烟幕发射器；UI 只做地面载具；"等等"只做 P0 烟幕；素材用程序化占位并标注。

## 阶段② 明细

### 项 0 — 接口/基类（纯规则）
- `entity/weapon/WeaponSelection.java`：武器槽（主炮/同轴/烟雾）与切换；坦克驾驶员=主炮↔烟雾，炮手=固定同轴。
- `entity/weapon/MainGunAmmoType.java`：120mm 弹种轮盘（MPAT/HEAT），只有 id 与 HUD 翻译键，**不含数值**。
- 单测：`WeaponSelectionTest`(6) + `MainGunAmmoTypeTest`(3)。

### 项 1 — 炮弹：双弹种弹仓接入坦克
新增：
- `entity/weapon/MainGunAmmo.java`（纯规则）：每弹种独立待发/备弹、切换、装填、计时、弹药箱补给；单测 `MainGunAmmoTest`(7)。
- `MainBattleTankEntity`：以 `MainGunAmmo` 取代单一 `magazine`；新增同步字段 `DATA_SHELL_TYPE`；`publishAmmo` 发布选中弹种的待发/备弹；NBT 按弹种存 `Ammo_<type>_Ready/Reserve`（兼容旧存档的单弹仓 `MagReady`）。
- 网络/输入：`GroundWeaponInputPacket` 增加 `shellDelta`（VarInt）；`GroundVehicleControls.applyWeaponInput` 增加该参数；新增键位 `SHELL_SWITCH`（默认 **B**，`key.battlefield.shell_switch`，中英文本地化）。
- `InfantryFightingVehicleEntity` 仅跟随接口签名（暂不使用弹种）。

**顺带修掉的既有缺陷**：`TwoStageMagazine.tick` 此前只有 `ApacheEntity` 调用，坦克/步战**从不推进装填计时**，导致 R 装填永远不会完成。本次在 `tickServer` 中补上 `mainGunAmmo.tick()`。

证据：`docs/plan/evidence/detail-phase/item1_shell_carousel.txt`（实机 5 次 NBT 读数）
| 阶段 | ShellType | MPAT ready | MPAT res | HEAT ready | HEAT res | Coax |
| --- | --- | --- | --- | --- | --- | --- |
| A 初始 | 0 | 5 | 15 | 5 | 15 | 300 |
| B 发射 MPAT 后 | 0 | **4** | 15 | 5 | 15 | 300 |
| C 按 B 切换后 | **1** | 4 | 15 | 5 | 15 | 300 |
| D 发射 HEAT 后 | 1 | 4 | 15 | **4** | 15 | 300 |
| E 按 R 等待后 | 1 | 4 | 15 | **5** | **14** | 300 |

结论：两弹种**互不干扰**；切换生效；装填**能完成**；同轴弹药不受影响。

验证命令：`./gradlew compileJava test` → BUILD SUCCESSFUL，单测 **280 / 0 failures**。

### 项 2 — 同轴机枪：独立准星（射手座不变）

新增：
- `client/GroundReticleMath.java`（纯规则）：相对方位角（±180 回绕）、水平 FOV 判定、由竖直 FOV+宽高比推水平 FOV、按散布取准星臂间隙；单测 `GroundReticleMathTest`(4)。
- `client/GroundHud.java`（`IGuiOverlay`，本轮**只画准星**）：**驾驶位**画居中的主炮重十字（长臂+宽隙）；**炮手位**把**炮塔实际指向**（网络同步的 yaw/pitch）投影到屏幕，画出**离轴**的同轴准星（短臂+角标+窄隙），并用 `withinFov` 判断是否仍在视野内。
- `GunshipHudDraw` 增加 `mainGunCrosshair` 与 `coaxCrosshair` 两个绘制原语（沿用既有配色常量）。
- `ClientModEvents` 注册 `ground_hud`。

设计要点：两个准星形状不同且**数据来自真实同步状态**（`getTurretYawDeg/getTurretPitchDeg`），没有写死假数据；同轴准星之所以"独立"，是因为炮塔由**驾驶员**铺设、炮手视角与炮口方向本就可以不一致。

证据：`docs/plan/evidence/detail-phase/item2_main_gun_reticle.png`（驾驶位准星可见）与
`item2_reticle_hidden_by_f1.png`（按 F1 隐藏 GUI 后准星消失，证明它确实由本 HUD 绘制）。

**未能实机验证的部分（如实标注）**：同轴准星只在**炮手座**显示，而单机无法占据第二座位（坦克没有换座包），因此**离轴准星的实机画面未取得**；其几何由单测覆盖（方位角回绕/FOV 边界/间隙钳制/宽高比），实机需第二玩家或后续补换座能力。

### 项 3 — 车载烟幕发射器（数量/冷却/持续 + 粒子云）

新增：
- `entity/weapon/SmokeLauncher.java`（纯规则）：`Spec(charges, cooldownTicks, durationTicks)` 与 `State(charges, cooldownTicks, activeTicks)`，含 `fire/tick/supply` 与两个 HUD 用的比例函数；单测 `SmokeLauncherTest`(6)。
- 配置 `LittleBirdConfig`：`smoke_charges=2`、`smoke_cooldown_seconds=20`、`smoke_duration_seconds=10`、`smoke_radius=4.0`（**占位值**，注释已注明非 BF2042 官方数值，待确认）。
- `MainBattleTankEntity`：`WeaponSelection.forTankDriver()` 正式接入——按住扳机时按选中槽决定**主炮还是烟幕**；新增同步字段 `DATA_SELECTED_WEAPON/DATA_SMOKE_CHARGES/DATA_SMOKE_COOLDOWN`；装填/补给沿既有弹药箱路径补 1 发；NBT 存 `WeaponSlot/Smoke* `。
- 网络/输入：`GroundWeaponInputPacket` 增加 `weaponDelta`；新增键位 `WEAPON_SWITCH`（默认 **G**；中英文本地化）。
- 表现：服务端在云持续期间每 tick 发送 `CAMPFIRE_COSY_SMOKE` 粒子（半径可配）。**MC 无体积雾，不承诺遮挡视线**，这点与用户口径一致。

证据：`docs/plan/evidence/detail-phase/item3_smoke.txt`（5 次 NBT 读数）+ `item3_smoke_cloud.png`。
| 阶段 | 武器槽 | 烟幕药 | 冷却 | 云 | 主炮待发 |
| --- | --- | --- | --- | --- | --- |
| A 上车 | 0 主炮 | 2 | 0 | 0 | 5 |
| B 按 G | **2 烟幕** | 2 | 0 | 0 | 5 |
| C 发射 | 2 | **1** | 343 | 143 | 5 |
| D 冷却中再扣 | 2 | 1（未再消耗） | 269 | 69 | 5 |
| E 冷却结束 | 2 | 1 | **0** | 0 | 5 |

**本项踩到并修复的缺陷**：旧存档（`MagReady` 格式）只恢复弹仓、不建烟幕，且 `ensureMagazines` 提前 return，导致 `addAdditionalSaveData` 对 null 的 `SmokeLauncher.State` 抛 NPE（`run/crash-reports/crash-2026-09-12_17.02.10-server.txt`）。改为**幂等初始化**（缺什么补什么）并对存档写入加空值保护；同一世界随后正常加载与保存。

## 阶段② 完成小结
炮弹（双弹种弹仓 + 装填计时修复）、同轴独立准星、烟幕发射器三项均已实现且各有实机证据。下一步进入**阶段③ UI 层**：把这些真实状态画成地面载具 HUD（武器槽与选中态、各武器弹药与冷却、血量与受损提示、功能冷却倒计时）。

## 阶段③ UI 层：地面载具 HUD（坦克）

新增/改动：
- `client/GroundHudLayout.java`（纯规则）：左下"车况"面板、右下"武器栈"面板的定位与读数换算（`fillWidth`、`cooldownSeconds`、`healthWarning`）；单测 `GroundHudLayoutTest`(7)。
- `client/GroundHud.java`：在既有准星之上画面板——**武器栈三行**：`1 120mm`（当前弹种 + 待发/备弹）、`2 同轴机枪`（弹药 + "炮手"标注）、`3 烟幕`（余量 + 冷却秒数/READY），**选中行高亮**；**车况面板**：血条 + 百分比，低于 35% 转红并显示 "LOW HULL"。
- 本地化：新增 9 个 `hud.battlefield.*` 键（中英），弹种名复用 `MainGunAmmoType.translationKey()`。
- 数据源全部为**实体同步状态**（`getReadyRounds/getReserveRounds/getCoaxAmmo/getShellTypeOrdinal/getSelectedWeapon/getSmokeCharges/getSmokeCooldownTicks/getHealth`），无写死值。

**踩到的坑（已修）**：本项目 `hud_scale`（默认 **2**）是**倍数**（`LittleBirdHudLayout` 用 `EDGE_MARGIN * scale`），我最初按百分比缩放 → 面板缩到 1/4 而字号不变，文字溢出屏幕外。已改为**面板尺寸固定、只有边距随倍数变化**，并把该约束写进单测（`scaleOnlyMovesTheMarginSoTextAlwaysFits`）。

证据：`docs/plan/evidence/detail-phase/item4_hud_full.png`（整屏）、`item4_hud_weapon_stack.png`（武器栈放大）、`item4_hud_low_hull.png`（`/damage` 95 后 "LOW HULL 21%"）。

### 阶段③ 续：EBAA 小野猫（IFV）HUD

- `GroundHud` 重构为**两车通用**：坦克左下/右下三行武器栈 + 车况；IFV 两行武器栈（`1 30mm 自动炮`（弹药，"驾驶员"）、`2 ATGM`（待发/备弹，"炮手"））+ 车况（上限 90）。
- 准星按座位：IFV **驾驶位**画紧凑自动炮准星（射速武器，间隙窄）；**炮手位**把炮塔线投影到屏幕（离轴，含 FOV 判定）。
- `GroundHudLayout.compute(w,h,scale,rows)` 支持按车携带的武器数决定行数；旧三参重载保留。
- 本地化新增 `ifv_autocannon/ifv_atgm/slot.driver`（中英，各 81 键）。

证据：`item5_ifv_hud_full.png`、`item5_ifv_weapon_stack.png`（`600` 自动炮 + `2/6` ATGM）、`item5_ifv_low_hull.png`（`/damage` 60 → `LOW HULL 33%`）；实机 NBT 同步显示 `Health 90 / AutocannonAmmo 600 / AtgmReady 2 / AtgmReserve 6`。

**过程记录**：首次截图误上了上一轮残留的坦克（聊天 "No entity was found" + 显示坦克武器栈）；清场后重测得以上结果——这类"残留实体"会导致张冠李戴，后续验收步骤先 `/kill` 全部载具。

## 追加：载具武器槽条 UI（按用户参考图）

参考图：**不在仓库内**，仅存在于聊天附件归一化副本
`/root/.dsh/attachments/v1/objects/02/0240095dd1757d80b85716b2ffe4a2b897d43c9dd54bf7bdd62057cb276c5715`（PNG 670x297）。
图中要素：右上读数（弹种名 / 大号待发数 + 环形装填弧 / 备弹符号 / 车名 + 环形血量），下方横排槽位条（选中槽为高亮宽块 + 炮弹图标 + 角标；机枪槽；反制槽；烟幕槽），槽位下有按键图例。

复用（『小鸟』HUD 既有方案）：`GunshipHudDraw` 的配色常量与面板/槽位写法、`LittleBirdHud.drawWeaponPanel` 的右下角锚点（`x = width - panelWidth - 6` + 青色顶边）、`LittleBirdHudLayout` 的"纯规则布局 + JUnit"范式；挂载沿用既有 `ground_hud` overlay（`ClientModEvents:44`），未新增挂载点。

改动文件：
- `client/GroundHudLayout.java`：槽位条几何（`SLOT_HEIGHT/SLOT_GAP/SLOT_SELECTED_WIDTH/SLOT_WIDTH/LEGEND_HEIGHT/READOUT_*`、`barWidth/slotX/slotWidth/readout`）。
- `client/GunshipHudDraw.java`：`SlotIcon{SHELL,BELT,SMOKE}` + `iconSlot/ammoCounter/vehicleChip`（程序化图形，无新素材）。
- `client/GroundHud.java`：右下角改为横排槽位条 + 弹药读数（坦克 3 槽 / 步战 2 槽），移除旧竖排 `drawRow` 栈。
- `GroundHudLayoutTest` +3 例；`lang/{en_us,zh_cn}.json` 各 +2 键（车名）。回退点 `docs/plan/backups/2026-09-12_pre-slotbar/`。

与参考图的差异（如实标注）：
1. 图例按键用本模组真实绑定（`WEAPON_SWITCH.getTranslatedKeyMessage()` = **G**；炮手槽标"炮手/gunner"；读数尾部标 `SHELL_SWITCH` = **B**），非参考图的 1/2/X/G；"反制"槽本模组无对应功能 → 坦克 3 槽、步战 2 槽。
2. 参考图的 ∞ 备弹不适用（本模组备弹有限）→ 显示真实数字。
3. ~~装填弧线为静态~~ → **已补**：`DATA_MAIN_GUN_RELOAD/DATA_MAIN_GUN_RESUPPLY` 已同步，弧线随装填进度填充、补给冷却期间备弹数字变暗。
4. 圆角/阴影未复刻：工程无圆角绘制原语，『小鸟』UI 亦为方角 + 青色顶边，按"沿用既有风格"保持方角。
5. 血量出现两处（读数环形 + 既有左下 `HULL` 面板）→ **明确不补（保持现状）**：按"不删除已有元素"的约束保留左下面板；是否合并需用户决定，未决定前不动。

证据：`item6_slotbar_854x480.png`、`item6_slotbar_1280x720.png`、`item6_slotbar_closeup.png`（两种窗口尺寸下均在右下角、未遮挡热键栏与聊天）。
构建：`./gradlew build` BUILD SUCCESSFUL，单测 **300 / 0 failures**，JAR `e23a5cc8…`（447498 B）与实例哈希一致、实例内 1 份。

## 追加：火炮打击效果查证 + 同轴机枪并列到驾驶员

**查证结论（阶段一/二，只读）**：120mm 主炮的现值**就是本工程自己的规格**——
`docs/plan/ground-units-plan.md` §3 数值映射表：`主武器 | 120mm 主炮：直击 80、对机体 40、溅射半径 3、5 待发，射击间隔约 2.5s`；
`副武器 | 同轴机枪：沿用 Apache 链炮数值，300 发`；而 §4 标题是 **「已拍板决策（2026-09-12，用户确认）」**（第 4 条即"接受 §3 草案映射"）。
代码现值与之逐项吻合：`LittleBirdConfig.java:359/362/365/368/371/374/377`（80/40/3.0/50/5/15/60）、`:380/383/386/389`（同轴 8/3/300/2）。
→ **数值一律未改**（依据：项目内既有规格 + 用户已确认；按硬约束"不得凭手感拍档"）。
如需**强于规格**，属新的平衡决策，需用户给目标值。

**本轮实际补齐的缺口**：同轴机枪此前**仅炮手座可用**（`WeaponSelection.forTankDriver()` 旧值为 {主炮, 烟幕}）。现改为驾驶员可选：{主炮, **同轴**, 烟幕}，复用既有 `AmmoPool coax` + `coaxCooldown` + `fireCoax`，与炮手**共用同一条弹链与热量时钟**（未新增独立开火逻辑）。
改动文件：`entity/weapon/WeaponSelection.java`（驾驶员允许槽 + javadoc）、`entity/MainBattleTankEntity.java`（驾驶员同轴开火分支）、`client/GroundHud.java`（同轴槽图例改用武器切换键）、`test/.../WeaponSelectionTest.java`（2 例随语义更新）。
回退点：`docs/plan/backups/2026-09-12_pre-coax-driver/`（三个文件 sha256 已记录）。

**实机证据**：`item7_driver_coax.txt` + `item7_driver_coax_hud.png`
| 阶段 | WeaponSlot | CoaxAmmo | 主炮待发 |
| --- | --- | --- | --- |
| A 驾驶位默认 | 0 主炮 | 300 | 5 |
| B 按 G 切同轴 | **1 同轴** | 300 | 5 |
| C 连发 1.5s | 1 | **285** | **5（不变）** |

构建：`./gradlew build` BUILD SUCCESSFUL，单测 **300 / 0 failures**，JAR `04c6a9d3…`（447546 B）与实例哈希一致、实例内 1 份。

## 追加：120mm 主炮弹种化 + 大口径表现（阶段 1 定位与方案）

用户 Q1–Q6 答复映射（不重复追问）：
- Q1 → 120mm 主炮；AP/HE 必须可区分。
- Q2 → **对装甲伤害**：AP **40**、HE **25**（即我上轮 Q2 问的就是"对机体/装甲伤害"这一参数）。
- Q3 → 我上轮 Q3 问的是**溅射半径**（当时提议 3.0 → 3.5–4.0）→ 取**增大**：HE 用区间上界 **4.0**，AP 保持规格值 3.0。
- Q4 → 炮口大口径火球（替换子弹样式）、命中处方块破坏 + 烟尘、完整音效、**无震屏**。
- Q5 → 风格对齐 BF2042。
- Q6 → 我上轮问的是"是否允许新增音效/贴图" → **允许**。

关键文件与行（现状基线）：
| 角色 | 位置 |
| --- | --- |
| 弹种枚举（现 MPAT/HEAT） | `entity/weapon/MainGunAmmoType.java:14,16`；翻译键 `:30` |
| 每弹种弹仓/切换 | `entity/weapon/MainGunAmmo.java`（已实现，AP/HE 各自待发+备弹） |
| 主炮数值配置 | `LittleBirdConfig.java:359-377`；访问器 `:638-655` |
| 主炮开火入口（现 hitscan="子弹样式"） | `MainBattleTankEntity.java:263-268 fireMainGun`；同轴 `:270-273`（**保持不动**） |
| 投射物范式 | `entity/LittleBirdRocketEntity.java:32-210`（tick/命中/explode/音效/粒子）、`entity/weapon/LittleBirdRocketBallistics.java` |
| 火球尾迹范式 | `entity/ApacheMissileEntity.java:234-238`（CAMPFIRE_COSY_SMOKE+SMOKE+**FLAME**）、`weapon/ApacheMissileTrail.java` |
| 实体注册范式 | `registry/ModEntities.java:63-79`；渲染器注册 `client/ClientModEvents.java:47-60` |
| 音效注册/资源 | `registry/ModSounds.java:16-20`（engine/gun/gun_shot/rocket_launch/explosion）；`assets/battlefield/sounds.json` + `sounds/*.ogg` |
| 方块破坏范式 | `LittleBirdRocketEntity.java:160-163`（`rockets_break_blocks` → TNT/NONE）；配置范式 `LittleBirdConfig.java:310,552` |
| 子弹样式曳光（仅主炮停用） | `client/LittleBirdTracerRenderer.java`；发包 `network/ModNetwork.java:71,85` |
| 震屏 | 上一轮已核实**全库无实现** |

阶段 2–8 落点：**2** `MainGunAmmoType` AP/HE + lang + HUD 已有弹种显示；**3** `LittleBirdConfig` 新增 `main_gun_ap/he_vehicle_damage=40/25`、`main_gun_ap/he_splash_radius=3.0/4.0`（并为直击伤害留同构键）；**4** 新建 `MainGunShellEntity`（服务器权威，仿火箭）+ 注册 + 复用 `LittleBirdRocketRenderer`，主炮不再发曳光；**5** 命中 `explode(..., ExplosionInteraction.TNT)`（新键 `main_gun_breaks_blocks` 默认 true）+ `EXPLOSION_EMITTER/LARGE_SMOKE` 烟尘；**6** 音效链路用现有 5 个 ogg **分层+变调**（开火=gun_shot 低音+explosion、飞行=rocket_launch 循环/低音、命中=explosion）——**本机无 ogg 编码器**（ffmpeg/sox/oggenc/opusenc 全缺），故不新增音频文件，这是合法替代方案；**7** 复核无震屏；**8** 证据。

### 阶段 2–3 已完成：AP/HE 弹种化 + 数值落配置

- **弹种**：`MainGunAmmoType` 由 `MPAT/HEAT` 改为 **`AP("ap")` / `HE("he")`**；lang 键 `hud.battlefield.shell.ap`=穿甲弹/AP、`shell.he`=高爆弹/HE；HUD 读数直接显示（实机截图见右读数 `120mm HE`）。
- **数值（全部在配置）**：AP 沿用原键 `main_gun_damage=80` / `main_gun_vehicle_damage=40` / `main_gun_splash_radius=3.0`；新增 HE 键 `main_gun_he_damage=80` / **`main_gun_he_vehicle_damage=25`** / **`main_gun_he_splash_radius=4.0`**（Q3"增大"取我上轮提议区间上界）。访问器改为按弹种取：`LittleBirdConfig.tankMainGunDamage/VehicleDamage/SplashRadius(MainGunAmmoType)`；`MainBattleTankEntity.fireMainGun` 按当前弹种取值。旧键名未删除 → 既有配置文件继续可用。
- 测试：`MainGunAmmoTypeTest`/`MainGunAmmoTest` 随改名更新；**300 / 0 failures**。

**实机证据**（`item8_shell_damage.txt`、`item8_shell_ap_he_hud.png`）：对装甲靶（IFV 90 血）
- **AP**：90 → **50**（Δ **40**，= `main_gun_vehicle_damage`）
- **HE**：90 → **65**（Δ **25**，= `main_gun_he_vehicle_damage`）
复现：进驾驶位 → 沿炮线 8 格召唤 IFV → 开火 → `/data get entity @e[type=battlefield:infantry_fighting_village,limit=1] Health` → 杀掉重召 → 按 **B** 换弹种 → 再开火。

构建：BUILD SUCCESSFUL，JAR `afa029a0…`（447919 B）与实例一致，已同步。

### 阶段 4–5 已完成：120mm 弹丸 + 炮口火球 + 命中破坏/烟尘

新增：`entity/MainGunShellEntity`（服务器权威弹丸，仿 70mm 火箭：整tick扫掠、命中即引爆）、`entity/weapon/MainGunShellBallistics`（纯规则：初速 10 格/t、重力 0.02、寿命 60t；+3 单测）、`client/MainGunShellRenderer`（**故意什么都不画**：火球才是视觉）。
改动：`MainBattleTankEntity.fireMainGun` 由 hitscan 改为**生成弹丸**（同轴 `fireCoax` 与其它武器**仍为 hitscan，未动**）；`ModEntities` 注册 `main_gun_shell`；`ClientModEvents` 注册其渲染器；`LittleBirdConfig` 新增 `main_gun_breaks_blocks`(默认 true，Q4) 与 `main_gun_particle_scale`(默认 1.0，预案要求的可调粒子量)。
表现：炮口 `EXPLOSION_EMITTER`+18×FLAME+10×LARGE_SMOKE；飞行中每 tick FLAME+SMOKE 火球、每 3t 一次变调破空音；命中 `EXPLOSION_EMITTER`+EXPLOSION+LARGE_SMOKE 烟尘 + `ExplosionInteraction.TNT` 方块破坏 + `EXPLOSION`(1.6/0.7)。

**踩坑（已修）**：新实体类型未注册渲染器 → 渲染分发器把 null 交给视锥检查 → 客户端 NPE 崩溃（`run/crash-reports/crash-2026-09-12_19.06.40-client.txt`）。加 `MainGunShellRenderer`（空渲染）后复测无新崩溃。

实机证据（`item9_shell_impact.txt`、`item9_shell_impact.png`）：石墙存在（`BEFORE_STONE`）→ 开火 → **`AFTER_WALL_GONE`（方块被摧毁）**；截图见炮口白色闪光+火焰与弹着爆炸粒子；主炮不再有曳光 streak。
**未能验证**：本 headless 机无法打开 OpenAL（`Error starting SoundSystem`）→ 音效无法试听，阶段 6 只能用代码+配置佐证。

构建：BUILD SUCCESSFUL，**303 / 0 failures**，JAR `db7af7bd…`（455870 B）与实例一致、已同步。

### 追加：让炮弹"看得见"（用户反馈"现在这个看不到"）

原设计只有火球粒子、弹丸本身不画模型（空渲染器）。按要求补上**可见实体**：
- 新增 `client/MainGunShellModel`（飞镖式弹体：弹身/锥形风帽/铜弹带/尾缘/四片尾翼，机头朝 -Z，64x64 UV）+ `textures/entity/main_gun_shell.png`（**自制** 64x64 调色：钢/铜/深色，脚本生成，无第三方素材）。
- `client/MainGunShellRenderer` 由"空渲染器"改为**真渲染**：按飞行 yaw/pitch 绘制；**HE 弹放大 1.15× 并偏暖色**，AP 为 1×，正好成为弹种的第二个可辨识点。
- **弹速 10 → 5 格/t（100 m/s）**：保证出膛后看得见，而不是 6 tick 就飞出屏幕。
- `ClientModEvents` 注册 `MainGunShellModel.SHELL_LAYER`。

证据：`item10_shell_entity.png`（弹体模型悬停于石墙前：弹身+铜环可见）、`item10_shell_entity.txt`；
`/summon battlefield:main_gun_shell` 可直接生成且**无崩溃**；JAR 内含贴图（已核对）。构建 BUILD SUCCESSFUL，303/0，JAR `5bfe9d89…` 已同步。
**仍难取到的证据**：单帧"飞行中"的弹丸（5 格/t + 截图 0.3–0.5s 延迟），以上为同一实体的静止画面。

## 未做 / 待办（每条都已归类：已补 / 明确不补+理由 / 只有用户或环境能补）

### 已补（本节此后不再重复列出）
- **装填弧线接真实进度**：新增同步字段 `DATA_MAIN_GUN_RELOAD/RESUPPLY`，弧线随装填填充、补给中备弹变暗（原"装填弧线为静态"条目作废）。
- **弹种差异落地**：`MainGunAmmoType` 改为 **AP/HE**，对装甲 **40/25**、溅射 **3.0/4.0**（原"MPAT 与 HEAT 差异待确认"条目作废）。
- **主炮两条冷却**：`main_gun_reload_ticks=60`、`main_gun_auto_reload=true`、`main_gun_resupply_ticks=100`、`main_gun_resupply_batch=1`。
- **右下角 UI 两处错位 + 快捷栏安全区**：读数与槽条同宽同左缘、车名行不再溢出、面板按与快捷栏的重叠量上移（证据 item15/16/17/18）。

### 明确不补（附理由）
- **震屏**：工程内没有任何震屏框架，实现它等于新建机制；且用户明确要求"不要震屏"。
- **飞机（小鸟/阿帕奇）视角的实机回归**：属飞行器模块，不在"地面载具细节阶段"范围内。
- **参考图的圆角/阴影**：工程无圆角绘制原语，『小鸟』UI 亦为方角 + 青色顶边，按"沿用既有风格"保持方角。
- **血量双显示合并**：按"不删除已有元素"保留左下 `HULL` 面板；合并与否属设计决定，未决定前不改。

### 只有用户或环境能补齐（不是代码缺陷）
- **烟幕占位数值**（2 发 / 20s / 10s / 4.0 格）与 AP/HE 之间的数值差异：等用户给目标值；不编造 BF2042 官方数据。
- **炮手位离轴准星的实机画面**：需第二名玩家；本机 LAN + 第二客户端在 Forge dev 启动器下起不来（已记录）。
- **`models/` 的 STL/参数导出件**仍是强化前盒集：是否重导由用户决定（属独立交付物）。
- **`docs/plan/implementation-status.md` 的「已知取舍（待确认）」章节**：历史文档，是否收口由用户决定。
- **准星散布占位值**（`GroundHud.java:50-52` 的 `COAX/AUTO_SPREAD_PLACEHOLDER`）：坦克同轴是"弹链+冷却"没有热量概念；步战的 30mm 有热量但未同步到客户端。要真正驱动散布需要先定"用哪个量"（热量还是冷却比例）→ 属设计决定，暂标不补。
- **烟幕占位值**（`LittleBirdConfig.java:429/749`）与上述准星占位值已在代码注释里写明原因（BF2042 未公开数值 / 等待设计决定），符合"只剩被明确标注并写明理由"的收口标准。

## 炮塔俯仰限位与旋转抽搐（坦克主炮）

用户裁定：目标 = **坦克主炮**；抽搐现象 = **驾驶时炮塔跟随视线发抖**。

### 改动
1. entity/ground/MainGun.java:17-22（**权威来源**，全工程唯一一处定义坦克俯仰止点）：
   MAX_ELEVATION_UP_DEG 10.0 -> 30.0、MAX_ELEVATION_DOWN_DEG 20.0 -> 10.0。内部单位就是度；
   Minecraft 约定 pitch 正 = 向下，故实际 clamp 区间为 **[-30, +10]**（仰 30 / 俯 10）。
   引用点只有 clampPitch() 与 atElevationStop()，没有第二处需要同步。
2. entity/MainBattleTankEntity.java（原 378-380 行）：**删除客户端的每帧炮塔角覆盖**
   （entityData.set(DATA_TURRET_YAW/PITCH, driver.getYRot()/getXRot())）。服务器用 MainGun
   缓动+限位后经 publishAim（:230-231）下发，客户端又把**未限位、未缓动**的原始视线角写回同一
   同步字段，两者每 tick 互踩 = 抽搐的根因。现在炮塔姿态**只有一个写入源**（读档恢复 :662-663
   是一次性的，不参与每帧）。
3. MainGunTest.theMountStopsAtThirtyUpAndTenDown：把 30/10 与 clamp 行为钉成数字断言。

### 证据
- 改动前：./gradlew test **312 / 0**；改动后：**313 / 0 failures / 0 errors**（新增 1 例）。
- ./gradlew build BUILD SUCCESSFUL；JAR 8ce2927e03990fd4... 与实例 mods 内哈希一致。
- 限位数值的**可复现证据 = 单元测试**（pitch +90 -> +10；pitch -90 -> -30；两止点 atElevationStop 为真；
  0 度为假）。
- 回退：docs/plan/backups/2026-09-12_pre-turret-stops/（3 文件 + sha256）；上一版 JAR 71731b30...。

### 本轮**未取到**的证据（如实标注）
- 实机画面：俯仰到底的截图在车体内取景**全黑**不可用；18 格外的 20 格高墙 + 弹着高度探针本轮
  **没有命中**（HIT_y 无输出），因此「炮塔实测停在 -10/30」目前只有**单元级**证据，缺实机读数。
- 抽搐的**时序前后对比**（录屏/连拍）本轮未取到；根因与消除是**代码级**证据（双写已删、只剩服务器单写）。
- 复现建议（给你自查）：驾驶中快速左右甩视线 -> 炮塔应平滑跟随（略有一拍服务器延迟），不应抖。

### 第 1 轮补充：实机数值探针的结果与**未决风险**（重要）

用 /data get entity @e[type=battlefield:main_battle_tank,limit=1] TurretPitch|TurretYaw 读**权威**角度
（addAdditionalSaveData 会写出 turret.aimYawDegrees()/aimPitchDegrees()）：

- 结果：**全部读数都是 0.0**，即使把视角朝上/朝下、左右扫 8 次之后仍然 0.0。
- 两种解释本轮**无法区分**：
  (a) 无头 rig 的输入没真正改变玩家视角——每次按 t 开聊天都会**松开鼠标**，随后的 mousemove_relative
      可能没旋转视角；而 /tp ... <yaw> <pitch> 会被客户端每帧上报的真实视角覆盖（这是第一轮读数全 0 的原因，已定位）。
  (b) 服务器侧炮塔**确实没有跟随驾驶员**：tickTurret() 在 tickServer():170 每 tick 调用，desired 取
      driver.getYRot()/getXRot()；若 driver() 解析不到驾驶员，就退化成「对准车体朝向」= 0。
- **风险（必须标注）**：若 (b) 成立，本轮「删除客户端预览」会让**炮管不再跟随鼠标**，比抽搐更糟。
  回退只需恢复 MainBattleTankEntity 里那 3 行（备份 docs/plan/backups/2026-09-12_pre-turret-stops/ 已存）。
- 判定 (a)/(b) 的干净办法（下一轮）：① 鼠标保持抓取（先点一下再移动），移动→读值交替，中途不经过聊天；
  ② 或在 tickTurret() 加一行**临时**日志打印 driver != null 与 driver.getXRot()（跑完即撤）；
  ③ 或「朝上开炮」看炮弹是否沿炮管仰角飞出（炮弹方向取自 turretDirection()，与驾驶员视线无关）。

### 第 3 轮：实机验收证据（临时 System.out 探针，跑完已撤）

探针在 tickTurret() 里每 20 tick 打印一次，字段：passengers / driver / driver.getXRot() /
desired.pitchDegrees() / turret.aimYawDegrees() / turret.aimPitchDegrees()。

**边界实测（权威值，非目视）**
- 抬头到底：driver.getXRot()=-90.0 -> desiredPitch=-30.0 -> turretPitch=**-30.0**（新仰角上限）
- 低头到底：driver.getXRot()=+90.0 -> desiredPitch=+10.0 -> turretPitch=**+10.0**（新俯角上限）
- 边界稳定性：连续多个采样都精确停在 -30.0 / +10.0，没有在止点附近来回跳（即边界不抖）。

**水平连续性**：向右连续转 16 步（累计 > 360 度）期间，turretYaw 单调收敛到视线角并停在
-179.9999（wrap 边界），过程中没有数值跳变、没有反向回弹。

**抽搐根因的最终判定**：探针证明服务器侧 driver=true、炮塔本来就在跟随并限位；用户看到的跟随完全
来自被删掉的客户端预览，而抽搐正是「客户端未限位/未缓动的原始视线角」与「服务器缓动+限位值」两个写入源
每 tick 互踩。删掉第二写入源后只剩服务器单写，故该改动是**修复**而非回归。

**早期两次读数全为 0.0 的原因**（已定位，非缺陷）：探针 passengers=0 -- 我那两次探针里玩家**没有成功
上坦克**（一次是 /tp 改视角被客户端覆盖，一次是误按 Escape 打开了暂停菜单）。

**本轮仍未取到**：抽搐的时序前后录屏/连拍（只有机制级证据 + 单写入源证明）。

---

## 追加：炮射霰弹（120mm CANISTER）散布改为椭锥（2026-09-14）

需求：以准心水平线为基准，**上下 2°、水平 5°** 的角度散布。

改前现状：CanisterShot 用 **5° 半角圆锥**（LittleBirdBallistics.spreadDirection，按面积均匀）；
且 GroundWeapons.fireCanister 收了 spreadDegrees 参数却**从未使用**（方向一直取 CanisterShot 常量 5.0）——本次一并修掉。

改动：
- **CanisterShot**：HORIZONTAL_HALF_DEGREES=5.0、VERTICAL_HALF_DEGREES=2.0；direction 改为两轴独立采样
  （样本 0.5 = 准心轴，0/1 = 负/正半角）；新增纯函数 horizontalAngleDegrees / verticalAngleDegrees。
- **GroundWeapons.fireCanister**：签名 spreadDegrees -> horizontalSpreadDegrees + verticalSpreadDegrees，
  并真正用于每颗弹丸方向。
- **MainBattleTankEntity**：传两个配置值。
- **LittleBirdConfig**：main_gun_canister_spread_degrees(5.0) 拆成
  main_gun_canister_spread_horizontal_degrees(5.0) 与 main_gun_canister_spread_vertical_degrees(2.0)。
  **注意**：旧配置键改名，已有配置文件会回退到默认值。
- **CanisterShotTest**：重写，断言水平上限 5°、垂直上限 2°、0.5/0.5 命中准心轴、0/1 落在边界、全采样不越界。

验证与同步：
- ./gradlew build -> BUILD SUCCESSFUL；**413 tests / 0 failures / 0 errors**。
- JAR 同步 sha256 f5b4802484814aedcb0af23a84a328c1991cf75bbb5eae774ad4d8858773fd4d，实例内仍单份。
- 待实机：游戏内目视弹着散布（WSL 无 runClient）。
