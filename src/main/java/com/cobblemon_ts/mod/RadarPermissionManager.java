// [线性-RDR2] 雷达权限管理器：服务端权威 + 名单持久化 + 登录/命令变更实时推送
//  判定：总开关(cobblemon_ts_global.json) && 个体权限(黑名单：名单内禁止，缺省允许)
//  [线性-RDR3] 名单存于 cobblemon_ts_global.json 的 radar_blacklist（与总开关同分组，指令即时读写生效）
package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.network.PacketDistributor;
import com.cobblemon_ts.mod.network.SyncRadarPermissionS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class RadarPermissionManager {

    // [线性-RDR2c] 服务端权威判定：总开关 && 非黑名单（黑名单内禁止，缺省允许）
    public static boolean canUseRadar(ServerPlayerEntity player) {
        if (!CobblemonTSConfig.POKEMON_RADAR_ENABLED.get()) return false;
        return !isBlacklisted(player.getGameProfile().getName());
    }

    // [线性-RDR2d] 命令变更：更新黑名单配置 -> 立即持久化 -> 实时推送目标玩家
    public static void setRadarPermission(ServerPlayerEntity player, boolean allowed) {
        String name = player.getGameProfile().getName();
        List<String> list = new ArrayList<>(CobblemonTSConfig.POKEMON_RADAR_BLACKLIST.get());
        if (allowed) {
            // 允许使用：从黑名单移除
            list.removeIf(entry -> entry != null && entry.equalsIgnoreCase(name));
        } else {
            // 禁止使用：加入黑名单（去重）
            boolean exists = list.stream().anyMatch(entry -> entry != null && entry.equalsIgnoreCase(name));
            if (!exists) list.add(name);
        }
        CobblemonTSConfig.POKEMON_RADAR_BLACKLIST.set(list);
        CobblemonTSConfig.save();
        PacketDistributor.sendToPlayer(player, new SyncRadarPermissionS2CPacket(allowed));
    }

    // [线性-RDR2e] 玩家登录：下发一次当前权限结果
    public static void onPlayerJoin(ServerPlayerEntity player) {
        PacketDistributor.sendToPlayer(player, new SyncRadarPermissionS2CPacket(canUseRadar(player)));
    }

    // [线性-RDR4] /cts radar reload：重新读取名单，向所有在线玩家重推当前权限（手动改配置文件后无需重启/重登）
    public static int resyncAllRadarPermissions(MinecraftServer server) {
        int count = 0;
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            // canUseRadar 内部每次都现取 POKEMON_RADAR_BLACKLIST.get()，即"重读"最新名单
            PacketDistributor.sendToPlayer(sp, new SyncRadarPermissionS2CPacket(canUseRadar(sp)));
            count++;
        }
        return count;
    }

    // [线性-RDR3] 玩家是否在黑名单内（大小写不敏感）
    private static boolean isBlacklisted(String name) {
        for (String entry : CobblemonTSConfig.POKEMON_RADAR_BLACKLIST.get()) {
            if (entry != null && entry.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}