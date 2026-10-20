package com.twilightbossplus.mixin;

import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoBrainHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.entity.monster.Minotaur;

@Mixin(Minoshroom.class)
public abstract class MinoshroomMixin extends Minotaur implements MinoBrainHolder {
    @Unique
    private MinoBrain tbp$brain;

    protected MinoshroomMixin(EntityType<? extends Minotaur> type, Level level) {
        super(type, level);
    }

    @Override
    public MinoBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new MinoBrain((Minoshroom) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void tbp$tickBrain(CallbackInfo ci) {
        this.tbp$getBrain().tick();
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void tbp$skillInvulnerable(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.level().isClientSide() || this.tbp$brain == null || !this.tbp$brain.invulnerable()) {
            return;
        }
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float tbp$stunBonus(float amount) {
        if (this.level().isClientSide() || this.tbp$brain == null || !this.tbp$brain.stunned()) {
            return amount;
        }
        return (float) (amount * (1.0 + MinoConfig.STUN_DAMAGE_BONUS.get()));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.tbp$brain != null && this.tbp$brain.blocksMelee()) {
            return false;
        }
        return super.doHurtTarget(target);
    }
}
