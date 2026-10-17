package com.twilightbossplus.mixin;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.naga.NagaControllerHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.Naga;

@Mixin(Naga.class)
public abstract class NagaMixin implements NagaControllerHolder {
    @Unique
    private NagaBossController twilightbossplus$controller;

    @Override
    public NagaBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new NagaBossController((Naga) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        this.twilightbossplus$getController().tick();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void twilightbossplus$afterMoveSegments(CallbackInfo ci) {
        Naga self = (Naga) (Object) this;
        if (!self.level().isClientSide()) {
            this.twilightbossplus$getController().afterMove();
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$shedInvulnerable(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.twilightbossplus$getController().isInvulnerable()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V", ordinal = 1))
    private void twilightbossplus$meleeKnockback(Entity target, double x, double y, double z) {
        if (!NagaConfig.ENABLED.get()) {
            target.push(x, y, z);
            return;
        }
        if (target instanceof LivingEntity living) {
            float yaw = ((Naga) (Object) this).getYRot() * Mth.DEG_TO_RAD;
            living.knockback(NagaConfig.MELEE_KNOCKBACK.get(), Mth.sin(yaw), -Mth.cos(yaw));
            return;
        }
        target.push(x, y, z);
    }

    @ModifyArg(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float twilightbossplus$stunlessBlockDamage(float amount) {
        if (!NagaConfig.ENABLED.get()) {
            return amount;
        }
        return this.twilightbossplus$getController().getAttackDamage() * NagaConfig.STUNLESS_BLOCK_MULTIPLIER.get().floatValue();
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
