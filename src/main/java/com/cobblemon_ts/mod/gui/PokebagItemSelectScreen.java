package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.network.PokebagClientCache;
import com.cobblemon_ts.mod.network.SyncPokebagContentsPacket;
import com.cobblemon_ts.mod.network.UsePokebagItemPacket;
import com.cobblemon_ts.mod.ui.PartyUIState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// [线性-C02] 道具袋物品选择界面（独立Screen，原版箱子GUI风格，无玩家物品栏）
// 用于精灵栏右键菜单"使用道具"时打开，选择道具袋中的物品对精灵使用
public class PokebagItemSelectScreen extends Screen {

    // [线性-C02] 原版箱子纹理
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    // 渲染常量
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int PANEL_W = 176;  // 原版箱子宽度

    // 状态
    private final int targetPartySlot;
    private int selectedSlot = -1;
    private int selectCount = 1;
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

    // [线性-C02] 根据道具袋物品数量计算行数（最小1行，最大6行）
    private int calcRows() {
        int itemCount = PokebagClientCache.getItems().size();
        int rows = Math.max(1, (itemCount + COLS - 1) / COLS);
        return Math.min(rows, 6); // 最多6行
    }

    // [线性-C02] 根据行数计算面板高度（只渲染道具袋部分，不渲染玩家物品栏）
    private int calcPanelHeight(int rows) {
        // 原版箱子：标题17px + 行数*18px + 底部间距7px
        return 17 + rows * 18 + 7;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // [线性-C02] 重写为空，禁用Screen默认的背景模糊/暗化
        // 我们自己在render里绘制背景
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // [线性-C02] 禁用背景模糊，只绘制半透明黑色遮罩
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        int rows = calcRows();
        int panelH = calcPanelHeight(rows);

        // 面板居中
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = (this.height - panelH) / 2;

        // [线性-C02] 绘制原版箱子纹理背景（只绘制道具袋部分，不绘制玩家物品栏）
        // 上半部分：标题 + 道具袋槽位
        guiGraphics.blit(CHEST_TEXTURE, panelX, panelY, 0, 0, PANEL_W, rows * 18 + 17);
        // 底部边缘线（从纹理的125位置取7px高的底部边缘）
        guiGraphics.blit(CHEST_TEXTURE, panelX, panelY + rows * 18 + 17, 0, 125, PANEL_W, 7);

        // 标题
        guiGraphics.drawString(this.font, this.title, panelX + 8, panelY + 6, 0x404040, false);

        // 获取道具袋物品
        List<SyncPokebagContentsPacket.ItemEntry> items = PokebagClientCache.getItems();
        int slotsPerPage = COLS * rows;
        int totalPages = Math.max(1, (items.size() + slotsPerPage - 1) / slotsPerPage);
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

        int startIndex = currentPage * slotsPerPage;

        // [线性-C02] 物品网格
        int gridX = panelX + 8;
        int gridY = panelY + 18;

        for (int i = 0; i < slotsPerPage; i++) {
            int itemIndex = startIndex + i;
            int col = i % COLS;
            int row = i / COLS;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;

            // [线性-C02] 选中高亮（红色覆盖层）
            if (itemIndex == selectedSlot) {
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FF0000);
            }

            if (itemIndex < items.size()) {
                var entry = items.get(itemIndex);
                // [线性-O06] 避免每帧创建ResourceLocation，使用parse缓存
                var itemOpt = BuiltInRegistries.ITEM.getOptional(
                        ResourceLocation.tryParse(entry.itemId()));
                if (itemOpt.isPresent()) {
                    // [线性-O06] 避免每帧创建ItemStack，使用单例stack渲染
                    ItemStack renderStack = entry.renderStack();
                    if (renderStack == null) {
                        renderStack = new ItemStack(itemOpt.get(), Math.min(entry.count(), 64));
                        entry.setRenderStack(renderStack);
                    }
                    guiGraphics.renderItem(renderStack, slotX + 1, slotY + 1);
                    if (entry.count() > 1) {
                        guiGraphics.renderItemDecorations(this.font, renderStack, slotX + 1, slotY + 1);
                    }
                }
            }
        }

        // [线性-C02] 底部信息栏（在面板下方）
        int infoY = panelY + panelH + 4;

        if (selectedSlot >= 0 && selectedSlot < items.size()) {
            var entry = items.get(selectedSlot);
            // [线性-O06] 避免每帧创建ResourceLocation和ItemStack
            var itemOpt = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.tryParse(entry.itemId()));
            String itemName = entry.cachedName();
            if (itemName == null) {
                itemName = itemOpt.map(i -> i.getName(new ItemStack(i)).getString()).orElse("未知物品");
                entry.setCachedName(itemName);
            }
            String info = String.format("%s  x%d / %d", itemName, selectCount, entry.count());
            guiGraphics.drawCenteredString(this.font, info, this.width / 2, infoY, 0xFFFFFF00);
            guiGraphics.drawCenteredString(this.font, "左键+1  Ctrl+左键-1  Shift+左键+10  滚轮±1  Shift滚轮±2", this.width / 2, infoY + 12, 0xFF888888);
        } else {
            guiGraphics.drawCenteredString(this.font, "左键选择物品，右键使用", this.width / 2, infoY, 0xFFAAAAAA);
        }

        // 页码
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

                if (button == 0) {
                    if (slotIndex < items.size()) {
                        if (selectedSlot == slotIndex) {
                            boolean ctrl = GLFW.glfwGetKey(this.minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                                    || GLFW.glfwGetKey(this.minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                            boolean shift = hasShiftDown();

                            if (ctrl) {
                                selectCount = Math.max(1, selectCount - 1);
                            } else if (shift) {
                                selectCount = Math.min(items.get(slotIndex).count(), selectCount + 10);
                            } else {
                                selectCount = Math.min(items.get(slotIndex).count(), selectCount + 1);
                            }
                        } else {
                            selectedSlot = slotIndex;
                            selectCount = 1;
                        }
                    }
                    return true;
                } else if (button == 1) {
                    if (slotIndex == selectedSlot && slotIndex < items.size()) {
                        useSelectedItem();
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

        if (selectedSlot >= 0 && selectedSlot < items.size()) {
            int maxCount = items.get(selectedSlot).count();
            boolean shift = hasShiftDown();
            int delta = scrollY > 0 ? 1 : -1;
            if (shift) delta *= 2;
            selectCount = Math.max(1, Math.min(maxCount, selectCount + delta));
        } else {
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
            if (selectedSlot >= 0) {
                selectedSlot = -1;
                selectCount = 1;
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

    private void useSelectedItem() {
        List<SyncPokebagContentsPacket.ItemEntry> items = PokebagClientCache.getItems();
        if (selectedSlot < 0 || selectedSlot >= items.size()) return;

        var entry = items.get(selectedSlot);
        var itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entry.itemId()));
        if (itemOpt.isEmpty()) return;

        ItemStack stack = new ItemStack(itemOpt.get(), selectCount);
        PartyUIState.handleUseItemOnPokemon(targetPartySlot, stack, null);

        PacketDistributor.sendToServer(
                new UsePokebagItemPacket(targetPartySlot, entry.itemId(), selectCount));

        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}