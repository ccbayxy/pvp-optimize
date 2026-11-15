package com.pvp.optimize.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主控板的视觉层 —— 按 DangoMusic 官方 {@code Theme.kt} / {@code musicui/Ui.kt} /
 * {@code SettingsPage.kt} / {@code Sidebar.kt} / {@code PlayerBar.kt} 反编译出的真实规格实现。
 *
 * <p>颜色不是"近似配色"，而是官方字节码 {@code Theme.<clinit>} 里的常量：
 * <ul>
 *   <li>{@code ACCENT = #0A84FF}（iOS 蓝，选中 / 焦点 / 主按钮 / 进度）</li>
 *   <li>{@code GREEN  = #30D158}（开关开启）</li>
 *   <li>{@code RED    = #FF375F}（仅 {@code musicui/UiKt.RED}，播放条进度与封面强调）</li>
 *   <li>{@code CARD = white 0.055 / CARD_HOVER = white 0.085 / HAIRLINE = white 0.08 /
 *       SEPARATOR = white 0.07}</li>
 *   <li>{@code TEXT_PRIMARY = white 0.96 / SECONDARY = (0.92,0.92,0.96)@0.62 /
 *       TERTIARY = (0.92,0.92,0.96)@0.38}</li>
 *   <li>{@code TRACK = (0.47,0.47,0.5)@0.30 / SWITCH_OFF = (0.47,0.47,0.5)@0.36}</li>
 * </ul>
 * 玻璃层按官方 {@code GlassStyle} 参数：窗口 r=30 tint(0.1,0.1,0.13)@0.18、
 * 侧栏 r=22 tint white 0.05、选中 r=12 tint white 0.13，都带顶部高光与底部暗边；
 * 官方用 GLSL 采样背景做折射，这里保持常数次 {@code fill} 近似（低配机不掉帧），
 * 但观感与层级严格对齐。
 *
 * <p>性能约束沿用：所有原语固定次数 fill；翻译与裁剪结果缓存；界面打开时游戏世界照常渲染、
 * 不全屏变黑。
 */
public final class Ui {

    private Ui() {}

    // ===== 官方 Theme.kt 真实常量 (ARGB) =====
    public static final int TEXT         = 0xF5FFFFFF;   // TEXT_PRIMARY: white @0.96
    public static final int TEXT_MUTED   = 0x9EEBEBF5;   // TEXT_SECONDARY: (0.92,0.92,0.96) @0.62
    public static final int TEXT_FAINT   = 0x61EBEBF5;   // TEXT_TERTIARY: (0.92,0.92,0.96) @0.38
    public static final int WHITE        = 0xFFFFFFFF;

    public static final int ACCENT       = 0xFF0A84FF;   // Theme.ACCENT #0A84FF
    public static final int ACCENT_2     = 0xFF0A84FF;
    public static final int ACCENT_DIM   = 0x400A84FF;
    public static final int GREEN        = 0xFF30D158;   // Theme.GREEN
    public static final int GOOD         = GREEN;
    public static final int RED          = 0xFFFF375F;   // musicui/UiKt.RED（仅播放进度）
    public static final int WARN         = 0xFFFFD60A;
    public static final int BAD          = 0xFFFF453A;

    public static final int CARD         = 0x0EFFFFFF;   // white @0.055
    public static final int CARD_ALT     = 0x0BFFFFFF;
    public static final int CARD_HOVER   = 0x16FFFFFF;   // white @0.085
    public static final int CARD_EDGE    = 0x14FFFFFF;   // HAIRLINE white @0.08
    public static final int DIVIDER      = 0x12FFFFFF;   // SEPARATOR white @0.07
    public static final int HAIRLINE     = CARD_EDGE;
    public static final int SELECTION    = 0x21FFFFFF;   // GLASS_SELECTION tint white @0.13
    public static final int SIDEBAR      = 0x0DFFFFFF;   // GLASS_SIDEBAR tint white @0.05
    public static final int TRACK        = 0x4D788080;   // (0.47,0.47,0.5) @0.30
    public static final int TRACK_OFF    = 0x5C788080;   // SWITCH_OFF (0.47,0.47,0.5) @0.36
    public static final int ICON_DISABLED = 0xFF616B6B;  // (0.38,0.38,0.42) @1.0

    // 玻璃层：官方 GLASS_WINDOW 是 tint (0.1,0.1,0.13)@0.18 叠在**已模糊的**世界上。
    // 这里保留同一色相，alpha 由 guiGlassOpacity 控制（默认 24%），配合 renderBackground 的
    // 原版背景模糊就是真正的液态玻璃：能看见世界轮廓、文字仍可读。
    public static final int GLASS_RGB    = 0x0E1118;
    public static final int GLASS_DEEP   = 0xC0080A0F;   // 仅用于 HUD 灵动岛（背景不可模糊，须更深）
    public static final int GLASS_EDGE   = 0x1CFFFFFF;
    public static final int BG_TOP       = 0x28000000;
    public static final int BG_BOTTOM    = 0x28000000;

    /** 玻璃主色：按配置的不透明度实时计算（0-60%）。 */
    public static int glassTint() {
        int pct = 24;
        try {
            pct = com.pvp.optimize.PvPOptimizeConfig.get().guiGlassOpacity;
        } catch (Throwable ignored) {
        }
        int a = Math.max(0, Math.min(60, pct)) * 255 / 100;
        return (a << 24) | GLASS_RGB;
    }

    public static final int GLASS      = 0x3D0E1118;   // glassTint() 的默认值（24%），仅供常量引用
    public static final int PANEL      = GLASS;
    public static final int PANEL_EDGE = GLASS_EDGE;

    /** 官方 GlassStyle 圆角：窗口 30 / 侧栏 22 / 选中 12。 */
    public static final int R_WINDOW   = 30;
    public static final int R_SIDEBAR = 22;
    public static final int R_CARD    = 12;
    public static final int R_SELECT  = 12;

    /** 封面占位色：官方歌单墙的粉 / 蓝 / 紫 / 青调。 */
    private static final int[] COVER_COLORS = {
            0xFFFF375F, 0xFF0A84FF, 0xFF9380FF, 0xFF30D158,
            0xFFFF9F0A, 0xFF5E5CE6, 0xFFFF5C8A, 0xFF64D2FF
    };

    // ===== 常数开销的绘制原语 =====

    /** 倒角矩形：固定 6 次 fill，与半径无关（不逐行 sqrt）。 */
    public static void rounded(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        r = Math.max(0, Math.min(r, Math.min(x2 - x1, y2 - y1) / 2));
        if (r == 0) { c.fill(x1, y1, x2, y2, color); return; }
        c.fill(x1 + r, y1, x2 - r, y2, color);
        c.fill(x1, y1 + r, x2, y2 - r, color);
        int inset = Math.max(1, r / 3);
        c.fill(x1 + inset, y1, x2 - inset, y1 + 1, color);
        c.fill(x1 + inset, y2 - 1, x2 - inset, y2, color);
    }

    /** 带细边框的面板：固定 2 次 fill。 */
    public static void panel(DrawContext c, int x1, int y1, int x2, int y2, int r,
                             int fill, int edge) {
        c.fill(x1, y1, x2, y2, edge);
        c.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    /**
     * 官方 GLASS_WINDOW：外投影 + 半透明玻璃体 + bevel 高光/暗边 + 内部霜化。
     *
     * <p>投影是「浮起来」的关键 —— 官方 {@code ShadowStyle} 给窗口下方一层大范围柔影，
     * 之前完全没有画，所以面板看着是贴在画面上的。这里用 4 层常数 fill 近似，
     * 每帧固定 4 次填充，与窗口高度无关，低配机无压力。
     */
    public static void glass(DrawContext c, int x1, int y1, int x2, int y2, int r) {
        // 1) 外投影：从远到近三层，向下偏移，模拟官方柔影
        rounded(c, x1 - 10, y1 + 4, x2 + 10, y2 + 20, r + 10, 0x08000000);
        rounded(c, x1 - 5, y1 + 4, x2 + 5, y2 + 14, r + 5, 0x10000000);
        rounded(c, x1 - 1, y1 + 3, x2 + 1, y2 + 8, r + 1, 0x1A000000);
        // 2) 玻璃体（真半透明，背后是已模糊的世界）
        int tint = glassTint();
        rounded(c, x1, y1, x2, y2, r, tint);
        // 3) frost：内部上亮下暗，模拟折射后的亮度衰减（1 次渐变，圆角内缩避免溢出）
        if (x2 - x1 > r * 2 + 2 && y2 - y1 > r * 2 + 2) {
            c.fillGradient(x1 + r, y1 + r, x2 - r, y2 - r, 0x16FFFFFF, 0x06000000);
        }
        // 4) bevel：顶部高光 + 底部暗边 + 两侧极淡反光
        c.fill(x1 + r, y1 + 1, x2 - r, y1 + 2, 0x3CFFFFFF);
        c.fill(x1 + r, y1 + 2, x2 - r, y1 + 3, 0x14FFFFFF);
        c.fill(x1 + r, y2 - 2, x2 - r, y2 - 1, 0x28000000);
        c.fill(x1 + 1, y1 + r, x1 + 2, y2 - r, 0x14FFFFFF);
        c.fill(x2 - 2, y1 + r, x2 - 1, y2 - r, 0x14FFFFFF);
    }

    /** 官方 GLASS_SIDEBAR：比主窗口略亮的白色叠加层 + 右缘 hairline。 */
    public static void glassSidebar(DrawContext c, int x1, int y1, int x2, int y2, int r) {
        rounded(c, x1, y1, x2, y2, r, 0x14FFFFFF);
        c.fill(x1 + r, y1 + 1, x2 - r, y1 + 2, 0x22FFFFFF);
        c.fill(x1 + r, y2 - 2, x2 - r, y2 - 1, 0x0E000000);
        c.fill(x2 - 1, y1 + r, x2, y2 - r, 0x0CFFFFFF);
    }

    /** 官方 CARD：white 0.055 叠加 + HAIRLINE 描边，固定 3 次 fill。 */
    public static void card(DrawContext c, int x1, int y1, int x2, int y2, int r, int fill) {
        rounded(c, x1, y1, x2, y2, r, fill);
        c.fill(x1 + r, y1, x2 - r, y1 + 1, CARD_EDGE);
        c.fill(x1 + r, y2 - 1, x2 - r, y2, 0x08FFFFFF);
    }

    /** 官方选中胶囊：GLASS_SELECTION tint + 左侧 ACCENT 竖条。 */
    public static void selection(DrawContext c, int x1, int y1, int x2, int y2) {
        rounded(c, x1, y1, x2, y2, R_SELECT, SELECTION);
        rounded(c, x1 + 2, y1 + 3, x1 + 5, y2 - 3, 1, ACCENT);
    }

    /** 纯黑胶囊（灵动岛关闭液态玻璃时的形态）。 */
    public static void pillDark(DrawContext c, int x1, int y1, int x2, int y2, int r) {
        rounded(c, x1, y1, x2, y2, r, GLASS_DEEP);
        c.fill(x1 + r, y1 + 1, x2 - r, y1 + 2, 0x12FFFFFF);
    }

    /** 渐变退化为单色：固定 1 次 fill（视觉上取中值色）。 */
    public static void gradient(DrawContext c, int x1, int y1, int x2, int y2,
                                int top, int bottom) {
        if (x2 <= x1 || y2 <= y1) return;
        c.fill(x1, y1, x2, y2, mix(top, bottom, 0.5f));
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        int ca = Math.round(aa + (ba - aa) * t);
        int cr = Math.round(ar + (br - ar) * t);
        int cg = Math.round(ag + (bg - ag) * t);
        int cb = Math.round(ab + (bb - ab) * t);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    /** 按倍率缩放 alpha。 */
    public static int fade(int color, float a) {
        a = Math.max(0f, Math.min(1f, a));
        int alpha = Math.round(((color >>> 24) & 255) * a);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static void line(DrawContext c, int x1, int x2, int y, int color) {
        c.fill(x1, y, x2, y + 1, color);
    }

    public static void vline(DrawContext c, int x, int y1, int y2, int color) {
        c.fill(x, y1, x + 1, y2, color);
    }

    /** 官方 SettingsPage 滑块 / 播放条进度：灰轨道 + ACCENT 已填充 + 白色小圆钮。 */
    public static void progress(DrawContext c, int x, int y, int w, int h, double frac, int color) {
        frac = Math.max(0.0, Math.min(1.0, frac));
        rounded(c, x, y, x + w, y + h, h / 2, TRACK);
        int fw = (int) Math.round(w * frac);
        if (fw > 0) rounded(c, x, y, x + Math.max(h, fw), y + h, h / 2, color);
        int kx = x + Math.max(0, Math.min(w - 4, fw - 2));
        c.fill(kx, y - 2, kx + 5, y + h + 2, WHITE);
    }

    /** 不带圆钮的超细进度条（播放条 / 灵动岛展开卡片）。 */
    public static void thinProgress(DrawContext c, int x, int y, int w, int h, double frac, int color) {
        frac = Math.max(0.0, Math.min(1.0, frac));
        c.fill(x, y, x + w, y + h, TRACK);
        int fw = (int) Math.round(w * frac);
        if (fw > 0) c.fill(x, y, x + Math.max(2, fw), y + h, color);
    }

    /** 官方 iOS 风格开关：SWITCH_OFF 灰 / GREEN 开 + 白色圆钮。 */
    public static void toggle(DrawContext c, int x, int y, int w, int h, boolean on, boolean hover) {
        int bg = on ? GREEN : (hover ? 0x3DFFFFFF : TRACK_OFF);
        rounded(c, x, y, x + w, y + h, h / 2, bg);
        int knob = h - 4;
        int kx = on ? x + w - knob - 2 : x + 2;
        rounded(c, kx, y + 2, kx + knob, y + 2 + knob, knob / 2, WHITE);
    }

    /** 胶囊按钮：hover 时略亮，文字居中。 */
    public static void pillButton(DrawContext c, TextRenderer font, int x1, int y1, int x2, int y2,
                                  int fill, String label, int textColor, boolean hover) {
        rounded(c, x1, y1, x2, y2, (y2 - y1) / 2, hover ? mix(fill, WHITE, 0.14f) : fill);
        textCenter(c, font, label, (x1 + x2) / 2, y1 + ((y2 - y1) - 8) / 2, textColor);
    }

    /** 官方 SettingsPage 键位按钮：胶囊 + 键名，捕获 / 冲突态由调用方给 fill。 */
    public static void keyCap(DrawContext c, TextRenderer font, int x, int y, int w, int h,
                              int fill, String label, int textColor) {
        rounded(c, x, y, x + w, y + h, h / 2, fill);
        textCenter(c, font, clip(font, label, w - 8), x + w / 2, y + (h - 8) / 2, textColor);
    }

    /** 封面占位：粉 / 蓝 / 紫的色块 + 顶部高光，按 hash 取色。 */
    public static void cover(DrawContext c, int x, int y, int w, int h, int r, int seed) {
        int i = (seed & 0x7FFFFFFF) % COVER_COLORS.length;
        int a = COVER_COLORS[i];
        int b = COVER_COLORS[(i + 3) % COVER_COLORS.length];
        rounded(c, x, y, x + w, y + h, r, mix(a, b, 0.45f));
        c.fill(x + r, y + 1, x + w - r, y + 2, 0x30FFFFFF);
    }

    /** 官方灵动岛跳动的音浪：4 根小柱子，底部对齐 yBottom。 */
    public static void waveBars(DrawContext c, int x, int yBottom, boolean playing, int maxH, int color) {
        double t = System.nanoTime() / 1e9;
        for (int i = 0; i < 4; i++) {
            int hh = playing
                    ? (int) (3 + (maxH - 3) * (0.5 + 0.5 * Math.sin(t * (5.5 + i * 1.9) + i * 1.3)))
                    : 3;
            c.fill(x + i * 4, yBottom - hh, x + i * 4 + 2, yBottom, color);
        }
    }

    // ===== 字符串缓存 =====

    private static final Map<String, String> TR_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> CLIP_CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_LIMIT = 512;

    /** 翻译结果缓存：避免每帧创建 Text 对象。语言切换时由 clearCaches() 清空。 */
    public static String tr(String key) {
        String v = TR_CACHE.get(key);
        if (v == null) {
            v = Text.translatable(key).getString();
            if (TR_CACHE.size() < CACHE_LIMIT) TR_CACHE.put(key, v);
        }
        return v;
    }

    /** 裁剪结果缓存：trimToWidth 会逐字符计算宽度，重复调用代价不小。 */
    public static String clip(TextRenderer font, String s, int maxWidth) {
        if (s == null || s.isEmpty()) return "";
        if (maxWidth <= 0) return "";
        String key = maxWidth + "\u0001" + s;
        String v = CLIP_CACHE.get(key);
        if (v == null) {
            v = font.trimToWidth(s, maxWidth);
            // 满了就整体清空，而不是「满了以后永不缓存」——后者会让长时间游戏后
            // 每帧都退化成逐字测宽（切歌、换歌单时尤其明显）。
            if (CLIP_CACHE.size() >= CACHE_LIMIT) CLIP_CACHE.clear();
            CLIP_CACHE.put(key, v);
        }
        return v;
    }

    public static void clearCaches() {
        TR_CACHE.clear();
        CLIP_CACHE.clear();
    }

    public static void text(DrawContext c, TextRenderer font, String s, int x, int y, int color) {
        c.drawTextWithShadow(font, s, x, y, color);
    }

    public static void textRight(DrawContext c, TextRenderer font, String s, int right, int y, int color) {
        c.drawTextWithShadow(font, s, right - font.getWidth(s), y, color);
    }

    public static void textCenter(DrawContext c, TextRenderer font, String s, int cx, int y, int color) {
        c.drawCenteredTextWithShadow(font, s, cx, y, color);
    }

    public static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
