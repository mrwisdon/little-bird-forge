package io.github.littlebird.network;

import io.github.littlebird.entity.MainBattleTankEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** One press of the seat switch key while aboard a main battle tank (driver <-> cupola). */
public record TankSeatSwitchPacket() {
    public static void encode(TankSeatSwitchPacket packet, FriendlyByteBuf buffer) {
    }

    public static TankSeatSwitchPacket decode(FriendlyByteBuf buffer) {
        return new TankSeatSwitchPacket();
    }

    public static void handle(
            TankSeatSwitchPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof MainBattleTankEntity tank) {
            tank.switchSeats(player);
        }
        context.setPacketHandled(true);
    }
}
