package com.cobblemon_ts.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.lwjgl.glfw.GLFW;

import com.cobblemon_ts.mod.network.SyncPokebagContentsPacket;
import com.cobblemon_ts.mod.network.SyncCharmStatesS2CPacket;
import com.cobblemon_ts.mod.network.OpenHudScreenS2CPacket;
import com.cobblemon_ts.mod.network.SyncRadarPermissionS2CPacket;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorNetwork;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorClientHandler;
import com.cobblemon_ts.mod.pokemonradar.PokemonRadarRenderer;
import com.cobblemon_ts.mod.pokemonradar.PokemonRadarScreen;
import com.cobblemon_ts.mod.pokemonradar.PokemonRadarState;
import com.cobblemon_ts.mod.network.IPayloadContext;
import com.cobblemon_ts.mod.gui.ModContainers;
import com.cobblemon_ts.mod.gui.PartyHeldItemScreen;
import com.cobblemon_ts.mod.gui.PokebagScreen;
import com.cobblemon_ts.mod.gui.ChunkBarrierScreen;

import java.util.Map;
import java.util.UUID;

public class CobblemonTSClient implements ClientModInitializer {

    // [线性-R06] 精灵雷达双按键：NUMPAD7=编辑器, NUMPAD8=开关追踪
    public static KeyBinding RADAR_EDITOR_KEY;
    public static KeyBinding RADAR_TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        // [线性-CMD-HUD] HUD打开：改为服务端/cts hud命令触发网络包，客户端接收后打开HUD设置屏
        ClientPlayNetworking.registerGlobalReceiver(OpenHudScreenS2CPacket.TYPE, (pkt, ctx) ->
                OpenHudScreenS2CPacket.handleClientSide(pkt, new IPayloadContext(MinecraftClient.getInstance())));

        // S→C网络包处理
        ClientPlayNetworking.registerGlobalReceiver(SyncPokebagContentsPacket.TYPE, (pkt, ctx) ->
                SyncPokebagContentsPacket.handleClientSide(pkt, new IPayloadContext(MinecraftClient.getInstance())));
        ClientPlayNetworking.registerGlobalReceiver(SyncCharmStatesS2CPacket.TYPE, (pkt, ctx) ->
                SyncCharmStatesS2CPacket.handleClientSide(pkt, new IPayloadContext(MinecraftClient.getInstance())));
        // [线性-RADAR-PERM] 接收雷达权限：更新缓存，被禁用时立即关闭追踪
        ClientPlayNetworking.registerGlobalReceiver(SyncRadarPermissionS2CPacket.TYPE, (pkt, ctx) -> {
            SyncRadarPermissionS2CPacket.handleClientSide(pkt, new IPayloadContext(MinecraftClient.getInstance()));
            if (!pkt.allowed()) {
                PokemonRadarState state = PokemonRadarState.getInstance();
                if (state.isRadarTrackingEnabled()) {
                    state.setRadarTrackingEnabled(false);
                    state.saveToFile();
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(PokemonEditorNetwork.OpenEditorPacket.TYPE, (pkt, ctx) ->
                PokemonEditorClientHandler.handleOpenEditor(pkt, new IPayloadContext(MinecraftClient.getInstance())));
        ClientPlayNetworking.registerGlobalReceiver(PokemonEditorNetwork.PokemonDataPacket.TYPE, (pkt, ctx) ->
                PokemonEditorClientHandler.handlePokemonData(pkt, new IPayloadContext(MinecraftClient.getInstance())));

        // 客户端Tick同步Cobblemon原生UI
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CobblemonTS.onClientTick(client);
        });

        // [线性-R06] 精灵雷达按键绑定：NUMPAD7=编辑器, NUMPAD8=开关追踪
        RADAR_EDITOR_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cobblemon_ts.radar.editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_7,
                "key.categories.cobblemon_ts"));
        RADAR_TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cobblemon_ts.radar.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_8,
                "key.categories.cobblemon_ts"));

        // [线性-R07a] 精灵雷达按键Tick：NUMPAD7打开编辑器, NUMPAD8开关追踪
        // [线性-RADAR-PERM] 权限门：先消费按键状态（避免堆积），再按服务端下发的权限缓存决定是否触发
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                boolean editorPressed = RADAR_EDITOR_KEY.wasPressed();
                boolean togglePressed = RADAR_TOGGLE_KEY.wasPressed();
                if (!SyncRadarPermissionS2CPacket.isClientAllowed()) {
                    if (editorPressed || togglePressed) {
                        client.player.sendMessage(Text.translatable("cobblemon_ts.radar.no_permission"), true);
                    }
                } else {
                    if (editorPressed) {
                        client.setScreen(new PokemonRadarScreen());
                    }
                    if (togglePressed) {
                        PokemonRadarState state = PokemonRadarState.getInstance();
                        state.setRadarTrackingEnabled(!state.isRadarTrackingEnabled());
                        state.saveToFile(); // [线性-R08c] 开关切换后持久化
                        client.player.sendMessage(
                            Text.translatable(state.isRadarTrackingEnabled()
                                    ? "cobblemon_ts.radar.tracking_on" : "cobblemon_ts.radar.tracking_off"), true);
                    }
                }
            }
        });

        // [线性-R05] 精灵雷达渲染：世界方框 + HUD悬停提示
        WorldRenderEvents.AFTER_TRANSLUCENT.register(PokemonRadarRenderer::renderWorld);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> PokemonRadarRenderer.renderHud(drawContext));

        // 客户端断线清理
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientEventHandler.onClientDisconnect();
            SyncRadarPermissionS2CPacket.clearCache();
        });

        // 屏幕注册
        HandledScreens.register(ModContainers.PARTY_HELD_ITEM, PartyHeldItemScreen::new);
        HandledScreens.register(ModContainers.POKEBAG, PokebagScreen::new);
        HandledScreens.register(ModContainers.CHUNK_BARRIER, ChunkBarrierScreen::new);
    }
}