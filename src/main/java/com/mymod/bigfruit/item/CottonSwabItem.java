package com.mymod.bigfruit.item;

import com.mymod.bigfruit.entity.BigDogEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class CottonSwabItem extends Item {
    public CottonSwabItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof BigDogEntity dog)) {
            return ActionResult.PASS;
        }
        if (user.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        dog.silence(BigDogEntity.SILENCE_TICKS);
        if (!user.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
