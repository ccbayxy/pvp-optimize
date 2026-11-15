package com.pvp.optimize.perf;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智能纹理动画 —— Sodium 兼容版。
 *
 * <p>OpenFine 的 {@code SmartAnimations} 会在区块网格构建时记录"本帧实际渲染过的 sprite"，
 * 下一帧只 tick 这些可见 sprite，跳过离屏动态纹理的上传。这个做法需要 hook 区块网格
 * 构建，在本项目的整合包（Sodium 0.5.11 完全重写了网格管线）里会直接冲突，因此这里
 * 采用等价思路但换一个零冲突的落点：只拦截 {@code SpriteAtlasTexture.tickAnimatedSprites}。
 *
 * <p>策略：
 * <ul>
 *   <li>只对方块图集 / 粒子图集做处理 —— 动态纹理（水、岩浆、传送门、火焰、烟）几乎
 *       全部集中在这两张图集；其它图集保持原版每帧 tick，避免误伤第三方 mod。</li>
 *   <li>正常帧率时完全放行，行为与原版一致；掉帧时按压力档把动画纹理更新降到
 *       每 2 / 每 3 帧一次，跳过 glTexSubImage 上传，动画略慢但帧时间更平稳。</li>
 * </ul>
 *
 * <p>跳过的只是"动画帧推进"，不影响纹理绑定与已上传内容，因此不会花屏或崩溃。
 */
public final class SmartTextureAnimations {

    private SmartTextureAnimations() {}

    private static final AtomicInteger TICK_COUNTER = new AtomicInteger();

    public static boolean shouldTick(Identifier atlasId) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.smartTextureAnimationsEnabled) return true;

        if (atlasId == null) return true;
        boolean animatedAtlas = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE.equals(atlasId)
                || SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE.equals(atlasId);
        if (!animatedAtlas) return true;

        int level = AdaptivePerformance.level();
        if (level == 0) return true;   // 帧率正常：完全等价于原版

        int interval = level == 2 ? 3 : 2;
        // 只让 1/interval 的 tick 通过，其余帧跳过动画上传
        return Math.floorMod(TICK_COUNTER.incrementAndGet(), interval) == 0;
    }
}
