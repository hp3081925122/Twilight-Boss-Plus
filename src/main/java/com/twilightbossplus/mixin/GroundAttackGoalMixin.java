package com.twilightbossplus.mixin;

import com.twilightbossplus.config.MinoshroomConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.GroundAttackGoal;

@Mixin(GroundAttackGoal.class)
public abstract class GroundAttackGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$disable(CallbackInfoReturnable<Boolean> cir) {
        if (MinoshroomConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
