package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.network.TestBuildOpenStarterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

// [线性-L1] 测试物品右键弹出的功能选择GUI
//   选项1: 打开初始宝可梦选择 → 发包给服务端
//   选项2: 切换精灵栏主题亮/暗 → 客户端本地切换配置
public class TestBuildScreen extends Screen {

    public TestBuildScreen() {
        super(Component.literal("测试功能选择"));
    }

    @Override
    protected void init() {
        int btnW = 200;
        int btnH = 20;
        int centerX = this.width / 2;
        int startY = this.height / 2 - 20;

        // [线性-L1-1] 按钮1: 初始宝可梦选择
        this.addRenderableWidget(Button.builder(
                Component.literal("打开初始宝可梦选择"),
                btn -> {
                    PacketDistributor.sendToServer(new TestBuildOpenStarterPacket());
                    this.onClose();
                })
                .pos(centerX - btnW / 2, startY)
                .size(btnW, btnH)
                .build());

        // [线性-L1-2] 按钮2: 切换精灵栏主题
        this.addRenderableWidget(Button.builder(
                Component.literal(getThemeToggleLabel()),
                btn -> {
                    boolean current = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
                    CobblemonTSConfig.LIGHT_PANEL_THEME.set(!current);
                    btn.setMessage(Component.literal(getThemeToggleLabel()));
                })
                .pos(centerX - btnW / 2, startY + 25)
                .size(btnW, btnH)
                .build());
    }

    private String getThemeToggleLabel() {
        boolean light = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
        return "切换精灵栏主题: " + (light ? "亮色 ✓" : "暗色 ✓");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, this.height / 2 - 45, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}