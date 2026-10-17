package com.twilightbossplus.mixin;

import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBossController;
import com.twilightbossplus.hydra.HydraControllerHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHeadContainer;
import twilightforest.entity.boss.HydraPart;

@Mixin(value = HydraHeadContainer.class, remap = false)
public abstract class HydraHeadContainerMixin {
    @Shadow
    @Final
    private Hydra hydra;

    @Shadow
    @Final
    private int headNum;

    @Unique
    private HydraBossController twilightbossplus$controller() {
        return ((HydraControllerHolder) this.hydra).twilightbossplus$getController();
    }

    @Redirect(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 0, remap = true))
    private boolean twilightbossplus$bite(Entity target, DamageSource source, float amount) {
        if (!HydraConfig.ENABLED.get()) {
            return target.hurt(source, amount);
        }
        boolean hurt = target.hurt(source, this.twilightbossplus$controller().getBiteDamage(amount));
        if (hurt && target instanceof LivingEntity living) {
            this.twilightbossplus$controller().applyBiteKnockback(living);
        }
        return hurt;
    }

    @Redirect(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", ordinal = 1, remap = true))
    private boolean twilightbossplus$flame(Entity target, DamageSource source, float amount) {
        return target.hurt(source, this.twilightbossplus$controller().getFlameDamage(this.headNum, amount));
    }

    @ModifyArg(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V", remap = true))
    private int twilightbossplus$flameBurn(int seconds) {
        return this.twilightbossplus$controller().getFlameBurnSeconds(seconds);
    }

    @Redirect(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V", remap = true))
    private void twilightbossplus$biteKnockback(LivingEntity living, double strength, double x, double z) {
        if (!HydraConfig.ENABLED.get()) {
            living.knockback(strength, x, z);
        }
    }

    @Redirect(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;send(Lnet/minecraftforge/network/PacketDistributor$PacketTarget;Ljava/lang/Object;)V"))
    private void twilightbossplus$throwPlayer(SimpleChannel channel, PacketDistributor.PacketTarget target, Object packet) {
        if (!HydraConfig.ENABLED.get()) {
            channel.send(target, packet);
        }
    }

    @Redirect(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;stopUsingItem()V", remap = true))
    private void twilightbossplus$shieldBlocked(Player player) {
        player.stopUsingItem();
        if (HydraConfig.ENABLED.get()) {
            this.twilightbossplus$controller().applyBiteKnockback(player);
        }
    }

    @ModifyConstant(method = "executeAttacks", constant = @Constant(intValue = 112))
    private int twilightbossplus$shieldDamage(int original) {
        if (!HydraConfig.ENABLED.get()) {
            return original;
        }
        return HydraConfig.BITE_SHIELD_DAMAGE.get();
    }

    @ModifyConstant(method = "executeAttacks", constant = @Constant(intValue = 200))
    private int twilightbossplus$shieldCooldown(int original) {
        if (!HydraConfig.ENABLED.get()) {
            return original;
        }
        return HydraConfig.BITE_SHIELD_COOLDOWN.get();
    }

    @Redirect(method = "executeAttacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean twilightbossplus$mortar(Level level, Entity entity) {
        if (this.twilightbossplus$controller().consumeSlagShot(this.headNum)) {
            return false;
        }
        return level.addFreshEntity(entity);
    }

    @ModifyArg(method = "setHeadFacing", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/HydraHeadContainer;moveTargetCoordsTowardsTargetEntity(D)V"))
    private double twilightbossplus$tracking(double speed) {
        return speed * this.twilightbossplus$controller().getTrackingScale();
    }

    @Inject(method = "addMouthParticles", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$mouthParticles(CallbackInfo ci) {
        if (HydraConfig.ENABLED.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "doExplosionOn", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$headExplosion(HydraPart part, boolean large, CallbackInfo ci) {
        if (HydraConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
            ci.cancel();
        }
    }
}
