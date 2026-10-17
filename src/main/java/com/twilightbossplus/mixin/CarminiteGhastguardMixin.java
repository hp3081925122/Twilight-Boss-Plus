package com.twilightbossplus.mixin;

import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.UrGhastConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

@Mixin(CarminiteGhastguard.class)
public abstract class CarminiteGhastguardMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$dust(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if ((Object) this instanceof UrGhast && level.isClientSide() && UrGhastConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }
}
