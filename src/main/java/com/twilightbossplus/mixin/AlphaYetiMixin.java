package com.twilightbossplus.mixin;

import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.yeti.AlphaYetiBossController;
import com.twilightbossplus.yeti.AlphaYetiControllerHolder;
import com.twilightbossplus.yeti.AlphaYetiSkillGoal;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(AlphaYeti.class)
public abstract class AlphaYetiMixin extends Monster implements AlphaYetiControllerHolder {
    @Unique
    private AlphaYetiBossController twilightbossplus$controller;

    protected AlphaYetiMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public AlphaYetiBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new AlphaYetiBossController((AlphaYeti) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void twilightbossplus$registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new AlphaYetiSkillGoal((AlphaYeti) (Object) this));
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.twilightbossplus$getController().tick();
        }
    }

    @Inject(method = "hitNearbyEntities", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$landing(CallbackInfo ci) {
        if (AlphaYetiConfig.ENABLED.get()) {
            ci.cancel();
            this.twilightbossplus$getController().onLanding();
        }
    }

    @Inject(method = "addSnowEffect", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$snowEffect(float rotation, float height, CallbackInfo ci) {
        if (this.level().isClientSide() && AlphaYetiConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
            ci.cancel();
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$tiredParticle(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if (level.isClientSide() && AlphaYetiConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
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
