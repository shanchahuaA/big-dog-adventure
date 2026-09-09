package com.mymod.bigfruit.item;

import com.mymod.bigfruit.entity.BigDogEntity;
import com.mymod.bigfruit.entity.DingdongChicken;
import com.mymod.bigfruit.registry.ModEffects;
import com.mymod.bigfruit.util.HorseColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.StewItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class HerbalSoupItem extends StewItem {
    public HerbalSoupItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient && user instanceof ServerPlayerEntity && world instanceof ServerWorld serverWorld) {
            ServerPlayerEntity player = (ServerPlayerEntity) user;
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof HorseEntity) {
                HorseEntity horse = (HorseEntity) vehicle;
                if (HorseColorUtil.RED.equals(HorseColorUtil.getColor(horse))) {
                    HorseColorUtil.setGreen(horse);
                }
            }
            for (Entity e : serverWorld.iterateEntities()) {
                if (e instanceof BigDogEntity) {
                    ((BigDogEntity) e).forgetTarget(player);
                } else if (e instanceof DingdongChicken) {
                    ((DingdongChicken) e).forgetTarget(player);
                }
            }
            player.addStatusEffect(new StatusEffectInstance(ModEffects.HERBAL_GRACE, 200, 0));
        }
        return result;
    }
}
