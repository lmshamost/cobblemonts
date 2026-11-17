package com.cobblemon_ts.mod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;


import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;

import com.cobblemon_ts.mod.network.SetHeldItemPacket;
import com.cobblemon_ts.mod.network.UseItemOnPokemonPacket;
import com.cobblemon_ts.mod.network.UsePokebagItemPacket;
import com.cobblemon_ts.mod.network.PartyToPCPacket;
import com.cobblemon_ts.mod.network.ReleasePokemonPacket;
import com.cobblemon_ts.mod.network.SwapPartySlotsPacket;
import com.cobblemon_ts.mod.network.OpenHeldItemPacket;
import com.cobblemon_ts.mod.network.OpenPokebagPacket;
import com.cobblemon_ts.mod.network.ClearAllCharmsPacket;
import com.cobblemon_ts.mod.network.TestBuildOpenStarterPacket;
import com.cobblemon_ts.mod.network.ThrowPokeBallPacket;
import com.cobblemon_ts.mod.network.RequestPokebagContentsPacket;
import com.cobblemon_ts.mod.network.SyncPokebagContentsPacket;
import com.cobblemon_ts.mod.network.UpdateCharmStatePacket;
import com.cobblemon_ts.mod.network.RequestCharmStatesPacket;
import com.cobblemon_ts.mod.network.SyncCharmStatesS2CPacket;
import com.cobblemon_ts.mod.command.ClearCharmCommand;
import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import com.cobblemon_ts.mod.network.SyncConfigPayload;
import com.cobblemon_ts.mod.network.RequestChunkCountPacket;
import com.cobblemon_ts.mod.network.SyncChunkCountS2CPacket;
import com.cobblemon_ts.mod.network.OpenHudScreenS2CPacket;
import com.cobblemon_ts.mod.network.SyncRadarPermissionS2CPacket;
import com.cobblemon_ts.mod.lucky.LuckyBlocks;
import com.cobblemon_ts.mod.lucky.LuckyRecipeCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import com.cobblemon_ts.mod.event.PokemonEntityInteractHandler;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorItems;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorNetwork;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorClientHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonSentEvent;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CobblemonTS implements ModInitializer {
    public static final String MODID = "cobblemon_ts";
    public static final Logger LOGGER = LogUtils.getLogger();

    // [线性-R10] 区块禁止器全局索引：维度 → 区块 → 聚合规则位标志
    public static final Map<ServerWorld, Map<Long, Integer>> BARRIER_INDEX = new java.util.HashMap<>();
    public static final Map<ServerWorld, java.util.Set<ChunkBarrierBlockEntity>> BARRIER_BE_SET = new java.util.HashMap<>();
    // [线性-R10] 维度 → 区块 → 覆盖该区块的 BE 列表（查幸运方块禁止/玩家黑白名单）
    public static final Map<ServerWorld, Map<Long, List<ChunkBarrierBlockEntity>>> CHUNK_BE_MAP = new java.util.HashMap<>();

    // [线性-R10] 幸运方块放行：同步命令窗口内的深度计数（int 原生、无装箱、无集合）
    private static int luckySpawnDepth = 0;
    // [线性-R10] 每tick计数器：20tick(每秒)检测一次手持幸运方块提示
    private static int luckyHandTickCounter = 0;

    // [线性-R09] 区块禁止器方块实体类型（在 onInitialize 中注册）
    public static BlockEntityType<ChunkBarrierBlockEntity> CHUNK_BARRIER_BE;

    // [线性-L07] 反射缓存：每tick高频调用，避免GC压力
    static Class<?> cachedStorageClass;
    static Object cachedStorageInstance;
    static java.lang.reflect.Field cachedPartyStoresField;
    static java.lang.reflect.Field cachedPartyField;
    static boolean tickCacheInitialized;

    @Override
    public void onInitialize() {
        // 配置加载（替代NeoForge ModConfigEvent）
        CharmConfig.load();
        CobblemonTSConfig.load();
        DebugConfig.load();
        ColorEditorConfig.load();

        // 注册方块的初始化（静态块自动执行）
        LuckyBlocks.init();
        PokemonEditorItems.init();
        CharmItems.init();

        // [线性-R09] 区块禁止器方块实体类型注册
        CHUNK_BARRIER_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MODID, "chunk_barrier"),
                BlockEntityType.Builder.create(ChunkBarrierBlockEntity::new, LuckyBlocks.CHUNK_BARRIER.get()).build());

        // [线性-L06] 幸运方块配方条件注册（对应NeoForge CONDITION_SERIALIZERS）
        ResourceConditions.register(LuckyRecipeCondition.TYPE);

        // [线性-TAB] 独立创造栏页（对应NeoForge charm_tab，位于COMBAT前）
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MODID, "charm_tab"),
            ItemGroup.create(ItemGroup.Row.TOP, 8)
                .displayName(Text.translatable("itemGroup.cobblemon_ts"))
                .icon(() -> new ItemStack(CharmItems.SHINY_CHARM.get()))
                .entries((displayContext, entries) -> {
                    entries.add(CharmItems.OVAL_CHARM.get());
                    entries.add(CharmItems.SHINY_CHARM.get());
                    entries.add(CharmItems.EXP_CHARM.get());
                    entries.add(CharmItems.CATCHING_CHARM.get());
                    entries.add(CharmItems.EXP_ALL.get());
                    entries.add(CharmItems.GOLD_CROWN.get());
                    entries.add(CharmItems.POKE_BAG.get());
                    entries.add(LuckyBlocks.LUCKY_BLOCK.get());
                    entries.add(LuckyBlocks.LEGENDARY_LUCKY_BLOCK.get());
                    entries.add(LuckyBlocks.ULTRA_BEAST_LUCKY_BLOCK.get());
                    entries.add(LuckyBlocks.SHINY_LUCKY_BLOCK.get());
                    entries.add(LuckyBlocks.ITEM_LUCKY_BLOCK.get());
                    entries.add(LuckyBlocks.CHUNK_BARRIER.get());
                    entries.add(PokemonEditorItems.POKEMON_EDITOR.get());
                    entries.add(CharmItems.TEST_BUILD.get());
                })
                .build());

        // [线性-D01] 服务器启动后检查配方
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var rm = server.getRecipeManager();
            int coblemonCount = 0;
            for (var entry : rm.values()) {
                if (entry.id().getNamespace().equals("cobblemon_ts")) {
                    coblemonCount++;
                    LOGGER.info("[RecipeCheck] 已加载cobblemon_ts配方: {}", entry.id());
                }
            }
            if (coblemonCount == 0) {
                LOGGER.warn("[RecipeCheck] ⚠ 未找到任何cobblemon_ts配方！");
            } else {
                LOGGER.info("[RecipeCheck] cobblemon_ts配方总数: {}", coblemonCount);
            }
        });

        // [线性-CMD03] 指令注册
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ClearCharmCommand.register(dispatcher);
        });

        // 网络包注册 C→S
        PayloadTypeRegistry.playC2S().register(SetHeldItemPacket.TYPE, SetHeldItemPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UseItemOnPokemonPacket.TYPE, UseItemOnPokemonPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PartyToPCPacket.TYPE, PartyToPCPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ReleasePokemonPacket.TYPE, ReleasePokemonPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SwapPartySlotsPacket.TYPE, SwapPartySlotsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TestBuildOpenStarterPacket.TYPE, TestBuildOpenStarterPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(OpenPokebagPacket.TYPE, OpenPokebagPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClearAllCharmsPacket.TYPE, ClearAllCharmsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ThrowPokeBallPacket.TYPE, ThrowPokeBallPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestPokebagContentsPacket.TYPE, RequestPokebagContentsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UsePokebagItemPacket.TYPE, UsePokebagItemPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestCharmStatesPacket.TYPE, RequestCharmStatesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateCharmStatePacket.TYPE, UpdateCharmStatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PokemonEditorNetwork.UpdatePokemonPacket.TYPE, PokemonEditorNetwork.UpdatePokemonPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PokemonEditorNetwork.DeletePokemonPacket.TYPE, PokemonEditorNetwork.DeletePokemonPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestChunkCountPacket.TYPE, RequestChunkCountPacket.STREAM_CODEC);

        // 网络包注册 S→C
        PayloadTypeRegistry.playS2C().register(SyncPokebagContentsPacket.TYPE, SyncPokebagContentsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCharmStatesS2CPacket.TYPE, SyncCharmStatesS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PokemonEditorNetwork.OpenEditorPacket.TYPE, PokemonEditorNetwork.OpenEditorPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PokemonEditorNetwork.PokemonDataPacket.TYPE, PokemonEditorNetwork.PokemonDataPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncChunkCountS2CPacket.TYPE, SyncChunkCountS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenHudScreenS2CPacket.TYPE, OpenHudScreenS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncRadarPermissionS2CPacket.TYPE, SyncRadarPermissionS2CPacket.STREAM_CODEC);

        // 网络包处理注册 C→S
        ServerPlayNetworking.registerGlobalReceiver(SetHeldItemPacket.TYPE, (pkt, ctx) -> SetHeldItemPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(UseItemOnPokemonPacket.TYPE, (pkt, ctx) -> UseItemOnPokemonPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(PartyToPCPacket.TYPE, (pkt, ctx) -> PartyToPCPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(ReleasePokemonPacket.TYPE, (pkt, ctx) -> ReleasePokemonPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(SwapPartySlotsPacket.TYPE, (pkt, ctx) -> SwapPartySlotsPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(TestBuildOpenStarterPacket.TYPE, (pkt, ctx) -> TestBuildOpenStarterPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(OpenPokebagPacket.TYPE, (pkt, ctx) -> OpenPokebagPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(ClearAllCharmsPacket.TYPE, (pkt, ctx) -> ClearAllCharmsPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(ThrowPokeBallPacket.TYPE, (pkt, ctx) -> ThrowPokeBallPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(RequestPokebagContentsPacket.TYPE, (pkt, ctx) -> RequestPokebagContentsPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(UsePokebagItemPacket.TYPE, (pkt, ctx) -> UsePokebagItemPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(RequestCharmStatesPacket.TYPE, (pkt, ctx) -> RequestCharmStatesPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(UpdateCharmStatePacket.TYPE, (pkt, ctx) -> UpdateCharmStatePacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(PokemonEditorNetwork.UpdatePokemonPacket.TYPE, (pkt, ctx) -> PokemonEditorNetwork.handleUpdatePokemon(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(PokemonEditorNetwork.DeletePokemonPacket.TYPE, (pkt, ctx) -> PokemonEditorNetwork.handleDeletePokemon(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(SyncConfigPayload.TYPE, (pkt, ctx) -> SyncConfigPayload.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(RequestChunkCountPacket.TYPE, (pkt, ctx) -> RequestChunkCountPacket.handleServerSide(pkt, new com.cobblemon_ts.mod.network.IPayloadContext(ctx.player())));

        // 物品栏注册
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(CharmItems.OVAL_CHARM.get());
            entries.add(CharmItems.SHINY_CHARM.get());
            entries.add(CharmItems.EXP_CHARM.get());
            entries.add(CharmItems.CATCHING_CHARM.get());
            entries.add(CharmItems.EXP_ALL.get());
            entries.add(CharmItems.GOLD_CROWN.get());
            entries.add(CharmItems.POKE_BAG.get());
            entries.add(PokemonEditorItems.POKEMON_EDITOR.get());
            entries.add(CharmItems.TEST_BUILD.get());
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(LuckyBlocks.LUCKY_BLOCK.get());
            entries.add(LuckyBlocks.LEGENDARY_LUCKY_BLOCK.get());
            entries.add(LuckyBlocks.ULTRA_BEAST_LUCKY_BLOCK.get());
            entries.add(LuckyBlocks.SHINY_LUCKY_BLOCK.get());
            entries.add(LuckyBlocks.ITEM_LUCKY_BLOCK.get());
            entries.add(LuckyBlocks.CHUNK_BARRIER.get());
        });

        // 玩家事件注册
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server2) -> {
            CharmEventListener.onPlayerLogin(handler.player);
            RadarPermissionManager.onPlayerJoin(handler.player);
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server2) -> {
            CharmEventListener.onPlayerLogout(handler.player);
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            CharmEventListener.onPlayerClone(oldPlayer, newPlayer, alive);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CharmEventListener.onServerStopping());
        // [线性-R10] 服务器启动：应用区块禁止器强制加载并重建索引
        ServerLifecycleEvents.SERVER_STARTED.register(server -> onServerStarted(server));
        // [线性-R10] 实体加入世界拦截：对应 NeoForge EntityJoinLevelEvent + MobSpawnEvent.PositionCheck
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            onEntityLoad(entity, world);
        });
        // [线性-R10修复] cobblemon 原生事件：玩家放出精灵前拦截（对应 NeoForge EntityJoinLevelEvent）
        CobblemonEvents.POKEMON_SENT_PRE.subscribe(event -> onPokemonSentPre(event));
        // [线性-R10修复] cobblemon 原生事件：野生精灵/NPC 生成前拦截（对应 NeoForge MobSpawnEvent.PositionCheck）
        CobblemonEvents.ENTITY_SPAWN.subscribe(event -> onCobblemonEntitySpawn(event));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            CharmEventListener.onServerTick(server.getPlayerManager().getPlayerList());
            onServerTick(server);
        });

        // 金色王冠实体交互
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && PokemonEntityInteractHandler.onEntityInteract(player, hand, entity)) {
                return net.minecraft.util.ActionResult.SUCCESS;
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        LOGGER.info("[CobblemonTS] Fabric init完成！");
    }

    // [线性-L07] 客户端Tick：同步Cobblemon原生UI，由CobblemonTSClient调用
    public static void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        initTickCache();
        if (cachedStorageInstance == null || cachedPartyStoresField == null || cachedPartyField == null) return;
        try {
            Object storesObj = cachedPartyStoresField.get(cachedStorageInstance);
            if (!(storesObj instanceof Map)) return;
            Map<UUID, Object> partyStores = (Map<UUID, Object>) storesObj;
            Object currentParty = cachedPartyField.get(cachedStorageInstance);
            UUID playerUUID = client.player.getUuid();
            Object fromStore = partyStores.get(playerUUID);
            if (fromStore != null && currentParty != fromStore) {
                cachedPartyField.set(cachedStorageInstance, fromStore);
            }
        } catch (Throwable ignored) {}
    }

    static void initTickCache() {
        if (tickCacheInitialized) return;
        try {
            cachedStorageClass = Class.forName("com.cobblemon.mod.common.client.storage.ClientStorageManager");
            for (java.lang.reflect.Field f : cachedStorageClass.getFields()) {
                if ((f.getName().equals("INSTANCE") || f.getName().equals("instance") || f.getName().equals("Companion"))
                        && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    cachedStorageInstance = f.get(null);
                    break;
                }
            }
            if (cachedStorageInstance == null) {
                for (java.lang.reflect.Method m : cachedStorageClass.getMethods()) {
                    if ((m.getName().equals("getINSTANCE") || m.getName().equals("getInstance") || m.getName().equals("instance"))
                            && m.getParameterCount() == 0
                            && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        cachedStorageInstance = m.invoke(null);
                        break;
                    }
                }
            }
            if (cachedStorageInstance == null) return;
            for (java.lang.reflect.Field f : cachedStorageClass.getDeclaredFields()) {
                if (f.getName().equals("partyStores")) { cachedPartyStoresField = f; f.setAccessible(true); }
                if (f.getName().equals("party")) { cachedPartyField = f; f.setAccessible(true); }
            }
            tickCacheInitialized = true;
        } catch (Throwable ignored) {}
    }

    // [线性-B04] 删除匹配的*.bak备份文件
    public static void deleteAllBakFiles(java.nio.file.Path configPath) {
        java.nio.file.Path parent = configPath.getParent();
        if (parent == null || !java.nio.file.Files.exists(parent)) return;
        String baseName = configPath.getFileName().toString();
        try (var stream = java.nio.file.Files.list(parent)) {
            stream.filter(p -> p.getFileName().toString().startsWith(baseName + ".bak"))
                  .forEach(p -> {
                      try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) {}
                  });
        } catch (Exception ignored) {}
    }

    // ==================== 区块禁止器核心 ====================

    // [线性-R10] 服务器启动：应用强制加载并重建索引
    public static void onServerStarted(net.minecraft.server.MinecraftServer server) {
        try {
            for (var level : server.getWorlds()) {
                if (!(level instanceof ServerWorld sl)) continue;
                var beSet = BARRIER_BE_SET.get(sl);
                if (beSet == null || beSet.isEmpty()) continue;
                int beCount = 0;
                int totalChunks = 0;
                int forceLoadChunks = 0;
                for (var be : beSet) {
                    if (be == null || be.isRemoved()) continue;
                    beCount++;
                    for (var e : be.getConfig().chunkRules.entrySet()) {
                        totalChunks++;
                        if ((e.getValue() & ChunkBarrierBlockEntity.BIT_FORCELOAD) != 0) {
                            forceLoadChunks++;
                        }
                    }
                    be.applyAllForceLoads(sl);
                }
                rebuildLevelIndex(sl);
                LOGGER.info("[ChunkBarrier] 维度 {}: {} 个BE, 覆盖 {} 个区块, {} 个强制加载",
                        sl.getRegistryKey().getValue(), beCount, totalChunks, forceLoadChunks);
            }
            LOGGER.info("[ChunkBarrier] 服务器启动完成");
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception during server start", t);
        }
    }

    // [线性-R10] 服务端tick：每20tick(1秒)检测一次手持幸运方块提示
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        luckyHandTickCounter++;
        if (luckyHandTickCounter < 20) return;
        luckyHandTickCounter = 0;
        try {
            for (var player : server.getPlayerManager().getPlayerList()) {
                if (player == null) continue;
                if (!(player.getWorld() instanceof ServerWorld sl)) continue;
                checkLuckyHandBlocked(player, sl);
            }
        } catch (Throwable t) {
            LOGGER.debug("[ChunkBarrier] onServerTick check fail: {}", t.getMessage());
        }
    }

    // [线性-R10] 检测玩家主手/副手幸运方块是否被当前区块禁止
    private static void checkLuckyHandBlocked(net.minecraft.entity.player.PlayerEntity player, ServerWorld sl) {
        for (var slot : new net.minecraft.item.ItemStack[]{player.getMainHandStack(), player.getOffHandStack()}) {
            if (slot == null || slot.isEmpty()) continue;
            var regKey = Registries.ITEM.getId(slot.getItem());
            if (regKey == null || !regKey.getNamespace().equals(CobblemonTS.MODID)) continue;

            LuckyType type = switch (regKey.getPath()) {
                case "lucky_block" -> LuckyType.LUCKY;
                case "shiny_lucky_block" -> LuckyType.SHINY;
                case "legendary_lucky_block" -> LuckyType.LEGENDARY;
                case "ultra_beast_lucky_block" -> LuckyType.ULTRA;
                default -> null;
            };
            if (type == null) continue;

            int cx = player.getBlockPos().getX() >> 4;
            int cz = player.getBlockPos().getZ() >> 4;
            long chunkKey = chunkKeyOf(cx, cz);

            if (isLuckyBlockBlocked(sl, chunkKey, type, player.getName().getString().toLowerCase())) {
                // [线性-R10] Actionbar提示：物品名称位置上方显示，而非聊天栏
                player.sendMessage(net.minecraft.text.Text.literal(
                        "[ChunkBarrier] 当前区块禁止" + type.getDisplayName() + "！"), true);
                return;
            }
        }
    }

    // [线性-R10] 实体加入世界拦截：对应 NeoForge EntityJoinLevelEvent + MobSpawnEvent.PositionCheck
    public static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity == null || world == null) return;
        try {
            // [线性-R10修复] cobblemon 宝可梦由 POKEMON_SENT_PRE / ENTITY_SPAWN 生成前拦截，
            // 此处跳过 discard，避免实体已加入世界后再删除导致重复召唤、复制实体
            if (isCobblemonPokemon(entity)) return;
            if (isEntityBlocked(entity, world)) {
                // [线性-R10a] 幸运方块豁免：同步命令窗口内 → 放行
                if (isLuckySpawnActive()) return;
                // [线性-R10] 玩家黑白名单豁免：白名单玩家召唤的宝可梦放行
                if (tryWhitelistBypass(entity, world)) return;
                entity.discard();
            }
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception in entity load", t);
        }
    }

    // [线性-R10修复] 判断实体是否为 cobblemon 宝可梦
    private static boolean isCobblemonPokemon(Entity entity) {
        try {
            var key = Registries.ENTITY_TYPE.getId(entity.getType());
            return key != null && key.getNamespace().equals("cobblemon") && key.getPath().equals("pokemon");
        } catch (Throwable t) {
            return false;
        }
    }

    // [线性-R10修复] 玩家放出精灵前拦截：owner 在该区块黑名单且勾选精灵禁止时取消召唤，从根源避免实体复制
    private static void onPokemonSentPre(PokemonSentEvent.Pre event) {
        try {
            ServerWorld level = event.getLevel();
            if (level == null || level.isClient) return;
            if (isLuckySpawnActive()) return;
            Pokemon pokemon = event.getPokemon();
            if (pokemon == null) return;
            ServerPlayerEntity owner = pokemon.getOwnerPlayer();
            if (owner == null) return;
            String ownerName = owner.getName().getString().toLowerCase();
            Vec3d pos = event.getPosition();
            long chunkKey = chunkKeyOf((int) pos.x, (int) pos.z);

            var levelMap = BARRIER_INDEX.get(level);
            if (levelMap == null || levelMap.isEmpty()) return;
            int flags = levelMap.getOrDefault(chunkKey, 0);
            // 需同时勾选精灵禁止(BIT_POKEMON)与玩家名单(BIT_PLAYER)才拦截
            if ((flags & ChunkBarrierBlockEntity.BIT_POKEMON) == 0) return;

            if (isPlayerBlockedInChunk(level, chunkKey, ownerName)) {
                event.cancel();
                LOGGER.debug("[ChunkBarrier] POKEMON_SENT_PRE blocked player {} at chunk {}", ownerName, chunkKey);
            }
        } catch (Throwable t) {
            LOGGER.debug("[ChunkBarrier] POKEMON_SENT_PRE fail: {}", t.getMessage());
        }
    }

    // [线性-R10修复] cobblemon 野生精灵/NPC 生成前拦截：对应 NeoForge MobSpawnEvent.PositionCheck
    private static void onCobblemonEntitySpawn(SpawnEvent<?> event) {
        try {
            Entity entity = event.getEntity();
            if (entity == null) return;
            if (isLuckySpawnActive()) return;
            var spawnablePosition = event.getSpawnablePosition();
            if (spawnablePosition == null) return;
            ServerWorld level = spawnablePosition.getWorld();
            if (level == null || level.isClient) return;

            if (isEntityBlockedAt(entity, level, spawnablePosition.getPosition())) {
                if (tryWhitelistBypassAt(entity, level, spawnablePosition.getPosition())) return;
                event.cancel();
                LOGGER.debug("[ChunkBarrier] ENTITY_SPAWN blocked entity {}", entity.getType());
            }
        } catch (Throwable t) {
            LOGGER.debug("[ChunkBarrier] ENTITY_SPAWN fail: {}", t.getMessage());
        }
    }

    // [线性-R09] 区块禁止器BE注册/注销
    public static void registerBarrierBE(ChunkBarrierBlockEntity be, ServerWorld sl) {
        BARRIER_BE_SET.computeIfAbsent(sl, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(be);
        rebuildLevelIndex(sl);
    }

    public static void unregisterBarrierBE(ChunkBarrierBlockEntity be, ServerWorld sl) {
        var set = BARRIER_BE_SET.get(sl);
        if (set != null) set.remove(be);
        rebuildLevelIndex(sl);
    }

    // [线性-R09] 统计某玩家的 ban chunks 和 forceload chunks（跨维度合计，同维度内区块去重）
    // 返回 [banChunks, forceloadChunks]
    public static int[] countPlayerChunks(java.util.UUID playerId, ChunkBarrierBlockEntity excludeThis) {
        int ban = 0, forceload = 0;
        for (var entry : BARRIER_BE_SET.entrySet()) {
            var levelBeSet = entry.getValue();
            java.util.Set<Long> banSet = new java.util.HashSet<>();
            java.util.Set<Long> forceSet = new java.util.HashSet<>();
            for (var be : levelBeSet) {
                if (be == null || be.isRemoved()) continue;
                if (be == excludeThis) continue;
                if (!playerId.equals(be.getOwnerId())) continue;
                for (var e : be.getConfig().chunkRules.entrySet()) {
                    long k = e.getKey();
                    int flags = e.getValue() & 0xFF;
                    if ((flags & (ChunkBarrierBlockEntity.BIT_POKEMON
                            | ChunkBarrierBlockEntity.BIT_NPC
                            | ChunkBarrierBlockEntity.BIT_HOSTILE
                            | ChunkBarrierBlockEntity.BIT_PASSIVE)) != 0) {
                        banSet.add(k);
                    }
                    if ((flags & ChunkBarrierBlockEntity.BIT_FORCELOAD) != 0) {
                        forceSet.add(k);
                    }
                }
            }
            ban += banSet.size();
            forceload += forceSet.size();
        }
        return new int[]{ban, forceload};
    }

    // [线性-R09] 重建某维度区块禁止索引
    public static void rebuildLevelIndex(ServerWorld sl) {
        try {
            var beSet = BARRIER_BE_SET.get(sl);
            var newMap = new java.util.HashMap<Long, Integer>();
            var newChunkBeMap = new java.util.HashMap<Long, java.util.List<ChunkBarrierBlockEntity>>();
            if (beSet != null) {
                for (var be : beSet) {
                    if (be == null || be.isRemoved()) continue;
                    for (var e : be.getConfig().chunkRules.entrySet()) {
                        long key = e.getKey();
                        int flags = e.getValue() & 0xFF;
                        if (flags != 0) {
                            newMap.merge(key, flags, (a, b) -> a | b);
                        }
                        newChunkBeMap.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(be);
                    }
                }
            }
            if (newMap.isEmpty()) {
                BARRIER_INDEX.remove(sl);
            } else {
                BARRIER_INDEX.put(sl, newMap);
            }
            if (newChunkBeMap.isEmpty()) {
                CHUNK_BE_MAP.remove(sl);
            } else {
                CHUNK_BE_MAP.put(sl, newChunkBeMap);
            }
            if (beSet == null || beSet.isEmpty()) {
                BARRIER_BE_SET.remove(sl);
            }
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception rebuilding index", t);
        }
    }

    // [线性-R09] 清理某维度区块禁止索引
    public static void clearLevelIndex(ServerWorld sl) {
        BARRIER_INDEX.remove(sl);
        BARRIER_BE_SET.remove(sl);
        CHUNK_BE_MAP.remove(sl);
    }

    // [线性-R10] 幸运方块禁止类型枚举
    public enum LuckyType {
        LUCKY("幸运方块"),
        SHINY("闪光幸运方块"),
        LEGENDARY("传说幸运方块"),
        ULTRA("究极异兽方块");

        private final String displayName;
        LuckyType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public static void beginLuckySpawn() { luckySpawnDepth++; }
    public static void endLuckySpawn()   { if (luckySpawnDepth > 0) luckySpawnDepth--; }
    public static boolean isLuckySpawnActive() { return luckySpawnDepth > 0; }

    // [线性-R10] 检查某玩家在某区块是否被禁止开启幸运方块
    // 条件：chunk上有 BIT_LUCKY + BE的banLuckyBlock=true + 玩家不在lucky白名单/在黑名单
    // playerNameLower=null 时跳过名单检查（红石触发等无玩家上下文场景）
    public static boolean isLuckyBlockBlocked(ServerWorld sl, long chunkKey, LuckyType type, String playerNameLower) {
        var levelMap = BARRIER_INDEX.get(sl);
        if (levelMap == null) return false;
        int flags = levelMap.getOrDefault(chunkKey, 0);
        if ((flags & ChunkBarrierBlockEntity.BIT_LUCKY) == 0) return false;

        var beMap = CHUNK_BE_MAP.get(sl);
        if (beMap == null) return false;
        var bes = beMap.get(chunkKey);
        if (bes == null) return false;
        for (var be : bes) {
            if (be == null || be.isRemoved()) continue;
            var cfg = be.getConfig();
            if (!cfg.banLuckyBlock) continue;
            if (playerNameLower == null) return true;
            if (cfg.isLuckyPlayerBanned(playerNameLower, be.getOwnerNameLower())) return true;
        }
        return false;
    }

    // [线性-R10] 检查某玩家在某区块是否被禁止扔宝可梦（per-chunk BIT_PLAYER + 全局 playerWhitelist）
    public static boolean isPlayerBlockedInChunk(ServerWorld sl, long chunkKey, String playerNameLower) {
        var levelMap = BARRIER_INDEX.get(sl);
        if (levelMap == null) return false;
        int flags = levelMap.getOrDefault(chunkKey, 0);
        if ((flags & ChunkBarrierBlockEntity.BIT_PLAYER) == 0) return false;

        var beMap = CHUNK_BE_MAP.get(sl);
        if (beMap == null) return false;
        var bes = beMap.get(chunkKey);
        if (bes == null) return false;
        for (var be : bes) {
            if (be == null || be.isRemoved()) continue;
            if (be.getConfig().isPlayerBanned(playerNameLower, be.getOwnerNameLower())) return true;
        }
        return false;
    }

    // [线性-R10] 工具：从 BlockPos 坐标计算 chunkKey
    public static long chunkKeyOf(int blockX, int blockZ) {
        int cx = blockX >> 4;
        int cz = blockZ >> 4;
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    // [线性-R10] 获取宝可梦 owner 名称（Fabric 端原生 API，无需反射）
    private static String getPokemonOwnerName(Entity entity) {
        try {
            if (!(entity instanceof PokemonEntity pe)) return null;
            Pokemon pokemon = pe.getPokemon();
            if (pokemon == null) return null;
            ServerPlayerEntity owner = pokemon.getOwnerPlayer();
            if (owner == null) return null;
            return owner.getName().getString().toLowerCase();
        } catch (Throwable t) {
            return null;
        }
    }

    // [线性-R10] 判断实体是否被当前区块禁止（使用实体当前位置）
    public static boolean isEntityBlocked(Entity entity, ServerWorld sl) {
        return isEntityBlockedAt(entity, sl, entity.getBlockPos());
    }

    // [线性-R10] 判断实体在指定位置是否被当前区块禁止
    public static boolean isEntityBlockedAt(Entity entity, ServerWorld sl, BlockPos pos) {
        long chunkKey = chunkKeyOf(pos.getX(), pos.getZ());

        var levelMap = BARRIER_INDEX.get(sl);
        if (levelMap == null || levelMap.isEmpty()) return false;
        int flags = levelMap.getOrDefault(chunkKey, 0);
        if (flags == 0) return false;

        var entityType = entity.getType();
        var typeKey = Registries.ENTITY_TYPE.getId(entityType);
        String ns = typeKey.getNamespace();
        String path = typeKey.getPath();

        // [线性-R10] 精灵禁止：野生精灵(owner为null)直接禁止；有主精灵按玩家黑白名单判断
        if ((flags & ChunkBarrierBlockEntity.BIT_POKEMON) != 0
                && ns.equals("cobblemon") && path.equals("pokemon")) {
            String ownerName = getPokemonOwnerName(entity);
            if (ownerName == null) return true;
            return isPlayerBlockedInChunk(sl, chunkKey, ownerName);
        }
        if ((flags & ChunkBarrierBlockEntity.BIT_NPC) != 0
                && ns.equals("cobblemon") && path.equals("npc")) return true;

        var cat = entityType.getSpawnGroup();
        boolean isHostile = cat == SpawnGroup.MONSTER;
        boolean isPassive = cat == SpawnGroup.CREATURE
                || cat == SpawnGroup.AMBIENT
                || cat == SpawnGroup.WATER_CREATURE
                || cat == SpawnGroup.WATER_AMBIENT
                || cat == SpawnGroup.UNDERGROUND_WATER_CREATURE
                || cat == SpawnGroup.AXOLOTLS;
        if (isHostile && (flags & ChunkBarrierBlockEntity.BIT_HOSTILE) != 0) return true;
        if (isPassive && (flags & ChunkBarrierBlockEntity.BIT_PASSIVE) != 0) return true;

        return false;
    }

    // [线性-R10] 尝试白名单豁免：如果 entity 是 cobblemon pokemon 且 owner 在白名单 → 放行
    private static boolean tryWhitelistBypass(Entity entity, ServerWorld sl) {
        return tryWhitelistBypassAt(entity, sl, entity.getBlockPos());
    }

    // [线性-R10] 尝试白名单豁免（指定位置，用于生成前事件）
    private static boolean tryWhitelistBypassAt(Entity entity, ServerWorld sl, BlockPos pos) {
        try {
            var key = Registries.ENTITY_TYPE.getId(entity.getType());
            if (key == null || !key.getNamespace().equals("cobblemon") || !key.getPath().equals("pokemon")) return false;

            String ownerName = getPokemonOwnerName(entity);
            if (ownerName == null) return false;

            long chunkKey = chunkKeyOf(pos.getX(), pos.getZ());

            return !isPlayerBlockedInChunk(sl, chunkKey, ownerName);
        } catch (Throwable t) {
            LOGGER.debug("[ChunkBarrier] Whitelist bypass check fail: {}", t.getMessage());
            return false;
        }
    }
}