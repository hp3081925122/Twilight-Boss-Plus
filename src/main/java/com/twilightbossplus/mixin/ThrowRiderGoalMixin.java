package com.twilightbossplus.mixin;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.GeneralConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.ThrowRiderGoal;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(value = ThrowRiderGoal.class, remap = false)
public abstract class ThrowRiderGoalMixin extends MeleeAttackGoal {
    @Unique
    private Entity twilightbossplus$rider;

    protected ThrowRiderGoalMixin(PathfinderMob mob, double speed, boolean memory) {
        super(mob, speed, memory);
    }

    @Inject(method = "stop", at = @At("HEAD"), remap = true)
    private void twilightbossplus$captureRider(CallbackInfo ci) {
        this.twilightbossplus$rider = null;
        if (!this.mob.getPassengers().isEmpty()) {
            this.twilightbossplus$rider = this.mob.getPassengers().get(0);
        }
    }

    @ModifyVariable(method = "stop", at = @At("STORE"), ordinal = 0, remap = true)
    private Vec3 twilightbossplus$scaleThrow(Vec3 original) {
        if (!(this.mob instanceof AlphaYeti) || !AlphaYetiConfig.ENABLED.get()) {
            return original;
        }
        double resistance = 0.0D;
        if (this.twilightbossplus$rider instanceof LivingEntity living) {
            resistance = Math.min(1.0D, living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        }
        double scale = AlphaYetiConfig.THROW_STRENGTH.get() * (1.0D - resistance);
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[AlphaYeti {}] throw vector scaled by {}", this.mob.getId(), scale);
        }
        return original.scale(scale);
    }
}
