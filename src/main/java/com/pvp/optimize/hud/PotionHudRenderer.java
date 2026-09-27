package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.MinecraftClient;
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
 * 药水时间 HUD (2026-09-27 新增).
 *
 * <p>显示当前玩家所有生效的 {@link StatusEffectInstance}, 在屏幕
 * 右上角绘制一个半透明面板, 每行:</p>
 *
 * <pre>
 *   速度 II          01:34   ← 绿色 (BENEFICIAL)
 *   力量             02:15   ← 绿色
 *   挖掘疲劳         00:45   ← 红色 (HARMFUL)
 * </pre>
 *
 * <p>格式参考用户提供的 PvP 服务器 "00:57" 倒计时样式. 长效效果
 * (>= 1 小时) 自动切换为 "1:23:45" 格式.</p>
 *
 * <p>受 {@link PvPOptimizeConfig.Data#potionHudEnabled} 控制;
 * 颜色分类由 {@link PvPOptimizeConfig.Data#potionHudColorByCategory} 控制;
 * 最大行数由 {@link PvPOptimizeConfig.Data#potionHudMaxLines} 控制 (0=不限).</p>
 */
public final class PotionHudRenderer {

    private PotionHudRenderer() {}

    private static final int BG_COLOR = 0x90000000;     // ARGB 半透明黑
    private static final int COLOR_BENEFICIAL = 0xFF55FF55; // 绿
    private static final int COLOR_HARMFUL    = 0xFFFF5555; // 红
    private static final int COLOR_NEUTRAL    = 0xFFFFFFFF; // 白
    private static final int COLOR_TITLE      = 0xFFFFAA00; // 橙 (面板标题)

    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 12;
    private static final int SCREEN_MARGIN = 8;

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

        // 收集所有生效效果
        List<StatusEffectInstance> effects = new ArrayList<>(
                mc.player.getStatusEffects());

        // 无效果则整面板隐藏 (节流, 不浪费 drawText 调用)
        if (effects.isEmpty()) return;

        // 排序: BENEFICIAL → NEUTRAL → HARMFUL, 同类按 duration 升序
        effects.sort(Comparator
                .comparingInt((StatusEffectInstance i) -> categoryOrder(i.getEffectType().value().getCategory()))
                .thenComparingInt(StatusEffectInstance::getDuration));

        int maxLines = cfg.potionHudMaxLines;
        if (maxLines > 0 && effects.size() > maxLines) {
            effects = effects.subList(0, maxLines);
        }

        // 构造标题行
        Text title = Text.literal("§6药水效果 (§l" + effects.size() + "§r§6)");
        // 构造所有行, 计算最大宽度
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(title, COLOR_TITLE));

        for (StatusEffectInstance inst : effects) {
            int color = cfg.potionHudColorByCategory
                    ? colorForCategory(inst.getEffectType().value().getCategory())
                    : COLOR_NEUTRAL;

            String name = effectName(inst);
            String amp = ampStr(inst.getAmplifier());
            String time = formatDuration(inst.getDuration());

            // "速度 II    01:34" —— 中间用空格对齐
            String body = name + amp + "  " + time;
            lines.add(new Line(Text.literal(body), color));
        }

        // 计算面板尺寸
        int textW = 0;
        for (Line l : lines) {
            int w = mc.textRenderer.getWidth(l.text);
            if (w > textW) textW = w;
        }
        int panelW = textW + PADDING * 2;
        int panelH = lines.size() * LINE_HEIGHT + PADDING * 2;

        // 屏幕右上角
        int screenW = mc.getWindow().getScaledWidth();
        int x = screenW - panelW - SCREEN_MARGIN;
        int y = SCREEN_MARGIN;

        ctx.fill(x, y, x + panelW, y + panelH, BG_COLOR);

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            ctx.drawText(mc.textRenderer, l.text,
                    x + PADDING,
                    y + PADDING + i * LINE_HEIGHT,
                    l.color, false);
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

    private static int colorForCategory(StatusEffectCategory c) {
        return switch (c) {
            case BENEFICIAL -> COLOR_BENEFICIAL;
            case HARMFUL -> COLOR_HARMFUL;
            case NEUTRAL -> COLOR_NEUTRAL;
        };
    }

    /** 通过 StatusEffect.getName() 获取本地化文本 (1.20.6 正确 API) */
    private static String effectName(StatusEffectInstance inst) {
        StatusEffect eff = inst.getEffectType().value();
        String translated = eff.getName().getString();
        if (translated == null || translated.isEmpty()) {
            Identifier id = Registries.STATUS_EFFECT.getId(eff);
            return id != null ? id.getPath() : "?";
        }
        return translated;
    }

    /** 等级显示: 0 级不显示, 1 级显示 "II", 2 级显示 "III"... */
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

    /** tick -> "MM:SS" 或 "H:MM:SS". 1 tick = 0.05s. */
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