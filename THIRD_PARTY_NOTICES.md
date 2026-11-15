# 版权与第三方声明 / Copyright and Third-Party Notices

PvP Optimize
Copyright (C) 2026 ccbayxy

本项目的完整源代码发布于 <https://github.com/ccbayxy/pvp-optimize>。

---

## 1. 本项目许可 / Project License

本项目以 **GNU 通用公共许可证第 3 版（GPL-3.0-or-later）** 发布，全文见 [LICENSE](LICENSE)。

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

**为什么是 GPL-3.0：** 发布的 mod jar 中内置了 DangoMusic（`me.sorihiko.dango`）的字节码，
用于驱动灵动岛、音乐界面与歌词渲染。DangoMusic 本身以 GPL-3.0 授权，与它合并成同一个
可执行作品再分发时，整体必须按 GPL-3.0 交付。因此本项目**不是**"Apache-2.0 与 GPL-3.0 任选其一"
的双许可项目 —— 那样会给下游传递错误的授权信息。Apache-2.0 只适用于下列第三方组件。

## 2. 第三方组件 / Bundled Third-Party Works

发布的 jar 内含以下作品，各自保留其原有许可：

| 组件 | 版本 | 许可 | 许可全文 |
| --- | --- | --- | --- |
| DangoMusic (`me.sorihiko.dango`) | 1.7.5 | GPL-3.0 | [LICENSE](LICENSE) |
| ZXing core | — | Apache-2.0 | [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt) |
| Kotlin standard library | — | Apache-2.0 | [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt) |
| JLayer | 1.0.1.4 | LGPL-2.1 | jar 内 `META-INF/licenses/JLayer-LGPL-2.1.txt` |
| JOrbis (含 Jogg) | — | LGPL-2.0-or-later | jar 内 `META-INF/licenses/JOrbis-LGPL-2.1.txt` |
| Inter 字体（子集） | — | SIL Open Font License 1.1 | jar 内 `META-INF/licenses/Inter-OFL.txt` |

上述许可全文同时也随 jar 一起分发在 `META-INF/licenses/` 目录下。

### LGPL 组件的说明

JLayer 与 JOrbis 以 LGPL 授权，本项目**未修改**其源码，仅以类文件形式随 jar 打包。
LGPL 允许这种使用方式，条件是：

1. 保留其版权与许可声明（见上表与 jar 内 `META-INF/licenses/`）；
2. 允许使用者用修改后的版本替换该库 —— 因为它与本项目其余代码没有静态链接成不可分割的整体，
   使用者可以自行替换 jar 内对应的 `javazoom/**`、`org/jorbis/**` 类文件后重新打包。

如需这些库的原始源码，见 <https://www.javazoom.net/javalayer/javalayer.html> 与
<https://www.jcraft.com/jorbis/>。

## 3. 对应源码 / Corresponding Source

GPL-3.0 第 6 条要求分发目标代码时提供对应源码：

- 本项目源码：<https://github.com/ccbayxy/pvp-optimize>
- DangoMusic 源码：见其发布页面（jar 内 `META-INF/licenses/README.txt` 注明"see LICENSE in the source repository"）

## 4. 与 Minecraft 的关系 / Not affiliated with Mojang

本项目是独立的第三方模组，与 Mojang Studios 及 Microsoft 无关联，也未获得其背书。
"Minecraft" 是 Mojang Synergies AB 的商标。
