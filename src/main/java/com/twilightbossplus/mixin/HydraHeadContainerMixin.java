package com.twilightbossplus.mixin;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraBrainHolder;
import com.twilightbossplus.hydra.HydraHeadControl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHead;
import twilightforest.entity.boss.HydraHeadContainer;

@Mixin(value = HydraHeadContainer.class, remap = false)
public abstract class HydraHeadContainerMixin implements HydraHeadControl {
    @Unique
    private static final float TBP$YAW_STEP = 12.0F;
    @Unique
    private static final float TBP$PITCH_STEP = 8.0F;

    @Shadow
    private int ticksNeeded;
    @Shadow
    private int ticksProgress;
    @Shadow
    @Final
    private int headNum;
    @Shadow
    @Final
    private Hydra hydra;
    @Shadow
    @Final
    public HydraHead headEntity;
    @Unique
    private int tbp$pending = -1;

    @Override
    public void tbp$duration(int ticks) {
        this.tbp$pending = ticks;
    }

    @Override
    public int tbp$headNum() {
        return this.headNum;
    }

    @Inject(method = "advanceHeadState", at = @At("TAIL"), remap = false)
    private void tbp$applyDuration(CallbackInfo ci) {
        if (this.tbp$pending > 0 && this.ticksProgress == 0) {
            this.ticksNeeded = this.tbp$pending;
            this.tbp$pending = -1;
        }
    }

    @Inject(method = "setHeadFacing", at = @At("TAIL"), remap = false)
    private void tbp$aim(CallbackInfo ci) {
        if (!HydraConfig.ENABLED.get() || !(this.hydra instanceof HydraBrainHolder holder)) {
            return;
        }
        HydraBrain brain = holder.tbp$getBrain();
        if (!brain.aiming(this.headNum)) {
            return;
        }
        this.headEntity.setYRot(Mth.approachDegrees(this.headEntity.getYRot(), brain.aimYaw(this.headNum), TBP$YAW_STEP));
        this.headEntity.setXRot(Mth.approachDegrees(this.headEntity.getXRot(), brain.aimPitch(this.headNum), TBP$PITCH_STEP));
    }

    @Inject(method = "executeAttacks", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceAttacks(CallbackInfo ci) {
        if (HydraConfig.ENABLED.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "addMouthParticles", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$replaceParticles(CallbackInfo ci) {
        if (HydraConfig.ENABLED.get()) {
            ci.cancel();
        }
    }
}
