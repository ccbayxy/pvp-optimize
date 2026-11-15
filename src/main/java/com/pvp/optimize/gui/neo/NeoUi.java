package com.pvp.optimize.gui.neo;

import com.pvp.optimize.gui.Ui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * 全新绘制层（与旧 {@code gui.Ui} / {@code gui.nl.NeverloseUi} 无任何继承或复用关系）。
 *
 * <p>设计原则，针对上一版被反馈的两个问题：
 * <ul>
 *   <li><b>图标不再依赖字体字形</b>：播放 / 暂停 / 上一首 / 下一首 / 音符 / 搜索 / 关闭 /
 *       下载 / 勾选 / 音量 全部用 1px 矩形与扫描线三角形画出来。文字字形（如 {@code ▶ ♪}）
 *       在资源包换字体后会变成缺字方块，这是「图标残缺」的根因。</li>
 *   <li><b>不再用透明覆盖当橡皮擦</b>：{@code fill} 的 alpha=0 不会擦除像素，旧代码用它
 *       挖圆环/放大镜的洞，结果只是叠了一层黑。这里所有形状都是「加法绘制」——
 *       圆环按内外半径逐行取区间，绝不覆盖已有像素。</li>
 *   <li><b>支持四角独立圆角</b>：窗口底、侧栏、标题栏可以各自只圆自己那一侧，
 *       拼在一起不会在角落露出方角台阶。</li>
 * </ul>
 *
 * <p>开销：所有形状都是常数级 {@code fill} 调用；只有窗口/侧栏这类大面板用扫描线，
 * 行数等于圆角半径（十几个）。每帧总填充次数在几百量级，低配机也无压力。
 */
public final class NeoUi {

    private NeoUi() {
    }

    // ==================== 调色板 ====================
    public static final int WINDOW        = 0xF20B0E15;   // 窗口主体（略透，露出模糊后的世界）
    public static final int WINDOW_EDGE   = 0xFF2A3140;   // 窗口描边
    public static final int TITLEBAR      = 0xFA0A0D13;   // 标题栏
    public static final int SIDEBAR       = 0xF70C1017;   // 侧栏（比标题栏略亮一档）
    public static final int CARD          = 0xFF161B25;   // 行卡片
    public static final int CARD_HOVER    = 0xFF1E2533;   // 行卡片悬停
    public static final int CARD_EDGE     = 0xFF242B39;   // 卡片描边
    public static final int FIELD         = 0xFF10151E;   // 输入框 / 数值框
    public static final int FIELD_HOVER   = 0xFF1B2230;
    public static final int TRACK         = 0xFF2B3242;   // 进度/滑块轨道
    public static final int ACCENT        = 0xFF4C8DFF;   // 主色
    public static final int ACCENT_SOFT   = 0x334C8DFF;
    public static final int ACCENT_DEEP   = 0xFF2F5DB3;
    public static final int TEXT          = 0xFFE9ECF3;
    public static final int TEXT_DIM      = 0xFF9AA3B6;
    public static final int TEXT_FAINT    = 0xFF6B7387;
    public static final int WHITE         = 0xFFFFFFFF;
    public static final int GREEN         = 0xFF3ED598;
    public static final int RED           = 0xFFFF5E6D;
    public static final int AMBER         = 0xFFFFB86B;
    public static final int BORDER        = 0xFF232A38;
    public static final int DIVIDER       = 0x14FFFFFF;
    public static final int SELECTION     = 0xFF1C2433;

    // ==================== 尺寸 ====================
    public static final int TITLE_H  = 48;
    public static final int PB_H     = 76;    // 播放条高度
    public static final int R_WINDOW = 14;
    public static final int R_CARD   = 8;
    public static final int R_FIELD  = 6;
    public static final int ROW_H    = 36;    // 设置行占位（含 3px 间隙）
    public static final int SWITCH_W = 34;
    public static final int SWITCH_H = 18;

    // ==================== 文本 ====================

    public static String tr(String key) {
        return Ui.tr(key);
    }

