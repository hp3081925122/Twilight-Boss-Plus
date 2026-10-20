package com.twilightbossplus.mixin;

import com.twilightbossplus.config.UrGhastConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.GhastguardAttackGoal;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

@Mixin(GhastguardAttackGoal.class)
public abstract class GhastguardAttackGoalMixin {
    @Shadow(remap = false)
    @Final
    private CarminiteGhastguard ghast;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tbp$blockUrGhast(CallbackInfoReturnable<Boolean> cir) {
        if (this.ghast instanceof UrGhast && UrGhastConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
