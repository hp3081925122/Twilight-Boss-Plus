package com.twilightbossplus.mixin;

import com.twilightbossplus.common.BossActivation;
import com.twilightbossplus.config.KnightConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.PhantomThrowWeaponGoal;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(PhantomThrowWeaponGoal.class)
public abstract class PhantomThrowWeaponGoalMixin {
    @Shadow(remap = false)
    @Final
    private KnightPhantom boss;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tbp$replaceThrows(CallbackInfoReturnable<Boolean> cir) {
        if (BossActivation.isEnhanced(this.boss, KnightConfig.ENABLED.get())) {
            cir.setReturnValue(false);
        }
    }
}
