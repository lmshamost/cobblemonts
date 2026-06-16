package com.cobblemon_ts.mod.gui.battle;

import com.cobblemon.mod.common.battles.HealItemActionResponse;
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
// [线性-B05zi] 槽位渲染pkbag.png(92x23)作为背景，悬停用pkbag1.png
// pkbag.png内右侧有20x20 #f5f5f5区域(x=70~89, y=2~21)，物品材质居中放此区域
// 翻页按键battle_next.png垂直strip渲染（上半=普通，下半=悬停蓝色），与BattleBackButton完全一致(29x17)
// 布局：BattleBackButton(12~41) ← 间距 → "1/3" ← 间距 → battle_next(29x17)
// [线性-B05ze] 物品分类显示：捕捉(#cobblemon:poke_balls)、恢复(#cobblemon:revives等)、战斗道具(#cobblemon:battle_items)
// 点击使用：球类→缩小战斗界面+切精灵球到手中投掷，恢复/战斗道具→HealItemActionResponse发送使用指令
public class PkBagActionSelection extends BattleActionSelection {

    private static final ResourceLocation BATTLE_NEXT = ResourceLocation.parse("coblemon_ts:textures/gui/battle_next.png");
    private static final ResourceLocation PKBAG = ResourceLocation.parse("coblemon_ts:textures/gui/pkbag.png");
    private static final ResourceLocation PKBAG_HOVER = ResourceLocation.parse("coblemon_ts:textures/gui/pkbag1.png");

    private static final int SLOT_W = 92;
    private static final int SLOT_H = 23;
    private static final int SLOTS_PER_PAGE = 6;
    private static final int PANEL_W = 92;
    private static final int PANEL_H = SLOT_H * SLOTS_PER_PAGE;  // 138

    // [线性-B05zh] 下一页按键：纹理58x68垂直strip布局（上半=普通，下半=悬停蓝色）
    // 模仿Cobblemon BattleBackButton的strip渲染：uWidth=58, vHeight=34, hover时vOffset=34
    private static final int NEXT_TEX_W = 58;   // 纹理总宽=渲染宽
    private static final int NEXT_TEX_H = 68;   // 纹理总高=34*2（上下两半）
    private static final int NEXT_RENDER_W = 58; // 渲染宽=全宽（与BattleBackButton一致）
    private static final int NEXT_RENDER_H = 34; // 渲染高=半高（与BattleBackButton一致，vHeight=34）
    private static final int NEXT_HOVER_V = 34;  // 悬停时vOffset=34（下半）
    private static final int SLOT_ICON_X = 72;  // 物品图标X偏移（20x20 #f5f5f5区域(70~89)居中16x16: 70+(20-16)/2=72）
    private static final int SLOT_ICON_Y = 4;   // 物品图标Y偏移（20x20 #f5f5f5区域(2~21)居中16x16: 2+(20-16)/2=4）

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

    // [线性-B05zj] 下一页按键：与BattleBackButton完全一致(29x17)
    // 固定位置：BattleBackButton(12~41) ← gap(9) → "1/3"(50~66) ← gap(9) → battle_next(75~104)
    private int nextX;
    private final int nextY;

    private int currentPage = 0;
    private int totalPages = 1;

    // [线性-B05zk] 自动使用模式开关（静态，跨GUI实例持久）
    public static boolean autoUseEnabled = false;

    // [线性-B05zk] 上次使用的物品缓存（用于自动使用）
    private static String lastUsedItemId = null;
    private static ItemCategory lastUsedCategory = null;

    // [线性-B05ze] 当前显示的分类，由BattleChangeMenuActionSelection点击按钮时设置
    private ItemCategory currentCategory = ItemCategory.CAPTURE;

