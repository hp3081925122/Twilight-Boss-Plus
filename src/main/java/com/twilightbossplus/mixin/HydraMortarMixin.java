package com.twilightbossplus.mixin;

import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraBrainHolder;
import com.twilightbossplus.hydra.HydraFx;
import com.twilightbossplus.hydra.HydraMortarControl;
import com.twilightbossplus.hydra.hazard.MagmaPool;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraMortar;
import twilightforest.entity.boss.HydraPart;
import twilightforest.init.TFDamageTypes;
import twilightforest.init.TFEntities;

@Mixin(HydraMortar.class)
public abstract class HydraMortarMixin extends ThrowableProjectile implements HydraMortarControl {
    @Unique
    private boolean tbp$enhanced;
    @Unique
    @Nullable
    private Hydra tbp$hydra;
    @Unique
    private double tbp$radius;
    @Unique
    private double tbp$multiplier;
    @Unique
    private double tbp$poolMultiplier;
    @Unique
    private int tbp$poolTicks;

    protected HydraMortarMixin(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public void tbp$configure(Hydra hydra, double radius, double multiplier, double poolMultiplier, int poolTicks) {
        this.tbp$enhanced = true;
        this.tbp$hydra = hydra;
        this.tbp$radius = radius;
        this.tbp$multiplier = multiplier;
        this.tbp$poolMultiplier = poolMultiplier;
        this.tbp$poolTicks = poolTicks;
    }

    @Inject(method = "detonate", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbp$detonate(CallbackInfo ci) {
        if (!this.tbp$enhanced) {
            return;
        }
        ci.cancel();
        if (this.level().isClientSide()) {
            this.discard();
            return;
        }
        Vec3 pos = this.position();
        HydraBrain brain = this.tbp$hydra instanceof HydraBrainHolder holder ? holder.tbp$getBrain() : null;
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.4F);
        if (brain != null) {
            HydraFx.burst(brain, pos, this.tbp$radius);
            brain.shake(pos, 0.35F, 10.0F, 6);
        }
        Entity owner = this.getOwner();
        boolean reflected = owner != null && !(owner instanceof Hydra);
        if (reflected) {
            for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(1.5))) {
                if (!entity.fireImmune() || entity instanceof Hydra || entity instanceof HydraPart) {
                    if (entity.hurt(TFDamageTypes.getIndirectEntityDamageSource(this.level(), TFDamageTypes.HYDRA_MORTAR, this, owner, TFEntities.HYDRA.get()), 18.0F)) {
                        entity.igniteForSeconds(5.0F);
                    }
                }
            }
        } else if (brain != null) {
            for (LivingEntity victim : brain.victimsAround(pos, this.tbp$radius, 2.5)) {
                if (brain.damage(victim, this.tbp$multiplier)) {
                    victim.igniteForSeconds(3.0F);
                    brain.knockFrom(victim, pos, 0.35, 0.25);
                }
            }
            if (this.tbp$poolTicks > 0) {
                MagmaPool pool = new MagmaPool(brain.ground(pos), this.tbp$radius * 0.85, this.tbp$poolTicks, this.tbp$poolMultiplier);
                brain.addHazard(pool);
                pool.announce(brain);
            }
        }
        this.discard();
    }
}
