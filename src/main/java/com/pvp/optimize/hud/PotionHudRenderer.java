package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 药水时间 HUD：每行一个生效药水的名称 / 等级 / 倒计时。
 *
 * <p>位置与大小由 {@link HudLayout} 统一托管（可在 HUD 布局编辑器里拖动 / 滚轮缩放）。
 * 绘制用矩阵 translate + scale，基准坐标从 (0,0) 起，彻底修掉旧版
 * 「先按 scale 算屏幕坐标、再对矩阵 scale」造成的偏移与双重缩放。
 *
 * <p><b>性能</b>：旧实现每帧都 {@code new ArrayList} + 排序 + 逐行 {@code Text.literal}，
 * 在 144Hz 下每秒产生上千个临时对象。现在改为「签名缓存」：每帧只遍历一次当前药水
 * 计算一个 long 签名（零分配），签名不变就直接复用上一次的行与宽度；签名里包含
 * 剩余秒数，所以倒计时跨秒时（每秒一次）才会真正重建。
 */
public final class PotionHudRenderer {

    private PotionHudRenderer() {}

    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 12;

    // ===== 签名缓存 =====
    private static long cacheSig = Long.MIN_VALUE;
    private static List<Line> cacheLines;
    private static int cacheW, cacheH;

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.potionHudEnabled) return;

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;   // 默认无药水时隐藏

        long sig = signature(effects, cfg);
        if (sig != cacheSig || cacheLines == null) {
            rebuild(effects, cfg, mc.textRenderer);
            cacheSig = sig;
        }
        if (cacheLines.isEmpty()) return;

        HudLayout.Rect rect = HudLayout.resolve(HudLayout.Id.POTION, mc, cfg, cacheW, cacheH);
        renderAt(ctx, mc, cfg, rect, cacheLines, cacheW, cacheH);
    }

    /** HUD 布局编辑器用的示例面板（不依赖当前是否真的有药水效果）。 */
    public static void renderPreview(DrawContext ctx, MinecraftClient mc,
                                     PvPOptimizeConfig.Data cfg, HudLayout.Rect rect) {
        List<Line> lines = List.of(
                new Line("\u00a76\u00a7l\u836f\u6c34\u6548\u679c\u00a7r\u00a76 (2)",
                        cfg.potionHudTitleColor),
                new Line("\u901f\u5ea6 II  01:34", cfg.potionHudBeneficialColor),
                new Line("\u529b\u91cf  02:15", cfg.potionHudBeneficialColor));
        int[] size = measure(mc.textRenderer, lines);
        renderAt(ctx, mc, cfg, rect, lines, size[0], size[1]);
    }

    private static void rebuild(Collection<StatusEffectInstance> source,
                                PvPOptimizeConfig.Data cfg, TextRenderer font) {
        List<StatusEffectInstance> effects = new ArrayList<>(source);
        effects.sort(Comparator
                .comparingInt((StatusEffectInstance i) -> categoryOrder(i.getEffectType().value().getCategory()))
                .thenComparingInt(StatusEffectInstance::getDuration));

        int maxLines = cfg.potionHudMaxLines;
        if (maxLines > 0 && effects.size() > maxLines) {
            effects = effects.subList(0, maxLines);
        }
        List<Line> lines = buildLines(effects, cfg);
        int[] size = measure(font, lines);
        cacheLines = lines;
        cacheW = size[0];
        cacheH = size[1];
    }

    /** 零分配签名：药水集合 + 等级 + 剩余秒数 + 影响排版/颜色的配置。 */
    private static long signature(Collection<StatusEffectInstance> effects,
                                  PvPOptimizeConfig.Data cfg) {
        long sig = 0x9E3779B97F4A7C15L * (effects.size() + 1)
                + cfg.potionHudMaxLines * 131L
                + (cfg.potionHudShowTitle ? 1 : 0) * 17L
                + (cfg.potionHudShowAmplifier ? 1 : 0) * 31L
                + (cfg.potionHudShowDuration ? 1 : 0) * 61L
                + (cfg.potionHudColorByCategory ? 1 : 0) * 97L
                + cfg.potionHudTitleColor * 7L
                + cfg.potionHudBeneficialColor * 11L
                + cfg.potionHudHarmfulColor * 13L
                + cfg.potionHudNeutralColor * 19L;
        for (StatusEffectInstance inst : effects) {
            sig = sig * 0x100000001B3L
                    + System.identityHashCode(inst.getEffectType().value())
                    + inst.getAmplifier() * 1_000_003L
                    + (inst.getDuration() / 20) * 7_919L;
        }
        return sig;
    }

    private static List<Line> buildLines(List<StatusEffectInstance> effects,
                                         PvPOptimizeConfig.Data cfg) {
        List<Line> lines = new ArrayList<>(effects.size() + 1);
        if (cfg.potionHudShowTitle) {
            lines.add(new Line("\u00a76\u00a7l\u836f\u6c34\u6548\u679c\u00a7r\u00a76 ("
                    + effects.size() + ")", cfg.potionHudTitleColor));
        }
        for (StatusEffectInstance inst : effects) {
            int color = cfg.potionHudColorByCategory
                    ? colorForCategory(inst.getEffectType().value().getCategory(), cfg)
                    : cfg.potionHudNeutralColor;
            StringBuilder sb = new StringBuilder();
            sb.append(effectName(inst));
            if (cfg.potionHudShowAmplifier) sb.append(ampStr(inst.getAmplifier()));
            if (cfg.potionHudShowDuration) sb.append("  ").append(formatDuration(inst.getDuration()));
            lines.add(new Line(sb.toString(), color));
        }
        return lines;
    }

    private static int[] measure(TextRenderer font, List<Line> lines) {
        int textW = 0;
        for (Line l : lines) textW = Math.max(textW, font.getWidth(l.text));
        return new int[]{textW + PADDING * 2, lines.size() * LINE_HEIGHT + PADDING * 2};
    }

    private static void renderAt(DrawContext ctx, MinecraftClient mc,
                                 PvPOptimizeConfig.Data cfg, HudLayout.Rect rect,
                                 List<Line> lines, int panelW, int panelH) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(rect.x, rect.y, 0);
        ctx.getMatrices().scale(rect.scale, rect.scale, 1.0f);

        int bgColor = (cfg.potionHudBgColor & 0x00FFFFFF)
                | ((int) (cfg.potionHudBgOpacity * 2.55f) << 24);
        ctx.fill(0, 0, panelW, panelH, bgColor);

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            ctx.drawText(mc.textRenderer, l.text, PADDING, PADDING + i * LINE_HEIGHT,
                    l.color, false);
        }
        ctx.getMatrices().pop();
    }

    // ============ helpers ============

    private static int categoryOrder(StatusEffectCategory c) {
        return switch (c) {
            case BENEFICIAL -> 0;
            case NEUTRAL -> 1;
            case HARMFUL -> 2;
        };
    }

    private static int colorForCategory(StatusEffectCategory c, PvPOptimizeConfig.Data cfg) {
        return switch (c) {
            case BENEFICIAL -> cfg.potionHudBeneficialColor;
            case HARMFUL -> cfg.potionHudHarmfulColor;
            case NEUTRAL -> cfg.potionHudNeutralColor;
        };
    }

    private static String effectName(StatusEffectInstance inst) {
        StatusEffect eff = inst.getEffectType().value();
        String translated = eff.getName().getString();
        if (translated == null || translated.isEmpty()) {
            Identifier id = Registries.STATUS_EFFECT.getId(eff);
            return id != null ? id.getPath() : "?";
        }
        return translated;
    }

    private static String ampStr(int amp) {
        if (amp <= 0) return "";
        return switch (amp) {
            case 1 -> " II";
            case 2 -> " III";
            case 3 -> " IV";
            case 4 -> " V";
            default -> " " + (amp + 1);
        };
    }

    private static String formatDuration(int ticks) {
        int totalSec = ticks / 20;
        if (totalSec < 0) totalSec = 0;
        int hours = totalSec / 3600;
        int mins = (totalSec % 3600) / 60;
        int secs = totalSec % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, mins, secs);
        return String.format("%02d:%02d", mins, secs);
    }

    /** 清缓存（切换语言 / 世界重载时调用）。 */
    public static void invalidate() {
        cacheSig = Long.MIN_VALUE;
        cacheLines = null;
    }

    private record Line(String text, int color) {}
}
