package com.twilightbossplus;

import com.mojang.logging.LogUtils;
import com.twilightbossplus.client.vfx.ClientVfx;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.network.BossNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TwilightBossPlus.MODID)
public final class TwilightBossPlus {
    public static final String MODID = "twilightbossplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TwilightBossPlus(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, MODID + "/general.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, NagaConfig.SPEC, MODID + "/naga.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, LichConfig.SPEC, MODID + "/lich.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, MinoConfig.SPEC, MODID + "/minoshroom.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, HydraConfig.SPEC, MODID + "/hydra.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, KnightConfig.SPEC, MODID + "/knight_phantom.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, UrGhastConfig.SPEC, MODID + "/ur_ghast.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, YetiConfig.SPEC, MODID + "/alpha_yeti.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, SnowQueenConfig.SPEC, MODID + "/snow_queen.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MODID + "/client.toml");
        modEventBus.addListener(BossNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(ClientVfx::onClientTick);
            NeoForge.EVENT_BUS.addListener(ClientVfx::onRenderLevel);
            NeoForge.EVENT_BUS.addListener(ClientVfx::onCameraAngles);
        }
    }

    public static void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            LOGGER.info(message, args);
        }
    }
}
