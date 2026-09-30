package io.github.littlebird.network;

import io.github.littlebird.entity.LittleBirdEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Pilot client to server weapon intent: selected weapon, salvo mode and whether
 * the trigger is held. Sent only when one of those changes.
 */
public record LittleBirdWeaponInputPacket(int weapon, boolean salvoMode, boolean triggerHeld) {
    public static void encode(LittleBirdWeaponInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.weapon);
        buffer.writeBoolean(packet.salvoMode);
        buffer.writeBoolean(packet.triggerHeld);
    }

    public static LittleBirdWeaponInputPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdWeaponInputPacket(
                buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(
            LittleBirdWeaponInputPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof LittleBirdEntity helicopter) {
            helicopter.applyWeaponInput(
                    player, packet.weapon(), packet.salvoMode(), packet.triggerHeld());
        } else if (player != null
                && player.getVehicle() instanceof io.github.littlebird.entity.ApacheEntity apache) {
            apache.applyWeaponInput(player, packet.weapon(), packet.triggerHeld());
        } else if (player != null
                && player.getVehicle() instanceof io.github.littlebird.entity.JetEntity jet) {
            jet.applyWeaponInput(
                    player, packet.weapon(), packet.salvoMode(), packet.triggerHeld());
        }
        context.setPacketHandled(true);
    }
}
