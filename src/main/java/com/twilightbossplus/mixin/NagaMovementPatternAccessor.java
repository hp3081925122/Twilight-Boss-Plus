package com.twilightbossplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import twilightforest.entity.ai.goal.NagaMovementPattern;

@Mixin(NagaMovementPattern.class)
public interface NagaMovementPatternAccessor {
    @Accessor(value = "stateCounter", remap = false)
    void setStateCounter(int value);
}
