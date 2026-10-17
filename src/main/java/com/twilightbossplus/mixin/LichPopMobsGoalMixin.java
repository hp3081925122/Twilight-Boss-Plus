package com.twilightbossplus.mixin;

import com.twilightbossplus.config.LichConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import twilightforest.entity.ai.goal.LichPopMobsGoal;

@Mixin(LichPopMobsGoal.class)
public abstract class LichPopMobsGoalMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/Lich;heal(F)V"))
    private float twilightbossplus$popHeal(float amount) {
        if (!LichConfig.ENABLED.get()) {
            return amount;
        }
        return LichConfig.POP_HEAL.get().floatValue();
    }
}
