package com.cobblemon_ts.mod.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.List;

public class ChunkBarrierEntityInterceptor {

    public static boolean shouldBlockSpawn(EntityType<?> entityType, int chunkX, int chunkZ,
                                            List<ChunkBarrierBlockEntity> barriers) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) return false;

        boolean isPokemon = key.getNamespace().equals("cobblemon") && key.getPath().equals("pokemon");
        boolean isNPC = key.getNamespace().equals("cobblemon") && key.getPath().equals("npc");
        MobCategory cat = entityType.getCategory();
        boolean isHostile = cat == MobCategory.MONSTER;
        boolean isPassive = cat == MobCategory.CREATURE
                || cat == MobCategory.AMBIENT
                || cat == MobCategory.WATER_CREATURE
                || cat == MobCategory.WATER_AMBIENT
                || cat == MobCategory.UNDERGROUND_WATER_CREATURE
                || cat == MobCategory.AXOLOTLS;

        for (ChunkBarrierBlockEntity be : barriers) {
            int flags = be.getConfig().getFlags(chunkX, chunkZ);
            if (flags == 0) continue;

            if (isPokemon && (flags & ChunkBarrierConfig.BIT_POKEMON) != 0) return true;
            if (isNPC && (flags & ChunkBarrierConfig.BIT_NPC) != 0) return true;
            if (isHostile && (flags & ChunkBarrierConfig.BIT_HOSTILE) != 0) return true;
            if (isPassive && (flags & ChunkBarrierConfig.BIT_PASSIVE) != 0) return true;
        }
        return false;
    }
}