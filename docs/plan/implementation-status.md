# 实施状态与自审报告（M0-M8）

生成时间：本轮实施结束时。对应目标：按 docs/plan 实现小鸟直升机（Forge 1.20.1）。
> **2026-09-12 迁移记录**：工程已从 `C:/Users/19818/Desktop/newmod/little-bird-forge` 迁移到 WSL 原生
> `/root/projects/little-bird-forge`（ext4）。WSL 装了原生 OpenJDK 17，Gradle 缓存复制到 `/root/.gradle`，
> 构建命令由 `gradlew.bat` 改为 `./gradlew`（第 6 节已更新），`local_mods_dir` 指向 `/mnt/c/...` 下的同一实例路径。
> **其余历史小节保留当时的原始命令与路径**（`gradlew.bat`、`C:/...` 均为迁移前事实）。

## 1. 已完成内容

| 里程碑 | 内容 | 代表文件 |
| --- | --- | --- |
| M0 | Gradle 工程脚手架（Forge 47.4.20 / MC 1.20.1 / official / Java 17），模组可加载 | build.gradle, gradle.properties, LittleBird.java |
| M1 | 纯规则层 + 单测：四轴飞行模型、输入整形、机枪循环、弹道、火箭巢状态 | entity/flight/*, entity/weapon/* |
| M2 | 载具实体：骑乘、客户端权威运动、滚转同步、NBT 持久化、远端插值、网络基座 | entity/LittleBirdEntity.java, network/* |
| M3 | 客户端飞控：鼠标偏航（复用原版鼠标旋转）、A/D 滚转、W/S 总距、X 下机、强制第三人称 | client/LittleBirdInput.java, LittleBirdCamera.java, LittleBirdKeyMappings.java |
| M4 | 模型与渲染：LayerDefinition 机体（旋翼/尾桨/机枪/火箭巢/滑橇）、渲染器、占位贴图 | client/LittleBirdModel.java, LittleBirdRenderer.java, LittleBirdModelMath.java |
| M5 | 双联速射机枪：转速预热、左右管交替 10 发/秒、热度与过热锁定、hitscan、收敛与散布、曳光粒子、命中反馈 | entity/LittleBirdEntity.java（Weapons 段）, LittleBirdGunCycle, LittleBirdBallistics |
| M6 | 70mm 火箭巢：2x7=14 发、单发/齐射两种模式、火箭弹道实体、爆炸伤害、落地自动补给 | entity/LittleBirdRocketEntity.java, client/LittleBirdRocketModel/Renderer |
| M7a | HUD：准星、命中标记、航向带、坡度指示、武器条（热度+14 格弹药）、速度/高度、RPM/机体、告警 | client/LittleBirdHud.java, LittleBirdHudLayout.java |
| M7b | 配置（CLIENT 手感/视角 + COMMON 平衡）、坠机伤害、自动补给、部署物品、创造标签、中英文本地化 | LittleBirdConfig.java, item/LittleBirdDeployItem.java, registry/ModItems, ModCreativeTabs |
| M8 | 全量 build（含 reobf + 测试）、JAR 同步与哈希核对、自审 | 见第 3/4 节 |

## 2. 验证证据

- 单测：`gradlew.bat test` → BUILD SUCCESSFUL；51 个测试全绿。
  - HelicopterFlightModelTest 14、LittleBirdGunCycleTest 7、LittleBirdBallisticsTest 7、RocketPodStateTest 8、
    LittleBirdModelMathTest 5、LittleBirdHudLayoutTest 10。
- 编译：`gradlew.bat compileJava` 多次通过；仅剩 3 条 Forge 47.4.x 的 deprecation 警告
  （FMLJavaModLoadingContext.get()、ModLoadingContext.get()），属已知且有意的选择（同版本参考工程同写法）。
- 专用服务器冒烟：`gradlew.bat runServer` 日志出现
  `Creating FMLModContainer instance for io.github.littlebird.LittleBird`、
  `Enabled Gametest Namespaces: [littlebird]`、`Done (1.825s)!`，无异常；结束后无残留 java 进程。
- 全量构建：`gradlew.bat build` BUILD SUCCESSFUL（compileJava → jar → reobfJar → test → build）。
- JAR 同步：`gradlew.bat build -Plocal_mods_dir="C:\Users\19818\AppData\Roaming\.minecraft\mods"`
  → syncLocalMods 执行成功；源与目标 SHA-256 一致（1915F74D00C1B9891170936FEB660799B2AC8EBA13DDC766E97961F8B739678F），
  JAR 88,556 字节，共 67 个条目（含 mods.toml、lang、模型、贴图）。
- 本地化：代码中出现的 8 个直接翻译键在 en_us/zh_cn 中均存在；两个模式键由三元表达式拼接，也已存在。

## 3. 自审发现并修正的问题

1. 机枪热度按 tick 增长会变成 3 秒过热（与文档的 6 秒不符）→ 改为按“每发”累加，测试锁定 59 发/约 126 tick 过热。
2. 侧滑方向符号写反（右坡度会向左漂移）→ 修正为右坡度向右漂移，并同步修正 flight-control.md 公式。
3. 旋翼未起转时悬停阻尼仍在抵消重力，导致“死旋翼”不会下坠 → 垂直权限与悬停阻尼改为按升力比例缩放，
   现在无升力时会自由下坠并受 maxFallSpeed 限速（测试覆盖）。
4. 火箭连发冷却少 1 tick（实际 6 tick 间隔）→ 冷却改为完整间隔，齐射节奏锁定为 7 tick。
5. 文档里 MC yaw 约定写错（写成 yaw 0 = -Z）→ 按 vanilla `Vec3.directionFromRotation` 修正为 yaw 0 = +Z(南)、90 = -X(西)，
   并在模型数学里用推导式断言锁定（LittleBirdModelMathTest）。
6. 模型枪管用 `PartDefinition` 当 `CubeListBuilder` 传参、以及给 `PartDefinition` 写 `yRot` → 编译期发现并修正。
7. 遗留死代码清理：`LittleBirdModel` 未使用的枪管访问器、`FlightConfig.health`（耐久实际在实体上）、
   火箭实体未使用的 `Player`/`SynchedEntityData` 导入、火箭 `owner()` 占位方法。
8. 配置项 `rockets_break_blocks` 原本无引用 → 接进火箭爆炸的 `ExplosionInteraction` 选择。
9. HUD 的 `headingShift` 参数类型与调用不匹配（float/int）→ 统一为 float；`LittleBirdHud` 构造函数可见性修正。
10. 测试自身错误：火箭管序期望值写错（管序是“每舱独立递增”）、“按 5 tick 间隔两次按键只应发 1 发”的期望未计入冷却，
    以及航向 -1 度应等价于 359 度 → 均按被验证的规则修正期望，而不是放宽断言。

## 4. 与规划的偏差（必须声明的近似与未做项）

1. 曳光：用原版粒子（END_ROD 描线 + SMOKE 枪口烟）实现，未做规划中的自定义线渲染器（原 DroneTracerRenderer 式）。
   计划中列为 M5b 后续切片。
2. 音效：全部使用原版音效事件（弩射击、烟花发射、物品拾取）作为占位；
   尚无引擎/旋翼循环音与机枪连发循环音（需要自制 ogg 资产）。
3. 火箭无独立“直击加成”伤害，命中伤害完全由半径 3 的爆炸提供。
4. 火箭为服务器权威 + 普通实体追踪，没有客户端预测，20 TPS 下高速飞行可能略有跳帧感。
5. 座舱只做 1 个驾驶位；副驾驶/舱门机枪手仍未做（规划中已列为第二版）。
6. 部署物品只有创造标签入口，没有合成配方；`MAX_HEALTH` 是常量，未进配置。
7. 驾驶时隐藏玩家与主手（与参考工程一致），没有渲染“坐进座舱的驾驶员”。

## 5. 仍需游戏内确认的事项（构建通过不等于观感正确）

1. 相机滚转方向：`LittleBirdCamera.CAMERA_ROLL_SIGN` 与 `LittleBirdModelMath.modelRollDegrees` 的符号已用数学推导+单测锁定，
   但“屏幕上的地平线往哪边倒”只能在客户端里看一眼确认；若相反，把 `CAMERA_ROLL_SIGN` 改成 -1.0F。
2. 鼠标偏航手感：偏航限速 180 度/秒、俯仰 120 度/秒、极速 0.9 格/t 是否顺手，用 `config/littlebird-client.toml` 调。
3. 第三人称取景：原版相机是绕玩家旋转的，机体是否挡住视线/旋翼是否遮挡视野需要实测。
4. 真实比例模型观感：旋翼直径 10 格、机身约 7 格、座舱顶 2.6 格；10 格旋翼在树林/建筑间会大量穿插（用户已确认接受真实比例）。
5. HUD 在 GUI scale 1/2/3、F1 隐藏、极窄窗口下的排版；以及航向带刻度与文字是否重叠。
6. 机枪 10 发/秒的“速射感”、3.5 伤害平衡、过热 6 秒与 4 秒锁定是否合适。
7. 火箭弹道（0.75→2.0 格/t、轻微下坠）、爆炸半径 3、落地 20 秒自动补给节奏。
8. 多人：滚转同步到其他客户端、18 格/秒下的远端插值、坠机包校验。

## 6. 构建 / 运行 / 同步命令

    # 2026-09-12 起：全部在 WSL 原生环境里跑（OpenJDK 17，Gradle 缓存 /root/.gradle）
    cd /root/projects/little-bird-forge
    ./gradlew compileJava                        # 快速编译
    ./gradlew test                               # 纯规则单测（本次 247 个）
    ./gradlew build                              # 全量：资源 + 测试 + reobf + 打包
    ./gradlew build -Plocal_mods_dir="<mods 目录>"   # 构建并同步 JAR（会核对大小/时间戳）
    ./gradlew runClient                          # 客户端（需要桌面显示）
    ./gradlew runServer                          # 专用服务器冒烟

## 7. 游戏内验收清单（建议一次性收集）

1. 创造模式标签取出部署物品 → 右键部署 → 右键骑乘。
2. 悬停第三人称截图；压坡度转弯截图（确认镜头滚转方向）；高速掠地截图。
3. 机枪：按住左键的曳光与枪口烟截图；持续射击到“机枪过热”告警与 HUD 热度条变红截图。
4. 火箭：按 2 切到火箭、单发一次截图；按 Z 切齐射长按截图；打空后 HUD 弹药格全暗 + NO ROCKETS 告警截图。
5. 落地静置 20 秒确认自动补给（火箭格恢复）。
6. 撞墙/坠地确认机体耐久下降、低于 30% 时 HUD 闪红与 LOW HEALTH 告警。
7. GUI scale 1/2/3 各一张 HUD 截图，并按 F1 确认 HUD 隐藏。
8. 中英文各一张（确认 HUD/物品/按键名都走本地化）。
## 8. 修正记录："载具隐形" 根因与修复（本轮追加）

### 8.1 现象与取证

- 用户报告：游戏内完全看不到载具。
- 客户端证据：`%APPDATA%\.minecraft\versions\1.20.1-Forge_47.4.20\logs\latest.log`
  （2026-09-10 20:49-20:51，正是 JAR 同步后 5 分钟的会话）显示：
  本模组的 `config/littlebird-client.toml` 与 `littlebird-common.toml` 被创建，**全程零异常、零渲染报错**。
  即模组已加载，问题在模型自身而不是注册链。
- （另一份 `%APPDATA%\.minecraft\logs\latest.log` 是 2026-05-17 的 Fabric/LabyMod 会话，与本模组无关，已排除。）

### 8.2 根因

原模型以实体原点为中心，而原版 `Entity#makeBoundingBox` 让实体位置等于碰撞箱**底面**：

    AABB = [y, y + height]，模型几何 y ∈ [-0.8125, +0.5] 格

于是机身（-0.375 ~ +0.31）与滑橇（-0.81）全部埋在地面以下，地面上只剩约 0.3 格高的灰条，
肉眼判断就是"没有载具 / 没有建模"。

### 8.3 修复

1. 新增纯规则常量类 `entity/LittleBirdGeometry.java`（无 Minecraft 类），把所有高度改为**从地面起算**，
   并用单测锁死不变量：滑橇底面 `SKID_BOTTOM_UNITS == 0`、机体高度 <= 碰撞箱高度、
   座舱宽度 <= 碰撞箱宽度、旋翼高于舱顶、驾驶员眼高低于旋翼盘、武器挂点与模型一致。
2. 按用户确认的**真实比例**重建模型：AH-6 风格 10 格旋翼（4 叶）、约 7 格机身、2.6 格座舱，
   阶梯式收窄机头、分面座舱玻璃与舷窗、发动机舱与排气管、短翼挂架、2 个 7 管火箭巢（红色端盖）、
   2 挺机头机枪（枪管伸出 60 单位）、管式滑橇与支柱、空速管与刀形天线，共约 40 个盒体。
3. 新增旋翼模糊：高于 45% 转速时叠加半透明十字/三角盘（`renderRotorBlur` + `RenderType.entityTranslucentCull`）。
4. 贴图重做为 512x512 手工分区面板图：橄榄蒙皮+铆钉+警示条纹、座舱框架、深色金属与桨尖条、
   火箭巢红端盖、白色维护标记（由 `tools/generate_placeholder_texture.ps1` 生成，可复现）。
5. 碰撞箱与乘坐点改为从常量类取值（2.4 x 2.6，乘坐偏移 0.6），机枪与火箭巢挂点同步到新几何，
   弹道常量改为枪口高度由实体提供、`MUZZLE_DROP = 0`、前伸 2.6 格。

### 8.4 修复后的验证

- 单测：61 个全绿（新增 LittleBirdGeometryTest 7 个、LittleBirdRotorBlurTest 3 个）。
- `gradlew.bat build` BUILD SUCCESSFUL；JAR 91,692 字节，
  SHA-256 `F1EFB5912C92A656B3E5AC2C14D281E9382E1B267257CC9B2EB926B75DE6BA8F`，源与 `.minecraft/mods` 目标一致。
- 本地化 61 个键、模型层与渲染器注册未变。

### 8.5 仍需游戏内确认（本轮修复的验收点）

1. 地面停放时滑橇应正好压在地面（不再半埋）、机身完整可见。
2. 悬停/飞行时机体在碰撞箱上方居中；第三人称能看到整机与旋翼盘。
3. 高速时旋翼模糊盘是否观感自然（若过重，调 `LittleBirdModelMath.ROTOR_BLUR_MAX_ALPHA`）。
4. 10 格旋翼的穿插程度是否可接受（不可接受则回到缩小比例，只需改一个常量类 + 模型尺寸）。
### 8.6 追加修正：缺少登机交互（用户提问「按什么键上飞机」时发现）

- 现象：部署出载具后，右键点击机体没有任何反应，只能靠 `/ride` 命令上机。
- 根因：只实现了骑乘管线（`canAddPassenger` / `getControllingPassenger` / 座位偏移 / 控制输入），
  但没有重写 `Entity#interact`，而原版自定义实体默认不处理上机（船与矿车都是各自重写 `interact` 才可乘坐）。
- 修正：为 `LittleBirdEntity` 增加 `interact(Player, InteractionHand)`：非潜行右键即 `player.startRiding(this)`，
  潜行右键保留给手中物品；服务端执行、客户端只回执 `sidedSuccess`。
- 操作方式：**右键点机体上机**；**X 下机**（可在「按键设置 → 小鸟直升机」里改）。
- 验证：`gradlew.bat test` 61 个测试全绿；`build` 成功；JAR 已重新同步到
  `versions\1.20.1-Forge_47.4.20\mods`，源与目标哈希一致。
## 9. 追加变更：缩小模型 / 正面玻璃 / C 舱内视角 / FOV / 操控阻尼

| 需求 | 实现 | 关键文件 |
| --- | --- | --- |
| 缩小模型 | `LittleBirdGeometry.SCALE = 0.65`，旋翼 10 → **6.5 格**，机身约 4.7 格；渲染器对姿态做同一缩放，碰撞箱/挂点/乘坐点全部由同一常量派生（改一个数字即可再调） | LittleBirdGeometry, LittleBirdRenderer |
| 正面玻璃可透视 | 座舱拆成 **玻璃**（独立半透明 pass，alpha 0.28）与**不透明框架**（顶梁/底梁/A 柱/中柱）；玻璃在 cutout 主 pass 里隐藏，避免双面叠加 | LittleBirdModel, LittleBirdRenderer |
| C 键舱内视角 | 第一人称 + 隐藏包围相机的座舱外壳（保留机头/风挡/框架/机枪/旋翼）；再按 C 回到第三人称 | LittleBirdInput, LittleBirdModel.setCockpitView |
| 第三人称 FOV | `third_person_fov_multiplier` 默认 1.15（舱内 1.00），用 `ViewportEvent.ComputeFov` 缩放 | LittleBirdCamera, LittleBirdConfig |
| 操控阻尼 | 四轴一阶滤波：偏航 0.20 / 俯仰 0.25 / 滚转 0.25 / 总距 0.30（每 tick 应用比例，1.0 = 关闭），纯规则类 + 6 个单测 | HelicopterInputDamping, FlightConfig, LittleBirdConfig |

### 9.1 期间自查发现的自身错误（均已修正）

1. 几何访问器改名后漏改两处引用（`ModEntities` 的 AABB、实体的乘坐点）→ 编译失败，已修正。
2. 批量替换时把替换内容错误地拆成了多个数组元素，导致 `LittleBirdConfig` 被截断（`HUD_SCALE`/`COCKPIT_FOV` 定义丢失、`client.comment` 未闭合）→ 按文件实际内容整块重写修复。
3. 两个阻尼单测期望写错：20 次迭代后收敛到 0.9976 而容差给了 1e-3；另一处把「负向指令下的单调下降」写成了上升 → 修正期望而非放宽实现。

### 9.2 舱内视角的几何限制（诚实记录）

MC 玩家眼高固定为乘坐点 + 1.62 格，而 0.65 比例下座舱只有约 0.81-1.34 格高，因此**默认乘坐点会让眼点位于座舱之上**。
为解决「透过正面玻璃看」，舱内视角使用 `cockpitRiderOffsetBlocks()`（负值，约 -0.54），且**只在客户端生效**：
服务端仍用安全乘坐点（>=0），避免把玩家埋进地面触发窒息伤害；客户端不同步自身位置（乘员位置由客户端自行推算），
因此不产生位置校验问题。不变量已由单测锁定：舱内眼点必须落在风挡带内、默认乘坐点必须非负。

### 9.3 本轮验证

- 单测 **68 个全绿**（新增 HelicopterInputDampingTest 6 个与几何舱内视角断言）。
- `gradlew.bat build` BUILD SUCCESSFUL；JAR 已同步到 `versions\1.20.1-Forge_47.4.20\mods`，源/目标 SHA-256 一致。

### 9.4 待游戏内确认

1. 6.5 格旋翼的穿插程度与手感是否合适（改 `LittleBirdGeometry.SCALE` 一个数字即可）。
2. 舱内视角是否真的「透过风挡」看出去，以及外壳隐藏后是否有漏空观感。
3. 第三人称 FOV 1.15 是否够宽（配置 `littlebird-client.toml` → `view.third_person_fov_multiplier`）。
4. 阻尼 0.20-0.30 是否顺手（想更灵敏就调大，1.0 = 关闭阻尼）。
## 10. 追加变更：舱内视角重做 / 黑色涂装 / 第三方 FOV 拉大

依据：用户提供的实机截图（舱内视角被整个机体包住：风挡贴脸、机翼与火箭巢在两侧、旋翼在头顶）与 BF 风格参考图。

| 问题 | 修正 |
| --- | --- |
| 舱内视角被外壳包住 | 舱内渲染改为隐藏 20 个外壳部件（座舱、顶盖、风挡玻璃、顶梁、中柱、**机头上半**、机翼、火箭巢、尾段、尾桨、主旋翼、滑橇、排气管、天线）；保留机头下半（鼻子甲板）、下颌横梁、左右细立柱、仪表台 + 仪表条、机枪 |
| 风挡玻璃挡视线 | 舱内不再执行风挡玻璃半透明 pass 与旋翼模糊盘 pass（它们仅用于外部视角） |
| 机头横带挡中央 | `nose_upper` 顶部在 y=0.98-1.30 格，正好横在眼高 1.08 前方 → 已加入隐藏清单 |
| 机体不是黑色 | 贴图重绘为石墨黑：机身 30/33/36、烟熏玻璃 22/28/32、深色金属 42/45/48、吊舱黑 + 暗红端盖、枪与滑橇近黑 16/18/20、仪表条浅灰 |
| 第三方 FOV 不够宽 | 默认 1.15 → **1.35**（范围上限放宽到 2.5），并已直接改写实例 `config/littlebird-client.toml` 立即生效 |

新增部件：`dashboard`（仪表台）与 `dash_glare`（仪表条），仅从舱内相机可见。

验证：`gradlew.bat build` BUILD SUCCESSFUL（自动同步）；JAR 97,805 字节，SHA-256 `3F7DFA62…`，
build/libs 与实例 mods 哈希一致。

待游戏内确认：舱内是否只剩天空 + 鼻子甲板 + 细立柱（若机头仍挡太多，可再隐藏 `nose_lower` 或下调 `cockpitRiderOffsetBlocks()`）；
FOV 1.35 是否够大（配置文件可自行改，无需重新构建）。
### 10.1 舱内相机第二轮修正（依据用户第二张实机截图）

- 现象：风挡框（左右立柱 + 下横梁）与仪表台位于 z -1.02~-1.58 格，正好卡在相机前方 1~1.6 格，
  形成画面中央的黑色「马镫」；机腹地板仍铺在脚下。
- 修正 1：舱内眼高 1.077 → **1.544 格**（`COCKPIT_EYE_UNITS = 38`），高于机头甲板顶 0.487 格。
- 修正 2：新增 `COCKPIT_SEAT_FORWARD_UNITS = 42`（约 1.706 格），并重写
  `LittleBirdEntity#positionRider(Entity, MoveFunction)`：舱内视角把乘员（即相机）沿机头方向前移，
  越过风挡框与仪表台，使这些部件退到相机后方与下方；仍为客户端专用，服务端乘坐点不变。
- 修正 3：机腹 `belly` 加入舱内隐藏清单；仪表台抬高到 y 0.81~1.06 格。
- 新增不变量：相机前移量必须大于风挡平面距离，否则风挡框会重新挡住画面中央。
- 验证：`gradlew.bat build` BUILD SUCCESSFUL（自动同步），源/目标哈希一致。
## 11. 追加变更：关自动回正 / 关“自带速度”三连 / 左右武器站分开建模 / 落点预测

| 需求 | 实现 | 关键配置（`config/littlebird-client.toml`） |
| --- | --- | --- |
| 关闭自动回正 | 俯仰回中率与坡度回平率默认 0：松手保持姿态 | `flight.auto_level`（默认 false；设 true 恢复自动回正） |
| 松杆后不再保持前进速度 | 新增空气阻力，且**推力改按操纵输入**而非姿态计算（否则自动回正关闭后机头保持下压会一直推力满） | `flight.horizontal_drag`（默认 0.03/tick） |
| 关闭悬停自稳 | 总距居中不再把垂直速度拉回 0，改为自然下沉 | `flight.hover_assist`（默认 false）、`flight.hover_sink_accel`（0.006） |
| 旋翼自转/模糊盘 | 转速随垂直油门变化（悬停 55%、爬升满速），模糊盘上限 0.35→0.20 且随负载变淡 | `flight.rotor_idle_spin_fraction`（0.55） |
| 左右机架分开建模 | 每侧短翼两个独立挂架：内侧机枪（housing+barrels 独立）、外侧 7 管火箭巢（含红端盖）；挂点与弹道常量全部由 `LittleBirdGeometry` 派生 | — |
| 落点预测 | 世界空间渲染：机枪命中红十字+瞄准线，火箭预测弹道折线+菱形落点；预测与服务器共用 `LittleBirdRocketBallistics` | — |

### 11.1 本轮自检抓到的真实缺陷（均修正）

1. 火箭原沿“当前速度方向”推进：重力使速度低头后水平分量反而衰减 → 改为沿发射轴推进（更接近直瞄火箭，预测弧线也不再摆动）。
2. 重力被施加在新建向量上而非累加，导致竖直速度恒为 −0.012、火箭根本不下坠 → 改为正确的轴向加速 + 重力累加积分。
3. 发动机启动期下坠过猛：2 秒起转期间 vy 累积到 −1.0 格/t（≈20 格/秒，掉十几格）→ 起转提至 0.5 秒、残余重力按升力²衰减，现启动仅轻微下沉（单测锁定 vy > −0.3）。
4. 阻力曾按姿态计算 → 自动回正关闭后阻力永不生效（实测 60 tick 后仍是 0.9 格/t）→ 改为按操纵输入计算。
5. 测试自身两处期望错误（收敛容差过紧、把负向指令的单调下降写成上升），一处角度回绕后仍做比例比较 → 修正期望。

### 11.2 本轮验证

- 单测 **81 个全绿**（新增阻尼 6、火箭弹道 5、旋翼油门 2、起转下沉等）。
- `gradlew.bat build` BUILD SUCCESSFUL（自动同步到 PCL2 实例 mods），源/目标 SHA-256 一致。
## 12. 追加变更：落点改屏幕投影 / 双联分开发射可见化 / 机枪与火箭真实化

| 需求 | 实现 |
| --- | --- |
| 不再画线，改用投影落点 | 删除全部世界空间线/弹道折线；新增纯规则类 `AimProjection`（世界点→屏幕点，含 MC 的 yaw/pitch 约定与相机滚转，6 个单测），`LittleBirdAimRenderer` 每帧把 3 个预测命中点投影成屏幕坐标，HUD 用 `GuiGraphics` 画准星（机枪两个十字 + 火箭一个菱形） |
| 双联分别开火 | 每次射击按站点交替（左/右各 5 发/秒，合计 10 发/秒，单测锁定枪管序列 0,1,0,1）；枪口位置、收敛方向、曳光粒子、**枪声位置与音高**均按当前开火的站点计算，两个准星也各自收敛 |
| 机枪更真实 | 机匣 + 供弹槽 + **6 管旋转枪管簇**（枪管按 60° 环绕布置、含枪口箍），枪管簇在预热/射击时旋转（`clusterSpinDegreesPerTick`：待机 0.6、预热 14、射击 26 度/帧），左右簇相位略有差异 |
| 火箭巢更真实 | 9×9 巢体 + 挂耳 + 尾部喷口板；**7 管布局**（1 中心 + 6 环绕，`LittleBirdPodTubes`），每管独立火箭弹头，**按管消耗**：发射后对应弹头隐藏（弹药数由实体同步） |

验证：单测 **90 个全绿**（新增 AimProjection 6、LittleBirdPodTubes 2 等）；`gradlew.bat build` BUILD SUCCESSFUL（自动同步），
JAR 112,456 字节，源/目标 SHA-256 一致。

待游戏内确认：投影准星是否贴合实际弹着点（机枪两枚十字应在收敛距离 32 格外重合、近距离分开）；枪管簇旋转与弹头消耗是否可见；
如果“双联分别开火”其实是指**左右分别用不同按键开火**，请说明，我改成左键=左枪、右键=右枪。
## 13. 追加变更：机枪按参考工程无人机风格重做 / 火箭大幅提速

依据：用户指出当前机枪「太塑料」，要求参考 `final-paradox-forge` 的无人机（已只读对照 `DroneModel.addGun`）。

### 13.1 机枪（照参考工程的枪械结构）

参考工程的枪由 mount / receiver / barrel_cluster（6 管 + 中心杆）/ heat_tips（枪口热梢，独立发光 pass）/ magazine / grip 组成。小鸟的机枪按同一结构在自身尺度重做：

- `mount` 3x3x3、`receiver` 4.6x3.2x7、`magazine` 3x2.4x2.2、`grip` 1.8x1.5x1.8
- `cluster`：6 根 0.9 单位枪管按 60° 环绕（环半径 1.24）+ 中心杆 1.1 + 枪口箍 5.8x5.8x1.6，开火时整簇旋转
- `heat_tips`：6 个枪口热梢（1.05x1.05x1.6），默认隐藏，仅由 `renderGunHeat` 用 `entityTranslucentEmissive` + `LightTexture.FULL_BRIGHT` 渲染，颜色由 `heatGlowColor`（0.4+0.6f, 0.1+0.65f, 0, f）随热度淡入
- 枪口位置随之更新：`GUN_MUZZLE_Z_UNITS` -38 → -22（与新的枪管长度一致，弹道/准星自动跟随几何常量）

### 13.2 火箭（大幅提速）

| 项 | 之前 | 现在 |
| --- | --- | --- |
| 初速 | 0.75 格/t（15 m/s） | **1.60 格/t（32 m/s）** |
| 巡航 | 2.00 格/t（40 m/s） | **4.00 格/t（80 m/s）** |
| 加速时间 | 20 tick | **10 tick（0.5 秒）** |
| 重力 | 0.012 | 0.010 |
| 寿命 | 120 tick（6 秒） | 60 tick（3 秒，最大射程约 240 格） |

落点预测与服务器弹道共用同一纯规则类，所以提速后准星自动一致。

验证：单测 **92 个全绿**（新增枪口热梢发光颜色断言等）；`gradlew.bat build` BUILD SUCCESSFUL（自动同步），JAR 113,324 字节，源/目标 SHA-256 一致。
## 14. 追加变更：自定义音效 + 真正的子弹曳光

### 14.1 音效（ffmpeg 程序化合成，非原版事件）

本机存在 ffmpeg（Gyan.FFmpeg 9.0），因此用 `tools/generate_weapon_sounds.ps1` 从噪声/颤音合成 5 个 OGG，
不引入任何第三方音频，可随时重新生成：

| 文件 | 合成方式 | 用途 |
| --- | --- | --- |
| engine.ogg | 粉噪声 lowpass 900 + 18 Hz tremolo，2 秒可循环 | 旋翼叶拍低频，音量/音调随转速 |
| gun.ogg | 粉噪声 lowpass 4200 + 30 Hz tremolo，1 秒循环 | 机枪连发，音调随热度 |
| gun_shot.ogg | 白噪声短瞬态 0.12 秒 | 单发机械质感（叠在循环上） |
| rocket_launch.ogg | 粉噪声带通 0.8 秒 | 火箭发射 |
| explosion.ogg | 棕噪声 lowpass 700 + 长尾 1.5 秒 | 战斗部低频轰鸣（叠在原版爆炸声之下） |

播放：`LittleBirdSoundManager` 每 tick 为**每架在听觉范围内的直升机**维护两个循环音实例
（`LittleBirdEngineSoundInstance` 随 RPM 变音量/音调；`LittleBirdGunSoundInstance` 由同步的枪状态驱动），
火箭发射与爆炸为服务端位置音。原先的弩/烟花/拾取占位已删除。

注：脚本里踩到一个坑 —— PowerShell 的 `$input` 是保留自动变量，用作参数名会静默绑定为空，
导致 ffmpeg 把 `-af` 当成输入文件；参数已改名 `$source` 并在脚本中注明。

### 14.2 子弹（真曳光，替代 hitscan 粒子点）

- 服务器每发一条 `LittleBirdTracerPacket`（枪口坐标 + 命中坐标 + 命中类型 + 左右站点），
  通过 `PacketDistributor.TRACKING_ENTITY` 广播，射击者本人也在跟踪集内。
- 客户端 `LittleBirdTracerRenderer` 绘制世界空间曳光弹：弹头以 14 格/秒前进、拖尾 0.85 格、
  枪口闪光 0.16 秒、命中时按类型产生方块碎屑/电火花或实体命中火花；左右两挺颜色略有差异。
- 纯规则时间线 `LittleBirdTracerTimeline` + 5 个单测（飞行速度、拖尾、到达淡出、过期、枪口闪光）。
- 与「不要画线」不冲突：**瞄准辅助**依然是 HUD 投影准星（无世界线条），**子弹**是可见曳光。

### 14.3 验证

- 单测 **95 个全绿**；`gradlew.bat build` BUILD SUCCESSFUL（自动同步）。
- JAR **188,861 字节**（含 5 个 OGG 与 sounds.json），源/目标 SHA-256 一致（`2D463EB4…`）。
- 专用服务器冒烟：`sound_event` 注册正常、`Done (1.946s)!`、无异常、无残留进程。
## 15. 追加变更：机枪射速翻倍 + 伤害大幅提高

| 项 | 之前 | 现在 |
| --- | --- | --- |
| 射速 | `FIRE_INTERVAL_TICKS = 2` → 10 发/秒 | **`FIRE_INTERVAL_TICKS = 1` → 20 发/秒**（每 tick 一发，左右交替 = 每挺 10 发/秒） |
| 每发热量 | 2 | **1**（射速翻倍后同步减半，保持 6 秒过热） |
| 单发伤害 | 3.5 | **14.0**（无护甲玩家 20 血 → 2 发；约 280 DPS） |
| 伤害配置范围 | 0.5 ~ 20 | 0.5 ~ 60 |

- 过热时长不变：120 热 / (1 热/发 × 20 发/秒) = **6 秒**，过热锁定仍是 4 秒。
- 曳光包速率随之变为 20 条/秒（每条约 50 字节，约 1 KB/s），可接受。
- 实例里已存在的 `config/littlebird-common.toml` 中的 `gun_damage` **已从 3.5 直接改写为 14.0**（Forge 不会覆盖已存在的键）。

验证：单测 **95 个全绿**（机枪节奏测试更新为 20 发/秒、119 发过热）；`build` 成功并自动同步，
JAR 188,894 字节，源/目标 SHA-256 `F38D4357…` 一致。

调参入口：`config/littlebird-common.toml` 的 `balance.gun_damage`（0.5~60）。
若还想更快，只能每 tick 多发（当前实现是一 tick 一发，理论上限 20 发/秒）。
## 16. 追加变更：机枪落点准星默认隐藏

用户反馈：屏幕中央那个大十字（= 两挺机枪各自的**投影落点准星**叠加）碍眼，要求隐藏。

- 新增 `view.show_gun_reticle`（默认 **false**）：机枪落点十字默认不绘制。
- 新增 `view.show_rocket_reticle`（默认 true）：火箭落点菱形保留。
- 中央的小十字准星（机头轴线指示）保持绘制；两个落点准星的开关互不影响。

验证：`gradlew.bat build` BUILD SUCCESSFUL（自动同步），JAR 189,127 字节，源/目标 SHA-256 `1767090A…` 一致。
实例配置里若还没有这两个键，Forge 会在下次启动时按默认值写入。
## 17. 修正：真正要隐藏的是中央准星，不是投影落点（第 16 节结论作废）

取证方式：把用户截图的中央区域用 ffmpeg 裁切并 2 倍放大后目视，中央实际叠了三个元素：

1. 红橙色大十字 = 两挺机枪的投影落点准星（用户要求保留）
2. 下方黄色菱形 = 火箭投影落点准星（保留）
3. 正中央白色小十字 = 原版准星 + 本模组准星叠加（这才是用户要隐藏的中央准心）

修正：
- view.show_gun_reticle 默认恢复 true（投影落点保留）。
- 新增 view.show_center_crosshair（默认 false）：本模组自绘的中央准星默认不画。
- 驾驶时通过 RenderGuiOverlayEvent.Pre + VanillaGuiOverlay.CROSSHAIR 取消原版准星的绘制；
  两者叠加正是那个又粗又大的十字的来源。

验证：gradlew.bat build BUILD SUCCESSFUL（自动同步），源/目标哈希一致。
## 18. 追加变更：红色准星恢复 + 火箭持续补给（每 10 秒 7 发）

### 18.1 红色准星为什么消失，以及怎么恢复

上一轮我先把 show_gun_reticle 的默认值改成 false，用户启动过游戏后 Forge 就把
show_gun_reticle = false 写进了实例配置；随后我把代码默认值改回 true，但 **Forge 不会覆盖已存在的键**，
所以红色投影准星仍然是关的。

处理：直接把实例 config/littlebird-client.toml 里的该项改写为 true。
教训：凡是改默认值的配置项，都要同时检查并修正用户实例里已存在的键，否则改动不会生效。

### 18.2 火箭补给：整包重装 -> 持续滴灌

| 项 | 之前 | 现在 |
| --- | --- | --- |
| 触发条件 | 落地且 20 秒未发射 | **每 10 秒一次，空中也补** |
| 补给量 | 一次补满 14 发 | **每次 +7 发**（左右吊舱交替，先补空的那个） |
| 从空到满 | 20 秒 | 20 秒（两次补给） |
| 配置键 | auto_rearm_seconds = 20 | **rocket_rearm_seconds = 10 / rocket_rearm_batch = 7** |

- 新增纯规则 `RocketPodState.rearm(snapshot, rounds)`：交替补给、每个吊舱不超过 7 发、负数与满弹都安全；
  新增 3 个单测（7 发 -> 4/3 分配、空吊舱优先、永不超装）。
- 实例 common 配置里的旧键 auto_rearm_seconds 已替换为新键。

验证：单测全绿；gradlew.bat build BUILD SUCCESSFUL（自动同步），
JAR 189,888 字节，源/目标 SHA-256 F5F91C49... 一致。
## 19. 追加变更：战地风格动态准星（随武器切换 + 散布扩散）

| 项 | 实现 |
| --- | --- |
| 随武器切换 | 选中机枪 -> 只画机枪十字；选中火箭 -> 只画火箭菱形（新增 view.reticle_follows_weapon，默认 true；关掉则所有已启用准星同时画） |
| 机枪十字 | 以**两挺机枪弹着点的中点**为圆心；四臂按散布向外扩散 + 中心小圆点（BF4 风格） |
| 扩散量怎么算 | 取当前热度对应的散布角（0.35 度 ~ 1.10 度），算出散布锥在该弹着距离上的半径 = tan(角度) x 距离（格），
  再把「弹着点」与「弹着点 + 侧向偏移该半径」两个世界点都投影到屏幕，两点像素距离就是臂的间隙 —— 所以距离越远、枪越热，十字张得越开，且始终对应真实散布范围 |
| 细节 | 最小间隙 3 px（否则退化成一点）、臂长 7 px、颜色沿用红色系 |

纯规则类 `LittleBirdCrosshairMath` + 4 个单测（冷热散布半径、半径随距离线性、间隙下限、武器切换判定）。
旧的两枚独立十字（每挺一个）已替换为单个动态十字。

验证：单测全绿；build 成功并自动同步，JAR 193,179 字节，源/目标 SHA-256 664CA542... 一致。
## 20. 追加变更：准星去圆点 / 火箭准星改战地 2042 点阵包线 / 肩射视角实施

### 20.1 准星

- 机枪动态十字**去掉中心小圆点**（只留四臂）。
- 火箭准星从菱形改为**战地 2042 风格点阵包线**：中央一条虚线（3 亮 3 灭、跨度 +-26 px）+ 左右各一簇
  3x3 **空心**点阵（点 2 px、间距 4 px、簇心偏移 +-15 px）。

### 20.2 肩射视角（按计划实施，采用默认值 + 可配置）

| 项 | 实现 |
| --- | --- |
| 触发 | 第三人称下**按住右键**（瞄准键）进入，松开退出；与舱内视角（C）互斥；第一人称不生效 |
| 相机偏移 | 向机体**左肩** 1.6 格 + 抬高 0.6 格（默认值），使机身滑出画面中心、落到右下方 |
| 实现方式 | 复用**客户端专用 `positionRider` 偏移**（第三人称相机绕隐身的乘员，偏移乘员即偏移相机），**无需 Mixin** |
| 配置 | view.shoulder_view（true）、view.shoulder_offset_blocks（1.6）、view.shoulder_height_blocks（0.6） |
| 方向约定 | 纯规则 `LittleBirdCameraMath.shoulderOffset`：MC 中 yaw 0 朝 +Z 时机体左侧为 +X；4 个单测锁定 |

深度/距离沿用原版第三人称（仍可滚轮缩放）；如需固定距离需另找 `Camera` 接口，暂不做。

### 20.3 本轮自审发现并修正

实体（common 侧）引用了 `client` 包的 `LittleBirdCameraMath` -> 已把该类（与其测试）移入 `entity` 包，
与之前 LittleBirdGeometry 的处理保持一致：**common 代码不得引用客户端包**。

验证：单测全绿；build 成功并自动同步，JAR 196,106 字节，源/目标 SHA-256 D7EE78F9... 一致。
## 21. 追加变更：精确复刻参考准星（点阵 = 火箭巢装填指示）

取证：把用户提供的 154x74 参考裁剪放大 8 倍（.codex-tmp/user_reticle_8x.png）后逐格读结构：

- **每侧恰好 7 个点**：外列 4 个（第 0~3 行）+ 内列 3 个（第 1~3 行，**内列顶格空缺**）
  -> 与 70mm 吊舱的 7 根管一一对应，正是用户所说「两边白点显示是否装填」。
- 每侧点阵靠中心一侧有一根**中间断开的竖条**（固定括号，不随弹药变化）。
- 正中一个**单点**（略高于水平中线）作为瞄准参照。
- 参考图中心右侧另有一小块模糊痕迹，判断为视频压缩噪声，未复刻。

实现：

- 火箭准星仍画在**预测落点**的投影位置；左右各一组点阵分别对应左右吊舱。
- 点 = 装填指示：**某根管里还有火箭就画点**，打掉就消失，因此开火时从外列顶端向内递减。
- 管序->点位映射是纯规则 `LittleBirdCrosshairMath.tubeDot(tube)`（管 0~3 走外列，管 4~6 填内列第 1~3 格），
  新增 3 个单测（7 管落在 7 个互不重复格、内列顶格永不用、越界抛错）。
- 尺寸常量（HUD 像素）：点 2、行距 5、列距 5、竖条偏移 10、竖条宽 2、半高 6、中心缺口 4。
- 机枪动态十字保持上一轮的样子（四臂、无中心点）。

验证：单测 **109 个全绿**；build 成功并自动同步，JAR 197,783 字节，源/目标 SHA-256 8C49BACF... 一致。
## 22. 追加变更：弹药点消失顺序 / 落点白圈 / 中央地平仪簇 / 右下武器面板（去重）

| 需求 | 实现 |
| --- | --- |
| 点从左到右、从上到下消失，左右交替 | 吊舱本就左右交替开火；点位映射反转：`tubeDot(tube) = sequenceDot(6 - tube)`，
  使**最先消失的是外列顶端**，随后外列自上而下、再内列自上而下；补给时反向长回。纯规则 + 2 个新单测 |
| 火箭落点白色小圆圈 | 在火箭预测落点画**白色空心圆**（半径 4 px，24 段点画），对应参考图中央那个白圈 |
| 中央 UI 参考图1 | 青色飞行簇：俯仰梯（±30 度、每 5 度一条、带数字、正角亮/负角暗、地平线中间留缺口，随坡度旋转）
  + 坡度刻度弧（-60~+60 每 15 度一点 + 随坡度移动的指针）+ 航向框（顶部，青色）+ 速度框（左，黄色，km/h）+ 高度框（右，黄色） |
| 右下 UI 参考图2 | 武器面板：武器名（7.62 毫米双联装速射机枪 / 70 毫米火箭巢）、弹药（机枪 ∞、火箭数量）、机枪热度条、
  三个槽位（1 机枪 / 2 火箭 / Z 齐射，选中槽填充青色）、底部 RPM 与机体耐久 |
| 去掉重复 UI | 删除旧的顶部航向带、左下速度/升降/Y 读数、底部武器条（热度+14 格弹药+模式文字）、旧坡度横线、旧右下 RPM/耐久 |

自审修正：合并读数时**一度漏掉机体耐久**，已补回武器面板底部（低于 30% 仍变红并触发 LOW HEALTH 告警）。
俯仰梯与坡度刻度的旋转/指针方向已用纯规则单测锁定数值，但左右方向是否与预期一致仍需游戏内目视确认（可一行取反）。

验证：单测 **115 个全绿**；build 成功并自动同步，JAR 199,388 字节，源/目标 SHA-256 2BD8B652... 一致。
## 23. 追加变更：俯仰梯大幅缩小 + 弹药点改为行优先消失

### 23.1 俯仰梯缩小

| 项 | 之前 | 现在 |
| --- | --- | --- |
| 刻度范围 | ±30 度（13 条） | **±15 度**（7 条） |
| 每度像素 | clamp(2, 8, 高/90) | **clamp(1.5, 4, 高/160)** |
| 线半宽 | 最大 34 px | **最大 15 px** |
| 数字偏移 | +-40/46 px | **+-18/22 px** |

以 540 GUI 像素高为例：垂直跨度 360 px -> **101 px**，宽度 68 px -> 30 px，占用面积约缩小 **8 倍**。

### 23.2 弹药点消失顺序（上一轮理解有误，已按用户澄清修正）

用户澄清为「先左右、后上下」= **行优先**：同一行先消耗左右两列，再换到下一行。
由于内列顶格为空，顺序为：(外0) -> (外1)(内1) -> (外2)(内2) -> (外3)(内3)。
`sequenceDot` 已改为行优先实现，`tubeDot(tube) = sequenceDot(6 - tube)` 保持不变（首发射出的管对应最先消失的点），
单测同步改为逐点断言这条顺序。视觉上因为左右吊舱交替开火，呈现「左一颗、右一颗」逐行消耗。

验证：单测 115 个全绿；build 成功并自动同步，JAR 199,488 字节量级，源/目标 SHA-256 一致。
## 24. 追加变更：中央 UI 左右分开 / 俯仰梯扩到 ±90 度 / 火箭射速大幅加快

| 需求 | 实现 |
| --- | --- |
| 中央 UI 左右分开 | 速度框与高度框不再贴着俯仰梯：改为距中心 `max(120, min(宽/4, 320))` 像素，速度框右对齐到左侧该位置、高度框左对齐到右侧该位置 |
| 俯仰梯扩到 ±90 度 | `LADDER_LIMIT_DEGREES = 90`（刻度范围），但只绘制当前姿态 +-20 度（`LADDER_WINDOW_DEGREES`）之内的线，
  且每度像素收紧到 `clamp(1.5, 3.0, 高/200)` -> 可视高度仍约 108 px，画面占用不变但姿态范围覆盖全量并可滚动 |
| 火箭射速大幅加快 | 单发间隔 5 -> **2 tick（4 -> 10 发/秒）**；齐射间隔 7 -> **3 tick（2.9 -> 6.7 发/秒）**；
  14 发打空从 3.5 秒缩到 **1.4 秒（单发）/ 2.1 秒（齐射）** |

单发间隔测试改写为「按固定节奏连点，测量两次发射的最小间隔 >= 间隔常量 - 1」，不再依赖具体 tick 数，
以后调整射速不会再打碎测试。

验证：单测 115 个全绿；build 成功并自动同步，源/目标 SHA-256 一致。
## 25. 追加变更：俯仰梯按参考图重排（去掉逼仄感）

依据：用户反馈中间 UI 逼仄，并要求「再看看参考图」。把参考图的俯仰梯区域裁切放大 1.5 倍后测得：
参考图是**线很长、间距很大、一次只显示三四条线、数字在两端且很大**。

| 项 | 之前（逼仄） | 现在（参考图节奏） |
| --- | --- | --- |
| 行距 | 每 5 度一条，2.70 px/度 -> 13 px | **每 10 度一条，7.71 px/度 -> 77 px** |
| 线长 | 半宽最大 15 px（整条 30 px） | **半宽最大 120 px（整条 240 px，中央缺口 32 px）** |
| 数字位置 | 固定 +-18/22 px（与下一行数字打架） | **紧贴线外端**（按线长动态，左端右对齐、右端左对齐，不再重叠） |
| 可视范围 | 当前姿态 +-20 度 | 当前姿态 +-15 度（约三四条线） |
| 刻度范围 | +-90 度（滚动） | 不变，仍是 +-90 度滚动 |

验证：单测 115 个全绿；build 成功并自动同步，源/目标 SHA-256 一致。
若仍偏密/偏疏，两个旋钮：`pixelsPerDegree`（现 clamp(4, 9, 高/70)）与线长 `reach`（现 max(36, 宽/8)）。
## 26. 修复：机枪准星在瞄天空/未命中方块时消失

用户报告：机枪准星在瞄准天地线以上、以及没有瞄到方块时会消失。

根因（本轮自身引入的回归）：`LittleBirdAimRenderer.gunImpact` 在 `level.clip` 返回 MISS 时**直接返回 null**，
于是 `midpoint(null, null)` 为 null，`GunReticle` 被判为 HIDDEN，准星整体不画。
另外当时只做了方块射线，**完全没有实体判定**，所以瞄准空中的生物也会落到天空（进而为 null）。

修复：

1. 射线**永远返回一个点**：命中方块 -> 方块命中点；否则 -> 射线最大射程点（`RANGE`）。
   保证准星在任何朝向下都有绘制位置。
2. 新增**实体射线**（`ProjectileUtil.getEntityHitResult`，排除自身、其它小鸟、旁观者、死亡实体）：
   瞄准生物时准星落在生物身上，而不是穿过它。
3. 散布间隙不受影响：间隙由**角度**决定（tan(散布角) x 距离再投影），128 格处角度仍是 0.35 度，
   因此远处天空下准星依旧收得很紧（受 3 px 最小间隙保护）。

对照：火箭准星本来就不会消失（轨迹循环结束后回退到末点位置）。

验证：build 成功并自动同步，单测 115 个全绿，源/目标 SHA-256 C51DC0BD... 一致。
## 27. 追加变更：火箭飞行速度再次大幅提高

| 项 | 之前 | 现在 |
| --- | --- | --- |
| 初速 | 1.60 格/t（32 m/s） | **2.50 格/t（50 m/s）** |
| 巡航 | 4.00 格/t（80 m/s） | **6.00 格/t（120 m/s）** |
| 加速时间 | 10 tick（0.5 秒） | **6 tick（0.3 秒）** |
| 寿命 | 60 tick（3 秒） | 40 tick（2 秒） |
| 最大射程 | 约 240 格 | 约 240 格（保持不变） |
| 重力 | 0.010 | 不变（相对速度更小，弹道更平） |

50 格处命中时间从约 0.63 秒缩短到 **0.42 秒**；14 发齐射在 2.1 秒内打完（射速见第 24 节）。

已知取舍：6 格/tick 意味着实体每个游戏刻位移 6 格（网络同步也是每刻一次），极近距离掠过时观感上是「一步一段」；
方块碰撞仍是扫掠式（`move` 不会穿墙），但**火箭不做实体扫掠判定**，直接掠过一个生物时靠半径 3 的爆炸兜住。
若嫌跳变明显，可把巡航速度退到 4.5~5.0 格/t 折中。

验证：单测 115 个全绿（弹道测试全部由常量驱动，无需改写）；build 成功并自动同步，源/目标 SHA-256 D8201A80... 一致。
## 28. 修复：俯仰超出射界时准星卡边抖动 / 仰头消失后延迟恢复

用户报告：下俯超过机枪射界时准星停在屏幕最下方抖动；仰头时消失，过一会才恢复。

根因（第 26 节修复的副作用）：准星画在**弹着点的屏幕投影**上，而
机体射界受姿态限制（俯仰被夹在 +-32 度且每 tick 速率受限），相机却能看到 +-90 度。
因此当玩家把视线压过/抬过射界时：

1. 弹着点投影跑到屏幕外 -> 旧逻辑判 HIDDEN（消失）；
2. 若投影刚好压在底边 -> 就贴在底边，而弹着点每 tick 在相邻方块间跳变 -> 抖动；
3. 机体姿态以 6 度/tick 追上来之后 -> 投影回到屏幕内 -> 看起来「过一会恢复」。

修复：

- 新增纯规则 `LittleBirdCrosshairMath.usesScreenCentre(visible, x, y, w, h)`：
  投影点不可见、出屏、在相机后方时，准星改为画在**屏幕中心**（永远可见、不卡边）。
- 新增纯规则 `smooth(current, target, factor)`（系数 0.5）做**帧间平滑**，
  消除弹着点在方块间跳变导致的逐 tick 抖动；离开载具时重置平滑状态。
- 机枪十字与火箭点阵**都**套用同一规则（火箭弹道末点也会出屏）。

新增 2 个单测（出屏回退判定、平滑收敛且不过冲）。

验证：单测 1xx 全绿；build 成功并自动同步，源/目标 SHA-256 一致。

## 29. 飞控优化：滚转从「角度伺服」改成速率指令 + 真正生效的协调转弯

用户报告：有滚转角，但滚转时只有一个固定角度，并且松开滚转键时会自动回正。

### 29.1 只读诊断（证据）

| 现象 | 证据 | 结论 |
| --- | --- | --- |
| 只有一个固定角度 | `HelicopterFlightModel.step` 旧第 28-29 行 `rollTarget = input.rollInput() * config.rollMaxDeg`、`roll = approach(roll, rollTarget, rollRate)` | 按住 A/D 只会把坡度伺服到恒定 ±38 度，没有「按住越久越斜」 |
| 松开自动回正 | 实例 `littlebird-client.toml` 里 `auto_level = false` → `rollLevelRateDegPerTick = 0`，看似应当保持 | 真正的元凶是滚转输入先过 `HelicopterInputDamping`（0.25/tick）：抬键后阻尼值指数衰减约 20 tick 才到 0，而 `rollTarget` 永远等于「阻尼值 × 38 度」，于是目标把坡度一起拖回 0；期间 `rollRate` 因为输入还没「恰好为 0」一直是满速率 7 度/t |
| 协调转弯几乎没效果 | 旧 `yaw += (roll/rollMax) * bankedTurn` 只有 3 度/t，而偏航是「鼠标绝对朝向 + 9 度/t 跟随」 | 这个常数项会被下一 tick 的 `approachAngle(yaw, 鼠标朝向, 9)` 直接吃掉，只留下 3 度的固定偏置，不会累积成转弯 |

### 29.2 修复

1. **滚转 = 速率指令**：`rollDeg = wrap(rollDeg + rollIn * rollRateDegPerTick)`，没有坡度上限，
   可以滚过 90 度甚至倒飞（滚过 180 度后回绕到 -180..180）。输入为 0 时才走
   `approachAngle(rollDeg, 0, rollLevelRateDegPerTick)`，默认 0 = **精确保留**当前坡度。
2. **阻尼对滚转轴「抬键立即归零」**（`HelicopterInputDamping`）：按下仍按 `roll_damping` 缓入
   （保留旋翼惯性手感），抬键当 tick 输入直接变 0，坡度能精确停住，不会被阻尼余量多滚 0.2 秒。
3. **协调转弯真正生效**：新增纯函数 `bankedTurnDegPerTick(speed, rollDeg, lift, cfg)` =
   `clamp(g * tan(坡度) / max(speed, 0.05 * maxForwardSpeed), ±bankedTurnDegPerTick)`
   `* clamp01(speed / (0.35 * maxForwardSpeed)) * clamp01(lift)`（转弯半径 v²/(g tan 坡度) 的等价式）。
   悬停时淡入系数为 0（只侧移不转弯），熄火无升力不转弯，配置设 0 可整体关闭。
   该角速度由 `LittleBirdEntity.applyLocalFlight` 同时施加到**骑乘玩家 yaw** 与**阻尼滤波器 yaw**
   （新增 `HelicopterInputDamping.rotateYaw`），机头、相机、准星一起转；若只加工实体不加工滤波器，
   滤波器会把这次转弯读成偏航误差并把机头拽回鼠标方向（蟹行）。
4. **侧滑改写**：`sin(坡度) * sideSlipAccel * lift * (1 - 速度/极速)`，悬停满侧移、巡航淡出。
5. **同步与显示**：`MAX_SYNCED_ROLL_DEG` 90 → **180**（倒飞坡度要能同步）；
   HUD 坡度指针新增 `LittleBirdHudMath.clampBankDegrees`，按 ±60 度钳位，满滚转时指针压边而不是绕到另一侧。
6. **顺手修回归**：旧代码靠协调转弯那一行里的 `wrapDegrees` 顺带回绕 yaw；删掉该行后
   `yawTakesTheShortestPathAcrossTheWrap` 立刻失败（解算结果变成 181 而不是 -179）。
   现在显式 `yaw = wrapDegrees(approachAngle(...))`，保住 -180..180 的状态契约。

### 29.3 配置

| 键 | 之前 | 现在 |
| --- | --- | --- |
| `roll_rate_degrees_per_tick` | 不存在（内部 7.0） | **新增，默认 6.0**（1..30，120 度/秒，整滚约 3 秒） |
| `banked_turn_degrees_per_tick` | 3.0（0..10，固定偏置） | **6.0**（0..15，协调转弯上限） |

实例 `versions\1.20.1-Forge_47.4.20\config\littlebird-client.toml` 已手动同步这两项：
Forge **不会覆盖已存在的键**，改默认值必须手改实例文件（沿用第 26 节教训）。

### 29.4 测试

新增/改写 **9** 个单测：滚转速率积分、按住越久越斜（>90 度）、越过 180 度回绕、
松开精确保留、`auto_level` 打开后按最短路径回平、悬停侧滑、协调转弯方向与上限、
坡度/速度单调性、悬停与熄火不转弯、阻尼抬键归零、`rotateYaw` 回绕、HUD 坡度钳位。

### 29.5 已知取舍（必须在游戏内确认）

- 坡度不再有上限：可以滚到倒飞。倒飞时 `tan(坡度)` 自然反向，转弯方向也跟着反（与真实一致）；
  HUD 俯仰梯与镜头会整体倒转（真实 HUD 也是这样）。
- 松开滚转键后**完全**没有回正力，需要手动回平；把 `auto_level` 打开可恢复自动回平（4.5 度/t）。
- 协调转弯会转动玩家视角（因为机头与相机耦合），第三人称盘旋时视野会跟着扫过去；这是设计选择。

验证：**126 个单测全绿**（+9）；`gradlew.bat build` 成功并自动同步，
实例 mods 中只有 1 份 JAR，源/目标 SHA-256 `747E2E63...` 一致。
构建成功不等于手感验证通过：滚转速率 6 度/t、协调转弯上限 6 度/t、侧滑淡出速度都需要进游戏实测。

## 30. 飞控优化（续）：俯仰放开到 ±90 度，推力与视线解耦

用户要求：「增加俯仰，取掉限制改为 ±90」。

### 30.1 旧行为（问题）

- `FlightConfig.pitchMaxDeg = 32`，实体里 `PILOT_PITCH_REFERENCE_DEG = 32` 把视线
  `xRot / 32` 变成输入轴，视线超过 32 度后输入饱和 → 机头最多只能到 32 度。
- 后果一：相机能看 ±90，机头只有 ±32 —— 这正是第 26/28 节「准星卡边 / 仰头消失」的射界根源。
- 后果二：机枪与火箭的炮口方向按机体姿态算（`Vec3.directionFromRotation(getXRot(), getYRot())`），
  视线超出射界时弹道与准星投影不同源。
- 后果三：推力是 `(pitchDeg / pitchMaxDeg) * forwardAccel * command`。只把 pitchMaxDeg 改成 90 的话，
  正常压 32 度只能拿到 32/90 ≈ 36% 推力，巡航会明显变肉。

### 30.2 改动

1. `pitchMaxDeg` 32 → **90**，并在解算里显式硬限幅
   `clamp(approach(...), ±pitchMaxDeg)`：配置缩小、旧存档状态、NaN 恢复都会被拉回包线内。
2. 新增 `pitchThrustReferenceDeg`（默认 32，不进 Forge 配置）：推力命令与姿态因子都在这个角度饱和，
   把「能俯冲到 90 度」和「满推力需要多大俯角」解耦 —— 巡航手感与改动前完全一致。
3. 实体侧删除常量 `PILOT_PITCH_REFERENCE_DEG`，改用 `flight.pitchMaxDeg` 作为视线 → 输入轴的参考，
   机头与视线 **1:1**（半舵视线 = 半舵姿态），32 度以上的射界错位消失。
4. 新增配置键 `pitch_max_degrees`（10~90，默认 90）；想回到老的紧凑包线填 32 即可。
5. 座舱视角：`positionRider` 原来只按偏航前移，俯冲到 90 度时相机会被留在机体外面。
   新增 `LittleBirdCameraMath.cockpitSeatOffset(yaw, pitch, forward)`，沿机体机头轴
   `(-sin yaw·cos pitch, -sin pitch, cos yaw·cos pitch)` 前移，相机跟着座椅一起俯仰。

### 30.3 已知取舍（需游戏内确认）

- 这是街机模型：俯仰只决定水平推力，没有「大俯角换俯冲速度」的耦合。
  机头 90 度朝下时水平速度仍受 maxForwardSpeed 限制、垂直速度仍由总距控制。
  需要的话可以再加「俯角超过 45 度时把升力转成俯冲加速」的耦合。
- 俯仰速率仍是 6 度/t（配置 `pitch_rate_degrees_per_tick`，1~30）：平飞→竖直需要 0.75 秒，
  快速甩鼠标时机头会明显滞后；嫌慢直接调大。
- 倒飞（滚转超过 90 度）时俯仰是机体系，视线与机头仍然一致，但 HUD 俯仰梯整体倒转。

### 30.4 验证

新增 5 个单测（俯仰到 ±90、半舵视线 1:1、推力在参考角饱和、超范围姿态被拉回、座舱座椅随俯仰）；
**131 个单测全绿**（+5）。`gradlew.bat build` 成功并自动同步，
实例 mods 只有 1 份 JAR，源/目标 SHA-256 `86E8A845...` 一致。
实例 `littlebird-client.toml` 已加入 `pitch_max_degrees = 90.0`。

## 31. 飞控优化（三）：推力沿机头轴输出 —— 机头指哪就飞哪

用户要求：「这个需要改，让飞机前进方向受到螺旋桨所指方向操控」。

### 31.1 旧行为（三处都断了）

| 位置 | 旧公式 | 后果 |
| --- | --- | --- |
| 推力方向 | `vx += forwardAccel * dirFwd.x`，`vz += ...`（只有水平分量） | 推力永远水平，机头压到 90 度也只是「水平推力打满」，**轨迹完全不受机头指向影响** |
| 垂直方向 | 只由总距的垂直速度伺服 `approach(vy, vyTarget*lift, accel)` 决定 | 永远不可能出现真正的俯冲；按住 W 就能一直托住 |
| 阻力 | `drag = 1 - horizontalDrag*(1-command)`，「命令推力取消阻力」 | 机头竖直向下、油门到底时水平速度还被命令托着，不衰减 |

### 31.2 新模型

    steer  = clamp01(cfg.pitchDiveCoupling)                       // 单一旋钮，默认 1.0
    thrust = min(1, |pitch|/pitchThrustReferenceDeg) * accel * liftScale * cmd   // 恒 >= 0
    fwd    = sign(pitch) * thrust * cos(pitch)                    // 机头轴的水平分量
    dive   = thrust * sin(pitch) * steer                          // 机头轴的垂直分量
    vy    -= dive
    verticalAuthority = liftScale * (1 - steer * (1 - cos(pitch)))
    vy     = approach(vy, vyTarget * verticalAuthority, verticalAccel * verticalAuthority)
    vy    -= gravity * (1 - verticalAuthority)^2
    drive  = clamp01(|fwd| / cfg.forwardAccel)                    // 阻力只被「实际前向推力」取消
    drag   = 1 - horizontalDrag * (1 - drive)

- **压 45 度**：前推力 0.707 倍，同时每秒往地面方向攒约 0.025 格/t → 前飞 + 下沉，按 W 才能保持高度。
- **拉 45 度**：后退 0.707 倍（刹车/倒飞）+ 垂直 +0.021 格/t（flare 爬升）。
- **压 90 度**：前推力约 0（cos90 = 0）、垂直权限 = 0（1 - steer），重力全量 + 俯冲分量约 0.07 格/t²
  → 约 0.85 秒冲到 `maxFallSpeed` 1.2 格/t（24 格/秒），水平速度因 `drive = 0` 而衰减。
- `steer = 0`：`fwd` 仍是 `thrust*cos(pitch)`（前进方向依旧由机头决定），但 `dive = 0`、
  `verticalAuthority = liftScale` → 垂直方向完全回到旧模型，只归总距管。

### 31.3 为什么没有用「俯角 > 45 度」硬阈值

用连续的 sin/cos 投影代替阈值：俯角越大，投影到垂直方向的分量越多、水平越少，跨过 45 度后
`(1-cos)^2` 的重力残留迅速超过总距能顶住的量（cos60 = 0.5 → 重力残留 0.010 格/t²，
cos90 = 0 → 0.040 格/t² 全量），所以「45 度以上明显变俯冲」是自然出现的，没有跳变。

### 31.4 配置

| 键 | 默认 | 范围 | 说明 |
| --- | --- | --- | --- |
| `pitch_dive_coupling` | 1.0 | 0.0 ~ 1.0 | 机头对垂直行程的操控强度；1 = 完全物理，0 = 垂直只由总距决定 |

实例 `littlebird-client.toml` 已写入。连带修正：**巡航不再需要压到 90 度**——
0.38 舵（约 35 度）就已是满推力（`pitchThrustReferenceDeg = 32`）。

### 31.5 验证

133 个单测全绿（+2：`theNoseSteersTheTravel` 压/拉两个方向、`aNinetyDegreeDiveTradesForwardSpeedForDescent`；
另有 4 个依赖「满舵 = 极速」的旧测试改成 0.5 舵即 45 度巡航角）。
`gradlew.bat build` 成功并自动同步，实例 mods 只有 1 份 JAR，源/目标 SHA-256 `10855112...` 一致。

### 31.6 还没做的（等用户决定）

- **俯冲撞地不扣血**：`LittleBirdEntity` 的撞机判定只看 `horizontalCollision` 与水平速度
  （`impactSpeed = sqrt(vx² + vz²)`），垂直方向 `verticalCollision` 只是把 vy 归零。
  旧的平飞模型里几乎攒不出垂直速度，所以看不出来；现在可以 24 格/秒垂直砸地而毫发无伤。
  要修就得给垂直方向单独一个更高的阈值（正常总距下降 0.5 格/t 不能算撞机），属于新的平衡决策。

## 32. 最大速度 100 km/h + 滚转启动阻尼

用户要求：「把最大速度调到 100，滚转做的更加平滑，带有启动阻尼」。

### 32.1 速度：100 km/h

HUD 的速度读数是 `toKilometresPerHour(水平格/t) = round(v * 20 * 3.6)`，所以 100 km/h =
**1.3889 格/t = 27.8 格/秒**（旧值 0.9 格/t = 18 格/秒 = 65 km/h）。

- `max_forward_speed` 默认 0.9 → 1.3889，配置上限 1.5 → 2.0（留余量），实例 toml 已同步。
- 连带影响（按比例缩放是正确的）：协调转弯的速度淡入基准、侧滑淡出基准都跟着 maxForwardSpeed 放大；
  满速正面撞墙的伤害由 (0.9-0.45)*90 ≈ 40 HP 变成 (1.3889-0.45)*90 ≈ 84 HP，
  也就是**满速撞墙直接报废**（机体 80 HP）。
- 加速度没动（forwardAccel 0.030），所以到极速的时间从约 2.2 秒变成约 4.5 秒。

### 32.2 滚转启动阻尼

旧行为：滚转输入只过 `HelicopterInputDamping`（0.25/tick 一阶滤波），第一 tick 就拿到 25% 舵量，
满速率 6 度/t 里立刻出现 1.5 度/t——手感上就是「一按就转」。

修复：滚转轴再串一级速率斜坡，新增纯规则类 `HelicopterRollDamper`：

    axis = approach(axis, dampedAxis, 1 / cfg.rollSpinUpTicks)   // 默认 10 tick 爬到满速率
    if (abs(command) < 1e-3) axis = 0                            // 抬键不阻尼：坡度精确保留

默认 `roll_spin_up_ticks = 10`：按下 A/D 后滚转速率在 0.5 秒内平滑爬到满舵，
第一 tick 只有 10% 舵量（0.6 度/t），这 0.5 秒累计约 33 度坡度。
两级级联（一阶阻尼 + 速率斜坡）让起步既没有速度跳变、也没有加速度跳变；
松开键仍然当 tick 归零，所以「松开保持坡度」这条没有被破坏。

### 32.3 配置

| 键 | 默认 | 范围 | 说明 |
| --- | --- | --- | --- |
| `max_forward_speed` | 1.3889 | 0.2 ~ 2.0 | 1.3889 = 100 km/h（HUD 显示值） |
| `roll_spin_up_ticks` | 10 | 2 ~ 40 | 滚转速率爬升时间；2 ≈ 几乎瞬时，40 ≈ 很沉 |

实例 `littlebird-client.toml` 两项都已写入。

### 32.4 验证

**139 个单测全绿**（+6：`HelicopterRollDamperTest` 起步不满舵、10 tick 到满速率、单调不过冲、
抬键立即归零、可配置、NaN 安全）。`gradlew.bat build` 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 `A813B1A7...` 一致。

### 32.5 待用户确认

- 「100」按 HUD 的 km/h 理解。如果指的是 100 格/秒（= 360 km/h），`max_forward_speed` 要填 5.0，
  那是单 tick 位移 5 格，碰撞扫掠、区块加载与观感都要另行处理，需要明确后再说。
- 俯冲撞地仍然不扣血（见 31.6 节），等确认。

## 33. 修复：机枪准星贴边抖动 + 座舱镜头抬高 0.40 格

用户报告（附截图）：「舱内视角还是有点矮了，并且机枪准心还是有 bug」。
追问确认：准星症状是**贴到屏幕边缘抖动、跳动**；镜头选择**抬高 0.40 格**。

### 33.1 准星抖动的根因（只读诊断，附证据）

- 截图里选中的是火箭巢，所以画面中只有火箭点阵准星（白圈 + 琥珀括号），看不到机枪十字；
  问题出在第 28 节引入的 `LittleBirdCrosshairMath.usesScreenCentre(visible, x, y, w, h)`：
  投影点一旦出屏就把绘制目标**整体切到屏幕中心**。
- 而弹着点是每 tick 重新做射线求交的，会随机体移动、方块边界与色散在「刚刚在屏内」与
  「刚出屏」之间反复跳变 → 绘制目标在**边缘附近**与**屏幕正中心**之间来回切换，
  再叠加 0.5 的帧间平滑，表现就是贴着边缘抖/跳。
- 同一路径还有第二处硬伤：机枪十字的**开口大小**用未平滑、未钳位的 `screen.x()/screen.y()`
  与色散边缘点算像素距离，掠射投影时该距离会爆到数百像素，四根臂被甩出屏幕。

### 33.2 修复

1. 新增 `LittleBirdCrosshairMath.clampToFrame(x, y, width, height)`（保留 6% 边框）：
   出屏时**朝出屏方向压到边框**，不再跳到中心。于是「刚刚出屏」与「远远出屏」得到
   **完全相同**的目标点，抖动源被直接消除（新单测断言这两种情况结果相等）。
   只有投影点位于相机后方（`behindCamera(!visible)`）无法定位时才退回屏幕中心。
2. 机枪十字开口改为用**钳位后**的中心点与边缘点计算，并给 `armGapPixels` 加
   `MAX_ARM_GAP_PIXELS = 24` 上限（同时防 NaN），掠射投影不再把十字甩出屏幕。
3. 删除 `usesScreenCentre`（全库已无引用），语义拆成 `behindCamera(boolean)` + `clampToFrame(...)`。

### 33.3 座舱镜头抬高

- 现状：`COCKPIT_EYE_UNITS = 38` → 眼位 1.544 格；机顶 ROOF_TOP 1.625 格，机头甲板顶 1.056 格，
  甲板只在视线下方约 0.49 格，近距离时占据屏幕下半部。
- 新增配置 `cockpit_eye_lift_blocks`（默认 **0.40**，范围 0~1.5）；几何侧新增
  `LittleBirdGeometry.cockpitRiderOffsetBlocks(liftBlocks)`，座舱视角下实体用它作为骑乘者偏移
  → 眼位升到 **1.944 格**（仍在旋翼盘 2.275 格之下）。
- 座舱视角下 cabin/roof/belly 等本来就隐藏，抬到机顶以上不会看到舱壳或旋翼；
  代价是俯视时更容易「穿透」机体看到地面（隐藏舱壳方案的固有取舍）。
  想收回来把 `cockpit_eye_lift_blocks` 调小即可（0 = 旧行为）。

### 33.4 验证

**140 个单测全绿**（+1：`theCockpitEyeLiftIsAppliedOnTopOfTheSeat`；
`armGapStaysInItsBand` 改为断言 24 px 上限与 NaN；
`reticlesArePulledInsideTheFrameInsteadOfJumpingToTheCentre` 取代旧的「回退中心」测试）。
`gradlew.bat build` 成功并自动同步，实例只有 1 份 JAR，源/目标 SHA-256 `98AA3654...` 一致。

### 33.5 仍需游戏内确认

- 抬到 1.944 格后甲板是否还挡视野；
- 准星贴边时是否彻底不抖（若仍抖，则是弹着点本身在动，需要再加大帧间阻尼）；
- 俯视穿透机体的观感能否接受。

## 34. 机枪准星回归最初版算法 + 第一人称隐藏全部机体建模

用户要求：「机枪准心回归最初版算法吧，然后进入第一人称视角后隐藏所有机体建模」。

### 34.1 机枪准星：回到最初版（去掉第 33 节的平滑/钳位/回退）

第 33 节那套「出屏压边框 + 帧间平滑」是冲着「卡边抖动」去的，但对机枪准星来说是多余的中间层：

- 机体俯仰现在与相机 **1:1** 且覆盖 ±90 度，炮口轴线不可能再跑出视野，出屏回退与钳位都不再需要；
- 帧间平滑会带来一帧以上的延迟，准星看起来「跟不上弹着点」。

因此 LittleBirdAimRenderer.gunCrosshair 回到最初版（第 20/26 节的实现）：

    screen = project(弹着点)
    if (!screen.visible()) -> 不画（弹着点在相机后方）
    gap    = armGapPixels(screen.x, screen.y, 色散边缘点屏幕坐标)   // 只保留 3 px 最小间隙
    return GunReticle(screen.x, screen.y, gap, distance, spread)

- 删除 smoothedGunX / smoothedGunY，机枪路径不再调用 clampToFrame / behindCamera；
- armGapPixels 去掉第 33 节加的 24 px 上限，恢复「开口跟着色散锥走」；
- **保留第 26 节的「射线永远返回一个点」**（方块 → 实体 → 最大射程），
  所以「瞄天空/没瞄到方块就消失」那次的修复没有回退：只有弹着点跑到相机身后才不画；
- 火箭准星维持现状（它那套钳位+平滑没有报过问题），clampToFrame / behindCamera 仍在被它使用。

### 34.2 第一人称（舱内）隐藏全部机体建模

- LittleBirdModel.setCockpitView(true) 除了隐藏原有的 COCKPIT_HIDDEN_PARTS（舱壳、机顶、机腹、机翼、
  挂架、尾段、主/尾旋翼、起落架、排气、天线），现在直接把 **root 隐藏**：
  基础 pass、以及动画/状态里稍后重新点亮的部件（挂架弹尖、炮口热辉）全部不画。
- LittleBirdRenderer 的炮口热辉 pass 追加 !entity.isCockpitView() 判断
  （该 pass 是直接 part.render 绕过 root 的，只靠 root.visible 挡不住）。
- 安全性：cockpitView 是**客户端字段且从不同步**，只有正在驾驶该机的本地玩家会被置 true，
  因此别的玩家看你时看到的仍是完整机体，不会变成「隐形飞机」。
- 代价：舱内视角完全看不到自己的机头与挂架（纯机内视野）。想恢复「看得见机头」，
  把 setCockpitView 里的 root 隐藏去掉即可（也可以后续做成配置开关）。

### 34.3 验证

**140 个单测全绿**（armGapStaysInItsBand 恢复为不受上限约束、仍断言 NaN 与最小间隙；
出屏钳位相关测试继续覆盖火箭路径）。
gradlew.bat build 成功并自动同步，实例 mods 只有 1 份 JAR，
源/目标 SHA-256 B23EB5C0... 一致（208054 字节）。构建成功不等于游戏内验证通过。

## 35. 修复：第一人称机枪准星被挤出画面

用户报告：第三人称的机枪准心正常，第一人称还是有问题。
追问确认：症状是**准星跑到屏幕下方/边缘外、被挤出去**；期望行为是**看不见就不画**。

### 35.1 根因（几何，附数字）

两个视角唯一的结构差别是**相机与炮口轴线的相对位置**：

| 视角 | 相机位置（相对机体） | 与炮线的高度差 |
| --- | --- | --- |
| 座舱第一人称 | 枪口**前方 0.81 格**（相机前移 1.706 格，枪口前移 0.894 格） | 眼位 1.944 格 vs 枪口 0.609 格 = **高 1.34 格** |
| 第三人称 | 机体后方 4~5 格且更高 | 弹着点投影偏移很小，始终在画面内 |

- 无重力 hitscan 的弹着点永远在炮线上，所以远距离（128 格）时第一人称也只偏下约 0.6 度（约 5 px）；
- 但**近距离**弹着点在第一人称会以极大角度落到视线下方：距离 d 处偏下
  atan(1.34 / (d + 0.81))，d = 3 格时约 24 度、d = 2 格时约 32 度，已接近/超出垂直半视角
  （70 度 FOV 的一半 = 35 度），投影被推到屏幕下边缘甚至画面之外，看起来就是「被挤出去」；
- 更近（d < 0.81 格，弹着点落在枪口与相机之间）时 depth <= 0.05，投影直接判为不可见。

### 35.2 修复（按用户选择：看不见就不画）

- 新增纯规则 LittleBirdCrosshairMath.outsideFrame(x, y, width, height)：
  以与火箭准星相同的 6% 边距判定「出画」（含 NaN）。
- LittleBirdAimRenderer.gunCrosshair：投影点在相机后方**或出画**时直接返回 GunReticle.HIDDEN，
  不再把它画到边缘外，也就没有「十字滑出屏幕」那一段；
  在画面内时仍是**最初版算法**（原始投影位置，不平滑、不钳位、不回退中心）。
- 火箭准星不动：它那套 6% 钳位会把它压在边距上、始终可见，用户没有报过问题；
  若要统一成「出画就不画」，改一行即可。

### 35.3 验证

**141 个单测全绿**（+1：offFrameReticlesAreNotDrawn 覆盖边距内/外、负坐标与 NaN）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，源/目标 SHA-256
A3AA1C38... 一致（208223 字节）。构建成功不等于游戏内验证通过。

### 35.4 备注

座舱相机比枪口更靠前是历史原因（相机早先必须前移到风挡之前才能「透过玻璃」看出去）；
第 34 节起第一人称已隐藏全部机体，这个前移不再必要。若仍觉得近距离弹着点太容易被挤出去，
把 COCKPIT_SEAT_FORWARD_UNITS 从 42 降到 10~14（约 0.4~0.6 格，回到机舱内）即可显著减小视差。

## 36. 机枪准星改为屏幕中心固定十字

用户要求：「算了，你就在屏幕中心画一个机枪准心吧」。

### 36.1 决策

投影式弹着点准星在第一人称的结构性缺陷（第 35 节：座舱相机在枪口前方 0.81 格、高出炮线 1.34 格，
近距离弹着点被投影挤出画面）无法用「贴边 / 不画」这类补丁彻底消除，用户选择直接改成**屏幕中心固定十字**。
这也更接近真实 HUD：机枪准星本来就应该钉在炮口轴线上（本模组里机头与相机 1:1，所以中心就是炮口轴线）。

### 36.2 改动

- LittleBirdAimRenderer：
  - GunReticle 精简为 (x, y, visible, gapPixels)，不再携带 distanceBlocks / spreadDegrees（无人使用）；
  - 删除机枪弹着点预测：gunImpact（方块+实体射线）、midpoint、gunImpactLeft/Right 字段、
    以及随之失去引用的 vec 辅助与 ProjectileUtil / EntityHitResult 导入；
  - 新增 centreCrosshair(helicopter, fov, width, height)：x/y 固定为 width/2、height/2，永远 visible；
  - 火箭弹道预测与火箭准星完全不变（仍然投影弹着点并做 6% 钳位 + 帧间平滑）。
- LittleBirdCrosshairMath：
  - 删除第 35 节临时加的 outsideFrame（已无调用者）及其测试；
  - 新增 crosshairGapPixels(spreadDegrees, referenceDistance, verticalFovDeg, height)：
    把**色散锥在汇聚距离（32 格）处的半径**折算成屏幕像素，钳在 3~28 px；
    这是固定十字唯一还会动的部分，所以枪管过热时开口仍会肉眼可见地张开。
- HUD 侧零改动（drawGunCrosshair 只用 x/y/visible/gapPixels）。

### 36.3 已知取舍

- 准星不再指示真实弹着点：飞向山体/建筑时不会再把十字钉在墙上，只表示炮口轴线。
  这是用户明确选择的行为（要弹着点信息就切第三人称，或后续加"第二个小圆点表示弹着点"）。
- 如果同时把 view.show_center_crosshair 打开，会出现两个中心十字（自绘简易十字 + 机枪十字），
  默认该配置为 false，保持不开即可。

### 36.4 验证

**141 个单测全绿**（删 1 加 1：移除 outsideFrame 测试，新增 theCentreCrossOpensUpWithHeat——
覆盖冷/热开口单调、零散布与非法参数回退最小间隙、45 度极端散布被钳到上限）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 FB985293... 一致（206610 字节）。构建成功不等于游戏内验证通过。

## 37. 载具乘客免疫摔落伤害 + 俯冲撞地直接坠毁

用户要求：「飞机上的玩家免疫摔落伤害，俯冲撞地直接坠毁」（同时了结第 31.6 节遗留的「俯冲撞地不扣血」）。

### 37.1 摔落免疫

- 服务端每 tick 对**全部乘客**调用 resetFallDistance()（LittleBirdEntity.tickServer 开头）：
  下降的是机体而不是人，乘客不应累积摔落距离，落地自然不吃摔落伤害。
- 只对**正在搭乘**的玩家生效：跳机之后的自由落体仍然按原版结算（该摔就摔）。

### 37.2 俯冲撞地 = 坠毁

判据是新的纯规则 HelicopterFlightModel.isDiveCrash(verticalSpeed, pitchDeg, cfg)：

| 条件 | 默认值 | 含义 |
| --- | --- | --- |
| 下降率 >= verticalCrashSpeedThreshold | 0.85 格/t（约 17 格/秒） | 总距最多只能给出 0.5 的下降，超出即视为失控/俯冲 |
| 或 撞地时机头下俯 >= diveCrashPitchDeg 且下降率 >= crashMinImpactSpeed | 55 度 / 0.25 格/t | 覆盖「低空、速度不大但姿势很陡」的撞地 |
| 下降率 < crashMinImpactSpeed | 0.25 格/t | 轻触一律算降落，哪怕姿势难看 |

- 客户端只在**垂直碰撞且向下**时判定并上报，新增 LittleBirdCrashPacket（通道 id 6）与实体侧
  applyCrash(sender)：
  1. 校验发送者就是驾驶员、机体未被移除；
  2. **先把乘客抛出**：stopRiding、resetFallDistance、invulnerableTime = 20、沿机体速度 0.4 倍 + 0.25 上抛；
  3. 再 destroyAircraft()（爆炸 + 移除，与被打爆同一套表现）。
- 结果：俯冲撞地 = 机体当场报废，驾驶员被抛出且不会被自己的残骸爆炸带走；结合 37.1，代价是飞机不是人。
- 水平撞墙的既有模型不变（满速撞墙约 84 点伤害，同样是报废）。

### 37.3 已知取舍（待确认）

- **无人驾驶**的飞机自行坠落（applyUnpilotedFall）目前仍只是停在地上，不会自毁；
  要不要一起纳入坠毁判定，等用户决定。
- 陡角低速触地也算坠毁（55 度 + 0.25 格/t）。嫌严格就把 FlightConfig.diveCrashPitchDeg 调大；
  这三个阈值目前只在 FlightConfig 里，未暴露到 Forge 配置（与 crashDamagePerSpeed 一致）。

### 37.4 验证

**143 个单测全绿**（+2：divingIntoTheGroundWrecksTheAirframe、gentleDescentsStayLandings）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 AFDA6933... 一致（208914 字节）。构建成功不等于游戏内验证通过。

## 38. 修复：降落「落地过猛」——垂直轴补上机体阻力 + 坠毁阈值重新标定

用户提问：「为什么我搭乘飞机在降落时还是会落地过猛」。

### 38.1 根因（为什么旧模型一定会砸地）

垂直轴此前**完全没有阻力**，只有「总距伺服」和「下沉/俯冲加速度」，于是：

| 场景 | 旧行为 |
| --- | --- |
| 总距居中悬停 | vy 每 tick -0.006，一路累积到 maxFallSpeed 上限 **1.2 格/t = 24 格/秒** |
| 机头略压（约 30 度）下降 | 俯冲耦合给约 0.021 格/t^2，同样一路累积到 1.2（约 3 秒） |
| 压 90 度俯冲 | 1.2 格/t = 24 格/秒，且**降不下来**（没有东西限制它） |
| 按住 S 总距下降 | 伺服把 vy 拉到 -0.5 格/t = **10 格/秒**（这已经比真实直升机着陆快一个量级） |

也就是说：**只要不按住总距，任何下降都会一路加速到 24 格/秒**，
所以「落地过猛」是模型本身的必然结果；而第 37 节的坠毁判定（下降率 >= 0.85）又把这种
正常下降当成俯冲，着陆直接判成坠毁。

### 38.2 修复

1. **新增 verticalDrag（默认 0.10，仅作用于「没有被总距指令的」垂直速度）**：
   悬停下沉、压坡度下降、俯冲下降都会在上面的加速度与阻力之间收敛到一个合理速度；
   按住 W/S 时仍然走总距伺服（不受阻力影响），所以「要下降多快」依旧由玩家决定。
   收敛值（估算）：悬停 0.06 格/t（1.2 格/秒）、30 度 0.21（4.2）、60 度 0.39（7.8）、
   90 度 0.70（14）；熄火自由落体仍为 1.2（24 格/秒，阻力只作用于旋翼有升力时）。
2. **坠毁阈值重新标定**：verticalCrashSpeedThreshold 0.85 -> **1.00**。
   现在正常下降最多 0.5（总距）或 0.2~0.7（俯冲耦合+阻力），只有**熄火自由落体/真正俯冲**
   才会碰到 1.0；俯冲撞地仍由「机头 >= 55 度且下降 >= 0.25」这条姿态判据抓住，
   所以「俯冲撞地直接坠毁」的语义没变，只是不再误伤正常着陆。

### 38.3 验证

**144 个单测全绿**（+1 aTiltedRotorDescentAlsoSettles；centredCollectiveSettlesInsteadOfHovering
改为 400 tick 后断言「收敛在 -0.2 以内、不会冲到坠落上限」；
aNinetyDegreeDiveTradesForwardSpeedForDescent 的下降速度断言由 1.2 放宽到 0.5，
因为阻力现在会给俯冲一个终端速度）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 FCC43C27... 一致（208967 字节）。

### 38.4 仍需游戏内确认

- 悬停下沉 1.2 格/秒是否够「稳」；嫌飘就加大 verticalDrag（0.15~0.2）。
- 俯冲终端速度 14 格/秒是否够猛；想要更快的俯冲可以把 verticalDrag 降到 0.05。
- 总距下降仍是 10 格/秒（有意保留：玩家按 S 就是要求这个下降率）；嫌太快可以调
  FlightConfig.maxDescentSpeed（目前未暴露到 Forge 配置）。

## 39. 搭乘期间玩家免疫所有伤害

用户要求：「算了，在飞机上时让玩家免疫所有伤害」（取代第 37 节的「只免疫摔落伤害」）。

### 39.1 实现

- 新增本模组**第一个 common 侧事件类** LittleBirdRiderProtection
  （Forge 事件总线，LivingAttackEvent）：只要受击者的直接载具是 LittleBirdEntity，
  就 setCanceled(true)。
  - LivingAttackEvent 在 LivingEntity.hurt 内、伤害与击退/受击表现之前触发，
    取消后返回 false，**所有伤害来源**（摔落、爆炸、火焰、近战、弹射物、PvP…）一律无效；
  - 客户端与服务端都会触发，所以受伤动画/红屏也一并消失；
  - 走的是 Forge 事件而非 Mixin，与项目既有边界（无 Mixin）一致。
- 只作用于**正在搭乘**的玩家：跳机后立刻恢复正常易伤。
- 因此第 37 节「俯冲坠毁前先抛出乘客」里的 invulnerableTime = 20 仍然是必要的桥梁：
  抛出瞬间免疫就结束了，而残骸还在脚下爆炸。
- tickServer 里对乘客 resetFallDistance 保留：它保证**站在地上跳机**不会被残留摔落距离误伤，
  而**高空跳机**则从头开始累计（该摔还是摔）。

### 39.2 边界与取舍

- 机体本身照常掉血（被打、撞墙、俯冲坠毁都还在），免疫只给乘员。
- 虚空伤害（fellOutOfWorld，带 BYPASSES_INVULNERABILITY 标签）在事件之前就被判定，
  所以掉出世界依旧会死；/kill 同理。
- 爆炸仍会把乘员**推开**（击退在 Explosion 内独立施加），但不再扣血——观感上还留着"被炸飞"的反馈。
- 代价：坐在飞机上时无法被任何人打死，包括 PvP。（用户明确要求，若要留 PvP 口子可以按伤害类型放行。）

### 39.3 验证

**144 个单测全绿**（纯事件接线，不影响纯规则测试）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 E76F4944... 一致（209803 字节）。构建成功不等于游戏内验证通过。

## 40. 小鸟狗斗：机炮与火箭对机体的独立伤害

用户要求：「加入小鸟狗斗，1 发子弹命中机体 -1 血，火箭命中 -10 血」。

### 40.1 之前为什么打不起来

- fireGun 的实体射线谓词里带着 !(candidate instanceof LittleBirdEntity) —— **机炮根本锁不到另一架小鸟**，
  狗斗无从谈起（此前飞机之间只能靠火箭爆炸范围蹭伤害）。
- 火箭对任何目标都是 rocket_direct_hit_damage = 40 直击**再加上**半径 3 的爆炸伤害：
  一发就能把 80 血的机体打残甚至带走，没有来回。

### 40.2 改动

1. 机炮射线放开对 LittleBirdEntity 的限制（自身与自己的驾驶员仍然排除）→ 可以互射。
2. 新增纯规则 LittleBirdBallistics.damageTo(airframe, normalDamage, airframeDamage)：
   目标是机体取狗斗价，其它目标取原价；负数/NaN 一律按 0，避免"负伤害治疗"。
3. 机炮命中机体走 gun_damage_aircraft（默认 **1**），其余目标仍是 gun_damage（14）。
4. 火箭直击机体走 rocket_damage_aircraft（默认 **10**），其余目标仍是 40。
5. **火箭爆炸不再向机体二次收费**：LittleBirdEntity.hurt 忽略「伤害类型是爆炸 且
   directEntity 是 LittleBirdRocketEntity」的伤害。为让直击与爆炸可区分，火箭直击的伤害来源改为
   **记在开火玩家名下**（explosion(owner, owner)），而爆炸本身来源仍是火箭实体。
   结果：一发火箭命中机体 = 恰好 10；被爆炸擦到的机体 = 0（狗斗要求真的打中）；
   同时 TNT/苦力怕等其它爆炸对机体照常生效。
6. 机体被击毁时的自爆仍会波及旁边的飞机（那次 directEntity 是飞机、不是火箭）—— 保持原样。

### 40.3 数值与节奏

- 机体 80 血：机炮 1 发/血、20 发/秒 → 打死一架需要 **80 发 ≈ 4 秒**连续命中；
  机枪过热上限是 120 发（6 秒），所以**一个过热周期刚好够打下一架**。
- 火箭 10 血/发、单侧弹巢 7 发 → 一个弹巢打不掉（70 血），**8 发**（约 1.2 秒连发）才能带走一架；
  自动补给 10 秒 7 发，狗斗节奏是"打光火箭再用机炮补刀"。
- 新增 2 个配置（common/balance）：gun_damage_aircraft（0~60，默认 1）、
  rocket_damage_aircraft（0~200，默认 10），实例 littlebird-common.toml 已写入。

### 40.4 验证

**145 个单测全绿**（+1 airframesArePricedForDogfights：机体/非机体取价、负数与 NaN 归零）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 06033A00... 一致（210411 字节）。构建成功不等于游戏内验证通过：
**两架飞机的实际互射、命中反馈与击毁爆炸需要双人（或第二架飞机）实测**。

### 40.5 待确认

- 狗斗时 HUD 看不到敌方机体血量（本机只有「机体 xx%」文本）；要不要加敌方血条或命中计数？
- 机炮对**玩家**仍是 14 伤害（两枪一个），但搭乘期间玩家免疫伤害，所以狗斗里真正的血量只有机体。

## 41. 击中敌机时的目标血量读数（准心下方红色小长条）

用户要求：「击中敌机时在准心下方显示一个红色小长条框来显示敌机剩余血量 100%，从右往左缩短」（重试一次）。

### 41.1 数据通路

- LittleBirdHitFeedbackPacket 由 (boolean kill) 扩为 (boolean kill, boolean aircraft, float healthFraction)，
  仍然是服务端到开火玩家的单播（PacketDistributor.PLAYER）。
- 机炮（fireGun）与**火箭直击**（LittleBirdRocketEntity.explode）都在造成伤害后上报：
  - aircraft = 目标 instanceof LittleBirdEntity（只有打飞机才显示这条读数）；
  - healthFraction = LittleBirdEntity.healthFractionOf(目标)：机体用 getHealth() / MAX_HEALTH，
    其它生物用 getHealth() / getMaxHealth()，所以满血机体的读数正好是 100%。
- 火箭此前**完全没有命中反馈**（只有机炮有），这次一并补上，否则火箭命中看不到血量条。

### 41.2 客户端显示

- LittleBirdHud 新增 enemyBarUntil / enemyBarFraction；onHitFeedback(kill, aircraft, fraction)
  在目标是机体时刷新，**显示 3 秒**（每次命中重新计时）。
- drawEnemyHealthBar（画在准心下方 ENEMY_BAR_OFFSET_PIXELS = 16 处）：
  1. 红色外框 DANGER —— 就是需求里的「红色小长条框」；
  2. 深色轨道 + 红色填充，填充**左端固定**，宽度 = enemyHealthFillWidth(fraction, 64)，
     所以掉血时是**从右往左缩短**；
  3. 条下方居中显示百分比文本（healthPercentLabel，如 100% / 87% / 0%）。
- 纯规则：LittleBirdHudLayout.enemyHealthFillWidth（越界钳位、NaN -> 0、宽度 <= 0 -> 0）与
  LittleBirdHudMath.healthPercentLabel（0~100%，钳位与 NaN 安全），都可单测。

### 41.3 验证

**147 个单测全绿**（+2：enemyHealthBarEmptiesFromTheRight、healthPercentLabelReadsAsAPercentage）。
gradlew.bat build 成功并自动同步，实例只有 1 份 JAR，
源/目标 SHA-256 8A5FE9AA... 一致（212029 字节）。
仍需游戏内确认：条的位置会不会压到俯仰梯、3 秒停留是否合适、百分比字号是否够看。

### 41.4 待确认

- 现在只有「打中才显示 3 秒」；要不要改成持续锁定最近命中的目标并实时刷新（更接近 BF 的目标 HUD）？
- 非飞机目标（玩家 / 生物）目前不显示这条读数；要不要也显示？

## 42. 改名：modid littlebird → battlefield（M8.5，阿帕奇前置切片）

用户决定：加入阿帕奇后本模组不再只装小鸟，**modid 改为 battlefield**。

### 42.1 改动清单（命名空间层面；Java 包名 io.github.littlebird 与类名不动）

| 位置 | 改动 |
| --- | --- |
| Java 常量 | LittleBird.MOD_ID: "littlebird" → "battlefield"（LittleBird.java:18） |
| 资源目录 | src/main/resources/assets/littlebird/** → assets/battlefield/**（整目录移动） |
| sounds.json | "littlebird:engine/gun/gun_shot/rocket_launch/explosion" → battlefield:* |
| 语言文件 | entity./item./key./hud./alert./itemGroup. 前缀全改（en_us + zh_cn）；标签值改 Battlefield / 战地载具 |
| 物品模型 | models/item/little_bird.json 的 layer0 命名空间 |
| HUD 代码 | LittleBirdHud 的 10 处 hud.littlebird.* / alert.littlebird.* |
| 键位 | LittleBirdKeyMappings 的 key.categories / key.littlebird.exit、cockpit、salvo |
| 创造标签 | ModCreativeTabs 的 itemGroup.littlebird |
| gradle | mod_id=battlefield；mod_name=Battlefield Aircraft；mod_description 改为两种直升机 |
| 实例 | 删除 mods/littlebird-0.1.0.jar，换成 battlefield-0.1.0.jar；config 复制为 battlefield-*.toml 后删除旧文件 |

### 42.2 自审抓到的真实问题（已修）

**PowerShell 的 `Set-Content -Encoding UTF8` 会写入 UTF-8 BOM**，第一次编译直接失败：

    LittleBirdHud.java:1: 错误: 非法字符: '\ufeff'
    LittleBirdHud.java:1: 错误: 需要 class、interface、enum 或 record

已用 .NET `UTF8Encoding($false)` 重写这 5 个被批量替换过的文件去掉 BOM。
**教训（写入 AGENTS/后续沿用）**：脚本改文本必须显式无 BOM
（`[System.IO.File]::WriteAllText($p, $text, (New-Object System.Text.UTF8Encoding($false)))`），
或优先用 edit/write 工具。

### 42.3 验证

- **147 个单测全绿**；`gradlew.bat build` 成功并自动同步；
- 实例只有一份 JAR：**battlefield-0.1.0.jar**（212201 字节，SHA-256 `7CA1867B...`，
  与 build/libs 完全一致）；
- 实例配置：`battlefield-client.toml` / `battlefield-common.toml`（内容从旧文件复制，保留所有调参）。

### 42.4 影响与提醒

- **旧存档里已放置的小鸟实体会失效**（注册名由 `littlebird:little_bird` 变为 `battlefield:little_bird`）；
  测试世界需要重新发一个物品。
- 历史文档（PLAN.md、第 1~41 节）里的 "littlebird" 是当时的真实记录，**不回改**。

## 43. 阿帕奇 M9：纯规则层（两段弹仓 / 炮塔 / 线控导弹 / 飞行预设 / 挂点）

目标第 1 轮切片（docs/plan/apache-plan.md 第 10 节 M9）。全部是纯规则类（无 Minecraft 类）+ JUnit。

### 43.1 新增内容

| 文件 | 内容 | 关键常量（用户已确认） |
| --- | --- | --- |
| entity/weapon/TwoStageMagazine.java | 两段弹仓：ready / reserve / 装填 / 补给，纯状态机 | rocketBay() = 19+19、装填 40t、补给 300t 补满 19；missileRail() = 1+5、装填 100t、补给 200t 补 1 发 |
| entity/weapon/ChainGunTurret.java | 30mm 炮塔：射界、回转限速、600rpm、过热、散布 | 方位 ±86°、仰 +11°/俯 −60°、回转 6°/t、每 2 tick 一发、MAX_HEAT 50（5 秒）、锁定 80t |
| entity/weapon/ApacheMissileBallistics.java | 陶氏线控（SACLOS）：朝操作者视线转向 + 重力偏置 | 初速 1.20、巡航 3.00、加速 10t、最大转向 7°/t、寿命 140t、射程 220 格 |
| entity/flight/ApacheFlightProfile.java | 阿帕奇飞行预设（更重、极速 90） | 1.25 格/t（=90 km/h）、yaw 6.0、pitch 4.5、roll 4.5、rollSpinUp 14、verticalDrag 0.12、俯仰仍 ±90、坡度仍无上限 |
| entity/weapon/ApachePods.java | 19 发在两翼之间的交替/剩余推导（给模型与 HUD 用） | 左 10 / 右 9 管，总是等于共享 ready 数 |

### 43.2 自审抓到并修掉的真实缺陷

**链炮射速实际是 20 发/秒而不是 10 发/秒**：
首版 `advance` 在扣减冷却后**同一 tick**就判定开火并置 `cooldown = INTERVAL - 1`，
于是冷却永远归零、每 tick 都发射。修正为「先判定开火（置满 INTERVAL）**再**消耗本 tick」，
周期精确等于 `FIRE_INTERVAL_TICKS` = 2 tick → 600 rpm → 5 秒正好 50 发过热。

这个缺陷之所以第一次没被测试抓住，是因为 `sixHundredRoundsPerMinute` 只数了 100 tick 内的发数：
20 发/秒 会在第 50 tick 过热锁定、后面 50 tick 不开火，总数**恰好也是 50**（巧合）。
现在该测试与新的过热测试（断言锁定发生在第 95~100 tick）同时约束射速与过热，
`ApachePods` 的越界输入语义也在测试里写清楚了（过多 ready = 视为未发射；过少 = 视为打空）。

### 43.3 验证

- **176 个单测全绿**（147 → 176，新增 29 个：弹仓 9、炮塔 7、导弹 6、飞行预设 3、挂点 4）；
- `gradlew.bat build` 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（224419 字节，SHA-256 `42F68AAC...` 与 build/libs 一致）。

### 43.4 下一片（M10）

双座实体与同步：ApacheEntity、座位分配（先上机者主驾驶）、V 切座、副驾驶输入包、
SynchedEntityData 里的炮塔角度、NBT 持久化。（已在第 44 节完成。）

## 44. 阿帕奇 M10：双座实体与同步

目标第 2 轮切片。**注意：本片还没有模型与渲染器，阿帕奇在游戏里暂时是隐形的**（M11 建模渲染）。

### 44.1 新增内容

| 文件 | 内容 |
| --- | --- |
| entity/seat/ApacheSeat.java（纯） | 座位规则：后座 = 主驾驶(PILOT)、前座 = 副驾驶(GUNNER)；乘员按登机顺序存放，**座位 = 列表下标 XOR 一个「换座」标志**，因此 V 切座只翻转一个同步布尔量，不动乘客列表，单人也能切到前座 |
| entity/flight/FlightControls.java（纯） | 四个控制位（CLIMB/DESCEND/ROLL_RIGHT/ROLL_LEFT）与轴还原，两机共用 |
| entity/ApacheGeometry.java | 单位空间尺寸：**SCALE 0.91 = 小鸟 0.65 的 1.4 倍**；串列座舱（后座更高更靠后、前座更低更靠前）；命中箱 2.366 格 |
| entity/ApacheEntity.java | 载具实体：extends Entity implements GunshipRider；飞行沿用小鸟的客户端权威模型 + ApacheFlightProfile；座位/登机（先上机者主驾驶）/V 切座/副驾驶瞄准校验；炮塔由服务端按副驾驶视线回转并同步两个角度；血量 120（待标定）、坠机与撞墙伤害、NBT（血量/发动机/旋翼/武器/换座标志） |
| item/ApacheDeployItem.java | 部署物品（向前 4 格生成，创造标签里与小鸟并列） |
| network/ApacheSeatSwitchPacket.java（通道 id 7） | V 键：翻转换座标志，服务端校验发送者确实是本机乘员 |
| network/ApacheGunnerAimPacket.java（通道 id 8） | 副驾驶视线(yaw,pitch)+扳机；**只在变化 >=0.25 度或扳机变化时发送**；服务端只接受前座乘客的包 |
| client/ApacheInput.java | 客户端输入：主驾驶给 W/S/A/D 轴；副驾驶不发飞行轴、只发瞄准；V 键（LittleBirdKeyMappings.SEAT_SWITCH，默认 V） |

### 44.2 自审抓到并修掉的两个真实缺陷

1. **单人切到前座会把飞机摔了**：换座以后 getControllingPassenger() 变成 null，没有任何客户端再驱动
   运动学，机体失速下坠，副驾驶根本没法用炮。修正：新增 applyUnpilotedMotion ——
   **机上有人但没人坐主驾驶时旋翼托住机体**（水平速度按 0.85 衰减、垂直按 hoverSinkAccel 缓慢下沉）；
   **机上无人或旋翼未起转**才按自由落体处理（沿用小鸟的坠机模型）。
2. **对阿帕奇的敌机血量条会永远显示 100%**：第 41 节的 healthFractionOf 只认 LittleBirdEntity 与普通
   LivingEntity，而 ApacheEntity 不是 LivingEntity，于是落到兜底分支返回 1.0。
   已补 ApacheEntity 分支（按 ApacheEntity.MAX_HEALTH 计算）。

另外修掉：炮塔在 M13 之前不该「空转过热」——tickTurret 现在传 trigger=false，
扳机只存下来给武器切片用，瞄准照常回转。

### 44.3 本片留下的统一项（M15 处理）

- 小鸟仍用自己的 CONTROL_* 常量，未切到 FlightControls；
- 两个部署物品几乎是同一份代码（可抽成带实体工厂的通用类）；
- 三个包处理器里是 instanceof 链（可抽成接口方法）；
- 阿帕奇还没有舱内/座位相机与模型（M11）；
- 阿帕奇的武器数据只有默认值，武器逻辑在 M12/M13；
- healthFractionOf 暂时挂在 LittleBirdEntity 上。

### 44.4 验证

- **187 个单测全绿**（176 -> 187：座位 5、控制位 2、几何 4）；
- gradlew.bat build 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（244629 字节，SHA-256 6BBA5AC8... 与 build/libs 一致）；
- 本地化新增 entity/item.battlefield.apache 与 key.battlefield.seat_switch（中英）。

**未做游戏内验证**：本片无法在游戏里目视（无模型），下一片 M11 建模渲染后一并验收。

## 45. 阿帕奇 M11：模型、贴图与渲染

目标第 3 轮切片。阿帕奇现在**在游戏里可见了**（本片交付模型 + 渲染 + 程序化贴图 + 座位相机）。

### 45.1 新增内容

| 文件 | 内容 |
| --- | --- |
| client/ApacheModel.java | 单位空间模型（沿用小鸟约定：机头朝 -Z、1 块 = 16 单位、由渲染器施加 SCALE）。部件：机身/机腹/机顶、加长的阶梯机头 + 下颌传感器、**串列双座舱**（座舱玻璃 + 框架 + 座椅之间的防爆隔板 + 前后仪表台）、两侧发动机舱与排气、**短翼（每侧外侧 19 管火箭巢 + 内侧导弹发射梁）**、**下颌链炮炮塔（独立 yRot/xRot）**、前三点轮式起落架、尾梁/垂尾/平尾/左侧尾桨、**桅顶长弓雷达（缓慢旋转）**、4 叶主旋翼 + 模糊盘 |
| client/ApacheModelMath.java（纯） | 雷达扫描角（含 wrap）、**炮塔 yRot = 负方位角**（模型镜像）、**xRot = 正仰角**（-Z 炮管被正 X 旋转抬起）、管位装填判定 |
| client/ApacheRenderer.java | 位姿（yaw/pitch/roll 沿用小鸟的符号）、按 ready 数显示两翼管位与导弹、炮塔按同步角度指向、玻璃与旋翼模糊两个半透明 pass |
| tools/generate_apache_texture.ps1 | 程序化贴图（512²，**深灰绿**配色以区别于小鸟的石墨黑）：五段 UV 带（机身面板 / 座舱玻璃 / 深色金属+桨尖条 / 火箭巢黑+暗红管口 / 炮塔-机轮-仪表带） |
| assets/battlefield/textures/entity/apache.png | 生成的贴图（9737 字节） |
| ApacheEntity | 新增舱内视角开关（客户端字段）、4 个挂载读数（ready/reserve 火箭与导弹，**先给满载默认值，M12 接手真正的弹仓**）、NBT 保存、与 setCockpitView 对应的模型隐藏 |
| ApacheInput | **C 键切座舱视角**：切到座舱时强制第一人称并隐藏整机；离开时恢复原视角；登机时强制第三人称（与小鸟一致的观感） |

### 45.2 自审抓到并修掉的问题

1. **PartDefinition 上不能设 visible**：首版在建模方法里写了 glass.visible = false / radar.visible = true，
   直接编译失败（可见性是 ModelPart 的字段，PartDefinition 只是建模期的构造器）。
   已把可见性统一放到构造器里按名字取到的 ModelPart 上 —— 与小鸟的写法一致。
2. **按 C 隐藏整机后什么都没了**：只翻转模型可见性而不切相机，第三人称会看到"隐形飞机"。
   现在 C 会同时强制第一人称，离开座舱视图再恢复。
3. **管位显示的是"可发射数"而不是总挂载**：ready 19 分布在两个 19 管巢上（10 + 9），
   所以满载时两翼看起来只有一半管位有弹。这是 ready/reserve 抽象的直接结果，**故意保留**
   （管子表示"现在能打多少"，与 HUD 的 19/19 数字一致）。若希望"物理上两翼始终满载"，
   改成 ready + reserve 即可（一行）。

### 45.3 遗留项

- 链炮的热辉 pass（对应小鸟的 gun heat）留到 M13 与射击一起做；
- 座位相机目前只用座椅位置 + 隐藏整机（没有像小鸟那样前移到风挡之外，因为整机隐藏后没有遮挡）；
- 模型的部件数量明显多于小鸟（含 2×19 管位），尚未做帧率测量（游戏内验收项）。

### 45.4 验证

- **191 个单测全绿**（187 -> 191：模型数学 4）；
- gradlew.bat build 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（257383 字节，SHA-256 BBA7B36B... 与 build/libs 一致）；
- 已确认打包内容：apache.png 与 13 个 Apache 类都在 JAR 内。

**仍需游戏内目视**：比例（比小鸟大 40%）、贴地（机轮着地）、串列座舱与炮塔朝向、雷达旋转、
旋翼模糊、舱内视角是否干净 —— 构建通过不代表观感正确。

## 46. 阿帕奇 M12：主驾驶武器（19/19 火箭巢 + 1/5 陶氏）

目标第 4 轮切片。后座现在能开火了：19 + 19 火箭、1 + 5 导弹、按 R 装填、1/2 切换武器。

### 46.1 新增内容

| 文件 | 内容 |
| --- | --- |
| ApacheEntity | 接入两个 TwoStageMagazine（火箭 19+19 / 导弹 1+5，与 M9 的纯规则一致）；每 tick 推进弹仓并同步 ready/reserve；扳机按 2 tick 间隔发射火箭（10 发/秒，19 发约 1.9 秒打完）、导弹 10 tick 间隔；两翼交替发射（ApachePods.nextPod），左右挂点位置来自 ApacheGeometry（前后/侧向/高度都是模型里的实测值）；NBT 保存两个弹仓的 4 个计时/计数 |
| entity/ApacheMissileEntity.java | 陶氏导弹实体：**SACLOS** —— 每 tick 用 ApacheMissileBallistics 朝操作者视线转向，带转向上限、加速段、重力偏置、寿命 140t / 射程 220 格；扫掠式命中判定（与火箭同款，比较方块与实体谁更近）；命中/到期爆炸半径 2，直击 60、对机体 20（走共享的机体定价）；命中反馈复用第 41 节管线；烟雾尾迹 |
| network/ApacheReloadPacket.java（id 9） | R 键装填；服务端校验发送者确实是驾驶员，再把两个弹仓从 reserve 装到 ready |
| LittleBirdWeaponInputPacket | 加阿帕奇分支（后座：武器选择 + 扳机），小鸟逻辑不变 |
| LittleBirdRocketEntity | 新增 damageScale（NBT 持久化）：**同一发 70mm 火箭按挂载定价** —— 小鸟 1.0（直击 40 / 机体 10），阿帕奇 0.6（直击 24 / 机体 6），因为阿帕奇一次带 38 发 |
| 键位 | 新增 RELOAD（默认 R），与 V/C 一起注册；本地化补 key.battlefield.reload（装填 / Reload） |

**为什么制导用机体指向而不是服务器上的玩家视线**：骑乘时原版只发载具位姿包，服务器手里的玩家 yaw/pitch
是旧的，所以"操作者视线"在服务端只能取自同步过的机体姿态 —— 而本模组的飞控里准星与机头本来就是同一个方向，
因此这就是 SACLOS 的忠实实现。视线移开就会把导弹带偏，这是线控武器的特征。

### 46.2 自审抓到并修掉的问题

1. **火箭 launch 是三参数**（方向、初速、owner），首版只传了两个 → 编译失败；已改为显式传
   LittleBirdRocketBallistics.INITIAL_SPEED。
2. **导弹渲染器无法复用**：火箭模型/渲染器原本是 EntityModel<LittleBirdRocketEntity> /
   EntityRenderer<LittleBirdRocketEntity>，给导弹类型注册时泛型不匹配。
   已把两者改为 Entity 泛型（模型 setupAnim 本来就是空的，渲染器只用 yRot/xRot/tickCount，
   都是 Entity 成员），于是同一套模型服务两种弹药，没有复制代码。
3. **导弹爆炸会二次收费机体**：第 40 节的"一次命中只收一次费"只排除了火箭实体的爆炸，
   导弹的爆炸会把机体再算一次。已在小鸟与阿帕奇两边的 hurt 里同时排除 ApacheMissileEntity。
4. Vec3 与纯规则 Vector3 的互转：导弹实体补了 vector/vec 辅助（与火箭同款）。

### 46.3 遗留项

- **HUD 还没显示阿帕奇弹药**（19/19、1/5、装填进度），模型上的管位/导弹能看出余量，数字读数在 M14；
- 导弹暂时复用 70mm 火箭的模型（外观细化放 M15）；
- 数值（装填间隔、伤害缩放、导弹伤害）目前是类常量，进 Forge 配置放 M15 的平衡标定。

### 46.4 验证

- **191 个单测全绿**（本片是实体与网络接线，纯规则测试未变）；
- gradlew.bat build 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（267153 字节，SHA-256 D7D3F4A7... 与 build/libs 一致）。

**仍需游戏内验证**：连射节奏（19 发/1.9 秒）、R 装填的 2 秒/5 秒、导弹是否跟着机头走、
大装填 15 秒/10 秒补弹（陶氏一次 1 发）。

## 47. 阿帕奇 M13：副驾驶 30mm 链炮

目标第 5 轮切片。前座现在真的能开炮了：600 rpm、溅射 1 格、直击 8 / 对机体 3、过热 5 秒。

### 47.1 新增内容

| 文件 | 内容 |
| --- | --- |
| ApacheEntity | tickTurret 现在把真实扳机交给 ChainGunTurret.advance，并在返回 fire 时开火：**每 2 tick 一发（600 rpm）**、散布随热度（0.30 度 -> 1.20 度）、**连续 50 发（5 秒）过热锁定 80 tick**；新增 DATA_TURRET_HEAT 同步炮管热度 |
| ApacheEntity.fireChainGun | hitscan 沿**炮塔自身指向**（机体姿态 + 方位/仰角），射程 128；炮口位置 = 炮塔枢轴（机体 up/forward 向量旋转）+ 炮管长度，所以俯仰/坡度都跟得上；方块与实体都做判定，实体优先更近者 |
| ApacheEntity.splashChainGun | **溅射半径 1 格**：对范围内每个目标按距离线性衰减造成"该弹伤害的一半"，机体照旧走机体定价；排除已经被直击命中的目标，避免同一发算两次 |
| ChainGunTurret.aimDirection（纯规则） | 机体姿态 + 炮塔方位/仰角 -> 世界单位向量（MC 约定：x=-sin(yaw)cos(pitch)、y=-sin(pitch)、z=cos(yaw)cos(pitch)；仰角向上与 MC 俯仰正方向相反，所以相减） |
| ApacheModel / ApacheRenderer | 炮口热辉：新增 emissive 部件 barrel_glow + renderGunHeat，按同步热度渐亮；LittleBirdModelMath.heatGlowColor 增加 0..1 分数重载（小鸟用整数热度，两者共用同一套渐变） |
| ModNetwork.sendTracer | 参数由 LittleBirdEntity 泛化为 Entity（阿帕奇也要发曳光） |

### 47.2 自审抓到并修掉的问题

1. **sendTracer 写死小鸟类型**：阿帕奇发曳光时编译报"ApacheEntity 无法转换为 LittleBirdEntity"。
   已把第一个参数改成 Entity —— 里面本来就只用 TRACKING_ENTITY 跟踪，小鸟调用点不受影响。
2. **阿帕奇实体缺 vector/vec 互转辅助**：纯规则弹道用自己的 Vector3 类型，补上与小鸟同名的两个私有辅助。
3. 第一次接线时把旧的"只瞄准不开火"注释留在了 tickTurret 里，已随实现一起删掉（避免注释与行为不符）。

### 47.3 遗留项

- **副驾驶的 HUD（炮口准星、热度条、过热告警）在 M14**；目前靠模型上的炮塔指向与曳光判断弹道；
- 每发播放一次 30mm 音效（10 次/秒），小鸟是"客户端循环音效 + 同步枪态"；
  阿帕奇改成循环音效是 M15 的打磨项（列为统一项）；
- 数值仍是类常量（进配置放 M15）。

### 47.4 验证

- **193 个单测全绿**（191 -> 193：炮口指向方向 2 个，含单位向量与垃圾角度安全）；
- gradlew.bat build 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（271095 字节，SHA-256 F7D666C2... 与 build/libs 一致）。

**仍需游戏内验证**：前后座两人同乘时炮塔能否独立于机体扫射、曳光与命中反馈、
5 秒过热锁定的手感、1 格溅射的实际杀伤范围。

## 48. 阿帕奇 M14：HUD 复用抽象与副驾驶准星

目标第 6 轮切片。两机现在共用同一套 HUD 绘制，阿帕奇有了自己的武器面板与炮手准星。

### 48.1 新增与改动

| 文件 | 内容 |
| --- | --- |
| client/GunshipHudDraw.java（新） | 两机共用的绘制：中心簇（航向/速度/高度/俯仰梯/坡度）、面板与条、武器槽、告警、可选中央准星、命中标记、敌机血量条，以及**命中反馈的共享状态**（原先挂在小鸟 HUD 上；包处理器现在直接调它，两机共用） |
| client/LittleBirdHud.java | 中心簇 / 告警 / 准星 / 命中标记 / 敌机血量条改为调用共享绘制（**喂同样的数值，像素不变**）；随之失效的 4 个常量、4 个状态字段、3 个绘制辅助与 3 个私有方法一并删除，不留死代码 |
| network/LittleBirdHitFeedbackPacket.java | 命中反馈改调 GunshipHudDraw |
| client/ApacheHud.java（新） | 阿帕奇 HUD：共享中心簇 + 命中标记 + 告警 + 敌机血量条；**后座面板** = 所选弹药（19 管火箭巢 / 陶氏导弹）、ready/reserve 读数（19/19、1/5）、两条弹仓水平条、1/2/R 三个槽位、旋翼转速与机体血量；**前座面板** = 30mm 链炮名、炮塔方位角、**热度条**（按阈值变色）与同样的状态读数 |
| client/ApacheAimRenderer.java（新） | 把**炮塔自身指向**投影到屏幕（用同步的方位/仰角），出屏钳位 + 帧间平滑；**压到射界极限时标记 pegged**（准星变琥珀色并加中心点） |
| ChainGunTurret.atLimit（纯规则） | 判断炮塔是否压在方位 ±86 / 仰角 +11 / 俯角 −60 的极限上 |
| client/ClientModEvents.java | 注册第二个 HUD 覆盖层 apache_hud（与小鸟的 little_bird_hud 并存，各自只在自己的载具里绘制） |
| 本地化 | 新增 5 个键 × 2 语言：19 管火箭巢、陶氏导弹、30mm 链式机炮、装填、弹药耗尽 |

### 48.2 自审说明

1. 小鸟 HUD 这一轮是**重构**而不是重写：共享方法体一模一样，只是输入从实体换成参数，
   因此像素应当零变化；但**我无法在这里目视**，必须由你在游戏里对比一次（见 48.4）。
2. 删除死代码前确认过引用：drawReadout / segment / readoutWidth 与三个私有绘制方法在委托之后
   确实没有调用者（grep 证据），不是凭感觉删的。
3. 面板里的「装填条」目前显示的是**弹仓水平**（ready / capacity），因为服务端还没有同步
   装填与补给的倒计时；做成真正的倒计时进度条列入 M15（同步两个 int 即可）。

### 48.3 遗留项

- 面板条的语义见 48.2 第 3 点；
- 副驾驶准星用的是**服务器同步角度**，所以有约一帧的滞后（与炮塔实际指向一致，但不如本地预测跟手）；
- 阿帕奇没有「弹着点预测」（小鸟有）：炮塔独立于机体，预测需要额外射线，列入 M15 可选项。

### 48.4 验证

- **194 个单测全绿**（193 -> 194：炮塔压限判定）；
- gradlew.bat build 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（282003 字节，SHA-256 FA6FEC6A... 与 build/libs 一致）。

**必须游戏内对比**：小鸟 HUD 是否有像素回归（中心簇 / 告警 / 命中标记 / 敌机血量条 四项）、
阿帕奇两个座位的面板内容、副驾驶准星是否跟着炮塔走并在压限时变色。
## 49. 阿帕奇 M15：配置接线、真实装填进度条与统一项

目标第 7 轮切片：把新增的 [balance.apache] 配置真正接进实体与 HUD，把面板条变成真实的倒计时进度条，
并清掉 M9 到 M14 留下的重复项。

### 49.1 新增与改动

| 文件 | 内容 |
| --- | --- |
| entity/ApacheEntity.java | 弹仓规格 Spec 改为**构造期按配置生成**；火箭伤害倍率、链炮直伤 / 对机体伤害 / 溅射半径、两套装填与补给秒数全部走 LittleBirdConfig；新增 4 个同步量 DATA_ROCKET_RELOAD / DATA_ROCKET_SUPPLY / DATA_MISSILE_RELOAD / DATA_MISSILE_SUPPLY，附 getter 与 0..1 进度换算 |
| entity/ApacheMissileEntity.java | 陶氏直伤 / 对机体伤害 / 爆炸半径走配置；不再使用的三个旧常量直接删掉，默认值只保留在配置注释里（不留死代码） |
| client/ApacheHud.java | 面板条改成真实进度：每个弹仓一行两段——上段是小装填（有倒计时就显示倒计时，否则显示 ready 满度），下段是大装填补给（空闲时显示 reserve 满度）；补上 70mm / TOW 行标签 |
| entity/weapon/TwoStageMagazine.java | Spec 新增 withTimings(小装填, 大装填)：装药量与补给批次是设计值，只有时间归配置 |
| entity/LittleBirdEntity.java | 四个 CONTROL_* 常量改为 FlightControls 的别名，rollAxis / collectiveAxis 直接委托 FlightControls，两机共用一套控制位定义 |
| 本地化 | 新增 2 键 × 2 语言（70mm / TOW 行标签） |
| 测试 | 新增 2 个纯规则测试：默认规格锁死设计节奏、withTimings 只换时钟不动装药量 |

### 49.2 自审说明（含刻意不做的项）

1. 上一轮 48.2 第 3 点欠的账已还：面板条不再是「弹仓水平冒充进度」，而是服务端同步的真实倒计时；
   空闲时退回显示满度，避免出现一条永远空着的死条。
2. 弹仓装药量现在只有一个来源：被单测锁住的 rocketBay() / missileRail() 两个工厂，实体只替换时间。
   连 ROCKET_READY_CAPACITY 等四个公开常量也改成从工厂读回，HUD、同步默认值与 NBT 兜底不可能再和武器本身
   不一致（上一轮的写法等于让生产代码和单测各写一份 19/19、1/5，配置一改就会漂移，这正是这轮要消除的隐患）。
3. 刻意没做：合并两个部署物品。两个物品是目前唯一能明确表达「要哪一架」的入口，
   合并必须再引入选择 UI 或潜行切换，收益是负的，决定保留。
4. 刻意没做：把包处理器里的 instanceof 链抽成接口。链上每个分支的载荷不同（座位 / 武器 / 装填），
   抽接口只是把分支挪个地方；等第三架载具出现再抽才有意义。
5. 30mm 仍是每发一个音效事件。射速 10 发/秒 = 每 2 tick 一发，已被射速本身节流，不是音效风暴；
   真正的循环音需要绑定触发状态的客户端 SoundInstance，列入后续可选。
6. withTimings 第一版插错了作用域（落在外层类上），编译立刻报找不到符号，已移进 Spec 记录体内；
   这是编译期暴露的问题，没有流到运行时。

### 49.3 验证

- **196 个单测全绿**（194 -> 196）；
- gradlew.bat build 成功并自动同步；实例只有一份 JAR：
  **battlefield-0.1.0.jar**（284330 字节，SHA-256 8B34306C... 与 build/libs 一致）。

**必须游戏内验证**（这里无法目视 / 体验手感）：

1. 后座面板两条弹仓条：开火后按 R 装填，上条是否随小装填时间推进；
2. 大装填条：reserve 不满时是否在补给落地瞬间跳回并继续循环；
3. 70mm / TOW 行标签是否压到条形（像素回归）；
4. 改 battlefield-common.toml 的 rocket_reload_seconds（例如 2 -> 5）后重启，装填是否真的变慢；
5. 控制位统一后，小鸟的爬升 / 下降 / 左右滚是否与之前完全一致（纯等价改写，但必须实测确认）。
## 50. 阿帕奇 M16：机身比例、第三人称相机、物品图标与第一视角准心

回应游戏内实测反馈：机身太短、第三人称太逼仄、物品没有图标、第一视角没有准心。

### 50.1 证据（只读诊断）

| 反馈 | 证据 |
| --- | --- |
| 机身太短 | ApacheModel 机身 z 跨度 116 单位（机头 −54 到 尾 +62），乘 SCALE 0.91 再除 16 = **6.60 格**；主旋翼直径 160 单位 = **9.10 格**，机身:旋翼 = **0.73**。真实 AH-64：机身 15.06 m / 旋翼 14.63 m = **1.03**。参考小鸟：122 单位 × 0.65 = 4.96 格 / 6.5 格 = 0.76 |
| 第三人称太逼仄 | javap 反汇编 Camera#setup：第三人称分支只有一句 getMaxZoom(4.0)（偏移 122-133），而 Camera#getMaxZoom 是 **private**、setPosition/setRotation 是 **protected**；GameRenderer#renderLevel 里相机位移在 ComputeCameraAngles 事件**之前**就已经烘进 pose stack（事件在偏移 456，姿态旋转紧随其后），所以「在相机事件里挪相机」根本不会动画面 —— 这是原计划 A（访问转换器 + 事件后拉）被否决的直接证据 |
| 物品没图标 | ModItems 注册 battlefield:little_bird 与 battlefield:apache；资源目录只有 models/item/little_bird.json 与 textures/item/little_bird.png，**阿帕奇的模型与贴图都不存在**，于是渲染成缺失模型的紫黑方块 |
| 第一视角没准心 | ApacheHud 只在 show_center_crosshair（CLIENT 配置，默认 false）时才画中心十字；ApacheAimRenderer 只投影**炮塔**指向，而且对两个座位都画 |
| 顺带发现 | 阿帕奇此前**没有任何相机事件订阅**（只有 LittleBirdCamera 有），所以自由滚转时机头翻转而地平线不动 |

### 50.2 改动

| 文件 | 内容 |
| --- | --- |
| entity/ApacheGeometry.java | 新增机身布局常量（NOSE_TIP −70 / TAIL_TIP 94 / TAIL_FIN_LENGTH 16 / TAIL_BOOM_FRONT 4 / MAIN_ROTOR_RADIUS 80 / BELLY_BOTTOM 8）、airframeLengthBlocks / rotorDiameterBlocks / airframeToRotorRatio / tailReachBehindSeatBlocks；炮塔枢轴前伸 30 → 44、高度 8 → 4（垂到腹部线以下） |
| client/ApacheModel.java | 机头（下颌传感器 + 长鼻）、发动机舱、尾梁、垂尾、稳定面、尾桨、前轮按新布局重排，**坐标全部读 ApacheGeometry 常量**，不再手写魔法数字；机身 116 → 164 单位（6.60 → 9.33 格），与 160 单位旋翼比 1.02 |
| client/ApacheCameraMath.java（新） | 纯规则：第三人称距离钳位（4-24）、只抬高不缩短原版请求、第一人称请求 0 原样返回、滚转角度与配置系数 |
| client/ApacheCamera.java（新） | FORGE 客户端事件：相机随滚转（复用 camera_roll_factor） |
| mixin/CameraDistanceMixin.java（新） | 唯一的 Mixin：@ModifyVariable 抬高 Camera#getMaxZoom 的**入参**，让原版碰撞收缩继续生效；只对阿帕奇乘员、非座舱视角生效 |
| battlefield.mixins.json（新） | Mixin 配置（client 端 CameraDistanceMixin，defaultRequire 0） |
| build.gradle | 接入 org.spongepowered.mixin 插件（生成 refmap、写 MixinConfigs 属性）+ mixin 块；Mixin 处理器自动配置 |
| assets/.../models/item/apache.json + textures/item/apache.png（新） | 阿帕奇部署物品的模型与 16x16 图标 |
| tools/generate_item_texture.ps1 | 增加 -Kind apache（长机身 + 串列座舱 + 下颌炮 + 短翼挂巢 + 尾桨 + 轮式起落架）；小鸟分支代码不动，重跑后输出**字节不变** |
| client/ApacheHud.java | 乘员**常驻**中心准心（机头 = 屏幕中心 = 火箭/陶氏瞄准线），炮塔投影准星只画给副驾驶；顺带删掉因此失效的 LittleBirdConfig 引用 |
| LittleBirdConfig.java | 新增 CLIENT 键 apache_third_person_distance（默认 9.0 格，范围 4-24） |
| 测试 | ApacheGeometryTest +3（机身:旋翼比、尾梁长度与尾鳍收尾、下颌炮低于腹部且炮口不超机头），ApacheCameraMathTest +4（新文件） |

### 50.3 自审说明

1. **相机为什么必须用 Mixin**：见 50.1 的反汇编证据 —— 位置在事件前就进了 pose stack，而 getMaxZoom 是 private，Forge 也没有开放距离的钩子。原计划 A 会在编译期看起来正确、运行期毫无效果，属于最坏的一类改动，因此改用 Mixin。
2. **Mixin 风险控制**：只改 getMaxZoom 的**入参**（碰撞收缩照旧由原版做）；refmap 由 MixinGradle 生成，内容已核对为 getMaxZoom(D)D → Lnet/minecraft/client/Camera;m_90566_(D)D，并在 forge-1.20.1-47.4.20-srg.jar 里确认 m_90566_(double) 确实存在且签名一致；mixin 配置声明方式（jar 根 json + MANIFEST 的 MixinConfigs）是照抄同版本 Jade 的实际做法；**defaultRequire 设为 0**，万一注入点找不到只降级成原版 4 格距离并在日志告警，不会让游戏起不来。
3. **比例的目标值**：按真机 1:1（164 单位机身 vs 160 单位旋翼）。模型里所有关键 z 坐标现在都从 ApacheGeometry 读取，测试锁了三条不变量，下一轮再调比例只改常量即可。
4. **炮塔为什么要动**：机头加长后，原来 y=8 的炮塔盒会被新机头盒吞掉（不可见），降到 y=4 才真正垂在腹部线（8）以下；几何常量与模型同步修改，所以服务端 hitscan 的炮口起点（读同一常量）自动跟随，不存在两处各写一份。
5. **图标**：先重跑小鸟图标并比对 SHA-256 确认输出字节不变，再加阿帕奇分支 —— 顺手改坏已有资源是这类改动最常见的事故。
6. **准心语义**：主驾驶的火箭/陶氏是 SACLOS，瞄准线就是机头，中心十字才是正确瞄准具；此前主驾驶也被画炮塔准星属于语义错误，现已只给副驾驶。
7. **未解决/待确认**：反馈里的「玩家骑乘右穿模」没有说明是哪一种。若是相机穿进机体，本轮后拉应已消除；若是**玩家模型从机身侧面穿出**，需要按实际座位/侧面调座舱尺寸或座位横向偏移（这是我下一轮要处理的第一项）。另外命中盒仍是 2.37 立方的方形盒（MC 的 XZ 盒不能随偏航旋转），9.33 格长的机头机尾没有碰撞体，与小鸟一致，属视觉溢出。

### 50.4 验证

- **203 个单测全绿**（196 → 203：机身比例 3 条 + 相机纯规则 4 条）；
- gradlew.bat build 成功并自动同步；JAR **289350 字节**，SHA-256 **F8771364...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR；
- 打包内容已核对：jar 根有 battlefield.mixins.json 与 battlefield.refmap.json，MANIFEST 有 MixinConfigs: battlefield.mixins.json，models/item/apache.json 与 textures/item/apache.png 均在包内。

**必须游戏内验证**（这里只能验证到编译与产物，验证不到画面）：

1. 第三人称距离：默认 9 格是否合适，不够就改 battlefield-client.toml 的 apache_third_person_distance（4-24）；
2. 贴墙/进机库时相机是否仍被地形正确顶回（原版收缩是否还生效）；
3. 机身比例是否像真机；若整机还想更大，改 ApacheGeometry.SCALE（旋翼、命中盒、座位会一起变，要一起看）；
4. 链炮曳光起点是否从下颌炮口出来，而不是从机头里；
5. 快捷栏与掉落物的阿帕奇图标；
6. 第一视角：主驾驶中心十字、副驾驶炮塔准星，第三人称是否同样正常；
7. 日志里搜 battlefield.mixins.json，确认没有 injection 失败告警（有的话相机不会后拉，把日志给我）；
8. 相机滚转方向是否正确（与小鸟共用 camera_roll_factor）。
## 51. 阿帕奇 M17：陶氏尾迹的渲染距离与双马尾尾焰

反馈：陶氏（TOW）的渲染距离要拉远到至少 64 格，并且要有标志性的双马尾形状尾焰。

### 51.1 证据（只读诊断）

| 项 | 证据 |
| --- | --- |
| 导弹本体不是瓶颈 | ModEntities 里 apache_missile 的 clientTrackingRange 是 **16**。该参数单位是**区块**，即 256 格，所以导弹实体一直都在被同步，问题不在实体 |
| 真正的瓶颈是粒子广播 | javap 反汇编 ServerLevel#sendParticles(ServerPlayer, boolean, DDD, Packet) 可见：overrideLimiter 为 false 时距离上限是 **32.0**（ldc2_w 32.0d），为 true 时是 **512.0**（ldc2_w 512.0d）。原实现用的是批量重载 server.sendParticles(...)，内部正是 overrideLimiter=false，所以**尾迹在 32 格外就没了**，而导弹还在飞 |
| 原尾迹形状 | 原来每 2 tick 在导弹中心发 2 个 SMOKE，既没有尾焰方向、也没有形状，更谈不上双马尾 |

### 51.2 改动

| 文件 | 内容 |
| --- | --- |
| entity/weapon/ApacheMissileTrail.java（新） | 纯规则几何：rightOf（含垂直发射的退化兜底）、tailAnchors（左右各一条尾迹，半间距 0.14 格）、nozzle（喷口在质心后方 0.45 格）、samples（沿本段飞行取样）、particleRange（钳位 **下限 64**、上限 256、默认 96）、TRAIL_INTERVAL_TICKS=2 |
| entity/ApacheMissileEntity.java | spawnTrail 重写：沿**最近 2 tick 的飞行线段**（而不是只这一段）取样，左右各发一条竖直平行的 CAMPFIRE_COSY_SMOKE 尾迹（浓且长寿，才留得住两条线），喷口再各发 1 次 SMOKE 与 2 个 FLAME；新增 broadcastParticles：按配置距离**逐玩家**调用 sendParticles(player, options, overrideLimiter=true, ...)，从而绕开 32 格上限 |
| LittleBirdConfig.java | 新增 COMMON 键 missile_particle_range（默认 96.0 格，范围 **64-256**） |
| 测试 | ApacheMissileTrailTest（新，7 条）：两条尾迹分居两侧且间距有界、rightOf 单位且垂直、垂直发射不退化、喷口在轴线上且在机身后方、取样覆盖整段飞行、**一次发射覆盖自上次以来的每一 tick**、距离钳位下限 64 且大于原版 32 |

### 51.3 自审说明

1. **为什么用逐玩家 API 而不是自己拼包**：ServerLevel 已经公开了带 overrideLimiter 的逐玩家重载（上限 512 格），自己拼 ClientboundLevelParticlesPacket 只会多一份需要跟着版本维护的代码；距离检查由我们做，因此既能精确控制又能设下限。
2. **自审抓到的真实缺陷**：第一版每 2 tick 只沿**当个 tick** 的位移取样，而导弹每 tick 飞 3 格，于是尾迹每 6 格断一次、中间空 3 格 —— 高速弹上非常明显。已改为沿「自上次发射以来的整段」（TRAIL_INTERVAL_TICKS × 速度），并补了一条单测把这个不变量钉住（相邻取样点不超过一 tick 的飞行距离）。
3. **双马尾怎么来的**：两簇粒子不是靠随机散布（那只会得到一团），而是沿机体**右向量**固定偏移 ±0.14 格各发一簇，速度参数给 0 让烟留在原地，于是形成两条与航迹平行的细线；喷口处的 FLAME 提供「尾焰」，CAMPFIRE_COSY_SMOKE 提供长寿烟迹。垂直发射时叉乘退化，rightOf 有兜底，否则两条尾迹会叠成一条。
4. **带宽**：每次发射 4 个包（两条尾迹各 1 + 喷口 SMOKE 1 + FLAME 1），每 2 tick 一次，即约 2 包/秒/玩家；批量重载虽然也能给到 512 格，但它会把包发给半径内所有人而无法设下限，且它自己不会做「至少 64 格」的钳位。
5. **未做**：导弹本体没有做「飞行中持续冒烟的历史轨迹实体」之类的东西，尾迹仍由粒子构成，玩家把粒子设置调到最低时会变稀 —— 这是原版行为，未强行覆盖（overrideLimiter=true 只保证包送达，不改变客户端画质设置对粒子的节流）。

### 51.4 验证

- **210 个单测全绿**（203 → 210：尾迹几何 7 条）；
- gradlew.bat build 成功并自动同步；JAR **292006 字节**，SHA-256 **184664A9...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**（画面我在这里看不到）：

1. 站在 60-90 格外看别人（或自己回放式地看）发射陶氏：烟迹是否还在（原来 32 格就断了）；不够就调 battlefield-common.toml 的 missile_particle_range（64-256）；
2. 双马尾形状：是否两条平行细线、间距是否合适（太散改 TAIL_SEPARATION，0.14 格）；
3. 喷口是否有明显尾焰（FLAME 数量/大小）；
4. 近距离是否会太糊/掉帧（粒子数量嫌多就减 samples 或 count）；
5. 垂直向上发射时尾迹是否仍是两条而不是叠成一条。
## 52. 阿帕奇 M18：第三人称隐藏乘员模型 + 补上火箭准心

反馈：第三人称会看到玩家模型（要去掉），以及「火箭特有的准心没有正常显示」。

### 52.1 证据（截图取证 + 代码）

1. 把截图中心 400x400 区域裁出来放大看（NearestNeighbor 放大 2 倍）后可以确认：**中心十字其实是画出来的**，
   但玩家模型（蓝色身体）正站在座舱里、正好压住准心 —— 这就是「准心没正常显示」的直接原因之一，
   同时也是独立的一条真 bug：阿帕奇模型没有座舱内构，乘员被画在机身里，头和肩膀穿出座舱。
2. 另一条原因在代码里：小鸟有**火箭专用准心**（LittleBirdHud.drawRocketReticle：两条括号 + 每侧弹巢点阵 + 白色圆环落在预测弹着点），
   而阿帕奇此前**一个火箭准心都没有**：M16 只给它加了固定中心十字，炮塔准星又只给副驾驶。

### 52.2 改动

| 文件 | 内容 |
| --- | --- |
| client/ApacheRiderRender.java（新） | RenderPlayerEvent.Pre：骑在阿帕奇上的玩家**不画模型**（内外视角都不画），彻底消除「乘员站在机身里穿模/挡住准心」 |
| client/GunshipHudDraw.java | 火箭准心绘制上移为共享代码：rocketReticle（括号 + 弹巢点阵 + 白环，保持原绘制顺序）与 rocketMarker（只要括号 + 白环），两机共用一套瞄准具 |
| client/LittleBirdHud.java | drawRocketReticle 改为委托共享绘制，删除随之失效的 3 个私有绘制方法与 9 个常量（**不外迁任何行为**，像素应与之前完全一致） |
| client/ApacheAimRenderer.java | 新增火箭弹着点预测：用与**服务端发射完全相同**的发射点（下一发来自哪个挂巢、挂巢横向偏移、挂巢高度/前伸）与同一套 LittleBirdRocketBallistics 逐 tick 推进 + raycast，再投影/钳位/平滑；每游戏 tick 只算一次 |
| client/ApacheHud.java | 主驾驶在选中火箭巢时绘制火箭准心（落点标记）；陶氏不需要——它沿机头飞，中心十字就是它的瞄准线 |
| entity/weapon/ApachePods.java | 新增 launchLateralBlocks(ready, lateral)：把「下一发来自哪个挂巢」的横向偏移收敛成一处，服务端发射与客户端预测共用 |
| entity/ApacheEntity.java | launchRocket 改用 ApachePods.launchLateralBlocks（行为等价，去掉重复的 pod/side 判断） |
| 测试 | ApachePodsTest +1：两侧交替、偏移恒等于一个挂巢宽度、且与服务端 nextPod 一致 |

### 52.3 自审说明

1. **火箭准心为什么会「不准」**：它不是固定贴图，而是落在**预测弹着点**上，所以预测用的发射点必须和真火箭一模一样。
   因此我把挂巢选择/横向偏移抽成 ApachePods.launchLateralBlocks 并让服务端发射与客户端预测都调它，再加单测钉住；
   否则两边各写一份，准心会静默偏出半个挂巢宽度 —— 这是这类「投影式准心」最常见的坏法。
2. **小鸟像素回归**：共享绘制是**搬家**而不是重写：绘制顺序（括号 → 左点阵 → 右点阵 → 白环）、常量值、循环都照抄。
   常量名改了（RETICLE_BAR_* → ROCKET_BAR_*），值未动。仍需你在游戏里对一次小鸟的火箭准心。
3. **阿帕奇的点阵**：第一版没画点阵（担心每侧 19 管硬套会糊）。确认机体实际分配是**左 10 管、右 9 管**后补上，
   见 52.5；点阵同时就是弹药读数，和右下角面板的数字同源（都来自 ApachePods）。
4. **隐藏乘员的副作用**：名字标签由外层 EntityRenderer#render 绘制，不在 PlayerRenderer#render（事件所在处）内，
   因此名字应当还在；这一点我只能从 Forge 的事件位置推断，实测若名字标签一起消失告诉我，我再单独处理。
   另外只对阿帕奇生效 —— 小鸟的乘员一直能看到，且它的肩扛视角依赖乘员位置，不动它。
5. **未解决**：上一轮遗留的「玩家模型从机身侧面穿出」现在直接通过不画模型解决；
   如果之后想要「看得见的乘员」，需要给模型补座舱内构与乘员坐姿适配，那是另一个量级的活。

### 52.4 验证

- **211 个单测全绿**（210 → 211：挂巢发侧一致性）；
- gradlew.bat build 成功并自动同步；JAR **294786 字节**，SHA-256 **F3C0BBD4...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**：

1. 第三人称：是否还看得到玩家模型（应完全看不到），准心是否不再被挡住；
2. 顺带确认名字标签是否仍在（若消失见 52.3 第 4 点）；
3. 主驾驶选火箭巢：是否出现「括号 + 白环」落在预测弹着点上，打远处的方块看落点与白环是否吻合（不吻合说明发射点仍与真火箭有偏差）；
4. 切到陶氏：火箭准心应消失，只剩中心十字（陶氏沿机头飞）；
5. 副驾驶：炮塔准星仍在，且不再被自己的模型挡住；
6. 小鸟的火箭准心是否与之前完全一致（纯搬家，但必须目视确认）。
### 52.5 追加：阿帕奇弹巢点阵（左 10 / 右 9）

机体实际分配是左侧弹巢 10 管、右侧 9 管（服务端 magazine 交替发射导致的自然分割，也是 ApachePods.podCapacity 的定义），
因此照小鸟的参考图样给阿帕奇也画点阵，点阵数量就是弹药读数。

| 文件 | 内容 |
| --- | --- |
| entity/weapon/ApachePods.java | 新增点阵布局（纯规则）：HUD_COLUMNS=2 / HUD_ROWS=5、tubeCount(pod)（10 / 9）、tubeLoaded(pod, tube, ready)、tubeDot(pod, tube)；点阵**从左到右、从上到下清空**，所以最先发射的 0 号管占最后熄灭的那个点 |
| client/GunshipHudDraw.java | 新增 apacheRocketReticle（括号 + 两侧点阵 + 白环）：10 管的那侧填满 2x5 网格，9 管的那侧保留参考图样里内列顶格的空缺 —— 那个空缺正好就是它少的那一发；删掉上一轮临时用的 rocketMarker（不再需要，不留死代码） |
| client/ApacheHud.java | 主驾驶的火箭准心改调 apacheRocketReticle，并把同步过来的 ready 弹药数喂进去 |
| 测试 | ApachePodsTest +3：左侧 10 点右侧 9 点且每管位置唯一、**点亮数恒等于该弹巢剩余弹数**（否则点阵会说谎）、最先发射的管子占最后熄灭的点（两侧都收敛到右下角内格）+ 越界抛异常 |

自审：这次没有沿用「小鸟 7 点、阿帕奇 19 点」的直觉去铺满 19 个点，因为 HUD 上真正的语义是**这一侧弹巢当前还能打几发**（10 / 9），
而不是模型上有几根管子；铺 19 个点会与右下角面板的数字打架。间距沿用小鸟的 5 px，行数从 4 提到 5，点阵整体以准心垂直居中。

验证更新：**214 个单测全绿**（211 → 214）；JAR **296682 字节**，SHA-256 **E66922BA...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

游戏内追加确认：主驾驶选火箭巢后，两侧是否各出现 10 / 9 个点、开火后是否从左上往右下逐个熄灭、打空一侧后该侧是否全灭。
## 53. 阿帕奇 M19：陶氏直击即摧毁 + 初速与极速大幅减慢

需求：陶氏加直击判定（一发击毁所有旋翼机），并且从初速到极速都大幅减慢。

### 53.1 现状与证据

| 项 | 现状 |
| --- | --- |
| 直击伤害 | ApacheMissileEntity 已有扫掠命中判定（findEntityHit），命中后按配置造成 60 直伤 / **20 对机体伤害** —— 阿帕奇 120 血要 6 发、小鸟 80 血要 4 发，命中一发几乎没有意义 |
| 速度 | INITIAL_SPEED 1.20、CRUISE_SPEED 3.00 格/tick（约 60 格/秒），加上 140 tick 寿命与 220 格射程，实际是「一眨眼就到」的准瞬时武器；尾迹也因此几乎没时间铺开 |

### 53.2 改动

| 文件 | 内容 |
| --- | --- |
| entity/weapon/ApacheMissileBallistics.java | INITIAL_SPEED 1.20 → **0.60**、CRUISE_SPEED 3.00 → **1.40**（初速与极速都约减半）；LIFETIME_TICKS 140 → **170**；新增纯规则 directHitDamage(剩余血量) = 血量 + 1，保证一发清空 |
| entity/ApacheMissileEntity.java | 命中伤害改走 damageFor：目标是旋翼机（阿帕奇 / 小鸟）且开关打开时用 directHitDamage，其余仍用原版弹头伤害；目标照旧走 hurt → 既有坠毁 / 弹射 / 爆炸流程，没有新写一套死亡逻辑 |
| LittleBirdConfig.java | 新增 COMMON 开关 missile_direct_hit_kills（默认 true） |
| 测试 | ApacheMissileBallisticsTest +3：速度确实减半且初速 < 极速、转弯半径随之变小；**寿命内可飞距离 ≥ 射程上限**；直击伤害必定大于目标剩余血量（120 / 80 / 1 / 0 血都验） |

### 53.3 自审说明

1. **减速会顺手把射程常量变成摆设**：这是我自己算出来的坑 —— 按新速度，140 tick 只能飞约 185 格，而 MAX_RANGE_BLOCKS 是 220，
   等于寿命先到期、射程上限永远用不上。因此把寿命提到 170 tick（8.5 秒，寿命内可飞约 233 格），并补了一条**把两者绑在一起**的单测，
   以后谁再改速度都会立刻被这条测试拦下。
2. **为什么直击规则要单独一条而不是直接调大配置伤害**：调大 airframe 伤害会同时影响溅射与其它弹种，
   而「直击即摧毁」是**命中方式**的规则。做成 directHitDamage(剩余血量) 之后它与血量无关地成立（满血阿帕奇 120、满血小鸟 80 都一发），
   并且是纯函数、可单测。
3. **减速的副作用（已知，未额外处理）**：转弯半径 = 速度 / 角速度，从约 24.6 格降到约 11.5 格，也就是导弹**更灵活**了；
   同时每 tick 扫掠段更短，反而**更不容易穿模漏判**。这两条都是减速的自然结果，如果你觉得太灵活，先调 MAX_TURN_DEGREES_PER_TICK。
4. **范围界定**：直击即摧毁只对旋翼机（阿帕奇 / 小鸟）生效；打玩家与生物仍是普通弹头伤害（默认 60）。
5. **留了开关**：missile_direct_hit_kills 默认 true，2v2 里觉得过强可以关掉，关掉后回到 20 对机体伤害的老行为。

### 53.4 验证

- **217 个单测全绿**（214 → 217）；
- gradlew.bat build 成功并自动同步；JAR **297180 字节**，SHA-256 **DC741D0F...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**：

1. 一发陶氏命中敌机是否立刻摧毁（机体爆炸 + 乘员弹射），不再需要 4-6 发；
2. 飞行手感：从发射到命中的时间是否明显变长、双马尾尾迹是否更连贯（速度减半后每段更密）；
3. 是否**太灵活**（转弯半径约减半）：如果像制导炸弹一样随意拐弯，我再降 MAX_TURN_DEGREES_PER_TICK；
4. 打玩家 / 生物仍是普通伤害（不秒杀）；
5. 射程：满寿命 8.5 秒能否打到约 200 格外的目标（寿命内可飞约 233 格）。
## 54. M20：部署时下沉一格

反馈：刚放置时会往下沉一格。

### 54.1 证据（可复现的算术）

两个部署物品都是这样算位置的：

```
Vec3 spawn = player.position().add(player.getLookAngle().scale(SPAWN_DISTANCE));
```

也就是说**投放高度直接由玩家的俯仰角决定**，完全不查地面。代入数值：

| 视线俯角 | 落点相对玩家脚底的高度（4 格距离） |
| --- | --- |
| 0° | 0.00 格 |
| −5° | −0.35 格 |
| **−15°** | **−1.04 格** |
| −30° | −2.00 格 |
| −45° | −2.83 格 |

放置载具时人自然会看前方的地面，所以典型情况就是 −10°~−20° → 落点在脚底下方约一格 → 机体**生成在地面方块里**。
而 Entity#move 只会取消运动、不会把实体从它生成时所在的方块里顶出来，于是它就一直陷在那里 —— 这正是「下沉一格」且不会自己浮起来的原因。

第二条相关的：ApacheEntity.applyUnpilotedMotion 在无人驾驶时**每 tick 都继续施加重力**（没有 onGround 归零），而小鸟早就写了「落地即归零」，两边行为不一致。

### 54.2 改动

| 文件 | 内容 |
| --- | --- |
| item/GunshipPlacement.java（新，纯规则） | 规则本身：**朝哪儿决定落在哪儿，地面决定落多高**；探测范围 PROBE_ABOVE=1（高于脚底一点，能发现面前一格台阶、又不会先撞到天花板）、PROBE_BELOW=8；baseY(是否命中, 地面高度, 瞄准高度) |
| item/GunshipPlacementProbe.java（新） | 向下射线探测（ClipContext.Block.COLLIDER）取该列地面高度；横向偏移改用**偏航方向**而不是完整视线向量 |
| item/ApacheDeployItem.java / LittleBirdDeployItem.java | 位置改由探测给出（声音播放点也跟着走） |
| entity/ApacheEntity.java | 无人且已落地时归零运动并 return，与小鸟的 applyUnpilotedFall 行为一致，停着的机体不会顺着坡度往下蹭 |
| 测试 | GunshipPlacementTest（新，2 条）：命中地面时地面高度**优先于**瞄准高度（含向上瞄准的情况）、未命中时保留玩家高度；探测上下界与方向 |

### 54.3 自审说明

1. **自己抓到的第二个坑**：只把高度交给探测还不够 —— 如果仍用完整视线向量算横向偏移，玩家**垂直向下看**时横向偏移为 0，飞机会生成在自己脚下/身上。
   因此横向改由偏航方向算（`Vec3.directionFromRotation(0, yRot)`），投放距离就恒定是 3 / 4 格，和视线俯仰无关。
2. **探测起点为什么是脚底 +1 而不是更高**：起点更高时，室内/树下会先打到天花板，把飞机放到屋顶上；脚底 +1 则高于一格台阶、低于任何正常天花板。
3. **未命中怎么办**：悬崖边、桥面、船上等探不到地面的情况，保留玩家所在高度，飞机自然落体 —— 这是合理行为，不是遗漏。
4. **停泊行为与小鸟对齐**：小鸟原来就是 `onGround → deltaMovement = ZERO`，这次只是让阿帕奇照做，属于修不一致，不是新规则。
5. **顺带核对过 M16 的加长没有破坏贴地线**：SKID_BOTTOM_UNITS 仍为 0、机轮盒子仍从 y=0 开始，所以实体底部正好落在方块顶面。

### 54.4 验证

- **219 个单测全绿**（217 → 219）；
- gradlew.bat build 成功并自动同步；JAR **299109 字节**，SHA-256 **C1CF1C41...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**（射线行为只能在游戏里确认）：

1. 平地放置：机体是否**正好站在地面**、不再陷入一格；
2. 面前一格台阶：机体是否站在台阶顶上而不是嵌进去；
3. 面前是落差：机体是否落到下一层地面（而不是悬空卡住）；
4. **垂直向下看**放置：机体是否出现在正前方地面而不是自己脚下；
5. 悬崖边/水面上放置：是否按预期落体。
## 55. 崩溃修复：两架机体互相引爆导致 StackOverflowError

### 55.1 证据（崩溃日志）

日志里是同一段调用栈无限重复，最后栈溢出：

```
io.github.littlebird.entity.ApacheEntity.destroyAircraft(ApacheEntity.java:877)
io.github.littlebird.entity.ApacheEntity.hurt(ApacheEntity.java:870)
net.minecraft.world.level.Explosion.finalizeExplosion(Explosion.java:199)
net.minecraft.server.level.ServerLevel.explode(ServerLevel.java:1009)
io.github.littlebird.entity.ApacheEntity.destroyAircraft(ApacheEntity.java:877)
... 无限重复 ...
```

机理：

1. 一架机体被打到血量 ≤ 0 → destroyAircraft → `server.explode(this, ...)`（以自身为 source，因此原版不会伤到自己）；
2. 但**爆炸会伤到旁边那架机体** → 那架也 ≤ 0 → 它 destroyAircraft → 又一次爆炸；
3. 第二次爆炸回过头来伤到**第一架**：它此时血量早已 ≤ 0，但 hurt 里**没有任何「已经死了」的判断**，于是又走一遍 destroyAircraft → 无限乒乓，直到栈溢出（崩溃报告里的实体上下文是一架载着玩家的小鸟，也印证了是两机互炸）。

顺带说明：这个缺陷早就存在，只是以前靠机炮（1 伤/发）与火箭（10 伤/机体）很难真的打死人；M19 把陶氏改成**直击即摧毁**之后，爆炸式死亡变成常态，于是必然被踩到。

### 55.2 改动

| 文件 | 内容 |
| --- | --- |
| entity/AircraftDestruction.java（新，纯规则） | 一次性写毁闸门：beginDestruction() 只在第一次返回 true，acceptsDamage() 在写毁后恒为 false |
| entity/ApacheEntity.java / LittleBirdEntity.java | hurt 增加 `|| !destruction.acceptsDamage()`；destroyAircraft 以 beginDestruction 开头（写毁只跑一次）；**写毁时先把乘员抛出** |
| entity/LittleBirdEntity.java | applyCrash 改为直接委托 destroyAircraft（弹出乘员这一段收敛成一处，不再两处各写一遍） |
| 测试 | AircraftDestructionTest（新，3 条）：写毁只跑一次、写毁后不再吃伤害、以及「两架互相引爆」的循环最多产生 2 次爆炸 |

### 55.3 自审说明

1. **为什么用闸门而不是「爆炸不伤机体」**：爆炸伤到邻居是想要的行为（坠毁会波及旁边），陶氏直击规则也依赖走 hurt；所以要在**写毁这个动作**上加一次性约束，而不是削弱伤害模型。
2. **顺带修掉第二个真实缺陷**：javap 反编译 Entity#remove 只有 `setRemoved + invalidateCaps`，**不会**把乘客放下来。也就是说被打爆的机体上的玩家会继续「骑」在一个已经被移除的实体上。现在 destroyAircraft 会先 ejectCrew（stopRiding + resetFallDistance + 20 tick 无敌 + 一点抛出速度），与坠毁路径行为一致；崩溃日志里正好有一位玩家骑在小鸟上，这正是会被这个缺陷咬到的场景。
3. **重复弹出是无害的**：applyCrash 原本自己弹一次、destroyAircraft 现在也弹一次；第一遍之后 getPassengers() 已空，第二遍是空循环。为了不留两份重复代码，applyCrash 已经改为只调 destroyAircraft。
4. **闸门不入 NBT**：写毁同一 tick 就 discard，没有「重载成一架已写毁的飞机」这种状态，持久化反而是多余的。
5. **测试只钉住形状**：MC 的爆炸接线没法单测，所以测试用两个闸门互相「打」来复现崩溃的**拓扑**（每个最多爆一次），真正的验证还是在游戏里打爆一对飞机。

### 55.4 验证

- **222 个单测全绿**（219 → 222）；
- gradlew.bat build 成功并自动同步；JAR **300090 字节**，SHA-256 **F23823ED...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**（崩溃只能在游戏里复现）：

1. 两架飞机靠近时打爆其中一架：不应再崩，两架各爆一次；
2. 被打爆时乘员是否被抛出（不再粘在残骸上）、能否立刻再上另一架；
3. 陶氏一发直击摧毁时同样不崩；
4. 坠毁（俯冲撞地）路径是否仍与以前一致（现在也走同一个写毁函数）。
## 56. 生存打造 M22：零件链 + 配方（datagen）

按 survival-crafting.md 第 8 节已拍板的决策实施第一片：材料链与配方。**弹药箱、回收、修复、成就留到 M23-M25**。

### 56.1 新增与改动

| 文件 | 内容 |
| --- | --- |
| registry/ModItems.java | 新增 9 个零件物品：airframe_section、rotor_assembly、aircraft_engine、avionics_unit、canopy_glass、weapon_pylon、targeting_computer、apache_airframe、apache_weapons |
| registry/ModCreativeTabs.java | 创造栏列出 9 个零件（方便调试） |
| data/ModDataGenerators.java（新） | GatherDataEvent 挂上配方与模型两个生成器 |
| data/ModRecipeProvider.java（新） | 11 条配方（7 零件 + 2 中间件 + 2 整机）+ 解锁条件；阿帕奇的门控按决策「不卡」，瞄准计算机只用金/红石/钻石，配方里没有末影珍珠 |
| data/ModItemModelProvider.java（新） | 9 个零件的物品模型（item/generated + layer0） |
| tools/generate_component_textures.ps1（新） | 9 个 16x16 图标，程序化生成（与既有两个图标同风格） |
| 本地化 | 9 个物品名 × 2 语言 |
| build.gradle | processResources 排除 datagen 的 .cache（否则会混进 JAR） |

### 56.2 材料与成本（生成物已核对）

| 配方 | 形状 | 材料 |
| --- | --- | --- |
| 机体框架 | IBI / B B / IBI | 4 铁锭 + 4 铁栏杆 |
| 旋翼总成 | III / B B / .I. | 4 铁锭 + 2 铁栏杆 |
| 航空发动机 | IFI / CRC / III | 5 铁锭 + 熔炉 + 2 铜 + 红石 |
| 航电组件 | CRC / SGS / .R. | 2 铜 + 红石×2 + 玻璃×2 + 金锭 |
| 座舱玻璃 | SSS / B B / SSS | 6 玻璃 + 2 铁栏杆 |
| 武器挂架 | I.I / IBI / .I. | 4 铁锭 + 铁栏杆 |
| 瞄准计算机 | GRG / RDR / GRG | 4 金锭 + 4 红石 + 钻石 |
| 阿帕奇机身 | 无序 | 4 框架 + 2 旋翼 + 2 发动机 + 1 航电 |
| 阿帕奇武器系统 | 无序 | 2 挂架 + 1 瞄准计算机 + 2 铁块 + 4 铜锭 |
| 小鸟 | ARA / .E. / ACV | 3 框架 + 旋翼 + 发动机 + 航电 + 座舱 |
| 阿帕奇 | A.. / .W. / .C. | 机身 + 武器系统 + 座舱 |

### 56.3 自审说明

1. **datagen 真的跑通了**（这是本片最大的不确定项）：runData 生成了 11 个配方 + 11 个配方解锁进度 + 9 个物品模型，全部落在 src/generated/resources；
   生成物内容已核对（例如 little_bird.json 的 pattern 与 key 正确）。原计划里「先试 datagen，不行就退回手写 JSON」的退路没有用上。
2. **自审抓到的一个打包污染**：datagen 会在输出目录写 .cache 哈希缓存，而该目录同时是资源目录，等于会把缓存塞进 JAR。已在 processResources 里排除，并在产物里核对 CACHE_ENTRIES=0。
3. **PowerShell 的坑**：图标脚本第一版把绘制辅助函数命名为 R，被 PowerShell 内置别名 r（Invoke-History）抢先解析，导致九个图标全是空图；改名 FillBox/NewBrush 后重新生成并逐个目视确认。脚本里留了注释说明这个坑。
4. **门控按「不卡」执行**：阿帕奇依然比小鸟重 2 倍以上（含钻石），但不强制末影珍珠，所以不需要末地进度。
5. **本片没有动物品逻辑**：零件目前只是材料，没有右键功能；弹药箱、回收、修复、坠毁掉落、成就都还没做。

### 56.4 验证

- **222 个单测全绿**（本片只加物品与数据，没有改逻辑）；
- gradlew.bat runData 成功；runData 产物：11 配方 + 11 解锁进度 + 9 物品模型；
- gradlew.bat build 成功并自动同步；JAR **321453 字节**，SHA-256 **A675364D...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR；
- 产物内核对：recipes=12（含目录项）、advancements=14、item models=12、item textures=12、cache 条目=0。

**必须游戏内验证**：

1. 打开配方书：拿到铁锭后是否逐步出现零件、再出现整机（解锁顺序）；
2. 逐个合成 9 个零件与两架整机，图标是否正常、名称是否正确（中英）；
3. 用生存模式实际凑一次小鸟的材料，确认难度手感；
4. 阿帕奇的合成是否需要跨过「机身 + 武器系统」两步。
## 57. 生存打造 M23a：弹药基础件（纯规则 + 配置）

按已确认的弹药决策，先把**纯规则与配置**落地；实体与弹药箱的接线（M23b）紧随其后。
这一片**没有改变任何现有行为**：自动回弹还在，弹药箱还没接线，所以不会出现「补给没了、箱子还没用」的空窗。

### 57.1 新增与改动

| 文件 | 内容 |
| --- | --- |
| entity/weapon/AmmoPool.java（新，纯规则） | 单池弹药（机枪弹链 / 链炮弹仓）：容量、开火、补充只到容量、0..1 读数；**不含自动补弹** |
| entity/weapon/AmmoSupply.java（新，纯规则） | 弹药箱补给速率：每秒多少发子弹 / 火箭 / 导弹，以及补给半径 6 格；整数换算保证「每秒 40 发」就是每秒 40 发，而不是每 5 tick 一次卡顿 |
| LittleBirdConfig.java | 新增 [ammo_crate] 三个速率键 + ammoSupply() 访问器（子弹 40/s、火箭 2/s、导弹 1/s） |
| 测试 | AmmoPoolTest（新，5 条）：满弹、逐发打空后拒绝射击、补充只到容量并返回实际补充量、多发改射要么全扣要么不扣、弹药箱速率随时间线性 |

### 57.2 已确认的容量表（M23b 落地）

| 载具 | 武器 | 容量 | 形式 |
| --- | --- | --- | --- |
| 小鸟 | 7.62 机枪 | 2000 | 单池 |
| 小鸟 | 70mm 火箭 | **待发 7（总）/ 备弹 42** | 两段弹仓；两挂巢按 4/3 分摊 7 发 |
| 阿帕奇 | 19 管火箭巢 | 19 / 95 | 两段弹仓 |
| 阿帕奇 | 陶氏 | 1 / 5 | 两段弹仓 |
| 阿帕奇 | 30mm 链炮 | 600 | 单池 |

### 57.3 M23b 改动清单（下一步，一次做完）

1. 小鸟：DATA_ROCKETS_LEFT/RIGHT 的初始与补给改为「待发 7（4/3 分摊）+ 备弹 42」，删掉 rocket_rearm 计时器，接上 R 键装填；
2. 小鸟 2000 发机枪弹链、阿帕奇 600 发链炮弹仓：新增同步量、开火扣弹、打空不能开火、HUD 显示；
3. 阿帕奇：删掉 TwoStageMagazine 的自动补给计时（火箭 15s / 陶氏 10s），备用弹仓上限改为 95；
4. 新增弹药箱方块 battlefield:ammo_crate（可合成、有模型与贴图、datagen 生成配方）：机体停在 6 格内按配置速率补弹；
5. HUD：两机面板补齐弹药读数，补给中给出反馈。

### 57.4 验证

- **227 个单测全绿**（222 → 227）；
- gradlew.bat build 成功并自动同步；JAR **323649 字节**，SHA-256 **B4A23C46...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。
## 58. 生存打造 M23b + M24 + M25：弹药箱、回收修复、成就

按已确认的决策一次做完剩下的三片。

### 58.1 弹药系统（M23b）

| 载具 | 武器 | 容量 | 实现 |
| --- | --- | --- | --- |
| 小鸟 | 7.62 机枪 | 2000 | 新增同步量 DATA_GUN_AMMO，开火扣一发，打空不能开火 |
| 小鸟 | 70mm 火箭 | **待发 7（总）/ 备弹 42** | RocketPodState 新增 READY_CAPACITY=7、RESERVE_CAPACITY=42、splitReady（7 → 左 4 右 3）；R 键装填 |
| 阿帕奇 | 19 管火箭巢 | 19 / 95 | TwoStageMagazine.rocketBay 备用弹 95 |
| 阿帕奇 | 陶氏 | 1 / 5 | 不变 |
| 阿帕奇 | 30mm 链炮 | 600 | 新增同步量 DATA_CHAIN_AMMO，开火扣一发 |

- **取消一切自动补给**：小鸟的 rocket_rearm 计时器删除；阿帕奇的弹仓补给时钟置 0（TwoStageMagazine 的 supply 取代了它）；
- **新增弹药箱方块** battlefield:ammo_crate（8 铁 + 4 火药 + 1 箱子，datagen 生成配方；方块状态/模型/16x16 贴图齐备）：
  机体停在 **6 格**内时，每秒补：子弹 40 发、火箭 2 发、导弹 1 发（[ammo_crate] 可调）；
- HUD：小鸟面板显示真实弹链数（原来是 ∞）与 待发/备弹；阿帕奇副驾面板显示链炮余弹；
- R 键现在对两机都生效（ApacheReloadPacket 增加了小鸟分支，LittleBirdInput 接上按键）。

### 58.2 回收 · 修复 · 坠毁（M24）

- **打包**：潜行 + 空手右键（机上没人时）= 收回物品，并把**当前血量写进物品 NBT**；部署物品生成机体时把血量读回去；
- **修复**：无序配方（小鸟 + 3 铁锭、阿帕奇 + 5 铁锭）→ 满血机体；
- **坠毁掉落**：写毁时掉少量铁锭（小鸟 2-4、阿帕奇 3-6），**在爆炸之后**生成 —— 先掉的话会被自己的爆炸炸掉；
- 打包用的是 discard，不爆炸：主动收起来不该有惩罚。

### 58.3 成就（M25）

两个自定义进度：轻型航空（造出第一架小鸟）→ 武装航空（造出阿帕奇，frame=challenge），中英双语。

### 58.4 自审说明（含已知欠账）

1. **两处 datagen 的坑**：一是 runData 会被 Gradle 判定 UP-TO-DATE 而根本不跑（要 --rerun-tasks）；二是生成器**没有输出**「同名物品进、同名物品出」的无序配方，两条修复配方因此静默缺失。
   已把这两条改成手写 JSON（放 src/main/resources/data/battlefield/recipes/），并在 provider 里留了注释说明原因 —— 一条不存在的配方比一条手写的配方更糟。
2. **已知欠账**：旧的回弹配置键（小鸟 rocket_rearm_seconds / rocket_rearm_batch，阿帕奇 rocket_resupply_seconds / missile_resupply_seconds）现在已经没有代码读取，属于死配置，下一轮清理。
3. **修复配方的边界**：原版配方无法判断 NBT，所以「满血机体 + 铁锭」也会被匹配（浪费铁锭，不会损坏东西）；要精确只能用自定义配方类型，超出本片范围。
4. **两个设计测试文件被重写**：旧测试锁的是老数值（14 发待发、19 备弹、自动补给），新测试锁新数值（7 发总待发 4/3 分摊、42/95 备弹、无人自动补给、弹药箱只补到上限）。这不是「改测试让构建变绿」——旧的断言按新需求就是不成立的。

### 58.5 验证

- **223 个单测全绿**（227 -> 223：两个文件重写后总数变化）；
- gradlew.bat runData --rerun-tasks 与 build 均成功并自动同步；JAR **337090 字节**，SHA-256 **063AB885...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR；
- 产物内核对：recipes=14（11 datagen + 弹药箱 + 2 手写修复）、advancements=16（含 2 个自定义）、方块状态/模型/贴图各 1、物品模型 12、物品贴图 11。

**必须游戏内验证**：

1. 合成弹药箱、放在基地，把打空的飞机停旁边，看每秒补弹（子弹/火箭/导弹三种速率）；
2. 小鸟打空 7 发待发后按 R 是否把 42 发备弹搬上来，机枪 2000 发是否真的会打完；
3. 潜行空手右键是否把飞机收回成物品、放下后血量是否保持；
4. 背包里「残血机 + 铁锭」是否修满；
5. 打爆一架是否掉 2-6 个铁锭、且铁锭不会立刻被爆炸清掉；
6. 两个成就的提示与进度链；
7. 弹药箱的方块外观与贴图。
## 59. 紧急修复：命名空间/显示名全变成原始键（lang 文件损坏）

反馈：JEI 与创造物品栏里所有名称显示成一串英文（其实是原始翻译键 item.battlefield.airframe_section 这种）。

### 59.1 证据

用 ConvertFrom-Json 直接校验两个语言文件：

| 文件 | 结果 |
| --- | --- |
| en_us.json | **BROKEN**：Invalid object passed in, ':' or '}' expected —— 我的追加编辑在 advancements 段最后一项后面**漏了逗号**，紧接着就是零件条目 |
| zh_cn.json | **BROKEN 且中文全毁**：内容变成 mojibake（小鸟直升机 -> 灏忛笩鐩村崌鏈?），连字符串的收尾引号都被吞掉，JSON 在第 53 字符处就断了 |

一个语言文件解析失败，Minecraft 就会把该语言的**所有**键都当原始键显示 —— 所以是「全炸」而不是「新加的那几条炸」。

### 59.2 修复

1. 两个文件**整体重写**（不再用「按行追加」的方式改 JSON）：en_us 51 个键、zh_cn 51 个键，键集合完全一致；
2. 中文文件不再走普通写入路径，而是把 UTF-8 内容以 base64 传给 PowerShell、用 **[System.IO.File]::WriteAllText + UTF8Encoding(false)** 落盘（无 BOM），规避编码往返；
3. 落盘后立刻校验：两个文件 ConvertFrom-Json 通过，且检查「弹药箱」首字符码点 = 24377（U+5F39 弹）—— 证明中文真的还在，而不是控制台显示恰好正常；
4. **新增 LangFileTest（4 条）**：两个文件能解析、键集合一致、代码用到的键都存在且非空、中文文件的首字符确实是汉字（CJK）—— 这类事故以后会在 build 阶段就红掉。

### 59.3 自审

1. **根因不是编码而是流程**：真正致命的是 en_us 的**漏逗号**（纯 ASCII，与编码无关），中文那份的编码损坏是同一批「读一行 → 拼接 → 写回」操作的第二个副作用。
   结论：对 JSON 这类有语法约束的文件不做逐行追加，改完必须解析一次；现在已经由测试强制。
2. **只验证源码树还不够**：这次额外把**成品 JAR 里的** zh_cn.json 解出来解析、并读出「弹药箱」，确认打包后的资源也是好的（ZIP_ZH_PARSES=True）。
3. **教训记录**：consoles 会把 UTF-8 当 GBK 打印，所以「肉眼看控制台」无法判断中文是否损坏 —— 必须用码点/解析来验证，这也是为什么这条测试要断言「首字符是汉字」。

### 59.4 验证

- **227 个单测全绿**（223 -> 227：新增 4 条语言文件测试）；
- gradlew.bat build 成功并自动同步；JAR **337066 字节**，SHA-256 **AFDB2C38...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR；
- 成品 JAR 内 zh_cn.json 可解析，block.battlefield.ammo_crate = 弹药箱。

**请游戏内确认**：JEI 与创造栏里两架飞机、9 个零件、弹药箱、以及中英两种语言的名称与成就名是否都正常。
## 60. 小鸟加三个副驾驶座（只观光，不操作）

需求：小鸟再加三个副驾驶座；三个副驾驶**只有第三人称视野**、**不能进行任何操作**（只能下飞机）。

### 60.1 改动

| 文件 | 内容 |
| --- | --- |
| entity/seat/LittleBirdSeat.java（新，纯规则） | 座位表：SEATS=4，0 号是飞行员；副驾 1/2 是左右长凳（±0.42 格）、3 号是舱尾中央；越界座位钳位而不是抛异常 |
| entity/LittleBirdEntity.java | canAddPassenger 从「只能一人」改为「不足 4 人且是玩家」；新增 seatOf / isPilot；positionRider 按座位表偏移（用偏航把前后/左右分解到机体自身轴向）；座舱相机与肩扛相机**只对飞行员生效** |
| client/LittleBirdInput.java | 非飞行员：不发送任何操控（setClientControls 归零）、不发送武器/装填/切换包、座舱视角键失效、**每 tick 强制 THIRD_PERSON_BACK**（F5 也会被拉回）；EXIT 键（下飞机）保留给所有人 |
| client/LittleBirdAimRenderer.java | 非飞行员直接 clear：不投影任何准心 |
| client/LittleBirdHud.java | 副驾只保留中心飞行簇（速度/高度/航向/坡度）；武器面板与准心只对飞行员绘制 |
| 测试 | LittleBirdSeatTest（新，4 条）：四个座位且只有 0 号是飞行员、每个座位位置互不相同、飞行员在最前且两长凳左右镜像、偏移不超出座舱（越界钳位） |

### 60.2 自审说明

1. **服务端早就是安全的**：所有操控/武器/装填入口本来就写着 `getControllingPassenger() != sender` 就直接返回，所以副驾**即使改客户端也打不出子弹**；
   本片做的是客户端侧的一致体验（不发包、不显示、不给座舱视角），不是权限补丁。
2. **强制第三人称用每 tick 覆写而不是「忽略按键」**：否则副驾按 F5 仍能切到第一人称，那是视野上的漏洞。
   代价是副驾无法用第一人称观光 —— 这正是需求要的。
3. **飞行员判定是座位 0**：与阿帕奇一致，谁先上机谁是飞行员；副驾想开就得先下来让位。
4. **座舱相机与肩扛视角只对飞行员生效**：否则副驾会被机头视角/肩扛偏移一起拖走（那是给驾驶用的取景）。
5. **已知取舍**：小鸟机身很小，三个副驾的位置在模型上是「勉强坐下」的观感（左右长凳 ±0.42 格、舱尾 −0.70 格）；
   如果目视发现穿模/叠人，调 LittleBirdSeat 的三个数值即可（有测试兜底）。

### 60.3 验证

- **231 个单测全绿**（227 -> 231：新增 4 条座位表测试）；
- gradlew.bat build 成功并自动同步；JAR **338393 字节**，SHA-256 **FCD6CB3F...**，build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**：

1. 四人同时上机：第一个人是飞行员，后三个坐长凳/舱尾，位置是否合理、有没有叠在一起；
2. 副驾视角：是否被锁在第三人称（按 F5 会不会被拉回）、C 键是否无效；
3. 副驾按左键/1/2/R/Z 是否完全无反应（准心、武器面板也不该出现）；
4. 副驾按 EXIT 或潜行是否能正常下机；
5. 飞行员视角/操控/准心是否与以前完全一致。

## 61. 阿帕奇机炮：炮口指向改为只由瞄准决定（缺陷修复）

用户反馈：**阿帕奇的机炮不受瞄准修改方向**；确认现象为「机炮模型完全不转，恒指一个固定方向」，
场景是**单人用 V 切到前座**，并且前座准星会顶在屏幕边缘不动。

### 61.1 根因（只读诊断结论）

机炮的世界指向 = **机体姿态 + 炮塔局部角**，而局部角被硬钳在 ±86°／+11°／−60°
（旧 `ChainGunTurret.java:15-17,65-70`）。关键在于局部角是**相对机体**量的：

| 环节 | 旧实现 |
| --- | --- |
| 期望角 | `desiredAim` = `炮手yaw − 机体yaw`（旧 `:57-62`） |
| 世界指向 | `aimDirection` = `机体yaw + 局部方位`（旧 `:117-127`） |

单人切到前座后，`getControllingPassenger()` 恒返回后座 `pilot()` 而此时后座为空 → 返回 null，
客户端 `applyLocalFlight` 不再执行，**机体 yaw/pitch 冻结在切座那一刻**。于是
「机体固定 + 局部方位钳 ±86°」使炮口在世界里被锁死在一个固定扇区：

| 炮手 yaw（机体 0°） | 期望方位 | 钳后与稳态世界指向 |
| --- | --- | --- |
| 0 / 30 / 60 / 86 | 0 / 30 / 60 / 86 | 同步跟手 ✔ |
| 90 / 120 / 180 / 240 | 90 / 120 / −180 / −120 | **86 / 86 / −86 / −86，不再变化 ✘** |

即：链路没断，是炮塔压在自己的机械止档上，之后鼠标再怎么动方向都不再改变。

### 61.2 修法（用户拍板：炮口指向只由瞄准决定，与机体无关）

| 文件 | 改动 |
| --- | --- |
| entity/weapon/ChainGunTurret.java | `State` 的方位/仰角语义改为**炮口绝对世界角**（Minecraft yaw/pitch 约定）；`desiredAim` 只取炮手视线、**不再减机体**；`aimDirection` 只由瞄准角构造单位向量、**不再加机体**；删除 `clampAim`／`atLimit`／`MAX_AZIMUTH_DEG`，方位**全向 ±180°**（`approachAngle` 走最短弧，跨接缝不会绕一圈）；俯仰保留真机止档（pitch −11 上仰 / +60 俯冲，旧的符号是反的，已修）；新增 `parkedAim`（无人时炮口停在机头）与 `muzzleOrigin`（炮口挂在机体上、但指向跟瞄准） |
| entity/ApacheEntity.java | 同步量 `DATA_TURRET_AZIMUTH/ELEVATION` → `DATA_TURRET_AIM_YAW/AIM_PITCH`；`tickTurret` 只用瞄准角；`fireChainGun` 用 `aimDirection(绝对角)` + `muzzleOrigin`；新增 `getTurretAimYawDeg/AimPitchDeg/getTurretRelativeBearingDeg` |
| client/ApacheModelMath.java | 新增 `relativeBearing/relativeElevation/wrapSigned`：把绝对瞄准角换算成相对机体的**网格**摆角 |
| client/ApacheModel.java | 炮塔网格用「绝对瞄准角 + 机体姿态」现算局部角，所以机体怎么转，炮口世界指向都不变 |
| client/ApacheAimRenderer.java / ApacheHud.java | 准星投影改用绝对瞄准角；压限判定由「方位压限」改为「俯仰压限」；前座面板读数改为**相对机头的方位** |

### 61.3 自审抓到并修掉的问题

1. **俯仰止档符号接反**：`clampPitch` 把「上仰上限」写成 `-MAX_ELEVATION_UP_DEG` 之外还
   错误地放行了 +11…+12 区间，导致炮手看 -20° 被钳成 -11°。单测第一次跑就抓到（2 条失败），
   修正为 `clamp(pitch, -MAX_ELEVATION_UP_DEG, MAX_ELEVATION_DOWN_DEG)` 并补了边界断言。
2. **相对角必须走最短弧**：绝对角方案下机体转动只能改变相对摆角，若不 wrap 就会出现
   170° → −170° 绕 340° 的假回转，已在 `wrapSigned` 与 `approachAngle` 两处保证。
3. **几何复核（为什么全向不会把炮管甩进机身）**：炮塔枢轴 `z=−44, y=4`（单位）、
   机腹线 `y=8`、炮管 24 单位。炮口随方位扫过时 y 恒为枢轴高度（低于机腹 0.23 格），
   x 最大 1.36 格、z 在 −3.87…−1.14 之间，**整条弧都在机腹下方**，180° 时炮口仍在机头与机尾之间。

### 61.4 验证

- **238 个单测全绿**（231 → 238：ChainGunTurretTest 重写并 +5，ApacheModelMathTest +4，
  含「方位全向每一度都改变指向」「炮口随瞄准摆出去、随机体抬起来」两条回归）；
- gradlew.bat build 成功并自动同步；JAR **339171 字节**，SHA-256 **C62B8958...**，
  build/libs 与实例一致，实例仍只有一份 battlefield JAR。
- 自审追加修掉两处：`advance` 现在把持有的绝对 yaw 收回 ±180（最短弧跨接缝会让它无界增长，
  长时间绕圈后作为 float 同步会掉精度）；`muzzleOrigin` 的轴向量归一化加了零长度/NaN 保护
  （原来的直接 `normalize()` 在零向量上会产出 NaN）。

**必须游戏内验证**（代码层只能验证到规则与编译）：

1. 单人 V 切前座：鼠标转到侧后方（±90°…±180°）时下颌炮是否跟着转、准星是否回到屏幕内；
2. 机体 90° 滚转/大俯仰时，炮口是否仍停在准星上（这是本次改动的核心承诺）；
3. 模型炮塔扭到侧后方时是否穿模（几何算下来不该穿，但要目视）；
4. 抬头超过 11°、低头超过 60° 时俯仰止档是否符合预期（会有压限提示）；
5. 双人时主驾转弯，副驾瞄准点是否稳定不动。

## 62. 陶氏改为「世界空间瞄准点 + 到点引爆」（参考 final-paradox 陶氏）

用户要求参考另一个项目 `final-paradox-forge`（大地漫游器）里 `TerrastalkerRoverEntity` 的
陶氏实现。参考实现要点（行号见该文件）：`launchMissile:747`、`aimPoint:854`、
`tickMissiles:917`、`limitTurn:1009`。

| 参考设计点 | 是否采纳 |
| --- | --- |
| 沿视线射线取瞄准点，未命中取视线射程 | **采纳**（`aimPoint:854-864`） |
| 每 tick 重算瞄准点，跟着操作者视线走 | **采纳**（`tickMissiles:925-927`） |
| 抵达瞄准点（1.5 格）或敌机 1 格内引爆 | **采纳**（`:941-943`） |
| `limitTurn` 轴角旋转 | 不采纳：本工程的单位向量插值与它等价（速度上限 1.40/t，每 tick 转 7°） |
| 发射前 6 tick 不制导 | 本轮未做（可后续补，属手感项） |
| 沿途最多炸开 3 个方块 | **不采纳**：破坏地形属玩法改动，需单独拍板 |
| 尾迹用 CRIT/END_ROD/FLAME | 不采纳：本工程的双马尾烟 + 96 格逐玩家广播是此前按要求做的 |

### 62.1 改动

| 文件 | 改动 |
| --- | --- |
| entity/weapon/ApacheMissileBallistics.java | 新增 `SIGHT_RANGE_BLOCKS=64`、`REACH_RADIUS_BLOCKS=1.5`、`PROXIMITY_FUSE_BLOCKS=1.0`、`AimPoint` 记录、`aimPoint()`（深度钳到射程、NaN/零向量退化）、`reachedAimPoint()`；**删除 `MAX_RANGE_BLOCKS` 与 `outOfRange()`**（射程改由瞄准点决定） |
| entity/ApacheMissileEntity.java | 新增 `sightDistance`（发射时定深，NBT 持久化）；`tick` 每 tick 用同深度重算瞄准点并朝它制导，抵达即引爆；新增静态 `aimDepthBlocks(level, muzzle, sightLine, shooter)` 承担射线（唯一需要 Level 的部分）；**删除已无读取方的 `travelled` 死状态** |
| entity/ApacheEntity.java | `launchMissile` 发射时算一次瞄准深度并传给导弹 |
| 单测 | `ApacheMissileBallisticsTest`：删除射程上限两条，新增瞄点几何 4 条 + 到点引爆 1 条，寿命测试改为覆盖视线射程 |

### 62.2 自审说明

1. **为什么固定深度而不是每 tick 自由射线**：若每 tick 从导弹自身位置自由射线，
   导弹越靠近地面命中距离越短，瞄准点会朝导弹收缩，最终在导弹脚下引爆（自毁循环）。
   参考实现是从**乘员眼睛**射线，所以没有这个问题；本工程把深度固定在发射时刻
   （由炮口射线取得），瞄准点只随视线**平移**，既保留「瞄哪炸哪」又不会塌陷。
2. **射线的原点必须是操作者眼睛而不是导弹本体**（自审第二轮抓到）：即使深度固定，
   若每 tick 从导弹当前位置射线，导弹上下偏离视线时命中深度仍会漂移——贴地时先命中近处地面，
   瞄准点被拉近，导弹被自己拽下去。现改为 `seekPoint()`：操作者在机上时取 `owner.getEyePosition()`，
   下机后回退到导弹自身位置（与参考实现 `aimPoint(rider)` 同源）。
3. **命中路径优先级未变**：本 tick 线段上先比方块与实体谁更近（沿用原逻辑），
   两者都没有才判是否抵达瞄准点，所以直击与撞地行为不受影响。
4. **射程语义变化**：陶氏最大作用距离从 220 格变为「准星能到的地方」（射线 64 格内取实际方块，
   开阔地/对空 64 格），这是采纳参考设计的直接结果，已同步写入 `apache-plan.md` 第 6.2 节。

### 62.3 验证

- **242 个单测全绿**（238 → 242：陶氏弹道删 2 增 6，净 +4；`--rerun-tasks` 强制重跑确认 0 失败 0 错误）；
- gradlew.bat build 成功并自动同步；JAR **341489 字节**，SHA-256 **588D4626...**，
  build/libs 与实例一致，实例仍只有一份 battlefield JAR。

**必须游戏内验证**：

1. 瞄 30-60 格外地面：导弹是否在准星落点处炸开，而不是飞过头或撞地才炸；
2. 瞄天空平射：导弹是否在约 64 格外自爆（而不是飞到 220 格）；
3. 瞄敌机：是否在 1 格近炸内命中并一发摧毁（M19 的直击即毁规则仍生效）；
4. 发射瞬间若机头压得很低，导弹是否会在过近处炸开（近炸自伤只有爆炸伤害，
   参考项目同样不豁免，如需豁免要单独拍板）；
5. 飞行中把准星从 A 点拖到 B 点，导弹是否跟着改向（线控特征）。

## 63. 武直肩射 + 小鸟火箭语言键 + 小鸟↔武直狗斗（本轮 4 项，合并一节）

用户要求：①给武直也加第三人称肩射视角；②小鸟的火箭语言键出错需修正；
③小鸟与武直的狗斗需修改；④每 2 发子弹打掉武直 1% 的 HP、一发火箭 7 HP。

### 63.1 已拍板决策

1. 肩射**只给后座驾驶员**（与小鸟一致），沿用现有 `view.shoulder_view / shoulder_offset_blocks=1.6 /
   shoulder_height_blocks=0.6`，**不新增配置、不引入 Mixin**；
2. 武直最大 HP **120 → 100**，每发子弹 **0.5**（2 发 = 1% = 1 HP），HUD 仍显示绝对值（100/99/98…）；
3. **任何**火箭打武直**固定 7**（武直自己的 `rocket_damage_scale=0.6` 不适用于打机体）；
4. 反向**不动**：武直 30mm 3/发、武直火箭打小鸟 6/发（10×0.6）、TOW 命中仍秒杀、小鸟↔小鸟仍 1/10。

### 63.2 根因（诊断证据）

- **语言键**：`LittleBirdHud.java:97` 取 `hud.battlefield.weapon.rockets`，两个 lang 文件都没有该键
  （已从成品 JAR 内解出复核）→ 切到火箭巢时显示原始键；`LangFileTest.REQUIRED` 只写死 6 个键，扫不到这一类。
- **狗斗**：`LittleBirdEntity.java:565` 与 `LittleBirdRocketEntity.java:149-153` 用 `instanceof LittleBirdEntity`
  判机体 → 武直被按「无装甲目标」计价：机炮 **14/发**（≈9 发 0.45 s 打光）、火箭 **40** 直击（3 发带走）；
  共用火箭实体同一谓词还导致**武直火箭打武直 = 24**；命中读数的 aircraft 标志为 false → 小鸟看不到武直血条。
  武直侧 `ApacheEntity.isAirframe` 已含双方，两侧判定不一致。
- **肩射**：`ApacheEntity.positionRider` 只有座位分支，全库无 `setShoulderView`。

### 63.3 改动

| 层 | 文件 | 要点 |
| --- | --- | --- |
| 纯规则 | `LittleBirdBallistics`（+测试） | 新增 `Airframe {NONE, LITTLE_BIRD, APACHE}` 与三档 `damageTo`；原 boolean 重载委托，语义不变 |
| 配置 | `LittleBirdConfig` | `gun_damage_apache=0.5`、`rocket_damage_apache=7.0`（+2 getter） |
| 单一来源 | `AirframeTargets`（新建） | `classify(Entity)`/`isAirframe(Entity)`，取代 4 处各自为政的 instanceof |
| 接线 | `LittleBirdEntity`（机炮）、`LittleBirdRocketEntity`（火箭）、`ApacheMissileEntity`（TOW）、`ApacheEntity`（30mm 委托 + `MAX_HEALTH=100`） | 四类武器统一按档取价；命中读数按「是否飞机」上报 |
| 肩射 | `ApacheEntity`（`shoulderView` 字段/访问器/`positionRider` 分支/`resetClientPrediction` 复位）、`ApacheInput`（右键按住驱动 + `endSession` 复位） | 复用 `LittleBirdCameraMath`，仅 `ApacheSeat.PILOT`，客户端专用 |
| 文本 | `en_us.json` / `zh_cn.json` / `LangFileTest` | 新增 `hud.battlefield.weapon.rockets` = **70mm Rocket Pod / 70mm 火箭巢**，REQUIRED 6→8 项 |

### 63.4 数值口径（武直 100 HP）

- 机炮 0.5/发、20 发/秒 → **10 HP/s**；一个过热周期（120 发 / 6 秒）打掉 **60 HP**，需两个周期；
- 火箭 7/发：小鸟 14 发全中 = **98 < 100**，必须机枪补刀；武直自己 38 发也按 7/发计；
- 反向不变（30mm 3/发 → 27 发带走 80 HP 的小鸟；TOW 秒杀）。

### 63.5 验证

- **247 单测全绿**（33 个测试类，0 failures / 0 errors / 0 skipped；本轮新增 6 条纯规则用例 + lang REQUIRED 2 项）；
- `gradlew.bat build` 成功并自动 `syncLocalMods`；JAR **345341 字节**，SHA-256 **D81EDAF6…D48AA1**，
  `build/libs` 与实例 mods 一致，实例中只有 1 份 battlefield JAR；
- 成品 JAR 内已含 `io/github/littlebird/entity/AirframeTargets.class` 与 `zh_cn` 的
  `hud.battlefield.weapon.rockets = 70mm 火箭巢`；
- **全代码翻译键审计**：25 个 `*.battlefield.*` 字面量键全部在两个 lang 文件中存在（本类缺陷已闭合）；
- `build/libs` 内仍留着改名前的 `littlebird-0.1.0.jar`；`syncLocalMods` 只 include
  `${mod_id}-${mod_version}.jar`，故不会被同步，实例 mods 已确认无它。

### 63.6 待游戏内实测（构建通过 ≠ 行为验证）

1. 武直肩射构图：1.6/0.6 偏移对**更大机身**是否把小机身挤出画面/穿模；武直第三人称距离是
   `apache_third_person_distance=9`（小鸟那套没有这个放大），取景可能需单独调；
2. 后座按住右键与机炮/换座/离机/死亡后的视角复位；前座炮手应完全不受影响；
3. 武直 HUD 机体读数（100 → 99 → 98）、2 发 = 1% 的手感、火箭固定 7 的击杀节奏（100/7 ≈ 15 发）；
4. 小鸟打武直时准心下方红条是否出现；反向 TOW 秒杀仍生效；
5. 旧存档或打包物品里 `Health=120` 的武直读档后被 `setHealth` 钳到 100（预期行为）。

### 63.7 仍可调整项

- 语言键文案取 **70mm 火箭巢**（与武直的「19 管火箭巢」对齐，未编造管数）；若想叫「7 管火箭巢」，
  只需改两个 lang 文件同一行。








