package com.cobblemon_ts.mod.gui.battle;

import com.cobblemon.mod.common.battles.HealItemActionResponse;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleBackButton;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import com.cobblemon.mod.common.item.battle.BagItem;
import com.cobblemon.mod.common.item.battle.BagItemLike;
import com.cobblemon_ts.mod.network.ThrowPokeBallPacket;
import com.cobblemon_ts.mod.network.RequestPokebagContentsPacket;
import com.cobblemon_ts.mod.network.SyncPokebagContentsPacket;
import com.cobblemon_ts.mod.network.PokebagClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

// [线性-B05x] 精灵球背包GUI作为BattleActionSelection嵌入BattleGUI
// 使用Cobblemon原生changeActionSelection切换，带返回按键+翻页按键
// [线性-B05zd] 每格92x23，6格纵向堆叠(138px)，png/png1做普通/悬停切换
// 翻页按键battle_next.png仅在pkbag界面显示，与返回键对称居中
// 位置：x=12左对齐道具tile(左下角)，底边对齐道具tile底边(guiScaledH-30)
// [线性-B05ze] 物品分类显示：捕捉(#cobblemon:poke_balls)、恢复(#cobblemon:revives等)、战斗道具(#cobblemon:battle_items)
// 点击使用：球类→缩小战斗界面+切精灵球到手中投掷，恢复/战斗道具→HealItemActionResponse发送使用指令
public class PkBagActionSelection extends BattleActionSelection {

    private static final ResourceLocation PKBAG = ResourceLocation.parse("coblemon_ts:textures/gui/pkbag.png");
    private static final ResourceLocation PKBAG_HOVER = ResourceLocation.parse("coblemon_ts:textures/gui/pkbag1.png");
    private static final ResourceLocation BATTLE_NEXT = ResourceLocation.parse("coblemon_ts:textures/gui/battle_next.png");

    private static final int SLOT_W = 92;
    private static final int SLOT_H = 23;
    private static final int SLOTS_PER_PAGE = 6;
    private static final int PANEL_W = 92;
    private static final int PANEL_H = SLOT_H * SLOTS_PER_PAGE;  // 138

    // [线性-B05zd] 下一页按键尺寸
    private static final int NEXT_W = 58;
    private static final int NEXT_H = 68;

    // [线性-B05zc] 道具tile(index=2): x=12, bottom=guiScaledH-30
    private static final int ITEMS_TILE_X = 12;
    private static final int ITEMS_TILE_BOTTOM_OFFSET = 30;

    // [线性-B05ze] 物品分类枚举 — 每个分类对应Cobblemon的物品标签
    public enum ItemCategory {
        BATTLE_ITEMS("#cobblemon:battle_items"),
        RESTORES("#cobblemon:revives", "#cobblemon:restores", "#cobblemon:potions", "#cobblemon:ethers"),
        CAPTURE("#cobblemon:poke_balls");

        final List<String> tagIds;

        ItemCategory(String... tagIds) {
            this.tagIds = List.of(tagIds);
        }
    }

    private final BattleBackButton backButton;
    private final int panelX;
    private final int panelY;

    // [线性-B05zd] 下一页按键位置（取中相对返回键：返回键x=12左，下一页x=12+92+4右）
    private final int nextX;
    private final int nextY;

    private int currentPage = 0;
    private int totalPages = 1;

    // [线性-B05ze] 当前显示的分类，由BattleChangeMenuActionSelection点击按钮时设置
    private ItemCategory currentCategory = ItemCategory.CAPTURE;

    public PkBagActionSelection(BattleGUI battleGUI, SingleActionRequest request) {
        super(battleGUI, request, 0, 0, battleGUI.width, battleGUI.height, Component.literal("精灵球背包"));
        this.backButton = new BattleBackButton(12.0f, battleGUI.height - 32.0f);

        // [线性-B05zd] 面板x=12左对齐道具tile，底边对齐道具tile底边(guiScaledH-30)
        this.panelX = ITEMS_TILE_X;
        this.panelY = battleGUI.height - ITEMS_TILE_BOTTOM_OFFSET - PANEL_H;

        // [线性-B05zd] 下一页按键：与返回键取中相对位置，返回键在面板左侧x=12，下一页在面板右侧
        this.nextX = panelX + PANEL_W + 4;
        this.nextY = battleGUI.height - 32 - NEXT_H;  // 底边对齐屏幕底-32（与返回键同底边）

        // [线性-B05zf] 请求服务端同步Pokebag库存到客户端缓存
        PacketDistributor.sendToServer(new RequestPokebagContentsPacket());

        // [线性-B05ze] 初始化时更新总页数
        updateTotalPages();
    }

