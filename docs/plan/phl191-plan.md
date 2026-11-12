# PHL-191 箱式远火（8×370）— 实现方案与落点

## 1. 用户要求

1. 能发射；
2. 火箭仓（发射箱）随抬射角度抬升；
3. 发射出的火箭有明显曳光；
4. 火箭跟随视角；
5. 瞄准时有落点提示。

## 2. 模型与几何

- 源模型：`/root/blender_m1a5/phl191`（Blender，10.90 m × 3.06 m，8 管）。
- 转成 Minecraft 箱体模型：`client/Phl191Model`，16 单位/格，车头 −Z，y 从地面起；
  所有箱体按 Blender 原始米制 min/max 声明，由 `add/addLauncher` 换算，避免两套坐标漂移。
- 关键尺寸集中在 `entity/Phl191Geometry`：耳轴 (−3.02 m, 1.70 m)、箱前端 2.10 m、
  箱 7.10×1.24×1.28 m、管距 0.56 m、四轴 3.95/1.55/−1.05/−3.55 m。
- 发射箱是 `launcher` 子部件，枢轴 = 耳轴；渲染时设 `launcher.xRot` 即整组箱体+框架抬起，
  与 `Phl191Geometry.muzzle()` 的炮口位置用同一角度。
- 贴图：`tools/generate_phl191_texture.py` 生成 1024² 数码迷彩（4 色双尺度）、
  128×64 火箭贴图、32×32 物品图标；模型 UV 由 `uvFor` 按箱体尺寸自动落进迷彩区。

## 3. 玩法与网络

| 项 | 实现 |
| --- | --- |
| 座席 | 1 席（驾驶员兼操作手），`GroundSeat.PHL191_DRIVER` |
| 驾驶 | 复用 `GroundDrive`（客户端预测，服务端不移动，与坦克一致） |
| 抬射 | **Z 抬升 / X 下降**（按住；`Phl191ElevationPacket` id 40 只在状态变化时发），服务端按 2.5°/tick 收敛到 5–65°，写入 `DATA_ELEVATION`；不再跟随视线俯仰 |
| 开火 | 左键（`GroundWeaponInputPacket` 复用），按住每 6 tick 打一发，8 管循环 |
| 下车 | **Shift+X**（X 已让给“降低发射箱”，裸 X 不再下车） |
| 装填 | 8 发打空后 **400 tick（20 s）** 自动补满；装填期间 **2 秒一轮的 QTE 读条持续循环**，每轮 **1.6–1.8 s** 内按 R 奖励 **−15 秒**并结束读条；读条期间按错（其它时刻）→ 重置回 20 秒重新读条；不按则读条一直循环到装填完成。读条只在命中或装填结束时消失 |
| 弹道 | `Phl191Ballistics`：初速 2.0 格/tick，推力 0.06×12 tick，巡航 2.72，重力 0.05 |
| 齐射散布 | **出射方位角与车头航向的偏差**为主 + 小量**出射仰角公差**补前后：8 管排成 4×2 网格（横向 −6/−2/+2/+6 格 × 仰角 −0.4°/+0.4°），仰角公差经装填仰角限位后用来算实际射程，再把横向槽位换算成偏航角（`yawOffsetForLateral`），所以横向落点始终约 ±6 格、前后有小幅错开；`phl191_spread_radius`（默认 6，0 关闭）、`phl191_pitch_tolerance_deg`（默认 0.4，0 关闭） |
| 火箭实体 | `Phl191RocketEntity`：真实飞行、逐 tick 命中检测、爆炸（半径 7，伤害 14/12/10）+ **地形爆破**（默认半径 5 格、深约 3 格，`phl191_breaks_blocks` / `phl191_crater_radius` 可调，复用 CraterShape，抗爆>100 保留，单发上限 20000 块） |

## 4. 曳光与相机

- **曳光**：火箭每 tick 在尾喷口发 FLAME + LAVA，沿尾迹 2.2 格补 CAMPFIRE_COSY_SMOKE/SMOKE；
  按玩家逐人广播到 256 格（原版批量包只到 32 格，飞远了会断）。
