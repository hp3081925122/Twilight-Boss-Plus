package com.twilightbossplus.mixin;

import com.twilightbossplus.config.LichConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.LichAbsorbMinionsGoal;

@Mixin(LichAbsorbMinionsGoal.class)
public abstract class LichAbsorbMinionsGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$disableInstantAbsorb(CallbackInfoReturnable<Boolean> cir) {
        if (LichConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
