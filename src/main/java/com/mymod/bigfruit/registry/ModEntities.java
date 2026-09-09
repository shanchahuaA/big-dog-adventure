package com.mymod.bigfruit.registry;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.entity.BigDogEntity;
import com.mymod.bigfruit.entity.DingdongChicken;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<BigDogEntity> BIG_DOG = Registry.register(
            Registries.ENTITY_TYPE,
            BigFruitMod.id("big_dog"),
            FabricEntityTypeBuilder.<BigDogEntity>createMob()
                    .entityFactory(BigDogEntity::new)
                    .spawnGroup(SpawnGroup.CREATURE)
                    .dimensions(EntityDimensions.fixed(0.8f, 1.2f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(2)
                    .build()
    );

    public static final EntityType<DingdongChicken> DINGDONG_CHICKEN = Registry.register(
            Registries.ENTITY_TYPE,
            BigFruitMod.id("dingdong_chicken"),
            FabricEntityTypeBuilder.<DingdongChicken>createMob()
                    .entityFactory(DingdongChicken::new)
                    .spawnGroup(SpawnGroup.CREATURE)
                    .dimensions(EntityDimensions.fixed(0.4f, 0.7f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(2)
                    .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(BIG_DOG, BigDogEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DINGDONG_CHICKEN, DingdongChicken.createAttributes());
        BigFruitMod.LOGGER.info("Registering entities for {}", BigFruitMod.MOD_ID);
    }
}
