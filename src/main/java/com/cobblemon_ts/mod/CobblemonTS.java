package com.cobblemon_ts.mod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.world.item.crafting.RecipeManager;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
import com.cobblemon_ts.mod.command.HudCommand;
import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierEntityInterceptor;
import com.cobblemon_ts.mod.gui.ChunkBarrierContainer;
import com.cobblemon_ts.mod.gui.ChunkBarrierScreen;
import com.cobblemon_ts.mod.lucky.LuckyBlocks;
import com.cobblemon_ts.mod.lucky.LuckyRecipeCondition;
import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import com.cobblemon_ts.mod.gui.ModContainers;
import com.cobblemon_ts.mod.gui.PartyHeldItemScreen;
import com.cobblemon_ts.mod.gui.PartyHeldItemContainer;
import com.cobblemon_ts.mod.gui.PokebagScreen;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorItems;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorNetwork;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorClientHandler;
import com.cobblemon_ts.mod.pokemonradar.PokemonRadarScreen;
import com.cobblemon_ts.mod.pokemonradar.PokemonRadarState;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

@Mod(CobblemonTS.MODID)
public class CobblemonTS {
    public static final String MODID = "cobblemon_ts";
    public static final java.util.Map<net.minecraft.server.level.ServerLevel, java.util.Map<Long, Integer>> BARRIER_INDEX = new java.util.HashMap<>();
    public static final java.util.Map<net.minecraft.server.level.ServerLevel, java.util.Set<com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity>> BARRIER_BE_SET = new java.util.HashMap<>();
    public static final Logger LOGGER = LogUtils.getLogger();

