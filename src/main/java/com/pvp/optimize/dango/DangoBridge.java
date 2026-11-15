package com.pvp.optimize.dango;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.util.CharInputBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * 把 DangoMusic 官方客户端字节码（v1.7.5 + 1.20.1-fabric，已做 1.20.6 兼容补丁）
 * 作为本 mod 的一部分驱动起来。
 *
 * <p>这里全部用反射调用，编译期不依赖 Dango 的类：Dango 的 UI / core / modern 平台层、
 * GLSL 着色器、Inter 字体、relocated Kotlin 运行库都原封不动打在同一个 jar 里，
 * 本项目没有任何自绘界面代码参与其中。反射也保证万一 Dango 初始化失败，
 * 优化功能（剔除 / 粒子 / 药水 HUD）照常工作，不会连累启动。
 *
 * <p>驱动的四件事与官方 Fabric 入口一致：
 * <ol>
 *   <li>{@code ModernKeyBindings.register(...)}：注册 Dango 自己的键位（默认 M），并让设置页可改键；</li>
 *   <li>{@code ModernDango.start()}：启动模块、配置、播放器与 Gfx；</li>
 *   <li>{@code ClientTickEvents.START_CLIENT_TICK -> ModernDango.tick()}：GL 初始化与逐 tick 事件；</li>
 *   <li>{@code HudRenderCallback -> ModernDango.hudOverlay(DrawContext)}：触发 HudRenderEvent，
 *       由 Dango 原版 DynamicIslandModule / SongCardModule 画灵动岛与歌曲卡片。</li>
 * </ol>
 * 官方入口里的 {@code PartyNetwork.register()}（一起听歌的服务器通道）刻意不接入：
 * 它使用 1.20.1 的 Fabric Networking API，在 1.20.6 上签名已变，且与界面无关。
 *
 * <p>另外补了 1.20.6 Windows 中文输入法：Dango 官方界面靠隐藏 EditBox 让 IMBlocker
 * 开启系统输入法，但合成字符只走 GLFWCharCallback，原版 charTyped 收不到。
 * 这里把 {@link CharInputBridge} 的 IME 字符直接投给 Dango 的 MusicScreen.keyTyped。
 */
