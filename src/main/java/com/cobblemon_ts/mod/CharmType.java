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