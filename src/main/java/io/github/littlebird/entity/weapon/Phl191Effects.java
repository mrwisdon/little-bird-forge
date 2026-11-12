package io.github.littlebird.entity.weapon;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Broadcasts the PHL-191's war effects out to a full observation distance.
 *
 * <p>Both vanilla shortcuts stop short for an artillery piece. The bulk particle packet only
 * reaches 32 blocks, and a server sound's audible radius is sixteen blocks per volume unit, so a
 * launch at volume two is heard a hundred paces away and no further. Everything here is routed
 * through one range constant instead: the packets are sent player by player, and the sound volume
 * is set to {@code RANGE / 16} so the server forwards it to everyone inside the ring.
 */
public final class Phl191Effects {
    /** How far away a launch or a warhead is watched and heard, in blocks. */
    public static final double RANGE = 150.0D;

    /** Sound volume that reaches {@link #RANGE} blocks: a server sound carries 16 blocks per unit. */
    public static final float SOUND_VOLUME = (float) (RANGE / 16.0D);

    private Phl191Effects() {
    }

    /** True when this seat is close enough to see the effect. */
    public static boolean within(ServerPlayer player, double x, double y, double z) {
        return player.distanceToSqr(x, y, z) <= RANGE * RANGE;
    }

    /** One particle burst seen by every player inside the observation ring. */
    public static void particles(ServerLevel server, ParticleOptions type, double x, double y,
                                 double z, int count, double xDist, double yDist, double zDist,
                                 double speed) {
        for (ServerPlayer player : server.players()) {
            if (within(player, x, y, z)) {
                server.sendParticles(player, type, true, x, y, z, count,
                        xDist, yDist, zDist, speed);
            }
        }
    }

    /** One sound heard by every player inside the observation ring. */
    public static void sound(ServerLevel server, Vec3 at, SoundEvent sound, float pitch,
                             float loudness) {
        server.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS,
                SOUND_VOLUME * loudness, pitch);
    }

    /** One particle burst around a world position, convenience overload. */
    public static void particles(ServerLevel server, ParticleOptions type, Vec3 at, int count,
                                 double spread, double speed) {
        particles(server, type, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /** One sound at a world position. */
    public static void sound(ServerLevel server, double x, double y, double z, SoundEvent sound,
                             float pitch, float loudness) {
        sound(server, new Vec3(x, y, z), sound, pitch, loudness);
    }
}
