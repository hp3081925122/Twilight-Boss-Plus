package com.twilightbossplus.mixin;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBossController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraMortar;

@Mixin(value = HydraMortar.class, remap = false)
public abstract class HydraMortarMixin {
    @Shadow
    private boolean megaBlast;

    @ModifyConstant(method = "detonate", constant = @Constant(floatValue = 18.0F))
    private float twilightbossplus$damage(float original) {
        Entity owner = ((HydraMortar) (Object) this).getOwner();
        if (!HydraConfig.ENABLED.get() || !(owner instanceof Hydra hydra)) {
            return original;
        }
        return HydraBossController.attackOf(hydra) * HydraConfig.MORTAR_MULTIPLIER.get().floatValue();
    }

    @ModifyArg(method = "detonate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V", remap = true))
    private int twilightbossplus$burn(int seconds) {
        if (!HydraConfig.ENABLED.get()) {
            return seconds;
        }
        return HydraConfig.MORTAR_BURN_SECONDS.get();
    }

    @ModifyConstant(method = "detonate", constant = @Constant(floatValue = 4.0F))
    private float twilightbossplus$power(float original) {
        if (!HydraConfig.ENABLED.get()) {
            return original;
        }
        return HydraConfig.MORTAR_MEGA_BLAST_POWER.get().floatValue();
    }

    @Redirect(method = "detonate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;", remap = true))
    private Explosion twilightbossplus$explode(Level level, Entity source, double x, double y, double z, float power, boolean fire, Level.ExplosionInteraction interaction) {
        if (HydraConfig.ENABLED.get() && !HydraConfig.MORTAR_BREAK_BLOCKS.get()) {
            return level.explode(source, x, y, z, power, false, Level.ExplosionInteraction.NONE);
        }
        return level.explode(source, x, y, z, power, fire, interaction);
    }

    @Inject(method = "setToBlasting", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$blasting(CallbackInfo ci) {
        if (HydraConfig.ENABLED.get() && !HydraConfig.MORTAR_MEGA_BLAST.get()) {
            this.megaBlast = false;
            ci.cancel();
        }
    }
}
