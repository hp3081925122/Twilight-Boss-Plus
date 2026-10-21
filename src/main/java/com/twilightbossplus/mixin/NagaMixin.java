package com.twilightbossplus.mixin;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaBrainHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.Naga;

@Mixin(Naga.class)
public abstract class NagaMixin extends Monster implements NagaBrainHolder {
    @Unique
    private NagaBrain tbp$brain;

    protected NagaMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public NagaBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new NagaBrain((Naga) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"), remap = false)
    private void tbp$tickBrain(CallbackInfo ci) {
        this.tbp$getBrain().tick();
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$skillInvulnerable(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!this.level().isClientSide() && this.tbp$brain != null && this.tbp$brain.invulnerable()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$blockMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.tbp$brain != null && this.tbp$brain.blocksMelee()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"), remap = false)
    private void tbp$scaleMeleePush(Entity target, double x, double y, double z) {
        if (!NagaConfig.ENABLED.get() || !(target instanceof LivingEntity living)) {
            target.push(x, y, z);
            return;
        }
        double scale = Math.max(0.0, 1.0 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        double horizontal = scale * NagaConfig.MELEE_KNOCKBACK.get();
        target.push(x * horizontal, y * scale, z * horizontal);
    }
}
