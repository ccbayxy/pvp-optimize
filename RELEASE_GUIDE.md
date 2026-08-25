# PvP-Optimize 发布与开源手册

> 配套 `LEARN_TO_CODE.md`（零基础编程）和 `UPDATE_GUIDE.md`（维护手册）。
> 目标：让你的 mod 安全备份、跨版本升级、发布到 Modrinth、托管到 GitHub 接受社区贡献。

---

## 目录

- 第 1 章 Git 基础（备份与版本管理）
- 第 2 章 Fabric 1.21+ Mojang 映射迁移
- 第 3 章 Modrinth / CurseForge 发布
- 第 4 章 GitHub 开源全流程（含检查清单）
- 附录 A Git 速查
- 附录 B Modrinth 元数据示例
- 附录 C GitHub 仓库必备文件清单

---

# 第 1 章 Git 基础

> 很多新手觉得 Git 难，其实常用的就 6 个命令。看完这章你就能用 Git 备份你的代码。

## 1.1 为什么需要 Git

没有 Git 的痛点：
- 改了 10 处代码发现有问题，想回退到昨天——**回不去**
- 想试一个新功能但又怕搞坏主代码——**不敢试**
- 硬盘坏了/重装系统——**全部丢失**
- 想和别人合作——**没法合并**

Git = 时光机 + 云备份 + 协作工具。

## 1.2 安装 Git

1. 打开 https://git-scm.com/download/win
2. 下载 64-bit Git for Windows Setup
3. 安装时全部用默认选项，一直 Next
4. 装完**关闭并重开 PowerShell**，验证：

```powershell
git --version
```

看到 `git version 2.x.x` 即可。

## 1.3 配置身份

每个 Git 提交都会记录"是谁改的"，需要先告诉它你是谁。

```powershell
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"
```

⚠️ 邮箱如果将来要传 GitHub，建议**用 GitHub 账号的邮箱**。但即使是私人邮箱也没事，提交历史里只显示邮箱地址。

## 1.4 三个核心概念

- **工作区（Working Tree）**：你电脑上的文件，蓝色（VS Code 左下角）
- **暂存区（Stage）**：准备提交的文件清单
- **仓库（Repository / Repo）**：所有历史版本的数据库，存放在 `.git/` 文件夹里

工作流：
```
修改文件 → git add（暂存）→ git commit（提交到本地仓库）→ git push（推到 GitHub）
```

## 1.5 第一次初始化

进入项目目录：

```powershell
cd C:\Users\16210\Documents\pvp-optimize
git init
```

会创建一个隐藏的 `.git/` 文件夹，包含所有版本数据。

## 1.6 .gitignore（关键！）

Git 默认会追踪**所有文件**，包括 `build/` 目录里的编译产物（占空间）和 `local-libs/` 里的 jar（重复）。

新建文件 `.gitignore`（项目根目录）：

```gitignore
# Gradle 编译产物
build/
.gradle/
out/

# IDE 配置
.idea/
*.iml
.vscode/
.classpath
.project
.settings/

# 本地 jar（从 Modrinth 下载，应当本地装而不是传仓库）
local-libs/

# 系统文件
.DS_Store
Thumbs.db
*.log

# Java 编译产物
*.class
```

## 1.7 第一次提交

```powershell
cd C:\Users\16210\Documents\pvp-optimize
git add .
git commit -m "Initial commit: PvP-Optimize v1.0.0"
```

`git add .` 表示"把当前目录所有文件加到暂存区"（除了 `.gitignore` 排除的）。
`git commit -m "..."` 表示"把暂存区的所有内容提交成一个版本"。

## 1.8 查看历史

```powershell
git log          # 列出所有提交（按时间倒序）
git log --oneline  # 一行显示
git status       # 当前哪些文件被改了
git diff         # 改了哪些行
```

## 1.9 推送到 GitHub

