package com.twilightbossplus.mixin;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBrain;
import com.twilightbossplus.urghast.UrGhastBrainHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

@Mixin(UrGhast.class)
public abstract class UrGhastMixin extends CarminiteGhastguard implements UrGhastBrainHolder {
    @Unique
    private UrGhastBrain tbp$brain;

    protected UrGhastMixin(EntityType<? extends CarminiteGhastguard> type, Level level) {
        super(type, level);
    }

    @Override
    public UrGhastBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new UrGhastBrain((UrGhast) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void tbp$tickBrain(CallbackInfo ci) {
        this.tbp$getBrain().tick();
    }

    @Inject(method = "doTantrumDamageEffects", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceTantrumDamage(CallbackInfo ci) {
        if (UrGhastConfig.ENABLED.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "spitFireball", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceFireball(CallbackInfo ci) {
        if (UrGhastConfig.ENABLED.get()) {
            ci.cancel();
        }
    }
}
