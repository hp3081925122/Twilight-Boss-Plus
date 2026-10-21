package com.twilightbossplus.mixin;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenBrainHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.SnowQueen;

@Mixin(SnowQueen.class)
public abstract class SnowQueenMixin extends Monster implements QueenBrainHolder {
    @Unique
    private QueenBrain tbp$brain;

    protected SnowQueenMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public QueenBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new QueenBrain((SnowQueen) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"), remap = false)
    private void tbp$tickBrain(CallbackInfo ci) {
        this.tbp$getBrain().tick();
    }

    @Inject(method = "hurt", at = @At("HEAD"), remap = false)
    private void tbp$recordHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.level().isClientSide() && this.tbp$brain != null && SnowQueenConfig.ENABLED.get()) {
            this.tbp$brain.onHurt(source);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, remap = false)
    private float tbp$shellReduction(float amount) {
        if (this.level().isClientSide() || this.tbp$brain == null) {
            return amount;
        }
        return this.tbp$brain.damageScale(amount);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$blockMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.tbp$brain != null && this.tbp$brain.blocksMelee()) {
            cir.setReturnValue(false);
        }
    }
}
