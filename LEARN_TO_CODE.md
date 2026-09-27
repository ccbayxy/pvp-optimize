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

#### 🎬 生活场景：店铺开张的"开业仪式"

想象你在商场租了一个小铺子，招牌写"**pvp_optimize**"。开业那天你要做几件事：

1. **在商场管理处的"合同"上签字** —— 合同里规定："每位租户开业当天必须执行一段固定的流程（你可以在里面随便写要做什么）"
2. **拿对讲机** —— 以后有什么事，可以通过对讲机通知商场管理处
3. **开门大吉** —— 正式开门，开始接待顾客

`PvPOptimize.java` 这个类就是"开业仪式清单"。

#### 完整代码（24 行）

```java
package com.pvp.optimize;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvPOptimize implements ClientModInitializer {

    // ============ 招牌名 ============
    public static final String MOD_ID = "pvp_optimize";

    // ============ 对讲机 ============
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // 开业 3 件事
        LOGGER.info("[PvP-Optimize] Initialized. Press H to toggle the status panel.");
        PvPOptimizeConfig.load();   // 1) 读配置文件
        OverlayHud.register();      // 2) 注册 HUD 渲染
    }
}
```

#### 逐块拆解（用"开张"的故事讲）

##### 第 1 块：和商场签合同

```java
public class PvPOptimize implements ClientModInitializer {
```

- **`implements ClientModInitializer`**（"实现"接口）
  - 等于你和商场管理处签了一份合同
  - 合同里规定："租户开业当天必须执行一个叫 `onInitializeClient()` 的方法"
  - 至于你在这个方法里具体干什么，商场不管 —— 你自己发挥
  - 忘了写这个方法？**编译报错**，类签名算"违约"
  - 这就是 3.9 学的"接口"：**必须实现接口的全部抽象方法**

##### 第 2 块：挂招牌

```java
public static final String MOD_ID = "pvp_optimize";
```

- **招牌**就是你这个 mod 的"身份证号"
- **全大写** = 常量约定（大家都这么写，但语法不强制）
- **`static`**（静态）= 这块招牌全店**只挂一个**，不是每件商品各挂一个
- **`final`** = 招牌钉死后**不能再换**
- 这个名字会出现在：
  - 配置文件名（`pvp_optimize.json`）
  - 键位分类（`category.pvp_optimize`）
  - 翻译文件路径（`assets/pvp_optimize/lang/zh_cn.json`）
  - 日志前缀（`[PvP-Optimize]`）

##### 第 3 块：拿对讲机

```java
public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
```

- **对讲机** = 日志记录器
- 你以后想喊话（往控制台打印信息），用 `LOGGER.info("...")`、`LOGGER.warn("...")`、`LOGGER.error("...")`
- 商场里所有租户共用一套广播系统（控制台），但每家租户有自己独立的频道（用 `MOD_ID` 区分）
- 玩家在 `latest.log` 里能搜到 `[PvP-Optimize]` 前缀的日志

> 💡 **为什么用对讲机而不是 `System.out.println()`？**
> - `System.out.println()` 只能打印到控制台
> - `LOGGER` 能：分等级（info/warn/error）、自动加时间戳、写到日志文件、被其他 mod 监听
> - 专业 mod 必须用 LOGGER

##### 第 4 块：开业当天

```java
@Override
public void onInitializeClient() {
    LOGGER.info("[PvP-Optimize] Initialized. Press H to toggle the status panel.");
    PvPOptimizeConfig.load();
    OverlayHud.register();
}
```

- **`@Override`**（3.12 注解）= 在"开业仪式"这张纸的左上角**贴一张便利贴**写"下面是改写自合同规定的方法"
  - 写错了方法名？便利贴会让编译器立刻报警
- **`onInitializeClient()`**（开业仪式）
  - Fabric 启动客户端时**唯一**会调的方法
  - 所有 mod 启动逻辑都从这里开始
  - 想象成：商场的"开业铃"一响，每个租户依次上台念自己的开业词

**开业要做 3 件事**：

1. **`LOGGER.info(...)`**
   - 第一句话：告诉玩家"mod 已经启动，按 H 切状态面板"
   - 启动后控制台会看到 `[PvP-Optimize] Initialized. Press H to toggle the status panel.`

2. **`PvPOptimizeConfig.load()`**
   - 读 `config/pvp_optimize.json`（玩家上次保存的开关状态）
   - 等于开业前先去**档案室取回本店历史开关设置**
   - 没有档案（第一次启动）？就建一份默认的

3. **`OverlayHud.register()`**
   - 注册 HUD 渲染回调 = 告诉游戏"每画一帧，请顺便画一下我们的状态面板和红色滤镜"
   - 等于在店里**装一台监控**：游戏每画一帧，监控就拍一张照片，根据开关决定要不要显示画面

#### 🎯 一句话总结

`PvPOptimize.java` 就像店铺的**开业仪式清单**：
- 跟商场（Fabric）签合同（`implements ClientModInitializer`）
- 挂招牌（`MOD_ID`）、拿对讲机（`LOGGER`）
- 开业当天喊话 + 取档案 + 装监控（`onInitializeClient` 里的 3 件事）

> 🚨 **踩坑提醒**：
> - **绝对不要**在 `onInitializeClient` 里调"画图""注册键位"以外的**耗时操作**（比如下载文件）
> - 整个 mod 启动**会卡住**，直到你做完这些事
> - 想做异步任务？用 3.11 学的线程或 Fabric 的 `ClientTickEvents.END_CLIENT_TICK`
### 6.3 `PvPOptimizeConfig.java` —— 配置管理（类比版）

#### 🎬 生活场景：店铺的"控制面板 + 档案室"

继续用"店铺"的故事：

- 6.2 里我们签了合同、挂了招牌、开业了
- 但**客人进门后**能不能调温度、能不能开音响、调到什么亮度？这些**得有个地方统一管理**
- 想象店铺后墙挂着一个**控制面板**（10 个开关 + 旋钮），每个开关控制一类功能
- 客人**改完开关**后，我们要把当前状态**写到档案室**（`pvp_optimize.json`）里，下次开门再读回来

`PvPOptimizeConfig.java` 就是这面"控制面板 + 档案室"。整个文件 117 行，分 6 部分。

---

#### 6.3.1 字段声明（Data 内部类）

##### 🎬 生活场景：控制面板上的开关

想象你家的"智能家居控制面板"长这样：

```
┌─────────────── 粒子过滤 ───────────────┐
│ [✓] 总开关                                 │
│ [✓] 保留暴击粒子（青色星星）              │
│ [✓] 保留伤害粒子（红心）                  │
│ [✓] 保留药水/状态粒子                     │
│ [✓] 保留经验球粒子                        │
└──────────────────────────────────────┘

┌─────────────── 实体剔除 ───────────────┐
│ [✓] 总开关                                 │
│ 距离: [16] 格（旋钮 1-64）              │
└──────────────────────────────────────┘

┌─────────────── 屏幕滤镜 ───────────────┐
│ [✓] 红色滤镜                              │
│ 颜色: [#10FF1010]                       │
│ 透明度: [0.15]（滑块 0.0-1.0）           │
└──────────────────────────────────────┘
```

##### 完整代码

```java
public static final class Data {
    // ===== 粒子过滤（5 个开关）=====
    public boolean particlesEnabled = true;     // 总开关
    public boolean keepCritParticles = true;    // 保留暴击粒子
    public boolean keepDamageParticles = true;  // 保留伤害粒子
    public boolean keepPotionParticles = true;  // 保留药水粒子
    public boolean keepXpParticles = true;      // 保留经验球粒子

    // ===== 实体剔除（2 个）=====
    public boolean entityCullingEnabled = true; // 总开关
    public double cullDistance = 16.0;          // 剔除半径

    // ===== 屏幕滤镜（3 个）=====
    public boolean redOverlayEnabled = true;    // 红色滤镜开关
    public int overlayColor = 0x10FF1010;       // ARGB 颜色
    public float overlayOpacity = 0.15f;        // 透明度
}
```

##### 逐块拆解

- **`public static final class Data`**（"实现"接口之外的"内部小类"）
  - 在 `PvPOptimizeConfig` 内部又定义了一个**小类**叫 `Data`
  - 内部类 = 一个文件夹里装一个更小的文件夹
  - 外部引用写法：`PvPOptimizeConfig.Data`（就像"档案室里的'客户档案'"）

- **`public boolean xxx = true`**（面板上的开关）
  - **`boolean`**（3.5 学过）= 只有 `true`（开）/ `false`（关）两种状态
  - **`= true`** = 默认开
  - **`public`**（公开）= Cloth Config 和 Mixin 能直接读写，不用 setter
  - 等于面板上的"开关 1"，不盖盖子，客人随时能扳

- **颜色 `0x10FF1010`**
  - 拆解：`0x`（十六进制前缀）+ `10`（透明度 Alpha）+ `FF`（红 Red）+ `10`（绿 Green）+ `10`（蓝 Blue）
  - ARGB = Alpha + RGB
  - Alpha `10`（十六进制）= `16`（十进制）= `16/255 ≈ 6.3%` 透明度
  - RGB 都是 `10`（十六进制）= `16`（十进制）= 微弱红光
  - 整体效果：**很淡的红色覆盖层**（不影响看清画面）

- **`float overlayOpacity = 0.15f`**
  - 浮点数（3.6 学过），`f` 后缀告诉编译器"这是 float 不是 double"
  - 范围 0.0（完全透明）~ 1.0（完全不透明）

---

#### 6.3.2 静态实例与 Gson

##### 🎬 生活场景：店里的"档案柜 + 翻译机"

```java
private static final Data DATA = new Data();
private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
```

- **`private`**（3.5 学过）= 这两个东西"藏在店铺里"，外面的人不能直接拿
- **`static`**（3.8 学过）= **整家店共享这一个**，不是每件商品各一个
- **`final`**（3.5 学过）= 钉死了，**不能再换一个新对象**（但对象内部字段能改）
- **`new Data()`**（3.7 学过）= 真的去"打"一块控制面板出来，10 个开关都按默认值拨好

- **`Gson` = 翻译机**
  - Java 对象（`Data`）和 JSON 文本（`pvp_optimize.json`）之间**双向翻译**
  - Fabric Loader 内置，不需要额外下载
  - **`setPrettyPrinting()`** = 翻译时加点缩进，让 JSON 文件**人也能读**

> 💡 **为什么要翻译？**
> - 内存里：`Data` 对象（10 个字段，开/关数字各种类型）
> - 磁盘上：JSON 文本（字符串）
> - 保存配置 = 对象 → 文本（翻译）
> - 读取配置 = 文本 → 对象（再翻译回来）

---

#### 6.3.3 配置文件路径

##### 🎬 生活场景：档案室的"文件柜位置"

```java
private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize.json");
```

- **`Path`** = Java 表示文件路径的类型（3.7 学过）
- **`FabricLoader.getInstance()`** = 拿到 Fabric 这个"商场管理处"
- **`.getConfigDir()`** = 问管理处："你们统一管档案的柜子在哪个文件夹？"
  - 答案：`.minecraft/config/`
- **`.resolve("pvp_optimize.json")`** = 在那个文件夹里**指一个具体文件**叫 `pvp_optimize.json`
- 最终路径示例：`C:\Users\你\.minecraft\config\pvp_optimize.json`

> 💡 **链式调用**（3.15 学过）：每个方法都"返回自己"，可以一直 `.` 下去
> `A.getInstance().getConfigDir().resolve(...)` 就是 3 个方法接力跑

---

#### 6.3.4 4 个键位注册

##### 🎬 生活场景：店铺里的"4 个总开关遥控器"

想象店铺墙上挂着 4 个**遥控器**，每个遥控器控制一类功能：

| 遥控器按键 | 作用 | 翻译键 |
|---|---|---|
| H | 切换状态面板 | `key.pvp_optimize.open_hud` |
| P | 切换粒子过滤总开关 | `key.pvp_optimize.toggle_particles` |
| K | 切换实体剔除总开关 | `key.pvp_optimize.toggle_cull` |
| J | 切换红色滤镜 | `key.pvp_optimize.toggle_red_overlay` |

> 🚨 **4 个键位**（不是 5 个，没有 Y），对应 4 个总开关

##### 完整代码

```java
public static final KeyBinding OPEN_HUD = new KeyBinding(
        "key.pvp_optimize.open_hud",         // 翻译键
        InputUtil.Type.KEYSYM,               // 输入类型：键盘
        GLFW.GLFW_KEY_H,                     // 默认按键 H
        "category.pvp_optimize"              // 分类
);
public static final KeyBinding TOGGLE_PARTICLES = new KeyBinding(
        "key.pvp_optimize.toggle_particles",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_P,                     // 默认 P
        "category.pvp_optimize"
);
public static final KeyBinding TOGGLE_CULL = new KeyBinding(
        "key.pvp_optimize.toggle_cull",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_K,                     // 默认 K
        "category.pvp_optimize"
);
public static final KeyBinding TOGGLE_RED_OVERLAY = new KeyBinding(
        "key.pvp_optimize.toggle_red_overlay",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_J,                     // 默认 J（不是 Y）
        "category.pvp_optimize"
);

public static void register() {
    load();   // 先取档案
    KeyBindingHelper.registerKeyBinding(OPEN_HUD);
    KeyBindingHelper.registerKeyBinding(TOGGLE_PARTICLES);
    KeyBindingHelper.registerKeyBinding(TOGGLE_CULL);
    KeyBindingHelper.registerKeyBinding(TOGGLE_RED_OVERLAY);
}
```

##### 逐块拆解

- **`KeyBinding` 4 个参数**（遥控器工厂的 4 个问题）：
  1. **翻译键**（遥控器贴的标签）= 字符串，会去 `lang/zh_cn.json` 找对应中文
  2. **输入类型**（遥控器是哪种）= `KEYSYM` = 键盘按键（`MOUSE` = 鼠标）
  3. **默认按键**（遥控器装几号电池）= 用 GLFW 编码（H=72, P=80, K=75, J=74）
  4. **分类**（遥控器放哪个抽屉）= 设置界面里这个键位出现在哪个分类下

- **`GLFW`**
  - 一个跨平台窗口/输入库
  - `GLFW.GLFW_KEY_H` 是个**常量**，数值是 H 键的内部编码
  - 就像 3.5 学的"颜色常量 `Color.RED`"，用名字代替数字更易读

- **`KeyBindingHelper.registerKeyBinding(...)`**（到管理处备案）
  - 遥控器造好后，得去商场管理处**登记备案**，否则游戏不认
  - 必须在 mod 启动时调用（6.2 学的 `onInitializeClient` 里）
  - **不能懒加载**（不能"等到客人按 H 才去备案"），否则按键无反应

> 🚨 **踩坑提醒**（来自 project memory）：
> - 键位注册必须发生在 mod 初始化阶段，不能 lazy-loaded
> - 之前踩过坑：在静态初始化器里注册键位，导致 `GameOptions` 已初始化后才执行 → 抛异常
> - 正确做法：在 `PvPOptimizeConfig.register()` 里集中注册，并在 mod 入口 `onInitializeClient` 里调用

