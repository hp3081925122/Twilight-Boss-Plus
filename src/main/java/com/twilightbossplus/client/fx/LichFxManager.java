package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.LichFxPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.Lich;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LichFxManager {
    private static final ResourceLocation SIGIL = tex("sigil");
    private static final ResourceLocation SIGIL_RING = tex("sigil_ring");
    private static final ResourceLocation MIRROR = tex("mirror");
    private static final ResourceLocation CHAIN = tex("chain");
    private static final ResourceLocation BEAM = tex("beam");
    private static final ResourceLocation HALO = tex("halo");
    private static final ResourceLocation HALO_BROKEN = tex("halo_broken");
    private static final ResourceLocation SLASH = tex("slash");
    private static final ResourceLocation BURST = tex("burst");
    private static final ResourceLocation SHARD = tex("shard");
    private static final ResourceLocation CROWN = tex("crown");
    private static final ResourceLocation SMOKE = tex("smoke");
    private static final ResourceLocation ORB = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/void_orb.png");
    private static final ResourceLocation PHANTOM = tex("phantom");

    private static final int GHOST_LIFE = 16;
    private static final int CHAIN_FADE = 10;
    private static final int BURST_LIFE = 12;
    private static final int SLASH_SWING = 4;
    private static final int SLASH_FADE = 8;
    private static final int SMOKE_LIFE = 30;
    private static final int BEAM_LIFE = 8;
    private static final int STAGGER_FADE = 8;
    private static final int CROWN_FADE = 6;

    private static final float VIOLET_R = 0.48F;
    private static final float VIOLET_G = 0.31F;
    private static final float VIOLET_B = 0.82F;
    private static final float SOUL_R = 0.37F;
    private static final float SOUL_G = 0.99F;
    private static final float SOUL_B = 0.89F;
    private static final float GOLD_R = 0.91F;
    private static final float GOLD_G = 0.78F;
    private static final float GOLD_B = 0.40F;

    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final List<Sigil> SIGILS = new ArrayList<>();
    private static final List<Chain> CHAINS = new ArrayList<>();
    private static final List<Shard> SHARDS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final List<Slash> SLASHES = new ArrayList<>();
    private static final List<Crown> CROWNS = new ArrayList<>();
    private static final List<Stagger> STAGGERS = new ArrayList<>();
    private static final List<Smoke> SMOKES = new ArrayList<>();
    private static final List<Beam> BEAMS = new ArrayList<>();

    private static Vec3 camera = Vec3.ZERO;

    private LichFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/lich/" + name + ".png");
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        if (type == LichFxPacket.TELEPORT && data.length >= 7) {
            GHOSTS.add(new Ghost(entityId, data[0], data[1], data[2], data[3], data[4], data[5], data[6]));
        } else if (type == LichFxPacket.SIGIL && data.length >= 6) {
            SIGILS.add(new Sigil(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4]), (int) data[5]));
        } else if (type == LichFxPacket.CHAIN_START && data.length >= 4) {
            int kind = (int) data[1];
            CHAINS.removeIf(chain -> chain.lichId == entityId && chain.kind == kind);
            CHAINS.add(new Chain(entityId, (int) data[0], kind, (int) data[2], (int) data[3]));
        } else if (type == LichFxPacket.CHAIN_END && data.length >= 2) {
            for (Chain chain : CHAINS) {
                if (chain.lichId == entityId && chain.kind == (int) data[0] && chain.endAge < 0) {
                    chain.endAge = chain.age;
                    chain.broken = data[1] > 0.5F;
                }
            }
        } else if (type == LichFxPacket.SHIELD_BREAK && data.length >= 2) {
            Entity entity = level.getEntity(entityId);
            int before = Math.max(1, (int) data[1]);
            int lost = before - (int) data[0];
            if (entity != null) {
                for (int c = 0; c < lost; c++) {
                    float angle = entity.tickCount / 5.0F + ((int) data[0] + c) * Mth.TWO_PI / before;
                    double sx = entity.getX() + Mth.cos(angle) * 0.7D;
                    double sy = entity.getY() + entity.getBbHeight() * 0.55D;
                    double sz = entity.getZ() + Mth.sin(angle) * 0.7D;
                    for (int i = 0; i < 6; i++) {
                        Shard shard = new Shard(sx, sy, sz, false);
                        shard.vx = Mth.cos(angle) * 0.12D + (random.nextDouble() - 0.5D) * 0.12D;
                        shard.vy = 0.12D + random.nextDouble() * 0.12D;
                        shard.vz = Mth.sin(angle) * 0.12D + (random.nextDouble() - 0.5D) * 0.12D;
                        shard.size = 0.05F + random.nextFloat() * 0.05F;
                        shard.spin = (random.nextFloat() - 0.5F) * 50.0F;
                        shard.life = 18 + random.nextInt(8);
                        SHARDS.add(shard);
                    }
                }
            }
        } else if (type == LichFxPacket.BOMB_BURST && data.length >= 4) {
            BURSTS.add(new Burst(data[0], data[1], data[2], data[3], 0, true));
        } else if (type == LichFxPacket.SLASH && data.length >= 5) {
            SLASHES.add(new Slash(data[0], data[1], data[2], data[3], data[4]));
        } else if (type == LichFxPacket.CROWN_START && data.length >= 3) {
            Entity entity = level.getEntity(entityId);
            double baseY = 0.0D;
            if (entity != null) {
                baseY = entity.getY();
            }
            CROWNS.removeIf(crown -> crown.lichId == entityId);
            CROWNS.add(new Crown(entityId, (int) data[0], data[1], data[2], baseY));
        } else if (type == LichFxPacket.CROWN_BURST && data.length >= 4) {
            for (Crown crown : CROWNS) {
                if (crown.lichId == entityId && crown.endAge < 0) {
                    crown.endAge = crown.age;
                }
            }
            BURSTS.add(new Burst(data[0], data[1] + 0.1F, data[2], data[3], 1, true));
            BURSTS.add(new Burst(data[0], data[1] + 0.1F, data[2], data[3] * 0.6F, 1, false));
        } else if (type == LichFxPacket.STAGGER && data.length >= 1) {
            STAGGERS.removeIf(stagger -> stagger.lichId == entityId);
            STAGGERS.add(new Stagger(entityId, Math.max(1, (int) data[0])));
        } else if (type == LichFxPacket.CANDLE_SNUFF && data.length >= 3) {
            for (int i = 0; i < 3; i++) {
                SMOKES.add(new Smoke(data[0] + (random.nextFloat() - 0.5F) * 0.2F, data[1], data[2] + (random.nextFloat() - 0.5F) * 0.2F, random.nextFloat() * 180.0F, i * 3));
            }
        } else if (type == LichFxPacket.BEAM && data.length >= 9) {
            BEAMS.add(new Beam(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8]));
        } else if (type == LichFxPacket.ORB_POP && data.length >= 3) {
            for (int i = 0; i < 10; i++) {
                Shard shard = new Shard(data[0], data[1], data[2], true);
                double yaw = random.nextDouble() * Math.PI * 2.0D;
                double pitch = (random.nextDouble() - 0.3D) * Math.PI * 0.5D;
                double speed = 0.12D + random.nextDouble() * 0.1D;
                shard.vx = Math.cos(yaw) * Math.cos(pitch) * speed;
                shard.vy = Math.sin(pitch) * speed + 0.05D;
                shard.vz = Math.sin(yaw) * Math.cos(pitch) * speed;
                shard.size = 0.05F + random.nextFloat() * 0.04F;
                shard.spin = (random.nextFloat() - 0.5F) * 60.0F;
                shard.life = 12 + random.nextInt(6);
                SHARDS.add(shard);
            }
            BURSTS.add(new Burst(data[0], data[1], data[2], 1.4F, 2, false));
        }
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
        GHOSTS.removeIf(ghost -> ++ghost.age > GHOST_LIFE);
        SIGILS.removeIf(sigil -> ++sigil.age > sigil.duration);
        Iterator<Chain> chains = CHAINS.iterator();
        while (chains.hasNext()) {
            Chain chain = chains.next();
            chain.age++;
            boolean missing = level.getEntity(chain.lichId) == null || level.getEntity(chain.targetId) == null;
            if (chain.endAge >= 0 && chain.age - chain.endAge > CHAIN_FADE) {
                chains.remove();
            } else if (chain.endAge < 0 && missing) {
                chains.remove();
            } else if (chain.age > chain.windup + chain.duration + 60) {
                chains.remove();
            }
        }
        Iterator<Shard> shards = SHARDS.iterator();
        while (shards.hasNext()) {
            Shard shard = shards.next();
            shard.px = shard.x;
            shard.py = shard.y;
            shard.pz = shard.z;
            shard.x += shard.vx;
            shard.y += shard.vy;
            shard.z += shard.vz;
            shard.vy -= 0.025D;
            shard.vx *= 0.94D;
            shard.vz *= 0.94D;
            shard.age++;
            if (shard.age > shard.life) {
                shards.remove();
            }
        }
        BURSTS.removeIf(burst -> ++burst.age > BURST_LIFE);
        SLASHES.removeIf(slash -> ++slash.age > SLASH_SWING + SLASH_FADE);
        Iterator<Crown> crowns = CROWNS.iterator();
        while (crowns.hasNext()) {
            Crown crown = crowns.next();
            crown.age++;
            if (crown.endAge >= 0 && crown.age - crown.endAge > CROWN_FADE) {
                crowns.remove();
            } else if (crown.age > crown.duration + 40) {
                crowns.remove();
            } else if (level.getEntity(crown.lichId) == null) {
                crowns.remove();
            }
        }
        STAGGERS.removeIf(stagger -> ++stagger.age > stagger.duration + STAGGER_FADE || level.getEntity(stagger.lichId) == null);
        SMOKES.removeIf(smoke -> ++smoke.age > SMOKE_LIFE + smoke.delay);
        BEAMS.removeIf(beam -> ++beam.age > BEAM_LIFE);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        GHOSTS.clear();
        SIGILS.clear();
        CHAINS.clear();
        SHARDS.clear();
        BURSTS.clear();
        SLASHES.clear();
        CROWNS.clear();
        STAGGERS.clear();
        SMOKES.clear();
        BEAMS.clear();
    }

    private static boolean isEmpty() {
        return GHOSTS.isEmpty() && SIGILS.isEmpty() && CHAINS.isEmpty() && SHARDS.isEmpty() && BURSTS.isEmpty()
            && SLASHES.isEmpty() && CROWNS.isEmpty() && STAGGERS.isEmpty() && SMOKES.isEmpty() && BEAMS.isEmpty();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || isEmpty()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        float partialTick = event.getPartialTick();
        camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (Sigil sigil : SIGILS) {
            renderSigil(poseStack, buffers, sigil, partialTick);
        }
        for (Chain chain : CHAINS) {
            renderChain(poseStack, buffers, level, chain, partialTick);
        }
        for (Crown crown : CROWNS) {
            renderCrown(poseStack, buffers, level, crown, partialTick);
        }
        for (Stagger stagger : STAGGERS) {
            renderStagger(poseStack, buffers, level, stagger, partialTick);
        }
        for (Burst burst : BURSTS) {
            if (burst.kind != 2 || decorative) {
                renderBurst(poseStack, buffers, burst, partialTick);
            }
        }
        if (decorative) {
            for (Ghost ghost : GHOSTS) {
                renderGhost(poseStack, buffers, ghost, partialTick);
            }
            for (Slash slash : SLASHES) {
                renderSlash(poseStack, buffers, slash, partialTick);
            }
            for (Smoke smoke : SMOKES) {
                renderSmoke(poseStack, buffers, smoke, partialTick);
            }
            for (Beam beam : BEAMS) {
                renderBeam(poseStack, buffers, beam, partialTick);
            }
            renderShards(poseStack, buffers, partialTick);
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static Vec3 lerpPos(Entity entity, float partialTick) {
        return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a0, Vec3 a1, Vec3 b1, Vec3 b0, float u0, float u1, float v0, float v1,
                             float r, float g, float b, float alphaA, float alphaB, Vec3 normal) {
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;
        FxDraw.vertex(consumer, pose, (float) a0.x, (float) a0.y, (float) a0.z, u0, v0, r, g, b, alphaA, FxDraw.FULL_BRIGHT, nx, ny, nz);
        FxDraw.vertex(consumer, pose, (float) a1.x, (float) a1.y, (float) a1.z, u0, v1, r, g, b, alphaA, FxDraw.FULL_BRIGHT, nx, ny, nz);
        FxDraw.vertex(consumer, pose, (float) b1.x, (float) b1.y, (float) b1.z, u1, v1, r, g, b, alphaB, FxDraw.FULL_BRIGHT, nx, ny, nz);
        FxDraw.vertex(consumer, pose, (float) b0.x, (float) b0.y, (float) b0.z, u1, v0, r, g, b, alphaB, FxDraw.FULL_BRIGHT, nx, ny, nz);
    }

    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to, float width0, float width1, float u0, float u1,
                               float r, float g, float b, float alpha0, float alpha1) {
        Vec3 dir = to.subtract(from);
        Vec3 view = camera.subtract(from.add(to).scale(0.5D));
        Vec3 side = dir.cross(view);
        if (side.lengthSqr() < 1.0E-8D) {
            return;
        }
        side = side.normalize();
        Vec3 normal = view.normalize();
        quad(consumer, pose, from.subtract(side.scale(width0)), from.add(side.scale(width0)), to.add(side.scale(width1)), to.subtract(side.scale(width1)),
            u0, u1, 0.0F, 1.0F, r, g, b, alpha0, alpha1, normal);
    }

    private static void annulus(VertexConsumer consumer, PoseStack.Pose pose, float inner, float outer, int segments, float uRepeat,
                                float r, float g, float b, float innerAlpha, float outerAlpha) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            float u0 = uRepeat * i / segments;
            float u1 = uRepeat * (i + 1) / segments;
            FxDraw.vertex(consumer, pose, c0 * inner, 0.0F, s0 * inner, u0, 0.0F, r, g, b, innerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, c0 * outer, 0.0F, s0 * outer, u0, 1.0F, r, g, b, outerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, c1 * outer, 0.0F, s1 * outer, u1, 1.0F, r, g, b, outerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, c1 * inner, 0.0F, s1 * inner, u1, 0.0F, r, g, b, innerAlpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void band(VertexConsumer consumer, PoseStack.Pose pose, float radius, float y0, float y1, int segments, float uRepeat,
                             float r, float g, float b, float alpha) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            float u0 = uRepeat * i / segments;
            float u1 = uRepeat * (i + 1) / segments;
            float nx = Mth.cos((a0 + a1) * 0.5F);
            float nz = Mth.sin((a0 + a1) * 0.5F);
            FxDraw.vertex(consumer, pose, c0 * radius, y1, s0 * radius, u0, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, c0 * radius, y0, s0 * radius, u0, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, c1 * radius, y0, s1 * radius, u1, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(consumer, pose, c1 * radius, y1, s1 * radius, u1, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
        }
    }

    private static void panel(VertexConsumer consumer, PoseStack.Pose pose, float halfWidth, float halfHeight, float z,
                              float r, float g, float b, float alpha) {
        FxDraw.vertex(consumer, pose, -halfWidth, -halfHeight, z, 1.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, halfWidth, -halfHeight, z, 0.0F, 1.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, halfWidth, halfHeight, z, 0.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, -halfWidth, halfHeight, z, 1.0F, 0.0F, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
    }

    private static void renderGhost(PoseStack poseStack, MultiBufferSource buffers, Ghost ghost, float partialTick) {
        float t = Mth.clamp((ghost.age + partialTick) / GHOST_LIFE, 0.0F, 1.0F);
        Vec3 from = new Vec3(ghost.x, ghost.y, ghost.z);
        Vec3 to = new Vec3(ghost.tx, ghost.ty, ghost.tz);
        if (t < 0.5F) {
            float beamAlpha = 0.55F * (1.0F - t * 2.0F);
            VertexConsumer beam = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
            ribbon(beam, poseStack.last(), from.add(0.0D, 1.1D, 0.0D), to.add(0.0D, 1.1D, 0.0D), 0.04F, 0.16F, 0.0F, 3.0F,
                VIOLET_R, VIOLET_G, VIOLET_B, beamAlpha * 0.3F, beamAlpha);
        }
        float alpha = 0.55F * (1.0F - t) * (1.0F - t);
        if (alpha <= 0.01F) {
            return;
        }
        float r = VIOLET_R * 1.3F;
        float g = VIOLET_G * 1.3F;
        float b = 1.0F;
        int light = FxDraw.FULL_BRIGHT;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(PHANTOM));
        poseStack.pushPose();
        poseStack.translate(ghost.x, ghost.y + t * 0.9F, ghost.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - ghost.yaw));
        poseStack.scale(1.0F - t * 0.35F, 1.0F + t * 0.8F, 1.0F - t * 0.35F);
        PoseStack.Pose pose = poseStack.last();
        FxDraw.box(consumer, pose, -0.36F, 0.0F, -0.3F, 0.36F, 0.36F, 0.3F, 0.0F, 0.5F, 0.5F, 1.0F, r, g, b, alpha, light);
        FxDraw.box(consumer, pose, -0.28F, 0.36F, -0.22F, 0.28F, 0.96F, 0.22F, 0.0F, 0.0F, 0.5F, 0.5F, r, g, b, alpha, light);
        FxDraw.box(consumer, pose, -0.22F, 0.96F, -0.17F, 0.22F, 1.32F, 0.17F, 0.0F, 0.0F, 0.5F, 0.4F, r, g, b, alpha, light);
        FxDraw.box(consumer, pose, -0.33F, 1.3F, -0.2F, 0.33F, 1.44F, 0.2F, 0.0F, 0.0F, 0.5F, 0.12F, r, g, b, alpha, light);
        FxDraw.box(consumer, pose, -0.2F, 1.42F, -0.2F, 0.2F, 1.86F, 0.2F, 0.0F, 0.0F, 0.5F, 0.3F, r, g, b, alpha, light);
        FxDraw.box(consumer, pose, -0.18F, 1.42F, -0.218F, 0.18F, 1.84F, -0.2F, 0.5F, 0.0F, 1.0F, 0.5F, r, g, b, alpha, light);
        float sleeve = 0.05F * Mth.sin((ghost.age + partialTick) * 0.4F);
        FxDraw.box(consumer, pose, -0.38F - sleeve, 0.7F, -0.09F, -0.26F - sleeve, 1.32F, 0.09F, 0.0F, 0.5F, 0.25F, 1.0F, r, g, b, alpha, light);
        FxDraw.box(consumer, pose, 0.26F + sleeve, 0.7F, -0.09F, 0.38F + sleeve, 1.32F, 0.09F, 0.25F, 0.5F, 0.5F, 1.0F, r, g, b, alpha, light);
        float spin = (ghost.age + partialTick) * 0.15F;
        for (int i = 0; i < 6; i++) {
            float angle = spin + i * Mth.TWO_PI / 6.0F;
            float cx = Mth.cos(angle) * 0.2F;
            float cz = Mth.sin(angle) * 0.2F;
            FxDraw.box(consumer, pose, cx - 0.04F, 1.94F, cz - 0.04F, cx + 0.04F, 2.08F, cz + 0.04F, 0.5F, 0.5F, 1.0F, 1.0F,
                GOLD_R, GOLD_G, GOLD_B, alpha, light);
        }
        poseStack.popPose();
    }

    private static void renderSigil(PoseStack poseStack, MultiBufferSource buffers, Sigil sigil, float partialTick) {
        float age = sigil.age + partialTick;
        float t = Mth.clamp(age / sigil.duration, 0.0F, 1.0F);
        float appear = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        float fade = 1.0F;
        if (sigil.duration - age < 3.0F) {
            fade = Mth.clamp((sigil.duration - age) / 3.0F, 0.0F, 1.0F);
        }
        float r = VIOLET_R;
        float g = VIOLET_G;
        float b = VIOLET_B;
        float diskRadius = 1.1F;
        if (sigil.kind == 1) {
            r = GOLD_R;
            g = GOLD_G;
            b = GOLD_B;
            diskRadius = 0.9F;
        }
        float alpha = (0.35F + 0.55F * t) * appear * fade;
        poseStack.pushPose();
        poseStack.translate(sigil.x, sigil.y, sigil.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-sigil.yaw));
        if (sigil.kind == 0) {
            VertexConsumer mirror = buffers.getBuffer(RenderType.entityTranslucent(MIRROR));
            float rise = 0.15F + 0.85F * appear;
            panel(mirror, poseStack.last(), 0.55F, 1.1F * rise, 0.02F, 0.75F, 0.65F, 1.0F, alpha * 0.8F);
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 3.0F));
        VertexConsumer disk = buffers.getBuffer(RenderType.entityTranslucent(SIGIL));
        panel(disk, poseStack.last(), diskRadius * appear, diskRadius * appear, -0.03F, r, g, b, alpha);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-age * 6.0F));
        float ringRadius = diskRadius * (2.0F - t);
        VertexConsumer ring = buffers.getBuffer(RenderType.entityTranslucent(SIGIL_RING));
        panel(ring, poseStack.last(), ringRadius, ringRadius, -0.01F, r, g, b, alpha * (0.4F + 0.6F * t));
        poseStack.popPose();
        poseStack.popPose();
        if (sigil.kind == 1) {
            VertexConsumer ground = buffers.getBuffer(RenderType.entityTranslucent(SIGIL));
            FxDraw.flatSquare(ground, poseStack.last(), (float) sigil.x, (float) sigil.y - 1.07F, (float) sigil.z, 1.3F, age * 0.05F, r, g, b, alpha * 0.8F, FxDraw.FULL_BRIGHT);
        }
    }

    private static void renderChain(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Chain chain, float partialTick) {
        Entity lich = level.getEntity(chain.lichId);
        Entity target = level.getEntity(chain.targetId);
        if (lich == null || target == null) {
            return;
        }
        float age = chain.age + partialTick;
        Vec3 lichPos = lerpPos(lich, partialTick);
        float bodyYaw = lich.getYRot();
        if (lich instanceof Lich living) {
            bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        }
        Vec3 right = NagaBossController.directionOf(bodyYaw + 90.0F);
        Vec3 forward = NagaBossController.directionOf(bodyYaw);
        Vec3 from = lichPos.add(right.scale(-0.35D)).add(forward.scale(0.35D)).add(0.0D, lich.getBbHeight() * 0.62D, 0.0D);
        Vec3 to = lerpPos(target, partialTick).add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        float r = SOUL_R;
        float g = SOUL_G;
        float b = SOUL_B;
        if (chain.kind == 1) {
            r = VIOLET_R;
            g = VIOLET_G;
            b = VIOLET_B;
        }
        float alpha = 0.9F;
        float width = 0.11F;
        boolean charging = age < chain.windup;
        if (charging) {
            float k = age / Math.max(1, chain.windup);
            alpha = 0.2F + 0.2F * k + 0.1F * Mth.sin(age * 2.2F);
            width = 0.04F + 0.07F * k;
        }
        float shake = 0.0F;
        float reach = 1.0F;
        if (chain.endAge >= 0) {
            float fade = Mth.clamp(1.0F - (age - chain.endAge) / CHAIN_FADE, 0.0F, 1.0F);
            alpha *= fade;
            if (chain.broken) {
                shake = 0.12F * fade;
                r = 1.0F;
                g = 0.55F;
                b = 0.55F;
            } else {
                reach = fade;
            }
        }
        if (alpha <= 0.01F) {
            return;
        }
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.1D) {
            return;
        }
        Vec3 axis = dir.scale(1.0D / length);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(axis.y) > 0.95D) {
            up = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 e1 = axis.cross(up).normalize();
        Vec3 e2 = e1.cross(axis).normalize();
        float sag = (float) Math.min(1.2D, length * 0.08D);
        if (!charging) {
            sag *= 0.5F + 0.15F * Mth.sin(age * 0.3F);
        }
        int segments = Math.max(8, (int) (length * 3.0D));
        float scroll = age * 0.12F;
        if (chain.kind == 0) {
            scroll = -scroll;
        }
        RandomSource random = level.random;
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float s = (float) i / segments * reach;
            Vec3 point = from.add(dir.scale(s)).subtract(0.0D, sag * Mth.sin(Mth.PI * s / Math.max(reach, 0.01F)), 0.0D);
            if (shake > 0.0F && i > 0 && i < segments) {
                point = point.add((random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake);
            }
            points[i] = point;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(CHAIN));
        PoseStack.Pose pose = poseStack.last();
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < segments; i++) {
                float s0 = (float) i / segments;
                float s1 = (float) (i + 1) / segments;
                float twist0 = s0 * 6.0F + age * 0.08F + strand * Mth.HALF_PI;
                float twist1 = s1 * 6.0F + age * 0.08F + strand * Mth.HALF_PI;
                Vec3 side0 = e1.scale(Mth.cos(twist0)).add(e2.scale(Mth.sin(twist0))).scale(width);
                Vec3 side1 = e1.scale(Mth.cos(twist1)).add(e2.scale(Mth.sin(twist1))).scale(width);
                float u0 = (float) (s0 * length * reach / 0.5D) + scroll;
                float u1 = (float) (s1 * length * reach / 0.5D) + scroll;
                Vec3 normal = side0.cross(axis);
                quad(consumer, pose, points[i].subtract(side0), points[i].add(side0), points[i + 1].add(side1), points[i + 1].subtract(side1),
                    u0, u1, 0.0F, 1.0F, r, g, b, alpha, alpha, normal.normalize());
            }
        }
        if (charging || chain.endAge >= 0) {
            return;
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
        for (int bead = 0; bead < 3; bead++) {
            float phase = (age * 0.06F + bead / 3.0F) % 1.0F;
            float s = 1.0F - phase;
            if (chain.kind == 1) {
                s = phase;
            }
            int index = Mth.clamp((int) (s * segments), 0, segments - 1);
            float local = s * segments - index;
            Vec3 center = points[index].add(points[index + 1].subtract(points[index]).scale(local));
            float size = 0.09F + 0.03F * Mth.sin(age * 0.8F + bead);
            poseStack.pushPose();
            poseStack.translate(center.x, center.y, center.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 9.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 7.0F));
            FxDraw.box(glow, poseStack.last(), -size, -size, -size, size, size, size, 0.3F, 0.4F, 0.7F, 0.6F, r, g, b, 0.85F, FxDraw.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

    private static void renderBurst(PoseStack poseStack, MultiBufferSource buffers, Burst burst, float partialTick) {
        float t = Mth.clamp((burst.age + partialTick) / BURST_LIFE, 0.0F, 1.0F);
        float ease = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        float outer = Math.max(0.05F, burst.radius * ease);
        float inner = outer * (0.55F + 0.3F * t);
        float alpha = 1.0F - t;
        float r = VIOLET_R;
        float g = VIOLET_G;
        float b = VIOLET_B;
        if (burst.kind == 1) {
            r = GOLD_R;
            g = GOLD_G;
            b = GOLD_B;
        }
        int segments = Math.max(24, (int) (burst.radius * 8.0F));
        float uRepeat = Math.max(4.0F, outer * 3.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(BURST));
        poseStack.pushPose();
        poseStack.translate(burst.x, burst.y, burst.z);
        annulus(consumer, poseStack.last(), inner, outer, segments, uRepeat, r, g, b, 0.0F, alpha);
        if (burst.orthogonal) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            annulus(consumer, poseStack.last(), inner * 0.8F, outer * 0.8F, segments, uRepeat, r, g, b, 0.0F, alpha * 0.8F);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            annulus(consumer, poseStack.last(), inner * 0.8F, outer * 0.8F, segments, uRepeat, r, g, b, 0.0F, alpha * 0.8F);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderSlash(PoseStack poseStack, MultiBufferSource buffers, Slash slash, float partialTick) {
        float age = slash.age + partialTick;
        float swing = Mth.clamp(age / SLASH_SWING, 0.0F, 1.0F);
        float fade = 1.0F;
        if (age > SLASH_SWING) {
            fade = Mth.clamp(1.0F - (age - SLASH_SWING) / SLASH_FADE, 0.0F, 1.0F);
        }
        if (fade <= 0.0F) {
            return;
        }
        float halfArc = 45.0F;
        float tail = Math.max(0.0F, swing - 0.75F);
        int slices = 14;
        float inner = slash.reach * 0.3F;
        float outer = slash.reach + 0.3F;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SLASH));
        PoseStack.Pose pose = poseStack.last();
        Vec3 normal = new Vec3(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < slices; i++) {
            float p0 = tail + (swing - tail) * i / slices;
            float p1 = tail + (swing - tail) * (i + 1) / slices;
            float a0 = slash.yaw + halfArc - halfArc * 2.0F * p0;
            float a1 = slash.yaw + halfArc - halfArc * 2.0F * p1;
            Vec3 d0 = NagaBossController.directionOf(a0);
            Vec3 d1 = NagaBossController.directionOf(a1);
            double h0 = slash.y + 1.7D - 1.2D * p0;
            double h1 = slash.y + 1.7D - 1.2D * p1;
            Vec3 in0 = new Vec3(slash.x + d0.x * inner, h0 - 0.2D, slash.z + d0.z * inner);
            Vec3 out0 = new Vec3(slash.x + d0.x * outer, h0 + 0.15D, slash.z + d0.z * outer);
            Vec3 in1 = new Vec3(slash.x + d1.x * inner, h1 - 0.2D, slash.z + d1.z * inner);
            Vec3 out1 = new Vec3(slash.x + d1.x * outer, h1 + 0.15D, slash.z + d1.z * outer);
            float u0 = (float) i / slices;
            float u1 = (float) (i + 1) / slices;
            quad(consumer, pose, in0, out0, out1, in1, u0, u1, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, fade, fade, normal);
        }
    }

    private static void renderCrown(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Crown crown, float partialTick) {
        Entity lich = level.getEntity(crown.lichId);
        if (lich == null) {
            return;
        }
        float age = crown.age + partialTick;
        float progress = Mth.clamp(age / crown.duration, 0.0F, 1.0F);
        float fade = 1.0F;
        if (crown.endAge >= 0) {
            fade = Mth.clamp(1.0F - (age - crown.endAge) / CROWN_FADE, 0.0F, 1.0F);
        }
        if (fade <= 0.0F) {
            return;
        }
        Vec3 pos = lerpPos(lich, partialTick);
        float height = lich.getBbHeight();

        VertexConsumer crownBuffer = buffers.getBuffer(RenderType.entityTranslucent(CROWN));
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y + height + 0.3D + 0.06D * Mth.sin(age * 0.2F), pos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 4.0F));
        float crownScale = 0.6F + 0.4F * Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        poseStack.scale(crownScale, crownScale, crownScale);
        for (int i = 0; i < 8; i++) {
            float spike = 0.18F;
            if (i % 2 == 0) {
                spike = 0.3F;
            }
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 45.0F));
            poseStack.translate(0.0F, 0.0F, 0.3F);
            FxDraw.box(crownBuffer, poseStack.last(), -0.13F, 0.0F, -0.03F, 0.13F, spike, 0.03F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, fade, FxDraw.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();

        float ringAlpha = (0.3F + 0.6F * progress) * fade;
        VertexConsumer halo = buffers.getBuffer(RenderType.entityTranslucent(HALO));
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y + height * 0.55D, pos.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2.0F));
        float radius = Math.max(1.0F, crown.radius * 0.5F);
        annulus(halo, poseStack.last(), radius - 0.18F, radius + 0.18F, 48, Math.max(4.0F, radius * 2.0F), GOLD_R, GOLD_G, GOLD_B, ringAlpha, ringAlpha);
        poseStack.popPose();

        VertexConsumer ground = buffers.getBuffer(RenderType.entityTranslucent(SIGIL_RING));
        FxDraw.flatSquare(ground, poseStack.last(), (float) pos.x, (float) crown.baseY + 0.05F, (float) pos.z, crown.pull, age * 0.01F,
            VIOLET_R, VIOLET_G, VIOLET_B, (0.25F + 0.4F * progress) * fade, FxDraw.FULL_BRIGHT);

        if (crown.endAge >= 0) {
            return;
        }
        VertexConsumer rays = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
        Vec3 center = pos.add(0.0D, height * 0.55D, 0.0D);
        for (int i = 0; i < 12; i++) {
            float angle = i * Mth.TWO_PI / 12.0F + age * 0.01F;
            Vec3 start = new Vec3(pos.x + Mth.cos(angle) * crown.pull, crown.baseY + 0.2D, pos.z + Mth.sin(angle) * crown.pull);
            float flicker = 0.6F + 0.4F * Mth.sin(age * 0.5F + i * 1.3F);
            ribbon(rays, poseStack.last(), start, center, 0.03F, 0.1F, -age * 0.25F, -age * 0.25F + crown.pull / 1.5F,
                VIOLET_R, VIOLET_G, VIOLET_B, 0.0F, 0.6F * progress * flicker);
        }
    }

    private static void renderStagger(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Stagger stagger, float partialTick) {
        Entity lich = level.getEntity(stagger.lichId);
        if (lich == null) {
            return;
        }
        float age = stagger.age + partialTick;
        float alpha = 0.75F + 0.25F * Mth.sin(age * 1.3F);
        if (age > stagger.duration) {
            alpha *= Mth.clamp(1.0F - (age - stagger.duration) / STAGGER_FADE, 0.0F, 1.0F);
        }
        if (alpha <= 0.0F) {
            return;
        }
        Vec3 pos = lerpPos(lich, partialTick);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(HALO_BROKEN));
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y + lich.getBbHeight() * 0.45D, pos.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.3F) * 9.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(age * 0.23F) * 7.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 3.0F));
        band(consumer, poseStack.last(), 0.78F, -0.12F, 0.12F, 24, 2.0F, GOLD_R, GOLD_G, GOLD_B, alpha);
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 7.0F));
        band(consumer, poseStack.last(), 0.95F, -0.06F, 0.06F, 24, 3.0F, VIOLET_R, VIOLET_G, VIOLET_B, alpha * 0.6F);
        poseStack.popPose();
    }

    private static void renderSmoke(PoseStack poseStack, MultiBufferSource buffers, Smoke smoke, float partialTick) {
        float age = smoke.age + partialTick - smoke.delay;
        if (age < 0.0F) {
            return;
        }
        float t = Mth.clamp(age / SMOKE_LIFE, 0.0F, 1.0F);
        float alpha = 0.7F * (1.0F - t);
        float height = 0.3F + 1.0F * t;
        float width = 0.1F + 0.12F * t;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SMOKE));
        poseStack.pushPose();
        poseStack.translate(smoke.x, smoke.y + t * 0.6F + height * 0.5F, smoke.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(smoke.yaw + age * 2.0F));
        panel(consumer, poseStack.last(), width, height * 0.5F, 0.0F, 0.85F, 0.95F, 0.95F, alpha);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        panel(consumer, poseStack.last(), width, height * 0.5F, 0.0F, 0.85F, 0.95F, 0.95F, alpha);
        poseStack.popPose();
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffers, Beam beam, float partialTick) {
        float t = Mth.clamp((beam.age + partialTick) / BEAM_LIFE, 0.0F, 1.0F);
        Vec3 from = new Vec3(beam.x0, beam.y0, beam.z0);
        Vec3 to = new Vec3(beam.x1, beam.y1, beam.z1);
        float length = (float) from.distanceTo(to);
        float width = 0.12F * (1.0F - t * 0.6F);
        float alpha = 0.9F * (1.0F - t);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
        float scroll = -(beam.age + partialTick) * 0.3F;
        ribbon(consumer, poseStack.last(), from, to, width * 0.5F, width, scroll, scroll + length, beam.r, beam.g, beam.b, alpha * 0.6F, alpha);
    }

    private static void renderShards(PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
        if (SHARDS.isEmpty()) {
            return;
        }
        VertexConsumer gold = buffers.getBuffer(RenderType.entityTranslucent(SHARD));
        renderShardSet(poseStack, gold, false, partialTick);
        VertexConsumer orb = buffers.getBuffer(RenderType.entityTranslucent(ORB));
        renderShardSet(poseStack, orb, true, partialTick);
    }

    private static void renderShardSet(PoseStack poseStack, VertexConsumer consumer, boolean orb, float partialTick) {
        for (Shard shard : SHARDS) {
            if (shard.orb != orb) {
                continue;
            }
            float age = shard.age + partialTick;
            float size = shard.size * Mth.clamp(1.0F - (age - shard.life * 0.6F) / (shard.life * 0.4F), 0.0F, 1.0F);
            if (size <= 0.0F) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(Mth.lerp(partialTick, shard.px, shard.x), Mth.lerp(partialTick, shard.py, shard.y), Mth.lerp(partialTick, shard.pz, shard.z));
            poseStack.mulPose(Axis.YP.rotationDegrees(age * shard.spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * shard.spin * 0.7F));
            float u1 = 1.0F;
            if (orb) {
                u1 = 0.5F;
            }
            FxDraw.box(consumer, poseStack.last(), -size, -size * 0.4F, -size, size, size * 0.4F, size, 0.0F, 0.0F, u1, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, FxDraw.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

    private static final class Ghost {
        final int entityId;
        final double x;
        final double y;
        final double z;
        final double tx;
        final double ty;
        final double tz;
        final float yaw;
        int age;

        Ghost(int entityId, double x, double y, double z, double tx, double ty, double tz, float yaw) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.yaw = yaw;
        }
    }

    private static final class Sigil {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final int duration;
        final int kind;
        int age;

        Sigil(double x, double y, double z, float yaw, int duration, int kind) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.duration = duration;
            this.kind = kind;
        }
    }

    private static final class Chain {
        final int lichId;
        final int targetId;
        final int kind;
        final int windup;
        final int duration;
        int age;
        int endAge = -1;
        boolean broken;

        Chain(int lichId, int targetId, int kind, int windup, int duration) {
            this.lichId = lichId;
            this.targetId = targetId;
            this.kind = kind;
            this.windup = windup;
            this.duration = duration;
        }
    }

    private static final class Shard {
        final boolean orb;
        double x;
        double y;
        double z;
        double px;
        double py;
        double pz;
        double vx;
        double vy;
        double vz;
        float size;
        float spin;
        int age;
        int life;

        Shard(double x, double y, double z, boolean orb) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.px = x;
            this.py = y;
            this.pz = z;
            this.orb = orb;
        }
    }

    private static final class Burst {
        final double x;
        final double y;
        final double z;
        final float radius;
        final int kind;
        final boolean orthogonal;
        int age;

        Burst(double x, double y, double z, float radius, int kind, boolean orthogonal) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.kind = kind;
            this.orthogonal = orthogonal;
        }
    }

    private static final class Slash {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float reach;
        int age;

        Slash(double x, double y, double z, float yaw, float reach) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.reach = reach;
        }
    }

    private static final class Crown {
        final int lichId;
        final int duration;
        final float pull;
        final float radius;
        final double baseY;
        int age;
        int endAge = -1;

        Crown(int lichId, int duration, float pull, float radius, double baseY) {
            this.lichId = lichId;
            this.duration = Math.max(1, duration);
            this.pull = pull;
            this.radius = radius;
            this.baseY = baseY;
        }
    }

    private static final class Stagger {
        final int lichId;
        final int duration;
        int age;

        Stagger(int lichId, int duration) {
            this.lichId = lichId;
            this.duration = duration;
        }
    }

    private static final class Smoke {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final int delay;
        int age;

        Smoke(double x, double y, double z, float yaw, int delay) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.delay = delay;
        }
    }

    private static final class Beam {
        final double x0;
        final double y0;
        final double z0;
        final double x1;
        final double y1;
        final double z1;
        final float r;
        final float g;
        final float b;
        int age;

        Beam(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