    public PkBagActionSelection(BattleGUI battleGUI, SingleActionRequest request) {
        super(battleGUI, request, 0, 0, battleGUI.width, battleGUI.height, Component.literal("精灵球背包"));
        this.backButton = new BattleBackButton(12.0f, battleGUI.height - 32.0f);

        // [线性-B05zh] 面板x=12左对齐道具tile，底边对齐道具tile底边(guiScaledH-30)
        this.panelX = ITEMS_TILE_X;
        this.panelY = battleGUI.height - ITEMS_TILE_BOTTOM_OFFSET - PANEL_H;

        // [线性-B05zj] 下一页Y=返回键Y(同X层)，X=75固定(与返回键左右对称)
        this.nextX = 75;  // 75=面板右边缘104-29（右对齐面板，与返回键x=12左对齐对称）
        this.nextY = battleGUI.height - 32;  // 与BattleBackButton同Y层（blitk scale=0.5后视觉Y=height-32）

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

    // [线性-B05zk] 获取当前分类的物品列表，优先道具袋→玩家背包
    private List<ItemStack> getItemsForCurrentCategory() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return List.of();

        List<ItemStack> result = new ArrayList<>();
        // [线性-B05zk] 优先遍历Pokebag客户端缓存（道具袋）
        for (SyncPokebagContentsPacket.ItemEntry entry : PokebagClientCache.getItems()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.itemId()));
            if (item != null) {
                ItemStack stack = new ItemStack(item, entry.count());
                if (isItemInCurrentCategory(stack)) {
                    result.add(stack);
                }
            }
        }
        // [线性-B05zk] 再遍历玩家背包
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && isItemInCurrentCategory(stack)) {
                result.add(stack);
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
        Minecraft mc = Minecraft.getInstance();

        // [线性-B05zi] 渲染6格槽位：pkbag.png作为背景(92x23)，悬停用pkbag1.png
        // 物品材质居中放在pkbag.png内20x20 #f5f5f5区域(x=70~89, y=2~21)
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int slotY = panelY + i * SLOT_H;
            int itemIndex = startIndex + i;
            boolean isHovered = (i == hoveredSlot && itemIndex < items.size());

            // [线性-B05zi] 渲染槽位背景：pkbag.png(普通) / pkbag1.png(悬停)
            ResourceLocation bgTex = isHovered ? PKBAG_HOVER : PKBAG;
            graphics.blit(bgTex, panelX, slotY, 0, 0, SLOT_W, SLOT_H, SLOT_W, SLOT_H);

            // [线性-B05zi] 渲染物品图标和名称
            if (itemIndex < items.size()) {
                ItemStack stack = items.get(itemIndex);
                // [线性-B05zi] 物品材质居中放在20x20 #f5f5f5区域 (panelX+72, slotY+4)
                graphics.renderItem(stack, panelX + SLOT_ICON_X, slotY + SLOT_ICON_Y);
                // 物品数量覆盖在图标上
                graphics.renderItemDecorations(mc.font, stack, panelX + SLOT_ICON_X, slotY + SLOT_ICON_Y);
                // 物品名称（保持原位置）
                graphics.drawString(mc.font,
                        stack.getHoverName().getString(),
                        panelX + 24, slotY + 7, 0xFFFFFF);
            }
        }

        // 返回按键
        this.backButton.render(graphics, mouseX, mouseY, partialTick);

        // [线性-B05zj] 渲染页码指示器（固定位置，居中面板下方）
        // 布局：BattleBackButton(12~41) ← gap(9) → "1/3"(50~66) ← gap(9) → battle_next(75~104)
        if (totalPages > 1) {
            String pageText = (currentPage + 1) + "/" + totalPages;
            int textW = mc.font.width(pageText);
            graphics.drawString(mc.font, pageText,
                    panelX + (PANEL_W - textW) / 2,
                    panelY + PANEL_H + 4, 0xAAAAAA);
        }

        // [线性-B05zh] 下一页按键：垂直strip渲染，上半=普通，下半=悬停蓝色
        // blit到屏幕29x17（与BattleBackButton blitk(scale=0.5)后尺寸一致）
        if (currentPage < totalPages - 1) {
            boolean isHovered = isNextHovered(mouseX, mouseY);
            int vOffset = isHovered ? NEXT_HOVER_V : 0;  // 悬停=下半(v=34)，普通=上半(v=0)
            graphics.blit(BATTLE_NEXT, nextX, nextY,
                    29, 17,                    // 屏幕渲染宽高（与BattleBackButton一致）
                    0, vOffset,                // 纹理UV起点
                    NEXT_RENDER_W, NEXT_RENDER_H,  // 纹理区域宽高(58x34)
                    NEXT_TEX_W, NEXT_TEX_H);       // 纹理总宽高(58x68)
        }
    }

    // [线性-B05zd] 检测鼠标悬停在第几格（0~5），-1=无
    private int getHoveredSlot(int mouseX, int mouseY) {
        if (mouseX < panelX || mouseX > panelX + SLOT_W) return -1;
        int relY = mouseY - panelY;
        if (relY < 0 || relY >= PANEL_H) return -1;
        return relY / SLOT_H;
    }

    // [线性-B05zh] 检测下一页按键是否悬停（29x17，与BattleBackButton hit检测一致）
    private boolean isNextHovered(double mouseX, double mouseY) {
        if (currentPage >= totalPages - 1) return false;
        return mouseX >= nextX && mouseX <= nextX + 29
                && mouseY >= nextY && mouseY <= nextY + 17;
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

    // [线性-B05zk] 使用物品：记录最后使用的物品，自动模式下BattleChangeMenu可直接复用
    private void useItem(ItemStack stack) {
        // [线性-B05zk] 记录最后使用的物品ID和分类
        lastUsedItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        lastUsedCategory = currentCategory;

        if (currentCategory == ItemCategory.CAPTURE) {
            // [线性-B05ze] 球类 → 缩小战斗界面，切换精灵球到手中，玩家投掷触发捕捉
            usePokeball(stack);
        } else {
            // [线性-B05ze] 恢复/战斗道具 → 通过HealItemActionResponse发送使用指令
            // Cobblemon原版拿道具右键精灵的事件，由showdown处理
            useBagItem(stack);
        }
    }

    // [线性-B05zk] 自动使用上次物品（BattleChangeMenu调用）
    public static void autoUseLastItem(BattleGUI battleGUI, SingleActionRequest request) {
        if (lastUsedItemId == null || lastUsedCategory == null) return;
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(lastUsedItemId));
        if (item == null) return;
        ItemStack stack = new ItemStack(item, 1);
        Minecraft mc = Minecraft.getInstance();
        if (lastUsedCategory == ItemCategory.CAPTURE) {
            // 球类 → 发送ThrowPokeBallPacket
            PacketDistributor.sendToServer(new ThrowPokeBallPacket(lastUsedItemId));
        } else {
            // 恢复/战斗道具 → HealItemActionResponse
            if (stack.getItem() instanceof BagItemLike bagItemLike) {
                BagItem bagItem = bagItemLike.getBagItem(stack);
                HealItemActionResponse response = new HealItemActionResponse(bagItem.getItemName());
                battleGUI.selectAction(request, response);
            }
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