# 零基础 PvP-Optimize 编程教程

> 这是一份**面向完全没写过代码的人**的教程。
> 目标：读完之后你能独立修改 PvP-Optimize 的任何一行、升级到新 MC 版本、甚至从零自己写一个 Fabric Mod。
> 配套项目：`C:\Users\16210\Documents\pvp-optimize`

---

## 第 0 章 路线图

| 章节 | 你将学到 | 预计耗时 |
|---|---|---|
| 1 编程是什么 | 程序的本质、语言、工具链 | 20 分钟 |
| 2 搭建环境 | 装 Java、IDE、命令行 | 30 分钟 |
| 3 Java 语法 | 变量、循环、类、对象（本教程的核心） | 2-3 小时 |
| 4 Gradle 入门 | 构建工具、`build.gradle` 怎么读 | 20 分钟 |
| 5 Minecraft + Fabric 基础 | 游戏的程序结构、Mod 怎么挂上去 | 30 分钟 |
| 6 PvP-Optimize 代码精读 | 每个文件每段代码什么意思 | 1.5 小时 |
| 7 动手实战 | 改 5 个真实功能 | 1 小时 |
| 8 调试技巧 | 看日志、定位错误 | 30 分钟 |
| 9 进阶方向 | 1.21 升级、推荐资源 | 10 分钟 |

**学习建议**：每章末尾有"动手做"环节，**一定要亲手敲**一遍再往下。只看不敲 = 永远学不会。

---

## 第 1 章 编程是什么

### 1.1 程序是什么

程序 = **给计算机的指令清单**。

你日常用的 Minecraft 也是个程序。它本质上就是一个超长的 `.txt` 文本文件，里面写着几十万行"做什么"的指令。你按 W 键，它就知道"角色前进"；你点鼠标左键，它就知道"破坏面前的方块"。

我们这次要改的 PvP-Optimize 也是一个程序，但它只写了大约 700 行代码，远比 Minecraft 主体小，但同样能让游戏按我们的想法改变。

### 1.2 编程语言

人写"中文"给中国人看，写"英文"给英国人看。写代码也有语言：Java、Python、C++、JavaScript……

我们用 **Java**，原因是：
1. Minecraft 本体就是 Java 写的
2. Fabric Mod 必须用 Java
3. Java 学一次能用一辈子（Android、服务器后端、大数据都用它）

### 1.3 你需要的三种"工具"

| 工具 | 干什么 | 我们的项目里对应什么 |
|---|---|---|
| **JDK（Java Development Kit）** | 把你写的 `.java` 翻译成电脑能跑的 `.class` | `C:\Program Files\Zulu\zulu-21` |
| **IDE（编辑器）** | 写代码、跳转、查错、提示 | 后面会装 VS Code 或 IntelliJ |
| **Gradle（构建工具）** | 编译全部代码、打包成 `.jar` | 项目根目录的 `gradlew.bat` |

---

## 第 2 章 搭建开发环境

### 2.1 检查 Java 是否已装

按 `Win + R`，输入 `powershell`，回车。然后敲：

```powershell
java -version
```

看到类似 `openjdk version "21.x.x"` 就 OK。

如果提示"找不到 java"：
- 你之前装的是 `C:\Program Files\Zulu\zulu-21`，先确认这个目录存在
- 如果不存在，去 https://www.azul.com/downloads/?package=jdk 下载 Zulu 21，安装到那个目录
- 装完打开**新的** PowerShell 窗口再试

### 2.2 选一个 IDE

新手建议 **VS Code**（轻量、启动快）。老手建议 **IntelliJ IDEA Community**（补全强、Fabric 官方推荐）。

#### 方案 A：装 VS Code

1. 打开 https://code.visualstudio.com/ 下载安装
2. 装好后启动，按 `Ctrl+Shift+X` 打开扩展商店
3. 搜索安装这些扩展（一个一个搜）：
   - `Extension Pack for Java`（微软官方 Java 工具包，6 个扩展一键装好）
   - `Chinese (Simplified) Language Pack`（可选，中文界面）
   - `Gradle for Java`（Gradle 文件高亮）

#### 方案 B：装 IntelliJ IDEA

1. 打开 https://www.jetbrains.com/idea/download/ 下载 Community 版
2. 装好后启动，第一次会让你选"导入设置"→ 选 Do not import
3. 点 "Plugins" → 搜 "Minecraft Development" → 安装（提供 Fabric 项目模板）
4. 重启 IDEA

### 2.3 打开项目

**VS Code**：菜单 File → Open Folder → 选 `C:\Users\16210\Documents\pvp-optimize`

**IntelliJ**：菜单 Open → 选 `C:\Users\16210\Documents\pvp-optimize` 目录

第一次打开，VS Code 会自动开始配置 Java 工具链，等右下角进度条跑完。

### 2.4 验证环境

在 VS Code 里按 `Ctrl + 反引号（`` ` ``）`打开终端，输入：

```powershell
.\gradlew.bat --version
```

看到 Gradle 8.10.2 输出就成功了。

### 2.5 动手做

✅ **任务 2.1**：在 PowerShell 里跑 `java -version`，截图保存。
✅ **任务 2.2**：装好 VS Code 和 Java 扩展。
✅ **任务 2.3**：用 VS Code 打开 `pvp-optimize` 项目，能看到左边的文件树。

---

## 第 3 章 Java 语法（最重要的章节）

> 这一章很长，但全是干货。Java 语法就那么点东西，学会了你能读懂 90% 的 Minecraft Mod 代码。

### 3.1 你的第一个程序

用 VS Code 创建一个新文件 `Hello.java`（在桌面就行），输入：

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("你好，世界！");
    }
}
```

在文件管理器里，对着 `Hello.java` 按住 Shift + 右键 → "在此处打开 PowerShell"，输入：

