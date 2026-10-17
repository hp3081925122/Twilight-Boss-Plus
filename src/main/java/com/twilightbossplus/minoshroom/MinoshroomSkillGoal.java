package com.twilightbossplus.minoshroom;

import com.twilightbossplus.config.MinoshroomConfig;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import twilightforest.entity.boss.Minoshroom;

public class MinoshroomSkillGoal extends Goal {
    private final Minoshroom mob;

    public MinoshroomSkillGoal(Minoshroom mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean busy() {
        return MinoshroomConfig.ENABLED.get() && ((MinoshroomControllerHolder) this.mob).twilightbossplus$getController().isBusy();
    }

    @Override
    public boolean canUse() {
        return this.busy();
    }

    @Override
    public boolean canContinueToUse() {
        return this.busy();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
