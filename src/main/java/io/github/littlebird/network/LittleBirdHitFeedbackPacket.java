package io.github.littlebird.network;

import io.github.littlebird.client.GunshipHudDraw;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to pilot: the guns connected, so the client can flash a hit marker and, when the
 * target was an airframe, show how much of it is left.
 */
public record LittleBirdHitFeedbackPacket(boolean kill, boolean aircraft, float healthFraction) {
    public static void encode(LittleBirdHitFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.kill());
        buffer.writeBoolean(packet.aircraft());
        buffer.writeFloat(packet.healthFraction());
    }

    public static LittleBirdHitFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdHitFeedbackPacket(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readFloat());
    }

    public static void handle(
            LittleBirdHitFeedbackPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GunshipHudDraw.onHitFeedback(packet.kill(), packet.aircraft(),
                        packet.healthFraction()));
        context.setPacketHandled(true);
    }
}
