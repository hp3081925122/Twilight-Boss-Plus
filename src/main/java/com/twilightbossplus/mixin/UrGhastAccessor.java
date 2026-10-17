package com.twilightbossplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import twilightforest.entity.boss.UrGhast;

@Mixin(value = UrGhast.class, remap = false)
public interface UrGhastAccessor {
    @Accessor("damageUntilNextPhase")
    void twilightbossplus$setDamageUntilNextPhase(float value);

    @Invoker("startTantrum")
    void twilightbossplus$startTantrum();
}
