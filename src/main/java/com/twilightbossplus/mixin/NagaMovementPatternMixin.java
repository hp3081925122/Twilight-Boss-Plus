package com.twilightbossplus.mixin;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.naga.NagaControllerHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

@Mixin(NagaMovementPattern.class)
public abstract class NagaMovementPatternMixin {
    @Shadow(remap = false)
    @Final
    private Naga naga;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$pauseForSkill(CallbackInfo ci) {
        if (((NagaControllerHolder) this.naga).twilightbossplus$getController().isMovementLocked()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.5F))
    private float twilightbossplus$stunlessChanceCap(float cap) {
        if (!NagaConfig.ENABLED.get()) {
            return cap;
        }
        NagaBossController controller = ((NagaControllerHolder) this.naga).twilightbossplus$getController();
        if (controller.getPhase() >= 3) {
            return 0.75F;
        }
        return cap;
    }
}
