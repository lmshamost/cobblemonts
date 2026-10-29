package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.network.PokebagClientCache;
import com.cobblemon_ts.mod.network.SyncPokebagContentsPacket;
import com.cobblemon_ts.mod.ui.PartyUIState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.*;

// [线性-RESP-07] 道具袋物品选择界面 — 多选 + 右键使用
//   - 左键点击：选中/取消选中（单点切换）
//   - Ctrl+左键：多选模式（不取消其他已选）
//   - 右键：使用所有已选物品（UseItemOnPokemonPacket fromPokebag=true → 确认生效 → 扣道具袋）
//   - Shift+左键：已选物品数量+1
//   - Shift+右键：已选物品数量-1
//   - 滚轮：有选中时调整数量(±1)，Shift滚轮(±2)；无选中时翻页
//   - ESC：清除所有选中，再次ESC关闭界面
public class PokebagItemSelectScreen extends Screen {

    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int PANEL_W = 176;

    private final int targetPartySlot;

    // [线性-RESP-07] 多选：Map<槽位索引, 选中数量>
    private final Map<Integer, Integer> selectedSlots = new LinkedHashMap<>();
    private int currentPage = 0;

    public PokebagItemSelectScreen(int targetPartySlot) {
        super(Component.literal("选择道具"));
        this.targetPartySlot = targetPartySlot;
    }

    @Override
    protected void init() {
        super.init();
        PacketDistributor.sendToServer(new com.cobblemon_ts.mod.network.RequestPokebagContentsPacket());
    }

    private int calcRows() {
        int itemCount = PokebagClientCache.getItems().size();
        int rows = Math.max(1, (itemCount + COLS - 1) / COLS);
        return Math.min(rows, 6);
    }

    private int calcPanelHeight(int rows) {
        return 17 + rows * 18 + 7;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

        List<SyncPokebagContentsPacket.ItemEntry> items = PokebagClientCache.getItems();
        int slotsPerPage = COLS * rows;
        int totalPages = Math.max(1, (items.size() + slotsPerPage - 1) / slotsPerPage);
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

        int startIndex = currentPage * slotsPerPage;
        int gridX = panelX + 8;
        int gridY = panelY + 18;

        for (int i = 0; i < slotsPerPage; i++) {
            int itemIndex = startIndex + i;
            int col = i % COLS;
            int row = i / COLS;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;

            // [线性-RESP-07] 多选高亮
            if (selectedSlots.containsKey(itemIndex)) {
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FF0000);
            }

            if (itemIndex < items.size()) {
                var entry = items.get(itemIndex);
                var itemOpt = BuiltInRegistries.ITEM.getOptional(
                        ResourceLocation.tryParse(entry.itemId()));
                if (itemOpt.isPresent()) {
                    ItemStack renderStack = entry.renderStack();
                    if (renderStack == null) {
                        renderStack = new ItemStack(itemOpt.get(), Math.min(entry.count(), 64));
                        entry.setRenderStack(renderStack);
                    }
                    guiGraphics.renderItem(renderStack, slotX + 1, slotY + 1);

                    // [线性-FIX-baguse-06] 用renderItemDecorations(内部Z+200防覆盖)渲染选中数量
                    Integer selCount = selectedSlots.get(itemIndex);
                    if (selCount != null && selCount > 0) {
                        ItemStack countStack = new ItemStack(renderStack.getItem(), selCount);
                        // count=1时原版不显示，手动传入text参数强显
                        guiGraphics.renderItemDecorations(this.font, countStack, slotX + 1, slotY + 1,
                                selCount == 1 ? "1" : null);
                    } else if (entry.count() > 1) {
                        guiGraphics.renderItemDecorations(this.font, renderStack, slotX + 1, slotY + 1);
                    }
                }
            }
        }

        // [线性-RESP-07] 底部信息栏
        int infoY = panelY + panelH + 4;

