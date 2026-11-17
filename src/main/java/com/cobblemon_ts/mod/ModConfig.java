package com.cobblemon_ts.mod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private final String filename;
    private JsonObject json;

    public ModConfig(String filename) {
        this.filename = filename;
        this.json = new JsonObject();
    }

    public void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(filename);
        if (Files.exists(configFile)) {
            try {
                String content = Files.readString(configFile);
                json = JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("Failed to load config {}", filename, e);
                json = new JsonObject();
            }
        }
    }

    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(filename);
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, json.toString());
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to save config {}", filename, e);
        }
    }

    public JsonObject getJson() {
        return json;
    }

    // 类型安全读取
    public double getDouble(String key, double defaultValue) {
        if (json.has(key)) return json.get(key).getAsDouble();
        json.addProperty(key, defaultValue);
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        if (json.has(key)) return json.get(key).getAsInt();
        json.addProperty(key, defaultValue);
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (json.has(key)) return json.get(key).getAsBoolean();
        json.addProperty(key, defaultValue);
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        if (json.has(key)) return json.get(key).getAsString();
        json.addProperty(key, defaultValue);
        return defaultValue;
    }

    /** [线性-WRAP] 可变int包装器，兼容NeoForge ConfigValue<Integer>.get()/.set() */
    public static class IntWrapper {
        private int v;
        public IntWrapper(int v) { this.v = v; }
        public int get() { return v; }
        public void set(int v) { this.v = v; }
    }
}