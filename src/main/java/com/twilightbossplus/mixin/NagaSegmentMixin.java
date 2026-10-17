package com.twilightbossplus.mixin;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaControllerHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.Naga;
import twilightforest.entity.boss.NagaSegment;

@Mixin(NagaSegment.class)
public abstract class NagaSegmentMixin {
    @Redirect(method = "collideWithEntity", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true))
    private boolean twilightbossplus$segmentContact(Entity entity, DamageSource source, float amount) {
        if (!NagaConfig.ENABLED.get()) {
            return entity.hurt(source, amount);
        }
        NagaSegment self = (NagaSegment) (Object) this;
        Naga naga = self.getParent();
        float damage = ((NagaControllerHolder) naga).twilightbossplus$getController().getAttackDamage() * NagaConfig.SEGMENT_DAMAGE_MULTIPLIER.get().floatValue();
        if (entity instanceof Animal) {
            damage *= 3.0F;
        }
        boolean result = entity.hurt(source, damage);
        if (result && entity instanceof LivingEntity living) {
            living.knockback(0.4D, self.getX() - entity.getX(), self.getZ() - entity.getZ());
        }
        return result;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void twilightbossplus$weakScaleHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        NagaSegment self = (NagaSegment) (Object) this;
        if (self.level().isClientSide() || source.getEntity() == null || source.getEntity() == self.getParent()) {
            return;
        }
        ((NagaControllerHolder) self.getParent()).twilightbossplus$getController().onSegmentHurt(self);
    }
}
