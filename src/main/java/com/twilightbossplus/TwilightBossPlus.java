package com.twilightbossplus;

import com.mojang.logging.LogUtils;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.network.BossNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(TwilightBossPlus.MODID)
public final class TwilightBossPlus {
    public static final String MODID = "twilightbossplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TwilightBossPlus() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, MODID + "/general.toml");
        context.registerConfig(ModConfig.Type.COMMON, NagaConfig.SPEC, MODID + "/naga.toml");
        context.registerConfig(ModConfig.Type.COMMON, LichConfig.SPEC, MODID + "/lich.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MODID + "/client.toml");
        BossNetwork.register();
    }

    public static void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            LOGGER.info(message, args);
        }
    }
}
