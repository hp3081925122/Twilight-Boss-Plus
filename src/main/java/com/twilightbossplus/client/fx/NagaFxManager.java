package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.network.NagaFxPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
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
import net.minecraftforge.entity.PartEntity;
import twilightforest.entity.boss.Naga;

public final class NagaFxManager {
    private static final ResourceLocation SCALES = tex("naga2/scales");
    private static final ResourceLocation VENOM = tex("naga2/venom");
    public static final ResourceLocation POOL = tex("naga2/pool");
    private static final ResourceLocation CRACK = tex("naga2/crack");
    private static final ResourceLocation RUNE = tex("naga2/rune");
    private static final ResourceLocation FLAKE = tex("naga2/flake");
    private static final ResourceLocation HUSK = tex("naga/husk");
    public static final ResourceLocation DROPLET = VENOM;

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
    private static final float DUST_R = 0.42F;
    private static final float DUST_G = 0.37F;
    private static final float DUST_B = 0.28F;

    private static final List<Husk> HUSKS = new ArrayList<>();
    private static final List<Crack> CRACKS = new ArrayList<>();
    private static final List<Constrict> CONSTRICTS = new ArrayList<>();

    private NagaFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/" + name + ".png");
    }

    private static float angleOf(float yaw) {
        return (yaw + 90.0F) * Mth.DEG_TO_RAD;
    }

    private static Vec3 forward(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    private static Naga naga(int id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(id);
        if (entity instanceof Naga naga && naga.isAlive()) {
            return naga;
        }
        return null;
    }

    private static Vec3 mouth(Naga naga, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, naga.yRotO, naga.getYRot());
        double x = Mth.lerp(partialTick, naga.xo, naga.getX());
        double y = Mth.lerp(partialTick, naga.yo, naga.getY());
        double z = Mth.lerp(partialTick, naga.zo, naga.getZ());
        return new Vec3(x, y + naga.getBbHeight() * 0.55D, z).add(forward(yaw).scale(naga.getBbWidth() * 0.6D));
    }

    private static void prune() {
        HUSKS.removeIf(husk -> husk.age >= husk.life);
        CRACKS.removeIf(crack -> crack.age >= crack.life);
        CONSTRICTS.removeIf(constrict -> constrict.age >= constrict.life);
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        prune();
        RandomSource random = level.random;
        if (type == NagaFxPacket.DECAL && data.length >= 6) {
            int life = Math.max(4, (int) data[4]);
            if (data[5] > 0.5F) {
                FxWorld.decal(RUNE, data[0], data[1], data[2], data[3] * 1.5F, life, VENOM_R, VENOM_G, VENOM_B).spin(random.nextFloat() * 6.0F, 0.09F);
                FxWorld.add(new ClosingRing(data[0], data[1], data[2], data[3] * 1.15F, life, GOLD_R, GOLD_G, GOLD_B));
                FxWorld.add(new GroundRumble(data[0], data[1], data[2], data[3], life));
            } else {
                FxWorld.decal(RUNE, data[0], data[1], data[2], data[3] * 1.2F, life, GREEN_R, GREEN_G, GREEN_B).spin(random.nextFloat() * 6.0F, 0.04F);
                FxWorld.add(new ClosingRing(data[0], data[1], data[2], data[3], life, VENOM_R, VENOM_G, VENOM_B));
            }
        } else if (type == NagaFxPacket.FANG_WINDUP && data.length >= 2) {
            FxWorld.add(new MouthCharge(entityId, (int) data[0], MouthCharge.FANG));
        } else if (type == NagaFxPacket.FANG_BITE && data.length >= 5) {
            fangBite(random, data[0], data[1], data[2], data[3], (int) data[4]);
        } else if (type == NagaFxPacket.IMPACT && data.length >= 4) {
            impact(level, random, data[0], data[1], data[2], (int) data[3]);
        } else if (type == NagaFxPacket.BARRAGE_WINDUP && data.length >= 1) {
            FxWorld.add(new MouthCharge(entityId, (int) data[0], MouthCharge.BARRAGE));
        } else if (type == NagaFxPacket.SWEEP && data.length >= 12) {
            sweep(random, data);
        } else if (type == NagaFxPacket.SHED && data.length >= 6) {
            Husk husk = new Husk(entityId);
            for (int i = 0; i + 5 < data.length; i += 6) {
                husk.pieces.add(new float[] {data[i], data[i + 1], data[i + 2], data[i + 3], data[i + 4], data[i + 5]});
            }
            HUSKS.add(FxWorld.add(husk));
            float[] head = husk.pieces.get(0);
            FxShake.at(head[0], head[1], head[2], 0.6F, 20.0F, 10);
            FxWorld.flash(head[0], head[1] + 1.0D, head[2], 4.0F, 12, GREEN_R, GREEN_G, GREEN_B);
            for (float[] piece : husk.pieces) {
                flakes(random, piece[0], piece[1] + piece[5] * 0.5D, piece[2], FxWorld.scaled(4), 0.12D, GREEN_R, GREEN_G, GREEN_B);
            }
        } else if (type == NagaFxPacket.MOLT_BURST && data.length >= 5) {
            moltBurst(level, random, entityId, data[0], data[1], data[2], data[3], data[4]);
        } else if (type == NagaFxPacket.SLAM && data.length >= 4) {
            slam(level, random, data[0], data[1], data[2]);
        } else if (type == NagaFxPacket.REND_POINT && data.length >= 5) {
            rendPoint(level, random, entityId, data[0], data[1], data[2], data[3], data[4] > 0.5F);
        } else if (type == NagaFxPacket.SPIKE && data.length >= 5) {
            FxWorld.add(new Spike(data[0], data[1], data[2], (int) data[3], data[4], random.nextFloat()));
        } else if (type == NagaFxPacket.BITE && data.length >= 4) {
            for (Crack crack : CRACKS) {
                if (crack.nagaId == entityId) {
                    crack.close();
                }
            }
            FxWorld.add(new Jaw(data[0], data[1], data[2], data[3]));
        } else if (type == NagaFxPacket.TEMPEST_WINDUP && data.length >= 1) {
            FxWorld.add(new MouthCharge(entityId, (int) data[0], MouthCharge.TEMPEST));
        } else if (type == NagaFxPacket.TEMPEST_WAVE && data.length >= 8) {
            tempestWave(random, data);
        } else if (type == NagaFxPacket.CONSTRICT_START && data.length >= 8) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId) {
                    constrict.end(false);
                }
            }
            CONSTRICTS.add(FxWorld.add(new Constrict(entityId, data[0], data[1], data[2], data[3], data[4], (int) data[5], (int) data[6], (int) data[7])));
        } else if (type == NagaFxPacket.CONSTRICT_END && data.length >= 1) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId && constrict.endAge < 0) {
                    constrict.end(data[0] > 0.5F);
                }
            }
        } else if (type == NagaFxPacket.WEAK_HIT) {
            for (Constrict constrict : CONSTRICTS) {
                if (constrict.nagaId == entityId && constrict.endAge < 0) {
                    constrict.flash = 8;
                    Vec3 weak = constrict.weakPos(1.0F);
                    if (weak != null) {
                        FxShake.at(weak.x, weak.y, weak.z, 0.5F, 12.0F, 6);
                        FxWorld.flash(weak.x, weak.y, weak.z, 2.5F, 8, GOLD_R, GOLD_G, GOLD_B);
                        sparks(random, weak.x, weak.y, weak.z, FxWorld.scaled(18), 0.35D, 1.0F, 0.92F, 0.6F);
                    }
                }
            }
        }
    }

    public static void poolSplash(double x, double y, double z) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        FxWorld.shockwave(x, y, z, 2.2F, 0.7F, 10, VENOM_R, VENOM_G, VENOM_B).start(0.3F);
        FxWorld.flash(x, y + 0.3D, z, 1.6F, 6, VENOM_R, VENOM_G, VENOM_B).flare(0.0F);
        droplets(random, x, y + 0.2D, z, FxWorld.scaled(12), 0.18D, 0.28D, y);
        toxicSmoke(random, x, y + 0.2D, z, FxWorld.scaled(3), 0.8D);
    }

    private static void droplets(RandomSource random, double x, double y, double z, int count, double speed, double up, double floor) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.4D + random.nextDouble() * 0.8D);
            FxParticles.add(VENOM, x, y, z, Math.cos(angle) * s, up * (0.5D + random.nextDouble()), Math.sin(angle) * s, 16 + random.nextInt(12),
                    0.16F + random.nextFloat() * 0.14F)
                .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G * 0.8F, GREEN_B).gravity(0.04D).drag(0.97D).streak(1.4F)
                .floor(floor + 0.05D, 0.0D).shade(0.5F, 0.2F, 1.0F);
        }
    }

    private static void toxicSmoke(RandomSource random, double x, double y, double z, int count, double spread) {
        for (int i = 0; i < count; i++) {
            FxParticles.alpha(FxWorld.SMOKE, x + (random.nextDouble() - 0.5D) * spread, y + random.nextDouble() * 0.4D, z + (random.nextDouble() - 0.5D) * spread,
                    (random.nextDouble() - 0.5D) * 0.03D, 0.02D + random.nextDouble() * 0.03D, (random.nextDouble() - 0.5D) * 0.03D, 34 + random.nextInt(20),
                    0.9F + random.nextFloat() * 0.6F)
                .color(GREEN_R * 0.55F, GREEN_G * 0.6F, GREEN_B * 0.5F).fadeTo(DARK_R, DARK_G, DARK_B).alpha(0.5F).grow(2.6F).drag(0.93D)
                .fade(0.15F, 1.2F).spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.05F).shade(0.0F, 0.8F, 1.4F)
                .scroll(0.05F, 0.08F).dissolve(0.45F);
        }
    }

    private static void flakes(RandomSource random, double x, double y, double z, int count, double speed, float r, float g, float b) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.5D + random.nextDouble());
            FxParticles.add(FLAKE, x, y, z, Math.cos(angle) * s, 0.06D + random.nextDouble() * 0.14D, Math.sin(angle) * s, 28 + random.nextInt(16),
                    0.22F + random.nextFloat() * 0.16F)
                .color(r, g, b).fadeTo(DARK_R * 2.0F, DARK_G * 2.0F, DARK_B * 2.0F).gravity(0.008D).drag(0.93D)
                .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.45F).shade(0.4F, 0.0F, 1.0F).dissolve(0.6F);
        }
    }

    private static void sparks(RandomSource random, double x, double y, double z, int count, double speed, float r, float g, float b) {
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.25D, random.nextDouble() - 0.5D).normalize()
                .scale(speed * (0.5D + random.nextDouble()));
            FxParticles.add(FxWorld.SPARK, x, y, z, dir.x, dir.y, dir.z, 7 + random.nextInt(7), 0.12F + random.nextFloat() * 0.08F)
                .color(1.0F, 1.0F, 0.9F).fadeTo(r, g, b).drag(0.84D).gravity(0.012D).streak(2.6F).shade(0.8F, 0.0F, 1.0F);
        }
    }

    private static void dust(RandomSource random, double x, double y, double z, int count, double spread, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.4D + random.nextDouble());
            FxParticles.alpha(FxWorld.SMOKE, x + Math.cos(angle) * spread * random.nextDouble(), y + 0.15D, z + Math.sin(angle) * spread * random.nextDouble(),
                    Math.cos(angle) * s, 0.02D + random.nextDouble() * 0.04D, Math.sin(angle) * s, 26 + random.nextInt(16), 1.0F + random.nextFloat() * 0.6F)
                .color(DUST_R, DUST_G, DUST_B).fadeTo(DUST_R * 0.7F, DUST_G * 0.7F, DUST_B * 0.7F).alpha(0.55F).grow(2.8F).drag(0.88D)
                .fade(0.1F, 1.3F).spin(random.nextFloat() * 6.0F, 0.02F).shade(0.0F, 0.7F, 1.2F).dissolve(0.5F);
        }
    }

    private static void debris(ClientLevel level, double x, double y, double z, int count, double power) {
        BlockState state = level.getBlockState(BlockPos.containing(x, y - 0.5D, z));
        if (state.isAir() || count <= 0) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
        FxWorld.add(new Debris(level.random, sprite, x, y, z, count, power));
    }

    private static void fangBite(RandomSource random, float x, float y, float z, float yaw, int index) {
        Vec3 mouth = new Vec3(x, y, z);
        Vec3 dir = forward(yaw);
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        float tilt = 0.6F;
        if (index % 2 == 1) {
            tilt = -0.6F;
        }
        Vec3 e2 = side.scale(Mth.cos(tilt)).add(0.0D, Mth.sin(tilt), 0.0D).normalize();
        Vec3 center = mouth.subtract(dir.scale(0.4D));
        float from = -1.25F;
        float to = 1.25F;
        if (index % 2 == 1) {
            from = 1.25F;
            to = -1.25F;
        }
        FxWorld.slash(center, dir, e2, 2.9F, 1.3F, from, to, 12, VENOM_R, VENOM_G, VENOM_B).reveal(0.3F);
        FxWorld.slash(center, dir, e2, 2.4F, 0.6F, from, to, 10, GREEN_R, GREEN_G, GREEN_B).reveal(0.25F);
        Vec3 tip = mouth.add(dir.scale(1.6D));
        FxWorld.flash(tip.x, tip.y, tip.z, 1.8F, 6, VENOM_R, VENOM_G, VENOM_B).flare(1.4F);
        for (int i = 0; i < FxWorld.scaled(10); i++) {
            Vec3 v = dir.scale(0.3D + random.nextDouble() * 0.3D).add((random.nextDouble() - 0.5D) * 0.2D, random.nextDouble() * 0.15D, (random.nextDouble() - 0.5D) * 0.2D);
            FxParticles.add(VENOM, tip.x, tip.y, tip.z, v.x, v.y, v.z, 14 + random.nextInt(8), 0.14F + random.nextFloat() * 0.1F)
                .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).gravity(0.035D).drag(0.93D).streak(1.8F).shade(0.5F, 0.0F, 1.0F);
        }
        double ground = y - 1.1D;
        FxWorld.groove(CRACK, mouth.add(0.0D, -1.05D, 0.0D), mouth.add(dir.scale(3.2D)).add(0.0D, -1.05D, 0.0D), 0.7F, 40,
            GREEN_R, GREEN_G, GREEN_B).grow(5).alpha(0.75F);
        toxicSmoke(random, tip.x, ground + 0.3D, tip.z, FxWorld.scaled(2), 1.2D);
        FxShake.at(x, y, z, 0.25F, 10.0F, 4);
    }

    private static void impact(ClientLevel level, RandomSource random, float x, float y, float z, int kind) {
        if (kind == 1) {
            FxWorld.flash(x, y, z, 1.6F, 6, DUST_R * 1.6F, DUST_G * 1.6F, DUST_B * 1.6F).flare(1.0F);
            debris(level, x, y - 0.5D, z, FxWorld.scaled(5), 0.8D);
            sparks(random, x, y, z, FxWorld.scaled(8), 0.25D, GREEN_R, GREEN_G, GREEN_B);
        } else if (kind == 2) {
            FxWorld.flash(x, y, z, 1.8F, 6, GREEN_R, GREEN_G, GREEN_B).flare(1.2F);
            flakes(random, x, y, z, FxWorld.scaled(8), 0.2D, VENOM_R, VENOM_G, VENOM_B);
            sparks(random, x, y, z, FxWorld.scaled(10), 0.32D, GREEN_R, GREEN_G, GREEN_B);
        } else {
            FxWorld.flash(x, y, z, 2.0F, 7, VENOM_R, VENOM_G, VENOM_B).flare(1.4F);
            droplets(random, x, y, z, FxWorld.scaled(10), 0.16D, 0.2D, y - 1.5D);
            sparks(random, x, y, z, FxWorld.scaled(6), 0.3D, VENOM_R, VENOM_G, VENOM_B);
        }
        FxScreen.pulse(x, y, z, GREEN_R, GREEN_G, GREEN_B, 0.35F, 3.0F, 10);
    }

    private static void sweep(RandomSource random, float[] data) {
        double px = data[0];
        double py = data[1];
        double pz = data[2];
        float yaw = data[3];
        float radius = data[4];
        float half = data[5];
        int warn = (int) data[6];
        int strike = (int) data[7];
        float sign = data[8];
        boolean twice = data[9] > 0.5F;
        int pause = (int) data[10];
        FxWorld.add(new SweepWarn(px, py, pz, yaw, radius, half, warn, twice));
        float a0 = angleOf(yaw - sign * half);
        float a1 = angleOf(yaw + sign * half);
        Vec3 center = new Vec3(px, py + 0.7D, pz);
        Vec3 e1 = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 e2 = new Vec3(0.0D, 0.0D, 1.0D);
        FxWorld.slash(center, e1, e2, radius + 0.4F, 2.4F, a0, a1, strike + 10, GREEN_R, GREEN_G, GREEN_B).reveal(0.4F).delay(warn);
        FxWorld.slash(center.add(0.0D, 0.25D, 0.0D), e1, e2, radius, 0.8F, a0, a1, strike + 8, VENOM_R, VENOM_G, VENOM_B).reveal(0.35F).delay(warn);
        FxWorld.slash(new Vec3(px, py + 0.06D, pz), e1, e2, radius, 1.2F, a0, a1, 40, DARK_R * 3.0F, DARK_G * 3.0F, DARK_B * 3.0F).reveal(0.15F).delay(warn);
        FxWorld.later(warn + strike / 2, () -> sweepSparks(random, px, py, pz, radius, a0, a1));
        if (twice) {
            int second = warn + strike + pause;
            FxWorld.slash(center, e1, e2, radius + 0.4F, 2.4F, a1, a0, strike + 10, GREEN_R, GREEN_G, GREEN_B).reveal(0.4F).delay(second);
            FxWorld.slash(center.add(0.0D, 0.25D, 0.0D), e1, e2, radius, 0.8F, a1, a0, strike + 8, GOLD_R, GOLD_G, GOLD_B).reveal(0.35F).delay(second);
            FxWorld.later(second + strike / 2, () -> sweepSparks(random, px, py, pz, radius, a1, a0));
        }
    }

    private static void sweepSparks(RandomSource random, double px, double py, double pz, float radius, float a0, float a1) {
        FxShake.at(px, py, pz, 0.5F, 14.0F, 6);
        int count = FxWorld.scaled(20);
        for (int i = 0; i < count; i++) {
            float a = Mth.lerp(random.nextFloat(), a0, a1);
            double r = radius * (0.4D + random.nextDouble() * 0.65D);
            double x = px + Mth.cos(a) * r;
            double z = pz + Mth.sin(a) * r;
            float tangent = a + Mth.HALF_PI * Math.signum(a1 - a0);
            double s = 0.25D + random.nextDouble() * 0.25D;
            FxParticles.add(FxWorld.SPARK, x, py + 0.2D + random.nextDouble() * 0.8D, z, Mth.cos(tangent) * s, 0.05D + random.nextDouble() * 0.1D,
                    Mth.sin(tangent) * s, 8 + random.nextInt(6), 0.14F)
                .color(1.0F, 1.0F, 0.85F).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.85D).gravity(0.02D).streak(2.8F).shade(0.8F, 0.0F, 1.0F);
        }
        dust(random, px, py, pz, FxWorld.scaled(6), radius * 0.8D, 0.06D);
    }

    private static void moltBurst(ClientLevel level, RandomSource random, int entityId, float x, float y, float z, float maxRadius, float speed) {
        for (Husk husk : HUSKS) {
            if (husk.nagaId == entityId) {
                husk.burst();
            }
        }
        int life = Math.max(4, Math.round((maxRadius - 0.5F) / speed));
        FxWorld.add(new MoltWave(x, y, z, maxRadius, life));
        FxWorld.flash(x, y + 1.2D, z, 7.0F, 16, VENOM_R, VENOM_G, VENOM_B).flare(2.6F);
        FxWorld.shockwave(x, y, z, 5.0F, 1.8F, 12, 1.0F, 1.0F, 0.85F).start(0.5F).hot(1.0F);
        FxWorld.pillar(x, y, z, 2.2F, 7.0F, 20, GREEN_R, GREEN_G, GREEN_B);
        FxShake.at(x, y, z, 1.2F, 26.0F, 16);
        FxScreen.pulse(x, y, z, GREEN_R, GREEN_G, GREEN_B, 0.9F, 18.0F, 22);
        flakes(random, x, y + 1.0D, z, FxWorld.scaled(36), 0.32D, GREEN_R, GREEN_G, GREEN_B);
        droplets(random, x, y + 1.0D, z, FxWorld.scaled(24), 0.25D, 0.45D, y);
        toxicSmoke(random, x, y + 0.5D, z, FxWorld.scaled(10), 4.0D);
        debris(level, x, y, z, FxWorld.scaled(10), 1.0D);
    }

    private static void slam(ClientLevel level, RandomSource random, float x, float y, float z) {
        FxWorld.shockwave(x, y, z, 4.5F, 1.4F, 14, DUST_R * 2.0F, DUST_G * 2.0F, DUST_B * 1.6F).start(0.4F).wall(1.1F).hot(0.3F);
        FxWorld.shockwave(x, y, z, 3.0F, 0.6F, 10, VENOM_R, VENOM_G, VENOM_B).start(0.2F);
        FxWorld.flash(x, y + 0.4D, z, 3.0F, 8, VENOM_R, VENOM_G, VENOM_B).flare(1.6F);
        FxShake.at(x, y, z, 0.9F, 20.0F, 10);
        debris(level, x, y, z, FxWorld.scaled(14), 1.2D);
        dust(random, x, y, z, FxWorld.scaled(12), 1.6D, 0.14D);
    }

    private static void rendPoint(ClientLevel level, RandomSource random, int entityId, float x, float y, float z, float yaw, boolean start) {
        Crack crack = null;
        for (Crack existing : CRACKS) {
            if (existing.nagaId == entityId && existing.open) {
                crack = existing;
            }
        }
        if (crack == null || start) {
            if (crack != null) {
                crack.close();
            }
            crack = FxWorld.add(new Crack(entityId));
            CRACKS.add(crack);
        }
        crack.add(x, y, z);
        if (random.nextFloat() < 0.6F) {
            debris(level, x, y, z, 1, 0.5D);
        }
        if (random.nextFloat() < FxWorld.quality()) {
            dust(random, x, y, z, 1, 0.3D, 0.03D);
        }
        if (random.nextFloat() < FxWorld.quality()) {
            FxParticles.add(FxWorld.GLOW, x + (random.nextDouble() - 0.5D) * 0.5D, y + 0.1D, z + (random.nextDouble() - 0.5D) * 0.5D,
                    (random.nextDouble() - 0.5D) * 0.02D, 0.05D + random.nextDouble() * 0.06D, (random.nextDouble() - 0.5D) * 0.02D, 16 + random.nextInt(10),
                    0.3F + random.nextFloat() * 0.2F)
                .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.95D).flicker(0.4F, random.nextFloat() * 10.0F);
        }
    }

    private static void tempestWave(RandomSource random, float[] data) {
        TempestWave wave = new TempestWave(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
        FxWorld.add(wave);
        FxWorld.shockwave(data[0], data[1], data[2], 3.0F, 1.0F, 8, 1.0F, 1.0F, 0.85F).start(0.5F).hot(1.0F);
        FxWorld.flash(data[0], data[1] + 1.2D, data[2], 3.5F, 8, GREEN_R, GREEN_G, GREEN_B).flare(2.0F);
        FxShake.at(data[0], data[1], data[2], 0.5F, 18.0F, 6);
        flakes(random, data[0], data[1] + 1.2D, data[2], FxWorld.scaled(12), 0.35D, GREEN_R, GREEN_G, GREEN_B);
    }

    private static final class ClosingRing extends FxWorld.Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private final float r;
        private final float g;
        private final float b;

        private ClosingRing(double x, double y, double z, float radius, int life, float r, float g, float b) {
            super(life);
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float close = FxEase.inOutSine(Math.min(1.0F, t / 0.85F));
            float current = Mth.lerp(close, this.radius * 2.4F, this.radius);
            float a = FxEase.window(t, 0.15F, 0.08F) * (0.4F + 0.6F * close);
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.7F, 1.0F, 0.0F);
            VertexConsumer band = FxBuffers.get(FxRenderTypes.add(FxWorld.BAND));
            FxDraw.ring(band, pose, this.x, this.y + 0.05D, this.z, current - 0.22F, current + 0.22F, FxWorld.segments(40), 0.0F, 1.0F,
                this.r, this.g, this.b, a, a);
            if (t > 0.85F) {
                float burst = (t - 0.85F) / 0.15F;
                FxDraw.ring(band, pose, this.x, this.y + 0.06D, this.z, this.radius - 0.3F, this.radius + 0.3F + burst * 0.4F, FxWorld.segments(40), 0.0F, 1.0F,
                    1.0F, 1.0F, 1.0F, 1.0F - burst, 1.0F - burst);
            }
        }
    }

    private static final class GroundRumble extends FxWorld.Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float radius;

        private GroundRumble(double x, double y, double z, float radius, int life) {
            super(life);
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }

        @Override
        protected void tick() {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || level.random.nextFloat() > FxWorld.quality() * 0.8F) {
                return;
            }
            RandomSource random = level.random;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = random.nextDouble() * this.radius;
            double px = this.x + Math.cos(angle) * distance;
            double pz = this.z + Math.sin(angle) * distance;
            dust(random, px, this.y, pz, 1, 0.2D, 0.02D);
            if (random.nextBoolean()) {
                debris(level, px, this.y, pz, 1, 0.35D);
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
        }
    }

    private static final class MouthCharge extends FxWorld.Effect {
        private static final int FANG = 0;
        private static final int BARRAGE = 1;
        private static final int TEMPEST = 2;
        private final int nagaId;
        private final int kind;

        private MouthCharge(int nagaId, int life, int kind) {
            super(life);
            this.nagaId = nagaId;
            this.kind = kind;
        }

        @Override
        protected void tick() {
            Naga naga = naga(this.nagaId);
            ClientLevel level = Minecraft.getInstance().level;
            if (naga == null || level == null) {
                this.kill();
                return;
            }
            RandomSource random = level.random;
            Vec3 mouth = mouth(naga, 1.0F);
            float t = (float) this.age / this.life;
            if (this.kind == TEMPEST) {
                int count = FxWorld.scaled(4);
                for (int i = 0; i < count; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double radius = 2.6D - t * 1.2D;
                    Vec3 center = naga.position().add(0.0D, naga.getBbHeight() * 0.5D, 0.0D);
                    double px = center.x + Math.cos(angle) * radius;
                    double pz = center.z + Math.sin(angle) * radius;
                    double tangent = angle + Math.PI * 0.5D;
                    double s = 0.22D + t * 0.2D;
                    FxParticles.add(FLAKE, px, center.y + (random.nextDouble() - 0.5D) * 1.6D, pz,
                            Math.cos(tangent) * s - Math.cos(angle) * 0.06D, (random.nextDouble() - 0.3D) * 0.06D, Math.sin(tangent) * s - Math.sin(angle) * 0.06D,
                            10 + random.nextInt(6), 0.26F + random.nextFloat() * 0.14F)
                        .color(GREEN_R, GREEN_G, GREEN_B).fadeTo(VENOM_R, VENOM_G, VENOM_B).drag(0.9D)
                        .spin(random.nextFloat() * 6.0F, 0.4F).shade(0.3F + t * 0.6F, 0.0F, 1.0F);
                }
                return;
            }
            int count = FxWorld.scaled(3);
            for (int i = 0; i < count; i++) {
                Vec3 offset = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).normalize().scale(1.4D + random.nextDouble());
                Vec3 start = mouth.add(offset);
                Vec3 v = offset.scale(-0.12D);
                FxParticles.add(FxWorld.SPARK, start.x, start.y, start.z, v.x, v.y, v.z, 8, 0.1F + random.nextFloat() * 0.06F)
                    .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(1.0F, 1.0F, 0.8F).drag(1.0D).streak(2.0F).fade(0.3F, 1.0F).shade(0.6F, 0.0F, 1.0F);
            }
            if (this.kind == BARRAGE && random.nextFloat() < 0.6F) {
                FxParticles.add(VENOM, mouth.x, mouth.y - 0.2D, mouth.z, (random.nextDouble() - 0.5D) * 0.02D, -0.02D, (random.nextDouble() - 0.5D) * 0.02D,
                        14, 0.12F + random.nextFloat() * 0.08F)
                    .color(VENOM_R, VENOM_G, VENOM_B).gravity(0.03D).drag(0.98D).streak(1.2F).floor(naga.getY() + 0.05D, 0.0D);
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            Naga naga = naga(this.nagaId);
            if (naga == null) {
                return;
            }
            float grow = FxEase.outCubic(t);
            float pulse = 0.85F + 0.15F * Mth.sin((this.age + partialTick) * 1.4F);
            if (this.kind == TEMPEST) {
                double x = Mth.lerp(partialTick, naga.xo, naga.getX());
                double y = Mth.lerp(partialTick, naga.yo, naga.getY());
                double z = Mth.lerp(partialTick, naga.zo, naga.getZ());
                FxDraw.params(0.0F, 0.1F, 0.1F, 0.05F, 0.4F, 1.5F, 0.3F);
                VertexConsumer rune = FxBuffers.get(FxRenderTypes.add(RUNE));
                FxDraw.flat(rune, pose, x, y + 0.06D, z, 3.2F * FxEase.outBack(Math.min(1.0F, t * 3.0F)), (this.age + partialTick) * (0.05F + t * 0.2F),
                    GREEN_R, GREEN_G, GREEN_B, 0.85F * FxEase.window(t, 0.1F, 0.05F));
                FxDraw.params(0.0F, 0.1F, 0.0F, -0.8F, 0.5F, 2.0F, 0.8F);
                VertexConsumer wall = FxBuffers.get(FxRenderTypes.add(FxWorld.WALL));
                FxDraw.wallArc(wall, pose, x, y, z, 2.6F - grow * 1.0F, 1.6F - grow * 0.6F, 2.6F * grow, 0.0F, Mth.TWO_PI, FxWorld.segments(32), 4.0F,
                    (this.age + partialTick) * 0.05F, GREEN_R, GREEN_G, GREEN_B, 0.55F * grow * pulse, 0.0F);
                return;
            }
            Vec3 mouth = mouth(naga, partialTick);
            float size = 0.6F + grow * 1.4F;
            float r = VENOM_R;
            float g = VENOM_G;
            float b = VENOM_B;
            if (this.kind == BARRAGE) {
                size = 0.8F + grow * 1.8F;
                r = GREEN_R;
                g = GREEN_G;
                b = GREEN_B;
            }
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.9F, 1.0F, 0.0F);
            VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(FxWorld.GLOW));
            FxDraw.sprite(glow, pose, mouth.x, mouth.y, mouth.z, size * 2.2F * pulse, 0.0F, r, g, b, 0.6F);
            FxDraw.sprite(glow, pose, mouth.x, mouth.y, mouth.z, size * 0.8F * pulse, 0.0F, 1.0F, 1.0F, 0.9F, 0.9F);
            FxDraw.params(0.0F, 0.1F, 0.3F, 0.2F, 0.6F, 1.5F, 0.6F);
            VertexConsumer venom = FxBuffers.get(FxRenderTypes.add(VENOM));
            FxDraw.sprite(venom, pose, mouth.x, mouth.y, mouth.z, size * pulse, (this.age + partialTick) * 0.12F, r, g, b, 0.95F);
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.5F, 1.0F, 0.0F);
            VertexConsumer flare = FxBuffers.get(FxRenderTypes.add(FxWorld.FLARE));
            FxDraw.sprite(flare, pose, mouth.x, mouth.y, mouth.z, size * 2.6F * grow, (this.age + partialTick) * 0.05F, r, g, b, 0.5F * grow);
            if (this.kind == FANG) {
                double groundY = Mth.lerp(partialTick, naga.yo, naga.getY()) + 0.05D;
                FxDraw.params(0.0F, 0.1F, 0.1F, 0.05F, 0.3F, 1.5F, 0.3F);
                VertexConsumer rune = FxBuffers.get(FxRenderTypes.add(RUNE));
                FxDraw.flat(rune, pose, mouth.x, groundY, mouth.z, 2.0F * FxEase.outBack(Math.min(1.0F, t * 3.0F)), (this.age + partialTick) * 0.15F,
                    VENOM_R, VENOM_G, VENOM_B, 0.7F * FxEase.window(t, 0.1F, 0.1F));
            }
        }
    }

    private static final class SweepWarn extends FxWorld.Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float radius;
        private final float half;
        private final int warn;
        private final boolean twice;

        private SweepWarn(double x, double y, double z, float yaw, float radius, float half, int warn, boolean twice) {
            super(warn + 6);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.radius = radius;
            this.half = half;
            this.warn = warn;
            this.twice = twice;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float time = this.age + partialTick;
            float fill = FxEase.outCubic(Math.min(1.0F, time / this.warn));
            float fade = 1.0F;
            if (time > this.warn) {
                fade = Math.max(0.0F, 1.0F - (time - this.warn) / 6.0F);
            }
            float pulse = 0.75F + 0.25F * Mth.sin(time * 1.1F);
            float a0 = angleOf(this.yaw - this.half - 10.0F);
            float a1 = angleOf(this.yaw + this.half + 10.0F);
            int segments = FxWorld.segments(32);
            float r = GREEN_R;
            float g = GREEN_G;
            float b = GREEN_B;
            if (this.twice) {
                r = GOLD_R;
                g = GOLD_G;
                b = GOLD_B;
            }
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.15F, 0.2F, 1.2F, 0.5F);
            VertexConsumer fillType = FxBuffers.get(FxRenderTypes.add(FxWorld.SHOCK));
            FxDraw.arc(fillType, pose, this.x, this.y + 0.05D, this.z, 0.4F, this.radius * fill, a0, a1, segments, 3.0F, r, g, b,
                0.05F * fade, 0.55F * fade * pulse);
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.8F, 1.0F, 0.0F);
            VertexConsumer band = FxBuffers.get(FxRenderTypes.add(FxWorld.BAND));
            FxDraw.arc(band, pose, this.x, this.y + 0.06D, this.z, this.radius - 0.18F, this.radius + 0.18F, a0, a1, segments, 1.0F, r, g, b,
                0.9F * fade * fill, 0.9F * fade * fill);
            for (int i = 0; i < 2; i++) {
                float a = a0;
                if (i == 1) {
                    a = a1;
                }
                Vec3 from = new Vec3(this.x, this.y + 0.06D, this.z);
                Vec3 to = from.add(Mth.cos(a) * this.radius * fill, 0.0D, Mth.sin(a) * this.radius * fill);
                Vec3 side = new Vec3(-Mth.sin(a) * 0.15D, 0.0D, Mth.cos(a) * 0.15D);
                FxDraw.quad(band, pose, from.add(side), from.subtract(side), to.subtract(side), to.add(side), 0.0F, 0.0F, 1.0F, 1.0F, r, g, b, 0.8F * fade);
            }
        }
    }

    private static final class Husk extends FxWorld.Effect {
        private static final int HOLD = 70;
        private static final int CRUMBLE = 26;
        private final int nagaId;
        private final List<float[]> pieces = new ArrayList<>();
        private int burstAge = -1;

        private Husk(int nagaId) {
            super(HOLD + CRUMBLE);
            this.nagaId = nagaId;
        }

        private void burst() {
            if (this.burstAge < 0) {
                this.burstAge = this.age;
            }
        }

        @Override
        protected void tick() {
            if (this.burstAge < 0 && this.age >= HOLD - 1) {
                this.burstAge = this.age;
            }
            if (this.burstAge >= 0 && this.age > this.burstAge + CRUMBLE) {
                this.kill();
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || this.burstAge >= 0 || level.random.nextFloat() > FxWorld.quality()) {
                return;
            }
            float[] piece = this.pieces.get(level.random.nextInt(this.pieces.size()));
            FxParticles.add(FxWorld.GLOW, piece[0] + (level.random.nextDouble() - 0.5D) * piece[5], piece[1] + piece[5], piece[2] + (level.random.nextDouble() - 0.5D) * piece[5],
                    0.0D, 0.04D + level.random.nextDouble() * 0.04D, 0.0D, 14, 0.3F)
                .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.95D).flicker(0.5F, level.random.nextFloat() * 10.0F);
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            float time = this.age + partialTick;
            float crumble = 0.0F;
            if (this.burstAge >= 0) {
                crumble = Mth.clamp((time - this.burstAge) / CRUMBLE, 0.0F, 1.0F);
            }
            float charge = Mth.clamp(time / 30.0F, 0.0F, 1.0F);
            if (this.burstAge >= 0) {
                charge = 1.0F;
            }
            VertexConsumer base = FxBuffers.get(RenderType.entityTranslucent(HUSK));
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());
            for (int index = 0; index < this.pieces.size(); index++) {
                float[] piece = this.pieces.get(index);
                float size = Math.max(piece[5], 0.5F);
                float half = size * 0.5F + 0.02F;
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(piece[0], piece[1] + size * 0.5F, piece[2]));
                stack.pushPose();
                stack.translate(piece[0], piece[1] + size * 0.5F, piece[2]);
                stack.mulPose(Axis.YP.rotationDegrees(-piece[3]));
                stack.mulPose(Axis.XP.rotationDegrees(piece[4]));
                if (crumble <= 0.0F) {
                    float swell = 1.0F + 0.06F * charge * Mth.sin(time * 0.9F + index);
                    float edge = half * swell;
                    FxDraw.box(base, stack.last(), -edge, -edge, -edge, edge, edge, edge, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.85F, light);
                } else {
                    float pieceHalf = half * 0.5F * (1.0F - crumble * 0.5F);
                    float since = time - this.burstAge;
                    for (int i = 0; i < 8; i++) {
                        float ox = ((i & 1) - 0.5F) * half;
                        float oy = (((i >> 1) & 1) - 0.5F) * half;
                        float oz = (((i >> 2) & 1) - 0.5F) * half;
                        float dx = ox * 0.16F * since;
                        float dz = oz * 0.16F * since;
                        float dy = oy * 0.08F * since + 0.1F * since - 0.012F * since * since;
                        float tumble = since * (10.0F + ((index + i) % 3) * 5.0F);
                        if ((i + index) % 2 == 0) {
                            tumble = -tumble;
                        }
                        stack.pushPose();
                        stack.translate(ox + dx, oy + dy, oz + dz);
                        stack.mulPose(Axis.XP.rotationDegrees(tumble));
                        stack.mulPose(Axis.ZP.rotationDegrees(tumble * 0.6F));
                        float u0 = (i & 1) * 0.5F;
                        float v0 = ((i >> 1) & 1) * 0.5F;
                        FxDraw.box(base, stack.last(), -pieceHalf, -pieceHalf, -pieceHalf, pieceHalf, pieceHalf, pieceHalf, u0, v0, u0 + 0.5F, v0 + 0.5F,
                            1.0F, 1.0F, 1.0F, 0.8F * (1.0F - crumble), light);
                        stack.popPose();
                    }
                }
                stack.popPose();
            }
            float dissolve = crumble * 0.95F;
            float glowAlpha = (0.25F + 0.65F * charge) * (0.8F + 0.2F * Mth.sin(time * 1.3F));
            FxDraw.params(dissolve, 0.2F, 0.08F, 0.2F, 0.5F + charge * 0.5F, 1.0F, 0.6F);
            VertexConsumer shell = FxBuffers.get(FxRenderTypes.add(SCALES));
            for (float[] piece : this.pieces) {
                float size = Math.max(piece[5], 0.5F);
                float half = size * 0.5F + 0.06F + crumble * 0.6F;
                FxDraw.cube(shell, pose, piece[0], piece[1] + size * 0.5F - half, piece[2], half, half * 2.0F, (-piece[3]) * Mth.DEG_TO_RAD,
                    GREEN_R, GREEN_G, GREEN_B, glowAlpha * (1.0F - crumble * 0.5F));
            }
        }
    }

    private static final class MoltWave extends FxWorld.Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float maxRadius;

        private MoltWave(double x, double y, double z, float maxRadius, int life) {
            super(life + 6);
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxRadius = maxRadius;
        }

        private float radiusAt(float time) {
            int travel = this.life - 6;
            return Mth.lerp(Mth.clamp(time / travel, 0.0F, 1.0F), 0.5F, this.maxRadius);
        }

        @Override
        protected void tick() {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            RandomSource random = level.random;
            float radius = this.radiusAt(this.age);
            int count = FxWorld.scaled(6);
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double px = this.x + Math.cos(angle) * radius;
                double pz = this.z + Math.sin(angle) * radius;
                FxParticles.add(VENOM, px, this.y + 0.2D, pz, Math.cos(angle) * 0.08D, 0.12D + random.nextDouble() * 0.18D, Math.sin(angle) * 0.08D,
                        14 + random.nextInt(8), 0.14F + random.nextFloat() * 0.1F)
                    .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).gravity(0.035D).drag(0.96D).streak(1.4F).floor(this.y + 0.05D, 0.0D);
            }
            if (this.age % 3 == 0) {
                toxicSmoke(random, this.x + Math.cos(random.nextDouble() * 6.3D) * radius, this.y + 0.2D, this.z + Math.sin(random.nextDouble() * 6.3D) * radius,
                    FxWorld.scaled(1), 0.6D);
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float time = this.age + partialTick;
            float radius = this.radiusAt(time);
            float fade = 1.0F;
            if (time > this.life - 6) {
                fade = Math.max(0.0F, (this.life - time) / 6.0F);
            }
            int segments = FxWorld.segments(64);
            float circumference = Mth.TWO_PI * radius;
            float uRepeat = Math.max(4.0F, Math.round(circumference / 2.0F));
            FxDraw.params(0.0F, 0.1F, 0.4F, 0.0F, 0.6F, 2.0F, 0.6F);
            VertexConsumer ring = FxBuffers.get(FxRenderTypes.add(FxWorld.SHOCK));
            FxDraw.ring(ring, pose, this.x, this.y + 0.06D, this.z, Math.max(0.0F, radius - 1.6F), radius + 0.2F, segments, 0.0F, uRepeat * 0.5F,
                GREEN_R, GREEN_G, GREEN_B, 0.0F, 0.95F * fade);
            FxDraw.params(0.0F, 0.12F, 0.0F, -0.9F, 0.4F, 2.5F, 0.85F);
            VertexConsumer wall = FxBuffers.get(FxRenderTypes.add(FxWorld.WALL));
            FxDraw.wallArc(wall, pose, this.x, this.y, this.z, radius, radius + 0.25F, 1.0F, 0.0F, Mth.TWO_PI, segments, uRepeat, 0.0F,
                VENOM_R, VENOM_G, VENOM_B, 0.9F * fade, 0.0F);
            FxDraw.params(0.0F, 0.1F, 0.25F, 0.0F, 0.2F, 1.5F, 0.4F);
            VertexConsumer scales = FxBuffers.get(FxRenderTypes.alpha(SCALES));
            FxDraw.wallArc(scales, pose, this.x, this.y, this.z, radius - 0.05F, radius + 0.15F, 0.75F, 0.0F, Mth.TWO_PI, segments, uRepeat, 0.0F,
                DARK_R * 2.5F, DARK_G * 2.5F, DARK_B * 2.5F, 0.75F * fade, 0.0F);
        }
    }

    private static final class Crack extends FxWorld.Effect {
        private static final int POINT_LIFE = 70;
        private final int nagaId;
        private final List<double[]> points = new ArrayList<>();
        private boolean open = true;

        private Crack(int nagaId) {
            super(600);
            this.nagaId = nagaId;
        }

        private void add(double x, double y, double z) {
            this.points.add(new double[] {x, y + 0.06D, z, this.age});
        }

        private void close() {
            this.open = false;
        }

        @Override
        protected void tick() {
            if (this.points.isEmpty()) {
                return;
            }
            double newest = this.points.get(this.points.size() - 1)[3];
            if (!this.open || this.age - newest > 40) {
                this.open = false;
                if (this.age - newest > POINT_LIFE) {
                    this.kill();
                }
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            if (this.points.size() < 2) {
                return;
            }
            float time = this.age + partialTick;
            VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(CRACK));
            VertexConsumer dark = FxBuffers.get(FxRenderTypes.alpha(CRACK));
            float u = 0.0F;
            for (int i = 0; i < this.points.size() - 1; i++) {
                double[] p0 = this.points.get(i);
                double[] p1 = this.points.get(i + 1);
                double dx = p1[0] - p0[0];
                double dz = p1[2] - p0[2];
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 1.0E-3D) {
                    continue;
                }
                float age0 = (float) (time - p0[3]);
                float k = Mth.clamp(age0 / POINT_LIFE, 0.0F, 1.0F);
                float open = FxEase.outBack(Mth.clamp(age0 / 6.0F, 0.0F, 1.0F));
                float width = 0.9F * open * (1.0F - k * 0.3F);
                double sx = -dz / len * width * 0.5D;
                double sz = dx / len * width * 0.5D;
                float nextU = u + (float) (len / 3.0D);
                float a = 1.0F - FxEase.inCubic(k);
                float dissolve = Math.max(0.0F, (k - 0.5F) / 0.5F) * 0.9F;
                float hot = Math.max(0.0F, 1.0F - age0 / 12.0F);
                Vec3 q0 = new Vec3(p0[0] + sx, p0[1], p0[2] + sz);
                Vec3 q1 = new Vec3(p0[0] - sx, p0[1], p0[2] - sz);
                Vec3 q2 = new Vec3(p1[0] - sx, p1[1], p1[2] - sz);
                Vec3 q3 = new Vec3(p1[0] + sx, p1[1], p1[2] + sz);
                FxDraw.params(dissolve, 0.15F, 0.0F, 0.0F, 0.0F, 1.0F, 0.3F);
                FxDraw.quad(dark, pose, q0, q1, q2, q3, u, 0.0F, nextU, 1.0F, DARK_R, DARK_G, DARK_B, 0.85F * a);
                FxDraw.params(dissolve, 0.15F, -0.6F, 0.0F, 0.4F + hot * 0.6F, 1.5F, 0.5F);
                FxDraw.quad(glow, pose, q0.add(0.0D, 0.01D, 0.0D), q1.add(0.0D, 0.01D, 0.0D), q2.add(0.0D, 0.01D, 0.0D), q3.add(0.0D, 0.01D, 0.0D),
                    u, 0.0F, nextU, 1.0F, VENOM_R, VENOM_G, VENOM_B, (0.6F + hot * 0.4F) * a);
                u = nextU;
            }
            double[] head = this.points.get(this.points.size() - 1);
            if (this.open) {
                float pulse = 0.8F + 0.2F * Mth.sin(time * 1.6F);
                FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
                VertexConsumer light = FxBuffers.get(FxRenderTypes.add(FxWorld.GLOW));
                FxDraw.flat(light, pose, head[0], head[1] + 0.02D, head[2], 1.4F * pulse, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.8F);
                FxDraw.sprite(light, pose, head[0], head[1] + 0.4D, head[2], 1.6F * pulse, 0.0F, GREEN_R, GREEN_G, GREEN_B, 0.6F);
            }
        }
    }

    private static final class Spike extends FxWorld.Effect {
        private static final int RISE = 4;
        private static final int HOLD = 10;
        private static final int SINK = 12;
        private final double x;
        private final double y;
        private final double z;
        private final int warn;
        private final float yaw;
        private final float seed;
        private boolean erupted;

        private Spike(double x, double y, double z, int warn, float yaw, float seed) {
            super(warn + RISE + HOLD + SINK);
            this.x = x;
            this.y = y;
            this.z = z;
            this.warn = warn;
            this.yaw = yaw;
            this.seed = seed;
        }

        @Override
        protected void tick() {
            if (this.erupted || this.age < this.warn) {
                return;
            }
            this.erupted = true;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            RandomSource random = level.random;
            FxWorld.shockwave(this.x, this.y, this.z, 1.8F, 0.6F, 8, DUST_R * 2.0F, DUST_G * 2.0F, DUST_B * 1.6F).start(0.3F).hot(0.2F);
            debris(level, this.x, this.y, this.z, FxWorld.scaled(5), 0.9D);
            dust(random, this.x, this.y, this.z, FxWorld.scaled(3), 0.5D, 0.06D);
            sparks(random, this.x, this.y + 0.5D, this.z, FxWorld.scaled(5), 0.2D, GREEN_R, GREEN_G, GREEN_B);
            FxShake.at(this.x, this.y, this.z, 0.3F, 10.0F, 4);
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float time = this.age + partialTick;
            if (time < this.warn) {
                float w = time / this.warn;
                float pulse = 0.7F + 0.3F * Mth.sin(time * 1.8F);
                FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.6F, 1.0F, 0.0F);
                VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(FxWorld.GLOW));
                FxDraw.flat(glow, pose, this.x, this.y + 0.04D, this.z, 0.6F + w * 0.6F, 0.0F, VENOM_R, VENOM_G, VENOM_B, 0.7F * w * pulse);
                FxDraw.params(0.0F, 0.1F, 0.1F, 0.0F, 0.4F, 1.0F, 0.3F);
                VertexConsumer crack = FxBuffers.get(FxRenderTypes.add(CRACK));
                Vec3 d = forward(this.yaw + 90.0F).scale(0.8D + w * 0.4D);
                Vec3 s = forward(this.yaw).scale(0.25D);
                Vec3 c = new Vec3(this.x, this.y + 0.05D, this.z);
                FxDraw.quad(crack, pose, c.subtract(d).add(s), c.subtract(d).subtract(s), c.add(d).subtract(s), c.add(d).add(s), 0.0F, 0.0F, 1.0F, 1.0F,
                    GREEN_R, GREEN_G, GREEN_B, 0.8F * w);
                return;
            }
            float local = time - this.warn;
            float height;
            float dissolve = 0.0F;
            if (local < RISE) {
                height = FxEase.outBack(local / RISE);
            } else if (local < RISE + HOLD) {
                height = 1.0F;
            } else {
                float k = (local - RISE - HOLD) / SINK;
                height = 1.0F - FxEase.inCubic(k) * 0.6F;
                dissolve = k * 0.95F;
            }
            float tall = 2.1F + this.seed * 0.6F;
            float tiltX = (this.seed - 0.5F) * 0.5F;
            float tiltZ = (this.seed * 3.7F % 1.0F - 0.5F) * 0.5F;
            double topX = this.x + tiltX;
            double topY = this.y - 0.3D + tall * height;
            double topZ = this.z + tiltZ;
            FxDraw.params(dissolve, 0.18F, 0.0F, -0.2F, 0.15F, 1.5F, 0.4F);
            VertexConsumer body = FxBuffers.get(FxRenderTypes.alpha(SCALES));
            FxDraw.spike(body, pose, this.x, this.y - 0.3D, this.z, topX, topY, topZ, 0.42F, 5, this.seed * 6.0F, DARK_R * 2.6F, DARK_G * 2.6F, DARK_B * 2.6F, 0.95F);
            FxDraw.params(dissolve, 0.18F, 0.0F, -0.6F, 0.7F, 2.0F, 0.6F);
            VertexConsumer edge = FxBuffers.get(FxRenderTypes.add(SCALES));
            FxDraw.spike(edge, pose, this.x, this.y - 0.3D, this.z, topX, topY + 0.05D, topZ, 0.46F, 5, this.seed * 6.0F, GREEN_R, GREEN_G, GREEN_B, 0.55F);
            for (int i = 0; i < 2; i++) {
                float side = i * 2 - 1;
                double bx = this.x + forward(this.yaw + 90.0F).x * 0.5D * side;
                double bz = this.z + forward(this.yaw + 90.0F).z * 0.5D * side;
                FxDraw.spike(body, pose, bx, this.y - 0.2D, bz, bx + side * 0.35D * forward(this.yaw + 90.0F).x, this.y - 0.2D + 1.1D * height,
                    bz + side * 0.35D * forward(this.yaw + 90.0F).z, 0.24F, 4, this.seed * 4.0F + i, DARK_R * 2.6F, DARK_G * 2.6F, DARK_B * 2.6F, 0.9F);
            }
            if (local < RISE + 4) {
                float flash = 1.0F - local / (RISE + 4);
                FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
                VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(FxWorld.GLOW));
                FxDraw.sprite(glow, pose, topX, topY, topZ, 1.6F * flash, 0.0F, VENOM_R, VENOM_G, VENOM_B, flash);
            }
        }
    }

    private static final class Jaw extends FxWorld.Effect {
        private static final int CLOSE = 6;
        private static final int HOLD = 10;
        private static final int SINK = 14;
        private static final int TEETH = 5;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private boolean bitten;

        private Jaw(double x, double y, double z, float yaw) {
            super(CLOSE + HOLD + SINK);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }

        @Override
        protected void tick() {
            if (this.bitten || this.age < CLOSE - 1) {
                return;
            }
            this.bitten = true;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            RandomSource random = level.random;
            FxWorld.flash(this.x, this.y + 1.4D, this.z, 4.5F, 10, VENOM_R, VENOM_G, VENOM_B).flare(2.2F);
            FxWorld.shockwave(this.x, this.y, this.z, 5.0F, 1.6F, 14, VENOM_R, VENOM_G, VENOM_B).start(1.0F).wall(1.4F).hot(0.8F);
            FxWorld.shockwave(this.x, this.y, this.z, 3.2F, 1.0F, 10, DUST_R * 2.0F, DUST_G * 2.0F, DUST_B * 1.6F).start(0.6F);
            FxShake.at(this.x, this.y, this.z, 1.4F, 20.0F, 12);
            FxScreen.pulse(this.x, this.y, this.z, VENOM_R, VENOM_G, VENOM_B, 0.7F, 10.0F, 14);
            debris(level, this.x, this.y, this.z, FxWorld.scaled(16), 1.4D);
            dust(random, this.x, this.y, this.z, FxWorld.scaled(10), 1.5D, 0.12D);
            droplets(random, this.x, this.y + 1.4D, this.z, FxWorld.scaled(20), 0.2D, 0.35D, this.y);
            sparks(random, this.x, this.y + 1.2D, this.z, FxWorld.scaled(14), 0.4D, VENOM_R, VENOM_G, VENOM_B);
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float time = this.age + partialTick;
            float close = FxEase.outBack(Mth.clamp(time / CLOSE, 0.0F, 1.0F));
            float rise = FxEase.outCubic(Mth.clamp(time / (CLOSE * 0.6F), 0.0F, 1.0F));
            float dissolve = 0.0F;
            float sink = 0.0F;
            if (time > CLOSE + HOLD) {
                float k = (time - CLOSE - HOLD) / SINK;
                dissolve = k * 0.95F;
                sink = FxEase.inCubic(k);
            }
            Vec3 forwardDir = forward(this.yaw);
            Vec3 sideDir = forward(this.yaw + 90.0F);
            VertexConsumer body = FxBuffers.get(FxRenderTypes.alpha(SCALES));
            VertexConsumer edge = FxBuffers.get(FxRenderTypes.add(SCALES));
            for (int jaw = 0; jaw < 2; jaw++) {
                float sideSign = jaw * 2 - 1;
                for (int i = 0; i < TEETH; i++) {
                    float k = (i - (TEETH - 1) * 0.5F) / ((TEETH - 1) * 0.5F);
                    double along = k * 1.7D;
                    double out = 2.2D - (1.0D - k * k) * 0.5D;
                    double baseX = this.x + forwardDir.x * along + sideDir.x * out * sideSign;
                    double baseZ = this.z + forwardDir.z * along + sideDir.z * out * sideSign;
                    float tall = (float) (2.6D + (1.0D - k * k) * 0.9D);
                    double lean = Mth.lerp(close, 0.2D, out * 0.85D);
                    double tipX = baseX - sideDir.x * lean * sideSign;
                    double tipZ = baseZ - sideDir.z * lean * sideSign;
                    double baseY = this.y - 0.4D - sink * 1.5D;
                    double tipY = baseY + tall * rise * (1.0F - sink * 0.3F);
                    FxDraw.params(dissolve, 0.18F, 0.0F, -0.2F, 0.2F, 1.5F, 0.4F);
                    FxDraw.spike(body, pose, baseX, baseY, baseZ, tipX, tipY, tipZ, 0.42F, 5, i + jaw * 2.0F, DARK_R * 2.6F, DARK_G * 2.6F, DARK_B * 2.6F, 0.95F);
                    FxDraw.params(dissolve, 0.18F, 0.0F, -0.8F, 0.85F, 2.0F, 0.6F);
                    FxDraw.spike(edge, pose, baseX, baseY, baseZ, tipX, tipY + 0.06D, tipZ, 0.46F, 5, i + jaw * 2.0F, VENOM_R, VENOM_G, VENOM_B, 0.6F);
                }
            }
            if (time < CLOSE + 4) {
                float glowA = Mth.clamp(time / CLOSE, 0.0F, 1.0F) * Math.max(0.0F, 1.0F - (time - CLOSE) / 4.0F);
                FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
                VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(FxWorld.GLOW));
                FxDraw.flat(glow, pose, this.x, this.y + 0.05D, this.z, 3.0F, 0.0F, VENOM_R, VENOM_G, VENOM_B, glowA);
            }
        }
    }

    private static final class TempestWave extends FxWorld.Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float base;
        private final float speed;
        private final float maxRadius;
        private final float gapHalf;
        private final float start;

        private TempestWave(double x, double y, double z, float base, float speed, float maxRadius, float gapHalf, float start) {
            super(Math.max(4, Math.round((maxRadius - start) / speed)) + 1);
            this.x = x;
            this.y = y;
            this.z = z;
            this.base = base;
            this.speed = speed;
            this.maxRadius = maxRadius;
            this.gapHalf = gapHalf;
            this.start = start;
        }

        @Override
        protected void tick() {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            RandomSource random = level.random;
            float radius = this.start + this.age * this.speed;
            for (int g = 0; g < 3; g++) {
                if (random.nextFloat() > FxWorld.quality()) {
                    continue;
                }
                for (int e = 0; e < 2; e++) {
                    float side = e * 2 - 1;
                    float a = (this.base + g * 120.0F + side * this.gapHalf) * Mth.DEG_TO_RAD;
                    FxParticles.add(FxWorld.SPARK, this.x + Mth.cos(a) * radius, this.y + random.nextDouble() * 1.2D, this.z + Mth.sin(a) * radius,
                            Mth.cos(a) * this.speed, 0.04D, Mth.sin(a) * this.speed, 6, 0.12F)
                        .color(1.0F, 1.0F, 0.85F).fadeTo(GOLD_R, GOLD_G, GOLD_B).drag(0.9D).streak(2.0F).shade(0.8F, 0.0F, 1.0F);
                }
            }
            if (random.nextFloat() < FxWorld.quality()) {
                float a = random.nextFloat() * Mth.TWO_PI;
                FxParticles.add(FLAKE, this.x + Mth.cos(a) * radius, this.y + 0.3D + random.nextDouble() * 0.8D, this.z + Mth.sin(a) * radius,
                        Mth.cos(a) * this.speed * 0.8D, 0.05D, Mth.sin(a) * this.speed * 0.8D, 12, 0.24F)
                    .color(GREEN_R, GREEN_G, GREEN_B).fadeTo(DARK_R * 2.0F, DARK_G * 2.0F, DARK_B * 2.0F).drag(0.88D).spin(a, 0.5F).shade(0.4F, 0.0F, 1.0F);
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float time = this.age + partialTick;
            float radius = this.start + time * this.speed;
            float fade = Math.min(1.0F, (this.maxRadius + 0.5F - radius) / 3.0F);
            if (fade <= 0.0F) {
                return;
            }
            float appear = Math.min(1.0F, time / 4.0F);
            int segments = FxWorld.segments(20);
            VertexConsumer wall = FxBuffers.get(FxRenderTypes.add(FxWorld.WALL));
            VertexConsumer scales = FxBuffers.get(FxRenderTypes.add(SCALES));
            VertexConsumer band = FxBuffers.get(FxRenderTypes.add(FxWorld.BAND));
            for (int g = 0; g < 3; g++) {
                float a0 = (this.base + g * 120.0F + this.gapHalf) * Mth.DEG_TO_RAD;
                float a1 = (this.base + (g + 1) * 120.0F - this.gapHalf) * Mth.DEG_TO_RAD;
                float arcLength = (a1 - a0) * radius;
                float uRepeat = Math.max(1.0F, Math.round(arcLength / 1.5F));
                FxDraw.params(0.0F, 0.1F, 0.0F, -0.8F, 0.55F, 2.5F, 0.7F);
                FxDraw.wallArc(wall, pose, this.x, this.y, this.z, radius, radius + 0.3F, 1.2F * appear, a0, a1, segments, uRepeat, 0.0F,
                    GREEN_R, GREEN_G, GREEN_B, 0.95F * fade, 0.05F * fade);
                FxDraw.params(0.0F, 0.1F, -0.5F, 0.0F, 0.3F, 1.0F, 0.3F);
                FxDraw.wallArc(scales, pose, this.x, this.y, this.z, radius - 0.04F, radius + 0.2F, 0.9F * appear, a0, a1, segments, uRepeat, 0.0F,
                    VENOM_R, VENOM_G, VENOM_B, 0.6F * fade, 0.0F);
                FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.9F, 1.0F, 0.0F);
                FxDraw.arc(band, pose, this.x, this.y + 0.05D, this.z, radius - 0.25F, radius + 0.25F, a0, a1, segments, 1.0F,
                    1.0F, 1.0F, 0.85F, 0.9F * fade, 0.9F * fade);
            }
        }
    }

    private static final class Debris extends FxWorld.Effect {
        private final TextureAtlasSprite sprite;
        private final double[] pos;
        private final double[] prev;
        private final double[] vel;
        private final float[] size;
        private final float[] spin;
        private final int[] uv;
        private final double floor;

        private Debris(RandomSource random, TextureAtlasSprite sprite, double x, double y, double z, int count, double power) {
            super(28);
            this.sprite = sprite;
            this.pos = new double[count * 3];
            this.prev = new double[count * 3];
            this.vel = new double[count * 3];
            this.size = new float[count];
            this.spin = new float[count];
            this.uv = new int[count * 2];
            this.floor = y;
            for (int i = 0; i < count; i++) {
                this.pos[i * 3] = x + (random.nextDouble() - 0.5D) * 0.8D;
                this.pos[i * 3 + 1] = y + 0.05D;
                this.pos[i * 3 + 2] = z + (random.nextDouble() - 0.5D) * 0.8D;
                this.vel[i * 3] = (random.nextDouble() - 0.5D) * 0.24D * power;
                this.vel[i * 3 + 1] = (0.2D + random.nextDouble() * 0.24D) * power;
                this.vel[i * 3 + 2] = (random.nextDouble() - 0.5D) * 0.24D * power;
                this.size[i] = 0.07F + random.nextFloat() * 0.09F;
                this.spin[i] = (random.nextFloat() - 0.5F) * 40.0F;
                this.uv[i * 2] = random.nextInt(12);
                this.uv[i * 2 + 1] = random.nextInt(12);
            }
            System.arraycopy(this.pos, 0, this.prev, 0, this.pos.length);
        }

        @Override
        protected void tick() {
            System.arraycopy(this.pos, 0, this.prev, 0, this.pos.length);
            for (int i = 0; i < this.size.length; i++) {
                this.pos[i * 3] += this.vel[i * 3];
                this.pos[i * 3 + 1] += this.vel[i * 3 + 1];
                this.pos[i * 3 + 2] += this.vel[i * 3 + 2];
                this.vel[i * 3] *= 0.96D;
                this.vel[i * 3 + 1] = this.vel[i * 3 + 1] * 0.98D - 0.04D;
                this.vel[i * 3 + 2] *= 0.96D;
                if (this.pos[i * 3 + 1] < this.floor) {
                    this.pos[i * 3 + 1] = this.floor;
                    this.vel[i * 3 + 1] = -this.vel[i * 3 + 1] * 0.25D;
                    this.vel[i * 3] *= 0.5D;
                    this.vel[i * 3 + 2] *= 0.5D;
                }
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            VertexConsumer consumer = FxBuffers.get(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());
            float age = this.age + partialTick;
            float shrink = Mth.clamp(1.0F - (t - 0.6F) / 0.4F, 0.0F, 1.0F);
            for (int i = 0; i < this.size.length; i++) {
                float s = this.size[i] * shrink;
                if (s <= 0.0F) {
                    continue;
                }
                double x = Mth.lerp(partialTick, this.prev[i * 3], this.pos[i * 3]);
                double y = Mth.lerp(partialTick, this.prev[i * 3 + 1], this.pos[i * 3 + 1]);
                double z = Mth.lerp(partialTick, this.prev[i * 3 + 2], this.pos[i * 3 + 2]);
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(x, y + 0.1D, z));
                stack.pushPose();
                stack.translate(x, y + s, z);
                stack.mulPose(Axis.YP.rotationDegrees(age * this.spin[i]));
                stack.mulPose(Axis.XP.rotationDegrees(age * this.spin[i] * 0.7F));
                float u0 = this.sprite.getU(this.uv[i * 2]);
                float u1 = this.sprite.getU(this.uv[i * 2] + 4);
                float v0 = this.sprite.getV(this.uv[i * 2 + 1]);
                float v1 = this.sprite.getV(this.uv[i * 2 + 1] + 4);
                FxDraw.box(consumer, stack.last(), -s, -s, -s, s, s, s, u0, v0, u1, v1, 1.0F, 1.0F, 1.0F, 1.0F, light);
                stack.popPose();
            }
        }
    }

    private static final class Constrict extends FxWorld.Effect {
        private static final int FADE = 16;
        private final int nagaId;
        private final double x;
        private final double y;
        private final double z;
        private final float startRadius;
        private final float endRadius;
        private final int duration;
        private final int warmup;
        private final int weakIndex;
        private int endAge = -1;
        private boolean broken;
        private int flash;

        private Constrict(int nagaId, double x, double y, double z, float startRadius, float endRadius, int duration, int warmup, int weakIndex) {
            super(duration + 200);
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

        private float radiusAt(float age) {
            float progress = Mth.clamp(age / this.duration, 0.0F, 1.0F);
            return Mth.lerp(progress, this.startRadius, this.endRadius);
        }

        private void end(boolean broken) {
            if (this.endAge >= 0) {
                return;
            }
            this.endAge = this.age;
            this.broken = broken;
            if (!broken) {
                return;
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            RandomSource random = level.random;
            float radius = this.radiusAt(this.age);
            FxShake.at(this.x, this.y, this.z, 1.3F, 24.0F, 14);
            FxScreen.pulse(this.x, this.y, this.z, GOLD_R, GOLD_G, GOLD_B, 0.8F, 16.0F, 18);
            FxWorld.shockwave(this.x, this.y, this.z, radius * 2.2F, 1.6F, 16, GOLD_R, GOLD_G, GOLD_B).start(radius).wall(1.6F).hot(0.9F);
            Vec3 weak = this.weakPos(1.0F);
            if (weak != null) {
                FxWorld.flash(weak.x, weak.y, weak.z, 6.0F, 14, GOLD_R, GOLD_G, GOLD_B).flare(2.6F);
                FxWorld.pillar(weak.x, weak.y - 1.0D, weak.z, 1.4F, 8.0F, 18, GOLD_R, GOLD_G, GOLD_B);
                sparks(random, weak.x, weak.y, weak.z, FxWorld.scaled(30), 0.5D, GOLD_R, GOLD_G, GOLD_B);
            }
            int count = FxWorld.scaled(48);
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2.0D * i / count;
                double speed = 0.12D + random.nextDouble() * 0.14D;
                FxParticles.add(FLAKE, this.x + Math.cos(angle) * radius, this.y + 0.2D + random.nextDouble() * 2.4D, this.z + Math.sin(angle) * radius,
                        Math.cos(angle) * speed, 0.1D + random.nextDouble() * 0.15D, Math.sin(angle) * speed, 26 + random.nextInt(14), 0.3F)
                    .color(1.0F, 0.92F, 0.6F).fadeTo(GOLD_R, GOLD_G * 0.6F, GOLD_B * 0.3F).gravity(0.012D).drag(0.94D)
                    .spin(random.nextFloat() * 6.0F, (random.nextFloat() - 0.5F) * 0.5F).shade(0.6F, 0.0F, 1.0F).dissolve(0.5F);
            }
        }

        private Vec3 weakPos(float partialTick) {
            if (this.weakIndex < 0) {
                return null;
            }
            Naga naga = naga(this.nagaId);
            if (naga == null) {
                return null;
            }
            PartEntity<?>[] parts = naga.getParts();
            if (parts == null || this.weakIndex >= parts.length) {
                return null;
            }
            PartEntity<?> part = parts[this.weakIndex];
            if (part.getBbWidth() <= 0.0F) {
                return null;
            }
            return new Vec3(Mth.lerp(partialTick, part.xo, part.getX()), Mth.lerp(partialTick, part.yo, part.getY()) + part.getBbHeight() * 0.5D,
                Mth.lerp(partialTick, part.zo, part.getZ()));
        }

        @Override
        protected void tick() {
            if (this.flash > 0) {
                this.flash--;
            }
            if (this.endAge >= 0 && this.age > this.endAge + FADE) {
                this.kill();
                return;
            }
            if (this.endAge < 0 && this.age > this.duration + 40) {
                this.end(false);
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || this.endAge >= 0 || this.age < this.warmup) {
                return;
            }
            RandomSource random = level.random;
            float radius = this.radiusAt(this.age);
            int count = FxWorld.scaled(3);
            for (int i = 0; i < count; i++) {
                float a = random.nextFloat() * Mth.TWO_PI;
                FxParticles.add(FLAKE, this.x + Mth.cos(a) * radius, this.y + random.nextDouble() * 0.3D, this.z + Mth.sin(a) * radius,
                        -Mth.sin(a) * 0.08D, 0.08D + random.nextDouble() * 0.08D, Mth.cos(a) * 0.08D, 18, 0.22F)
                    .color(VENOM_R, VENOM_G, VENOM_B).fadeTo(GREEN_R, GREEN_G, GREEN_B).drag(0.94D).spin(a, 0.3F).shade(0.5F, 0.0F, 1.0F);
            }
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float age = this.age + partialTick;
            float radius = this.radiusAt(age);
            if (this.endAge >= 0) {
                radius = this.radiusAt(this.endAge);
            }
            float rise = FxEase.outBack(Mth.clamp(age / this.warmup, 0.0F, 1.0F));
            float height = 2.8F * rise;
            float fade = 1.0F;
            float dissolve = 0.0F;
            if (this.endAge >= 0) {
                float k = Mth.clamp((age - this.endAge) / FADE, 0.0F, 1.0F);
                fade = 1.0F - k;
                dissolve = k * 0.95F;
                if (this.broken) {
                    height += (age - this.endAge) * 0.35F;
                }
            }
            if (fade <= 0.0F) {
                return;
            }
            boolean active = age >= this.warmup;
            float r = GREEN_R;
            float g = GREEN_G;
            float b = GREEN_B;
            float alpha = 0.45F;
            if (active) {
                r = VENOM_R;
                g = VENOM_G;
                b = VENOM_B;
                alpha = 0.9F;
            }
            if (this.broken) {
                r = GOLD_R;
                g = GOLD_G;
                b = GOLD_B;
            }
            int segments = FxWorld.segments(64);
            float uRepeat = Math.max(4.0F, Math.round(Mth.TWO_PI * radius / 1.6F));
            FxDraw.params(dissolve, 0.15F, 0.05F, 0.05F, 0.2F, 1.2F, 0.4F);
            VertexConsumer fill = FxBuffers.get(FxRenderTypes.add(RUNE));
            FxDraw.flat(fill, pose, this.x, this.y + 0.04D, this.z, radius * 1.05F, age * 0.012F, r, g, b, 0.35F * fade);
            FxDraw.params(dissolve, 0.15F, 0.0F, -0.7F, 0.5F, 2.5F, 0.8F);
            VertexConsumer wall = FxBuffers.get(FxRenderTypes.add(FxWorld.WALL));
            FxDraw.wall(wall, pose, this.x, this.y, this.z, radius, height, segments, uRepeat, 0.0F, r, g, b, alpha * fade, 0.0F);
            FxDraw.params(dissolve, 0.15F, 0.12F, -0.05F, 0.15F, 1.0F, 0.35F);
            VertexConsumer scaleWall = FxBuffers.get(FxRenderTypes.alpha(SCALES));
            FxDraw.wall(scaleWall, pose, this.x, this.y, this.z, radius + 0.06F, height * 0.75F, segments, uRepeat, age * 0.01F,
                DARK_R * 2.6F, DARK_G * 2.6F, DARK_B * 2.6F, 0.75F * fade, 0.0F);
            FxDraw.params(dissolve, 0.15F, 0.0F, 0.0F, 0.9F, 1.0F, 0.0F);
            VertexConsumer band = FxBuffers.get(FxRenderTypes.add(FxWorld.BAND));
            FxDraw.ring(band, pose, this.x, this.y + 0.05D, this.z, radius - 0.35F, radius + 0.35F, segments, 0.0F, 1.0F, r, g, b, fade, fade);
            if (active && age < this.warmup + 8.0F && this.endAge < 0) {
                float flashA = 1.0F - (age - this.warmup) / 8.0F;
                FxDraw.params(0.0F, 0.1F, 0.0F, -1.0F, 1.0F, 2.0F, 0.6F);
                VertexConsumer burst = FxBuffers.get(FxRenderTypes.add(FxWorld.WALL));
                FxDraw.wall(burst, pose, this.x, this.y, this.z, radius - 0.05F, height * 1.4F, segments, uRepeat, 0.0F, 1.0F, 1.0F, 0.9F, flashA, 0.0F);
            }
            if (this.endAge >= 0) {
                return;
            }
            Vec3 weak = this.weakPos(partialTick);
            Naga naga = naga(this.nagaId);
            if (weak == null || naga == null) {
                return;
            }
            PartEntity<?> part = naga.getParts()[this.weakIndex];
            float yaw = Mth.rotLerp(partialTick, part.yRotO, part.getYRot());
            float half = part.getBbWidth() * 0.5F + 0.12F;
            float pulse = 0.6F + 0.3F * Mth.sin(age * 0.35F);
            float grow = 1.0F;
            if (this.flash > 0) {
                pulse = 1.0F;
                grow = 1.0F + this.flash * 0.05F;
            }
            float edge = half * grow;
            FxDraw.params(0.0F, 0.1F, 0.15F, 0.1F, 0.6F, 1.0F, 0.5F);
            VertexConsumer shell = FxBuffers.get(FxRenderTypes.add(SCALES));
            FxDraw.cube(shell, pose, weak.x, weak.y - edge, weak.z, edge, edge * 2.0F, -yaw * Mth.DEG_TO_RAD, GOLD_R, GOLD_G, GOLD_B, pulse);
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.8F, 1.0F, 0.0F);
            VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(FxWorld.GLOW));
            FxDraw.sprite(glow, pose, weak.x, weak.y, weak.z, half * 6.0F * grow, 0.0F, GOLD_R, GOLD_G, GOLD_B, pulse * 0.6F);
            FxDraw.params(0.0F, 0.1F, -1.5F, 0.0F, 0.8F, 1.5F, 0.5F);
            VertexConsumer beam = FxBuffers.get(FxRenderTypes.add(FxWorld.BAND));
            FxDraw.beam(beam, pose, new Vec3(weak.x, weak.y, weak.z), new Vec3(weak.x, weak.y + 7.0D, weak.z), 0.45F * grow, 0.1F, 0.0F, 2.0F,
                GOLD_R, GOLD_G, GOLD_B, pulse * 0.85F, 0.0F);
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.6F, 1.0F, 0.0F);
            VertexConsumer flare = FxBuffers.get(FxRenderTypes.add(FxWorld.FLARE));
            FxDraw.sprite(flare, pose, weak.x, weak.y + half + 0.5D, weak.z, (1.6F + 0.4F * Mth.sin(age * 0.5F)) * grow, age * 0.04F, 1.0F, 0.95F, 0.75F, pulse);
            if (this.flash > 0) {
                FxDraw.sprite(flare, pose, weak.x, weak.y, weak.z, half * 10.0F * this.flash / 8.0F, 0.0F, 1.0F, 1.0F, 1.0F, this.flash / 8.0F);
            }
        }
    }
}
