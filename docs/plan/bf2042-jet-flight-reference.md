# BF2042 喷气机飞控参考资料（社区/资料向）

采集于 2026-09-14，用于对齐 `JetFlightModel` 的手感设计。**非官方数据挖掘**：公开渠道只找得到操作说明、攻略与玩家实测描述；没有 BF2042 飞控参数表（sym.gg 只有武器数据，game8 只有 0–100 展示卡）。

## 1. 控制方案（PC 默认）

- 鼠标是**双轴虚拟摇杆**：左右 = 滚转，上下 = 俯仰（默认反轴，可在设置里改）。鼠标不是自由视角。
- W/S = 节流阀（W 加速、S 减速），A/D = 滚转；方向舵可改绑到方向键。
- 官网《Piloting tips》只确认 W/A/S/D + HUD 读数（速度/高度/姿态/健康），没有气动细节。
- 攻略站建议把方向舵改到方向键、滚转放 A/D，用手感换精度——说明默认鼠标同时管俯仰+滚转。

来源：[EA Piloting tips](https://www.ea.com/games/battlefield/battlefield-2042/tips-and-tricks-hub/flying-vehicles)、[RPS how to fly jets](https://www.rockpapershotgun.com/battlefield-2042-jets)、[EA PC controls](https://www.ea.com/games/battlefield/battlefield-2042/tips-and-tricks-hub/pc-controls)

## 2. 转向机动：压坡度 + 拉杆（社区共识）

- RPS："Rolling is key … roll in the direction you want to turn, and then pitch up. Yaw is much slower."
- r/battlefield2042（tightest turning）："rotate the plane 90 degrees to the direction you wanna turn, and pitch up to laterally pull a tight turn."
- Steam：新手抱怨自己"全推力+加力也转得慢"，而高手"像停在半空原地掉头，不到 2 秒转 360°"。
- 结论：**滚到约 90° 后拉满俯仰，在水平面内转**；方向舵只做瞄准微调。转弯是**高角速率、由俯仰输入驱动**的街机模型，不是温和的 g·tanφ/V。

来源：[RPS](https://www.rockpapershotgun.com/battlefield-2042-jets)、[tightest turning](https://www.reddit.com/r/battlefield2042/comments/15otf80/what_is_the_method_for_tightest_turning_in_jets/)、[Steam 90-Degree turns](https://steamcommunity.com/app/1517290/discussions/0/5774239174015486063/)

## 3. 转弯率与速度：存在角点速度（corner speed）

- Steam："you need to be going roughly medium speed … if you slow down from max speed to say middleish, you will be at your maximum turning speed."
- r/battlefield2042（Jet tips）："you have to get the right speed to turn the fastest"；灵敏度只影响"进入转弯的快慢"，转满后的角速度不变。
- 结论：**中速转弯最快**（角点速度），高速转弯半径变大、低速舵效/过载下降。

来源：[Steam How do you turn Jets](https://steamcommunity.com/app/1517290/discussions/0/5774239174015486063/)、[Jet tips](https://www.reddit.com/r/battlefield2042/comments/1bfnhgt/jet_tips/)

## 4. 能量：拉杆掉速、加力补速

- 持续转弯明显掉速；EA 论坛加力补丁帖抱怨"双方都在慢慢飞、掉速不可接受"，加力"撑不过第一个转弯"。
- 玩家对最强转弯的油门有分歧：一派"全加力 + 拉满"，一派"收油门到怠速再拉满更紧"——与版本和角点速度有关。
- 结论：转弯是**耗能**机动，油门/加力是管理手段；但掉速应是**渐进**的，不该是瞬时腰斩。

来源：[EA 论坛 Jet Afterburner change feedback](https://forums.ea.com/discussions/battlefield-2042-general-discussion-en/jet-afterburner-change-feedback/7212296)、[tightest turning](https://www.reddit.com/r/battlefield2042/comments/15otf80/what_is_the_method_for_tightest_turning_in_jets/)

## 5. 失速

- r/battlefield2042（Stall mechanics）："The stall time in this game is UNBELIEVABLY long"，且失速后很难改出，还会和飞行天花板叠加。
- 结论：**失速是低速的深层状态**，不是巡航拉一次杆就触发；进入后恢复缓慢。

来源：[Stall mechanics in jets](https://www.reddit.com/r/battlefield2042/comments/19e35po/stall_mechanics_in_jets/)

## 6. 两机差异

- 历史 bug 报告：Su-57 转弯速度占优（F-35 只多一个 VTOL）。game8 展示卡两机相同（Maneuverability 40 / Speed 95）。

来源：[F-35 vs SU-57 turn speed](https://www.reddit.com/r/PilotsofBattlefield/comments/rel13b/bf2042_jet_turn_speed_unbalanced_between_f35_and/)、[game8 F-35E](https://game8.co/games/Battlefield2042/archives/350453)、[game8 Su-57](https://game8.co/games/Battlefield2042/archives/350454)

## 7. 对照我们的 JetFlightModel（差距）

| 项 | BF2042 | 我们现状 | 位置 |
| --- | --- | --- | --- |
| 转弯驱动 | 俯仰（拉杆）驱动、高角速率 | 只看坡度与速度，拉杆不参与 | `JetFlightModel.java:117-120` |
| 速度曲线 | 中速角点最快 | `g*tan(roll)/flow` 单调，越慢越快 | `JetFlightModel.java:154-167` |
| 能量 | 渐进掉速、加力补 | 按俯仰**瞬时钳**速度上限 | `JetFlightModel.java:130,133,135-139` |
| 失速 | 低速深层、长恢复 | 巡航拉杆约 2s 就失速、失速覆盖操纵 | `JetFlightModel.java:42-47` |
| 拉杆与升力 | 拉杆增升力/转向 | 拉杆只掉 `flow`、不增 `support` | `JetFlightModel.java:109-115` |
| 相机 | 相机跟机头 | 相机俯仰 = 玩家 xRot，机头滞后 | `JetEntity.java:341` |

## 8. 建议的改造方向（2026-09-14 已实施，见 jet-plan §9.15；实现时另补了"速度矢量跟随机头"，并保留 `pitchSpeedCoupling=0.5`）

1. **俯仰驱动转弯**：`turn = cornerShape(flow) * (1 + pullTurnGain * pull)`，`cornerShape` 在 `vCorner`（约 0.6–0.7 × 巡航）取峰；坡度决定转弯平面/方向，不再只靠 `g*tan/V`。
2. **拉杆并入升力**：`support = lift * clamp01(cos(roll) + pullSupport * pull)`，坡度中拉杆能保持高度。
3. **去掉瞬时上限钳制**：代价改为诱导阻力（`horizontalDrag += inducedDrag * pull`），掉速平滑。
4. **失速下移阈值、放慢恢复**，失速期间保留部分操纵、不用低头完全覆盖飞行员输入。
5. （可选）相机俯仰锚定机头，消除准星与机头滞后。
6. 参数按角点速度标定，并补单测（60° 坡度拉杆 20 刻不失速；转向率随拉杆上升；中速转弯快于高速/低速；俯冲仍可超巡航）。

## 9. 不可靠/未证实项

- 具体的角点速度数值、最大角速率、失速速度没有公开数据，只能按社区描述相对标定。
- "加力帮助转弯"与"收油门帮助转弯"在不同版本/玩家间矛盾，实施时以可调参数暴露，进游戏标定。
- 90°/2s 掉头属于玩家主观描述，用于定"高角速率"的量级，不作为精确指标。
