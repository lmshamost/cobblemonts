package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerInventory;

import java.lang.reflect.Method;

public class PartyHeldItemScreen extends HandledScreen<PartyHeldItemContainer> {

    // [线性-O03] 反射缓存：静态缓存避免每帧重复反射
    private static boolean iconReflectionReady;
    private static Method getIconMethod;
    private static Method guiBlitkMethod;
    private static Object cachedCobblemonInstance;
    private static Method cachedGetStorageMethod;
    private static Method cachedGetPartyMethod;
    private static Method cachedPartySizeMethod;
    private static Method cachedPartyGetMethod;
    private static boolean reflectionInitialized;

    public PartyHeldItemScreen(PartyHeldItemContainer handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    public void render(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
        super.render(DrawContext, mouseX, mouseY, partialTick);
        drawPokemonIcons(DrawContext);
    }

    @Override
    protected void drawBackground(DrawContext DrawContext, float partialTick, int mouseX, int mouseY) {
        DrawContext.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xC0101010);
        DrawContext.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + 13, 0xC0000000);
    }

    @Override
    protected void drawForeground(DrawContext DrawContext, int mouseX, int mouseY) {
        DrawContext.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        DrawContext.drawText(this.textRenderer, this.playerInventoryTitle,
                this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    // [线性-O03] 初始化所有反射缓存（只执行一次）
    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        try {
            // 缓存 Cobblemon 实例
            var cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            cachedCobblemonInstance = cobblemonClass.getField("INSTANCE").get(null);
            cachedGetStorageMethod = cachedCobblemonInstance.getClass().getMethod("getStorage");

            // 缓存 Party 方法
            var storageClass = cachedGetStorageMethod.getReturnType();
            cachedGetPartyMethod = storageClass.getMethod("getParty",
                    Class.forName("net.minecraft.entity.player.PlayerEntity"));

            // 缓存 Pokemon 图标方法
            Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            getIconMethod = pokemonClass.getMethod("getIcon");
            Class<?> guiUtilsClass = Class.forName("com.cobblemon.mod.common.api.gui.GuiUtilsKt");
            guiBlitkMethod = guiUtilsClass.getMethod("blitk",
                    net.minecraft.client.util.math.MatrixStack.class,
                    Identifier.class,
                    Number.class, Number.class);

            iconReflectionReady = true;
            reflectionInitialized = true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[HeldItemScreen] Icon reflection failed: {}", e.getMessage());
        }
    }

    private void drawPokemonIcons(DrawContext DrawContext) {
        if (!reflectionInitialized) initReflection();
        if (!iconReflectionReady || client == null || client.player == null) return;

        try {
            // [线性-O03] 使用缓存的反射获取 party
            var storage = cachedGetStorageMethod.invoke(cachedCobblemonInstance);
            var party = cachedGetPartyMethod.invoke(storage, client.player);
            if (party == null) return;

            // 缓存 party 的 size/get 方法（首次调用时缓存）
            if (cachedPartySizeMethod == null) {
                cachedPartySizeMethod = party.getClass().getMethod("size");
                cachedPartyGetMethod = party.getClass().getMethod("get", int.class);
            }

            int size = (Integer) cachedPartySizeMethod.invoke(party);

            for (int i = 0; i < Math.min(size, 6); i++) {
                var pokemon = cachedPartyGetMethod.invoke(party, i);
                if (pokemon == null) continue;

                try {
                    Object icon = getIconMethod.invoke(pokemon);
                    if (icon instanceof Identifier iconRL) {
                        int sx = this.x + 80 + i * 20 + 2;
                        int sy = this.y + 20 + 1;
                        guiBlitkMethod.invoke(null, DrawContext.getMatrices(), iconRL, sx, sy);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[HeldItemScreen] drawIcons failed: {}", e.toString());
        }
    }
}