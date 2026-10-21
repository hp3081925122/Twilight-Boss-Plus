package com.twilightbossplus.mixin;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrainHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.ThrowRiderGoal;

@Mixin(ThrowRiderGoal.class)
public abstract class ThrowRiderGoalMixin extends MeleeAttackGoal {
    protected ThrowRiderGoalMixin(PathfinderMob mob, double speed, boolean memory) {
        super(mob, speed, memory);
    }

    @Unique
    private boolean tbp$busy() {
        return YetiConfig.ENABLED.get() && this.mob instanceof YetiBrainHolder holder && holder.tbp$getBrain().busy();
    }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$waitForSkill(CallbackInfoReturnable<Boolean> cir) {
        if (this.tbp$busy()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkAndPerformAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$noGrabDuringSkill(LivingEntity victim, CallbackInfo ci) {
        if (this.tbp$busy()) {
            ci.cancel();
        }
    }
}
