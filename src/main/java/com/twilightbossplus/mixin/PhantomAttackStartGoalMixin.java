package com.twilightbossplus.mixin;

import com.twilightbossplus.knight.KnightGroups;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.PhantomAttackStartGoal;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(value = PhantomAttackStartGoal.class, remap = false)
public abstract class PhantomAttackStartGoalMixin {
    @Shadow
    @Final
    private KnightPhantom boss;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = true)
    private void twilightbossplus$tick(CallbackInfo ci) {
        if (KnightGroups.of(this.boss) != null) {
            ci.cancel();
        }
    }
}
