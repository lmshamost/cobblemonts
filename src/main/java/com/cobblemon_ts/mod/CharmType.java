package com.cobblemon_ts.mod;

public enum CharmType {
    SHINY("shiny_charm", "闪耀护符"),
    OVAL("oval_charm", "圆形护符"),
    EXP("exp_charm", "经验护符"),
    CATCHING("catching_charm", "防晃护符"),
    EXP_ALL("exp_all", "高级学习装置");
    
    private final String itemId;
    private final String displayName;
    
    CharmType(String itemId, String displayName) {
        this.itemId = itemId;
        this.displayName = displayName;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    // [线性-L10] 玩家可见的本地化护符名（复用物品翻译键 item.coblemon_ts.<itemId>）
    public net.minecraft.network.chat.Component getDisplayComponent() {
        return net.minecraft.network.chat.Component.translatable("item.coblemon_ts." + itemId);
    }
    
    public net.minecraft.world.item.Item getItem() {
        return switch (this) {
            case SHINY -> CharmItems.SHINY_CHARM.get();
            case OVAL -> CharmItems.OVAL_CHARM.get();
            case EXP -> CharmItems.EXP_CHARM.get();
            case CATCHING -> CharmItems.CATCHING_CHARM.get();
            case EXP_ALL -> CharmItems.EXP_ALL.get();
        };
    }
}