```powershell
javac Hello.java
java Hello
```

你会看到输出 `你好，世界！`。

**这段代码每一行什么意思**：

```java
public class Hello {              // 声明一个"类"，名叫 Hello，public = 公开的
    public static void main(String[] args) {  // 主方法，程序从这里开始执行
        System.out.println("你好，世界！");     // 在控制台打印一行字
    }                              // main 方法结束
}                                  // Hello 类结束
```

**关键概念**：
- `class` = 类。Java 里所有代码都必须装在"类"里。
- `public` = 公开的（任何地方都能用）。还有 `private`（私有的，只能本类用）。
- `static` = 静态的（不依赖于"对象"，可直接通过类名调用）。
- `void` = 没有返回值。
- `String[] args` = 字符串数组，命令行参数（暂时用不到）。
- `System.out.println(...)` = 打印到控制台。

### 3.2 变量和数据类型

变量 = **存数据的盒子**。盒子里能放什么类型的东西，一开始就定好。

```java
public class VariableDemo {
    public static void main(String[] args) {
        // 基本类型（8 种）
        int a = 10;              // 整数，比如 10, -5, 0
        long b = 9999999999L;    // 长整数，末尾要 L
        float c = 3.14f;         // 单精度小数，末尾要 f
        double d = 3.14159;      // 双精度小数，最常用
        boolean e = true;        // 真或假
        char f = ''A'';           // 单个字符
        byte g = 100;            // 小整数
        short h = 1000;          // 中整数

        // 引用类型（对象）
        String name = "小明";     // 字符串
        int[] scores = {90, 85, 70};  // 数组

        System.out.println(name + "考了" + scores[0] + "分");
    }
}
```

**动手做**：在 `Hello.java` 旁边新建 `VariableDemo.java`，把上面代码粘进去，编译运行。

### 3.3 运算符

```java
int a = 10, b = 3;
System.out.println(a + b);   // 13 加
System.out.println(a - b);   // 7  减
System.out.println(a * b);   // 30 乘
System.out.println(a / b);   // 3  整除（注意！小数部分会丢）
System.out.println(a % b);   // 1  取余
System.out.println(a > b);   // true 大于
System.out.println(a == b);  // false 等于（注意是两个等号！）
System.out.println(a != b);  // true 不等于
System.out.println(a > 0 && b > 0);  // true 且
System.out.println(a > 0 || b < 0);  // true 或
System.out.println(!true);          // false 非
```

### 3.4 条件语句（if / else）

根据条件决定做什么。

```java
int score = 85;

if (score >= 90) {
    System.out.println("优秀");
} else if (score >= 80) {
    System.out.println("良好");
} else if (score >= 60) {
    System.out.println("及格");
} else {
    System.out.println("不及格");
}
```

### 3.5 循环

重复做同一件事。

```java
// for 循环：明确知道次数
for (int i = 1; i <= 5; i++) {
    System.out.println("第" + i + "次");
}

// while 循环：不知道次数
int n = 0;
while (n < 3) {
    System.out.println("n = " + n);
    n++;  // n += 1 的简写
}
```

### 3.6 函数（方法）

把一段逻辑打包，下次直接用。

```java
public class FunctionDemo {
    // 定义一个函数：输入两个数，返回大的那个
    static int max(int a, int b) {
        if (a > b) {
            return a;
        } else {
            return b;
        }
    }

    public static void main(String[] args) {
        int result = max(10, 20);
        System.out.println("较大值：" + result);  // 较大值：20
    }
}
```

**PvP-Optimize 里的真实例子** —— `hud/OverlayHud.java` 第 86 行：

```java
private static String onOff(boolean b) { return b ? "开" : "关"; }
```

- `private static` = 私有、静态
- `String` = 返回字符串
- `onOff` = 函数名
- `(boolean b)` = 接收一个布尔参数
- `b ? "开" : "关"` = 三元运算符，b 为真返回"开"，否则返回"关"

### 3.7 类和对象

类 = **模板**。对象 = **根据模板造的具体东西**。

打个比方：
- "汽车图纸" = 类
- "你家那辆红色 SUV" = 对象（按图纸造出来的实例）

```java
// 类（模板）
class Player {
    String name;       // 字段（属性）
    int hp;

    // 构造方法（new 时调用）
    Player(String name, int hp) {
        this.name = name;  // this.name 表示"当前对象的 name 字段"
        this.hp = hp;
    }

    // 方法（行为）
    void attack() {
        System.out.println(name + " 攻击！造成 5 伤害");
    }
}

public class ObjectDemo {
    public static void main(String[] args) {
        Player p1 = new Player("小明", 100);  // new 一个对象
        Player p2 = new Player("小红", 80);
        p1.attack();
        p2.attack();
        System.out.println(p1.name + " HP:" + p1.hp);
    }
}
```

**PvP-Optimize 里的真实例子** —— `PvPOptimizeConfig.java`：

```java
public static final class Data {
    public boolean particlesEnabled = true;
    public boolean keepDamageParticles = true;
    // ... 更多字段
}
```

`Data` 是个类，它里面的 `particlesEnabled` 是字段（每个 Data 对象都有这些字段）。`PvPOptimizeConfig` 文件后面有 `private static final Data DATA = new Data();` —— 创建了一个**全局唯一的 Data 实例**。

### 3.8 继承

一个类可以"继承"另一个类，自动获得它的字段和方法。

```java
// 父类
class Animal {
    String name;
    void speak() { System.out.println("..."); }
}

// 子类
class Dog extends Animal {
    @Override  // 注解：表示这个方法覆盖了父类的同名方法
    void speak() { System.out.println("汪！"); }
}

class Cat extends Animal {
    @Override
    void speak() { System.out.println("喵～"); }
}
```

