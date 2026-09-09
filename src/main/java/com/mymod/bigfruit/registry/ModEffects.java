package com.mymod.bigfruit.registry;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.effect.HerbalGraceEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEffects {
    private ModEffects() {}

    public static final StatusEffect HERBAL_GRACE = Registry.register(
            Registries.STATUS_EFFECT, BigFruitMod.id("herbal_grace"), new HerbalGraceEffect());

    public static void register() {
        BigFruitMod.LOGGER.info("Registering effects for {}", BigFruitMod.MOD_ID);
    }
}
