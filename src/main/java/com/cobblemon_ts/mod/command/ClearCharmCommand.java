// [线性-CMD01-V2] /cts set <PlayerEntity> <charm> <on|off> 设置指定玩家护符状态
package com.cobblemon_ts.mod.command;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.RadarPermissionManager;
import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.network.OpenHudScreenS2CPacket;
import com.cobblemon_ts.mod.network.PacketDistributor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;


import java.util.Collection;
import java.util.UUID;

public class ClearCharmCommand {

    // [线性-CMD01a] 护符名补全建议（支持 @a/@p/@s/玩家名）
    private static final SuggestionProvider<ServerCommandSource> CHARM_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (String name : CharmCommandData.TOGGLE_CHARM_IDS) {
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        // [线性-RADAR-PERM-CMD] 雷达权限：非护符，独立补全建议
        if ("radar".startsWith(remaining)) {
            builder.suggest("radar");
        }
        return builder.buildFuture();
    };

    // [线性-CMD01b] 注册 /cts set 指令
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
            CommandManager.literal("cts")
                .requires(src -> true)
                .then(CommandManager.literal("set")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("player", EntityArgumentType.players())
                        .then(CommandManager.argument("charm", StringArgumentType.word())
                            .suggests(CHARM_SUGGESTIONS)
                            .then(CommandManager.argument("onoff", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "player");
                                    String charmName = StringArgumentType.getString(ctx, "charm").toLowerCase();
                                    boolean enable = BoolArgumentType.getBool(ctx, "onoff");
                                    executeSet(ctx.getSource(), players, charmName, enable);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                // [线性-CMD02a] 注册 /cts clear chunks [days] 子指令
                .then(CommandManager.literal("clear")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.literal("chunks")
                        .executes(ctx -> executeClearChunks(ctx.getSource(), 7))
                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 3650))
                            .executes(ctx -> executeClearChunks(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "days")))
                        )
                    )
                )
                // [线性-CMD-HUD] 注册 /cts hud 子指令：服务端命令触发，网络包通知客户端打开HUD设置屏
                .then(CommandManager.literal("hud")
                    .executes(ctx -> executeOpenHud(ctx.getSource()))
                )
        );
    }

    // [线性-CMD-HUD] 执行 /cts hud：向玩家发送打开HUD设置屏的网络包
    private static int executeOpenHud(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("该指令只能由玩家执行"));
                return 0;
            }
            PacketDistributor.sendToPlayer(player, new OpenHudScreenS2CPacket());
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("打开HUD设置失败: " + e.getMessage()));
            return 0;
        }
    }

    // [线性-CMD02b] 执行 /cts clear chunks：清理无主或离线的区块禁止器
    private static int executeClearChunks(ServerCommandSource source, int days) {
        try {
            var server = source.getServer();
            int removed = 0;
            int checked = 0;

            // 1. 收集在线玩家UUID（在线玩家的BE不清理）
            java.util.Set<UUID> onlineUuids = new java.util.HashSet<>();
            for (var online : server.getPlayerManager().getPlayerList()) {
                if (online != null) onlineUuids.add(online.getUuid());
            }

            // 2. 遍历所有维度的区块禁止器BE
            for (ServerWorld level : server.getWorlds()) {
                var beSet = CobblemonTS.BARRIER_BE_SET.get(level);
                if (beSet == null || beSet.isEmpty()) continue;

                // 先收集要删的，避免并发修改
                var toRemove = new java.util.ArrayList<ChunkBarrierBlockEntity>();
                for (ChunkBarrierBlockEntity be : beSet) {
                    if (be == null || be.isRemoved()) continue;
                    checked++;
                    UUID owner = be.getOwnerId();
                    if (owner == null || !onlineUuids.contains(owner)) {
                        toRemove.add(be);
                    }
                }

                for (ChunkBarrierBlockEntity be : toRemove) {
                    level.breakBlock(be.getPos(), false);
                    removed++;
                }
            }

            String msg = String.format("[ChunkBarrier] 扫描 %d 个区块禁止器，清理了 %d 个离线/无主条目 (离线天数参数 %d 已预留)",
                    checked, removed, days);
            source.sendMessage(Text.literal(msg));
            CobblemonTS.LOGGER.info(msg);
            return removed;
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[ChunkBarrier] clear chunks 命令异常", t);
            source.sendMessage(Text.literal("执行失败: " + t.getMessage()));
            return 0;
        }
    }

    // [线性-CMD01c] 执行设置：on=true启用 / on=false清除
    private static void executeSet(ServerCommandSource source, Collection<ServerPlayerEntity> players,
                                   String charmName, boolean enable) {
        // [线性-RADAR-PERM-CMD] 雷达权限独立处理：不属于护符，走 RadarPermissionManager
        if ("radar".equals(charmName)) {
            for (ServerPlayerEntity sp : players) {
                RadarPermissionManager.setRadarPermission(sp, enable);
            }
            String statusKey = enable ? "command.cobblemon_ts.cts.radar.on" : "command.cobblemon_ts.cts.radar.off";
            for (ServerPlayerEntity sp : players) {
                sp.sendMessage(Text.translatable(statusKey));
            }
            String summaryKey = enable ? "command.cobblemon_ts.cts.radar.success_on" : "command.cobblemon_ts.cts.radar.success_off";
            source.sendFeedback(() -> Text.translatable(summaryKey, players.size()), true);
            return;
        }
        for (ServerPlayerEntity sp : players) {
            CharmPlayerData data = CharmPlayerData.get(sp);
            if (data == null) {
                source.sendMessage(Text.literal("PlayerEntity data not found for " + sp.getName().getString()));
                continue;
            }
            CharmSlotManager manager = data.getCharmSlotManager();

            // [线性-CMD01d] 根据护符名分发
            switch (charmName) {
                case "shiny_charm"    -> manager.setCharmState(CharmType.SHINY,     stateFor(enable));
                case "oval_charm"     -> manager.setCharmState(CharmType.OVAL,      stateFor(enable));
                case "exp_charm"      -> manager.setCharmState(CharmType.EXP,       stateFor(enable));
                case "catching_charm" -> manager.setCharmState(CharmType.CATCHING,  stateFor(enable));
                case "exp_all"        -> manager.setCharmState(CharmType.EXP_ALL,   stateFor(enable));
                case "pokebag"        -> manager.setPokebagOwned(enable);
                // [线性-CMD01f] all：全部护符状态 + 道具袋（金冠有独立获取途径，不归指令管）
                case "all" -> {
                    CharmState s = stateFor(enable);
                    for (CharmType type : CharmType.values()) {
                        manager.setCharmState(type, s);
                    }
                    manager.setPokebagOwned(enable);
                }
                default -> {
                    source.sendMessage(Text.literal("Unknown charm: " + charmName));
                    return;
                }
            }
            // [线性-CMD01g] 反馈给目标玩家
            String statusKey = enable ? "command.cobblemon_ts.cts.set.on" : "command.cobblemon_ts.cts.set.off";
            sp.sendMessage(Text.translatable(statusKey, charmName));
        }
        // [线性-CMD01h] 反馈给执行者
        String summaryKey = enable ? "command.cobblemon_ts.cts.set.success_on" : "command.cobblemon_ts.cts.set.success_off";
        source.sendFeedback(() -> Text.translatable(summaryKey, players.size(), charmName), true);
    }

    // [线性-CMD01i] on→ACTIVE, off→DISABLED
    private static CharmState stateFor(boolean enable) {
        return enable ? CharmState.ACTIVE : CharmState.DISABLED;
    }
}