# 模型重做阶段日志（参考图驱动）

目标：`goal-e277badf-8236-4d4d-ab16-4e7e34f3a2a5`
参考图清单：`docs/plan/ground-units-reference-images.md`
流水线约束：**不跑 `./gradlew build`**（避免覆盖 Windows 侧 JAR）；只用 `compileJava` + `runClient`。

## 回退点（阶段纪律要求）

目录：`docs/plan/backups/2026-09-12_pre-remodel/`（改动前的 4 份源文件副本）

| 文件 | 改动前 sha256 |
| --- | --- |
| MainBattleTankModel.java | `b2f79cad4ecd0ff05471659b19ada69c0aabc928fd2e9513689d4e24c1d5a53a` |
| InfantryFightingVehicleModel.java | `d258163548592fdcc97ad2bd5289b62682aa1a1699d3536d71b751e448df9a4e` |
| MainBattleTankGeometry.java | `51beb145d30332b687755884880988a9f713b73ee08793a4be5d8c0f5c1f829a` |
| InfantryFightingVehicleGeometry.java | `d240004a77f29619d1b0e9cd694b3f9aba72bafc10c24fe34b4d1bf3f68ccafb` |

回退方法：把上述 4 份文件复制回 `src/main/java/io/github/littlebird/` 对应位置即可。几何类本次**未改**（单位/朝向/AABB/座位不变）。

## 阶段 1：结构定形（已完成）

改了什么（只改 `createBodyLayer()` 的结构，不动单位/朝向/用法）：
- **坦克** `client/MainBattleTankModel.java`：阶梯式首上/首下装甲、上下两层车体、尾部发动机甲板与后板；**每侧 7 个负重轮**、后置主动轮、前置诱导轮、两侧履带包络；炮塔（含尾舱）、防盾、120mm 炮（炮身+热护套+炮口）、车长塔、装填手舱盖。addBox 由 8 处增至 22 处（含 7 轮循环 → 运行时 34 个盒）。
- **步战** `client/InfantryFightingVehicleModel.java`：**由 4 轮两轴改为 6×6 三轴**（修正结构级错误）、加高箱式车体、前风挡带、车头防撞杠、车顶左侧导弹箱 + 右侧遥控武器站 + 观瞄。addBox 由 8 处增至 15 处（运行时 20 个盒）。

证据（`docs/plan/evidence/ground-units-model/`）：
- `p1_tank_side.png`、`p1_tank_threequarter.png`、`p1_ifv_side.png`、`p1_ifv_threequarter.png`

## 阶段 2：中观细节（已完成）

改了什么：
- **坦克**：两侧各 6 段侧裙、抛射式烟幕发射器（炮塔两侧）、车长机枪（枪座+枪管）、两根天线、炮长瞄具、风传感器、发动机甲板 3 条格栅、前照灯×2、拖钩×2、排气管。
- **步战**：两侧各 3 个侧挂储物箱、驾驶舱侧窗与后视镜、前照灯×2、导弹箱 4 个发射管面、武器站观瞄与弹药箱、车顶双舱盖、天线、尾门把手×2、发动机格栅。

证据：`p2_tank_side.png`、`p2_tank_threequarter.png`、`p2_ifv_side.png`、`p2_ifv_threequarter.png`

编译：`./gradlew compileJava` → BUILD SUCCESSFUL（仅既有 3 条 LittleBird.java deprecation 警告）。

## 阶段 3：微观细节 + 最小 UV/配色（已完成）

改了什么：
- **坦克**：履带纹块（每侧 9 块）、每个负重轮轮毂（14 个）、侧裙螺栓条、炮塔顶面板缝、车长塔 3 个潜望镜、炮管抽烟装置、发动机甲板改为 8 条格栅叶片。
- **步战**：轮毂（6）、防撞杠 4 根竖杆、风挡框、导弹箱管口环、储物箱铰链、尾门中缝、舱盖铰链、观瞄镜片。
- **贴图**：新增 `tools/generate_ground_unit_textures.py`，生成 512×512 分区调色板（橄榄/深橄榄/钢/履带黑/炮钢/护套沙/灯黄/玻璃/烟幕红），两个模型的 texOffs 全部落区（审计：texOffs-outside-palette=0）。

证据：`p3_*.png`（配色前）、`p4_*.png`（修正后最终渲染）。

## 阶段 4：核验与清理（已完成）

