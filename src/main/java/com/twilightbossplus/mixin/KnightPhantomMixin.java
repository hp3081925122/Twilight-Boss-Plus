package com.twilightbossplus.mixin;

import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.KnightPhantomConfig;
import com.twilightbossplus.knight.KnightDataHolder;
import com.twilightbossplus.knight.KnightGroup;
import com.twilightbossplus.knight.KnightGroups;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(KnightPhantom.class)
public abstract class KnightPhantomMixin extends FlyingMob implements KnightDataHolder {
    @Unique
    private int twilightbossplus$savedPhase;
    @Unique
    private int twilightbossplus$savedInitial;

    protected KnightPhantomMixin(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
    }

    @Override
    public int twilightbossplus$getSavedPhase() {
        return this.twilightbossplus$savedPhase;
    }

    @Override
    public int twilightbossplus$getSavedInitial() {
        return this.twilightbossplus$savedInitial;
    }

    @Unique
    private void twilightbossplus$addModifier(AttributeInstance instance, AttributeModifier modifier) {
        if (!KnightPhantomConfig.ENABLED.get()) {
            instance.addTransientModifier(modifier);
            return;
        }
        if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
            return;
        }
        double amount = KnightPhantomConfig.NON_CHARGING_ARMOR_MULTIPLIER.get() - 1.0D;
        if (amount <= 0.0D) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(modifier.getId(), modifier.getName(), amount, modifier.getOperation()));
    }

    @Redirect(method = "setChargingAtPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addTransientModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V", remap = true), remap = false)
    private void twilightbossplus$chargingModifier(AttributeInstance instance, AttributeModifier modifier) {
        this.twilightbossplus$addModifier(instance, modifier);
    }

    @Redirect(method = "finalizeSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addTransientModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"))
    private void twilightbossplus$spawnModifier(AttributeInstance instance, AttributeModifier modifier) {
        this.twilightbossplus$addModifier(instance, modifier);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void twilightbossplus$ensureGroup(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            KnightGroups.of((KnightPhantom) (Object) this);
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$chargeParticles(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if (KnightPhantomConfig.ENABLED.get()) {
            return;
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }

    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void twilightbossplus$deathParticles(Level level, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
        if (level.isClientSide() && KnightPhantomConfig.ENABLED.get() && ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        level.addParticle(options, x, y, z, dx, dy, dz);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$melee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        KnightGroup group = KnightGroups.of((KnightPhantom) (Object) this);
        if (group == null) {
            return;
        }
        Boolean result = group.onMelee((KnightPhantom) (Object) this, entity);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void twilightbossplus$death(DamageSource source, CallbackInfo ci) {
        KnightGroup group = KnightGroups.of((KnightPhantom) (Object) this);
        if (group != null) {
            group.onDeath((KnightPhantom) (Object) this);
        }
    }

    @Inject(method = "setNumber", at = @At("TAIL"), remap = false)
    private void twilightbossplus$keepWeapon(int number, CallbackInfo ci) {
        KnightGroup group = KnightGroups.of((KnightPhantom) (Object) this);
        if (group == null) {
            return;
        }
        ItemStack weapon = group.weaponOverride((KnightPhantom) (Object) this);
        if (weapon != null) {
            this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$save(CompoundTag tag, CallbackInfo ci) {
        KnightGroup group = KnightGroups.of((KnightPhantom) (Object) this);
        if (group != null) {
            group.save(tag);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void twilightbossplus$load(CompoundTag tag, CallbackInfo ci) {
        this.twilightbossplus$savedPhase = tag.getInt(KnightGroup.PHASE_TAG);
        this.twilightbossplus$savedInitial = tag.getInt(KnightGroup.INITIAL_TAG);
    }
}
