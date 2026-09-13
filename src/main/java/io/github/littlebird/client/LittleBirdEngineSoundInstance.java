package io.github.littlebird.client;

import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Positional rotor loop that follows one helicopter and tracks its RPM. */
final class LittleBirdEngineSoundInstance extends AbstractTickableSoundInstance {
    private final LittleBirdEntity helicopter;

    LittleBirdEngineSoundInstance(LittleBirdEntity helicopter) {
        super(ModSounds.ENGINE.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.helicopter = helicopter;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        update();
    }

    @Override
    public void tick() {
        if (helicopter.isRemoved() || helicopter.getRenderRotorRpm() <= 0.02F) {
            stop();
            return;
        }
        update();
    }

    private void update() {
        this.x = helicopter.getX();
        this.y = helicopter.getY();
        this.z = helicopter.getZ();
        float rpm = helicopter.getRenderRotorRpm();
        this.volume = 0.12F + 0.85F * rpm;
        this.pitch = 0.75F + 0.75F * rpm;
    }
}