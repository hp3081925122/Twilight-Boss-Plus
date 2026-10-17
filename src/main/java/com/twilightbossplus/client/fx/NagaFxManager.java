package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.NagaFxPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.Naga;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NagaFxManager {
    private static final ResourceLocation RING = tex("ring");
    private static final ResourceLocation RING_FILL = tex("ring_fill");
    private static final ResourceLocation RING_SCALE = tex("ring_scale");
    private static final ResourceLocation FAN = tex("fan");
    private static final ResourceLocation TAIL_ARC = tex("tail_arc");
    private static final ResourceLocation HUSK = tex("husk");
    private static final ResourceLocation CRACK = tex("crack");
    private static final ResourceLocation CRACK_GLOW = tex("crack_glow");
    private static final ResourceLocation JAW = tex("jaw");
    private static final ResourceLocation WALL = tex("wall");
    private static final ResourceLocation WEAK_SCALE = tex("weak_scale");

    private static final int RING_SCALES = 12;
    private static final int HUSK_HOLD = 60;
    private static final int HUSK_CRUMBLE = 24;
    private static final int CRACK_LIFE = 70;
    private static final int CRACK_FADE = 20;
    private static final int BITE_LIFE = 24;
    private static final int SWEEP_FADE = 6;
    private static final int CONSTRICT_FADE = 10;

    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Sweep> SWEEPS = new ArrayList<>();
    private static final List<Husk> HUSKS = new ArrayList<>();
    private static final List<Crack> CRACKS = new ArrayList<>();
    private static final List<Debris> DEBRIS = new ArrayList<>();
    private static final List<Bite> BITES = new ArrayList<>();
    private static final List<Constrict> CONSTRICTS = new ArrayList<>();

    private NagaFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/naga/" + name + ".png");
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        if (type == NagaFxPacket.RING && data.length >= 6) {
            RINGS.add(new Ring(data[0], data[1], data[2], data[3], (int) data[4], (int) data[5]));
        } else if (type == NagaFxPacket.SWEEP && data.length >= 9) {
            SWEEPS.add(new Sweep(data[0], data[1], data[2], data[3], data[4], data[5], (int) data[6], (int) data[7], data[8]));
        } else if (type == NagaFxPacket.SHED && data.length >= 6) {
            Husk husk = new Husk();
            for (int i = 0; i + 5 < data.length; i += 6) {
                husk.pieces.add(new float[] {data[i], data[i + 1], data[i + 2], data[i + 3], data[i + 4], data[i + 5]});
            }
            HUSKS.add(husk);
        } else if (type == NagaFxPacket.REND_POINT && data.length >= 5) {
            Crack crack = null;
            for (Crack existing : CRACKS) {
                if (existing.nagaId == entityId && existing.open) {
                    crack = existing;
                }
            }
            if (crack == null || data[4] > 0.5F) {
                if (crack != null) {
                    crack.open = false;
                }
                crack = new Crack(entityId);
                CRACKS.add(crack);
            }
            crack.points.add(new CrackPoint(data[0], data[1], data[2], data[3]));
            spawnDebris(level, data[0], data[1], data[2], 2);
        } else if (type == NagaFxPacket.BITE && data.length >= 4) {
            for (Crack existing : CRACKS) {
                if (existing.nagaId == entityId) {
                    existing.open = false;
                }
            }
            BITES.add(new Bite(data[0], data[1], data[2], data[3]));
            spawnDebris(level, data[0], data[1], data[2], 10);
        } else if (type == NagaFxPacket.CONSTRICT_START && data.length >= 8) {
            CONSTRICTS.removeIf(constrict -> constrict.nagaId == entityId);
            CONSTRICTS.add(new Constrict(entityId, data[0], data[1], data[2], data[3], data[4], (int) data[5], (int) data[6], (int) data[7]));
        } else if (type == NagaFxPacket.CONSTRICT_END && data.length >= 1) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId && constrict.endAge < 0) {
                    constrict.endAge = constrict.age;
                    constrict.broken = data[0] > 0.5F;
                }
            }
        } else if (type == NagaFxPacket.WEAK_HIT) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId) {
                    constrict.flash = 6;
                }
            }
        }
    }

    private static void spawnDebris(ClientLevel level, double x, double y, double z, int count) {
        if (!ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        BlockPos pos = BlockPos.containing(x, y - 0.5D, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
        for (int i = 0; i < count; i++) {
            Debris debris = new Debris();
            debris.x = x + (level.random.nextDouble() - 0.5D) * 0.8D;
            debris.y = y + 0.05D;
            debris.z = z + (level.random.nextDouble() - 0.5D) * 0.8D;
            debris.px = debris.x;
            debris.py = debris.y;
            debris.pz = debris.z;
            debris.vx = (level.random.nextDouble() - 0.5D) * 0.16D;
            debris.vy = 0.22D + level.random.nextDouble() * 0.16D;
            debris.vz = (level.random.nextDouble() - 0.5D) * 0.16D;
            debris.size = 0.09F + level.random.nextFloat() * 0.06F;
            debris.spin = (level.random.nextFloat() - 0.5F) * 40.0F;
            debris.life = 14 + level.random.nextInt(6);
            debris.sprite = sprite;
            debris.u = level.random.nextInt(12);
            debris.v = level.random.nextInt(12);
            DEBRIS.add(debris);
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
        RINGS.removeIf(ring -> ++ring.age > ring.duration);
        SWEEPS.removeIf(sweep -> ++sweep.age > sweep.warn + sweep.strike + SWEEP_FADE);
        HUSKS.removeIf(husk -> ++husk.age > HUSK_HOLD + HUSK_CRUMBLE);
        Iterator<Crack> cracks = CRACKS.iterator();
        while (cracks.hasNext()) {
            Crack crack = cracks.next();
            crack.points.removeIf(point -> ++point.age > CRACK_LIFE);
            if (crack.points.isEmpty() && !crack.open) {
                cracks.remove();
            } else if (crack.points.isEmpty()) {
                crack.idle++;
                if (crack.idle > 40) {
                    cracks.remove();
                }
            }
        }
        Iterator<Debris> debrisIterator = DEBRIS.iterator();
        while (debrisIterator.hasNext()) {
            Debris debris = debrisIterator.next();
            debris.px = debris.x;
            debris.py = debris.y;
            debris.pz = debris.z;
            debris.x += debris.vx;
            debris.y += debris.vy;
            debris.z += debris.vz;
            debris.vy -= 0.04D;
            debris.vx *= 0.92D;
            debris.vz *= 0.92D;
            debris.age++;
            if (debris.age > debris.life) {
                debrisIterator.remove();
            }
        }
        BITES.removeIf(bite -> ++bite.age > BITE_LIFE);
        Iterator<Constrict> constricts = CONSTRICTS.iterator();
        while (constricts.hasNext()) {
            Constrict constrict = constricts.next();
            constrict.age++;
            if (constrict.flash > 0) {
                constrict.flash--;
            }
            if (constrict.endAge >= 0 && constrict.age - constrict.endAge > CONSTRICT_FADE) {
                constricts.remove();
            } else if (constrict.age > constrict.duration + 200) {
                constricts.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearAll();
        }
    }

    private static void clearAll() {
        RINGS.clear();
        SWEEPS.clear();
        HUSKS.clear();
        CRACKS.clear();
        DEBRIS.clear();
        BITES.clear();
        CONSTRICTS.clear();
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
        if (RINGS.isEmpty() && SWEEPS.isEmpty() && HUSKS.isEmpty() && CRACKS.isEmpty() && DEBRIS.isEmpty() && BITES.isEmpty() && CONSTRICTS.isEmpty()) {
            return;
        }
        boolean decorative = ClientConfig.VFX_ENABLED.get();
        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (Ring ring : RINGS) {
            renderRing(poseStack, buffers, ring, partialTick);
        }
        for (Sweep sweep : SWEEPS) {
            renderSweep(poseStack, buffers, sweep, partialTick, decorative);
        }
        for (Constrict constrict : CONSTRICTS) {
            renderConstrict(poseStack, buffers, level, constrict, partialTick);
        }
        if (decorative) {
            for (Husk husk : HUSKS) {
                renderHusk(poseStack, buffers, level, husk, partialTick);
            }
            for (Crack crack : CRACKS) {
                renderCrack(poseStack, buffers, level, crack, partialTick);
            }
            for (Bite bite : BITES) {
                renderBite(poseStack, buffers, level, bite, partialTick);
            }
            renderDebris(poseStack, buffers, level, partialTick);
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderRing(PoseStack poseStack, MultiBufferSource buffers, Ring ring, float partialTick) {
        float t = Mth.clamp((ring.age + partialTick) / Math.max(1, ring.duration), 0.0F, 1.0F);
        float r = 0.78F;
        float g = 0.88F;
        float b = 0.29F;
        float spin = 1.0F;
        if (ring.kind == 1) {
            r = 0.86F;
            g = 0.27F;
            b = 0.18F;
            spin = -1.4F;
        }
        PoseStack.Pose pose = poseStack.last();
        float y = (float) ring.y + 0.03F;
        VertexConsumer fill = buffers.getBuffer(RenderType.entityTranslucent(RING_FILL));
        FxDraw.flatSquare(fill, pose, (float) ring.x, y, (float) ring.z, ring.radius, 0.0F, r, g, b, 0.25F + 0.35F * t, FxDraw.FULL_BRIGHT);
        VertexConsumer outer = buffers.getBuffer(RenderType.entityTranslucent(RING));
        float contract = ring.radius * (1.9F - 0.9F * t);
        FxDraw.flatSquare(outer, pose, (float) ring.x, y + 0.005F, (float) ring.z, contract, 0.0F, r, g, b, 0.35F + 0.6F * t, FxDraw.FULL_BRIGHT);
        FxDraw.flatSquare(outer, pose, (float) ring.x, y + 0.004F, (float) ring.z, ring.radius, 0.0F, r, g, b, 0.3F + 0.3F * t, FxDraw.FULL_BRIGHT);
        VertexConsumer scales = buffers.getBuffer(RenderType.entityTranslucent(RING_SCALE));
        float head = (ring.age + partialTick) * 0.35F * Math.abs(spin);
        float step = Mth.TWO_PI / RING_SCALES;
        for (int i = 0; i < RING_SCALES; i++) {
            float lag = (head - i) % RING_SCALES;
            if (lag < 0.0F) {
                lag += RING_SCALES;
            }
            float glow = Math.max(0.0F, 1.0F - lag / 4.0F);
            float theta = i * step;
            float facing = theta + Mth.PI;
            if (spin < 0.0F) {
                theta = -theta;
                facing = theta;
            }
            float size = ring.radius * 0.13F * (0.7F + 0.3F * glow);
            float alpha = (0.15F + 0.8F * glow) * (0.4F + 0.6F * t);
            float sx = (float) ring.x + Mth.cos(theta) * ring.radius * 0.9F;
            float sz = (float) ring.z + Mth.sin(theta) * ring.radius * 0.9F;
            FxDraw.flatSquare(scales, pose, sx, y + 0.006F, sz, size, facing, r, g, b, alpha, FxDraw.FULL_BRIGHT);
        }
    }

    private static void renderSweep(PoseStack poseStack, MultiBufferSource buffers, Sweep sweep, float partialTick, boolean decorative) {
        float age = sweep.age + partialTick;
        PoseStack.Pose pose = poseStack.last();
        int slices = 16;
        float y = (float) sweep.y + 0.04F;
        if (age < sweep.warn + sweep.strike) {
            float warnProgress = Mth.clamp(age / sweep.warn, 0.0F, 1.0F);
            float alpha = 0.18F + 0.4F * warnProgress + 0.08F * Mth.sin(age * 0.9F);
            if (age >= sweep.warn) {
                alpha = 0.6F * (1.0F - (age - sweep.warn) / sweep.strike);
            }
            VertexConsumer fan = buffers.getBuffer(RenderType.entityTranslucent(FAN));
            float inner = 0.6F;
            for (int i = 0; i < slices; i++) {
                float a0 = sweep.yaw - sweep.halfArc + sweep.halfArc * 2.0F * i / slices;
                float a1 = sweep.yaw - sweep.halfArc + sweep.halfArc * 2.0F * (i + 1) / slices;
                Vec3 d0 = NagaBossController.directionOf(a0);
                Vec3 d1 = NagaBossController.directionOf(a1);
                float arc0 = i * 0.6F;
                float arc1 = (i + 1) * 0.6F;
                float reach = sweep.radius * Math.min(1.0F, 0.35F + warnProgress);
                FxDraw.vertex(fan, pose, (float) (sweep.x + d0.x * inner), y, (float) (sweep.z + d0.z * inner), arc0, 0.0F, 0.64F, 0.83F, 0.09F, alpha * 0.5F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(fan, pose, (float) (sweep.x + d0.x * reach), y, (float) (sweep.z + d0.z * reach), arc0, reach / 1.2F, 0.64F, 0.83F, 0.09F, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(fan, pose, (float) (sweep.x + d1.x * reach), y, (float) (sweep.z + d1.z * reach), arc1, reach / 1.2F, 0.64F, 0.83F, 0.09F, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
                FxDraw.vertex(fan, pose, (float) (sweep.x + d1.x * inner), y, (float) (sweep.z + d1.z * inner), arc1, 0.0F, 0.64F, 0.83F, 0.09F, alpha * 0.5F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            }
        }
        if (!decorative || age < sweep.warn) {
            return;
        }
        float strikeProgress = Mth.clamp((age - sweep.warn) / sweep.strike, 0.0F, 1.0F);
        float fade = 1.0F;
        if (age > sweep.warn + sweep.strike) {
            fade = 1.0F - (age - sweep.warn - sweep.strike) / SWEEP_FADE;
        }
        if (fade <= 0.0F) {
            return;
        }
        float tailStart = Math.max(0.0F, strikeProgress - 0.6F);
        VertexConsumer arc = buffers.getBuffer(RenderType.entityTranslucent(TAIL_ARC));
        float inner = sweep.radius * 0.45F;
        float outer = sweep.radius + 0.2F;
        float bandY = y + 0.9F;
        float scroll = age * 0.15F;
        for (int i = 0; i < slices; i++) {
            float p0 = tailStart + (strikeProgress - tailStart) * i / slices;
            float p1 = tailStart + (strikeProgress - tailStart) * (i + 1) / slices;
            float a0 = sweep.yaw + sweep.sign * (-sweep.halfArc + sweep.halfArc * 2.0F * p0);
            float a1 = sweep.yaw + sweep.sign * (-sweep.halfArc + sweep.halfArc * 2.0F * p1);
            Vec3 d0 = NagaBossController.directionOf(a0);
            Vec3 d1 = NagaBossController.directionOf(a1);
            float alpha0 = fade * i / slices;
            float alpha1 = fade * (i + 1) / slices;
            float u0 = scroll + i * 0.25F;
            float u1 = scroll + (i + 1) * 0.25F;
            FxDraw.vertex(arc, pose, (float) (sweep.x + d0.x * inner), bandY, (float) (sweep.z + d0.z * inner), u0, 0.0F, 1.0F, 1.0F, 1.0F, alpha0 * 0.4F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(arc, pose, (float) (sweep.x + d0.x * outer), bandY, (float) (sweep.z + d0.z * outer), u0, 1.0F, 1.0F, 1.0F, 1.0F, alpha0, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(arc, pose, (float) (sweep.x + d1.x * outer), bandY, (float) (sweep.z + d1.z * outer), u1, 1.0F, 1.0F, 1.0F, 1.0F, alpha1, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(arc, pose, (float) (sweep.x + d1.x * inner), bandY, (float) (sweep.z + d1.z * inner), u1, 0.0F, 1.0F, 1.0F, 1.0F, alpha1 * 0.4F, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);

            FxDraw.vertex(arc, pose, (float) (sweep.x + d0.x * outer), y + 0.1F, (float) (sweep.z + d0.z * outer), u0, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, FxDraw.FULL_BRIGHT, (float) d0.x, 0.0F, (float) d0.z);
            FxDraw.vertex(arc, pose, (float) (sweep.x + d0.x * outer), bandY + 0.6F, (float) (sweep.z + d0.z * outer), u0, 1.0F, 1.0F, 1.0F, 1.0F, alpha0, FxDraw.FULL_BRIGHT, (float) d0.x, 0.0F, (float) d0.z);
            FxDraw.vertex(arc, pose, (float) (sweep.x + d1.x * outer), bandY + 0.6F, (float) (sweep.z + d1.z * outer), u1, 1.0F, 1.0F, 1.0F, 1.0F, alpha1, FxDraw.FULL_BRIGHT, (float) d1.x, 0.0F, (float) d1.z);
            FxDraw.vertex(arc, pose, (float) (sweep.x + d1.x * outer), y + 0.1F, (float) (sweep.z + d1.z * outer), u1, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, FxDraw.FULL_BRIGHT, (float) d1.x, 0.0F, (float) d1.z);
        }
    }

    private static void renderHusk(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Husk husk, float partialTick) {
        float age = husk.age + partialTick;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(HUSK));
        for (int index = 0; index < husk.pieces.size(); index++) {
            float[] piece = husk.pieces.get(index);
            float size = Math.max(piece[5], 0.5F);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(piece[0], piece[1] + size * 0.5F, piece[2]));
            poseStack.pushPose();
            poseStack.translate(piece[0], piece[1] + size * 0.5F, piece[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(-piece[3]));
            poseStack.mulPose(Axis.XP.rotationDegrees(piece[4]));
            float half = size * 0.5F + 0.02F;
            if (age < HUSK_HOLD) {
                float alpha = 0.85F - 0.15F * age / HUSK_HOLD;
                float shrink = 1.0F;
                if (age < 4.0F) {
                    shrink = 1.0F + 0.08F * (1.0F - age / 4.0F);
                }
                float edge = half * shrink;
                FxDraw.box(consumer, poseStack.last(), -edge, -edge, -edge, edge, edge, edge, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, light);
            } else {
                float crumble = (age - HUSK_HOLD) / HUSK_CRUMBLE;
                float alpha = 0.7F * (1.0F - crumble);
                float pieceHalf = half * 0.5F * (1.0F - crumble * 0.45F);
                float time = age - HUSK_HOLD;
                for (int i = 0; i < 8; i++) {
                    float ox = ((i & 1) - 0.5F) * half;
                    float oy = (((i >> 1) & 1) - 0.5F) * half;
                    float oz = (((i >> 2) & 1) - 0.5F) * half;
                    float dx = ox * 0.06F * time;
                    float dz = oz * 0.06F * time;
                    float dy = oy * 0.03F * time - 0.012F * time * time;
                    float tumble = time * (8.0F + ((index + i) % 3) * 4.0F);
                    if ((i + index) % 2 == 0) {
                        tumble = -tumble;
                    }
                    poseStack.pushPose();
                    poseStack.translate(ox + dx, oy + dy, oz + dz);
                    poseStack.mulPose(Axis.XP.rotationDegrees(tumble));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(tumble * 0.6F));
                    float u0 = (i & 1) * 0.5F;
                    float v0 = ((i >> 1) & 1) * 0.5F;
                    FxDraw.box(consumer, poseStack.last(), -pieceHalf, -pieceHalf, -pieceHalf, pieceHalf, pieceHalf, pieceHalf, u0, v0, u0 + 0.5F, v0 + 0.5F, 1.0F, 1.0F, 1.0F, alpha, light);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
    }

    private static void renderCrack(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Crack crack, float partialTick) {
        if (crack.points.size() < 2) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer base = buffers.getBuffer(RenderType.entityTranslucent(CRACK));
        float distance = 0.0F;
        for (int i = 0; i < crack.points.size() - 1; i++) {
            CrackPoint p0 = crack.points.get(i);
            CrackPoint p1 = crack.points.get(i + 1);
            float length = (float) Math.sqrt((p1.x - p0.x) * (p1.x - p0.x) + (p1.z - p0.z) * (p1.z - p0.z));
            float alpha0 = crackAlpha(p0, partialTick);
            float alpha1 = crackAlpha(p1, partialTick);
            float width0 = 0.55F * crackOpen(p0, partialTick);
            float width1 = 0.55F * crackOpen(p1, partialTick);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(p0.x, p0.y + 0.5D, p0.z));
            quadAlong(base, pose, p0, p1, width0, width1, 0.03F, distance / 1.2F, (distance + length) / 1.2F, 1.0F, 1.0F, 1.0F, alpha0, alpha1, light);
            distance += length;
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(CRACK_GLOW));
        distance = 0.0F;
        for (int i = 0; i < crack.points.size() - 1; i++) {
            CrackPoint p0 = crack.points.get(i);
            CrackPoint p1 = crack.points.get(i + 1);
            float length = (float) Math.sqrt((p1.x - p0.x) * (p1.x - p0.x) + (p1.z - p0.z) * (p1.z - p0.z));
            float pulse0 = 0.55F + 0.35F * Mth.sin((p0.age + partialTick) * 0.5F + i * 0.7F);
            float pulse1 = 0.55F + 0.35F * Mth.sin((p1.age + partialTick) * 0.5F + (i + 1) * 0.7F);
            float alpha0 = crackAlpha(p0, partialTick) * pulse0;
            float alpha1 = crackAlpha(p1, partialTick) * pulse1;
            float width0 = 0.16F * crackOpen(p0, partialTick);
            float width1 = 0.16F * crackOpen(p1, partialTick);
            quadAlong(glow, pose, p0, p1, width0, width1, 0.035F, distance / 1.2F, (distance + length) / 1.2F, 1.0F, 1.0F, 1.0F, alpha0, alpha1, FxDraw.FULL_BRIGHT);
            distance += length;
        }
    }

    private static float crackAlpha(CrackPoint point, float partialTick) {
        float age = point.age + partialTick;
        if (age > CRACK_LIFE - CRACK_FADE) {
            return Mth.clamp((CRACK_LIFE - age) / CRACK_FADE, 0.0F, 1.0F);
        }
        return 1.0F;
    }

    private static float crackOpen(CrackPoint point, float partialTick) {
        return Mth.clamp((point.age + partialTick) / 4.0F, 0.2F, 1.0F);
    }

    private static void quadAlong(VertexConsumer consumer, PoseStack.Pose pose, CrackPoint p0, CrackPoint p1, float width0, float width1, float lift,
                                  float v0, float v1, float r, float g, float b, float alpha0, float alpha1, int light) {
        Vec3 side0 = NagaBossController.directionOf(p0.yaw + 90.0F);
        Vec3 side1 = NagaBossController.directionOf(p1.yaw + 90.0F);
        float y0 = (float) p0.y + lift;
        float y1 = (float) p1.y + lift;
        FxDraw.vertex(consumer, pose, (float) (p0.x - side0.x * width0), y0, (float) (p0.z - side0.z * width0), 0.0F, v0, r, g, b, alpha0, light, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (p1.x - side1.x * width1), y1, (float) (p1.z - side1.z * width1), 0.0F, v1, r, g, b, alpha1, light, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (p1.x + side1.x * width1), y1, (float) (p1.z + side1.z * width1), 1.0F, v1, r, g, b, alpha1, light, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) (p0.x + side0.x * width0), y0, (float) (p0.z + side0.z * width0), 1.0F, v0, r, g, b, alpha0, light, 0.0F, 1.0F, 0.0F);
    }

    private static void renderBite(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Bite bite, float partialTick) {
        float age = bite.age + partialTick;
        float rise;
        float angle;
        float alpha = 1.0F;
        if (age < 5.0F) {
            rise = -2.4F * (1.0F - age / 5.0F);
            angle = 55.0F;
        } else if (age < 8.0F) {
            rise = 0.0F;
            angle = 55.0F - 81.0F * (age - 5.0F) / 3.0F;
        } else if (age < 14.0F) {
            rise = 0.0F;
            angle = -26.0F;
        } else {
            float sink = (age - 14.0F) / (BITE_LIFE - 14.0F);
            rise = -2.4F * sink;
            angle = -26.0F + 10.0F * sink;
            alpha = 1.0F - sink;
        }
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(bite.x, bite.y + 1.0D, bite.z));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(JAW));
        poseStack.pushPose();
        poseStack.translate(bite.x, bite.y, bite.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bite.yaw));
        for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
            float side = 1.0F;
            if (sideIndex == 1) {
                side = -1.0F;
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, rise, side * 0.9F);
            poseStack.mulPose(Axis.XP.rotationDegrees(side * angle));
            PoseStack.Pose pose = poseStack.last();
            FxDraw.box(consumer, pose, -1.3F, 0.0F, -0.28F, 1.3F, 0.75F, 0.28F, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, light);
            for (int fang = 0; fang < 5; fang++) {
                float fx = -1.0F + fang * 0.5F;
                float height = 1.05F;
                if (fang % 2 == 1) {
                    height = 0.8F;
                }
                if (fang == 2) {
                    height = 1.25F;
                }
                float inner = -side * 0.2F;
                float z0 = Math.min(inner - 0.11F, inner + 0.11F);
                float z1 = Math.max(inner - 0.11F, inner + 0.11F);
                FxDraw.box(consumer, pose, fx - 0.12F, 0.75F, z0, fx + 0.12F, 0.75F + height, z1, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, light);
                FxDraw.box(consumer, pose, fx - 0.06F, 0.75F + height, z0 + 0.05F, fx + 0.06F, 0.75F + height + 0.22F, z1 - 0.05F, 0.5F, 0.0F, 1.0F, 0.3F, 1.0F, 1.0F, 1.0F, alpha, light);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderDebris(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, float partialTick) {
        if (DEBRIS.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (Debris debris : DEBRIS) {
            double x = Mth.lerp(partialTick, debris.px, debris.x);
            double y = Mth.lerp(partialTick, debris.py, debris.y);
            double z = Mth.lerp(partialTick, debris.pz, debris.z);
            float age = debris.age + partialTick;
            float size = debris.size * Mth.clamp(1.0F - (age - debris.life * 0.6F) / (debris.life * 0.4F), 0.0F, 1.0F);
            if (size <= 0.0F) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * debris.spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * debris.spin * 0.7F));
            float u0 = debris.sprite.getU(debris.u);
            float u1 = debris.sprite.getU(debris.u + 4);
            float v0 = debris.sprite.getV(debris.v);
            float v1 = debris.sprite.getV(debris.v + 4);
            FxDraw.box(consumer, poseStack.last(), -size, -size, -size, size, size, size, u0, v0, u1, v1, 1.0F, 1.0F, 1.0F, 1.0F, light);
            poseStack.popPose();
        }
    }

    private static void renderConstrict(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Constrict constrict, float partialTick) {
        float age = constrict.age + partialTick;
        float progress = Mth.clamp(age / constrict.duration, 0.0F, 1.0F);
        float radius = constrict.startRadius + (constrict.endRadius - constrict.startRadius) * progress;
        float rise = Mth.clamp(age / constrict.warmup, 0.0F, 1.0F);
        float height = 2.8F * rise;
        float fade = 1.0F;
        if (constrict.endAge >= 0) {
            fade = Mth.clamp(1.0F - (age - constrict.endAge) / CONSTRICT_FADE, 0.0F, 1.0F);
            if (constrict.broken) {
                height += (age - constrict.endAge) * 0.25F;
            }
        }
        if (fade <= 0.0F) {
            return;
        }
        float r = 0.64F;
        float g = 0.83F;
        float b = 0.09F;
        float baseAlpha = 0.35F;
        if (age >= constrict.warmup) {
            r = 0.75F;
            g = 0.95F;
            b = 0.30F;
            baseAlpha = 0.7F;
        }
        if (constrict.broken && constrict.endAge >= 0) {
            r = 0.95F;
            g = 0.76F;
            b = 0.31F;
        }
        baseAlpha *= fade;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer wall = buffers.getBuffer(RenderType.entityTranslucent(WALL));
        int segments = 48;
        float circumference = Mth.TWO_PI * radius;
        float scroll = -age * 0.04F;
        float y0 = (float) constrict.y;
        float yMid = y0 + height * 0.45F;
        float yTop = y0 + height;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float x0 = (float) constrict.x + Mth.cos(a0) * radius;
            float z0 = (float) constrict.z + Mth.sin(a0) * radius;
            float x1 = (float) constrict.x + Mth.cos(a1) * radius;
            float z1 = (float) constrict.z + Mth.sin(a1) * radius;
            float u0 = circumference * i / segments / 1.5F;
            float u1 = circumference * (i + 1) / segments / 1.5F;
            float nx = Mth.cos((a0 + a1) * 0.5F);
            float nz = Mth.sin((a0 + a1) * 0.5F);
            float vBottom = scroll + height / 1.5F;
            float vMid = scroll + (height - height * 0.45F) / 1.5F;
            float vTop = scroll;
            FxDraw.vertex(wall, pose, x0, y0, z0, u0, vBottom, r, g, b, baseAlpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(wall, pose, x0, yMid, z0, u0, vMid, r, g, b, baseAlpha * 0.7F, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(wall, pose, x1, yMid, z1, u1, vMid, r, g, b, baseAlpha * 0.7F, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(wall, pose, x1, y0, z1, u1, vBottom, r, g, b, baseAlpha, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);

            FxDraw.vertex(wall, pose, x0, yMid, z0, u0, vMid, r, g, b, baseAlpha * 0.7F, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(wall, pose, x0, yTop, z0, u0, vTop, r, g, b, 0.0F, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(wall, pose, x1, yTop, z1, u1, vTop, r, g, b, 0.0F, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
            FxDraw.vertex(wall, pose, x1, yMid, z1, u1, vMid, r, g, b, baseAlpha * 0.7F, FxDraw.FULL_BRIGHT, nx, 0.0F, nz);
        }
        VertexConsumer ground = buffers.getBuffer(RenderType.entityTranslucent(RING));
        FxDraw.flatSquare(ground, pose, (float) constrict.x, y0 + 0.03F, (float) constrict.z, radius * 1.06F, age * 0.02F, r, g, b, baseAlpha, FxDraw.FULL_BRIGHT);

        if (constrict.endAge >= 0 || constrict.weakIndex < 0) {
            return;
        }
        Entity entity = level.getEntity(constrict.nagaId);
        if (!(entity instanceof Naga naga)) {
            return;
        }
        PartEntity<?>[] parts = naga.getParts();
        if (parts == null || constrict.weakIndex >= parts.length) {
            return;
        }
        PartEntity<?> part = parts[constrict.weakIndex];
        if (part.getBbWidth() <= 0.0F) {
            return;
        }
        double px = Mth.lerp(partialTick, part.xo, part.getX());
        double py = Mth.lerp(partialTick, part.yo, part.getY());
        double pz = Mth.lerp(partialTick, part.zo, part.getZ());
        float yaw = Mth.rotLerp(partialTick, part.yRotO, part.getYRot());
        float half = part.getBbWidth() * 0.5F + 0.08F;
        float pulse = 0.5F + 0.3F * Mth.sin(age * 0.35F);
        float wr = 1.0F;
        float wg = 1.0F;
        float wb = 1.0F;
        if (constrict.flash > 0) {
            pulse = 1.0F;
            wb = 1.0F;
        } else {
            wb = 0.85F;
        }
        VertexConsumer weak = buffers.getBuffer(RenderType.entityTranslucent(WEAK_SCALE));
        poseStack.pushPose();
        poseStack.translate(px, py + part.getBbHeight() * 0.5D, pz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        float grow = 1.0F;
        if (constrict.flash > 0) {
            grow = 1.0F + constrict.flash * 0.03F;
        }
        float edge = half * grow;
        FxDraw.box(weak, poseStack.last(), -edge, -edge, -edge, edge, edge, edge, 0.0F, 0.0F, 1.0F, 1.0F, wr, wg, wb, pulse, FxDraw.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static final class Ring {
        final double x;
        final double y;
        final double z;
        final float radius;
        final int duration;
        final int kind;
        int age;

        Ring(double x, double y, double z, float radius, int duration, int kind) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.duration = duration;
            this.kind = kind;
        }
    }

    private static final class Sweep {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float radius;
        final float halfArc;
        final int warn;
        final int strike;
        final float sign;
        int age;

        Sweep(double x, double y, double z, float yaw, float radius, float halfArc, int warn, int strike, float sign) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.radius = radius;
            this.halfArc = halfArc;
            this.warn = warn;
            this.strike = strike;
            this.sign = sign;
        }
    }

    private static final class Husk {
        final List<float[]> pieces = new ArrayList<>();
        int age;
    }

    private static final class CrackPoint {
        final double x;
        final double y;
        final double z;
        final float yaw;
        int age;

        CrackPoint(double x, double y, double z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }

    private static final class Crack {
        final int nagaId;
        final List<CrackPoint> points = new ArrayList<>();
        boolean open = true;
        int idle;

        Crack(int nagaId) {
            this.nagaId = nagaId;
        }
    }

    private static final class Debris {
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
        int u;
        int v;
        TextureAtlasSprite sprite;
    }

    private static final class Bite {
        final double x;
        final double y;
        final double z;
        final float yaw;
        int age;

        Bite(double x, double y, double z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }

    private static final class Constrict {
        final int nagaId;
        final double x;
        final double y;
        final double z;
        final float startRadius;
        final float endRadius;
        final int duration;
        final int warmup;
        final int weakIndex;
        int age;
        int endAge = -1;
        boolean broken;
        int flash;

        Constrict(int nagaId, double x, double y, double z, float startRadius, float endRadius, int duration, int warmup, int weakIndex) {
            this.nagaId = nagaId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.duration = duration;
            this.warmup = warmup;
            this.weakIndex = weakIndex;
        }
    }
}
