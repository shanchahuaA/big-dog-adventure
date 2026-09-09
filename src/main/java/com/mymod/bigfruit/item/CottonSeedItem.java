package com.mymod.bigfruit.item;

import com.mymod.bigfruit.block.CottonCropBlock;
import com.mymod.bigfruit.registry.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CottonSeedItem extends Item {
    public CottonSeedItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getSide() != Direction.UP) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        if (!CottonCropBlock.isPlantableOn(world.getBlockState(pos))) {
            return ActionResult.PASS;
        }
        BlockPos above = pos.up();
        if (!world.isAir(above)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        world.setBlockState(above, ModBlocks.COTTON_CROP.getDefaultState());
        ItemStack stack = context.getStack();
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
