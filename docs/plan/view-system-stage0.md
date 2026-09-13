# 视角系统修复：阶段 0 查证（只读，未改代码）

目标 goal：`goal-f5735739-fcd1-43bb-973d-4bff3f742b49`

## A. 视角/相机现状（**全部只对飞行器**）

| 项 | 现值 | 位置 |
| --- | --- | --- |
| 第三方 FOV 倍数 | **1.35**（0.5–2.5） | `LittleBirdConfig.java:178`，访问器 `:494` |
| 座舱 FOV 倍数 | **1.00**（0.5–2.0） | `:181`，访问器 `:498` |
| FOV 应用点 | **唯一一处**：`LittleBirdCamera.computeFov`，且**仅当骑 `LittleBirdEntity`**（阿帕奇没有 FOV 钩子） | `LittleBirdCamera.java:26-36` |
| 相机滚转 | `LittleBirdCamera.java:38-48`、`ApacheCamera.java:23-33`（各自飞行器） | — |
| 视角切换 | `COCKPIT` 键（默认 **C**）：`LittleBirdInput.java:113`、`ApacheInput.java:101`；状态 `cockpitView`：`LittleBirdEntity.java:136,361-367`、`ApacheEntity.java:162,386-392` | — |
| 座舱相机数学 | `LittleBirdCameraMath`、`ApacheCameraMath` | — |
| 阿帕奇第三人称距离 | 9.0 格 | `LittleBirdConfig.java:186` |
| **地面载具（坦克/步战）** | **完全没有相机/FOV/视角类**；client 侧只有 `GroundHud`、`*Model`、`*Renderer` | 目录核对 |

## B. 准心 / 火控标记现状
- 地面 HUD（本会话新建 `GroundHud`）：**驾驶位 = 屏幕中心的居中十字**（`:81-82`，即"相机前向"）；**炮手位 = 把炮塔线投影到屏幕的离轴准星**（`:86-89`、`drawProjectedReticle:188-211`，含 `withinFov`）；IFV 同构（`:143-151`）。
- 可复用原语（`GunshipHudDraw`）：`crosshair / hitMarker / enemyHealthBar / rocketReticle / apacheRocketReticle / alerts`。
- 地面载具目前**没有**"炮管准心（区别于相机前向）"与"火控标记"这两个概念。

## C. 玩家模型可见性
- 唯一隐藏机制：`ApacheRiderRender`（`RenderPlayerEvent.Pre → event.setCanceled(true)`，**仅当 `getVehicle() instanceof ApacheEntity`**）`ApacheRiderRender.java:24-29`。
- 小鸟走另一套：`LittleBirdModel.COCKPIT_HIDDEN_PARTS`（`:57,75-77`）+ 输入侧处理。
- **地面载具无任何 rider 渲染/隐藏代码**：玩家模型由原版渲染，第三人称能看到自己坐在车上。
- 载具模型是纯 `EntityModel`（渲染），碰撞体来自 `EntityType.sized(...)`（`ModEntities`）→ **隐藏玩家只动渲染，不动碰撞**。

## D. 待确认项（阻塞阶段 1–3 的设计选择）
1. **范围**：修**地面载具**（坦克/步战），还是连飞行器一起？
2. **"炮塔内视角"不存在**：方案 A 新增地面"炮塔内相机"（复用 C 键 + 新 `GroundCamera`）；方案 B 把现有第一人称驾驶视角当作炮塔内视角。选哪个？（我倾向 A）
3. **准心对齐**：驾驶位是否改为**投影炮管实际指向**（炮塔偏转时准心偏离视线）？还是保持居中、炮塔线仅作附加标记？（我建议以炮管指向为准）
4. **火控标记元素清单**：现有语义只有"准星 / `hitMarker` / `enemyHealthBar` / 火箭落点"。请勾选要哪些（例：炮管指向准心、装填/冷却环、弹种读数、测距距离、命中标记、目标血条）。
5. **第三方 FOV 默认值**：建议新增 `ground_third_person_fov_multiplier` 默认 **1.35**（与飞行器一致，范围 1.0–2.5），仅地面载具非炮塔内视角生效。确认？
6. **隐藏玩家范围**：只在炮塔内视角隐藏，还是第三方也隐藏？（阿帕奇是"骑着就隐藏"；我建议炮塔内隐藏、第三方保留）
7. **相机碰撞**：炮塔内相机位于车体内，原版相机方块碰撞会把镜头拉近/穿墙抖动——是否授权对该视角**关闭相机方块碰撞**？