---

#### 6.3.5 load() 反序列化

##### 🎬 生活场景：开门时"读档案"

每天店铺开门前，要从档案室取出昨天的开关设置，把控制面板上的 10 个开关**拨到正确位置**。

##### 完整代码

```java
public static void load() {
    if (!Files.exists(CONFIG_PATH)) {
        save();           // 第一次：档案不存在 → 写一份默认的
        return;
    }
    try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
        Data loaded = GSON.fromJson(r, Data.class);   // JSON → Data 对象
        if (loaded != null) {
            // 逐字段拷贝（不能用 DATA = loaded，因为 DATA 是 final）
            DATA.particlesEnabled      = loaded.particlesEnabled;
            DATA.keepCritParticles     = loaded.keepCritParticles;
            DATA.keepDamageParticles   = loaded.keepDamageParticles;
            DATA.keepPotionParticles   = loaded.keepPotionParticles;
            DATA.keepXpParticles       = loaded.keepXpParticles;
            DATA.entityCullingEnabled  = loaded.entityCullingEnabled;
            DATA.cullDistance          = loaded.cullDistance;
            DATA.redOverlayEnabled     = loaded.redOverlayEnabled;
            DATA.overlayColor          = loaded.overlayColor;
            DATA.overlayOpacity        = loaded.overlayOpacity;
        }
    } catch (IOException e) {
        PvPOptimize.LOGGER.warn("[PvP-Optimize] Failed to read config, using defaults", e);
    }
}
```

##### 逐块拆解

- **`if (!Files.exists(CONFIG_PATH))`**（档案不存在？）
  - **第一次启动**时档案室是空的
  - 直接调用 `save()` 写一份**默认配置**（10 个开关都是 true，距离 16，颜色默认）
  - 然后 `return` 退出，不用读
  - 等于第一次开店，**没有历史档案** → 用默认模板新建一份

- **`try (Reader r = ...)`**（try-with-resources，3.10 学过）
  - 打开档案文件，自动在末尾关闭
  - 防止"忘记关文件"导致资源泄漏

- **`Gson.fromJson(r, Data.class)`**（翻译机读档案）
  - **`Data.class`**（3.7 学过）= `Data` 类的"身份证"
  - 翻译机看到身份证就知道"这份档案要翻译成 `Data` 类型"
  - 返回**新对象** `loaded`

- **逐字段拷贝**（把 `loaded` 里的每个开关"拨"到 `DATA` 上）
  - 不能写 `DATA = loaded`（3.5 学过：final 不能换指向）
  - 但**可以改 `DATA` 内部的字段**（final 管的是"指向谁"，不管"对象内字段值"）
  - 等于"档案柜里换了一份新档案，但柜子还是同一个"

> 💡 **为什么 Gson 不能直接"翻译到 DATA"？**
> - Gson 只能 `new` 出**新对象**，不能把字段灌进**已存在**的对象
> - 折中：新建 `loaded` → 逐字段拷贝到 `DATA`

- **`catch (IOException e)`**（读档失败兜底）
  - 万一文件坏了、被杀毒软件锁了……
  - 不崩，warn 一条日志，继续用默认配置
  - **健壮性** = 出错不要紧，别让游戏崩

---

#### 6.3.6 save() 序列化

##### 🎬 生活场景：关门时"写档案"

每天店铺关门，要把控制面板上**当前的 10 个开关位置**抄到档案室，下次开门再读回来。

##### 完整代码

```java
public static void save() {
    try {
        Files.createDirectories(CONFIG_PATH.getParent());   // 确保 config 目录存在
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(DATA, w);                            // Data 对象 → JSON
        }
    } catch (IOException e) {
        PvPOptimize.LOGGER.warn("[PvP-Optimize] Failed to write config", e);
    }
}
```

##### 逐块拆解

- **`Files.createDirectories(CONFIG_PATH.getParent())`**（先造档案柜）
  - 万一 `.minecraft/config/` 文件夹都不存在？先创建
  - 等于"店铺档案室都没装修，先把柜子装好"

- **`try (Writer w = ...)`**（打开文件，自动关闭）
  - **`Writer`** = 写入器（向文件"写东西"的工具）
  - `try-with-resources` 自动关流

- **`GSON.toJson(DATA, w)`**（翻译机写档案）
  - 把内存里的 `DATA` 对象翻译成 JSON 文本，写到磁盘
  - 翻译出来的文件长这样（`setPrettyPrinting()` 让它带缩进）：
  ```json
  {
    "particlesEnabled": true,
    "keepCritParticles": true,
    "keepDamageParticles": true,
    ...
  }
  ```

> 💡 **save() 何时被调用？**
> - 6.2 启动时（如果配置不存在，写默认）
> - Cloth Config 界面点"完成"按钮时（自动调用 `setSaveConsumer` 里的回调）
> - 玩家在 Mod Menu 改设置时

---

#### 🎯 一句话总结

`PvPOptimizeConfig.java` = **店铺的"控制面板 + 档案室"**：
- **Data 类** = 控制面板（10 个开关 + 旋钮）
- **Gson** = 翻译机（对象 ↔ JSON）
- **CONFIG_PATH** = 档案柜位置（`.minecraft/config/pvp_optimize.json`）
- **4 个 KeyBinding** = 4 个遥控器（H/P/K/J）
- **load()** = 开门时读档案，拨开关
- **save()** = 关门时抄开关，写档案

> 🚨 **新手常见错误**：
> 1. 把 `DATA` 写成 `new Data()` 在 `load()` 里 → 旧配置丢光
> 2. 漏写 `static` → 每个引用方各自 new 一个，控制面板分裂
> 3. `try-catch` 不写 → 配文件损坏时游戏直接崩
> 4. `save()` 不在 `onInitializeClient` 之后调 → 默认配置写不进去
### 6.4 `ParticleFilter.java` —— 副本 ①：粒子迷宫

#### 🗺️ 副本背景

> *"勇者啊，欢迎来到 1.20.6 王国。这片土地被一场'视觉灾变'笼罩——粒子怪物们成群结队地刷在每个角落，遮挡视线、消耗显卡。但我们**不能全清**，暴击时的青色星星（暴击反馈）、红心（伤害指示）、状态效果光环（中毒、治疗）……这些是冒险者赖以生存的'视觉语言'。*
>
> *请进入**粒子迷宫**，把无用的怪物清掉，把重要的 NPC 留下。"*
>
> —— 村长

#### 🧙 副本档案

| 项目 | 内容 |
|---|---|
| 副本名 | **粒子迷宫**（Particle Maze） |
| 推荐等级 | Lv.5（会写 `if` 即可） |
| Boss | 100+ 种粒子怪物 |
| 关键 NPC | 暴击星 / 伤害心 / 状态光环 / 经验绿点 |
| 核心技能 | `shouldRender(Particle)` |
| 奖励 | 帧率 +30%，画面清爽 |

---

#### 🧙 完整技能卷轴（62 行）

```java
package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

public final class ParticleFilter {

    private ParticleFilter() {}   // 技能导师：禁止召唤本职业

    // ========== 技能 1：单只怪物鉴定 ==========
    public static boolean shouldRender(Particle particle) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.particlesEnabled) return true;   // 总开关关 → 全部放行

        String name = particle.getClass().getName().toLowerCase();

        // 青色星星（暴击 NPC）
        if (name.contains("critparticle")) {
            return cfg.keepCritParticles;
        }

        // 红心（伤害指示 NPC）
        if (name.contains("damageparticle")) {
            return cfg.keepDamageParticles;
        }

        // 喷溅云 + 状态光环（药水 NPC）
        if (name.contains("effectparticle")
                || name.contains("entityeffectparticle")) {
            return cfg.keepPotionParticles;
        }

        // 绿点（经验球 NPC）
        if (name.contains("experienceorbparticle")) {
            return cfg.keepXpParticles;
        }

        // 其余怪物：全部驱逐
        return false;
    }

    // ========== 技能 2：投射物特效鉴定 ==========
    public static boolean shouldRender(Particle particle, Entity source) {
        if (shouldRender(particle)) return true;
        if (source == null) return false;

        // 投射物本体的飞行特效（雪球、末影珍珠、喷溅药水、经验瓶、箭）
        // 这些是"重要 NPC 身边的随从"，得放行
        if (source instanceof EnderPearlEntity)       return true;
        if (source instanceof SnowballEntity)         return true;
        if (source instanceof PotionEntity)           return true;
        if (source instanceof ExperienceBottleEntity) return true;
        if (source instanceof ArrowEntity)            return true;

        return false;
    }
}
```

---

#### 🧙 技能解读（按"副本任务书"读）

##### 第 1 段：职业声明

```java
public final class ParticleFilter {
    private ParticleFilter() {}
```

- **`public final class`**（公开 + 最终）
  - 你的职业叫"粒子清道夫"
  - **`final`** = 转职后**终身不改**，不能 `extends ParticleFilter`（无法继承）
- **`private ParticleFilter() {}`**（私有构造器）
  - 技能导师告诉你："这个职业是**工具职业**，不准召唤实体"
  - 等于"清道夫本人不能被召唤出来打架，只能用静态技能"
  - 任何写 `new ParticleFilter()` 的人 → **编译报错**

##### 第 2 段：技能 1 —— 单只怪物鉴定

```java
public static boolean shouldRender(Particle particle) {
    PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
    if (!cfg.particlesEnabled) return true;
    String name = particle.getClass().getName().toLowerCase();
    ...
}
```

> 🗡️ **技能说明**：`shouldRender` = "这只怪物该不该放行？"
> - 参数 `Particle particle` = 一只被"鉴定请求"送来的粒子怪物
> - 返回 `boolean` = `true`（放行）或 `false`（驱逐）

**逐行拆解**：

- **`PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();`**
  - 打开 6.3 的**控制面板**，读取当前玩家设置
  - 5 个开关：`particlesEnabled` / `keepCritParticles` 等
  - 把面板塞进 `cfg` 这个小背包里方便后面查

- **`if (!cfg.particlesEnabled) return true;`**
  - 总开关 `particlesEnabled` 关闭？**全部放行**（不过滤）
  - `!` = 取反（3.5 学过）
  - 这条逻辑是"**总闸门**"：关了就别管后面的精细判断

- **`String name = particle.getClass().getName().toLowerCase();`**
  - **`particle.getClass()`**（3.7 学过）= 拿到这只粒子的"职业证明"
  - **`.getName()`** = 读取职业全名，比如 `net.minecraft.client.particle.CritParticle`
  - **`.toLowerCase()`** = 统一小写（防止不同 yarn 版本大小写差异）
  - 存到 `name` 这张"身份证复印件"上

##### 第 3 段：4 个"重要 NPC"识别

```java
if (name.contains("critparticle"))        return cfg.keepCritParticles;
if (name.contains("damageparticle"))      return cfg.keepDamageParticles;
if (name.contains("effectparticle") ...)  return cfg.keepPotionParticles;
if (name.contains("experienceorbparticle")) return cfg.keepXpParticles;
```

- **`name.contains("...")`**（3.5 学的 String 方法）
  - 问字符串："你里面**包含**这个子串吗？"
  - `"CritParticle".contains("critparticle")` → `true`（因为小写后匹配）
  - 这种**模糊匹配**比"完整路径比对"宽松，跨版本更稳
- **`return cfg.keepXxxParticles`**
  - 玩家面板上的开关决定"放不放行"
  - 玩家说"我要看暴击" → 开关是 `true` → 放行
  - 玩家说"我看腻了" → 开关是 `false` → 驱逐

##### 第 4 段：默认驱逐

```java
return false;
```

- 走到这里 = **前面 4 个 if 都没匹配上**
- 意味着这只怪物是"未被列入白名单"的杂兵
- 驱逐！返回 `false`（不渲染）
- 整个 mod 的**核心思想**就藏在这行：**白名单制**——只放行名单上的，其他全清

##### 第 5 段：技能 2 —— 投射物随从鉴定

```java
public static boolean shouldRender(Particle particle, Entity source) {
    if (shouldRender(particle)) return true;
    if (source == null) return false;

    if (source instanceof EnderPearlEntity)       return true;
    if (source instanceof SnowballEntity)         return true;
    if (source instanceof PotionEntity)           return true;
    if (source instanceof ExperienceBottleEntity) return true;
    if (source instanceof ArrowEntity)            return true;

    return false;
}
```

> 🗡️ **技能说明**：`shouldRender(粒子, 来源实体)` = "这只粒子的'主人'是不是投射物？"
> - 多了一个参数 `Entity source` = 粒子的"召唤者"
> - 比如：雪球砸出去时**带出的雪花粒子** → 召唤者是 `SnowballEntity`（雪球实体）→ 放行

**逐行拆解**：

- **`if (shouldRender(particle)) return true;`**
  - 先让技能 1 鉴定一遍
  - 已经是"重要 NPC"（暴击/伤害/药水/经验）？直接放行
- **`if (source == null) return false;`**
  - 没记录召唤者（粒子自己蹦出来的） → 驱逐
  - 防止 NullPointerException（3.5 学过）
- **`instanceof`**（3.7 学过）= "这个对象是某个类的实例吗？"
  - `source instanceof SnowballEntity` = "这个 source 是一只雪球吗？"
  - 5 个投射物类（雪球/末影珍珠/喷溅药水/经验瓶/箭）都是"重要 NPC 的随从"，放行

> 💡 **为什么要分两个 `shouldRender`？**
> - **技能 1**（单参数）= 通用鉴定（看粒子自己）
> - **技能 2**（双参数）= 投射物专属鉴定（看粒子的"主人"）
> - Mixin 那边（6.6 会讲）会**先调技能 2**，技能 2 拿不定主意时再调技能 1
> - 相当于"先查'随从表'，再查'本体表'"

---

#### 🗺️ 副本怪物图鉴（1.20.6 粒子类名速查）

| 关键字（`contains` 匹配） | 视觉 | 用途 | 处理方式 |
|---|---|---|---|
| `critparticle` | 青色星星 ✦ | 暴击反馈 | ✅ 保留（看配置） |
| `damageparticle` | 红心 ❤ | 伤害指示 | ✅ 保留（看配置） |
| `effectparticle` | 喷溅云 ☁ | 喷溅药水 | ✅ 保留（看配置） |
| `entityeffectparticle` | 围绕光点 ✨ | 状态效果 | ✅ 保留（看配置） |
| `experienceorbparticle` | 绿点 ⬢ | 经验球 | ✅ 保留（看配置） |
| `heartparticle` | 粉红爱心 💕 | 动物繁殖 | ❌ 驱逐（PvP 没用） |
| `sweepattackparticle` | 弧形 🌙 | 剑横扫 | ❌ 驱逐（容易被误判为暴击） |
| `flameparticle` | 火焰 🔥 | 熔炉/岩浆 | ❌ 驱逐 |
| `smokeparticle` | 烟 💨 | 燃烧 | ❌ 驱逐 |
| `bubbleparticle` | 气泡 💧 | 水下 | ❌ 驱逐 |
| 其他 100+ 种 | 各种 | 各种 | ❌ 驱逐 |

