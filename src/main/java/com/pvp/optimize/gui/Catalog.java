package com.pvp.optimize.gui;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.hud.HudLayout;
import com.pvp.optimize.music.MusicLibrary;
import com.pvp.optimize.music.MusicPlayer;
import net.minecraft.client.option.KeyBinding;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 设置目录：把 {@link PvPOptimizeConfig.Data} 里每一个可调字段登记成一个可视条目。
 *
 * <p>新 GUI 不通过 Cloth Config 读取配置，而是直接读写 {@code Data} 并调用
 * {@link PvPOptimizeConfig#save()}，因此热路径代码（每帧读配置）无需改动，
 * 所有改动即时生效。标签复用已有的 {@code config.pvp_optimize.*} 翻译键，
 * 中英文都能直接用，不需要为每个控件重复写一遍文案。
 */
public final class Catalog {

    private Catalog() {}

    public interface BoolSetter { void set(boolean v); }

    public enum Kind { TOGGLE, SLIDER, CYCLE, COLOR, BUTTON, LABEL }

    /** 一个可显示 / 可交互的设置条目。 */
    public static final class Entry {
        public String id, cat, labelKey, tipKey, unit = "";
        public Kind kind;
        public BooleanSupplier boolGet;
        public BoolSetter boolSet;
        public DoubleSupplier numGet;
        public DoubleConsumer numSet;
        public double min, max, step;
        public boolean integer;
        public String[] optionKeys;
        public IntSupplier idxGet;
        public IntConsumer idxSet;
        public IntSupplier colorGet;
        public IntConsumer colorSet;
        public Runnable action;
        public Supplier<String> dynamic;

        public String label() {
            return dynamic != null ? dynamic.get() : Ui.tr(labelKey);
        }

        /** 缺失翻译时 Text.translatable 会原样返回 key，这里据此判空。 */
        public String tip() {
            if (tipKey == null) return "";
            String s = Ui.tr(tipKey);
            return s.equals(tipKey) ? "" : s;
        }

        public String desc() {
            String t = tip();
            if (t.isEmpty()) return "";
            int n = t.indexOf('\n');
            return n < 0 ? t : t.substring(0, n);
        }

        public String valueText() {
            return switch (kind) {
                case TOGGLE -> boolGet.getAsBoolean()
                        ? Ui.tr("gui.pvp_optimize.on") : Ui.tr("gui.pvp_optimize.off");
                case SLIDER -> formatNumber(numGet.getAsDouble());
                case CYCLE -> optionText(idxGet.getAsInt());
                case COLOR -> String.format(Locale.ROOT, "%08X", colorGet.getAsInt());
                case BUTTON -> "";
                case LABEL -> "";
            };
        }

        public String formatNumber(double v) {
            String s = integer ? Integer.toString((int) Math.round(v))
                    : (Math.abs(v - Math.round(v)) < 0.001
                        ? Integer.toString((int) Math.round(v))
                        : String.format(Locale.ROOT, "%.2f", v));
            return s + unit;
        }

        public String optionText(int i) {
            if (optionKeys == null || optionKeys.length == 0) return "";
            int n = ((i % optionKeys.length) + optionKeys.length) % optionKeys.length;
            return Ui.tr(optionKeys[n]);
        }

        public int optionCount() { return optionKeys == null ? 0 : optionKeys.length; }

        public void setFromFraction(double f) {
            double raw = min + (max - min) * Ui.clamp(f, 0.0, 1.0);
            double st = step > 0 ? step : (max - min) / 20.0;
            raw = min + Math.round((raw - min) / st) * st;
            raw = Ui.clamp(raw, min, max);
            if (integer) raw = Math.round(raw);
            numSet.accept(raw);
        }

        public void nudge(int dir) {
            double st = step > 0 ? step : (max - min) / 20.0;
            double raw = Ui.clamp(numGet.getAsDouble() + dir * st, min, max);
            if (integer) raw = Math.round(raw);
            numSet.accept(raw);
        }

        public double fraction() {
            return Ui.clamp((numGet.getAsDouble() - min) / Math.max(0.00001, max - min), 0.0, 1.0);
        }

        public void cycle(int dir) {
            int n = optionCount();
            if (n == 0) return;
            idxSet.accept(((idxGet.getAsInt() + dir) % n + n) % n);
        }

        public void nextColorPreset() {
            int cur = colorGet.getAsInt();
            int at = 0;
            for (int i = 0; i < COLOR_PRESETS.length; i++) {
                if (COLOR_PRESETS[i] == cur) { at = i; break; }
            }
            colorSet.accept(COLOR_PRESETS[(at + 1) % COLOR_PRESETS.length]);
        }

        /** 精确值编辑：颜色用 16 进制，数值用 10 进制。 */
        public void applyEditText(String s) {
            try {
                if (kind == Kind.COLOR) {
                    String hex = s.trim().toLowerCase(Locale.ROOT);
                    if (hex.startsWith("0x")) hex = hex.substring(2);
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    if (hex.isEmpty()) return;
                    colorSet.accept((int) Long.parseLong(hex, 16));
                } else {
                    double v = Double.parseDouble(s.trim());
                    numSet.accept(Ui.clamp(integer ? Math.round(v) : v, min, max));
                }
            } catch (NumberFormatException ignored) {
                // 输入非法时保持原值
            }
        }

        public String editInitialText() {
            return kind == Kind.COLOR
                    ? String.format(Locale.ROOT, "%08X", colorGet.getAsInt())
                    : (integer ? Integer.toString((int) Math.round(numGet.getAsDouble()))
                               : String.format(Locale.ROOT, "%.2f", numGet.getAsDouble()));
        }
    }

    /** 侧栏分类。 */
    public static final class Tab {
        public final String id, titleKey, group;
        public final int dot;
        public Tab(String id, String titleKey, String group, int dot) {
            this.id = id; this.titleKey = titleKey; this.group = group; this.dot = dot;
        }
        public String title() { return Ui.tr(titleKey); }
    }

    public static final List<Tab> TABS = List.of(
            new Tab("overview",  "gui.pvp_optimize.tab.overview",   "main",     0xFF5EEAD4),
            new Tab("music",     "config.pvp_optimize.category.music", "main",  0xFF9380FF),
            new Tab("online",    "gui.pvp_optimize.tab.online",      "main",     0xFF62C6FF),
            new Tab("particles", "config.pvp_optimize.category.particles", "settings", 0xFF5EEAD4),
            new Tab("culling",   "config.pvp_optimize.category.culling",   "settings", 0xFF62C6FF),
            new Tab("player",    "config.pvp_optimize.category.player",    "settings", 0xFF62C6FF),
            new Tab("aggressive","config.pvp_optimize.category.aggressive", "settings", 0xFFFFC46B),
            new Tab("advanced",  "config.pvp_optimize.category.advanced",   "settings", 0xFFFFC46B),
            new Tab("hud",       "config.pvp_optimize.category.hud",       "settings", 0xFFFF9AD0),
            new Tab("overlay",   "config.pvp_optimize.category.overlay",    "settings", 0xFFFF7A90),
            new Tab("stability", "config.pvp_optimize.category.stability",  "settings", 0xFF9AA5BA),
            new Tab("pause_menu","config.pvp_optimize.category.pause_menu", "settings", 0xFF9AA5BA),
            new Tab("hud_layout","config.pvp_optimize.category.hud_layout", "tools", 0xFFFF9AD0),
            new Tab("keys",      "gui.pvp_optimize.tab.keys",         "tools",    0xFF9380FF),
            new Tab("gui",       "config.pvp_optimize.category.gui",  "tools",    0xFF62C6FF)
    );

    public static final Map<String, Tab> TAB_BY_ID;
    static {
        Map<String, Tab> m = new LinkedHashMap<>();
        for (Tab t : TABS) m.put(t.id, t);
        TAB_BY_ID = m;
    }

    // ===== 枚举选项 =====
    private static final String[] POS_KEYS = {
            "gui.pvp_optimize.pos.top_left", "gui.pvp_optimize.pos.top_right",
            "gui.pvp_optimize.pos.bottom_left", "gui.pvp_optimize.pos.bottom_right"
    };
    private static final String[] POS_VALUES = {
            "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"
    };
    private static final String[] WINDOW_KEYS = {
            "gui.pvp_optimize.window.compact",
            "gui.pvp_optimize.window.standard",
            "gui.pvp_optimize.window.large"
    };

    /** 背景压暗三档：不遮 / 轻遮（默认） / 深遮。纯色横带，零后处理。 */
    private static final String[] BACKDROP_KEYS = {
            "gui.pvp_optimize.backdrop.none",
            "gui.pvp_optimize.backdrop.light",
            "gui.pvp_optimize.backdrop.deep"
    };
    /** 歌词 / 灵动岛省电三档（见 {@link #applyHudLite}）。 */
    private static final String[] LITE_KEYS = {
            "gui.pvp_optimize.lite.full",
            "gui.pvp_optimize.lite.save",
            "gui.pvp_optimize.lite.min"
    };
    private static final String[] COLOR_KEYS = {
            "config.pvp_optimize.overlayColor.darkRed", "config.pvp_optimize.overlayColor.red",
            "config.pvp_optimize.overlayColor.orange", "config.pvp_optimize.overlayColor.yellow",
            "config.pvp_optimize.overlayColor.green", "config.pvp_optimize.overlayColor.cyan",
            "config.pvp_optimize.overlayColor.blue", "config.pvp_optimize.overlayColor.purple",
            "config.pvp_optimize.overlayColor.black", "config.pvp_optimize.overlayColor.white"
    };
    public static final int[] OVERLAY_COLORS = {
            0xFFFF1010, 0xFFFF0000, 0xFFFF8800, 0xFFFFFF00, 0xFF33FF33,
            0xFF00FFFF, 0xFF2244FF, 0xFF8800FF, 0xFF000000, 0xFFFFFFFF
    };
    public static final int[] COLOR_PRESETS = {
            0xFF000000, 0x90000000, 0xFFFFFFFF, 0xFF55FF55,
            0xFFFF5555, 0xFFFFAA00, 0xFF33AAFF, 0xFFAA66FF
    };

    public static final List<Entry> ALL = build();
    public static final Map<String, Entry> BY_ID;
    static {
        Map<String, Entry> m = new HashMap<>();
        for (Entry e : ALL) m.put(e.id, e);
        BY_ID = m;
    }

    public static List<Entry> forCategory(String cat) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ALL) if (e.cat.equals(cat)) out.add(e);
        return out;
    }

    public static List<Entry> search(String q) {
        String s = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        if (s.isEmpty()) return out;
        for (Entry e : ALL) {
            if (e.kind == Kind.LABEL) continue;   // 说明文字不是设置项
            String hay = (e.label() + " " + e.tip() + " " + e.id + " "
                    + (TAB_BY_ID.containsKey(e.cat) ? TAB_BY_ID.get(e.cat).title() : e.cat))
                    .toLowerCase(Locale.ROOT);
            if (hay.contains(s)) out.add(e);
        }
        return out;
    }

    // ===== 条目构造 =====

    private static Entry base(String cat, String id, Kind kind) {
        Entry e = new Entry();
        e.cat = cat;
        e.id = id;
        e.kind = kind;
        e.labelKey = "config.pvp_optimize." + id;
        e.tipKey = "config.pvp_optimize." + id + ".tooltip";
        return e;
    }

    private static Entry toggle(String cat, String id, BooleanSupplier get, BoolSetter set) {
        Entry e = base(cat, id, Kind.TOGGLE);
        e.boolGet = get; e.boolSet = set;
        return e;
    }

    private static Entry slider(String cat, String id, double min, double max, double step,
                                boolean integer, String unit,
                                DoubleSupplier get, DoubleConsumer set) {
        Entry e = base(cat, id, Kind.SLIDER);
        e.min = min; e.max = max; e.step = step;
        e.integer = integer; e.unit = unit;
        e.numGet = get; e.numSet = set;
        return e;
    }

    private static Entry cycle(String cat, String id, String[] optionKeys,
                               IntSupplier get, IntConsumer set) {
        Entry e = base(cat, id, Kind.CYCLE);
        e.optionKeys = optionKeys; e.idxGet = get; e.idxSet = set;
        return e;
    }

    private static Entry color(String cat, String id, IntSupplier get, IntConsumer set) {
        Entry e = base(cat, id, Kind.COLOR);
        e.colorGet = get; e.colorSet = set;
        return e;
    }

    private static Entry button(String cat, String id, String labelKey, String tipKey, Runnable action) {
        Entry e = base(cat, id, Kind.BUTTON);
        e.labelKey = labelKey; e.tipKey = tipKey;
        e.action = action;
        return e;
    }

    private static Entry info(String cat, String id, Supplier<String> text) {
        Entry e = new Entry();
        e.cat = cat; e.id = id; e.kind = Kind.LABEL;
        e.dynamic = text;
        return e;
    }

    private static int posIndex(String v) {
        for (int i = 0; i < POS_VALUES.length; i++) if (POS_VALUES[i].equals(v)) return i;
        return 1;
    }

    private static int colorIndex(int argb) {
        for (int i = 0; i < OVERLAY_COLORS.length; i++) if (OVERLAY_COLORS[i] == argb) return i;
        return 0;
    }

    private static List<Entry> build() {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        List<Entry> l = new ArrayList<>();

        // ---------- 粒子过滤 ----------
        l.add(toggle("particles", "particlesEnabled", () -> d.particlesEnabled, v -> d.particlesEnabled = v));
        l.add(toggle("particles", "keepCritParticles", () -> d.keepCritParticles, v -> d.keepCritParticles = v));
        l.add(toggle("particles", "keepDamageParticles", () -> d.keepDamageParticles, v -> d.keepDamageParticles = v));
        l.add(toggle("particles", "keepPotionParticles", () -> d.keepPotionParticles, v -> d.keepPotionParticles = v));
        l.add(toggle("particles", "keepXpParticles", () -> d.keepXpParticles, v -> d.keepXpParticles = v));
        l.add(info("particles", "particles.note",
                () -> Ui.tr("gui.pvp_optimize.info.particles")));
        l.add(info("particles", "particles.advanced",
                () -> Ui.tr("gui.pvp_optimize.info.particles_advanced")));

        // ---------- 实体剔除 ----------
        l.add(toggle("culling", "entityCullingEnabled", () -> d.entityCullingEnabled, v -> d.entityCullingEnabled = v));
        l.add(slider("culling", "cullDistance", 1, 64, 1, true, " 格",
                () -> d.cullDistance, v -> d.cullDistance = v));
        l.add(info("culling", "culling.note", () -> Ui.tr("gui.pvp_optimize.info.culling")));

        // ---------- 玩家渲染 ----------
        l.add(toggle("player", "hidePlayerSkinLayers", () -> d.hidePlayerSkinLayers, v -> d.hidePlayerSkinLayers = v));
        l.add(slider("player", "hideSkinLayersDistance", 0, 64, 1, true, " 格",
                () -> d.hideSkinLayersDistance, v -> d.hideSkinLayersDistance = v));
        l.add(toggle("player", "transparentNametagBg", () -> d.transparentNametagBg, v -> d.transparentNametagBg = v));
        l.add(slider("player", "hideNametagsDistance", 0, 64, 1, true, " 格",
                () -> d.hideNametagsDistance, v -> d.hideNametagsDistance = v));
        l.add(slider("player", "crowdPlayerThreshold", 3, 20, 1, true, " 人",
                () -> d.crowdPlayerThreshold, v -> d.crowdPlayerThreshold = (int) v));
        l.add(slider("player", "crowdFpsThreshold", 10, 120, 5, true, " FPS",
                () -> d.crowdFpsThreshold, v -> d.crowdFpsThreshold = v));
        l.add(toggle("player", "hideFarEmptyPlayers", () -> d.hideFarEmptyPlayers, v -> d.hideFarEmptyPlayers = v));

        // ---------- 药水 HUD ----------
        l.add(toggle("hud", "potionHudEnabled", () -> d.potionHudEnabled, v -> d.potionHudEnabled = v));
        l.add(slider("hud", "potionHudMaxLines", 0, 20, 1, true, " 行",
                () -> d.potionHudMaxLines, v -> d.potionHudMaxLines = (int) v));
        l.add(toggle("hud", "potionHudHideWhenEmpty", () -> d.potionHudHideWhenEmpty, v -> d.potionHudHideWhenEmpty = v));
        l.add(toggle("hud", "potionHudShowTitle", () -> d.potionHudShowTitle, v -> d.potionHudShowTitle = v));
        l.add(toggle("hud", "potionHudShowAmplifier", () -> d.potionHudShowAmplifier, v -> d.potionHudShowAmplifier = v));
        l.add(toggle("hud", "potionHudShowDuration", () -> d.potionHudShowDuration, v -> d.potionHudShowDuration = v));
        l.add(toggle("hud", "potionHudColorByCategory", () -> d.potionHudColorByCategory, v -> d.potionHudColorByCategory = v));
        l.add(slider("hud", "potionHudBgOpacity", 0, 100, 5, true, "%",
                () -> d.potionHudBgOpacity, v -> d.potionHudBgOpacity = (int) v));
        l.add(color("hud", "potionHudBgColor", () -> d.potionHudBgColor, v -> d.potionHudBgColor = v));
        l.add(color("hud", "potionHudBeneficialColor", () -> d.potionHudBeneficialColor, v -> d.potionHudBeneficialColor = v));
        l.add(color("hud", "potionHudHarmfulColor", () -> d.potionHudHarmfulColor, v -> d.potionHudHarmfulColor = v));
        l.add(color("hud", "potionHudNeutralColor", () -> d.potionHudNeutralColor, v -> d.potionHudNeutralColor = v));
        l.add(color("hud", "potionHudTitleColor", () -> d.potionHudTitleColor, v -> d.potionHudTitleColor = v));

        // ---------- 屏幕滤镜 ----------
        l.add(toggle("overlay", "redOverlayEnabled", () -> d.redOverlayEnabled, v -> d.redOverlayEnabled = v));
        l.add(cycle("overlay", "overlayColor", COLOR_KEYS,
                () -> colorIndex(d.overlayColor), i -> d.overlayColor = OVERLAY_COLORS[i]));
        l.add(slider("overlay", "overlayOpacity", 0, 100, 5, true, "%",
                () -> d.overlayOpacity, v -> d.overlayOpacity = (int) v));

        // ---------- 激进优化 ----------
        l.add(toggle("aggressive", "aggressiveEnabled", () -> d.aggressiveEnabled, v -> d.aggressiveEnabled = v));
        l.add(toggle("aggressive", "tileEntityCullingEnabled", () -> d.tileEntityCullingEnabled, v -> d.tileEntityCullingEnabled = v));
        l.add(slider("aggressive", "tileEntityCullDistance", 4, 64, 1, true, " 格",
                () -> d.tileEntityCullDistance, v -> d.tileEntityCullDistance = v));
        l.add(toggle("aggressive", "animationFreezeEnabled", () -> d.animationFreezeEnabled, v -> d.animationFreezeEnabled = v));
        l.add(slider("aggressive", "animationFreezeDistance", 4, 64, 1, true, " 格",
                () -> d.animationFreezeDistance, v -> d.animationFreezeDistance = v));
        l.add(toggle("aggressive", "soundThrottleEnabled", () -> d.soundThrottleEnabled, v -> d.soundThrottleEnabled = v));
        l.add(slider("aggressive", "soundThrottleDistance", 4, 64, 1, true, " 格",
                () -> d.soundThrottleDistance, v -> d.soundThrottleDistance = v));

        // ---------- 动态性能 ----------
        l.add(toggle("advanced", "adaptiveEnabled", () -> d.adaptiveEnabled, v -> d.adaptiveEnabled = v));
        l.add(slider("advanced", "adaptiveMinScale", 0.4, 1.0, 0.05, false, "×",
                () -> d.adaptiveMinScale, v -> d.adaptiveMinScale = v));
        l.add(toggle("advanced", "particleDistanceCullEnabled", () -> d.particleDistanceCullEnabled, v -> d.particleDistanceCullEnabled = v));
        l.add(slider("advanced", "particleCullDistance", 4, 64, 1, true, " 格",
                () -> d.particleCullDistance, v -> d.particleCullDistance = v));
        l.add(toggle("advanced", "particleBudgetEnabled", () -> d.particleBudgetEnabled, v -> d.particleBudgetEnabled = v));
        l.add(slider("advanced", "particleMaxPerFrame", 0, 2000, 10, true, " 个",
                () -> d.particleMaxPerFrame, v -> d.particleMaxPerFrame = (int) v));
        l.add(toggle("advanced", "smartTextureAnimationsEnabled", () -> d.smartTextureAnimationsEnabled, v -> d.smartTextureAnimationsEnabled = v));

        // ---------- 运行时稳定性 ----------
        l.add(toggle("stability", "overlapGuardEnabled",
                () -> Boolean.TRUE.equals(d.overlapGuardEnabled), v -> d.overlapGuardEnabled = v));
        l.add(toggle("stability", "entityCullingGuardEnabled",
                () -> Boolean.TRUE.equals(d.entityCullingGuardEnabled), v -> d.entityCullingGuardEnabled = v));
        l.add(toggle("stability", "memoryAdaptiveEnabled",
                () -> Boolean.TRUE.equals(d.memoryAdaptiveEnabled), v -> d.memoryAdaptiveEnabled = v));
        l.add(toggle("stability", "diagnosticsLogEnabled",
                () -> Boolean.TRUE.equals(d.diagnosticsLogEnabled), v -> d.diagnosticsLogEnabled = v));

        // ---------- 暂停界面 ----------
        l.add(toggle("pause_menu", "pauseMenuRelocateEnabled", () -> d.pauseMenuRelocateEnabled, v -> d.pauseMenuRelocateEnabled = v));
        l.add(slider("pause_menu", "pauseMenuExitButtonX", 0, 400, 4, true, " px",
                () -> d.pauseMenuExitButtonX, v -> d.pauseMenuExitButtonX = (int) v));
        l.add(slider("pause_menu", "pauseMenuExitButtonY", 0, 400, 4, true, " px",
                () -> d.pauseMenuExitButtonY, v -> d.pauseMenuExitButtonY = (int) v));
        l.add(slider("pause_menu", "pauseMenuExitButtonWidth", 0, 400, 4, true, " px",
                () -> d.pauseMenuExitButtonWidth, v -> d.pauseMenuExitButtonWidth = (int) v));

        // ---------- 音乐播放器（曲目显示走 DangoMusic 官方灵动岛） ----------
        l.add(toggle("music", "musicPlayerEnabled", () -> d.musicPlayerEnabled, v -> d.musicPlayerEnabled = v));
        l.add(toggle("music", "musicHudEnabled", () -> d.musicHudEnabled, v -> d.musicHudEnabled = v));
        // 歌词面板：Dango 风格的 2D 覆盖层（Dango 的 3D 世界歌词在 1.20.6 补丁里渲染不出来）
        l.add(toggle("music", "lyricsHudEnabled",
                () -> !Boolean.FALSE.equals(d.lyricsHudEnabled), v -> d.lyricsHudEnabled = v));
        l.add(slider("music", "lyricsHudLines", 1, 5, 1, true, " 行",
                () -> d.lyricsHudLines, v -> d.lyricsHudLines = (int) v));
        l.add(toggle("music", "lyricsHudBg",
                () -> !Boolean.FALSE.equals(d.lyricsHudBg), v -> d.lyricsHudBg = v));
        // 和药水 HUD 一致的外观项：不透明度 / 背景色 / 文字色 / 阴影 / 圆角 / 进度条
        l.add(slider("music", "lyricsHudBgOpacity", 0, 100, 5, true, "%",
                () -> d.lyricsHudBgOpacity, v -> d.lyricsHudBgOpacity = (int) v));
        l.add(color("music", "lyricsHudBgColor",
                () -> d.lyricsHudBgColor & 0x00FFFFFF, v -> d.lyricsHudBgColor = v & 0x00FFFFFF));
        l.add(color("music", "lyricsHudHighlightColor",
                () -> d.lyricsHudHighlightColor | 0xFF000000, v -> d.lyricsHudHighlightColor = v | 0xFF000000));
        l.add(color("music", "lyricsHudTextColor",
                () -> d.lyricsHudTextColor | 0xFF000000, v -> d.lyricsHudTextColor = v | 0xFF000000));
        l.add(toggle("music", "lyricsHudRounded",
                () -> !Boolean.FALSE.equals(d.lyricsHudRounded), v -> d.lyricsHudRounded = v));
        l.add(toggle("music", "lyricsHudShadow",
                () -> !Boolean.FALSE.equals(d.lyricsHudShadow), v -> d.lyricsHudShadow = v));
        l.add(toggle("music", "lyricsHudProgress",
                () -> !Boolean.FALSE.equals(d.lyricsHudProgress), v -> d.lyricsHudProgress = v));
        // 一键轻量化：掉帧时不用翻十几个滑块，一档把灵动岛玻璃 / 歌词阴影 / 行数一起降下来
        l.add(cycle("music", "hudLiteMode", LITE_KEYS, () -> d.hudLiteMode, Catalog::applyHudLite));
        l.add(info("music", "music.perf_note", () -> Ui.tr("gui.pvp_optimize.info.hud_cost")));
        l.add(slider("music", "musicVolume", 0, 100, 5, true, "%",
                MusicPlayer::volume, v -> MusicPlayer.setVolume((int) v)));
        l.add(toggle("music", "musicAutoPlay", () -> d.musicAutoPlay, v -> d.musicAutoPlay = v));
        l.add(toggle("music", "musicDuckVanilla", () -> d.musicDuckVanilla, v -> d.musicDuckVanilla = v));
        // DangoMusic 灵动岛：这几项直接写 Dango 自己的模块值，改完立刻生效
        l.add(toggle("music", "musicIslandGlass",
                () -> !Boolean.FALSE.equals(d.musicIslandGlass), v -> d.musicIslandGlass = v));
        l.add(toggle("music", "musicIslandExpand",
                () -> !Boolean.FALSE.equals(d.musicIslandExpand), v -> d.musicIslandExpand = v));
        l.add(slider("music", "musicIslandNotifySeconds", 1, 10, 1, true, " s",
                () -> d.musicIslandNotifySeconds, v -> d.musicIslandNotifySeconds = (int) v));
        l.add(slider("music", "islandScale", 60, 160, 5, true, "%",
                () -> d.islandScale, v -> d.islandScale = (int) v));
        l.add(button("music", "music.refresh", "gui.pvp_optimize.action.refresh", null,
                MusicPlayer::refreshLibrary));
        l.add(button("music", "music.open_dir", "gui.pvp_optimize.action.open_dir", null,
                MusicLibrary::openFolder));
        l.add(button("music", "music.stop", "gui.pvp_optimize.action.stop", null,
                MusicPlayer::stop));
        l.add(info("music", "music.dir", () -> Ui.tr("gui.pvp_optimize.info.music_dir")
                + " " + MusicLibrary.musicDir()));

        // ---------- 界面 / GUI ----------
        // 背景压暗：只画三条纯色横带，没有任何 GPU 后处理（旧版的 6-pass 模糊已删除）
        l.add(cycle("gui", "guiBackdrop", BACKDROP_KEYS,
                () -> d.guiBackdrop, v -> d.guiBackdrop = v));
        l.add(slider("gui", "guiGlassOpacity", 0, 60, 2, true, "%",
                () -> d.guiGlassOpacity, v -> d.guiGlassOpacity = (int) v));
        l.add(cycle("gui", "guiWindowSize", WINDOW_KEYS,
                () -> d.guiWindowSize, v -> d.guiWindowSize = v));
        l.add(info("gui", "gui.note", () -> Ui.tr("gui.pvp_optimize.info.gui")));

        // ---------- 自定义 HUD 布局 (Badlion 风格) ----------
        l.add(toggle("hud_layout", "hudCustomLayoutEnabled",
                () -> !Boolean.FALSE.equals(d.hudCustomLayoutEnabled),
                v -> d.hudCustomLayoutEnabled = v));
        l.add(button("hud_layout", "hud.edit", "gui.pvp_optimize.hud.edit", null,
                HudEditorScreen::open));
        l.add(slider("hud_layout", "potionHudX", -1, 1600, 4, true, " px",
                () -> d.potionHudX, v -> d.potionHudX = (int) v));
        l.add(slider("hud_layout", "potionHudY", 0, 1000, 4, true, " px",
                () -> d.potionHudY, v -> d.potionHudY = (int) v));
        l.add(slider("hud_layout", "potionHudScale", 50, 200, 5, true, "%",
                () -> d.potionHudScale, v -> d.potionHudScale = (int) v));
        // 歌词面板（坐标 null = 自动，读出来按 -1 显示）
        l.add(slider("hud_layout", "lyricsHudX", -1, 1600, 4, true, " px",
                () -> d.lyricsHudX == null ? -1 : d.lyricsHudX, v -> d.lyricsHudX = (int) v));
        l.add(slider("hud_layout", "lyricsHudY", -1, 1000, 4, true, " px",
                () -> d.lyricsHudY == null ? -1 : d.lyricsHudY, v -> d.lyricsHudY = (int) v));
        l.add(slider("hud_layout", "lyricsHudScale", 50, 200, 5, true, "%",
                () -> d.lyricsHudScale == null ? 100 : d.lyricsHudScale,
                v -> d.lyricsHudScale = (int) v));
        l.add(button("hud_layout", "hud.reset", "gui.pvp_optimize.hud.reset", null,
                HudLayout::resetAll));
        l.add(info("hud_layout", "hud.note", () -> Ui.tr("gui.pvp_optimize.info.hud_layout")));

        // ---------- 在线音乐 ----------
        l.add(toggle("online", "onlineMusicEnabled",
                () -> d.onlineMusicEnabled, v -> d.onlineMusicEnabled = v));
        l.add(slider("online", "onlineSearchLimit", 3, 30, 1, true, " 首",
                () -> d.onlineSearchLimit, v -> d.onlineSearchLimit = (int) v));
        l.add(info("online", "online.note", () -> Ui.tr("gui.pvp_optimize.info.online")));

        return l;
    }

    // ===== 一键预设 =====

    /**
     * 歌词 / 灵动岛的「省电档」：一次改掉真正吃 GPU 的几项。
     *
     * <ul>
     *   <li>全效果：灵动岛液态玻璃 + 3 行歌词 + 文字阴影 + 进度条（默认，最好看）；</li>
     *   <li>省电：关掉灵动岛玻璃（换成纯色胶囊）+ 2 行歌词 + 无文字阴影；</li>
     *   <li>极限：再加关掉进度条、只留 1 行、底板更淡 —— 面板几乎不占帧时间。</li>
     * </ul>
     */
    public static void applyHudLite(int mode) {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        int m = Math.max(0, Math.min(2, mode));
        d.hudLiteMode = m;
        switch (m) {
            case 0 -> {
                d.musicIslandGlass = true;
                d.lyricsHudShadow = true;
                d.lyricsHudProgress = true;
                d.lyricsHudLines = 3;
                d.lyricsHudBgOpacity = 78;
            }
            case 1 -> {
                d.musicIslandGlass = false;
                d.lyricsHudShadow = false;
                d.lyricsHudProgress = true;
                d.lyricsHudLines = 2;
                d.lyricsHudBgOpacity = 60;
            }
            default -> {
                d.musicIslandGlass = false;
                d.lyricsHudShadow = false;
                d.lyricsHudProgress = false;
                d.lyricsHudLines = 1;
                d.lyricsHudBgOpacity = 40;
            }
        }
        save();
    }

    /** 0 = 性能优先, 1 = 均衡, 2 = 画质优先。只动性能相关字段，不动 HUD / 滤镜 / 音乐 / 暂停界面。 */
    public static void applyPreset(int preset) {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        switch (preset) {
            case 0 -> {
                d.particlesEnabled = true;
                d.entityCullingEnabled = true;
                d.cullDistance = 12.0;
                d.hidePlayerSkinLayers = true;
                d.transparentNametagBg = true;
                d.hideFarEmptyPlayers = true;
                d.aggressiveEnabled = true;
                d.tileEntityCullingEnabled = true;
                d.animationFreezeEnabled = true;
                d.soundThrottleEnabled = true;
                d.adaptiveEnabled = true;
                d.adaptiveMinScale = 0.5;
                d.particleDistanceCullEnabled = true;
                d.particleCullDistance = 16.0;
                d.particleBudgetEnabled = true;
                d.particleMaxPerFrame = 120;
                d.smartTextureAnimationsEnabled = true;
                d.entityCullingGuardEnabled = false;
            }
            case 1 -> {
                d.particlesEnabled = true;
                d.entityCullingEnabled = true;
                d.cullDistance = 16.0;
                d.hidePlayerSkinLayers = true;
                d.transparentNametagBg = true;
                d.hideFarEmptyPlayers = true;
                d.aggressiveEnabled = false;
                d.adaptiveEnabled = true;
                d.adaptiveMinScale = 0.6;
                d.particleDistanceCullEnabled = true;
                d.particleCullDistance = 24.0;
                d.particleBudgetEnabled = true;
                d.particleMaxPerFrame = 200;
                d.smartTextureAnimationsEnabled = true;
                d.entityCullingGuardEnabled = false;
            }
            default -> {
                d.particlesEnabled = false;
                d.entityCullingEnabled = false;
                d.hidePlayerSkinLayers = false;
                d.transparentNametagBg = false;
                d.hideFarEmptyPlayers = false;
                d.aggressiveEnabled = false;
                d.adaptiveEnabled = false;
                d.particleDistanceCullEnabled = false;
                d.particleBudgetEnabled = false;
                d.smartTextureAnimationsEnabled = false;
            }
        }
        save();
    }

    /** 用一份全新的 Data 覆盖所有字段，等价于「全部恢复出厂设置」。 */
    public static void resetDefaults() {
        PvPOptimizeConfig.Data d = PvPOptimizeConfig.get();
        PvPOptimizeConfig.Data fresh = new PvPOptimizeConfig.Data();
        try {
            for (Field f : PvPOptimizeConfig.Data.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.set(d, f.get(fresh));
            }
        } catch (IllegalAccessException e) {
            // Data 全是 public 字段，正常不会走到这里
        }
        MusicPlayer.setVolume(d.musicVolume);
        save();
    }

    public static void save() {
        PvPOptimizeConfig.save();
        MusicPlayer.setVolume(PvPOptimizeConfig.get().musicVolume);
    }

    /** 快捷键页用的键位表。 */
    public static final KeyBinding[] KEYS = {
            PvPOptimizeConfig.OPEN_HUD,
            PvPOptimizeConfig.OPEN_MUSIC,
            PvPOptimizeConfig.MUSIC_PLAY_PAUSE,
            PvPOptimizeConfig.MUSIC_NEXT,
            PvPOptimizeConfig.MUSIC_PREV,
            PvPOptimizeConfig.TOGGLE_PARTICLES,
            PvPOptimizeConfig.TOGGLE_CULL,
            PvPOptimizeConfig.TOGGLE_RED_OVERLAY,
            PvPOptimizeConfig.OPEN_HUD_EDITOR
    };
}