    public static String clip(TextRenderer f, String s, int maxW) {
        return Ui.clip(f, s, maxW);
    }

    public static void text(DrawContext c, TextRenderer f, String s, int x, int y, int color) {
        c.drawTextWithShadow(f, s, x, y, color);
    }

    public static void textRight(DrawContext c, TextRenderer f, String s, int right, int y, int color) {
        c.drawTextWithShadow(f, s, right - f.getWidth(s), y, color);
    }

    public static void textCenter(DrawContext c, TextRenderer f, String s, int cx, int y, int color) {
        c.drawTextWithShadow(f, s, cx - f.getWidth(s) / 2, y, color);
    }

    public static int mix(int a, int b, float t) {
        return Ui.mix(a, b, t);
    }

    public static int fade(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void clearCaches() {
        Ui.clearCaches();
    }

    // ==================== 形状 ====================

    public static void hline(DrawContext c, int x1, int x2, int y, int color) {
        if (x2 > x1) c.fill(x1, y, x2, y + 1, color);
    }

    public static void vline(DrawContext c, int x, int y1, int y2, int color) {
        if (y2 > y1) c.fill(x, y1, x + 1, y2, color);
    }

    /** 均匀圆角矩形：逐行扫描，圆弧真实（不是拿小方块凑）。 */
    public static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        rrect4(c, x1, y1, x2, y2, r, r, r, r, color);
    }

    /**
     * 四角独立圆角矩形。行内做插值 inset，保证拼合时角落不露方角。
     * 只有圆角区域才逐行填充，中间整块一次填充。
     */
    public static void rrect4(DrawContext c, int x1, int y1, int x2, int y2,
                              int rTL, int rTR, int rBR, int rBL, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int w = x2 - x1, h = y2 - y1;
        int max = Math.min(w, h) / 2;
        rTL = Math.max(0, Math.min(rTL, max));
        rTR = Math.max(0, Math.min(rTR, max));
        rBR = Math.max(0, Math.min(rBR, max));
        rBL = Math.max(0, Math.min(rBL, max));
        if (rTL == 0 && rTR == 0 && rBR == 0 && rBL == 0) {
            c.fill(x1, y1, x2, y2, color);
            return;
        }
        int topRows = Math.max(rTL, rTR);
        int botRows = Math.max(rBL, rBR);
        if (y2 - botRows > y1 + topRows) {
            c.fill(x1, y1 + topRows, x2, y2 - botRows, color);
        }
        for (int i = 0; i < topRows; i++) {
            int a = i < rTL ? insetOf(i, rTL) : 0;
            int b = i < rTR ? insetOf(i, rTR) : 0;
            if (x2 - b > x1 + a) c.fill(x1 + a, y1 + i, x2 - b, y1 + i + 1, color);
        }
        for (int i = 0; i < botRows; i++) {
            int a = i < rBL ? insetOf(i, rBL) : 0;
            int b = i < rBR ? insetOf(i, rBR) : 0;
            if (x2 - b > x1 + a) c.fill(x1 + a, y2 - i - 1, x2 - b, y2 - i, color);
        }
        // 中间兜底：圆角半径差很大时补齐缝隙
        if (y2 - botRows > y1 + topRows) {
            c.fill(x1, y1 + topRows, x2, y2 - botRows, color);
        }
    }

    /** 第 i 行（从圆角外沿数）需要内缩多少像素。 */
    private static int insetOf(int i, int r) {
        double dy = r - i - 0.5;
        double dx = Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
        return (int) Math.round(r - dx);
    }

