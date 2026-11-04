package com.santiagogd.orbitallaser.network;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OrbitalLaserMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int id;
    private NetworkHandler() {}
    public static void register() {
        CHANNEL.registerMessage(id++, StartCinematicS2CPacket.class,
                StartCinematicS2CPacket::encode, StartCinematicS2CPacket::decode,
                StartCinematicS2CPacket::handle);
        OrbitalLaserMod.LOGGER.info("Orbital cinematic network channel registered.");
    }
}
