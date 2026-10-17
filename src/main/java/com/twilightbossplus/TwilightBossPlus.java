package com.twilightbossplus;

import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.config.KnightPhantomConfig;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.config.MinoshroomConfig;
import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.registry.ModEntities;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TwilightBossPlus.MOD_ID)
public final class TwilightBossPlus {
    public static final String MOD_ID = "twilightbossplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TwilightBossPlus(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, MOD_ID + "/general.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "/client.toml");
        context.registerConfig(ModConfig.Type.COMMON, NagaConfig.SPEC, MOD_ID + "/naga.toml");
        context.registerConfig(ModConfig.Type.COMMON, LichConfig.SPEC, MOD_ID + "/lich.toml");
        context.registerConfig(ModConfig.Type.COMMON, MinoshroomConfig.SPEC, MOD_ID + "/minoshroom.toml");
        context.registerConfig(ModConfig.Type.COMMON, HydraConfig.SPEC, MOD_ID + "/hydra.toml");
        context.registerConfig(ModConfig.Type.COMMON, KnightPhantomConfig.SPEC, MOD_ID + "/knight_phantom.toml");
        context.registerConfig(ModConfig.Type.COMMON, UrGhastConfig.SPEC, MOD_ID + "/ur_ghast.toml");
        context.registerConfig(ModConfig.Type.COMMON, AlphaYetiConfig.SPEC, MOD_ID + "/alpha_yeti.toml");
        context.registerConfig(ModConfig.Type.COMMON, SnowQueenConfig.SPEC, MOD_ID + "/snow_queen.toml");
        ModEntities.ENTITY_TYPES.register(context.getModEventBus());
        ModNetwork.register();
        LOGGER.info("[TwilightBossPlus] mod constructed");
    }
}
