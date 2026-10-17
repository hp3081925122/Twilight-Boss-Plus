package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.render.YetiSnowballRenderer;
import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.network.YetiFxPacket;
import com.twilightbossplus.yeti.AlphaYetiBossController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.projectile.FallingIce;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YetiFxManager {
    public static final ResourceLocation SNOW = tex("snow");
    public static final ResourceLocation MIST = tex("mist");
    private static final ResourceLocation RIME = tex("rime");
    private static final ResourceLocation CRACK = tex("crack");
    private static final ResourceLocation CRACK_THIN = tex("crack_thin");
    private static final ResourceLocation PLUME = tex("plume");
    private static final ResourceLocation SHADOW = tex("shadow");
    private static final ResourceLocation ICICLE = tex("icicle");
    private static final ResourceLocation SLUSH = tex("slush");
    private static final ResourceLocation WAVE = tex("wave");
    private static final ResourceLocation ROAR = tex("roar");
    private static final ResourceLocation VEIL = tex("veil");
    private static final ResourceLocation FLECK = tex("fleck");

    private static final float SNOW_R = 0.957F;
    private static final float SNOW_G = 0.973F;
    private static final float SNOW_B = 0.984F;
    private static final float MIST_R = 0.788F;
    private static final float MIST_G = 0.839F;
    private static final float MIST_B = 0.875F;
    private static final float FROST_R = 0.659F;
    private static final float FROST_G = 0.847F;
    private static final float FROST_B = 0.910F;
    private static final float RIFT_R = 0.310F;
    private static final float RIFT_G = 0.639F;
    private static final float RIFT_B = 0.820F;
    private static final float DEEP_R = 0.122F;
    private static final float DEEP_G = 0.306F;
    private static final float DEEP_B = 0.451F;

    private static final double MOUTH_HEIGHT = 4.2D;
    private static final double MOUTH_FORWARD = 1.6D;
    private static final int BREATH_FADE = 40;
    private static final int RIFT_LINGER = 40;
    private static final int RIFT_FADE = 20;
    private static final int PLUME_LIFE = 12;
    private static final int ICICLE_FALL = 6;
    private static final double ICICLE_LENGTH = 2.2D;
    private static final int BURST_LIFE = 10;
    private static final int ROAR_LIFE = 20;
    private static final double ROAR_RADIUS = 18.0D;
    private static final int MAX_PUFFS = 400;

    private static final Map<Integer, BossTrack> TRACKS = new HashMap<>();
    private static final Set<Integer> FALLING_ICE = new HashSet<>();
    private static final Map<Integer, Double> ICE_GROUND = new HashMap<>();
    private static final List<BreathFx> BREATHS = new ArrayList<>();
    private static final List<RiftFx> RIFTS = new ArrayList<>();
    private static final List<PlumeFx> PLUMES = new ArrayList<>();
    private static final List<IcicleFx> ICICLES = new ArrayList<>();
    private static final List<BurstFx> BURSTS = new ArrayList<>();
    private static final List<DecalFx> SLUSHES = new ArrayList<>();
    private static final List<WaveFx> WAVES = new ArrayList<>();
    private static final List<RingFx> RINGS = new ArrayList<>();
    private static final List<Puff> PUFFS = new ArrayList<>();

    private YetiFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/yeti/px/" + name + ".png");
    }

    private static BossTrack track(int id) {
        return TRACKS.computeIfAbsent(id, key -> new BossTrack());
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        if (type == YetiFxPacket.CAST && data.length >= 2) {
            BossTrack track = track(entityId);
            Entity entity = level.getEntity(entityId);
            track.pose = (int) data[0];
            track.poseDuration = Math.max(1, (int) data[1]);
            track.poseStart = 0;
            if (entity != null) {
                track.poseStart = entity.tickCount;
            }
            if (track.pose == AlphaYetiBossController.POSE_ROAR && entity != null) {
                RINGS.add(new RingFx(entity.getX(), entity.getY() + 2.5D, entity.getZ(), 0));
                RINGS.add(new RingFx(entity.getX(), entity.getY() + 2.5D, entity.getZ(), -8));
                FxShake.at(entity.getX(), entity.getY(), entity.getZ(), 0.7F, 22.0F, 14);
            }
        } else if (type == YetiFxPacket.BREATH && data.length >= 7) {
            BREATHS.add(new BreathFx(entityId, data[0], data[1], data[2], data[3], (int) data[4], (int) data[5], data[6]));
        } else if (type == YetiFxPacket.RIFT && data.length >= 6) {
            float[] yaws = new float[data.length - 6];
            System.arraycopy(data, 6, yaws, 0, yaws.length);
            RIFTS.add(new RiftFx(data[0], data[1], data[2], data[3], (int) data[4], (int) data[5], yaws));
        } else if (type == YetiFxPacket.ICICLE && data.length >= 6) {
            ICICLES.add(new IcicleFx(data[0], data[1], data[2], data[3], (int) data[4], data[5]));
        } else if (type == YetiFxPacket.SLUSH && data.length >= 5) {
            SLUSHES.add(new DecalFx(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4])));
        } else if (type == YetiFxPacket.WAVE && data.length >= 5) {
            WAVES.add(new WaveFx(data[0], data[1], data[2], data[3], data[4]));
            FxShake.at(data[0], data[1], data[2], 0.5F, 16.0F, 10);
        } else if (type == YetiFxPacket.BOULDER_BREAK && data.length >= 4) {
            BURSTS.add(new BurstFx(data[0], data[1], data[2], data[3] * 1.4F));
            spawnFlecks(level.getRandom(), data[0], data[1] + data[3] * 0.5D, data[2], 30, 0.35D);
            FxShake.at(data[0], data[1], data[2], 0.6F, 14.0F, 10);
        }
    }

    private static void spawnFlecks(RandomSource random, double x, double y, double z, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.4D + random.nextDouble() * 0.6D);
            FxParticles.soft(FLECK, x, y, z, Math.cos(angle) * s, 0.15D + random.nextDouble() * 0.3D, Math.sin(angle) * s, 20 + random.nextInt(8),
                    0.2F + random.nextFloat() * 0.2F)
                .color(SNOW_R, SNOW_G, SNOW_B).fadeTo(MIST_R, MIST_G, MIST_B).gravity(0.05D).drag(0.92D)
                .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.6F).fade(0.0F, 4.0F);
        }
    }

    public static void applyPose(AlphaYeti entity, float ageInTicks, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        BossTrack track = TRACKS.get(entity.getId());
        if (track == null || track.pose == AlphaYetiBossController.POSE_NONE || entity.isTired() || entity.isRampaging()) {
            return;
        }
        float p = Mth.clamp((ageInTicks - track.poseStart) / track.poseDuration, 0.0F, 1.0F);
        float pi = (float) Math.PI;
        if (track.pose == AlphaYetiBossController.POSE_INHALE) {
            head.xRot = -0.6F * p;
            body.xRot = -0.15F * p;
            rightArm.zRot = 0.5F * p;
            leftArm.zRot = -0.5F * p;
        } else if (track.pose == AlphaYetiBossController.POSE_BLOW) {
            head.xRot = 0.35F;
            head.yRot += Mth.sin(ageInTicks * 1.5F) * 0.05F;
            body.xRot = 0.3F;
            rightArm.xRot = -0.6F;
            leftArm.xRot = -0.6F;
            rightArm.zRot = 0.6F;
            leftArm.zRot = -0.6F;
        } else if (track.pose == AlphaYetiBossController.POSE_RIFT) {
            float raise = Mth.lerp(p, -pi * 0.5F, -pi * 0.95F);
            rightArm.xRot = raise;
            leftArm.xRot = raise;
            rightArm.zRot = -0.2F * p;
            leftArm.zRot = 0.2F * p;
            body.xRot = -0.2F * p;
        } else if (track.pose == AlphaYetiBossController.POSE_SLAM) {
            float slam = Mth.clamp(p / 0.35F, 0.0F, 1.0F);
            float arm = Mth.lerp(slam, -pi * 0.95F, -0.4F);
            rightArm.xRot = arm;
            leftArm.xRot = arm;
            body.xRot = Mth.lerp(slam, -0.2F, 0.45F) * (1.0F - Math.max(0.0F, p - 0.6F) * 2.5F);
        } else if (track.pose == AlphaYetiBossController.POSE_BOULDER) {
            float lift = Mth.clamp((p - 0.6F) / 0.4F, 0.0F, 1.0F);
            float arm = Mth.lerp(lift, -1.1F - 0.4F * Math.min(1.0F, p / 0.6F), -2.8F);
            rightArm.xRot = arm;
            leftArm.xRot = arm;
            rightArm.yRot = -0.45F;
            leftArm.yRot = 0.45F;
            body.xRot = Mth.lerp(lift, 0.15F, -0.25F);
        } else if (track.pose == AlphaYetiBossController.POSE_THROW) {
            float swing = Mth.clamp(p / 0.4F, 0.0F, 1.0F);
            float arm = Mth.lerp(swing, -2.8F, -0.6F);
            rightArm.xRot = arm;
            leftArm.xRot = arm;
            body.xRot = Mth.lerp(swing, -0.25F, 0.4F);
        } else if (track.pose == AlphaYetiBossController.POSE_ROAR) {
            float up = Math.min(1.0F, p * 3.0F);
            rightArm.xRot = -2.6F * up;
            leftArm.xRot = -2.6F * up;
            rightArm.zRot = 0.7F * up;
            leftArm.zRot = -0.7F * up;
            head.xRot = -0.7F * up;
            head.yRot += Mth.sin(ageInTicks * 2.0F) * 0.08F * up;
            body.xRot = -0.25F * up;
        }
    }

    private static double scanDown(Level level, double x, double y, double z, double depth) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startY = Mth.floor(y);
        int endY = Mth.floor(y - depth);
        for (int checkY = startY; checkY >= endY; checkY--) {
            pos.set(Mth.floor(x), checkY, Mth.floor(z));
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return checkY + shape.max(Direction.Axis.Y);
            }
        }
        return Double.NaN;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof AlphaYeti) {
            track(event.getEntity().getId());
        } else if (event.getEntity() instanceof FallingIce) {
            FALLING_ICE.add(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        TRACKS.remove(event.getEntity().getId());
        FALLING_ICE.remove(event.getEntity().getId());
        ICE_GROUND.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        TRACKS.clear();
        FALLING_ICE.clear();
        ICE_GROUND.clear();
        BREATHS.clear();
        RIFTS.clear();
        PLUMES.clear();
        ICICLES.clear();
        BURSTS.clear();
        SLUSHES.clear();
        WAVES.clear();
        RINGS.clear();
        PUFFS.clear();
    }

    private static Vec3 forward(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0D, Mth.cos(rad));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            if (!TRACKS.isEmpty() || !BREATHS.isEmpty() || !ICICLES.isEmpty()) {
                clearAll();
            }
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        RandomSource random = level.getRandom();
        for (Map.Entry<Integer, BossTrack> entry : TRACKS.entrySet()) {
            BossTrack track = entry.getValue();
            track.age++;
            if (!(level.getEntity(entry.getKey()) instanceof AlphaYeti yeti)) {
                continue;
            }
            if (yeti.isRampaging() || yeti.isTired()) {
                track.pose = AlphaYetiBossController.POSE_NONE;
            }
            if (decorative && yeti.isTired() && track.age % 8 == 0) {
                Vec3 look = forward(yeti.yBodyRot);
                FxParticles.soft(MIST, yeti.getX() + look.x * 1.2D, yeti.getY() + 3.4D, yeti.getZ() + look.z * 1.2D, look.x * 0.03D, 0.05D, look.z * 0.03D, 24, 0.5F)
                    .color(SNOW_R, SNOW_G, SNOW_B).fadeTo(MIST_R, MIST_G, MIST_B).alpha(0.6F).grow(1.5F).drag(0.95D).spin(random.nextFloat() * 6.0F, 0.04F);
            }
            if (decorative && track.pose == AlphaYetiBossController.POSE_BOULDER && track.age % 2 == 0) {
                Vec3 look = forward(yeti.yBodyRot);
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = 2.0D + random.nextDouble() * 2.5D;
                double tx = yeti.getX() + look.x * 2.0D;
                double tz = yeti.getZ() + look.z * 2.0D;
                double sx = tx + Math.cos(angle) * dist;
                double sz = tz + Math.sin(angle) * dist;
                FxParticles.soft(FLECK, sx, yeti.getY() + 0.2D, sz, (tx - sx) * 0.08D, 0.2D, (tz - sz) * 0.08D, 12, 0.3F)
                    .color(SNOW_R, SNOW_G, SNOW_B).gravity(0.015D).drag(0.95D).spin(random.nextFloat() * 6.0F, 0.3F);
            }
            if (decorative && yeti.isRampaging() && track.age % 2 == 0) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                FxParticles.soft(FLECK, yeti.getX() + Math.cos(angle) * 3.4D, yeti.getY() + random.nextDouble() * 5.0D, yeti.getZ() + Math.sin(angle) * 3.4D,
                        -Math.sin(angle) * 0.3D, 0.02D, Math.cos(angle) * 0.3D, 14, 0.25F)
                    .color(SNOW_R, SNOW_G, SNOW_B).drag(0.9D).spin(random.nextFloat() * 6.0F, 0.4F);
            }
        }
        Iterator<BreathFx> breaths = BREATHS.iterator();
        while (breaths.hasNext()) {
            BreathFx breath = breaths.next();
            breath.age++;
            if (breath.age > breath.inhale + breath.blow + BREATH_FADE) {
                breaths.remove();
                continue;
            }
            if (breath.age <= breath.inhale || breath.age > breath.inhale + breath.blow) {
                continue;
            }
            if (!(level.getEntity(breath.bossId) instanceof AlphaYeti yeti)) {
                continue;
            }
            float yaw = breath.yawAt(breath.age);
            Vec3 look = forward(yaw);
            Vec3 mouth = yeti.position().add(look.x * MOUTH_FORWARD, MOUTH_HEIGHT, look.z * MOUTH_FORWARD);
            for (int k = 0; k < 3 && PUFFS.size() < MAX_PUFFS; k++) {
                float spread = (random.nextFloat() * 2.0F - 1.0F) * breath.half * 0.85F;
                Vec3 dir = forward(yaw + spread);
                double pitch = -0.45D + random.nextDouble() * 0.35D;
                double speed = breath.range / 16.0D;
                Vec3 vel = new Vec3(dir.x * speed, pitch * speed, dir.z * speed);
                PUFFS.add(new Puff(mouth, vel, 16 + random.nextInt(4), 0.6F, 2.6F, 0.85F));
            }
        }
        Iterator<RiftFx> rifts = RIFTS.iterator();
        while (rifts.hasNext()) {
            RiftFx rift = rifts.next();
            rift.age++;
            int step = rift.age - rift.windup;
            if (step == 1) {
                FxShake.at(rift.x, rift.y, rift.z, 0.55F, 18.0F, 10);
            }
            if (step >= 1 && step <= rift.spread) {
                double reach = rift.length * step / rift.spread;
                for (float yaw : rift.yaws) {
                    Vec3 dir = forward(yaw);
                    double x = rift.x + dir.x * reach;
                    double z = rift.z + dir.z * reach;
                    PLUMES.add(new PlumeFx(x, rift.y, z));
                    if (decorative && step % 2 == 0) {
                        spawnFlecks(random, x, rift.y + 0.3D, z, 3, 0.12D);
                    }
                }
            }
            if (rift.age > rift.windup + rift.spread + RIFT_LINGER + RIFT_FADE) {
                rifts.remove();
            }
        }
        PLUMES.removeIf(plume -> ++plume.age > PLUME_LIFE);
        Iterator<IcicleFx> icicles = ICICLES.iterator();
        while (icicles.hasNext()) {
            IcicleFx icicle = icicles.next();
            icicle.age++;
            if (icicle.age >= icicle.warning + ICICLE_FALL) {
                icicles.remove();
                BURSTS.add(new BurstFx(icicle.x, icicle.y, icicle.z, icicle.radius * 1.6F));
                spawnFlecks(random, icicle.x, icicle.y + 0.2D, icicle.z, 14, 0.25D);
                FxShake.at(icicle.x, icicle.y, icicle.z, 0.35F, 8.0F, 6);
            }
        }
        BURSTS.removeIf(burst -> ++burst.age > BURST_LIFE);
        SLUSHES.removeIf(slush -> ++slush.age >= slush.ticks);
        WAVES.removeIf(wave -> ++wave.age * wave.speed > wave.max);
        if (decorative) {
            for (WaveFx wave : WAVES) {
                float radius = wave.age * wave.speed;
                for (int k = 0; k < 6; k++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    FxParticles.soft(FLECK, wave.x + Math.cos(angle) * radius, wave.y + 1.1D, wave.z + Math.sin(angle) * radius,
                            Math.cos(angle) * wave.speed * 0.6D, 0.15D + random.nextDouble() * 0.12D, Math.sin(angle) * wave.speed * 0.6D, 14, 0.28F)
                        .color(SNOW_R, SNOW_G, SNOW_B).gravity(0.04D).drag(0.92D).spin(random.nextFloat() * 6.0F, 0.3F);
                }
            }
        }
        RINGS.removeIf(ring -> ++ring.age > ROAR_LIFE);
        Iterator<Puff> puffs = PUFFS.iterator();
        while (puffs.hasNext()) {
            Puff puff = puffs.next();
            puff.prev = puff.pos;
            puff.pos = puff.pos.add(puff.vel);
            puff.vel = puff.vel.scale(0.94D);
            if (++puff.age > puff.life) {
                puffs.remove();
            }
        }
        Iterator<Integer> ice = FALLING_ICE.iterator();
        while (ice.hasNext()) {
            int id = ice.next();
            Entity entity = level.getEntity(id);
            if (entity == null) {
                ice.remove();
                ICE_GROUND.remove(id);
                continue;
            }
            if (!ICE_GROUND.containsKey(id) || entity.tickCount % 10 == 0) {
                ICE_GROUND.put(id, scanDown(level, entity.getX(), entity.getY() - 0.5D, entity.getZ(), 32.0D));
            }
        }
    }

    private static boolean hasWork() {
        return !TRACKS.isEmpty() || !BREATHS.isEmpty() || !RIFTS.isEmpty() || !PLUMES.isEmpty() || !ICICLES.isEmpty() || !BURSTS.isEmpty()
            || !SLUSHES.isEmpty() || !WAVES.isEmpty() || !RINGS.isEmpty() || !PUFFS.isEmpty();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !AlphaYetiConfig.ENABLED.get() || !hasWork()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        float partialTick = event.getPartialTick();
        Camera camera = event.getCamera();
        FxDraw.camera(camera);
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        PoseStack.Pose pose = poseStack.last();

        for (BreathFx breath : BREATHS) {
            renderRime(pose, buffers, level, breath, partialTick);
        }
        for (RiftFx rift : RIFTS) {
            renderRift(pose, buffers, rift, partialTick);
        }
        if (!PLUMES.isEmpty()) {
            VertexConsumer base = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            for (PlumeFx plume : PLUMES) {
                float t = Mth.clamp((plume.age + partialTick) / PLUME_LIFE, 0.0F, 1.0F);
                FxDraw.flat(base, pose, plume.x, plume.y + 0.08D, plume.z, 1.4F, 0.0F, RIFT_R, RIFT_G, RIFT_B, 0.6F * (1.0F - t));
            }
            VertexConsumer plumes = buffers.getBuffer(FxRenderTypes.soft(PLUME));
            for (PlumeFx plume : PLUMES) {
                float t = Mth.clamp((plume.age + partialTick) / PLUME_LIFE, 0.0F, 1.0F);
                float height = 3.4F * Math.min(1.0F, t * 3.0F);
                float width = 0.7F + 0.6F * t;
                crossQuad(pose, plumes, plume.x, plume.y, plume.z, width, height, SNOW_R, SNOW_G, SNOW_B, 0.95F * (1.0F - t * t));
            }
        }
        for (IcicleFx icicle : ICICLES) {
            renderIcicle(poseStack, pose, buffers, icicle, partialTick, level);
        }
        if (!BURSTS.isEmpty()) {
            VertexConsumer bursts = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
            for (BurstFx burst : BURSTS) {
                float t = Mth.clamp((burst.age + partialTick) / BURST_LIFE, 0.0F, 1.0F);
                FxDraw.flat(bursts, pose, burst.x, burst.y + 0.07D, burst.z, burst.radius * (float) Math.sqrt(t), 0.0F, FROST_R, FROST_G, FROST_B, 0.7F * (1.0F - t));
            }
        }
        if (!SLUSHES.isEmpty()) {
            VertexConsumer slush = buffers.getBuffer(FxRenderTypes.soft(SLUSH));
            for (DecalFx decal : SLUSHES) {
                float age = decal.age + partialTick;
                float grow = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
                float fade = Mth.clamp((decal.ticks - age) / 25.0F, 0.0F, 1.0F);
                FxDraw.flat(slush, pose, decal.x, decal.y + 0.05D, decal.z, decal.radius * grow, decal.x * 0.37F, 1.0F, 1.0F, 1.0F, 0.9F * fade);
            }
        }
        if (!WAVES.isEmpty()) {
            VertexConsumer waves = buffers.getBuffer(FxRenderTypes.soft(WAVE));
            for (WaveFx wave : WAVES) {
                float radius = (wave.age + partialTick) * wave.speed;
                float fade = Mth.clamp((wave.max - radius) / 2.0F, 0.0F, 1.0F);
                ring(waves, pose, wave.x, wave.y, wave.y + 1.3F, wave.z, radius, 2.0F, 0.0F, SNOW_R, SNOW_G, SNOW_B, 0.95F * fade);
            }
            VertexConsumer front = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            for (WaveFx wave : WAVES) {
                float radius = (wave.age + partialTick) * wave.speed;
                float fade = Mth.clamp((wave.max - radius) / 2.0F, 0.0F, 1.0F);
                FxDraw.ring(front, pose, wave.x, wave.y + 0.06D, wave.z, Math.max(0.0F, radius - 0.5F), radius + 0.3F, 48, 0.0F, 1.0F,
                    FROST_R, FROST_G, FROST_B, 0.6F * fade, 0.6F * fade);
            }
        }
        if (!RINGS.isEmpty()) {
            VertexConsumer roar = buffers.getBuffer(FxRenderTypes.glow(ROAR));
            for (RingFx ring : RINGS) {
                if (ring.age < 0) {
                    continue;
                }
                float t = Mth.clamp((ring.age + partialTick) / ROAR_LIFE, 0.0F, 1.0F);
                float radius = (float) (ROAR_RADIUS * (1.0F - (1.0F - t) * (1.0F - t)));
                float half = 1.5F * (1.0F - t * 0.5F);
                ring(roar, pose, ring.x, ring.y - half, ring.y + half, ring.z, radius, 2.0F, t * 2.0F, SNOW_R, SNOW_G, SNOW_B, 0.75F * (1.0F - t));
            }
            VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
            for (RingFx ring : RINGS) {
                if (ring.age < 0) {
                    continue;
                }
                float t = Mth.clamp((ring.age + partialTick) / ROAR_LIFE, 0.0F, 1.0F);
                float radius = (float) (ROAR_RADIUS * (1.0F - (1.0F - t) * (1.0F - t)));
                FxDraw.flat(shock, pose, ring.x, ring.y - 2.45D, ring.z, radius, 0.0F, MIST_R, MIST_G, MIST_B, 0.5F * (1.0F - t));
            }
        }
        for (Map.Entry<Integer, BossTrack> entry : TRACKS.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof AlphaYeti yeti) || !yeti.isAlive()) {
                continue;
            }
            BossTrack track = entry.getValue();
            if (track.pose == AlphaYetiBossController.POSE_BOULDER) {
                renderHeldBoulder(poseStack, buffers, yeti, track, partialTick, level);
            }
            if (decorative && yeti.isRampaging()) {
                Vec3 pos = yeti.getPosition(partialTick);
                float time = track.age + partialTick;
                VertexConsumer veil = buffers.getBuffer(FxRenderTypes.soft(VEIL));
                ring(veil, pose, (float) pos.x, (float) pos.y, (float) pos.y + 5.5F, (float) pos.z, 3.2F, 3.0F, time * 0.05F, SNOW_R, SNOW_G, SNOW_B, 0.7F);
                ring(veil, pose, (float) pos.x, (float) pos.y + 0.5F, (float) pos.y + 6.0F, (float) pos.z, 3.7F, 3.0F, -time * 0.035F, MIST_R, MIST_G, MIST_B, 0.5F);
            }
        }
        if (!FALLING_ICE.isEmpty() && !TRACKS.isEmpty()) {
            VertexConsumer shadows = buffers.getBuffer(FxRenderTypes.soft(SHADOW));
            for (int id : FALLING_ICE) {
                Entity entity = level.getEntity(id);
                Double ground = ICE_GROUND.get(id);
                if (entity == null || ground == null || ground.isNaN()) {
                    continue;
                }
                Vec3 pos = entity.getPosition(partialTick);
                float pulse = 0.55F + 0.2F * Mth.sin((entity.tickCount + partialTick) * 0.4F);
                FxDraw.flat(shadows, pose, pos.x, ground + 0.06D, pos.z, 1.3F, 0.0F, DEEP_R, DEEP_G, DEEP_B, pulse);
            }
        }
        if (!PUFFS.isEmpty()) {
            VertexConsumer puffs = buffers.getBuffer(FxRenderTypes.soft(MIST));
            for (Puff puff : PUFFS) {
                float t = Mth.clamp((puff.age + partialTick) / puff.life, 0.0F, 1.0F);
                Vec3 pos = puff.prev.lerp(puff.pos, partialTick);
                float size = Mth.lerp(t, puff.startSize, puff.endSize);
                float roll = ((puff.age + partialTick) * 2.0F + puff.spin) * Mth.DEG_TO_RAD;
                FxDraw.sprite(puffs, pose, pos.x, pos.y, pos.z, size, roll, Mth.lerp(t, SNOW_R, FROST_R), Mth.lerp(t, SNOW_G, FROST_G), Mth.lerp(t, SNOW_B, FROST_B),
                    puff.alpha * (1.0F - t * t));
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderRime(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, BreathFx breath, float partialTick) {
        float age = breath.age + partialTick;
        float radius = breath.range;
        float alpha = 0.75F;
        if (age < breath.inhale) {
            radius = breath.range * Mth.clamp(age / breath.inhale, 0.05F, 1.0F);
            alpha = 0.55F;
        } else if (age > breath.inhale + breath.blow) {
            alpha = 0.75F * Mth.clamp(1.0F - (age - breath.inhale - breath.blow) / BREATH_FADE, 0.0F, 1.0F);
        }
        if (alpha <= 0.01F) {
            return;
        }
        float yaw = breath.yawAt(age);
        if (level.getEntity(breath.bossId) instanceof AlphaYeti yeti) {
            breath.originX = yeti.getX();
            breath.originZ = yeti.getZ();
        }
        double cx = breath.originX;
        double cz = breath.originZ;
        double y = breath.groundY + 0.04D;
        VertexConsumer frost = buffers.getBuffer(FxRenderTypes.soft(RIME));
        int segments = 14;
        for (int s = 0; s < segments; s++) {
            Vec3 d0 = forward(yaw - breath.half + 2.0F * breath.half * s / segments);
            Vec3 d1 = forward(yaw - breath.half + 2.0F * breath.half * (s + 1) / segments);
            double x0 = cx + d0.x * radius;
            double z0 = cz + d0.z * radius;
            double x1 = cx + d1.x * radius;
            double z1 = cz + d1.z * radius;
            FxDraw.glowVertex(frost, pose, cx, y, cz, (float) cx / 3.0F, (float) cz / 3.0F, FROST_R, FROST_G, FROST_B, alpha);
            FxDraw.glowVertex(frost, pose, x0, y, z0, (float) x0 / 3.0F, (float) z0 / 3.0F, SNOW_R, SNOW_G, SNOW_B, alpha);
            FxDraw.glowVertex(frost, pose, x1, y, z1, (float) x1 / 3.0F, (float) z1 / 3.0F, SNOW_R, SNOW_G, SNOW_B, alpha);
            FxDraw.glowVertex(frost, pose, cx, y, cz, (float) cx / 3.0F, (float) cz / 3.0F, FROST_R, FROST_G, FROST_B, alpha);
        }
        VertexConsumer edge = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        float start = (yaw - breath.half) * Mth.DEG_TO_RAD + Mth.HALF_PI;
        float end = (yaw + breath.half) * Mth.DEG_TO_RAD + Mth.HALF_PI;
        FxDraw.arc(edge, pose, cx, y + 0.01D, cz, radius - 0.25F, radius + 0.25F, start, end, 20, 1.0F, FROST_R, FROST_G, FROST_B, alpha, alpha);
        groundStrip(edge, pose, (float) cx, (float) cz, forward(yaw - breath.half), 0.5F, radius, (float) y + 0.01F, 0.4F, 1.0F, FROST_R, FROST_G, FROST_B, alpha * 0.8F);
        groundStrip(edge, pose, (float) cx, (float) cz, forward(yaw + breath.half), 0.5F, radius, (float) y + 0.01F, 0.4F, 1.0F, FROST_R, FROST_G, FROST_B, alpha * 0.8F);
    }

    private static void renderRift(PoseStack.Pose pose, MultiBufferSource buffers, RiftFx rift, float partialTick) {
        float age = rift.age + partialTick;
        float reach = 0.0F;
        if (age > rift.windup) {
            reach = rift.length * Mth.clamp((age - rift.windup) / rift.spread, 0.0F, 1.0F);
        }
        float fade = 1.0F;
        float end = rift.windup + rift.spread + RIFT_LINGER;
        if (age > end) {
            fade = Mth.clamp(1.0F - (age - end) / RIFT_FADE, 0.0F, 1.0F);
        }
        float preview = 0.35F + 0.5F * Mth.clamp(age / rift.windup, 0.0F, 1.0F);
        float y = rift.y + 0.05F;
        if (reach < rift.length) {
            VertexConsumer thin = buffers.getBuffer(FxRenderTypes.soft(CRACK_THIN));
            for (float yaw : rift.yaws) {
                groundStrip(thin, pose, rift.x, rift.z, forward(yaw), reach, rift.length, y, 0.6F, 1.0F / 1.2F, SNOW_R, SNOW_G, SNOW_B, preview * fade);
            }
        }
        if (reach > 0.0F) {
            float glowPulse = 0.45F + 0.15F * Mth.sin(age * 0.5F);
            VertexConsumer under = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            for (float yaw : rift.yaws) {
                groundStrip(under, pose, rift.x, rift.z, forward(yaw), 0.6F, reach, y + 0.005F, 2.6F, 1.0F, RIFT_R, RIFT_G, RIFT_B, glowPulse * fade);
            }
            VertexConsumer crack = buffers.getBuffer(FxRenderTypes.soft(CRACK));
            for (float yaw : rift.yaws) {
                groundStrip(crack, pose, rift.x, rift.z, forward(yaw), 0.6F, reach, y + 0.01F, 1.3F, 1.0F / 2.6F, 1.0F, 1.0F, 1.0F, fade);
            }
        }
    }

    private static void groundStrip(VertexConsumer consumer, PoseStack.Pose pose, float cx, float cz, Vec3 dir, float from, float to, float y, float width, float uScale,
                                    float r, float g, float b, float alpha) {
        if (to - from < 0.05F || alpha <= 0.01F) {
            return;
        }
        double sx = -dir.z * width * 0.5D;
        double sz = dir.x * width * 0.5D;
        double x0 = cx + dir.x * from;
        double z0 = cz + dir.z * from;
        double x1 = cx + dir.x * to;
        double z1 = cz + dir.z * to;
        float u0 = from * uScale;
        float u1 = to * uScale;
        FxDraw.glowVertex(consumer, pose, x0 + sx, y, z0 + sz, u0, 0.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, x0 - sx, y, z0 - sz, u0, 1.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, x1 - sx, y, z1 - sz, u1, 1.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, x1 + sx, y, z1 + sz, u1, 0.0F, r, g, b, alpha);
    }

    private static void renderIcicle(PoseStack poseStack, PoseStack.Pose world, MultiBufferSource buffers, IcicleFx icicle, float partialTick, ClientLevel level) {
        float age = icicle.age + partialTick;
        float warn = Mth.clamp(age / icicle.warning, 0.0F, 1.0F);
        VertexConsumer shadow = buffers.getBuffer(FxRenderTypes.soft(SHADOW));
        FxDraw.flat(shadow, world, icicle.x, icicle.y + 0.06D, icicle.z, icicle.radius * (1.8F - 0.8F * warn), 0.0F, DEEP_R, DEEP_G, DEEP_B, 0.5F + 0.45F * warn);
        if (age > icicle.warning - 10) {
            float cue = Mth.clamp((age - icicle.warning + 10) / 10.0F, 0.0F, 1.0F);
            VertexConsumer line = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.beam(line, world, new Vec3(icicle.x, icicle.top - ICICLE_LENGTH, icicle.z), new Vec3(icicle.x, icicle.y, icicle.z), 0.05F, 0.12F, 0.0F, 1.0F,
                FROST_R, FROST_G, FROST_B, 0.0F, 0.5F * cue);
        }
        float tip = icicle.top - (float) ICICLE_LENGTH;
        float wobble = 0.0F;
        if (age > icicle.warning - 10) {
            wobble = Mth.sin(age * 2.2F) * 4.0F;
        }
        if (age > icicle.warning) {
            float t = Mth.clamp((age - icicle.warning) / ICICLE_FALL, 0.0F, 1.0F);
            tip = Mth.lerp(t * t, tip, icicle.y);
            wobble = 0.0F;
        }
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(icicle.x, tip + 1.0D, icicle.z));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(ICICLE));
        poseStack.pushPose();
        poseStack.translate(icicle.x, tip, icicle.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(wobble));
        PoseStack.Pose pose = poseStack.last();
        float h = (float) ICICLE_LENGTH;
        float w = 0.45F;
        float[][] corners = {{-w, -w}, {w, -w}, {w, w}, {-w, w}};
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i];
            float[] b = corners[(i + 1) % 4];
            float nx = (a[0] + b[0]) * 0.5F;
            float nz = (a[1] + b[1]) * 0.5F;
            FxDraw.vertex(consumer, pose, 0.0F, 0.0F, 0.0F, 0.5F, 1.0F, SNOW_R, SNOW_G, SNOW_B, 1.0F, light, nx, -0.3F, nz);
            FxDraw.vertex(consumer, pose, 0.0F, 0.0F, 0.0F, 0.5F, 1.0F, SNOW_R, SNOW_G, SNOW_B, 1.0F, light, nx, -0.3F, nz);
            FxDraw.vertex(consumer, pose, b[0], h, b[1], 1.0F, 0.0F, SNOW_R, SNOW_G, SNOW_B, 1.0F, light, nx, -0.3F, nz);
            FxDraw.vertex(consumer, pose, a[0], h, a[1], 0.0F, 0.0F, SNOW_R, SNOW_G, SNOW_B, 1.0F, light, nx, -0.3F, nz);
        }
        poseStack.popPose();
    }

    private static void renderHeldBoulder(PoseStack poseStack, MultiBufferSource buffers, AlphaYeti yeti, BossTrack track, float partialTick, ClientLevel level) {
        float age = yeti.tickCount + partialTick;
        float p = Mth.clamp((age - track.poseStart) / track.poseDuration, 0.0F, 1.0F);
        float lift = Mth.clamp((p - 0.6F) / 0.4F, 0.0F, 1.0F);
        Vec3 pos = yeti.getPosition(partialTick);
        Vec3 look = forward(Mth.rotLerp(partialTick, yeti.yBodyRotO, yeti.yBodyRot));
        double forwardDist = Mth.lerp(lift, 2.0D, 0.6D);
        double height = Mth.lerp(lift, 2.4D, 6.4D);
        float size = 0.4F + 1.4F * Math.min(1.0F, p / 0.6F);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(pos.x, pos.y + height, pos.z));
        poseStack.pushPose();
        poseStack.translate(pos.x + look.x * forwardDist, pos.y + height, pos.z + look.z * forwardDist);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 6.0F));
        YetiSnowballRenderer.drawSphere(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(SNOW)), size * 0.5F, light);
        poseStack.popPose();
    }

    private static void ring(VertexConsumer consumer, PoseStack.Pose pose, float cx, float y0, float y1, float cz, float radius, float tile, float scroll,
                             float r, float g, float b, float alpha) {
        if (radius < 0.05F || alpha <= 0.01F) {
            return;
        }
        int segments = Mth.clamp((int) (radius * 6.0F), 12, 64);
        float repeat = Math.max(1, Math.round((float) (Math.PI * 2.0D * radius) / tile));
        for (int s = 0; s < segments; s++) {
            double a0 = Math.PI * 2.0D * s / segments;
            double a1 = Math.PI * 2.0D * (s + 1) / segments;
            double x0 = cx + Math.cos(a0) * radius;
            double z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius;
            double z1 = cz + Math.sin(a1) * radius;
            float u0 = repeat * s / segments + scroll;
            float u1 = repeat * (s + 1) / segments + scroll;
            FxDraw.glowVertex(consumer, pose, x0, y1, z0, u0, 0.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, x0, y0, z0, u0, 1.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, x1, y0, z1, u1, 1.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, x1, y1, z1, u1, 0.0F, r, g, b, alpha);
        }
    }

    private static void crossQuad(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float halfWidth, float height,
                                  float r, float g, float b, float alpha) {
        for (int i = 0; i < 2; i++) {
            float dx = halfWidth;
            float dz = 0.0F;
            if (i == 1) {
                dx = 0.0F;
                dz = halfWidth;
            }
            FxDraw.glowVertex(consumer, pose, x - dx, y + height, z - dz, 0.0F, 0.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, x - dx, y, z - dz, 0.0F, 1.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, x + dx, y, z + dz, 1.0F, 1.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, x + dx, y + height, z + dz, 1.0F, 0.0F, r, g, b, alpha);
        }
    }

    private static final class BossTrack {
        private int age;
        private int pose;
        private int poseStart;
        private int poseDuration = 1;
    }

    private static final class BreathFx {
        private final int bossId;
        private final float startYaw;
        private final float endYaw;
        private final float half;
        private final float range;
        private final int inhale;
        private final int blow;
        private final float groundY;
        private double originX;
        private double originZ;
        private int age;

        private BreathFx(int bossId, float startYaw, float endYaw, float half, float range, int inhale, int blow, float groundY) {
            this.bossId = bossId;
            this.startYaw = startYaw;
            this.endYaw = endYaw;
            this.half = half;
            this.range = range;
            this.inhale = Math.max(1, inhale);
            this.blow = Math.max(1, blow);
            this.groundY = groundY;
        }

        private float yawAt(float age) {
            float t = Mth.clamp((age - this.inhale) / this.blow, 0.0F, 1.0F);
            return Mth.lerp(t, this.startYaw, this.endYaw);
        }
    }

    private static final class RiftFx {
        private final float x;
        private final float y;
        private final float z;
        private final float length;
        private final int windup;
        private final int spread;
        private final float[] yaws;
        private int age;

        private RiftFx(float x, float y, float z, float length, int windup, int spread, float[] yaws) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.length = length;
            this.windup = Math.max(1, windup);
            this.spread = Math.max(1, spread);
            this.yaws = yaws;
        }
    }

    private static final class PlumeFx {
        private final float x;
        private final float y;
        private final float z;
        private int age;

        private PlumeFx(double x, double y, double z) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
        }
    }

    private static final class IcicleFx {
        private final float x;
        private final float y;
        private final float z;
        private final float top;
        private final int warning;
        private final float radius;
        private int age;

        private IcicleFx(float x, float y, float z, float top, int warning, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.top = top;
            this.warning = Math.max(1, warning);
            this.radius = radius;
        }
    }

    private static final class BurstFx {
        private final float x;
        private final float y;
        private final float z;
        private final float radius;
        private int age;

        private BurstFx(float x, float y, float z, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
    }

    private static final class DecalFx {
        private final float x;
        private final float y;
        private final float z;
        private final float radius;
        private final int ticks;
        private int age;

        private DecalFx(float x, float y, float z, float radius, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ticks = ticks;
        }
    }

    private static final class WaveFx {
        private final float x;
        private final float y;
        private final float z;
        private final float max;
        private final float speed;
        private int age;

        private WaveFx(float x, float y, float z, float max, float speed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.max = max;
            this.speed = Math.max(0.01F, speed);
        }
    }

    private static final class RingFx {
        private final float x;
        private final float y;
        private final float z;
        private int age;

        private RingFx(double x, double y, double z, int age) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            this.age = age;
        }
    }

    private static final class Puff {
        private Vec3 pos;
        private Vec3 prev;
        private Vec3 vel;
        private final int life;
        private final float startSize;
        private final float endSize;
        private final float alpha;
        private final float spin;
        private int age;

        private Puff(Vec3 pos, Vec3 vel, int life, float startSize, float endSize, float alpha) {
            this.pos = pos;
            this.prev = pos;
            this.vel = vel;
            this.life = life;
            this.startSize = startSize;
            this.endSize = endSize;
            this.alpha = alpha;
            this.spin = (float) (pos.x * 37.0D % 360.0D);
        }
    }
}
