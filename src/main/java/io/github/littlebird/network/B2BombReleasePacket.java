package io.github.littlebird.network;

import io.github.littlebird.entity.B2Entity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * One press of the bomb release key in a B-2: single or salvo. The server only spends bombs if
 * the sender is really the pilot of a B-2, the same guard every other aircraft packet uses.
 */
public record B2BombReleasePacket(boolean salvo) {
    public static void encode(B2BombReleasePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.salvo());
    }

    public static B2BombReleasePacket decode(FriendlyByteBuf buffer) {
        return new B2BombReleasePacket(buffer.readBoolean());
    }

    public static void handle(
            B2BombReleasePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof B2Entity bomber) {
            bomber.applyBombRelease(player, packet.salvo());
        }
        context.setPacketHandled(true);
    }
}
