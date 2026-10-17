package com.twilightbossplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import twilightforest.entity.boss.HydraHeadContainer;

@Mixin(value = HydraHeadContainer.class, remap = false)
public interface HydraHeadContainerAccessor {
    @Accessor("targetX")
    void twilightbossplus$setTargetX(double value);

    @Accessor("targetY")
    void twilightbossplus$setTargetY(double value);

    @Accessor("targetZ")
    void twilightbossplus$setTargetZ(double value);
}
