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
        CHANNEL.messageBuilder(Plz05ElevationPacket.class, 41,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(Plz05ElevationPacket::encode)
                .decoder(Plz05ElevationPacket::decode)
                .consumerMainThread(Plz05ElevationPacket::handle)
                .add();
        CHANNEL.messageBuilder(Phl191ElevationPacket.class, 40,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(Phl191ElevationPacket::encode)
                .decoder(Phl191ElevationPacket::decode)
                .consumerMainThread(Phl191ElevationPacket::handle)
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
        CHANNEL.messageBuilder(MainGunImpactPacket.class, 13, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MainGunImpactPacket::encode)
                .decoder(MainGunImpactPacket::decode)
                .consumerMainThread(MainGunImpactPacket::handle)
                .add();
        CHANNEL.messageBuilder(B2BombReleasePacket.class, 14, NetworkDirection.PLAY_TO_SERVER)
                .encoder(B2BombReleasePacket::encode)
                .decoder(B2BombReleasePacket::decode)
                .consumerMainThread(B2BombReleasePacket::handle)
                .add();
        CHANNEL.messageBuilder(B2PodDesignatePacket.class, 15, NetworkDirection.PLAY_TO_SERVER)
                .encoder(B2PodDesignatePacket::encode)
                .decoder(B2PodDesignatePacket::decode)
                .consumerMainThread(B2PodDesignatePacket::handle)
                .add();
        CHANNEL.messageBuilder(B2WeaponSelectPacket.class, 16, NetworkDirection.PLAY_TO_SERVER)
                .encoder(B2WeaponSelectPacket::encode)
                .decoder(B2WeaponSelectPacket::decode)
                .consumerMainThread(B2WeaponSelectPacket::handle)
                .add();

        CHANNEL.messageBuilder(B2BombSpawnPacket.class, 17, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(B2BombSpawnPacket::encode)
                .decoder(B2BombSpawnPacket::decode)
                .consumerMainThread(B2BombSpawnPacket::handle)
                .add();

        CHANNEL.messageBuilder(B2BombKillPacket.class, 18, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(B2BombKillPacket::encode)
                .decoder(B2BombKillPacket::decode)
                .consumerMainThread(B2BombKillPacket::handle)
                .add();

        CHANNEL.messageBuilder(ArtilleryStrikeCallPacket.class, 42,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(ArtilleryStrikeCallPacket::encode)
                .decoder(ArtilleryStrikeCallPacket::decode)
                .consumerMainThread(ArtilleryStrikeCallPacket::handle)
                .add();
    }

    /** Bullet traces go to everyone tracking the aircraft, shooter included. */
    public static void sendTracer(
            net.minecraft.world.entity.Entity shooter, LittleBirdTracerPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> shooter), packet);
    }

    /**
     * Hit marker feedback for the shooter, carrying the target's name and remaining integrity so
     * the readout under the crosshair can print "60 / 120" instead of only a percentage.
     */
    public static void sendHitFeedback(
            net.minecraft.server.level.ServerPlayer player, boolean kill, String name,
            float health, float maxHealth) {
        if (player == null || player.connection == null) {
            return;
        }
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new LittleBirdHitFeedbackPacket(kill, name, health, maxHealth));
    }
}
