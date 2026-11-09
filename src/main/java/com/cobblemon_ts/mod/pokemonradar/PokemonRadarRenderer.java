// [线性-R05] 精灵雷达渲染器：绘制匹配精灵的碰撞箱方框 + 神兽模式悬停提示
package com.cobblemon_ts.mod.pokemonradar;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = CobblemonTS.MODID, value = Dist.CLIENT)
public class PokemonRadarRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // [线性-R05a] 只在AFTER_TRANSLUCENT_BLOCKS阶段渲染，确保框在实体之上
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        // [线性-R05b] 检查雷达总开关
        if (!CobblemonTSConfig.POKEMON_RADAR_ENABLED.get()) return;

        PokemonRadarState state = PokemonRadarState.getInstance();
        if (!state.isAnyModeActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        float frameTick = mc.getTimer().getGameTimeDeltaPartialTick(false);

        // [线性-R05d2] 玩家插值中心(同帧与实体同步，无晃动)
        Vec3 pPos = mc.player.getPosition(frameTick);
        double pcx = pPos.x - camPos.x;
        double pcy = pPos.y + mc.player.getEyeHeight() / 2.0 - camPos.y;
        double pcz = pPos.z - camPos.z;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        RenderSystem.disableDepthTest(); // [线性-R05i] 穿透方块渲染

        // [线性-R05c] 遍历所有宝可梦实体（加距离过滤：radarRange内）
        int range = state.getRadarRange();
        double rangeSq = (double) range * range;
        Vec3 playerPos = mc.player.position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pokemonEntity)) continue;

            // [线性-R07] 距离过滤
            if (entity.distanceToSqr(playerPos) > rangeSq) continue;

            List<String> activeModes = state.getActiveModesForEntity(pokemonEntity);
            if (activeModes.isEmpty()) continue;

            // [线性-R05d] AABB→相机坐标
            AABB aabb = pokemonEntity.getBoundingBox();
            float x1 = (float)(aabb.minX - camPos.x), y1 = (float)(aabb.minY - camPos.y), z1 = (float)(aabb.minZ - camPos.z);
            float x2 = (float)(aabb.maxX - camPos.x), y2 = (float)(aabb.maxY - camPos.y), z2 = (float)(aabb.maxZ - camPos.z);
            float cx = (x1 + x2) / 2f, cy = (y1 + y2) / 2f, cz = (z1 + z2) / 2f;

            // [线性-R05e] 方框+连线，同模式一色
            for (String mode : activeModes) {
                int color = state.getColorForMode(mode);
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;

                // 12条线画框(直接cam-relative坐标,无translate)
                drawLine(lineConsumer, poseStack, x1, y1, z1, x2, y1, z1, r, g, b);
                drawLine(lineConsumer, poseStack, x2, y1, z1, x2, y1, z2, r, g, b);
                drawLine(lineConsumer, poseStack, x2, y1, z2, x1, y1, z2, r, g, b);
                drawLine(lineConsumer, poseStack, x1, y1, z2, x1, y1, z1, r, g, b);
                drawLine(lineConsumer, poseStack, x1, y2, z1, x2, y2, z1, r, g, b);
                drawLine(lineConsumer, poseStack, x2, y2, z1, x2, y2, z2, r, g, b);
                drawLine(lineConsumer, poseStack, x2, y2, z2, x1, y2, z2, r, g, b);
                drawLine(lineConsumer, poseStack, x1, y2, z2, x1, y2, z1, r, g, b);
                drawLine(lineConsumer, poseStack, x1, y1, z1, x1, y2, z1, r, g, b);
                drawLine(lineConsumer, poseStack, x2, y1, z1, x2, y2, z1, r, g, b);
                drawLine(lineConsumer, poseStack, x2, y1, z2, x2, y2, z2, r, g, b);
                drawLine(lineConsumer, poseStack, x1, y1, z2, x1, y2, z2, r, g, b);

                // [线性-R05h] 连线: 玩家插值中心→实体插值中心(同frameTick,不晃动)
                drawLine(lineConsumer, poseStack, pcx, pcy, pcz, cx, cy, cz, r, g, b);
            }
        }

        // [线性-R06] NPC训练师追踪：紫色方框，空名=跟随RCT图鉴追踪目标
        // [线性-R06f] 先更新图鉴目标+最近训练师（O(n) 一次）
        state.updateCardTarget(mc.level);
        if (state.isNpcModeActive()) {
            try {
                Class<?> trainerMobClass = Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob");
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (!trainerMobClass.isInstance(entity)) continue;
                    if (!state.matchesNpcFilter(entity)) continue;
                    // [线性-R07] NPC也加距离过滤
                    if (entity.distanceToSqr(playerPos) > rangeSq) continue;

                    AABB aabb = entity.getBoundingBox();
                    double x = aabb.minX - camPos.x;
                    double y = aabb.minY - camPos.y;
                    double z = aabb.minZ - camPos.z;
                    double x2 = aabb.maxX - camPos.x;
                    double y2 = aabb.maxY - camPos.y;
                    double z2 = aabb.maxZ - camPos.z;

                    float r = 0.67f, g = 0.0f, b = 1.0f; // 紫色
                    double w = x2 - x;
                    double h = y2 - y;
                    double d = z2 - z;

                    poseStack.pushPose();
                    poseStack.translate(x, y, z);

                    drawLine(lineConsumer, poseStack, 0, 0, 0, w, 0, 0, r, g, b);
                    drawLine(lineConsumer, poseStack, w, 0, 0, w, 0, d, r, g, b);
                    drawLine(lineConsumer, poseStack, w, 0, d, 0, 0, d, r, g, b);
                    drawLine(lineConsumer, poseStack, 0, 0, d, 0, 0, 0, r, g, b);
                    drawLine(lineConsumer, poseStack, 0, h, 0, w, h, 0, r, g, b);
                    drawLine(lineConsumer, poseStack, w, h, 0, w, h, d, r, g, b);
                    drawLine(lineConsumer, poseStack, w, h, d, 0, h, d, r, g, b);
                    drawLine(lineConsumer, poseStack, 0, h, d, 0, h, 0, r, g, b);
                    drawLine(lineConsumer, poseStack, 0, 0, 0, 0, h, 0, r, g, b);
                    drawLine(lineConsumer, poseStack, w, 0, 0, w, h, 0, r, g, b);
                    drawLine(lineConsumer, poseStack, w, 0, d, w, h, d, r, g, b);
                    drawLine(lineConsumer, poseStack, 0, 0, d, 0, h, d, r, g, b);

                    // 连线: 玩家→实体中心
                    double npx = mc.player.getX() - aabb.minX;
                    double npy = mc.player.getY() - aabb.minY;
                    double npz = mc.player.getZ() - aabb.minZ;
                    drawLine(lineConsumer, poseStack, npx, npy, npz, w / 2, h / 2, d / 2, r, g, b);

                    poseStack.popPose();
                }
            } catch (ClassNotFoundException ignored) {} // zy模组未安装
        }

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.enableDepthTest();
    }

    private static void drawLine(VertexConsumer consumer, PoseStack poseStack,
                                  double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  float r, float g, float b) {
        PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(r, g, b, 1f).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(r, g, b, 1f).setNormal(pose, 0, 1, 0);
    }

    // [线性-R05f] 神兽模式悬停提示：玩家准星对准神兽时显示名字
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiEvent.Post event) {
        if (!CobblemonTSConfig.POKEMON_RADAR_ENABLED.get()) return;

        PokemonRadarState state = PokemonRadarState.getInstance();
        if (!state.isLegendaryMode()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.hitResult == null) return;
        if (mc.hitResult.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult entityHit = (EntityHitResult) mc.hitResult;
        Entity target = entityHit.getEntity();
        if (!(target instanceof PokemonEntity pokemonEntity)) return;

        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon == null || pokemon.getSpecies() == null) return;
        if (!PokemonRadarState.isLegendarySpecies(pokemon.getSpecies())) return;

        // [线性-R05g] 显示简易名称提示
        String name = pokemon.getSpecies().getTranslatedName().getString();
        if (pokemon.getShiny()) {
            name = "✨ " + name;
        }
        Component tooltip = Component.literal(name);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int textW = mc.font.width(tooltip);
        int x = screenW / 2 - textW / 2;
        int y = screenH / 2 - 30;

        event.getGuiGraphics().fill(x - 4, y - 2, x + textW + 4, y + 12, 0xAA000000);
        event.getGuiGraphics().drawString(mc.font, tooltip, x, y, 0xFFFFD700, false);
    }
}