package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.perf.AggressiveOptimizer;
import com.pvp.optimize.perf.RuntimeGuard;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 方块实体 (TileEntity) 距离裁剪: 超过配置距离的方块实体直接跳过渲染。
 * 注入点对应老版本 refmap 的 class_824.method_3555。
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$cullTileEntity(BlockEntity blockEntity, float tickDelta,
                                             MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                             CallbackInfo ci) {
        if (!AggressiveOptimizer.isEnabled(PvPOptimizeConfig.get().tileEntityCullingEnabled)) return;
        if (RuntimeGuard.shouldSkipTileEntityCulling()) return;
        BlockPos pos = blockEntity.getPos();
        double distSq = AggressiveOptimizer.squaredDistanceTo(
                (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5);
        if (AggressiveOptimizer.isBeyond(distSq, PvPOptimizeConfig.get().tileEntityCullDistance)) {
            ci.cancel();
        }
    }
}
