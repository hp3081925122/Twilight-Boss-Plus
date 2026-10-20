package com.twilightbossplus.mixin;

import com.twilightbossplus.config.MinoConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.GroundAttackGoal;

@Mixin(GroundAttackGoal.class)
public abstract class GroundAttackGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tbp$replacedSlam(CallbackInfoReturnable<Boolean> cir) {
        if (MinoConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
