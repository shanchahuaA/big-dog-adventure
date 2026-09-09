package com.mymod.bigfruit.world;

import com.mymod.bigfruit.item.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.village.TradeOffer;

public final class ModLootAndTrades {
    private ModLootAndTrades() {}

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (id.equals(LootTables.DESERT_PYRAMID_CHEST)
                    || id.equals(LootTables.VILLAGE_DESERT_HOUSE_CHEST)
                    || id.equals(LootTables.VILLAGE_PLAINS_CHEST)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.35f))
                        .with(ItemEntry.builder(ModItems.COTTON_SEEDS)
                                .weight(10)
                                .apply(SetCountLootFunction.builder(
                                        UniformLootNumberProvider.create(1.0f, 3.0f)))));
            }
        });

        TradeOfferHelper.registerWanderingTraderOffers(1, factories -> factories.add(
                (entity, random) -> new TradeOffer(
                        new ItemStack(Items.EMERALD, 1),
                        new ItemStack(ModItems.COTTON_SEEDS, 2),
                        6, 1, 0.05f)));
    }
}
