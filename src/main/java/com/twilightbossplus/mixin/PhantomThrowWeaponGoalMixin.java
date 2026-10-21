package com.twilightbossplus.mixin;

import com.twilightbossplus.config.KnightConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.PhantomThrowWeaponGoal;

@Mixin(PhantomThrowWeaponGoal.class)
public abstract class PhantomThrowWeaponGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceThrows(CallbackInfoReturnable<Boolean> cir) {
        if (KnightConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
