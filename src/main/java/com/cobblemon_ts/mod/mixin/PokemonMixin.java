package com.cobblemon_ts.mod.mixin;

import com.cobblemon.mod.common.api.pokemon.experience.ExperienceSource;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.pokemon.AddExperienceResult;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmEventHandler;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmType;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pokemon.class)
public abstract class PokemonMixin {

    @Inject(
        method = "addExperience",
        at = @At("RETURN"),
        remap = false
    )
    private void cobblemon_ts$onAddExperience(
        ExperienceSource source,
        int experience,
        CallbackInfoReturnable<AddExperienceResult> cir
    ) {
        // 1. 获取返回结果，如果为空则不处理
        AddExperienceResult result = cir.getReturnValue();
        if (result == null) {
            return;
        }

        // 2. 防止递归：如果这是我们自己的 sidemod 触发的经验分配，跳过
        if (source instanceof SidemodExperienceSource ses) {
            String sid = ses.getSidemodId();
            if ("cobblemon_ts".equals(sid)) {
                return;
            }
        }

        // 3. 判断是否应该触发高级学习装置
        //    - 普通战斗：source.isBattle() == true
        //    - quickbattle：SidemodExperienceSource 且 sidemodId == "cobblemon_quick_battle"
        boolean isBattle = source.isBattle();
        boolean isQuickBattle = false;
        if (source instanceof SidemodExperienceSource ses) {
            isQuickBattle = "cobblemon_quick_battle".equals(ses.getSidemodId());
        }
        if (!isBattle && !isQuickBattle) {
            return;
        }

        // 4. 确定要分享的经验值
        //    - 优先使用传入的 experience > 0
        //    - 否则使用 result.getExperienceAdded()
        int offeredExp = experience;
        if (offeredExp <= 0) {
            offeredExp = result.getExperienceAdded();
        }
        if (offeredExp <= 0) {
            return;
        }

        // 5. 获取当前 Pokemon 的所有者
        @SuppressWarnings("deprecation")
        Pokemon self = (Pokemon) (Object) this;
        ServerPlayer player = self.getOwnerPlayer();
        if (player == null) {
            return;
        }

        // 6. 检查高级学习装置是否激活
        CharmPlayerData data = CharmPlayerData.get(player);
        if (data == null) {
            return;
        }
        CharmSlotManager manager = data.getCharmSlotManager();
        if (manager == null || !manager.isCharmActive(CharmType.EXP_ALL)) {
            return;
        }

        // 7. 按百分比计算分配给队伍的经验
        double percent = ((Double) CharmConfig.EXP_ALL_PERCENT.get()).doubleValue();
        int partyExp = (int) (offeredExp * percent / 100.0);
        if (partyExp <= 0) {
            return;
        }

        // 8. 分配经验给队伍中其他宝可梦
        CharmEventHandler.distributePartyExp(player, self, partyExp);
    }
}