package com.twilightbossplus.mixin;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.common.BossActivation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.SnowQueen;

@Mixin(value = MeleeAttackGoal.class, remap = false)
public abstract class QueenMeleeGoalMixin {
    @Shadow(remap = false)
    @Final
    protected PathfinderMob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$blockQueen(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof SnowQueen && BossActivation.isEnhanced(this.mob, SnowQueenConfig.ENABLED.get())) {
            cir.setReturnValue(false);
        }
    }
}
