package io.github.littlebird.network;

import io.github.littlebird.entity.B2Entity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Weapon switch in a B-2: 0 = JDAM, 1 = incendiary. The value is clamped server side, because the
 * B-2 has no cannon - anything a client invents falls back to the JDAM bay.
 */
public record B2WeaponSelectPacket(int weapon) {
    public static void encode(B2WeaponSelectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.weapon());
    }

    public static B2WeaponSelectPacket decode(FriendlyByteBuf buffer) {
        return new B2WeaponSelectPacket(buffer.readVarInt());
    }

    public static void handle(
            B2WeaponSelectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof B2Entity bomber) {
            bomber.setBombWeapon(packet.weapon());
        }
        context.setPacketHandled(true);
    }
}
