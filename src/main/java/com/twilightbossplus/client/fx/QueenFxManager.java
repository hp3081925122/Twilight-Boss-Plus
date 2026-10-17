package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.network.QueenFxPacket;
import com.twilightbossplus.queen.SnowQueenBossController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.SnowQueen;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QueenFxManager {
    private static final ResourceLocation CRYSTAL = tex("px/crystal");
    private static final ResourceLocation SIGIL = tex("px/sigil");
    private static final ResourceLocation ARM = tex("px/arm");
    private static final ResourceLocation FLAKE = tex("px/flake");
    private static final ResourceLocation MARK = tex("px/mark");
    private static final ResourceLocation SHARD = tex("px/shard");
    private static final ResourceLocation AURORA = tex("aurora");

    private static final float ICE_R = 0.918F;
    private static final float ICE_G = 0.965F;
    private static final float ICE_B = 1.0F;
    private static final float PRISM_R = 0.498F;
    private static final float PRISM_G = 0.890F;
    private static final float PRISM_B = 0.941F;
    private static final float GREEN_R = 0.424F;
    private static final float GREEN_G = 0.949F;
    private static final float GREEN_B = 0.690F;
    private static final float VIOLET_R = 0.706F;
    private static final float VIOLET_G = 0.549F;
    private static final float VIOLET_B = 1.0F;

    private static final int SIGIL_AFTER = 24;
    private static final int LANCE_HOLD = 24;
    private static final int LANCE_FADE = 8;
    private static final double BEAM_LENGTH = 20.0D;
    private static final double HAND_HEIGHT = 1.45D;

    private static final Map<Integer, QueenTrack> TRACKS = new HashMap<>();
    private static final List<SigilFx> SIGILS = new ArrayList<>();
    private static final List<LanceFx> LANCES = new ArrayList<>();
    private static final List<CurtainFx> CURTAINS = new ArrayList<>();

    private QueenFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/queen/" + name + ".png");
    }

    private static QueenTrack track(int id) {
        return TRACKS.computeIfAbsent(id, key -> new QueenTrack());
    }

    private static Vec3 forward(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0D, Mth.cos(rad));
    }

    private static float flatAngle(float yaw) {
        return (yaw + 90.0F) * Mth.DEG_TO_RAD;
    }

    public static void handle(int type, int entityId, float[] data) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (type == QueenFxPacket.CAST && data.length >= 2) {
            QueenTrack track = track(entityId);
            track.pose = (int) data[0];
            track.poseDuration = Math.max(1, (int) data[1]);
            track.poseStart = 0;
            if (level.getEntity(entityId) != null) {
                track.poseStart = level.getEntity(entityId).tickCount;
            }
        } else if (type == QueenFxPacket.SIGIL && data.length >= 8) {
            SIGILS.add(new SigilFx(data[0], data[1], data[2], data[3], (int) data[4], data[5], data[6], data[7]));
        } else if (type == QueenFxPacket.LANCE && data.length >= 5) {
            LANCES.add(new LanceFx(data[0], data[1], data[2], (int) data[3], data[4], level.getRandom().nextFloat() * 60.0F));
        } else if (type == QueenFxPacket.AURORA && data.length >= 10) {
            CURTAINS.add(new CurtainFx(data[0], data[1], data[2], data[3], data[4], data[5], data[6], (int) data[7], (int) data[8], data[9]));
        } else if (type == QueenFxPacket.REFRACT && data.length >= 1) {
            track(entityId).refract = data[0];
        }
    }

    public static void applyPose(SnowQueen entity, float ageInTicks, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        QueenTrack track = TRACKS.get(entity.getId());
        if (track == null || track.pose == SnowQueenBossController.POSE_NONE || entity.isBreathing()) {
            return;
        }
        float p = Mth.clamp((ageInTicks - track.poseStart) / track.poseDuration, 0.0F, 1.0F);
        float in = Math.min(1.0F, p * 4.0F);
        if (track.pose == SnowQueenBossController.POSE_SIGIL) {
            rightArm.xRot = Mth.lerp(in, rightArm.xRot, -0.35F);
            leftArm.xRot = Mth.lerp(in, leftArm.xRot, -0.35F);
            rightArm.yRot = 0.0F;
            leftArm.yRot = 0.0F;
            rightArm.zRot = 1.0F * in + Mth.sin(ageInTicks * 0.4F) * 0.04F;
            leftArm.zRot = -1.0F * in - Mth.sin(ageInTicks * 0.4F) * 0.04F;
            head.xRot = 0.45F * in;
            body.xRot = 0.15F * in;
        } else if (track.pose == SnowQueenBossController.POSE_LANCE) {
            rightArm.xRot = -(float) Math.PI * 0.5F * in - 0.15F * in;
            rightArm.yRot = head.yRot * in;
            rightArm.zRot = 0.0F;
            leftArm.zRot = -0.35F * in;
            leftArm.xRot = 0.2F * in;
        } else if (track.pose == SnowQueenBossController.POSE_AURORA) {
            rightArm.xRot = -(float) Math.PI * 0.85F * in;
            leftArm.xRot = -(float) Math.PI * 0.85F * in;
            rightArm.yRot = 0.0F;
            leftArm.yRot = 0.0F;
            rightArm.zRot = 0.35F * in;
            leftArm.zRot = -0.35F * in;
            head.xRot = -0.5F * in;
            body.xRot = -0.1F * in;
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof SnowQueen) {
            track(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            TRACKS.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        TRACKS.clear();
        SIGILS.clear();
        LANCES.clear();
        CURTAINS.clear();
    }

    private static void spawnShards(RandomSource random, double x, double y, double z, double floor, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.4D + random.nextDouble() * 0.6D);
            FxParticles.Particle shard = FxParticles.soft(SHARD, x, y, z, Math.cos(angle) * s, 0.18D + random.nextDouble() * 0.25D, Math.sin(angle) * s,
                    18 + random.nextInt(10), 0.22F + random.nextFloat() * 0.14F)
                .gravity(0.045D).drag(0.92D).spin(random.nextFloat() * Mth.TWO_PI, (random.nextFloat() - 0.5F) * 0.6F).fade(0.0F, 3.0F);
            if (!Double.isNaN(floor)) {
                shard.floor(floor + 0.05D, 0.3D);
            }
        }
    }

    private static void spawnFlakes(RandomSource random, double x, double y, double z, int count, double speed, float size) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.5D + random.nextDouble() * 0.5D);
            FxParticles.glow(FLAKE, x, y, z, Math.cos(angle) * s, 0.04D + random.nextDouble() * 0.12D, Math.sin(angle) * s, 22 + random.nextInt(10), size)
                .color(ICE_R, ICE_G, ICE_B).fadeTo(PRISM_R, PRISM_G, PRISM_B).gravity(0.006D).drag(0.93D)
                .spin(random.nextFloat() * Mth.TWO_PI, (random.nextFloat() - 0.5F) * 0.3F).flicker(0.25F, random.nextFloat() * 10.0F);
        }
    }

    private static Vec3 orbitPos(Vec3 pos, float time, int index) {
        double angle = time * 0.06D + index * Math.PI * 2.0D / 3.0D;
        return new Vec3(pos.x + Math.cos(angle) * 1.1D, pos.y + 1.3D + Math.sin(time * 0.1D + index * 2.0D) * 0.2D, pos.z + Math.sin(angle) * 1.1D);
    }

    private static int traceBeams(ClientLevel level, SnowQueen queen, float refract, Vec3 origin, Vec3 look, Vec3[] ends, boolean[] hits, Vec3[] dirs) {
        int count = 1;
        if (refract > 0.0F) {
            count = 3;
        }
        for (int i = 0; i < count; i++) {
            Vec3 dir = look;
            if (i == 1) {
                dir = look.yRot(refract * Mth.DEG_TO_RAD);
            } else if (i == 2) {
                dir = look.yRot(-refract * Mth.DEG_TO_RAD);
            }
            Vec3 end = origin.add(dir.scale(BEAM_LENGTH));
            HitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, queen));
            hits[i] = hit.getType() != HitResult.Type.MISS;
            if (hits[i]) {
                end = hit.getLocation();
            }
            ends[i] = end;
            dirs[i] = dir;
        }
        return count;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            if (!TRACKS.isEmpty() || !SIGILS.isEmpty() || !LANCES.isEmpty() || !CURTAINS.isEmpty()) {
                clearAll();
            }
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        RandomSource random = level.getRandom();
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        for (Map.Entry<Integer, QueenTrack> entry : TRACKS.entrySet()) {
            QueenTrack track = entry.getValue();
            track.age++;
            if (!(level.getEntity(entry.getKey()) instanceof SnowQueen queen)) {
                continue;
            }
            if (!queen.isBreathing()) {
                track.refract = 0.0F;
            }
            if (queen.getCurrentPhase() == SnowQueen.Phase.DROP && track.age % 5 == 0) {
                track.dropGround = SnowQueenBossController.groundBelow(level, queen.getX(), queen.getY(), queen.getZ());
            } else if (queen.getCurrentPhase() != SnowQueen.Phase.DROP) {
                track.dropGround = Double.NaN;
            }
            if (!decorative || !queen.isAlive()) {
                continue;
            }
            if (queen.isBreathing() && track.age % 2 == 0) {
                Vec3 look = queen.getViewVector(1.0F);
                Vec3 origin = queen.position().add(0.0D, HAND_HEIGHT, 0.0D).add(look.scale(0.6D));
                Vec3[] ends = new Vec3[3];
                boolean[] hits = new boolean[3];
                Vec3[] dirs = new Vec3[3];
                int count = traceBeams(level, queen, track.refract, origin, look, ends, hits, dirs);
                for (int i = 0; i < count; i++) {
                    if (hits[i]) {
                        spawnShards(random, ends[i].x, ends[i].y, ends[i].z, Double.NaN, 1, 0.16D);
                        FxParticles.glow(FxTextures.MOTE, ends[i].x, ends[i].y, ends[i].z, (random.nextDouble() - 0.5D) * 0.2D, 0.05D + random.nextDouble() * 0.08D,
                                (random.nextDouble() - 0.5D) * 0.2D, 14, 0.25F)
                            .color(ICE_R, ICE_G, ICE_B).fadeTo(PRISM_R, PRISM_G, PRISM_B).drag(0.9D);
                    }
                    double d = random.nextDouble() * origin.distanceTo(ends[i]);
                    Vec3 p = origin.add(dirs[i].scale(d));
                    FxParticles.glow(FxTextures.SPARK, p.x, p.y, p.z, dirs[i].x * 0.5D, dirs[i].y * 0.5D, dirs[i].z * 0.5D, 6, 0.18F)
                        .color(ICE_R, ICE_G, ICE_B).fadeTo(PRISM_R, PRISM_G, PRISM_B).streak(3.0F).drag(0.8D);
                }
            }
            if (track.age % 3 == 0) {
                Vec3 crystal = orbitPos(queen.position(), track.age, random.nextInt(3));
                FxParticles.glow(FxTextures.MOTE, crystal.x, crystal.y, crystal.z, 0.0D, -0.015D, 0.0D, 14, 0.14F)
                    .color(PRISM_R, PRISM_G, PRISM_B).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.95D);
            }
            if (!Double.isNaN(track.dropGround) && track.age % 2 == 0) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = 1.6D + random.nextDouble() * 1.2D;
                FxParticles.glow(FxTextures.MOTE, queen.getX() + Math.cos(angle) * dist, track.dropGround + 0.1D, queen.getZ() + Math.sin(angle) * dist,
                        -Math.cos(angle) * dist / 14.0D, 0.03D, -Math.sin(angle) * dist / 14.0D, 14, 0.2F)
                    .color(VIOLET_R, VIOLET_G, VIOLET_B).drag(1.0D);
            }
        }
        Iterator<SigilFx> sigils = SIGILS.iterator();
        while (sigils.hasNext()) {
            SigilFx sigil = sigils.next();
            sigil.age++;
            if (sigil.age < sigil.windup && sigil.age % 2 == 0) {
                float charge = (float) sigil.age / sigil.windup;
                Vec3 dir = forward(sigil.start + sigil.spin * charge + random.nextInt(6) * 60.0F);
                double d = random.nextDouble() * sigil.radius * charge;
                FxParticles.glow(FxTextures.MOTE, sigil.x + dir.x * d, sigil.y + 0.1D, sigil.z + dir.z * d, 0.0D, 0.03D + random.nextDouble() * 0.04D, 0.0D, 18, 0.2F)
                    .color(PRISM_R, PRISM_G, PRISM_B).fadeTo(ICE_R, ICE_G, ICE_B).drag(0.98D).flicker(0.3F, random.nextFloat() * 10.0F);
            }
            if (sigil.age == sigil.windup) {
                float angle = sigil.start + sigil.spin;
                FxShake.at(sigil.x, sigil.y, sigil.z, 0.5F, sigil.radius + 10.0F, 8);
                for (int i = 0; i < 6; i++) {
                    Vec3 dir = forward(angle + i * 60.0F);
                    for (int k = 1; k <= 3; k++) {
                        double d = sigil.radius * k / 3.5D;
                        spawnShards(random, sigil.x + dir.x * d, sigil.y + 0.6D, sigil.z + dir.z * d, sigil.y, 2, 0.12D);
                    }
                    Vec3 tip = new Vec3(sigil.x + dir.x * sigil.radius, sigil.y + 0.3D, sigil.z + dir.z * sigil.radius);
                    spawnFlakes(random, tip.x, tip.y, tip.z, 2, 0.08D, 0.4F);
                }
                spawnFlakes(random, sigil.x, sigil.y + 0.5D, sigil.z, 4, 0.12D, 0.55F);
            }
            if (sigil.age > sigil.windup + SIGIL_AFTER) {
                sigils.remove();
            }
        }
        Iterator<LanceFx> lances = LANCES.iterator();
        while (lances.hasNext()) {
            LanceFx lance = lances.next();
            lance.age++;
            if (lance.age < lance.warning && lance.age % 3 == 0) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = lance.radius * 1.8D;
                FxParticles.glow(FxTextures.MOTE, lance.x + Math.cos(angle) * dist, lance.y + 0.1D, lance.z + Math.sin(angle) * dist,
                        -Math.cos(angle) * dist / 12.0D, 0.02D, -Math.sin(angle) * dist / 12.0D, 12, 0.18F)
                    .color(PRISM_R, PRISM_G, PRISM_B).fadeTo(ICE_R, ICE_G, ICE_B).drag(1.0D);
            }
            if (lance.age == lance.warning) {
                FxShake.at(lance.x, lance.y, lance.z, 0.4F, 12.0F, 6);
                spawnShards(random, lance.x, lance.y + 0.5D, lance.z, lance.y, 8, 0.2D);
                for (int i = 0; i < 6; i++) {
                    FxParticles.glow(FxTextures.SPARK, lance.x + (random.nextDouble() - 0.5D) * lance.radius, lance.y + 0.2D,
                            lance.z + (random.nextDouble() - 0.5D) * lance.radius, 0.0D, 0.45D + random.nextDouble() * 0.3D, 0.0D, 8, 0.25F)
                        .color(ICE_R, ICE_G, ICE_B).fadeTo(PRISM_R, PRISM_G, PRISM_B).streak(4.0F).drag(0.85D);
                }
            } else if (lance.age == lance.warning + LANCE_HOLD) {
                spawnShards(random, lance.x, lance.y + 1.5D, lance.z, lance.y, 10, 0.14D);
                spawnFlakes(random, lance.x, lance.y + 2.0D, lance.z, 3, 0.1D, 0.4F);
            }
            if (lance.age > lance.warning + LANCE_HOLD + LANCE_FADE) {
                lances.remove();
            }
        }
        Iterator<CurtainFx> curtains = CURTAINS.iterator();
        while (curtains.hasNext()) {
            CurtainFx curtain = curtains.next();
            curtain.age++;
            Vec3 axis = forward(curtain.yaw);
            if (curtain.age > curtain.warning) {
                curtain.x += -axis.z * curtain.drift;
                curtain.z += axis.x * curtain.drift;
            }
            if (curtain.age == curtain.warning) {
                FxShake.at(curtain.x, curtain.y, curtain.z, 0.3F, curtain.length * 0.5F + 10.0F, 6);
            }
            if (curtain.age > curtain.warning && curtain.age % 2 == 0) {
                double t = random.nextDouble() - 0.5D;
                double px = curtain.x + axis.x * curtain.length * t;
                double pz = curtain.z + axis.z * curtain.length * t;
                float r = GREEN_R;
                float g = GREEN_G;
                float b = GREEN_B;
                if (random.nextBoolean()) {
                    r = VIOLET_R;
                    g = VIOLET_G;
                    b = VIOLET_B;
                }
                FxParticles.glow(FxTextures.MOTE, px, curtain.y + 0.2D + random.nextDouble() * curtain.height * 0.5D, pz, 0.0D, 0.04D + random.nextDouble() * 0.04D, 0.0D, 24, 0.24F)
                    .color(r, g, b).fadeTo(ICE_R, ICE_G, ICE_B).drag(0.98D).flicker(0.4F, random.nextFloat() * 10.0F);
                if (curtain.age % 6 == 0) {
                    FxParticles.soft(FLAKE, px, curtain.y + curtain.height, pz, 0.0D, -0.05D, 0.0D, 30, 0.3F)
                        .color(ICE_R, ICE_G, ICE_B).alpha(0.8F).drag(0.99D).spin(random.nextFloat() * Mth.TWO_PI, 0.08F);
                }
            }
            if (curtain.age > curtain.warning + curtain.active) {
                curtains.remove();
            }
        }
    }

    private static boolean hasWork() {
        return !TRACKS.isEmpty() || !SIGILS.isEmpty() || !LANCES.isEmpty() || !CURTAINS.isEmpty();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !SnowQueenConfig.ENABLED.get() || !hasWork()) {
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

        for (SigilFx sigil : SIGILS) {
            renderSigil(pose, buffers, sigil, partialTick);
        }
        for (LanceFx lance : LANCES) {
            renderLance(pose, buffers, lance, partialTick);
        }
        for (CurtainFx curtain : CURTAINS) {
            renderCurtain(pose, buffers, curtain, partialTick);
        }
        for (Map.Entry<Integer, QueenTrack> entry : TRACKS.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof SnowQueen queen) || !queen.isAlive()) {
                continue;
            }
            QueenTrack track = entry.getValue();
            float time = track.age + partialTick;
            if (queen.isBreathing()) {
                renderBeams(pose, buffers, level, queen, track, partialTick, time);
            }
            if (!Double.isNaN(track.dropGround)) {
                renderDrop(pose, buffers, queen, track, partialTick, time);
            }
            if (decorative) {
                Vec3 pos = queen.getPosition(partialTick);
                VertexConsumer crystals = buffers.getBuffer(FxRenderTypes.soft(CRYSTAL));
                for (int i = 0; i < 3; i++) {
                    Vec3 p = orbitPos(pos, time, i);
                    prism(crystals, pose, (float) p.x, (float) p.y, (float) p.z, 0.09F, 0.35F, 0.15F, time * 3.0F + i * 40.0F, true, ICE_R, ICE_G, ICE_B, 0.85F);
                }
                VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
                for (int i = 0; i < 3; i++) {
                    Vec3 p = orbitPos(pos, time, i);
                    FxDraw.sprite(glow, pose, p.x, p.y + 0.17D, p.z, 0.8F, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.35F + 0.1F * Mth.sin(time * 0.3F + i));
                }
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderSigil(PoseStack.Pose pose, MultiBufferSource buffers, SigilFx sigil, float partialTick) {
        float age = sigil.age + partialTick;
        float charge = Mth.clamp(age / sigil.windup, 0.0F, 1.0F);
        float angle = sigil.start + sigil.spin * charge;
        float rad = flatAngle(angle);
        float fade = 1.0F;
        if (age > sigil.windup) {
            fade = Mth.clamp(1.0F - (age - sigil.windup) / SIGIL_AFTER, 0.0F, 1.0F);
        }
        float grow = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.5F);

        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, sigil.x, sigil.y + 0.03D, sigil.z, sigil.radius * 1.25F, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.22F * (0.4F + 0.6F * charge) * fade);

        VertexConsumer decal = buffers.getBuffer(FxRenderTypes.glow(SIGIL));
        FxDraw.flat(decal, pose, sigil.x, sigil.y + 0.05D, sigil.z, sigil.radius * (0.6F + 0.4F * grow), rad, PRISM_R, PRISM_G, PRISM_B,
            (0.45F + 0.45F * charge) * fade * pulse);
        FxDraw.flat(decal, pose, sigil.x, sigil.y + 0.055D, sigil.z, sigil.radius * 0.42F * grow, -rad * 1.5F, ICE_R, ICE_G, ICE_B, 0.55F * charge * fade);

        VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        float ringAlpha = 0.6F * charge * fade;
        FxDraw.ring(beam, pose, sigil.x, sigil.y + 0.06D, sigil.z, sigil.radius - 0.2F, sigil.radius + 0.2F, 48, rad, 6.0F, PRISM_R, PRISM_G, PRISM_B, ringAlpha, ringAlpha);
        for (int i = 0; i < 6; i++) {
            Vec3 dir = forward(angle + i * 60.0F);
            float length = sigil.radius;
            if (age <= sigil.windup) {
                length = sigil.radius * charge;
            }
            armStrip(beam, pose, sigil.x, sigil.y + 0.058F, sigil.z, dir, length, sigil.width * 2.4F, length / 2.0F, PRISM_R, PRISM_G, PRISM_B, 0.45F * fade);
        }

        VertexConsumer arms = buffers.getBuffer(FxRenderTypes.glow(ARM));
        for (int i = 0; i < 6; i++) {
            Vec3 dir = forward(angle + i * 60.0F);
            if (age <= sigil.windup) {
                armStrip(arms, pose, sigil.x, sigil.y + 0.065F, sigil.z, dir, sigil.radius * charge, sigil.width * 1.2F, charge, ICE_R, ICE_G, ICE_B, 0.4F + 0.6F * charge);
            } else {
                armStrip(arms, pose, sigil.x, sigil.y + 0.065F, sigil.z, dir, sigil.radius, sigil.width * 1.2F, 1.0F, ICE_R, ICE_G, ICE_B, fade);
            }
        }
        if (age <= sigil.windup) {
            return;
        }
        float after = age - sigil.windup;
        if (after < 8.0F) {
            float t = after / 8.0F;
            VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
            FxDraw.flat(shock, pose, sigil.x, sigil.y + 0.08D, sigil.z, sigil.radius * (0.5F + 0.9F * t), rad, PRISM_R, PRISM_G, PRISM_B, 0.85F * (1.0F - t));
            VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
            FxDraw.sprite(flare, pose, sigil.x, sigil.y + 0.6D, sigil.z, sigil.radius * 0.9F * (1.0F - t), rad, ICE_R, ICE_G, ICE_B, 0.9F * (1.0F - t));
        }
        float rise = Mth.clamp(after / 3.0F, 0.0F, 1.0F);
        float sink = Mth.clamp((after - (SIGIL_AFTER - 8)) / 8.0F, 0.0F, 1.0F);
        float scale = rise * (1.0F - sink);
        if (scale <= 0.01F) {
            return;
        }
        int count = Math.max(3, (int) (sigil.radius / 1.1F));
        VertexConsumer crystals = buffers.getBuffer(FxRenderTypes.soft(CRYSTAL));
        for (int i = 0; i < 6; i++) {
            Vec3 dir = forward(angle + i * 60.0F);
            for (int k = 0; k < count; k++) {
                float d = sigil.radius * (k + 0.7F) / (count + 0.2F);
                float height = (0.7F + 1.1F * (1.0F - (float) k / count)) * scale;
                float radius = sigil.width * 0.32F * (1.0F - 0.3F * k / count);
                float tint = (float) k / count;
                prism(crystals, pose, (float) (sigil.x + dir.x * d), sigil.y, (float) (sigil.z + dir.z * d), radius, height, height * 0.45F, angle + k * 17.0F, false,
                    Mth.lerp(tint, ICE_R, PRISM_R), Mth.lerp(tint, ICE_G, PRISM_G), Mth.lerp(tint, ICE_B, PRISM_B), 0.88F);
            }
        }
        VertexConsumer bases = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        for (int i = 0; i < 6; i++) {
            Vec3 dir = forward(angle + i * 60.0F);
            for (int k = 0; k < count; k++) {
                float d = sigil.radius * (k + 0.7F) / (count + 0.2F);
                FxDraw.flat(bases, pose, sigil.x + dir.x * d, sigil.y + 0.07D, sigil.z + dir.z * d, sigil.width * 0.9F, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.4F * scale);
            }
        }
    }

    private static void renderLance(PoseStack.Pose pose, MultiBufferSource buffers, LanceFx lance, float partialTick) {
        float age = lance.age + partialTick;
        float spin = lance.spin * Mth.DEG_TO_RAD;
        if (age < lance.warning) {
            float t = Mth.clamp(age / lance.warning, 0.0F, 1.0F);
            VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            FxDraw.flat(glow, pose, lance.x, lance.y + 0.03D, lance.z, lance.radius * 1.5F, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.2F + 0.25F * t);
            VertexConsumer marks = buffers.getBuffer(FxRenderTypes.glow(MARK));
            FxDraw.flat(marks, pose, lance.x, lance.y + 0.05D, lance.z, lance.radius * (1.9F - 0.9F * t), age * 0.12F + spin, PRISM_R, PRISM_G, PRISM_B, 0.45F + 0.5F * t);
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.beam(beam, pose, new Vec3(lance.x, lance.y, lance.z), new Vec3(lance.x, lance.y + 3.5D, lance.z), 0.1F + 0.2F * t, 0.02F, 0.0F, 1.0F,
                PRISM_R, PRISM_G, PRISM_B, 0.4F * t, 0.0F);
            return;
        }
        float after = age - lance.warning;
        float rise = Mth.clamp(after / 3.0F, 0.0F, 1.0F);
        float alpha = 0.9F;
        float shrink = 1.0F;
        if (after > LANCE_HOLD) {
            float t = Mth.clamp((after - LANCE_HOLD) / LANCE_FADE, 0.0F, 1.0F);
            alpha = 0.9F * (1.0F - t);
            shrink = 1.0F - 0.4F * t;
        }
        float height = 3.2F * rise * shrink;
        VertexConsumer crystals = buffers.getBuffer(FxRenderTypes.soft(CRYSTAL));
        prism(crystals, pose, lance.x, lance.y - 0.2F, lance.z, lance.radius * 0.55F * shrink, height, height * 0.35F, lance.spin, false, ICE_R, ICE_G, ICE_B, alpha);
        for (int i = 0; i < 3; i++) {
            float a = lance.spin + i * 120.0F + 30.0F;
            Vec3 dir = forward(a);
            float side = 1.6F * rise * shrink;
            prism(crystals, pose, (float) (lance.x + dir.x * lance.radius * 0.6F), lance.y - 0.2F, (float) (lance.z + dir.z * lance.radius * 0.6F),
                lance.radius * 0.25F * shrink, side, side * 0.4F, a, false, PRISM_R, PRISM_G, PRISM_B, alpha);
        }
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, lance.x, lance.y + 0.04D, lance.z, lance.radius * 1.3F, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.35F * alpha);
        FxDraw.sprite(glow, pose, lance.x, lance.y + height * 0.5D, lance.z, lance.radius * 2.2F, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.18F * alpha);
        if (after < 8.0F) {
            float t = after / 8.0F;
            VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
            FxDraw.flat(shock, pose, lance.x, lance.y + 0.06D, lance.z, lance.radius * (1.0F + 2.2F * t), spin, ICE_R, ICE_G, ICE_B, 0.85F * (1.0F - t));
            VertexConsumer marks = buffers.getBuffer(FxRenderTypes.glow(MARK));
            FxDraw.flat(marks, pose, lance.x, lance.y + 0.07D, lance.z, lance.radius * (1.0F + 1.2F * t), spin, ICE_R, ICE_G, ICE_B, 0.9F * (1.0F - t));
            VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
            FxDraw.sprite(flare, pose, lance.x, lance.y - 0.2D + height + height * 0.35D, lance.z, 2.4F * (1.0F - t), spin, ICE_R, ICE_G, ICE_B, 0.95F * (1.0F - t));
        }
    }

    private static void renderCurtain(PoseStack.Pose pose, MultiBufferSource buffers, CurtainFx curtain, float partialTick) {
        float age = curtain.age + partialTick;
        Vec3 axis = forward(curtain.yaw);
        float cx = curtain.x;
        float cz = curtain.z;
        if (curtain.age > curtain.warning) {
            cx += (float) (-axis.z * curtain.drift * partialTick);
            cz += (float) (axis.x * curtain.drift * partialTick);
        }
        float alpha;
        float heightScale;
        if (age < curtain.warning) {
            float t = age / curtain.warning;
            alpha = 0.18F + 0.12F * t + 0.06F * Mth.sin(age * 0.6F);
            heightScale = 0.35F + 0.25F * t;
        } else {
            float active = age - curtain.warning;
            float in = Mth.clamp(active / 4.0F, 0.0F, 1.0F);
            float out = Mth.clamp((curtain.warning + curtain.active - age) / 10.0F, 0.0F, 1.0F);
            alpha = (0.3F + 0.6F * in) * out;
            heightScale = 0.6F + 0.4F * in;
        }
        if (alpha <= 0.01F) {
            return;
        }
        float half = curtain.length * 0.5F;
        float sx = (float) (-axis.z * curtain.width * 0.5F);
        float sz = (float) (axis.x * curtain.width * 0.5F);
        float x0 = (float) (cx - axis.x * half);
        float z0 = (float) (cz - axis.z * half);
        float x1 = (float) (cx + axis.x * half);
        float z1 = (float) (cz + axis.z * half);
        float y = curtain.y + 0.05F;
        float scroll = age * 0.05F;
        float ga = alpha * 0.8F;
        VertexConsumer ground = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.vertex(ground, pose, x0 + sx, y, z0 + sz, scroll, 0.0F, GREEN_R, GREEN_G, GREEN_B, ga, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(ground, pose, x0 - sx, y, z0 - sz, scroll, 1.0F, GREEN_R, GREEN_G, GREEN_B, ga, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(ground, pose, x1 - sx, y, z1 - sz, scroll + curtain.length / 3.0F, 1.0F, VIOLET_R, VIOLET_G, VIOLET_B, ga, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(ground, pose, x1 + sx, y, z1 + sz, scroll + curtain.length / 3.0F, 0.0F, VIOLET_R, VIOLET_G, VIOLET_B, ga, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        VertexConsumer ribbon = buffers.getBuffer(FxRenderTypes.glow(AURORA));
        float height = curtain.height * heightScale;
        ribbon(ribbon, pose, cx, curtain.y, cz, axis, curtain.length, height, age, 0.0F, 0.0F, alpha,
            GREEN_R, GREEN_G, GREEN_B, VIOLET_R, VIOLET_G, VIOLET_B);
        ribbon(ribbon, pose, cx, curtain.y + 0.4F, cz, axis, curtain.length * 0.9F, height * 0.8F, age, 0.35F, 2.1F, alpha * 0.6F,
            VIOLET_R, VIOLET_G, VIOLET_B, PRISM_R, PRISM_G, PRISM_B);
        ribbon(ribbon, pose, cx, curtain.y + 0.1F, cz, axis, curtain.length * 1.05F, height * 1.15F, age * 0.7F, -0.3F, 4.3F, alpha * 0.35F,
            PRISM_R, PRISM_G, PRISM_B, GREEN_R, GREEN_G, GREEN_B);
    }

    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, float cx, float y, float cz, Vec3 axis, float length, float height, float age,
                               float offset, float phase, float alpha, float br, float bg, float bb, float tr, float tg, float tb) {
        int segments = 24;
        float px = (float) -axis.z;
        float pz = (float) axis.x;
        float top = y + height;
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float w0 = Mth.sin(t0 * 7.0F + age * 0.08F + phase) * 0.45F + offset;
            float w1 = Mth.sin(t1 * 7.0F + age * 0.08F + phase) * 0.45F + offset;
            float e0 = Mth.sin(t0 * (float) Math.PI);
            float e1 = Mth.sin(t1 * (float) Math.PI);
            float x0 = (float) (cx + axis.x * length * (t0 - 0.5F)) + px * w0;
            float z0 = (float) (cz + axis.z * length * (t0 - 0.5F)) + pz * w0;
            float x1 = (float) (cx + axis.x * length * (t1 - 0.5F)) + px * w1;
            float z1 = (float) (cz + axis.z * length * (t1 - 0.5F)) + pz * w1;
            float u0 = t0 * length / 6.0F + age * 0.012F + phase;
            float u1 = t1 * length / 6.0F + age * 0.012F + phase;
            float a0 = alpha * (0.35F + 0.65F * e0);
            float a1 = alpha * (0.35F + 0.65F * e1);
            float h0 = top + Mth.sin(t0 * 11.0F + age * 0.05F + phase) * height * 0.08F;
            float h1 = top + Mth.sin(t1 * 11.0F + age * 0.05F + phase) * height * 0.08F;
            FxDraw.vertex(consumer, pose, x0, h0, z0, u0, 0.0F, tr, tg, tb, a0, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
            FxDraw.vertex(consumer, pose, x0, y, z0, u0, 1.0F, br, bg, bb, a0, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
            FxDraw.vertex(consumer, pose, x1, y, z1, u1, 1.0F, br, bg, bb, a1, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
            FxDraw.vertex(consumer, pose, x1, h1, z1, u1, 0.0F, tr, tg, tb, a1, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
        }
    }

    private static void renderBeams(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, SnowQueen queen, QueenTrack track, float partialTick, float time) {
        Vec3 look = queen.getViewVector(partialTick);
        Vec3 origin = queen.getPosition(partialTick).add(0.0D, HAND_HEIGHT, 0.0D).add(look.scale(0.6D));
        Vec3[] ends = new Vec3[3];
        boolean[] hits = new boolean[3];
        Vec3[] dirs = new Vec3[3];
        int count = traceBeams(level, queen, track.refract, origin, look, ends, hits, dirs);
        float[] rs = new float[3];
        float[] gs = new float[3];
        float[] bs = new float[3];
        float[] lengths = new float[3];
        for (int i = 0; i < count; i++) {
            rs[i] = PRISM_R;
            gs[i] = PRISM_G;
            bs[i] = PRISM_B;
            if (i > 0) {
                rs[i] = VIOLET_R;
                gs[i] = VIOLET_G;
                bs[i] = VIOLET_B;
            }
            lengths[i] = (float) origin.distanceTo(ends[i]);
        }
        float flicker = 0.9F + 0.1F * Mth.sin(time * 1.7F);

        VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        for (int i = 0; i < count; i++) {
            float u0 = -time * 0.15F;
            FxDraw.beam(beam, pose, origin, ends[i], 0.3F, 0.65F, u0, u0 + lengths[i] / 2.0F, rs[i], gs[i], bs[i], 0.6F * flicker, 0.4F * flicker);
            FxDraw.beam(beam, pose, origin, ends[i], 0.08F, 0.16F, u0, u0 + lengths[i] / 2.0F, ICE_R, ICE_G, ICE_B, 1.0F, 0.85F);
        }
        VertexConsumer energy = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
        for (int i = 0; i < count; i++) {
            float u0 = -time * 0.3F;
            FxDraw.beam(energy, pose, origin, ends[i], 0.2F, 0.45F, u0, u0 + lengths[i] / 3.0F, ICE_R, ICE_G, ICE_B, 0.7F, 0.5F);
        }
        VertexConsumer flakes = buffers.getBuffer(FxRenderTypes.glow(FLAKE));
        for (int i = 0; i < count; i++) {
            Vec3 dir = dirs[i];
            Vec3 side = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 1.0E-4D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            }
            side = side.normalize();
            Vec3 up = side.cross(dir).normalize();
            for (int k = 0; k < 8; k++) {
                double d = (time * 0.6D + k * 2.5D) % Math.max(0.5D, lengths[i]);
                double spin = time * 0.3D + k * 0.785D;
                double rad = 0.35D + d * 0.03D;
                Vec3 p = origin.add(dir.scale(d)).add(side.scale(Math.cos(spin) * rad)).add(up.scale(Math.sin(spin) * rad));
                float edge = Mth.clamp((float) d / 1.5F, 0.0F, 1.0F) * Mth.clamp((lengths[i] - (float) d) / 1.5F, 0.0F, 1.0F);
                FxDraw.sprite(flakes, pose, p.x, p.y, p.z, 0.3F + 0.02F * (float) d, time * 0.15F + k, ICE_R, ICE_G, ICE_B, 0.9F * edge);
            }
        }
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, pose, origin.x, origin.y, origin.z, 1.5F * flicker, 0.0F, PRISM_R, PRISM_G, PRISM_B, 0.7F);
        for (int i = 0; i < count; i++) {
            if (hits[i]) {
                FxDraw.sprite(glow, pose, ends[i].x, ends[i].y, ends[i].z, 2.2F * flicker, 0.0F, rs[i], gs[i], bs[i], 0.6F);
            }
        }
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        FxDraw.sprite(flare, pose, origin.x, origin.y, origin.z, 1.7F, time * 0.05F, ICE_R, ICE_G, ICE_B, 0.85F);
        for (int i = 0; i < count; i++) {
            if (hits[i]) {
                FxDraw.sprite(flare, pose, ends[i].x, ends[i].y, ends[i].z, 1.8F + 0.3F * Mth.sin(time * 0.9F), -time * 0.04F, ICE_R, ICE_G, ICE_B, 0.9F);
            }
        }
        VertexConsumer marks = buffers.getBuffer(FxRenderTypes.glow(MARK));
        for (int i = 0; i < count; i++) {
            if (hits[i]) {
                FxDraw.sprite(marks, pose, ends[i].x, ends[i].y, ends[i].z, 1.3F + 0.2F * Mth.sin(time * 0.9F), time * 0.1F, rs[i], gs[i], bs[i], 0.8F);
            }
        }
    }

    private static void renderDrop(PoseStack.Pose pose, MultiBufferSource buffers, SnowQueen queen, QueenTrack track, float partialTick, float time) {
        Vec3 pos = queen.getPosition(partialTick);
        float height = (float) Math.max(0.0D, pos.y - track.dropGround);
        float size = 1.4F + Math.min(2.0F, height * 0.25F);
        float pulse = 0.55F + 0.25F * Mth.sin(time * 0.5F);
        float ground = (float) track.dropGround;
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, pos.x, ground + 0.04D, pos.z, size * 1.4F, 0.0F, VIOLET_R, VIOLET_G, VIOLET_B, 0.3F * pulse);
        VertexConsumer marks = buffers.getBuffer(FxRenderTypes.glow(MARK));
        FxDraw.flat(marks, pose, pos.x, ground + 0.06D, pos.z, size, -time * 0.08F, VIOLET_R, VIOLET_G, VIOLET_B, pulse);
        FxDraw.flat(marks, pose, pos.x, ground + 0.065D, pos.z, size * 0.55F, time * 0.12F, ICE_R, ICE_G, ICE_B, pulse * 0.7F);
        if (height > 0.5F) {
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.beam(beam, pose, pos, new Vec3(pos.x, ground, pos.z), 0.05F, 0.28F, time * 0.1F, time * 0.1F + height / 2.0F,
                VIOLET_R, VIOLET_G, VIOLET_B, 0.0F, 0.45F * pulse);
        }
    }

    private static void armStrip(VertexConsumer consumer, PoseStack.Pose pose, float cx, float y, float cz, Vec3 dir, float length, float width, float uEnd,
                                 float r, float g, float b, float alpha) {
        if (length < 0.05F || alpha <= 0.01F) {
            return;
        }
        float sx = (float) (-dir.z * width * 0.5D);
        float sz = (float) (dir.x * width * 0.5D);
        float ex = (float) (cx + dir.x * length);
        float ez = (float) (cz + dir.z * length);
        FxDraw.vertex(consumer, pose, cx + sx, y, cz + sz, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, cx - sx, y, cz - sz, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, ex - sx, y, ez - sz, uEnd, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, ex + sx, y, ez + sz, uEnd, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    private static void prism(VertexConsumer consumer, PoseStack.Pose pose, float cx, float y, float cz, float radius, float height, float tip, float yaw,
                              boolean doubleTip, float r, float g, float b, float alpha) {
        if (height <= 0.01F || alpha <= 0.01F) {
            return;
        }
        float[] xs = new float[6];
        float[] zs = new float[6];
        for (int i = 0; i < 6; i++) {
            double a = (yaw + i * 60.0F) * Mth.DEG_TO_RAD;
            xs[i] = (float) (cx + Math.cos(a) * radius);
            zs[i] = (float) (cz + Math.sin(a) * radius);
        }
        float top = y + height;
        float apex = top + tip;
        float bottom = y;
        float low = y - tip;
        for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            float nx = (xs[i] + xs[j]) * 0.5F - cx;
            float nz = (zs[i] + zs[j]) * 0.5F - cz;
            float u0 = i / 6.0F;
            float u1 = (i + 1) / 6.0F;
            float shade = 0.82F + 0.18F * (float) Math.cos(i * Math.PI / 3.0D);
            FxDraw.vertex(consumer, pose, xs[i], top, zs[i], u0, 0.2F, r * shade, g * shade, b * shade, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, xs[i], bottom, zs[i], u0, 1.0F, r * shade, g * shade, b * shade, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, xs[j], bottom, zs[j], u1, 1.0F, r * shade, g * shade, b * shade, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, xs[j], top, zs[j], u1, 0.2F, r * shade, g * shade, b * shade, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, cx, apex, cz, (u0 + u1) * 0.5F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.6F, nz);
            FxDraw.vertex(consumer, pose, cx, apex, cz, (u0 + u1) * 0.5F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.6F, nz);
            FxDraw.vertex(consumer, pose, xs[i], top, zs[i], u0, 0.2F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.6F, nz);
            FxDraw.vertex(consumer, pose, xs[j], top, zs[j], u1, 0.2F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.6F, nz);
            if (doubleTip) {
                FxDraw.vertex(consumer, pose, cx, low, cz, (u0 + u1) * 0.5F, 1.0F, r * 0.8F, g * 0.8F, b * 0.8F, alpha, FxDraw.FULL_BRIGHT, nx, -0.6F, nz);
                FxDraw.vertex(consumer, pose, cx, low, cz, (u0 + u1) * 0.5F, 1.0F, r * 0.8F, g * 0.8F, b * 0.8F, alpha, FxDraw.FULL_BRIGHT, nx, -0.6F, nz);
                FxDraw.vertex(consumer, pose, xs[j], bottom, zs[j], u1, 1.0F, r * 0.8F, g * 0.8F, b * 0.8F, alpha, FxDraw.FULL_BRIGHT, nx, -0.6F, nz);
                FxDraw.vertex(consumer, pose, xs[i], bottom, zs[i], u0, 1.0F, r * 0.8F, g * 0.8F, b * 0.8F, alpha, FxDraw.FULL_BRIGHT, nx, -0.6F, nz);
            }
        }
    }

    private static final class QueenTrack {
        private int age;
        private int pose;
        private int poseStart;
        private int poseDuration = 1;
        private float refract;
        private double dropGround = Double.NaN;
    }

    private static final class SigilFx {
        private final float x;
        private final float y;
        private final float z;
        private final float radius;
        private final int windup;
        private final float start;
        private final float spin;
        private final float width;
        private int age;

        private SigilFx(float x, float y, float z, float radius, int windup, float start, float spin, float width) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.windup = Math.max(1, windup);
            this.start = start;
            this.spin = spin;
            this.width = width;
        }
    }

    private static final class LanceFx {
        private final float x;
        private final float y;
        private final float z;
        private final int warning;
        private final float radius;
        private final float spin;
        private int age;

        private LanceFx(float x, float y, float z, int warning, float radius, float spin) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.warning = Math.max(1, warning);
            this.radius = radius;
            this.spin = spin;
        }
    }

    private static final class CurtainFx {
        private float x;
        private final float y;
        private float z;
        private final float yaw;
        private final float length;
        private final float width;
        private final float height;
        private final int warning;
        private final int active;
        private final float drift;
        private int age;

        private CurtainFx(float x, float y, float z, float yaw, float length, float width, float height, int warning, int active, float drift) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.length = length;
            this.width = width;
            this.height = height;
            this.warning = Math.max(1, warning);
            this.active = active;
            this.drift = drift;
        }
    }
}
