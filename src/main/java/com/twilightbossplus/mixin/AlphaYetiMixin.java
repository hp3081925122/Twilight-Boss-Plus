package com.twilightbossplus.mixin;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiBrainHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(AlphaYeti.class)
public abstract class AlphaYetiMixin extends Monster implements YetiBrainHolder {
    @Unique
    private YetiBrain tbp$brain;

    protected AlphaYetiMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public YetiBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new YetiBrain((AlphaYeti) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "aiStep", at = @At("TAIL"), remap = false)
    private void tbp$tickBrain(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.tbp$getBrain().tick();
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, remap = false)
    private float tbp$tiredBonus(float amount) {
        if (this.level().isClientSide() || !YetiConfig.ENABLED.get() || !((AlphaYeti) (Object) this).isTired()) {
            return amount;
        }
        return (float) (amount * (1.0 + YetiConfig.TIRED_DAMAGE_BONUS.get()));
    }

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceIceBomb(LivingEntity target, float distanceFactor, CallbackInfo ci) {
        if (YetiConfig.ENABLED.get()) {
            ci.cancel();
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
