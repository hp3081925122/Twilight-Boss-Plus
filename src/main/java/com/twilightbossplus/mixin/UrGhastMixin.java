package com.twilightbossplus.mixin;

import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBossController;
import com.twilightbossplus.urghast.UrGhastControllerHolder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

@Mixin(UrGhast.class)
public abstract class UrGhastMixin extends CarminiteGhastguard implements UrGhastControllerHolder {
    @Unique
    private UrGhastBossController twilightbossplus$controller;

    protected UrGhastMixin(EntityType<? extends CarminiteGhastguard> type, Level level) {
        super(type, level);
    }

    @Override
    public UrGhastBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new UrGhastBossController((UrGhast) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        this.twilightbossplus$getController().tick();
    }

    @Inject(method = "doTantrumDamageEffects", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$tantrumEffects(CallbackInfo ci) {
        if (UrGhastConfig.ENABLED.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "spitFireball", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$fireVolley(CallbackInfo ci) {
        if (UrGhastConfig.ENABLED.get()) {
            ci.cancel();
            this.twilightbossplus$getController().fireVolley();
        }
    }

    @ModifyConstant(method = "hurt", constant = @Constant(floatValue = 10.0F))
    private float twilightbossplus$tantrumDivisor(float original) {
        if (UrGhastConfig.ENABLED.get()) {
            return UrGhastConfig.TANTRUM_DAMAGE_DIVISOR.get().floatValue();
        }
        return original;
    }

    @ModifyConstant(method = "resetDamageUntilNextPhase", constant = @Constant(floatValue = 18.0F), remap = false)
    private float twilightbossplus$tantrumThreshold(float original) {
        if (UrGhastConfig.ENABLED.get()) {
            return (float) (this.getMaxHealth() * UrGhastConfig.TANTRUM_RATIO.get());
        }
        return original;
    }

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/UrGhast;heal(F)V"))
    private void twilightbossplus$absorbHeal(UrGhast ghast, float amount) {
        if (UrGhastConfig.ENABLED.get()) {
            ghast.heal(this.twilightbossplus$getController().onAbsorb());
            return;
        }
        ghast.heal(amount);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$tearParticle(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if (level.isClientSide() && UrGhastConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void twilightbossplus$onDeath(DamageSource source, CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.twilightbossplus$getController().onDeath();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$save(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$getController().save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$load(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$getController().load(tag);
    }
}
