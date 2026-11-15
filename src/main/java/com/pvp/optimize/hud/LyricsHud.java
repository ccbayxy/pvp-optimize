package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.dango.DangoLyrics;
import com.pvp.optimize.gui.neo.NeoUi;
import com.pvp.optimize.music.MusicLibrary;
import com.pvp.optimize.music.MusicPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * 歌词面板 —— 和药水 HUD 同一套「可调外观」：
 * 背景色 + 背景不透明度 + 圆角/直角 + 三种文字颜色 + 文字阴影 + 进度条 + 行数。
 *
 * <p>数据源两个：
 * <ul>
 *   <li>本 mod 本地播放器：读同名 {@code .lrc}（{@code MusicLibrary.lyricsFor}）；</li>
 *   <li>Dango 在线曲目：{@link DangoLyrics}（先读它自己的缓存，没有再异步调用官方取词接口）。</li>
 * </ul>
 * 选择规则是「谁真的在出声」：本地正在播放 → 本地；否则 Dango 有在显示的曲目 → Dango；
 * 都停下 → 本地暂停曲（保持面板不跳）。
 *
 * <p><b>性能</b>：卡片宽度按「整首歌最长一行」缓存，切歌才重算；每帧只做一次
 * {@code Lrc.lineAt}（二分）、≤5 次文字绘制，零后处理。文字过宽才裁剪（避免每帧 substring）。
 */
public final class LyricsHud {

    private LyricsHud() {}

    private static final int PAD_X = 7;
    private static final int PAD_Y = 4;
    private static final int LINE_H = 11;
    private static final int MAX_W = 300;
    private static final int MIN_W = 92;
    private static final int RADIUS = 6;

    /** 复用的行缓冲，避免每帧新建数组。 */
    private static final String[] LINES = new String[5];
    /** 每行对应的歌词下标，-1 表示空行。 */
    private static final int[] INDEX = new int[5];

    // 宽度缓存：同一首歌 + 同一种行数只量一次
    private static MusicLibrary.Lrc cacheLrc;
    private static int cacheRows = -1;
    private static int cacheW;

    public static void render(DrawContext ctx, MinecraftClient mc, PvPOptimizeConfig.Data cfg) {
        if (Boolean.FALSE.equals(cfg.lyricsHudEnabled)) return;
        if (!cfg.musicPlayerEnabled) return;
        if (mc.options.hudHidden) return;          // F1 隐藏 HUD 时不要自己再画一层

        // 数据源：谁在出声就用谁
        MusicLibrary.Lrc lrc = null;
        double pos = 0;
        boolean localPlaying = MusicPlayer.status() == MusicPlayer.Status.PLAYING;
        if (localPlaying) {
            lrc = MusicPlayer.lyrics();
            pos = MusicPlayer.position();
        }
        if (lrc == null || lrc.isEmpty()) {
            if (DangoLyrics.isShowing()) {
                MusicLibrary.Lrc dango = DangoLyrics.currentLrc();
                if (dango != null && !dango.isEmpty()) {
                    lrc = dango;
                    pos = DangoLyrics.shownPositionSec();
                }
            }
        }
        // Dango 没在显示（或这首确实没歌词）时，回落到本地暂停中的那首
        if ((lrc == null || lrc.isEmpty()) && MusicPlayer.status() != MusicPlayer.Status.STOPPED) {
            lrc = MusicPlayer.lyrics();
            pos = MusicPlayer.position();
        }
        if (lrc == null || lrc.isEmpty()) return;

        int rows = clamp(cfg.lyricsHudLines, 1, 5);
        int total = lrc.size();
        int cur = lrc.lineAt(pos);

        fillWindow(lrc, cur, rows, total);
        int contentW = width(lrc, rows);
        int contentH = rows * LINE_H + PAD_Y * 2;

        HudLayout.Rect rect = HudLayout.resolve(HudLayout.Id.LYRICS, mc, cfg, contentW, contentH);
        draw(ctx, mc, cfg, rect, rows, cur, lrc, pos, contentW, contentH);
    }

    /** HUD 布局编辑器用的示意面板（不依赖是否真的在放歌）。 */
    public static void renderPreview(DrawContext ctx, MinecraftClient mc,
                                     PvPOptimizeConfig.Data cfg, HudLayout.Rect rect) {
        int rows = fillDemo(cfg);
        int contentW = Math.max(MIN_W, width(mc.textRenderer, rows));
        int contentH = rows * LINE_H + PAD_Y * 2;
        HudLayout.Rect r = new HudLayout.Rect(rect.id, rect.x, rect.y,
                Math.round(contentW * rect.scale), Math.round(contentH * rect.scale), rect.scale);
        draw(ctx, mc, cfg, r, rows, rows / 2, null, 0, contentW, contentH);
    }

    /** 编辑器计算预览矩形用：与 {@link #renderPreview} 完全同一套尺寸。 */
    public static int[] previewSize(MinecraftClient mc, PvPOptimizeConfig.Data cfg) {
        int rows = fillDemo(cfg);
        int contentW = Math.max(MIN_W, width(mc.textRenderer, rows));
        return new int[]{contentW, rows * LINE_H + PAD_Y * 2};
    }

    private static int fillDemo(PvPOptimizeConfig.Data cfg) {
        int rows = clamp(cfg.lyricsHudLines, 1, 5);
        int mid = rows / 2;
        for (int i = 0; i < rows; i++) {
            INDEX[i] = i;
            if (i < mid) LINES[i] = "\u521a\u5531\u8fc7\u7684\u4e00\u884c\u6b4c\u8bcd";
            else if (i == mid) LINES[i] = "\u6b63\u5728\u64ad\u653e\u7684\u8fd9\u4e00\u884c\u6b4c\u8bcd";
            else LINES[i] = "\u5373\u5c06\u5230\u6765\u7684\u4e0b\u4e00\u884c";
        }
        return rows;
    }

