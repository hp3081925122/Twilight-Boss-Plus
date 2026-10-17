package com.twilightbossplus.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import twilightforest.entity.boss.Naga;

public class VenomPool extends Entity {
    public static final float RADIUS = 1.5F;
    public static final int LIFETIME = 100;

    public VenomPool(EntityType<? extends VenomPool> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.tickCount >= LIFETIME) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 != 1) {
            return;
        }
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.0D, 1.2D, 0.0D))) {
            if (living instanceof Naga || !living.isAlive()) {
                continue;
            }
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            if (dx * dx + dz * dz > RADIUS * RADIUS) {
                continue;
            }
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), this);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.tickCount);
    }
}