    /** 1px 圆角描边（只描外圈，不做填充）。 */
    public static void outline(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        r = Math.max(1, Math.min(r, Math.min(x2 - x1, y2 - y1) / 2));
        hline(c, x1 + r, x2 - r, y1, color);
        hline(c, x1 + r, x2 - r, y2 - 1, color);
        vline(c, x1, y1 + r, y2 - r, color);
        vline(c, x2 - 1, y1 + r, y2 - r, color);
        for (int i = 0; i < r; i++) {
            int a = insetOf(i, r);
            c.fill(x1 + a, y1 + i, x1 + a + 1, y1 + i + 1, color);
            c.fill(x2 - a - 1, y1 + i, x2 - a, y1 + i + 1, color);
            c.fill(x1 + a, y2 - i - 1, x1 + a + 1, y2 - i, color);
            c.fill(x2 - a - 1, y2 - i - 1, x2 - a, y2 - i, color);
        }
    }

    /** 实心圆：按行取半弦长，边缘完整（不含任何透明覆盖）。 */
    public static void disc(DrawContext c, int cx, int cy, int r, int color) {
        if (r <= 0) return;
        for (int i = -r; i < r; i++) {
            double dy = i + 0.5;
            int dx = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            if (dx > 0) c.fill(cx - dx, cy + i, cx + dx, cy + i + 1, color);
        }
    }

    /** 圆环：逐行只填「外圆减内圆」的两段区间，绝不靠覆盖挖洞。 */
    public static void ring(DrawContext c, int cx, int cy, int r, int thickness, int color) {
        if (r <= 0) return;
        int t = Math.max(1, thickness);
        int ir = Math.max(0, r - t);
        for (int i = -r; i < r; i++) {
            double dy = i + 0.5;
            int od = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            int id = ir > 0 ? (int) Math.round(Math.sqrt(Math.max(0.0, (double) ir * ir - dy * dy))) : 0;
            if (od <= 0) continue;
            if (id <= 0) {
                c.fill(cx - od, cy + i, cx + od, cy + i + 1, color);
            } else {
                c.fill(cx - od, cy + i, cx - id, cy + i + 1, color);
                c.fill(cx + id, cy + i, cx + od, cy + i + 1, color);
            }
        }
    }

    /** 窗口柔影：三层向下偏移的圆角暗块，给面板「浮起来」的层次。 */
    public static void shadow(DrawContext c, int x1, int y1, int x2, int y2, int r) {
        final int[] spread = {16, 9, 4};
        final int[] alpha = {0x1E, 0x22, 0x28};
        for (int k = 0; k < spread.length; k++) {
            int s = spread[k];
            rrect(c, x1 - s, y1 - s + 4, x2 + s, y2 + s + 4, r + s, alpha[k] << 24);
        }
    }

    /** 进度条：轨道 + 已播放段，两端半圆。 */
    public static void progress(DrawContext c, int x, int y, int w, int h, double frac, int color) {
        if (w <= 0) return;
        frac = Math.max(0, Math.min(1, frac));
        int hh = Math.max(2, h);
        rrect(c, x, y, x + w, y + hh, hh / 2, TRACK);
        int fw = (int) Math.round(w * frac);
        if (fw >= hh) rrect(c, x, y, x + fw, y + hh, hh / 2, color);
        else if (fw > 0) rrect(c, x, y, x + fw, y + hh, Math.max(1, fw / 2), color);
    }

    /** 细滚动条：给定视口与内容高度，thumb 长度按比例。 */
    public static void scrollbar(DrawContext c, int x, int y1, int y2, int viewH, int totalH,
                                 int scroll, int maxScroll, boolean hover) {
        int h = y2 - y1;
        if (totalH <= viewH || h <= 0) return;
        rrect(c, x, y1, x + 3, y2, 2, 0x18FFFFFF);
        int thumbH = Math.max(26, h * viewH / totalH);
        int thumbY = y1 + (int) ((h - thumbH) * (maxScroll <= 0 ? 0 : (double) scroll / maxScroll));
        rrect(c, x, thumbY, x + 3, thumbY + thumbH, 2, hover ? ACCENT : 0x55FFFFFF);
    }

    // ==================== 矢量图标（无字体依赖）====================

    private static void diag(DrawContext c, int x, int y, int len, int dx, int dy, int color) {
        for (int i = 0; i < len; i++) {
            c.fill(x + i * dx, y + i * dy, x + i * dx + 1, y + i * dy + 1, color);
        }
    }

