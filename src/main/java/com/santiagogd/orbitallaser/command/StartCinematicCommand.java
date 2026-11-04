package com.santiagogd.orbitallaser.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.santiagogd.orbitallaser.network.NetworkHandler;
import com.santiagogd.orbitallaser.network.StartCinematicS2CPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StartCinematicCommand {
    private StartCinematicCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("orbitallaser")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(StartCinematicCommand::start))
                .then(Commands.literal("stop").executes(StartCinematicCommand::stop)));
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Este comando debe ejecutarse como jugador."));
            return 0;
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StartCinematicS2CPacket(player.getX(), player.getY(), player.getZ(), true));
        context.getSource().sendSuccess(() -> Component.literal("Cinemática orbital iniciada."), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Este comando debe ejecutarse como jugador."));
            return 0;
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StartCinematicS2CPacket(0, 0, 0, false));
        context.getSource().sendSuccess(() -> Component.literal("Cinemática orbital detenida."), false);
        return 1;
    }
}
