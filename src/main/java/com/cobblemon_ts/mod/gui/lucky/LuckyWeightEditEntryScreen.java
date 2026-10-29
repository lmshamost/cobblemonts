package com.cobblemon_ts.mod.gui.lucky;

import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import com.cobblemon_ts.mod.pokemoneditor.PokemonNameMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// [线性-LWE-03] 幸运方块条目编辑界面
// 编辑单个条目的: 物品/精灵列表(支持中文名), 权重值, 保底值
// 精灵类幸运方块使用PokemonNameMap双向转换中文名
// 物品类幸运方块使用PokemonNameMap.itemToChinese/ToEnglish
@OnlyIn(Dist.CLIENT)
public class LuckyWeightEditEntryScreen extends Screen {
    private final ResourceLocation blockId;
    private final String blockDisplayName;
    private final int entryIndex;
    private final LuckyWeightManager.WeightEntry<?> entry;
    private final Screen parent;

    private EditBox itemsField;
    private EditBox weightField;
    private EditBox guaranteeField;
    private boolean isPokemonBlock; // 是否为精灵类幸运方块

    public LuckyWeightEditEntryScreen(ResourceLocation blockId, String blockDisplayName,
                                      int entryIndex, LuckyWeightManager.WeightEntry<?> entry, Screen parent) {
        super(Component.translatable("coblemon_ts.lucky.edit.title", entryIndex + 1));
        this.blockId = blockId;
        this.blockDisplayName = blockDisplayName;
        this.entryIndex = entryIndex;
        this.entry = entry;
        this.parent = parent;
        // 只有item_lucky_block是物品类，其他都是精灵类
        this.isPokemonBlock = !"coblemon_ts:item_lucky_block".equals(blockId.toString());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 40;
        int labelX = centerX - 180;
        int fieldW = 360;
        int fieldH = 20;

        // 方块名称
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.edit.block_label", blockDisplayName),
                btn -> {})
                .pos(centerX - 100, 10)
                .size(200, 18)
                .build());

        // 物品/精灵输入框
        String itemsRaw = String.join(", ", entry.entries.stream()
                .map(obj -> {
                    String s = obj.toString();
                    if (s.isEmpty()) return "";
                    // 精灵类: 显示中文名
                    if (isPokemonBlock) {
                        String zh = PokemonNameMap.toChinese(s);
                        return zh.equals(s) ? s : zh;
                    }
                    return s;
                }).toList());

        this.addRenderableWidget(Button.builder(
                Component.translatable(isPokemonBlock
                        ? "coblemon_ts.lucky.edit.pokemon_name"
                        : "coblemon_ts.lucky.edit.item_id"),
                btn -> {})
                .pos(labelX, y)
                .size(60, fieldH)
                .build());

        itemsField = new EditBox(this.font, labelX + 65, y, fieldW - 65, fieldH,
                Component.literal("items"));
        itemsField.setMaxLength(500);
        itemsField.setValue(itemsRaw);
        itemsField.setResponder(s -> {});
        this.addRenderableWidget(itemsField);
        y += 28;

        // 提示文字
        this.addRenderableWidget(Button.builder(
                Component.translatable(isPokemonBlock
                        ? "coblemon_ts.lucky.edit.hint_pokemon"
                        : "coblemon_ts.lucky.edit.hint_item"),
                btn -> {})
                .pos(labelX, y)
                .size(fieldW, 14)
                .build());
        y += 20;

        // 权重输入框
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.edit.weight"),
                btn -> {})
                .pos(labelX, y)
                .size(60, fieldH)
                .build());

        weightField = new EditBox(this.font, labelX + 65, y, 100, fieldH,
                Component.literal("weight"));
        weightField.setMaxLength(10);
        weightField.setValue(String.valueOf(entry.weight));
        weightField.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(weightField);
        y += 28;

        // 保底输入框
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.edit.guarantee"),
                btn -> {})
                .pos(labelX, y)
                .size(60, fieldH)
                .build());

        guaranteeField = new EditBox(this.font, labelX + 65, y, 100, fieldH,
                Component.literal("guarantee"));
        guaranteeField.setMaxLength(10);
        guaranteeField.setValue(String.valueOf(entry.guarantee));
        guaranteeField.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(guaranteeField);
        y += 34;

        // 权重区间预览
        int totalWeight = 0;
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
        if (config != null) {
            for (LuckyWeightManager.WeightEntry<?> e : config.allEntries) {
                if (e.weight > 0) totalWeight += e.weight;
            }
        }
        int thisWeight = entry.weight;
        String preview = totalWeight > 0
                ? Component.translatable("coblemon_ts.lucky.edit.probability",
                        thisWeight, totalWeight, String.format("%.1f", 100.0 * thisWeight / totalWeight)).getString()
                : Component.translatable("coblemon_ts.lucky.edit.probability_zero").getString();
        this.addRenderableWidget(Button.builder(
                Component.literal(preview),
                btn -> {})
                .pos(labelX, y)
                .size(fieldW, 14)
                .build());
        y += 22;

        // 保存按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.edit.save"),
                btn -> saveEntry())
                .pos(centerX - 100, y + 5)
                .size(80, 20)
                .build());

        // 删除按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.edit.delete"),
                btn -> {
                    LuckyWeightManager.LuckyWeightConfig cfg = LuckyWeightManager.getConfig(blockId);
                    if (cfg != null && entryIndex < cfg.allEntries.size()) {
                        cfg.allEntries.remove(entryIndex);
                        // 删除后立即保存配置
                        if (parent instanceof LuckyWeightEntryListScreen ps) {
                            ps.saveConfigAndRefresh();
                        }
                    }
                    Minecraft.getInstance().setScreen(parent);
                })
                .pos(centerX - 10, y + 5)
                .size(80, 20)
                .build());

        // 返回按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.edit.back"),
                btn -> {
                    // 直接返回，不保存修改
                    Minecraft.getInstance().setScreen(parent);
                })
                .pos(centerX + 80, y + 5)
                .size(60, 20)
                .build());
    }

    // 保存条目修改
    private void saveEntry() {
        // 解析物品列表
        String itemsStr = itemsField.getValue().trim();
        List<String> items = new ArrayList<>();
        for (String item : itemsStr.split(",")) {
            item = item.trim();
            if (item.isEmpty()) continue;

            if (isPokemonBlock) {
                // 精灵类: 尝试中文→英文转换
                String en = PokemonNameMap.toEnglish(item);
                if (en != null && !en.equals(item)) {
                    items.add(en);
                } else {
                    items.add(item.toLowerCase());
                }
            } else {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            items.add(""); // 空标记
        }

        // 解析权重和保底
        int weight;
        int guarantee;
        try { weight = Integer.parseInt(weightField.getValue().trim()); } catch (NumberFormatException e) { weight = 0; }
        try { guarantee = Integer.parseInt(guaranteeField.getValue().trim()); } catch (NumberFormatException e) { guarantee = 0; }

        // 更新条目
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
        if (config != null) {
            LuckyWeightManager.WeightEntry<?> newEntry = new LuckyWeightManager.WeightEntry<>(items, weight, guarantee);
            if (entryIndex < config.allEntries.size()) {
                // 已存在条目：替换
                config.allEntries.set(entryIndex, newEntry);
            } else {
                // 新增条目：追加
                config.allEntries.add(newEntry);
            }
        }

        // 提示
        if (Minecraft.getInstance().player != null) {
            String display = String.join(",", items);
            if (isPokemonBlock) {
                List<String> zhNames = new ArrayList<>();
                for (String en : items) {
                    if (en.isEmpty()) {
                        zhNames.add(Component.translatable("coblemon_ts.lucky.empty_entry").getString());
                        continue;
                    }
                    String zh = PokemonNameMap.toChinese(en);
                    zhNames.add(zh.equals(en) ? en : zh);
                }
                display = String.join(",", zhNames);
            }
            String typeLabel = Component.translatable(isPokemonBlock
                    ? "coblemon_ts.lucky.edit.type_pokemon"
                    : "coblemon_ts.lucky.edit.type_item").getString();
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("coblemon_ts.lucky.edit.updated",
                            typeLabel, display, weight, guarantee),
                    false);
        }

        // 返回列表并保存配置
        Minecraft.getInstance().setScreen(parent);
        if (parent instanceof LuckyWeightEntryListScreen ps) {
            ps.saveConfigAndRefresh();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // ESC直接返回，不保存修改
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}