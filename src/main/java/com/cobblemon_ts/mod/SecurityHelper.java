// [线性-SEC01] C→S发包权限校验工具：仅创造模式或OP通过
package com.cobblemon_ts.mod;

import net.minecraft.server.network.ServerPlayerEntity;

public class SecurityHelper {

    // [线性-SEC01a] 玩家必须创造模式或OP(World>=2)
    public static boolean isTrusted(ServerPlayerEntity player) {
        return player.isInCreativeMode() || player.hasPermissionLevel(2);
    }
}