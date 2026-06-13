package com.cobblemon_ts.mod;

public enum CharmState {
    DISABLED,
    AVAILABLE,
    ACTIVE;
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean isAvailable() {
        return this == AVAILABLE || this == ACTIVE;
    }
    
    public boolean isDisabled() {
        return this == DISABLED;
    }
}