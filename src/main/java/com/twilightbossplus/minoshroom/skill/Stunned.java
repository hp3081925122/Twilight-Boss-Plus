package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;

public final class Stunned extends MinoSkill {
    private final int duration;

    public Stunned(MinoBrain brain, int duration) {
        super(brain);
        this.duration = duration;
    }

    @Override
    public String name() {
        return "Stunned";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        MinoFx.stun(this.brain, this.mino, this.mino.getBbHeight() + 0.5, this.duration);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        this.mino.getLookControl().setLookAt(this.mino.getX() + this.forward().x, this.mino.getEyeY() - 1.5, this.mino.getZ() + this.forward().z);
        return this.age < this.duration;
    }
}
