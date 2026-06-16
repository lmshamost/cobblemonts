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

import java.util.List;
import java.util.Random;

public class UltraBeastBlock extends BaseLuckyBlock {
    private static final Random RANDOM = new Random();

    public UltraBeastBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        List<? extends String> ultraBeasts = CharmConfig.ULTRA_BEAST_POKEMON.get();
        if (ultraBeasts.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] 究极异兽列表为空!");
            return;
        }

        String species = ultraBeasts.get(RANDOM.nextInt(ultraBeasts.size()));
        int pokemonLevel = 45 + RANDOM.nextInt(16);

        String command = String.format("spawnpokemon %s lvl=%d x=%d y=%d z=%d",
                normalizeSpeciesName(species), pokemonLevel, pos.getX(), pos.getY() + 1, pos.getZ());

        CommandSourceStack source = new CommandSourceStack(
                level.getServer(), Vec3.atCenterOf(pos), Vec2.ZERO,
                level, 2, "LuckyBlock",
                Component.literal("LuckyBlock"), level.getServer(), null);

        level.getServer().getCommands().performPrefixedCommand(source, command);
        CobblemonTS.LOGGER.info("[LuckyBlock] 生成究极异兽 {} Lv.{} at {}", species, pokemonLevel, pos);
    }

    private String normalizeSpeciesName(String name) {
        return name.toLowerCase().replace(" ", "").replace("-", "");
    }
}