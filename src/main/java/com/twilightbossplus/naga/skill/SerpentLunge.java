package com.twilightbossplus.naga.skill;

import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaFx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class SerpentLunge extends NagaSkill {
    private static final double SPEED = 1.35;
    private static final double MIN_LENGTH = 10.0;
    private static final double MAX_LENGTH = 22.0;
    private static final double WIDTH = 2.8;

    private final int windup;
    private final double multiplier;
    private final Set<Integer> hit = new HashSet<>();
    private int remaining;
    private boolean dashing;
    private int timer;
    private Vec3 direction = Vec3.ZERO;
    private double length;
    private double travelled;

    public SerpentLunge(NagaBrain brain, int lunges, int windup, double multiplier) {
        super(brain);
        this.remaining = lunges;
        this.windup = windup;
        this.multiplier = multiplier;
    }

    @Override
    public String name() {
        if (this.remaining > 1) {
            return "serpent_chain";
        }
        return "serpent_lunge";
    }

    @Override
    public void start() {
        super.start();
        this.aim();
    }

    private boolean aim() {
        LivingEntity target = this.target();
        if (target == null) {
            return false;
        }
        Vec3 from = this.naga.position();
        Vec3 offset = new Vec3(target.getX() - from.x, 0.0, target.getZ() - from.z);
        double distance = offset.length();
        if (distance < 1.0E-3) {
            offset = Vec3.directionFromRotation(0.0F, this.naga.getYRot());
            distance = 1.0;
        }
        this.direction = offset.normalize();
        this.length = Mth.clamp(distance + 6.0, MIN_LENGTH, MAX_LENGTH);
        this.travelled = 0.0;
        this.dashing = false;
        this.timer = 0;
        this.hit.clear();
        this.remaining--;
        this.setYaw((float) (Math.toDegrees(Math.atan2(this.direction.z, this.direction.x)) - 90.0));
        Vec3 end = from.add(this.direction.scale(this.length));
        NagaFx.lunge(this.brain, new Vec3(from.x, this.brain.groundY(from.x, from.z, from.y), from.z), new Vec3(end.x, this.brain.groundY(end.x, end.z, from.y), end.z), WIDTH, this.windup);
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 3.0F, 1.1F + this.naga.getRandom().nextFloat() * 0.2F);
        return true;
    }

    @Override
    protected boolean tick() {
        this.timer++;
        if (!this.dashing) {
            this.holdStill();
            this.setYaw((float) (Math.toDegrees(Math.atan2(this.direction.z, this.direction.x)) - 90.0));
            if (this.timer >= this.windup) {
                this.dashing = true;
                this.timer = 0;
                this.naga.setCharging(true);
                this.naga.playSound(TFSounds.NAGA_HISS.get(), 2.5F, 0.8F);
            }
            return true;
        }
        Vec3 motion = this.naga.getDeltaMovement();
        this.naga.setDeltaMovement(this.direction.x * SPEED, motion.y, this.direction.z * SPEED);
        this.setYaw((float) (Math.toDegrees(Math.atan2(this.direction.z, this.direction.x)) - 90.0));
        this.travelled += SPEED;
        for (LivingEntity victim : this.brain.victims(this.naga.getBoundingBox().inflate(0.9, 0.3, 0.9))) {
            if (!this.hit.add(victim.getId())) {
                continue;
            }
            if (this.shieldBlocked(victim)) {
                return this.blocked(victim);
            }
            this.brain.damage(victim, this.multiplier);
            this.brain.knock(victim, this.direction.x, this.direction.z, 0.7, 0.3);
        }
        boolean wall = this.timer > 3 && this.naga.horizontalCollision;
        if (this.travelled >= this.length || wall || this.timer > 40) {
            this.naga.setCharging(false);
            if (wall) {
                this.brain.shake(this.naga.position(), 0.5F, 16.0F, 8);
            }
            if (this.remaining > 0) {
                return this.aim();
            }
            return false;
        }
        return true;
    }

    private boolean shieldBlocked(LivingEntity victim) {
        if (!victim.isBlocking()) {
            return false;
        }
        Vec3 look = victim.getViewVector(1.0F);
        Vec3 toNaga = new Vec3(this.naga.getX() - victim.getX(), 0.0, this.naga.getZ() - victim.getZ()).normalize();
        return look.x * toNaga.x + look.z * toNaga.z > 0.0;
    }

    private boolean blocked(LivingEntity victim) {
        if (victim instanceof ServerPlayer player) {
            player.getUseItem().hurtAndBreak(5, player, player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        if (victim instanceof Player) {
            this.brain.knock(victim, this.direction.x, this.direction.z, 0.6, 0.25);
        }
        this.naga.level().playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F + this.naga.getRandom().nextFloat() * 0.4F);
        Vec3 motion = this.naga.getDeltaMovement();
        this.naga.setDeltaMovement(-this.direction.x * 0.6, motion.y + 0.3, -this.direction.z * 0.6);
        this.naga.setCharging(false);
        this.brain.shake(victim.position(), 0.6F, 12.0F, 8);
        this.brain.requestDaze();
        return false;
    }

    @Override
    public void end() {
        this.naga.setCharging(false);
    }
}
