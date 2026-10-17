package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.network.UrGhastFxPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
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
import org.joml.Quaternionf;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.projectile.UrGhastFireball;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class UrGhastFxManager {
    public static final ResourceLocation TEAR = tex("tear");
    public static final ResourceLocation CORE = tex("core");
    public static final ResourceLocation MARKER = tex("marker");
    private static final ResourceLocation STREAK = tex("streak");
    private static final ResourceLocation CURTAIN = tex("curtain");
    private static final ResourceLocation EYE = tex("eye");
    private static final ResourceLocation IRIS = tex("iris");
    private static final ResourceLocation GAZE = tex("gaze");
    private static final ResourceLocation GAZE_DASH = tex("gaze_dash");
    private static final ResourceLocation RIPPLE = tex("ripple");
    private static final ResourceLocation POOL = tex("pool");
    private static final ResourceLocation HALO = tex("halo");

    public static final float PALE_R = 0.902F;
    public static final float PALE_G = 0.953F;
    public static final float PALE_B = 1.0F;
    public static final float BLUE_R = 0.561F;
    public static final float BLUE_G = 0.769F;
    public static final float BLUE_B = 0.941F;
    public static final float CRIMSON_R = 0.816F;
    public static final float CRIMSON_G = 0.157F;
    public static final float CRIMSON_B = 0.235F;
    private static final float DARK_R = 0.353F;
    private static final float DARK_G = 0.063F;
    private static final float DARK_B = 0.133F;
    private static final float GOLD_R = 1.0F;
    private static final float GOLD_G = 0.827F;
    private static final float GOLD_B = 0.42F;

    private static final double FACE_FORWARD = 6.8D;
    private static final double TEAR_FORWARD = 8.5D;
    private static final double TEAR_DROP = 3.0D;
    private static final double RAIN_SCAN = 48.0D;
    private static final float RAIN_FALL_SPEED = 1.4F;
    private static final float STREAK_LENGTH = 1.8F;
    private static final int GAZE_CLOSE = 8;
    private static final int SPLASH_LIFE = 12;
    private static final int DROPLET_LIFE = 20;
    private static final int RIPPLE_LIFE = 12;
    private static final int TRAIL_LENGTH = 8;
    private static final int MAX_DROPLETS = 240;
    private static final int MAX_RIPPLES = 160;

    private static final Map<Integer, BossTrack> TRACKS = new HashMap<>();
    private static final Set<Integer> FIREBALLS = new HashSet<>();
    private static final Map<Integer, List<Vec3>> FIRE_TRAILS = new HashMap<>();
    private static final List<PoolFx> POOLS = new ArrayList<>();
    private static final List<SplashFx> SPLASHES = new ArrayList<>();
    private static final List<Droplet> DROPLETS = new ArrayList<>();
    private static final List<RippleFx> RIPPLES = new ArrayList<>();

    private UrGhastFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/urghast/" + name + ".png");
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
        if (type == UrGhastFxPacket.GAZE && data.length >= 1) {
            BossTrack track = track(entityId);
            track.gazeDuration = Math.max(1, (int) data[0]);
            track.gazeIds = new int[data.length - 1];
            for (int i = 1; i < data.length; i++) {
                track.gazeIds[i - 1] = (int) data[i];
            }
            track.gazeSight = new boolean[track.gazeIds.length];
            for (int i = 0; i < track.gazeSight.length; i++) {
                track.gazeSight[i] = true;
            }
            track.gazeActive = true;
            track.gazeAge = 0;
            track.endAge = -1;
        } else if (type == UrGhastFxPacket.GAZE_END) {
            BossTrack track = track(entityId);
            track.gazeActive = false;
            if (data.length >= 2) {
                int count = data.length / 2;
                track.endIds = new int[count];
                track.endHits = new boolean[count];
                for (int i = 0; i < count; i++) {
                    track.endIds[i] = (int) data[i * 2];
                    track.endHits[i] = data[i * 2 + 1] > 0.5F;
                }
            } else {
                track.endIds = track.gazeIds;
                track.endHits = new boolean[track.gazeIds.length];
            }
            track.endAge = 0;
        } else if (type == UrGhastFxPacket.RAIN && data.length >= 3) {
            BossTrack track = track(entityId);
            if (track.rainFade <= 0.0F) {
                track.rainX = data[0];
                track.rainZ = data[1];
                track.rainR = data[2];
                track.prevRainX = data[0];
                track.prevRainZ = data[1];
                track.prevRainR = data[2];
            }
            track.goalX = data[0];
            track.goalZ = data[1];
            track.goalR = data[2];
            track.rainOn = true;
        } else if (type == UrGhastFxPacket.RAIN_END) {
            track(entityId).rainOn = false;
        } else if (type == UrGhastFxPacket.RECALL && data.length >= 1) {
            BossTrack track = track(entityId);
            track.recallIds = new int[data.length - 1];
            for (int i = 1; i < data.length; i++) {
                track.recallIds[i - 1] = (int) data[i];
            }
            track.recallOn = true;
            track.recallAge = 0;
        } else if (type == UrGhastFxPacket.RECALL_END) {
            track(entityId).recallOn = false;
        } else if (type == UrGhastFxPacket.STACKS && data.length >= 1) {
            track(entityId).stacks = Math.max(0, (int) data[0]);
        } else if (type == UrGhastFxPacket.TEAR_CHARGE && data.length >= 1) {
            BossTrack track = track(entityId);
            if (data[0] > 0.5F) {
                track.tearTotal = (int) data[0];
                track.tearAge = 0;
                track.charging = true;
            } else {
                track.charging = false;
            }
        } else if (type == UrGhastFxPacket.POOL && data.length >= 5) {
            POOLS.add(new PoolFx(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4])));
        } else if (type == UrGhastFxPacket.SPLASH && data.length >= 6) {
            boolean air = data[4] > 0.5F;
            SPLASHES.add(new SplashFx(data[0], data[1], data[2], data[3], air, (int) data[5]));
            if (ClientConfig.VFX_ENABLED.get()) {
                RandomSource random = level.getRandom();
                int count = 18;
                if (air) {
                    count = 24;
                }
                for (int i = 0; i < count && DROPLETS.size() < MAX_DROPLETS; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double speed = 0.15D + random.nextDouble() * 0.25D;
                    double up = 0.2D + random.nextDouble() * 0.3D;
                    if (air) {
                        up = (random.nextDouble() - 0.5D) * 0.4D;
                    }
                    DROPLETS.add(new Droplet(new Vec3(data[0], data[1] + 0.3D, data[2]), new Vec3(Math.cos(angle) * speed, up, Math.sin(angle) * speed)));
                }
            }
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
        if (event.getEntity() instanceof UrGhast) {
            track(event.getEntity().getId());
        } else if (event.getEntity() instanceof UrGhastFireball) {
            FIREBALLS.add(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        TRACKS.remove(event.getEntity().getId());
        FIREBALLS.remove(event.getEntity().getId());
        FIRE_TRAILS.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        TRACKS.clear();
        FIREBALLS.clear();
        FIRE_TRAILS.clear();
        POOLS.clear();
        SPLASHES.clear();
        DROPLETS.clear();
        RIPPLES.clear();
    }

    private static Vec3 faceOrigin(UrGhast boss, float partialTick, double forward, double drop) {
        Vec3 look = boss.getViewVector(partialTick);
        Vec3 pos = boss.getPosition(partialTick);
        return new Vec3(pos.x + look.x * forward, pos.y + boss.getBbHeight() * 0.5D + look.y * forward - drop, pos.z + look.z * forward);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            if (!TRACKS.isEmpty() || !POOLS.isEmpty() || !FIREBALLS.isEmpty()) {
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
            if (!(level.getEntity(entry.getKey()) instanceof UrGhast boss)) {
                continue;
            }
            if (track.gazeActive) {
                track.gazeAge++;
                Vec3 origin = faceOrigin(boss, 1.0F, FACE_FORWARD, 0.0D);
                for (int i = 0; i < track.gazeIds.length; i++) {
                    if (level.getEntity(track.gazeIds[i]) instanceof LivingEntity target) {
                        HitResult result = level.clip(new ClipContext(origin, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
                        track.gazeSight[i] = result.getType() == HitResult.Type.MISS;
                    }
                }
            }
            if (track.endAge >= 0) {
                track.endAge++;
                if (track.endAge > GAZE_CLOSE + 2) {
                    track.endAge = -1;
                }
            }
            if (track.recallOn) {
                track.recallAge++;
            }
            if (track.charging) {
                track.tearAge++;
            }
            track.prevRainX = track.rainX;
            track.prevRainZ = track.rainZ;
            track.prevRainR = track.rainR;
            track.rainX = Mth.lerp(0.35D, track.rainX, track.goalX);
            track.rainZ = Mth.lerp(0.35D, track.rainZ, track.goalZ);
            track.rainR = Mth.lerp(0.35D, track.rainR, track.goalR);
            if (track.rainOn) {
                track.rainFade = Math.min(1.0F, track.rainFade + 0.1F);
            } else {
                track.rainFade = Math.max(0.0F, track.rainFade - 0.08F);
            }
            if (track.rainFade <= 0.0F) {
                continue;
            }
            if (track.colAngle == null) {
                track.initColumns(random);
            }
            if (track.age % 10 == 0 || Double.isNaN(track.centerGround)) {
                double top = boss.getY();
                double center = scanDown(level, track.rainX, top, track.rainZ, RAIN_SCAN);
                if (Double.isNaN(center)) {
                    center = top - 25.0D;
                }
                track.centerGround = center;
                for (int i = 0; i < track.columns; i++) {
                    double x = track.rainX + Math.cos(track.colAngle[i]) * track.colRadius[i] * track.rainR;
                    double z = track.rainZ + Math.sin(track.colAngle[i]) * track.colRadius[i] * track.rainR;
                    double ground = scanDown(level, x, top, z, RAIN_SCAN);
                    if (Double.isNaN(ground)) {
                        ground = center;
                    }
                    track.colGround[i] = ground;
                }
            }
            if (decorative && track.rainFade > 0.5F) {
                for (int k = 0; k < 4 && RIPPLES.size() < MAX_RIPPLES; k++) {
                    int i = random.nextInt(track.columns);
                    double x = track.rainX + Math.cos(track.colAngle[i]) * track.colRadius[i] * track.rainR;
                    double z = track.rainZ + Math.sin(track.colAngle[i]) * track.colRadius[i] * track.rainR;
                    RIPPLES.add(new RippleFx(x, track.colGround[i], z));
                }
            }
        }
        Iterator<Integer> fireballs = FIREBALLS.iterator();
        while (fireballs.hasNext()) {
            int id = fireballs.next();
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive()) {
                fireballs.remove();
                FIRE_TRAILS.remove(id);
                continue;
            }
            List<Vec3> trail = FIRE_TRAILS.computeIfAbsent(id, key -> new ArrayList<>());
            trail.add(0, entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
            while (trail.size() > TRAIL_LENGTH) {
                trail.remove(trail.size() - 1);
            }
        }
        POOLS.removeIf(pool -> ++pool.age >= pool.ticks);
        SPLASHES.removeIf(splash -> ++splash.age > SPLASH_LIFE);
        RIPPLES.removeIf(ripple -> ++ripple.age > RIPPLE_LIFE);
        Iterator<Droplet> droplets = DROPLETS.iterator();
        while (droplets.hasNext()) {
            Droplet droplet = droplets.next();
            droplet.prev = droplet.pos;
            droplet.pos = droplet.pos.add(droplet.vel);
            droplet.vel = new Vec3(droplet.vel.x * 0.92D, droplet.vel.y - 0.04D, droplet.vel.z * 0.92D);
            if (++droplet.age > DROPLET_LIFE) {
                droplets.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !UrGhastConfig.ENABLED.get()) {
            return;
        }
        if (TRACKS.isEmpty() && POOLS.isEmpty() && SPLASHES.isEmpty() && DROPLETS.isEmpty() && RIPPLES.isEmpty()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        Quaternionf cameraRotation = event.getCamera().rotation();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (Map.Entry<Integer, BossTrack> entry : TRACKS.entrySet()) {
            BossTrack track = entry.getValue();
            if (!(level.getEntity(entry.getKey()) instanceof UrGhast boss)) {
                continue;
            }
            if (track.rainFade > 0.0F && track.colAngle != null) {
                renderRain(poseStack, buffers, boss, track, camera, partialTick);
            }
            if (track.gazeActive || track.endAge >= 0) {
                renderGaze(poseStack, buffers, level, boss, track, camera, cameraRotation, partialTick);
            }
            if (track.recallOn) {
                renderRecall(poseStack, buffers, level, boss, track, cameraRotation, partialTick, decorative);
            }
            if (track.stacks > 0 && boss.isAlive()) {
                renderStacks(poseStack, buffers, boss, track, cameraRotation, partialTick);
            }
            if (track.charging && boss.isAlive()) {
                Vec3 origin = faceOrigin(boss, partialTick, TEAR_FORWARD, TEAR_DROP);
                float progress = Mth.clamp((track.tearAge + partialTick) / Math.max(1, track.tearTotal), 0.0F, 1.0F);
                poseStack.pushPose();
                poseStack.translate(origin.x, origin.y, origin.z);
                drawTear(poseStack, buffers, cameraRotation, 0.3F + 1.3F * progress, track.age + partialTick, 0.5F + 0.45F * progress);
                poseStack.popPose();
            }
        }
        for (PoolFx pool : POOLS) {
            renderPool(poseStack, buffers, pool, partialTick, decorative);
        }
        for (SplashFx splash : SPLASHES) {
            renderSplash(poseStack, buffers, level, splash, camera, partialTick);
        }
        if (decorative) {
            VertexConsumer tears = buffers.getBuffer(RenderType.entityTranslucent(TEAR));
            for (Droplet droplet : DROPLETS) {
                float life = 1.0F - (droplet.age + partialTick) / DROPLET_LIFE;
                if (life <= 0.0F) {
                    continue;
                }
                Vec3 pos = droplet.prev.lerp(droplet.pos, partialTick);
                billboardRect(poseStack, tears, cameraRotation, pos.x, pos.y, pos.z, 0.1F, 0.15F, BLUE_R, BLUE_G, BLUE_B, 0.85F * life);
            }
            VertexConsumer ripples = buffers.getBuffer(RenderType.entityTranslucent(RIPPLE));
            for (RippleFx ripple : RIPPLES) {
                float t = Mth.clamp((ripple.age + partialTick) / RIPPLE_LIFE, 0.0F, 1.0F);
                FxDraw.flatSquare(ripples, poseStack.last(), (float) ripple.x, (float) ripple.y + 0.03F, (float) ripple.z, 0.1F + 0.5F * t, 0.0F, BLUE_R, BLUE_G, BLUE_B, 0.6F * (1.0F - t), FxDraw.FULL_BRIGHT);
            }
            VertexConsumer cores = buffers.getBuffer(RenderType.entityTranslucent(CORE));
            for (List<Vec3> trail : FIRE_TRAILS.values()) {
                for (int i = 1; i < trail.size(); i++) {
                    float fade = 1.0F - (float) i / TRAIL_LENGTH;
                    Vec3 pos = trail.get(i);
                    billboardRect(poseStack, cores, cameraRotation, pos.x, pos.y, pos.z, 0.45F * fade, 0.45F * fade, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.65F * fade);
                }
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderRain(PoseStack poseStack, MultiBufferSource buffers, UrGhast boss, BossTrack track, Vec3 camera, float partialTick) {
        double cx = Mth.lerp(partialTick, track.prevRainX, track.rainX);
        double cz = Mth.lerp(partialTick, track.prevRainZ, track.rainZ);
        double radius = Mth.lerp(partialTick, track.prevRainR, track.rainR);
        double top = Mth.lerp(partialTick, boss.yo, boss.getY()) + 1.0D;
        float fade = track.rainFade;
        float time = track.age + partialTick;
        VertexConsumer streaks = buffers.getBuffer(RenderType.entityTranslucent(STREAK));
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < track.columns; i++) {
            double x = cx + Math.cos(track.colAngle[i]) * track.colRadius[i] * radius;
            double z = cz + Math.sin(track.colAngle[i]) * track.colRadius[i] * radius;
            double span = top - track.colGround[i];
            if (span < 1.0D) {
                continue;
            }
            double travel = (time * RAIN_FALL_SPEED + track.colPhase[i] * span) % span;
            double head = top - travel;
            double tail = Math.min(top, head + STREAK_LENGTH);
            double dx = x - camera.x;
            double dz = z - camera.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.01D) {
                continue;
            }
            float sx = (float) (-dz / len * 0.04D);
            float sz = (float) (dx / len * 0.04D);
            float fx = (float) x;
            float fz = (float) z;
            float alpha = 0.75F * fade;
            FxDraw.vertex(streaks, pose, fx - sx, (float) tail, fz - sz, 0.0F, 0.0F, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(streaks, pose, fx - sx, (float) head, fz - sz, 0.0F, 1.0F, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(streaks, pose, fx + sx, (float) head, fz + sz, 1.0F, 1.0F, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(streaks, pose, fx + sx, (float) tail, fz + sz, 1.0F, 0.0F, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
        double bottom = track.centerGround;
        if (Double.isNaN(bottom)) {
            bottom = top - 25.0D;
        }
        float height = (float) (top - bottom);
        if (height > 0.5F) {
            VertexConsumer curtain = buffers.getBuffer(RenderType.entityTranslucent(CURTAIN));
            int segments = 40;
            float scroll = -time * 0.06F;
            float circumference = (float) (Math.PI * 2.0D * radius);
            for (int s = 0; s < segments; s++) {
                double a0 = Math.PI * 2.0D * s / segments;
                double a1 = Math.PI * 2.0D * (s + 1) / segments;
                float x0 = (float) (cx + Math.cos(a0) * radius);
                float z0 = (float) (cz + Math.sin(a0) * radius);
                float x1 = (float) (cx + Math.cos(a1) * radius);
                float z1 = (float) (cz + Math.sin(a1) * radius);
                float u0 = circumference * s / segments / 2.0F;
                float u1 = circumference * (s + 1) / segments / 2.0F;
                float v0 = scroll;
                float v1 = scroll + height / 4.0F;
                float alpha = 0.3F * fade;
                FxDraw.vertex(curtain, pose, x0, (float) top, z0, u0, v0, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(curtain, pose, x0, (float) bottom, z0, u0, v1, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(curtain, pose, x1, (float) bottom, z1, u1, v1, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(curtain, pose, x1, (float) top, z1, u1, v0, BLUE_R, BLUE_G, BLUE_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            }
        }
        VertexConsumer ring = buffers.getBuffer(RenderType.entityTranslucent(RIPPLE));
        FxDraw.flatSquare(ring, pose, (float) cx, (float) bottom + 0.05F, (float) cz, (float) radius, 0.0F, BLUE_R, BLUE_G, BLUE_B, 0.55F * fade, FxDraw.FULL_BRIGHT);
    }

    private static void renderGaze(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, UrGhast boss, BossTrack track, Vec3 camera,
                                   Quaternionf cameraRotation, float partialTick) {
        Vec3 origin = faceOrigin(boss, partialTick, FACE_FORWARD, 0.0D);
        if (track.gazeActive) {
            float progress = Mth.clamp((track.gazeAge + partialTick) / track.gazeDuration, 0.0F, 1.0F);
            for (int i = 0; i < track.gazeIds.length; i++) {
                if (!(level.getEntity(track.gazeIds[i]) instanceof LivingEntity target)) {
                    continue;
                }
                Vec3 eye = target.getEyePosition(partialTick);
                boolean sight = track.gazeSight[i];
                float alpha = 0.35F;
                if (sight) {
                    alpha = 0.55F + 0.35F * progress;
                }
                renderBeam(poseStack, buffers, origin, eye.add(0.0D, -0.2D, 0.0D), camera, 0.1F + 0.12F * progress, CRIMSON_R, CRIMSON_G, CRIMSON_B, alpha, !sight, (track.age + partialTick) * 0.15F);
                float open = (float) Math.pow(progress, 0.6D);
                renderEye(poseStack, buffers, cameraRotation, eye.x, eye.y + 0.95D, eye.z, open, PALE_R, PALE_G, PALE_B, 0.9F);
            }
            return;
        }
        if (track.endIds == null) {
            return;
        }
        float t = Mth.clamp((track.endAge + partialTick) / GAZE_CLOSE, 0.0F, 1.0F);
        for (int i = 0; i < track.endIds.length; i++) {
            if (!(level.getEntity(track.endIds[i]) instanceof LivingEntity target)) {
                continue;
            }
            Vec3 eye = target.getEyePosition(partialTick);
            float r = PALE_R;
            float g = PALE_G;
            float b = PALE_B;
            if (track.endHits[i]) {
                r = CRIMSON_R;
                g = CRIMSON_G;
                b = CRIMSON_B;
                float flash = 1.0F - Mth.clamp((track.endAge + partialTick) / 4.0F, 0.0F, 1.0F);
                if (flash > 0.0F) {
                    renderBeam(poseStack, buffers, origin, eye.add(0.0D, -0.2D, 0.0D), camera, 0.45F * flash + 0.1F, CRIMSON_R, CRIMSON_G, CRIMSON_B, flash, false, 0.0F);
                }
            }
            renderEye(poseStack, buffers, cameraRotation, eye.x, eye.y + 0.95D, eye.z, 1.0F - t, r, g, b, 0.9F * (1.0F - t * 0.5F));
        }
    }

    private static void renderEye(PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraRotation, double x, double y, double z, float open,
                                  float r, float g, float b, float alpha) {
        if (open <= 0.02F) {
            return;
        }
        VertexConsumer eye = buffers.getBuffer(RenderType.entityTranslucent(EYE));
        billboardRect(poseStack, eye, cameraRotation, x, y, z, 0.55F, 0.28F * open, r, g, b, alpha);
        if (open > 0.3F) {
            VertexConsumer iris = buffers.getBuffer(RenderType.entityTranslucent(IRIS));
            float size = 0.17F * Math.min(1.0F, (open - 0.3F) / 0.5F);
            billboardRect(poseStack, iris, cameraRotation, x, y, z, size, size, GOLD_R, GOLD_G, GOLD_B, alpha);
        }
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffers, Vec3 from, Vec3 to, Vec3 camera, float width,
                                   float r, float g, float b, float alpha, boolean dashed, float scroll) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.1D || alpha <= 0.01F) {
            return;
        }
        Vec3 mid = from.add(to).scale(0.5D);
        Vec3 side = dir.cross(mid.subtract(camera));
        if (side.lengthSqr() < 1.0E-6D) {
            return;
        }
        side = side.normalize().scale(width * 0.5D);
        ResourceLocation texture = GAZE;
        if (dashed) {
            texture = GAZE_DASH;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        float u1 = (float) (length / 1.5D);
        FxDraw.vertex(consumer, pose, (float) (from.x + side.x), (float) (from.y + side.y), (float) (from.z + side.z), -scroll, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (from.x - side.x), (float) (from.y - side.y), (float) (from.z - side.z), -scroll, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (to.x - side.x), (float) (to.y - side.y), (float) (to.z - side.z), u1 - scroll, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (to.x + side.x), (float) (to.y + side.y), (float) (to.z + side.z), u1 - scroll, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    private static void renderRecall(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, UrGhast boss, BossTrack track,
                                     Quaternionf cameraRotation, float partialTick, boolean decorative) {
        Vec3 center = boss.getPosition(partialTick).add(0.0D, boss.getBbHeight() * 0.5D, 0.0D);
        VertexConsumer tears = buffers.getBuffer(RenderType.entityTranslucent(TEAR));
        float time = track.recallAge + partialTick;
        for (int id : track.recallIds) {
            Entity ghast = level.getEntity(id);
            if (ghast == null || !ghast.isAlive()) {
                continue;
            }
            Vec3 start = ghast.getPosition(partialTick).add(0.0D, ghast.getBbHeight() * 0.5D, 0.0D);
            Vec3 path = center.subtract(start);
            double length = path.length();
            int count = Math.max(1, (int) (length / 1.6D));
            for (int k = 0; k < count; k++) {
                float f = ((k + time * 0.15F) % count) / count;
                Vec3 pos = start.add(path.scale(f));
                float fade = Math.min(1.0F, f * 6.0F);
                billboardRect(poseStack, tears, cameraRotation, pos.x, pos.y, pos.z, 0.14F, 0.21F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.85F * fade);
            }
        }
        if (!decorative) {
            return;
        }
        VertexConsumer halo = buffers.getBuffer(RenderType.entityTranslucent(HALO));
        for (int id : track.recallIds) {
            Entity ghast = level.getEntity(id);
            if (ghast == null || !ghast.isAlive()) {
                continue;
            }
            Vec3 pos = ghast.getPosition(partialTick).add(0.0D, ghast.getBbHeight() * 0.5D, 0.0D);
            float pulse = 1.2F + 0.15F * Mth.sin(time * 0.4F);
            billboardRect(poseStack, halo, cameraRotation, pos.x, pos.y, pos.z, pulse, pulse, DARK_R, DARK_G, DARK_B, 0.7F);
        }
    }

    private static void renderStacks(PoseStack poseStack, MultiBufferSource buffers, UrGhast boss, BossTrack track, Quaternionf cameraRotation, float partialTick) {
        Vec3 base = boss.getPosition(partialTick);
        VertexConsumer tears = buffers.getBuffer(RenderType.entityTranslucent(TEAR));
        float time = track.age + partialTick;
        for (int i = 0; i < track.stacks; i++) {
            double angle = Math.PI * 2.0D * i / track.stacks + time * 0.02D;
            double x = base.x + Math.cos(angle) * 3.5D;
            double z = base.z + Math.sin(angle) * 3.5D;
            double y = base.y - 0.8D - 0.25D * Math.sin(time * 0.1D + i);
            billboardRect(poseStack, tears, cameraRotation, x, y, z, 0.32F, 0.48F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.9F);
        }
    }

    private static void renderPool(PoseStack poseStack, MultiBufferSource buffers, PoolFx pool, float partialTick, boolean decorative) {
        float age = pool.age + partialTick;
        float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float fade = Mth.clamp((pool.ticks - age) / 30.0F, 0.0F, 1.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(POOL));
        FxDraw.flatSquare(consumer, poseStack.last(), pool.x, pool.y + 0.04F, pool.z, pool.radius * grow, age * 0.01F, BLUE_R, BLUE_G, BLUE_B, 0.7F * fade, FxDraw.FULL_BRIGHT);
        if (!decorative) {
            return;
        }
        VertexConsumer ripple = buffers.getBuffer(RenderType.entityTranslucent(RIPPLE));
        for (int k = 0; k < 2; k++) {
            float phase = (age / 30.0F + k * 0.5F) % 1.0F;
            FxDraw.flatSquare(ripple, poseStack.last(), pool.x, pool.y + 0.05F, pool.z, pool.radius * phase * grow, 0.0F, PALE_R, PALE_G, PALE_B, 0.5F * (1.0F - phase) * fade, FxDraw.FULL_BRIGHT);
        }
    }

    private static void renderSplash(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, SplashFx splash, Vec3 camera, float partialTick) {
        float t = Mth.clamp((splash.age + partialTick) / SPLASH_LIFE, 0.0F, 1.0F);
        if (splash.air) {
            if (level.getEntity(splash.ownerId) instanceof UrGhast boss) {
                Vec3 center = boss.getPosition(partialTick).add(0.0D, boss.getBbHeight() * 0.5D, 0.0D);
                renderBeam(poseStack, buffers, new Vec3(splash.x, splash.y, splash.z), center, camera, 0.35F * (1.0F - t) + 0.05F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 1.0F - t, false, 0.0F);
            }
            return;
        }
        VertexConsumer ripple = buffers.getBuffer(RenderType.entityTranslucent(RIPPLE));
        float radius = splash.radius * (float) Math.sqrt(t);
        FxDraw.flatSquare(ripple, poseStack.last(), splash.x, splash.y + 0.06F, splash.z, radius, 0.0F, PALE_R, PALE_G, PALE_B, 0.9F * (1.0F - t), FxDraw.FULL_BRIGHT);
    }

    public static void drawTear(PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraRotation, float size, float age, float alpha) {
        VertexConsumer tear = buffers.getBuffer(RenderType.entityTranslucent(TEAR));
        float half = size * 0.5F;
        float bottom = -size * 0.55F;
        float top = size * 0.95F;
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 60.0F + age * 1.5F));
            PoseStack.Pose pose = poseStack.last();
            FxDraw.vertex(tear, pose, -half, top, 0.0F, 0.0F, 0.0F, PALE_R, PALE_G, PALE_B, alpha * 0.7F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            FxDraw.vertex(tear, pose, -half, bottom, 0.0F, 0.0F, 1.0F, PALE_R, PALE_G, PALE_B, alpha * 0.7F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            FxDraw.vertex(tear, pose, half, bottom, 0.0F, 1.0F, 1.0F, PALE_R, PALE_G, PALE_B, alpha * 0.7F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            FxDraw.vertex(tear, pose, half, top, 0.0F, 1.0F, 0.0F, PALE_R, PALE_G, PALE_B, alpha * 0.7F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            poseStack.popPose();
        }
        VertexConsumer core = buffers.getBuffer(RenderType.entityTranslucent(CORE));
        float pulse = size * (0.22F + 0.03F * Mth.sin(age * 0.3F));
        poseStack.pushPose();
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 3.0F));
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(core, pose, -pulse, -pulse, 0.0F, 0.0F, 1.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(core, pose, pulse, -pulse, 0.0F, 1.0F, 1.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(core, pose, pulse, pulse, 0.0F, 1.0F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(core, pose, -pulse, pulse, 0.0F, 0.0F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        poseStack.popPose();
    }

    public static void billboardRect(PoseStack poseStack, VertexConsumer consumer, Quaternionf cameraRotation, double x, double y, double z, float halfWidth, float halfHeight,
                                     float r, float g, float b, float alpha) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(consumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        poseStack.popPose();
    }

    private static final class BossTrack {
        private int age;
        private boolean gazeActive;
        private int gazeAge;
        private int gazeDuration = 1;
        private int[] gazeIds = new int[0];
        private boolean[] gazeSight = new boolean[0];
        private int endAge = -1;
        private int[] endIds;
        private boolean[] endHits;
        private boolean rainOn;
        private float rainFade;
        private double rainX;
        private double rainZ;
        private double rainR;
        private double prevRainX;
        private double prevRainZ;
        private double prevRainR;
        private double goalX;
        private double goalZ;
        private double goalR;
        private double centerGround = Double.NaN;
        private int columns;
        private double[] colAngle;
        private double[] colRadius;
        private double[] colPhase;
        private double[] colGround;
        private boolean recallOn;
        private int recallAge;
        private int[] recallIds = new int[0];
        private int stacks;
        private boolean charging;
        private int tearAge;
        private int tearTotal = 1;

        private void initColumns(RandomSource random) {
            this.columns = 140;
            this.colAngle = new double[this.columns];
            this.colRadius = new double[this.columns];
            this.colPhase = new double[this.columns];
            this.colGround = new double[this.columns];
            for (int i = 0; i < this.columns; i++) {
                this.colAngle[i] = random.nextDouble() * Math.PI * 2.0D;
                this.colRadius[i] = Math.sqrt(random.nextDouble());
                this.colPhase[i] = random.nextDouble();
                this.colGround[i] = Double.NaN;
            }
            this.centerGround = Double.NaN;
        }
    }

    private static final class PoolFx {
        private final float x;
        private final float y;
        private final float z;
        private final float radius;
        private final int ticks;
        private int age;

        private PoolFx(float x, float y, float z, float radius, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ticks = ticks;
        }
    }

    private static final class SplashFx {
        private final float x;
        private final float y;
        private final float z;
        private final float radius;
        private final boolean air;
        private final int ownerId;
        private int age;

        private SplashFx(float x, float y, float z, float radius, boolean air, int ownerId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.air = air;
            this.ownerId = ownerId;
        }
    }

    private static final class Droplet {
        private Vec3 pos;
        private Vec3 prev;
        private Vec3 vel;
        private int age;

        private Droplet(Vec3 pos, Vec3 vel) {
            this.pos = pos;
            this.prev = pos;
            this.vel = vel;
        }
    }

    private static final class RippleFx {
        private final double x;
        private final double y;
        private final double z;
        private int age;

        private RippleFx(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
