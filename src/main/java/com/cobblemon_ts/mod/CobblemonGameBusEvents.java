package com.cobblemon_ts.mod;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

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

@SubscribeEvent
public static void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
        CobblemonTS.clearLevelIndex(sl);
    }
}
}