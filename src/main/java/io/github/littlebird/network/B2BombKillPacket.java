package io.github.littlebird.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to crew: what the bomb that just went off killed.
 *
 * <p>The release id groups a stick, so the two bombs of a salvo land on one line of the feed.
 */
public record B2BombKillPacket(int releaseId, int weapon, List<String> names) {
    /** Enough for any realistic blast, and it bounds the packet. */
    private static final int MAX_NAMES = 12;
    private static final int MAX_NAME_LENGTH = 48;

    public B2BombKillPacket {
        names = names == null ? List.of() : List.copyOf(names);
    }

    public static void encode(B2BombKillPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.releaseId());
        buffer.writeVarInt(packet.weapon());
        buffer.writeVarInt(packet.names().size());
        for (String name : packet.names()) {
            buffer.writeUtf(name, MAX_NAME_LENGTH);
        }
    }

    public static B2BombKillPacket decode(FriendlyByteBuf buffer) {
        int releaseId = buffer.readVarInt();
        int weapon = buffer.readVarInt();
        int count = Math.min(MAX_NAMES, Math.max(0, buffer.readVarInt()));
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add(buffer.readUtf(MAX_NAME_LENGTH));
        }
        return new B2BombKillPacket(releaseId, weapon, names);
    }

    public static void handle(
            B2BombKillPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> io.github.littlebird.client.B2KillFeed.report(packet.releaseId(),
                        packet.weapon(), packet.names())));
        context.setPacketHandled(true);
    }
}