        if (!selectedSlots.isEmpty()) {
            int totalItems = selectedSlots.size();
            String summary = "§e已选 " + totalItems + " 种物品";
            guiGraphics.drawCenteredString(this.font, Component.literal(summary), this.width / 2, infoY, 0xFFFF00);
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("§7右键使用 | Shift+左键+1 | Shift+右键-1 | 滚轮±1 | Shift滚轮±2"), this.width / 2, infoY + 12, 0x888888);
        } else {
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("§7左键选择 | Ctrl+左键多选 | 右键使用 | 滚轮翻页"), this.width / 2, infoY, 0xAAAAAA);
        }

        if (totalPages > 1) {
            String pageStr = String.format("%d / %d", currentPage + 1, totalPages);
            guiGraphics.drawString(this.font, pageStr, panelX + PANEL_W - 40, panelY + 6, 0xFF888888);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rows = calcRows();
        int panelH = calcPanelHeight(rows);
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = (this.height - panelH) / 2;
        int gridX = panelX + 8;
        int gridY = panelY + 18;

        List<SyncPokebagContentsPacket.ItemEntry> items = PokebagClientCache.getItems();
        int slotsPerPage = COLS * rows;
        int startIndex = currentPage * slotsPerPage;

        if (mouseX >= gridX && mouseX <= gridX + COLS * SLOT_SIZE
                && mouseY >= gridY && mouseY <= gridY + rows * SLOT_SIZE) {
            int col = (int) ((mouseX - gridX) / SLOT_SIZE);
            int row = (int) ((mouseY - gridY) / SLOT_SIZE);
            if (col >= 0 && col < COLS && row >= 0 && row < rows) {
                int slotIndex = startIndex + row * COLS + col;
                if (slotIndex >= items.size()) return true;

                boolean ctrl = Screen.hasControlDown();
                boolean shift = Screen.hasShiftDown();
                int maxCount = items.get(slotIndex).count();

                if (button == 0) {
                    // ── 左键 ──
                    if (shift && selectedSlots.containsKey(slotIndex)) {
                        // [线性-RESP-07] Shift+左键：已选物品+1
                        int cur = selectedSlots.get(slotIndex);
                        selectedSlots.put(slotIndex, Math.min(maxCount, cur + 1));
                    } else if (selectedSlots.containsKey(slotIndex)) {
                        // [线性-RESP-07] 点击已选中：取消选中
                        selectedSlots.remove(slotIndex);
                    } else {
                        // [线性-RESP-07] 点击未选中：选中（Ctrl不清空其他，否则清空）
                        if (!ctrl) selectedSlots.clear();
                        selectedSlots.put(slotIndex, 1);
                    }
                    return true;
                } else if (button == 1) {
                    // ── 右键 ──
                    if (shift && selectedSlots.containsKey(slotIndex)) {
                        // [线性-RESP-07] Shift+右键：已选物品-1
                        int cur = selectedSlots.get(slotIndex);
                        if (cur <= 1) {
                            selectedSlots.remove(slotIndex);
                        } else {
                            selectedSlots.put(slotIndex, cur - 1);
                        }
                    } else if (!selectedSlots.isEmpty()) {
                        // [线性-RESP-07] 右键：使用所有已选物品
                        useSelectedItems();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<SyncPokebagContentsPacket.ItemEntry> items = PokebagClientCache.getItems();

        if (!selectedSlots.isEmpty()) {
            // [线性-RESP-07] 有选中：调整所有已选物品数量
            boolean shift = Screen.hasShiftDown();
            int delta = scrollY > 0 ? 1 : -1;
            if (shift) delta *= 2;

            List<Integer> toRemove = new ArrayList<>();
            for (var entry : selectedSlots.entrySet()) {
                int slot = entry.getKey();
                int cur = entry.getValue();
                if (slot < 0 || slot >= items.size()) continue;
                int max = items.get(slot).count();
                int next = cur + delta;
                if (next < 1) {
                    toRemove.add(slot);
                } else {
                    selectedSlots.put(slot, Math.min(max, next));
                }
            }
            toRemove.forEach(selectedSlots::remove);
        } else {
            // [线性-RESP-07] 无选中：翻页
            int rows = calcRows();
            int slotsPerPage = COLS * rows;
            int totalPages = Math.max(1, (items.size() + slotsPerPage - 1) / slotsPerPage);
            if (scrollY > 0) {
                currentPage = Math.max(0, currentPage - 1);
            } else {
                currentPage = Math.min(totalPages - 1, currentPage + 1);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!selectedSlots.isEmpty()) {
                // [线性-RESP-07] 第一次ESC：清除所有选中
                selectedSlots.clear();
                return true;
            } else {
                this.onClose();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // [线性-RESP-07] 使用所有已选物品
    //   UseItemOnPokemonPacket(fromPokebag=true) — 完整use逻辑 + 生效后才从道具袋扣库存（原子操作）
    private void useSelectedItems() {
        List<SyncPokebagContentsPacket.ItemEntry> items = PokebagClientCache.getItems();
        if (selectedSlots.isEmpty()) return;

        for (var entry : selectedSlots.entrySet()) {
            int slot = entry.getKey();
            int count = entry.getValue();
            if (slot < 0 || slot >= items.size()) continue;
            var item = items.get(slot);
            var itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item.itemId()));
            if (itemOpt.isEmpty()) continue;

            // [线性-FIX-baguse-04] 单包原子：UseItemOnPokemonPacket(fromPokebag=true)
            //   内部确认生效后才调用deductFromPokebag → 不生效不扣 → 火之石给超梦不扣
            ItemStack stack = new ItemStack(itemOpt.get(), count);
            PartyUIState.handleUseItemOnPokemon(targetPartySlot, stack, null, true);
        }

        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}