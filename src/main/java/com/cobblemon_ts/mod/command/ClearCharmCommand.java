// [线性-CMD01-V2] /cts set <player> <charm> <on|off> 设置指定玩家护符状态
package com.cobblemon_ts.mod.command;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

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
            String statusKey = enable ? "command.coblemon_ts.cts.set.on" : "command.coblemon_ts.cts.set.off";
            sp.sendSystemMessage(Component.translatable(statusKey, charmName));
        }
        // [线性-CMD01h] 反馈给执行者
        String summaryKey = enable ? "command.coblemon_ts.cts.set.success_on" : "command.coblemon_ts.cts.set.success_off";
        source.sendSuccess(() -> Component.translatable(summaryKey, players.size(), charmName), true);
    }

    // [线性-CMD01i] on→ACTIVE, off→DISABLED
    private static CharmState stateFor(boolean enable) {
        return enable ? CharmState.ACTIVE : CharmState.DISABLED;
    }
}