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

    private QuickBattleKeys() {}
}