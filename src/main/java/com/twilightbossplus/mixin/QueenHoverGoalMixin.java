package com.twilightbossplus.mixin;

import com.twilightbossplus.common.BossActivation;
import com.twilightbossplus.config.SnowQueenConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.HoverBaseGoal;
import twilightforest.entity.ai.goal.HoverBeamGoal;
import twilightforest.entity.ai.goal.HoverSummonGoal;
import twilightforest.entity.ai.goal.HoverThenDropGoal;
import twilightforest.entity.boss.SnowQueen;

@Mixin({HoverSummonGoal.class, HoverThenDropGoal.class, HoverBeamGoal.class})
public abstract class QueenHoverGoalMixin extends HoverBaseGoal<SnowQueen> {
    protected QueenHoverGoalMixin(SnowQueen queen, float height, float radius) {
        super(queen, height, radius);
    }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tbp$replaceHover(CallbackInfoReturnable<Boolean> cir) {
        if (BossActivation.isEnhanced(this.attacker, SnowQueenConfig.ENABLED.get())) {
            cir.setReturnValue(false);
        }
    }
}
