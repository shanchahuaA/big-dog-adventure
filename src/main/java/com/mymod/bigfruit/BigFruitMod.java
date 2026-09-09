package com.mymod.bigfruit;

import com.mymod.bigfruit.ai.ChickenBgmManager;
import com.mymod.bigfruit.event.HorseTickHandler;
import com.mymod.bigfruit.item.ModItems;
import com.mymod.bigfruit.registry.ModBlocks;
import com.mymod.bigfruit.registry.ModEffects;
import com.mymod.bigfruit.registry.ModEntities;
import com.mymod.bigfruit.registry.ModSounds;
import com.mymod.bigfruit.world.ModLootAndTrades;
import com.mymod.bigfruit.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigFruitMod implements ModInitializer {
	public static final String MOD_ID = "big-fruit-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModEntities.register();
		ModSounds.register();
		ModEffects.register();
		ModItems.registerItems();
		ModWorldGen.register();
		ModLootAndTrades.register();
		ChickenBgmManager.register();
		HorseTickHandler.register();
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
