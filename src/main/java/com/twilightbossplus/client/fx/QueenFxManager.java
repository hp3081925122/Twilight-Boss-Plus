package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
import org.joml.Quaternionf;
import twilightforest.entity.boss.SnowQueen;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QueenFxManager {
    private static final ResourceLocation CRYSTAL = tex("crystal");
    private static final ResourceLocation SIGIL = tex("sigil");
    private static final ResourceLocation ARM = tex("arm");
    private static final ResourceLocation AURORA = tex("aurora");
    private static final ResourceLocation BEAM = tex("beam");
    private static final ResourceLocation FLAKE = tex("flake");
    private static final ResourceLocation MARK = tex("mark");
    private static final ResourceLocation SHARD = tex("shard");

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
    private static final int MAX_SHARDS = 300;

    private static final Map<Integer, QueenTrack> TRACKS = new HashMap<>();
    private static final List<SigilFx> SIGILS = new ArrayList<>();
    private static final List<LanceFx> LANCES = new ArrayList<>();
    private static final List<CurtainFx> CURTAINS = new ArrayList<>();
    private static final List<Shard> SHARDS = new ArrayList<>();

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
        SHARDS.clear();
    }

    private static void spawnShards(RandomSource random, double x, double y, double z, int count, double speed) {
        if (!ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        for (int i = 0; i < count && SHARDS.size() < MAX_SHARDS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.4D + random.nextDouble() * 0.6D);
            SHARDS.add(new Shard(new Vec3(x, y, z), new Vec3(Math.cos(angle) * s, 0.18D + random.nextDouble() * 0.25D, Math.sin(angle) * s), random.nextFloat() * 360.0F));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            if (!TRACKS.isEmpty() || !SIGILS.isEmpty() || !CURTAINS.isEmpty()) {
                clearAll();
            }
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        RandomSource random = level.getRandom();
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
        }
        Iterator<SigilFx> sigils = SIGILS.iterator();
        while (sigils.hasNext()) {
            SigilFx sigil = sigils.next();
            sigil.age++;
            if (sigil.age == sigil.windup) {
                float angle = sigil.start + sigil.spin;
                for (int i = 0; i < 6; i++) {
                    Vec3 dir = forward(angle + i * 60.0F);
                    for (int k = 1; k <= 3; k++) {
                        double d = sigil.radius * k / 3.5D;
                        spawnShards(random, sigil.x + dir.x * d, sigil.y + 0.6D, sigil.z + dir.z * d, 2, 0.12D);
                    }
                }
            }
            if (sigil.age > sigil.windup + SIGIL_AFTER) {
                sigils.remove();
            }
        }
        Iterator<LanceFx> lances = LANCES.iterator();
        while (lances.hasNext()) {
            LanceFx lance = lances.next();
            lance.age++;
            if (lance.age == lance.warning) {
                spawnShards(random, lance.x, lance.y + 0.5D, lance.z, 8, 0.2D);
            } else if (lance.age == lance.warning + LANCE_HOLD) {
                spawnShards(random, lance.x, lance.y + 1.5D, lance.z, 6, 0.12D);
            }
            if (lance.age > lance.warning + LANCE_HOLD + LANCE_FADE) {
                lances.remove();
            }
        }
        Iterator<CurtainFx> curtains = CURTAINS.iterator();
        while (curtains.hasNext()) {
            CurtainFx curtain = curtains.next();
            curtain.age++;
            if (curtain.age > curtain.warning) {
                Vec3 axis = forward(curtain.yaw);
                curtain.x += -axis.z * curtain.drift;
                curtain.z += axis.x * curtain.drift;
            }
            if (curtain.age > curtain.warning + curtain.active) {
                curtains.remove();
            }
        }
        Iterator<Shard> shards = SHARDS.iterator();
        while (shards.hasNext()) {
            Shard shard = shards.next();
            shard.prev = shard.pos;
            shard.pos = shard.pos.add(shard.vel);
            shard.vel = new Vec3(shard.vel.x * 0.92D, shard.vel.y - 0.045D, shard.vel.z * 0.92D);
            if (++shard.age > 20) {
                shards.remove();
            }
        }
    }

    private static boolean hasWork() {
        return !TRACKS.isEmpty() || !SIGILS.isEmpty() || !LANCES.isEmpty() || !CURTAINS.isEmpty() || !SHARDS.isEmpty();
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
        Vec3 camera = event.getCamera().getPosition();
        Quaternionf cameraRotation = event.getCamera().rotation();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (SigilFx sigil : SIGILS) {
            renderSigil(poseStack, buffers, sigil, partialTick);
        }
        for (LanceFx lance : LANCES) {
            renderLance(poseStack, buffers, lance, partialTick);
        }
        for (CurtainFx curtain : CURTAINS) {
            renderCurtain(poseStack, buffers, curtain, partialTick);
        }
        for (Map.Entry<Integer, QueenTrack> entry : TRACKS.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof SnowQueen queen) || !queen.isAlive()) {
                continue;
            }
            QueenTrack track = entry.getValue();
            float time = track.age + partialTick;
            if (queen.isBreathing()) {
                renderBeams(poseStack, buffers, level, queen, track, partialTick, camera, cameraRotation, time);
            }
            if (!Double.isNaN(track.dropGround)) {
                Vec3 pos = queen.getPosition(partialTick);
                float height = (float) Math.max(0.0D, pos.y - track.dropGround);
                float size = 1.4F + Math.min(2.0F, height * 0.25F);
                float pulse = 0.55F + 0.25F * Mth.sin(time * 0.5F);
                VertexConsumer marks = buffers.getBuffer(RenderType.entityTranslucent(MARK));
                FxDraw.flatSquare(marks, poseStack.last(), (float) pos.x, (float) track.dropGround + 0.06F, (float) pos.z, size, -time * 0.08F,
                    VIOLET_R, VIOLET_G, VIOLET_B, pulse, FxDraw.FULL_BRIGHT);
            }
            if (decorative) {
                Vec3 pos = queen.getPosition(partialTick);
                VertexConsumer crystals = buffers.getBuffer(RenderType.entityTranslucent(CRYSTAL));
                for (int i = 0; i < 3; i++) {
                    double angle = time * 0.06D + i * Math.PI * 2.0D / 3.0D;
                    float x = (float) (pos.x + Math.cos(angle) * 1.1D);
                    float z = (float) (pos.z + Math.sin(angle) * 1.1D);
                    float y = (float) (pos.y + 1.3D + Math.sin(time * 0.1D + i * 2.0D) * 0.2D);
                    prism(poseStack, crystals, x, y, z, 0.09F, 0.35F, 0.15F, time * 3.0F + i * 40.0F, true, ICE_R, ICE_G, ICE_B, 0.85F);
                }
            }
        }
        if (decorative && !SHARDS.isEmpty()) {
            VertexConsumer shards = buffers.getBuffer(RenderType.entityTranslucent(SHARD));
            for (Shard shard : SHARDS) {
                float life = 1.0F - (shard.age + partialTick) / 20.0F;
                if (life <= 0.0F) {
                    continue;
                }
                Vec3 pos = shard.prev.lerp(shard.pos, partialTick);
                billboard(poseStack, shards, cameraRotation, pos.x, pos.y, pos.z, 0.18F, shard.spin + shard.age * 20.0F, ICE_R, ICE_G, ICE_B, Math.min(1.0F, life * 2.0F));
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderSigil(PoseStack poseStack, MultiBufferSource buffers, SigilFx sigil, float partialTick) {
        float age = sigil.age + partialTick;
        float charge = Mth.clamp(age / sigil.windup, 0.0F, 1.0F);
        float angle = sigil.start + sigil.spin * charge;
        float fade = 1.0F;
        if (age > sigil.windup) {
            fade = Mth.clamp(1.0F - (age - sigil.windup) / SIGIL_AFTER, 0.0F, 1.0F);
        }
        float grow = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
        VertexConsumer decal = buffers.getBuffer(RenderType.entityTranslucent(SIGIL));
        groundDecal(poseStack, decal, sigil.x, sigil.y + 0.04F, sigil.z, sigil.radius * (0.6F + 0.4F * grow), angle, PRISM_R, PRISM_G, PRISM_B, (0.35F + 0.35F * charge) * fade);
        VertexConsumer arms = buffers.getBuffer(RenderType.entityTranslucent(ARM));
        for (int i = 0; i < 6; i++) {
            Vec3 dir = forward(angle + i * 60.0F);
            if (age <= sigil.windup) {
                armStrip(poseStack, arms, sigil.x, sigil.y + 0.06F, sigil.z, dir, sigil.radius * charge, sigil.width * 1.2F, charge, ICE_R, ICE_G, ICE_B, 0.4F + 0.6F * charge);
            } else {
                armStrip(poseStack, arms, sigil.x, sigil.y + 0.06F, sigil.z, dir, sigil.radius, sigil.width * 1.2F, 1.0F, ICE_R, ICE_G, ICE_B, fade);
            }
        }
        if (age <= sigil.windup) {
            return;
        }
        float after = age - sigil.windup;
        float rise = Mth.clamp(after / 3.0F, 0.0F, 1.0F);
        float sink = Mth.clamp((after - (SIGIL_AFTER - 8)) / 8.0F, 0.0F, 1.0F);
        float scale = rise * (1.0F - sink);
        if (scale <= 0.01F) {
            return;
        }
        VertexConsumer crystals = buffers.getBuffer(RenderType.entityTranslucent(CRYSTAL));
        for (int i = 0; i < 6; i++) {
            Vec3 dir = forward(angle + i * 60.0F);
            int count = Math.max(3, (int) (sigil.radius / 1.1F));
            for (int k = 0; k < count; k++) {
                float d = sigil.radius * (k + 0.7F) / (count + 0.2F);
                float height = (0.7F + 1.1F * (1.0F - (float) k / count)) * scale;
                float radius = sigil.width * 0.32F * (1.0F - 0.3F * k / count);
                float x = (float) (sigil.x + dir.x * d);
                float z = (float) (sigil.z + dir.z * d);
                prism(poseStack, crystals, x, sigil.y, z, radius, height, height * 0.45F, angle + k * 17.0F, false, ICE_R, ICE_G, ICE_B, 0.9F);
            }
        }
    }

    private static void renderLance(PoseStack poseStack, MultiBufferSource buffers, LanceFx lance, float partialTick) {
        float age = lance.age + partialTick;
        if (age < lance.warning) {
            float t = Mth.clamp(age / lance.warning, 0.0F, 1.0F);
            VertexConsumer marks = buffers.getBuffer(RenderType.entityTranslucent(MARK));
            FxDraw.flatSquare(marks, poseStack.last(), lance.x, lance.y + 0.05F, lance.z, lance.radius * (1.9F - 0.9F * t), age * 0.12F + lance.spin,
                PRISM_R, PRISM_G, PRISM_B, 0.45F + 0.5F * t, FxDraw.FULL_BRIGHT);
            return;
        }
        float after = age - lance.warning;
        float rise = Mth.clamp(after / 3.0F, 0.0F, 1.0F);
        float alpha = 0.92F;
        float shrink = 1.0F;
        if (after > LANCE_HOLD) {
            float t = Mth.clamp((after - LANCE_HOLD) / LANCE_FADE, 0.0F, 1.0F);
            alpha = 0.92F * (1.0F - t);
            shrink = 1.0F - 0.4F * t;
        }
        VertexConsumer crystals = buffers.getBuffer(RenderType.entityTranslucent(CRYSTAL));
        float height = 3.2F * rise * shrink;
        prism(poseStack, crystals, lance.x, lance.y - 0.2F, lance.z, lance.radius * 0.55F * shrink, height, height * 0.35F, lance.spin, false, ICE_R, ICE_G, ICE_B, alpha);
        for (int i = 0; i < 3; i++) {
            float a = lance.spin + i * 120.0F + 30.0F;
            Vec3 dir = forward(a);
            float side = 1.6F * rise * shrink;
            prism(poseStack, crystals, (float) (lance.x + dir.x * lance.radius * 0.6F), lance.y - 0.2F, (float) (lance.z + dir.z * lance.radius * 0.6F),
                lance.radius * 0.25F * shrink, side, side * 0.4F, a, false, PRISM_R, PRISM_G, PRISM_B, alpha);
        }
        if (after < 6.0F) {
            VertexConsumer marks = buffers.getBuffer(RenderType.entityTranslucent(MARK));
            float t = after / 6.0F;
            FxDraw.flatSquare(marks, poseStack.last(), lance.x, lance.y + 0.05F, lance.z, lance.radius * (1.0F + 1.2F * t), lance.spin,
                ICE_R, ICE_G, ICE_B, 0.9F * (1.0F - t), FxDraw.FULL_BRIGHT);
        }
    }

    private static void renderCurtain(PoseStack poseStack, MultiBufferSource buffers, CurtainFx curtain, float partialTick) {
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
        VertexConsumer ground = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
        float half = curtain.length * 0.5F;
        float sx = (float) (-axis.z * curtain.width * 0.5F);
        float sz = (float) (axis.x * curtain.width * 0.5F);
        float x0 = (float) (cx - axis.x * half);
        float z0 = (float) (cz - axis.z * half);
        float x1 = (float) (cx + axis.x * half);
        float z1 = (float) (cz + axis.z * half);
        float y = curtain.y + 0.05F;
        float scroll = age * 0.05F;
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(ground, pose, x0 + sx, y, z0 + sz, scroll, 0.0F, GREEN_R, GREEN_G, GREEN_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(ground, pose, x0 - sx, y, z0 - sz, scroll, 1.0F, GREEN_R, GREEN_G, GREEN_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(ground, pose, x1 - sx, y, z1 - sz, scroll + curtain.length / 3.0F, 1.0F, GREEN_R, GREEN_G, GREEN_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(ground, pose, x1 + sx, y, z1 + sz, scroll + curtain.length / 3.0F, 0.0F, GREEN_R, GREEN_G, GREEN_B, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        VertexConsumer ribbon = buffers.getBuffer(RenderType.entityTranslucent(AURORA));
        ribbon(poseStack, ribbon, cx, curtain.y, cz, axis, curtain.length, curtain.height * heightScale, age, 0.0F, 0.0F, alpha);
        ribbon(poseStack, ribbon, cx, curtain.y + 0.4F, cz, axis, curtain.length * 0.9F, curtain.height * heightScale * 0.8F, age, 0.35F, 2.1F, alpha * 0.55F);
    }

    private static void ribbon(PoseStack poseStack, VertexConsumer consumer, float cx, float y, float cz, Vec3 axis, float length, float height, float age,
                               float offset, float phase, float alpha) {
        int segments = 18;
        PoseStack.Pose pose = poseStack.last();
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
            FxDraw.vertex(consumer, pose, x0, top, z0, u0, 0.0F, 1.0F, 1.0F, 1.0F, a0, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
            FxDraw.vertex(consumer, pose, x0, y, z0, u0, 1.0F, 1.0F, 1.0F, 1.0F, a0, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
            FxDraw.vertex(consumer, pose, x1, y, z1, u1, 1.0F, 1.0F, 1.0F, 1.0F, a1, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
            FxDraw.vertex(consumer, pose, x1, top, z1, u1, 0.0F, 1.0F, 1.0F, 1.0F, a1, FxDraw.FULL_BRIGHT, px, 0.0F, pz);
        }
    }

    private static void renderBeams(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, SnowQueen queen, QueenTrack track, float partialTick,
                                    Vec3 camera, Quaternionf cameraRotation, float time) {
        Vec3 look = queen.getViewVector(partialTick);
        Vec3 origin = queen.getPosition(partialTick).add(0.0D, HAND_HEIGHT, 0.0D).add(look.scale(0.6D));
        VertexConsumer beam = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
        VertexConsumer flakes = buffers.getBuffer(RenderType.entityTranslucent(FLAKE));
        int beams = 1;
        if (track.refract > 0.0F) {
            beams = 3;
        }
        for (int i = 0; i < beams; i++) {
            Vec3 dir = look;
            float tint = 1.0F;
            if (i == 1) {
                dir = look.yRot(track.refract * Mth.DEG_TO_RAD);
                tint = 0.0F;
            } else if (i == 2) {
                dir = look.yRot(-track.refract * Mth.DEG_TO_RAD);
                tint = 0.0F;
            }
            Vec3 end = origin.add(dir.scale(BEAM_LENGTH));
            HitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, queen));
            if (hit.getType() != HitResult.Type.MISS) {
                end = hit.getLocation();
            }
            float r = Mth.lerp(tint, VIOLET_R, PRISM_R);
            float g = Mth.lerp(tint, VIOLET_G, PRISM_G);
            float b = Mth.lerp(tint, VIOLET_B, PRISM_B);
            beamQuad(poseStack, beam, camera, origin, end, 1.1F, time, r, g, b, 0.45F);
            beamQuad(poseStack, beam, camera, origin, end, 0.4F, time * 1.6F, ICE_R, ICE_G, ICE_B, 0.95F);
            double length = origin.distanceTo(end);
            Vec3 side = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 1.0E-4D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            }
            side = side.normalize();
            Vec3 up = side.cross(dir).normalize();
            for (int k = 0; k < 8; k++) {
                double d = ((time * 0.6D + k * 2.5D) % length);
                double spin = time * 0.3D + k * 0.785D;
                double rad = 0.45D;
                Vec3 p = origin.add(dir.scale(d)).add(side.scale(Math.cos(spin) * rad)).add(up.scale(Math.sin(spin) * rad));
                billboard(poseStack, flakes, cameraRotation, p.x, p.y, p.z, 0.32F, time * 8.0F + k * 45.0F, ICE_R, ICE_G, ICE_B, 0.9F);
            }
            VertexConsumer marks = buffers.getBuffer(RenderType.entityTranslucent(MARK));
            if (hit.getType() != HitResult.Type.MISS) {
                billboard(poseStack, marks, cameraRotation, end.x, end.y, end.z, 1.2F + 0.2F * Mth.sin(time * 0.9F), time * 6.0F, r, g, b, 0.75F);
            }
        }
    }

    private static void beamQuad(PoseStack poseStack, VertexConsumer consumer, Vec3 camera, Vec3 start, Vec3 end, float width, float time,
                                 float r, float g, float b, float alpha) {
        Vec3 dir = end.subtract(start);
        Vec3 mid = start.add(end).scale(0.5D);
        Vec3 side = dir.cross(camera.subtract(mid));
        if (side.lengthSqr() < 1.0E-6D) {
            return;
        }
        side = side.normalize().scale(width * 0.5D);
        float length = (float) dir.length();
        float u0 = -time * 0.15F;
        float u1 = u0 + length / 2.0F;
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(consumer, pose, (float) (start.x + side.x), (float) (start.y + side.y), (float) (start.z + side.z), u0, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (start.x - side.x), (float) (start.y - side.y), (float) (start.z - side.z), u0, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (end.x - side.x), (float) (end.y - side.y), (float) (end.z - side.z), u1, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (end.x + side.x), (float) (end.y + side.y), (float) (end.z + side.z), u1, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    private static void groundDecal(PoseStack poseStack, VertexConsumer consumer, float cx, float y, float cz, float half, float yaw,
                                    float r, float g, float b, float alpha) {
        Vec3 f = forward(yaw);
        float fx = (float) f.x * half;
        float fz = (float) f.z * half;
        float px = -fz;
        float pz = fx;
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(consumer, pose, cx - fx - px, y, cz - fz - pz, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, cx - fx + px, y, cz - fz + pz, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, cx + fx + px, y, cz + fz + pz, 1.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, cx + fx - px, y, cz + fz - pz, 1.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    private static void armStrip(PoseStack poseStack, VertexConsumer consumer, float cx, float y, float cz, Vec3 dir, float length, float width, float uEnd,
                                 float r, float g, float b, float alpha) {
        if (length < 0.05F || alpha <= 0.01F) {
            return;
        }
        float sx = (float) (-dir.z * width * 0.5D);
        float sz = (float) (dir.x * width * 0.5D);
        float ex = (float) (cx + dir.x * length);
        float ez = (float) (cz + dir.z * length);
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(consumer, pose, cx + sx, y, cz + sz, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, cx - sx, y, cz - sz, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, ex - sx, y, ez - sz, uEnd, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, ex + sx, y, ez + sz, uEnd, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    private static void prism(PoseStack poseStack, VertexConsumer consumer, float cx, float y, float cz, float radius, float height, float tip, float yaw,
                              boolean doubleTip, float r, float g, float b, float alpha) {
        if (height <= 0.01F || alpha <= 0.01F) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
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

    private static void billboard(PoseStack poseStack, VertexConsumer consumer, Quaternionf cameraRotation, double x, double y, double z, float size, float spin,
                                  float r, float g, float b, float alpha) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        PoseStack.Pose pose = poseStack.last();
        float half = size * 0.5F;
        FxDraw.vertex(consumer, pose, -half, -half, 0.0F, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, half, -half, 0.0F, 1.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, half, half, 0.0F, 1.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, -half, half, 0.0F, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        poseStack.popPose();
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

    private static final class Shard {
        private Vec3 pos;
        private Vec3 prev;
        private Vec3 vel;
        private final float spin;
        private int age;

        private Shard(Vec3 pos, Vec3 vel, float spin) {
            this.pos = pos;
            this.prev = pos;
            this.vel = vel;
            this.spin = spin;
        }
    }
}
