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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
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
    private static final ResourceLocation RAIN = tex("rain");
    private static final ResourceLocation EYE = tex("eye");
    private static final ResourceLocation IRIS = tex("iris");
    private static final ResourceLocation GAZE_DASH = tex("gaze_dash");
    private static final ResourceLocation RIPPLE = tex("ripple");
    private static final ResourceLocation POOL = tex("pool");

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
    private static final float CLOUD_R = 0.11F;
    private static final float CLOUD_G = 0.13F;
    private static final float CLOUD_B = 0.2F;

    private static final double FACE_FORWARD = 6.8D;
    private static final double TEAR_FORWARD = 8.5D;
    private static final double TEAR_DROP = 3.0D;
    private static final double EYE_LIFT = 0.95D;
    private static final double RAIN_SCAN = 48.0D;
    private static final float RAIN_FALL_SPEED = 1.4F;
    private static final float STREAK_LENGTH = 1.8F;
    private static final float STREAK_WIDTH = 0.07F;
    private static final int GAZE_CLOSE = 8;
    private static final int SPLASH_LIFE = 12;
    private static final int RIPPLE_LIFE = 14;
    private static final int TRAIL_LENGTH = 8;
    private static final int MAX_RIPPLES = 160;

    private static final Map<Integer, BossTrack> TRACKS = new HashMap<>();
    private static final Set<Integer> FIREBALLS = new HashSet<>();
    private static final Map<Integer, List<Vec3>> FIRE_TRAILS = new HashMap<>();
    private static final List<PoolFx> POOLS = new ArrayList<>();
    private static final List<SplashFx> SPLASHES = new ArrayList<>();
    private static final List<RippleFx> RIPPLES = new ArrayList<>();

    private UrGhastFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/urghast/px/" + name + ".png");
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
        RandomSource random = level.getRandom();
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
            for (int i = 0; i < track.endIds.length; i++) {
                if (!track.endHits[i] || !(level.getEntity(track.endIds[i]) instanceof LivingEntity target)) {
                    continue;
                }
                Vec3 eye = target.getEyePosition();
                FxShake.at(eye.x, eye.y, eye.z, 0.5F, 4.0F, 8);
                for (int k = 0; k < 14; k++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double speed = 0.12D + random.nextDouble() * 0.12D;
                    FxParticles.glow(FxTextures.SPARK, eye.x, eye.y + EYE_LIFT, eye.z, Math.cos(angle) * speed, (random.nextDouble() - 0.3D) * 0.2D,
                            Math.sin(angle) * speed, 8 + random.nextInt(5), 0.16F)
                        .color(1.0F, 0.85F, 0.88F).fadeTo(CRIMSON_R, CRIMSON_G, CRIMSON_B).drag(0.85D).streak(2.5F);
                }
            }
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
            if (!air) {
                FxShake.at(data[0], data[1], data[2], 0.6F, 14.0F, 10);
            }
            int count = 20;
            if (air) {
                count = 26;
            }
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.15D + random.nextDouble() * 0.25D;
                double up = 0.25D + random.nextDouble() * 0.3D;
                if (air) {
                    up = (random.nextDouble() - 0.5D) * 0.4D;
                }
                FxParticles.glow(TEAR, data[0], data[1] + 0.3D, data[2], Math.cos(angle) * speed, up, Math.sin(angle) * speed, 18 + random.nextInt(8),
                        0.22F + random.nextFloat() * 0.14F)
                    .color(PALE_R, PALE_G, PALE_B).fadeTo(BLUE_R, BLUE_G, BLUE_B).gravity(0.04D).drag(0.92D);
            }
            if (air) {
                for (int i = 0; i < 8; i++) {
                    FxParticles.glow(CORE, data[0], data[1], data[2], (random.nextDouble() - 0.5D) * 0.4D, (random.nextDouble() - 0.5D) * 0.4D,
                            (random.nextDouble() - 0.5D) * 0.4D, 16 + random.nextInt(8), 0.3F)
                        .drag(0.9D).gravity(0.02D).spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.5F).fadeTo(0.4F, 0.4F, 0.4F);
                }
            } else {
                for (int i = 0; i < 10; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    FxParticles.soft(FxTextures.SMOKE, data[0] + Math.cos(angle) * data[3] * 0.4D, data[1] + 0.3D, data[2] + Math.sin(angle) * data[3] * 0.4D,
                            Math.cos(angle) * 0.08D, 0.02D, Math.sin(angle) * 0.08D, 24 + random.nextInt(10), 1.0F)
                        .color(PALE_R, PALE_G, PALE_B).fadeTo(BLUE_R, BLUE_G, BLUE_B).alpha(0.5F).grow(2.2F).drag(0.9D);
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
                        if (track.gazeAge % 5 == 0) {
                            FxParticles.glow(TEAR, target.getX() + (random.nextDouble() - 0.5D) * 0.3D, target.getEyeY() + EYE_LIFT - 0.2D,
                                    target.getZ() + (random.nextDouble() - 0.5D) * 0.3D, 0.0D, -0.02D, 0.0D, 16, 0.2F)
                                .color(CRIMSON_R, CRIMSON_G, CRIMSON_B).fadeTo(DARK_R, DARK_G, DARK_B).gravity(0.012D).drag(0.98D);
                        }
                    }
                }
                if (track.gazeAge % 2 == 0) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double tilt = (random.nextDouble() - 0.5D) * Math.PI;
                    double dist = 3.0D;
                    double ox = Math.cos(angle) * Math.cos(tilt) * dist;
                    double oy = Math.sin(tilt) * dist;
                    double oz = Math.sin(angle) * Math.cos(tilt) * dist;
                    FxParticles.glow(FxTextures.SPARK, origin.x + ox, origin.y + oy, origin.z + oz, -ox * 0.12D, -oy * 0.12D, -oz * 0.12D, 8, 0.2F)
                        .color(1.0F, 0.6F, 0.65F).fadeTo(CRIMSON_R, CRIMSON_G, CRIMSON_B).drag(1.0D).streak(2.0F);
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
                if (track.recallAge % 2 == 0) {
                    Vec3 center = boss.position().add(0.0D, boss.getBbHeight() * 0.5D, 0.0D);
                    for (int id : track.recallIds) {
                        Entity ghast = level.getEntity(id);
                        if (ghast == null || !ghast.isAlive()) {
                            continue;
                        }
                        Vec3 dir = center.subtract(ghast.position()).normalize();
                        FxParticles.glow(FxTextures.MOTE, ghast.getX() + (random.nextDouble() - 0.5D) * 1.5D, ghast.getY() + random.nextDouble() * ghast.getBbHeight(),
                                ghast.getZ() + (random.nextDouble() - 0.5D) * 1.5D, -dir.x * 0.05D, -dir.y * 0.05D + 0.02D, -dir.z * 0.05D, 18, 0.3F)
                            .color(CRIMSON_R, CRIMSON_G, CRIMSON_B).fadeTo(DARK_R, DARK_G, DARK_B).drag(0.95D);
                    }
                }
            }
            if (track.charging) {
                track.tearAge++;
                Vec3 origin = faceOrigin(boss, 1.0F, TEAR_FORWARD, TEAR_DROP);
                float progress = Mth.clamp((float) track.tearAge / Math.max(1, track.tearTotal), 0.0F, 1.0F);
                for (int k = 0; k < 2; k++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double tilt = (random.nextDouble() - 0.5D) * Math.PI;
                    double dist = 2.5D + 1.5D * (1.0D - progress);
                    double ox = Math.cos(angle) * Math.cos(tilt) * dist;
                    double oy = Math.sin(tilt) * dist;
                    double oz = Math.sin(angle) * Math.cos(tilt) * dist;
                    FxParticles.glow(FxTextures.SPARK, origin.x + ox, origin.y + oy, origin.z + oz, -ox * 0.13D, -oy * 0.13D, -oz * 0.13D, 7, 0.18F)
                        .color(PALE_R, PALE_G, PALE_B).fadeTo(BLUE_R, BLUE_G, BLUE_B).drag(1.0D).streak(2.0F);
                }
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
                    if (k < 2) {
                        FxParticles.glow(TEAR, x, track.colGround[i] + 0.05D, z, (random.nextDouble() - 0.5D) * 0.08D, 0.12D + random.nextDouble() * 0.08D,
                                (random.nextDouble() - 0.5D) * 0.08D, 10, 0.14F)
                            .color(PALE_R, PALE_G, PALE_B).fadeTo(BLUE_R, BLUE_G, BLUE_B).gravity(0.035D).drag(0.95D);
                    }
                }
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = Math.sqrt(random.nextDouble()) * track.rainR;
                FxParticles.soft(FxTextures.SMOKE, track.rainX + Math.cos(angle) * dist, boss.getY() + 0.5D, track.rainZ + Math.sin(angle) * dist,
                        Math.cos(angle + Math.PI * 0.5D) * 0.05D, 0.0D, Math.sin(angle + Math.PI * 0.5D) * 0.05D, 34, 2.4F)
                    .color(0.3F, 0.34F, 0.46F).fadeTo(CLOUD_R, CLOUD_G, CLOUD_B).alpha(0.55F).grow(3.6F).drag(0.98D).fade(0.25F, 2.0F);
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
            Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            List<Vec3> trail = FIRE_TRAILS.computeIfAbsent(id, key -> new ArrayList<>());
            trail.add(0, center);
            while (trail.size() > TRAIL_LENGTH) {
                trail.remove(trail.size() - 1);
            }
            if (decorative && entity.tickCount % 2 == 0) {
                FxParticles.glow(TEAR, center.x + (random.nextDouble() - 0.5D) * 0.6D, center.y + (random.nextDouble() - 0.5D) * 0.6D,
                        center.z + (random.nextDouble() - 0.5D) * 0.6D, 0.0D, -0.02D, 0.0D, 14, 0.24F)
                    .color(CRIMSON_R, CRIMSON_G, CRIMSON_B).fadeTo(DARK_R, DARK_G, DARK_B).gravity(0.015D).drag(0.95D);
            }
        }
        for (PoolFx pool : POOLS) {
            if (decorative && pool.age % 4 == 0) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = Math.sqrt(random.nextDouble()) * pool.radius * 0.85D;
                FxParticles.glow(FxTextures.MOTE, pool.x + Math.cos(angle) * dist, pool.y + 0.1D, pool.z + Math.sin(angle) * dist,
                        0.0D, 0.03D + random.nextDouble() * 0.03D, 0.0D, 24, 0.2F)
                    .color(PALE_R, PALE_G, PALE_B).fadeTo(BLUE_R, BLUE_G, BLUE_B).drag(0.97D).flicker(0.3F, random.nextFloat() * 10.0F);
            }
        }
        POOLS.removeIf(pool -> ++pool.age >= pool.ticks);
        SPLASHES.removeIf(splash -> ++splash.age > SPLASH_LIFE);
        RIPPLES.removeIf(ripple -> ++ripple.age > RIPPLE_LIFE);
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
        if (TRACKS.isEmpty() && POOLS.isEmpty() && SPLASHES.isEmpty() && RIPPLES.isEmpty()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        float partialTick = event.getPartialTick();
        Camera camera = event.getCamera();
        FxDraw.camera(camera);
        Vec3 cameraPos = camera.getPosition();
        Quaternionf cameraRotation = camera.rotation();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        PoseStack.Pose pose = poseStack.last();

        for (Map.Entry<Integer, BossTrack> entry : TRACKS.entrySet()) {
            BossTrack track = entry.getValue();
            if (!(level.getEntity(entry.getKey()) instanceof UrGhast boss)) {
                continue;
            }
            if (track.rainFade > 0.0F && track.colAngle != null) {
                renderRain(pose, buffers, boss, track, cameraPos, partialTick);
            }
            if (track.gazeActive || track.endAge >= 0) {
                renderGaze(pose, buffers, level, boss, track, partialTick);
            }
            if (track.recallOn) {
                renderRecall(pose, buffers, level, boss, track, partialTick, decorative);
            }
            if (track.stacks > 0 && boss.isAlive()) {
                renderStacks(pose, buffers, boss, track, partialTick);
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
        if (!POOLS.isEmpty()) {
            VertexConsumer water = buffers.getBuffer(FxRenderTypes.soft(POOL));
            for (PoolFx pool : POOLS) {
                FxDraw.flat(water, pose, pool.x, pool.y + 0.04D, pool.z, pool.radius * poolGrow(pool, partialTick), 0.0F, 1.0F, 1.0F, 1.0F, 0.85F * poolFade(pool, partialTick));
            }
            VertexConsumer rim = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            for (PoolFx pool : POOLS) {
                float radius = pool.radius * poolGrow(pool, partialTick);
                float alpha = 0.55F * poolFade(pool, partialTick);
                FxDraw.ring(rim, pose, pool.x, pool.y + 0.05D, pool.z, radius * 0.9F, radius * 1.02F, 40, 0.0F, 1.0F, BLUE_R, BLUE_G, BLUE_B, alpha, alpha);
            }
            if (decorative) {
                VertexConsumer ripple = buffers.getBuffer(FxRenderTypes.glow(RIPPLE));
                for (PoolFx pool : POOLS) {
                    float age = pool.age + partialTick;
                    float grow = poolGrow(pool, partialTick);
                    float fade = poolFade(pool, partialTick);
                    for (int k = 0; k < 3; k++) {
                        float phase = (age / 36.0F + k / 3.0F) % 1.0F;
                        FxDraw.flat(ripple, pose, pool.x, pool.y + 0.06D, pool.z, pool.radius * phase * grow, 0.0F, PALE_R, PALE_G, PALE_B, 0.6F * (1.0F - phase) * fade);
                    }
                }
            }
        }
        renderSplashes(pose, buffers, level, partialTick);
        if (decorative) {
            if (!RIPPLES.isEmpty()) {
                VertexConsumer ripples = buffers.getBuffer(FxRenderTypes.glow(RIPPLE));
                for (RippleFx ripple : RIPPLES) {
                    float t = Mth.clamp((ripple.age + partialTick) / RIPPLE_LIFE, 0.0F, 1.0F);
                    FxDraw.flat(ripples, pose, ripple.x, ripple.y + 0.03D, ripple.z, 0.1F + 0.55F * t, 0.0F, PALE_R, PALE_G, PALE_B, 0.7F * (1.0F - t));
                }
            }
            if (!FIRE_TRAILS.isEmpty()) {
                VertexConsumer ribbon = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
                for (List<Vec3> trail : FIRE_TRAILS.values()) {
                    int count = trail.size();
                    if (count < 2) {
                        continue;
                    }
                    Vec3[] points = new Vec3[count];
                    float[] widths = new float[count];
                    float[] alphas = new float[count];
                    for (int i = 0; i < count; i++) {
                        points[i] = trail.get(count - 1 - i);
                        float f = (float) i / (count - 1);
                        widths[i] = 0.08F + 0.5F * f;
                        alphas[i] = 0.85F * f;
                    }
                    FxDraw.trail(ribbon, pose, points, count, widths, alphas, 0.0F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B);
                }
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static float poolGrow(PoolFx pool, float partialTick) {
        return Mth.clamp((pool.age + partialTick) / 6.0F, 0.0F, 1.0F);
    }

    private static float poolFade(PoolFx pool, float partialTick) {
        return Mth.clamp((pool.ticks - pool.age - partialTick) / 30.0F, 0.0F, 1.0F);
    }

    private static void renderRain(PoseStack.Pose pose, MultiBufferSource buffers, UrGhast boss, BossTrack track, Vec3 camera, float partialTick) {
        double cx = Mth.lerp(partialTick, track.prevRainX, track.rainX);
        double cz = Mth.lerp(partialTick, track.prevRainZ, track.rainZ);
        double radius = Mth.lerp(partialTick, track.prevRainR, track.rainR);
        double top = Mth.lerp(partialTick, boss.yo, boss.getY()) + 1.0D;
        float fade = track.rainFade;
        float time = track.age + partialTick;
        VertexConsumer streaks = buffers.getBuffer(FxRenderTypes.glow(RAIN));
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
            double sx = -dz / len * STREAK_WIDTH;
            double sz = dx / len * STREAK_WIDTH;
            float alpha = 0.85F * fade;
            FxDraw.glowVertex(streaks, pose, x - sx, tail, z - sz, 0.0F, 0.0F, PALE_R, PALE_G, PALE_B, alpha);
            FxDraw.glowVertex(streaks, pose, x - sx, head, z - sz, 0.0F, 1.0F, PALE_R, PALE_G, PALE_B, alpha);
            FxDraw.glowVertex(streaks, pose, x + sx, head, z + sz, 1.0F, 1.0F, PALE_R, PALE_G, PALE_B, alpha);
            FxDraw.glowVertex(streaks, pose, x + sx, tail, z + sz, 1.0F, 0.0F, PALE_R, PALE_G, PALE_B, alpha);
        }
        double bottom = track.centerGround;
        if (Double.isNaN(bottom)) {
            bottom = top - 25.0D;
        }
        float height = (float) (top - bottom);
        if (height > 0.5F) {
            VertexConsumer curtain = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
            int segments = 48;
            float circumference = (float) (Math.PI * 2.0D * radius);
            for (int layer = 0; layer < 2; layer++) {
                float scroll = -time * 0.05F;
                float vOffset = 0.0F;
                float layerRadius = (float) radius;
                float r = BLUE_R;
                float g = BLUE_G;
                float b = BLUE_B;
                float alpha = 0.42F * fade;
                if (layer == 1) {
                    scroll = -time * 0.11F;
                    vOffset = 0.5F;
                    layerRadius = (float) radius - 0.2F;
                    r = PALE_R;
                    g = PALE_G;
                    b = PALE_B;
                    alpha = 0.26F * fade;
                }
                float uTop = scroll;
                float uBottom = scroll + height / 6.0F;
                for (int s = 0; s < segments; s++) {
                    double a0 = Math.PI * 2.0D * s / segments;
                    double a1 = Math.PI * 2.0D * (s + 1) / segments;
                    double x0 = cx + Math.cos(a0) * layerRadius;
                    double z0 = cz + Math.sin(a0) * layerRadius;
                    double x1 = cx + Math.cos(a1) * layerRadius;
                    double z1 = cz + Math.sin(a1) * layerRadius;
                    float v0 = vOffset + circumference * s / segments / 3.0F;
                    float v1 = vOffset + circumference * (s + 1) / segments / 3.0F;
                    FxDraw.glowVertex(curtain, pose, x0, top, z0, uTop, v0, r, g, b, alpha * 0.25F);
                    FxDraw.glowVertex(curtain, pose, x0, bottom, z0, uBottom, v0, r, g, b, alpha);
                    FxDraw.glowVertex(curtain, pose, x1, bottom, z1, uBottom, v1, r, g, b, alpha);
                    FxDraw.glowVertex(curtain, pose, x1, top, z1, uTop, v1, r, g, b, alpha * 0.25F);
                }
            }
        }
        VertexConsumer border = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.ring(border, pose, cx, bottom + 0.05D, cz, (float) radius - 0.3F, (float) radius + 0.3F, 56, 0.0F, 1.0F, BLUE_R, BLUE_G, BLUE_B, 0.8F * fade, 0.8F * fade);
        VertexConsumer cloud = buffers.getBuffer(FxRenderTypes.soft(FxTextures.GLOW));
        FxDraw.flat(cloud, pose, cx, top - 0.6D, cz, (float) radius * 1.35F, 0.0F, CLOUD_R, CLOUD_G, CLOUD_B, 0.6F * fade);
    }

    private static void renderGaze(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, UrGhast boss, BossTrack track, float partialTick) {
        Vec3 origin = faceOrigin(boss, partialTick, FACE_FORWARD, 0.0D);
        float time = track.age + partialTick;
        if (track.gazeActive) {
            float progress = Mth.clamp((track.gazeAge + partialTick) / track.gazeDuration, 0.0F, 1.0F);
            int count = track.gazeIds.length;
            Vec3[] eyes = new Vec3[count];
            for (int i = 0; i < count; i++) {
                if (level.getEntity(track.gazeIds[i]) instanceof LivingEntity target) {
                    eyes[i] = target.getEyePosition(partialTick);
                }
            }
            float open = (float) Math.pow(progress, 0.6D);
            VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            FxDraw.sprite(glow, pose, origin.x, origin.y, origin.z, 2.5F + 2.0F * progress, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.3F + 0.5F * progress);
            for (int i = 0; i < count; i++) {
                if (eyes[i] != null) {
                    FxDraw.sprite(glow, pose, eyes[i].x, eyes[i].y + EYE_LIFT, eyes[i].z, 1.6F * open, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.55F);
                }
            }
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            for (int i = 0; i < count; i++) {
                if (eyes[i] == null || !track.gazeSight[i]) {
                    continue;
                }
                Vec3 end = eyes[i].add(0.0D, -0.2D, 0.0D);
                float width = 0.08F + 0.12F * progress;
                FxDraw.beam(beam, pose, origin, end, width * 1.6F, width, 0.0F, 1.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.5F + 0.4F * progress, 0.5F + 0.4F * progress);
                FxDraw.beam(beam, pose, origin, end, width * 0.4F, width * 0.3F, 0.0F, 1.0F, 1.0F, 0.85F, 0.88F, 0.7F * progress, 0.7F * progress);
            }
            VertexConsumer flow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
            for (int i = 0; i < count; i++) {
                if (eyes[i] == null || !track.gazeSight[i]) {
                    continue;
                }
                Vec3 end = eyes[i].add(0.0D, -0.2D, 0.0D);
                float length = (float) origin.distanceTo(end);
                float scroll = time * 0.08F;
                FxDraw.beam(flow, pose, origin, end, 0.5F, 0.25F, -scroll, length / 8.0F - scroll, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.6F * progress, 0.6F * progress);
            }
            VertexConsumer dash = buffers.getBuffer(FxRenderTypes.glow(GAZE_DASH));
            for (int i = 0; i < count; i++) {
                if (eyes[i] == null || track.gazeSight[i]) {
                    continue;
                }
                Vec3 end = eyes[i].add(0.0D, -0.2D, 0.0D);
                float length = (float) origin.distanceTo(end);
                float scroll = time * 0.15F;
                FxDraw.beam(dash, pose, origin, end, 0.14F, 0.14F, -scroll, length / 1.5F - scroll, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.5F, 0.5F);
            }
            VertexConsumer lid = buffers.getBuffer(FxRenderTypes.soft(EYE));
            for (int i = 0; i < count; i++) {
                if (eyes[i] != null && open > 0.02F) {
                    FxDraw.stretchedSprite(lid, pose, eyes[i].x, eyes[i].y + EYE_LIFT, eyes[i].z, 1.2F, 0.6F * open, 0.0F, PALE_R, PALE_G, PALE_B, 0.95F);
                }
            }
            if (open > 0.3F) {
                VertexConsumer iris = buffers.getBuffer(FxRenderTypes.soft(IRIS));
                float size = 0.36F * Math.min(1.0F, (open - 0.3F) / 0.5F);
                for (int i = 0; i < count; i++) {
                    if (eyes[i] != null) {
                        FxDraw.stretchedSprite(iris, pose, eyes[i].x, eyes[i].y + EYE_LIFT, eyes[i].z, size, Math.min(size, 0.42F * open), 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
            return;
        }
        if (track.endIds == null) {
            return;
        }
        float t = Mth.clamp((track.endAge + partialTick) / GAZE_CLOSE, 0.0F, 1.0F);
        float flash = 1.0F - Mth.clamp((track.endAge + partialTick) / 4.0F, 0.0F, 1.0F);
        int count = track.endIds.length;
        Vec3[] eyes = new Vec3[count];
        for (int i = 0; i < count; i++) {
            if (level.getEntity(track.endIds[i]) instanceof LivingEntity target) {
                eyes[i] = target.getEyePosition(partialTick);
            }
        }
        if (flash > 0.0F) {
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            for (int i = 0; i < count; i++) {
                if (eyes[i] != null && track.endHits[i]) {
                    FxDraw.beam(beam, pose, origin, eyes[i].add(0.0D, -0.2D, 0.0D), 0.5F * flash + 0.1F, 0.3F * flash + 0.08F, 0.0F, 1.0F,
                        CRIMSON_R, CRIMSON_G, CRIMSON_B, flash, flash);
                }
            }
            VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
            for (int i = 0; i < count; i++) {
                if (eyes[i] != null && track.endHits[i]) {
                    FxDraw.sprite(flare, pose, eyes[i].x, eyes[i].y + EYE_LIFT, eyes[i].z, 2.4F * flash + 0.6F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, flash);
                }
            }
        }
        VertexConsumer lid = buffers.getBuffer(FxRenderTypes.soft(EYE));
        for (int i = 0; i < count; i++) {
            if (eyes[i] == null || t >= 0.98F) {
                continue;
            }
            float r = PALE_R;
            float g = PALE_G;
            float b = PALE_B;
            if (track.endHits[i]) {
                r = CRIMSON_R;
                g = CRIMSON_G;
                b = CRIMSON_B;
            }
            FxDraw.stretchedSprite(lid, pose, eyes[i].x, eyes[i].y + EYE_LIFT, eyes[i].z, 1.2F + 0.2F * t, 0.6F * (1.0F - t), 0.0F, r, g, b, 0.95F * (1.0F - t * 0.5F));
        }
    }

    private static void renderRecall(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, UrGhast boss, BossTrack track, float partialTick, boolean decorative) {
        Vec3 center = boss.getPosition(partialTick).add(0.0D, boss.getBbHeight() * 0.5D, 0.0D);
        float time = track.recallAge + partialTick;
        List<Vec3> starts = new ArrayList<>();
        for (int id : track.recallIds) {
            Entity ghast = level.getEntity(id);
            if (ghast != null && ghast.isAlive()) {
                starts.add(ghast.getPosition(partialTick).add(0.0D, ghast.getBbHeight() * 0.5D, 0.0D));
            }
        }
        if (starts.isEmpty()) {
            return;
        }
        if (decorative) {
            VertexConsumer halo = buffers.getBuffer(FxRenderTypes.soft(FxTextures.GLOW));
            float pulse = 2.6F + 0.3F * Mth.sin(time * 0.4F);
            for (Vec3 start : starts) {
                FxDraw.sprite(halo, pose, start.x, start.y, start.z, pulse, 0.0F, DARK_R, DARK_G, DARK_B, 0.8F);
            }
        }
        VertexConsumer line = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        for (Vec3 start : starts) {
            FxDraw.beam(line, pose, start, center, 0.12F, 0.3F, 0.0F, 1.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.1F, 0.3F);
        }
        VertexConsumer tears = buffers.getBuffer(FxRenderTypes.glow(TEAR));
        for (Vec3 start : starts) {
            Vec3 path = center.subtract(start);
            double length = path.length();
            int count = Math.max(1, (int) (length / 1.6D));
            for (int k = 0; k < count; k++) {
                float f = ((k + time * 0.15F) % count) / count;
                Vec3 pos = start.add(path.scale(f));
                float fade = Math.min(1.0F, f * 6.0F);
                FxDraw.sprite(tears, pose, pos.x, pos.y, pos.z, 0.5F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.9F * fade);
            }
        }
    }

    private static void renderStacks(PoseStack.Pose pose, MultiBufferSource buffers, UrGhast boss, BossTrack track, float partialTick) {
        Vec3 base = boss.getPosition(partialTick);
        float time = track.age + partialTick;
        double[] xs = new double[track.stacks];
        double[] ys = new double[track.stacks];
        double[] zs = new double[track.stacks];
        for (int i = 0; i < track.stacks; i++) {
            double angle = Math.PI * 2.0D * i / track.stacks + time * 0.02D;
            xs[i] = base.x + Math.cos(angle) * 3.5D;
            zs[i] = base.z + Math.sin(angle) * 3.5D;
            ys[i] = base.y - 0.8D - 0.25D * Math.sin(time * 0.1D + i);
        }
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        for (int i = 0; i < track.stacks; i++) {
            FxDraw.sprite(glow, pose, xs[i], ys[i], zs[i], 1.6F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.45F);
        }
        VertexConsumer tears = buffers.getBuffer(FxRenderTypes.soft(TEAR));
        for (int i = 0; i < track.stacks; i++) {
            FxDraw.sprite(tears, pose, xs[i], ys[i], zs[i], 1.0F, 0.12F * Mth.sin(time * 0.1F + i), CRIMSON_R, CRIMSON_G, CRIMSON_B, 0.95F);
        }
    }

    private static void renderSplashes(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, float partialTick) {
        if (SPLASHES.isEmpty()) {
            return;
        }
        VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
        for (SplashFx splash : SPLASHES) {
            if (!splash.air) {
                float t = Mth.clamp((splash.age + partialTick) / SPLASH_LIFE, 0.0F, 1.0F);
                FxDraw.flat(shock, pose, splash.x, splash.y + 0.06D, splash.z, splash.radius * (float) Math.sqrt(t), 0.0F, PALE_R, PALE_G, PALE_B, 0.9F * (1.0F - t));
            }
        }
        VertexConsumer ripple = buffers.getBuffer(FxRenderTypes.glow(RIPPLE));
        for (SplashFx splash : SPLASHES) {
            if (!splash.air) {
                float t = Mth.clamp((splash.age + partialTick) / SPLASH_LIFE, 0.0F, 1.0F);
                FxDraw.flat(ripple, pose, splash.x, splash.y + 0.07D, splash.z, splash.radius * (0.3F + 0.9F * t), 0.0F, BLUE_R, BLUE_G, BLUE_B, 1.0F - t);
            }
        }
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        for (SplashFx splash : SPLASHES) {
            float flash = 1.0F - Mth.clamp((splash.age + partialTick) / 5.0F, 0.0F, 1.0F);
            if (splash.air) {
                FxDraw.sprite(flare, pose, splash.x, splash.y, splash.z, 3.0F * flash + 0.5F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, flash);
            } else {
                FxDraw.sprite(flare, pose, splash.x, splash.y + 0.5D, splash.z, 4.0F * flash + 0.5F, 0.0F, PALE_R, PALE_G, PALE_B, flash);
            }
        }
        VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        for (SplashFx splash : SPLASHES) {
            if (splash.air && level.getEntity(splash.ownerId) instanceof UrGhast boss) {
                float t = Mth.clamp((splash.age + partialTick) / SPLASH_LIFE, 0.0F, 1.0F);
                Vec3 center = boss.getPosition(partialTick).add(0.0D, boss.getBbHeight() * 0.5D, 0.0D);
                FxDraw.beam(beam, pose, new Vec3(splash.x, splash.y, splash.z), center, 0.35F * (1.0F - t) + 0.05F, 0.6F * (1.0F - t) + 0.05F, 0.0F, 1.0F,
                    CRIMSON_R, CRIMSON_G, CRIMSON_B, 1.0F - t, (1.0F - t) * 0.6F);
            }
        }
    }

    public static void drawTear(PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraRotation, float size, float age, float alpha) {
        VertexConsumer halo = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        billboard(poseStack, halo, cameraRotation, 0.0D, size * 0.1D, 0.0D, size * 1.4F, size * 1.4F, 0.0F, BLUE_R, BLUE_G, BLUE_B, alpha * 0.5F);
        billboard(poseStack, halo, cameraRotation, 0.0D, 0.0D, 0.0D, size * 0.7F, size * 0.7F, 0.0F, CRIMSON_R, CRIMSON_G, CRIMSON_B, alpha * 0.6F);
        VertexConsumer tear = buffers.getBuffer(FxRenderTypes.glow(TEAR));
        float wobble = 1.0F + 0.05F * Mth.sin(age * 0.2F);
        billboard(poseStack, tear, cameraRotation, 0.0D, size * 0.2D, 0.0D, size * 0.84F / wobble, size * 0.84F * wobble, 0.0F, BLUE_R, BLUE_G, BLUE_B, alpha * 0.55F);
        billboard(poseStack, tear, cameraRotation, 0.0D, size * 0.2D, 0.0D, size * 0.66F, size * 0.66F, 0.0F, PALE_R, PALE_G, PALE_B, alpha * 0.8F);
        VertexConsumer core = buffers.getBuffer(FxRenderTypes.soft(CORE));
        float pulse = size * (0.24F + 0.03F * Mth.sin(age * 0.3F));
        float roll = Mth.floor(age / 6.0F) * Mth.HALF_PI * 0.5F;
        billboard(poseStack, core, cameraRotation, 0.0D, -size * 0.03D, 0.0D, pulse, pulse, roll, 1.0F, 1.0F, 1.0F, alpha);
    }

    public static void billboard(PoseStack poseStack, VertexConsumer consumer, Quaternionf cameraRotation, double x, double y, double z, float halfWidth, float halfHeight,
                                 float roll, float r, float g, float b, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(roll));
        }
        PoseStack.Pose pose = poseStack.last();
        FxDraw.glowVertex(consumer, pose, -halfWidth, -halfHeight, 0.0D, 0.0F, 1.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, halfWidth, -halfHeight, 0.0D, 1.0F, 1.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, halfWidth, halfHeight, 0.0D, 1.0F, 0.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, -halfWidth, halfHeight, 0.0D, 0.0F, 0.0F, r, g, b, alpha);
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
