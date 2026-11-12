package io.github.littlebird.network;

import io.github.littlebird.entity.Plz05Entity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Launcher elevation intent from the PHL-191's driver: the two keys are held, not tapped, so the
 * packet is only sent when one of them changes state.
 */
public record Plz05ElevationPacket(boolean up, boolean down) {
    public static void encode(Plz05ElevationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.up());
        buffer.writeBoolean(packet.down());
    }

    public static Plz05ElevationPacket decode(FriendlyByteBuf buffer) {
        return new Plz05ElevationPacket(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(
            Plz05ElevationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof Plz05Entity launcher) {
            launcher.setElevationInput(packet.up(), packet.down());
        }
        context.setPacketHandled(true);
    }
}
