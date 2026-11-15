package com.pvp.optimize.gui;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.dango.DangoBridge;
import com.pvp.optimize.gui.Catalog.Entry;
import com.pvp.optimize.gui.Catalog.Tab;
import com.pvp.optimize.gui.neo.NeoUi;
import com.pvp.optimize.music.MusicLibrary;
import com.pvp.optimize.music.MusicPlayer;
import com.pvp.optimize.music.OnlineMusic;
import com.pvp.optimize.perf.AdaptivePerformance;
import com.pvp.optimize.perf.HudCost;
import com.pvp.optimize.perf.RuntimeGuard;
import com.pvp.optimize.util.CharInputBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 游戏内主控板（整体重写版）。
 *
 * <p>本类与之前的实现没有共用任何布局或绘制代码，改为「先算后画」：
 * {@link #layout()} 一次性把标题栏、侧栏、内容视口、播放条及其每一个子按钮的矩形算好存进字段，
 * 绘制与点击都只读这些字段。上一版「图层重叠」的根因正是绘制和命中判定各算一套坐标，
 * 加上侧栏在窗口圆角处铺方角矩形露台阶——这里用四角独立圆角与唯一坐标源彻底消除。
 *
 * <p>图标全部由 {@link NeoUi} 的矢量原语绘制（三角形/圆环/圆点），不依赖任何字体字形，
 * 因此换资源包、换字体都不会出现缺字方块。
 *
 * <p>功能与旧版一致：全部优化开关/滑块/枚举/颜色/预设、搜索、快捷键改键、
 * 在线搜歌与下载、本地歌单、HUD 布局编辑器、以及内置 DangoMusic 官方界面入口。
 */
public final class ControlPanelScreen extends Screen implements CharInputBridge.Target {

    public static final String TAB_OVERVIEW = "overview";
    public static final String TAB_MUSIC = "music";
    public static final String TAB_ONLINE = "online";

    /** 三档窗口尺寸（都是浮层，不铺满画面）。 */
    private static final int[] WIN_W = {600, 760, 900};
    private static final int[] WIN_H = {520, 600, 680};

    private static final int GAP = 8;

    /** 版本号缓存：只读一次 fabric.mod.json，避免每帧查元数据。 */
    private static String MOD_VERSION;

    /**
     * 当前 mod 版本，来源于 gradle.properties 的 {@code mod_version}
     * （Gradle 注入到 fabric.mod.json 的 {@code version} 字段）。
     *
     * <p>以前这个字符串是硬编码在语言文件里的（{@code "… · v1.0.6-v1"}），
     * 每次升版本都得同时改中英两份 lang，容易漏。现在统一从这里取。
     */
    private static String modVersion() {
        if (MOD_VERSION == null) {
            String v = "?";
            try {
                v = FabricLoader.getInstance().getModContainer("pvp_optimize")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("?");
            } catch (Throwable ignored) {
                // 元数据缺失（如某些开发环境）时退回 "?"，不影响界面
            }
            MOD_VERSION = v;
        }
        return MOD_VERSION;
    }

    public static void open(String tab) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new ControlPanelScreen(mc.currentScreen, tab));
    }

    private enum RowKind {
        SECTION, ENTRY, STATS, PRESETS, PLAYER_CARD, TRACK, KEY, TEXT,
        ONLINE_INPUT, ONLINE_ACTIONS, ONLINE_STATUS, ONLINE_RESULT, DANGO_CARD
    }

    private static final class Row {
        final RowKind kind;
        final int top, h, index;
        final String text;
        final Entry entry;

        Row(RowKind kind, int top, int h, String text, Entry entry, int index) {
            this.kind = kind;
            this.top = top;
            this.h = h;
            this.text = text;
            this.entry = entry;
            this.index = index;
        }
    }

    // ==================== 布局字段（唯一坐标源）====================
    private int panelX, panelY, panelW, panelH, titleBottom, sideW;
    // 标题栏
    private int logoX, logoY, logoSize, titleX;
    private int btnY2, btnH2, btnCloseX, btnSizeX, btnHudX, btnW2;
    // 侧栏
    private int sideX1, sideX2, sideTop, sideBottom;
    private int searchX1, searchY, searchX2, searchH;
    private int navX1, navY1, navX2, navY2, navItemH, navGroupH, navContentH, navMaxScroll;
    private int hudBtnX1, hudBtnY1, hudBtnX2, hudBtnY2;
    private int dangoBtnX1, dangoBtnY1, dangoBtnX2, dangoBtnY2;
    private int accountX1, accountY1, accountX2, accountY2;
    private boolean showAccount;
    // 内容
    private int cx1, cx2, cy1, cy2, ctrlX, ctrlW;
    // 播放条
    private int pbX1, pbX2, pbY1, pbY2;
    private int pbCoverX, pbCoverY, pbCoverS, pbInfoX, pbInfoW;
    private int pbPrevX, pbPlayX, pbNextX, pbLoopX, pbShuffleX, pbVolX, pbVolW, pbMusicX;
    private int pbCtrlY, pbCtrlH, pbProgX, pbProgY, pbProgW;
    private boolean showVolume, showLoop, richBar;
    // 响应式列数
    private int statCols, presetCols, onlineCols;

    // ==================== 状态 ====================
    private final Screen parent;
    private String tab;
    private String query = "";
    private boolean searchFocused;
    private int scroll, maxScroll, cursor, navScroll;

    private final List<Row> rows = new ArrayList<>();
    private List<Path> tracks = new ArrayList<>();
    private List<String> trackNames = new ArrayList<>();

    private Entry editEntry;
    private String editText = "";
    private boolean editColor;
    private Entry dragEntry;
    private int dragTrackX, dragTrackW;
    private boolean dragScrollbar, dragVolume, draggingWindow;
    private int dragWinStartX, dragWinStartY, dragWinStartOffX, dragWinStartOffY;
    private boolean dirty;
    private String hoverTip;

    private int keyCapture = -1;
    private boolean[] keyConflict = new boolean[Catalog.KEYS.length];

    private String onlineQuery = "";
    private String onlineUrl = "";
    private String lenoDirText = "";
    private int onlineFocus;

    // 指标
    private long lastFrameNs, lastMetricsNs;
    private double guiFps;
    private String badgeCache = "";
    private final String[] statCells = new String[12];
    private long onlineStamp;
    private String tipCacheKey = "";
    private List<String> tipCacheLines = new ArrayList<>();

    public ControlPanelScreen(Screen parent) {
        this(parent, TAB_OVERVIEW);
    }

    public ControlPanelScreen(Screen parent, String tab) {
        super(Text.translatable("gui.pvp_optimize.title"));
        this.parent = parent;
        this.tab = tab == null ? TAB_OVERVIEW : tab;
    }

    // ==================== 布局 ====================

    @Override
    protected void init() {
        CharInputBridge.install(client);
        NeoUi.clearCaches();
        java.util.Arrays.fill(statCells, "");
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        lenoDirText = d.lenoMusicDir == null ? "" : d.lenoMusicDir;
        onlineUrl = "";
        onlineQuery = "";
        layout();
        rebuild();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        NeoUi.clearCaches();
        super.resize(client, width, height);
        layout();
        rebuild();
    }

    /**
     * 唯一布局计算入口。所有矩形在这里算好；绘制与点击只读结果，绝不重复推导，
     * 因此不存在「画的位置」和「点的位置」错位，也不会有元素互相压盖。
     */
    private void layout() {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        int mode = Math.max(0, Math.min(2, d.guiWindowSize));

        panelW = Math.min(WIN_W[mode], width - 16);
        if (panelW < 360) panelW = width - 4;
        panelH = Math.min(WIN_H[mode], height - 16);
        if (panelH < 300) panelH = height - 4;
        panelX = NeoUi.clamp(8 + d.guiWindowOffsetX, 4, Math.max(4, width - panelW - 4));
        panelY = NeoUi.clamp((height - panelH) / 2 + d.guiWindowOffsetY, 4, Math.max(4, height - panelH - 4));

        titleBottom = panelY + NeoUi.TITLE_H;
        sideW = panelW < 640 ? 152 : 176;
        sideX1 = panelX;
        sideX2 = panelX + sideW;
        sideTop = titleBottom;
        sideBottom = panelY + panelH;

        // ---- 标题栏：从右往左排按钮，永不重叠 ----
        btnW2 = 26;
        btnH2 = 20;
        btnY2 = panelY + (NeoUi.TITLE_H - btnH2) / 2;
        int right = panelX + panelW - 12;
        btnCloseX = right - btnW2;
        btnSizeX = btnCloseX - 6 - btnW2;
        btnHudX = btnSizeX - 6 - btnW2;
        logoSize = 22;
        logoX = panelX + 14;
        logoY = panelY + (NeoUi.TITLE_H - logoSize) / 2;
        titleX = logoX + logoSize + 9;

        // ---- 侧栏：从上往下 搜索 → 导航(可滚动) → ... → HUD → Dango → 账号 ----
        searchH = 28;
        searchX1 = sideX1 + 12;
        searchX2 = sideX2 - 12;
        searchY = sideTop + 12;

        showAccount = panelH - NeoUi.TITLE_H >= 470;
        int bottom = sideBottom - 12;
        if (showAccount) {
            accountY2 = bottom;
            accountY1 = bottom - 42;
            accountX1 = sideX1 + 12;
            accountX2 = sideX2 - 12;
            bottom = accountY1 - 8;
        }
        dangoBtnY2 = bottom;
        dangoBtnY1 = bottom - 32;
        dangoBtnX1 = sideX1 + 12;
        dangoBtnX2 = sideX2 - 12;
        bottom = dangoBtnY1 - 6;
        hudBtnY2 = bottom;
        hudBtnY1 = bottom - 26;
        hudBtnX1 = sideX1 + 12;
        hudBtnX2 = sideX2 - 12;

        navX1 = sideX1 + 8;
        navX2 = sideX2 - 8;
        navY1 = searchY + searchH + 10;
        navY2 = Math.max(navY1 + 40, hudBtnY1 - 10);
        navItemH = 24;
        navGroupH = 16;
        int content = 0;
        String prev = null;
        for (Tab t : Catalog.TABS) {
            if (!t.group.equals(prev)) {
                content += navGroupH;
                prev = t.group;
            }
            content += navItemH + 2;
        }
        navContentH = content;
        navMaxScroll = Math.max(0, navContentH - (navY2 - navY1));
        navScroll = NeoUi.clamp(navScroll, 0, navMaxScroll);

        // ---- 内容视口 ----
        cx1 = sideX2 + 16;
        cx2 = panelX + panelW - 16;
        cy1 = titleBottom + 12;

        // ---- 播放条：贴内容区底部，与内容视口之间留 12px ----
        pbX1 = cx1;
        pbX2 = cx2;
        pbY2 = panelY + panelH - 14;
        pbY1 = pbY2 - NeoUi.PB_H;
        cy2 = pbY1 - 12;

        ctrlW = NeoUi.clamp((cx2 - cx1) / 3, 104, 176);
        ctrlX = cx2 - 10 - ctrlW;

        // 播放条内部：左侧封面+文字，右侧按钮组从右往左排。
        // 宽度不足时逐级降级（隐藏音量 → 隐藏循环/随机 → 只留播放键），保证任何宽度都不重叠。
        pbCoverY = pbY1 + 10;
        pbCtrlY = pbY1 + 10;
        pbCtrlH = 28;

        int pbRight = pbX2 - 12;
        int barW = pbX2 - pbX1;
        pbMusicX = pbRight - 32;
        int rx = pbMusicX - 10;

        richBar = barW >= 240;
        if (richBar) {
            showLoop = barW >= 430;
            if (showLoop) {
                pbShuffleX = rx - 24;
                pbLoopX = pbShuffleX - 28;
                rx = pbLoopX - 10;
            }
            pbNextX = rx - 28;
            pbPlayX = pbNextX - 4 - 34;
            pbPrevX = pbPlayX - 4 - 28;
            showVolume = barW >= 380;
            if (showVolume) {
                pbVolW = 56;
                pbVolX = pbPrevX - 12 - pbVolW;
            }
        } else {
            showLoop = false;
            showVolume = false;
            pbMusicX = pbRight - 28;
            pbPlayX = pbMusicX - 8 - 34;
            pbPrevX = pbPlayX;
            pbNextX = pbPlayX + 34;
        }

        pbCoverS = barW >= 260 ? 40 : 0;
        pbCoverX = pbX1 + 10;
        pbInfoX = pbCoverS > 0 ? pbCoverX + pbCoverS + 10 : pbX1 + 12;
        int controlsLeft = showVolume ? pbVolX - 8 : (richBar ? pbPrevX : pbPlayX);
        pbInfoW = Math.max(20, controlsLeft - pbInfoX - 8);

        pbProgX = pbX1 + 10;
        pbProgW = Math.max(20, barW - 20);
        pbProgY = pbY1 + 56;
    }

    // ==================== 内容构建 ====================

    private void rebuild() {
        rows.clear();
        cursor = 0;
        tracks = MusicLibrary.tracks();
        trackNames = new ArrayList<>(tracks.size());
        for (Path p : tracks) trackNames.add(MusicLibrary.displayTitle(p));
        computeKeyConflicts();
        onlineStamp = OnlineMusic.state().stamp;

        int availW = cx2 - cx1;
        statCols = availW >= 470 ? 4 : 2;
        presetCols = availW >= 320 ? 4 : 2;
        onlineCols = availW >= 400 ? 4 : 2;

        if (!query.trim().isEmpty()) {
            List<Entry> found = Catalog.search(query);
            section(NeoUi.tr("gui.pvp_optimize.search.results") + "  " + found.size());
            if (found.isEmpty()) addWrapped(NeoUi.tr("gui.pvp_optimize.search.empty"));
            else for (Entry e : found) addEntry(e);
        } else {
            switch (tab) {
                case "overview" -> buildOverview();
                case "music" -> buildMusic();
                case "online" -> buildOnline();
                case "keys" -> buildKeys();
                default -> buildCategory();
            }
        }
        cursor += 12;
        maxScroll = Math.max(0, cursor - (cy2 - cy1));
        scroll = NeoUi.clamp(scroll, 0, maxScroll);
    }

    private void buildOverview() {
        addRow(RowKind.DANGO_CARD, 88, null, null, 0);
        section(NeoUi.tr("gui.pvp_optimize.section.status"));
        addRow(RowKind.STATS, statCols == 4 ? 76 : 154, null, null, 0);
        section(NeoUi.tr("gui.pvp_optimize.section.quick"));
        String[] quick = {"particlesEnabled", "entityCullingEnabled", "redOverlayEnabled",
                "aggressiveEnabled", "adaptiveEnabled", "musicPlayerEnabled"};
        for (String id : quick) {
            Entry e = Catalog.BY_ID.get(id);
            if (e != null) addEntry(e);
        }
        section(NeoUi.tr("gui.pvp_optimize.section.preset"));
        addRow(RowKind.PRESETS, presetCols == 4 ? 38 : 74, null, null, 0);
        section(NeoUi.tr("gui.pvp_optimize.section.keybinds_edit"));
        for (int i = 0; i < Catalog.KEYS.length; i++) addRow(RowKind.KEY, 30, null, null, i);
        section(NeoUi.tr("gui.pvp_optimize.section.tips"));
        addWrapped(NeoUi.tr("gui.pvp_optimize.tip.1"));
        addWrapped(NeoUi.tr("gui.pvp_optimize.tip.2"));
        addWrapped(NeoUi.tr("gui.pvp_optimize.tip.3"));
        addWrapped(NeoUi.tr("gui.pvp_optimize.tip.rebind"));
    }

    private void buildMusic() {
        section(NeoUi.tr("gui.pvp_optimize.section.playing"));
        addRow(RowKind.PLAYER_CARD, 94, null, null, 0);
        section(NeoUi.tr("gui.pvp_optimize.section.music_settings"));
        for (Entry e : Catalog.forCategory("music")) addCatalogEntry(e);
        section(NeoUi.tr("gui.pvp_optimize.section.playlist") + "  " + tracks.size());
        if (tracks.isEmpty()) addWrapped(NeoUi.tr("gui.pvp_optimize.player.empty"));
        else for (int i = 0; i < tracks.size(); i++) addRow(RowKind.TRACK, 28, null, null, i);
    }

    private void buildKeys() {
        section(NeoUi.tr("gui.pvp_optimize.section.keybinds_edit"));
        for (int i = 0; i < Catalog.KEYS.length; i++) addRow(RowKind.KEY, 30, null, null, i);
        section(NeoUi.tr("gui.pvp_optimize.section.tips"));
        addWrapped(NeoUi.tr("gui.pvp_optimize.tip.rebind"));
        addWrapped(NeoUi.tr("gui.pvp_optimize.tip.keys"));
    }

    private void buildOnline() {
        section(NeoUi.tr("gui.pvp_optimize.online.search"));
        addRow(RowKind.ONLINE_INPUT, 32, null, null, 1);
        addRow(RowKind.ONLINE_ACTIONS, onlineCols == 4 ? 40 : 76, null, null, 0);
        addRow(RowKind.ONLINE_INPUT, 32, null, null, 2);
        addRow(RowKind.ONLINE_INPUT, 32, null, null, 3);
        addRow(RowKind.ONLINE_STATUS, 40, null, null, 0);
        List<OnlineMusic.Song> songs = OnlineMusic.state().songs;
        section(NeoUi.tr("gui.pvp_optimize.section.playlist") + "  " + songs.size());
        if (songs.isEmpty()) addWrapped(NeoUi.tr("gui.pvp_optimize.online.hint"));
        else for (int i = 0; i < songs.size(); i++) addRow(RowKind.ONLINE_RESULT, 28, null, null, i);
        addWrapped(NeoUi.tr("gui.pvp_optimize.online.disclaimer"));
    }

    private void buildCategory() {
        Tab t = Catalog.TAB_BY_ID.get(tab);
        section(t == null ? tab : t.title());
        for (Entry e : Catalog.forCategory(tab)) addCatalogEntry(e);
    }

    private void addCatalogEntry(Entry e) {
        if (e.kind == Catalog.Kind.LABEL) addWrapped(e.label());
        else addEntry(e);
    }

    private void computeKeyConflicts() {
        keyConflict = new boolean[Catalog.KEYS.length];
        for (int i = 0; i < Catalog.KEYS.length; i++) {
            if (Catalog.KEYS[i].isUnbound()) continue;
            String a = Catalog.KEYS[i].getBoundKeyTranslationKey();
            for (int j = i + 1; j < Catalog.KEYS.length; j++) {
                if (Catalog.KEYS[j].isUnbound()) continue;
                if (a.equals(Catalog.KEYS[j].getBoundKeyTranslationKey())) {
                    keyConflict[i] = true;
                    keyConflict[j] = true;
                }
            }
        }
    }

    private void section(String title) {
        addRow(RowKind.SECTION, 30, title, null, 0);
    }

    private void addEntry(Entry e) {
        addRow(RowKind.ENTRY, NeoUi.ROW_H, null, e, 0);
    }

    private void addRow(RowKind kind, int h, String text, Entry e, int index) {
        rows.add(new Row(kind, cursor, h, text, e, index));
        cursor += h;
    }

    private void addWrapped(String s) {
        if (s == null || s.isEmpty()) return;
        for (String line : wrap(s, Math.max(80, cx2 - cx1 - 12))) {
            addRow(RowKind.TEXT, 13, line, null, 0);
        }
    }

    private List<String> wrap(String s, int maxW) {
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            if (para.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < para.length(); i++) {
                String test = cur.toString() + para.charAt(i);
                if (textRenderer.getWidth(test) > maxW && cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                cur.append(para.charAt(i));
            }
            if (cur.length() > 0) out.add(cur.toString());
        }
        return out;
    }

    // ==================== 渲染 ====================

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        // 纯顶点绘制：三条横带近似竖向渐变。零纹理采样、零后处理 ——
        // 旧版在这里驱动原版 blur 的 6 个 pass（1080p 每帧上亿次采样），是界面卡顿的
        // 唯一来源，现已整体删除。现在主控板打开时每帧多出来的开销只有 3 个 quad。
        int mode = Math.max(0, Math.min(2, cfg.guiBackdrop));
        boolean noWorld = client == null || client.world == null;
        if (mode == 0 && !noWorld) return;   // 玩家显式选了「完全不遮」

        int a1 = noWorld ? 0x78 : (mode >= 2 ? 0x86 : 0x40);
        int a2 = noWorld ? 0x8E : (mode >= 2 ? 0x9E : 0x50);
        int a3 = noWorld ? 0x74 : (mode >= 2 ? 0x7E : 0x3C);
        if (mode == 0) { a1 = 0x5A; a2 = 0x66; a3 = 0x56; }

        int h1 = height / 3 + 1;
        int h2 = (height * 2) / 3 + 1;
        ctx.fill(0, 0, width, h1, a1 << 24);
        ctx.fill(0, h1, width, h2, a2 << 24);
        ctx.fill(0, h2, width, height, a3 << 24);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        if (lastFrameNs > 0) {
            double instant = 1_000_000_000.0 / Math.max(1L, now - lastFrameNs);
            guiFps = guiFps <= 0 ? instant : guiFps * 0.88 + instant * 0.12;
        }
        lastFrameNs = now;

        if (TAB_ONLINE.equals(tab) && query.trim().isEmpty()
                && OnlineMusic.state().stamp != onlineStamp) {
            onlineStamp = OnlineMusic.state().stamp;
            rebuild();
        }
        if (now - lastMetricsNs > 250_000_000L) {
            lastMetricsNs = now;
            updateMetrics();
        }

        renderBackground(ctx, mouseX, mouseY, delta);

        final int R = NeoUi.R_WINDOW;
        NeoUi.shadow(ctx, panelX, panelY, panelX + panelW, panelY + panelH, R);
        // 窗口主体：四角全圆
        NeoUi.rrect(ctx, panelX, panelY, panelX + panelW, panelY + panelH, R, NeoUi.WINDOW);
        // 标题栏：只圆上两角
        NeoUi.rrect4(ctx, panelX, panelY, panelX + panelW, titleBottom, R, R, 0, 0, NeoUi.TITLEBAR);
        // 侧栏：只圆左下角，右缘用 1px 分隔线，绝不覆盖窗口圆角
        NeoUi.rrect4(ctx, panelX, titleBottom, sideX2, sideBottom, 0, 0, 0, R, NeoUi.SIDEBAR);
        NeoUi.vline(ctx, sideX2, titleBottom, sideBottom - R, NeoUi.BORDER);
        NeoUi.hline(ctx, panelX + R, panelX + panelW - R, titleBottom - 1, NeoUi.BORDER);
        NeoUi.outline(ctx, panelX, panelY, panelX + panelW, panelY + panelH, R, NeoUi.WINDOW_EDGE);

        drawTitleBar(ctx, mouseX, mouseY);
        drawSidebar(ctx, mouseX, mouseY);

        hoverTip = null;
        ctx.enableScissor(cx1, cy1, cx2, cy2);
        for (Row r : rows) {
            int y = cy1 + r.top - scroll;
            if (y + r.h <= cy1 || y >= cy2) continue;
            drawRow(ctx, r, y, mouseX, mouseY);
        }
        ctx.disableScissor();
        NeoUi.scrollbar(ctx, cx2 + 4, cy1, cy2, cy2 - cy1, Math.max(cy2 - cy1, cursor),
                scroll, maxScroll, isOverScrollbar(mouseX, mouseY));

        drawPlayerBar(ctx, mouseX, mouseY);
        if (hoverTip != null && !hoverTip.isEmpty()) drawTooltip(ctx, hoverTip, mouseX, mouseY);
    }

    private boolean isOverScrollbar(int mx, int my) {
        return mx >= cx2 && mx <= cx2 + 10 && my >= cy1 && my <= cy2;
    }

    private void drawTitleBar(DrawContext ctx, int mx, int my) {
        NeoUi.rrect(ctx, logoX, logoY, logoX + logoSize, logoY + logoSize, 7, NeoUi.ACCENT);
        NeoUi.textCenter(ctx, textRenderer, "P", logoX + logoSize / 2, logoY + 7, NeoUi.WHITE);

        String title = Catalog.TAB_BY_ID.containsKey(tab)
                ? Catalog.TAB_BY_ID.get(tab).title() : NeoUi.tr("gui.pvp_optimize.title");
        int badgeW = textRenderer.getWidth(badgeCache) + 16;
        int badgeX2 = btnHudX - 12;
        int badgeX1 = badgeX2 - badgeW;
        int textMax = Math.max(30, badgeX1 - 10 - titleX);
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, title, textMax),
                titleX, logoY + 7, NeoUi.TEXT);
        if (textMax > textRenderer.getWidth(title) + 12) {
            NeoUi.iconChevronDown(ctx, titleX + textRenderer.getWidth(title) + 8, logoY + 10, 8,
                    NeoUi.TEXT_FAINT);
        }

        if (badgeX1 > titleX + 90) {
            NeoUi.rrect(ctx, badgeX1, logoY + 4, badgeX2, logoY + 18, 7, NeoUi.FIELD);
            NeoUi.textCenter(ctx, textRenderer, badgeCache, (badgeX1 + badgeX2) / 2, logoY + 7,
                    NeoUi.TEXT_FAINT);
        }

        // 三个图标按钮：HUD 布局 / 窗口尺寸 / 关闭
        boolean hudHover = NeoUi.inside(mx, my, btnHudX, btnY2, btnHudX + btnW2, btnY2 + btnH2);
        titleButton(ctx, btnHudX, hudHover);
        NeoUi.iconGrid(ctx, btnHudX + btnW2 / 2, btnY2 + btnH2 / 2, hudHover ? NeoUi.TEXT : NeoUi.TEXT_DIM);

        boolean sizeHover = NeoUi.inside(mx, my, btnSizeX, btnY2, btnSizeX + btnW2, btnY2 + btnH2);
        titleButton(ctx, btnSizeX, sizeHover);
        int mode = PvPOptimizeConfig.get().guiWindowSize;
        String sizeMark = mode == 0 ? "S" : mode == 1 ? "M" : "L";
        NeoUi.textCenter(ctx, textRenderer, sizeMark, btnSizeX + btnW2 / 2, btnY2 + 6,
                sizeHover ? NeoUi.TEXT : NeoUi.TEXT_DIM);

        boolean closeHover = NeoUi.inside(mx, my, btnCloseX, btnY2, btnCloseX + btnW2, btnY2 + btnH2);
        NeoUi.rrect(ctx, btnCloseX, btnY2, btnCloseX + btnW2, btnY2 + btnH2, 6,
                closeHover ? 0xFFE24A5A : NeoUi.FIELD);
        NeoUi.iconClose(ctx, btnCloseX + btnW2 / 2, btnY2 + btnH2 / 2, 8,
                closeHover ? NeoUi.WHITE : NeoUi.TEXT_DIM);
    }

    private void titleButton(DrawContext ctx, int x, boolean hover) {
        NeoUi.rrect(ctx, x, btnY2, x + btnW2, btnY2 + btnH2, 6,
                hover ? NeoUi.CARD_HOVER : NeoUi.FIELD);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my) {
        drawSearch(ctx, mx, my);
        drawNav(ctx, mx, my);
        drawSidebarActions(ctx, mx, my);
    }

    private void drawSearch(DrawContext ctx, int mx, int my) {
        boolean hover = NeoUi.inside(mx, my, searchX1, searchY, searchX2, searchY + searchH);
        NeoUi.rrect(ctx, searchX1, searchY, searchX2, searchY + searchH, 7,
                searchFocused ? NeoUi.FIELD_HOVER : NeoUi.FIELD);
        if (searchFocused) {
            NeoUi.outline(ctx, searchX1, searchY, searchX2, searchY + searchH, 7, NeoUi.ACCENT);
        }
        NeoUi.iconSearch(ctx, searchX1 + 13, searchY + 14, 11,
                searchFocused ? NeoUi.ACCENT : NeoUi.TEXT_FAINT);

        String shown = query.isEmpty() ? NeoUi.tr("gui.pvp_optimize.search.hint") : query;
        int maxW = Math.max(16, searchX2 - searchX1 - (query.isEmpty() ? 44 : 50));
        String clipped = NeoUi.clip(textRenderer, shown, maxW);
        NeoUi.text(ctx, textRenderer, clipped, searchX1 + 26, searchY + 10,
                query.isEmpty() ? NeoUi.TEXT_FAINT : NeoUi.TEXT);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = searchX1 + 26 + textRenderer.getWidth(clipped);
            ctx.fill(caret, searchY + 8, caret + 1, searchY + 20, NeoUi.ACCENT);
        }
        if (!query.isEmpty()) {
            boolean xHover = NeoUi.inside(mx, my, searchX2 - 24, searchY, searchX2, searchY + searchH);
            NeoUi.iconClose(ctx, searchX2 - 13, searchY + 14, 6,
                    xHover ? NeoUi.TEXT : NeoUi.TEXT_FAINT);
        }
    }

    private void drawNav(DrawContext ctx, int mx, int my) {
        ctx.enableScissor(navX1, navY1, navX2, navY2);
        int y = navY1 - navScroll;
        String prev = null;
        for (Tab t : Catalog.TABS) {
            if (!t.group.equals(prev)) {
                if (y + navGroupH > navY1 && y < navY2) {
                    NeoUi.text(ctx, textRenderer,
                            NeoUi.tr("gui.pvp_optimize.group." + t.group).toUpperCase(Locale.ROOT),
                            navX1 + 10, y + 6, NeoUi.TEXT_FAINT);
                }
                y += navGroupH;
                prev = t.group;
            }
            boolean sel = t.id.equals(tab) && query.trim().isEmpty();
            boolean hover = NeoUi.inside(mx, my, navX1, y, navX2, y + navItemH)
                    && my >= navY1 && my < navY2;
            if (sel) {
                NeoUi.rrect(ctx, navX1, y, navX2, y + navItemH, 7, NeoUi.CARD_HOVER);
                NeoUi.rrect(ctx, navX1 + 2, y + 5, navX1 + 4, y + navItemH - 5, 1, NeoUi.ACCENT);
            } else if (hover) {
                NeoUi.rrect(ctx, navX1, y, navX2, y + navItemH, 7, 0x14FFFFFF);
            }
            int dotColor = sel ? NeoUi.ACCENT : (hover ? NeoUi.TEXT_DIM : NeoUi.TEXT_FAINT);
            NeoUi.rrect(ctx, navX1 + 14, y + navItemH / 2 - 4, navX1 + 22, y + navItemH / 2 + 4, 2,
                    sel ? NeoUi.ACCENT : NeoUi.fade(t.dot, hover ? 0.85f : 0.45f));
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, t.title(), navX2 - navX1 - 44),
                    navX1 + 28, y + (navItemH - 8) / 2,
                    sel ? NeoUi.TEXT : (hover ? NeoUi.TEXT : NeoUi.TEXT_DIM));
            y += navItemH + 2;
        }
        ctx.disableScissor();
        if (navMaxScroll > 0) {
            NeoUi.scrollbar(ctx, navX2 - 1, navY1, navY2, navY2 - navY1, navContentH,
                    navScroll, navMaxScroll, false);
        }
    }

    private void drawSidebarActions(DrawContext ctx, int mx, int my) {
        // HUD 布局编辑器
        boolean hudHover = NeoUi.inside(mx, my, hudBtnX1, hudBtnY1, hudBtnX2, hudBtnY2);
        NeoUi.rrect(ctx, hudBtnX1, hudBtnY1, hudBtnX2, hudBtnY2, 7,
                hudHover ? NeoUi.CARD_HOVER : NeoUi.FIELD);
        NeoUi.iconGrid(ctx, hudBtnX1 + 18, (hudBtnY1 + hudBtnY2) / 2,
                hudHover ? NeoUi.TEXT : NeoUi.TEXT_DIM);
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.sidebar.settings"),
                        hudBtnX2 - hudBtnX1 - 40),
                hudBtnX1 + 32, (hudBtnY1 + hudBtnY2) / 2 - 4,
                hudHover ? NeoUi.TEXT : NeoUi.TEXT_DIM);

        // DangoMusic 官方界面入口（侧栏最显眼的主色按钮）
        boolean dHover = NeoUi.inside(mx, my, dangoBtnX1, dangoBtnY1, dangoBtnX2, dangoBtnY2);
        NeoUi.rrect(ctx, dangoBtnX1, dangoBtnY1, dangoBtnX2, dangoBtnY2, 8,
                dHover ? NeoUi.mix(NeoUi.ACCENT, NeoUi.WHITE, 0.15f) : NeoUi.ACCENT);
        NeoUi.iconNote(ctx, dangoBtnX1 + 19, (dangoBtnY1 + dangoBtnY2) / 2, 12, NeoUi.WHITE);
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.dango.open"),
                        dangoBtnX2 - dangoBtnX1 - 46),
                dangoBtnX1 + 34, (dangoBtnY1 + dangoBtnY2) / 2 - 4, NeoUi.WHITE);
        if (dangoBtnX2 - dangoBtnX1 > 120) {
            NeoUi.iconChevronRight(ctx, dangoBtnX2 - 15, (dangoBtnY1 + dangoBtnY2) / 2, 6, NeoUi.WHITE);
        }

        if (showAccount) {
            boolean aHover = NeoUi.inside(mx, my, accountX1, accountY1, accountX2, accountY2);
            NeoUi.rrect(ctx, accountX1, accountY1, accountX2, accountY2, 8,
                    aHover ? NeoUi.CARD_HOVER : 0x00000000);
            NeoUi.disc(ctx, accountX1 + 20, accountY1 + 21, 12, NeoUi.mix(NeoUi.ACCENT, NeoUi.TITLEBAR, 0.55f));
            NeoUi.iconNote(ctx, accountX1 + 20, accountY1 + 21, 10, NeoUi.WHITE);
            String player = client != null && client.player != null
                    ? client.player.getName().getString() : "Player";
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, player, accountX2 - accountX1 - 46),
                    accountX1 + 38, accountY1 + 11, NeoUi.TEXT);
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.sidebar.foot") + " · v" + modVersion(),
                            accountX2 - accountX1 - 46),
                    accountX1 + 38, accountY1 + 24, NeoUi.TEXT_FAINT);
            NeoUi.iconChevronRight(ctx, accountX2 - 12, accountY1 + 21, 8, NeoUi.TEXT_FAINT);
        }
    }

    private void drawRow(DrawContext ctx, Row r, int y, int mx, int my) {
        switch (r.kind) {
            case SECTION -> {
                NeoUi.text(ctx, textRenderer, r.text, cx1 + 10, y + 12, NeoUi.TEXT_DIM);
                NeoUi.hline(ctx, cx1 + 10 + textRenderer.getWidth(r.text) + 8, cx2, y + 16, NeoUi.DIVIDER);
            }
            case TEXT -> NeoUi.text(ctx, textRenderer, r.text, cx1 + 4, y + 2, NeoUi.TEXT_FAINT);
            case STATS -> drawStats(ctx, y);
            case PRESETS -> drawPresets(ctx, y, mx, my);
            case PLAYER_CARD -> drawPlayerCard(ctx, y, mx, my);
            case TRACK -> drawTrack(ctx, r, y, mx, my);
            case KEY -> drawKeyRow(ctx, r, y, mx, my);
            case ENTRY -> drawEntry(ctx, r.entry, y, mx, my);
            case ONLINE_INPUT -> drawOnlineInput(ctx, r, y, mx, my);
            case ONLINE_ACTIONS -> drawOnlineActions(ctx, y, mx, my);
            case ONLINE_STATUS -> drawOnlineStatus(ctx, y);
            case ONLINE_RESULT -> drawOnlineResult(ctx, r, y, mx, my);
            case DANGO_CARD -> drawDangoCard(ctx, y, mx, my);
        }
    }

    // ---------- 行内几何（draw 与 click 共用，避免错位）----------

    private int rowTop(int y) {
        return y + 3;
    }

    private int rowBot(int y) {
        return y + NeoUi.ROW_H - 3;
    }

    private int toggleX() {
        return cx2 - 10 - NeoUi.SWITCH_W;
    }

    private int toggleY(int y) {
        return rowTop(y) + (30 - NeoUi.SWITCH_H) / 2;
    }

    private int comboY(int y) {
        return rowTop(y) + 6;
    }

    private int comboH() {
        return 18;
    }

    /** {trackX, trackY, trackW, boxX, boxW} */
    private int[] sliderGeom(int y, String value) {
        int boxW = Math.max(38, textRenderer.getWidth(value) + 14);
        int boxX = cx2 - 10 - boxW;
        int trackX = ctrlX;
        int trackW = Math.max(28, boxX - 10 - trackX);
        return new int[]{trackX, rowTop(y) + 6, trackW, boxX, boxW};
    }

    private void drawEntry(DrawContext ctx, Entry e, int y, int mx, int my) {
        int top = rowTop(y), bot = rowBot(y);
        boolean hover = NeoUi.inside(mx, my, cx1, top, cx2, bot);
        NeoUi.rrect(ctx, cx1, top, cx2, bot, NeoUi.R_CARD,
                hover ? NeoUi.CARD_HOVER : NeoUi.CARD);
        int labelMax = Math.max(40, ctrlX - cx1 - 24);
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, e.label(), labelMax),
                cx1 + 12, top + (bot - top - 8) / 2, NeoUi.TEXT);
        if (hover && !e.tip().isEmpty() && mx < ctrlX - 6) hoverTip = e.tip();
        drawControl(ctx, e, y, mx, my);
    }

    private void drawControl(DrawContext ctx, Entry e, int y, int mx, int my) {
        if (e == editEntry) {
            NeoUi.rrect(ctx, ctrlX, comboY(y), cx2 - 10, comboY(y) + comboH(), NeoUi.R_FIELD, NeoUi.FIELD);
            NeoUi.outline(ctx, ctrlX, comboY(y), cx2 - 10, comboY(y) + comboH(), NeoUi.R_FIELD, NeoUi.ACCENT);
            NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, editText, ctrlW - 12),
                    ctrlX + 7, comboY(y) + 5, NeoUi.TEXT);
            return;
        }
        switch (e.kind) {
            case TOGGLE -> {
                boolean on = e.boolGet.getAsBoolean();
                int tx = toggleX(), ty = toggleY(y);
                boolean hover = NeoUi.inside(mx, my, tx - 40, ty - 4, tx + NeoUi.SWITCH_W, ty + NeoUi.SWITCH_H + 4);
                NeoUi.toggle(ctx, tx, ty, NeoUi.SWITCH_W, NeoUi.SWITCH_H, on, hover);
                NeoUi.textRight(ctx, textRenderer, e.valueText(), tx - 9,
                        ty + (NeoUi.SWITCH_H - 8) / 2, on ? NeoUi.TEXT_DIM : NeoUi.TEXT_FAINT);
            }
            case SLIDER -> {
                int[] g = sliderGeom(y, e.valueText());
                boolean hover = NeoUi.inside(mx, my, g[0] - 4, g[1] - 6, g[3] + g[4] + 4, g[1] + 14);
                NeoUi.slider(ctx, textRenderer, g[0], g[1], g[2], e.fraction(), e.valueText(), hover);
            }
            case CYCLE -> {
                boolean hover = NeoUi.inside(mx, my, ctrlX, comboY(y), cx2 - 10, comboY(y) + comboH());
                NeoUi.combo(ctx, textRenderer, ctrlX, comboY(y), ctrlW, comboH(), e.valueText(), hover);
                if (e.optionCount() > 1) {
                    NeoUi.iconChevronDown(ctx, ctrlX + 3, comboY(y) + comboH() / 2, 5, NeoUi.TEXT_FAINT);
                }
            }
            case COLOR -> {
                int swY = rowTop(y) + 5;
                NeoUi.rrect(ctx, ctrlX, swY, ctrlX + 18, swY + 18, 4,
                        0xFF000000 | (e.colorGet.getAsInt() & 0x00FFFFFF));
                NeoUi.outline(ctx, ctrlX, swY, ctrlX + 18, swY + 18, 4, 0x33FFFFFF);
                int boxX = ctrlX + 24;
                int boxW = cx2 - 10 - boxX;
                boolean hover = NeoUi.inside(mx, my, boxX, swY, boxX + boxW, swY + 18);
                NeoUi.rrect(ctx, boxX, swY, boxX + boxW, swY + 18, NeoUi.R_FIELD,
                        hover ? NeoUi.FIELD_HOVER : NeoUi.FIELD);
                NeoUi.textCenter(ctx, textRenderer, "0x" + e.valueText(),
                        boxX + boxW / 2, swY + 5, hover ? NeoUi.TEXT : NeoUi.TEXT_DIM);
            }
            case BUTTON -> {
                int bw = 74;
                int bx = cx2 - 10 - bw;
                int by = rowTop(y) + 6;
                boolean hover = NeoUi.inside(mx, my, bx, by, bx + bw, by + 18);
                NeoUi.rrect(ctx, bx, by, bx + bw, by + 18, NeoUi.R_FIELD,
                        hover ? NeoUi.ACCENT : NeoUi.FIELD);
                NeoUi.textCenter(ctx, textRenderer, NeoUi.tr("gui.pvp_optimize.action.run"),
                        bx + bw / 2, by + 5, hover ? NeoUi.WHITE : NeoUi.TEXT_DIM);
            }
            case LABEL -> {
            }
        }
    }

    private void drawStats(DrawContext ctx, int y) {
        int cols = statCols;
        int cw = (cx2 - cx1 - GAP * (cols - 1)) / cols;
        int ch = 70;
        for (int i = 0; i < 4; i++) {
            int c = i % cols, row = i / cols;
            int x = cx1 + c * (cw + GAP);
            int yy = y + 3 + row * (ch + GAP);
            NeoUi.rrect(ctx, x, yy, x + cw, yy + ch, NeoUi.R_CARD, NeoUi.CARD);
            drawStatCard(ctx, x, yy, cw, i);
        }
    }

    private void drawStatCard(DrawContext ctx, int x, int yy, int cw, int i) {
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, statCells[i * 3], cw - 20),
                x + 10, yy + 9, NeoUi.TEXT_FAINT);
        int color = (i == 1 && AdaptivePerformance.level() == 2) ? NeoUi.AMBER : NeoUi.TEXT;
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, statCells[i * 3 + 1], cw - 20),
                x + 10, yy + 24, color);
        NeoUi.hline(ctx, x + 10, x + cw - 10, yy + 42, NeoUi.DIVIDER);
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, statCells[i * 3 + 2], cw - 20),
                x + 10, yy + 50, NeoUi.TEXT_DIM);
    }

    private void drawPresets(DrawContext ctx, int y, int mx, int my) {
        String[] labels = {
                NeoUi.tr("gui.pvp_optimize.preset.performance"),
                NeoUi.tr("gui.pvp_optimize.preset.balanced"),
                NeoUi.tr("gui.pvp_optimize.preset.quality"),
                NeoUi.tr("gui.pvp_optimize.preset.reset")
        };
        int cols = presetCols;
        int bw = (cx2 - cx1 - GAP * (cols - 1)) / cols;
        for (int i = 0; i < 4; i++) {
            int c = i % cols, row = i / cols;
            int x = cx1 + c * (bw + GAP);
            int yy = y + 3 + row * 36;
            boolean hover = NeoUi.inside(mx, my, x, yy, x + bw, yy + 30);
            int fill = i == 0 ? NeoUi.ACCENT : i == 3 ? 0xFF3A2028 : NeoUi.CARD;
            if (hover) fill = NeoUi.mix(fill, NeoUi.WHITE, 0.12f);
            NeoUi.rrect(ctx, x, yy, x + bw, yy + 30, 7, fill);
            NeoUi.textCenter(ctx, textRenderer, NeoUi.clip(textRenderer, labels[i], bw - 10),
                    x + bw / 2, yy + 11,
                    i == 0 ? NeoUi.WHITE : (i == 3 ? NeoUi.RED : (hover ? NeoUi.TEXT : NeoUi.TEXT_DIM)));
        }
    }

    private void drawDangoCard(DrawContext ctx, int y, int mx, int my) {
        int top = y + 3, bot = y + 81;
        boolean hover = NeoUi.inside(mx, my, cx1, top, cx2, bot);
        NeoUi.rrect(ctx, cx1, top, cx2, bot, NeoUi.R_CARD,
                hover ? NeoUi.CARD_HOVER : NeoUi.CARD);
        int coverS = (cx2 - cx1) >= 300 ? 54 : 40;
        NeoUi.cover(ctx, cx1 + 12, top + (bot - top - coverS) / 2, coverS, coverS, 8, 1, NeoUi.WHITE);

        int bw = 92;
        int bx = cx2 - 12 - bw;
        int tx = cx1 + 12 + coverS + 12;
        int maxW = Math.max(0, bx - tx - 12);
        if (maxW > 24) {
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.dango.title"), maxW),
                    tx, top + 14, NeoUi.TEXT);
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.dango.desc"), maxW),
                    tx, top + 30, NeoUi.TEXT_DIM);
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.dango.status") + " "
                            + DangoBridge.status(), maxW),
                    tx, top + 47, DangoBridge.isReady() ? NeoUi.GREEN : NeoUi.TEXT_FAINT);
            // 每帧 HUD 实测耗时（灵动岛 / 歌词 / 药水各自的毫秒数），判断掉帧归谁
            NeoUi.text(ctx, textRenderer,
                    NeoUi.clip(textRenderer, "HUD " + HudCost.summary(), maxW),
                    tx, top + 62, NeoUi.TEXT_FAINT);
        }

        boolean bh = NeoUi.inside(mx, my, bx, top + 24, bx + bw, top + 54);
        NeoUi.rrect(ctx, bx, top + 24, bx + bw, top + 54, 8,
                bh ? NeoUi.mix(NeoUi.ACCENT, NeoUi.WHITE, 0.15f) : NeoUi.ACCENT);
        NeoUi.iconNote(ctx, bx + 18, top + 39, 11, NeoUi.WHITE);
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, NeoUi.tr("gui.pvp_optimize.dango.open"), bw - 32),
                bx + 30, top + 35, NeoUi.WHITE);
    }

    private void drawPlayerCard(DrawContext ctx, int y, int mx, int my) {
        int top = y + 3, bot = y + 91;
        NeoUi.rrect(ctx, cx1, top, cx2, bot, NeoUi.R_CARD, NeoUi.CARD);
        NeoUi.cover(ctx, cx1 + 12, top + 12, 66, 66, 8, 11, NeoUi.WHITE);

        int tx = cx1 + 90;
        int maxW = Math.max(60, cx2 - 12 - tx);
        String name = MusicPlayer.status() == MusicPlayer.Status.STOPPED
                ? NeoUi.tr("gui.pvp_optimize.player.no_track") : MusicPlayer.trackName();
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, name, maxW), tx, top + 14, NeoUi.TEXT);

        String status = switch (MusicPlayer.status()) {
            case PLAYING -> NeoUi.tr("gui.pvp_optimize.player.playing");
            case PAUSED -> NeoUi.tr("gui.pvp_optimize.player.paused");
            default -> NeoUi.tr("gui.pvp_optimize.player.stopped");
        };
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, status + "   " + MusicLibrary.formatTime(MusicPlayer.position())
                        + " / " + MusicLibrary.formatTime(MusicPlayer.duration()), maxW),
                tx, top + 30, NeoUi.TEXT_DIM);

        String err = MusicPlayer.error();
        if (err != null) {
            NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, err, maxW), tx, top + 46, NeoUi.RED);
        } else {
            MusicLibrary.Lrc lrc = MusicPlayer.lyrics();
            int line = lrc.lineAt(MusicPlayer.position());
            String cur = line >= 0 ? lrc.textAt(line) : "";
            String nxt = line >= 0 ? lrc.textAt(line + 1) : "";
            NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, cur, maxW), tx, top + 46, NeoUi.TEXT_DIM);
            NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, nxt, maxW), tx, top + 60, NeoUi.TEXT_FAINT);
        }

        double dur = MusicPlayer.duration();
        NeoUi.progress(ctx, tx, top + 78, maxW, 3,
                dur > 0 ? MusicPlayer.position() / dur : 0, NeoUi.RED);
    }

    private void drawTrack(DrawContext ctx, Row r, int y, int mx, int my) {
        boolean hover = NeoUi.inside(mx, my, cx1, y, cx2, y + 26);
        boolean cur = r.index == MusicLibrary.currentIndex();
        if (cur || hover) {
            NeoUi.rrect(ctx, cx1, y, cx2, y + 26, 6, cur ? NeoUi.SELECTION : NeoUi.CARD);
            if (cur) NeoUi.vline(ctx, cx1 + 1, y + 4, y + 22, NeoUi.ACCENT);
        }
        NeoUi.text(ctx, textRenderer, String.format(Locale.ROOT, "%02d", r.index + 1),
                cx1 + 10, y + 9, NeoUi.TEXT_FAINT);
        NeoUi.cover(ctx, cx1 + 28, y + 5, 16, 16, 3, r.index * 7 + 3, 0x00FFFFFF);
        String cached = r.index < trackNames.size() ? trackNames.get(r.index)
                : MusicLibrary.displayTitle(tracks.get(r.index));
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, cached, cx2 - cx1 - 78),
                cx1 + 50, y + 9, cur ? NeoUi.TEXT : (hover ? NeoUi.TEXT : NeoUi.TEXT_DIM));
        if (cur && MusicPlayer.isPlaying()) {
            NeoUi.iconPlay(ctx, cx2 - 16, y + 13, 9, NeoUi.ACCENT);
        }
    }

    private void drawKeyRow(DrawContext ctx, Row r, int y, int mx, int my) {
        KeyBinding key = Catalog.KEYS[r.index];
        int top = y + 2, bot = y + 28;
        NeoUi.rrect(ctx, cx1, top, cx2, bot, NeoUi.R_CARD, NeoUi.CARD);
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, NeoUi.tr(key.getTranslationKey()), ctrlX - cx1 - 26),
                cx1 + 12, top + 8, NeoUi.TEXT_DIM);

        boolean capturing = keyCapture == r.index;
        boolean conflict = r.index < keyConflict.length && keyConflict[r.index];
        String bound = capturing ? NeoUi.tr("gui.pvp_optimize.key.capturing")
                : (key.isUnbound() ? NeoUi.tr("gui.pvp_optimize.key.unbound")
                        : key.getBoundKeyLocalizedText().getString());
        int bx = ctrlX, bw = ctrlW;
        boolean hover = NeoUi.inside(mx, my, bx, top + 2, bx + bw, bot - 2);
        int fill = capturing ? NeoUi.ACCENT : conflict ? 0xFF3A2A18 : (hover ? NeoUi.FIELD_HOVER : NeoUi.FIELD);
        NeoUi.rrect(ctx, bx, top + 2, bx + bw, bot - 2, NeoUi.R_FIELD, fill);
        int color = capturing ? NeoUi.WHITE : conflict ? NeoUi.AMBER : (hover ? NeoUi.TEXT : NeoUi.TEXT_DIM);
        NeoUi.textCenter(ctx, textRenderer, NeoUi.clip(textRenderer, bound, bw - 10),
                bx + bw / 2, top + 7, color);
    }

    // ---------- 在线音乐 ----------

    private static final String[] ONLINE_FIELDS = {
            "gui.pvp_optimize.online.keyword",
            "gui.pvp_optimize.online.url",
            "gui.pvp_optimize.online.dir"
    };
    private static final String[] ONLINE_ACTIONS = {
            "gui.pvp_optimize.online.search_btn",
            "gui.pvp_optimize.online.url_btn",
            "gui.pvp_optimize.online.dir_btn",
            "gui.pvp_optimize.online.cancel_btn"
    };

    private String onlineValue(int field) {
        return field == 1 ? onlineQuery : field == 2 ? onlineUrl : lenoDirText;
    }

    private void setOnlineValue(int field, String s) {
        if (field == 1) onlineQuery = s;
        else if (field == 2) onlineUrl = s;
        else lenoDirText = s;
    }

    private int onlineBtnW() {
        return (cx2 - cx1 - GAP * (onlineCols - 1)) / onlineCols;
    }

    private int onlineBtnX(int i) {
        return cx1 + (i % onlineCols) * (onlineBtnW() + GAP);
    }

    private int onlineBtnY(int y, int i) {
        return y + 3 + (i / onlineCols) * 36;
    }

    private void drawOnlineInput(DrawContext ctx, Row r, int y, int mx, int my) {
        int field = r.index;
        boolean focused = onlineFocus == field;
        int top = y + 2, bot = y + 30;
        NeoUi.rrect(ctx, cx1, top, cx2, bot, NeoUi.R_FIELD,
                focused ? NeoUi.FIELD_HOVER : NeoUi.CARD);
        if (focused) NeoUi.outline(ctx, cx1, top, cx2, bot, NeoUi.R_FIELD, NeoUi.ACCENT);
        String label = NeoUi.tr(ONLINE_FIELDS[field - 1]);
        int labelW = textRenderer.getWidth(label) + 14;
        NeoUi.text(ctx, textRenderer, label, cx1 + 10, top + 8, NeoUi.ACCENT);
        String value = onlineValue(field);
        String shown = value.isEmpty() ? NeoUi.tr("gui.pvp_optimize.online.input_hint") : value;
        int valueX = cx1 + labelW + 6;
        int avail = Math.max(16, cx2 - 10 - valueX);
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, shown, avail), valueX, top + 8,
                value.isEmpty() ? NeoUi.TEXT_FAINT : NeoUi.TEXT);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = valueX + Math.min(avail, textRenderer.getWidth(value));
            ctx.fill(caret, top + 6, caret + 1, top + 18, NeoUi.ACCENT);
        }
    }

    private void drawOnlineActions(DrawContext ctx, int y, int mx, int my) {
        int bw = onlineBtnW();
        for (int i = 0; i < 4; i++) {
            int x = onlineBtnX(i), yy = onlineBtnY(y, i);
            boolean hover = NeoUi.inside(mx, my, x, yy, x + bw, yy + 30);
            int fill = i == 0 ? NeoUi.ACCENT : i == 3 ? 0xFF3A2028 : NeoUi.CARD;
            if (hover) fill = NeoUi.mix(fill, NeoUi.WHITE, 0.12f);
            NeoUi.rrect(ctx, x, yy, x + bw, yy + 30, 7, fill);
            NeoUi.textCenter(ctx, textRenderer,
                    NeoUi.clip(textRenderer, NeoUi.tr(ONLINE_ACTIONS[i]), bw - 12),
                    x + bw / 2, yy + 11,
                    i == 0 ? NeoUi.WHITE : (i == 3 ? NeoUi.RED : (hover ? NeoUi.TEXT : NeoUi.TEXT_DIM)));
        }
    }

    private void drawOnlineStatus(DrawContext ctx, int y) {
        OnlineMusic.State s = OnlineMusic.state();
        int top = y + 2, bot = y + 38;
        NeoUi.rrect(ctx, cx1, top, cx2, bot, NeoUi.R_CARD, NeoUi.CARD);
        int color = s.phase == OnlineMusic.Phase.ERROR ? NeoUi.RED
                : s.phase == OnlineMusic.Phase.DONE ? NeoUi.GREEN
                : s.phase == OnlineMusic.Phase.IDLE ? NeoUi.TEXT_FAINT : NeoUi.AMBER;
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, s.message.isEmpty()
                        ? NeoUi.tr("gui.pvp_optimize.online.idle") : s.message, cx2 - cx1 - 24),
                cx1 + 10, top + 6, color);
        NeoUi.progress(ctx, cx1 + 10, top + 24, cx2 - cx1 - 20, 3, s.progress,
                s.phase == OnlineMusic.Phase.ERROR ? NeoUi.RED : NeoUi.ACCENT);
    }

    private void drawOnlineResult(DrawContext ctx, Row r, int y, int mx, int my) {
        List<OnlineMusic.Song> songs = OnlineMusic.state().songs;
        if (r.index < 0 || r.index >= songs.size()) return;
        boolean hover = NeoUi.inside(mx, my, cx1, y, cx2, y + 26);
        if (hover) NeoUi.rrect(ctx, cx1, y, cx2, y + 26, 6, NeoUi.CARD);
        NeoUi.cover(ctx, cx1 + 8, y + 5, 16, 16, 3, r.index * 11 + 5, 0x00FFFFFF);
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, songs.get(r.index).display(), cx2 - cx1 - 70),
                cx1 + 30, y + 9, hover ? NeoUi.TEXT : NeoUi.TEXT_DIM);
        NeoUi.iconDownload(ctx, cx2 - 16, y + 13, 12,
                hover ? NeoUi.ACCENT : NeoUi.TEXT_FAINT);
    }

    // ---------- 播放条 ----------

    private void drawPlayerBar(DrawContext ctx, int mx, int my) {
        NeoUi.rrect(ctx, pbX1, pbY1, pbX2, pbY2, 10, NeoUi.CARD);
        NeoUi.outline(ctx, pbX1, pbY1, pbX2, pbY2, 10, NeoUi.CARD_EDGE);

        boolean coverHover = pbCoverS > 0
                && NeoUi.inside(mx, my, pbCoverX, pbCoverY, pbCoverX + pbCoverS, pbCoverY + pbCoverS);
        if (pbCoverS > 0) {
            NeoUi.cover(ctx, pbCoverX, pbCoverY, pbCoverS, pbCoverS, 7, 5,
                    coverHover ? NeoUi.WHITE : 0xCCFFFFFF);
        }

        String name = MusicPlayer.status() == MusicPlayer.Status.STOPPED
                ? NeoUi.tr("gui.pvp_optimize.player.no_track") : MusicPlayer.trackName();
        NeoUi.text(ctx, textRenderer, NeoUi.clip(textRenderer, name, pbInfoW),
                pbInfoX, pbCtrlY, NeoUi.TEXT);
        NeoUi.text(ctx, textRenderer,
                NeoUi.clip(textRenderer, MusicPlayer.statusText() + "  "
                        + MusicLibrary.formatTime(MusicPlayer.position()) + " / "
                        + MusicLibrary.formatTime(MusicPlayer.duration()), pbInfoW),
                pbInfoX, pbCtrlY + 13, NeoUi.TEXT_FAINT);

        // 控制键：上一首 / 播放 / 下一首（图标全部矢量，窄条时只留播放键）
        if (richBar) {
            barButton(ctx, pbPrevX, 28, mx, my, false);
            NeoUi.iconPrev(ctx, pbPrevX + 14, pbCtrlY + pbCtrlH / 2, 11, iconColor(mx, my, pbPrevX, 28));
        }
        barButton(ctx, pbPlayX, 34, mx, my, true);
        if (MusicPlayer.isPlaying()) {
            NeoUi.iconPause(ctx, pbPlayX + 17, pbCtrlY + pbCtrlH / 2, 12, NeoUi.WHITE);
        } else {
            NeoUi.iconPlay(ctx, pbPlayX + 17, pbCtrlY + pbCtrlH / 2, 13, NeoUi.WHITE);
        }
        if (richBar) {
            barButton(ctx, pbNextX, 28, mx, my, false);
            NeoUi.iconNext(ctx, pbNextX + 14, pbCtrlY + pbCtrlH / 2, 11, iconColor(mx, my, pbNextX, 28));
        }

        if (showLoop) {
            boolean loopHover = NeoUi.inside(mx, my, pbLoopX, pbCtrlY, pbLoopX + 24, pbCtrlY + pbCtrlH);
            boolean one = MusicLibrary.repeat() == MusicLibrary.Repeat.ONE;
            boolean on = MusicLibrary.repeat() != MusicLibrary.Repeat.OFF;
            NeoUi.rrect(ctx, pbLoopX, pbCtrlY, pbLoopX + 24, pbCtrlY + pbCtrlH, 6,
                    loopHover ? NeoUi.CARD_HOVER : NeoUi.FIELD);
            NeoUi.iconRepeat(ctx, pbLoopX + 12, pbCtrlY + pbCtrlH / 2,
                    on ? NeoUi.ACCENT : NeoUi.TEXT_FAINT);
            if (one) NeoUi.disc(ctx, pbLoopX + 18, pbCtrlY + pbCtrlH / 2 + 5, 2, NeoUi.ACCENT);

            boolean shHover = NeoUi.inside(mx, my, pbShuffleX, pbCtrlY, pbShuffleX + 24, pbCtrlY + pbCtrlH);
            NeoUi.rrect(ctx, pbShuffleX, pbCtrlY, pbShuffleX + 24, pbCtrlY + pbCtrlH, 6,
                    shHover ? NeoUi.CARD_HOVER : NeoUi.FIELD);
            NeoUi.iconShuffle(ctx, pbShuffleX + 12, pbCtrlY + pbCtrlH / 2,
                    MusicLibrary.isShuffle() ? NeoUi.ACCENT : NeoUi.TEXT_FAINT);
        }

        if (showVolume) {
            int vy = pbCtrlY + pbCtrlH / 2;
            NeoUi.iconVolume(ctx, pbVolX - 4, vy, NeoUi.TEXT_FAINT);
            double f = MusicPlayer.volume() / 100.0;
            NeoUi.progress(ctx, pbVolX + 8, vy - 1, pbVolW - 8, 3, f, NeoUi.ACCENT);
            NeoUi.disc(ctx, pbVolX + 8 + (int) Math.round((pbVolW - 8) * f), vy, 3, NeoUi.WHITE);
        }

        // Dango 入口
        boolean mHover = NeoUi.inside(mx, my, pbMusicX, pbCtrlY, pbMusicX + 32, pbCtrlY + pbCtrlH);
        NeoUi.rrect(ctx, pbMusicX, pbCtrlY, pbMusicX + 32, pbCtrlY + pbCtrlH, 7,
                mHover ? NeoUi.mix(NeoUi.ACCENT, NeoUi.WHITE, 0.15f) : NeoUi.ACCENT);
        NeoUi.iconNote(ctx, pbMusicX + 16, pbCtrlY + pbCtrlH / 2, 12, NeoUi.WHITE);

        // 进度与时间（独立一行，绝不与按钮重叠）
        double dur = MusicPlayer.duration();
        NeoUi.progress(ctx, pbProgX, pbProgY, pbProgW, 3,
                dur > 0 ? MusicPlayer.position() / dur : 0, NeoUi.RED);
        NeoUi.text(ctx, textRenderer, MusicLibrary.formatTime(MusicPlayer.position()),
                pbProgX, pbProgY + 7, NeoUi.TEXT_FAINT);
        NeoUi.textRight(ctx, textRenderer, MusicLibrary.formatTime(dur),
                pbProgX + pbProgW, pbProgY + 7, NeoUi.TEXT_FAINT);
    }

    private int iconColor(int mx, int my, int x, int w) {
        return NeoUi.inside(mx, my, x, pbCtrlY, x + w, pbCtrlY + pbCtrlH) ? NeoUi.TEXT : NeoUi.TEXT_DIM;
    }

    private void barButton(DrawContext ctx, int x, int w, int mx, int my, boolean primary) {
        boolean hover = NeoUi.inside(mx, my, x, pbCtrlY, x + w, pbCtrlY + pbCtrlH);
        int fill = primary ? (hover ? NeoUi.mix(NeoUi.ACCENT, NeoUi.WHITE, 0.12f) : NeoUi.ACCENT)
                : (hover ? NeoUi.CARD_HOVER : NeoUi.FIELD);
        NeoUi.rrect(ctx, x, pbCtrlY, x + w, pbCtrlY + pbCtrlH, 7, fill);
    }

    private void drawTooltip(DrawContext ctx, String tip, int mx, int my) {
        int maxW = Math.min(300, width - 30);
        String key = tip + "\u0002" + maxW;
        List<String> lines;
        if (key.equals(tipCacheKey)) {
            lines = tipCacheLines;
        } else {
            lines = wrap(tip, maxW - 16);
            tipCacheKey = key;
            tipCacheLines = lines;
        }
        int tw = 0;
        for (String s : lines) tw = Math.max(tw, textRenderer.getWidth(s));
        tw = Math.min(maxW, tw + 16);
        int th = lines.size() * 10 + 12;
        int x = NeoUi.clamp(mx + 12, 4, width - tw - 4);
        int y = NeoUi.clamp(my + 12, 4, height - th - 4);
        NeoUi.rrect(ctx, x, y, x + tw, y + th, 6, 0xF00A0D14);
        NeoUi.outline(ctx, x, y, x + tw, y + th, 6, NeoUi.BORDER);
        for (int i = 0; i < lines.size(); i++) {
            NeoUi.text(ctx, textRenderer, lines.get(i), x + 8, y + 6 + i * 10, NeoUi.TEXT_DIM);
        }
    }

    private void updateMetrics() {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        badgeCache = (int) guiFps + " FPS · " + heapPct() + "%";
        statCells[0] = NeoUi.tr("gui.pvp_optimize.stat.fps");
        statCells[1] = (int) guiFps + " FPS";
        statCells[2] = NeoUi.tr("gui.pvp_optimize.stat.frame") + " "
                + String.format(Locale.ROOT, "%.1f", AdaptivePerformance.avgFrameMs()) + " ms";
        int lvl = AdaptivePerformance.level();
        statCells[3] = NeoUi.tr("gui.pvp_optimize.stat.adaptive");
        statCells[4] = NeoUi.tr("gui.pvp_optimize.level." + lvl);
        statCells[5] = "× " + String.format(Locale.ROOT, "%.2f", AdaptivePerformance.scale());
        statCells[6] = NeoUi.tr("gui.pvp_optimize.stat.mem");
        statCells[7] = usedMemoryMB() + " MB";
        statCells[8] = heapPct() + "% / " + maxMemoryMB() + " MB";
        boolean eOn = d.entityCullingEnabled && !RuntimeGuard.shouldSkipEntityCulling();
        boolean tOn = d.aggressiveEnabled && d.tileEntityCullingEnabled
                && !RuntimeGuard.shouldSkipTileEntityCulling();
        boolean aOn = d.aggressiveEnabled;
        statCells[9] = NeoUi.tr("gui.pvp_optimize.stat.culling");
        statCells[10] = ((eOn ? 1 : 0) + (tOn ? 1 : 0) + (aOn ? 1 : 0)) + " / 3";
        statCells[11] = NeoUi.tr("gui.pvp_optimize.on") + ": "
                + (eOn ? NeoUi.tr("gui.pvp_optimize.stat.entity") + " " : "")
                + (tOn ? NeoUi.tr("gui.pvp_optimize.stat.tile") + " " : "")
                + (aOn ? NeoUi.tr("gui.pvp_optimize.stat.aggressive") : "");
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 && button != 1) return false;

        if (keyCapture >= 0) {
            int idx = keyCapture;
            keyCapture = -1;
            applyKeyBinding(idx, InputUtil.Type.MOUSE.createFromCode(button));
            return true;
        }
        if (editEntry != null) commitEdit();

        if (!NeoUi.inside(mx, my, panelX, panelY, panelX + panelW, panelY + panelH)) {
            close();
            return true;
        }

        // 标题栏按钮
        if (NeoUi.inside(mx, my, btnCloseX, btnY2, btnCloseX + btnW2, btnY2 + btnH2)) {
            close();
            return true;
        }
        if (NeoUi.inside(mx, my, btnSizeX, btnY2, btnSizeX + btnW2, btnY2 + btnH2)) {
            PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
            d.guiWindowSize = (d.guiWindowSize + 1) % 3;
            PvPOptimizeConfig.save();
            NeoUi.clearCaches();
            layout();
            rebuild();
            return true;
        }
        if (NeoUi.inside(mx, my, btnHudX, btnY2, btnHudX + btnW2, btnY2 + btnH2)) {
            openHudEditor();
            return true;
        }
        if (NeoUi.inside(mx, my, panelX, panelY, panelX + panelW, titleBottom)) {
            draggingWindow = true;
            dragWinStartX = (int) mx;
            dragWinStartY = (int) my;
            dragWinStartOffX = PvPOptimizeConfig.get().guiWindowOffsetX;
            dragWinStartOffY = PvPOptimizeConfig.get().guiWindowOffsetY;
            return true;
        }

        // 侧栏搜索
        if (NeoUi.inside(mx, my, searchX1, searchY, searchX2, searchY + searchH)) {
            if (!query.isEmpty() && mx > searchX2 - 26) {
                query = "";
                rebuild();
            } else {
                clearFocus();
                searchFocused = true;
            }
            return true;
        }

        // 侧栏导航
        String hitTab = hitTab(mx, my);
        if (hitTab != null) {
            tab = hitTab;
            query = "";
            clearFocus();
            scroll = 0;
            rebuild();
            return true;
        }

        // 侧栏底部按钮
        if (NeoUi.inside(mx, my, hudBtnX1, hudBtnY1, hudBtnX2, hudBtnY2)) {
            tab = "hud_layout";
            query = "";
            clearFocus();
            scroll = 0;
            rebuild();
            return true;
        }
        if (NeoUi.inside(mx, my, dangoBtnX1, dangoBtnY1, dangoBtnX2, dangoBtnY2)) {
            openDango();
            return true;
        }
        if (showAccount && NeoUi.inside(mx, my, accountX1, accountY1, accountX2, accountY2)) {
            openDango();
            return true;
        }

        clearFocus();

        // 播放条
        if (NeoUi.inside(mx, my, pbX1, pbY1, pbX2, pbY2)) {
            return clickPlayerBar(mx, my);
        }

        // 滚动条
        if (isOverScrollbar((int) mx, (int) my)) {
            dragScrollbar = true;
            return dragScrollTo(my);
        }

        // 内容行
        if (NeoUi.inside(mx, my, cx1, cy1, cx2, cy2)) {
            for (Row r : rows) {
                int y = cy1 + r.top - scroll;
                if (y + r.h <= cy1 || y >= cy2) continue;
                if (my < y || my >= y + r.h) continue;
                switch (r.kind) {
                    case ENTRY -> {
                        if (mx >= ctrlX - 8) return clickEntry(r.entry, mx, my, y, button);
                        return true;
                    }
                    case PRESETS -> {
                        int cols = presetCols;
                        int bw = (cx2 - cx1 - GAP * (cols - 1)) / cols;
                        for (int i = 0; i < 4; i++) {
                            int c = i % cols, row = i / cols;
                            int x = cx1 + c * (bw + GAP);
                            int yy = y + 3 + row * 36;
                            if (NeoUi.inside(mx, my, x, yy, x + bw, yy + 30)) {
                                if (i == 3) Catalog.resetDefaults();
                                else Catalog.applyPreset(i);
                                rebuild();
                                return true;
                            }
                        }
                        return true;
                    }
                    case TRACK -> {
                        if (r.index >= 0 && r.index < tracks.size()) MusicPlayer.playIndex(r.index);
                        return true;
                    }
                    case KEY -> {
                        if (NeoUi.inside(mx, my, ctrlX, y + 2, ctrlX + ctrlW, y + 28)) {
                            if (button == 1) applyKeyBinding(r.index, InputUtil.UNKNOWN_KEY);
                            else {
                                clearFocus();
                                keyCapture = r.index;
                            }
                        }
                        return true;
                    }
                    case ONLINE_INPUT -> {
                        clearFocus();
                        onlineFocus = r.index;
                        return true;
                    }
                    case ONLINE_ACTIONS -> {
                        int bw = onlineBtnW();
                        for (int i = 0; i < 4; i++) {
                            int x = onlineBtnX(i), yy = onlineBtnY(y, i);
                            if (NeoUi.inside(mx, my, x, yy, x + bw, yy + 30)) {
                                onlineAction(i);
                                return true;
                            }
                        }
                        return true;
                    }
                    case ONLINE_RESULT -> {
                        List<OnlineMusic.Song> songs = OnlineMusic.state().songs;
                        if (r.index >= 0 && r.index < songs.size()) OnlineMusic.download(songs.get(r.index));
                        return true;
                    }
                    case PLAYER_CARD -> {
                        int tx = cx1 + 90;
                        int pw = Math.max(40, cx2 - 12 - tx);
                        if (NeoUi.inside(mx, my, tx, y + 78, tx + pw, y + 88)) {
                            double dur = MusicPlayer.duration();
                            if (dur > 0) MusicPlayer.seekFraction((mx - tx) / pw);
                        }
                        return true;
                    }
                    case DANGO_CARD -> {
                        openDango();
                        return true;
                    }
                    default -> {
                        return true;
                    }
                }
            }
        }
        return true;
    }

    /** 按当前 navScroll 现算命中（矩形不缓存，滚动后也不会错位）。 */
    private String hitTab(double mx, double my) {
        if (!NeoUi.inside(mx, my, navX1, navY1, navX2, navY2)) return null;
        int y = navY1 - navScroll;
        String prev = null;
        for (Tab t : Catalog.TABS) {
            if (!t.group.equals(prev)) {
                y += navGroupH;
                prev = t.group;
            }
            if (NeoUi.inside(mx, my, navX1, y, navX2, y + navItemH)) return t.id;
            y += navItemH + 2;
        }
        return null;
    }

    private void clearFocus() {
        searchFocused = false;
        onlineFocus = 0;
    }

    private void openHudEditor() {
        if (dirty) {
            Catalog.save();
            dirty = false;
        }
        PvPOptimizeConfig.save();
        HudEditorScreen.open();
    }

    private void openDango() {
        if (editEntry != null) commitEdit();
        if (dirty) {
            Catalog.save();
            dirty = false;
        }
        PvPOptimizeConfig.get().lenoMusicDir = lenoDirText;
        PvPOptimizeConfig.save();
        DangoBridge.open();
    }

    private void onlineAction(int i) {
        switch (i) {
            case 0 -> OnlineMusic.search(onlineQuery);
            case 1 -> OnlineMusic.downloadUrl(onlineUrl);
            case 2 -> OnlineMusic.importFolder(lenoDirText);
            default -> OnlineMusic.cancel();
        }
    }

    private void applyKeyBinding(int index, InputUtil.Key key) {
        if (index < 0 || index >= Catalog.KEYS.length || client == null) return;
        KeyBinding binding = Catalog.KEYS[index];
        String code = key.getTranslationKey();
        for (KeyBinding other : client.options.allKeys) {
            if (other != binding && !other.isUnbound()
                    && code.equals(other.getBoundKeyTranslationKey())) {
                other.setBoundKey(InputUtil.UNKNOWN_KEY);
            }
        }
        client.options.setKeyCode(binding, key);
        KeyBinding.updateKeysByCode();
        client.options.write();
        computeKeyConflicts();
    }

    private boolean clickEntry(Entry e, double mx, double my, int y, int button) {
        switch (e.kind) {
            case TOGGLE -> {
                int tx = toggleX(), ty = toggleY(y);
                if (!NeoUi.inside(mx, my, tx - 40, ty - 6, tx + NeoUi.SWITCH_W, ty + NeoUi.SWITCH_H + 6)) {
                    return false;
                }
                e.boolSet.set(!e.boolGet.getAsBoolean());
            }
            case SLIDER -> {
                int[] g = sliderGeom(y, e.valueText());
                if (NeoUi.inside(mx, my, g[0] - 4, g[1] - 8, g[0] + g[2] + 4, g[1] + 16)) {
                    e.setFromFraction((mx - g[0]) / g[2]);
                    dragEntry = e;
                    dragTrackX = g[0];
                    dragTrackW = g[2];
                    dirty = true;
                    return true;
                }
                if (NeoUi.inside(mx, my, g[3], g[1] - 6, g[3] + g[4], g[1] + 12)) {
                    startEdit(e);
                    return true;
                }
                return false;
            }
            case CYCLE -> {
                if (!NeoUi.inside(mx, my, ctrlX, comboY(y), cx2 - 10, comboY(y) + comboH())) return false;
                e.cycle(button == 1 ? -1 : 1);
            }
            case COLOR -> {
                int swY = rowTop(y) + 5;
                if (NeoUi.inside(mx, my, ctrlX, swY, ctrlX + 18, swY + 18)) {
                    e.nextColorPreset();
                } else if (NeoUi.inside(mx, my, ctrlX + 24, swY, cx2 - 10, swY + 18)) {
                    startEdit(e);
                    return true;
                } else {
                    return false;
                }
            }
            case BUTTON -> {
                int bw = 74;
                int bx = cx2 - 10 - bw;
                int by = rowTop(y) + 6;
                if (!NeoUi.inside(mx, my, bx, by, bx + bw, by + 18)) return false;
                e.action.run();
                rebuild();
            }
            case LABEL -> {
                return false;
            }
        }
        Catalog.save();
        return true;
    }

    /** 音量条 x 坐标 → 0-100 音量。 */
    private int volumeAt(double mx) {
        int trackW = Math.max(1, pbVolW - 8);
        double f = (mx - (pbVolX + 8)) / trackW;
        return NeoUi.clamp((int) Math.round(f * 100.0), 0, 100);
    }

    private boolean clickPlayerBar(double mx, double my) {
        if (richBar && NeoUi.inside(mx, my, pbPrevX, pbCtrlY, pbPrevX + 28, pbCtrlY + pbCtrlH)) {
            MusicPlayer.previous();
            return true;
        }
        if (NeoUi.inside(mx, my, pbPlayX, pbCtrlY, pbPlayX + 34, pbCtrlY + pbCtrlH)) {
            MusicPlayer.toggle();
            return true;
        }
        if (richBar && NeoUi.inside(mx, my, pbNextX, pbCtrlY, pbNextX + 28, pbCtrlY + pbCtrlH)) {
            MusicPlayer.next();
            return true;
        }
        if (NeoUi.inside(mx, my, pbMusicX, pbCtrlY, pbMusicX + (richBar ? 32 : 28), pbCtrlY + pbCtrlH)) {
            openDango();
            return true;
        }
        if (pbCoverS > 0 && NeoUi.inside(mx, my, pbCoverX, pbCoverY,
                pbCoverX + pbCoverS, pbCoverY + pbCoverS)) {
            tab = TAB_MUSIC;
            scroll = 0;
            rebuild();
            return true;
        }
        if (showLoop) {
            if (NeoUi.inside(mx, my, pbLoopX, pbCtrlY, pbLoopX + 24, pbCtrlY + pbCtrlH)) {
                MusicLibrary.setRepeat(switch (MusicLibrary.repeat()) {
                    case OFF -> MusicLibrary.Repeat.ALL;
                    case ALL -> MusicLibrary.Repeat.ONE;
                    case ONE -> MusicLibrary.Repeat.OFF;
                });
                return true;
            }
            if (NeoUi.inside(mx, my, pbShuffleX, pbCtrlY, pbShuffleX + 24, pbCtrlY + pbCtrlH)) {
                MusicLibrary.setShuffle(!MusicLibrary.isShuffle());
                return true;
            }
        }
        if (showVolume && NeoUi.inside(mx, my, pbVolX, pbCtrlY + 4, pbVolX + pbVolW, pbCtrlY + pbCtrlH - 4)) {
            MusicPlayer.setVolume(volumeAt(mx));
            dragVolume = true;
            Catalog.save();
            return true;
        }
        double dur = MusicPlayer.duration();
        if (NeoUi.inside(mx, my, pbProgX, pbProgY - 6, pbProgX + pbProgW, pbProgY + 20)) {
            if (dur > 0) MusicPlayer.seekFraction((mx - pbProgX) / pbProgW);
            return true;
        }
        return true;
    }

    private void startEdit(Entry e) {
        editEntry = e;
        editText = e.editInitialText();
        editColor = e.kind == Catalog.Kind.COLOR;
        searchFocused = false;
    }

    private void commitEdit() {
        if (editEntry == null) return;
        editEntry.applyEditText(editText);
        Catalog.save();
        editEntry = null;
        editText = "";
    }

    private void cancelEdit() {
        editEntry = null;
        editText = "";
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingWindow) {
            PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
            d.guiWindowOffsetX = dragWinStartOffX + (int) (mx - dragWinStartX);
            d.guiWindowOffsetY = dragWinStartOffY + (int) (my - dragWinStartY);
            layout();
            return true;
        }
        if (dragEntry != null) {
            dragEntry.setFromFraction((mx - dragTrackX) / dragTrackW);
            dirty = true;
            return true;
        }
        if (dragVolume) {
            MusicPlayer.setVolume(volumeAt(mx));
            return true;
        }
        if (dragScrollbar) return dragScrollTo(my);
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private boolean dragScrollTo(double my) {
        int viewH = cy2 - cy1;
        int totalH = Math.max(viewH, cursor);
        int thumbH = Math.max(26, viewH * viewH / totalH);
        double f = (my - cy1 - thumbH / 2.0) / Math.max(1, viewH - thumbH);
        scroll = NeoUi.clamp((int) Math.round(f * maxScroll), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingWindow) {
            draggingWindow = false;
            PvPOptimizeConfig.save();
            return true;
        }
        if (dragEntry != null || dragScrollbar || dragVolume) {
            dragEntry = null;
            dragScrollbar = false;
            dragVolume = false;
            if (dirty) {
                Catalog.save();
                dirty = false;
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (Math.abs(vertical) > 0.001) {
            int step = (int) Math.signum(vertical) * 24;
            if (NeoUi.inside(mx, my, sideX1, navY1, sideX2, navY2)) {
                navScroll = NeoUi.clamp(navScroll - step, 0, navMaxScroll);
            } else {
                scroll = NeoUi.clamp(scroll - step, 0, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCapture >= 0) {
            int idx = keyCapture;
            keyCapture = -1;
            applyKeyBinding(idx, keyCode == GLFW.GLFW_KEY_ESCAPE
                    ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(keyCode, scanCode));
            return true;
        }
        if (onlineFocus > 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                onlineFocus = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                int field = onlineFocus;
                onlineAction(field == 1 ? 0 : field == 2 ? 1 : 2);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String v = onlineValue(onlineFocus);
                if (!v.isEmpty()) setOnlineValue(onlineFocus, v.substring(0, v.length() - 1));
                return true;
            }
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V
                    && client != null) {
                try {
                    String clip = client.keyboard.getClipboard();
                    if (clip != null) setOnlineValue(onlineFocus,
                            (onlineValue(onlineFocus) + clip).replace('\n', ' '));
                } catch (Exception ignored) {
                }
                return true;
            }
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_A) {
                setOnlineValue(onlineFocus, "");
                return true;
            }
            return true;
        }
        if (editEntry != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editText.isEmpty()) {
                editText = editText.substring(0, editText.length() - 1);
                return true;
            }
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scroll = 0;
                rebuild();
                return true;
            }
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V
                    && client != null) {
                String clip = client.keyboard.getClipboard();
                if (clip != null) {
                    query = (query + clip).replace('\n', ' ');
                    if (query.length() > 40) query = query.substring(0, 40);
                    scroll = 0;
                    rebuild();
                }
                return true;
            }
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_A) {
                query = "";
                scroll = 0;
                rebuild();
                return true;
            }
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_F) {
            clearFocus();
            searchFocused = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onImeChar(int codePoint) {
        String s = new String(Character.toChars(codePoint));
        if (onlineFocus > 0) {
            String v = onlineValue(onlineFocus);
            if (v.length() + s.length() <= 500) setOnlineValue(onlineFocus, v + s);
            return;
        }
        if (editEntry != null) return;
        if (searchFocused && query.length() + s.length() <= 40) {
            query += s;
            scroll = 0;
            rebuild();
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (CharInputBridge.consumeDuplicate(chr)) return true;
        if (onlineFocus > 0) {
            if (chr >= 32 && chr != 127) {
                String v = onlineValue(onlineFocus);
                if (v.length() < 500) setOnlineValue(onlineFocus, v + chr);
            }
            return true;
        }
        if (editEntry != null) {
            if (editText.length() >= 12) return true;
            boolean ok = editColor
                    ? ((chr >= '0' && chr <= '9') || (chr >= 'a' && chr <= 'f')
                    || (chr >= 'A' && chr <= 'F') || chr == 'x' || chr == '#')
                    : ((chr >= '0' && chr <= '9') || chr == '.' || chr == '-');
            if (ok) editText += chr;
            return true;
        }
        if (searchFocused) {
            if (chr >= 32 && query.length() < 40) {
                query += chr;
                scroll = 0;
                rebuild();
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        NeoUi.clearCaches();
        super.removed();
    }

    @Override
    public void close() {
        if (editEntry != null) commitEdit();
        if (dirty) {
            Catalog.save();
            dirty = false;
        }
        keyCapture = -1;
        PvPOptimizeConfig.get().lenoMusicDir = lenoDirText;
        PvPOptimizeConfig.save();
        if (client != null) client.setScreen(parent);
    }

    // ==================== 指标 ====================

    private static long usedMemoryMB() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) >> 20;
    }

    private static long maxMemoryMB() {
        return Runtime.getRuntime().maxMemory() >> 20;
    }

    private static int heapPct() {
        Runtime r = Runtime.getRuntime();
        long used = r.totalMemory() - r.freeMemory();
        long max = Math.max(1L, r.maxMemory());
        return (int) (used * 100L / max);
    }
}
