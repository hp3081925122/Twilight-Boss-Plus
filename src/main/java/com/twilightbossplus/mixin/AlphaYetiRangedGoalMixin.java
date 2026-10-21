package com.twilightbossplus.mixin;

import com.twilightbossplus.config.YetiConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "twilightforest.entity.boss.AlphaYeti$1")
public abstract class AlphaYetiRangedGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replacedIceBomb(CallbackInfoReturnable<Boolean> cir) {
        if (YetiConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