- **编译**：`./gradlew compileJava` → BUILD SUCCESSFUL（仅既有 3 条 LittleBird.java deprecation 警告）。
- **共面重叠（z-fighting 风险）审计**：脚本按盒体轴对齐面做同向共面判定（炮塔子件已加 +34/+36 偏移）。
  - 首次：坦克 25 处、步战 4 处 → 逐项修正（前照灯外移、轮/主动轮/诱导轮外移、裙板螺栓外移、排气管外移、炮塔板缝移位、抽烟装置移到护套外、导弹箱缩短 1 单位）。
  - 复跑：**步战 0 处；坦克 1 处**（`engine_deck` 与 `turret` 底面在 z16–20 共面）——两者底面都贴在 `hull_upper` 顶面（y=34）之下，被车体遮挡，外部不可见，判定为可接受并留档。
- **非流形/破面/游离顶点**：Minecraft `LayerDefinition` 盒模型由引擎按立方体生成四边面，不存在自由顶点或非流形边；客户端实机渲染无破面。
- **单位/朝向**：`MainBattleTankGeometry` / `InfantryFightingVehicleGeometry` **未改**（16 单位=1 格、机头 -Z、+X 为右、AABB/座位不变），渲染器未改。
- **比例偏差**：参考图车身长:高 = 1202:555 = **2.17:1**；模型车体长 100 单位、履带底到炮塔顶 50 单位 = **2.00:1**，偏差 **7.7%**（模型略短/略高，属盒模型包络取舍）。

## 交付清单

### 参考图清单（来源 URL + 用途）
见 `docs/plan/ground-units-reference-images.md`（R1 Wikimedia 正交侧视 / R2 swiki M1A5 3/4 / R3 Fandom Wildcat 3/4 / R4 装备界面 / R5 真实 EBRC Jaguar）。

### 已实现
坦克：上下车体、阶梯首上/首下、发动机甲板、**7 对负重轮 + 主动轮 + 诱导轮 + 履带纹**、6 段侧裙、炮塔含尾舱、炮盾、120mm（炮身/热护套/抽烟/炮口）、车长塔+机枪+潜望镜、装填手舱盖、烟幕发射器、双天线、炮长瞄具、风传感器、格栅、前照灯、拖钩、排气管。
步战：**6×6 三轴**、高箱式车体、风挡+框、前防撞杠（含竖杆）、车顶左导弹箱（4 管）+ 右遥控武器站（枪/弹药箱/观瞄）、侧挂储物箱×6、侧窗、后视镜、前照灯、车顶双舱盖、天线、尾门中缝与把手、发动机格栅。

### 未实现 / 受限（需你知悉）
1. **材质仍是最小分区调色板**，不是美术级贴图（按你选的 5A 范围）。
2. **无正交正/背/俯视参考图** → 车宽、炮塔俯视外形、车顶布局为 3/4 图估算。
3. 参考图被伪装网/植被遮挡的小件（天线座、把手细节）只做了可见轮廓。
4. 待裁定：导弹箱左右侧（R3/R4 视角受限）；M1A5 生存性 75 vs 80（数值不改，仅留档）。
5. **未同步**：全程 **未运行 `./gradlew build`**，因此**未覆盖 Windows 侧 JAR**；需要同步时请明确下令。

### 证据目录
`docs/plan/evidence/ground-units-model/`：`p1_*`（阶段1）、`p2_*`（阶段2）、`p3_*`（阶段3 配色前）、`p4_*`（最终）、`compare_tank_side.png`、`compare_ifv_threequarter.png`（参考图 | 渲染 同视角对比）。

---

## 追加：细节强化轮 A（坦克，用户选 A"游戏内精细"路线）

回退点：`docs/plan/backups/2026-09-12_pre-detail-pass/MainBattleTankModel.java`
（sha256 `d9a4f90adda77ce045ae1cfeabf0cbcc108d924d1065d19d987de8e953cfeeec`）

