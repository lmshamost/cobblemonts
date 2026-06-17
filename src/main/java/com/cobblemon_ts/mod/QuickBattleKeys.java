package com.cobblemon_ts.mod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class QuickBattleKeys {

    public static final KeyMapping QUICK_BATTLE = new KeyMapping(
            "key.cobblemon_quick_battle.quick_battle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.gameplay"
    );

    // [线性-C02] 道具袋快捷键，默认未绑定（GLFW.GLFW_KEY_UNKNOWN）
    public static final KeyMapping OPEN_POKEBAG = new KeyMapping(
            "key.coblemon_ts.open_pokebag",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,  // 默认空，未绑定
            "key.categories.gameplay"
    );

    private QuickBattleKeys() {}
}