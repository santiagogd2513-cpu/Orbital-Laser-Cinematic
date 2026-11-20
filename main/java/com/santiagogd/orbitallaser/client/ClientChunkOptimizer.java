package com.santiagogd.orbitallaser.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Uses Minecraft's built-in nearby-first chunk rebuild scheduling so chunks
 * around the player become ready before distant scenery.
 */
@Mod.EventBusSubscriber(modid = "orbitallaser", value = Dist.CLIENT)
public final class ClientChunkOptimizer {
    private ClientChunkOptimizer() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options == null) return;

        if (minecraft.options.prioritizeChunkUpdates().get() != PrioritizeChunkUpdates.NEARBY) {
            minecraft.options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NEARBY);
        }
    }
}
