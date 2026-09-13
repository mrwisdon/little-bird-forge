# UI / HUD 设计：Little Bird

## 0. 先行事实（只读调查结论，很重要）

- 参考工程 final-paradox-forge 里**没有任何 HUD / 覆盖层 / 自定义 Screen 先例**：
  以下模式全部无匹配：RegisterGuiOverlays、IGuiOverlay、LayeredDraw、GuiGraphics、
  RenderGuiOverlayEvent、extends Screen、MenuType、AbstractContainerMenu。
  客户端在世界空间画字的先例只有实体渲染器里的 font.drawInBatch（B8DetonatorBombRenderer 58 行等）。
- 家族里唯一的「轻量 UI」先例是**动作栏文字**：
  player.displayClientMessage(Component.translatable(...), true)（ArcaneMasteryState 20/25/26 行等大量使用），
  以及 181KB/183KB 的 en_us.json / zh_cn.json 语言文件。
- 结论：HUD 是一个**全新子系统**，没有可复制的模板，必须自己定规范；
  同时意味着风险集中在「首版做多少」——因此下面给出「极简 / 标准」两档，默认标准但可一键降级。

## 1. 设计原则

1. 只在驾驶本载具时出现；尊重原版 F1（options.hideGui）与打开 GUI（minecraft.screen != null）时隐藏。
2. 不挡视野：中央只留准星，其余全部贴边、半透明、可关闭。
3. 信息优先级：瞄准反馈 > 武器状态 > 飞行仪表 > 载具耐久。
4. 不阻塞操作：首版**不做容器界面/菜单**，武器切换用 1/2 + HUD 高亮反馈。
5. 颜色不单独承载信息：过热 = 红 + 条满 + 文字；空巢 = 图标变暗 + 文字 + 告警。
6. 全部文案走语言键（zh_cn / en_us），数字右对齐，等宽一致。
7. 性能：只在驾驶时渲染；不每帧 new 对象；字符串宽度缓存；纯几何 + 原版字体，不自带字体贴图。
8. 不使用 BF6 的 UI 素材或图标，避免版权与外部资源依赖。

## 2. 三档风格（配置 hudStyle，默认 STANDARD）

| 风格 | 内容 | 用途 |
| --- | --- | --- |
| MINIMAL 极简 | 准星 + 弹药/热度 + 告警 | 追求沉浸、录制视频 |
| STANDARD 标准 | 极简 + 航向带 + 坡度指示 + 速度/高度 + 载具耐久 | 默认 |
| ALERTS_ONLY 仅告警 | 准星 + 告警 | 极端低干扰 |
| OFF 关闭 | 只保留原版准星（不画我们的覆盖层） | 兼容其他 HUD 模组 |

另有 hudScale（0.75 / 1.0 / 1.25 / 1.5）与 hudOffsetX/Y，以及 highContrast 开关。

## 3. 布局（坐标系：GUI 缩放后的屏幕 W x H，锚点为相对位置）

| # | 元素 | 位置 | 尺寸/内容 |
| --- | --- | --- | --- |
| 1 | 准星（机头轴线） | 屏幕正中 | 十字：2px 粗、臂长 9px、间隙 3px + 中心 1px 点 |
| 2 | 命中标记 hit marker | 准星同点 | 4 条斜线，200 ms 淡出；击杀时变红加粗 |
| 3 | 航向带 | 顶部居中, y=6 | 160x10，显示 ±40° 窗口，15° 一刻度、45° 标数字/N·E·S·W，中心游标三角 |
| 4 | 坡度指示 | 准星正下方 +34px | 120px 参考横线 + 一对随滚转移动的小三角（±38° 满偏 60px） |
| 5 | 武器条 | 底部居中, y=H-30 | 左：机枪热度条 80x6 + 百分比；右：火箭巢 2x7 小方格（5x8，间距 2）+ 单发/连发文字；当前武器加 1px 高亮边框 |
| 6 | 飞行仪表 | 左下 x=6, y=H-48 | 三行：水平速度、垂直速度（带 ↑/↓ 箭头）、海拔 Y |
| 7 | 载具状态 | 右下（右对齐） | 旋翼转速文字 RPM xx% + 耐久条 60x5（30% 以下闪红并叠加屏幕四角红色暗角） |
| 8 | 告警区 | 中央上方 y=H/2-70 | 最多 2 行，优先级队列，每条最长 2 秒 |
| 9 | 首次骑乘引导 | 右下上移一档 | 4 行操作提示，按过对应键后该行淡出（纯客户端本地状态） |

告警条目（按优先级）：PULL UP（前方/下方 8 格内有地形且在下沉）、OVERHEAT（机枪过热）、
NO ROCKETS / RELOADING（弹药耗尽/补给倒计时）、ENGINE OFF（旋翼转速 < 15%）、LOW HEALTH（耐久 < 30%）。

