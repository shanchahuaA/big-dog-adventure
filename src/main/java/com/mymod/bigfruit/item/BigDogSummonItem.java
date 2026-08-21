package com.mymod.bigfruit.item;

import com.mymod.bigfruit.registry.ModEntities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class BigDogSummonItem extends Item {
    public BigDogSummonItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        BlockPos spawnPos = pos.offset(side);
        ItemStack stack = context.getStack();

        ServerWorld serverWorld = (ServerWorld) world;
        var type = ModEntities.BIG_DOG;
        var entity = type.create(serverWorld);
        if (entity != null) {
            entity.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360.0f, 0.0f);
            if (stack.hasCustomName()) {
                entity.setCustomName(stack.getName());
            }
            serverWorld.spawnEntity(entity);
            world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return ActionResult.CONSUME;
        }
        return ActionResult.FAIL;
    }
}
