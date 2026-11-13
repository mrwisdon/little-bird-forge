package io.github.littlebird.client;

import io.github.littlebird.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Positional diesel loop that follows one ground vehicle and tracks how hard it is being driven.
 *
 * <p>One instance per vehicle, held by {@link GroundEngineSoundManager}. The loop stops itself as
 * soon as the driver's seat is empty, the hull is removed, or the engine request goes away, so a
 * parked and abandoned vehicle is silent without the manager having to be told.
 */
final class GroundEngineSoundInstance extends AbstractTickableSoundInstance {
    private final Entity vehicle;

    GroundEngineSoundInstance(Entity vehicle) {
        super(ModSounds.ENGINE.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        update();
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved() || !vehicle.isAlive() || !GroundEngineSoundManager.isDriven(vehicle)) {
            stop();
            return;
        }
        update();
    }

    /** Manager-side shutdown: {@code stop()} is protected on the vanilla base class. */
    void halt() {
        stop();
    }

    private void update() {
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
        Vec3 motion = vehicle.getDeltaMovement();
        float load = GroundEngineAudio.load(Math.hypot(motion.x, motion.z));
        this.volume = GroundEngineAudio.volume(load);
        this.pitch = GroundEngineAudio.pitch(load);
    }
}