## 4. 视觉规范

- 配色：正常 #E6E6E6、良好 #7FD46A、警告 #F2C94C、危险 #EB5757、信息/航向 #8AC6FF。
- 所有文字与图形叠加 0x80000000（0xB3000000 高对比）半透明黑底 + 1px 0x40FFFFFF 边框，
  保证雪地、天空、水下都可读。
- 数字一律右对齐并用等宽间距；单位文案走语言键（GUN 弹药、ROCKETS 火箭等）。
- 高对比模式：纯黑底、纯白/纯黄文字，不依赖红绿区分（色盲友好）。
- 屏幕震动：火箭命中/坠机时 3 tick 轻微相机抖动，可配置关闭。

## 5. 技术实现（对应 M7a 切片）

- 注册：client/LittleBirdHud.java，@Mod.EventBusSubscriber(modid, Dist.CLIENT, bus = MOD)，
  在 RegisterGuiOverlaysEvent 注册 ResourceLocation(littlebird:heli_hud) 的 IGuiOverlay。
  这是新项目第一个 GUI 覆盖层（参考工程无先例，必须在真实客户端验证注册事件时机）。
- 布局计算：client/LittleBirdHudLayout.java 为**纯规则类**（不 import 任何 net.minecraft.*），
  输入 (W, H, scale, style, 数值状态) → 输出各元素的矩形与对齐基准，可 JUnit 断言。
- 数据来源：实体 SynchedEntityData（弹药、热度、耐久、武器模式、RPM）——HUD 只读，不参与游戏逻辑。
- 命中反馈：服务器命中时向驾驶员发 LittleBirdHitFeedbackPacket（含命中/击杀标志），
  客户端记录时间戳并在 200 ms 内绘制标记（参考工程的曳光包分发已证明 TRACKING_ENTITY/PLAYER 分发可行）。
- 降级方案：若覆盖层注册在客户端出现兼容问题，告警可退回动作栏文字
  （displayClientMessage(..., true)，家族内已验证的写法）；准星/弹药条无降级，必须实测。
- 性能约束：不每帧 new Component/String；数字用预分配缓冲；避免在 render 中读世界数据。

## 6. 界面（Screen / Menu）决策

- 首版**不做**任何自定义 Screen、容器菜单或物品栏界面（BF 直升机武器为固定挂载，没有换装需求）。
- 玩家改键：直接使用原版「按键设置」里的模组分类（EXIT / SALVO 由 RegisterKeyMappingsEvent 自动出现）。
- 手感与 HUD 配置：使用 Forge 为 ModConfigSpec 自动生成的配置界面（模组列表 → 配置），
  提供灵敏度、俯仰死区、反转俯仰、相机滚转系数、hudStyle、hudScale、位置偏移、高对比、震动开关。
- 第二版可选（独立里程碑，需用户确认）：挂载配置屏幕（MenuType + AbstractContainerScreen），
  用于更换武器挂载/副油箱；届时再设计同步与容器逻辑。

## 7. 无障碍与本地化

- 不依赖颜色区分状态（形状/文字/闪烁三重表达）。
- 尊重 GUI 缩放：所有尺寸按 scale 计算，在 GUI scale 1/2/3/4 下都不重叠、不溢出。
- 语言键规划前缀：hud.littlebird.*（标签）、key.littlebird.*（按键）、alert.littlebird.*（告警）、
  tutorial.littlebird.*（首次引导）、entity.littlebird.little_bird / item.littlebird.deploy 等。

## 8. 验证方法（构建通过不算视觉验证）

- 纯规则单测（不需启动游戏）：
  - 布局锚点：极窄/极宽/极矮窗口下元素不重叠、右对齐基准正确；
  - 文字超宽截断；数字格式化（速度 km/h 与 格/秒 两种单位）；
  - 告警优先级队列（同时触发时取高优先级两条、超时移除）。
- 游戏内截图批次（一次性收集，技能要求）：GUI scale 1/2/3、F1 隐藏、四种 hudStyle、
  机枪过热、火箭打空、耐久 20%、夜间/雪地/水下三种背景、第三人称与旁观视角。
- 兼容性实测：与其他 HUD 模组共存的覆盖层顺序（如与 Nvidium/其他 HUD 冲突以实测结论为准，不预设）。

## 9. 待用户确认的 UI 决策

1. 速度单位：km/h（更接近 BF6）还是 格/秒（更 MC）？
2. HUD 默认风格：标准（全仪表，推荐）还是极简（准星 + 弹药 + 告警）？
3. 是否要击杀提示与首次骑乘操作引导（均为客户端本地、可关）？
4. 是否要屏幕震动与低耐久红色暗角（默认开，怕晕的玩家可关）。
