package com.mymod.bigdog.util;

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
    public static final String DEMONIZED = "demonized";
    public static final String SUBLIMED = "sublimed";
    public static final String NONE = "none";

    public static final int DEMONIZED_DYE = 0xE02020;
    public static final int SUBLIMED_DYE = 0x20C020;

    public static final double DEMONIZED_MAX_HEALTH = 300.0;
    public static final double SUBLIMED_MAX_HEALTH = 500.0;

    private static final UUID DEMONIZED_SPEED_UUID = UUID.fromString("7d4e9c1a-2b5f-4e8a-9d3c-6f1a2b3c4d5e");
    private static final EntityAttributeModifier DEMONIZED_SPEED_BONUS = new EntityAttributeModifier(
            DEMONIZED_SPEED_UUID, "Demonized horse speed", 1.0, EntityAttributeModifier.Operation.MULTIPLY_BASE);

    public static String getColor(HorseEntity horse) {
        if (!(horse instanceof ColoredHorse colored)) {
            return null;
        }
        String raw = colored.bigdog_getColor();
        if (raw == null || raw.equals(NONE)) {
            return null;
        }
        return raw;
    }

    public static String getRaw(HorseEntity horse) {
        if (horse instanceof ColoredHorse colored) {
            return colored.bigdog_getColor();
        }
        return null;
    }

    public static void setRaw(HorseEntity horse, String color) {
        if (horse instanceof ColoredHorse colored) {
            colored.bigdog_setColor(color);
        }
    }

    public static boolean isColored(HorseEntity horse) {
        return getColor(horse) != null;
    }

    public static void applyMaxHealth(HorseEntity horse, String color, boolean heal) {
        double expected = SUBLIMED.equals(color) ? SUBLIMED_MAX_HEALTH : DEMONIZED_MAX_HEALTH;
        EntityAttributeInstance health = horse.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null) {
            return;
        }
        if (health.getBaseValue() != expected) {
            health.setBaseValue(expected);
        }
        if (heal || horse.getHealth() > expected) {
            horse.setHealth((float) expected);
        }
    }

    public static void setDemonized(HorseEntity horse) {
        setRaw(horse, DEMONIZED);
        equipDyedArmor(horse, DEMONIZED_DYE);
        applyMaxHealth(horse, DEMONIZED, true);
    }

    public static void setSublimed(HorseEntity horse) {
        setRaw(horse, SUBLIMED);
        equipDyedArmor(horse, SUBLIMED_DYE);
        applyMaxHealth(horse, SUBLIMED, true);
    }

    public static void clear(HorseEntity horse) {
        setRaw(horse, null);
        updateDemonizedSpeed(horse, false);
    }

    public static boolean isRidingDemonized(PlayerEntity player) {
        return DEMONIZED.equals(getRiddenColor(player));
    }

    /** 唯一的「骑马」判定入口：只有骑彩马（魔化/升华）才算，骑普通马不算。 */
    public static boolean isRidingColored(PlayerEntity player) {
        return getRiddenColor(player) != null;
    }

    public static String getRiddenColor(PlayerEntity player) {
        if (player.getVehicle() instanceof HorseEntity horse) {
            return getColor(horse);
        }
        return null;
    }

    public static void updateDemonizedSpeed(HorseEntity horse, boolean active) {
        EntityAttributeInstance speed = horse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean has = speed.getModifier(DEMONIZED_SPEED_UUID) != null;
        if (active && !has) {
            speed.addTemporaryModifier(DEMONIZED_SPEED_BONUS);
        } else if (!active && has) {
            speed.removeModifier(DEMONIZED_SPEED_UUID);
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
