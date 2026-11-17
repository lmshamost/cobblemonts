package com.cobblemon_ts.mod;

/*=================================================================
||  线性依赖: DebugConfig提供调试开关
||  线性结构: 8个独立布尔开关
||  线性下游: 各MixIn/UI绘制前先检查对应开关
===================================================================*/

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class DebugConfig {
    public static final CharmConfig.BooleanValue DEBUG_PARTY   = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_CLICK   = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_CHARM   = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_SLOT    = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_CONFIG  = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_EDITOR  = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_MOVES   = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue DEBUG_POKEBAG = new CharmConfig.BooleanValue(false);

    static { load(); }

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("cobblemonts/cobblemon_ts_debug.json");
        if (!Files.exists(configFile)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();
            if (json.has("debug_party"))   DEBUG_PARTY.v   = json.get("debug_party").getAsBoolean();
            if (json.has("debug_click"))   DEBUG_CLICK.v   = json.get("debug_click").getAsBoolean();
            if (json.has("debug_charm"))   DEBUG_CHARM.v   = json.get("debug_charm").getAsBoolean();
            if (json.has("debug_slot"))    DEBUG_SLOT.v    = json.get("debug_slot").getAsBoolean();
            if (json.has("debug_config"))  DEBUG_CONFIG.v  = json.get("debug_config").getAsBoolean();
            if (json.has("debug_editor"))  DEBUG_EDITOR.v  = json.get("debug_editor").getAsBoolean();
            if (json.has("debug_moves"))   DEBUG_MOVES.v   = json.get("debug_moves").getAsBoolean();
            if (json.has("debug_pokebag")) DEBUG_POKEBAG.v = json.get("debug_pokebag").getAsBoolean();
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to load debug config", e);
        }
    }
}