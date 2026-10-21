package com.twilightbossplus.mixin;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraBrainHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Hydra;

@Mixin(Hydra.class)
public abstract class HydraMixin extends Mob implements HydraBrainHolder {
    @Unique
    private HydraBrain tbp$brain;

    protected HydraMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Override
    public HydraBrain tbp$getBrain() {
        if (this.tbp$brain == null) {
            this.tbp$brain = new HydraBrain((Hydra) (Object) this);
        }
        return this.tbp$brain;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"), remap = false)
    private void tbp$tickBrain(CallbackInfo ci) {
        this.tbp$getBrain().tick();
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceAttacks(Entity target, float distance, CallbackInfo ci) {
        if (HydraConfig.ENABLED.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "secondaryAttacks", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceSecondary(CallbackInfo ci) {
        if (HydraConfig.ENABLED.get()) {
            ci.cancel();
        }
    }
}