**PvP-Optimize 里的真实例子** —— 实体过滤：

```java
// EntityFilter.java 第 60 行
if (entity instanceof VillagerEntity) return false;
```

`VillagerEntity` 是 Minecraft 的类，它继承自更上层的类。我们这里的 `instanceof` = "判断这个对象是不是某个类的实例"。

### 3.9 接口

接口 = **只规定"做什么"，不规定"怎么做"**。

```java
interface Flyable {
    void fly();  // 抽象方法（没有方法体）
}

class Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("鸟儿扇翅膀飞");
    }
}

class Airplane implements Flyable {
    @Override
    public void fly() {
        System.out.println("飞机引擎推力飞");
    }
}
```

**PvP-Optimize 里的真实例子** —— `config/PvPOptimizeModMenu.java`：

```java
public class PvPOptimizeModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> PvPOptimizeConfigScreen.create(parent);
    }
}
```

`ModMenuApi` 是 Mod Menu 提供的接口。我们"实现"（implements）它，必须提供 `getModConfigScreenFactory` 方法。Mod Menu 加载时就会找到这个类并调用这个方法，从而把我们的配置界面塞到 Mod 列表里。

### 3.10 异常处理

代码可能出错（文件找不到、网络断、数组越界等）。用 try-catch 兜住。

```java
try {
    int result = 10 / 0;  // 会出错：除以 0
} catch (ArithmeticException e) {
    System.out.println("出错了：" + e.getMessage());
} finally {
    System.out.println("不管有没有错都会执行");
}
```

**PvP-Optimize 里的真实例子** —— `mixin/ParticleManagerMixin.java`：

```java
try {
    java.lang.reflect.Field particles = ParticleManager.class.getDeclaredField("particles");
    particles.setAccessible(true);
    // ...
} catch (Throwable ignored) {
    // field renamed in this yarn build - redirect above still active
}
```

反射（reflect）能"强行"访问类的私有字段，但字段名可能因为 Minecraft 升级而改名，所以用 try-catch 兜底。

### 3.11 包（package）和导入（import）

包 = 文件夹。导入 = 引用别的文件夹里的类。

`PvPOptimizeConfig.java` 第 1-12 行：

```java
package com.pvp.optimize;        // 我自己在 com/pvp/optimize 文件夹下

import com.google.gson.Gson;      // 从 google 的 gson 库导入 Gson 类
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;  // 从 fabric loader 库导入
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;  // fabric api
import net.minecraft.client.option.KeyBinding; // minecraft 自己的
```

包名 = 域名倒着写 + 路径。`com.pvp.optimize` 对应 `src/main/java/com/pvp/optimize/`。

### 3.12 动手做

✅ **任务 3.1**：写 `LoopDemo.java`，用 for 循环打印 1 到 100，能被 3 整除但不背 5 整除的数。
✅ **任务 3.2**：写 `CounterDemo.java`，定义一个 `Counter` 类，有 `count` 字段和 `increment()` 方法。new 一个对象，调用 3 次 increment，打印 count 值。
✅ **任务 3.3**：打开 `pvp-optimize\src\main\java\com\pvp\optimize\particle\ParticleFilter.java`，**自己读一遍**每行代码的意思（结合本节学到的语法）。读不懂就跳过，下一章我们精读。

---

## 第 4 章 Gradle 入门

### 4.1 什么是 Gradle

Gradle = **自动构建工具**。你写完一堆 `.java` 文件，手工用 `javac` 一一编译太烦，Gradle 能：

- 找到所有 `.java` 文件
- 自动下载依赖（Fabric、Minecraft、Mod Menu 等 jar）
- 按顺序编译
- 打成 `.jar`
- 输出到固定目录

### 4.2 `build.gradle` 精读

打开 `pvp-optimize\build.gradle`：

```groovy
plugins {                                          // 插件声明
    id ''fabric-loom'' version ''1.6-SNAPSHOT''    // 用 fabric-loom 插件（专门为 Fabric Mod 服务的）
    id ''maven-publish''                           // 发布到 Maven 仓库（暂时用不到）
    id ''java''                                     // Java 插件
}

repositories {                                     // 依赖从哪里下载
    mavenCentral()                                 // Maven 中央仓库
    flatDir { dirs "local-libs" }                  // 本地文件夹（用来放手动下载的 jar）
}

dependencies {                                    // 依赖列表
    minecraft "com.mojang:minecraft:${project.minecraft_version}"  // Minecraft 本体
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"       // yarn 映射（人话版的类名）
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"  // Fabric Loader
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"  // Fabric API
    modImplementation files("local-libs/modmenu-10.0.0.jar", ...)  // 本地 jar
}
```

`${project.minecraft_version}` 这种 `${...}` 是 Groovy 语法，从 `gradle.properties` 读变量。

### 4.3 `gradle.properties` 精读

```properties
minecraft_version=1.20.6           # Minecraft 版本
yarn_mappings=1.20.6+build.1       # yarn 映射版本
loader_version=0.16.5              # Fabric Loader 版本
fabric_version=0.100.8+1.20.6      # Fabric API 版本
mod_version=1.0.0                  # 我们这个 mod 的版本
```

升级 MC 版本时，只改这个文件。

### 4.4 常用命令

| 命令 | 干什么 |
|---|---|
| `.\gradlew.bat build -x test --no-daemon` | 编译 + 打包，输出到 `build/libs/` |
| `.\gradlew.bat clean` | 清理 `build/` 目录 |
| `.\gradlew.bat build --offline` | 强制使用本地缓存，不联网 |

完整流程：
```
编辑源码
  ↓
.\gradlew.bat build
  ↓
build\libs\pvp-optimize-1.0.0.jar
  ↓
复制到 mods 文件夹
```

### 4.5 动手做

