package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ShinyPokemonBlock extends BaseLuckyBlock {
    private static final Random RANDOM = new Random();

    public ShinyPokemonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        int minLevel = CharmConfig.MIN_SPAWN_LEVEL.get();
        int maxLevel = CharmConfig.MAX_SPAWN_LEVEL.get();
        int pokemonLevel = minLevel + RANDOM.nextInt(maxLevel - minLevel + 1);

        String command = String.format("spawnpokemon random lvl=%d shiny x=%d y=%d z=%d",
                pokemonLevel, pos.getX(), pos.getY() + 1, pos.getZ());

        CommandSourceStack source = new CommandSourceStack(
                level.getServer(), Vec3.atCenterOf(pos), Vec2.ZERO,
                level, 2, "LuckyBlock",
                Component.literal("LuckyBlock"), level.getServer(), null);

        level.getServer().getCommands().performPrefixedCommand(source, command);
        CobblemonTS.LOGGER.info("[LuckyBlock] 生成随机闪光宝可梦 Lv.{} at {}", pokemonLevel, pos);
    }
}