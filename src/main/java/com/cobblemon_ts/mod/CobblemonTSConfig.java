package com.cobblemon_ts.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CobblemonTSConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 初始精灵设置 ====================
    public static final ModConfigSpec.BooleanValue STARTER_IS_SHINY;
    public static final ModConfigSpec.BooleanValue STARTER_PERFECT_IVS;
    public static final ModConfigSpec.BooleanValue INITIAL_POKEMON_SHINY;
    public static final ModConfigSpec.BooleanValue INITIAL_POKEMON_MAX_IV;

    // ==================== 努力值设置 ====================
    public static final ModConfigSpec.BooleanValue ENABLE_UNLIMITED_EVS;
    public static final ModConfigSpec.BooleanValue EV_EFFORT_CAP_252X6;

    // ==================== HUD 显示设置 ====================
    public static final ModConfigSpec.BooleanValue SHOW_EFFECT_ICONS;
    public static final ModConfigSpec.BooleanValue SHOW_INVENTORY_EFFECT_ICONS;
    public static final ModConfigSpec.BooleanValue HIDE_TOGGLE_BUTTONS;

    static {
        BUILDER.comment("初始精灵设置").push("starter");
        STARTER_IS_SHINY = BUILDER
                .comment("初始精灵是否为闪光(true=闪光, false=正常)")
                .define("starter_is_shiny", false);
        STARTER_PERFECT_IVS = BUILDER
                .comment("初始精灵是否为满6V(true=满6V, false=随机)")
                .define("starter_perfect_ivs", false);
        INITIAL_POKEMON_SHINY = STARTER_IS_SHINY;
        INITIAL_POKEMON_MAX_IV = STARTER_PERFECT_IVS;
        BUILDER.pop();

        BUILDER.comment("努力值设置").push("ev");
        ENABLE_UNLIMITED_EVS = BUILDER
                .comment("是否开启努力值上限为252*6(true=开启, false=原版限制)")
                .define("enable_unlimited_evs", false);
        EV_EFFORT_CAP_252X6 = ENABLE_UNLIMITED_EVS;
        BUILDER.pop();

        BUILDER.comment("HUD 显示设置").push("hud");
        SHOW_EFFECT_ICONS = BUILDER
                .comment("是否在屏幕右上角显示药水buff图标(true=显示, false=隐藏)")
                .define("show_effect_icons", true);
        SHOW_INVENTORY_EFFECT_ICONS = BUILDER
                .comment("是否在背包界面显示药水buff图标(true=显示, false=隐藏)")
                .define("show_inventory_effect_icons", true);
        HIDE_TOGGLE_BUTTONS = BUILDER
                .comment("是否隐藏背包精灵栏和护符栏的开关按钮(true=开关按钮变透明不可见, false=显示绿红小方块正常显示)")
                .define("hide_toggle_buttons", false);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** 获取努力值上限（供宝可梦编辑器使用）：开启 = 252*6=1512，关闭 = 原版 510 */
    public static int getEVTotalCap() {
        return Boolean.TRUE.equals(EV_EFFORT_CAP_252X6.get()) ? 1512 : 510;
    }
}