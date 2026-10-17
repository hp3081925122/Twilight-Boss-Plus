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
import net.minecraft.util.RandomSource;
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
import org.joml.Vector4f;
import twilightforest.entity.boss.Naga;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NagaFxManager {
    private static final ResourceLocation RING = tex("ring");
    private static final ResourceLocation RING_FILL = tex("ring_fill");
    private static final ResourceLocation SCALES = tex("scales");
    private static final ResourceLocation TAIL_ARC = tex("tail_arc");
    private static final ResourceLocation HUSK = tex("husk");
    private static final ResourceLocation CRACK = tex("crack");
    private static final ResourceLocation CRACK_GLOW = tex("crack_glow");
    private static final ResourceLocation JAW = tex("jaw");
    private static final ResourceLocation WALL = tex("wall");
    private static final ResourceLocation WEAK_SCALE = tex("weak_scale");
    public static final ResourceLocation DROPLET = tex("px/droplet");
    private static final ResourceLocation FLAKE = tex("px/flake");

    private static final float DARK_R = 0.106F;
    private static final float DARK_G = 0.220F;
    private static final float DARK_B = 0.043F;
    private static final float GREEN_R = 0.643F;
    private static final float GREEN_G = 0.827F;
    private static final float GREEN_B = 0.086F;
    private static final float VENOM_R = 0.784F;
    private static final float VENOM_G = 0.878F;
    private static final float VENOM_B = 0.290F;
    private static final float GOLD_R = 0.949F;
    private static final float GOLD_G = 0.757F;
    private static final float GOLD_B = 0.306F;
    private static final float BLOOD_R = 0.95F;
    private static final float BLOOD_G = 0.24F;
    private static final float BLOOD_B = 0.12F;

    private static final int HUSK_HOLD = 60;
    private static final int HUSK_CRUMBLE = 24;
    private static final int CRACK_LIFE = 70;
    private static final int CRACK_FADE = 20;
    private static final int BITE_LIFE = 24;
    private static final int SWEEP_FADE = 8;
    private static final int CONSTRICT_FADE = 14;

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

    private static float angleOf(float yaw) {
        return (yaw + 90.0F) * Mth.DEG_TO_RAD;
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
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
            float[] head = husk.pieces.get(0);
            FxShake.at(head[0], head[1], head[2], 0.6F, 20.0F, 10);
            for (float[] piece : husk.pieces) {
                for (int i = 0; i < 4; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double speed = 0.06D + random.nextDouble() * 0.1D;
                    FxParticles.glow(FLAKE, piece[0], piece[1] + piece[5] * 0.5D, piece[2], Math.cos(angle) * speed, 0.05D + random.nextDouble() * 0.12D,
                            Math.sin(angle) * speed, 26 + random.nextInt(14), 0.22F + random.nextFloat() * 0.12F)
                        .color(GREEN_R, GREEN_G, GREEN_B).fadeTo(DARK_R, DARK_G * 2.0F, DARK_B).gravity(0.004D).drag(0.94D)
                        .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.4F);
                }
            }
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
            FxParticles.soft(FxTextures.DUST, data[0] + (random.nextDouble() - 0.5D) * 0.6D, data[1] + 0.2D, data[2] + (random.nextDouble() - 0.5D) * 0.6D,
                    (random.nextDouble() - 0.5D) * 0.04D, 0.03D + random.nextDouble() * 0.03D, (random.nextDouble() - 0.5D) * 0.04D, 22 + random.nextInt(10),
                    0.8F + random.nextFloat() * 0.5F)
                .color(0.36F, 0.32F, 0.24F).alpha(0.55F).grow(1.8F).drag(0.9D).fade(0.15F, 1.5F).spin(random.nextFloat() * 6.0F, 0.02F);
            for (int i = 0; i < 2; i++) {
                FxParticles.glow(FxTextures.MOTE, data[0] + (random.nextDouble() - 0.5D) * 0.5D, data[1] + 0.1D, data[2] + (random.nextDouble() - 0.5D) * 0.5D,
                        (random.nextDouble() - 0.5D) * 0.03D, 0.06D + random.nextDouble() * 0.08D, (random.nextDouble() - 0.5D) * 0.03D, 18 + random.nextInt(12),
                        0.18F + random.nextFloat() * 0.12F)
                    .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.95D).flicker(0.4F, random.nextFloat() * 10.0F);
            }
        } else if (type == NagaFxPacket.BITE && data.length >= 4) {
            for (Crack existing : CRACKS) {
                if (existing.nagaId == entityId) {
                    existing.open = false;
                }
            }
            BITES.add(new Bite(data[0], data[1], data[2], data[3]));
            spawnDebris(level, data[0], data[1], data[2], 14);
            FxShake.at(data[0], data[1], data[2], 1.4F, 18.0F, 12);
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2.0D * i / 14.0D + random.nextDouble() * 0.3D;
                double speed = 0.12D + random.nextDouble() * 0.1D;
                FxParticles.soft(FxTextures.DUST, data[0] + Math.cos(angle) * 0.8D, data[1] + 0.3D, data[2] + Math.sin(angle) * 0.8D,
                        Math.cos(angle) * speed, 0.03D + random.nextDouble() * 0.04D, Math.sin(angle) * speed, 26 + random.nextInt(12), 1.2F)
                    .color(0.38F, 0.33F, 0.25F).alpha(0.6F).grow(2.6F).drag(0.88D).fade(0.1F, 1.4F).spin(random.nextFloat() * 6.0F, 0.03F);
            }
            for (int i = 0; i < 18; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.08D + random.nextDouble() * 0.16D;
                FxParticles.glow(DROPLET, data[0], data[1] + 1.4D, data[2], Math.cos(angle) * speed, 0.25D + random.nextDouble() * 0.25D, Math.sin(angle) * speed,
                        22 + random.nextInt(10), 0.16F + random.nextFloat() * 0.12F)
                    .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).gravity(0.035D).drag(0.98D).streak(1.6F).floor(data[1] + 0.05D, 0.0D);
            }
        } else if (type == NagaFxPacket.CONSTRICT_START && data.length >= 8) {
            CONSTRICTS.removeIf(constrict -> constrict.nagaId == entityId);
            CONSTRICTS.add(new Constrict(entityId, data[0], data[1], data[2], data[3], data[4], (int) data[5], (int) data[6], (int) data[7]));
        } else if (type == NagaFxPacket.CONSTRICT_END && data.length >= 1) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId && constrict.endAge < 0) {
                    constrict.endAge = constrict.age;
                    constrict.broken = data[0] > 0.5F;
                    if (constrict.broken) {
                        burstConstrict(random, constrict);
                    }
                }
            }
        } else if (type == NagaFxPacket.WEAK_HIT) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId) {
                    constrict.flash = 8;
                    Vec3 weak = weakScalePos(level, constrict, 1.0F);
                    if (weak != null) {
                        FxShake.at(weak.x, weak.y, weak.z, 0.5F, 12.0F, 6);
                        for (int i = 0; i < 16; i++) {
                            Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.2D, random.nextDouble() - 0.5D).normalize().scale(0.25D + random.nextDouble() * 0.2D);
                            FxParticles.glow(FxTextures.SPARK, weak.x, weak.y, weak.z, dir.x, dir.y, dir.z, 8 + random.nextInt(6), 0.14F)
                                .color(1.0F, 0.95F, 0.7F).fadeTo(GOLD_R, GOLD_G * 0.7F, GOLD_B * 0.4F).drag(0.82D).gravity(0.01D).streak(2.5F);
                        }
                    }
                }
            }
        }
    }

    private static void burstConstrict(RandomSource random, Constrict constrict) {
        FxShake.at(constrict.x, constrict.y, constrict.z, 1.2F, 24.0F, 14);
        float radius = constrict.currentRadius(constrict.age);
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2.0D * i / 48.0D;
            double speed = 0.1D + random.nextDouble() * 0.12D;
            FxParticles.glow(FLAKE, constrict.x + Math.cos(angle) * radius, constrict.y + 0.2D + random.nextDouble() * 2.4D, constrict.z + Math.sin(angle) * radius,
                    Math.cos(angle) * speed, 0.1D + random.nextDouble() * 0.15D, Math.sin(angle) * speed, 26 + random.nextInt(14), 0.26F)
                .color(1.0F, 0.92F, 0.6F).fadeTo(GOLD_R, GOLD_G * 0.6F, GOLD_B * 0.3F).gravity(0.012D).drag(0.94D)
                .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.5F);
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
            debris.vx = (level.random.nextDouble() - 0.5D) * 0.2D;
            debris.vy = 0.24D + level.random.nextDouble() * 0.2D;
            debris.vz = (level.random.nextDouble() - 0.5D) * 0.2D;
            debris.size = 0.08F + level.random.nextFloat() * 0.08F;
            debris.spin = (level.random.nextFloat() - 0.5F) * 40.0F;
            debris.life = 16 + level.random.nextInt(8);
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
        ClientLevel level = minecraft.level;
        if (level == null) {
            clearAll();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        RandomSource random = level.random;
        Iterator<Ring> rings = RINGS.iterator();
        while (rings.hasNext()) {
            Ring ring = rings.next();
            ring.age++;
            float t = (float) ring.age / Math.max(1, ring.duration);
            if (ring.age % 2 == 0) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = Math.sqrt(random.nextDouble()) * ring.radius;
                float r = VENOM_R;
                float g = VENOM_G;
                float b = VENOM_B;
                if (ring.kind == 1) {
                    r = BLOOD_R;
                    g = BLOOD_G + 0.25F;
                    b = BLOOD_B;
                }
                FxParticles.glow(FxTextures.MOTE, ring.x + Math.cos(angle) * dist, ring.y + 0.1D, ring.z + Math.sin(angle) * dist,
                        0.0D, 0.04D + random.nextDouble() * 0.05D + t * 0.05D, 0.0D, 16 + random.nextInt(10), 0.2F + random.nextFloat() * 0.15F)
                    .color(r, g, b).drag(0.97D).flicker(0.3F, random.nextFloat() * 10.0F);
            }
            if (ring.age == ring.duration) {
                for (int i = 0; i < 10; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double speed = 0.05D + random.nextDouble() * 0.12D;
                    FxParticles.glow(DROPLET, ring.x, ring.y + 0.2D, ring.z, Math.cos(angle) * speed, 0.2D + random.nextDouble() * 0.15D, Math.sin(angle) * speed,
                            18 + random.nextInt(8), 0.16F)
                        .color(VENOM_R, VENOM_G, VENOM_B).gravity(0.035D).streak(1.5F).floor(ring.y + 0.05D, 0.0D);
                }
            }
            if (ring.age > ring.duration + 6) {
                rings.remove();
            }
        }
        Iterator<Sweep> sweeps = SWEEPS.iterator();
        while (sweeps.hasNext()) {
            Sweep sweep = sweeps.next();
            sweep.age++;
            if (sweep.age == sweep.warn) {
                FxShake.at(sweep.x, sweep.y, sweep.z, 0.45F, 14.0F, 8);
            }
            if (sweep.age >= sweep.warn && sweep.age <= sweep.warn + sweep.strike) {
                float p = Mth.clamp((float) (sweep.age - sweep.warn) / Math.max(1, sweep.strike), 0.0F, 1.0F);
                float angle = sweep.yaw + sweep.sign * (-sweep.halfArc + sweep.halfArc * 2.0F * p);
                Vec3 dir = NagaBossController.directionOf(angle);
                Vec3 tangent = NagaBossController.directionOf(angle + 90.0F * sweep.sign);
                for (int i = 0; i < 4; i++) {
                    double dist = sweep.radius * (0.5D + random.nextDouble() * 0.55D);
                    double speed = 0.25D + random.nextDouble() * 0.2D;
                    FxParticles.glow(FxTextures.SPARK, sweep.x + dir.x * dist, sweep.y + 0.6D + random.nextDouble() * 0.8D, sweep.z + dir.z * dist,
                            tangent.x * speed + dir.x * 0.05D, 0.02D + random.nextDouble() * 0.06D, tangent.z * speed + dir.z * 0.05D, 7 + random.nextInt(5), 0.12F)
                        .color(1.0F, 1.0F, 0.75F).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.84D).streak(2.2F);
                }
                double dist = sweep.radius * (0.7D + random.nextDouble() * 0.3D);
                FxParticles.soft(FxTextures.DUST, sweep.x + dir.x * dist, sweep.y + 0.2D, sweep.z + dir.z * dist, tangent.x * 0.06D, 0.03D, tangent.z * 0.06D,
                        20, 0.9F)
                    .color(0.4F, 0.36F, 0.27F).alpha(0.45F).grow(2.0F).drag(0.9D).fade(0.15F, 1.4F);
            }
            if (sweep.age > sweep.warn + sweep.strike + SWEEP_FADE) {
                sweeps.remove();
            }
        }
        Iterator<Husk> husks = HUSKS.iterator();
        while (husks.hasNext()) {
            Husk husk = husks.next();
            husk.age++;
            if (husk.age == HUSK_HOLD) {
                for (float[] piece : husk.pieces) {
                    FxParticles.soft(FxTextures.DUST, piece[0], piece[1] + piece[5] * 0.5D, piece[2], (random.nextDouble() - 0.5D) * 0.04D, 0.02D,
                            (random.nextDouble() - 0.5D) * 0.04D, 30, piece[5] * 1.2F)
                        .color(0.78F, 0.8F, 0.72F).alpha(0.5F).grow(piece[5] * 2.6F).drag(0.9D).fade(0.1F, 1.3F).spin(random.nextFloat() * 6.0F, 0.02F);
                }
            }
            if (husk.age > HUSK_HOLD + HUSK_CRUMBLE) {
                husks.remove();
            }
        }
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
            } else if (crack.points.size() > 1 && random.nextInt(3) == 0) {
                CrackPoint point = crack.points.get(random.nextInt(crack.points.size()));
                if (point.age < CRACK_LIFE - CRACK_FADE) {
                    FxParticles.glow(FxTextures.MOTE, point.x + (random.nextDouble() - 0.5D) * 0.3D, point.y + 0.05D, point.z + (random.nextDouble() - 0.5D) * 0.3D,
                            0.0D, 0.05D + random.nextDouble() * 0.05D, 0.0D, 20 + random.nextInt(10), 0.16F)
                        .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(DARK_R * 2.0F, DARK_G * 2.0F, DARK_B * 2.0F).drag(0.96D);
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
            debris.vy -= 0.045D;
            debris.vx *= 0.93D;
            debris.vz *= 0.93D;
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
            if (constrict.age == constrict.warmup && constrict.endAge < 0) {
                FxShake.at(constrict.x, constrict.y, constrict.z, 0.5F, 20.0F, 8);
            }
            if (constrict.endAge < 0) {
                float radius = constrict.currentRadius(constrict.age);
                float height = 2.8F * Mth.clamp((float) constrict.age / Math.max(1, constrict.warmup), 0.0F, 1.0F);
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    FxParticles.glow(FxTextures.MOTE, constrict.x + Math.cos(angle) * radius, constrict.y + random.nextDouble() * height, constrict.z + Math.sin(angle) * radius,
                            0.0D, 0.04D + random.nextDouble() * 0.05D, 0.0D, 18 + random.nextInt(10), 0.18F + random.nextFloat() * 0.1F)
                        .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.97D).flicker(0.35F, random.nextFloat() * 10.0F);
                }
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
        FxDraw.camera(event.getCamera());
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
                renderCrack(poseStack, buffers, crack, partialTick);
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
        float age = ring.age + partialTick;
        float t = Mth.clamp(age / Math.max(1, ring.duration), 0.0F, 1.0F);
        float after = Mth.clamp((age - ring.duration) / 6.0F, 0.0F, 1.0F);
        float fade = 1.0F - after;
        float r = VENOM_R;
        float g = VENOM_G;
        float b = VENOM_B;
        float spin = 1.0F;
        if (ring.kind == 1) {
            r = BLOOD_R;
            g = BLOOD_G;
            b = BLOOD_B;
            spin = -1.6F;
        }
        float pulse = 0.85F + 0.15F * Mth.sin(age * (0.6F + t * 0.8F));
        PoseStack.Pose pose = poseStack.last();
        double x = ring.x;
        double y = ring.y + 0.04D;
        double z = ring.z;

        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, x, y, z, ring.radius * 1.5F, 0.0F, r, g, b, (0.25F + 0.35F * t) * pulse * fade);
        if (t > 0.8F) {
            float flash = (t - 0.8F) / 0.2F;
            FxDraw.flat(glow, pose, x, y + 0.01D, z, ring.radius * (0.6F + 0.6F * flash), 0.0F, 1.0F, 1.0F, 0.85F, 0.55F * flash * fade);
        }
        if (after > 0.0F) {
            FxDraw.flat(glow, pose, x, y + 0.02D, z, ring.radius * 1.8F, 0.0F, 1.0F, 1.0F, 0.9F, 0.9F * fade);
        }

        VertexConsumer fill = buffers.getBuffer(FxRenderTypes.glow(RING_FILL));
        FxDraw.flat(fill, pose, x, y + 0.005D, z, ring.radius, age * 0.02F * spin, r, g, b, (0.3F + 0.45F * t) * fade);

        VertexConsumer scales = buffers.getBuffer(FxRenderTypes.glow(RING));
        float contract = ring.radius * (2.1F - 1.1F * t * t);
        FxDraw.flat(scales, pose, x, y + 0.01D, z, contract, -age * 0.05F * spin, r, g, b, (0.35F + 0.65F * t) * fade);
        FxDraw.flat(scales, pose, x, y + 0.012D, z, ring.radius, age * 0.03F * spin, r * 0.6F + 0.4F, g * 0.6F + 0.4F, b * 0.6F + 0.4F, (0.25F + 0.4F * t) * pulse * fade);

        VertexConsumer edge = buffers.getBuffer(FxRenderTypes.glow(FxTextures.RING));
        FxDraw.flat(edge, pose, x, y + 0.015D, z, ring.radius / 0.86F, 0.0F, r, g, b, (0.5F + 0.5F * t) * fade);

        VertexConsumer wall = buffers.getBuffer(FxRenderTypes.glow(FxTextures.STREAKS));
        float height = 0.5F + 0.9F * t;
        if (ring.kind == 1) {
            height *= 1.6F;
        }
        FxDraw.wall(wall, pose, x, y, z, ring.radius, height, 32, Math.max(2.0F, ring.radius * 2.0F), -age * 0.03F, r, g, b, (0.25F + 0.45F * t) * fade, 0.0F);
    }

    private static void renderSweep(PoseStack poseStack, MultiBufferSource buffers, Sweep sweep, float partialTick, boolean decorative) {
        float age = sweep.age + partialTick;
        PoseStack.Pose pose = poseStack.last();
        int slices = 20;
        double y = sweep.y + 0.04D;
        float startAngle = angleOf(sweep.yaw - sweep.halfArc * sweep.sign);
        float endAngle = angleOf(sweep.yaw + sweep.halfArc * sweep.sign);
        if (age < sweep.warn + sweep.strike) {
            float warnProgress = Mth.clamp(age / sweep.warn, 0.0F, 1.0F);
            float alpha = (0.3F + 0.45F * warnProgress) * (0.85F + 0.15F * Mth.sin(age * 1.1F));
            if (age >= sweep.warn) {
                alpha = 0.75F * (1.0F - (age - sweep.warn) / sweep.strike);
            }
            float reach = sweep.radius * Math.min(1.0F, 0.4F + warnProgress);
            VertexConsumer fan = buffers.getBuffer(FxRenderTypes.glow(SCALES));
            FxDraw.arc(fan, pose, sweep.x, y, sweep.z, 0.6F, reach, startAngle, endAngle, slices, sweep.halfArc / 12.0F,
                GREEN_R, GREEN_G, GREEN_B, alpha * 0.25F, alpha * 0.8F);
            VertexConsumer line = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.arc(line, pose, sweep.x, y + 0.01D, sweep.z, reach - 0.14F, reach + 0.14F, startAngle, endAngle, slices, 1.0F,
                VENOM_R, VENOM_G, VENOM_B, alpha * 1.2F, alpha * 1.2F);
            if (age < sweep.warn) {
                float scan = (age * 0.09F) % 1.0F;
                float s0 = Mth.lerp(Math.max(0.0F, scan - 0.18F), startAngle, endAngle);
                float s1 = Mth.lerp(scan, startAngle, endAngle);
                VertexConsumer scanner = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SOLID));
                FxDraw.arc(scanner, pose, sweep.x, y + 0.02D, sweep.z, 0.6F, reach, s0, s1, 6, 1.0F, VENOM_R, VENOM_G, VENOM_B, 0.0F, alpha * 0.35F);
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
        float eased = 1.0F - (1.0F - strikeProgress) * (1.0F - strikeProgress);
        float tailStart = Math.max(0.0F, eased - 0.7F);
        float a0 = angleOf(sweep.yaw + sweep.sign * (-sweep.halfArc + sweep.halfArc * 2.0F * tailStart));
        float a1 = angleOf(sweep.yaw + sweep.sign * (-sweep.halfArc + sweep.halfArc * 2.0F * eased));
        float inner = sweep.radius * 0.35F;
        float outer = sweep.radius + 0.4F;
        double bandY = y + 0.9D;
        VertexConsumer arc = buffers.getBuffer(FxRenderTypes.glow(TAIL_ARC));
        for (int i = 0; i < slices; i++) {
            float p0 = (float) i / slices;
            float p1 = (float) (i + 1) / slices;
            float b0 = Mth.lerp(p0, a0, a1);
            float b1 = Mth.lerp(p1, a0, a1);
            float f0 = fade * p0 * p0;
            float f1 = fade * p1 * p1;
            float u0 = p0 * 2.0F - age * 0.1F;
            float u1 = p1 * 2.0F - age * 0.1F;
            float c0 = Mth.cos(b0);
            float s0 = Mth.sin(b0);
            float c1 = Mth.cos(b1);
            float s1 = Mth.sin(b1);
            FxDraw.glowVertex(arc, pose, sweep.x + c0 * inner, bandY - 0.25D, sweep.z + s0 * inner, u0, 0.0F, GREEN_R, GREEN_G, GREEN_B, f0);
            FxDraw.glowVertex(arc, pose, sweep.x + c0 * outer, bandY + 0.15D, sweep.z + s0 * outer, u0, 1.0F, GREEN_R, GREEN_G, GREEN_B, f0);
            FxDraw.glowVertex(arc, pose, sweep.x + c1 * outer, bandY + 0.15D, sweep.z + s1 * outer, u1, 1.0F, GREEN_R, GREEN_G, GREEN_B, f1);
            FxDraw.glowVertex(arc, pose, sweep.x + c1 * inner, bandY - 0.25D, sweep.z + s1 * inner, u1, 0.0F, GREEN_R, GREEN_G, GREEN_B, f1);
            float hotInner = outer - 0.6F;
            FxDraw.glowVertex(arc, pose, sweep.x + c0 * hotInner, bandY, sweep.z + s0 * hotInner, u0, 0.0F, 1.0F, 1.0F, 0.85F, f0);
            FxDraw.glowVertex(arc, pose, sweep.x + c0 * outer, bandY + 0.15D, sweep.z + s0 * outer, u0, 1.0F, 1.0F, 1.0F, 0.85F, f0);
            FxDraw.glowVertex(arc, pose, sweep.x + c1 * outer, bandY + 0.15D, sweep.z + s1 * outer, u1, 1.0F, 1.0F, 1.0F, 0.85F, f1);
            FxDraw.glowVertex(arc, pose, sweep.x + c1 * hotInner, bandY, sweep.z + s1 * hotInner, u1, 0.0F, 1.0F, 1.0F, 0.85F, f1);
            FxDraw.glowVertex(arc, pose, sweep.x + c0 * outer, y + 0.1D, sweep.z + s0 * outer, u0, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.0F);
            FxDraw.glowVertex(arc, pose, sweep.x + c0 * outer, bandY + 0.9D, sweep.z + s0 * outer, u0, 1.0F, VENOM_R, VENOM_G, VENOM_B, f0 * 0.8F);
            FxDraw.glowVertex(arc, pose, sweep.x + c1 * outer, bandY + 0.9D, sweep.z + s1 * outer, u1, 1.0F, VENOM_R, VENOM_G, VENOM_B, f1 * 0.8F);
            FxDraw.glowVertex(arc, pose, sweep.x + c1 * outer, y + 0.1D, sweep.z + s1 * outer, u1, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.0F);
        }
        VertexConsumer floor = buffers.getBuffer(FxRenderTypes.glow(SCALES));
        FxDraw.arc(floor, pose, sweep.x, y + 0.02D, sweep.z, 0.6F, sweep.radius, a0, a1, slices, sweep.halfArc / 12.0F, VENOM_R, VENOM_G, VENOM_B, 0.0F, 0.6F * fade);
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        float tipX = Mth.cos(a1) * outer;
        float tipZ = Mth.sin(a1) * outer;
        FxDraw.sprite(flare, pose, sweep.x + tipX, bandY + 0.1D, sweep.z + tipZ, 2.2F * fade, age * 0.2F, 1.0F, 1.0F, 0.8F, fade);
        FxDraw.sprite(flare, pose, sweep.x + tipX, bandY + 0.1D, sweep.z + tipZ, 3.4F * fade, -age * 0.1F, GREEN_R, GREEN_G, GREEN_B, 0.6F * fade);
    }

    private static void renderHusk(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Husk husk, float partialTick) {
        float age = husk.age + partialTick;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(HUSK));
        drawHusk(poseStack, consumer, level, husk, age, false);
        if (age < 18.0F || (age > HUSK_HOLD && age < HUSK_HOLD + 8.0F)) {
            VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(HUSK));
            drawHusk(poseStack, glow, level, husk, age, true);
        }
        if (age < 16.0F) {
            float[] head = husk.pieces.get(0);
            float t = age / 16.0F;
            float ease = 1.0F - (1.0F - t) * (1.0F - t);
            VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
            FxDraw.flat(shock, poseStack.last(), head[0], head[1] + 0.08D, head[2], 1.0F + 9.0F * ease, 0.0F, GREEN_R, GREEN_G, GREEN_B, 0.9F * (1.0F - t));
            VertexConsumer light = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            FxDraw.flat(light, poseStack.last(), head[0], head[1] + 0.06D, head[2], 4.0F + 3.0F * ease, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.7F * (1.0F - t));
        }
    }

    private static void drawHusk(PoseStack poseStack, VertexConsumer consumer, ClientLevel level, Husk husk, float age, boolean glow) {
        for (int index = 0; index < husk.pieces.size(); index++) {
            float[] piece = husk.pieces.get(index);
            float size = Math.max(piece[5], 0.5F);
            int light = FxDraw.FULL_BRIGHT;
            float glowAlpha = 0.0F;
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            if (glow) {
                if (age < 18.0F) {
                    glowAlpha = 0.85F * (1.0F - age / 18.0F);
                } else {
                    glowAlpha = 0.6F * (1.0F - (age - HUSK_HOLD) / 8.0F);
                }
                r = GREEN_R;
                g = GREEN_G;
                b = GREEN_B;
            } else {
                light = LevelRenderer.getLightColor(level, BlockPos.containing(piece[0], piece[1] + size * 0.5F, piece[2]));
            }
            poseStack.pushPose();
            poseStack.translate(piece[0], piece[1] + size * 0.5F, piece[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(-piece[3]));
            poseStack.mulPose(Axis.XP.rotationDegrees(piece[4]));
            float half = size * 0.5F + 0.02F;
            if (age < HUSK_HOLD) {
                float alpha = 0.85F - 0.15F * age / HUSK_HOLD;
                float shrink = 1.0F;
                if (age < 4.0F) {
                    shrink = 1.0F + 0.1F * (1.0F - age / 4.0F);
                }
                if (glow) {
                    alpha = glowAlpha;
                    shrink += 0.04F;
                }
                float edge = half * shrink;
                FxDraw.box(consumer, poseStack.last(), -edge, -edge, -edge, edge, edge, edge, 0.0F, 0.0F, 1.0F, 1.0F, r, g, b, alpha, light);
            } else {
                float crumble = (age - HUSK_HOLD) / HUSK_CRUMBLE;
                float alpha = 0.75F * (1.0F - crumble);
                if (glow) {
                    alpha = glowAlpha;
                }
                float pieceHalf = half * 0.5F * (1.0F - crumble * 0.45F);
                float time = age - HUSK_HOLD;
                for (int i = 0; i < 8; i++) {
                    float ox = ((i & 1) - 0.5F) * half;
                    float oy = (((i >> 1) & 1) - 0.5F) * half;
                    float oz = (((i >> 2) & 1) - 0.5F) * half;
                    float dx = ox * 0.07F * time;
                    float dz = oz * 0.07F * time;
                    float dy = oy * 0.03F * time - 0.014F * time * time;
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
                    FxDraw.box(consumer, poseStack.last(), -pieceHalf, -pieceHalf, -pieceHalf, pieceHalf, pieceHalf, pieceHalf, u0, v0, u0 + 0.5F, v0 + 0.5F,
                        r, g, b, alpha, light);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
    }

    private static void renderCrack(PoseStack poseStack, MultiBufferSource buffers, Crack crack, float partialTick) {
        if (crack.points.size() < 2) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer base = buffers.getBuffer(FxRenderTypes.soft(CRACK));
        crackStrip(base, pose, crack, partialTick, 0.75F, 0.03F, 1.0F, 1.0F, 1.0F, 0.95F, false);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(CRACK_GLOW));
        crackStrip(glow, pose, crack, partialTick, 1.4F, 0.035F, GREEN_R, GREEN_G, GREEN_B, 0.35F, true);
        crackStrip(glow, pose, crack, partialTick, 0.5F, 0.04F, VENOM_R, VENOM_G, VENOM_B, 1.0F, true);
        crackStrip(glow, pose, crack, partialTick, 0.2F, 0.045F, 1.0F, 1.0F, 0.85F, 0.7F, true);
        if (!crack.open) {
            return;
        }
        CrackPoint head = crack.points.get(crack.points.size() - 1);
        float age = head.age + partialTick;
        float pulse = 0.8F + 0.2F * Mth.sin(age * 1.4F);
        VertexConsumer light = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(light, pose, head.x, head.y + 0.05D, head.z, 2.4F * pulse, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.7F);
        FxDraw.sprite(light, pose, head.x, head.y + 0.5D, head.z, 2.2F * pulse, 0.0F, GREEN_R, GREEN_G, GREEN_B, 0.6F);
        VertexConsumer shaft = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        Vec3 bottom = new Vec3(head.x, head.y, head.z);
        FxDraw.beam(shaft, pose, bottom, bottom.add(0.0D, 2.2D, 0.0D), 0.45F, 0.15F, 0.0F, 1.0F, VENOM_R, VENOM_G, VENOM_B, 0.75F, 0.0F);
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        FxDraw.sprite(flare, pose, head.x, head.y + 0.3D, head.z, 1.6F * pulse, age * 0.15F, 1.0F, 1.0F, 0.8F, 0.9F);
    }

    private static void crackStrip(VertexConsumer consumer, PoseStack.Pose pose, Crack crack, float partialTick, float width, float lift,
                                   float r, float g, float b, float alphaScale, boolean pulse) {
        float distance = 0.0F;
        for (int i = 0; i < crack.points.size() - 1; i++) {
            CrackPoint p0 = crack.points.get(i);
            CrackPoint p1 = crack.points.get(i + 1);
            float length = (float) Math.sqrt((p1.x - p0.x) * (p1.x - p0.x) + (p1.z - p0.z) * (p1.z - p0.z));
            float alpha0 = crackAlpha(p0, partialTick) * alphaScale;
            float alpha1 = crackAlpha(p1, partialTick) * alphaScale;
            if (pulse) {
                alpha0 *= 0.65F + 0.35F * Mth.sin((p0.age + partialTick) * 0.5F + i * 0.7F);
                alpha1 *= 0.65F + 0.35F * Mth.sin((p1.age + partialTick) * 0.5F + (i + 1) * 0.7F);
            }
            float width0 = width * crackOpen(p0, partialTick);
            float width1 = width * crackOpen(p1, partialTick);
            Vec3 side0 = NagaBossController.directionOf(p0.yaw + 90.0F);
            Vec3 side1 = NagaBossController.directionOf(p1.yaw + 90.0F);
            double y0 = p0.y + lift;
            double y1 = p1.y + lift;
            float v0 = distance / 1.6F;
            float v1 = (distance + length) / 1.6F;
            FxDraw.glowVertex(consumer, pose, p0.x - side0.x * width0, y0, p0.z - side0.z * width0, 0.0F, v0, r, g, b, alpha0);
            FxDraw.glowVertex(consumer, pose, p1.x - side1.x * width1, y1, p1.z - side1.z * width1, 0.0F, v1, r, g, b, alpha1);
            FxDraw.glowVertex(consumer, pose, p1.x + side1.x * width1, y1, p1.z + side1.z * width1, 1.0F, v1, r, g, b, alpha1);
            FxDraw.glowVertex(consumer, pose, p0.x + side0.x * width0, y0, p0.z + side0.z * width0, 1.0F, v0, r, g, b, alpha0);
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
        PoseStack.Pose world = poseStack.last();
        float t = Mth.clamp(age / 12.0F, 0.0F, 1.0F);
        float ease = 1.0F - (1.0F - t) * (1.0F - t);
        VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
        FxDraw.flat(shock, world, bite.x, bite.y + 0.08D, bite.z, 0.8F + 5.0F * ease, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.95F * (1.0F - t));
        if (age > 5.0F) {
            float t2 = Mth.clamp((age - 5.0F) / 10.0F, 0.0F, 1.0F);
            FxDraw.flat(shock, world, bite.x, bite.y + 0.1D, bite.z, 0.8F + 3.5F * t2, 0.0F, 1.0F, 1.0F, 0.85F, 0.8F * (1.0F - t2));
        }
        VertexConsumer light = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        float lightAlpha = Mth.clamp(1.0F - age / BITE_LIFE, 0.0F, 1.0F);
        FxDraw.flat(light, world, bite.x, bite.y + 0.05D, bite.z, 4.5F, 0.0F, GREEN_R, GREEN_G, GREEN_B, 0.8F * lightAlpha);
        if (age > 6.0F && age < 11.0F) {
            float snap = 1.0F - Math.abs(age - 8.0F) / 3.0F;
            FxDraw.sprite(light, world, bite.x, bite.y + 1.6D, bite.z, 4.0F, 0.0F, 1.0F, 1.0F, 0.85F, snap);
            VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
            FxDraw.sprite(flare, world, bite.x, bite.y + 1.6D, bite.z, 6.0F, age * 0.05F, VENOM_R, VENOM_G, VENOM_B, snap);
        }
        VertexConsumer cracks = buffers.getBuffer(FxRenderTypes.glow(CRACK_GLOW));
        for (int i = 0; i < 7; i++) {
            float a = bite.yaw * Mth.DEG_TO_RAD + i * Mth.TWO_PI / 7.0F + 0.4F;
            float len = (1.8F + (i % 3) * 0.7F) * Math.min(1.0F, age / 4.0F);
            Vec3 dir = new Vec3(Mth.cos(a), 0.0D, Mth.sin(a));
            Vec3 side = new Vec3(-dir.z, 0.0D, dir.x).scale(0.18D);
            Vec3 start = new Vec3(bite.x, bite.y + 0.06D, bite.z).add(dir.scale(0.6D));
            Vec3 end = start.add(dir.scale(len));
            FxDraw.quad(cracks, world, start.subtract(side), end.subtract(side.scale(0.3D)), end.add(side.scale(0.3D)), start.add(side), 0.0F, 0.0F, 1.0F, len / 1.6F,
                VENOM_R, VENOM_G, VENOM_B, lightAlpha);
        }

        int lightLevel = LevelRenderer.getLightColor(level, BlockPos.containing(bite.x, bite.y + 1.0D, bite.z));
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
            FxDraw.box(consumer, pose, -1.3F, 0.0F, -0.28F, 1.3F, 0.75F, 0.28F, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, lightLevel);
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
                FxDraw.box(consumer, pose, fx - 0.12F, 0.75F, z0, fx + 0.12F, 0.75F + height, z1, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, lightLevel);
                FxDraw.box(consumer, pose, fx - 0.06F, 0.75F + height, z0 + 0.05F, fx + 0.06F, 0.75F + height + 0.22F, z1 - 0.05F, 0.5F, 0.0F, 1.0F, 0.3F,
                    1.0F, 1.0F, 1.0F, alpha, lightLevel);
            }
            poseStack.popPose();
        }
        poseStack.popPose();

        if (age < 14.0F) {
            VertexConsumer fangGlow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            PoseStack local = new PoseStack();
            local.translate(bite.x, bite.y, bite.z);
            local.mulPose(Axis.YP.rotationDegrees(-bite.yaw));
            float glowAlpha = 0.5F + 0.5F * Mth.sin(age * 0.9F);
            for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
                float side = 1.0F;
                if (sideIndex == 1) {
                    side = -1.0F;
                }
                local.pushPose();
                local.translate(0.0F, rise, side * 0.9F);
                local.mulPose(Axis.XP.rotationDegrees(side * angle));
                for (int fang = 0; fang < 5; fang++) {
                    float fx = -1.0F + fang * 0.5F;
                    float tipY = 1.8F;
                    if (fang % 2 == 1) {
                        tipY = 1.55F;
                    }
                    if (fang == 2) {
                        tipY = 2.0F;
                    }
                    Vector4f tip = new Vector4f(fx, tipY, -side * 0.2F, 1.0F);
                    local.last().pose().transform(tip);
                    FxDraw.sprite(fangGlow, world, tip.x(), tip.y(), tip.z(), 0.7F, 0.0F, VENOM_R, VENOM_G, VENOM_B, glowAlpha * 0.8F);
                }
                local.popPose();
            }
        }
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

    private static Vec3 weakScalePos(ClientLevel level, Constrict constrict, float partialTick) {
        if (constrict.weakIndex < 0) {
            return null;
        }
        Entity entity = level.getEntity(constrict.nagaId);
        if (!(entity instanceof Naga naga)) {
            return null;
        }
        PartEntity<?>[] parts = naga.getParts();
        if (parts == null || constrict.weakIndex >= parts.length) {
            return null;
        }
        PartEntity<?> part = parts[constrict.weakIndex];
        if (part.getBbWidth() <= 0.0F) {
            return null;
        }
        return new Vec3(Mth.lerp(partialTick, part.xo, part.getX()), Mth.lerp(partialTick, part.yo, part.getY()) + part.getBbHeight() * 0.5D,
            Mth.lerp(partialTick, part.zo, part.getZ()));
    }

    private static void renderConstrict(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Constrict constrict, float partialTick) {
        float age = constrict.age + partialTick;
        float radius = constrict.currentRadius(age);
        float rise = Mth.clamp(age / constrict.warmup, 0.0F, 1.0F);
        float height = 2.8F * (1.0F - (1.0F - rise) * (1.0F - rise));
        float fade = 1.0F;
        float burst = 0.0F;
        if (constrict.endAge >= 0) {
            fade = Mth.clamp(1.0F - (age - constrict.endAge) / CONSTRICT_FADE, 0.0F, 1.0F);
            burst = 1.0F - fade;
            if (constrict.broken) {
                height += (age - constrict.endAge) * 0.45F;
            }
        }
        if (fade <= 0.0F) {
            return;
        }
        boolean active = age >= constrict.warmup;
        float r = GREEN_R;
        float g = GREEN_G;
        float b = GREEN_B;
        float baseAlpha = 0.4F;
        if (active) {
            r = VENOM_R;
            g = VENOM_G;
            b = VENOM_B;
            baseAlpha = 0.85F;
        }
        if (constrict.broken && constrict.endAge >= 0) {
            r = GOLD_R;
            g = GOLD_G;
            b = GOLD_B;
        }
        baseAlpha *= fade;
        PoseStack.Pose pose = poseStack.last();
        double x = constrict.x;
        double y = constrict.y;
        double z = constrict.z;
        float circumference = Mth.TWO_PI * radius;
        float uRepeat = Math.max(4.0F, Math.round(circumference / 1.5F));
        int segments = 64;

        VertexConsumer fill = buffers.getBuffer(FxRenderTypes.glow(RING_FILL));
        FxDraw.flat(fill, pose, x, y + 0.03D, z, radius, age * 0.01F, r, g, b, 0.22F * fade);

        VertexConsumer wall = buffers.getBuffer(FxRenderTypes.glow(WALL));
        FxDraw.wall(wall, pose, x, y, z, radius, height, segments, uRepeat, -age * 0.035F, r, g, b, baseAlpha, 0.0F);
        FxDraw.wall(wall, pose, x, y, z, radius + 0.08F, height * 0.6F, segments, uRepeat, age * 0.02F, DARK_R * 3.0F, DARK_G * 3.0F, DARK_B * 3.0F, baseAlpha * 0.5F, 0.0F);

        VertexConsumer streaks = buffers.getBuffer(FxRenderTypes.glow(FxTextures.STREAKS));
        FxDraw.wall(streaks, pose, x, y, z, radius - 0.05F, height * 1.15F, segments, uRepeat * 0.5F, -age * 0.06F, r, g, b, baseAlpha * 0.9F, 0.0F);

        VertexConsumer line = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.ring(line, pose, x, y + 0.04D, z, radius - 0.3F, radius + 0.3F, segments, 0.0F, 1.0F, r, g, b, baseAlpha * 1.2F, baseAlpha * 1.2F);
        FxDraw.ring(line, pose, x, y + 0.045D, z, radius - 0.1F, radius + 0.1F, segments, 0.0F, 1.0F, 1.0F, 1.0F, 0.9F, baseAlpha, baseAlpha);

        if (active && age < constrict.warmup + 8.0F) {
            float flash = 1.0F - (age - constrict.warmup) / 8.0F;
            FxDraw.wall(streaks, pose, x, y, z, radius, height * 1.4F, segments, uRepeat * 0.5F, 0.0F, 1.0F, 1.0F, 0.9F, flash * fade, 0.0F);
        }
        if (burst > 0.0F && constrict.broken) {
            VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
            FxDraw.flat(shock, pose, x, y + 0.1D, z, radius * (1.0F + burst * 1.2F), 0.0F, GOLD_R, GOLD_G, GOLD_B, fade);
        }

        if (constrict.endAge >= 0) {
            return;
        }
        Vec3 weak = weakScalePos(level, constrict, partialTick);
        if (weak == null) {
            return;
        }
        Entity entity = level.getEntity(constrict.nagaId);
        if (!(entity instanceof Naga naga)) {
            return;
        }
        PartEntity<?> part = naga.getParts()[constrict.weakIndex];
        float yaw = Mth.rotLerp(partialTick, part.yRotO, part.getYRot());
        float half = part.getBbWidth() * 0.5F + 0.1F;
        float pulse = 0.55F + 0.35F * Mth.sin(age * 0.35F);
        float grow = 1.0F;
        if (constrict.flash > 0) {
            pulse = 1.0F;
            grow = 1.0F + constrict.flash * 0.04F;
        }
        float edge = half * grow;
        VertexConsumer shell = buffers.getBuffer(FxRenderTypes.glow(WEAK_SCALE));
        poseStack.pushPose();
        poseStack.translate(weak.x, weak.y, weak.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        FxDraw.box(shell, poseStack.last(), -edge, -edge, -edge, edge, edge, edge, 0.0F, 0.0F, 1.0F, 1.0F, GOLD_R, GOLD_G, GOLD_B, pulse, FxDraw.FULL_BRIGHT);
        poseStack.popPose();
        VertexConsumer halo = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(halo, pose, weak.x, weak.y, weak.z, half * 6.0F * grow, 0.0F, GOLD_R, GOLD_G, GOLD_B, pulse * 0.6F);
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        FxDraw.sprite(flare, pose, weak.x, weak.y + half + 0.4D, weak.z, (1.4F + 0.4F * Mth.sin(age * 0.5F)) * grow, age * 0.04F, 1.0F, 0.95F, 0.75F, pulse);
        if (constrict.flash > 0) {
            FxDraw.sprite(flare, pose, weak.x, weak.y, weak.z, half * 9.0F * constrict.flash / 8.0F, 0.0F, 1.0F, 1.0F, 1.0F, constrict.flash / 8.0F);
        }
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

        float currentRadius(float age) {
            float progress = Mth.clamp(age / Math.max(1, this.duration), 0.0F, 1.0F);
            return this.startRadius + (this.endRadius - this.startRadius) * progress;
        }
    }
}
