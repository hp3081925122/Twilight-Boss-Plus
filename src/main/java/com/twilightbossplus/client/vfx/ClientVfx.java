package com.twilightbossplus.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.network.VfxPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVfx {
    private static final List<VfxEffect> EFFECTS = new ArrayList<>();
    private static final List<VfxEffect> PENDING = new ArrayList<>();
    private static final List<Mote> MOTES = new ArrayList<>();
    private static final List<float[]> SHAKES = new ArrayList<>();
    private static VfxCanvas canvas;
    private static boolean ticking;
    private static boolean showcaseCamActive;
    private static int showcaseBossId = -1;
    private static int showcaseBossKind;
    private static Vec3 showcaseCenter = Vec3.ZERO;
    private static Vec3 showcaseFront = new Vec3(0.0, 0.0, -1.0);
    private static Vec3 smoothCamPos = Vec3.ZERO;
    private static Vec3 smoothLookPos = Vec3.ZERO;
    private static boolean camInitialized;
    private static float lockedYaw;
    private static float lockedPitch;

    private ClientVfx() {
    }

    public static Vec3 vec(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "x"), tag.getDouble(key + "y"), tag.getDouble(key + "z"));
    }

    public static void handle(int type, CompoundTag d) {
        if (type == VfxPacket.SHOWCASE_HUD) {
            Minecraft.getInstance().options.hideGui = d.getBoolean("hide");
            return;
        }
        if (type == VfxPacket.SHOWCASE_CAMERA) {
            showcaseCamActive = d.getBoolean("active");
            if (showcaseCamActive) {
                int newId = d.getInt("id");
                if (newId != showcaseBossId) {
                    camInitialized = false;
                }
                showcaseBossId = newId;
                showcaseBossKind = d.getInt("kind");
                showcaseCenter = vec(d, "c");
                showcaseFront = vec(d, "f");
            } else {
                showcaseBossId = -1;
                camInitialized = false;
            }
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (type == VfxPacket.SHAKE) {
            Vec3 c = vec(d, "c");
            SHAKES.add(new float[]{(float) c.x, (float) c.y, (float) c.z, d.getFloat("s"), d.getFloat("r"), d.getInt("d"), 0.0F});
            return;
        }
        if (type >= 300 && type < 400) {
            MinoPose.record(level, type, d);
        }
        if (type >= 500 && type < 600) {
            KnightPose.record(level, type, d);
        }
        if (type >= 700 && type < 800) {
            YetiPose.record(level, type, d);
        }
        if (type >= 800 && type < 900) {
            QueenPose.record(level, type, d);
        }
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        if (type >= 800) {
            QueenVfx.handle(level, type, d);
        } else if (type >= 700) {
            YetiVfx.handle(level, type, d);
        } else if (type >= 600) {
            UrGhastVfx.handle(level, type, d);
        } else if (type >= 500) {
            KnightVfx.handle(level, type, d);
        } else if (type >= 400) {
            HydraVfx.handle(level, type, d);
        } else if (type >= 300) {
            MinoVfx.handle(level, type, d);
        } else if (type >= 200) {
            LichVfx.handle(level, type, d);
        } else if (type >= 100) {
            NagaVfx.handle(level, type, d);
        } else {
            TwilightBossPlus.debug("Client vfx ignored unknown packet type {}", type);
        }
    }

    public static void add(VfxEffect effect) {
        if (ticking) {
            PENDING.add(effect);
            return;
        }
        EFFECTS.add(effect);
    }

    public static List<VfxEffect> effects() {
        List<VfxEffect> all = new ArrayList<>(EFFECTS);
        all.addAll(PENDING);
        return all;
    }

    public static int count(int base) {
        return Math.max(1, Math.round(base * ClientConfig.QUALITY.get().scale));
    }

    public static void spawn(Mote mote) {
        if (!ClientConfig.ENABLED.get() || MOTES.size() >= ClientConfig.MAX_SHARDS.get()) {
            return;
        }
        MOTES.add(mote);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            EFFECTS.clear();
            PENDING.clear();
            MOTES.clear();
            SHAKES.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        ticking = true;
        EFFECTS.removeIf(effect -> !effect.tick(level));
        ticking = false;
        if (!PENDING.isEmpty()) {
            EFFECTS.addAll(PENDING);
            PENDING.clear();
        }
        MOTES.removeIf(mote -> !mote.tick(level));
        SHAKES.removeIf(shake -> ++shake[6] >= shake[5]);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !ClientConfig.ENABLED.get()) {
            return;
        }
        if (EFFECTS.isEmpty() && MOTES.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (canvas == null) {
            canvas = new VfxCanvas();
        }
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        canvas.begin(level, pose.last().pose(), cameraPos, camera.getLeftVector(), camera.getUpVector(), event.getPartialTick());
        for (VfxEffect effect : EFFECTS) {
            effect.render(canvas);
        }
        for (Mote mote : MOTES) {
            mote.render(canvas);
        }
        canvas.finish();
        pose.popPose();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !showcaseCamActive || showcaseBossId < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return;
        }
        Entity entity = level.getEntity(showcaseBossId);
        if (entity == null || !entity.isAlive()) {
            return;
        }
        float pt = event.renderTickTime;
        Vec3 bossPos = entity.getPosition(pt);
        float bodyYaw = entity instanceof LivingEntity living
                ? Mth.rotLerp(pt, living.yBodyRotO, living.yBodyRot)
                : entity.getViewYRot(pt);
        double rad = Math.toRadians(bodyYaw);
        Vec3 bossDir = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
        if (bossDir.dot(showcaseFront) < -0.2) {
            bossDir = showcaseFront;
        }
        Vec3 blendedFront = showcaseFront.scale(0.62).add(bossDir.scale(0.38));
        if (blendedFront.lengthSqr() < 1.0E-4) {
            blendedFront = showcaseFront;
        } else {
            blendedFront = blendedFront.normalize();
        }
        double sideAngle = Math.toRadians(15.0);
        double cosA = Math.cos(sideAngle);
        double sinA = Math.sin(sideAngle);
        Vec3 camDir = new Vec3(
                blendedFront.x * cosA - blendedFront.z * sinA,
                0.0,
                blendedFront.x * sinA + blendedFront.z * cosA
        );

        double dist = switch (showcaseBossKind) {
            case 1 -> 12.8;
            case 2 -> 13.5;
            case 3 -> 18.5;
            case 4 -> 15.0;
            case 5 -> 23.5;
            case 6 -> 14.2;
            case 7 -> 14.0;
            default -> 14.5;
        };
        double camHeight = switch (showcaseBossKind) {
            case 1 -> 4.0;
            case 2 -> 4.3;
            case 3 -> 6.0;
            case 4 -> 4.8;
            case 5 -> 7.2;
            case 6 -> 4.6;
            case 7 -> 4.8;
            default -> 4.6;
        };
        double groundY = showcaseCenter.y;
        Vec3 anchorHoriz = new Vec3(
                Mth.clamp(bossPos.x, showcaseCenter.x - 10.0, showcaseCenter.x + 10.0),
                groundY,
                Mth.clamp(bossPos.z, showcaseCenter.z - 10.0, showcaseCenter.z + 10.0)
        );
        Vec3 desiredCam = new Vec3(
                anchorHoriz.x + camDir.x * dist,
                groundY + camHeight,
                anchorHoriz.z + camDir.z * dist
        );
        double lookY = switch (showcaseBossKind) {
            case 3 -> bossPos.y + 3.0;
            case 5 -> groundY + 4.0;
            case 7 -> groundY + 2.3;
            default -> bossPos.y + entity.getBbHeight() * 0.55;
        };
        Vec3 desiredLook = new Vec3(
                bossPos.x + blendedFront.x * 1.6,
                lookY,
                bossPos.z + blendedFront.z * 1.6
        );

        if (!camInitialized) {
            smoothCamPos = desiredCam;
            smoothLookPos = desiredLook;
            camInitialized = true;
        } else {
            smoothCamPos = smoothCamPos.lerp(desiredCam, 0.12);
            smoothLookPos = smoothLookPos.lerp(desiredLook, 0.16);
        }

        double dx = smoothLookPos.x - smoothCamPos.x;
        double dy = smoothLookPos.y - smoothCamPos.y;
        double dz = smoothLookPos.z - smoothCamPos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        lockedYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        lockedPitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(1.0E-4, horiz))));

        double feetY = smoothCamPos.y - player.getEyeHeight();
        player.xo = smoothCamPos.x;
        player.xOld = smoothCamPos.x;
        player.yo = feetY;
        player.yOld = feetY;
        player.zo = smoothCamPos.z;
        player.zOld = smoothCamPos.z;
        player.setPosRaw(smoothCamPos.x, feetY, smoothCamPos.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.yRotO = lockedYaw;
        player.setYRot(lockedYaw);
        player.yHeadRotO = lockedYaw;
        player.yHeadRot = lockedYaw;
        player.xRotO = lockedPitch;
        player.setXRot(lockedPitch);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (showcaseCamActive && camInitialized) {
            event.setYaw(lockedYaw);
            event.setPitch(lockedPitch);
        }
        if (SHAKES.isEmpty() || !ClientConfig.SCREEN_SHAKE.get()) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        double strength = 0.0;
        for (float[] shake : SHAKES) {
            double dx = cam.x - shake[0];
            double dy = cam.y - shake[1];
            double dz = cam.z - shake[2];
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double falloff = Math.max(0.0, 1.0 - distance / shake[4]);
            double fade = 1.0 - shake[6] / shake[5];
            strength = Math.max(strength, shake[3] * falloff * fade);
        }
        if (strength <= 0.0) {
            return;
        }
        strength *= ClientConfig.SHAKE_STRENGTH.get() * 2.2;
        double time = (Minecraft.getInstance().level.getGameTime() + event.getPartialTick()) * 1.7;
        event.setYaw((float) (event.getYaw() + Math.sin(time * 3.1) * strength));
        event.setPitch((float) (event.getPitch() + Math.cos(time * 2.7) * strength));
        event.setRoll((float) (event.getRoll() + Math.sin(time * 4.3) * strength * 0.5));
    }
}
