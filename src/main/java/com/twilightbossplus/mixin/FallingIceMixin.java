package com.twilightbossplus.mixin;

import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.ClientConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.projectile.FallingIce;

@Mixin(FallingIce.class)
public abstract class FallingIceMixin {
    @Inject(method = "makeWarningTrail", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$warningTrail(CallbackInfo ci) {
        if (AlphaYetiConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
            ci.cancel();
        }
    }

    @Redirect(method = "causeFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$shatterParticles(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if (level.isClientSide() && AlphaYetiConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get() && level.getRandom().nextInt(5) != 0) {
            return;
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }
}
