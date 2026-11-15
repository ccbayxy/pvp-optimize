package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.MinecraftClient;

/**
 * 自定义 HUD 布局（Badlion 风格：位置 + 缩放，可拖动编辑）。
 *
 * <p>两个目标：
 * <ul>
 *   <li>{@link Id#POTION} —— 药水时间面板（老功能，坐标 -1 表示自动靠右上）；</li>
 *   <li>{@link Id#LYRICS} —— Dango 风格歌词面板（坐标 {@code null} 表示自动顶部居中，
 *       正好落在灵动岛下方）。</li>
 * </ul>
 *
 * <p>播放器卡片本身仍然由 DangoMusic 官方灵动岛渲染（见 {@code dango/DangoHudFeed}），
 * 它自带位置与缩放，不在这套规则里。
 *
 * <p>坐标一律是<b>缩放后</b>的屏幕像素；绘制侧用 translate + scale 按 1:1 基准尺寸画，
 * 所以放大不会出现「先缩放坐标再缩放矩阵」的双重偏差。
 */
public final class HudLayout {

    private HudLayout() {}

    public enum Id { POTION, LYRICS }

    /** 自动停靠：x = -1。 */
    public static final int AUTO = -1;

    /** 编辑器里药水面板的预览基准尺寸。 */
    public static final int POTION_PREVIEW_W = 132;
    public static final int POTION_PREVIEW_H = 54;

    /** 歌词面板基准尺寸（1:1 时的内容尺寸，实际高度按行数算）。 */
    public static final int LYRICS_BASE_W = 180;
    public static final int LYRICS_LINE_H = 11;
    public static final int LYRICS_PAD = 5;

    /** 歌词默认纵向位置：灵动岛（顶部 6px、高约 30px）正下方留一点空隙。 */
    public static final int LYRICS_AUTO_Y = 42;
    /** 与药水面板横向相撞时，歌词让到它下面的位置（药水默认 8 + 高约 54 + 间距）。 */
    public static final int LYRICS_AUTO_Y_CLEAR = 66;
    /** 为右上角药水面板预留的横向宽度（含边距）。 */
    private static final int POTION_RESERVE_W = 104;

    public static final class Rect {
        public final Id id;
        public final int x, y, w, h;
        public final float scale;

        Rect(Id id, int x, int y, int w, int h, float scale) {
            this.id = id; this.x = x; this.y = y;
            this.w = w; this.h = h; this.scale = scale;
        }

        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public static boolean custom(PvPOptimizeConfig.Data d) {
        return !Boolean.FALSE.equals(d.hudCustomLayoutEnabled);
    }

    public static float scale(PvPOptimizeConfig.Data d, Id id) {
        int pct = id == Id.LYRICS
                ? (d.lyricsHudScale == null ? 100 : d.lyricsHudScale)
                : d.potionHudScale;
        return clamp(pct, 50, 200) / 100.0f;
    }

    public static Rect resolve(Id id, MinecraftClient mc, PvPOptimizeConfig.Data d,
                               int contentW, int contentH) {
        int sw = Math.max(1, mc.getWindow().getScaledWidth());
        int sh = Math.max(1, mc.getWindow().getScaledHeight());
        float s = scale(d, id);
        int w = Math.max(1, Math.round(contentW * s));
        int h = Math.max(1, Math.round(contentH * s));

        int x = clamp(autoX(id, d, sw, w), 0, Math.max(0, sw - w));
        int y = clamp(autoY(id, d, sw, sh, h, x, w), 0, Math.max(0, sh - h));
        return new Rect(id, x, y, w, h, s);
    }

    /** 自动横向锚点：药水靠右上，歌词居中（右侧留给药水面板，挤不下就贴左边 8px）。 */
    private static int autoX(Id id, PvPOptimizeConfig.Data d, int sw, int w) {
        if (id == Id.LYRICS) {
            if (d.lyricsHudX != null && d.lyricsHudX >= 0 && custom(d)) return d.lyricsHudX;
            int centered = (sw - w) / 2;
            if (centered + w > sw - POTION_RESERVE_W) return 8;   // 会和右上药水面板叠在一起
            return Math.max(8, centered);
        }
        if (!custom(d)) {
            boolean left = d.potionHudPosition != null && d.potionHudPosition.endsWith("LEFT");
            return left ? 4 : sw - w - 4;
        }
        return d.potionHudX < 0 ? sw - w - 8 : d.potionHudX;
    }

    /**
     * 自动纵向锚点：药水 8px；歌词落在灵动岛下方。
     *
     * <p>歌词默认居中，而药水面板占着右上角 —— 窄屏上两者会在 42px 这一行撞上，
     * 这时把歌词整块下移到药水面板下方（{@link #LYRICS_AUTO_Y_CLEAR}），而不是横向挤在一起。
     */
    private static int autoY(Id id, PvPOptimizeConfig.Data d, int sw, int sh, int h, int x, int w) {
        if (id == Id.LYRICS) {
            if (d.lyricsHudY != null && d.lyricsHudY >= 0 && custom(d)) return d.lyricsHudY;
            int y = LYRICS_AUTO_Y;
            if (x + w > sw - POTION_RESERVE_W) {
                // 仍在药水面板的横向范围内：让到它下面
                y = LYRICS_AUTO_Y_CLEAR;
            }
            return Math.min(y, Math.max(0, sh - h - 4));
        }
        if (!custom(d)) {
            boolean bottom = d.potionHudPosition != null && d.potionHudPosition.startsWith("BOTTOM");
            return bottom ? sh - h - 4 : 8;
        }
        return d.potionHudY < 0 ? 8 : d.potionHudY;
    }

    public static void setPos(Id id, MinecraftClient mc, PvPOptimizeConfig.Data d,
                              int x, int y, int contentW, int contentH) {
        int sw = Math.max(1, mc.getWindow().getScaledWidth());
        int sh = Math.max(1, mc.getWindow().getScaledHeight());
        float s = scale(d, id);
        int w = Math.max(1, Math.round(contentW * s));
        int h = Math.max(1, Math.round(contentH * s));
        int cx = clamp(x, 0, Math.max(0, sw - w));
        int cy = clamp(y, 0, Math.max(0, sh - h));
        if (id == Id.LYRICS) {
            d.lyricsHudX = cx;
            d.lyricsHudY = cy;
        } else {
            d.potionHudX = cx;
            d.potionHudY = cy;
        }
    }

    public static void setScale(Id id, PvPOptimizeConfig.Data d, int pct) {
        int v = clamp(pct, 50, 200);
        if (id == Id.LYRICS) {
            d.lyricsHudScale = v;
        } else {
            d.potionHudScale = v;
        }
    }

    public static void reset(Id id) {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        if (id == Id.LYRICS) {
            d.lyricsHudX = null;
            d.lyricsHudY = null;
            d.lyricsHudScale = 100;
        } else {
            d.potionHudX = AUTO;
            d.potionHudY = 8;
            d.potionHudScale = 100;
        }
        PvPOptimizeConfig.save();
    }

    public static void resetAll() {
        reset(Id.POTION);
        reset(Id.LYRICS);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