> 💡 **小知识**：Minecraft 1.20.6 里粒子类有 100+ 种，全部写 `if` 会累死。用 **`contains` + 小写** 模糊匹配 4 个关键字就够，省事且稳定。

---

#### 🏆 副本通关总结

`ParticleFilter.java` = **粒子迷宫副本的"清道夫职业"**：
- **职业特性**：纯工具类，不能 `new`
- **技能 1**（`shouldRender(particle)`）= **白名单制**——只放行 4 个重要 NPC（暴击/伤害/药水/经验）
- **技能 2**（`shouldRender(particle, source)`）= **白名单制 v2**——再放行 5 种投射物随从
- **核心思想**：**白名单**（白名单上的放行，其他全清），不要尝试黑名单

> 🚨 **勇者心得**：
> 1. **不要漏写小写转换** —— yarn 映射改了类名大小写，整套过滤就崩
> 2. **不要写 `equals("CritParticle")`** —— 不同版本类名路径可能变，模糊匹配最稳
> 3. **`instanceof` 顺序无关** —— 5 个投射物没有父子继承关系，怎么写都 OK
> 4. **未来想加新粒子？** 在 4 个 `if` 后面加一个 `if` 即可，别动其他代码
### 6.5 `EntityFilter.java` —— 副本 ②：远视结界

#### 🗺️ 副本背景

> *"勇者大人，粒子迷宫清完了，但新的麻烦又来了——*
>
> *王国的战场上到处是**远视实体**（远处的敌对生物、动物、展示框、画、盔甲架……），它们刷在你的视野尽头，看似无害，但 GPU 不知道啊！每帧都把它们画一遍，**白白浪费 30% 算力**。*
>
> *但你**不能把所有远视实体都屏蔽**——战利品（矿物掉落物）必须看见，敌对玩家必须看见（你不会把敌人误判为矿物吧？），BOSS（凋灵、末影龙）也必须看见。*
>
> *进入**远视结界**副本，召唤一道**16 格的视野结界**。结界内的实体可见，结界外的实体隐身（不渲染）。但结界里有几位'VIP 通缉犯'必须永驻——白名单管理交给你了。"*
>
> —— 王国军需官

#### 🧙 副本档案

| 项目 | 内容 |
|---|---|
| 副本名 | **远视结界**（Far Sight Barrier） |
| 推荐等级 | Lv.8（会用 `instanceof` + `Set`） |
| Boss | 200+ 种实体（动物 / 怪物 / 物品展示） |
| 关键 NPC | 玩家 / 村民 / 投射物 / 矿物 / BOSS |
| 核心技能 | `shouldCull(Entity)` + `isMineralItem` + `isWithinRange` |
| 奖励 | 帧率 +20%，战斗视野清晰 |

---

#### 🧙 完整技能卷轴（100 行）

```java
package com.pvp.optimize.entity;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Set;

public final class EntityFilter {

    private EntityFilter() {}

    // ===== 通缉犯白名单：矿物战利品 =====
    private static final Set<Item> ALLOWED_MINERALS = Set.of(
            Items.COAL,            // 煤炭
            Items.IRON_INGOT,      // 铁锭
            Items.GOLD_INGOT,      // 金锭
            Items.DIAMOND,         // 钻石
            Items.EMERALD,         // 绿宝石
            Items.NETHERITE_INGOT, // 下界合金
            Items.QUARTZ,          // 下界石英
            Items.LAPIS_LAZULI,    // 青金石
            Items.REDSTONE,        // 红石
            Items.COPPER_INGOT     // 铜锭
    );

    /**
     * 返回 true 表示这个实体应该被剔除（不渲染）
     */
    public static boolean shouldCull(Entity entity) {
        if (!PvPOptimizeConfig.get().entityCullingEnabled) return false;  // 总开关关

        // VIP 1：玩家（自己和他人）
        if (entity instanceof PlayerEntity) return false;

        // VIP 2：村民（交易要靠他）
        if (entity instanceof VillagerEntity) return false;

        // VIP 3：投射物（飞行轨迹必须看见）
        if (entity instanceof SnowballEntity)         return false;
        if (entity instanceof EnderPearlEntity)       return false;
        if (entity instanceof ArrowEntity)            return false;
        if (entity instanceof PotionEntity)           return false;
        if (entity instanceof ExperienceBottleEntity) return false;
        if (entity instanceof EggEntity)              return false;

        // VIP 4：矿物战利品
        if (isMineralItem(entity)) return false;

        // VIP 5：BOSS（凋灵、末影龙）
        if (entity instanceof WitherEntity)        return false;
        if (entity instanceof EnderDragonEntity)   return false;

        // 其他所有杂兵：距离 > 16 格就剔除
        return !isWithinRange(entity);
    }

    /** 通缉犯鉴定：是不是矿物掉落物 */
    private static boolean isMineralItem(Entity entity) {
        if (!(entity instanceof ItemEntity item)) return false;  // 不是掉落物 → 否
        ItemStack stack = item.getStack();
        if (stack.isEmpty()) return false;                       // 物品栏空 → 否
        return ALLOWED_MINERALS.contains(stack.getItem());       // 命中白名单？
    }

    /** 距离鉴定：实体在 16 格内吗？ */
    private static boolean isWithinRange(Entity entity) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return true;       // 玩家没上线（罕见）→ 默认可见
        double distSq = mc.player.squaredDistanceTo(entity);  // 平方距离
        double max = PvPOptimizeConfig.get().cullDistance;    // 16.0
        return distSq <= max * max;              // 平方比较（避免开方）
    }
}
```

---

#### 🧙 技能解读（按"结界任务书"读）

##### 第 1 段：职业声明 + 矿物白名单

```java
public final class EntityFilter {
    private EntityFilter() {}
```

- 和 6.4 的 `ParticleFilter` 一样：**工具职业**，`final` 不能继承，`private` 构造器不能 `new`

```java
private static final Set<Item> ALLOWED_MINERALS = Set.of(
        Items.COAL, Items.IRON_INGOT, ...
);
```

> 🗡️ **技能说明**：`ALLOWED_MINERALS` = **通缉犯白名单**
> - 10 种矿物：煤炭 / 铁 / 金 / 钻石 / 绿宝石 / 下界合金 / 下界石英 / 青金石 / 红石 / 铜锭
> - 这些是 PvP 中**真正有用的战利品**，必须永驻视野
> - 排除：苹果、面包、腐肉、雪球等"无价值物品"（会刷屏且没用）

- **`Set.of(...)`**（3.7 学过）= Java 9+ 不可变集合字面量
  - 相当于"一个写死的名单表"
  - 不能 `add` / `remove`（不可变）
  - O(1) 查找（哈希表）

- **`private static final`** = 这张名单表**全副本共享一份，初始化后永不更改**

##### 第 2 段：核心技能 `shouldCull`（结界判定）

```java
public static boolean shouldCull(Entity entity) {
```

> 🗡️ **技能说明**：`shouldCull` = "**这只实体应该被'遮蔽'吗？**"
> - 返回 `true` = 剔除（不渲染）
> - 返回 `false` = 保留（继续渲染）

**核心结构 = 4 道关卡**：

```
关卡 1：总开关关了吗？         → 关：放行（不剔除）
关卡 2：白名单 VIP 吗？        → 是：放行（永驻）
关卡 3：白名单矿物吗？         → 是：放行
关卡 4：BOSS 吗？              → 是：放行
关卡 5：剩下杂兵：> 16 格？    → 是：剔除
```

**逐关拆解**：

- **关卡 1：总开关**
  ```java
  if (!PvPOptimizeConfig.get().entityCullingEnabled) return false;
  ```
  - 总开关关 → **全部放行**（一个都不剔）
  - 玩家的"紧急逃生门"

- **关卡 2：VIP 实体（玩家 / 村民 / 投射物）**
  ```java
  if (entity instanceof PlayerEntity) return false;
  if (entity instanceof VillagerEntity) return false;
  if (entity instanceof SnowballEntity) return false;
  ...
  ```
  - 13 个白名单类（1 玩家 + 1 村民 + 6 投射物 + 1 矿物逻辑 + 2 BOSS + ...）
  - **`instanceof`**（3.7 学过）= "这个 entity 是 XXX 类型吗？"
  - 这些实体**永不剔除**（不看距离）

> 💡 **为什么不把"矿物检查"放进白名单？**
> - 矿物是 **`ItemEntity` 类型**（掉落物），但 ItemEntity 里有 100+ 种物品
> - 不能"凡是 ItemEntity 就放行"——苹果、面包、腐肉这些无价值物品也算 ItemEntity
> - 所以要再调 `isMineralItem` 二次鉴定：是不是 ItemEntity → 是不是矿物 → 才放行

- **关卡 3：矿物战利品**
  ```java
  if (isMineralItem(entity)) return false;
  ```

- **关卡 4：BOSS**
  ```java
  if (entity instanceof WitherEntity) return false;
  if (entity instanceof EnderDragonEntity) return false;
  ```
  - 凋灵 / 末影龙：PvP 玩家必须看到血条和位置 → 永不剔除
  - 未来加新 BOSS？继续 `if (entity instanceof X) return false;`

- **关卡 5：杂兵距离判定**
  ```java
  return !isWithinRange(entity);
  ```
  - 走到这里 = **没在白名单上的杂兵**（动物 / 敌对生物 / 画 / 展示框 / 盔甲架 / 无价值掉落物……）
  - `!isWithinRange(entity)` = "**不在 16 格内吗？**"
  - 是 → 剔除（`true`）；否 → 保留（`false`）

##### 第 3 段：通缉犯鉴定 `isMineralItem`

```java
private static boolean isMineralItem(Entity entity) {
    if (!(entity instanceof ItemEntity item)) return false;
    ItemStack stack = item.getStack();
    if (stack.isEmpty()) return false;
    return ALLOWED_MINERALS.contains(stack.getItem());
}
```

> 🗡️ **技能说明**：`isMineralItem` = "**这只实体是 10 种矿物之一吗？**"
> - 私有工具方法（`private static`，6.4 学过"工具职业"的写法）

**逐行拆解**：

- **`!(entity instanceof ItemEntity item)`**（Java 16+ 模式匹配，3.13 学过）
  - 拆解：`entity instanceof ItemEntity item` = "entity 是 ItemEntity 吗？是的话**同时**把 entity 赋给 `item` 这个变量"
  - 加上 `!(...)` 和 `return false` = "**不是 ItemEntity 直接 false**"
  - 一行完成"类型检查 + 类型转换 + 否决返回"

> ⚠️ **这是 Java 16+ 语法**！Minecraft 1.20.6 强制 Java 21，可以用
> 旧 Java 写法：`ItemEntity item = (ItemEntity) entity; if (item == null) return false;`

- **`ItemStack stack = item.getStack();`**
  - `ItemStack` = Minecraft 的"物品栏格子"类型
  - 每个 ItemEntity 身上都**背着一袋物品**，那就是它的 `getStack()`

- **`stack.isEmpty()`**（3.5 学过）
  - 物品栏空？直接 false（避免接下来 `getItem()` 抛 NPE）

- **`ALLOWED_MINERALS.contains(stack.getItem())`**
  - `stack.getItem()` 取出物品栏里的物品（Item 类型）
  - `.contains(...)` 查白名单表（O(1)）
  - 在表里 → `true`（是矿物），不在 → `false`

##### 第 4 段：距离鉴定 `isWithinRange`

```java
private static boolean isWithinRange(Entity entity) {
    net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
    if (mc.player == null) return true;
    double distSq = mc.player.squaredDistanceTo(entity);
    double max = PvPOptimizeConfig.get().cullDistance;
    return distSq <= max * max;
}
```

> 🗡️ **技能说明**：`isWithinRange` = "**这只实体在 16 格内吗？**"
> - 用**平方距离**比大小（避免开方运算）

**逐行拆解**：

- **`MinecraftClient.getInstance()`** = 拿到游戏客户端"主控台"单例
  - 类比：副本里需要"询问管理员当前玩家位置"，管理员就是 `mc`
- **`if (mc.player == null) return true;`**
  - 玩家没上线（比如刚启动）→ 默认"在范围内"（不剔除），保守起见
- **`mc.player.squaredDistanceTo(entity)`**
  - 计算玩家和实体的**平方距离**（不开方！）
  - 性能优化：每帧可能有 100+ 实体要判定，省掉 `Math.sqrt` 性能**提升 3-5 倍**
- **`max * max`** = 配置里 `cullDistance`（默认 16.0）的平方 = 256.0
- **`distSq <= max * max`**
  - 等价于 `sqrt(distSq) <= max`（即"真实距离 ≤ 16"）
  - 但**省掉了开方**

> 💡 **为什么不直接 `mc.player.distanceTo(entity)`？**
> - `distanceTo` 内部会 `Math.sqrt`（求平方根）
> - 我们只要"比大小"，不需要真实距离
> - **勾股定律**告诉我们：a² + b² = c²，比较 c² 和 max² 跟比较 c 和 max 是等价的
> - 性能优化原则：**能不计算就不计算**

---

#### 🗺️ 副本结界图鉴（实体白名单速查）

| 类型 | 类名关键字 | 处理方式 | 原因 |
|---|---|---|---|
| 玩家 | `PlayerEntity` | ✅ 永驻 | PvP 核心 |
| 村民 | `VillagerEntity` | ✅ 永驻 | 交易要靠他 |
| 雪球 | `SnowballEntity` | ✅ 永驻 | 看飞行轨迹 |
| 末影珍珠 | `EnderPearlEntity` | ✅ 永驻 | 看传送轨迹 |
| 箭 | `ArrowEntity` | ✅ 永驻 | 看箭矢方向 |
| 喷溅药水 | `PotionEntity` | ✅ 永驻 | 看药水落点 |
| 经验瓶 | `ExperienceBottleEntity` | ✅ 永驻 | 看经验轨迹 |
| 鸡蛋 | `EggEntity` | ✅ 永驻 | 看鸡蛋轨迹 |
| 矿物战利品 | `ItemEntity` + 10 种 | ✅ 永驻 | 战利品必须看见 |
| 凋灵 | `WitherEntity` | ✅ 永驻 | BOSS |
| 末影龙 | `EnderDragonEntity` | ✅ 永驻 | BOSS |
| 动物 | 牛/羊/猪/鸡… | ❌ >16 格剔除 | PvP 无关 |
| 敌对生物 | 僵尸/骷髅/苦力怕… | ❌ >16 格剔除 | 远视没必要 |
| 画/展示框/盔甲架 | `PaintingEntity` 等 | ❌ >16 格剔除 | 装饰物 |
| 无价值掉落物 | 苹果/面包/腐肉… | ❌ >16 格剔除 | 没用还刷屏 |

---

#### 🏆 副本通关总结

`EntityFilter.java` = **远视结界副本的"结界术士职业"**：
- **职业特性**：纯工具类，私有构造器
- **白名单表**（`ALLOWED_MINERALS`）= 10 种矿物，Set 存储 O(1) 查找
- **核心技能**（`shouldCull`）= 5 道关卡：总开关 → VIP 实体 → 矿物 → BOSS → 距离判定
- **辅助技能**（`isMineralItem`）= 矿物鉴定（用 Java 16+ 模式匹配）
- **辅助技能**（`isWithinRange`）= 距离鉴定（用平方距离避免开方）

