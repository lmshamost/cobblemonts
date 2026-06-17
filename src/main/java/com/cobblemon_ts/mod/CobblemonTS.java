package com.cobblemon_ts.mod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
import com.cobblemon_ts.mod.lucky.LuckyBlocks;
import com.cobblemon_ts.mod.lucky.LuckyRecipeCondition;
import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import com.cobblemon_ts.mod.gui.ModContainers;
import com.cobblemon_ts.mod.gui.PartyHeldItemScreen;
import com.cobblemon_ts.mod.gui.PartyHeldItemContainer;
import com.cobblemon_ts.mod.gui.PokebagScreen;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorItems;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorNetwork;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorClientHandler;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
    public static final String MODID = "coblemon_ts";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHARM_TAB = CREATIVE_MODE_TABS.register("charm_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.coblemon_ts"))
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
        ModContainers.MENU_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, CharmConfig.SPEC, "CoblemonTest/common.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, CobblemonTSConfig.SPEC, "CoblemonTest/global.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, DebugConfig.SPEC, "CoblemonTest/debug.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ColorEditorConfig.SPEC, "CoblemonTest/colors.toml");


        modEventBus.register(CobblemonTS.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerKeyBindings);
            modEventBus.addListener(this::registerScreens);
        }
        modEventBus.addListener(this::registerPayloads);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        CharmEventHandler.init();
        // [线性-D01] 配方追踪：服务器启动后检查RecipeManager
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CobblemonTS::onServerStarted);
        // 注册 NeoForge 游戏事件总线（用于客户端 Tick 同步 Cobblemon 原生 UI）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CobblemonTS::onClientTick);
            // [线性-C02] 注册按键事件监听道具袋快捷键
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CobblemonTS::onKeyInput);
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

    // [线性-D01] 服务器启动后检查配方加载情况
    public static void onServerStarted(ServerStartedEvent event) {
        RecipeManager rm = event.getServer().getRecipeManager();
        LOGGER.info("[RecipeCheck] ========== 服务器启动，检查RecipeManager ==========");
        int coblemonCount = 0;
        for (ResourceLocation key : rm.getRecipeIds().toList()) {
            if (key.getNamespace().equals("coblemon_ts")) {
                coblemonCount++;
                LOGGER.info("[RecipeCheck] 已加载coblemon_ts配方: {}", key);
            }
        }
        if (coblemonCount == 0) {
            LOGGER.warn("[RecipeCheck] ⚠ 未找到任何coblemon_ts配方！");
        } else {
            LOGGER.info("[RecipeCheck] coblemon_ts配方总数: {}", coblemonCount);
        }
        LOGGER.info("[RecipeCheck] ================================================");
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

    // [线性-C02] 按键事件：监听道具袋快捷键按下
    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    public static void onKeyInput(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        if (Minecraft.getInstance().player == null) return;
        if (QuickBattleKeys.OPEN_POKEBAG.consumeClick()) {
            // [线性-C02] 检查玩家是否拥有道具袋，未拥有则不打开
            var playerData = CharmPlayerData.get(Minecraft.getInstance().player);
            if (playerData != null && playerData.getCharmSlotManager().isPokebagOwned()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpenPokebagPacket());
            }
        }
    }

    private void registerKeyBindings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(QuickBattleKeys.QUICK_BATTLE);
        event.register(QuickBattleKeys.OPEN_POKEBAG);  // [线性-C02] 注册道具袋快捷键
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContainers.PARTY_HELD_ITEM.get(), PartyHeldItemScreen::new);
        event.register(ModContainers.POKEBAG.get(), PokebagScreen::new);
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