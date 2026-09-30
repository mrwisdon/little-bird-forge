package io.github.littlebird.network;

import io.github.littlebird.entity.JetEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** One press of the flare key: the server spends a decoy only if the sender is really flying. */
public record JetFlarePacket() {
    public static void encode(JetFlarePacket packet, FriendlyByteBuf buffer) {
    }

    public static JetFlarePacket decode(FriendlyByteBuf buffer) {
        return new JetFlarePacket();
    }

    public static void handle(
            JetFlarePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof JetEntity jet) {
            jet.applyFlare(player);
        }
        context.setPacketHandled(true);
    }
}
