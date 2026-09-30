package io.github.littlebird.network;

import io.github.littlebird.entity.JetEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Jet pilot client to server: the attitude and engine state that
 * {@code ServerboundMoveVehiclePacket} cannot carry (roll, throttle, burner charge and hover
 * mode). The server validates the sender really is the controlling passenger before republishing
 * the values through synched entity data.
 */
public record JetControlPacket(
        float rollDeg,
        float throttle,
        float afterburnerCharge,
        boolean vtol,
        boolean engineOn) {
    public static void encode(JetControlPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.rollDeg);
        buffer.writeFloat(packet.throttle);
        buffer.writeFloat(packet.afterburnerCharge);
        buffer.writeBoolean(packet.vtol);
        buffer.writeBoolean(packet.engineOn);
    }

    public static JetControlPacket decode(FriendlyByteBuf buffer) {
        return new JetControlPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    public static void handle(
            JetControlPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof JetEntity jet) {
            jet.applyClientControl(packet.rollDeg(), packet.throttle(),
                    packet.afterburnerCharge(), packet.vtol(), packet.engineOn(), player);
        }
        context.setPacketHandled(true);
    }
}