    // [线性-R06] 精灵雷达双按键：NUMPAD7=编辑器, NUMPAD8=开关追踪
    public static final KeyMapping RADAR_EDITOR_KEY = new KeyMapping(
            "key.cobblemon_ts.radar.editor",
            InputConstants.KEY_NUMPAD7,
            "key.categories.cobblemon_ts"
    );
    public static final KeyMapping RADAR_TOGGLE_KEY = new KeyMapping(
            "key.cobblemon_ts.radar.toggle",
            InputConstants.KEY_NUMPAD8,
            "key.categories.cobblemon_ts"
    );
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHARM_TAB = CREATIVE_MODE_TABS.register("charm_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cobblemon_ts"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CharmItems.SHINY_CHARM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CharmItems.OVAL_CHARM);
                output.accept(CharmItems.SHINY_CHARM);
                output.accept(CharmItems.EXP_CHARM);
                output.accept(CharmItems.CATCHING_CHARM);
                output.accept(CharmItems.EXP_ALL);
                output.accept(CharmItems.GOLD_CROWN);
                output.accept(CharmItems.POKE_BAG);
                output.accept(LuckyBlocks.LUCKY_BLOCK);
                output.accept(LuckyBlocks.LEGENDARY_LUCKY_BLOCK);
                output.accept(LuckyBlocks.ULTRA_BEAST_LUCKY_BLOCK);
                output.accept(LuckyBlocks.SHINY_LUCKY_BLOCK);
                output.accept(LuckyBlocks.ITEM_LUCKY_BLOCK);
                output.accept(PokemonEditorItems.POKEMON_EDITOR);
                output.accept(CharmItems.TEST_BUILD);
            }).build());

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, MODID);

    // [线性-L06] 战利品修饰器注册
    public static final DeferredRegister<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChunkBarrierBlockEntity>> CHUNK_BARRIER_BE =
            BLOCK_ENTITY_TYPES.register("chunk_barrier",
                    () -> BlockEntityType.Builder.of(ChunkBarrierBlockEntity::new, LuckyBlocks.CHUNK_BARRIER.get()).build(null));

    static {
        CONDITION_SERIALIZERS.register("lucky_recipes_enabled", () -> LuckyRecipeCondition.CODEC);
        LOOT_MODIFIER_SERIALIZERS.register("charm_loot", () -> CharmLootModifier.CODEC); // [线性-L06] 护符战利品
    }

    public CobblemonTS(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        CharmItems.ITEMS.register(modEventBus);
        PokemonEditorItems.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        LuckyBlocks.register(modEventBus);
        CONDITION_SERIALIZERS.register(modEventBus);
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus); // [线性-L06] 注册战利品修饰器
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ModContainers.MENU_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, CharmConfig.SPEC, "cobblemonts/server.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, CobblemonTSConfig.SPEC, "cobblemonts/common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ColorEditorConfig.SPEC, "cobblemonts/client.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ChunkBarrierServerConfig.SPEC, "cobblemonts/server.toml");


        modEventBus.register(CobblemonTS.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
            modEventBus.addListener(this::registerKeyMappings);
        }
        modEventBus.addListener(this::registerPayloads);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        CharmEventHandler.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CobblemonTS::onClientTick);
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClearCharmCommand::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CobblemonTS::onServerStarted);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.cobblemon_ts.mod.command.HudCommand::register);
        }
        LOGGER.info("Cobblemon TS initialized!");
    }

    // [线性-B01] 配置文件.bak清理：每次配置加载后删除所有 common-*.toml.bak，让下次保存从 common-1 开始
    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent.Loading event) {
        deleteAllBakFiles(event.getConfig().getFullPath());
    }

    // [线性-B01a] 配置重载时也清理（/reload命令触发）
    @SubscribeEvent
    public static void onConfigReloaded(ModConfigEvent.Reloading event) {
        deleteAllBakFiles(event.getConfig().getFullPath());
    }

    // [线性-B01b] 删除指定配置的 *-数字.toml.bak，只清当前触发配置的备份
    // 例如传入 common.toml → 只删 common-1.toml.bak / common-2.toml.bak ... 不影响 global / debug / colors
    public static void deleteAllBakFiles(Path configPath) {
        if (configPath == null) return;
        Path configDir = configPath.getParent();
        if (configDir == null || !Files.isDirectory(configDir)) return;
        String baseName = configPath.getFileName().toString().replace(".toml", ""); // "common"

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, entry ->
                entry.getFileName().toString().startsWith(baseName + "-"))) {
            for (Path path : stream) {
                try {
                    Files.delete(path);
                    LOGGER.debug("[ConfigBak] 删除: {}", path.getFileName());
                } catch (IOException e) {
                    LOGGER.warn("[ConfigBak] 删除失败: {}", path.getFileName(), e);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void onServerStarted(ServerStartedEvent event) {
        try {
            var allLevels = event.getServer().getAllLevels();
            for (var level : allLevels) {
                if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) continue;
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
                        if ((e.getValue() & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_FORCELOAD) != 0) {
                            forceLoadChunks++;
                        }
                    }
                    be.applyAllForceLoads(sl);
                }
                rebuildLevelIndex(sl);
                LOGGER.info("[ChunkBarrier] 维度 {}: {} 个BE, 覆盖 {} 个区块, {} 个强制加载",
                        sl.dimension().location(), beCount, totalChunks, forceLoadChunks);
            }
            LOGGER.info("[ChunkBarrier] 服务器启动完成");
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception during server start", t);
        }
    }

    // ========== 客户端 Tick：同步 Cobblemon ClientStorageManager.this.party ← partyStores ==========
    // [线性-L07] 反射缓存：每tick高频调用，必须缓存避免GC压力
    private static Class<?> cachedStorageClass;
    private static Object cachedStorageInstance;
    private static java.lang.reflect.Field cachedPartyStoresField;
    private static java.lang.reflect.Field cachedPartyField;
    private static boolean tickCacheInitialized;

    private static void initTickCache() {
        if (tickCacheInitialized) return;
        try {
            cachedStorageClass = Class.forName("com.cobblemon.mod.common.client.storage.ClientStorageManager");
            // 查找单例
            for (java.lang.reflect.Field f : cachedStorageClass.getFields()) {
                if ((f.getName().equals("INSTANCE") || f.getName().equals("instance") || f.getName().equals("Companion"))
                        && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    cachedStorageInstance = f.get(null);
                    break;
                }
            }
            if (cachedStorageInstance == null) {
                for (Method m : cachedStorageClass.getMethods()) {
                    if ((m.getName().equals("getINSTANCE") || m.getName().equals("getInstance") || m.getName().equals("instance"))
                            && m.getParameterCount() == 0
                            && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        cachedStorageInstance = m.invoke(null);
                        break;
                    }
                }
            }
            if (cachedStorageInstance == null) return;
            // 缓存字段
            for (java.lang.reflect.Field f : cachedStorageClass.getDeclaredFields()) {
                if (f.getName().equals("partyStores")) { cachedPartyStoresField = f; f.setAccessible(true); }
                if (f.getName().equals("party")) { cachedPartyField = f; f.setAccessible(true); }
            }
            tickCacheInitialized = true;
        } catch (Throwable ignored) {}
    }

    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        // [线性-R07a] 精灵雷达按键：NUMPAD7打开编辑器, NUMPAD8开关追踪（需配置总开关开启）
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && CobblemonTSConfig.POKEMON_RADAR_ENABLED.get()) {
            if (RADAR_EDITOR_KEY.consumeClick()) {
                mc.setScreen(new PokemonRadarScreen());
            }
            if (RADAR_TOGGLE_KEY.consumeClick()) {
                PokemonRadarState state = PokemonRadarState.getInstance();
                state.setRadarTrackingEnabled(!state.isRadarTrackingEnabled());
                state.saveToFile(); // [线性-R08c] 开关切换后持久化
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                            state.isRadarTrackingEnabled() ? "cobblemon_ts.radar.tracking_on" : "cobblemon_ts.radar.tracking_off"), true);
                }
            }
        }

        if (Minecraft.getInstance().player == null) return;
        initTickCache(); // [线性-L07] 首次调用初始化缓存，后续直接复用
        if (cachedStorageInstance == null || cachedPartyStoresField == null || cachedPartyField == null) return;

        try {
            Object storesObj = cachedPartyStoresField.get(cachedStorageInstance);
            if (!(storesObj instanceof Map)) return;
            @SuppressWarnings("unchecked")
            Map<UUID, Object> partyStores = (Map<UUID, Object>) storesObj;
            Object currentParty = cachedPartyField.get(cachedStorageInstance);

            UUID playerUUID = Minecraft.getInstance().player.getUUID();
            Object fromStore = partyStores.get(playerUUID);
            if (fromStore != null && currentParty != fromStore) {
                cachedPartyField.set(cachedStorageInstance, fromStore);
            }
        } catch (Throwable ignored) {}
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContainers.PARTY_HELD_ITEM.get(), PartyHeldItemScreen::new);
        event.register(ModContainers.POKEBAG.get(), PokebagScreen::new);
        event.register(ModContainers.CHUNK_BARRIER.get(), ChunkBarrierScreen::new);
    }

    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RADAR_EDITOR_KEY);
        event.register(RADAR_TOGGLE_KEY);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SetHeldItemPacket.TYPE,
                SetHeldItemPacket.STREAM_CODEC,
                SetHeldItemPacket::handleServerSide
        );
        registrar.playToServer(
                UseItemOnPokemonPacket.TYPE,
                UseItemOnPokemonPacket.STREAM_CODEC,
                UseItemOnPokemonPacket::handleServerSide
        );
        registrar.playToServer(
                PartyToPCPacket.TYPE,
                PartyToPCPacket.STREAM_CODEC,
                PartyToPCPacket::handleServerSide
        );
        registrar.playToServer(
                ReleasePokemonPacket.TYPE,
                ReleasePokemonPacket.STREAM_CODEC,
                ReleasePokemonPacket::handleServerSide
        );
        registrar.playToServer(
                SwapPartySlotsPacket.TYPE,
                SwapPartySlotsPacket.STREAM_CODEC,
                SwapPartySlotsPacket::handleServerSide
        );
        // [线性-L1] 测试物品：请求打开初始宝可梦选择
        registrar.playToServer(
                TestBuildOpenStarterPacket.TYPE,
                TestBuildOpenStarterPacket.STREAM_CODEC,
                TestBuildOpenStarterPacket::handleServerSide
        );
        // [线性-L07] 道具袋：打开道具袋界面
        registrar.playToServer(
                OpenPokebagPacket.TYPE,
                OpenPokebagPacket.STREAM_CODEC,
                OpenPokebagPacket::handleServerSide
        );
        // [线性-L1] 清除所有护符和道具袋
        registrar.playToServer(
                ClearAllCharmsPacket.TYPE,
                ClearAllCharmsPacket.STREAM_CODEC,
                ClearAllCharmsPacket::handleServerSide
        );
        // [线性-B05ze] 直接投掷精灵球（跳过gameMode.useItem→PokeBallItem.use链路）
        registrar.playToServer(
                ThrowPokeBallPacket.TYPE,
                ThrowPokeBallPacket.STREAM_CODEC,
                ThrowPokeBallPacket::handleServerSide
        );
        // [线性-B05zf] 请求Pokebag库存同步 C→S
        registrar.playToServer(
                RequestPokebagContentsPacket.TYPE,
                RequestPokebagContentsPacket.STREAM_CODEC,
                RequestPokebagContentsPacket::handleServerSide
        );
        // [线性-B05zf] Pokebag库存同步 S→C
        registrar.playToClient(
                SyncPokebagContentsPacket.TYPE,
                SyncPokebagContentsPacket.STREAM_CODEC,
                SyncPokebagContentsPacket::handleClientSide
        );
        // [线性-C02] 使用道具袋物品 C→S
        registrar.playToServer(
                UsePokebagItemPacket.TYPE,
                UsePokebagItemPacket.STREAM_CODEC,
                UsePokebagItemPacket::handleServerSide
        );

        // ==== Pokemon Editor Network Packets ====
        // Server -> Client: 打开编辑器
        registrar.playToClient(
                PokemonEditorNetwork.OpenEditorPacket.TYPE,
                PokemonEditorNetwork.OpenEditorPacket.STREAM_CODEC,
                PokemonEditorClientHandler::handleOpenEditor
        );
        // Server -> Client: 宝可梦数据
        registrar.playToClient(
                PokemonEditorNetwork.PokemonDataPacket.TYPE,
                PokemonEditorNetwork.PokemonDataPacket.STREAM_CODEC,
                PokemonEditorClientHandler::handlePokemonData
        );
        // Client -> Server: 更新宝可梦
        registrar.playToServer(
                PokemonEditorNetwork.UpdatePokemonPacket.TYPE,
                PokemonEditorNetwork.UpdatePokemonPacket.STREAM_CODEC,
                PokemonEditorNetwork::handleUpdatePokemon
        );
        // Client -> Server: 删除宝可梦
        registrar.playToServer(
                PokemonEditorNetwork.DeletePokemonPacket.TYPE,
                PokemonEditorNetwork.DeletePokemonPacket.STREAM_CODEC,
                PokemonEditorNetwork::handleDeletePokemon
        );

        // ==== TestBuild Charm State Packets ====
        // [线性-P04] 客户端请求护符状态快照 C→S
        registrar.playToServer(
                RequestCharmStatesPacket.TYPE,
                RequestCharmStatesPacket.STREAM_CODEC,
                RequestCharmStatesPacket::handleServerSide
        );
        // [线性-P04a] 服务端响应护符状态快照 S→C
        registrar.playToClient(
                SyncCharmStatesS2CPacket.TYPE,
                SyncCharmStatesS2CPacket.STREAM_CODEC,
                SyncCharmStatesS2CPacket::handleClientSide
        );
        // [线性-P04b] 客户端护符开关保存 C→S
        registrar.playToServer(
                UpdateCharmStatePacket.TYPE,
                UpdateCharmStatePacket.STREAM_CODEC,
                UpdateCharmStatePacket::handleServerSide
        );
        registrar.playToServer(
                com.cobblemon_ts.mod.network.SyncConfigPayload.TYPE,
                com.cobblemon_ts.mod.network.SyncConfigPayload.STREAM_CODEC,
                com.cobblemon_ts.mod.network.SyncConfigPayload::handleServerSide
        );
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CharmItems.OVAL_CHARM);
            event.accept(CharmItems.SHINY_CHARM);
            event.accept(CharmItems.EXP_CHARM);
            event.accept(CharmItems.CATCHING_CHARM);
            event.accept(CharmItems.EXP_ALL);
            event.accept(CharmItems.GOLD_CROWN);
            event.accept(CharmItems.POKE_BAG);
            event.accept(PokemonEditorItems.POKEMON_EDITOR);
            event.accept(CharmItems.TEST_BUILD);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(LuckyBlocks.LUCKY_BLOCK);
            event.accept(LuckyBlocks.LEGENDARY_LUCKY_BLOCK);
            event.accept(LuckyBlocks.ULTRA_BEAST_LUCKY_BLOCK);
            event.accept(LuckyBlocks.SHINY_LUCKY_BLOCK);
            event.accept(LuckyBlocks.ITEM_LUCKY_BLOCK);
            event.accept(LuckyBlocks.CHUNK_BARRIER);
        }
    }

    public static void registerBarrierBE(com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity be, net.minecraft.server.level.ServerLevel sl) {
        BARRIER_BE_SET.computeIfAbsent(sl, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(be);
        rebuildLevelIndex(sl);
    }

    public static void unregisterBarrierBE(com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity be, net.minecraft.server.level.ServerLevel sl) {
        var set = BARRIER_BE_SET.get(sl);
        if (set != null) set.remove(be);
        rebuildLevelIndex(sl);
    }

    public static void rebuildLevelIndex(net.minecraft.server.level.ServerLevel sl) {
        try {
            var beSet = BARRIER_BE_SET.get(sl);
            var newMap = new java.util.HashMap<Long, Integer>();
            if (beSet != null) {
                for (var be : beSet) {
                    if (be == null || be.isRemoved()) continue;
                    for (var e : be.getConfig().chunkRules.entrySet()) {
                        long key = e.getKey();
                        int flags = e.getValue() & 0xFF;
                        if (flags != 0) {
                            newMap.merge(key, flags, (a, b) -> a | b);
                        }
                    }
                }
            }
            if (newMap.isEmpty()) {
                BARRIER_INDEX.remove(sl);
            } else {
                BARRIER_INDEX.put(sl, newMap);
            }
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception rebuilding index", t);
        }
    }

    public static boolean isEntityBlocked(net.minecraft.world.entity.Entity entity, net.minecraft.server.level.ServerLevel sl) {
        long cx = entity.blockPosition().getX() >> 4;
        long cz = entity.blockPosition().getZ() >> 4;
        long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);

        var levelMap = BARRIER_INDEX.get(sl);
        if (levelMap == null || levelMap.isEmpty()) return false;
        int flags = levelMap.getOrDefault(chunkKey, 0);
        if (flags == 0) return false;

        var entityType = entity.getType();
        var typeKey = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        String ns = typeKey.getNamespace();
        String path = typeKey.getPath();

        if ((flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_POKEMON) != 0
                && ns.equals("cobblemon") && path.equals("pokemon")) return true;
        if ((flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_NPC) != 0
                && ns.equals("cobblemon") && path.equals("npc")) return true;

        var cat = entityType.getCategory();
        boolean isHostile = cat == net.minecraft.world.entity.MobCategory.MONSTER;
        boolean isPassive = cat == net.minecraft.world.entity.MobCategory.CREATURE
                || cat == net.minecraft.world.entity.MobCategory.AMBIENT
                || cat == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                || cat == net.minecraft.world.entity.MobCategory.WATER_AMBIENT
                || cat == net.minecraft.world.entity.MobCategory.UNDERGROUND_WATER_CREATURE
                || cat == net.minecraft.world.entity.MobCategory.AXOLOTLS;
        if (isHostile && (flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_HOSTILE) != 0) return true;
        if (isPassive && (flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_PASSIVE) != 0) return true;

        return false;
    }

    public static void onEntityJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        var entity = event.getEntity();
        if (entity == null) return;
        var level = event.getLevel();
        if (level == null || level.isClientSide()) return;
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;

        try {
            long cx = entity.blockPosition().getX() >> 4;
            long cz = entity.blockPosition().getZ() >> 4;

            if (isEntityBlocked(entity, sl)) {
                event.setCanceled(true);
            }
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception in EntityJoinLevel", t);
        }
    }

    public static void onMobSpawnPositionCheck(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        try {
            if (event.getLevel() == null || event.getLevel().isClientSide()) return;
            var level = event.getLevel();
            if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
            var spawnType = event.getSpawnType();
            if (spawnType == null) return;
            if (spawnType == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG
                    || spawnType == net.minecraft.world.entity.MobSpawnType.BREEDING
                    || spawnType == net.minecraft.world.entity.MobSpawnType.DISPENSER
                    || spawnType == net.minecraft.world.entity.MobSpawnType.BUCKET
                    || spawnType == net.minecraft.world.entity.MobSpawnType.COMMAND
                    || spawnType == net.minecraft.world.entity.MobSpawnType.EVENT
                    || spawnType == net.minecraft.world.entity.MobSpawnType.TRIAL_SPAWNER
                    || spawnType == net.minecraft.world.entity.MobSpawnType.CONVERSION
                    || spawnType == net.minecraft.world.entity.MobSpawnType.TRIGGERED) return;

            long cx = (long) event.getX() >> 4;
            long cz = (long) event.getZ() >> 4;
            long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);

            var levelMap = BARRIER_INDEX.get(sl);
            if (levelMap == null || levelMap.isEmpty()) return;
            int flags = levelMap.getOrDefault(chunkKey, 0);

            var entity = event.getEntity();
            if (entity == null) return;
            var entityType = entity.getType();

            if (flags == 0) return;
            var typeKey = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            String ns = typeKey.getNamespace();
            String path = typeKey.getPath();

            boolean blocked = false;
            if ((flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_POKEMON) != 0
                    && ns.equals("cobblemon") && path.equals("pokemon")) blocked = true;
            if (!blocked && (flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_NPC) != 0
                    && ns.equals("cobblemon") && path.equals("npc")) blocked = true;
            if (!blocked) {
                var cat = entityType.getCategory();
                boolean isHostile = cat == net.minecraft.world.entity.MobCategory.MONSTER;
                boolean isPassive = cat == net.minecraft.world.entity.MobCategory.CREATURE
                        || cat == net.minecraft.world.entity.MobCategory.AMBIENT
                        || cat == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                        || cat == net.minecraft.world.entity.MobCategory.WATER_AMBIENT
                        || cat == net.minecraft.world.entity.MobCategory.UNDERGROUND_WATER_CREATURE
                        || cat == net.minecraft.world.entity.MobCategory.AXOLOTLS;
                if (isHostile && (flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_HOSTILE) != 0) blocked = true;
                if (isPassive && (flags & com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity.BIT_PASSIVE) != 0) blocked = true;
            }

            if (blocked) {
                event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
            }
        } catch (Throwable t) {
            LOGGER.error("[ChunkBarrier] Exception in spawn check", t);
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CharmConfig.SPEC) {
            LOGGER.info("CobblemonTS: CharmConfig reloaded, catching={}, exp={}, shiny={}, minLevel={}, maxLevel={}",
                    CharmConfig.CATCHING_CHARM_BOOST.get(),
                    CharmConfig.EXP_CHARM_BOOST.get(),
                    CharmConfig.SHINY_CHARM_BOOST.get(),
                    CharmConfig.MIN_SPAWN_LEVEL.get(),
                    CharmConfig.MAX_SPAWN_LEVEL.get());
        }
    }
}