    /** ▶ 播放：左竖边、右侧尖角的实心三角。 */
    public static void iconPlay(DrawContext c, int cx, int cy, int size, int color) {
        int h = Math.max(5, size);
        int w = Math.max(4, Math.round(size * 0.68f));
        int x0 = cx - Math.max(2, w / 2);
        int y0 = cy - h / 2;
        for (int i = 0; i < h; i++) {
            double t = (i + 0.5 - h / 2.0) / (h / 2.0);
            int ww = (int) Math.round(w * (1.0 - Math.abs(t) * 0.9));
            if (ww > 0) c.fill(x0, y0 + i, x0 + ww, y0 + i + 1, color);
        }
    }

    /** ⏸ 暂停：两根竖条。 */
    public static void iconPause(DrawContext c, int cx, int cy, int size, int color) {
        int h = Math.max(5, size);
        int bw = Math.max(2, size / 4);
        int gap = Math.max(2, size / 4);
        int y0 = cy - h / 2;
        int x0 = cx - (bw * 2 + gap) / 2;
        rrect(c, x0, y0, x0 + bw, y0 + h, 1, color);
        rrect(c, x0 + bw + gap, y0, x0 + bw * 2 + gap, y0 + h, 1, color);
    }

    /** ⏮ 上一首：左侧竖线 + 向左的三角。 */
    public static void iconPrev(DrawContext c, int cx, int cy, int size, int color) {
        int h = Math.max(5, size);
        int w = Math.max(4, Math.round(size * 0.62f));
        int barW = Math.max(1, size / 7);
        int x0 = cx - (w + barW) / 2;
        for (int i = 0; i < h; i++) {
            double t = (i + 0.5 - h / 2.0) / (h / 2.0);
            int ww = (int) Math.round(w * (1.0 - Math.abs(t) * 0.9));
            if (ww > 0) c.fill(x0 + barW, cy - h / 2 + i, x0 + barW + ww, cy - h / 2 + i + 1, color);
        }
        rrect(c, x0, cy - h / 2, x0 + barW, cy - h / 2 + h, 0, color);
    }

    /** ⏭ 下一首：向右的三角 + 右侧竖线。 */
    public static void iconNext(DrawContext c, int cx, int cy, int size, int color) {
        int h = Math.max(5, size);
        int w = Math.max(4, Math.round(size * 0.62f));
        int barW = Math.max(1, size / 7);
        int total = w + barW;
        int x0 = cx - total / 2;
        int y0 = cy - h / 2;
        for (int i = 0; i < h; i++) {
            double t = (i + 0.5 - h / 2.0) / (h / 2.0);
            int ww = (int) Math.round(w * (1.0 - Math.abs(t) * 0.9));
            if (ww > 0) c.fill(x0, y0 + i, x0 + ww, y0 + i + 1, color);
        }
        rrect(c, x0 + w, y0, x0 + total, y0 + h, 0, color);
    }

    /** ♪ 音符：符头圆 + 符干 + 符尾。 */
    public static void iconNote(DrawContext c, int cx, int cy, int size, int color) {
        int s = Math.max(6, size);
        int headR = Math.max(2, s / 4);
        int hx = cx - s / 5;
        int hy = cy + s / 3;
        disc(c, hx, hy, headR, color);
        int stemX = hx + headR - 1;
        int top = cy - s / 2;
        vline(c, stemX, top, hy, color);
        vline(c, stemX + 1, top, hy, color);
        hline(c, stemX, stemX + Math.max(2, s / 3), top, color);
        hline(c, stemX, stemX + Math.max(2, s / 3) - 1, top + 1, color);
        hline(c, stemX, stemX + Math.max(1, s / 4), top + 3, color);
    }

