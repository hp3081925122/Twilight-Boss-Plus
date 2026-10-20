package com.twilightbossplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(KnightPhantom.class)
public interface KnightPhantomAccessor {
    @Invoker(value = "setChargingAtPlayer", remap = false)
    void tbp$setCharging(boolean flag);
}
