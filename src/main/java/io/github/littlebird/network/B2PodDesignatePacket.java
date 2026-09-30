package io.github.littlebird.network;

import io.github.littlebird.entity.B2Entity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Targeting pod designation from the back of a B-2: the client ray-traces its crosshair and sends
 * the world point it hit. {@code clear} wipes the designation instead of setting one, so the same
 * packet covers both edges of the key.
 *
 * <p>The server never trusts the client's aim beyond this: the point is only ever used as a
 * convergence target for a guided bomb, and a non-finite point is dropped outright.
 */
public record B2PodDesignatePacket(double x, double y, double z, boolean clear) {
    public static void encode(B2PodDesignatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.clear());
        buffer.writeDouble(packet.x());
        buffer.writeDouble(packet.y());
        buffer.writeDouble(packet.z());
    }

    public static B2PodDesignatePacket decode(FriendlyByteBuf buffer) {
        boolean clear = buffer.readBoolean();
        return new B2PodDesignatePacket(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), clear);
    }

    public static void handle(
            B2PodDesignatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof B2Entity bomber) {
            if (packet.clear()) {
                bomber.clearPodDesignation();
            } else if (Double.isFinite(packet.x())
                    && Double.isFinite(packet.y())
                    && Double.isFinite(packet.z())) {
                bomber.setPodDesignation(packet.x(), packet.y(), packet.z());
            }
        }
        context.setPacketHandled(true);
    }
}
