package com.twilightbossplus.mixin;

import com.twilightbossplus.urghast.UrGhastBossController;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.projectile.UrGhastFireball;

@Mixin(UrGhastFireball.class)
public abstract class UrGhastFireballMixin extends LargeFireball {
    protected UrGhastFireballMixin(EntityType<? extends LargeFireball> type, Level level) {
        super(type, level);
    }

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean twilightbossplus$fireballDamage(Entity hit, DamageSource source, float amount) {
        return hit.hurt(source, UrGhastBossController.fireballDamage(this.getOwner(), hit, amount));
    }
}
