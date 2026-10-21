package com.twilightbossplus.mixin;

import com.twilightbossplus.config.MinoConfig;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.ai.goal.ChargeAttackGoal;
import twilightforest.entity.boss.Minoshroom;

@Mixin(ChargeAttackGoal.class)
public abstract class ChargeAttackGoalMixin {
    @Shadow(remap = false)
    @Final
    private PathfinderMob charger;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replacedCharge(CallbackInfoReturnable<Boolean> cir) {
        if (this.charger instanceof Minoshroom && MinoConfig.ENABLED.get()) {
            cir.setReturnValue(false);
        }
    }
}
