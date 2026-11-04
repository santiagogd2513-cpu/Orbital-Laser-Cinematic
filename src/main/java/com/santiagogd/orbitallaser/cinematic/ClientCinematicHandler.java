package com.santiagogd.orbitallaser.cinematic;

import com.santiagogd.orbitallaser.network.StartCinematicS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientCinematicHandler {
    private ClientCinematicHandler() {}
    public static void startFromItem(Player player, Vec3 target) {
        if (CinematicManager.isPlaying()) {
            player.displayClientMessage(Component.literal("§cYa hay una cinemática en curso."), true);
            return;
        }
        CinematicManager.startCinematic(target);
        player.displayClientMessage(Component.literal("§b§lObjetivo marcado§r §7→ Iniciando cinemática orbital..."), true);
    }

    public static void handle(StartCinematicS2CPacket packet) {
        if (packet.start()) CinematicManager.startCinematic(packet.target());
        else CinematicManager.stopCinematic();
    }
}
