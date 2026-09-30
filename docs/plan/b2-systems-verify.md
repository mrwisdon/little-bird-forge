# B-2 系统独立验收报告（b2-verify / task-14）

命令：flock -w 900 /root/.lb-gradle.lock ./gradlew test build（我跑的；另用 gradle-free 反射 runner 先独立跑过一遍对抗用例）
结论：**两次 BUILD SUCCESSFUL**（12:32 用 18s / 12:34 用 10s，FULL_RC 均 0）；每次 test-results/test/*.xml 74 个 = **554 tests / 0 failures / 0 errors / 0 skipped**；build/libs 均只有 1 个 JAR：12:32 battlefield-0.1.0.jar 1601826 B sha256 792b6ff506…，12:34（树里多了刚落盘的 client/BombRenderer.java）1605460 B sha256 32c68f22ce…。我的两个测试文件 md5 在两次构建前后一致（8bec5b19… / dd64665d…），无并发写入。

| 判据 | 结果 | 原始数字 |
|---|---|---|
| ./gradlew test build | **PASS** | BUILD SUCCESSFUL；554/0/0/0；build/libs 仅 battlefield-0.1.0.jar |
| B2BombRulesAdversarialTest（我写） | **PASS** | gradle XML 实测 20 tests / 0 fail / 0.02s：0/负/Integer.MAX 库存、连投清仓不欠投、cooldown 网格、BombKind 契约值(120/6/45/4)、FLOOR_DETONATE_Y=-70 含边界、无制导 vs 有制导、>64 格退化为弹道、GUIDANCE_AUTHORITY 上限、null 状态、终端速度 1.25、1000 tick 长落有限且单调、NaN/Inf |
| B2PodMathAdversarialTest（我写） | **PASS** | 16 tests / 0 fail / 0.025s：yaw±110、pitch[-120,10] 夹取与边界、拖拽饱和后立即回程、chase 端点与 no-overshoot、lookVector 单位长度与 ±90°、水平/上视不交地面、交点共线与地面、FOV 端点 0.16/1.0 与单调、zoom 反函数全区间、±180 接缝（wrap/reticleOffset/锁窗）、锁窗含边界、NaN |
| 独立复算 dropLeadDistance | **PASS** | 与我自写的分步积分（两种步序）相对误差 <1%：5 组 probe(速度/高度) 0.5/40、1/120、0.9/300、1.4/8、0.2/800 |
| 实体 id | **PASS** | register("jdam_bomb")=1、register("incendiary_bomb")=1；entity ids=15 无重复、item ids=30 无重复 |
| 网络 id | **PASS（按修订版 14/15/16）** | 0..16 共 17 条无重复；12=JetFlarePacket、13=MainGunImpactPacket 已占，B2 用 14/15/16。注意 spec §0.3 仍写 12/13/14，与实现/口头修订不符，建议改文档 |
| lang | **PASS** | en=145、zh=145，键集完全相等；§0.6 十键 en/zh 全在，无空值 |
| 配置键 §0.7 | **PASS** | 13 键全在 LittleBirdConfig |
| 按键 B2_POD | **PASS** | LittleBirdKeyMappings 声明 1 次 + ClientModEvents register(B2_POD) 1 次 |
| 实现文件齐备 | **PASS** | JdamBombEntity / IncendiaryBombEntity / B2PodView / B2BombReleasePacket / B2PodDesignatePacket / B2WeaponSelectPacket 全部存在 |

备注（非阻塞，待 Lead 裁决）：① nextCooldown(true)=12=6×SALVO_COUNT（实现按“齐投锁定整个 ripple”），spec §2 措辞是“按 6 tick 间隔齐投/单投”，而 bombsToDrop(true) 同 tick 落 2 枚 → ripple 并未逐枚建模；我按不变量断言（网格倍数、salvo≥single）故未判 FAIL，请二选一。② 我最初按 y=0=地面 断言 shouldDetonate(0,false)=true 挂了，实测阈值是 FLOOR_DETONATE_Y=-70，已改为真实语义边界——这是我的假设错误，不是实现缺陷（同类 3 处 pod 断言符号错也已自查修正）。③ 12:2x 有个短暂窗口我的测试文件被参数上限截断成半截（Lead 看到的那次编译错误），已修复：gradle-free javac RC=0 与 compileTestJava BUILD SUCCESSFUL 双重通过。④ Lead 的客户端文件在我两次构建期间仍在落盘（BombRenderer.java 12:33:32，B2Camera/B2Hud/B2Input/B2PodOverlay/B2PodView 12:25），故 JAR 哈希只绑定我那一刻的树；Lead 停手后建议再跑一次终态 build，我随时可跑（同一把 flock）。