✅ **任务 4.1**：在 VS Code 里打开 `build.gradle` 和 `gradle.properties`，对照本节标记每个字段的含义。
✅ **任务 4.2**：跑一次 `.\gradlew.bat clean build -x test --no-daemon`，看控制台输出。理解每个 Task 做了什么。

---

## 第 5 章 Minecraft + Fabric 基础

### 5.1 Minecraft 的程序结构

Minecraft 拆分成很多层（从下到上）：

```
┌─────────────────────────────────┐
│  玩家看到的画面（渲染层）         │  ← ParticleManager, EntityRenderDispatcher
├─────────────────────────────────┤
│  游戏逻辑（规则、合成、合成表等） │  ← MinecraftClient, World
├─────────────────────────────────┤
│  实体、生物、方块（数据层）       │  ← Player, Villager, Block
├─────────────────────────────────┤
│  网络（多人游戏）                 │  ← ClientConnection
├─────────────────────────────────┤
│  原生代码（声音、图形、IO）       │  ← LWJGL 调 OpenGL
└─────────────────────────────────┘
```

我们的 mod 主要改**渲染层**（不让某些粒子、不让某些实体画出来）和**客户端**逻辑（开关红色滤镜）。

### 5.2 什么是 Mod

Mod（Modification）= **外挂的小程序**。它不修改 Minecraft 本身，而是在 Minecraft 启动时"插"进去，**偷偷改一些方法的行为**。

实现这个"插"的技术叫 **Mixin**（下一节）。

### 5.3 什么是 Fabric

Fabric = **Mod 加载器**。负责：
- 启动 Minecraft 前准备好运行环境
- 找到所有 Mod 的 jar
- 让 Mod 之间能互相通信
- 提供 Mod 用的 API（比如注册键位、注册 HUD 事件）

没有 Fabric，你的 jar 就是个死文件，进了 mods 目录也不会有反应。

### 5.4 Mod 入口点

Mod 启动时，Minecraft 会找"入口点" —— 也就是第一个被调用的类。

`fabric.mod.json` 里声明：

```json
"entrypoints": {
    "client": ["com.pvp.optimize.PvPOptimize"],
    "modmenu": ["com.pvp.optimize.config.PvPOptimizeModMenu"]
}
```

`client` = 客户端入口，每个客户端 Mod 都需要。
`modmenu` = Mod Menu 入口（可选），Mod Menu 加载时会调用。

`PvPOptimize.java` 是个**实现了 `ClientModInitializer` 接口的类**：

```java
public class PvPOptimize implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 这里是 mod 启动时会运行的代码
        PvPOptimizeConfig.load();
        OverlayHud.register();
    }
}
```

### 5.5 yarn 映射（Mappings）

Minecraft 的 `.class` 文件里类名是混淆的，比如 `class_638`、`method_1234`，人看不懂。

yarn = 社区做的一套"翻译表"，把这些混淆名翻译成人话：

| 混淆名 | yarn 翻译 | 中文 |
|---|---|---|
| `class_638` | `Camera` | 摄像机 |
| `class_761` | `MinecraftClient` | 客户端主类 |
| `class_310` | `MinecraftClient`（入口）| 客户端入口 |
| `method_3192` | `render` | 渲染方法 |

我们的代码里写 `Camera camera;` 而不是 `class_638 camera;`，就是 yarn 在背后帮我们做了翻译。

### 5.6 Mixin 是什么

Mixin = **不改源代码就能改方法行为的技术**。

打个比方：Minecraft 的 `ParticleManager.renderParticles` 方法原本会画所有粒子。用 Mixin 我们"在它画粒子之前偷偷插一脚"，检查一下这个粒子是不是我们想保留的——不是就跳过。

`mixin/ParticleManagerMixin.java`：

```java
@Mixin(ParticleManager.class)  // 注入目标：ParticleManager 类
public class ParticleManagerMixin {

    @Redirect(
        method = "renderParticles",                          // 目标方法
        at = @At(value = "INVOKE",
                 target = "L.../Particle;buildGeometry(...)V") // 拦截点
    )
    private void pvpoptimize$redirectBuildGeometry(Particle particle, ...) {
        if (ParticleFilter.shouldRender(particle)) {
            particle.buildGeometry(...);  // 我们判断通过才真正画
        }
        // 否则不画（悄悄丢弃这个粒子）
    }
}
```

Mixin's `pvp_optimize.mixins.json` 决定加载哪些 mixin 类。Fabric 启动时读这个文件去加载。

### 5.7 动手做

✅ **任务 5.1**：打开 `pvp-optimize\src\main\resources\pvp_optimize.mixins.json`，看看里面声明了哪些 mixin 类。
✅ **任务 5.2**：打开 `fabric.mod.json`，标记出每个字段对应本章的哪一节。

---

## 第 6 章 PvP-Optimize 代码精读

> 这是整个教程的核心。**这一章你必须亲手读每一行**，遇到不懂的回头翻第 3 章。

### 6.1 整体架构

```
┌─ PvPOptimize.java（入口，启动时调用）
│   ├─ PvPOptimizeConfig.load()  → 读 JSON 配置文件
│   └─ OverlayHud.register()     → 注册 HUD 渲染回调
│
├─ PvPOptimizeConfig.java（配置管理）
│   ├─ Data 类（13 个字段：开关/距离/颜色等）
│   ├─ load() / save()  → JSON 读写
│   └─ 4 个 KeyBinding（H / K / Y / J）
│
├─ particle/ParticleFilter.java（粒子过滤判定）
│   └─ shouldRender(Particle) → true 保留 / false 过滤
│
├─ entity/EntityFilter.java（实体剔除判定）
│   └─ shouldCull(Entity) → true 删除 / false 保留
│
├─ mixin/ParticleManagerMixin.java（注入到游戏渲染）
│   └─ 拦截每个粒子的绘制，过滤掉不想要的
│
├─ mixin/EntityRenderDispatcherMixin.java
│   └─ 拦截每个实体的绘制，距离远的跳过
│
├─ hud/OverlayHud.java（屏幕滤镜 + 状态面板）
│   ├─ 全屏红色滤镜
│   └─ 状态面板（开/H 切换）
│
└─ config/
    ├─ PvPOptimizeModMenu.java（Mod Menu 入口）
    └─ PvPOptimizeConfigScreen.java（Cloth Config 屏幕）
```