改了什么（仍为 Java `LayerDefinition` 盒模型，格式/单位/朝向未变）：
- **首次使用旋转子部件**（`PartPose.offsetAndRotation`，弧度）：首上装甲改成铰接在车顶线、下倾 38° 的真实斜板；炮塔两侧各一块偏航 30° 的楔形颊甲。
- 数量：`addBox` 调用点 70 处，**运行时约 233 个盒**（旧版 117）。
- 新增：TUSK 式侧向 ERA 阵列（每侧 12 块）、18 节履带链、负重轮轮胎+轮毂、主动轮/诱导轮履带齿、6 段侧裙（带螺栓）、炮塔泄爆板（4 块留缝）、车长塔 6 个潜望镜环、.50 机枪（枪座/机匣/枪管）、装填手舱盖+M240、两侧各 6 管烟幕发射器、尾舱框架与行包、炮长主瞄具、风传感器、GPS、双天线、拖车缆、随车工具、CIP 板、把手、前后灯。

证据：`a2_side.png`、`a2_threequarter.png`、`a2_side_close.png`、`a2_threequarter_close.png`，
以及同机位前后对比 `compare_tank_detail_before_after.png`、`compare_tank_detail_34_before_after.png`。

已知短板（下一步可继续）：
1. **负重轮仍不显眼**：轮子与履带共用深色贴图区，侧裙又遮住上半，7 对负重轮这个 M1 特征读不出来 → 应给轮/毂单独贴图区并让裙板下缘抬高。
2. **炮塔偏小**：相对车体，炮塔体量比参考图小，应整体放大 10–15%。
3. **贴图仍是 9 色平涂**：近距离看很素；换自制实贴图（面板线/铆钉/迷彩/编号/做旧）是视觉提升最大的一步。
4. **导出件滞后**：`models/` 里的 STL/参数仍是强化前的盒集，待外观定稿后需重新同步。

---

## 追加：压瘪 + 炮管加长（用户反馈"坦克需要更瘪一点，炮管加长一点"）

回退点：`docs/plan/backups/2026-09-12_pre-flatten/`
（`MainBattleTankModel.java` sha256 `97feaff165622eb81aa11e9a7fab8e38bc5325c5bf5dfe6cf22231bb1e5c7567`；
`MainBattleTankGeometry.java` sha256 `51beb145d30332b687755884880988a9f713b73ee08793a4be5d8c0f5c1f829a`）

改动（格式/单位/朝向仍未变）：
- **压瘪**：车体顶 34→**28** units，车体下 26→22、上 22→28（高 8→6），发动机甲板 34→28，炮塔枢轴 34→**28**，炮塔体高 16→14，车长塔/泄爆板/烟幕/瞄具/天线等全部随之下移；侧裙高 16→12；ERA 27→22。
- **同步几何常量**：`MainBattleTankGeometry`：`HULL_TOP_UNITS` 34→28、`TURRET_PIVOT_HEIGHT_UNITS` 34→28、`DRIVER_SEAT_HEIGHT_UNITS` 30→24、`GUNNER_SEAT_HEIGHT_UNITS` 34→28（保证乘员坐姿与炮口起点跟着模型走）。
- **炮管加长**：炮管 z −74→**−92**（长 48→66），热护套 −60→−78（长 20→22），抽烟装置 −66→−86，炮口 −78→**−96**；`BARREL_LENGTH_UNITS` 56→**96**，使服务器端炮口起点与视觉炮口一致。

比例变化（单位制，车体长 100 units）：`长:高(到炮塔顶)` 由 **2.00:1 → 2.38:1**（参考图 3.25:1，仍偏方，但方向正确且更接近）。

证据：`b_side.png`、`b_threequarter.png`、`b_side_close.png`；
同机位前后对比 `compare_tank_flatten_before_after.png`、`compare_tank_flatten_34_before_after.png`。

**碰撞体同步（响应"改尺寸时同步更新碰撞体"）**：视觉炮塔顶由 3.13 格降到 2.63 格后，原 AABB 高 2.4 格不再覆盖炮塔；
已把 `MainBattleTankGeometry.AABB_HEIGHT_BLOCKS` 由 **2.4 → 2.6**（宽度 3.4 未动，本次只改了 Y 向尺寸）。
实体位置（贴地）不受影响：履带底面仍在模型 y=0，与实体原点重合。

验证：`./gradlew compileJava test` → **BUILD SUCCESSFUL**；单测 **264 / 0 failures**；客户端实机运行无新增报错。

未做：**没有跑 build、没有同步 Windows 侧 JAR**（按同步纪律，需你明确下令）；`models/` 导出件仍未同步。

---

## 追加：步战车细节强化轮 A（与坦克同规格）

