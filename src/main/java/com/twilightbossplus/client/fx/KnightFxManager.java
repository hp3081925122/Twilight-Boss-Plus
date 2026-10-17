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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
import org.joml.Quaternionf;
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
    private static final int SHARD_LIFE = 30;
    private static final int SHARD_COUNT = 20;
    private static final int AXE_TRAIL = 5;
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
    private static final List<Shard> SHARDS = new ArrayList<>();
    private static final List<RegroupFx> REGROUPS = new ArrayList<>();
    @Nullable
    private static EncircleFx encircle;

    private KnightFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/knight/" + name + ".png");
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
        if (type == KnightFxPacket.TRACK && data.length >= 1) {
            KnightTrack track = track(entityId);
            track.trackTarget = (int) data[0];
            track.trackAge = 0;
        } else if (type == KnightFxPacket.LINE && data.length >= 9) {
            if (entityId >= 0) {
                track(entityId).trackTarget = -1;
            }
            LINES.add(new ChargeLine(data[0], data[1], data[2], data[3], data[4], data[5], (int) data[6], data[7] > 0.5F, Math.max(1, (int) data[8]),
                groundY(level, (data[0] + data[3]) * 0.5D, Math.max(data[1], data[4]), (data[2] + data[5]) * 0.5D, Math.min(data[1], data[4]))));
        } else if (type == KnightFxPacket.AXE_GLOW && data.length >= 1) {
            KnightTrack track = track(entityId);
            track.glowMax = Math.max(1, (int) data[0]);
            track.glow = track.glowMax;
        } else if (type == KnightFxPacket.PICK_RING && data.length >= 5) {
            RINGS.add(new PickRing(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4])));
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
            THRUSTS.add(new ThrustFx(data[0], groundY(level, data[0], data[1] + 1.0D, data[2], data[1]), data[2], data[3]));
        } else if (type == KnightFxPacket.ECHO_SPAWN && data.length >= 6) {
            ClientEcho echo = new ClientEcho(data[0], data[1], data[2], data[3], (int) data[4], data[5]);
            ECHOES.put(entityId, echo);
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
        SHARDS.clear();
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
            if (decorative && knight.isChargingAtPlayer() && track.lastPos != null && pos.distanceToSqr(track.lastPos) > 0.35D * 0.35D && track.age % 2 == 0) {
                addAfterimage(pos, knight.yBodyRot, false);
            }
            track.lastPos = pos;
            if (knight.deathTime == 1 && !track.shattered) {
                track.shattered = true;
                if (decorative) {
                    spawnShards(level.random, knight);
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
        Iterator<Shard> shards = SHARDS.iterator();
        while (shards.hasNext()) {
            Shard shard = shards.next();
            shard.age++;
            if (shard.age > 0) {
                shard.prevX = shard.x;
                shard.prevY = shard.y;
                shard.prevZ = shard.z;
                shard.x += shard.vx;
                shard.y += shard.vy;
                shard.z += shard.vz;
                shard.vx *= 0.92D;
                shard.vz *= 0.92D;
            }
            if (shard.age > SHARD_LIFE) {
                shards.remove();
            }
        }
        if (encircle != null) {
            encircle.age++;
            if (encircle.age > encircle.warn + encircle.shrink + encircle.thrust + 40) {
                encircle = null;
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

    private static void spawnShards(RandomSource random, KnightPhantom knight) {
        double height = knight.getBbHeight();
        for (int i = 0; i < SHARD_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.2D + random.nextDouble() * knight.getBbWidth() * 0.5D;
            Shard shard = new Shard();
            shard.x = knight.getX() + Math.cos(angle) * radius;
            shard.y = knight.getY() + height * i / SHARD_COUNT;
            shard.z = knight.getZ() + Math.sin(angle) * radius;
            shard.prevX = shard.x;
            shard.prevY = shard.y;
            shard.prevZ = shard.z;
            shard.vx = Math.cos(angle) * 0.04D;
            shard.vy = 0.03D + random.nextDouble() * 0.03D;
            shard.vz = Math.sin(angle) * 0.04D;
            shard.age = -i / 2;
            shard.spin = random.nextFloat() * 360.0F;
            shard.size = 0.08F + random.nextFloat() * 0.08F;
            SHARDS.add(shard);
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
        if (KNIGHTS.isEmpty() && ECHOES.isEmpty() && LINES.isEmpty() && SHARDS.isEmpty() && THRUSTS.isEmpty() && encircle == null && REGROUPS.isEmpty() && AFTERIMAGES.isEmpty()) {
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

        for (RegroupFx regroup : REGROUPS) {
            renderRegroup(poseStack, buffers, regroup, partialTick);
        }
        for (ChargeLine line : LINES) {
            renderLine(poseStack, buffers, line, partialTick);
        }
        for (int id : KNIGHTS) {
            if (level.getEntity(id) instanceof KnightPhantom knight && knight.isAlive()) {
                renderKnight(poseStack, buffers, level, knight, track(id), cameraRotation, partialTick);
            }
        }
        if (encircle != null) {
            renderEncircle(poseStack, buffers, level, encircle, partialTick, decorative);
        }
        for (PickRing ring : RINGS) {
            renderPickRing(poseStack, buffers, ring, partialTick);
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
                renderGhost(poseStack, buffers, image.pos.x, image.pos.y, image.pos.z, image.yaw, CHARGE_SCALE, r, g, b, 0.35F * life, true);
            }
            for (ThrustFx thrust : THRUSTS) {
                renderThrust(poseStack, buffers, thrust, partialTick);
            }
            renderShards(poseStack, buffers, cameraRotation, partialTick);
            renderAxeTrails(poseStack, buffers, level, cameraRotation, partialTick);
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderGhost(PoseStack poseStack, MultiBufferSource buffers, double x, double y, double z, float yaw, float scale,
                                    float r, float g, float b, float alpha, boolean glow) {
        if (alpha <= 0.01F) {
            return;
        }
        RenderType type = RenderType.entityTranslucent(GHOST);
        if (glow) {
            type = RenderType.entityTranslucentEmissive(GHOST);
        }
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(scale, scale, scale);
        ghostMesh(poseStack, buffers.getBuffer(type), 0.0F, 0.0F, 0.0F, r, g, b, alpha);
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
        poseStack.pushPose();
        poseStack.translate(x, y - 0.06D, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(scale, scale, scale);
        ghostMesh(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(GHOST)), age, headYaw, pitch, TEAL_R, TEAL_G, TEAL_B, alpha);
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

    private static void renderKnight(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, KnightPhantom knight, KnightTrack track,
                                     Quaternionf cameraRotation, float partialTick) {
        double x = Mth.lerp(partialTick, knight.xo, knight.getX());
        double y = Mth.lerp(partialTick, knight.yo, knight.getY());
        double z = Mth.lerp(partialTick, knight.zo, knight.getZ());
        float age = track.age + partialTick;
        if (knight.isChargingAtPlayer()) {
            double ground = groundY(level, x, y + 1.0D, z, y);
            VertexConsumer frame = buffers.getBuffer(RenderType.entityTranslucent(FRAME));
            float pulse = 0.65F + 0.2F * Mth.sin(age * 0.25F);
            FxDraw.flatSquare(frame, poseStack.last(), (float) x, (float) ground + 0.05F, (float) z, 1.4F, age * 0.04F, TEAL_R, TEAL_G, TEAL_B, pulse, FxDraw.FULL_BRIGHT);
            FxDraw.flatSquare(frame, poseStack.last(), (float) x, (float) ground + 0.06F, (float) z, 0.9F, -age * 0.06F, SILVER_R, SILVER_G, SILVER_B, pulse * 0.6F, FxDraw.FULL_BRIGHT);
            renderShell(poseStack, buffers, knight, partialTick, 0.28F + 0.08F * Mth.sin(age * 0.3F));
        }
        if (track.trackTarget >= 0 && level.getEntity(track.trackTarget) instanceof LivingEntity target) {
            double tx = Mth.lerp(partialTick, target.xo, target.getX());
            double ty = Mth.lerp(partialTick, target.yo, target.getY());
            double tz = Mth.lerp(partialTick, target.zo, target.getZ());
            double ground = groundY(level, tx, ty + 1.0D, tz, ty);
            renderStrip(poseStack, buffers, x, z, tx, tz, ground + 0.04D, 0.7F, SILVER_R, SILVER_G, SILVER_B, 0.28F, age * 0.05F, true);
        }
        if (track.glow > 0) {
            float t = 1.0F - (track.glow - partialTick) / track.glowMax;
            float yaw = Mth.rotLerp(partialTick, knight.yBodyRotO, knight.yBodyRot) * Mth.DEG_TO_RAD;
            double hx = x - Mth.cos(yaw) * 0.55D * CHARGE_SCALE - Mth.sin(yaw) * 0.35D;
            double hz = z - Mth.sin(yaw) * 0.55D * CHARGE_SCALE + Mth.cos(yaw) * 0.35D;
            double hy = y + knight.getBbHeight() * 0.55D;
            billboard(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLINT)), cameraRotation, hx, hy, hz, 0.4F + 0.5F * t, age * 12.0F,
                TEAL_R, TEAL_G, TEAL_B, 0.5F + 0.5F * t);
        }
        if (track.oath > 0) {
            double top = y + knight.getBbHeight() + 0.7D;
            billboard(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(OATH)), cameraRotation, x, top, z, 0.32F, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F);
            VertexConsumer gems = buffers.getBuffer(RenderType.entityTranslucentEmissive(GEM));
            float spacing = 0.26F;
            float start = -(track.oath - 1) * spacing * 0.5F;
            for (int i = 0; i < track.oath; i++) {
                poseStack.pushPose();
                poseStack.translate(x, top - 0.45D, z);
                poseStack.mulPose(cameraRotation);
                float off = start + i * spacing;
                PoseStack.Pose pose = poseStack.last();
                float s = 0.1F;
                FxDraw.vertex(gems, pose, off - s, -s, 0.0F, 0.0F, 1.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
                FxDraw.vertex(gems, pose, off + s, -s, 0.0F, 1.0F, 1.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
                FxDraw.vertex(gems, pose, off + s, s, 0.0F, 1.0F, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
                FxDraw.vertex(gems, pose, off - s, s, 0.0F, 0.0F, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
                poseStack.popPose();
            }
        }
    }

    private static void renderStrip(PoseStack poseStack, MultiBufferSource buffers, double x0, double z0, double x1, double z1, double y, float width,
                                    float r, float g, float b, float alpha, float scroll, boolean dashed) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.1D || alpha <= 0.01F) {
            return;
        }
        double nx = -dz / length * width * 0.5D;
        double nz = dx / length * width * 0.5D;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SWORD_LINE));
        PoseStack.Pose pose = poseStack.last();
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
            FxDraw.vertex(consumer, pose, (float) (ax + nx), (float) y, (float) (az + nz), u0, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) (ax - nx), (float) y, (float) (az - nz), u0, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) (bx - nx), (float) y, (float) (bz - nz), u1, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) (bx + nx), (float) y, (float) (bz + nz), u1, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void renderLine(PoseStack poseStack, MultiBufferSource buffers, ChargeLine line, float partialTick) {
        float age = line.age + partialTick;
        float alpha = 0.75F;
        if (line.dim) {
            alpha = 0.45F;
            if (age > line.warn - 12) {
                alpha = 0.75F;
            }
        }
        float end = line.warn + line.dash;
        if (age > end) {
            alpha *= Mth.clamp(1.0F - (age - end) / LINE_FADE, 0.0F, 1.0F);
        }
        float r = SILVER_R;
        float g = SILVER_G;
        float b = SILVER_B;
        if (age < LINE_FLASH) {
            float flash = 1.0F - age / LINE_FLASH;
            alpha = Math.min(1.0F, alpha + flash * 0.4F);
            r = Mth.lerp(flash, r, 1.0F);
            g = Mth.lerp(flash, g, 1.0F);
            b = Mth.lerp(flash, b, 1.0F);
        }
        float width = 1.1F;
        if (age < LINE_FLASH) {
            width += 0.4F * (1.0F - age / LINE_FLASH);
        }
        renderStrip(poseStack, buffers, line.sx, line.sz, line.ex, line.ez, line.y + 0.05D, width, r, g, b, alpha, age * 0.12F, false);
        if (age > line.warn && age < end) {
            float progress = Mth.clamp((age - line.warn) / line.dash, 0.0F, 1.0F);
            double px = Mth.lerp(progress, line.sx, line.ex);
            double pz = Mth.lerp(progress, line.sz, line.ez);
            renderStrip(poseStack, buffers, line.sx, line.sz, px, pz, line.y + 0.06D, width * 0.6F, TEAL_R, TEAL_G, TEAL_B, alpha, age * 0.3F, false);
        }
    }

    private static void renderPickRing(PoseStack poseStack, MultiBufferSource buffers, PickRing ring, float partialTick) {
        float life = 1.0F - (ring.age + partialTick) / RING_LIFE;
        if (life <= 0.0F) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(SWORD_LINE));
        PoseStack.Pose pose = poseStack.last();
        float step = Mth.TWO_PI / ring.count;
        float inner = 0.9F;
        float outer = 1.9F + (1.0F - life) * 0.8F;
        for (int i = 0; i < ring.count; i++) {
            float angle = ring.base + step * i;
            float cx = Mth.cos(angle);
            float cz = Mth.sin(angle);
            float nx = -cz * 0.06F;
            float nz = cx * 0.06F;
            float y = (float) ring.y;
            FxDraw.vertex(consumer, pose, (float) ring.x + cx * inner + nx, y, (float) ring.z + cz * inner + nz, 0.0F, 0.0F, SILVER_R, SILVER_G, SILVER_B, life, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) ring.x + cx * inner - nx, y, (float) ring.z + cz * inner - nz, 0.0F, 1.0F, SILVER_R, SILVER_G, SILVER_B, life, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) ring.x + cx * outer - nx, y, (float) ring.z + cz * outer - nz, 1.0F, 1.0F, TEAL_R, TEAL_G, TEAL_B, life, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) ring.x + cx * outer + nx, y, (float) ring.z + cz * outer + nz, 1.0F, 0.0F, TEAL_R, TEAL_G, TEAL_B, life, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void renderEncircle(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, EncircleFx ring, float partialTick, boolean decorative) {
        float age = ring.age + partialTick;
        int count = ring.ids.length;
        if (count < 2) {
            return;
        }
        boolean solid = age >= ring.warn;
        VertexConsumer consumer;
        if (solid) {
            consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(SWORD_WALL));
        } else {
            consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(SWORD_WALL_DASH));
        }
        float alpha = 0.85F;
        if (!solid) {
            alpha = 0.35F + 0.25F * Mth.sin(age * 0.6F);
        }
        float end = ring.warn + ring.shrink + ring.thrust;
        if (age > end) {
            alpha *= Mth.clamp(1.0F - (age - end) / 6.0F, 0.0F, 1.0F);
        }
        PoseStack.Pose pose = poseStack.last();
        float scroll = 0.0F;
        if (solid) {
            scroll = age * 0.05F;
        }
        for (int i = 0; i < count; i++) {
            if (i == ring.open) {
                continue;
            }
            Entity a = level.getEntity(ring.ids[i]);
            Entity b = level.getEntity(ring.ids[(i + 1) % count]);
            if (!(a instanceof KnightPhantom ka) || !(b instanceof KnightPhantom kb) || !ka.isAlive() || !kb.isAlive()) {
                continue;
            }
            float ax = (float) Mth.lerp(partialTick, a.xo, a.getX());
            float az = (float) Mth.lerp(partialTick, a.zo, a.getZ());
            float bx = (float) Mth.lerp(partialTick, b.xo, b.getX());
            float bz = (float) Mth.lerp(partialTick, b.zo, b.getZ());
            float length = (float) Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
            float y0 = (float) ring.y;
            float y1 = y0 + WALL_HEIGHT;
            float u1 = Math.max(1.0F, length / 2.0F);
            FxDraw.vertex(consumer, pose, ax, y0, az, 0.0F, 1.0F + scroll, SILVER_R, SILVER_G, SILVER_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, bx, y0, bz, u1, 1.0F + scroll, SILVER_R, SILVER_G, SILVER_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, bx, y1, bz, u1, scroll, TEAL_R, TEAL_G, TEAL_B, alpha * 0.4F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, ax, y1, az, 0.0F, scroll, TEAL_R, TEAL_G, TEAL_B, alpha * 0.4F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, ax, y1, az, 0.0F, scroll, TEAL_R, TEAL_G, TEAL_B, alpha * 0.4F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, bx, y1, bz, u1, scroll, TEAL_R, TEAL_G, TEAL_B, alpha * 0.4F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, bx, y0, bz, u1, 1.0F + scroll, SILVER_R, SILVER_G, SILVER_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, ax, y0, az, 0.0F, 1.0F + scroll, SILVER_R, SILVER_G, SILVER_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
        float thrustStart = ring.warn + ring.shrink - THRUST_PREVIEW;
        if (age >= thrustStart && age <= ring.warn + ring.shrink + ring.thrust) {
            float t = Mth.clamp((age - thrustStart) / THRUST_PREVIEW, 0.0F, 1.0F);
            float radius = ring.thrustRadius;
            if (age > ring.warn + ring.shrink) {
                radius *= 1.0F - 0.6F * Mth.clamp((age - ring.warn - ring.shrink) / Math.max(1, ring.thrust), 0.0F, 1.0F);
            }
            VertexConsumer decal = buffers.getBuffer(RenderType.entityTranslucent(THRUST));
            FxDraw.flatSquare(decal, poseStack.last(), (float) ring.x, (float) ring.y + 0.07F, (float) ring.z, radius, 0.0F, SILVER_R, SILVER_G, SILVER_B, 0.85F * t, FxDraw.FULL_BRIGHT);
        }
    }

    private static void renderThrust(PoseStack poseStack, MultiBufferSource buffers, ThrustFx thrust, float partialTick) {
        float t = (thrust.age + partialTick) / THRUST_LIFE;
        if (t >= 1.0F) {
            return;
        }
        float alpha = 1.0F - t;
        VertexConsumer decal = buffers.getBuffer(RenderType.entityTranslucentEmissive(THRUST));
        FxDraw.flatSquare(decal, poseStack.last(), (float) thrust.x, (float) thrust.y + 0.08F, (float) thrust.z, thrust.radius * (0.4F + t * 0.9F), t * 0.6F, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT);
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
        renderGhost(poseStack, buffers, x, y, z, echo.yaw, CHARGE_SCALE, SHADOW_R + 0.25F, SHADOW_G + 0.3F, SHADOW_B + 0.32F, 0.6F * alpha, false);
        renderGhost(poseStack, buffers, x, y - 0.06D, z, echo.yaw, CHARGE_SCALE * 1.06F, TEAL_R, TEAL_G, TEAL_B, 0.3F * alpha, true);
    }

    private static void renderRegroup(PoseStack poseStack, MultiBufferSource buffers, RegroupFx regroup, float partialTick) {
        float age = regroup.age + partialTick;
        float t = Mth.clamp(age / regroup.duration, 0.0F, 1.0F);
        float alpha = 0.8F;
        if (age > regroup.duration) {
            alpha *= Mth.clamp(1.0F - (age - regroup.duration) / 10.0F, 0.0F, 1.0F);
        }
        VertexConsumer frame = buffers.getBuffer(RenderType.entityTranslucentEmissive(FRAME));
        float size = Mth.lerp(t, 4.0F, 1.8F);
        FxDraw.flatSquare(frame, poseStack.last(), (float) regroup.x, (float) regroup.y + 0.05F, (float) regroup.z, size, age * 0.05F, SILVER_R, SILVER_G, SILVER_B, alpha, FxDraw.FULL_BRIGHT);
        FxDraw.flatSquare(frame, poseStack.last(), (float) regroup.x, (float) regroup.y + 0.06F, (float) regroup.z, size * 0.7F, -age * 0.05F + 0.785F, TEAL_R, TEAL_G, TEAL_B, alpha * 0.7F, FxDraw.FULL_BRIGHT);
    }

    private static void renderShards(PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraRotation, float partialTick) {
        if (SHARDS.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(SHARD));
        for (Shard shard : SHARDS) {
            if (shard.age < 0) {
                continue;
            }
            float life = 1.0F - (shard.age + partialTick) / SHARD_LIFE;
            if (life <= 0.0F) {
                continue;
            }
            double x = Mth.lerp(partialTick, shard.prevX, shard.x);
            double y = Mth.lerp(partialTick, shard.prevY, shard.y);
            double z = Mth.lerp(partialTick, shard.prevZ, shard.z);
            billboard(poseStack, consumer, cameraRotation, x, y, z, shard.size, shard.spin + shard.age * 9.0F, SILVER_R, SILVER_G, SILVER_B, life);
        }
    }

    private static void renderAxeTrails(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Quaternionf cameraRotation, float partialTick) {
        if (AXE_TRAILS.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(ARC));
        for (Map.Entry<Integer, List<Vec3>> entry : AXE_TRAILS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            List<Vec3> trail = entry.getValue();
            for (int i = 0; i < trail.size(); i++) {
                Vec3 pos = trail.get(i);
                float fade = 1.0F - (float) i / AXE_TRAIL;
                billboard(poseStack, consumer, cameraRotation, pos.x, pos.y + 0.15D, pos.z, 0.45F * fade + 0.15F, (entity.tickCount + partialTick - i) * 40.0F,
                    TEAL_R, TEAL_G, TEAL_B, 0.55F * fade);
            }
        }
    }

    private static void billboard(PoseStack poseStack, VertexConsumer consumer, Quaternionf cameraRotation, double x, double y, double z, float size, float spin,
                                  float r, float g, float b, float alpha) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(consumer, pose, -size, -size, 0.0F, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, size, -size, 0.0F, 1.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, size, size, 0.0F, 1.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, -size, size, 0.0F, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        poseStack.popPose();
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

    private static final class Shard {
        private double x;
        private double y;
        private double z;
        private double prevX;
        private double prevY;
        private double prevZ;
        private double vx;
        private double vy;
        private double vz;
        private int age;
        private float spin;
        private float size;
    }
}