1. 打开 https://github.com/new （登录后）
2. Repository name: `pvp-optimize`
3. Description: `A Fabric mod for Minecraft that filters particles, culls far entities, and adds a configurable HUD overlay.`
4. **Public**（开源就选这个）
5. **不要**勾选 Add a README file（我们手动建）
6. **不要**勾选 Add .gitignore（我们手动建）
7. 点 Create repository
8. 看到 "Quick setup" 页面，复制 HTTPS URL（类似 `https://github.com/你的用户名/pvp-optimize.git`）

回到 PowerShell：

```powershell
git remote add origin https://github.com/你的用户名/pvp-optimize.git
git branch -M main        # 把默认分支重命名为 main
git push -u origin main   # 第一次推送需要 -u
```

`git push` 会让你登录 GitHub。Windows 会弹窗让你用浏览器授权。

## 1.10 日常三步走

改完代码后：

```powershell
git status              # 看改了哪些文件
git diff                # 看具体改了什么
git add .               # 全部暂存（或者 git add 文件名 只加特定文件）
git commit -m "描述改了什么"
git push                # 推送到 GitHub
```

写好 commit message 的习惯（强烈推荐）：

```
[类型] 简短描述

详细说明（可选）

类型可以是：
  feat: 新功能
  fix:  修 bug
  refactor: 重构
  docs:  文档
  chore: 杂项
  style: 格式调整
```

例子：
```
feat: add dragon breath particle filter

- Add `keepDragonBreath` config option
- Filter `DragonBreathParticle` in ParticleFilter
- Add zh_cn and en_us translations
```

## 1.11 后悔药

| 想做的事 | 命令 |
|---|---|
| 撤销还没 add 的修改 | `git checkout -- 文件名` |
| 撤销已经 add 但还没 commit | `git restore --staged 文件名` |
| 撤销最近一次 commit（保留修改） | `git reset --soft HEAD~1` |
| 撤销最近一次 commit（丢弃修改） | `git reset --hard HEAD~1` ⚠️ 危险 |
| 回到某个历史版本 | `git checkout 提交ID` |
| 从某个历史版本回来 | `git checkout main` |

## 1.12 灾难恢复

只要你的代码 push 到了 GitHub，**永远不会丢**：
- 硬盘坏了？克隆到新电脑：`git clone https://github.com/你/pvp-optimize.git`
- 改坏了？`git log` 找到上一个好的版本，`git checkout` 回去
- 误删了？`git status` 看删除的文件，`git checkout -- 文件名` 恢复

## 1.13 动手做

✅ **任务 1.1**：装好 Git，配置 user.name 和 user.email。
✅ **任务 1.2**：在 `pvp-optimize` 目录跑 `git init` 并创建 `.gitignore`。
✅ **任务 1.3**：跑第一次 `git add .` 和 `git commit -m "Initial commit"`。
✅ **任务 1.4**：在 GitHub 创建空仓库，push 上去。
✅ **任务 1.5**：故意改一个文件，跑 `git status` 和 `git diff` 看效果。
✅ **任务 1.6**：跑 `git add . && git commit -m "test" && git push` 看是否能正常推上去。

---

# 第 2 章 Fabric 1.21+ Mojang 映射迁移

> Minecraft 1.21.2 起，Mojang 官方公开了混淆映射表（官方叫"official"），社区的 yarn 映射表停止更新。Fabric 1.21.2+ 推荐改用 **Mojang 官方映射**或新的 **intermediary + Loom 自带映射**方案。

## 2.1 背景

- 1.20.6 及之前：Mojang 用商业混淆（`class_638`、`method_1234`），社区维护 yarn 把它们翻译成人话
- 1.21.2 起：Mojang 改用开放混淆，直接在 jar 里放映射文件，可以直接读人话版类名
- 1.21.x 期间 yarn 仍可用但已不再更新
- 1.22+：yarn 全面废弃

## 2.2 迁移路径

**方案 A：用 yarn 1.21.x**（最简单，仅支持到 1.21.4 左右）
**方案 B：用 Mojang 官方映射**（推荐，1.21.2+）