回退点：`docs/plan/backups/2026-09-12_pre-ifv-detail/InfantryFightingVehicleModel.java`
（sha256 `1db8c910bc2d901cc3bdf0c3c5edf676592ba8c0418c5f6d7c4ab77a2d2bf420`）

改动（格式/单位/朝向/尺寸全未变；几何类未改，座位/碰撞不受影响）：
- **首次在步战上使用旋转子部件**：车头首上板改为铰接在车体前缘、上仰 22° 的斜板（`addGlacis`）。
- 盒数：`addBox` 调用点 37 → **68**（运行时约 61 → **约 140**）。
- 新增：每轴**轮拱**（3 轴×2）、两侧各 **3 段侧裙带螺栓**、**尾门铰链×4**、前后**拖钩×4**、**前照灯护罩**、**后视镜支臂**、**驾驶舱 4 个潜望镜**、炮塔两侧共 **6 管烟幕发射器**、**导弹发射管口框 ×4**、武器站**瞄具镜片**、炮塔**舱盖与线缆**、车体**两条拖缆**、**油桶×2**、**排气管**、**天线座**。

证据：`c_ifv_side.png`、`c_ifv_threequarter.png`、`c_ifv_side_close.png`；
同机位前后对比 `compare_ifv_detail_before_after.png`、`compare_ifv_detail_34_before_after.png`。
编译：`./gradlew compileJava test` → BUILD SUCCESSFUL。

---

## 追加：用 Blender 源模型替换游戏内坦克建模（2026-09-14）