## 阶段 1 已完成：地面载具第三方视角大 FOV

采用默认（用户未回 7 项确认）：范围=**地面载具**、FOV 默认 **1.35**、仅该视角、飞行器键不动。

改动文件与内容：
- `LittleBirdConfig.java`：新增 CLIENT 键 **`ground_third_person_fov_multiplier`**（默认 **1.35**，范围 1.0–2.5，注释说明只作用于地面载具外部相机、不影响炮塔内视角与飞行器）+ 访问器 `groundThirdPersonFovMultiplier()`。
- `client/GroundCamera.java`（新）：FORGE 总线 `computeFov`，**仅当骑 `MainBattleTankEntity`/`InfantryFightingVehicleEntity`** 时生效 → 与 `LittleBirdCamera`（飞行器）完全分离。
- `client/GroundViewMath.java`（新，纯规则）+ `GroundViewMathTest`（4 例）：飞行器返回 1.0、地面外部取地面键、炮塔内取内视键（阶段 2 用）、非法配置回退 1.0。

证据：`item11_fov_compare.png`（左 1.0 / 右 1.35，同车同机位，FOV 差异肉眼可辨）、`item11_ground_fov.txt`（含配置文件三次核对：飞行器键 1.35 全程未动）。
构建：BUILD SUCCESSFUL，单测 **307 / 0 failures**，JAR `9ba574dc…`（461219 B）已同步，实例内 1 份。

未决项：阶段 2（炮塔内视角与火控标记元素）、阶段 3（隐藏玩家范围）仍待你在阶段 0 的 D1–D7 上确认；当前按我的默认推进。

## 阶段 2 已完成：炮塔内视角（炮管准心 + 火控标记，仅该视角）

改动：
- `client/GroundView.java`（新接口）：`isInteriorView()/setInteriorView()`，纯客户端标志（不同步）。
- `MainBattleTankEntity` + `InfantryFightingVehicleEntity`：实现该接口；`positionRider` 在内视时把乘员座位**抬高 `ground_interior_camera_lift_blocks`（默认 0.60）** → 相机落在炮塔圈内。
- `client/GroundVehicleInput`：**复用既有 COCKPIT 键（C）** 切换内视，并切到 `FIRST_PERSON`（退出时回 `THIRD_PERSON_BACK`）。
- `LittleBirdConfig`：新增 `ground_interior_fov_multiplier`（默认 1.00，0.5–2.0）与 `ground_interior_camera_lift_blocks`（默认 0.60，0–2.0）——**与外部视角 FOV 分开的键**，两者互不拖动。
- `client/GroundHud`：内视时**只画火控**——准心画在**炮管指向**（投影炮塔 yaw/pitch，不是屏幕中心）、**炮线首个方块的距离（米）**、命中标记 `hitMarker`、以及炮管偏离视线 ≥1° 时的 `GUN xx°` 读数；**外部视角一律不画这些**。

证据：`item12_interior_fire_control.png`（内视：准心 + **18 m** 测距 + 弹药/车况面板）、`item12_exterior_no_fire_control.png`（按 C 回第三人称后火控全部消失，只剩外部准星）；`GroundViewMathTest` 钉住 FOV 归属（外部键/内视键/飞行器 1.0）。
构建：BUILD SUCCESSFUL，**307 / 0 failures**，JAR `ddd34285…` 已同步。
未验证：炮手位变体需第二名玩家；`GUN xx°` 离轴读数因炮塔随动过快未在单帧中捕获。

## 阶段 3 已完成：隐藏玩家自身模型

改动：`client/GroundRiderRender.java`（新）——`RenderPlayerEvent.Pre` 在玩家骑 `MainBattleTankEntity`/`InfantryFightingVehicleEntity` 时 `setCanceled(true)`，与既有 `ApacheRiderRender` 同一范式。**只动渲染**：车体/炮塔/炮管/其它单位/HUD 不受影响；碰撞体来自 `EntityType.sized(...)`，与渲染无关。

