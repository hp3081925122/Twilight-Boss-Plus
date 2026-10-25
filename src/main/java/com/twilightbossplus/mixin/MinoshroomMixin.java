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
import twilightforest.entity.boss.BaseTFBoss;

@Mixin(Minoshroom.class)
public abstract class MinoshroomMixin extends BaseTFBoss implements MinoBrainHolder {
    @Unique
    private MinoBrain tbp$brain;

    protected MinoshroomMixin(EntityType<? extends BaseTFBoss> type, Level level) {
        super(type, level);
    }

    @Override
    public MinoBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new MinoBrain((Minoshroom) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "aiStep", at = @At("TAIL"), remap = false)
    private void tbp$tickBrain(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.tbp$getBrain().tick();
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (this.tbp$brain != null) {
            if (this.tbp$brain.invulnerable() && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)) {
                return false;
            }
            if (this.tbp$brain.stunned()) {
                amount *= (float) (1.0 + MinoConfig.STUN_DAMAGE_BONUS.get());
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$blockMelee(net.minecraft.server.level.ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.tbp$brain != null && this.tbp$brain.blocksMelee()) {
            cir.setReturnValue(false);
        }
    }
}
