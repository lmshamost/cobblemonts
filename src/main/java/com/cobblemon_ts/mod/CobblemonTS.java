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

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cobblemon_ts.mod.network.SetHeldItemPacket;
import com.cobblemon_ts.mod.network.UseItemOnPokemonPacket;
import com.cobblemon_ts.mod.network.PartyToPCPacket;
import com.cobblemon_ts.mod.network.ReleasePokemonPacket;
import com.cobblemon_ts.mod.network.SwapPartySlotsPacket;
import com.cobblemon_ts.mod.network.OpenHeldItemPacket;
import com.cobblemon_ts.mod.lucky.LuckyBlocks;
import com.cobblemon_ts.mod.lucky.LuckyConfig;
import com.cobblemon_ts.mod.lucky.LuckyRecipeCondition;
import com.cobblemon_ts.mod.gui.ModContainers;
import com.cobblemon_ts.mod.gui.PartyHeldItemScreen;
import com.cobblemon_ts.mod.gui.PartyHeldItemContainer;
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

    static {
        CONDITION_SERIALIZERS.register("lucky_recipes_enabled", () -> LuckyRecipeCondition.CODEC);
    }

    public CobblemonTS(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        CharmItems.ITEMS.register(modEventBus);
        PokemonEditorItems.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        LuckyBlocks.register(modEventBus);
        CONDITION_SERIALIZERS.register(modEventBus);
        ModContainers.MENU_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, CharmConfig.SPEC, MODID + "-common.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, LuckyConfig.SPEC, MODID + "-lucky.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, CobblemonTSConfig.SPEC, MODID + "-global.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, DebugConfig.SPEC, MODID + "-debug.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, com.cobblemon_ts.mod.pokemoneditor.PokemonEditorConfig.SPEC, MODID + "-pokemon_editor.toml");


        modEventBus.register(CobblemonTS.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerKeyBindings);
            modEventBus.addListener(this::registerScreens);
        }
        modEventBus.addListener(this::registerPayloads);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        CharmEventHandler.init();
        StarterPokemonInitializer.init();
        // 注册 NeoForge 游戏事件总线（用于客户端 Tick 同步 Cobblemon 原生 UI）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CobblemonTS::onClientTick);
        }
        LOGGER.info("Cobblemon TS initialized!");
    }

    // ========== 客户端 Tick：同步 Cobblemon ClientStorageManager.this.party ← partyStores ==========
    // 问题：Cobblemon sendTo() 发送的 InitializePartyPacket 只更新 partyStores，不更新 this.party
    // 而 Cobblemon 原生精灵栏 overlay 读的是 this.party，导致显示旧数据
    // 解决：每 tick 检查并同步
    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) return;

        try {
            // 1. 获取 Cobblemon ClientStorageManager 单例
            Class<?> storageCls = Class.forName("com.cobblemon.mod.common.client.storage.ClientStorageManager");
            Object storage = null;
            // 先找静态字段 INSTANCE / instance / 单例字段
            for (java.lang.reflect.Field f : storageCls.getFields()) {
                if ((f.getName().equals("INSTANCE") || f.getName().equals("instance") || f.getName().equals("Companion"))
                        && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    try {
                        storage = f.get(null);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
            if (storage == null) {
                // 尝试静态方法 getINSTANCE / getInstance / instance()
                for (Method m : storageCls.getMethods()) {
                    if ((m.getName().equals("getINSTANCE") || m.getName().equals("getInstance") || m.getName().equals("instance"))
                            && m.getParameterCount() == 0
                            && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        try {
                            storage = m.invoke(null);
                            break;
                        } catch (Throwable ignored) {}
                    }
                }
            }
            if (storage == null) return;

            // 2. 获取 partyStores 和 this.party
            java.lang.reflect.Field partyStoresF = null;
            java.lang.reflect.Field partyF = null;
            for (java.lang.reflect.Field f : storageCls.getDeclaredFields()) {
                if (f.getName().equals("partyStores")) partyStoresF = f;
                if (f.getName().equals("party")) partyF = f;
            }
            if (partyStoresF == null || partyF == null) return;
            partyStoresF.setAccessible(true);
            partyF.setAccessible(true);

            Object storesObj = partyStoresF.get(storage);
            if (!(storesObj instanceof Map)) return;
            @SuppressWarnings("unchecked")
            Map<UUID, Object> partyStores = (Map<UUID, Object>) storesObj;
            Object currentParty = partyF.get(storage);

            UUID playerUUID = Minecraft.getInstance().player.getUUID();
            Object fromStore = partyStores.get(playerUUID);
            if (fromStore != null && currentParty != fromStore) {
                // partyStores 中有更新但 this.party 没更新 → 同步
                partyF.set(storage, fromStore);
            }
        } catch (Throwable ignored) {
            // 静默失败：找不到类/方法/字段时不影响游戏
        }
    }

    private void registerKeyBindings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(QuickBattleKeys.QUICK_BATTLE);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContainers.PARTY_HELD_ITEM.get(), PartyHeldItemScreen::new);
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
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CharmConfig.SPEC) {
            LOGGER.info("CobblemonTS: CharmConfig loaded from file");
        }
        if (event.getConfig().getSpec() == LuckyConfig.SPEC) {
            LOGGER.info("CobblemonTS: LuckyConfig loaded from file");
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CharmConfig.SPEC) {
            LOGGER.info("CobblemonTS: CharmConfig reloaded, catching={}, exp={}, shiny={}",
                    CharmConfig.CATCHING_CHARM_BOOST.get(),
                    CharmConfig.EXP_CHARM_BOOST.get(),
                    CharmConfig.SHINY_CHARM_BOOST.get());
        }
        if (event.getConfig().getSpec() == LuckyConfig.SPEC) {
            LOGGER.info("CobblemonTS: LuckyConfig reloaded, recipes={}, minLevel={}, maxLevel={}",
                    LuckyConfig.LUCKY_RECIPES_ENABLED.get(),
                    LuckyConfig.MIN_SPAWN_LEVEL.get(),
                    LuckyConfig.MAX_SPAWN_LEVEL.get());
        }
    }
}