package com.twilightbossplus.mixin;

import com.twilightbossplus.config.SnowQueenConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.HoverBeamGoal;
import twilightforest.entity.ai.goal.HoverSummonGoal;
import twilightforest.entity.ai.goal.HoverThenDropGoal;

@Mixin({HoverSummonGoal.class, HoverThenDropGoal.class, HoverBeamGoal.class})
public abstract class QueenHoverGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceHover(CallbackInfoReturnable<Boolean> cir) {
        if (SnowQueenConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
