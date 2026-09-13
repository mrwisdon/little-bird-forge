package io.github.littlebird.network;

import io.github.littlebird.entity.ground.GroundVehicleControls;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Rider client to server weapon intent for the ground vehicles: whether the trigger is held,
 * whether the reload key was pressed this tick, and how many steps to move through the weapon
 * carousel. The seat the sender occupies decides which weapon the server fires, so one packet
 * covers a tank driver, a tank gunner and the IFV crew alike.
 */
public record GroundWeaponInputPacket(boolean triggerHeld, boolean reload, int shellDelta,
                                      int weaponDelta) {
    public static void encode(GroundWeaponInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.triggerHeld());
        buffer.writeBoolean(packet.reload());
        buffer.writeVarInt(packet.shellDelta());
        buffer.writeVarInt(packet.weaponDelta());
    }

    public static GroundWeaponInputPacket decode(FriendlyByteBuf buffer) {
        return new GroundWeaponInputPacket(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(
            GroundWeaponInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof GroundVehicleControls controls) {
            controls.applyWeaponInput(player, packet.triggerHeld(), packet.reload(),
                    packet.shellDelta(), packet.weaponDelta());
        }
        context.setPacketHandled(true);
    }
}
