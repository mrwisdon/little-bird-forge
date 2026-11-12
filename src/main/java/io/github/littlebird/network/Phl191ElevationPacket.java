package io.github.littlebird.network;

import io.github.littlebird.entity.Phl191Entity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Launcher elevation intent from the PHL-191's driver: the two keys are held, not tapped, so the
 * packet is only sent when one of them changes state.
 */
public record Phl191ElevationPacket(boolean up, boolean down) {
    public static void encode(Phl191ElevationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.up());
        buffer.writeBoolean(packet.down());
    }

    public static Phl191ElevationPacket decode(FriendlyByteBuf buffer) {
        return new Phl191ElevationPacket(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(
            Phl191ElevationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof Phl191Entity launcher) {
            launcher.setElevationInput(packet.up(), packet.down());
        }
        context.setPacketHandled(true);
    }
}
