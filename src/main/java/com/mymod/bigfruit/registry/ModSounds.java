package com.mymod.bigfruit.registry;

import com.mymod.bigfruit.BigFruitMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    private ModSounds() {}

    public static final SoundEvent BIG_DOG_CHARGE_LOOP = register("big_dog_charge_loop");
    public static final SoundEvent BIG_DOG_CALL = register("big_dog_call");

    private static SoundEvent register(String path) {
        Identifier id = BigFruitMod.id(path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        BigFruitMod.LOGGER.info("Registering sounds for {}", BigFruitMod.MOD_ID);
    }
}