    /** 放大镜：圆环 + 斜手柄。 */
    public static void iconSearch(DrawContext c, int cx, int cy, int size, int color) {
        int r = Math.max(3, size / 3);
        ring(c, cx - 1, cy - 1, r, Math.max(1, size / 7), color);
        int hx = cx - 1 + r - 1;
        int hy = cy - 1 + r - 1;
        diag(c, hx, hy, Math.max(3, size / 3), 1, 1, color);
        diag(c, hx + 1, hy, Math.max(2, size / 3 - 1), 1, 1, color);
    }

    /** 下拉箭头。 */
    public static void iconChevronDown(DrawContext c, int cx, int cy, int w, int color) {
        int half = Math.max(2, w / 2);
        for (int i = 0; i < half; i++) {
            c.fill(cx - i - 1, cy + i, cx - i, cy + i + 1, color);
            c.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
        }
    }

    /** 右箭头（子菜单）。 */
    public static void iconChevronRight(DrawContext c, int cx, int cy, int h, int color) {
        int half = Math.max(2, h / 2);
        for (int i = 0; i < half; i++) {
            c.fill(cx + i, cy - i - 1, cx + i + 1, cy - i, color);
            c.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
        }
    }

    /** 关闭 X。 */
    public static void iconClose(DrawContext c, int cx, int cy, int size, int color) {
        int half = Math.max(3, size / 2);
        for (int i = -half; i <= half; i++) {
            c.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
            c.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
        }
    }

    /** 下载箭头：竖线 + 三角头 + 底部托盘。 */
    public static void iconDownload(DrawContext c, int cx, int cy, int size, int color) {
        int h = Math.max(6, size);
        int top = cy - h / 2;
        vline(c, cx - 1, top, cy + 1, color);
        vline(c, cx, top, cy + 1, color);
        for (int i = 0; i < h / 3; i++) {
            c.fill(cx - 1 - i, cy - i, cx + 1 + i, cy - i + 1, color);
        }
        hline(c, cx - h / 3, cx + h / 3 + 1, top + h - 1, color);
        hline(c, cx - h / 3, cx + h / 3 + 1, top + h - 2, color);
    }

    /** 勾选。 */
    public static void iconCheck(DrawContext c, int cx, int cy, int size, int color) {
        int s = Math.max(4, size / 2);
        diag(c, cx - s, cy, s, 1, 1, color);
        diag(c, cx - s + 1, cy, s, 1, 1, color);
        diag(c, cx, cy + s, s + 1, 1, -1, color);
        diag(c, cx, cy + s - 1, s + 1, 1, -1, color);
    }

    /** 音量：喇叭梯形 + 声波弧（弧用两条斜线近似）。 */
    public static void iconVolume(DrawContext c, int cx, int cy, int color) {
        int h = 6;
        c.fill(cx - 5, cy - 1, cx - 3, cy + 1, color);
        for (int i = 0; i < h / 2; i++) {
            c.fill(cx - 3, cy - i - 1, cx - 1, cy + i + 2, color);
        }
        c.fill(cx + 1, cy - 3, cx + 2, cy - 2, color);
        c.fill(cx + 2, cy - 2, cx + 3, cy - 1, color);
        c.fill(cx + 2, cy + 2, cx + 3, cy + 3, color);
        c.fill(cx + 1, cy + 3, cx + 2, cy + 4, color);
    }

    /** 循环：上下两条带箭头的横线。 */
    public static void iconRepeat(DrawContext c, int cx, int cy, int color) {
        hline(c, cx - 4, cx + 3, cy - 3, color);
        hline(c, cx - 3, cx + 4, cy + 2, color);
        // 右端下折箭头
        c.fill(cx + 3, cy - 3, cx + 4, cy - 1, color);
        c.fill(cx + 2, cy - 2, cx + 3, cy - 1, color);
        // 左端上折箭头
        c.fill(cx - 4, cy, cx - 3, cy + 2, color);
        c.fill(cx - 3, cy + 1, cx - 2, cy + 2, color);
    }

