package io.github.littlebird.network;

import io.github.littlebird.entity.LittleBirdEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Pilot client to server crash report; the server validates it before damage. */
public record LittleBirdImpactPacket(float speed) {
    public static void encode(LittleBirdImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.speed());
    }

    public static LittleBirdImpactPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdImpactPacket(buffer.readFloat());
    }

    public static void handle(
            LittleBirdImpactPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof LittleBirdEntity helicopter) {
            helicopter.applyImpact(player, packet.speed());
        } else if (player != null
                && player.getVehicle() instanceof io.github.littlebird.entity.ApacheEntity apache) {
            apache.applyImpact(player, packet.speed());
        } else if (player != null
                && player.getVehicle() instanceof io.github.littlebird.entity.JetEntity jet) {
            jet.applyImpact(player, packet.speed());
        }
        context.setPacketHandled(true);
    }
}