    // ==================== 窗口与宽度 ====================

    private static void fillWindow(MusicLibrary.Lrc lrc, int cur, int rows, int total) {
        int half = rows / 2;
        int start = cur - half;
        if (cur < 0) start = 0;                       // 还没到第一句
        if (start + rows > total) start = Math.max(0, total - rows);
        if (start < 0) start = 0;
        for (int i = 0; i < rows; i++) {
            int li = start + i;
            if (li < 0 || li >= total) {
                INDEX[i] = -1;
                LINES[i] = "";
            } else {
                INDEX[i] = li;
                LINES[i] = lrc.textAt(li);
            }
        }
    }

    /** 整首歌最长一行决定卡片宽度：宽度稳定，切行不会左右跳。 */
    private static int width(MusicLibrary.Lrc lrc, int rows) {
        if (lrc == cacheLrc && rows == cacheRows && cacheW > 0) return cacheW;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int w = MIN_W;
        int n = lrc.size();
        for (int i = 0; i < n; i++) {
            String s = lrc.textAt(i);
            if (s == null || s.isEmpty()) continue;
            w = Math.max(w, font.getWidth(s) + PAD_X * 2 + 6);
            if (w >= MAX_W) { w = MAX_W; break; }
        }
        cacheLrc = lrc;
        cacheRows = rows;
        cacheW = Math.min(MAX_W, w);
        return cacheW;
    }

    private static int width(TextRenderer font, int rows) {
        int w = MIN_W;
        for (int i = 0; i < rows; i++) {
            if (LINES[i] == null || LINES[i].isEmpty()) continue;
            w = Math.max(w, font.getWidth(LINES[i]) + PAD_X * 2 + 6);
        }
        return Math.min(MAX_W, w);
    }

    // ==================== 绘制 ====================

    private static void draw(DrawContext ctx, MinecraftClient mc, PvPOptimizeConfig.Data cfg,
                             HudLayout.Rect rect, int rows, int curRow,
                             MusicLibrary.Lrc lrc, double pos, int contentW, int contentH) {
        TextRenderer font = mc.textRenderer;
        boolean bg = !Boolean.FALSE.equals(cfg.lyricsHudBg);
        boolean rounded = !Boolean.FALSE.equals(cfg.lyricsHudRounded);
        boolean shadow = !Boolean.FALSE.equals(cfg.lyricsHudShadow);
        boolean progress = !Boolean.FALSE.equals(cfg.lyricsHudProgress);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(rect.x, rect.y, 0);
        ctx.getMatrices().scale(rect.scale, rect.scale, 1.0f);

        if (bg) {
            int alpha = clamp(cfg.lyricsHudBgOpacity, 0, 100);
            int bgColor = (alpha * 255 / 100) << 24 | (cfg.lyricsHudBgColor & 0x00FFFFFF);
            if (rounded) {
                NeoUi.rrect(ctx, 0, 0, contentW, contentH, RADIUS, bgColor);
                NeoUi.outline(ctx, 0, 0, contentW, contentH, RADIUS, 0xFF2A3140);
            } else {
                // 和药水 HUD 一样：纯矩形填充，不带圆角与描边
                ctx.fill(0, 0, contentW, contentH, bgColor);
            }
        }

        int textMax = contentW - PAD_X * 2 - 4;
        int highlight = cfg.lyricsHudHighlightColor | 0xFF000000;
        int normal = cfg.lyricsHudTextColor | 0xFF000000;
        int past = NeoUi.fade(normal, 0.55f);

        for (int i = 0; i < rows; i++) {
            String line = LINES[i];
            if (line == null || line.isEmpty()) continue;
            int li = INDEX[i];
            int color;
            if (li < 0) {
                color = NeoUi.TEXT_FAINT;
            } else if (li == curRow) {
                color = highlight;
            } else if (li < curRow) {
                color = past;                          // 已经唱过：淡
            } else {
                color = normal;                        // 即将到来
            }
            int x = PAD_X;
            int y = PAD_Y + i * LINE_H;
            // 只有真的放不下才裁剪，避免每帧 substring 分配
            if (font.getWidth(line) > textMax) line = NeoUi.clip(font, line, textMax);
            if (shadow) ctx.drawTextWithShadow(font, line, x, y, color);
            else ctx.drawText(font, line, x, y, color, false);
        }

        // 当前行左侧主色竖条 + 行内卡拉 OK 进度（Dango 的进度条语汇）
        if (bg && rounded && curRow >= 0) {
            int barY = PAD_Y + curRow * LINE_H;
            ctx.fill(2, barY - 1, 4, barY + LINE_H - 2, highlight);
        }

        if (progress && lrc != null && curRow >= 0) {
            double from = lrc.timeAt(curRow);
            double to = lrc.timeAt(curRow + 1);
            if (to <= from) to = from + 4.0;
            float frac = (float) Math.max(0, Math.min(1, (pos - from) / (to - from)));
            int trackY = contentH - 2;
            ctx.fill(PAD_X, trackY, contentW - PAD_X, trackY + 1, 0x33FFFFFF);
            int pw = Math.round((contentW - PAD_X * 2) * frac);
            if (pw > 0) ctx.fill(PAD_X, trackY, PAD_X + pw, trackY + 1, highlight);
        }

        ctx.getMatrices().pop();
    }

    /** 切歌 / 换行数时清宽度缓存。 */
    public static void invalidate() {
        cacheLrc = null;
        cacheRows = -1;
        cacheW = 0;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