    /** 随机：两条交叉线 + 箭头。 */
    public static void iconShuffle(DrawContext c, int cx, int cy, int color) {
        diag(c, cx - 4, cy - 3, 8, 1, 1, color);
        diag(c, cx - 4, cy + 3, 8, 1, -1, color);
        c.fill(cx + 3, cy + 2, cx + 5, cy + 4, color);
        c.fill(cx + 3, cy - 4, cx + 5, cy - 2, color);
    }

    /** 网格（HUD 布局）：2×2 小方块。 */
    public static void iconGrid(DrawContext c, int cx, int cy, int color) {
        rrect(c, cx - 5, cy - 5, cx - 1, cy - 1, 1, color);
        rrect(c, cx + 1, cy - 5, cx + 5, cy - 1, 1, color);
        rrect(c, cx - 5, cy + 1, cx - 1, cy + 5, 1, color);
        rrect(c, cx + 1, cy + 1, cx + 5, cy + 5, 1, color);
    }

    /** 封面占位：单色块 + 顶部高光 + 中央音符（alpha=0 时不画音符）。 */
    public static void cover(DrawContext c, int x, int y, int w, int h, int r, int seed, int noteColor) {
        final int[] colors = {
                0xFF2C3550, 0xFF3A2E48, 0xFF254038, 0xFF40392A,
                0xFF2D3250, 0xFF3E2B38, 0xFF263D4C, 0xFF33402F
        };
        rrect(c, x, y, x + w, y + h, r, colors[Math.abs(seed) % colors.length]);
        hline(c, x + r, x + w - r, y, 0x22FFFFFF);
        if ((noteColor >>> 24) != 0) {
            iconNote(c, x + w / 2, y + h / 2, Math.max(8, Math.min(w, h) / 2), noteColor);
        }
    }

    /** 开关：轨道 + 圆钮，开启时主色实心。 */
    public static void toggle(DrawContext c, int x, int y, int w, int h, boolean on, boolean hover) {
        int track = on ? (hover ? mix(ACCENT, WHITE, 0.12f) : ACCENT) : (hover ? 0xFF2A3242 : 0xFF202633);
        rrect(c, x, y, x + w, y + h, h / 2, track);
        if (!on) outline(c, x, y, x + w, y + h, h / 2, 0x1AFFFFFF);
        int kr = Math.max(3, h / 2 - 3);
        int kx = on ? x + w - h / 2 : x + h / 2;
        disc(c, kx, y + h / 2, kr, on ? WHITE : 0xFF8B93A6);
    }

    /** 滑块：轨道 + 主色已选段 + 白色圆钮。 */
    public static void slider(DrawContext c, TextRenderer f, int x, int y, int w,
                              double frac, String value, boolean hover) {
        frac = Math.max(0, Math.min(1, frac));
        int ty = y + 6;
        rrect(c, x, ty, x + w, ty + 4, 2, hover ? 0xFF353D4E : TRACK);
        int fw = (int) Math.round(w * frac);
        if (fw > 1) rrect(c, x, ty, x + fw, ty + 4, 2, ACCENT);
        disc(c, x + fw, ty + 2, 5, WHITE);
        disc(c, x + fw, ty + 2, 3, ACCENT);
        int boxW = Math.max(34, f.getWidth(value) + 14);
        int bx = x + w + 10;
        rrect(c, bx, y - 3, bx + boxW, y + 13, R_FIELD, FIELD);
        textCenter(c, f, value, bx + boxW / 2, y + 1, hover ? WHITE : TEXT_DIM);
    }

    /** 下拉框。 */
    public static void combo(DrawContext c, TextRenderer f, int x, int y, int w, int h,
                             String value, boolean hover) {
        rrect(c, x, y, x + w, y + h, R_FIELD, hover ? FIELD_HOVER : FIELD);
        outline(c, x, y, x + w, y + h, R_FIELD, hover ? 0xFF39445A : CARD_EDGE);
        text(c, f, clip(f, value, w - 24), x + 8, y + (h - 8) / 2, hover ? WHITE : TEXT_DIM);
        iconChevronDown(c, x + w - 12, y + h / 2 - 2, 8, TEXT_FAINT);
    }
}
