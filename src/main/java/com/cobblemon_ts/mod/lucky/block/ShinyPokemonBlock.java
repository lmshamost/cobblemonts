package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class ShinyPokemonBlock extends BaseLuckyBlock {

    public ShinyPokemonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this);
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);

        int minLevel = CharmConfig.MIN_SPAWN_LEVEL.get();
        int maxLevel = CharmConfig.MAX_SPAWN_LEVEL.get();
        int pokemonLevel = minLevel + ThreadLocalRandom.current().nextInt(maxLevel - minLevel + 1);
        String species;

        if (config != null) {
            // [线性-E01] 有配置 → 保底或加权随机
            LuckyWeightManager.WeightEntry<String> entry = LuckyWeightManager.checkGuarantee(config);
            if (entry == null) {
                entry = LuckyWeightManager.selectWeighted(config, getPlayerLuck(player));
            }
            if (entry != null) {
                String selected = LuckyWeightManager.randomEntry(entry);
                species = selected != null ? selected : "random";
            } else {
                species = "random";
            }
        } else {
            species = "random";
        }

        String command = String.format("spawnpokemon %s lvl=%d shiny x=%d y=%d z=%d",
                species, pokemonLevel, pos.getX(), pos.getY() + 1, pos.getZ());

        CommandSourceStack source = new CommandSourceStack(
                level.getServer(), Vec3.atCenterOf(pos), Vec2.ZERO,
                level, 2, "LuckyBlock",
                Component.literal("LuckyBlock"), level.getServer(), null);

        level.getServer().getCommands().performPrefixedCommand(source, command);
        CobblemonTS.LOGGER.info("[LuckyBlock] 生成随机闪光宝可梦 {} Lv.{} at {}", species, pokemonLevel, pos);
    }
}