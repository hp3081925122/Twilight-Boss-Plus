package com.twilightbossplus.yeti;

import com.twilightbossplus.config.AlphaYetiConfig;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import twilightforest.entity.boss.AlphaYeti;

public class AlphaYetiSkillGoal extends Goal {
    private final AlphaYeti mob;

    public AlphaYetiSkillGoal(AlphaYeti mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private boolean busy() {
        return AlphaYetiConfig.ENABLED.get() && ((AlphaYetiControllerHolder) this.mob).twilightbossplus$getController().isBusy();
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
