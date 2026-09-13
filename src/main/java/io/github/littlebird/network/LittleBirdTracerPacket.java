package io.github.littlebird.network;

import io.github.littlebird.client.LittleBirdTracerRenderer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** One S2C bullet trace: where it left the barrel and where it stopped. */
public record LittleBirdTracerPacket(
        double startX, double startY, double startZ,
        double endX, double endY, double endZ,
        int impactType, int station) {

    public static final int IMPACT_MISS = 0;
    public static final int IMPACT_BLOCK = 1;
    public static final int IMPACT_ENTITY = 2;

    public static void encode(LittleBirdTracerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.startX());
        buffer.writeDouble(packet.startY());
        buffer.writeDouble(packet.startZ());
        buffer.writeDouble(packet.endX());
        buffer.writeDouble(packet.endY());
        buffer.writeDouble(packet.endZ());
        buffer.writeByte(packet.impactType());
        buffer.writeByte(packet.station());
    }

    public static LittleBirdTracerPacket decode(FriendlyByteBuf buffer) {
        return new LittleBirdTracerPacket(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                sanitizeImpact(buffer.readUnsignedByte()),
                Math.max(0, Math.min(1, buffer.readUnsignedByte())));
    }

    private static int sanitizeImpact(int impactType) {
        return impactType >= IMPACT_MISS && impactType <= IMPACT_ENTITY
                ? impactType : IMPACT_MISS;
    }

    public static void handle(
            LittleBirdTracerPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> LittleBirdTracerRenderer.add(packet));
        context.setPacketHandled(true);
    }
}