package com.twilightbossplus.mixin;

import com.twilightbossplus.config.YetiConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.YetiRampageGoal;

@Mixin(YetiRampageGoal.class)
public abstract class YetiRampageGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replacedRampage(CallbackInfoReturnable<Boolean> cir) {
        if (YetiConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
