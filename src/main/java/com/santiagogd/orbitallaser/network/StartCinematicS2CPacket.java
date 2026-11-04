package com.santiagogd.orbitallaser.network;

import com.santiagogd.orbitallaser.cinematic.ClientCinematicHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record StartCinematicS2CPacket(double x, double y, double z, boolean start) {
    public static void encode(StartCinematicS2CPacket message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeBoolean(message.start);
    }
    public static StartCinematicS2CPacket decode(FriendlyByteBuf buffer) {
        return new StartCinematicS2CPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean());
    }
    public static void handle(StartCinematicS2CPacket message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientCinematicHandler.handle(message)));
        ctx.setPacketHandled(true);
    }
    public Vec3 target() { return new Vec3(x, y, z); }
}
