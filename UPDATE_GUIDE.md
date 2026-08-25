# PvP-Optimize 维护手册

> 适用版本：Minecraft 1.20.6 + Fabric Loader 0.16.x
> 项目路径：`C:\Users\16210\Documents\pvp-optimize`
> 输出 jar：`build/libs/pvp-optimize-1.0.0.jar`
> 部署路径：`C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\mods\`

---

## 0. 项目结构速查

```
pvp-optimize/
├── build.gradle                     # 构建脚本：插件、依赖、Java 版本
├── gradle.properties                # 版本号集中地（MC、yarn、loader、fabric、modmenu、cloth）
├── gradle/wrapper/                  # Gradle 8.10.2 wrapper（首次构建必需）
├── gradlew.bat / gradlew            # Gradle 启动脚本
├── local-libs/                      # 离线 jar 依赖（modmenu、cloth-config）
│   ├── modmenu-10.0.0.jar
│   └── cloth-config-fabric-14.0.139.jar  # 注意：构建前要 strip 掉 Fabric-Loom-Version
└── src/main/
    ├── java/com/pvp/optimize/
    │   ├── PvPOptimize.java         # Mod 入口（onInitializeClient）
    │   ├── PvPOptimizeConfig.java   # 配置类（Data + JSON 持久化 + 键位）
    │   ├── particle/ParticleFilter.java     # 粒子过滤判定
    │   ├── entity/EntityFilter.java         # 实体剔除判定
    │   ├── mixin/ParticleManagerMixin.java  # 注入到 ParticleManager.renderParticles
    │   ├── mixin/EntityRenderDispatcherMixin.java  # 注入到 EntityRenderDispatcher.render
    │   ├── hud/OverlayHud.java      # 红色滤镜 + 状态面板
    │   └── config/
    │       ├── PvPOptimizeModMenu.java       # Mod Menu 入口
    │       └── PvPOptimizeConfigScreen.java  # Cloth Config 屏幕
    └── resources/
        ├── fabric.mod.json          # Mod 元数据
        ├── pvp_optimize.mixins.json # Mixin 加载清单
        └── assets/pvp_optimize/
            ├── icon.png
            └── lang/
                ├── zh_cn.json       # 中文翻译
                └── en_us.json       # 英文翻译
```

---

## 1. 同版本内的常规修改流程

> 场景：你想改某个粒子、键位、配置项、或者加功能。

### 1.1 改源码

直接编辑对应的 `.java` 文件。例如：
- 想多保留一种粒子 → 改 `ParticleFilter.java` 第 20-39 行
- 想让某个实体始终保留 → 改 `EntityFilter.java` 第 60-85 行
- 想改默认剔除半径 → 改 `PvPOptimizeConfig.java` 的 `Data.cullDistance = 16.0`
- 想加新配置项 → 同步改 `Data` 字段、`load()` 反序列化、`PvPOptimizeConfigScreen` 控件、`zh_cn.json` / `en_us.json` 翻译
- 想换默认键位 → 改 `PvPOptimizeConfig.register()` 里的 `InputUtil.Type.KEYSYM` / `GLFW.GLFW_KEY_X`

### 1.2 重新构建

打开 PowerShell：

```powershell
cd C:\Users\16210\Documents\pvp-optimize
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build -x test --no-daemon
```

构建产物：`build\libs\pvp-optimize-1.0.0.jar`

### 1.3 部署

```powershell
Copy-Item `
  -Path "C:\Users\16210\Documents\pvp-optimize\build\libs\pvp-optimize-1.0.0.jar" `
  -Destination "C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\mods\pvp-optimize-1.0.0.jar" `
  -Force
```

或者用资源管理器手动复制粘贴覆盖。

### 1.4 验证

启动游戏 → 主菜单应该有 `[PvP-Optimize] Initialized. Press H to toggle the status panel.` 日志。**没有这个日志 = 加载失败**，看 `latest.log` 排查。

---

## 2. 跨大版本升级（1.20.6 → 1.21.x）

> 这是最容易出错的环节。Minecraft 大版本升级会同时动：yarn 映射、Fabric API、Mod Menu、Cloth Config、Loom。

### 2.1 升级前清单

