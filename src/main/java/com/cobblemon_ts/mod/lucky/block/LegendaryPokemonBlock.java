package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LegendaryPokemonBlock extends BaseLuckyBlock {

    public LegendaryPokemonBlock(AbstractBlock.Settings properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerWorld world, BlockPos pos, PlayerEntity player) {
        // [线性-R10] 区块禁止器：传说幸运方块禁止
        long ck = CobblemonTS.chunkKeyOf(pos.getX(), pos.getZ());
        if (CobblemonTS.isLuckyBlockBlocked(world, ck, CobblemonTS.LuckyType.LEGENDARY, player != null ? player.getName().getString().toLowerCase() : null)) return;

        Identifier blockId = Registries.BLOCK.getId(this);
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);

        String species;
        if (config != null) {
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
            List<? extends String> legendaries = CharmConfig.LEGENDARY_POKEMON.get();
            if (legendaries.isEmpty()) {
                CobblemonTS.LOGGER.warn("[LuckyBlock] Legendary list is empty!");
                return;
            }
            species = legendaries.get(ThreadLocalRandom.current().nextInt(legendaries.size()));
        }

        if (species == null) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] No legendary pokemon available!");
            return;
        }

        int pokemonLevel = 50 + ThreadLocalRandom.current().nextInt(21);
        String command = String.format("spawnpokemon %s lvl=%d x=%d y=%d z=%d",
                normalizeSpeciesName(species), pokemonLevel, pos.getX(), pos.getY() + 1, pos.getZ());

        ServerCommandSource source = new ServerCommandSource(
                world.getServer(), Vec3d.ofCenter(pos), Vec2f.ZERO,
                world, 2, "LuckyBlock",
                Text.literal("LuckyBlock"), world.getServer(), null);

        // [线性-R10a] 幸运方块豁免：同步命令窗口内放行，避免被区块禁止器实体拦截误杀
        CobblemonTS.beginLuckySpawn();
        try {
            world.getServer().getCommandManager().executeWithPrefix(source, command);
        } finally {
            CobblemonTS.endLuckySpawn();
        }
        CobblemonTS.LOGGER.info("[LuckyBlock] Spawned legendary {} Lv.{} at {}", species, pokemonLevel, pos);
    }

    private String normalizeSpeciesName(String name) {
        return name.toLowerCase().replace(" ", "").replace("-", "");
    }
}