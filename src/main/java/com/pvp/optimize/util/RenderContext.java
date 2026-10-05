package com.pvp.optimize.util;

import net.minecraft.entity.Entity;

/**
 * 渲染线程上下文, 在玩家渲染流程中传递当前 Entity 引用。
 *
 * 用于距离 LOD: PlayerEntityModel.setVisible 拿不到 entity,
 * 借助 EntityRenderer.render 入口处写入的 ThreadLocal 传递。
 */
public final class RenderContext {

    private RenderContext() {}

    private static final ThreadLocal<Entity> CURRENT = new ThreadLocal<>();

    public static void set(Entity e) {
        CURRENT.set(e);
    }

    public static Entity get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}