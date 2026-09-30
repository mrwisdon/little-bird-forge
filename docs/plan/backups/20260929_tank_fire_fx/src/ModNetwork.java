package io.github.littlebird.network;

import io.github.littlebird.LittleBird;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(LittleBirdControlPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LittleBirdControlPacket::encode)
                .decoder(LittleBirdControlPacket::decode)
                .consumerMainThread(LittleBirdControlPacket::handle)
                .add();
        CHANNEL.messageBuilder(LittleBirdExitPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LittleBirdExitPacket::encode)
                .decoder(LittleBirdExitPacket::decode)
                .consumerMainThread(LittleBirdExitPacket::handle)
                .add();
        CHANNEL.messageBuilder(LittleBirdWeaponInputPacket.class, 2,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(LittleBirdWeaponInputPacket::encode)
                .decoder(LittleBirdWeaponInputPacket::decode)
                .consumerMainThread(LittleBirdWeaponInputPacket::handle)
                .add();
        CHANNEL.messageBuilder(LittleBirdImpactPacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LittleBirdImpactPacket::encode)
                .decoder(LittleBirdImpactPacket::decode)
                .consumerMainThread(LittleBirdImpactPacket::handle)
                .add();
        CHANNEL.messageBuilder(ApacheReloadPacket.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApacheReloadPacket::encode)
                .decoder(ApacheReloadPacket::decode)
                .consumerMainThread(ApacheReloadPacket::handle)
                .add();
        CHANNEL.messageBuilder(ApacheSeatSwitchPacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApacheSeatSwitchPacket::encode)
                .decoder(ApacheSeatSwitchPacket::decode)
                .consumerMainThread(ApacheSeatSwitchPacket::handle)
                .add();
        CHANNEL.messageBuilder(ApacheGunnerAimPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApacheGunnerAimPacket::encode)
                .decoder(ApacheGunnerAimPacket::decode)
                .consumerMainThread(ApacheGunnerAimPacket::handle)
                .add();
        CHANNEL.messageBuilder(LittleBirdCrashPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LittleBirdCrashPacket::encode)
                .decoder(LittleBirdCrashPacket::decode)
                .consumerMainThread(LittleBirdCrashPacket::handle)
                .add();
        CHANNEL.messageBuilder(JetControlPacket.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JetControlPacket::encode)
                .decoder(JetControlPacket::decode)
                .consumerMainThread(JetControlPacket::handle)
                .add();
        CHANNEL.messageBuilder(GroundWeaponInputPacket.class, 10,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(GroundWeaponInputPacket::encode)
                .decoder(GroundWeaponInputPacket::decode)
                .consumerMainThread(GroundWeaponInputPacket::handle)
                .add();
        CHANNEL.messageBuilder(JetFlarePacket.class, 12, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JetFlarePacket::encode)
                .decoder(JetFlarePacket::decode)
                .consumerMainThread(JetFlarePacket::handle)
                .add();
        CHANNEL.messageBuilder(LittleBirdTracerPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LittleBirdTracerPacket::encode)
                .decoder(LittleBirdTracerPacket::decode)
                .consumerMainThread(LittleBirdTracerPacket::handle)
                .add();
        CHANNEL.messageBuilder(LittleBirdHitFeedbackPacket.class, 3,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LittleBirdHitFeedbackPacket::encode)
                .decoder(LittleBirdHitFeedbackPacket::decode)
                .consumerMainThread(LittleBirdHitFeedbackPacket::handle)
                .add();
    }

    /** Bullet traces go to everyone tracking the aircraft, shooter included. */
    public static void sendTracer(
            net.minecraft.world.entity.Entity shooter, LittleBirdTracerPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> shooter), packet);
    }

    /**
     * Hit marker feedback for the shooter, carrying the target's remaining health so a hull hit
     * can also refresh the target readout.
     */
    public static void sendHitFeedback(
            net.minecraft.server.level.ServerPlayer player, boolean kill, boolean vehicle,
            float healthFraction) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new LittleBirdHitFeedbackPacket(kill, vehicle, healthFraction));
    }
}
