package com.mymod.bigdog.registry;

import com.mymod.bigdog.BigDogMod;
import com.mymod.bigdog.block.ElementCropBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block ELEMENT_CROP = register("element_crop",
            new ElementCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    private static Block register(String id, Block block) {
        return Registry.register(Registries.BLOCK, BigDogMod.id(id), block);
    }

    public static void register() {
        BigDogMod.LOGGER.info("Registering blocks for {}", BigDogMod.MOD_ID);
    }
}
