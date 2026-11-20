package com.santiagogd.orbitallaser.server;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

/**
 * Requests the next chunk in the player's travel direction ahead of time.
 * Vanilla/Forge's chunk pipeline performs the actual asynchronous loading.
 */
@Mod.EventBusSubscriber(modid = OrbitalLaserMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BackgroundChunkPrefetch {
    private static final TicketType<ChunkPos> PREFETCH_TICKET =
            TicketType.create("orbitallaser:background_prefetch",
                    Comparator.comparingLong(ChunkPos::toLong), 40);

    private BackgroundChunkPrefetch() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        Vec3 motion = player.getDeltaMovement();
        Vec3 direction = new Vec3(motion.x, 0.0, motion.z);
        if (direction.lengthSqr() < 0.0004) {
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x, 0.0, look.z);
        }
        if (direction.lengthSqr() < 0.0004) return;

        direction = direction.normalize();
        int aheadX = net.minecraft.core.BlockPos.containing(
                player.getX() + direction.x * 24.0, player.getY(),
                player.getZ() + direction.z * 24.0).getX() >> 4;
        int aheadZ = net.minecraft.core.BlockPos.containing(
                player.getX() + direction.x * 24.0, player.getY(),
                player.getZ() + direction.z * 24.0).getZ() >> 4;

        ChunkPos target = new ChunkPos(aheadX, aheadZ);
        player.serverLevel().getChunkSource().addRegionTicket(PREFETCH_TICKET, target, 2, target);
    }
}
