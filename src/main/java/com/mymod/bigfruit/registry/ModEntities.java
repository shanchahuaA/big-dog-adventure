package com.mymod.bigfruit.registry;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.entity.BigDogEntity;
import com.mymod.bigfruit.entity.DingdongChicken;
import com.mymod.bigfruit.entity.MagicCircleEntity;
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
                    .dimensions(EntityDimensions.fixed(0.4f, 1.42f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(2)
                    .build()
    );

    /**
     * 合奏法阵：纯表现、不持久（{@code disableSaving}），生成方 30 tick 后自行 discard。
     * 碰撞箱给得扁而宽是为了别被视锥过早剔除；{@code isCollidable()} 基础实现本来就返回 false。
     */
    public static final EntityType<MagicCircleEntity> MAGIC_CIRCLE = Registry.register(
            Registries.ENTITY_TYPE,
            BigFruitMod.id("magic_circle"),
            FabricEntityTypeBuilder.<MagicCircleEntity>create()
                    .entityFactory(MagicCircleEntity::new)
                    .spawnGroup(SpawnGroup.MISC)
                    .dimensions(EntityDimensions.fixed(6.0f, 0.0625f))
                    .trackRangeBlocks(64)
                    .disableSaving()
                    .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(BIG_DOG, BigDogEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DINGDONG_CHICKEN, DingdongChicken.createAttributes());
        BigFruitMod.LOGGER.info("Registering entities for {}", BigFruitMod.MOD_ID);
    }
}
