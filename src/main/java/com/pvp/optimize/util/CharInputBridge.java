package com.pvp.optimize.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCharCallbackI;

/**
 * 中文 / 日文 / 韩文输入法字符桥。
 *
 * <p>Minecraft 1.20.6 的 {@code Keyboard.setup} 只注册了 {@code GLFWCharModsCallback}
 * （{@code onChar(long,int,int)}），而 GLFW 在 Windows 上由 IME 合成产生的字符只走
 * {@code GLFWCharCallback}（plain 输入），不会触发 char mods 回调 —— 这就是聊天框
 * 与本 mod 自定义输入框「切到中文输入法打不出字」的根因。
 *
 * <p>这里额外注册一个 {@code GLFWCharCallback}，把 codepoint &gt;= 0x80 的字符直接
 * 交给当前界面；ASCII 仍由原版 char mods 路径处理，避免重复输入。若某些平台两条
 * 回调都触发，用 {@link #consumeDuplicate(char)} 做 200ms 去重。
 *
 * <p>不会覆盖已有回调：保存原回调并链式调用；回调对象用静态字段持有，防止被 GC 回收
 * 导致 JNI 崩溃。
 */
public final class CharInputBridge {

    private CharInputBridge() {}

    /** 需要接收 IME 字符的界面实现此接口。 */
    public interface Target {
        void onImeChar(int codePoint);
    }

    private static GLFWCharCallback previous;
    private static GLFWCharCallback installed;
    private static boolean done;

    private static volatile int lastCp = -1;
    private static volatile long lastAt;

    /**
     * 外部输入法接收者（DangoMusic 官方界面走这里）。
     * 当前界面没有实现 {@link Target} 时，IME 合成字符交给它处理。
     */
    public static volatile java.util.function.IntConsumer externalTarget;

    public static synchronized void install(MinecraftClient mc) {
        if (done || mc == null || mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();
        if (handle == 0L) return;
        try {
            previous = GLFW.glfwSetCharCallback(handle, null);
            GLFWCharCallbackI cb = (window, codePoint) -> {
                if (previous != null) previous.invoke(window, codePoint);
                if (codePoint < 0x80) return;
                lastCp = codePoint;
                lastAt = System.nanoTime();
                if (mc.currentScreen instanceof Target t) {
                    t.onImeChar(codePoint);
                } else {
                    java.util.function.IntConsumer external = externalTarget;
                    if (external != null) external.accept(codePoint);
                }
            };
            installed = GLFWCharCallback.create(cb);
            GLFW.glfwSetCharCallback(handle, installed);
            done = true;
        } catch (Throwable ignored) {
            // 某些启动器 / 平台不支持额外回调时静默降级，界面仍可输入 ASCII
        }
    }

    /** 回调是否已成功安装（窗口就绪前会返回 false，供调用方下个 tick 重试）。 */
    public static synchronized boolean isInstalled() {
        return done;
    }

    /** 原版 charTyped 路径若收到了刚刚由 IME 回调投递的同一个字符，丢弃一次避免重复。 */
    public static boolean consumeDuplicate(char chr) {
        int cp = lastCp;
        if (cp < 0 || (int) chr != cp) return false;
        if (System.nanoTime() - lastAt > 200_000_000L) return false;
        lastCp = -1;
        return true;
    }
}
