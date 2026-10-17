package com.twilightbossplus.queen;

import com.twilightbossplus.config.SnowQueenConfig;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.SnowQueen;

public class SnowQueenSkillGoal extends Goal {
    private final SnowQueen mob;

    public SnowQueenSkillGoal(SnowQueen mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean busy() {
        return SnowQueenConfig.ENABLED.get() && ((SnowQueenControllerHolder) this.mob).twilightbossplus$getController().isBusy();
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
    public void tick() {
        Vec3 motion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(motion.x * 0.6D, 0.0D, motion.z * 0.6D);
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
