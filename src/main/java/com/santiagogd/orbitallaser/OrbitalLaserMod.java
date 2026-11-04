package com.santiagogd.orbitallaser;

import com.mojang.logging.LogUtils;
import com.santiagogd.orbitallaser.entity.SatelliteEntity;
import com.santiagogd.orbitallaser.item.ModItems;
import com.santiagogd.orbitallaser.network.NetworkHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(OrbitalLaserMod.MOD_ID)
public class OrbitalLaserMod {
    public static final String MOD_ID = "orbitallaser";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<EntityType<SatelliteEntity>> SATELLITE =
            ENTITIES.register("satellite", () -> EntityType.Builder
                    .<SatelliteEntity>of(SatelliteEntity::new, MobCategory.MISC)
                    .sized(4.0f, 2.0f).clientTrackingRange(256).updateInterval(1)
                    .build("satellite"));

    public OrbitalLaserMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Orbital Laser Cinematic loading.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.register(
                        com.santiagogd.orbitallaser.cinematic.CinematicManager.class)));
    }
}
