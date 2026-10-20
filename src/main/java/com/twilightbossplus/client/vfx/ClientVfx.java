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
import net.minecraft.nbt.CompoundTag;
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

    private ClientVfx() {
    }

    public static Vec3 vec(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "x"), tag.getDouble(key + "y"), tag.getDouble(key + "z"));
    }

    public static void handle(int type, CompoundTag d) {
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
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        if (type >= 400) {
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
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
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
