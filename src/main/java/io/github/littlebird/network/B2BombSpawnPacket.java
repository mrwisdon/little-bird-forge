package io.github.littlebird.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to pilot: the bomb that just left the bay, so the client can put the chase camera on it.
 *
 * <p>Naming the entity is the only thing the client cannot work out for itself - a stick of two
 * bombs six ticks apart would otherwise have the camera guessing which one to follow.
 */
public record B2BombSpawnPacket(int entityId) {
    public static void encode(B2BombSpawnPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static B2BombSpawnPacket decode(FriendlyByteBuf buffer) {
        return new B2BombSpawnPacket(buffer.readVarInt());
    }

    public static void handle(
            B2BombSpawnPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> io.github.littlebird.client.B2BombCam.follow(packet.entityId())));
        context.setPacketHandled(true);
    }
}
