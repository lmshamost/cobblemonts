package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.resource.featuretoggle.FeatureSet;

public class ModContainers {
    public static final ScreenHandlerType<PartyHeldItemContainer> PARTY_HELD_ITEM =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(CobblemonTS.MODID, "party_held_item"),
                    new ScreenHandlerType<>(PartyHeldItemContainer::new, FeatureSet.empty()));

    public static final ScreenHandlerType<PokebagContainer> POKEBAG =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(CobblemonTS.MODID, "pokebag"),
                    new ScreenHandlerType<>(PokebagContainer::new, FeatureSet.empty()));

    // [线性-CB] 区块禁止器容器（Fabric 扩展屏幕机制同步配置）
    public static final ExtendedScreenHandlerType<ChunkBarrierContainer, ChunkBarrierSyncData> CHUNK_BARRIER =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(CobblemonTS.MODID, "chunk_barrier"),
                    new ExtendedScreenHandlerType<>((id, inv, data) -> new ChunkBarrierContainer(id, inv, data), ChunkBarrierSyncData.PACKET_CODEC));
}