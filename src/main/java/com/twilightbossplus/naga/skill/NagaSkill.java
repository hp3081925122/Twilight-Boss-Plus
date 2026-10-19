package com.twilightbossplus.naga.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.naga.NagaBrain;
import twilightforest.entity.boss.Naga;

public abstract class NagaSkill extends BossSkill<Naga, NagaBrain> {
    protected final Naga naga;

    protected NagaSkill(NagaBrain brain) {
        super(brain);
        this.naga = brain.naga();
    }

    @Override
    public void start() {
        super.start();
        this.naga.setCharging(false);
    }
}
