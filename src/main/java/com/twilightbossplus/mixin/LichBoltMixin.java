package com.twilightbossplus.mixin;

import com.twilightbossplus.config.LichConfig;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.projectile.LichBolt;

@Mixin(LichBolt.class)
public abstract class LichBoltMixin {
    @ModifyArg(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float twilightbossplus$boltDamage(float amount) {
        if (!LichConfig.ENABLED.get()) {
            return amount;
        }
        if (((LichBolt) (Object) this).getOwner() instanceof Lich lich) {
            return (float) lich.getAttributeValue(Attributes.ATTACK_DAMAGE) * LichConfig.BOLT_MULTIPLIER.get().floatValue();
        }
        return amount;
    }
}
