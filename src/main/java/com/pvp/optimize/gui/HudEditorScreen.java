package com.pvp.optimize.gui;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.gui.neo.NeoUi;
import com.pvp.optimize.hud.HudLayout;
import com.pvp.optimize.hud.LyricsHud;
import com.pvp.optimize.hud.PotionHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * HUD 布局编辑器 —— 一块透明浮层，直接拖真实尺寸的预览。
 *
 * <p>两个目标：<b>药水面板</b> 与 <b>歌词面板</b>。点哪个面板就选中哪个，
 * 拖动移动、滚轮缩放、右键复位；底部还有两颗胶囊可以直接切换目标。
 *
 * <p>播放器卡片本身由 DangoMusic 官方灵动岛渲染，它自带位置与缩放
 * （主控板「音乐」页调灵动岛缩放），不在这套规则里。
 *
 * <p>画面始终透明：不画背景、不压暗、<b>不做任何模糊</b>，世界照常渲染 ——
 * 编辑器本身不会是掉帧源。
 */
public final class HudEditorScreen extends Screen {

    private final Screen parent;
    private boolean dragging, resizing;
    private int dragDX, dragDY, resizeStartY, resizeStartScale;
    private HudLayout.Id target = HudLayout.Id.POTION;

    private int barX, barY, barW, barH;
    private int doneX, resetX;
    private int chipAX, chipBX, chipW;
    private static final int BTN_W = 62;
    private static final int BTN_H = 22;
    private static final int CHIP_H = 18;

