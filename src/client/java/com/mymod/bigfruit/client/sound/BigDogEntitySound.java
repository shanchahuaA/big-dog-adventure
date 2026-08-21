package com.mymod.bigfruit.client.sound;

import com.mymod.bigfruit.entity.BigDogEntity;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

public class BigDogEntitySound extends EntityTrackingSoundInstance {
    private final BigDogEntity bigDog;

    public BigDogEntitySound(BigDogEntity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        super(sound, category, 1.0f, pitch, entity, 1L);
        this.bigDog = entity;
        this.volume = volume;
        this.repeat = false;
        this.repeatDelay = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.bigDog.isRemoved() || !this.bigDog.isAlive()) {
            this.setDone();
        }
    }
}
