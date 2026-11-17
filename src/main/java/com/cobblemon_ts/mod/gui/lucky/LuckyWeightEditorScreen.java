package com.cobblemon_ts.mod.gui.lucky;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

// [线性-LWE-01] 幸运方块权重编辑器主界面
// 列出所有幸运方块，点击进入对应方块的条目列表
@Environment(EnvType.CLIENT)
public class LuckyWeightEditorScreen extends Screen {

    // 所有幸运方块ID → 显示名称
    private static final LinkedHashMap<Identifier, String> ALL_BLOCKS = new LinkedHashMap<>();
    static {
        ALL_BLOCKS.put(Identifier.of("cobblemon_ts:lucky_block"), "cobblemon_ts.lucky.block.random_pokemon");
        ALL_BLOCKS.put(Identifier.of("cobblemon_ts:legendary_lucky_block"), "cobblemon_ts.lucky.block.legendary");
        ALL_BLOCKS.put(Identifier.of("cobblemon_ts:ultra_beast_lucky_block"), "cobblemon_ts.lucky.block.ultra_beast");
        ALL_BLOCKS.put(Identifier.of("cobblemon_ts:shiny_lucky_block"), "cobblemon_ts.lucky.block.shiny");
        ALL_BLOCKS.put(Identifier.of("cobblemon_ts:item_lucky_block"), "cobblemon_ts.lucky.block.items");
    }

    public LuckyWeightEditorScreen() {
        super(Text.translatable("cobblemon_ts.lucky.title"));
    }

    @Override
    protected void init() {
        int btnW = 220;
        int btnH = 22;
        int centerX = this.width / 2;
        int startY = this.height / 2 - (ALL_BLOCKS.size() * 26) / 2;

        int index = 0;
        for (Map.Entry<Identifier, String> entry : ALL_BLOCKS.entrySet()) {
            Identifier blockId = entry.getKey();
            String displayKey = entry.getValue();
            String displayName = Text.translatable(displayKey).getString();

            // 显示配置状态提示
            LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
            int entryCount = (config != null) ? config.allEntries.size() : 0;
            String suffix = entryCount > 0
                    ? " §a[" + entryCount + " " + Text.translatable("cobblemon_ts.lucky.entries_count").getString() + "]"
                    : " §7[" + Text.translatable("cobblemon_ts.lucky.no_config").getString() + "]";

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(displayName + suffix),
                    btn -> MinecraftClient.getInstance().setScreen(
                            new LuckyWeightEntryListScreen(blockId, displayName, this)))
                    .position(centerX - btnW / 2, startY + index * 26)
                    .size(btnW, btnH)
                    .build());
            index++;
        }

        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.lucky.back"),
                btn -> this.close())
                .position(centerX - 50, startY + index * 26 + 10)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("cobblemon_ts.lucky.hint"),
                this.width / 2, 35, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}