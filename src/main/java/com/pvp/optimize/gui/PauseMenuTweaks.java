package com.pvp.optimize.gui;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;

/**
 * 暂停界面调整：把「断开连接 / 返回主菜单」按钮从底部按钮网格移到屏幕左上角。
 *
 * <p>不 @Shadow 目标类的私有字段，也不用 @Redirect 改网格布局 —— 那样容易和
 * Mod Menu / ImmediatelyFast 等同样注入 {@code GameMenuScreen} 的 mod 抢注入点。
 * 这里只在 {@code init()} 结束后遍历 screen 的子控件，按翻译键认出目标按钮再改坐标，
 * 对原版布局零侵入，窗口尺寸变化导致重新 init 时会自动再定位一次。</p>
 */
public final class PauseMenuTweaks {

    private PauseMenuTweaks() {}

    private static final String KEY_DISCONNECT = "menu.disconnect";        // 多人服务器
    private static final String KEY_RETURN_TO_MENU = "menu.returnToMenu";  // 单人游戏
    private static final String KEY_SAVING_LEVEL = "menu.savingLevel";     // 单人保存中

    /**
     * @param children    {@code Screen.children()}，按钮由 GridWidget.forEachChild(addDrawableChild) 加入
     * @param screenWidth 当前界面宽度，用于把配置坐标 clamp 进屏幕内
     */
    public static void relocateExitButton(List<? extends Element> children,
                                          int screenWidth, int screenHeight) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.pauseMenuRelocateEnabled) return;

        ButtonWidget target = findExitButton(children);
        if (target == null) {
            // showMenu=false（例如部分 Realms 场景）时原版根本不创建该按钮，属正常情况。
            PvPOptimize.LOGGER.debug("[PvP-Optimize] 暂停界面未发现断开连接按钮, 跳过重定位");
            return;
        }

        int width = cfg.pauseMenuExitButtonWidth > 0
                ? cfg.pauseMenuExitButtonWidth : target.getWidth();
        int height = target.getHeight();
        int x = clamp(cfg.pauseMenuExitButtonX, 0, Math.max(0, screenWidth - width));
        int y = clamp(cfg.pauseMenuExitButtonY, 0, Math.max(0, screenHeight - height));

        if (width != target.getWidth()) target.setWidth(width);
        target.setX(x);
        target.setY(y);
    }

    private static ButtonWidget findExitButton(List<? extends Element> children) {
        String disconnectText = Text.translatable(KEY_DISCONNECT).getString();
        String returnText = Text.translatable(KEY_RETURN_TO_MENU).getString();

        for (Element e : children) {
            if (!(e instanceof ButtonWidget button)) continue;
            String key = translationKey(button.getMessage());
            if (key != null) {
                if (KEY_DISCONNECT.equals(key) || KEY_RETURN_TO_MENU.equals(key)
                        || KEY_SAVING_LEVEL.equals(key)) {
                    return button;
                }
            } else {
                // 兜底：某些 mod 换掉了按钮的 Text 实现，改用本地化后的字符串比对。
                String msg = button.getMessage().getString();
                if (msg.equals(disconnectText) || msg.equals(returnText)) return button;
            }
        }
        return null;
    }

    private static String translationKey(Text text) {
        if (text == null) return null;
        TextContent content = text.getContent();
        return content instanceof TranslatableTextContent translatable
                ? translatable.getKey() : null;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }
}
