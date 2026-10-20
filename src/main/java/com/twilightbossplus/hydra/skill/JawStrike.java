package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraFx;
import com.twilightbossplus.hydra.HydraHeads;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class JawStrike extends HydraSkill {
    public static final double MIN_RANGE = 3.0;
    public static final double MAX_RANGE = 11.0;
    private static final int RAISE = 14;
    private static final int READY = 8;
    private static final int HIT = RAISE + READY + 2;
    private static final double RADIUS = 2.4;

    private int head = -1;
    private Vec3 point = Vec3.ZERO;
    private boolean finished;

    public JawStrike(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "JawStrike";
    }

    @Override
    public void start() {
        LivingEntity target = this.target();
        List<Integer> front = this.brain.aliveHeads(0, 1, 2);
        if (target == null || front.isEmpty()) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (int index : front) {
            double distance = this.brain.headPos(index).distanceToSqr(target.position());
            if (distance < best) {
                best = distance;
                this.head = index;
            }
        }
        this.heads.add(this.head);
        this.point = this.brain.ground(this.lead(target, 6.0));
        this.aimAt(this.head, this.point);
        this.command(HydraHeads.BITE_BEGINNING, RAISE);
        HydraFx.bite(this.brain, this.point, RADIUS, HIT);
        this.sound(TFSounds.HYDRA_GROWL.get(), this.brain.headPos(this.head), 2.0F, 1.2F);
    }

    @Override
    protected boolean tick() {
        if (this.head < 0 || !this.brain.headAlive(this.head)) {
            return false;
        }
        this.aimAt(this.head, this.point.add(0.0, 1.0, 0.0));
        if (this.age == RAISE) {
            this.command(HydraHeads.BITE_READY, READY + 20);
            this.sound(TFSounds.HYDRA_WARN.get(), this.brain.headPos(this.head), 1.6F, 1.3F);
        }
        if (this.age == RAISE + READY) {
            this.command(HydraHeads.BITING, 7);
        }
        if (this.age == HIT) {
            this.strike();
        }
        if (this.age >= HIT + 4) {
            this.finished = true;
            this.release(HydraHeads.BITE_ENDING, 20);
            return false;
        }
        return true;
    }

    private void strike() {
        Vec3 origin = this.hydra.position();
        for (LivingEntity victim : this.brain.victimsAround(this.point, RADIUS, 2.5)) {
            double dx = victim.getX() - origin.x;
            double dz = victim.getZ() - origin.z;
            if (victim.isBlocking() && victim instanceof Player player) {
                player.disableShield(true);
                this.brain.knock(victim, dx, dz, 0.8, 0.3);
                this.sound(SoundEvents.SHIELD_BREAK, victim.position(), 1.2F, 0.8F);
                continue;
            }
            if (this.brain.damage(victim, HydraConfig.JAW_STRIKE.multiplier.get())) {
                this.brain.knock(victim, dx, dz, 0.6, 0.3);
            }
        }
        this.brain.shake(this.point, 0.6F, 12.0F, 8);
        this.sound(SoundEvents.EVOKER_FANGS_ATTACK, this.point, 2.0F, 0.6F);
        this.sound(SoundEvents.PLAYER_ATTACK_CRIT, this.point, 1.5F, 0.6F);
    }

    @Override
    public void end() {
        if (!this.finished && this.head >= 0) {
            this.release(HydraHeads.BITE_ENDING, 20);
        }
    }
}
