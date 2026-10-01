package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.config.PvPOptimizeConfigScreen.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 药水时间 HUD (2026-09-27 v5).
 *
 * <p>在屏幕一角绘制半透明面板, 每行显示一个生效药水的名称/等级/倒计时.
 * showing the active effect name, level, and remaining duration.
 *
 * <pre>
 *   药水效果 (3)
 *   速度 II           01:34   ← 绿
 *   力量              02:15   ← 绿
 *   挖掘疲劳          00:45   ← 红
 * </pre>
 */
public final class PotionHudRenderer {

    private PotionHudRenderer() {}

    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 12;
    private static final int SCREEN_MARGIN = 8;

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.potionHudEnabled) return;

        List<StatusEffectInstance> effects = new ArrayList<>(
                mc.player.getStatusEffects());

        if (effects.isEmpty()) {
            if (cfg.potionHudHideWhenEmpty) return;
            // Otherwise show empty title: user likely wants default hide, just exit
            return;
        }

        effects.sort(Comparator
                .comparingInt((StatusEffectInstance i) -> categoryOrder(i.getEffectType().value().getCategory()))
                .thenComparingInt(StatusEffectInstance::getDuration));

        int maxLines = cfg.potionHudMaxLines;
        if (maxLines > 0 && effects.size() > maxLines) {
            effects = effects.subList(0, maxLines);
        }

        // 构建行
        List<Line> lines = new ArrayList<>();
        if (cfg.potionHudShowTitle) {
            lines.add(new Line(
                    Text.literal("\u00a76\u00a7l\u836f\u6c34\u6548\u679c\u00a7r\u00a76 (" + effects.size() + ")"),
                    cfg.potionHudTitleColor));
        }
        for (StatusEffectInstance inst : effects) {
            int color = cfg.potionHudColorByCategory
                    ? colorForCategory(inst.getEffectType().value().getCategory(), cfg)
                    : cfg.potionHudNeutralColor;

            StringBuilder sb = new StringBuilder();
            sb.append(effectName(inst));
            if (cfg.potionHudShowAmplifier) {
                sb.append(ampStr(inst.getAmplifier()));
            }
            if (cfg.potionHudShowDuration) {
                sb.append("  ").append(formatDuration(inst.getDuration()));
            }
            lines.add(new Line(Text.literal(sb.toString()), color));
        }

        // 计算面板原始尺寸
        TextRenderer font = mc.textRenderer;
        int textW = 0;
        for (Line l : lines) textW = Math.max(textW, font.getWidth(l.text));
        int panelW = textW + PADDING * 2;
        int panelH = lines.size() * LINE_HEIGHT + PADDING * 2;

        // 计算缩放
        float scale = cfg.potionHudScale / 100.0f;
        if (scale != 1.0f) {
            // 缩放: 通过 matrix 实现
            ctx.getMatrices().push();
            ctx.getMatrices().scale(scale, scale, 1.0f);
            renderPanel(ctx, mc, lines, panelW, panelH, scale);
            ctx.getMatrices().pop();
        } else {
            renderPanel(ctx, mc, lines, panelW, panelH, 1.0f);
        }
    }

    private static void renderPanel(DrawContext ctx, MinecraftClient mc,
                                    List<Line> lines, int panelW, int panelH, float scale) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        HudPosition pos;
        try {
            pos = HudPosition.valueOf(cfg.potionHudPosition);
        } catch (IllegalArgumentException e) {
            pos = HudPosition.TOP_RIGHT;
        }

        // 屏幕坐标 (scaled). 把边距也按 scale 缩放
        int scaledMargin = Math.round(SCREEN_MARGIN * scale);
        int x, y;
        switch (pos) {
            case TOP_LEFT -> { x = scaledMargin; y = scaledMargin; }
            case TOP_RIGHT -> { x = screenW - (int)(panelW * scale) - scaledMargin; y = scaledMargin; }
            case BOTTOM_LEFT -> { x = scaledMargin; y = screenH - (int)(panelH * scale) - scaledMargin; }
            case BOTTOM_RIGHT -> { x = screenW - (int)(panelW * scale) - scaledMargin; y = screenH - (int)(panelH * scale) - scaledMargin; }
            default -> { x = screenW - (int)(panelW * scale) - scaledMargin; y = scaledMargin; }
        }

        // 背景: 根据 bgOpacity 调整 A
        int bgColor = (cfg.potionHudBgColor & 0x00FFFFFF)
                | ((int)(cfg.potionHudBgOpacity * 2.55f) << 24);
        ctx.fill(x, y, x + (int)(panelW * scale), y + (int)(panelH * scale), bgColor);

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            int ly = y + Math.round((PADDING + i * LINE_HEIGHT) * scale);
            int lx = x + Math.round(PADDING * scale);
            ctx.drawText(mc.textRenderer, l.text, lx, ly, l.color, false);
        }
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
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, mins, secs);
        }
        return String.format("%02d:%02d", mins, secs);
    }

    private record Line(Text text, int color) {}
}