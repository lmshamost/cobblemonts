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
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:lucky_block"), "随机宝可梦幸运方块");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:legendary_lucky_block"), "传说宝可梦幸运方块");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:ultra_beast_lucky_block"), "究极异兽幸运方块");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:shiny_lucky_block"), "闪光宝可梦幸运方块");
        ALL_BLOCKS.put(ResourceLocation.parse("coblemon_ts:item_lucky_block"), "物品幸运方块");
    }

    public LuckyWeightEditorScreen() {
        super(Component.literal("幸运方块权重编辑器"));
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
            String displayName = entry.getValue();

            // 显示配置状态提示
            LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
            String suffix = (config != null && !config.allEntries.isEmpty())
                    ? " §a[" + config.allEntries.size() + "条]"
                    : " §7[无配置]";

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
                Component.literal("返回"),
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
        graphics.drawCenteredString(this.font, "§7点击幸运方块查看和编辑其权重配置",
                this.width / 2, 35, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}