package com.twilightbossplus.registry;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.entity.MushroomPillar;
import com.twilightbossplus.entity.PrimalTear;
import com.twilightbossplus.entity.YetiSnowball;
import com.twilightbossplus.entity.Puffball;
import com.twilightbossplus.entity.SoulCandle;
import com.twilightbossplus.entity.ThrownAxe;
import com.twilightbossplus.entity.VenomGlob;
import com.twilightbossplus.entity.VenomPool;
import com.twilightbossplus.entity.VoidOrb;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TwilightBossPlus.MOD_ID);

    public static final RegistryObject<EntityType<VenomGlob>> VENOM_GLOB = ENTITY_TYPES.register("venom_glob",
        () -> EntityType.Builder.<VenomGlob>of(VenomGlob::new, MobCategory.MISC)
            .sized(0.7F, 0.7F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build("venom_glob"));

    public static final RegistryObject<EntityType<VenomPool>> VENOM_POOL = ENTITY_TYPES.register("venom_pool",
        () -> EntityType.Builder.<VenomPool>of(VenomPool::new, MobCategory.MISC)
            .sized(VenomPool.RADIUS * 2.0F, 0.3F)
            .clientTrackingRange(8)
            .updateInterval(20)
            .fireImmune()
            .build("venom_pool"));

    public static final RegistryObject<EntityType<VoidOrb>> VOID_ORB = ENTITY_TYPES.register("void_orb",
        () -> EntityType.Builder.<VoidOrb>of(VoidOrb::new, MobCategory.MISC)
            .sized(0.8F, 0.8F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .fireImmune()
            .build("void_orb"));

    public static final RegistryObject<EntityType<SoulCandle>> SOUL_CANDLE = ENTITY_TYPES.register("soul_candle",
        () -> EntityType.Builder.<SoulCandle>of(SoulCandle::new, MobCategory.MISC)
            .sized(0.5F, 0.9F)
            .clientTrackingRange(8)
            .updateInterval(20)
            .fireImmune()
            .build("soul_candle"));

    public static final RegistryObject<EntityType<MushroomPillar>> MUSHROOM_PILLAR = ENTITY_TYPES.register("mushroom_pillar",
        () -> EntityType.Builder.<MushroomPillar>of(MushroomPillar::new, MobCategory.MISC)
            .sized(MushroomPillar.WIDTH, MushroomPillar.HEIGHT)
            .clientTrackingRange(8)
            .updateInterval(20)
            .fireImmune()
            .build("mushroom_pillar"));

    public static final RegistryObject<EntityType<Puffball>> PUFFBALL = ENTITY_TYPES.register("puffball",
        () -> EntityType.Builder.<Puffball>of(Puffball::new, MobCategory.MISC)
            .sized(0.7F, 0.7F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build("puffball"));

    public static final RegistryObject<EntityType<ThrownAxe>> THROWN_AXE = ENTITY_TYPES.register("thrown_axe",
        () -> EntityType.Builder.<ThrownAxe>of(ThrownAxe::new, MobCategory.MISC)
            .sized(0.8F, 0.8F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .fireImmune()
            .build("thrown_axe"));

    public static final RegistryObject<EntityType<PrimalTear>> PRIMAL_TEAR = ENTITY_TYPES.register("primal_tear",
        () -> EntityType.Builder.<PrimalTear>of(PrimalTear::new, MobCategory.MISC)
            .sized(1.6F, 1.6F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .fireImmune()
            .build("primal_tear"));
    public static final RegistryObject<EntityType<YetiSnowball>> YETI_SNOWBALL = ENTITY_TYPES.register("yeti_snowball",
        () -> EntityType.Builder.<YetiSnowball>of(YetiSnowball::new, MobCategory.MISC)
            .sized(2.2F, 2.2F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build("yeti_snowball"));

    private ModEntities() {
    }
}
