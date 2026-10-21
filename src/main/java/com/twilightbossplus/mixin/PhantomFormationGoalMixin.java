package com.twilightbossplus.mixin;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.PhantomUpdateFormationAndMoveGoal;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(PhantomUpdateFormationAndMoveGoal.class)
public abstract class PhantomFormationGoalMixin {
    @Shadow(remap = false)
    @Final
    private KnightPhantom boss;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$pauseWhileBusy(CallbackInfo ci) {
        if (KnightConfig.ENABLED.get() && KnightBrain.of(this.boss).busy()) {
            ci.cancel();
        }
    }

    @Inject(method = "makeARandomKnightCharge", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceCharge(List<KnightPhantom> knights, CallbackInfo ci) {
        if (KnightConfig.ENABLED.get()) {
            ci.cancel();
        }
    }
}
