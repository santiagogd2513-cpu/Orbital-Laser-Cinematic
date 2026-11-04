package com.santiagogd.orbitallaser.network;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OrbitalLaserMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        // Packets will be registered here later:
        // - StartCinematicPacket
        // - SyncSatelliteStatePacket
        // - BeamImpactPacket
        OrbitalLaserMod.LOGGER.info("NetworkHandler registered");
    }
}