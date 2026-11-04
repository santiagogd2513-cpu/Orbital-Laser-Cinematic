package com.santiagogd.orbitallaser.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.santiagogd.orbitallaser.cinematic.CinematicManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StartCinematicCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("orbitallaser")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start")
                                .executes(StartCinematicCommand::startAtPlayer)
                        )
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    CinematicManager.stopCinematic();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Cinematic stopped."), true);
                                    return 1;
                                })
                        )
        );
    }

    private static int startAtPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            Vec3 target = player.position();

            // On client side the cinematic is handled by CinematicManager
            // For now we just trigger it locally when the command is run in singleplayer / integrated
            CinematicManager.startCinematic(target);

            source.sendSuccess(() -> Component.literal(
                    "§b§lOrbital Laser Cinematic started§r §7(3 minutes)§r\n" +
                    "Camera will leave the planet and show the satellite charge-up + beam strike."
            ), true);
            return 1;
        }

        source.sendFailure(Component.literal("This command must be run by a player."));
        return 0;
    }
}