package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.lucky.LuckyConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class LegendaryPokemonBlock extends BaseLuckyBlock {
    private static final Random RANDOM = new Random();

    public LegendaryPokemonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        List<? extends String> legendaries = LuckyConfig.LEGENDARY_POKEMON.get();
        if (legendaries.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] 传说宝可梦列表为空!");
            return;
        }

        String species = legendaries.get(RANDOM.nextInt(legendaries.size()));
        int pokemonLevel = 50 + RANDOM.nextInt(21);

        String command = String.format("spawnpokemon %s lvl=%d x=%d y=%d z=%d",
                normalizeSpeciesName(species), pokemonLevel, pos.getX(), pos.getY() + 1, pos.getZ());

        CommandSourceStack source = new CommandSourceStack(
                level.getServer(), Vec3.atCenterOf(pos), Vec2.ZERO,
                level, 2, "LuckyBlock",
                Component.literal("LuckyBlock"), level.getServer(), null);

        level.getServer().getCommands().performPrefixedCommand(source, command);
        CobblemonTS.LOGGER.info("[LuckyBlock] 生成传说宝可梦 {} Lv.{} at {}", species, pokemonLevel, pos);
    }

    private String normalizeSpeciesName(String name) {
        return name.toLowerCase().replace(" ", "").replace("-", "");
    }
}