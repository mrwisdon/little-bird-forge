package io.github.littlebird.network;

import io.github.littlebird.entity.LittleBirdEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Pilot client to server: current roll attitude and engine request. Roll is not
 * carried by ServerboundMoveVehiclePacket, so it needs its own channel; the
 * server validates the sender really is the controlling passenger and then
 * republishes the value through synched entity data.
 */
public record LittleBirdControlPacket(float rollDeg, boolean engineOn) {
    public static void encode(LittleBirdControlPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.rollDeg);
        buffer.writeBoolean(packet.engineOn);
    }

    public static LittleBirdControlPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdControlPacket(buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(
            LittleBirdControlPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }
        if (player.getVehicle() instanceof LittleBirdEntity helicopter) {
            helicopter.applyClientControl(packet.rollDeg(), packet.engineOn(), player);
        } else if (player.getVehicle() instanceof io.github.littlebird.entity.ApacheEntity apache) {
            apache.applyClientControl(packet.rollDeg(), packet.engineOn(), player);
        }
        context.setPacketHandled(true);
    }
}
