package io.github.littlebird.network;

import io.github.littlebird.entity.LittleBirdEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Pilot client to server dive-crash report. The client is the only place that knows how the
 * airframe met the ground, so it says so and the server validates the sender before writing
 * the aircraft off.
 */
public record LittleBirdCrashPacket() {
    public static void encode(LittleBirdCrashPacket packet, FriendlyByteBuf buffer) {
    }

    public static LittleBirdCrashPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdCrashPacket();
    }

    public static void handle(
            LittleBirdCrashPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof LittleBirdEntity helicopter) {
            helicopter.applyCrash(player);
        } else if (player != null
                && player.getVehicle() instanceof io.github.littlebird.entity.ApacheEntity apache) {
            apache.applyCrash(player);
        }
        context.setPacketHandled(true);
    }
}