    // [线性-B05ze] 设置当前分类并重置页码，由BattleChangeMenuActionSelection调用
    public void setCurrentCategory(ItemCategory category) {
        this.currentCategory = category;
        this.currentPage = 0;
        updateTotalPages();
    }

    // [线性-B05ze] 获取当前分类的物品列表，从玩家背包+Pokebag客户端缓存中筛选
    private List<ItemStack> getItemsForCurrentCategory() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return List.of();

        List<ItemStack> result = new ArrayList<>();
        // [线性-B05ze] 遍历玩家背包所有物品
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && isItemInCurrentCategory(stack)) {
                result.add(stack);
            }
        }
        // [线性-B05zf] 遍历Pokebag客户端缓存（服务端同步过来的数据）
        for (SyncPokebagContentsPacket.ItemEntry entry : PokebagClientCache.getItems()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.itemId()));
            if (item != null) {
                ItemStack stack = new ItemStack(item, entry.count());
                if (isItemInCurrentCategory(stack)) {
                    result.add(stack);
                }
            }
        }
        return result;
    }

    // [线性-B05ze] 检查物品是否属于当前分类的标签
    private boolean isItemInCurrentCategory(ItemStack stack) {
        return currentCategory.tagIds.stream().anyMatch(tagId -> {
            // [线性-B05ze] tagId格式为"#cobblemon:poke_balls"，去掉#前缀后解析为ResourceLocation
            String cleanId = tagId.startsWith("#") ? tagId.substring(1) : tagId;
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(cleanId));
            return stack.is(tag);
        });
    }

    // [线性-B05ze] 更新总页数（每页SLOTS_PER_PAGE个物品）
    private void updateTotalPages() {
        List<ItemStack> items = getItemsForCurrentCategory();
        this.totalPages = Math.max(1, (items.size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<ItemStack> items = getItemsForCurrentCategory();
        int startIndex = currentPage * SLOTS_PER_PAGE;
        int hoveredSlot = getHoveredSlot(mouseX, mouseY);

        // [线性-B05ze] 渲染6格槽位，悬停格用pkbag1.png，否则用pkbag.png
        // 每格内渲染物品图标+名称
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int slotY = panelY + i * SLOT_H;
            int itemIndex = startIndex + i;
            ResourceLocation tex = (i == hoveredSlot && itemIndex < items.size()) ? PKBAG_HOVER : PKBAG;
            graphics.blit(tex, panelX, slotY, 0, 0, SLOT_W, SLOT_H, SLOT_W, SLOT_H);

            // [线性-B05ze] 渲染物品图标和名称
            if (itemIndex < items.size()) {
                ItemStack stack = items.get(itemIndex);
                // 物品图标
                graphics.renderItem(stack, panelX + 4, slotY + 3);
                // 物品数量
                graphics.renderItemDecorations(Minecraft.getInstance().font, stack, panelX + 4, slotY + 3);
                // 物品名称
                graphics.drawString(Minecraft.getInstance().font,
                        stack.getHoverName().getString(),
                        panelX + 24, slotY + 7, 0xFFFFFF);
            }
        }

        // 返回按键
        this.backButton.render(graphics, mouseX, mouseY, partialTick);

        // [线性-B05zd] 仅当有下一页时渲染翻页按键
        if (currentPage < totalPages - 1) {
            graphics.blit(BATTLE_NEXT, nextX, nextY, 0, 0, NEXT_W, NEXT_H, NEXT_W, NEXT_H);
        }

        // [线性-B05ze] 渲染页码指示器
        if (totalPages > 1) {
            String pageText = (currentPage + 1) + "/" + totalPages;
            int textW = Minecraft.getInstance().font.width(pageText);
            graphics.drawString(Minecraft.getInstance().font, pageText,
                    panelX + (PANEL_W - textW) / 2,
                    panelY + PANEL_H + 4, 0xAAAAAA);
        }
    }

    // [线性-B05zd] 检测鼠标悬停在第几格（0~5），-1=无
    private int getHoveredSlot(int mouseX, int mouseY) {
        if (mouseX < panelX || mouseX > panelX + SLOT_W) return -1;
        int relY = mouseY - panelY;
        if (relY < 0 || relY >= PANEL_H) return -1;
        return relY / SLOT_H;
    }

    // [线性-B05zd] 检测下一页按键是否悬停
    private boolean isNextHovered(double mouseX, double mouseY) {
        if (currentPage >= totalPages - 1) return false;
        return mouseX >= nextX && mouseX <= nextX + NEXT_W
                && mouseY >= nextY && mouseY <= nextY + NEXT_H;
    }

    // [线性-B05ze] 点击处理：返回按键→三选一菜单，下一页→翻页，外部区域→三选一菜单，槽位→使用物品
    @Override
    public boolean mousePrimaryClicked(double mouseX, double mouseY) {
        if (this.backButton.isHovered(mouseX, mouseY)) {
            // 返回三选一菜单
            getBattleGUI().changeActionSelection(
                    new BattleChangeMenuActionSelection(getBattleGUI(), getRequest()));
            return true;
        }

        // [线性-B05zd] 下一页按键
        if (isNextHovered(mouseX, mouseY)) {
            currentPage++;
            return true;
        }

        // [线性-B05ze] 点击槽位 → 使用物品
        int slot = getHoveredSlot((int) mouseX, (int) mouseY);
        if (slot != -1) {
            List<ItemStack> items = getItemsForCurrentCategory();
            int index = currentPage * SLOTS_PER_PAGE + slot;
            if (index < items.size()) {
                useItem(items.get(index));
                return true;
            }
        }

        // [线性-B05zd] 点击面板外部区域 → 回三选一菜单（与返回键行为一致）
        if (mouseX < panelX || mouseX > panelX + PANEL_W || mouseY < panelY || mouseY > panelY + PANEL_H) {
            getBattleGUI().changeActionSelection(
                    new BattleChangeMenuActionSelection(getBattleGUI(), getRequest()));
            return true;
        }

        return false;
    }

    // [线性-B05ze] 使用物品：捕捉类→缩小战斗界面+切精灵球到手中，恢复/战斗道具→发送HealItemActionResponse
    private void useItem(ItemStack stack) {
        if (currentCategory == ItemCategory.CAPTURE) {
            // [线性-B05ze] 球类 → 缩小战斗界面，切换精灵球到手中，玩家投掷触发捕捉
            usePokeball(stack);
        } else {
            // [线性-B05ze] 恢复/战斗道具 → 通过HealItemActionResponse发送使用指令
            // Cobblemon原版拿道具右键精灵的事件，由showdown处理
            useBagItem(stack);
        }
    }

    // [线性-B05ze] 精灵球使用：直接发网络包→服务端Invoker调用throwPokeBall()投掷精灵球
    // 跳过gameMode.useItem()→ServerboundUseItemPacket→PokeBallItem.use()这条长链路
    private void usePokeball(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        // 发送自定义包→服务端handleServerSide→PokeBallItemInvoker.invokeThrowPokeBall()→捕捉
        PacketDistributor.sendToServer(new ThrowPokeBallPacket(itemId));
    }

    // [线性-B05ze] 恢复/战斗道具使用：通过BagItemLike获取BagItem，发送HealItemActionResponse
    private void useBagItem(ItemStack stack) {
        if (stack.getItem() instanceof BagItemLike bagItemLike) {
            BagItem bagItem = bagItemLike.getBagItem(stack);
            String itemName = bagItem.getItemName();
            HealItemActionResponse response = new HealItemActionResponse(itemName);
            // [线性-B05ze] 通过BattleGUI.selectAction发送响应，Cobblemon原版处理流程
            getBattleGUI().selectAction(getRequest(), response);
        }
    }
}