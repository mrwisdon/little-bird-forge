# 地面载具参考图清单（阶段二产出）

用途：为 `MainBattleTankModel` / `InfantryFightingVehicleModel` 的结构重做提供依据。
版权：R2–R5 为 Battlefield 2042 游戏渲染/截图，**仅作建模参考，不随工程或 JAR 分发**；R1 为 Wikimedia 绘图。
所有图片在核对时下载到 `/tmp/ref/`（工程外），未加入 `src/`。

| # | 来源站点 | URL | 分辨率 | 视角 / 用途 |
| --- | --- | --- | --- | --- |
| R1 | Wikimedia Commons | https://commons.wikimedia.org/wiki/File:M1A1_Abrams.svg | 源 990×765（取 1920px 渲染，轮廓 bbox 1853×814） | **正交侧视**；主轮廓/比例基准：7 对负重轮、后置主动轮+前置诱导轮、侧裙、TUSK 反应装甲、炮塔泄爆板、120mm 热护套+炮口、车长塔 |
| R2 | bf2042.swiki.jp | https://bf2042.swiki.jp/index.php?M1A5 | 1920×607 | BF2042 M1A5 **3/4 前左**渲染；伪装网、烟雾弹、UI 数值 |
| R3 | battlefield.fandom（Fandom） | https://battlefield.fandom.com/wiki/EBAA_Wildcat → File:BF2042-EBAA Wildcat.png | 1107×642 | Wildcat **3/4 前左**渲染；**6×6 三轴**、防撞杠、车顶右侧遥控武器站、左侧导弹箱、侧挂储物箱 |
| R4 | battlefield.fandom | File:EBAA Dual 30mm.jpg | 1920×1080 | Wildcat 装备界面 **3/4 前右**；双联 30mm 炮塔、座位名（DRIVER / WEAPON STATION / WEAPON BAY / COMMANDER SEAT） |
| R5 | battlefield.fandom | File:Ebrc.jpg / File:Jaguar IRL.jpg | 1921×1080 / 925×617 | 真实原型 **EBRC Jaguar**（确认 Wildcat 的现实参照） |

## 从参考图读出的结构要点（已用于阶段 1）

- 坦克：车体分上下两层 + 阶梯状首上/首下装甲；**每侧 7 个负重轮**，后置主动轮、前置诱导轮，履带包络轮组；炮塔略靠车体中部，尾部有弹仓尾舱，120mm 炮含热护套与炮口段；车长塔与装填手舱盖在炮塔顶部。
- 步战：**6×6（三轴）**高箱式车体、前部驾驶舱风挡带、车头防撞杠；车顶左侧折叠导弹箱、右侧遥控武器站（带机枪）、中间观瞄；尾部双开门。
- 比例锚点（R1 像素测量）：车身（含履带）长约 1202 px、车体高约 555 px（长:高 ≈ 2.17:1）；全轮廓（含炮）1853×814 px。

## 参考图缺口（如实标注，影响哪些部位）

1. **两车都没有正交正视/背视/俯视图；Wildcat 无纯侧视图** → 车宽、车高、炮塔俯视外形、车顶布局、轮距只能由 3/4 图估算，属近似。
2. R2/R3 有伪装网与植被遮挡 → 车体上部一些小构件（天线座、灯组、把手）看不清，阶段 2 只能做"可见轮廓"部分。
3. R5 的真实 Jaguar 图清晰度低（游戏内过场），只用于确认原型，不作比例依据。

## 冲突记录（未擅自取舍）

- M1A5 生存性：bf2042.swiki.jp（R2）标 **75**，3楼猫门户数据表为 **80**；工程当前按 80 实现。本次不改数值，仅留档。
- 导弹箱左右侧：R3/R4 两张 3/4 图受视角影响，无法百分百确定导弹箱在车体左或右；阶段 1 暂按"左导弹箱、右武器站"实现，属待裁定项。
