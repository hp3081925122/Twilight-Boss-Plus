package com.twilightbossplus.lich.skill;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.lich.LichFx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Lich;
import twilightforest.init.TFSounds;

public final class ShadowCrossfire extends LichSkill {
    private static final double RANGE = 26.0;
    private static final double BEAM_RADIUS = 0.7;
    private static final int AFTER = 10;

    private final int windup;
    private final List<Lich> casters = new ArrayList<>();
    private final List<Vec3[]> lines = new ArrayList<>();
    private final Set<Integer> hit = new HashSet<>();

    public ShadowCrossfire(LichBrain brain) {
        super(brain);
        this.windup = LichConfig.CROSSFIRE_WINDUP.get();
    }

    @Override
    public String name() {
        return "ShadowCrossfire";
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        if (target == null) {
            return;
        }
        this.casters.add(this.lich);
        this.casters.addAll(this.brain.clones());
        Vec3 aim = chest(target).add(target.getDeltaMovement().multiply(6.0, 0.0, 6.0));
        for (Lich caster : this.casters) {
            caster.getNavigation().stop();
            caster.getLookControl().setLookAt(target, 100.0F, 100.0F);
            Vec3 eye = caster.getEyePosition();
            Vec3 dir = aim.subtract(eye).normalize();
            Vec3 end = this.clip(eye, eye.add(dir.scale(RANGE)));
            this.lines.add(new Vec3[]{eye, end});
            LichFx.beam(this.brain, eye, end, BEAM_RADIUS * 2.0, this.windup, this.windup + AFTER, LichColors.TWILIGHT);
            caster.swing(InteractionHand.MAIN_HAND);
        }
        this.sound(TFSounds.LICH_AMBIENT.get(), this.lich.position(), 2.0F, 0.6F);
    }

    @Override
    protected boolean tick() {
        if (this.lines.isEmpty()) {
            return false;
        }
        this.holdStill();
        for (int i = 0; i < this.casters.size(); i++) {
            Lich caster = this.casters.get(i);
            if (caster.isAlive()) {
                caster.getNavigation().stop();
                caster.getLookControl().setLookAt(this.lines.get(i)[1].x, this.lines.get(i)[1].y, this.lines.get(i)[1].z);
            }
        }
        if (this.age == this.windup) {
            for (Vec3[] line : this.lines) {
                for (LivingEntity victim : this.brain.victimsOnSegment(line[0], line[1], BEAM_RADIUS)) {
                    if (this.hit.add(victim.getId()) && this.brain.damage(victim, LichConfig.SHADOW_CROSSFIRE.multiplier.get())) {
                        Vec3 dir = line[1].subtract(line[0]);
                        this.brain.knock(victim, dir.x, dir.z, 0.45, 0.2);
                    }
                }
                this.sound(TFSounds.LICH_SHOOT.get(), line[0], 1.6F, 0.7F);
            }
            this.brain.shake(this.lich.position(), 0.5F, 20.0F, 8);
        }
        return this.age < this.windup + AFTER;
    }
}