证据：`item13_rider_hidden_compare.png`（左=改前，骑手头/背露出炮塔顶；右=改后，车上无玩家模型）、`item13_view_switch_stable.png`（C/F5 各三轮反复切换后仍稳定：内视火控仍显示 18 m、无残留模型、无新崩溃）；日志错误只有本 headless 环境固有的 flite/OpenAL 两条。

**与阶段 0 默认的差异（需你知悉）**：我把隐藏做成**两种视角都生效**（与阿帕奇一致），而不是"仅炮塔内"——因为原版第一人称本来就隐藏自机模型，若只在内视隐藏等于没做；而第三人称下骑手确实从炮塔顶露出来。若你要"第三人称保留身体"，改成 `view.isInteriorView()` 门控即可（一行）。

## 追加：视角对齐"小鸟"（按用户"视角和小鸟一致"）

对照后改掉 3 处不一致 + 确认 1 处本就一致：
1. **FOV 键统一**：删除 `ground_third_person_fov_multiplier` / `ground_interior_fov_multiplier`；`GroundCamera` 改为直接读小鸟同款 **`third_person_fov_multiplier`（外部）/ `cockpit_fov_multiplier`（内视）**。
2. **内视抬升统一**：删除 `ground_interior_camera_lift_blocks`；`positionRider` 改用共享的 **`cockpit_eye_lift_blocks`（0.40）**。
3. **相机模式统一**：`GroundVehicleInput.applyCameraMode` 照搬 `LittleBirdInput` 语义——上车记住玩家原相机并强制第三人称；C 切内视→FIRST_PERSON；离开时恢复记住的相机（原本是 FIRST_PERSON 则回落 THIRD_PERSON_BACK）。
4. **骑手隐藏**：`GroundRiderRender` 与 `LittleBirdInput.onRenderPlayer` **同一条规则**（骑着就取消玩家渲染）——本就一致。切换键也同为 **C**。

**有意不照搬**（地面载具无对应物，已在证据文件里标注）：相机滚转（飞机随机身倾斜）、座舱隐藏模型部件（MC 背面剔除使炮塔内壁本就通透，且炮管必须可见以对准心）、肩视（右键过肩，飞机专属且你未要求）。

证据：`item14_aligned_interior.png`（内视火控 18 m）、`item14_aligned_camera_restored.png`（下车后相机与玩家模型均恢复）、`item14_view_aligned_with_little_bird.txt`（含改动清单与"没照搬"的理由）；运行期配置只剩小鸟那三个键。构建 BUILD SUCCESSFUL，**307 / 0 failures**，JAR `3cc2e693…` 已同步。

## 追加：修掉坦克右下角 UI 错位

**症状（按像素测量，854x480 截图）**：读数青色顶边 x=566..829（264 px = 132 GUI），武器槽条 x=590..830（240 px = 120 GUI）——右缘对齐、**左缘差 24 px**，读数比它所属的槽条宽出 12 GUI。

**成因**：GroundHudLayout 自带 READOUT_WIDTH=132，而槽条宽是 barWidth(slots)=120，两者都从右 margin 定位；且 readout() 用默认 3 槽计算 Y，两武器车（步战）会把读数压到错误的条高上。

**修法（只动布局，不动绘制）**：删除 READOUT_WIDTH；读数直接取槽条自身矩形（同 x、同宽，上移 READOUT_HEIGHT），新增 readout(w,h,scale,rows)；GroundHud 传入真实 TANK_ROWS/IFV_ROWS；GroundHudLayoutTest 断言读数与槽条**两边都对齐**，并新增两武器车（更窄条）用例。

**修后实测**：读数青色顶边 x=590..829（240 px = 120 GUI），与槽条完全重合；整图青色包围盒 240x124 @ +590,+314 —— 读数/槽位/图例共用一个左缘与右缘。

证据：item15_hud_alignment_compare.png（左=修前 / 右=修后）、item15_hud_alignment.txt。构建 BUILD SUCCESSFUL，单测 **308 / 0 failures**，JAR d6887c6c... 与实例一致。

## E. 结论
三件事在**地面载具上全部属于新增**（飞行器已有 FOV/视角切换/rider 隐藏可作范式）。阶段 0 只读，未改任何代码。
