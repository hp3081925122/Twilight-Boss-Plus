package com.twilightbossplus.mixin;

import com.twilightbossplus.naga.NagaBrainHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

@Mixin(NagaMovementPattern.class)
public abstract class NagaMovementPatternMixin {
    @Shadow(remap = false)
    @Final
    private Naga naga;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$yieldToSkills(CallbackInfoReturnable<Boolean> cir) {
        if (((NagaBrainHolder) this.naga).tbp$getBrain().busy()) {
            cir.setReturnValue(false);
        }
    }
}
