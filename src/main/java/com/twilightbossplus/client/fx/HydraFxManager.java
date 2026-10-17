package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraHeads;
import com.twilightbossplus.network.HydraFxPacket;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
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
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHead;
import twilightforest.entity.boss.HydraMortar;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HydraFxManager {
    private static final ResourceLocation FLAME = tex("flame");
    private static final ResourceLocation HEAT_CORE = tex("heat_core");
    private static final ResourceLocation FANGS = tex("fangs");
    private static final ResourceLocation ASH_FAN = tex("ash_fan");
    private static final ResourceLocation SCORCH = tex("scorch");
    private static final ResourceLocation SLAG = tex("slag");
    private static final ResourceLocation LAVA = tex("lava");
    private static final ResourceLocation BUBBLE = tex("bubble");
    private static final ResourceLocation HEAT_WAVE = tex("heat_wave");

    private static final float GOLD_R = 1.0F;
    private static final float GOLD_G = 0.71F;
    private static final float GOLD_B = 0.18F;
    private static final float ORANGE_R = 1.0F;
    private static final float ORANGE_G = 0.416F;
    private static final float ORANGE_B = 0.102F;
    private static final float EMBER_R = 0.639F;
    private static final float EMBER_G = 0.149F;
    private static final float EMBER_B = 0.106F;
    private static final float CHAR_R = 0.169F;
    private static final float CHAR_G = 0.114F;
    private static final float CHAR_B = 0.102F;
    private static final float CORE_R = 1.0F;
    private static final float CORE_G = 0.949F;
    private static final float CORE_B = 0.784F;

    private static final double MOUTH_DIST = 3.5D;
    private static final double FLAME_RANGE = 30.0D;
    private static final int FLAME_SEGMENTS = 6;
    private static final int SECTOR_FADE = 6;
    private static final int WAVE_FADE = 6;
    private static final int GEYSER_FADE = 6;
    private static final int FLASH_LIFE = 10;
    private static final int MORTAR_FUSE = 80;
    private static final int MAX_EMBERS = 300;
    private static final int MAX_SCORCHES = 160;
    private static final double WAVE_HEIGHT = 2.5D;

    private static final Set<Integer> HYDRAS = new HashSet<>();
    private static final Set<Integer> MORTARS = new HashSet<>();
    private static final Map<Integer, int[]> HEAD_TRACK = new HashMap<>();
    private static final Map<Integer, Integer> MORTAR_LANDED = new HashMap<>();
    private static final List<Sector> SECTORS = new ArrayList<>();
    private static final List<Scorch> SCORCHES = new ArrayList<>();
    private static final List<Slag> SLAGS = new ArrayList<>();
    private static final List<Flash> FLASHES = new ArrayList<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Geyser> GEYSERS = new ArrayList<>();
    private static final List<Ember> EMBERS = new ArrayList<>();

    private HydraFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/hydra/" + name + ".png");
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        if (type == HydraFxPacket.TAIL_WARN && data.length >= 9) {
            SECTORS.add(new Sector(0, data[0], data[1], data[2], data[3], data[4], data[5], data[6], (int) data[7], (int) data[8]));
        } else if (type == HydraFxPacket.SWEEP_WARN && data.length >= 10) {
            SECTORS.add(new Sector(1, data[0], data[9], data[2], data[3], data[4], data[5], data[6], (int) data[7], (int) data[8]));
        } else if (type == HydraFxPacket.SCORCH && data.length >= 5) {
            if (SCORCHES.size() < MAX_SCORCHES) {
                SCORCHES.add(new Scorch(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4]), true));
            }
        } else if (type == HydraFxPacket.SLAG && data.length >= 8) {
            SLAGS.add(new Slag(new Vec3(data[0], data[1], data[2]), new Vec3(data[3], data[4], data[5]), Math.max(1, (int) data[6]), data[7]));
        } else if (type == HydraFxPacket.SLAG_IMPACT && data.length >= 4) {
            FLASHES.add(new Flash(data[0], data[1], data[2], data[3]));
            if (SCORCHES.size() < MAX_SCORCHES) {
                SCORCHES.add(new Scorch(data[0], data[1], data[2], data[3] * 0.9F, 60, false));
            }
            burstEmbers(level.random, data[0], data[1] + 0.2D, data[2], 14, 0.28D);
        } else if (type == HydraFxPacket.WAVE && data.length >= 5) {
            WAVES.add(new Wave(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4])));
        } else if (type == HydraFxPacket.GEYSER && data.length >= 8) {
            List<Vec3> points = new ArrayList<>();
            for (int i = 5; i + 2 < data.length; i += 3) {
                points.add(new Vec3(data[i], data[i + 1], data[i + 2]));
            }
            GEYSERS.add(new Geyser(points, (int) data[0], (int) data[1], data[2], data[3], (int) data[4]));
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Hydra) {
            HYDRAS.add(event.getEntity().getId());
        } else if (event.getEntity() instanceof HydraMortar) {
            MORTARS.add(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        HYDRAS.remove(event.getEntity().getId());
        MORTARS.remove(event.getEntity().getId());
        MORTAR_LANDED.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        HYDRAS.clear();
        MORTARS.clear();
        HEAD_TRACK.clear();
        MORTAR_LANDED.clear();
        SECTORS.clear();
        SCORCHES.clear();
        SLAGS.clear();
        FLASHES.clear();
        WAVES.clear();
        GEYSERS.clear();
        EMBERS.clear();
    }

    private static void burstEmbers(RandomSource random, double x, double y, double z, int count, double speed) {
        if (!ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        for (int i = 0; i < count && EMBERS.size() < MAX_EMBERS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double horizontal = speed * (0.3D + random.nextDouble() * 0.7D);
            EMBERS.add(new Ember(x, y, z, Math.cos(angle) * horizontal, speed * (0.6D + random.nextDouble()), Math.sin(angle) * horizontal, random));
        }
    }

    private static Vec3 mouthOf(HydraHead head, float partialTick) {
        double x = Mth.lerp(partialTick, head.xo, head.getX());
        double y = Mth.lerp(partialTick, head.yo, head.getY()) + 1.0D;
        double z = Mth.lerp(partialTick, head.zo, head.getZ());
        Vec3 look = head.getViewVector(partialTick);
        return new Vec3(x + look.x * MOUTH_DIST, y + look.y * MOUTH_DIST, z + look.z * MOUTH_DIST);
    }

    @Nullable
    private static Vec3 lookHit(ClientLevel level, HydraHead head, float partialTick) {
        Vec3 source = new Vec3(Mth.lerp(partialTick, head.xo, head.getX()), Mth.lerp(partialTick, head.yo, head.getY()) + 1.0D, Mth.lerp(partialTick, head.zo, head.getZ()));
        Vec3 end = source.add(head.getViewVector(partialTick).scale(FLAME_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(source, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, head));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        return hit.getLocation();
    }

    private static int headAge(HydraHead head) {
        int[] track = HEAD_TRACK.get(head.getId());
        if (track == null) {
            return 0;
        }
        return track[1];
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearAll();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        ClientLevel level = minecraft.level;
        RandomSource random = level.random;
        boolean decorative = ClientConfig.VFX_ENABLED.get();

        Set<Integer> seenHeads = new HashSet<>();
        Iterator<Integer> hydras = HYDRAS.iterator();
        while (hydras.hasNext()) {
            Entity entity = level.getEntity(hydras.next());
            if (!(entity instanceof Hydra hydra) || !hydra.isAlive()) {
                if (entity == null) {
                    hydras.remove();
                }
                continue;
            }
            for (int i = 0; i < hydra.hc.length; i++) {
                HydraHead head = hydra.hc[i].headEntity;
                if (head == null) {
                    continue;
                }
                int state = HydraHeads.stateOf(head);
                seenHeads.add(head.getId());
                int[] track = HEAD_TRACK.get(head.getId());
                if (track == null || track[0] != state) {
                    HEAD_TRACK.put(head.getId(), new int[]{state, 0});
                } else {
                    track[1]++;
                }
                if (!decorative) {
                    continue;
                }
                if (state == HydraHeads.DYING && level.getGameTime() % 2L == 0L) {
                    burstEmbers(random, head.getX(), head.getY() + 1.0D, head.getZ(), 3, 0.18D);
                } else if (state == HydraHeads.FLAMING && level.getGameTime() % 3L == 0L) {
                    Vec3 hit = lookHit(level, head, 1.0F);
                    if (hit != null) {
                        burstEmbers(random, hit.x, hit.y + 0.2D, hit.z, 2, 0.2D);
                    }
                }
            }
        }
        HEAD_TRACK.keySet().retainAll(seenHeads);

        Iterator<Integer> mortars = MORTARS.iterator();
        while (mortars.hasNext()) {
            int id = mortars.next();
            Entity entity = level.getEntity(id);
            if (entity == null) {
                mortars.remove();
                MORTAR_LANDED.remove(id);
                continue;
            }
            if (entity.onGround()) {
                MORTAR_LANDED.merge(id, 1, Integer::sum);
            }
        }

        SECTORS.removeIf(sector -> ++sector.age > sector.warn + sector.active + SECTOR_FADE);
        SCORCHES.removeIf(scorch -> ++scorch.age > scorch.life);
        SLAGS.removeIf(slag -> ++slag.age > slag.flight);
        FLASHES.removeIf(flash -> ++flash.age > FLASH_LIFE);
        WAVES.removeIf(wave -> ++wave.age > wave.expand + WAVE_FADE);

        Iterator<Geyser> geysers = GEYSERS.iterator();
        while (geysers.hasNext()) {
            Geyser geyser = geysers.next();
            geyser.age++;
            if (geyser.age > geyser.delay + geyser.duration + GEYSER_FADE) {
                geysers.remove();
                continue;
            }
            if (decorative && geyser.age >= geyser.delay && geyser.age < geyser.delay + geyser.duration && geyser.age % 2 == 0) {
                for (Vec3 point : geyser.points) {
                    if (random.nextInt(3) == 0) {
                        burstEmbers(random, point.x, point.y + geyser.height, point.z, 1, 0.22D);
                    }
                }
            }
        }

        Iterator<Ember> embers = EMBERS.iterator();
        while (embers.hasNext()) {
            Ember ember = embers.next();
            ember.px = ember.x;
            ember.py = ember.y;
            ember.pz = ember.z;
            ember.x += ember.vx;
            ember.y += ember.vy;
            ember.z += ember.vz;
            ember.vy -= 0.03D;
            ember.vx *= 0.92D;
            ember.vz *= 0.92D;
            if (++ember.age > ember.life) {
                embers.remove();
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
        if (level == null) {
            return;
        }
        if (HYDRAS.isEmpty() && MORTARS.isEmpty() && SECTORS.isEmpty() && SCORCHES.isEmpty() && SLAGS.isEmpty() && FLASHES.isEmpty()
            && WAVES.isEmpty() && GEYSERS.isEmpty() && EMBERS.isEmpty()) {
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

        for (Scorch scorch : SCORCHES) {
            renderScorch(poseStack, buffers, scorch, partialTick);
        }
        for (Sector sector : SECTORS) {
            renderSector(poseStack, buffers, sector, partialTick);
        }
        for (Wave wave : WAVES) {
            renderWave(poseStack, buffers, wave, partialTick);
        }
        for (Geyser geyser : GEYSERS) {
            renderGeyser(poseStack, buffers, geyser, cameraRotation, partialTick);
        }
        for (Slag slag : SLAGS) {
            renderSlag(poseStack, buffers, slag, cameraRotation, partialTick);
        }
        for (Flash flash : FLASHES) {
            renderFlash(poseStack, buffers, flash, partialTick);
        }
        if (HydraConfig.ENABLED.get()) {
            for (int id : HYDRAS) {
                if (level.getEntity(id) instanceof Hydra hydra && hydra.isAlive()) {
                    renderHeads(poseStack, buffers, level, hydra, cameraRotation, partialTick);
                }
            }
            for (int id : MORTARS) {
                Entity mortar = level.getEntity(id);
                if (mortar != null) {
                    renderMortar(poseStack, buffers, mortar, cameraRotation, partialTick);
                }
            }
        }
        if (decorative) {
            renderEmbers(poseStack, buffers, partialTick);
        }

        poseStack.popPose();
        buffers.endBatch();
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

    private static void sectorFill(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float yaw0, float yaw1, float inner, float outer,
                                   float r, float g, float b, float innerAlpha, float outerAlpha) {
        float span = yaw1 - yaw0;
        int segments = Math.max(2, (int) (Math.abs(span) / 6.0F));
        float uRepeat = Math.max(1.0F, Math.abs(span) / 30.0F);
        for (int i = 0; i < segments; i++) {
            float a0 = (yaw0 + span * i / segments) * Mth.DEG_TO_RAD;
            float a1 = (yaw0 + span * (i + 1) / segments) * Mth.DEG_TO_RAD;
            float x0 = -Mth.sin(a0);
            float z0 = Mth.cos(a0);
            float x1 = -Mth.sin(a1);
            float z1 = Mth.cos(a1);
            float u0 = uRepeat * i / segments;
            float u1 = uRepeat * (i + 1) / segments;
            FxDraw.vertex(consumer, pose, (float) (cx + x0 * inner), (float) y, (float) (cz + z0 * inner), u0, 0.0F, r, g, b, innerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) (cx + x0 * outer), (float) y, (float) (cz + z0 * outer), u0, 1.0F, r, g, b, outerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) (cx + x1 * outer), (float) y, (float) (cz + z1 * outer), u1, 1.0F, r, g, b, outerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, (float) (cx + x1 * inner), (float) y, (float) (cz + z1 * inner), u1, 0.0F, r, g, b, innerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void tube(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float radius, float height, int sides, float vOffset,
                             float r, float g, float b, float bottomAlpha, float topAlpha) {
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            float u0 = (float) i / sides;
            float u1 = (float) (i + 1) / sides;
            float v1 = vOffset + height * 0.5F;
            FxDraw.vertex(consumer, pose, (float) (x + c0 * radius), (float) y, (float) (z + s0 * radius), u0, v1, r, g, b, bottomAlpha, FxDraw.FULL_BRIGHT, c0, 0.0F, s0);
            FxDraw.vertex(consumer, pose, (float) (x + c0 * radius), (float) (y + height), (float) (z + s0 * radius), u0, vOffset, r, g, b, topAlpha, FxDraw.FULL_BRIGHT, c0, 0.0F, s0);
            FxDraw.vertex(consumer, pose, (float) (x + c1 * radius), (float) (y + height), (float) (z + s1 * radius), u1, vOffset, r, g, b, topAlpha, FxDraw.FULL_BRIGHT, c1, 0.0F, s1);
            FxDraw.vertex(consumer, pose, (float) (x + c1 * radius), (float) y, (float) (z + s1 * radius), u1, v1, r, g, b, bottomAlpha, FxDraw.FULL_BRIGHT, c1, 0.0F, s1);
        }
    }

    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static void renderSector(PoseStack poseStack, MultiBufferSource buffers, Sector sector, float partialTick) {
        float age = sector.age + partialTick;
        float fade = 1.0F;
        if (age > sector.warn + sector.active) {
            fade = Mth.clamp(1.0F - (age - sector.warn - sector.active) / SECTOR_FADE, 0.0F, 1.0F);
        }
        float r = CHAR_R;
        float g = CHAR_G;
        float b = CHAR_B;
        float hotR = EMBER_R;
        float hotG = EMBER_G;
        float hotB = EMBER_B;
        if (sector.kind == 1) {
            r = EMBER_R;
            g = EMBER_G;
            b = EMBER_B;
            hotR = ORANGE_R;
            hotG = ORANGE_G;
            hotB = ORANGE_B;
        }
        float base = 0.3F + 0.35F * Mth.clamp(age / sector.warn, 0.0F, 1.0F);
        if (age < sector.warn && sector.warn - age < 6.0F) {
            base += 0.15F * Mth.sin(age * 2.0F);
        }
        double y = sector.y + 0.06D;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(ASH_FAN));
        sectorFill(consumer, poseStack.last(), sector.x, y, sector.z, sector.yaw, sector.yaw + sector.sweep, sector.inner, sector.outer,
            r, g, b, base * 0.5F * fade, base * fade);
        if (age >= sector.warn) {
            float progress = Mth.clamp((age - sector.warn) / Math.max(1, sector.active), 0.0F, 1.0F);
            if (sector.kind == 0) {
                progress = ease(progress);
            }
            if (progress > 0.0F) {
                sectorFill(consumer, poseStack.last(), sector.x, y + 0.01D, sector.z, sector.yaw, sector.yaw + sector.sweep * progress, sector.inner, sector.outer,
                    hotR, hotG, hotB, 0.45F * fade, 0.85F * fade);
            }
        }
    }

    private static void renderScorch(PoseStack poseStack, MultiBufferSource buffers, Scorch scorch, float partialTick) {
        float age = scorch.age + partialTick;
        float alpha = Mth.clamp((scorch.life - age) / 20.0F, 0.0F, 1.0F) * Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        if (alpha <= 0.01F) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SCORCH));
        FxDraw.flatSquare(consumer, poseStack.last(), (float) scorch.x, (float) scorch.y + 0.04F, (float) scorch.z, scorch.radius, scorch.spin,
            1.0F, 1.0F, 1.0F, alpha * 0.9F, FxDraw.FULL_BRIGHT);
        if (scorch.burning) {
            float pulse = 0.75F + 0.25F * Mth.sin(age * 0.5F + scorch.spin * 3.0F);
            VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
            FxDraw.flatSquare(glow, poseStack.last(), (float) scorch.x, (float) scorch.y + 0.05F, (float) scorch.z, scorch.radius * 0.8F * pulse, -scorch.spin,
                ORANGE_R, ORANGE_G, ORANGE_B, alpha * 0.45F, FxDraw.FULL_BRIGHT);
        }
    }

    private static Vec3 slagPos(Slag slag, float t) {
        Vec3 pos = slag.from.lerp(slag.to, t);
        return pos.add(0.0D, slag.arc * 4.0D * t * (1.0D - t), 0.0D);
    }

    private static void renderSlag(PoseStack poseStack, MultiBufferSource buffers, Slag slag, Quaternionf cameraRotation, float partialTick) {
        float age = slag.age + partialTick;
        float t = Mth.clamp(age / slag.flight, 0.0F, 1.0F);
        VertexConsumer marker = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
        float size = slag.radius * (1.6F - 0.6F * t);
        FxDraw.flatSquare(marker, poseStack.last(), (float) slag.to.x, (float) slag.to.y + 0.07F, (float) slag.to.z, size, age * 0.05F,
            ORANGE_R, ORANGE_G, ORANGE_B, 0.3F + 0.55F * t, FxDraw.FULL_BRIGHT);
        FxDraw.flatSquare(marker, poseStack.last(), (float) slag.to.x, (float) slag.to.y + 0.075F, (float) slag.to.z, slag.radius * 0.35F, -age * 0.08F,
            CORE_R, CORE_G, CORE_B, 0.2F + 0.6F * t, FxDraw.FULL_BRIGHT);
        for (int k = 4; k >= 1; k--) {
            float trail = t - k * 0.035F;
            if (trail <= 0.0F) {
                continue;
            }
            Vec3 p = slagPos(slag, trail);
            billboard(poseStack, marker, cameraRotation, p.x, p.y, p.z, 0.45F - k * 0.07F, age * 15.0F + k * 40.0F,
                ORANGE_R, ORANGE_G, ORANGE_B, 0.6F - k * 0.12F);
        }
        Vec3 pos = slagPos(slag, t);
        VertexConsumer body = buffers.getBuffer(RenderType.entityTranslucent(SLAG));
        billboard(poseStack, body, cameraRotation, pos.x, pos.y, pos.z, 0.55F, age * 20.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderFlash(PoseStack poseStack, MultiBufferSource buffers, Flash flash, float partialTick) {
        float t = Mth.clamp((flash.age + partialTick) / FLASH_LIFE, 0.0F, 1.0F);
        float grow = 1.0F - (1.0F - t) * (1.0F - t);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
        FxDraw.flatSquare(consumer, poseStack.last(), (float) flash.x, (float) flash.y + 0.08F, (float) flash.z, flash.radius * (0.6F + 0.9F * grow), t * 2.0F,
            CORE_R, CORE_G, CORE_B, (1.0F - t) * 0.9F, FxDraw.FULL_BRIGHT);
    }

    private static void renderWave(PoseStack poseStack, MultiBufferSource buffers, Wave wave, float partialTick) {
        float age = wave.age + partialTick;
        float t = Mth.clamp(age / wave.expand, 0.0F, 1.0F);
        float radius = wave.maxRadius * (1.0F - (1.0F - t) * (1.0F - t));
        if (radius <= 0.1F) {
            return;
        }
        float alpha = 0.85F * (1.0F - t * 0.5F);
        if (age > wave.expand) {
            alpha *= Mth.clamp(1.0F - (age - wave.expand) / WAVE_FADE, 0.0F, 1.0F);
        }
        int sides = Mth.clamp((int) (radius * 6.0F), 16, 64);
        VertexConsumer wall = buffers.getBuffer(RenderType.entityTranslucent(HEAT_WAVE));
        tube(wall, poseStack.last(), wave.x, wave.y, wave.z, radius, (float) WAVE_HEIGHT, sides, -age * 0.12F,
            ORANGE_R, ORANGE_G, ORANGE_B, alpha, 0.0F);
        poseStack.pushPose();
        poseStack.translate(wave.x, wave.y + 0.05D, wave.z);
        VertexConsumer ground = buffers.getBuffer(RenderType.entityTranslucent(SCORCH));
        sectorFill(ground, poseStack.last(), 0.0D, 0.0D, 0.0D, 0.0F, 360.0F, Math.max(0.0F, radius - 1.4F), radius, 1.0F, 1.0F, 1.0F, 0.0F, alpha * 0.8F);
        poseStack.popPose();
    }

    private static void renderGeyser(PoseStack poseStack, MultiBufferSource buffers, Geyser geyser, Quaternionf cameraRotation, float partialTick) {
        float age = geyser.age + partialTick;
        int warnStart = geyser.delay - geyser.warn;
        if (age < warnStart) {
            return;
        }
        if (age < geyser.delay) {
            float progress = Mth.clamp((age - warnStart) / Math.max(1, geyser.warn), 0.0F, 1.0F);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(BUBBLE));
            for (int i = 0; i < geyser.points.size(); i++) {
                Vec3 p = geyser.points.get(i);
                float pulse = 0.85F + 0.15F * Mth.sin(age * 1.2F + i * 1.7F);
                FxDraw.flatSquare(consumer, poseStack.last(), (float) p.x, (float) p.y + 0.06F, (float) p.z, geyser.columnRadius * (0.5F + 0.5F * progress) * pulse,
                    age * 0.06F + i, ORANGE_R, ORANGE_G, ORANGE_B, 0.45F + 0.5F * progress, FxDraw.FULL_BRIGHT);
            }
            return;
        }
        float active = age - geyser.delay;
        float rise = Mth.clamp(active / 3.0F, 0.0F, 1.0F);
        float sink = 1.0F;
        if (active > geyser.duration) {
            sink = Mth.clamp(1.0F - (active - geyser.duration) / GEYSER_FADE, 0.0F, 1.0F);
        }
        float height = geyser.height * rise * sink;
        if (height <= 0.05F) {
            return;
        }
        VertexConsumer lava = buffers.getBuffer(RenderType.entityTranslucent(LAVA));
        for (Vec3 p : geyser.points) {
            tube(lava, poseStack.last(), p.x, p.y, p.z, geyser.columnRadius * 0.75F, height, 8, -age * 0.15F, 1.0F, 1.0F, 1.0F, 1.0F, 0.75F);
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
        for (int i = 0; i < geyser.points.size(); i++) {
            Vec3 p = geyser.points.get(i);
            billboard(poseStack, glow, cameraRotation, p.x, p.y + height, p.z, geyser.columnRadius * 1.1F, age * 12.0F + i * 50.0F,
                GOLD_R, GOLD_G, GOLD_B, 0.8F * sink);
        }
    }

    private static void renderHeads(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Hydra hydra, Quaternionf cameraRotation, float partialTick) {
        for (int i = 0; i < hydra.hc.length; i++) {
            HydraHead head = hydra.hc[i].headEntity;
            if (head == null) {
                continue;
            }
            int state = HydraHeads.stateOf(head);
            float age = headAge(head) + partialTick;
            if (state == HydraHeads.FLAMING) {
                renderFlame(poseStack, buffers, level, head, cameraRotation, age, partialTick);
            } else if (state == HydraHeads.FLAME_BEGINNING) {
                Vec3 mouth = mouthOf(head, partialTick);
                float t = Mth.clamp(age / 40.0F, 0.0F, 1.0F);
                VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
                billboard(poseStack, glow, cameraRotation, mouth.x, mouth.y, mouth.z, 0.3F + 1.0F * t, age * 9.0F,
                    ORANGE_R, ORANGE_G, ORANGE_B, 0.4F + 0.5F * t);
                billboard(poseStack, glow, cameraRotation, mouth.x, mouth.y, mouth.z, 0.15F + 0.4F * t, -age * 13.0F,
                    CORE_R, CORE_G, CORE_B, 0.3F + 0.6F * t);
            } else if (state == HydraHeads.BITE_BEGINNING || state == HydraHeads.BITE_READY) {
                Vec3 mouth = mouthOf(head, partialTick);
                float pulse = 0.8F + 0.2F * Mth.sin(age * 0.8F);
                VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
                billboard(poseStack, glow, cameraRotation, mouth.x, mouth.y, mouth.z, 0.7F * pulse, age * 6.0F,
                    EMBER_R, EMBER_G, EMBER_B, 0.75F);
                if (state == HydraHeads.BITE_READY) {
                    Vec3 hit = lookHit(level, head, partialTick);
                    if (hit != null) {
                        VertexConsumer fangs = buffers.getBuffer(RenderType.entityTranslucent(FANGS));
                        float yaw = Mth.lerp(partialTick, head.yRotO, head.getYRot()) * Mth.DEG_TO_RAD;
                        FxDraw.flatSquare(fangs, poseStack.last(), (float) hit.x, (float) hit.y + 0.06F, (float) hit.z, 1.3F * pulse, -yaw,
                            EMBER_R, EMBER_G, EMBER_B, 0.55F + 0.3F * Mth.clamp(age / 40.0F, 0.0F, 1.0F), FxDraw.FULL_BRIGHT);
                    }
                }
            } else if (state == HydraHeads.MORTAR_BEGINNING || state == HydraHeads.MORTAR_SHOOTING) {
                Vec3 mouth = mouthOf(head, partialTick);
                float size = 0.6F;
                if (state == HydraHeads.MORTAR_SHOOTING) {
                    size += 0.4F * (1.0F - (age % 10.0F) / 10.0F);
                }
                VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
                billboard(poseStack, glow, cameraRotation, mouth.x, mouth.y, mouth.z, size, age * 7.0F, GOLD_R, GOLD_G, GOLD_B, 0.8F);
            } else if (state == HydraHeads.DYING) {
                double x = Mth.lerp(partialTick, head.xo, head.getX());
                double y = Mth.lerp(partialTick, head.yo, head.getY()) + 1.0D;
                double z = Mth.lerp(partialTick, head.zo, head.getZ());
                float fade = Mth.clamp(1.0F - age / 70.0F, 0.0F, 1.0F);
                VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
                billboard(poseStack, glow, cameraRotation, x, y, z, 1.4F, age * 4.0F, EMBER_R, EMBER_G, EMBER_B, 0.7F * fade);
            }
        }
    }

    private static void renderFlame(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, HydraHead head, Quaternionf cameraRotation, float age, float partialTick) {
        Vec3 mouth = mouthOf(head, partialTick);
        Vec3 dir = head.getViewVector(partialTick).normalize();
        Vec3 hit = lookHit(level, head, partialTick);
        double length = FLAME_RANGE - MOUTH_DIST;
        if (hit != null) {
            length = Math.max(1.0D, hit.distanceTo(mouth));
        }
        double reach = Math.min(length, 2.0D + age * 4.0D);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(dir.y) > 0.95D) {
            up = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 s1 = dir.cross(up).normalize();
        Vec3 s2 = dir.cross(s1).normalize();
        float scroll = age * 0.35F;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(FLAME));
        PoseStack.Pose pose = poseStack.last();
        for (int k = 0; k < 3; k++) {
            float angle = k * Mth.PI / 3.0F + age * 0.05F;
            Vec3 side = s1.scale(Mth.cos(angle)).add(s2.scale(Mth.sin(angle)));
            for (int j = 0; j < FLAME_SEGMENTS; j++) {
                float t0 = (float) j / FLAME_SEGMENTS;
                float t1 = (float) (j + 1) / FLAME_SEGMENTS;
                float w0 = 0.3F + 1.5F * t0 + 0.12F * Mth.sin(age * 1.3F + j * 2.1F + k);
                float w1 = 0.3F + 1.5F * t1 + 0.12F * Mth.sin(age * 1.3F + (j + 1) * 2.1F + k);
                Vec3 c0 = mouth.add(dir.scale(reach * t0));
                Vec3 c1 = mouth.add(dir.scale(reach * t1));
                float u0 = (float) (reach * t0 / 3.0D) - scroll;
                float u1 = (float) (reach * t1 / 3.0D) - scroll;
                float a0 = 0.95F - 0.45F * t0;
                float a1 = 0.95F - 0.45F * t1;
                float g0 = Mth.lerp(t0, CORE_G, ORANGE_G);
                float g1 = Mth.lerp(t1, CORE_G, ORANGE_G);
                float b0 = Mth.lerp(t0, CORE_B, ORANGE_B);
                float b1 = Mth.lerp(t1, CORE_B, ORANGE_B);
                Vec3 p00 = c0.subtract(side.scale(w0));
                Vec3 p01 = c0.add(side.scale(w0));
                Vec3 p11 = c1.add(side.scale(w1));
                Vec3 p10 = c1.subtract(side.scale(w1));
                FxDraw.vertex(consumer, pose, (float) p00.x, (float) p00.y, (float) p00.z, u0, 0.0F, 1.0F, g0, b0, a0, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(consumer, pose, (float) p01.x, (float) p01.y, (float) p01.z, u0, 1.0F, 1.0F, g0, b0, a0, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(consumer, pose, (float) p11.x, (float) p11.y, (float) p11.z, u1, 1.0F, 1.0F, g1, b1, a1, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(consumer, pose, (float) p10.x, (float) p10.y, (float) p10.z, u1, 0.0F, 1.0F, g1, b1, a1, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            }
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
        billboard(poseStack, glow, cameraRotation, mouth.x, mouth.y, mouth.z, 0.9F, age * 11.0F, CORE_R, CORE_G, CORE_B, 0.85F);
        if (hit != null && reach >= length - 0.01D) {
            float pulse = 0.85F + 0.15F * Mth.sin(age * 1.7F);
            billboard(poseStack, glow, cameraRotation, hit.x, hit.y + 0.4D, hit.z, 1.6F * pulse, -age * 9.0F, ORANGE_R, ORANGE_G, ORANGE_B, 0.8F);
        }
    }

    private static void renderMortar(PoseStack poseStack, MultiBufferSource buffers, Entity mortar, Quaternionf cameraRotation, float partialTick) {
        double x = Mth.lerp(partialTick, mortar.xo, mortar.getX());
        double y = Mth.lerp(partialTick, mortar.yo, mortar.getY());
        double z = Mth.lerp(partialTick, mortar.zo, mortar.getZ());
        float age = mortar.tickCount + partialTick;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.9F);
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(HEAT_CORE));
        billboard(poseStack, glow, cameraRotation, x, y + mortar.getBbHeight() * 0.5D, z, 0.9F * pulse, age * 8.0F, GOLD_R, GOLD_G, GOLD_B, 0.7F);
        Integer landed = MORTAR_LANDED.get(mortar.getId());
        if (landed == null) {
            return;
        }
        float t = Mth.clamp((landed + partialTick) / MORTAR_FUSE, 0.0F, 1.0F);
        float flicker = 1.0F;
        if (t > 0.75F) {
            flicker = 0.7F + 0.3F * Mth.sin(age * 2.5F);
        }
        VertexConsumer ground = buffers.getBuffer(RenderType.entityTranslucent(SCORCH));
        FxDraw.flatSquare(ground, poseStack.last(), (float) x, (float) y + 0.05F, (float) z, 0.6F + 1.6F * t, age * 0.02F, 1.0F, 1.0F, 1.0F, 0.5F + 0.4F * t, FxDraw.FULL_BRIGHT);
        FxDraw.flatSquare(glow, poseStack.last(), (float) x, (float) y + 0.06F, (float) z, (0.4F + 1.2F * t) * flicker, -age * 0.04F,
            ORANGE_R, ORANGE_G, ORANGE_B, 0.35F + 0.45F * t, FxDraw.FULL_BRIGHT);
    }

    private static void renderEmbers(PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
        if (EMBERS.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(LAVA));
        for (Ember ember : EMBERS) {
            float age = ember.age + partialTick;
            float size = ember.size * Mth.clamp(1.0F - (age - ember.life * 0.5F) / (ember.life * 0.5F), 0.0F, 1.0F);
            if (size <= 0.0F) {
                continue;
            }
            double x = Mth.lerp(partialTick, ember.px, ember.x);
            double y = Mth.lerp(partialTick, ember.py, ember.y);
            double z = Mth.lerp(partialTick, ember.pz, ember.z);
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * ember.spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * ember.spin * 0.7F));
            FxDraw.box(consumer, poseStack.last(), -size, -size, -size, size, size, size, ember.u0, ember.v0, ember.u0 + 0.25F, ember.v0 + 0.25F,
                1.0F, 1.0F, 1.0F, 1.0F, FxDraw.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

    private static final class Sector {
        final int kind;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float sweep;
        final float inner;
        final float outer;
        final int warn;
        final int active;
        int age;

        Sector(int kind, double x, double y, double z, float yaw, float sweep, float inner, float outer, int warn, int active) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.sweep = sweep;
            this.inner = inner;
            this.outer = outer;
            this.warn = Math.max(1, warn);
            this.active = Math.max(1, active);
        }
    }

    private static final class Scorch {
        final double x;
        final double y;
        final double z;
        final float radius;
        final int life;
        final boolean burning;
        final float spin;
        int age;

        Scorch(double x, double y, double z, float radius, int life, boolean burning) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.life = life;
            this.burning = burning;
            this.spin = (float) ((x * 7.0D + z * 13.0D) % Mth.TWO_PI);
        }
    }

    private static final class Slag {
        final Vec3 from;
        final Vec3 to;
        final int flight;
        final float radius;
        final double arc;
        int age;

        Slag(Vec3 from, Vec3 to, int flight, float radius) {
            this.from = from;
            this.to = to;
            this.flight = flight;
            this.radius = radius;
            this.arc = 3.0D + from.distanceTo(to) * 0.3D;
        }
    }

    private static final class Flash {
        final double x;
        final double y;
        final double z;
        final float radius;
        int age;

        Flash(double x, double y, double z, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
    }

    private static final class Wave {
        final double x;
        final double y;
        final double z;
        final float maxRadius;
        final int expand;
        int age;

        Wave(double x, double y, double z, float maxRadius, int expand) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxRadius = maxRadius;
            this.expand = expand;
        }
    }

    private static final class Geyser {
        final List<Vec3> points;
        final int delay;
        final int warn;
        final float columnRadius;
        final float height;
        final int duration;
        int age;

        Geyser(List<Vec3> points, int delay, int warn, float columnRadius, float height, int duration) {
            this.points = points;
            this.delay = delay;
            this.warn = warn;
            this.columnRadius = columnRadius;
            this.height = height;
            this.duration = duration;
        }
    }

    private static final class Ember {
        double x;
        double y;
        double z;
        double px;
        double py;
        double pz;
        double vx;
        double vy;
        double vz;
        final float size;
        final float spin;
        final int life;
        final float u0;
        final float v0;
        int age;

        Ember(double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.px = x;
            this.py = y;
            this.pz = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.size = 0.04F + random.nextFloat() * 0.05F;
            this.spin = (random.nextFloat() - 0.5F) * 50.0F;
            this.life = 12 + random.nextInt(10);
            this.u0 = random.nextInt(4) * 0.25F;
            this.v0 = random.nextInt(4) * 0.25F;
        }
    }
}
