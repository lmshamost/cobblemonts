package com.cobblemon_ts.mod.gui.lucky;

import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.Map;

// [线性-LWE-01] 幸运方块权重编辑器主界面
// 列出所有幸运方块，点击进入对应方块的条目列表
@OnlyIn(Dist.CLIENT)
public class LuckyWeightEditorScreen extends Screen {

    // 所有幸运方块ID → 显示名称
    private static final LinkedHashMap<ResourceLocation, String> ALL_BLOCKS = new LinkedHashMap<>();
    static {
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:lucky_block"), "coblemon_ts.lucky.block.random_pokemon");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:legendary_lucky_block"), "coblemon_ts.lucky.block.legendary");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:ultra_beast_lucky_block"), "coblemon_ts.lucky.block.ultra_beast");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:shiny_lucky_block"), "coblemon_ts.lucky.block.shiny");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:item_lucky_block"), "coblemon_ts.lucky.block.items");
    }

    public LuckyWeightEditorScreen() {
        super(Component.translatable("coblemon_ts.lucky.title"));
    }

    @Override
    protected void init() {
        int btnW = 220;
        int btnH = 22;
        int centerX = this.width / 2;
        int startY = this.height / 2 - (ALL_BLOCKS.size() * 26) / 2;

        int index = 0;
        for (Map.Entry<ResourceLocation, String> entry : ALL_BLOCKS.entrySet()) {
            ResourceLocation blockId = entry.getKey();
            String displayKey = entry.getValue();
            String displayName = Component.translatable(displayKey).getString();

            // 显示配置状态提示
            LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
            int entryCount = (config != null) ? config.allEntries.size() : 0;
            String suffix = entryCount > 0
                    ? " §a[" + entryCount + " " + Component.translatable("coblemon_ts.lucky.entries_count").getString() + "]"
                    : " §7[" + Component.translatable("coblemon_ts.lucky.no_config").getString() + "]";

            this.addRenderableWidget(Button.builder(
                    Component.literal(displayName + suffix),
                    btn -> Minecraft.getInstance().setScreen(
                            new LuckyWeightEntryListScreen(blockId, displayName, this)))
                    .pos(centerX - btnW / 2, startY + index * 26)
                    .size(btnW, btnH)
                    .build());
            index++;
        }

        // 返回按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.lucky.back"),
                btn -> this.onClose())
                .pos(centerX - 50, startY + index * 26 + 10)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("coblemon_ts.lucky.hint"),
                this.width / 2, 35, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}