    public static void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new HudEditorScreen(mc.currentScreen));
    }

    private HudEditorScreen(Screen parent) {
        super(Text.translatable("gui.pvp_optimize.hud_editor.title"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 不画任何背景：透明、无模糊
    }

    @Override
    protected void init() {
        PvPOptimizeConfig.get().hudCustomLayoutEnabled = true;
        PvPOptimizeConfig.save();
        barW = Math.min(400, width - 24);
        barH = 46;
        barX = (width - barW) / 2;
        barY = Math.max(6, height - barH - 12);
        resetX = barX + barW - 12 - BTN_W;
        doneX = resetX - 6 - BTN_W;
        chipW = 52;
        chipAX = barX + 12;
        chipBX = chipAX + chipW + 6;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        init();
    }

    private HudLayout.Rect rectOf(HudLayout.Id id, PvPOptimizeConfig.Data d) {
        if (id == HudLayout.Id.LYRICS) {
            int[] size = LyricsHud.previewSize(client, d);
            return HudLayout.resolve(HudLayout.Id.LYRICS, client, d, size[0], size[1]);
        }
        return HudLayout.resolve(HudLayout.Id.POTION, client, d,
                HudLayout.POTION_PREVIEW_W, HudLayout.POTION_PREVIEW_H);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();

        HudLayout.Rect potion = rectOf(HudLayout.Id.POTION, d);
        HudLayout.Rect lyrics = rectOf(HudLayout.Id.LYRICS, d);

        // 选中的最后画，压在另一个上面
        if (target == HudLayout.Id.LYRICS) {
            PotionHudRenderer.renderPreview(ctx, client, d, potion);
            LyricsHud.renderPreview(ctx, client, d, lyrics);
        } else {
            LyricsHud.renderPreview(ctx, client, d, lyrics);
            PotionHudRenderer.renderPreview(ctx, client, d, potion);
        }
        drawSelection(ctx, potion, HudLayout.Id.POTION, d);
        drawSelection(ctx, lyrics, HudLayout.Id.LYRICS, d);
        drawBar(ctx, mx, my);
    }

    private void drawBar(DrawContext ctx, int mx, int my) {
        NeoUi.rrect(ctx, barX, barY, barX + barW, barY + barH, 10, NeoUi.WINDOW);
        NeoUi.outline(ctx, barX, barY, barX + barW, barY + barH, 10, NeoUi.WINDOW_EDGE);

        chip(ctx, chipAX, HudLayout.Id.POTION, NeoUi.tr("gui.pvp_optimize.hud_editor.potion"), mx, my);
        chip(ctx, chipBX, HudLayout.Id.LYRICS, NeoUi.tr("gui.pvp_optimize.hud_editor.lyrics"), mx, my);

        int textX = chipBX + chipW + 10;
        int textMax = Math.max(0, doneX - 10 - textX);
        if (textMax > 60) {
            String tip = NeoUi.tr("gui.pvp_optimize.hud_editor.tip");
            NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, tip, textMax),
                    textX, barY + 12, NeoUi.TEXT_FAINT);
            String note = NeoUi.tr("gui.pvp_optimize.hud_editor.tip2");
            NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, note, textMax),
                    textX, barY + 25, NeoUi.TEXT_FAINT);
        }

        button(ctx, doneX, NeoUi.tr("gui.pvp_optimize.hud_editor.done"),
                NeoUi.ACCENT, NeoUi.WHITE, mx, my);
        button(ctx, resetX, NeoUi.tr("gui.pvp_optimize.hud_editor.reset"),
                0xFF3A2028, NeoUi.RED, mx, my);
    }

    private void chip(DrawContext ctx, int x, HudLayout.Id id, String label, int mx, int my) {
        int cy = barY + (barH - CHIP_H) / 2;
        boolean on = target == id;
        boolean hover = NeoUi.inside(mx, my, x, cy, x + chipW, cy + CHIP_H);
        NeoUi.rrect(ctx, x, cy, x + chipW, cy + CHIP_H, 7,
                on ? NeoUi.ACCENT : (hover ? NeoUi.FIELD_HOVER : NeoUi.FIELD));
        NeoUi.textCenter(ctx, textRenderer, NeoUi.clip(textRenderer, label, chipW - 8),
                x + chipW / 2, cy + 5, on ? NeoUi.WHITE : NeoUi.TEXT_DIM);
    }

    private void button(DrawContext ctx, int x, String label, int fill, int textColor, int mx, int my) {
        int by = barY + (barH - BTN_H) / 2;
        boolean hover = NeoUi.inside(mx, my, x, by, x + BTN_W, by + BTN_H);
        NeoUi.rrect(ctx, x, by, x + BTN_W, by + BTN_H, 7,
                hover ? NeoUi.mix(fill, NeoUi.WHITE, 0.15f) : fill);
        NeoUi.textCenter(ctx, textRenderer, NeoUi.clip(textRenderer, label, BTN_W - 10),
                x + BTN_W / 2, by + (BTN_H - 8) / 2, textColor);
    }

    /** 选中框：1px 主色描边 + 右下角缩放手柄 + 顶部名称与比例标签。 */
    private void drawSelection(DrawContext ctx, HudLayout.Rect r, HudLayout.Id id, PvPOptimizeConfig.Data d) {
        boolean on = target == id;
        int color = on ? NeoUi.ACCENT : 0xFF43506A;
        NeoUi.outline(ctx, r.x - 1, r.y - 1, r.x + r.w + 1, r.y + r.h + 1, 3, color);
        if (on) {
            NeoUi.rrect(ctx, r.x + r.w - 9, r.y + r.h - 9, r.x + r.w - 1, r.y + r.h - 1, 2, NeoUi.ACCENT);
        }

        String nameKey = id == HudLayout.Id.LYRICS
                ? "gui.pvp_optimize.hud_editor.lyrics"
                : "gui.pvp_optimize.hud_editor.potion";
        String label = NeoUi.tr(nameKey) + "  " + Math.round(HudLayout.scale(d, id) * 100) + "%";
        int lw = textRenderer.getWidth(label) + 12;
        int lx = NeoUi.clamp(r.x, 4, Math.max(4, width - lw - 4));
        int ly = Math.max(4, r.y - 15);
        NeoUi.rrect(ctx, lx, ly, lx + lw, ly + 12, 4, 0xE60A0D14);
        NeoUi.text(ctx, textRenderer, label, lx + 6, ly + 2, color);
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int by = barY + (barH - BTN_H) / 2;
        if (NeoUi.inside(mx, my, doneX, by, doneX + BTN_W, by + BTN_H)) {
            close();
            return true;
        }
        if (NeoUi.inside(mx, my, resetX, by, resetX + BTN_W, by + BTN_H)) {
            HudLayout.reset(target);
            return true;
        }
        int cy = barY + (barH - CHIP_H) / 2;
        if (NeoUi.inside(mx, my, chipAX, cy, chipAX + chipW, cy + CHIP_H)) {
            target = HudLayout.Id.POTION;
            return true;
        }
        if (NeoUi.inside(mx, my, chipBX, cy, chipBX + chipW, cy + CHIP_H)) {
            target = HudLayout.Id.LYRICS;
            return true;
        }

        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        // 先判选中的那个，让重叠时也能抓到它
        HudLayout.Id[] order = target == HudLayout.Id.LYRICS
                ? new HudLayout.Id[]{HudLayout.Id.LYRICS, HudLayout.Id.POTION}
                : new HudLayout.Id[]{HudLayout.Id.POTION, HudLayout.Id.LYRICS};
        for (HudLayout.Id id : order) {
            HudLayout.Rect r = rectOf(id, d);
            if (!r.contains(mx, my)) continue;
            target = id;
            if (button == 1) {
                HudLayout.reset(id);
                return true;
            }
            if (mx >= r.x + r.w - 12 && my >= r.y + r.h - 12) {
                resizing = true;
                resizeStartY = (int) my;
                resizeStartScale = Math.round(r.scale * 100);
            } else {
                dragging = true;
                dragDX = (int) mx - r.x;
                dragDY = (int) my - r.y;
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        if (dragging) {
            int[] size = contentSize(target, d);
            HudLayout.setPos(target, client, d, (int) mx - dragDX, (int) my - dragDY,
                    size[0], size[1]);
            return true;
        }
        if (resizing) {
            int pct = resizeStartScale + (int) ((my - resizeStartY) * 0.6);
            HudLayout.setScale(target, d, NeoUi.clamp(Math.round(pct / 5f) * 5, 50, 200));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private int[] contentSize(HudLayout.Id id, PvPOptimizeConfig.Data d) {
        if (id == HudLayout.Id.LYRICS) return LyricsHud.previewSize(client, d);
        return new int[]{HudLayout.POTION_PREVIEW_W, HudLayout.POTION_PREVIEW_H};
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging || resizing) {
            dragging = false;
            resizing = false;
            PvPOptimizeConfig.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (Math.abs(vertical) > 0.001) {
            PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
            int pct = Math.round(HudLayout.scale(d, target) * 100) + (vertical > 0 ? 5 : -5);
            HudLayout.setScale(target, d, NeoUi.clamp(pct, 50, 200));
            PvPOptimizeConfig.save();
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            target = target == HudLayout.Id.POTION ? HudLayout.Id.LYRICS : HudLayout.Id.POTION;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        PvPOptimizeConfig.save();
        super.removed();
    }

    @Override
    public void close() {
        PvPOptimizeConfig.save();
        if (client != null) client.setScreen(parent);
    }
}
