# Windows 构建与部署（本机验证，2026-11-12）

本文件只记录「在这台 Windows 机器上怎么构建/出包」，与作者原来的 WSL 流程并存。
AGENTS.md 里以 WSL（`/root/projects/little-bird-forge`、`/mnt/c/...`）为准的段落在本机不适用。

## 环境

| 项 | 值 |
| --- | --- |
| JDK | `D:\DevTools\Java\jdk-17.0.20+8`（17.0.20，Forge 1.20.1 要求 17） |
| Gradle | wrapper 8.8（`gradle-8.8-bin` 已在 `GRADLE_USER_HOME` 缓存里） |
| GRADLE_USER_HOME | `D:\PackageData\gradle`（环境变量已设；不是 `~/.gradle`） |
| 工程 | `D:\dsh工作区\战争兵器\little-bird-forge` |

`JAVA_HOME` 必须显式指到 JDK 17：系统 `java` 在 PATH 上是 23，Gradle 8.8 不支持。

## 命令（Git Bash 里）

```bash
export JAVA_HOME="D:\\DevTools\\Java\\jdk-17.0.20+8"
./gradlew compileJava      # 增量编译，几秒
./gradlew test             # 730 个 JUnit 用例
./gradlew build            # 资源 + 测试 + reobf + 打包
./gradlew build --offline  # 依赖已缓存时用这个：见下面“Gradle 可能卡在联网检查”
./gradlew runClient        # 开发客户端（首次会下载 MC 资源，约数百 MB）
```

> **Gradle 可能卡在联网检查**：2026-11-12 实测，`./gradlew build` 有一次卡在
> `maven.minecraftforge.net` 的连接上 7 分钟无进展（daemon 仅持有到 Cloudflare 的 ESTABLISHED 连接、
> 缓存不增长）。依赖已缓存后加 `--offline`，37 秒完成。真需要下载新依赖时再联网跑一次。

## 产物

- `build/libs/battlefield-0.1.0.jar`（约 2.2 MB，已 reobf，可直接进 Forge 47.4.20 的 mods）
- 校验：`sha256sum build/libs/battlefield-0.1.0.jar`（jar 清单里带构建时间戳，每次 build 哈希都会变，属正常）
- 本机第一次完整交付副本：`..\battlefield-0.1.0-forge-1.20.1.jar`

## 自动同步到游戏实例（可选）

`gradle.properties` 的 `local_mods_dir` 默认留空。要用「build 完自动进 mods」就写 Windows 路径：

```properties
local_mods_dir=C:/Users/<you>/AppData/Roaming/.minecraft/versions/<forge版本目录>/mods
```

留空时 `syncLocalMods` 任务不会注册，build 不会碰任何游戏目录。

## 本机部署踩过的坑

1. **首次构建很慢**：`maven.minecraftforge.net` 直连约 250 KB/s，依赖下载约 96 MB、耗时约 11 分钟；
   失败过一次（`Could not resolve net.minecraftforge:forge:...:userdev`，实为网络超时），原样重跑即通过。
   下载结果缓存在 `D:\PackageData\gradle`，之后增量构建 20–30 秒。
2. **不要照抄 `local_mods_dir=/mnt/c/...`**：Windows 上会被当成相对路径，凭空建出 `D:\mnt\c\...` 垃圾目录。
3. **AGENTS.md 第 1 条要求的技能文件**（`C:\Users\19818\.codex\skills\recreate-minecraft-map-items\SKILL.md`）
   在本机不存在（那是作者另一台机器的路径），需要用户补一份或明确替代规则。

## 仓库与文档

- 远端：https://github.com/mrwisdon/little-bird-forge，本机克隆 HEAD `76b7187`
  （feat: smoke grenades, PHL-191, PLZ-05 fire missions and unified HUD）。
- 交接读法：`docs/plan/HANDOFF.md` → `implementation-status.md` 最后一节（§76）。
