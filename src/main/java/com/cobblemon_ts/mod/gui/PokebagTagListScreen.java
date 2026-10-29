package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

// [线性-RESP-05] 完全响应式布局：宽度/高度都按 this.width / this.height 动态计算
//   1. 条目行数 rowsPerPage 由可用高度决定，不再固定10行（高GUI缩放时不会压住底部按钮）
//   2. 底部6按钮按可用宽度均分，窄屏自动折行
//   3. 页码信息改为纯文本绘制，不再占用控件
@OnlyIn(Dist.CLIENT)
public class PokebagTagListScreen extends Screen {

    private static final int MAX_ROWS = 10;
    private static final int ITEM_H = 20;
    private static final int BTN_H = 20;
    private static final int INFO_H = 12;
    private static final int BOTTOM_MARGIN = 6;
    private static final int BTN_GAP = 4;

    private final boolean isWhitelist;
    private final Screen parent;
    final List<String> entries;
    private int page = 0;

    private int listTop;
    private int rowsPerPage = MAX_ROWS;
    private int pageInfoY;

    public PokebagTagListScreen(boolean isWhitelist, Screen parent) {
        super(Component.translatable(isWhitelist
                ? "coblemon_ts.pokebag_config.whitelist_title"
                : "coblemon_ts.pokebag_config.blacklist_title"));
        this.isWhitelist = isWhitelist;
        this.parent = parent;
        this.entries = new ArrayList<>(getList());
    }

    private List<? extends String> getList() {
        return isWhitelist
                ? CharmConfig.POKEBAG_WHITELIST_TAGS.get()
                : CharmConfig.POKEBAG_BLACKLIST_TAGS.get();
    }

    // [线性-RESP-05] 计算底部按钮布局：每行放几个、共几行
    private int buttonsPerRow(int btnW) {
        int usable = this.width - 20;
        int perRow = (usable + BTN_GAP) / (btnW + BTN_GAP);
        return Math.max(1, Math.min(6, perRow));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // ── 底部按钮尺寸（先算，才能确定列表可用高度）──
        int btnW = Math.max(40, Math.min(58, (this.width - 20 - BTN_GAP * 5) / 6));
        int perRow = buttonsPerRow(btnW);
        int btnRows = (6 + perRow - 1) / perRow;
        int btnBlockH = btnRows * BTN_H + (btnRows - 1) * BTN_GAP;

        int infoY = this.height - BOTTOM_MARGIN - INFO_H;
        int btnBlockY = infoY - 4 - btnBlockH;

        // ── 列表区域：从标题下方到按钮块上方 ──
        this.listTop = 34;
        this.pageInfoY = infoY;
        int listBottom = btnBlockY - 6;

        this.rowsPerPage = Math.max(1, Math.min(MAX_ROWS, (listBottom - this.listTop) / ITEM_H));

        int totalPages = Math.max(1, (entries.size() + rowsPerPage - 1) / rowsPerPage);
        if (page > totalPages - 1) page = totalPages - 1;
        if (page < 0) page = 0;

        // ── 条目区宽度 ──
        int contentWidth = Math.min(400, this.width - 20);
        int deleteW = Math.min(45, contentWidth / 4);
        int entryW = contentWidth - deleteW - BTN_GAP;
        int entryX = centerX - contentWidth / 2;

        int start = page * rowsPerPage;
        int end = Math.min(start + rowsPerPage, entries.size());
        for (int i = start; i < end; i++) {
            int idx = i;
            int y = listTop + (i - start) * ITEM_H;
            String entry = entries.get(i);

            // 条目文本按钮（点击编辑）
            this.addRenderableWidget(Button.builder(
                    Component.literal(formatEntry(entry)),
                    btn -> Minecraft.getInstance().setScreen(
                            new PokebagTagEditScreen(isWhitelist, idx, entry, this)))
                    .pos(entryX, y)
                    .size(entryW, ITEM_H - 2)
                    .build());

            // 删除按钮
            this.addRenderableWidget(Button.builder(
                    Component.translatable("coblemon_ts.pokebag_config.delete"),
                    btn -> {
                        entries.remove(idx);
                        if (page > 0 && page * rowsPerPage >= entries.size()) page--;
                        rebuildWidgets();
                    })
                    .pos(entryX + entryW + BTN_GAP, y)
                    .size(deleteW, ITEM_H - 2)
                    .build());
        }

        // ── 底部按钮：按 perRow 折行排列，每行居中 ──
        List<Button> bottomBtns = new ArrayList<>();

        // 上一页
        Button prevBtn = Button.builder(
                Component.translatable("coblemon_ts.lucky.list.prev_page"),
                btn -> { page--; rebuildWidgets(); })
                .size(btnW, BTN_H).build();
        prevBtn.active = page > 0;
        bottomBtns.add(prevBtn);

        // 下一页
        Button nextBtn = Button.builder(
                Component.translatable("coblemon_ts.lucky.list.next_page"),
                btn -> { page++; rebuildWidgets(); })
                .size(btnW, BTN_H).build();
        nextBtn.active = (page + 1) * rowsPerPage < entries.size();
        bottomBtns.add(nextBtn);

        // 添加
        bottomBtns.add(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.add_entry"),
                btn -> {
                    int newIdx = entries.size();
                    entries.add("");
                    page = newIdx / rowsPerPage;
                    rebuildWidgets();
                    Minecraft.getInstance().setScreen(
                            new PokebagTagEditScreen(isWhitelist, newIdx, "", this));
                })
                .size(btnW, BTN_H).build());

