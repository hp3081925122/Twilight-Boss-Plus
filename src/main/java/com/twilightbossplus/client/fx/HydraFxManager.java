package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHead;
import twilightforest.entity.boss.HydraMortar;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HydraFxManager {
    private static final ResourceLocation FLAME = tex("flame");
    private static final ResourceLocation FIRE_WALL = tex("fire_wall");
    private static final ResourceLocation ASH_FAN = tex("ash_fan");
    private static final ResourceLocation FIRE = tex("px/fire");
    private static final ResourceLocation EMBER = tex("px/ember");
    private static final ResourceLocation DROP = tex("px/drop");
    private static final ResourceLocation BUBBLE = tex("px/bubble");
    private static final ResourceLocation SLAG = tex("px/slag");
    private static final ResourceLocation SCORCH = tex("px/scorch");
    private static final ResourceLocation SCORCH_GLOW = tex("px/scorch_glow");
    private static final ResourceLocation LAVA_POOL = tex("px/lava_pool");
    private static final ResourceLocation HEAT_RING = tex("px/heat_ring");
    private static final ResourceLocation FANGS = tex("px/fangs");
    private static final ResourceLocation LAVA = tex("px/lava");
    private static final ResourceLocation LAVA_GLOW = tex("px/lava_glow");

    private static final float GOLD_R = 1.0F;
    private static final float GOLD_G = 0.71F;
    private static final float GOLD_B = 0.18F;
    private static final float ORANGE_R = 1.0F;
    private static final float ORANGE_G = 0.416F;
    private static final float ORANGE_B = 0.102F;
    private static final float EMBER_R = 0.639F;
    private static final float EMBER_G = 0.149F;
    private static final float EMBER_B = 0.106F;
    private static final float CORE_R = 1.0F;
    private static final float CORE_G = 0.949F;
    private static final float CORE_B = 0.784F;

    private static final double MOUTH_DIST = 3.5D;
    private static final double FLAME_RANGE = 30.0D;
    private static final int FLAME_SEGMENTS = 8;
    private static final int SECTOR_FADE = 6;
    private static final int WAVE_FADE = 8;
    private static final int GEYSER_FADE = 6;
    private static final int FLASH_LIFE = 12;
    private static final int MORTAR_FUSE = 80;
    private static final int SCORCH_BUBBLES = 3;
    private static final float BUBBLE_CYCLE = 18.0F;
    private static final int MAX_SCORCHES = 160;
    private static final double WAVE_HEIGHT = 2.6D;
    private static final double PARTICLE_RANGE_SQR = 64.0D * 64.0D;
    private static final double SCORCH_RANGE_SQR = 40.0D * 40.0D;

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
        RandomSource random = level.random;
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
                SCORCHES.add(new Scorch(data[0], data[1], data[2], data[3] * 0.9F, 80, false));
            }
            slagBurst(random, data[0], data[1], data[2], data[3]);
            FxShake.at(data[0], data[1], data[2], 0.6F, data[3] * 5.0F + 8.0F, 10);
        } else if (type == HydraFxPacket.WAVE && data.length >= 5) {
            WAVES.add(new Wave(data[0], data[1], data[2], data[3], Math.max(1, (int) data[4])));
            FxShake.at(data[0], data[1], data[2], 0.5F, data[3] + 10.0F, 10);
            for (int i = 0; i < 14; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.12D + random.nextDouble() * 0.12D;
                FxParticles.glow(FIRE, data[0] + Math.cos(angle) * 0.8D, data[1] + 0.3D, data[2] + Math.sin(angle) * 0.8D,
                        Math.cos(angle) * speed, 0.06D + random.nextDouble() * 0.06D, Math.sin(angle) * speed, 14 + random.nextInt(8), 1.0F + random.nextFloat() * 0.6F)
                    .fadeTo(0.6F, 0.2F, 0.1F).grow(2.0F).drag(0.88D).gravity(-0.004D);
            }
        } else if (type == HydraFxPacket.GEYSER && data.length >= 8) {
            List<Vec3> points = new ArrayList<>();
            for (int i = 5; i + 2 < data.length; i += 3) {
                points.add(new Vec3(data[i], data[i + 1], data[i + 2]));
            }
            GEYSERS.add(new Geyser(points, (int) data[0], (int) data[1], data[2], data[3], (int) data[4]));
        }
    }

    private static void slagBurst(RandomSource random, double x, double y, double z, float radius) {
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.08D + random.nextDouble() * 0.12D * radius;
            FxParticles.glow(FIRE, x + Math.cos(angle) * 0.4D, y + 0.3D, z + Math.sin(angle) * 0.4D,
                    Math.cos(angle) * speed, 0.08D + random.nextDouble() * 0.12D, Math.sin(angle) * speed, 14 + random.nextInt(8), 0.8F + random.nextFloat() * 0.5F)
                .fadeTo(0.55F, 0.2F, 0.1F).grow(1.8F).drag(0.86D).gravity(-0.004D);
        }
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.12D + random.nextDouble() * 0.22D;
            FxParticles.glow(EMBER, x, y + 0.3D, z, Math.cos(angle) * speed, 0.25D + random.nextDouble() * 0.3D, Math.sin(angle) * speed,
                    18 + random.nextInt(12), 0.14F + random.nextFloat() * 0.08F)
                .gravity(0.04D).drag(0.97D).floor(y + 0.05D, 0.35D).flicker(0.3F, random.nextFloat() * 10.0F);
        }
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.1D + random.nextDouble() * 0.15D;
            FxParticles.glow(DROP, x, y + 0.4D, z, Math.cos(angle) * speed, 0.3D + random.nextDouble() * 0.2D, Math.sin(angle) * speed,
                    22 + random.nextInt(8), 0.22F)
                .gravity(0.05D).drag(0.98D).streak(1.4F).floor(y + 0.05D, 0.0D);
        }
        for (int i = 0; i < 6; i++) {
            FxParticles.soft(FxTextures.SMOKE, x + (random.nextDouble() - 0.5D) * radius, y + 0.6D, z + (random.nextDouble() - 0.5D) * radius,
                    (random.nextDouble() - 0.5D) * 0.04D, 0.05D + random.nextDouble() * 0.04D, (random.nextDouble() - 0.5D) * 0.04D,
                    34 + random.nextInt(16), 1.0F + random.nextFloat() * 0.6F)
                .color(0.28F, 0.22F, 0.2F).fadeTo(0.12F, 0.1F, 0.1F).alpha(0.75F).grow(3.2F).drag(0.93D).fade(0.1F, 1.5F)
                .spin(random.nextFloat() * 6.0F, 0.02F);
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

    private static double flameReach(Vec3 mouth, @Nullable Vec3 hit, float age) {
        double length = FLAME_RANGE - MOUTH_DIST;
        if (hit != null) {
            length = Math.max(1.0D, hit.distanceTo(mouth));
        }
        return Math.min(length, 2.0D + age * 4.0D);
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
        long time = level.getGameTime();

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
                    track = new int[]{state, 0};
                    HEAD_TRACK.put(head.getId(), track);
                } else {
                    track[1]++;
                }
                if (!decorative || FxDraw.cameraDistanceSqr(head.getX(), head.getY(), head.getZ()) > PARTICLE_RANGE_SQR) {
                    continue;
                }
                int age = track[1];
                if (state == HydraHeads.DYING) {
                    FxParticles.soft(FxTextures.SMOKE, head.getX() + (random.nextDouble() - 0.5D), head.getY() + 1.0D, head.getZ() + (random.nextDouble() - 0.5D),
                            0.0D, 0.05D + random.nextDouble() * 0.04D, 0.0D, 30 + random.nextInt(10), 1.0F)
                        .color(0.25F, 0.2F, 0.18F).alpha(0.7F).grow(2.6F).drag(0.94D).fade(0.1F, 1.5F);
                    FxParticles.glow(EMBER, head.getX(), head.getY() + 1.0D, head.getZ(), (random.nextDouble() - 0.5D) * 0.2D, 0.1D + random.nextDouble() * 0.15D,
                            (random.nextDouble() - 0.5D) * 0.2D, 16 + random.nextInt(8), 0.16F)
                        .gravity(0.03D).drag(0.96D).flicker(0.3F, random.nextFloat() * 10.0F);
                } else if (state == HydraHeads.FLAMING) {
                    Vec3 mouth = mouthOf(head, 1.0F);
                    Vec3 dir = head.getViewVector(1.0F).normalize();
                    Vec3 hit = lookHit(level, head, 1.0F);
                    double reach = flameReach(mouth, hit, age);
                    for (int k = 0; k < 3; k++) {
                        double t = random.nextDouble();
                        double spread = 0.3D + 1.4D * t;
                        Vec3 p = mouth.add(dir.scale(reach * t)).add((random.nextDouble() - 0.5D) * spread, (random.nextDouble() - 0.5D) * spread, (random.nextDouble() - 0.5D) * spread);
                        float size = 0.6F + 1.2F * (float) t;
                        FxParticles.glow(FIRE, p.x, p.y, p.z, dir.x * 0.5D + (random.nextDouble() - 0.5D) * 0.08D, dir.y * 0.5D + 0.03D, dir.z * 0.5D + (random.nextDouble() - 0.5D) * 0.08D,
                                8 + random.nextInt(5), size)
                            .fadeTo(0.6F, 0.18F, 0.08F).grow(size * 1.8F).drag(0.82D);
                    }
                    if (hit != null && reach >= hit.distanceTo(mouth) - 0.01D) {
                        for (int k = 0; k < 2; k++) {
                            FxParticles.glow(FIRE, hit.x + (random.nextDouble() - 0.5D) * 1.6D, hit.y + 0.2D, hit.z + (random.nextDouble() - 0.5D) * 1.6D,
                                    (random.nextDouble() - 0.5D) * 0.06D, 0.08D + random.nextDouble() * 0.08D, (random.nextDouble() - 0.5D) * 0.06D,
                                    10 + random.nextInt(6), 0.9F + random.nextFloat() * 0.6F)
                                .fadeTo(0.6F, 0.18F, 0.08F).grow(1.8F).drag(0.9D);
                        }
                        double angle = random.nextDouble() * Math.PI * 2.0D;
                        FxParticles.glow(EMBER, hit.x, hit.y + 0.2D, hit.z, Math.cos(angle) * 0.2D, 0.2D + random.nextDouble() * 0.2D, Math.sin(angle) * 0.2D,
                                14 + random.nextInt(8), 0.14F)
                            .gravity(0.04D).drag(0.97D).floor(hit.y + 0.05D, 0.3D);
                        if (time % 3L == 0L) {
                            FxParticles.soft(FxTextures.SMOKE, hit.x, hit.y + 1.2D, hit.z, (random.nextDouble() - 0.5D) * 0.04D, 0.07D, (random.nextDouble() - 0.5D) * 0.04D,
                                    30 + random.nextInt(10), 1.2F)
                                .color(0.22F, 0.18F, 0.16F).alpha(0.6F).grow(3.4F).drag(0.95D).fade(0.15F, 1.5F).spin(random.nextFloat() * 6.0F, 0.02F);
                        }
                    }
                } else if (state == HydraHeads.FLAME_BEGINNING) {
                    Vec3 mouth = mouthOf(head, 1.0F);
                    for (int k = 0; k < 2; k++) {
                        Vec3 offset = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).normalize().scale(1.6D + random.nextDouble());
                        Vec3 from = mouth.add(offset);
                        FxParticles.glow(EMBER, from.x, from.y, from.z, -offset.x * 0.14D, -offset.y * 0.14D, -offset.z * 0.14D, 10, 0.14F)
                            .color(1.0F, 0.9F, 0.6F).fadeTo(ORANGE_R, ORANGE_G, ORANGE_B).drag(0.92D).fade(0.3F, 2.0F);
                    }
                } else if (state == HydraHeads.BITE_READY && time % 3L == 0L) {
                    Vec3 mouth = mouthOf(head, 1.0F);
                    FxParticles.glow(DROP, mouth.x + (random.nextDouble() - 0.5D) * 0.6D, mouth.y - 0.3D, mouth.z + (random.nextDouble() - 0.5D) * 0.6D,
                            0.0D, -0.02D, 0.0D, 18, 0.18F)
                        .gravity(0.03D).streak(1.6F).fadeTo(EMBER_R, EMBER_G, EMBER_B);
                } else if (state == HydraHeads.MORTAR_SHOOTING && age % 5 == 0) {
                    Vec3 mouth = mouthOf(head, 1.0F);
                    FxParticles.soft(FxTextures.SMOKE, mouth.x, mouth.y, mouth.z, 0.0D, 0.06D, 0.0D, 22, 0.9F)
                        .color(0.3F, 0.24F, 0.2F).alpha(0.65F).grow(2.2F).drag(0.93D).fade(0.1F, 1.5F);
                } else if (state == HydraHeads.ROAR_RAWR && age % 2 == 0) {
                    Vec3 mouth = mouthOf(head, 1.0F);
                    Vec3 dir = head.getViewVector(1.0F).normalize();
                    FxParticles.glow(EMBER, mouth.x, mouth.y, mouth.z, dir.x * 0.4D + (random.nextDouble() - 0.5D) * 0.2D, dir.y * 0.4D + 0.08D,
                            dir.z * 0.4D + (random.nextDouble() - 0.5D) * 0.2D, 16 + random.nextInt(8), 0.16F)
                        .gravity(0.02D).drag(0.95D).flicker(0.3F, random.nextFloat() * 10.0F);
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
                int landed = MORTAR_LANDED.merge(id, 1, Integer::sum);
                if (decorative && landed % 4 == 0) {
                    FxParticles.soft(FxTextures.SMOKE, entity.getX(), entity.getY() + 0.4D, entity.getZ(), (random.nextDouble() - 0.5D) * 0.03D, 0.05D,
                            (random.nextDouble() - 0.5D) * 0.03D, 24, 0.6F)
                        .color(0.3F, 0.25F, 0.22F).alpha(0.6F).grow(1.8F).drag(0.94D).fade(0.1F, 1.5F);
                }
            }
        }

        Iterator<Sector> sectors = SECTORS.iterator();
        while (sectors.hasNext()) {
            Sector sector = sectors.next();
            sector.age++;
            if (sector.age > sector.warn + sector.active + SECTOR_FADE) {
                sectors.remove();
                continue;
            }
            if (sector.age == sector.warn) {
                FxShake.at(sector.x, sector.y, sector.z, 0.35F, sector.outer + 8.0F, 8);
            }
            if (!decorative || sector.age < sector.warn || sector.age > sector.warn + sector.active) {
                continue;
            }
            float progress = Mth.clamp((float) (sector.age - sector.warn) / sector.active, 0.0F, 1.0F);
            if (sector.kind == 0) {
                progress = ease(progress);
            }
            float lead = (sector.yaw + sector.sweep * progress + 90.0F) * Mth.DEG_TO_RAD;
            for (int k = 0; k < 3; k++) {
                double dist = sector.inner + random.nextDouble() * (sector.outer - sector.inner);
                double px = sector.x + Mth.cos(lead) * dist;
                double pz = sector.z + Mth.sin(lead) * dist;
                if (sector.kind == 1) {
                    FxParticles.glow(FIRE, px, sector.y + 0.3D, pz, (random.nextDouble() - 0.5D) * 0.05D, 0.06D + random.nextDouble() * 0.06D,
                            (random.nextDouble() - 0.5D) * 0.05D, 12 + random.nextInt(6), 0.8F + random.nextFloat() * 0.6F)
                        .fadeTo(0.6F, 0.18F, 0.08F).grow(1.8F).drag(0.9D);
                } else {
                    FxParticles.soft(FxTextures.DUST, px, sector.y + 0.3D, pz, (random.nextDouble() - 0.5D) * 0.06D, 0.04D + random.nextDouble() * 0.04D,
                            (random.nextDouble() - 0.5D) * 0.06D, 20 + random.nextInt(8), 0.8F)
                        .color(0.32F, 0.26F, 0.24F).alpha(0.7F).grow(2.2F).drag(0.9D).fade(0.1F, 1.5F).spin(random.nextFloat() * 6.0F, 0.03F);
                    FxParticles.glow(EMBER, px, sector.y + 0.2D, pz, (random.nextDouble() - 0.5D) * 0.15D, 0.15D + random.nextDouble() * 0.15D,
                            (random.nextDouble() - 0.5D) * 0.15D, 14 + random.nextInt(8), 0.13F)
                        .gravity(0.04D).drag(0.96D).floor(sector.y + 0.05D, 0.3D);
                }
            }
        }

        Iterator<Scorch> scorches = SCORCHES.iterator();
        while (scorches.hasNext()) {
            Scorch scorch = scorches.next();
            scorch.age++;
            if (scorch.age > scorch.life) {
                scorches.remove();
                continue;
            }
            if (!decorative || !scorch.burning || FxDraw.cameraDistanceSqr(scorch.x, scorch.y, scorch.z) > SCORCH_RANGE_SQR) {
                continue;
            }
            if (random.nextInt(14) == 0) {
                FxParticles.glow(FIRE, scorch.x + (random.nextDouble() - 0.5D) * scorch.radius, scorch.y + 0.2D, scorch.z + (random.nextDouble() - 0.5D) * scorch.radius,
                        0.0D, 0.04D + random.nextDouble() * 0.04D, 0.0D, 12 + random.nextInt(6), 0.4F + random.nextFloat() * 0.3F)
                    .fadeTo(0.6F, 0.18F, 0.08F).grow(0.9F).drag(0.92D);
            }
            if (random.nextInt(30) == 0) {
                FxParticles.soft(FxTextures.SMOKE, scorch.x, scorch.y + 0.5D, scorch.z, 0.0D, 0.04D, 0.0D, 28, 0.6F)
                    .color(0.24F, 0.2F, 0.18F).alpha(0.5F).grow(1.8F).drag(0.95D).fade(0.2F, 1.5F);
            }
        }

        Iterator<Slag> slags = SLAGS.iterator();
        while (slags.hasNext()) {
            Slag slag = slags.next();
            slag.age++;
            if (slag.age > slag.flight) {
                slags.remove();
                continue;
            }
            if (!decorative) {
                continue;
            }
            Vec3 pos = slagPos(slag, (float) slag.age / slag.flight);
            FxParticles.soft(FxTextures.SMOKE, pos.x, pos.y, pos.z, (random.nextDouble() - 0.5D) * 0.03D, 0.02D, (random.nextDouble() - 0.5D) * 0.03D,
                    22 + random.nextInt(8), 0.6F)
                .color(0.26F, 0.2F, 0.18F).alpha(0.7F).grow(1.8F).drag(0.92D).fade(0.05F, 1.5F).spin(random.nextFloat() * 6.0F, 0.04F);
            FxParticles.glow(FIRE, pos.x, pos.y, pos.z, (random.nextDouble() - 0.5D) * 0.04D, 0.02D, (random.nextDouble() - 0.5D) * 0.04D, 8, 0.7F)
                .fadeTo(0.6F, 0.18F, 0.08F).grow(0.3F).drag(0.9D);
        }

        FLASHES.removeIf(flash -> ++flash.age > FLASH_LIFE);

        Iterator<Wave> waves = WAVES.iterator();
        while (waves.hasNext()) {
            Wave wave = waves.next();
            wave.age++;
            if (wave.age > wave.expand + WAVE_FADE) {
                waves.remove();
                continue;
            }
            if (!decorative || wave.age > wave.expand) {
                continue;
            }
            float t = (float) wave.age / wave.expand;
            float radius = wave.maxRadius * (1.0F - (1.0F - t) * (1.0F - t));
            int count = Math.min(6, 1 + (int) (radius / 3.0F));
            for (int k = 0; k < count; k++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double px = wave.x + Math.cos(angle) * radius;
                double pz = wave.z + Math.sin(angle) * radius;
                FxParticles.glow(FIRE, px, wave.y + 0.4D, pz, Math.cos(angle) * 0.05D, 0.07D + random.nextDouble() * 0.06D, Math.sin(angle) * 0.05D,
                        10 + random.nextInt(6), 0.9F + random.nextFloat() * 0.5F)
                    .fadeTo(0.6F, 0.18F, 0.08F).grow(1.8F).drag(0.9D);
                if (random.nextBoolean()) {
                    FxParticles.glow(EMBER, px, wave.y + 0.3D, pz, Math.cos(angle) * 0.18D, 0.2D + random.nextDouble() * 0.15D, Math.sin(angle) * 0.18D,
                            14 + random.nextInt(8), 0.13F)
                        .gravity(0.04D).drag(0.96D).floor(wave.y + 0.05D, 0.3D);
                }
            }
        }

        Iterator<Geyser> geysers = GEYSERS.iterator();
        while (geysers.hasNext()) {
            Geyser geyser = geysers.next();
            geyser.age++;
            if (geyser.age > geyser.delay + geyser.duration + GEYSER_FADE) {
                geysers.remove();
                continue;
            }
            if (geyser.age == geyser.delay && !geyser.points.isEmpty()) {
                Vec3 first = geyser.points.get(0);
                FxShake.at(first.x, first.y, first.z, 0.7F, 20.0F, 12);
            }
            if (!decorative) {
                continue;
            }
            if (geyser.age >= geyser.delay - geyser.warn && geyser.age < geyser.delay) {
                if (geyser.age % 3 == 0) {
                    for (Vec3 p : geyser.points) {
                        double angle = random.nextDouble() * Math.PI * 2.0D;
                        double dist = random.nextDouble() * geyser.columnRadius;
                        FxParticles.glow(BUBBLE, p.x + Math.cos(angle) * dist, p.y + 0.15D, p.z + Math.sin(angle) * dist, 0.0D, 0.01D, 0.0D, 10, 0.25F)
                            .grow(0.5F).fade(0.2F, 3.0F);
                    }
                }
            } else if (geyser.age >= geyser.delay && geyser.age < geyser.delay + geyser.duration) {
                boolean burst = geyser.age == geyser.delay;
                for (Vec3 p : geyser.points) {
                    double top = p.y + geyser.height;
                    int drops = 1;
                    if (burst) {
                        drops = 8;
                    }
                    for (int k = 0; k < drops; k++) {
                        double angle = random.nextDouble() * Math.PI * 2.0D;
                        double speed = 0.08D + random.nextDouble() * 0.14D;
                        FxParticles.glow(DROP, p.x, top, p.z, Math.cos(angle) * speed, 0.15D + random.nextDouble() * 0.2D, Math.sin(angle) * speed,
                                26 + random.nextInt(10), 0.24F)
                            .gravity(0.045D).drag(0.98D).streak(1.4F).floor(p.y + 0.05D, 0.0D);
                    }
                    if (geyser.age % 2 == 0) {
                        FxParticles.glow(FIRE, p.x, top, p.z, (random.nextDouble() - 0.5D) * 0.08D, 0.1D, (random.nextDouble() - 0.5D) * 0.08D,
                                12, 1.0F + geyser.columnRadius * 0.6F)
                            .fadeTo(0.6F, 0.18F, 0.08F).grow(2.2F).drag(0.9D);
                    }
                    if (geyser.age % 4 == 0) {
                        FxParticles.soft(FxTextures.SMOKE, p.x, top + 0.6D, p.z, (random.nextDouble() - 0.5D) * 0.04D, 0.07D, (random.nextDouble() - 0.5D) * 0.04D,
                                30 + random.nextInt(10), 1.2F)
                            .color(0.24F, 0.2F, 0.18F).alpha(0.6F).grow(3.4F).drag(0.95D).fade(0.15F, 1.5F).spin(random.nextFloat() * 6.0F, 0.02F);
                    }
                }
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
            && WAVES.isEmpty() && GEYSERS.isEmpty()) {
            return;
        }
        float partialTick = event.getPartialTick();
        FxDraw.camera(event.getCamera());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();

        renderScorches(pose, buffers, partialTick);
        for (Sector sector : SECTORS) {
            renderSector(pose, buffers, sector, partialTick);
        }
        for (Wave wave : WAVES) {
            renderWave(pose, buffers, wave, partialTick);
        }
        for (Geyser geyser : GEYSERS) {
            renderGeyser(pose, buffers, geyser, partialTick);
        }
        for (Slag slag : SLAGS) {
            renderSlag(pose, buffers, slag, partialTick);
        }
        for (Flash flash : FLASHES) {
            renderFlash(pose, buffers, flash, partialTick);
        }
        if (HydraConfig.ENABLED.get()) {
            for (int id : HYDRAS) {
                if (level.getEntity(id) instanceof Hydra hydra && hydra.isAlive()) {
                    renderHeads(pose, buffers, level, hydra, partialTick);
                }
            }
            for (int id : MORTARS) {
                Entity mortar = level.getEntity(id);
                if (mortar != null) {
                    renderMortar(pose, buffers, mortar, partialTick);
                }
            }
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static void radialLine(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float angle, float from, float to, float half,
                                   float r, float g, float b, float a) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float tx = -sin * half;
        float tz = cos * half;
        FxDraw.glowVertex(consumer, pose, cx + cos * from - tx, y, cz + sin * from - tz, 0.0F, 0.0F, r, g, b, a);
        FxDraw.glowVertex(consumer, pose, cx + cos * from + tx, y, cz + sin * from + tz, 0.0F, 1.0F, r, g, b, a);
        FxDraw.glowVertex(consumer, pose, cx + cos * to + tx, y, cz + sin * to + tz, 1.0F, 1.0F, r, g, b, a);
        FxDraw.glowVertex(consumer, pose, cx + cos * to - tx, y, cz + sin * to - tz, 1.0F, 0.0F, r, g, b, a);
    }

    private static void curtain(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float angle, float from, float to, float height,
                                float vOffset, float r, float g, float b, float bottomAlpha, float topAlpha) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double x0 = cx + cos * from;
        double z0 = cz + sin * from;
        double x1 = cx + cos * to;
        double z1 = cz + sin * to;
        float uRepeat = Math.max(0.5F, (to - from) / height);
        FxDraw.glowVertex(consumer, pose, x0, y + height, z0, 0.0F, vOffset, r, g, b, topAlpha);
        FxDraw.glowVertex(consumer, pose, x0, y, z0, 0.0F, vOffset + 1.0F, r, g, b, bottomAlpha);
        FxDraw.glowVertex(consumer, pose, x1, y, z1, uRepeat, vOffset + 1.0F, r, g, b, bottomAlpha);
        FxDraw.glowVertex(consumer, pose, x1, y + height, z1, uRepeat, vOffset, r, g, b, topAlpha);
    }

    private static void renderScorches(PoseStack.Pose pose, MultiBufferSource buffers, float partialTick) {
        if (SCORCHES.isEmpty()) {
            return;
        }
        VertexConsumer decal = buffers.getBuffer(FxRenderTypes.soft(SCORCH));
        for (Scorch scorch : SCORCHES) {
            FxDraw.flat(decal, pose, scorch.x, scorch.y + 0.04D, scorch.z, scorch.radius, scorch.spin, 1.0F, 1.0F, 1.0F, scorchAlpha(scorch, partialTick) * 0.92F);
        }
        VertexConsumer cracks = buffers.getBuffer(FxRenderTypes.glow(SCORCH_GLOW));
        for (Scorch scorch : SCORCHES) {
            float age = scorch.age + partialTick;
            float heat;
            if (scorch.burning) {
                heat = 0.75F + 0.12F * Mth.sin(age * 0.5F + scorch.spin * 3.0F) + 0.13F * Mth.sin(age * 1.37F + scorch.spin * 5.0F);
            } else {
                heat = Mth.clamp(1.0F - age / (scorch.life * 0.7F), 0.0F, 1.0F);
            }
            FxDraw.flat(cracks, pose, scorch.x, scorch.y + 0.05D, scorch.z, scorch.radius * 0.95F, scorch.spin, heat, heat * heat, heat * heat * heat,
                scorchAlpha(scorch, partialTick));
        }
        VertexConsumer bubbles = buffers.getBuffer(FxRenderTypes.glow(BUBBLE));
        for (Scorch scorch : SCORCHES) {
            if (!scorch.burning) {
                continue;
            }
            float age = scorch.age + partialTick;
            float alpha = scorchAlpha(scorch, partialTick);
            for (int slot = 0; slot < SCORCH_BUBBLES; slot++) {
                float clock = age / BUBBLE_CYCLE + slot / (float) SCORCH_BUBBLES + scorch.spin;
                int cycle = Mth.floor(clock);
                float local = clock - cycle;
                float seed = Mth.sin(cycle * 12.9898F + slot * 78.233F + scorch.spin * 37.719F) * 43758.547F;
                seed -= Mth.floor(seed);
                float angle = seed * Mth.TWO_PI;
                float dist = scorch.radius * 0.55F * Mth.sqrt((seed * 7.0F) % 1.0F);
                float size = scorch.radius * 0.16F * local / 0.8F;
                float bubbleAlpha = alpha * 0.9F;
                if (local > 0.8F) {
                    float pop = (local - 0.8F) / 0.2F;
                    size = scorch.radius * 0.16F * (1.0F + pop * 0.8F);
                    bubbleAlpha *= 1.0F - pop;
                }
                FxDraw.flat(bubbles, pose, scorch.x + Mth.cos(angle) * dist, scorch.y + 0.06D, scorch.z + Mth.sin(angle) * dist, size, angle,
                    1.0F, 1.0F, 1.0F, bubbleAlpha);
            }
        }
    }

    private static float scorchAlpha(Scorch scorch, float partialTick) {
        float age = scorch.age + partialTick;
        return Mth.clamp((scorch.life - age) / 20.0F, 0.0F, 1.0F) * Mth.clamp(age / 3.0F, 0.0F, 1.0F);
    }

    private static void renderSector(PoseStack.Pose pose, MultiBufferSource buffers, Sector sector, float partialTick) {
        float age = sector.age + partialTick;
        float fade = 1.0F;
        if (age > sector.warn + sector.active) {
            fade = Mth.clamp(1.0F - (age - sector.warn - sector.active) / SECTOR_FADE, 0.0F, 1.0F);
        }
        float a0 = (sector.yaw + 90.0F) * Mth.DEG_TO_RAD;
        float a1 = (sector.yaw + sector.sweep + 90.0F) * Mth.DEG_TO_RAD;
        int segments = Math.max(4, (int) (Math.abs(sector.sweep) / 5.0F));
        float mid = (sector.inner + sector.outer) * 0.5F;
        float uRepeat = Math.max(1.0F, Math.abs(a1 - a0) * mid / Math.max(0.5F, (sector.outer - sector.inner) * 4.0F));
        float warnT = Mth.clamp(age / sector.warn, 0.0F, 1.0F);
        float blink = 1.0F;
        if (age < sector.warn && sector.warn - age < 8.0F) {
            blink = 0.6F + 0.4F * Math.abs(Mth.sin(age * 0.9F));
        }
        double y = sector.y + 0.06D;
        float edgeR = EMBER_R;
        float edgeG = EMBER_G;
        float edgeB = EMBER_B;
        float tintG = 1.0F;
        float tintB = 1.0F;
        if (sector.kind == 1) {
            edgeR = ORANGE_R;
            edgeG = ORANGE_G;
            edgeB = ORANGE_B;
            tintG = 0.8F;
            tintB = 0.7F;
        }
        VertexConsumer ash = buffers.getBuffer(FxRenderTypes.soft(ASH_FAN));
        FxDraw.arc(ash, pose, sector.x, y, sector.z, sector.inner, sector.outer, a0, a1, segments, uRepeat, 1.0F, tintG, tintB,
            (0.25F + 0.35F * warnT) * fade, (0.55F + 0.35F * warnT) * fade);

        float edgeAlpha = (0.35F + 0.55F * warnT) * blink * fade;
        VertexConsumer edge = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.arc(edge, pose, sector.x, y + 0.01D, sector.z, sector.outer - 0.18F, sector.outer + 0.18F, a0, a1, segments, uRepeat * 2.0F,
            edgeR, edgeG, edgeB, edgeAlpha, edgeAlpha);
        if (sector.inner > 0.3F) {
            FxDraw.arc(edge, pose, sector.x, y + 0.01D, sector.z, sector.inner - 0.12F, sector.inner + 0.12F, a0, a1, segments, uRepeat,
                edgeR, edgeG, edgeB, edgeAlpha * 0.6F, edgeAlpha * 0.6F);
        }
        radialLine(edge, pose, sector.x, y + 0.01D, sector.z, a0, sector.inner, sector.outer, 0.16F, edgeR, edgeG, edgeB, edgeAlpha);
        radialLine(edge, pose, sector.x, y + 0.01D, sector.z, a1, sector.inner, sector.outer, 0.16F, edgeR, edgeG, edgeB, edgeAlpha);

        if (age < sector.warn) {
            return;
        }
        float progress = Mth.clamp((age - sector.warn) / sector.active, 0.0F, 1.0F);
        if (sector.kind == 0) {
            progress = ease(progress);
        }
        if (progress <= 0.001F) {
            return;
        }
        float lead = a0 + (a1 - a0) * progress;
        int fillSegments = Math.max(2, (int) (segments * progress));
        VertexConsumer fire = buffers.getBuffer(FxRenderTypes.glow(FIRE_WALL));
        if (sector.kind == 1) {
            FxDraw.arc(fire, pose, sector.x, y + 0.02D, sector.z, sector.inner, sector.outer, a0, lead, fillSegments, Math.max(1.0F, uRepeat * progress * 2.0F),
                ORANGE_R, ORANGE_G, ORANGE_B, 0.45F * fade, 0.85F * fade);
            curtain(fire, pose, sector.x, sector.y, sector.z, lead, sector.inner, sector.outer, 2.4F, -age * 0.09F, ORANGE_R, ORANGE_G, ORANGE_B, 0.9F * fade, 0.0F);
            curtain(fire, pose, sector.x, sector.y, sector.z, lead, sector.inner, sector.outer, 1.2F, -age * 0.14F, GOLD_R, GOLD_G, GOLD_B, 0.7F * fade, 0.0F);
        } else {
            FxDraw.arc(fire, pose, sector.x, y + 0.02D, sector.z, sector.inner, sector.outer, a0, lead, fillSegments, Math.max(1.0F, uRepeat * progress * 2.0F),
                EMBER_R, EMBER_G, EMBER_B, 0.3F * fade, 0.6F * fade);
            curtain(fire, pose, sector.x, sector.y, sector.z, lead, sector.inner, sector.outer, 1.1F, -age * 0.1F, EMBER_R, EMBER_G, EMBER_B, 0.7F * fade, 0.0F);
        }
        VertexConsumer line = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        radialLine(line, pose, sector.x, y + 0.03D, sector.z, lead, sector.inner, sector.outer, 0.3F, CORE_R, CORE_G, CORE_B, 0.9F * fade);
    }

    private static Vec3 slagPos(Slag slag, float t) {
        Vec3 pos = slag.from.lerp(slag.to, t);
        return pos.add(0.0D, slag.arc * 4.0D * t * (1.0D - t), 0.0D);
    }

    private static void heatRings(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float radius, float clock, float alpha) {
        for (int k = 0; k < 3; k++) {
            float phase = (clock + k / 3.0F) % 1.0F;
            float size = radius * (1.7F - 1.4F * phase);
            float fade = 4.0F * phase * (1.0F - phase);
            FxDraw.flat(consumer, pose, x, y + k * 0.002D, z, size, k * 2.1F, ORANGE_R, ORANGE_G, ORANGE_B, alpha * fade);
        }
    }

    private static void renderSlag(PoseStack.Pose pose, MultiBufferSource buffers, Slag slag, float partialTick) {
        float age = slag.age + partialTick;
        float t = Mth.clamp(age / slag.flight, 0.0F, 1.0F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, slag.to.x, slag.to.y + 0.07D, slag.to.z, slag.radius * 1.2F, 0.0F,
            Mth.lerp(t, EMBER_R, ORANGE_R), Mth.lerp(t, EMBER_G, ORANGE_G), Mth.lerp(t, EMBER_B, ORANGE_B), 0.25F + 0.55F * t);
        Vec3 pos = slagPos(slag, t);
        FxDraw.sprite(glow, pose, pos.x, pos.y, pos.z, 2.4F, 0.0F, ORANGE_R, ORANGE_G, ORANGE_B, 0.75F);

        VertexConsumer rings = buffers.getBuffer(FxRenderTypes.glow(HEAT_RING));
        heatRings(rings, pose, slag.to.x, slag.to.y + 0.075D, slag.to.z, slag.radius, age * 0.05F, 0.35F + 0.6F * t);

        int count = 10;
        Vec3[] points = new Vec3[count];
        float[] widths = new float[count];
        float[] alphas = new float[count];
        for (int k = 0; k < count; k++) {
            float back = (count - 1 - k) * 0.03F;
            points[k] = slagPos(slag, Math.max(0.0F, t - back));
            float f = (float) k / (count - 1);
            widths[k] = 0.05F + 0.4F * f;
            alphas[k] = 0.9F * f;
        }
        VertexConsumer trail = buffers.getBuffer(FxRenderTypes.glow(FLAME));
        FxDraw.trail(trail, pose, points, count, widths, alphas, -age * 0.3F, 0.3F, ORANGE_R, ORANGE_G * 1.3F, ORANGE_B);

        VertexConsumer body = buffers.getBuffer(FxRenderTypes.soft(SLAG));
        FxDraw.sprite(body, pose, pos.x, pos.y, pos.z, 1.1F, age * 0.25F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderFlash(PoseStack.Pose pose, MultiBufferSource buffers, Flash flash, float partialTick) {
        float t = Mth.clamp((flash.age + partialTick) / FLASH_LIFE, 0.0F, 1.0F);
        float grow = 1.0F - (1.0F - t) * (1.0F - t);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, flash.x, flash.y + 0.08D, flash.z, flash.radius * 1.8F, 0.0F, GOLD_R, GOLD_G, GOLD_B, (1.0F - t) * 0.8F);
        VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
        FxDraw.flat(shock, pose, flash.x, flash.y + 0.1D, flash.z, flash.radius * (0.6F + 2.2F * grow), t * 2.0F, ORANGE_R, ORANGE_G, ORANGE_B, (1.0F - t) * 0.9F);
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        FxDraw.sprite(flare, pose, flash.x, flash.y + 0.6D, flash.z, flash.radius * 4.0F * (1.0F - t * 0.5F), t, CORE_R, CORE_G, CORE_B, (1.0F - t) * (1.0F - t));
    }

    private static void renderWave(PoseStack.Pose pose, MultiBufferSource buffers, Wave wave, float partialTick) {
        float age = wave.age + partialTick;
        float t = Mth.clamp(age / wave.expand, 0.0F, 1.0F);
        float radius = wave.maxRadius * (1.0F - (1.0F - t) * (1.0F - t));
        if (radius <= 0.1F) {
            return;
        }
        float alpha = 0.95F * (1.0F - t * 0.35F);
        if (age > wave.expand) {
            alpha *= Mth.clamp(1.0F - (age - wave.expand) / WAVE_FADE, 0.0F, 1.0F);
        }
        int segments = Mth.clamp((int) (radius * 5.0F), 20, 72);
        float circumference = radius * Mth.TWO_PI;
        float height = (float) WAVE_HEIGHT * (1.0F - 0.35F * t);
        VertexConsumer ash = buffers.getBuffer(FxRenderTypes.soft(ASH_FAN));
        FxDraw.ring(ash, pose, wave.x, wave.y + 0.05D, wave.z, Math.max(0.0F, radius - 2.2F), radius, segments, 0.0F, circumference / 8.0F,
            1.0F, 1.0F, 1.0F, 0.0F, alpha * 0.85F);
        VertexConsumer fire = buffers.getBuffer(FxRenderTypes.glow(FIRE_WALL));
        FxDraw.wall(fire, pose, wave.x, wave.y, wave.z, radius, height, segments, circumference / height, -age * 0.07F,
            ORANGE_R, ORANGE_G, ORANGE_B, alpha, 0.0F);
        FxDraw.wall(fire, pose, wave.x, wave.y, wave.z, radius * 0.97F, height * 0.5F, segments, circumference / height, -age * 0.11F,
            GOLD_R, GOLD_G, GOLD_B, alpha * 0.8F, 0.0F);
        VertexConsumer front = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.ring(front, pose, wave.x, wave.y + 0.06D, wave.z, radius - 0.25F, radius + 0.25F, segments, 0.0F, circumference / 4.0F,
            CORE_R, CORE_G, CORE_B, alpha * 0.9F, alpha * 0.9F);
    }

    private static void renderGeyser(PoseStack.Pose pose, MultiBufferSource buffers, Geyser geyser, float partialTick) {
        float age = geyser.age + partialTick;
        int warnStart = geyser.delay - geyser.warn;
        if (age < warnStart) {
            return;
        }
        float radius = geyser.columnRadius;
        if (age < geyser.delay) {
            float progress = Mth.clamp((age - warnStart) / Math.max(1, geyser.warn), 0.0F, 1.0F);
            float blink = 1.0F;
            if (geyser.delay - age < 6.0F) {
                blink = 0.6F + 0.4F * Math.abs(Mth.sin(age * 1.1F));
            }
            VertexConsumer pool = buffers.getBuffer(FxRenderTypes.glow(LAVA_POOL));
            for (int i = 0; i < geyser.points.size(); i++) {
                Vec3 p = geyser.points.get(i);
                float pulse = 0.9F + 0.1F * Mth.sin(age * 1.2F + i * 1.7F);
                FxDraw.flat(pool, pose, p.x, p.y + 0.06D, p.z, radius * (0.7F + 0.9F * progress) * pulse, age * 0.03F + i,
                    1.0F, 1.0F, 1.0F, (0.45F + 0.55F * progress) * blink);
            }
            VertexConsumer rings = buffers.getBuffer(FxRenderTypes.glow(HEAT_RING));
            for (int i = 0; i < geyser.points.size(); i++) {
                Vec3 p = geyser.points.get(i);
                for (int k = 0; k < 2; k++) {
                    float phase = (age * 0.05F + k * 0.5F + i * 0.13F) % 1.0F;
                    float fade = 4.0F * phase * (1.0F - phase);
                    FxDraw.flat(rings, pose, p.x, p.y + 0.07D, p.z, radius * (2.6F - 1.8F * phase), k * 1.7F, ORANGE_R, ORANGE_G, ORANGE_B,
                        fade * (0.4F + 0.6F * progress));
                }
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
        VertexConsumer lava = buffers.getBuffer(FxRenderTypes.soft(LAVA));
        for (Vec3 p : geyser.points) {
            FxDraw.wallArc(lava, pose, p.x, p.y, p.z, radius * 0.75F, radius * 0.6F, height, 0.0F, Mth.TWO_PI, 10, 2.0F, -age * 0.18F,
                1.0F, 1.0F, 1.0F, 1.0F, 0.9F * sink);
        }
        VertexConsumer lavaGlow = buffers.getBuffer(FxRenderTypes.glow(LAVA_GLOW));
        for (Vec3 p : geyser.points) {
            FxDraw.wallArc(lavaGlow, pose, p.x, p.y, p.z, radius * 0.8F, radius * 0.66F, height, 0.0F, Mth.TWO_PI, 10, 2.0F, -age * 0.25F,
                1.0F, 1.0F, 1.0F, 0.8F * sink, 0.5F * sink);
        }
        VertexConsumer fire = buffers.getBuffer(FxRenderTypes.glow(FIRE_WALL));
        for (Vec3 p : geyser.points) {
            FxDraw.wallArc(fire, pose, p.x, p.y, p.z, radius * 1.05F, radius * 0.75F, height * 1.05F, 0.0F, Mth.TWO_PI, 12, 2.0F, -age * 0.1F,
                ORANGE_R, ORANGE_G, ORANGE_B, 0.55F * sink, 0.0F);
        }
        VertexConsumer base = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        for (Vec3 p : geyser.points) {
            FxDraw.flat(base, pose, p.x, p.y + 0.07D, p.z, radius * 2.2F, 0.0F, ORANGE_R, ORANGE_G, ORANGE_B, 0.7F * sink);
        }
        VertexConsumer splash = buffers.getBuffer(FxRenderTypes.glow(HEAT_RING));
        for (int i = 0; i < geyser.points.size(); i++) {
            Vec3 p = geyser.points.get(i);
            float phase = (active * 0.08F + i * 0.3F) % 1.0F;
            FxDraw.flat(splash, pose, p.x, p.y + 0.08D, p.z, radius * (1.0F + 1.6F * phase), i * 0.9F, GOLD_R, GOLD_G, GOLD_B, (1.0F - phase) * 0.8F * sink);
        }
        VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
        for (int i = 0; i < geyser.points.size(); i++) {
            Vec3 p = geyser.points.get(i);
            FxDraw.sprite(flare, pose, p.x, p.y + height, p.z, radius * 3.2F * (0.9F + 0.1F * Mth.sin(age * 0.9F + i)), age * 0.05F + i,
                GOLD_R, GOLD_G, GOLD_B, 0.85F * sink);
        }
    }

    private static void renderHeads(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, Hydra hydra, float partialTick) {
        for (int i = 0; i < hydra.hc.length; i++) {
            HydraHead head = hydra.hc[i].headEntity;
            if (head == null) {
                continue;
            }
            int state = HydraHeads.stateOf(head);
            float age = headAge(head) + partialTick;
            if (state == HydraHeads.FLAMING) {
                renderFlame(pose, buffers, level, head, age, partialTick);
            } else if (state == HydraHeads.FLAME_BEGINNING) {
                Vec3 mouth = mouthOf(head, partialTick);
                float t = Mth.clamp(age / 40.0F, 0.0F, 1.0F);
                VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
                FxDraw.sprite(glow, pose, mouth.x, mouth.y, mouth.z, 0.6F + 2.2F * t, 0.0F, ORANGE_R, ORANGE_G, ORANGE_B, 0.4F + 0.5F * t);
                VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
                FxDraw.sprite(flare, pose, mouth.x, mouth.y, mouth.z, 0.4F + 1.4F * t, age * 0.15F, CORE_R, CORE_G, CORE_B, 0.3F + 0.6F * t);
            } else if (state == HydraHeads.BITE_BEGINNING || state == HydraHeads.BITE_READY) {
                Vec3 mouth = mouthOf(head, partialTick);
                float pulse = 0.8F + 0.2F * Mth.sin(age * 0.8F);
                VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
                FxDraw.sprite(glow, pose, mouth.x, mouth.y, mouth.z, 1.4F * pulse, 0.0F, EMBER_R, EMBER_G, EMBER_B, 0.75F);
                if (state == HydraHeads.BITE_READY) {
                    Vec3 hit = lookHit(level, head, partialTick);
                    if (hit != null) {
                        float t = Mth.clamp(age / 40.0F, 0.0F, 1.0F);
                        float yaw = Mth.lerp(partialTick, head.yRotO, head.getYRot()) * Mth.DEG_TO_RAD;
                        VertexConsumer fangs = buffers.getBuffer(FxRenderTypes.glow(FANGS));
                        FxDraw.flat(fangs, pose, hit.x, hit.y + 0.06D, hit.z, (1.8F - 0.4F * t) * pulse, -yaw,
                            Mth.lerp(t, EMBER_R, ORANGE_R), Mth.lerp(t, EMBER_G, ORANGE_G), Mth.lerp(t, EMBER_B, ORANGE_B), 0.6F + 0.35F * t);
                    }
                }
            } else if (state == HydraHeads.BITING && age < 8.0F) {
                Vec3 hit = lookHit(level, head, partialTick);
                if (hit != null) {
                    float t = age / 8.0F;
                    float yaw = Mth.lerp(partialTick, head.yRotO, head.getYRot()) * Mth.DEG_TO_RAD;
                    VertexConsumer fangs = buffers.getBuffer(FxRenderTypes.glow(FANGS));
                    FxDraw.flat(fangs, pose, hit.x, hit.y + 0.06D, hit.z, 1.4F * (1.0F - 0.7F * t), -yaw, CORE_R, CORE_G, CORE_B, 1.0F - t);
                    VertexConsumer shock = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SHOCK));
                    FxDraw.flat(shock, pose, hit.x, hit.y + 0.08D, hit.z, 0.8F + 2.2F * t, yaw, ORANGE_R, ORANGE_G, ORANGE_B, (1.0F - t) * 0.8F);
                }
            } else if (state == HydraHeads.MORTAR_BEGINNING || state == HydraHeads.MORTAR_SHOOTING) {
                Vec3 mouth = mouthOf(head, partialTick);
                float size = 1.2F;
                if (state == HydraHeads.MORTAR_SHOOTING) {
                    size += 0.8F * (1.0F - (age % 10.0F) / 10.0F);
                }
                VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
                FxDraw.sprite(glow, pose, mouth.x, mouth.y, mouth.z, size, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.8F);
                VertexConsumer flare = buffers.getBuffer(FxRenderTypes.glow(FxTextures.FLARE));
                FxDraw.sprite(flare, pose, mouth.x, mouth.y, mouth.z, size * 0.7F, age * 0.12F, CORE_R, CORE_G, CORE_B, 0.6F);
            } else if (state == HydraHeads.DYING) {
                double x = Mth.lerp(partialTick, head.xo, head.getX());
                double y = Mth.lerp(partialTick, head.yo, head.getY()) + 1.0D;
                double z = Mth.lerp(partialTick, head.zo, head.getZ());
                float fade = Mth.clamp(1.0F - age / 70.0F, 0.0F, 1.0F);
                VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
                FxDraw.sprite(glow, pose, x, y, z, 2.8F, 0.0F, EMBER_R, EMBER_G, EMBER_B, 0.7F * fade);
            }
        }
    }

    private static void renderFlame(PoseStack.Pose pose, MultiBufferSource buffers, ClientLevel level, HydraHead head, float age, float partialTick) {
        Vec3 mouth = mouthOf(head, partialTick);
        Vec3 dir = head.getViewVector(partialTick).normalize();
        Vec3 hit = lookHit(level, head, partialTick);
        double reach = flameReach(mouth, hit, age);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(dir.y) > 0.95D) {
            up = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 s1 = dir.cross(up).normalize();
        Vec3 s2 = dir.cross(s1).normalize();
        float scroll = age * 0.45F;
        VertexConsumer consumer = buffers.getBuffer(FxRenderTypes.glow(FLAME));
        for (int j = 0; j < FLAME_SEGMENTS; j++) {
            float t0 = (float) j / FLAME_SEGMENTS;
            float t1 = (float) (j + 1) / FLAME_SEGMENTS;
            float tm = (t0 + t1) * 0.5F;
            float w0 = 0.35F + 1.9F * t0 + 0.12F * Mth.sin(age * 1.3F + j * 2.1F);
            float w1 = 0.35F + 1.9F * t1 + 0.12F * Mth.sin(age * 1.3F + (j + 1) * 2.1F);
            Vec3 c0 = mouth.add(dir.scale(reach * t0));
            Vec3 c1 = mouth.add(dir.scale(reach * t1));
            float u0 = (float) (reach * t0 / 4.0D) - scroll;
            float u1 = (float) (reach * t1 / 4.0D) - scroll;
            float g;
            float b;
            if (tm < 0.5F) {
                g = Mth.lerp(tm * 2.0F, CORE_G, ORANGE_G);
                b = Mth.lerp(tm * 2.0F, CORE_B, ORANGE_B);
            } else {
                g = Mth.lerp((tm - 0.5F) * 2.0F, ORANGE_G, EMBER_G);
                b = Mth.lerp((tm - 0.5F) * 2.0F, ORANGE_B, EMBER_B);
            }
            FxDraw.beam(consumer, pose, c0, c1, w0, w1, u0, u1, 1.0F, g, b, 0.9F - 0.5F * t0, 0.9F - 0.5F * t1);
            for (int k = 0; k < 2; k++) {
                float angle = k * Mth.HALF_PI + age * 0.04F;
                Vec3 side = s1.scale(Mth.cos(angle)).add(s2.scale(Mth.sin(angle)));
                float pw0 = w0 * 0.85F;
                float pw1 = w1 * 0.85F;
                FxDraw.glowVertex(consumer, pose, c0.x - side.x * pw0, c0.y - side.y * pw0, c0.z - side.z * pw0, u0 + 0.37F, 0.0F, 1.0F, g, b, (0.9F - 0.5F * t0) * 0.55F);
                FxDraw.glowVertex(consumer, pose, c0.x + side.x * pw0, c0.y + side.y * pw0, c0.z + side.z * pw0, u0 + 0.37F, 1.0F, 1.0F, g, b, (0.9F - 0.5F * t0) * 0.55F);
                FxDraw.glowVertex(consumer, pose, c1.x + side.x * pw1, c1.y + side.y * pw1, c1.z + side.z * pw1, u1 + 0.37F, 1.0F, 1.0F, g, b, (0.9F - 0.5F * t1) * 0.55F);
                FxDraw.glowVertex(consumer, pose, c1.x - side.x * pw1, c1.y - side.y * pw1, c1.z - side.z * pw1, u1 + 0.37F, 0.0F, 1.0F, g, b, (0.9F - 0.5F * t1) * 0.55F);
            }
            float coreScroll = age * 0.8F;
            FxDraw.beam(consumer, pose, c0, c1, 0.12F + 0.5F * t0, 0.12F + 0.5F * t1, u0 * 1.5F - coreScroll, u1 * 1.5F - coreScroll,
                CORE_R, CORE_G, CORE_B, 0.8F * (1.0F - t0), 0.8F * (1.0F - t1));
        }
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, pose, mouth.x, mouth.y, mouth.z, 2.0F, 0.0F, CORE_R, CORE_G, CORE_B, 0.9F);
        if (hit != null && reach >= hit.distanceTo(mouth) - 0.01D) {
            float pulse = 0.85F + 0.15F * Mth.sin(age * 1.7F);
            FxDraw.sprite(glow, pose, hit.x, hit.y + 0.4D, hit.z, 3.2F * pulse, 0.0F, ORANGE_R, ORANGE_G, ORANGE_B, 0.85F);
        }
    }

    private static void renderMortar(PoseStack.Pose pose, MultiBufferSource buffers, Entity mortar, float partialTick) {
        double x = Mth.lerp(partialTick, mortar.xo, mortar.getX());
        double y = Mth.lerp(partialTick, mortar.yo, mortar.getY());
        double z = Mth.lerp(partialTick, mortar.zo, mortar.getZ());
        float age = mortar.tickCount + partialTick;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.9F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, pose, x, y + mortar.getBbHeight() * 0.5D, z, 1.8F * pulse, 0.0F, GOLD_R, GOLD_G, GOLD_B, 0.75F);
        Integer landed = MORTAR_LANDED.get(mortar.getId());
        if (landed == null) {
            return;
        }
        float t = Mth.clamp((landed + partialTick) / MORTAR_FUSE, 0.0F, 1.0F);
        float flicker = 1.0F;
        if (t > 0.75F) {
            flicker = 0.7F + 0.3F * Mth.sin(age * 2.5F);
        }
        VertexConsumer ground = buffers.getBuffer(FxRenderTypes.soft(SCORCH));
        FxDraw.flat(ground, pose, x, y + 0.05D, z, 0.6F + 1.6F * t, mortar.getId() * 0.7F, 1.0F, 1.0F, 1.0F, 0.5F + 0.4F * t);
        VertexConsumer pool = buffers.getBuffer(FxRenderTypes.glow(LAVA_POOL));
        FxDraw.flat(pool, pose, x, y + 0.06D, z, (0.5F + 1.4F * t) * flicker, mortar.getId() * 1.3F, 1.0F, 1.0F, 1.0F, 0.35F + 0.5F * t);
        VertexConsumer rings = buffers.getBuffer(FxRenderTypes.glow(HEAT_RING));
        heatRings(rings, pose, x, y + 0.07D, z, 0.6F + 1.6F * t, (landed + partialTick) * (0.03F + 0.05F * t), 0.3F + 0.5F * t);
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
}
