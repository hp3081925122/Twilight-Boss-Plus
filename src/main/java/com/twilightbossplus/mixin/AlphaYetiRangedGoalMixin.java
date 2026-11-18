package com.twilightbossplus.mixin;

import com.twilightbossplus.common.BossActivation;
import com.twilightbossplus.config.YetiConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(RangedAttackGoal.class)
public abstract class AlphaYetiRangedGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tbp$replacedIceBomb(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof AlphaYeti && BossActivation.isEnhanced(this.mob, YetiConfig.ENABLED.get())) {
            cir.setReturnValue(false);
        }
    }
}
