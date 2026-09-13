# Little Bird Forge 1.20.1 — 项目约定

本工作区是「战场载具」独立模组项目（Forge 1.20.1，modid: **battlefield**，原 littlebird），与
../final-paradox-forge 是两个互不依赖的 Gradle 工程。

## 强制规则（继承根工作区 AGENTS.md）

1. 任何实现、修复、构建、JAR 同步之前，必须先完整阅读技能
   C:\Users\19818\.codex\skills\recreate-minecraft-map-items\SKILL.md 并按其规则执行。
2. 问题默认先做只读诊断，报告证据后等待用户确认，再实施修改；只读的规划请求不得实施。
3. 证据优先：引用具体文件、行号、命令输出。构建成功不等于视觉/行为验证通过。
4. 子代理只执行封闭切片（单一子系统，通常 1-5 个文件，绝不超过 8 个），
   根代理负责诊断、方案、切片、集成、全量构建、自审与 JAR 同步。
5. 本项目的实现计划见 docs/plan/。开工前必须确认 docs/plan/PLAN.md 第 9 节的待定决策。

## 工程事实（已核实）

- 工程位置：**WSL 原生 `/root/projects/little-bird-forge`**（ext4；2026-09-12 从 `C:/Users/19818/Desktop/newmod/little-bird-forge` 迁移，Windows 侧原件保留为迁移前副本）。
- JDK 17: WSL 原生 `/usr/lib/jvm/java-17-openjdk-amd64`（apt `openjdk-17-jdk-headless`）；不再需要 `C:/Program Files/Java/jdk-17`，也不再需要转发 `JAVA_HOME`。
- Gradle wrapper 8.8，Forge 47.4.20 / MC 1.20.1 / official mappings 1.20.1；Gradle 缓存已从 Windows `~/.gradle` 复制到 `/root/.gradle`。
- 参考工程（只读先例，不修改）：`/mnt/c/Users/19818/Desktop/newmod/final-paradox-forge`（仍在 Windows 侧）。

## 构建与验证（实施阶段生效）

- 构建全部在 WSL 原生环境里跑：`cd /root/projects/little-bird-forge`，JDK = `/usr/lib/jvm/java-17-openjdk-amd64`，Gradle 缓存 `/root/.gradle`。
- 快速编译:  ./gradlew compileJava
- 纯规则测试: ./gradlew test --tests "*LittleBird*"
- 全量构建:  ./gradlew build   （资源校验 + reobf + 测试 + 打包）
- JAR 同步必须作为 build 成功后的独立收尾步骤，并且不能声称替代游戏内验证。
## 同步规则（每次更新都必须做）

1. 任何实现改动的收尾都是 `./gradlew build`（在 `/root/projects/little-bird-forge` 里跑）。`gradle.properties` 里的 `local_mods_dir` 已指向
   `/mnt/c/Users/19818/AppData/Roaming/.minecraft/versions/1.20.1-Forge_47.4.20/mods`（PCL2 版本隔离的游戏目录；WSL 经 `/mnt/c` 访问），
   build 成功后由 `syncLocalMods` 自动把 reobf JAR 复制过去，不需要额外参数。
2. 同步后必须核对 `build/libs/battlefield-<version>.jar` 与实例 mods 中 JAR 的大小与 SHA-256 一致，
   并确认实例 mods 中只有一份 battlefield JAR（避免 duplicate mod id；改名前的 littlebird JAR 必须删除）。
3. 游戏正在运行时 Windows 可能锁住目标 JAR：先完全退出游戏再构建。
4. 构建成功不等于游戏内验证通过；视觉与手感仍需在客户端实测。

## Token 消耗约定（2026-09-11 起，强制）

基线（改造前）：implementation-status.md 1010 行 / 157 KB / 60 节；docs/plan 共 249 KB。

1. **收尾报告 ≤ 10 行**：结论 + 关键证据（文件:行号 / 数字）+ 验证（测试数、JAR 哈希）+ 待实测项。
   明细写进文档，**不在聊天里重复文档里的表格**。
2. **文档合并写**：implementation-status.md 每 3-4 个切片合并一节，只写要点、取舍与证据；不复制测试清单，
   不重复已被测试钉住的数字。
3. **读代码用 grep 定位 + offset/limit**，不整篇读；同一轮内一次读够锚点，避免同一文件反复读
   （ApacheEntity 986 行 / LittleBirdEntity 915 行是大头）。
4. **一轮内先批量改完再构建**：build / runData 取最小次数；javac 输出只过滤 BUILD/FAILED/error。
5. **javap / 反汇编只打印命中的几行**，不 dump 整段字节码。
6. **长日志先裁剪**：崩溃日志只贴头部约 30 行 + 一次完整调用栈；或直接说「看最新 crash-report」，
   由代理自己读文件并只提取需要的行（14:59 那份 309 KB 的日志单次就抵好几轮开销）。
7. **图片按需看**：能用码点/数值验证的（编码、坐标、哈希）不靠目视；不事后重复看图。
8. **纯文档改动不跑 build**：只核对 JAR 哈希未变并说明，省掉一次 Gradle 噪声。
