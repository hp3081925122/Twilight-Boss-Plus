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
    private static final ResourceLocation BURST = tex("burst");
    private static final ResourceLocation MIRROR = tex("mirror");
    private static final ResourceLocation CHAIN = tex("chain");
    private static final ResourceLocation HALO = tex("halo");
    private static final ResourceLocation HALO_BROKEN = tex("halo_broken");
    private static final ResourceLocation SLASH = tex("slash");
    private static final ResourceLocation CROWN = tex("crown");
    private static final ResourceLocation SHARD = tex("px/shard");
    private static final ResourceLocation PHANTOM = tex("phantom");
    public static final ResourceLocation[] RUNES = {tex("px/rune0"), tex("px/rune1"), tex("px/rune2"), tex("px/rune3")};

    private static final int GHOST_LIFE = 16;
    private static final int CHAIN_FADE = 10;
    private static final int BURST_LIFE = 14;
    private static final int SLASH_SWING = 4;
    private static final int SLASH_FADE = 8;
    private static final int BEAM_LIFE = 8;
    private static final int STAGGER_FADE = 8;
    private static final int CROWN_FADE = 6;

    private static final float VIOLET_R = 0.48F;
    private static final float VIOLET_G = 0.31F;
    private static final float VIOLET_B = 0.82F;
    private static final float LILAC_R = 0.78F;
    private static final float LILAC_G = 0.66F;
    private static final float LILAC_B = 1.0F;
    private static final float SOUL_R = 0.37F;
    private static final float SOUL_G = 0.99F;
    private static final float SOUL_B = 0.89F;
    private static final float GOLD_R = 0.91F;
    private static final float GOLD_G = 0.78F;
    private static final float GOLD_B = 0.40F;

    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final List<Sigil> SIGILS = new ArrayList<>();
    private static final List<Chain> CHAINS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final List<Slash> SLASHES = new ArrayList<>();
    private static final List<Crown> CROWNS = new ArrayList<>();
    private static final List<Stagger> STAGGERS = new ArrayList<>();
    private static final List<Beam> BEAMS = new ArrayList<>();

    private LichFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/lich/" + name + ".png");
    }

    private static ResourceLocation rune(RandomSource random) {
        return RUNES[random.nextInt(RUNES.length)];
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        if (type == LichFxPacket.TELEPORT && data.length >= 7) {
            GHOSTS.add(new Ghost(data[0], data[1], data[2], data[3], data[4], data[5], data[6]));
            BURSTS.add(new Burst(data[3], data[4] + 0.05F, data[5], 2.4F, 3, false));
            for (int i = 0; i < 22; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = 0.2D + random.nextDouble() * 0.4D;
                FxParticles.glow(FxTextures.MOTE, data[0] + Math.cos(angle) * dist, data[1] + random.nextDouble() * 2.0D, data[2] + Math.sin(angle) * dist,
                        -Math.sin(angle) * 0.05D, 0.04D + random.nextDouble() * 0.06D, Math.cos(angle) * 0.05D, 18 + random.nextInt(12), 0.2F + random.nextFloat() * 0.15F)
                    .color(LILAC_R, LILAC_G, LILAC_B).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.93D).flicker(0.3F, random.nextFloat() * 10.0F);
            }
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.08D + random.nextDouble() * 0.08D;
                FxParticles.glow(FxTextures.MOTE, data[3], data[4] + 0.2D + random.nextDouble() * 1.6D, data[5], Math.cos(angle) * speed, 0.02D, Math.sin(angle) * speed,
                        14 + random.nextInt(8), 0.22F)
                    .color(1.0F, 0.95F, 1.0F).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.88D);
            }
            for (int i = 0; i < 3; i++) {
                FxParticles.glow(rune(random), data[0] + (random.nextDouble() - 0.5D) * 0.8D, data[1] + 0.6D + random.nextDouble() * 1.2D, data[2] + (random.nextDouble() - 0.5D) * 0.8D,
                        0.0D, 0.05D, 0.0D, 18 + random.nextInt(6), 0.35F)
                    .color(LILAC_R, LILAC_G, LILAC_B).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.92D).fade(0.15F, 2.0F);
            }
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
                    if (chain.broken) {
                        Vec3[] ends = chainEnds(level, chain, 1.0F);
                        if (ends != null) {
                            FxShake.at(ends[1].x, ends[1].y, ends[1].z, 0.6F, 10.0F, 8);
                            for (int i = 0; i < 24; i++) {
                                Vec3 point = ends[0].lerp(ends[1], random.nextDouble());
                                Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.3D, random.nextDouble() - 0.5D).normalize().scale(0.1D + random.nextDouble() * 0.15D);
                                FxParticles.glow(FxTextures.SPARK, point.x, point.y, point.z, dir.x, dir.y, dir.z, 8 + random.nextInt(8), 0.12F)
                                    .color(1.0F, 0.85F, 0.85F).fadeTo(1.0F, 0.35F, 0.35F).drag(0.86D).gravity(0.01D).streak(2.0F);
                            }
                        }
                    }
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
                    FxParticles.glow(FxTextures.FLARE, sx, sy, sz, 0.0D, 0.0D, 0.0D, 7, 2.6F)
                        .color(1.0F, 0.95F, 0.75F).fadeTo(GOLD_R, GOLD_G, GOLD_B).grow(0.6F).fade(0.0F, 1.5F).spin(random.nextFloat() * 6.0F, 0.1F);
                    for (int i = 0; i < 7; i++) {
                        FxParticles.glow(SHARD, sx, sy, sz, Mth.cos(angle) * 0.12D + (random.nextDouble() - 0.5D) * 0.14D, 0.1D + random.nextDouble() * 0.14D,
                                Mth.sin(angle) * 0.12D + (random.nextDouble() - 0.5D) * 0.14D, 20 + random.nextInt(10), 0.18F + random.nextFloat() * 0.12F)
                            .color(1.0F, 0.9F, 0.6F).fadeTo(GOLD_R, GOLD_G * 0.8F, GOLD_B * 0.5F).gravity(0.02D).drag(0.95D)
                            .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.8F).flicker(0.25F, random.nextFloat() * 10.0F);
                    }
                    for (int i = 0; i < 8; i++) {
                        Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.2D, random.nextDouble() - 0.5D).normalize().scale(0.2D + random.nextDouble() * 0.15D);
                        FxParticles.glow(FxTextures.SPARK, sx, sy, sz, dir.x, dir.y, dir.z, 6 + random.nextInt(5), 0.1F)
                            .color(1.0F, 0.97F, 0.85F).fadeTo(GOLD_R, GOLD_G, GOLD_B).drag(0.82D).streak(2.5F);
                    }
                }
            }
        } else if (type == LichFxPacket.BOMB_BURST && data.length >= 4) {
            BURSTS.add(new Burst(data[0], data[1], data[2], data[3], 0, true));
            FxShake.at(data[0], data[1], data[2], 0.8F, data[3] * 4.0F, 10);
            for (int i = 0; i < 30; i++) {
                Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).normalize();
                double speed = 0.12D + random.nextDouble() * 0.18D;
                FxParticles.glow(FxTextures.MOTE, data[0], data[1], data[2], dir.x * speed, dir.y * speed, dir.z * speed, 14 + random.nextInt(10), 0.3F)
                    .color(LILAC_R, LILAC_G, LILAC_B).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.88D).streak(1.2F);
            }
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2.0D * i / 6.0D;
                FxParticles.glow(rune(random), data[0] + Math.cos(angle) * data[3] * 0.4D, data[1] + 0.3D, data[2] + Math.sin(angle) * data[3] * 0.4D,
                        Math.cos(angle) * 0.06D, 0.06D, Math.sin(angle) * 0.06D, 18 + random.nextInt(6), 0.45F)
                    .color(LILAC_R, LILAC_G, LILAC_B).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.9D);
            }
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
            FxShake.at(data[0], data[1], data[2], 1.3F, data[3] * 3.0F, 14);
            for (int i = 0; i < 40; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.25D + random.nextDouble() * 0.25D;
                FxParticles.glow(FxTextures.SPARK, data[0], data[1] + 0.3D + random.nextDouble() * 1.5D, data[2], Math.cos(angle) * speed, 0.02D + random.nextDouble() * 0.1D,
                        Math.sin(angle) * speed, 10 + random.nextInt(8), 0.16F)
                    .color(1.0F, 0.96F, 0.8F).fadeTo(GOLD_R, GOLD_G, GOLD_B).drag(0.88D).gravity(0.008D).streak(2.4F);
            }
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * 2.0D * i / 10.0D;
                FxParticles.glow(rune(random), data[0] + Math.cos(angle) * 1.2D, data[1] + 1.0D, data[2] + Math.sin(angle) * 1.2D,
                        Math.cos(angle) * 0.15D, 0.08D, Math.sin(angle) * 0.15D, 20 + random.nextInt(6), 0.55F)
                    .color(1.0F, 0.92F, 0.65F).fadeTo(GOLD_R, GOLD_G, GOLD_B).drag(0.9D);
            }
        } else if (type == LichFxPacket.STAGGER && data.length >= 1) {
            STAGGERS.removeIf(stagger -> stagger.lichId == entityId);
            STAGGERS.add(new Stagger(entityId, Math.max(1, (int) data[0])));
        } else if (type == LichFxPacket.CANDLE_SNUFF && data.length >= 3) {
            FxParticles.glow(FxTextures.GLOW, data[0], data[1] + 0.1D, data[2], 0.0D, 0.0D, 0.0D, 8, 1.4F)
                .color(SOUL_R, SOUL_G, SOUL_B).grow(0.3F).fade(0.0F, 1.5F);
            for (int i = 0; i < 4; i++) {
                FxParticles.soft(FxTextures.SMOKE, data[0] + (random.nextDouble() - 0.5D) * 0.2D, data[1] + 0.1D + i * 0.08D, data[2] + (random.nextDouble() - 0.5D) * 0.2D,
                        (random.nextDouble() - 0.5D) * 0.01D, 0.03D + random.nextDouble() * 0.02D, (random.nextDouble() - 0.5D) * 0.01D, 30 + random.nextInt(14), 0.25F)
                    .color(0.72F, 0.82F, 0.86F).alpha(0.55F).grow(1.1F).drag(0.97D).fade(0.2F, 1.4F).spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.08F);
            }
            for (int i = 0; i < 5; i++) {
                FxParticles.glow(FxTextures.MOTE, data[0], data[1] + 0.15D, data[2], (random.nextDouble() - 0.5D) * 0.05D, 0.04D + random.nextDouble() * 0.05D,
                        (random.nextDouble() - 0.5D) * 0.05D, 16 + random.nextInt(10), 0.14F)
                    .color(SOUL_R, SOUL_G, SOUL_B).drag(0.95D).flicker(0.5F, random.nextFloat() * 10.0F);
            }
        } else if (type == LichFxPacket.BEAM && data.length >= 9) {
            BEAMS.add(new Beam(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8]));
            FxParticles.glow(FxTextures.FLARE, data[3], data[4], data[5], 0.0D, 0.0D, 0.0D, 6, 1.6F).color(data[6], data[7], data[8]).fade(0.0F, 1.5F);
            for (int i = 0; i < 6; i++) {
                Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).normalize().scale(0.12D);
                FxParticles.glow(FxTextures.MOTE, data[3], data[4], data[5], dir.x, dir.y, dir.z, 10 + random.nextInt(6), 0.18F).color(data[6], data[7], data[8]).drag(0.88D);
            }
        } else if (type == LichFxPacket.ORB_POP && data.length >= 3) {
            BURSTS.add(new Burst(data[0], data[1], data[2], 1.6F, 2, false));
            for (int i = 0; i < 16; i++) {
                Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.3D, random.nextDouble() - 0.5D).normalize().scale(0.1D + random.nextDouble() * 0.12D);
                FxParticles.glow(FxTextures.MOTE, data[0], data[1], data[2], dir.x, dir.y, dir.z, 12 + random.nextInt(8), 0.25F)
                    .color(LILAC_R, LILAC_G, LILAC_B).fadeTo(VIOLET_R * 0.7F, VIOLET_G * 0.5F, VIOLET_B).drag(0.9D).gravity(0.004D);
            }
        }
    }

    private static Vec3 lerpPos(Entity entity, float partialTick) {
        return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static Vec3[] chainEnds(ClientLevel level, Chain chain, float partialTick) {
        Entity lich = level.getEntity(chain.lichId);
        Entity target = level.getEntity(chain.targetId);
        if (lich == null || target == null) {
            return null;
        }
        Vec3 lichPos = lerpPos(lich, partialTick);
        float bodyYaw = lich.getYRot();
        if (lich instanceof Lich living) {
            bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        }
        Vec3 right = NagaBossController.directionOf(bodyYaw + 90.0F);
        Vec3 forward = NagaBossController.directionOf(bodyYaw);
        Vec3 from = lichPos.add(right.scale(-0.35D)).add(forward.scale(0.35D)).add(0.0D, lich.getBbHeight() * 0.62D, 0.0D);
        Vec3 to = lerpPos(target, partialTick).add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        return new Vec3[] {from, to};
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
        GHOSTS.removeIf(ghost -> ++ghost.age > GHOST_LIFE);
        Iterator<Sigil> sigils = SIGILS.iterator();
        while (sigils.hasNext()) {
            Sigil sigil = sigils.next();
            sigil.age++;
            if (sigil.age % 3 == 0) {
                Vec3 forward = NagaBossController.directionOf(sigil.yaw);
                Vec3 right = NagaBossController.directionOf(sigil.yaw + 90.0F);
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 1.0D + random.nextDouble() * 0.4D;
                double px = sigil.x + right.x * Math.cos(angle) * radius;
                double py = sigil.y + Math.sin(angle) * radius;
                double pz = sigil.z + right.z * Math.cos(angle) * radius;
                float r = LILAC_R;
                float g = LILAC_G;
                float b = LILAC_B;
                if (sigil.kind == 1) {
                    r = 1.0F;
                    g = 0.9F;
                    b = 0.6F;
                }
                FxParticles.glow(FxTextures.MOTE, px, py, pz, forward.x * 0.04D, 0.01D, forward.z * 0.04D, 12 + random.nextInt(6), 0.18F).color(r, g, b).drag(0.95D);
            }
            if (sigil.age == sigil.duration) {
                Vec3 forward = NagaBossController.directionOf(sigil.yaw);
                for (int i = 0; i < 12; i++) {
                    Vec3 dir = forward.scale(0.12D).add((random.nextDouble() - 0.5D) * 0.2D, (random.nextDouble() - 0.5D) * 0.2D, (random.nextDouble() - 0.5D) * 0.2D);
                    FxParticles.glow(FxTextures.SPARK, sigil.x, sigil.y, sigil.z, dir.x, dir.y, dir.z, 8 + random.nextInt(4), 0.12F)
                        .color(1.0F, 1.0F, 1.0F).fadeTo(VIOLET_R, VIOLET_G, VIOLET_B).drag(0.85D).streak(2.0F);
                }
            }
        }
        SIGILS.removeIf(sigil -> sigil.age > sigil.duration);
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
            } else if (chain.endAge < 0 && chain.age > chain.windup && chain.age % 2 == 0) {
                Vec3[] ends = chainEnds(level, chain, 1.0F);
                if (ends != null) {
                    Vec3 source = ends[1];
                    Vec3 sink = ends[0];
                    float r = SOUL_R;
                    float g = SOUL_G;
                    float b = SOUL_B;
                    if (chain.kind == 1) {
                        source = ends[0];
                        sink = ends[1];
                        r = LILAC_R;
                        g = LILAC_G;
                        b = LILAC_B;
                    }
                    Vec3 vel = sink.subtract(source).scale(0.06D);
                    FxParticles.glow(FxTextures.MOTE, source.x + (random.nextDouble() - 0.5D) * 0.5D, source.y + (random.nextDouble() - 0.5D) * 0.6D,
                            source.z + (random.nextDouble() - 0.5D) * 0.5D, vel.x, vel.y, vel.z, 16, 0.2F)
                        .color(r, g, b).drag(1.0D).fade(0.2F, 2.0F);
                }
            }
        }
        BURSTS.removeIf(burst -> ++burst.age > BURST_LIFE);
        Iterator<Slash> slashes = SLASHES.iterator();
        while (slashes.hasNext()) {
            Slash slash = slashes.next();
            slash.age++;
            if (slash.age <= SLASH_SWING) {
                float p = (float) slash.age / SLASH_SWING;
                Vec3 dir = NagaBossController.directionOf(slash.yaw + 45.0F - 90.0F * p);
                Vec3 tangent = NagaBossController.directionOf(slash.yaw - 45.0F - 90.0F * p);
                for (int i = 0; i < 5; i++) {
                    double dist = slash.reach * (0.6D + random.nextDouble() * 0.45D);
                    double speed = 0.15D + random.nextDouble() * 0.15D;
                    FxParticles.glow(FxTextures.SPARK, slash.x + dir.x * dist, slash.y + 1.7D - 1.2D * p, slash.z + dir.z * dist,
                            tangent.x * speed, -0.05D, tangent.z * speed, 7 + random.nextInt(5), 0.1F)
                        .color(1.0F, 0.96F, 0.8F).fadeTo(GOLD_R, GOLD_G, GOLD_B).drag(0.85D).gravity(0.01D).streak(2.0F);
                }
            }
            if (slash.age > SLASH_SWING + SLASH_FADE) {
                slashes.remove();
            }
        }
        Iterator<Crown> crowns = CROWNS.iterator();
        while (crowns.hasNext()) {
            Crown crown = crowns.next();
            crown.age++;
            Entity lich = level.getEntity(crown.lichId);
            if (crown.endAge >= 0 && crown.age - crown.endAge > CROWN_FADE) {
                crowns.remove();
            } else if (crown.age > crown.duration + 40) {
                crowns.remove();
            } else if (lich == null) {
                crowns.remove();
            } else if (crown.endAge < 0) {
                for (int i = 0; i < 2; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double px = lich.getX() + Math.cos(angle) * crown.pull;
                    double pz = lich.getZ() + Math.sin(angle) * crown.pull;
                    double py = crown.baseY + 0.2D + random.nextDouble() * 0.6D;
                    double ty = lich.getY() + lich.getBbHeight() * 0.55D;
                    FxParticles.glow(FxTextures.MOTE, px, py, pz, (lich.getX() - px) * 0.05D, (ty - py) * 0.05D, (lich.getZ() - pz) * 0.05D, 20, 0.22F)
                        .color(LILAC_R, LILAC_G, LILAC_B).fadeTo(GOLD_R, GOLD_G, GOLD_B).drag(1.0D).streak(1.5F);
                }
            }
        }
        Iterator<Stagger> staggers = STAGGERS.iterator();
        while (staggers.hasNext()) {
            Stagger stagger = staggers.next();
            stagger.age++;
            Entity lich = level.getEntity(stagger.lichId);
            if (stagger.age > stagger.duration + STAGGER_FADE || lich == null) {
                staggers.remove();
            } else if (stagger.age < stagger.duration && random.nextInt(3) == 0) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                FxParticles.glow(FxTextures.SPARK, lich.getX() + Math.cos(angle) * 0.8D, lich.getY() + lich.getBbHeight() * 0.45D, lich.getZ() + Math.sin(angle) * 0.8D,
                        0.0D, -0.06D, 0.0D, 10 + random.nextInt(6), 0.1F)
                    .color(1.0F, 0.92F, 0.6F).fadeTo(GOLD_R, GOLD_G * 0.6F, GOLD_B * 0.3F).gravity(0.01D).streak(2.0F);
            }
        }
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
        BURSTS.clear();
        SLASHES.clear();
        CROWNS.clear();
        STAGGERS.clear();
        BEAMS.clear();
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
        if (GHOSTS.isEmpty() && SIGILS.isEmpty() && CHAINS.isEmpty() && BURSTS.isEmpty() && SLASHES.isEmpty() && CROWNS.isEmpty() && STAGGERS.isEmpty()
            && BEAMS.isEmpty()) {
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
            if (burst.kind < 2 || decorative) {
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
            for (Beam beam : BEAMS) {
                renderBeam(poseStack, buffers, beam, partialTick);
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void panel(VertexConsumer consumer, PoseStack.Pose pose, float halfWidth, float halfHeight, float z, float r, float g, float b, float alpha) {
        FxDraw.glowVertex(consumer, pose, -halfWidth, -halfHeight, z, 1.0F, 1.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, halfWidth, -halfHeight, z, 0.0F, 1.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, halfWidth, halfHeight, z, 0.0F, 0.0F, r, g, b, alpha);
        FxDraw.glowVertex(consumer, pose, -halfWidth, halfHeight, z, 1.0F, 0.0F, r, g, b, alpha);
    }

    private static void upright(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float width, float height,
                                float r, float g, float b, float alpha) {
        Vec3 camera = FxDraw.cameraPos();
        double dx = camera.x - x;
        double dz = camera.z - z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4D) {
            return;
        }
        Vec3 right = new Vec3(-dz / len * width * 0.5D, 0.0D, dx / len * width * 0.5D);
        Vec3 top = new Vec3(x, y + height, z);
        Vec3 bottom = new Vec3(x, y, z);
        FxDraw.quad(consumer, pose, top.subtract(right), bottom.subtract(right), bottom.add(right), top.add(right), 0.0F, 0.0F, 1.0F, 1.0F, r, g, b, alpha);
    }

    private static void renderGhost(PoseStack poseStack, MultiBufferSource buffers, Ghost ghost, float partialTick) {
        float age = ghost.age + partialTick;
        float t = Mth.clamp(age / GHOST_LIFE, 0.0F, 1.0F);
        PoseStack.Pose pose = poseStack.last();
        Vec3 from = new Vec3(ghost.x, ghost.y + 1.1D, ghost.z);
        Vec3 to = new Vec3(ghost.tx, ghost.ty + 1.1D, ghost.tz);
        if (t < 0.5F) {
            float k = 1.0F - t * 2.0F;
            float length = (float) from.distanceTo(to);
            VertexConsumer energy = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
            FxDraw.beam(energy, pose, from, to, 0.5F * k, 0.25F * k, -age * 0.2F, -age * 0.2F + length / 4.0F, VIOLET_R, VIOLET_G, VIOLET_B, 0.5F * k, 0.9F * k);
            VertexConsumer core = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.beam(core, pose, from, to, 0.12F * k, 0.08F * k, 0.0F, 1.0F, 1.0F, 0.9F, 1.0F, 0.6F * k, k);
        }
        float alpha = (1.0F - t) * (1.0F - t);
        float width = 1.25F * (1.0F - t * 0.45F);
        float height = 2.3F * (1.0F + t * 0.7F);
        double baseY = ghost.y + t * 0.7D;
        VertexConsumer phantom = buffers.getBuffer(FxRenderTypes.glow(PHANTOM));
        upright(phantom, pose, ghost.x, baseY - 0.05D, ghost.z, width * 1.15F, height * 1.06F, VIOLET_R, VIOLET_G, VIOLET_B, alpha * 0.9F);
        upright(phantom, pose, ghost.x, baseY, ghost.z, width, height, LILAC_R, LILAC_G, LILAC_B, alpha);
        VertexConsumer light = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(light, pose, ghost.x, ghost.y + 0.05D, ghost.z, 2.0F, 0.0F, VIOLET_R, VIOLET_G, VIOLET_B, 0.7F * (1.0F - t));
        FxDraw.flat(light, pose, ghost.tx, ghost.ty + 0.05D, ghost.tz, 2.2F, 0.0F, LILAC_R, LILAC_G, LILAC_B, 0.8F * (1.0F - t));
        VertexConsumer shaft = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        Vec3 foot = new Vec3(ghost.tx, ghost.ty, ghost.tz);
        FxDraw.beam(shaft, pose, foot, foot.add(0.0D, 3.2D, 0.0D), 0.7F * (1.0F - t), 0.2F * (1.0F - t), 0.0F, 1.0F, LILAC_R, LILAC_G, LILAC_B, 0.8F * (1.0F - t), 0.0F);
    }

    private static void renderSigil(PoseStack poseStack, MultiBufferSource buffers, Sigil sigil, float partialTick) {
        float age = sigil.age + partialTick;
        float t = Mth.clamp(age / sigil.duration, 0.0F, 1.0F);
        float appear = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
        float pop = appear * (1.0F + 0.25F * Mth.sin(appear * Mth.PI));
        float fade = 1.0F;
        if (sigil.duration - age < 3.0F) {
            fade = Mth.clamp((sigil.duration - age) / 3.0F, 0.0F, 1.0F);
        }
        float r = VIOLET_R;
        float g = VIOLET_G;
        float b = VIOLET_B;
        float hr = LILAC_R;
        float hg = LILAC_G;
        float hb = LILAC_B;
        float diskRadius = 1.1F;
        if (sigil.kind == 1) {
            r = GOLD_R;
            g = GOLD_G;
            b = GOLD_B;
            hr = 1.0F;
            hg = 0.95F;
            hb = 0.75F;
            diskRadius = 0.9F;
        }
        float alpha = (0.55F + 0.45F * t) * appear * fade;
        float pulse = 0.85F + 0.15F * Mth.sin(age * (0.5F + t));
        poseStack.pushPose();
        poseStack.translate(sigil.x, sigil.y, sigil.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-sigil.yaw));

        VertexConsumer light = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        panel(light, poseStack.last(), diskRadius * 2.2F * pop, diskRadius * 2.2F * pop, -0.05F, r, g, b, alpha * 0.5F * pulse);
        if (sigil.kind == 0) {
            VertexConsumer mirror = buffers.getBuffer(FxRenderTypes.glow(MIRROR));
            float rise = 0.15F + 0.85F * appear;
            panel(mirror, poseStack.last(), 0.6F, 1.15F * rise, 0.06F, 0.85F, 0.78F, 1.0F, alpha * 0.85F);
        }

        VertexConsumer disk = buffers.getBuffer(FxRenderTypes.glow(SIGIL));
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 2.5F));
        panel(disk, poseStack.last(), diskRadius * pop, diskRadius * pop, 0.0F, r, g, b, alpha * pulse);
        panel(disk, poseStack.last(), diskRadius * pop, diskRadius * pop, 0.01F, hr, hg, hb, alpha * 0.5F);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-age * 4.0F));
        poseStack.translate(0.0F, 0.0F, -0.25F);
        panel(disk, poseStack.last(), diskRadius * 0.55F * pop, diskRadius * 0.55F * pop, 0.0F, hr, hg, hb, alpha * 0.7F);
        poseStack.popPose();

        VertexConsumer ring = buffers.getBuffer(FxRenderTypes.glow(SIGIL_RING));
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-age * 6.0F));
        float ringRadius = diskRadius * (2.0F - 0.75F * t);
        panel(ring, poseStack.last(), ringRadius, ringRadius, 0.03F, hr, hg, hb, alpha * (0.4F + 0.6F * t));
        poseStack.popPose();

        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        panel(flare, poseStack.last(), 0.8F * pulse * appear, 0.8F * pulse * appear, 0.08F, hr, hg, hb, alpha);
        if (fade < 1.0F) {
            float flash = 1.0F - fade;
            panel(flare, poseStack.last(), diskRadius * 2.5F * flash, diskRadius * 2.5F * flash, 0.1F, 1.0F, 1.0F, 1.0F, fade);
        }
        poseStack.popPose();

        if (sigil.kind == 1) {
            PoseStack.Pose pose = poseStack.last();
            double groundY = sigil.y - 1.05D;
            VertexConsumer ground = buffers.getBuffer(FxRenderTypes.glow(SIGIL));
            FxDraw.flat(ground, pose, sigil.x, groundY, sigil.z, 1.4F * pop, age * 0.05F, r, g, b, alpha * 0.85F);
            VertexConsumer shaft = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            Vec3 foot = new Vec3(sigil.x, groundY, sigil.z);
            FxDraw.beam(shaft, pose, foot, foot.add(0.0D, 2.4D, 0.0D), 0.6F, 0.15F, 0.0F, 1.0F, hr, hg, hb, alpha * 0.6F, 0.0F);
        }
    }

    private static void renderChain(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Chain chain, float partialTick) {
        Vec3[] ends = chainEnds(level, chain, partialTick);
        if (ends == null) {
            return;
        }
        Vec3 from = ends[0];
        Vec3 to = ends[1];
        float age = chain.age + partialTick;
        float r = SOUL_R;
        float g = SOUL_G;
        float b = SOUL_B;
        if (chain.kind == 1) {
            r = LILAC_R;
            g = LILAC_G;
            b = LILAC_B;
        }
        float alpha = 1.0F;
        float width = 0.16F;
        boolean charging = age < chain.windup;
        if (charging) {
            float k = age / Math.max(1, chain.windup);
            alpha = 0.3F + 0.3F * k + 0.2F * Mth.sin(age * 2.2F);
            width = 0.05F + 0.11F * k;
        }
        float shake = 0.0F;
        float reach = 1.0F;
        if (chain.endAge >= 0) {
            float fade = Mth.clamp(1.0F - (age - chain.endAge) / CHAIN_FADE, 0.0F, 1.0F);
            alpha *= fade;
            if (chain.broken) {
                shake = 0.16F * fade;
                r = 1.0F;
                g = 0.4F;
                b = 0.4F;
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
        float[] widths = new float[segments + 1];
        float[] coreWidths = new float[segments + 1];
        float[] alphas = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float s = (float) i / segments * reach;
            Vec3 point = from.add(dir.scale(s)).subtract(0.0D, sag * Mth.sin(Mth.PI * s / Math.max(reach, 0.01F)), 0.0D);
            if (shake > 0.0F && i > 0 && i < segments) {
                point = point.add((random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake);
            }
            points[i] = point;
            float pulse = 0.8F + 0.2F * Mth.sin(s * 12.0F - age * 0.6F);
            widths[i] = width * 2.4F * pulse;
            coreWidths[i] = width * 0.45F;
            alphas[i] = alpha * pulse;
        }
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer energy = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
        FxDraw.trail(energy, pose, points, segments + 1, widths, alphas, scroll * 2.0F, 0.35F, r, g, b);
        VertexConsumer links = buffers.getBuffer(FxRenderTypes.glow(CHAIN));
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < segments; i++) {
                float s0 = (float) i / segments;
                float s1 = (float) (i + 1) / segments;
                float twist0 = s0 * 6.0F + age * 0.08F + strand * Mth.HALF_PI;
                float twist1 = s1 * 6.0F + age * 0.08F + strand * Mth.HALF_PI;
                Vec3 side0 = e1.scale(Mth.cos(twist0)).add(e2.scale(Mth.sin(twist0))).scale(width);
                Vec3 side1 = e1.scale(Mth.cos(twist1)).add(e2.scale(Mth.sin(twist1))).scale(width);
                float u0 = (float) (s0 * length * reach / 0.6D) + scroll;
                float u1 = (float) (s1 * length * reach / 0.6D) + scroll;
                FxDraw.quad(links, pose, points[i].subtract(side0), points[i].add(side0), points[i + 1].add(side1), points[i + 1].subtract(side1),
                    u0, 0.0F, u1, 1.0F, r, g, b, alpha);
            }
        }
        VertexConsumer core = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.trail(core, pose, points, segments + 1, coreWidths, alphas, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, pose, points[0].x, points[0].y, points[0].z, 1.0F + width * 3.0F, 0.0F, r, g, b, alpha * 0.8F);
        FxDraw.sprite(glow, pose, points[segments].x, points[segments].y, points[segments].z, 1.4F + width * 3.0F, 0.0F, r, g, b, alpha * 0.9F);
        if (charging || chain.endAge >= 0) {
            return;
        }
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        for (int bead = 0; bead < 3; bead++) {
            float phase = (age * 0.06F + bead / 3.0F) % 1.0F;
            float s = 1.0F - phase;
            if (chain.kind == 1) {
                s = phase;
            }
            int index = Mth.clamp((int) (s * segments), 0, segments - 1);
            float local = s * segments - index;
            Vec3 center = points[index].add(points[index + 1].subtract(points[index]).scale(local));
            float size = 0.7F + 0.2F * Mth.sin(age * 0.8F + bead);
            FxDraw.sprite(flare, pose, center.x, center.y, center.z, size, age * 0.1F + bead, 1.0F, 1.0F, 1.0F, 0.9F);
            FxDraw.sprite(flare, pose, center.x, center.y, center.z, size * 1.6F, -age * 0.07F, r, g, b, 0.7F);
        }
    }

    private static void renderBurst(PoseStack poseStack, MultiBufferSource buffers, Burst burst, float partialTick) {
        float age = burst.age + partialTick;
        float t = Mth.clamp(age / BURST_LIFE, 0.0F, 1.0F);
        float ease = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        float alpha = 1.0F - t;
        float r = VIOLET_R;
        float g = VIOLET_G;
        float b = VIOLET_B;
        float hr = LILAC_R;
        float hg = LILAC_G;
        float hb = LILAC_B;
        if (burst.kind == 1) {
            r = GOLD_R;
            g = GOLD_G;
            b = GOLD_B;
            hr = 1.0F;
            hg = 0.95F;
            hb = 0.75F;
        }
        float outer = Math.max(0.1F, burst.radius * ease) / 0.9F;
        PoseStack.Pose pose = poseStack.last();
        if (t < 0.45F) {
            float flash = 1.0F - t / 0.45F;
            VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
            FxDraw.sprite(glow, pose, burst.x, burst.y, burst.z, burst.radius * 1.6F, 0.0F, hr, hg, hb, flash);
            VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
            FxDraw.sprite(flare, pose, burst.x, burst.y, burst.z, burst.radius * 1.4F * (0.6F + 0.4F * flash), age * 0.1F, 1.0F, 1.0F, 1.0F, flash);
        }
        VertexConsumer ring = buffers.getBuffer(FxRenderTypes.glow(BURST));
        FxDraw.flat(ring, pose, burst.x, burst.y, burst.z, outer, age * 0.04F, r, g, b, alpha);
        FxDraw.flat(ring, pose, burst.x, burst.y + 0.01D, burst.z, outer * 0.97F, age * 0.04F, hr, hg, hb, alpha * 0.6F);
        if (burst.orthogonal) {
            poseStack.pushPose();
            poseStack.translate(burst.x, burst.y, burst.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 3.0F));
            panel(ring, poseStack.last(), outer * 0.8F, outer * 0.8F, 0.0F, r, g, b, alpha * 0.8F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            panel(ring, poseStack.last(), outer * 0.8F, outer * 0.8F, 0.0F, r, g, b, alpha * 0.8F);
            poseStack.popPose();
        }
        VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
        FxDraw.flat(shock, pose, burst.x, burst.y + 0.02D, burst.z, outer * 1.25F, 0.0F, r, g, b, alpha * 0.6F);
        if (burst.kind == 1 || burst.kind == 3) {
            VertexConsumer sigil = buffers.getBuffer(FxRenderTypes.glow(SIGIL));
            float size = burst.radius * 0.7F;
            if (burst.kind == 3) {
                size = burst.radius * (1.0F - 0.5F * ease);
            }
            FxDraw.flat(sigil, pose, burst.x, burst.y + 0.03D, burst.z, size, -age * 0.08F, hr, hg, hb, alpha);
        }
    }

    private static void renderSlash(PoseStack poseStack, MultiBufferSource buffers, Slash slash, float partialTick) {
        float age = slash.age + partialTick;
        float swing = Mth.clamp(age / SLASH_SWING, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - swing) * (1.0F - swing);
        float fade = 1.0F;
        if (age > SLASH_SWING) {
            fade = Mth.clamp(1.0F - (age - SLASH_SWING) / SLASH_FADE, 0.0F, 1.0F);
        }
        if (fade <= 0.0F) {
            return;
        }
        float halfArc = 45.0F;
        float tail = Math.max(0.0F, eased - 0.8F);
        int slices = 16;
        float inner = slash.reach * 0.3F;
        float outer = slash.reach + 0.35F;
        float hotInner = outer - 0.45F;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(FxRenderTypes.glow(SLASH));
        for (int i = 0; i < slices; i++) {
            float q0 = (float) i / slices;
            float q1 = (float) (i + 1) / slices;
            float p0 = tail + (eased - tail) * q0;
            float p1 = tail + (eased - tail) * q1;
            Vec3 d0 = NagaBossController.directionOf(slash.yaw + halfArc - halfArc * 2.0F * p0);
            Vec3 d1 = NagaBossController.directionOf(slash.yaw + halfArc - halfArc * 2.0F * p1);
            double h0 = slash.y + 1.7D - 1.2D * p0;
            double h1 = slash.y + 1.7D - 1.2D * p1;
            float a0 = fade * q0 * q0;
            float a1 = fade * q1 * q1;
            float u0 = q0 * 2.0F - age * 0.15F;
            float u1 = q1 * 2.0F - age * 0.15F;
            Vec3 in0 = new Vec3(slash.x + d0.x * inner, h0 - 0.2D, slash.z + d0.z * inner);
            Vec3 out0 = new Vec3(slash.x + d0.x * outer, h0 + 0.15D, slash.z + d0.z * outer);
            Vec3 in1 = new Vec3(slash.x + d1.x * inner, h1 - 0.2D, slash.z + d1.z * inner);
            Vec3 out1 = new Vec3(slash.x + d1.x * outer, h1 + 0.15D, slash.z + d1.z * outer);
            Vec3 hot0 = new Vec3(slash.x + d0.x * hotInner, h0 + 0.05D, slash.z + d0.z * hotInner);
            Vec3 hot1 = new Vec3(slash.x + d1.x * hotInner, h1 + 0.05D, slash.z + d1.z * hotInner);
            FxDraw.glowVertex(consumer, pose, in0.x, in0.y, in0.z, u0, 0.0F, GOLD_R, GOLD_G, GOLD_B, a0);
            FxDraw.glowVertex(consumer, pose, out0.x, out0.y, out0.z, u0, 1.0F, GOLD_R, GOLD_G, GOLD_B, a0);
            FxDraw.glowVertex(consumer, pose, out1.x, out1.y, out1.z, u1, 1.0F, GOLD_R, GOLD_G, GOLD_B, a1);
            FxDraw.glowVertex(consumer, pose, in1.x, in1.y, in1.z, u1, 0.0F, GOLD_R, GOLD_G, GOLD_B, a1);
            FxDraw.glowVertex(consumer, pose, hot0.x, hot0.y, hot0.z, u0, 0.0F, 1.0F, 0.97F, 0.85F, a0);
            FxDraw.glowVertex(consumer, pose, out0.x, out0.y, out0.z, u0, 1.0F, 1.0F, 0.97F, 0.85F, a0);
            FxDraw.glowVertex(consumer, pose, out1.x, out1.y, out1.z, u1, 1.0F, 1.0F, 0.97F, 0.85F, a1);
            FxDraw.glowVertex(consumer, pose, hot1.x, hot1.y, hot1.z, u1, 0.0F, 1.0F, 0.97F, 0.85F, a1);
        }
        Vec3 tipDir = NagaBossController.directionOf(slash.yaw + halfArc - halfArc * 2.0F * eased);
        double tipY = slash.y + 1.7D - 1.2D * eased + 0.1D;
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        FxDraw.sprite(flare, pose, slash.x + tipDir.x * outer, tipY, slash.z + tipDir.z * outer, 1.6F * fade, age * 0.3F, 1.0F, 1.0F, 0.9F, fade);
        FxDraw.sprite(flare, pose, slash.x + tipDir.x * outer, tipY, slash.z + tipDir.z * outer, 2.6F * fade, -age * 0.15F, GOLD_R, GOLD_G, GOLD_B, 0.6F * fade);
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
        float pulse = 0.85F + 0.15F * Mth.sin(age * (0.3F + progress * 0.6F));
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer ground = buffers.getBuffer(FxRenderTypes.glow(SIGIL_RING));
        FxDraw.flat(ground, pose, pos.x, crown.baseY + 0.05D, pos.z, crown.pull, age * 0.01F, VIOLET_R, VIOLET_G, VIOLET_B, (0.4F + 0.5F * progress) * fade);
        VertexConsumer inner = buffers.getBuffer(FxRenderTypes.glow(SIGIL));
        FxDraw.flat(inner, pose, pos.x, crown.baseY + 0.06D, pos.z, Math.min(crown.pull * 0.5F, 2.5F), -age * 0.02F, GOLD_R, GOLD_G, GOLD_B, (0.3F + 0.6F * progress) * fade * pulse);
        VertexConsumer wall = buffers.getBuffer(FxRenderTypes.glow(FxTextures.STREAKS));
        FxDraw.wall(wall, pose, pos.x, crown.baseY, pos.z, crown.pull, 1.0F + progress, 64, Math.max(4.0F, crown.pull * 2.0F), -age * 0.02F,
            VIOLET_R, VIOLET_G, VIOLET_B, (0.3F + 0.4F * progress) * fade, 0.0F);

        if (crown.endAge < 0) {
            VertexConsumer rays = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
            Vec3 center = pos.add(0.0D, height * 0.55D, 0.0D);
            for (int i = 0; i < 12; i++) {
                float angle = i * Mth.TWO_PI / 12.0F + age * 0.01F;
                Vec3 start = new Vec3(pos.x + Mth.cos(angle) * crown.pull, crown.baseY + 0.2D, pos.z + Mth.sin(angle) * crown.pull);
                float flicker = 0.6F + 0.4F * Mth.sin(age * 0.5F + i * 1.3F);
                FxDraw.beam(rays, pose, start, center, 0.08F, 0.2F, -age * 0.25F + i * 0.37F, -age * 0.25F + i * 0.37F + crown.pull / 3.0F,
                    LILAC_R, LILAC_G, LILAC_B, 0.0F, 0.75F * progress * flicker);
            }
        }

        VertexConsumer halo = buffers.getBuffer(FxRenderTypes.glow(HALO));
        float radius = Math.max(1.0F, crown.radius * 0.5F);
        float ringAlpha = (0.45F + 0.55F * progress) * fade;
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y + height * 0.55D, pos.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2.0F));
        FxDraw.ring(halo, poseStack.last(), 0.0D, 0.0D, 0.0D, radius - 0.22F, radius + 0.22F, 48, 0.0F, Math.max(4.0F, radius * 2.0F), GOLD_R, GOLD_G, GOLD_B, ringAlpha, ringAlpha);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 5.0F));
        FxDraw.ring(halo, poseStack.last(), 0.0D, 0.0D, 0.0D, radius * 0.75F - 0.12F, radius * 0.75F + 0.12F, 40, 0.0F, Math.max(3.0F, radius * 1.5F),
            1.0F, 0.95F, 0.75F, ringAlpha * 0.7F, ringAlpha * 0.7F);
        poseStack.popPose();

        double crownY = pos.y + height + 0.25D + 0.06D * Mth.sin(age * 0.2F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, pose, pos.x, crownY + 0.2D, pos.z, 2.0F * pulse, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.6F * fade);
        VertexConsumer spikes = buffers.getBuffer(FxRenderTypes.glow(CROWN));
        float crownScale = 0.6F + 0.4F * Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(pos.x, crownY, pos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 4.0F));
        poseStack.scale(crownScale, crownScale, crownScale);
        for (int i = 0; i < 8; i++) {
            float spike = 0.22F;
            if (i % 2 == 0) {
                spike = 0.4F;
            }
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 45.0F));
            poseStack.translate(0.0F, 0.0F, 0.32F);
            poseStack.mulPose(Axis.XP.rotationDegrees(12.0F));
            FxDraw.shard(spikes, poseStack.last(), 0.07F, spike * 0.6F, spike * 0.5F, 4, GOLD_R, GOLD_G, GOLD_B, 0.9F * fade);
            poseStack.popPose();
        }
        poseStack.popPose();
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
        double centerY = pos.y + lich.getBbHeight() * 0.45D;
        VertexConsumer consumer = buffers.getBuffer(FxRenderTypes.glow(HALO_BROKEN));
        poseStack.pushPose();
        poseStack.translate(pos.x, centerY, pos.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.3F) * 9.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(age * 0.23F) * 7.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 3.0F));
        FxDraw.wall(consumer, poseStack.last(), 0.0D, -0.14D, 0.0D, 0.8F, 0.28F, 32, 2.0F, 0.0F, GOLD_R, GOLD_G, GOLD_B, alpha, alpha);
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 7.0F));
        FxDraw.wall(consumer, poseStack.last(), 0.0D, -0.07D, 0.0D, 0.98F, 0.14F, 32, 3.0F, 0.0F, LILAC_R, LILAC_G, LILAC_B, alpha * 0.7F, alpha * 0.7F);
        poseStack.popPose();
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        double headY = pos.y + lich.getBbHeight() + 0.3D;
        for (int i = 0; i < 3; i++) {
            float angle = age * 0.25F + i * Mth.TWO_PI / 3.0F;
            FxDraw.sprite(flare, poseStack.last(), pos.x + Mth.cos(angle) * 0.45D, headY + Mth.sin(angle * 2.0F) * 0.08D, pos.z + Mth.sin(angle) * 0.45D,
                0.55F, age * 0.2F, 1.0F, 0.92F, 0.6F, alpha);
        }
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffers, Beam beam, float partialTick) {
        float age = beam.age + partialTick;
        float t = Mth.clamp(age / BEAM_LIFE, 0.0F, 1.0F);
        Vec3 from = new Vec3(beam.x0, beam.y0, beam.z0);
        Vec3 to = new Vec3(beam.x1, beam.y1, beam.z1);
        float length = (float) from.distanceTo(to);
        float width = 0.3F * (1.0F - t * 0.6F);
        float alpha = 1.0F - t;
        float scroll = -age * 0.3F;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer energy = buffers.getBuffer(FxRenderTypes.glow(FxTextures.ENERGY));
        FxDraw.beam(energy, pose, from, to, width * 0.6F, width, scroll, scroll + length / 3.0F, beam.r, beam.g, beam.b, alpha * 0.6F, alpha);
        VertexConsumer core = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.beam(core, pose, from, to, width * 0.2F, width * 0.3F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha * 0.7F, alpha);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, pose, from.x, from.y, from.z, 0.8F, 0.0F, beam.r, beam.g, beam.b, alpha);
    }

    private static final class Ghost {
        final double x;
        final double y;
        final double z;
        final double tx;
        final double ty;
        final double tz;
        final float yaw;
        int age;

        Ghost(double x, double y, double z, double tx, double ty, double tz, float yaw) {
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
