package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.ui.PartyUIState;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 精灵栏和护符栏的渲染 Mixin
 * Mixin 到 InventoryScreen 和 EffectRenderingInventoryScreen (NeoForge 添加的效果图标的子类)
 * 因为游戏在有/无药水效果时创建不同的实例
 */
@Mixin({net.minecraft.client.gui.screens.inventory.InventoryScreen.class,
         net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen.class,
         net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class})
public abstract class InventoryScreenMixin extends AbstractContainerScreen {

    private InventoryScreenMixin() {
        super(null, null, null);
    }

    // ==================== init ====================
    @Inject(method = "init()V", at = @At("TAIL"), require = 0)
    private void init(CallbackInfo ci) {
        if (!PartyUIState.isValidScreen(this)) return;
        PartyUIState.State state = PartyUIState.getState(this);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            state.charmManager = CharmPlayerData.get(player).getCharmSlotManager();
        }
        // 重置临时交互状态（保留动画/护符/隐藏状态，跨打开保持用户选择）
        PartyUIState.resetInteractionState(state);
        PartyUIState.initBasicReflection();
        PartyUIState.invalidatePartyCache();
    }

    // ==================== render ====================
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), require = 0)
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY,
                        @SuppressWarnings("unused") float partialTick, CallbackInfo ci) {
        if (!PartyUIState.isValidScreen(this)) return;

        PartyUIState.State state = PartyUIState.getState(this);

        if (state.charmManager == null) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                state.charmManager = CharmPlayerData.get(player).getCharmSlotManager();
            }
        }

        int leftPos = this.leftPos;
        int topPos = this.topPos;
        int fullPanelHeight = PartyUIState.getPanelHeight();

        // ===== 关键修复：先更新动画，再计算所有坐标！防止坐标随动画波动 =====
        updateCharmDrawerAnimation(state);

        // ===== 开关位置计算（固定偏移，不依赖字体宽度，确保点击和渲染坐标一致） =====
        int partyPanelX = PartyUIState.getPartyPanelX(leftPos);
        int TOGGLE_SIZE = 12;
        int TOGGLE_Y = topPos + 2;

        // 精灵栏开关：精灵栏右侧（不与"队伍"重叠，队伍居中在 30，宽度~12，到36）
        int partyToggleX = partyPanelX + PartyUIState.PARTY_WIDTH - TOGGLE_SIZE - 4;

        // 护符开关：贴着精灵栏左边外侧
        int charmToggleX = partyPanelX - TOGGLE_SIZE - 2;

        // ===== 绘制右键菜单（白字黑底，动态宽度） =====
        if (state.contextMenuOpen) {
            drawContextMenu(guiGraphics, mouseX, mouseY);
        }

        // ===== 绘制护符抽屉 =====
        if (state.charmDrawerOffset > 0) {
            int charmDrawerX = PartyUIState.getCharmDrawerX(state, leftPos);
            drawCharmDrawer(guiGraphics, mouseX, mouseY, charmDrawerX, topPos, fullPanelHeight);
        }

        // ===== 绘制精灵栏（支持隐藏） =====
        if (!state.partyHidden) {
            drawPartyPanel(guiGraphics, mouseX, mouseY, partyPanelX, topPos, fullPanelHeight, state);
        }
        // partyHidden=true 时完全不绘制（包括窄条和"队伍"文字也一起隐藏）

        // ===== 绘制两个小正方形开关按钮 =====
        drawToggleButton(guiGraphics, mouseX, mouseY, charmToggleX, TOGGLE_Y, TOGGLE_SIZE, state.charmDrawerOut);
        drawToggleButton(guiGraphics, mouseX, mouseY, partyToggleX, TOGGLE_Y, TOGGLE_SIZE, !state.partyHidden);

        // ===== 绘制携带物 tooltip =====
        if (!state.partyHidden) {
            drawPartyItemTooltips(guiGraphics, mouseX, mouseY, partyPanelX, topPos);
        }
    }

    // ===== 绘制小正方形开关按钮 =====
    private void drawToggleButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                   int x, int y, int size, boolean active) {
        // 配置隐藏：开关仍可点击（交互逻辑在 mouseClicked 中独立判断），但视觉上完全透明
        if (Boolean.TRUE.equals(CobblemonTSConfig.HIDE_TOGGLE_BUTTONS.get())) return;

        boolean isHovering = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        // 外框
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        // 内部：绿色=开启，红色=关闭
        int innerColor;
        if (active) {
            innerColor = isHovering ? 0xFF00DD00 : 0xFF00AA00;  // 绿
        } else {
            innerColor = isHovering ? 0xFFDD0000 : 0xFFAA0000;  // 红
        }
        guiGraphics.fill(x, y, x + size, y + size, innerColor);
    }

    // ===== 隐藏状态下精灵栏的最小化显示（窄条 + "队伍"） =====
    private void drawCollapsedPartyHeader(GuiGraphics guiGraphics, int partyPanelX, int topPos) {
        int panelWidth = PartyUIState.PARTY_WIDTH;
        // 绘制一个小窄条（高度仅标题行）
        guiGraphics.fill(partyPanelX, topPos, partyPanelX + panelWidth, topPos + 18, -856295947);
        guiGraphics.fill(partyPanelX + 1, topPos + 1, partyPanelX + panelWidth - 1, topPos + 17, -855638017);
        guiGraphics.drawCenteredString(this.font, "\u961f\u4f0d",
                partyPanelX + panelWidth / 2, topPos + 5, 0xFFFFFFFF);
    }

    // ==================== containerTick ====================
    @Inject(method = "containerTick()V", at = @At("HEAD"), require = 0)
    private void containerTick(CallbackInfo ci) {
        if (!PartyUIState.isValidScreen(this)) return;
        PartyUIState.State state = PartyUIState.getState(this);
        if (state.ticksTillClick > 0) {
            state.ticksTillClick--;
        }
    }

    // ==================== renderEffects 控制 ====================
    @Inject(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void injectRenderEffects(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!PartyUIState.isValidScreen(this)) return;
        if (Boolean.FALSE.equals(CobblemonTSConfig.SHOW_INVENTORY_EFFECT_ICONS.get())) {
            ci.cancel();
        }
    }

    // ==================== 动画 (与 JAR 完全一致，使用常量) ====================
    private void updateCharmDrawerAnimation(PartyUIState.State state) {
        if (state.charmDrawerOut && state.charmDrawerOffset < PartyUIState.CHARM_DRAWER_MAX_OFFSET) {
            state.charmDrawerOffset += PartyUIState.CHARM_ANIM_STEP;
            if (state.charmDrawerOffset > PartyUIState.CHARM_DRAWER_MAX_OFFSET)
                state.charmDrawerOffset = PartyUIState.CHARM_DRAWER_MAX_OFFSET;
        } else if (!state.charmDrawerOut && state.charmDrawerOffset > 0) {
            state.charmDrawerOffset -= PartyUIState.CHARM_ANIM_STEP;
            if (state.charmDrawerOffset < 0) state.charmDrawerOffset = 0;
        }
    }

    // ==================== 精灵面板绘制 (与 JAR drawPartyPanel 字节码一致) ====================
    private void drawPartyPanel(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                int panelX, int panelY, int panelHeight,
                                PartyUIState.State state) {
        int panelWidth = PartyUIState.PARTY_WIDTH;

        // JAR L235-L236: 外框 -856295947, 内框 -855638017 (浅色亮色面板)
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, -856295947);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, -855638017);

        if (PartyUIState.debugParty()) {
            guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0x44FFFFFF);
        }

        // JAR L82-L104: 居中标题 "队伍"
        guiGraphics.drawCenteredString(this.font, "\u961f\u4f0d",
                panelX + panelWidth / 2, panelY + 4, 0xFFFFFFFF);

        // JAR L105-L111: 获取队伍列表（使用缓存）
        List<Pokemon> party = PartyUIState.getCachedPartyList();

        // JAR L189-L448: 循环绘制6个槽位
        int slotSize = PartyUIState.SLOT_SIZE;
        int slotSpacing = PartyUIState.SLOT_SPACING;
        int startY = panelY + PartyUIState.HEADER_HEIGHT;

        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = (party != null && i < party.size()) ? party.get(i) : null;

            int slotX = panelX + 3;
            int slotY = startY + i * (slotSize + slotSpacing);

            boolean isHovering = mouseX >= slotX && mouseX <= slotX + panelWidth - 6
                    && mouseY >= slotY && mouseY <= slotY + slotSize;

            boolean isSelected = (i == state.selectedPartySlot || i == state.dragSourceSlot);

            int borderColor;
            if (isSelected) {
                borderColor = -16733441;  // JAR L267: 选中边框 0xFF00AAFF
            } else if (isHovering && pokemon != null) {
                borderColor = -1;          // JAR L269: 悬停边框 0xFFFFFFFF
            } else {
                borderColor = -11184811;   // JAR L265: 普通边框 0xFF555555
            }

            guiGraphics.fill(slotX - 1, slotY - 1, slotX + panelWidth - 5, slotY + slotSize + 1, borderColor);

            // JAR L274: 背景 hovered?-573517616:-570425345 (浅色半透明)
            int innerColor = (isHovering && pokemon != null) ? -573517616 : -570425345;
            guiGraphics.fill(slotX, slotY, slotX + panelWidth - 6, slotY + slotSize, innerColor);

            // JAR L439: 绘制槽位内容 (w = SLOT_CONTENT_WIDTH = 52)
            drawPartySlot(guiGraphics, slotX, slotY, PartyUIState.SLOT_CONTENT_WIDTH, slotSize, pokemon, state);
        }
    }

    // ==================== 精灵槽位绘制 (与 JAR drawPartySlot 字节码完全一致) ====================
    private void drawPartySlot(GuiGraphics guiGraphics, int x, int y, int w, int h,
                               Object pokemon, PartyUIState.State state) {
        if (pokemon == null) return;
        if (!(pokemon instanceof com.cobblemon.mod.common.pokemon.Pokemon pkm)) return;

        try {
            // JAR L12-L100: 获取精灵数据
            String name = PartyUIState.getPokemonName(pkm);           // getTranslatedName().getString()
            int level = PartyUIState.getPokemonLevel(pkm);
            int currentHP = PartyUIState.getPokemonCurrentHP(pkm);
            int maxHP = PartyUIState.getPokemonMaxHP(pkm);
            ItemStack heldItem = PartyUIState.getPokemonHeldItem(pkm);

            // JAR L188-L195: modelX = x + w - 20 (分隔线位置)
            int modelX = x + w - 20;

            // JAR L197-L296: 3D模型渲染 (PokemonItem + PoseStack)
            // JAR L235-L247: 模型中心 = x + (modelX - x) / 2 = x + (w - 20) / 2
            int modelCenterX = x + (w - 20) / 2;
            int modelCenterY = y + h / 2 - 1;
            ItemStack pokemonItem = PartyUIState.getPokemonItem(pkm);
            if (!pokemonItem.isEmpty()) {
                try {
                    PoseStack poseStack = guiGraphics.pose();
                    poseStack.pushPose();
                    // JAR L259-L270: translate(modelCenterX, modelCenterY, 100.0F)
                    poseStack.translate(modelCenterX, modelCenterY, 100.0F);
                    // JAR L273-L278: scale(2.0, 2.0, 1.0)
                    poseStack.scale(2.0F, 2.0F, 1.0F);
                    // JAR L281-L288: renderFakeItem(pokemonItem, -8, -8)
                    guiGraphics.renderFakeItem(pokemonItem, -8, -8);
                    poseStack.popPose();
                } catch (Exception e) {
                    CobblemonTS.LOGGER.warn("[PartyUI] PokemonItem render failed: {}", e.toString());
                }
            }

            // JAR L327: 垂直分隔线 -1996488705 (0x88AAAAAA 灰色半透明)
            guiGraphics.fill(modelX, y, modelX + 1, y + h, -1996488705);

            // JAR L335-L422: 状态图标
            ResourceLocation statusIcon = PartyUIState.getPokemonStatusIcon(pkm);
            if (statusIcon != null) {
                // JAR L395-L414: blit(statusIcon, x+2, y+2, 0, 0, 12, 12, 12, 12)
                guiGraphics.blit(statusIcon, x + 2, y + 2, 0, 0, 12, 12, 12, 12);
            }

            // JAR L422-L445: 名称截断 (最多5字符，取后5个)
            if (name.length() > 5) {
                name = name.substring(name.length() - 5);
            }

            // JAR L445-L547: 名称和HP信息 (仅在护符抽屉关闭时显示)
            if (state.charmDrawerOffset == 0) {
                // JAR L452-L488: 名称在槽位左侧，右对齐 (x - 2 - nameWidth)
                // JAR: 名称贴着槽位左边框外侧，右端对齐 x - 2
                int nameWidth = this.font.width(name);
                int nameX = x - 2 - nameWidth;
                // 注意: JAR 调用的是 5 参数 drawString(无 shadow 参数)，默认带阴影。
                // 颜色必须是 ARGB 格式，最高字节 0xFF 确保不透明。
                guiGraphics.drawString(this.font, name, nameX, y + 4, 0xFFFFFFFF);

                // JAR L489-L547: HP信息 (level/currentHP/maxHP) 右对齐，与名称同位置
                // JAR: x - 2 - hpWidth
                String hpInfo = "Lv." + level + " " + currentHP + "/" + maxHP;
                int hpWidth = this.font.width(hpInfo);
                // JAR L350-L352: HP信息 颜色 maxHp>0?5635925:11184810 (绿/灰)
                // 转换为 ARGB：加 0xFF 前缀确保不透明 (原颜色最高字节为0会导致透明)
                int hpColor = maxHP > 0 ? 0xFF55FF55 : 0xFFAAAAAA;
                guiGraphics.drawString(this.font, hpInfo, x - 2 - hpWidth, y + 16, hpColor);
            }

            // JAR L548-L600: 携带物槽位
            int itemX = x + w - 18;
            int itemY = y + 6;

            // JAR L563-L585: 携带物槽位边框 (外层 0xFF333333, 内层 0xFF8B8B8B)
            guiGraphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0xFF333333);
            guiGraphics.fill(itemX, itemY, itemX + 16, itemY + 16, 0xFF8B8B8B);

            // JAR L588-...: 渲染携带物
            if (!heldItem.isEmpty()) {
                guiGraphics.renderItem(heldItem, itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, heldItem, itemX, itemY);
            }

        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to draw party slot: {}", e.getMessage());
        }
    }

    // ==================== 精灵携带物Tooltip (与 JAR drawPartyItemTooltips 一致) ====================
    private void drawPartyItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                       int partyX, int panelY) {
        if (mouseX < partyX || mouseX > partyX + PartyUIState.PARTY_WIDTH) return;

        List<Pokemon> party = PartyUIState.getCachedPartyList();
        if (party == null) return;

        int slotSize = PartyUIState.SLOT_SIZE;
        int slotSpacing = PartyUIState.SLOT_SPACING;
        int startY = panelY + PartyUIState.HEADER_HEIGHT;

        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = i < party.size() ? party.get(i) : null;
            if (pokemon == null) continue;
            int sY = startY + i * (slotSize + slotSpacing);
            int itemSlotX = partyX + 3 + PartyUIState.SLOT_CONTENT_WIDTH - 18;
            int itemSlotY = sY + 6;
            if (mouseX >= itemSlotX - 1 && mouseX <= itemSlotX + 17
                    && mouseY >= itemSlotY - 1 && mouseY <= itemSlotY + 17) {
                try {
                    ItemStack heldItem = PartyUIState.getPokemonHeldItem(pokemon);
                    if (!heldItem.isEmpty()) {
                        guiGraphics.renderTooltip(this.font, heldItem, mouseX, mouseY);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==================== 护符抽屉绘制（与精灵栏像素级对齐：顶部同高，每个护符单元=精灵槽位大小） ====================
    private void drawCharmDrawer(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                 int drawerX, int panelY, int panelHeight) {
        PartyUIState.State state = PartyUIState.getState(this);
        int drawerWidth = state.charmDrawerOffset;

        // 护符栏顶部 = 精灵栏顶部（与精灵栏同一顶部）
        int charmDrawerY = panelY;
        // 总高度与精灵栏相同：标题(16) + 空1槽位(31) + 5×单元(31) + 底部间距(3) = 205
        int totalHeight = PartyUIState.CHARM_HEADER_HEIGHT + 1 * (PartyUIState.SLOT_SIZE + PartyUIState.SLOT_SPACING)
                + PartyUIState.CHARM_TYPES.length * PartyUIState.CHARM_CELL_HEIGHT + PartyUIState.SLOT_SPACING;

        // 外框 -856295947, 内框 -855638017（与精灵面板同款浅色）
        guiGraphics.fill(drawerX, charmDrawerY,
                drawerX + drawerWidth + 2, charmDrawerY + totalHeight, -856295947);
        guiGraphics.fill(drawerX + 1, charmDrawerY + 1,
                drawerX + drawerWidth + 1, charmDrawerY + totalHeight - 1, -855638017);

        // 只有抽屉宽度 > CHARM_DRAWER_MIN_VISIBLE(40) 时才显示内容
        if (drawerWidth <= PartyUIState.CHARM_DRAWER_MIN_VISIBLE) return;

        // ===== 顶部标题："护符"（对齐精灵栏"队伍"） =====
        String title = "\u62a4\u7b26";
        int titleX = drawerX + (drawerWidth - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, charmDrawerY + 4, 0xFFFFFFFF);
        // 标题下方留 1 个精灵槽位大小的空白（对应精灵1的位置）

        // ===== 绘制护符列表：从精灵2的位置开始，每个护符单元 = 31px（与精灵槽位+间距同高） =====
        int contentStartY = charmDrawerY + PartyUIState.getCharmDrawerContentOffset();
        CharmType[] charmTypes = PartyUIState.CHARM_TYPES;
        for (int i = 0; i < charmTypes.length; i++) {
            CharmType type = charmTypes[i];
            CharmState charmState = state.charmManager != null
                    ? state.charmManager.getCharmState(type)
                    : CharmState.DISABLED;

            int cellY = contentStartY + i * PartyUIState.CHARM_CELL_HEIGHT;
            int cellSize = PartyUIState.CHARM_CELL_HEIGHT;  // = 31

            if (charmState != CharmState.DISABLED) {
                // 名称（单元上半部分，居中，按宽度自然截断，不再限制3字）
                String displayName = type.getDisplayName();
                int maxDrawWidth = drawerWidth - 4;
                if (this.font.width(displayName) > maxDrawWidth) {
                    displayName = this.font.plainSubstrByWidth(displayName, maxDrawWidth);
                }
                int nameX = drawerX + (drawerWidth - this.font.width(displayName)) / 2;
                int nameY = cellY + 3;  // 名称在单元顶部附近
                guiGraphics.drawString(this.font, displayName, nameX, nameY, 0xFFFFFFFF);

                // 图标（单元下半部分，居中，标准16px大小，不需要缩小）
                ItemStack icon = new ItemStack(type.getItem());
                int iconSize = 16;
                int iconX = drawerX + (drawerWidth - iconSize) / 2;
                int iconY = cellY + cellSize - iconSize - 3;  // 图标在单元底部附近
                if (!icon.isEmpty()) {
                    guiGraphics.renderItem(icon, iconX, iconY);
                }

                // AVAILABLE 状态：在图标上覆盖半透明遮罩
                if (charmState == CharmState.AVAILABLE) {
                    guiGraphics.fill(iconX - 1, iconY - 1, iconX + iconSize + 1, iconY + iconSize + 1, -2130771968);
                    guiGraphics.drawString(this.font, "\u2715", iconX + iconSize + 2, iconY + 2, 0xFFFFFF00);
                }
            } else {
                // DISABLED 状态：显示 "?" 居中
                int questionX = drawerX + (drawerWidth - this.font.width("?")) / 2;
                int questionY = cellY + cellSize / 2 - 4;
                guiGraphics.drawString(this.font, "?", questionX, questionY, 0xFF666666);
            }
        }
    }

    // ==================== 右键菜单绘制：白字黑底，动态宽度 ====================
    private void drawContextMenu(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PartyUIState.State state = PartyUIState.getState(this);

        Object pokemon = PartyUIState.getPartyPokemon(state.contextMenuSlot);
        if (pokemon == null) return;

        String[] items = {"\u7cbe\u7075\u4fe1\u606f", "\u8bbe\u4e3a\u9996\u4f4d", "\u8fdb\u5316", "\u653e\u5165PC", "\u653e\u751f"};
        int itemH = PartyUIState.CONTEXT_MENU_ITEM_HEIGHT;
        int menuH = items.length * itemH;

        // 动态宽度：最长文字宽度 + 左右padding，缓存到 state 供点击检测使用
        int maxTextWidth = 0;
        for (String item : items) {
            int w = this.font.width(item);
            if (w > maxTextWidth) maxTextWidth = w;
        }
        int menuW = maxTextWidth + 12;
        state.contextMenuWidth = menuW;  // 缓存宽度供交互检测

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 500.0F);

        // 外框：纯黑
        guiGraphics.fill(state.contextMenuX, state.contextMenuY,
                state.contextMenuX + menuW, state.contextMenuY + menuH, 0xFF000000);
        // 内框：深灰黑
        guiGraphics.fill(state.contextMenuX + 1, state.contextMenuY + 1,
                state.contextMenuX + menuW - 1, state.contextMenuY + menuH - 1, 0xFF111111);

        for (int i = 0; i < items.length; i++) {
            int itemY = state.contextMenuY + i * itemH;

            boolean isHovering = mouseX >= state.contextMenuX && mouseX <= state.contextMenuX + menuW
                    && mouseY >= itemY && mouseY <= itemY + itemH;

            if (isHovering) {
                // 悬停项背景：深灰
                guiGraphics.fill(state.contextMenuX + 2, itemY,
                        state.contextMenuX + menuW - 2, itemY + itemH, 0xFF333333);
            }

            // 文字：纯白
            guiGraphics.drawString(this.font, items[i], state.contextMenuX + 6, itemY + 3, 0xFFFFFFFF);
        }

        poseStack.popPose();
    }
}