> 🚨 **勇者心得**：
> 1. **白名单制 vs 黑名单制** —— 6.4 和 6.5 都用白名单，**安全**。黑名单（"剔除 X 实体"）容易漏剔新版本加的实体
> 2. **不要把所有 ItemEntity 都保留** —— 苹果、面包、腐肉这些会刷屏
> 3. **距离用平方比较** —— 不要调 `Math.sqrt`，浪费性能
> 4. **未来加新 VIP**？在 `shouldCull` 里加一个 `if (entity instanceof X) return false;` 即可
> 5. **未来调视野距离**？改 `gradle.properties` 里的 `cullDistance` 默认值，或在 Mod Menu 里改
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

### 6.7 `OverlayHud.java` —— 副本 ④：魔法绘卷

#### 🗺️ 副本背景

> *"勇者，你已经把神器附魔到法器上了。但还有最后一件事——*
>
> *王国军需官需要 2 块**魔法绘卷**贴在你的视野里：*
> - **红色滤镜绘卷**（全屏微红）—— 让你在 PvP 紧张时血液沸腾
> - **状态面板绘卷**（屏幕左上角文字）—— 显示你所有功能开关当前是开是关
>
> *这两块绘卷要**每帧重画**（游戏画面每动一下都要更新），还要**响应 4 个遥控器**（H/P/K/J）的信号：*
> - **H** 键 → 切换状态面板显示
> - **P** 键 → 切换粒子总开关 + 立即保存
> - **K** 键 → 切换实体剔除总开关 + 立即保存
> - **J** 键 → 切换红色滤镜 + 立即保存*
>
> *进入**魔法绘卷**副本，召唤你的"绘卷师"职业。"*
>
> —— 王国军需官

#### 🧙 副本档案

| 项目 | 内容 |
|---|---|
| 副本名 | **魔法绘卷**（Magic Scroll） |
| 推荐等级 | Lv.10（会用 lambda + 事件订阅） |
| 绘卷师 | `OverlayHud`（87 行） |
| 2 块绘卷 | 红色滤镜 + 状态面板 |
| 4 个遥控器 | H / P / K / J（6.3 注册的 KeyBinding） |
| 关键 API | `ClientTickEvents`（每 tick 事件）+ `HudRenderCallback`（每帧渲染事件） |
| 奖励 | 玩家可"肉眼"看到 mod 在工作 |

---

#### 🧙 完整技能卷轴（87 行）

```java
package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class OverlayHud {

    private static boolean hudVisible = false;   // 状态面板是否显示

    public static void register() {
        // 订阅"每 tick 触发"事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

            // H 键：切换状态面板
            while (PvPOptimizeConfig.OPEN_HUD.wasPressed()) {
                hudVisible = !hudVisible;
            }
            // P 键：切换粒子总开关 + 立即保存
            while (PvPOptimizeConfig.TOGGLE_PARTICLES.wasPressed()) {
                cfg.particlesEnabled = !cfg.particlesEnabled;
                PvPOptimizeConfig.save();
            }
            // K 键：切换实体剔除总开关
            while (PvPOptimizeConfig.TOGGLE_CULL.wasPressed()) {
                cfg.entityCullingEnabled = !cfg.entityCullingEnabled;
                PvPOptimizeConfig.save();
            }
            // J 键：切换红色滤镜
            while (PvPOptimizeConfig.TOGGLE_RED_OVERLAY.wasPressed()) {
                cfg.redOverlayEnabled = !cfg.redOverlayEnabled;
                PvPOptimizeConfig.save();
            }
        });

        // 订阅"HUD 渲染时回调"事件
        HudRenderCallback.EVENT.register(OverlayHud::render);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;   // 不在游戏中不画

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

        // ========== 1. 全屏红色滤镜 ==========
        if (cfg.redOverlayEnabled) {
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int color = cfg.overlayColor;
            int alpha = (int) (cfg.overlayOpacity * 255.0f);
            int argb = (alpha << 24) | (color & 0x00FFFFFF);
            ctx.fill(0, 0, w, h, argb);   // 填满整个屏幕
        }

        // ========== 2. 状态面板 ==========
        if (!hudVisible) return;          // 没开就不画

        int x = 4, y = 4;
        int lineHeight = 12, padding = 4;

        // 4 行文字
        Text[] lines = {
                Text.literal("PvP-Optimize"),
                Text.literal("粒子: " + onOff(cfg.particlesEnabled)
                        + "  (暴击=" + onOff(cfg.keepCritParticles)
                        + " 受击=" + onOff(cfg.keepDamageParticles)
                        + " 药水=" + onOff(cfg.keepPotionParticles)
                        + " 经验=" + onOff(cfg.keepXpParticles) + ")"),
                Text.literal("实体剔除: " + onOff(cfg.entityCullingEnabled)
                        + "  半径=" + ((int) cfg.cullDistance) + "格"),
                Text.literal("红色滤镜: " + onOff(cfg.redOverlayEnabled)
                        + "  透明度=" + String.format("%.2f", cfg.overlayOpacity)),
        };

        // 计算背景框宽度
        int width = 0;
        for (Text t : lines) {
            int w = mc.textRenderer.getWidth(t);
            if (w > width) width = w;
        }
        int boxH = lines.length * lineHeight + padding * 2;

        // 画半透明黑底
        ctx.fill(x, y, x + width + padding * 2, y + boxH, 0x90000000);
        // 画文字
        for (int i = 0; i < lines.length; i++) {
            ctx.drawText(mc.textRenderer, lines[i], x + padding, y + padding + i * lineHeight, 0xFFFFFFFF, false);
        }
    }

    private static String onOff(boolean b) { return b ? "开" : "关"; }
}
```

---

#### 🧙 技能解读（按"绘卷师任务书"读）

##### 第 1 段：职业声明

```java
public final class OverlayHud {
    private static boolean hudVisible = false;
```

- **`public final class`** = 你的职业叫"魔法绘卷师"，转职后不可改
- **`private static boolean hudVisible`** = 一块**私有的画布开关**
  - `hudVisible` 记录"状态面板绘卷现在显示吗？"
  - `static` = 全副本**共享这一个状态**
  - 没在 `Data` 类里——因为它**不需要持久化**（按一次 H 就切换，不存盘）

##### 第 2 段：注册（`register()` 方法）

> 这是 mod 启动时被 6.2 `onInitializeClient()` 调用的入口

**第 2 段上：订阅"每 tick"事件**

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
    while (PvPOptimizeConfig.OPEN_HUD.wasPressed()) {
        hudVisible = !hudVisible;
    }
    ...
});
```

> 🗡️ **技能说明**：每 1/20 秒（一个 tick）调一次这个 lambda
> - **`ClientTickEvents.END_CLIENT_TICK`** = Fabric 的"游戏每 tick 末尾"事件
> - **`.register(lambda)`** = 订阅（3.9 学过"接口回调"）
> - `client -> { ... }` = 3.14 学的 lambda：client 是 Fabric 传给我们的客户端对象，我们不用，所以省略 `client ->` 改成 `client ->`

**`while (... .wasPressed())`**（4 个键位都长一样）：

- **`wasPressed()`** = Minecraft 的"刚刚被按下"判断
  - 为什么用 `while` 而不是 `if`？因为按住键可能**一 tick 内触发多次**，`while` 确保每次都处理
  - 比如按 H：第一个 tick 进入循环 → 处理 → 下次 tick `wasPressed()` 变 `false` → 退出循环
- **`hudVisible = !hudVisible`** = 切换布尔值（3.5 学过 `!` 取反）
- **P/K/J 键额外调用 `save()`** = 立即写回 JSON（不然下次开游戏设置丢了）
- **H 键不调 save()** = 因为 `hudVisible` 不在 `Data` 里，是"瞬时状态"

> 💡 **设计取舍**：
> - H 切换的"面板是否显示"是**视觉状态**，玩家期望"按一次立即切，不存盘"
> - P/K/J 切换的是**功能开关**，期望"切换 + 持久化（下次开游戏还有）"
> - 所以 H 不 save，P/K/J 都 save

**第 2 段下：订阅"HUD 渲染"事件**

```java
HudRenderCallback.EVENT.register(OverlayHud::render);
```

> 🗡️ **技能说明**：每帧（1/60 秒）画 HUD 时调一次 `render` 方法
> - **`HudRenderCallback.EVENT`** = Fabric 的"HUD 渲染时触发"事件
> - **`.register(OverlayHud::render)`** = 3.7 学过的"方法引用"
>   - 等价于 `.register((ctx, dt) -> OverlayHud.render(ctx, dt))`
>   - 双冒号 `::` 是方法引用，**让 lambda 短一点**

##### 第 3 段：渲染（`render()` 方法）

> 绘制 2 块绘卷的核心方法。每帧调用一次。

**第 3 段上：环境检查 + 全屏红色滤镜**

```java
private static void render(DrawContext ctx, float tickDelta) {
    MinecraftClient mc = MinecraftClient.getInstance();
    if (mc.player == null || mc.world == null) return;
```

- **`DrawContext ctx`** = Minecraft 的"画图工具"（3.15 链式调用）
- **`float tickDelta`** = "距离上一帧的时间差"（秒），用于动画
- **`mc.player == null`** = 玩家没上线（在主菜单）→ 不画
- **`mc.world == null`** = 没世界（在主菜单）→ 不画

```java
if (cfg.redOverlayEnabled) {
    int w = mc.getWindow().getScaledWidth();
    int h = mc.getWindow().getScaledHeight();
    int color = cfg.overlayColor;
    int alpha = (int) (cfg.overlayOpacity * 255.0f);
    int argb = (alpha << 24) | (color & 0x00FFFFFF);
    ctx.fill(0, 0, w, h, argb);   // 填满整个屏幕
}
```

> 🗡️ **技能说明**：画"全屏红色滤镜"（在 0,0 到 w,h 画个半透明矩形）
> - **`ctx.fill(x1, y1, x2, y2, argb)`** = 在矩形区域填颜色
> - **`(0, 0, w, h)`** = 从屏幕左上角 (0,0) 到右下角 (w, h) 填满
> - **`alpha << 24`**（位移运算）= 把 alpha（0-255）放到整数的**最高 8 位**
> - **`color & 0x00FFFFFF`**（按位与）= 把原 color 的**最低 24 位**（RGB）保留，屏蔽掉它的 alpha
> - **`|`**（按位或）= 把 alpha 和 RGB 拼成一个 32 位整数

**ARGB 构造图解**（3.5 学的位运算）：

```
原 color  (0x10FF1010): 0001 0000  1111 1111  0001 0000  0001 0000
                      ← A=0x10 →←── R=0xFF ──→← G=0x10 →← B=0x10 →

color & 0x00FFFFFF:    0000 0000  1111 1111  0001 0000  0001 0000
                      (清掉 A)    ←── R ──→    ← G →     ← B →

alpha=0x26 (38):       0010 0110  0000 0000  0000 0000  0000 0000
                      (左移 24 位)  (最低 24 位全 0)

OR 之后:               0010 0110  1111 1111  0001 0000  0001 0000
                      ← A=0x26 →←── R=0xFF ──→← G=0x10 →← B=0x10 →
```

> 💡 **为什么不用现成方法？**
> - Minecraft 没提供 `setColor(red, green, blue, alpha)` 这种方便方法
> - 颜色必须自己**位运算拼** 32 位整数
> - 这是 Minecraft 的"祖传 API"，从 1.0 沿用至今

**第 3 段下：状态面板**

```java
if (!hudVisible) return;          // 没开就不画

Text[] lines = {
        Text.literal("PvP-Optimize"),
        Text.literal("粒子: " + onOff(cfg.particlesEnabled) + " ..."),
        ...
};
```

> 🗡️ **技能说明**：画"状态面板绘卷"（屏幕左上角的 4 行文字）
> - **`Text[] lines`**（3.5 数组）= 4 行文字，按顺序显示
> - **`Text.literal("...")`** = 创建一个"普通文本"对象（不支持翻译，但稳定）
> - **`onOff(boolean)`** = 自定义小工具方法：`true → "开"`、`false → "关"`
> - **`String.format("%.2f", cfg.overlayOpacity)`** = 把 0.15 格式化为 "0.15" 字符串（保留 2 位小数）

```java
// 1. 计算背景框宽度
int width = 0;
for (Text t : lines) {
    int w = mc.textRenderer.getWidth(t);
    if (w > width) width = w;
}
int boxH = lines.length * lineHeight + padding * 2;

// 2. 画半透明黑底
ctx.fill(x, y, x + width + padding * 2, y + boxH, 0x90000000);

// 3. 画文字
for (int i = 0; i < lines.length; i++) {
    ctx.drawText(mc.textRenderer, lines[i], x + padding, y + padding + i * lineHeight, 0xFFFFFFFF, false);
}
```

- **先量尺寸，再画背景，再画文字**（固定套路）
- **`mc.textRenderer.getWidth(t)`** = 问 Minecraft 的字体渲染器："这段文字画出来多宽？"
- **`0x90000000`** = "半透明黑"（`90` 十六进制 = `144` 十进制 = 144/255 ≈ 56% 不透明）
- **`0xFFFFFFFF`** = 纯白（不透明白）
- **`false`**（最后一个参数）= 不带阴影（`true` 会带阴影）

> 💡 **为什么先量尺寸？**
> - 不知道文字多宽，就不知道背景框多宽
> - 不知道背景框多宽，就画不好看的"刚好包住文字"的黑底
> - 这是 GUI 编程的"先量后画"固定套路

---

#### 🗺️ 副本 API 速查（Fabric 渲染相关）

| API | 作用 | 触发时机 |
|---|---|---|
| `ClientTickEvents.END_CLIENT_TICK` | 每 tick 末尾触发 | 1/20 秒一次 |
| `HudRenderCallback.EVENT` | HUD 渲染时触发 | 1/60 秒一次（每帧） |
| `DrawContext.fill(x1,y1,x2,y2,argb)` | 填充矩形 | 一次性 |
| `DrawContext.drawText(...)` | 画文字 | 一次性 |
| `mc.textRenderer.getWidth(t)` | 测文字宽度 | 调用时立刻算 |
| `mc.getWindow().getScaledWidth()` | 屏幕逻辑宽度 | 调用时立刻算 |

---

#### 🏆 副本通关总结

`OverlayHud.java` = **魔法绘卷副本的"绘卷师职业"**：
- **职业特性**：纯工具类，`final` 不能继承
- **核心方法** `register()` = 订阅 2 个事件（`END_CLIENT_TICK` + `HudRenderCallback`）
- **4 个键位响应**（H/P/K/J）= 在 tick 事件里检查 `wasPressed()` 并切换状态
- **2 块绘卷** = 红色滤镜（全屏 fill）+ 状态面板（黑底 + 4 行文字）
- **ARGB 颜色构造** = 位运算拼装 32 位颜色整数

> 🚨 **勇者心得**：
> 1. **`while` + `wasPressed()`** 是按键检测的固定写法（不是 `if`）
> 2. **P/K/J 调 `save()`** 持久化；H 不调（视觉状态不存盘）
> 3. **ARGB 用位运算拼**（`<<` 和 `|`）—— Minecraft 祖传 API，没办法
> 4. **先量尺寸再画** —— GUI 编程固定套路
> 5. **方法引用 `OverlayHud::render`** 比写 lambda 短一些
### 6.8 + 6.9 Mod Menu + Cloth Config —— 副本 ⑤：魔法商店

#### 🗺️ 副本背景

> *"勇者，你的技能已经齐全，但还有一个问题——*
>
> *村民们不知道**怎么用**这些开关！他们打开 Minecraft 看到的是一片荒野（默认设置），没法改。*
>
> *王国办了一家**魔法商店**（Mod Menu），让玩家可以**图形化地**配置 mod。但这家商店需要每个 mod 自己提供'**商品目录**'——*
> - **6.8** = 给 Mod Menu 装一个'**店招**'（`PvPOptimizeModMenu`），让 Mod Menu 知道我们这家店存在
> - **6.9** = 我们的'**商品橱窗**'（`PvPOptimizeConfigScreen`），用 Cloth Config 库**装修**橱窗
>
> *进入**魔法商店**副本，给你的 mod 装修一家店铺。"*
>
> —— 王国商业大臣

#### 🧙 副本档案

| 项目 | 内容 |
|---|---|
| 副本名 | **魔法商店**（Magic Shop） |
| 推荐等级 | Lv.10（会用接口 + 链式调用） |
| 店招（6.8） | `PvPOptimizeModMenu`（21 行） |
| 橱窗（6.9） | `PvPOptimizeConfigScreen`（119 行） |
| 装修库 | Mod Menu + Cloth Config（3 个依赖） |
| 商品数 | 10 个配置项（3 个分类） |
| 翻译文件 | `assets/pvp_optimize/lang/zh_cn.json` |

---

#### 🧙 第 1 份卷轴：店招 `PvPOptimizeModMenu.java`（21 行）

> 这份卷轴只做**一件事**——告诉 Mod Menu"我们家在 Mod Menu 列表里有个配置按钮，按钮按下后用这个方法造配置屏幕"。

```java
package com.pvp.optimize.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * 给 Mod Menu 一个"钩子"：当玩家在 Mod Menu 列表里点 PvP-Optimize
 * 的配置按钮时，调这个方法拿到我们的设置屏幕。
 *
 * Mod Menu 10+ 用 ConfigScreenFactory<Screen>（泛型，3.13 学过）
 */
public class PvPOptimizeModMenu implements ModMenuApi {

    @Override   // 3.12 学的注解：表示"重写接口方法"
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        // 返回一个 lambda：parent 是 Mod Menu 传来的"父屏幕"
        // 我们返回 PvPOptimizeConfigScreen.create(parent) 创建的配置屏幕
        // 3.14 学的 lambda：parent -> ... 是单参数箭头函数
        return parent -> PvPOptimizeConfigScreen.create(parent);
    }
}
```

##### 卷轴解读

```java
public class PvPOptimizeModMenu implements ModMenuApi {
```

> 🗡️ **技能说明**：和 Mod Menu 签合同
> - **`implements ModMenuApi`**（3.9 接口）= "我们承诺按 Mod Menu 的接口规范提供配置入口"
> - Mod Menu 启动时会扫描所有实现 `ModMenuApi` 的类，挨个问它们"你家配置屏幕是啥？"

```java
@Override
public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
    return parent -> PvPOptimizeConfigScreen.create(parent);
}
```

> 🗡️ **技能说明**：被 Mod Menu 问时这么回答
> - **`@Override`**（3.12）= "这是重写接口方法"
> - 返回类型 `ConfigScreenFactory<Screen>`：3.13 学的**泛型**，"这是一个能造 `Screen` 类型对象的工厂"
> - **`parent -> PvPOptimizeConfigScreen.create(parent)`**：3.14 学的 lambda
>   - `parent` = Mod Menu 传进来的"父屏幕"（返回键按这个能回 Mod Menu 列表）
>   - 调 `PvPOptimizeConfigScreen.create(parent)` 造我们的设置屏幕

> 💡 **设计精髓**：
> - 6.8 是个"**空壳**"，只负责**对接** Mod Menu
> - 真正的"装修"在 6.9
> - 这种"分层"很常见：外层做协议，内层做实现

---

#### 🧙 第 2 份卷轴：橱窗 `PvPOptimizeConfigScreen.java`（119 行）

> 这是最长的一份卷轴。用 Cloth Config 库把 10 个配置项**装进 3 个分类**，每个分类一个折叠面板。

```java
package com.pvp.optimize.config;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class PvPOptimizeConfigScreen {

    private PvPOptimizeConfigScreen() {}

    public static Screen create(Screen parent) {
        PvPOptimizeConfig.Data data = PvPOptimizeConfig.get();

        // ========== 构建器（3.15 链式调用）==========
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)                                    // 返回键回到这个屏幕
                .setTitle(Text.translatable("config.pvp_optimize.title"));   // 标题从 lang 文件取

        ConfigEntryBuilder eb = builder.entryBuilder();   // 拿"输入项构建器"

        // ========== 分类 1：粒子过滤（5 个布尔开关）==========
        ConfigCategory particles = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.particles"));

        // 每个 addEntry 是一行配置项
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particlesEnabled"),
                        data.particlesEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particlesEnabled = v)   // 用户改值时调用
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepCritParticles"),
                        data.keepCritParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepCritParticles = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepDamageParticles"),
                        data.keepDamageParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepDamageParticles = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepPotionParticles"),
                        data.keepPotionParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepPotionParticles = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepXpParticles"),
                        data.keepXpParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepXpParticles = v)
                .build());

        // ========== 分类 2：实体剔除（1 开关 + 1 滑块）==========
        ConfigCategory culling = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.culling"));

        culling.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.entityCullingEnabled"),
                        data.entityCullingEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.entityCullingEnabled = v)
                .build());

        culling.addEntry(eb.startDoubleField(                          // 浮点输入框
                        Text.translatable("config.pvp_optimize.cullDistance"),
                        data.cullDistance)
                .setDefaultValue(16.0)
                .setMin(1.0).setMax(64.0)                              // 范围 1-64
                .setSaveConsumer(v -> data.cullDistance = v)
                .build());

        // ========== 分类 3：屏幕滤镜（1 开关 + 1 颜色 + 1 透明度）==========
        ConfigCategory overlay = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.overlay"));

        overlay.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.redOverlayEnabled"),
                        data.redOverlayEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.redOverlayEnabled = v)
                .build());

        overlay.addEntry(eb.startIntField(                            // 整数输入框
                        Text.translatable("config.pvp_optimize.overlayColor"),
                        data.overlayColor)
                .setDefaultValue(0x10FF1010)
                .setSaveConsumer(v -> data.overlayColor = v)
                .build());

        overlay.addEntry(eb.startFloatField(                          // 浮点滑块
                        Text.translatable("config.pvp_optimize.overlayOpacity"),
                        data.overlayOpacity)
                .setDefaultValue(0.15f)
                .setMin(0.0f).setMax(1.0f)
                .setSaveConsumer(v -> data.overlayOpacity = v)
                .build());

        // 用户点"完成"时调用
        builder.setSavingRunnable(PvPOptimizeConfig::save);   // 3.14 方法引用
        return builder.build();                                // 构建并返回 Screen
    }
}
```

##### 卷轴解读

**第 1 段：构建器起步**

```java
ConfigBuilder builder = ConfigBuilder.create()
        .setParentScreen(parent)                                    // 返回键回到这个屏幕
        .setTitle(Text.translatable("config.pvp_optimize.title"));
