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

// [线性-PTL-01] 道具袋白名单/黑名单条目列表界面（仿LuckyWeightEntryListScreen按键模式）
@OnlyIn(Dist.CLIENT)
public class PokebagTagListScreen extends Screen {

    private final boolean isWhitelist;
    private final Screen parent;
    final List<String> entries;
    private int page = 0;
    private static final int PAGE_SIZE = 10;

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

    @Override
    protected void init() {
        int listStartY = 40;
        int itemH = 22;
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= totalPages) page = totalPages - 1;

        // 条目按钮
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        for (int i = start; i < end; i++) {
            int idx = i;
            int y = listStartY + (i - start) * itemH;
            String entry = entries.get(i);

            // 条目文本按钮（点击编辑）
            this.addRenderableWidget(Button.builder(
                    Component.literal(formatEntry(entry)),
                    btn -> Minecraft.getInstance().setScreen(
                            new PokebagTagEditScreen(isWhitelist, idx, entry, this)))
                    .pos(this.width / 2 - 175, y)
                    .size(270, itemH - 2)
                    .build());

            // 删除按钮
            this.addRenderableWidget(Button.builder(
                    Component.translatable("coblemon_ts.pokebag_config.delete"),
                    btn -> {
                        entries.remove(idx);
                        if (page * PAGE_SIZE >= entries.size() && page > 0) page--;
                        rebuildWidgets();
                    })
                    .pos(this.width / 2 + 100, y)
                    .size(60, itemH - 2)
                    .build());
        }

        // 底部按钮
        int bottomY = this.height - 60;

        // 上一页
        Button prevBtn = this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.list.prev_page"),
                btn -> { page--; rebuildWidgets(); })
                .pos(this.width / 2 - 140, bottomY)
                .size(60, 20)
                .build());
        prevBtn.active = page > 0;

        // 下一页
        Button nextBtn = this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.list.next_page"),
                btn -> { page++; rebuildWidgets(); })
                .pos(this.width / 2 - 70, bottomY)
                .size(60, 20)
                .build());
        nextBtn.active = (page + 1) * PAGE_SIZE < entries.size();

        // 添加条目
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.add_entry"),
                btn -> {
                    int newIdx = entries.size();
                    entries.add("");
                    page = newIdx / PAGE_SIZE;
                    rebuildWidgets();
                    Minecraft.getInstance().setScreen(
                            new PokebagTagEditScreen(isWhitelist, newIdx, "", this));
                })
                .pos(this.width / 2 + 5, bottomY)
                .size(60, 20)
                .build());

        // [线性-IPC-05] 从物品栏选择物品添加
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.pick_from_inv"),
                btn -> Minecraft.getInstance().setScreen(
                        new PokebagInventoryPickScreen(isWhitelist, this)))
                .pos(this.width / 2 + 70, bottomY)
                .size(60, 20)
                .build());

        // 保存
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save_list"),
                btn -> saveList())
                .pos(this.width / 2 + 140, bottomY)
                .size(60, 20)
                .build());

        // 返回
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.back_list"),
                btn -> Minecraft.getInstance().setScreen(parent))
                .pos(this.width / 2 + 210, bottomY)
                .size(60, 20)
                .build());

        // 页码显示
        String pageInfo = (page + 1) + " / " + totalPages
                + "  (" + entries.size() + " "
                + Component.translatable("coblemon_ts.pokebag_config.entries_count").getString() + ")";
        this.addRenderableWidget(Button.builder(
                Component.literal(pageInfo),
                btn -> {})
                .pos(this.width / 2 - 50, bottomY + 25)
                .size(100, 14)
                .build());
    }

    private String formatEntry(String entry) {
        if (entry == null || entry.isEmpty())
            return "§7" + Component.translatable("coblemon_ts.lucky.empty_entry").getString();
        if (entry.startsWith("#")) return "§d" + entry; // 标签紫色
        return "§f" + entry; // 物品ID白色
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
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.tag_hint"),
                this.width / 2, 28, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}