package com.cobblemon_ts.mod;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = CobblemonTS.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CobblemonGameBusEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        CobblemonTS.onEntityJoinLevel(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMobSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        CobblemonTS.onMobSpawnPositionCheck(event);
    }
}