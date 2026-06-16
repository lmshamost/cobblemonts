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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LegendaryPokemonBlock extends BaseLuckyBlock {

    public LegendaryPokemonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this);
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);

        String species;
        if (config != null) {
            // [线性-C01] 有配置 → 保底或加权随机
            LuckyWeightManager.WeightEntry<String> entry = LuckyWeightManager.checkGuarantee(config);
            if (entry == null) {
                entry = LuckyWeightManager.selectWeighted(config, getPlayerLuck(player));
            }
            if (entry != null) {
                species = LuckyWeightManager.randomEntry(entry);
            } else {
                species = null;
            }
        } else {
            // [线性-C02] 无配置 → 旧行为: CharmConfig列表均匀随机
            List<? extends String> legendaries = CharmConfig.LEGENDARY_POKEMON.get();
            if (legendaries.isEmpty()) {
                CobblemonTS.LOGGER.warn("[LuckyBlock] 传说宝可梦列表为空!");
                return;
            }
            species = legendaries.get(ThreadLocalRandom.current().nextInt(legendaries.size()));
        }

        if (species == null) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] 没有可选传说宝可梦!");
            return;
        }

        int pokemonLevel = 50 + ThreadLocalRandom.current().nextInt(21);
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