package io.github.littlebird.network;

import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.LittleBirdEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * One press of the reload key: the Apache tops both magazines up from their reserves, the
 * Little Bird moves reserve rockets into its pods. The server checks the sender really is
 * flying before spending anything.
 */
public record ApacheReloadPacket() {
    public static void encode(ApacheReloadPacket packet, FriendlyByteBuf buffer) {
    }

    public static ApacheReloadPacket decode(FriendlyByteBuf buffer) {
        return new ApacheReloadPacket();
    }

    public static void handle(
            ApacheReloadPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof ApacheEntity apache) {
            apache.applyReload(player);
        } else if (player != null && player.getVehicle() instanceof LittleBirdEntity bird) {
            bird.applyReload(player);
        } else if (player != null
                && player.getVehicle() instanceof io.github.littlebird.entity.JetEntity jet) {
            jet.applyReload(player);
        }
        context.setPacketHandled(true);
    }
}
