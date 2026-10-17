package com.twilightbossplus.mixin;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBossController;
import com.twilightbossplus.hydra.HydraControllerHolder;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHeadContainer;

@Mixin(Hydra.class)
public abstract class HydraMixin extends Mob implements HydraControllerHolder {
    @Unique
    private HydraBossController twilightbossplus$controller;

    protected HydraMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Override
    public HydraBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new HydraBossController((Hydra) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        this.twilightbossplus$getController().tick();
    }

    @ModifyConstant(method = "customServerAiStep", constant = @Constant(intValue = 120))
    private int twilightbossplus$severThreshold(int original) {
        return this.twilightbossplus$getController().getSeverThreshold(original);
    }

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/HydraHeadContainer;setRespawnCounter(I)V", remap = false))
    private void twilightbossplus$respawnTicks(HydraHeadContainer container, int ticks) {
        container.setRespawnCounter(this.twilightbossplus$getController().getRespawnTicks(ticks));
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$attackEntity(Entity target, float distance, CallbackInfo ci) {
        if (this.twilightbossplus$getController().isBusy()) {
            ci.cancel();
        }
    }

    @Inject(method = "collideWithEntities", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$collide(List<Entity> entities, Entity part, CallbackInfo ci) {
        if (!HydraConfig.ENABLED.get()) {
            return;
        }
        ci.cancel();
        if (!this.level().isClientSide()) {
            this.twilightbossplus$getController().collide(entities, part);
        }
    }

    @ModifyConstant(method = "attackEntityFromPart", constant = @Constant(floatValue = 8.0F), remap = false)
    private float twilightbossplus$bodyDivisor(float original) {
        return this.twilightbossplus$getController().getBodyDivisor(original);
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(intValue = 1000))
    private int twilightbossplus$regenDelay(int original) {
        return this.twilightbossplus$getController().getRegenDelay(original);
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(intValue = 5))
    private int twilightbossplus$regenInterval(int original) {
        return Math.max(1, this.twilightbossplus$getController().getRegenInterval(original));
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 1.0F))
    private float twilightbossplus$regenAmount(float original) {
        return this.twilightbossplus$getController().getRegenAmount(original);
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
}