我们这里只讲**方案 A** 因为改动最小；如果你想用方案 B 查 Fabric 官方迁移文档 https://fabricmc.net/develop/

## 2.3 改 `gradle.properties`

```diff
-minecraft_version=1.20.6
-yarn_mappings=1.20.6+build.1
-loader_version=0.16.5
+minecraft_version=1.21.4
+yarn_mappings=1.21.4+build.1
+loader_version=0.16.5
-fabric_version=0.100.8+1.20.6
+fabric_version=0.110.0+1.21.4
```

每个版本号去哪里查：
- minecraft_version / yarn_mappings：https://maven.fabricmc.net/net/fabricmc/yarn/
- loader_version：https://fabricmc.net/wiki/loader/latest
- fabric_version：https://modrinth.com/mod/fabric-api/versions

## 2.4 改 `build.gradle`

Loom 版本要选 1.7+（支持 1.21）：

```diff
-    id ''fabric-loom'' version ''1.6-SNAPSHOT''
+    id ''fabric-loom'' version ''1.8-SNAPSHOT''
```

## 2.5 改 `local-libs/`

- 下载新版本 modmenu 和 cloth-config jar
- 重新跑 `UPDATE_GUIDE.md` §3.3 的 strip 流程

## 2.6 改源码（按概率排序）

| 改动点 | 1.20.6 → 1.21.4 影响 | 修法 |
|---|---|---|
| `KeyBindingHelper` 导入 | 没变 | 不用动 |
| `Particle` / `ParticleManager` | 类名没变，但 `buildGeometry` 签名可能变 | 看编译错误 |
| `Entity` / `PlayerEntity` / `VillagerEntity` | 类名没变 | 不用动 |
| `ItemEntity` | 类名没变 | 不用动 |
| `Text.translatable` | 没变 | 不用动 |
| `DrawContext.fill(int, int, int, int, int)` | 没变 | 不用动 |
| `MinecraftClient.getInstance()` | 没变 | 不用动 |
| 一些 `SoundEvent`、`Identifier` | 构造方式可能改 | 看编译错误 |
| 自定义数据包（packet）API | 1.21 重写过 | 大改 |

## 2.7 实际迁移步骤

```powershell
# 1. 备份
git add .
git commit -m "chore: pre-upgrade snapshot"
git push

# 2. 改版本号（见 2.3）
# 编辑 gradle.properties

# 3. 改 build.gradle
# 编辑 build.gradle 的 fabric-loom 版本

# 4. 换 local-libs
# 删旧 jar，从 Modrinth 下载新 jar 放入

# 5. strip manifest
# 参考 UPDATE_GUIDE.md §3.3

# 6. 试编译
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build -x test --no-daemon

# 7. 看红色错误，逐个改源码
```

最常见错误及修法：

```
错误: cannot find symbol class ClientTickEvents
解决: import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
     （大部分 import 不变，但有些要查新 Fabric API 文档）
```

## 2.8 验证

跑完 `gradlew build` 没报错后，把 `build/libs/pvp-optimize-X.X.X.jar` 复制到 mods 目录启动游戏。日志里看到 `[PvP-Optimize] Initialized` 就成功。

## 2.9 动手做

✅ **任务 2.1**：查 https://maven.fabricmc.net/net/fabricmc/yarn/ 找到 1.21.4 的最新 build 号。
✅ **任务 2.2**：（不真升级）把 `gradle.properties` 里的版本号改成 1.21.4 试编译，看有多少错。
✅ **任务 2.3**：改回去（这是练习）。

---

# 第 3 章 Modrinth / CurseForge 发布

> 让全世界的玩家用你的 mod。

## 3.1 注册 Modrinth 账号

1. 打开 https://modrinth.com/
2. 点右上角 "Sign Up"
3. 用 GitHub 账号登录（推荐，省事）

## 3.2 准备 mod 元数据

