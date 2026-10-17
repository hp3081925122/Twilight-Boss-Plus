package com.twilightbossplus.mixin;

import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.yeti.AlphaYetiControllerHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.YetiTiredGoal;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(value = YetiTiredGoal.class, remap = false)
public abstract class YetiTiredGoalMixin {
    @Shadow
    @Final
    private AlphaYeti yeti;

    @Shadow
    private int tiredTimer;

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true, remap = true)
    private void twilightbossplus$tiredDuration(CallbackInfoReturnable<Boolean> cir) {
        if (AlphaYetiConfig.ENABLED.get()) {
            cir.setReturnValue(this.tiredTimer < ((AlphaYetiControllerHolder) this.yeti).twilightbossplus$getController().getTiredTicks());
        }
    }
}