目标：goal-f27dc563-76e3-41fc-b686-1965ecabe024。用户决策：表示方式 = **LayerDefinition 盒模型（F-35 同款）**；
尺寸 = **采用真实 M1A2 尺度并同步 Geometry**；顺带**刷新 models/**。

### 流水线（可重复）
- blender_m1a5/export_mc.py：从 m1a5.blend 导出每个部件的 AABB（16 单位/格，Y 上、机头 -Z，米→单位），
  含炮塔/车体分组、纹理分区、履带环参数、船体五边形剖面与炮塔放样截面 → tools/data/m1a5_boxes.json。
- tools/generate_ground_unit_model.py：确定性生成 client/MainBattleTankModel.java（**173 盒**）：
  船体/炮塔用旋转装甲板重建斜面，履带用空心环带（顶/底/前后端盖，长条切段以满足贴图跨度），
  内置 texOffs 越界审计；并生成 models/src/ground_units_tank_data.py。
- tools/generate_ground_unit_textures.py：坦克改用新的 TANK_ZONES（512×160 olive 容纳真实尺度车体盒），
  新增 roller/hub 分区让 7 对负重轮与轮毂可辨；IFV 沿用原 IFV_ZONES，输出**逐字节不变**。

### 几何（MainBattleTankGeometry，真实尺度）
HULL_LENGTH 112→**126.88**、HULL_WIDTH 56→**58.56**、HULL_TOP/炮塔环 28→**26.24**、BARREL 96→**92.88**、
TRACK_HEIGHT 8→**15.52**、AABB 3.4→**3.7**；驾驶员/炮手座位与两挺机枪炮口按 Blender 实测重标
（TankMachineGunMuzzleTest 同步更新，容差 1e-9→1e-6，以容纳 float 常数与 double 期望的 ~2e-8 差）。

### 验证
- ./gradlew compileJava → BUILD SUCCESSFUL（仅既有 3 条 deprecation 警告）。
- ./gradlew test → BUILD SUCCESSFUL，**413 tests / 0 failures**。
- models/src/build_models.py + verify_models.py → **VERIFY PASSED**：tank 173 盒 / 2076 三角面，
  bbox 3.66×4.34×9.77 格；IFV 仍 61 盒 / 732 三角面（未动）。
- texOffs 越界审计：**0**。
- 预览证据：docs/plan/evidence/ground-units-model/mc_lf.png、mc_side.png（Blender 重建的 MC 盒模型）、
  models/build/main_battle_tank_side.png。

### 回退点
docs/plan/backups/2026-09-14_pre_blender/：MainBattleTankModel.java、MainBattleTankGeometry.java、
generate_ground_unit_textures.py + SHA256SUMS。

### 同步（2026-09-14 23:26，用户下令）
- 已运行 `./gradlew build` → **BUILD SUCCESSFUL**，`:syncLocalMods` 已把 JAR 复制到 PCL2 实例
  （`local_mods_dir=/mnt/c/Users/19818/AppData/Roaming/.minecraft/versions/1.20.1-Forge_47.4.20/mods`）。
- 核对：`build/libs/battlefield-0.1.0.jar` 与实例副本 **sha256 一致**
  `b3c1e9e9e8f50f38706417258e5c26b4209d2661d076bd28f2f5b483ae3c2e8a`，大小 773565 B；
  实例 mods 内 battlefield/littlebird JAR **仅 1 份**（无重复 mod id）；
  JAR 内 `main_battle_tank.png` 与源文件 sha256 一致（`a8dd69e7…`），并含新的
  `MainBattleTankModel.class` / `MainBattleTankGeometry.class`。
- `:test` 为 UP-TO-DATE（上一次 413 tests / 0 failures）。

### 未做 / 待实机
- **runClient 在本 WSL 环境无法启动**：./gradlew runClient 在 LWJGL 初始化阶段失败——
  "Platform/architecture mismatch ... Platform available on classpath: windows/x86"，
  java.lang.UnsatisfiedLinkError: Failed to locate library: liblwjgl.so。
  这是**迁移遗留的环境问题**（Gradle 缓存里只有 Windows/x86 的 LWJGL natives），与本模型改动无关：
  compileJava 与 413 条测试均通过。完整报错存于
  docs/plan/evidence/ground-units-model/runclient_wsl_failure.txt。
- **游戏内目视待做**：贴图 UV 对齐、炮塔与轮组比例、两挺机枪炮口位置需实机截图确认。
  当前以 Blender 重建的 MC 盒模型渲染（mc_lf.png / mc_side.png）作为最接近的替代证据。

---

## 追加：rev2 造型修正（对比官方渲染后，2026-09-14）

用户反馈"建模好肥、不够帅"，要求与网上参考图对比后调整。

### 参考图（新增于 blender_m1a5/refs/）
- BF2042 M1A5 官方 3/4 渲染：swiki（https://bf2042.swiki.jp/index.php?M1A5 → M1A5.jpg，1920x607）与
  Fandom（File:BF2042-M1A5.png，1311x655）——游戏版权，**仅作造型参考，不随产物分发**。
- 真实 M1A2 照片：Wikimedia Commons **Public Domain**（M1_Abrams_noBG.jpg 等）——可用于比对。

### 对比后确认的"粗糙/偏肥"点（旧版）
1. **车体太高、炮塔太小**——旧版车顶 1.64 m、炮塔只有 0.80 m 高，读起来像"大盒子顶小帽子"。
2. 首上装甲偏陡偏短，没有 M1 的长浅斜面。
3. 侧裙是等高等长的平板，前沿/后沿没有让出诱导轮与主动轮。
4. 缺 BF2042 M1A5 的显著特征：炮塔前侧 6 管烟幕发射器、右侧遥控武器站。
5. 炮盾偏小且位置靠里。

### 本次改动（blender_m1a5/build.py）
- 车体顶 1.64 → **1.52 m**；首上斜面向后延伸（NOSE_Y+0.50 → NOSE_Y+1.55），坡度变缓。
- 炮塔放大加长：前缘 -1.90 → **-2.10**，后缘 1.10 → **1.30**，最宽 2.72 m，塔高 0.80 → **0.92 m**；
  炮盾前移加宽（宽 1.10）。
- 侧裙下缘分段阶梯（前 0.92、后 0.94，中间 0.66–0.76），让出诱导轮/主动轮。
- 新增每侧 6 管烟幕发射器 + 右侧遥控武器站（RWS）。
- 几何同步：TURRET_PIVOT/HULL_TOP/GUNNER_SEAT 26.24 → **24.32**；
  COAX_MG_HEIGHT 16.32 → **18.24**；CUPOLA_MG_HEIGHT 18.24 → **20.16**（TankMachineGunMuzzleTest 同步）。
- 修正一处失误：烟幕发射器一度放在炮塔宽度之外（bbox 曾涨到 4.81 m），已收回塔侧，bbox 恢复 3.66 m。

### 验证与同步
- ./gradlew test → BUILD SUCCESSFUL（413 tests / 0 failures）。
- models：188 盒 / 2256 三角面，VERIFY PASSED；bbox 仍 3.66 x 4.34 x 9.77 格。
- ./gradlew build → BUILD SUCCESSFUL，JAR 已同步，sha256
  47d72a4e90a5389d47979c9e7fdb1aa749542606128f728193f5479e53ec22b8，774090 B，实例内仍单份。
- 预览：docs/plan/evidence/ground-units-model/mc_lf.png、mc_side.png（新）；Blender 渲染
  blender_m1a5/renders/rev3_lf.png、rev3_side.png。

### 仍偏粗糙、下一轮可做
- 炮塔前角缺斜切"颊板"（现在是台阶状，M1 的斜面颊甲读不出来）。
- 炮塔顶面偏"平台化"，缺面板缝/舱盖细节。
- 负重轮轮毂仍是方块，履带无单节板。
- 裙板无面板线/螺栓；无 BF2042 的伪装网、编号涂装（无公开色值来源，未编造）。
- 尾部动力舱格栅、排气管简化。

---

## 追加：细节强化轮 rev3（2026-09-14）

依对比文档 docs/plan/ground-units-blender-compare.md 的建议 1→5 执行：

- **炮塔**：生成器改为按放样截面逐段出盒（步进更细）+ 每侧 3 块 yaw 斜板**颊甲**，前部台阶改成斜面。
- **侧裙**：新增 skirt 贴图分区（TUSK 式 **ERA 方块阵列** + 面板缝 + 螺栓），只改贴图，不加几何、不超 3.66 m 宽。
- **烟幕发射器**移到炮盾两侧（各 6 管、朝前）；新增车长独立热像仪 **CITV**（左）与遥控武器站（右）。
- 塔侧每侧 4 块**备用履带板** + 长**储物轨**；**尾舱加大**（宽 2.3→2.5 m、深 0.95→1.15 m、高 0.42→0.52 m）。
- 车首**大灯改长条**并加中央**橡胶挡板**。

结果：Blender 175 对象 / 4416 三角面 / 几何哈希 5ca932fe；MC 模型 208 盒；bbox 仍 3.66 x 9.77 x 4.34 m。

验证与同步：
- ./gradlew test -> BUILD SUCCESSFUL（413 tests / 0 failures）。
- models：208 盒 / 2496 三角面，verify VERIFY PASSED。
- ./gradlew build -> BUILD SUCCESSFUL，JAR 同步 sha256
  342a8fcdefde5d5bdcb093662214edb61d2953aa164b42aea40a9ea343455a37，775643 B，实例内仍单份。
- 预览：docs/plan/evidence/ground-units-model/mc_lf.png / mc_side.png / mc_front.png；
  blender_m1a5/renders/rev4_lf.png / rev4_side.png。

仍待下一轮：炮塔顶面舱盖/面板缝细节、负重轮毂方块化、单节履带板、涂装（迷彩/编号无公开色值来源，不做）。

---

## 追加：把精细化重建的 Blender 模型接进 MC（2026-09-15）

- export_mc.py 升级：新增**尺寸/名称过滤**（min 0.03 m；跳过 Bolt/Rivet/TrackPin/Tooth/Hinge/Spring/
  Peri/Strap/MRS/LiftEye/CamoNet/Shock）；炮塔前缀扩展；履带环判定改为 M1A5_Track_*；
  车体剖面由 HullLower+HullUpper 合成（车体已改两段式），半宽走 profiles.hull_half。
- generate_ground_unit_model.py：按 kind=complex 跳过；车体半宽取自导出。
- 结果：Blender 727 对象 -> 过滤后 385 部件 -> MC **418 盒**（旧版 208 盒）。
- 几何同步：炮塔环/车顶/炮手座 24.32 -> **23.04**（车顶 1.44 m）；两挺机枪口按新模型重标
  （coax 侧 -15.68 / 高 18.88 / 管 8.80；cupola 侧 13.76 / 高 20.8 / 管 25.6），TankMachineGunMuzzleTest 同步。
- 贴图按新分区重生成（IFV 不变）。
- 验证：**413 tests / 0 failures**；build SUCCESSFUL。
- 同步：JAR sha256 `aeec9fb85a17bc6d0414a395e30c5b1c08f2da68702ba1a7f2d64bbbbb8f606d`，782880 B，实例内单份。
- 取舍：微件（螺栓/履带销/铆钉/铰链/潜望镜/鞭天线/伪装网）未进 MC 模型，MC 侧靠贴图与近似盒表达。
- 未做：游戏内目视（WSL 起不了 runClient，LWJGL natives 只有 windows/x86）。

