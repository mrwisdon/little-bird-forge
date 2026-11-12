package io.github.littlebird.network;

import io.github.littlebird.entity.ArtilleryPlatform;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * A fire mission called from an artillery radar screen. The crew picks a point,
 * the client sends the two world coordinates and the server decides whether the battery answers.
 *
 * <p>The client's coordinates are only ever a request. The gun checks that the caller is really its
 * driver, that the point is inside the radar picture, that no mission is already running and that
 * the cooldown has expired; a non-finite point is dropped outright, the same way the B-2 pod
 * designation packet treats its own input.
 */
public record ArtilleryStrikeCallPacket(double x, double z) {
    public static void encode(ArtilleryStrikeCallPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x());
        buffer.writeDouble(packet.z());
    }

    public static ArtilleryStrikeCallPacket decode(FriendlyByteBuf buffer) {
        return new ArtilleryStrikeCallPacket(buffer.readDouble(), buffer.readDouble());
    }

    /** Client side: ask for a fire mission on this world point. */
    public static void send(double x, double z) {
        ModNetwork.CHANNEL.sendToServer(new ArtilleryStrikeCallPacket(x, z));
    }

    public static void handle(
            ArtilleryStrikeCallPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof ArtilleryPlatform artillery) {
            artillery.strike().request(player, packet.x(), packet.z());
        }
        context.setPacketHandled(true);
    }
}