### 6.2 `PvPOptimize.java` —— Mod 入口

```java
public class PvPOptimize implements ClientModInitializer {
    public static final String MOD_ID = "pvp_optimize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PvP-Optimize] Initialized. Press H to toggle the status panel.");
        PvPOptimizeConfig.load();
        OverlayHud.register();
    }
}
```

逐行解释：
- `public class PvPOptimize implements ClientModInitializer` —— 声明这个类，实现 `ClientModInitializer` 接口（来自 Fabric API）
- `MOD_ID` —— 全大写 = 常量，跨文件用的唯一标识
- `LOGGER` —— 日志记录器，可以在控制台和 latest.log 打印消息
- `@Override` —— 表示下面是实现接口的方法（必须重写）
- `onInitializeClient()` —— Fabric 启动客户端时调用的方法，**所有 mod 启动逻辑都从这里开始**
- `LOGGER.info(...)` —— 打印 INFO 级别日志
- `PvPOptimizeConfig.load()` —— 读我们之前保存的 JSON 配置
- `OverlayHud.register()` —— 注册 HUD 渲染回调（这样游戏每帧画图时会调用我们的代码）

### 6.3 `PvPOptimizeConfig.java` —— 配置管理

最长的一个文件，分 4 部分：

#### 6.3.1 Data 内部类

```java
public static final class Data {
    public boolean particlesEnabled = true;   // 粒子过滤总开关
    public boolean keepCritParticles = true;  // 保留暴击粒子
    public boolean keepDamageParticles = true;// 保留伤害红心
    public boolean keepPotionParticles = true;// 保留药水粒子
    public boolean keepXpParticles = true;    // 保留经验球

    public boolean entityCullingEnabled = true;  // 实体剔除总开关
    public double cullDistance = 16.0;           // 剔除半径

    public boolean redOverlayEnabled = true;     // 红色滤镜开关
    public int overlayColor = 0x10FF1010;        // 颜色（AARRGGBB，A=透明度）
    public float overlayOpacity = 0.15f;         // 透明度
}
```

#### 6.3.2 持久化

```java
private static final Path CONFIG_PATH =
    FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize.json");
```

`FabricLoader.getInstance().getConfigDir()` = `.minecraft/config/` 目录。
`resolve("pvp_optimize.json")` = 在这个目录下拼一个 `pvp_optimize.json` 路径。

```java
public static void save() {
    try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
        GSON.toJson(DATA, w);
    } catch (IOException e) {
        LOGGER.warn("Failed to save config", e);
    }
}
```

把 `DATA` 对象序列化成 JSON 写到文件。`try-with-resources` 自动关闭 Writer。

#### 6.3.3 键位注册

```java
public static final KeyBinding OPEN_HUD = new KeyBinding(
    "key.pvp_optimize.open_hud",
    InputUtil.Type.KEYSYM,
    GLFW.GLFW_KEY_H,
    "category.pvp_optimize"
);

public static void register() {
    KeyBindingHelper.registerKeyBinding(OPEN_HUD);
    KeyBindingHelper.registerKeyBinding(TOGGLE_PARTICLES);
    // ...
}
```

每个 `KeyBinding` 需要 4 个参数：
- 翻译键（去 lang 文件里找对应文字）
- 输入类型（`KEYSYM` = 键盘按键）
- 默认按键的 GLFW 编码
- 分类（设置界面里这个键位出现在哪个分类下）

#### 6.3.4 load() 反序列化

```java
public static void load() {
    if (!Files.exists(CONFIG_PATH)) {
        save();  // 第一次运行：写默认配置
        return;
    }
    try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
        Data loaded = GSON.fromJson(r, Data.class);  // 读 JSON 转成 Data 对象
        if (loaded != null) {
            DATA.particlesEnabled = loaded.particlesEnabled;  // 逐字段拷贝
            // ...
        }
    } catch (IOException e) {
        LOGGER.warn("Failed to read config, using defaults", e);
    }
}
```

### 6.4 `ParticleFilter.java` —— 粒子过滤判定

这是最核心的业务逻辑。

```java
public static boolean shouldRender(Particle particle) {
    PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
    if (!cfg.particlesEnabled) return true;  // 总开关关 → 不过滤，全部放行

    String name = particle.getClass().getName().toLowerCase();

    if (name.contains("critparticle")) {
        return cfg.keepCritParticles;  // 暴击星形 → 看配置
    }

    if (name.contains("damageparticle")) {
        return cfg.keepDamageParticles;  // 伤害红心 → 看配置
    }

    // ... 其他类型

    return false;  // 其余一律丢弃
}
```

**关键技巧**：`particle.getClass().getName()` 拿到这个粒子对象的"类的全名"，比如 `net.minecraft.client.particle.CritParticle`。`.toLowerCase()` 统一小写，然后用 `name.contains(...)` 模糊匹配关键字。

**Minecraft 1.20.6 粒子类名速查**：

