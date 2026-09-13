# 交接文件（一页）

开新会话时先读这一页 + docs/plan/survival-crafting.md 第 8 节（已拍板决策），即可接上进度。

## 工程与流程

- 工程：little-bird-forge（modid battlefield，Forge 47.4.20 / MC 1.20.1 / official mappings / Java 17）。
- 构建：JAVA_HOME='C:\Program Files\Java\jdk-17'，GRADLE_USER_HOME=.codex-gradle-user-home，命令 .\gradlew.bat build（成功后自动同步到 PCL2 实例 mods；runData 要加 --rerun-tasks 才真跑）。
- 现状：242 单测全绿；JAR 341489 字节，SHA-256 588D4626...（与实例一致，实例只有一份 battlefield JAR）。
- 约定：纯规则类（不引用 net.minecraft）+ 单测是验证主干；每片收尾 build + 哈希 + 自审，见 AGENTS.md。

## 未目视确认清单（只能在游戏里看，按优先级）

1. 座舱/相机：阿帕奇第三人称距离（apache_third_person_distance 默认 9 格）、贴墙是否被地形顶回、相机随滚转方向；日志搜 battlefield.mixins.json 确认 Mixin 注入无告警（有告警则相机不会后拉）。
2. 四座小鸟（最新）：四人上机后第一个是飞行员、后三个坐长凳/舱尾是否叠人；副驾按 F5 是否被拉回第三人称、C 键无效、左键/1/2/R/Z 完全无反应、EXIT 能下机；飞行员行为是否与以前一致。
3. 弹药闭环：弹药箱（8 铁 + 4 火药 + 1 箱）三种补给速率；小鸟 2000 发弹链会打空、7 发待发打空后 R 搬 42 发备弹；阿帕奇 600 链炮、19/95 火箭、1/5 陶氏；HUD 读数。
4. 回收/修复/坠毁：潜行空手右键收回（血量随物品保留、放下后保持）；残血机 + 铁锭修复；打爆一架掉 2-6 铁锭且不被自己的爆炸清掉。
5. 配方与成就：配方书解锁顺序（铁锭 -> 零件 -> 整机）、9 零件与两机合成、图标与中英名称、两个成就提示。
6. 陶氏：一发直击是否即时摧毁（不再 4-6 发）、减速后的手感（是否太灵活）、打玩家不秒杀、64+ 格双马尾尾迹可见、垂直发射尾迹不叠成一条。
   另有 §62 新行为待验：瞄地面时是否在**准星落点**炸开、瞄天空是否约 64 格自爆、拖准星时导弹是否跟着改向。
7. 模型/贴图：阿帕奇机身比例（9.33 格）与下颌炮口位置、物品图标与弹药箱方块外观、乘员模型是否已隐藏（名字标签是否还在）。
8. 机炮指向（本次改动，§61）：单人 V 切前座后鼠标转到侧后方（±90°…±180°）下颌炮是否跟着转、准星回到屏幕内；
   机体滚转/大俯仰时炮口是否仍停在准星上；炮塔扭到侧后方是否穿模；俯仰压限提示；双人时主驾转弯副驾瞄准点是否稳定。

## 已知欠账（代码层，可顺手清）

- 死配置键：rocket_rearm_seconds / rocket_rearm_batch / apache rocket_resupply_seconds / missile_resupply_seconds（代码已不读取，自动补给已被弹药箱取代）。
- datagen 不输出「同名物品进、同名物品出」的无序配方，两条修复配方因此手写（src/main/resources/data）。
- 修复配方无法判断 NBT：满血机体 + 铁锭也会被匹配（浪费铁锭）。

## 工具坑（踩过的）

- runData 会被 Gradle 判 UP-TO-DATE 而不执行，要 --rerun-tasks。
- lang/*.json 不要逐行追加：漏一个逗号会让整份文件失效，游戏里所有名称变原始键；改完必须过 LangFileTest。
- 非 ASCII 资源走 base64 -> PowerShell [System.IO.File]::WriteAllText(UTF8Encoding(false))，不要用普通写入路径（本次交接文件第一次就是这么写坏的）。
- PowerShell 里函数名别用单字母（R 会被解析成内置别名 Invoke-History）。
- 控制台会把 UTF-8 当 GBK 打印，判断中文是否损坏只能用码点/解析，不能靠肉眼。

## 怎么继续

读本文件 + survival-crafting.md 第 8 节 -> 看 implementation-status.md 最后一节（§62 陶氏瞄准点）-> 开始下一片；历史节只在追溯证据时 grep 定位。
