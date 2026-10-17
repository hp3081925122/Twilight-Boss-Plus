package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.render.MushroomPillarRenderer;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.entity.ThrownAxe;
import com.twilightbossplus.network.MinoFxPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.entity.monster.Minotaur;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MinoFxManager {
    private static final ResourceLocation ARROW = tex("arrow");
    private static final ResourceLocation FAIRY_RING = tex("fairy_ring");
    private static final ResourceLocation CRACK = tex("crack");
    private static final ResourceLocation SPORE_CLOUD = tex("spore_cloud");
    private static final ResourceLocation SPORE_RING = tex("spore_ring");
    private static final ResourceLocation BITS = tex("bits");

    private static final int ARROW_FADE = 6;
    private static final int WAVE_FADE = 8;
    private static final int CLOUD_GROW = 6;
    private static final int CLOUD_LIFE = 34;
    private static final int SMALL_CLOUD_LIFE = 14;
    private static final int RING_LIFE = 10;
    private static final int ROAR_LIFE = 16;
    private static final int STUN_FADE = 6;
    private static final int HORN_FADE = 8;
    private static final int HORN_TIMEOUT = 1200;
    private static final int MAX_CHUNKS = 400;

    private static final float SPORE_R = 0.91F;
    private static final float SPORE_G = 0.84F;
    private static final float SPORE_B = 0.65F;
    private static final float RAGE_R = 1.0F;
    private static final float RAGE_G = 0.29F;
    private static final float RAGE_B = 0.16F;

    private static final List<ChargeArrow> ARROWS = new ArrayList<>();
    private static final List<Chunk> CHUNKS = new ArrayList<>();
    private static final List<Stun> STUNS = new ArrayList<>();
    private static final List<WaveFx> WAVES = new ArrayList<>();
    private static final List<PillarWarn> WARNS = new ArrayList<>();
    private static final List<Cloud> CLOUDS = new ArrayList<>();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final Map<Integer, Horn> HORNS = new HashMap<>();
    private static final Map<Integer, ItemStack> HIDDEN_AXES = new HashMap<>();

    private MinoFxManager() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/mino/" + name + ".png");
    }

    public static float getHornGlow(int entityId, float partialTick) {
        Horn horn = HORNS.get(entityId);
        if (horn == null) {
            return 0.0F;
        }
        float t = Mth.clamp((horn.age + partialTick) / HORN_FADE, 0.0F, 1.0F);
        if (horn.on) {
            return t;
        }
        return 1.0F - t;
    }

    public static void handle(int type, int entityId, float[] data) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        if (type == MinoFxPacket.CHARGE_WARN && data.length >= 9) {
            ARROWS.removeIf(arrow -> arrow.entityId == entityId);
            ARROWS.add(new ChargeArrow(entityId, data[0], data[1], data[2], data[3], data[4], (int) data[5], data[6] > 0.5F, data[7], data[8]));
        } else if (type == MinoFxPacket.CHARGE_END) {
            for (ChargeArrow arrow : ARROWS) {
                if (arrow.entityId == entityId && arrow.endAge < 0) {
                    arrow.endAge = arrow.age;
                }
            }
        } else if (type == MinoFxPacket.STUN && data.length >= 1) {
            STUNS.removeIf(stun -> stun.entityId == entityId);
            STUNS.add(new Stun(entityId, Math.max(1, (int) data[0])));
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                BlockState ground = groundAt(level, entity.getX(), entity.getY(), entity.getZ());
                burstChunks(level, random, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6D, entity.getZ(), 10, 0.18D, spriteOf(ground), 1.0F, 1.0F, 1.0F);
            }
        } else if (type == MinoFxPacket.WAVE && data.length >= 6) {
            BlockState ground = groundAt(level, data[0], data[1], data[2]);
            WAVES.add(new WaveFx(data[0], data[1], data[2], data[3], data[4], (int) data[5], spriteOf(ground)));
        } else if (type == MinoFxPacket.PILLAR_WARN && data.length >= 5) {
            WARNS.add(new PillarWarn(data[0], data[1], data[2], Math.max(1, (int) data[3])));
        } else if (type == MinoFxPacket.PILLAR_RISE && data.length >= 4) {
            TextureAtlasSprite ground = spriteOf(groundAt(level, data[0], data[1], data[2]));
            burstChunks(level, random, data[0], data[1] + 0.2D, data[2], 16, 0.22D, ground, 1.0F, 1.0F, 1.0F);
            RINGS.add(new Ring(data[0], data[1] + 0.03D, data[2], 1.6F, 1, false));
        } else if (type == MinoFxPacket.PILLAR_BREAK && data.length >= 4) {
            float[] cap = MushroomPillarRenderer.RED_SIDE;
            if ((int) data[3] == 1) {
                cap = MushroomPillarRenderer.BROWN_SIDE;
            }
            float[] stem = MushroomPillarRenderer.STEM;
            burstChunks(level, random, data[0], data[1] + 2.6D, data[2], 12, 0.2D, cap[0], cap[1], cap[2], cap[3], true, 1.0F, 1.0F, 1.0F);
            for (int i = 0; i < 3; i++) {
                burstChunks(level, random, data[0], data[1] + 0.5D + i * 0.7D, data[2], 4, 0.12D, stem[0], stem[1], stem[2], stem[3], true, 1.0F, 1.0F, 1.0F);
            }
        } else if (type == MinoFxPacket.SPORE_BURST && data.length >= 5) {
            boolean big = data[4] > 0.5F;
            float radius = data[3];
            int count = 2;
            if (big) {
                count = 6;
            }
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = random.nextDouble() * radius * 0.5D;
                CLOUDS.add(new Cloud(data[0] + Math.cos(angle) * distance, data[1] + random.nextDouble() * 0.6D, data[2] + Math.sin(angle) * distance,
                    radius * (0.5F + random.nextFloat() * 0.3F), big, random.nextFloat() * 360.0F));
            }
            if (big) {
                RINGS.add(new Ring(data[0], data[1] - 0.3D, data[2], radius, 0, true));
            }
        } else if (type == MinoFxPacket.HORN && data.length >= 1) {
            boolean on = data[0] > 0.5F;
            Horn horn = HORNS.get(entityId);
            if (horn == null) {
                if (on) {
                    HORNS.put(entityId, new Horn(true));
                }
            } else if (horn.on != on) {
                float glow = getHornGlow(entityId, 0.0F);
                horn.on = on;
                if (on) {
                    horn.age = (int) (glow * HORN_FADE);
                } else {
                    horn.age = (int) ((1.0F - glow) * HORN_FADE);
                }
            }
        } else if (type == MinoFxPacket.ROAR && data.length >= 3) {
            RINGS.add(new Ring(data[0], data[1] + 0.05D, data[2], 6.0F, 2, false));
            RINGS.add(new Ring(data[0], data[1] + 0.06D, data[2], 4.0F, 3, false));
        }
    }

    private static BlockState groundAt(ClientLevel level, double x, double y, double z) {
        BlockState state = level.getBlockState(BlockPos.containing(x, y - 0.2D, z));
        if (state.isAir()) {
            state = level.getBlockState(BlockPos.containing(x, y - 1.2D, z));
        }
        if (state.isAir()) {
            state = Blocks.MYCELIUM.defaultBlockState();
        }
        return state;
    }

    private static TextureAtlasSprite spriteOf(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
    }

    private static void burstChunks(ClientLevel level, RandomSource random, double x, double y, double z, int count, double speed,
                                    TextureAtlasSprite sprite, float r, float g, float b) {
        burstChunks(level, random, x, y, z, count, speed, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), false, r, g, b);
    }

    private static void burstChunks(ClientLevel level, RandomSource random, double x, double y, double z, int count, double speed,
                                    float u0, float v0, float u1, float v1, boolean pillar, float r, float g, float b) {
        if (!ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        for (int i = 0; i < count && CHUNKS.size() < MAX_CHUNKS; i++) {
            Chunk chunk = new Chunk(x + (random.nextDouble() - 0.5D) * 0.6D, y, z + (random.nextDouble() - 0.5D) * 0.6D, u0, v0, u1, v1, pillar, random);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double horizontal = speed * (0.4D + random.nextDouble() * 0.6D);
            chunk.vx = Math.cos(angle) * horizontal;
            chunk.vy = speed * (0.8D + random.nextDouble());
            chunk.vz = Math.sin(angle) * horizontal;
            chunk.r = r;
            chunk.g = g;
            chunk.b = b;
            CHUNKS.add(chunk);
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
        RandomSource random = level.random;
        boolean decorative = ClientConfig.VFX_ENABLED.get();

        Iterator<ChargeArrow> arrows = ARROWS.iterator();
        while (arrows.hasNext()) {
            ChargeArrow arrow = arrows.next();
            arrow.age++;
            int maxAge = arrow.windup + (int) (arrow.length / Math.max(0.1F, arrow.speed)) + 40;
            if ((arrow.endAge >= 0 && arrow.age - arrow.endAge > ARROW_FADE) || arrow.age > maxAge) {
                arrows.remove();
                continue;
            }
            Entity entity = level.getEntity(arrow.entityId);
            if (decorative && arrow.endAge < 0 && entity instanceof Minotaur minotaur && minotaur.isCharging() && CHUNKS.size() < MAX_CHUNKS) {
                TextureAtlasSprite sprite = spriteOf(groundAt(level, entity.getX(), entity.getY(), entity.getZ()));
                Vec3 back = Vec3.directionFromRotation(0.0F, arrow.yaw).scale(-0.8D);
                for (int i = 0; i < 2; i++) {
                    Chunk chunk = new Chunk(entity.getX() + back.x + (random.nextDouble() - 0.5D) * entity.getBbWidth(), entity.getY() + 0.1D,
                        entity.getZ() + back.z + (random.nextDouble() - 0.5D) * entity.getBbWidth(), sprite, random);
                    chunk.vx = back.x * 0.12D + (random.nextDouble() - 0.5D) * 0.08D;
                    chunk.vy = 0.12D + random.nextDouble() * 0.12D;
                    chunk.vz = back.z * 0.12D + (random.nextDouble() - 0.5D) * 0.08D;
                    chunk.size *= 0.8F;
                    CHUNKS.add(chunk);
                }
            }
        }

        Iterator<Chunk> chunks = CHUNKS.iterator();
        while (chunks.hasNext()) {
            Chunk chunk = chunks.next();
            chunk.px = chunk.x;
            chunk.py = chunk.y;
            chunk.pz = chunk.z;
            chunk.x += chunk.vx;
            chunk.y += chunk.vy;
            chunk.z += chunk.vz;
            chunk.vy -= 0.04D;
            chunk.vx *= 0.94D;
            chunk.vz *= 0.94D;
            BlockPos pos = BlockPos.containing(chunk.x, chunk.y, chunk.z);
            if (chunk.vy < 0.0D && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                chunk.y = chunk.py;
                chunk.vy = 0.0D;
                chunk.vx *= 0.5D;
                chunk.vz *= 0.5D;
            }
            if (++chunk.age > chunk.life) {
                chunks.remove();
            }
        }

        STUNS.removeIf(stun -> ++stun.age > stun.duration + STUN_FADE || level.getEntity(stun.entityId) == null);

        Iterator<WaveFx> waves = WAVES.iterator();
        while (waves.hasNext()) {
            WaveFx wave = waves.next();
            wave.age++;
            float radius = wave.radius(0.0F);
            if (radius >= wave.maxRadius) {
                wave.doneAge++;
                if (wave.doneAge > WAVE_FADE) {
                    waves.remove();
                }
                continue;
            }
            if (decorative && radius > 0.5F && CHUNKS.size() < MAX_CHUNKS) {
                int count = Math.min(5, 1 + (int) (radius * 0.6F));
                for (int i = 0; i < count; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    Chunk chunk = new Chunk(wave.x + Math.cos(angle) * radius, wave.y + 0.15D, wave.z + Math.sin(angle) * radius, wave.sprite, random);
                    chunk.vx = Math.cos(angle) * 0.08D;
                    chunk.vy = 0.18D + random.nextDouble() * 0.12D;
                    chunk.vz = Math.sin(angle) * 0.08D;
                    CHUNKS.add(chunk);
                }
            }
        }

        Iterator<PillarWarn> warns = WARNS.iterator();
        while (warns.hasNext()) {
            PillarWarn warn = warns.next();
            warn.age++;
            if (warn.age > warn.duration + 2) {
                warns.remove();
                continue;
            }
            if (decorative && warn.age % 4 == 0) {
                burstChunks(level, random, warn.x, warn.y + 0.1D, warn.z, 2, 0.1D, spriteOf(groundAt(level, warn.x, warn.y, warn.z)), 1.0F, 1.0F, 1.0F);
            }
        }

        Iterator<Cloud> clouds = CLOUDS.iterator();
        while (clouds.hasNext()) {
            Cloud cloud = clouds.next();
            cloud.age++;
            cloud.y += 0.01D;
            int life = SMALL_CLOUD_LIFE;
            if (cloud.big) {
                life = CLOUD_LIFE;
            }
            if (cloud.age > life) {
                clouds.remove();
            }
        }

        RINGS.removeIf(ring -> ++ring.age > ring.life());

        Iterator<Map.Entry<Integer, Horn>> horns = HORNS.entrySet().iterator();
        while (horns.hasNext()) {
            Map.Entry<Integer, Horn> entry = horns.next();
            Horn horn = entry.getValue();
            horn.age++;
            horn.total++;
            if ((!horn.on && horn.age > HORN_FADE) || horn.total > HORN_TIMEOUT || level.getEntity(entry.getKey()) == null) {
                horns.remove();
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
        ARROWS.clear();
        CHUNKS.clear();
        STUNS.clear();
        WAVES.clear();
        WARNS.clear();
        CLOUDS.clear();
        RINGS.clear();
        HORNS.clear();
        HIDDEN_AXES.clear();
        ThrownAxe.CLIENT_HIDDEN_OWNERS.clear();
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Minoshroom mob)) {
            return;
        }
        if (!ThrownAxe.CLIENT_HIDDEN_OWNERS.contains(mob.getId())) {
            ItemStack stale = HIDDEN_AXES.remove(mob.getId());
            if (stale != null && mob.getMainHandItem().isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, stale);
            }
            return;
        }
        if (!HIDDEN_AXES.containsKey(mob.getId())) {
            ItemStack held = mob.getMainHandItem();
            if (!held.isEmpty()) {
                HIDDEN_AXES.put(mob.getId(), held);
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Minoshroom mob) {
            ItemStack held = HIDDEN_AXES.remove(mob.getId());
            if (held != null) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, held);
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
        if (ARROWS.isEmpty() && CHUNKS.isEmpty() && STUNS.isEmpty() && WAVES.isEmpty() && WARNS.isEmpty() && CLOUDS.isEmpty() && RINGS.isEmpty()) {
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

        for (PillarWarn warn : WARNS) {
            renderPillarWarn(poseStack, buffers, warn, partialTick);
        }
        for (ChargeArrow arrow : ARROWS) {
            renderArrow(poseStack, buffers, arrow, partialTick);
        }
        for (WaveFx wave : WAVES) {
            renderWave(poseStack, buffers, level, wave, partialTick, decorative);
        }
        for (Stun stun : STUNS) {
            renderStun(poseStack, buffers, level, stun, partialTick);
        }
        for (Ring ring : RINGS) {
            if (ring.functional || decorative) {
                renderRing(poseStack, buffers, ring, partialTick);
            }
        }
        if (decorative) {
            for (Cloud cloud : CLOUDS) {
                renderCloud(poseStack, buffers, cloud, cameraRotation, partialTick);
            }
            renderChunks(poseStack, buffers, level, partialTick);
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void flatQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a0, Vec3 a1, Vec3 b1, Vec3 b0, float u0, float u1, float v0, float v1,
                                 float r, float g, float b, float alpha) {
        FxDraw.vertex(consumer, pose, (float) a0.x, (float) a0.y, (float) a0.z, u0, v0, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) a1.x, (float) a1.y, (float) a1.z, u1, v0, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) b1.x, (float) b1.y, (float) b1.z, u1, v1, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        FxDraw.vertex(consumer, pose, (float) b0.x, (float) b0.y, (float) b0.z, u0, v1, r, g, b, alpha, FxDraw.FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
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

    private static void renderArrow(PoseStack poseStack, MultiBufferSource buffers, ChargeArrow arrow, float partialTick) {
        float age = arrow.age + partialTick;
        float appear = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
        float alpha;
        if (age < arrow.windup) {
            alpha = 0.35F + 0.45F * (age / arrow.windup);
            if (arrow.windup - age < 4.0F) {
                alpha += 0.2F * Mth.sin(age * 2.2F);
            }
        } else {
            alpha = 0.55F;
        }
        if (arrow.endAge >= 0) {
            alpha *= Mth.clamp(1.0F - (age - arrow.endAge) / ARROW_FADE, 0.0F, 1.0F);
        }
        alpha *= appear;
        if (alpha <= 0.01F) {
            return;
        }
        float r = SPORE_R;
        float g = SPORE_G;
        float b = SPORE_B;
        if (arrow.red) {
            r = RAGE_R;
            g = RAGE_G;
            b = RAGE_B;
        }
        Vec3 dir = new Vec3(-Mth.sin(arrow.yaw * Mth.DEG_TO_RAD), 0.0D, Mth.cos(arrow.yaw * Mth.DEG_TO_RAD));
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        double half = arrow.width * 0.5D + 0.1D;
        float reach = arrow.length * appear;
        Vec3 start = new Vec3(arrow.x, arrow.y + 0.04D, arrow.z);
        Vec3 end = start.add(dir.scale(reach));
        float tiles = (float) (reach / (half * 2.0D));
        float scroll = age * 0.12F;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(ARROW));
        flatQuad(consumer, poseStack.last(), start.subtract(side.scale(half)), start.add(side.scale(half)), end.add(side.scale(half)), end.subtract(side.scale(half)),
            0.0F, 1.0F, -scroll, tiles - scroll, r, g, b, alpha);
        Vec3 tip = end.add(dir.scale(half * 1.2D));
        flatQuad(consumer, poseStack.last(), end.subtract(side.scale(half * 1.6D)), end.add(side.scale(half * 1.6D)), tip.add(side.scale(0.05D)), tip.subtract(side.scale(0.05D)),
            0.06F, 0.94F, 0.0F, 0.25F, r, g, b, Math.min(1.0F, alpha * 1.4F));
    }

    private static void renderWave(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, WaveFx wave, float partialTick, boolean decorative) {
        float radius = wave.radius(partialTick);
        if (radius <= 0.05F) {
            return;
        }
        float sink = 1.0F;
        if (wave.doneAge > 0) {
            sink = Mth.clamp(1.0F - (wave.doneAge + partialTick) / WAVE_FADE, 0.0F, 1.0F);
        }
        if (decorative) {
            poseStack.pushPose();
            poseStack.translate(wave.x, wave.y + 0.03D, wave.z);
            VertexConsumer ring = buffers.getBuffer(RenderType.entityTranslucent(FAIRY_RING));
            float inner = Math.max(0.0F, radius - 1.1F);
            annulus(ring, poseStack.last(), inner, radius, Math.max(16, (int) (radius * 6.0F)), Math.max(2.0F, radius * 1.5F), 1.0F, 1.0F, 1.0F, 0.0F, 0.85F * sink);
            poseStack.popPose();
        }
        TextureAtlasSprite sprite = wave.sprite;
        float su0 = sprite.getU0();
        float sv0 = sprite.getV0();
        float su1 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * 0.5F;
        float sv1 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * 0.4F;
        VertexConsumer slabs = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        int segments = Mth.clamp((int) (radius * 5.0F), 10, 48);
        float tangent = Mth.TWO_PI * radius / segments * 0.48F;
        for (int i = 0; i < segments; i++) {
            float angle = Mth.TWO_PI * (i + 0.5F) / segments;
            double px = wave.x + Mth.cos(angle) * radius;
            double pz = wave.z + Mth.sin(angle) * radius;
            int hash = (i * 73 + wave.seed) & 7;
            float height = (0.32F + hash * 0.03F) * sink;
            if (height <= 0.01F) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(px, wave.y + 0.5D, pz));
            poseStack.pushPose();
            poseStack.translate(px, wave.y - 0.05D, pz);
            poseStack.mulPose(Axis.YP.rotation(-angle));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-28.0F - hash * 3.0F));
            FxDraw.box(slabs, poseStack.last(), -0.07F, 0.0F, -tangent, 0.07F, height, tangent, su0, sv0, su1, sv1, 1.0F, 1.0F, 1.0F, 1.0F, light);
            poseStack.popPose();
        }
    }

    private static void renderPillarWarn(PoseStack poseStack, MultiBufferSource buffers, PillarWarn warn, float partialTick) {
        float age = warn.age + partialTick;
        float t = Mth.clamp(age / warn.duration, 0.0F, 1.0F);
        float alpha = 0.4F + 0.6F * t;
        if (age > warn.duration) {
            alpha *= Mth.clamp(1.0F - (age - warn.duration) / 2.0F, 0.0F, 1.0F);
        }
        float size = 0.55F + 0.55F * t;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(CRACK));
        FxDraw.flatSquare(consumer, poseStack.last(), (float) warn.x, (float) warn.y + 0.03F, (float) warn.z, size, warn.spin, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT);
        float pulse = 0.85F + 0.15F * Mth.sin(age * 1.4F);
        FxDraw.flatSquare(consumer, poseStack.last(), (float) warn.x, (float) warn.y + 0.035F, (float) warn.z, size * 0.45F * pulse, -warn.spin, RAGE_R, 0.55F, 0.35F, alpha * 0.6F, FxDraw.FULL_BRIGHT);
    }

    private static void renderStun(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, Stun stun, float partialTick) {
        Entity entity = level.getEntity(stun.entityId);
        if (entity == null) {
            return;
        }
        float age = stun.age + partialTick;
        float scale = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        if (age > stun.duration) {
            scale *= Mth.clamp(1.0F - (age - stun.duration) / STUN_FADE, 0.0F, 1.0F);
        }
        if (scale <= 0.01F) {
            return;
        }
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() + 0.35D;
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(BITS));
        for (int i = 0; i < 3; i++) {
            float angle = age * 0.25F + i * Mth.TWO_PI / 3.0F;
            poseStack.pushPose();
            poseStack.translate(x + Mth.cos(angle) * 0.6D, y + Mth.sin(age * 0.4F + i) * 0.08D, z + Mth.sin(angle) * 0.6D);
            poseStack.mulPose(Axis.YP.rotation(-angle));
            poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F * Mth.sin(age * 0.5F + i)));
            poseStack.scale(scale, scale, scale);
            FxDraw.box(consumer, poseStack.last(), -0.04F, 0.0F, -0.04F, 0.04F, 0.14F, 0.04F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, light);
            FxDraw.box(consumer, poseStack.last(), -0.11F, 0.12F, -0.11F, 0.11F, 0.22F, 0.11F, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, light);
            poseStack.popPose();
        }
    }

    private static void renderRing(PoseStack poseStack, MultiBufferSource buffers, Ring ring, float partialTick) {
        float age = ring.age + partialTick;
        float t = Mth.clamp(age / ring.life(), 0.0F, 1.0F);
        float grow = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        float alpha = 1.0F - t;
        float r = SPORE_R;
        float g = SPORE_G;
        float b = SPORE_B;
        if (ring.kind == 1) {
            r = 0.49F;
            g = 0.43F;
            b = 0.53F;
        } else if (ring.kind == 3) {
            r = RAGE_R;
            g = 0.55F;
            b = 0.35F;
        }
        poseStack.pushPose();
        poseStack.translate(ring.x, ring.y, ring.z);
        poseStack.mulPose(Axis.YP.rotation(age * 0.04F));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SPORE_RING));
        float radius = ring.radius * grow;
        if (ring.kind == 0) {
            radius = ring.radius;
            alpha = Mth.clamp(1.0F - Math.abs(t - 0.3F) / 0.7F, 0.0F, 1.0F);
        }
        FxDraw.flatSquare(consumer, poseStack.last(), 0.0F, 0.0F, 0.0F, radius, 0.0F, r, g, b, alpha * 0.9F, FxDraw.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void renderCloud(PoseStack poseStack, MultiBufferSource buffers, Cloud cloud, Quaternionf cameraRotation, float partialTick) {
        float age = cloud.age + partialTick;
        int life = SMALL_CLOUD_LIFE;
        if (cloud.big) {
            life = CLOUD_LIFE;
        }
        float grow = Mth.clamp(age / CLOUD_GROW, 0.0F, 1.0F);
        grow = 1.0F - (1.0F - grow) * (1.0F - grow);
        float alpha = Mth.clamp((life - age) / (life * 0.5F), 0.0F, 1.0F) * 0.85F;
        if (alpha <= 0.01F) {
            return;
        }
        float size = cloud.size * (0.3F + 0.7F * grow) * (1.0F + age * 0.006F);
        poseStack.pushPose();
        poseStack.translate(cloud.x, cloud.y + 0.01D * partialTick, cloud.z);
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.ZP.rotationDegrees(cloud.rot + age * 0.8F));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SPORE_CLOUD));
        PoseStack.Pose pose = poseStack.last();
        FxDraw.vertex(consumer, pose, -size, -size, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, size, -size, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, size, size, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        FxDraw.vertex(consumer, pose, -size, size, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        poseStack.popPose();
    }

    private static void renderChunks(PoseStack poseStack, MultiBufferSource buffers, ClientLevel level, float partialTick) {
        if (CHUNKS.isEmpty()) {
            return;
        }
        for (int pass = 0; pass < 2; pass++) {
            boolean pillarPass = pass == 1;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
            if (pillarPass) {
                consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(MushroomPillarRenderer.TEXTURE));
            }
            for (Chunk chunk : CHUNKS) {
                if (chunk.pillar != pillarPass) {
                    continue;
                }
                float age = chunk.age + partialTick;
                float size = chunk.size * Mth.clamp(1.0F - (age - chunk.life * 0.6F) / (chunk.life * 0.4F), 0.0F, 1.0F);
                if (size <= 0.0F) {
                    continue;
                }
                double x = Mth.lerp(partialTick, chunk.px, chunk.x);
                double y = Mth.lerp(partialTick, chunk.py, chunk.y);
                double z = Mth.lerp(partialTick, chunk.pz, chunk.z);
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
                poseStack.pushPose();
                poseStack.translate(x, y, z);
                poseStack.mulPose(Axis.YP.rotationDegrees(age * chunk.spin));
                poseStack.mulPose(Axis.XP.rotationDegrees(age * chunk.spin * 0.6F));
                FxDraw.box(consumer, poseStack.last(), -size, -size, -size, size, size, size, chunk.u0, chunk.v0, chunk.u1, chunk.v1,
                    chunk.r, chunk.g, chunk.b, 1.0F, light);
                poseStack.popPose();
            }
        }
    }

    private static final class ChargeArrow {
        final int entityId;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float length;
        final int windup;
        final boolean red;
        final float width;
        final float speed;
        int age;
        int endAge = -1;

        ChargeArrow(int entityId, double x, double y, double z, float yaw, float length, int windup, boolean red, float width, float speed) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.length = length;
            this.windup = Math.max(1, windup);
            this.red = red;
            this.width = width;
            this.speed = speed;
        }
    }

    private static final class Chunk {
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
        int life;
        int age;
        final float u0;
        final float v0;
        final float u1;
        final float v1;
        final boolean pillar;
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;

        Chunk(double x, double y, double z, TextureAtlasSprite sprite, RandomSource random) {
            this(x, y, z, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), false, random);
        }

        Chunk(double x, double y, double z, float regionU0, float regionV0, float regionU1, float regionV1, boolean pillar, RandomSource random) {
            this.pillar = pillar;
            this.x = x;
            this.y = y;
            this.z = z;
            this.px = x;
            this.py = y;
            this.pz = z;
            this.size = 0.05F + random.nextFloat() * 0.06F;
            this.spin = (random.nextFloat() - 0.5F) * 40.0F;
            this.life = 16 + random.nextInt(10);
            float du = (regionU1 - regionU0) * 0.25F;
            float dv = (regionV1 - regionV0) * 0.25F;
            this.u0 = regionU0 + du * random.nextInt(3);
            this.v0 = regionV0 + dv * random.nextInt(3);
            this.u1 = this.u0 + du;
            this.v1 = this.v0 + dv;
        }
    }

    private static final class Stun {
        final int entityId;
        final int duration;
        int age;

        Stun(int entityId, int duration) {
            this.entityId = entityId;
            this.duration = duration;
        }
    }

    private static final class WaveFx {
        final double x;
        final double y;
        final double z;
        final float maxRadius;
        final float speed;
        final int delay;
        final TextureAtlasSprite sprite;
        final int seed;
        int age;
        int doneAge;

        WaveFx(double x, double y, double z, float maxRadius, float speed, int delay, TextureAtlasSprite sprite) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxRadius = maxRadius;
            this.speed = speed;
            this.delay = delay;
            this.sprite = sprite;
            this.seed = (int) (x * 31.0D + z * 17.0D);
        }

        float radius(float partialTick) {
            return Mth.clamp((this.age + partialTick - this.delay) * this.speed, 0.0F, this.maxRadius);
        }
    }

    private static final class PillarWarn {
        final double x;
        final double y;
        final double z;
        final int duration;
        final float spin;
        int age;

        PillarWarn(double x, double y, double z, int duration) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.duration = duration;
            this.spin = (float) ((x * 7.0D + z * 13.0D) % Mth.TWO_PI);
        }
    }

    private static final class Cloud {
        final double x;
        double y;
        final double z;
        final float size;
        final boolean big;
        final float rot;
        int age;

        Cloud(double x, double y, double z, float size, boolean big, float rot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
            this.big = big;
            this.rot = rot;
        }
    }

    private static final class Ring {
        final double x;
        final double y;
        final double z;
        final float radius;
        final int kind;
        final boolean functional;
        int age;

        Ring(double x, double y, double z, float radius, int kind, boolean functional) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.kind = kind;
            this.functional = functional;
        }

        int life() {
            if (this.kind == 2 || this.kind == 3) {
                return ROAR_LIFE;
            }
            return RING_LIFE;
        }
    }

    private static final class Horn {
        boolean on;
        int age;
        int total;

        Horn(boolean on) {
            this.on = on;
        }
    }
}
