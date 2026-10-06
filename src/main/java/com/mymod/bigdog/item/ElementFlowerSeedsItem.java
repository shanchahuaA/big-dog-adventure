package com.mymod.bigdog.item;

import com.mymod.bigdog.block.ElementCropBlock;
import com.mymod.bigdog.registry.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ElementFlowerSeedsItem extends Item {
    public ElementFlowerSeedsItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getSide() != Direction.UP) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        if (!ElementCropBlock.isPlantableOn(world.getBlockState(pos))) {
            return ActionResult.PASS;
        }
        BlockPos above = pos.up();
        if (!world.isAir(above)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        world.setBlockState(above, ModBlocks.ELEMENT_CROP.getDefaultState());
        ItemStack stack = context.getStack();
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
