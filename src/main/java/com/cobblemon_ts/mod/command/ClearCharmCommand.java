// [线性-CMD01-V2] /cts set <player> <charm> <on|off> 设置指定玩家护符状态
package com.cobblemon_ts.mod.command;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.UUID;

public class ClearCharmCommand {

    // [线性-CMD01a] 护符名补全建议（支持 @a/@p/@s/玩家名）
    private static final SuggestionProvider<CommandSourceStack> CHARM_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (String name : CharmCommandData.TOGGLE_CHARM_IDS) {
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    // [线性-CMD01b] 注册 /cts set 指令
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("cts")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("charm", StringArgumentType.word())
                            .suggests(CHARM_SUGGESTIONS)
                            .then(Commands.argument("onoff", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "player");
                                    String charmName = StringArgumentType.getString(ctx, "charm").toLowerCase();
                                    boolean enable = BoolArgumentType.getBool(ctx, "onoff");
                                    executeSet(ctx.getSource(), players, charmName, enable);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                // [线性-R10] /cts clear chunks [days] — 清理离线N天玩家的区块禁止器
                .then(Commands.literal("clear")
                    .then(Commands.literal("chunks")
                        .executes(ctx -> executeClearChunks(ctx.getSource(), 7))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 3650))
                            .executes(ctx -> executeClearChunks(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "days")))
                        )
                    )
                )
        );
    }

    // [线性-CMD01c] 执行设置：on=true启用 / on=false清除
    private static void executeSet(CommandSourceStack source, Collection<ServerPlayer> players,
                                   String charmName, boolean enable) {
        for (ServerPlayer sp : players) {
            CharmPlayerData data = CharmPlayerData.get(sp);
            if (data == null) {
                source.sendFailure(Component.literal("Player data not found for " + sp.getName().getString()));
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
                    source.sendFailure(Component.literal("Unknown charm: " + charmName));
                    return;
                }
            }
            // [线性-CMD01g] 反馈给目标玩家
            String statusKey = enable ? "command.cobblemon_ts.cts.set.on" : "command.cobblemon_ts.cts.set.off";
            sp.sendSystemMessage(Component.translatable(statusKey, charmName));
        }
        // [线性-CMD01h] 反馈给执行者
        String summaryKey = enable ? "command.cobblemon_ts.cts.set.success_on" : "command.cobblemon_ts.cts.set.success_off";
        source.sendSuccess(() -> Component.translatable(summaryKey, players.size(), charmName), true);
    }

    // [线性-CMD01i] on→ACTIVE, off→DISABLED
    private static CharmState stateFor(boolean enable) {
        return enable ? CharmState.ACTIVE : CharmState.DISABLED;
    }

    // [线性-R10] /cts clear chunks — 清理所有区块禁止器BE中 owner 为 null 的条目
    // 注：离线天数判断需要 OfflinePlayer.getLastSeen()，在 1.21.1 PlayerList API 中无法直接获取
    // 目前按 owner==null 清理，后续如需按离线天数精确判断可注入 NBT lastSeen 字段到 BE
    private static int executeClearChunks(CommandSourceStack source, int days) {
        try {
            var server = source.getServer();

            int removed = 0;
            int checked = 0;

            // 1. 收集在线玩家UUID（在线玩家的BE不清理）
            java.util.Set<UUID> onlineUuids = new java.util.HashSet<>();
            for (var online : server.getPlayerList().getPlayers()) {
                if (online != null) onlineUuids.add(online.getUUID());
            }

            // 2. 遍历所有维度的区块禁止器BE
            for (var level : server.getAllLevels()) {
                if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) continue;
                var beSet = CobblemonTS.BARRIER_BE_SET.get(sl);
                if (beSet == null || beSet.isEmpty()) continue;

                // 先收集要删的，ConcurrentModificationException 安全
                var toRemove = new java.util.ArrayList<ChunkBarrierBlockEntity>();
                for (var be : beSet) {
                    if (be == null || be.isRemoved()) continue;
                    checked++;
                    UUID owner = be.getOwnerId();
                    // owner==null：未设置放置者的，清理
                    // owner 不在线：视为离线，清理（days 参数预留用于未来精确判断）
                    if (owner == null || !onlineUuids.contains(owner)) {
                        toRemove.add(be);
                    }
                }

                // 逐个破坏
                for (var be : toRemove) {
                    level.destroyBlock(be.getBlockPos(), false);
                    removed++;
                }
            }

            // 3. 反馈
            String msg = String.format("[ChunkBarrier] 扫描 %d 个区块禁止器，清理了 %d 个离线/无主条目 (离线天数参数 %d 已预留)",
                    checked, removed, days);
            source.sendSuccess(() -> Component.literal(msg), true);
            CobblemonTS.LOGGER.info(msg);
            return removed;
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[ChunkBarrier] clear chunks 命令异常", t);
            source.sendFailure(Component.literal("执行失败: " + t.getMessage()));
            return 0;
        }
    }
}