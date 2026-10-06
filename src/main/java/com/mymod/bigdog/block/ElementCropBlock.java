package com.mymod.bigdog.block;

import com.mymod.bigdog.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class ElementCropBlock extends CropBlock {
    public ElementCropBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return isPlantableOn(floor);
    }

    public static boolean isPlantableOn(BlockState floor) {
        return floor.isOf(Blocks.SAND) || floor.isOf(Blocks.RED_SAND) || floor.isOf(Blocks.CLAY);
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.ELEMENT_FLOWER_SEEDS;
    }
}
