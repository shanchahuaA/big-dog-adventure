package com.mymod.bigfruit.registry;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.block.CottonCropBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block COTTON_CROP = register("cotton_crop",
            new CottonCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    private static Block register(String id, Block block) {
        return Registry.register(Registries.BLOCK, BigFruitMod.id(id), block);
    }

    public static void register() {
        BigFruitMod.LOGGER.info("Registering blocks for {}", BigFruitMod.MOD_ID);
    }
}