        // 物品栏选择
        bottomBtns.add(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.pick_from_inv"),
                btn -> Minecraft.getInstance().setScreen(
                        new PokebagInventoryPickScreen(isWhitelist, this)))
                .size(btnW, BTN_H).build());

        // 保存
        bottomBtns.add(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save_list"),
                btn -> saveList())
                .size(btnW, BTN_H).build());

        // 返回
        bottomBtns.add(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.back_list"),
                btn -> Minecraft.getInstance().setScreen(parent))
                .size(btnW, BTN_H).build());

        for (int i = 0; i < bottomBtns.size(); i++) {
            int row = i / perRow;
            int col = i % perRow;
            int inRow = Math.min(perRow, bottomBtns.size() - row * perRow);
            int rowW = inRow * btnW + (inRow - 1) * BTN_GAP;
            int rowX = centerX - rowW / 2;
            Button b = bottomBtns.get(i);
            b.setPosition(rowX + col * (btnW + BTN_GAP), btnBlockY + row * (BTN_H + BTN_GAP));
            this.addRenderableWidget(b);
        }
    }

    private String formatEntry(String entry) {
        if (entry == null || entry.isEmpty())
            return "§7" + Component.translatable("coblemon_ts.lucky.empty_entry").getString();
        if (entry.startsWith("#")) return "§d" + entry;
        return "§f" + entry;
    }

    void saveList() {
        List<String> clean = new ArrayList<>(entries);
        clean.removeIf(s -> s == null || s.isBlank());

        if (isWhitelist) {
            CharmConfig.POKEBAG_WHITELIST_TAGS.set(clean);
            CharmConfig.POKEBAG_WHITELIST_TAGS.save();
        } else {
            CharmConfig.POKEBAG_BLACKLIST_TAGS.set(clean);
            CharmConfig.POKEBAG_BLACKLIST_TAGS.save();
        }

        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("coblemon_ts.pokebag_config.saved"), false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        int centerX = this.width / 2;
        g.drawCenteredString(this.font, this.title, centerX, 8, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.tag_hint"),
                centerX, 21, 0xAAAAAA);

        // [线性-RESP-05] 页码信息纯文本绘制
        int totalPages = Math.max(1, (entries.size() + rowsPerPage - 1) / rowsPerPage);
        String pageInfo = (page + 1) + " / " + totalPages
                + "  (" + entries.size() + " "
                + Component.translatable("coblemon_ts.pokebag_config.entries_count").getString() + ")";
        g.drawCenteredString(this.font, Component.literal("§7" + pageInfo), centerX, pageInfoY, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}