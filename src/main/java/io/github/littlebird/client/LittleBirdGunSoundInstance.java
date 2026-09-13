package io.github.littlebird.client;

import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.weapon.LittleBirdGunCycle;
import io.github.littlebird.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Looping Gatling report; rises in pitch as the barrels heat up. */
final class LittleBirdGunSoundInstance extends AbstractTickableSoundInstance {
    private final LittleBirdEntity helicopter;

    LittleBirdGunSoundInstance(LittleBirdEntity helicopter) {
        super(ModSounds.GUN.get(), SoundSource.PLAYERS, RandomSource.create());
        this.helicopter = helicopter;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        update();
    }

    @Override
    public void tick() {
        int state = helicopter.getGunState();
        if (helicopter.isRemoved()
                || (state != LittleBirdGunCycle.WARMING && state != LittleBirdGunCycle.FIRING)) {
            stop();
            return;
        }
        update();
    }

    private void update() {
        this.x = helicopter.getX();
        this.y = helicopter.getY();
        this.z = helicopter.getZ();
        float heat = helicopter.getGunHeat() / (float) LittleBirdGunCycle.MAX_HEAT;
        this.volume = helicopter.getGunState() == LittleBirdGunCycle.WARMING ? 0.55F : 0.95F;
        this.pitch = 0.95F + 0.25F * heat;
    }
}