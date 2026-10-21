package com.twilightbossplus.mixin;

import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichBrainHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.Lich;

@Mixin(Lich.class)
public abstract class LichMixin extends Monster implements LichBrainHolder {
    @Unique
    private LichBrain tbp$brain;

    protected LichMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public LichBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new LichBrain((Lich) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"), remap = false)
    private void tbp$tickBrain(CallbackInfo ci) {
        this.tbp$getBrain().tick();
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$skillInvulnerable(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.level().isClientSide() || this.tbp$brain == null || !this.tbp$brain.invulnerable()) {
            return;
        }
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.tbp$brain != null && this.tbp$brain.blocksMelee()) {
            return false;
        }
        return super.doHurtTarget(target);
    }
}
