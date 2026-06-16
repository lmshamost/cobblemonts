package com.cobblemon_ts.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DebugConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_PARTY;
    public static final ModConfigSpec.BooleanValue DEBUG_CLICK;
    public static final ModConfigSpec.BooleanValue DEBUG_CHARM;
    public static final ModConfigSpec.BooleanValue DEBUG_SLOT;
    public static final ModConfigSpec.BooleanValue DEBUG_CONFIG;
    public static final ModConfigSpec.BooleanValue DEBUG_EDITOR;
    public static final ModConfigSpec.BooleanValue DEBUG_MOVES;
    public static final ModConfigSpec.BooleanValue DEBUG_POKEBAG;

    static {
        BUILDER.comment("调试设置（独立配置文件，按功效命名）");
        DEBUG_PARTY = BUILDER
                .comment("精灵栏渲染调试：面板底色、队伍信息、partyStores、slots")
                .define("debug_party", false);
        DEBUG_CLICK = BUILDER
                .comment("点击检测调试：mouseClicked 入口、inPartyArea 范围、右键菜单")
                .define("debug_click", false);
        DEBUG_CHARM = BUILDER
                .comment("护符栏调试：箭头渲染、箭头点击、抽屉切换、点击拦截")
                .define("debug_charm", false);
        DEBUG_SLOT = BUILDER
                .comment("槽位物品调试：左键点击、拖拽、使用物品、heldItem、mouseReleased")
                .define("debug_slot", false);
        DEBUG_CONFIG = BUILDER
                .comment("配置加载调试")
                .define("debug_config", false);
        DEBUG_EDITOR = BUILDER
                .comment("宝可梦编辑器调试：保存流程、技能读写、回发数据、UI刷新")
                .define("debug_editor", false);
        DEBUG_MOVES = BUILDER
                .comment("技能读写调试：MoveSet探测、按索引读写、PP/MaxPP/raised计算")
                .define("debug_moves", false);
        DEBUG_POKEBAG = BUILDER
                .comment("道具袋调试：容器创建、物品复制、setItem截断、堆叠上限、保存回写")
                .define("debug_pokebag", false);
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}