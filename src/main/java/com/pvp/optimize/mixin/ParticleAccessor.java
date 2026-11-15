package com.pvp.optimize.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 Particle 基类里的 protected 坐标字段。
 *
 * 直接把 x/y/z @Shadow 到 BillboardParticle 时，loom 会报
 * "Cannot find target for @Shadow field"（字段定义在父类 Particle 上），
 * 运行时会读到默认值甚至让 Mixin 应用失败。标准做法是让 Mixin 给
 * Particle 注入一个 Accessor 接口，再在渲染 Mixin 里强转调用。
 */
@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor("x") double pvpoptimize$getX();

    @Accessor("y") double pvpoptimize$getY();

    @Accessor("z") double pvpoptimize$getZ();
}
