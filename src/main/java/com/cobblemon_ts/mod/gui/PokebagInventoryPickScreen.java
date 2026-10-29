package com.cobblemon_ts.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// [线性-IPC-01] 物品栏选择器：左键选中/取消，右键确认添加物品ID到白名单/黑名单
//   shows player inventory items (deduplicated by item ID), chest-style render
@OnlyIn(Dist.CLIENT)
public class PokebagInventoryPickScreen extends Screen {

    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int PANEL_W = 176;

    private final boolean isWhitelist;
    private final PokebagTagListScreen parent;
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final List<String> displayIds;

    public PokebagInventoryPickScreen(boolean isWhitelist, PokebagTagListScreen parent) {
        super(Component.translatable("coblemon_ts.pokebag_config.pick_inv"));
        this.isWhitelist = isWhitelist;
        this.parent = parent;

        // [线性-IPC-01] 收集玩家物品栏所有唯一物品ID
        Set<String> ids = new LinkedHashSet<>();
        if (Minecraft.getInstance().player != null) {
            Inventory inv = Minecraft.getInstance().player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    ids.add(key.toString());
                }
            }
        }
        this.displayIds = new ArrayList<>(ids);
    }

    private int calcRows() {
        return Math.max(1, Math.min(6, (displayIds.size() + COLS - 1) / COLS));
    }

    private int calcPanelHeight(int rows) {
        return 17 + rows * 18 + 7;
    }

    @Override
    protected void init() {
        // No widgets needed, all handled in render + mouse handling
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // [线性-IPC-01] 禁用默认模糊，自行绘制半透明黑遮罩
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        int rows = calcRows();
        int panelH = calcPanelHeight(rows);

        int panelX = (this.width - PANEL_W) / 2;
        int panelY = (this.height - panelH) / 2;

        guiGraphics.blit(CHEST_TEXTURE, panelX, panelY, 0, 0, PANEL_W, rows * 18 + 17);
        guiGraphics.blit(CHEST_TEXTURE, panelX, panelY + rows * 18 + 17, 0, 125, PANEL_W, 7);

        guiGraphics.drawString(this.font, this.title, panelX + 8, panelY + 6, 0x404040, false);

        // [线性-IPC-01] 物品网格
        int gridX = panelX + 8;
        int gridY = panelY + 18;

        for (int i = 0; i < displayIds.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;

            String itemId = displayIds.get(i);
            boolean selected = selectedIds.contains(itemId);

            // [线性-IPC-01] 选中高亮：绿色覆盖层（左键选中/取消）
            if (selected) {
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8000FF00);
            }

            var itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(itemId));
            if (itemOpt.isPresent()) {
                ItemStack stack = new ItemStack(itemOpt.get());
                guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
            }
        }

        // [线性-IPC-01] 底部提示
        int infoY = panelY + panelH + 4;
        String hint = "左键选中/取消 | 右键确认添加";
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.pick_hint"),
                this.width / 2, infoY, 0xFFAAAAAA);

        if (!selectedIds.isEmpty()) {
            String selInfo = selectedIds.size() + " 个物品已选中";
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("§a" + selectedIds.size()
                            + " " + Component.translatable("coblemon_ts.pokebag_config.selected").getString()),
                    this.width / 2, infoY + 12, 0xFF55FF55);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rows = calcRows();
        int panelH = calcPanelHeight(rows);
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = (this.height - panelH) / 2;

        int gridX = panelX + 8;
        int gridY = panelY + 18;

        int col = (int) ((mouseX - gridX) / SLOT_SIZE);
        int row = (int) ((mouseY - gridY) / SLOT_SIZE);
        int idx = row * COLS + col;

        if (idx >= 0 && idx < displayIds.size()
                && mouseX >= gridX && mouseX < gridX + COLS * SLOT_SIZE
                && mouseY >= gridY && mouseY < gridY + rows * SLOT_SIZE) {

            String itemId = displayIds.get(idx);

            if (button == 0) {
                // [线性-IPC-02] 左键：选中/取消选中
                if (selectedIds.contains(itemId)) {
                    selectedIds.remove(itemId);
                } else {
                    selectedIds.add(itemId);
                }
                return true;
            } else if (button == 1) {
                // [线性-IPC-03] 右键：确认添加已选中的物品ID到白名单/黑名单
                for (String id : selectedIds) {
                    if (!parent.entries.contains(id)) {
                        parent.entries.add(id);
                    }
                }
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
        }

        // [线性-IPC-04] 点击空白区域返回
        if (button == 1) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}