package io.github.littlebird.network;

import io.github.littlebird.entity.GunshipRider;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** One press of the dedicated dismount key while crewing any vehicle in the pack. */
public record LittleBirdExitPacket() {
    public static void encode(LittleBirdExitPacket packet, FriendlyByteBuf buffer) {
    }

    public static LittleBirdExitPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdExitPacket();
    }

    public static void handle(
            LittleBirdExitPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof GunshipRider) {
            player.stopRiding();
        }
        context.setPacketHandled(true);
    }
}
