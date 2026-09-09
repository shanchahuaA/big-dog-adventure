package com.mymod.bigfruit.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.UUID;

public final class HorseColorUtil {
    private HorseColorUtil() {}

    public static final String NBT_KEY = "horse_color";
    public static final String RED = "red";
    public static final String GREEN = "green";
    public static final String NONE = "none";

    public static final int RED_DYE = 0xE02020;
    public static final int GREEN_DYE = 0x20C020;

    private static final UUID RED_SPEED_UUID = UUID.fromString("7d4e9c1a-2b5f-4e8a-9d3c-6f1a2b3c4d5e");
    private static final EntityAttributeModifier RED_SPEED_BONUS = new EntityAttributeModifier(
            RED_SPEED_UUID, "Red horse speed", 1.0, EntityAttributeModifier.Operation.MULTIPLY_BASE);

    public static String getColor(HorseEntity horse) {
        if (!(horse instanceof ColoredHorse colored)) {
            return null;
        }
        String raw = colored.bigfruit_getColor();
        if (raw == null || raw.equals(NONE)) {
            return null;
        }
        return raw;
    }

    public static String getRaw(HorseEntity horse) {
        if (horse instanceof ColoredHorse colored) {
            return colored.bigfruit_getColor();
        }
        return null;
    }

    public static void setRaw(HorseEntity horse, String color) {
        if (horse instanceof ColoredHorse colored) {
            colored.bigfruit_setColor(color);
        }
    }

    public static void setRed(HorseEntity horse) {
        setRaw(horse, RED);
        equipDyedArmor(horse, RED_DYE);
    }

    public static void setGreen(HorseEntity horse) {
        setRaw(horse, GREEN);
        equipDyedArmor(horse, GREEN_DYE);
    }

    public static void clear(HorseEntity horse) {
        setRaw(horse, null);
        updateRedSpeed(horse, false);
    }

    public static boolean isRidingRed(PlayerEntity player) {
        return RED.equals(getRiddenColor(player));
    }

    public static String getRiddenColor(PlayerEntity player) {
        if (player.getVehicle() instanceof HorseEntity horse) {
            return getColor(horse);
        }
        return null;
    }

    public static void updateRedSpeed(HorseEntity horse, boolean active) {
        EntityAttributeInstance speed = horse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean has = speed.getModifier(RED_SPEED_UUID) != null;
        if (active && !has) {
            speed.addTemporaryModifier(RED_SPEED_BONUS);
        } else if (!active && has) {
            speed.removeModifier(RED_SPEED_UUID);
        }
    }

    public static void equipDyedArmor(HorseEntity horse, int dyeColor) {
        ItemStack current = horse.getEquippedStack(EquipmentSlot.CHEST);
        if (!current.isEmpty() && !current.isOf(Items.LEATHER_HORSE_ARMOR)) {
            return;
        }
        ItemStack armor = new ItemStack(Items.LEATHER_HORSE_ARMOR);
        if (armor.getItem() instanceof DyeableItem dyeable) {
            dyeable.setColor(armor, dyeColor);
        }
        horse.equipStack(EquipmentSlot.CHEST, armor);
    }
}
