package com.twilightbossplus.mixin;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.network.LichFxPacket;
import com.twilightbossplus.network.ModNetwork;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.monster.LichMinion;
import twilightforest.entity.projectile.LichBomb;

@Mixin(LichBomb.class)
public abstract class LichBombMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilightbossplus$burst(CallbackInfo ci) {
        if (!LichConfig.ENABLED.get()) {
            return;
        }
        ci.cancel();
        LichBomb bomb = (LichBomb) (Object) this;
        Level level = bomb.level();
        if (level.isClientSide() || bomb.isRemoved()) {
            return;
        }
        Entity owner = bomb.getOwner();
        float attack = 3.0F;
        if (owner instanceof Lich lich) {
            attack = (float) lich.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        float damage = attack * LichConfig.BOMB_MULTIPLIER.get().floatValue();
        double radius = LichConfig.BOMB_RADIUS.get();
        double knockback = LichConfig.BOMB_KNOCKBACK.get();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(bomb.position(), bomb.position()).inflate(radius))) {
            if (living instanceof Lich || living instanceof LichMinion || !living.isAlive()) {
                continue;
            }
            double dx = living.getX() - bomb.getX();
            double dy = living.getY() + living.getBbHeight() * 0.5D - bomb.getY();
            double dz = living.getZ() - bomb.getZ();
            if (dx * dx + dy * dy + dz * dz > radius * radius) {
                continue;
            }
            if (living.hurt(level.damageSources().indirectMagic(bomb, owner), damage)) {
                living.knockback(knockback, -dx, -dz);
            }
        }
        ModNetwork.sendTracking(bomb, new LichFxPacket(LichFxPacket.BOMB_BURST, bomb.getId(), (float) bomb.getX(), (float) bomb.getY(), (float) bomb.getZ(), (float) radius));
        level.playSound(null, bomb.getX(), bomb.getY(), bomb.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 1.4F);
        level.playSound(null, bomb.getX(), bomb.getY(), bomb.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        bomb.discard();
    }
}