| 步骤 | 改的文件 | 改什么 |
|---|---|---|
| ① 查目标版本支持的组件 | （外部） | 查 Modrinth / Fabric 官方文档，确认目标 MC 版本对应的：yarn build 号、Fabric Loader 最低版本、Fabric API 版本、Mod Menu 版本、Cloth Config 版本、Loom 插件版本 |
| ② 改 `gradle.properties` | `gradle.properties` | 同步 `minecraft_version`、`yarn_mappings`、`loader_version`、`fabric_version`、`modmenu_version`、`cloth_config_version` |
| ③ 改 Loom 版本 | `build.gradle` 第 2 行 | `id ''fabric-loom'' version ''X.Y.Z''` 选 Loom 1.6.x（与 Fabric Loader 0.16.x 匹配）；如果用 Loom 1.7+，需要 Fabric Loader 0.17+ |
| ④ 换 local-libs 里的 jar | `local-libs/` | 从 Modrinth 下载匹配目标 MC 版本的 modmenu 和 cloth-config jar，覆盖进去 |
| ⑤ 修源码里过期的类名 | `*.java` | 编译报错时会列出来，对照 yarn 映射逐个改 |

### 2.2 典型的 1.20.6 → 1.21.x 改动点

按概率排序：

1. **Mojang 在 1.21.2+ 启用了 obfuscation 映射**，yarn 不再直接可用，要切到 [Mojmap](https://fabricmc.net/develop/) 或 [Intermediary](https://github.com/FabricMC/intermediary)。`build.gradle` 的 `mappings` 行要改：
   ```groovy
   mappings "net.fabricmc:yarn:..."  // 旧
   mappings "org.mojang:mojangmap:..." // 新（具体看 Fabric 官方教程）
   ```
2. **KeyBinding 注册**：1.21 把 `KeyBindingHelper` 从 fabric-api 移到了 fabric-key-binding 独立模块（如果用新版本 Fabric API，需要在 build.gradle 显式加 `modImplementation "net.fabricmc.fabric-api:fabric-key-binding-api:..."`）
3. **`DrawContext` 用法**：1.21.2+ 部分方法签名变了，比如 `fill(int, int, int, int, int)` 变成 `fill(RenderLayer, ...)`
4. **Mixin 目标类签名变动**：ParticleManager / EntityRenderDispatcher 的 render 方法在每版都微调，需要按新 yarn 重新匹配
5. **配置类 `Data` 字段类型**：`cullDistance` 原来是 `double`，新版 GUI 可能要求 `int` 或 slider 控件

### 2.3 升级后编译报错的处理

执行 §1.2 的构建命令，看 `> Task :compileJava FAILED` 下方的红色错误行。常见错误对应表：

| 报错片段 | 含义 | 修法 |
|---|---|---|
| `cannot find symbol: class XXX` | yarn 类名变了 | 查新 yarn 的同名类（功能通常未变，只是类名重命名） |
| `incompatible types: X cannot be converted to Y` | 字段/方法返回类型变了 | 改用新 API，比如 `int` 变 `long`、`Map<K,V>` 变 `Map<K,List<V>>` |
| `method does not override or implement a method from a supertype` | 父类方法签名变了 | 删 `@Override` 重写，或改参数列表 |
| `Mixin [...] FAILED during APPLY` | Mixin 目标方法不存在 | 在 IDE 里用 `find usages` 找新方法名，更新 `@At` / `@Inject` 里的 target |
| `ClassNotFoundException: ...` | 依赖缺失 | build.gradle 加 `modImplementation` |
| `InvalidInjectionException` | Mixin `@At` 描述符过期 | 按 mixin 报错里提示的"expected"签名改 |

---

## 3. 紧急情况处理

### 3.1 AI 罢工 / 付费

完全按本文件操作即可。本文件已经包含：
- 项目结构
- 所有源码的职责说明
- 构建命令
- 升级步骤
- 报错对照表

不需要 AI 也能继续维护。

### 3.2 Fabric / Maven Central 服务器崩了，无法下载依赖

Gradle 默认从 `https://repo1.maven.org/maven2/` 和 `https://maven.fabricmc.net/` 拉依赖。如果挂掉：

1. **构建时加 `--offline`** 强制使用本地缓存：
   ```powershell
   .\gradlew.bat build -x test --no-daemon --offline
   ```
2. **缓存位置**：`C:\Users\16210\.gradle\caches\modules-2\files-2.1\`。只要这台机器上跑过同类项目就有缓存。
3. **如果新项目需要新依赖而没缓存**：先在能联网的机器上把 jar 下下来放进 `local-libs/`，然后在 `build.gradle` 改用 `files(...)` 引用（参考 `build.gradle` 第 27 行）
4. **Gradle 自身下载不到**（wrapper 损坏）：从任何机器复制 `gradle/wrapper/gradle-wrapper.jar` 和 `gradle/wrapper/gradle-wrapper.properties` 过来；或者装系统级 Gradle（`choco install gradle`）

### 3.3 Loom 版本校验失败（`Mod was built with a newer version of Loom`）

原因是 `local-libs/` 里的 jar 自带 `Fabric-Loom-Version: 1.7.413` 这种 manifest 字段，但你的 Loom 是 1.6.x，会拒绝加载。

**手动 strip 字段**（PowerShell）：

```powershell
$jar = "C:\Users\16210\Documents\pvp-optimize\local-libs\<可疑jar名>.jar"
$tmp = $jar + ".tmp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$src = [System.IO.Compression.ZipFile]::OpenRead($jar)
$dst = [System.IO.Compression.ZipFile]::Open($tmp, [System.IO.Compression.ZipArchiveMode]::Create)
foreach ($e in $src.Entries) {
    $ne = $dst.CreateEntry($e.FullName)
    $is = $e.Open(); $os = $ne.Open()
    if ($e.FullName -eq ''META-INF/MANIFEST.MF'') {
        $r = New-Object System.IO.StreamReader($is)
        $m = $r.ReadToEnd(); $r.Dispose()
        $m = $m -replace "Fabric-Loom-Version:.*\r?\n", ""
        $b = [System.Text.Encoding]::UTF8.GetBytes($m)
        $os.Write($b, 0, $b.Length)
    } else { $is.CopyTo($os) }
    $is.Dispose(); $os.Dispose()
}
$src.Dispose(); $dst.Dispose()
Move-Item $tmp $jar -Force
```

### 3.4 找不到 Java 21 / Gradle wrapper 缺失

```powershell
# 查看可用 JDK
Get-ChildItem "C:\Program Files\Zulu" -Directory
# 查看系统 gradle（基本不会有）
where.exe gradle
```

如果 `zulu-21` 不存在：
- 装 [Zulu 21](https://www.azul.com/downloads/?package=jdk) → 解压到 `C:\Program Files\Zulu\zulu-21`
- 重新跑 §1.2 的构建命令

如果 `gradlew.bat` / `gradle/wrapper/` 缺失：
- 从任何能跑的项目复制 `gradlew.bat`、`gradlew`、`gradle/wrapper/gradle-wrapper.jar`、`gradle/wrapper/gradle-wrapper.properties` 四个文件到本项目根目录

### 3.5 Mixin 在游戏里运行时崩溃

1. 打开 `C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\logs\latest.log`
2. 搜 `pvp_optimize` 或 `Mixin`
3. 看崩溃栈顶部的 `Caused by:` 行，定位是哪个 mixin 出的问题
4. 常见原因：
   - **目标方法签名变了**（多半是大版本升级）→ 改 `@Inject` / `@Redirect` / `@At` 里的 target
   - **目标类被其他 mod 先重命名了**（冲突）→ 在 mixin config 里加 `priority` 或者 `compatibilityLevel` 字段
   - **目标方法被混淆了**（用了 intermediary 而不是 yarn）→ 在 `@At` 里改用 `target = "L<intermediary>;"`
5. 临时绕过：把 mixin config 里的对应 mixin 注释掉（用 `//` 开头），重新构建

### 3.6 整个 mods 目录崩了需要回退

1. 把现在 modpack 里的 `pvp-optimize-1.0.0.jar` 改名加后缀 `.disabled`
2. 重启游戏，看崩溃是否消失
3. 如果消失 = 确认是 pvp-optimize 的问题
4. 恢复时把 `.disabled` 删掉即可

---

## 4. 关键参考链接

- Fabric 官方文档：https://fabricmc.net/develop/
- Yarn 映射索引：https://maven.fabricmc.net/net/fabricmc/yarn/
- Fabric Loom GitHub：https://github.com/FabricMC/fabric-loom
- Mod Menu 仓库：https://github.com/TerraformersMC/ModMenu
- Cloth Config 仓库：https://github.com/shedaniel/cloth-config
- 玩家/服务器崩溃排错通用：https://fabricmc.net/wiki/players/troubleshooting

---

## 5. 版本历史

| 版本 | MC 版本 | 关键改动 |
|---|---|---|
| 1.0.0 | 1.20.6 | 初版：粒子过滤、实体剔除、红色滤镜、Mod Menu 集成、中文界面 |

---

## 6. 维护日志模板

每次改动后建议在这里记一行：

```
2026-08-25 16:31  v1.0.0
  - 分离 crit / damage 粒子独立开关
  - 移除 sweepattack、heartparticle 误匹配
  - 移除 fabric.mod.json 的 client badge（消除日志警告）
```