- **跟随视角**：`client/RocketCam` —— 驾驶员开火后在 48 格内捕获最近的火箭，镜头挂在
  火箭后方 7 格、上方 2.4 格，朝向取火箭速度方向，FOV 72；火箭消失后保持 1 s 再交还，
  最长 260 tick 兜底。位置由 `B2PodCameraMixin` 在 `Camera#setup` 末尾改写，
  角度/FOV 走 `ViewportEvent`，与 B-2 投弹相机同一套机制。

## 4.5 战术雷达（M）

- 按 M 在左下角键位提示上方开一块 最大 190x190 的圆形雷达（窗口矮时自适应缩小），**以车体为中心、半径 140 格**：
  - 所有存活生物/载具 = 红点（载具点更大），每 4 tick 刷新；四道环分别对应 35/70/105/140 格；
  - 预测落点 = 金色十字（超出 64 格时贴边显示）；
  - 车头 ±15° 内最近的目标 = 白圈 + 连线，旁边给出该距离所需射击俯仰角；
  - 底部仰角条（5°–65°）+ 当前仰角数字。
- 纯数据：client/Phl191Recon 读世界与载具状态，Phl191Hud.drawRadar 用 GuiGraphics 画（圆盘/环/点/线全部用 fill 拼），
  不依赖任何离屏渲染或纹理抓取；雷达打开时左侧仰角面板移到右边，避免遮挡。
- 纯规则：Phl191Ballistics.rangeAt(elevation) 与 requiredElevation(distance)（与实际弹道同一条代码，
  5°–65° 每 0.5° 搜索），3 个单测钉住射程曲线与反解。

## 5. 落点提示

- `client/Phl191Aim`：用**同一份** `Phl191Ballistics` 在客户端逐 tick 步进（含方块 raycast），
  得到落点、距离、飞行 tick；`AimProjection` 投到屏幕。
- `client/Phl191Hud`：屏幕落点画环形+十字标记，下方显示「距离 m / 飞行 s」；
  左侧面板显示仰角、余弹、装填倒计时；底部提示键位；跟随视角激活时显示「火箭跟随视角」。
- 服务端与客户端共用同一弹道代码，标记与真实落点不会分叉（与 70 mm 火箭的做法一致）。

## 6. 文件

新增：`Phl191Geometry`、`weapon/Phl191Ballistics`、`Phl191Entity`、`Phl191RocketEntity`、
`client/Phl191Model`、`client/Phl191Renderer`、`client/Phl191RocketModel`、
`client/Phl191RocketRenderer`、`client/Phl191Aim`、`client/Phl191Hud`、
`client/RocketCam`、`item/Phl191DeployItem`、`tools/generate_phl191_texture.py`、
`Phl191BallisticsTest`、贴图 3 张、物品模型 1 个。

改动：`ModEntities`、`ModItems`、`ModCreativeTabs`、`ClientModEvents`、
`LittleBirdConfig.phl191DriveSpec()`、`GroundSeat`、`B2PodCameraMixin`、
`lang/en_us`、`lang/zh_cn`。

## 7. 验证

- `./gradlew test`：8 个 `Phl191BallisticsTest` 用例（方向/限位/收敛/推力/重力/炮口八管）全绿。
- `./gradlew build`：资源校验 + reobf + 测试 + 打包通过，`syncLocalMods` 已同步，
  JAR 与实例内 SHA-256 一致（单份）。
- 待客户端实测：贴图/UV 观感、跟随相机手感、落点标记与实际弹着的一致性。

## 8. 已知简化

- 抬射由 Z/X 键直接驱动（2.5°/tick），没有手感曲线或独立手轮轴。
- 8 管齐射按固定 6 tick 间隔，没有配置化射击时长；装填固定 30 s。
- 车体没有履带/悬挂姿态拟合（平坦地面），斜坡上的姿态仍为平地。
- 支腿、伪装网卷等细节未进 MC 模型（箱体级简化，保留识别特征）。
