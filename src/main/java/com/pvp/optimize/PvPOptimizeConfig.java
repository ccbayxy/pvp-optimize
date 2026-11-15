package com.pvp.optimize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central mod configuration / state holder.
 *
 * The user-tweakable values are stored in {@link Data} and persisted to
 * {@code config/pvp_optimize.json} so they survive game restarts. Keybinds
 * are registered in {@link #register()} which the mod entry point calls
 * from {@code onInitializeClient} (must run before the first tick).
 */
public final class PvPOptimizeConfig {

    private PvPOptimizeConfig() {}

    // ============ Persisted data ============
    public static final class Data {
        public boolean particlesEnabled = true;
        public boolean keepCritParticles = true;
        public boolean keepDamageParticles = true;
        public boolean keepPotionParticles = true;
        public boolean keepXpParticles = true;


        public boolean entityCullingEnabled = true;
        public double cullDistance = 16.0;

        // 红色滤镜 (red filter / full-screen overlay)
        public boolean redOverlayEnabled = true;
        public int overlayColor = 0xFFFF1010;     // ARGB (老版本默认 暗红)
        public int overlayOpacity = 15;           // 0-100 百分比

        // ====== 玩家渲染优化 (1.0.2 新增) ======
        // 隐藏玩家的多层皮肤 (jacket / leftSleeve / rightSleeve / leftPants / rightPants)
        public boolean hidePlayerSkinLayers = true;
        // 头顶名字标签背景透明 (保留文字)
        public boolean transparentNametagBg = true;
        // 距离 LOD (1.0.4): 玩家超过该距离 (方块) 时隐藏皮肤层/名字标签, 0 表示禁用
        public double hideSkinLayersDistance = 16.0;
        public double hideNametagsDistance = 16.0;
        // 群体隐身 (1.0.5): 6+ 玩家聚集 + fps 低于阈值 -> 隐藏皮肤层 (名字保留)
        public int crowdPlayerThreshold = 6;
        public double crowdFpsThreshold = 50.0;
        // 距离 LOD 强化 (1.0.6): 距离超阈值 + 玩家装备全空 -> 完全隐身 (整体不渲染)
        public boolean hideFarEmptyPlayers = true;

        // ====== 药水时间 HUD (1.0.1 新增) ======
        public boolean potionHudEnabled = true;
        public boolean potionHudColorByCategory = true;
        public int potionHudMaxLines = 6;
        public String potionHudPosition = "TOP_RIGHT";
        public int potionHudScale = 100;
        public int potionHudBgOpacity = 90;
        public int potionHudBgColor = 0x90000000;
        public int potionHudBeneficialColor = 0xFF55FF55;
        public int potionHudHarmfulColor = 0xFFFF5555;
        public int potionHudNeutralColor = 0xFFFFFFFF;
        public int potionHudTitleColor = 0xFFFFAA00;
        public boolean potionHudShowTitle = true;
        public boolean potionHudShowAmplifier = true;
        public boolean potionHudShowDuration = true;
        public boolean potionHudHideWhenEmpty = true;

        // ====== 激进优化 (功能完整老 1.0.6) ======
        public boolean aggressiveEnabled = true;
        public boolean tileEntityCullingEnabled = true;
        public double tileEntityCullDistance = 24.0;
        public boolean animationFreezeEnabled = true;
        public double animationFreezeDistance = 16.0;
        public boolean soundThrottleEnabled = true;
        public double soundThrottleDistance = 32.0;

        // ====== 1.0.6-v1 运行时稳定性 ======
        // 用 Boolean 可空: 老配置缺字段时 Gson 置 null, load 保底沿用构造器默认值
        public Boolean overlapGuardEnabled = true;      // 方块实体剔除对 EBE/MoreCulling 让位, 避免双重 Mixin 开销
        // 实体距离剔除不随 overlapGuardEnabled 一起让位: EntityCulling 做的是视锥/遮挡剔除,
        // 与本 mod 的"距离 + PvP 白名单"剔除互补而非重复。旧版无条件让位会让实体剔除整体失效。
        public Boolean entityCullingGuardEnabled = false; // 仅显式开启时, 检测到 EntityCulling 才停用实体距离剔除
        public Boolean memoryAdaptiveEnabled = false;   // 内存/GC 压力升高时自动缩小剔除距离 (改观感, 默认关)
        public Boolean diagnosticsLogEnabled = true;    // 周期性把堆占用/GC/重叠 mod 状态写入 latest.log

        // ====== 1.1.0 新增优化 (借鉴 OpenFine 思路, 独立实现) ======
        // 动态性能自适应: 帧时间环形采样 -> 带滞回的压力档 -> 收缩各项距离/预算
        public boolean adaptiveEnabled = true;
        public double adaptiveMinScale = 0.6;          // 重度掉帧时的最小收缩系数
        // 粒子: 在原有类型白名单之外叠加距离门槛与每帧预算
        public boolean particleDistanceCullEnabled = true;
        public double particleCullDistance = 24.0;
        public boolean particleBudgetEnabled = true;
        public int particleMaxPerFrame = 200;          // 0 表示不限制
        // 智能纹理动画 (Sodium 兼容版): 只放行必要图集 + 掉帧时降频 tick
        public boolean smartTextureAnimationsEnabled = true;

        // ====== 暂停界面 (1.2.0 新增) ======
        // 把「断开连接 / 返回主菜单」按钮移到左上角, 避免误触, 也腾出菜单中央空间
        public boolean pauseMenuRelocateEnabled = true;
        public int pauseMenuExitButtonX = 4;
        public int pauseMenuExitButtonY = 4;
        public int pauseMenuExitButtonWidth = 0;   // 0 = 保持原版宽度 (204)

        // ====== 游戏内音乐播放器 (1.3.0 新增, 纯本地 / 零联网) ======
        public boolean musicPlayerEnabled = true;
        public int musicVolume = 35;                  // 0-100，采用感知音量曲线，见 MUSIC_MASTER_GAIN
        public boolean musicAutoPlay = false;         // 重启后自动接着上次播放
        public boolean musicDuckVanilla = true;       // 播放时让路原版背景音乐
        /**
         * 音乐目录覆盖。留空 = 默认目录 &lt;游戏实例&gt;/config/pvp_optimize/music。
         *
         * <p>默认目录是<b>相对</b>游戏实例算出来的（{@code FabricLoader.getConfigDir()}），
         * 所以分享出去的 mod 在每个玩家自己的实例里各算各的，不会串到作者机器上的路径。
         * 想把歌库放到 D 盘或其他地方时，在这里填绝对路径（如 {@code D:/Music/mc}）；
         * 填相对路径则相对游戏目录解析。
         */
        public String musicDir = "";

        // ====== 轻量 GUI / 动态键位 / 在线音乐 ======
        /**
         * 背景压暗：0 = 完全不遮（世界原样可见）／1 = 轻压暗（默认）／2 = 深压暗。
         *
         * <p><b>这里只画三条纯色横带</b>，没有任何 GPU 后处理。之前的版本为了让面板看起来
         * 像液态玻璃，在 {@code renderBackground} 里驱动原版 blur 的 6 个 pass，1080p 下
         * 每帧上亿次纹理采样 —— 那是整个界面卡顿的唯一来源，现在已彻底删除。
         */
        public int guiBackdrop = 1;
        public int guiGlassOpacity = 24;               // 面板主体不透明度 0-60（%），24 ≈ 官方 tint 观感
        public int guiWindowSize = 0;                  // 0 紧凑(560x460) 1 标准(720x540) 2 大(880x620)
        public int guiWindowOffsetX = 0;               // 窗口相对默认位置（左侧居中）的偏移，拖动后持久化
        public int guiWindowOffsetY = 0;
        public boolean onlineMusicEnabled = true;      // 在线搜歌 / 下载（默认开，可随时关闭）
        public int onlineSearchLimit = 10;             // 每次搜索结果条数
        public String lenoMusicDir = "";               // LenoMusic (或任意下载器) 的输出目录，用于一键导入

        // ====== DangoMusic 风格灵动岛 ======
        /**
         * 是否把本地播放器接入灵动岛。
         *
         * <p>打开后本地曲目（曲名 / 歌手 / 封面 / 进度 / 暂停态）会写进 DangoMusic 官方的
         * 「外部音源」通道，由灵动岛用它自己的液态玻璃管线渲染 —— 本 mod 不再自绘播放器 HUD。
         */
        public boolean musicHudEnabled = true;
        public Boolean musicIslandExpand = true;      // 切歌 / 暂停 / 继续时展开成大卡片
        public Boolean musicIslandGlass = true;       // 液态玻璃质感（关 = 纯黑胶囊）
        public int musicIslandNotifySeconds = 4;      // 展开大卡片停留秒数 (1-10)
        public int islandScale = 100;                 // 灵动岛缩放 60-160 %

        // ====== 自定义 HUD 布局 (Badlion 风格：位置 + 大小，可拖动编辑) ======
        public Boolean hudCustomLayoutEnabled = true;
        public int potionHudX = -1;     // -1 = 自动靠右
        public int potionHudY = 8;
        // potionHudScale 沿用上面的旧字段（50-200 %），避免两个缩放来源打架

        // ====== 歌词面板（和药水 HUD 同一套可调外观） ======
        // Dango 的 3D 世界歌词在 1.20.6 兼容补丁里渲染不出来，这里用 2D 面板补上。
        // 外观项刻意和药水 HUD 对齐：背景色 + 背景不透明度 + 文字颜色 + 阴影 + 圆角。
        public Boolean lyricsHudEnabled = true;
        public Boolean lyricsHudBg = true;          // 是否画底板（关掉只剩文字）
        public int lyricsHudLines = 3;              // 1-5 行
        public int lyricsHudBgOpacity = 78;         // 底板不透明度 0-100 (%)
        public int lyricsHudBgColor = 0x0B0E15;     // 底板颜色（RGB，透明度由上一项决定）
        public int lyricsHudHighlightColor = 0xFF4C8DFF;   // 当前行（高亮）颜色
        public int lyricsHudTextColor = 0xFF9AA3B6;        // 其它行颜色（已唱过的自动淡到 55%）
        public Boolean lyricsHudRounded = true;     // 圆角卡片 / 直角矩形（药水 HUD 那种）
        public Boolean lyricsHudShadow = true;      // 文字阴影（关掉少一半文字顶点）
        public Boolean lyricsHudProgress = true;    // 底部行内进度条
        public Integer lyricsHudX = null;           // null = 自动（顶部居中，灵动岛下方）
        public Integer lyricsHudY = null;
        public Integer lyricsHudScale = null;       // null = 100 %
        /**
         * 歌词 / 灵动岛省电档：0 全效果 / 1 省电 / 2 极限。
         *
         * <p>它不是一个「生效中的开关」，而是主控板上那个循环按钮记住的档位；
         * 切换时由 {@code Catalog.applyHudLite} 一把改掉灵动岛玻璃、歌词阴影、行数、
         * 底板不透明度这些真正吃 GPU 的项。这样「掉帧时想一键轻下来」不用翻十几个滑块。
         */
        public int hudLiteMode = 0;

        // ====== 配置迁移标记 ======
        public int hudLayoutVersion = 1;    // 旧配置缺该字段 -> 0，保留新版 HUD 默认值
        public int musicVolumeVersion = 2;  // <2 表示旧线性音量，加载时换算到感知曲线
    }

    /**
     * 软件音量上限：100% 时也只是 0.30 的线性增益（约 -10.5 dB）。
     * 音乐文件通常是 0 dBFS 母带，原版游戏音效远低于此，早期线性映射导致
     * 70% 就盖过游戏声音、用户只能调到 3%。现在用 gain = MASTER * (v/100)^2，
     * 低音量段更细腻，100% 也不会压过游戏。
     */
    public static final float MUSIC_MASTER_GAIN = 0.30f;

    private static final Data DATA = new Data();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize.json");

    public static Data get() { return DATA; }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save(); // first run: write defaults
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            Data loaded = GSON.fromJson(r, Data.class);
            if (loaded != null) {
                DATA.particlesEnabled = loaded.particlesEnabled;
                DATA.keepCritParticles = loaded.keepCritParticles;
            DATA.keepDamageParticles = loaded.keepDamageParticles;
                DATA.keepPotionParticles = loaded.keepPotionParticles;
                DATA.keepXpParticles = loaded.keepXpParticles;
                DATA.entityCullingEnabled = loaded.entityCullingEnabled;
                DATA.cullDistance = loaded.cullDistance;
                DATA.redOverlayEnabled = loaded.redOverlayEnabled;
                DATA.overlayColor = loaded.overlayColor;
                DATA.overlayOpacity = loaded.overlayOpacity >= 0 && loaded.overlayOpacity <= 100 ? loaded.overlayOpacity : 15;
                DATA.hidePlayerSkinLayers = loaded.hidePlayerSkinLayers;
                DATA.transparentNametagBg  = loaded.transparentNametagBg;
                DATA.hideSkinLayersDistance = loaded.hideSkinLayersDistance > 0 ? loaded.hideSkinLayersDistance : 16.0;
                DATA.hideNametagsDistance   = loaded.hideNametagsDistance   > 0 ? loaded.hideNametagsDistance   : 16.0;
                DATA.crowdPlayerThreshold  = loaded.crowdPlayerThreshold > 0 ? loaded.crowdPlayerThreshold : 6;
                DATA.crowdFpsThreshold     = loaded.crowdFpsThreshold > 0 ? loaded.crowdFpsThreshold : 50.0;
                DATA.hideFarEmptyPlayers    = loaded.hideFarEmptyPlayers;
                DATA.potionHudEnabled           = loaded.potionHudEnabled;
                DATA.potionHudColorByCategory   = loaded.potionHudColorByCategory;
                DATA.potionHudMaxLines          = loaded.potionHudMaxLines;
                DATA.potionHudPosition          = loaded.potionHudPosition != null ? loaded.potionHudPosition : "TOP_RIGHT";
                DATA.potionHudScale             = loaded.potionHudScale > 0 ? loaded.potionHudScale : 100;
                DATA.potionHudBgOpacity         = loaded.potionHudBgOpacity;
                DATA.potionHudBgColor           = loaded.potionHudBgColor;
                DATA.potionHudBeneficialColor   = loaded.potionHudBeneficialColor;
                DATA.potionHudHarmfulColor      = loaded.potionHudHarmfulColor;
                DATA.potionHudNeutralColor      = loaded.potionHudNeutralColor;
                DATA.potionHudTitleColor        = loaded.potionHudTitleColor;
                DATA.potionHudShowTitle         = loaded.potionHudShowTitle;
                DATA.potionHudShowAmplifier     = loaded.potionHudShowAmplifier;
                DATA.potionHudShowDuration      = loaded.potionHudShowDuration;
                DATA.potionHudHideWhenEmpty     = loaded.potionHudHideWhenEmpty;
                DATA.aggressiveEnabled          = loaded.aggressiveEnabled;
                DATA.tileEntityCullingEnabled   = loaded.tileEntityCullingEnabled;
                DATA.tileEntityCullDistance     = loaded.tileEntityCullDistance > 0.0 ? loaded.tileEntityCullDistance : 24.0;
                DATA.animationFreezeEnabled     = loaded.animationFreezeEnabled;
                DATA.animationFreezeDistance    = loaded.animationFreezeDistance > 0.0 ? loaded.animationFreezeDistance : 16.0;
                DATA.soundThrottleEnabled       = loaded.soundThrottleEnabled;
                DATA.soundThrottleDistance      = loaded.soundThrottleDistance > 0.0 ? loaded.soundThrottleDistance : 32.0;
                if (loaded.overlapGuardEnabled != null)   DATA.overlapGuardEnabled   = loaded.overlapGuardEnabled;
                if (loaded.entityCullingGuardEnabled != null) DATA.entityCullingGuardEnabled = loaded.entityCullingGuardEnabled;
                if (loaded.memoryAdaptiveEnabled != null) DATA.memoryAdaptiveEnabled = loaded.memoryAdaptiveEnabled;
                if (loaded.diagnosticsLogEnabled != null) DATA.diagnosticsLogEnabled = loaded.diagnosticsLogEnabled;
                DATA.adaptiveEnabled = loaded.adaptiveEnabled;
                DATA.adaptiveMinScale = loaded.adaptiveMinScale > 0.4 && loaded.adaptiveMinScale <= 1.0
                        ? loaded.adaptiveMinScale : 0.6;
                DATA.particleDistanceCullEnabled = loaded.particleDistanceCullEnabled;
                DATA.particleCullDistance = loaded.particleCullDistance > 0.0 ? loaded.particleCullDistance : 24.0;
                DATA.particleBudgetEnabled = loaded.particleBudgetEnabled;
                DATA.particleMaxPerFrame = Math.max(0, loaded.particleMaxPerFrame);
                DATA.smartTextureAnimationsEnabled = loaded.smartTextureAnimationsEnabled;
                DATA.pauseMenuRelocateEnabled = loaded.pauseMenuRelocateEnabled;
                DATA.pauseMenuExitButtonX = Math.max(0, loaded.pauseMenuExitButtonX);
                DATA.pauseMenuExitButtonY = Math.max(0, loaded.pauseMenuExitButtonY);
                DATA.pauseMenuExitButtonWidth = Math.max(0, loaded.pauseMenuExitButtonWidth);
                DATA.musicPlayerEnabled = loaded.musicPlayerEnabled;
                DATA.musicHudEnabled = loaded.musicHudEnabled;
                // v2：musicHudEnabled 的语义从「自绘角落卡片」改成「接入 Dango 灵动岛」。
                // 旧版可能被关掉过，这里强制打开一次，之后用户的选择照常保存。
                if (loaded.hudLayoutVersion < 2) {
                    DATA.musicHudEnabled = true;
                }
                // 音量迁移：旧版是线性 gain=v/100，新版是 MASTER*(v/100)^2。
                // 反解 v = 100*sqrt(oldGain/MASTER)，用户原来调到 3% 的手感会落到约 32%。
                if (loaded.musicVolumeVersion < 2) {
                    double oldGain = Math.max(0.0, Math.min(1.0, loaded.musicVolume / 100.0));
                    DATA.musicVolume = (int) Math.round(100.0 * Math.sqrt(oldGain / MUSIC_MASTER_GAIN));
                } else {
                    DATA.musicVolume = Math.max(0, Math.min(100, loaded.musicVolume));
                }
                DATA.musicVolume = Math.max(0, Math.min(100, DATA.musicVolume));
                DATA.musicVolumeVersion = 2;
                DATA.musicAutoPlay = loaded.musicAutoPlay;
                DATA.musicDuckVanilla = loaded.musicDuckVanilla;
                // 目录覆盖：null / 空白都回落到默认目录，避免旧配置或手改出错导致扫不到歌
                DATA.musicDir = loaded.musicDir == null ? "" : loaded.musicDir.trim();
                // 背景压暗：0 无 / 1 轻（默认） / 2 深。旧配置没有这个字段，Gson 给 0
                // （= 完全不遮），那会让面板文字压在亮场景上，所以 0 也按默认档处理。
                DATA.guiBackdrop = Math.max(1, Math.min(2, loaded.guiBackdrop));
                DATA.guiGlassOpacity = Math.max(0, Math.min(60, loaded.guiGlassOpacity));
                DATA.guiWindowSize = Math.max(0, Math.min(2, loaded.guiWindowSize));
                DATA.guiWindowOffsetX = Math.max(-2000, Math.min(2000, loaded.guiWindowOffsetX));
                DATA.guiWindowOffsetY = Math.max(-2000, Math.min(2000, loaded.guiWindowOffsetY));
                DATA.onlineMusicEnabled = loaded.onlineMusicEnabled;
                DATA.onlineSearchLimit = Math.max(3, Math.min(30, loaded.onlineSearchLimit));
                DATA.lenoMusicDir = loaded.lenoMusicDir != null ? loaded.lenoMusicDir : "";

                // 灵动岛：这些字段由主控板写入 Dango 自己的模块值；旧配置缺字段时保留默认
                if (loaded.musicIslandExpand != null) DATA.musicIslandExpand = loaded.musicIslandExpand;
                if (loaded.musicIslandGlass != null) DATA.musicIslandGlass = loaded.musicIslandGlass;
                if (loaded.musicIslandNotifySeconds > 0) {
                    DATA.musicIslandNotifySeconds = Math.min(10, loaded.musicIslandNotifySeconds);
                }
                if (loaded.islandScale > 0) {
                    DATA.islandScale = Math.max(60, Math.min(160, loaded.islandScale));
                }

                // 自定义 HUD 布局：药水面板 + 歌词面板
                if (loaded.hudCustomLayoutEnabled != null) {
                    DATA.hudCustomLayoutEnabled = loaded.hudCustomLayoutEnabled;
                }
                DATA.potionHudX = Math.max(-1, Math.min(4000, loaded.potionHudX));
                DATA.potionHudY = Math.max(0, Math.min(4000, loaded.potionHudY));

                // 歌词面板：新字段用可空整数，旧配置缺字段时保留默认（自动顶部居中）
                if (loaded.lyricsHudEnabled != null) {
                    DATA.lyricsHudEnabled = loaded.lyricsHudEnabled;
                }
                if (loaded.lyricsHudBg != null) DATA.lyricsHudBg = loaded.lyricsHudBg;
                // 旧配置缺该字段时 Gson 给 0，不能让它把默认 3 行压成 1 行
                if (loaded.lyricsHudLines >= 1) {
                    DATA.lyricsHudLines = Math.min(5, loaded.lyricsHudLines);
                }
                if (loaded.lyricsHudX != null) {
                    DATA.lyricsHudX = Math.max(-1, Math.min(4000, loaded.lyricsHudX));
                }
                if (loaded.lyricsHudY != null) {
                    DATA.lyricsHudY = Math.max(0, Math.min(4000, loaded.lyricsHudY));
                }
                if (loaded.lyricsHudScale != null) {
                    DATA.lyricsHudScale = Math.max(50, Math.min(200, loaded.lyricsHudScale));
                }
                // v3：歌词面板外观（背景不透明度 / 颜色 / 阴影 / 圆角 / 进度条）。
                // 旧配置没有这些字段，整体跳过 -> 保留上面的默认值。
                if (loaded.hudLayoutVersion >= 3) {
                    DATA.lyricsHudBgOpacity = Math.max(0, Math.min(100, loaded.lyricsHudBgOpacity));
                    DATA.lyricsHudBgColor = loaded.lyricsHudBgColor & 0x00FFFFFF;
                    DATA.lyricsHudHighlightColor = loaded.lyricsHudHighlightColor | 0xFF000000;
                    DATA.lyricsHudTextColor = loaded.lyricsHudTextColor | 0xFF000000;
                    if (loaded.lyricsHudRounded != null) DATA.lyricsHudRounded = loaded.lyricsHudRounded;
                    if (loaded.lyricsHudShadow != null) DATA.lyricsHudShadow = loaded.lyricsHudShadow;
                    if (loaded.lyricsHudProgress != null) DATA.lyricsHudProgress = loaded.lyricsHudProgress;
                    DATA.hudLiteMode = Math.max(0, Math.min(2, loaded.hudLiteMode));
                }
                DATA.hudLayoutVersion = 3;
            }
        } catch (Exception e) {
            PvPOptimize.LOGGER.warn("[PvP-Optimize] Failed to read config, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(DATA, w);
            }
        } catch (Exception e) {
            PvPOptimize.LOGGER.warn("[PvP-Optimize] Failed to write config", e);
        }
    }

    // ============ Keybinds ============
    public static final KeyBinding OPEN_HUD = new KeyBinding(
            "key.pvp_optimize.open_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.pvp_optimize");
    public static final KeyBinding TOGGLE_PARTICLES = new KeyBinding(
            "key.pvp_optimize.toggle_particles",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.pvp_optimize");
    public static final KeyBinding TOGGLE_CULL = new KeyBinding(
            "key.pvp_optimize.toggle_cull",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.pvp_optimize");
    /** 红色滤镜开关 (J) */
    public static final KeyBinding TOGGLE_RED_OVERLAY = new KeyBinding(
            "key.pvp_optimize.toggle_red_overlay",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.pvp_optimize");

    // ====== 音乐播放器快捷键 (1.3.0) ======
    /**
     * 打开主控板的「音乐」页（本地播放器 / 在线下载）。
     * 默认不绑定：M 归内置 DangoMusic 官方界面，避免两套播放器抢同一个键；
     * 用户可在主控板「快捷键」页给它自由绑定任意键。
     */
    public static final KeyBinding OPEN_MUSIC = new KeyBinding(
            "key.pvp_optimize.open_music",
            InputUtil.Type.KEYSYM,
            -1,   // GLFW_KEY_UNKNOWN：默认未绑定
            "category.pvp_optimize");
    public static final KeyBinding MUSIC_PLAY_PAUSE = new KeyBinding(
            "key.pvp_optimize.music_play_pause",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.pvp_optimize");
    public static final KeyBinding MUSIC_NEXT = new KeyBinding(
            "key.pvp_optimize.music_next",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.pvp_optimize");
    public static final KeyBinding MUSIC_PREV = new KeyBinding(
            "key.pvp_optimize.music_prev",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.pvp_optimize");
    /** 打开 HUD 布局编辑器（拖动 / 缩放音乐与药水 HUD）。 */
    public static final KeyBinding OPEN_HUD_EDITOR = new KeyBinding(
            "key.pvp_optimize.hud_editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "category.pvp_optimize");

    /**
     * 注册全部键位。主控板的「快捷键」页会把这 9 个 {@link KeyBinding} 全部列出并允许改键，
     * 所以每一个都必须真正注册，否则改键结果不会写进 options.txt、也不会生效。
     *
     * <p>分工：H = 主控板总览，U = HUD 布局编辑器，P/K/J = 三个优化开关，
     * V/N/B = 本地播放器控制；M 由内置 DangoMusic 官方客户端自己注册（打开官方音乐界面），
     * 因此本 mod 的 {@link #OPEN_MUSIC} 默认不绑定，避免两套播放器抢同一个键。
     */
    public static void register() {
        load();
        KeyBindingHelper.registerKeyBinding(OPEN_HUD);
        KeyBindingHelper.registerKeyBinding(TOGGLE_PARTICLES);
        KeyBindingHelper.registerKeyBinding(TOGGLE_CULL);
        KeyBindingHelper.registerKeyBinding(TOGGLE_RED_OVERLAY);
        KeyBindingHelper.registerKeyBinding(OPEN_HUD_EDITOR);
        KeyBindingHelper.registerKeyBinding(OPEN_MUSIC);
        KeyBindingHelper.registerKeyBinding(MUSIC_PLAY_PAUSE);
        KeyBindingHelper.registerKeyBinding(MUSIC_NEXT);
        KeyBindingHelper.registerKeyBinding(MUSIC_PREV);
    }
}