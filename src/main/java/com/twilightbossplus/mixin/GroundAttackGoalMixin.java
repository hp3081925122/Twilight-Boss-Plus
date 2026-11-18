package com.twilightbossplus.mixin;

import com.twilightbossplus.common.BossActivation;
import com.twilightbossplus.config.MinoConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.GroundAttackGoal;
import twilightforest.entity.boss.Minoshroom;

@Mixin(GroundAttackGoal.class)
public abstract class GroundAttackGoalMixin {
    @Shadow(remap = false)
    @Final
    private Minoshroom attacker;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tbp$replacedSlam(CallbackInfoReturnable<Boolean> cir) {
        if (BossActivation.isEnhanced(this.attacker, MinoConfig.ENABLED.get())) {
            cir.setReturnValue(false);
        }
    }
}
