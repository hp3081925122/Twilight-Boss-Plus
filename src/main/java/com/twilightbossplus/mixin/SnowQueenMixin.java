package com.twilightbossplus.mixin;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.SnowQueenBossController;
import com.twilightbossplus.queen.SnowQueenControllerHolder;
import com.twilightbossplus.queen.SnowQueenSkillGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.SnowQueen;

@Mixin(SnowQueen.class)
public abstract class SnowQueenMixin extends Monster implements SnowQueenControllerHolder {
    @Unique
    private SnowQueenBossController twilightbossplus$controller;

    protected SnowQueenMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public SnowQueenBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new SnowQueenBossController((SnowQueen) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void twilightbossplus$registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new SnowQueenSkillGoal((SnowQueen) (Object) this));
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.twilightbossplus$getController().tick();
        }
    }

    @Inject(method = "spawnParticles", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$particles(CallbackInfo ci) {
        if (SnowQueenConfig.ENABLED.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "applyShieldCollision", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$shieldCollision(Entity collider, Entity collided, CallbackInfo ci) {
        if (SnowQueenConfig.ENABLED.get()) {
            ci.cancel();
            this.twilightbossplus$getController().onShieldHit(collider, collided);
        }
    }

    @ModifyArg(method = "doBreathAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true), index = 1, remap = false)
    private float twilightbossplus$beamDamage(float original) {
        if (SnowQueenConfig.ENABLED.get()) {
            return this.twilightbossplus$getController().beamDamage();
        }
        return original;
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
