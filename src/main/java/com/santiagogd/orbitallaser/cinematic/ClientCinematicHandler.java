package com.santiagogd.orbitallaser.cinematic;

import com.santiagogd.orbitallaser.network.StartCinematicS2CPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientCinematicHandler {
    private ClientCinematicHandler() {}
    public static void handle(StartCinematicS2CPacket packet) {
        if (packet.start()) CinematicManager.startCinematic(packet.target());
        else CinematicManager.stopCinematic();
    }
}