发布前需要准备：
- **mod logo**（推荐 256x256 PNG，正方形）
- **至少 3 张截图**（推荐 1280x720，展示各功能）
- **英文描述**（250 字符以内简短版 + 长版）
- **中文描述**（如果你会双语）
- **许可证声明**（我们用 MIT）

## 3.3 准备 jar

发布**不要**用 `build/libs/pvp-optimize-1.0.0.jar`！那是 dev jar。生产 jar 需要：

```powershell
.\gradlew.bat clean build -x test --no-daemon
```

确保 `build/libs/` 下只有 1 个 jar（不是 sources jar）。

## 3.4 创建项目

1. 登录 Modrith
2. 点右上角头像 → "Create a project"
3. 填写：
   - Project name: `PvP Optimize`
   - Project URL: `pvp-optimize`（这会变成 modrinth.com/mod/pvp-optimize）
   - Summary: `A Fabric mod that filters particles, culls far entities, and adds a configurable HUD overlay for PvP.`
   - Type: **Mod**
   - Categories: **Performance** + **Utility**
   - License: **MIT**
4. 点 Create

## 3.5 配置项目

进入项目管理后台，需要填写：

#### 侧边栏 (Sidebar)

格式：Markdown。下面是模板：

````markdown
# PvP Optimize

A lightweight Fabric mod for Minecraft 1.20.6 that:
- Filters out decorative particles, keeping only attack crits and damage hearts
- Culls entities beyond 16 blocks from the player (item frames, armor stands, etc.)
- Adds a configurable red full-screen overlay (toggleable with J key)
- Comes with a Chinese/English Mod Menu config UI

## Features