```

> 🗡️ **技能说明**：用 Cloth Config 的构建器（3.15 链式调用）造设置屏幕
> - **`ConfigBuilder.create()`**（3.15 链式起点）= 拿到"屏幕构造器"
> - **`.setParentScreen(parent)`** = 设置"返回键的父屏幕"（玩家按 ESC 回到 `parent`，也就是 Mod Menu 列表）
> - **`.setTitle(Text.translatable("key"))`** = 标题，**去 lang 文件找翻译**
>   - 不写死 `"PvP-Optimize 设置"`，而是写翻译键 `"config.pvp_optimize.title"`
>   - `Text.translatable(...)` 会去 `assets/pvp_optimize/lang/zh_cn.json` 找 `"config.pvp_optimize.title": "PvP-Optimize 设置"`
>   - 改语言（如切到英文）自动显示英文，不动代码

```java
ConfigEntryBuilder eb = builder.entryBuilder();
```

> 🗡️ **技能说明**：从"屏幕构造器"拿到"输入项构造器"
> - 哲学：屏幕是"大盒子"，里面装各种"输入项"
> - 大盒子的工具是 `ConfigBuilder`
> - 输入项的工具是 `ConfigEntryBuilder`（简称 `eb`）

**第 2 段：3 个分类 × 10 个商品**

> Cloth Config 把 10 个配置项**装进 3 个分类**（侧边栏切换），每个分类里面是开关/滑块。

```java
ConfigCategory particles = builder.getOrCreateCategory(
        Text.translatable("config.pvp_optimize.category.particles"));
```

> 🗡️ **技能说明**：拿到（或新建）一个分类
> - **`getOrCreateCategory(...)`** = "有就拿，没有就建"
> - 名字是翻译键 `"config.pvp_optimize.category.particles"` → "粒子过滤"

**第 2 段核心：每行商品 = 4 步固定套路**

```java
particles.addEntry(eb.startBooleanToggle(
                Text.translatable("config.pvp_optimize.particlesEnabled"),
                data.particlesEnabled)
        .setDefaultValue(true)
        .setSaveConsumer(v -> data.particlesEnabled = v)
        .build());
