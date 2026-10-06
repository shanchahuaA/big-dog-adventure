package com.mymod.bigdog.item;

import com.mymod.bigdog.entity.BigDogEntity;
import com.mymod.bigdog.entity.WizardChicken;
import com.mymod.bigdog.registry.ModEffects;
import com.mymod.bigdog.util.HorseColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.StewItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class ManaPotionItem extends StewItem {
    public ManaPotionItem(Settings settings) {
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
                if (HorseColorUtil.DEMONIZED.equals(HorseColorUtil.getColor(horse))) {
                    HorseColorUtil.setSublimed(horse);
                }
            }
            for (Entity e : serverWorld.iterateEntities()) {
                if (e instanceof BigDogEntity) {
                    ((BigDogEntity) e).forgetTarget(player);
                } else if (e instanceof WizardChicken) {
                    ((WizardChicken) e).forgetTarget(player);
                }
            }
            player.addStatusEffect(new StatusEffectInstance(ModEffects.MANA_GRACE, 200, 0));
        }
        return result;
    }
}
