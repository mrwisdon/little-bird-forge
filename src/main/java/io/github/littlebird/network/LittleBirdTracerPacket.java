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
        int impactType, int station, int ownerId) {

    /**
     * Tracer with no known owner: the client draws it exactly as the server measured it. Shots from
     * a hull the client is riding carry the owner so the client can start the line at the muzzle it
     * is actually drawing.
     */
    public LittleBirdTracerPacket(double startX, double startY, double startZ, double endX,
                                  double endY, double endZ, int impactType, int station) {
        this(startX, startY, startZ, endX, endY, endZ, impactType, station, -1);
    }

    public static final int STATION_CANISTER = 2;

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
        buffer.writeInt(packet.ownerId());
    }

    public static LittleBirdTracerPacket decode(FriendlyByteBuf buffer) {
        double startX = buffer.readDouble();
        double startY = buffer.readDouble();
        double startZ = buffer.readDouble();
        double endX = buffer.readDouble();
        double endY = buffer.readDouble();
        double endZ = buffer.readDouble();
        int impact = sanitizeImpact(buffer.readUnsignedByte());
        int station = buffer.readUnsignedByte();
        int ownerId = buffer.readInt();
        return new LittleBirdTracerPacket(startX, startY, startZ, endX, endY, endZ,
                impact, station <= STATION_CANISTER ? station : 1, ownerId);
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