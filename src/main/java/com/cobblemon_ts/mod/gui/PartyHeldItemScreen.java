package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.lang.reflect.Method;

public class PartyHeldItemScreen extends AbstractContainerScreen<PartyHeldItemContainer> {

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

    public PartyHeldItemScreen(PartyHeldItemContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        drawPokemonIcons(guiGraphics);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 13, 0xC0000000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
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
                    Class.forName("net.minecraft.world.entity.player.Player"));

            // 缓存 Pokemon 图标方法
            Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            getIconMethod = pokemonClass.getMethod("getIcon");
            Class<?> guiUtilsClass = Class.forName("com.cobblemon.mod.common.api.gui.GuiUtilsKt");
            guiBlitkMethod = guiUtilsClass.getMethod("blitk",
                    com.mojang.blaze3d.vertex.PoseStack.class,
                    ResourceLocation.class,
                    Number.class, Number.class);

            iconReflectionReady = true;
            reflectionInitialized = true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[HeldItemScreen] Icon reflection failed: {}", e.getMessage());
        }
    }

    private void drawPokemonIcons(GuiGraphics guiGraphics) {
        if (!reflectionInitialized) initReflection();
        if (!iconReflectionReady || minecraft == null || minecraft.player == null) return;

        try {
            // [线性-O03] 使用缓存的反射获取 party
            var storage = cachedGetStorageMethod.invoke(cachedCobblemonInstance);
            var party = cachedGetPartyMethod.invoke(storage, minecraft.player);
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
                    if (icon instanceof ResourceLocation iconRL) {
                        int sx = leftPos + 80 + i * 20 + 2;
                        int sy = topPos + 20 + 1;
                        guiBlitkMethod.invoke(null, guiGraphics.pose(), iconRL, sx, sy);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[HeldItemScreen] drawIcons failed: {}", e.toString());
        }
    }
}