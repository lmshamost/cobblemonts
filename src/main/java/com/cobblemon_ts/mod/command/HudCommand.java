// [线性-CMD-HUD] /cts hud → 纯客户端指令，直接打开HUD设置界面，无TestBuild入口防作弊
package com.cobblemon_ts.mod.command;

import com.cobblemon_ts.mod.gui.HudSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public class HudCommand {
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("cts")
                .then(Commands.literal("hud")
                    .executes(ctx -> {
                        Minecraft.getInstance().setScreen(new HudSettingsScreen());
                        return 1;
                    })
                )
        );
    }
}