public final class DangoBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("pvp_optimize/dango");

    /** DangoMusic 官方版本，写进启动诊断，方便排错时确认内置的是哪一版。 */
    public static final String DANGO_VERSION = "1.7.5 (official 1.20.1-fabric build, 1.20.6 compat patched r2: Matrix4fStack)";

    private static boolean ready;
    private static boolean hudDisabled;
    private static boolean hudWarned;
    private static boolean imeInstalled;
    private static Method tickMethod;
    private static Method hudOverlayMethod;
    private static Method openMusicScreenMethod;
    private static Object platformInstance;
    private static Field vanillaMusicHold;
    private static Field musicScreenField;
    private static Method musicKeyTyped;

    private DangoBridge() { }

    public static boolean isReady() {
        return ready;
    }

    /** 在客户端初始化阶段调用，与官方 DangoMusicFabric 保持一致。 */
    public static void init() {
        try {
            Class<?> modernDango = Class.forName("me.sorihiko.dango.modern.ModernDango");
            Method start = modernDango.getMethod("start");
            tickMethod = modernDango.getMethod("tick");
            hudOverlayMethod = modernDango.getMethod("hudOverlay", Object.class);

            // 官方键位（M / 播放控制）。Dango 侧的 Consumer<KeyMapping> 在运行时泛型已擦除，
            // KeyMapping 与 1.20.6 的 KeyBinding 是同一个 intermediary 类 class_304。
            Class<?> keys = Class.forName("me.sorihiko.dango.modern.ModernKeyBindings");
            Method register = keys.getMethod("register", Consumer.class);
            Consumer<KeyBinding> typed = KeyBindingHelper::registerKeyBinding;
            @SuppressWarnings("rawtypes")
            Consumer raw = typed;
            register.invoke(null, raw);

            start.invoke(null);
            ClientTickEvents.START_CLIENT_TICK.register(client -> tick());

            Class<?> platform = Class.forName("me.sorihiko.dango.modern.ModernPlatform");
            platformInstance = platform.getField("INSTANCE").get(null);
            openMusicScreenMethod = platform.getMethod("openMusicScreen");

            vanillaMusicHold = Class.forName("me.sorihiko.dango.platform.VanillaMusicHold")
                    .getField("active");

            // 中文输入法：把 IME 合成字符转发给 Dango 官方输入框
            Class<?> shell = Class.forName("me.sorihiko.dango.modern.ModernMusicScreen");
            musicScreenField = shell.getDeclaredField("screen");
            musicScreenField.setAccessible(true);
            musicKeyTyped = Class.forName("me.sorihiko.dango.music.MusicScreen")
                    .getMethod("keyTyped", char.class, int.class);
            CharInputBridge.externalTarget = DangoBridge::onImeChar;

            verifyModelViewStack();
            ready = true;
            LOGGER.info("[PvP-Optimize] DangoMusic {} 已内置并启动；按 M 打开官方液态玻璃播放器。",
                    DANGO_VERSION);
        } catch (Throwable t) {
            ready = false;
            LOGGER.error("[PvP-Optimize] DangoMusic 初始化失败（优化功能不受影响）", t);
        }
    }

    /**
     * 1.20.1 的 getModelViewStack() 返回 PoseStack，1.20.6 改成 org.joml.Matrix4fStack。
     * 官方 1.20.1 字节码在画 CJK 文字时会按旧签名调用，导致 NoSuchMethodError 崩溃；
     * 内置包已把 McCompat 的 pushIdentityModelView / popModelView 改成新 API。
     * 启动自检把结果写进日志，换 Minecraft 版本时能立刻看出是否还兼容。
     */
    private static void verifyModelViewStack() {
        try {
            Class<?> ret = Class.forName("com.mojang.blaze3d.systems.RenderSystem")
                    .getMethod("getModelViewStack").getReturnType();
            boolean ok = "org.joml.Matrix4fStack".equals(ret.getName());
            LOGGER.info("[诊断] DangoMusic矩阵兼容: getModelViewStack() -> {} ({})",
                    ret.getName(), ok ? "已适配Matrix4fStack" : "签名不匹配，界面可能崩溃");
        } catch (Throwable t) {
            LOGGER.warn("[诊断] DangoMusic矩阵兼容: 自检失败 {}", t.toString());
        }
    }

    private static void tick() {
        if (!ready) return;
        // GLFW 窗口在客户端初始化阶段可能还没建好，首个可用 tick 再装 IME 桥
        if (!imeInstalled) {
            try {
                CharInputBridge.install(MinecraftClient.getInstance());
                imeInstalled = CharInputBridge.isInstalled();
            } catch (Throwable ignored) {
                // 下个 tick 再试
            }
        }
        try {
            tickMethod.invoke(null);
        } catch (Throwable t) {
            // tick 里的 GL 初始化偶发失败不应刷屏；只记一次，后续靠下一 tick 自愈。
            if (!hudWarned) {
                hudWarned = true;
                LOGGER.warn("[PvP-Optimize] DangoMusic tick 异常: {}", t.toString());
            }
        }
    }

    /** GLFWCharCallback 收到的中文 / 日文 / 韩文合成字符，转投 Dango 官方输入框。 */
    private static void onImeChar(int codePoint) {
        if (codePoint > 0xFFFF || musicScreenField == null || musicKeyTyped == null) return;
        Object current = MinecraftClient.getInstance().currentScreen;
        if (current == null || !musicScreenField.getDeclaringClass().isInstance(current)) return;
        try {
            Object musicScreen = musicScreenField.get(current);
            // 第二个参数是 Dango 的 Keys.NONE = 0（纯字符输入，不对应按键）
            musicKeyTyped.invoke(musicScreen, (char) codePoint, 0);
        } catch (Throwable ignored) {
            // 界面刚好关闭等情况，直接丢弃这个字符
        }
    }

    /**
     * 每帧 HUD 回调里交给 Dango 原版渲染管线。
     * 这里只转发一个原版 DrawContext，其余（玻璃 shader、字体、灵动岛弹簧动画）全由 Dango 自己画。
     */
    public static void hud(Object drawContext) {
        if (!ready || hudDisabled) return;
        try {
            hudOverlayMethod.invoke(null, drawContext);
        } catch (Throwable t) {
            hudDisabled = true;
            LOGGER.error("[PvP-Optimize] DangoMusic HUD 渲染已停用（游戏本身不受影响）", t);
        }
    }

    /** 打开 Dango 官方播放器界面（主菜单按钮 / H 键用）。 */
    public static void open() {
        if (!ready || platformInstance == null) return;
        try {
            openMusicScreenMethod.invoke(platformInstance);
        } catch (Throwable t) {
            LOGGER.warn("[PvP-Optimize] 打开 DangoMusic 界面失败: {}", t.toString());
        }
    }

    /** Dango 正在放歌时它自己会把这个标志置真，用来让原版背景音乐让路。 */
    public static boolean isDuckingVanillaMusic() {
        if (!ready || vanillaMusicHold == null) return false;
        try {
            return vanillaMusicHold.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 供启动诊断使用。 */
    public static String status() {
        if (!ready) return "未启动";
        return hudDisabled ? "已启动/HUD停用" : "已启动";
    }

    static {
        PvPOptimize.LOGGER.debug("[PvP-Optimize] DangoBridge loaded");
    }
}
