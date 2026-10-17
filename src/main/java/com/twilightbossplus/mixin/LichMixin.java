package com.twilightbossplus.mixin;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBossController;
import com.twilightbossplus.lich.LichControllerHolder;
import com.twilightbossplus.network.LichFxPacket;
import com.twilightbossplus.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Lich;

@Mixin(Lich.class)
public abstract class LichMixin implements LichControllerHolder {
    @Unique
    private LichBossController twilightbossplus$controller;

    @Override
    public LichBossController twilightbossplus$getController() {
        if (this.twilightbossplus$controller == null) {
            this.twilightbossplus$controller = new LichBossController((Lich) (Object) this);
        }
        return this.twilightbossplus$controller;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void twilightbossplus$tickController(CallbackInfo ci) {
        this.twilightbossplus$getController().tick();
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float twilightbossplus$scaleDamageTaken(float amount) {
        if (((Lich) (Object) this).isShadowClone()) {
            return amount;
        }
        return amount * this.twilightbossplus$getController().getDamageTakenMultiplier();
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void twilightbossplus$onDeath(DamageSource source, CallbackInfo ci) {
        this.twilightbossplus$getController().onDeath();
    }

    @Inject(method = "makeTeleportTrail", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$teleportTrail(double srcX, double srcY, double srcZ, double destX, double destY, double destZ, CallbackInfo ci) {
        if (!LichConfig.ENABLED.get()) {
            return;
        }
        Lich self = (Lich) (Object) this;
        if (!self.level().isClientSide()) {
            LichBossController.sendTeleport(self, new Vec3(srcX, srcY, srcZ), new Vec3(destX, destY, destZ));
        }
        ci.cancel();
    }

    @Inject(method = "makeMagicTrail", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$magicTrail(Vec3 source, Vec3 target, float red, float green, float blue, CallbackInfo ci) {
        if (!LichConfig.ENABLED.get()) {
            return;
        }
        Lich self = (Lich) (Object) this;
        if (!self.level().isClientSide()) {
            float r = red;
            float g = green;
            float b = blue;
            if (r + g + b < 0.05F) {
                r = 0.48F;
                g = 0.31F;
                b = 0.82F;
            }
            ModNetwork.sendTracking(self, new LichFxPacket(LichFxPacket.BEAM, self.getId(),
                (float) source.x, (float) source.y, (float) source.z, (float) target.x, (float) target.y, (float) target.z, r, g, b));
        }
        ci.cancel();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$save(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$getController().save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$load(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$getController().load(tag);
    }
}
