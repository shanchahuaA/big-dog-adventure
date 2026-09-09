package com.mymod.bigfruit.world;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class ModWorldGen {
    private ModWorldGen() {}

    private static RegistryKey<PlacedFeature> placedKey(String path) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, BigFruitMod.id(path));
    }

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        BiomeKeys.BADLANDS, BiomeKeys.WOODED_BADLANDS, BiomeKeys.ERODED_BADLANDS),
                GenerationStep.Feature.VEGETAL_DECORATION, placedKey("cotton_patch_badlands"));
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT),
                GenerationStep.Feature.VEGETAL_DECORATION, placedKey("cotton_patch_desert"));
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld()
                        .and(BiomeSelectors.excludeByKey(
                                BiomeKeys.BADLANDS, BiomeKeys.WOODED_BADLANDS,
                                BiomeKeys.ERODED_BADLANDS, BiomeKeys.DESERT)),
                GenerationStep.Feature.VEGETAL_DECORATION, placedKey("cotton_patch_others"));
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS),
                SpawnGroup.CREATURE, ModEntities.DINGDONG_CHICKEN, 4, 1, 1);
    }
}
