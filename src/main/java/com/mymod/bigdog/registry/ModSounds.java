package com.mymod.bigdog.registry;

import com.mymod.bigdog.BigDogMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    private ModSounds() {}

    public static final SoundEvent BIG_DOG_CHARGE_LOOP = register("big_dog_charge_loop");
    public static final SoundEvent BIG_DOG_CALL = register("big_dog_call");
    public static final SoundEvent WIZARD_CHICKEN_BGM = register("wizard_chicken_bgm");

    private static SoundEvent register(String path) {
        Identifier id = BigDogMod.id(path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        BigDogMod.LOGGER.info("Registering sounds for {}", BigDogMod.MOD_ID);
    }
}
