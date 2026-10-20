package com.twilightbossplus.mixin;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightBrainHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(KnightPhantom.class)
public abstract class KnightPhantomMixin extends FlyingMob implements KnightBrainHolder {
    @Unique
    private KnightBrain tbp$brain;

    protected KnightPhantomMixin(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
    }

    @Override
    public KnightBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new KnightBrain((KnightPhantom) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void tbp$tickBrain(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.tbp$getBrain().tick();
        }
    }

    @Inject(method = "switchToFormation", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$holdFormation(KnightPhantom.Formation formation, CallbackInfo ci) {
        if (KnightConfig.ENABLED.get() && this.tbp$getBrain().busy()) {
            ci.cancel();
        }
    }
}