![screenshot](https://cdn.modrinth.com/data/xxx/screenshots/main.png)

### Particle Filter
Keeps only:
- ✨ Crit particles (cyan stars)
- ❤ Damage hearts (red)
- ✨ Potion effect particles
- ✨ XP orb particles

### Entity Culling
Automatically removes entities farther than 16 blocks:
- Item frames
- Armor stands
- Paintings
- Hostile mobs
- Passive animals

### HUD Overlay
- Red full-screen filter (J to toggle)
- Status panel (H to toggle)

## Configuration
Open **Mods → PvP Optimize → Config** in the Mods menu.

## Compatibility
- Minecraft 1.20.6
- Fabric Loader 0.16+
- Mod Menu 10.0+ (required for GUI)
- Cloth Config 14.0+ (required for GUI)
````

#### Description（详细）

````markdown
PvP Optimize is a performance-oriented mod designed for PvP servers. It blocks non-essential particles and culls far entities, reducing GPU/CPU load during combat.

**Particle Filter**
By default, only the following particles are shown:
- Crit (cyan star)
- Damage heart (red)
- Potion effect (heart-shape)
- XP orb (green dot)

You can toggle each category in the config.

**Entity Culling**
When an entity (other than players, villagers, projectiles, or smelted mineral drops) moves more than 16 blocks away from you, it is automatically removed. This includes item frames, armor stands, paintings, and most mobs.

**HUD Overlay**
A semi-transparent red overlay covers the screen, similar to the vanilla hurt overlay. Useful for indicating you're in a fight. Press J to toggle.

**Status Panel**
Press H to open a small status panel in the top-left showing current settings.

## License
MIT
````

## 3.6 上传版本

1. 进入项目 → Versions → "Create version"
2. Version number: `1.0.0`
3. Version title: `Initial release`
4. Minecraft versions: 勾选 `1.20.6`
5. Loaders: 勾选 `fabric`
6. Release channel: **Release**
7. Mod loader: **fabric**
8. Game versions: `1.20.6`
9. 上传 `build/libs/pvp-optimize-1.0.0.jar`
10. Changelog:

````markdown
## Initial Release

- Particle filter (crit / damage / potion / XP)
- Entity culling beyond 16 blocks
- Red HUD overlay (J key toggle)
- Mod Menu integration with Chinese UI
- JSON configuration file
````

11. Submit for review

## 3.7 审核

Modrinth 审核通常 1-3 天。通过后会出现在 https://modrinth.com/mod/pvp-optimize

## 3.8 CurseForge（可选）

如果想覆盖更广的受众（特别是 1.7.10 等老版本玩家），可以同步发到 CurseForge。流程类似，网址 https://www.curseforge.com/minecraft/mc-mods

⚠️ 但 CurseForge 现在必须用它的 API 上传，不能网页直接操作。**新手先发 Modrinth 就够了**。

## 3.9 维护更新

每次发布新版：
1. 改 `gradle.properties` 的 `mod_version=1.0.1`
2. 改 `fabric.mod.json` 的 changelog（如果有）
3. 跑构建
4. 在 Modrith 创建新 version
5. 写 changelog

---

# 第 4 章 GitHub 开源全流程

> 开源不是"把代码 push 上去就完事"，还要让社区用得舒服、贡献得进来。

## 4.1 开源前自检清单

在执行下面任何步骤前，先回答这 10 个问题：

| 问题 | 我们的 mod | 行动 |
|---|---|---|
| 1. 许可证是？ | MIT（已设置） | ✅ |
| 2. 有没有把不该传的东西传上去？ | 没有密码、token、邮箱 | ✅ |
| 3. 有没有 commit 历史里的脏数据？ | （初次 push，无问题） | ✅ |
| 4. 仓库描述清晰吗？ | 需要写 | 📝 |
| 5. 有没有 README？ | 需要写 | 📝 |
| 6. 有没有 .gitignore？ | 需要写 | 📝 |
| 7. 有没有截图？ | 需要 | 📝 |
| 8. 提交者邮箱要公开吗？ | 用 `noreply@github.com` 保护隐私 | 📝 |
| 9. 接受贡献吗？ | 建议接受 | 📝 |
| 10. 有 Discord/QQ 群吗？ | 可选 | 💭 |

## 4.2 仓库命名

✅ 好名字：
- `pvp-optimize`
- `pvp-optimize-fabric`
- `fabric-pvp-optimize`

❌ 坏名字：
- `my-mod-v2-final-FINAL`（带版本号）
- `PVP-Optimize-FABRIC-1.20.6`（带版本号 + 大小写混乱）
- `pvp_optimize_java_gradle_minecraft`（过长）

## 4.3 私有邮箱保护

GitHub 默认会展示你的邮箱。要用假邮箱（推荐）：

1. 打开 https://github.com/settings/emails
2. 找到 "Primary email address"
3. 勾选 "Keep my email addresses private"（使用 `noreply@github.com`）
4. 重新设置本地 Git：
```powershell
git config --global user.email "你的ID+noreply@users.noreply.github.com"
```
（把"你的ID"替换成 GitHub 用户名）

⚠️ 如果你之前的 commit 已经用了真邮箱，要清空：
```powershell
# 找到所有用了真邮箱的 commit 并改掉
# 注意：会改写历史，需要 force push
git filter-branch --env-filter '
OLD_EMAIL="旧邮箱@example.com"
CORRECT_NAME="你的名字"
CORRECT_EMAIL="新邮箱@users.noreply.github.com"
if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL" ]
then
    export GIT_COMMITTER_NAME="$CORRECT_NAME"
    export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
fi
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL" ]
then
    export GIT_AUTHOR_NAME="$CORRECT_NAME"
    export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
fi
' --tag-name-filter cat -- --branches --tags
git push --force
```

## 4.4 README 模板

新建 `README.md`（仓库首页）：

````markdown
# PvP Optimize

> A lightweight Fabric mod for Minecraft 1.20.6 that filters particles, culls far entities, and adds a configurable HUD overlay for PvP.

![GitHub release](https://img.shields.io/github/v/release/你的用户名/pvp-optimize?style=for-the-badge)
![License](https://img.shields.io/github/license/你的用户名/pvp-optimize?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6-brightgreen?style=for-the-badge)

[English](#english) | [中文](#中文)

## English

### Features
- **Particle filter** - keeps only crits, damage hearts, potion effects, and XP orbs
- **Entity culling** - removes non-essential entities beyond 16 blocks
- **HUD overlay** - semi-transparent red filter, toggle with `J` key
- **Status panel** - press `H` to view current settings
- **Mod Menu integration** - graphical config in `Mods → PvP Optimize → Config`
- **Chinese + English** - all UI is translated

### Installation
1. Install [Fabric Loader](https://fabricmc.net/) 0.16+
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) 0.100+
3. Install [Mod Menu](https://modrinth.com/mod/modmenu) 10.0+
4. Install [Cloth Config](https://modrinth.com/mod/cloth-config) 14.0+
5. Download the latest [release](https://github.com/你的用户名/pvp-optimize/releases)
6. Drop the `.jar` into your `mods/` folder

### Screenshots
![Particle filter](docs/screenshots/particles.png)
![Entity culling](docs/screenshots/culling.png)
![Config screen](docs/screenshots/config.png)

### Default Keybindings
- `H` - Toggle status panel
- `K` - Toggle particle filter
- `Y` - Toggle entity culling
- `J` - Toggle red overlay

### License
MIT License. See [LICENSE](LICENSE) for details.

## 中文

### 功能
- **粒子过滤** - 只保留暴击星、伤害红心、药水效果和经验球粒子
- **实体剔除** - 距离玩家 16 格以外的展示框、盔甲架、画、敌对生物等自动移除
- **屏幕滤镜** - 半透明红色全屏覆盖，按 `J` 切换
- **状态面板** - 按 `H` 查看当前配置
- **Mod Menu 集成** - 图形化配置入口：`Mods → PvP Optimize → Config`
- **中英双语** - 全部界面已翻译

### 安装
1. 安装 [Fabric Loader](https://fabricmc.net/) 0.16+
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 0.100+
3. 安装 [Mod Menu](https://modrinth.com/mod/modmenu) 10.0+
4. 安装 [Cloth Config](https://modrinth.com/mod/cloth-config) 14.0+
5. 下载最新 [Release](https://github.com/你的用户名/pvp-optimize/releases)
6. 把 `.jar` 放进 `mods/` 文件夹

### 截图
（同上）

### 默认按键
- `H` - 状态面板
- `K` - 粒子过滤
- `Y` - 实体剔除
- `J` - 红色滤镜

### 许可证
MIT 许可证。详见 [LICENSE](LICENSE)。
````

## 4.5 LICENSE

我们用 MIT 许可证（最宽松）。新建 `LICENSE`：

```
MIT License

Copyright (c) 2026 你的名字

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 4.6 贡献指南（CONTRIBUTING.md）

新建 `CONTRIBUTING.md`：

````markdown
# Contributing to PvP Optimize

Thanks for your interest! Here's how to contribute.

## Reporting Bugs

Open an issue at https://github.com/你的用户名/pvp-optimize/issues with:
- Minecraft version
- Mod version
- Other mods installed (especially optimization mods)
- `logs/latest.log` attached
- Steps to reproduce

## Suggesting Features

Open an issue with the `enhancement` label. Please describe:
- What the feature does
- Why it would be useful
- Any examples in other mods (with credit)

## Submitting Code Changes

1. Fork the repository
2. Create a branch: `git checkout -b my-feature`
3. Make your changes
4. Test in Minecraft
5. Commit: `git commit -m "feat: my feature"`
6. Push: `git push origin my-feature`
7. Open a Pull Request

### Code Style
- Use 4-space indentation (no tabs)
- Follow existing naming conventions
- Add comments for non-obvious logic
- Test before submitting

### Code of Conduct
Be respectful. No harassment. No spam.
````

## 4.7 Issue 模板

在 GitHub 仓库 → Settings → Features → Issues → "Set up templates" → 选 "Bug report" 和 "Feature request"。

**Bug report** 模板（`.github/ISSUE_TEMPLATE/bug.yml`）：

```yaml
name: Bug Report
description: Report a problem with PvP Optimize
labels: [bug]
body:
  - type: dropdown
    id: mc-version
    attributes:
      label: Minecraft version
      options:
        - 1.20.6
  - type: input
    id: mod-version
    attributes:
      label: Mod version
  - type: textarea
    id: description
    attributes:
      label: Description
  - type: textarea
    id: log
    attributes:
      label: latest.log (paste relevant section)
  - type: textarea
    id: steps
    attributes:
      label: Steps to reproduce
```

**Feature request** 模板（`.github/ISSUE_TEMPLATE/feature.yml`）：

```yaml
name: Feature Request
description: Suggest a new feature
labels: [enhancement]
body:
  - type: textarea
    id: description
    attributes:
      label: What feature do you want?
  - type: textarea
    id: why
    attributes:
      label: Why is it useful?
```

## 4.8 GitHub Actions（自动构建）

在 `.github/workflows/build.yml`：

```yaml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: ''zulu''
          java-version: ''21''
      - uses: gradle/actions/setup-gradle@v3
        with:
          gradle-home-cache-cleanup: true
      - run: ./gradlew build -x test
      - uses: actions/upload-artifact@v4
        with:
          name: pvp-optimize
          path: build/libs/*.jar
```

这样每次你 push 代码，GitHub 都会自动帮你编译一遍。如果有人 PR 进来，GitHub 也会自动跑编译，编译通过才能合并。

⚠️ 我们的项目用了 `local-libs/` 离线 jar，GitHub Actions 上没这些 jar。要在 workflow 里加：

```yaml
      - name: Download local jars
        run: |
          mkdir -p local-libs
          # 这里需要从 GitHub Releases 下载 jar
          # 或者上传到 workflow 仓库的某个地方
```

更简单的办法：把 modmenu 和 cloth-config 改成从 Modrinth 下载（修改 build.gradle），不依赖 local-libs。

## 4.9 徽章（Badges）

在 README 顶部加徽章让仓库"看起来专业"：

```markdown
![GitHub release](https://img.shields.io/github/v/release/用户名/仓库名)
![License](https://img.shields.io/github/license/用户名/仓库名)
![Downloads](https://img.shields.io/github/downloads/用户名/仓库名/total)
![Build status](https://img.shields.io/github/actions/workflow/status/用户名/仓库名/build.yml)
![Stars](https://img.shields.io/github/stars/用户名/仓库名)
```

## 4.10 不应该传的东西

**绝对不要**传：
- 任何 .jar（除非是 mod 本身，且只放 release 用的那个）
- 你的 Minecraft 账户信息
- 任何 token / API key
- 你自己的 log 文件（可能含敏感信息）
- 截图里的聊天内容（可能含服务器信息）

**可以传**：
- 源码 `.java`、`.gradle`、`.json`
- 文档 `.md`
- 图标 `.png`
- 经过审核的截图

## 4.11 完整文件清单

开源仓库最终应该长这样：

```
pvp-optimize/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug.yml
│   │   └── feature.yml
│   └── workflows/
│       └── build.yml
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradle/
│   └── wrapper/
├── gradlew
├── gradlew.bat
├── LICENSE                       ← 新增
├── README.md                     ← 新增
├── UPDATE_GUIDE.md               ← 可选
├── LEARN_TO_CODE.md              ← 可选
├── RELEASE_GUIDE.md              ← 新增
├── CONTRIBUTING.md               ← 新增
├── src/
│   └── main/
│       ├── java/
│       └── resources/
└── docs/                          ← 新增
    └── screenshots/
        ├── particles.png
        ├── culling.png
        └── config.png
```

可选文件夹 `docs/` 放截图。

## 4.12 隐私保护清单

发布前最后一遍扫：
- [ ] `git log` 里没有真邮箱
- [ ] 没有 `.env` / `application.yml` / 任何 token 文件
- [ ] 没有 `userdata/`、`logs/` 等
- [ ] 截图里的人名/聊天被涂掉
- [ ] `.gitignore` 写好
- [ ] `local-libs/` 在 `.gitignore` 里

---

# 附录 A Git 速查

| 命令 | 干什么 |
|---|---|
| `git init` | 把当前目录初始化为 Git 仓库 |
| `git clone URL` | 从远端克隆 |
| `git status` | 看哪些文件被改了 |
| `git add 文件` | 把文件加到暂存区 |
| `git add .` | 加全部 |
| `git commit -m "msg"` | 提交 |
| `git log` | 看历史 |
| `git diff` | 看未暂存的修改 |
| `git diff --staged` | 看已暂存但未提交的 |
| `git push` | 推送到远端 |
| `git pull` | 拉取远端更新 |
| `git branch` | 看所有分支 |
| `git branch 新分支` | 创建分支 |
| `git checkout 分支` | 切换分支 |
| `git merge 分支` | 合并分支到当前 |
| `git stash` | 暂存当前修改（不提交） |
| `git stash pop` | 恢复暂存 |
| `git remote -v` | 看远端地址 |
| `git tag v1.0.0` | 打标签（标记发布版本） |

---

# 附录 B Modrinth 项目元数据示例

我们的 mod 在 Modrinth 上的元数据应该长这样：

```json
{
  "id": "pvp-optimize",
  "slug": "pvp-optimize",
  "name": "PvP Optimize",
  "summary": "Fabric mod for Minecraft that filters particles, culls far entities, and adds a configurable HUD overlay.",
  "description": "...",
  "categories": ["performance", "utility"],
  "client_side": "required",
  "server_side": "unsupported",
  "license": {
    "id": "MIT",
    "name": "MIT License"
  },
  "loaders": ["fabric"],
  "versions": ["1.20.6"],
  "donation_urls": [],
  "issues_url": "https://github.com/用户名/pvp-optimize/issues",
  "source_url": "https://github.com/用户名/pvp-optimize",
  "wiki_url": "https://github.com/用户名/pvp-optimize/wiki",
  "discord_url": null
}
```

---

# 附录 C GitHub 仓库必备文件清单

发布前 checklist：

- [ ] `README.md` - 项目说明
- [ ] `LICENSE` - 许可证（MIT 文本）
- [ ] `.gitignore` - 排除 build/、.gradle/、local-libs/ 等
- [ ] `.github/ISSUE_TEMPLATE/bug.yml` - bug 报告模板
- [ ] `.github/ISSUE_TEMPLATE/feature.yml` - 功能请求模板
- [ ] `.github/workflows/build.yml` - 自动构建（可选）
- [ ] `docs/screenshots/` - 至少 3 张截图
- [ ] `CONTRIBUTING.md` - 贡献指南（可选）
- [ ] `CHANGELOG.md` - 更新日志（可选但推荐）
- [ ] GitHub Repo Settings:
  - [ ] Description 填好
  - [ ] Website 填 Modrinth 链接
  - [ ] Topics 填 `minecraft`、`fabric`、`mod`、`performance`
  - [ ] Releases 已发布至少一个
  - [ ] Issues 已启用

---

# 后记

学完这份手册你应该能：
- ✅ 用 Git 备份代码到 GitHub，永不丢失
- ✅ 安全地把代码开源给全世界
- ✅ 跨大版本升级到 1.21+
- ✅ 发布到 Modrith 让玩家下载
- ✅ 接受社区的 bug 报告和代码贡献
- ✅ 保护自己的隐私

**下一步**：
1. 立即备份：跑第 1 章的 Git 流程
2. 慢慢打磨：补 README、LICENSE、CONTRIBUTING.md
3. 发布公测：Modrith 提交审核
4. 接收反馈：处理 Issue 和 PR
5. 持续迭代：每 1-2 周发一个小版本

**祝你开源顺利！**