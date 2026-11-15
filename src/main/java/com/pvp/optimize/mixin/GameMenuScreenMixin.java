package com.pvp.optimize.mixin;

import com.pvp.optimize.gui.PauseMenuTweaks;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 暂停界面调整注入点。
 *
 * <p>选在 {@code init()} 的 TAIL：此时 GridWidget 已完成 refreshPositions / setPos，
 * 所有按钮的绝对坐标都已固定，改完不会被后续布局覆盖；{@code render()} 只读坐标
 * 绘制举报草稿图标，不重设位置。窗口缩放重新 init 时会自动再定位一次。</p>
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {

    @Inject(method = "init()V", at = @At("TAIL"))
    private void pvpoptimize$relocateExitButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        PauseMenuTweaks.relocateExitButton(self.children(), self.width, self.height);
    }
}
