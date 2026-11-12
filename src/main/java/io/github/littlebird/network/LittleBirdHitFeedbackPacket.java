package io.github.littlebird.network;

import io.github.littlebird.client.hud.HudFeedback;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to shooter: the guns connected, so the client can flash a hit marker and print the
 * target's name with its remaining integrity for a few seconds.
 */
public record LittleBirdHitFeedbackPacket(boolean kill, String name, float health, float maxHealth) {
    public static void encode(LittleBirdHitFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.kill());
        buffer.writeUtf(packet.name() == null ? "" : packet.name());
        buffer.writeFloat(packet.health());
        buffer.writeFloat(packet.maxHealth());
    }

    public static LittleBirdHitFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdHitFeedbackPacket(buffer.readBoolean(), buffer.readUtf(),
                buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(
            LittleBirdHitFeedbackPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HudFeedback.onHitFeedback(packet.kill(), packet.name(),
                        packet.health(), packet.maxHealth()));
        context.setPacketHandled(true);
    }
}
