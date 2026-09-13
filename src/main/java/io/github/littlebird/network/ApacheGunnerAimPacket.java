package io.github.littlebird.network;

import io.github.littlebird.entity.ApacheEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Front seat to server: where the gunner is looking, so the server can slew the chain gun
 * turret onto it. The turret itself stays server authoritative and is republished through
 * synched entity data for everyone who can see the aircraft.
 */
public record ApacheGunnerAimPacket(float yawDeg, float pitchDeg, boolean triggerHeld) {
    public static void encode(ApacheGunnerAimPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.yawDeg());
        buffer.writeFloat(packet.pitchDeg());
        buffer.writeBoolean(packet.triggerHeld());
    }

    public static ApacheGunnerAimPacket decode(FriendlyByteBuf buffer) {
        return new ApacheGunnerAimPacket(
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(
            ApacheGunnerAimPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof ApacheEntity apache) {
            apache.applyGunnerAim(player, packet.yawDeg(), packet.pitchDeg(),
                    packet.triggerHeld());
        }
        context.setPacketHandled(true);
    }
}
