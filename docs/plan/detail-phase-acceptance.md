# 细节阶段 阶段④ 验收清单（可复现）

范围：地面载具武器功能 + HUD（对标 BF2042 **信息架构**；数值为占位，未复制其素材）。
全部命令在 `/root/projects/little-bird-forge` 下执行；证据在 `docs/plan/evidence/detail-phase/`。

## 1. 主炮：弹种与数量管理
- 复现：召唤坦克 → 进驾驶位 → 开火（左键）→ 按 **B** 换弹种 → 再开火 → 按 **R** 装填并等 3 秒。
- 观测（`/data get entity @s RootVehicle`）：`Ammo_mpat_Ready` 5→4；`ShellType` 0→1；`Ammo_heat_Ready` 5→4 而 MPAT 不变；R 后 HEAT 待发 4→5、备弹 15→14。
- 证据：`item1_shell_carousel.txt`。结论：**两弹种互不干扰 + 装填可完成**（顺带修复了"地面载具从不推进装填计时"的既有缺陷）。

## 2. 同轴机枪：独立弹药/冷却/准星
- 复现：驾驶位开火多次 → `Ammo_mpat_Ready` 变化而 `CoaxAmmo` 恒为 300（独立）。
- 准星：驾驶位显示**主炮重十字**；F1 隐藏 GUI 后消失（证明由本 HUD 绘制）。
- 证据：`item1_shell_carousel.txt`（CoaxAmmo 恒 300）、`item2_main_gun_reticle.png`、`item2_reticle_hidden_by_f1.png`。
- **未实测（已知缺口）**：**炮手位**的离轴同轴准星（把炮塔线投影到屏幕）需要第二名玩家占据座位 1；单机无法实现。其几何已由 `GroundReticleMathTest`（方位角回绕/FOV 边界/间隙钳制/宽高比）覆盖。尝试过"LAN + 第二客户端"，本机 Forge dev 启动器下第二实例因 mixin 资源解析失败无法启动，故如实标注。

## 3. 烟雾弹：数量/冷却/持续
- 复现：进驾驶位 → 按 **G** 切到烟幕槽 → 开火 → 再开火 → 等 20 秒。
- 观测：`WeaponSlot` 0→2；`SmokeCharges` 2→1；`SmokeCooldown` 343；冷却中再扣扳机**不消耗**；冷却归零；`Ammo_mpat_Ready` 全程 5（槽位互不干扰）。
- 证据：`item3_smoke.txt`、`item3_smoke_cloud.png`（粒子云）。
- 说明：**MC 无体积雾，不承诺遮挡视线**；2 发/20s/10s 为**占位数值**。

## 4. HUD（坦克）
- 复现：进驾驶位 → 观察右下武器栈与左下"车况"；`/damage @e[type=battlefield:main_battle_tank,limit=1] 95`。
- 观测：三行武器栈（`1 120mm` 弹种+待发/备弹、`2 同轴` 300+`炮手`、`3 烟幕` 余量+冷却秒数/READY），选中行高亮；车况 `HULL 100%` → 受损后红色 `LOW HULL 21%`。
- 证据：`item4_hud_full.png`、`item4_hud_weapon_stack.png`、`item4_hud_low_hull.png`。

## 5. HUD（步战 EBAA 小野猫）
- 复现：清场（`/kill` 两型载具，避免误上残留实体）→ 召唤 IFV → 进驾驶位 → 开火 → `/damage ... 60`。
- 观测：NBT `Health 90 / AutocannonAmmo 600 / AtgmReady 2 / AtgmReserve 6`；HUD 两行（`1 30mm 600 驾驶员`、`2 ATGM 2/6 炮手`）；受损 `LOW HULL 33%`。
- 证据：`item5_ifv_hud_full.png`、`item5_ifv_weapon_stack.png`、`item5_ifv_low_hull.png`。

## 构建/同步状态
`./gradlew build` BUILD SUCCESSFUL；单测 **297 / 0 failures**；JAR `8dcc9a61…`（444006 B）与实例 mods 内 JAR 哈希一致、实例内仅 1 份。

## 待用户确认（占位数值）
1. 烟幕：2 发 / 20s 冷却 / 10s 持续 / 4 格半径。
2. MPAT 与 HEAT 的差异（目前两弹种数值一致，仅弹仓分离）。
3. 是否要补"换座位"能力，以便单人也能验证炮手位准星。