| 类名（去前缀 net.minecraft.client.particle.） | 视觉 | 用途 |
|---|---|---|
| `CritParticle` | 青色星星 | 暴击 |
| `DamageParticle` | 红心 | 伤害指示 |
| `SweepAttackParticle` | 弧形 | 剑横扫 |
| `HeartParticle` | 粉红爱心 | 动物繁殖 |
| `EffectParticle` | 药水云 | 喷溅药水 |
| `EntityEffectParticle` | 围绕实体的光点 | 状态效果 |
| `ExperienceOrbParticle` | 绿点 | 经验球 |
| `FlameParticle`、`SmokeParticle`、`BubbleParticle`…… | 各种 | 通用 |

### 6.5 `EntityFilter.java` —— 实体剔除判定

逻辑类似：

```java
public static boolean shouldCull(Entity entity) {
    if (!PvPOptimizeConfig.get().entityCullingEnabled) return false;

    if (entity instanceof PlayerEntity) return false;       // 玩家永远保留
    if (entity instanceof VillagerEntity) return false;    // 村民保留
    if (entity instanceof SnowballEntity) return false;    // 雪球保留
    if (entity instanceof EnderPearlEntity) return false;  // 末影珍珠保留
    if (entity instanceof ArrowEntity) return false;       // 箭保留
    if (entity instanceof PotionEntity) return false;      // 喷溅药水保留
    // ...

    if (isMineralItem(entity)) return false;  // 矿物掉落物保留

    // 其它所有实体：距离 > 16 格就剔除
    return !isWithinRange(entity);
}
```

### 6.6 Mixin 注入

#### `ParticleManagerMixin.java`

```java
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Redirect(
        method = "renderParticles",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/particle/Particle;buildGeometry(...)V")
    )
    private void pvpoptimize$redirectBuildGeometry(Particle particle, ...) {
        if (ParticleFilter.shouldRender(particle)) {
            particle.buildGeometry(...);  // 通过：真正画这个粒子
        }
        // 不通过：什么都不做，等于丢弃
    }
}
```

`@Redirect` 替换原本的 `Particle.buildGeometry` 调用。`@At` 里的 `target` 是方法的"全名"格式 `L包/类名;方法名(参数)返回类型`。

#### `EntityRenderDispatcherMixin.java`

类似，拦截 `EntityRenderDispatcher.render`，距离过远就跳过。

### 6.7 `OverlayHud.java` —— HUD 渲染

```java
public static void register() {
    ClientTickEvents.END_CLIENT_TICK.register(client -> {
        // 每 tick 跑一次
        while (PvPOptimizeConfig.OPEN_HUD.wasPressed()) {
            hudVisible = !hudVisible;  // 切换显示
        }
    });

    HudRenderCallback.EVENT.register(OverlayHud::render);  // 渲染时回调
}

private static void render(DrawContext ctx, float tickDelta) {
    // 1. 全屏红色滤镜
    if (cfg.redOverlayEnabled) {
        ctx.fill(0, 0, width, height, argb);  // 填整个屏幕
    }

    // 2. 状态面板
    if (hudVisible) {
        // ... 画文字
    }
}
```

`ClientTickEvents.END_CLIENT_TICK` = Fabric 提供的事件，每 tick 触发一次。
`HudRenderCallback.EVENT` = HUD 渲染前触发。
`DrawContext` = Minecraft 的画图工具（类似画笔）。

### 6.8 `config/PvPOptimizeModMenu.java`

```java
public class PvPOptimizeModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> PvPOptimizeConfigScreen.create(parent);
    }
}
```

`ModMenuApi` 接口。Mod Menu 加载时会调用 `getModConfigScreenFactory()`，返回一个 lambda，这个 lambda 接收"父屏幕"，返回"我们自己的设置屏幕"。`parent -> ...` 是 lambda 语法。

### 6.9 `config/PvPOptimizeConfigScreen.java`

用 Cloth Config 库构建配置界面。

```java
ConfigBuilder builder = ConfigBuilder.create()
    .setParentScreen(parent)
    .setTitle(Text.translatable("config.pvp_optimize.title"));

ConfigCategory particles = builder.getOrCreateCategory(
    Text.translatable("config.pvp_optimize.category.particles"));

particles.addEntry(eb.startBooleanToggle(
    Text.translatable("config.pvp_optimize.keepCritParticles"),
    data.keepCritParticles)
.setDefaultValue(true)
.setSaveConsumer(v -> data.keepCritParticles = v)
.build());

builder.setSavingRunnable(PvPOptimizeConfig::save);
return builder.build();
```

- `ConfigBuilder.create()` = 创建一个配置构建器
- `getOrCreateCategory(...)` = 获取或新建一个分类
- `startBooleanToggle(...)` = 开始构造一个布尔开关
- `setSaveConsumer(v -> ...)` = 用户点保存时调用，v 是新值
- `setSavingRunnable(PvPOptimizeConfig::save)` = 点保存按钮时调用 save() 写 JSON

### 6.10 动手做

✅ **任务 6.1**：合上电脑，凭记忆写出 mod 的 5 个核心类名。
✅ **任务 6.2**：在 `ParticleFilter.java` 里数一下，目前一共处理了几种粒子类。
✅ **任务 6.3**：在 `EntityFilter.java` 里数一下白名单的实体类型。

---

## 第 7 章 动手实战

> 每节都包含：目标 → 步骤 → 验证。

### 7.1 实战 1：把暴击粒子从默认开启改成默认关闭

**目标**：新玩家装上 mod 默认看不到暴击粒子，但可以手动开。

**步骤**：

1. 打开 `PvPOptimizeConfig.java`
2. 找到第 26 行附近：`public boolean keepCritParticles = true;`
3. 改成 `public boolean keepCritParticles = false;`
4. 打开 `ParticleFilter.java` 第 24 行附近的注释
5. 把中文注释里的"默认"字样注释修正（可选）

**验证**：

```powershell
cd C:\Users\16210\Documents\pvp-optimize
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build -x test --no-daemon
```

