package com.mymod.bigfruit.item;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.registry.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item FRESH_FRUIT = register("fresh_fruit", new Item(new Item.Settings()));
    public static final Item BIG_DOG_SPAWN_EGG = register("big_dog_spawn_egg",
            new SpawnEggItem(ModEntities.BIG_DOG, 0xE6C87A, 0x6B3A2A, new Item.Settings()));
    public static final Item BIG_DOG_SUMMON = register("big_dog_summon",
            new BigDogSummonItem(new Item.Settings()));

    public static Item registerItems(String id, Item item){
        return Registry.register(Registries.ITEM, RegistryKey.of(Registries.ITEM.getKey(), new Identifier(BigFruitMod.MOD_ID, id)), item);
    }
    public static Item registerItem(String id, Item item){
        return Registry.register(Registries.ITEM, new Identifier(BigFruitMod.MOD_ID, id), item);
    }
    public static Item register(String id, Item item) {
        return register(new Identifier(BigFruitMod.MOD_ID, id), item);
    }

    public static Item register(Identifier id, Item item) {
        return register(RegistryKey.of(Registries.ITEM.getKey(), id), item);
    }

    public static Item register(RegistryKey<Item> key, Item item) {
        if (item instanceof BlockItem) {
            ((BlockItem)item).appendBlocks(Item.BLOCK_ITEMS, item);
        }

        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerItems(){
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(BIG_DOG_SPAWN_EGG);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(BIG_DOG_SUMMON);
        });
    }
}
