package io.github.littlebird.network;

import io.github.littlebird.client.ImpactClientEvents;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to nearby clients: a 120mm round landed, shake this hard and draw it this brightly.
 *
 * <p>Two strengths travel in one packet because the two jobs have different ranges. The camera
 * shock stays inside a short radius, since a shake that reached the whole map would be noise. The
 * visual strength reaches much further, because vanilla particle packets stop a few dozen blocks
 * out: without it a sniper at 300 blocks watches a tracer fly off and then nothing happens. The
 * position rides along so the far client draws its own blast where the round actually landed.
 */
public record MainGunImpactPacket(
        float x, float y, float z, float intensity, float visualStrength) {
    public static void encode(MainGunImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.x());
        buffer.writeFloat(packet.y());
        buffer.writeFloat(packet.z());
        buffer.writeFloat(packet.intensity());
        buffer.writeFloat(packet.visualStrength());
    }

    public static MainGunImpactPacket decode(FriendlyByteBuf buffer) {
        return new MainGunImpactPacket(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(
            MainGunImpactPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // Client only, the same split the hit marker uses: the server never handles this direction.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ImpactClientEvents.localImpact(packet.x(), packet.y(), packet.z(),
                        packet.intensity(), packet.visualStrength()));
        context.setPacketHandled(true);
    }
}
