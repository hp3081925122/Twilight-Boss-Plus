package com.twilightbossplus.mixin;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastControllerHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.GhastguardAttackGoal;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

@Mixin(value = GhastguardAttackGoal.class, remap = false)
public abstract class GhastguardAttackGoalMixin {
    @Shadow
    @Final
    private CarminiteGhastguard ghast;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = true)
    private void twilightbossplus$pauseForSkill(CallbackInfo ci) {
        if (this.ghast instanceof UrGhast boss && UrGhastConfig.ENABLED.get() && ((UrGhastControllerHolder) boss).twilightbossplus$getController().isCasting()) {
            boss.setCharging(false);
            ci.cancel();
        }
    }
}
