// [线性-RADAR-PERM] 雷达权限管理器（服务端权威）
// 总开关 pokemon_radar_enabled（改后重启）：false 时所有玩家（含离线）一律禁止，这是唯一的"默认所有人"开关。
// 个体权限 cts set <玩家> radar t/f：只针对被指定的玩家，持久化到服务端 radar_permissions.json，离线再上线仍生效。
// 性能：只在登录下发一次、命令变更推送一次，客户端按键读缓存，不持续检测。
package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.network.PacketDistributor;
import com.cobblemon_ts.mod.network.SyncRadarPermissionS2CPacket;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadarPermissionManager {

    // [线性-RADAR-PERM] 玩家个体权限表：玩家名 -> 是否允许（默认 true，仅存被命令/手动设置过的玩家）
    private static final Map<String, Boolean> PLAYER_RADAR_ALLOWED = new ConcurrentHashMap<>();

    // [线性-RADAR-PERM] 名单持久化文件（服务端 config/cobblemonts/radar_permissions.json）
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("cobblemonts").resolve("radar_permissions.json");

    // [线性-RADAR-PERM-LOAD] 启动加载持久化名单
    public static void load() {
        PLAYER_RADAR_ALLOWED.clear();
        if (!Files.exists(CONFIG_FILE)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(CONFIG_FILE)).getAsJsonObject();
            json.entrySet().forEach(e ->
                    PLAYER_RADAR_ALLOWED.put(e.getKey(), e.getValue().getAsBoolean()));
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to load radar permissions", e);
        }
    }

    // [线性-RADAR-PERM-SAVE] 保存持久化名单
    public static void save() {
        JsonObject json = new JsonObject();
        PLAYER_RADAR_ALLOWED.forEach((name, allowed) -> json.addProperty(name, allowed));
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, json.toString());
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to save radar permissions", e);
        }
    }

    // [线性-RADAR-PERM] 服务端权威判定：总开关（默认所有人，重启生效） && 玩家个体名单
    public static boolean canUseRadar(ServerPlayerEntity player) {
        if (!CobblemonTSConfig.POKEMON_RADAR_ENABLED.get()) return false;
        return PLAYER_RADAR_ALLOWED.getOrDefault(player.getGameProfile().getName(), true);
    }

    // [线性-RADAR-PERM-CMD] 命令设置：更新名单 + 持久化 + 立即推送给目标玩家
    public static void setRadarPermission(ServerPlayerEntity player, boolean allowed) {
        PLAYER_RADAR_ALLOWED.put(player.getGameProfile().getName(), allowed);
        save();
        PacketDistributor.sendToPlayer(player, new SyncRadarPermissionS2CPacket(allowed));
    }

    // [线性-RADAR-PERM] 登录下发：把服务端权威结果发给刚进服的玩家
    public static void onPlayerJoin(ServerPlayerEntity player) {
        PacketDistributor.sendToPlayer(player, new SyncRadarPermissionS2CPacket(canUseRadar(player)));
    }
}