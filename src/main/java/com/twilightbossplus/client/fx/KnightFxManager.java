package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.KnightPhantomConfig;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.KnightFxPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.entity.projectile.ThrownWep;
import twilightforest.init.TFItems;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KnightFxManager {
    private static final ResourceLocation GHOST = tex("ghost");
    private static final ResourceLocation FRAME = tex("frame");
    private static final ResourceLocation SWORD_LINE = tex("sword_line");
    private static final ResourceLocation SWORD_WALL = tex("sword_wall");
    private static final ResourceLocation SWORD_WALL_DASH = tex("sword_wall_dash");
    private static final ResourceLocation THRUST = tex("thrust");
    private static final ResourceLocation OATH = tex("oath");
    private static final ResourceLocation GEM = tex("gem");
    private static final ResourceLocation SHARD = tex("shard");
    private static final ResourceLocation GLINT = tex("glint");
    private static final ResourceLocation ARC = tex("arc");

    private static final float SILVER_R = 0.847F;
    private static final float SILVER_G = 0.894F;
    private static final float SILVER_B = 0.918F;
    private static final float TEAL_R = 0.373F;
    private static final float TEAL_G = 0.659F;
    private static final float TEAL_B = 0.722F;
    private static final float SHADOW_R = 0.118F;
    private static final float SHADOW_G = 0.157F;
    private static final float SHADOW_B = 0.188F;
    private static final float GOLD_R = 0.910F;
    private static final float GOLD_G = 0.769F;
    private static final float GOLD_B = 0.416F;

    private static final float CHARGE_SCALE = 1.8F;
    private static final int LINE_FADE = 8;
    private static final int LINE_FLASH = 6;
    private static final int AFTERIMAGE_LIFE = 10;
    private static final int AFTERIMAGE_MAX = 5;
    private static final int ECHO_RISE = 8;
    private static final int ECHO_FADE = 8;
    private static final int RING_LIFE = 8;
    private static final int THRUST_LIFE = 10;
    private static final int THRUST_PREVIEW = 12;
    private static final int SHARD_COUNT = 20;
    private static final int AXE_TRAIL = 6;
    private static final float WALL_HEIGHT = 3.5F;

    private static final Set<Integer> KNIGHTS = new HashSet<>();
    private static final Set<Integer> AXES = new HashSet<>();
    private static final Map<Integer, KnightTrack> TRACKS = new HashMap<>();
    private static final Map<Integer, List<Vec3>> AXE_TRAILS = new HashMap<>();
    private static final Map<Integer, ClientEcho> ECHOES = new HashMap<>();
    private static final List<ChargeLine> LINES = new ArrayList<>();
    private static final List<PickRing> RINGS = new ArrayList<>();
    private static final List<Afterimage> AFTERIMAGES = new ArrayList<>();
    private static final List<ThrustFx> THRUSTS = new ArrayList<>();
    private static final List<RegroupFx> REGROUPS = new ArrayList<>();
    @Nullable
    private static EncircleFx encircle;

    private KnightFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/knight/px/" + name + ".png");
    }

    private static KnightTrack track(int id) {
        return TRACKS.computeIfAbsent(id, key -> new KnightTrack());
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        if (type == KnightFxPacket.TRACK && data.length >= 1) {
            KnightTrack track = track(entityId);
            track.trackTarget = (int) data[0];
            track.trackAge = 0;
        } else if (type == KnightFxPacket.LINE && data.length >= 9) {
            if (entityId >= 0) {
                track(entityId).trackTarget = -1;
            }
            double y = groundY(level, (data[0] + data[3]) * 0.5D, Math.max(data[1], data[4]), (data[2] + data[5]) * 0.5D, Math.min(data[1], data[4]));
            LINES.add(new ChargeLine(data[0], data[1], data[2], data[3], data[4], data[5], (int) data[6], data[7] > 0.5F, Math.max(1, (int) data[8]), y));
            for (int i = 0; i < 12; i++) {
                double t = random.nextDouble();
                FxParticles.glow(FxTextures.SPARK, Mth.lerp(t, data[0], data[3]), y + 0.1D, Mth.lerp(t, data[2], data[5]),
                        (random.nextDouble() - 0.5D) * 0.04D, 0.08D + random.nextDouble() * 0.1D, (random.nextDouble() - 0.5D) * 0.04D, 10 + random.nextInt(6), 0.16F)
                    .color(1.0F, 1.0F, 1.0F).fadeTo(TEAL_R, TEAL_G, TEAL_B).drag(0.86D).streak(2.0F);
            }
        } else if (type == KnightFxPacket.AXE_GLOW && data.length >= 1) {
            KnightTrack track = track(entityId);
            track.glowMax = Math.max(1, (int) data[0]);
            track.glow = track.glowMax;
        } else if (type == KnightFxPacket.PICK_RING && data.length >= 5) {
            RINGS.add(new PickRing(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4])));
            for (int i = 0; i < (int) data[4]; i++) {
                float angle = data[3] + Mth.TWO_PI / data[4] * i;
                FxParticles.glow(FxTextures.SPARK, data[0] + Mth.cos(angle) * 1.0D, data[1] + 0.1D, data[2] + Mth.sin(angle) * 1.0D,
                        Mth.cos(angle) * 0.3D, 0.02D, Mth.sin(angle) * 0.3D, 8, 0.18F)
                    .color(SILVER_R, SILVER_G, SILVER_B).fadeTo(TEAL_R, TEAL_G, TEAL_B).drag(0.8D).streak(2.5F);
            }
        } else if (type == KnightFxPacket.ENCIRCLE && data.length >= 11) {
            int count = (int) data[10];
            int[] ids = new int[count];
            for (int i = 0; i < count && 11 + i < data.length; i++) {
                ids[i] = (int) data[11 + i];
            }
            encircle = new EncircleFx(data[0], groundY(level, data[0], data[1] + 1.0D, data[2], data[1]), data[2], data[3], data[4], (int) data[5], (int) data[6], (int) data[7], (int) data[8], data[9], ids);
        } else if (type == KnightFxPacket.ENCIRCLE_END) {
            encircle = null;
        } else if (type == KnightFxPacket.THRUST && data.length >= 4) {
            double y = groundY(level, data[0], data[1] + 1.0D, data[2], data[1]);
            THRUSTS.add(new ThrustFx(data[0], y, data[2], data[3]));
            FxShake.at(data[0], y, data[2], 0.45F, 14.0F, 8);
            for (int i = 0; i < 20; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.25D + random.nextDouble() * 0.25D;
                FxParticles.glow(FxTextures.SPARK, data[0], y + 0.3D + random.nextDouble() * 1.2D, data[2], Math.cos(angle) * speed, 0.03D, Math.sin(angle) * speed,
                        8 + random.nextInt(5), 0.2F)
                    .color(1.0F, 1.0F, 1.0F).fadeTo(TEAL_R, TEAL_G, TEAL_B).drag(0.84D).streak(3.0F);
            }
        } else if (type == KnightFxPacket.ECHO_SPAWN && data.length >= 6) {
            ClientEcho echo = new ClientEcho(data[0], data[1], data[2], data[3], (int) data[4], data[5]);
            ECHOES.put(entityId, echo);
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = random.nextDouble() * 0.8D;
                FxParticles.glow(FxTextures.MOTE, data[0] + Math.cos(angle) * dist, data[1] + 0.1D, data[2] + Math.sin(angle) * dist,
                        0.0D, 0.06D + random.nextDouble() * 0.06D, 0.0D, 20 + random.nextInt(10), 0.18F)
                    .color(TEAL_R, TEAL_G, TEAL_B).fadeTo(SHADOW_R, SHADOW_G, SHADOW_B).drag(0.96D).flicker(0.3F, random.nextFloat() * 10.0F);
            }
        } else if (type == KnightFxPacket.ECHO_DASH && data.length >= 8) {
            ClientEcho echo = ECHOES.get(entityId);
            if (echo == null) {
                echo = new ClientEcho(data[0], data[1], data[2], 0.0F, -1, 0.0F);
                ECHOES.put(entityId, echo);
            }
            echo.follow = -1;
            echo.start = new Vec3(data[0], data[1], data[2]);
            echo.end = new Vec3(data[3], data[4], data[5]);
            echo.pos = echo.start;
            echo.prevPos = echo.start;
            echo.delay = (int) data[6];
            echo.speed = Math.max(0.05F, data[7]);
            echo.dashAge = 0;
            echo.yaw = (float) (Mth.atan2(data[5] - data[2], data[3] - data[0]) * Mth.RAD_TO_DEG) - 90.0F;
        } else if (type == KnightFxPacket.ECHO_END) {
            ClientEcho echo = ECHOES.get(entityId);
            if (echo != null && echo.fadeAge < 0) {
                echo.fadeAge = 0;
            }
        } else if (type == KnightFxPacket.OATH && data.length >= 1) {
            track(entityId).oath = (int) data[0];
        } else if (type == KnightFxPacket.REGROUP && data.length >= 4) {
            REGROUPS.add(new RegroupFx(data[0], groundY(level, data[0], data[1] + 1.0D, data[2], data[1]), data[2], Math.max(1, (int) data[3])));
        }
    }

    private static double groundY(ClientLevel level, double x, double y, double z, double fallback) {
        double ground = NagaBossController.findGround(level, x, y, z);
        if (Double.isNaN(ground) || Math.abs(ground - y) > 8.0D) {
            return fallback;
        }
        return ground;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof KnightPhantom) {
            KNIGHTS.add(event.getEntity().getId());
        } else if (event.getEntity() instanceof ThrownWep) {
            AXES.add(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        KNIGHTS.remove(event.getEntity().getId());
        TRACKS.remove(event.getEntity().getId());
        AXES.remove(event.getEntity().getId());
        AXE_TRAILS.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        KNIGHTS.clear();
        AXES.clear();
        TRACKS.clear();
        AXE_TRAILS.clear();
        ECHOES.clear();
        LINES.clear();
        RINGS.clear();
        AFTERIMAGES.clear();
        THRUSTS.clear();
        REGROUPS.clear();
        encircle = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            if (!KNIGHTS.isEmpty() || !ECHOES.isEmpty() || !LINES.isEmpty()) {
                clearAll();
            }
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        RandomSource random = level.random;
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        for (int id : KNIGHTS) {
            if (!(level.getEntity(id) instanceof KnightPhantom knight)) {
                continue;
            }
            KnightTrack track = track(id);
            track.age++;
            if (track.glow > 0) {
                track.glow--;
            }
            if (track.trackTarget >= 0) {
                track.trackAge++;
                if (!knight.isChargingAtPlayer() || track.trackAge > 200) {
                    track.trackTarget = -1;
                }
            }
            Vec3 pos = knight.position();
            if (decorative && knight.isChargingAtPlayer() && track.lastPos != null) {
                Vec3 motion = pos.subtract(track.lastPos);
                if (motion.lengthSqr() > 0.35D * 0.35D) {
                    if (track.age % 2 == 0) {
                        addAfterimage(pos, knight.yBodyRot, false);
                    }
                    FxParticles.glow(FxTextures.SPARK, pos.x + (random.nextDouble() - 0.5D) * 0.8D, pos.y + 0.4D + random.nextDouble() * 2.4D,
                            pos.z + (random.nextDouble() - 0.5D) * 0.8D, -motion.x * 0.3D, 0.0D, -motion.z * 0.3D, 7, 0.14F)
                        .color(SILVER_R, SILVER_G, SILVER_B).fadeTo(TEAL_R, TEAL_G, TEAL_B).drag(0.85D).streak(3.0F);
                }
            }
            track.lastPos = pos;
            if (knight.deathTime == 1 && !track.shattered) {
                track.shattered = true;
                double height = knight.getBbHeight();
                for (int i = 0; i < SHARD_COUNT; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double radius = 0.2D + random.nextDouble() * knight.getBbWidth() * 0.5D;
                    double speed = 0.03D + random.nextDouble() * 0.05D;
                    FxParticles.glow(SHARD, knight.getX() + Math.cos(angle) * radius, knight.getY() + height * i / SHARD_COUNT, knight.getZ() + Math.sin(angle) * radius,
                            Math.cos(angle) * speed, 0.03D + random.nextDouble() * 0.04D, Math.sin(angle) * speed, 26 + random.nextInt(12),
                            0.22F + random.nextFloat() * 0.18F)
                        .color(SILVER_R, SILVER_G, SILVER_B).fadeTo(TEAL_R, TEAL_G, TEAL_B).drag(0.93D).gravity(-0.002D)
                        .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.4F);
                }
                for (int i = 0; i < 14; i++) {
                    FxParticles.glow(FxTextures.MOTE, knight.getX() + (random.nextDouble() - 0.5D), knight.getY() + random.nextDouble() * height,
                            knight.getZ() + (random.nextDouble() - 0.5D), 0.0D, 0.04D + random.nextDouble() * 0.04D, 0.0D, 30 + random.nextInt(14), 0.2F)
                        .color(TEAL_R, TEAL_G, TEAL_B).drag(0.97D).flicker(0.4F, random.nextFloat() * 10.0F);
                }
            }
        }
        for (int id : AXES) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof ThrownWep wep) || !wep.getItem().is(TFItems.KNIGHTMETAL_AXE.get())) {
                continue;
            }
            List<Vec3> trail = AXE_TRAILS.computeIfAbsent(id, key -> new ArrayList<>());
            trail.add(0, wep.position());
            while (trail.size() > AXE_TRAIL) {
                trail.remove(trail.size() - 1);
            }
        }
        Iterator<ChargeLine> lines = LINES.iterator();
        while (lines.hasNext()) {
            ChargeLine line = lines.next();
            line.age++;
            if (line.age > line.warn + line.dash + LINE_FADE) {
                lines.remove();
            }
        }
        RINGS.removeIf(ring -> ++ring.age > RING_LIFE);
        AFTERIMAGES.removeIf(image -> ++image.age > AFTERIMAGE_LIFE);
        THRUSTS.removeIf(thrust -> ++thrust.age > THRUST_LIFE);
        REGROUPS.removeIf(regroup -> ++regroup.age > regroup.duration + 10);
        if (encircle != null) {
            encircle.age++;
            if (encircle.age > encircle.warn + encircle.shrink + encircle.thrust + 40) {
                encircle = null;
            } else if (decorative && encircle.age >= encircle.warn && encircle.age % 3 == 0) {
                int count = encircle.ids.length;
                for (int i = 0; i < count && count >= 2; i++) {
                    if (i == encircle.open) {
                        continue;
                    }
                    Entity a = level.getEntity(encircle.ids[i]);
                    Entity b = level.getEntity(encircle.ids[(i + 1) % count]);
                    if (a == null || b == null || !a.isAlive() || !b.isAlive()) {
                        continue;
                    }
                    double t = random.nextDouble();
                    FxParticles.glow(FxTextures.MOTE, Mth.lerp(t, a.getX(), b.getX()), encircle.y + 0.2D, Mth.lerp(t, a.getZ(), b.getZ()),
                            0.0D, 0.12D + random.nextDouble() * 0.06D, 0.0D, 22, 0.2F)
                        .color(SILVER_R, SILVER_G, SILVER_B).fadeTo(TEAL_R, TEAL_G, TEAL_B).drag(0.98D);
                }
            }
        }
        Iterator<ClientEcho> echoes = ECHOES.values().iterator();
        while (echoes.hasNext()) {
            ClientEcho echo = echoes.next();
            echo.age++;
            echo.prevPos = echo.pos;
            if (echo.follow >= 0) {
                Entity follow = level.getEntity(echo.follow);
                if (follow instanceof KnightPhantom knight) {
                    float yaw = knight.yBodyRot * Mth.DEG_TO_RAD;
                    echo.pos = new Vec3(knight.getX() + Mth.cos(yaw) * echo.side, knight.getY(), knight.getZ() + Mth.sin(yaw) * echo.side);
                    echo.yaw = knight.yBodyRot;
                } else if (echo.fadeAge < 0) {
                    echo.fadeAge = 0;
                }
            } else if (echo.start != null && echo.end != null) {
                echo.dashAge++;
                if (echo.dashAge > echo.delay) {
                    double length = echo.start.distanceTo(echo.end);
                    double progress = 1.0D;
                    if (length > 1.0E-3D) {
                        progress = Math.min(1.0D, (echo.dashAge - echo.delay) * echo.speed / length);
                    }
                    echo.pos = echo.start.add(echo.end.subtract(echo.start).scale(progress));
                    if (decorative && progress < 1.0D && echo.dashAge % 2 == 0) {
                        addAfterimage(echo.pos, echo.yaw, true);
                    }
                    if (progress >= 1.0D && echo.fadeAge < 0) {
                        echo.fadeAge = 0;
                    }
                }
            }
            if (echo.fadeAge >= 0) {
                echo.fadeAge++;
            }
            if (echo.fadeAge > ECHO_FADE || echo.age > 600) {
                echoes.remove();
            }
        }
    }

    private static void addAfterimage(Vec3 pos, float yaw, boolean echo) {
        AFTERIMAGES.add(new Afterimage(pos, yaw, echo));
        int count = 0;
        for (int i = AFTERIMAGES.size() - 1; i >= 0; i--) {
            count++;
            if (count > AFTERIMAGE_MAX * 8) {
                AFTERIMAGES.remove(i);
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
        if (level == null || !KnightPhantomConfig.ENABLED.get()) {
            return;
        }
        if (KNIGHTS.isEmpty() && ECHOES.isEmpty() && LINES.isEmpty() && THRUSTS.isEmpty() && encircle == null && REGROUPS.isEmpty() && AFTERIMAGES.isEmpty()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        float partialTick = event.getPartialTick();
        Camera camera = event.getCamera();
        FxDraw.camera(camera);
        Vec3 cameraPos = camera.getPosition();
        Vector3f left = camera.getLeftVector();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        PoseStack.Pose pose = poseStack.last();

        for (RegroupFx regroup : REGROUPS) {
            renderRegroup(pose, buffers, regroup, partialTick);
        }
        for (ChargeLine line : LINES) {
            renderLine(pose, buffers, line, partialTick);
        }
        for (int id : KNIGHTS) {
            if (level.getEntity(id) instanceof KnightPhantom knight && knight.isAlive()) {
                renderKnight(poseStack, buffers, level, knight, track(id), left, partialTick);
            }
        }
        if (encircle != null) {
            renderEncircle(pose, buffers, level, encircle, partialTick);
        }
        for (PickRing ring : RINGS) {
            renderPickRing(pose, buffers, ring, partialTick);
        }
        for (ClientEcho echo : ECHOES.values()) {
            renderEcho(poseStack, buffers, echo, partialTick);
        }
        if (decorative) {
            for (Afterimage image : AFTERIMAGES) {
                float life = 1.0F - (image.age + partialTick) / AFTERIMAGE_LIFE;
                if (life <= 0.0F) {
                    continue;
                }
                float r = Mth.lerp(1.0F - life, SILVER_R, TEAL_R);
                float g = Mth.lerp(1.0F - life, SILVER_G, TEAL_G);
                float b = Mth.lerp(1.0F - life, SILVER_B, TEAL_B);
                if (image.echo) {
                    r = Mth.lerp(life, SHADOW_R, TEAL_R);
                    g = Mth.lerp(life, SHADOW_G, TEAL_G);
                    b = Mth.lerp(life, SHADOW_B, TEAL_B);
                }
                renderGhost(poseStack, buffers, image.pos.x, image.pos.y, image.pos.z, image.yaw, CHARGE_SCALE, r, g, b, 0.4F * life, true);
            }
            for (ThrustFx thrust : THRUSTS) {
                renderThrust(pose, buffers, thrust, partialTick);
            }
            renderAxeTrails(pose, buffers, level, partialTick);
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderGhost(PoseStack poseStack, MultiBufferSource buffers, double x, double y, double z, float yaw, float scale,
                                    float r, float g, float b, float alpha, boolean glow) {
        if (alpha <= 0.01F) {
            return;
        }
        VertexConsumer consumer;
        if (glow) {
            consumer = buffers.getBuffer(FxRenderTypes.glow(GHOST));
        } else {
            consumer = buffers.getBuffer(FxRenderTypes.soft(GHOST));
        }
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(scale, scale, scale);
        ghostMesh(poseStack, consumer, 0.0F, 0.0F, 0.0F, r, g, b, alpha);
        poseStack.popPose();
    }

    private static void renderShell(PoseStack poseStack, MultiBufferSource buffers, KnightPhantom knight, float partialTick, float alpha) {
        float age = knight.tickCount + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, knight.yBodyRotO, knight.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, knight.yHeadRotO, knight.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(partialTick, knight.xRotO, knight.getXRot());
        double x = Mth.lerp(partialTick, knight.xo, knight.getX());
        double y = Mth.lerp(partialTick, knight.yo, knight.getY());
        double z = Mth.lerp(partialTick, knight.zo, knight.getZ());
        float scale = CHARGE_SCALE * 1.06F;
        VertexConsumer consumer = buffers.getBuffer(FxRenderTypes.glow(GHOST));
        poseStack.pushPose();
        poseStack.translate(x, y - 0.06D, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(scale, scale, scale);
        ghostMesh(poseStack, consumer, age, headYaw, pitch, TEAL_R, TEAL_G, TEAL_B, alpha);
        poseStack.popPose();
    }

    private static void ghostMesh(PoseStack poseStack, VertexConsumer consumer, float age, float headYaw, float headPitch,
                                  float r, float g, float b, float a) {
        int light = FxDraw.FULL_BRIGHT;
        float sway = 0.04F * Mth.sin(age * 0.3F);
        FxDraw.box(consumer, poseStack.last(), -0.2F + sway * 0.5F, 0.5F, -0.12F, 0.2F + sway * 0.5F, 0.72F, 0.12F,
            0.0F, 0.5F, 0.5F, 0.68F, r, g, b, a, light);
        FxDraw.box(consumer, poseStack.last(), -0.14F + sway, 0.25F, -0.09F, 0.14F + sway, 0.5F, 0.09F,
            0.0F, 0.68F, 0.5F, 0.85F, r, g, b, a, light);
        FxDraw.box(consumer, poseStack.last(), -0.08F + sway * 2.0F, 0.0F, -0.05F, 0.08F + sway * 2.0F, 0.25F, 0.05F,
            0.0F, 0.85F, 0.5F, 1.0F, r, g, b, a, light);
        FxDraw.box(consumer, poseStack.last(), -0.24F, 0.72F, -0.14F, 0.24F, 0.86F, 0.14F, 0.5F, 0.5F, 1.0F, 1.0F, r, g, b, a, light);
        FxDraw.box(consumer, poseStack.last(), -0.27F, 0.89F, -0.16F, 0.27F, 1.42F, 0.15F, 0.0F, 0.0F, 0.5F, 0.5F, r, g, b, a, light);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.47F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch));
        FxDraw.box(consumer, poseStack.last(), -0.2F, 0.0F, -0.2F, 0.2F, 0.42F, 0.2F, 0.0F, 0.0F, 0.5F, 0.5F, r, g, b, a, light);
        FxDraw.box(consumer, poseStack.last(), -0.17F, 0.08F, -0.218F, 0.17F, 0.34F, -0.2F, 0.5F, 0.0F, 1.0F, 0.5F, r, g, b, a, light);
        FxDraw.box(consumer, poseStack.last(), -0.025F, 0.4F, -0.14F, 0.025F, 0.6F, 0.22F, 0.0F, 0.0F, 0.12F, 0.5F, r, g, b, a, light);
        poseStack.popPose();
        for (int i = 0; i < 2; i++) {
            float side = -1.0F;
            float reach = 0.2F;
            if (i == 1) {
                side = 1.0F;
                reach = 0.35F;
            }
            poseStack.pushPose();
            poseStack.translate(side * 0.37F, 1.4F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotation(-side * 0.3F));
            FxDraw.box(consumer, poseStack.last(), -0.14F, -0.07F, -0.18F, 0.14F, 0.09F, 0.18F, 0.0F, 0.0F, 0.5F, 0.25F, r, g, b, a, light);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(side * 0.4F, 1.3F, 0.0F);
            poseStack.mulPose(Axis.XP.rotation(reach + sway));
            FxDraw.box(consumer, poseStack.last(), -0.08F, -0.5F, -0.08F, 0.08F, -0.1F, 0.08F, 0.5F, 0.5F, 1.0F, 1.0F, r, g, b, a, light);
            FxDraw.box(consumer, poseStack.last(), -0.1F, -0.64F, -0.1F, 0.1F, -0.5F, 0.1F, 0.0F, 0.25F, 0.5F, 0.5F, r, g, b, a, light);
            poseStack.popPose();
        }
    }

    private static void cornerPillars(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float half, float angle, float height,
                                      float r, float g, float b, float alpha) {
        for (int k = 0; k < 4; k++) {
            float a = angle + Mth.HALF_PI * 0.5F + k * Mth.HALF_PI;
            double cx = x + Mth.cos(a) * half * Mth.SQRT_OF_TWO;
            double cz = z + Mth.sin(a) * half * Mth.SQRT_OF_TWO;
            FxDraw.beam(consumer, pose, new Vec3(cx, y, cz), new Vec3(cx, y + height, cz), 0.09F, 0.02F, 0.0F, 1.0F, r, g, b, alpha, 0.0F);
        }
    }

    private static void renderKnight(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, KnightPhantom knight, KnightTrack track,
                                     Vector3f left, float partialTick) {
        PoseStack.Pose pose = poseStack.last();
        double x = Mth.lerp(partialTick, knight.xo, knight.getX());
        double y = Mth.lerp(partialTick, knight.yo, knight.getY());
        double z = Mth.lerp(partialTick, knight.zo, knight.getZ());
        float age = track.age + partialTick;
        if (knight.isChargingAtPlayer()) {
            double ground = groundY(level, x, y + 1.0D, z, y);
            float pulse = 0.65F + 0.2F * Mth.sin(age * 0.25F);
            float outerAngle = clickAngle(age, 10.0F);
            VertexConsumer frame = buffers.getBuffer(FxRenderTypes.glow(FRAME));
            FxDraw.flat(frame, pose, x, ground + 0.05D, z, 1.4F, outerAngle, TEAL_R, TEAL_G, TEAL_B, pulse);
            FxDraw.flat(frame, pose, x, ground + 0.06D, z, 0.9F, clickAngle(age + 5.0F, 10.0F), SILVER_R, SILVER_G, SILVER_B, pulse * 0.6F);
            VertexConsumer pillars = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            cornerPillars(pillars, pose, x, ground + 0.05D, z, 1.2F, outerAngle, 1.6F, TEAL_R, TEAL_G, TEAL_B, pulse * 0.8F);
            renderShell(poseStack, buffers, knight, partialTick, 0.28F + 0.08F * Mth.sin(age * 0.3F));
        }
        if (track.trackTarget >= 0 && level.getEntity(track.trackTarget) instanceof LivingEntity target) {
            double tx = Mth.lerp(partialTick, target.xo, target.getX());
            double ty = Mth.lerp(partialTick, target.yo, target.getY());
            double tz = Mth.lerp(partialTick, target.zo, target.getZ());
            double ground = groundY(level, tx, ty + 1.0D, tz, ty);
            VertexConsumer strip = buffers.getBuffer(FxRenderTypes.glow(SWORD_LINE));
            renderStrip(strip, pose, x, z, tx, tz, ground + 0.04D, 0.7F, SILVER_R, SILVER_G, SILVER_B, 0.4F, age * 0.05F, true);
        }
        if (track.glow > 0) {
            float t = 1.0F - (track.glow - partialTick) / track.glowMax;
            float yaw = Mth.rotLerp(partialTick, knight.yBodyRotO, knight.yBodyRot) * Mth.DEG_TO_RAD;
            double hx = x - Mth.cos(yaw) * 0.55D * CHARGE_SCALE - Mth.sin(yaw) * 0.35D;
            double hz = z - Mth.sin(yaw) * 0.55D * CHARGE_SCALE + Mth.cos(yaw) * 0.35D;
            double hy = y + knight.getBbHeight() * 0.55D;
            VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            FxDraw.sprite(glow, pose, hx, hy, hz, 1.0F + 0.8F * t, 0.0F, TEAL_R, TEAL_G, TEAL_B, 0.3F + 0.4F * t);
            VertexConsumer glint = buffers.getBuffer(FxRenderTypes.glow(GLINT));
            FxDraw.sprite(glint, pose, hx, hy, hz, 0.8F + 1.0F * t, clickAngle(age, 6.0F), SILVER_R, SILVER_G, SILVER_B, 0.5F + 0.5F * t);
        }
        if (track.oath > 0) {
            double top = y + knight.getBbHeight() + 0.7D;
            VertexConsumer oath = buffers.getBuffer(FxRenderTypes.glow(OATH));
            FxDraw.sprite(oath, pose, x, top, z, 0.64F, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F);
            VertexConsumer gems = buffers.getBuffer(FxRenderTypes.glow(GEM));
            float spacing = 0.26F;
            float start = -(track.oath - 1) * spacing * 0.5F;
            for (int i = 0; i < track.oath; i++) {
                float off = -(start + i * spacing);
                FxDraw.sprite(gems, pose, x + left.x() * off, top - 0.45D + left.y() * off, z + left.z() * off, 0.2F, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F);
            }
        }
    }

    private static void renderStrip(VertexConsumer consumer, PoseStack.Pose pose, double x0, double z0, double x1, double z1, double y, float width,
                                    float r, float g, float b, float alpha, float scroll, boolean dashed) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.1D || alpha <= 0.01F) {
            return;
        }
        double nx = -dz / length * width * 0.5D;
        double nz = dx / length * width * 0.5D;
        int segments = Math.max(1, Mth.ceil(length));
        for (int i = 0; i < segments; i++) {
            if (dashed && i % 2 == 1) {
                continue;
            }
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;
            float u0 = (float) (i * length / segments) - scroll;
            float u1 = (float) ((i + 1) * length / segments) - scroll;
            double ax = x0 + dx * t0;
            double az = z0 + dz * t0;
            double bx = x0 + dx * t1;
            double bz = z0 + dz * t1;
            FxDraw.glowVertex(consumer, pose, ax + nx, y, az + nz, u0, 0.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, ax - nx, y, az - nz, u0, 1.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, bx - nx, y, bz - nz, u1, 1.0F, r, g, b, alpha);
            FxDraw.glowVertex(consumer, pose, bx + nx, y, bz + nz, u1, 0.0F, r, g, b, alpha);
        }
    }

    private static void renderLine(PoseStack.Pose pose, MultiBufferSource buffers, ChargeLine line, float partialTick) {
        float age = line.age + partialTick;
        float alpha = 0.85F;
        if (line.dim) {
            alpha = 0.5F;
            if (age > line.warn - 12) {
                alpha = 0.85F;
            }
        }
        float end = line.warn + line.dash;
        if (age > end) {
            alpha *= Mth.clamp(1.0F - (age - end) / LINE_FADE, 0.0F, 1.0F);
        }
        float r = SILVER_R;
        float g = SILVER_G;
        float b = SILVER_B;
        float flash = 0.0F;
        if (age < LINE_FLASH) {
            flash = 1.0F - age / LINE_FLASH;
            alpha = Math.min(1.0F, alpha + flash * 0.4F);
            r = Mth.lerp(flash, r, 1.0F);
            g = Mth.lerp(flash, g, 1.0F);
            b = Mth.lerp(flash, b, 1.0F);
        }
        float width = 1.1F + 0.4F * flash;
        VertexConsumer under = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        renderStrip(under, pose, line.sx, line.sz, line.ex, line.ez, line.y + 0.04D, width * 1.9F, TEAL_R, TEAL_G, TEAL_B, alpha * 0.35F, 0.0F, false);
        VertexConsumer strip = buffers.getBuffer(FxRenderTypes.glow(SWORD_LINE));
        renderStrip(strip, pose, line.sx, line.sz, line.ex, line.ez, line.y + 0.05D, width, r, g, b, alpha, age * 0.12F, false);
        boolean dashing = age > line.warn && age < end;
        double px = line.ex;
        double pz = line.ez;
        if (dashing) {
            float progress = Mth.clamp((age - line.warn) / line.dash, 0.0F, 1.0F);
            px = Mth.lerp(progress, line.sx, line.ex);
            pz = Mth.lerp(progress, line.sz, line.ez);
            VertexConsumer fill = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            renderStrip(fill, pose, line.sx, line.sz, px, pz, line.y + 0.06D, width * 0.7F, TEAL_R, TEAL_G, TEAL_B, alpha, 0.0F, false);
        }
        if (flash > 0.0F || dashing) {
            VertexConsumer glint = buffers.getBuffer(FxRenderTypes.glow(GLINT));
            if (flash > 0.0F) {
                FxDraw.sprite(glint, pose, line.sx, line.y + 0.4D, line.sz, 1.6F * flash + 0.6F, 0.0F, 1.0F, 1.0F, 1.0F, flash);
                FxDraw.sprite(glint, pose, line.ex, line.y + 0.4D, line.ez, 1.6F * flash + 0.6F, 0.0F, 1.0F, 1.0F, 1.0F, flash);
            }
            if (dashing) {
                FxDraw.sprite(glint, pose, px, line.y + 0.3D, pz, 1.2F, clickAngle(age, 4.0F), TEAL_R, TEAL_G, TEAL_B, alpha);
            }
        }
    }

    private static void renderPickRing(PoseStack.Pose pose, MultiBufferSource buffers, PickRing ring, float partialTick) {
        float life = 1.0F - (ring.age + partialTick) / RING_LIFE;
        if (life <= 0.0F) {
            return;
        }
        float step = Mth.TWO_PI / ring.count;
        float inner = 0.9F;
        float outer = 1.9F + (1.0F - life) * 0.8F;
        VertexConsumer consumer = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        for (int i = 0; i < ring.count; i++) {
            float angle = ring.base + step * i;
            float cx = Mth.cos(angle);
            float cz = Mth.sin(angle);
            float nx = -cz * 0.14F;
            float nz = cx * 0.14F;
            FxDraw.glowVertex(consumer, pose, ring.x + cx * inner + nx, ring.y, ring.z + cz * inner + nz, 0.0F, 0.0F, SILVER_R, SILVER_G, SILVER_B, life);
            FxDraw.glowVertex(consumer, pose, ring.x + cx * inner - nx, ring.y, ring.z + cz * inner - nz, 0.0F, 1.0F, SILVER_R, SILVER_G, SILVER_B, life);
            FxDraw.glowVertex(consumer, pose, ring.x + cx * outer - nx, ring.y, ring.z + cz * outer - nz, 1.0F, 1.0F, TEAL_R, TEAL_G, TEAL_B, life);
            FxDraw.glowVertex(consumer, pose, ring.x + cx * outer + nx, ring.y, ring.z + cz * outer + nz, 1.0F, 0.0F, TEAL_R, TEAL_G, TEAL_B, life);
        }
        VertexConsumer tips = buffers.getBuffer(FxRenderTypes.glow(GLINT));
        for (int i = 0; i < ring.count; i++) {
            float angle = ring.base + step * i;
            FxDraw.flat(tips, pose, ring.x + Mth.cos(angle) * outer, ring.y + 0.01D, ring.z + Mth.sin(angle) * outer, 0.35F, angle, SILVER_R, SILVER_G, SILVER_B, life);
        }
    }

    private static void renderEncircle(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, EncircleFx ring, float partialTick) {
        float age = ring.age + partialTick;
        int count = ring.ids.length;
        if (count < 2) {
            return;
        }
        boolean solid = age >= ring.warn;
        float alpha = 0.9F;
        if (!solid) {
            alpha = 0.4F + 0.3F * Math.abs(Mth.sin(age * 0.3F));
        }
        float end = ring.warn + ring.shrink + ring.thrust;
        if (age > end) {
            alpha *= Mth.clamp(1.0F - (age - end) / 6.0F, 0.0F, 1.0F);
        }
        float[] wall = new float[count * 5];
        boolean[] valid = new boolean[count];
        for (int i = 0; i < count; i++) {
            Entity a = level.getEntity(ring.ids[i]);
            Entity b = level.getEntity(ring.ids[(i + 1) % count]);
            if (!(a instanceof KnightPhantom ka) || !(b instanceof KnightPhantom kb) || !ka.isAlive() || !kb.isAlive()) {
                continue;
            }
            valid[i] = true;
            wall[i * 5] = (float) Mth.lerp(partialTick, a.xo, a.getX());
            wall[i * 5 + 1] = (float) Mth.lerp(partialTick, a.zo, a.getZ());
            wall[i * 5 + 2] = (float) Mth.lerp(partialTick, b.xo, b.getX());
            wall[i * 5 + 3] = (float) Mth.lerp(partialTick, b.zo, b.getZ());
            float dx = wall[i * 5 + 2] - wall[i * 5];
            float dz = wall[i * 5 + 3] - wall[i * 5 + 1];
            wall[i * 5 + 4] = (float) Math.sqrt(dx * dx + dz * dz);
        }
        float y0 = (float) ring.y;
        float y1 = y0 + WALL_HEIGHT;
        VertexConsumer base = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        for (int i = 0; i < count; i++) {
            if (!valid[i]) {
                continue;
            }
            if (i == ring.open) {
                renderStrip(base, pose, wall[i * 5], wall[i * 5 + 1], wall[i * 5 + 2], wall[i * 5 + 3], y0 + 0.04D, 0.6F, GOLD_R, GOLD_G, GOLD_B, alpha * 0.6F, 0.0F, true);
            } else {
                renderStrip(base, pose, wall[i * 5], wall[i * 5 + 1], wall[i * 5 + 2], wall[i * 5 + 3], y0 + 0.04D, 0.9F, TEAL_R, TEAL_G, TEAL_B, alpha * 0.7F, 0.0F, false);
            }
        }
        ResourceLocation swordTexture = SWORD_WALL_DASH;
        if (solid) {
            swordTexture = SWORD_WALL;
        }
        VertexConsumer swords = buffers.getBuffer(FxRenderTypes.glow(swordTexture));
        for (int i = 0; i < count; i++) {
            if (!valid[i] || i == ring.open) {
                continue;
            }
            float ax = wall[i * 5];
            float az = wall[i * 5 + 1];
            float bx = wall[i * 5 + 2];
            float bz = wall[i * 5 + 3];
            float u1 = Math.max(1.0F, wall[i * 5 + 4] / 2.0F);
            FxDraw.glowVertex(swords, pose, ax, y1, az, 0.0F, 0.0F, TEAL_R, TEAL_G, TEAL_B, alpha * 0.8F);
            FxDraw.glowVertex(swords, pose, ax, y0, az, 0.0F, 1.0F, SILVER_R, SILVER_G, SILVER_B, alpha);
            FxDraw.glowVertex(swords, pose, bx, y0, bz, u1, 1.0F, SILVER_R, SILVER_G, SILVER_B, alpha);
            FxDraw.glowVertex(swords, pose, bx, y1, bz, u1, 0.0F, TEAL_R, TEAL_G, TEAL_B, alpha * 0.8F);
        }
        if (solid) {
            float drift = age * 0.02F;
            float shimmer = 0.75F + 0.25F * Mth.sin(age * 0.4F);
            VertexConsumer light = buffers.getBuffer(FxRenderTypes.glow(FxTextures.STREAKS));
            for (int i = 0; i < count; i++) {
                if (!valid[i] || i == ring.open) {
                    continue;
                }
                float ax = wall[i * 5];
                float az = wall[i * 5 + 1];
                float bx = wall[i * 5 + 2];
                float bz = wall[i * 5 + 3];
                float u1 = wall[i * 5 + 4] / 3.0F;
                float a = alpha * 0.55F * shimmer;
                FxDraw.glowVertex(light, pose, ax, y1, az, drift, 0.0F, TEAL_R, TEAL_G, TEAL_B, a);
                FxDraw.glowVertex(light, pose, ax, y0, az, drift, 1.0F, TEAL_R, TEAL_G, TEAL_B, a);
                FxDraw.glowVertex(light, pose, bx, y0, bz, drift + u1, 1.0F, TEAL_R, TEAL_G, TEAL_B, a);
                FxDraw.glowVertex(light, pose, bx, y1, bz, drift + u1, 0.0F, TEAL_R, TEAL_G, TEAL_B, a);
            }
        }
        float thrustStart = ring.warn + ring.shrink - THRUST_PREVIEW;
        if (age >= thrustStart && age <= ring.warn + ring.shrink + ring.thrust) {
            float t = Mth.clamp((age - thrustStart) / THRUST_PREVIEW, 0.0F, 1.0F);
            float radius = ring.thrustRadius;
            if (age > ring.warn + ring.shrink) {
                radius *= 1.0F - 0.6F * Mth.clamp((age - ring.warn - ring.shrink) / Math.max(1, ring.thrust), 0.0F, 1.0F);
            }
            VertexConsumer decal = buffers.getBuffer(FxRenderTypes.glow(THRUST));
            FxDraw.flat(decal, pose, ring.x, ring.y + 0.07D, ring.z, radius, 0.0F, SILVER_R, SILVER_G, SILVER_B, 0.9F * t);
        }
    }

    private static void renderThrust(PoseStack.Pose pose, MultiBufferSource buffers, ThrustFx thrust, float partialTick) {
        float t = (thrust.age + partialTick) / THRUST_LIFE;
        if (t >= 1.0F) {
            return;
        }
        float alpha = 1.0F - t;
        VertexConsumer decal = buffers.getBuffer(FxRenderTypes.glow(THRUST));
        FxDraw.flat(decal, pose, thrust.x, thrust.y + 0.08D, thrust.z, thrust.radius * (0.4F + t * 0.9F), t * 0.6F, 1.0F, 1.0F, 1.0F, alpha);
        VertexConsumer glint = buffers.getBuffer(FxRenderTypes.glow(GLINT));
        FxDraw.sprite(glint, pose, thrust.x, thrust.y + 1.0D, thrust.z, thrust.radius * 2.4F * (1.0F - t * 0.4F), 0.0F, 1.0F, 1.0F, 1.0F, alpha);
        VertexConsumer pillar = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.beam(pillar, pose, new Vec3(thrust.x, thrust.y, thrust.z), new Vec3(thrust.x, thrust.y + 5.0D, thrust.z), 0.5F * alpha, 0.05F, 0.0F, 1.0F,
            SILVER_R, SILVER_G, SILVER_B, alpha, 0.0F);
    }

    private static void renderEcho(PoseStack poseStack, MultiBufferSource buffers, ClientEcho echo, float partialTick) {
        float alpha = Mth.clamp((echo.age + partialTick) / ECHO_RISE, 0.0F, 1.0F);
        if (echo.fadeAge >= 0) {
            alpha *= Mth.clamp(1.0F - (echo.fadeAge + partialTick) / ECHO_FADE, 0.0F, 1.0F);
        }
        double rise = (1.0F - Mth.clamp((echo.age + partialTick) / ECHO_RISE, 0.0F, 1.0F)) * 1.0D;
        double x = Mth.lerp(partialTick, echo.prevPos.x, echo.pos.x);
        double y = Mth.lerp(partialTick, echo.prevPos.y, echo.pos.y) - rise;
        double z = Mth.lerp(partialTick, echo.prevPos.z, echo.pos.z);
        renderGhost(poseStack, buffers, x, y, z, echo.yaw, CHARGE_SCALE, SHADOW_R + 0.25F, SHADOW_G + 0.3F, SHADOW_B + 0.32F, 0.65F * alpha, false);
        renderGhost(poseStack, buffers, x, y - 0.06D, z, echo.yaw, CHARGE_SCALE * 1.06F, TEAL_R, TEAL_G, TEAL_B, 0.35F * alpha, true);
    }

    private static float clickAngle(float age, float period) {
        int step = Mth.floor(age / period);
        float ease = Mth.clamp((age - step * period) / 3.0F, 0.0F, 1.0F);
        ease = ease * ease * (3.0F - 2.0F * ease);
        return (step + ease) * Mth.HALF_PI * 0.5F;
    }

    private static void renderRegroup(PoseStack.Pose pose, MultiBufferSource buffers, RegroupFx regroup, float partialTick) {
        float age = regroup.age + partialTick;
        float t = Mth.clamp(age / regroup.duration, 0.0F, 1.0F);
        float alpha = 0.85F;
        if (age > regroup.duration) {
            alpha *= Mth.clamp(1.0F - (age - regroup.duration) / 10.0F, 0.0F, 1.0F);
        }
        float size = Mth.lerp(t, 4.0F, 1.8F);
        float angle = clickAngle(age, 8.0F);
        VertexConsumer frame = buffers.getBuffer(FxRenderTypes.glow(FRAME));
        FxDraw.flat(frame, pose, regroup.x, regroup.y + 0.05D, regroup.z, size, angle, SILVER_R, SILVER_G, SILVER_B, alpha);
        FxDraw.flat(frame, pose, regroup.x, regroup.y + 0.06D, regroup.z, size * 0.7F, clickAngle(age + 4.0F, 8.0F), TEAL_R, TEAL_G, TEAL_B, alpha * 0.7F);
        VertexConsumer pillars = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        cornerPillars(pillars, pose, regroup.x, regroup.y + 0.05D, regroup.z, size * 0.85F, angle, 1.0F + 2.5F * t, SILVER_R, SILVER_G, SILVER_B, alpha * 0.8F);
    }

    private static void renderAxeTrails(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, float partialTick) {
        if (AXE_TRAILS.isEmpty()) {
            return;
        }
        VertexConsumer ribbon = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        for (Map.Entry<Integer, List<Vec3>> entry : AXE_TRAILS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            List<Vec3> trail = entry.getValue();
            if (entity == null || trail.size() < 2) {
                continue;
            }
            int count = trail.size();
            Vec3[] points = new Vec3[count];
            float[] widths = new float[count];
            float[] alphas = new float[count];
            for (int i = 0; i < count; i++) {
                points[i] = trail.get(count - 1 - i).add(0.0D, 0.15D, 0.0D);
                float f = (float) i / (count - 1);
                widths[i] = 0.05F + 0.3F * f;
                alphas[i] = 0.7F * f;
            }
            FxDraw.trail(ribbon, pose, points, count, widths, alphas, 0.0F, 0.0F, TEAL_R, TEAL_G, TEAL_B);
        }
        VertexConsumer arcs = buffers.getBuffer(FxRenderTypes.glow(ARC));
        for (Map.Entry<Integer, List<Vec3>> entry : AXE_TRAILS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            List<Vec3> trail = entry.getValue();
            for (int i = 0; i < Math.min(3, trail.size()); i++) {
                Vec3 pos = trail.get(i);
                float fade = 1.0F - i / 3.0F;
                FxDraw.sprite(arcs, pose, pos.x, pos.y + 0.15D, pos.z, 0.9F * fade + 0.3F, (entity.tickCount + partialTick - i) * 0.7F,
                    SILVER_R, SILVER_G, SILVER_B, 0.75F * fade);
            }
        }
    }

    private static final class KnightTrack {
        private int age;
        private int trackTarget = -1;
        private int trackAge;
        private int glow;
        private int glowMax = 1;
        private int oath;
        private boolean shattered;
        @Nullable
        private Vec3 lastPos;
    }

    private static final class ChargeLine {
        private final double sx;
        private final double sz;
        private final double ex;
        private final double ez;
        private final double y;
        private final int warn;
        private final boolean dim;
        private final int dash;
        private int age;

        private ChargeLine(double sx, double sy, double sz, double ex, double ey, double ez, int warn, boolean dim, int dash, double y) {
            this.sx = sx;
            this.sz = sz;
            this.ex = ex;
            this.ez = ez;
            this.warn = warn;
            this.dim = dim;
            this.dash = dash;
            this.y = y;
        }
    }

    private static final class PickRing {
        private final double x;
        private final double y;
        private final double z;
        private final float base;
        private final int count;
        private int age;

        private PickRing(double x, double y, double z, float base, int count) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.base = base;
            this.count = count;
        }
    }

    private static final class Afterimage {
        private final Vec3 pos;
        private final float yaw;
        private final boolean echo;
        private int age;

        private Afterimage(Vec3 pos, float yaw, boolean echo) {
            this.pos = pos;
            this.yaw = yaw;
            this.echo = echo;
        }
    }

    private static final class ClientEcho {
        private Vec3 pos;
        private Vec3 prevPos;
        private float yaw;
        private int follow;
        private final double side;
        @Nullable
        private Vec3 start;
        @Nullable
        private Vec3 end;
        private int delay;
        private float speed = 0.9F;
        private int dashAge;
        private int age;
        private int fadeAge = -1;

        private ClientEcho(double x, double y, double z, float yaw, int follow, double side) {
            this.pos = new Vec3(x, y, z);
            this.prevPos = this.pos;
            this.yaw = yaw;
            this.follow = follow;
            this.side = side;
        }
    }

    private static final class EncircleFx {
        private final double x;
        private final double y;
        private final double z;
        private final float startRadius;
        private final float endRadius;
        private final int warn;
        private final int shrink;
        private final int thrust;
        private final int open;
        private final float thrustRadius;
        private final int[] ids;
        private int age;

        private EncircleFx(double x, double y, double z, float startRadius, float endRadius, int warn, int shrink, int thrust, int open, float thrustRadius, int[] ids) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.warn = warn;
            this.shrink = shrink;
            this.thrust = thrust;
            this.open = open;
            this.thrustRadius = thrustRadius;
            this.ids = ids;
        }
    }

    private static final class ThrustFx {
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private int age;

        private ThrustFx(double x, double y, double z, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
    }

    private static final class RegroupFx {
        private final double x;
        private final double y;
        private final double z;
        private final int duration;
        private int age;

        private RegroupFx(double x, double y, double z, int duration) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.duration = duration;
        }
    }
}