成功的话 `build/libs/pvp-optimize-1.0.0.jar` 时间戳更新。复制到 mods 目录，启动游戏。暴击时应该没有青色星星，但 Mod Menu → Config → 粒子过滤 → "保留暴击粒子"勾上后又会出现。

### 7.2 实战 2：把状态面板默认键位从 H 改成 G

**目标**：开 H 容易误触，改成 G。

**步骤**：

1. 打开 `PvPOptimizeConfig.java`
2. 找到 `OPEN_HUD` 的定义（搜 `OPEN_HUD`）
3. 把 `GLFW.GLFW_KEY_H` 改成 `GLFW.GLFW_KEY_G`
4. 打开 `hud/OverlayHud.java` 第 18 行的 `PvPOptimizeConfig.OPEN_HUD` 也改成 `G`（其实是引用，自动跟着变，无需改）

**注意**：G 键在 Minecraft 是切换生存模式的快捷键。要选一个不冲突的键，参考 [Minecraft 按键冲突表](https://minecraft.fandom.com/wiki/Key_Bindings)。

### 7.3 实战 3：加一个新配置项 "保留末影龙粒子"

**目标**：默认保留末影龙吐息的紫色粒子。

**步骤**：

1. **加 Data 字段** —— 打开 `PvPOptimizeConfig.java`，在 Data 类里加一行：
   ```java
   public boolean keepDragonBreath = true;
   ```

2. **加 load 反序列化** —— 找到 `load()` 方法里那串 `DATA.xxx = loaded.xxx;`，加一行：
   ```java
   DATA.keepDragonBreath = loaded.keepDragonBreath;
   ```

3. **加过滤逻辑** —— 打开 `ParticleFilter.java`，在 `shouldRender` 里加：
   ```java
   if (name.contains("dragonbreathparticle")) {
       return cfg.keepDragonBreath;
   }
   ```

4. **加配置 UI** —— 打开 `config/PvPOptimizeConfigScreen.java`，在 `particles.addEntry(...)` 那段后面加：
   ```java
   particles.addEntry(eb.startBooleanToggle(
       Text.translatable("config.pvp_optimize.keepDragonBreath"),
       data.keepDragonBreath)
       .setDefaultValue(true)
       .setSaveConsumer(v -> data.keepDragonBreath = v)
       .build());
   ```

5. **加翻译** —— 打开 `zh_cn.json`，在 `keepXpParticles` 后面加：
   ```json
   "config.pvp_optimize.keepDragonBreath": "保留末影龙粒子",
   ```
   同样加到 `en_us.json`：
   ```json
   "config.pvp_optimize.keepDragonBreath": "Keep dragon breath particles",
   ```

6. **更新状态面板**（可选）—— `OverlayHud.java` 第 63 行那串加一项：
   ```java
   + " 龙息=" + onOff(cfg.keepDragonBreath)
   ```

**验证**：跑构建命令 → 启动游戏 → 招一只末影龙 → 应该看到紫色龙息粒子 → 进 Mod Menu → 关掉"保留末影龙粒子" → 重启游戏 → 龙息应该消失。

### 7.4 实战 4：把状态面板移到屏幕右上角

**目标**：默认在左上角，移到右上。

**步骤**：

1. 打开 `OverlayHud.java` 第 57-58 行附近：
   ```java
   int x = 4;
   int y = 4;
   ```
2. 改成动态计算：
   ```java
   int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
   int x = screenW - width - 8;  // 屏幕宽 - 文字宽 - 边距
   int y = 4;
   ```

**注意**：`width` 是文字宽，要在算 x 之前先算好。看原代码，`width` 是在 `lines` 之后算的，所以要调整顺序。

### 7.5 实战 5：把红色滤镜默认改成蓝色

**目标**：改成淡蓝色滤镜像受伤屏效。

**步骤**：

1. 打开 `PvPOptimizeConfig.java`，找到：
   ```java
   public int overlayColor = 0x10FF1010;
   ```
2. 改成：
   ```java
   public int overlayColor = 0x101010FF;  // A=10(透明度), R=10, G=10, B=FF
   ```

**颜色格式**：`AARRGGBB`（不是常见的 `RRGGBB`！注意区分）
- `0x10FF1010` = 透明 16/255 + 红 255 + 绿 16 + 蓝 16 = 红色
- `0x101010FF` = 透明 16/255 + 红 16 + 绿 16 + 蓝 255 = 蓝色
- `0x2000FF00` = 透明 32/255 + 绿色

### 7.6 动手做

✅ **任务 7.1**：完成实战 1，重新构建。
✅ **任务 7.2**：完成实战 3，验证新配置项在 Mod Menu 出现。
✅ **任务 7.3**：自己设计一个实战任务并完成（例如：加一个"保留爆炸粒子"开关）。

---

## 第 8 章 调试技巧

### 8.1 三种日志

游戏会产生 3 个日志文件，都在 `C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\logs\`：

| 文件 | 用途 |
|---|---|
| `latest.log` | 最近一次启动的完整日志 |
| `debug.log` | 调试日志（需要在启动参数加 `-Dfabric.debug=1`） |
| `latest.log` 之外的有数字后缀 | 历史启动日志 |

### 8.2 排查流程

1. **游戏崩了**：
   - 打开 `latest.log`
   - 搜 `ERROR` 或 `Exception`
   - 看堆栈最顶部的 `Caused by:`
   - 记下报错行号

2. **Mod 没加载**：
   - 搜 `Initialized` 或你的 mod id
   - 没找到 = 启动就失败了，看 `[main/ERROR]` 级别的日志

3. **修改没生效**：
   - 检查 `build/libs/` 里的 jar 时间戳是不是刚才构建的
   - 检查 mods 目录里的 jar 是不是刚才的版本
   - 删掉 `build/` 重新 `clean build` 试试

4. **编译失败**：
   - 仔细读红色错误行
   - 大概率是拼写错误、缺分号、缺大括号
   - 找不到符号 = 没 import 或类名错

### 8.3 推荐的 IDE 调试

在 IntelliJ 里：
1. 打开项目
2. 在代码左边点击行号右边空白处（加红点 = 断点）
3. 菜单 Run → Edit Configurations → 加 Application
4. Main class: `net.fabricmc.loader.impl.launch.knot.KnotClient`
5. Working dir: `.minecraft` 所在目录
6. 点 Debug

游戏启动到断点会暂停，可以单步执行、查看变量。

**零基础暂时不要求学调试**，先把改 → 构建 → 部署流程跑通就够。

### 8.4 动手做

✅ **任务 8.1**：故意在 `ParticleFilter.java` 里删一个分号，重新构建。看 Gradle 报什么错。
✅ **任务 8.2**：把 `latest.log` 搜 "PvP-Optimize"，确认我们的初始化日志在。

---

## 第 9 章 进阶方向

### 9.1 升级到 1.21.x

参考 `UPDATE_GUIDE.md` 第 2 章。最核心的改动是 5 个文件：`gradle.properties`、`build.gradle`、`local-libs/*.jar`、源码（编译报错时改）、lang 文件（一般不用改）。

### 9.2 推荐的学习资源

| 资源 | 链接 | 适合 |
|---|---|---|
| 《Java 核心技术 卷 I》 | 任意网店 | 深入学 Java |
| Fabric 官方文档 | https://fabricmc.net/develop/ | 学 Fabric 进阶 |
| Yarn 映射表 | https://maven.fabricmc.net/net/fabricmc/yarn/ | 查类名 |
| Fabric Modding Discord | https://discord.fabricmc.net/ | 提问 |
| B 站搜"Fabric Mod 开发" | B 站 | 视频教程 |

### 9.3 推荐的项目练习

按难度递增：

1. **加一个新粒子保留规则**（实战 3 已演示）
2. **加一个新键位**（比如把红色滤镜从 J 改成一个组合键 `Ctrl+R`）
3. **加一个独立 GUI 屏幕**（用 Cloth Config 做一个"调试面板"，显示当前世界里有多少实体）
4. **加一个 mixin**（比如屏蔽某种 UI 元素）
5. **跨版本升级**（1.20.6 → 1.21.4）
6. **发布到 Modrinth**（让全网玩家用你的 mod）

### 9.4 推荐的职业方向

掌握 Fabric Mod 开发后，可以走：
- **Minecraft Mod 开发者**（业余也能接外包赚钱）
- **游戏客户端工程师**（Unity、Unreal 也用类似技术）
- **Java 后端工程师**（学完 Java 直接能转岗）
- **客户端渲染工程师**（学完 OpenGL/Mixin 后可以走图形学方向）

---

## 附录 A 速查表

### A.1 常用命令

```powershell
# 构建
cd C:\Users\16210\Documents\pvp-optimize
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build -x test --no-daemon

# 部署
Copy-Item build\libs\pvp-optimize-1.0.0.jar `
  "C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\mods\pvp-optimize-1.0.0.jar" `
  -Force

# 清理
.\gradlew.bat clean

# 离线构建
.\gradlew.bat build -x test --no-daemon --offline
```

### A.2 关键文件改什么

| 想改 | 改这个文件 |
|---|---|
| 加/减粒子 | `particle/ParticleFilter.java` |
| 加/减白名单实体 | `entity/EntityFilter.java` |
| 改键位 | `PvPOptimizeConfig.java` 里的 `KeyBinding` |
| 加配置项 | `PvPOptimizeConfig.java` + `config/PvPOptimizeConfigScreen.java` + `lang/*.json` |
| 改红色滤镜 | `PvPOptimizeConfig.java` 的 `overlayColor` / `overlayOpacity` |
| 改版本号 | `gradle.properties` 的 `mod_version` |

### A.3 错误信息速查

| 报错 | 原因 | 解法 |
|---|---|---|
| `cannot find symbol` | 漏 import / 拼错 | 检查类名 |
| `; expected` | 漏分号 | 找上一行加 `;` |
| `class, interface, or enum expected` | 大括号不匹配 | 用 IDE 折叠所有大括号检查 |
| `non-static method cannot be referenced from a static context` | 没 new 对象就调用方法 | 加 `new Xxx().method()` |
| `actual and formal argument lists differ in length` | 函数参数数量不对 | 检查函数定义 |
| `incompatible types` | 类型不匹配 | 强转 `(int)xxx` 或换类型 |
| `BUILD FAILED` | 编译失败 | 往上翻看具体错误 |
| `Mixin [...] FAILED during APPLY` | Mixin 目标方法不存在 | 改 `@At` target |
| `Mod was built with a newer version of Loom` | local-libs 里的 jar 太新 | 参考 `UPDATE_GUIDE.md` §3.3 strip manifest |

### A.4 常用导入

```java
// Minecraft 本身
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

// Fabric
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;

// Mixin
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Cloth Config
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;

// Mod Menu
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
```

---

## 后记

学完这份教程你应该能做到：
- ✅ 独立阅读 PvP-Optimize 的每一行代码
- ✅ 修改粒子、实体、键位、配置项、颜色
- ✅ 添加新功能（比如实战 3 的龙息粒子）
- ✅ 跨大版本升级
- ✅ 排查常见错误
- ✅ 借助 `UPDATE_GUIDE.md` 在 AI 不可用时继续维护

下一步建议：
1. 把第 7 章的 5 个实战**全部做一遍**（3-4 小时）
2. 在 B 站找一个 Fabric Mod 入门视频跟着做一遍
3. 尝试发布到 Modrinth，让更多人用

**祝你学得开心！**