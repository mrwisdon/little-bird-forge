package io.github.littlebird.network;

import io.github.littlebird.entity.ApacheEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** One press of the seat switch key while aboard an AH-64 (rear seat <-> front seat). */
public record ApacheSeatSwitchPacket() {
    public static void encode(ApacheSeatSwitchPacket packet, FriendlyByteBuf buffer) {
    }

    public static ApacheSeatSwitchPacket decode(FriendlyByteBuf buffer) {
        return new ApacheSeatSwitchPacket();
    }

    public static void handle(
            ApacheSeatSwitchPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof ApacheEntity apache) {
            apache.switchSeats(player);
        }
        context.setPacketHandled(true);
    }
}