```

**4 步拆解**（这是 Cloth Config 的**固定套路**，所有配置项都长这样）：

| 步骤 | 调用 | 作用 |
|---|---|---|
| 1. 开始造 | `eb.startBooleanToggle(标签, 当前值)` | 造一个"布尔开关"，传入翻译键和当前值 |
| 2. 设默认值 | `.setDefaultValue(true)` | "重置"按钮用 |
| 3. 设保存回调 | `.setSaveConsumer(v -> data.foo = v)` | 3.14 学的 lambda，玩家改值时调 |
| 4. 完工 | `.build()` | 造完，返回一个"商品"对象 |

> - **`addEntry(...)`** = 把"商品"挂到分类下
> - **`.setSaveConsumer(v -> data.foo = v)`** = 3.14 学的 lambda
>   - `v` 是 Cloth Config 传进来的"用户改后的新值"
>   - `data.foo = v` 把新值写回全局 `Data` 对象
>   - 这里**不调 `save()`**，因为玩家可能还在改别项
>   - 真正存盘是点"完成"按钮时（下面的 `setSavingRunnable`）

**3 种输入项类型**：

| 调用 | 类型 | 用途 | 默认值 | 范围 |
|---|---|---|---|---|
| `eb.startBooleanToggle(标签, 值)` | 布尔开关 | on/off 切换 | `true` | - |
| `eb.startIntField(标签, 值)` | 整数输入框 | 整数（如颜色 0x10FF1010） | `0x10FF1010` | - |
| `eb.startDoubleField(标签, 值)` | 双精度浮点 | 距离 16.0 | `16.0` | `1.0`-`64.0` |
| `eb.startFloatField(标签, 值)` | 单精度浮点 | 透明度 0.15 | `0.15f` | `0.0f`-`1.0f` |

**第 3 段：点"完成"时存盘**

```java
builder.setSavingRunnable(PvPOptimizeConfig::save);
return builder.build();
```

> 🗡️ **技能说明**：注册"完成"按钮的回调 + 造完返回屏幕
> - **`setSavingRunnable(PvPOptimizeConfig::save)`** = 3.14 学的**方法引用**
>   - 等价于 `() -> PvPOptimizeConfig.save()`
>   - 玩家点"完成"按钮时调，把当前 `Data` 写盘
> - **`builder.build()`**（3.15 链式收尾）= 构建并返回最终的 `Screen` 对象
>   - 整个 `create()` 方法返回的是 `Screen` 类型

---

#### 🗺️ 魔法商店商品图鉴

| 分类 | 商品（10 个） | 类型 | 默认值 | 范围 |
|---|---|---|---|---|
| 🟢 粒子过滤 | particlesEnabled | 布尔 | 开 | - |
| | keepCritParticles | 布尔 | 开 | - |
| | keepDamageParticles | 布尔 | 开 | - |
| | keepPotionParticles | 布尔 | 开 | - |
| | keepXpParticles | 布尔 | 开 | - |
| 🟡 实体剔除 | entityCullingEnabled | 布尔 | 开 | - |
| | cullDistance | 双精度 | 16.0 | 1.0-64.0 |
| 🔴 屏幕滤镜 | redOverlayEnabled | 布尔 | 开 | - |
| | overlayColor | 整数 | 0x10FF1010 | - |
| | overlayOpacity | 单精度 | 0.15 | 0.0-1.0 |

---

#### 🗺️ 关键 API 速查

| API | 作用 | 出现次数 |
|---|---|---|
| `ConfigBuilder.create()` | 链式起点 | 1 |
| `.setParentScreen(parent)` | 设置返回键父屏幕 | 1 |
| `.setTitle(Text.translatable("..."))` | 标题 | 1 |
| `builder.entryBuilder()` | 拿输入项构造器 | 1 |
| `builder.getOrCreateCategory(...)` | 拿/建分类 | 3（3 个分类） |
| `eb.startBooleanToggle(...)` | 造布尔开关 | 7（5 粒子 + 1 剔除 + 1 滤镜） |
| `eb.startIntField(...)` | 造整数框 | 1（颜色） |
| `eb.startDoubleField(...)` | 造双精度框 | 1（距离） |
| `eb.startFloatField(...)` | 造单精度滑块 | 1（透明度） |
| `.setDefaultValue(...)` | 默认值 | 10 |
| `.setSaveConsumer(lambda)` | 保存回调 | 10 |
| `.setMin(...)` / `.setMax(...)` | 范围 | 2（数字类型才需要） |
| `.build()` | 完工 | 10 |
| `builder.setSavingRunnable(...)` | 完成时回调 | 1 |

---

#### 🏆 副本通关总结

`config/PvPOptimizeModMenu.java` + `config/PvPOptimizeConfigScreen.java` = **魔法商店副本的"店招 + 橱窗"**：

**店招（6.8）= 21 行**
- 实现 `ModMenuApi` 接口
- 返回一个 lambda：被 Mod Menu 问时"我家配置屏幕是 `PvPOptimizeConfigScreen.create(parent)`"

**橱窗（6.9）= 119 行**
- 链式构造 `ConfigBuilder`（屏幕容器）
- 3 个分类（粒子过滤 / 实体剔除 / 屏幕滤镜）
- 10 个配置项（5 开关 + 1 整数 + 2 浮点 + 2 浮点），每个走 4 步套路
- "完成"按钮回调 = `PvPOptimizeConfig::save`（方法引用）

> 🚨 **勇者心得**：
> 1. **6.8 是空壳，6.9 才是主力** —— 6.8 只做协议对接
> 2. **每个配置项都是 4 步套路**（start / setDefault / setSaveConsumer / build）—— 改 1 个字段 = 改 1 个 entry
> 3. **不要在 `setSaveConsumer` 里调 `save()`** —— 那里只是"暂存"，真正存盘点"完成"时统一做
> 4. **`Text.translatable("key")` 不要写死中文** —— 走 lang 文件，改语言不动代码
> 5. **方法引用 `PvPOptimizeConfig::save` 比 lambda 短** —— 3.14 学的
> 6. **未来加新配置项**？在 6.3 `Data` 加字段 → 在 6.9 加一个 `addEntry` → 在 `lang/zh_cn.json` 加翻译
### 6.10 副本 ⑥：实战挑战（6 个试炼）

#### 🗺️ 副本背景

> *"勇者，你已经走完 5 个副本，掌握了 9 份卷轴的全部技能。但要真正成为'清道夫大师'，必须**亲手试炼**——*
>
> *王国军需官给你准备了 6 个试炼任务，每完成 1 个都能加深对前面技能的理解。**禁止复制粘贴**，必须手写代码，才能把知识刻进肌肉记忆。*
>
> *试炼等级从 Lv.1（简单改字）到 Lv.6（综合挑战），完成后你就是真·mod 开发者了。"*
>
> —— 王国军需官

---

#### ⚔️ 试炼 6.1：白名单艺术（Lv.1 · 粒子迷宫）

**任务**：打开 [ParticleFilter.java](file:///C:/Users/16210/Documents/pvp-optimize/src/main/java/com/pvp/optimize/particle/ParticleFilter.java)，把 `name.contains("critparticle")` 改成精确匹配类名 `CritParticle`（用 `endsWith` 而不是 `contains`）。比较两种写法的差异。

**学习目标**：
- 理解 `contains`（模糊）vs `endsWith`（精确）的取舍
- 知道"模糊匹配"为何在跨版本时更稳

**通关要点**：
- `name.endsWith("CritParticle")` 要求类名**以这串结尾**
- vs `name.contains("critparticle")` 只要**包含**这串就匹配
- 想想：未来如果 Minecraft 加个新类 `SomeCritParticleExtra`（重名），两种写法分别会怎样？

---

#### ⚔️ 试炼 6.2：召唤新的 VIP（Lv.2 · 远视结界）

**任务**：在 [EntityFilter.java](file:///C:/Users/16210/Documents/pvp-optimize/src/main/java/com/pvp/optimize/entity/EntityFilter.java) 的白名单里**加入** `WitherSkullEntity`（凋灵骷髅头投射物），让凋灵骷髅的头也保留。

**学习目标**：
- 学会往 `instanceof` 白名单里加新条目
- 复习 `import` 语句怎么写

**通关要点**：
- 凋灵骷髅头在 `net.minecraft.entity.projectile.WitherSkullEntity`（不是 `WitherSkullEntity`！）
- 要先 `import` 这个类
- 然后在 `shouldCull` 方法里加一个 `if`
- 凋灵骷髅头虽然是凋灵的"投射物"，但凋灵实体本身已经被白名单（`WitherEntity`），但它的**头**是独立实体，得单独加

---

#### ⚔️ 试炼 6.3：情报员的留言（Lv.2 · 魔法绘卷）

**任务**：在 [OverlayHud.java](file:///C:/Users/16210/Documents/pvp-optimize/src/main/java/com/pvp/optimize/hud/OverlayHud.java) 的状态面板里**新增一行**"版本：1.0.0"，硬编码就行。

**学习目标**：
- 复习 `Text[] lines` 数组怎么加元素
- 复习"先量尺寸再画"的固定套路

**通关要点**：
- 在 `lines` 数组里加一行 `Text.literal("版本: 1.0.0")`
- 注意 `boxH = lines.length * lineHeight + padding * 2` **自动重算**（因为 `lines.length` 是动态的）
- 不需要改 `width` 计算（`for` 循环会重新跑）

---

#### ⚔️ 试炼 6.4：魔法反转（Lv.3 · 神器仪式）

**任务**：把 `ParticleManagerMixin.java` 里的 `@Inject` 改用 `@Inject(method = "renderParticles", at = @At("HEAD"))` 然后在方法里**反着过滤**（默认全部画，只在 `shouldRender` 返回 false 时调 `ci.cancel()`），观察游戏表现。

**学习目标**：
- 理解 `@At("HEAD")` 和 `@At("TAIL")` 的位置区别
- 体验 `ci.cancel()` 的"取消整个方法"效果（vs `@Redirect` 的"替换调用"）

**通关要点**：
- **HEAD 位置**：方法**最开头**注入，**先于**所有代码执行
- **取消 vs 替换**：
  - `@Redirect` 替换的是**某个方法调用**（精准）
  - `@Inject + ci.cancel()` 取消的是**整个方法**（粗暴）
- 反着过滤会有副作用：因为整个 `renderParticles` 被取消，连**没问题的粒子也不画了**（除非 Redirect 那一关能拦下来）

> ⚠️ **预期现象**：你做完会发现游戏**几乎没有粒子**（因为 HEAD 一刀切了 renderParticles 整个方法）
> - 这是**预期效果**，让你直观感受 `@Inject + cancel` 的"破坏力"
> - 想恢复？把 HEAD 改回 TAIL，把 cancel 改回 removeIf

---

#### ⚔️ 试炼 6.5：新增商品上架（Lv.4 · 综合挑战）

**任务**：在 `PvPOptimizeConfigScreen.java` 的"粒子过滤"分类里**新增一个开关**"保留末影龙粒子"（key: `keepDragonParticles`，默认 true），并让 `ParticleFilter.java` 识别它。

**学习目标**：
- 走完"**新增配置项**"的**完整 6 步流程**（综合考验 6.3/6.4/6.6/6.9 知识）

**完整 6 步清单**：

1. **加 Data 字段**（6.3 控制面板）—— `PvPOptimizeConfig.Data` 加一行 `public boolean keepDragonParticles = true;`
2. **加 load 反序列化**（6.3 档案室）—— `load()` 那串 `DATA.xxx = loaded.xxx;` 加一行
3. **加过滤逻辑**（6.4 粒子迷宫）—— `ParticleFilter.shouldRender` 加一个 `if` 分支
4. **加配置 UI**（6.9 魔法商店）—— `PvPOptimizeConfigScreen` 在 `particles` 分类里加一个 `addEntry`
5. **加翻译**（6.9 lang 文件）—— `zh_cn.json` 和 `en_us.json` 各加一行
6. **更新状态面板**（6.7 魔法绘卷，可选）—— `OverlayHud` 那串加一项

**通关要点**：
- 末影龙吐息的粒子类名包含 `dragonbreath`（你可以用 `name.contains("dragonbreath")`）
- 别忘了 `import` 新类（如果有）
- 改完跑构建命令 → 启动游戏 → 招末影龙 → 看到龙息 → 进 Mod Menu 改开关 → 重启 → 验证

---

#### ⚔️ 试炼 6.6：可调颜色滤镜（Lv.3 · 魔法绘卷）

**任务**：把 `OverlayHud.java` 里的红色滤镜改成可调颜色（用 `cfg.overlayColor` 而不是硬编码红色）。

**学习目标**：
- 复习 ARGB 颜色位运算
- 理解"配置驱动"的设计思想

**通关要点**：
- 代码里已经有 `cfg.overlayColor`，你只需要让滤镜**实际用上它**
- 检查现有代码是否已经"用 `cfg.overlayColor`"——如果已经用了，那这就是个**复习任务**
- 如果没用到，思考：怎么从 `cfg.overlayColor`（int）和 `cfg.overlayOpacity`（float）拼出最终 ARGB？
- 提示：6.7 的 ARGB 构造图解可以直接套用

---

#### 🏆 试炼完成度对照表

| 试炼 | 等级 | 涉及副本 | 是否完成 |
|---|---|---|---|
| 6.1 白名单艺术 | Lv.1 | 粒子迷宫 | ☐ |
| 6.2 召唤新 VIP | Lv.2 | 远视结界 | ☐ |
| 6.3 情报员留言 | Lv.2 | 魔法绘卷 | ☐ |
| 6.4 魔法反转 | Lv.3 | 神器仪式 | ☐ |
| 6.5 新增商品 | Lv.4 | 综合 | ☐ |
| 6.6 可调颜色 | Lv.3 | 魔法绘卷 | ☐ |

---

#### 🎓 6 个副本完整回顾

| 副本 | 卷轴 | 副本副标题 | 核心技能 |
|---|---|---|---|
| ① 粒子迷宫 | `ParticleFilter.java` | 清道夫职业 | 白名单 + `contains` 模糊匹配 |
| ② 远视结界 | `EntityFilter.java` | 结界术士 | 白名单 + 平方距离比较 |
| ③ 神器仪式 | `mixin/*.java`（2 份） | 附魔师 | `@Mixin` / `@Inject` / `@Redirect` |
| ④ 魔法绘卷 | `OverlayHud.java` | 绘卷师 | 事件订阅 + ARGB 位运算 |
| ⑤ 魔法商店 | `config/*.java`（2 份） | 装修工 | Cloth Config 链式 + 接口实现 |
| ⑥ 实战挑战 | 6 个试炼 | 真正的大师 | **综合运用前 5 副本所有技能** |

> 🎉 **通关感言**：
> - 6 个副本 + 6 个试炼，勇者你已经具备**独立开发中等难度 mod** 的能力
> - 下一个里程碑：**自己写一个 mod**（不是这个 PvP-Optimize）——比如"自动钓鱼 mod"、"迷你地图 mod"
> - 王国还有更多副本在等你（7 章实战 + 8 章调试 + 9 章进阶），敬请期待！
## 第 7 章 副本 ⑦：实战演练场

#### 🗺️ 副本背景
> *"勇者大人，恭喜你完成前 6 个副本！"*
>
> *现在王国要考察你的实战能力。**实战演练场**已经开放，里面有 5 个**限时任务**——这些任务不是新技能，而是**真实玩家会提的需求**。完成它们，你就真正'出师'了。"*
>
> *每个任务都有：*
> - 🎯 **任务目标**（玩家提的需求）
> - 🪜 **操作步骤**（5-6 步手把手）
> - ✅ **通关验证**（怎么知道做对了）
> - ⚠️ **任务陷阱**（容易踩的坑，提前避开）
>
> *演练场的军需官等你。"*
>
> —— 王国军需官

#### 🎁 副本奖励
- 完成任意 3 个任务 → 解锁称号"🛡️ 合格勇者"
- 完成全部 5 个任务 → 解锁称号"👑 王国认证 Mod 开发者"

---

### 7.1 任务 ①：把暴击粒子从默认开启改成默认关闭

#### 🎯 任务目标
> *"我发现每次打怪都冒一堆青色星星，太花哨了，新玩家装上 mod 看不到暴击粒子就行，老玩家想看自己在 Mod Menu 里开。"*

#### 🪜 操作步骤

**第 1 步：打开"魔法卷宗柜"**

找到 `src\main\java\com\pvp\optimize\PvPOptimizeConfig.java`，这是王国所有"默认设置"的存档室。

**第 2 步：找到"暴击粒子"的默认开关**

```java
// 找到这一行（约在第 26 行）
public boolean keepCritParticles = true;   // ← 这就是"暴击粒子默认开"
```

**第 3 步：把 `true` 改成 `false`**

```java
public boolean keepCritParticles = false;  // ← 改完之后，默认就不显示了
```

**第 4 步：（可选）更新注释**

把第 24 行附近的中文注释里的"默认"字样修正（注释不影响代码运行，但读起来更舒服）。

#### ✅ 通关验证

打开 PowerShell，敲出"召唤构建术"：

```powershell
cd C:\Users\16210\Documents\pvp-optimize
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build -x test --no-daemon
```

看到 `BUILD SUCCESSFUL` 就成功一半。`build/libs/pvp-optimize-1.0.0.jar` 的时间戳应该是刚才的。

把这个新 jar 复制到 `mods` 目录，启动游戏，拿钻石剑打一只僵尸——**应该看不到青色星星了**。然后进 Mod Menu → Config → 粒子过滤 → 勾上"保留暴击粒子" → 再打僵尸 → 青色星星又出来了。

#### ⚠️ 任务陷阱
- **千万别忘了复制 jar**！很多勇者构建成功却忘了把新 jar 复制到 mods 目录，抱怨"为啥没生效"。
- **别把 `boolean` 写成 `Boolean`**（大写）——这是封装类型，对配置文件不友好。
- **别忘了分号 `;`** —— Java 里每行声明末尾都要分号。

---

### 7.2 任务 ②：把状态面板默认键位从 H 改成 G

#### 🎯 任务目标
> *"H 键我老按到，打字聊天还会切出输入框，给我改成 G 吧。"*

#### 🪜 操作步骤

**第 1 步：找到"键位登记表"**

还是 `PvPOptimizeConfig.java`，搜 `OPEN_HUD`，会看到类似：

```java
public static final KeyBinding OPEN_HUD = new KeyBinding(
    "key.pvp_optimize.open_hud",
    GLFW.GLFW_KEY_H,           // ← 这就是要改的"魔法手势"
    "category.pvp_optimize.main"
);
```

**第 2 步：把 H 改成 G**

```java
GLFW.GLFW_KEY_G               // ← 改完就完事了！
```

**第 3 步：检查 `OverlayHud.java` 是否需要改**

打开 `src\main\java\com\pvp\optimize\hud\OverlayHud.java`，第 18 行附近：

```java
client.options.allKeys  // ... 里面有 PvPOptimizeConfig.OPEN_HUD
```

你看到的是**引用**（不是写死的 H 字符），所以**这里不用改**！它会自动跟着键位表走。

#### ✅ 通关验证
构建 → 启动游戏 → 按 G → 状态面板应该出现 → 按 G → 状态面板应该消失。

#### ⚠️ 任务陷阱
- **G 键在原版 Minecraft 是切换生存/创造模式**！会和你原版快捷键打架。
- 推荐用不冲突的键，比如 `F8`、`R`、`V` 之类。完整的按键冲突表参考 https://minecraft.fandom.com/wiki/Key_Bindings
- `GLFW.GLFW_KEY_X` 的命名规律：字母键就是 `GLFW_KEY_<大写字母>`，F1~F12 是 `GLFW_KEY_F1`~`F12`，Esc 是 `GLFW_KEY_ESCAPE`

---

### 7.3 任务 ③：加一个新配置项 "保留末影龙粒子"

#### 🎯 任务目标
> *"我打末影龙的时候，紫色龙息粒子帮我判断走位，能不能加个开关默认开？"*

这是 5 个任务里**最复杂**的一个，要改 5 个文件！但都是我们之前学过的。

#### 🪜 操作步骤

**第 1 步：在"配置神庙"的 Data 类里加字段**

打开 `PvPOptimizeConfig.java`，找到 `Data` 类（里面全是 `public boolean xxx = ...`），加一行：

```java
public boolean keepDragonBreath = true;   // ← 默认开
```

**第 2 步：在 `load()` 方法里加反序列化**

找到 `load()` 里那串 `DATA.xxx = loaded.xxx;`，加一行：

```java
DATA.keepDragonBreath = loaded.keepDragonBreath;  // ← 读取存档里的设置
```

**第 3 步：在"粒子迷宫"里加过滤规则**

打开 `src\main\java\com\pvp\optimize\particle\ParticleFilter.java`，在 `shouldRender(...)` 方法里加：

```java
if (name.contains("dragon_breath")) {       // 末影龙吐息的粒子名
    return cfg.keepDragonBreath;
}
```

**第 4 步：在"魔法商店"里加 UI 入口**

打开 `src\main\java\com\pvp\optimize\config\PvPOptimizeConfigScreen.java`，找到 `particles.addEntry(...)` 那段，在最后加一项：

```java
particles.addEntry(eb.startBooleanToggle(
    Text.translatable("config.pvp_optimize.keepDragonBreath"),
    data.keepDragonBreath)
    .setDefaultValue(true)                              // 默认值
    .setSaveConsumer(v -> data.keepDragonBreath = v)    // 用户改完后写入
    .build());
```

**第 5 步：在"翻译卷轴"里加中文**

打开 `src\main\resources\assets\pvp_optimize\lang\zh_cn.json`，在 `keepXpParticles` 后面加：

```json
"config.pvp_optimize.keepDragonBreath": "保留末影龙粒子",
```

英文版 `en_us.json` 也要加：

```json
"config.pvp_optimize.keepDragonBreath": "Keep dragon breath particles",
```

**第 6 步：（可选）让"状态面板"显示这个开关**

打开 `src\main\java\com\pvp\optimize\hud\OverlayHud.java`，在第 63 行那串 `+ " XX=" + onOff(cfg.XX)` 加一项：

```java
+ " 龙息=" + onOff(cfg.keepDragonBreath)
```

#### ✅ 通关验证
构建 → 启动游戏 → 用指令 `summon minecraft:ender_dragon` 召唤末影龙 → 应该看到紫色龙息粒子 → 进 Mod Menu → 关掉"保留末影龙粒子" → 重启游戏 → 龙息应该消失。

#### ⚠️ 任务陷阱
- **5 个文件都要改**，漏一个都不行！最容易漏的是 `en_us.json`（不加会显示成 `config.pvp_optimize.keepDragonBreath` 这种英文 key）
- **粒子名是 `dragon_breath` 不是 `dragonbreath`**！MC 用下划线分词
- **`setDefaultValue(true)` 必须和 Data 字段初始值一致**，否则玩家重置配置时会以为出 bug
- **修改后必须重启游戏**才能生效（不像 7.1 那种默认值的修改，重启才能让新默认值写入存档）

---

### 7.4 任务 ④：把状态面板移到屏幕右上角

#### 🎯 任务目标
> *"面板在左上角挡住小地图了，给我挪到右上角去。"*

#### 🪜 操作步骤

**第 1 步：找到 HUD 渲染代码**

打开 `src\main\java\com\pvp\optimize\hud\OverlayHud.java`，定位到 `renderHud` 方法（约第 55-60 行）：

```java
int x = 4;     // 距左边 4 像素
int y = 4;     // 距顶部 4 像素
```

**第 2 步：获取屏幕宽度**

在 `x` 之前先取屏幕宽：

```java
int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
```

**第 3 步：改成动态计算 x**

```java
int x = screenW - width - 8;  // 屏幕宽 - 文字宽 - 右边距 8
int y = 4;
```

#### ✅ 通关验证
构建 → 启动游戏 → 按 H 打开状态面板 → 应该在右上角。

#### ⚠️ 任务陷阱
- **`width` 必须在算 `x` 之前先算出来**！原来的代码里 `width` 是在多行文本拼接之后才算的，所以要把 `getScaledWidth()` 调用挪到 `width` 计算之后
- **scaledWidth 不是 width**！前者按 GUI 缩放比算（比如 2K 屏设 1080p 渲染就是 1920），后者是物理像素

---

### 7.5 任务 ⑤：把红色滤镜默认改成蓝色

#### 🎯 任务目标
> *"红色滤镜看着像血，不吉利，给我换个淡蓝色，看着像水之呼吸。"*

#### 🪜 操作步骤

**第 1 步：找到颜色配置**

打开 `PvPOptimizeConfig.java`，找到：

```java
public int overlayColor = 0x10FF1010;
```

**第 2 步：改成蓝色**

```java
public int overlayColor = 0x101010FF;  // A=10(透明度), R=10, G=10, B=FF
```

#### ✅ 通关验证
构建 → 启动游戏 → 按 J → 屏幕应该蒙上一层**淡蓝色**。

#### ⚠️ 任务陷阱
- **颜色格式是 `AARRGGBB`，不是常见的 `RRGGBB`！** 前两位是透明度，后六位才是 RGB
- 常见调色对照表：
  - `0x10FF1010` = 透明 16/255 + 红 255 = **淡红**
  - `0x101010FF` = 透明 16/255 + 蓝 255 = **淡蓝**
  - `0x2000FF00` = 透明 32/255 + 绿 255 = **半透明绿**
  - `0x40000000` = 透明 64/255 + 黑 = **半透明黑**
- **透明度别超过 0x80**（128/255），否则屏幕会看不清

---

### 7.6 演练场总结：5 个任务都用到了什么？

| 任务 | 涉及文件 | 核心技能 |
|---|---|---|
| ① 默认开关 | `PvPOptimizeConfig.java` | 改字段默认值 |
| ② 改键位 | `PvPOptimizeConfig.java` | GLFW 键位常量 |
| ③ 加配置项 | 5 个文件 | 全流程 |
| ④ 改 HUD 位置 | `OverlayHud.java` | 调用 `getScaledWidth()` |
| ⑤ 改颜色 | `PvPOptimizeConfig.java` | ARGB 颜色编码 |

### 7.7 动手做

✅ **任务 7.1**：完成实战 1，重新构建，确认青色暴击粒子在默认状态下不出现。

✅ **任务 7.2**：完成实战 3，验证"保留末影龙粒子"在 Mod Menu 出现，关闭后龙息粒子消失。

✅ **任务 7.3（自由发挥）**：自己设计一个实战任务并完成，比如：
- 加一个"保留爆炸粒子"开关
- 把状态面板的字号调大
- 加一个"低血量时屏幕震动"功能（提示自己快死了）

#### 🏆 副本结束语
> *"勇者大人，演练场的 5 个任务都完成了。你已经掌握了从'看代码'到'改代码'到'加代码'的完整能力。"*
>
> *接下来进入**神官日志殿**（第 8 章），学习当程序出错时怎么办——这是每个 Mod 开发者最重要的生存技能。"*
>
> —— 王国军需官
## 第 8 章 副本 ⑧：神官日志殿

#### 🗺️ 副本背景
> *"勇者大人，欢迎来到王国最神秘的地方——**神官日志殿**。"*
>
> *这里没有怪物，没有陷阱，但**比任何副本都危险**。因为这里是'亡者之殿'——所有程序崩溃的灵魂都会飞到这里，留下最后的'遗言'（错误日志）。"*
>
> *每个 Mod 开发者都**必须**会解读遗言，否则你的 mod 一旦出错，你就两眼一抹黑——明明游戏崩了，你却不知道为啥。"*
>
> *日志殿里有 3 大神龛（3 种日志），还有 1 本'亡者辞典'（IDE 调试器）。学完这些，你就能从崩溃的废墟里挖出真相。"*
>
> —— 王国图书管理员

#### 🎁 副本奖励
- 学会读日志 → 解锁"🕯️ 亡者之友"称号
- 学会用 IDE 调试 → 解锁"🔮 透视之眼"称号
- 两者皆会 → 解锁"🧙 大神官"称号

---

### 8.1 神龛 ①：3 本"亡者遗言录"的位置

王国的所有"亡者遗言"都存放在固定位置：

```
C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\logs\
```

打开这个文件夹，你会看到一堆文件：

| 文件名 | 性质 | 用途 |
|---|---|---|
| `latest.log` | 当前灵魂的遗言 | 最近一次启动的完整日志（最重要） |
| `debug.log` | 神官的详细笔录 | 调试日志（要在启动参数加 `-Dfabric.debug=1`） |
| `latest-2025-08-26-3.log` | 历史灵魂 | 之前几次启动的存档（数字越大越旧） |
| `gzlogs/` | 压缩的远古遗言 | 极旧日志，gzip 压缩格式 |

#### 🧠 概念比喻
- **latest.log** = 急诊室护士手里那本"今天谁来过"
- **debug.log** = 病历本，记录每个病人从头到尾的所有生理指标
- **历史日志** = 档案室，留着供追溯病情变化

#### ⚠️ 踩坑提醒
- **不同启动器的 logs 路径不一样**！PCL、PCL2、HMCL、官方启动器都不一样，找到你自己的
- **latest.log 每次启动会被覆盖**！崩溃后想保留现场，先把它复制成 `crash-backup.log`

---

### 8.2 神龛 ②：4 大常见死因的排查流程

#### ☠️ 死因 A：游戏崩了（黑屏 / 闪退 / 卡死）

**第 1 步**：打开 `latest.log`

**第 2 步**：搜关键词：

```powershell
Select-String -Path "$env:APPDATA\.minecraft\logs\latest.log" -Pattern "ERROR|Exception|Caused by"
```

（或者直接记事本打开，按 `Ctrl+F` 搜 `ERROR`）

**第 3 步**：找到报错后，**只看最顶部的 `Caused by:`**——这才是真正的"凶手"，下面的栈跟踪是"凶手留下的脚印"。

**第 4 步**：把 `Caused by: xxx` 这行**完整复制**（包括后面的类名和行号），发给 AI 助手或自己查。

**典型报错长这样**：

```
[22:30:15] [main/ERROR]: Failed to load mod "pvp-optimize"
java.lang.NoSuchMethodError: 'void net.minecraft.client.particle.ParticleManager.<init>(...)'
    at com.pvp.optimize.mixin.ParticleManagerMixin.<init>(ParticleManagerMixin.java:15)
Caused by: 找不到方法
```

这段话告诉你：**Mixin's `ParticleManager` 找不到对应方法**——多半是 MC 版本不匹配，或者 Loom 映射表错了。

#### ☠️ 死因 B：Mod 没加载（游戏里看不到 mod）

**第 1 步**：在 `latest.log` 搜你的 mod id（`pvp_optimize`）：

```powershell
Select-String -Path "latest.log" -Pattern "pvp_optimize"
```

**第 2 步**：看 3 种结果：

| 搜到的内容 | 含义 |
|---|---|
| `[main/INFO]: Loading mod pvp_optimize` + `[main/INFO]: Initialized pvp_optimize` | ✅ mod 加载成功 |
| `[main/INFO]: Loading mod pvp_optimize`，但**没**看到 `Initialized` | ❌ 启动中崩溃了，看 ERROR 段 |
| 完全没搜到 | ❌ mod 根本没被加载，看 jar 是不是放对位置 |

**第 3 步**：检查 `mods/` 目录：
- 你的 jar 在不在？（`pvp-optimize-1.0.0.jar`）
- 名字对不对？（不能有空格、不能有中文）
- Fabric Loader 在不在？（`fabric-loader-x.x.x.jar`）

#### ☠️ 死因 C：修改没生效（构建成功但游戏没变）

这是**最常见**的"亡灵假死"——程序没崩，但就是不对。

**第 1 步**：检查 jar 时间戳

```powershell
Get-Item "C:\Users\16210\Documents\pvp-optimize\build\libs\pvp-optimize-1.0.0.jar" | Select-Object LastWriteTime
Get-Item "C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date\mods\pvp-optimize-1.0.0.jar" | Select-Object LastWriteTime
```

两个时间戳应该**几乎一致**（构建 jar 的时间）。

**第 2 步**：如果不一致，说明**你忘了把 jar 复制到 mods 目录**！

**第 3 步**：如果时间戳一致但还是没生效，尝试 `clean build`：

```powershell
.\gradlew.bat clean build -x test --no-daemon
```

`clean` 会清掉 `build/` 目录的旧产物，强制重新打包。

#### ☠️ 死因 D：编译失败（Gradle 报错）

构建时报错一般是**语法错误**，Java 编译器很严格。

**典型错误对照表**：

| 错误信息 | 原因 | 修法 |
|---|---|---|
| `error: ';' expected` | 漏了分号 | 在指定行加上 `;` |
| `error: cannot find symbol` | 类没 import、变量名打错 | 检查 import 和拼写 |
| `error: class, interface, or enum expected` | 大括号 `{}` 不匹配 | 数 `{}` 个数，确保左右配对 |
| `error: method does not override or implement a method from a supertype` | `@Override` 写错或父类方法签名变了 | 检查方法名和参数 |
| `error: incompatible types` | 类型不匹配，比如把 int 赋给 String | 改类型或加转换 |
| `error: package xxx does not exist` | 缺依赖或 import 路径错 | 检查 `build.gradle` 和 import |

#### 🧠 心法口诀
> **崩溃看 ERROR，没加载搜 mod id，没生效看时间戳，编译错查拼写。**

---

### 8.3 神龛 ③：亡者辞典（IDE 调试器）

普通的 `println` 调试是"听死者最后一句话"，而 IDE 调试是"**让死者复活，慢慢走一遍他最后 3 秒**"。

#### 🪄 调试器的超能力

| 能力 | 比喻 |
|---|---|
| **断点（Breakpoint）** | 让时间在某一刻暂停 |
| **单步执行（Step Over）** | 慢动作播放下一步 |
| **进入方法（Step Into）** | 钻进代码深处看细节 |
| **查看变量（Watches）** | 透视野怪身上的装备栏 |
| **热重载（Hot Reload）** | 改完代码不用重启游戏 |

#### 🛠️ IntelliJ 调试教程（推荐）

**第 1 步**：打开项目（`File → Open → 选 pvp-optimize 文件夹`）

**第 2 步**：在你想暂停的代码行**左边**点击（行号右边的灰色区域），会出现一个🔴红点（断点）。

比如你想看 `ParticleFilter.shouldRender` 什么时候被调用、参数是啥，就在它开头点一下。

**第 3 步**：配置启动项

1. 菜单 `Run → Edit Configurations`
2. 点左上角 `+` → `Application`
3. 填写：
   - **Name**: `Minecraft 1.20.6`
   - **Main class**: `net.fabricmc.loader.impl.launch.knot.KnotClient`
   - **Working directory**: 你的 `.minecraft` 目录（包含 `mods/` 的那个）
   - **Use classpath of module**: `main`
4. 点 `OK`

**第 4 步**：点工具栏的🐞**Debug**按钮（不是 Run！）

游戏会启动到断点处暂停，IntelliJ 会自动弹出**调试器面板**：

```
Frames:
  ParticleManager.render()  ← 当前正在执行的方法
  ParticleFilter.shouldRender()  ← 你的代码

Variables:
  name = "minecraft:crit"  ← 当前变量值
  cfg = Data{...}  ← 整个配置对象
```

你可以：
- **F8** = 单步执行下一步
- **F9** = 继续跑到下一个断点
- **F7** = 进入方法内部
- **鼠标悬停** = 看任意变量的值

#### 🛠️ VS Code 调试教程（轻量备选）

装插件 `Debugger for Java`（`Extension Pack for Java` 已经包含了）。

在代码左边点红点 → F5 启动 → 同样进入调试模式。

#### ⚠️ 零基础建议
> **调试器很强大，但零基础先别碰。** 我们的日志已经够用 90% 的情况。先把"改 → 构建 → 部署 → 看日志"流程跑通，等你熟悉了整个 mod 之后再学调试器。

---

### 8.4 副本 ⑧ 的"急救箱"：3 个救命脚本

把这些命令保存到 `C:\Users\16210\Documents\pvp-optimize\` 下的 `quick-debug.ps1`，下次出问题直接跑：

```powershell
# quick-debug.ps1 —— 快速诊断脚本
$ErrorActionPreference = "Stop"
$modDir = "C:\Users\16210\Documents\pvp-optimize"
$gameDir = "C:\Users\16210\Desktop\PCL 正式版 2.13.0.1\.minecraft\versions\Simply Optimized & Up to Date"

Write-Host "===== 1. 检查 jar 时间戳 =====" -ForegroundColor Cyan
Get-Item "$modDir\build\libs\pvp-optimize-1.0.0.jar" -ErrorAction SilentlyContinue | 
    Select-Object Name, LastWriteTime, Length | Format-Table

Write-Host "===== 2. 检查 mods 目录 =====" -ForegroundColor Cyan
Get-ChildItem "$gameDir\mods" -Filter "pvp-optimize*" | 
    Select-Object Name, LastWriteTime, Length | Format-Table

Write-Host "===== 3. 最近 20 行日志 =====" -ForegroundColor Cyan
$logPath = "$gameDir\logs\latest.log"
if (Test-Path $logPath) {
    Get-Content $logPath -Tail 20
} else {
    Write-Host "找不到 $logPath" -ForegroundColor Red
}
```

以后出问题就：
```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\16210\Documents\pvp-optimize\quick-debug.ps1"
```

3 秒内给出"健康报告"。

---

### 8.5 动手做

✅ **任务 8.1**：故意在 `ParticleFilter.java` 里删一个分号，重新构建。看 Gradle 报什么错。**然后把分号加回去，确认能恢复**。

✅ **任务 8.2**：把 `latest.log` 搜 "PvP-Optimize"，确认我们的初始化日志在。把它截图保存到文档，作为"健康检查"的证据。

✅ **任务 8.3（挑战）**：故意把 `build.gradle` 里的 `minecraft_version` 改成 1.99.0，重新构建。看 Gradle 报什么错。然后改回 1.20.6 确认能恢复。**这是模拟"升级 MC 版本失败"的场景**。

#### 🏆 副本结束语
> *"勇者大人，你已经掌握了从'亡者遗言'里读取真相的能力。"*
>
> *王国所有的崩溃都将对你透明——你不再是那个'看到错误就懵'的菜鸟了。"*
>
> *最后一站：**王国远征**（第 9 章）——走出新手村，看看外面的世界还有哪些冒险在等着你。"*
>
> —— 王国图书管理员
## 第 9 章 副本 ⑨：王国远征

#### 🗺️ 副本背景
> *"勇者大人，恭喜你走完了新手村所有副本！"*
>
> *但**王国只是世界的一小部分**。王国之外，有更广阔的 1.21 雪原（升级目标）、有 Modrinth 帝国（发布平台）、有 Fabric 圣殿（顶级知识库）、还有无数未探索的副本等待着你。"*
>
> *这一章没有具体任务，只有一张**远征地图**。每个地标都是一扇门，**你随时可以出发，也可以慢慢来**。重要的是——"*
>
> *你已经具备了独自远行的能力。"*
>
> —— 国王

#### 🎁 副本奖励
- 完成远征的任意一段路 → 解锁"🗺️ 远征者"称号
- 走完全图 → 解锁"🌟 王国之光"称号

---

### 9.1 远征路 ①：升级到 1.21.x（雪原远征）

#### 🗺️ 地图说明
> *1.21 雪原是 MC 的一个新王国。地表被雪覆盖（新增了雪片粒子），地底有试炼大厅（新增了试炼密室），天空的 boss 换了形态（新增了风弹）。"*
>
> *但雪原的温度（API 接口）和 1.20 完全不同——你不能直接穿着 1.20 的装备过去。"

#### 🛠️ 远征装备（要改的 5 个文件）

| 文件 | 改什么 | 难易度 |
|---|---|---|
| `gradle.properties` | `minecraft_version=1.21.4`、`loader_version=0.16.x`、`yarn_mappings=1.21.4+build.xxx`、`fabric_version=0.x.x+1.21.4` | ⭐ |
| `build.gradle` | `loom_version`（要匹配新 MC） | ⭐⭐ |
| `local-libs/*.jar` | Mod Menu 升级到 6.x、Cloth Config 升级到 14.x | ⭐⭐ |
| 源码 | 编译报错的地方按新 API 改 | ⭐⭐⭐ |
| `lang` 文件 | 一般不用改（但新粒子可能需要新增翻译） | ⭐ |

#### 🪜 远征路线（10 步走）

**第 1 步**：复制项目

```powershell
Copy-Item -Path "C:\Users\16210\Documents\pvp-optimize" `
          -Destination "C:\Users\16210\Documents\pvp-optimize-1.21" `
          -Recurse
```

**第 2 步**：打开 `gradle.properties`，改 4 个版本号

```properties
minecraft_version=1.21.4
yarn_mappings=1.21.4+build.1
loader_version=0.16.10
fabric_version=0.112.2+1.21.4
```

**第 3 步**：打开 `build.gradle`，改 Loom 版本

```groovy
plugins {
    id 'fabric-loom' version '1.8-SNAPSHOT'   // 1.21.x 对应 Loom 1.8+
}
```

**第 4 步**：升级本地 jar

去 [Modrinth Mod Menu](https://modrinth.com/mod/modmenu/versions?g=1.21.4) 下载 1.21.4 版的 Mod Menu jar。
去 [Modrinth Cloth Config](https://modrinth.com/mod/cloth-config/versions?g=1.21.4) 下载 1.21.4 版的 Cloth Config jar。
替换 `local-libs/` 里的旧 jar。

**第 5 步**：清缓存，构建

```powershell
cd C:\Users\16210\Documents\pvp-optimize-1.21
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build -x test --no-daemon
```

**第 6 步**：看错误，修代码

构建会报一堆 `error: cannot find symbol`，别慌，**每个错误都是 API 改名的提醒**。比如：

- `ParticleManager` 构造函数改了 → 改 Mixin 的 `@Inject` 方法签名
- `EntityRenderDispatcher.render` 参数加了 `f` → 在 mixin 方法里加参数

**第 7 步**：用 Yarn 映射表查新名字

访问 https://maven.fabricmc.net/net/fabricmc/yarn/ ，选你的 MC 版本，能查每个类的完整方法列表。

**第 8 步**：循环"构建 → 看错 → 改"，直到 `BUILD SUCCESSFUL`

**第 9 步**：复制 jar 到 mods 目录，启动游戏，验证功能

**第 10 步**：搞定！发布或自用

#### 📚 详细升级指南
完整攻略在项目里的 `UPDATE_GUIDE.md` 第 2 章。本教程只给"地图"，详细"战术"看那份文档。

#### ⚠️ 远征陷阱
- **别只改 `gradle.properties`**！很多人误以为只改这一个文件就够了，结果构建失败。
- **Loom 版本必须和 MC 版本联动**！1.20.x 用 Loom 1.6-1.7，1.21.x 用 Loom 1.8+
- **Cloth Config 14.0.139 可能在 Loom 1.6.x 下报 manifest 错误**，要么升级 Loom 要么降 Cloth Config
- **构建失败不要硬扛**，80% 是缓存问题，试试 `clean build` 或删 `~/.gradle/caches/`

---

### 9.2 远征路 ②：知识圣殿（推荐的学习资源）

| 资源 | 链接 | 适合 |
|---|---|---|
| 《Java 核心技术 卷 I》 | 任意网店 / Z-Library | 深入学 Java 语法 |
| Fabric 官方文档 | https://fabricmc.net/develop/ | 学 Fabric 进阶 |
| Yarn 映射表 | https://maven.fabricmc.net/net/fabricmc/yarn/ | 查类名（升级必备） |
| Fabric Discord | https://discord.fabricmc.net/ | 提问（英文为主） |
| B 站搜 "Fabric Mod 开发" | B 站 | 视频教程（中文） |
| GitHub awesome-fabric | github.com/anthonyhilyard/awesome-fabric | 资源合集 |

#### 🎓 学习路径建议

| 阶段 | 资源 | 时长 |
|---|---|---|
| 巩固 Java 基础 | 《Java 核心技术》前 8 章 | 1-2 周 |
| 学 Mixin 原理 | Fabric 官方 wiki | 3 天 |
| 看别人怎么写 Mod | GitHub 搜 `fabric-example-mod` | 1 周 |
| 仿写一个简单 Mod | 参考 [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod) | 2 周 |
| 自己设计并实现 | 从你日常玩 MC 时的小需求入手 | 持续 |

---

### 9.3 远征路 ③：6 大副本（推荐的项目练习）

按难度递增：

#### 🗺️ 副本 A：加一个新粒子保留规则
> 难度 ⭐（实战 3 已演示）

仿照 7.3，给"保留经验球粒子"加个开关。

#### 🗺️ 副本 B：加一个新键位（组合键）
> 难度 ⭐⭐

把红色滤镜从单键 `J` 改成组合键 `Ctrl+R`。需要学 `KeyBinding` 的 `matches` 高级用法。

#### 🗺️ 副本 C：加一个独立 GUI 屏幕
> 难度 ⭐⭐⭐

用 Cloth Config 做一个"调试面板"，显示当前世界里有多少实体、按类型分类。需要学 Cloth Config 进阶。

#### 🗺️ 副本 D：加一个新 mixin
> 难度 ⭐⭐⭐

屏蔽某种 UI 元素，比如把血量槽隐藏。需要学 `@Shadow` 和 `@Redirect` 注解。

#### 🗺️ 副本 E：跨版本升级
> 难度 ⭐⭐⭐⭐

实战 9.1，把 mod 从 1.20.6 升级到 1.21.4。

#### 🗺️ 副本 F：发布到 Modrinth
> 难度 ⭐⭐⭐

让全网玩家用你的 mod。需要注册账号、写 mod 介绍、生成 .mrpack 文件、填版本号。详细流程看 `RELEASE_GUIDE.md` 第 3 章。

#### 🏆 通关奖励
- 副本 A 完 → 解锁"⚔️ 实战新兵"
- 副本 A+B+C 完 → 解锁"🎖️ 资深勇者"
- 副本 D 完 → 解锁"🧙 大法师"
- 副本 E 完 → 解锁"🐉 屠龙者"
- 副本 F 完 → 解锁"👑 王国传奇"

---

### 9.4 远征路 ④：4 条职业岔路

掌握 Fabric Mod 开发后，可以走：

#### ⚔️ 岔路 1：Minecraft Mod 开发者
- 业余也能接外包赚钱（一个定制 mod 几百到几千美元）
- CurseForge、Modrinth 上发布作品吸引粉丝
- 维护一个 mod 长期更新（社区认可）

#### 🎮 岔路 2：游戏客户端工程师
- Unity、Unreal 也用类似技术（Component、Render 流程）
- 引擎原理都相通，技能可迁移
- 工资范围：一线 25-50K/月

#### ☕ 岔路 3：Java 后端工程师
- 学完 Java 直接能转岗（语法 90% 通用）
- Spring Boot、MySQL、Redis 是下一步学习内容
- 工资范围：一线 15-40K/月（初级到高级）

#### 🎨 岔路 4：客户端渲染工程师
- 学完 OpenGL/Mixin 后可以走图形学方向
- 需要额外学 C++ 和 Vulkan/Metal/DirectX
- 工资范围：一线 30-80K/月

#### 💡 怎么选？
> *"勇者大人，没有'最好'的路，只有'最适合'的路。"*
>
> *如果你喜欢游戏 → 选 1 或 2*
> *如果你喜欢稳定 → 选 3*
> *如果你喜欢炫技 → 选 4*
>
> *无论选哪条路，本教程教你的"看代码、改代码、加代码"的能力都是**通用技能**——会永远跟着你。"

---

### 9.5 王国的礼物：3 个"远征锦囊"

#### 🎁 锦囊 1：建立"知识花园"

建议建一个笔记（推荐 Obsidian / Notion），分类记录：
- 常用代码片段（比如 "怎么注册一个 mixin"）
- 踩过的坑（比如 "Loom 1.6 + Cloth Config 14 manifest 错误"）
- 灵感池（想到的 mod 创意）

#### 🎁 锦囊 2：加入"勇者公会"

- **Fabric Discord**（英文，但高手云集）
- **B 站 Minecraft Mod 开发频道**（中文，新手友好）
- **GitHub Discussions**（很多 mod 作者在那里回答问题）

#### 🎁 锦囊 3：保持"玩心"

> *"代码是工具，mod 是玩具。* *别忘了你最初为什么学——是因为好玩。* *当 mod 写累了，就去 MC 里盖房子、打怪、挖矿。* *玩耍的灵感和写代码的动力会互相滋养。"*
>
> —— 国王

---

### 9.6 动手做

✅ **任务 9.1**：去 Modrinth 注册账号，浏览前 5 个热门 mod，看它们的源码怎么写。

✅ **任务 9.2**：在 GitHub 搜 `fabric-example-mod`，克隆下来跑通，作为下一阶段的"练手副本"。

✅ **任务 9.3（终极挑战）**：在 1.20.6 的 mod 基础上，**额外**加一个你想加的功能（参考 9.3 的 6 个副本选一个），并发布到 GitHub。

#### 🏆 副本结束语
> *"勇者大人，9 个副本全部完成！"*
>
> *你已经从'零基础勇者'成长为'Mod 开发者'。王国的城墙在你身后，远方的世界在你面前。"*
>
> *去吧，去创造，去冒险，去让这个 Minecraft 世界因你而不同。"*
>
> *愿方块常在你脚下。"*
>
> —— 国王

---

## 🎉 教程结语

> *"到这里，**零基础 PvP-Optimize 编程教程**就结束了。*
>
> *你学到了：*
> - 编程的本质（第 1 章）
> - 搭建开发环境（第 2 章）
> - Java 核心语法（第 3 章）
> - Gradle 构建系统（第 4 章）
> - Minecraft + Fabric 基础（第 5 章）
> - PvP-Optimize 全部源码精读（第 6 章）
> - 5 个实战任务（第 7 章）
> - 调试与日志解读（第 8 章）
> - 升级、发布、职业方向（第 9 章）
>
> *学完这些，你已经超过了 90% 的 Minecraft 玩家。*
>
> *剩下的路，**靠你自己走**。*
>
> *如果你在远征中遇到问题，**随时回来翻这份教程**。*
>
> *再见，勇者大人！"*
>
> —— TRAE · 2026
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