package com.twilightbossplus.mixin;

import com.twilightbossplus.config.MinoshroomConfig;
import com.twilightbossplus.minoshroom.MinoshroomBossController;
import com.twilightbossplus.minoshroom.MinoshroomControllerHolder;
import com.twilightbossplus.minoshroom.MinoshroomSkillGoal;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.entity.monster.Minotaur;

@Mixin(Minoshroom.class)
public abstract class MinoshroomMixin extends Minotaur implements MinoshroomControllerHolder {
    @Unique
    private MinoshroomBossController twilightbossplus$controller;

    protected MinoshroomMixin(EntityType<? extends Minotaur> type, Level level) {
        super(type, level);
    }

    @Override
    public MinoshroomBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new MinoshroomBossController((Minoshroom) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void twilightbossplus$registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new MinoshroomSkillGoal((Minoshroom) (Object) this));
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        this.twilightbossplus$getController().tick();
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void twilightbossplus$onDeath(DamageSource source, CallbackInfo ci) {
        this.twilightbossplus$getController().onDeath();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$save(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$getController().save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$load(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$getController().load(tag);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$slamParticles(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if (MinoshroomConfig.ENABLED.get()) {
            return;
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }
}
