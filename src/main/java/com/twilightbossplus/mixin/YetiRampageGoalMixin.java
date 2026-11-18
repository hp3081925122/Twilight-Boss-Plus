package com.twilightbossplus.mixin;

import com.twilightbossplus.common.BossActivation;
import com.twilightbossplus.config.YetiConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.YetiRampageGoal;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(value = YetiRampageGoal.class, remap = false)
public abstract class YetiRampageGoalMixin {
    @Shadow(remap = false)
    @Final
    private AlphaYeti yeti;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replacedRampage(CallbackInfoReturnable<Boolean> cir) {
        if (BossActivation.isEnhanced(this.yeti, YetiConfig.ENABLED.get())) {
            cir.setReturnValue(false);
        }
    }
}
