package com.mymod.bigdog.registry;

import com.mymod.bigdog.BigDogMod;
import com.mymod.bigdog.effect.ManaGraceEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEffects {
    private ModEffects() {}

    public static final StatusEffect MANA_GRACE = Registry.register(
            Registries.STATUS_EFFECT, BigDogMod.id("mana_grace"), new ManaGraceEffect());

    public static void register() {
        BigDogMod.LOGGER.info("Registering effects for {}", BigDogMod.MOD_ID);
    }
}
