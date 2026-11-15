# PvP Optimize

面向 PvP 场景的 Fabric 客户端优化 mod（Minecraft 1.20.6），带一套自绘界面、药水 HUD、可自由摆放的 HUD 布局，以及内置本地/在线音乐播放器与歌词显示。

## 功能

**性能**

- 粒子剔除：按类型/距离/数量上限过滤，保留暴击与伤害粒子
- 实体剔除：远处实体、无装备实体、群体中的冗余实体不渲染
- 自适应画质：按实测帧时间分档收缩渲染距离与粒子预算
- 纹理动画降频、音效源精简、内存压力告警

**界面**

- 类 HachimiLocal 的轻量主控板（无 GPU 后处理，背景仅三条纯色横带）
- 药水 HUD：剩余时间、图标、颜色分级，可改位置/缩放/不透明度
- 自定义 HUD 布局：药水与歌词面板均可拖动、滚轮缩放、右键复位（Badlion 式）

**音乐**

- 本地播放器：mp3 / wav / ogg，支持 LRC 歌词（UTF-8 / GBK）
- 在线搜索与下载
- 接入 DangoMusic 的灵动岛显示曲目、封面与进度
- 歌词面板：当前行高亮 + 相邻行淡出 + 行内进度，背景色/不透明度/圆角均可调

## 安装

1. 安装 Fabric Loader（>= 0.16.0）与 Minecraft 1.20.6
2. 把 jar 放进 `.minecraft/mods/`
3. 需要 Fabric API；可选 Mod Menu、Cloth Config（装了才有图形配置界面）

## 音乐目录

默认是**当前游戏实例**下的 `config/pvp_optimize/music`，首次启动自动创建。
各玩各的实例、各算各的路径，换盘符或换启动器都不影响。

想把歌库放到别处（比如 `D:/Music/mc`），在同目录的 `config/pvp_optimize.json` 里填：

```json
"musicDir": "D:/Music/mc"
```

留空即用默认目录；相对路径相对游戏目录解析。音乐页有「打开音乐文件夹」按钮。

## 构建

需要 JDK 17+。仓库**不包含**下列二进制依赖，需要自行放入 `local-libs/`（该目录已在 `.gitignore` 中）：

```
local-libs/
├── modmenu-10.0.0.jar
├── cloth-config-fabric-14.0.139.jar
├── jlayer-1.0.1.4.jar
├── dangomusic-ui-1.7.5.jar        # DangoMusic 1.7.5（GPL-3.0），需自行取得
└── DangoMusic-GPL-3.0.txt         # 随 jar 分发的许可全文
```

```bash
./gradlew build
```

产物在 `build/libs/`。

## 许可

本项目以 **GPL-3.0-or-later** 发布，全文见 [LICENSE](LICENSE)。

发布的 jar 内置了 DangoMusic（`me.sorihiko.dango`，版本 1.7.5）的字节码用于灵动岛与歌词渲染，它本身以 GPL-3.0 授权，因此整体按 GPL-3.0 交付。同时打包的 ZXing、Kotlin 标准库（Apache-2.0）、JLayer、JOrbis（LGPL）、Inter 字体（OFL）各自保留原许可，详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

致谢 DangoMusic 的作者 sorihiko。

本项目与 Mojang Studios 及 Microsoft 无关联。
